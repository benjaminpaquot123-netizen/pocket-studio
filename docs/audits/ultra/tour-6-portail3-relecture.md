# Tour 6, lot du banc « portail3 » : relecture neuve (deux passes)
Relu : e5385415596b5a1cf295911a01e7b0c1ce0a776b

Relecteur neuf : je n'ai écrit aucune ligne de ce lot. Première passe sur `a4ff1473` (2 graves), seconde sur `e5385415` (les corrections de la première). Lu : `git diff cc6175a4..a4ff1473` puis `git diff a4ff1473..e5385415`, `AUDIT-ULTRA.md` §13, les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`, la fiche 6 du gabarit, le rapport et les décisions du lot. Gradle n'a pas été lancé (consigne) : rien ici ne parle des tests Kotlin ni des rejeux.

## Ce qui a été refait, première passe (`a4ff1473`)

- Tests du lot : `test_pre_vol.sh` « tout est vert », `test_socle.py` OK, `test_portail3.py` 68 tests OK, `test_ultra_bench.py` OK (atelier jetable resté immobile).
- Quatre rouges rejoués sur le parent de leur vert, tous rouges sur une assertion puis verts : `d2ca9e3b` puis `6f245a20` (le rouge tombe surtout parce que l'option `--fin-nommees` n'existe pas encore) ; `6af1fcfb` puis la tête ; `b500b6d7` puis les fichiers de `8e08f8cc` (13 ROUGE, puis vert) ; `ba77ddf3` puis les fichiers de `acd00ce6` (13 échecs, puis OK).
- 33 contrôles cassés exprès, un par un, puis remis : 22 font rougir leur test, 11 non (M4).
- Le juge sur la tête : 15 écarts nommés, comme le rapport le dit.
- 18 des 31 lignes nommées regardées avec `git show --stat` des deux commits : aucune fausse paire trouvée. Quatre verts retouchent aussi le test de leur rouge (`4d88d329`, `5fadb52b`, `05a0d767`, `ecc00a17`) : sans rejeu, personne n'a vu le rouge d'origine passer ; les décisions le disent.
- Sièges : un rapport de deux lignes dans un siège de plus ne change rien aux sorties de `fuse-reports.py` et de `tableau-de-bord.py` (lot A du tour 6, sorties comparées).
- `.gitignore` : seuls les quatre dossiers `etat/` de `fumee-finale` sont à la fois suivis et ignorés.
- Attaques essayées : deux fausses lignes dans le fichier des paires nommées (le juge passait de 35 à 34 chaque fois) ; sur un dépôt jouet, la gravité abaissée dans la branche, la ligne retirée, l'état suivi d'autres mots, le tri récrit (sortie 0 chaque fois, contre 1 pour le témoin).

## Ce qui a été refait, seconde passe (`e5385415`)

- Tests : `test_portail3.py` 77 tests OK ; `test_pre_vol.sh` « tout est vert » ; `test_socle.py` OK ; `test_ultra_bench.py` « ultra bench scripts: OK » (atelier jetable resté immobile).
- Le rouge neuf `04e34782` rejoué sur son commit, parent du vert : 7 échecs de la classe `ReviewOfTheLot`, chacun sur une assertion. Avec les fichiers du vert `0d7b4061` posés dessus : 76 verts, et un rouge, `test_this_lot_s_own_report_passes` (le rapport nomme maintenant G1, que le tri du dépôt jouet connaît). Le commit suivant `e5385415` le règle : la tête est verte. Le commit « vert: » n'était donc pas tout vert seul (m6).
- Mes attaques de G1, rejouées : une ligne ajoutée au fichier, une ligne vide ajoutée, le fichier retiré. Le juge répond 69, 69 et 68 écarts au lieu de 15, avec sa ligne « changé depuis le lot portail3 … aucune paire nommée n'est lue ». Le fichier remis : 15. Plus rien ne se cache par ce fichier sans toucher aussi l'empreinte dans `verifie-chantier.py`, ce qui est un changement du juge, visible.
- Mes attaques de G2, rejouées sur le dépôt jouet : gravité abaissée dans la branche, ligne retirée, état suivi de « (lot z) », tri passé à moyen, tri passé à « voulu » : sortie 1 chaque fois, avec la bonne ligne. Un lot honnête passe : la ligne `corrige:` avec « rejeu impossible : raison », et un lot qui ne touche pas la table.
- Esquives neuves essayées contre G2 : la colonne « gravite » renommée dans la branche (refusé, bien) ; la colonne « etat » renommée (accepté : M8) ; l'état écrit sans accent ou « fait » (accepté, mais aucun programme du banc ne lit alors la ligne comme fermée) ; ni base ni tête données (accepté, mais le portail et le pré-vol donnent toujours les deux) ; un bloc de code jamais refermé qui cache un numéro du tri (accepté : nommer reste volontaire, les décisions le disent).
- Le rapport du lot : `rapport-zone.py e5385415 …` l'accepte ; avec `--corrections … base:cc6175a4 tete:cc6175a4 tour:tour-6` aussi (sortie 0, plus rien).
- Neuf contrôles neufs cassés exprès : sept font rougir leur test (l'empreinte, les trois lectures de G2, le mot avec sa suite, la ligne retirée, les blocs de code, la branche gardée de côté) ; deux non (M4).
- M2 : la branche locale remplacée est dite et gardée sous `refs/portail-file/…` (test vu rouge sans la ligne). Le refus propre quand un atelier tient la branche est lu dans le code, pas couvert par un test.

## Constats

| id | gravité | constat | fichier:ligne | état |
|---|---|---|---|---|
| G1 | grave | Le fichier des paires nommées n'était pas gelé : un lot futur y ajoutait une ligne et le juge cachait un écart (essayé deux fois, 35 puis 34). | `scripts/bench/verifie-chantier.py:1161`, `:1173` | **corrigé** en `0d7b4061` : empreinte `NAMED_SHA256` ; fichier changé, aucune paire lue et un écart de plus ; revérifié (69, 69, 68 écarts au lieu de 15) |
| G2 | grave | La règle « correction sans rejeu » lisait la gravité dans la branche seule : gravité abaissée, ligne retirée, état suivi d'autres mots ou tri récrit passaient. | `scripts/bench/rapport-zone.py:155`, `:198`, `:232` | **corrigé** en `0d7b4061` : gravité lue dans la branche, sa base et la tête, la plus forte gagne ; revérifié sur cinq attaques, sortie 1 |
| M1 | moyen | Le bloc de code du rapport qui montre le format était lu comme des lignes mal formées. | `scripts/bench/rapport-zone.py:169` | corrigé en `0d7b4061`, revérifié sur le rapport du lot |
| M2 | moyen | La file déplaçait de force la branche locale sans le dire, et mourait sur l'erreur brute de git quand un atelier la tenait. | `scripts/bench/portail-file.sh:104` | corrigé en `0d7b4061` ; la partie « dite et gardée » revérifiée |
| M3 | moyen | Un lot relancé pour un seul siège récrit `code-teste.tsv` en entier : le commit noté des autres sièges est perdu. | `scripts/bench/sortie-de-siege.py:140` | ouvert, voulu par le lot |
| M4 | moyen | Douze contrôles neufs sans test : les onze de la première passe, plus le refus propre de la file quand la branche ne peut pas bouger. | `verifie-chantier.py` ; `portail-file.sh:42`, `:44`, `:111`, `:126` ; `paquet-verifie.sh:131`, `:147` ; `rapport-zone.py:181`, `:238` ; `fusion-equipes.sh:190` | ouvert |
| M5 | moyen | Un siège qui n'a rien écrit passe `verifie` : les deux lignes du banc suffisent. | `scripts/bench/sortie-de-siege.py:187` | ouvert |
| M6 | moyen | Le refus d'un rapport de siège n'arrête rien : sortie avalée, `rapports_refuses` lu par personne, pré-vol non branché. | `scripts/human-tester.sh:42` | ouvert |
| M7 | moyen | Premier `--ouvre` sans ligne au registre : le juge compte pour vert sans être comparé. | `scripts/bench/paquet-verifie.sh:226` | ouvert |
| M8 | moyen | Une branche qui renomme la colonne « etat » de `verdicts.tsv` ferme une ligne lourde sans que la règle la voie (essayé). Pas discret : l'en-tête d'une table partagée change et les autres programmes ne lisent plus aucun état. | `scripts/bench/rapport-zone.py:243` | ouvert |
| m1 | mineur | Le rapport disait 34 tests pour `Batch`. | rapport, ligne 14 | corrigé (32) |
| m2 | mineur | Le rapport citait `human-tester.sh`, que le lot ne change pas. | rapport, ligne 16 | corrigé |
| m3 | mineur | La file saute le pré-vol sans le dire s'il n'est pas exécutable ; elle dit « conflit » pour tout échec de la fusion jetable. | `scripts/bench/portail-file.sh:96`, `:112` | ouvert |
| m4 | mineur | `PS_PAQUET_CI_JSON`, porte des tests, reste ouverte dans le vrai script. | `scripts/bench/paquet-verifie.sh:52` | ouvert |
| m5 | mineur | La file ne voit une vague que si « VAGUE » est le premier mot, en capitales. | `scripts/bench/portail-file.sh:55` | ouvert |
| m6 | mineur | Le commit « vert: » `0d7b4061` laisse un test rouge, réglé par le commit suivant ; le test de M2 est entré dans ce vert sans rouge à lui. | histoire du lot | ouvert, dit ici |
| m7 | mineur | Seuls les blocs à trois accents graves sont sautés : un bloc `~~~` qui montre le format est encore lu (essayé). La référence gardée de côté par la file est écrasée au passage suivant du même lot. | `scripts/bench/rapport-zone.py:169` ; `scripts/bench/portail-file.sh:108` | ouvert |

## Ce qui tient

- Le portail, règle du juge : fusion bien annulée, trace d'avant trouvée par l'histoire, rien ne change avant le paquet 4 que deux lignes d'information.
- `paquet-verifie.sh` : aucun chemin trouvé où du non-vert sort vert, hors M7.
- La file ne pousse que la tête de l'atelier après un portail réussi, jamais de force.
- Les corrections de la seconde passe ne refusent pas un lot honnête dans mes essais, et le lot passe sa propre règle.

## Les moyens ouverts, une ligne chacun (pour `docs/REVIEW.md`)

- M3 : `sortie-de-siege.py avant` doit garder dans `code-teste.tsv` les lignes des sièges qu'il ne relance pas.
- M4 : un test par contrôle neuf pour les douze contrôles nommés.
- M5 : `verifie` doit refuser un rapport sans bloc « siege: ».
- M6 : brancher `verifie` sur le pré-vol ou sur la lecture du lot.
- M7 : sans ligne au registre, `--ouvre` doit demander la ligne du paquet d'avant.
- M8 : `rapport-zone.py --corrections` doit refuser une table `verdicts.tsv` dont l'en-tête n'a plus ses colonnes « item », « gravite », « etat ».

Verdict : 0 bloquant, 0 grave
