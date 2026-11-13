# Tour 6 — lot 6 « morceau-export-padlive » : relecture neuve (v2, après le self-test)
Relu : 6c4344fc89ea787ea188becd799bf3319d6ee901

Delta relu : fdb844ef..6c4344fc (3 commits : 69e09546 annulation d'U11, 7c36fe67 flèches de TrackBar, 6c4344fc rapport
et décisions), plus la capture `app/screenshots/tracks.png`. Tests non relancés (lecture seule) ; le self-test vert
(203 verts, 0 rouge) est celui annoncé par l'orchestrateur.

## Bloquant
Aucun.

## Grave
Aucun. Vérifié :
- U11 annulé proprement : le diff de 69e09546 sur `RhythmViewModel.kt` est l'exact inverse de 413e088a (`git diff
  413e088a~1 HEAD` ne touche plus `finishPerformance` ni `endPerformanceOnStop`) : plus de paramètre `stop`, plus
  d'appel `toggleTransport(aloneSongStep())`, `leaveSong` + `pushAutomation` revenus dans le même bloc. Le test
  `Lot6VmTakeEndTest` (3a3691f8) est supprimé et n'est plus cité nulle part (rapport : sa ligne `chemin:` retirée).
  Rien d'autre retiré : les autres corrections du lot (U20 dans `listenPerformance`, U3, U5, R4, U28…) sont intactes.
- TrackBar : seul `RowMoreSign` change (icône ArrowBack / ArrowForward au lieu des chevrons), même taille 22 dp, même
  teinte, sans toucher, mêmes `testTag` (`track_bar_less` / `track_bar_more`) ; `IconShapes` connaît « ← » et « → ».
  Aucun autre effet.
- Capture `tracks.png` regardée en entier : flèche → nette au bord droit de la rangée de pistes, dans son fondu, sans
  recouvrir de puce ; rien d'autre ne bouge à l'écran.
- Rapport et décisions disent vrai : « 20 sur 24, plus N25 », U11 passé dans « Laissé de côté (4 défauts du lot) » avec
  ses deux rouges nommés et la preuve (arrêt coupé : verts) ; décision U11 datée du 13 avec retour arrière (rejouer
  3a3691f8 et 413e088a) ; décision R19 mise à jour (flèches, raison du banc).

## Moyen
- (relecture précédente, tient) `PadLiveScreen.kt:274-276` U21 couché ou petit écran : les pads rétrécissent encore,
  non testé ; le rapport le dit désormais.
- U11 rouvert : « Fin » laisse la musique continuer (défaut d'origine du siège E a #38), cause de la casse
  d'« Écouter » / « Enregistrer ce que je joue » après un arrêt non trouvée ; à reprendre dans un lot suivant.

## Mineur
- `TrackBar.kt:33` : l'import `KeyboardArrowRight` n'est plus utilisé (simple avertissement Kotlin, pas d'erreur de
  compilation : aucun `allWarningsAsErrors`).
- (relecture précédente, tiennent, une ligne chacun) U3 export « Boucle » Pro choisi à la main ne dit plus « changé » ;
  U5 écouteur ne réagit qu'au changement de valeur et clé « format » dupliquée ; `scroll_hint_fade` Box repère, fondu
  non mesuré ; R23 `filesAtOpen` par composition ; U28 `at` ignoré sur morceau vide ; `Spacer(0.dp)` de PadLiveScreen.

Verdict : 0 bloquant, 0 grave — fusion possible
