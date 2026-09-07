from pathlib import Path
import os

root = Path(os.environ['GRADLE_DIR'])
html = root / 'app/src/main/assets/index.html'
s = html.read_text(encoding='utf-8')

# Dépendances locales dans l'APK.
s = s.replace(
    '<script src="https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2"></script>',
    '<script src="vendor/supabase-js.min.js"></script>'
)
s = s.replace(
    'auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}',
    "auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:location.protocol!=='file:'}"
)

css = r'''
<style id="pme-login-android-5109">
#authGate.auth-gate{position:fixed!important;inset:0!important;width:100%!important;height:100dvh!important;max-height:100dvh!important;overflow-y:auto!important;overflow-x:hidden!important;-webkit-overflow-scrolling:touch!important;align-items:flex-start!important;justify-content:center!important;padding:max(14px,env(safe-area-inset-top)) 14px max(28px,env(safe-area-inset-bottom))!important;background:rgba(8,12,20,.92)!important;z-index:2147483600!important}
#authGate .auth-card{width:min(520px,100%)!important;max-width:520px!important;max-height:none!important;overflow:visible!important;margin:0 auto!important;padding:18px!important;border-radius:18px!important;background:#fff!important;color:#111827!important;box-shadow:0 20px 60px rgba(0,0,0,.45)!important}
#authGate .auth-card h1,#authGate .auth-card h2,#authGate .auth-card h3,#authGate .auth-card strong{color:#111827!important;-webkit-text-fill-color:#111827!important}
#authGate .auth-card p,#authGate .auth-card .muted{color:#5b6472!important;-webkit-text-fill-color:#5b6472!important;opacity:1!important}
#authGate .auth-card label{color:#374151!important;-webkit-text-fill-color:#374151!important;opacity:1!important;font-weight:700!important}
#authGate .auth-card input,#authGate .auth-card select,#authGate .auth-card textarea{background:#fff!important;color:#111827!important;-webkit-text-fill-color:#111827!important;border:1px solid #9ca3af!important;opacity:1!important;min-height:48px!important}
#authGate .auth-card button.primary,#authGate #emailPasswordLoginBtn{background:#001F97!important;color:#fff!important;-webkit-text-fill-color:#fff!important;min-height:48px!important}
#authGate .auth-card button.secondary{background:#111827!important;color:#fff!important;-webkit-text-fill-color:#fff!important;border:1px solid #334155!important;min-height:46px!important}
#authInlineStatus{display:block!important;margin:8px 0 2px!important;padding:9px 10px!important;border-radius:9px!important;background:#eef2ff!important;color:#1f2937!important;-webkit-text-fill-color:#1f2937!important;font-size:13px!important;font-weight:700!important;line-height:1.3!important;min-height:36px!important}
#authInlineStatus.ok{background:#ecfdf5!important;color:#065f46!important;-webkit-text-fill-color:#065f46!important}
#authInlineStatus.err{background:#fef2f2!important;color:#991b1b!important;-webkit-text-fill-color:#991b1b!important}
#godGlobalLauncher{z-index:9000!important}
@media(max-width:600px){#authGate.auth-gate{padding-left:10px!important;padding-right:10px!important}#authGate .auth-card{padding:16px 14px!important;border-radius:16px!important}#authGate .auth-card h2{font-size:20px!important;line-height:1.2!important}}
</style>
'''
if 'pme-login-android-5109' not in s:
    s = s.replace('</head>', css + '\n</head>', 1)

js = r'''
<script id="pme-login-runtime-5109">
(function(){
  const DEFAULT_URL='https://tziawkkcwkzhfylmldih.supabase.co';
  const DEFAULT_KEY='sb_publishable_21Hj2UvI7rPkG3d_GUkEQQ_EByUbC-V';
  let busy=false;
  const $=id=>document.getElementById(id);
  function setStatus(text,kind){
    let n=$('authInlineStatus');
    if(!n){
      const b=$('emailPasswordLoginBtn');
      if(b){n=document.createElement('div');n.id='authInlineStatus';n.setAttribute('aria-live','polite');b.insertAdjacentElement('afterend',n)}
    }
    if(n){n.className=kind||'';n.textContent=text}
    const old=$('authStatus');if(old)old.textContent=text;
  }
  function ensureConfig(){
    try{localStorage.setItem('sq-supabase-config-v1',JSON.stringify({url:DEFAULT_URL,key:DEFAULT_KEY}))}catch(e){}
  }
  function timeout(p,ms,label){
    return Promise.race([p,new Promise((_,rej)=>setTimeout(()=>rej(new Error(label||'Délai dépassé')),ms))]);
  }
  async function backgroundCloud(){
    try{if(typeof cloudEnsureMembership==='function')await timeout(Promise.resolve(cloudEnsureMembership()),8000,'membership timeout')}catch(e){console.warn('membership background',e)}
    try{if(typeof initCloudDataBridge==='function')await timeout(Promise.resolve(initCloudDataBridge({hydrate:true})),10000,'sync timeout')}catch(e){console.warn('sync background',e)}
  }
  async function robustLogin(){
    if(busy)return;
    const email=($('authEmail')?.value||'').trim().toLowerCase();
    const password=$('authPassword')?.value||'';
    const btn=$('emailPasswordLoginBtn');
    if(!email){setStatus('Entre ton adresse courriel.','err');$('authEmail')?.focus();return}
    if(!password){setStatus('Entre ton mot de passe.','err');$('authPassword')?.focus();return}
    ensureConfig();
    if(!window.supabase || typeof window.supabase.createClient!=='function'){setStatus('Erreur interne : moteur Supabase absent.','err');return}
    let sb=null;
    try{sb=(typeof cloudClient==='function'&&cloudClient())||window.supabase.createClient(DEFAULT_URL,DEFAULT_KEY,{auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:false}})}catch(e){setStatus('Impossible d’initialiser Supabase : '+String(e?.message||e),'err');return}
    if(!sb){setStatus('Impossible d’initialiser la connexion Cloud.','err');return}
    busy=true;if(btn){btn.disabled=true;btn.textContent='Connexion en cours…'}
    setStatus('Étape 1/3 — connexion au serveur…','');
    try{
      const result=await timeout(sb.auth.signInWithPassword({email,password}),12000,'Le serveur ne répond pas après 12 secondes.');
      if(result?.error)throw result.error;
      const user=result?.data?.user||result?.data?.session?.user;
      if(!user)throw new Error('Supabase n’a retourné aucun utilisateur.');
      setStatus('Étape 2/3 — authentification acceptée. Ouverture de l’application…','ok');
      const normalized=(typeof normalizeAccountEmail==='function'?normalizeAccountEmail(email):email)||email;
      const session={provider:'supabase',identifier:normalized,email:normalized,display:normalized,role:(typeof isAuthorizedAdminEmail==='function'&&isAuthorizedAdminEmail(normalized))?'admin':'edition',created:new Date().toISOString()};
      try{
        if($('rememberDeviceCheck')?.checked)localStorage.setItem('sq-trusted-device-v1',JSON.stringify({email:normalized,enabled:true,at:new Date().toISOString()}));
        if(typeof saveAuthSession==='function')saveAuthSession(session);else{localStorage.setItem('sq-auth-session-v1',JSON.stringify(session));if(typeof applyAuthState==='function')applyAuthState()}
      }catch(e){throw new Error('Session locale impossible : '+String(e?.message||e))}
      setStatus('Étape 3/3 — connecté ✓ Synchronisation en arrière-plan.','ok');
      setTimeout(()=>{try{if(typeof applyAuthState==='function')applyAuthState()}catch(e){}},0);
      setTimeout(backgroundCloud,50);
    }catch(e){
      const msg=String(e?.message||e||'Erreur inconnue');
      let friendly=msg;
      if(/invalid login credentials|invalid credentials/i.test(msg))friendly='Courriel ou mot de passe incorrect.';
      else if(/email not confirmed/i.test(msg))friendly='Le courriel du compte doit être confirmé.';
      else if(/failed to fetch|network|load failed/i.test(msg))friendly='Le téléphone n’arrive pas à joindre Supabase. Vérifie Internet puis réessaie.';
      setStatus('Connexion refusée : '+friendly,'err');
    }finally{busy=false;if(btn){btn.disabled=false;btn.textContent='Se connecter'}}
  }
  function install(){
    ensureConfig();
    const btn=$('emailPasswordLoginBtn');
    if(!btn)return;
    if(!$('authInlineStatus')){const n=document.createElement('div');n.id='authInlineStatus';n.setAttribute('aria-live','polite');n.textContent='Prêt à se connecter.';btn.insertAdjacentElement('afterend',n)}
    document.addEventListener('click',function(e){const b=e.target&&e.target.closest?e.target.closest('#emailPasswordLoginBtn'):null;if(!b)return;e.preventDefault();e.stopImmediatePropagation();robustLogin()},true);
    $('authPassword')?.addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();robustLogin()}});
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});else install();
})();
</script>
'''
if 'pme-login-runtime-5109' not in s:
    s = s.replace('</body>', js + '\n</body>', 1)

html.write_text(s, encoding='utf-8')
print('Correctif connexion Android 5.10.9 appliqué')
