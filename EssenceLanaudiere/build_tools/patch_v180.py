from pathlib import Path

p=Path("EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java")
s=p.read_text(encoding="utf-8")

if "private boolean eqAtHome=false;" not in s:
    s=s.replace("public class MainActivity extends Activity {",
                "public class MainActivity extends Activity {\n    private boolean eqAtHome=false;",1)

a=s.find('        LinearLayout search=neonPanel();')
b=s.find('        LinearLayout filters=', a)
if a>=0 and b>0:
    replacement='''        eqAtHome=true;
        LinearLayout tabs=new LinearLayout(this); tabs.setGravity(Gravity.CENTER); tabs.setBackground(round(Color.rgb(7,12,26),12));
        Button tMap=primaryButton("Carte"); Button tFav=ghostButton("Favoris");
        tMap.setOnClickListener(v->{}); tFav.setOnClickListener(v->showFavorites());
        tabs.addView(tMap,new LinearLayout.LayoutParams(0,dp(50),1)); tabs.addView(tFav,new LinearLayout.LayoutParams(0,dp(50),1));
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2); tp.bottomMargin=dp(8); page.addView(tabs,tp);

'''
    s=s[:a]+replacement+s[b:]

needle='stationMap.setHorizontalScrollBarEnabled(false);'
if needle in s and "eqMapGestureGuard" not in s:
    s=s.replace(needle, needle+'''
        stationMap.getSettings().setSupportZoom(true);
        final boolean[] eqMapGestureGuard={false};
        stationMap.setOnTouchListener((v,e)->{
            int pc=e.getPointerCount();
            if(pc>=2){eqMapGestureGuard[0]=true;v.getParent().requestDisallowInterceptTouchEvent(true);}
            if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){
                eqMapGestureGuard[0]=false;v.getParent().requestDisallowInterceptTouchEvent(false);
            } else if(pc<2&&!eqMapGestureGuard[0]&&e.getActionMasked()==MotionEvent.ACTION_MOVE){
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });''',1)

s=s.replace('TextView addr=text(displayStationAddress(x),14,false,ink); if(isHighest)eqCycleAlertText(addr);',
            'TextView addr=text(displayStationAddress(x),14,false,isCheapest?Color.rgb(0,145,230):ink); if(isHighest)eqCycleAlertText(addr);')
s=s.replace('TextView nm=text(x.name,19,true,ink); if(isHighest)eqCycleAlertText(nm);',
            'TextView nm=text(x.name,19,true,isCheapest?Color.rgb(0,145,230):ink); if(isHighest)eqCycleAlertText(nm);')

if "private void eqAwardXp(int amount)" not in s:
    marker='    private SharedPreferences eqProfilePrefs(){'
    method='''    private void eqAwardXp(int amount){
        if(amount<=0)return;
        SharedPreferences p=eqProfilePrefs();
        p.edit().putInt("xp",Math.max(0,p.getInt("xp",0)+amount)).apply();
    }

'''
    if marker in s:s=s.replace(marker,method+marker,1)

s=s.replace('c.setOnClickListener(v->showStationReference(x));',
            'c.setOnClickListener(v->{eqAwardXp(2);showStationReference(x);});')
s=s.replace('route.setOnClickListener(v->openMap(s));',
            'route.setOnClickListener(v->{eqAwardXp(10);openMap(s);});')
s=s.replace('toggleStationFavorite(s); favorite.setText',
            'toggleStationFavorite(s); eqAwardXp(5); favorite.setText')

if "private View eqNeighborPricePanel()" not in s:
    marker='    private View fakeSearchBar(){'
    methods='''    private View eqNeighborPricePanel(){
        LinearLayout card=neonPanel();card.setPadding(dp(14),dp(12),dp(14),dp(12));
        TextView title=text("PRIX MOYEN AUTOUR DU QUÉBEC",16,true,Color.WHITE);card.addView(title);
        TextView sub=text("Ordinaire • ¢ CA/L • dernières données disponibles",11,false,Color.rgb(180,198,220));card.addView(sub);
        String[] names={"Ontario","Nouveau-Brunswick","Maine","New Hampshire","New York","Vermont"};
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
        SharedPreferences p=getSharedPreferences("eq_neighbor_prices",MODE_PRIVATE);
        for(String n:names){
            LinearLayout r=new LinearLayout(this);r.setPadding(0,dp(7),0,dp(7));
            TextView left=text(n,14,true,Color.rgb(55,205,255));
            String k=n.toLowerCase(Locale.CANADA_FRENCH).replace(" ","_").replace("-","_");
            float v=p.getFloat(k,Float.NaN);
            TextView right=text(Float.isNaN(v)?"Mise à jour…":String.format(Locale.CANADA_FRENCH,"%.1f ¢/L",v),14,true,Color.rgb(80,255,150));
            r.addView(left,new LinearLayout.LayoutParams(0,-2,1));r.addView(right);grid.addView(r);
        }
        card.addView(grid);
        TextView src=text("Valeurs mises en cache localement; aucune valeur inventée si la source n'est pas disponible.",10,false,Color.rgb(150,170,195));card.addView(src);
        return card;
    }

'''
    if marker in s:s=s.replace(marker,methods+marker,1)

show=s.find('    private void showMain(){')
if show>=0:
    end=s.find('    private void addTools(){',show)
    sub=s[show:end]
    if 'eqNeighborPricePanel()' not in sub:
        pos=sub.find('page.addView(status);')
        if pos>=0:
            pos += len('page.addView(status);')
            sub=sub[:pos]+'\n        LinearLayout.LayoutParams npp=new LinearLayout.LayoutParams(-1,-2);npp.topMargin=dp(8);page.addView(eqNeighborPricePanel(),npp);'+sub[pos:]
            s=s[:show]+sub+s[end:]

start=s.find('    private void showStatsReference(){')
end=s.find('    private View statCell(',start)
if start>=0 and end>start:
    new=r'''    private void showStatsReference(){
        eqAtHome=false;
        SharedPreferences p=eqProfilePrefs();
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackground(referenceNight());
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(16),dp(18),dp(28));sc.addView(root);applySafeInsets(root,18,12,18,22);
        TextView back=text("‹",44,true,Color.WHITE);back.setOnClickListener(v->showMain());root.addView(back,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView avatar=new ImageView(this);avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);avatar.setBackground(round(Color.rgb(20,28,50),80));
        File af=new File(new File(getFilesDir(),"profile"),"avatar.jpg");
        if(af.exists()){android.graphics.Bitmap bm=android.graphics.BitmapFactory.decodeFile(af.getAbsolutePath());if(bm!=null)avatar.setImageBitmap(bm);}else avatar.setImageResource(R.drawable.essence_quebec_logo);
        head.addView(avatar,new LinearLayout.LayoutParams(dp(92),dp(92)));
        LinearLayout htxt=new LinearLayout(this);htxt.setOrientation(LinearLayout.VERTICAL);htxt.setPadding(dp(14),0,0,0);
        String nm=p.getString("name","").trim();if(nm.isEmpty())nm="Pilote Québécois";
        htxt.addView(text(nm+"  ⚜",27,true,Color.WHITE));
        int xp=p.getInt("xp",0), level=1+xp/1000, cur=xp%1000;
        htxt.addView(text("Niveau "+level,17,true,Color.WHITE));
        htxt.addView(text(cur+" / 1 000 XP",13,true,Color.rgb(210,215,230)));
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress(cur);htxt.addView(bar,new LinearLayout.LayoutParams(-1,dp(16)));
        head.addView(htxt,new LinearLayout.LayoutParams(0,-2,1));root.addView(head);
        double savings=Double.longBitsToDouble(p.getLong("savings_total",Double.doubleToLongBits(0)));
        LinearLayout eco=neonPanel();eco.setPadding(dp(18),dp(18),dp(18),dp(18));eco.addView(text("ÉCONOMIE AUJOURD'HUI",18,true,Color.WHITE));
        LinearLayout er=new LinearLayout(this);er.setGravity(Gravity.CENTER_VERTICAL);
        er.addView(text(String.format(Locale.CANADA_FRENCH,"%.2f $",savings),42,true,Color.rgb(35,235,140)),new LinearLayout.LayoutParams(0,-2,1));
        ImageView money=new ImageView(this);money.setImageResource(R.drawable.money_bundle);money.setScaleType(ImageView.ScaleType.CENTER_INSIDE);er.addView(money,new LinearLayout.LayoutParams(dp(105),dp(70)));eco.addView(er);
        eco.addView(text("Ce n'est pas juste de l'essence, c'est plus de liberté.",13,false,Color.WHITE));
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);ep.topMargin=dp(20);root.addView(eco,ep);
        TextView st=text("TES STATS",20,true,Color.WHITE);st.setPadding(0,dp(22),0,dp(8));root.addView(st);
        int trips=p.getInt("trip_count",0), fav=eqFavoriteCount();
        LinearLayout stats=new LinearLayout(this);
        stats.addView(statCell("⛽",String.valueOf(trips),"pleins suivis"),new LinearLayout.LayoutParams(0,dp(112),1));
        stats.addView(statCell("$",String.format(Locale.CANADA_FRENCH,"%.1f $",savings),"économisés\nce mois-ci"),new LinearLayout.LayoutParams(0,dp(112),1));
        stats.addView(statCell("★",String.valueOf(fav),"meilleurs prix\ntrouvés"),new LinearLayout.LayoutParams(0,dp(112),1));root.addView(stats);
        TextView rank=text("CLASSEMENT DES STATIONS",20,true,Color.WHITE);rank.setPadding(0,dp(22),0,dp(8));root.addView(rank);
        List<Station> rr=new ArrayList<>(lastStations);Collections.sort(rr,(aa,bb)->Double.compare(aa.price,bb.price));int i=1;
        for(Station x:rr){LinearLayout r=neonPanel();r.setPadding(dp(10),dp(8),dp(10),dp(8));LinearLayout ln=new LinearLayout(this);ln.setGravity(Gravity.CENTER_VERTICAL);
            ln.addView(text(String.valueOf(i),20,true,Color.rgb(80,255,150)),new LinearLayout.LayoutParams(dp(42),-2));
            ImageView logo=new ImageView(this);logo.setImageResource(stationBannerResource(x.name));logo.setScaleType(ImageView.ScaleType.CENTER_CROP);ln.addView(logo,new LinearLayout.LayoutParams(dp(42),dp(42)));
            TextView nn=text(x.name,16,true,Color.WHITE);nn.setPadding(dp(10),0,0,0);ln.addView(nn,new LinearLayout.LayoutParams(0,-2,1));
            ln.addView(text(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L",x.price),16,true,Color.WHITE));r.addView(ln);root.addView(r);if(++i>3)break;}
        TextView quote=text("« Moins cher aujourd’hui,\nplus loin demain. »",22,false,Color.WHITE);quote.setGravity(Gravity.CENTER);quote.setTypeface(Typeface.create("cursive",Typeface.ITALIC));quote.setPadding(0,dp(28),0,0);root.addView(quote);
        setContentView(sc);
    }

'''
    s=s[:start]+new+s[end:]

s=s.replace('profile.setOnClickListener(v->showPilotProfile());','profile.setOnClickListener(v->showStatsReference());')

for sig in ['private void showStationReference(Station s){','private void showMissionsReference(){','private void showFavorites(){','private void showOfflineManager(){','private void showPilotProfile(){']:
    s=s.replace(sig,sig+'\n        eqAtHome=false;',1)

if '@Override public void onBackPressed()' not in s:
    method=r'''
    @Override public void onBackPressed(){
        if(!eqAtHome){showMain();return;}
        new AlertDialog.Builder(this)
            .setTitle("Quitter Essence Québec ?")
            .setMessage("Voulez-vous vraiment fermer l'application ?")
            .setNegativeButton("ANNULER",null)
            .setPositiveButton("QUITTER",(d,w)->{try{finishAndRemoveTask();}catch(Exception e){finish();}})
            .show();
    }
'''
    idx=s.rfind('\n}')
    s=s[:idx]+method+s[idx:]

if 'ESSENCE_QUEBEC_180' not in s:
    s=s.replace('private static final String CHANNEL_ID="best_price";',
                'private static final String CHANNEL_ID="best_price";\n    private static final String ESSENCE_QUEBEC_180="1.8.0";',1)

p.write_text(s,encoding="utf-8")
print("Essence Quebec 1.8.0 UI/navigation/profile patch applied")
