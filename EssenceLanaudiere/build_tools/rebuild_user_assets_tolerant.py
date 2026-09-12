from pathlib import Path
import base64, zipfile
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
encoded=''.join(p.read_text(encoding='utf-8').strip() for p in parts)
raw=base64.b64decode(encoded, validate=True)
out=root/'user_assets.zip'
out.write_bytes(raw)
with zipfile.ZipFile(out,'r') as z:
    names=set(z.namelist())
required={'assets/marker_mccafe.png','assets/marker_tim.png','assets/marker_atm.png','assets/marker_radar.png','res/drawable/essence_quebec_logo.png','res/drawable/station_shell.jpg','res/drawable/station_irving.jpg','res/drawable/station_esso.jpg','res/drawable/station_petro_canada.jpg','res/drawable/station_generic_pump.png','res/drawable/station_canadian_tire.jpg','res/drawable/station_harnois.jpg','res/drawable/station_costco.jpg','res/drawable/station_ultramar.jpg'}
missing_names=sorted(required-names)
if missing_names:
    raise SystemExit('Asset pack missing: '+', '.join(missing_names))
print(f'Rebuilt {out} ({len(raw)} bytes, {len(names)} entries)')
