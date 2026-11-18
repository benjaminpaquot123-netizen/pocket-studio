> CORRECTIONS DU RÉDACTEUR (2026-11-18). Elles priment sur le texte dessous.
- Base : `git fetch origin ccr-80af1a1b-mn0wec`, tête 3e9c0524 au moins.
- SDK et Gradle : comme le bloc « CORRECTIONS DU CHEF » de `fiche-captures8.md`, puis `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh …`. Une dépendance qui tombe au hasard : relance avec `--refresh-dependencies` (8 fois au plus).
- Vert = job « Build, unit tests, screenshots » vert ET « Captures : 0 différentes ». Un lot du banc ne change aucune capture : une image changée = une ligne au chef.
- Textes : aucune phrase française de l'appli touchée (lot T0c seul).
- Le plan écrit `app/src/test/resources/bench/**` : ce dossier n'existe pas encore (vérifié le 2026-11-18 ; il n'y a que `audit/`, `capture/`, `real-projects/`, `reference-projects/`, `tuto/`, `upgrade/`). Tu le crées pour les écrans d'audit copiés (point 5 g) ; tu ne touches pas aux autres dossiers de ressources.
- B, S et N touchent tous `human/` ou `scripts/bench/` : B d'abord, seul ; S et N partent de la tête qui contient B.

---

# Fiche tour 8 : lot B « banc9 » (branche `eq/tour-8-banc9`)

> Chef d'orchestre → <NOM>, rôle BANC. Lot tour-8 banc9 : restes banc8 et banc8b, doigt sur cible couverte, attente de « Mes projets ». Aucune question à Benjamin : une ligne pour moi.

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

1. Base : `git fetch origin ccr-80af1a1b-mn0wec`, puis la branche neuve `eq/tour-8-banc9` depuis `FETCH_HEAD`.
2. Lis :
   - `docs/audits/ultra/PLAN-TOUR-8-brouillon.md`, lot B (l. ~101-110) et les deux points du banc en fin de plan (l. ~191 et 195) ;
   - `docs/audits/ultra/PLAN-BANC-FIABLE.md` ;
   - `docs/audits/ultra/sieges/tour-7/incidents-banc.tsv` ;
   - les deux fiches de `docs/AIDE-FUSIONS-TESTS.md`.
3. Tes fichiers, et eux seuls (tous sous `app/src/test/java/com/pocketstudio/app/` sauf mention) :
   - `human/HumanTester.kt` (2332 lignes), `human/SeatOptions.kt`, `human/ScreenText.kt`, `human/TutoAnchor.kt` ;
   - `human/BenchShortSeatTest.kt`, `human/BenchAmeneesTest.kt`, `human/Tour5BloquantsTest.kt` (l. 192-193 et 215-216 seulement) ;
   - `app/src/test/resources/bench/**` (neuf) ;
   - `scripts/human-act.py`, `scripts/bench/*` (avec leurs tests `test_ultra_bench.py`, `test_socle.py`, `test_pre_vol.sh`) ;
   - `docs/audits/ultra/rejeux/*`, `docs/audits/ultra/sieges/tour-7/incidents-banc.tsv`.
   - Hors liste : tout `app/src/main` (même `remix/RemixScreen.kt`, qui porte `remix_pick` l. 317 et 425 : le banc s'adapte, l'appli ne bouge pas) ; `human/OrdinaryTestsLeaveNothingTest.kt` et `StrictTestApplication.kt` (lot S) ; `scripts/bench/fabrique-sieges.py` et `seat-metrics.py` (lot N : n'y touche pas, pour que N parte sans conflit) ; `ui/Coquille2SeatTest.kt` (lu seulement : il attend déjà `projects_loading` l. 38).
4. Pas de rejeu de siège : chaque point est prouvé par un test du banc, « rouge: » avant, « vert: » après.
5. Les points, chacun avec son test rouge puis vert (Robolectric court ou `test_ultra_bench.py`) :
   - a. banc8b, moyen : `HumanTester.kt` l. 905-912, `repeat(8)` de glissés vers le milieu quand la cible n'est pas dans un conteneur défilable (~12 s pour rien, puis toucher du milieu caché). Viser le centre de la partie dessinée ; aucun glissé quand rien ne défile. Test : cible à moitié dessinée hors conteneur défilable → 0 glissé, le doigt tombe dans la partie dessinée.
   - b. l. 904 : `performScrollTo` pour toute cible pas entière ; ne défiler que si la partie dessinée est trop petite pour être touchée.
   - c. l. 919-922 : `drawnAt` muet en cas d'échec (`getOrDefault(true)`) : l'échec s'écrit dans le journal du siège.
   - d. `scripts/human-act.py` l. 132 (relecture `docs/audits/ultra/tour-7-banc8b-relecture.md` l. 17) : `n == turn(seat)` est toujours vrai, donc sans effet ; garder seulement la condition « fin jouée ou siège fini ». Test dans `test_ultra_bench.py` : une image demandée au tour n alors que le siège continue reste refusée.
   - e. `BenchAmeneesTest.kt` : un seul cas d'amenée réparée par banc8b ; ajouter `tuto:techno:9` en ko, phone360, 130 % (le siège T-b du plan).
   - f. banc8, moyens :
     - `handBackNote` avec `replayN = 0` (`SeatOptions.kt` l. 133-134) ;
     - rendre la main quand un élément sans étiquette a bougé (`fingerGone`, appelé l. 449, défini l. 498) ;
     - `reaimTag` avec `dans:` (`SeatOptions.kt` l. 126, `HumanTester.kt` l. 396 ; les tests l. 97-107 de `BenchShortSeatTest` ont déjà `dans:` : ajouter le cas qui casse aujourd'hui) ;
     - étiquette de conteneur intouchable : `remix_pick` autour de `remix_pick_thumb` (attente l. 866-868) ;
     - `lastDiff` jamais remis à zéro (`HumanTester.kt` l. 252, lu l. 263, 272, 311).
   - g. écrans d'audit copiés en ressources de test : `BenchShortSeatTest` lit des écrans réels sous `app/src/test/resources/bench/` (quelques fichiers, pas un tour entier).
   - h. `TutoAnchor.kt` : écrire dans le journal les pas que l'amenée n'a pas pu faire.
   - i. Doigt sur cible couverte (vu sur t4-26) : HumanTester annonce « DOIGT » sur un bouton couvert par un élément dessiné non cliquable ; aujourd'hui seule une ligne `#n LAYOUT …` (l. 533-547, `LayoutScan.Kind.OVERLAP`) le dit. Le doigt échoue, avec la raison, quand un élément dessiné couvre le point visé.
   - j. `reachTag` (l. 895) attend tant que `projects_loading` est affiché (comme `Coquille2SeatTest` l. 38) ; puis retirer l'attente faite à la main ou absente dans `Tour5BloquantsTest` l. 192-193 et 215-216.
6. Phrases d'acceptation (vues du banc) :
   - « en ko, deux mains, phone360 à 130 %, Mode Simple : l'amenée `tuto:techno:9` m'arrive au chapitre 9 avec 0 glissé inutile dans le journal. »
   - « sur un écran où un élément dessiné couvre le bouton, le doigt du banc échoue et le journal dit qui couvre qui. »
   - Plus : `scripts/bench/selftest.sh` vert ; `PS_REJEUX_SELFTEST=$(mktemp -d) scripts/bench/rejeu-defauts.sh` : tous tenus sauf `t4-26-prise-clavier` (défaut de l'appli, lot K) ; chaque point a son test rouge puis vert.
7. Tests ciblés : `ANDROID_HOME=/opt/android-sdk bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests 'com.pocketstudio.app.human.Bench*' --tests '*Tour5Bloquants*' --tests '*Coquille2Seat*'` ; `python3 -m pytest scripts/bench/test_ultra_bench.py -q` (sortie filtrée).
8. Rapport `docs/audits/ultra/tour-8-banc9-rapport.md`. Puis `scripts/bench/selftest.sh`, `pre-vol.sh --avant-relecture tour-8 banc9`, relecture neuve en dernier commit, `pre-vol.sh tour-8 banc9` doit dire PRÊT.
9. Parallèle et place :
   - libre à côté de captures8, M, T0a, T0b, T0c, K, R (aucun fichier de l'appli) ;
   - jamais à côté de S ni de N (mêmes dossiers) : B d'abord ;
   - fusionné hors de toute vague de testeurs, avant les sièges de contrôle (T-a à T-d) ; place proposée : en parallèle des lots de l'appli, rangé dans le premier paquet libre après captures8.
10. Une ligne : « <NOM> fini lot banc9 : <branche> <sha>, points faits N/10, demandes : …, laissés de côté : … ; selftest vert ; pré-vol PRÊT ; rejeux tous tenus sauf t4-26 ».

> **Demander au chef.** Un fichier hors de ta liste, un doute sur la phrase d'acceptation, un conflit avec un autre lot, un test qui casse hors de ta zone : une ligne au chef (`send_message`), puis tu continues le reste de ta fiche en attendant sa réponse. Jamais de question à Benjamin, jamais de décision hors de ta fiche.
> **Travailler en silence.** Aucun récit, aucun résumé, aucun texte pour Benjamin dans ta discussion. Tu n'écris qu'au chef, une ligne, à trois moments : « prêt », « demande / bloqué », « fini ». Sorties de commandes filtrées (`tail`, `grep`).
