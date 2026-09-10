from pathlib import Path

p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')

anchor='tabs.addView(tMap,new LinearLayout.LayoutParams(0,dp(50),1)); tabs.addView(tList,new LinearLayout.LayoutParams(0,dp(50),1)); tabs.addView(tFav,new LinearLayout.LayoutParams(0,dp(50),1)); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2); tp.topMargin=dp(8); page.addView(tabs,tp);'
if anchor not in s:
    raise SystemExit('ancrage onglets introuvable pour QUB radio')

radio=r'''tabs.addView(tMap,new LinearLayout.LayoutParams(0,dp(50),1)); tabs.addView(tList,new LinearLayout.LayoutParams(0,dp(50),1)); tabs.addView(tFav,new LinearLayout.LayoutParams(0,dp(50),1)); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2); tp.topMargin=dp(8); page.addView(tabs,tp);

        Button qub=ghostButton(QubPlaybackService.isActive()?"■  QUB RADIO • EN DIRECT":"▶  QUB RADIO • EN DIRECT");
        qub.setTextSize(13); qub.setAllCaps(false); qub.setOnClickListener(v->toggleQubRadio(qub));
        LinearLayout.LayoutParams qrp=new LinearLayout.LayoutParams(-1,dp(46)); qrp.topMargin=dp(7); page.addView(qub,qrp);'''
s=s.replace(anchor,radio,1)

marker='    private void addTools(){'
method=r'''    private void toggleQubRadio(Button button){
        Intent i=new Intent(this,QubPlaybackService.class);
        if(QubPlaybackService.isActive()){
            i.setAction(QubPlaybackService.ACTION_STOP);
            startService(i);
            button.setText("▶  QUB RADIO • EN DIRECT");
        }else{
            i.setAction(QubPlaybackService.ACTION_PLAY);
            if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
            button.setText("■  QUB RADIO • EN DIRECT");
            requestNotificationPermissionIfNeeded();
        }
    }

'''
if marker not in s:
    raise SystemExit('ancrage méthode QUB introuvable')
if 'private void toggleQubRadio(Button button)' not in s:
    s=s.replace(marker,method+marker,1)

required=['QUB RADIO • EN DIRECT','QubPlaybackService.isActive()','startForegroundService(i)','private void toggleQubRadio(Button button)']
missing=[x for x in required if x not in s]
if missing:
    raise SystemExit('QUB radio incomplet: '+repr(missing))

p.write_text(s,encoding='utf-8')
print('QUB radio: contrôle direct et lecture arrière-plan ajoutés')
