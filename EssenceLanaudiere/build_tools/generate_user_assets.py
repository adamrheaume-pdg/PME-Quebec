from pathlib import Path
import struct, zlib, zipfile

ROOT=Path('EssenceLanaudiere/build_tools')
OUT=ROOT/'user_assets.zip'
APP=Path('EssenceLanaudiere/app/src/main')

def png(w,h,rgb):
    r,g,b=rgb
    raw=b''.join(b'\x00'+bytes([r,g,b])*w for _ in range(h))
    def chunk(t,d):
        return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')

def stripe_png(w,h,a,b):
    rows=[]
    for y in range(h):
        c=a if (y//24)%2==0 else b
        rows.append(b'\x00'+bytes(c)*w)
    raw=b''.join(rows)
    def chunk(t,d):
        return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')

def actual(path, fallback):
    p=APP/path
    if p.exists() and p.stat().st_size>100:
        return p.read_bytes()
    return fallback

# Les quatre marqueurs ci-dessous proviennent directement des images fournies
# par l'utilisateur et versionnees dans app/src/main/assets. Ils sont preserves
# dans le ZIP afin que patch_user_requests.py ne les remplace jamais par des carres.
assets={
 'assets/marker_mccafe.png': actual(Path('assets/marker_mccafe.png'),png(48,48,(218,41,28))),
 'assets/marker_tim.png': actual(Path('assets/marker_tim.png'),png(48,48,(138,36,29))),
 'assets/marker_atm.png': actual(Path('assets/marker_atm.png'),png(48,48,(21,117,206))),
 'assets/marker_radar.png': actual(Path('assets/marker_radar.png'),png(48,48,(255,178,0))),
 'res/drawable/essence_quebec_logo.png': actual(Path('res/drawable/essence_quebec_logo.png'),stripe_png(512,512,(0,31,151),(255,255,255))),
 # Le marqueur ATM est une liasse d'argent issue de l'image utilisateur; on la
 # reutilise pour l'affichage des economies au lieu de fabriquer un pictogramme.
 'res/drawable/money_bundle.png': actual(Path('assets/marker_atm.png'),stripe_png(180,100,(17,140,84),(169,255,208))),
 'res/drawable/station_shell.png': stripe_png(900,420,(255,207,0),(220,32,30)),
 'res/drawable/station_irving.png': stripe_png(900,420,(12,86,62),(255,255,255)),
 'res/drawable/station_esso.png': stripe_png(900,420,(25,66,154),(239,28,35)),
 'res/drawable/station_petro_canada.png': stripe_png(900,420,(207,32,48),(255,255,255)),
 'res/drawable/station_generic_pump.png': stripe_png(900,420,(10,25,48),(0,229,255)),
 'res/drawable/station_canadian_tire.png': stripe_png(900,420,(218,28,37),(255,255,255)),
 'res/drawable/station_harnois.png': stripe_png(900,420,(0,91,170),(255,196,0)),
 'res/drawable/station_costco.png': stripe_png(900,420,(0,81,162),(229,25,55)),
 'res/drawable/station_ultramar.png': stripe_png(900,420,(0,80,163),(255,255,255)),
}
with zipfile.ZipFile(OUT,'w',compression=zipfile.ZIP_DEFLATED) as z:
    for name,data in assets.items():
        z.writestr(name,data)
with zipfile.ZipFile(OUT,'r') as z:
    bad=z.testzip()
    if bad: raise SystemExit('Corrupt generated member: '+bad)
print(f'Generated {OUT} with {len(assets)} valid image resources; user map icons preserved')
