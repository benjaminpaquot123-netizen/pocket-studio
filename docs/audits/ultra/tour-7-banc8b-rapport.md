# Lot « banc8b » (tour 7) : trois défauts du banc vus au contrôle tuto2

Base : 108b200b7 · branche `eq/tour-7-banc8b` · contrôle d'origine : code f1b9fdb1f, `docs/audits/ultra/sieges/tour-7/controle/tuto2/`.
Les trois sont des défauts du banc ; aucun n'est un défaut de l'appli (aucune ligne « appli (...) » ajoutée à `incidents-banc.tsv`).

## 1. Siège c : `amenee=tuto:techno:9` ouvrait le chapitre 1 (fait)
- Cause : en coréen, sur phone360 à 130 %, la ligne du chapitre 9 était coupée par le bord de la liste. Le banc ne la faisait pas défiler, car son milieu « non coupé » tombait dans la zone du milieu de l'écran. Ce milieu (y 923) était en fait sur « Recommencer le tuto », sous la liste. Résultat : un projet neuf, au chapitre 1.
- Correction (`HumanTester.kt`) :
  - la ligne visée défile toujours jusqu'à être vue ;
  - le banc ne touche jamais un milieu hors de la partie dessinée de la ligne ;
  - l'amenée tuto échoue en clair (« le tuto est à …, pas au chapitre N ») si l'étape du tuto n'est pas dans le chapitre demandé.
- Test : `BenchAmeneesTest.tutoAnchorLandsOnItsChapterWhenItsRowIsCut` (ko, phone360, 130 %, simple, `tuto:techno:9`). Rouge avant : tuto à « ref », chapitre 1. Vert après.

## 2. Siège d : texte vide aux écrans 16 et 39, image normale (fait)
- Cause : l'anneau du tuto (`tuto_spot_ring`, « Je le fais ») et son doigt (`tuto_finger`, « Montre-moi ») sont des dessins sur tout l'écran qui laissent passer les touchers. Le banc les prenait pour une page posée par-dessus (comme Pad Live) : il cachait tout ce qui était dessous et donnait l'anneau comme route. C'est aussi la cause des lignes « banc? » A/a 3 et 102 de `incidents-banc.tsv`.
- Correction (`ScreenText.kt`) : ces deux étiquettes, nommées une à une (`PASS_THROUGH_TAGS`), ne cachent plus rien et ne sont plus la route. Une autre surcouche plein écran inconnue cache toujours ce qu'elle couvre (pas de défaut d'appli masqué).
- Tests : `BenchScreenTextOverlayTest`.
  - Anneau : rouge avant (texte vide), vert après.
  - Doigt : vert après.
  - Témoin : une vraie page plein écran cache toujours ce qui est dessous.

## 3. Siège e : image du dernier écran refusée après des « attends » (fait)
- Cause : le plafond (une image pour 5 gestes) ne compte pas « attends ». Un siège qui finit sur des attentes n'a donc jamais droit à l'image de l'écran atteint.
- Correction (`scripts/human-act.py`) :
  - l'image du dernier écran est permise dès que le siège est fini (« fin » joué, ou fin posée par le banc), quel que soit le plafond ;
  - en cours de jeu, le refus du plafond le dit (« L'image du dernier écran reste permise après « fin » »).
- Test : `last_screen_image_after_waits` dans `scripts/bench/test_ultra_bench.py`. Rouge avant, vert après.

## Vérifications
- `scripts/bench/selftest.sh` : vert, 222 verts, 0 rouge, 0 en quarantaine, 44 sautés. Tampon vérifié sur le code.
- `scripts/bench/rejeu-defauts.sh` : 5 oracles tenus.
  - Seul t4-26-prise-clavier reste rouge (défaut d'appli connu, laissé tel quel).
  - `rejeux/_echecs/` a été supprimé.
- Tests `com.pocketstudio.app.human.*` : 326 tests, 0 échec, 58 sautés.

## Ce qui reste
- Les lignes A/a 3 et 102 (« banc? : anneau du tuto ») de `incidents-banc.tsv` ont la même cause que le défaut 2. Un prochain contrôle doit le constater.
- Si l'appli ajoute une autre surcouche plein écran transparente, il faudra l'ajouter à `PASS_THROUGH_TAGS`.
