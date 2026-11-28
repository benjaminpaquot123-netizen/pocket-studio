> CORRECTIONS DU RÉDACTEUR (2026-11-18). Elles priment sur le texte dessous.
- Base : `git fetch origin ccr-80af1a1b-mn0wec`, sur une tête qui contient B (banc9). Si B tarde, le chef peut te lancer plus tôt : tu ne touches alors que tes deux fichiers.
- SDK et Gradle : comme le bloc « CORRECTIONS DU CHEF » de `fiche-captures8.md`, puis `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh …`. Une dépendance qui tombe au hasard : relance la même commande, sans `--refresh-dependencies` (8 fois au plus).
- Vert = job « Build, unit tests, screenshots » vert ET « Captures : 0 différentes » (artefact `screenshots-diff`, `results-summary.json`, champ `golden_file_path`).
- Textes : aucune phrase française de l'appli neuve ou modifiée (lot T0c seul).
- Un lot du banc ne change aucune capture : une image changée = une ligne au chef.

---

# Fiche tour 8 : lot S « stable8 » (branche `eq/tour-8-stable8`)

> Chef d'orchestre → <NOM>, rôle BANC. Lot tour-8 stable8 : `OrdinaryTestsLeaveNothingTest#c_…` rouge sous charge. Aucune question à Benjamin : une ligne pour moi.

Rappels :
- Dépôt PUBLIC.
- Jamais `git add -A`, `git add .` ni `git commit -a` : on ajoute par chemin.
- Pas de push sur l'intégration (`ccr-80af1a1b-mn0wec`) ni sur `main`. Pas de PR.
- Aucun identifiant de modèle dans les commits.
- Commits « rouge: » puis « vert: ».
- Code et commits en anglais.

Lignes du banc (gabarit §3) :
- Tes fichiers sont ceux du banc seulement (`app/src/test/java/com/pocketstudio/app/human/`, `scripts/bench/`, `scripts/human-*`, `docs/audits/ultra/rejeux*/`) ; jamais `app/src/main`.
- Un contrôle neuf a un test qui rougit quand on casse le contrôle (`scripts/bench/test_ultra_bench.py`, `test_socle.py`, `test_pre_vol.sh`).
- ⚠ Une règle neuve du portail porte `from_paquet <numéro du paquet suivant>` sur sa ligne de refus : elle ne vaut qu'au paquet suivant. Seul le chef change `scripts/bench/portail-paquet.txt`.
- Ton lot n'est jamais fusionné pendant une vague de testeurs.

1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis la branche neuve `eq/tour-8-stable8` depuis `FETCH_HEAD`.
2. Lis :
   - `docs/audits/ultra/PLAN-TOUR-8-brouillon.md`, lot S (l. ~110-113) ;
   - la ligne « Tour 6, lot stable » de `docs/DECISIONS.md` et `scripts/bench/test_stable.py` ;
   - les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`.
3. Tes fichiers, et eux seuls :
   - `app/src/test/java/com/pocketstudio/app/human/OrdinaryTestsLeaveNothingTest.kt` (120 lignes ; le test `c_…` l. 60-74) ;
   - `app/src/test/java/com/pocketstudio/app/StrictTestApplication.kt` (44 lignes ; `afterTest` l. 30).
   - Hors liste : tout le reste, en particulier les autres fichiers de `human/` (lots B et N) et `app/src/main`.
4. Le défaut : `c_anOrdinaryTestThatLeavesTheAppsWorkRunningIsRedItself` rougit sous charge avec « red itself, by name: null » (`afterTest` n'a rien levé : il n'a pas vu le travail laissé sur `Dispatchers.Default`) ; seul, il est vert.
   - D'abord la cause, mesurée, pas devinée : une des trois pistes du plan (attente en temps réel trop courte, ordre des tests, nom perdu), ou une autre. Commit « rouge: » avec un test qui la montre sans charge (par ex. un délai de démarrage du worker forcé), puis « vert: ».
   - Interdit : quarantaine, `@Ignore`, ligne dans `selftest-quarantaine.txt`, relâcher l'assertion « red itself, by name ».
5. Phrase d'acceptation (vue du banc) : « avec un rejeu Gradle lancé à côté, les tests `com.pocketstudio.app.human.*` passent 20 fois de suite, `c_…` compris, sans quarantaine. »
   - Preuve : la boucle de 20 passages, `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests 'com.pocketstudio.app.human.*'` avec un `rejeu-defauts.sh` lancé en même temps ; le compte « 20/20 » et la cause écrite dans le rapport.
   - Les tests `a_`, `b_`, `d_`, `e_` du même fichier restent verts sans changement de leurs assertions.
6. Rapport `docs/audits/ultra/tour-8-stable8-rapport.md` (cause, preuve, 20/20). Puis `scripts/bench/selftest.sh`, `pre-vol.sh --avant-relecture tour-8 stable8`, relecture neuve en dernier commit, `pre-vol.sh tour-8 stable8` doit dire PRÊT.
7. Parallèle et place :
   - après B ; libre à côté de N (fichiers disjoints) et de tous les lots de l'appli ;
   - fusionné hors de toute vague de testeurs ; place proposée : le paquet qui suit B, avec N.
8. Une ligne : « <NOM> fini lot stable8 : <branche> <sha>, cause : …, 20/20 sous charge, demandes : … ; selftest vert ; pré-vol PRÊT ».

> **Demander au chef.** Un fichier hors de ta liste, un doute sur la phrase d'acceptation, un conflit avec un autre lot, un test qui casse hors de ta zone : une ligne au chef (`send_message`), puis tu continues le reste de ta fiche en attendant sa réponse. Jamais de question à Benjamin, jamais de décision hors de ta fiche.
> **Travailler en silence.** Aucun récit, aucun résumé, aucun texte pour Benjamin dans ta discussion. Tu n'écris qu'au chef, une ligne, à trois moments : « prêt », « demande / bloqué », « fini ». Sorties de commandes filtrées (`tail`, `grep`).
