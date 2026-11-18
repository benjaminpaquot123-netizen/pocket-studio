> CORRECTIONS DU RÉDACTEUR (2026-11-18). Elles priment sur le texte dessous.
- Base : `git fetch origin ccr-80af1a1b-mn0wec`, sur une tête qui contient B (banc9).
- SDK et Gradle : comme le bloc « CORRECTIONS DU CHEF » de `fiche-captures8.md`, puis `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh …`. Une dépendance qui tombe au hasard : relance avec `--refresh-dependencies` (8 fois au plus).
- Vert = job « Build, unit tests, screenshots » vert ET « Captures : 0 différentes » (artefact `screenshots-diff`, `results-summary.json`, champ `golden_file_path`).
- Textes : aucune phrase française de l'appli neuve ou modifiée (lot T0c seul).
- Un lot du banc ne change aucune capture : une image changée = une ligne au chef.
- Où sont les choses (vérifié le 2026-11-18) : les missions et leurs oracles dans `scripts/bench/seat-metrics.py` (`MISSIONS` l. ~422, `resume_later` l. 418 qui répond toujours « non », `JOURNAL_ONLY` l. ~442) ; le but lu par le testeur dans `scripts/bench/fabrique-sieges.py` (`GOALS` l. ~17, 11 colonnes lues l. ~78) ; le tableau de bord dans `scripts/bench/tableau-de-bord.py` ; le gabarit des sièges = `docs/audits/ultra/sieges/<tour>/sieges-proposes.tsv` (11 colonnes, en-tête l. 4 de `fabrique-sieges.py`).

---

# Fiche tour 8 : lot N « niveaux » (branche `eq/tour-8-niveaux`)

> Chef d'orchestre → <NOM>, rôle BANC. Lot tour-8 niveaux : 4 missions neuves avec oracle, oracle de « reprendre », colonne niveau, tableau de bord par niveau. Aucune question à Benjamin : une ligne pour moi.

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

1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis la branche neuve `eq/tour-8-niveaux` depuis `FETCH_HEAD`.
2. Lis :
   - `docs/audits/ultra/PLAN-TOUR-8-brouillon.md`, lot N (l. ~115-119) ;
   - `docs/audits/ultra/PLAN-NIVEAUX.md` §2 (les cinq niveaux), §3 (ce qu'il faut ajouter au banc, avec la limite du faux moteur), §5 (le tableau par niveau), §7 ;
   - `docs/audits/AUDIT-ULTRA.md` §13.4 (le siège pilote) ;
   - les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`.
3. Tes fichiers, et eux seuls :
   - `scripts/bench/seat-metrics.py`, `scripts/bench/fabrique-sieges.py`, `scripts/bench/tableau-de-bord.py` ;
   - `scripts/bench/test_ultra_bench.py` (les tests de ces missions) ;
   - projets de test des oracles : un dossier neuf sous `scripts/bench/` (par ex. `scripts/bench/oracles-niveaux/`), un projet « tient » et un « ne tient pas » par mission ;
   - pour `reprendre` seulement, si le siège doit avancer l'horloge : `app/src/test/java/com/pocketstudio/app/human/SeatOptions.kt` et `SeatState.kt`, après B et avec une ligne au chef d'abord.
   - Hors liste : `app/src/main` ; `HumanTester.kt` (B) ; `OrdinaryTestsLeaveNothingTest.kt` et `StrictTestApplication.kt` (S) ; les sièges déjà joués (`sieges/tour-*`), sauf un gabarit neuf pour le tour 8 si le chef le demande.
4. Pas de rejeu de siège : chaque oracle est prouvé par un test rouge puis vert (« rouge: » d'abord).
5. Les points et leur preuve :
   - a. Quatre missions neuves dans `MISSIONS` et `GOALS` : `mon-morceau`, `boucle-imposee`, `projet-vide`, `pistes-separees` (définitions : PLAN-NIVEAUX §2 et §3). L'oracle lit le projet sauvé (état du siège) et, pour un export, l'existence, le nombre, le format et la longueur des fichiers ; jamais ce qu'ils font entendre (faux moteur). Pour `boucle-imposee`, la fiche du siège donne rythme, basse et accords ; l'oracle compare les notes.
   - b. `reprendre` : un oracle réel à la place de `resume_later` (« non » d'office) ; il lit que le projet rouvert après l'avance de l'horloge garde ses parties, pistes et notes. Si avancer l'horloge sous le banc n'est pas faisable sans `app/src/main` : une ligne au chef, et `reprendre` reste hors du tour 8 (écrit dans le rapport).
   - c. Colonne `niveau` (0 à 4 ou X) dans `sieges-proposes.tsv`, lue par `fabrique-sieges.py`, recopiée dans `mesures.json` ; un siège sans niveau est refusé, sauf `libre` qui ne compte pour aucun niveau.
   - d. `tableau-de-bord.py` : une ligne par niveau (PLAN-NIVEAUX §5).
6. Phrases d'acceptation (vues du banc) :
   - pour chaque mission neuve et `reprendre` : « sur un projet qui ne tient pas la mission, l'oracle dit non et le but manquant ; sur un projet qui la tient, il dit oui » ;
   - « un tableau de bord fait sur des sièges de niveaux 0 à 4 montre une ligne par niveau, et un siège `libre` n'en remplit aucune » ;
   - un siège pilote par mission neuve (AUDIT-ULTRA §13.4), joué hors vague : l'oracle donne le résultat attendu.
   - Plus : `scripts/bench/selftest.sh` vert ; `python3 -m pytest scripts/bench/test_ultra_bench.py -q` vert (sortie filtrée).
7. Rapport `docs/audits/ultra/tour-8-niveaux-rapport.md`. Puis `pre-vol.sh --avant-relecture tour-8 niveaux`, relecture neuve en dernier commit, `pre-vol.sh tour-8 niveaux` doit dire PRÊT.
8. Parallèle et place :
   - après B ; libre à côté de S et de tous les lots de l'appli ;
   - condition du « Prochain départ » d'`EN-COURS.md` : fusionné avant les sièges P-c et P-d ; sinon ces deux sièges jouent `libre`, avec une dérogation écrite (niveau confirmé non couvert, comme au tour 7) ;
   - place proposée : le paquet qui suit B, avec S, hors vague.
9. Une ligne : « <NOM> fini lot niveaux : <branche> <sha>, oracles N/5, pilotes P/4, demandes : …, laissés de côté : … ; selftest vert ; pré-vol PRÊT ».

> **Demander au chef.** Un fichier hors de ta liste, un doute sur la phrase d'acceptation, un conflit avec un autre lot, un test qui casse hors de ta zone : une ligne au chef (`send_message`), puis tu continues le reste de ta fiche en attendant sa réponse. Jamais de question à Benjamin, jamais de décision hors de ta fiche.
> **Travailler en silence.** Aucun récit, aucun résumé, aucun texte pour Benjamin dans ta discussion. Tu n'écris qu'au chef, une ligne, à trois moments : « prêt », « demande / bloqué », « fini ». Sorties de commandes filtrées (`tail`, `grep`).
