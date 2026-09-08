package quebec.maison.universelle;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.webkit.*;
import android.widget.*;
import android.view.*;
import android.content.*;

public class BellRouterActivity extends Activity {
    private WebView web;
    private TextView status;
    private String targetIp, targetMac, gateway;
    private boolean block;
    private final Handler handler = new Handler();
    private int attempts = 0;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        Intent i=getIntent();
        targetIp=i.getStringExtra("target_ip");
        targetMac=i.getStringExtra("target_mac");
        gateway=i.getStringExtra("gateway");
        block=i.getBooleanExtra("block",true);
        if(gateway==null||gateway.isEmpty())gateway="192.168.2.1";

        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
        status=new TextView(this);status.setTextSize(15);status.setTextColor(Color.rgb(16,33,58));status.setPadding(16,16,16,16);
        status.setText((block?"Blocage":"Déblocage")+" de "+targetIp+(targetMac==null||targetMac.isEmpty()?"":" • "+targetMac)+" — connexion au routeur Bell…");
        root.addView(status,new LinearLayout.LayoutParams(-1,-2));

        Button retry=new Button(this);retry.setText("Réessayer l’action");retry.setOnClickListener(v->{attempts=0;tryAutomation();});root.addView(retry,new LinearLayout.LayoutParams(-1,-2));

        web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);s.setUserAgentString(s.getUserAgentString()+" WiFiQuebec/1.3");
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String url){super.onPageFinished(v,url);handler.postDelayed(()->tryAutomation(),900);}
        });
        web.loadUrl("http://"+gateway+"/");
    }

    private void tryAutomation(){
        if(web==null||attempts++>8)return;
        String ip=js(targetIp==null?"":targetIp);
        String mac=js(targetMac==null?"":targetMac);
        String mode=block?"block":"unblock";
        String script="(function(){"+
            "function norm(x){return (x||'').toLowerCase().replace(/\\s+/g,' ').trim();}"+
            "function txt(e){return norm(e.innerText||e.textContent||e.value||e.getAttribute('aria-label')||'');}"+
            "var body=norm(document.body&&document.body.innerText);"+
            "if(body.indexOf('mot de passe')>=0||body.indexOf('password')>=0){return 'LOGIN';}"+
            "var all=[].slice.call(document.querySelectorAll('a,button,input,[role=button],label,div,span'));"+
            "var access=all.find(function(e){var t=txt(e);return t==='access control'||t.indexOf('contrôle d’accès')>=0||t.indexOf('controle d acces')>=0;});"+
            "if(access&&!/access/i.test(location.href)){try{access.click();return 'NAV_ACCESS';}catch(e){}}"+
            "var target='"+mac+"'; if(!target)target='"+ip+"';"+
            "var node=all.find(function(e){return target&&txt(e).indexOf(target.toLowerCase())>=0;});"+
            "if(!node){var show=all.find(function(e){var t=txt(e);return t.indexOf('show all')>=0||t.indexOf('afficher tout')>=0;});if(show){try{show.click();return 'SHOW_ALL';}catch(e){}}return 'TARGET_NOT_FOUND';}"+
            "var row=node;for(var k=0;k<5&&row;k++,row=row.parentElement){if(row.querySelector&&row.querySelector('input[type=checkbox],input[type=radio],button,[role=switch]'))break;}"+
            "if(!row)row=node.parentElement;"+
            "var controls=row.querySelectorAll?row.querySelectorAll('input[type=checkbox],input[type=radio],button,[role=switch]'):[];"+
            "if(controls.length){var c=controls[0];var checked=!!(c.checked||c.getAttribute('aria-checked')==='true'||c.classList.contains('active'));if(('"+mode+"'==='block'&&!checked)||('"+mode+"'==='unblock'&&checked)){try{c.click();}catch(e){}}}"+
            "if('"+mode+"'==='block'){var always=all.find(function(e){var t=txt(e);return t==='always'||t.indexOf('always block')>=0||t.indexOf('toujours')>=0;});if(always){try{always.click();}catch(e){}}}"+
            "var apply=all.find(function(e){var t=txt(e);return t==='apply'||t==='appliquer';});if(apply){try{apply.click();}catch(e){}}"+
            "var save=all.find(function(e){var t=txt(e);return t==='save'||t==='enregistrer';});if(save){try{save.click();return 'SAVED';}catch(e){}}"+
            "return 'READY';"+
        "})();";
        web.evaluateJavascript(script,value->{
            String r=value==null?"":value.replace("\"","");
            if(r.contains("LOGIN")){
                status.setText("Connecte-toi à la Borne giga 2.0. WiFi Québec reprendra automatiquement l’action après l’ouverture de session.");
                handler.postDelayed(()->tryAutomation(),2500);
            } else if(r.contains("SAVED")){
                status.setText((block?"Blocage":"Déblocage")+" envoyé au routeur Bell pour "+targetIp+". Vérifie l’état dans Contrôle d’accès.");
            } else if(r.contains("TARGET_NOT_FOUND")){
                status.setText("Contrôle d’accès Bell ouvert, mais l’appareil n’a pas encore été trouvé automatiquement. Choisis l’appareil correspondant à "+targetIp+(targetMac==null||targetMac.isEmpty()?"":" / "+targetMac)+".");
                handler.postDelayed(()->tryAutomation(),1800);
            } else {
                status.setText("WiFi Québec prépare "+(block?"le blocage":"le déblocage")+" de "+targetIp+" dans Contrôle d’accès Bell…");
                handler.postDelayed(()->tryAutomation(),1500);
            }
        });
    }

    private String js(String s){return s.replace("\\","\\\\").replace("'","\\'").replace("\n","");}
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(web!=null)web.destroy();super.onDestroy();}
}
