# Tour 6, lot du banc « paquet7 » : la règle du tuto vaut au paquet 7

Branche : `eq/tour-6-paquet7` (base `339d6eaa`, tête poussée de l'intégration). Décisions : `docs/decisions/tour-6-paquet7.md`. Aucun fichier de `app/` touché, `scripts/bench/portail-paquet.txt` non plus.

tests: aucun : lot du banc seul (scripts du portail), aucun test Gradle changé ; banc : `python3 -I scripts/bench/test_socle.py`, `python3 -I scripts/bench/test_portail4.py`, `bash scripts/bench/test_pre_vol.sh`

## Ce qui était rouge

Contrôles rapides de la CI 43521351450 (sur 52d8ad87f) : `test_socle.py`, `PortalRulesFrozenPerBatch.test_a_rule_added_during_the_batch_waits_for_the_next` : « rule added during batch 6 without from_paquet 7 ». Le paquet 6 a été ouvert avant l'entrée de portail4, dont la règle disait `from_paquet 6`.

## Correction

- `fusion-equipes.sh` : `from_paquet 7` sur la règle du tuto (sortie 10).
- `pre-vol.sh` : `rule_from 7` sur la même règle, `KNOWN_RULES="17 c445a874a1"` (le texte de la ligne du portail a changé, donc son empreinte).
- Tests : `test_portail4.py` (deux lectures), `test_pre_vol.sh` (au paquet 6 la règle attend, au paquet 7 elle refuse).

Paires : « rouge: the tutorial rule waits for batch 7 » (tests seuls, vus rouges : 2 échecs de test_portail4, 2 ROUGE de test_pre_vol) puis « vert: the tutorial rule waits for batch 7 ».

## Après l'entrée

Le chef passe `portail-paquet.txt` à 7 : la règle vaut alors, et `test_socle` compare au portail du commit d'ouverture du paquet 7, qui la tient déjà.

## Chemins
- chemin: banc › autotest du testeur humain | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace
