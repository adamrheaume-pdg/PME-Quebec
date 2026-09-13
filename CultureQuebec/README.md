# Culture du Québec — Données Culture Québec

Application Android de préparation et de normalisation de catalogues culturels.

## Version 1.0.0

Fonctions :
- import CSV (virgule, point-virgule ou tabulation) et XLSX (première feuille);
- choix parmi 5 profils : MétaMusique, Arts de la scène, Cinéma, Livre — éditeurs, Exposition muséale;
- normalisation des en-têtes, espaces, langues, dates, villes et ISBN;
- détection de champs sectoriels suggérés absents, cellules vides et lignes dupliquées;
- aperçu avant/après;
- export CSV UTF-8 avec BOM;
- traitement local hors ligne;
- lien vers le programme officiel du Gouvernement du Québec.

## Important

Cette application n'est pas une application officielle du Gouvernement du Québec. Les profils intégrés servent au diagnostic et à la préparation des catalogues; ils ne constituent pas à eux seuls une certification de conformité aux normes sectorielles officielles.

## Compilation

```bash
gradle :app:assembleDebug
```

APK : `app/build/outputs/apk/debug/app-debug.apk`
