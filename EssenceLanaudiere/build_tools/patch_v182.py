from pathlib import Path
import shutil, zipfile, re

root=Path('EssenceLanaudiere')
main=root/'app/src/main'
assetzip=root/'build_tools/eq182_assets.zip'
if not assetzip.exists():
    raise SystemExit('eq182_assets.zip absent')
with zipfile.ZipFile(assetzip) as z:
    z.extractall('/tmp/eq182_assets')
src=Path('/tmp/eq182_assets')
(main/'assets').mkdir(parents=True, exist_ok=True)
(main/'res/drawable').mkdir(parents=True, exist_ok=True)
shutil.copy2(src/'marker_user_stationary.png', main/'assets/marker_user_stationary.png')
shutil.copy2(src/'marker_user_moving.png', main/'assets/marker_user_moving.png')
shutil.copy2(src/'marker_safety.png', main/'assets/marker_safety.png')
shutil.copy2(src/'marker_finance.png', main/'assets/marker_finance.png')
shutil.copy2(src/'marker_safety.png', main/'assets/marker_radar.png')
shutil.copy2(src/'marker_finance.png', main/'assets/marker_atm.png')
shutil.copy2(src/'essence_quebec_logo_v2.png', main/'res/drawable/essence_quebec_logo.png')
shutil.copy2(src/'money_bundle_real.png', main/'res/drawable/money_bundle.png')

p=main/'java/quebec/lanaudiere/essence/MainActivity.java'
s=p.read_text(encoding='utf-8')
s=s.replace("file:///android_asset/marker_radar.png", "file:///android_asset/marker_safety.png")
s=s.replace("file:///android_asset/marker_atm.png", "file:///android_asset/marker_finance.png")
s=s.replace('nwr(around:60000,"+userLat+","+userLng+")[amenity=\\\"atm\\\"];',
            'nwr(around:50000,"+userLat+","+userLng+")[amenity=\\\"atm\\\"];')
if '[amenity=\\\"bank\\\"]' not in s:
    s=s.replace('"nwr(around:50000,"+userLat+","+userLng+")[amenity=\\\"atm\\\"];"+',
                '"nwr(around:50000,"+userLat+","+userLng+")[amenity=\\\"atm\\\"];"+\n            "nwr(around:50000,"+userLat+","+userLng+")[amenity=\\\"bank\\\"];"+',1)
s=s.replace("const kind=(t.amenity==='atm')?'atm':", "const kind=(t.amenity==='atm'||t.amenity==='bank')?'atm':")
s=s.replace("const label=kind==='atm'?(name||'Guichet automatique'):name;",
            "const label=kind==='atm'?(name||(t.amenity==='bank'?'Institution financière':'Guichet automatique')):name;")
old_re=re.compile(r"window\.eqMoveUser=\(la,lo,heading\)=>\{.*?\};", re.S)
new=("window.eqMoveUser=(la,lo,heading,speed)=>{"
     "const ll=[Number(la),Number(lo)],sp=Number(speed||0),moving=sp>1.2;window.eqLastUser=ll;"
     "const src=moving?'file:///android_asset/marker_user_moving.png':'file:///android_asset/marker_user_stationary.png';"
     "const ico=L.divIcon({className:'',html:\"<div class='eqUserPin'><img src='\"+src+\"'></div>\",iconSize:moving?[58,58]:[48,64],iconAnchor:moving?[29,29]:[24,58]});"
     "if(window.eqUserDot&&window.eqUserDot.remove){window.eqUserDot.remove();}"
     "if(window.eqUserLabel&&window.eqUserLabel.remove){window.eqUserLabel.remove();window.eqUserLabel=null;}"
     "window.eqUserDot=L.marker(ll,{icon:ico,zIndexOffset:5000}).addTo(map).bindPopup(moving?'<b>EN MOUVEMENT</b>':'<b>VOUS ÊTES ICI</b>');"
     "if(eqFollowUser)map.panTo(ll,{animate:true,duration:.45});};")
if 'marker_user_stationary.png' not in s:
    s,n=old_re.subn(new,s,count=1)
    if n==0: raise SystemExit('eqMoveUser introuvable')
if '.eqUserPin img{' not in s:
    s=s.replace('</style></head><body>', ".eqUserPin{background:transparent!important;border:0!important}.eqUserPin img{width:100%;height:100%;object-fit:contain;filter:drop-shadow(0 4px 7px #0009)}</style></head><body>",1)
if 'final float sp=l.hasSpeed()?l.getSpeed():0f;' not in s:
    s=s.replace('final double la=l.getLatitude(),lo=l.getLongitude(); final float br=l.hasBearing()?l.getBearing():0f;',
                'final double la=l.getLatitude(),lo=l.getLongitude(); final float br=l.hasBearing()?l.getBearing():0f; final float sp=l.hasSpeed()?l.getSpeed():0f;')
s=s.replace('window.eqMoveUser("+la+","+lo+","+br+");', 'window.eqMoveUser("+la+","+lo+","+br+","+sp+");')
s=s.replace('window.eqMoveUser("+la+","+lo+","+br+");}', 'window.eqMoveUser("+la+","+lo+","+br+",0);}')
if 'ESSENCE_QUEBEC_182' not in s:
    anchor='private static final String ESSENCE_QUEBEC_181="1.8.1";'
    if anchor in s:
        s=s.replace(anchor, anchor+'\n    private static final String ESSENCE_QUEBEC_182="1.8.2";',1)
    else:
        s=s.replace('private static final String CHANNEL_ID="best_price";', 'private static final String CHANNEL_ID="best_price";\n    private static final String ESSENCE_QUEBEC_182="1.8.2";',1)
p.write_text(s,encoding='utf-8')
g=root/'app/build.gradle'
gs=g.read_text(encoding='utf-8')
gs=re.sub(r"versionCode\s+\d+", "versionCode 31", gs, count=1)
gs=re.sub(r"versionName\s+'[^']+'", "versionName '1.8.2'", gs, count=1)
g.write_text(gs,encoding='utf-8')
required=['ESSENCE_QUEBEC_182','marker_user_stationary.png','marker_user_moving.png','marker_safety.png','marker_finance.png','amenity=\\\"bank\\\"','around:50000']
missing=[x for x in required if x not in s and x not in gs]
if missing: raise SystemExit('patch_v182 incomplet: '+repr(missing))
print('Essence Quebec 1.8.2 patch applied')
