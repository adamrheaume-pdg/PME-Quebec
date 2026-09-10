package com.musiquequebec.fixed;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String APP_ORIGIN = "https://musique-quebec.local/";
    private boolean initialPageLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Window window = getWindow();
            window.setStatusBarColor(Color.rgb(11, 13, 16));
            window.setNavigationBarColor(Color.rgb(11, 13, 16));

            webView = new WebView(this);
            webView.setBackgroundColor(Color.rgb(11, 13, 16));
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            WebSettings s = webView.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setMediaPlaybackRequiresUserGesture(false);
            s.setAllowFileAccess(true);
            s.setAllowContentAccess(true);
            s.setBuiltInZoomControls(false);
            s.setDisplayZoomControls(false);
            s.setSupportZoom(false);

            webView.setWebChromeClient(new WebChromeClient());
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    if (request.isForMainFrame()) {
                        String url = String.valueOf(request.getUrl());
                        if (initialPageLoaded && (APP_ORIGIN.equals(url) || (APP_ORIGIN + "/").equals(url))) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (url != null && url.startsWith(APP_ORIGIN)) initialPageLoaded = true;
                }
            });
            setContentView(webView);

            boolean restored = false;
            if (savedInstanceState != null) {
                try { restored = webView.restoreState(savedInstanceState) != null; } catch (Throwable ignored) { }
            }

            if (!restored) {
                String html = readAsset("index.html");
                html = html.replace("<div class=\"logo\">⚜️</div>",
                        "<img class=\"logo\" src=\"file:///android_res/drawable/musique_quebec.webp\" alt=\"Musique Québec\">");
                webView.loadDataWithBaseURL(APP_ORIGIN, html, "text/html", "UTF-8", null);
            } else {
                initialPageLoaded = true;
            }
        } catch (Throwable e) {
            showFallback(e);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        try { if (webView != null) webView.saveState(outState); } catch (Throwable ignored) { }
        super.onSaveInstanceState(outState);
    }

    private void showFallback(Throwable error) {
        try {
            if (webView == null) webView = new WebView(this);
            setContentView(webView);
            String msg = String.valueOf(error).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            webView.loadData("<html><body style='background:#0b0d10;color:white;font-family:sans-serif;padding:24px'><h2>Musique Québec</h2><p>Erreur de démarrage :</p><pre style='white-space:pre-wrap'>" + msg + "</pre></body></html>", "text/html", "UTF-8");
        } catch (Throwable ignored) { }
    }

    private String readAsset(String name) throws Exception {
        try (InputStream in = getAssets().open(name); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            try { webView.stopLoading(); webView.setWebChromeClient(null); webView.setWebViewClient(null); webView.destroy(); } catch (Throwable ignored) { }
            webView = null;
        }
        super.onDestroy();
    }
}
