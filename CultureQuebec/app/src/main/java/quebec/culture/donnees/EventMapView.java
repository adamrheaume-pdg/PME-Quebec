package quebec.culture.donnees;

import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.regex.*;

public class EventMapView extends LinearLayout {
    private final WebView web;
    private final TextView info;
    private boolean pageReady=false;
    private String pendingJson="[]";

    public EventMapView(Context context){
        super(context);
        setOrientation(VERTICAL);
        setPadding(0,0,0,0);

        info=new TextView(context);
        info.setText("Carte du Québec — chargement…");
        info.setTextColor(Color.rgb(55,70,85));
        info.setTextSize(12);
        info.setPadding(dp(2),0,dp(2),dp(8));
        addView(info,new LayoutParams(-1,-2));

        web=new WebView(context);
        web.setBackgroundColor(Color.rgb(232,238,243));
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setOverScrollMode(OVER_SCROLL_NEVER);
        web.setOnTouchListener((v,e)->{
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN || e.getActionMasked()==MotionEvent.ACTION_MOVE){
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if(e.getActionMasked()==MotionEvent.ACTION_UP || e.getActionMasked()==MotionEvent.ACTION_CANCEL){
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){pageReady=true;pushMarkers();}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                Uri u=request.getUrl();
                String s=u==null?"":u.toString();
                if(s.startsWith("http://")||s.startsWith("https://")){
                    try{context.startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}
                    return true;
                }
                return false;
            }
        });
        LayoutParams webLp=new LayoutParams(-1,0,1f);
        addView(web,webLp);
        loadBase();
    }

    public void setRecords(List<JSONObject> rows,String source){
        JSONArray markers=new JSONArray();
        int total=rows==null?0:rows.size();
        if(rows!=null){
            for(JSONObject row:rows){
                double[] ll=coordinates(row);
                if(ll==null) continue;
                JSONObject m=new JSONObject();
                try{
                    m.put("lat",ll[0]);m.put("lon",ll[1]);
                    m.put("title",title(row));
                    m.put("date",date(row));
                    m.put("place",place(row));
                    m.put("type",first(row,"TypeEtablissement","type","Type","categorie","Categorie","category","discipline"));
                    m.put("theme",first(row,"Theme","theme","Genre","genre","sous_categorie","SousCategorie","discipline"));
                    m.put("desc",first(row,"Description","description","Resume","resume","details","DescriptionDetaillee"));
                    m.put("url",url(row));
                    String rowSource=first(row,"_map_source");
                    m.put("source",rowSource.isEmpty()?(source==null?"":source):rowSource);
                    markers.put(m);
                }catch(Exception ignored){}
            }
        }
        pendingJson=markers.toString();
        info.setText(markers.length()+" point(s) géolocalisé(s) sur "+total+" fiche(s) · glissez, pincez, double-tapez ou touchez un point.");
        pushMarkers();
    }

    private void pushMarkers(){
        if(!pageReady) return;
        web.evaluateJavascript("window.setCultureMarkers("+pendingJson+");",null);
    }

    private void loadBase(){
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=yes'>"+
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"+
                "<style>html,body,#map{height:100%;margin:0}body{font-family:Arial,sans-serif;background:#eef3f6;overflow:hidden}.leaflet-control-zoom a{width:46px!important;height:46px!important;line-height:46px!important;font-size:27px!important}.leaflet-popup-content{font-size:14px;line-height:1.35;min-width:205px}.leaflet-popup-content b{font-size:16px;color:#12324b}.chip{display:inline-block;background:#f7cd6f;padding:3px 7px;border-radius:10px;margin:4px 4px 2px 0;font-weight:700}.go{display:inline-block;margin-top:7px;background:#064f89;color:white!important;padding:8px 11px;border-radius:8px;text-decoration:none;font-weight:700}.reset{position:absolute;z-index:999;right:10px;top:10px;border:0;border-radius:12px;background:white;color:#064f89;padding:12px 14px;font-weight:700;box-shadow:0 1px 7px #5557}.legend{position:absolute;z-index:998;left:8px;bottom:24px;background:rgba(255,255,255,.94);padding:7px 9px;border-radius:10px;box-shadow:0 1px 7px #5555;font-size:11px;line-height:1.55;max-width:180px}.dot{width:11px;height:11px;border-radius:50%;display:inline-block;vertical-align:-1px;margin-right:5px;border:2px solid #fff;box-shadow:0 0 0 1px #7776}.b{background:#0b6bb3}.g{background:#178447}.r{background:#d62f2f}.o{background:#e07a18}.p{background:#7a46a8}.pulse-b{animation:pulseB 1.35s infinite}.pulse-g{animation:pulseG 1.35s infinite}.pulse-r{animation:pulseR 1.15s infinite}@keyframes pulseB{0%,100%{fill:#0b6bb3;fill-opacity:.95;stroke-width:3}50%{fill:#7fd0ff;fill-opacity:.48;stroke-width:7}}@keyframes pulseG{0%,100%{fill:#178447;fill-opacity:.95;stroke-width:3}50%{fill:#7ee2a8;fill-opacity:.48;stroke-width:7}}@keyframes pulseR{0%,100%{fill:#d62f2f;fill-opacity:.95;stroke-width:3}50%{fill:#ff8b8b;fill-opacity:.48;stroke-width:7}}@media (prefers-reduced-motion:reduce){.pulse-b,.pulse-g,.pulse-r{animation:none!important}}</style></head>"+
                "<body><div id='map'></div><button class='reset' id='reset'>Québec</button><div class='legend'><b>Légende</b><br><span class='dot b'></span>Culture<br><span class='dot b pulse-b'></span>Musique<br><span class='dot g'></span>Théâtre<br><span class='dot g pulse-g'></span>Film / TV / Web<br><span class='dot r pulse-r'></span>Patrimoine / histoire<br><span class='dot r'></span>Autochtone<br><span class='dot p'></span>Livre / littérature<br><span class='dot o'></span>Autre événement</div>"+
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>"+
                "const map=L.map('map',{zoomControl:true,minZoom:3,maxZoom:18,doubleClickZoom:true,touchZoom:true,scrollWheelZoom:true,dragging:true}).setView([52.2,-71.4],5);"+
                "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap'}).addTo(map);"+
                "let layer=L.layerGroup().addTo(map);let bounds=[];"+
                "function esc(s){return String(s||'').replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[c]));}"+
                "function has(s,words){return words.some(w=>s.includes(w));}"+
                "function cat(x){const s=(String(x.title||'')+' '+String(x.type||'')+' '+String(x.theme||'')+' '+String(x.desc||'')+' '+String(x.place||'')).toLowerCase();if(has(s,['autocht','première nation','premieres nations','inuit','innu','atikamekw','anichin','anishina','mohawk','kanien','mi’gmaq','mi\'gmaq','wendat','naskapi','crie','eeyou','wolastoq','abéna','abenaki']))return{fill:'#d62f2f',cls:'',label:'Autochtone'};if(has(s,['patrimoine','historique','histoire','archéolog','archeolog','site religieux','édifice religieux','edifice religieux','lieu patrimonial']))return{fill:'#d62f2f',cls:'pulse-r',label:'Patrimoine / histoire'};if(has(s,['film','cinéma','cinema','télévision','television','télévis','televis','série','serie','websérie','webserie','création web','creation web','numérique','numerique','multimédia','multimedia']))return{fill:'#178447',cls:'pulse-g',label:'Film / TV / Web'};if(has(s,['théâtre','theatre','théat','theat','dramaturg','scène','scene']))return{fill:'#178447',cls:'',label:'Théâtre'};if(has(s,['musique','concert','chanson','musical','orchestre','jazz','rock','rap','hip-hop','hip hop','folk','classique']))return{fill:'#0b6bb3',cls:'pulse-b',label:'Musique'};if(has(s,['livre','littérature','litterature','auteur','poésie','poesie','roman','bibliothèque','bibliotheque']))return{fill:'#7a46a8',cls:'',label:'Livre / littérature'};if(has(s,['culture','musée','musee','exposition','art','galerie','festival']))return{fill:'#0b6bb3',cls:'',label:'Culture'};return{fill:'#e07a18',cls:'',label:'Autre événement'};}"+
                "window.setCultureMarkers=function(items){layer.clearLayers();bounds=[];(items||[]).forEach(x=>{if(!Number.isFinite(+x.lat)||!Number.isFinite(+x.lon))return;const c=cat(x);let html='<b>'+esc(x.title)+'</b><br><span class=\"chip\">'+esc(c.label)+'</span>';if(x.type)html+='<span class=\"chip\">'+esc(x.type)+'</span>';if(x.date)html+='<br>📅 '+esc(x.date);if(x.place)html+='<br>📍 '+esc(x.place);if(x.source)html+='<br><small>Source : '+esc(x.source)+'</small>';if(x.url)html+='<br><a class=\"go\" href=\"'+esc(x.url)+'\">PLUS D’INFO</a>';let marker=L.circleMarker([+x.lat,+x.lon],{radius:10,weight:3,color:'#ffffff',fillColor:c.fill,fillOpacity:.94,className:c.cls}).bindPopup(html,{maxWidth:290});marker.addTo(layer);bounds.push([+x.lat,+x.lon]);});if(bounds.length===1)map.setView(bounds[0],11);else if(bounds.length>1)map.fitBounds(bounds,{padding:[26,26],maxZoom:10});else map.setView([52.2,-71.4],5);};"+
                "document.getElementById('reset').addEventListener('click',()=>{if(bounds.length>1)map.fitBounds(bounds,{padding:[26,26],maxZoom:10});else if(bounds.length===1)map.setView(bounds[0],11);else map.setView([52.2,-71.4],5);});"+
                "</script></body></html>";
        web.loadDataWithBaseURL("https://culture-quebec.local/",html,"text/html","UTF-8",null);
    }

    private double[] coordinates(JSONObject row){
        Double lat=null,lon=null;
        Iterator<String> it=row.keys();
        while(it.hasNext()){
            String k=it.next(); Object raw=row.opt(k); if(raw==null||JSONObject.NULL.equals(raw))continue;
            String n=normalize(k); Double d=number(raw);
            if(d!=null){
                if((n.equals("lat")||n.contains("latitude")) && d>=40&&d<=63) lat=d;
                if((n.equals("lon")||n.equals("lng")||n.contains("longitude")) && d>=-85&&d<=-50) lon=d;
            }
            if(raw instanceof JSONObject){
                JSONObject g=(JSONObject)raw;
                JSONArray c=g.optJSONArray("coordinates");
                if(c!=null&&c.length()>=2){Double lo=number(c.opt(0)),la=number(c.opt(1));if(la!=null&&lo!=null&&la>=40&&la<=63&&lo>=-85&&lo<=-50){lat=la;lon=lo;}}
            }
            if((n.contains("coord")||n.contains("geoloc"))&&raw instanceof String){
                Matcher m=Pattern.compile("(-?\\d{2,3}\\.\\d+)\\s*[,; ]\\s*(-?\\d{2,3}\\.\\d+)").matcher(String.valueOf(raw));
                if(m.find()){
                    try{double a=Double.parseDouble(m.group(1)),b=Double.parseDouble(m.group(2));if(a>=40&&a<=63&&b>=-85&&b<=-50){lat=a;lon=b;}else if(b>=40&&b<=63&&a>=-85&&a<=-50){lat=b;lon=a;}}catch(Exception ignored){}
                }
            }
        }
        return lat!=null&&lon!=null?new double[]{lat,lon}:null;
    }

    private Double number(Object v){try{String s=String.valueOf(v).trim().replace(',', '.');return Double.parseDouble(s);}catch(Exception e){return null;}}
    private String normalize(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replace("_","").replace("-","").replace(" ","");}
    private String title(JSONObject o){String s=first(o,"NomEtablissement","titre","Titre","nom","Nom","name","Name","evenement","Evenement");return s.isEmpty()?"Événement culturel":s;}
    private String date(JSONObject o){String s=first(o,"DateDebut","dateDebut","date_debut","date","Date","start_date","startDate","debut","DATE_DEBUT");if(s.length()>=10&&s.charAt(4)=='-'&&s.charAt(7)=='-')return s.substring(8,10)+"/"+s.substring(5,7)+"/"+s.substring(0,4);return s;}
    private String place(JSONObject o){String a=first(o,"Lieu","lieu","adresse","Adresse","NomEtablissement");String c=first(o,"Ville","ville","municipalite","Municipalite","city","RegionTouristique","RegionAdministrative");if(a.isEmpty())return c;if(!c.isEmpty()&&!a.contains(c))return a+" · "+c;return a;}
    private String url(JSONObject o){Iterator<String> it=o.keys();while(it.hasNext()){String key=it.next();String k=key.toLowerCase(Locale.ROOT);if(!(k.contains("url")||k.contains("lien")||k.contains("site")))continue;String s=String.valueOf(o.opt(key));int p=s.indexOf("http");if(p>=0){String u=s.substring(p).trim();int cut=u.indexOf(' ');if(cut>0)u=u.substring(0,cut);return u.replace("\"","").replace("]","").replace("}","");}}return "";}
    private String first(JSONObject o,String... keys){for(String k:keys){Object v=o.opt(k);if(v!=null&&!JSONObject.NULL.equals(v)){String s=String.valueOf(v).replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").trim();if(!s.isEmpty()&&!s.equalsIgnoreCase("null"))return s;}}return "";}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
