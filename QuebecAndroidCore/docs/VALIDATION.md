# Protocole de validation

## Niveaux

### EXPERIMENTAL
Le code existe mais aucune conclusion n'est permise sur sa fiabilité.

### BUILD_OK
La compilation a réussi. Cela ne prouve pas que l'écran, le bouton, la permission, la base de données ou le service fonctionne réellement.

### VALIDATED
Exiger au minimum :
- compilation réussie;
- installation réussie;
- lancement sans crash;
- navigation vers la fonction;
- action principale réellement exécutée;
- données conservées lorsque la fonction promet de la persistance;
- permissions acceptées/refusées sans blocage;
- aucune régression évidente sur les fonctions essentielles.

## Fiche obligatoire d'un composant validé

- nom/id;
- version;
- APK/projet source;
- fichiers à copier;
- dépendances Gradle;
- permissions Manifest;
- configuration requise;
- test effectué;
- appareil/API testé;
- résultat;
- problèmes connus;
- date de validation.

## Interdictions

Ne jamais marquer `VALIDATED` uniquement parce que Gradle retourne BUILD SUCCESSFUL.
Ne jamais réutiliser une clé API, un mot de passe ou un secret provenant d'une autre application.
Ne jamais supposer qu'une permission Android ancienne reste valide sur une version Android récente.
