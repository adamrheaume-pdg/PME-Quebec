from pathlib import Path
import re

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# A 60 km, demander d'abord les stations par distance pour conserver toutes les stations proches.
# Le tri final de l'application est ensuite reapplique cote client avec sortStations(ss,sort).
old='String u=String.format(Locale.US,"https://www.gasquebec.ca/api/stations/nearby?lat=%.6f&lng=%.6f&radius=%d&fuelType=%s&limit=50&sort=%s",l.getLatitude(),l.getLongitude(),radius,fuel,sort);'
new='String apiSort=radius>=60?"distance":sort; int apiLimit=radius>=60?250:50; String u=String.format(Locale.US,"https://www.gasquebec.ca/api/stations/nearby?lat=%.6f&lng=%.6f&radius=%d&fuelType=%s&limit=%d&sort=%s",l.getLatitude(),l.getLongitude(),radius,fuel,apiLimit,apiSort);'
if old not in s:
    raise SystemExit('URL stations introuvable')
s=s.replace(old,new,1)

if 'ESSENCE_QUEBEC_185' not in s:
    s=s.replace('private static final String ESSENCE_QUEBEC_184="1.8.4";','private static final String ESSENCE_QUEBEC_184="1.8.4";\n    private static final String ESSENCE_QUEBEC_185="1.8.5";',1)

p.write_text(s,encoding='utf-8')

g=Path('EssenceLanaudiere/app/build.gradle')
gs=g.read_text(encoding='utf-8')
gs=re.sub(r'versionCode\s+\d+','versionCode 34',gs,count=1)
gs=re.sub(r"versionName\s+'[^']+'","versionName '1.8.5'",gs,count=1)
g.write_text(gs,encoding='utf-8')

required=['ESSENCE_QUEBEC_185','apiSort=radius>=60?"distance":sort','apiLimit=radius>=60?250:50','limit=%d&sort=%s']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('patch_v185 incomplet: '+repr(missing))
print('Essence Quebec 1.8.5: rayon 60 km conserve les stations proches')
