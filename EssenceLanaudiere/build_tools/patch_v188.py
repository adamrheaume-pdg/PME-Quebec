from pathlib import Path
import base64,re

root=Path('EssenceLanaudiere')
main=root/'app/src/main'
assets=main/'assets'
assets.mkdir(parents=True,exist_ok=True)
b64dir=root/'build_tools/eq188_assets_b64'

# Images fournies par l'utilisateur, avec transparence.
(assets/'marker_mccafe.png').write_bytes(base64.b64decode((b64dir/'mccafe_00.txt').read_text(encoding='utf-8').strip()))
(assets/'marker_timhortons.webp').write_bytes(base64.b64decode((b64dir/'timhortons.webp.b64').read_text(encoding='utf-8').strip()))

p=main/'java/quebec/lanaudiere/essence/MainActivity.java'
s=p.read_text(encoding='utf-8')

# patch_v183 reconstruit foodPoiJavascript après le patch POI initial.
# Remplacer explicitement son ancien fichier Tim Hortons et imposer la taille fixe finale.
s=s.replace("file:///android_asset/marker_tim.png","file:///android_asset/marker_timhortons.webp")

start=s.find('    private String foodPoiJavascript(double userLat,double userLng){')
end=s.find('\n    private String mtqCameraJavascript',start)
if start < 0 or end < 0:
    raise SystemExit('foodPoiJavascript final introuvable')
seg=s[start:end]
if 'marker_mccafe.png' not in seg or 'marker_timhortons.webp' not in seg or 'marker_finance.png' not in seg:
    raise SystemExit('ressources POI finales incompletes')

# McCafe et Tim Hortons: 40x40 fixes. Finance conserve sa propre branche.
seg=seg.replace("iconSize:kind==='atm'?[50,50]:[46,46],iconAnchor:kind==='atm'?[25,25]:[23,23]",
                "iconSize:kind==='atm'?[50,50]:[40,40],iconAnchor:kind==='atm'?[25,25]:[20,20]")
s=s[:start]+seg+s[end:]

# Surcharge CSS finale : aucune variation de taille au zoom.
css=".foodPoi{width:40px!important;height:40px!important;border-radius:10px!important;display:flex!important;align-items:center!important;justify-content:center!important;border:1px solid #ffffff99!important;box-shadow:0 3px 10px #0008!important;background:#101522!important;overflow:hidden!important}.foodPoi img{display:block!important;width:36px!important;height:36px!important;max-width:none!important;max-height:none!important;object-fit:contain!important;transform:none!important}"
if '.foodPoi img{display:block!important;width:36px!important' not in s:
    pos=s.find('</style>')
    if pos<0: raise SystemExit('style carte introuvable')
    s=s[:pos]+css+s[pos:]

if 'ESSENCE_QUEBEC_188' not in s:
    s=s.replace('private static final String ESSENCE_QUEBEC_187="1.8.7";',
                'private static final String ESSENCE_QUEBEC_187="1.8.7";\n    private static final String ESSENCE_QUEBEC_188="1.8.8";',1)

p.write_text(s,encoding='utf-8')

g=root/'app/build.gradle'; gs=g.read_text(encoding='utf-8')
gs=re.sub(r'versionCode\s+\d+','versionCode 37',gs,count=1)
gs=re.sub(r"versionName\s+'[^']+'","versionName '1.8.8'",gs,count=1)
g.write_text(gs,encoding='utf-8')

for f in [assets/'marker_mccafe.png',assets/'marker_timhortons.webp']:
    if not f.exists() or f.stat().st_size < 500:
        raise SystemExit('ressource cafe invalide: '+str(f))
required=['ESSENCE_QUEBEC_188','marker_mccafe.png','marker_timhortons.webp','marker_finance.png',"[40,40]","[20,20]",'width:36px!important']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('patch_v188 incomplet: '+repr(missing))
print('Essence Quebec 1.8.8: images McCafe/Tim Hortons fixes 40x40, finance preservee')
