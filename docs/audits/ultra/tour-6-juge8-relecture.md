# Tour 6, lot du banc « juge8 » : relecture neuve (trois passes)
Relu : b64b0025e1b148244e822509862e7129a6ee0a44

Relecteur neuf : je n'ai écrit aucune ligne de ce lot. Gradle n'a pas été lancé (consigne) : rien ici ne parle des tests Kotlin.
- Première passe sur `63db6470` : 2 graves.
- Seconde passe sur `96408f3b6` : G1 et G2 corrigés, un grave neuf, G3.
- Troisième passe sur `b64b0025e` : G3 corrigé.

Lu : `git diff bdb777a94..63db6470`, `git diff 1d8f21829..96408f3b6`, `git diff 96408f3b6..b64b0025e`, le rapport et les décisions du lot, la docstring du juge, les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`, `scripts/bench/relecture-verdict.py`.

## Ce qui a été refait, première passe (`63db6470`)

- Suites : `test_ultra_bench.py` OK, `test_socle.py` 29 OK, `test_portail3.py` 77 OK, `test_pre_vol.sh` « tout est vert ».
- Paires rejouées jouet par jouet (banc de chaque commit sorti par `git archive`) : `c945c5d91`, `86fd28df9` et `4eba9c9d0` rouges, puis `07dbf052c`, `67e9d885b` et `23119e421` verts.
- Juge de la base et juge du lot sur le même arbre : 51 et 51, sorties identiques. Avec remix2 fusionné : 57 et 57.
- Attaques A1 à A8 et onze contrôles cassés : constats G1, G2, M1, M2.

## Ce qui a été refait, seconde passe (`96408f3b6`)

- Suites : les quatre vertes.
- `be2577d42` et `045359711` rouges, puis `4dc547f45` et `bba9b2026` verts.
- Le juge sur la base réelle, trace `langues2` : 51 avec les juges de la tête, de l'intégration `8afb9abc6`, de `bba9b2026` et de `4dc547f45`, sorties identiques. Le commit de vitesse ne change aucun résultat (13 s au lieu de 1 min 30).
- A1, A2, A3, A5, A6 et A8 refusés. Esquives neuves :
  - B1, un lot parti de la pointe d'un lot déjà fusionné, est apparié : c'est G3.
  - C1 à C4 sont refusés.
  - C5, C6 et C8 sont acceptés (M4, M5). C7 est accepté, c'est voulu.
- Six contrôles cassés : quatre rougissent, deux non (M3).

## Ce qui a été refait, troisième passe (`b64b0025e`)

- Suites sur la tête, arbre calme : `test_ultra_bench.py` « ultra bench scripts: OK » (sortie 0, arbre propre après), `test_socle.py` OK, `test_portail3.py` 77 OK, `test_pre_vol.sh` « tout est vert ».
- `768b88518` : `closed_red_same_lot_only` rouge, « a red already inside a « Merge team » gives a later lot nothing ». `0ede90223` : vert. Ce vert ne touche aucun test.
- Le juge sur la base réelle (tête, `--resume` de la trace `langues2`) : 51 écarts avec le juge de la tête et 51 avec celui de l'intégration `8afb9abc6`, sorties identiques (`diff` vide).
- `eq/tour-6-remix2` fusionné dans une branche jetable sous le sujet du portail « Merge team remix2 of tour-6 (audit ultra) », branche supprimée ensuite :
  - 58 écarts avec le juge de la tête et 58 avec celui de l'intégration, sorties identiques.
  - Par rapport aux 57 de la première passe, il y a une ligne de plus : « fusion sans trace du portail », normale pour une fusion jetable.
  - La fusion « Merge team remix2 » n'est pas un ancêtre des verts de remix2 (ce sont eux qui sont ses ancêtres). La règle ne retire donc rien à remix2.
- Esquives contre le chemin d'ascendance (`attack2.py`, jouets) :
  - B1, un lot parti de la pointe d'un lot déjà fusionné, est maintenant refusé, bien.
  - B2 (un lot parti de l'intégration qui la refusionne en route), B3 (l'intégration en premier parent) et B6 (un lot parti de l'intégration juste avant la fusion de K) sont refusés, bien.
  - B7, lot honnête (le rouge, un vert, l'intégration fusionnée en route, le second vert du même lot) : apparié, bien.
  - B5, un lot fusionné dans l'intégration par une fusion qui ne s'appelle pas « Merge team … », puis un lot parti de sa pointe : apparié (M6).
  - B8, un lot qui nomme lui-même « Merge team … » sa fusion de l'intégration : son second vert est refusé. C'est un refus, jamais une fausse paire (m8).
  - B4, rouge et vert repris par `cherry-pick` : apparié, comme avant (m6).
- Dans l'histoire réelle depuis `8f36fa3`, les fusions de la ligne de l'intégration qui ne s'appellent pas « Merge team » sont `2bb86bc3a`, `292d0244e`, `a6a4b7878` et `50cbc3feb` (lots de documents). Aucune n'apporte de commit `rouge:` ou `vert:`.
- Trois contrôles cassés un par un sur une copie jetable du juge de la tête, puis remis (tout vert) :
  - « Merge team » sur le chemin d'ascendance non cherché : `closed_red_same_lot_only` rouge, « a green across a « Merge team » stays a gap ».
  - Fichier de test ajouté ou retiré accepté : `juge8_controls_second` rouge (« added »).
  - Toute fonction portant `@Test` retirée du texte comparé : `juge8_controls_second` rouge (« multiann »).
  - Le jouet de M3 rougit donc comme le lot le dit.

## Constats

| id | gravité | constat | fichier:ligne | état |
|---|---|---|---|---|
| G1 | grave | Un vert seul s'appariait à tout rouge fermé de tout lot en citant un test non pris, même sans code (A1, A6). | `scripts/bench/verifie-chantier.py:1465` | **corrigé** en `4dc547f45` : A1 et A6 refusés, quatre contrôles cassés chacun rouges (seconde passe) |
| G2 | grave | Seul le corps du test nommé était comparé : aide, `@Before`, annotation changés passaient (A2, A3, A5). | `scripts/bench/verifie-chantier.py:1022` | **corrigé** en `bba9b2026` : A2, A3, A5, C1 à C4 refusés ; restes en M4 |
| G3 | grave | Un lot ultérieur parti de la pointe de la branche d'un lot déjà fusionné s'appariait au test non pris de ce lot (B1). | `scripts/bench/verifie-chantier.py:705` (`lot_line`) | **corrigé** en `0ede90223` : B1 refusé ; B2, B3, B6 refusés ; B7 (honnête) apparié ; contrôle cassé, rouge |
| M1 | moyen | `vert.xml` reste la parole du lot. Les Demandes qui le font rejouer sont dans le rapport, pas encore dans la fiche ni dans `relecture-verdict.py`. | `scripts/bench/verifie-chantier.py:1085` | ouvert, voulu |
| M2 | moyen | Cinq contrôles de la première passe sans test qui rougit. | `scripts/bench/test_ultra_bench.py` | corrigé en `1e1f15900` (quatre rougissent, le cinquième ne peut pas : m5) |
| M3 | moyen | Deux contrôles de la seconde passe sans test qui rougit. | `scripts/bench/test_ultra_bench.py` | corrigé en `7a2c2db18`, recassés : rouges |
| M4 | moyen | Restes de G2 : un test nommé qui appelle une autre fonction `@Test` (C6), ou une aide plus indentée sous un autre test à corps d'expression (C8), se laissent affaiblir. Il faut un commit de préparation. Ce n'est pas plus fort que la voie qui existait déjà par une aide dans `src/main`. | `scripts/bench/verifie-chantier.py:1003` (`_fun_end`) | ouvert |
| M5 | moyen | Préexistant (lot « portail ») : le vert d'une preuve `rouges-constates/` peut changer `app/build.gradle.kts` et exclure son test (C5). | `scripts/bench/verifie-chantier.py:1067` | ouvert |
| M6 | moyen | La frontière d'un lot se lit au nom « Merge team … ». Un lot fusionné dans l'intégration sous un autre nom (B5, essayé) garde ses tests non pris ouverts à un lot parti de sa pointe. Un lot empilé sur un lot pas encore fusionné, puis fusionné avant lui, aussi (lu dans le code, pas essayé). Aujourd'hui seul le portail fusionne, sous ce nom ; les quatre fusions faites à la main de l'histoire n'apportent aucun rouge. | `scripts/bench/verifie-chantier.py:723` | ouvert |
| m5 | mineur | Le contrôle « # » dans `test:` ne fait rien de plus que `test_in` : il ne peut pas rougir. | `scripts/bench/verifie-chantier.py:1065` | ouvert, sans effet |
| m6 | mineur | Un rouge et son vert repris par `cherry-pick` ouvrent leurs tests au lot qui les reprend (B4). Ce n'est pas neuf : un lot peut écrire son propre rouge. | — | ouvert, dit ici |
| m7 | mineur | Un vert régularisé peut retirer un autre test simple du fichier (C7). | décisions | ouvert, voulu |
| m8 | mineur | Un lot qui nomme lui-même « Merge team … » sa fusion de l'intégration se fait refuser son vert suivant (B8). C'est un refus, jamais une fausse paire. | `scripts/bench/verifie-chantier.py:723` | ouvert |

## Recommandation sur `vert.xml` (M1, inchangée)

`vert.xml` est acceptable seulement si la fiche du relecteur exige de le rejouer : « Si le dossier porte un vert.xml : « rouge constaté : <sha du vert> : rejoué sur <sha de son parent> : <ce que tu as vu> ; rejoué sur <sha du vert> : <ce que tu as vu> » : le test échoue sur le parent et passe sur le vert. » `relecture-verdict.py` doit aussi exiger ce second « rejoué sur ». Les deux Demandes du rapport le disent. Tant qu'elles ne sont pas appliquées, `vert.xml` est la parole du lot.

## Les moyens ouverts, une ligne chacun (pour `docs/REVIEW.md`)

- M1 : faire rejouer `vert.xml` par la relecture et le faire vérifier par `relecture-verdict.py` (Demandes du rapport).
- M4 : dans `without_other_tests`, ne retirer une autre fonction `@Test` que si le test nommé ne l'appelle pas, et borner un corps d'expression à son expression.
- M5 : dans `rouges-constates/`, refuser un vert qui change un fichier de build (`*.gradle*`).
- M6 : le portail seul fusionne les lots dans l'intégration, toujours sous « Merge team … » ; sinon, lire la frontière d'un lot autrement que par un nom.

Verdict : 0 bloquant, 0 grave
