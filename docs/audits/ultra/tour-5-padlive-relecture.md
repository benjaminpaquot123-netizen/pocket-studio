# Relecture du chantier 7 « Pad Live » (tour 5, étape 4)
Relu : 274844ce006f2297ef22f993ea7baa071ede8129

Relecteur neuf (ni auteur du code). Diff lu : `git diff bb4f1a26...HEAD` (moteur `ClipLauncher`, `RhythmViewModel`, `PadLiveScreen`, `AppRoot`, aides 14 langues, tests, captures), le rapport de l'auteur et les 6 lignes de DECISIONS.

## Moteur (priorité, lu ligne à ligne)
- `ClipLauncher::toggle` tourne dans le fil audio (vidage de la file, `Engine.cpp` `ToggleClip`). Ajout : un `std::none_of` sur les 64 cases avec un lambda sans capture allouée, des affectations de champs, un `store` relaxé sur l'atomique d'état déjà publiée. Ni allocation, ni verrou, ni log, ni JNI. Aucune nouvelle commande ni file : l'UI parle toujours par la SpscQueue (`post` → `nativePost`, 100 ms d'attente puis `droppedCommands`, inchangé).
- « Coup » seul, lecture arrêtée : `Off` → l'horloge repart à 0 (comme avant) ; `Playing` → `startBeat = beat_`, `Waiting`, et `render()` le relance au bloc suivant (`restart` fait fondre l'ancien passage, pas de clic). L'horloge des mesures n'est pas remise à zéro sur un retoucher : les lignes de mesure de « Enregistrer ce que je joue » restent justes.
- Boucles allumées, lecture arrêtée : un coup en attente n'est plus annulé (branche vide, état republié tel quel). Lecture en marche (`idleClock` faux) : rien ne change, donc les prises de perf (`LivePadTake`, sur le temps du transport) restent fidèles au moteur.
- `stopAll` (« Tout couper ») n'est pas touché ; toutes les cases repassent à `Off`, la case suivante relance l'horloge à 0. État remis à zéro : oui.
- `./scripts/host-tests.sh` : ALL PASSED (0 failures), 6 min 39.

## « Tout couper » coupe-t-il tout ?
- `cutEverything` = ■ du transport (s'il joue) puis `stopLive`. Le ■ du transport finit proprement ce qui enregistre : prise ● des notes (`stopRecordingTake`, gardée, un pas d'annulation), prise de perf (`endPerformanceOnStop` → `finishPerformance`, gardée), prise micro (le suivi voit le transport arrêté → `stopAudioRecording`, écrite par `TakeVault`). Aucune perte de prise.
- « Enregistrer ce que je joue » (rééchantillonnage moteur) : le transport arrêté ne le termine pas ; la prise continue, et se termine au toucher suivant sur une ligne de mesure (`clips_.render` tourne à chaque bloc, l'horloge des pads aussi) ou quand le tampon est plein. Rien n'est perdu (voir M1).

## Gestes
- `HeldEffect` : chaque chemin qui a allumé un effet l'éteint (glissé précoce : `holdNow(false)` ; fin du geste ou annulation : `finally` avec `on`). Montée ne part qu'au lâcher sans glissé. Aucun effet bloqué trouvé, y compris quand le parent consomme le défilement.

## Mutations
- Moteur M1 : `if (s.oneShot && alone && s.state != Playing)` (un coup qui sonne retouché ne repart plus tout de suite) → `padlive_hit_retouched_sounds_at_once` rouge (8 échecs). Tuée, restaurée.
- Moteur M2 : la branche « jamais annulé » retirée (`else if (false)`) → même test rouge (2 échecs, l. 478 et 480). Tuée, restaurée.
- Kotlin M3 : `cutEverything` sans le ■ du transport → `BenchPadLiveStopTest#padLivesStopAllIsInSightAndStopsTheTempoToo` rouge (« la musique du projet joue en boucle »). Tuée, restaurée.
- Kotlin M4 : glissé précoce sans `holdNow(false)` → `PadLiveGlideTest#aHeldEffectLetsGoAsSoonAsTheFingerGlides` rouge (`[brake true]`). Tuée, restaurée.

## Tests JVM ciblés (via `scripts/bench/gradle-file.sh`)
PadLiveGlideTest, BenchPadLiveStopTest, BenchPadLiveTest, Tour5AppProof3Test, Tour4AppProofTest, BenchTour4Test, PadLiveSceneTest, PadLiveTouchCountTest, HelpCoverageTest : 57 cas, 0 échec (2 « drive » du banc ignorés comme prévu).

## Captures regardées
padlive, padlive_more, padlive_small, padlive_recording, sizes/expanded_padlive : carré rouge « Tout couper » entier sur deux lignes, « Enregistrer ce que je joue » sur deux lignes sans coupure, « Plus » sans « Tout couper ». « F… » de padlive_small déjà présent à la base (pas de ce lot).

## Points restants (aucun grave)
| n° | gravité | constat | suite |
|---|---|---|---|
| M1 | moyen | Pendant « Enregistrer ce que je joue » (bouton voisin), « Tout couper » laisse la prise tourner : des mesures de silence s'ajoutent jusqu'au toucher suivant ou au plein. Rien n'est perdu. | Faire finir la prise sur sa ligne (`played.press()` si elle enregistre, `cancel()` si armée) dans `cutEverything`. |
| M2 | mineur | DECISIONS dit « ■ blanc » ; le code et les captures montrent un ■ noir (contraste 2,8:1 du blanc). | Corriger la ligne. |
| M3 | mineur | `Tour4AppProofTest` : la phrase « musique arrêtée » n'est vérifiée que par l'absence de mots ; c'est l'assertion sur le transport qui porte la preuve. | Chercher une phrase positive. |
| M4 | mineur | Montée s'allume dès le doigt posé alors qu'elle ne part qu'au lâcher (elle s'éteint sur un glissé). | Allumer au lâcher seulement. |
| M5 | mineur | Lecture arrêtée, une boucle allumée : un coup en attente ne peut plus être annulé en le retouchant (choix R.7 assumé). | Rien, noté. |

## Tests à rejouer sur téléphone
tel-08, tel-11, tel-12, tel-17 (comme le rapport), plus : « Enregistrer ce que je joue » lancé, puis « Tout couper », puis toucher le bouton : la prise doit se poser (M1).

## Delta : M1 et fusion de fx-int
- M1 corrigé (rouge 7d9dc3b2, vert 6aa204c5) : `cutEverything` appelle `PlayedTake.endForCut()` avant d'arrêter la lecture ; la prise qui enregistre finit sur sa ligne de mesure, une prise armée est lâchée, rien n'est armé sinon. Le test couvre les trois cas ; rien à redire.
- Fusion c3542fe1 (`git show --cc`) : trois conflits, les deux côtés gardés. `I18n.kt` liste « padlivestop » puis « accueil » ; DECISIONS garde les 7 lignes du chantier 7 puis celles de fx-int ; `verdicts.tsv` garde T5-PADRETRIG, T5-Ed-40 et T5-Ed-11 « corrigé » (ce lot) et T5-DEMO-VIDE, T5-Cc-23, T5-Cc-41c de fx-int. Aucune ligne perdue.
- Moteur : `git diff bb4f1a26 HEAD -- engine/` ne touche que `ClipLauncher::toggle` (et son test), jamais le rendu ni le synthé.
- Temps de calcul, `engine_tests` rejoué 3 fois en alternance (ligne « wide synth », limite 15 %) : base fx-int 12,5 / 11,9 / 13,6 %, atelier 12,6 / 12,2 / 13,1 % (charge 1,3 à 6,4, d'autres Gradle tournaient). Les chiffres se recouvrent ; la base seule a aussi raté une fois `master_tests.inc:326`, et l'atelier une fois `studio_tests.inc:345` à l'échauffement : bruit de machine, pas le lot. Aucune limite touchée.


## Delta après fusion de fx-int (2)

- Fusion 7daa06ef (`git show --cc`) : aucun conflit, aucune résolution manuelle. 400e3507 ne change que l'attendu de T5-PADRETRIG dans `test_ultra_bench.py` : la preuve moteur est maintenant `padlive_hit_retouched_sounds_at_once` (le défaut est corrigé) ; `verifie-chantier.py --preuves T5-PADRETRIG` sort exactement cette ligne, `test_ultra_bench.py` OK.
- Le rapport reçoit sa ligne « tests: » (commit `0f447527`), relu ici.

## Delta (3) : saut connu du banc, fusion de fx-int

- `d409a1dd` n'ajoute que la ligne `BenchPadLiveStopTest#drive` à `selftest-sauts-connus.txt`, au bon rang alphabétique, au format des autres ; la classe hérite de `HumanTester` ; le selftest ne signale rien (rc=0) ; `test_ultra_bench.py` OK.
- `274844ce` : fusion de fx-int (lot « access ») sans conflit ni résolution manuelle.

Verdict : 0 bloquant, 0 grave — fusion possible
