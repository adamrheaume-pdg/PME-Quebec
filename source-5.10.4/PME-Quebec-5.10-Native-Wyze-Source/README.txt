PME Québec 5.10.1 — moteur Wyze ARM64 intégrable

Ce projet Android Studio reconstruit la couche Android native autour de l’interface PME Québec et branche réellement le bouton « Tester / récupérer les caméras » sur go2rtc v1.9.14.

Fonctionnement :
1. Au premier build, Gradle télécharge le binaire officiel go2rtc_linux_arm64 v1.9.14.
2. Le SHA-256 attendu est vérifié : 359fabade8a7a51e81a55fe6df6b0ef81764a5e1d63179577534eaaa71904b50.
3. Le binaire est empaqueté dans jniLibs/arm64-v8a sous le nom libgo2rtc.so pour être extrait avec permission d’exécution.
4. Au lancement, l’app démarre go2rtc sur 127.0.0.1:1984.
5. Les identifiants Wyze (courriel, mot de passe, Key ID, API Key) sont chiffrés avec Android Keystore.
6. « Tester / récupérer les caméras » appelle POST /api/wyze, récupère les sources Wyze, crée les flux temporaires via PUT /api/streams et retourne les lecteurs locaux à la WebView.

Aucune clé ni mot de passe n’est inclus dans le projet.

Compilation : ouvrir ce dossier avec Android Studio, laisser Gradle synchroniser, puis Build > Build APK(s). Une connexion Internet est nécessaire au premier build pour récupérer le binaire officiel ARM64.

Limite : l’APK est ARM64 uniquement (arm64-v8a), ce qui correspond aux appareils Android récents comme le Pixel 10 Pro XL. Le premier test réel des caméras doit être fait sur l’appareil, car cet environnement n’a pas de téléphone Android ni de réseau local Wyze.
