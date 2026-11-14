# Relecture du lot « banc-fin » (étape 3, lot 5) par un relecteur neuf
Relu : e2fdddeeb2fabc3cdf052deb977e45f58438d336

Atelier `/home/user/eq-tour-5-banc-fin`, branche `eq/tour-5-banc-fin`, base fx-int 3dfdd0b, 9 commits (3f83167 … e2fddde). Relecteur qui n'a pas écrit ce code. Rien n'est corrigé ni poussé ; seul ce fichier est commité. Gradle uniquement par `scripts/bench/gradle-file.sh`, 7 passages ciblés ; chaque fichier touché temporairement (BenchEngine.kt, Tour3RenameIdleTest.kt, ZzRenameLoopTest.kt) remis ou retiré un par un, `git status` ensuite : seules les lignes « T engine/third_party », « T separation/third_party ».

## En bref

- Règle (c) du juge : les 3 contournements de la relecture « pads-live » sont bien refusés, l'historique réel reste à 22 écarts (identiques, ligne à ligne, à ceux du juge de la base). Mais 7 autres contournements passent encore (moyen).
- A.1 : les 6 sondes testent bien leur constat. Rejouées une mutation à la fois (et non toutes ensemble comme m1), chacune rougit seule, pour sa raison, et les autres restent vertes. Les 18 sha cités au § 3 existent et disent ce qu'on leur prête.
- Renommage : la paire rouge 6eeceba / vert d65c607 est honnête, et la cause est confirmée par un contre-essai : même boucle, règle v1 remise : 4 renommages rouges sur 18 ; règle v2 : 0 sur 18. La v2 ne masque pas de défaut de l'appli ; `app/src/main` n'est pas modifié.

## Moyen

### M1. Règle (c) : la garde « venue d'un rouge » se contente du préfixe « rouge: » ; la date de début est falsifiable ; un vert peut éteindre le test

`guard_from_red` (scripts/bench/verifie-chantier.py) accepte la garde dès que le commit qui l'a fait apparaître a un sujet commençant par « rouge: », sans vérifier que ce rouge parle de ce test, ni que l'apparition est un ajout réel (et pas un renommage, un déplacement ou un reformatage). La borne `DISCIPLINE_SINCE` compare la date de commit (`%cI`), que `GIT_COMMITTER_DATE` fixe à volonté. Le test gardé doit seulement rester une `fun` : `@Test` peut devenir `@Ignore`.

Script adverse : `/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/relecture-bancfin/judge_adv2.py` (petit dépôt construit par `judge_root` de `test_ultra_bench.py`, juge de l'atelier). Chaque cas passe par la règle (c) : le rouge qui pose la garde parle d'un autre test, et un « vert: » de cet autre test l'a déjà fermé. Sortie copiée :

```
b7a0b94 « vert: ddd2 fixed (DddProofTest#ddd2Test) » : vert sans rouge du même test avant lui
84cb277 « vert: iii fixed (IiiProofTest#iiiTest) » : vert sans rouge du même test avant lui
chantier : 2 écarts

A garde glissée dans le rouge d'une autre classe                       985d323 ACCEPTÉ comme paire
B garde posée par un rouge, levée sans toucher au code                 19dc867 ACCEPTÉ comme paire
C chore antidaté avant DISCIPLINE_SINCE                                22c5741 ACCEPTÉ comme paire
C2 témoin : chore daté d'aujourd'hui                                   b7a0b94 REFUSÉ
D blanchiment : renommage de classe dans un rouge                      8a7281e ACCEPTÉ comme paire
E blanchiment : reformatage dans un rouge                              eb45ea2 ACCEPTÉ comme paire
E2 blanchiment : fichier déplacé dans un rouge                         4e4b2ca ACCEPTÉ comme paire
F vert qui lève la garde et met @Ignore                                0152b40 ACCEPTÉ comme paire
F2 garde dans une « inner class », nommée Classe externe#méthode       523c56d ACCEPTÉ comme paire
G témoin honnête                                                       0a9b5fa ACCEPTÉ comme paire
H témoin : garde honnête, fichier déplacé par un chore                 84cb277 REFUSÉ
```

Ce qui passe à tort :
- A : une garde `@KnownDefect` glissée sur `AaaProofTest#aaaTest` dans « rouge: other thing fails (BbbProofTest#bbbRedCase) ».
- B : un rouge qui pose une garde sur un test qui passait. KnownDefectRule le fait rougir par « no longer fails », c'est donc un vrai rouge, mais pas celui d'un défaut. Le vert qui retire la garde ne touche que le fichier de test.
- C : une garde posée par un « chore: », dont le commit est daté du 2026-11-01.
- D, E et E2 : une garde posée par un « chore: », puis « blanchie » par un rouge d'un autre test. Ce rouge renomme la classe (D), reformate `@KnownDefect(id = …)`, que la regex ne voyait pas, en `@KnownDefect("…")` (E), ou déplace le fichier avec `git mv` (E2).
- F : le vert retire la garde et remplace `@Test` par `@Ignore`.

Tout cela demande une tricherie délibérée. L'historique réel n'en contient pas (0d049be et 2e6e828 touchent `RhythmViewModel.kt` et leurs gardes viennent de 4168889). D'où « moyen » et non « grave ».

Pistes (une ligne chacune, pas faites) :
- exiger `names(message du rouge, test)` ;
- remplacer la date par `git merge-base --is-ancestor <commit de la garde> 8f36fa3` (4168889 en est bien l'ancêtre : vérifié) ;
- exiger que le vert modifie au moins un fichier hors `app/src/test` (vrai pour 0d049be et 2e6e828) et que la `fun` gardée porte encore `@Test`.

Vérifié aussi :
- `python3 -I scripts/bench/test_ultra_bench.py` → « ultra bench scripts: OK » ;
- `verifie-chantier.py --resume <lot-pads-live-final>/selftest-resume.txt` → « chantier : 22 écarts », et la même chose avec le résumé du selftest de ce lot ;
- le juge de la base (3dfdd0b) sur le même arbre donne aussi 22 écarts, avec les mêmes lignes une fois les sha ôtés (`diff` vide).

## Mineur

- m1. Faux négatif conservateur (cas H) : `git log -- <chemin>` ne suit pas les renommages. Une garde honnête dont le fichier a été déplacé par un « chore: » ne compte plus (« vert sans rouge »). Gênant un jour, pas dangereux.
- m2. `CLASS` ne reconnaît ni `inner`, ni `sealed`, ni `enum`, ni `object`/`companion object`. Une garde dans `inner class Hidden` est rangée sous la classe externe (cas F2 : `JjjProofTest#jjjTest`). Cela touche le nom affiché, pas la preuve.
- m3. A.1, rejeu m1 : les mutations F1, F2, F4 et F5 sont posées ensemble, donc m1 seul ne prouve pas que chaque sonde rougit pour son constat. Je les ai rejouées une à une (`mut1.py` et `run-all.sh` dans le même scratchpad `relecture-bancfin/`, `AuditFideliteTmpTest` seul à chaque fois). Résultats copiés :
  ```
  F1  : a1 FAIL « the take never reached its track (recording null, notice null) » ; a2 a3 b1 b2 b3 ok
  F2  : b3 FAIL « one curve, made by the gesture expected:<1> but was:<0> » ; a3 FAIL « … step 8 expected:<8> but was:<-1> » ; a1 a2 b1 b2 ok
  F4  : b1 FAIL « no jump back at 60 BPM expected:<16> but was:<0> » ; b2 FAIL « counting in: no transport step expected:<-1> but was:<8> » ; a1 a2 a3 b3 ok
  F5  : a2 FAIL « the third part is at its step 8 expected:<8> but was:<40> » ; les 5 autres ok
  F5b : (currentStep() = absoluteStep(), position dans la partie et non pas du motif batterie, mutation à moi) a3 FAIL « the drum grid's head is on its step 8 expected:<8> but was:<40> » ; les 5 autres ok
  ```
  Chaque sonde voit donc son constat, et lui seul. Les lignes du § 3 sont exactes. Celle de F5 ne cite que `a2`, alors que `a3` voit aussi la seconde moitié de F5 (F5b ci-dessus) ; on pourrait l'ajouter. Sous F1, a1 rougit par l'attente de 90 s (90,076 s) : le rouge est juste, mais lent.
- m4. A.1, § 3 : les 18 sha cités existent et leurs sujets correspondent (49c3325/2e650ca A.7, c6e9f7e/c9bb80d A.8, 0fcb63d/419a5c4 A.9, e3db5e2/7bfe58b A.15, ec9cc11/dddbb0d A.13, f3c283e et 2e6e828 partage, bc8bb91 et f59af32/6c2f776 A.16, fusions b87717a, 4437ccc, 0911520) ; tous sont ancêtres de 3dfdd0b. Les preuves sont rangées sous `docs/audits/ultra/audit-banc/a1-2026-11-10/{propre,m1,m2}/` : XML JUnit plus `resume.txt`, et `mutation.diff` pour m1 et m2. Le diff m1 ne contient que les lignes annoncées. Les XML sont rangés à plat, alors que le script les écrit sous `<dossier>/junit/` : sans effet.
- m5. Renommage : la garde `Tour3RenameEffectsIdleTest` vérifie la règle de test (sur quel fil reprend le collecteur), pas la perte de recomposition elle-même. Son rouge est déterministe : une fois la règle v1 remise, il rougit encore avec « written on the main thread (SDK 36 Main Thread @kotlinx.coroutines.test runner#8376), as a phone does: [io-emitter] ». Le lien de cause à effet tient par l'expérience, que j'ai refaite (voir « Renommage »). Les 180 autres classes en règle v1 gardent le même risque, comme le rapport le dit.

## Renommage : contrôles faits

- Boucle `ZzRenameLoopTest` (16 renommages paramétrés, copiée puis retirée) après `Bench*`, avec les deux renommages et la garde, en un seul passage Gradle chacun :
  - tête (v2) : « BUILD SUCCESSFUL in 5m 22s », 147 ok, 0 échec, 18 renommages verts ;
  - règle v1 remise (une ligne de `Tour3RenameIdleBase.composeRule()`, puis restaurée) : « 188 tests completed, 5 failed », « BUILD FAILED in 6m 58s ». Échouent la garde et 4 renommages sur 18 : `Tour3StudioRenameIdleTest`, puis `ZzRenameLoopTest` [2 Studio], [5 Night] et [12 Studio], chacun avec « IllegalStateException: no project's ⋮ in Mes projets; projects: loading=false n=1 busy=false message=null … » (22,2 s, le symptôme de la CI). Les `Bench*` sont tous verts.
  Le rouge rougit donc pour la cause annoncée, et la v2 le fait disparaître dans les mêmes conditions.
- Défaut masqué sur téléphone ? Non. `AppRoot.kt` l.825 et l.1595 collectent avec `projectsVm.state.collectAsStateWithLifecycle()`, sans contexte : les effets tournent alors sur le répartiteur de la composition (fil principal sur un téléphone). `ProjectsViewModel` émet un `MutableStateFlow` depuis `Dispatchers.IO`, ce qui est sûr entre fils. `grep` de `Dispatchers.Unconfined`, de `collectAsState…(context = …)` et de `context = Dispatchers` dans `app/src/main` : rien.
- `git diff 3dfdd0b..e2fddde -- app/src/main | wc -l` → 0. Hors `docs/`, `scripts/bench/` et `app/src/test/` : aucun fichier touché. Aucun lien symbolique (`grep -c '^120000'` → 0), aucun identifiant de modèle dans les messages.

## Bloquant, grave

Aucun.

Verdict : 0 bloquant, 0 grave — fusion possible
