from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# V2 visuelle: garde toutes les fonctions de 1.2.5, améliore hiérarchie, carte et lisibilité.
s=s.replace('TextView title=text("Compare les prix près de toi",30,true,Color.WHITE);',
'''TextView title=text("ESSENCE QUÉBEC",31,true,Color.WHITE); title.setLetterSpacing(.045f);
        TextView subTitle=text("COMPARE • ÉCONOMISE • ROULE",12,true,Color.rgb(114,205,255)); subTitle.setLetterSpacing(.10f); page.addView(title); page.addView(subTitle);''')
# Évite l'ancien ajout du title si la ligne suivante existe après le remplacement.
s=s.replace('page.addView(title);\n        page.addView(title);','page.addView(title);')

s=s.replace('TextView mapTitle=text("CARTE INTERACTIVE • ⛽ STATIONS • 🍔 • ☕ • 📷",14,true,ink);',
'''TextView mapTitle=text("CARTE 360° • PRIX • SERVICES",15,true,ink); mapTitle.setLetterSpacing(.035f);''')

# Carte plus immersive sans devenir envahissante.
s=s.replace('mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(420)));',
              'mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(455)));')
s=s.replace('box.addView(w,new LinearLayout.LayoutParams(-1,dp(650)));',
              'box.addView(w,new LinearLayout.LayoutParams(-1,dp(690)));')

# Renforce le CSS injecté par patch_navigation_map: contrôles verre, marqueurs plus propres, attribution lisible.
anchor='.leaflet-container{cursor:grab;-webkit-tap-highlight-color:transparent}.leaflet-container:active{cursor:grabbing}'
extra=anchor+".leaflet-control-zoom,.leaflet-control-layers{overflow:hidden;border:1px solid #ffffffaa!important;backdrop-filter:blur(8px)}.leaflet-control-zoom a,.leaflet-control-layers,.navBox button{background:#ffffffed!important}.navBox button:active,.leaflet-control-zoom a:active{transform:scale(.94)}.pin{border-color:#0a78d1!important;border-radius:13px!important;box-shadow:0 5px 14px #001b3b55!important}.pin span{font-size:14px!important}.leaflet-popup-content-wrapper{border-radius:16px;box-shadow:0 8px 26px #00152e55}.leaflet-control-attribution{background:#fffffff0!important;border-radius:8px 0 0 0;font-size:10px!important}.leaflet-control-scale-line{border-radius:6px}.you{box-shadow:0 0 0 5px #1677ff33,0 4px 12px #001b3b55!important}"
if anchor in s and 'backdrop-filter:blur(8px)' not in s:
    s=s.replace(anchor,extra,1)

# Popup station: rend immédiatement visibles prix et distance.
old='String pop="<b>"+html(x.name)+"</b><br>"+html(x.address)+"<br>"+price+" ¢/L • "+String.format(Locale.CANADA_FRENCH,"%.1f",x.distance)+" km";'
new='String pop="<div style=\\\"font-size:17px;font-weight:900;color:#10213a\\\">"+html(x.name)+"</div><div style=\\\"margin:4px 0 7px;color:#56657a\\\">"+html(x.address)+"</div><div style=\\\"font-size:18px;font-weight:900;color:#0877cf\\\">"+price+" ¢/L <span style=\\\"color:#26364c;font-size:14px\\\">• "+String.format(Locale.CANADA_FRENCH,"%.1f",x.distance)+" km</span></div>";'
if old in s:
    s=s.replace(old,new,1)

required=['ESSENCE QUÉBEC','COMPARE • ÉCONOMISE • ROULE','CARTE 360° • PRIX • SERVICES','dp(455)','dp(690)','backdrop-filter:blur(8px)','tile.openstreetmap.org','id=meBtn','id=allBtn']
missing=[x for x in required if x not in s]
if missing:
    raise SystemExit('V2 visuelle incomplète: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Essence Québec: interface punchée appliquée sans retirer les fonctions 1.2.5')
