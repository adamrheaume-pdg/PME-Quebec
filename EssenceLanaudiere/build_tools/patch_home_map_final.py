from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

old="window.eqMoveUser=(la,lo,heading)=>{const ll=[Number(la),Number(lo)];if(window.eqUserDot)window.eqUserDot.setLatLng(ll);if(window.eqUserLabel)window.eqUserLabel.setLatLng(ll);if(eqFollowUser)map.panTo(ll,{animate:true,duration:.45});};"
new="window.eqMoveUser=(la,lo,heading)=>{const ll=[Number(la),Number(lo)];window.eqLastUser=ll;if(!window.eqUserDot){window.eqUserDot=L.circleMarker(ll,{radius:10,color:'#fff',weight:3,fillColor:'#1677ff',fillOpacity:1}).addTo(map).bindPopup('<b>VOUS ÊTES ICI</b>');}else{window.eqUserDot.setLatLng(ll);}if(!window.eqUserLabel){window.eqUserLabel=L.marker(ll,{icon:L.divIcon({className:'',html:'<div class=you>VOUS</div>',iconSize:[54,22],iconAnchor:[27,-12]})}).addTo(map);}else{window.eqUserLabel.setLatLng(ll);}if(eqFollowUser)map.panTo(ll,{animate:true,duration:.45});};"
if old in s:
    s=s.replace(old,new,1)

old2='''document.getElementById('meBtn').onclick=()=>{eqFollowUser=true;map.setView(["+centerLat+","+centerLng+"],15,{animate:true});};'''
new2='''document.getElementById('meBtn').onclick=()=>{eqFollowUser=true;const ll=window.eqLastUser||["+centerLat+","+centerLng+"];map.setView(ll,15,{animate:true});};'''
if old2 in s:
    s=s.replace(old2,new2,1)

old3='''    private void renderStationMap(List<Station> stations){
        if(stationMap==null)return;
        stationMap.loadDataWithBaseURL("https://carto.com/",buildMapHtml(stations),"text/html","UTF-8",null);
    }'''
new3='''    private void renderStationMap(List<Station> stations){
        if(stationMap==null)return;
        stationMap.setWebViewClient(new android.webkit.WebViewClient(){
            @Override public void onPageFinished(android.webkit.WebView view,String url){
                super.onPageFinished(view,url);
                if(lastLocation!=null){
                    double la=lastLocation.getLatitude(),lo=lastLocation.getLongitude();
                    float br=lastLocation.hasBearing()?lastLocation.getBearing():0f;
                    view.evaluateJavascript("if(window.eqMoveUser){window.eqMoveUser("+la+","+lo+","+br+");}",null);
                }
                startLiveMapTracking();
            }
        });
        stationMap.loadDataWithBaseURL("https://carto.com/",buildMapHtml(stations),"text/html","UTF-8",null);
    }'''
if old3 in s:
    s=s.replace(old3,new3,1)

p.write_text(s,encoding='utf-8')
print('Carte accueil: navigation dynamique, suivi GPS et marqueur VOUS assures')
