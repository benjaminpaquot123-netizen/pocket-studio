# Tour 6, lot du banc « portail4 » : relecture neuve (trois passes)
Relu : 5dacfa15c8978b1d99c779bada359dbf1cff8246

Relecteur neuf : je n'ai écrit aucune ligne de ce lot. Première passe sur `e8503cbe` (2 graves), seconde sur `8f80df83` (les corrections de la première), troisième, courte, sur `5dacfa15` (la fusion de l'intégration `8afb9abc` dans la branche, puis la règle reportée au paquet 6). Lu : `git diff ce0aed6f..e8503cbe` puis `git diff e8503cbe..8f80df83`, `AUDIT-ULTRA.md` §13.3 et §13.5, les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`, `relecture-verdict.py`, la fiche 6 du gabarit, le rapport et les décisions du lot, la relecture de portail3 comme modèle. Gradle n'a pas été lancé (consigne) ; l'atelier principal n'a pas été touché. Essais sur dépôts jouets et arbres jetables (`git archive`) seulement.

## Ce qui a été refait, première passe (`e8503cbe`)

- Tests sur la tête : `test_pre_vol.sh` « tout est vert » ; `test_socle.py` 29 OK ; `test_portail3.py` 77 OK ; `test_portail4.py` 37 OK (vraies fusions lues) ; `test_ultra_bench.py` « ultra bench scripts: OK ».
- Rouges rejoués sur le parent de leur vert : `4fa61525` rouge, plus de 20 échecs, chacun sur une assertion ; `6eebc47f` vert (36). `f95f0a85` rouge sur une assertion (`test_a_change_the_integration_already_holds_is_not_the_lot_s`) ; `594004f1` vert (37).
- La règle relancée sur les 13 fusions du tour 6 : `tutogrille` ne comptait que `PianoRoll.kt`, pas `RhythmScreen.kt` changé pour le tuto (`036c2c85`) ; `tutoconsignes` changeait `PlainWords.kt` et `TrackSheet.kt` (`b621715d`, `d1fabadd`) hors de la règle (G1).
- Sommeil : `portail-paquet.txt` dit `paquet: 3`, le portail n'appelle `--tuto` que sous `from_paquet 5` ; le pré-vol ne dit que « À VOIR ». Le refus tombe avant `git merge`, la règle ne fait que lire : l'atelier reste immobile.
- Attaques acceptées alors : correction du tuto dans `RhythmScreen.kt` après le siège ; lot du tuto qui ne change que `RhythmScreen.kt` (G1) ; nouveau `i18n/fr/tuto_café.json` sans siège (G2) ; rapport du siège récrit dans la branche (M2) ; format réel « - chapitre 5 « … » : bloqué » (M3) ; seconde ligne `1. controle: … | bloqués: 2 | …` (m1).
- 13 contrôles cassés : 10 rougissaient, 3 non (M4).

## Ce qui a été refait, seconde passe (`8f80df83`)

- Tests sur la tête : `test_portail4.py` 59 OK ; `test_portail3.py` 77 OK ; `test_socle.py` 29 OK ; `bash test_pre_vol.sh` « tout est vert » ; `test_ultra_bench.py` « ultra bench scripts: OK ».
- Le rouge neuf `d4c7b116` rejoué : 16 échecs, tous `FAIL` sur une assertion, aucune `ERROR` (classe `ReviewOfTheLot`) ; son vert `69bd040e` : 59 OK. Les six tests du rouge déjà verts sont ceux que les décisions nomment (pas de faux refus, nom avec une espace, les trois contrôles de M4 qui existaient déjà) ; je les ai prouvés autrement, en cassant le contrôle (ci-dessous).
- Mes scénarios de la première passe, rejoués : G1 (correction dans `RhythmScreen.kt` après le siège) refusé, « was played before the lot's last change of the tutorial » ; lot qui ne change que `RhythmScreen.kt` et dont le rapport dit « tuto » : refusé ; G2 (`tuto_café.json`) refusé ; M3 (format de `controle/Da`) refusé ; m1 (seconde ligne numérotée `bloqués: 2`) refusé ; M2 (rapport du siège récrit) toujours accepté, dit ouvert.
- Esquives neuves essayées contre les corrections. Refusées, bien : `RhythmScreen.kt` changé à l'intérieur d'une fusion de l'intégration après le siège, le rapport muet sur le tuto ; fichiers de `tutos/` aux noms avec tabulation, guillemet, étoile ; chapitre renommé hors de `tutos/` ; rapport qui dit « TUTO » ou « tutoriel ». Acceptées : voir m1, m4, m5.
- Lots honnêtes : ce lot passe sa propre règle (`--tuto … base:ce0aed6f tete:ce0aed6f tour:tour-6`), le mode simple et `--corrections` (sortie 0, rien d'écrit) ; lot du banc, textes hors `tuto*.json`, fusion de l'intégration seule, siège qui a joué la fusion de l'intégration : acceptés sans un mot. Sur l'arbre réel, `guidecoquille`, `cablage`, `morceauexport`, `restes`, `remixdecoupe`, `portail3`, `banc7` : rien. Faux refus neufs : ceux de M1 (dits par le lot) et un titre « ## Contrôle : … » (m2).
- Test existant changé dans le vert : `emulateur` passe des lots laissés tranquilles aux lots à qui l'on demande un siège. Justifié : sa ligne de `PlainWords.kt` (`4bd2329e`, `4e31f98c`) répare un plantage que son propre rapport situe dans « le panneau du tuto » ; c'est l'effet direct de G1 sur l'arbre réel, pas un rouge qu'on fait taire. `reparations` entre dans la liste refusée, `guidecoquille` et `cablage` dans la liste laissée tranquille : contrôlé sur l'arbre réel, même résultat.
- 11 contrôles cassés exprès, un par un, puis remis (copie jetable) : tous font rougir `test_portail4.py` : fichiers partagés comptés dès que le lot touche la zone (4 rouges), les trois noms neufs de `ZONE_IF_SAID` (5), `-z` de la zone (5), `-z` des fichiers changés par une fusion (1), blocs `~~~` (2), format de chapitre de `Da` (1), formes de la ligne `controle:` (2), `--no-renames` (1), « .. » dans l'id (1), sortie 2 sans options (1), `-z` du mode simple (1).
- Sommeil et sortie avant la fusion : inchangés par la seconde passe (`fusion-equipes.sh` et `pre-vol.sh` non touchés depuis `e8503cbe`).

## Ce qui a été refait, troisième passe (`5dacfa15`)

- La fusion `c85d4c0e` (parents `fd1c2637`, ma relecture de la seconde passe, et `8afb9abc`, tête poussée de l'intégration) n'apporte que l'intégration : le diff de `8afb9abc` à `5dacfa15` ne montre que les neuf fichiers du lot ; le diff de `fd1c2637` à `c85d4c0e` sur les six scripts du lot est vide : rien du lot n'est perdu.
- Le commit `5dacfa15` ne fait que passer 5 → 6 : la ligne du portail (`from_paquet 6`), `rule_from 6` du pré-vol, `KNOWN_RULES="17 b3bb3a0b98"` (recalculé par moi sur le portail : 17 lignes, même empreinte), deux épingles de `test_portail4.py`, le cas « avant le paquet 6 » de `test_pre_vol.sh`, une ligne de docstring, le rapport et les décisions (une ligne neuve qui dit pourquoi). Aucun contrôle de la règle ne change.
- Le dépôt est au paquet 5 (`portail-paquet.txt`) : la règle dort aujourd'hui. Le cas de `test_pre_vol.sh` qui le prouve tourne au paquet 4 ; je l'ai rejoué au paquet 5 sur une copie jetable : « before batch 6 … only said » et « before batch 6 the portal merges … » restent verts. Au paquet 6, les cas du dépôt jouet refusent (sortie 10, atelier immobile) : verts.
- Tests sur la tête : `test_pre_vol.sh` « tout est vert » ; `test_socle.py` 29 OK ; `test_portail3.py` 77 OK ; `test_portail4.py` 59 OK ; `test_ultra_bench.py` « ultra bench scripts: OK ».

## Constats

| id | gravité | constat | fichier:ligne | état |
|---|---|---|---|---|
| G1 | grave | La zone oubliait `RhythmScreen.kt`, `PlainWords.kt`, `TrackSheet.kt` (§13.3) : une correction du tuto dans `RhythmScreen.kt` après le siège passait, comme la retouche de la nuit du 12. | `scripts/bench/rapport-zone.py:300`, `:325` | **corrigé** en `69bd040e` : les quatre fichiers partagés dans `ZONE_IF_SAID`, et comptés quoi que dise le rapport dès que le lot touche la zone ; revérifié (scénario refusé, aussi par une fusion après le siège ; contrôles cassés : rouge) |
| G2 | grave | Un nom de fichier non ASCII (`tuto_café.json`), cité entre guillemets par git, échappait à la règle. | `scripts/bench/rapport-zone.py:319` | **corrigé** en `69bd040e` : noms lus avec `-z` aux trois endroits ; revérifié (accent, tabulation, guillemet, étoile : refusés ; `-z` cassé : rouge) |
| M1 | moyen | Un lot honnête qui change un des quatre fichiers partagés et dont le rapport écrit « tuto » en passant se voit demander un siège (vraie fusion `langues` du tour 5 ; `emulateur` est discutable, il réparait le panneau du tuto). | `scripts/bench/rapport-zone.py:301` | ouvert, dit par le lot |
| M2 | moyen | Le rapport du siège s'écrit ou se récrit à la main dans la branche (« bloqué » devenu « hésitation » : accepté) ; `code-teste.tsv` n'est pas lu. | `scripts/bench/rapport-zone.py:405` | ouvert, dit par le lot |
| M3 | moyen | Le recoupement « bloqué » ne lisait pas le format réel de `controle/Da`. | `scripts/bench/rapport-zone.py:311` | corrigé en `69bd040e`, revérifié |
| M4 | moyen | Trois contrôles sans test qui rougit (`--no-renames`, « .. », sortie 2) et une phrase du rapport plus forte que la preuve. | `scripts/bench/rapport-zone.py` ; rapport | corrigé en `69bd040e` : trois tests, chacun vu rouge en cassant son contrôle ; phrase reprise |
| m1 | mineur | Une ligne `controle:` dans la deuxième case d'un tableau (« \| Da \| controle: … \| bloqués: 2 … ») n'est pas lue : à côté d'une bonne ligne, elle passe (essayé). La ligne est déclarée par le lot. Première passe : numéro, citation, gras, « ô » décomposé, réglés en `69bd040e`. | `scripts/bench/rapport-zone.py:303` | ouvert |
| m2 | mineur | Un titre « ## Contrôle : … » dans le rapport d'un lot du tuto est lu comme une ligne mal formée et refuse le lot (essayé) ; le refus nomme la ligne, la parade est de la reformuler. | `scripts/bench/rapport-zone.py:303` | ouvert |
| m3 | mineur | Blocs `~~~` non sautés. | `scripts/bench/rapport-zone.py` | corrigé en `69bd040e`, revérifié |
| m4 | mineur | Un nom de fichier qui contient un saut de ligne échappe encore à `ZONE` (`$` et `.` sans `re.S`) : essayé, accepté. Aucun lot honnête n'écrit un tel nom, et une fraude volontaire a déjà plus simple (M2). | `scripts/bench/rapport-zone.py:298` | ouvert |
| m5 | mineur | « bloqué » après un tiret (« chapitre 5 « … » — bloqué ») n'est pas recoupé ; un rapport qui dit « didacticiel » sans « tuto » ne fait pas compter les fichiers partagés seuls (limite dite). | `scripts/bench/rapport-zone.py:311`, `:301` | ouvert |
| m6 | mineur | Limites dites et acceptables : graves neufs non recoupés, débutant neuf, tuto entier, même langue et même profil, défauts visés dits corrigés (§13.5) ; la relecture neuve et le chef les lisent. | rapport | ouvert, dit par le lot |

## Ce qui tient

- Le portail : appel unique, sous `from_paquet 6` depuis `5dacfa15` (`from_paquet 5` lors des deux premières passes), sortie 10, avant la fusion, lecture seule ; `KNOWN_RULES` du pré-vol à jour (17 règles) ; aujourd'hui (paquet 5) rien ne change pour aucun lot.
- La date du siège : chaque commit propre au lot qui change la zone ou un fichier partagé est ancêtre de `code testé`, et ces fichiers sont identiques au commit joué et à la tête : une fusion qui les change après le siège est vue.
- Chaque contrôle neuf a un test qui rougit quand on le casse (11 sur 11 dans cette passe).

## Les moyens ouverts, une ligne chacun (pour `docs/REVIEW.md`)

- M1 : un lot hors tuto qui change `PianoRoll.kt`, `RhythmScreen.kt`, `PlainWords.kt` ou `TrackSheet.kt` et écrit « tuto » en passant se voit demander un siège ; lire plutôt une ligne `tuto: oui|non` du rapport, ou nommer le mot fautif dans le refus.
- M2 : lire `code-teste.tsv` du banc (ou l'histoire du dossier du siège) pour qu'un rapport de siège ne s'écrive pas à la main.

Verdict : 0 bloquant, 0 grave
