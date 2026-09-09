from pathlib import Path
import os
root=Path(os.environ.get('GRADLE_DIR','source-5.10.4/PME-Quebec-5.10-Native-Wyze-Source'))
p=root/'app/src/main/assets/index.html'
s=p.read_text(encoding='utf-8')
if 'pme-quebec-visual-7000' in s:
    print('Visuel Québec déjà appliqué'); raise SystemExit(0)
STYLE=r'''
<style id="pme-quebec-visual-7000">
:root{--pme-blue:#0077ff;--pme-cyan:#20c7ff;--pme-navy:#031022;--pme-deep:#02060d;--pme-card:#07152a;--pme-line:#0d6dff;--pme-text:#f7fbff;--pme-muted:#9eb9d8}
html,body{background:radial-gradient(circle at 50% -10%,#0b3b8f 0,#061a3d 28%,#020712 62%,#010307 100%)!important;color:var(--pme-text)!important}
body:before{content:"";position:fixed;inset:0;pointer-events:none;z-index:-1;background:linear-gradient(140deg,rgba(0,119,255,.10),transparent 40%),radial-gradient(circle at 85% 15%,rgba(32,199,255,.16),transparent 24%)}
header{background:linear-gradient(180deg,rgba(3,16,34,.98),rgba(2,8,18,.96))!important;border-bottom:1px solid rgba(32,199,255,.28)!important;box-shadow:0 10px 30px rgba(0,0,0,.35)!important}
main{background:transparent!important}
.card,.module-link,.warehouse-panel,.ad-card,.site-preview,.saved-item{background:linear-gradient(180deg,rgba(8,30,63,.96),rgba(3,13,30,.98))!important;color:#fff!important;border:1px solid rgba(21,135,255,.58)!important;box-shadow:inset 0 0 0 1px rgba(255,255,255,.03),0 0 16px rgba(0,119,255,.12)!important}
h1,h2,h3,strong,label{color:#fff!important}.muted,.pmeMuted,small{color:var(--pme-muted)!important}
input,select,textarea{background:#041022!important;color:#fff!important;border:1px solid #20559b!important;box-shadow:inset 0 0 12px rgba(0,119,255,.08)!important}input::placeholder,textarea::placeholder{color:#7893b5!important}
button,.primary,.secondary{background:linear-gradient(180deg,#0b4fb8,#062e78)!important;color:#fff!important;border:1px solid #1aa7ff!important;box-shadow:inset 0 1px 0 rgba(255,255,255,.12),0 0 12px rgba(0,119,255,.18)!important}
button:active{transform:scale(.985)!important;filter:brightness(1.18)}
#pmeQuickBar,#pmeSuiteNav{display:grid!important;overflow:visible!important;background:transparent!important;border:0!important;padding:8px 12px!important;gap:10px!important}
#pmeQuickBar{grid-template-columns:repeat(3,minmax(0,1fr))!important}
#pmeSuiteNav{grid-template-columns:repeat(2,minmax(0,1fr))!important}
#pmeQuickBar button,#pmeSuiteNav button{width:100%!important;min-height:72px!important;white-space:normal!important;border-radius:16px!important;background:linear-gradient(180deg,#0b2b62 0%,#061936 58%,#030d1f 100%)!important;border:1px solid #168cff!important;color:#fff!important;font-size:15px!important;font-weight:850!important;letter-spacing:.01em!important;text-shadow:0 1px 2px #000!important;box-shadow:inset 0 0 18px rgba(0,132,255,.16),0 0 10px rgba(0,119,255,.14)!important}
#pmeQuickBar button span,#pmeSuiteNav button span{color:#fff!important}
#pmeOverlay{background:radial-gradient(circle at 50% 0,#0a2a63 0,#041126 30%,#020711 68%,#010307 100%)!important}
#pmeOverlay .pmeHead{background:rgba(2,8,18,.94)!important;backdrop-filter:blur(10px);border-bottom:1px solid rgba(32,199,255,.35)!important}
#pmeOverlay .pmeCard{background:linear-gradient(180deg,#0b2d64,#06152e 64%,#030b19)!important;border:1px solid #168cff!important;border-radius:18px!important;box-shadow:inset 0 0 22px rgba(0,119,255,.12),0 0 20px rgba(0,119,255,.13)!important;color:#fff!important}
#pmeOverlay button{background:linear-gradient(180deg,#0c55c9,#07317e)!important;border:1px solid #20c7ff!important;box-shadow:0 0 12px rgba(0,119,255,.18)!important}
#pmeOverlay .pmeTable th{color:#9fd9ff!important;background:#061a3a!important}#pmeOverlay .pmeTable td{color:#fff!important}
#pmeQuebecHero{display:flex;align-items:center;gap:12px;margin:10px 12px 2px;padding:14px 16px;border:1px solid rgba(32,199,255,.55);border-radius:18px;background:linear-gradient(135deg,rgba(8,51,112,.98),rgba(2,12,28,.98));box-shadow:inset 0 0 28px rgba(0,119,255,.14),0 0 24px rgba(0,92,255,.16)}
#pmeQuebecHero img{width:72px;height:72px;object-fit:contain;filter:drop-shadow(0 0 10px rgba(32,199,255,.55))}
#pmeQuebecHero .brandTitle{font-weight:950;font-size:24px;line-height:1;background:linear-gradient(180deg,#fff,#c9d6e8 52%,#34bfff);-webkit-background-clip:text;background-clip:text;color:transparent!important;text-transform:uppercase;letter-spacing:.02em}
#pmeQuebecHero .tag{font-size:12px;color:#b9d6f5;margin-top:5px;letter-spacing:.05em;text-transform:uppercase}
#pmeQuebecHero .fleur{font-size:20px;color:#43c7ff;text-shadow:0 0 12px #0af}
#pmeLegacyShortcutsHidden{display:none!important}
.sync-indicator,[class*="sync"]{border-color:rgba(31,202,255,.35)!important}
@media(max-width:700px){
  #pmeQuickBar{grid-template-columns:repeat(3,minmax(0,1fr))!important;padding:8px 8px!important;gap:7px!important}
  #pmeSuiteNav{grid-template-columns:repeat(2,minmax(0,1fr))!important;padding:0 8px 10px!important;gap:7px!important}
  #pmeQuickBar button,#pmeSuiteNav button{min-height:66px!important;padding:8px 6px!important;font-size:13px!important;border-radius:14px!important}
  #pmeQuebecHero{margin:8px 8px 2px;padding:12px}#pmeQuebecHero img{width:58px;height:58px}#pmeQuebecHero .brandTitle{font-size:20px}
  .card{border-radius:16px!important}
}
</style>
'''
SCRIPT=r'''
<script id="pme-quebec-visual-runtime-7000">
(function(){'use strict';
function hero(){
  if(document.getElementById('pmeQuebecHero')) return;
  var quick=document.getElementById('pmeQuickBar'); if(!quick) return;
  var h=document.createElement('section'); h.id='pmeQuebecHero';
  h.innerHTML='<img src="pme-brand-logo.png" alt="PME Québec"><div><div class="brandTitle">PME Québec</div><div class="tag">Gestion • Productivité • Croissance</div><div class="fleur">⚜ Fait au Québec</div></div>';
  quick.parentNode.insertBefore(h,quick);
}
function hideLegacy(){
  var nodes=[].slice.call(document.querySelectorAll('nav,section,div'));
  nodes.forEach(function(el){
    if(el.id==='pmeQuickBar'||el.id==='pmeSuiteNav'||el.closest('#pmeOverlay'))return;
    var txt=(el.innerText||'').replace(/\s+/g,' ').trim();
    if(txt.indexOf('Soumission')>=0&&txt.indexOf('Chargé de projet')>=0&&txt.indexOf('Marketing')>=0&&el.children.length<18){el.id='pmeLegacyShortcutsHidden'}
  });
}
function relabel(){
  var map={messages:'💬\nMessagerie',agenda:'📅\nAgenda',notifications:'🔔\nNotifications',management:'🏢\nCentre de gestion PME',office:'🗂️\nBureau',commerce:'🛒\nCommerce',documents:'📄\nDocuments'};
  document.querySelectorAll('[data-pme]').forEach(function(b){var k=b.getAttribute('data-pme');if(map[k]&&!b.dataset.visual7000){b.dataset.visual7000='1';var badge=b.querySelector('span');var badgeText=badge?badge.outerHTML:'';b.innerHTML=map[k].replace('\n','<br>')+badgeText;}})
}
function run(){hero();hideLegacy();relabel();}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run);else run();
new MutationObserver(run).observe(document.documentElement,{childList:true,subtree:true});
})();
</script>
'''
pos=s.lower().rfind('</head>')
if pos<0: raise SystemExit('Balise head introuvable')
s=s[:pos]+STYLE+s[pos:]
pos=s.lower().rfind('</body>')
if pos<0: raise SystemExit('Balise body introuvable')
s=s[:pos]+SCRIPT+s[pos:]
p.write_text(s,encoding='utf-8')
print('Visuel PME Québec bleu/chrome applique')
