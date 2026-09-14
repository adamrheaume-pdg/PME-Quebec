from pathlib import Path
import re
p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')
if 'ESSENCE_QUEBEC_189' not in s:
    s=s.replace('private static final String ESSENCE_QUEBEC_188="1.8.8";','private static final String ESSENCE_QUEBEC_188="1.8.8";\n    private static final String ESSENCE_QUEBEC_189="1.8.9";',1)
p.write_text(s,encoding='utf-8')
g=Path('EssenceLanaudiere/app/build.gradle');t=g.read_text(encoding='utf-8')
t=re.sub(r'versionCode\s+\d+','versionCode 38',t,count=1)
t=re.sub(r"versionName\s+'[^']+'","versionName '1.8.9'",t,count=1)
g.write_text(t,encoding='utf-8')
for x in ['ESSENCE_QUEBEC_189','layerLegendJavascript()','eqAutoSyncHandler']:
    if x not in s: raise SystemExit('manque '+x)
if "versionName '1.8.9'" not in t: raise SystemExit('version invalide')
print('Essence Quebec 1.8.9 finalise')
