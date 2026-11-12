# Relecture du lot « juge7 » (tour 5), par un relecteur neuf
Relu : c05acd7d00aef27e6308923d5e162d4ad5b17af5

Cible : `git diff 1585bb46..9640d69b` (le juge `scripts/bench/verifie-chantier.py`, ses tests dans `scripts/bench/test_ultra_bench.py`, les preuves `paires-rejouees/37e2c8ed…`, `rouges-mutes/a95d9667…`, `rouges-mutes/2e2c5f3d…`, le rapport et les décisions). Aucun code de l'appli touché.

## Ce qui a été refait
- `python3 -I scripts/bench/test_ultra_bench.py` sur la tête : vert (« ultra bench scripts: OK »).
- Témoins rejoués sur les commits rouges (worktrees jetables, retirés) :
  - d36c60ff : `replay_window` rougit (fenêtre db66a3ce). La fenêtre remise à 1585bb46 à la main, `replay_python` rougit (« vert sans rouge » pour honest, skipped, helper). `replay_python` neutralisé en plus, `mutated_bench_helper` rougit (kthelper, pyscript, intest refusés pareil). Les trois témoins rougissent donc bien sans le changement.
  - 81d5fbc4 : `red_seen_bench_test` rougit (« chantier : 0 écarts » au lieu de 1).
- Paire rejouée d56efddf / 37e2c8ed, `junit.py … test_socle NewReplaysAimByTag.<test>` sur les trois tests : les trois en échec sur le rouge (mêmes messages que `rouge.xml` : « 'touche mode_card_simple' », « 0 != 2 », « 'swipeByTag' not found »), les trois verts sur le vert. Le vert 37e2c8ed ne touche pas `test_socle.py` (HumanTester.kt, ScreenText.kt, rejeu-etiquettes.py, rejeu-nouveau.sh).
- Mutation a95d9667 rejouée : sans le patch `test_a_command_can_stand_for_an_untagged_button` vert ; `git apply mutation.patch` puis rejeu : en échec, « Gestures with no tag under the finger: see above. », comme `rouge.xml`.
- Mutation 2e2c5f3d jugée à la lecture (pas de Gradle) : le patch ôte la seule ligne qui affecte `best` dans `nearestReachable`, qui rend donc toujours null ; le test échoue alors à `assertTrue("a point of the card is within reach", at != null)`, le message de `rouge.xml`. Cohérent.
- Juge lancé comme le portail (`--resume …/tour-6/lotA/selftest-resume.txt`) : résumé d'un autre code (attendu), et 8 écarts de paires : 2b34c216, a2a6ec58, 11c48708, 411b2d51, 9b8fc4fd, 96a7a702, ade1a174 (les 7 acceptés de juge6) et bc0e6f27. Plus de d56efddf, 37e2c8ed, a95d9667, 2e2c5f3d.
- bc0e6f27 relu : le vert ne change que `scripts/bench/test_ultra_bench.py` (3 lignes, `os.environ.pop("PS_REJEUX_DIR", None)`), pas de rouge avant lui. Le raisonnement du rapport est juste : ni rejeu, ni mutation (le seul fichier est un test), ni rouge constaté (le vert touche le test). Le garder nommé est le bon choix.
- `test_in` côté Kotlin : le `git grep` sans résultat donne maintenant une liste vide au lieu de `return False`, puis la recherche Python ; pour un nom Kotlin sans classe Python homonyme le résultat est le même False. Pas de changement de comportement Kotlin trouvé.

## Constats

### G1 (grave) : la règle « fichier Kotlin sans @Test = code du banc » laisse muter un fichier de test, dans un vrai fichier du dépôt

**corrigé** par f016e61a (rouge b6da3cff, voir Delta).
Dans `mutated_red`, un fichier de `TEST_DIRS` compte comme du code quand `"@Test" not in without_comments(texte)`. C'est une sous-chaîne, pas la règle que le juge applique ailleurs : `still_runs` accepte `@org.junit.Test` (`@(?:org\.junit\.)?Test\b`), qui ne contient pas « @Test ». Un fichier dont les tests portent `@org.junit.Test` (ou `@kotlin.test.Test`, ou un import renommé) passe donc pour du code du banc, et une mutation peut ôter une ligne du test lui-même : le test rougit, le vert est fermé sans rouge de sa correction.
Ce n'est pas théorique : `app/src/test/java/com/pocketstudio/app/UiScaleScreenshotTest.kt` porte ses tests en `@org.junit.Test` et zéro « @Test » ; il est dans la liste des fichiers « sans @Test ».
Reproduit (jouet construit avec les aides de `test_ultra_bench.py`, script hors dépôt `/tmp/claude-0/-home-user-pocket-studio/7aee178f-f3d8-5614-9304-422cd425f774/scratchpad/toy7/probe.py`) : un vert ajoute `FqReachTest` avec `@org.junit.Test fun aCaseHolds() { var y = 1; check(y == 1) }`, `mutation.patch` ôte `var y = 1` du fichier de test, `rouge.xml` en échec, `vert.xml` vert : aucun écart (fermé comme un rouge). Le même jouet avec `@Test` : écart (refusé). Le décret du lot (« un fichier qui contient un `@Test` … reste un test, jamais muté ») et le docstring de `mutated_bench_helper` disent plus que le code.
Borné à l'histoire avant `REPLAYS_UNTIL`, comme le G1 de juge6, mais c'est la garantie même que le lot annonce.
À corriger : décider « contient un test » avec la règle de `still_runs` (au moins `@(?:org\.junit\.|kotlin\.test\.)?Test\b`, mieux : une fonction du fichier pour laquelle `still_runs` vaut vrai, ou toute annotation finissant par `Test`), et un cas `@org.junit.Test` dans `mutated_bench_helper` qui doit rester un écart.

### M1 (moyen) : la règle « code du banc » vaut pour tout fichier d'aide des tests, pas seulement pour le banc

**corrigé** par f016e61a (voir Delta ; reste m4).
L'intention écrite est « le code du banc qui vit sous les tests (HumanTester.kt, SeatOptions.kt) ». Le code accepte n'importe quel `.kt` des tests sans `@Test` (31 fichiers aujourd'hui : `nav/AppHarness.kt`, `audit/LayoutScan.kt`, `StrictTestApplication.kt`…). Un vert qui corrige le code de l'appli et ajoute une aide de test à côté de son test peut se faire fermer par une mutation qui ôte une ligne de l'aide : le test rougit parce que son outillage est cassé, pas parce que la correction est retirée. Le juge ne sait pas faire la différence ; seul le relecteur qui lit le patch la fait. Même nature que le M1 de juge6 (borné à l'histoire, le patch est là pour être relu). Piste : n'accepter un fichier d'aide que si le vert ne touche aucun code hors des tests (le vert répare le banc lui-même, cas de 2e2c5f3d), ou une liste nommée des fichiers du banc.

### m1 (mineur) : `py_test_in` lit les lignes, pas Python
Faux positifs possibles, tous sans effet sans un XML mensonger (le XML est déjà la parole du lot) : décorateur de saut sur plusieurs lignes ou sur la classe (`@unittest.skip` au-dessus de `class`), décorateur sous un autre nom (`@ut.skip`), classe qui n'hérite pas de `unittest.TestCase`, méthode redéfinie plus bas avec un saut (la première l'emporte au juge, la dernière en Python), et un texte entre triples guillemets à la colonne 0 qui ressemble à une classe. Le module n'est pas lié non plus : `rejeu.txt` nomme `Classe#méthode`, et `_case_result` ne garde que le dernier segment de `classname`, donc deux `test_*.py` avec une classe du même nom se confondent. Témoins absents pour ces cas.

### m2 (mineur) : les preuves ne couvrent qu'une partie des verts
a95d9667 (« a swipe by tag needs its proof; --commande for untagged buttons ») n'est prouvé que sur `--commande` ; 2e2c5f3d que sur `nearestReachable`, pas sur le branchement `reachableNear` dans HumanTester. C'est la doctrine de juge6 (« sa propre correction, en partie reprise »), le rapport ne prétend pas plus ; à garder en tête pour la lecture « fermé ».

### m3 (mineur) : `rouge.xml` de la paire 37e2c8ed pèse 143 Ko
Le troisième échec recopie tout `HumanTester.kt` dans le message (`assertIn` sur le fichier entier). Sans effet sur le juge ; tronquer le corps dans `junit.py` éviterait de grossir le dépôt.

## Ce qui est juste
- Le rapport ne dit rien de plus fort que ses preuves, sauf la phrase sur « un fichier qui contient un test … n'est jamais muté » (G1).
- `red_seen` refuse bien un vert qui touche `scripts/bench/test_*.py` (témoin rouge puis vert).
- Le déplacement de `REPLAYS_UNTIL` suit la même règle que juge6 (base du lot, histoire déjà écrite).

## Delta (relu c05acd7d, après b6da3cff rouge, f016e61a vert, c05acd7d docs)
- Correction : `mutated_red` n'accepte plus comme code du banc qu'un `.kt` de `BENCH_HELPERS` (`app/src/test/java/com/pocketstudio/app/human/`) sans `@(?:org\.junit\.)?Test\b` (la règle de `still_runs`).
- `python3 -I scripts/bench/test_ultra_bench.py` sur c05acd7d : vert (« ultra bench scripts: OK »).
- Rouge b6da3cff rejoué (worktree jetable, retiré) : `mutated_bench_helper` échoue, « a mutation that does not hold (fqtest) is no red » (le témoin s'arrête au premier échec : otherhelper n'y est pas vu seul, mais la règle du dossier est lisible dans le diff et couverte au vert).
- Ma sonde `/tmp/claude-0/-home-user-pocket-studio/7aee178f-f3d8-5614-9304-422cd425f774/scratchpad/toy7/probe.py` : le cas `@org.junit.Test` est maintenant refusé (écart). Comme elle vit dans `app/src/test/java/x` (hors du dossier du banc), je l'ai rejouée aussi dans `…/human/` (`probe_human.py`, même dossier) : refusé aussi. Le refus tient donc par l'annotation, pas seulement par le dossier.
- Juge comme le portail (`--resume …/tour-6/lotA/selftest-resume.txt`) : toujours 8 écarts de paires (2b34c216, a2a6ec58, 11c48708, 411b2d51, bc0e6f27, 9b8fc4fd, 96a7a702, ade1a174) ; 2e2c5f3d reste fermé (SeatOptions.kt est dans `human/`, sans test).
- G1 : **corrigé**. M1 : **corrigé** (une aide des tests de l'appli n'est plus du code).

### m4 (mineur, après correction) : ce qui reste de G1 / M1
- Un vert qui corrige l'appli et ajoute une aide dans `human/` pourrait encore se faire fermer en cassant cette aide ; le relecteur lit le patch (même nature que le M1 de juge6, borné à l'histoire).
- `@kotlin.test.Test` ou un import renommé de `org.junit.Test` ne sont pas reconnus ; aucun fichier du dépôt n'en utilise aujourd'hui.

Verdict : 0 bloquant, 0 grave — fusion possible
