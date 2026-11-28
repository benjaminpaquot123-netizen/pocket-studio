# Feuille de route du chef pour la nuit du 24 novembre (départ 2 h 05)

But : lancer tout le plan `PLAN-APRES-ROBOT.md` (sa section 7 fait foi) sans bug, sans attente inutile, sans perdre de quota. Le chef est la discussion principale (`session_01NqonxZBBysqqWBbnsC2VTP`) ; le rappel `trig_01CN1EkYPspt76rNhzHDSjSf` la réveille.

Consigne de Benjamin (2026-11-23, soir) : « Tout ce qu'on vient de parler, tu lances tout à 2h05 […] c'est toi le chef d'orchestre. » Il est à l'hôtel : pas de tablette, pas de test sur appareil avant son retour.

**Ce qui fait foi** : cette feuille, relue par un auditeur neuf le 23 au soir. Elle passe avant le texte du rappel (plus ancien : il ne la cite pas et dit « rien pour F ») et avant le plan sur deux points : F0 part cette nuit avec 8 styles, et A1 s'arrête après ses deux premiers paquets. Après un compactage : relire cette feuille et `EN-COURS.md`, pas le plan entier.

## 0. Pièges connus

1. **Disque** : 462 Mo libres. Le disque affiche 252 Go mais n'en donne que 40 environ : « libérer 15 Go » est hors de portée. But réaliste : **10 Go**. Jamais de portail sous 4 Go. Liste et ordre : section 1.
2. **`Scenarios.kt` et le robot n'existent que dans `/home/user/bt-build`** (`eq/bentest-robot-2`, 2c0cab99c ; GitHub n'a que l'ancien `eq/bentest-robot`). E en a besoin autant que A1. Le chef ne pousse que l'intégration (règle de sa session) : Benjamin a donné son accord écrit le 23 au soir (« Oui ») pour pousser les branches de travail `eq/…` : le chef pousse cette branche en premier après le disque. Repli seulement si le push est refusé : E et A1 sont des sous-agents du chef dans `/home/user/bt-build` (E en lecture seule), après le nettoyage du disque ; le lot A1 attend l'accord pour sa CI.
3. **Le plan n'est pas dans la branche du robot** : E et A1 le lisent par `git show` (c'est écrit dans leur fiche).
4. **`portail-file.sh` refusera deux fois** : `/home/user/fx-int2` est en tête détachée, et le paquet 9 compte déjà 27 lots. La recette qui marche est en section 6.
5. **Pas de CI automatique sur une branche `eq/…`** (pas de PR). Le chef la lance à la main (section 5).
6. **Pousser sur l'intégration pendant qu'une CI y tourne l'annule.** Aucun commit de texte poussé pendant une CI de paquet.
7. **Le lot fable5 n'est pas prêt pour le portail** : pas de relecture, 10 commits de retard, pas de CI, mauvais nom de branche. Fiche en section 3.
8. **Mémoire du chef** : 137 k sur 200 k le 23 au soir ; un compactage arrivera dans la nuit. Chaque identifiant (discussion, branche, sha, numéro de CI) s'écrit dans `EN-COURS.md` dès qu'il existe. Le chef ne lit jamais un journal ni un rapport entier : `tail`, ou un sous-agent.
9. **La clé Freesound n'existe qu'ici** (dossier `secrets` du bloc-notes) : F0 est un sous-agent du chef. Jamais la clé dans une fiche, un commit, un journal ou un message.
10. **Pousser** : le chef pousse seulement l'intégration `ccr-80af1a1b-mn0wec` et les branches de travail `eq/…` (accord écrit de Benjamin, 2026-11-23 : « Oui ») ; jamais `main`. Les discussions poussent leur branche `eq/…` ou `aud/…` neuve. Jamais `main`, jamais la PR #60.
11. **Modèle** : une discussion neuve prend le modèle du chef si on ne dit rien. Toujours écrire `model`.

## 1. Départ (10 minutes, dans cet ordre)

1. Vérifier qu'aucun Gradle ni banc ne tourne (`pgrep -fa gradle`), puis libérer la place, dans cet ordre, en s'arrêtant à 10 Go (`df -h /home`) :
   - dans `/tmp` : les dossiers `robolectric-*`, `siege*`, `rejeu*`, `origine*`, `drapeaux*`, `t9-parts*` (3,2 Go, tous vieux de plus d'un jour) ;
   - `/root/.gradle/caches/build-cache-1` (11 Go ; simple cache, la première compilation sera plus lente) ;
   - dans le bloc-notes, les artefacts de CI déjà lus : `ci403`, `ci404`, `t0c380`, `s382`, `gf387`, `art`, `c8-art`, `shots361` et son zip, `utr`, `recalcul-t9` (1,4 Go) ; les APK du robot v1 à v5 (270 Mo) ; `/tmp/clt.zip` (133 Mo) ;
   - en dernier recours : `/home/user/fx-int2/app/build` (350 Mo), les `build/` de `/home/user/wt-t965` (200 Mo), `/home/user/bt-build/app/build` (1,1 Go, seulement une fois le robot poussé).
   - **Jamais** : `/root/.gradle/caches/modules-2` et `caches/9.8.1` (il faudrait tout retélécharger), `/opt/android-sdk`, un dossier `.git`, `/home/user/fx-int` (le banc le lit), dans le bloc-notes `rap1` à `rap9`, `trial`, `secrets`, les APK v6 et v7, ni l'atelier `/home/user/wt-t965` lui-même (son commit 84af36df4 n'est poussé nulle part).
2. `/home/user/fx-int2` : `git fetch origin ccr-80af1a1b-mn0wec`, la tête locale égale `FETCH_HEAD`. Le dossier `docs/audits/ultra/rejeux/_echecs/` non suivi reste là.
3. CI de la tête : `scripts/bench/ci-etat.sh` (il répond maintenant, sans `gh`). Rouge ou en cours : une ligne de note, et tout part quand même ; rien ne fusionne avant le vert.
4. La clé est présente : test de taille du fichier, sans l'afficher.
5. Accord de Benjamin donné : `git -C /home/user/bt-build push origin 2c0cab99c:refs/heads/eq/bentest-robot-2`. Si le push est refusé : A1 et E sont des sous-agents du chef dans `/home/user/bt-build` (E en lecture seule).
6. Écrire dans `EN-COURS.md` (local, pas poussé tout de suite) : heure de départ, place libre, état de la CI.

## 2. Ce qui démarre

| Qui | Où | Modèle | Départ (`source_revision`) | Lot | Rend |
|---|---|---|---|---|---|
| E Audit scénarios et compteurs | discussion, lecture seule | `claude-opus-5-5` | `eq/bentest-robot-2` | `aud/tour-9-audit-E` | un fichier poussé + une ligne |
| A1 Robot, réparations | discussion | `claude-opus-5-5` | `eq/bentest-robot-2` | `eq/tour-9-robot1` | sha poussé |
| B1 Moteur, mesure | discussion | `claude-fable-5-2` (décidé par Benjamin le 23) | `ccr-80af1a1b-mn0wec` | `eq/tour-9-moteur1` | sha poussé + tableau |
| C Fluidité | discussion | `claude-opus-5-5` | `ccr-80af1a1b-mn0wec` | `eq/tour-9-fluide1` | sha poussé |
| D Service audio | discussion | `claude-opus-5-5` | `ccr-80af1a1b-mn0wec` | `eq/tour-9-audio1` | sha poussé |
| F0 Essais d'écoute | sous-agent du chef, bloc-notes | Opus 5.5 | sans git | aucun | MP3 + liste d'origine |
| fable5 Relecture | discussion (la branche est sur GitHub) | `claude-opus-5-5` | `ccr-56d43d5b-vfdmoc` (0758fd6cf) | `eq/tour-9-fable5` | sha poussé |

Ouvrir une discussion : `create_session` avec `source_url` = `https://github.com/benjaminpaquot123-netizen/pocket-studio`, `source_revision`, `model`, `title`, `clone_depth: 1000` (un historique coupé donne de faux rouges au pré-vol), `prompt` = la fiche + les règles communes. Jamais `permission_mode: "plan"` (elle attendrait quelqu'un toute la nuit). Les cinq dans le même tour, puis les deux sous-agents. Noter aussitôt les cinq identifiants dans `EN-COURS.md`.

Sous-agent du chef : F0 (et E, A1 seulement dans le repli) ; un seul Gradle local à la fois.

## 3. Les fiches (à coller telles quelles)

### Règles communes (à la fin de chaque fiche de discussion)

> Lis seulement `CLAUDE.md`, la partie « En arrivant » de `docs/audits/ultra/EN-COURS.md` et cette fiche. Pas de message « prêt ».
> Tour : `tour-9`. Ta zone et ta branche sont dans la fiche. Hors de ta liste de fichiers tu ne touches à rien : tu le demandes au chef.
> Chaque réparation : un commit « rouge: sujet » avec le test qui échoue, puis « vert: même sujet ». Trois réparations au plus. Tests ciblés seulement, par `scripts/bench/gradle-file.sh`. `git add` par chemin.
> À rendre dans la branche : `docs/audits/ultra/tour-9-<zone>-rapport.md` (une ligne `tests:` et au moins une ligne `chemin: <écran> | test: Classe#méthode`, un test de `app/src/test` de ta branche), `docs/decisions/tour-9-<zone>.md`. Tu dis « tenté », jamais « corrigé ».
> Avant de finir : `git fetch origin ccr-80af1a1b-mn0wec && git merge FETCH_HEAD`, puis `scripts/bench/pre-vol.sh --avant-relecture tour-9 <zone>`, puis une relecture neuve par un sous-agent qui n'a pas écrit le code, en **dernier commit** (`docs/audits/ultra/tour-9-<zone>-relecture.md`, ligne 2 « Relu : <sha> », dernière ligne « Verdict : 0 bloquant, 0 grave »), puis le pré-vol doit dire PRÊT. Pousse ta branche seulement ; jamais l'intégration, jamais `main`, pas de PR.
> Demander au chef : un doute = une ligne à `session_01NqonxZBBysqqWBbnsC2VTP` (`send_message`), jamais une question à Benjamin.
> Travailler en silence : aucun texte pour Benjamin, pas de récit. Un seul message de fin : « fini <sha> » ou « bloqué : <raison en une ligne> ». Tu n'attends pas ta CI.

### E, audit des scénarios et des compteurs

> Rôle : lecteur-vérificateur, **lecture seule** sur le code. Départ : `eq/bentest-robot-2`.
> Ta mission : `git fetch origin ccr-80af1a1b-mn0wec` puis `git show FETCH_HEAD:docs/audits/ultra/PLAN-APRES-ROBOT.md`, lis la section « Discussion E », le tableau des constats C1 à C15 et la section 7.
> Lis `Scenarios.kt` et ses tests (`app/src/benTest/**`, `app/src/journey/**`, `app/src/test/java/com/pocketstudio/bentest/**`) : scénarios distincts ou copies, oracles qui peuvent mentir (chaque oracle de geste comparé au modèle de l'appli), restes d'un scénario au suivant, recherche des éléments à l'écran, seuils ; puis les quatre compteurs (images lentes, écart entre blocs, mémoire, délai du son) ; puis le bon dessin pour passer à 400 scénarios.
> Rendu : un seul fichier, `docs/audits/ultra/tour-9-audit-scenarios.md`, sur une branche neuve `aud/tour-9-audit-E`. En tête, « Règles pour le catalogue » : 10 lignes au plus. Ensuite chaque faute : fichier, ligne, ce qui ment, la preuve. Rien d'autre ne change. Pas de Gradle, pas de rapport de zone, pas de relecture.
> Si une faute touche C8, C10, C11, C5, C2 ou C3 (réparés cette nuit par A1), envoie-la au chef en une ligne dès que tu la vois, sans attendre la fin.
> Fin : « fini <sha> ».

### A1, robot, réparations

> Rôle : correcteur. Zone `robot1`. Départ : `eq/bentest-robot-2` ; crée `eq/tour-9-robot1`, puis tout de suite `git fetch origin ccr-80af1a1b-mn0wec && git merge FETCH_HEAD` (un conflit hors de tes fichiers : « bloqué »).
> Tes fichiers : `app/src/benTest/**`, `app/src/journey/**`, `app/src/test/java/com/pocketstudio/bentest/**`, `scripts/bentest/**`, le bloc benTest de `app/build.gradle.kts`. Rien dans les écrans, le moteur, `project-model/**`.
> Ta mission : `docs/audits/ultra/PLAN-APRES-ROBOT.md`, section « Discussion A1 ». Cette nuit, **les paquets 1 et 2 seulement** : C8, C11, C10, puis C5, C2, C3. Tests du robot verts entre les deux. Les paquets 3 et 4 et le confort iront à une discussion neuve.
> Aucun scénario neuf (verrou de l'audit E). La garde `ScenariosVraisEcransTest` (téléphone étroit et tablette) doit couvrir chaque scénario que tu touches.
> Il te manque un nom d'élément à l'écran (`testTag`) : une ligne au chef, tu ne touches pas l'écran.
> Ton rapport de zone couvre aussi les 19 commits du robot déjà dans la branche. Aucune APK.
> Fin : « fini <sha> ».

### B1, moteur, la mesure (Fable)

> Rôle : correcteur. Zone `moteur1`. Départ : `ccr-80af1a1b-mn0wec` ; branche `eq/tour-9-moteur1`.
> Le problème : une tablette d'entrée de gamme coupe le son sur plusieurs styles, et on ne sait pas quelle brique coûte le plus ; la mesure « écart entre blocs » (C3) ment peut-être.
> Tes fichiers : `engine/src/main/cpp/core/**`, `engine/host-tests/**`, `engine/bench/**`, `scripts/host-tests.sh`, `scripts/live-bench.sh`, `scripts/cpu-bench.sh`. Rien dans `engine/src/main/cpp/android/**` ni en Kotlin. Tu ne changes aucune signature publique du cœur (`CallbackLoad.h`, `Engine.h`) : une autre discussion travaille dessus cette nuit ; besoin contraire = une ligne au chef.
> La preuve attendue, **sans rien changer au son** : (1) tableau de la charge par style et par brique (synthés, effets, réverbération, mixage), briques classées par coût ; (2) un test du moteur avec des blocs de tailles inégales qui dit si l'écart de C3 ment ; s'il ment, mesure corrigée, rouge puis vert ; (3) un budget de charge par style dans les tests du moteur, **relatif** (rapport mesuré dans le même passage, pas des millisecondes : les machines de la CI varient).
> À dire dans le rapport : le classement sur Linux vaut pour l'ordre, pas pour les chiffres. Règles du thread audio de `CLAUDE.md` : non négociables.
> Ligne `chemin:` du rapport : cite un test existant de `app/src/test` qui joue le moteur, sans en écrire. Pas de Gradle sauf le pré-vol.
> Détail : `docs/audits/ultra/PLAN-APRES-ROBOT.md`, section « Discussion B1 ». Fin : « fini <sha> », puis ferme-toi.

### D, service audio

> Rôle : correcteur. Zone `audio1`. Départ : `ccr-80af1a1b-mn0wec` ; branche `eq/tour-9-audio1`.
> Tes fichiers : `engine/src/main/cpp/android/**`, `engine/src/main/java/**`, et dans l'appli `playback/`, `audio/Headphones.kt`, `audio/MicRecorder.kt`, `latency/`, `settings/Settings.kt`. Rien dans `engine/src/main/cpp/core/**`, les écrans, le robot, `app/build.gradle.kts`. Tes tests : des fichiers neufs.
> Ta mission : C4 (reprise propre quand la sortie du son change) puis le tampon automatique (monter d'un cran quand les coupures s'accumulent, sans redémarrer le son en boucle). Pas C5 ni C2 cette nuit. Chaque réparation : un test qui simule la panne, rouge puis vert.
> Si la cause est dans le cœur du moteur : une ligne au chef, tu n'y touches pas.
> Détail : `docs/audits/ultra/PLAN-APRES-ROBOT.md`, section « Discussion D ». Fin : « fini <sha> ».

### C, fluidité

> Rôle : correcteur. Zone `fluide1`. Départ : `ccr-80af1a1b-mn0wec` ; branche `eq/tour-9-fluide1`.
> Tes fichiers, dans l'appli : `mixer/`, `live/`, `projects/`, `library/`, `sounds/`, `session/`, `ui/`, `rhythm/RhythmViewModel.kt` seul, et `ui-components/**`. Rien dans le moteur, le robot, `playback/`, `settings/`, `app/build.gradle.kts`. Tes tests : des fichiers neufs.
> Ta mission : C6. D'abord analyser ce qui redessine trop sur Mix et Live pendant la lecture, puis trois réparations au plus. Pas C7, C8 ni C9.
> Tu ne renommes ni ne retires aucun `testTag` : le robot les cherche. Aucune capture changée sans raison ; captures des seuls écrans touchés, regardées en entier.
> Détail : `docs/audits/ultra/PLAN-APRES-ROBOT.md`, section « Discussion C ». Fin : « fini <sha> ».

### fable5, relecture (sous-agent du chef)

> Le lot `ccr-56d43d5b-vfdmoc` (0758fd6cf) ne change qu'un test (`Tour5BloquantsTest.kt`) ; il a son rapport et ses décisions, pas de relecture. Dans un atelier git neuf du bloc-notes : crée `eq/tour-9-fable5` depuis 0758fd6cf, fusionne `origin/ccr-80af1a1b-mn0wec`, relis le changement en relecteur neuf (fiche du relecteur de `docs/AIDE-FUSIONS-TESTS.md`), écris `docs/audits/ultra/tour-9-fable5-relecture.md` en dernier commit, pousse `eq/tour-9-fable5`. Aucun Gradle. Si le rapport cite une décision de seuil absente de `docs/DECISIONS.md`, dis-le : ne l'invente pas. Rends : le sha, le verdict, une ligne.

### F0, essais d'écoute (sous-agent du chef)

> Benjamin, 2026-11-23 : « il me faut plus d'essais à écouter pour voir si tu es sur la bonne voie ». Travail dans le dossier `ecoute-f0` du bloc-notes ; **rien dans git**.
> 8 styles : trap/drill, Jersey Club, minimale/tech house, afro house/amapiano, UK garage, techno, lo-fi/boom bap, pop/reggaeton. Pour chacun un MP3 d'environ 30 s : « avant » (le son actuel de l'appli, rendu par le moteur sur Linux), une seconde de blanc, « après » (le même rythme avec des sons synthétisés ou CC0 de Freesound). Les deux moitiés au même volume. Plus un fichier « kicks et basses », coup par coup.
> Outils présents : `ffmpeg`, le réseau vers Freesound. La clé : lue depuis son fichier dans une variable, jamais affichée, jamais écrite ailleurs.
> Règles d'origine (section F du plan) : CC0 seulement, pas de disque, pas de break connu, pas de voix de chanson ; un vrai lit enregistré en CC0 est permis. Une liste d'origine par fichier (identifiant, auteur, licence, adresse).
> Pas de Gradle. Si le disque passe sous 2 Go : arrête et dis-le. Rends : la liste des fichiers et trois lignes.

## 4. Verrou E → catalogue

- A1 ne crée aucun scénario neuf.
- Quand E rend : le chef lit seulement « Règles pour le catalogue » (`git show origin/aud/tour-9-audit-E:docs/audits/ultra/tour-9-audit-scenarios.md | head -30`). Ces règles iront dans la fiche de A3, plus tard.
- Une faute de E sur un oracle que A1 répare : le chef la passe à A1 en une ligne, tout de suite.

## 5. Surveiller sans gaspiller

- Les discussions écrivent au chef, les sous-agents le réveillent à la fin. Aucune minuterie périodique.
- Voir tout le monde d'un coup : `list_sessions` avec `children: true`. Lire une discussion : `list_events` avec `kinds: ["assistant","result"]` et `limit: 100`, jamais tout le fil.
- Lancer la CI d'un lot : `workflow_dispatch` de `ci.yml` sur la branche du lot (outil GitHub `actions_run_trigger` ; `gh` n'est pas connecté). Lire : `scripts/bench/ci-etat.sh <sha>`.
- Attendre une CI sans minuterie, en arrière-plan (la fin de la commande réveille le chef) :
  `timeout 5400 bash -c 'until scripts/bench/ci-etat.sh <sha> | grep -qE "^ci_build (vert|rouge)"; do sleep 300; done'`
  Pas plus d'une lecture toutes les 5 minutes : GitHub en donne 60 par heure sans compte.
- Une seule minuterie de secours (`send_later`, 50 minutes, une fois), seulement quand rien d'autre ne peut prévenir ; la retirer dès qu'un événement arrive.
- Alarmes : disque sous 2 Go → nettoyer (section 1) ; Gradle figé plus de 20 min → tuer, relancer une fois ; discussion sans commit depuis 45 min → lire sa dernière ligne, recadrer ou arrêter ; même rouge deux fois sur un lot → une ligne à Benjamin au matin, proposer Fable sur une discussion neuve (il décide).
- Une discussion qui rend est archivée tout de suite (`archive_session`), sauf si son lot peut être refusé au portail : alors après la fusion.

## 6. Fusionner

Les deux fiches de `docs/AIDE-FUSIONS-TESTS.md` font foi. Trois lots au plus par paquet, comptés à la main cette nuit. Rien n'entre sur une base rouge. On ne fusionne que la tête poussée.

- **Paquet 1** : `robot1`, puis `fable5`. **Paquet 2** : `moteur1`, `audio1`, `fluide1`. E n'entre pas par le portail (un fichier de texte, rangé par le chef à la clôture).
- Avant le portail, pour chaque lot : sa CI complète verte sur sa tête, et cette tête contient l'intégration du moment (sinon : renvoyer à sa discussion, « fusionne l'intégration »).
- La recette, dans `/home/user/fx-int2` (tête détachée, c'est normal) :
  1. `git fetch origin ccr-80af1a1b-mn0wec` ; la tête locale est la même, sans rien de modifié ;
  2. `git fetch origin eq/tour-9-<zone> && git branch -f eq/tour-9-<zone> FETCH_HEAD` ;
  3. `git merge-base --is-ancestor HEAD eq/tour-9-<zone>` (sinon renvoyer le lot) ;
  4. `scripts/bench/pre-vol.sh tour-9 <zone>` : doit dire PRÊT ;
  5. `scripts/bench/fusion-equipes.sh tour-9 <zone> > <bloc-notes>/portail-<zone>.log 2>&1`, en arrière-plan ; au réveil, `tail -15` du journal, jamais le journal entier ;
  6. après le dernier lot du paquet : `git push origin HEAD:refs/heads/ccr-80af1a1b-mn0wec`, puis attendre sa CI (section 5). Rien d'autre n'est poussé avant le vert, pas même un texte.
- Un lot : environ 40 minutes de tests locaux, un seul Gradle à la fois. **Les cinq lots ne tiendront pas tous dans la nuit** : paquet 1 d'abord ; le paquet 2 attend sa CI verte, même si c'est le matin.
- Refus du portail : fiche 7 de `GABARIT-FICHES-V3.md`, renvoyée à la discussion du lot. Deux refus pour le même lot : arrêter, le noter pour Benjamin.
- `moteur1` : si le portail le refuse seulement faute de test d'appli, le chef fusionne à la main comme pour L13, avec une ligne dans `docs/DECISIONS.md`.
- Le robot v8 ne se compile pas cette nuit : il attend une tête d'intégration à CI verte et la place sur le disque.

## 7. Parler à Benjamin

- La nuit : rien, sauf un blocage qu'il est seul à pouvoir lever.
- On ne sait pas quand il se réveille, et aucune minuterie ne l'attend. Donc le chef tient le point du matin prêt dans `EN-COURS.md`, à jour à chaque événement, et le dit à la fin de son dernier tour de la nuit.
- Le point (on le tutoie, heure belge, 5 lignes au plus, après lecture de la CI) :
  1. le compteur en trois lignes (« Bugs corrigés dans l'appli », « En salle d'attente », « Bugs encore à corriger ») ; cette nuit rien n'est « corrigé », tout est « tenté » ;
  2. chaque lot en un mot (entré, en attente, refusé) et le numéro de la CI ;
  3. les essais d'écoute : combien, et comment les écouter ;
  4. ce qu'il doit faire, un test à la fois, quand il a ses appareils (aucun à l'hôtel) ;
  5. « Ce qui n'est pas vert ».
- Les MP3 ne vont ni dans git ni dans une version publiée : au réveil, le chef les envoie dans la conversation (`SendUserFile`), un fichier « avant puis après » par style, comme d'habitude avec Benjamin.

## 8. Clôture de la nuit

- Chaque discussion archivée à la fin de son lot.
- Le fichier de E rangé dans l'intégration avec les textes.
- `EN-COURS.md` et `PROGRESS.md` à jour, poussés quand aucune CI ne tourne sur l'intégration.
- `/home/user/bt-build` gardé tant que le robot n'est pas fusionné.
