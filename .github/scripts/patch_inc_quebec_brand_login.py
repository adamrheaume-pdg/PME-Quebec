from pathlib import Path
import os
root=Path(os.environ['GRADLE_DIR'])
html=root/'app/src/main/assets/index.html'
s=html.read_text(encoding='utf-8')
# Branding visible seulement
for a,b in [
    ('Connexion à Soumission Québec','Connexion à INC Québec'),
    ('Soumission Québec','INC Québec'),
    ('PME Québec','INC Québec'),
    ('Créé au Québec par PME Québec','Créé au Québec par INC Québec')
]:
    s=s.replace(a,b)
s=s.replace('<title>PME Québec</title>','<title>INC Québec</title>')
if 'inc-quebec-login-runtime-8000' not in s:
    js=r'''
<script id="inc-quebec-login-runtime-8000">
(function(){
'use strict';
function closeNow(){
  var g=document.getElementById('authGate');
  if(g){g.classList.add('pme-auth-closed','hidden-force','hidden');g.style.setProperty('display','none','important');g.style.setProperty('visibility','hidden','important');g.style.setProperty('pointer-events','none','important');g.setAttribute('aria-hidden','true');}
  document.documentElement.classList.add('pme-auth-resolved');
  if(document.body)document.body.classList.remove('auth-open');
  try{if(typeof applyAuthState==='function')applyAuthState()}catch(e){}
  try{if(typeof restorePersistentNavigationState==='function')restorePersistentNavigationState({force:true})}catch(e){}
}
function localAccess(){
  try{
    var sess={provider:'device',identifier:'inc-local-device',email:'',display:'Appareil local',role:'admin',created:new Date().toISOString()};
    localStorage.setItem('sq-auth-session-v1',JSON.stringify(sess));
    localStorage.setItem('sq-trusted-device-v1',JSON.stringify({enabled:true,at:new Date().toISOString(),mode:'local'}));
    try{if(typeof saveAuthSession==='function')saveAuthSession(sess)}catch(e){}
    closeNow();
    setTimeout(function(){location.reload()},80);
  }catch(e){alert('Impossible d’activer le mode appareil local : '+(e.message||e));}
}
function install(){
  document.title='INC Québec';
  var all=document.querySelectorAll('button');
  all.forEach(function(b){
    var t=(b.textContent||'').toLowerCase();
    if(t.includes('entrer sur cet appareil sans mot de passe')){
      b.disabled=false;
      b.style.removeProperty('opacity');
      b.addEventListener('click',function(ev){ev.preventDefault();ev.stopImmediatePropagation();localAccess()},true);
    }
  });
  try{if(localStorage.getItem('sq-auth-session-v1'))closeNow()}catch(e){}
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});else install();
window.INC_QUEBEC_LOCAL_ACCESS=localAccess;
})();
</script>
'''
    pos=s.lower().rfind('</body>')
    if pos<0: raise SystemExit('Balise body introuvable')
    s=s[:pos]+js+s[pos:]
html.write_text(s,encoding='utf-8')
print('Branding INC Québec et accès appareil local appliqués')
