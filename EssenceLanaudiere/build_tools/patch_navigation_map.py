from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Interface plus compacte pour laisser davantage de place aux résultats et à la carte.
s=s.replace('TextView title=text("Compare les\\nprix à proximité",38,true,Color.WHITE);',
            'TextView title=text("Compare les prix près de toi",30,true,Color.WHITE);')
s=s.replace('TextView mapTitle=text("CARTE DES STATIONS • ● VOUS",15,true,ink);',
            'TextView mapTitle=text("CARTE INTERACTIVE • ⛽ STATIONS • 🍔 • ☕ • 📷",14,true,ink);')
s=s.replace('mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(330)));',
            'mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(420)));')
s=s.replace('box.addView(w,new LinearLayout.LayoutParams(-1,dp(520)));',
            'box.addView(w,new LinearLayout.LayoutParams(-1,dp(650)));')

# Carte : 3 fonds, recentrage, vue globale et échelle. Les blocs ci-dessous sont
# du code Java à injecter, donc on les garde comme chaînes Python littérales.
old = r'''          "<script>const map=L.map('map',{zoomControl:true}).setView(["+centerLat+","+centerLng+"],12);L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{subdomains:'abcd',maxZoom:20,attribution:'© OpenStreetMap contributors © CARTO'}).addTo(map);"+js+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+"</script></body></html>";'''

new = r'''          "<script>const map=L.map('map',{zoomControl:true,preferCanvas:true}).setView(["+centerLat+","+centerLng+"],12);"+
          "const clean=L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{subdomains:'abcd',maxZoom:20,attribution:'© OpenStreetMap contributors © CARTO'}).addTo(map);"+
          "const streets=L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:20,attribution:'© OpenStreetMap contributors'});"+
          "const satellite=L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',{maxZoom:19,attribution:'Tiles © Esri'});"+
          "L.control.layers({'Clair':clean,'Rues':streets,'Satellite':satellite},null,{position:'topright',collapsed:true}).addTo(map);"+
          "L.control.scale({metric:true,imperial:false,position:'bottomleft'}).addTo(map);"+
          js+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+
          "const Nav=L.Control.extend({options:{position:'bottomright'},onAdd:function(){const d=L.DomUtil.create('div','navBox leaflet-bar');d.innerHTML='<button id=meBtn>⌖</button><button id=allBtn>⛽</button>';L.DomEvent.disableClickPropagation(d);return d;}});new Nav().addTo(map);"+
          "document.getElementById('meBtn').onclick=()=>map.setView(["+centerLat+","+centerLng+"],15,{animate:true});"+
          "document.getElementById('allBtn').onclick=()=>{if(bounds.length>1)map.fitBounds(bounds,{padding:[45,45],maxZoom:14,animate:true});};"+
          "setTimeout(()=>map.invalidateSize(),250);"+
          "</script></body></html>";'''

if old not in s:
    raise SystemExit('ancrage HTML carte introuvable')
s=s.replace(old,new,1)

# CSS mobile : gros contrôles, sélecteur de fond lisible et popups propres.
css_anchor=".you{background:#1677ff;color:#fff;font:bold 12px sans-serif;border-radius:10px;padding:3px 8px;text-align:center;box-shadow:0 1px 4px #0005}"
css_add=".you{background:#1677ff;color:#fff;font:bold 12px sans-serif;border-radius:10px;padding:3px 8px;text-align:center;box-shadow:0 1px 4px #0005}.navBox{background:transparent!important;border:0!important;box-shadow:none!important;display:flex;gap:7px;flex-direction:column}.navBox button{width:46px;height:46px;border:0;border-radius:14px;background:#fff;color:#10213a;font-size:23px;font-weight:900;box-shadow:0 3px 12px #0006}.leaflet-control-layers{border-radius:12px!important;box-shadow:0 3px 12px #0005!important}.leaflet-control-zoom a{width:42px!important;height:42px!important;line-height:42px!important;font-size:24px!important}.leaflet-popup-content{font:14px sans-serif;line-height:1.4}.leaflet-control-scale-line{background:#ffffffdd}"
if css_anchor in s and '.navBox{' not in s:
    s=s.replace(css_anchor,css_add,1)

required=['Compare les prix près de toi','CARTE INTERACTIVE','dp(420)','dp(650)','const streets=L.tileLayer','const satellite=L.tileLayer','L.control.layers','L.control.scale','id=meBtn','id=allBtn','.navBox{']
missing=[x for x in required if x not in s]
if missing:
    raise SystemExit('navigation/carte incomplète: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Navigation carte améliorée: Clair/Rues/Satellite, recentrage, vue globale, échelle et grands contrôles')
