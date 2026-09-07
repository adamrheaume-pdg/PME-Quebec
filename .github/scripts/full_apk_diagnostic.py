from pathlib import Path
from html.parser import HTMLParser
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

# Compter uniquement les vraies balises HTML. Un simple count('<script') est faux
# parce que l'application génère aussi des fragments HTML dans des template strings JS.
class StructureParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.script_open=0; self.script_close=0
        self.style_open=0; self.style_close=0
        self.script_depth=0; self.style_depth=0
        self.max_script_depth=0; self.max_style_depth=0
    def handle_starttag(self,tag,attrs):
        t=tag.lower()
        if t=='script':
            self.script_open+=1; self.script_depth+=1
            self.max_script_depth=max(self.max_script_depth,self.script_depth)
        elif t=='style':
            self.style_open+=1; self.style_depth+=1
            self.max_style_depth=max(self.max_style_depth,self.style_depth)
    def handle_endtag(self,tag):
        t=tag.lower()
        if t=='script':
            self.script_close+=1; self.script_depth=max(0,self.script_depth-1)
        elif t=='style':
            self.style_close+=1; self.style_depth=max(0,self.style_depth-1)

parser=StructureParser()
try:
    parser.feed(s); parser.close()
    parser_ok=True
except Exception as e:
    parser_ok=False
    issues.append(f'Parse HTML: {e}')

check('Parse HTML exécutable',parser_ok)
check('Balises script HTML équilibrées',parser.script_open==parser.script_close and parser.script_depth==0,f"{parser.script_open} / {parser.script_close}")
check('Balises style HTML équilibrées',parser.style_open==parser.style_close and parser.style_depth==0,f"{parser.style_open} / {parser.style_close}")
check('Pas de script imbriqué',parser.max_script_depth<=1,f"profondeur {parser.max_script_depth}")

print('=== DIAGNOSTIC COMPLET PME QUEBEC ===')
for name,passed,detail in checks: print(('PASS' if passed else 'FAIL'),'-',name,detail)
for w in warns: print('WARN -',w)
if issues:
    print('\nBLOQUANTS:')
    for x in issues: print('-',x)
    sys.exit(1)
print('\nDIAGNOSTIC STATIQUE: OK')
