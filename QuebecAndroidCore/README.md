# Quebec Android Core

Bibliothèque de référence pour réutiliser les composants Android éprouvés dans les applications Québec.

## Règle principale

**BUILD OK != APPLICATION OK**

Un composant ne devient `VALIDATED` qu'après compilation ET vérification fonctionnelle sur appareil/émulateur.

## Statuts

- `EXPERIMENTAL` : code en développement, aucune garantie.
- `BUILD_OK` : compilation Android réussie, fonctionnement réel non confirmé.
- `VALIDATED` : compilation + test fonctionnel confirmés.
- `DEPRECATED` : ne plus réutiliser.

## Organisation

- `registry/` : registre des composants et de leur niveau de validation.
- `docs/` : règles d'architecture, validation et réutilisation.
- `templates/` : patrons pour démarrer une nouvelle application.
- `modules/` : emplacement des composants réutilisables après extraction/validation.

## Familles prévues

Core : UI, navigation, stockage, réseau, permissions, fichiers, diagnostic.

Features : caméra, QR/code-barres, localisation, cartes, PDF, partage, Supabase.

Métier : clients, employés, fournisseurs, inventaire, commandes, taxes et tableaux de bord.

## Principe de réutilisation

1. Chercher d'abord un composant `VALIDATED`.
2. Le copier/importer avec ses dépendances et permissions documentées.
3. Ne modifier que ce qui est propre à la nouvelle APK.
4. Compiler.
5. Tester les fonctions essentielles.
6. Promouvoir le composant dans le registre seulement après validation.

Ce dossier est créé dans une branche dédiée afin de ne pas perturber PME Québec pendant la construction du Core.