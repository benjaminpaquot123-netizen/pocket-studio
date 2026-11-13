# Tour 6 — lot « remix2 » (zone Remix, siège B a) : rapport

Siège d'origine : `docs/audits/ultra/sieges/tour-6/lotB/a` (espagnol, téléphone, police 100 %, Mode simple, main gauche). Base : `ccr-80af1a1b-mn0wec` (tête poussée). Preuves rejouées par le banc dans les conditions du siège, appli entière (`RemixLeftThumbSeatTest`), jamais sur un écran isolé.

Fichiers touchés : `CutScreen.kt`, `CutWave.kt`, `RemixScreen.kt`, et le test `RemixLeftThumbSeatTest.kt`.

tests: com.pocketstudio.app.remix.RemixLeftThumbSeatTest com.pocketstudio.app.remix.CutBoutBigTextTest com.pocketstudio.app.remix.CutZoomGestureTest com.pocketstudio.app.remix.RemixCutFromCardTest

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
- **A7** : toucher un morceau de voix sur la courbe (Frases/Compases) le choisit, au pouce gauche ; « En los pads » s'allume. `RemixLeftThumbSeatTest#a7_touchingAVoicePieceChoosesIt` (rouge 4cbd19c0, vert 5893e286).
- **A8** : plus de second ✂ « Cortar lo que quiero » sur la page Remix quand la chanson est dans le projet. `RemixLeftThumbSeatTest#a8_oneCutButtonInProject` (même paire).

chemin: Remix › Cortar la canción › Frases (toucher un morceau de voix, main gauche) | test: RemixLeftThumbSeatTest#a7_touchingAVoicePieceChoosesIt
chemin: Remix › Cortar la canción › Frases (courbe) | test: CutZoomGestureTest#placingThePlayHeadChoosesThePieceAndNeverUnticksIt
chemin: Remix › panneau du projet (carte de la chanson) | test: RemixCutFromCardTest#aSongAlreadyInTheProjectOpensItsCutScreenAtOnce

## Tenté (code + capture, non vérifiable par le banc dans les termes du siège)
- **A7** (morceau choisi marqué nettement) : bande vive de la couleur du morceau, encadrée, en plus du numéro (`CutWave.kt`). C'est un dessin : pas de nœud à lire pour le banc. Le geste pour cocher n'est pas changé (régression du tour 3).

## Pas pris (hors des fichiers `remix/` ou trop gros)
- **A6** (avertir « la fin sera coupée » + proposer d'allonger) : l'action d'allongement n'existe pas dans `RemixActions` ; elle touche `RhythmViewModel`/`AppRoot`. Demande au chef.
- **A8 (Descargar)** (double « Descargar el modelo ») : retirer le doublon casse des tests hors `remix/` (`nav/SimpleModeAuditTest`, `audit/BenchContrastRemixLibraryTest`, sentinelle du banc `human/`). Demande au chef.
- **« Remix » du menu « Más » au pouce gauche** : navigation (menu « Más »), hors zone Remix.

## Demandes
- Demande : `RhythmViewModel.kt` / `AppRoot.kt` (A6, allonger le projet pour la chanson).
- Demande : `nav/SimpleModeAuditTest.kt`, `audit/BenchContrastRemixLibraryTest.kt`, `human/HumanTester.kt` + bancs (A8 Descargar : retirer le doublon de carte et mettre à jour les tests/sentinelles).

## Laissés de côté
- A4 (en « Phrases » la ligne d'outils reste visible) : non vérifié ce tour ; les puces restent en haut en mode Phrases (la liste des morceaux a besoin des puces au-dessus). À rejouer dans un siège Phrases.

## Relecture neuve (sous-agent, sur 41d39ef5)
- Relancé : `:app:testDebugUnitTest --tests 'com.pocketstudio.app.remix.*'` → 300 tests, 0 échec.
- 0 bloquant, 0 grave. Moyen (corrigé avant fusion) : à 488 dp sans défilement la courbe tombait sous 120 dp ; test de garde `CutBoutBigTextTest#aPieceJustAboveTheScrollThreshold` vu rouge (courbe 0 dp), vert après le correctif (courbe fixe 120–180 dp, défilement toujours).
- Mineurs laissés : à 130 % le chip « Resto » demande un glissement latéral (rangée défilante) ; A7 non vérifié sur une toile claire (il n'y en a pas aujourd'hui).
- Après correctif : remix.* 0 échec (siège a2/a3/a5/a8 verts), captures cut_bout* réenregistrées et regardées.
