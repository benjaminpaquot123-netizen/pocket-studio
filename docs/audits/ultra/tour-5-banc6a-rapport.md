# Lot banc « banc6a » (étape 3, lot 6, première partie : 4.4, 4.6, 4.7) : rapport de l'auteur

Atelier `/home/user/eq-tour-5-banc6a`, branche `eq/tour-5-banc6a`, partie de fx-int `dba28dab`. Zone : `app/src/test/.../human`, `scripts/bench`, `docs`. Une seule ligne dans `app/src/main` : l'essai tel-20 ajouté à `recette.json` (demandé par 4.6).

## 4.4 « attends » quand l'appli est bloquée
- Mesure (rouge ff69cb40, `BenchSeparationWaitTest#aStuckStandInGivesTheHandBackWithinTwentySeconds`) : « attends 2 » rendait la main après **120,2 s** réelles, avec le seul drapeau de séparation (rien ne tourne) comme avec le remplaçant bloqué (`timeScale` 100 000).
- Correction (vert cdab9801) : `slowWorkDone` n'attend que si quelque chose bouge : battement de `BenchSeparation` (chaque Mo envoyé, chaque pas) ou fil occupé hors socket ; rien depuis 15 s : la main revient (ligne « Banc : … n'avance plus » si un fil dort dans le remplaçant ; rien de dit pour le drapeau seul, c'est ce que montre l'appli). Plafond de 120 s gardé.
- Vert : **15,4 s** dans les deux cas ; `twoWaitsOfThirtySecondsGiveTheSeparatedSong` et `HumanRemixJourneyTest` restent verts.
- Trouvé en route : le téléchargement coupé par la fermeture du remplaçant posait « Échec : connexion coupée » après la remise à zéro (règle « pristine » rouge) ; `end()` le laisse finir avant. Commit préalable 37d840e1 : le remplaçant dort sur un verrou que `close()` relâche.

## 4.6 Écran vide au démarrage
- Mesure : F a (120 s) et F c (300 s) attendaient **avant** l'écran 1 (démarrage de Gradle et de la JVM) ; leur écran 1 n'était pas vide. Le vide réel est « Ouverture du projet… » (lecture du projet après les sauvegardes en cours). Chaque siège écrit maintenant `#0 PREMIER ECRAN reel_s simule_s jvm_s travail vide attente_s`, lu dans `mesures.json` (`premier_ecran_s`) : rouge d55884fa / vert e7756ba3 (`test_ultra_bench.py::first_screen`).
- Rouge 7bb62944 (`BenchSelfTest#aBlankFirstScreenIsWaitedForOnlyWhenTheBenchIsBusy`) : un ralenti injecté par `ClosingSaves.launch` (un fil qui calcule 12 s) laissait « Ouverture du projet… » au testeur.
- Correction (vert 516c8726) : le banc n'attend (30 s au plus) que si ses fils travaillent ; au repos (sauvegarde qui dort : `aBlankFirstScreenWithTheBenchAtRestIsShownAsItIs`) l'écran est montré tel quel, verdict « à trancher » : `T5-PREMIER-ECRAN` dans `verdicts.tsv`, essai tel-20 « Premier écran après Pro ».

## 4.7 Taille et durée d'un export
- Profil sous Robolectric (`BenchEngineTest#exportProfile`, 931644c6) : M4A du banc = PCM 16 bits recopié par le faux codec (768 000 octets pour 4 s, 3 072 000 pour 16 s, environ 6 fois un vrai AAC), WAV 1 536 192 / 6 144 192 octets ; 1,3 à 1,9 s réelles quelle que soit la longueur. Pas d'attente évitable côté encodeur : rien réduit. « 1 KB » et « 4 min » ne se reproduisent pas ici : cause non mesurée (export presque vide, liste de « Mes exports », pas des gestes) ; non représentatifs, déjà écrits à l'écran (banc45).
- Rouge 510a1e19 / vert 50ba6a28 (`test_ultra_bench.py::export_figures_are_the_benchs`) : `fuse-reports.py` classe « banc » (marque « [banc : taille ou durée d'un export, non représentatives] ») une grappe de fiches sur la taille ou le temps pris d'un export ; la longueur du fichier (en mesures) reste de l'appli. Fait dans `fuse-reports.py` et non `seat-metrics.py` (DECISIONS).

## Ce qui reste
- 4.7 : la garde de T5-Cc-41b teste encore une chaîne (`shareNote`) ; un test `Bench*` qui ouvre « Mes exports » et lit la ligne de son (mineur de la relecture banc45) n'est pas écrit. La cause des « 4 min » d'un siège n'est pas trouvée.
- 4.6 : « vide » = page « Ouverture du projet… » ou rien de touchable ; un écran à moitié chargé (D b #8, `tuto_spot_ring`, en cours de siège) n'est pas couvert (pas un premier écran).
- `ScreenshotTest#recette` est rouge, mais déjà avant ce lot : la référence montre encore « 18 essais » et l'ancien changelog ; le 11e essai n'est pas dans la partie capturée. Pas réenregistrée ici (capture de l'appli).

## À rejouer à la fusion
- `python3 -I scripts/bench/test_ultra_bench.py` (`first_screen`, `export_figures_are_the_benchs`).
- Classes : `BenchSeparationWaitTest`, `BenchSelfTest`, `BenchEngineTest`, `HumanRemixJourneyTest`, `BenchPristine*` (toutes jouées par le selftest).
- `selftest.sh` puis `verifie-chantier.py --resume` : sur e18e6f32, 175 verts, 0 rouge, 41 sautés ; juge 27 écarts, les mêmes que la trace du portail de « textes » (`portail/2026-11-11-tour-5-textes`) : les 5 de plus que « 22 » sont des paires de « textes » (oneWordPerThing, SimpleNoDecibelTest), aucun de ce lot.
