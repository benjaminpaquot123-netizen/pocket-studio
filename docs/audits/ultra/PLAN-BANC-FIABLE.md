# Plan « banc fiable » (prêt à lancer : Benjamin dit « fais le banc »)

Écrit le 2026-11-06 à partir des deux méta-audits (`META-AUDIT.md`, `META-AUDIT-2.md`), de la synthèse du tour 5 et des incidents du banc. But : rendre le banc **fiable** (il ne ment pas, il ne fige pas, il refuse de tourner cassé, il se souvient, il rejoue) avant toute correction de l'appli et avant le tour 6. Pas « parfait » : ce qu'il ne fera jamais est écrit au §7.

Règle apprise (et désormais dans `CLAUDE.md`) : **l'outil de mesure se vérifie avant de mesurer ; une réparation se prouve par un test ; un verdict « banc » se prouve aussi.**

## 0. Cadre

- **Déclencheur** : le message « fais le banc » de Benjamin. Je démarre à l'étape 1.1 sans rien demander ; une question au plus par séance, groupée, seulement si je suis bloqué.
- **Périmètre** : le banc (`app/src/test/java/com/pocketstudio/app/human/`, `scripts/bench/`, `scripts/human-*.{sh,py}`), ses scripts de mesure, les gabarits et les règles (`docs/audits/AUDIT-ULTRA.md` §12). **Aucune correction de l'appli** : un défaut de l'appli découvert ici reçoit un verdict et un test rouge dans `verdicts.tsv`, pas un correctif.
- **Où** : une branche `eq/tour-5-banc` dans un atelier créé par `scripts/bench/equipe-atelier.sh cree tour-5 banc` (c'est aussi le premier vrai essai de l'organisation v2). Fusion dans `fx-int` par `scripts/bench/fusion-equipes.sh` après une relecture neuve. Tout Gradle par `scripts/bench/gradle-file.sh` (un seul à la fois, démon Kotlin arrêté, ≥ 6 Go libres).
- **Sauvegarde** : un commit poussé à la fin de chaque étape (par chemin, jamais `git add -A`, jamais de lien de sous-module : `git ls-tree -r HEAD | grep -c '^120000'` = 0), même au milieu d'une séance.
- **Découpage en 3 séances** d'environ une journée de machine chacune, pour que Benjamin puisse arrêter entre deux sans rien perdre. Fin de séance = commit poussé + 5 lignes pour Benjamin.
- **Quota prévu** : je fais l'essentiel moi-même ; agents en plus : 1 lecteur pour les états des tours 1-4 (séance 2), 4 testeurs pour la fumée (séance 3), 1 relecteur neuf (séance 3). Environ 6 agents en tout.
- **Ce que Benjamin doit faire** : rien pendant les trois séances. À la fin, 10 points au plus à essayer sur son téléphone (§6), quand il veut.

## 1. Séance 1 : le banc ne ment plus et ne laisse plus passer (≈ 1 jour)

| # | Quoi | Fichier(s) | Preuve avant / après | Durée |
|---|---|---|---|---|
| 1.1 | **Essai à blanc des scripts v2** avant de s'en servir : `equipe-atelier.sh cree tour-5 banc` (liens de sous-modules, `git status` propre, `df`), un test `Bench*` dans l'atelier pendant qu'un `gradle-file.sh help` attend en file, `fusion-equipes.sh tour-5 essai` avec un rejeu rouge (doit sortir 5 et revenir en arrière) puis sans (doit fusionner), puis tout défaire | `scripts/bench/equipe-atelier.sh`, `fusion-equipes.sh`, `gradle-file.sh` | chaque point noté réussi / échoué dans `docs/audits/ultra/banc-fiable-journal.md` ; un échec se répare avant de continuer | 1 h |
| 1.2 | **Le portail d'auto-tests ne se contourne plus** : `Tour3RenameIdleTest` coupé en deux classes (une activité par classe) ; liste de quarantaine `scripts/bench/selftest-quarantaine.txt` (test, raison, date, qui) lue par `selftest.sh` ; `PS_SKIP_SELFTEST` accepté par `human-tester.sh` **seulement** s'il nomme un test de la liste ; `rejeu-defauts.sh` ne saute plus le portail par défaut ; les 38 tests « sautés » expliqués (quels `assumeTrue`, pourquoi) dans le journal | `Tour3RenameIdleTest.kt`, `selftest.sh`, `human-tester.sh`, `rejeu-defauts.sh` | avant : `selftest.sh` rouge (1 test) et lot lançable quand même ; après : `selftest.sh` **117 verts, 0 rouge, sautés listés avec leur raison**, et un `human-tester.sh` avec une raison bidon refuse (sortie 3) | 3 h |
| 1.3 | **Chaque réponse du banc nomme son siège** : `human-act.py` écrit « Siège lotE/c · écran N » en tête ; `scripts/human-act.py` refuse un chemin de siège qui ne contient pas `/lot[A-F]/[a-d]` ; consigne testeur : aucun fichier hors de son siège (déjà écrite, rendue vérifiable : le lanceur signale tout fichier créé à la racine du scratchpad) | `scripts/human-act.py`, `GABARIT-CONSIGNES-TESTEUR-V2.md`, `test_ultra_bench.py` (cas « en-tête de siège ») | avant : la contamination E a → E c est invisible ; après : impossible sans que ça se voie | 30 min |
| 1.4 | **`attends` attend vraiment** : la fausse séparation dort en temps réel (20 s) mais `attends N` n'avance que l'horloge simulée (plafond 30 s) → 13 min et 6 gestes brûlés au tour 5. Pendant `attends`, pomper le fil principal en temps réel jusqu'à la fin du travail de fond (comme `backgroundQuiet` le fait 5 s) ; plafond 120 s ; au-delà, ligne « BANC : travail encore en cours » | `HumanTester.kt` (`settle`/`pass`), `BenchSeparation.kt` si besoin | test `BenchSeparationWaitTest` : séparation finie en ≤ 2 `attends 30` (rouge avant, vert après) | 1 h 30 |
| 1.5 | **Rejeu possible depuis le dépôt** : `rejeu-nouveau.sh` prend le geste dans `journal.txt` (`#i > …`) quand `cmd-i.txt` manque ; `rejeu-defauts.sh` garde le dossier d'un rejeu rouge (`rejeux/_echecs/<date>/`) et montre ses 10 dernières lignes ; un rejeu naît sur la branche de l'équipe, jamais dans fx-int avant sa correction (règle écrite dans le gabarit équipes) | `scripts/bench/rejeu-nouveau.sh`, `rejeu-defauts.sh`, `GABARIT-CONSIGNES-EQUIPES-V2.md` | créer les 8 rejeux des items ≥ grave du tour 5 (A a #27, E a #117, B c #23, F d #2/#34/#51, D b #39, C d #50) ; `rejeu-defauts.sh` doit dire **« non tenu » pour les 8** et « tenu » pour `t4-26-prise-clavier` | 2 h |
| 1.6 | **Verdicts : qui écrit quoi** : `verdicts.tsv` écrit par l'équipe « banc » (nommée, c'est moi dans ce chantier) ; le coordinateur passe `etat` à « corrigé » dans `verdicts.tsv` et `base.tsv` après chaque fusion ; `tableau-de-bord.py` exclut `etat = corrigé` et `[banc?]` de la condition A, affiche « ? » en B tant qu'une fiche « banc? » ≥ moyen n'a pas sa ligne | `AUDIT-ULTRA.md` §12.3-12.4, `tableau-de-bord.py`, `test_ultra_bench.py` | cas de test : un item « corrigé » ne compte plus dans A ; B = « ? » avec une fiche « banc? » sans verdict | 1 h |

Fin de séance 1 : commit poussé ; message à Benjamin (5 lignes).

## 2. Séance 2 : le banc se souvient et compte juste (≈ 1 jour)

| # | Quoi | Fichier(s) | Preuve | Durée |
|---|---|---|---|---|
| 2.1 | **Mémoire des défauts remplie** (`base.tsv`, tours 1 à 5) : pour une fiche v1, la clé vient du journal (`tag_of(journal, écran − 1)`), les faux tags « rien » et « vue » écartés ; `--maj-base` ajoute le tour à `vu` au lieu d'ignorer une clé connue ; nouvel état « déjà vu (tour-N) » ; `fuse-reports.py sieges/tour-N --maj-base --tour tour-N` pour N = 1 à 5, dans l'ordre | `scripts/bench/fuse-reports.py`, `docs/audits/ultra/base.tsv` | `base.tsv` passe de 31 à ≈ 200 lignes ; la fusion du tour 5 marque au moins A a #30 (T4-27), B a #43 (T4-30), C c #41 / C d #68, A d #10 / E d #11 comme « déjà vu » | 3 h |
| 2.2 | **États « corrigé / banc / voulu » des tours 1-4** : un `corrections.tsv` (tour, item, siège#écran, état, commit, test) écrit une fois à partir du tableau Q1 du 1er méta-audit (30 lignes), des rapports de zone et de `DECISIONS.md` (lecteur : 1 agent, lecture seule), résolu en clés, appliqué tour par tour avant la fusion du tour suivant | nouveau `docs/audits/ultra/corrections.tsv`, `fuse-reports.py` | la fusion du tour 5 avec la base pleine affiche des **[revient]** là où la synthèse dit [T4] (≈ 40 attendus ; écart expliqué un par un) | 3 h |
| 2.3 | **Catalogue des promesses** : `promesses-catalogue.tsv` (P001-P120) tiré des 13 tableaux de `meta/lectures/lecture-promesses.md` ; `fabrique.py` prend des ids (`P93 ; P94`) et `libre:` ; un id une seule fois par rapport ; `tableau-de-bord.py` compte des sièges distincts sur les deux derniers tours ; contradiction signalée quand une fiche cite P93 et que P93 est « oui » dans le même rapport | nouveau `docs/audits/ultra/promesses-catalogue.tsv`, `scratchpad…/fabrique.py` → versionné dans `scripts/bench/fabrique-sieges.py`, `tableau-de-bord.py` | la condition D du tour 5 recalculée dit « 38 tenues / 90 lignes ; 0 vérifiée par 2 sièges » au lieu de « 0/2 ; 85/120 » | 2 h |
| 2.4 | **Clé de grappe v3** : un tag de navigation (`tab_*`, `wizard_next/back`, `guide_bar`, « bouton retour », « rien », « vue ») n'est jamais clé ; vrai bouton → `tag|symptôme` ; ouverture d'un dialogue → `dialogue|symptôme` ; « candidats » (même tag, ≥ 2 sièges, symptômes différents) listés sans fusion ; champ v3 `element:` dans le gabarit ; le marquage `[banc? bouton]` n'écrase plus un `banc?: non` | `fuse-reports.py`, `GABARIT-RAPPORT-TESTEUR.md`, `test_ultra_bench.py` | sur les 190 fiches du tour 5 : 6 grappes ≥ 2 sièges (les 3 justes gardées, la fausse `tab_simple_Clavier` et la lâche `wizard_next` défaites, `dialogue:my_exports` et « Envoyer le projet » gagnées), 0 fusion fausse (vérifié à la main) | 2 h |
| 2.5 | **Tableau de bord sans « ? »** : G, H1, I calculés par programme (branches `eq/`, commits sans test, `git status` sans ` T`, `gh run list` sur le commit du tour, cases « VIDE » de la carte, sièges joués / coupés depuis `mesures.json`) ; H2 = téléphone, seule ligne humaine ; condition « ≥ 16 joués, ≤ 3 coupés » ajoutée ; colonne `echelle` (v1/v2), deux colonnes de graves (mot du testeur / conséquence), ligne « changement d'échelle » entre les tours 4 et 5 ; oracle « libre » durci (une fiche `mission-impossible` revue = mission « non ») ; `test_ultra_bench.py` mis à jour | `tableau-de-bord.py`, `seat-metrics.py`, `test_ultra_bench.py`, `AUDIT-ULTRA.md` §12.10 | `tableau-de-bord.py` sur le tour 5 : **0 « ? » sauf H2** ; E a passe « non » (bloquant #117) | 3 h |

Fin de séance 2 : commit poussé ; message à Benjamin.

## 3. Séance 3 : le banc sait tout faire, et on le prouve (≈ 1 jour)

| # | Quoi | Fichier(s) | Preuve | Durée |
|---|---|---|---|---|
| 3.1 | **Tri des 23 fiches « banc? » et des 13 incidents** dans `verdicts.tsv`, les 6 défauts probables de l'appli d'abord, chacun avec un test **sans banc** rouge si c'est l'appli : (1) compteur du haut figé pendant la lecture (A b, F a, F c, F b #63 ; T4-17) ; (2) le .zip absent de « Mes exports » (C c #41, C d #68) ; (3) fausse alerte « le morceau a changé » après un export (C c #30, E c #61) ; (4) « Prêt à partager ? » exporte toujours 8 mesures (C c #27) ; (5) curseur perdu après ✕ Effacer (A c #61, D b #16, D c #4 ; même bouton que T4-19) ; (6) un son part seul à l'ouverture de Pad Live (A d #43). Les vrais « banc » (C a #1, #32, C c « 1 KB », C b #67) notés tels quels | `docs/audits/ultra/verdicts.tsv`, tests `*AppProofTest` / `BenchSelfTest.kt` (nouveau) | 23 lignes de verdict avec leurs 3 preuves ou « non tranché : téléphone » ; condition B passe de « 0 » à une valeur vraie | 3 h |
| 3.2 | **Batterie d'auto-tests du banc complétée** (les 14 contrôles du 1er méta-audit, Q2 §5) dans `BenchSelfTest.kt` : rendu de la longueur demandée (dont 3 min), position de lecture qui avance **et compteur de l'écran qui change entre deux `attends`**, prise au clavier dans la partie affichée, dialogue à champ en < 5 s mesuré, « Tout couper » / ■ dans Pad Live **et** vue Live **et** ■ de colonne, partage de N fichiers = N, micro par la carte **et** par le ● du haut, pads remplis à chaque ouverture (accueil, après un morceau, `fresh=reel`), touchers Pad Live et Live › Pads, exception de test jamais transformée en « plantage » (4 exceptions provoquées → ligne « BANC »), mesures en direct marquées « (banc) », amenées sur tous les profils du lot (`start.txt` lus), machine (1 démon, ≥ 2 Go disque, ≥ 4 Go mémoire) | `BenchSelfTest.kt`, `selftest.sh`, `seat-metrics.py` (fin `banc`) | chaque contrôle a un test ; `selftest.sh` les lance ; **le contrôle 10** (chaque bouton qui exige le moteur est couvert) n'a plus d'objet depuis le moteur toujours présent : remplacé par « aucun `UnsatisfiedLinkError` en 24 sièges » lu dans les journaux | 4 h |
| 3.3 | **Préalables P3 puis P1** : P3 = geste `tape X Y n intervalle_ms` (une suite de touchers à intervalle précis, horloge du banc avancée d'autant) pour les défis ; P1 = option `fichier=<chemin>` rendue par le sélecteur hors Remix (un .zip et un .mid de test fabriqués par le banc, jamais une vraie chanson). P2 (horloge +3 jours + oracle `reprendre`) et P4 (TalkBack texte) décrits, chiffrés, laissés pour le tour 7 sauf si le temps reste | `HumanTester.kt`, `SeatOptions.kt`, `human-act.py`, `seat-metrics.py`, gabarit testeur | `BenchSelfTest.aTappedRhythmEarnsAStar` vert ; `BenchSelfTest.aZipImportedOutsideRemixOpens` vert ; les adaptations restantes (A c, E b) comptées dans `cases_vides` | 3 h |
| 3.4 | **Tour de fumée** : 4 sièges (A a, A d, C d, E c du tour 5 rejoués avec leurs options, par 4 testeurs neufs, budget 40 gestes) sur le banc fiable, `selftest.sh` vert **sans dérogation** avant | `scratchpad/ultra/fumee/` | 0 gel, 0 incident « gel/moteur » dans `incidents-banc.tsv`, 4 `mesures.json`, fusion v3 et tableau de bord sans « ? » | 1 h 30 |
| 3.5 | **Relecture neuve puis fusion** : un relecteur qui n'a rien écrit relit la branche `eq/tour-5-banc` (callback audio intact ? tests qui testent ? pas de chanson de Benjamin dans le dépôt ?) → `docs/audits/ultra/tour-5-banc-relecture.md` ; `fusion-equipes.sh tour-5 banc -- 'com.pocketstudio.app.human.Bench*' 'com.pocketstudio.app.human.Tour*AppProofTest'` ; CI lancée à la main sur le commit (`ci.yml`, `workflow_dispatch`) et tag `audit-5` pour `audit-emulator.yml` | — | fusion réussie du premier coup (sinon la casse est notée et réparée : c'est le but) ; CI build et émulateur verts ou rouges **expliqués** | 1 h 30 |
| 3.6 | **Clôture** : `AUDIT-ULTRA.md` §12 mis à jour (ce qui a changé), `PROGRESS.md`, `DECISIONS.md`, tableau de bord du tour 5 régénéré, message de fin à Benjamin avec les 10 points téléphone (§6) | docs | — | 30 min |

## 4. Critères de sortie (« banc fiable » = tout vrai)

1. `scripts/bench/selftest.sh` : 100 % vert, 0 dérogation, chaque test sauté nommé avec sa raison dans la liste de quarantaine.
2. Un lot ne peut plus démarrer sans tampon vert ; une raison hors liste est refusée.
3. Tour de fumée : 4 sièges, 0 gel, 0 incident « gel/moteur », chaque réponse du banc nomme son siège.
4. Les 24 sièges du tour 5 sont rejouables depuis le dépôt ; les 8 items ≥ grave ont leur rejeu « non tenu ».
5. `base.tsv` connaît les tours 1 à 5 ; la fusion du tour 5 affiche des « revient » là où la synthèse disait [T4].
6. `tableau-de-bord.py` : 0 « ? » sauf H2 (téléphone) ; échelle v1/v2 visible ; « ≥ 16 joués, ≤ 3 coupés » calculé.
7. `verdicts.tsv` : les 23 « banc? » du tour 5 tranchés ou « non tranché : téléphone » avec la ligne de recette correspondante.
8. Promesses : catalogue P001-P120 ; `fabrique-sieges.py` versionné ; D compte des sièges distincts.
9. Les scripts v2 (atelier, rejeu, fusion, file Gradle) ont servi une fois pour de vrai (ce chantier) et leurs casses sont réparées.
10. `CLAUDE.md` porte la règle « l'outil de mesure se vérifie avant de mesurer ».

## 5. Risques et parades

- **Robolectric ne se laisse pas faire** (repos de Compose, fenêtres) : chaque attente du banc est déjà bornée (8 s) ; si un contrôle du §3.2 ne peut pas exister sur Robolectric, il devient une ligne « hors banc » du tableau, jamais un test sauté en silence.
- **Mémoire** (15 Go, 4 cœurs) : un seul Gradle, démon Kotlin arrêté, ≥ 6 Go avant chaque lancement (`gradle-file.sh`) ; pas de tour de fumée pendant un `selftest.sh`.
- **Temps** : chaque étape a sa preuve ; si une étape dépasse 2 fois sa durée, elle est coupée en deux ou reportée au tour 7, noté dans `banc-fiable-journal.md`, jamais contournée.
- **Quota de Benjamin** : les 3 séances s'arrêtent proprement ; « continue » reprend à la première étape non cochée du journal.
- **Tentation de corriger l'appli** : interdite ici ; un défaut de l'appli = verdict + test rouge, point.

## 6. Les 10 points téléphone pour Benjamin (après la séance 3, quand il veut)

1. Renommer un projet : toucher le champ, ✕ Effacer, taper, OK : ça répond tout de suite ? (T4-19)
2. Lancer un morceau : le compteur de mesures en haut avance ?
3. Envoyer le projet (.zip) : il apparaît dans « Mes exports » ?
4. Un export WAV : il est dans Musique › BenStudio, et sa taille est normale ?
5. Ouvrir Pad Live depuis l'accueil : un son part-il tout seul ?
6. « Prêt à partager ? » : la longueur exportée est bien celle réglée dans Mix ?
7. Juste après un export, l'alerte « le morceau a changé » apparaît-elle sans raison ?
8. Pad Live, « Tout couper » : la musique du projet s'arrête aussi ?
9. Mode simple, « Jouer » : tout le morceau, ou une boucle ?
10. Enregistrer une mélodie, « Retour » pendant la prise, passer par Pad Live, revenir : les notes sont encore là ?

Chaque réponse va dans `recette.json` et remplit H2 du tableau de bord.

## 7. Ce que le banc ne fera jamais (à savoir avant de le juger)

- **Entendre** : il sait qu'un son part, jamais s'il est beau, fort ou en place. Le son, c'est Benjamin.
- **Un vrai doigt, un vrai écran** : pas de geste imprécis, pas d'écran qui rame, pas de vibration ; les touchers sont des pixels.
- **Être le téléphone de Benjamin** : Honor, casque, batterie, 3 heures d'usage, mises à jour : hors banc, lignes H2.
- **Lire comme un humain** : un testeur IA lit tout ou rien (`lit=non`) ; la vraie malvoyance et TalkBack restent approchés (P4).
