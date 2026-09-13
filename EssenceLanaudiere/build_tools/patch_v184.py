from pathlib import Path
import re

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

# Marqueur utilisateur 1.8.4 : dimensions constantes, pointe exactement sur la coordonnée GPS,
# et mise à jour du même objet Leaflet au lieu de suppression/recréation.
pat=re.compile(r'window\.eqMoveUser=\(la,lo,heading,speed\)=>\{.*?\};"\+', re.S)
m=pat.search(s)
if not m:
    raise SystemExit('eqMoveUser v183 introuvable')

new_js=r'''window.eqMoveUser=(la,lo,heading,speed)=>{const ll=[Number(la),Number(lo)],sp=Number(speed||0),now=Date.now();let delta=0;if(window.eqLastUser){const a=window.eqLastUser[0]*Math.PI/180,b=ll[0]*Math.PI/180,dp=(ll[0]-window.eqLastUser[0])*Math.PI/180,dl=(ll[1]-window.eqLastUser[1])*Math.PI/180,q=Math.sin(dp/2)**2+Math.cos(a)*Math.cos(b)*Math.sin(dl/2)**2;delta=2*6371000*Math.asin(Math.sqrt(q));}const recent=!window.eqLastUserTs||(now-window.eqLastUserTs)<20000;const moving=sp>0.8||(recent&&delta>12);window.eqLastUser=ll;window.eqLastUserTs=now;const src=moving?'file:///android_asset/marker_user_moving.png':'file:///android_asset/marker_user_stationary.png';const ico=L.divIcon({className:'eqUserLeafletIcon',html:\"<div class='eqUserPin'><img src='\"+src+\"'></div>\",iconSize:[48,64],iconAnchor:[24,64]});if(window.eqUserLabel&&window.eqUserLabel.remove){window.eqUserLabel.remove();window.eqUserLabel=null;}if(window.eqUserDot&&window.eqUserDot.setLatLng){window.eqUserDot.setLatLng(ll);window.eqUserDot.setIcon(ico);window.eqUserDot.setPopupContent(moving?'<b>EN MOUVEMENT</b>':'<b>VOUS ÊTES ICI</b>');}else{window.eqUserDot=L.marker(ll,{icon:ico,zIndexOffset:5000,riseOnHover:false}).addTo(map).bindPopup(moving?'<b>EN MOUVEMENT</b>':'<b>VOUS ÊTES ICI</b>');}if(eqFollowUser)map.panTo(ll,{animate:true,duration:.45});};"+'''
s=s[:m.start()]+new_js+s[m.end():]

# Corriger aussi le marqueur initial : même taille et ancrage exact au bout de la pointe.
s=s.replace("iconSize:[48,64],iconAnchor:[24,58]","iconSize:[48,64],iconAnchor:[24,64]")

# Forcer une boîte CSS stable; aucune transition/animation de taille sur l'icône elle-même.
s=s.replace(".eqUserPin{background:transparent!important;border:0!important}.eqUserPin img{width:100%;height:100%;object-fit:contain;filter:drop-shadow(0 4px 7px #0009)}",
            ".eqUserLeafletIcon{background:transparent!important;border:0!important;width:48px!important;height:64px!important}.eqUserPin{background:transparent!important;border:0!important;width:48px!important;height:64px!important;overflow:visible!important;transition:none!important;animation:none!important}.eqUserPin img{display:block!important;width:48px!important;height:64px!important;max-width:none!important;max-height:none!important;object-fit:contain!important;transform:none!important;transition:none!important;animation:none!important;filter:drop-shadow(0 4px 7px #0009)}")

if 'ESSENCE_QUEBEC_184' not in s:
    s=s.replace('private static final String ESSENCE_QUEBEC_183="1.8.3";',
                'private static final String ESSENCE_QUEBEC_183="1.8.3";\n    private static final String ESSENCE_QUEBEC_184="1.8.4";',1)

p.write_text(s,encoding='utf-8')

g=Path('EssenceLanaudiere/app/build.gradle')
gs=g.read_text(encoding='utf-8')
gs=re.sub(r'versionCode\s+\d+','versionCode 33',gs,count=1)
gs=re.sub(r"versionName\s+'[^']+'","versionName '1.8.4'",gs,count=1)
g.write_text(gs,encoding='utf-8')

required=['ESSENCE_QUEBEC_184','iconSize:[48,64],iconAnchor:[24,64]','setLatLng(ll)','setIcon(ico)','eqUserLeafletIcon','width:48px!important','height:64px!important']
missing=[x for x in required if x not in s]
if missing:
    raise SystemExit('patch_v184 incomplet: '+repr(missing))
print('Essence Quebec 1.8.4: marqueur GPS fixe et ancre exactement sur la position utilisateur')
