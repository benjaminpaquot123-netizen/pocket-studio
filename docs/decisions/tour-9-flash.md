# Décisions du lot « flash » (tour 9)

- 2026-11-21 : au tout premier lancement, le studio apparaissait ~0,5 s entre le logo et « Bienvenue ! » (la fenêtre Bienvenue attendait la fin du logo, `!Intro.playing`). Remède d'octobre repris : Bienvenue est dessinée dans l'écran (plus de fenêtre à part), déjà en place sous le logo, et le studio est couvert (`studioCover`, sémantique effacée) tant que le guide n'est pas vu. Elle est posée hors de `key(simple)` pour que son « Simple ou Pro ? » survive au changement de mode. Comportement inchangé (langues, choix, retour : `BackHandler` = même `dismiss`). Retour arrière : annuler ca8b0d2bf.
- 2026-11-21 : deux tests du banc rougissent (fichiers du banc, hors de ce lot) : non touchés, demandés au chef (voir le rapport).
