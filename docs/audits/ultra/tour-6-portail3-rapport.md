# Tour 6, lot du banc « portail3 » : ce que les programmes ont appris (AUDIT-ULTRA §13.9)

Branche : `eq/tour-6-portail3` (base `cc6175a4`, tête poussée de l'intégration). Décisions : `docs/decisions/tour-6-portail3.md`. Aucun fichier de `app/src/main` touché, `scripts/bench/portail-paquet.txt` non plus.

tests: aucun : lot du banc seul (scripts du portail, du juge et des sièges), aucun test Gradle changé ; banc : `python3 -I scripts/bench/test_ultra_bench.py`, `python3 -I scripts/bench/test_portail3.py`, `python3 -I scripts/bench/test_socle.py`, `bash scripts/bench/test_pre_vol.sh`

## Les six points de 13.9

| Point | Fait par | Preuve (rouge puis vert) |
|---|---|---|
| Le portail refuse un lot qui fait monter le juge (`from_paquet 4`, sortie 9) | `fusion-equipes.sh`, miroir dans `pre-vol.sh` | 13f4c158 / 75b18150 ; `test_pre_vol.sh`, 7 cas, 6 contrôles cassés exprès |
| Le portail refuse un bloquant ou un grave corrigé sans rejeu ni « rejeu impossible : … » (`from_paquet 4`, sortie 8) | `rapport-zone.py --corrections`, `fusion-equipes.sh`, `pre-vol.sh` | b500b6d7 / 8e08f8cc ; `test_pre_vol.sh`, 21 cas, 14 contrôles cassés exprès |
| `portail-file.sh`, la file du chef | script neuf | 3eab99a1 / 17ee95a7, puis 6af1fcfb / 85ffe7a7 ; `test_ultra_bench.py` `portal_queue`, 10 contrôles cassés exprès |
| `paquet-verifie.sh` (CI, juge, captures, verdict dans EN-COURS, `--ouvre`) | script neuf, registre `portail-paquets.tsv` | af275665 / bb6dd5bc ; `test_portail3.py` classe `Batch`, 32 tests |
| `fiche.sh` (une fiche du gabarit avec ses variables) | script neuf | 30716cee / 8f3ae85a ; `test_portail3.py` classe `Cards`, 18 tests |
| « code testé » et « joué par » écrits par le banc ; `etat/` hors de git | `sortie-de-siege.py`, `fabrique-sieges.py`, `human-act.py`, `.gitignore` | ba77ddf3 / acd00ce6 ; `test_portail3.py` classes `SeatReport` (15) et `SeatStateOutOfGit` (2) |
| Le juge revient vers son plancher | `verifie-chantier.py` (`named_pair`), `juge-paires-nommees.tsv` (31 paires) | d2ca9e3b / 6f245a20 ; `test_ultra_bench.py` `named_pairs` |

## Le format neuf du rapport de zone (refusé sans lui à partir du paquet 4)
```
corrige: <id du défaut> | gravité: bloquant|grave|moyen|mineur | rejeu: <id du rejeu>
corrige: <id du défaut> | gravité: bloquant|grave | rejeu impossible : <raison>
ne corrige pas: <id> : <raison>
```
Ce lot ne corrige aucun défaut de l'appli : il n'en porte aucune.

## Le juge, avant et après
- Avant (base `cc6175a4`) : 68 écarts, dont 66 paires dépareillées.
- Après : 15 écarts nommés un par un dans les décisions (les 8 du plancher d'après « juge7 », 7 de la nuit qui ne s'apparient pas honnêtement). 52 écarts de paires fermés par 31 lignes nommées. Rien n'a été rejoué : le juge vérifie l'ordre, la classe de test et que le vert change du code ; c'est plus faible qu'un rejeu, et c'est dit.
- Le chiffre exact du portail se lit dans la trace de la fusion (l'autotest y est scellé pour le code fusionné).

## Relecture neuve (première passe sur a4ff1473 : 2 graves, corrigés)
- G1, le fichier des paires nommées n'était pas gelé : le juge porte maintenant son empreinte (`NAMED_SHA256`) ; changé, le fichier n'apparie plus rien et compte pour un écart. Test : `test_portail3.py` `ReviewOfTheLot#test_the_named_pairs_are_frozen`.
- G2, la gravité se lisait dans la branche seule : elle se lit aussi à la base et à la tête de l'intégration (la plus forte gagne), « corrigé » se lit avec des mots derrière, une ligne grave retirée demande sa ligne `corrige:`. Tests : `ReviewOfTheLot`, cinq cas.
- M1 (un bloc de code qui montre le format était lu comme des lignes) et M2 (la file déplaçait la branche locale sans le dire) : corrigés avec leur test.
- Restent ouverts, à recopier dans `docs/REVIEW.md` (responsable : banc, lot « rejeux7 ») : M3 `code-teste.tsv` récrit en entier à chaque `avant` (un lot relancé pour un siège perd le commit noté des autres) ; M4 onze contrôles neufs sans test qui rougit (liste dans la relecture) ; M5 un siège qui n'a rien écrit passe `verifie` ; M6 le refus d'un rapport de siège n'arrête rien (ni `human-tester.sh` ni le pré-vol ne le lisent) ; M7 au premier `--ouvre` sans ligne au registre, le juge n'est comparé à rien.
- Rouge puis vert de ces corrections : voir `git log` (« review of portail3 »).

## Ce qui n'est pas fait, ou pas vu
- `paquet-verifie.sh` n'a jamais lu le vrai GitHub (outil `gh` non connecté sur cette machine) : prouvé sur une doublure seulement. Son premier vrai passage est celui du chef.
- Un cas de test de la règle « correction sans rejeu » (le tri lu à la tête de l'intégration) est entré dans le commit « vert: » 8e08f8cc, vu rouge à la main mais sans commit rouge à lui.
- Quatre dossiers `etat/` restent suivis par git (`sieges/fumee-finale/lotA/{a,b,c,d}/etat/`) : non supprimés, hors de la fiche.
- Les règles ne voient pas : un lot qui ferme un écart du juge et en ouvre un autre (le compte ne bouge pas) ; un grave qu'aucune table ne connaît ; si le rejeu nommé est bien celui du défaut.
- `selftest.sh` ne joue `test_portail3.py` que s'il existe (les dépôts jouets copient `selftest.sh` sans lui).

## Demandes (fichiers hors de la fiche)
- Demande : `docs/AIDE-FUSIONS-TESTS.md` et `docs/audits/ultra/GABARIT-FICHES-V3.md` : ajouter les lignes `corrige:` / `ne corrige pas:` à la fiche des équipes (point 11) et du correcteur.
- Demande : `docs/audits/ultra/EN-COURS.md` : écrire « VAGUE » en premier mot de « Temps en cours » au départ d'une vague (la file s'y arrête) ; y garder les deux marques `<!-- paquet-verifie:debut -->` / `<!-- paquet-verifie:fin -->`.
- Demande : `app/src/test/java/com/pocketstudio/app/human/SeatOptions.kt` (lot « rejeux7 ») : accepter la clé `joue.par`.

## Chemins
- chemin: banc › autotest du testeur humain | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace
