# FrancaisPME Québec

MVP Android local/offline pour aider les PME québécoises à organiser leur conformité linguistique.

## Fonctions incluses
- Parcours automatique selon le nombre d'employés au Québec.
- Questionnaire local pour le personnel.
- Calcul de la proportion des réponses indiquant une incapacité à communiquer en français au travail.
- Échéances et rappels Android.
- Registre simple des preuves/documents.
- Modèles de communications en français.
- Tableau de préparation interne.
- Liens vers l'OQLF et avertissement clair : outil non officiel.

## Cadre réglementaire intégré — vérifié le 13 septembre 2026
- 5 à 24 employés : déclaration au Registraire de la proportion du personnel qui n'est pas en mesure de communiquer en français au travail (mesure en vigueur depuis le 1er juin 2025).
- 25 employés ou plus pendant 6 mois : inscription et démarche de francisation auprès de l'OQLF selon la Charte.

Sources officielles :
- https://www.oqlf.gouv.qc.ca/charte/changementslegislatifs/
- https://www.oqlf.gouv.qc.ca/francisation/entreprises/

## Compilation
JDK 17 + Gradle 8.9 : `gradle assembleDebug`

APK : `app/build/outputs/apk/debug/app-debug.apk`.
