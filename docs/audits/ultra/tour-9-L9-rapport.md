# Tour 9, lot L9 « banc » (zone banc) : rapport

Branche : eq/tour-9-L9, partie de l'intégration ccr-80af1a1b-mn0wec (base 7455749a5).
Rôle : correcteur du banc. Aucun fichier de l'appli touché (ni app/src/main ni project-model/src/main).

Ce qui change :
- D17 (kit du tuto qui disparaît, siège t9-75) : c'est le banc. Les FLAC du pack arrivent pendant une attente et le banc ne les montrait au décodeur de Robolectric qu'avant et après chaque geste. Le banc les montre maintenant aussi pendant l'attente (`BenchFlac`). Sur un téléphone, le kit reste.
- Tests creux : `Tour8PlusRowsFitTest` dit pour chaque cas la disposition attendue et ajoute les écrans où les tuiles pleines sont dues. `Tour8GuideNoEmptyStepTest` mesure le trou (seuil 8 dp).
- `OrdinaryTestsLeaveNothingTest` : un travail lancé en retard rougit son propre test (`HeldWorker` dans le paquet `benchhold`).
- Pré-vol sur un clone coupé : les fusions sans ancêtre commun sont sautées comme un manque d'environnement, pas comme un vert (`test_portail4.py`).
- `ci-etat.sh` sans `gh` : repli par curl, testé (`test_ultra_bench.py`).
- Coquille2 p5 capricieux : la liste des projets se charge en temps réel. Le banc attend tant qu'elle charge (jusqu'à 60 s réelles), au lieu de seulement 40 « attends 0.5 ». Cinq passages verts d'affilée.
- Liste des sauts connus du selftest : ajout de `BenchTutoKitDownloadTest#drive`.

tests: *BenchTutoKitDownloadTest* *T9D17KitDisappearsTest* *Tour8PlusRowsFitTest* *Tour8GuideNoEmptyStepTest* *OrdinaryTestsLeaveNothingTest* *Coquille2SeatTest*

chemin: tuto UK Garage, Pro, kit téléchargé pendant une attente | test: BenchTutoKitDownloadTest#aKitDownloadedDuringAWaitStaysOnThePads
chemin: tuto, kit du pack décodé comme sur le téléphone | test: T9D17KitDisappearsTest#a_withThePhonesDecoderTheKitStays
chemin: page Plus, Mode Simple, tablet8 à 100 % | test: Tour8PlusRowsFitTest#tabletFrench100
chemin: page Plus, Mode Simple, phone411 à 100 % | test: Tour8PlusRowsFitTest#phone411French100
chemin: carte du Guide réglée, sans étape | test: Tour8GuideNoEmptyStepTest#settledCardHasNoEmptyStepBlock
chemin: banc, test ordinaire qui laisse du travail lancé en retard | test: OrdinaryTestsLeaveNothingTest#c_anOrdinaryTestThatLeavesTheAppsWorkStartedLateIsRedItself
chemin: Coquille, liste des projets lente à charger | test: Coquille2SeatTest#p5_aProjectsListHeldInRealTimeIsWaitedFor
chemin: Coquille, retour au projet du Live | test: Coquille2SeatTest#p5_theLiveProjectOpensAgainOnPadLive
