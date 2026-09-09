package quebec.detecteur360;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.*;
import android.location.*;
import android.net.*;
import android.net.wifi.*;
import android.nfc.NfcAdapter;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private LocationManager locationManager;
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private LinearLayout content;
    private final Map<Integer, TextView> sensorViews = new HashMap<>();
    private TextView wifiView, btView, gpsView, lanView, systemView;
    private final Set<String> btFound = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Handler main = new Handler(Looper.getMainLooper());
    private float[] gravity = new float[3];

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (BluetoothDevice.ACTION_FOUND.equals(i.getAction())) {
                BluetoothDevice d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (d != null) {
                    String name = "Appareil Bluetooth";
                    try { if (checkSelfPermissionCompat(Manifest.permission.BLUETOOTH_CONNECT)) name = d.getName(); } catch (Exception ignored) {}
                    if (name == null || name.isBlank()) name = "Appareil Bluetooth";
                    btFound.add(name + " • " + d.getAddress());
                    updateBluetoothText();
                }
            }
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(0, 31, 151));
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        buildUi();
        requestNeededPermissions();
        registerSensors();
        registerBluetoothReceiver();
        refreshAll();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(244, 247, 255));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(28));
        scroll.addView(content);

        TextView title = text("Détecteur 360 Québec", 28, Color.rgb(0,31,151), true);
        content.addView(title);
        TextView sub = text("Capteurs • réseaux • ondes • position • environnement", 14, Color.DKGRAY, false);
        sub.setPadding(0, dp(2), 0, dp(14));
        content.addView(sub);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button scan = button("SCAN 360");
        Button lan = button("RÉSEAU LOCAL");
        buttons.addView(scan, new LinearLayout.LayoutParams(0, dp(52), 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1); lp.setMargins(dp(8),0,0,0);
        buttons.addView(lan, lp);
        content.addView(buttons);
        scan.setOnClickListener(v -> refreshAll());
        lan.setOnClickListener(v -> scanLan());

        addHeader("MESURES EN DIRECT");
        addSensorCard(Sensor.TYPE_MAGNETIC_FIELD, "Champ magnétique", "µT • intensité et axes X/Y/Z");
        addSensorCard(Sensor.TYPE_LIGHT, "Lumière", "lux");
        addSensorCard(Sensor.TYPE_PRESSURE, "Pression atmosphérique", "hPa");
        addSensorCard(Sensor.TYPE_PROXIMITY, "Proximité", "cm");
        addSensorCard(Sensor.TYPE_AMBIENT_TEMPERATURE, "Température ambiante", "°C si capteur présent");
        addSensorCard(Sensor.TYPE_RELATIVE_HUMIDITY, "Humidité", "% si capteur présent");
        addSensorCard(Sensor.TYPE_ACCELEROMETER, "Niveau / inclinaison", "° par rapport à l'horizontale");
        addSensorCard(Sensor.TYPE_GYROSCOPE, "Gyroscope", "rad/s");

        addHeader("RÉSEAUX ET POSITION");
        wifiView = addCard("Wi‑Fi", "Analyse du réseau et des réseaux visibles");
        btView = addCard("Bluetooth", "Détection d'appareils à proximité");
        gpsView = addCard("Position", "GPS / réseau");
        lanView = addCard("Appareils réseau local", "Appuie sur RÉSEAU LOCAL pour un balayage du sous-réseau actuel");
        systemView = addCard("Capacités du téléphone", "Inventaire automatique");

        TextView note = text("Thermique : Android ne peut pas fabriquer une caméra thermique par logiciel. Une vraie image thermique exige un capteur FLIR/USB‑C ou équivalent. L'app affiche seulement les températures réellement exposées par le téléphone.", 13, Color.DKGRAY, false);
        note.setPadding(dp(4), dp(16), dp(4), 0);
        content.addView(note);
        setContentView(scroll);
    }

    private void addHeader(String s) {
        TextView v = text(s, 13, Color.rgb(0,31,151), true);
        v.setPadding(dp(2), dp(20), 0, dp(7));
        content.addView(v);
    }

    private void addSensorCard(int type, String title, String unit) {
        TextView v = addCard(title, unit + "\nEn attente de mesure…");
        sensorViews.put(type, v);
    }

    private TextView addCard(String title, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(13), dp(16), dp(13));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1), Color.rgb(210,220,245));
        box.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0,0,0,dp(10));
        content.addView(box,p);
        box.addView(text(title, 16, Color.rgb(0,31,151), true));
        TextView v = text(value, 14, Color.DKGRAY, false); v.setPadding(0,dp(5),0,0); box.addView(v);
        return v;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v;
    }

    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(Color.rgb(0,31,151)); g.setCornerRadius(dp(14)); b.setBackground(g); return b;
    }

    private void registerSensors() {
        for (Integer type : sensorViews.keySet()) {
            Sensor s = sensorManager.getDefaultSensor(type);
            TextView v = sensorViews.get(type);
            if (s != null) sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
            else v.setText("Capteur physique non présent sur cet appareil");
        }
    }

    @Override public void onSensorChanged(SensorEvent e) {
        TextView v = sensorViews.get(e.sensor.getType()); if (v == null) return;
        float[] x = e.values;
        switch (e.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                double total = Math.sqrt(x[0]*x[0]+x[1]*x[1]+x[2]*x[2]);
                v.setText(String.format(Locale.CANADA_FRENCH,"%.1f µT total\nX %.1f • Y %.1f • Z %.1f µT",total,x[0],x[1],x[2])); break;
            case Sensor.TYPE_LIGHT: v.setText(String.format(Locale.CANADA_FRENCH,"%.1f lux",x[0])); break;
            case Sensor.TYPE_PRESSURE: v.setText(String.format(Locale.CANADA_FRENCH,"%.2f hPa",x[0])); break;
            case Sensor.TYPE_PROXIMITY: v.setText(String.format(Locale.CANADA_FRENCH,"%.2f cm",x[0])); break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE: v.setText(String.format(Locale.CANADA_FRENCH,"%.1f °C",x[0])); break;
            case Sensor.TYPE_RELATIVE_HUMIDITY: v.setText(String.format(Locale.CANADA_FRENCH,"%.1f %%",x[0])); break;
            case Sensor.TYPE_GYROSCOPE: v.setText(String.format(Locale.CANADA_FRENCH,"X %.3f • Y %.3f • Z %.3f rad/s",x[0],x[1],x[2])); break;
            case Sensor.TYPE_ACCELEROMETER:
                gravity = x.clone();
                double roll = Math.toDegrees(Math.atan2(gravity[1], gravity[2]));
                double pitch = Math.toDegrees(Math.atan2(-gravity[0], Math.sqrt(gravity[1]*gravity[1]+gravity[2]*gravity[2])));
                v.setText(String.format(Locale.CANADA_FRENCH,"Roulis %.1f° • Tangage %.1f°\nAccélération X %.2f • Y %.2f • Z %.2f m/s²",roll,pitch,x[0],x[1],x[2])); break;
        }
    }
    @Override public void onAccuracyChanged(Sensor s, int accuracy) {}

    private void refreshAll() { refreshWifi(); startBluetoothScan(); refreshLocation(); refreshCapabilities(); }

    private void refreshWifi() {
        try {
            WifiInfo info = wifiManager.getConnectionInfo();
            StringBuilder sb = new StringBuilder();
            sb.append("Signal ").append(info.getRssi()).append(" dBm • ").append(info.getLinkSpeed()).append(" Mb/s");
            if (Build.VERSION.SDK_INT >= 21) sb.append(" • ").append(info.getFrequency()).append(" MHz");
            List<ScanResult> results = wifiManager.getScanResults();
            if (checkSelfPermissionCompat(Manifest.permission.ACCESS_FINE_LOCATION)) try { wifiManager.startScan(); } catch (Exception ignored) {}
            sb.append("\nRéseaux visibles: ").append(results == null ? 0 : results.size());
            if (results != null) {
                results.sort((a,b)->Integer.compare(b.level,a.level)); int n=0;
                for (ScanResult r: results) { if (n++ >= 8) break; sb.append("\n").append(r.SSID == null || r.SSID.isBlank()?"Réseau masqué":r.SSID).append(" • ").append(r.level).append(" dBm • ").append(r.frequency).append(" MHz"); }
            }
            wifiView.setText(sb.toString());
        } catch (Exception e) { wifiView.setText("Wi‑Fi non accessible: " + e.getClass().getSimpleName()); }
    }

    private void registerBluetoothReceiver() {
        IntentFilter f = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(btReceiver, f, RECEIVER_NOT_EXPORTED); else registerReceiver(btReceiver, f);
    }

    private void startBluetoothScan() {
        if (bluetoothAdapter == null) { btView.setText("Bluetooth non présent"); return; }
        if (Build.VERSION.SDK_INT >= 31 && !checkSelfPermissionCompat(Manifest.permission.BLUETOOTH_SCAN)) { btView.setText("Permission Bluetooth requise"); return; }
        btFound.clear();
        try { if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery(); bluetoothAdapter.startDiscovery(); btView.setText("Balayage Bluetooth en cours…"); }
        catch (Exception e) { btView.setText("Bluetooth indisponible: " + e.getClass().getSimpleName()); }
    }

    private void updateBluetoothText() {
        StringBuilder s = new StringBuilder("Appareils détectés: ").append(btFound.size()); int n=0;
        for (String x: btFound) { if (n++ >= 12) break; s.append("\n").append(x); }
        main.post(() -> btView.setText(s.toString()));
    }

    private void refreshLocation() {
        if (!checkSelfPermissionCompat(Manifest.permission.ACCESS_FINE_LOCATION) && !checkSelfPermissionCompat(Manifest.permission.ACCESS_COARSE_LOCATION)) { gpsView.setText("Permission de localisation requise"); return; }
        try {
            Location best = null;
            for (String p: locationManager.getProviders(true)) { Location l = locationManager.getLastKnownLocation(p); if (l != null && (best == null || l.getAccuracy() < best.getAccuracy())) best = l; }
            if (best != null) gpsView.setText(String.format(Locale.CANADA_FRENCH,"Lat %.6f • Lon %.6f\nPrécision ±%.1f m • Altitude %.1f m",best.getLatitude(),best.getLongitude(),best.getAccuracy(),best.getAltitude()));
            else gpsView.setText("Position en attente…");
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, l -> gpsView.setText(String.format(Locale.CANADA_FRENCH,"Lat %.6f • Lon %.6f\nPrécision ±%.1f m • Altitude %.1f m",l.getLatitude(),l.getLongitude(),l.getAccuracy(),l.getAltitude())), Looper.getMainLooper());
        } catch (Exception e) { gpsView.setText("Position indisponible: " + e.getClass().getSimpleName()); }
    }

    private void refreshCapabilities() {
        List<Sensor> all = sensorManager.getSensorList(Sensor.TYPE_ALL);
        boolean nfc = NfcAdapter.getDefaultAdapter(this) != null;
        Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float bt = Float.NaN; if (batt != null) bt = batt.getIntExtra("temperature",0)/10f;
        StringBuilder s = new StringBuilder("Capteurs Android exposés: ").append(all.size()).append("\nNFC: ").append(nfc?"oui":"non");
        if (!Float.isNaN(bt) && bt > 0) s.append("\nTempérature batterie/appareil: ").append(String.format(Locale.CANADA_FRENCH,"%.1f °C",bt));
        s.append("\nCaméra thermique native: non détectable comme capteur standard Android");
        systemView.setText(s.toString());
    }

    private void scanLan() {
        lanView.setText("Balayage du réseau local en cours…");
        ExecutorService pool = Executors.newFixedThreadPool(24);
        pool.submit(() -> {
            try {
                String base = currentSubnetBase();
                if (base == null) { main.post(() -> lanView.setText("Impossible de déterminer le sous-réseau Wi‑Fi actuel")); return; }
                List<String> found = Collections.synchronizedList(new ArrayList<>());
                CountDownLatch latch = new CountDownLatch(254);
                for (int i=1;i<=254;i++) { final int host=i; pool.submit(() -> { try { InetAddress a=InetAddress.getByName(base+host); if (a.isReachable(180)) found.add(a.getHostAddress()); } catch(Exception ignored) {} finally { latch.countDown(); } }); }
                latch.await(12, TimeUnit.SECONDS);
                Collections.sort(found);
                StringBuilder s = new StringBuilder("Appareils répondant sur ").append(base).append("0/24: ").append(found.size());
                for (String ip: found) s.append("\n• ").append(ip);
                main.post(() -> lanView.setText(s.toString()));
            } catch (Exception e) { main.post(() -> lanView.setText("Balayage impossible: "+e.getClass().getSimpleName())); }
        });
    }

    private String currentSubnetBase() {
        try {
            ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork(); if (n == null) return null;
            LinkProperties lp = cm.getLinkProperties(n); if (lp == null) return null;
            for (LinkAddress la: lp.getLinkAddresses()) {
                InetAddress a = la.getAddress(); if (a instanceof Inet4Address && !a.isLoopbackAddress()) { byte[] b=a.getAddress(); return (b[0]&255)+"."+(b[1]&255)+"."+(b[2]&255)+"."; }
            }
        } catch(Exception ignored) {} return null;
    }

    private void requestNeededPermissions() {
        ArrayList<String> p = new ArrayList<>();
        if (!checkSelfPermissionCompat(Manifest.permission.ACCESS_FINE_LOCATION)) p.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 31) {
            if (!checkSelfPermissionCompat(Manifest.permission.BLUETOOTH_SCAN)) p.add(Manifest.permission.BLUETOOTH_SCAN);
            if (!checkSelfPermissionCompat(Manifest.permission.BLUETOOTH_CONNECT)) p.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (!p.isEmpty()) requestPermissions(p.toArray(new String[0]), 360);
    }

    private boolean checkSelfPermissionCompat(String p) { return Build.VERSION.SDK_INT < 23 || checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED; }
    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) { super.onRequestPermissionsResult(r,p,g); refreshAll(); }
    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() {
        super.onDestroy(); sensorManager.unregisterListener(this);
        try { unregisterReceiver(btReceiver); } catch(Exception ignored) {}
        try { if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery(); } catch(Exception ignored) {}
    }
}
