from pathlib import Path

p = Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s = p.read_text(encoding='utf-8')

if 'NUCLEAR_SHUTDOWN_ARMED' not in s:
    anchor = '        Button fav=toolButton("★  SUIS TES STATIONS FAVORITES"); fav.setOnClickListener(v->showFavorites()); grid.addView(fav); page.addView(grid);\n'
    repl = anchor + '''        Button shutdown=toolButton("☢  ARRÊT TOTAL");\n        shutdown.setTag("SAFE");\n        shutdown.setAllCaps(false);\n        shutdown.setTextSize(16);\n        shutdown.setPadding(dp(18),dp(16),dp(18),dp(16));\n        shutdown.setBackground(shutdownSafeBg());\n        shutdown.setOnClickListener(v->handleShutdownButton(shutdown));\n        grid.addView(shutdown);\n'''
    if anchor not in s:
        raise SystemExit('anchor addTools introuvable')
    s = s.replace(anchor, repl, 1)

if 'private static final String NUCLEAR_SHUTDOWN_ARMED' not in s:
    anchor = '    private void addTools(){\n'
    method = '''    private static final String NUCLEAR_SHUTDOWN_ARMED = "ARMED";\n\n    private GradientDrawable shutdownSafeBg(){\n        GradientDrawable g=new GradientDrawable();\n        g.setColor(Color.rgb(32,44,68));\n        g.setCornerRadius(dp(22));\n        g.setStroke(dp(2),Color.rgb(120,150,190));\n        return g;\n    }\n\n    private GradientDrawable shutdownArmedBg(){\n        GradientDrawable g=new GradientDrawable();\n        g.setColor(Color.rgb(178,28,28));\n        g.setShape(GradientDrawable.OVAL);\n        g.setStroke(dp(4),Color.rgb(255,210,70));\n        return g;\n    }\n\n    private void handleShutdownButton(Button b){\n        Object state=b.getTag();\n        if(!NUCLEAR_SHUTDOWN_ARMED.equals(state)){\n            b.setTag(NUCLEAR_SHUTDOWN_ARMED);\n            b.setText("☢  ARMÉ — APPUIE ENCORE");\n            b.setTextColor(Color.WHITE);\n            b.setBackground(shutdownArmedBg());\n            b.getLayoutParams().height=dp(92);\n            b.requestLayout();\n            b.animate().scaleX(1.06f).scaleY(1.06f).setDuration(140).withEndAction(() ->\n                b.animate().scaleX(1f).scaleY(1f).setDuration(140).start()).start();\n            b.postDelayed(() -> {\n                if(NUCLEAR_SHUTDOWN_ARMED.equals(b.getTag())){\n                    b.setTag("SAFE");\n                    b.setText("☢  ARRÊT TOTAL");\n                    b.setBackground(shutdownSafeBg());\n                    b.getLayoutParams().height=android.view.ViewGroup.LayoutParams.WRAP_CONTENT;\n                    b.requestLayout();\n                }\n            },5000);\n            return;\n        }\n        b.setText("⏻  EXTINCTION…");\n        b.setEnabled(false);\n        b.postDelayed(this::shutdownApp,220);\n    }\n\n    private void shutdownApp(){\n        try {\n            if(Build.VERSION.SDK_INT >= 21) finishAndRemoveTask();\n            else finishAffinity();\n        } catch(Exception ignored) { finish(); }\n    }\n\n'''
    if anchor not in s:
        raise SystemExit('anchor methode introuvable')
    s = s.replace(anchor, method + anchor, 1)

p.write_text(s, encoding='utf-8')
print('shutdown patch applied')
