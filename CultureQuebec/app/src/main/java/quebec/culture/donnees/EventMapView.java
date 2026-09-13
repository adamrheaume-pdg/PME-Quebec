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
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
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
        String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"+
                "<style>html,body{height:100%;margin:0;overflow:hidden;font-family:Arial,sans-serif;background:#dfe8ee}#map{position:relative;width:100%;height:100%;overflow:hidden;touch-action:none;background:#dfe8ee}.tiles,.markers{position:absolute;left:0;top:0;width:100%;height:100%}.tile{position:absolute;width:256px;height:256px;user-select:none;-webkit-user-drag:none}.marker{position:absolute;width:20px;height:20px;border-radius:50%;border:3px solid #fff;box-shadow:0 1px 5px #0007;transform:translate(-50%,-50%);z-index:20}.pulse-b{animation:pulseB 1.35s infinite}.pulse-g{animation:pulseG 1.35s infinite}.pulse-r{animation:pulseR 1.15s infinite}@keyframes pulseB{0%,100%{box-shadow:0 0 0 0 rgba(11,107,179,.65),0 1px 5px #0007}50%{box-shadow:0 0 0 11px rgba(11,107,179,.12),0 1px 5px #0007}}@keyframes pulseG{0%,100%{box-shadow:0 0 0 0 rgba(23,132,71,.65),0 1px 5px #0007}50%{box-shadow:0 0 0 11px rgba(23,132,71,.12),0 1px 5px #0007}}@keyframes pulseR{0%,100%{box-shadow:0 0 0 0 rgba(214,47,47,.65),0 1px 5px #0007}50%{box-shadow:0 0 0 11px rgba(214,47,47,.12),0 1px 5px #0007}}@media(prefers-reduced-motion:reduce){.pulse-b,.pulse-g,.pulse-r{animation:none!important}}.ctrl{position:absolute;z-index:50;background:#fff;border:0;border-radius:11px;box-shadow:0 1px 7px #5557;color:#064f89;font-weight:700}.reset{right:10px;top:10px;padding:12px 14px}.zoom{left:10px;top:10px;display:flex;flex-direction:column;overflow:hidden}.zoom button{width:46px;height:46px;border:0;background:#fff;color:#064f89;font-size:27px;font-weight:700}.zoom button+button{border-top:1px solid #ddd}.legend{position:absolute;z-index:45;left:8px;bottom:22px;background:rgba(255,255,255,.95);padding:8px 10px;border-radius:10px;box-shadow:0 1px 7px #5555;font-size:11px;line-height:1.55;max-width:180px}.dot{width:11px;height:11px;border-radius:50%;display:inline-block;vertical-align:-1px;margin-right:5px;border:2px solid #fff;box-shadow:0 0 0 1px #7776}.b{background:#0b6bb3}.g{background:#178447}.r{background:#d62f2f}.o{background:#e07a18}.p{background:#7a46a8}.popup{position:absolute;z-index:60;left:50%;top:50%;transform:translate(-50%,-115%);background:#fff;border-radius:14px;padding:12px 14px;min-width:210px;max-width:78%;box-shadow:0 3px 16px #0006;font-size:14px;line-height:1.35;display:none}.popup b{font-size:16px;color:#12324b}.chip{display:inline-block;background:#f7cd6f;padding:3px 7px;border-radius:10px;margin:4px 4px 2px 0;font-weight:700}.go{display:inline-block;margin-top:7px;background:#064f89;color:white!important;padding:8px 11px;border-radius:8px;text-decoration:none;font-weight:700}.attrib{position:absolute;z-index:40;right:4px;bottom:2px;background:rgba(255,255,255,.82);font-size:10px;padding:2px 4px;color:#345}</style></head>"+
                "<body><div id='map'><div class='tiles' id='tiles'></div><div class='markers' id='markers'></div><div class='zoom ctrl'><button id='zin'>+</button><button id='zout'>−</button></div><button class='reset ctrl' id='reset'>Québec</button><div class='legend'><b>Légende</b><br><span class='dot b'></span>Culture<br><span class='dot b pulse-b'></span>Musique<br><span class='dot g'></span>Théâtre<br><span class='dot g pulse-g'></span>Film / TV / Web<br><span class='dot r pulse-r'></span>Patrimoine / histoire<br><span class='dot r'></span>Autochtone<br><span class='dot p'></span>Livre / littérature<br><span class='dot o'></span>Autre événement</div><div class='popup' id='popup'></div><div class='attrib'>© OpenStreetMap</div></div>"+
                "<script>const map=document.getElementById('map'),tiles=document.getElementById('tiles'),markersEl=document.getElementById('markers'),popup=document.getElementById('popup');let zoom=5,center={lat:52.2,lon:-71.4},items=[],drag=null,pinch=null;const TILE=256;function clamp(v,a,b){return Math.max(a,Math.min(b,v))}function world(lat,lon,z){const n=Math.pow(2,z)*TILE;const x=(lon+180)/360*n;const s=Math.sin(lat*Math.PI/180);const y=(.5-Math.log((1+s)/(1-s))/(4*Math.PI))*n;return{x,y}}function unworld(x,y,z){const n=Math.pow(2,z)*TILE;const lon=x/n*360-180;const yy=.5-y/n;const lat=90-360*Math.atan(Math.exp(-yy*2*Math.PI))/Math.PI;return{lat,lon}}function esc(s){return String(s||'').replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',\"'\":'&#39;'}[c]))}function has(s,w){return w.some(x=>s.includes(x))}function cat(x){const s=(String(x.title||'')+' '+String(x.type||'')+' '+String(x.theme||'')+' '+String(x.desc||'')+' '+String(x.place||'')).toLowerCase();if(has(s,['autocht','première nation','premieres nations','inuit','innu','atikamekw','anichin','anishina','mohawk','kanien','mi’gmaq','mi\'gmaq','wendat','naskapi','crie','eeyou','wolastoq','abéna','abenaki']))return{fill:'#d62f2f',cls:'',label:'Autochtone'};if(has(s,['patrimoine','historique','histoire','archéolog','archeolog','site religieux','édifice religieux','edifice religieux','lieu patrimonial']))return{fill:'#d62f2f',cls:'pulse-r',label:'Patrimoine / histoire'};if(has(s,['film','cinéma','cinema','télévision','television','télévis','televis','série','serie','websérie','webserie','création web','creation web','numérique','numerique','multimédia','multimedia']))return{fill:'#178447',cls:'pulse-g',label:'Film / TV / Web'};if(has(s,['théâtre','theatre','théat','theat','dramaturg','scène','scene']))return{fill:'#178447',cls:'',label:'Théâtre'};if(has(s,['musique','concert','chanson','musical','orchestre','jazz','rock','rap','hip-hop','hip hop','folk','classique']))return{fill:'#0b6bb3',cls:'pulse-b',label:'Musique'};if(has(s,['livre','littérature','litterature','auteur','poésie','poesie','roman','bibliothèque','bibliotheque']))return{fill:'#7a46a8',cls:'',label:'Livre / littérature'};if(has(s,['culture','musée','musee','exposition','art','galerie','festival']))return{fill:'#0b6bb3',cls:'',label:'Culture'};return{fill:'#e07a18',cls:'',label:'Autre événement'}}function render(){const w=map.clientWidth,h=map.clientHeight,cw=world(center.lat,center.lon,zoom),left=cw.x-w/2,top=cw.y-h/2;tiles.innerHTML='';const max=Math.pow(2,zoom),x0=Math.floor(left/TILE),x1=Math.floor((left+w)/TILE),y0=Math.floor(top/TILE),y1=Math.floor((top+h)/TILE);for(let ty=y0;ty<=y1;ty++){if(ty<0||ty>=max)continue;for(let tx=x0;tx<=x1;tx++){let wx=((tx%max)+max)%max;const im=document.createElement('img');im.className='tile';im.draggable=false;im.src='https://tile.openstreetmap.org/'+zoom+'/'+wx+'/'+ty+'.png';im.style.left=(tx*TILE-left)+'px';im.style.top=(ty*TILE-top)+'px';tiles.appendChild(im)}}markersEl.innerHTML='';items.forEach((x,i)=>{const p=world(+x.lat,+x.lon,zoom),sx=p.x-left,sy=p.y-top;if(sx<-30||sy<-30||sx>w+30||sy>h+30)return;const c=cat(x),m=document.createElement('button');m.className='marker '+c.cls;m.style.background=c.fill;m.style.left=sx+'px';m.style.top=sy+'px';m.setAttribute('aria-label',x.title||'Événement');m.addEventListener('click',e=>{e.stopPropagation();showPopup(x,c,sx,sy)});markersEl.appendChild(m)});}function showPopup(x,c,sx,sy){let html='<b>'+esc(x.title)+'</b><br><span class=\"chip\">'+esc(c.label)+'</span>';if(x.type)html+='<span class=\"chip\">'+esc(x.type)+'</span>';if(x.date)html+='<br>📅 '+esc(x.date);if(x.place)html+='<br>📍 '+esc(x.place);if(x.source)html+='<br><small>Source : '+esc(x.source)+'</small>';if(x.url)html+='<br><a class=\"go\" href=\"'+esc(x.url)+'\">PLUS D’INFO</a>';popup.innerHTML=html;popup.style.left=clamp(sx,120,map.clientWidth-120)+'px';popup.style.top=clamp(sy,120,map.clientHeight-40)+'px';popup.style.display='block'}function setCenterFromPixel(dx,dy){const c=world(center.lat,center.lon,zoom);const n=unworld(c.x-dx,c.y-dy,zoom);center={lat:clamp(n.lat,-85,85),lon:n.lon};render()}function z(delta,focusX,focusY){const old=zoom,nz=clamp(zoom+delta,3,18);if(nz===old)return;const w=map.clientWidth,h=map.clientHeight,c=world(center.lat,center.lon,old),wx=c.x+(focusX-w/2),wy=c.y+(focusY-h/2),geo=unworld(wx,wy,old);zoom=nz;const nw=world(geo.lat,geo.lon,nz),ncx=nw.x-(focusX-w/2),ncy=nw.y-(focusY-h/2),nc=unworld(ncx,ncy,nz);center={lat:nc.lat,lon:nc.lon};popup.style.display='none';render()}function fit(){if(!items.length){zoom=5;center={lat:52.2,lon:-71.4};render();return}let minLat=90,maxLat=-90,minLon=180,maxLon=-180;items.forEach(x=>{minLat=Math.min(minLat,+x.lat);maxLat=Math.max(maxLat,+x.lat);minLon=Math.min(minLon,+x.lon);maxLon=Math.max(maxLon,+x.lon)});center={lat:(minLat+maxLat)/2,lon:(minLon+maxLon)/2};zoom=5;for(let zz=10;zz>=3;zz--){const a=world(minLat,minLon,zz),b=world(maxLat,maxLon,zz);if(Math.abs(b.x-a.x)<map.clientWidth*.78&&Math.abs(b.y-a.y)<map.clientHeight*.72){zoom=zz;break}}render()}window.setCultureMarkers=function(a){items=(a||[]).filter(x=>Number.isFinite(+x.lat)&&Number.isFinite(+x.lon));fit()};map.addEventListener('pointerdown',e=>{map.setPointerCapture(e.pointerId);if(!drag)drag={id:e.pointerId,x:e.clientX,y:e.clientY};popup.style.display='none'});map.addEventListener('pointermove',e=>{if(drag&&drag.id===e.pointerId){const dx=e.clientX-drag.x,dy=e.clientY-drag.y;drag.x=e.clientX;drag.y=e.clientY;setCenterFromPixel(dx,dy)}});map.addEventListener('pointerup',e=>{if(drag&&drag.id===e.pointerId)drag=null});map.addEventListener('dblclick',e=>{e.preventDefault();z(1,e.offsetX,e.offsetY)});map.addEventListener('wheel',e=>{e.preventDefault();z(e.deltaY<0?1:-1,e.offsetX,e.offsetY)},{passive:false});document.getElementById('zin').onclick=()=>z(1,map.clientWidth/2,map.clientHeight/2);document.getElementById('zout').onclick=()=>z(-1,map.clientWidth/2,map.clientHeight/2);document.getElementById('reset').onclick=fit;map.addEventListener('click',()=>{popup.style.display='none'});let t0=null;map.addEventListener('touchstart',e=>{if(e.touches.length===2){const a=e.touches[0],b=e.touches[1];t0={d:Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY),x:(a.clientX+b.clientX)/2-map.getBoundingClientRect().left,y:(a.clientY+b.clientY)/2-map.getBoundingClientRect().top,z:zoom}}},{passive:true});map.addEventListener('touchmove',e=>{if(t0&&e.touches.length===2){const a=e.touches[0],b=e.touches[1],d=Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY);if(d>t0.d*1.35){z(1,t0.x,t0.y);t0.d=d}else if(d<t0.d*.74){z(-1,t0.x,t0.y);t0.d=d}}},{passive:true});map.addEventListener('touchend',()=>{t0=null},{passive:true});window.addEventListener('resize',render);render();</script></body></html>";
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
