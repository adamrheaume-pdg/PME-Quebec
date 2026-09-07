#!/usr/bin/env python3
import base64,zlib,json,subprocess,pathlib,re
from concurrent.futures import ThreadPoolExecutor, as_completed

ROOT=pathlib.Path(__file__).resolve().parents[2]
SOURCE=ROOT/'.github/scripts/resolve_youtube_defaults.py'
HTML=ROOT/'youtube-playlist-player/app/src/main/assets/index.html'
src=SOURCE.read_text(encoding='utf-8')
m=re.search(r"B64='([^']+)'",src)
if not m: raise SystemExit('Données des playlists introuvables')
TRACKS=json.loads(zlib.decompress(base64.b64decode(m.group(1))).decode())

def resolve(item):
    q=item['artist']+' '+item['title']
    for x in (q+' official',q):
        try:
            p=subprocess.run(['yt-dlp','--flat-playlist','--skip-download','--playlist-end','1','--print','%(id)s\t%(title)s','ytsearch1:'+x],capture_output=True,text=True,timeout=20)
            if p.returncode: continue
            line=next((s.strip() for s in p.stdout.splitlines() if s.strip()),'')
            if not line: continue
            parts=line.split('\t',1); vid=parts[0].strip(); title=parts[1].strip() if len(parts)>1 else ''
            if re.fullmatch(r'[A-Za-z0-9_-]{11}',vid):
                return vid,title
        except Exception:
            pass
    return None,None

flat=[]
for key,items in TRACKS.items():
    for pos,item in enumerate(items): flat.append((key,pos,item))
results={key:[None]*len(items) for key,items in TRACKS.items()}
ok=0
with ThreadPoolExecutor(max_workers=10) as ex:
    future_map={ex.submit(resolve,item):(key,pos,item) for key,pos,item in flat}
    for f in as_completed(future_map):
        key,pos,item=future_map[f]
        try: vid,found=f.result()
        except Exception: vid,found=None,None
        if vid: ok+=1
        results[key][pos]={'id':vid,'url':'https://www.youtube.com/watch?v='+vid if vid else None,'label':item['title'],'artist':item['artist'],'resolvedTitle':found}
        print(f'[{key} {pos+1:02d}/{len(TRACKS[key])}] '+(('OK '+vid) if vid else 'NON RESOLU')+' - '+item['artist']+' - '+item['title'],flush=True)

total=len(flat)
payload=json.dumps(results,ensure_ascii=False,separators=(',',':'))
runtime='''<script id="pme-default-playlists-v2">\nwindow.DEFAULT_PLAYLISTS=__PAYLOAD__;\n(function(){\nconst baseLoad=loadSelected,baseDelete=deleteSelected,baseRender=render,baseSave=savePlaylist,baseLoadCurrent=loadCurrent;\nwindow.refreshSaved=function(){const s=$("savedSelect"),d=saved(),c=s.value;s.innerHTML='<option value="">Choisir une playlist…</option>';[["default:global","🌎 Sélection originale",DEFAULT_PLAYLISTS.global],["default:quebec","⚜️ Québec 2000–2026",DEFAULT_PLAYLISTS.quebec]].forEach(([v,n,l])=>{const o=document.createElement("option");o.value=v;o.textContent=n+" ("+l.length+")";s.appendChild(o)});Object.keys(d).sort().forEach(k=>{const o=document.createElement("option");o.value="user:"+k;o.textContent="💾 "+k+" ("+d[k].length+")";s.appendChild(o)});if([...s.options].some(o=>o.value===c))s.value=c};\nwindow.loadSelected=function(){const v=$("savedSelect").value,d=saved();if(!v)return;if(v==="default:global"){tracks=JSON.parse(JSON.stringify(DEFAULT_PLAYLISTS.global));$("playlistName").value="Sélection originale"}else if(v==="default:quebec"){tracks=JSON.parse(JSON.stringify(DEFAULT_PLAYLISTS.quebec));$("playlistName").value="Québec 2000–2026"}else if(v.startsWith("user:")){const n=v.slice(5);if(!d[n])return;tracks=JSON.parse(JSON.stringify(d[n]));$("playlistName").value=n}else return baseLoad();index=0;render();if(ready&&tracks.length)loadCurrent(false)};\nwindow.deleteSelected=function(){const v=$("savedSelect").value;if(v&&v.startsWith("default:")){ $("importStatus").textContent="Les playlists intégrées ne peuvent pas être supprimées.";return }if(v&&v.startsWith("user:")){const n=v.slice(5),d=saved();delete d[n];writeSaved(d);$("importStatus").textContent="Playlist supprimée.";return}return baseDelete()};\nwindow.savePlaylist=function(){baseSave();const n=$("playlistName").value.trim();if(n)$("savedSelect").value="user:"+n};\nwindow.render=function(){baseRender();const n=tracks.filter(t=>t&&/^[A-Za-z0-9_-]{11}$/.test(t.id||"")).length;$("countLabel").textContent=tracks.length+" titre"+(tracks.length>1?"s":"")+(tracks.length?" • "+n+" lisible"+(n>1?"s":""):"")};\nwindow.loadCurrent=function(a=true){if(tracks.length&&(!tracks[index]||!/^[A-Za-z0-9_-]{11}$/.test(tracks[index].id||""))){let f=-1;for(let s=1;s<=tracks.length;s++){const j=(index+s)%tracks.length;if(tracks[j]&&/^[A-Za-z0-9_-]{11}$/.test(tracks[j].id||"")){f=j;break}}if(f<0){$("playerStatus").textContent="Aucune vidéo directe résolue dans cette playlist.";render();return}index=f}return baseLoadCurrent(a)};\nrefreshSaved();$("savedSelect").value="default:quebec";loadSelected();\n})();\n</script>'''.replace('__PAYLOAD__',payload)
s=HTML.read_text(encoding='utf-8')
s=re.sub(r'\n?<script id="pme-default-playlists-v2">.*?</script>\s*','\n',s,flags=re.S)
s=s.replace('</body>',runtime+'\n</body>')
HTML.write_text(s,encoding='utf-8')
print(f'Resolus: {ok}/{total}',flush=True)
if ok<120: raise SystemExit(f'Trop peu de titres resolus: {ok}/{total}')
