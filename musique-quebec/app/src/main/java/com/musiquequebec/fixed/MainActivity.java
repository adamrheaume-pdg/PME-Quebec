package com.musiquequebec.fixed;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView webView;
    private ExecutorService loader;
    private final Handler main = new Handler(Looper.getMainLooper());
    private static final String APP_ORIGIN = "https://musique-quebec.local/";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(11, 13, 16));
        getWindow().setNavigationBarColor(Color.rgb(11, 13, 16));
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 13, 16));
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(webView);

        webView.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom, left, right;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top; bottom = bars.bottom; left = bars.left; right = bars.right;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft();
                right = insets.getSystemWindowInsetRight();
            }
            if (v.getPaddingLeft() != left || v.getPaddingTop() != top ||
                    v.getPaddingRight() != right || v.getPaddingBottom() != bottom) {
                v.setPadding(left, top, right, bottom);
            }
            return insets;
        });

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);
        if (Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.requestApplyInsets();

        // Affiche d'abord une page locale minuscule afin que la première frame arrive vite.
        webView.loadDataWithBaseURL(APP_ORIGIN,
                "<!doctype html><meta name=viewport content='width=device-width,initial-scale=1'>" +
                "<body style='margin:0;background:#0b0d10;color:#fff;font-family:sans-serif'>" +
                "<div style='padding:28px 20px;font-size:22px;font-weight:700'>Musique Québec</div></body>",
                "text/html", "UTF-8", APP_ORIGIN);

        // Les fichiers de l'application sont préparés hors du thread UI pour éviter l'ANR.
        loader = Executors.newSingleThreadExecutor();
        loader.execute(() -> {
            try {
                String html = readAsset("index.html");
                String logo64 = readAsset("logo.b64").replace("\n", "").replace("\r", "");
                String logoData = "data:image/webp;base64," + logo64;
                html = html.replace("__LOGO_DATA__", logoData);
                html = html.replace("<div class=\"logo\">⚜️</div>",
                        "<img class=\"logo\" src=\"" + logoData + "\" alt=\"Musique Québec\">");
                final String finalHtml = html;
                main.post(() -> {
                    if (!isFinishing() && !isDestroyed() && webView != null) {
                        webView.loadDataWithBaseURL(APP_ORIGIN, finalHtml,
                                "text/html", "UTF-8", APP_ORIGIN);
                    }
                });
            } catch (Exception e) {
                final String msg = String.valueOf(e).replace("<", "&lt;").replace(">", "&gt;");
                main.post(() -> {
                    if (webView != null) {
                        webView.loadData("<body style='background:#0b0d10;color:#fff'><h2>Erreur de chargement</h2><pre>" + msg + "</pre></body>",
                                "text/html", "UTF-8");
                    }
                });
            }
        });
    }

    private String readAsset(String name) throws Exception {
        try (InputStream in = getAssets().open(name);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (loader != null) {
            loader.shutdownNow();
            loader = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
