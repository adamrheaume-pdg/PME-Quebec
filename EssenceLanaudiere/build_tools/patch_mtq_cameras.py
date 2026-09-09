from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Ce patch ajoute une couche indépendante des stations: source officielle MTMD WFS,
# marqueurs caméra et carte PIP à <=100 m. Il ne modifie pas la logique essence.
if 'MTQ_CAMERA_WFS' not in s:
    anchor='    private static final String CHANNEL_ID="best_price";\n'
    add='''    private static final String MTQ_CAMERA_WFS="https://ws.mapserver.transports.gouv.qc.ca/swtq?service=wfs&version=2.0.0&request=getfeature&typename=ms:infos_cameras&outfile=Camera&srsname=EPSG:4326&outputformat=geojson";\n    private static final float MTQ_CAMERA_PIP_METERS=100f;\n'''
    if anchor not in s: raise SystemExit('channel anchor missing')
    s=s.replace(anchor,anchor+add,1)

# Hooks are deliberately lightweight: map patch can call this JS bridge after location updates.
if 'mtqCameraPipHtml' not in s:
    anchor='    private void addTools(){\n'
    methods=r'''    private String mtqCameraPipHtml(String title,String imageUrl){
        String safeTitle=title==null?"Caméra Québec 511":title.replace("<","&lt;").replace(">","&gt;");
        String safeUrl=imageUrl==null?"":imageUrl.replace("'","%27");
        return "<div id='mtqPip' style='position:absolute;right:12px;bottom:14px;width:46%;max-width:280px;z-index:9999;background:rgba(8,22,45,.96);border:2px solid #55b9ff;border-radius:18px;overflow:hidden;box-shadow:0 10px 28px rgba(0,0,0,.38);font-family:sans-serif'>"
            +"<div style='padding:8px 10px;color:white;font-weight:800;font-size:12px'>📹 QUÉBEC 511 <span style='float:right;color:#72e7a2'>● À PROXIMITÉ</span></div>"
            +"<img src='"+safeUrl+"' style='display:block;width:100%;aspect-ratio:16/9;object-fit:cover;background:#13243a'/>"
            +"<div style='padding:7px 10px;color:#dcecff;font-size:11px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis'>"+safeTitle+" • ≤ 100 m</div></div>";
    }

    private String mtqCameraJavascript(){
        return "window.mtqCameraLayer=window.mtqCameraLayer||L.layerGroup().addTo(map);"
            +"window.mtqCameraMarkers=window.mtqCameraMarkers||[];"
            +"window.mtqDistance=function(a,b,c,d){var R=6371000,p=Math.PI/180,x=(c-a)*p,y=(d-b)*p;var q=Math.sin(x/2)**2+Math.cos(a*p)*Math.cos(c*p)*Math.sin(y/2)**2;return 2*R*Math.asin(Math.sqrt(q));};"
            +"window.mtqShowPip=function(title,url){var old=document.getElementById('mtqPip');if(old)old.remove();document.body.insertAdjacentHTML('beforeend',Android.mtqPip(title,url));};"
            +"window.mtqHidePip=function(){var e=document.getElementById('mtqPip');if(e)e.remove();};";
    }

'''
    if anchor not in s: raise SystemExit('method anchor missing')
    s=s.replace(anchor,methods+anchor,1)

p.write_text(s,encoding='utf-8')
print('MTQ camera PIP patch installed; independent from gas-station logic')
