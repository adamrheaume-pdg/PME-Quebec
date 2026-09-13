from pathlib import Path
import re

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# 1.8.6 — marqueur caméra/radar/sécurité uniforme, compact et de taille fixe.
s=s.replace("file:///android_asset/marker_radar.png", "file:///android_asset/marker_safety.png")
s=re.sub(r"iconSize:\[44,44\],iconAnchor:\[22,22\]", "iconSize:[28,28],iconAnchor:[14,14]", s)
s=re.sub(r"iconSize:\[38,38\],iconAnchor:\[19,19\]", "iconSize:[28,28],iconAnchor:[14,14]", s)

# Normaliser la taille interne de l'image quelle que soit la version CSS produite par les patchs précédents.
s=re.sub(r"\.radarCam img\{[^}]*\}",
         ".radarCam img{display:block!important;width:28px!important;height:28px!important;max-width:none!important;max-height:none!important;object-fit:contain!important;transform:none!important}",
         s,count=1)
if 'width:28px!important' not in s:
    s=s.replace('</style></head><body>',
                ".radarCam{width:28px!important;height:28px!important;background:transparent!important;border:0!important;box-shadow:none!important}.radarCam img{display:block!important;width:28px!important;height:28px!important;max-width:none!important;max-height:none!important;object-fit:contain!important;transform:none!important}</style></head><body>",1)

if 'ESSENCE_QUEBEC_186' not in s:
    s=s.replace('private static final String ESSENCE_QUEBEC_185="1.8.5";',
                'private static final String ESSENCE_QUEBEC_185="1.8.5";\n    private static final String ESSENCE_QUEBEC_186="1.8.6";',1)

p.write_text(s,encoding='utf-8')

g=Path('EssenceLanaudiere/app/build.gradle')
gs=g.read_text(encoding='utf-8')
gs=re.sub(r'versionCode\s+\d+','versionCode 35',gs,count=1)
gs=re.sub(r"versionName\s+'[^']+'","versionName '1.8.6'",gs,count=1)
g.write_text(gs,encoding='utf-8')

required=['ESSENCE_QUEBEC_186','marker_safety.png','iconSize:[28,28],iconAnchor:[14,14]','width:28px!important','height:28px!important']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('patch_v186 incomplet: '+repr(missing))
print('Essence Quebec 1.8.6: icones camera/radar/securite uniformes 28x28')
