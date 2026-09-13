package quebec.culture.donnees;

import android.app.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        root.addView(text("Données culturelles en direct",28,BLUE,true));
        root.addView(text("Consultez directement des listes publiques sans avoir à trouver vous-même un fichier Excel.",15,TEXT,false),mb());

        LinearLayout help = panel();
        help.addView(text("D'où viennent ces listes?",20,TEXT,true));
        help.addView(text("L'application interroge l'API publique de Données Québec. Elle récupère les jeux de données publiés par les organismes publics, puis les affiche ici sous forme de fiches lisibles. Les données restent attribuées à leur organisme diffuseur.",14,TEXT,false));
        root.addView(help,mb());

        LinearLayout choices = panel();
        choices.addView(text("Choisir une liste",20,TEXT,true));
        Button events = secondary("📅  Événements culturels partout au Québec");
        events.setOnClickListener(v -> loadPackage("sit-quebec-evenements","Événements culturels du Québec"));
        choices.addView(events);
        Button places = secondary("🏛  Culture, arts et patrimoine au Québec");
        places.setOnClickListener(v -> loadPackage("sit-quebec-culture-art-et-patrimoine","Culture, arts et patrimoine"));
        choices.addView(places);
        Button mtl = secondary("📍  Événements publics de Montréal");
        mtl.setOnClickListener(v -> loadPackage("vmtl-evenements-publics","Événements publics de Montréal"));
        choices.addView(mtl);
        Button all = secondary("🔎  Explorer les jeux de données culturels");
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
                if(resources != null) {
                    for(int i=0;i<resources.length();i++) {
                        JSONObject r = resources.getJSONObject(i);
                        if(r.optBoolean("datastore_active",false)) { selected = r; break; }
                    }
                }
                String source = pkg.optString("organization","");
                try { source = pkg.getJSONObject("organization").optString("title",source); } catch(Exception ignored) {}
                String license = pkg.optString("license_title","");
                String modified = pkg.optString("metadata_modified","");
                if(selected != null) {
                    String resourceId = selected.optString("id");
                    String dataUrl = "https://www.donneesquebec.ca/recherche/api/3/action/datastore_search?resource_id="+URLEncoder.encode(resourceId,"UTF-8")+"&limit=100";
                    JSONObject data = new JSONObject(get(dataUrl)).getJSONObject("result");
                    JSONArray records = data.optJSONArray("records");
                    showRecords(title,source,license,modified,records);
                } else {
                    JSONArray copy = resources;
                    showResources(title,source,license,modified,copy);
                }
            } catch(Exception e) {
                showError("Impossible de charger cette liste pour le moment.\n"+safe(e.getMessage()));
            }
        }).start();
    }

    private void searchCulture() {
        setLoading("Recherche des jeux de données culturels…");
        new Thread(() -> {
            try {
                String url = "https://www.donneesquebec.ca/recherche/api/3/action/package_search?q="+URLEncoder.encode("culture","UTF-8")+"&rows=30&sort=metadata_modified%20desc";
                JSONObject result = new JSONObject(get(url)).getJSONObject("result");
                JSONArray arr = result.optJSONArray("results");
                ui.post(() -> {
                    results.removeAllViews();
                    status.setText((arr==null?0:arr.length())+" jeu(x) de données trouvé(s). Touchez un jeu pour voir ses données.");
                    if(arr==null) return;
                    for(int i=0;i<arr.length();i++) {
                        JSONObject p = arr.optJSONObject(i); if(p==null) continue;
                        String name = p.optString("name");
                        String title = p.optString("title",name);
                        String notes = strip(p.optString("notes",""));
                        LinearLayout card=panel();
                        card.addView(text(title,17,TEXT,true));
                        if(!notes.isEmpty()) card.addView(text(shorten(notes,220),13,Color.DKGRAY,false));
                        Button open=secondary("Voir cette liste ›");
                        open.setOnClickListener(v -> loadPackage(name,title));
                        card.addView(open);
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
            status.setText(n+" fiche(s) affichée(s) — données récupérées en direct.");
            LinearLayout head=panel();
            head.addView(text(title,21,TEXT,true));
            head.addView(text("Source : "+(source.isEmpty()?"Données Québec":source)+"\nLicence : "+(license.isEmpty()?"voir la source":license)+"\nMise à jour du jeu : "+dateOnly(modified),13,Color.DKGRAY,false));
            results.addView(head,mb());
            if(records==null||records.length()==0) {
                results.addView(text("Aucune fiche n'est disponible dans le DataStore de cette ressource.",14,Color.DKGRAY,false));
                return;
            }
            for(int i=0;i<records.length();i++) {
                JSONObject row=records.optJSONObject(i); if(row==null) continue;
                LinearLayout card=panel();
                Iterator<String> keys=row.keys(); int shown=0; String heading="Fiche "+(i+1);
                ArrayList<String> lines=new ArrayList<>();
                while(keys.hasNext()) {
                    String k=keys.next(); if(k.startsWith("_")) continue;
                    String val=String.valueOf(row.opt(k));
                    if(val.equals("null")||val.trim().isEmpty()) continue;
                    if(shown==0 && val.length()<100) heading=val;
                    if(shown<7) lines.add(pretty(k)+" : "+shorten(strip(val),180));
                    shown++;
                }
                card.addView(text(shorten(heading,120),16,TEXT,true));
                for(String line:lines) card.addView(text(line,13,Color.rgb(55,70,85),false));
                results.addView(card,mb());
            }
        });
    }

    private void showResources(String title,String source,String license,String modified,JSONArray resources) {
        ui.post(() -> {
            results.removeAllViews();
            status.setText("Ce jeu ne fournit pas actuellement de table directement interrogeable. Les ressources disponibles sont affichées ci-dessous.");
            LinearLayout head=panel(); head.addView(text(title,21,TEXT,true));
            head.addView(text("Source : "+source+"\nLicence : "+license+"\nMise à jour : "+dateOnly(modified),13,Color.DKGRAY,false)); results.addView(head,mb());
            if(resources==null) return;
            for(int i=0;i<resources.length();i++) {
                JSONObject r=resources.optJSONObject(i); if(r==null) continue;
                LinearLayout card=panel();
                card.addView(text(r.optString("name","Ressource "+(i+1)),16,TEXT,true));
                card.addView(text("Format : "+r.optString("format","inconnu"),13,Color.DKGRAY,false));
                results.addView(card,mb());
            }
        });
    }

    private String get(String address) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","CultureDuQuebec/1.3");
        int code=c.getResponseCode();
        InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();
        BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();c.disconnect();
        if(code<200||code>=300)throw new IOException("HTTP "+code);return sb.toString();
    }

    private void setLoading(String s){ui.post(()->{status.setText(s);results.removeAllViews();ProgressBar p=new ProgressBar(this);results.addView(p);});}
    private void showError(String s){ui.post(()->{results.removeAllViews();status.setText(s);});}
    private String pretty(String s){return s.replace('_',' ').replace('-',' ');}
    private String strip(String s){return s==null?"":s.replaceAll("<[^>]*>"," ").replace("&nbsp;"," ").replaceAll("\\s+"," ").trim();}
    private String shorten(String s,int max){return s.length()<=max?s:s.substring(0,max-1)+"…";}
    private String dateOnly(String s){if(s==null||s.isEmpty())return "non indiquée";return s.length()>=10?s.substring(0,10):s;}
    private String safe(String s){return s==null?"Erreur réseau":s;}

    private void applyInsets(View v){v.setOnApplyWindowInsetsListener((view,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());view.setPadding(dp(18),dp(18)+bars.top,dp(18),dp(34)+bars.bottom);return in;});v.requestApplyInsets();}
    private LinearLayout panel(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(16),dp(16),dp(16),dp(16));p.setBackgroundResource(R.drawable.panel);return p;}
    private TextView text(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);t.setLineSpacing(0,1.12f);return t;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(BLUE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_secondary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);b.setLayoutParams(lp);return b;}
    private LinearLayout.LayoutParams mb(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
