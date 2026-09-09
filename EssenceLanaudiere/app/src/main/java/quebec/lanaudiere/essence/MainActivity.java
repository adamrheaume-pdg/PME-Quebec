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
    private static final int REQ_LOCATION=101, REQ_NOTIFICATIONS=102;
    private static final String CHANNEL_ID="best_price";
    private final int navy=Color.rgb(57,82,118), navyDark=Color.rgb(30,45,67), ink=Color.rgb(45,52,67), accent=Color.rgb(30,125,240), pale=Color.rgb(246,248,251);
    private LinearLayout page, results;
    private TextView status, hero;
    private Spinner fuelSpinner, sortSpinner, radiusSpinner;
    private Location lastLocation;
    private List<Station> lastStations=new ArrayList<>();
    private int introPage=0;

    private final Set<String> lanaudiereCities=new HashSet<>(Arrays.asList(
        "berthierville","charlemagne","chertsey","crabtree","entrelacs","joliette","lanoraie","l'assomption","l'épiphanie","lavaltrie","mandeville","mascouche","notre-dame-de-la-merci","notre-dame-de-lourdes","notre-dame-des-prairies","rawdon","repentigny","saint-alexis","saint-alphonse-rodriguez","saint-ambroise-de-kildare","saint-barthélemy","saint-calixte","saint-charles-borromée","saint-côme","saint-cuthbert","saint-damien","saint-didace","saint-donat","saint-esprit","saint-félix-de-valois","saint-gabriel","saint-gabriel-de-brandon","saint-ignace-de-loyola","saint-jacques","saint-jean-de-matha","saint-liguori","saint-lin-laurentides","saint-michel-des-saints","saint-norbert","saint-paul","saint-pierre","saint-roch-de-l'achigan","saint-roch-ouest","saint-sulpice","saint-thomas","sainte-béatrix","sainte-élisabeth","sainte-émélie-de-l'énergie","sainte-geneviève-de-berthier","sainte-julienne","sainte-marcelline-de-kildare","sainte-marie-salomé","sainte-mélanie","terrebonne","la visitation-de-l'île-dupas"));

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(18,18,18));
        getWindow().setNavigationBarColor(Color.rgb(18,18,18));
        createNotificationChannel();
        showIntro();
    }

    private void showIntro(){
        String[] titles={"Paye ton gaz\nmoins cher !","Compare les\nprix à proximité","Anticipe les\nhausses de prix","Trouve la station\nla moins chère","Économise\nmaintenant","Suis tes stations\nfavorites"};
        String[] subtitles={"Prix synchronisés avec les données ouvertes de la Régie !","Compare prix, distance et meilleur compromis autour de toi.","Visualise la tendance locale et repère rapidement les mouvements de prix.","Localise en un coup d’œil les stations les plus intéressantes.","Calcule le coût d’un trajet selon ta consommation et le prix du litre.","Garde tes stations préférées sous la main et retrouve-les rapidement."};
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackgroundColor(navy);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setPadding(dp(26),dp(26),dp(26),dp(28)); sc.addView(root);
        TextView back=text("‹",42,false,Color.WHITE); back.setGravity(Gravity.START); back.setOnClickListener(v->{ if(introPage>0){introPage--;showIntro();} else finish();}); root.addView(back,new LinearLayout.LayoutParams(-1,dp(54)));
        if(introPage==0){
            ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.essence_quebec_logo); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE); root.addView(logo,new LinearLayout.LayoutParams(-1,dp(210)));
            TextView brand=text("Essence Québec",24,false,Color.WHITE); brand.setGravity(Gravity.CENTER); root.addView(brand);
        }
        TextView title=text(titles[introPage], introPage==0?38:42,true,Color.WHITE); title.setGravity(Gravity.CENTER); title.setLineSpacing(0,1.08f); title.setPadding(0,dp(18),0,dp(28)); root.addView(title);
        LinearLayout visual=cardWhite(); visual.setMinimumHeight(dp(330)); visual.setGravity(Gravity.CENTER_HORIZONTAL); visual.setPadding(dp(16),dp(20),dp(16),dp(20));
        if(introPage==0){
            TextView bubble=text(subtitles[0],20,false,Color.WHITE); bubble.setGravity(Gravity.CENTER); bubble.setPadding(dp(16),dp(14),dp(16),dp(14)); bubble.setBackground(round(Color.rgb(95,185,226),16)); visual.setBackgroundColor(Color.TRANSPARENT); visual.addView(bubble,new LinearLayout.LayoutParams(-1,-2));
        } else if(introPage==1){
            visual.addView(fakeSearchBar());
            visual.addView(fakeStation("8.5 km","Esso","3100 Bd Moïse-Vincent","171.8",true));
            visual.addView(fakeStation("9.1 km","Petro-Canada","5995 Bd Cousineau","181.8",false));
            visual.addView(fakeStation("6.9 km","Costco","Bd Saint-Bruno","181.9",true));
        } else if(introPage==2){
            visual.addView(text("TENDANCES DES PRIX PAR RÉGION",17,false,ink));
            TextView n=text("184.0 ¢/litre",44,true,ink); n.setGravity(Gravity.CENTER); n.setPadding(0,dp(22),0,dp(12)); visual.addView(n);
            LinearLayout alert=cardDark(); alert.addView(text("PROBABILITÉ DE HAUSSE",14,false,Color.WHITE)); alert.addView(text("Élevée    80%",32,true,Color.WHITE)); visual.addView(alert,new LinearLayout.LayoutParams(-1,-2));
            TextView warn=text("●  Hausse très probable\nNos indicateurs pointent vers une hausse possible dans les prochaines 72 heures.",16,true,Color.rgb(130,20,20)); warn.setPadding(dp(12),dp(14),dp(12),dp(14)); visual.addView(warn);
        } else if(introPage==3){
            visual.addView(fakeSearchBar());
            TextView map=text("📍\n\nCARTE DES STATIONS\n\n⛽ 159.9      ⛽ 152.9\n\n     ● VOUS\n\n⛽ 153.9      ⛽ 156.9",22,true,ink); map.setGravity(Gravity.CENTER); map.setBackground(round(Color.rgb(225,233,240),8)); visual.addView(map,new LinearLayout.LayoutParams(-1,dp(270)));
        } else if(introPage==4){
            visual.addView(text("CALCULATEUR DE TRAJET",19,false,ink));
            visual.addView(calcPreviewRow("Consommation d'essence","9.2 L/100 km"));
            visual.addView(calcPreviewRow("Distance à parcourir","250 km"));
            visual.addView(calcPreviewRow("Prix de l'essence","159.9 ¢/litre"));
            TextView total=text("36.78 $",46,true,ink); total.setGravity(Gravity.CENTER); total.setPadding(0,dp(16),0,0); visual.addView(total);
            TextView litres=text("23.00 litres",32,true,ink); litres.setGravity(Gravity.CENTER); visual.addView(litres);
        } else {
            visual.addView(text("TENDANCE DES DEUX DERNIÈRES SEMAINES",19,true,ink));
            TextView chart=text("200 ┤                 ╭──\n195 ┤             ╭───╯\n190 ┤         ╭───╯\n185 ┤    ╭────╯\n180 ┤────╯",18,false,accent); chart.setTypeface(Typeface.MONOSPACE); chart.setPadding(0,dp(20),0,dp(20)); visual.addView(chart);
            visual.addView(fakeStation("4.0 km","Costco","3233 Av. Watt","184.9",true));
            visual.addView(fakeStation("20.0 km","Costco","300 Rue Bridge","188.9",true));
        }
        root.addView(visual,new LinearLayout.LayoutParams(-1,-2));
        TextView desc=text(subtitles[introPage],16,false,Color.WHITE); desc.setGravity(Gravity.CENTER); desc.setPadding(0,dp(20),0,dp(14)); root.addView(desc);
        LinearLayout buttons=new LinearLayout(this); buttons.setGravity(Gravity.CENTER); buttons.setPadding(0,dp(10),0,0);
        Button skip=ghostButton("PASSER"); skip.setOnClickListener(v->showMain()); buttons.addView(skip,new LinearLayout.LayoutParams(0,dp(52),1));
        Button next=primaryButton(introPage==titles.length-1?"COMMENCER":"SUIVANT"); next.setOnClickListener(v->{if(introPage<titles.length-1){introPage++;showIntro();}else showMain();}); LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,dp(52),1); np.leftMargin=dp(10); buttons.addView(next,np); root.addView(buttons,new LinearLayout.LayoutParams(-1,-2));
        setContentView(sc);
    }

    private void showMain(){
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackgroundColor(navy);
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(18),dp(18),dp(18),dp(26)); page.setBackgroundColor(navy); sc.addView(page);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.essence_quebec_logo); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE); header.addView(logo,new LinearLayout.LayoutParams(dp(82),dp(82)));
        LinearLayout ht=new LinearLayout(this); ht.setOrientation(LinearLayout.VERTICAL); TextView name=text("ESSENCE QUÉBEC",24,true,Color.WHITE); TextView tag=text("Paye ton gaz moins cher",14,false,Color.rgb(205,220,240)); ht.addView(name); ht.addView(tag); header.addView(ht,new LinearLayout.LayoutParams(0,-2,1)); page.addView(header);
        TextView title=text("Compare les\nprix à proximité",38,true,Color.WHITE); title.setPadding(0,dp(12),0,dp(18)); page.addView(title);
        LinearLayout search=cardWhite(); search.setPadding(dp(12),dp(12),dp(12),dp(12));
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        EditText q=new EditText(this); q.setHint("🔍 Rechercher ici"); q.setTextSize(16); q.setSingleLine(true); q.setBackground(round(Color.rgb(51,60,80),8)); q.setHintTextColor(Color.LTGRAY); q.setTextColor(Color.WHITE); row.addView(q,new LinearLayout.LayoutParams(0,dp(52),1));
        fuelSpinner=spinner(new String[]{"Ordinaire","Super","Diesel"}); LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(118),dp(52)); fp.leftMargin=dp(8); row.addView(fuelSpinner,fp); search.addView(row);
        LinearLayout filters=new LinearLayout(this); filters.setGravity(Gravity.CENTER_VERTICAL); sortSpinner=spinner(new String[]{"Prix / km","Moins cher","Plus proche"}); sortSpinner.setSelection(2); radiusSpinner=spinner(new String[]{"5 km","10 km","20 km","30 km"}); radiusSpinner.setSelection(3); filters.addView(sortSpinner,new LinearLayout.LayoutParams(0,dp(52),1)); LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(52),1); rp.leftMargin=dp(8); filters.addView(radiusSpinner,rp); Button refresh=primaryButton("↻"); refresh.setOnClickListener(v->locateAndLoad()); LinearLayout.LayoutParams rb=new LinearLayout.LayoutParams(dp(58),dp(52)); rb.leftMargin=dp(8); filters.addView(refresh,rb); search.addView(filters); page.addView(search);
        hero=text("📍 Localisation en cours…",17,true,Color.WHITE); hero.setPadding(dp(14),dp(13),dp(14),dp(13)); hero.setBackground(round(navyDark,10)); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=dp(10); page.addView(hero,hp);
        status=text("Prix récents • données ouvertes • vérifie le prix à la pompe",12,false,Color.rgb(220,228,238)); status.setPadding(0,dp(8),0,dp(10)); page.addView(status);
        results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); page.addView(results);
        addTools();
        setContentView(sc);
        requestPermissionsAndLocate();
    }

    private void addTools(){
        TextView h=text("Outils Essence Québec",24,true,Color.WHITE); h.setPadding(0,dp(18),0,dp(8)); page.addView(h);
        LinearLayout grid=new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        Button map=toolButton("🗺  TROUVE LA STATION LA MOINS CHÈRE"); map.setOnClickListener(v->{if(!lastStations.isEmpty()) openMap(lastStations.get(0)); else locateAndLoad();}); grid.addView(map);
        Button trend=toolButton("📈  ANTICIPE LES HAUSSES DE PRIX"); trend.setOnClickListener(v->showTrendDialog()); grid.addView(trend);
        Button calc=toolButton("🧮  CALCULATEUR DE TRAJET"); calc.setOnClickListener(v->showCalculator()); grid.addView(calc);
        Button fav=toolButton("★  SUIS TES STATIONS FAVORITES"); fav.setOnClickListener(v->showFavorites()); grid.addView(fav); page.addView(grid);
        TextView footer=text("Créé au Québec • adamrheaume@gmail.com",12,true,Color.rgb(210,222,238)); footer.setGravity(Gravity.CENTER); footer.setPadding(0,dp(20),0,dp(8)); page.addView(footer);
    }

    private View fakeSearchBar(){ LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); TextView s=text("🔍",24,false,Color.WHITE); s.setGravity(Gravity.CENTER); s.setBackground(round(Color.rgb(52,61,80),8)); r.addView(s,new LinearLayout.LayoutParams(0,dp(50),1)); TextView f=text("Ordinaire⌄",15,false,ink); f.setGravity(Gravity.CENTER); r.addView(f,new LinearLayout.LayoutParams(dp(112),dp(50))); return r; }
    private View fakeStation(String d,String n,String a,String p,boolean star){ LinearLayout c=cardStation(); LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); TextView dist=text(d,14,true,Color.WHITE); dist.setGravity(Gravity.CENTER); dist.setBackground(round(Color.rgb(61,70,91),6)); top.addView(dist,new LinearLayout.LayoutParams(dp(86),dp(32))); TextView nm=text(n,19,true,ink); nm.setGravity(Gravity.CENTER); top.addView(nm,new LinearLayout.LayoutParams(0,-2,1)); top.addView(text(star?"★":"☆",26,true,star?Color.rgb(255,188,0):Color.LTGRAY)); c.addView(top); LinearLayout body=new LinearLayout(this); body.setGravity(Gravity.CENTER_VERTICAL); TextView addr=text(a,14,false,ink); body.addView(addr,new LinearLayout.LayoutParams(0,-2,1)); TextView price=text(p,32,true,ink); price.setGravity(Gravity.CENTER); price.setBackground(round(Color.WHITE,4)); body.addView(price,new LinearLayout.LayoutParams(dp(130),dp(74))); c.addView(body); return c; }
    private View calcPreviewRow(String a,String b){ LinearLayout c=cardStation(); c.setGravity(Gravity.CENTER_HORIZONTAL); TextView t=text(a,16,true,ink); t.setGravity(Gravity.CENTER); c.addView(t); TextView v=text(b,21,false,ink); v.setGravity(Gravity.CENTER); v.setPadding(0,dp(6),0,0); c.addView(v); return c; }

    private boolean hasLocationPermission(){return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    private void requestPermissionsAndLocate(){ if(!hasLocationPermission()){ hero.setText("📍 Autorisation GPS requise"); requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION);} else {locateAndLoad();requestNotificationPermissionIfNeeded();} }
    private void requestNotificationPermissionIfNeeded(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);}
    @Override public void onRequestPermissionsResult(int q,String[] p,int[] g){super.onRequestPermissionsResult(q,p,g);if(q==REQ_LOCATION){if(hasLocationPermission()){locateAndLoad();requestNotificationPermissionIfNeeded();}else hero.setText("GPS requis pour trouver les stations proches");}}

    private void locateAndLoad(){
        if(hero==null)return; if(!hasLocationPermission()){requestPermissionsAndLocate();return;} hero.setText("📍 Recherche de ta position…"); status.setText("Recherche GPS + réseau en cours…");
        LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE); boolean ge=lm.isProviderEnabled(LocationManager.GPS_PROVIDER), ne=lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if(!ge&&!ne){hero.setText("⚠️ Localisation désactivée");try{startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));}catch(Exception ignored){}return;}
        Location best=null; for(String p:Arrays.asList(LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER))try{if(lm.isProviderEnabled(p)){Location l=lm.getLastKnownLocation(p);if(l!=null&&(best==null||l.getTime()>best.getTime()))best=l;}}catch(Exception ignored){}
        if(best!=null&&System.currentTimeMillis()-best.getTime()<900000){lastLocation=best;loadStations(best);return;}
        final Location fallback=best; Handler h=new Handler(Looper.getMainLooper()); boolean[] done={false}; LocationListener listener=new LocationListener(){public void onLocationChanged(Location l){if(done[0]||l==null)return;done[0]=true;h.removeCallbacksAndMessages(null);try{lm.removeUpdates(this);}catch(Exception ignored){}lastLocation=l;loadStations(l);} public void onProviderEnabled(String p){} public void onProviderDisabled(String p){} public void onStatusChanged(String p,int s,Bundle e){}};
        try{if(ne)lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,listener,Looper.getMainLooper());if(ge)lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,listener,Looper.getMainLooper());h.postDelayed(()->{if(done[0])return;done[0]=true;try{lm.removeUpdates(listener);}catch(Exception ignored){}if(fallback!=null){lastLocation=fallback;loadStations(fallback);}else{hero.setText("⚠️ Position introuvable");status.setText("Active la localisation précise puis actualise.");}},8000);}catch(Exception e){if(fallback!=null){lastLocation=fallback;loadStations(fallback);}else hero.setText("Impossible d’obtenir la position");}
    }

    private void loadStations(Location l){
        hero.setText("⛽ Recherche des prix dans Lanaudière…"); results.removeAllViews();
        String fuel=new String[]{"ordinaire","super","diesel"}[fuelSpinner.getSelectedItemPosition()];
        int sp=sortSpinner.getSelectedItemPosition(); String sort=sp==1?"price":sp==2?"distance":"value";
        int radius=new int[]{5,10,20,30}[radiusSpinner.getSelectedItemPosition()];
        String u=String.format(Locale.US,"https://www.gasquebec.ca/api/stations/nearby?lat=%.6f&lng=%.6f&radius=%d&fuelType=%s&limit=50&sort=%s",l.getLatitude(),l.getLongitude(),radius,fuel,sort);
        new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","EssenceQuebec/2.0 Android");int code=c.getResponseCode();if(code!=200)throw new IOException("Erreur de données ("+code+")");JSONObject o=new JSONObject(readAll(c.getInputStream()));JSONArray a=o.optJSONArray("stations");String source=parseSourceName(o.opt("source"));List<Station> ss=new ArrayList<>();if(a!=null)for(int i=0;i<a.length();i++){JSONObject s=a.getJSONObject(i);String city=s.optString("city","");if(!isLanaudiere(city)||s.isNull("price"))continue;ss.add(new Station(s.optString("name","Station"),s.optString("address",""),city,s.optDouble("lat"),s.optDouble("lng"),s.optDouble("price"),s.optDouble("distanceKm")));}sortStations(ss,sort);runOnUiThread(()->showStations(ss,source,fuel));}catch(Exception e){runOnUiThread(()->{hero.setText("⚠️ Prix indisponibles");status.setText(e.getMessage()==null?"Impossible de joindre la source de prix.":e.getMessage());});}}).start();
    }

    private void showStations(List<Station> s,String source,String fuel){
        results.removeAllViews(); lastStations=new ArrayList<>(s); status.setText("Source : "+source+" • prix récents • touche une carte pour l’itinéraire.");
        if(s.isEmpty()){hero.setText("Aucune station de Lanaudière trouvée dans ce rayon");return;}
        Station cheap=Collections.min(s,Comparator.comparingDouble(x->x.price)); hero.setText(String.format(Locale.CANADA_FRENCH,"🏆 %.1f ¢/L • %s • %.1f km",cheap.price,cheap.name,cheap.distance)); notifyBest(cheap,fuel);
        int rank=1; for(Station x:s){LinearLayout c=cardStation(); c.setOnClickListener(v->openMap(x)); LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); TextView dist=text(String.format(Locale.CANADA_FRENCH,"%.1f km",x.distance),14,true,Color.WHITE); dist.setGravity(Gravity.CENTER); dist.setBackground(round(ink,6)); top.addView(dist,new LinearLayout.LayoutParams(dp(86),dp(32))); TextView name=text(x.name,19,true,ink); name.setGravity(Gravity.CENTER); top.addView(name,new LinearLayout.LayoutParams(0,-2,1)); Button fav=new Button(this); fav.setText(isFavorite(x)?"★":"☆"); fav.setTextSize(22); fav.setTextColor(isFavorite(x)?Color.rgb(255,188,0):Color.LTGRAY); fav.setBackgroundColor(Color.TRANSPARENT); fav.setOnClickListener(v->{toggleFavorite(x);fav.setText(isFavorite(x)?"★":"☆");fav.setTextColor(isFavorite(x)?Color.rgb(255,188,0):Color.LTGRAY);}); top.addView(fav,new LinearLayout.LayoutParams(dp(54),dp(48))); c.addView(top); LinearLayout body=new LinearLayout(this); body.setGravity(Gravity.CENTER_VERTICAL); TextView addr=text(x.address+"\n"+x.city,14,false,ink); body.addView(addr,new LinearLayout.LayoutParams(0,-2,1)); TextView price=text(String.format(Locale.CANADA_FRENCH,"%.1f ›",x.price),31,true,ink); price.setGravity(Gravity.CENTER); price.setBackground(round(Color.WHITE,4)); body.addView(price,new LinearLayout.LayoutParams(dp(135),dp(74))); c.addView(body); TextView stamp=text(rank==1?"meilleur prix autour de toi":"données récentes",10,false,Color.GRAY); stamp.setGravity(Gravity.END); c.addView(stamp); results.addView(c); rank++; }
    }

    private void showTrendDialog(){ double avg=0,min=0,max=0; if(!lastStations.isEmpty()){min=Double.MAX_VALUE;for(Station s:lastStations){avg+=s.price;min=Math.min(min,s.price);max=Math.max(max,s.price);}avg/=lastStations.size();} LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(12),dp(18),dp(12));box.addView(text("TENDANCES DES PRIX PAR RÉGION",18,false,ink));TextView p=text(lastStations.isEmpty()?"Aucune donnée chargée":String.format(Locale.CANADA_FRENCH,"Moyenne %.1f ¢/L\nMinimum %.1f • Maximum %.1f",avg,min,max),28,true,ink);p.setGravity(Gravity.CENTER);p.setPadding(0,dp(18),0,dp(18));box.addView(p);LinearLayout alert=cardDark();alert.addView(text("INDICATEUR LOCAL",13,false,Color.WHITE));alert.addView(text(lastStations.size()>3?"Prix dispersés • surveille les écarts":"Charge les prix pour analyser",22,true,Color.WHITE));box.addView(alert);new AlertDialog.Builder(this).setTitle("Anticipe les hausses de prix").setView(box).setPositiveButton("FERMER",null).show(); }

    private void showCalculator(){ LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(8),dp(18),dp(8)); EditText cons=input("Consommation L/100 km","9.2"); EditText dist=input("Distance km","250"); EditText price=input("Prix ¢/litre",lastStations.isEmpty()?"159.9":String.format(Locale.US,"%.1f",lastStations.get(0).price)); box.addView(cons);box.addView(dist);box.addView(price); TextView out=text("",28,true,ink);out.setGravity(Gravity.CENTER);out.setPadding(0,dp(14),0,0);box.addView(out); Runnable calc=()->{try{double c=Double.parseDouble(cons.getText().toString().replace(',','.')),d=Double.parseDouble(dist.getText().toString().replace(',','.')),p=Double.parseDouble(price.getText().toString().replace(',','.'));double litres=c*d/100.0,total=litres*p/100.0;out.setText(String.format(Locale.CANADA_FRENCH,"%.2f $\n%.2f litres",total,litres));}catch(Exception e){out.setText("Entre des nombres valides");}}; calc.run(); Button b=primaryButton("CALCULER");b.setOnClickListener(v->calc.run());box.addView(b,new LinearLayout.LayoutParams(-1,dp(52)));new AlertDialog.Builder(this).setTitle("Économise maintenant").setView(box).setNegativeButton("FERMER",null).show(); }

    private void showFavorites(){ Set<String> f=getPreferences(MODE_PRIVATE).getStringSet("favorites",new HashSet<>()); StringBuilder sb=new StringBuilder(); for(String x:f)sb.append("★ ").append(x).append("\n"); if(sb.length()==0)sb.append("Aucune station favorite pour le moment."); new AlertDialog.Builder(this).setTitle("Suis tes stations favorites").setMessage(sb.toString()).setPositiveButton("OK",null).show(); }
    private boolean isFavorite(Station s){return getPreferences(MODE_PRIVATE).getStringSet("favorites",new HashSet<>()).contains(s.name+"|"+s.address);}
    private void toggleFavorite(Station s){SharedPreferences p=getPreferences(MODE_PRIVATE);Set<String> f=new HashSet<>(p.getStringSet("favorites",new HashSet<>()));String k=s.name+"|"+s.address;if(f.contains(k))f.remove(k);else f.add(k);p.edit().putStringSet("favorites",f).apply();}

    private String parseSourceName(Object v){if(v instanceof JSONObject){String n=((JSONObject)v).optString("name","").trim();return n.isEmpty()?"Régie essence Québec":n;}String raw=v==null?"":String.valueOf(v).trim();if(raw.startsWith("{")&&raw.endsWith("}"))try{String n=new JSONObject(raw).optString("name","").trim();if(!n.isEmpty())return n;}catch(JSONException ignored){}return raw.isEmpty()||"null".equalsIgnoreCase(raw)?"Régie essence Québec":raw;}
    private void sortStations(List<Station>s,String sort){if("distance".equals(sort))s.sort(Comparator.comparingDouble(x->x.distance));else if("price".equals(sort))s.sort(Comparator.comparingDouble((Station x)->x.price).thenComparingDouble(x->x.distance));else s.sort(Comparator.comparingDouble((Station x)->x.price+x.distance).thenComparingDouble(x->x.distance));}
    private boolean isLanaudiere(String city){return lanaudiereCities.contains(city.trim().toLowerCase(Locale.CANADA_FRENCH).replace('’','\''));}
    private void openMap(Station s){Uri u=Uri.parse("geo:"+s.lat+","+s.lng+"?q="+s.lat+","+s.lng+"("+Uri.encode(s.name)+")");try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query="+s.lat+","+s.lng)));}}
    private void notifyBest(Station s,String fuel){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;Notification n=new Notification.Builder(this,CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Essence Québec • meilleur prix").setContentText(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L chez %s • %.1f km",s.price,s.name,s.distance)).setAutoCancel(true).build();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(7,n);}
    private void createNotificationChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL_ID,"Meilleur prix d'essence",NotificationManager.IMPORTANCE_DEFAULT);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    private String readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)o.write(b,0,n);return o.toString(StandardCharsets.UTF_8.name());}

    private LinearLayout cardWhite(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setBackground(round(Color.rgb(252,252,253),12));l.setElevation(dp(3));return l;}
    private LinearLayout cardDark(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(14),dp(14),dp(14),dp(14));l.setBackground(round(ink,10));return l;}
    private LinearLayout cardStation(){LinearLayout l=cardWhite();l.setPadding(dp(10),dp(9),dp(10),dp(9));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(9);l.setLayoutParams(p);return l;}
    private Spinner spinner(String[] a){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,a));s.setBackground(round(Color.WHITE,8));return s;}
    private EditText input(String hint,String val){EditText e=new EditText(this);e.setHint(hint);e.setText(val);e.setTextSize(18);e.setSingleLine(true);e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(round(Color.rgb(238,243,248),8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.bottomMargin=dp(8);e.setLayoutParams(p);return e;}
    private Button primaryButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(accent,10));return b;}
    private Button ghostButton(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(navyDark,10));return b;}
    private Button toolButton(String s){Button b=ghostButton(s);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setPadding(dp(16),0,dp(16),0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.topMargin=dp(8);b.setLayoutParams(p);return b;}
    private TextView text(String s,int z,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}

    static class Station{String name,address,city;double lat,lng,price,distance;Station(String n,String a,String c,double la,double lo,double p,double d){name=n;address=a;city=c;lat=la;lng=lo;price=p;distance=d;}}
}
