from pathlib import Path
import os
import runpy
root=Path(os.environ.get('GRADLE_DIR','source-5.10.4/PME-Quebec-5.10-Native-Wyze-Source'))
p=root/'app/src/main/assets/index.html'
s=p.read_text(encoding='utf-8')
if 'pme-employee-files-fix-6200' in s:
    print('Correctif dossier employés déjà présent')
else:
    STYLE='''\n<style id="pme-employee-files-fix-6200">\n/* Le dossier employés doit réellement afficher la gestion des employés. */\n#employeeFilesPage #adminEmployeeManagement{display:block!important;visibility:visible!important;opacity:1!important;max-height:none!important;overflow:visible!important}\n#employeeFilesPage #adminEmployeeManagement.hidden{display:block!important}\n#employeeFilesPage #v5EmployeeFilesHost:empty::after{content:'Chargement du dossier employés…';display:block;color:#9fb4d8;padding:16px}\n</style>\n'''
    SCRIPT='''\n<script id="pme-employee-files-runtime-fix-6200">\n(function(){\n'use strict';\nfunction repairEmployeeFiles(){\n  var page=document.getElementById('employeeFilesPage');\n  var host=document.getElementById('v5EmployeeFilesHost');\n  var panel=document.getElementById('adminEmployeeManagement');\n  if(!page||!host||!panel)return false;\n  if(panel.parentElement!==host)host.appendChild(panel);\n  panel.classList.remove('hidden');\n  panel.style.setProperty('display','block','important');\n  panel.style.setProperty('visibility','visible','important');\n  panel.style.setProperty('opacity','1','important');\n  try{ if(typeof renderEmployees==='function') renderEmployees(); }catch(e){console.warn('renderEmployees',e)}\n  try{ if(typeof renderEmployeeOptions==='function') renderEmployeeOptions(); }catch(e){}\n  return true;\n}\nfunction bindEmployeePage(){\n  document.addEventListener('click',function(ev){\n    var b=ev.target&&ev.target.closest?ev.target.closest('button'):null;\n    if(!b)return;\n    var txt=(b.textContent||'').toLowerCase();\n    if((b.getAttribute('onclick')||'').includes("employeeFilesPage") || txt.includes('dossier employés') || txt.includes('dossier employ')){\n      setTimeout(repairEmployeeFiles,0);\n      setTimeout(repairEmployeeFiles,120);\n    }\n  },true);\n}\nfunction boot(){repairEmployeeFiles();bindEmployeePage();new MutationObserver(function(){repairEmployeeFiles()}).observe(document.documentElement,{childList:true,subtree:true});}\nif(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();\nwindow.PME_REPAIR_EMPLOYEE_FILES=repairEmployeeFiles;\n})();\n</script>\n'''
    pos=s.lower().rfind('</head>')
    if pos<0: raise SystemExit('Balise head introuvable')
    s=s[:pos]+STYLE+s[pos:]
    pos=s.lower().rfind('</body>')
    if pos<0: raise SystemExit('Balise body introuvable')
    s=s[:pos]+SCRIPT+s[pos:]
    p.write_text(s,encoding='utf-8')
    print('Correctif dossier employés appliqué')

# Toujours appliquer ensuite le module Sécurité réseau et permissions étendues.
runpy.run_path(str(Path('.github/scripts/patch_security_network_scanner.py')), run_name='__main__')
# Puis stabiliser le retour au WebView après les boîtes de permissions Android.
runpy.run_path(str(Path('.github/scripts/patch_permission_flow_stability.py')), run_name='__main__')
# Enfin appliquer le branding INC Québec et un accès appareil local fiable.
runpy.run_path(str(Path('.github/scripts/patch_inc_quebec_brand_login.py')), run_name='__main__')
