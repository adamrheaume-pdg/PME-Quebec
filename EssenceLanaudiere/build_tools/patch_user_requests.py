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

s=s.replace(
    '''            "nwr(around:60000,"+userLat+","+userLng+")[name=\\\"Tim Hortons\\\"];"+
            ");out center tags;";''',
    '''            "nwr(around:60000,"+userLat+","+userLng+")[name=\\\"Tim Hortons\\\"];"+
            "nwr(around:60000,"+userLat+","+userLng+")[amenity=\\\"atm\\\"];"+
            ");out center tags;";'''
)

old_poi='''            +"function poiIcon(kind){const emo=kind==='mcd'?'🍔':'☕';const cls=kind==='mcd'?'mcdPoi':'timPoi';return L.divIcon({className:'',html:\\\"<div class='foodPoi \\\"+cls+\\\"'>\\\"+emo+\\\"</div>\\\",iconSize:[38,38],iconAnchor:[19,19]});}"
            +"function escPoi(x){return String(x||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}"
            +"fetch(poiEndpoint,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:'data='+encodeURIComponent(poiQuery)}).then(r=>r.json()).then(d=>{const seen=new Set();for(const e of (d.elements||[])){const t=e.tags||{},name=String(t.name||t.brand||'');const low=name.toLowerCase();const kind=low.includes(\\\"mcdonald\\\")?'mcd':(low.includes('tim hortons')?'tim':null);if(!kind)continue;const la=Number(e.lat||(e.center&&e.center.lat)),lo=Number(e.lon||(e.center&&e.center.lon));if(!isFinite(la)||!isFinite(lo))continue;const key=kind+':'+la.toFixed(5)+':'+lo.toFixed(5);if(seen.has(key))continue;seen.add(key);const addr=[t['addr:housenumber'],t['addr:street'],t['addr:city']].filter(Boolean).join(' ');L.marker([la,lo],{icon:poiIcon(kind)}).addTo(foodLayer).bindPopup('<b>'+escPoi(name)+'</b>'+(addr?'<br>'+escPoi(addr):''));}}).catch(()=>{});";'''
new_poi='''            +"function poiIcon(kind){const src=kind==='mcd'?'file:///android_asset/marker_mccafe.png':(kind==='tim'?'file:///android_asset/marker_tim.png':'file:///android_asset/marker_atm.png');const cls=kind==='mcd'?'mcdPoi':(kind==='tim'?'timPoi':'atmPoi');return L.divIcon({className:'',html:\\\"<div class='foodPoi \\\"+cls+\\\"'><img src='\\\"+src+\\\"'></div>\\\",iconSize:[46,46],iconAnchor:[23,23]});}"
            +"function escPoi(x){return String(x||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}"
            +"fetch(poiEndpoint,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:'data='+encodeURIComponent(poiQuery)}).then(r=>r.json()).then(d=>{const seen=new Set();for(const e of (d.elements||[])){const t=e.tags||{},name=String(t.name||t.brand||'');const low=name.toLowerCase();const kind=(t.amenity==='atm')?'atm':(low.includes(\\\"mcdonald\\\")?'mcd':(low.includes('tim hortons')?'tim':null));if(!kind)continue;const la=Number(e.lat||(e.center&&e.center.lat)),lo=Number(e.lon||(e.center&&e.center.lon));if(!isFinite(la)||!isFinite(lo))continue;const key=kind+':'+la.toFixed(5)+':'+lo.toFixed(5);if(seen.has(key))continue;seen.add(key);const addr=[t['addr:housenumber'],t['addr:street'],t['addr:city']].filter(Boolean).join(' ');const label=kind==='atm'?(name||'Guichet automatique'):name;L.marker([la,lo],{icon:poiIcon(kind)}).addTo(foodLayer).bindPopup('<b>'+escPoi(label)+'</b>'+(addr?'<br>'+escPoi(addr):''));}}).catch(()=>{});";'''
if old_poi in s:
    s=s.replace(old_poi,new_poi,1)

if 'foodPoiJavascript(centerLat,centerLng)+' not in s:
    s=s.replace('js+radarSafetyJavascript(centerLat,centerLng)+',
                'js+radarSafetyJavascript(centerLat,centerLng)+foodPoiJavascript(centerLat,centerLng)+',1)

s=s.replace(
    ".foodPoi{width:34px;height:34px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:21px;border:2px solid #fff;box-shadow:0 3px 10px #0007}.mcdPoi{background:#efc200}.timPoi{background:#8b2c1d}",
    ".foodPoi{width:44px;height:44px;border-radius:14px;display:flex;align-items:center;justify-content:center;border:1px solid #ffffff66;background:#07101dcc;box-shadow:0 4px 14px #000a}.foodPoi img{width:42px;height:42px;object-fit:contain}.mcdPoi,.timPoi,.atmPoi{background:#07101dcc}"
)

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
            @Override public void onStatusChanged(String p,int st,Bundle b){}
        };
        try{
            if(liveLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))liveLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,1f,liveLocationListener,Looper.getMainLooper());
            if(liveLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))liveLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2000L,3f,liveLocationListener,Looper.getMainLooper());
        }catch(SecurityException ignored){}
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
    'eqMoveUser','startLiveMapTracking','marker_mccafe.png','marker_tim.png','marker_atm.png','marker_radar.png','money_bundle'
]
missing=[x for x in required if x not in s]
if missing: raise SystemExit('user request patch incomplete: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('Essence Quebec user-request patch applied')
