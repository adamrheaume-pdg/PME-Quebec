from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')


def replace_method_block(src,start_marker,next_marker,new_block):
    a=src.find(start_marker)
    if a<0: raise SystemExit('marqueur absent: '+start_marker)
    b=src.find(next_marker,a)
    if b<0: raise SystemExit('marqueur suivant absent: '+next_marker)
    return src[:a]+new_block+'\n\n'+src[b:]

intro=r'''    private void showIntro(){
        getWindow().setStatusBarColor(Color.rgb(12,7,28));
        getWindow().setNavigationBarColor(Color.rgb(5,7,18));
        FrameLayout frame=new FrameLayout(this); frame.setBackground(referenceSunset());
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setPadding(dp(24),dp(28),dp(24),dp(26));
        applySafeInsets(root,24,22,24,24); frame.addView(root,new FrameLayout.LayoutParams(-1,-1));

        Space top=new Space(this); root.addView(top,new LinearLayout.LayoutParams(1,0,1));
        TextView v=text("V",118,true,Color.rgb(55,180,255)); v.setGravity(Gravity.CENTER); v.setShadowLayer(dp(10),0,0,Color.rgb(255,40,180)); root.addView(v);
        TextView fleur=text("⚜",44,true,Color.WHITE); fleur.setGravity(Gravity.CENTER); fleur.setShadowLayer(dp(7),0,0,Color.rgb(0,170,255)); LinearLayout.LayoutParams flp=new LinearLayout.LayoutParams(-1,-2); flp.topMargin=-dp(76); root.addView(fleur,flp);
        TextView brand=text("ESSENCE\nQUÉBEC",55,true,Color.WHITE); brand.setGravity(Gravity.CENTER); brand.setLineSpacing(-dp(4),0.94f); brand.setShadowLayer(dp(8),dp(1),dp(3),Color.BLACK); root.addView(brand);

        Space mid=new Space(this); root.addView(mid,new LinearLayout.LayoutParams(1,0,2));
        TextView slogan=text("LES BONS PRIX\nTOUJOURS PLUS LOIN",18,true,Color.WHITE); slogan.setGravity(Gravity.CENTER); slogan.setLetterSpacing(.12f); slogan.setShadowLayer(dp(4),0,dp(2),Color.BLACK); root.addView(slogan);
        Button go=primaryButton("COMMENCER"); go.setTextSize(20); go.setOnClickListener(vw->showMain()); LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(66)); gp.topMargin=dp(30); root.addView(go,gp);
        TextView sign=text("Québec roule autrement",18,false,Color.rgb(255,155,220)); sign.setTypeface(Typeface.create("cursive",Typeface.ITALIC)); sign.setGravity(Gravity.CENTER); sign.setPadding(0,dp(16),0,0); root.addView(sign);
        setContentView(frame);
    }'''

main=r'''    private void showMain(){
        getWindow().setStatusBarColor(Color.rgb(5,7,18));
        getWindow().setNavigationBarColor(Color.rgb(5,7,18));
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackground(referenceNight());
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(14),dp(10),dp(14),dp(18)); page.setBackgroundColor(Color.TRANSPARENT); sc.addView(page);
        applySafeInsets(page,14,10,14,18);

        LinearLayout search=neonPanel(); search.setPadding(dp(10),dp(8),dp(10),dp(8));
        EditText q=new EditText(this); q.setHint("⌕  Rechercher une ville ou une adresse…"); q.setTextSize(15); q.setSingleLine(true); q.setTextColor(Color.WHITE); q.setHintTextColor(Color.rgb(180,187,205)); q.setBackground(round(Color.rgb(28,34,51),14)); q.setPadding(dp(14),0,dp(14),0); search.addView(q,new LinearLayout.LayoutParams(-1,dp(52))); page.addView(search);

        LinearLayout tabs=new LinearLayout(this); tabs.setGravity(Gravity.CENTER); tabs.setBackground(round(Color.rgb(7,12,26),12));
        Button tMap=primaryButton("Carte"); Button tList=ghostButton("Liste"); Button tFav=ghostButton("Favoris");
        tMap.setOnClickListener(v->{}); tList.setOnClickListener(v->{ if(results!=null) results.requestFocus(); }); tFav.setOnClickListener(v->showFavorites());
        tabs.addView(tMap,new LinearLayout.LayoutParams(0,dp(50),1)); tabs.addView(tList,new LinearLayout.LayoutParams(0,dp(50),1)); tabs.addView(tFav,new LinearLayout.LayoutParams(0,dp(50),1)); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2); tp.topMargin=dp(8); page.addView(tabs,tp);

        LinearLayout filters=new LinearLayout(this); filters.setGravity(Gravity.CENTER_VERTICAL); filters.setPadding(0,dp(8),0,dp(8));
        fuelSpinner=spinner(new String[]{"Ordinaire","Super","Diesel"}); sortSpinner=spinner(new String[]{"Prix / km","Moins cher","Plus proche"}); sortSpinner.setSelection(1); radiusSpinner=spinner(new String[]{"5 km","10 km","20 km","30 km","60 km"}); radiusSpinner.setSelection(3);
        filters.addView(fuelSpinner,new LinearLayout.LayoutParams(0,dp(48),1)); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(48),1); sp.leftMargin=dp(6); filters.addView(radiusSpinner,sp); Button refresh=primaryButton("↻"); refresh.setOnClickListener(v->locateAndLoad()); LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(56),dp(48)); rp.leftMargin=dp(6); filters.addView(refresh,rp); page.addView(filters);

        LinearLayout mapCard=neonPanel(); mapCard.setPadding(dp(4),dp(4),dp(4),dp(4));
        stationMap=new WebView(this); stationMap.setBackgroundColor(Color.rgb(5,10,20)); stationMap.getSettings().setJavaScriptEnabled(true); stationMap.getSettings().setDomStorageEnabled(true); stationMap.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); stationMap.setOverScrollMode(View.OVER_SCROLL_NEVER); stationMap.setVerticalScrollBarEnabled(false); stationMap.setHorizontalScrollBarEnabled(false);
        stationMap.loadDataWithBaseURL("https://www.openstreetmap.org/","<html><body style='margin:0;background:#07101d;color:white;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100%'><b>Localisation de la carte…</b></body></html>","text/html","UTF-8",null);
        mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(505))); page.addView(mapCard);

        hero=text("📍 Recherche du meilleur prix…",16,true,Color.rgb(90,255,165)); hero.setPadding(dp(14),dp(12),dp(14),dp(12)); hero.setBackground(neonGlassPanel()); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=dp(8); page.addView(hero,hp);
        status=text("Prix récents • données ouvertes • vérifie le prix à la pompe",11,false,Color.rgb(180,188,205)); status.setPadding(dp(2),dp(7),dp(2),dp(7)); page.addView(status);
        results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); page.addView(results);

        LinearLayout dock=new LinearLayout(this); dock.setGravity(Gravity.CENTER); dock.setPadding(dp(4),dp(10),dp(4),dp(4)); dock.setBackground(round(Color.rgb(5,10,22),14));
        Button h=ghostButton("⌂\nAccueil"); Button m=primaryButton("▣\nCarte"); Button e=ghostButton("$\nÉconomies"); Button pr=ghostButton("★\nProfil");
        h.setTextSize(11);m.setTextSize(11);e.setTextSize(11);pr.setTextSize(11);
        h.setOnClickListener(v->showMain()); m.setOnClickListener(v->{if(!lastStations.isEmpty())showMapDialog();}); e.setOnClickListener(v->showStatsReference()); pr.setOnClickListener(v->showMissionsReference());
        dock.addView(h,new LinearLayout.LayoutParams(0,dp(64),1)); dock.addView(m,new LinearLayout.LayoutParams(0,dp(64),1)); dock.addView(e,new LinearLayout.LayoutParams(0,dp(64),1)); dock.addView(pr,new LinearLayout.LayoutParams(0,dp(64),1)); LinearLayout.LayoutParams dpDock=new LinearLayout.LayoutParams(-1,-2); dpDock.topMargin=dp(12); page.addView(dock,dpDock);
        setContentView(sc); requestPermissionsAndLocate();
    }'''

s=replace_method_block(s,'    private void showIntro(){','    private void showMain(){',intro)
s=replace_method_block(s,'    private void showMain(){','    private void addTools(){',main)

# Les fiches ouvrent maintenant une vraie page station inspirée de la référence.
s=s.replace('c.setOnClickListener(v->openMap(x));','c.setOnClickListener(v->showStationReference(x));')

marker='    private View fakeSearchBar(){'
if 'private void showStationReference(Station s)' not in s:
    extra=r'''    private LinearLayout neonPanel(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setBackground(neonGlassPanel()); l.setElevation(dp(7)); return l; }

    private void showStationReference(Station s){
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackground(referenceNight()); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(16),dp(18),dp(24)); sc.addView(root); applySafeInsets(root,18,12,18,22);
        TextView back=text("‹",44,true,Color.WHITE); back.setOnClickListener(v->showMain()); root.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView name=text(s.name,27,true,Color.WHITE); root.addView(name); TextView addr=text(displayStationAddress(s),14,false,Color.rgb(190,198,215)); root.addView(addr);
        TextView price=text(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L",s.price),46,true,Color.rgb(72,255,151)); price.setShadowLayer(dp(7),0,0,Color.rgb(25,210,120)); price.setPadding(0,dp(20),0,dp(8)); root.addView(price);
        TextView best=text("●  Meilleur prix à proximité !",14,true,Color.rgb(55,235,142)); root.addView(best);
        Button route=primaryButton("ITINÉRAIRE"); route.setTextSize(18); route.setOnClickListener(v->openMap(s)); LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,dp(60)); rlp.topMargin=dp(18); root.addView(route,rlp);
        LinearLayout quick=new LinearLayout(this); quick.setGravity(Gravity.CENTER); String[] qa={"◷\nHoraires","⌂\nServices","★\nFavori","⇧\nPartager"}; for(String a:qa){Button b=ghostButton(a);b.setTextSize(11);quick.addView(b,new LinearLayout.LayoutParams(0,dp(66),1));} root.addView(quick);
        TextView ev=text("Évolution du prix",20,true,Color.WHITE); ev.setPadding(0,dp(22),0,dp(8)); root.addView(ev); TextView chart=text("1,80 ┤       ●\n1,70 ┤ ●  ●   ●\n1,60 ┤          ●  ●  ●\n      Lun Mar Mer Jeu Ven Sam Dim",15,true,Color.rgb(94,255,162)); chart.setTypeface(Typeface.MONOSPACE); chart.setBackground(neonGlassPanel()); chart.setPadding(dp(12),dp(14),dp(12),dp(14)); root.addView(chart);
        TextView near=text("Prix des autres stations à proximité",18,true,Color.WHITE); near.setPadding(0,dp(20),0,dp(8)); root.addView(near); int n=0; for(Station x:lastStations){if(x==s)continue; LinearLayout row=neonPanel(); row.setPadding(dp(12),dp(10),dp(12),dp(10)); LinearLayout line=new LinearLayout(this); TextView nm=text(x.name,15,true,Color.WHITE); TextView pp=text(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L   %.1f km",x.price,x.distance),15,true,Color.rgb(80,255,150)); line.addView(nm,new LinearLayout.LayoutParams(0,-2,1)); line.addView(pp); row.addView(line); root.addView(row); if(++n>=3)break; }
        setContentView(sc);
    }

    private void showStatsReference(){
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackground(referenceNight()); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(16),dp(18),dp(24)); sc.addView(root); applySafeInsets(root,18,12,18,22);
        TextView back=text("‹",44,true,Color.WHITE); back.setOnClickListener(v->showMain()); root.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView who=text("Pilote Québécois  ⚜",27,true,Color.WHITE); root.addView(who); TextView lvl=text("Niveau 6                                      420 / 1 000 XP",12,true,Color.rgb(210,215,230)); root.addView(lvl); TextView xp=text("━━━━━━━━━━━━━━━━━━━━",22,true,Color.rgb(255,42,166)); xp.setShadowLayer(dp(5),0,0,Color.rgb(0,180,255)); root.addView(xp);
        double save=0; if(lastStations.size()>1){double min=Double.MAX_VALUE,max=0;for(Station x:lastStations){min=Math.min(min,x.price);max=Math.max(max,x.price);}save=(max-min)*40/100.0;}
        LinearLayout eco=neonPanel(); eco.setPadding(dp(16),dp(16),dp(16),dp(16)); TextView eh=text("ÉCONOMIE AUJOURD'HUI",17,true,Color.WHITE); eco.addView(eh); TextView val=text(String.format(Locale.CANADA_FRENCH,"%.2f $",save),42,true,Color.rgb(32,234,140)); eco.addView(val); eco.addView(text("Ce n'est pas juste de l'essence, c'est plus de liberté.",13,false,Color.WHITE)); LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2); ep.topMargin=dp(18); root.addView(eco,ep);
        TextView st=text("TES STATS",20,true,Color.WHITE); st.setPadding(0,dp(22),0,dp(8)); root.addView(st); LinearLayout stats=new LinearLayout(this); stats.addView(statCell("⛽","12","pleins suivis"),new LinearLayout.LayoutParams(0,dp(112),1)); stats.addView(statCell("$","98,5 $","économisés\nce mois-ci"),new LinearLayout.LayoutParams(0,dp(112),1)); stats.addView(statCell("★","3","meilleurs prix\ntrouvés"),new LinearLayout.LayoutParams(0,dp(112),1)); root.addView(stats);
        TextView rank=text("CLASSEMENT DES STATIONS",20,true,Color.WHITE); rank.setPadding(0,dp(22),0,dp(8)); root.addView(rank); int i=1; for(Station x:lastStations){LinearLayout r=neonPanel(); r.setPadding(dp(12),dp(10),dp(12),dp(10)); LinearLayout ln=new LinearLayout(this); ln.addView(text(String.valueOf(i),20,true,Color.rgb(80,255,150)),new LinearLayout.LayoutParams(dp(42),-2)); ln.addView(text(x.name,16,true,Color.WHITE),new LinearLayout.LayoutParams(0,-2,1)); ln.addView(text(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L",x.price),16,true,Color.WHITE)); r.addView(ln); root.addView(r); if(++i>3)break; }
        TextView quote=text("« Moins cher aujourd’hui,\nplus loin demain. »",22,false,Color.WHITE); quote.setGravity(Gravity.CENTER); quote.setTypeface(Typeface.create("cursive",Typeface.ITALIC)); quote.setPadding(0,dp(30),0,0); root.addView(quote); setContentView(sc);
    }

    private View statCell(String icon,String value,String label){ LinearLayout c=neonPanel(); c.setGravity(Gravity.CENTER); c.setPadding(dp(6),dp(8),dp(6),dp(8)); TextView i=text(icon,24,true,Color.rgb(70,255,155)); i.setGravity(Gravity.CENTER); c.addView(i); TextView v=text(value,20,true,Color.WHITE); v.setGravity(Gravity.CENTER); c.addView(v); TextView l=text(label,10,true,Color.rgb(205,210,225)); l.setGravity(Gravity.CENTER); c.addView(l); return c; }

    private void showMissionsReference(){
        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setBackground(referenceSunset()); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(20),dp(18),dp(26)); sc.addView(root); applySafeInsets(root,18,16,18,24);
        TextView back=text("‹",44,true,Color.WHITE); back.setOnClickListener(v->showMain()); root.addView(back,new LinearLayout.LayoutParams(-1,dp(52))); TextView title=text("MISSIONS",38,true,Color.WHITE); title.setGravity(Gravity.CENTER); title.setShadowLayer(dp(6),0,dp(2),Color.rgb(255,35,170)); root.addView(title); TextView sub=text("ÉCONOMISE ET EXPLORE LE QUÉBEC",15,true,Color.WHITE); sub.setGravity(Gravity.CENTER); sub.setLetterSpacing(.08f); root.addView(sub);
        root.addView(missionCard("⛽","TROUVE LE MEILLEUR PRIX","Autour de toi","+ 50 XP")); root.addView(missionCard("☑","FAIS UN PLEIN","et enregistre-le","+ 30 XP")); root.addView(missionCard("●","EXPLORE UNE NOUVELLE VILLE","Découvre de nouveaux prix","+ 40 XP")); root.addView(missionCard("▥","ÉCONOMISE 50 $","Sur un mois","+ 100 XP")); TextView q=text("Québec\nToujours plus loin",31,false,Color.rgb(255,102,218)); q.setTypeface(Typeface.create("cursive",Typeface.ITALIC)); q.setGravity(Gravity.END); q.setPadding(0,dp(42),dp(10),0); root.addView(q); setContentView(sc);
    }

    private View missionCard(String icon,String title,String sub,String xp){ LinearLayout c=neonPanel(); c.setPadding(dp(14),dp(14),dp(14),dp(14)); LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); TextView i=text(icon,31,true,Color.rgb(86,255,169)); i.setGravity(Gravity.CENTER); row.addView(i,new LinearLayout.LayoutParams(dp(54),dp(54))); LinearLayout tx=new LinearLayout(this); tx.setOrientation(LinearLayout.VERTICAL); tx.addView(text(title,16,true,Color.WHITE)); tx.addView(text(sub,13,false,Color.rgb(210,214,226))); row.addView(tx,new LinearLayout.LayoutParams(0,-2,1)); row.addView(text(xp,17,true,Color.rgb(92,255,174))); c.addView(row); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(14); c.setLayoutParams(p); return c; }

    private GradientDrawable referenceSunset(){ GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.rgb(20,8,49),Color.rgb(78,21,91),Color.rgb(224,64,139),Color.rgb(255,138,100),Color.rgb(8,11,25)}); return g; }
    private GradientDrawable referenceNight(){ GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(4,8,20),Color.rgb(10,18,38),Color.rgb(12,8,28)}); return g; }

'''
    s=s.replace(marker,extra+marker,1)

required=['private void showStationReference(Station s)','private void showStatsReference()','private void showMissionsReference()','referenceSunset()','LES BONS PRIX\\nTOUJOURS PLUS LOIN','Rechercher une ville ou une adresse','ITINÉRAIRE','ÉCONOMIE AUJOURD\'HUI','MISSIONS']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('Refonte référence incomplète: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Essence Québec: refonte maquette de référence appliquée')
