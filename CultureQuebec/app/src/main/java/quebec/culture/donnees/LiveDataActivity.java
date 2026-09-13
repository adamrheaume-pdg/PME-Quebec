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
import java.util.regex.*;

public class LiveDataActivity extends Activity {
    private static final int BLUE=Color.rgb(0,59,113), TEXT=Color.rgb(28,45,62);
    private LinearLayout results;
    private TextView status,mapStatus;
    private EventMapView mapView;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ArrayList<JSONObject> currentRecords=new ArrayList<>();
    private String currentTitle="",currentSource="",currentLicense="",currentModified="";
    private int currentSort=0;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(0,40,77));
        getWindow().setNavigationBarColor(Color.rgb(0,40,77));
        setContentView(build());
        refreshMapFeeds();
        loadPackage("sit-quebec-evenements","Événements - Système d’information touristique Québec (SIT Québec)",0);
    }

    private View build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(18),dp(18),dp(34));root.setBackgroundColor(Color.rgb(245,247,250));
        scroll.addView(root,new ScrollView.LayoutParams(-1,-1));applyInsets(root);

        root.addView(text("Culture directe — Québec",28,BLUE,true));
        root.addView(text("Les données brutes sont converties en fiches visuelles avec titre, catégorie, date, lieu, image et lien officiel quand la source fournit ces informations.",15,TEXT,false),mb());

        LinearLayout mapPanel=panel();
        mapPanel.setPadding(dp(8),dp(12),dp(8),dp(12));
        mapPanel.addView(text("🗺 Carte culturelle interactive du Québec",21,TEXT,true));
        mapPanel.addView(text("Carte tactile agrandie : glissez, pincez, double-tapez et touchez un point. Les couleurs indiquent le type de contenu : bleu culture, bleu pulsant musique, vert théâtre, vert pulsant film/TV/Web, rouge pulsant patrimoine/histoire, rouge autochtone.",13,Color.DKGRAY,false));
        mapStatus=text("Mise à jour de la carte…",12,Color.rgb(20,108,67),true);mapPanel.addView(mapStatus);
        mapView=new EventMapView(this);LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(-1,dp(540));mlp.topMargin=dp(8);mapPanel.addView(mapView,mlp);
        Button refresh=secondary("↻ Actualiser les événements sur la carte");refresh.setOnClickListener(v->refreshMapFeeds());mapPanel.addView(refresh);
        mapPanel.addView(text("Sources cartographiques : jeux de données publics géolocalisés. Les contenus Web/RSS ne sont placés sur la carte que s’ils fournissent une localisation exploitable; aucune coordonnée n’est inventée.",11,Color.DKGRAY,false));
        root.addView(mapPanel,mb());

        LinearLayout choices=panel();
        choices.addView(text("Découvrir nos richesses culturelles",20,TEXT,true));
        choices.addView(text("Source : API Données Québec · les informations restent attribuées à leur organisme diffuseur.",13,Color.DKGRAY,false));
        Button events=secondary("📅  Tous les événements");events.setOnClickListener(v->loadPackage("sit-quebec-evenements","Événements - Système d’information touristique Québec (SIT Québec)",0));choices.addView(events);
        Button places=secondary("🏛  Culture et patrimoine");places.setOnClickListener(v->loadPackage("sit-quebec-culture-art-et-patrimoine","Culture, arts et patrimoine",2));choices.addView(places);
        Button mtl=secondary("📍  Événements de Montréal");mtl.setOnClickListener(v->loadPackage("vmtl-evenements-publics","Événements publics",0));choices.addView(mtl);
        Button all=secondary("🔎  Explorer les jeux culturels");all.setOnClickListener(v->searchCulture());choices.addView(all);
        root.addView(choices,mb());

        status=text("Chargement des événements…",14,Color.DKGRAY,false);root.addView(status,mb());
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);root.addView(results);
        return scroll;
    }

    private void refreshMapFeeds(){
        if(mapStatus!=null)mapStatus.setText("Actualisation des sources publiques…");
        new Thread(()->{
            ArrayList<JSONObject> combined=new ArrayList<>();
            int sources=0;
            try{combined.addAll(fetchPackageRecords("sit-quebec-evenements",250,"SIT Québec"));sources++;}catch(Exception ignored){}
            try{combined.addAll(fetchPackageRecords("vmtl-evenements-publics",150,"Ville de Montréal"));sources++;}catch(Exception ignored){}
            try{combined.addAll(fetchPackageRecords("sit-quebec-culture-art-et-patrimoine",150,"SIT Québec — culture et patrimoine"));sources++;}catch(Exception ignored){}
            final int sourceCount=sources;
            ui.post(()->{
                mapView.setRecords(combined,"Données Québec / organismes diffuseurs");
                mapStatus.setText(sourceCount+" source(s) publique(s) actualisée(s) · "+combined.size()+" fiche(s) analysée(s)");
            });
        }).start();
    }

    private ArrayList<JSONObject> fetchPackageRecords(String id,int limit,String sourceLabel)throws Exception{
        ArrayList<JSONObject> out=new ArrayList<>();
        String base="https://www.donneesquebec.ca/recherche/api/3/action/package_show?id="+URLEncoder.encode(id,"UTF-8");
        JSONObject pkg=new JSONObject(get(base)).getJSONObject("result");JSONArray resources=pkg.optJSONArray("resources");JSONObject selected=null;
        if(resources!=null)for(int i=0;i<resources.length();i++){JSONObject r=resources.optJSONObject(i);if(r!=null&&r.optBoolean("datastore_active",false)){selected=r;break;}}
        if(selected==null)return out;
        String dataUrl="https://www.donneesquebec.ca/recherche/api/3/action/datastore_search?resource_id="+URLEncoder.encode(selected.optString("id"),"UTF-8")+"&limit="+limit;
        JSONArray rows=new JSONObject(get(dataUrl)).getJSONObject("result").optJSONArray("records");
        if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null){try{row.put("_map_source",sourceLabel);}catch(Exception ignored){}out.add(row);}}
        return out;
    }

    private void loadPackage(String id,String title,int defaultSort){
        currentSort=defaultSort;setLoading("Chargement de « "+title+" »…");
        new Thread(()->{
            try{
                String base="https://www.donneesquebec.ca/recherche/api/3/action/package_show?id="+URLEncoder.encode(id,"UTF-8");
                JSONObject pkg=new JSONObject(get(base)).getJSONObject("result");JSONArray resources=pkg.optJSONArray("resources");JSONObject selected=null;
                if(resources!=null)for(int i=0;i<resources.length();i++){JSONObject r=resources.optJSONObject(i);if(r!=null&&r.optBoolean("datastore_active",false)){selected=r;break;}}
                String source="Données Québec";try{source=pkg.getJSONObject("organization").optString("title",source);}catch(Exception ignored){}
                String license=pkg.optString("license_title",""),modified=pkg.optString("metadata_modified","");
                if(selected!=null){String dataUrl="https://www.donneesquebec.ca/recherche/api/3/action/datastore_search?resource_id="+URLEncoder.encode(selected.optString("id"),"UTF-8")+"&limit=100";JSONArray records=new JSONObject(get(dataUrl)).getJSONObject("result").optJSONArray("records");showRecords(title,source,license,modified,records);}else showResources(title,source,license,modified,resources);
            }catch(Exception e){showError("Impossible de charger cette liste pour le moment.\n"+safe(e.getMessage()));}
        }).start();
    }

    private void searchCulture(){
        setLoading("Recherche des jeux de données culturels…");
        new Thread(()->{
            try{
                String u="https://www.donneesquebec.ca/recherche/api/3/action/package_search?q="+URLEncoder.encode("culture","UTF-8")+"&rows=30&sort=metadata_modified%20desc";
                JSONArray arr=new JSONObject(get(u)).getJSONObject("result").optJSONArray("results");
                ui.post(()->{results.removeAllViews();status.setText((arr==null?0:arr.length())+" jeu(x) de données trouvé(s).");if(arr==null)return;for(int i=0;i<arr.length();i++){JSONObject p=arr.optJSONObject(i);if(p==null)continue;String name=p.optString("name"),title=p.optString("title",name);LinearLayout card=panel();card.addView(text(title,17,TEXT,true));String notes=strip(p.optString("notes",""));if(!notes.isEmpty())card.addView(text(shorten(notes,220),13,Color.DKGRAY,false));Button open=secondary("Voir les fiches ›");open.setOnClickListener(v->loadPackage(name,title,2));card.addView(open);results.addView(card,mb());}});
            }catch(Exception e){showError("Recherche impossible.\n"+safe(e.getMessage()));}
        }).start();
    }

    private void showRecords(String title,String source,String license,String modified,JSONArray records){
        currentTitle=title;currentSource=source;currentLicense=license;currentModified=modified;currentRecords.clear();
        if(records!=null)for(int i=0;i<records.length();i++){JSONObject r=records.optJSONObject(i);if(r!=null)currentRecords.add(r);}ui.post(this::renderCurrentRecords);
    }

    private void renderCurrentRecords(){
        results.removeAllViews();status.setText(currentRecords.size()+" fiche(s) affichée(s) — données interprétées automatiquement.");
        LinearLayout head=panel();head.addView(text(currentTitle,21,TEXT,true));head.addView(text("Source : "+currentSource+"\nLicence : "+(currentLicense.isEmpty()?"voir la source":currentLicense)+"\nMise à jour : "+dateOnly(currentModified),13,Color.DKGRAY,false));head.addView(text("Trier les fiches",14,TEXT,true));
        Spinner sort=new Spinner(this);String[] modes={"📅 Date — plus proche d’abord","📅 Date — plus éloignée d’abord","🔤 Ordre alphabétique A → Z","🔤 Ordre alphabétique Z → A"};sort.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,modes));sort.setSelection(currentSort,false);sort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){boolean first=true;public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}currentSort=pos;renderCurrentRecords();}public void onNothingSelected(android.widget.AdapterView<?> p){}});head.addView(sort,new LinearLayout.LayoutParams(-1,dp(52)));results.addView(head,mb());
        if(currentRecords.isEmpty()){results.addView(text("Aucune fiche disponible.",14,Color.DKGRAY,false));return;}
        ArrayList<JSONObject> sorted=new ArrayList<>(currentRecords);Collections.sort(sorted,comparatorFor(currentSort));for(JSONObject row:sorted)results.addView(eventCard(row,currentSource),mb());
    }

    private Comparator<JSONObject> comparatorFor(int mode){return(a,b)->{if(mode==2||mode==3){int c=titleOf(a).compareToIgnoreCase(titleOf(b));return mode==3?-c:c;}String da=dateSortKey(dateStart(a)),db=dateSortKey(dateStart(b));if(da.isEmpty()&&db.isEmpty())return titleOf(a).compareToIgnoreCase(titleOf(b));if(da.isEmpty())return 1;if(db.isEmpty())return-1;int c=da.compareTo(db);if(c==0)c=titleOf(a).compareToIgnoreCase(titleOf(b));return mode==1?-c:c;};}

    private View eventCard(JSONObject row,String source){
        LinearLayout card=panel();String title=titleOf(row),type=first(row,"TypeEtablissement","type","Type","categorie","Categorie","category","discipline"),theme=first(row,"Theme","theme","genre","Genre","sous_categorie"),date=dateStart(row),end=dateEnd(row),city=first(row,"Ville","ville","municipalite","Municipalite","city","RegionTouristique","RegionAdministrative"),place=first(row,"Lieu","lieu","adresse","Adresse","location","NomEtablissement"),url=firstUrl(row,false),image=firstUrl(row,true);
        if(!image.isEmpty()){ImageView iv=new ImageView(this);iv.setScaleType(ImageView.ScaleType.CENTER_CROP);iv.setBackgroundColor(Color.rgb(225,230,236));card.addView(iv,new LinearLayout.LayoutParams(-1,dp(180)));loadImage(iv,image);}
        card.addView(text(title,18,TEXT,true));LinearLayout badges=new LinearLayout(this);badges.setOrientation(LinearLayout.HORIZONTAL);badges.setPadding(0,dp(6),0,dp(4));if(!type.isEmpty())badges.addView(badge(type));if(!theme.isEmpty()&&!theme.equals(type))badges.addView(badge(theme));if(badges.getChildCount()>0)card.addView(badges);
        if(!date.isEmpty())card.addView(text("📅  "+friendlyDate(date)+(end.isEmpty()?"":" → "+friendlyDate(end)),14,TEXT,true));else card.addView(text("📅  Date non indiquée par la source",13,Color.DKGRAY,false));
        String where=!place.isEmpty()?place:city;if(!city.isEmpty()&&!where.contains(city))where+=" · "+city;if(!where.isEmpty())card.addView(text("📍  "+where,14,TEXT,false));String desc=first(row,"Description","description","Resume","resume","details","DescriptionDetaillee");if(!desc.isEmpty())card.addView(text(shorten(strip(desc),220),13,Color.DKGRAY,false));
        if(!url.isEmpty()){Button info=primarySmall("PLUS D’INFO");info.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}});card.addView(info);}card.addView(text("Source officielle : "+source,12,Color.rgb(70,85,100),false));return card;
    }

    private String titleOf(JSONObject r){String t=first(r,"NomEtablissement","titre","Titre","nom","Nom","name","Name","evenement","Evenement","description_courte");return t.isEmpty()?"Fiche culturelle":t;}
    private String dateStart(JSONObject r){String d=first(r,"DateDebut","dateDebut","date_debut","DateDébut","date début","date","Date","start_date","startDate","debut","Début","date_evenement","dateEvenement","DATE_DEBUT");return d.isEmpty()?findDateByKey(r,false):d;}
    private String dateEnd(JSONObject r){String d=first(r,"DateFin","dateFin","date_fin","DateFinEvenement","end_date","endDate","fin","Fin","DATE_FIN");return d.isEmpty()?findDateByKey(r,true):d;}
    private String findDateByKey(JSONObject row,boolean end){Iterator<String>it=row.keys();while(it.hasNext()){String k=it.next(),n=k.toLowerCase(Locale.ROOT).replace("_","").replace("-","").replace(" ","");boolean is=n.contains("date")||n.contains("debut")||n.contains("start")||n.contains("fin")||n.contains("end");if(!is)continue;if(end&&!(n.contains("fin")||n.contains("end")))continue;if(!end&&(n.contains("fin")||n.contains("end")))continue;Object v=row.opt(k);if(v==null||JSONObject.NULL.equals(v))continue;String s=strip(String.valueOf(v));if(looksLikeDate(s))return s;}return"";}
    private boolean looksLikeDate(String s){return Pattern.compile(".*(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}).*").matcher(s).matches();}
    private String dateSortKey(String s){if(s==null)return"";Matcher m=Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").matcher(s);if(m.find())return m.group(1)+pad2(m.group(2))+pad2(m.group(3));m=Pattern.compile("(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})").matcher(s);if(m.find())return m.group(3)+pad2(m.group(2))+pad2(m.group(1));return"";}
    private String pad2(String s){try{int n=Integer.parseInt(s);return n<10?"0"+n:String.valueOf(n);}catch(Exception e){return s;}}
    private TextView badge(String value){TextView b=text(shorten(value,28),12,Color.rgb(65,45,0),true);b.setPadding(dp(9),dp(4),dp(9),dp(4));b.setBackgroundColor(Color.rgb(247,205,111));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=dp(6);b.setLayoutParams(lp);return b;}
    private String first(JSONObject o,String...keys){for(String k:keys){Object v=o.opt(k);if(v!=null&&!JSONObject.NULL.equals(v)){String s=strip(String.valueOf(v));if(!s.isEmpty()&&!s.equalsIgnoreCase("null"))return s;}}return"";}
    private String firstUrl(JSONObject o,boolean imageOnly){Iterator<String>it=o.keys();while(it.hasNext()){String k=it.next();Object v=o.opt(k);if(v==null||JSONObject.NULL.equals(v))continue;String s=String.valueOf(v),lower=s.toLowerCase(Locale.ROOT);boolean keyImage=k.toLowerCase(Locale.ROOT).matches(".*(image|photo|media|visuel|thumbnail).*?"),looksImage=lower.matches(".*\\.(jpg|jpeg|png|webp)(\\?.*)?$");if(imageOnly&&(keyImage||looksImage)){String u=extractUrl(s);if(!u.isEmpty())return u;}if(!imageOnly&&(k.toLowerCase(Locale.ROOT).contains("url")||k.toLowerCase(Locale.ROOT).contains("lien")||k.toLowerCase(Locale.ROOT).contains("site"))){String u=extractUrl(s);if(!u.isEmpty()&&!looksImage)return u;}}return"";}
    private String extractUrl(String s){int p=s.indexOf("http");if(p<0)return"";String u=s.substring(p).trim();int cut=u.indexOf(' ');if(cut>0)u=u.substring(0,cut);return u.replace("\"","").replace("]","").replace("}","");}
    private String friendlyDate(String s){s=strip(s);Matcher m=Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").matcher(s);if(m.find())return pad2(m.group(3))+"/"+pad2(m.group(2))+"/"+m.group(1);return shorten(s,40);}
    private void loadImage(ImageView iv,String url){new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","CultureDuQuebec/1.9");InputStream in=c.getInputStream();Bitmap bm=BitmapFactory.decodeStream(in);in.close();c.disconnect();if(bm!=null)ui.post(()->iv.setImageBitmap(bm));}catch(Exception ignored){}}).start();}
    private void showResources(String title,String source,String license,String modified,JSONArray resources){ui.post(()->{results.removeAllViews();status.setText("Cette source fournit des fichiers plutôt qu’une table interrogeable.");LinearLayout head=panel();head.addView(text(title,21,TEXT,true));head.addView(text("Source : "+source+"\nLicence : "+license+"\nMise à jour : "+dateOnly(modified),13,Color.DKGRAY,false));results.addView(head,mb());if(resources==null)return;for(int i=0;i<resources.length();i++){JSONObject r=resources.optJSONObject(i);if(r==null)continue;LinearLayout card=panel();card.addView(text(r.optString("name","Ressource "+(i+1)),16,TEXT,true));card.addView(text("Format : "+r.optString("format","inconnu"),13,Color.DKGRAY,false));String u=r.optString("url","");if(!u.isEmpty()){Button b=secondary("Ouvrir la ressource");b.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u))));card.addView(b);}results.addView(card,mb());}});}
    private String get(String address)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","CultureDuQuebec/1.9");int code=c.getResponseCode();InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();c.disconnect();if(code<200||code>=300)throw new IOException("HTTP "+code);return sb.toString();}
    private void setLoading(String s){ui.post(()->{status.setText(s);results.removeAllViews();results.addView(new ProgressBar(this));});}
    private void showError(String s){ui.post(()->{results.removeAllViews();status.setText(s);});}
    private String strip(String s){return s==null?"":s.replaceAll("<[^>]*>"," ").replace("&nbsp;"," ").replaceAll("\\s+"," ").trim();}
    private String shorten(String s,int max){return s.length()<=max?s:s.substring(0,max-1)+"…";}
    private String dateOnly(String s){if(s==null||s.isEmpty())return"non indiquée";return s.length()>=10?s.substring(0,10):s;}
    private String safe(String s){return s==null?"Erreur réseau":s;}
    private void applyInsets(View v){v.setOnApplyWindowInsetsListener((view,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars());view.setPadding(dp(18),dp(18)+bars.top,dp(18),dp(34)+bars.bottom);return in;});v.requestApplyInsets();}
    private LinearLayout panel(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(16),dp(16),dp(16),dp(16));p.setBackgroundResource(R.drawable.panel);return p;}
    private TextView text(String s,int sp,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(c);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);t.setLineSpacing(0,1.12f);return t;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextColor(BLUE);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_secondary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);b.setLayoutParams(lp);return b;}
    private Button primarySmall(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_primary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.topMargin=dp(8);lp.bottomMargin=dp(8);b.setLayoutParams(lp);return b;}
    private LinearLayout.LayoutParams mb(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);return p;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
