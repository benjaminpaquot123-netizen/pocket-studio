# Lot banc « banc6b » : relecture neuve
Relu : 6a646f88b8700544411b64178848171d00cebbdb

Relecteur neuf (n'a pas écrit le lot). Base fx-int `165c4e9c`, tête de l'auteur `0af799c5`, plus mes deux paires de correctifs. Lu : le diff entier (juge, portail, `relecture-verdict.py`, `fuse-reports.py`, `HumanTester`, `BenchEngine`, tests du banc), le rapport de l'auteur. Rejoué : `python3 -I scripts/bench/test_ultra_bench.py` (OK avant et après), `BenchSelfTest#aTestExceptionIsNeverTheAppsCrash` par `gradle-file.sh` (vert à `0af799c5`, rouge à `995d6cb3`, vert à `6a646f88`), paires du juge sur l'historique réel (mêmes 6 écarts « paires » qu'à `0af799c5` : les 5 de « textes » restent fermés).

## Constats corrigés

- **Grave (G1)** : l'appariement par sujet était trop large. Un mot d'un « Classe#méthode » comptait comme sujet : « vert: FooTest#autreCas » fermait « rouge: FooTest#unCas (…) » par la classe, soit un vert d'un autre test qui ferme un rouge. Reproduit dans le jouet `judge_topic_pairs` (rouge `eba06a00`, le juge annonçait 3 écarts au lieu de 5). Le mot accolé à `#`, `.`, `::` n'est plus un sujet (ces formes sont déjà appariées par les noms) — **corrigé** en `801fec56` (vert). Historique réel inchangé.
- **Grave (G2)** : le tri des exceptions (5.10) s'arrêtait au premier maillon qui a une trame `com.pocketstudio` : une exception de l'appli relancée par une trame du banc (cause enveloppée, boucle ou aide qui relance) devenait « BANC … ce n'est pas l'appli », un vrai plantage caché. Rouge `995d6cb3` (cas « relancée par le banc » dans `aTestExceptionIsNeverTheAppsCrash`, échec sur son `assertEquals("plantage")`) ; tout maillon lancé par l'appli en fait un plantage — **corrigé** en `6a646f88` (vert).

## Mutations

- `NEGATION` sans « jamais » (`relecture-verdict.py`) : `review_rows` rougit sur « jamais **corrigé** ». Remis.
- `test_in` sans `still_runs` (`verifie-chantier.py`) : `red_seen_in_a_run` rougit (5 écarts au lieu de 7). Remis.
- `TINY_BYTES = 0`, puis le test `EMPTY` coupé (`fuse-reports.py`) : la suite reste verte dans les deux cas (voir M1). Remis.
- `thrownByTheApp` d'origine face au cas « relancée par le banc » : rouge (G2 ci-dessus).

## Points moyens restants (non bloquants)

- M1. `export_figures_are_the_benchs` : la carte « vide » dit à la fois « 1 KB » et « vide » ; chaque garde (seuil 64 Ko, mot « vide ») couvre l'autre, aucune n'est prouvée seule. Ajouter une carte « 3 Ko » sans « vide » et une « vide » sans taille.
- M2. `relecture-verdict.py` : « non rejoué sur <parent> » et « jamais rejoué sur … » sont acceptés (seul « pas » est lu devant « rejoué ») ; « pas du tout **corrigé** », « pas vraiment **corrigé** » passent pour corrigés (seul le dernier mot, ou celui avant « encore », est lu). Texte de relecteur, pas une forge facile ; à durcir au prochain lot du portail.
- M3. `HumanTester` : sur une exception du banc, `problems.clear()` jette les erreurs d'écran de ce geste sans les écrire (« SCREEN ERROR ») et `stalls()` est sauté. Écrire les deux avant le `continue`.
- M4. Un rouge qui nomme une classe seule (« rouge: FooTest (…) ») est fermé par n'importe quel vert nommant « FooTest » avant sa parenthèse : voulu (sujet = classe), à garder en tête.
- m. 5.2 et 5.4 rouges prouvés à la main seulement ; `play()` appelle `followTextDialogs()` deux fois (sans effet vu).

## Rouges constatés

La branche n'apporte ni ne change aucun dossier `rouges-constates/` ni `rejeux/` : aucune ligne « rouge constaté » ni « rejeu » exigée.

Verdict : 0 bloquant, 0 grave — fusion possible
