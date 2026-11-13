# Tour 6, lot du banc « portail4 » : pas de lot du tuto sans son siège de contrôle

Branche : `eq/tour-6-portail4` (base `ce0aed6f`, tête poussée de l'intégration). Décisions : `docs/decisions/tour-6-portail4.md`. Aucun fichier de `app/` touché, `scripts/bench/portail-paquet.txt` non plus.

tests: aucun : lot du banc seul (scripts du portail), aucun test Gradle changé ; banc : `python3 -I scripts/bench/test_portail4.py`, `bash scripts/bench/test_pre_vol.sh`, `python3 -I scripts/bench/test_socle.py`, `python3 -I scripts/bench/test_portail3.py`, `python3 -I scripts/bench/test_ultra_bench.py`

## La règle (à partir du paquet 5)

Le portail refuse, avant la fusion et avec la sortie 10, un lot qui change la zone du tuto si son rapport de zone ne porte pas, pour chaque siège de contrôle, cette ligne :

```
controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0
```

Le refus du portail : `<zone>: tutorial lot without its control seat: <raisons>: not merged.` Le pré-vol dit la même chose : « lot du tuto sans son siège de contrôle : … », RECALÉ dès le paquet 5, « À VOIR … refusé à partir du paquet 5 » avant.

- **Zone** : un fichier que le lot change (diff depuis sa base, et différent de la tête de l'intégration) sous `src/main`, dans un dossier `tuto/` ou `tutos/`, ou nommé `i18n/<langue>/tuto*.json` ; et `PianoRoll.kt` quand le rapport dit le mot (hors blocs de code). Les tests, les captures de référence et les docs n'en font pas partie.
- **Lignes** : au moins une ; chacune bien formée, avec 0 et 0. Une seule mauvaise refuse le lot.
- **`<sha>`** : un commit de la branche, joué après le dernier changement du tuto fait par le lot, et dont les fichiers du tuto sont ceux de la tête de la branche (une fusion ne les a pas changés depuis).
- **`<id>`** : `docs/audits/ultra/sieges/<tour>/controle/<id>/rapport.md` est dans la branche, sa tête dit « code testé : » avec le même commit, et aucune de ses lignes `chapitre:` ne dit « bloqué ».
- Un lot hors de la zone n'entend parler de rien.

## Ordre pour un lot du tuto

Correction → siège de contrôle sur la branche → dossier du siège et ligne `controle:` commités → relecture neuve → portail. Tout ce qui change après la relecture demande une nouvelle relecture : le siège passe donc avant.

## Essai à blanc sur les vraies fusions du tour 6 (lecture seule)

| Fusion | Lot | Ce que dit la règle |
|---|---|---|
| `35645c88` | tutofleches | touche au tuto (63 fichiers), pas de ligne : refusé |
| `0b8ba680` | tutoconsignes | touche au tuto (124 fichiers), pas de ligne : refusé |
| `1980dff6`, `ea294587` | tutogrille (deux passages) | touche au tuto (`PianoRoll.kt`, le rapport le dit), pas de ligne : refusé |
| `2c146a42` | reparations | touche au tuto (`PianoRoll.kt`, section « Tuto » du rapport), pas de ligne : refusé |
| `42f608f6`, `c40b834e`, `911c6195` | portail3, banc7, emulateur | rien |
| `d4958947`, `9b106bd5`, `ebd06ef3`, `9cc8e9c6`, `4966a019` | guidecoquille, cablage, morceauexport, restes, remixdecoupe | rien |
| `cc492946` | langues (tour 5) | rien (aucun `tuto*.json` changé) |

## Preuves

- Rouge puis vert : `4fa61525` / `6eebc47f` (la règle), `f95f0a85` / `594004f1` (un changement que l'intégration tient déjà).
- `scripts/bench/test_portail4.py` : 37 tests (zone 10, ligne 7, commit 7, siège 7, portail et pré-vol 3, vraies fusions 2, branchements 1). `scripts/bench/test_pre_vol.sh` : 12 cas neufs, donnés au vrai portail sur son dépôt jouet.
- 37 contrôles cassés exprès, un par un, puis remis : chacun fait rougir au moins un test. Un seul ne rougissait rien au premier passage (le filtre « version de la tête de l'intégration ») : il a reçu son test et sa propre paire.

## Ce que la règle ne voit pas

- Si le siège était bien un débutant neuf, s'il a joué le tuto entier, dans la même langue et le même profil que le siège d'avant, si chaque défaut visé est dit « corrigé » : la relecture neuve et le chef le lisent.
- Un grave décrit dans le rapport du siège alors que la ligne dit 0 (seul « bloqué » se recoupe).
- Un lot qui change `PianoRoll.kt` pour le tuto sans que son rapport dise le mot ; les trois autres fichiers de la zone de §13.3 (`RhythmScreen.kt`, `PlainWords.kt`, `TrackSheet.kt`).
- Un rapport de siège écrit à la main dans la branche : la règle demande le fichier et son commit, pas la preuve que le banc l'a écrit (`code-teste.tsv` n'est pas lu).
- Les trois sièges de contrôle du tour 6 déjà dans le dépôt n'ont pas « code testé » en tête : ils ne peuvent pas servir de preuve, c'est voulu.

## Demandes (fichiers hors de la fiche)

- Demande : `docs/AIDE-FUSIONS-TESTS.md` : ajouter au point 11 de la fiche des équipes la ligne `controle:` (lots du tuto, refusé sans elle à partir du paquet 5) et l'ordre « siège avant relecture ».
- Demande : `docs/audits/ultra/GABARIT-FICHES-V3.md` : fiche du correcteur, point 9 : après le contrôle, committer le dossier du siège et la ligne `controle:` dans le rapport de zone ; fiche 1 (siège de contrôle) : rappeler que la tête du rapport porte le commit joué.
- Demande : `docs/audits/AUDIT-ULTRA.md` §13.3 : dire si les trois autres fichiers de la zone doivent entrer dans la règle.

## Chemins
- chemin: banc › autotest du testeur humain | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace
