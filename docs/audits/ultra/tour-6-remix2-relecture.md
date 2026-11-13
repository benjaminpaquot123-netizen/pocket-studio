# Tour 6 — lot « remix2 » (zone Remix, siège B a, pouce gauche) : relecture neuve, aller-retour 1 du contrôle Ba-2, puis trois passes sur le grave (a1bbd8f1, c4aac715, 3ec13da4)
Relu : e5ba40c55f98d697662fab995d91fc7301c02de1

Relecteur neuf (n'a pas écrit ce code). Lu : `docs/AIDE-FUSIONS-TESTS.md` (fiche du relecteur), le lot entier `ce0aed6f..HEAD` (`CutScreen.kt`, `CutWave.kt`, `CutModel.kt`, `RemixScreen.kt`, `RemixLeftThumbSeatTest.kt`, `CutBoutBigTextTest.kt`, `CutZoomGestureTest.kt`, `RemixCutFromCardTest.kt`), dont le delta `a4b719df..864e3d90`.
Captures regardées en entier : `cut_bout.png`, `cut_bout_130.png`, `cut_screen.png`, `remix/cut_bout_short_{fr_100,fr_130,ptbr_200}.png`, `remix_board_button.png`, `remix_song_mix.png` (comparée à sa version de a4b719df).
Rejoué :
- à HEAD 3ec13da4 : `:app:testDebugUnitTest --tests 'com.pocketstudio.app.remix.*'`, 308 tests, 0 échec, dont `RemixLeftThumbSeatTest#a7` (302 à 864e3d90, 303 à a1bbd8f1, 307 à c4aac715) ;
- au passage précédent (7046598a) : `verifyRoborazziDebug` des captures de découpe, de tailles et de tablette, vert.

Delta 10830ce2..12e5e818, relu : de la documentation seule. Ce sont la copie texte des sièges de contrôle Ba-2 et Ba-3, et les lignes `controle:` et « Constaté / Pas pris » du rapport de zone ; aucun fichier hors de `docs/`, aucun lien symbolique. Les verdicts A2, A3, A5, A7 et A8 du rapport correspondent à ceux de `controle/Ba-3/rapport.md` (« corrigé », mêmes écrans), mais une phrase en disait plus que le siège (moyen n°8). Delta 12e5e818..e5ba40c5 relu : de la documentation seule (le rapport de zone et `docs/decisions/tour-6-remix2.md`), qui règle les moyens 8 et 9.

Les rouges et les sondes ont été joués dans des worktrees jetables, retirés ensuite. Les sondes sont des tests ajoutés seulement dans ces worktrees, jamais commités.

## Ce qui tient

- **A8, aucun accès perdu.** Le ✂ de la carte disparaît seulement si `ui.remix.setId == set.id && ui.board?.setId == set.id`. La rangée du pouce affiche « Découper la chanson » sous `ui.board != null && !ui.home && ui.remix.setId != null`. Quand la carte perd son ✂, le bouton du pouce est donc toujours là.
  - Chanson dans le projet, board pas encore chargé (`ui.board == null` ou board d'une autre chanson) : la carte garde son ✂.
  - Les autres chansons de la page gardent le leur.
  - L'ancien `remix_open_cut` du panneau (retiré en e33521f2) s'affichait sous la même condition que la rangée du pouce.
  - `remix_board_button.png` : un seul « Découper la chanson », en bas, à portée du pouce.
- **A7, les tests mis à jour testent encore quelque chose.**
  - `CutZoomGestureTest#aTouchNearALinePlacesThePlayHeadAndLeavesTheCutsAlone` vérifie toujours que les coupes ne bougent pas (`before == m.pieces`), qu'il n'y a pas d'étape d'annulation, et qu'aucune sauvegarde ne change les morceaux.
  - `#placingThePlayHeadChoosesThePieceAndNeverUnticksIt` (3, 7, 7 → `[3, 7]`) prouve l'ajout sans décochage.
  - Une poignée saisie ne choisit rien (`if (!grabbing)`).
  - « Couper ici » après un placement de tête marche encore : `CutJourneyTest` est vert à HEAD.
- **A5 et le seuil de 485 dp** (aller précédent). En « Un bout », la colonne défile toujours derrière une courbe fixe de 120 à 180 dp ; le plancher de `weight` est revenu à 120 dp ; le test de garde à 488 dp / 100 % est vert.
- **A2, A3.** Les puces sont sous les outils à police normale, visibles sans défiler. Elles défilent horizontalement (l.341). L'intro n'est masquée qu'une fois une chanson séparée.
- **Son non touché.** Que de l'interface Compose et le modèle de découpe : pas de C++, pas de callback audio. La sauvegarde n'est touchée qu'à travers les choix et les morceaux de `CutModel` : voir le grave n°1.

rouge constaté : 59ce2e6d : rejoué sur le code de c4aac715 : `CutZoomGestureTest#removeAfterReopeningTakesOnlyThePieceUnderTheHead` « expected:<63> but was:<62> »
rouge constaté : 436ad6a6 : rejoué sur le code de a1bbd8f1 : `CutZoomGestureTest#removeAfterTouchesAndACutTakesOnlyThePieceUnderTheHead` « after cut expected:<63> but was:<61> », `#removeAfterTouchesAndALoopTakesOnlyThePieceUnderTheHead` « after loop expected:<63> but was:<61> », `#removeAfterTouchesACutAndUndoTakesOnlyThePieceUnderTheHead` « after cut+undo expected:<63> but was:<61> »
rouge constaté : 1ffefd76 : rejoué sur le code de 864e3d90 : `CutZoomGestureTest#removeAfterTouchesTakesOnlyThePieceUnderTheHead` « expected:<63> but was:<61> »
rouge constaté : 5893e286 : rejoué sur le code de a4b719df : `RemixLeftThumbSeatTest#a7_touchingAVoicePieceChoosesIt` « touching a voice piece chose nothing (« En los pads » stays grey) » et `#a8_oneCutButtonInProject` « a second ✂ on the page expected:<0> but was:<1> » ; aussi `CutZoomGestureTest` « expected:<[3, 7]> but was:<[]> » et « expected:<[31]> but was:<[]> », `RemixCutFromCardTest` « Did not expect any node but found '1' node … (TestTag = 'cut_song_cut') »
rouge constaté : 203fcac9 : rejoué sur le code de 41d39ef5 : `CutBoutBigTextTest#aPieceJustAboveTheScrollThreshold` « the curve is 0.0.dp high at 100.0 % on 488.0.dp: a finger needs 120 dp »
rouge constaté : e33521f2 : rejoué sur le code de 4eeee030 : `RemixLeftThumbSeatTest#a8_oneCutButtonInProject` « two « Découper la chanson » buttons expected:<1> but was:<2> »
rouge constaté : b6d6ede5 : rejoué sur le code de 4eeee030 : `RemixLeftThumbSeatTest#a2_stemChipsReachableWithLeftThumb` « cut_stem_3 is out of the left thumb » et `#a5_spanToolsWholeOnArrival` « cut_start cut off »
rouge constaté : f678d1f0 : rejoué sur le code de 4eeee030 : `RemixLeftThumbSeatTest#a3_cutHintWholeNotUnderThumb` « cut_song_hint_… cut off (under the thumb bar) »

(Les rouges du siège de l'aller précédent ont été joués en une passe sur le code de production de 4eeee030, qui ne touche que le test. Pour b6d6ede5 et f678d1f0, ce n'est pas leur parent exact, qui contient déjà A8 ; mais les assertions A2, A3 et A5 n'en dépendent pas.)

## Bloquant

Aucun.

## Grave

1. **« Enlever » retire les bouts touchés pour placer la tête.** Vu à 864e3d90 : toucher 3, 7 puis 5, puis « Enlever », retirait 3 bouts au lieu d'un.
   - Trois passes :
     - 1ffefd76 (`byTouch`) : vidé par toute édition ;
     - 436ad6a6 (`ticked`, par bornes) : les choix de la tête sauvegardés redevenaient des coches à la réouverture (sonde : `removed=2 fiveGone=false`) ;
     - 59ce2e6d (`toSave`) : tout `onChange` ne sauvegarde que les coches faites exprès.
   - **corrigé** en 59ce2e6d. Sondes jouées à 3ec13da4 dans un worktree jetable, toutes justes :
     - réouverture : toucher 3 et 7, recréer le modèle sur la dernière sauvegarde, toucher 5, « Enlever » : `chosenAtOpen=[] removed=1 fiveGone=true` ;
     - vraie coupe (modèle de 16 bouts) : toucher 3, 7, 5, « Couper ici » (17 bouts), « Enlever » : 1 retiré ; de même après « Boucle » et après « Couper ici » + « Annuler » ;
     - un choix de la tête allume l'envoi sur les bons bouts : toucher 3 et 7 donne `toSend()` = 2 bouts, `chosen=[3, 7]`, et `[]` sauvegardé ;
     - une coche exprès survit à la réouverture : 4 et 9 cochés, `[4, 9]` rouverts ;
     - « Tout choisir », décocher le 4, « Enlever » : 63 sur 64, le 4 gardé ;
     - « Annuler » remet en mémoire le choix de la tête et la coche (`[3, 6]`), et sauvegarde seulement la coche (`[6]`) : cohérent ;
     - toucher un bout coché exprès par son numéro ou sa tuile le décoche, comme avant ;
     - `toSave` porte la piste dans sa clé : une autre piste n'est pas touchée (à la lecture du code).

## Moyen

2. **Un bout choisi par mégarde avec « placer la tête, puis Couper ici ».** C'est exactement la règle du tour 3 que ce delta lève. Sonde à HEAD : toucher dans le bout 10, puis « Couper ici » donne `chosen=[10]` et `toSend=1`. Les boutons d'envoi passent du gris à l'actif, et le bout part si on envoie, même si on voulait seulement couper.
   - Ce qui atténue : l'envoi demande un appui de plus, et la barre dit « N bouts choisis ».
   - C'est le prix voulu d'A7 (pouce gauche) ; il est maintenant écrit dans `docs/decisions/tour-6-remix2.md` (a1bbd8f1), avec son retour arrière.
   - Après une coupe, les indices `chosen` suivent les morceaux par recouvrement de plus de la moitié (`keepChoice`) : les moitiés d'un bout coché restent cochées, les autres indices sont recalculés. Pas d'indice périmé vu.
3. **`remix_song_mix.png` réenregistrée avec une fenêtre par-dessus le sujet** (réglé en 1ffefd76 : la capture est revenue à sa version de base). Les 7 captures `remix_*` réenregistrées montrent maintenant l'avis « La chanson dépasse la fin du morceau de 0:58 : sa fin sera coupée. » (`RhythmViewModel.kt` l.6296, lot 6 R4). Il vient de la base, pas du code du lot.
   - Comparée à sa version de a4b719df : sur `remix_song_mix.png`, il couvre « Batterie » et « Basse » du panneau que la capture doit montrer.
   - La capture ne vérifie donc plus ce qu'elle doit. `RemixSongScreenshotTest` devrait fermer l'avis (« Compris ») avant la prise.
   - `remix_board_button.png` n'a pas l'avis.
4. **Téléphones hauts et tablettes en portrait : la courbe de « Un bout » est plafonnée à 180 dp** (aller précédent, 203fcac9). Sur `cut_bout.png` (914 dp), il reste un vide d'environ un quart d'écran. Rien n'est bloqué. À décider, ou à noter dans les décisions.

5. **Une coche exprès coupée en deux n'est plus sauvegardée.** Sonde à 3ec13da4, modèle de 16 bouts : cocher 4 et 9 exprès, puis « Couper ici » dans le 4. En mémoire : `[4, 5, 10]` (les deux moitiés restent choisies, `keepChoice`). Sauvegardé : `[10]`. À la réouverture : `[10]`.
   - `ticked` est rangé par bornes, et les moitiés ont de nouvelles bornes. Il en va sûrement de même si l'on tire un bord ou si l'on fait « Début » / « Fin » sur un bout coché (les bornes changent pareil ; non sondé).
   - Aucun bout n'est perdu, seulement la coche : il faut recocher, un geste par bout. « Enlever » prend alors moins, jamais plus.
   - Avant ce lot, les coches survivaient aux coupes. À dire dans `docs/decisions/tour-6-remix2.md`, ou à faire suivre `ticked` comme `keepChoice`.
6. **Les tests « after cut » de `CutZoomGestureTest` ne coupent pas.** `setUp()` crée 90 bouts d'une mesure, ramenés à `MAX_PIECES` = 64 (`SongCuts.kt` l.81). `Cuts.split` refuse donc toute coupe à 64 bouts, et `cutHere` n'y fait rien. Le compte 63 l'atteste : 64 au départ, moins 1.
   - `removeAfterTouchesAndACutTakesOnlyThePieceUnderTheHead` et `…ACutAndUndo…` prouvent donc le cas « édition sans changement », qui était bien le défaut de a1bbd8f1 (rouge réel), mais pas une vraie coupe.
   - La vraie coupe ne tient aujourd'hui que par ma sonde. À faire : un `setUp` de 16 bouts pour ces deux tests.
7. **`CutJourneyTest#cutTheVoiceMoveACutSendThreePiecesUndoSaveAndReopen` est affaibli.** L'égalité `ticked == again.chosen` devient « 3 est là et rien de neuf n'apparaît ». Rejoué sur le code de c4aac715, il passe aussi : il ne distingue plus l'ancien comportement du nouveau.
   - Il laisserait passer la perte d'une coche faite exprès par une tuile plus haut dans le parcours (0 ou 2).
   - Honnête sur son intention, mais trop large. Mieux : retenir les tuiles cochées exprès dans le parcours, et exiger `again.chosen` égal à exactement celles-là.

8. **Le rapport de zone dit plus que le siège Ba-3** (`tour-6-remix2-rapport.md`, delta 12e5e818).
   - Il écrit « Garde-fou « Enlever un seul morceau » tenu (Ba-3, écran 40) ». Or Ba-3, ligne A9, dit « aucun bouton « Quitar » vu ; en touchant le rond numéroté d'un des 2 morceaux, seul celui-là part (écran 40, 2 → 1 trozo) ». `screen-40.txt` montre « 1 trozo elegido » et aucun « Quitar ».
   - Le siège a décoché un choix ; il n'a jamais touché « Enlever », qui n'était pas à l'écran en Compases (A4, pas pris). Le garde-fou « Enlever un seul » n'est prouvé que par les tests et mes sondes, pas par le siège.
   - « Buts du siège 3/3 » n'est écrit nulle part par Ba-3. On peut le déduire : `abandon: jamais`, séparation, découpe et pose faites (écran 70). Il faut l'écrire comme une lecture du chef.
   - À reformuler (fiche, point 6), par exemple : « A9 : décocher par le rond numéroté retire un seul choix (écran 40) ; « Quitar » n'a pas été vu ».
   - **corrigé** (documenté) en e5ba40c5. Le rapport dit maintenant qu'à l'écran 40, un bout a été décoché par son rond, que « Enlever » n'a pas été touché, et que le garde-fou n'est prouvé que par les tests. « Buts 3/3 » y est donné comme une lecture du chef.
9. **Ba-3, défaut du geste 27 : il faut 4 gestes, sans aide, pour retirer un des deux bouts choisis** (« contourne-en-4-gestes », « sans-aide »). Le siège a retouché le bout, puis fait un appui long : rien ne se passe, car A7 choisit sans jamais décocher. Seul le rond numéroté décoche.
   - Plus de 3 gestes, mais `revu: non` le ramène à moyen par la règle de `fuse-reports.py` : cohérent avec « 0 grave neuf ».
   - Le rapport de zone ne le cite pas, alors qu'il découle directement du choix « jamais décoché » d'A7. À noter dans le rapport et les décisions : une aide sur le rond numéroté, ou un second toucher sur un bout choisi par la tête qui le décoche.
   - **corrigé** (documenté) en e5ba40c5. Le rapport le cite comme moyen de Ba-3, pas pris dans ce lot. `docs/decisions/tour-6-remix2.md` y consacre la ligne « Décocher après A7 », avec sa raison et son retour arrière. Le défaut lui-même reste à jouer par un siège.

## Mineur

10. Commentaires périmés : `CutScreen.kt` l.217 (« when the screen scrolls (a large font) »), l.228 (« the curve takes the room »), l.242 (« that fits without scrolling »), (le commentaire périmé de `CutWave.kt` l.249-252 a été corrigé en 1ffefd76).
11. À 200 %, la 4e puce (« Resto ») est coupée net au bord droit, sans fondu. Elle reste atteignable en glissant. Le défaut existait déjà avant le lot.
12. A2 n'est prouvé qu'à 100 % : à 130 %, `bigText` est faux et les puces sont sous le pli.
13. A7 (bande vive encadrée) n'a été regardé qu'en sombre ; le jaune et le vert clair, sur une toile claire, n'ont pas été vérifiés.

Verdict : 0 bloquant, 0 grave
