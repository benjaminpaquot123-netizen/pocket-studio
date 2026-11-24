# Tour 9, lot gr-l0 : relecture neuve

relu : 2f062096

Relecteur : sous-agent neuf (n'a pas écrit le code), sur le diff depuis origin/ccr-80af1a1b-mn0wec ; `scripts/packs/tests/run.sh` et `build_index.py --check` verts.

- grave (**corrigé** en 2f062096) : un pack de bibliothèque vide (`sounds: []`, `credits: []`) arrêtait toute la publication ; il est de nouveau mis de côté, test `test_an_empty_library_pack_is_left_out_not_fatal`.
- moyen (**corrigé** en 2f062096) : le slug d'un son `open_hat_…` était mal lu ; le type vient du manifeste, cas ajouté au test.
- moyen (**corrigé** en 2f062096) : un pack des catalogues sans `credits` passait sans contrôle ; il est refusé.
- moyen (ouvert, hors des fichiers du lot) : `scripts/packs/tests/run.sh` n'est lancé par aucun workflow ; signalé au chef (étape Python légère dans la CI).
- léger (**corrigé** en 2f062096) : CC-BY-2.5 accepté.
- léger (ouvert) : `recipeParts` copié du catalogue, sans vérifier que les zones construites couvrent ces touches ; à valider par L1/L2.
- léger (**corrigé** en 2f062096) : l'audio gelé est une liste exacte de fichiers.
- léger : l'appli lit l'index avec ignoreUnknownKeys, PackCatalogTest lit libraries.json en arbre : les champs neufs ne gênent pas.

Thread audio : non concerné.

Verdict : 0 bloquant, 0 grave
