# Relecture neuve — lot tour-9-L9 (eq/tour-9-L9, 7455749a5..b350c86d)

relu : 37c5817544dbfdf4d50f94912c23d478af75f799

Périmètre : 12 commits, uniquement `app/src/test/**`, `scripts/bench/**` et un compte rendu dans `docs/`. Rien dans
`app/src/main` ni `project-model/src/main`, aucun lien symbolique, pas d'identifiant de modèle dans les messages (seule la
ligne d'attribution imposée). Aucun test désactivé ni `@Ignore`. Pas de Gradle lancé (consigne) ; lancés :
`test_portail4.py CutHistory RealLots` (vert, RealLots sauté : clone court) et `ci_state_without_gh` (vert).

## 1. D17 — kit du tuto téléchargé pendant une attente
- Le « rouge » tient : `BenchTutoKitDownloadTest` (vrai FLAC fait par `FlacEncoder`, posé 300 ms dans « attends 3 » sur
  l'horloge du looper) échoue sans le crochet, puisque `BenchFlac.register` n'était appelé qu'avant/après un geste. Le
  « vert » branche le crochet existant `tutoKitDecodeForTest` (main inchangé) sur `BenchFlac.readable` : même chemin
  `AudioDecoder.decode` que l'appli, seulement précédé d'un `register()`. `@Synchronized` couvre l'appel hors thread
  principal. Crochets remis à zéro dans `@After` (`TutoTestHooks.packs`). Correct.
- **mineur** `app/src/test/java/com/pocketstudio/app/tuto/T9D17KitDisappearsTest.kt:139-141` : le commentaire dit « A real
  (valid) FLAC », mais `scripts/bench/oracles-niveaux/.../Trois.flac` fait 4 octets (« fLaC » seul). Le cas (b) échoue donc
  sur un fichier invalide, pas sur « MediaCodec sans codec » comme l'écrivent le test et `tour-9-verif-D17.md`. La
  conclusion « c'est le banc » reste prouvée par `BenchTutoKitDownloadTest`, mais la preuve sans banc est plus faible
  qu'annoncée. Utiliser un FLAC fait par `FlacEncoder`, comme l'autre test.
- **mineur** même fichier, `b_…:166-170` : le test fige comme attendu le repli silencieux (`assertFalse("a message shown", …)`),
  que la remarque du compte rendu propose justement de corriger. Le jour où la finition « Kit indisponible, kit Studio
  gardé » entre, ce test rougira pour une bonne raison. À noter dans le tri de la finition.

## 2. Tests creux : Tour8PlusRowsFitTest, Tour8GuideNoEmptyStepTest
- Plus : le `return` qui sautait tout est parti ; chaque cas nomme sa disposition attendue (`assertEquals(listOf(layout), shown)`),
  six cas « plein » / limites ajoutés (phone411, tablet8). Plus de test creux. Correct.
- Guide : l'écart sous le « pourquoi » est mesuré (borne haute 8 dp + marge propre), cas « réglé qui ouvre un écran » ajouté.
  Correct.
- **mineur** `Tour8PlusRowsFitTest.kt:89-93` : pour compact/liste, « aucune rangée à cheval sur le pli » n'est pas une règle
  de l'appli (une liste qui défile peut couper une rangée) ; vert aujourd'hui, mais fragile au moindre changement de
  texte. Les dispositions attendues par cas figent aussi l'heuristique actuelle.
- **mineur** pas de commit « rouge: » pour ces deux corrections (preuve faite en local, décrite dans le message). Logique
  pour un test creux sur du code juste, mais écart à la règle écrite.

## 3. OrdinaryTestsLeaveNothing — travail démarré tard (BenchPristine)
- Le nouveau cas est vraiment tardif : tous les ouvriers de `Dispatchers.Default` sont tenus par `benchhold.HeldWorker`
  (hors `com.pocketstudio`), le travail de l'appli attend dans la file quand `afterTest` regarde. Sans `queuedWork()` le
  contrôle répond « rien » → le test rougit. Le test vérifie aussi `queuedWork() > 0` : si la réflexion casse avec une
  autre version de kotlinx-coroutines, il rougit au lieu de rendre le contrôle aveugle. `finally` libère les ouvriers et
  joint le travail : pas de fuite vers le test suivant. Correct.
- **mineur** `BenchPristine.kt:305-306` : `appWorkEnded` attend aussi tant que les files contiennent du travail qui n'est pas
  de l'appli ; sur une machine saturée, chaque test ordinaire peut attendre jusqu'à `WORK_END_CAP_MS` (10 s) pour rien
  avant de conclure « pas une fuite ». Borné, mais coût possible en temps de suite à surveiller.
- **mineur** le commit « rouge » 046624bc appelle déjà le contrôle sans `queuedWork` ; il compile, et rougit pour la bonne
  raison. Rien à redire, noté pour l'historique.

## 4. ci-etat.sh sans gh
- Repli `curl` + `python3 -I` sur l'API publique ; les `null` de jq sont reproduits ; test jouet `ci_state_without_gh`
  (gh qui échoue, faux curl) : vert ici, et il rougit sans le repli. Correct.
- **mineur** `scripts/bench/ci-etat.sh:48` : sans jeton, l'API publique limite à 60 appels par heure (1 + 1 par passage).
  Si la liste des passages répond mais qu'un appel `jobs` est refusé, `|| true` donne zéro tâche et l'état devient
  « absent » au lieu de « ? » (le pré-vol peut lire « absent » pour une CI rouge). Le même trou existait avec gh, il
  devient plus probable. Mettre « ? » quand un appel `jobs` échoue.

## 5. test_portail4.py — clone court
- `CutHistory` construit un vrai clone `--depth 2` où la base manque et vérifie le `skipTest` avec « git fetch --deepen ».
  Le saut est un saut d'environnement (comme celui « fusion absente » juste au-dessus), pas un saut pour verdir : sur la
  CI (checkout profondeur 1) ces fusions sont déjà absentes et sautées avant. `ROOT` est rétabli par `addCleanup`. Correct.
- **mineur** `test_portail4.py:590` : la condition est « base vide », pas « dépôt superficiel ». Des historiques sans
  ancêtre commun sauteraient aussi en parlant de `--deepen`. Tester `git rev-parse --is-shallow-repository` en plus.

## 6. Coquille2SeatTest p5 — « Mes projets » en temps réel
- `waitWhileListLoads` : au moins 40 « attends 0.5 » ET au moins 60 s réelles tant que « Chargement de la liste… » est là,
  plafond 400 attentes. Le nouveau test retient la lecture 40 s réelles (`beforeOperationForTest`, exécuté sur le fil
  des opérations) : l'ancienne boucle (40 attentes) échoue, la nouvelle attend. Rouge réel, plus de liste qui « ne vient
  jamais » sans échec (borné). Correct.
- **mineur** `Coquille2SeatTest.kt:37` + `HumanTester.kt:1072` : `tap` appelle `waitWhileListLoads` puis `reachTag` le refait ;
  pour une liste vraiment bloquée, l'échec arrive après environ 2 × 60 s réelles. Supprimer l'appel dans `tap`.
- **mineur** `HumanTester.kt:1052-1063` : la nouvelle KDoc de `waitWhileListLoads` est insérée sous celle de `reachTag`,
  qui reste orpheline (deux blocs de doc d'affilée).
- **mineur** le nouveau test coûte au moins 40 s réelles à chaque suite (la retenue s'applique aussi aux opérations
  suivantes du même modèle).

Verdict : 0 bloquant, 0 grave

## Seconde relecture neuve (b350c86d9..37c58175, demandée par le portail)

Relancé : `selftest-attendus.py --sauts-connus` (rc 0), `test_ultra_bench.py` (OK), `test_portail4.py` (60 OK, 2 sautés : clone court). Pas de Gradle : tests ciblés verts après fusion, CI 44571785042 verte sur 37c58175.
- 3a953578 : une ligne `BenchTutoKitDownloadTest#drive` ; la classe hérite de HumanTester, `drive` est son entrée de siège. Mineur : « vert: » sans commit « rouge: », le rouge est le selftest de 4ef94d89 (une ligne de liste).
- 1c99548d : les 8 tests « chemin: » et les 6 classes « tests: » existent en `@Test` sous app/src/test ; relecture déplacée, contenu identique.
- 37c58175 (fusion de c79d3442) : `git diff c79d3442 37c58175` identique octet pour octet aux seuls changements du lot ; rien dans app/src/main ni project-model/src/main.
- Mineur : « cinq passages verts d'affilée » (Coquille2 p5) est une affirmation du lot, non rejouée ici.

Verdict : 0 bloquant, 0 grave
