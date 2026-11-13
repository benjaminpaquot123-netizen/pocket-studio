# Tour 6, lot du banc « paquet7 » : relecture neuve (deux passes)
Relu : e18d6932e3720581a480e0d16832bfad0cbb16eb

## Première passe (Relu 9b844e266, tête cfd56635b)

Lu le diff depuis la base 339d6eaaa (rouge « the tutorial rule waits for batch 7 », vert, rapport et décisions), `PortalRulesFrozenPerBatch` de `test_socle.py`, `rule_from` et `KNOWN_RULES` de `pre-vol.sh`. Tests du banc verts (29, 59, test_pre_vol) ; empreinte « 17 c445a874a1 » égale à `KNOWN_RULES` ; dans un arbre jetable, le commit rouge échoue contre l'ancien code, la base a `test_socle` rouge, la tête plus « paquet: 7 » reste verte. Constats : M1 (commentaires « from batch 6 » restants), M2 (trois commits au lieu des quatre annoncés, rien ne manque). Verdict : 0 bloquant, 0 grave.

## Deuxième passe (tête e18d6932e)

Lu : `git diff 9b844e266..HEAD`, un commit, cinq fichiers. Dans `fusion-equipes.sh`, `pre-vol.sh`, `test_portail4.py` et `rapport-zone.py`, seuls des commentaires ou docstrings passent de « from batch 6 » à « from batch 7 » ; aucune ligne de code. Aucune ligne « not merged » / « undone » du portail n'est touchée : empreinte recalculée comme dans `pre-vol.sh` (`grep -E 'not merged|undone' scripts/bench/fusion-equipes.sh | sed 's/^ *//'`) : « 17 c445a874a1 », toujours égale à `KNOWN_RULES`. Plus aucun « batch 6 » dans ces quatre fichiers. Le rapport de zone gagne une section « ## Chemins » avec « chemin: banc › autotest du testeur humain | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace » ; le test existe dans `app/src/test/.../human/BenchSelfTest.kt`.

Lancé sur la tête : `python3 -I scripts/bench/test_socle.py` (29 OK), `test_portail4.py` (59 OK), `bash scripts/bench/test_pre_vol.sh` (tout vert), `python3 -I scripts/bench/rapport-zone.py eq/tour-6-paquet7 docs/audits/ultra/tour-6-paquet7-rapport.md` (accepté, sortie 0, aucun filtre puisque « tests: aucun : … »).

- M1 : corrigé dans e18d6932e (les quatre commentaires disent « batch 7 »). `rapport-zone.py` était hors de la liste du lot ; changement de commentaire seulement, sans effet, à noter par le chef.
- M2 : inchangé, sans conséquence (paire rouge/vert, rapport, décisions présents).
- M3 : le chemin du rapport cite un test d'appli que le lot ne touche pas (`BenchSelfTest#reachTagTouchesAVisibleElementInPlace`) : il satisfait le format exigé par `rapport-zone.py` pour un lot du banc seul, sans prouver quoi que ce soit de ce lot ; acceptable, le lot ne change aucun écran.

Verdict : 0 bloquant, 0 grave
