package quebec.detecteur360;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.*;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.*;
import android.net.*;
import android.net.wifi.*;
import android.nfc.NfcAdapter;
import android.os.*;
import android.telephony.TelephonyManager;
import android.view.*;
import android.widget.*;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity implements SensorEventListener {
    private static final int NAVY = Color.rgb(2,12,42), BLUE = Color.rgb(0,31,151), CYAN = Color.rgb(0,190,255);
    private SensorManager sm; private LocationManager lm; private WifiManager wm; private BluetoothAdapter ba;
    private LinearLayout content; private final Map<Integer,TextView> sensorViews=new HashMap<>();
    private TextView wifiView,btView,gpsView,lanView,capView,cellView,usbView,compassView,seismoState,seismoStats,historyView;
    private SeismoView seismoView; private SeekBar quakeThreshold; private float quakeLimit=.18f; private float maxQuake=0, sumQuake=0; private long quakeSamples=0,lastEvent=0;
    private float[] accel=null,mag=null; private float levelZeroRoll=0,levelZeroPitch=0;
    private final Handler main=new Handler(Looper.getMainLooper()); private final Set<String> btFound=Collections.synchronizedSet(new LinkedHashSet<>());
    private GnssStatus.Callback gnssCb; private int sats=0; private SharedPreferences prefs;

    private final BroadcastReceiver btReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
        if(BluetoothDevice.ACTION_FOUND.equals(i.getAction())){ BluetoothDevice d=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); if(d==null)return;
            short rssi=i.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE); String name="Appareil Bluetooth";
            try{ if(can(Manifest.permission.BLUETOOTH_CONNECT)){String n=d.getName(); if(n!=null&&!n.isBlank())name=n;} }catch(Exception ignored){}
            btFound.add(name+" • "+d.getAddress()+" • "+rssi+" dBm"); updateBluetooth(); }
    }};

    @Override protected void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
        sm=(SensorManager)getSystemService(SENSOR_SERVICE); lm=(LocationManager)getSystemService(LOCATION_SERVICE); wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE); ba=BluetoothAdapter.getDefaultAdapter(); prefs=getSharedPreferences("d360",MODE_PRIVATE);
        buildUi(); requestPermissionsNow(); registerSensors(); registerBt(); refreshAll(); }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(NAVY);scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((v,in)->{v.setPadding(0,in.getSystemWindowInsetTop(),0,in.getSystemWindowInsetBottom());return in;});
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(14),dp(14),dp(14),dp(34));scroll.addView(content);
        ImageView logo=new ImageView(this); logo.setImageResource(getResources().getIdentifier("detecteur_logo","drawable",getPackageName())); logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(dp(124),dp(124));ilp.gravity=Gravity.CENTER_HORIZONTAL;content.addView(logo,ilp);
        TextView title=text("DÉTECTEUR 360 QUÉBEC",27,Color.WHITE,true);title.setGravity(Gravity.CENTER);content.addView(title);
        TextView sub=text("Centre multisenseur • diagnostic • vibrations • réseaux",13,Color.rgb(190,220,255),false);sub.setGravity(Gravity.CENTER);sub.setPadding(0,dp(4),0,dp(14));content.addView(sub);
        LinearLayout top=new LinearLayout(this); Button scan=button("SCAN 360");Button cal=button("CALIBRER");top.addView(scan,new LinearLayout.LayoutParams(0,dp(50),1));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(50),1);cp.setMargins(dp(8),0,0,0);top.addView(cal,cp);content.addView(top);
        scan.setOnClickListener(v->refreshAll());cal.setOnClickListener(v->calibrate());

        addHeader("SÉISME / VIBRATIONS");
        LinearLayout qbox=box(); seismoState=text("STABLE",22,Color.WHITE,true);qbox.addView(seismoState);seismoStats=text("Pose le téléphone immobile sur une surface solide.",13,Color.rgb(210,230,255),false);qbox.addView(seismoStats);
        seismoView=new SeismoView(this);qbox.addView(seismoView,new LinearLayout.LayoutParams(-1,dp(150)));
        TextView qlabel=text("Seuil d'alerte",12,Color.rgb(180,215,250),true);qbox.addView(qlabel); quakeThreshold=new SeekBar(this);quakeThreshold.setMax(95);quakeThreshold.setProgress(13);qbox.addView(quakeThreshold);
        quakeThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){quakeLimit=.05f+p*.01f;qlabel.setText(String.format(Locale.CANADA_FRENCH,"Seuil d'alerte : %.2f m/s²",quakeLimit));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        historyView=addCard("Historique des secousses",prefs.getString("quake_history","Aucun événement enregistré."));Button clear=button("EFFACER HISTORIQUE");content.addView(clear,new LinearLayout.LayoutParams(-1,dp(46)));clear.setOnClickListener(v->{prefs.edit().remove("quake_history").apply();historyView.setText("Aucun événement enregistré.");});
        addCard("Comment l'interpréter?","Le téléphone mesure les accélérations. Une secousse peut venir d'un séisme, d'un camion, d'une porte ou d'un contact avec le téléphone. Détecteur 360 signale une vibration locale; il ne confirme pas à lui seul un tremblement de terre.");

        addHeader("MESURES EN DIRECT");
        addSensor(Sensor.TYPE_MAGNETIC_FIELD,"Champ magnétique","µT • total + axes X/Y/Z");addSensor(Sensor.TYPE_LIGHT,"Lumière","lux");addSensor(Sensor.TYPE_PRESSURE,"Pression atmosphérique","hPa + altitude barométrique estimée");addSensor(Sensor.TYPE_PROXIMITY,"Proximité","distance exposée par le capteur");addSensor(Sensor.TYPE_AMBIENT_TEMPERATURE,"Température ambiante","°C si capteur physique présent");addSensor(Sensor.TYPE_RELATIVE_HUMIDITY,"Humidité","% si capteur présent");addSensor(Sensor.TYPE_ACCELEROMETER,"Niveau / accélération","inclinaison + m/s²");addSensor(Sensor.TYPE_GYROSCOPE,"Gyroscope","rad/s");addSensor(Sensor.TYPE_GRAVITY,"Gravité","m/s²");addSensor(Sensor.TYPE_LINEAR_ACCELERATION,"Accélération linéaire","m/s² sans gravité");addSensor(Sensor.TYPE_ROTATION_VECTOR,"Rotation 3D","vecteur d'orientation");addSensor(Sensor.TYPE_STEP_COUNTER,"Compteur de pas","si exposé par Android");compassView=addCard("Boussole 360°","En attente du magnétomètre + accéléromètre…");

        addHeader("RÉSEAUX / RADIO / POSITION"); wifiView=addCard("Analyse Wi‑Fi","En attente…");btView=addCard("Bluetooth","En attente…");cellView=addCard("Réseau cellulaire","En attente…");gpsView=addCard("GPS / GNSS","En attente…");lanView=addCard("Réseau local","Appuie sur SCAN 360 pour actualiser, ou lance le balayage ci-dessous.");Button lan=button("BALAYER LE RÉSEAU LOCAL");content.addView(lan,new LinearLayout.LayoutParams(-1,dp(46)));lan.setOnClickListener(v->scanLan());
        addHeader("MATÉRIEL / CAPTEURS EXTERNES");usbView=addCard("USB / capteurs externes","Recherche de périphériques USB connectés…");capView=addCard("Capacités du téléphone","Inventaire automatique des capteurs Android…");
        addCard("Limites physiques","Une application seule ne peut pas créer une caméra thermique, un compteur Geiger, un détecteur de gaz ou un analyseur RF large bande. Ces fonctions exigent un capteur externe compatible. Détecteur 360 n'affiche que des mesures réellement accessibles au téléphone.");
        TextView foot=text("⚜ Créé au Québec • Détecteur 360 Québec v4",12,Color.rgb(165,205,245),false);foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(18),0,0);content.addView(foot);setContentView(scroll);
    }

    private LinearLayout box(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(15),dp(13),dp(15),dp(13));GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(4,20,58),Color.rgb(7,42,95),Color.rgb(3,17,51)});g.setCornerRadius(dp(16));g.setStroke(dp(1),Color.rgb(20,115,235));b.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));content.addView(b,p);return b;}
    private TextView addCard(String t,String val){LinearLayout b=box();b.addView(text(t,16,Color.WHITE,true));TextView v=text(val,13,Color.rgb(214,228,250),false);v.setPadding(0,dp(5),0,0);b.addView(v);return v;}
    private void addSensor(int type,String title,String unit){TextView v=addCard(title,unit+"\nEn attente de mesure…");sensorViews.put(type,v);}
    private void addHeader(String s){TextView v=text(s,13,CYAN,true);v.setPadding(dp(2),dp(19),0,dp(7));content.addView(v);} private TextView text(String s,float z,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);GradientDrawable g=new GradientDrawable();g.setColor(BLUE);g.setCornerRadius(dp(13));g.setStroke(dp(1),CYAN);b.setBackground(g);return b;}

    private void registerSensors(){for(Integer type:sensorViews.keySet()){Sensor s=sm.getDefaultSensor(type);if(s!=null)sm.registerListener(this,s,SensorManager.SENSOR_DELAY_GAME);else sensorViews.get(type).setText("Capteur physique non présent sur cet appareil");}}
    @Override public void onSensorChanged(SensorEvent e){float[] x=e.values;TextView v=sensorViews.get(e.sensor.getType());
        switch(e.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER: accel=x.clone();double roll=Math.toDegrees(Math.atan2(x[1],x[2]))-levelZeroRoll;double pitch=Math.toDegrees(Math.atan2(-x[0],Math.sqrt(x[1]*x[1]+x[2]*x[2])))-levelZeroPitch;if(v!=null)v.setText(String.format(Locale.CANADA_FRENCH,"Roulis %.1f° • Tangage %.1f°\nX %.2f • Y %.2f • Z %.2f m/s²",roll,pitch,x[0],x[1],x[2]));processQuake(x);updateCompass();break;
            case Sensor.TYPE_MAGNETIC_FIELD: mag=x.clone();double total=Math.sqrt(x[0]*x[0]+x[1]*x[1]+x[2]*x[2]);if(v!=null)v.setText(String.format(Locale.CANADA_FRENCH,"%.1f µT total\nX %.1f • Y %.1f • Z %.1f µT",total,x[0],x[1],x[2]));updateCompass();break;
            case Sensor.TYPE_LIGHT:if(v!=null)v.setText(String.format(Locale.CANADA_FRENCH,"%.1f lux",x[0]));break;
            case Sensor.TYPE_PRESSURE:if(v!=null){double alt=44330.0*(1-Math.pow(x[0]/1013.25,0.1903));v.setText(String.format(Locale.CANADA_FRENCH,"%.2f hPa\nAltitude barométrique ≈ %.0f m",x[0],alt));}break;
            case Sensor.TYPE_PROXIMITY:if(v!=null)v.setText(String.format(Locale.CANADA_FRENCH,"%.2f cm",x[0]));break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:if(v!=null)v.setText(String.format(Locale.CANADA_FRENCH,"%.1f °C",x[0]));break;
            case Sensor.TYPE_RELATIVE_HUMIDITY:if(v!=null)v.setText(String.format(Locale.CANADA_FRENCH,"%.1f %%",x[0]));break;
            case Sensor.TYPE_GYROSCOPE:if(v!=null)v.setText(String.format(Locale.CANADA_FRENCH,"X %.3f • Y %.3f • Z %.3f rad/s",x[0],x[1],x[2]));break;
            case Sensor.TYPE_GRAVITY:case Sensor.TYPE_LINEAR_ACCELERATION:if(v!=null)v.setText(String.format(Locale.CANADA_FRENCH,"X %.3f • Y %.3f • Z %.3f m/s²",x[0],x[1],x[2]));break;
            case Sensor.TYPE_ROTATION_VECTOR:if(v!=null){StringBuilder s=new StringBuilder();for(int i=0;i<x.length;i++){if(i>0)s.append(" • ");s.append("V").append(i).append(" ").append(String.format(Locale.CANADA_FRENCH,"%.4f",x[i]));}v.setText(s.toString());}break;
            case Sensor.TYPE_STEP_COUNTER:if(v!=null)v.setText(String.format(Locale.CANADA_FRENCH,"%.0f pas depuis le redémarrage",x[0]));break;
        }}
    @Override public void onAccuracyChanged(Sensor s,int a){}

    private void processQuake(float[] x){double m=Math.sqrt(x[0]*x[0]+x[1]*x[1]+x[2]*x[2]);float vibration=(float)Math.abs(m-SensorManager.GRAVITY_EARTH);maxQuake=Math.max(maxQuake,vibration);sumQuake+=vibration;quakeSamples++;if(seismoView!=null)seismoView.add(vibration,quakeLimit);
        String state=vibration<.04?"STABLE":vibration<.10?"VIBRATIONS FAIBLES":vibration<.25?"SECOUSSE NOTABLE":"FORTE VIBRATION";seismoState.setText(state);seismoStats.setText(String.format(Locale.CANADA_FRENCH,"Instantané %.3f m/s² • pic %.3f • moyenne %.3f\nSeuil %.2f m/s²",vibration,maxQuake,sumQuake/Math.max(1,quakeSamples),quakeLimit));
        long now=System.currentTimeMillis();if(vibration>=quakeLimit&&now-lastEvent>10000){lastEvent=now;recordQuake(vibration);Toast.makeText(this,"Secousse locale détectée — événement sismique à confirmer",Toast.LENGTH_LONG).show();}}
    private void recordQuake(float v){String stamp=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CANADA_FRENCH).format(new Date());String old=prefs.getString("quake_history","");String line="• "+stamp+" — pic "+String.format(Locale.CANADA_FRENCH,"%.3f m/s²",v);String n=line+(old.isBlank()?"":"\n"+old);String[] rows=n.split("\n");if(rows.length>40)n=String.join("\n",Arrays.copyOf(rows,40));prefs.edit().putString("quake_history",n).apply();historyView.setText(n);}
    private void updateCompass(){if(accel==null||mag==null||compassView==null)return;float[] R=new float[9],I=new float[9];if(SensorManager.getRotationMatrix(R,I,accel,mag)){float[] o=new float[3];SensorManager.getOrientation(R,o);float deg=(float)Math.toDegrees(o[0]);if(deg<0)deg+=360;String dir=deg<22.5||deg>=337.5?"N":deg<67.5?"NE":deg<112.5?"E":deg<157.5?"SE":deg<202.5?"S":deg<247.5?"SO":deg<292.5?"O":"NO";compassView.setText(String.format(Locale.CANADA_FRENCH,"%.0f° • %s",deg,dir));}}
    private void calibrate(){if(accel!=null){levelZeroRoll=(float)Math.toDegrees(Math.atan2(accel[1],accel[2]));levelZeroPitch=(float)Math.toDegrees(Math.atan2(-accel[0],Math.sqrt(accel[1]*accel[1]+accel[2]*accel[2])));}maxQuake=0;sumQuake=0;quakeSamples=0;if(seismoView!=null)seismoView.clear();Toast.makeText(this,"Surface calibrée",Toast.LENGTH_SHORT).show();}

    private void refreshAll(){refreshWifi();startBtScan();refreshLocation();refreshCapabilities();refreshCell();refreshUsb();}
    private void refreshWifi(){try{WifiInfo info=wm.getConnectionInfo();StringBuilder s=new StringBuilder();s.append("Connexion: ").append(info.getRssi()).append(" dBm • ").append(info.getLinkSpeed()).append(" Mb/s • ").append(info.getFrequency()).append(" MHz");List<ScanResult> rs=wm.getScanResults();if(can(Manifest.permission.ACCESS_FINE_LOCATION))try{wm.startScan();}catch(Exception ignored){}s.append("\nRéseaux visibles: ").append(rs==null?0:rs.size());if(rs!=null){rs.sort((a,b)->Integer.compare(b.level,a.level));int n=0;for(ScanResult r:rs){if(n++>=12)break;s.append("\n").append(r.SSID==null||r.SSID.isBlank()?"Réseau masqué":r.SSID).append(" • ").append(r.level).append(" dBm • ").append(band(r.frequency)).append(" ch ").append(channel(r.frequency)).append(" • ").append(r.frequency).append(" MHz");}}wifiView.setText(s.toString());}catch(Exception e){wifiView.setText("Wi‑Fi non accessible: "+e.getClass().getSimpleName());}}
    private String band(int f){return f>=5925?"6 GHz":f>=4900?"5 GHz":"2,4 GHz";} private int channel(int f){if(f==2484)return 14;if(f>=2412&&f<=2472)return(f-2407)/5;if(f>=5000&&f<5925)return(f-5000)/5;if(f>=5955)return(f-5950)/5;return 0;}
    private void registerBt(){IntentFilter f=new IntentFilter(BluetoothDevice.ACTION_FOUND);if(Build.VERSION.SDK_INT>=33)registerReceiver(btReceiver,f,RECEIVER_NOT_EXPORTED);else registerReceiver(btReceiver,f);} private void startBtScan(){if(ba==null){btView.setText("Bluetooth non présent");return;}if(Build.VERSION.SDK_INT>=31&&!can(Manifest.permission.BLUETOOTH_SCAN)){btView.setText("Permission Bluetooth requise");return;}btFound.clear();try{if(ba.isDiscovering())ba.cancelDiscovery();ba.startDiscovery();btView.setText("Balayage Bluetooth en cours…");}catch(Exception e){btView.setText("Bluetooth indisponible: "+e.getClass().getSimpleName());}}
    private void updateBluetooth(){StringBuilder s=new StringBuilder("Appareils détectés: ").append(btFound.size());int n=0;for(String x:btFound){if(n++>=15)break;s.append("\n").append(x);}main.post(()->btView.setText(s.toString()));}

    private void refreshLocation(){if(!can(Manifest.permission.ACCESS_FINE_LOCATION)&&!can(Manifest.permission.ACCESS_COARSE_LOCATION)){gpsView.setText("Permission de localisation requise");return;}try{Location best=null;for(String p:lm.getProviders(true)){Location l=lm.getLastKnownLocation(p);if(l!=null&&(best==null||l.getAccuracy()<best.getAccuracy()))best=l;}showLocation(best);if(Build.VERSION.SDK_INT>=24&&can(Manifest.permission.ACCESS_FINE_LOCATION)){if(gnssCb==null)gnssCb=new GnssStatus.Callback(){@Override public void onSatelliteStatusChanged(GnssStatus status){sats=status.getSatelliteCount();}};lm.registerGnssStatusCallback(gnssCb,main);}lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000,1,new LocationListener(){@Override public void onLocationChanged(Location l){showLocation(l);try{lm.removeUpdates(this);}catch(Exception ignored){}}},Looper.getMainLooper());}catch(Exception e){gpsView.setText("Position indisponible: "+e.getClass().getSimpleName());}}
    private void showLocation(Location l){if(l==null){gpsView.setText("Position en attente…");return;}gpsView.setText(String.format(Locale.CANADA_FRENCH,"Lat %.6f • Lon %.6f\nPrécision ±%.1f m • altitude %.1f m\nVitesse %.1f km/h • direction %.0f° • satellites visibles %d",l.getLatitude(),l.getLongitude(),l.getAccuracy(),l.getAltitude(),l.getSpeed()*3.6,l.getBearing(),sats));}
    private void refreshCell(){try{TelephonyManager tm=(TelephonyManager)getSystemService(TELEPHONY_SERVICE);String op=tm.getNetworkOperatorName();String type="type "+tm.getDataNetworkType();cellView.setText((op==null||op.isBlank()?"Opérateur inconnu":op)+" • "+type+"\nAndroid limite certaines données radio selon permissions, opérateur et version.");}catch(Exception e){cellView.setText("Informations cellulaires limitées: "+e.getClass().getSimpleName());}}
    private void refreshUsb(){try{UsbManager u=(UsbManager)getSystemService(USB_SERVICE);Collection<UsbDevice> ds=u.getDeviceList().values();StringBuilder s=new StringBuilder("Périphériques USB: ").append(ds.size());for(UsbDevice d:ds)s.append("\n• ").append(d.getDeviceName()).append(" • VID ").append(d.getVendorId()).append(" / PID ").append(d.getProductId());s.append("\nDes sondes externes compatibles peuvent étendre les capacités physiques de l'app.");usbView.setText(s.toString());}catch(Exception e){usbView.setText("USB indisponible: "+e.getClass().getSimpleName());}}
    private void refreshCapabilities(){List<Sensor> all=sm.getSensorList(Sensor.TYPE_ALL);NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);Intent batt=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));float temp=batt==null?Float.NaN:batt.getIntExtra("temperature",0)/10f;StringBuilder s=new StringBuilder("Capteurs Android exposés: ").append(all.size()).append("\nNFC: ").append(nfc==null?"non":nfc.isEnabled()?"oui, activé":"oui, désactivé");if(!Float.isNaN(temp)&&temp>0)s.append("\nTempérature batterie: ").append(String.format(Locale.CANADA_FRENCH,"%.1f °C",temp));s.append("\n\nINVENTAIRE COMPLET");int shown=0;for(Sensor x:all){if(shown++>=50){s.append("\n…");break;}s.append("\n\n• ").append(x.getName()).append("\n  ").append(x.getVendor()).append(" • type ").append(x.getType()).append(" • portée ").append(String.format(Locale.CANADA_FRENCH,"%.2f",x.getMaximumRange())).append(" • résolution ").append(String.format(Locale.CANADA_FRENCH,"%.5f",x.getResolution())).append(" • ").append(String.format(Locale.CANADA_FRENCH,"%.2f mA",x.getPower()));}capView.setText(s.toString());}

    private void scanLan(){lanView.setText("Balayage du réseau local en cours…");ExecutorService pool=Executors.newFixedThreadPool(24);pool.submit(()->{try{String base=subnet();if(base==null){main.post(()->lanView.setText("Impossible de déterminer le sous-réseau actuel"));return;}List<String> found=Collections.synchronizedList(new ArrayList<>());CountDownLatch latch=new CountDownLatch(254);for(int i=1;i<=254;i++){final int h=i;pool.submit(()->{try{InetAddress a=InetAddress.getByName(base+h);if(a.isReachable(160))found.add(a.getHostAddress());}catch(Exception ignored){}finally{latch.countDown();}});}latch.await(12,TimeUnit.SECONDS);Collections.sort(found);StringBuilder s=new StringBuilder("Appareils répondant sur ").append(base).append("0/24: ").append(found.size());for(String ip:found)s.append("\n• ").append(ip);main.post(()->lanView.setText(s.toString()));}catch(Exception e){main.post(()->lanView.setText("Balayage impossible: "+e.getClass().getSimpleName()));}});}
    private String subnet(){try{ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();if(n==null)return null;LinkProperties lp=cm.getLinkProperties(n);if(lp==null)return null;for(LinkAddress la:lp.getLinkAddresses()){InetAddress a=la.getAddress();if(a instanceof Inet4Address){byte[] b=a.getAddress();return (b[0]&255)+"."+(b[1]&255)+"."+(b[2]&255)+".";}}}catch(Exception ignored){}return null;}

    private boolean can(String p){return Build.VERSION.SDK_INT<23||checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;} private void requestPermissionsNow(){if(Build.VERSION.SDK_INT<23)return;ArrayList<String> p=new ArrayList<>();for(String x:new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION})if(!can(x))p.add(x);if(Build.VERSION.SDK_INT>=31){if(!can(Manifest.permission.BLUETOOTH_SCAN))p.add(Manifest.permission.BLUETOOTH_SCAN);if(!can(Manifest.permission.BLUETOOTH_CONNECT))p.add(Manifest.permission.BLUETOOTH_CONNECT);}if(Build.VERSION.SDK_INT>=23&&!can(Manifest.permission.READ_PHONE_STATE))p.add(Manifest.permission.READ_PHONE_STATE);if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),77);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);main.postDelayed(this::refreshAll,300);}
    @Override protected void onDestroy(){super.onDestroy();try{unregisterReceiver(btReceiver);}catch(Exception ignored){}try{sm.unregisterListener(this);}catch(Exception ignored){}if(gnssCb!=null&&Build.VERSION.SDK_INT>=24)try{lm.unregisterGnssStatusCallback(gnssCb);}catch(Exception ignored){}}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}

    private static class SeismoView extends View {private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG),grid=new Paint(Paint.ANTI_ALIAS_FLAG),limit=new Paint(Paint.ANTI_ALIAS_FLAG);private final ArrayDeque<Float> data=new ArrayDeque<>();private float threshold=.18f;SeismoView(Context c){super(c);line.setColor(Color.CYAN);line.setStrokeWidth(4);grid.setColor(Color.rgb(35,75,120));grid.setStrokeWidth(1);limit.setColor(Color.rgb(255,180,0));limit.setStrokeWidth(2);}void add(float v,float t){threshold=t;if(data.size()>=220)data.removeFirst();data.addLast(v);invalidate();}void clear(){data.clear();invalidate();}@Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();for(int i=1;i<4;i++)c.drawLine(0,h*i/4f,w,h*i/4f,grid);float max=Math.max(1f,threshold*2);float y=h-(threshold/max*h);c.drawLine(0,y,w,y,limit);if(data.size()<2)return;Path p=new Path();int i=0,n=data.size();for(float v:data){float x=i++*(w/(float)Math.max(1,n-1));float yy=h-Math.min(1f,v/max)*h;if(i==1)p.moveTo(x,yy);else p.lineTo(x,yy);}c.drawPath(p,line);}}
}
