# Lecture historique de l'audit ultra (tours 0 à 4) : ce que disent les commits, les décisions et les rapports

Lecteur « histo » du méta-audit, 2026-11-05. Dépôt `/home/user/fx-int` (branche `fx-int`, poussée comme `ccr-80af1a1b-mn0wec`, dernier commit `20ea449`). Lecture seule.

## Sources et façon de dater
- Heures des commits : `git log --date=iso` (UTC). 203 commits de `d6f307a` (3 nov. 14:15) à `20ea449` (5 nov. 15:10) ; 73 avant le plan révisé `c71762c` (premier Audit, étages 1 à 5), 130 depuis.
- Heures des sièges (testeurs) : dates d'écriture des fichiers `screen-N.txt` et `rapport.md` dans le scratchpad (`scratchpad/ultra/tour-N/lotX/s/`), des fichiers `synthese.md`, `consignes-*.md`, `notes-banc.txt`. Ce sont des dates de fichier, pas des horodatages écrits dans les rapports (les copies texte sous `docs/audits/ultra/sieges/` n'en portent aucune).
- CI : GitHub Actions (`gh run list`, runs 232 à 243), lu en lecture seule.
- Rapports : `docs/audits/ultra/tour-*.md`, `docs/audits/ultra/sieges/tour-N/*.{md,txt,tsv}`, `docs/DECISIONS.md` lignes 1127 à 1301, `docs/REVIEW.md` lignes 1139 à 1148, `docs/PROGRESS.md` ligne 10.
- Les rapports de zone ne citent presque jamais leurs commits (seuls `tour-1-zone3`, `tour-1-zone5-6`, `tour-2-zone3`, `tour-2-zone5`, `tour-2-zone6`, `tour-3-zone6` en citent) : l'attribution commit → équipe ci-dessous vient des messages de commit (« Round N zone K », « n° 13 to 19 ») et des numéros de synthèse qu'ils portent.

---

## 1. Chronologie par tour (heures UTC)

### Avant les tours : du premier Audit au plan « ultra »
| Quand | Quoi | Preuve |
|---|---|---|
| 3 nov. 15:05 | Plan « audit ultra » écrit (foule de testeurs, ~80 passages) ; 15:29 règles d'économie de quota ; 15:37 validé par Benjamin | `43cbe0a`, `cb1e88c`, `89c08d4` ; DECISIONS l. 1127-1128 |
| 4 nov. 06:33 | Tas des tests JVM 512 Mo → 2 Go, le banc attend le travail de fond en temps réel (tests instables) | `7e343fe` ; DECISIONS l. 1146-1147 |
| 4 nov. 10:19 | Plan révisé par un panel de 6 experts + un critique neuf : programmes d'abord, 8 sièges par tour, 3 à 5 tours, plafond 60/40 agents, étage 6 fusionné en tour 0 | `c71762c` ; DECISIONS l. 1155-1157 |
| 4 nov. 12:34 | Benjamin lance l'audit ultra ; équipes P0 A (banc 2) et B (programmes) | `233407d` ; DECISIONS l. 1163 |
| 4 nov. 12:53 → 13:59 | P0 : 25 commits (banc 2 : options de siège, écran en texte, rejeu, mesures, fusion ; tutos DE/AR, recensement des écrans, StrictMode, tests de mise à jour) ; fusions `60ea97d` 13:32 et `70f8899` 13:59 | `git log c71762c~1..846ddfa` |

### Tour 0 (4 nov.) : 3 éclaireurs
| Phase | Début → fin | Durée | Preuve |
|---|---|---|---|
| Suite complète après P0 (StrictMode en option, recensement sans captures) | 14:40 | — | `90a91cc` |
| Lots « Benjamin » en parallèle (intro « corde tendue », muet/solo, assistant, découpe « Un bout ») | 14:31 → 18:50 | — | `a6606d7`…`fa146f8` |
| Sièges a, b, c (parent pressé FR ; fan de remix EN 411 dp 130 % Simple ; producteur Pad Live tablette Pro) | 16:48 → 16:55 | 7 min | `scratchpad/ultra/tour-0/{a,b,c}/s/screen-*.txt`, `bench.log` 16:54 |
| Grappes (29 grappes, 0 vue par 2 sièges) | 16:55 | — | `scratchpad/ultra/tour-0/clusters.md` |
| Équipe de correction (banc + appli) | 17:30 → 19:02 | 1 h 32 | `34d56bb`, `aa5fc52`, `1b38738`, `1551290`, `d1b8d60`, rapport `e86f0bd` |
| Rangement (captures, contraste) et poussée | 19:09 → 19:10 | — | `5fa9f0d` ; CI 240 lancée 19:10 |
| CI 240 | 19:10 → 20:44 | 1 h 34 | émulateur rouge, le reste vert, APK non publiée |
| Pause : « le tour 1 ne démarre que sur le signal de Benjamin » | 19:01 | — | `82f9237` ; DECISIONS l. 1185 |

Bilan du tour 0 (`docs/audits/ultra/tour-0.md`) : 21 lignes corrigées, dont 14 verdicts « banc » (moteur du banc absent là où un téléphone l'a, FLAC, région de langue) et 7 « appli » ; 5 restes appli, 1 reste banc.

### Tour 1 (4 → 5 nov.) : 24 sièges, 5 équipes + banc
| Phase | Début → fin | Durée | Preuve |
|---|---|---|---|
| Lancement sur le mot de Benjamin ; élargi à 16 puis 24 testeurs pendant le tour | 19:12 ; 19:20 ; 19:39 | — | `d79d8d2`, `9267cf5`, `e89987f` ; DECISIONS l. 1191-1192 |
| Lot A (4 sièges) | 19:12 → 19:18 | 6 min | `tour-1/lotA/*/s/` |
| Gel des sièges A a et A d après ▶ → correctif banc, rejeux (`rejeu-a-23-192048`, `rejeu-d-20-193305`) | 19:20 → 19:38 | 18 min d'arrêt | `72bc474` 19:38 ; dossiers `lotA/rejeu-*` |
| Lot B (3 sièges + 1 amenée échouée) | 19:39 → 19:50 | 11 min | `tour-1/lotB/` ; `notes-banc.txt` |
| Lot C (4 + le rejeu de B c) | 19:51 → 20:03 | 12 min | `tour-1/lotC/e` 19:58 → 20:03 |
| Lot D (4) ; « ecris » sans champ pris pour un plantage → correctif banc | 20:05 → 20:13 ; 20:14 | 8 min | `c94967d` |
| Lot E (3 + 1 amenée échouée) | 20:15 → 20:22 | 7 min | `tour-1/lotE/` |
| Lot F (4 + le rejeu de E c) | 20:23 → 20:40 | 17 min | `tour-1/lotF/` |
| **Sièges, total** | 19:12 → 20:41 | **1 h 29** (24 sièges, 4 à la fois) | |
| Synthèse (42 points) et consignes des équipes | 20:42 | 1 min après le dernier siège | `scratchpad/ultra/tour-1/synthese.md` ; `7989a0c` 20:43 |
| Équipes (zone 1 ; zones 2+4 ; zone 3 ; zones 5+6 ; 4b « suite » ; banc) | 20:42 → 23:08 | 2 h 26 (1er commit 21:13) | `14e0b60` … `3189ee1` |
| Intégration : lignes mal placées remises, barre du pouce, tests suivis, captures | 23:10 → 02:20 | 3 h 10 | `9975fd6`, `58cf27e`, `018bdc5`, `f0b51e6` |
| Relecture neuve (en parallèle de l'intégration) : notes à 23:16, corrections 02:48 → 03:09, docs 03:29 | 23:16 → 03:29 | 4 h 13 (chevauche l'intégration) | `scratchpad/ultra/tour-1/relecture.md` 23:16 ; `bbba50b`…`0cc6cd2`, `b985cdb` |
| Poussée et CI 241 | 02:21 → 04:03 | 1 h 42 | émulateur rouge, build vert |
| Étiquette `ultra-1` posée en local seulement (proxy refuse les étiquettes) | 03:30 | — | `75c0909` ; DECISIONS l. 1239 |
| **Tour 1, total** | 19:12 → 03:30 | **8 h 18** | |

### Tour 2 (5 nov.) : 8 sièges, 6 équipes
| Phase | Début → fin | Durée | Preuve |
|---|---|---|---|
| Consignes testeur, lots A et B (2 × 4 sièges) | 03:31 ; 03:32 → 03:42 | 10 min | `tour-2/lot{A,B}/*/s/` |
| Notes banc, synthèse (31 points, avec « revient »), consignes des équipes | 03:42 → 03:43 | 1 min | `tour-2/notes-banc.txt`, `synthese.md` |
| Équipes (zones 1 à 6, chacune 1 à 3 commits) | 03:43 → 05:04 | 1 h 21 (1er commit 03:58) | `5c133ee` … `529f7d0` |
| Orchestrateur, en parallèle : test de fumée émulateur (rouge « depuis longtemps ») | 04:58 | — | `be9c456` ; DECISIONS l. 1247 |
| CI 241 (du tour 1) finit pendant que les équipes travaillent | 04:03 | — | run 41954692874 |
| Intégration : morceaux mal placés, 6 tests rouges, suite gelée 1 h 30, ~215 captures | 05:04 → 08:14 | 3 h 10 | `241d28b`, `29578fc`, `9ab811a`, `ddc55b6` ; `tour-2-integration.md` |
| Préparation du tour 3 (sieges.tsv, consignes) pendant l'intégration | 06:17 | — | `scratchpad/ultra/tour-3/sieges.tsv` |
| Poussée (avec la ligne « promesse » du fuseur) et CI 242 | 08:15 → 09:14 | 59 min | `a8578c5` ; build ET émulateur rouges |
| Relecture neuve (1 Grave) | 08:14 → 09:18 | 1 h 04 | `fbcdebd`, `ff9b9c3` |
| **Tour 2, total** | 03:32 → 09:18 | **5 h 46** | |

### Tour 3 (5 nov.) : 24 sièges « promesses », 6 équipes + agent banc
| Phase | Début → fin | Durée | Preuve |
|---|---|---|---|
| Briefs des sièges ; lot A | 08:16 → 08:30 | 14 min | `tour-3/briefs/Aa.txt` 08:16 ; `lotA/*/s/` |
| Lot B ; siège B a gelé sur « ecris » dans « Nom du projet » (08:42) ; thread dump | 08:31 → 08:52 ; jstack 08:51 | 21 min | `tour-3/notes-banc.txt` ; `scratchpad/jstack-B-a.txt` |
| Lot C (après 26 min sans siège) | 09:18 → 09:23 | 5 min | `lotC/*/s/` |
| Lot D | 09:24 → 09:31 | 7 min | |
| Lot E ; siège E c gelé deux fois 5 min (champ « Name ») | 09:32 → 09:56 | 24 min | `notes-banc.txt` 09:57 |
| Lot F | 09:59 → 10:04 | 5 min | |
| **Sièges, total** | 08:16 → 10:04 | **1 h 48** | commencés avant la fin de la CI 242 (09:14) et avant la relecture du tour 2 (09:18) |
| Synthèse (38 points) et consignes | 10:05 | 1 min | `tour-3/synthese.md` |
| Décision « sans plafond de tours » | 10:23 | — | `2a335ec` ; DECISIONS l. 1270 |
| Agent banc (exports 3 s, Pad Live, lecture depuis une partie, gel du renommage = « appli ») | 10:41 | — | `60c7a2f` ; `tour-3-banc.md` |
| Équipes zones 1 à 6 | 10:05 → 11:29 | 1 h 24 (commits 11:03 → 11:29) | `2ce48c3` … `b3ee0d6` |
| CI 242 (rouge à 09:14) lue et notée | 11:26 | 2 h 12 après le rouge | `tour-3/ci-242.md` |
| Intégration : morceaux mal placés, CI 242, banc, suite (10 rouges), 175 captures, rapport | 11:57 → 13:49 | 1 h 52 (+ 28 min d'attente après le dernier commit d'équipe) | `5d59c88`, `de2faf9`, `531564f`, `7924d57`, `cdef9f8`, `3eb8968`, `7b7d74e` |
| Poussée et CI 243 | 13:50 → 15:23 | 1 h 33 | émulateur rouge (JourneyTest Simple), build vert |
| Relecture neuve (2 Moyens) | 13:49 → 13:58 | 9 min après le rapport d'intégration | `4dc642b`, `b1e678a` |
| **Tour 3, total** | 08:16 → 13:58 | **5 h 42** | |

### Tour 4 (5 nov.) : 20 sièges, puis pause
| Phase | Début → fin | Durée | Preuve |
|---|---|---|---|
| Consignes et sièges (rejeux des profils du tour 3) | 13:59 | 1 min après `ea72949` | `tour-4/sieges.tsv` |
| Lots A → E (4 sièges chacun) ; lot C a gelé sur « Effacer » (✕ du tour 3) | 14:00 → 14:43 | 43 min | `lot{A..E}/*/s/` ; `tour-4/notes-banc.txt` 14:28 |
| Règle du banc (Gradle tué par manque de mémoire, 2 fois le 5 nov.) | 14:04 | — | `scratchpad/ultra/regle-banc.txt` |
| CI 243 lue (émulateur rouge 14:38) | 14:42 | 4 min | `tour-4/ci-243.md` |
| Lot F : jamais lancé (dossiers créés, aucun écran) | — | — | `tour-4/lotF/*/start.txt` seuls |
| Prompt du méta-audit, puis pause (sièges copiés en texte dans `docs/`) | 15:05 ; 15:10 | — | `85fc8c3`, `20ea449` ; PROGRESS l. 10 (« 16:10 ») |
| Travail non commité laissé par l'agent « appli ou banc » interrompu | — | — | `git status` : `BenchWindows.kt`, `HumanTester.kt` modifiés, `BenchLaunchLines.kt` nouveau (+ `BenchTour4Test.kt`, `Tour4AppProofTest.kt` cités par PROGRESS) |

### Temps par phase, tours 1 à 3 (le cœur du cycle)
| Tour | Sièges | Équipes | Intégration | Relecture | Poussée → CI finie | Total |
|---|---|---|---|---|---|---|
| 1 | 1 h 29 (24) | 2 h 26 | 3 h 10 | en parallèle (4 h 13) | 1 h 42 (rouge) | 8 h 18 |
| 2 | 10 min (8) | 1 h 21 | 3 h 10 | 1 h 04 | 59 min (rouge) | 5 h 46 |
| 3 | 1 h 48 (24) | 1 h 24 | 1 h 52 | 9 min | 1 h 33 (rouge) | 5 h 42 |

Lecture : l'intégration coûte autant ou plus que les équipes (3 h 10 contre 1 h 21 au tour 2) ; la synthèse est écrite en 1 minute après le dernier siège ; les tours se chevauchent (tour 3 testé avant la relecture du tour 2 et avant que la CI 242 ait rendu son verdict ; CI 242 lue 2 h 12 après son rouge). La CI n'a jamais été verte pendant l'audit ultra : les 12 runs 232 à 243 (4 nov. 07:08 → 5 nov. 13:50) sont « failure », l'étape « Publish APK » toujours sautée ; dans 240, 241 et 243 seul le test de fumée sur émulateur est rouge, dans 242 le job « Build, unit tests, screenshots » aussi (`ForegroundSoundTest`, `BenchAmeneesTest` NPE, `tour-3/ci-242.md`).

---

## 2. Commits par tour et par équipe, fichiers touchés par plusieurs équipes

Comptage par `git log --name-only` sur les plages de commits (`scratchpad/meta/histo_files.py`, sortie `histo_files.out`). Captures `.png` et `docs/` exclues du comptage des fichiers partagés.

### Tour 0 (`846ddfa..5fa9f0d`, 26 commits)
- Banc : 3 (`34d56bb`, `1b38738`, `1551290`) ; équipe appli : 2 (`aa5fc52` 54 f., `d1b8d60` 16 f.) ; orchestrateur et lots Benjamin : 11 (intro, muet/solo, assistant, découpe « Un bout » `3056251` 55 f. et `b9069ac` 35 f., pads `fa146f8` 72 f.) ; docs : 8 ; rangement : 1 (`5fa9f0d` 24 f.).
- Fichiers touchés par ≥ 2 équipes : 6 (`CutScreen.kt`, `RhythmViewModel.kt`, `SoundLibraryViewModel.kt`, `WizardScreen.kt`, `recensement-acceptes.tsv`, `en/restes.json`).

### Tour 1 (`5fa9f0d..75c0909`, 33 commits)
| Équipe | Commits | Fichiers | Détail |
|---|---|---|---|
| Zone 1 (Remix → pads, Pad Live, défis) | 2 | 114 | `ae3ea45` (96 f.), `844415d` captures |
| Équipe 2 = zones 2 + 4 (exports, master, enregistrement) | 2 | ~105 | `e0d2157` (96 f., n° 13-19, 29-31), `684bc03` captures |
| Zone 3 (navigation, prise en main) | 5 | 163 | `14e0b60`, `7f79b39`, `b09096f` (54 f.), `7d14ef1` (162 captures), `57886bb` |
| Zones 5 + 6 (équipe 4) et « suite » (équipe 4b) | 4 | 145 | `05801b8` (103 f.), `b38b81d`, `f6bf551` (37 f.), `3189ee1` |
| Banc | 3 | 13 | `72bc474`, `c94967d`, `5b2b940` |
| Intégration | 5 | 336 | `9975fd6`, `58cf27e` (21 f.), `018bdc5` (14 f.), `dfd1fb6`, `f0b51e6` (296 captures) |
| Relecture neuve | 6 | 83 | `bbba50b` (24 f.), `7f7b832`, `f4d7184`, `297f6c0`, `0cc6cd2` (42 f.), `b985cdb` |
| Docs / orchestrateur | 6 | 4 | `d79d8d2`, `9267cf5`, `e89987f`, `7989a0c`, `bd150c1`, `75c0909` |

Fichiers touchés par ≥ 2 équipes : **62**. En tête : `app/ui/AppRoot.kt` (6 équipes, 8 commits), `app/rhythm/RhythmViewModel.kt` (6 équipes, 6 commits), `model/i18n/I18n.kt` (4 équipes, 5 commits), `onboarding/Welcome.kt` (4/4), `ui/TopBar.kt` (4/4), `human/HumanTester.kt` (3/5), `library/SoundLibraryViewModel.kt` (3/4), `audiotracks/AudioTracksScreen.kt`, `mixer/MixerScreen.kt`, `padlive/PadLiveScreen.kt`, `projects/NewProjectPage.kt` (3/3 chacun), et les 15 catalogues `i18n/*/tour1nav.json` (3/3). `docs/DECISIONS.md` : 12 commits.

### Tour 2 (`75c0909..ff9b9c3`, 20 commits)
| Équipe | Commits | Fichiers | Détail |
|---|---|---|---|
| Zone 1 | 2 | 80 | `f597f7d` (79 f.), `de9d32e` (rattrapage d'un patch mal appliqué) |
| Zone 2 | 2 | 65 | `90f36a0` (test hôte), `3b286fe` (64 f.) |
| Zone 3 | 1 | 61 | `66f4488` |
| Zone 4 | 1 | 42 | `69a60ab` |
| Zone 5 | 3 | 59 | `d6e2dfc`, `c494208` (52 f.), `529f7d0` |
| Zone 6 | 2 | 63 | `5c133ee` (32 f., coréen/japonais), `a70337d` (32 f.) |
| Intégration | 4 (+ 1 captures) | 16 (+ 217) | `be9c456`, `241d28b`, `29578fc`, `ddc55b6`, `9ab811a` |
| Banc | 1 | 1 | `a8578c5` (fuseur : « promesse ») |
| Relecture neuve | 2 | 6 | `fbcdebd`, `ff9b9c3` |

Fichiers touchés par ≥ 2 équipes : **21**. `AppRoot.kt` (6 équipes, 7 commits), `RhythmViewModel.kt` (5/5), `I18n.kt` (4/5), `onboarding/LaunchChoice.kt` (4/4), `app/src/test/.../ScreenshotTest.kt` (4/4), `export/ShareSheet.kt` (3/3), `rhythm/RhythmScreen.kt` (3/3). `DECISIONS.md` : 10 commits.

### Tour 3 (`ff9b9c3..ea72949`, 24 commits)
| Équipe | Commits | Fichiers | Détail |
|---|---|---|---|
| Zone 1 | 2 | 50 | `151c4c9` (36 f.), `ae04cc4` (16 f.) |
| Zone 2 | 4 | 67 | `0bb71bf` (65 f.), `5e49267`, `958c78c`, `ca85bfd` (DECISIONS dédoublé) |
| Zone 3 | 1 | 27 | `b804a22` |
| Zone 4 | 1 | 20 | `ff77b35` |
| Zone 5 | 1 | 102 | `b3ee0d6` |
| Zone 6 | 3 | 218 | `2ce48c3` (206 f., pluriels dans 15 langues), `4d8f967`, `700872e` |
| Banc | 2 | 10 | `60c7a2f`, `531564f` |
| Intégration | 6 | 191 | `5d59c88`, `de2faf9`, `7924d57`, `cdef9f8` (12 f.), `3eb8968` (164 captures), `7b7d74e` |
| Relecture neuve | 2 | 22 | `4dc642b` (18 f.), `b1e678a` |
| Docs | 2 | 2 | `2a335ec`, `ea72949` |

Fichiers touchés par ≥ 2 équipes : **56**. `RhythmViewModel.kt` (6 équipes, 7 commits), `AppRoot.kt` (6/6), `I18n.kt` (5/5), `help/ScreenHelp.kt` (4/4), `live/LiveScreen.kt` (3/4), `RhythmScreen.kt` (3/4), `HumanTester.kt` (3/4), `human/Tour3RenameIdleTest.kt` (3/4 : banc, zone 3, intégration, chacun avec un verdict différent), `ShareSheet.kt` (3/3). `DECISIONS.md` : 11 commits.

### Sur les trois tours
- `AppRoot.kt` (4 809 lignes) : 21 commits, 6 équipes par tour à chaque tour. `RhythmViewModel.kt` (11 746 lignes) : 18 commits, 5 à 6 équipes par tour. `I18n.kt` : 15 commits. Ce sont exactement les trois fichiers nommés « partagés » dans les consignes (`consignes-equipes.md` de chaque tour) et les trois où les morceaux mal placés sont apparus (§3).
- `docs/DECISIONS.md` : 33 commits en trois tours, écrit par toutes les équipes à la fois (un dédoublage nécessaire, `ca85bfd`).
- Tous les commits d'équipe sont gros (36 à 206 fichiers) parce qu'un texte nouveau se traduit dans 15 catalogues et que les captures changent.

---

## 3. Incidents d'intégration et d'exploitation

### 3.1 Morceaux (hunks) mal placés par l'index partagé
| Tour | Quoi | Preuve |
|---|---|---|
| 1 | « put back the lines the teams' merges misplaced (wizard length, Guide's « Fait », thumb bar, master gain, stem pad lane) » : `AppRoot.kt` (166 lignes bougées), `RhythmViewModel.kt` (54), `SoundLibraryViewModel.kt`, `MixerScreen.kt`, `ThumbBar.kt`, `TopBar.kt` | `9975fd6` (23:10) |
| 2 | `ShareSheet.kt` : argument `target =` après la parenthèse fermante (ne compilait pas) et ligne « Volume visé » dans la mauvaise colonne ; `RhythmViewModel.kt` : `var reportOk` déclaré dans la boucle de rendu ; `ScreenshotTest.kt` : argument après le `Modifier` ; trois commentaires/KDoc séparés de leur code (`AppRoot.kt`, `ThumbBar.kt`, `SynthScreen.kt`) | `241d28b` ; `tour-2-integration.md` § « Morceaux mal placés » |
| 2 | La zone 1 elle-même rattrape « a patch put them after the screen's end » (arguments du défi de Pad Live hors de l'appel) | `de9d32e` 05:03 |
| 3 | `RhythmViewModel.kt` (rendu d'une partie du remix avant la description du projet, résultat d'export lu avant le rendu, message d'annulation effacé dans Rétablir, KDoc empilées), `AppRoot.kt` (`sendProject` collé entre deux instructions, `effectsActions` coupé), `HumanTester.kt` (constantes du banc entre `@RunWith` et la classe) : restés **non commités** dans l'arbre de travail, remis en place par l'intégrateur | `5d59c88` ; `tour-3-integration.md` § « Restes non commités » |

### 3.2 Commits qui embarquent ou laissent le travail d'une autre équipe
| Tour | Quoi | Preuve |
|---|---|---|
| 1 | Zone 3 commite `ThumbBar.kt` avec « Annuler » parce que la clé « Annuler (défaire) » de l'équipe 6 n'est pas encore commitée ; laisse 2 imports de `HumanTester.kt` non commités (compilation du banc cassée en cours d'édition) ; ses 162 captures « peuvent porter des changements non commités » des autres | `tour-1-zone3.md` l. 29-30 |
| 1 | Zones 5-6 laissent aux autres les noms des curseurs de `MasterPro.kt` (« fichier réécrit par l'équipe 2 ») et `ThumbBar.kt` (« fichier neuf de l'équipe 3 ») | `tour-1-zone5-6.md` l. 29 |
| 2 | « une partie d'AppRoot, I18n et PlayStrip est partie dans `de9d32e` de la zone 1, l'index étant partagé » | `tour-2-zone5.md` l. 24 |
| 2 | Zone 6 corrige 3 lignes dans le fichier **non commité** de la zone 5 (`ko/ultra2z5.json`) ; captures ré-enregistrées non commitées « elles montrent aussi le travail en cours des autres zones » | `tour-2-zone6.md` l. 23 et 38 ; `tour-2-zone3.md` l. 32 |
| 3 | L'index partagé a copié les 5 lignes DECISIONS de la zone 2 dans le commit de la zone 6 (`2ce48c3`) | `ca85bfd` (−5 lignes) |
| 3 | Le point 7 de la zone 1 (coin des pads nommé) est « corrigé par une autre équipe » via une modification non commitée de `PadLiveScreen.kt`, que la zone 1 ne touche pas | `tour-3-zone1.md` l. 14 |
| 3 | `Tour3RenameIdleTest.kt` écrit par le banc (preuve « appli », `@Ignore`), réécrit par la zone 3 (verdict « banc »), réécrit par l'intégration (vérifie le renommage, plus de `@Ignore`) | `60c7a2f`, `b804a22`, `531564f` |

### 3.3 Fichiers égarés
- `app/src/main/java/com/pocketstudio/app/ui/AppRoot.kt.orig` laissé dans l'arbre au tour 3 (reste d'un `git apply` en trois voies), effacé avant commit (`tour-3/ci-242.md` point 4 ; `tour-3-integration.md`).
- Tour 4 : cinq fichiers de test non commités laissés par l'agent « appli ou banc » interrompu (`BenchWindows.kt`, `HumanTester.kt`, `BenchLaunchLines.kt`, `BenchTour4Test.kt`, `Tour4AppProofTest.kt`) : PROGRESS l. 10 ; `git status` en montre trois.

### 3.4 Tests rouges réparés à l'intégration (le coût caché)
| Tour | Rouges et cause | Preuve |
|---|---|---|
| 1 | Les tests devaient « suivre ce que les équipes ont changé » (Retour mène à l'accueil, carte du kit à la place du panneau, bout sur le pad 9, étoiles du défi…) ; plafonds du recensement relevés « chacun avec sa raison » ; barre du pouce qui ne laissait plus de place aux touches à 130 % (défaut créé par la zone 3, trouvé à l'intégration) ; suite complète 7 855 tests, 2 rouges relancés verts (`RemixSongScreenshotTest`, `ForegroundSoundTest` « journal lu trop tôt sous charge ») | `018bdc5`, `58cf27e` ; DECISIONS l. 1226-1228 ; `tour-1-relecture.md` |
| 2 | 6 rouges : `RemixFindableTest` ×2 (les cartes « Enregistrer ma voix » et « J'apprends » de la zone 4/3 poussaient le Remix sous le pli), `ScreenScanTest` (accueil : textes du studio comptés sous l'écran opaque), `SimpleModeAuditTest` (question « 174 Mo » de la zone 6), `ViewModelFuzzTest` (`runBlocking { flushNow() }` : **toute la suite gelée 1 h 30**) ; 2e passage 80 rouges = 79 captures changées + `ForegroundSoundTest` ; ~215 captures ré-enregistrées ; `master_cpu_budget` 2,27 % pour 2 % (charge) | `tour-2-integration.md` |
| 3 | 10 rouges : anciens tests contredits par les décisions des zones (bout sur un pad live vide, compte de la prise, dossier du kit, poignée « Replier », durée réelle du rendu du banc), « Pas » doublé dans le lexique, fuzz bloqué sur la fenêtre « Renommer » ; 2e passage 7 984 tests, 0 échec ; 175 captures ré-enregistrées ; `master_cpu_budget` 2,4 % (charge) | `cdef9f8`, `3eb8968` ; `tour-3-integration.md` |
| 3 | CI 242 (poussée du tour 2) rouge depuis 09:14 : intro figée sur l'émulateur (`delay` en mouvement réduit), `ForegroundSoundTest` (ouverture du projet revenue sur le fil principal), `BenchAmeneesTest` NPE ; corrigé seulement à l'intégration du tour 3 | `tour-3/ci-242.md` ; `de2faf9`, `531564f` |

### 3.5 Mémoire, disque, conteneur
| Quand | Quoi | Preuve |
|---|---|---|
| 4 nov. 06:33 (avant l'ultra) | Suite complète étouffée à 512 Mo par travailleur (centaines de GC complets, un tuto Pro à 1 130 s au lieu de 15) → 2 Go (3 Go pour les bancs) | `7e343fe` ; DECISIONS l. 1146 |
| 4 nov. (tour 0 / 1) | Cache de construction Gradle coupé sur la machine de l'agent : « il remplissait le disque (2 à 3 Go en une heure) » | DECISIONS l. 1190 |
| Tour 1, consignes | « Disque serré (~2 Go) : pas de nouvel atelier, pas de clean » ; Gradle toujours sous `flock gradle-b.lock` | `tour-1/consignes-equipes.md` |
| Tour 1, intégration (02:20) | La vidéo du banc (Robolectric écrit des images brutes) : « 3 Go et disque plein » → le test s'arrête dès les premières images | `018bdc5` ; DECISIONS l. 1230 |
| Tour 2, consignes | Disque ~3 Go | `tour-2/consignes-equipes.md` |
| Tour 3, consignes (10:05) | « MÉMOIRE serrée (un Gradle a été tué par manque de mémoire) : un seul Gradle à la fois, toujours sous le flock » ; disque ~7 Go | `tour-3/consignes-equipes.md` |
| 5 nov. 14:04 | « deux démons Gradle vivants en même temps (4-5 Go chacun) = manque de mémoire, le système tue le banc (arrivé 2 fois le 5 nov) » → `./gradlew --stop` et tuer les JVM orphelines avant chaque lot | `scratchpad/ultra/regle-banc.txt` ; copié dans `docs/audits/ultra/sieges/regle-banc.txt` |
| Tour 3 sièges | Siège B a figé 10 min (08:42 → 08:52), siège E c deux fois 5 min : le banc attend un repos de Compose qui ne vient jamais sous un dialogue à champ de texte ; « vu par Benjamin avant moi » → règle de veille (5 min sans écran = banc figé, jstack) | `tour-3/notes-banc.txt` ; `jstack-B-a.txt` 08:51 |
| Redémarrage du conteneur | Aucun redémarrage noté pendant l'audit ultra. Risque connu (DECISIONS l. 1017, 2 nov. : « le scratchpad peut disparaître au redémarrage du conteneur ») : c'est la raison de la copie des sièges en texte dans `docs/` à la pause (`20ea449`, 275 fichiers). Non vérifié au-delà. |
| Journaux | Aucun journal du scratchpad ne garde la trace d'un `OutOfMemoryError` ou d'un « Killed » : les deux morts par manque de mémoire ne sont attestées que par `regle-banc.txt` et les consignes du tour 3 (non vérifié dans un journal). |

### 3.6 Banc : faux défauts qui ont coûté un lot ou une équipe
- Tour 1 : lot A gelé (2 sièges sur 4 rejoués), 2 amenées échouées (B c, E c, rejoués en C e et F e), « ecris » sans champ compté plantage (lot D) : `72bc474`, `c94967d`, `tour-1/notes-banc.txt`. Dans la synthèse du tour 1, 6 points marqués « banc ? » (n° 4, 10, 16, 19, 29, 40) ; l'équipe banc en classe 8 « banc » (n° 4, 10, 16, 19, 27, 29, 34, 40, dont 27 et 34 non marqués dans la synthèse) plus les deux amenées et « rien ne change » (`tour-1-banc.md`) ; le n° 13 est tranché « banc » par l'équipe 2 (DECISIONS l. 1209).
- Tour 2 : « Poser » muet (`UnsatisfiedLinkError`), Pad Live vide, export 3 s et LUFS identiques, contrastes `LAYOUT CONTRAST` : banc (DECISIONS l. 1242, 1244, 1258-1259 ; `tour-2/notes-banc.txt`).
- Tour 3 : export 3 s, ▶/■ de ligne et « Tout couper », lecture depuis une partie : banc (`60c7a2f`) ; gel du renommage : « appli » pour l'agent banc (10:41), « banc » pour la zone 3 (`Zone3DialogDiagTest`, DECISIONS l. 1286), tests réécrits par l'intégration (`531564f`) ; au tour 4 le gel revient sur « Effacer » (`tour-4/notes-banc.txt`, non tranché).

---

## 4. Trouvailles des relectures neuves, par tour, et leur origine

Règle lue dans chaque rapport : relecteur qui n'a pas écrit le code, diff du tour, chaque correction avec un test rouge avant. Origine établie par `git log <plage du tour> -- <fichier>` : « même tour » = le fichier cité a été modifié par un commit d'équipe du tour.

### Tour 1 (`tour-1-relecture.md`, notes `sieges/tour-1/relecture.md`) : 0 Grave, 6 Moyens, 6 Mineurs, + 3 restes d'intégration
| Point | Gravité | Origine (commit du même tour) | Même tour ? |
|---|---|---|---|
| M1 plusieurs bouts sans pad libre écrasent Kick, Kick 2, Caisse (`StemBoardRouter.pickPad`) | Moyen | zone 1 `ae3ea45` (n° 2 « pieces never cover a kit pad unasked ») | oui : la correction du n° 2 laissait un chemin |
| M2 « Découper ce que je veux » transforme une boucle en morceau de 90 mesures | Moyen | zone 1 `ae3ea45` (n° 1) | oui |
| M3 page de Pad Live remplie seule effacée par Annuler, pack touché ignoré (`PadLiveController`) | Moyen | zone 1 `ae3ea45` (n° 5) | oui |
| M4 « Montre-moi » contre la règle stricte des pads du rôle (`TutoChecks`) | Moyen | zones 5-6 `05801b8` (n° 34) | oui |
| M5 « + Copie » prend une scène déjà dans le morceau (`newSongPart(copy)`) | Moyen | zone 3 `b09096f` (n° 24) | oui |
| M6 Streaming sur tout le morceau sans le dire (`LoudnessTarget.wholeSong`) | Moyen | équipe 2 `e0d2157` (n° 13-15) | oui |
| m son gardé à moitié appliqué (`applySynthFavorite`) | Mineur | 4b `f6bf551` (n° 35) | oui |
| m « moteur pas prêt : réessaie » trompeur | Mineur | équipe 2 `e0d2157` (n° 29) | oui |
| m nom de fichier coupé sur une demi-lettre (`WavExporter.take(40)`) | Mineur | équipe 2 `e0d2157` (n° 17) | oui |
| m `NameField` avale une composition CJK | Mineur | zone 3 `b09096f` (n° 26) | oui |
| m dossier avec seulement `project.json.tmp` écrasé (`ProjectOpening`) | Mineur | zone 3 `7f79b39` (n° 22) | oui |
| m « C'est fait » du Guide sans changement (`GuideViewModel`) | Mineur, non corrigé | fichier touché par zone 3 `b09096f` (n° 25) ; la cause peut être antérieure (non vérifié) | probable |
| Restes d'intégration : « Acid 303 » coupé, mot des pads groupés, « Rétablir » introuvable | — | intégration `58cf27e` (barre du pouce), zone 1 / zones 5-6 | oui |

12 points sur 12 viennent des changements du tour (dont 3 Moyens sur la seule correction du n° 2/n° 5 de la zone 1). Relecture commencée à 23:16, en parallèle de l'intégration ; les 3 restes d'intégration viennent de ce chevauchement.

### Tour 2 (`tour-2-relecture.md`) : 1 Grave, 0 Moyen, 7 Mineurs
| Point | Gravité | Origine | Même tour ? |
|---|---|---|---|
| G1 premier projet « intact » effacé pour de bon alors qu'une prise l'attend dans `takes/` ou dans son journal Live (`CurrentProject.takeUnusedStarter`) | Grave | zone 3 `66f4488` (n° 14) ; le test de l'équipe `noProjectNobodyMadeInMyProjects` « ne vérifiait que l'effacement, jamais qu'un projet utilisé reste » | oui |
| m1 effacement sans corbeille ; m2 message du sauvetage faux ; m3 projet redevenu identique effacé | Mineur | même n° 14 | oui |
| m4 « 1 » partagé entre un son sans numéro et « Prise 1 » (`takesInOrder`) | Mineur | zone 4 `69a60ab` (n° 18) | oui |
| m5 `SelectAllTextField` avale le premier déplacement du curseur après un focus clavier | Mineur | zone 5 `d6e2dfc` (n° 26) | oui ; devient au tour 3 le « suspect principal » du gel du renommage (`tour-3/notes-banc.txt`) |
| m6 `WavExporter.copyInto` : second essai MIME sur téléphone plein | Mineur | zone 2 `3b286fe` (n° 10) | oui |
| m7 `ArrivalGuard` : drapeau lu en composition | Mineur | zone 5 `c494208` (n° 22) | oui ; `ArrivalGuard` retiré au tour 3 (DECISIONS l. 1293) |

8 sur 8 du même tour. Le Grave est passé à travers 6 équipes, l'intégration, 7 909 tests et ~215 captures : seul le relecteur a posé le scénario « enregistrer puis « Nouveau morceau » tout de suite ».

### Tour 3 (`tour-3-relecture.md`) : 0 Bloquant, 0 Grave, 2 Moyens, 8 Mineurs
| Point | Gravité | Origine | Même tour ? |
|---|---|---|---|
| toucher la courbe de la découpe cochait le bout touché (envoi de bouts jamais choisis), `CutWave.kt` | Moyen (« choix de design ») | zone 1 `151c4c9` (n° 9) | oui |
| une étoile du Défi rendait l'export « périmé » (`ExportUi.madeHash`) | Moyen | zone 2 `0bb71bf` (n° 12) | oui |
| 1 rendu « une partie du remix seule » sans test ; 2 `madeHash` sur tout le projet à chaque modification | Mineur | zone 2 `0bb71bf` | oui |
| 3 `ProjectOpeningViewModel.opened` sans `@Volatile` | Mineur | **intégration** `de2faf9` (CI 242) | oui, par l'intégrateur |
| 4 « 0 mesures » en français (`trCount`) ; 6 `Contractions` sur un nom tapé | Mineur | zone 6 `2ce48c3` (n° 38) | oui |
| 5 `PackSearch.fold` retire signes voyelles hindi et dakuten ; 7 bloc kit invisible panneau replié ; 8 regex de `tutoPadNames` à chaque recomposition | Mineur | zone 5 `b3ee0d6` (n° 33, 28, 29) | oui |

10 sur 10 du même tour. Le relecteur note aussi un test d'équipe qui ne prouve pas ce qu'il dit (`theRemixGivesOneFilePerStem` : liste des fichiers seulement).

### Bilan des trois relectures
1 Grave, 8 Moyens, 21 Mineurs ; **30 points sur 30 nés des corrections du tour même** (aucun défaut préexistant remonté par les relectures) ; 0 Bloquant. Les entrées correspondantes de `docs/REVIEW.md` (l. 1146-1148) ne reprennent que les comptes et les restes.

---

## 5. Décisions prises pendant l'audit qui ont changé la méthode en marche

Source : `docs/DECISIONS.md` (numéro de ligne), commits, notes du scratchpad. Classées par ce qu'elles changent.

### Le cadre (plan, arrêt, nombre de tours)
- l. 1127-1128 (3 nov.) : second audit « ultra » décidé ; validé par Benjamin ; la règle de quota est retirée du plan (il gère la limite lui-même).
- l. 1155-1157, `c71762c` (4 nov. 10:19) : plan réécrit après un panel de 6 experts : programmes d'abord, agents seulement pour juger ; 8 sièges par tour puis 6 ; 3 à 5 tours ; plafond 60 agents lourds / 40 légers ; étage 6 du premier Audit = tour 0 ; un seul jeu de critères d'arrêt (§7) ; aucun testeur IA ne juge le son.
- l. 1159 : gel : toute fonction nouvelle va dans le lot « après l'arrêt ».
- l. 1185, `82f9237` (4 nov. 19:01) : pause après le tour 0, tour 1 seulement sur le mot de Benjamin (le plan enchaînait).
- l. 1191, `9267cf5` (19:20) : tour 1 élargi de 8 à 16 testeurs « sur demande de Benjamin » ; l. 1192, `e89987f` (19:39) : 24 testeurs, 4 à la fois (`maxParallelForks=4`), « au-delà, le goulot est la correction, pas les testeurs » ; plafond du §8 « dépassé d'autant, accepté ».
- l. 1212, `bd150c1` : Benjamin veut d'abord les trouvailles en clair après chaque lot.
- l. 1270, `2a335ec` (5 nov. 10:23) : **plus de plafond de 5 tours** ; arrêt = deux tours de suite sans Bloquant/Grave confirmé côté appli, sans « revient », ≥ 90 % de promesses tenues ; chaque correction garde un test qui rejoue les gestes du testeur ; chaque tour couvre la carte des fonctions (zones jamais visitées d'abord). Remplace le §7 du plan sans réécrire le plan.
- `a8578c5` (08:14) et `tour-3/sieges.tsv` : le rapport de siège gagne un champ « promesse » ; le tour 3 devient « l'appli tient-elle ses promesses ? » et le tour 4 rejoue les profils du tour 3 (`tour-4/sieges.tsv` : « (rejeu tour 3) »).
- l. 1301, `85fc8c3`/`20ea449` (15:05-15:10) : pause après le tour 4 pour un méta-audit de la méthode.

### Qui tranche « banc ou appli »
- l. 1157 : aucun testeur IA ne juge le son.
- `tour-0.md`, l. 1186 : règle du tour 0 : « banc = le banc ment là où un téléphone a son moteur » ; le moteur du banc vient pour ce qu'un téléphone fait (prise, lumières, packs, écoutes).
- l. 1189 : §4.4 appliqué : un Grave vu par un seul profil (Remix en Simple) attend un second siège avant correction.
- l. 1205 : n° 27 (son changé → boucle de l'Intro) tranché « banc » ; au banc de relancer `toggleSong`, pas ▶.
- l. 1209-1210 : n° 13 et n° 17-18 tranchés en partie banc (sinusoïde unique, JVM en ASCII) : `LC_ALL` ajouté au build des tests.
- l. 1244 (tour 2) : « club = streaming » tranché **sans le banc** : le test hôte mesure les WAV écrits ; le moteur du banc applique désormais la cible.
- `tour-3/synthese.md` en-tête : un agent « banc » à part dont « les verdicts priment » ; chaque « revient » doit expliquer pourquoi la correction n'a pas tenu, avec un test qui rejoue les gestes exacts du siège.
- l. 1271 (10:41) puis l. 1286 (zone 3) : le même gel du renommage classé « appli » puis « banc » dans la même journée ; l. 1299 : le banc n'attend plus le repos de Compose devant un dialogue à champ de texte.
- `tour-3/notes-banc.txt` : règle de veille (siège sans écran depuis 5 min = banc figé → jstack, et le testeur écrit son rapport).

### Le banc lui-même, retouché pendant les tours
- l. 1164-1165 (P0) : options de siège par qualificateurs, « gel » = même méthode en haut de pile > 700 ms, « rien ne change » ignore les écrans après « attends ».
- l. 1171 : StrictMode seulement avec `-Pstrict=on` (8 tests rouges au premier passage complet).
- l. 1172-1173 : recensement sans captures Roborazzi ; tolérance de pixels sur une capture.
- l. 1186-1188 (tour 0) : moteur du banc pour Pad Live, prises, packs FLAC ; `72bc474`, `c94967d` (tour 1, en cours de tour) ; l. 1221-1225 (tour 1) : amenées depuis l'accueil ou « Que veux-tu faire ? », faux micro, moteur pour Exporter/vidéo/Montre-moi, « regarde » = un regard, oracles tuto/synthe/libre/défi ; l. 1230 : vidéo coupée (disque), geste `double-touche`.
- l. 1258-1259 (tour 2) : le banc garde son moteur tant que la bibliothèque est ouverte ; fabrique le jeu de pads si Pad Live s'ouvre vide.
- l. 1271, `60c7a2f`, `531564f` (tour 3) : rendus de la durée demandée et dépendants du projet, pads qui suivent les lignes et « Tout couper », position de lecture qui avance, tous les fichiers d'un partage lus.
- `tour-2/consignes-testeur.md`, `tour-3/consignes-testeur.md`, `tour-4/consignes-testeur.md` : consigne testeur réécrite à chaque tour (non détaillée ici).

### L'organisation des équipes et de la machine
- l. 1145 (4 nov., nuit du premier Audit) : une équipe arrêtée la nuit est relancée sur la même mission.
- l. 1181 : Benjamin donne tous les pouvoirs de relance (équipes, sièges, banc, CI) ; tâche planifiée « Veille de l'audit ultra » toutes les 2 h.
- l. 1182, `8ccef7c` (18:09) : une minuterie par audit, lot et étape, heure de départ dans son nom (règle ajoutée à CLAUDE.md).
- `consignes-equipes.md` tours 1 → 3 : « CINQ » puis « SIX » équipes dans le même atelier ; commits par chemin, `git apply --cached` pour n'ajouter que son morceau, « ne reviens jamais sur les modifications d'une autre équipe », Gradle sous `flock`, tests ciblés seulement, disque ~2 Go → ~3 Go → ~7 Go, au tour 3 « un seul Gradle à la fois » (mémoire).
- l. 1190 : cache de construction Gradle coupé (disque) ; `regle-banc.txt` (14:04) : `gradlew --stop` et JVM orphelines tuées avant chaque lot de banc.
- l. 1163 : modèle des équipes = Opus 5.5 ; Fable 5.1 seulement pour les synthèses « sur demande » (les synthèses des tours 1-3 ont été écrites en 1 minute après le dernier siège, auteur non vérifié).
- l. 1239 : l'étiquette `ultra-1` ne peut pas être poussée (proxy) : la « zone changée depuis l'étiquette du tour précédent » du §7 n'a jamais pu s'appuyer sur une étiquette distante.
- PROGRESS l. 10 et `201facc` : aucune APK pendant l'audit tant que Benjamin ne la demande pas ; l'APK 338 (`61442a3`, 16:42) est faite en local, hors CI.

### Décisions produit prises « à chaud » qui ont redéfini des défauts
- l. 1207 (tour 1) : une seule cible de volume dans toute l'appli, Club = −8 LUFS partout (plus jamais −9) : cela fait disparaître les n° 13-14 plutôt que de les corriger un à un.
- l. 1240 (tour 2) : « Drop », « Break », « 808 » gardés en Mode simple : jargon accepté par décision, retiré des défauts.
- l. 1246, 1262 (tour 2) : « le partage garde son propre format (M4A), pas un défaut » ; « glisser les familles de sons : non reproduit hors banc ».
- l. 1263 puis 1293 : n° 28 (la carte du kit remplace le tuto) : tour 2 = protection d'1 s (`ArrivalGuard`), tour 3 (« 3e fois ») = l'offre entre dans le panneau et la protection est retirée « la touche des sièges arrivait des secondes après ». Un même défaut, trois réponses successives.
- l. 1257 (tour 2, zone 4) : « un toucher de plus pour les pads, assumé ».

---

## 6. Chiffres clés
- 130 commits d'audit ultra en ~29 h (4 nov. 10:19 → 5 nov. 15:10) ; 79 sièges joués (3 + 24 + 8 + 24 + 20), 4 jamais joués (tour 4 lot F), 4 rejoués (tour 1 : 2 gels, 2 amenées).
- Durée d'un tour complet : 8 h 18 (tour 1), 5 h 46 (tour 2), 5 h 42 (tour 3). Sièges : 1 h 29 / 10 min / 1 h 48. Intégration : 3 h 10 / 3 h 10 / 1 h 52, soit 33 à 55 % du tour.
- Points de synthèse : 29 grappes (tour 0), 42 (tour 1), 31 (tour 2), 38 (tour 3).
- Fichiers touchés par ≥ 2 équipes : 62 (tour 1), 21 (tour 2), 56 (tour 3) ; `AppRoot.kt` et `RhythmViewModel.kt` touchés par 5 à 6 équipes à chaque tour, 39 commits à eux deux en trois tours.
- Incidents d'index partagé : 4 commits de remise en place (`9975fd6`, `241d28b`, `de9d32e`, `5d59c88`), 1 dédoublage de DECISIONS (`ca85bfd`), 1 fichier `.orig`, 1 compilation cassée (`ShareSheet.kt`, tour 2), 6 mentions de travail laissé non commité pour une autre équipe.
- Tests rouges repris à l'intégration : 2 + 6 (+ 79 captures) + 10 (+ CI 242 : 3) ; une suite gelée 1 h 30 (tour 2).
- CI : 12 runs, 12 rouges, 0 APK publiée ; le test de fumée émulateur rouge sur les 12 ; CI 242 aussi rouge sur le build, lue 2 h 12 plus tard.
- Mémoire/disque : 1 montée du tas (512 Mo → 2 Go), 1 cache Gradle coupé (2-3 Go/h), 1 vidéo coupée (3 Go), 2 morts du banc par manque de mémoire le 5 nov. (attestées par une note, non par un journal).
- Relectures neuves : 1 Grave, 8 Moyens, 21 Mineurs, tous issus des corrections du tour même ; un même gel (n° 16 du tour 3) avec quatre verdicts successifs : « appli ou banc ? » (notes banc 09:57) → « appli » (agent banc 10:41) → « banc » (zone 3) → « non tranché » (tour 4, lot C a).
