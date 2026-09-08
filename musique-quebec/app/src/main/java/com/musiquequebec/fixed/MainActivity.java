package com.musiquequebec.fixed;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String APP_ORIGIN = "https://musique-quebec.local/";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setBuiltInZoomControls(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);
        try {
            String html = readAsset("index.html");
            String logo64 = readAsset("logo.b64").replace("\n", "").replace("\r", "");
            html = html.replace("__LOGO_DATA__", "data:image/png;base64," + logo64);
            webView.loadDataWithBaseURL(APP_ORIGIN, html, "text/html", "UTF-8", APP_ORIGIN);
        } catch (Exception e) {
            webView.loadData("<h2>Erreur de chargement</h2><pre>" + e + "</pre>", "text/html", "UTF-8");
        }
    }

    private String readAsset(String name) throws Exception {
        InputStream in = getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString(StandardCharsets.UTF_8.name());
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
