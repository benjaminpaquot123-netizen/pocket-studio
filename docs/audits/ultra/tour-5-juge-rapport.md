# Tour 5, lot 1 « Le juge » : rapport

Atelier `/home/user/eq-tour-5-juge`, branche `eq/tour-5-juge` (base `fx-int` b243996). Rien n'est poussé. Un seul auteur, pas de sous-agent. Aucun code de l'appli ni du cœur moteur n'a changé : scripts du banc, tests du banc, nommage dans le lanceur de tests C++, mémoire du chantier.

## Ce qui a changé, ligne par ligne

| Ligne | Ce qui change | rouge | vert |
|---|---|---|---|
| A.4 | `selftest-verdict.py` : classe sautée en entier = rouge ; saut hors de `selftest-sauts-connus.txt` = rouge ; le résumé nomme chaque test vert (`vert : Classe#méthode`) et le juge ne lit plus « sauté » comme vert. | 004e366 | 7f5a0ce |
| A.10 | `verifie-chantier.py` applique ses quatre critères : ligne « appli » = preuve hors banc qui existe OU essai ❌ daté ; ligne « banc » = preuve hors banc ; `--resume` obligatoire (écart « résumé absent ») ; paires rouge/vert appariées par nom de test et lignes cochées du journal liées à `preuve: rouge <sha> / vert <sha>` ; « corrigé » = commit qui existe. Cinq cas neufs dans `test_ultra_bench.py` (`judge_criteria`). `marque-corrige.sh` et `selftest.sh` lancent le juge avec `--resume`. | c3eebf7 | 7e33ec5 |
| A.12 | Preuve moteur lisible : `engine/host-tests/<fichier>#<fonction>`, vérifiée (définie et inscrite dans une liste `PS_*_TESTS`). Le lanceur C++ reconnaît le préfixe `known_defect_` et écrit « KNOWN DEFECT still there ». Colonne `garde_id` dans `verdicts.tsv`, vérifiée dans les deux sens. | 03c768c | 8180d82 |
| A.3 | `selftest-attendus.py` calcule la liste attendue : classes du banc, du renommage, et toute classe porteuse d'un `@KnownDefect` (passe « defauts » pour `ChallengeControllerTest`). Lignes de doc corrigées : en-tête de `selftest.sh`, `AUDIT-ULTRA.md` §12.1, KDoc de `@KnownDefect`. | 0a4d552 | a460613 |
| A.4 et A.10 (suite, trouvée par le vrai selftest) | Un saut connu nommé sans paramètres couvre ses cas paramétrés ; les douze `drive` des sièges sont inscrits ; un selftest rouge montre quand même le juge. | c39ca77 | 3dab50a |
| Données | `verdicts.tsv` : dates des essais ❌ reprises de `retours-benjamin.md`, commit `ffd58ce` pour T4-17 et T5-Fc-8 ; journal : lignes cochées du lot avec leurs paires. | | b173bcf |

## Commandes lancées et résultats réels

- `python3 -I scripts/bench/test_ultra_bench.py` : rouge à chaque commit `rouge:` (A.10 : « FAIL: appli line C has no proof » ; A.12 : « FAIL: an engine host test, defined and registered, is a proof » ; A.3 : `selftest-attendus.py` absent ; suite : « FAIL: a known skip covers its parameterised cases »), puis « ultra bench scripts: OK » à chaque `vert:`.
- `./scripts/host-tests.sh` (une fois, en entier) : 361 `[ OK ]`, dont `known_defect_t5_padretrig (KNOWN DEFECT still there)`, « ALL PASSED (0 failures) », code 0.
- `bash scripts/bench/selftest.sh <lot>` (une seule fois, via `gradle-file.sh`) : **rouge**, « 130 verts, 41 rouges, 1 en quarantaine, 41 sautés ». Les 41 rouges sont exactement les 41 sautés : les `drive` des sièges (« only with -Phuman.dir »), que la règle A.4 neuve refuse tant qu'ils ne sont pas inscrits. Aucun autre rouge. La liste attendue contient bien `Tour5AppProof2Test` et `ChallengeControllerTest`, tous deux joués et verts (16 lignes `vert :` pour ces classes).
- Après inscription des douze `drive` (3dab50a), le verdict rejoué **sur les mêmes résultats JUnit** (sans relancer Gradle) : « 130 verts, 0 rouges, 1 en quarantaine, 41 sautés », code 0. La quarantaine est `Tour3StudioRenameIdleTest#aProjectRenamedInSimpleModeKeepsItsNewName` (connue, banc 4.5).
- `python3 -I scripts/bench/verifie-chantier.py --resume <lot>/selftest-resume-rejoue.txt` : « chantier : 24 écarts », code 1 (détail ci-dessous). Aucun garde « pas vert ».

## Ce qui reste (écarts honnêtes, règle non adoucie)

1. **11 lignes « banc » sans preuve hors banc** : T4-19, T4-16, T4-17, T5-Dc-4, T5-Fb-73, T5-Ac-61, T5-Db-16, T5-Fc-8, T5-Cc-41b, T5-Ab-14, T5-Ab-27. Leur seule preuve est un test `Bench*` ou un essai ✅ ; il faut un test sans le banc pour chacune.
2. **3 lignes « appli » sans preuve** : T4-01 (tel-12 est un ✅ partiel), T5-Fc-31 (lot 2, A.5), T5-TEL10 (essai de contrôle ✅, comportement voulu).
3. **3 paires historiques** dont le rouge et le vert ne nomment pas le même test : 8f36fa3/0b1cbbc, df794a9/dcc3367, d503254/87b6693 (6 écarts ; l'historique ne se réécrit pas).
4. **4 lignes cochées du journal sans `preuve:`** : 4.0, 4.2, 4.3, 4.0 (b).
5. **Non fait** : le selftest complet n'a pas été relancé après 3dab50a (règle : une seule fois) ; il n'y a donc pas de tampon vert pour ce lot, seulement le verdict rejoué. « Un `@KnownDefect` rendu vert à la main met le selftest au rouge » n'a pas été rejoué dans Gradle : c'est prouvé par la liste attendue (la classe est jouée) et par `KnownDefectRule`, qui échoue quand le défaut ne se reproduit plus.
6. Le juge dans le selftest ne bloque pas le lot (voir `DECISIONS.md`) : à durcir quand le compteur sera à zéro.
