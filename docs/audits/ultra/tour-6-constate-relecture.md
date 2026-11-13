# Tour 6, lot du banc « constate » : relecture neuve (trois passes)
Relu : 42b2ea99b3eda7ef8f6e52fa65aa7b1c13b49bf6

Relecteur neuf : je n'ai écrit aucune ligne de ce lot. Première passe sur `6e144989` (1 grave), seconde sur `a8d03728` (les corrections de la première : paires `18cffddd`/`d346c9ed` et `16925f45`/`936d4963`), troisième, courte, sur `42b2ea99` (fusion de la tête d'intégration `8afb9abc` par `9b871be5`, puis règles de ce lot et de portail4 regardées au paquet 6). Lu : les commits propres au lot (`git diff aea5b506..6e144989`, puis `git diff 6e144989..a8d03728`), `PLAN-REPARATIONS-CONSTATEES.md` en entier, les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`, `relecture-verdict.py`, la fiche 6 du gabarit, `AUDIT-ULTRA.md` §13, le rapport et les décisions du lot, la relecture de portail4 comme modèle. Gradle n'a pas été lancé (consigne) ; l'atelier principal n'a pas été touché. Essais sur dépôts jouets, arbres jetables (`git worktree`, `git archive`) et copies temporaires.

## Ce qui a été refait, première passe (`6e144989`)

- Tests sur la tête : `test_pre_vol.sh` « tout est vert » (94) ; `test_socle.py` 29 OK ; `test_portail3.py` 77 OK ; `test_portail4.py` 59 OK ; `test_constate.py` 48 OK ; `test_ultra_bench.py` « ultra bench scripts: OK ».
- Rouges rejoués sur leur commit : `c7baadfe` 9 échecs, `2385e569` 24 échecs et 7 ROUGE de `test_pre_vol.sh`, `4475642e` 11, `037d4f73` 1 ; tous des `FAIL` sur assertion. Verts qui changent aussi leur test (`7adffbb5`, `2da01534`, `6ede7980`, `42700d30`) : tous déclarés, honnêtes.
- `--lot` en lecture seule sur 12 vraies fusions du tour 6 et sur le lot lui-même : sortie 0. Orthographes et formes de fichier essayées sur dépôt jouet : voir les constats.
- `--constate` sur les vrais rapports `controle/Ba`, `Ba-3`, `Ha-4` ; `fuse-reports.py`, `tableau-de-bord.py`, `applique-corrections.py --dry` avant/après sur des copies des tours 5 et 6 : sièges ordinaires inchangés, changements tous voulus. `human-act.py` et le verrou du tuto : pas plus larges qu'avant (le M2 de portail4 reste tel quel). `fiche.sh --variables` 1 à 6 : sortie 0.
- 22 contrôles cassés : 20 rouges, 2 verts (M6, m3).

## Ce qui a été refait, seconde passe (`a8d03728`)

- Tests sur la tête, arbre au repos : `test_pre_vol.sh` « tout est vert » (94) ; `test_socle.py` 29 OK ; `test_portail3.py` 77 OK ; `test_portail4.py` 59 OK ; `test_constate.py` 64 OK ; `test_ultra_bench.py` « ultra bench scripts: OK ».
- Rouges neufs rejoués sur leur commit : `18cffddd` 2 échecs, `16925f45` 26 échecs, tous `FAIL`, aucune `ERROR` ; verts : `d346c9ed` 51 OK, `936d4963` 64 OK.
- Tests changés dans le second vert (`936d4963`) : la base passée à `--lot` devient la vraie base de fusion (celle que le portail passe), deux tests d'abaissement, une ligne « corrigé : non », U+2028 écrit en échappement. Ce sont des ajouts ou une mise au réel ; aucun rouge n'est fait taire : honnête, et déclaré.
- G1 rejoué dans les deux sens : la ligne forgée d'une branche à 9 colonnes est refusée (« 12 cells, its branch's header has 9 … merge the integration ») ; la même branche, sans toucher au registre, passe ; après avoir fusionné la tête (12 colonnes, ligne du trieur), son passage « pas pris » → « tenté » passe.
- Mes scénarios de la première passe, rejoués. Refusés : « constaté » ou « rejoué » rabaissés en « tenté », sa ligne « tenté » remise en « pas pris », ligne « constaté » retirée ou mise en commentaire, ligne neuve (en « tenté » ou en « pas pris »), U+2028 caché, CRLF, phrase du trieur effacée ou récrite, case `constat` remplie. `applique-corrections.py` lit par `reparations.load` ; sa sortie sur le vrai dépôt est la même avec les scripts de `2e29fdd7` (347 citations, 315 clés, 0 sans clé). M3 : `Classe#méthode` d'un vrai @Test (`TutoGrilleTest#proBassNotesAreAboveThePanelOrPointedAt`) est tenu sous `com.pocketstudio.app.tuto.TutoGrilleTest`, `*TutoGrilleTest`, `TutoGrilleTest`, `pkg.Classe.méthode` ; pas sous `*Grille*`, `*Tuto*`, une autre classe, ni pour une méthode qui n'existe pas. M5 : dossier hors du dépôt, `controle/../lotB/a`, contrôle d'un autre tour : refusés ; le vrai `Ba-3` (code testé `10830ce2`) constate A2 quand le commit de la ligne en est un ancêtre, refuse sinon, refuse un commit inconnu et le « autre » de A9.
- Esquives neuves. Refusées : BOM en tête (« changes the register's header »), tabulation en fin de ligne (13 cases), clé changée (écran récrit : ajout + retrait), seconde ligne de la clé d'un « constaté » en « tenté » (« lowers »). Acceptées sans dommage : lignes réordonnées, doublon d'une ligne « pas pris » à côté de sa copie « tenté ». Acceptées et notées : registre supprimé (M1), espace insécable ou espace final dans une case de clé (m5), ligne neuve d'un trieur (M7).
- Lots honnêtes : ce lot passe sa propre règle (`base:2e29fdd7 tete:2e29fdd7`) ; 12 vraies fusions du tour 6, sortie 0 ; une branche à l'ancien en-tête qui ne touche pas au registre, sortie 0 ; un lot de correction qui ne fait que passer sa ligne de « pas pris » à « tenté », sortie 0, aussi après avoir fusionné la tête où le trieur l'a écrite. Une branche à l'ancien en-tête qui écrit une ligne à 12 cases sans fusionner la tête est refusée, avec le remède dans le message : acceptable.
- 15 contrôles neufs cassés un par un, puis remis (arbre jetable) : tous rougissent `test_constate.py`. Ce sont : état « pas pris » permis, refus d'ajout, abaissement, retrait, U+2028 de `FORBIDDEN`, `covers()`, `is_app_test`, dossier de contrôle, relation d'ancêtre, longueur de G1, `DENIED`, lecture en octets de `show()`, cases du lot, « tenté » depuis un autre état, lecteur unique d'`applique-corrections.py`.

## Ce qui a été refait, troisième passe (`42b2ea99`)

- La fusion `9b871be5` (parents `078b4e56`, `8afb9abc`) n'apporte que l'intégration : `git diff 8afb9abc 42b2ea99 --stat` ne nomme que les fichiers de ce lot et de portail4 (23 fichiers) ; entre `a8d03728` et `9b871be5`, ces fichiers ne changent pas, sauf l'ajout de cette relecture : rien du lot n'est perdu.
- La re-garde `42b2ea99` ne fait que passer de 5 à 6 (relu en `--word-diff`) : les lignes de sortie 10 et 11 et le bloc « rejoué » de `fusion-equipes.sh` passent à `from_paquet 6`, les deux `rule_from` du pré-vol à 6, `KNOWN_RULES="18 2c75cdfbfb"`, les épingles des tests (`test_constate.py`, `test_portail4.py`, `test_pre_vol.sh`) et les documents. S'y ajoutent deux lignes « Re-garde » et deux décisions, plus un « paquet 4 → 5 » dans le texte du rapport. `portail-paquet.txt` dit `paquet: 5` : les règles dorment. `test_pre_vol.sh` le montre : « before batch 6 … is only said » et « the portal merges … » pour le tuto et pour le registre.
- Tests sur la tête, arbre au repos : `test_pre_vol.sh` « tout est vert » (94) ; `test_socle.py` 29 OK ; `test_portail3.py` 77 OK ; `test_portail4.py` 59 OK ; `test_constate.py` 64 OK ; `test_ultra_bench.py` « ultra bench scripts: OK ».
- Hors de cette relecture : le pré-vol refuse encore « decisions written outside docs/decisions/tour-6-constate.md: docs/decisions/tour-6-portail4.md » tant que portail4 n'est pas passé seul au portail.

## Constats

| id | gravité | constat | fichier:ligne | état |
|---|---|---|---|---|
| G1 | grave | Une branche partie avant les trois colonnes était lue avec son en-tête à 9 colonnes : la phrase, le siège d'origine et le `constat` d'une ligne à 12 cases étaient invisibles pour `--lot`, et la fusion propre les gardait (essayé sur `6e144989` : sortie 0). | `scripts/bench/reparations.py:356` | **corrigé** en `d346c9ed` : lignes lues avec `COLUMNS`, ligne plus longue que l'en-tête de sa branche refusée ; revérifié dans les deux sens (forgée : refusée ; branche intacte, ou qui a fusionné la tête : acceptée) ; longueur cassée : rouge |
| M1 | moyen | Seuls les ajouts étaient contrôlés. Rabaisser, retirer ou remettre en « pas pris » une ligne : maintenant refusé (`936d4963`, revérifié). Reste ouvert : supprimer ou renommer tout le registre passe encore, `brought()` rend `None` et `lot_refusals()` rend une liste vide (essayé, sortie 0). La fusion efface alors tous les « constaté » et « rejoué » ; le tableau de bord affiche 0. | `scripts/bench/reparations.py:330`, `:367` | ouvert (en partie) |
| M2 | moyen | Deux lecteurs coupaient les lignes différemment (U+2028 cachait un « constaté » à `--lot`, pas à `applique-corrections.py`). | `scripts/bench/reparations.py:89` | corrigé en `936d4963` : un seul lecteur, séparateurs et caractères de contrôle refusés, `git show` lu en octets ; revérifié |
| M3 | moyen | « rejoué » par un test Gradle ne valait jamais avec les filtres réels. | `scripts/bench/reparations.py:430` | corrigé en `936d4963` : `covers()` et un vrai @Test au commit fusionné ; revérifié sur `TutoGrilleTest` |
| M4 | moyen | Le lot pouvait écrire « pas pris ». | `scripts/bench/reparations.py:85` | corrigé en `936d4963` : `LOT_STATES = ("tenté",)` ; revérifié |
| M5 | moyen | `--constate` acceptait n'importe quel dossier. | `scripts/bench/reparations.py:245` | corrigé en `936d4963` : dossier sous `sieges/<tour>/controle/`, « code testé » qui contient le commit de la ligne ; revérifié sur `Ba-3` |
| M6 | moyen | Le refus « phrase du trieur effacée » n'avait aucun test qui rougit. | `scripts/bench/test_constate.py` | corrigé en `16925f45` : test ajouté ; revérifié |
| M7 | moyen | Toute ligne neuve est refusée, aussi celle du trieur. Exemple essayé : « tour-6 A9 B/a#40 pas pris » avec sa phrase, refusé (« says « pas pris » … the sorter's »). Les deux écritures historiques du registre (`2731431e`, `6c2ea01c` : 345 lignes ajoutées, `cible` remplie) le seraient aussi. Or §13 interdit au chef de fusionner hors du portail. Le seul chemin restant, que le chef recopie le fichier du trieur sans fusion, comme pour le texte des sièges, n'est écrit nulle part : ni dans la fiche 4b, ni dans le message de refus, ni dans les décisions. Une branche de tri passée au portail (comme « verdicts » au tour 5) est refusée dès le paquet 6 (re-garde). | `scripts/bench/reparations.py:393` ; `docs/audits/ultra/GABARIT-FICHES-V3.md` fiche 4b | ouvert |
| m1 | mineur | « corrigé ? non… » lu comme « corrigé ». | `scripts/bench/reparations.py:164` | corrigé en `936d4963` (`DENIED`), revérifié |
| m2 | mineur | La condition d'entrée de la règle 3 n'est pas contrôlée. | rapport | ouvert, maintenant dit par le lot |
| m3 | mineur | `\r` gardé par `rewrite()` sans test. | `scripts/bench/reparations.py:189` | corrigé en `936d4963` : `\r` refusé par le lecteur, plus rien à garder |
| m4 | mineur | La courbe compte les sièges de contrôle, E non. | `scripts/bench/tableau-de-bord.py:217` | ouvert, dit et justifié par le lot |
| m5 | mineur | Une case de clé avec une espace insécable ou finale (« A2 ») passe `--lot`, car `key()` enlève les espaces. Mais `--constate` et le décompte lisent la case brute : la ligne ne se constate plus sous « A2 » et compte comme un autre défaut. Il faut le vouloir. | `scripts/bench/reparations.py:322` | ouvert |

## Ce qui tient

- Une ligne neuve, rabaissée, retirée ou dont une case autre que `etat`, `commit`, `test`, `note` change est refusée, quelle que soit l'écriture (casse, accents, espaces, CRLF, séparateurs cachés, BOM, tabulations en trop, en-tête permuté). Seule exception : le registre supprimé entier (M1).
- Un seul lecteur du registre ; « rejoué » posé par le portail seul, après la fusion verte, à partir du paquet 6, sur les lignes du lot, avec une preuve qui a vraiment tourné ; défait avec la fusion. « constaté » seulement d'après un siège de contrôle du même tour qui a joué la réparation.
- Les lignes des tours 1 à 4 sont intactes ; sièges ordinaires, `applique-corrections.py` et grappes inchangés ; aucun lot honnête de correction n'est refusé à tort.
- Chaque contrôle neuf a un test qui rougit quand on le casse (15 sur 15 dans cette passe).

## Les moyens ouverts, une ligne chacun (pour `docs/REVIEW.md`)

- M1 : refuser aussi la branche qui supprime ou renomme `corrections.tsv` quand la base ou la tête l'ont (`brought()` rend `None`).
- M7 : écrire le chemin des lignes du trieur (le chef recopie le fichier de la branche de tri sans fusion, ou le portail accepte des lignes neuves « pas pris » d'une branche de tri) et le dire dans la fiche 4b et dans le message de refus.

Verdict : 0 bloquant, 0 grave
