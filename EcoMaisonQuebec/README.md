# ÉcoMaison Québec V0.1
Prototype Android natif pour suivi d'économie d'énergie résidentielle au Québec.

Fonctions V0.1 :
- profil de maison;
- sélection tarif Hydro-Québec D, Flex D, DT, DP, DM, DN;
- calcul superficie, volume et densité de chauffage installée W/pi²;
- gestes d'économie d'énergie;
- structure d'alertes et d'ÉcoScore.

Compilation locale : Android SDK 35 + Gradle 8.10.2 + JDK 17, puis `gradle assembleDebug`.
Le workflow GitHub Actions inclus compile automatiquement l'APK.
