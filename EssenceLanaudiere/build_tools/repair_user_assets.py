from pathlib import Path
import zipfile, shutil
root=Path('EssenceLanaudiere/build_tools')
src=root/'user_assets.zip'
tmp=root/'user_assets_repaired.zip'
with zipfile.ZipFile(src,'r') as zin, zipfile.ZipFile(tmp,'w',compression=zipfile.ZIP_DEFLATED) as zout:
    for info in zin.infolist():
        if info.filename=='res/drawable/money_bundle.png':
            continue
        try:
            data=zin.read(info.filename)
        except Exception as e:
            raise SystemExit(f'Unexpected corrupt member {info.filename}: {e}')
        zout.writestr(info.filename,data)
    money='''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
  <item android:left="8dp" android:top="10dp" android:right="2dp" android:bottom="2dp"><shape android:shape="rectangle"><corners android:radius="8dp"/><solid android:color="#0D6B3F"/><stroke android:width="2dp" android:color="#65E6A8"/></shape></item>
  <item android:left="2dp" android:top="2dp" android:right="8dp" android:bottom="10dp"><shape android:shape="rectangle"><corners android:radius="8dp"/><solid android:color="#118C54"/><stroke android:width="2dp" android:color="#A9FFD0"/></shape></item>
</layer-list>'''
    zout.writestr('res/drawable/money_bundle.xml',money.encode('utf-8'))
shutil.move(tmp,src)
with zipfile.ZipFile(src,'r') as z:
    bad=z.testzip()
    if bad:
        raise SystemExit('Archive still corrupt: '+bad)
print('Asset archive repaired and verified')
