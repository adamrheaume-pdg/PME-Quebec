from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# 1) Carte beaucoup plus immersive et proche de la maquette.
s=s.replace('mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(505)));',
            'mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(620)));')

# 2) Satellite sombre par défaut; rues restent disponibles via le sélecteur.
s=s.replace("const streets=L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);",
            "const streets=L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'});")
s=s.replace("const satellite=L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',{maxZoom:19,attribution:'Tiles © Esri'});",
            "const satellite=L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',{maxZoom:19,attribution:'Tiles © Esri'}).addTo(map);")

# 3) Ne pas saturer la carte avec restaurants/cafés dans la vue principale.
s=s.replace('js+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+',
            'js+radarSafetyJavascript(centerLat,centerLng)+')

# 4) Marqueurs prix façon maquette: épingles colorées simples, logos retirés de la carte.
old_icon='String icon="<div class=\'pin\'><img src=\'"+brandLogoUrl(x.name)+"\' onerror=\\\"this.style.display=\'none\'\\\"><span>"+price+"</span></div>";'
new_icon='String tone=x.price<=stations.stream().mapToDouble(z->z.price).min().orElse(x.price)+2.0?"best":(x.price>=stations.stream().mapToDouble(z->z.price).max().orElse(x.price)-2.0?"high":"mid"); String icon="<div class=\'pricePin "+tone+"\'><b>"+price+"</b><i></i></div>";'
if old_icon in s:
    s=s.replace(old_icon,new_icon)

# 5) CSS des marqueurs et contrôles, avec esthétique plus proche de la référence.
css_old=".pin{min-width:64px;height:36px;background:#fff;border:2px solid #31405a;border-radius:10px;padding:3px 5px;display:flex;align-items:center;gap:4px;box-shadow:0 2px 6px #0005}.pin img{width:26px;height:26px;object-fit:contain}.pin span{font:bold 13px sans-serif;color:#283348;white-space:nowrap}"
css_new=".pricePin{position:relative;min-width:58px;height:40px;padding:0 10px;border-radius:12px;display:flex;align-items:center;justify-content:center;color:#fff;font:bold 16px sans-serif;border:3px solid #fff;box-shadow:0 4px 14px #000a,0 0 14px currentColor}.pricePin i{position:absolute;left:50%;bottom:-13px;transform:translateX(-50%);width:0;height:0;border-left:10px solid transparent;border-right:10px solid transparent;border-top:14px solid currentColor}.pricePin.best{background:#22a861;color:#22d87e}.pricePin.mid{background:#d89a18;color:#ffc43a}.pricePin.high{background:#d83a42;color:#ff515d}.pricePin b{color:#fff;white-space:nowrap}"
s=s.replace(css_old,css_new)

# 6) Contrôles Leaflet plus discrets comme la référence.
s=s.replace('width:58px;height:58px','width:52px;height:52px')
s=s.replace('width:56px!important;height:56px!important','width:50px!important;height:50px!important')
s=s.replace('width:54px!important;height:54px!important;line-height:54px!important','width:48px!important;height:48px!important;line-height:48px!important')

# 7) Barre de filtres visuellement moins dominante, même fonctions conservées.
s=s.replace('filters.addView(fuelSpinner,new LinearLayout.LayoutParams(0,dp(48),1));',
            'filters.addView(fuelSpinner,new LinearLayout.LayoutParams(0,dp(42),1));')
s=s.replace('LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(48),1);',
            'LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(42),1);')
s=s.replace('LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(56),dp(48));',
            'LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(48),dp(42));')

# 8) Carte station sélectionnée en style HUD sombre sous la carte, avec itinéraire immédiat.
hero_old='hero=text("📍 Recherche du meilleur prix…",16,true,Color.rgb(90,255,165)); hero.setPadding(dp(14),dp(12),dp(14),dp(12)); hero.setBackground(neonGlassPanel());'
hero_new='hero=text("Sélectionne une station sur la carte",17,true,Color.rgb(80,255,155)); hero.setPadding(dp(18),dp(16),dp(18),dp(16)); hero.setBackground(neonGlassPanel()); hero.setGravity(Gravity.CENTER_VERTICAL);'
s=s.replace(hero_old,hero_new)

# 9) Fond principal plus noir/bleuté et panneaux magenta moins envahissants.
s=s.replace('sc.setBackground(referenceNight());','sc.setBackground(referenceNight());')

required=['dp(620)','pricePin','const satellite=L.tileLayer','satellite=L.tileLayer','Sélectionne une station sur la carte']
missing=[x for x in required if x not in s]
if missing:
    raise SystemExit('patch_reference_map_v2 incomplet: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Essence Québec: carte immersive référence v2 appliquée')
