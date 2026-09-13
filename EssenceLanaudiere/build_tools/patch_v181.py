from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Essence Quebec 1.8.1 - commandes utilisateur 2026-09-12

# 1) Pont JS -> page station et double-tap sur la fiche Leaflet.
js_enable='stationMap.getSettings().setJavaScriptEnabled(true);'
bridge='''stationMap.getSettings().setJavaScriptEnabled(true); stationMap.addJavascriptInterface(new Object(){ @JavascriptInterface public void openStation(final int index){ runOnUiThread(()->{ if(index>=0&&index<lastStations.size()){ eqAwardXp(2); showStationReference(lastStations.get(index)); } }); } },"EQ");'''
if 'addJavascriptInterface(new Object()' not in s:
    s=s.replace(js_enable,bridge,1)

old_loop='''        for(Station x:stations){
            String price=String.format(Locale.CANADA_FRENCH,"%.1f",x.price);'''
new_loop='''        int eqStationIndex=0;
        for(Station x:stations){
            final int stationIndex=eqStationIndex++;
            String price=String.format(Locale.CANADA_FRENCH,"%.1f",x.price);'''
if old_loop in s:
    s=s.replace(old_loop,new_loop,1)

old_pop='''            String pop="<div style=\\"font-size:17px;font-weight:900;color:#10213a\\">"+html(x.name)+"</div><div style=\\"margin:4px 0 7px;color:#56657a\\">"+html(x.address)+"</div><div style=\\"font-size:18px;font-weight:900;color:#19d982\\">"+price+" ¢/L <span style=\\"color:#26364c;font-size:14px\\">• "+String.format(Locale.CANADA_FRENCH,"%.1f",x.distance)+" km</span></div>";
            js.append("L.marker([").append(x.lat).append(',').append(x.lng).append("],{icon:L.divIcon({className:'',html:").append(JSONObject.quote(icon)).append(",iconSize:[70,48],iconAnchor:[35,48]})}).addTo(map).bindPopup(").append(JSONObject.quote(pop)).append(");");'''
new_pop='''            String pop="<div class='eqStationPopup' data-i='"+stationIndex+"' style=\\"font-size:17px;font-weight:900;color:#10213a;touch-action:manipulation;user-select:none\\">"+html(x.name)+"<div style=\\"margin:4px 0 7px;color:#56657a;font-size:15px;font-weight:500\\">"+html(x.address)+"</div><div style=\\"font-size:18px;font-weight:900;color:#19d982\\">"+price+" ¢/L <span style=\\"color:#26364c;font-size:14px\\">• "+String.format(Locale.CANADA_FRENCH,"%.1f",x.distance)+" km</span></div><div style=\\"font-size:11px;color:#738097;margin-top:7px\\">Double-tape pour ouvrir la station</div></div>";
            js.append("{const eqM=L.marker([").append(x.lat).append(',').append(x.lng).append("],{icon:L.divIcon({className:'',html:").append(JSONObject.quote(icon)).append(",iconSize:[70,48],iconAnchor:[35,48]})}).addTo(map).bindPopup(").append(JSONObject.quote(pop)).append(");let eqLastTap=0;eqM.on('popupopen',()=>{const el=eqM.getPopup().getElement();if(!el)return;const box=el.querySelector('.eqStationPopup');if(!box)return;box.ondblclick=()=>EQ.openStation(").append(stationIndex).append(");box.ontouchend=()=>{const n=Date.now();if(eqLastTap&&n-eqLastTap<380){EQ.openStation(").append(stationIndex).append(");eqLastTap=0;}else{eqLastTap=n;}};});}");'''
if old_pop in s:
    s=s.replace(old_pop,new_pop,1)

# 2) Liasse d'argent immediatement a droite du prix de la page station (~75% hauteur chiffres).
old_price='''        TextView price=text(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L",s.price),46,true,Color.rgb(72,255,151)); price.setShadowLayer(dp(7),0,0,Color.rgb(25,210,120)); price.setPadding(0,dp(20),0,dp(8)); root.addView(price);'''
new_price='''        LinearLayout priceRow=new LinearLayout(this); priceRow.setGravity(Gravity.CENTER_VERTICAL); priceRow.setPadding(0,dp(18),0,dp(6));
        TextView price=text(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L",s.price),46,true,Color.rgb(72,255,151)); price.setShadowLayer(dp(7),0,0,Color.rgb(25,210,120)); price.setPadding(0,0,dp(10),0); priceRow.addView(price,new LinearLayout.LayoutParams(0,-2,1));
        ImageView stationMoney=new ImageView(this); stationMoney.setImageResource(R.drawable.money_bundle); stationMoney.setScaleType(ImageView.ScaleType.CENTER_INSIDE); stationMoney.setAdjustViewBounds(true); priceRow.addView(stationMoney,new LinearLayout.LayoutParams(dp(72),dp(48))); root.addView(priceRow);'''
if old_price in s:
    s=s.replace(old_price,new_price,1)

# 3) Logos locaux: Ultramar fourni par l'utilisateur, station inconnue = pompe verte transparente.
if 'private int localBrandLogoResource(String name)' not in s:
    marker='    private String brandLogoUrl(String name)'
    methods='''    private int localBrandLogoResource(String name){
        String n=name==null?"":name.toLowerCase(Locale.CANADA_FRENCH);
        if(n.contains("ultramar"))return R.drawable.logo_ultramar;
        boolean known=n.contains("petro")||n.contains("shell")||n.contains("esso")||n.contains("costco")||n.contains("irving")||n.contains("couche-tard")||n.contains("coupe-tard")||n.contains("circle k")||n.contains("harnois")||n.contains("sonic")||n.contains("canadian tire")||n.contains("gas+");
        return known?0:R.drawable.logo_unknown;
    }\n\n'''
    s=s.replace(marker,methods+marker,1)

old_load='''    private void loadBrandLogo(ImageView view,String name){
        view.setContentDescription(name);
        new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(brandLogoUrl(name)).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);Bitmap b=BitmapFactory.decodeStream(c.getInputStream());if(b!=null)runOnUiThread(()->view.setImageBitmap(b));}catch(Exception ignored){}}).start();
    }'''
new_load='''    private void loadBrandLogo(ImageView view,String name){
        view.setContentDescription(name);
        int local=localBrandLogoResource(name);
        if(local!=0){ view.setImageResource(local); view.setScaleType(ImageView.ScaleType.CENTER_INSIDE); return; }
        new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(brandLogoUrl(name)).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);Bitmap b=BitmapFactory.decodeStream(c.getInputStream());if(b!=null)runOnUiThread(()->view.setImageBitmap(b));}catch(Exception ignored){}}).start();
    }'''
if old_load in s:
    s=s.replace(old_load,new_load,1)

old_brand='''ImageView brand=new ImageView(this); brand.setScaleType(ImageView.ScaleType.CENTER_INSIDE); brand.setBackground(round(Color.WHITE,8)); body.addView(brand,new LinearLayout.LayoutParams(dp(62),dp(62))); loadBrandLogo(brand,x.name);'''
new_brand='''ImageView brand=new ImageView(this); brand.setScaleType(ImageView.ScaleType.CENTER_INSIDE); brand.setBackground(round(Color.WHITE,8)); int localLogo=localBrandLogoResource(x.name); body.addView(brand,new LinearLayout.LayoutParams(localLogo==R.drawable.logo_unknown?dp(118):dp(62),localLogo==R.drawable.logo_unknown?dp(74):dp(62))); loadBrandLogo(brand,x.name);'''
if old_brand in s:
    s=s.replace(old_brand,new_brand,1)

# 4) Prix autour du Quebec: afficher les dernieres valeurs reelles disponibles en ¢ CA/L.
start=s.find('    private View eqNeighborPricePanel(){')
if start>=0:
    end=s.find('\n    private ',start+10)
    new_method='''    private View eqNeighborPricePanel(){
        LinearLayout card=neonPanel();card.setPadding(dp(14),dp(12),dp(14),dp(12));
        TextView title=text("PRIX MOYEN AUTOUR DU QUÉBEC",16,true,Color.WHITE);card.addView(title);
        TextView sub=text("Ordinaire • ¢ CA/L • données du 12-09-2026",11,false,Color.rgb(180,198,220));card.addView(sub);
        String[] names={"Ontario","Nouveau-Brunswick","Maine","New Hampshire","New York","Vermont"};
        float[] vals={184.9f,194.6f,157.9f,156.0f,160.6f,160.5f};
        String[] notes={"repère Ontario","repère Moncton","AAA + CAD","AAA + CAD","AAA + CAD","AAA + CAD"};
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<names.length;i++){
            LinearLayout r=new LinearLayout(this);r.setPadding(0,dp(7),0,dp(7));
            TextView left=text(names[i],14,true,Color.rgb(55,205,255));
            TextView right=text(String.format(Locale.CANADA_FRENCH,"%.1f ¢/L",vals[i]),14,true,Color.rgb(80,255,150));
            right.setContentDescription(notes[i]);
            r.addView(left,new LinearLayout.LayoutParams(0,-2,1));r.addView(right);grid.addView(r);
        }
        card.addView(grid);
        TextView src=text("USA: AAA 12-09-2026, converti avec USD/CAD 1,3866 (Banque du Canada 11-09). Ontario/N.-B.: derniers repères disponibles; aucune valeur inventée.",10,false,Color.rgb(150,170,195));card.addView(src);
        return card;
    }'''
    s=s[:start]+new_method+s[end:]

# 5) Version interne.
if 'ESSENCE_QUEBEC_181' not in s:
    s=s.replace('private static final String ESSENCE_QUEBEC_180="1.8.0";', 'private static final String ESSENCE_QUEBEC_180="1.8.0";\n    private static final String ESSENCE_QUEBEC_181="1.8.1";',1)

required=['ESSENCE_QUEBEC_181','addJavascriptInterface(new Object()','Double-tape pour ouvrir la station','stationMoney','logo_ultramar','logo_unknown','184.9f,194.6f,157.9f,156.0f,160.6f,160.5f']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('patch_v181 incomplet: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Essence Quebec 1.8.1: commandes utilisateur appliquees')
