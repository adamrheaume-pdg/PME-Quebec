from pathlib import Path
import os

root = Path(os.environ['GRADLE_DIR'])
html = root / 'app/src/main/assets/index.html'
s = html.read_text(encoding='utf-8')

# Supabase doit être embarqué dans l'APK : aucune dépendance CDN au moment de se connecter.
s = s.replace(
    '<script src="https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2"></script>',
    '<script src="vendor/supabase-js.min.js"></script>'
)

# Un fichier local Android n'a pas à interpréter son URL comme un callback OAuth.
s = s.replace(
    'auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}',
    "auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:location.protocol!=='file:'}"
)

css = r'''
<style id="pme-login-android-5107">
#authGate.auth-gate{
  position:fixed!important;
  inset:0!important;
  width:100%!important;
  height:100dvh!important;
  max-height:100dvh!important;
  overflow-y:auto!important;
  overflow-x:hidden!important;
  -webkit-overflow-scrolling:touch!important;
  align-items:flex-start!important;
  justify-content:center!important;
  padding:max(14px,env(safe-area-inset-top)) 14px max(28px,env(safe-area-inset-bottom))!important;
  background:rgba(8,12,20,.92)!important;
  z-index:2147483600!important;
}
#authGate .auth-card{
  width:min(520px,100%)!important;
  max-width:520px!important;
  max-height:none!important;
  overflow:visible!important;
  margin:0 auto!important;
  padding:18px!important;
  border-radius:18px!important;
  background:#ffffff!important;
  color:#111827!important;
  box-shadow:0 20px 60px rgba(0,0,0,.45)!important;
}
#authGate .auth-card h1,#authGate .auth-card h2,#authGate .auth-card h3,
#authGate .auth-card strong{color:#111827!important;-webkit-text-fill-color:#111827!important}
#authGate .auth-card p,#authGate .auth-card .muted{
  color:#5b6472!important;
  -webkit-text-fill-color:#5b6472!important;
  opacity:1!important;
}
#authGate .auth-card label{
  color:#374151!important;
  -webkit-text-fill-color:#374151!important;
  opacity:1!important;
  font-weight:700!important;
}
#authGate .auth-card input,#authGate .auth-card select,#authGate .auth-card textarea{
  background:#ffffff!important;
  color:#111827!important;
  -webkit-text-fill-color:#111827!important;
  border:1px solid #9ca3af!important;
  opacity:1!important;
  min-height:48px!important;
}
#authGate .auth-card input::placeholder{color:#6b7280!important;opacity:1!important}
#authGate .auth-card button.primary,#authGate #emailPasswordLoginBtn{
  background:#001F97!important;
  color:#ffffff!important;
  -webkit-text-fill-color:#ffffff!important;
  min-height:48px!important;
}
#authGate .auth-card button.secondary{
  background:#111827!important;
  color:#ffffff!important;
  -webkit-text-fill-color:#ffffff!important;
  border:1px solid #334155!important;
  min-height:46px!important;
}
#authGate #authStatus{
  display:block!important;
  min-height:20px!important;
  color:#374151!important;
  -webkit-text-fill-color:#374151!important;
  font-weight:650!important;
  opacity:1!important;
}
#authGate .auth-brand{margin-bottom:8px!important}
#godGlobalLauncher{z-index:9000!important}
@media(max-width:600px){
  #authGate.auth-gate{padding-left:10px!important;padding-right:10px!important}
  #authGate .auth-card{padding:16px 14px!important;border-radius:16px!important}
  #authGate .auth-card h2{font-size:20px!important;line-height:1.2!important}
  #authGate .auth-card p{line-height:1.35!important}
  #authGate #authEmailBox>label{margin-top:7px!important}
  #authGate .auth-social{margin-top:6px!important;margin-bottom:6px!important}
}
@media(max-height:760px){
  #authGate .auth-card{padding-top:12px!important;padding-bottom:14px!important}
  #authGate .auth-card button{padding-top:9px!important;padding-bottom:9px!important}
}
</style>
'''

if 'pme-login-android-5107' not in s:
    s = s.replace('</head>', css + '\n</head>', 1)

js = r'''
<script id="pme-login-runtime-5107">
(function(){
  function authStatus(text){var n=document.getElementById('authStatus');if(n)n.textContent=text}
  function verifyRuntime(){
    if(!window.supabase || typeof window.supabase.createClient!=='function'){
      authStatus('Le module de connexion n’a pas chargé. Fermez puis rouvrez l’application.');
      return false;
    }
    return true;
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',verifyRuntime,{once:true});
  else verifyRuntime();
  window.addEventListener('load',verifyRuntime,{once:true});
  document.addEventListener('click',function(e){
    var b=e.target&&e.target.closest?e.target.closest('#emailPasswordLoginBtn'):null;
    if(!b)return;
    if(!verifyRuntime()){e.preventDefault();e.stopImmediatePropagation();return;}
    b.disabled=true;
    var old=b.textContent;
    b.textContent='Connexion…';
    setTimeout(function(){b.disabled=false;b.textContent=old},12000);
  },true);
})();
</script>
'''

if 'pme-login-runtime-5107' not in s:
    s = s.replace('</body>', js + '\n</body>', 1)

html.write_text(s, encoding='utf-8')
print('Correctif connexion Android appliqué')
