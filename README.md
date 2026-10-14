# Le Crible

Appli Android qui « crible » tes notifications : elle les capte, les retire de l'écran et les range par application. Tu les retrouves en ouvrant l'appli.

- Réglages : mode « tout sauf les applis cochées » ou « seulement les applis cochées ».
- Exclus par défaut : SMS, WhatsApp, téléphone (appels entrants et manqués).
- Au premier lancement : appuyer sur « Autoriser l'accès » puis activer Le Crible dans la liste.

## Compiler, tester et analyser avec GitHub

1. Envoyer le contenu du dossier sur un dépôt GitHub (branche `main`).
2. Onglet **Actions** → workflow **Build APK** (se lance à chaque push, ou bouton *Run workflow*).
3. Une fois terminé, trois artefacts sont disponibles en bas de la page du run :
   - **LeCrible-debug-apk** : l'APK à installer ;
   - **LeCrible-rapport-tests** : rapport des tests unitaires (ouvrir `index.html`) ;
   - **LeCrible-rapport-lint** : rapport lint (ouvrir `lint-results-debug.html`).

## Tests

- `app/src/test/` : tests unitaires JVM (règles de tri des notifications, `NotifFilterTest`).
  En local : `./gradlew testDebugUnitTest` (ou clic droit sur le dossier dans Android Studio).
- `app/src/androidTest/` : tests instrumentés de la base Room (`NotifDaoTest`), à lancer sur un émulateur
  ou un téléphone : `./gradlew connectedDebugAndroidTest`. Ils ne sont pas exécutés par GitHub Actions.

## Lint

`./gradlew lintDebug` produit `app/build/reports/lint-results-debug.html`.
Les avertissements ne font pas échouer le build (`abortOnError = false` dans `app/build.gradle.kts`) ;
passe-le à `true` quand tu voudras que le lint soit bloquant.

Le dossier `store/` contient l'icône 1024 px et celle de 512 px pour le Play Store ; `assets-source/` garde les images d'origine.

## Versions de l'outillage

Android Gradle Plugin 9.1.1 (Kotlin intégré, donc pas de plugin `kotlin-android`), Gradle 9.3.1, Kotlin 2.3.10,
KSP 2.3.6, compileSdk et targetSdk 37, Compose BOM 2026.09.00, Room 2.8.5. JDK 17 requis.
Pour ouvrir le projet dans Android Studio, utiliser une version récente qui gère AGP 9.
