from pathlib import Path

import zipfile

asset_pack=Path('EssenceLanaudiere/build_tools/user_assets.zip')
app_main=Path('EssenceLanaudiere/app/src/main')
if not asset_pack.exists():
    raise SystemExit('asset pack missing: '+str(asset_pack))
with zipfile.ZipFile(asset_pack,'r') as z:
    z.extractall(app_main)

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

if 'private LocationManager liveLocationManager;' not in s:
    s=s.replace('    private Location lastLocation;\n',
                '    private Location lastLocation;\n    private LocationManager liveLocationManager;\n    private LocationListener liveLocationListener;\n',1)

old_dot='''            js.append("L.circleMarker([").append(la).append(',').append(lo).append("],{radius:10,color:'#fff',weight:3,fillColor:'#1677ff',fillOpacity:1}).addTo(map).bindPopup('<b>VOUS ÊTES ICI</b>');");
            js.append("L.marker([").append(la).append(',').append(lo).append("],{icon:L.divIcon({className:'',html:\\\"<div class='you'>VOUS</div>\\\",iconSize:[54,22],iconAnchor:[27,-12]})}).addTo(map);");'''
new_dot='''            js.append("window.eqUserDot=L.circleMarker([").append(la).append(',').append(lo).append("],{radius:10,color:'#fff',weight:3,fillColor:'#1677ff',fillOpacity:1}).addTo(map).bindPopup('<b>VOUS ÊTES ICI</b>');");
            js.append("window.eqUserLabel=L.marker([").append(la).append(',').append(lo).append("],{icon:L.divIcon({className:'',html:\\\"<div class='you'>VOUS</div>\\\",iconSize:[54,22],iconAnchor:[27,-12]})}).addTo(map);");'''
if old_dot in s:
    s=s.replace(old_dot,new_dot,1)

nav_anchor='''          "document.getElementById('allBtn').onclick=()=>{if(bounds.length>1)map.fitBounds(bounds,{padding:[55,55],maxZoom:14,animate:true});};"+
          "setTimeout(()=>map.invalidateSize(),250);"+'''
nav_repl='''          "let eqFollowUser=true;map.on('dragstart zoomstart',()=>{eqFollowUser=false;});"+
          "window.eqMoveUser=(la,lo,heading)=>{const ll=[Number(la),Number(lo)];if(window.eqUserDot)window.eqUserDot.setLatLng(ll);if(window.eqUserLabel)window.eqUserLabel.setLatLng(ll);if(eqFollowUser)map.panTo(ll,{animate:true,duration:.45});};"+
          "document.getElementById('meBtn').onclick=()=>{eqFollowUser=true;map.setView(["+centerLat+","+centerLng+"],15,{animate:true});};"+
          "document.getElementById('allBtn').onclick=()=>{eqFollowUser=false;if(bounds.length>1)map.fitBounds(bounds,{padding:[55,55],maxZoom:14,animate:true});};"+
          "setTimeout(()=>map.invalidateSize(),250);"+'''
if nav_anchor in s:
    s=s.replace(nav_anchor,nav_repl,1)

s=s.replace('lastStations=new ArrayList<>(s); renderStationMap(s);',
            'lastStations=new ArrayList<>(s); renderStationMap(s); startLiveMapTracking();')

station_head='''        TextView back=text("‹",44,true,Color.WHITE); back.setOnClickListener(v->showMain()); root.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView name=text(s.name,27,true,Color.WHITE); root.addView(name); TextView addr=text(displayStationAddress(s),14,false,Color.rgb(190,198,215)); root.addView(addr);'''
station_head_new='''        TextView back=text("‹",44,true,Color.WHITE); back.setOnClickListener(v->showMain()); root.addView(back,new LinearLayout.LayoutParams(-1,dp(52)));
        ImageView banner=new ImageView(this); banner.setImageResource(stationBannerResource(s.name)); banner.setScaleType(ImageView.ScaleType.CENTER_CROP); banner.setAdjustViewBounds(false); banner.setBackground(round(Color.rgb(8,12,25),18)); banner.setContentDescription("Bannière "+s.name); LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,dp(235)); blp.bottomMargin=dp(16); root.addView(banner,blp);
        TextView name=text(s.name,27,true,Color.WHITE); root.addView(name); TextView addr=text(displayStationAddress(s),14,false,Color.rgb(190,198,215)); root.addView(addr);'''
if station_head in s:
    s=s.replace(station_head,station_head_new,1)

quick='''        LinearLayout quick=new LinearLayout(this); quick.setGravity(Gravity.CENTER); String[] qa={"◷\\nHoraires","⌂\\nServices","★\\nFavori","⇧\\nPartager"}; for(String a:qa){Button b=ghostButton(a);b.setTextSize(11);quick.addView(b,new LinearLayout.LayoutParams(0,dp(66),1));} root.addView(quick);'''
quick_new='''        LinearLayout quick=new LinearLayout(this); quick.setGravity(Gravity.CENTER);
        Button hours=ghostButton("◷\\nHoraires"); hours.setTextSize(11); hours.setOnClickListener(v->showStationHours(s)); quick.addView(hours,new LinearLayout.LayoutParams(0,dp(72),1));
        Button services=ghostButton("⌂\\nServices"); services.setTextSize(11); services.setOnClickListener(v->showStationServices(s)); quick.addView(services,new LinearLayout.LayoutParams(0,dp(72),1));
        Button favorite=ghostButton(isStationFavorite(s)?"★\\nFavori":"☆\\nFavori"); favorite.setTextSize(11); favorite.setOnClickListener(v->{toggleStationFavorite(s); favorite.setText(isStationFavorite(s)?"★\\nFavori":"☆\\nFavori");}); quick.addView(favorite,new LinearLayout.LayoutParams(0,dp(72),1));
        Button share=ghostButton("⇧\\nPartager"); share.setTextSize(11); share.setOnClickListener(v->shareStation(s)); quick.addView(share,new LinearLayout.LayoutParams(0,dp(72),1));
        root.addView(quick);'''
if quick in s:
    s=s.replace(quick,quick_new,1)

# Les POI McCafe/Tim Hortons sont maintenant installés par patch_uncluster_poi.py
# à partir des images fournies par l'utilisateur. Les anciens remplacements emoji/ATM
# de ce patch sont volontairement ignorés quand la nouvelle fonction est déjà présente.

if 'foodPoiJavascript(centerLat,centerLng)+' not in s:
    s=s.replace('js+radarSafetyJavascript(centerLat,centerLng)+',
                'js+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+',1)

s=s.replace(
    '''+"function radarIcon(hot){return L.divIcon({className:'',html:\\\"<div class='radarCam \\\"+(hot?\\\"radarBlink\\\":\\\"\\\")+\\\"'>📷</div>\\\",iconSize:[38,38],iconAnchor:[19,19]});}"''',
    '''+"function radarIcon(hot){return L.divIcon({className:'',html:\\\"<div class='radarCam \\\"+(hot?\\\"radarBlink\\\":\\\"\\\")+\\\"><img src='file:///android_asset/marker_radar.png'></div>\\\",iconSize:[44,44],iconAnchor:[22,22]});}"'''
)
if '.radarCam img{' not in s:
    s=s.replace('.radarBlink{animation:radarFlash',
                '.radarCam img{width:40px;height:40px;object-fit:contain}.radarBlink{animation:radarFlash',1)

marker='    private View fakeSearchBar(){'
if 'private int stationBannerResource(String stationName)' not in s:
    methods=r'''    private int stationBannerResource(String stationName){
        String n=stationName==null?"":stationName.toLowerCase(Locale.CANADA_FRENCH);
        if(n.contains("irving"))return R.drawable.station_irving;
        if(n.contains("shell"))return R.drawable.station_shell;
        if(n.contains("petro"))return R.drawable.station_petro_canada;
        if(n.contains("esso"))return R.drawable.station_esso;
        if(n.contains("ultramar"))return R.drawable.station_ultramar;
        if(n.contains("canadian tire")||n.contains("gas+"))return R.drawable.station_canadian_tire;
        if(n.contains("costco"))return R.drawable.station_costco;
        if(n.contains("harnois"))return R.drawable.station_harnois;
        return R.drawable.station_generic_pump;
    }

    private void showStationHours(Station s){
        new AlertDialog.Builder(this).setTitle("Horaires • "+s.name)
            .setMessage("Les heures d'ouverture ne sont pas disponibles dans la source actuelle pour cette station. Aucune heure n'est inventée.")
            .setPositiveButton("FERMER",null).show();
    }

    private void showStationServices(Station s){
        new AlertDialog.Builder(this).setTitle("Services • "+s.name)
            .setMessage("Les services détaillés ne sont pas disponibles dans la source actuelle pour cette station. L'application les affichera lorsqu'ils sont fournis par la source.")
            .setPositiveButton("FERMER",null).show();
    }

    private String favoriteKey(Station s){
        return (s.name+"|"+s.lat+"|"+s.lng).toLowerCase(Locale.CANADA_FRENCH);
    }

    private boolean isStationFavorite(Station s){
        return getSharedPreferences("eq_favorites",MODE_PRIVATE).getBoolean(favoriteKey(s),false);
    }

    private void toggleStationFavorite(Station s){
        boolean next=!isStationFavorite(s);
        getSharedPreferences("eq_favorites",MODE_PRIVATE).edit().putBoolean(favoriteKey(s),next).apply();
        Toast.makeText(this,next?"Ajoutée aux favoris":"Retirée des favoris",Toast.LENGTH_SHORT).show();
    }

    private void shareStation(Station s){
        String txt=s.name+"\n"+displayStationAddress(s)+"\n"+String.format(Locale.CANADA_FRENCH,"%.1f ¢/L",s.price)+
            "\nhttps://www.google.com/maps/search/?api=1&query="+Uri.encode(s.lat+","+s.lng);
        Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_SUBJECT,"Essence Québec • "+s.name); i.putExtra(Intent.EXTRA_TEXT,txt);
        startActivity(Intent.createChooser(i,"Partager la station"));
    }

    private void startLiveMapTracking(){
        if(!hasLocationPermission()||stationMap==null)return;
        stopLiveMapTracking();
        liveLocationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
        liveLocationListener=new LocationListener(){
            @Override public void onLocationChanged(Location l){
                if(l==null)return; lastLocation=l;
                final double la=l.getLatitude(),lo=l.getLongitude(); final float br=l.hasBearing()?l.getBearing():0f;
                runOnUiThread(()->{if(stationMap!=null)stationMap.evaluateJavascript("if(window.eqMoveUser){window.eqMoveUser("+la+","+lo+","+br+");}",null);});
            }
            @Override public void onProviderEnabled(String p){}
            @Override public void onProviderDisabled(String p){}
            @SuppressWarnings("deprecation") @Override public void onStatusChanged(String p,int s,Bundle b){}
        };
        try{
            liveLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1500L,3f,liveLocationListener);
            liveLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,3500L,8f,liveLocationListener);
        }catch(Exception ignored){}
    }

    private void stopLiveMapTracking(){
        if(liveLocationManager!=null&&liveLocationListener!=null){try{liveLocationManager.removeUpdates(liveLocationListener);}catch(Exception ignored){}}
        liveLocationListener=null; liveLocationManager=null;
    }

'''
    if marker not in s: raise SystemExit('helper insertion marker missing')
    s=s.replace(marker,methods+marker,1)

# Savings amount: amount + money bundle at ~75% of numeral height.
old_savings='        LinearLayout eco=neonPanel(); eco.setPadding(dp(16),dp(16),dp(16),dp(16)); TextView eh=text("ÉCONOMIE AUJOURD\'HUI",17,true,Color.WHITE); eco.addView(eh); TextView val=text(String.format(Locale.CANADA_FRENCH,"%.2f $",save),42,true,Color.rgb(32,234,140)); eco.addView(val); eco.addView(text("Ce n\'est pas juste de l\'essence, c\'est plus de liberté.",13,false,Color.WHITE)); LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2); ep.topMargin=dp(18); root.addView(eco,ep);'
new_savings='        LinearLayout eco=neonPanel(); eco.setPadding(dp(16),dp(16),dp(16),dp(16)); TextView eh=text("ÉCONOMIE AUJOURD\'HUI",17,true,Color.WHITE); eco.addView(eh); LinearLayout moneyRow=new LinearLayout(this); moneyRow.setGravity(Gravity.CENTER_VERTICAL); TextView val=text(String.format(Locale.CANADA_FRENCH,"%.2f $",save),42,true,Color.rgb(32,234,140)); moneyRow.addView(val,new LinearLayout.LayoutParams(0,-2,1)); ImageView cash=new ImageView(this); cash.setImageResource(R.drawable.money_bundle); cash.setScaleType(ImageView.ScaleType.CENTER_INSIDE); moneyRow.addView(cash,new LinearLayout.LayoutParams(dp(68),dp(46))); eco.addView(moneyRow); eco.addView(text("Ce n\'est pas juste de l\'essence, c\'est plus de liberté.",13,false,Color.WHITE)); LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2); ep.topMargin=dp(18); root.addView(eco,ep);'
if old_savings in s:
    s=s.replace(old_savings,new_savings,1)

required=[
    'stationBannerResource','station_irving','station_shell','station_petro_canada','station_esso',
    'station_ultramar','station_canadian_tire','station_costco','station_harnois','station_generic_pump',
    'showStationHours(s)','showStationServices(s)','toggleStationFavorite(s)','shareStation(s)',
    'eqMoveUser','startLiveMapTracking','marker_mccafe.png','marker_timhortons.webp','money_bundle'
]
missing=[x for x in required if x not in s]
if missing: raise SystemExit('user request patch incomplete: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Essence Quebec user-request patch applied')