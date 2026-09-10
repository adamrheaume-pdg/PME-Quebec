from pathlib import Path

html_path = Path('medias-quebec/app/src/main/assets/index.html')
html = html_path.read_text(encoding='utf-8')

# Styles
if '.radioPlayer{' not in html:
    html = html.replace('</style>', '''
.favoritesBtn{min-width:112px;display:flex;align-items:center;justify-content:center;gap:7px;font-size:14px;font-weight:800}.favoritesBtn b{min-width:22px;height:22px;display:inline-flex;align-items:center;justify-content:center;border-radius:11px;background:#0b6ed7;color:#fff;font-size:11px}.favoritesBtn.active{border-color:#ffd45a;color:#ffd45a}.favToast{position:fixed;left:50%;bottom:92px;transform:translateX(-50%) translateY(20px);opacity:0;pointer-events:none;z-index:99;background:#092443;border:1px solid rgba(55,200,255,.38);color:#fff;padding:9px 13px;border-radius:14px;font-size:12px;font-weight:800;transition:.2s}.favToast.show{opacity:1;transform:translateX(-50%) translateY(0)}
.radioPlayer{display:none;margin:0 0 13px;border:1px solid rgba(24,201,255,.28);border-radius:20px;background:linear-gradient(135deg,rgba(8,40,81,.98),rgba(4,20,43,.98));padding:13px 14px;box-shadow:0 10px 28px rgba(0,0,0,.2)}.radioPlayer.show{display:block}.radioHead{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:10px}.radioBrand{display:flex;align-items:center;gap:10px}.radioLiveDot{width:10px;height:10px;border-radius:50%;background:#ff375f;box-shadow:0 0 12px #ff375f}.radioTitle{font-size:16px;font-weight:900}.radioSub{font-size:11px;color:#9fb9d2;margin-top:2px}.radioPlayer audio{width:100%;height:42px}.radioOpen{margin-top:9px;border:1px solid rgba(64,180,255,.34);background:#08264a;color:#dff5ff;border-radius:12px;padding:8px 10px;font-size:12px;font-weight:800}
</style>''')

# Make the toolbar Favorites button explicit and visible.
html = html.replace('<button class="filterbtn" onclick="showFavorites()" title="Favoris">☆</button>', '<button id="favTopBtn" class="filterbtn favoritesBtn" onclick="showFavorites()" title="Ouvrir les favoris">★ <span>Favoris</span><b id="favCount">0</b></button>')

# QUB player, only shown in the live section.
if 'id="qubPlayer"' not in html:
    toolbar_end = '''  </div>\n  <div id="veilleInfo"'''
    player = '''  </div>\n  <section id="qubPlayer" class="radioPlayer" aria-label="Lecteur QUB radio en direct">\n    <div class="radioHead"><div class="radioBrand"><span class="radioLiveDot"></span><div><div class="radioTitle">QUB radio — EN DIRECT</div><div class="radioSub">Radio numérique québécoise • lecture en continu</div></div></div></div>\n    <audio id="qubAudio" controls preload="none" playsinline>\n      <source src="https://playerservices.streamtheworld.com/api/livestream-redirect/QUB_RADIO_SC" type="audio/mpeg">\n      <source src="https://playerservices.streamtheworld.com/api/livestream-redirect/QUB_RADIOAAC.aac?dist=" type="audio/aac">\n      Ton appareil ne peut pas lire ce flux audio.\n    </audio>\n    <button class="radioOpen" onclick="openItem('https://www.qub.ca/')">Ouvrir QUB si le flux est indisponible</button>\n  </section>\n  <div id="veilleInfo"'''
    html = html.replace(toolbar_end, player)

# Toast container.
if 'id="favToast"' not in html:
    html = html.replace('</nav>\n<script>', '</nav>\n<div id="favToast" class="favToast">Favori enregistré</div>\n<script>')

# Stronger favorites persistence + visual feedback.
old = "function saveFavs(){localStorage.setItem('mq_favorites',JSON.stringify(favorites))}\nfunction toggleFav(id){if(favorites[id]) delete favorites[id]; else {const it=currentItems.find(x=>itemId(x)===id); if(it) favorites[id]=it} saveFavs(); applyFilter()}"
new = """function updateFavCount(){const el=document.getElementById('favCount');if(el)el.textContent=Object.keys(favorites).length;const b=document.getElementById('favTopBtn');if(b)b.classList.toggle('active',showingFav)}\nfunction favToast(msg){const t=document.getElementById('favToast');if(!t)return;t.textContent=msg;t.classList.add('show');clearTimeout(window.__favToastTimer);window.__favToastTimer=setTimeout(()=>t.classList.remove('show'),1200)}\nfunction saveFavs(){try{localStorage.setItem('mq_favorites',JSON.stringify(favorites))}catch(e){}updateFavCount()}\nfunction toggleFav(id){const existed=!!favorites[id];if(existed){delete favorites[id]}else{const it=currentItems.find(x=>itemId(x)===id);if(it)favorites[id]=it}saveFavs();favToast(existed?'Retiré des favoris':'Ajouté aux favoris');if(showingFav){currentItems=Object.values(favorites)}applyFilter()}"""
if old in html:
    html = html.replace(old, new)

# QUB player visibility and top favorites button state.
old_switch = "document.getElementById('veilleInfo').style.display=section==='VEILLE'?'block':'none';document.getElementById('keywordStrip').style.display=section==='VEILLE'?'flex':'none';"
new_switch = old_switch + "\n const qp=document.getElementById('qubPlayer');if(qp)qp.classList.toggle('show',section==='LIVE');updateFavCount();"
if new_switch not in html and old_switch in html:
    html = html.replace(old_switch, new_switch)

# Favorites screen should update state and count immediately.
old_show = "function showFavorites(){showingFav=true;currentItems=Object.values(favorites);document.getElementById('title').textContent='Favoris hors ligne';document.getElementById('veilleInfo').style.display='none';document.getElementById('keywordStrip').style.display='none';document.querySelectorAll('.tab').forEach(b=>b.classList.remove('active'));document.querySelectorAll('.nav').forEach(n=>n.classList.remove('active'));document.getElementById('navFav').classList.add('active');setStatus('offline');applyFilter()}"
new_show = "function showFavorites(){showingFav=true;currentItems=Object.values(favorites);document.getElementById('title').textContent='Favoris';document.getElementById('veilleInfo').style.display='none';document.getElementById('keywordStrip').style.display='none';const qp=document.getElementById('qubPlayer');if(qp)qp.classList.remove('show');document.querySelectorAll('.tab').forEach(b=>b.classList.remove('active'));document.querySelectorAll('.nav').forEach(n=>n.classList.remove('active'));document.getElementById('navFav').classList.add('active');setStatus(currentMode||'offline');updateFavCount();applyFilter()}"
if old_show in html:
    html = html.replace(old_show, new_show)

# Initialize favorite count on load.
html = html.replace("window.addEventListener('load',()=>{setTimeout(()=>{loadKeywords();switchSection('QUEBEC')},120)});", "window.addEventListener('load',()=>{updateFavCount();setTimeout(()=>{loadKeywords();switchSection('QUEBEC')},120)});")

html_path.write_text(html, encoding='utf-8')
print('Favoris corrigés + lecteur QUB ajouté')
