# Pocket Studio : règles permanentes

Référence complète : `docs/SPEC.md` (le prompt d'origine ; ne le modifier que sur demande de Benjamin).
Au début de chaque session : lire ce fichier, `docs/PROGRESS.md` et les issues GitHub ouvertes (rapports de recette et bugs) ; regarder aussi le dernier passage de « Nightly real use » (`docs/TESTS-REELS.md`) et traiter un échec (seul, il ne teste que main : pendant la reprise, il est lancé à la main sur fx-int à chaque rangement).
À la fin de chaque session : commit poussé (même en plein travail) et `docs/PROGRESS.md` à jour.
« continue » = reprendre la prochaine étape notée dans `docs/PROGRESS.md` (la première case non cochée de `docs/PLAN-REPRISE.md`), sans rien demander.

## Rôles
- Benjamin ne code pas, ne lit pas le code et ne recopie jamais d'erreur. Il tranche, teste sur son téléphone, valide.
- Autorisation permanente de Benjamin (2026-10-19) : travailler sur tout le projet sans attendre son accord. En fin de phase, après la revue critique et une CI verte, l'agent fusionne lui-même la PR (elle remplace le feu vert D4 = A) et enchaîne sur la phase suivante.
- Le prévenir seulement quand il doit faire quelque chose (tester, un compte, une licence, un secret GitHub : une seule demande groupée, pas à pas), et continuer quand même tout ce qui peut avancer sans lui.
- Questions : numérotées, 2 à 4 options, la recommandée en premier, conséquence en une ligne, réponse du type « 1A, 2B ». Un lot par jalon au maximum.
- Confiance de Benjamin (2026-10-24) : « prends les décisions pour moi ». Ne plus attendre ses réponses : choisir l'option recommandée, la noter dans `docs/DECISIONS.md`, avancer. Ne lui demander que ce qu'il est seul à pouvoir faire (tester sur son téléphone, un compte, un secret, une licence).
- Mode économie de quota (par défaut, permanent) : la « Méthode de travail » ci-dessous (elle remplace « à la file » et « modèle léger », 2026-11-02) ; une seule relecture par version, messages courts, pas de vérifications inutiles.
- Retours de Benjamin (captures + explications de bug) : ne pas les analyser tout de suite. Les ranger dans `docs/RETOURS.md` (date, capture décrite, texte de Benjamin, une ligne par retour), répondre en une phrase « noté », et tout analyser d'un coup seulement quand Benjamin le demande (économie de quota). Après traitement, marquer chaque retour comme fait.
- Tout le reste : décider, noter une ligne dans `docs/DECISIONS.md` (choix, raison, retour arrière), avancer.
- Toujours parler français avec Benjamin (conversation, messages, rappels).
- Messages pour lui : français, courts, sans jargon, écoutables en audio. Humour Kaamelott bienvenu dans les changelogs.
- Interface en français. Code, commentaires et commits en anglais.
- Rappels et tâches planifiées (nom et message) : toujours en français.
- Une minuterie par audit, par lot et par étape (Benjamin, 2026-11-04) : tâche planifiée toutes les 2 h, nommée « <étape> (partie le <jour> à <heure de Paris>) », qui vérifie que l'équipe tourne, relance ce qui est tombé, se renomme à chaque relance et se désactive quand l'étape est finie. La créer au lancement de chaque étape.
- Design : moderne mais pas trop, facile à prendre en main, façon FL Studio Mobile en plus clair. Règles dans `docs/DESIGN.md`. Benjamin a délégué les choix de design : décider sans lui demander, montrer les captures dans la PR.
- Tout changement de design validé s'applique partout dans l'appli (raccord sur tous les écrans), pas seulement là où il a été demandé.
- Captures envoyées à Benjamin (maquettes, captures de PR, APK) : sans la pastille flottante de retours « ● REC · Noter · ⋯ » (il ne s'en sert pas). Le bouton REC du Live, lui, reste.

## Travail en équipes (auto-surveillance)
- Dès que des agents, des testeurs ou des tâches longues tournent en arrière-plan, lancer une surveillance automatique (Monitor) et la relancer à chaque expiration, tant qu'il reste du travail en cours.
- Elle signale : chaque compte rendu ou capture qui arrive, et une file de compilation bloquée (des builds en attente pendant que la machine ne fait presque rien).
- Prévenir Benjamin à chaque fin d'agent, avec les captures « après » pour le design ; ne jamais attendre qu'il demande des nouvelles.
- Compilations : un verrou `flock` par groupe d'équipes (au moins deux en parallèle quand la mémoire le permet) ; banc testeur humain : `--max-workers` au moins égal au nombre de sièges.

- Chaque erreur signalée par Benjamin : la corriger là où il l'a vue ET vérifier partout où elle peut se reproduire, avec un test qui couvre tous les cas.

## Méthode de travail (Benjamin, 2026-11-02 : rapide et économe, toujours)
Ordre des étapes, règles anti-conflit et sources : `docs/PLAN-REPRISE.md`.
- Deux équipes en parallèle d'office quand c'est possible (parties qui ne se touchent pas, machine qui suit : disque > 5 Go, tests qui ne ralentissent pas), sinon une ; jamais plus de deux (sauf les sièges d'un tour de testeurs de l'Audit, seuls sur la machine) ; toujours le modèle le plus fort (choix 1B).
- Consignes fermées : fichiers exacts (via `docs/CARTE-CODE.md`), tests qui prouvent la fin, ce qu'il ne faut pas toucher.
- Les équipes : tests ciblés seulement, résumé des journaux (jamais le journal entier), captures des seuls écrans touchés, commit à chaque morceau vert.
- L'orchestrateur lance lui-même les grandes vérifications (une seule, au moment de ranger dans fx-int) et la compilation de l'APK.
- Nouvelles à Benjamin à chaque étape, en 3 à 5 lignes.
- Ne jamais attendre pour rien : surveillance automatique (disque < 2 Go, test Gradle figé > 20 min, démon Gradle bloqué, équipe sans nouveau commit ni note de suivi depuis 45 min, même échec relancé 2 fois) ; à chaque alerte, l'orchestrateur regarde la note de suivi et agit (tuer le processus figé, libérer le disque, relancer une fois, recadrer ou arrêter l'équipe qui tourne en rond, découper le lot) au lieu d'attendre.

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
- `./gradlew :app:assembleRelease` : ne JAMAIS le lancer à chaque chantier ni à chaque phase (ça coûte du quota). Benjamin (2026-10-23) : l'APK ne se construit et ne s'envoie qu'une fois tout le plan terminé. La CI de GitHub fait ses propres constructions toute seule. La CI ajoute un test de fumée sur émulateur x86_64.
- `./scripts/live-bench.sh` : « écoute en direct », les morceaux joués comme sur le téléphone (moteur temps réel, mêmes commandes que l'appli), niveau seconde par seconde, alerte fondu / silence / saut. Benjamin (2026-10-24) : chaque audit et chaque écoute de vérification passent aussi par ce chemin, jamais seulement par l'export.
- APK : Benjamin (2026-10-24) : n'en construire et n'en envoyer que sur sa demande explicite. Déjà demandée : l'APK d'audit (étape G de `docs/PLAN-REPRISE.md`, consigne du 2026-10-31) et les versions release de l'Audit.
- CI rouge : lire les journaux, corriger, relancer. N'en parler que si réglé ou vraiment bloqué.
- Audits (leçon des tours 0 à 5, 2026-11-06) : **l'outil de mesure se vérifie avant de mesurer**. Aucun lot de testeurs sans `scripts/bench/selftest.sh` vert et sans dérogation ; une réparation du banc se prouve par un test rouge avant, vert après ; un verdict « c'est le banc » se prouve par un test sans banc, jamais par une impression. Plan : `docs/audits/ultra/PLAN-BANC-FIABLE.md`.
- Captures (Benjamin, 2026-11-06) : toujours regarder l'écran **entier**, pas seulement ce qui vient de changer ; tout ajout se compare à ses voisins (forme, taille, alignement : `docs/DESIGN.md`, principe 11). Dans les audits, tout est analysé : chaque écran passe la grille « œil neuf » (`docs/audits/AUDIT-ULTRA.md` §12.11).

## Revue de fin de phase (obligatoire)
À la fin de chaque phase, AVANT de demander le feu vert : faire relire par un sous-agent neuf qui n'a pas écrit le code.
- Cible : les fichiers modifiés pendant la phase et les zones sensibles : callback audio sans allocation ni verrou, sauvegarde sans perte, plantages, fuites mémoire, tests qui vérifient vraiment quelque chose.
- Corriger tout ce qui est grave, noter le reste dans `docs/REVIEW.md`.
- Résumer le tout à Benjamin en trois lignes.

## Livraison
- Une branche et une pull request par phase ; chaque PR publie l'aperçu sur la pré-release fixe `preview`.
- Chaque fusion sur main publie une Release ; `releases/latest/download/pocket-studio.apk` = dernière version stable.
- versionCode = numéro d'exécution de la CI + 100 (dès l'étape G de `docs/PLAN-REPRISE.md` : l'APK 234 avait été faite en local quand la CI en était à 223). Toute APK envoyée et tout AAB pour Play viennent de la CI, jamais d'une construction locale (le mot de passe ne survit pas d'une session à l'autre) : une seule suite de numéros, toujours croissante, donc chaque version s'installe par-dessus la précédente et Play l'accepte. Changelog français de 5 lignes max dans `app/src/main/assets/changelog.txt`.
- Liste de recette du jalon en cours : `app/src/main/assets/recette.json` (à réécrire à chaque jalon).
- Benjamin (2026-11-08) : un test important à lui faire faire se demande **ici, dans la conversation**, un à la fois, avec le mode, les gestes et la réponse attendue (chemin vérifié dans le code avant). La page Recette n'est plus le canal principal.
- Signature : keystore chiffré `signing/release.p12` dans le dépôt, mot de passe dans le secret GitHub `SIGNING_PASSWORD`.

## Plan et suivi
- `docs/ROADMAP.md` : phases, jalons, critères d'acceptation.
- `docs/PROGRESS.md` : où on en est, prochaine étape exacte.
- `docs/DECISIONS.md` : journal des décisions.
- `docs/REVIEW.md` : revues de fin de phase et points restants.
- `docs/ARCHITECTURE.md` : schéma et modules.
