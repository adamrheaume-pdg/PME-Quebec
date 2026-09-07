from pathlib import Path
import os

root = Path(os.environ['GRADLE_DIR'])
html = root / 'app/src/main/assets/index.html'
s = html.read_text(encoding='utf-8')

s = s.replace('<script src="https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2"></script>','<script src="vendor/supabase-js.min.js"></script>')
s = s.replace('auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}',"auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:location.protocol!=='file:'}")

css=r'''
<style id="pme-login-android-5110">
#authGate.auth-gate{position:fixed!important;inset:0!important;width:100%!important;height:100dvh!important;max-height:100dvh!important;overflow-y:auto!important;overflow-x:hidden!important;-webkit-overflow-scrolling:touch!important;align-items:flex-start!important;justify-content:center!important;padding:max(14px,env(safe-area-inset-top)) 14px max(28px,env(safe-area-inset-bottom))!important;background:rgba(8,12,20,.92)!important;z-index:2147483600!important}
#authGate.pme-auth-closed{display:none!important;visibility:hidden!important;opacity:0!important;pointer-events:none!important}
#authGate .auth-card{width:min(520px,100%)!important;max-width:520px!important;max-height:none!important;overflow:visible!important;margin:0 auto!important;padding:18px!important;border-radius:18px!important;background:#fff!important;color:#111827!important}
#authInlineStatus{display:block!important;margin:8px 0 2px!important;padding:9px 10px!important;border-radius:9px!important;background:#eef2ff!important;color:#1f2937!important;font-size:13px!important;font-weight:700!important;line-height:1.3!important;min-height:36px!important}
#authInlineStatus.ok{background:#ecfdf5!important;color:#065f46!important}#authInlineStatus.err{background:#fef2f2!important;color:#991b1b!important}
#authGate .auth-card button.primary,#authGate #emailPasswordLoginBtn{background:#001F97!important;color:#fff!important;min-height:48px!important}#authGate .auth-card button.secondary{background:#111827!important;color:#fff!important;min-height:46px!important}
</style>
'''
if 'pme-login-android-5110' not in s:s=s.replace('</head>',css+'\n</head>',1)

js=r'''
<script id="pme-login-runtime-5110">
(function(){
const URL='https://tziawkkcwkzhfylmldih.supabase.co',KEY='sb_publishable_21Hj2UvI7rPkG3d_GUkEQQ_EByUbC-V';let busy=false;const $=id=>document.getElementById(id);
function status(t,k){let n=$('authInlineStatus');if(!n){let b=$('emailPasswordLoginBtn');if(b){n=document.createElement('div');n.id='authInlineStatus';b.insertAdjacentElement('afterend',n)}}if(n){n.className=k||'';n.textContent=t}}
function closeGate(){const g=$('authGate');if(g){g.classList.add('pme-auth-closed','hidden-force','hidden');g.style.setProperty('display','none','important');g.style.setProperty('visibility','hidden','important');g.setAttribute('aria-hidden','true')}document.documentElement.classList.add('pme-auth-resolved');document.body?.classList.remove('auth-open');try{if(typeof applyAuthState==='function')applyAuthState()}catch(e){}try{if(typeof restorePersistentNavigationState==='function')restorePersistentNavigationState({force:true})}catch(e){}try{if(typeof showAppPage==='function')showAppPage('quoteSection',{scroll:false})}catch(e){}window.scrollTo(0,0)}
function timeout(p,ms){return Promise.race([p,new Promise((_,r)=>setTimeout(()=>r(new Error('Le serveur ne répond pas après 12 secondes.')),ms))])}
async function login(){if(busy)return;const email=($('authEmail')?.value||'').trim().toLowerCase(),password=$('authPassword')?.value||'',btn=$('emailPasswordLoginBtn');if(!email){status('Entre ton adresse courriel.','err');return}if(!password){status('Entre ton mot de passe.','err');return}if(!window.supabase){status('Moteur Supabase absent.','err');return}busy=true;if(btn){btn.disabled=true;btn.textContent='Connexion en cours…'}status('Étape 1/3 — connexion au serveur…','');try{localStorage.setItem('sq-supabase-config-v1',JSON.stringify({url:URL,key:KEY}));const sb=(typeof cloudClient==='function'&&cloudClient())||window.supabase.createClient(URL,KEY,{auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:false}});const r=await timeout(sb.auth.signInWithPassword({email,password}),12000);if(r?.error)throw r.error;const user=r?.data?.user||r?.data?.session?.user;if(!user)throw new Error('Aucun utilisateur retourné.');status('Étape 2/3 — authentification acceptée.','ok');const em=(typeof normalizeAccountEmail==='function'?normalizeAccountEmail(email):email)||email;const sess={provider:'supabase',identifier:em,email:em,display:em,role:(typeof isAuthorizedAdminEmail==='function'&&isAuthorizedAdminEmail(em))?'admin':'edition',created:new Date().toISOString()};localStorage.setItem('sq-auth-session-v1',JSON.stringify(sess));try{if(typeof saveAuthSession==='function')saveAuthSession(sess)}catch(e){}if($('rememberDeviceCheck')?.checked)localStorage.setItem('sq-trusted-device-v1',JSON.stringify({email:em,enabled:true,at:new Date().toISOString()}));status('Étape 3/3 — connecté ✓','ok');closeGate();setTimeout(closeGate,50);setTimeout(closeGate,250);setTimeout(()=>{try{if(typeof cloudEnsureMembership==='function')cloudEnsureMembership()}catch(e){}try{if(typeof initCloudDataBridge==='function')initCloudDataBridge({hydrate:true})}catch(e){}},300)}catch(e){let m=String(e?.message||e);if(/invalid login credentials|invalid credentials/i.test(m))m='Courriel ou mot de passe incorrect.';status('Connexion refusée : '+m,'err')}finally{busy=false;if(btn){btn.disabled=false;btn.textContent='Se connecter'}}}
function install(){const b=$('emailPasswordLoginBtn');if(!b)return;if(!$('authInlineStatus')){let n=document.createElement('div');n.id='authInlineStatus';n.textContent='Prêt à se connecter.';b.insertAdjacentElement('afterend',n)}document.addEventListener('click',e=>{let x=e.target?.closest?.('#emailPasswordLoginBtn');if(!x)return;e.preventDefault();e.stopImmediatePropagation();login()},true);$('authPassword')?.addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();login()}});try{if(localStorage.getItem('sq-auth-session-v1'))closeGate()}catch(e){}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});else install();
})();
</script>
'''
if 'pme-login-runtime-5110' not in s:s=s.replace('</body>',js+'\n</body>',1)
html.write_text(s,encoding='utf-8')
print('Correctif connexion Android 5.11.0 appliqué')
