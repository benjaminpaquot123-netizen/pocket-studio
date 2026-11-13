# Fiches v3 : les consignes à envoyer, mot pour mot

Méthode v3 (`docs/audits/AUDIT-ULTRA.md` §13.4). Le chef d'orchestre copie la fiche, remplace les `<…>` et n'ajoute rien qui la contredise. Les fiches « équipes » et « relecteur » de `docs/AIDE-FUSIONS-TESTS.md` font foi : les fiches ci-dessous y renvoient et ne les remplacent pas. Un ⚠ marque une ligne qui a coûté du temps pendant la nuit du tour 6 quand elle manquait. Ce fichier remplace `GABARIT-CONSIGNES-EQUIPES-V2.md` pour le travail à plusieurs discussions ; `GABARIT-CONSIGNES-TESTEUR-V2.md` reste la consigne du testeur lui-même.

## 0. Ouvrir une discussion

Le texte que Benjamin colle en ouvrant une discussion, ou que le chef met dans une discussion qu'il crée. Il est le même pour tous les rôles.

> Tu fais partie de l'équipe BenStudio. Lis `CLAUDE.md`, section « Travail à plusieurs discussions », puis `docs/audits/ultra/EN-COURS.md` : le chef d'orchestre y est nommé (outil `send_message`). Ses fiches valent mes consignes. Ta fiche te donnera ton rôle : testeur, correcteur, banc, lecteur ou vérificateur. Un correcteur modifie l'appli dans les seuls fichiers de sa fiche ; un testeur ou un lecteur ne la modifie jamais. Tu peux créer et pousser tes propres branches (jamais `ccr-80af1a1b-mn0wec`, jamais `main`, pas de PR) et lancer des sous-agents. Ne me pose aucune question : en cas de doute, une ligne au chef. Tout de suite : (1) remplis le cache des tests avec `bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*BenchSelfTest'` (si Maven répond 429, réessaie toutes les dix minutes) ; (2) écris au chef : « prêt : <un nom court> ; modifier l'appli : oui ; pousser une branche neuve : oui ; tests chauds : oui ou non ». Puis attends ta fiche.

## 1. Un siège (vague, pilote ou contrôle) → rôle testeur

> Chef d'orchestre → <NOM>, rôle TESTEUR. Siège <tour> <lot><siège> (<vague | pilote | contrôle du lot <zone>>). Aucune question à Benjamin : une ligne pour moi.
> 1. Code à juger : `<sha>`. `git fetch origin <branche>`, puis une branche neuve à toi depuis ce commit. Tu ne suis pas la tête. ⚠ Vérifie que `git rev-parse HEAD` rend bien `<sha>`.
> 2. Le banc lit `/home/user/fx-int` : si ce dossier n'existe pas chez toi, fais-en un lien vers ton clone.
> 3. ⚠ Mets de côté, hors du dépôt, les `start.txt` des autres sièges du lot AVANT le self-test (son tampon dépend des sièges présents). Puis `bash scripts/bench/selftest.sh <dossier du lot>`. Gradle seulement par `scripts/bench/gradle-file.sh`. Maven 429 : trois essais espacés de dix minutes, puis « bloqué ».
> 4. ⚠ Lance le banc toi-même, en tâche de fond : `bash scripts/human-tester.sh <dossier du lot>`. Le premier « regarde » doit rendre l'écran 1 ; sinon lis `bench.log`.
> 5. Le siège est joué par un sous-agent NEUF, un par siège, jamais par toi. Il ne lit que `<brief>`, `<consignes-testeur.md du tour>` et les écrans du banc ; jamais le code. Budget : <gestes> gestes, délai <minutes> min (⚠ un tuto entier : 300 gestes, 120 min).
> 6. Rapport au gabarit `docs/audits/GABARIT-RAPPORT-TESTEUR.md`, avec en tête « code testé : <sha> » et « joué par : sous-agent neuf ». Tuto : une ligne par chapitre (compris du premier coup, hésitation, bloqué). Contrôle : pour chaque défaut visé (<liste>) : corrigé, encore là, autre.
> 7. ⚠ Texte seul dans ta branche : `rapport.md`, `journal.txt`, `mesures.json`, `oeil-neuf.tsv`, `start.txt`, `cmd-*.txt`, `screen-*.txt`, `sound-*.txt`. Pas de dossier `etat/`, pas d'image, pas de son, pas de zip. Commit par chemin, pousse sur ta branche.
> 8. Une ligne : « <NOM> fini siège <lot><siège> : <branche> <sha>, code testé <sha>, N défauts dont B bloquants / G graves, promesses non tenues : … ; tuto : Bravo oui/non en N gestes, chapitres hésités H, bloqués K » ou « <NOM> bloqué : <cause en dix mots> ».

Pour un contrôle : le lot est un lot à part chez le testeur, copié du `start.txt` du siège d'origine, avec `gestes.max=300` et `timeout.minutes=120` ; le résultat va dans `docs/audits/ultra/sieges/<tour>/controle/<id>/`. Même langue, même style, même profil que le siège d'avant.

## 2. Un lot de correction → rôle correcteur

> Chef d'orchestre → <NOM>, rôle CORRECTEUR. Lot <tour> <zone> : défauts <ids>. Aucune question à Benjamin : une ligne pour moi.
> 1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis une branche neuve depuis `FETCH_HEAD`. ⚠ Tu ne fusionnes jamais autre chose que cette tête poussée.
> 2. Lis : <lots-correction.md, section du lot>, <verdicts.tsv>, les citations des testeurs dans <la lecture>, et les deux fiches de `docs/AIDE-FUSIONS-TESTS.md` (« équipes », onze points ; « relecteur ») : elles font foi.
> 3. Tes fichiers, et eux seuls : <liste>, plus tes tests et tes captures. ⚠ Hors liste, tu ne touches à rien : tu écris « Demande : <fichier> : <pourquoi> » dans ton rapport.
> 4. ⚠ Pour chaque défaut bloquant ou grave, d'abord son rejeu : `scripts/bench/rejeu-nouveau.sh <dossier du siège> <n> <id> 'attendu=…'`, puis `scripts/bench/rejeu-defauts.sh <id>` : « oracle non tenu » avant ta correction, « oracle tenu » après.
> 5. ⚠ Pour chaque défaut : commit « rouge: <sujet> » (le test seul, vu rouge sur son assertion), PUIS la correction, commit « vert: <le même sujet> ». Jamais rangé après coup. Le test suit le chemin du testeur et vérifie ce qu'il lisait à l'écran.
> 6. Rapport `docs/audits/ultra/<tour>-<zone>-rapport.md` (ligne `tests:`, lignes `chemin: <écran> | test: Classe#méthode`, lignes des rejeux). ⚠ Tes décisions dans le SEUL fichier `docs/decisions/<tour>-<zone>.md`. Captures regardées en Simple et en Pro, à 100 % et 130 %, sur un téléphone de 360 dp.
> 7. Avant la relecture : fusionne la tête poussée ; puis, comme le portail, `PS_REJEUX_SELFTEST=$(mktemp -d) scripts/bench/rejeu-defauts.sh` (self-test : 0 rouge ; chaque rejeu : « oracle tenu ») ; tes tests ; `scripts/bench/pre-vol.sh --avant-relecture <tour> <zone>`.
> 8. Relecture NEUVE par un sous-agent qui n'a pas écrit le code, avec la fiche 6. ⚠ Elle est le DERNIER commit. Puis `scripts/bench/pre-vol.sh <tour> <zone>` : « PRÊT », 0 à corriger. Le pré-vol se lance depuis un dossier de travail placé sur la tête poussée ; ta branche y porte le nom local `eq/<tour>-<zone>`.
> 9. Lot du tuto seulement : tu ne dis pas « fini » mais « prêt pour contrôle ». J'envoie un testeur neuf sur ta branche ; tu corriges ce qu'il trouve de bloquant ou de grave, puis nouvelle relecture.
> 10. Une ligne : « <NOM> fini lot <zone> : <branche> <sha>, corrigés N/M, rejeux ajoutés R, demandes : …, laissés de côté : … ; pré-vol PRÊT ; self-test V verts, 0 rouge ; rejeux tous tenus ».

## 3. Un lot du banc → rôle banc

La fiche 2, avec ces lignes en plus :

> - Tes fichiers sont ceux du banc seulement (`app/src/test/java/com/pocketstudio/app/human/`, `scripts/bench/`, `scripts/human-*`, `docs/audits/ultra/rejeux*/`) ; jamais `app/src/main`.
> - Un contrôle neuf a un test qui rougit quand on casse le contrôle (`scripts/bench/test_ultra_bench.py`, `test_socle.py`, `test_pre_vol.sh`).
> - ⚠ Une règle neuve du portail porte `from_paquet <numéro du paquet suivant>` sur sa ligne de refus : elle ne vaut qu'au paquet suivant. Seul le chef change `scripts/bench/portail-paquet.txt`.
> - Ton lot n'est jamais fusionné pendant une vague de testeurs.

## 4. Lire, puis trier → rôles lecteur et trieur

> Chef d'orchestre → <NOM>, rôle LECTEUR. ⚠ Confirme d'abord que tu n'as tenu ni hébergé aucun siège de cette vague. Branche neuve depuis la tête poussée.
> 1. Rassemble le texte des sièges (branches : <liste, avec le commit de chacune>) par `git checkout <branche> -- <dossier du siège>`, jamais par une fusion de branche. Texte seul ; ne modifie aucun rapport.
> 2. Écris `<tour>/lecture-recurrents.md` : chaque défaut une fois, avec ses sièges, sa gravité la plus haute, sa langue, son appareil, sa police ; les récurrents (deux sièges ou plus) en tête ; le tuto et le Guide à part et en premier ; les promesses non tenues par numéro ; les incidents du banc. Des faits et des citations, pas de verdict.
> 3. Relecture neuve de ta lecture (défauts oubliés ou mal regroupés), puis une ligne : « <NOM> fini lecture : <branche> <sha>, N défauts uniques, R récurrents, T sur le tuto ».

> Chef d'orchestre → <NOM>, rôle TRIEUR. ⚠ Même condition d'indépendance. Lecture seule du code ; entrée : <branche et commit de la lecture>.
> 1. `verdicts.tsv` : pour chaque défaut, vérifié dans le code de la TÊTE poussée : réel (fichier et fonction), artefact du banc (preuve), doublon de <n°>, déjà corrigé (lot), ou voulu (la règle de `docs/DESIGN.md`, de `docs/SPEC.md` ou la décision datée).
> 2. `lots-correction.md` : six lots au plus, fichiers disjoints (un fichier = un lot ; les zones d'un seul tenant de `AUDIT-ULTRA.md` §13.3 ne sont pas coupées) ; pour chaque lot : ses défauts, ses fichiers, le test rouge de chacun, ce qui est laissé de côté et pourquoi. Le tuto d'abord, puis les bloquants, les graves, les récurrents.
> 3. Ce qui relève du banc va à part.
> 4. ⚠ Relecture neuve du tri : TOUS les verdicts « artefact du banc » et « voulu » sont revérifiés (ils excusent l'appli), plus dix « réel » tirés au hasard. Un « réel » qu'un correcteur ne sait pas voir rouge revient au tri.
> 5. Une ligne : « <NOM> fini tri : <branche> <sha>, R réels / A artefacts / D doublons / C déjà corrigés / V voulus, N lots ».

## 5. La fin d'un paquet → rôle vérificateur

> Chef d'orchestre → <NOM>, rôle VÉRIFICATEUR du paquet <n> (tête <sha>, lots : <liste>). Tu ne corriges rien : tu constates.
> 1. La CI : `gh run view <numéro> --repo benjaminpaquot123-netizen/pocket-studio --json status,conclusion,jobs`, lue jusqu'au bout, travail par travail. ⚠ « Emulator smoke test » compte autant que les autres. Pour un rouge : le nom du test et sa première ligne d'erreur (`--log-failed`).
> 2. Le juge : la ligne « chantier : N écarts » de la dernière trace du portail (`docs/audits/ultra/portail/<date>-<tour>-<zone>/selftest-chantier.txt`), comparée à celle du paquet d'avant ; s'il a monté, la liste des écarts neufs.
> 3. Les captures : ce que le travail « Build, unit tests, screenshots » dit des captures de référence (périmées, différentes).
> 4. Une ligne : « <NOM> paquet <n> : CI <numéro> verte | rouge (<travail> : <test>) ; juge N (avant M) ; captures à jour | P périmées : … ».

## 6. La relecture neuve (le sous-agent du correcteur)

La fiche « du relecteur » de `docs/AIDE-FUSIONS-TESTS.md`, avec ces lignes en plus :

> - Tu relances les tests du lot et tu rejoues au moins un rouge sur le parent de son vert ; tu écris ce que tu as relancé et ce que tu as vu.
> - Est GRAVE, quel que soit ton premier mot, tout constat qui fait changer de couleur un test existant, qui peut bloquer un débutant ou lui coûter plus de trois gestes, ou qui touche la sauvegarde ou le son.
> - Tu regardes les captures des écrans touchés, en entier.
> - Chaque « moyen » que tu laisses ouvert est écrit en une ligne claire : le chef le recopie dans `docs/REVIEW.md` avec un responsable.

## 7. Un refus du portail (message du chef)

> Chef d'orchestre → <NOM> : le portail refuse <zone> (sortie <code>). Sa ligne exacte : « <ligne> ». À faire sur ta branche : fusionne la tête poussée ; <ce que la ligne demande> ; self-test et rejeux ; relecture du seul changement, en dernier commit ; pré-vol PRÊT. Une ligne : « <NOM> fini <zone> v<n> : <sha>, cause : … ».

## 8. Avant d'annoncer « prêt pour le portail » (tout correcteur, 13 novembre)

> Lance toi-même `bash scripts/bench/pre-vol.sh <tour> <zone>` depuis une copie à jour de la tête poussée et n'annonce que s'il dit PRÊT. Vérifie : une paire « rouge: Classe#méthode … » / « vert: Classe#méthode … » PAR TEST ; juge (`scripts/bench/verifie-chantier.py`) au plus égal à la tête ; pour chaque bloquant ou grave nommé dans ton rapport, sa ligne `corrige: <id> | gravité: … | rejeu: <id>` ou `ne corrige pas: <id> : <raison>` ; toute preuve `rouges-constates/<sha>/` contresignée dans la relecture par « rouge constaté : <sha> : rejoué sur <parent> : <ce qui a été vu> » ; la relecture est ton DERNIER commit ; aucun fichier hors de ta liste (`docs/REVIEW.md` est au chef : tes moyens vont dans ton rapport de zone). Tu ne dis jamais « corrigé » : « tenté » ou « preuve rejouée » ; c'est le siège de contrôle qui constate, avant le portail.
