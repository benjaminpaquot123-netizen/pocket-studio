# Tour 8, lot « niveaux » : relecture neuve
Relu : a02e9bd5

Portée : `git diff b63a667c...a02e9bd5` (seat-metrics.py, fabrique-sieges.py, tableau-de-bord.py, test_ultra_bench.py, sièges d'exemple `scripts/bench/oracles-niveaux/`, la ligne « changed » de `SeatState.kt`), lu par un sous-agent neuf qui n'a pas écrit le code. Depuis sa lecture, seuls ont bougé « \t1 » à `test_portail3.py:829` (accordé par le chef) et le rapport. Lecture seule, Gradle non joué (pas de SDK ici) : la ligne de `SeatState.kt` est jugée compilable à la lecture, la CI la compilera.

Points vérifiés sans défaut : un fait absent donne « inconnu », jamais « non » ; `libre` et X ne remplissent aucune ligne du tableau ; un siège sans niveau, `libre` avec niveau et `reprendre` sont refusés par la fabrique ; les quatre paires d'exemple donnent « reussie » / « non » + ce qui manque.

Constats (aucun bloquant, aucun grave) :
1. moyen : les durées .flac ne sont jamais lues : des pistes .flac justes donnent « partielle ». Ouvert.
2. moyen : le nombre de pistes attendu par `pistes-separees` est le maximum sur tous les projets, pas celui du projet exporté. Ouvert.
3. moyen : basse et accords de `boucle-imposee` ignorent la ligne acid et une boucle répétée sur 2 mesures. Ouvert.
4. mineur : le rythme ignore les pistes en trop ; l'export de `projet-vide` accepte n'importe quel fichier partagé ; `mon-morceau` compte un morceau « first » avec un style. Ouverts.
5. mineur : `int(k)` et une piste nulle plantent l'oracle sur un projet abîmé. Ouvert.
6. mineur : pas de test pour « partielle », .flac ni niveau X ; la docstring du tableau dit « mots-faux » alors que l'expression compte les lignes « - ecran: | vu: | attendu: ». Ouverts.

Verdict : 0 bloquant, 0 grave — fusion possible
