package quebec.maison.universelle;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.webkit.*;
import android.widget.*;
import android.view.*;
import android.content.*;
import android.text.InputType;

public class BellRouterActivity extends Activity {
    private WebView web;
    private TextView status;
    private EditText password;
    private String targetIp, targetMac, gateway;
    private boolean block;
    private final Handler handler=new Handler();
    private int attempts=0;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        Intent i=getIntent();
        targetIp=i.getStringExtra("target_ip");
        targetMac=i.getStringExtra("target_mac");
        gateway=i.getStringExtra("gateway");
        block=i.getBooleanExtra("block",true);
        if(gateway==null||gateway.isEmpty())gateway="192.168.2.1";
        if(targetIp==null)targetIp="";
        if(targetMac==null)targetMac="";

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title=new TextView(this);
        title.setText("⚜ Borne Giga directe");
        title.setTextSize(24);
        title.setTypeface(null,1);
        title.setTextColor(Color.rgb(0,63,151));
        title.setPadding(18,18,18,8);
        root.addView(title,new LinearLayout.LayoutParams(-1,-2));

        status=new TextView(this);
        status.setTextSize(14);
        status.setTextColor(Color.rgb(16,33,58));
        status.setPadding(18,6,18,12);
        status.setText(targetIp.isEmpty()?"Connexion locale à la Borne Giga 2.0. Le mot de passe reste sur ce téléphone.":(block?"Blocage":"Déblocage")+" demandé pour "+targetIp+(targetMac.isEmpty()?"":" • "+targetMac));
        root.addView(status,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout loginRow=new LinearLayout(this);
        loginRow.setOrientation(LinearLayout.HORIZONTAL);
        loginRow.setPadding(14,0,14,8);
        password=new EditText(this);
        password.setHint("Mot de passe administrateur de la borne");
        password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        loginRow.addView(password,new LinearLayout.LayoutParams(0,-2,1));
        Button connect=new Button(this);
        connect.setText("Connexion");
        connect.setOnClickListener(v->startLocalLogin());
        loginRow.addView(connect,new LinearLayout.LayoutParams(-2,-2));
        root.addView(loginRow,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout actions=new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(14,0,14,8);
        Button retry=new Button(this);
        retry.setText("Réessayer direct");
        retry.setOnClickListener(v->{attempts=0;tryDirectControl();});
        actions.addView(retry,new LinearLayout.LayoutParams(0,-2,1));
        Button bellApp=new Button(this);
        bellApp.setText("Bell Wi-Fi");
        bellApp.setOnClickListener(v->openBellWifi());
        actions.addView(bellApp,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(actions,new LinearLayout.LayoutParams(-1,-2));

        web=new WebView(this);
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(s.getUserAgentString()+" WiFiQuebec/1.5");
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String url){
                super.onPageFinished(v,url);
                handler.postDelayed(()->{if(password.getText().length()>0)autoFillLogin();else tryDirectControl();},600);
            }
        });
        web.loadUrl("http://"+gateway+"/");
    }

    private void startLocalLogin(){
        if(password.getText().toString().isEmpty()){
            Toast.makeText(this,"Entre le mot de passe administrateur de la borne.",Toast.LENGTH_SHORT).show();
            return;
        }
        status.setText("Connexion locale à "+gateway+"…");
        autoFillLogin();
    }

    private void autoFillLogin(){
        String p=js(password.getText().toString());
        String script="(function(){"+
            "var pass=document.querySelector('input[type=password],input[name*=pass i],input[id*=pass i]');"+
            "if(!pass)return 'NO_PASSWORD_FIELD';"+
            "pass.focus();pass.value='"+p+"';"+
            "pass.dispatchEvent(new Event('input',{bubbles:true}));pass.dispatchEvent(new Event('change',{bubbles:true}));"+
            "var all=[].slice.call(document.querySelectorAll('button,input[type=submit],a,[role=button]'));"+
            "var b=all.find(function(e){var t=(e.innerText||e.value||e.textContent||'').toLowerCase();return t.indexOf('ouvrir une session')>=0||t.indexOf('login')>=0||t.indexOf('connexion')>=0;});"+
            "if(b){b.click();return 'LOGIN_SENT';}"+
            "if(pass.form){pass.form.submit();return 'LOGIN_SENT';}return 'NO_LOGIN_BUTTON';"+
        "})();";
        web.evaluateJavascript(script,v->{
            String r=v==null?"":v.replace("\"","");
            if(r.contains("LOGIN_SENT")){
                status.setText("Authentification envoyée localement. Recherche du contrôle d’accès…");
                handler.postDelayed(()->tryDirectControl(),1600);
            }else{
                status.setText("Champ de connexion Bell non détecté automatiquement. Tu peux te connecter dans la page ci-dessous.");
            }
        });
    }

    private void tryDirectControl(){
        if(web==null||attempts++>10)return;
        String ip=js(targetIp);
        String mac=js(targetMac);
        String mode=block?"block":"unblock";
        String script="(function(){"+
            "function n(x){return (x||'').toLowerCase().replace(/\\s+/g,' ').trim();}"+
            "function t(e){return n(e.innerText||e.textContent||e.value||e.getAttribute('aria-label')||'');}"+
            "var body=n(document.body&&document.body.innerText);"+
            "if(body.indexOf('mot de passe')>=0||body.indexOf('password')>=0)return 'LOGIN';"+
            "var all=[].slice.call(document.querySelectorAll('a,button,input,[role=button],label,div,span'));"+
            "var target='"+mac+"';if(!target)target='"+ip+"';"+
            "var node=all.find(function(e){return target&&t(e).indexOf(target.toLowerCase())>=0;});"+
            "var access=all.find(function(e){var x=t(e);return x.indexOf('contrôle d’accès')>=0||x.indexOf('controle d acces')>=0||x.indexOf('access control')>=0||x.indexOf('bloquer')>=0||x.indexOf('block')>=0;});"+
            "if(!access&&!node)return 'NO_LOCAL_CONTROL';"+
            "if(access){try{access.click();}catch(e){}}"+
            "if(!target)return 'ROUTER_CONNECTED';"+
            "if(!node)return 'TARGET_NOT_FOUND';"+
            "var row=node;for(var k=0;k<6&&row;k++,row=row.parentElement){if(row.querySelector&&row.querySelector('input[type=checkbox],input[type=radio],button,[role=switch]'))break;}"+
            "if(!row)return 'TARGET_FOUND';"+
            "var c=row.querySelector('input[type=checkbox],input[type=radio],[role=switch],button');"+
            "if(!c)return 'TARGET_FOUND';"+
            "var checked=!!(c.checked||c.getAttribute('aria-checked')==='true'||c.classList.contains('active'));"+
            "if(('"+mode+"'==='block'&&!checked)||('"+mode+"'==='unblock'&&checked)){try{c.click();}catch(e){}}"+
            "var save=all.find(function(e){var x=t(e);return x==='enregistrer'||x==='save'||x==='appliquer'||x==='apply';});"+
            "if(save){try{save.click();return 'ACTION_SENT';}catch(e){}}"+
            "return 'TARGET_FOUND';"+
        "})();";
        web.evaluateJavascript(script,value->{
            String r=value==null?"":value.replace("\"","");
            if(r.contains("LOGIN"))status.setText("Connexion requise. Entre le mot de passe administrateur ci-dessus.");
            else if(r.contains("ACTION_SENT"))status.setText((block?"Blocage":"Déblocage")+" envoyé par l’interface locale. Vérifie que l’appareil change bien d’état.");
            else if(r.contains("NO_LOCAL_CONTROL"))status.setText("Connexion locale réussie, mais ce firmware Bell ne montre pas le contrôle d’accès dans l’interface Web. Utilise Bell Wi‑Fi pour cette action.");
            else if(r.contains("TARGET_NOT_FOUND"))status.setText("Borne accessible, mais l’appareil cible n’est pas visible dans cette page locale.");
            else if(r.contains("ROUTER_CONNECTED"))status.setText("Borne Giga accessible localement.");
            else{status.setText("Borne accessible. Recherche du contrôle direct…");handler.postDelayed(()->tryDirectControl(),1500);}
        });
    }

    private void openBellWifi(){
        final String pkg="com.plumewifi.plume.iguana";
        try{Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);if(launch!=null){startActivity(launch);return;}}catch(Exception ignored){}
        try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("market://details?id="+pkg)));}
        catch(Exception e){startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://play.google.com/store/apps/details?id="+pkg)));}
    }

    private String js(String s){return (s==null?"":s).replace("\\","\\\\").replace("'","\\'").replace("\n","");}
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(web!=null)web.destroy();super.onDestroy();}
}
