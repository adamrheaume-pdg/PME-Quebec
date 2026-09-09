from pathlib import Path
import os
root=Path(os.environ.get("GRADLE_DIR","source-5.10.4/PME-Quebec-5.10-Native-Wyze-Source"))
p=root/"app/src/main/assets/index.html"
s=p.read_text(encoding="utf-8")
if "pme-erp-suite-6000" in s:
    print("ERP suite déjà présente"); raise SystemExit(0)
style=r"""
<style id="pme-erp-suite-6000">
html{scroll-behavior:auto!important}button,a,[role=button]{touch-action:manipulation}
#pmeBrandStrip{display:flex;align-items:center;gap:10px;padding:8px 12px;background:#071120;border-bottom:1px solid #17335f}
#pmeBrandStrip img{width:52px;height:52px;object-fit:contain}
#pmeQuickBar,#pmeSuiteNav{display:flex;gap:8px;overflow-x:auto;padding:8px 10px;background:#0d1320;position:relative;z-index:9}
#pmeQuickBar button,#pmeSuiteNav button{white-space:nowrap;background:#001F97;color:#fff;border:1px solid #2447c7}
#pmeSuiteNav{background:#111827;border-top:1px solid #28364d;border-bottom:1px solid #28364d}
#pmeOverlay{position:fixed;inset:0;z-index:2147481000;background:#0b0f17;color:#fff;display:none;overflow:auto;padding:calc(12px + env(safe-area-inset-top)) 12px calc(20px + env(safe-area-inset-bottom))}
#pmeOverlay.show{display:block}#pmeOverlay .pmeHead{position:sticky;top:0;background:#0b0f17;padding:8px 0;display:flex;justify-content:space-between;align-items:center;z-index:2}
#pmeOverlay .pmeCard{background:#111827;border:1px solid #2c3748;border-radius:14px;padding:14px;margin:10px 0}
#pmeOverlay input,#pmeOverlay select,#pmeOverlay textarea{background:#0b0f17;color:#fff;border:1px solid #3d485a}
.pmeGrid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.pmeGrid3{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px}
.pmeTable{width:100%;border-collapse:collapse}.pmeTable th,.pmeTable td{padding:8px;border-bottom:1px solid #2c3748;text-align:left;font-size:12px}
.pmeBlinkRed{animation:pmeRed .8s infinite}.pmeBlinkYellow{animation:pmeYellow 1s infinite}.pmeBlinkGreen{animation:pmeGreen 1.1s infinite}.pmeBlinkBlue{animation:pmeBlue 1.1s infinite}
@keyframes pmeRed{50%{box-shadow:0 0 18px #f00;background:#5b1010}}@keyframes pmeYellow{50%{box-shadow:0 0 18px #ffd000;background:#5b4a10}}
@keyframes pmeGreen{50%{box-shadow:0 0 18px #00d66b;background:#0d4f2d}}@keyframes pmeBlue{50%{box-shadow:0 0 18px #2f7cff;background:#10295b}}
.pmeInvBlue{border:2px solid #2876ff!important}.pmeInvYellow{background:#fff1a8!important;color:#111!important}.pmeInvRed{animation:pmeRed .8s infinite}.pmeInvSmall{animation:pmeGreen 1s infinite}
.pmeBadge{display:inline-block;border-radius:999px;padding:4px 8px;background:#1b2a44}.pmeMuted{color:#aeb9c9}.pmeReadonly{opacity:.82}
#pmeDocViewer img,#pmeDocViewer iframe{max-width:100%;width:100%;min-height:62vh;object-fit:contain;background:#000;touch-action:pinch-zoom}
@media(max-width:700px){.pmeGrid,.pmeGrid3{grid-template-columns:1fr}#pmeBrandStrip{padding-top:8px}}
</style>
"""
js=r"""
<script src="vendor/jsbarcode.min.js"></script>
<script id="pme-erp-suite-runtime-6000">
(function(){
'use strict';
const KEY='pme-erp-suite-v1';
const blank={employees:[],vacations:[],bulletins:[],sales:[],products:[],stock:[],purchaseOrders:[],stockMoves:[],agenda:[],notifications:[],messages:[],docs:[]};
let st=load();
function load(){try{return Object.assign({},blank,JSON.parse(localStorage.getItem(KEY)||'{}'))}catch(e){return JSON.parse(JSON.stringify(blank))}}
function save(){localStorage.setItem(KEY,JSON.stringify(st));}
function id(p){return p+'-'+Date.now()+'-'+Math.random().toString(36).slice(2,7)}
function money(v){return Number(v||0).toLocaleString('fr-CA',{style:'currency',currency:'CAD'})}
function esc(x){return String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function addNotif(t,b){st.notifications.unshift({id:id('n'),t,b,date:new Date().toISOString(),read:false});save();renderBadges()}
function ensureChrome(){
 let h=document.querySelector('body>header')||document.querySelector('header');
 if(!h||document.getElementById('pmeBrandStrip'))return;
 let b=document.createElement('div');b.id='pmeBrandStrip';b.innerHTML='<img src="pme-brand-logo.png" alt="Inc Québec"><div><strong>PME Québec</strong><div class="pmeMuted">Prospérité • gestion intégrée</div></div>';h.insertAdjacentElement('afterend',b);
 let q=document.createElement('div');q.id='pmeQuickBar';q.innerHTML='<button data-pme="messages">💬 Messagerie <span id="pmeMsgBadge"></span></button><button data-pme="agenda">📅 Agenda</button><button data-pme="notifications">🔔 Notifications <span id="pmeNotifBadge"></span></button>';b.insertAdjacentElement('afterend',q);
 let n=document.createElement('div');n.id='pmeSuiteNav';n.innerHTML='<button data-pme="management">🏢 Centre de gestion PME</button><button data-pme="office">🗂️ Bureau</button><button data-pme="commerce">🛒 Commerce</button><button data-pme="documents">📄 Documents</button>';q.insertAdjacentElement('afterend',n);
}
function overlay(){let o=document.getElementById('pmeOverlay');if(!o){o=document.createElement('div');o.id='pmeOverlay';o.innerHTML='<div class="pmeHead"><strong id="pmeTitle">PME Québec</strong><button id="pmeClose">✕ Fermer</button></div><div id="pmeBody"></div>';document.body.appendChild(o);o.querySelector('#pmeClose').onclick=()=>o.classList.remove('show')}return o}
function openPage(name){let o=overlay(),b=o.querySelector('#pmeBody'),t=o.querySelector('#pmeTitle');o.classList.add('show');({management,office,commerce,agenda,messages,notifications,documents}[name]||management)(b,t)}
function cards(items){return '<div class="pmeGrid">'+items.map(x=>'<button data-open="'+x[0]+'" class="pmeCard">'+x[1]+'<br><small>'+x[2]+'</small></button>').join('')+'</div>'}
function management(b,t){t.textContent='Centre de gestion PME';b.innerHTML=cards([['admin','⚙️ Administration','Contrôle, approbations et audit'],['accounting','💰 Comptabilité','Factures, paiements, comptes à recevoir'],['hr','👥 Ressources humaines','Employés, vacances, babillard'],['stockAdmin','📦 Gestion des stocks','Inventaire, commandes, entrepôts et historique']]);wireSub(b)}
function office(b,t){t.textContent='Bureau — supervision';b.innerHTML=cards([['project','📋 Chargé de projet','Projets et échéanciers'],['stockView','📦 Inventaire — lecture seule','Stocks, entrepôts et historique'],['supervision','🦺 Supervision','Sécurité, SST, contremaître, chef d’équipe, gérant de projet']]);wireSub(b)}
function wireSub(b){b.querySelectorAll('[data-open]').forEach(x=>x.onclick=()=>sub(x.dataset.open,b))}
function sub(n,b){if(n==='hr')return hr(b);if(n==='stockAdmin')return stockAdmin(b,false);if(n==='stockView')return stockAdmin(b,true);b.innerHTML='<div class="pmeCard"><h2>'+esc(({admin:'Administration',accounting:'Comptabilité',project:'Chargé de projet',supervision:'Supervision — Sécurité • SST • Contremaître • Chef d’équipe • Gérant de projet'}[n]||n))+'</h2><p class="pmeMuted">Cette section centralise les fonctions existantes de PME Québec. Utilise le menu principal pour ouvrir les pages opérationnelles déjà présentes.</p></div>'}
function hr(b){
 b.innerHTML='<div class="pmeCard"><h2>Employés</h2><div class="pmeGrid3"><input id="eName" placeholder="Nom"><input id="eRole" placeholder="Poste"><input id="eSup" placeholder="Superviseur"></div><button id="eAdd">Ajouter employé</button><div id="eList"></div></div><div class="pmeCard"><h2>Vacances / absences</h2><div class="pmeGrid3"><select id="vEmp"></select><input id="vFrom" type="date"><input id="vTo" type="date"></div><button id="vAdd">Ajouter</button><div id="vList"></div></div><div class="pmeCard"><h2>Babillard</h2><input id="bTitle" placeholder="Activité / annonce"><input id="bDate" type="datetime-local"><textarea id="bText" placeholder="Description"></textarea><button id="bAdd">Publier</button><div id="bList"></div></div>';
 function r(){eList.innerHTML=st.employees.map(e=>'<div class="pmeCard"><b>'+esc(e.name)+'</b> — '+esc(e.role)+'<br><small>Superviseur: '+esc(e.supervisor||'—')+'</small> <button data-del-e="'+e.id+'">Supprimer</button></div>').join('');vEmp.innerHTML=st.employees.map(e=>'<option value="'+e.id+'">'+esc(e.name)+'</option>').join('');vList.innerHTML=st.vacations.map(v=>'<div>'+esc(v.name)+' : '+esc(v.from)+' → '+esc(v.to)+'</div>').join('');bList.innerHTML=st.bulletins.map(x=>'<div class="pmeCard"><b>'+esc(x.title)+'</b><br>'+esc(x.date)+'<br>'+esc(x.text)+'</div>').join('');document.querySelectorAll('[data-del-e]').forEach(x=>x.onclick=()=>{st.employees=st.employees.filter(e=>e.id!==x.dataset.delE);save();r()})}
 eAdd.onclick=()=>{if(!eName.value.trim())return;st.employees.push({id:id('emp'),name:eName.value.trim(),role:eRole.value.trim(),supervisor:eSup.value.trim(),active:true});save();r()};
 vAdd.onclick=()=>{let e=st.employees.find(x=>x.id===vEmp.value);if(!e)return;st.vacations.push({id:id('vac'),emp:e.id,name:e.name,from:vFrom.value,to:vTo.value});st.agenda.push({id:id('a'),type:'vacances',title:'Vacances — '+e.name,date:vFrom.value,status:'blue'});save();r()};
 bAdd.onclick=()=>{st.bulletins.unshift({id:id('bb'),title:bTitle.value,date:bDate.value,text:bText.value});save();r()};r()
}
function stockAdmin(b,ro){
 b.innerHTML='<div class="pmeCard '+(ro?'pmeReadonly':'')+'"><h2>Inventaire '+(ro?'— supervision, lecture seule':'— Administration')+'</h2>'+(!ro?'<div class="pmeGrid3"><input id="pName" placeholder="Produit"><input id="pSku" placeholder="UGS / code-barres"><input id="pQty" type="number" placeholder="Quantité"><input id="pPlace" placeholder="Entrepôt / magasin"><input id="pSupplier" placeholder="Fournisseur"><input id="pCost" type="number" step=".01" placeholder="Coût"></div><button id="pAdd">Ajouter / ajuster</button>':'')+'<div id="pList"></div></div><div class="pmeCard"><h2>Historique stock / commandes fournisseurs</h2><div id="moveList"></div></div>';
 function r(){pList.innerHTML='<table class="pmeTable"><tr><th>Produit</th><th>UGS</th><th>Lieu</th><th>Qté</th><th>Fournisseur</th></tr>'+st.products.map(x=>'<tr><td>'+esc(x.name)+'</td><td>'+esc(x.sku)+'</td><td>'+esc(x.place)+'</td><td class="'+(x.qty<10?'pmeBlinkRed':'')+'">'+x.qty+'</td><td>'+esc(x.supplier)+'</td></tr>').join('')+'</table>';moveList.innerHTML=st.stockMoves.slice().reverse().map(m=>'<div>'+esc(m.date)+' — '+esc(m.label)+' ('+m.delta+')</div>').join('')}
 if(!ro)pAdd.onclick=()=>{let sku=pSku.value.trim(),x=st.products.find(z=>z.sku===sku&&z.place===pPlace.value.trim());let nq=Number(pQty.value||0);if(x){let d=nq-x.qty;x.qty=nq;st.stockMoves.push({id:id('m'),date:new Date().toLocaleString('fr-CA'),label:x.name+' — '+x.place,delta:d})}else{x={id:id('p'),name:pName.value.trim(),sku,qty:nq,place:pPlace.value.trim(),supplier:pSupplier.value.trim(),cost:Number(pCost.value||0)};st.products.push(x);st.stockMoves.push({id:id('m'),date:new Date().toLocaleString('fr-CA'),label:x.name+' — '+x.place,delta:nq})}checkLow(x);save();r()};r()
}
function checkLow(x){if(Number(x.qty)>=10)return;let exists=st.purchaseOrders.some(o=>o.productId===x.id&&['approval','approved','ordered'].includes(o.status));if(exists)return;let o={id:id('po'),productId:x.id,product:x.name,supplier:x.supplier,qty:Math.max(10-x.qty,10),status:'approval',date:new Date().toISOString()};st.purchaseOrders.push(o);addNotif('Autorisation d’achat requise','Stock sous 10 : '+x.name+' — '+x.place)}
function commerce(b,t){
 t.textContent='Commerce / Ventes';b.innerHTML='<div class="pmeCard"><h2>Nouvelle vente</h2><div class="pmeGrid3"><input id="sClient" placeholder="Client"><input id="sAmount" type="number" step=".01" placeholder="Montant TTC"><select id="sType"><option>Vente au détail</option><option>Vente par téléphone</option><option>Service / bon de travail</option></select><select id="sSeller"></select><input id="sSupervisor" placeholder="Superviseur"><select id="sPay"><option>Interac débit</option><option>Carte de crédit</option><option>Comptant</option><option>Virement</option><option>Chèque</option><option>Sur compte</option></select></div><label><input id="sInstall" type="checkbox"> Paiement échelonné / en attente</label><input id="sDue" type="date"><button id="sCreate">Créer facture</button><div class="pmeMuted">Débit/crédit : la facture ne passe à Payée qu’après confirmation du terminal.</div></div><div id="saleList"></div>';
 sSeller.innerHTML='<option value="">Vendeur</option>'+st.employees.filter(e=>e.active).map(e=>'<option>'+esc(e.name)+'</option>').join('');
 sSeller.onchange=()=>{let e=st.employees.find(x=>x.name===sSeller.value);if(e)sSupervisor.value=e.supervisor||''};
 sCreate.onclick=()=>{let sale={id:id('inv'),client:sClient.value,amount:Number(sAmount.value||0),type:sType.value,seller:sSeller.value,supervisor:sSupervisor.value,pay:sPay.value,status:(/Interac|crédit/.test(sPay.value)?'awaiting-terminal':'pending'),created:new Date().toISOString(),due:sDue.value,installments:!!sInstall.checked,activeEmployees:st.employees.filter(e=>e.active).map(e=>e.name)};st.sales.unshift(sale);st.agenda.push({id:id('a'),type:'sale',title:'Vente '+sale.client+' '+money(sale.amount),date:new Date().toISOString().slice(0,10),status:'blue'});save();renderSales();};
 renderSales();
 function renderSales(){saleList.innerHTML=st.sales.map(v=>'<div class="pmeCard '+invClass(v)+'"><b>'+money(v.amount)+' — '+esc(v.client||'Client comptoir')+'</b><br><span class="pmeBadge">'+esc(v.status)+'</span> • '+esc(v.pay)+'<br>Vendeur: '+esc(v.seller||'—')+' | Superviseur: '+esc(v.supervisor||'—')+'<br><small>Employés actifs: '+esc((v.activeEmployees||[]).join(', ')||'—')+'</small><br>'+(/Interac|crédit/.test(v.pay)&&v.status!=='paid'?'<button data-terminal="'+v.id+'">Envoyer au terminal</button>':'')+'</div>').join('');document.querySelectorAll('[data-terminal]').forEach(x=>x.onclick=()=>{let v=st.sales.find(s=>s.id===x.dataset.terminal);v.status='awaiting-terminal';save();addNotif('Paiement en attente terminal',money(v.amount)+' — '+v.client);renderSales()})}
}
function invClass(v){let a=Number(v.amount);if(a>=5000)return'pmeInvRed';if(a>=1000)return'pmeInvYellow';if(a>=100)return'pmeInvBlue';if(a<100&&v.status!=='paid')return'pmeInvSmall';return''}
window.PMEERP=window.PMEERP||{};
window.PMEERP.confirmTerminalPayment=function(ref,result){let v=st.sales.find(x=>x.id===ref);if(!v)return false;if(result&&result.approved===true){v.status='paid';v.terminalRef=result.reference||'';v.paidAt=new Date().toISOString();addNotif('Paiement confirmé',money(v.amount)+' — '+v.client)}else{v.status='pending';addNotif('Paiement refusé/annulé',v.client||ref)}save();return true};
function agenda(b,t){t.textContent='Agenda intégré';let all=[...st.agenda];st.purchaseOrders.forEach(o=>all.push({title:'Commande fournisseur — '+o.product,date:o.date.slice(0,10),status:o.status==='delivered'?'green':o.status==='shipping'?'yellow':'blue'}));st.vacations.forEach(v=>all.push({title:'Vacances — '+v.name,date:v.from,status:'blue'}));b.innerHTML='<div class="pmeCard"><b>Légende :</b> <span class="pmeBlinkRed">retard</span> • <span class="pmeBlinkYellow">en livraison</span> • <span class="pmeBlinkGreen">livré et payé</span> • <span class="pmeBlinkBlue">avant livraison</span></div>'+all.sort((a,b)=>String(a.date).localeCompare(String(b.date))).map(x=>'<div class="pmeCard '+statusClass(x)+'"><b>'+esc(x.title)+'</b><br>'+esc(x.date||'')+'</div>').join('')}
function statusClass(x){if(x.status==='red'||(x.date&&new Date(x.date)<new Date(new Date().toDateString())&&!['green','paid'].includes(x.status)))return'pmeBlinkRed';return x.status==='yellow'?'pmeBlinkYellow':x.status==='green'?'pmeBlinkGreen':'pmeBlinkBlue'}
function messages(b,t){t.textContent='Messagerie';b.innerHTML='<div class="pmeCard"><textarea id="msgText" placeholder="Nouveau message interne"></textarea><button id="msgSend">Envoyer</button></div><div id="msgs"></div>';function r(){msgs.innerHTML=st.messages.map(m=>'<div class="pmeCard">'+esc(m.date)+'<br>'+esc(m.text)+'</div>').join('')}msgSend.onclick=()=>{if(msgText.value.trim()){st.messages.unshift({id:id('msg'),date:new Date().toLocaleString('fr-CA'),text:msgText.value.trim()});msgText.value='';save();r()}};r()}
function notifications(b,t){t.textContent='Notifications';st.notifications.forEach(n=>n.read=true);save();b.innerHTML=st.notifications.map(n=>'<div class="pmeCard"><b>'+esc(n.t)+'</b><br>'+esc(n.b)+'<br><small>'+esc(n.date)+'</small></div>').join('')||'<p>Aucune notification.</p>';renderBadges()}
function documents(b,t){t.textContent='Documents';b.innerHTML='<div class="pmeCard"><input id="docFile" type="file" accept="image/*,.pdf,application/pdf"><button id="docOpen">Ouvrir / zoomer</button><div id="pmeDocViewer"></div></div>';docOpen.onclick=()=>{let f=docFile.files&&docFile.files[0];if(!f)return;let u=URL.createObjectURL(f);pmeDocViewer.innerHTML=f.type==='application/pdf'?'<iframe src="'+u+'"></iframe>':'<img src="'+u+'" alt="document">'}}
function renderBadges(){let n=document.getElementById('pmeNotifBadge'),m=document.getElementById('pmeMsgBadge');if(n)n.textContent=st.notifications.filter(x=>!x.read).length||'';if(m)m.textContent=st.messages.length?('('+st.messages.length+')'):''}
function autosave(){
 document.addEventListener('change',e=>{let x=e.target;if(!x.matches('input,textarea,select')||x.type==='password'||x.type==='file'||x.closest('#pmeOverlay'))return;let k='pme-field:'+location.pathname+':'+(x.id||x.name);if(!x.id&&!x.name)return;try{localStorage.setItem(k,x.type==='checkbox'?String(x.checked):x.value)}catch(_){}})
 document.querySelectorAll('input,textarea,select').forEach(x=>{if(x.type==='password'||x.type==='file'||x.closest('#pmeOverlay')||(!x.id&&!x.name))return;let v=localStorage.getItem('pme-field:'+location.pathname+':'+(x.id||x.name));if(v!==null&&!x.value){if(x.type==='checkbox')x.checked=v==='true';else x.value=v}})
}
function barcodes(){if(typeof JsBarcode!=='function'||!/Bon de livraison/i.test(document.body.innerText))return;document.querySelectorAll('table').forEach(t=>{let hs=[...t.querySelectorAll('th')].map(x=>x.textContent.trim());let i=hs.findIndex(x=>/UGS|code.?barre/i.test(x));if(i<0)return;[...t.querySelectorAll('tr')].slice(1).forEach(r=>{let c=r.children[i];if(!c||c.querySelector('svg'))return;let val=c.textContent.trim();if(!val)return;let svg=document.createElementNS('http://www.w3.org/2000/svg','svg');c.appendChild(svg);try{JsBarcode(svg,val,{format:'CODE128',height:28,width:1,displayValue:false,margin:2})}catch(_){svg.remove()}})})}
document.addEventListener('click',e=>{let x=e.target.closest('[data-pme]');if(x){e.preventDefault();openPage(x.dataset.pme)}});
function boot(){ensureChrome();overlay();autosave();renderBadges();barcodes();setTimeout(()=>{ensureChrome();barcodes()},800)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
new MutationObserver(()=>{clearTimeout(window.__pmeErpMo);window.__pmeErpMo=setTimeout(()=>{ensureChrome();barcodes()},160)}).observe(document.documentElement,{subtree:true,childList:true});
})();
</script>
"""
def ins(tag,payload):
 global s
 i=s.lower().rfind(tag)
 if i<0: raise SystemExit(tag+" introuvable")
 s=s[:i]+payload+"\n"+s[i:]
ins("</head>",style)
ins("</body>",js)
p.write_text(s,encoding="utf-8")
print("Suite ERP PME Québec 6.0 injectée")
