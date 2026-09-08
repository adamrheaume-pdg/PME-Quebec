package quebec.maison.universelle;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.*;
import android.net.nsd.*;
import android.net.wifi.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    private LinearLayout list;
    private TextView status;
    private SharedPreferences prefs;
    private ExecutorService pool;
    private WifiManager.MulticastLock multicastLock;
    private final Set<String> seenIps = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Map<String,TextView> titles = Collections.synchronizedMap(new HashMap<>());
    private final Map<String,TextView> macViews = Collections.synchronizedMap(new HashMap<>());
    private final Map<String,TextView> detailsViews = Collections.synchronizedMap(new HashMap<>());
    private final Map<String,TextView> iconViews = Collections.synchronizedMap(new HashMap<>());

    private static final String[] TYPES = {"Automatique","Télévision","Cellulaire","Xbox","Caméra","Imprimante","Alexa / Echo","Ordinateur","Tablette","Routeur","Prise Wi-Fi","Lumière","Console","Haut-parleur","NAS / Serveur","Autre"};
    private static final String[] ICONS = {"🌐","📺","📱","🎮","📹","🖨️","🔊","💻","📱","📡","🔌","💡","🕹️","🔈","🗄️","⚙️"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        list=findViewById(R.id.deviceList);
        status=findViewById(R.id.status);
        prefs=getSharedPreferences("wifi_quebec",MODE_PRIVATE);
        findViewById(R.id.btnScan).setOnClickListener(v->scanNetwork());
        findViewById(R.id.btnAdd).setOnClickListener(v->showAddDialog());
        findViewById(R.id.btnHA).setOnClickListener(v->showHomeAssistantDialog());
        loadSavedManualDevices();
    }

    private void scanNetwork(){
        list.removeAllViews(); seenIps.clear(); titles.clear(); macViews.clear(); detailsViews.clear(); iconViews.clear();
        loadSavedManualDevices();
        Set<String> subnets=localSubnets24();
        if(subnets.isEmpty()){status.setText("Impossible de déterminer le réseau local.");return;}
        acquireMulticast();
        status.setText("Recherche Wi‑Fi 2,4 / 5 / 6 GHz, mDNS, SSDP et appareils actifs…");
        discoverNsd("_http._tcp."); discoverNsd("_https._tcp."); discoverNsd("_hap._tcp."); discoverNsd("_matter._tcp.");
        discoverSsdp();
        final int total=subnets.size()*254; final int[] done={0};
        pool=Executors.newFixedThreadPool(48);
        for(String subnet:subnets) for(int i=1;i<255;i++){
            final String host=subnet+i;
            pool.execute(()->{
                ProbeResult r=probe(host);
                if(r.alive) runOnUiThread(()->addOrMergeDevice("Appareil réseau",host,r.details(),null,null));
                synchronized(done){done[0]++; if(done[0]==total) runOnUiThread(()->{
                    refreshMacs();
                    status.setText("Analyse terminée — "+seenIps.size()+" appareil(s) connecté(s) détecté(s).");
                    releaseMulticastLater();
                });}
            });
        }
        pool.shutdown();
    }

    private Set<String> localSubnets24(){
        Set<String> out=new LinkedHashSet<>();
        try{
            Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
            while(en.hasMoreElements()){
                NetworkInterface n=en.nextElement(); if(!n.isUp()||n.isLoopback())continue;
                Enumeration<InetAddress> as=n.getInetAddresses();
                while(as.hasMoreElements()){
                    InetAddress a=as.nextElement();
                    if(a instanceof Inet4Address && a.isSiteLocalAddress()){
                        String ip=a.getHostAddress(); out.add(ip.substring(0,ip.lastIndexOf('.')+1));
                    }
                }
            }
        }catch(Exception ignored){}
        return out;
    }

    private ProbeResult probe(String host){
        int[] ports={53,80,443,554,1400,1883,5000,5001,5540,8000,8008,8009,8080,8123,8443,8883,9000,9080,9100,9999,10001,18400,32400};
        boolean alive=false; List<Integer> open=new ArrayList<>();
        try{alive=InetAddress.getByName(host).isReachable(170);}catch(Exception ignored){}
        for(int p:ports) try(Socket s=new Socket()){
            s.connect(new InetSocketAddress(host,p),95); open.add(p); alive=true; if(open.size()>=4)break;
        }catch(Exception ignored){}
        return new ProbeResult(alive,open);
    }
    static class ProbeResult{
        boolean alive; List<Integer> ports; ProbeResult(boolean a,List<Integer>p){alive=a;ports=p;}
        String details(){if(ports.isEmpty())return "Actif sur le réseau local";return "Ports ouverts : "+ports.toString().replace("[","").replace("]","");}
    }

    private void acquireMulticast(){try{WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(wm!=null){multicastLock=wm.createMulticastLock("WifiQuebecDiscovery");multicastLock.setReferenceCounted(false);multicastLock.acquire();}}catch(Exception ignored){}}
    private void releaseMulticastLater(){new Handler(getMainLooper()).postDelayed(()->{try{if(multicastLock!=null&&multicastLock.isHeld())multicastLock.release();}catch(Exception ignored){}},5000);}

    private void discoverNsd(String type){
        try{
            NsdManager nsd=(NsdManager)getSystemService(NSD_SERVICE);
            nsd.discoverServices(type,NsdManager.PROTOCOL_DNS_SD,new NsdManager.DiscoveryListener(){
                public void onDiscoveryStarted(String s){}
                public void onServiceFound(NsdServiceInfo info){try{nsd.resolveService(info,new NsdManager.ResolveListener(){
                    public void onResolveFailed(NsdServiceInfo x,int e){}
                    public void onServiceResolved(NsdServiceInfo x){if(x.getHost()!=null){String ip=x.getHost().getHostAddress();runOnUiThread(()->addOrMergeDevice(x.getServiceName(),ip,"mDNS "+type+" • port "+x.getPort(),null,null));}}
                });}catch(Exception ignored){}}
                public void onServiceLost(NsdServiceInfo i){} public void onDiscoveryStopped(String s){}
                public void onStartDiscoveryFailed(String s,int e){try{nsd.stopServiceDiscovery(this);}catch(Exception ignored){}}
                public void onStopDiscoveryFailed(String s,int e){try{nsd.stopServiceDiscovery(this);}catch(Exception ignored){}}
            });
        }catch(Exception ignored){}
    }

    private void discoverSsdp(){
        Executors.newSingleThreadExecutor().execute(()->{
            DatagramSocket socket=null;
            try{
                socket=new DatagramSocket();socket.setSoTimeout(700);
                String msg="M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: ssdp:all\r\n\r\n";
                byte[] data=msg.getBytes(StandardCharsets.UTF_8); DatagramPacket q=new DatagramPacket(data,data.length,InetAddress.getByName("239.255.255.250"),1900);socket.send(q);socket.send(q);
                long until=System.currentTimeMillis()+3500;byte[] buf=new byte[8192];
                while(System.currentTimeMillis()<until)try{
                    DatagramPacket r=new DatagramPacket(buf,buf.length);socket.receive(r);String text=new String(r.getData(),0,r.getLength(),StandardCharsets.UTF_8);String ip=r.getAddress().getHostAddress();
                    String server=header(text,"SERVER"),loc=header(text,"LOCATION");String n=server.isEmpty()?"Appareil UPnP/SSDP":server;String d="SSDP/UPnP"+(loc.isEmpty()?"":" • "+loc);
                    runOnUiThread(()->addOrMergeDevice(n,ip,d,null,null));
                }catch(SocketTimeoutException ignored){}
            }catch(Exception ignored){}finally{if(socket!=null)socket.close();}
        });
    }
    private String header(String text,String key){Matcher m=Pattern.compile("(?im)^"+Pattern.quote(key)+"\\s*:\\s*(.+)$").matcher(text);return m.find()?m.group(1).trim():"";}

    private void addOrMergeDevice(String detectedName,String ip,String details,String onUrl,String offUrl){
        if(ip==null||ip.trim().isEmpty())return;ip=ip.trim();
        DevicePrefs dp=getPrefs(ip); String name=!dp.name.isEmpty()?dp.name:detectedName;
        if(seenIps.contains(ip)){
            TextView title=titles.get(ip),dv=detailsViews.get(ip),mv=macViews.get(ip),iv=iconViews.get(ip);
            if(title!=null&&dp.name.isEmpty()&&detectedName!=null&&!detectedName.startsWith("Appareil réseau")&&title.getText().toString().startsWith("Appareil réseau"))title.setText(detectedName);
            if(dv!=null&&details!=null&&!details.isEmpty()&&!dv.getText().toString().contains(details))dv.setText(dv.getText()+"\n"+details);
            String mac=!dp.mac.isEmpty()?dp.mac:lookupMac(ip);if(mv!=null&&!mac.isEmpty())mv.setText("MAC : "+mac);
            if(iv!=null&&dp.type==0)iv.setText(iconFor(autoType(name,details,ip)));
            return;
        }
        seenIps.add(ip);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(18,18,18,18);card.setBackgroundResource(R.drawable.card);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,12);card.setLayoutParams(cp);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);
        int type=dp.type==0?autoType(name,details,ip):dp.type;
        TextView icon=tv(iconFor(type),30,false);icon.setPadding(0,0,14,0);head.addView(icon);
        LinearLayout headText=new LinearLayout(this);headText.setOrientation(LinearLayout.VERTICAL);headText.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        TextView title=tv(name==null||name.isEmpty()?"Appareil réseau":name,18,true);headText.addView(title);
        TextView connected=tv("● CONNECTÉ",12,true);headText.addView(connected);startBlink(connected);head.addView(headText);card.addView(head);
        card.addView(tv("IP : "+ip,14,false));
        String mac=!dp.mac.isEmpty()?dp.mac:lookupMac(ip);TextView mv=tv("MAC : "+(mac.isEmpty()?"non disponible":mac),13,false);card.addView(mv);
        TextView dv=tv(details==null?"Réseau local":details,13,false);card.addView(dv);
        titles.put(ip,title);macViews.put(ip,mv);detailsViews.put(ip,dv);iconViews.put(ip,icon);
        final String fip=ip;
        LinearLayout r1=row();Button edit=button("Modifier");edit.setOnClickListener(v->showEditDevice(fip));r1.addView(edit);Button open=button("Ouvrir");open.setOnClickListener(v->openBrowser(fip));r1.addView(open);card.addView(r1);
        LinearLayout r2=row();Button block=button("Bloquer");block.setOnClickListener(v->routerAction(fip,true));r2.addView(block);Button unblock=button("Débloquer");unblock.setOnClickListener(v->routerAction(fip,false));r2.addView(unblock);card.addView(r2);
        list.addView(card);
    }

    private void startBlink(TextView v){Handler h=new Handler(getMainLooper());Runnable[] rr=new Runnable[1];rr[0]=new Runnable(){boolean red=true;public void run(){if(v.getWindowToken()==null)return;v.setTextColor(red?Color.rgb(220,0,0):Color.rgb(0,75,210));red=!red;h.postDelayed(this,550);}};h.post(rr[0]);}
    private int autoType(String name,String d,String ip){String s=((name==null?"":name)+" "+(d==null?"":d)).toLowerCase(Locale.ROOT);if(ip.equals(gatewayIp()))return 9;if(s.contains("brother")||s.contains("printer")||s.contains("imprim")||s.contains("9100"))return 5;if(s.contains("xbox"))return 3;if(s.contains("camera")||s.contains("caméra")||s.contains("rtsp")||s.contains("554"))return 4;if(s.contains("alexa")||s.contains("echo"))return 6;if(s.contains("tv")||s.contains("television")||s.contains("renderer")||s.contains("media"))return 1;if(s.contains("iphone")||s.contains("pixel")||s.contains("android")||s.contains("phone"))return 2;if(s.contains("chromecast")||s.contains("speaker")||s.contains("sonos"))return 13;if(s.contains("nas")||s.contains("server"))return 14;return 0;}
    private String iconFor(int type){if(type<0||type>=ICONS.length)return ICONS[0];return ICONS[type];}

    private void showEditDevice(String ip){
        DevicePrefs d=getPrefs(ip);LinearLayout box=dialogBox();EditText name=input("Nom personnalisé");name.setText(d.name);EditText mac=input("Adresse MAC");String detected=d.mac.isEmpty()?lookupMac(ip):d.mac;mac.setText(detected);
        Spinner sp=new Spinner(this);sp.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,TYPES));sp.setSelection(Math.max(0,Math.min(d.type,TYPES.length-1)));
        box.addView(name);box.addView(mac);box.addView(tv("Icône / type d’appareil",13,true));box.addView(sp);
        new AlertDialog.Builder(this).setTitle("Modifier l’appareil").setView(box).setPositiveButton("Enregistrer",(x,w)->{
            int type=sp.getSelectedItemPosition();savePrefs(ip,name.getText().toString().trim(),normalizeMac(mac.getText().toString()),type);
            TextView t=titles.get(ip);if(t!=null&&!name.getText().toString().trim().isEmpty())t.setText(name.getText().toString().trim());TextView m=macViews.get(ip);if(m!=null)m.setText("MAC : "+(mac.getText().toString().trim().isEmpty()?"non disponible":normalizeMac(mac.getText().toString())));TextView iv=iconViews.get(ip);if(iv!=null)iv.setText(iconFor(type==0?autoType(name.getText().toString(),detailsViews.get(ip)==null?"":detailsViews.get(ip).getText().toString(),ip):type));
        }).setNegativeButton("Annuler",null).show();
    }

    static class DevicePrefs{String name="";String mac="";int type=0;}
    private DevicePrefs getPrefs(String ip){DevicePrefs d=new DevicePrefs();try{JSONObject all=new JSONObject(prefs.getString("device_prefs","{}"));JSONObject o=all.optJSONObject(ip);if(o!=null){d.name=o.optString("name","");d.mac=o.optString("mac","");d.type=o.optInt("type",0);}}catch(Exception ignored){}return d;}
    private void savePrefs(String ip,String name,String mac,int type){try{JSONObject all=new JSONObject(prefs.getString("device_prefs","{}"));JSONObject o=new JSONObject();o.put("name",name);o.put("mac",mac);o.put("type",type);all.put(ip,o);prefs.edit().putString("device_prefs",all.toString()).apply();}catch(Exception ignored){}}
    private String normalizeMac(String s){return s==null?"":s.trim().toUpperCase(Locale.ROOT).replace('-',':');}
    private void refreshMacs(){Executors.newSingleThreadExecutor().execute(()->{for(String ip:new ArrayList<>(seenIps)){String mac=getPrefs(ip).mac;if(mac.isEmpty())mac=lookupMac(ip);final String f=mac;runOnUiThread(()->{TextView v=macViews.get(ip);if(v!=null&&!f.isEmpty())v.setText("MAC : "+f);});}});}
    private String lookupMac(String ip){try(BufferedReader br=new BufferedReader(new FileReader("/proc/net/arp"))){String line;while((line=br.readLine())!=null){String[]p=line.trim().split("\\s+");if(p.length>=4&&p[0].equals(ip)&&p[3].matches("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")&&!p[3].equals("00:00:00:00:00:00"))return p[3].toUpperCase(Locale.ROOT);}}catch(Exception ignored){}return "";}

    private void routerAction(String ip,boolean block){
        String mac=getPrefs(ip).mac;if(mac.isEmpty())mac=lookupMac(ip);
        Intent i=new Intent(this,BellRouterActivity.class);
        i.putExtra("target_ip",ip);i.putExtra("target_mac",mac);i.putExtra("block",block);i.putExtra("gateway",gatewayIp());
        startActivity(i);
    }
    private String gatewayIp(){try{WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);DhcpInfo d=wm.getDhcpInfo();int g=d.gateway;if(g!=0)return String.format(Locale.US,"%d.%d.%d.%d",g&255,(g>>8)&255,(g>>16)&255,(g>>24)&255);}catch(Exception ignored){}return "192.168.2.1";}

    private void showAddDialog(){LinearLayout box=dialogBox();EditText n=input("Nom");EditText ip=input("IP");EditText mac=input("MAC facultative");box.addView(n);box.addView(ip);box.addView(mac);new AlertDialog.Builder(this).setTitle("Ajouter un appareil").setView(box).setPositiveButton("Enregistrer",(d,w)->{saveManual(n.getText().toString(),ip.getText().toString(),mac.getText().toString());list.removeAllViews();seenIps.clear();loadSavedManualDevices();}).setNegativeButton("Annuler",null).show();}
    private void saveManual(String n,String ip,String mac){try{JSONArray a=new JSONArray(prefs.getString("manual","[]"));JSONObject o=new JSONObject();o.put("name",n.isEmpty()?"Appareil manuel":n);o.put("ip",ip.trim());o.put("mac",normalizeMac(mac));a.put(o);prefs.edit().putString("manual",a.toString()).apply();}catch(Exception ignored){}}
    private void loadSavedManualDevices(){try{JSONArray a=new JSONArray(prefs.getString("manual","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String ip=o.optString("ip");if(!o.optString("mac").isEmpty()){DevicePrefs d=getPrefs(ip);savePrefs(ip,d.name,o.optString("mac"),d.type);}addOrMergeDevice(o.optString("name"),ip,"Ajout manuel",null,null);}}catch(Exception ignored){}}

    private void showHomeAssistantDialog(){LinearLayout box=dialogBox();EditText url=input("URL Home Assistant");url.setText(prefs.getString("ha_url",""));EditText token=input("Jeton longue durée");token.setText(prefs.getString("ha_token",""));box.addView(url);box.addView(token);new AlertDialog.Builder(this).setTitle("Home Assistant").setView(box).setPositiveButton("Connecter",(d,w)->{prefs.edit().putString("ha_url",url.getText().toString()).putString("ha_token",token.getText().toString()).apply();loadHomeAssistant();}).setNegativeButton("Annuler",null).show();}
    private void loadHomeAssistant(){String base=prefs.getString("ha_url","").replaceAll("/$","");String token=prefs.getString("ha_token","");if(base.isEmpty()||token.isEmpty()){status.setText("URL ou jeton Home Assistant manquant.");return;}status.setText("Connexion Home Assistant…");Executors.newSingleThreadExecutor().execute(()->{try{String body=requestText("GET",base+"/api/states",token);JSONArray arr=new JSONArray(body);runOnUiThread(()->{int count=0;for(int i=0;i<arr.length();i++)try{JSONObject s=arr.getJSONObject(i);String id=s.optString("entity_id");if(!(id.startsWith("light.")||id.startsWith("switch.")||id.startsWith("fan.")||id.startsWith("cover.")))continue;JSONObject at=s.optJSONObject("attributes");String friendly=at==null?id:at.optString("friendly_name",id);addHaCard(friendly,id,s.optString("state"),base,token);count++;}catch(Exception ignored){}status.setText("Home Assistant : "+count+" appareil(s) contrôlable(s).");});}catch(Exception e){runOnUiThread(()->status.setText("Home Assistant : "+e.getMessage()));}});}
    private void addHaCard(String name,String entity,String state,String base,String token){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(18,18,18,18);c.setBackgroundResource(R.drawable.card);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,12);c.setLayoutParams(cp);c.addView(tv("💡 "+name,18,true));TextView con=tv("● CONNECTÉ",12,true);c.addView(con);startBlink(con);c.addView(tv(entity+" • état : "+state,13,false));LinearLayout r=row();Button on=button("ON");Button off=button("OFF");on.setOnClickListener(v->haService(base,token,entity,"turn_on"));off.setOnClickListener(v->haService(base,token,entity,"turn_off"));r.addView(on);r.addView(off);c.addView(r);list.addView(c);}
    private void haService(String base,String token,String entity,String service){String domain=entity.substring(0,entity.indexOf('.'));String u=base+"/api/services/"+domain+"/"+service;JSONObject o=new JSONObject();try{o.put("entity_id",entity);}catch(Exception ignored){}Executors.newSingleThreadExecutor().execute(()->{try{int c=request("POST",u,o.toString(),token);runOnUiThread(()->status.setText(entity+" : HTTP "+c));}catch(Exception e){runOnUiThread(()->status.setText("Erreur : "+e.getMessage()));}});}

    private void openBrowser(String ip){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("http://"+ip)));}catch(Exception e){Toast.makeText(this,"Aucune interface Web détectée",Toast.LENGTH_SHORT).show();}}
    private int request(String method,String u,String body,String token)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(2500);c.setReadTimeout(3500);c.setRequestMethod(method);if(token!=null&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Content-Type","application/json");if(body!=null){c.setDoOutput(true);try(OutputStream os=c.getOutputStream()){os.write(body.getBytes(StandardCharsets.UTF_8));}}int code=c.getResponseCode();c.disconnect();return code;}
    private String requestText(String method,String u,String token)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(3500);c.setReadTimeout(6000);c.setRequestMethod(method);if(token!=null&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);int code=c.getResponseCode();InputStream is=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[]buf=new byte[4096];int n;while(is!=null&&(n=is.read(buf))>0)out.write(buf,0,n);c.disconnect();if(code<200||code>=300)throw new IOException("HTTP "+code);return out.toString("UTF-8");}

    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setPadding(0,8,0,0);return r;}
    private Button button(String s){Button b=new Button(this);b.setText(s);return b;}
    private TextView tv(String s,int sp,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.rgb(16,33,58));if(bold)t.setTypeface(null,1);return t;}
    private LinearLayout dialogBox(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);int p=(int)(16*getResources().getDisplayMetrics().density);b.setPadding(p,p/2,p,p/2);return b;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);return e;}
}
