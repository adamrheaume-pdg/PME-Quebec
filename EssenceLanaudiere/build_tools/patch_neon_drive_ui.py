from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Direction visuelle: nuit tropicale / néon rose-violet-bleu, proche de la référence fournie,
# sans ajouter ni copier d'illustration externe. On conserve toutes les fonctions et données 1.2.5+.
s=s.replace('private final int navy=Color.rgb(3,18,46), navyDark=Color.rgb(6,30,67), ink=Color.rgb(34,46,68), accent=Color.rgb(0,145,255), pale=Color.rgb(246,249,255);',
'''private final int navy=Color.rgb(5,8,20), navyDark=Color.rgb(12,15,35), ink=Color.rgb(238,242,255), accent=Color.rgb(255,42,166), pale=Color.rgb(11,15,31);''')

# Barres système cohérentes avec l'ambiance.
s=s.replace('getWindow().setStatusBarColor(Color.rgb(18,18,18));','getWindow().setStatusBarColor(Color.rgb(8,8,18));')
s=s.replace('getWindow().setNavigationBarColor(Color.rgb(18,18,18));','getWindow().setNavigationBarColor(Color.rgb(8,8,18));')

# Branding principal plus proche du visuel de référence.
s=s.replace('TextView subTitle=text("COMPARE • ÉCONOMISE • ROULE",12,true,Color.rgb(114,205,255));',
            'TextView subTitle=text("LES BONS PRIX • TOUJOURS PLUS LOIN",12,true,Color.rgb(255,102,211));')
s=s.replace('TextView mapTitle=text("CARTE 360° • PRIX • SERVICES",15,true,ink);',
            'TextView mapTitle=text("CARTE • LISTE • FAVORIS",15,true,Color.WHITE);')

# Recherche et cartes en verre sombre.
s=s.replace('search.setPadding(dp(12),dp(12),dp(12),dp(12));','search.setPadding(dp(12),dp(12),dp(12),dp(12)); search.setBackground(neonGlassPanel());')
s=s.replace('hero.setBackground(neonDarkPanel());','hero.setBackground(neonGlassPanel());')

# Prix sur les fiches: grand, vert lumineux, fond sombre au lieu d'un bloc blanc.
s=s.replace('TextView price=text(String.format(Locale.CANADA_FRENCH,"%.1f ›",x.price),31,true,ink); price.setGravity(Gravity.CENTER); price.setBackground(round(Color.WHITE,4));',
'''TextView price=text(String.format(Locale.CANADA_FRENCH,"%.1f ›",x.price),31,true,Color.rgb(74,255,154)); price.setGravity(Gravity.CENTER); price.setShadowLayer(dp(5),0,0,Color.rgb(35,255,135)); price.setBackground(neonPricePanel());''')
s=s.replace('dist.setBackground(round(ink,6));','dist.setBackground(neonDistancePill());')

# Les cartes station deviennent de vrais panneaux HUD sombres.
s=s.replace('private LinearLayout cardStation(){LinearLayout l=cardWhite();l.setPadding(dp(10),dp(9),dp(10),dp(9));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(9);l.setLayoutParams(p);return l;}',
'''private LinearLayout cardStation(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(12),dp(11),dp(12),dp(11));l.setBackground(neonGlassPanel());l.setElevation(dp(7));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(10);l.setLayoutParams(p);return l;}''')

# Boutons principaux: rose/magenta, comme la référence.
s=s.replace('private GradientDrawable neonButton(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(0,100,230),Color.rgb(0,180,255)});g.setCornerRadius(dp(12));g.setStroke(dp(1),Color.rgb(120,220,255));return g;}',
'''private GradientDrawable neonButton(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(215,25,176),Color.rgb(255,47,166),Color.rgb(255,79,201)});g.setCornerRadius(dp(14));g.setStroke(dp(1),Color.rgb(255,146,226));return g;}''')

# Panneaux sombre/verre et fond coucher de soleil abstrait, sans image.
s=s.replace('private GradientDrawable premiumCard(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.rgb(255,255,255),Color.rgb(239,246,255)});g.setCornerRadius(dp(16));g.setStroke(dp(1),Color.rgb(70,165,255));return g;}',
'''private GradientDrawable premiumCard(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(16,23,48),Color.rgb(8,13,30)});g.setCornerRadius(dp(18));g.setStroke(dp(1),Color.rgb(53,88,142));return g;}''')
s=s.replace('private GradientDrawable neonDarkPanel(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(7,28,62),Color.rgb(10,52,105)});g.setCornerRadius(dp(13));g.setStroke(dp(1),Color.rgb(0,155,255));return g;}',
'''private GradientDrawable neonDarkPanel(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(12,18,39),Color.rgb(28,15,47)});g.setCornerRadius(dp(15));g.setStroke(dp(1),Color.rgb(125,65,171));return g;}''')
s=s.replace('private GradientDrawable blueBackdrop(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(2,13,35),Color.rgb(5,35,78),Color.rgb(3,18,46)});return g;}',
'''private GradientDrawable blueBackdrop(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(5,9,25),Color.rgb(35,15,58),Color.rgb(8,37,68),Color.rgb(7,10,25)});return g;}''')

# Helpers supplémentaires pour l'effet HUD / néon.
marker='    private GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}'
if 'private GradientDrawable neonGlassPanel()' not in s:
    helpers='''    private GradientDrawable neonGlassPanel(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(20,26,50),Color.rgb(12,17,35)});g.setCornerRadius(dp(18));g.setStroke(dp(1),Color.rgb(199,47,170));return g;}\n    private GradientDrawable neonPricePanel(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(7,35,35),Color.rgb(10,27,31)});g.setCornerRadius(dp(12));g.setStroke(dp(1),Color.rgb(40,226,132));return g;}\n    private GradientDrawable neonDistancePill(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(58,30,75),Color.rgb(28,28,58)});g.setCornerRadius(dp(12));g.setStroke(dp(1),Color.rgb(255,74,190));return g;}\n'''
    if marker not in s: raise SystemExit('helper round introuvable')
    s=s.replace(marker,helpers+marker,1)

# Barre de navigation visuelle en bas des outils, reliée aux fonctions existantes.
footer='TextView footer=text("Créé au Québec • adamrheaume@gmail.com",12,true,Color.rgb(210,222,238));'
if 'NAV • ACCUEIL' not in s and footer in s:
    dock='''LinearLayout dock=new LinearLayout(this); dock.setGravity(Gravity.CENTER); dock.setPadding(dp(4),dp(12),dp(4),dp(4)); dock.setBackground(neonGlassPanel());\n        Button navHome=ghostButton("⌂\\nACCUEIL"); navHome.setTextSize(11); dock.addView(navHome,new LinearLayout.LayoutParams(0,dp(62),1));\n        Button navMap=primaryButton("🗺\\nCARTE"); navMap.setTextSize(11); navMap.setOnClickListener(v->{if(!lastStations.isEmpty()) showMapDialog(); else locateAndLoad();}); LinearLayout.LayoutParams nmp=new LinearLayout.LayoutParams(0,dp(62),1); nmp.leftMargin=dp(6); dock.addView(navMap,nmp);\n        Button navSave=ghostButton("$\\nÉCONOMIES"); navSave.setTextSize(11); navSave.setOnClickListener(v->showCalculator()); LinearLayout.LayoutParams nsp=new LinearLayout.LayoutParams(0,dp(62),1); nsp.leftMargin=dp(6); dock.addView(navSave,nsp);\n        Button navFav=ghostButton("★\\nFAVORIS"); navFav.setTextSize(11); navFav.setOnClickListener(v->showFavorites()); LinearLayout.LayoutParams nfp=new LinearLayout.LayoutParams(0,dp(62),1); nfp.leftMargin=dp(6); dock.addView(navFav,nfp); page.addView(dock,new LinearLayout.LayoutParams(-1,-2));\n        // NAV • ACCUEIL • CARTE • ÉCONOMIES • FAVORIS\n        '''
    s=s.replace(footer,dock+footer,1)

# Carte Leaflet: marqueurs et contrôles dans la même palette néon.
s=s.replace("border-color:#0a78d1!important", "border-color:#ff3eae!important")
s=s.replace("box-shadow:0 5px 14px #001b3b55!important", "box-shadow:0 0 0 2px #ff4fbc55,0 7px 18px #001022aa!important")
s=s.replace("color:#0877cf", "color:#19d982")
s=s.replace("background:#1677ff;color:#fff", "background:#ff2ca8;color:#fff")

required=['LES BONS PRIX • TOUJOURS PLUS LOIN','CARTE • LISTE • FAVORIS','neonGlassPanel()','neonPricePanel()','neonDistancePill()','NAV • ACCUEIL','Color.rgb(74,255,154)','Color.rgb(255,42,166)','tile.openstreetmap.org']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('interface néon incomplète: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Essence Québec: interface néon conduite appliquée')
