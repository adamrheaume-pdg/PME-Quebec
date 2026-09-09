from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

if 'RADAR_PHOTO_GEOJSON' not in s:
    anchor='    private static final String CHANNEL_ID="best_price";\n'
    add='''    private static final String RADAR_PHOTO_GEOJSON="https://ws.mapserver.transports.gouv.qc.ca/swtq?service=wfs&version=2.0.0&request=getfeature&typename=ms:radars_photos&outfile=RadarPhoto&srsname=EPSG:4326&outputformat=geojson";\n    private static final int RADAR_ALERT_METERS=1000;\n'''
    if anchor not in s: raise SystemExit('channel anchor missing')
    s=s.replace(anchor,anchor+add,1)

if 'radarSafetyJavascript' not in s:
    marker='    private void showMapDialog(){\n'
    methods=r'''    private String radarSafetyJavascript(double userLat,double userLng){
        String url=JSONObject.quote(RADAR_PHOTO_GEOJSON);
        return "const radarUrl="+url+";"
            +"const radarLayer=L.layerGroup().addTo(map);"
            +"function radarDist(a,b,c,d){const R=6371000,p=Math.PI/180,dl=(c-a)*p,dg=(d-b)*p,q=Math.sin(dl/2)**2+Math.cos(a*p)*Math.cos(c*p)*Math.sin(dg/2)**2;return 2*R*Math.asin(Math.sqrt(q));}"
            +"function radarIcon(hot){return L.divIcon({className:'',html:\"<div class='radarCam \"+(hot?\"radarBlink\":\"\")+\"'>📷</div>\",iconSize:[38,38],iconAnchor:[19,19]});}"
            +"function showRadarBanner(distance,label){let e=document.getElementById('radarSafety');if(!e){e=document.createElement('div');e.id='radarSafety';e.className='radarSafety';document.body.appendChild(e);}e.innerHTML=\"<b>📷 RADAR PHOTO À PROXIMITÉ</b><br><span>\"+Math.round(distance)+\" m • \"+label+\"</span><small>Respecte la limite affichée.</small>\";}"
            +"fetch(radarUrl).then(r=>r.json()).then(g=>{let nearest=null;for(const f of (g.features||[])){if(!f.geometry||f.geometry.type!=='Point')continue;const c=f.geometry.coordinates,lo=Number(c[0]),la=Number(c[1]);if(!isFinite(la)||!isFinite(lo))continue;const pr=f.properties||{},label=String(pr.typeAppareil||pr.type_appareil||pr.TYPE_APPAREIL||pr.nom||pr.NOM||'Radar photo');const d=radarDist("+userLat+","+userLng+",la,lo);const hot=d<=1000;L.marker([la,lo],{icon:radarIcon(hot)}).addTo(radarLayer).bindPopup('<b>📷 '+label.replace(/</g,'&lt;')+'</b><br>Donnée officielle MTMD');if(hot&&(!nearest||d<nearest.d))nearest={d:d,label:label};}if(nearest)showRadarBanner(nearest.d,nearest.label);}).catch(()=>{});";
    }

'''
    if marker not in s: raise SystemExit('showMapDialog anchor missing')
    s=s.replace(marker,methods+marker,1)

old=".marker-cluster div{background:#1677ff;color:#fff;font:bold 13px sans-serif}</style></head><body><div id='map'></div>"
new=".marker-cluster div{background:#1677ff;color:#fff;font:bold 13px sans-serif}.radarCam{width:34px;height:34px;border-radius:12px;background:#111827;border:2px solid #ffd84a;display:flex;align-items:center;justify-content:center;font-size:21px;box-shadow:0 3px 10px #0008}.radarBlink{animation:radarFlash .55s infinite alternate}@keyframes radarFlash{0%{transform:scale(1);background:#111827;box-shadow:0 0 4px #fff}50%{transform:scale(1.14);background:#fff;box-shadow:0 0 18px #ffd84a}100%{transform:scale(1.05);background:#ffd84a;box-shadow:0 0 22px #fff}}.radarSafety{position:absolute;left:50%;top:12px;transform:translateX(-50%);z-index:9999;min-width:240px;max-width:82%;padding:10px 14px;border-radius:16px;background:rgba(10,24,46,.96);border:2px solid #ffd84a;color:#fff;text-align:center;font-family:sans-serif;box-shadow:0 8px 24px #0007}.radarSafety b{color:#ffd84a;font-size:13px}.radarSafety span{font-size:12px}.radarSafety small{display:block;color:#bfe1ff;margin-top:4px;font-size:10px}</style></head><body><div id='map'></div>"
if 'radarBlink' not in s:
    if old not in s: raise SystemExit('map css anchor missing')
    s=s.replace(old,new,1)

old_script='"<script>const map=L.map(\'map\',{zoomControl:true}).setView(["+centerLat+","+centerLng+"],12);L.tileLayer(\'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png\',{subdomains:\'abcd\',maxZoom:20,attribution:\'© OpenStreetMap contributors © CARTO\'}).addTo(map);"+js+"</script></body></html>";'
new_script='"<script>const map=L.map(\'map\',{zoomControl:true}).setView(["+centerLat+","+centerLng+"],12);L.tileLayer(\'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png\',{subdomains:\'abcd\',maxZoom:20,attribution:\'© OpenStreetMap contributors © CARTO\'}).addTo(map);"+js+radarSafetyJavascript(centerLat,centerLng)+"</script></body></html>";'
if 'js+radarSafetyJavascript(centerLat,centerLng)' not in s:
    if old_script not in s: raise SystemExit('map script anchor missing')
    s=s.replace(old_script,new_script,1)

required=['RADAR_PHOTO_GEOJSON','RADAR_ALERT_METERS=1000','radarSafetyJavascript','radarBlink','RADAR PHOTO À PROXIMITÉ','Respecte la limite affichée.']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('radar safety patch incomplete: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Radar safety layer installed at 1 km using official MTMD GeoJSON')
