from pathlib import Path
p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Classe des marqueurs de station pour affichage/masquage.
anchor="String pop=\"<div class='eqStationPopup'"
i=s.find(anchor)
if i<0: raise SystemExit('bloc station introuvable')
j=min(len(s),i+4500)
b=s[i:j]
if "className:'eqStationLeafletIcon'" not in b:
    if "className:'',html:" not in b: raise SystemExit('classe station introuvable')
    b=b.replace("className:'',html:","className:'eqStationLeafletIcon',html:",1)
    s=s[:i]+b+s[j:]
s=s.replace("className:'',html:\\\"<div class='eqUserPin'","className:'eqUserLeafletIcon',html:\\\"<div class='eqUserPin'")

start=s.find('    private String foodPoiJavascript(double userLat,double userLng){')
end=s.find('\n    private String mtqCameraJavascript',start)
if start<0 or end<0: raise SystemExit('foodPoiJavascript introuvable')
new=r'''    private String foodPoiJavascript(double userLat,double userLng){
        String endpoint=JSONObject.quote(FOOD_POI_OVERPASS);
        String q="[out:json][timeout:25];("+
            "nwr(around:60000,"+userLat+","+userLng+")[brand=\"McDonald's\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[name=\"McDonald's\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[brand=\"Tim Hortons\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[name=\"Tim Hortons\"];"+
            "nwr(around:50000,"+userLat+","+userLng+")[amenity=\"atm\"];"+
            "nwr(around:50000,"+userLat+","+userLng+")[amenity=\"bank\"];"+
            "nwr(around:50000,"+userLat+","+userLng+")[office=\"financial\"];"+
            ");out center tags;";
        return "const poiEndpoint="+endpoint+";const poiQuery="+JSONObject.quote(q)+";"
            +"const mcdLayer=L.layerGroup().addTo(map),timLayer=L.layerGroup().addTo(map),bankLayer=L.layerGroup().addTo(map),atmLayer=L.layerGroup().addTo(map);"
            +"function poiDistance(a,b,c,d){const R=6371000,p=Math.PI/180,dl=(c-a)*p,dg=(d-b)*p,q=Math.sin(dl/2)**2+Math.cos(a*p)*Math.cos(c*p)*Math.sin(dg/2)**2;return 2*R*Math.asin(Math.sqrt(q));}"
            +"function poiIcon(kind){const finance=(kind==='atm'||kind==='bank');const src=kind==='mcd'?'file:///android_asset/marker_mccafe.png':(kind==='tim'?'file:///android_asset/marker_timhortons.webp':'file:///android_asset/marker_finance.png');const cls=kind==='mcd'?'mcdPoi':(kind==='tim'?'timPoi':(kind==='bank'?'bankPoi':'atmPoi'));return L.divIcon({className:'',html:\"<div class='foodPoi \"+cls+\"'><img src='\"+src+\"'></div>\",iconSize:finance?[50,50]:[40,40],iconAnchor:finance?[25,25]:[20,20]});}"
            +"function escPoi(x){return String(x||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}"
            +"fetch(poiEndpoint,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:'data='+encodeURIComponent(poiQuery)}).then(r=>r.json()).then(d=>{const seen=new Set();for(const e of (d.elements||[])){const t=e.tags||{},name=String(t.name||t.brand||t.operator||''),low=name.toLowerCase();const kind=t.amenity==='atm'?'atm':((t.amenity==='bank'||t.office==='financial')?'bank':(low.includes(\"mcdonald\")?'mcd':(low.includes('tim hortons')?'tim':null)));if(!kind)continue;const la=Number(e.lat||(e.center&&e.center.lat)),lo=Number(e.lon||(e.center&&e.center.lon));if(!isFinite(la)||!isFinite(lo))continue;const dist=poiDistance("+userLat+","+userLng+",la,lo);if((kind==='atm'||kind==='bank')&&dist>50000)continue;const key=kind+':'+la.toFixed(5)+':'+lo.toFixed(5);if(seen.has(key))continue;seen.add(key);const addr=[t['addr:housenumber'],t['addr:street'],t['addr:city']].filter(Boolean).join(' ');const label=(kind==='atm'||kind==='bank')?(name||(kind==='bank'?'Institution financière':'Guichet automatique')):name;const layer=kind==='atm'?atmLayer:(kind==='bank'?bankLayer:(kind==='mcd'?mcdLayer:timLayer));L.marker([la,lo],{icon:poiIcon(kind)}).addTo(layer).bindPopup('<b>'+escPoi(label)+'</b>'+(addr?'<br>'+escPoi(addr):'')+((kind==='atm'||kind==='bank')?'<br>'+((dist/1000).toFixed(1))+' km':''));}}).catch(()=>{});";
    }

    private String layerLegendJavascript(){
        return "const eqLayerState={stations:true,mcd:true,tim:true,bank:true,atm:true,user:true};"
            +"function eqLayerClass(cls,on){document.querySelectorAll('.'+cls).forEach(e=>e.style.display=on?'':'none');}"
            +"function eqApplyLayers(){eqLayerClass('eqStationLeafletIcon',eqLayerState.stations);eqLayerClass('eqUserLeafletIcon',eqLayerState.user);const pairs=[[mcdLayer,eqLayerState.mcd],[timLayer,eqLayerState.tim],[bankLayer,eqLayerState.bank],[atmLayer,eqLayerState.atm]];for(const p of pairs){if(p[1]&&!map.hasLayer(p[0]))map.addLayer(p[0]);if(!p[1]&&map.hasLayer(p[0]))map.removeLayer(p[0]);}}"
            +"const eqLegend=L.control({position:'topright'});eqLegend.onAdd=()=>{const d=L.DomUtil.create('div','eqLegend');d.innerHTML=\"<b>AFFICHAGE</b><label><input type='checkbox' data-k='stations' checked> Stations-service</label><label><input type='checkbox' data-k='mcd' checked> McDonald’s / McCafé</label><label><input type='checkbox' data-k='tim' checked> Tim Hortons</label><label><input type='checkbox' data-k='bank' checked> Banques / caisses</label><label><input type='checkbox' data-k='atm' checked> Guichets automatiques</label><label><input type='checkbox' data-k='user' checked> Ma position</label><button data-all='1'>Tout afficher</button><button data-all='0'>Tout masquer</button>\";L.DomEvent.disableClickPropagation(d);d.querySelectorAll('input').forEach(x=>x.onchange=()=>{eqLayerState[x.dataset.k]=x.checked;eqApplyLayers();});d.querySelectorAll('button').forEach(b=>b.onclick=()=>{const on=b.dataset.all==='1';Object.keys(eqLayerState).forEach(k=>eqLayerState[k]=on);d.querySelectorAll('input').forEach(x=>x.checked=on);eqApplyLayers();});return d;};eqLegend.addTo(map);eqApplyLayers();";
    }
'''
s=s[:start]+new+s[end:]
needle='foodPoiJavascript(centerLat,centerLng)+'
if 'layerLegendJavascript()' not in s:
    if needle not in s: raise SystemExit('assemblage POI introuvable')
    s=s.replace(needle,needle+'layerLegendJavascript()+',1)
css=".eqLegend{background:#08111eee;color:#fff;padding:10px;border:1px solid #ff3aa7;border-radius:12px;font:600 12px sans-serif;min-width:170px}.eqLegend b,.eqLegend label{display:block;margin:5px 0}.eqLegend b{color:#6cff9c}.eqLegend button{margin:5px 4px 0 0;background:#111d30;color:#fff;border:1px solid #6cff9c;border-radius:7px;padding:5px;font-size:10px}"
if '.eqLegend{' not in s:
    pos=s.find('</style>')
    if pos<0: raise SystemExit('style introuvable')
    s=s[:pos]+css+s[pos:]
p.write_text(s,encoding='utf-8')
for x in ['layerLegendJavascript()','around:50000','office=\\"financial\\"','bankLayer','atmLayer','eqStationLeafletIcon','eqUserLeafletIcon']:
    if x not in s: raise SystemExit('manque '+x)
print('v1.8.9 layers OK')
