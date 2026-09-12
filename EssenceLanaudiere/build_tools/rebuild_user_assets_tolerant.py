from pathlib import Path
import base64, zipfile, re

root=Path('EssenceLanaudiere/build_tools')
parts=[]
for i in range(9):
    if i==6:
        parts.extend(root / f'user_assets_06_{j}.txt' for j in range(6))
    else:
        parts.append(root / f'user_assets_{i:02d}.txt')
missing=[str(p) for p in parts if not p.exists()]
if missing:
    raise SystemExit('Missing chunks: '+', '.join(missing))

# Chaque morceau peut avoir son propre padding base64. On enlève ce padding
# intermédiaire avant de concaténer, puis on repadde seulement la chaîne finale.
chunks=[]
for p in parts:
    x=re.sub(r'[^A-Za-z0-9+/=]','',p.read_text(encoding='utf-8'))
    chunks.append(x.rstrip('='))
encoded=''.join(chunks)
encoded += '=' * ((4 - len(encoded) % 4) % 4)
raw=base64.b64decode(encoded, validate=False)
out=root/'user_assets.zip'
out.write_bytes(raw)

try:
    with zipfile.ZipFile(out,'r') as z:
        names=set(z.namelist())
        bad=z.testzip()
        if bad:
            raise SystemExit('Corrupt member: '+bad)
except zipfile.BadZipFile as e:
    raise SystemExit('Rebuilt archive is not a valid ZIP: '+str(e))

required={
 'assets/marker_mccafe.png','assets/marker_tim.png','assets/marker_atm.png','assets/marker_radar.png',
 'res/drawable/essence_quebec_logo.png','res/drawable/money_bundle.png',
 'res/drawable/station_shell.jpg','res/drawable/station_irving.jpg','res/drawable/station_esso.jpg',
 'res/drawable/station_petro_canada.jpg','res/drawable/station_generic_pump.png',
 'res/drawable/station_canadian_tire.jpg','res/drawable/station_harnois.jpg',
 'res/drawable/station_costco.jpg','res/drawable/station_ultramar.jpg'
}
missing_names=sorted(required-names)
if missing_names:
    raise SystemExit('Asset pack missing: '+', '.join(missing_names))
print(f'Rebuilt {out} ({len(raw)} bytes, {len(names)} entries)')
