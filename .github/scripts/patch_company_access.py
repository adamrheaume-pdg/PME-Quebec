from pathlib import Path
import os

root=Path(os.environ['GRADLE_DIR'])
html=root/'app/src/main/assets/index.html'
s=html.read_text(encoding='utf-8')

# Force the existing email/password route to be administration-master only.
old="const user=r?.data?.user||r?.data?.session?.user;if(!user)throw new Error('Aucun utilisateur retourné.');status('Étape 2/3 — authentification acceptée.','ok');const em=(typeof normalizeAccountEmail==='function'?normalizeAccountEmail(email):email)||email;const sess={provider:'supabase',identifier:em,email:em,display:em,role:(typeof isAuthorizedAdminEmail==='function'&&isAuthorizedAdminEmail(em))?'admin':'edition',created:new Date().toISOString()};"
new="const user=r?.data?.user||r?.data?.session?.user;if(!user)throw new Error('Aucun utilisateur retourné.');const portal=await timeout(sb.rpc('my_organization_portal'),12000);if(portal?.error)throw portal.error;const company=portal?.data?.[0];if(!company||company.member_role!=='admin')throw new Error('Ce portail est réservé à l’administration maître de l’entreprise.');status('Étape 2/3 — administration maître vérifiée.','ok');const em=(typeof normalizeAccountEmail==='function'?normalizeAccountEmail(email):email)||email;const sess={provider:'supabase',identifier:em,email:em,display:em,role:'admin',organizationId:company.organization_id,organizationName:company.organization_name,organizationCode:company.organization_code,created:new Date().toISOString()};"
if old in s:
    s=s.replace(old,new,1)

css=r'''
<style id="pme-company-access-5120">
#pmeAccessModes{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin:0 0 14px}
#pmeAccessModes button{min-height:42px;border:1px solid #cbd5e1;background:#f8fafc;color:#111827}
#pmeAccessModes button.active{background:#001F97;color:#fff;border-color:#001F97}
#pmeEmployeeLogin{display:none;margin-top:10px;padding-top:12px;border-top:1px solid #e5e7eb}
#pmeEmployeeLogin.show{display:block}
#pmeEmployeeLogin label{color:#374151!important}
#pmeEmployeeLogin input{background:#fff!important;color:#111827!important;border:1px solid #cbd5e1!important}
#pmeEmployeeAccessStatus{margin-top:8px;font-size:13px;font-weight:700;min-height:22px}
#pmeEmployeeAccessAdmin{margin-top:14px;padding:12px;border:1px solid #334155;border-radius:12px;background:#0f172a;color:#fff}
#pmeEmployeeAccessAdmin input,#pmeEmployeeAccessAdmin select{background:#fff;color:#111827}
</style>
'''
if 'pme-company-access-5120' not in s:
    s=s.replace('</head>',css+'\n</head>',1)

js=r'''
<script id="pme-company-access-runtime-5120">
(()=>{
const URL='https://tziawkkcwkzhfylmldih.supabase.co',KEY='sb_publishable_21Hj2UvI7rPkG3d_GUkEQQ_EByUbC-V', $=id=>document.getElementById(id);
const jset=(k,v)=>localStorage.setItem(k,JSON.stringify(v));
function closeGate(){const g=$('authGate');if(g){g.classList.add('pme-auth-closed','hidden-force','hidden');g.style.setProperty('display','none','important');g.style.setProperty('visibility','hidden','important');g.setAttribute('aria-hidden','true')}document.body?.classList.remove('auth-open');try{showAppPage?.('quoteSection',{scroll:false})}catch(e){}}
function setEmployeeStatus(t,ok=false){const e=$('pmeEmployeeAccessStatus');if(e){e.textContent=t;e.style.color=ok?'#047857':'#991b1b'}}
function installLoginModes(){const card=document.querySelector('#authGate .auth-card');const adminBtn=$('emailPasswordLoginBtn');if(!card||!adminBtn||$('pmeAccessModes'))return;
  const modes=document.createElement('div');modes.id='pmeAccessModes';modes.innerHTML='<button type="button" id="pmeAdminMode" class="active">Administration maître</button><button type="button" id="pmeEmployeeMode">Employé</button>';card.insertBefore(modes,card.firstChild);
  const pane=document.createElement('div');pane.id='pmeEmployeeLogin';pane.innerHTML='<h3>Connexion employé</h3><p class="muted">L’accès est créé et contrôlé par ton entreprise.</p><label>Code entreprise</label><input id="pmeEmployeeOrg" autocomplete="organization" placeholder="Ex. PME-AB12"><label>Code employé</label><input id="pmeEmployeeCode" type="password" inputmode="text" autocomplete="one-time-code" placeholder="Code fourni par l’administration"><button type="button" class="primary" id="pmeEmployeeLoginBtn" style="width:100%;margin-top:10px">Se connecter comme employé</button><div id="pmeEmployeeAccessStatus"></div>';adminBtn.parentElement?.appendChild(pane);
  const adminFields=[$('authEmail')?.closest('div')||$('authEmail'),$('authPassword')?.closest('div')||$('authPassword'),adminBtn,$('authInlineStatus')].filter(Boolean);
  const setMode=employee=>{modes.querySelector('#pmeAdminMode')?.classList.toggle('active',!employee);modes.querySelector('#pmeEmployeeMode')?.classList.toggle('active',employee);pane.classList.toggle('show',employee);adminFields.forEach(x=>x.style.display=employee?'none':'')};
  $('pmeAdminMode').onclick=()=>setMode(false);$('pmeEmployeeMode').onclick=()=>setMode(true);$('pmeEmployeeLoginBtn').onclick=employeeLogin;
}
async function employeeLogin(){const org=($('pmeEmployeeOrg')?.value||'').trim().toUpperCase(),code=($('pmeEmployeeCode')?.value||'').trim().toUpperCase(),btn=$('pmeEmployeeLoginBtn');if(!org||!code){setEmployeeStatus('Entre le code entreprise et ton code employé.');return}btn.disabled=true;btn.textContent='Connexion…';setEmployeeStatus('Vérification auprès de l’entreprise…');try{
  const res=await fetch(URL+'/functions/v1/employee-login',{method:'POST',headers:{'Content-Type':'application/json','apikey':KEY},body:JSON.stringify({organization_code:org,employee_code:code})});const data=await res.json().catch(()=>({}));if(!res.ok||!data.ok)throw new Error(data.error||'Accès refusé.');
  if(!window.supabase)throw new Error('Moteur Supabase indisponible.');const sb=(typeof cloudClient==='function'&&cloudClient())||window.supabase.createClient(URL,KEY,{auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:false}});const vr=await sb.auth.verifyOtp({token_hash:data.token_hash,type:data.verify_type||'magiclink'});if(vr.error)throw vr.error;
  const sess={provider:'supabase',role:'employee',identifier:data.employee?.id,email:vr.data?.user?.email||'',display:data.employee?.name||'Employé',employeeId:data.employee?.id,organizationId:data.organization?.id,organizationName:data.organization?.name,organizationCode:data.organization?.code,permissions:data.employee?.permissions||{},localCacheVersion:data.employee?.local_cache_version,created:new Date().toISOString()};jset('sq-auth-session-v1',sess);jset('pme-company-session-v1',{organization:data.organization,employee:data.employee,local_first:data.local_first,at:new Date().toISOString()});localStorage.setItem('pme-local-first-v1','1');try{saveAuthSession?.(sess)}catch(e){}setEmployeeStatus('Connexion autorisée ✓',true);closeGate();setTimeout(()=>{try{initCloudDataBridge?.({hydrate:true})}catch(e){}},250);
 }catch(e){setEmployeeStatus(String(e?.message||e))}finally{btn.disabled=false;btn.textContent='Se connecter comme employé'}}
function installAdminProvisioning(){const host=$('adminEmployeeManagement');if(!host||$('pmeEmployeeAccessAdmin'))return;const box=document.createElement('div');box.id='pmeEmployeeAccessAdmin';box.innerHTML='<h3>🔐 Accès employés</h3><p class="muted">L’administration maître crée ou remplace le code de connexion de chaque employé.</p><label>Employé</label><select id="pmeAccessEmployee"></select><label>Code employé (6 à 32 caractères)</label><input id="pmeAccessCode" type="password" autocomplete="new-password"><label>Rôle</label><select id="pmeAccessRole"><option value="employee">Employé</option><option value="accounting">Comptabilité</option></select><button type="button" class="primary" id="pmeProvisionEmployee" style="margin-top:10px">Créer / remplacer l’accès</button><div id="pmeProvisionStatus" class="muted" style="margin-top:8px"></div>';host.appendChild(box);fillEmployeeSelect();$('pmeProvisionEmployee').onclick=provisionEmployee}
function fillEmployeeSelect(){const sel=$('pmeAccessEmployee');if(!sel)return;let rows=[];try{rows=JSON.parse(localStorage.getItem('sq-employees-v2')||'[]')}catch(e){}sel.innerHTML='<option value="">Choisir…</option>'+rows.map(e=>`<option value="${String(e.id||e.employee_number||'').replace(/"/g,'&quot;')}">${String(e.name||e.fullName||e.employee_number||'Employé').replace(/</g,'&lt;')}</option>`).join('')}
async function provisionEmployee(){const id=$('pmeAccessEmployee')?.value||'',code=$('pmeAccessCode')?.value||'',role=$('pmeAccessRole')?.value||'employee',out=$('pmeProvisionStatus');if(!id||code.length<6){if(out)out.textContent='Choisis un employé et un code d’au moins 6 caractères.';return}try{const sb=typeof cloudClient==='function'?cloudClient():null;if(!sb)throw new Error('Supabase indisponible');const {data:{session}}=await sb.auth.getSession();if(!session?.access_token)throw new Error('Session administration requise');let rows=[];try{rows=JSON.parse(localStorage.getItem('sq-employees-v2')||'[]')}catch(e){}const emp=rows.find(x=>String(x.id||x.employee_number||'')===String(id))||{};const res=await fetch(URL+'/functions/v1/admin-provision-employee',{method:'POST',headers:{'Content-Type':'application/json','apikey':KEY,'Authorization':'Bearer '+session.access_token},body:JSON.stringify({employee_id:/^[0-9a-f-]{36}$/i.test(String(emp.id||''))?emp.id:'',employee_legacy_id:String(emp.id||emp.employee_number||id),employee_name:emp.name||emp.fullName||'Employé',employee_email:emp.email||'',access_code:code,role})});const data=await res.json().catch(()=>({}));if(!res.ok||!data.ok)throw new Error(data.error||'Création refusée');$('pmeAccessCode').value='';if(out)out.textContent='Accès créé ✓ Entreprise : '+(data.organization_code||'')+' · '+(data.employee_name||'Employé')}catch(e){if(out)out.textContent='Erreur : '+String(e?.message||e)}}
function reduceCloudTraffic(){try{localStorage.setItem('pme-local-first-v1','1');localStorage.setItem('pme-sync-policy-v1',JSON.stringify({mode:'local-first',debounce_ms:5000,realtime_hydrate_ms:1500,updatedAt:new Date().toISOString()}))}catch(e){}}
function init(){installLoginModes();installAdminProvisioning();reduceCloudTraffic();setTimeout(installAdminProvisioning,1200)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(init,80),{once:true});else setTimeout(init,80);
})();
</script>
'''
if 'pme-company-access-runtime-5120' not in s:
    s=s.replace('</body>',js+'\n</body>',1)

# Reduce high-frequency cloud chatter while keeping local storage immediate.
s=s.replace('},500);\n  cloudPendingKeys.set(key,t)','},5000);\n  cloudPendingKeys.set(key,t)')
s=s.replace('},250);\n          cloudRealtimeHydrateTimers.set(key,t)','},1500);\n          cloudRealtimeHydrateTimers.set(key,t)')

html.write_text(s,encoding='utf-8')
print('Architecture entreprise maître / employés et mode local-first appliqués')
