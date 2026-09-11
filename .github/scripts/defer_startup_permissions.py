from pathlib import Path
import os

root = Path(os.environ['GRADLE_DIR'])
p = root / 'app/src/main/java/com/pmequebec/app/MainActivity.java'
s = p.read_text(encoding='utf-8')

# Ne pas interrompre le chargement du portail au lancement.
s = s.replace('        requestInitialPermissions();\n', '')

if 'requestInitialPermissions();' in s:
    raise SystemExit('La demande globale au démarrage est encore appelée')

p.write_text(s, encoding='utf-8')
print('Démarrage sans demande globale: autorisations déclenchées par les fonctions qui en ont besoin')
