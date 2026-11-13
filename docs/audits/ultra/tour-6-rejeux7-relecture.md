# Tour 6, lot du banc « rejeux7 » : relecture neuve
Relu : b0590d2c6f760b11467c3324bd5947552e9add4c

Relecteur neuf (n'a rien écrit de ce lot). Lu : la fiche du relecteur (`docs/AIDE-FUSIONS-TESTS.md`), le rapport, `docs/decisions/tour-6-rejeux7.md`, `git diff cc6175a4..b0590d2c`.

## Ce que touche le lot

- Hors `docs/` : rien (0 fichier). Seul fichier existant modifié : `docs/audits/ultra/EN-COURS.md`, venu de la fusion de l'intégration (`ea7a1e25`), pas du lot.
- `rejeux/t4-26-prise-clavier/` : intact (aucune ligne du diff) ; seul son journal de passage `_journaux/t4-26-prise-clavier/` est ajouté (« oracle tenu »). Aucun rejeu existant retiré ni changé.

## 1. Les trois rejeux ajoutés

Gestes comparés au `journal.txt` du siège source, ligne `#i DOIGT <étiquette>`, geste par geste :

| rejeu | siège | gestes | oracle contre l'écran n+1 du siège |
|---|---|---|---|
| `t6-ec17-un-bout-courbe` | lotE/c, 1 à 17 | identiques par étiquette ; les glissés 2, 3, 11 à 16 deviennent « attends 1 » (règle de `rejeu-etiquettes.py`) ; `cut_song_*` = `cut_song_3a02…` raccourci par l'outil | « Pince para ampliar » absent de l'écran 18 du siège |
| `t6-ga14-phrases-batterie` | lotG/a, 1 à 14 | identiques, un pour un | « (Davul · 16 pad) » absent de l'écran 15 |
| `t6-da15-ecoute-passee` | lotD/a, 1 à 15 | identiques, un pour un | « チャプター 1/15 · 完了 » absent de l'écran 16 |

- Aucun pixel, aucune option `--cible`, `--saute`, `--commande`.
- Les oracles collent aux défauts de `verdicts.tsv` : R3-EC17 (bloquant, la courbe n'a pas de place en « Un bout » à 200 %), R6 (grave, phrases sans nommer la batterie), N19 (grave, l'écoute passée par « Suivant » ne compte pas).
- Journaux `_non-tenu-1585bb46/` : les trois finissent en « oracle non tenu : … absent » ; les étiquettes touchées sont exactement celles du siège ; 0 geste refusé, 0 étiquette introuvable. Journaux `_journaux/` : « oracle tenu », mêmes étiquettes.

## 2. En attente et impossibles

- `t6-ha19-cases-panneau` (T4, cases) : non tenu sur 1585bb46 et **non tenu sur la tête** (journaux commités). Le ranger dans `rejeux-attente/` est le bon choix : dans `rejeux/`, il bloquerait toute fusion.
- T4 (notes du piano roll), N4, N5 : la raison tient. Les notes du piano roll ne sont pas dans le texte de l'écran (aucun oracle possible), et le chemin passe par des touchers sans étiquette (pixels interdits). N19 depuis D a-suite : couvert par `t6-da15`, même cause, même siège.
- Rejeu oublié : aucun. Les seuls bloquants ou graves de `verdicts.tsv` sont R3-EC17, T4, R6, N4, N5 et N19 ; chacun est rejoué, en attente ou classé impossible. T3, cité dans les décisions, n'est ni bloquant ni grave.

## 3. Rejoué moi-même

- Machine : pas de SDK Android au départ. Installé (`/opt/android-sdk` : plateforme 37.0, build-tools 37, NDK 30.0.16248370, CMake 3.31.0), sous-modules initialisés, `local.properties` créé (hors commit).
- Maven Central : 429 au premier essai, 200 dix minutes plus tard.
- Tête (`b0590d2c`), `scripts/bench/rejeu-defauts.sh t6-ga14-phrases-batterie` :
  - 1er passage : self-test rouge, les rejeux n'ont pas été joués. Cause : le téléchargement de l'image Robolectric `android-all-instrumented:16-…-i7` a échoué (le réseau, pas le code) ; les autres classes n'ont pas tourné.
  - 2e passage : self-test vert, puis **« t6-ga14-phrases-batterie : oracle tenu »**. Mêmes 12 étiquettes que le siège.
- 1585bb46 (worktree détaché, banc de ce commit, `PS_REJEUX_DIR` = copie du dossier de la branche) : self-test vert, puis **« oracle non tenu : « (Davul · 16 pad) » absent »**. Mêmes 12 étiquettes, 0 refusé, 0 introuvable. Le worktree a ensuite été retiré, et le journal réécrit dans l'arbre a été remis tel quel.
- Pas rejoués moi-même : `t6-ec17` et `t6-da15`. Pour ces deux-là, j'ai vérifié leurs journaux commités (point 1).

## 4. Rapport de zone

`python3 -I scripts/bench/rapport-zone.py ccr-a548b78b-rejeux7 docs/audits/ultra/tour-6-rejeux7-rapport.md ajout:t6-ec17-un-bout-courbe ajout:t6-ga14-phrases-batterie ajout:t6-da15-ecoute-passee` : code 0. Filtres sortis : `CutBoutBigTextTest`, `StemBoardRouterTest`, `Tour6TutoPanelListenSkipTest` ; les quatre tests de la liste des chemins existent dans `app/src/test`. Aucun test existant ne change de couleur : le lot ne touche aucun code.

## Points moyens et mineurs laissés ouverts

- moyen : T4 (cases en pointillé coupées, téléphone, 100 %) n'est **pas corrigé** sur la tête (`t6-ha19` non tenu). Le chef doit le rendre au tri, zone tuto (`RhythmScreen.kt`), sinon T4 restera compté comme corrigé à tort.
- moyen : l'oracle de `t6-ec17` (« Pince para ampliar » visible) prouve que la ligne de la courbe a sa place, pas que la courbe a une hauteur correcte. Une courbe écrasée comme en R3 (≈ 50 dp, moyen) le tiendrait encore. Le test `CutBoutBigTextTest` couvre la hauteur ; le rejeu, lui, ne protège que le bloquant.
- mineur : dans les passages sur 1585bb46, les écrans ne sont pas comparés à l'origine (« pas d'écran-texte à l'origine »). La non-divergence repose sur les étiquettes, qui sont identiques. Les `verdict.txt` gardent aussi des chemins absolus `/tmp/…` d'une autre session.
- mineur, côté machine : un premier passage peut tomber en self-test rouge si Robolectric ne télécharge pas son image. Relancer suffit.

Verdict : 0 bloquant, 0 grave
