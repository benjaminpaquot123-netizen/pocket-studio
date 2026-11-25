# Tour 9, lot kick-ci : rapport

Rouge de la CI 45508694147 (tranche 5/6, deux fois) : `TutoKickSoundTest#karimsGesturesOnTheOldStep`, « the kick's sheet: not in time ».

Cause (prouvée, pas une lenteur) : le test laissait le catalogue des packs au réseau (`TutoTestHooks.packs` nul, donc `libraryVm.refreshPacks()` dans AppRoot). Sur les machines de GitHub le vrai catalogue se charge : la carte « kit du style » s'affiche dans le panneau ouvert, le panneau grandit (haut à 208 au lieu de 274 sur 360 × 780) et garde cette hauteur tant que son étape reste (règle du lot « tuto 3 »). La carte du pad n'a plus de place : « Réglages du pad » mesure 0 × 0, le toucher n'ouvre aucune feuille, l'attente de 120 s expire. Hors réseau (machine locale), pas de carte : vert.

Écarté : l'ordre de la tranche ou un état laissé par une classe d'avant. Les 249 classes de la tranche 5, rejouées en local dans l'ordre de la CI : ce test vert (0,29 s).

Correction (le test seul, rien dans l'appli) : le test pose lui-même le catalogue, des deux façons. Sans les packs du style, c'est l'ancien test. Avec, c'est le cas de la CI, `karimsGesturesWithTheKitCardOffered` : Karim répond « Plus tard » à la carte puis replie le panneau pour atteindre les pads.

- rouge : a8ff9c23f. Le test du cas CI est rouge en local avec le message exact de la CI (« the kick's sheet: not in time », 120,4 s), les trois autres sont verts.
- vert : a59db829e. TutoKickSoundTest : 4 sur 4 verts ; `--tests com.pocketstudio.app.tuto.*` sur a59db829e : 475 tests, 0 échec (72 sautés, déjà sautés sur la base).

tests: com.pocketstudio.app.tuto.*

chemin: Tuto Pro, étape « son du kick », Sons → Réglages du pad → feuille du kick | test: TutoKickSoundTest#karimsGesturesOnTheOldStep
chemin: même chemin, carte du kit offerte → « Plus tard » → Replier → Réglages du pad | test: TutoKickSoundTest#karimsGesturesWithTheKitCardOffered
chemin: étape « son du kick » → Montre-moi → carte du kit « Plus tard » → J'ai compris | test: TutoKickSoundTest#theKickStepNowSaysShowsAndTicks

À voir (hors lot, design) : en Pro sur 360 × 780, la carte du pad n'a pas de place sous le panneau ouvert, même sans la carte du kit (on n'y arrive qu'en faisant défiler). Après « Plus tard », le panneau garde une bande vide de 66 dp là où était la carte.
