# Tour 5, lot « mixwake » : Mix saccadé en lecture, décrochage au retour de l'écran (2026-11-12)

Atelier `eq-tour-5-mixwake`, branche `eq/tour-5-mixwake` (depuis fx-int 193d17ee). Rien de poussé. À relire par un relecteur neuf.
Point de départ : BenStudio Test v5 « Tout tester » sur le HONOR BKQ-N49 (Android 17) : Mix 42 % d'images lentes
(241 images, v4 : 48 %), les autres écrans 0 à 4 % ; « Écran éteint » rouge : 4 décrochages, « pire bloc 525 %,
plus long écart 5,3 blocs », 3 s après « Premier plan : son ouvert ». Décisions : `docs/decisions/tour-5-mixwake.md`.

tests: *PlayingRecomposeTest *SimpleMixTest *Lot2TextsTest com.pocketstudio.app.mixer.*

## A. Mix pendant la lecture

Mesure : `PlayingRecomposeTest` (l'appli entière, 60 lectures du moteur, comme le lot « jank »), plus la présence d'un
calque propre sur chaque élément qui bouge à chaque lecture ou à chaque image. Robolectric ne dessine pas : le temps d'une
image n'est pas mesuré ici.

| Écran | Avant (rouge a240a4b1) | Après | Cause |
|---|---|---|---|
| Mix (5 espaces) | 374 portées, aucun calque | 83, calques | loudness et ligne du limiteur : un nouveau texte à chaque lecture (20/s), mesuré et remis en page ; vumètres et faders sans calque |
| Mix (Mode simple) | 2995 portées, aucun calque | 118, calques | « Ça sature » lu dans la composition de l'onglet : chaque passage recomposait toutes les bandes (un `SimpleMixUi` neuf) |

Pourquoi les calques : un vumètre lit son niveau au dessin, mais sa crête qui retombe le redessine à **chaque image**
(60 à 120 par seconde). Sans calque à lui, Compose réenregistre le dessin du calque qui le contient : pour le vumètre de
sortie, toute la colonne défilante du Mix (console, courbes, sortie, bus, export) ; pour un fader, toute la console.

Correctifs (vert b49d8f74) :
- `graphicsLayer()` sur `LevelMeter` (ui-components), `VerticalFader` (console), `MixFader` (Mode simple), le texte de
  loudness et la ligne du limiteur.
- `rememberChangesEvery` (ui-components, `Changes.kt`) : un nombre lu du moteur change 4 fois par seconde au plus
  (`NUMBER_EVERY_MS` = 250 ms), toujours la dernière valeur ; la première est lue tout de suite.
- Mode simple : « Ça sature » suivi dans un `snapshotFlow` pour le conseil ; `SimpleMixUi` mémorisé ; les mots de
  « Sortie » dans leur propre portée (`OutputWordsText`).

Captures : `verifyRoborazziDebug` (ScreenshotTest, WriteScreenshotTest, TabletScreenshotTest, UiScaleScreenshotTest,
I18nScreenshotTest, TabletI18nScreenshotTest, MotionScreenshotTest, TutoScreenshotTest, nav.*, TutoAppScreenshotTest,
ui.*, ShareScreenshotTest, rhythm.*, mixer.*, projects.*) : 1723 tests, 0 échec, 513 captures comparées « unchanged »,
aucune réenregistrée. Regardées : Mix (sortie générale) et Mix du Mode simple.

## B. Décrochage au retour de l'écran

Ce que dit journal.json (heures du téléphone) :
- 12:16:42 « Fin du morceau » (écran encore éteint) ; ensuite le moteur tourne à vide (`playing` faux), charge lissée 0,72.
- 12:16:49,9 la charge tombe à 0,24 : l'écran se rallume (avant même « Premier plan », 12:16:50,69). Le flux n'est pas
  rouvert (`restarts` 0, aucun écart d'une seconde) : `start()` le trouve déjà démarré.
- À « Premier plan », `engineStarted()` renvoie tout le projet au moteur (réparti par paquets de 1024 commandes).
- De 12:16:50,9 à 12:16:53,4 la charge remonte de 0,24 à 0,50, travail identique (à l'arrêt, aucune action au journal).
- Entre 12:16:53,4 et 12:16:53,7 : **un** rappel prend 5,25 fois son temps (pire bloc 525 %, écart 5,3 blocs : c'est le
  même rappel), 4 décrochages, puis tout redevient normal (pire bloc 0,5 à 0,6).

Lecture : la charge qui double sans travail en plus est la fréquence du processeur qui redescend après le coup de fouet
du réveil ; le blocage tombe au bout de cette descente, 3 s après le renvoi du projet, à l'arrêt. Rien de notre code
connu ne tourne dans le rappel à ce moment-là (pas de commande lourde en cours, pas de rouverture, pas de changement de
taille de tampon). Le rappel reste sans allocation, verrou, journal ni JNI (`AudioHost::onAudioReady` → `Engine::process`,
relus). **Cause non trouvée dans le code** ; probable côté téléphone (fréquence, migration de cœur, ou mémoire rendue
pendant l'écran éteint). Aucune parade justifiée par ces mesures : pas de changement de code.
Noté pour plus tard (décisions) : le renvoi complet du projet au retour au premier plan alors que le flux n'a jamais été
fermé (il coupe les notes tenues d'un instrument quand la musique joue en arrière-plan) ; il n'est pas la cause démontrée.

## Vérifié
- `:app:testDebugUnitTest --tests '*PlayingRecomposeTest*' --tests '*SimpleMixTest*' --tests '*Lot2TextsTest*' --tests 'com.pocketstudio.app.mixer.*'` : vert.
- Rouge constaté sur a240a4b1 : Mix 374 > 120 ; Mode simple 2995 > 160 ; calques absents.
- Moteur non touché (host-tests non concernés).

## Limites
- Le gain réel en images lentes ne se voit que sur le téléphone (Robolectric ne dessine pas).
- Le Mix compte encore la ligne du limiteur et la loudness (un texte, 4 fois par seconde) et le conseil « Ça sature »
  du Mode simple (toutes les bandes quand le conseil change).

## Essais téléphone à rejouer
« Tout tester » : « Mémoire et images lentes » (le Mix, en 5 espaces et en Mode simple) et « Écran éteint » ; pour B,
une trace système (Perfetto) au rallumage dira fréquence, cœur ou faute de page.

## Chemins
- chemin: Mix en lecture (vumètres, faders de la console, loudness, limiteur) | test: PlayingRecomposeTest#mixComposesLittleWhilePlaying
- chemin: Mode simple › Mix en lecture (faders, Sortie et ses mots, conseil) | test: PlayingRecomposeTest#simpleMixComposesLittleWhilePlaying
- chemin: Mix, ligne du limiteur à l'ouverture | test: Lot2TextsTest#theLimiterNeverSaysAnAbsurdNumber
- chemin: Mix › ligne du limiteur après un seul clip court | test: ClipHoldTest#aSingleShortClipShowsInTheLimiterLine
- chemin: Mode simple › Mix, mots « Ça sature » et conseil après un seul clip court | test: ClipHoldTest#aSingleShortClipShowsInTheWordsAndTheTipAgrees
