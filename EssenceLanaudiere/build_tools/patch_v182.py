from pathlib import Path
import base64,re

root=Path('EssenceLanaudiere')
main=root/'app/src/main'
b64dir=root/'build_tools/eq182_assets_b64'
assets=main/'assets'; draw=main/'res/drawable'
assets.mkdir(parents=True,exist_ok=True); draw.mkdir(parents=True,exist_ok=True)

def decode_file(src,dst):
    raw=''.join((b64dir/src).read_text(encoding='utf-8').split())
    dst.write_bytes(base64.b64decode(raw))

def decode_parts(prefix,count,dst):
    raw=''.join(''.join((b64dir/f'{prefix}_{i:02d}.txt').read_text(encoding='utf-8').split()) for i in range(count))
    dst.write_bytes(base64.b64decode(raw))

decode_file('marker_finance.png.b64',assets/'marker_finance.png')
decode_file('marker_safety.png.b64',assets/'marker_safety.png')
decode_file('marker_user_stationary.png.b64',assets/'marker_user_stationary.png')
decode_parts('moving',4,assets/'marker_user_moving.png')
decode_parts('logo',5,draw/'essence_quebec_logo.png')
mbxml=draw/'money_bundle.xml'
if mbxml.exists(): mbxml.unlink()
decode_parts('money',3,draw/'money_bundle.png')
# aliases for older map code
(assets/'marker_radar.png').write_bytes((assets/'marker_safety.png').read_bytes())
(assets/'marker_atm.png').write_bytes((assets/'marker_finance.png').read_bytes())

p=main/'java/quebec/lanaudiere/essence/MainActivity.java'
s=p.read_text(encoding='utf-8')
s=s.replace("file:///android_asset/marker_radar.png","file:///android_asset/marker_safety.png")
s=s.replace("file:///android_asset/marker_atm.png","file:///android_asset/marker_finance.png")
s=s.replace('nwr(around:60000,"+userLat+","+userLng+")[amenity=\\\"atm\\\"];','nwr(around:50000,"+userLat+","+userLng+")[amenity=\\\"atm\\\"];')
if '[amenity=\\\"bank\\\"]' not in s:
    s=s.replace('"nwr(around:50000,"+userLat+","+userLng+")[amenity=\\\"atm\\\"];"+', '"nwr(around:50000,"+userLat+","+userLng+")[amenity=\\\"atm\\\"];"+\n            "nwr(around:50000,"+userLat+","+userLng+")[amenity=\\\"bank\\\"];"+',1)
s=s.replace("const kind=(t.amenity==='atm')?'atm':","const kind=(t.amenity==='atm'||t.amenity==='bank')?'atm':")
s=s.replace("const label=kind==='atm'?(name||'Guichet automatique'):name;","const label=kind==='atm'?(name||(t.amenity==='bank'?'Institution financière':'Guichet automatique')):name;")
# add distance to finance popup when possible
s=s.replace("L.marker([la,lo],{icon:poiIcon(kind)}).addTo(foodLayer).bindPopup('<b>'+escPoi(label)+'</b>'+(addr?'<br>'+escPoi(addr):''));", "const dist=Math.round(window.mtqDistance?window.mtqDistance(userLat,userLng,la,lo):0);L.marker([la,lo],{icon:poiIcon(kind)}).addTo(foodLayer).bindPopup('<b>'+escPoi(label)+'</b>'+(addr?'<br>'+escPoi(addr):'')+(kind==='atm'&&dist?'<br>'+((dist/1000).toFixed(1))+' km':''));")
# replace user marker updater with speed-aware images
pat=re.compile(r"window\.eqMoveUser=\(la,lo,heading(?:,speed)?\)=>\{.*?\};",re.S)
new=("window.eqMoveUser=(la,lo,heading,speed)=>{const ll=[Number(la),Number(lo)],sp=Number(speed||0),moving=sp>1.2;window.eqLastUser=ll;"
     "const src=moving?'file:///android_asset/marker_user_moving.png':'file:///android_asset/marker_user_stationary.png';"
     "const ico=L.divIcon({className:'',html:\"<div class='eqUserPin'><img src='\"+src+\"'></div>\",iconSize:moving?[58,58]:[48,64],iconAnchor:moving?[29,29]:[24,58]});"
     "if(window.eqUserDot&&window.eqUserDot.remove)window.eqUserDot.remove();if(window.eqUserLabel&&window.eqUserLabel.remove){window.eqUserLabel.remove();window.eqUserLabel=null;}"
     "window.eqUserDot=L.marker(ll,{icon:ico,zIndexOffset:5000}).addTo(map).bindPopup(moving?'<b>EN MOUVEMENT</b>':'<b>VOUS ÊTES ICI</b>');if(eqFollowUser)map.panTo(ll,{animate:true,duration:.45});};")
s,n=pat.subn(new,s,count=1)
if n==0: raise SystemExit('eqMoveUser introuvable')
if '.eqUserPin img{' not in s:
    s=s.replace('</style></head><body>',".eqUserPin{background:transparent!important;border:0!important}.eqUserPin img{width:100%;height:100%;object-fit:contain;filter:drop-shadow(0 4px 7px #0009)}</style></head><body>",1)
if 'final float sp=l.hasSpeed()?l.getSpeed():0f;' not in s:
    s=s.replace('final double la=l.getLatitude(),lo=l.getLongitude(); final float br=l.hasBearing()?l.getBearing():0f;','final double la=l.getLatitude(),lo=l.getLongitude(); final float br=l.hasBearing()?l.getBearing():0f; final float sp=l.hasSpeed()?l.getSpeed():0f;')
s=s.replace('window.eqMoveUser("+la+","+lo+","+br+");','window.eqMoveUser("+la+","+lo+","+br+","+sp+");')
# money bundle on station price row: real PNG, roughly same visual height as digits
s=s.replace('stationMoney.setImageResource(R.drawable.money_bundle);','stationMoney.setImageResource(R.drawable.money_bundle); stationMoney.setBackgroundColor(Color.TRANSPARENT);')
# safety icon also used for MTQ and radar layers
s=s.replace("📹 QUÉBEC 511","QUÉBEC 511")
if 'ESSENCE_QUEBEC_182' not in s:
    anchor='private static final String ESSENCE_QUEBEC_181="1.8.1";'
    s=s.replace(anchor,anchor+'\n    private static final String ESSENCE_QUEBEC_182="1.8.2";',1)
p.write_text(s,encoding='utf-8')

g=root/'app/build.gradle'; gs=g.read_text(encoding='utf-8')
gs=re.sub(r'versionCode\s+\d+','versionCode 31',gs,count=1)
gs=re.sub(r"versionName\s+'[^']+'","versionName '1.8.2'",gs,count=1)
g.write_text(gs,encoding='utf-8')

for f in [assets/'marker_user_stationary.png',assets/'marker_user_moving.png',assets/'marker_safety.png',assets/'marker_finance.png',draw/'money_bundle.png',draw/'essence_quebec_logo.png']:
    if not f.exists() or f.stat().st_size<500: raise SystemExit('ressource invalide: '+str(f))
required=['ESSENCE_QUEBEC_182','marker_user_stationary.png','marker_user_moving.png','marker_safety.png','marker_finance.png','around:50000','amenity=\\\"bank\\\"']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('patch_v182 incomplet: '+repr(missing))
print('Essence Quebec 1.8.2 patch applied')
