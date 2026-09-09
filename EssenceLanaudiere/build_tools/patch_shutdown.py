from pathlib import Path

p = Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s = p.read_text(encoding='utf-8')

if 'NUCLEAR_SHUTDOWN_ARMED' not in s:
    anchor = '        Button fav=toolButton("★  SUIS TES STATIONS FAVORITES"); fav.setOnClickListener(v->showFavorites()); grid.addView(fav); page.addView(grid);\n'
    repl = anchor + '''        Button shutdown=toolButton("☢  ARRÊT TOTAL  ☢");
        shutdown.setTag("SAFE");
        shutdown.setAllCaps(false);
        shutdown.setTextSize(16);
        shutdown.setPadding(dp(18),dp(16),dp(18),dp(16));
        shutdown.setBackground(shutdownSafeBg());
        startNuclearIconBlink(shutdown);
        shutdown.setOnClickListener(v->handleShutdownButton(shutdown));
        grid.addView(shutdown);
'''
    if anchor not in s:
        raise SystemExit('anchor addTools introuvable')
    s = s.replace(anchor, repl, 1)

if 'private static final String NUCLEAR_SHUTDOWN_ARMED' not in s:
    anchor = '    private void addTools(){\n'
    method = '''    private static final String NUCLEAR_SHUTDOWN_ARMED = "ARMED";

    private GradientDrawable shutdownSafeBg(){
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(32,44,68));
        g.setCornerRadius(dp(22));
        g.setStroke(dp(2),Color.rgb(120,150,190));
        return g;
    }

    private GradientDrawable shutdownArmedBg(){
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(178,28,28));
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(dp(4),Color.rgb(255,210,70));
        return g;
    }

    private void startNuclearIconBlink(Button b){
        final int[] colors={Color.rgb(255,220,45),Color.WHITE,Color.rgb(70,235,110)};
        final int[] step={0};
        Runnable blink=new Runnable(){
            @Override public void run(){
                if(b.getWindowToken()==null){ b.postDelayed(this,520); return; }
                b.setTextColor(colors[step[0]%colors.length]);
                step[0]++;
                b.postDelayed(this,520);
            }
        };
        b.post(blink);
    }

    private void handleShutdownButton(Button b){
        Object state=b.getTag();
        if(!NUCLEAR_SHUTDOWN_ARMED.equals(state)){
            b.setTag(NUCLEAR_SHUTDOWN_ARMED);
            b.setText("☢  ARMÉ — APPUIE ENCORE  ☢");
            b.setBackground(shutdownArmedBg());
            b.getLayoutParams().height=dp(92);
            b.requestLayout();
            b.animate().scaleX(1.06f).scaleY(1.06f).setDuration(140).withEndAction(() ->
                b.animate().scaleX(1f).scaleY(1f).setDuration(140).start()).start();
            b.postDelayed(() -> {
                if(NUCLEAR_SHUTDOWN_ARMED.equals(b.getTag())){
                    b.setTag("SAFE");
                    b.setText("☢  ARRÊT TOTAL  ☢");
                    b.setBackground(shutdownSafeBg());
                    b.getLayoutParams().height=android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                    b.requestLayout();
                }
            },5000);
            return;
        }
        b.setText("☢  EXTINCTION…  ☢");
        b.setEnabled(false);
        b.postDelayed(this::shutdownApp,220);
    }

    private void shutdownApp(){
        try {
            if(Build.VERSION.SDK_INT >= 21) finishAndRemoveTask();
            else finishAffinity();
        } catch(Exception ignored) { finish(); }
    }

'''
    if anchor not in s:
        raise SystemExit('anchor methode introuvable')
    s = s.replace(anchor, method + anchor, 1)

p.write_text(s, encoding='utf-8')
print('shutdown patch applied with blinking nuclear icons')
