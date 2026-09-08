package quebec.lanaudiere.essence;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION = 101;
    private static final int REQ_NOTIFICATIONS = 102;
    private static final String CHANNEL_ID = "best_price";
    private LinearLayout root, results;
    private TextView status, bestBanner;
    private Spinner fuelSpinner, sortSpinner, radiusSpinner;
    private Location lastLocation;
    private final int blue = Color.rgb(0, 63, 165);
    private final int pale = Color.rgb(247, 251, 255);

    private final Set<String> lanaudiereCities = new HashSet<>(Arrays.asList(
        "berthierville","charlemagne","chertsey","crabtree","entrelacs","joliette","lanoraie","l'assomption","l'épiphanie","lavaltrie","mandeville","mande ville","mascouche","notre-dame-de-la-merci","notre-dame-de-lourdes","notre-dame-des-prairies","rawdon","repentigny","saint-alexis","saint-alphonse-rodriguez","saint-ambroise-de-kildare","saint-barthélemy","saint-calixte","saint-charles-borromée","saint-côme","saint-cuthbert","saint-damien","saint-didace","saint-donat","saint-esprit","saint-félix-de-valois","saint-gabriel","saint-gabriel-de-brandon","saint-ignace-de-loyola","saint-jacques","saint-jean-de-matha","saint-liguori","saint-lin-laurentides","saint-michel-des-saints","saint-norbert","saint-paul","saint-pierre","saint-roch-de-l'achigan","saint-roch-ouest","saint-sulpice","saint-thomas","sainte-béatrix","sainte-élisabeth","sainte-émélie-de-l'énergie","sainte-geneviève-de-berthier","sainte-julienne","sainte-marcelline-de-kildare","sainte-marie-salomé","sainte-mélanie","terrebonne","la visitation-de-l'île-dupas"
    ));

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(pale);
        getWindow().setNavigationBarColor(pale);
        int ui = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) ui |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(ui);
        createNotificationChannel();
        buildUi();
        requestPermissionsAndLocate();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.setBackgroundColor(pale);
        scroll.addView(root);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(dp(18), dp(18) + top, dp(18), dp(28) + bottom);
            return insets;
        });

        TextView title = text("ESSENCE LANAUDIÈRE", 28, true, blue);
        root.addView(title);
        TextView sub = text("Prix autour de moi • GPS • meilleur prix", 15, false, Color.DKGRAY);
        sub.setPadding(0, dp(4), 0, dp(14));
        root.addView(sub);

        bestBanner = text("📍 Localisation en cours…", 18, true, Color.WHITE);
        bestBanner.setPadding(dp(16), dp(14), dp(16), dp(14));
        bestBanner.setBackground(round(blue, 18));
        root.addView(bestBanner, matchWrap(dp(8)));

        LinearLayout controls = card();
        controls.addView(label("Carburant"));
        fuelSpinner = spinner(new String[]{"Ordinaire", "Super", "Diesel"});
        controls.addView(fuelSpinner);
        controls.addView(label("Classement"));
        sortSpinner = spinner(new String[]{"Moins cher", "Plus proche", "Meilleur compromis"});
        sortSpinner.setSelection(1);
        controls.addView(sortSpinner);
        controls.addView(label("Rayon"));
        radiusSpinner = spinner(new String[]{"5 km", "10 km", "20 km", "30 km"});
        radiusSpinner.setSelection(3);
        controls.addView(radiusSpinner);

        Button refresh = button("ACTUALISER LES PRIX");
        refresh.setOnClickListener(v -> locateAndLoad());
        controls.addView(refresh, matchWrap(dp(12)));
        root.addView(controls, matchWrap(dp(12)));

        status = text("Les prix proviennent de Régie Essence Québec et sont présentés via Gas Québec.", 13, false, Color.DKGRAY);
        status.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(status);

        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results);

        LinearLayout refs = card();
        refs.addView(text("Références", 18, true, blue));
        Button caa = secondaryButton("CAA‑QUÉBEC • INFO ESSENCE");
        caa.setOnClickListener(v -> openUrl("https://www.caaquebec.com/fr/mobilite/info-essence"));
        refs.addView(caa, matchWrap(dp(8)));
        Button regie = secondaryButton("RÉGIE ESSENCE QUÉBEC");
        regie.setOnClickListener(v -> openUrl("https://www.regie-energie.qc.ca/fr/produits/regie-essence-quebec"));
        refs.addView(regie, matchWrap(0));
        TextView note = text("CAA‑Québec sert de référence régionale pour le prix réaliste. Le classement des stations utilise les prix publiés par les commerçants à la Régie de l’énergie. Les prix peuvent avoir quelques heures de décalage avec la pompe.", 12, false, Color.GRAY);
        note.setPadding(0, dp(10), 0, 0);
        refs.addView(note);
        root.addView(refs, matchWrap(dp(12)));
        setContentView(scroll);
        root.requestApplyInsets();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsAndLocate() {
        if (!hasLocationPermission()) {
            bestBanner.setText("📍 Autorisation GPS requise");
            status.setText("Autorise la localisation pour afficher les stations autour de toi.");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        } else {
            locateAndLoad();
            requestNotificationPermissionIfNeeded();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (hasLocationPermission()) {
                locateAndLoad();
                requestNotificationPermissionIfNeeded();
            } else {
                bestBanner.setText("GPS requis pour trouver les stations proches");
                status.setText("Appuie sur ACTUALISER LES PRIX pour redemander la localisation. Si Android ne l'affiche plus, ouvre les paramètres de l'application et autorise Localisation.");
            }
        }
    }

    private void locateAndLoad() {
        if (!hasLocationPermission()) {
            bestBanner.setText("📍 Autorisation GPS requise");
            status.setText("Autorise la localisation pour afficher les stations autour de toi.");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }

        bestBanner.setText("📍 Recherche de ta position…");
        status.setText("Recherche GPS + réseau en cours…");
        LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsEnabled && !networkEnabled) {
            bestBanner.setText("⚠️ Localisation du téléphone désactivée");
            status.setText("Active la localisation du téléphone, puis appuie sur ACTUALISER LES PRIX.");
            try { startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); } catch (Exception ignored) {}
            return;
        }

        Location best = null;
        for (String p : Arrays.asList(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
            try {
                if (!lm.isProviderEnabled(p)) continue;
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (Exception ignored) {}
        }

        if (best != null && System.currentTimeMillis() - best.getTime() < 15 * 60 * 1000L) {
            lastLocation = best;
            loadStations(best);
            return;
        }

        final Location fallback = best;
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] finished = {false};

        LocationListener listener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (finished[0] || location == null) return;
                finished[0] = true;
                handler.removeCallbacksAndMessages(null);
                try { lm.removeUpdates(this); } catch (Exception ignored) {}
                lastLocation = location;
                loadStations(location);
            }
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        try {
            if (networkEnabled) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, Looper.getMainLooper());
            if (gpsEnabled) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, Looper.getMainLooper());

            handler.postDelayed(() -> {
                if (finished[0]) return;
                finished[0] = true;
                try { lm.removeUpdates(listener); } catch (Exception ignored) {}
                if (fallback != null) {
                    bestBanner.setText("📍 Position approximative utilisée");
                    status.setText("Le GPS précis tarde à répondre; utilisation de la dernière position connue.");
                    lastLocation = fallback;
                    loadStations(fallback);
                } else {
                    bestBanner.setText("⚠️ Position introuvable");
                    status.setText("Aucune position n'a été obtenue après 8 secondes. Active la localisation précise, puis appuie sur ACTUALISER LES PRIX.");
                }
            }, 8000L);
        } catch (Exception e) {
            if (fallback != null) {
                lastLocation = fallback;
                loadStations(fallback);
            } else {
                bestBanner.setText("Impossible d’obtenir la position");
                status.setText("Vérifie la permission de localisation et active la localisation précise.");
            }
        }
    }

    private void loadStations(Location loc) {
        bestBanner.setText("⛽ Recherche des prix dans Lanaudière…");
        results.removeAllViews();
        String fuel = new String[]{"ordinaire","super","diesel"}[fuelSpinner.getSelectedItemPosition()];
        String sort = new String[]{"price","distance","value"}[sortSpinner.getSelectedItemPosition()];
        int radius = new int[]{5,10,20,30}[radiusSpinner.getSelectedItemPosition()];
        String url = String.format(Locale.US, "https://www.gasquebec.ca/api/stations/nearby?lat=%.6f&lng=%.6f&radius=%d&fuelType=%s&limit=50&sort=%s", loc.getLatitude(), loc.getLongitude(), radius, fuel, sort);
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                c.setRequestProperty("Accept","application/json");
                c.setRequestProperty("User-Agent", "EssenceLanaudiere/1.0 Android");
                int code = c.getResponseCode();
                if (code == 429) throw new IOException("Trop de demandes. Réessaie dans quelques instants.");
                if (code != 200) throw new IOException("Erreur de données (" + code + ")");
                String body = readAll(c.getInputStream());
                JSONObject obj = new JSONObject(body);
                JSONArray arr = obj.optJSONArray("stations");
                String source = parseSourceName(obj.opt("source"));
                List<Station> stations = new ArrayList<>();
                if (arr != null) for (int i=0; i<arr.length(); i++) {
                    JSONObject s = arr.getJSONObject(i);
                    String city = s.optString("city", "");
                    if (!isLanaudiere(city)) continue;
                    if (s.isNull("price")) continue;
                    stations.add(new Station(s.optString("name","Station"), s.optString("address",""), city, s.optDouble("lat"), s.optDouble("lng"), s.optDouble("price"), s.optDouble("distanceKm")));
                }
                sortStations(stations, sort);
                runOnUiThread(() -> showStations(stations, source, fuel, sort));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    bestBanner.setText("⚠️ Prix indisponibles");
                    status.setText(e.getMessage() == null ? "Impossible de joindre la source de prix." : e.getMessage());
                });
            }
        }).start();
    }

    private String parseSourceName(Object sourceValue) {
        if (sourceValue instanceof JSONObject) {
            String name = ((JSONObject) sourceValue).optString("name", "").trim();
            return name.isEmpty() ? "Régie essence Québec" : name;
        }
        String raw = sourceValue == null ? "" : String.valueOf(sourceValue).trim();
        if (raw.startsWith("{") && raw.endsWith("}")) {
            try {
                String name = new JSONObject(raw).optString("name", "").trim();
                if (!name.isEmpty()) return name;
            } catch (JSONException ignored) {}
        }
        if (raw.isEmpty() || "null".equalsIgnoreCase(raw)) return "Régie essence Québec";
        return raw;
    }

    private void sortStations(List<Station> stations, String sort) {
        if ("distance".equals(sort)) {
            stations.sort(Comparator.comparingDouble(s -> s.distance));
        } else if ("price".equals(sort)) {
            stations.sort(Comparator.comparingDouble((Station s) -> s.price).thenComparingDouble(s -> s.distance));
        } else {
            stations.sort(Comparator.comparingDouble((Station s) -> s.price + s.distance).thenComparingDouble(s -> s.distance));
        }
    }

    private boolean isLanaudiere(String city) {
        String n = city.trim().toLowerCase(Locale.CANADA_FRENCH).replace('’','\'');
        return lanaudiereCities.contains(n);
    }

    private void showStations(List<Station> stations, String source, String fuel, String sort) {
        results.removeAllViews();
        String orderText = "distance".equals(sort) ? " • du plus près au plus loin" : ("price".equals(sort) ? " • du moins cher au plus cher" : " • meilleur compromis prix/distance");
        status.setText("Source : " + source + orderText + " • prix récents, non garantis en temps réel à la pompe.");
        if (stations.isEmpty()) {
            bestBanner.setText("Aucune station de Lanaudière trouvée dans ce rayon");
            results.addView(text("Essaie un rayon plus grand ou actualise ta position.", 15, false, Color.DKGRAY));
            return;
        }
        Station cheapest = Collections.min(stations, Comparator.comparingDouble(s -> s.price));
        bestBanner.setText(String.format(Locale.CANADA_FRENCH, "🏆 MOINS CHER : %.1f ¢/L • %s • %.1f km", cheapest.price, cheapest.name, cheapest.distance));
        notifyBest(cheapest, fuel);

        int rank = 1;
        for (Station s : stations) {
            LinearLayout c = card();
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView r = text("#" + rank, 16, true, s == cheapest ? blue : Color.DKGRAY);
            top.addView(r, new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView name = text(s.name, 17, true, Color.rgb(25,25,25));
            top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView p = text(String.format(Locale.CANADA_FRENCH, "%.1f ¢", s.price), 21, true, blue);
            top.addView(p);
            c.addView(top);
            c.addView(text(s.address + (s.city.isEmpty()?"":" • " + s.city), 13, false, Color.DKGRAY));
            c.addView(text(String.format(Locale.CANADA_FRENCH, "📍 %.1f km", s.distance), 14, true, Color.DKGRAY));
            Button route = secondaryButton("ITINÉRAIRE");
            route.setOnClickListener(v -> openMap(s));
            c.addView(route, matchWrap(dp(8)));
            results.addView(c, matchWrap(dp(10)));
            rank++;
        }
    }

    private void openMap(Station s) {
        Uri uri = Uri.parse("geo:" + s.lat + "," + s.lng + "?q=" + s.lat + "," + s.lng + "(" + Uri.encode(s.name) + ")");
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        try { startActivity(i); } catch (Exception e) { openUrl("https://www.google.com/maps/search/?api=1&query=" + s.lat + "," + s.lng); }
    }

    private void openUrl(String u) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }

    private void notifyBest(Station s, String fuel) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Notification n = new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(quebec.lanaudiere.essence.R.drawable.ic_launcher)
            .setContentTitle("Essence Lanaudière • meilleur prix")
            .setContentText(String.format(Locale.CANADA_FRENCH, "%.1f ¢/L chez %s • %.1f km", s.price, s.name, s.distance))
            .setAutoCancel(true).build();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(7, n);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Meilleur prix d'essence", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Alerte lorsque l’application trouve la station au prix le plus bas autour de toi.");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n=in.read(buf))!=-1) out.write(buf,0,n);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(14),dp(14),dp(14),dp(14));
        l.setBackground(round(Color.WHITE,18));
        l.setElevation(dp(2));
        return l;
    }
    private TextView label(String s) { TextView t=text(s,12,true,Color.GRAY); t.setPadding(0,dp(8),0,dp(3)); return t; }
    private Spinner spinner(String[] items) { Spinner s=new Spinner(this); s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items)); return s; }
    private Button button(String s) { Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackground(round(blue,14)); return b; }
    private Button secondaryButton(String s) { Button b=new Button(this); b.setText(s); b.setTextColor(blue); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackground(round(Color.rgb(233,242,255),14)); return b; }
    private TextView text(String s,int sp,boolean bold,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private GradientDrawable round(int color,int radiusDp) { GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radiusDp)); return g; }
    private LinearLayout.LayoutParams matchWrap(int bottom) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); p.bottomMargin=bottom; return p; }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }

    static class Station {
        final String name,address,city;
        final double lat,lng,price,distance;
        Station(String n,String a,String c,double la,double ln,double p,double d){name=n;address=a;city=c;lat=la;lng=ln;price=p;distance=d;}
    }
}
