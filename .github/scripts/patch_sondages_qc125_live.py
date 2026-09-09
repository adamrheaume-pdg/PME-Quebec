from pathlib import Path

p = Path('SondagesQuebec2026/app/src/main/java/com/pmequebec/sondages2026/MainActivity.java')
s = p.read_text(encoding='utf-8')

# imports
if 'import android.os.Handler;' not in s:
    s = s.replace('import android.os.Bundle;\n', 'import android.os.Bundle;\nimport android.os.Handler;\nimport android.os.Looper;\n')

# add dedicated Qc125 button after live listener
anchor = '        live.setOnClickListener(v -> openLive());\n\n        addSummary(false);\n'
insert = '''        live.setOnClickListener(v -> openLive());\n\n        TextView qcTitle = text("QC125 — DONNÉES EN DIRECT", 18, NAVY, true);\n        qcTitle.setPadding(0, dp(16), 0, dp(6));\n        content.addView(qcTitle);\n        TextView qcInfo = text("Projection, sondages, régions, groupes démographiques et souveraineté directement depuis Qc125. La page se recharge automatiquement toutes les 5 minutes lorsqu’elle est ouverte.", 13, Color.DKGRAY, false);\n        qcInfo.setPadding(0, 0, 0, dp(8));\n        content.addView(qcInfo);\n        Button qc125 = button("OUVRIR QC125 — TABLEAUX EN DIRECT");\n        qc125.setOnClickListener(v -> openQc125());\n        content.addView(qc125, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));\n\n        addSummary(false);\n'''
if anchor in s:
    s = s.replace(anchor, insert)

# keep refresh replacement index correct after 3 new Qc125 views
s = s.replace('while (content.getChildCount() > 5) content.removeViewAt(5);', 'while (content.getChildCount() > 8) content.removeViewAt(8);')

# add Qc125 dialog before openLive
marker = '    private void openLive() {\n'
qc_method = '''    private void openQc125() {\n        Dialog d = new Dialog(this);\n        LinearLayout root = new LinearLayout(this);\n        root.setOrientation(LinearLayout.VERTICAL);\n        root.setBackgroundColor(Color.WHITE);\n\n        LinearLayout bar = new LinearLayout(this);\n        bar.setGravity(Gravity.CENTER_VERTICAL);\n        bar.setPadding(dp(10), dp(8), dp(10), dp(8));\n        bar.setBackgroundColor(NAVY);\n        TextView ttl = text("Qc125 — Québec 2026", 17, Color.WHITE, true);\n        Button close = button("FERMER");\n        bar.addView(ttl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));\n        bar.addView(close, new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));\n        root.addView(bar);\n\n        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);\n        LinearLayout tabs = new LinearLayout(this);\n        tabs.setOrientation(LinearLayout.HORIZONTAL);\n        tabs.setPadding(dp(6), dp(6), dp(6), dp(6));\n        tabsScroll.addView(tabs);\n        root.addView(tabsScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));\n\n        WebView web = new WebView(this);\n        WebSettings ws = web.getSettings();\n        ws.setJavaScriptEnabled(true);\n        ws.setDomStorageEnabled(true);\n        ws.setBuiltInZoomControls(true);\n        ws.setDisplayZoomControls(false);\n        ws.setLoadWithOverviewMode(true);\n        ws.setUseWideViewPort(true);\n        web.setWebViewClient(new WebViewClient());\n\n        String[][] links = {\n            {"PROJECTION", "https://qc125.com/"},\n            {"SONDAGES", "https://qc125.com/sondages.htm"},\n            {"DÉMOGRAPHIE", "https://qc125.com/sondages-demo.htm"},\n            {"SOUVERAINETÉ", "https://qc125.com/sondages-souv.htm"}\n        };\n        for (String[] item : links) {\n            Button b = button(item[0]);\n            b.setOnClickListener(v -> web.loadUrl(item[1]));\n            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(125), dp(44));\n            lp.setMargins(dp(3), 0, dp(3), 0);\n            tabs.addView(b, lp);\n        }\n\n        web.loadUrl("https://qc125.com/sondages.htm");\n        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));\n\n        TextView liveState = text("● Qc125 direct — actualisation automatique aux 5 min", 12, Color.rgb(24,120,74), true);\n        liveState.setPadding(dp(10), dp(6), dp(10), dp(8));\n        root.addView(liveState);\n\n        Handler handler = new Handler(Looper.getMainLooper());\n        Runnable reload = new Runnable() {\n            @Override public void run() {\n                if (d.isShowing()) {\n                    web.reload();\n                    String now = new SimpleDateFormat("HH:mm", Locale.CANADA_FRENCH).format(new Date());\n                    liveState.setText("● Qc125 direct — actualisé à " + now + " • prochain rechargement dans 5 min");\n                    handler.postDelayed(this, 5 * 60 * 1000L);\n                }\n            }\n        };\n\n        d.setContentView(root);\n        close.setOnClickListener(v -> d.dismiss());\n        d.setOnShowListener(x -> {\n            if (d.getWindow() != null) d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);\n            handler.postDelayed(reload, 5 * 60 * 1000L);\n        });\n        d.setOnDismissListener(x -> handler.removeCallbacks(reload));\n        d.show();\n    }\n\n'''
if marker in s and 'private void openQc125()' not in s:
    s = s.replace(marker, qc_method + marker)

# More truthful status wording: local chart remains cached, direct sources are live.
s = s.replace('● Données locales prêtes — dernière vague intégrée : 6 septembre 2026',
              '● Tableau local prêt • Qc125 et Flux direct = données web à jour')

p.write_text(s, encoding='utf-8')
print('Qc125 live section patched')
