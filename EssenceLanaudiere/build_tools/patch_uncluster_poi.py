from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# 1) Stop clustering gas stations: each station remains an individual marker with its logo/price.
s=s.replace("StringBuilder js=new StringBuilder(\"const bounds=[];const cluster=L.markerClusterGroup({showCoverageOnHover:false,maxClusterRadius:46,spiderfyOnMaxZoom:true});\");",
            "StringBuilder js=new StringBuilder(\"const bounds=[];\");")
s=s.replace("js.append(\"cluster.addLayer(L.marker([\").append(x.lat).append(',').append(x.lng).append(\"],{icon:L.divIcon({className:'',html:\").append(JSONObject.quote(icon)).append(\",iconSize:[70,48],iconAnchor:[35,48]})}).bindPopup(\").append(JSONObject.quote(pop)).append(\"));\");",
            "js.append(\"L.marker([\").append(x.lat).append(',').append(x.lng).append(\"],{icon:L.divIcon({className:'',html:\").append(JSONObject.quote(icon)).append(\",iconSize:[70,48],iconAnchor:[35,48]})}).addTo(map).bindPopup(\").append(JSONObject.quote(pop)).append(\");\");")
s=s.replace("js.append(\"map.addLayer(cluster);if(bounds.length>1)map.fitBounds(bounds,{padding:[35,35],maxZoom:14});\");",
            "js.append(\"if(bounds.length>1)map.fitBounds(bounds,{padding:[35,35],maxZoom:14});\");")

# 2) Remove marker-cluster JS/CSS dependencies from generated map HTML when present.
for dep in [
    "<link rel='stylesheet' href='https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.css'>",
    "<link rel='stylesheet' href='https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.Default.css'>",
    "<script src='https://unpkg.com/leaflet.markercluster@1.5.3/dist/leaflet.markercluster.js'></script>"
]: s=s.replace(dep,'')

# 3) McDonald's + Tim Hortons as a completely separate POI layer using OpenStreetMap Overpass.
if 'FOOD_POI_OVERPASS' not in s:
    anchor='    private static final String CHANNEL_ID="best_price";\n'
    add='    private static final String FOOD_POI_OVERPASS="https://overpass-api.de/api/interpreter";\n'
    if anchor not in s: raise SystemExit('channel anchor missing')
    s=s.replace(anchor,anchor+add,1)

if 'foodPoiJavascript' not in s:
    marker='    private String radarSafetyJavascript(double userLat,double userLng){\n'
    methods=r'''    private String foodPoiJavascript(double userLat,double userLng){
        String endpoint=JSONObject.quote(FOOD_POI_OVERPASS);
        String q="[out:json][timeout:18];("+
            "nwr(around:60000,"+userLat+","+userLng+")[brand=\"McDonald's\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[name=\"McDonald's\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[brand=\"Tim Hortons\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[name=\"Tim Hortons\"];"+
            ");out center tags;";
        return "const poiEndpoint="+endpoint+";const poiQuery="+JSONObject.quote(q)+";"
            +"const foodLayer=L.layerGroup().addTo(map);"
            +"function poiIcon(kind){const emo=kind==='mcd'?'🍔':'☕';const cls=kind==='mcd'?'mcdPoi':'timPoi';return L.divIcon({className:'',html:\"<div class='foodPoi \"+cls+\"'>\"+emo+\"</div>\",iconSize:[38,38],iconAnchor:[19,19]});}"
            +"function escPoi(x){return String(x||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}"
            +"fetch(poiEndpoint,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:'data='+encodeURIComponent(poiQuery)}).then(r=>r.json()).then(d=>{const seen=new Set();for(const e of (d.elements||[])){const t=e.tags||{},name=String(t.name||t.brand||'');const low=name.toLowerCase();const kind=low.includes(\"mcdonald\")?'mcd':(low.includes('tim hortons')?'tim':null);if(!kind)continue;const la=Number(e.lat||(e.center&&e.center.lat)),lo=Number(e.lon||(e.center&&e.center.lon));if(!isFinite(la)||!isFinite(lo))continue;const key=kind+':'+la.toFixed(5)+':'+lo.toFixed(5);if(seen.has(key))continue;seen.add(key);const addr=[t['addr:housenumber'],t['addr:street'],t['addr:city']].filter(Boolean).join(' ');L.marker([la,lo],{icon:poiIcon(kind)}).addTo(foodLayer).bindPopup('<b>'+escPoi(name)+'</b>'+(addr?'<br>'+escPoi(addr):''));}}).catch(()=>{});";
    }

'''
    if marker not in s: raise SystemExit('radar method anchor missing')
    s=s.replace(marker,methods+marker,1)

# CSS injection is intentionally independent of radar CSS wording.
css_add=".foodPoi{width:34px;height:34px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:21px;border:2px solid #fff;box-shadow:0 3px 10px #0007}.mcdPoi{background:#efc200}.timPoi{background:#8b2c1d}"
if '.foodPoi{' not in s:
    # Insert immediately before the first </style> embedded in the Java map HTML.
    pos=s.find('</style>')
    if pos < 0: raise SystemExit('map style closing tag missing')
    s=s[:pos]+css_add+s[pos:]

# Inject POI fetch after station rendering and radar overlay.
needle='js+radarSafetyJavascript(centerLat,centerLng)+"</script></body></html>";'
if 'foodPoiJavascript(centerLat,centerLng)' not in s:
    if needle not in s: raise SystemExit('map script injection anchor missing')
    s=s.replace(needle,'js+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+"</script></body></html>";',1)

required=['FOOD_POI_OVERPASS','foodPoiJavascript','🍔','☕','McDonald','Tim Hortons','.foodPoi{']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('poi patch incomplete: '+repr(missing))
if 'markerClusterGroup' in s or 'cluster.addLayer' in s or 'map.addLayer(cluster)' in s:
    raise SystemExit('station clustering still present')

p.write_text(s,encoding='utf-8')
print('Gas clustering removed; McDonald burger and Tim Hortons coffee markers added')
