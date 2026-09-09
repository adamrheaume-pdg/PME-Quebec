from pathlib import Path

p = Path("EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java")
s = p.read_text(encoding="utf-8")

s = s.replace('radiusSpinner=spinner(new String[]{"5 km","10 km","20 km","30 km"}); radiusSpinner.setSelection(3);',
              'radiusSpinner=spinner(new String[]{"5 km","10 km","20 km","30 km","60 km"}); radiusSpinner.setSelection(3);')
s = s.replace('int radius=new int[]{5,10,20,30}[radiusSpinner.getSelectedItemPosition()];',
              'int radius=new int[]{5,10,20,30,60}[radiusSpinner.getSelectedItemPosition()];')

if 'import android.webkit.*;' not in s:
    s = s.replace('import android.widget.*;\n', 'import android.widget.*;\nimport android.webkit.*;\n')
if 'import android.graphics.Bitmap;' not in s:
    s = s.replace('import android.graphics.Typeface;\n', 'import android.graphics.Typeface;\nimport android.graphics.Bitmap;\nimport android.graphics.BitmapFactory;\n')
if 'private WebView stationMap;' not in s:
    s = s.replace('private LinearLayout page, results;\n', 'private LinearLayout page, results;\n    private WebView stationMap;\n')

intro = 'LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setPadding(dp(26),dp(26),dp(26),dp(28)); sc.addView(root);'
if 'applySafeInsets(root,26,26,26,28);' not in s:
    s = s.replace(intro, intro + '\n        applySafeInsets(root,26,26,26,28);', 1)

main = 'page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(18),dp(18),dp(18),dp(26)); page.setBackgroundColor(navy); sc.addView(page);'
if 'applySafeInsets(page,18,18,18,26);' not in s:
    s = s.replace(main, main + '\n        applySafeInsets(page,18,18,18,26);', 1)

anchor = 'status=text("Prix récents • données ouvertes • vérifie le prix à la pompe",12,false,Color.rgb(220,228,238)); status.setPadding(0,dp(8),0,dp(10)); page.addView(status);\n        results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); page.addView(results);'
if 'CARTE DES STATIONS • ● VOUS' not in s:
    repl = '''status=text("Prix récents • données ouvertes • vérifie le prix à la pompe",12,false,Color.rgb(220,228,238)); status.setPadding(0,dp(8),0,dp(10)); page.addView(status);
        LinearLayout mapCard=cardWhite(); mapCard.setPadding(dp(6),dp(6),dp(6),dp(6));
        TextView mapTitle=text("CARTE DES STATIONS • ● VOUS",15,true,ink); mapTitle.setPadding(dp(8),dp(6),dp(8),dp(8)); mapCard.addView(mapTitle);
        stationMap=new WebView(this); stationMap.setBackgroundColor(Color.rgb(232,238,245)); stationMap.getSettings().setJavaScriptEnabled(true); stationMap.getSettings().setDomStorageEnabled(true); stationMap.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        stationMap.loadDataWithBaseURL("https://carto.com/","<html><body style='font-family:sans-serif;background:#e8eef5;color:#2d3443;display:flex;align-items:center;justify-content:center;height:100%;margin:0'><b>La carte apparaîtra après la localisation.</b></body></html>","text/html","UTF-8",null);
        mapCard.addView(stationMap,new LinearLayout.LayoutParams(-1,dp(330)));
        LinearLayout.LayoutParams mcp=new LinearLayout.LayoutParams(-1,-2); mcp.bottomMargin=dp(10); page.addView(mapCard,mcp);
        results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); page.addView(results);'''
    if anchor not in s:
        raise SystemExit("Point insertion carte introuvable")
    s = s.replace(anchor, repl, 1)

s = s.replace('map.setOnClickListener(v->{if(!lastStations.isEmpty()) openMap(lastStations.get(0)); else locateAndLoad();});',
              'map.setOnClickListener(v->{if(!lastStations.isEmpty()) showMapDialog(); else locateAndLoad();});')
s = s.replace('results.removeAllViews(); lastStations=new ArrayList<>(s); status.setText("Source : "+source+" • prix récents • touche une carte pour l’itinéraire.");',
              'results.removeAllViews(); lastStations=new ArrayList<>(s); renderStationMap(s); status.setText("Source : "+source+" • prix récents • touche une carte pour l’itinéraire.");')

old_body = 'LinearLayout body=new LinearLayout(this); body.setGravity(Gravity.CENTER_VERTICAL); TextView addr=text(x.address+"\\n"+x.city,14,false,ink); body.addView(addr,new LinearLayout.LayoutParams(0,-2,1));'
if 'loadBrandLogo(brand,x.name);' not in s:
    new_body = 'LinearLayout body=new LinearLayout(this); body.setGravity(Gravity.CENTER_VERTICAL); ImageView brand=new ImageView(this); brand.setScaleType(ImageView.ScaleType.CENTER_INSIDE); brand.setBackground(round(Color.WHITE,8)); body.addView(brand,new LinearLayout.LayoutParams(dp(62),dp(62))); loadBrandLogo(brand,x.name); TextView addr=text(x.address+"\\n"+x.city,14,false,ink); LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,-2,1); ap.leftMargin=dp(10); body.addView(addr,ap);'
    if old_body not in s:
        raise SystemExit("Point insertion logos introuvable")
    s = s.replace(old_body, new_body, 1)

methods = r'''
    private void renderStationMap(List<Station> stations){
        if(stationMap==null)return;
        stationMap.loadDataWithBaseURL("https://carto.com/",buildMapHtml(stations),"text/html","UTF-8",null);
    }

    private String buildMapHtml(List<Station> stations){
        double centerLat=lastLocation!=null?lastLocation.getLatitude():(stations.isEmpty()?46.0:stations.get(0).lat);
        double centerLng=lastLocation!=null?lastLocation.getLongitude():(stations.isEmpty()?-73.5:stations.get(0).lng);
        StringBuilder js=new StringBuilder("const bounds=[];const cluster=L.markerClusterGroup({showCoverageOnHover:false,maxClusterRadius:46,spiderfyOnMaxZoom:true});");
        if(lastLocation!=null){
            double la=lastLocation.getLatitude(),lo=lastLocation.getLongitude();
            js.append("L.circleMarker([").append(la).append(',').append(lo).append("],{radius:10,color:'#fff',weight:3,fillColor:'#1677ff',fillOpacity:1}).addTo(map).bindPopup('<b>VOUS ÊTES ICI</b>');");
            js.append("L.marker([").append(la).append(',').append(lo).append("],{icon:L.divIcon({className:'',html:\"<div class='you'>VOUS</div>\",iconSize:[54,22],iconAnchor:[27,-12]})}).addTo(map);");
            js.append("bounds.push([").append(la).append(',').append(lo).append("]);");
        }
        for(Station x:stations){
            String price=String.format(Locale.CANADA_FRENCH,"%.1f",x.price);
            String icon="<div class='pin'><img src='"+brandLogoUrl(x.name)+"' onerror=\"this.style.display='none'\"><span>"+price+"</span></div>";
            String pop="<b>"+html(x.name)+"</b><br>"+html(x.address)+"<br>"+price+" ¢/L • "+String.format(Locale.CANADA_FRENCH,"%.1f",x.distance)+" km";
            js.append("cluster.addLayer(L.marker([").append(x.lat).append(',').append(x.lng).append("],{icon:L.divIcon({className:'',html:").append(JSONObject.quote(icon)).append(",iconSize:[70,48],iconAnchor:[35,48]})}).bindPopup(").append(JSONObject.quote(pop)).append("));");
            js.append("bounds.push([").append(x.lat).append(',').append(x.lng).append("]);");
        }
        js.append("map.addLayer(cluster);if(bounds.length>1)map.fitBounds(bounds,{padding:[35,35],maxZoom:14});");
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"+
          "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"+
          "<link rel='stylesheet' href='https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.css'>"+
          "<link rel='stylesheet' href='https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.Default.css'>"+
          "<style>html,body,#map{width:100%;height:100%;margin:0}.pin{min-width:64px;height:36px;background:#fff;border:2px solid #31405a;border-radius:10px;padding:3px 5px;display:flex;align-items:center;gap:4px;box-shadow:0 2px 6px #0005}.pin img{width:26px;height:26px;object-fit:contain}.pin span{font:bold 13px sans-serif;color:#283348;white-space:nowrap}.you{background:#1677ff;color:#fff;font:bold 12px sans-serif;border-radius:10px;padding:3px 8px;text-align:center;box-shadow:0 1px 4px #0005}.marker-cluster-small,.marker-cluster-medium,.marker-cluster-large{background:#1677ff55}.marker-cluster div{background:#1677ff;color:#fff;font:bold 13px sans-serif}</style></head><body><div id='map'></div>"+
          "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"+
          "<script src='https://unpkg.com/leaflet.markercluster@1.5.3/dist/leaflet.markercluster.js'></script>"+
          "<script>const map=L.map('map',{zoomControl:true}).setView(["+centerLat+","+centerLng+"],12);L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{subdomains:'abcd',maxZoom:20,attribution:'© OpenStreetMap contributors © CARTO'}).addTo(map);"+js+"</script></body></html>";
    }

    private void showMapDialog(){
        if(lastStations.isEmpty()){locateAndLoad();return;}
        WebView w=new WebView(this); w.getSettings().setJavaScriptEnabled(true); w.getSettings().setDomStorageEnabled(true); w.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        w.loadDataWithBaseURL("https://carto.com/",buildMapHtml(lastStations),"text/html","UTF-8",null);
        LinearLayout box=new LinearLayout(this); box.setPadding(dp(8),dp(8),dp(8),dp(8)); box.addView(w,new LinearLayout.LayoutParams(-1,dp(520)));
        new AlertDialog.Builder(this).setTitle("Carte des stations • ● Vous").setView(box).setPositiveButton("FERMER",null).show();
    }

    private String brandDomain(String name){
        String n=name==null?"":name.toLowerCase(Locale.CANADA_FRENCH);
        if(n.contains("petro"))return "petro-canada.ca";
        if(n.contains("ultramar"))return "ultramar.ca";
        if(n.contains("shell"))return "shell.ca";
        if(n.contains("esso"))return "esso.ca";
        if(n.contains("costco"))return "costco.ca";
        if(n.contains("irving"))return "irvingoil.com";
        if(n.contains("couche-tard")||n.contains("coupe-tard"))return "couche-tard.com";
        if(n.contains("circle k"))return "circlek.com";
        if(n.contains("harnois"))return "harnoisenergies.com";
        if(n.contains("sonic"))return "energiesonic.com";
        if(n.contains("canadian tire"))return "canadiantire.ca";
        return "gasquebec.ca";
    }

    private String brandLogoUrl(String name){return "https://www.google.com/s2/favicons?sz=128&domain="+Uri.encode(brandDomain(name));}
    private void loadBrandLogo(ImageView view,String name){
        view.setContentDescription(name);
        new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(brandLogoUrl(name)).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);Bitmap b=BitmapFactory.decodeStream(c.getInputStream());if(b!=null)runOnUiThread(()->view.setImageBitmap(b));}catch(Exception ignored){}}).start();
    }
    private String html(String x){return x==null?"":x.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}

    private void applySafeInsets(View v,int leftDp,int topDp,int rightDp,int bottomDp){
        v.setOnApplyWindowInsetsListener((view,insets)->{
            int l,t,r,b;
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets z=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());l=z.left;t=z.top;r=z.right;b=z.bottom;}
            else {l=insets.getSystemWindowInsetLeft();t=insets.getSystemWindowInsetTop();r=insets.getSystemWindowInsetRight();b=insets.getSystemWindowInsetBottom();}
            view.setPadding(dp(leftDp)+l,dp(topDp)+t,dp(rightDp)+r,dp(bottomDp)+b);
            return insets;
        });
        v.requestApplyInsets();
    }

'''
if 'private void renderStationMap(List<Station> stations)' not in s:
    marker = '    private void showTrendDialog(){'
    if marker not in s:
        raise SystemExit("Point insertion méthodes introuvable")
    s = s.replace(marker, methods + marker, 1)

required = ['60 km','5,10,20,30,60','CARTE DES STATIONS • ● VOUS','renderStationMap(s)','VOUS ÊTES ICI','loadBrandLogo(brand,x.name)','applySafeInsets(page,18,18,18,26)','basemaps.cartocdn.com','markerClusterGroup']
missing = [x for x in required if x not in s]
if missing:
    raise SystemExit("Fonctions manquantes: " + repr(missing))
p.write_text(s, encoding="utf-8")
