#!/usr/bin/env python3
import json, pathlib, re, subprocess

ROOT = pathlib.Path(__file__).resolve().parents[2]
HTML = ROOT / 'musique-quebec/app/src/main/assets/index.html'
OLD_COMMIT = '7a3492843d47f34a57fa03c421e691232edd0404'
OLD_PATH = 'youtube-playlist-player/app/src/main/assets/index.html'


def balanced_json_after(src, marker):
    pos = src.index(marker) + len(marker)
    while pos < len(src) and src[pos].isspace(): pos += 1
    start = src.index('{', pos)
    depth = 0; string = False; esc = False
    for i in range(start, len(src)):
        c = src[i]
        if string:
            if esc: esc = False
            elif c == '\\': esc = True
            elif c == '"': string = False
        else:
            if c == '"': string = True
            elif c == '{': depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    return json.loads(src[start:i+1])
    raise RuntimeError('Objet JSON incomplet après ' + marker)

old = subprocess.check_output(['git','show',f'{OLD_COMMIT}:{OLD_PATH}'], cwd=ROOT, text=True)
base = balanced_json_after(old, 'window.DEFAULT_PLAYLISTS=')
wedding = balanced_json_after(old, 'const W=')
presets = {
    'global': base['global'],
    'quebec': base['quebec'],
    'wedding': wedding['wedding'],
    'wedding_qc': wedding['wedding_qc'],
}
assert [len(presets[k]) for k in ('global','quebec','wedding','wedding_qc')] == [80,80,100,20]

s = HTML.read_text(encoding='utf-8')
# Retirer une ancienne injection éventuelle.
s = re.sub(r'\n?<script id="mq-presets-v1">.*?</script>\s*', '\n', s, flags=re.S)

# Zone sûre sous la barre d'état Android. Le padding normal de 18 px est conservé en plus.
s = re.sub(r'\.app\{max-width:720px;margin:auto;padding:[^}]+\}',
           '.app{max-width:720px;margin:auto;padding:calc(54px + env(safe-area-inset-top, 0px)) 14px 40px}', s, count=1)
# Select visuellement identique aux champs.
s = s.replace('input,textarea,button{font:inherit}', 'input,textarea,select,button{font:inherit}')
s = s.replace('input,textarea{width:100%;', 'input,textarea,select{width:100%;')

old_input = '<input id="playlistName" placeholder="Nom de la playlist" autocomplete="off">'
new_input = '<div class="row"><input id="playlistName" placeholder="Nom de la playlist" autocomplete="off"><select id="savedSelect"><option value="">Choisir une playlist…</option></select></div>'
if old_input not in s:
    raise SystemExit('Champ playlistName introuvable')
s = s.replace(old_input, new_input, 1)

payload = json.dumps(presets, ensure_ascii=False, separators=(',', ':'))
js = r'''<script id="mq-presets-v1">
const MQ_DEFAULT_PLAYLISTS=__PAYLOAD__;
const MQ_DEFAULT_META={
 'default:global':['Sélection originale','global'],
 'default:quebec':['Québec 2000–2026','quebec'],
 'default:wedding':['Mariage — Top 100','wedding'],
 'default:wedding_qc':['Mariage québécois — 20 titres','wedding_qc']
};
const mqBaseSavePlaylist=savePlaylist;
const mqBaseLoadPlaylist=loadPlaylist;
function mqRefreshPlaylistSelect(){
 const sel=$('savedSelect'); if(!sel)return;
 const current=sel.value; sel.innerHTML='<option value="">Choisir une playlist…</option>';
 const defs=[
  ['default:global','🌎 Sélection originale',MQ_DEFAULT_PLAYLISTS.global],
  ['default:quebec','⚜️ Québec 2000–2026',MQ_DEFAULT_PLAYLISTS.quebec],
  ['default:wedding','💍 Mariage — Top 100',MQ_DEFAULT_PLAYLISTS.wedding],
  ['default:wedding_qc','⚜️💍 Mariage québécois — 20 titres',MQ_DEFAULT_PLAYLISTS.wedding_qc]
 ];
 defs.forEach(([v,n,l])=>{const o=document.createElement('option');o.value=v;o.textContent=n+' ('+l.length+')';sel.appendChild(o)});
 Object.keys(getSaved()).sort((a,b)=>a.localeCompare(b,'fr')).forEach(n=>{const o=document.createElement('option');o.value='user:'+n;o.textContent='💾 '+n+' ('+getSaved()[n].length+')';sel.appendChild(o)});
 if([...sel.options].some(o=>o.value===current))sel.value=current;
}
window.savePlaylist=function(){mqBaseSavePlaylist();mqRefreshPlaylistSelect();const n=$('playlistName').value.trim();if(n)$('savedSelect').value='user:'+n};
window.loadPlaylist=function(){
 const sel=$('savedSelect'), choice=sel?sel.value:'';
 if(MQ_DEFAULT_META[choice]){
   const [name,key]=MQ_DEFAULT_META[choice]; destroyPlayer();
   tracks=JSON.parse(JSON.stringify(MQ_DEFAULT_PLAYLISTS[key])); index=0;
   $('playlistName').value=name; render(); persistCurrent();
   $('importStatus').textContent='Playlist « '+name+' » chargée.';
   if(tracks.length) ensurePlayer(tracks[0].id,false); return;
 }
 if(choice&&choice.startsWith('user:')) $('playlistName').value=choice.slice(5);
 mqBaseLoadPlaylist();
};
mqRefreshPlaylistSelect();
const mqRestored = tracks.length>0;
if(!mqRestored){ $('savedSelect').value='default:quebec'; loadPlaylist(); }
else { const n=$('playlistName').value.trim(); if(n)$('savedSelect').value='user:'+n; }
</script>'''.replace('__PAYLOAD__', payload)

s = s.replace('</body>', js + '\n</body>')
HTML.write_text(s, encoding='utf-8')
print('Playlists restaurées:', {k:len(v) for k,v in presets.items()})
