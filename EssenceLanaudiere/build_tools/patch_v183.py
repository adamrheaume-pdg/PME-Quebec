from pathlib import Path
import re

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# 1) marqueur initial: utiliser immédiatement l'icône stationnaire au lieu du vieux point bleu + VOUS
pat_initial=re.compile(r'\s*js\.append\("window\.eqUserDot=L\.circleMarker.*?window\.eqUserLabel=L\.marker.*?;"\);',re.S)
m=pat_initial.search(s)
if not m:
    raise SystemExit('marqueur initial ancien introuvable')
new='\n            js.append("window.eqLastUser=[").append(la).append(\',\').append(lo).append("];window.eqUserDot=L.marker([").append(la).append(\',\').append(lo).append("],{icon:L.divIcon({className:\'\',html:\\"<div class=\'eqUserPin\'><img src=\'file:///android_asset/marker_user_stationary.png\'></div>\\",iconSize:[48,64],iconAnchor:[24,58]}),zIndexOffset:5000}).addTo(map).bindPopup(\'<b>VOUS ÊTES ICI</b>\');");'
s=s[:m.start()]+new+s[m.end():]

# 2) déplacement utilisateur: vitesse GPS OU déplacement réel > 12 m, pour éviter les faux immobiles
pat=re.compile(r'window\.eqMoveUser=\(la,lo,heading,speed\)=>\{.*?\};"\+',re.S)
m=pat.search(s)
if not m:
    raise SystemExit('eqMoveUser v182 introuvable')
new_js=r'''window.eqMoveUser=(la,lo,heading,speed)=>{const ll=[Number(la),Number(lo)],sp=Number(speed||0),now=Date.now();let delta=0;if(window.eqLastUser){const a=window.eqLastUser[0]*Math.PI/180,b=ll[0]*Math.PI/180,dp=(ll[0]-window.eqLastUser[0])*Math.PI/180,dl=(ll[1]-window.eqLastUser[1])*Math.PI/180,q=Math.sin(dp/2)**2+Math.cos(a)*Math.cos(b)*Math.sin(dl/2)**2;delta=2*6371000*Math.asin(Math.sqrt(q));}const recent=!window.eqLastUserTs||(now-window.eqLastUserTs)<20000;const moving=sp>0.8||(recent&&delta>12);window.eqLastUser=ll;window.eqLastUserTs=now;const src=moving?'file:///android_asset/marker_user_moving.png':'file:///android_asset/marker_user_stationary.png';const ico=L.divIcon({className:'',html:\"<div class='eqUserPin'><img src='\"+src+\"'></div>\",iconSize:moving?[58,58]:[48,64],iconAnchor:moving?[29,29]:[24,58]});if(window.eqUserDot&&window.eqUserDot.remove)window.eqUserDot.remove();if(window.eqUserLabel&&window.eqUserLabel.remove){window.eqUserLabel.remove();window.eqUserLabel=null;}window.eqUserDot=L.marker(ll,{icon:ico,zIndexOffset:5000}).addTo(map).bindPopup(moving?'<b>EN MOUVEMENT</b>':'<b>VOUS ÊTES ICI</b>');if(eqFollowUser)map.panTo(ll,{animate:true,duration:.45});};"+'''
s=s[:m.start()]+new_js+s[m.end():]

# 3) ajouter une vraie couche caméras MTQ utilisant le même marqueur sécurité rouge
insert_before='    private String radarSafetyJavascript(double userLat,double userLng){'
if insert_before not in s:
    raise SystemExit('radarSafetyJavascript introuvable')
if 'private String mtqCameraJavascript' not in s:
    method=r'''    private String mtqCameraJavascript(double userLat,double userLng){
        String url=JSONObject.quote(MTQ_CAMERA_WFS);
        return "const mtqCamUrl="+url+";"
            +"const mtqCamLayer=L.layerGroup().addTo(map);"
            +"function mtqCamIcon(){return L.divIcon({className:'',html:\"<div class='radarCam'><img src='file:///android_asset/marker_safety.png'></div>\",iconSize:[44,44],iconAnchor:[22,22]});}"
            +"fetch(mtqCamUrl).then(r=>r.json()).then(g=>{for(const f of (g.features||[])){if(!f.geometry||f.geometry.type!=='Point')continue;const c=f.geometry.coordinates,lo=Number(c[0]),la=Number(c[1]);if(!isFinite(la)||!isFinite(lo))continue;const p=f.properties||{},name=String(p.nom||p.NOM||p.description||p.route||'Caméra MTQ');L.marker([la,lo],{icon:mtqCamIcon()}).addTo(mtqCamLayer).bindPopup('<b>Caméra MTQ / Québec 511</b><br>'+name.replace(/</g,'&lt;'));}}).catch(()=>{});";
    }

'''
    s=s.replace(insert_before,method+insert_before,1)

needle='          js+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+'
replacement='          js+mtqCameraJavascript(centerLat,centerLng)+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+'
if needle not in s:
    raise SystemExit('assemblage JS carte introuvable')
s=s.replace(needle,replacement,1)

# 4) finance: couche distincte, haversine autonome, ATM+banque 50 km et popup distance
start=s.find('    private String foodPoiJavascript(double userLat,double userLng){')
end=s.find('\n    private String mtqCameraJavascript',start)
if start<0 or end<0:
    raise SystemExit('foodPoiJavascript bornes introuvables')
new_food=r'''    private String foodPoiJavascript(double userLat,double userLng){
        String endpoint=JSONObject.quote(FOOD_POI_OVERPASS);
        String q="[out:json][timeout:25];("+
            "nwr(around:60000,"+userLat+","+userLng+")[brand=\"McDonald's\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[name=\"McDonald's\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[brand=\"Tim Hortons\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[name=\"Tim Hortons\"];"+
            "nwr(around:50000,"+userLat+","+userLng+")[amenity=\"atm\"];"+
            "nwr(around:50000,"+userLat+","+userLng+")[amenity=\"bank\"];"+
            ");out center tags;";
        return "const poiEndpoint="+endpoint+";const poiQuery="+JSONObject.quote(q)+";"
            +"const foodLayer=L.layerGroup().addTo(map),financeLayer=L.layerGroup().addTo(map);"
            +"function poiDistance(a,b,c,d){const R=6371000,p=Math.PI/180,dl=(c-a)*p,dg=(d-b)*p,q=Math.sin(dl/2)**2+Math.cos(a*p)*Math.cos(c*p)*Math.sin(dg/2)**2;return 2*R*Math.asin(Math.sqrt(q));}"
            +"function poiIcon(kind){const src=kind==='mcd'?'file:///android_asset/marker_mccafe.png':(kind==='tim'?'file:///android_asset/marker_tim.png':'file:///android_asset/marker_finance.png');const cls=kind==='mcd'?'mcdPoi':(kind==='tim'?'timPoi':'atmPoi');return L.divIcon({className:'',html:\"<div class='foodPoi \"+cls+\"'><img src='\"+src+\"'></div>\",iconSize:kind==='atm'?[50,50]:[46,46],iconAnchor:kind==='atm'?[25,25]:[23,23]});}"
            +"function escPoi(x){return String(x||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}"
            +"fetch(poiEndpoint,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:'data='+encodeURIComponent(poiQuery)}).then(r=>r.json()).then(d=>{const seen=new Set();for(const e of (d.elements||[])){const t=e.tags||{},name=String(t.name||t.brand||t.operator||''),low=name.toLowerCase();const kind=(t.amenity==='atm'||t.amenity==='bank')?'atm':(low.includes(\"mcdonald\")?'mcd':(low.includes('tim hortons')?'tim':null));if(!kind)continue;const la=Number(e.lat||(e.center&&e.center.lat)),lo=Number(e.lon||(e.center&&e.center.lon));if(!isFinite(la)||!isFinite(lo))continue;const dist=poiDistance("+userLat+","+userLng+",la,lo);if(kind==='atm'&&dist>50000)continue;const key=kind+':'+la.toFixed(5)+':'+lo.toFixed(5);if(seen.has(key))continue;seen.add(key);const addr=[t['addr:housenumber'],t['addr:street'],t['addr:city']].filter(Boolean).join(' ');const label=kind==='atm'?(name||(t.amenity==='bank'?'Institution financière':'Guichet automatique')):name;const layer=kind==='atm'?financeLayer:foodLayer;L.marker([la,lo],{icon:poiIcon(kind)}).addTo(layer).bindPopup('<b>'+escPoi(label)+'</b>'+(addr?'<br>'+escPoi(addr):'')+(kind==='atm'?'<br>'+((dist/1000).toFixed(1))+' km':''));}}).catch(()=>{});";
    }
'''
s=s[:start]+new_food+s[end:]

if 'ESSENCE_QUEBEC_183' not in s:
    s=s.replace('private static final String ESSENCE_QUEBEC_182="1.8.2";','private static final String ESSENCE_QUEBEC_182="1.8.2";\n    private static final String ESSENCE_QUEBEC_183="1.8.3";',1)
p.write_text(s,encoding='utf-8')

g=Path('EssenceLanaudiere/app/build.gradle')
gs=g.read_text(encoding='utf-8')
gs=re.sub(r'versionCode\s+\d+','versionCode 32',gs,count=1)
gs=re.sub(r"versionName\s+'[^']+'","versionName '1.8.3'",gs,count=1)
g.write_text(gs,encoding='utf-8')

required=['ESSENCE_QUEBEC_183','marker_user_stationary.png','marker_user_moving.png','mtqCameraJavascript(centerLat,centerLng)','marker_safety.png','around:50000','amenity=\\"bank\\"','marker_finance.png','delta>12','sp>0.8']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('patch_v183 incomplet: '+repr(missing))
print('Essence Quebec 1.8.3: marqueurs et couches carte corriges')
