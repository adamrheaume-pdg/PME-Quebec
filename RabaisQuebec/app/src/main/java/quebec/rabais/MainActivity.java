package quebec.rabais;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.Window;

public class MainActivity extends Activity {
    private WebView web;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(6,26,53));
        w.setNavigationBarColor(Color.rgb(6,26,53));
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("http".equals(u.getScheme()) || "https".equals(u.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                    return true;
                }
                return false;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
