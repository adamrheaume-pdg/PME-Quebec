from pathlib import Path
import re

root=Path('EssenceLanaudiere')
p=root/'app/src/main/java/quebec/lanaudiere/essence/MainActivity.java'
s=p.read_text(encoding='utf-8')

# 1.8.7 — horaires par station façon Google Maps : état ouvert/fermé + 7 jours.
# Le code Google Places existe déjà depuis v1.7.0; ici on améliore l'affichage et la configuration de clé.
s=s.replace('body.put("textQuery",station.name+" "+displayStationAddress(station));',
            'body.put("textQuery",station.name+" "+displayStationAddress(station)); body.put("languageCode","fr-CA"); body.put("regionCode","CA");',1)

old='''            runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Horaires • "+station.name)\n                .setMessage(result).setPositiveButton("FERMER",null).show());'''
new='''            runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Horaires • "+station.name)\n                .setView(eqGoogleHoursView(result)).setPositiveButton("FERMER",null).show());'''
if old not in s:
    raise SystemExit('dialog horaires Google introuvable')
s=s.replace(old,new,1)

marker='    private String eqFetchGoogleHours(Station station,String key){'
if 'private View eqGoogleHoursView(String result)' not in s:
    helper=r'''    private View eqGoogleHoursView(String result){
        ScrollView sc=new ScrollView(this);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22),dp(10),dp(22),dp(12)); sc.addView(box);
        String raw=result==null?"":result.trim();
        String[] lines=raw.isEmpty()?new String[0]:raw.split("\\n");
        String status=""; LinkedHashMap<String,String> byDay=new LinkedHashMap<>();
        String[] order={"Dimanche","Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi"};
        for(String line:lines){
            String t=line==null?"":line.trim(); if(t.isEmpty())continue;
            if(t.equalsIgnoreCase("OUVERT MAINTENANT")||t.equalsIgnoreCase("FERMÉ MAINTENANT")){status=t;continue;}
            boolean matched=false;
            for(String d:order){
                if(t.toLowerCase(Locale.CANADA_FRENCH).startsWith(d.toLowerCase(Locale.CANADA_FRENCH))){
                    String v=t.substring(Math.min(t.length(),d.length())).replaceFirst("^[\\s:–—-]+","").trim();
                    byDay.put(d,v.isEmpty()?"Horaire non disponible":v); matched=true; break;
                }
            }
            if(!matched&&t.contains(":")){int k=t.indexOf(':');String d=t.substring(0,k).trim();String v=t.substring(k+1).trim();if(!d.isEmpty())byDay.put(d,v);}
        }
        if(!status.isEmpty()){
            boolean open=status.startsWith("OUVERT");
            TextView st=text(open?"●  Ouvert maintenant":"●  Fermé maintenant",18,true,open?Color.rgb(72,210,125):Color.rgb(255,95,110));
            st.setPadding(0,dp(4),0,dp(14)); box.addView(st);
        }
        if(byDay.isEmpty()){
            TextView msg=text(raw.isEmpty()?"Horaire non disponible pour cette station.":raw,16,false,Color.WHITE); msg.setPadding(0,dp(4),0,dp(8)); box.addView(msg); return sc;
        }
        for(String d:order){
            if(!byDay.containsKey(d))continue;
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.TOP); row.setPadding(0,dp(8),0,dp(8));
            TextView day=text(d,16,d.equals("Dimanche"),Color.WHITE); row.addView(day,new LinearLayout.LayoutParams(0,-2,1));
            TextView hrs=text(byDay.get(d),16,d.equals("Dimanche"),Color.rgb(225,230,238)); hrs.setGravity(Gravity.END); row.addView(hrs,new LinearLayout.LayoutParams(0,-2,1));
            box.addView(row);
        }
        for(Map.Entry<String,String> e:byDay.entrySet()){
            boolean known=false;for(String d:order)if(d.equals(e.getKey())){known=true;break;}if(known)continue;
            LinearLayout row=new LinearLayout(this);row.setPadding(0,dp(8),0,dp(8));TextView day=text(e.getKey(),16,false,Color.WHITE);row.addView(day,new LinearLayout.LayoutParams(0,-2,1));TextView hrs=text(e.getValue(),16,false,Color.rgb(225,230,238));hrs.setGravity(Gravity.END);row.addView(hrs,new LinearLayout.LayoutParams(0,-2,1));box.addView(row);
        }
        return sc;
    }

'''
    if marker not in s: raise SystemExit('eqFetchGoogleHours introuvable')
    s=s.replace(marker,helper+marker,1)

if 'ESSENCE_QUEBEC_187' not in s:
    s=s.replace('private static final String ESSENCE_QUEBEC_186="1.8.6";',
                'private static final String ESSENCE_QUEBEC_186="1.8.6";\n    private static final String ESSENCE_QUEBEC_187="1.8.7";',1)

p.write_text(s,encoding='utf-8')

# Clé Google Places fournie à la compilation via variable d'environnement/GitHub Secret.
g=root/'app/build.gradle'; gs=g.read_text(encoding='utf-8')
needle="targetSdk 35\n"
if 'manifestPlaceholders' not in gs:
    if needle not in gs: raise SystemExit('targetSdk introuvable dans build.gradle')
    gs=gs.replace(needle,needle+'        manifestPlaceholders = [GOOGLE_PLACES_API_KEY: (System.getenv("GOOGLE_PLACES_API_KEY") ?: "")]\n',1)
gs=re.sub(r'versionCode\s+\d+','versionCode 36',gs,count=1)
gs=re.sub(r"versionName\s+'[^']+'","versionName '1.8.7'",gs,count=1)
g.write_text(gs,encoding='utf-8')

m=root/'app/src/main/AndroidManifest.xml'; ms=m.read_text(encoding='utf-8')
meta='        <meta-data android:name="com.google.android.geo.API_KEY" android:value="${GOOGLE_PLACES_API_KEY}" />\n'
if 'com.google.android.geo.API_KEY' not in ms:
    pos=ms.find('        <activity android:name=".MainActivity"')
    if pos<0: raise SystemExit('activity MainActivity introuvable dans manifest')
    ms=ms[:pos]+meta+ms[pos:]
m.write_text(ms,encoding='utf-8')

required=['ESSENCE_QUEBEC_187','eqGoogleHoursView(result)','languageCode","fr-CA','regionCode","CA']
missing=[x for x in required if x not in s]
if missing: raise SystemExit('patch_v187 incomplet: '+repr(missing))
if 'GOOGLE_PLACES_API_KEY' not in gs or 'com.google.android.geo.API_KEY' not in ms: raise SystemExit('configuration Google Places incomplete')
print('Essence Quebec 1.8.7: horaires Google par station, affichage 7 jours + statut ouvert/ferme')
