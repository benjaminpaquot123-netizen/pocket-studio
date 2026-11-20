# Relecture neuve — lot tour-9 « ci-rapide » (c79d3442..b800b89c)
Relu : b800b89ca96002e006876019a6e0dda97741834c

Périmètre : 4 commits, outils de la CI seulement (`ci.yml`, `app/build.gradle.kts` bloc des tests unitaires,
`scripts/screenshot-diff.py`, `scripts/bench/ci-rouge.sh`), leurs tests et deux documents. Rien dans `app/src/main` ni
`project-model/src/main`. Aucun test désactivé, aucun `@Ignore`. Pas d'identifiant de modèle dans le code ni les
commentaires ; les messages de commit ne portent que les lignes d'attribution. Pas de Gradle lancé (consigne).
Lancés ici : `test_stable.py` (6 OK), `test_socle.py` (30 OK), `test_portail3.py Batch` (35 OK).

rouge constaté : d977d18b : rejoué sur b76bc637 : `test_stable.py` de b76bc637 avec son `ci-rouge.sh`, le test neuf rougit pour la bonne raison (« rang : classe 3/4 », « avant : com.x.AClass > com.x.BClass » : les deux machines mélangées) ; vert sur la tête.

## 1. Perte ou double passage d'une classe
- La règle (`app/build.gradle.kts:147-149`) est une partition : chaque fichier `.class` a un seul reste de
  `floorMod(hash, n)`, les machines 1..n couvrent tous les restes. `Foo$Bar` est haché sur `Foo` (même machine). Les
  dossiers ne sont jamais exclus (`!el.isDirectory`). La liste `part: [1, 2, 3, 4]` et `TEST_PARTS: "4"` sont tenues
  ensemble par `test_socle.py`. `:project-model:test` seulement sur la machine 1 (expression `matrix.part == 1 && … || ''`
  correcte). Sans `-Pshard`, le bloc ne fait rien (bancs, `-Pshuffle`, `-Phuman.dir`, lancements locaux inchangés).
- Cache : `inputs.property("shard", spec)` plus les fichiers candidats filtrés (entrée de la tâche Test) : un résultat
  de la part 1 ne peut pas servir à la part 2.
- Répartition estimée ici en Python (même `String.hashCode`, classes `…Test` lues dans les sources, n = 4) : 186 / 187 /
  193 / 183 classes, 822 à 901 `@Test` par part. Équilibré par le nombre, pas par la durée (écrit dans les décisions).

## 2. Le travail qui rassemble
- `ci.yml:213-214,249-252` : `needs: [quick, tests]`, `if: !cancelled()`, vert seulement si `needs.tests.result` vaut
  `success` (résultat agrégé de la matrice : une part rouge, annulée, hors délai ou sautée le rend rouge ; `quick` rouge
  → parts sautées → rouge). `publish` (`ci.yml:499-501`, condition sans fonction d'état donc `success()` implicite)
  attend toujours `build`. Pas de vert GitHub possible avec une part rouge.
- Disposition des artefacts : `pattern:` sans `merge-multiple` → un sous-dossier par nom d'artefact
  (`parts/screenshots/screenshots-diff-part-k/…`, racine commune `app/build/` dedans), ce que cherchent le `find` de
  `ci.yml:235` et le motif `(?:^|-)part-(\d+)$` de `ci-rouge.sh`. `ci-rouge.sh <run>` télécharge `unit-test-reports`,
  qui contient `unit-test-reports-part-k/test-results/testDebugUnitTest/TEST-*.xml` : lu et classé par machine.
- `paquet-verifie.sh:167` exige des chiffres après « Captures : » : le commentaire du script de l'étape, recopié en tête
  du journal, ne peut pas être pris pour la ligne.

## 3. Constats
- **moyen** `app/src/test/java/com/pocketstudio/app/CiShardTest.kt:39-45` (`aNestedClassRunsWithItsOuterClass`) : test
  creux. `part()` (ligne 16) coupe au `$` avant de hacher, donc une classe imbriquée a toujours la part de sa classe
  externe : la liste `strays` est vide par construction, le test ne peut pas rougir. Ce qui tient vraiment la propriété,
  c'est `theBuildScriptSplitsByThisRule` (texte du script identique à `part()`). À retirer ou à remplacer par un test
  qui lit le comportement de Gradle ; le rapport le cite pourtant comme preuve du « chemin » « une classe imbriquée suit sa
  classe ».
- **moyen** `ci.yml:172-184` (« What this part ran ») : la ligne « Tests, partie k/4 » n'est qu'imprimée, aucune part
  ne rougit à 0 test. Scénario : si la règle d'exclusion se comportait mal pour une part (classes toutes exclues → tâche
  Test « NO-SOURCE », verte), cette part passerait en vert sans rien tester ; si son résumé Roborazzi existait avec 0
  capture, la somme des quatre resterait > 0 et `paquet-verifie.sh` dirait « captures à jour ». Improbable (même code
  pour toutes les parts, répartition équilibrée), mais rien ne le garde : faire échouer l'étape quand `tests == 0`.
- **mineur** `ci.yml:221,226` + `233-239` : un téléchargement raté (`continue-on-error`) laisse le travail « build » vert
  sur GitHub sans ligne « Captures : » ; seul `paquet-verifie.sh` le lit « captures non lues ». Voulu et écrit dans les
  décisions, mais la coche verte de GitHub ne suffit donc pas à elle seule.
- **mineur** `ci.yml:235` : `find … | head -n 1` prend le premier `results-summary.json` trouvé dans la part ; s'il en
  existait deux (rapport et résultats), l'ordre du système de fichiers choisit. Sans effet si ce sont les mêmes chiffres.
- **mineur** `ci.yml:116-119` : le commentaire « ci3 » dit encore que « build », « sound » et « apk » ont « the same
  steps » ; « build » ne fait plus que rassembler.
- **mineur** messages de commit : l'attribution nomme un autre modèle que la ligne imposée à cette session ; la règle
  « seulement les lignes d'attribution » est tenue, la cohérence avec la consigne de la discussion du lot n'est pas
  vérifiable ici.

## 4. Non vérifié (sans Gradle ni CI finie)
- Aucune CI finie sur la tête : CI 44590027560 (d977d18b) en cours, CI 44590947483 (e88861b8) en attente au moment de la
  relecture ; b800b89c ne diffère de e88861b8 que par `docs/`. Donc pas vu : la règle `Spec` sous le cache de
  configuration (`org.gradle.configuration-cache=true`), le résumé Roborazzi de chaque part (un quart des captures,
  sommes égales au total d'avant, 1014), la durée réelle par part face au délai de 45 min.
- `CiShardTest` n'a pas tourné ici (compilation lue : chaînes `'\$'` et regex brute `"""…"(\d+)""""` correctes ; dossier
  de travail du module `app/` comme les autres tests qui lisent `../`).
- Relance des seuls travaux en échec : artefacts d'une tentative précédente lus par `pattern:` et nom
  `unit-test-reports` / `screenshots-diff` déjà présent ; comportement de `download-artifact` v7 non lu.
- Rapport et décisions : rien de plus fort que la preuve (les durées « après » renvoyées à la CI, « Ce qui n'est pas
  vert » honnête), hormis le chemin « une classe imbriquée suit sa classe » adossé au test creux ci-dessus.

Verdict : 0 bloquant, 0 grave

## Relecture neuve du complément (cache Robolectric, annulation des CI de lot)
Relu : 375161e3 (`.github/workflows/ci.yml`, `scripts/ci-robolectric-jars.sh`), et la fusion 67965245 pour ci.yml seul.

### Bloquant
- Aucun.

### Grave
- Aucun.

### Mineur
- `scripts/ci-robolectric-jars.sh:54-55` : curl sans `--max-time` ni `--speed-limit/--speed-time`. Une connexion
  ouverte qui ne débite plus bloque l'étape jusqu'au délai du travail (45 min) au lieu de réessayer. Robolectric seul
  avait le même défaut, donc ce n'est pas un recul ; ajouter par ex. `--speed-limit 10000 --speed-time 60`.
- `scripts/ci-robolectric-jars.sh:88` : après les essais, `exit 1` rougit la part sans laisser Robolectric tenter
  lui-même (pas de `continue-on-error`). Voulu (échec en 2 min au lieu de 533 tests rouges), mais ce n'est pas le
  « repli silencieux » : ce repli ne vaut que pour une version absente de la table (vérifié, voir plus bas).
- `ci.yml:19` : le nom de l'intégration `ccr-80af1a1b-mn0wec` est écrit en dur, en double de `PS_INTEGRATION`
  (`scripts/bench/portail-file.sh:23`). Si l'intégration change de branche (nom en `ccr-`), sa CI devient annulable
  tant que ci.yml n'est pas mis à jour.
- `ci.yml:19` : `on.push` ne vise que `main`, donc la clause `eq/`/`ccr-` ne joue que pour les CI lancées à la main
  (workflow_dispatch). Une CI de lot annulée se lit « rouge » dans `scripts/bench/ci-etat.sh` (completed/cancelled)
  pour l'ancien sha seulement ; sans effet sur la tête poussée.
- `ci.yml:177` : clé avec le hachage de tout `libs.versions.toml` (toute montée de dépendance = nouvelle entrée de
  ~213 Mo) et cache rangé par branche : une branche de lot ne relit que son propre cache ou celui de `main`, donc
  la première CI de chaque lot télécharge quand même (avec les essais du script, c'est l'essentiel du gain).
  Les 4 parts qui sauvent la même clé : la première gagne, les autres n'ont qu'un avertissement (pas de rouge).

### Vérifié (sans défaut)
- Fichier partiel : téléchargement dans `.part.$$` puis `.new`, déplacé dans `~/.m2` seulement après contrôle sha512 ;
  le jar n'est jamais en place sans être vérifié ; rien ne reste après un échec (essai avec un dépôt injoignable).
- Clé : change avec la version de Robolectric (nom de l'artefact + hachage du toml) et avec le sdk ; version inconnue
  → nom vide, rien écrit, code 0 (essayé avec `4.99`), Robolectric télécharge et le cache garde ce qu'il a pris.
- `set -euo pipefail` : les `[ … ] && …` ne font pas sortir ; tableau vide sous `set -u` sans erreur (bash 5).
- Concurrence : `main` et l'intégration jamais annulées (push sur main : `ref_name`=`main` → faux) ; pull_request
  inchangé.
- Actions : `actions/cache` épinglée par sha (même sha que le travail « smoke »). Fusion 67965245 : ci.yml identique
  au côté du lot (a4002a4a), l'intégration n'y avait rien changé ; YAML lu, étapes du travail « tests » sans doublon
  ni perte.

### Ce qui a tourné
- Script réel dans un dossier temporaire : jar de 213 Mo et pom récupérés, sha512 égal à celui de Central, même
  disposition que le `~/.m2` écrit par Robolectric ; second passage : rien à faire (0,01 s). `--names` → 
  `16-robolectric-13921718-i7`.
- `test_portail3.py` (80 verts), `test_stable.py` (6 verts), `test_socle.py` : 29 verts, 1 rouge
  `PortalRulesFrozenPerBatch.test_a_rule_added_during_the_batch_waits_for_the_next`, **déjà rouge sur 9c466fd1**
  (worktree, 28 verts 1 rouge) ; cause : la règle `elif from_paquet 4` ajoutée par 19e40b8f (19/11 19:16) après
  l'ouverture du paquet 9 (b746a9aa, 18:33). En clone de profondeur 1 (comme `actions/checkout`), 30 verts : la CI ne le
  voit pas.

### Non vérifié
- Aucune CI GitHub de 375161e3 lue : restauration/sauvegarde réelle du cache, durée, avertissement de course.
- `CiShardTest` non lancé (il ne lit que `TEST_PARTS`, inchangé).
- Que Robolectric 4.17 ne revérifie pas le jar local (lu de mémoire : il se fie à la présence du jar) ; sans conséquence,
  le jar et son `.sha512` sont cohérents.

Verdict : 0 bloquant, 0 grave
