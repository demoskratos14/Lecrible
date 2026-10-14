# Le Crible

Appli Android qui « crible » tes notifications : elle les capte, les retire de l'écran et les range par application. Tu les retrouves en ouvrant l'appli.

- Réglages : mode « tout sauf les applis cochées » ou « seulement les applis cochées ».
- Exclus par défaut : SMS, WhatsApp, téléphone (appels entrants et manqués).
- Au premier lancement : appuyer sur « Autoriser l'accès » puis activer Le Crible dans la liste.

## Compiler l'APK avec GitHub

1. Envoyer le contenu du dossier sur un dépôt GitHub (branche `main`).
2. Onglet **Actions** → workflow **Build APK** (se lance à chaque push, ou bouton *Run workflow*).
3. Une fois terminé : télécharger l'artefact **LeCrible-debug-apk** en bas de la page du run.

Le dossier `store/` contient l'icône 1024 px et celle de 512 px pour le Play Store ; `assets-source/` garde l'image d'origine et la version à fond retiré.
