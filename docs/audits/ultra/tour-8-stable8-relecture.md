# Relecture lot stable8 (tour 8)
Relu : d5801992edce0d3458828803382826fde645f0ec

Base 91ae5964, commits relus : d7edc082 (rouge), 855cb50a (vert), b4c38e35, d445d9bf, d5801992 (docs). Lecture seule, Gradle non relancé.

## Ce qui est vérifié et tient
- Fichiers touchés : `OrdinaryTestsLeaveNothingTest.kt` seul côté code (dans la liste) ; `StrictTestApplication.kt` non touché ; rien dans `app/src/main` ; docs : rapport et `docs/decisions/tour-8-stable8.md` (convention des lots déjà en place). Pas de quarantaine, pas de `@Ignore`.
- Cause : cohérente avec le code. `BenchPristine.afterAnOrdinaryTest` → `appWorkEnded` ne boucle que tant que `appThreads()` (fils `DefaultDispatcher-worker` avec du code `pocketstudio` dans la pile) ou des coroutines du fil principal existent. Un travail en file, pas encore démarré, n'apparaît nulle part : la boucle rend `true` tout de suite, rien n'est levé, `e == null` → « red itself, by name: null ». Exactement le message vu sous charge.
- Le correctif traite la cause du test, ne la masque pas : le test dit « laisse du travail EN COURS » ; avec le verrou, il en laisse vraiment avant d'appeler `afterTest`. `countDown()` est dans `invokeSuspend` du lambda, donc la pile du worker contient déjà une frame `OrdinaryTestsLeaveNothingTest…` quand le verrou s'ouvre : `afterTest` la voit.
- Assertion non relâchée : « red itself, by name » garde la même forme ; pour `c_`, `name` vaut mot pour mot l'ancien littéral ; « naming the work » inchangée. `a_`, `b_`, `d_`, `e_` : aucune ligne changée (diff vérifié).
- Le test neuf rougit sans le correctif (raisonnement sur d7edc082) : avec `lateDefault(300)`, le bloc attend 300 ms sur un `Thread` nommé `Thread-N` (pas `DefaultDispatcher-worker`, donc invisible pour `appThreads`) ; `closingSaves()` + `restore()` sans charge rendent la main bien avant (rien en attente) ; `appWorkEnded` ne voit rien → `e == null` → rouge avec le même message. À 855cb50a, l'attente du verrou absorbe les 300 ms → vert.
- Fuites : le `Thread` de `lateDefault` dort 300 ms, envoie le bloc, se termine. Si `await` échoue (60 s), le `finally` met `stop` à vrai : la coroutine, même démarrée plus tard, sort de sa boucle au premier tour. Le travail ne survit pas au test.
- Rapport et décisions : conformes au diff (un seul fichier de code, 6 tests, rouge 1 échec / vert 0, assertions inchangées) ; la boucle 20/20 n'est pas vérifiable par lecture, elle est décrite avec ses écarts (passage 1 en partie sans charge, passage 11 rejoué).

## Constats

### Mineur 1 (dans la fiche) : `@OptIn` déplacé sur la mauvaise déclaration
À 855cb50a, `@OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)` est resté au-dessus de `val running = CountDownLatch(1)` (ligne insérée entre l'annotation et l'appel). Il annote donc la variable du verrou, plus l'appel `GlobalScope.launch`. `DelicateCoroutinesApi` est au niveau WARNING et aucun `allWarningsAsErrors` n'est posé : ça compile, avec un avertissement d'opt-in neuf. Correction : remonter `val running` au-dessus de l'annotation.

### Mineur 2 (dans la fiche) : après le correctif, le test neuf ne garde que le test, pas le contrôle
Une fois le verrou attendu, `c_…StartedLate` et `c_…Running` font la même chose (les 300 ms sont absorbées avant `afterTest`). Le test neuf prouve qu'on n'enlèvera pas l'attente sans rougir, rien de plus sur `BenchPristine`. C'est conforme à la fiche (le défaut est celui du test), mais à dire tel quel : le trou du contrôle reste ouvert (voir mineur 5).

### Mineur 3 (dans la fiche) : la preuve rouge dépend de la vitesse de `restore()`
Le rouge à d7edc082 suppose que `closingSaves()` + `restore()` (dont `TakeVault.awaitIdleBlocking`, suppression du dossier `projects`) durent moins de 300 ms. Vrai sans charge en pratique (et mesuré par l'auteur), mais une machine lente pourrait rendre ce rouge vert. Sans effet sur la tête (le vert ne dépend pas de ce délai).

### Mineur 4 (dans la fiche) : 10 s de plus par passage
Chaque `c_` attend `WORK_END_CAP_MS` = 10 s avant que `afterTest` lève : deux `c_` = 20 s au lieu de 10. Acceptable, à savoir pour le temps de la suite.

### Mineur 5 (NEUF, hors fiche, déjà signalé au chef par l'auteur) : `BenchPristine.appWorkEnded` ne voit pas le travail en file
Un vrai test ordinaire qui laisse du travail sur `Dispatchers.Default` dont le worker démarre en retard passe inaperçu sous charge ; le travail est alors vu par le test SUIVANT, rouge à la place du coupable (le défaut que ce contrôle devait éviter). Fichier hors liste (`BenchPristine.kt`) : à ouvrir comme lot à part (par ex. un court délai de grâce ou la file du `Dispatchers.Default` regardée), pas dans ce lot.

### Mineur 6 (dans la fiche, procédure) : boucle 20/20 hors `gradle-file.sh`
La fiche demande `gradle-file.sh` ; la boucle a tourné en `./gradlew` direct avec une copie jetable dont les gardes sont coupées. Raison écrite et plausible (la file arrête les démons de l'autre Gradle), rien de commis. Au chef de dire si cette preuve vaut la phrase d'acceptation.

Verdict : 0 bloquant, 0 grave
