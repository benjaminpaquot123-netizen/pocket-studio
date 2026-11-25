# Tour 9, lot ci-solide : relecture
Relu : 62681cbd

Relecteur neuf (sous-agent qui n'a pas écrit ce code). Lu : `git diff f4301337..62681cbd` (8 commits ; rouge 14fecb43, vert ffcf98cb).

## Contrôles

- YAML : `yaml.safe_load` sur tous les workflows : OK. `bash -n` sur `submodules.sh`, `separation-tests.sh`, `session-android.sh` : OK. Les trois scripts sont exécutables.
- Sous-modules : les 12 anciens blocs (ci 9, nightly 2, audit-emulator 1) sont tous remplacés par `scripts/ci/submodules.sh`, qui prend les 8 chemins de `.gitmodules`. Joué ici sur un faux dépôt dont les deux sous-modules sont introuvables (attentes retirées) : sortie 1. Pas de `-e`, mais chaque échec passe par `status` puis `exit 1` : rien de caché.
- `apt-install.sh python3-pil` : un paquet obligatoire en échec rend le code de sortie ; licence des packs : `--retry 5 --retry-all-errors --max-time 60` avec `-f` : un vrai 404 reste rouge.
- `separation-tests.sh` (`set -euo pipefail`) : archive entière, dépliée dans `ort.partial`, puis `mv` du seul dossier final ; un arrêt en route laisse au pire `ort.tgz`/`ort.partial`, effacés au passage suivant, jamais un dossier final à moitié. Chemin du cache (dossier présent) inchangé.
- Nuit : la clé et le script Robolectric sont ceux de `ci.yml` ; le job `linux` (soak, cpu-bench, real-projects) les a ; le job émulateur n'en a pas besoin.
- `RealUse.MainWatchdog` : limite 5000 ms inchangée (`EnduranceTest`). Fil principal bloqué, machine saine : témoin ≈ 0, retard compté en entier ; bloqué plus de 30 s : 30000 compté. Champs `@Volatile`, dernier `look()` après `join` ; le témoin est arrêté (`quitSafely`). Compilation non rejouée ici (pas d'émulateur).
- Écoute : `sonne()` vaut `null` seulement avec moins de 4 lectures après fenêtre + 30 s ; un vrai silence lu (4 lectures et plus, toutes sous -50 dB) donne `false`, donc toujours rouge. Aucune assertion retirée, aucun seuil touché (`SILENCE_DB`, `READS_PER_SECOND` inchangés), `ToutTester.style` garde `reads > 0`.
- Les tests prouvent : le faux `Journey.idle` dort 5 ms et chaque lecture 40 ms, donc 2 lectures en 60 ms sans le correctif : aucune seconde, « silence ». Rouge 14fecb43 ne touche que `ScenariosTest`.
- Rejoué ici : `./gradlew :app:testDebugUnitTest --tests ...ToutTesterTest --tests ...ScenariosTest` : ScenariosTest 36 tests, ToutTesterTest 13, 0 échec.
- Docs, vérifié contre le code : 12 jobs ; 4 essais GitHub et 10 GitLab ; `Acquire::Retries=5`, délai 20 s, `~/.cache/ps-apt` ; 4 miroirs Maven et sha1 (`robolectric-jar.sh`) ; sha256 du modèle de séparation ; `ffmpeg`/`fdkaac`/`python3-pil` par `apt-install.sh` ; licence 5 essais : tout juste. Aucune doc ne conseille de couper TLS, de contourner un hôte refusé ni d'élargir un seuil (elles l'interdisent).

## Constats

| Point | Gravité | Constat | Piste |
|---|---|---|---|
| `RealUse.kt:167` et `:186` | mineur | Le « témoin machine » vit dans le processus de l'appli, à la même priorité que le fil principal. Un arrêt de tout le processus causé par l'appli (longue pause GC, fils de l'appli qui prennent tout le processeur) retarde aussi le témoin et serait excusé. Un fil principal bloqué seul reste bien rouge. | Monter la priorité du témoin (`Process.THREAD_PRIORITY_URGENT_DISPLAY` ou plus) pour que seul un gel de la machine le retarde ; garder le temps excusé dans le message (déjà fait par `mainRaw`). |
| `RealUse.kt:167` | mineur | `HandlerThread` n'est pas un fil démon : si la manche lève avant `watchdog.close()` (pas de `try/finally` dans `EnduranceTest`), le témoin reste vivant jusqu'à la fin du processus. | `isDaemon = true` avant `start()`, ou `close()` dans un `finally`. |
| `Scenarios.kt:378`, `:634`, `:903`, `:954` | mineur | Le `return nonMesure(PAS_ENTENDU)` passe avant `check(undone)` (et avant `bilan` au tempo) : sur une machine trop lente, un « Annuler » qui ne retire pas l'effet serait masqué en « non mesuré ». | Faire les autres contrôles d'abord et le test « entendu » en dernier, comme déjà fait pour l'arrière-plan (`check(back)` avant). |
| `ScenariosTest.kt:679` | mineur | Le commit vert retire `jouer_arreter_10` du test rouge, sans raison écrite. Et aucun test ne prouve qu'un vrai silence sur machine lente reste rouge au niveau d'un scénario (seul `Bilan.sonne()` est testé). | Une ligne de raison ; un test « machine lente + moteur muet = ÉCHEC ». |
| `separation-tests.sh:44` | mineur, d'avant le lot | Après les 5 essais, un échec réseau du modèle devient « test sauté », sortie 0 : la CI reste verte sans le test. | Ne sauter que sur un 404 (release absente), rouge sinon. |
| `separation-tests.sh:30`, `session-android.sh:10` | mineur | Archives onnxruntime et outils Android sans empreinte, alors que la règle écrite dit « une empreinte quand elle existe ». | Poser le sha256 publié et le vérifier. |
| `submodules.sh:20` | mineur | Avec `pipefail`, `git submodule status \| grep -q` peut valoir faux si `git` prend un SIGPIPE ; sans effet ici (`status` couvre les échecs, sortie courte). | Capturer `git submodule status` dans une variable avant le `grep`. |

Verdict : 0 bloquant, 0 grave
