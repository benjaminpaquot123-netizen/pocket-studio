# Tour 9, lot « porte-paquet » (banc) : rapport

But : faire passer jusqu'à trois zones par le portail avec UN seul self-test au lieu d'un par zone (46 à 75 min par lot avant).

- rouge : e8ab325b (`scripts/bench/test_porte_paquet.py` seul : porte-paquet.sh absent, rouge sur la base)
- vert : d6f52868 (`scripts/bench/porte-paquet.sh` neuf, étapes `PS_PAQUET_ETAPE` dans `fusion-equipes.sh`)

Ce que fait `scripts/bench/porte-paquet.sh [--limite <min>] tour-9 <zone> [<zone> <zone>] [-- filtres]` :
1. pré-contrôle de quelques secondes, refus « REFUSÉ : … » si : disque libre < 8 Go, limite de temps < 90 min (`--limite`, `PS_PAQUET_LIMITE_MIN`, `BASH_DEFAULT_TIMEOUT_MS`, `BASH_MAX_TIMEOUT_MS`), branche `eq/tour-9-<zone>` absente ici et sur origin (créée depuis origin sinon), pre-vol pas PRÊT, contrôles d'une zone refusés, filtre de tests sans aucune classe ;
2. contrôles puis fusion de chaque zone ;
3. un seul self-test (avec les rejeux), puis l'union des filtres par la file Gradle ;
4. les traces du portail de chaque zone, chacune nommant le self-test commun (« paquet: … », « selftest commun: … »).
Rouge après le pré-contrôle : retour au commit de départ, puis le portail zone par zone (il nomme la zone fautive). Ne pousse jamais, ne touche pas `portail-paquet.txt`.

Garde-fou : les étapes de paquet ne valent qu'à partir de `from_paquet 10` ; sans `PS_PAQUET_ETAPE`, le portail se comporte octet pour octet comme avant (test `PortalUnchangedWithoutBatch`). Le juge (`verifie-chantier.py`) accepte déjà les traces d'un paquet sans changement : test avec le vrai juge, 0 « fusion sans trace du portail ».

Preuves : `test_porte_paquet.py` 12 tests verts ; `test_socle.py`, `test_ultra_bench.py`, `test_portail3.py`, `test_portail4.py` verts ; `selftest.sh` vert ; empreinte pre-vol inchangée (« 18 90127c7c59 »).

tests: aucun : lot du banc seul, aucun code de l'appli touché (preuves par les tests du banc ci-dessus)

Statut : tenté (le premier vrai paquet le constatera).
