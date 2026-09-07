package com.nexusduo.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.net.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;

public class BrowserActivity extends Activity {
    WebView w;

    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    TextView b(String s){
        TextView t=new TextView(this);
        t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(20);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(10),0,dp(10),0);
        return t;
    }

    private boolean isWeb(Uri u){
        if(u==null) return false;
        String s=u.getScheme();
        return "http".equalsIgnoreCase(s)||"https".equalsIgnoreCase(s);
    }

    private boolean keepInside(WebView view, Uri u){
        if(u==null) return true;
        if(isWeb(u)){
            view.loadUrl(u.toString());
            return true;
        }
        String scheme=u.getScheme()==null?"":u.getScheme().toLowerCase();
        if("intent".equals(scheme)){
            try{
                Intent intent=Intent.parseUri(u.toString(),Intent.URI_INTENT_SCHEME);
                String fallback=intent.getStringExtra("browser_fallback_url");
                if(fallback!=null){
                    Uri f=Uri.parse(fallback);
                    if(isWeb(f)) view.loadUrl(f.toString());
                }
            }catch(Exception ignored){}
            Toast.makeText(this,"Ouverture d’application bloquée — reste dans Nexus",Toast.LENGTH_SHORT).show();
            return true;
        }
        // Bloque les deep-links d'apps (tiktok://, instagram://, fb://, twitter://, etc.)
        Toast.makeText(this,"Lien d’application bloqué — reste dans Nexus",Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override public void onCreate(Bundle x){
        super.onCreate(x);
        String name=getIntent().getStringExtra("name");
        String url=getIntent().getStringExtra("url");

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8,10,15));

        LinearLayout bar=new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4),0,dp(4),0);
        TextView back=b("‹");
        TextView title=b(name);
        title.setTextSize(16); title.setTypeface(null,1);
        TextView refresh=b("↻");
        TextView ext=b("↗");
        bar.addView(back,new LinearLayout.LayoutParams(dp(52),dp(54)));
        bar.addView(title,new LinearLayout.LayoutParams(0,dp(54),1));
        bar.addView(refresh,new LinearLayout.LayoutParams(dp(52),dp(54)));
        bar.addView(ext,new LinearLayout.LayoutParams(dp(52),dp(54)));
        root.addView(bar);

        w=new WebView(this);
        WebSettings s=w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        if(Build.VERSION.SDK_INT>=26) s.setSafeBrowsingEnabled(true);
        // Présente Nexus comme un navigateur mobile plutôt qu'une WebView Android.
        String ua=s.getUserAgentString();
        s.setUserAgentString(ua.replace("; wv", "").replace(" Version/4.0", ""));

        CookieManager cm=CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(w,true);

        w.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg){
                // Refuse les fenêtres externes/popups qui servent souvent à lancer une app.
                return false;
            }
        });

        w.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
                return keepInside(view,request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,String target){
                return keepInside(view,Uri.parse(target));
            }
        });

        root.addView(w,new LinearLayout.LayoutParams(-1,0,1));
        back.setOnClickListener(v->{ if(w.canGoBack()) w.goBack(); else finish(); });
        refresh.setOnClickListener(v->w.reload());
        // Seul ce bouton, choisi volontairement par l'utilisateur, peut quitter Nexus.
        ext.setOnClickListener(v->{
            try{
                String current=w.getUrl();
                if(current!=null) startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(current)));
            }catch(Exception e){
                Toast.makeText(this,"Impossible d’ouvrir l’application externe",Toast.LENGTH_SHORT).show();
            }
        });

        w.loadUrl(url);
        setContentView(root);
    }

    @Override public void onBackPressed(){
        if(w!=null&&w.canGoBack()) w.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy(){
        if(w!=null){ w.stopLoading(); w.destroy(); }
        super.onDestroy();
    }
}
