from pathlib import Path
import base64,re

root=Path('EssenceLanaudiere')
main=root/'app/src/main'
assets=main/'assets'
assets.mkdir(parents=True,exist_ok=True)
b64dir=root/'build_tools/eq188_assets_b64'

# Images fournies par l'utilisateur, préparées avec transparence et taille source 48x48.
(assets/'marker_mccafe.png').write_bytes(base64.b64decode((b64dir/'mccafe_00.txt').read_text(encoding='utf-8').strip()))
(assets/'marker_timhortons.webp').write_bytes(base64.b64decode((b64dir/'timhortons.webp.b64').read_text(encoding='utf-8').strip()))

p=main/'java/quebec/lanaudiere/essence/MainActivity.java'
s=p.read_text(encoding='utf-8')

old="function poiIcon(kind){const emo=kind==='mcd'?'🍔':'☕';const cls=kind==='mcd'?'mcdPoi':'timPoi';return L.divIcon({className:'',html:\"<div class='foodPoi \"+cls+\"'>\"+emo+\"</div>\",iconSize:[38,38],iconAnchor:[19,19]});}"
new="function poiIcon(kind){const src=kind==='mcd'?'file:///android_asset/marker_mccafe.png':'file:///android_asset/marker_timhortons.webp';const cls=kind==='mcd'?'mcdPoi':'timPoi';return L.divIcon({className:'',html:\"<div class='foodPoi \"+cls+\"'><img src='\"+src+\"'></div>\",iconSize:[40,40],iconAnchor:[20,20]});}"
if old not in s:
    raise SystemExit('poiIcon McDonald/Tim Hortons introuvable')
s=s.replace(old,new,1)

# Taille visuelle fixe, sans emoji, sans redimensionnement au zoom.
s=s.replace(".foodPoi{width:34px;height:34px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:21px;border:2px solid #fff;box-shadow:0 3px 10px #0007}.mcdPoi{background:#efc200}.timPoi{background:#8b2c1d}",
            ".foodPoi{width:40px!important;height:40px!important;border-radius:10px;display:flex;align-items:center;justify-content:center;border:1px solid #ffffff99;box-shadow:0 3px 10px #0008;background:#101522!important;overflow:hidden}.foodPoi img{display:block!important;width:36px!important;height:36px!important;max-width:none!important;max-height:none!important;object-fit:contain!important;transform:none!important}")

if 'ESSENCE_QUEBEC_188' not in s:
    s=s.replace('private static final String ESSENCE_QUEBEC_187="1.8.7";',
                'private static final String ESSENCE_QUEBEC_187="1.8.7";\n    private static final String ESSENCE_QUEBEC_188="1.8.8";',1)

p.write_text(s,encoding='utf-8')

g=root/'app/build.gradle'; gs=g.read_text(encoding='utf-8')
gs=re.sub(r'versionCode\s+\d+','versionCode 37',gs,count=1)
gs=re.sub(r"versionName\s+'[^']+'","versionName '1.8.8'",gs,count=1)
g.write_text(gs,encoding='utf-8')

required=['ESSENCE_QUEBEC_188','marker_mccafe.png','marker_timhortons.webp','iconSize:[40,40],iconAnchor:[20,20]','foodPoi img']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('patch_v188 incomplet: '+repr(missing))
print('Essence Quebec 1.8.8: icones McCafe et Tim Hortons fixes 40x40')
