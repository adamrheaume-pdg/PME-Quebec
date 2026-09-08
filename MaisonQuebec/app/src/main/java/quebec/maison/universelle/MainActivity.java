package quebec.maison.universelle;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.net.*;
import android.net.nsd.*;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.ColorDrawable;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private LinearLayout list;
    private TextView status;
    private final Set<String> seen = Collections.synchronizedSet(new HashSet<>());
    private ExecutorService pool;
    private SharedPreferences prefs;

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
        list.removeAllViews(); seen.clear(); loadSavedManualDevices();
        String ip = localIpv4();
        if (ip == null) { status.setText("Impossible de déterminer l'adresse IP locale."); return; }
        String subnet = ip.substring(0, ip.lastIndexOf('.') + 1);
        status.setText("Recherche sur " + subnet + "0/24…");
        discoverNsd();
        pool = Executors.newFixedThreadPool(32);
        final int[] done = {0};
        for (int i=1; i<255; i++) {
            final String host = subnet + i;
            pool.execute(() -> {
                int port = probe(host);
                if (port > 0) runOnUiThread(() -> addDeviceCard("Appareil réseau", host, "Port " + port, null, null));
                synchronized(done) { done[0]++; if (done[0] == 254) runOnUiThread(() -> status.setText("Analyse terminée — " + seen.size() + " appareil(s) détecté(s).")); }
            });
        }
        pool.shutdown();
    }

    private int probe(String host) {
        int[] ports = {80, 443, 554, 8000, 8080, 8123, 1883};
        for (int p : ports) try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, p), 180);
            return p;
        } catch(Exception ignored) {}
        return -1;
    }

    private String localIpv4() {
        try {
            Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
            while(en.hasMoreElements()) {
                java.net.NetworkInterface n = en.nextElement();
                if (!n.isUp() || n.isLoopback()) continue;
                Enumeration<InetAddress> as = n.getInetAddresses();
                while(as.hasMoreElements()) {
                    InetAddress a = as.nextElement();
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
                }
            }
        } catch(Exception ignored) {}
        return null;
    }

    private void discoverNsd() {
        try {
            NsdManager nsd = (NsdManager)getSystemService(NSD_SERVICE);
            nsd.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, new NsdManager.DiscoveryListener() {
                public void onDiscoveryStarted(String s) {}
                public void onServiceFound(NsdServiceInfo info) {
                    nsd.resolveService(info, new NsdManager.ResolveListener() {
                        public void onResolveFailed(NsdServiceInfo x, int e) {}
                        public void onServiceResolved(NsdServiceInfo x) {
                            if (x.getHost()!=null) runOnUiThread(() -> addDeviceCard(x.getServiceName(), x.getHost().getHostAddress(), "mDNS • port " + x.getPort(), null, null));
                        }
                    });
                }
                public void onServiceLost(NsdServiceInfo i) {}
                public void onDiscoveryStopped(String s) {}
                public void onStartDiscoveryFailed(String s,int e) { try{nsd.stopServiceDiscovery(this);}catch(Exception ignored){} }
                public void onStopDiscoveryFailed(String s,int e) { try{nsd.stopServiceDiscovery(this);}catch(Exception ignored){} }
            });
        } catch(Exception ignored) {}
    }

    private void addDeviceCard(String name, String ip, String details, String onUrl, String offUrl) {
        String key = name + "|" + ip + "|" + details;
        if (!seen.add(key)) return;
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(18,18,18,18);
        card.setBackgroundResource(R.drawable.card);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,0,0,12); card.setLayoutParams(cp);
        TextView title = tv(name, 18, true); card.addView(title);
        card.addView(tv("IP : " + ip, 14, false));
        card.addView(tv(details == null ? "Réseau local" : details, 13, false));
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0,12,0,0);
        Button open = new Button(this); open.setText("Ouvrir"); open.setOnClickListener(v -> openBrowser(ip)); row.addView(open);
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
            for(int i=0;i<a.length();i++) { JSONObject o=a.getJSONObject(i); addDeviceCard(o.optString("name"),o.optString("ip"),"Ajout manuel",o.optString("on"),o.optString("off")); }
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
        String key="ha|"+entity; if(!seen.add(key))return;
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
