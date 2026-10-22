# Le Crible

Appli Android qui « crible » tes notifications : elle les capte, les retire de l'écran et les range par application. Tu les retrouves en ouvrant l'appli.

- Réglages : mode « tout sauf les applis cochées » ou « seulement les applis cochées ».
- Exclus par défaut : SMS, WhatsApp, téléphone (appels entrants et manqués).
- Au premier lancement : appuyer sur « Autoriser l'accès » puis activer Le Crible dans la liste.

## Pastilles de notifications

- **Sur l'icône** : Le Crible poste une notification silencieuse (« N en attente »), sans son ni icône dans la barre d'état,
  ce qui fait apparaître la pastille native de ton lanceur sur l'icône. Sa couleur et son style dépendent du lanceur :
  l'appli ne peut pas les choisir. Sur Xiaomi, vérifier dans les réglages de notifications de Le Crible que les pastilles
  sont autorisées.
- **Pastilles de couleur** : Réglages → touche le rond d'une appli pour choisir sa couleur. Le widget « Le Crible »
  (à ajouter sur l'écran d'accueil, taille d'une icône) affiche une bulle par application, dans la couleur choisie, avec son nombre
  de notifications ; 4 bulles par page, on change de page en glissant le doigt vers le haut ou le bas.

## Ouverture, permanentes et fiabilité (v1.1)

- **Toucher une notification** rouvre l'écran exact, comme Android. Cette intention n'existe qu'en mémoire : si Android a
  arrêté l'appli ou redémarré le téléphone, Le Crible ouvre à la place le premier lien trouvé dans la notification (🔗,
  aussi dans le menu ⋮), sinon l'application d'origine, et le dit.
- **Permanentes** : section à part en haut de la liste, elles restent après ouverture, ne comptent pas dans la pastille ni
  le widget et ne sont pas réécrites en boucle quand elles se mettent à jour sans changer. Pour les cacher de l'écran de
  verrouillage : ⋮ → réglages → écran de verrouillage (sans désactiver la catégorie, sinon Android ne la transmet plus).
- **Fiabilité** : onglet « État » (service relié ou non, dernière connexion / déconnexion, dernière erreur ou plantage,
  bouton « Reconnecter »), chien de garde toutes les ~15 min, erreurs en arrière-plan interceptées au lieu d'arrêter l'appli.

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
