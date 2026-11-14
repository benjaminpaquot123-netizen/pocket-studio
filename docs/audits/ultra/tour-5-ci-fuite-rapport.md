# Tour 5, réparation « CI : fuite d'état » : rapport de l'auteur (2026-11-10)

Atelier `eq-tour-5-ci-fuite`, branche `eq/tour-5-ci-fuite`, au-dessus de c35594a. Un seul auteur, rien n'est poussé. Aucune ligne de l'appli (`app/src/main`) n'est changée.

## Le rouge

Tâche CI « Build, unit tests, screenshots », passages 42728646876 (a683872) et 42748085657 (c35594a) : les trois mêmes tests rouges sur 8071 puis 8086, tous à `BenchPristine.kt:54` (le détecteur de fuite) :

- `BenchAmeneesProfilesTest > theAmeneeIsReady[premier-morceau … mode=pro main=gauche]`
- `BenchEngineTest > thePlayHeadMovesWithTheTempo`
- `RetoursClearOfControlsTest > phonePro`

Deux passages, les trois mêmes tests : ce n'est pas une affaire de temps.

## La cause

Le détecteur (`BenchPristineRule`, lot « mémoire ») exige un état propre avant chaque test du banc. Il n'avait jamais tourné dans la suite complète : le selftest ne joue que les tests du banc. Dans la suite complète, les tests ordinaires de l'appli passent dans les mêmes bacs à sable Robolectric, et ils laissent leur état derrière eux. Le premier test du banc qui passe après eux est rouge. Les trois rouges sont les trois « premiers du banc » : le premier de `human/` dans le bac à sable graphique natif, le premier de `human/` dans l'autre bac à sable, et celui de `retours/`.

Le détecteur a donc raison. Les coupables sont des tests sans la règle, comme la relecture de banc45 l'avait prévu (« c'est la victime qui est rouge, pas le coupable »).

## Les preuves

1. **Sonde sur la suite complète** (non commitée : une ligne de journal dans `StrictTestApplication.beforeTest`, qui lit `BenchPristine.leaks()` avant chaque test Robolectric). Passage arrêté au bout de vingt minutes d'essais par le garde de `gradle-file.sh` (aucune sortie pendant 20 min : la suite verte n'écrit rien), donc sur une partie de la suite seulement : 620 débuts de test sur un état sale, laissés par 59 classes. Détail : la fabrique de ViewModel qui garde l'`Application` du test d'avant (603 fois, tout test qui ouvre un écran), une sauvegarde de fermeture encore en cours (31 fois, `GuideAloneTest`, `SimpleMixTest`, `Kit808Test`…), les réglages changés (`ShellFixesTest`, `OnboardingUiTest`), `I18n.everyday` (`OnboardingUiTest`), `LaunchRule.askAtStartForTest` (`SpacesRestoreTest`). Aucun fil de travail encore occupé.
2. **Reproduction ciblée**, sur le commit rouge 95194d5 : `ForegroundSoundTest`, `ShellFixesTest`, `OnboardingUiTest`, `GuideAloneTest`, `SimpleMixTest` avec `BenchEngineTest` et `RetoursClearOfControlsTest` dans un seul Gradle. Résultat : deux des trois rouges de la CI, avec leur message.
   - `BenchEngineTest#thePlayHeadMovesWithTheTempo` : « left by the test before (none in this JVM), or by a test without the rule run in between: SettingsStore.state = … guideSeen=true … simpleMode=false; AndroidViewModelFactory._instance ».
   - `RetoursClearOfControlsTest#phonePro` : « … SettingsStore.state …; I18n.everyday = true; ClosingSaves busy = 3; AndroidViewModelFactory._instance ».
3. **Une deuxième fuite, d'un test du banc cette fois**, vue par la sonde : `Tour5AppProofTest#myExportsListsTheProjectZipJustSent` laisse la fabrique de ViewModel depuis qu'il compose le vrai « Envoyer le projet » (lot « faux moteur », dddbb0d). Dans l'ordre de cette machine, `Tour5AppProof2Test` passe juste après et devient rouge (commit rouge 4d316b3). Dans l'ordre de la CI, aucun test du banc ne le suit : la CI ne le montre pas encore.

## La réparation

- **Tests ordinaires** (rouge 95194d5, vert ef635a1, `OrdinaryTestsLeaveNothingTest#b_theTestAfterAnOrdinaryTestStartsPristine`). L'appli telle que tout test Robolectric la lance (`StrictTestApplication.afterTest`) attend les sauvegardes de fermeture puis remet l'état, avec la liste du détecteur (`BenchPristine.restore()`), après chaque test **qui ne porte pas la règle**. Un test qui porte la règle n'est pas nettoyé : il répond de ce qu'il laisse, et le détecteur garde toutes ses dents. Le détecteur lui-même n'est pas touché (ni liste, ni seuil, ni reprise).
- **`Tour5AppProofTest`** (rouge 4d316b3, vert a488ba6, `Tour5AppProofTest#myExportsListsTheProjectZipJustSent`). Son `@After` remet la fabrique de ViewModel, comme `HumanTester.end()` et `BenchShareTest.end()`. Une règle de la classe dit ce que chaque preuve laisse, une fois son `@After` passé : le dernier test de la classe est suivi d'un test de l'appli, que le détecteur ne regarde pas.
- **CI** (5c2e9df). Quand la tâche de build échoue, `app/build/reports/tests/testDebugUnitTest` et `app/build/test-results/testDebugUnitTest` sont gardés dans l'artefact « unit-test-reports » : le prochain rouge se lira avec son message. Vérifié sur les résultats d'ici : rapports de tests seulement (noms, durées, messages, sorties des tests), aucune propriété système, aucun mot de passe, aucun projet.

Coût : les classes qui laissent des sauvegardes de fermeture attendent maintenant leur fin (`SimpleMixTest` : 8,6 s avant, 12,7 s après).

## Ce qui a tourné

Tout par `bash scripts/bench/gradle-file.sh`, aucun APK.

| Passage | Classes | Résultat |
|---|---|---|
| Sonde, suite complète (non commitée) | toutes, arrêté à 20 min | 620 lignes de fuite, 59 classes |
| Rouge 95194d5 | `OrdinaryTestsLeaveNothingTest`, `GuideAloneTest`, `SimpleMixTest`, `ShellFixesTest`, `OnboardingUiTest`, `ForegroundSoundTest`, `BenchEngineTest`, `RetoursClearOfControlsTest` | 52 tests, 8 rouges : la garde, `BenchEngineTest#thePlayHeadMovesWithTheTempo`, les six de `RetoursClearOfControlsTest` |
| Vert ef635a1 | les mêmes, plus `BenchPristineRuleTest`, `SpacesRestoreTest`, `Tour5AppProofTest` | verts (hors la première garde de T5-ZIP, abandonnée, voir plus bas) |
| Rouge 4d316b3 | `Tour5AppProofTest`, `Tour5AppProof2Test` | 8 tests, 2 rouges : la preuve de T5-ZIP et sa victime `Tour5AppProof2Test#undoingALiveTakeGivesBackTheSongItReplaced` |
| Vert a488ba6 | les deux gardes, les classes ci-dessus, plus `BenchAmeneesProfilesTest`, `BenchShareTest`, `Tour3AppProofTest`, `Tour4AppProofTest`, `BenchSongClockTest` | 17 classes, 0 rouge |
| **Suite complète, sur la tête** (`:app:testDebugUnitTest`, `PS_STALL_MIN=70`) | 475 classes | **8088 tests, 0 rouge**, 4479 sautés, 29 min 24 s (la CI : 8086 tests, 3 rouges, 30 min) |
| `python3 -I scripts/bench/test_ultra_bench.py` | | « ultra bench scripts: OK » |

Le selftest complet du banc (`scripts/bench/selftest.sh`) n'a pas été rejoué : ses classes sont dans la suite complète.

À rejouer à la fusion : `OrdinaryTestsLeaveNothingTest`, `Tour5AppProofTest`, `Tour5AppProof2Test`, `BenchPristineRuleTest`, `BenchEngineTest`, `BenchAmeneesProfilesTest`, `RetoursClearOfControlsTest`, avec `ShellFixesTest`, `ForegroundSoundTest`, `OnboardingUiTest` et `SimpleMixTest` dans le même Gradle (ce sont eux qui salissent).

Pour lancer la suite complète par `gradle-file.sh`, il faut `PS_STALL_MIN=70` : sinon le garde la tue à 20 minutes.

## Ce qui reste incertain

- **Un ordre caché dans `Tour5AppProofTest`**, trouvé en essayant une première garde (non gardée) : quand la preuve de T5-ZIP passe avant `noChangedWarningRightAfterSharing` dans le même bac à sable, ce dernier échoue (« Android's share menu never opened for the song », 30 s), même une fois la fabrique remise. Ce que la preuve de T5-ZIP laisse là n'est pas dans la liste du détecteur, et je ne l'ai pas trouvé. Dans la classe, JUnit joue toujours la preuve de T5-ZIP en dernier : la suite ne le montre pas. À regarder dans un lot à part.
- `OrdinaryTestsLeaveNothingTest` n'est pas dans le selftest du banc (son nom ne commence pas par `Bench`) : il tourne dans la suite complète de la CI.
- « Emulator smoke test » est rouge aussi (« the take's notes: [] »), et l'était déjà sur c55ffc1 : hors de cette réparation.
- Le correctif n'a pas tourné sur la CI (rien n'est poussé).
