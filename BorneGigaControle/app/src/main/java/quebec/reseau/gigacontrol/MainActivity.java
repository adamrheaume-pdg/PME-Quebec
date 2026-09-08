package quebec.reseau.gigacontrol;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private LinearLayout list;
    private TextView status;
    private EditText search;
    private final List<Device> devices = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService pool = Executors.newFixedThreadPool(32);
    private static final String MODEM = "http://192.168.2.1/";

    static class Device {
        String ip, mac, alias;
        Device(String ip, String mac) { this.ip=ip; this.mac=mac; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        list=findViewById(R.id.deviceList); status=findViewById(R.id.status); search=findViewById(R.id.search);
        findViewById(R.id.scanBtn).setOnClickListener(v -> scanNetwork());
        findViewById(R.id.modemBtn).setOnClickListener(v -> openModem(null));
        search.setOnEditorActionListener((v, actionId, event) -> { findOrOpen(search.getText().toString().trim()); return true; });
        scanNetwork();
    }

    private void findOrOpen(String q) {
        if(q.isEmpty()) return;
        for(Device d: devices) if(d.ip.equalsIgnoreCase(q) || (d.mac!=null && d.mac.equalsIgnoreCase(q))) { showEdit(d); return; }
        Device d = new Device(q.contains(":") ? "—" : q, q.contains(":") ? q : "—");
        showEdit(d);
    }

    private void scanNetwork() {
        list.removeAllViews(); devices.clear(); status.setText("Analyse du réseau 192.168.2.0/24…");
        new Thread(() -> {
            CountDownLatch latch = new CountDownLatch(254);
            for(int i=1;i<=254;i++) {
                final int n=i;
                pool.submit(() -> { try {
                    String ip="192.168.2."+n;
                    if(isAlive(ip)) synchronized(devices){ devices.add(new Device(ip, lookupMac(ip))); }
                } finally { latch.countDown(); }});
            }
            try { latch.await(12, TimeUnit.SECONDS); } catch(Exception ignored) {}
            devices.sort(Comparator.comparingInt(d -> Integer.parseInt(d.ip.substring(d.ip.lastIndexOf('.')+1))));
            runOnUiThread(this::renderDevices);
        }).start();
    }

    private boolean isAlive(String ip) {
        try {
            InetAddress a=InetAddress.getByName(ip);
            if(a.isReachable(280)) return true;
            Socket s=new Socket(); s.connect(new InetSocketAddress(ip,80),180); s.close(); return true;
        } catch(Exception e) { return false; }
    }

    private String lookupMac(String ip) {
        String mac = fromProcArp(ip);
        if(mac != null) return mac;
        try {
            Process p = new ProcessBuilder("ip","neigh","show",ip).redirectErrorStream(true).start();
            BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line=br.readLine();
            if(line!=null) {
                String[] t=line.split("\\s+");
                for(int i=0;i<t.length-1;i++) if("lladdr".equals(t[i])) return t[i+1].toUpperCase(Locale.ROOT);
            }
        } catch(Exception ignored) {}
        return "Non disponible";
    }

    private String fromProcArp(String ip) {
        try(BufferedReader br=new BufferedReader(new FileReader("/proc/net/arp"))) {
            String line; while((line=br.readLine())!=null) {
                String[] p=line.trim().split("\\s+");
                if(p.length>=4 && p[0].equals(ip) && !p[3].equals("00:00:00:00:00:00")) return p[3].toUpperCase(Locale.ROOT);
            }
        } catch(Exception ignored) {}
        return null;
    }

    private void renderDevices() {
        list.removeAllViews();
        status.setText(devices.size()+" appareil(s) détecté(s) • toucher pour gérer");
        if(devices.isEmpty()) {
            TextView t=new TextView(this); t.setText("Aucun appareil détecté. Vérifie que le téléphone est connecté au Wi‑Fi de la Borne Bell."); t.setPadding(20,20,20,20); list.addView(t); return;
        }
        for(Device d: devices) {
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(18,14,18,14);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(10,5,10,5); card.setLayoutParams(cp); card.setBackgroundColor(Color.WHITE);
            TextView title=new TextView(this); title.setText((d.alias==null?"Appareil réseau":d.alias)+"   "+d.ip); title.setTextSize(16); title.setTextColor(Color.rgb(0,61,165)); title.setTypeface(null,1);
            TextView sub=new TextView(this); sub.setText("MAC : "+d.mac); sub.setTextSize(13); sub.setTextColor(Color.DKGRAY);
            Button b=new Button(this); b.setText("GÉRER / BLOQUER"); b.setOnClickListener(v -> showEdit(d));
            card.addView(title); card.addView(sub); card.addView(b); list.addView(card);
        }
    }

    private void showEdit(Device d) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); int p=36; box.setPadding(p,10,p,0);
        EditText alias=new EditText(this); alias.setHint("Nom personnalisé"); alias.setText(d.alias==null?"":d.alias);
        TextView info=new TextView(this); info.setText("IP : "+d.ip+"\nMAC : "+d.mac+"\n\nLe blocage est appliqué par la Borne Bell, pas par le téléphone."); info.setPadding(0,18,0,8);
        box.addView(alias); box.addView(info);
        new AlertDialog.Builder(this).setTitle("Gérer l’appareil").setView(box)
            .setNeutralButton("Renommer", (x,w) -> { d.alias=alias.getText().toString().trim(); renderDevices(); })
            .setNegativeButton("Fermer", null)
            .setPositiveButton("Bloquer / Débloquer", (x,w) -> openModem(d)).show();
    }

    private void openModem(Device d) {
        final WebView web=new WebView(this);
        WebSettings ws=web.getSettings(); ws.setJavaScriptEnabled(true); ws.setDomStorageEnabled(true); ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.setWebViewClient(new WebViewClient()); web.loadUrl(MODEM);
        String msg = d==null ? "Administration locale de la Borne Giga 2.0" : "Appareil : "+d.ip+" • "+d.mac+"\nDans Bell : Contrôle d’accès → Toujours bloquer l’accès.";
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL);
        TextView note=new TextView(this); note.setText(msg); note.setTextColor(Color.WHITE); note.setBackgroundColor(Color.rgb(0,61,165)); note.setPadding(18,12,18,12);
        wrap.addView(note,new LinearLayout.LayoutParams(-1,-2)); wrap.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog dlg=new AlertDialog.Builder(this).setView(wrap).setNegativeButton("Fermer",null).create();
        dlg.setOnShowListener(x -> dlg.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dlg.show();
    }
}
