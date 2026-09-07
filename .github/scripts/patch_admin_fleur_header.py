from pathlib import Path
import os

root = Path(os.environ.get("GRADLE_DIR", "source-5.10.4/PME-Quebec-5.10-Native-Wyze-Source"))
html_path = root / "app/src/main/assets/index.html"
html = html_path.read_text(encoding="utf-8")

STYLE_MARKER = "pme-admin-fleur-header-5130"
SCRIPT_MARKER = "pme-admin-fleur-runtime-5130"

style = r'''
<style id="pme-admin-fleur-header-5130">
/* Correctif mobile: entête et barre d'édition restent dans le flux normal. */
@media (max-width: 760px) {
  header {
    position: sticky !important;
    top: 0 !important;
    z-index: 900 !important;
    width: 100% !important;
    max-width: 100vw !important;
    padding: 14px 16px !important;
    padding-top: calc(14px + env(safe-area-inset-top, 0px)) !important;
    display: grid !important;
    grid-template-columns: 1fr !important;
    gap: 6px !important;
    overflow: visible !important;
  }
  header h1, header small {
    position: static !important;
    display: block !important;
    width: 100% !important;
    max-width: 100% !important;
    margin: 0 !important;
    transform: none !important;
    white-space: normal !important;
    overflow-wrap: anywhere !important;
  }
  header h1 { line-height: 1.2 !important; }
  header small { line-height: 1.35 !important; }
  .edit-toolbar {
    position: static !important;
    inset: auto !important;
    transform: none !important;
    width: 100% !important;
    max-width: 100% !important;
    margin: 0 0 14px !important;
  }
}

/* L'ancien lanceur GOD ne doit plus flotter à l'écran. */
.god-global-launcher { display: none !important; }

/* Nouveau lanceur discret, uniquement visible dans Administration. */
#pmeAdminFleurLauncher {
  position: fixed;
  right: 16px;
  bottom: calc(16px + env(safe-area-inset-bottom, 0px));
  z-index: 2147481901;
  width: 58px;
  height: 58px;
  border: 1px solid rgba(255,255,255,.28);
  border-radius: 50%;
  background: linear-gradient(145deg,#0b2f9a,#071d63);
  color: #fff;
  display: none;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 31px;
  line-height: 1;
  box-shadow: 0 8px 24px rgba(0,0,0,.35), inset 0 1px 0 rgba(255,255,255,.22);
}
#pmeAdminFleurLauncher.pme-show { display: flex !important; }
#pmeAdminFleurLauncher:active { transform: scale(.96); }
</style>
'''

script = r'''
<script id="pme-admin-fleur-runtime-5130">
(function(){
  'use strict';
  function visible(el){
    if(!el) return false;
    var s = getComputedStyle(el);
    if(s.display === 'none' || s.visibility === 'hidden' || s.opacity === '0') return false;
    var r = el.getBoundingClientRect();
    return r.width > 0 && r.height > 0;
  }
  function administrationVisible(){
    var selectors = [
      '#administration','#admin','#adminSection','#administrationSection',
      '[data-page="administration"]','[data-section="administration"]',
      '[id*="admin" i]','[class*="admin" i]'
    ];
    for(var i=0;i<selectors.length;i++){
      var nodes = document.querySelectorAll(selectors[i]);
      for(var j=0;j<nodes.length;j++) if(visible(nodes[j])) return true;
    }
    var heads = document.querySelectorAll('h1,h2,h3,.page-title,.section-title,[role="heading"]');
    for(var k=0;k<heads.length;k++){
      if(visible(heads[k]) && /administration/i.test((heads[k].textContent||'').trim())) return true;
    }
    return false;
  }
  function ensureFleur(){
    var btn = document.getElementById('pmeAdminFleurLauncher');
    if(!btn){
      btn = document.createElement('button');
      btn.id = 'pmeAdminFleurLauncher';
      btn.type = 'button';
      btn.setAttribute('aria-label','Outils avancés d’administration');
      btn.setAttribute('title','Administration');
      btn.textContent = '⚜';
      btn.addEventListener('click', function(){
        var legacy = document.querySelector('.god-global-launcher');
        if(legacy){ legacy.click(); return; }
        if(typeof window.openGodMode === 'function'){ window.openGodMode(); return; }
        if(typeof window.toggleGodMode === 'function'){ window.toggleGodMode(); }
      });
      document.body.appendChild(btn);
    }
    btn.classList.toggle('pme-show', administrationVisible());
  }
  function hideLegacy(){
    document.querySelectorAll('.god-global-launcher').forEach(function(el){
      el.style.setProperty('display','none','important');
      el.setAttribute('aria-hidden','true');
    });
  }
  function refresh(){ hideLegacy(); ensureFleur(); }
  if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', refresh);
  else refresh();
  var observer = new MutationObserver(function(){
    clearTimeout(window.__pmeAdminFleurTimer);
    window.__pmeAdminFleurTimer = setTimeout(refresh, 80);
  });
  observer.observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style','hidden','aria-hidden']});
  window.addEventListener('hashchange',refresh);
  window.addEventListener('popstate',refresh);
  setInterval(refresh,1200);
})();
</script>
'''

def insert_before_last(text, closing_tag, payload):
    pos = text.lower().rfind(closing_tag.lower())
    if pos < 0:
        raise SystemExit(f"Balise {closing_tag} introuvable")
    return text[:pos] + payload + "\n" + text[pos:]

# Important: utiliser la DERNIÈRE vraie balise de fermeture. Le fichier contient
# aussi des chaînes JavaScript/HTML qui peuvent inclure </head> ou </body>.
if STYLE_MARKER not in html:
    html = insert_before_last(html, "</head>", style)
if SCRIPT_MARKER not in html:
    html = insert_before_last(html, "</body>", script)

html_path.write_text(html, encoding="utf-8")
print("Correctif entête mobile + fleur de lys Administration appliqué sans corruption HTML.")
