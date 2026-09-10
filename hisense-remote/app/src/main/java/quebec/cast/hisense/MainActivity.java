package quebec.cast.hisense;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import org.json.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private static final String IP="192.168.2.104", MAC="e0:3e:cb:ea:88:08"; private static final int PICK=4401;
 private final ExecutorService exec=Executors.newSingleThreadExecutor(); private PyObject bridge; private DirectDlnaCaster dlna; private TextView status,ping; private EditText pin; private LinearLayout pairBox; private float sx,sy; private Vibrator vib;
 private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} private GradientDrawable bg(int stroke,int fill,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
 private TextView text(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.rgb(220,245,255));t.setGravity(Gravity.CENTER);t.setPadding(dp(6),dp(8),dp(6),dp(8));return t;}
 private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackground(bg(Color.rgb(0,220,255),Color.rgb(18,27,35),16));b.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){v.setAlpha(.65f);haptic();}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)v.setAlpha(1);return false;});return b;}
 private void row(LinearLayout r,Button...bs){LinearLayout x=new LinearLayout(this);x.setPadding(0,dp(4),0,dp(4));for(Button b:bs){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(58),1);p.setMargins(dp(3),0,dp(3),0);x.addView(b,p);}r.addView(x);} private void haptic(){if(vib!=null&&android.os.Build.VERSION.SDK_INT>=26)vib.vibrate(VibrationEffect.createOneShot(18,80));}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);vib=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);bridge=Python.getInstance().getModule("bridge");dlna=new DirectDlnaCaster(this,IP,this::setStatus);
  ScrollView sv=new ScrollView(this);sv.setBackgroundColor(Color.BLACK);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(16),dp(14),dp(28));sv.addView(root);
  TextView title=text("HISENSE // 50A6KV",25);title.setTextColor(Color.rgb(0,235,255));root.addView(title);root.addView(text("CYBER REMOTE • DIRECT CAST",12));
  LinearLayout tele=new LinearLayout(this);tele.setOrientation(LinearLayout.VERTICAL);tele.setPadding(dp(10),dp(8),dp(10),dp(8));tele.setBackground(bg(Color.rgb(0,220,255),Color.argb(90,18,32,40),18));status=text("● Liaison : prête  •  IP "+IP,13);status.setTextColor(Color.rgb(255,145,55));ping=text("PING -- ms     ▂▃▅▇  VOL",13);tele.addView(status);tele.addView(ping);root.addView(tele);
  Button connect=btn("LIAISON VIDAA"),wake=btn("POWER NET");row(root,connect,wake);pairBox=new LinearLayout(this);pin=new EditText(this);pin.setHint("PIN TV");pin.setTextColor(Color.WHITE);pin.setHintTextColor(Color.GRAY);Button pair=btn("ASSOCIER");pairBox.addView(pin,new LinearLayout.LayoutParams(0,dp(55),1));pairBox.addView(pair,new LinearLayout.LayoutParams(dp(125),dp(55)));pairBox.setVisibility(View.GONE);root.addView(pairBox);
  root.addView(text("NAVIGATION HOLOGRAPHIQUE",16));TextView pad=text("✦\nGLISSER POUR NAVIGUER\nDOUBLE TAP = OK",18);pad.setTextColor(Color.rgb(0,235,255));pad.setBackground(bg(Color.rgb(0,220,255),Color.rgb(7,18,25),34));root.addView(pad,new LinearLayout.LayoutParams(-1,dp(230))); final long[] last={0};pad.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){sx=e.getX();sy=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-sx,dy=e.getY()-sy;if(Math.abs(dx)>45||Math.abs(dy)>45){key(Math.abs(dx)>Math.abs(dy)?(dx>0?"KEY_RIGHT":"KEY_LEFT"):(dy>0?"KEY_DOWN":"KEY_UP"));}else{long n=System.currentTimeMillis();if(n-last[0]<350)key("KEY_OK");last[0]=n;}haptic();return true;}return true;});
  Button back=btn("↶ RETOUR"),home=btn("⌂ HOME"),menu=btn("☰ MENU");row(root,back,home,menu);Button vd=btn("VOL −"),mute=btn("MUET"),vu=btn("VOL +");row(root,vd,mute,vu);
  root.addView(text("SOURCES / CAST DIRECT",16));Button h1=btn("HDMI 1"),h2=btn("HDMI 2"),cast=btn("CAST");row(root,h1,h2,cast);Button play=btn("▶"),pause=btn("Ⅱ"),stop=btn("■");row(root,play,pause,stop);
  Button cinema=btn("MODE CINÉMA — macro Home + HDMI 1");root.addView(cinema,new LinearLayout.LayoutParams(-1,dp(60)));TextView note=text("Automatisation rapide • retour haptique • interface AMOLED. Mode pointeur gyroscopique disponible quand VIDAA expose un curseur réseau compatible.",11);note.setTextColor(Color.LTGRAY);root.addView(note);setContentView(sv);
  connect.setOnClickListener(v->connect());wake.setOnClickListener(v->exec.submit(()->{try{bridge.callAttr("wake",MAC);setStatus("Signal POWER NET envoyé");}catch(Exception e){setStatus("Power réseau indisponible");}}));pair.setOnClickListener(v->pair());bind(back,"KEY_RETURNS");bind(home,"KEY_HOME");bind(menu,"KEY_MENU");bind(vd,"KEY_VOLUMEDOWN");bind(vu,"KEY_VOLUMEUP");bind(mute,"KEY_MUTE");source(h1,"hdmi1");source(h2,"hdmi2");cast.setOnClickListener(v->pick());play.setOnClickListener(v->exec.submit(()->{try{dlna.play();}catch(Exception e){setStatus("Lecture indisponible");}}));pause.setOnClickListener(v->exec.submit(()->{try{dlna.pause();}catch(Exception e){setStatus("Pause indisponible");}}));stop.setOnClickListener(v->exec.submit(()->{try{dlna.stopPlayback();}catch(Exception e){setStatus("Arrêt indisponible");}}));cinema.setOnClickListener(v->exec.submit(()->{try{bridge.callAttr("key","KEY_HOME");Thread.sleep(500);bridge.callAttr("source","hdmi1");setStatus("Macro Mode Cinéma envoyée");}catch(Exception e){setStatus("Connecte d’abord la télécommande");}}));ping(); }
 private void ping(){exec.submit(()->{try{long a=System.nanoTime();java.net.InetAddress.getByName(IP).isReachable(900);long ms=(System.nanoTime()-a)/1000000;runOnUiThread(()->ping.setText("PING "+ms+" ms     ▂▃▅▇  VOL"));}catch(Exception ignored){}});}
 private void connect(){setStatus("Connexion VIDAA…");exec.submit(()->{try{JSONObject r=new JSONObject(bridge.callAttr("connect",IP,getFilesDir().getAbsolutePath()).toString());setStatus(r.optString("message"));if(r.optBoolean("ok")&&!r.optBoolean("paired"))runOnUiThread(()->pairBox.setVisibility(View.VISIBLE));}catch(Exception e){setStatus("VIDAA refuse encore la liaison — Cast direct reste indépendant");}});}
 private void pair(){String p=pin.getText().toString().trim();if(p.isEmpty())return;exec.submit(()->{try{JSONObject r=new JSONObject(bridge.callAttr("pair",p).toString());setStatus(r.optString("message"));if(r.optBoolean("ok"))runOnUiThread(()->pairBox.setVisibility(View.GONE));}catch(Exception e){setStatus("Association refusée");}});}
 private void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","audio/*","image/*"});startActivityForResult(i,PICK);}
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK&&c==RESULT_OK&&d!=null&&d.getData()!=null){Uri u=d.getData();exec.submit(()->{try{dlna.cast(u);}catch(Exception e){setStatus("Cast: "+e.getMessage());}});}}
 private void setStatus(String s){runOnUiThread(()->status.setText("● "+s));}private void key(String k){exec.submit(()->{try{bridge.callAttr("key",k);}catch(Exception e){setStatus("Commande VIDAA non liée");}});}private void bind(Button b,String k){b.setOnClickListener(v->key(k));}private void source(Button b,String s){b.setOnClickListener(v->exec.submit(()->{try{bridge.callAttr("source",s);}catch(Exception e){setStatus("Source indisponible");}}));}
 @Override protected void onDestroy(){super.onDestroy();exec.shutdownNow();}
}
