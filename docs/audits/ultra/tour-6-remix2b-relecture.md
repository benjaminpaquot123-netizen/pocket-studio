# Tour 6 — lot « remix2b » (zone Remix, siège B a, pouce gauche ; branche reconstruite) : relecture neuve
Relu : bdf4621b64215f31d31dd7750c5d08c60fbcbf47

Relecteur neuf (n'a pas écrit ce code). Branche `eq/tour-6-remix2b`, reconstruite depuis bdb777a9 en paires rouge/vert, une par test ; je l'ai relue commit par commit.

Delta e0ef9cba..bdf4621b relu : seul `tour-6-remix2b-rapport.md` change (erratum d'histoire, « Fichiers touchés », section Tenté, toile claire).

Code et tests identiques octet pour octet à 90803505 (code relu en e5ba40c5) : `git diff --stat 90803505 e0ef9cba -- app/src/main app/src/test app/screenshots` ne sort rien. La relecture de fond de `eq/tour-6-remix2` (ses quatre allers-retours, sondes comprises) vaut donc pour ce code ; l'essentiel en est repris ci-dessous, avec les sha neufs.

Vérifié sur la reconstruction :
- **Chaque « rouge: » ne contient que des tests** (`git show --stat`). Son parent n'a pas encore la correction : les 8 rouges de la branche, tous rejoués ci-dessous, rougissent.
- **Les méthodes nommées dans les messages existent dans l'arbre de leur commit**, sauf une (moyen n°1).
- **La table « anciens → nouveaux » du rapport et des décisions est juste sur le fond.** Comparaison fichier par fichier des diffs :
  - identiques pour e33521f2 → 4d51a05a, b6d6ede5+43a70627 → a8b147d5 (`CutScreen.kt`), f678d1f0 → 6cd5fb75 (`RemixScreen.kt`), 0800c4f6+41d39ef5 → a80fe9f4, 203fcac9 → c118321a+5714af68, 4cbd19c0+5893e286 → 7b8a2145+b5c5dc32 (code et tests de découpe), 1ffefd76 → 4fbe1811 (code et test), 436ad6a6 → 250716a6, 59ce2e6d → f74c36eb ;
  - seuls écarts, sans effet sur l'arbre final : les écritures de débogage `a3.txt`, `a5.txt`, `a8.txt` du test du siège ne sont plus là, et `remix_song_mix.png` n'est plus réenregistrée puis remise ;
  - la table ne nomme que les anciens verts (pas les anciens rouges 3e0461ff, 3bb00b84, f48a2ba7) : mineur n°10.
- **Rejoué à la tête du code** (3ec13da4, même code) : `:app:testDebugUnitTest --tests 'com.pocketstudio.app.remix.*'`, 308 tests, 0 échec. Les rouges ont été joués dans un worktree jetable depuis le dépôt principal, retiré ensuite. Jouer un commit « rouge: », c'est jouer ses tests sur le code de son parent.

rouge constaté : 4d51a05a : rejoué sur le code de bdb777a9 : `RemixLeftThumbSeatTest#a8_oneCutButtonInProject` « two « Découper la chanson » buttons expected:<1> but was:<2> »
rouge constaté : a8b147d5 : rejoué sur le code de 4d51a05a : `RemixLeftThumbSeatTest#a2_stemChipsReachableWithLeftThumb` « cut_stem_3 is out of the left thumb » et `#a5_spanToolsWholeOnArrival` « cut_start cut off »
rouge constaté : 6cd5fb75 : rejoué sur le code de a8b147d5 : `RemixLeftThumbSeatTest#a3_cutHintWholeNotUnderThumb` « cut_song_hint_… cut off (under the thumb bar) »
rouge constaté : 5714af68 : rejoué sur le code de a80fe9f4 : `CutBoutBigTextTest#aPieceJustAboveTheScrollThreshold` « the curve is 0.0.dp high at 100.0 % on 488.0.dp: a finger needs 120 dp »
rouge constaté : b5c5dc32 : rejoué sur le code de 5714af68 : `RemixLeftThumbSeatTest#a7_touchingAVoicePieceChoosesIt` « touching a voice piece chose nothing (« En los pads » stays grey) » et `#a8_oneCutButtonInProject` « a second ✂ on the page expected:<0> but was:<1> »
rouge constaté : 4fbe1811 : rejoué sur le code de b5c5dc32 : `CutZoomGestureTest#removeAfterTouchesTakesOnlyThePieceUnderTheHead` « expected:<63> but was:<61> »
rouge constaté : 250716a6 : rejoué sur le code de 4fbe1811 : `CutZoomGestureTest#removeAfterTouchesAndAnEditTakesOnlyThePieceUnderTheHead` (nom de la méthode dans 3a57ff12) « after cut expected:<63> but was:<61> »
rouge constaté : f74c36eb : rejoué sur le code de 250716a6 : `CutZoomGestureTest#removeAfterReopeningTakesOnlyThePieceUnderTheHead` « expected:<63> but was:<62> »

## Ce qui tient (repris de la relecture de `eq/tour-6-remix2`, code identique)

- **A8, aucun accès perdu à la découpe.**
  - Le ✂ de la carte disparaît seulement si la chanson est dans le projet et que son board est chargé. Dans ce cas, la rangée du pouce affiche « Découper la chanson » (`ui.board != null && !ui.home && ui.remix.setId != null`).
  - Board pas encore chargé : la carte garde son ✂.
  - L'ancien `remix_open_cut` s'affichait sous la même condition que la rangée du pouce.
- **A2, A3, A5.**
  - Les puces sont sous les outils à police normale, visibles sans défiler ; leur rangée défile horizontalement.
  - L'intro n'est masquée qu'une fois une chanson séparée.
  - En « Un bout », la colonne défile toujours derrière une courbe fixe de 120 à 180 dp ; le test de garde à 488 dp est vert.
- **A7.**
  - Toucher le corps d'un bout le choisit, sans jamais le décocher ; les coupes ne bougent pas, et une poignée saisie ne choisit rien.
  - « Enlever » ne prend que les bouts cochés exprès (`ticked`, par piste et par bornes), sinon le bout sous la tête. Les choix faits par la tête ne sont pas sauvegardés (`toSave`).
  - Sondé à 3ec13da4 (même code), et juste :
    - réouverture ;
    - vraie coupe sur un modèle de 16 bouts ;
    - « Boucle » ;
    - « Couper ici » + « Annuler » ;
    - « Tout choisir » puis décocher un bout ;
    - envoi des bons bouts après un choix de la tête (`toSend` = 2) ;
    - coche exprès gardée à la réouverture.
- **Son non touché.** Pas de C++, pas de callback audio. La sauvegarde n'est touchée qu'à travers les choix et les morceaux de `CutModel` (grave ci-dessous, corrigé).

## Bloquant

Aucun.

## Grave

Aucun ouvert. Le grave de la relecture de `eq/tour-6-remix2` (« Enlever » retirait les bouts touchés pour placer la tête : 3 retirés au lieu d'un) est **corrigé**, dans le même code, par 4fbe1811, 250716a6 et f74c36eb (anciens 1ffefd76, 436ad6a6, 59ce2e6d) ; les trois rouges sont rejoués ci-dessus.

## Moyen

1. **Un message de rouge nomme une méthode absente de son commit.**
   - 3a57ff12 (« rouge: CutZoomGestureTest#removeAfterTouchesAndACutTakesOnlyThePieceUnderTheHead, after a cut, a loop, an undo ») ne contient pas cette méthode. Le test qui rougit s'y appelle `removeAfterTouchesAndAnEditTakesOnlyThePieceUnderTheHead`, et enchaîne `setUp()` en boucle. C'est 250716a6 (le vert) qui le renomme et le découpe en `…AndACut…`, `…AndALoop…` et `…ACutAndUndo…`.
   - Le rouge est réel (« after cut expected:<63> but was:<61> »), mais un juge qui apparie par `Classe#méthode` ne trouvera pas la méthode du message dans l'arbre du rouge.
   - À corriger avant le portail : mettre dans 3a57ff12 les trois méthodes sous leurs noms définitifs, de sorte que 250716a6 ne touche plus que `CutModel.kt` ; ou faire nommer au message la méthode réelle.
   - Les 19 autres références `Classe#méthode` des messages existent dans l'arbre de leur commit.
   - **corrigé** (documenté, erratum) en bdf4621b, sans réécrire la branche poussée. Le rapport nomme le vrai test de 3a57ff12, son rouge, et sa découpe en 250716a6. Les deux commits portent exactement le même sujet, ce que j'ai vérifié : un juge qui apparie par le sujet les retrouve.
2. **Un bout choisi par mégarde avec « placer la tête, puis Couper ici »** (règle du tour 3 levée). Le bout part si on envoie. C'est voulu, et écrit dans les décisions.
3. **Téléphones hauts et tablettes en portrait : la courbe de « Un bout » est plafonnée à 180 dp.** Sur `cut_bout.png`, il reste un vide d'environ un quart d'écran. Rien n'est bloqué. À décider.
4. **Une coche exprès coupée en deux n'est plus sauvegardée.** Les bornes changent, et la coche est perdue à la réouverture. Aucun bout n'est perdu. La décision « Enlever après A7 » (deuxième passe) dit « aucune coupe, boucle ni annulation ne les perd » : c'est vrai pour « Enlever » en mémoire, pas pour la sauvegarde ; à préciser.
5. **Les tests « after cut » de `CutZoomGestureTest` ne coupent pas.** `setUp()` crée 90 bouts, ramenés à `MAX_PIECES` = 64, et `cutHere` n'y fait rien. Ils prouvent le cas « édition sans changement » ; la vraie coupe ne tient que par ma sonde. À faire : un `setUp` de 16 bouts.
6. **`CutJourneyTest#cutTheVoiceMoveACutSendThreePiecesUndoSaveAndReopen` est affaibli** (dans f74c36eb). L'égalité devient « 3 est là et rien de neuf n'apparaît ». Il passe aussi sur le code d'avant, et laisserait passer la perte d'une coche faite par une tuile.
7. **Ba-3, geste 27 : 4 gestes, sans aide, pour retirer un des deux bouts choisis** (`revu: non`, donc moyen). Documenté dans le rapport (pas pris) et dans les décisions (« Décocher après A7 »).

## Mineur

8. **Les messages des verts taisent une partie de leur diff.**
   - b5c5dc32 met aussi à jour deux attentes de `CutZoomGestureTest`, puis `RemixCutFromCardTest`, et 6 captures `remix_*`.
   - 250716a6 découpe et renomme le test (voir moyen n°1).
   - f74c36eb change l'assertion de `CutJourneyTest` (moyen n°6).
   - Rien n'y est faux, mais c'est incomplet.
9. **Le rapport `tour-6-remix2b-rapport.md` a vieilli par endroits.**
   - « Fichiers touchés » ne cite que `CutScreen.kt`, `CutWave.kt`, `RemixScreen.kt` et le test du siège. Or `CutModel.kt`, `CutZoomGestureTest`, `CutBoutBigTextTest`, `RemixCutFromCardTest` et `CutJourneyTest` sont touchés aussi.
   - La section « Tenté » dit « Le geste pour cocher n'est pas changé », contredite par la section Ba-2 juste en dessous.
   - « Relecture neuve (sous-agent, sur a80fe9f4) » dit « A7 non vérifié sur une toile claire (il n'y en a pas aujourd'hui) » : je n'ai pas vérifié qu'il n'y en a pas.
   - **corrigé** en bdf4621b, pour les trois points : « Fichiers touchés » est complet (code, cinq tests, captures) ; la section Tenté dit que le geste a changé au retour de Ba-2 ; la toile claire est dite « non vérifié s'il en existe une ».
10. **La table de correspondance ne nomme pas les anciens rouges** 3e0461ff, 3bb00b84 et f48a2ba7, ni 4cbd19c0 comme rouge de 7b8a2145 (il y est, mais nommé seul). Le fond est juste.
11. **Commentaires périmés** dans `CutScreen.kt` : l.217, l.228, l.242.
12. **À 200 %, la 4e puce (« Resto ») est coupée net sans fondu** (atteignable en glissant ; défaut d'avant le lot).
13. **A2 n'est prouvé qu'à 100 %** : à 130 %, les puces sont sous le pli.
14. **A7 (bande vive) n'a été regardé qu'en sombre.**

Verdict : 0 bloquant, 0 grave
