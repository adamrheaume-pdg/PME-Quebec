package quebec.maison.universelle;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.net.*;
import android.net.nsd.*;
import android.net.wifi.WifiManager;
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
    private ExecutorService pool;
    private SharedPreferences prefs;
    private final Set<String> seenIps = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, TextView> titles = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, TextView> detailsViews = Collections.synchronizedMap(new HashMap<>());
    private final Set<String> foundServices = Collections.synchronizedSet(new HashSet<>());
    private WifiManager.MulticastLock multicastLock;
    private volatile int scanFound = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        list = findViewById(R.id.deviceList);
        status = findViewById(R.id.status);
        prefs = getSharedPreferences("mq", MODE_PRIVATE);
        findViewById(R.id.btnScan).setOnClickListener(v -> scanNetwork());
        findViewById(R.id.btnAdd).setOnClickListener(v -> showAddDialog());
        findViewById(R.id.btnHA).setOnClickListener(v -> showHomeAssistantDialog());
        loadSavedManualDevices();
    }

    private void scanNetwork() {
        list.removeAllViews();
        seenIps.clear(); titles.clear(); detailsViews.clear(); foundServices.clear(); scanFound = 0;
        loadSavedManualDevices();

        Set<String> subnets = localSubnets24();
        if (subnets.isEmpty()) {
            status.setText("Impossible de déterminer le réseau local.");
            return;
        }

        acquireMulticast();
        status.setText("Recherche avancée : Wi-Fi 2,4/5/6 GHz, mDNS et SSDP…");
        discoverNsdType("_http._tcp.");
        discoverNsdType("_https._tcp.");
        discoverNsdType("_hap._tcp.");
        discoverNsdType("_matter._tcp.");
        discoverSsdp();

        int total = subnets.size() * 254;
        final int[] done = {0};
        pool = Executors.newFixedThreadPool(48);
        for (String subnet : subnets) {
            for (int i = 1; i < 255; i++) {
                final String host = subnet + i;
                pool.execute(() -> {
                    ProbeResult r = probe(host);
                    if (r.alive) runOnUiThread(() -> addOrMergeDevice("Appareil réseau", host, r.details(), null, null));
                    synchronized (done) {
                        done[0]++;
                        if (done[0] == total) runOnUiThread(() -> {
                            status.setText("Analyse terminée — " + seenIps.size() + " appareil(s) uniques détecté(s). Les appareils isolés par le routeur peuvent rester invisibles.");
                            releaseMulticastLater();
                        });
                    }
                });
            }
        }
        pool.shutdown();
    }

    private Set<String> localSubnets24() {
        Set<String> result = new LinkedHashSet<>();
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface n = en.nextElement();
                if (!n.isUp() || n.isLoopback()) continue;
                Enumeration<InetAddress> as = n.getInetAddresses();
                while (as.hasMoreElements()) {
                    InetAddress a = as.nextElement();
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        String ip = a.getHostAddress();
                        result.add(ip.substring(0, ip.lastIndexOf('.') + 1));
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private ProbeResult probe(String host) {
        int[] ports = {80,443,53,554,8000,8008,8009,8080,8123,8443,1883,8883,9100,9999,10001,1400,32400,5000,5001};
        boolean alive = false;
        List<Integer> open = new ArrayList<>();
        try { alive = InetAddress.getByName(host).isReachable(180); } catch (Exception ignored) {}
        for (int p : ports) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, p), 110);
                open.add(p); alive = true;
                if (open.size() >= 3) break;
            } catch (Exception ignored) {}
        }
        return new ProbeResult(alive, open);
    }

    private static class ProbeResult {
        final boolean alive; final List<Integer> ports;
        ProbeResult(boolean a, List<Integer> p) { alive=a; ports=p; }
        String details() {
            if (ports.isEmpty()) return "Actif sur le réseau local";
            StringBuilder b = new StringBuilder("Ports ouverts : ");
            for (int i=0;i<ports.size();i++) { if(i>0)b.append(", "); b.append(ports.get(i)); }
            return b.toString();
        }
    }

    private void acquireMulticast() {
        try {
            WifiManager wm = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null) {
                multicastLock = wm.createMulticastLock("MaisonQuebecDiscovery");
                multicastLock.setReferenceCounted(false);
                multicastLock.acquire();
            }
        } catch (Exception ignored) {}
    }

    private void releaseMulticastLater() {
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            try { if (multicastLock != null && multicastLock.isHeld()) multicastLock.release(); } catch(Exception ignored) {}
        }, 5000);
    }

    private void discoverNsdType(String type) {
        try {
            NsdManager nsd = (NsdManager)getSystemService(NSD_SERVICE);
            nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, new NsdManager.DiscoveryListener() {
                public void onDiscoveryStarted(String s) {}
                public void onServiceFound(NsdServiceInfo info) {
                    try {
                        nsd.resolveService(info, new NsdManager.ResolveListener() {
                            public void onResolveFailed(NsdServiceInfo x, int e) {}
                            public void onServiceResolved(NsdServiceInfo x) {
                                if (x.getHost()!=null) {
                                    String ip=x.getHost().getHostAddress();
                                    String detail="mDNS " + type + " • port " + x.getPort();
                                    runOnUiThread(() -> addOrMergeDevice(x.getServiceName(), ip, detail, null, null));
                                }
                            }
                        });
                    } catch(Exception ignored) {}
                }
                public void onServiceLost(NsdServiceInfo i) {}
                public void onDiscoveryStopped(String s) {}
                public void onStartDiscoveryFailed(String s,int e) { try{nsd.stopServiceDiscovery(this);}catch(Exception ignored){} }
                public void onStopDiscoveryFailed(String s,int e) { try{nsd.stopServiceDiscovery(this);}catch(Exception ignored){} }
            });
        } catch(Exception ignored) {}
    }

    private void discoverSsdp() {
        Executors.newSingleThreadExecutor().execute(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(700);
                String msg = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: ssdp:all\r\n\r\n";
                byte[] data = msg.getBytes(StandardCharsets.UTF_8);
                DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName("239.255.255.250"), 1900);
                socket.send(p); socket.send(p);
                long until = System.currentTimeMillis() + 3500;
                byte[] buf = new byte[8192];
                while (System.currentTimeMillis() < until) {
                    try {
                        DatagramPacket r = new DatagramPacket(buf, buf.length);
                        socket.receive(r);
                        String text = new String(r.getData(),0,r.getLength(),StandardCharsets.UTF_8);
                        String ip = r.getAddress().getHostAddress();
                        String server = header(text,"SERVER");
                        String location = header(text,"LOCATION");
                        String name = server.isEmpty() ? "Appareil UPnP/SSDP" : server;
                        String detail = "SSDP/UPnP" + (location.isEmpty()?"":" • "+location);
                        runOnUiThread(() -> addOrMergeDevice(name, ip, detail, null, null));
                    } catch(SocketTimeoutException ignored) {}
                }
            } catch(Exception ignored) {}
            finally { if(socket!=null) socket.close(); }
        });
    }

    private String header(String text, String key) {
        Matcher m=Pattern.compile("(?im)^"+Pattern.quote(key)+"\\s*:\\s*(.+)$").matcher(text);
        return m.find()?m.group(1).trim():"";
    }

    private void addOrMergeDevice(String name, String ip, String details, String onUrl, String offUrl) {
        if (ip == null || ip.trim().isEmpty()) return;
        ip = ip.trim();
        if (seenIps.contains(ip)) {
            TextView title = titles.get(ip);
            TextView dv = detailsViews.get(ip);
            if (title != null && name != null && !name.startsWith("Appareil réseau") && title.getText().toString().startsWith("Appareil réseau")) title.setText(name);
            if (dv != null && details != null && !details.isEmpty() && !dv.getText().toString().contains(details)) dv.setText(dv.getText() + "\n" + details);
            return;
        }
        seenIps.add(ip); scanFound++;
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(18,18,18,18); card.setBackgroundResource(R.drawable.card);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,0,0,12); card.setLayoutParams(cp);
        TextView title = tv(name==null||name.isEmpty()?"Appareil réseau":name,18,true); card.addView(title);
        TextView ipView = tv("IP : " + ip,14,false); card.addView(ipView);
        TextView dv = tv(details==null?"Réseau local":details,13,false); card.addView(dv);
        titles.put(ip,title); detailsViews.put(ip,dv);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0,12,0,0);
        Button open = new Button(this); open.setText("Ouvrir"); final String finalIp=ip; open.setOnClickListener(v -> openBrowser(finalIp)); row.addView(open);
        if (onUrl != null && !onUrl.isEmpty()) { Button on = new Button(this); on.setText("ON"); on.setOnClickListener(v -> httpAction(onUrl)); row.addView(on); }
        if (offUrl != null && !offUrl.isEmpty()) { Button off = new Button(this); off.setText("OFF"); off.setOnClickListener(v -> httpAction(offUrl)); row.addView(off); }
        card.addView(row); list.addView(card);
    }

    private TextView tv(String s, int sp, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.rgb(16,33,58)); if(bold)t.setTypeface(null,1); return t;
    }

    private void openBrowser(String ip) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+ip))); }
        catch(Exception e) { Toast.makeText(this,"Aucune interface Web détectée",Toast.LENGTH_SHORT).show(); }
    }

    private void httpAction(String url) {
        status.setText("Commande envoyée…");
        Executors.newSingleThreadExecutor().execute(() -> {
            try { int code = request("GET", url, null, null); runOnUiThread(() -> status.setText("Commande HTTP : " + code)); }
            catch(Exception e) { runOnUiThread(() -> status.setText("Échec : " + e.getMessage())); }
        });
    }

    private void showAddDialog() {
        LinearLayout box = dialogBox();
        EditText name = input("Nom (ex. Lampe salon)"); EditText ip = input("IP (ex. 192.168.2.45)");
        EditText on = input("URL ON facultative"); EditText off = input("URL OFF facultative");
        box.addView(name); box.addView(ip); box.addView(on); box.addView(off);
        new AlertDialog.Builder(this).setTitle("Ajouter un appareil").setView(box)
            .setPositiveButton("Enregistrer", (d,w)-> { saveManual(name.getText().toString(), ip.getText().toString(), on.getText().toString(), off.getText().toString()); loadSavedManualDevices(); })
            .setNegativeButton("Annuler", null).show();
    }

    private void saveManual(String n, String ip, String on, String off) {
        try {
            JSONArray a = new JSONArray(prefs.getString("manual", "[]"));
            JSONObject o = new JSONObject(); o.put("name", n.isEmpty()?"Appareil manuel":n); o.put("ip",ip); o.put("on",on); o.put("off",off); a.put(o);
            prefs.edit().putString("manual",a.toString()).apply();
        } catch(Exception ignored) {}
    }

    private void loadSavedManualDevices() {
        try {
            JSONArray a = new JSONArray(prefs.getString("manual", "[]"));
            for(int i=0;i<a.length();i++) { JSONObject o=a.getJSONObject(i); addOrMergeDevice(o.optString("name"),o.optString("ip"),"Ajout manuel",o.optString("on"),o.optString("off")); }
        } catch(Exception ignored) {}
    }

    private void showHomeAssistantDialog() {
        LinearLayout box = dialogBox();
        EditText url=input("URL Home Assistant (ex. http://192.168.2.10:8123)"); url.setText(prefs.getString("ha_url",""));
        EditText token=input("Jeton longue durée"); token.setText(prefs.getString("ha_token",""));
        box.addView(url); box.addView(token);
        new AlertDialog.Builder(this).setTitle("Home Assistant").setView(box)
            .setPositiveButton("Connecter",(d,w)-> { prefs.edit().putString("ha_url",url.getText().toString()).putString("ha_token",token.getText().toString()).apply(); loadHomeAssistant(); })
            .setNegativeButton("Annuler",null).show();
    }

    private void loadHomeAssistant() {
        String base=prefs.getString("ha_url","").replaceAll("/$",""); String token=prefs.getString("ha_token","");
        if(base.isEmpty()||token.isEmpty()){status.setText("URL ou jeton Home Assistant manquant.");return;}
        status.setText("Connexion à Home Assistant…");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String body = requestText("GET",base+"/api/states",null,token);
                JSONArray arr=new JSONArray(body); final int[] count={0};
                runOnUiThread(() -> {
                    for(int i=0;i<arr.length();i++) try {
                        JSONObject s=arr.getJSONObject(i); String id=s.optString("entity_id");
                        if(!(id.startsWith("light.")||id.startsWith("switch.")||id.startsWith("fan.")||id.startsWith("cover."))) continue;
                        String friendly=s.optJSONObject("attributes")!=null?s.optJSONObject("attributes").optString("friendly_name",id):id;
                        addHaCard(friendly,id,s.optString("state"),base,token); count[0]++;
                    } catch(Exception ignored) {}
                    status.setText("Home Assistant : " + count[0] + " appareil(s) contrôlable(s).");
                });
            } catch(Exception e) { runOnUiThread(() -> status.setText("Home Assistant : " + e.getMessage())); }
        });
    }

    private void addHaCard(String name,String entity,String state,String base,String token) {
        String key="ha|"+entity; if(!foundServices.add(key))return;
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(18,18,18,18); card.setBackgroundResource(R.drawable.card);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,0,0,12); card.setLayoutParams(cp);
        card.addView(tv(name,18,true)); card.addView(tv(entity+" • état : "+state,13,false));
        LinearLayout row=new LinearLayout(this); Button on=new Button(this); on.setText("ON"); Button off=new Button(this); off.setText("OFF");
        on.setOnClickListener(v->haService(base,token,entity,"turn_on")); off.setOnClickListener(v->haService(base,token,entity,"turn_off")); row.addView(on); row.addView(off); card.addView(row); list.addView(card);
    }

    private void haService(String base,String token,String entity,String service) {
        String domain=entity.substring(0,entity.indexOf('.')); String u=base+"/api/services/"+domain+"/"+service;
        JSONObject o=new JSONObject(); try{o.put("entity_id",entity);}catch(Exception ignored){}
        Executors.newSingleThreadExecutor().execute(() -> { try { int c=request("POST",u,o.toString(),token); runOnUiThread(()->status.setText(entity+" : HTTP "+c)); } catch(Exception e){runOnUiThread(()->status.setText("Erreur : "+e.getMessage()));} });
    }

    private int request(String method,String u,String body,String token) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(2500); c.setReadTimeout(3500); c.setRequestMethod(method);
        if(token!=null&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token); c.setRequestProperty("Content-Type","application/json");
        if(body!=null){c.setDoOutput(true);try(OutputStream os=c.getOutputStream()){os.write(body.getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode(); c.disconnect(); return code;
    }

    private String requestText(String method,String u,String body,String token) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection(); c.setConnectTimeout(3500); c.setReadTimeout(6000); c.setRequestMethod(method);
        if(token!=null&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token); c.setRequestProperty("Content-Type","application/json");
        int code=c.getResponseCode(); InputStream is=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n; while(is!=null&&(n=is.read(buf))>0)out.write(buf,0,n); c.disconnect();
        if(code<200||code>=300)throw new IOException("HTTP "+code); return out.toString("UTF-8");
    }

    private LinearLayout dialogBox(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);int p=(int)(16*getResources().getDisplayMetrics().density);b.setPadding(p,p/2,p,p/2);return b;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);return e;}
}
