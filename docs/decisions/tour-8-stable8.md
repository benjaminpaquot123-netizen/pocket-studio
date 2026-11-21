# Tour 8, lot stable8 : décisions

- Cause mesurée, pas devinée : `c_` rejoué avec le démarrage du worker retenu 300 ms, sans charge, donne le même rouge mot pour mot (« red itself, by name: null »). Retour arrière : revenir au commit d'avant le « rouge: ».
- Correction dans le test seulement : il attend que le travail laissé tourne vraiment (`running.await`, 60 s au plus) avant d'appeler `afterTest`. Les assertions ne changent pas. Raison : le test dit « un test qui laisse du travail EN COURS » ; sans l'attente il ne laissait parfois rien en cours.
- Le trou du contrôle lui-même (du travail encore en file, pas démarré, n'est pas vu) est dans `BenchPristine.kt`, hors de ma liste : signalé au chef, pas corrigé ici.
- Charge de la preuve : une copie jetable du dépôt (`git worktree`) rejoue `rejeu-defauts.sh t6-da15-ecoute-passee` en boucle, avec son propre verrou et sans arrêter les démons de l'autre Gradle (sinon les deux Gradle se tuent l'un l'autre) ; la garde « 1 démon au plus » du selftest de cette copie est coupée. Rien de cela n'est commité.
