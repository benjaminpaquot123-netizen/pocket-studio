# Pocket Studio : règles permanentes

Référence complète : `docs/SPEC.md` (le prompt d'origine ; ne le modifier que sur demande de Benjamin).
Au début de chaque session : lire ce fichier, `docs/PROGRESS.md` et les issues GitHub ouvertes (rapports de recette et bugs).
À la fin de chaque session : commit poussé (même en plein travail) et `docs/PROGRESS.md` à jour.
« continue » = reprendre la prochaine étape notée dans `docs/PROGRESS.md`, sans rien demander.

## Rôles
- Benjamin ne code pas, ne lit pas le code et ne recopie jamais d'erreur. Il tranche, teste sur son téléphone, valide.
- Autorisation permanente de Benjamin (2026-10-19) : travailler sur tout le projet sans attendre son accord. En fin de phase, après la revue critique et une CI verte, l'agent fusionne lui-même la PR (elle remplace le feu vert D4 = A) et enchaîne sur la phase suivante.
- Le prévenir seulement quand il doit faire quelque chose (tester, un compte, une licence, un secret GitHub : une seule demande groupée, pas à pas), et continuer quand même tout ce qui peut avancer sans lui.
- Questions : numérotées, 2 à 4 options, la recommandée en premier, conséquence en une ligne, réponse du type « 1A, 2B ». Un lot par jalon au maximum.
- Mode économie de quota (par défaut, permanent) : peu d'agents et à la file (1 agent pour 2-3 tâches proches), modèle léger pour relectures et petites tâches, une seule relecture par version, messages courts, pas de vérifications inutiles. Si Benjamin dit « tu peux accélérer », mode rapide (agents en parallèle) pour la phase en cours seulement ; à la fin de cette phase, revenir automatiquement en économie et le lui dire.
- Retours de Benjamin (captures + explications de bug) : ne pas les analyser tout de suite. Les ranger dans `docs/RETOURS.md` (date, capture décrite, texte de Benjamin, une ligne par retour), répondre en une phrase « noté », et tout analyser d'un coup seulement quand Benjamin le demande (économie de quota). Après traitement, marquer chaque retour comme fait.
- Tout le reste : décider, noter une ligne dans `docs/DECISIONS.md` (choix, raison, retour arrière), avancer.
- Toujours parler français avec Benjamin (conversation, messages, rappels).
- Messages pour lui : français, courts, sans jargon, écoutables en audio. Humour Kaamelott bienvenu dans les changelogs.
- Interface en français. Code, commentaires et commits en anglais.
- Rappels et tâches planifiées (nom et message) : toujours en français.
- Design : moderne mais pas trop, facile à prendre en main, façon FL Studio Mobile en plus clair. Règles dans `docs/DESIGN.md`. Benjamin a délégué les choix de design : décider sans lui demander, montrer les captures dans la PR.

## Règle d'or
Réutiliser l'open source éprouvé (GPL/AGPL acceptées, usage personnel). Vérifier licence et dernière version, tenir `THIRD_PARTY.md` à jour. Trois échecs sérieux sur une brique = alternative suivante + ligne dans DECISIONS.md.

## Thread audio (non négociable)
- Dans le callback audio : aucune allocation, aucun verrou, aucun log, aucun appel JNI vers Kotlin.
- Commandes UI → moteur et mesures moteur → UI par files SPSC lock-free (`engine/src/main/cpp/core/SpscQueue.h`) ou atomiques.
- Séquenceur à l'échantillon près dans le thread audio, jamais de timers Kotlin.
- Le cœur (`engine/src/main/cpp/core/`) ne dépend pas d'Android : il compile et se teste sur Linux (`engine/host-tests/`).
- Float 48 kHz, AAudio exclusif + LowLatency via Oboe, mode performance soutenue.

## Vérifier avant de livrer
- `./scripts/host-tests.sh` : tests du moteur sur Linux (rendu hors-ligne, timing, ni NaN ni saturation).
- `./gradlew :app:testDebugUnitTest` (logique) et `./gradlew :app:verifyRoborazziDebug` / `recordRoborazziDebug` (captures d'écran, à regarder soi-même).
- `./gradlew :app:assembleRelease` doit passer. La CI ajoute un test de fumée sur émulateur x86_64.
- CI rouge : lire les journaux, corriger, relancer. N'en parler que si réglé ou vraiment bloqué.

## Revue de fin de phase (obligatoire)
À la fin de chaque phase, AVANT de demander le feu vert : faire relire par un sous-agent neuf qui n'a pas écrit le code.
- Cible : les fichiers modifiés pendant la phase et les zones sensibles : callback audio sans allocation ni verrou, sauvegarde sans perte, plantages, fuites mémoire, tests qui vérifient vraiment quelque chose.
- Corriger tout ce qui est grave, noter le reste dans `docs/REVIEW.md`.
- Résumer le tout à Benjamin en trois lignes.

## Livraison
- Une branche et une pull request par phase ; chaque PR publie l'aperçu sur la pré-release fixe `preview`.
- Chaque fusion sur main publie une Release ; `releases/latest/download/pocket-studio.apk` = dernière version stable.
- versionCode = numéro d'exécution de la CI. Changelog français de 5 lignes max dans `app/src/main/assets/changelog.txt`.
- Liste de recette du jalon en cours : `app/src/main/assets/recette.json` (à réécrire à chaque jalon).
- Signature : keystore chiffré `signing/release.p12` dans le dépôt, mot de passe dans le secret GitHub `SIGNING_PASSWORD`.

## Plan et suivi
- `docs/ROADMAP.md` : phases, jalons, critères d'acceptation.
- `docs/PROGRESS.md` : où on en est, prochaine étape exacte.
- `docs/DECISIONS.md` : journal des décisions.
- `docs/REVIEW.md` : revues de fin de phase et points restants.
- `docs/ARCHITECTURE.md` : schéma et modules.
