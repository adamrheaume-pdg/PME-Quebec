package quebec.maison.universelle;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class DomotiqueActivity extends Activity {
    private LinearLayout devices;
    private TextView status;
    private ExecutorService pool;
    private final Set<String> shown = Collections.synchronizedSet(new HashSet<>());
    private final Map<String,Device> found = Collections.synchronizedMap(new LinkedHashMap<>());

    static class Device {
        String ip,name,kind,protocol,details;
        boolean controllable;
        Device(String ip,String name,String kind,String protocol,String details,boolean controllable){this.ip=ip;this.name=name;this.kind=kind;this.protocol=protocol;this.details=details;this.controllable=controllable;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(245,249,255));
        buildUi();
        scan();
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(245,249,255));root.setPadding(16,16,16,12);
        TextView title=t("🏠 Domotique — WiFi Québec",25,true);title.setTextColor(Color.rgb(0,67,160));root.addView(title);
        root.addView(t("Détection locale et commandes disponibles sur ton réseau",14,false));
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setPadding(0,12,0,8);
        Button scan=b("Scanner");scan.setOnClickListener(v->scan());bar.addView(scan,new LinearLayout.LayoutParams(0,-2,1));
        Button scenes=b("Tout éteindre");scenes.setOnClickListener(v->allOff());LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.setMargins(8,0,0,0);bar.addView(scenes,bp);root.addView(bar);
        status=t("Prêt",13,false);root.addView(status);
        ScrollView sv=new ScrollView(this);devices=new LinearLayout(this);devices.setOrientation(LinearLayout.VERTICAL);devices.setPadding(0,8,0,20);sv.addView(devices);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void scan(){
        devices.removeAllViews();shown.clear();found.clear();status.setText("Recherche des appareils domotiques…");
        Set<String> nets=subnets();if(nets.isEmpty()){status.setText("Aucun réseau local détecté.");return;}
        pool=Executors.newFixedThreadPool(36);final int total=nets.size()*254;final int[] done={0};
        for(String net:nets)for(int i=1;i<255;i++){final String ip=net+i;pool.execute(()->{Device d=identify(ip);if(d!=null){found.put(ip,d);runOnUiThread(()->addCard(d));}synchronized(done){done[0]++;if(done[0]==total)runOnUiThread(()->status.setText("Analyse terminée — "+found.size()+" appareil(s) domotique(s) ou compatible(s)."));}});}
        pool.shutdown();
    }

    private Device identify(String ip){
        if(!open(ip,80,90) && !open(ip,443,90)) return null;
        String s=get("http://"+ip+"/shelly",500);
        if(s!=null && s.contains("\"type\"") || s!=null && s.toLowerCase().contains("shelly")){
            String name="Shelly";try{JSONObject o=new JSONObject(s);name=o.optString("name",o.optString("type","Shelly"));}catch(Exception ignored){}
            return new Device(ip,name,"🔌 Appareil Shelly","Shelly local","API locale détectée",true);
        }
        String g2=get("http://"+ip+"/rpc/Shelly.GetDeviceInfo",500);
        if(g2!=null && g2.contains("\"id\"")) return new Device(ip,"Shelly","🔌 Appareil Shelly","Shelly RPC","Gen2/Gen3 local",true);
        String tas=get("http://"+ip+"/cm?cmnd=Status%200",500);
        if(tas!=null && tas.contains("Status")) return new Device(ip,"Tasmota","💡 Tasmota","Tasmota HTTP","Commande locale détectée",true);
        String root=get("http://"+ip+"/",450);
        if(root!=null){String low=root.toLowerCase(Locale.ROOT);if(low.contains("wemo")||low.contains("belkin"))return new Device(ip,"WeMo / Belkin","🔌 Prise / interrupteur","UPnP/HTTP","Interface locale détectée",false);if(low.contains("esphome"))return new Device(ip,"ESPHome","⚙️ ESPHome","HTTP local","Appareil détecté; contrôle natif à compléter selon entités",false);if(low.contains("tuya"))return new Device(ip,"Tuya compatible","💡 Tuya","HTTP local","Détecté; beaucoup de modèles exigent une clé locale",false);}
        return null;
    }

    private void addCard(Device d){if(shown.contains(d.ip))return;shown.add(d.ip);
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(18,16,18,16);c.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,12);c.setLayoutParams(cp);
        c.addView(t(d.kind+" — "+d.name,18,true));c.addView(t("IP : "+d.ip+"\nProtocole : "+d.protocol+"\n"+d.details,13,false));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button on=b("ON");Button off=b("OFF");on.setEnabled(d.controllable);off.setEnabled(d.controllable);on.setOnClickListener(v->command(d,true));off.setOnClickListener(v->command(d,false));row.addView(on,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);rp.setMargins(8,0,0,0);row.addView(off,rp);c.addView(row);
        LinearLayout r2=new LinearLayout(this);r2.setOrientation(LinearLayout.HORIZONTAL);Button timer=b("Minuterie");timer.setEnabled(d.controllable);timer.setOnClickListener(v->timerDialog(d));Button schedule=b("Horaire");schedule.setEnabled(d.controllable);schedule.setOnClickListener(v->scheduleDialog(d));r2.addView(timer,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-2,1);sp.setMargins(8,0,0,0);r2.addView(schedule,sp);c.addView(r2);
        if(!d.controllable)c.addView(t("Détecté — commande directe non disponible pour ce protocole sans configuration supplémentaire.",12,false));devices.addView(c);
    }

    private void command(Device d,boolean on){status.setText((on?"Activation":"Arrêt")+" de "+d.name+"…");Executors.newSingleThreadExecutor().execute(()->{boolean ok=send(d,on);runOnUiThread(()->status.setText(ok?"✓ Commande envoyée à "+d.name:"Échec de la commande — vérifie que l'appareil autorise le contrôle local."));});}
    static boolean send(Device d,boolean on){try{String url;if(d.protocol.startsWith("Tasmota"))url="http://"+d.ip+"/cm?cmnd=Power%20"+(on?"On":"Off");else if(d.protocol.contains("RPC"))url="http://"+d.ip+"/rpc/Switch.Set?id=0&on="+(on?"true":"false");else url="http://"+d.ip+"/relay/0?turn="+(on?"on":"off");HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(1000);c.setReadTimeout(1200);c.setRequestMethod("GET");int code=c.getResponseCode();c.disconnect();return code>=200&&code<400;}catch(Exception e){return false;}}

    private void timerDialog(Device d){final String[] opts={"1 minute","5 minutes","15 minutes","30 minutes","60 minutes"};final int[] mins={1,5,15,30,60};new AlertDialog.Builder(this).setTitle("Éteindre automatiquement").setItems(opts,(x,which)->scheduleAfter(d,false,mins[which])).setNegativeButton("Annuler",null).show();}
    private void scheduleAfter(Device d,boolean on,int minutes){long when=System.currentTimeMillis()+minutes*60000L;scheduleAlarm(d,on,when);Toast.makeText(this,"Programmé dans "+minutes+" min",Toast.LENGTH_SHORT).show();}
    private void scheduleDialog(Device d){TimePicker tp=new TimePicker(this);tp.setIs24HourView(true);new AlertDialog.Builder(this).setTitle("Horaire quotidien — choisir l'heure").setView(tp).setPositiveButton("ON",(x,w)->scheduleClock(d,true,tp.getHour(),tp.getMinute())).setNeutralButton("OFF",(x,w)->scheduleClock(d,false,tp.getHour(),tp.getMinute())).setNegativeButton("Annuler",null).show();}
    private void scheduleClock(Device d,boolean on,int h,int m){Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);scheduleAlarm(d,on,c.getTimeInMillis());Toast.makeText(this,"Horaire enregistré : "+String.format(Locale.CANADA_FRENCH,"%02d:%02d",h,m),Toast.LENGTH_SHORT).show();}
    private void scheduleAlarm(Device d,boolean on,long when){Intent i=new Intent(this,AutomationReceiver.class);i.putExtra("ip",d.ip);i.putExtra("protocol",d.protocol);i.putExtra("name",d.name);i.putExtra("on",on);int id=(d.ip+d.protocol+on+when).hashCode();PendingIntent pi=PendingIntent.getBroadcast(this,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.set(AlarmManager.RTC_WAKEUP,when,pi);}
    private void allOff(){for(Device d:new ArrayList<>(found.values()))if(d.controllable)Executors.newSingleThreadExecutor().execute(()->send(d,false));Toast.makeText(this,"Commande Tout éteindre envoyée",Toast.LENGTH_SHORT).show();}

    private Set<String> subnets(){Set<String> out=new LinkedHashSet<>();try{Enumeration<NetworkInterface> e=NetworkInterface.getNetworkInterfaces();while(e.hasMoreElements()){NetworkInterface n=e.nextElement();if(!n.isUp()||n.isLoopback())continue;Enumeration<InetAddress>a=n.getInetAddresses();while(a.hasMoreElements()){InetAddress x=a.nextElement();if(x instanceof Inet4Address&&x.isSiteLocalAddress()){String ip=x.getHostAddress();out.add(ip.substring(0,ip.lastIndexOf('.')+1));}}}}catch(Exception ignored){}return out;}
    private boolean open(String ip,int port,int timeout){try(Socket s=new Socket()){s.connect(new InetSocketAddress(ip,port),timeout);return true;}catch(Exception e){return false;}}
    private String get(String u,int timeout){HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(timeout);c.setReadTimeout(timeout);c.setRequestProperty("User-Agent","WiFi-Quebec/1.14");InputStream in=c.getInputStream();ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]buf=new byte[2048];int n,total=0;while((n=in.read(buf))>0&&total<8192){o.write(buf,0,n);total+=n;}return o.toString("UTF-8");}catch(Exception e){return null;}finally{if(c!=null)c.disconnect();}}
    private TextView t(String s,int size,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.rgb(25,40,58));v.setPadding(4,4,4,4);if(bold)v.setTypeface(null,1);return v;}
    private Button b(String s){Button x=new Button(this);x.setText(s);x.setAllCaps(false);return x;}
}
