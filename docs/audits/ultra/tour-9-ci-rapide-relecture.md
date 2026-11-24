# Tour 9, lot « ci-rapide » : relecture neuve

Relu : 36baa1da

Commits relus : 77a4759d, 7dd14931, e7db06ff, cd02945e, c3d8a090, 5b54498a, 36baa1da. Joués ici : `shards.py selftest` (OK) et `shards.py policy` (OK, 11 jobs, 6 lots), plus `policy` sur des copies cassées de `ci.yml` (dans le dossier de travail, rien dans le dépôt). Ni Gradle ni la suite complète.

## Graves

aucun

Ce qui a été vérifié pour arriver là :
- Une classe ne peut pas se perdre sans bruit dans le cas normal : les six lots tirent le même partage (même liste de classes, même fichier de durées, ordre stable) ; `check` refuse un lot sans résultat, deux lots qui n'ont pas vu les mêmes classes, une classe dans aucun lot ou dans deux, une classe jouée deux fois ou hors de son lot, une classe à test sans résultat, un test rouge. Une classe neuve, absente des durées, prend 3 s par défaut et entre quand même dans un lot. Les classes imbriquées (`Outer$Inner`) suivent leur classe du dessus, des deux côtés (`shards.py` ligne 47 et `app/build.gradle.kts` ligne 153) de la même façon. Aucun test Java dans `app/src/test` : le dossier des classes Kotlin suffit.
- Lot coupé par sa limite, annulé ou rouge : son artefact manque ou ses classes attendues n'ont pas de résultat (problème nommé), et l'étape « Every shard green » (`ci.yml` ligne 270, `if: always()`) rend le job qui rassemble rouge dès qu'un lot n'est pas « success ». « Publish APK » attend `tests` et `build` sans `always()` : rien ne se publie sur un lot rouge.
- Rien de sauté ni d'affaibli : même commande Gradle par lot (`compareRoborazziDebug` pour chaque classe, `:project-model:test` dans le lot 1), `fail-fast: false`, aucun job retiré (émulateur, bancs de son, APK, publication présents ; sanitizers et écoute en direct déplacés tels quels dans leurs jobs). Rien ne change dans les secrets ni la signature.
- La correction d'isolation est vraie : les deux tests qui lisent « Ma bibliothèque » la vident et la relisent avant d'affirmer qu'elle est vide ; `LiveCellJourneyTest` nettoie en partant. Elle ne cache pas de défaut de l'appli : la bibliothèque est un objet unique par processus, partagé entre classes de test seulement dans la machine de test.

## À noter

- `scripts/ci/shards.py:88` : « contient un test » se lit dans le fichier de la classe seulement. Une classe qui hérite de tous ses tests d'une base dans un autre fichier (`CompactSizeScreenshotTest`, `Tour3RenameIdleTest`, `RecordScale130DutchScreenshotTest`…) n'est pas dans `expected.txt` : si Gradle ne la jouait pas, `check` ne le dirait pas. Aujourd'hui leur `@RunWith` les fait trouver par Gradle et le rapport a recompté classe par classe, mais le filet est plus mince pour elles. Piste : compter aussi `Lorg/junit/runner/RunWith;`.
- `scripts/ci/shards.py:394` (`policy`) : un job s'échappe s'il est écrit autrement que `  nom:` seul sur sa ligne. Essayé : `  separation:  # commentaire` sans limite, une clé entre guillemets (`'extra':`) ou un job en une ligne (`extra: { … }`) passent « OK ». Il ne lit que `ci.yml` alors que la ligne ajoutée à `CLAUDE.md` dit « tout nouveau job ou workflow » : `nightly.yml` (120 et 100 min), `packs.yml` (120, 60, 60) et `audit-emulator.yml` (90) sont déjà au-dessus de 50.
- `scripts/ci/shards.py:136` (`durations`) : la mise à jour des durées lit un seul dossier, non récursif ; les rapports réunis sont maintenant rangés en `shard-k/`, et l'artefact `unit-test-reports` n'existe que sur un échec. Le rafraîchissement décrit dans l'aide ne marche plus tel quel (il faudrait lire les six dossiers).
- `scripts/ci/shards.py:261` et `:281` : « passés » désigne les tests sautés (le rapport le reconnaît) ; à remplacer par « sautés ».
- `app/build.gradle.kts:151` : la liste est lue pendant la configuration, avec le cache de configuration actif. Sans risque en CI (machine neuve) ; en local, réécrire le même fichier `-Pshard.classes` sans changer son chemin pourrait réutiliser une configuration ancienne si Gradle ne suit pas cette lecture. À garder en tête pour un usage local.
- `ci.yml` ligne 236 : la boucle `for k in 1 2 3 4 5 6` n'est pas surveillée par `policy` ; passer à 7 lots sans la changer donnerait un rouge bruyant (« lot 7 : pas de résultat »), pas une perte silencieuse.
- `scripts/ci/apt-install.sh` : la clé de cache est fixe (`…-1`) ; si l'image Ubuntu change de versions, le cache ne sert plus et chaque CI retélécharge (lent, pas faux). Relancer « les jobs en échec » d'un seul lot n'a pas été vérifié ici (artefact du même nom dans une seconde tentative).
- 16 jobs en même temps par CI : deux CI côte à côte peuvent faire attendre des jobs en file (plus lent, pas faux).
- Le rapport dit vrai par rapport au code (jobs, étapes, isolation, limites, « Ce qui n'est pas vert ») ; seule nuance : « `policy` refuse un job sans limite » vaut pour l'écriture habituelle de `ci.yml` (voir plus haut).

Verdict : découpage solide, contrôle de complétude sérieux, aucun test perdu ni affaibli ; filets à épaissir (classes héritées, garde-fou `policy`) — fusion possible
