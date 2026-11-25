# Tour 9, lot gr-l0 (grand remplacement v2, L0) : rapport

Outillage seul (aucun son ajouté ni retiré, aucune écoute) : les sources d'un pack publié passent une liste blanche de licences (CC0, CC-BY, domaine public ; NC, SA, Sampling+ et licence sans nom arrêtent `build_index.py`), et l'index des packs dit quels sons une recette peut nommer (`recipeSounds` / `recipeParts`) : percussions VCSL (`percussions_acoustiques`), claquements de doigts (`bruitages_maison`, son `doigts`, OpenGameArt), flûte VSCO-2 (`instrument_bois`, touches 67 à 100).

- rouge : 702fed51 (tests `scripts/packs/tests/`, rouges sans l'outillage)
- vert : 00e2329b (`build_index.py`, drapeaux `recipes` des deux catalogues)

Preuves : `bash scripts/packs/tests/run.sh` (15 tests, dont un pack NC fabriqué qui arrête la construction) ; vraie construction des trois packs hors du dépôt puis `build_index.py` : l'index porte les trois sources ; `git status` sans aucun audio ; `:project-model:test --tests '*PackCatalogTest*'` vert.

tests: com.pocketstudio.app.library.DownloadAllTest

chemin: Bibliothèque, recherche « doigts » → claquement de doigts placé (index des packs lu par l'appli) | test: DownloadAllTest#theSearchFindsAFingerSnapAndPlacesIt

Statut : tenté (outillage ; sert le lot L2).
