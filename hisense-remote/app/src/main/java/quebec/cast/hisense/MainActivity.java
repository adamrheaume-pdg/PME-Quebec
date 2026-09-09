package quebec.cast.hisense;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import org.json.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final String HISENSE_IP = "192.168.2.104";
    private static final String HISENSE_MAC = "e0:3e:cb:ea:88:08";
    private PyObject bridge;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private TextView status;
    private EditText ip, pin;
    private LinearLayout pairBox;

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private void row(LinearLayout root, Button... bs){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);for(Button b:bs)r.addView(b,new LinearLayout.LayoutParams(0,dp(56),1));root.addView(r);}
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setGravity(Gravity.CENTER);t.setTextColor(Color.rgb(20,40,70));t.setPadding(4,8,4,8);return t;}

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        bridge=Python.getInstance().getModule("bridge");
        ScrollView scroll=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(14),dp(14),dp(24)); scroll.addView(root);
        root.addView(label("Hisense 50A6KV",28)); root.addView(label("Télécommande + Cast",15));
        root.addView(label("Réseau connu : 192.168.2.104  •  MAC e0:3e:cb:ea:88:08",13));
        root.addView(label("Masque 255.255.255.0  •  Passerelle 192.168.2.1  •  DNS 1 192.168.2.1  •  DNS 2 207.164.234.193",11));
        ip=new EditText(this); ip.setHint("Adresse IP de la télé"); ip.setSingleLine(true); ip.setText(getPreferences(MODE_PRIVATE).getString("tv_ip",HISENSE_IP)); root.addView(ip,new LinearLayout.LayoutParams(-1,dp(54)));
        Button direct=button("Connexion directe 50A6KV"), discover=button("Redétecter"); row(root,direct,discover);
        Button wake=button("Allumer via réseau (Wake-on-LAN)"); root.addView(wake,new LinearLayout.LayoutParams(-1,dp(56)));
        status=label("Prêt — Hisense 50A6KV sur 192.168.2.104.",14); root.addView(status);
        pairBox=new LinearLayout(this); pairBox.setOrientation(LinearLayout.HORIZONTAL); pairBox.setVisibility(View.GONE); pin=new EditText(this); pin.setHint("PIN de la télé"); pin.setInputType(2); Button pair=button("Associer"); pairBox.addView(pin,new LinearLayout.LayoutParams(0,dp(54),1)); pairBox.addView(pair,new LinearLayout.LayoutParams(dp(120),dp(54))); root.addView(pairBox);
        root.addView(label("TÉLÉCOMMANDE",18));
        Button power=button("Power"),home=button("Home"),menu=button("Menu");row(root,power,home,menu);
        Button up=button("Haut");root.addView(up,new LinearLayout.LayoutParams(-1,dp(56)));
        Button left=button("Gauche"),ok=button("OK"),right=button("Droite");row(root,left,ok,right);
        Button down=button("Bas");root.addView(down,new LinearLayout.LayoutParams(-1,dp(56)));
        Button back=button("Retour"),mute=button("Muet"),info=button("Info");row(root,back,mute,info);
        Button vd=button("Vol -"),vu=button("Vol +"),cd=button("CH -"),cu=button("CH +");row(root,vd,vu,cd,cu);
        Button rew=button("Recul"),play=button("Lecture"),pause=button("Pause"),ff=button("Avance");row(root,rew,play,pause,ff);
        root.addView(label("ENTRÉES",18)); Button h1=button("HDMI 1"),h2=button("HDMI 2"),h3=button("HDMI 3");row(root,h1,h2,h3);
        root.addView(label("CAST",18)); Button cast=button("Ouvrir Cast Android"); root.addView(cast,new LinearLayout.LayoutParams(-1,dp(56)));
        setContentView(scroll);
        direct.setOnClickListener(v->{ip.setText(HISENSE_IP);connectTv();}); discover.setOnClickListener(v->discover()); pair.setOnClickListener(v->pair());
        wake.setOnClickListener(v->exec.submit(()->{try{boolean okWake=bridge.callAttr("wake",HISENSE_MAC).toBoolean();setStatus(okWake?"Signal d’allumage envoyé à la télé.":"Impossible d’envoyer le signal Wake-on-LAN.");}catch(Exception e){setStatus("Wake-on-LAN impossible.");}}));
        bind(power,"KEY_POWER");bind(home,"KEY_HOME");bind(menu,"KEY_MENU");bind(up,"KEY_UP");bind(down,"KEY_DOWN");bind(left,"KEY_LEFT");bind(right,"KEY_RIGHT");bind(ok,"KEY_OK");bind(back,"KEY_RETURNS");bind(mute,"KEY_MUTE");bind(info,"KEY_INFO");bind(vd,"KEY_VOLUMEDOWN");bind(vu,"KEY_VOLUMEUP");bind(cd,"KEY_CHANNELDOWN");bind(cu,"KEY_CHANNELUP");bind(rew,"KEY_BACK");bind(play,"KEY_PLAY");bind(pause,"KEY_PAUSE");bind(ff,"KEY_FORWARDS");
        source(h1,"hdmi1");source(h2,"hdmi2");source(h3,"hdmi3");
        cast.setOnClickListener(v->{try{startActivity(new Intent("android.settings.CAST_SETTINGS"));}catch(Exception e){startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));}});
    }
    private void setStatus(String s){runOnUiThread(()->status.setText(s));}
    private void discover(){setStatus("Recherche de la Hisense...");exec.submit(()->{try{JSONArray a=new JSONArray(bridge.callAttr("discover").toString());if(a.length()==0){runOnUiThread(()->ip.setText(HISENSE_IP));setStatus("Non détectée automatiquement. IP connue conservée : "+HISENSE_IP);return;}JSONObject d=a.getJSONObject(0);String host=d.getString("ip");runOnUiThread(()->ip.setText(host));setStatus("Trouvée : "+host);}catch(Exception e){runOnUiThread(()->ip.setText(HISENSE_IP));setStatus("Découverte impossible. IP connue conservée : "+HISENSE_IP);}});}
    private void connectTv(){String host=ip.getText().toString().trim();if(host.isEmpty())host=HISENSE_IP;final String target=host;getPreferences(MODE_PRIVATE).edit().putString("tv_ip",target).apply();setStatus("Connexion à "+target+"...");exec.submit(()->{try{JSONObject r=new JSONObject(bridge.callAttr("connect",target,getFilesDir().getAbsolutePath()).toString());setStatus(r.optString("message"));if(r.optBoolean("ok")&&!r.optBoolean("paired"))runOnUiThread(()->pairBox.setVisibility(View.VISIBLE));}catch(Exception e){setStatus("Erreur de connexion à "+target+" : "+e.getMessage());}});}
    private void pair(){String p=pin.getText().toString().trim();if(p.isEmpty())return;exec.submit(()->{try{JSONObject r=new JSONObject(bridge.callAttr("pair",p).toString());setStatus(r.optString("message"));if(r.optBoolean("ok"))runOnUiThread(()->pairBox.setVisibility(View.GONE));}catch(Exception e){setStatus("Erreur : "+e.getMessage());}});}
    private void bind(Button b,String code){b.setOnClickListener(v->exec.submit(()->{try{if(!bridge.callAttr("key",code).toBoolean())setStatus("Connecte la télé d’abord.");}catch(Exception e){setStatus("Commande impossible.");}}));}
    private void source(Button b,String src){b.setOnClickListener(v->exec.submit(()->{try{if(!bridge.callAttr("source",src).toBoolean())setStatus("Entrée impossible.");}catch(Exception e){setStatus("Entrée impossible.");}}));}
    @Override protected void onDestroy(){super.onDestroy();exec.shutdownNow();}
}
