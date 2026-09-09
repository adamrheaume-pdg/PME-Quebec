from pathlib import Path

java_path = Path('medias-quebec/app/src/main/java/quebec/medias/app/MainActivity.java')
html_path = Path('medias-quebec/app/src/main/assets/index.html')

java = java_path.read_text(encoding='utf-8')
html = html_path.read_text(encoding='utf-8')

# --- Native bridge for the 24/7 conflict ticker ---
bridge_anchor = '''        @JavascriptInterface\n        public String getKeywords() {\n            JSONArray arr = new JSONArray();\n            for (String keyword : KEYWORDS) arr.put(keyword);\n            return arr.toString();\n        }\n'''
bridge_repl = bridge_anchor + '''\n        @JavascriptInterface\n        public void loadTicker() {\n            executor.execute(() -> loadTickerWorker());\n        }\n'''
if 'public void loadTicker()' not in java:
    java = java.replace(bridge_anchor, bridge_repl)

worker_anchor = '''    private JSONArray fetchSection(String section) {\n'''
worker_code = '''    private void loadTickerWorker() {\n        String cached = readCache("TICKER");\n        if (cached != null && !cached.isEmpty()) sendTickerToJs(cached, isOnline() ? "cache" : "offline");\n        if (!isOnline()) {\n            if (cached == null || cached.isEmpty()) sendTickerToJs("[]", "offline-empty");\n            return;\n        }\n        try {\n            JSONArray items = fetchTicker();\n            writeCache("TICKER", items.toString());\n            sendTickerToJs(items.toString(), "online");\n        } catch (Exception e) {\n            if (cached == null || cached.isEmpty()) sendTickerToJs("[]", "error");\n        }\n    }\n\n    private JSONArray fetchTicker() {\n        List<FeedSpec> feeds = new ArrayList<>();\n        String conflicts = "(Ukraine OR Russie OR Gaza OR Palestine OR Israël OR Iran)";\n\n        // Québec / Canada francophone\n        feeds.add(new FeedSpec("Radio-Canada", gnews(conflicts + " site:ici.radio-canada.ca", "fr-CA", "CA", "CA:fr"), "Monde"));\n        feeds.add(new FeedSpec("La Presse", gnews(conflicts + " site:lapresse.ca", "fr-CA", "CA", "CA:fr"), "Monde"));\n        feeds.add(new FeedSpec("Le Devoir", gnews(conflicts + " site:ledevoir.com", "fr-CA", "CA", "CA:fr"), "Monde"));\n        feeds.add(new FeedSpec("TVA Nouvelles", gnews(conflicts + " site:tvanouvelles.ca", "fr-CA", "CA", "CA:fr"), "Monde"));\n        feeds.add(new FeedSpec("Noovo Info", gnews(conflicts + " site:noovo.info", "fr-CA", "CA", "CA:fr"), "Monde"));\n        feeds.add(new FeedSpec("Le Droit", gnews(conflicts + " site:ledroit.com", "fr-CA", "CA", "CA:fr"), "Monde"));\n\n        // Canada anglophone\n        feeds.add(new FeedSpec("CBC News", gnews("(Ukraine OR Russia OR Gaza OR Palestine OR Israel OR Iran) site:cbc.ca/news", "en-CA", "CA", "CA:en"), "World"));\n        feeds.add(new FeedSpec("CTV News", gnews("(Ukraine OR Russia OR Gaza OR Palestine OR Israel OR Iran) site:ctvnews.ca", "en-CA", "CA", "CA:en"), "World"));\n        feeds.add(new FeedSpec("Global News", gnews("(Ukraine OR Russia OR Gaza OR Palestine OR Israel OR Iran) site:globalnews.ca", "en-CA", "CA", "CA:en"), "World"));\n\n        // France\n        feeds.add(new FeedSpec("Franceinfo", gnews(conflicts + " site:francetvinfo.fr", "fr", "FR", "FR:fr"), "Monde"));\n        feeds.add(new FeedSpec("France 24", gnews(conflicts + " site:france24.com/fr", "fr", "FR", "FR:fr"), "Monde"));\n        feeds.add(new FeedSpec("RFI", gnews(conflicts + " site:rfi.fr/fr", "fr", "FR", "FR:fr"), "Monde"));\n        feeds.add(new FeedSpec("Le Monde", gnews(conflicts + " site:lemonde.fr", "fr", "FR", "FR:fr"), "Monde"));\n        feeds.add(new FeedSpec("Le Figaro", gnews(conflicts + " site:lefigaro.fr", "fr", "FR", "FR:fr"), "Monde"));\n\n        JSONArray result = new JSONArray();\n        Set<String> seen = new HashSet<>();\n        for (FeedSpec feed : feeds) {\n            try {\n                for (JSONObject item : fetchFeed(feed)) {\n                    String title = item.optString("title");\n                    String link = item.optString("link");\n                    String key = (link.isEmpty() ? title : link).trim().toLowerCase(Locale.ROOT);\n                    if (key.isEmpty() || seen.contains(key)) continue;\n                    String hay = (title + " " + item.optString("description")).toLowerCase(Locale.CANADA_FRENCH);\n                    String tag = "MONDE";\n                    if (hay.contains("ukraine") || hay.contains("russ")) tag = "UKRAINE";\n                    else if (hay.contains("gaza") || hay.contains("palestin") || hay.contains("isra")) tag = "PALESTINE";\n                    else if (hay.contains("iran")) tag = "IRAN";\n                    item.put("tickerTag", tag);\n                    seen.add(key);\n                    result.put(item);\n                    if (result.length() >= 80) return result;\n                }\n            } catch (Exception ignored) { }\n        }\n        return result;\n    }\n\n'''
if 'private JSONArray fetchTicker()' not in java:
    java = java.replace(worker_anchor, worker_code + worker_anchor)

send_anchor = '''    private void sendToJs(String section, String json, String mode) {\n        String script = "window.onNativeData(" + JSONObject.quote(section) + "," + json + "," + JSONObject.quote(mode) + ");";\n        runOnUiThread(() -> webView.evaluateJavascript(script, null));\n    }\n'''
send_repl = send_anchor + '''\n    private void sendTickerToJs(String json, String mode) {\n        String script = "window.onTickerData(" + json + "," + JSONObject.quote(mode) + ");";\n        runOnUiThread(() -> webView.evaluateJavascript(script, null));\n    }\n'''
if 'sendTickerToJs' not in java:
    java = java.replace(send_anchor, send_repl)

# --- Ticker UI ---
css_anchor = '</style>'
css = '''\n.ticker24{display:flex;align-items:center;height:42px;overflow:hidden;border-top:1px solid rgba(255,88,88,.35);border-bottom:1px solid rgba(255,88,88,.35);background:linear-gradient(90deg,#19050a,#07152d 42%,#07152d);box-shadow:0 4px 18px rgba(0,0,0,.25)}\n.tickerLabel{height:100%;display:flex;align-items:center;gap:7px;flex:0 0 auto;padding:0 12px;background:#c9182b;color:#fff;font-size:12px;font-weight:900;letter-spacing:.7px;z-index:2}.tickerDot{width:7px;height:7px;border-radius:50%;background:#fff;box-shadow:0 0 8px #fff}\n.tickerViewport{overflow:hidden;white-space:nowrap;flex:1}.tickerTrack{display:inline-flex;align-items:center;gap:28px;min-width:max-content;animation:tickerMove 75s linear infinite}.ticker24:active .tickerTrack{animation-play-state:paused}.tickerItem{display:inline-flex;align-items:center;gap:7px;color:#edf7ff;font-size:13px}.tickerTag{font-size:10px;font-weight:900;border:1px solid rgba(76,205,255,.35);border-radius:8px;padding:3px 5px;color:#50d0ff}.tickerSource{color:#9cb8d2;font-size:11px;font-weight:700}@keyframes tickerMove{from{transform:translateX(100vw)}to{transform:translateX(-100%)}}\n'''
if '.ticker24{' not in html:
    html = html.replace(css_anchor, css + css_anchor)

html_anchor = '''  </div>\n</header>'''
ticker_html = '''  </div>\n  <div class="ticker24" id="ticker24">\n    <div class="tickerLabel"><span class="tickerDot"></span>MONDE 24/7</div>\n    <div class="tickerViewport"><div class="tickerTrack" id="tickerTrack"><span class="tickerItem">Chargement Ukraine • Palestine • Iran…</span></div></div>\n  </div>\n</header>'''
if 'id="ticker24"' not in html:
    html = html.replace(html_anchor, ticker_html, 1)

script_anchor = '''window.onNativeData=function(section,items,mode){if(section!==currentSection||showingFav)return;currentItems=Array.isArray(items)?items:[];currentMode=mode;setStatus(mode);applyFilter()}\n'''
ticker_js = script_anchor + '''\nwindow.onTickerData=function(items,mode){\n const list=Array.isArray(items)?items:[]; const track=document.getElementById('tickerTrack'); if(!track)return;\n if(!list.length){track.innerHTML='<span class="tickerItem">Fil international temporairement indisponible</span>';return;}\n const clean=list.slice(0,60).map(it=>`<span class="tickerItem" onclick="Android&&Android.openArticle(${JSON.stringify(it.link||'')})"><span class="tickerTag">${escapeHtml(it.tickerTag||'MONDE')}</span><strong>${escapeHtml(it.title||'')}</strong><span class="tickerSource">${escapeHtml(it.source||'')}</span></span>`).join('<span class="tickerItem">◆</span>');\n track.innerHTML=clean+clean;\n}\nfunction loadTicker(){if(window.Android&&Android.loadTicker)Android.loadTicker()}\n'''
if 'window.onTickerData=' not in html:
    html = html.replace(script_anchor, ticker_js)

# Start immediately and refresh every 5 minutes.
if 'setInterval(loadTicker' not in html:
    html = html.replace("switchSection('QUEBEC');", "switchSection('QUEBEC');\nloadTicker();\nsetInterval(loadTicker,300000);")

java_path.write_text(java, encoding='utf-8')
html_path.write_text(html, encoding='utf-8')
print('Médias Québec ticker patch appliqué')
