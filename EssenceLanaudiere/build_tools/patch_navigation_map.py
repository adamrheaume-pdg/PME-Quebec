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

# Empêche le ScrollView Android de voler le glissement/pincement à la carte.
station_anchor='stationMap=new WebView(this); stationMap.setBackgroundColor(Color.rgb(232,238,245)); stationMap.getSettings().setJavaScriptEnabled(true); stationMap.getSettings().setDomStorageEnabled(true); stationMap.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);'
station_touch=station_anchor+' stationMap.setOverScrollMode(View.OVER_SCROLL_NEVER); stationMap.setVerticalScrollBarEnabled(false); stationMap.setHorizontalScrollBarEnabled(false); stationMap.setOnTouchListener((v,e)->{int a=e.getActionMasked(); boolean lock=a!=MotionEvent.ACTION_UP&&a!=MotionEvent.ACTION_CANCEL; ViewParent parent=v.getParent(); while(parent!=null){parent.requestDisallowInterceptTouchEvent(lock); parent=parent.getParent();} return false;});'
if station_anchor in s and 'stationMap.setOnTouchListener' not in s:
    s=s.replace(station_anchor,station_touch,1)

dialog_anchor='WebView w=new WebView(this); w.getSettings().setJavaScriptEnabled(true); w.getSettings().setDomStorageEnabled(true); w.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);'
dialog_touch=dialog_anchor+' w.setOverScrollMode(View.OVER_SCROLL_NEVER); w.setVerticalScrollBarEnabled(false); w.setHorizontalScrollBarEnabled(false); w.setOnTouchListener((v,e)->{int a=e.getActionMasked(); boolean lock=a!=MotionEvent.ACTION_UP&&a!=MotionEvent.ACTION_CANCEL; ViewParent parent=v.getParent(); while(parent!=null){parent.requestDisallowInterceptTouchEvent(lock); parent=parent.getParent();} return false;});'
if dialog_anchor in s and 'w.setOnTouchListener' not in s:
    s=s.replace(dialog_anchor,dialog_touch,1)

# Remplace aussi la base WebView CARTO afin qu'aucune ressource CARTO ne soit appelée.
s=s.replace('https://carto.com/','https://www.openstreetmap.org/')

# Carte : OpenStreetMap par défaut (aucune clé API), satellite Esri en option,
# recentrage, vue globale et échelle.
old = r'''          "<script>const map=L.map('map',{zoomControl:true}).setView(["+centerLat+","+centerLng+"],12);L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{subdomains:'abcd',maxZoom:20,attribution:'© OpenStreetMap contributors © CARTO'}).addTo(map);"+js+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+"</script></body></html>";'''

new = r'''          "<script>const map=L.map('map',{zoomControl:true,preferCanvas:true,touchZoom:true,doubleClickZoom:true,dragging:true,tapTolerance:22}).setView(["+centerLat+","+centerLng+"],12);"+
          "const streets=L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);"+
          "const satellite=L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',{maxZoom:19,attribution:'Tiles © Esri'});"+
          "L.control.layers({'Rues':streets,'Satellite':satellite},null,{position:'topright',collapsed:true}).addTo(map);"+
          "L.control.scale({metric:true,imperial:false,position:'bottomleft'}).addTo(map);"+
          js+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+
          "const Nav=L.Control.extend({options:{position:'bottomright'},onAdd:function(){const d=L.DomUtil.create('div','navBox leaflet-bar');d.innerHTML='<button id=meBtn title=\"Ma position\">⌖</button><button id=allBtn title=\"Voir les stations\">⛽</button>';L.DomEvent.disableClickPropagation(d);L.DomEvent.disableScrollPropagation(d);return d;}});new Nav().addTo(map);"+
          "document.getElementById('meBtn').onclick=()=>map.setView(["+centerLat+","+centerLng+"],15,{animate:true});"+
          "document.getElementById('allBtn').onclick=()=>{if(bounds.length>1)map.fitBounds(bounds,{padding:[55,55],maxZoom:14,animate:true});};"+
          "setTimeout(()=>map.invalidateSize(),250);"+
          "</script></body></html>";'''

if old not in s:
    raise SystemExit('ancrage HTML carte introuvable')
s=s.replace(old,new,1)

# CSS mobile : zone tactile stable, gros contrôles et popups lisibles.
css_anchor=".you{background:#1677ff;color:#fff;font:bold 12px sans-serif;border-radius:10px;padding:3px 8px;text-align:center;box-shadow:0 1px 4px #0005}"
css_add=".you{background:#1677ff;color:#fff;font:bold 12px sans-serif;border-radius:10px;padding:3px 8px;text-align:center;box-shadow:0 1px 4px #0005}html,body,#map{touch-action:none!important;overscroll-behavior:none!important}.navBox{background:transparent!important;border:0!important;box-shadow:none!important;display:flex;gap:10px;flex-direction:column}.navBox button{width:58px;height:58px;border:0;border-radius:17px;background:#fff;color:#10213a;font-size:27px;font-weight:900;box-shadow:0 4px 14px #0007}.leaflet-control-layers{border-radius:14px!important;box-shadow:0 4px 14px #0006!important;font-size:16px!important}.leaflet-control-layers-toggle{width:56px!important;height:56px!important;background-size:30px 30px!important}.leaflet-control-zoom a{width:54px!important;height:54px!important;line-height:54px!important;font-size:28px!important}.leaflet-popup-content{font:15px sans-serif;line-height:1.45;min-width:150px}.leaflet-control-scale-line{background:#ffffffee;font-size:13px}.leaflet-container{cursor:grab;-webkit-tap-highlight-color:transparent}.leaflet-container:active{cursor:grabbing}"
if css_anchor in s and '.navBox{' not in s:
    s=s.replace(css_anchor,css_add,1)

required=['Compare les prix près de toi','CARTE INTERACTIVE','dp(420)','dp(650)','tile.openstreetmap.org','const satellite=L.tileLayer','L.control.layers','L.control.scale','id=meBtn','id=allBtn','.navBox{','stationMap.setOnTouchListener','w.setOnTouchListener','touch-action:none','tapTolerance:22','width:58px','width:54px']
missing=[x for x in required if x not in s]
if missing:
    raise SystemExit('navigation/carte incomplète: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Carte sans clé API: OpenStreetMap par défaut, satellite Esri, navigation tactile conservée')
