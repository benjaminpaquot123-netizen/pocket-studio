# Tour 6 — lot « remix2 » (zone Remix, siège B a) : rapport

Siège d'origine : `docs/audits/ultra/sieges/tour-6/lotB/a` (espagnol, téléphone, police 100 %, Mode simple, main gauche). Base : `ccr-80af1a1b-mn0wec` (tête poussée). Preuves rejouées par le banc dans les conditions du siège, appli entière (`RemixLeftThumbSeatTest`), jamais sur un écran isolé.

Fichiers touchés : `CutScreen.kt`, `CutWave.kt`, `RemixScreen.kt`, et le test `RemixLeftThumbSeatTest.kt`.

tests: com.pocketstudio.app.remix.RemixLeftThumbSeatTest

## Preuve rejouée (par le banc, conditions du siège)
- **A2** (puces hors du pouce gauche) : « Voz / Batería / Bajo / Resto » descendues sous les outils, atteignables au pouce gauche sans glisser. `RemixLeftThumbSeatTest#a2_stemChipsReachableWithLeftThumb`.
- **A3** (bouton ✂ et sa phrase d'aide coupés sous la barre flottante) : après séparation, « Cortar lo que quiero » ET sa phrase d'aide entiers, rien sous « Elegir una canción ». `#a3_cutHintWholeNotUnderThumb`.
- **A5** (Inicio/Fin/Escuchar écrasés à l'arrivée) : entiers et touchables sans glisser, à l'arrivée sur la découpe. `#a5_spanToolsWholeOnArrival`.
- **A8 (Cortar)** (deux « Cortar la canción » sur le panneau) : un seul, atteignable au pouce. `#a8_oneCutButtonInProject`.

chemin: Remix › Cortar la canción › Un trozo (es, téléphone, 100 %, Mode simple, main gauche) | test: RemixLeftThumbSeatTest#a2_stemChipsReachableWithLeftThumb
chemin: Remix › Cortar la canción › Un trozo (arrivée) | test: RemixLeftThumbSeatTest#a5_spanToolsWholeOnArrival
chemin: Remix › après la séparation (carte de la chanson) | test: RemixLeftThumbSeatTest#a3_cutHintWholeNotUnderThumb
chemin: Remix › panneau du projet | test: RemixLeftThumbSeatTest#a8_oneCutButtonInProject

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
