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
        info=new TextView(context);
        info.setText("Carte du Québec — chargement…");
        info.setTextColor(Color.rgb(55,70,85));
        info.setTextSize(12);
        info.setPadding(dp(2),0,dp(2),dp(8));
        addView(info,new LayoutParams(-1,-2));

        web=new WebView(context);
        web.setBackgroundColor(Color.rgb(223,232,238));
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setBlockNetworkImage(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setOverScrollMode(OVER_SCROLL_NEVER);
        web.setOnTouchListener((v,e)->{
            int a=e.getActionMasked();
            v.getParent().requestDisallowInterceptTouchEvent(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_MOVE);
            return false;
        });
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){pageReady=true;pushMarkers();}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                Uri u=request.getUrl();
                String x=u==null?"":u.toString();
                if(x.startsWith("http://")||x.startsWith("https://")){
                    try{context.startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}
                    return true;
                }
                return false;
            }
        });
        addView(web,new LayoutParams(-1,0,1f));
        web.loadUrl("file:///android_asset/culture_map.html");
    }

    public void setRecords(List<JSONObject> rows,String source){
        JSONArray markers=new JSONArray();
        int total=rows==null?0:rows.size();
        if(rows!=null){
            for(JSONObject row:rows){
                double[] ll=coordinates(row);
                if(ll==null)continue;
                JSONObject m=new JSONObject();
                try{
                    m.put("lat",ll[0]);m.put("lon",ll[1]);
                    m.put("title",title(row));m.put("date",date(row));m.put("place",place(row));
                    m.put("type",first(row,"TypeEtablissement","type","Type","categorie","Categorie","category","discipline"));
                    m.put("theme",first(row,"Theme","theme","Genre","genre","sous_categorie","SousCategorie","discipline"));
                    m.put("desc",first(row,"Description","description","Resume","resume","details","DescriptionDetaillee"));
                    m.put("url",url(row));
                    String rs=first(row,"_map_source");
                    m.put("source",rs.isEmpty()?(source==null?"":source):rs);
                    markers.put(m);
                }catch(Exception ignored){}
            }
        }
        pendingJson=markers.toString();
        info.setText(markers.length()+" point(s) géolocalisé(s) sur "+total+" fiche(s) · glissez, pincez, double-tapez ou touchez un point.");
        pushMarkers();
    }

    private void pushMarkers(){
        if(pageReady)web.evaluateJavascript("window.setCultureMarkers("+pendingJson+");",null);
    }

    private double[] coordinates(JSONObject row){
        Double lat=null,lon=null;
        Iterator<String> it=row.keys();
        while(it.hasNext()){
            String k=it.next();Object raw=row.opt(k);if(raw==null||JSONObject.NULL.equals(raw))continue;
            String n=normalize(k);Double d=number(raw);
            if(d!=null){
                if((n.equals("lat")||n.contains("latitude"))&&d>=40&&d<=63)lat=d;
                if((n.equals("lon")||n.equals("lng")||n.contains("longitude"))&&d>=-85&&d<=-50)lon=d;
            }
            if(raw instanceof JSONObject){
                JSONArray c=((JSONObject)raw).optJSONArray("coordinates");
                if(c!=null&&c.length()>=2){Double lo=number(c.opt(0)),la=number(c.opt(1));if(la!=null&&lo!=null&&la>=40&&la<=63&&lo>=-85&&lo<=-50){lat=la;lon=lo;}}
            }
            if((n.contains("coord")||n.contains("geoloc"))&&raw instanceof String){
                Matcher m=Pattern.compile("(-?\\d{2,3}\\.\\d+)\\s*[,; ]\\s*(-?\\d{2,3}\\.\\d+)").matcher(String.valueOf(raw));
                if(m.find())try{double a=Double.parseDouble(m.group(1)),b=Double.parseDouble(m.group(2));if(a>=40&&a<=63&&b>=-85&&b<=-50){lat=a;lon=b;}else if(b>=40&&b<=63&&a>=-85&&a<=-50){lat=b;lon=a;}}catch(Exception ignored){}
            }
        }
        return lat!=null&&lon!=null?new double[]{lat,lon}:null;
    }

    private Double number(Object v){try{return Double.parseDouble(String.valueOf(v).trim().replace(',','.'));}catch(Exception e){return null;}}
    private String normalize(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replace("_","").replace("-","").replace(" ","");}
    private String title(JSONObject o){String s=first(o,"NomEtablissement","titre","Titre","nom","Nom","name","Name","evenement","Evenement");return s.isEmpty()?"Événement culturel":s;}
    private String date(JSONObject o){String s=first(o,"DateDebut","dateDebut","date_debut","date","Date","start_date","startDate","debut","DATE_DEBUT");if(s.length()>=10&&s.charAt(4)=='-'&&s.charAt(7)=='-')return s.substring(8,10)+"/"+s.substring(5,7)+"/"+s.substring(0,4);return s;}
    private String place(JSONObject o){String a=first(o,"Lieu","lieu","adresse","Adresse","NomEtablissement"),c=first(o,"Ville","ville","municipalite","Municipalite","city","RegionTouristique","RegionAdministrative");if(a.isEmpty())return c;if(!c.isEmpty()&&!a.contains(c))return a+" · "+c;return a;}
    private String url(JSONObject o){Iterator<String>it=o.keys();while(it.hasNext()){String key=it.next(),k=key.toLowerCase(Locale.ROOT);if(!(k.contains("url")||k.contains("lien")||k.contains("site")))continue;String s=String.valueOf(o.opt(key));int p=s.indexOf("http");if(p>=0){String u=s.substring(p).trim();int cut=u.indexOf(' ');if(cut>0)u=u.substring(0,cut);return u.replace("\"","").replace("]","").replace("}","");}}return"";}
    private String first(JSONObject o,String...keys){for(String k:keys){Object v=o.opt(k);if(v!=null&&!JSONObject.NULL.equals(v)){String s=String.valueOf(v).replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").trim();if(!s.isEmpty()&&!s.equalsIgnoreCase("null"))return s;}}return"";}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
