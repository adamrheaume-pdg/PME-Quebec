from pathlib import Path
p=Path('EssenceLanaudiere/app/src/main/java/quebec/lanaudiere/essence/MainActivity.java')
s=p.read_text(encoding='utf-8')
field='    private int introPage=0;'
if 'eqAutoSyncHandler' not in s:
    add='''    private final Handler eqAutoSyncHandler=new Handler(Looper.getMainLooper());\n    private boolean eqAutoSyncEnabled=false;\n    private final Runnable eqAutoSyncTask=new Runnable(){@Override public void run(){if(!eqAutoSyncEnabled)return;if(hero!=null&&hasLocationPermission())locateAndLoad();eqAutoSyncHandler.postDelayed(this,180000L);}};'''
    if field not in s: raise SystemExit('champ introPage introuvable')
    s=s.replace(field,field+'\n'+add,1)
anchor='    private void showIntro(){'
if 'private void startEqAutoSync()' not in s:
    add='''    private void startEqAutoSync(){eqAutoSyncEnabled=true;eqAutoSyncHandler.removeCallbacks(eqAutoSyncTask);eqAutoSyncHandler.postDelayed(eqAutoSyncTask,180000L);}\n    private void stopEqAutoSync(){eqAutoSyncEnabled=false;eqAutoSyncHandler.removeCallbacks(eqAutoSyncTask);}\n    @Override protected void onResume(){super.onResume();if(hero!=null){startEqAutoSync();if(hasLocationPermission())locateAndLoad();}}\n    @Override protected void onPause(){stopEqAutoSync();super.onPause();}\n\n'''
    if anchor not in s: raise SystemExit('showIntro introuvable')
    s=s.replace(anchor,add+anchor,1)
if 'requestPermissionsAndLocate();\n        startEqAutoSync();' not in s:
    s=s.replace('        requestPermissionsAndLocate();\n    }','        requestPermissionsAndLocate();\n        startEqAutoSync();\n    }',1)
old='HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);'
new='HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setUseCaches(false);c.setRequestProperty("Cache-Control","no-cache, no-store, max-age=0");c.setRequestProperty("Pragma","no-cache");c.setConnectTimeout(10000);c.setReadTimeout(10000);'
if 'no-cache, no-store, max-age=0' not in s:
    if old not in s: raise SystemExit('connexion stations introuvable')
    s=s.replace(old,new,1)
p.write_text(s,encoding='utf-8')
for x in ['eqAutoSyncHandler','180000L','no-cache, no-store, max-age=0','startEqAutoSync()']:
    if x not in s: raise SystemExit('manque '+x)
print('v1.8.9 auto sync OK')
