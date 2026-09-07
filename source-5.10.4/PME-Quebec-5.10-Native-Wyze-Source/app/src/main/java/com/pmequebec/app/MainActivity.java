package com.pmequebec.app;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
    private static final String API = "http://127.0.0.1:1984";
    private static final String KS_ALIAS = "pme_wyze_credentials_v1";
    private static final String PREF = "pme_wyze_secure";
    private WebView webView;
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Map<String,String> playerById = new ConcurrentHashMap<>();
    private Process go2rtc;
    private volatile String engineState = "initialisation";
    private volatile String engineLastLog = "";
    private static final int REQ_CAMERA = 4104;
    private PermissionRequest pendingCameraRequest;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);

        // Keep the WebView inside Android system bars. This avoids Android 15+
        // edge-to-edge overlap on the login screen and floating controls.
        Window window = getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        window.setStatusBarColor(Color.rgb(10, 12, 18));
        window.setNavigationBarColor(Color.rgb(10, 12, 18));
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        webView = new WebView(this);
        webView.setFitsSystemWindows(true);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        webView.setScrollbarFadingEnabled(false);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUseWideViewPort(false);
        s.setLoadWithOverviewMode(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> handleWebPermissionRequest(request));
            }
        });
        webView.addJavascriptInterface(new WyzeBridge(), "WyzeNative");
        setContentView(webView);
        startGo2RtcIfBundled();
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void handleWebPermissionRequest(PermissionRequest request) {
        boolean wantsCamera = false;
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                wantsCamera = true;
                break;
            }
        }
        if (!wantsCamera) {
            request.deny();
            return;
        }

        if (android.os.Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            return;
        }

        pendingCameraRequest = request;
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_CAMERA) return;

        PermissionRequest request = pendingCameraRequest;
        pendingCameraRequest = null;
        if (request == null) return;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
        } else {
            request.deny();
        }
    }

    private void startGo2RtcIfBundled() {
        File bin = new File(getApplicationInfo().nativeLibraryDir, "libgo2rtc.so");
        if (!bin.exists()) {
            engineState = "binaire ARM64 absent";
            return;
        }
        engineState = "démarrage du moteur";
        io.execute(() -> {
            try {
                File cfg = new File(getFilesDir(), "go2rtc.yaml");
                try (FileOutputStream out = new FileOutputStream(cfg, false)) {
                    String text = "api:\n  listen: 127.0.0.1:1984\n  origin: '*'\n" +
                            "rtsp:\n  listen: 127.0.0.1:8554\n" +
                            "log:\n  level: info\n";
                    out.write(text.getBytes(StandardCharsets.UTF_8));
                }
                ProcessBuilder pb = new ProcessBuilder(bin.getAbsolutePath(), "-config", cfg.getAbsolutePath());
                pb.directory(getFilesDir());
                pb.redirectErrorStream(true);
                go2rtc = pb.start();
                engineState = "processus démarré";
                try (BufferedReader r = new BufferedReader(new InputStreamReader(go2rtc.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        engineLastLog = line;
                        android.util.Log.i("go2rtc", line);
                    }
                }
                int exit = go2rtc.waitFor();
                engineState = "moteur arrêté (code " + exit + ")";
            } catch (Exception e) {
                engineState = "échec démarrage: " + (e.getMessage()==null?e.toString():e.getMessage());
                android.util.Log.e("go2rtc", "Engine start failed", e);
            }
        });
    }

    private String http(String method, String path, String body) throws Exception {
        URL u = new URL(API + path);
        HttpURLConnection c = (HttpURLConnection)u.openConnection();
        c.setConnectTimeout(6000); c.setReadTimeout(20000); c.setRequestMethod(method);
        c.setUseCaches(false);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = readAll(in);
        if (code >= 400) throw new IOException("HTTP " + code + ": " + text.trim());
        return text;
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        try (InputStream x = in; ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192]; int n;
            while((n=x.read(buf))>0)b.write(buf,0,n);
            return b.toString("UTF-8");
        }
    }

    private static String enc(String s){ try{return URLEncoder.encode(s==null?"":s,"UTF-8");}catch(Exception e){return "";} }
    private static String jsonError(Throwable e){
        try { return new JSONObject().put("error", e.getMessage()==null?e.toString():e.getMessage()).toString(); }
        catch(Exception x){ return "{\"error\":\"native error\"}"; }
    }

    private SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (ks.containsAlias(KS_ALIAS)) return ((KeyStore.SecretKeyEntry)ks.getEntry(KS_ALIAS, null)).getSecretKey();
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(KS_ALIAS, KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return kg.generateKey();
    }

    private void securePut(String name, String value) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key());
        byte[] ct = c.doFinal((value==null?"":value).getBytes(StandardCharsets.UTF_8));
        String packed = Base64.encodeToString(c.getIV(), Base64.NO_WRAP) + "." + Base64.encodeToString(ct, Base64.NO_WRAP);
        getSharedPreferences(PREF, MODE_PRIVATE).edit().putString(name, packed).apply();
    }

    private String secureGet(String name) {
        try {
            String packed = getSharedPreferences(PREF, MODE_PRIVATE).getString(name, "");
            if (packed == null || packed.isEmpty()) return "";
            String[] p = packed.split("\\.",2); if(p.length!=2)return "";
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(p[0],Base64.NO_WRAP)));
            return new String(c.doFinal(Base64.decode(p[1],Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch(Exception e){ return ""; }
    }

    public class WyzeBridge {
        @JavascriptInterface public boolean engineAvailable() {
            try {
                if (go2rtc == null || !go2rtc.isAlive()) return false;
                http("GET", "/api/streams", null);
                engineState = "prêt";
                return true;
            } catch(Exception e) {
                engineState = "API locale indisponible: " + (e.getMessage()==null?e.toString():e.getMessage());
                return false;
            }
        }

        @JavascriptInterface public String getEngineStatus() {
            try {
                JSONObject o = new JSONObject();
                o.put("state", engineState);
                o.put("alive", go2rtc != null && go2rtc.isAlive());
                o.put("lastLog", engineLastLog);
                return o.toString();
            } catch(Exception e) { return jsonError(e); }
        }

        @JavascriptInterface public boolean saveSecureWyzeAccount(String email, String password, String apiId, String apiKey) {
            try { securePut("email",email); securePut("password",password); securePut("apiId",apiId); securePut("apiKey",apiKey); return true; }
            catch(Exception e){ return false; }
        }
        @JavascriptInterface public boolean saveSecureWyzeCredentials(String apiId, String apiKey) {
            try { securePut("apiId",apiId); securePut("apiKey",apiKey); return true; } catch(Exception e){ return false; }
        }
        @JavascriptInterface public String getSecureWyzeKeyId(){ return secureGet("apiId"); }
        @JavascriptInterface public String getSecureWyzeApiKey(){ return secureGet("apiKey"); }
        @JavascriptInterface public String getSecureWyzeEmail(){ return secureGet("email"); }
        @JavascriptInterface public boolean hasSecureWyzePassword(){ return !secureGet("password").isEmpty(); }
        @JavascriptInterface public void clearSecureWyzeCredentials(){ getSharedPreferences(PREF,MODE_PRIVATE).edit().clear().apply(); playerById.clear(); }

        @JavascriptInterface public String authenticate(String email, String password, String apiKey, String apiId) {
            try {
                String form = "email="+enc(email)+"&password="+enc(password)+"&api_key="+enc(apiKey)+"&api_id="+enc(apiId);
                return http("POST", "/api/wyze", form);
            } catch(Exception e){ return jsonError(e); }
        }

        @JavascriptInterface public String getDevices() {
            String email=secureGet("email"), password=secureGet("password"), apiId=secureGet("apiId"), apiKey=secureGet("apiKey");
            if(email.isEmpty()||password.isEmpty()||apiId.isEmpty()||apiKey.isEmpty()) return "{\"error\":\"Courriel, mot de passe, Key ID et API Key Wyze requis.\"}";
            try {
                String raw = authenticate(email,password,apiKey,apiId);
                JSONObject root = new JSONObject(raw);
                if(root.has("error")) return raw;
                JSONArray srcs = root.optJSONArray("sources");
                if(srcs==null) return "[]";
                JSONArray out = new JSONArray(); playerById.clear();
                for(int i=0;i<srcs.length();i++) {
                    JSONObject s=srcs.optJSONObject(i); if(s==null) continue;
                    String name=s.optString("name","Caméra "+(i+1));
                    String info=s.optString("info","");
                    String wyzeUrl=s.optString("url","");
                    String streamName="wyze_"+(i+1);
                    http("PUT", "/api/streams?name="+enc(streamName)+"&src="+enc(wyzeUrl), null);
                    JSONObject d=new JSONObject(); d.put("name",name); d.put("protocol","Cloud/P2P"); d.put("streamUrl", API+"/stream.html?src="+enc(streamName));
                    String[] parts=info.split("\\s*\\|\\s*");
                    if(parts.length>0)d.put("model",parts[0]); if(parts.length>1)d.put("mac",parts[1]); if(parts.length>2)d.put("ip",parts[2]);
                    try {
                        URL wu=new URL(wyzeUrl.replace("wyze://","http://"));
                        String query=wu.getQuery(); if(query!=null) for(String pair:query.split("&")){String[] kv=pair.split("=",2); if(kv.length==2){String k=URLDecoder.decode(kv[0],"UTF-8"),v=URLDecoder.decode(kv[1],"UTF-8"); if(k.equals("uid"))d.put("uid",v);}}
                    } catch(Exception ignored){}
                    String mac=d.optString("mac",""); String uid=d.optString("uid",""); String ip=d.optString("ip","");
                    String purl=d.getString("streamUrl"); if(!mac.isEmpty())playerById.put(mac,purl); if(!uid.isEmpty())playerById.put(uid,purl); if(!ip.isEmpty())playerById.put(ip,purl); playerById.put(String.valueOf(i+1),purl);
                    out.put(d);
                }
                return out.toString();
            } catch(Exception e){ return jsonError(e); }
        }

        @JavascriptInterface public String listDevices(){ return getDevices(); }
        @JavascriptInterface public String cameras(String ignoredEmail){ return getDevices(); }
        @JavascriptInterface public String getStreamUrl(String id){ return playerById.getOrDefault(id,""); }
        @JavascriptInterface public String streamUrl(String id){ return getStreamUrl(id); }
        @JavascriptInterface public String playerUrl(String streamName){ return API+"/stream.html?src="+enc(streamName); }
    }

    @Override protected void onDestroy() {
        if (go2rtc != null) go2rtc.destroy();
        io.shutdownNow();
        if(webView!=null)webView.destroy();
        super.onDestroy();
    }
}
