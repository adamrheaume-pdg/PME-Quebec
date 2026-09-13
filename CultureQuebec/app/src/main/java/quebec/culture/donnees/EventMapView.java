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
            if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN || e.getActionMasked()==android.view.MotionEvent.ACTION_MOVE){
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if(e.getActionMasked()==android.view.MotionEvent.ACTION_UP || e.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL){
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
        addView(web,new LayoutParams(-1,dp(360)));
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
                    m.put("type",first(row,"TypeEtablissement","type","Type","categorie","Categorie","category","Theme","theme"));
                    m.put("url",url(row));
                    m.put("source",source==null?"":source);
                    markers.put(m);
                }catch(Exception ignored){}
            }
        }
        pendingJson=markers.toString();
        info.setText(markers.length()+" événement(s)/lieu(x) géolocalisé(s) sur "+total+" fiche(s). Glissez la carte, pincez pour zoomer et touchez un marqueur.");
        pushMarkers();
    }

    private void pushMarkers(){
        if(!pageReady) return;
        String js="window.setCultureMarkers("+pendingJson+");";
        web.evaluateJavascript(js,null);
    }

    private void loadBase(){
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=yes'>"+
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"+
                "<style>html,body,#map{height:100%;margin:0}body{font-family:Arial,sans-serif;background:#eef3f6}.leaflet-control-zoom a{width:44px!important;height:44px!important;line-height:44px!important;font-size:25px!important}.leaflet-popup-content{font-size:14px;line-height:1.35;min-width:190px}.leaflet-popup-content b{font-size:16px;color:#12324b}.chip{display:inline-block;background:#f7cd6f;padding:3px 7px;border-radius:10px;margin:4px 0;font-weight:700}.go{display:inline-block;margin-top:7px;background:#064f89;color:white!important;padding:7px 10px;border-radius:8px;text-decoration:none;font-weight:700}.reset{position:absolute;z-index:999;right:10px;top:10px;border:0;border-radius:10px;background:white;color:#064f89;padding:10px 12px;font-weight:700;box-shadow:0 1px 6px #7776}</style></head>"+
                "<body><div id='map'></div><button class='reset' id='reset'>Québec</button>"+
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>"+
                "const map=L.map('map',{zoomControl:true,minZoom:3,maxZoom:18}).setView([52.2,-71.4],5);"+
                "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap'}).addTo(map);"+
                "let layer=L.layerGroup().addTo(map);let bounds=[];"+
                "function esc(s){return String(s||'').replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[c]));}"+
                "window.setCultureMarkers=function(items){layer.clearLayers();bounds=[];(items||[]).forEach(x=>{if(!Number.isFinite(+x.lat)||!Number.isFinite(+x.lon))return;let html='<b>'+esc(x.title)+'</b>';if(x.type)html+='<br><span class=\"chip\">'+esc(x.type)+'</span>';if(x.date)html+='<br>📅 '+esc(x.date);if(x.place)html+='<br>📍 '+esc(x.place);if(x.source)html+='<br><small>Source : '+esc(x.source)+'</small>';if(x.url)html+='<br><a class=\"go\" href=\"'+esc(x.url)+'\">PLUS D’INFO</a>';let marker=L.circleMarker([+x.lat,+x.lon],{radius:9,weight:3,color:'#ffffff',fillColor:'#075a94',fillOpacity:.92}).bindPopup(html,{maxWidth:280});marker.addTo(layer);bounds.push([+x.lat,+x.lon]);});if(bounds.length===1)map.setView(bounds[0],11);else if(bounds.length>1)map.fitBounds(bounds,{padding:[24,24],maxZoom:11});else map.setView([52.2,-71.4],5);};"+
                "document.getElementById('reset').addEventListener('click',()=>{if(bounds.length>1)map.fitBounds(bounds,{padding:[24,24],maxZoom:11});else if(bounds.length===1)map.setView(bounds[0],11);else map.setView([52.2,-71.4],5);});"+
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
    private String url(JSONObject o){Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next().toLowerCase(Locale.ROOT);if(!(k.contains("url")||k.contains("lien")||k.contains("site")))continue;String s=String.valueOf(o.opt(k));int p=s.indexOf("http");if(p>=0){String u=s.substring(p).trim();int cut=u.indexOf(' ');if(cut>0)u=u.substring(0,cut);return u.replace("\"","").replace("]","").replace("}","");}}return "";}
    private String first(JSONObject o,String... keys){for(String k:keys){Object v=o.opt(k);if(v!=null&&!JSONObject.NULL.equals(v)){String s=String.valueOf(v).replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").trim();if(!s.isEmpty()&&!s.equalsIgnoreCase("null"))return s;}}return "";}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
