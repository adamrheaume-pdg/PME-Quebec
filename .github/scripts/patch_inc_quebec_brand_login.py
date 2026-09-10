from pathlib import Path
import os, shutil, struct

root=Path(os.environ['GRADLE_DIR'])
html=root/'app/src/main/assets/index.html'
manifest=root/'app/src/main/AndroidManifest.xml'
res=root/'app/src/main/res'
icon=res/'drawable/pme_inc_quebec_icon.png'

s=html.read_text(encoding='utf-8')
for a,b in [
    ('Connexion à Soumission Québec','Connexion à INC Québec'),
    ('Soumission Québec','INC Québec'),
    ('PME Québec','INC Québec'),
    ('Créé au Québec par PME Québec','Créé au Québec par INC Québec')
]:
    s=s.replace(a,b)
s=s.replace('<title>PME Québec</title>','<title>INC Québec</title>')

style=r'''
<style id="inc-quebec-brand-style-8100">
html.inc-local-mode #authGate{display:none!important;visibility:hidden!important;opacity:0!important;pointer-events:none!important}
#incBrandLogo{display:block;width:112px;height:112px;object-fit:contain;margin:0 auto 12px;filter:drop-shadow(0 0 14px rgba(0,102,255,.42))}
#incLocalAccessBtn{width:100%;min-height:54px;margin:12px 0 2px;border:1px solid #1d4ed8!important;border-radius:12px!important;background:linear-gradient(180deg,#0f2b62,#081a3d)!important;color:#fff!important;font-weight:900!important;font-size:16px!important;box-shadow:0 0 18px rgba(0,90,255,.22)!important}
</style>
'''
if 'inc-quebec-brand-style-8100' not in s:
    pos=s.lower().rfind('</head>')
    if pos<0: raise SystemExit('Balise head introuvable')
    s=s[:pos]+style+s[pos:]

js=r'''
<script id="inc-quebec-login-runtime-8100">
(function(){
'use strict';
var LOCAL_KEY='inc-quebec-local-access-v2';
function isLocal(){try{return localStorage.getItem(LOCAL_KEY)==='1'}catch(e){return false}}
function closeNow(){
  var g=document.getElementById('authGate');
  if(g){
    g.classList.add('pme-auth-closed','hidden-force','hidden');
    g.style.setProperty('display','none','important');
    g.style.setProperty('visibility','hidden','important');
    g.style.setProperty('opacity','0','important');
    g.style.setProperty('pointer-events','none','important');
    g.setAttribute('aria-hidden','true');
    g.removeAttribute('aria-modal');
  }
  document.documentElement.classList.add('pme-auth-resolved','inc-local-mode');
  if(document.body){document.body.classList.remove('auth-open');document.body.style.removeProperty('overflow');document.body.style.removeProperty('pointer-events')}
  document.querySelectorAll('[inert]').forEach(function(n){n.removeAttribute('inert')});
  try{if(typeof restorePersistentNavigationState==='function')restorePersistentNavigationState({force:true})}catch(e){}
  try{if(typeof showAppPage==='function')showAppPage('quoteSection',{scroll:false})}catch(e){}
  try{window.focus();window.dispatchEvent(new Event('resize'))}catch(e){}
  return true;
}
function localAccess(){
  try{
    var now=new Date().toISOString();
    var sess={provider:'device',identifier:'inc-local-device',email:'',display:'Appareil local',role:'admin',created:now,local:true};
    localStorage.setItem(LOCAL_KEY,'1');
    localStorage.setItem('sq-auth-session-v1',JSON.stringify(sess));
    localStorage.setItem('sq-trusted-device-v1',JSON.stringify({enabled:true,at:now,mode:'local'}));
    try{if(typeof saveAuthSession==='function')saveAuthSession(sess)}catch(e){}
    closeNow();
    setTimeout(closeNow,40);setTimeout(closeNow,180);setTimeout(closeNow,700);
    return true;
  }catch(e){
    var n=document.getElementById('authInlineStatus');
    if(n){n.textContent='Erreur accès local : '+(e.message||e);n.className='err'}
    return false;
  }
}
function ensureUi(){
  document.title='INC Québec';
  var card=document.querySelector('#authGate .auth-card');
  if(!card)return false;
  if(!document.getElementById('incBrandLogo')){
    var logo=document.createElement('img');logo.id='incBrandLogo';logo.src='pme-brand-logo.png';logo.alt='INC Québec';card.insertBefore(logo,card.firstChild);
  }
  var old=null;
  card.querySelectorAll('button').forEach(function(b){var t=(b.textContent||'').toLowerCase();if(t.includes('entrer sur cet appareil sans mot de passe'))old=b});
  var btn=document.getElementById('incLocalAccessBtn');
  if(!btn){
    btn=document.createElement('button');btn.id='incLocalAccessBtn';btn.type='button';btn.textContent='📱 Entrer dans INC Québec sur cet appareil';
    if(old){old.style.display='none';old.insertAdjacentElement('afterend',btn)}else card.appendChild(btn);
  }
  btn.disabled=false;btn.style.pointerEvents='auto';
  btn.onclick=function(ev){if(ev){ev.preventDefault();ev.stopPropagation()}localAccess();return false};
  return true;
}
function install(){
  ensureUi();
  document.addEventListener('click',function(ev){var b=ev.target&&ev.target.closest?ev.target.closest('#incLocalAccessBtn'):null;if(!b)return;ev.preventDefault();ev.stopImmediatePropagation();localAccess()},true);
  document.addEventListener('pointerup',function(ev){var b=ev.target&&ev.target.closest?ev.target.closest('#incLocalAccessBtn'):null;if(!b)return;ev.preventDefault();localAccess()},true);
  if(isLocal())closeNow();
  var mo=new MutationObserver(function(){ensureUi();if(isLocal())closeNow()});
  mo.observe(document.documentElement,{attributes:true,childList:true,subtree:true,attributeFilter:['class','style','hidden','disabled']});
  setTimeout(function(){ensureUi();if(isLocal())closeNow()},250);
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});else install();
window.INC_QUEBEC_LOCAL_ACCESS=localAccess;
window.INC_QUEBEC_CLOSE_AUTH=closeNow;
window.INC_QUEBEC_SELF_TEST=function(){return {title:document.title==='INC Québec',gate:!!document.getElementById('authGate'),button:!!document.getElementById('incLocalAccessBtn'),local:isLocal(),logo:!!document.getElementById('incBrandLogo')}};
})();
</script>
'''
# Supprimer l'ancien runtime si jamais un build intermédiaire l'a déjà laissé dans la source.
start=s.find('<script id="inc-quebec-login-runtime-8000">')
if start>=0:
    end=s.find('</script>',start)
    if end>=0:s=s[:start]+s[end+9:]
if 'inc-quebec-login-runtime-8100' not in s:
    pos=s.lower().rfind('</body>')
    if pos<0: raise SystemExit('Balise body introuvable')
    s=s[:pos]+js+s[pos:]
html.write_text(s,encoding='utf-8')

# Icône Android: utiliser un vrai launcher mipmap/adaptive icon, pas un simple drawable.
if not icon.exists() or icon.stat().st_size < 5000:
    raise SystemExit('Logo INC Québec PNG absent ou invalide après décodage')
data=icon.read_bytes()
if data[:8] != b'\x89PNG\r\n\x1a\n': raise SystemExit('Logo INC Québec: PNG attendu')
w,h=struct.unpack('>II',data[16:24])
if w < 48 or h < 48: raise SystemExit(f'Logo INC Québec trop petit: {w}x{h}')
for density in ['mdpi','hdpi','xhdpi','xxhdpi','xxxhdpi']:
    d=res/f'mipmap-{density}';d.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(icon,d/'ic_launcher.png')
    shutil.copyfile(icon,d/'ic_launcher_round.png')
anydpi=res/'mipmap-anydpi-v26';anydpi.mkdir(parents=True,exist_ok=True)
adaptive='''<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/inc_icon_background"/><foreground android:drawable="@drawable/pme_inc_quebec_icon"/></adaptive-icon>\n'''
(anydpi/'ic_launcher.xml').write_text(adaptive,encoding='utf-8')
(anydpi/'ic_launcher_round.xml').write_text(adaptive,encoding='utf-8')
values=res/'values';values.mkdir(parents=True,exist_ok=True)
(values/'inc_icon_colors.xml').write_text('''<resources><color name="inc_icon_background">#001F97</color></resources>\n''',encoding='utf-8')
m=manifest.read_text(encoding='utf-8')
m=m.replace('android:label="PME Québec"','android:label="INC Québec"')
m=m.replace('android:icon="@drawable/pme_inc_quebec_icon"','android:icon="@mipmap/ic_launcher"')
m=m.replace('android:roundIcon="@drawable/pme_inc_quebec_icon"','android:roundIcon="@mipmap/ic_launcher_round"')
manifest.write_text(m,encoding='utf-8')

# Tests de génération: le build doit échouer si le correctif essentiel manque.
out=html.read_text(encoding='utf-8')
assert 'Connexion à INC Québec' in out
assert 'inc-quebec-login-runtime-8100' in out
assert 'incLocalAccessBtn' in out
assert 'location.reload()' not in out[out.find('inc-quebec-login-runtime-8100'):]
assert 'pme-brand-logo.png' in out
mm=manifest.read_text(encoding='utf-8')
assert 'android:label="INC Québec"' in mm
assert 'android:icon="@mipmap/ic_launcher"' in mm
assert 'android:roundIcon="@mipmap/ic_launcher_round"' in mm
print(f'INC Québec branding/auth/launcher corrigés; icône source {w}x{h}')
