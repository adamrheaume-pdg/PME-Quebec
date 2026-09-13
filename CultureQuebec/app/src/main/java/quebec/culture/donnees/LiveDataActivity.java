package quebec.culture.donnees;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LiveDataActivity extends Activity {
    private static final int BLUE = Color.rgb(0,59,113);
    private static final int TEXT = Color.rgb(28,45,62);
    private static final int GOLD = Color.rgb(225,165,55);
    private LinearLayout results;
    private TextView status;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(0,40,77));
        getWindow().setNavigationBarColor(Color.rgb(0,40,77));
        setContentView(build());
    }

    private View build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(34));
        root.setBackgroundColor(Color.rgb(245,247,250));
        scroll.addView(root,new ScrollView.LayoutParams(-1,-1));
        applyInsets(root);

        root.addView(text("Culture directe — Québec",28,BLUE,true));
        root.addView(text("L’application transforme les données brutes en fiches visuelles : titre, catégorie, date, lieu, image et lien officiel quand ces informations existent.",15,TEXT,false),mb());

        LinearLayout choices = panel();
        choices.addView(text("Découvrir nos richesses culturelles",20,TEXT,true));
        choices.addView(text("Source : API Données Québec · les informations restent attribuées à leur organisme diffuseur.",13,Color.DKGRAY,false));
        Button events = secondary("📅  Tous les événements");
        events.setOnClickListener(v -> loadPackage("sit-quebec-evenements","Événements - Système d’information touristique Québec (SIT Québec)"));
        choices.addView(events);
        Button places = secondary("🏛  Culture et patrimoine");
        places.setOnClickListener(v -> loadPackage("sit-quebec-culture-art-et-patrimoine","Culture, arts et patrimoine"));
        choices.addView(places);
        Button mtl = secondary("📍  Événements de Montréal");
        mtl.setOnClickListener(v -> loadPackage("vmtl-evenements-publics","Événements publics"));
        choices.addView(mtl);
        Button all = secondary("🔎  Explorer les jeux culturels");
        all.setOnClickListener(v -> searchCulture());
        choices.addView(all);
        root.addView(choices,mb());

        status = text("Choisissez une source ci-dessus.",14,Color.DKGRAY,false);
        root.addView(status,mb());
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results);
        return scroll;
    }

    private void loadPackage(String id,String title) {
        setLoading("Chargement de « "+title+" »…");
        new Thread(() -> {
            try {
                String base = "https://www.donneesquebec.ca/recherche/api/3/action/package_show?id=" + URLEncoder.encode(id,"UTF-8");
                JSONObject pkg = new JSONObject(get(base)).getJSONObject("result");
                JSONArray resources = pkg.optJSONArray("resources");
                JSONObject selected = null;
                if(resources != null) for(int i=0;i<resources.length();i++) {
                    JSONObject r = resources.getJSONObject(i);
                    if(r.optBoolean("datastore_active",false)) { selected = r; break; }
                }
                String source = "Données Québec";
                try { source = pkg.getJSONObject("organization").optString("title",source); } catch(Exception ignored) {}
                String license = pkg.optString("license_title","");
                String modified = pkg.optString("metadata_modified","");
                if(selected != null) {
                    String resourceId = selected.optString("id");
                    String dataUrl = "https://www.donneesquebec.ca/recherche/api/3/action/datastore_search?resource_id="+URLEncoder.encode(resourceId,"UTF-8")+"&limit=100";
                    JSONObject data = new JSONObject(get(dataUrl)).getJSONObject("result");
                    showRecords(title,source,license,modified,data.optJSONArray("records"));
                } else showResources(title,source,license,modified,resources);
            } catch(Exception e) { showError("Impossible de charger cette liste pour le moment.\n"+safe(e.getMessage())); }
        }).start();
    }

    private void searchCulture() {
        setLoading("Recherche des jeux de données culturels…");
        new Thread(() -> {
            try {
                String url = "https://www.donneesquebec.ca/recherche/api/3/action/package_search?q="+URLEncoder.encode("culture","UTF-8")+"&rows=30&sort=metadata_modified%20desc";
                JSONArray arr = new JSONObject(get(url)).getJSONObject("result").optJSONArray("results");
                ui.post(() -> {
                    results.removeAllViews();
                    status.setText((arr==null?0:arr.length())+" jeu(x) de données trouvé(s).");
                    if(arr==null) return;
                    for(int i=0;i<arr.length();i++) {
                        JSONObject p=arr.optJSONObject(i); if(p==null) continue;
                        String name=p.optString("name"); String title=p.optString("title",name);
                        LinearLayout card=panel(); card.addView(text(title,17,TEXT,true));
                        String notes=strip(p.optString("notes","")); if(!notes.isEmpty()) card.addView(text(shorten(notes,220),13,Color.DKGRAY,false));
                        Button open=secondary("Voir les fiches ›"); open.setOnClickListener(v->loadPackage(name,title)); card.addView(open);
                        results.addView(card,mb());
                    }
                });
            } catch(Exception e) { showError("Recherche impossible.\n"+safe(e.getMessage())); }
        }).start();
    }

    private void showRecords(String title,String source,String license,String modified,JSONArray records) {
        ui.post(() -> {
            results.removeAllViews();
            int n=records==null?0:records.length();
            status.setText(n+" fiche(s) affichée(s) — données interprétées automatiquement.");
            LinearLayout head=panel();
            head.addView(text(title,21,TEXT,true));
            head.addView(text("Source : "+source+"\nLicence : "+(license.isEmpty()?"voir la source":license)+"\nMise à jour : "+dateOnly(modified),13,Color.DKGRAY,false));
            results.addView(head,mb());
            if(records==null||records.length()==0){results.addView(text("Aucune fiche disponible.",14,Color.DKGRAY,false));return;}
            for(int i=0;i<records.length();i++) {
                JSONObject row=records.optJSONObject(i); if(row==null) continue;
                results.addView(eventCard(row,source),mb());
            }
        });
    }

    private View eventCard(JSONObject row,String source) {
        LinearLayout card=panel();
        String title=first(row,"NomEtablissement","titre","Titre","nom","Nom","name","Name","evenement","Evenement","description_courte");
        if(title.isEmpty()) title="Fiche culturelle";
        String type=first(row,"TypeEtablissement","type","Type","categorie","Categorie","category","discipline");
        String theme=first(row,"Theme","theme","genre","Genre","sous_categorie");
        String date=first(row,"DateDebut","dateDebut","date_debut","date","Date","start_date","debut");
        String end=first(row,"DateFin","dateFin","date_fin","end_date","fin");
        String city=first(row,"Ville","ville","municipalite","Municipalite","city","RegionTouristique","RegionAdministrative");
        String place=first(row,"Lieu","lieu","adresse","Adresse","location","NomEtablissement");
        String url=firstUrl(row,false);
        String image=firstUrl(row,true);

        if(!image.isEmpty()) {
            ImageView iv=new ImageView(this); iv.setScaleType(ImageView.ScaleType.CENTER_CROP); iv.setBackgroundColor(Color.rgb(225,230,236));
            card.addView(iv,new LinearLayout.LayoutParams(-1,dp(180))); loadImage(iv,image);
        }
        card.addView(text(title,18,TEXT,true));
        LinearLayout badges=new LinearLayout(this); badges.setOrientation(LinearLayout.HORIZONTAL); badges.setPadding(0,dp(6),0,dp(4));
        if(!type.isEmpty()) badges.addView(badge(type)); if(!theme.isEmpty()&&!theme.equals(type)) badges.addView(badge(theme));
        if(badges.getChildCount()>0) card.addView(badges);
        if(!date.isEmpty()) card.addView(text("📅  "+friendlyDate(date)+(end.isEmpty()?"":" → "+friendlyDate(end)),14,TEXT,false));
        String where=!place.isEmpty()?place:city; if(!city.isEmpty()&&!where.contains(city)) where += " · "+city;
        if(!where.isEmpty()) card.addView(text("📍  "+where,14,TEXT,false));

        String desc=first(row,"Description","description","Resume","resume","details","DescriptionDetaillee");
        if(!desc.isEmpty()) card.addView(text(shorten(strip(desc),220),13,Color.DKGRAY,false));

        if(!url.isEmpty()) {
            Button info=primarySmall("PLUS D’INFO");
            info.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}});
            card.addView(info);
        }
        card.addView(text("Source officielle : "+source,12,Color.rgb(70,85,100),false));
        return card;
    }

    private TextView badge(String value){TextView b=text(shorten(value,28),12,Color.rgb(65,45,0),true);b.setPadding(dp(9),dp(4),dp(9),dp(4));b.setBackgroundColor(Color.rgb(247,205,111));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=dp(6);b.setLayoutParams(lp);return b;}

    private String first(JSONObject o,String... keys){for(String k:keys){Object v=o.opt(k);if(v!=null&&!JSONObject.NULL.equals(v)){String s=strip(String.valueOf(v));if(!s.isEmpty()&&!s.equalsIgnoreCase("null"))return s;}}return "";}
    private String firstUrl(JSONObject o,boolean imageOnly){Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();Object v=o.opt(k);if(v==null||JSONObject.NULL.equals(v))continue;String s=String.valueOf(v);String lower=s.toLowerCase(Locale.ROOT);boolean keyImage=k.toLowerCase(Locale.ROOT).matches(".*(image|photo|media|visuel|thumbnail).*?");boolean looksImage=lower.matches(".*\\.(jpg|jpeg|png|webp)(\\?.*)?$");if(imageOnly&&(keyImage||looksImage)){String u=extractUrl(s);if(!u.isEmpty())return u;}if(!imageOnly&&(k.toLowerCase(Locale.ROOT).contains("url")||k.toLowerCase(Locale.ROOT).contains("lien")||k.toLowerCase(Locale.ROOT).contains("site"))){String u=extractUrl(s);if(!u.isEmpty()&&!looksImage)return u;}}return "";}
    private String extractUrl(String s){int p=s.indexOf("http");if(p<0)return "";String u=s.substring(p).trim();int cut=u.indexOf(' ');if(cut>0)u=u.substring(0,cut);u=u.replace("\"","").replace("]","").replace("}","");return u;}
    private String friendlyDate(String s){s=strip(s);if(s.length()>=10&&s.charAt(4)=='-'&&s.charAt(7)=='-')return s.substring(8,10)+"/"+s.substring(5,7)+"/"+s.substring(0,4);return shorten(s,30);}

    private void loadImage(ImageView iv,String url){new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","CultureDuQuebec/1.4");InputStream in=c.getInputStream();Bitmap bm=BitmapFactory.decodeStream(in);in.close();c.disconnect();if(bm!=null)ui.post(()->iv.setImageBitmap(bm));}catch(Exception ignored){}}).start();}

    private void showResources(String title,String source,String license,String modified,JSONArray resources) {
        ui.post(() -> {results.removeAllViews();status.setText("Cette source fournit des fichiers plutôt qu’une table interrogeable.");LinearLayout head=panel();head.addView(text(title,21,TEXT,true));head.addView(text("Source : "+source+"\nLicence : "+license+"\nMise à jour : "+dateOnly(modified),13,Color.DKGRAY,false));results.addView(head,mb());if(resources==null)return;for(int i=0;i<resources.length();i++){JSONObject r=resources.optJSONObject(i);if(r==null)continue;LinearLayout card=panel();card.addView(text(r.optString("name","Ressource "+(i+1)),16,TEXT,true));card.addView(text("Format : "+r.optString("format","inconnu"),13,Color.DKGRAY,false));String u=r.optString("url","");if(!u.isEmpty()){Button b=secondary("Ouvrir la ressource");b.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u))));card.addView(b);}results.addView(card,mb());}});
    }

    private String get(String address) throws Exception {HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","CultureDuQuebec/1.4");int code=c.getResponseCode();InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();c.disconnect();if(code<200||code>=300)throw new IOException("HTTP "+code);return sb.toString();}
    private void setLoading(String s){ui.post(()->{status.setText(s);results.removeAllViews();results.addView(new ProgressBar(this));});}
    private void showError(String s){ui.post(()->{results.removeAllViews();status.setText(s);});}
    private String strip(String s){return s==null?"":s.replaceAll("<[^>]*>"," ").replace("&nbsp;"," ").replaceAll("\\s+"," ").trim();}
    private String shorten(String s,int max){return s.length()<=max?s:s.substring(0,max-1)+"…";}
    private String dateOnly(String s){if(s==null||s.isEmpty())return "non indiquée";return s.length()>=10?s.substring(0,10):s;}
    private String safe(String s){return s==null?"Erreur réseau":s;}

    private void applyInsets(View v){v.setOnApplyWindowInsetsListener((view,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());view.setPadding(dp(18),dp(18)+bars.top,dp(18),dp(34)+bars.bottom);return in;});v.requestApplyInsets();}
    private LinearLayout panel(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(16),dp(16),dp(16),dp(16));p.setBackgroundResource(R.drawable.panel);return p;}
    private TextView text(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);t.setLineSpacing(0,1.12f);return t;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(BLUE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_secondary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);b.setLayoutParams(lp);return b;}
    private Button primarySmall(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_primary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.topMargin=dp(8);lp.bottomMargin=dp(8);b.setLayoutParams(lp);return b;}
    private LinearLayout.LayoutParams mb(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
