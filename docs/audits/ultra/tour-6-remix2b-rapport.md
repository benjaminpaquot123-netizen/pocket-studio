# Tour 6 — lot « remix2b » (zone Remix, siège B a) : rapport

Branche reconstruite (2026-11-13, accord du chef) : `eq/tour-6-remix2b`, depuis la tête bdb777a9, en paires rouge/vert une par test ; code et tests identiques octet pour octet à `90803505` de `eq/tour-6-remix2` (contrôles Ba-2 et Ba-3 valables). Anciens commits → nouveaux : 4eeee030 → 151dcd06/aa417fef/5d2b6f9c, e33521f2 → 4d51a05a, b6d6ede5+43a70627 → a8b147d5, f678d1f0 → 6cd5fb75, 0800c4f6+41d39ef5 → a80fe9f4, 203fcac9 → c118321a/5714af68, 4cbd19c0 → 7b8a2145, 5893e286 → b5c5dc32, 1ffefd76 → 4e3f65fb/4fbe1811, 436ad6a6 → 3a57ff12/250716a6, 59ce2e6d → 96711ec3/f74c36eb.

Siège d'origine : `docs/audits/ultra/sieges/tour-6/lotB/a` (espagnol, téléphone, police 100 %, Mode simple, main gauche). Base : `ccr-80af1a1b-mn0wec` (tête poussée). Preuves rejouées par le banc dans les conditions du siège, appli entière (`RemixLeftThumbSeatTest`), jamais sur un écran isolé.

Fichiers touchés : `CutScreen.kt`, `CutWave.kt`, `CutModel.kt`, `RemixScreen.kt` ; tests `RemixLeftThumbSeatTest.kt` (neuf), `CutBoutBigTextTest.kt`, `CutZoomGestureTest.kt`, `RemixCutFromCardTest.kt`, `CutJourneyTest.kt` ; captures `cut_*.png`, `remix/cut_bout_short_*.png`, `remix_*.png`.

Erratum d'histoire : le rouge 3a57ff12 nomme `CutZoomGestureTest#removeAfterTouchesAndACutTakesOnlyThePieceUnderTheHead`, absent de son arbre ; le test qui y rougit s'appelle `CutZoomGestureTest#removeAfterTouchesAndAnEditTakesOnlyThePieceUnderTheHead` (« after cut expected:<63> but was:<61> »), et le vert 250716a6 le découpe en `…AndACut…`, `…AndALoop…`, `…ACutAndUndo…`. Les deux commits portent le même sujet (le juge les apparie par lui) ; laissé tel quel pour ne pas réécrire une branche poussée.

tests: com.pocketstudio.app.remix.RemixLeftThumbSeatTest com.pocketstudio.app.remix.CutBoutBigTextTest com.pocketstudio.app.remix.CutZoomGestureTest com.pocketstudio.app.remix.RemixCutFromCardTest com.pocketstudio.app.remix.CutJourneyTest

controle: Ba-2 | code testé: a4b719df | bloqués: 0 | graves neufs: 0
controle: Ba-3 | code testé: 10830ce2 | bloqués: 0 | graves neufs: 0

## Constaté par un siège de contrôle (Ba-2, Ba-3 ; rapports dans `sieges/tour-6/controle/Ba-2` et `Ba-3`)
- **A2** : constaté (Ba-3, écrans 15-18). **A3** : constaté (Ba-3, écran 8). **A5** : constaté (Ba-3, écran 14). **A7** : constaté (Ba-3, écran 21). **A8** : constaté (Ba-3, écrans 8 et 41-51).
- Ba-3, écran 40 : un bout décoché par son rond numéroté (« 2 → 1 trozo ») ; « Enlever » n'a pas été touché par le siège (le garde-fou « Enlever un seul morceau » n'est prouvé que par les tests `CutZoomGestureTest#removeAfter…`). Buts 3/3 : lecture du chef (séparation, découpe et pose faites, jamais d'abandon), pas une ligne du siège.
- Moyen relevé par Ba-3 (geste 27, `revu: non`) : retirer un des deux bouts choisis demande 4 gestes sans aide ; retoucher le bout ou appui long ne le décoche pas (A7 : un choix par la tête n'est jamais décoché), seul le rond numéroté le fait. Pas pris dans ce lot.
- **Pas pris** : A4 (mode Phrases, ligne d'outils), A6 (avertir et allonger : `RhythmViewModel`/`AppRoot`), A8 « Descargar el modelo » en double (tests hors `remix/`), « Remix » du menu « Más » (navigation).

## Preuve rejouée (par le banc, conditions du siège)
- **A2** (puces hors du pouce gauche) : « Voz / Batería / Bajo / Resto » descendues sous les outils, atteignables au pouce gauche sans glisser. `RemixLeftThumbSeatTest#a2_stemChipsReachableWithLeftThumb`.
- **A3** (bouton ✂ et sa phrase d'aide coupés sous la barre flottante) : après séparation, « Cortar lo que quiero » ET sa phrase d'aide entiers, rien sous « Elegir una canción ». `#a3_cutHintWholeNotUnderThumb`.
- **A5** (Inicio/Fin/Escuchar écrasés à l'arrivée) : entiers et touchables sans glisser, à l'arrivée sur la découpe. `#a5_spanToolsWholeOnArrival`.
- **A8 (Cortar)** (deux « Cortar la canción » sur le panneau) : un seul, atteignable au pouce. `#a8_oneCutButtonInProject`.

chemin: Remix › Cortar la canción › Un trozo (es, téléphone, 100 %, Mode simple, main gauche) | test: RemixLeftThumbSeatTest#a2_stemChipsReachableWithLeftThumb
chemin: Remix › Cortar la canción › Un trozo (arrivée) | test: RemixLeftThumbSeatTest#a5_spanToolsWholeOnArrival
chemin: Remix › après la séparation (carte de la chanson) | test: RemixLeftThumbSeatTest#a3_cutHintWholeNotUnderThumb
chemin: Remix › panneau du projet | test: RemixLeftThumbSeatTest#a8_oneCutButtonInProject

## Contrôle Ba-2 (aller-retour 1) : A7 et A8 repris
- **A7** : toucher un morceau de voix sur la courbe (Frases/Compases) le choisit, au pouce gauche ; « En los pads » s'allume. `RemixLeftThumbSeatTest#a7_touchingAVoicePieceChoosesIt` (rouge 7b8a2145, vert b5c5dc32).
- **A8** : plus de second ✂ « Cortar lo que quiero » sur la page Remix quand la chanson est dans le projet. `RemixLeftThumbSeatTest#a8_oneCutButtonInProject` (même paire).

chemin: Remix › Cortar la canción › Frases (toucher un morceau de voix, main gauche) | test: RemixLeftThumbSeatTest#a7_touchingAVoicePieceChoosesIt
chemin: Remix › Cortar la canción › Frases (courbe) | test: CutZoomGestureTest#placingThePlayHeadChoosesThePieceAndNeverUnticksIt
chemin: Remix › panneau du projet (carte de la chanson) | test: RemixCutFromCardTest#aSongAlreadyInTheProjectOpensItsCutScreenAtOnce

## Tenté (code + capture, non vérifiable par le banc dans les termes du siège)
- **A7** (morceau choisi marqué nettement) : bande vive de la couleur du morceau, encadrée, en plus du numéro (`CutWave.kt`). Depuis le contrôle Ba-2, toucher le corps d'un morceau le choisit aussi (section Ba-2). C'est un dessin : pas de nœud à lire pour le banc. (Au premier aller, le geste pour cocher n'avait pas été changé ; il l'a été au retour du contrôle Ba-2.)

## Pas pris (hors des fichiers `remix/` ou trop gros)
- **A6** (avertir « la fin sera coupée » + proposer d'allonger) : l'action d'allongement n'existe pas dans `RemixActions` ; elle touche `RhythmViewModel`/`AppRoot`. Demande au chef.
- **A8 (Descargar)** (double « Descargar el modelo ») : retirer le doublon casse des tests hors `remix/` (`nav/SimpleModeAuditTest`, `audit/BenchContrastRemixLibraryTest`, sentinelle du banc `human/`). Demande au chef.
- **« Remix » du menu « Más » au pouce gauche** : navigation (menu « Más »), hors zone Remix.

## Demandes
- Demande : `RhythmViewModel.kt` / `AppRoot.kt` (A6, allonger le projet pour la chanson).
- Demande : `nav/SimpleModeAuditTest.kt`, `audit/BenchContrastRemixLibraryTest.kt`, `human/HumanTester.kt` + bancs (A8 Descargar : retirer le doublon de carte et mettre à jour les tests/sentinelles).

## Laissés de côté
- A4 (en « Phrases » la ligne d'outils reste visible) : non vérifié ce tour ; les puces restent en haut en mode Phrases (la liste des morceaux a besoin des puces au-dessus). À rejouer dans un siège Phrases.

## Relecture neuve (sous-agent, sur a80fe9f4)
- Relancé : `:app:testDebugUnitTest --tests 'com.pocketstudio.app.remix.*'` → 300 tests, 0 échec.
- 0 bloquant, 0 grave. Moyen (corrigé avant fusion) : à 488 dp sans défilement la courbe tombait sous 120 dp ; test de garde `CutBoutBigTextTest#aPieceJustAboveTheScrollThreshold` vu rouge (courbe 0 dp), vert après le correctif (courbe fixe 120–180 dp, défilement toujours).
- Mineurs laissés : à 130 % le chip « Resto » demande un glissement latéral (rangée défilante) ; A7 non vérifié sur une toile claire (non vérifié s'il en existe une).
- Après correctif : remix.* 0 échec (siège a2/a3/a5/a8 verts), captures cut_bout* réenregistrées et regardées.
