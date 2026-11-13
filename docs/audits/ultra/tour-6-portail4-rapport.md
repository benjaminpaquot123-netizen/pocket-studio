# Tour 6, lot du banc « portail4 » : pas de lot du tuto sans son siège de contrôle

Branche : `eq/tour-6-portail4` (base `ce0aed6f`, tête poussée de l'intégration). Décisions : `docs/decisions/tour-6-portail4.md`. Aucun fichier de `app/` touché, `scripts/bench/portail-paquet.txt` non plus.

tests: aucun : lot du banc seul (scripts du portail), aucun test Gradle changé ; banc : `python3 -I scripts/bench/test_portail4.py`, `bash scripts/bench/test_pre_vol.sh`, `python3 -I scripts/bench/test_socle.py`, `python3 -I scripts/bench/test_portail3.py`, `python3 -I scripts/bench/test_ultra_bench.py`

## La règle (à partir du paquet 6)

Le portail refuse, avant la fusion et avec la sortie 10, un lot qui change la zone du tuto si son rapport de zone ne porte pas, pour chaque siège de contrôle, cette ligne :

```
controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0
```

Le refus du portail : `<zone>: tutorial lot without its control seat: <raisons>: not merged.` Le pré-vol dit la même chose : « lot du tuto sans son siège de contrôle : … », RECALÉ dès le paquet 6, « À VOIR … refusé à partir du paquet 6 » avant.

- **Zone** : un fichier que le lot change (diff depuis sa base, et différent de la tête de l'intégration) sous `src/main`, dans un dossier `tuto/` ou `tutos/`, ou nommé `i18n/<langue>/tuto*.json`. Les quatre fichiers partagés de §13.3 (`PianoRoll.kt`, `RhythmScreen.kt`, `PlainWords.kt`, `TrackSheet.kt`) comptent quand le rapport dit le mot (hors blocs de code). Les tests, les captures de référence et les docs n'en font pas partie. Les noms de fichiers sont lus tels quels, accents et espaces compris.
- **Lignes** : au moins une ; chacune bien formée, avec 0 et 0. Une seule mauvaise refuse le lot. Une ligne écrite en puce, en numéro, en ligne de tableau, en citation ou en gras est lue comme les autres.
- **`<sha>`** : un commit de la branche, joué après le dernier changement du tuto fait par le lot, et dont les fichiers du tuto sont ceux de la tête de la branche (une fusion ne les a pas changés depuis). Dès qu'un lot touche la zone, les quatre fichiers partagés qu'il change comptent ici, que son rapport dise le mot ou non.
- **`<id>`** : `docs/audits/ultra/sieges/<tour>/controle/<id>/rapport.md` est dans la branche, sa tête dit « code testé : » avec le même commit, et aucune de ses lignes de chapitre ne dit « bloqué » (les deux écritures des sièges du tour 6 sont lues).
- Un lot hors de la zone n'entend parler de rien.

## Ordre pour un lot du tuto

Correction → siège de contrôle sur la branche → dossier du siège et ligne `controle:` commités → relecture neuve → portail. Tout ce qui change après la relecture demande une nouvelle relecture : le siège passe donc avant.

## Essai à blanc sur les vraies fusions (lecture seule, refait après la relecture)

| Fusion | Lot | Ce que dit la règle |
|---|---|---|
| `35645c88` | tutofleches | touche au tuto (63 fichiers), pas de ligne : refusé |
| `0b8ba680` | tutoconsignes | touche au tuto (126 fichiers, dont `TrackSheet.kt` et `PlainWords.kt`), pas de ligne : refusé |
| `1980dff6`, `ea294587` | tutogrille (deux passages) | touche au tuto (`PianoRoll.kt` et `RhythmScreen.kt`, le rapport le dit), pas de ligne : refusé |
| `2c146a42` | reparations | touche au tuto (`PianoRoll.kt`, section « Tuto » du rapport), pas de ligne : refusé |
| `911c6195` | emulateur | touche au tuto depuis la relecture (`PlainWords.kt`, une ligne ; le rapport écrit « le panneau du tuto ») : refusé |
| `cc492946` | langues (tour 5) | touche au tuto depuis la relecture (`RhythmScreen.kt` ; le rapport écrit « tutos ») : refusé |
| `42f608f6`, `c40b834e` | portail3, banc7 | rien |
| `d4958947`, `9b106bd5`, `ebd06ef3`, `9cc8e9c6`, `4966a019` | guidecoquille, cablage, morceauexport, restes, remixdecoupe | rien |

Les deux lignes « depuis la relecture » montrent le prix du point resté ouvert M1 : un lot qui change un fichier partagé et dont le rapport écrit le mot, même en passant, se voit demander un siège.

## Preuves

- Rouge puis vert : `4fa61525` / `6eebc47f` (la règle), `f95f0a85` / `594004f1` (un changement que l'intégration tient déjà), `d4c7b116` / `69bd040e` (la relecture neuve).
- `scripts/bench/test_portail4.py` : 59 tests (zone 10, ligne 7, commit 7, siège 7, relecture du lot 22, portail et pré-vol 3, vraies fusions 2, branchements 1). `scripts/bench/test_pre_vol.sh` : 12 cas neufs, donnés au vrai portail sur son dépôt jouet.
- Contrôles cassés exprès, un par un, puis remis. Premier passage : 37 cassures, 36 font rougir un test ; celle qui ne rougissait rien (le filtre « version de la tête de l'intégration ») a reçu son test et sa propre paire. La relecture neuve en a trouvé trois autres que je n'avais pas cassées (`--no-renames`, les deux points dans le nom du siège, la sortie 2 sans ses options) : la phrase d'avant, « chacun fait rougir au moins un test », était donc fausse. Second passage, après la relecture : 17 cassures (les corrections et ces trois contrôles), 17 font rougir un test.

## Relecture neuve (première passe sur e8503cbe : 2 graves, corrigés)

- G1, la zone oubliait `RhythmScreen.kt`, `PlainWords.kt` et `TrackSheet.kt` : ils rejoignent `PianoRoll.kt` (comptés quand le rapport dit le mot) ; et dès qu'un lot touche la zone, les quatre comptent pour « le siège a joué après le dernier changement » et « mêmes fichiers au commit joué et à la tête », quoi que dise le rapport. Tests : `ReviewOfTheLot`, six cas.
- G2, git met entre guillemets un nom de fichier qui n'est pas en ASCII simple, et la zone ne le voyait pas (`tuto_café.json`) : les noms se lisent avec `-z`, dans la zone, dans la liste des fichiers changés par une fusion et dans la recherche des tests du mode simple. Tests : `ReviewOfTheLot`, sept cas, dont un nom avec une espace.
- M3, « bloqué » se lit aussi dans l'écriture réelle des sièges du tour 6 (« - chapitre 5 « … » : bloqué ») ; un chapitre qui ne fait que citer le mot n'est pas bloqué. Deux tests.
- M4, les trois contrôles sans test ont le leur (renommage hors de la zone, deux points dans le nom du siège, sortie 2 sans `base:`, `tete:` ou `tour:`).
- Mineurs : les blocs `~~~` sont sautés comme les blocs à accents graves ; une ligne `controle:` en numéro, en ligne de tableau, en citation ou en gras est lue (bonne, elle compte ; mauvaise, elle refuse le lot).
- Un test existant a changé dans le commit « vert: » : la vraie fusion « emulateur » est passée des lots laissés tranquilles aux lots à qui l'on demande un siège (voir l'essai à blanc).
- Restent ouverts :
  - M1 : un lot honnête hors du tuto qui change un des quatre fichiers partagés et dont le rapport écrit « tuto » quelque part se voit demander un siège.
  - M2 : un rapport de siège écrit à la main dans la branche passe ; `code-teste.tsv` n'est pas lu.

## Ce que la règle ne voit pas

- Si le siège était bien un débutant neuf, s'il a joué le tuto entier, dans la même langue et le même profil que le siège d'avant, si chaque défaut visé est dit « corrigé » : la relecture neuve et le chef le lisent.
- Un grave décrit dans le rapport du siège alors que la ligne dit 0 (seul « bloqué » se recoupe, et seulement sur une ligne de chapitre).
- Un lot qui ne change que des fichiers partagés pour le tuto sans que son rapport dise le mot.
- Les trois sièges de contrôle du tour 6 déjà dans le dépôt n'ont pas « code testé » en tête : ils ne peuvent pas servir de preuve, c'est voulu.

## Demandes (fichiers hors de la fiche)

- Demande : `docs/AIDE-FUSIONS-TESTS.md` : ajouter au point 11 de la fiche des équipes la ligne `controle:` (lots du tuto, refusé sans elle à partir du paquet 6) et l'ordre « siège avant relecture ».
- Demande : `docs/audits/ultra/GABARIT-FICHES-V3.md` : fiche du correcteur, point 9 : après le contrôle, committer le dossier du siège et la ligne `controle:` dans le rapport de zone ; fiche 1 (siège de contrôle) : rappeler que la tête du rapport porte le commit joué.

## Chemins
- chemin: banc › autotest du testeur humain | test: BenchSelfTest#reachTagTouchesAVisibleElementInPlace
