# Relecture tour-9 ci-fiab
Relu : 55b5261c

Périmètre : de f4301337 à 55b5261c. Quatre fichiers bougent : BenchPristine.kt (la coupure et son appel dans la règle), le nouveau test BenchPristineNetworkTest.kt, le rapport et la note de décisions du lot. Les commits ae484dae et 710e885d s'annulent : aucun écart net sur .github. Aucun fichier de l'appli, aucun fichier interdit (Tour5BloquantsTest.kt, nightly.yml, CLAUDE.md, etc.) n'est touché. Thread audio : non concerné.

La coupure est juste. Elle remplace le ProxySelector par défaut le temps du test, renvoie les hôtes distants en http et https vers 127.0.0.1:9 (port fermé) et remet l'ancien dans un finally, seulement si personne ne l'a changé entre-temps. Un sélecteur d'avant nul est bien géré (NO_PROXY à l'aller, null remis au retour). Une règle dans une règle se défait dans l'ordre. Les tests d'une même JVM tournent l'un après l'autre : pas de course sur ce réglage global. PackStore.fetchCatalogue, VerifiedDownload.fetch et UpdateChecker passent tous par HttpURLConnection, donc par ce sélecteur. Les serveurs locaux du banc (BenchSeparation sur 127.0.0.1, le proxy lent de BenchNetworkEndTest) gardent leur route. Un vrai siège n'est pas coupé (seatJournalFor n'a pas d'effet de bord, l'appeler deux fois est sans risque). Aucun test sous la règle n'a besoin d'un hôte distant.

Le test rouge prouve quelque chose. Sans la coupure (c7ecf2d1), la route de github.com reste celle de la machine : la première assertion tombe. Avec elle, l'assertion « ConnectException en moins de 3 s » sur un hôte en .invalid prouve aussi que Java ne retente pas une connexion directe après le refus du proxy (sinon on verrait UnknownHostException). Les trois autres tests sont des garde-fous, verts avant comme après : ils protègent les exceptions voulues, ce qui est leur rôle.

Rejoué ici : BenchPristineNetworkTest 4 verts sur 4, BenchNetworkEndTest 2 verts (le seul sauté est le siège drive, sauté d'habitude).

Moyen : BenchPristine.kt:62, la coupure se lève dès la fin du test. Un travail réseau lancé après l'attente de end(), ou pendant la fermeture de la règle compose si JUnit la place autour de celle-ci (l'ordre des deux règles n'est pas fixé), part de nouveau sur le vrai réseau. Le risque est faible, car pendant le test chaque connexion échoue tout de suite ; le rapport ne cite pas cette limite.

Mineur : BenchPristine.kt:455, l'hôte local se reconnaît par son nom seulement (localhost, ::1, 127.x). Les adresses 0.0.0.0, 0:0:0:0:0:0:0:1 ou le nom de la machine seraient refusés. Aucun test actuel ne les emploie.

Mineur : seuls les schémas http et https sont coupés. Une socket brute, ou une future bibliothèque qui choisit son propre proxy, passerait. L'appli n'en a pas aujourd'hui.

Mineur : BenchPristine.kt:466, un sélecteur laissé par un test n'est ni remis ni signalé par leaks(). C'est voulu et écrit en commentaire.

Mineur : rapport honnête sur ses limites (rouge de la CI 449 pas reproduit, tests sans la règle comme TutoScreenTest toujours sur le vrai réseau, statut « tenté »). Les cinq passages verts mesurent la stabilité, pas la course du runner ; le rapport le dit.

Verdict : 0 bloquant, 0 grave
