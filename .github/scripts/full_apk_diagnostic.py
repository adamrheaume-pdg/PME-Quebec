from pathlib import Path
import os,re,sys
root=Path(os.environ['GRADLE_DIR'])
html=root/'app/src/main/assets/index.html'
main_java=root/'app/src/main/java/com/pmequebec/app/MainActivity.java'
manifest=root/'app/src/main/AndroidManifest.xml'
issues=[]; warns=[]; checks=[]
def check(name,cond,detail=''):
    checks.append((name,bool(cond),detail))
    if not cond: issues.append(f'{name}: {detail}')
s=html.read_text(encoding='utf-8',errors='ignore') if html.exists() else ''
j=main_java.read_text(encoding='utf-8',errors='ignore') if main_java.exists() else ''
m=manifest.read_text(encoding='utf-8',errors='ignore') if manifest.exists() else ''
check('index.html présent',html.exists())
check('MainActivity.java présent',main_java.exists())
check('AndroidManifest.xml présent',manifest.exists())
check('Permission INTERNET','android.permission.INTERNET' in m)
check('Permission CAMERA','android.permission.CAMERA' in m)
check('JavaScript WebView activé','setJavaScriptEnabled(true)' in j)
check('DOM storage activé','setDomStorageEnabled(true)' in j)
check('WebChromeClient présent','WebChromeClient' in j)
check('Gestion permission caméra WebView','onPermissionRequest' in j and 'RESOURCE_VIDEO_CAPTURE' in j)
check('Supabase local','vendor/supabase-js.min.js' in s)
check('Scanner local','vendor/html5-qrcode.min.js' in s)
check('Auth gate','id="authGate"' in s)
check('Fermeture auth 5.11.0','pme-login-runtime-5110' in s and 'function closeGate' in s)
check('Navigation principale','mainNavDrawer' in s)
check('Inventaire','inventory' in s.lower())
check('Scanner','Html5Qrcode' in s or 'html5-qrcode' in s)
check('Safe areas','safe-area-inset-top' in s and 'safe-area-inset-bottom' in s)
check('Scroll mobile','overflow-y:auto' in s)
for rel in ['app/src/main/assets/vendor/html5-qrcode.min.js','app/src/main/assets/vendor/supabase-js.min.js']:
    check('Asset '+rel,(root/rel).exists())
for bad,label in [('https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2','CDN Supabase résiduel'),('https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js','CDN scanner résiduel')]:
    if bad in s: issues.append(label)
for idv in ['authGate','emailPasswordLoginBtn','authEmail','authPassword','mainNavDrawer']:
    n=len(re.findall(r'id=["\']'+re.escape(idv)+r'["\']',s))
    if n!=1: warns.append(f'ID critique {idv}: {n} occurrences')
check('Balises script équilibrées',s.lower().count('<script')==s.lower().count('</script>'),f"{s.lower().count('<script')} / {s.lower().count('</script>')}")
check('Balises style équilibrées',s.lower().count('<style')==s.lower().count('</style>'),f"{s.lower().count('<style')} / {s.lower().count('</style>')}")
print('=== DIAGNOSTIC COMPLET PME QUEBEC ===')
for name,passed,detail in checks: print(('PASS' if passed else 'FAIL'),'-',name,detail)
for w in warns: print('WARN -',w)
if issues:
    print('\nBLOQUANTS:')
    for x in issues: print('-',x)
    sys.exit(1)
print('\nDIAGNOSTIC STATIQUE: OK')
