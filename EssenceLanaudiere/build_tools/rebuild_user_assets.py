from pathlib import Path
import base64
import zipfile

root = Path('EssenceLanaudiere/build_tools')
parts = [root / f'user_assets_{i:02d}.txt' for i in range(9)]
missing = [str(p) for p in parts if not p.exists()]
if missing:
    raise SystemExit('Missing asset chunks: ' + ', '.join(missing))

encoded = ''.join(p.read_text(encoding='utf-8').strip() for p in parts)
raw = base64.b64decode(encoded, validate=True)
out = root / 'user_assets.zip'
out.write_bytes(raw)

with zipfile.ZipFile(out, 'r') as z:
    bad = z.testzip()
    if bad:
        raise SystemExit('Corrupt asset member: ' + bad)

print(f'Rebuilt {out} ({len(raw)} bytes)')
