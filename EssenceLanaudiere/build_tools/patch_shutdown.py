from pathlib import Path

p = Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s = p.read_text(encoding='utf-8')

if 'FERMER COMPLÈTEMENT L’APPLICATION' not in s:
    anchor = '        Button fav=toolButton("★  SUIS TES STATIONS FAVORITES"); fav.setOnClickListener(v->showFavorites()); grid.addView(fav); page.addView(grid);\n'
    repl = anchor + '        Button shutdown=toolButton("⏻  FERMER COMPLÈTEMENT L’APPLICATION"); shutdown.setOnClickListener(v->shutdownApp()); grid.addView(shutdown);\n'
    if anchor not in s:
        raise SystemExit('anchor addTools introuvable')
    s = s.replace(anchor, repl, 1)

if 'private void shutdownApp()' not in s:
    anchor = '    private void addTools(){\n'
    method = '''    private void shutdownApp(){\n        new AlertDialog.Builder(this)\n            .setTitle("Fermer Essence Québec")\n            .setMessage("Fermer complètement l’application et arrêter les activités de cette session ?")\n            .setNegativeButton("ANNULER", null)\n            .setPositiveButton("FERMER", (d,w) -> {\n                try {\n                    if(Build.VERSION.SDK_INT >= 21) finishAndRemoveTask();\n                    else finishAffinity();\n                } catch(Exception ignored) { finish(); }\n            })\n            .show();\n    }\n\n'''
    if anchor not in s:
        raise SystemExit('anchor methode introuvable')
    s = s.replace(anchor, method + anchor, 1)

p.write_text(s, encoding='utf-8')
print('shutdown patch applied')
