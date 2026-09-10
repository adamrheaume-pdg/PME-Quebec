#!/usr/bin/env python3
import base64, json, pathlib, re, subprocess, zlib
from concurrent.futures import ThreadPoolExecutor, as_completed

ROOT = pathlib.Path(__file__).resolve().parents[2]
HTML = ROOT / 'musique-quebec/app/src/main/assets/index.html'


def read_branch_script(path):
    refs = ['origin/musique-quebec-fix', 'musique-quebec-fix']
    for ref in refs:
        try:
            return subprocess.check_output(['git','show',f'{ref}:{path}'], cwd=ROOT, text=True)
        except Exception:
            pass
    raise RuntimeError('Impossible de lire ' + path + ' dans musique-quebec-fix')


def decode_tracks(script_text):
    m = re.search(r"B64='([^']+)'", script_text)
    if not m:
        raise RuntimeError('Bloc B64 introuvable')
    return json.loads(zlib.decompress(base64.b64decode(m.group(1))).decode('utf-8'))

base_titles = decode_tracks(read_branch_script('.github/scripts/resolve_youtube_defaults.py'))
wedding_titles = decode_tracks(read_branch_script('.github/scripts/add_wedding_playlists.py'))
source = {
    'global': base_titles['global'],
    'quebec': base_titles['quebec'],
    'wedding': wedding_titles['wedding'],
    'wedding_qc': wedding_titles['wedding_qc'],
}


def resolve(item):
    artist = item.get('artist','').strip()
    title = item.get('title','').strip()
    q = (artist + ' ' + title).strip()
    for query in (q + ' official', q):
        try:
            p = subprocess.run([
                'yt-dlp','--flat-playlist','--skip-download','--playlist-end','1',
                '--print','%(id)s\t%(title)s','ytsearch1:' + query
            ], capture_output=True, text=True, timeout=30)
            if p.returncode != 0:
                continue
            line = next((x.strip() for x in p.stdout.splitlines() if x.strip()), '')
            if not line:
                continue
            parts = line.split('\t',1)
            vid = parts[0].strip()
            found = parts[1].strip() if len(parts)>1 else title
            if re.fullmatch(r'[A-Za-z0-9_-]{11}', vid):
                return {'id':vid,'url':'https://www.youtube.com/watch?v='+vid,'label':title,'artist':artist,'resolvedTitle':found}
        except Exception:
            pass
    return None

resolved = {k:[None]*len(v) for k,v in source.items()}
work=[]
with ThreadPoolExecutor(max_workers=12) as ex:
    for key, items in source.items():
        for i,item in enumerate(items):
            work.append((ex.submit(resolve,item),key,i,item))
    done=0
    for fut,key,i,item in work:
        r=fut.result()
        resolved[key][i]=r
        done += 1
        print(f'[{done}/280] {key} {i+1}: ' + (r['id'] if r else 'NON RESOLU'), flush=True)

# Garder seulement les entrées jouables; les listes restent préformatées avec URL directes.
resolved = {k:[x for x in v if x] for k,v in resolved.items()}
counts={k:len(v) for k,v in resolved.items()}
print('Résolution terminée:',counts,flush=True)
if counts['global'] < 60 or counts['quebec'] < 60 or counts['wedding'] < 75 or counts['wedding_qc'] < 15:
    raise SystemExit('Trop peu de liens YouTube résolus: '+str(counts))

s = HTML.read_text(encoding='utf-8')
# Retirer une ancienne injection de presets si présente.
s = re.sub(r'\n?<script id="mq-presets-v2">.*?</script>\s*', '\n', s, flags=re.S)

# Zone sûre: éviter que le titre/logo se superposent à la barre d'état Android.
s = re.sub(r'\.app\{max-width:720px;margin:auto;padding:[^}]+\}',
           '.app{max-width:720px;margin:auto;padding:calc(64px + env(safe-area-inset-top,0px)) 14px 40px}', s, count=1)

# Ajouter un sélecteur de playlists intégrées/sauvegardées.
old = '<input id="playlistName" placeholder="Nom de la playlist" autocomplete="off">'
new = '<div class="row"><input id="playlistName" placeholder="Nom de la playlist" autocomplete="off"><select id="savedSelect" style="width:100%;background:#0f131a;color:#fff;border:1px solid #303847;border-radius:12px;padding:11px"><option value="">Choisir une playlist…</option></select></div>'
if old in s:
    s = s.replace(old,new,1)

payload=json.dumps(resolved,ensure_ascii=False,separators=(',',':'))
js=r'''<script id="mq-presets-v2">
const MQ_PRESETS=__PAYLOAD__;
const MQ_META={
 'default:global':['Sélection originale','global'],
 'default:quebec':['Québec 2000–2026','quebec'],
 'default:wedding':['Mariage — Top 100','wedding'],
 'default:wedding_qc':['Mariage québécois — 20 titres','wedding_qc']
};
function mqRefreshSelect(){
 const sel=$('savedSelect'); if(!sel)return;
 const cur=sel.value; sel.innerHTML='<option value="">Choisir une playlist…</option>';
 const defs=[['default:global','🌎 Sélection originale','global'],['default:quebec','⚜️ Québec 2000–2026','quebec'],['default:wedding','💍 Mariage — Top 100','wedding'],['default:wedding_qc','⚜️💍 Mariage québécois — 20 titres','wedding_qc']];
 defs.forEach(([v,n,k])=>{const o=document.createElement('option');o.value=v;o.textContent=n+' ('+MQ_PRESETS[k].length+')';sel.appendChild(o)});
 const d=getSaved();Object.keys(d).sort((a,b)=>a.localeCompare(b,'fr')).forEach(n=>{const o=document.createElement('option');o.value='user:'+n;o.textContent='💾 '+n+' ('+d[n].length+')';sel.appendChild(o)});
 if([...sel.options].some(o=>o.value===cur))sel.value=cur;
}
const mqBaseSave=savePlaylist;
window.savePlaylist=function(){mqBaseSave();mqRefreshSelect();const n=$('playlistName').value.trim();if(n)$('savedSelect').value='user:'+n};
const mqBaseLoad=loadPlaylist;
window.loadPlaylist=function(){
 const sel=$('savedSelect'),v=sel?sel.value:'';
 if(MQ_META[v]){const [name,key]=MQ_META[v];destroyPlayer();tracks=JSON.parse(JSON.stringify(MQ_PRESETS[key]));index=0;$('playlistName').value=name;render();persistCurrent();$('importStatus').textContent='Playlist « '+name+' » chargée.';return}
 if(v&&v.startsWith('user:'))$('playlistName').value=v.slice(5);
 mqBaseLoad();
};
mqRefreshSelect();
if(!tracks.length){$('savedSelect').value='default:quebec';loadPlaylist();}else{const n=$('playlistName').value.trim();if(n)$('savedSelect').value='user:'+n;}
</script>'''.replace('__PAYLOAD__',payload)
s=s.replace('</body>',js+'\n</body>')
HTML.write_text(s,encoding='utf-8')
print('Patch Musique Québec appliqué avec zone sûre et playlists.',flush=True)
