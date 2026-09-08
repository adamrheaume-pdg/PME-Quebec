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
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    private LinearLayout list;
    private TextView status;
    private EditText search;
    private final Map<String, Device> devices = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(32);
    private final AtomicInteger scanGeneration = new AtomicInteger(0);
    private SharedPreferences prefs;
    private final Handler blinkHandler = new Handler(Looper.getMainLooper());
    private final List<TextView> blinkingOnlineDots = new ArrayList<>();
    private boolean blinkOn = true;
    private static final String MODEM = "http://192.168.2.1/";

    static class Device {
        String ip, mac, alias;
        boolean online;
        boolean manualBlocked;
        boolean scheduleEnabled;
        int startMinutes = 7 * 60;
        int endMinutes = 22 * 60;
        Device(String ip, String mac) { this.ip=ip; this.mac=mac; }
    }

    private final Runnable blinkTask = new Runnable() {
        @Override public void run() {
            blinkOn = !blinkOn;
            for (TextView dot : new ArrayList<>(blinkingOnlineDots)) dot.setAlpha(blinkOn ? 1f : 0.22f);
            blinkHandler.postDelayed(this, 650);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("devices", MODE_PRIVATE);
        list=findViewById(R.id.deviceList); status=findViewById(R.id.status); search=findViewById(R.id.search);
        findViewById(R.id.scanBtn).setOnClickListener(v -> scanNetwork());
        findViewById(R.id.modemBtn).setOnClickListener(v -> openModem(null));
        search.setOnEditorActionListener((v, actionId, event) -> { findOrOpen(search.getText().toString().trim()); return true; });
        loadKnownDevices();
        blinkHandler.post(blinkTask);
        scanNetwork();
    }

    @Override protected void onDestroy() {
        blinkHandler.removeCallbacks(blinkTask);
        super.onDestroy();
    }

    private void findOrOpen(String q) {
        if(q.isEmpty()) return;
        for(Device d: devices.values()) {
            if(d.ip.equalsIgnoreCase(q) || (d.mac!=null && d.mac.equalsIgnoreCase(q))) { showEdit(d); return; }
        }
        String ip = q.contains(":") ? "—" : q;
        String mac = q.contains(":") ? q.toUpperCase(Locale.ROOT) : "Non disponible";
        Device d = new Device(ip, mac);
        loadPrefs(d);
        showEdit(d);
    }

    private void scanNetwork() {
        final int generation = scanGeneration.incrementAndGet();
        for(Device d: devices.values()) d.online = false;
        status.setText("Analyse du réseau 192.168.2.0/24…");
        renderDevices();
        new Thread(() -> {
            CountDownLatch latch = new CountDownLatch(254);
            final Map<String, Device> found = new ConcurrentHashMap<>();
            for(int i=1;i<=254;i++) {
                final int n=i;
                pool.submit(() -> { try {
                    if (generation != scanGeneration.get()) return;
                    String ip="192.168.2."+n;
                    if(isAlive(ip)) {
                        String mac = lookupMac(ip);
                        Device d = new Device(ip, mac);
                        d.online = true;
                        loadPrefs(d);
                        found.put(ip, d);
                    }
                } finally { latch.countDown(); }});
            }
            try { latch.await(12, TimeUnit.SECONDS); } catch(Exception ignored) {}
            if (generation != scanGeneration.get()) return;
            for (Device d : found.values()) {
                Device old = devices.get(d.ip);
                if (old != null) {
                    d.alias = old.alias != null && !old.alias.isEmpty() ? old.alias : d.alias;
                    d.manualBlocked = old.manualBlocked;
                    d.scheduleEnabled = old.scheduleEnabled;
                    d.startMinutes = old.startMinutes;
                    d.endMinutes = old.endMinutes;
                    if ("Non disponible".equals(d.mac) && old.mac != null) d.mac = old.mac;
                }
                devices.put(d.ip, d);
                rememberIp(d.ip);
            }
            runOnUiThread(this::renderDevices);
        }).start();
    }

    private boolean isAlive(String ip) {
        try {
            InetAddress a=InetAddress.getByName(ip);
            if(a.isReachable(280)) return true;
        } catch(Exception ignored) {}
        int[] ports = {80, 443, 53, 8000, 8080};
        for(int port: ports) {
            try {
                Socket s=new Socket(); s.connect(new InetSocketAddress(ip,port),140); s.close(); return true;
            } catch(Exception ignored) {}
        }
        return false;
    }

    private String lookupMac(String ip) {
        String mac = fromProcArp(ip);
        if(mac != null) return mac;
        try {
            java.lang.Process p = new ProcessBuilder("ip","neigh","show",ip).redirectErrorStream(true).start();
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
        blinkingOnlineDots.clear();
        List<Device> sorted = new ArrayList<>(devices.values());
        sorted.sort((a,b) -> ipNumber(a.ip) - ipNumber(b.ip));
        int onlineCount = 0;
        for(Device d: sorted) if(d.online) onlineCount++;
        status.setText(onlineCount+" connecté(s) • "+sorted.size()+" appareil(s) connus");
        if(sorted.isEmpty()) {
            TextView t=new TextView(this); t.setText("Aucun appareil détecté. Vérifie que le téléphone est connecté au Wi‑Fi de la Borne Bell."); t.setPadding(20,20,20,20); list.addView(t); return;
        }
        for(Device d: sorted) {
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(18,14,18,14);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(10,5,10,5); card.setLayoutParams(cp); card.setBackgroundColor(Color.WHITE);

            LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
            TextView dot=new TextView(this); dot.setText("●"); dot.setTextSize(25); dot.setPadding(0,0,12,0);
            boolean blocked = isEffectivelyBlocked(d);
            if(blocked) dot.setTextColor(Color.rgb(210,30,45));
            else if(d.online) { dot.setTextColor(Color.rgb(0,185,90)); blinkingOnlineDots.add(dot); }
            else dot.setTextColor(Color.rgb(0,90,210));

            TextView title=new TextView(this);
            title.setText((d.alias==null || d.alias.isEmpty()?"Appareil réseau":d.alias)+"   "+d.ip);
            title.setTextSize(16); title.setTextColor(Color.rgb(0,61,165)); title.setTypeface(null,1);
            top.addView(dot,new LinearLayout.LayoutParams(-2,-2));
            top.addView(title,new LinearLayout.LayoutParams(0,-2,1));

            TextView sub=new TextView(this);
            String state = blocked ? "BLOQUÉ" : (d.online ? "CONNECTÉ" : "AUTORISÉ • HORS LIGNE");
            String sched = d.scheduleEnabled ? " • Horaire "+fmt(d.startMinutes)+"–"+fmt(d.endMinutes) : "";
            sub.setText("MAC : "+d.mac+"\nÉtat : "+state+sched);
            sub.setTextSize(13); sub.setTextColor(Color.DKGRAY);

            LinearLayout buttons=new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
            Button manage=new Button(this); manage.setText("GÉRER"); manage.setOnClickListener(v -> showEdit(d));
            Button schedule=new Button(this); schedule.setText("HORAIRE"); schedule.setOnClickListener(v -> showSchedule(d));
            buttons.addView(manage,new LinearLayout.LayoutParams(0,-2,1));
            buttons.addView(schedule,new LinearLayout.LayoutParams(0,-2,1));
            card.setOnClickListener(v -> showEdit(d));
            card.addView(top); card.addView(sub); card.addView(buttons); list.addView(card);
        }
    }

    private int ipNumber(String ip) {
        try { return Integer.parseInt(ip.substring(ip.lastIndexOf('.')+1)); } catch(Exception e) { return 9999; }
    }

    private void showEdit(Device d) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); int p=36; box.setPadding(p,10,p,0);
        EditText alias=new EditText(this); alias.setHint("Nom personnalisé"); alias.setText(d.alias==null?"":d.alias);
        TextView info=new TextView(this);
        info.setText("IP : "+d.ip+"\nMAC : "+d.mac+"\n\n● Vert clignotant : connecté\n● Bleu : autorisé mais hors ligne\n● Rouge : bloqué selon l’application/horaire\n\nLe blocage réel doit être appliqué par la Borne Bell.");
        info.setPadding(0,18,0,8);
        box.addView(alias); box.addView(info);
        String action = d.manualBlocked ? "Marquer autorisé" : "Marquer bloqué";
        new AlertDialog.Builder(this).setTitle("Gérer l’appareil").setView(box)
            .setNeutralButton("Horaire", (x,w) -> showSchedule(d))
            .setNegativeButton("Borne Bell", (x,w) -> { saveAlias(d, alias.getText().toString()); openModem(d); })
            .setPositiveButton(action, (x,w) -> {
                saveAlias(d, alias.getText().toString());
                d.manualBlocked = !d.manualBlocked;
                savePrefs(d);
                renderDevices();
                if(d.manualBlocked) openModem(d);
            }).show();
    }

    private void showSchedule(Device d) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(32,8,32,0);
        CheckBox enabled=new CheckBox(this); enabled.setText("Activer l’horaire Wi‑Fi pour cet appareil"); enabled.setChecked(d.scheduleEnabled);
        TextView startLabel=new TextView(this); startLabel.setText("Autoriser à partir de"); startLabel.setPadding(0,12,0,0);
        TimePicker start=new TimePicker(this); start.setIs24HourView(true); start.setHour(d.startMinutes/60); start.setMinute(d.startMinutes%60);
        TextView endLabel=new TextView(this); endLabel.setText("Bloquer à partir de"); endLabel.setPadding(0,12,0,0);
        TimePicker end=new TimePicker(this); end.setIs24HourView(true); end.setHour(d.endMinutes/60); end.setMinute(d.endMinutes%60);
        TextView note=new TextView(this); note.setText("L’APK mémorise et affiche automatiquement l’état prévu par l’horaire. Pour couper réellement Internet sur l’appareil, la règle doit aussi être enregistrée dans le contrôle d’accès de la Borne Bell."); note.setPadding(0,12,0,0);
        box.addView(enabled); box.addView(startLabel); box.addView(start); box.addView(endLabel); box.addView(end); box.addView(note);
        new AlertDialog.Builder(this).setTitle("Horaire Wi‑Fi").setView(box)
            .setNegativeButton("Annuler", null)
            .setNeutralButton("Ouvrir Borne Bell", (x,w) -> openModem(d))
            .setPositiveButton("Enregistrer", (x,w) -> {
                d.scheduleEnabled=enabled.isChecked();
                d.startMinutes=start.getHour()*60+start.getMinute();
                d.endMinutes=end.getHour()*60+end.getMinute();
                savePrefs(d); renderDevices();
            }).show();
    }

    private boolean isEffectivelyBlocked(Device d) {
        if(d.manualBlocked) return true;
        if(!d.scheduleEnabled) return false;
        Calendar c=Calendar.getInstance();
        int now=c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);
        if(d.startMinutes==d.endMinutes) return false;
        if(d.startMinutes<d.endMinutes) return now<d.startMinutes || now>=d.endMinutes;
        return now>=d.endMinutes && now<d.startMinutes;
    }

    private String fmt(int minutes) {
        return String.format(Locale.CANADA_FRENCH, "%02d:%02d", minutes/60, minutes%60);
    }

    private String prefKey(Device d) {
        if(d.mac!=null && d.mac.contains(":") && !"Non disponible".equals(d.mac)) return d.mac.replace(":", "_");
        return d.ip.replace('.', '_');
    }

    private void loadPrefs(Device d) {
        String k=prefKey(d);
        d.alias=prefs.getString(k+"_alias", d.alias);
        d.manualBlocked=prefs.getBoolean(k+"_blocked", false);
        d.scheduleEnabled=prefs.getBoolean(k+"_schedule", false);
        d.startMinutes=prefs.getInt(k+"_start", 7*60);
        d.endMinutes=prefs.getInt(k+"_end", 22*60);
    }

    private void savePrefs(Device d) {
        String k=prefKey(d);
        prefs.edit().putString(k+"_alias", d.alias==null?"":d.alias)
            .putBoolean(k+"_blocked", d.manualBlocked)
            .putBoolean(k+"_schedule", d.scheduleEnabled)
            .putInt(k+"_start", d.startMinutes)
            .putInt(k+"_end", d.endMinutes).apply();
        rememberIp(d.ip);
    }

    private void saveAlias(Device d, String alias) {
        d.alias=alias==null?"":alias.trim(); savePrefs(d);
    }

    private void rememberIp(String ip) {
        if(ip==null || !ip.startsWith("192.168.2.")) return;
        Set<String> known=new HashSet<>(prefs.getStringSet("known_ips", Collections.emptySet()));
        known.add(ip);
        prefs.edit().putStringSet("known_ips", known).apply();
    }

    private void loadKnownDevices() {
        Set<String> known=prefs.getStringSet("known_ips", Collections.emptySet());
        for(String ip: known) {
            Device d=new Device(ip,"Non disponible");
            loadPrefs(d); d.online=false;
            devices.put(ip,d);
        }
    }

    private void openModem(Device d) {
        final WebView web=new WebView(this);
        WebSettings ws=web.getSettings(); ws.setJavaScriptEnabled(true); ws.setDomStorageEnabled(true); ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.setWebViewClient(new WebViewClient()); web.loadUrl(MODEM);
        String msg = d==null ? "Administration locale de la Borne Giga 2.0" : "Appareil : "+d.ip+" • "+d.mac+"\nDans Bell : Contrôle d’accès → configure le blocage ou l’horaire pour cet appareil.";
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL);
        TextView note=new TextView(this); note.setText(msg); note.setTextColor(Color.WHITE); note.setBackgroundColor(Color.rgb(0,61,165)); note.setPadding(18,12,18,12);
        wrap.addView(note,new LinearLayout.LayoutParams(-1,-2)); wrap.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog dlg=new AlertDialog.Builder(this).setView(wrap).setNegativeButton("Fermer",null).create();
        dlg.setOnShowListener(x -> dlg.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dlg.show();
    }
}
