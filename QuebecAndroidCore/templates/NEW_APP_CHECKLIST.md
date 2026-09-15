# Nouvelle APK — checklist

## 1. Identité
- Nom de l'application
- applicationId unique
- versionName / versionCode
- icône et splash

## 2. Sélection du Core
N'utiliser en priorité que les modules `VALIDATED` du registre.

## 3. Configuration Android
- compileSdk/targetSdk/minSdk cohérents
- Java/Kotlin et Gradle compatibles
- permissions minimales
- gestion status bar/navigation bar
- stockage Android moderne

## 4. Fonctions
Pour chaque fonction : écran accessible, contrôles actifs, action réelle, état erreur/vide/chargement, persistance si nécessaire.

## 5. Tests avant livraison
- build
- installation
- lancement
- navigation complète
- boutons principaux
- saisie/formulaires
- retour Android
- rotation si supportée
- hors-ligne si applicable
- permissions refusées puis accordées
- redémarrage de l'application

## 6. Livraison
Ne jamais annoncer « fonctionnel » sur la seule base de la compilation. Indiquer séparément : BUILD, INSTALL, RUNTIME et FUNCTIONAL TESTS.
