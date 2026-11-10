# Étape 3, lot 5 « banc-fin » : rapport de l'auteur (2026-11-10)

Atelier `banc-fin`, branche `eq/tour-5-banc-fin`, au-dessus de fx-int 3dfdd0b. Un seul auteur, rien n'est poussé. À relire par un relecteur neuf avant fusion. Aucune ligne de `app/src/main` n'est changée (les mutations L1 y ont été posées puis retirées fichier par fichier) ; `app/screenshots` n'est pas touché.

## En bref

- Le juge ne se laisse plus tromper par un « vert: » qui retire une garde `@KnownDefect` sans vraie preuve rouge : les trois contournements du relecteur de « pads-live » sont refusés.
- Les six sondes que l'audit « banc crédible » n'avait jamais pu lancer sont réécrites et vues : vertes sur le banc d'aujourd'hui, rouges dès qu'on remet les lignes fautives de l'époque ; les mutations L1 et L2 sont rejouées. F1, F2, F4, F5, G1, G2 passent de « lu dans le code » à « vu ».
- Le rouge capricieux des tests de renommage a une cause trouvée et reproduite (3 rouges sur 18), réparée à la racine (0 rouge sur 36) : l'écran « Mes projets » recevait la liste sur un fil de travail au lieu du fil principal, à cause de la règle de test Compose, pas de l'appli.

## 1. Règle (c) du juge (moyen de la relecture « pads-live »)

| Avant | Après |
|---|---|
| Un « vert: » qui retire `@KnownDefect` d'un test gardé dont le message nomme la méthode compte comme paire. | La garde retirée doit avoir été posée par un commit « rouge: », ou avant la discipline rouge/vert (premier « rouge: » 8f36fa3, 2026-11-08 18:29 UTC, constante `DISCIPLINE_SINCE`) ; le message doit nommer `Classe#méthode`, la classe trouvée par les accolades (une classe imbriquée ne prend pas la place de la classe de test) ; un commit de fusion ne retire rien. |

- Paire : rouge 3f83167 / vert e73b423 (`test_ultra_bench.py::known_defect_lifted_needs_a_red`, 3 cas adverses rouges avant, 2 témoins).
- Chaque règle retirée par mutation (fusion acceptée, méthode seule, origine ignorée) rend le test rouge, chacune sur son cas.
- Le script adverse du relecteur (`judge_adv.py`, lancé contre cet atelier) : les cas 1, 2, 3 sont « FLAGGED ».
- Historique réel inchangé : 0d049be et 2e6e828 gardent leur paire (leurs gardes viennent de 4168889, 15:10 UTC le 2026-11-08, avant le premier « rouge: »). C'est pourquoi la borne n'est pas `--since` (2026-11-07) : elle aurait refusé ces deux verts honnêtes. Décision notée.
- Juge : `chantier : 22 écarts` avec le résumé du lot pads-live, les mêmes qu'avant (tous anciens).

## 2. A.1 : rejouer ce que l'audit n'a pas pu lancer

`app/src/test/java/com/pocketstudio/app/human/AuditFideliteTmpTest.kt` réécrit d'après l'annexe A (le fichier d'origine n'existe nulle part : ni dans l'arbre, ni dans le scratchpad de l'audit). Chaque sonde joue le scénario du constat par l'appli (RhythmViewModel, faux moteur posé comme sur un siège) et lit ce que l'appli lit. Les diffs L1 et L2 de l'audit ont été retrouvés dans le scratchpad `audit-banc/gardes-et-preuves/` (`mutation-l1.diff`, `mutation-l2.py`, `chain-l2.sh`). `docs/audits/ultra/audit-banc/rejoue-a1.sh propre|m1|m2 <dossier>` rejoue le tout ; sorties JUnit et résumés sous `docs/audits/ultra/audit-banc/a1-2026-11-10/`.

| Passage | Ce qu'il fait | Résultat vu |
|---|---|---|
| propre | le code tel quel | 18 verts sur 18 (6 sondes, 6 tests de `Tour5AppProofTest`, les 6 tests de L2) |
| m1 | remet dans le faux moteur les lignes de bebe0c9 : fin du morceau sans `freezeTakes` (F1), `currentStep()` = −1 (F2), pas du transport sans décompte et compté au tempo du moment (F4), positions depuis ▶ (F5) ; casse le câblage du partage là où AppRoot l'appelle (L1 d'aujourd'hui) | les 6 sondes rouges, chacune pour sa raison ; `myFilesListsAProjectZipSavedFromMyProjects` et `noChangedWarningRightAfterSharing` rouges |
| m2 | mutation L2 de l'audit : `toggleClip`, `launchClipRow`, `armLaunch` ne postent plus | les 3 gardes Bench et les 3 preuves hors banc rouges |

Une ligne par constat dans AUDIT-BANC-CREDIBLE.md § 3 (« Rejeu A.1 »), chacune « corrigé par <lot> (<sha>), vu : <preuve> ». Limites :

- F4 sous m1 n'est pas la copie exacte de bebe0c9 : l'ancienne tête de lecture comptait depuis ▶ (« 8 » après 120→60) ; la mutation compte depuis le dernier repère du faux moteur (« 0 »). Les deux sont faux, la sonde voit les deux.
- L1 d'origine n'a plus d'objet : T5-ZIP et T5-ALERTE sont corrigés (lot « partage »), il n'y a plus de `@KnownDefect` à faire tourner, et le diff ne s'applique plus à AppRoot. La L1 d'aujourd'hui casse le câblage composé par les gardes. `myExportsListsTheProjectZipJustSent` reste vert sous m1 : il lit la liste de « Mes fichiers », pas le message de `ProjectZipSender` ; ce chemin (« Prêt à partager ? ») n'a pas de garde sur le message « une copie est dans Documents ».
- `AuditFideliteTmpTest` reste dans l'arbre (nom gardé pour suivre l'annexe A) ; il recoupe des tests existants (`BenchSongClockTest`, `BenchEngineTest`) mais passe par l'appli. Il n'est pas dans les classes attendues du selftest (ni `Bench*`, ni `Tour*`).

## 3. Le test capricieux des renommages (CI 42789204633)

Ce que disait l'échec : la liste de « Mes projets » chargée (1 projet) dans le ViewModel, mais aucune ligne composée, l'accueil encore là avec « Plus tard » aux bornes 0x0, « Mes projets » touché 4 fois.

Pistes écartées, preuve à l'appui :
- répartiteur de Compose bloqué (la cause du lot « pads-live ») : son état lu au début, avant et après la fermeture de chaque renommage, toujours libre, rouges compris ;
- « mainLooper paused=true » : normal sous Robolectric (la boucle principale ne tourne que quand le test la fait tourner) ;
- « Plus tard » aux bornes 0x0 : le bouton est sous le pli de l'accueil, qui défile ; il est aussi là dans les passages verts. Ce n'est pas la cause.
- un test précédent précis : la bissection ne donne pas de coupable unique (rouge après la moitié A des `Bench*` une fois sur quatre, après l'autre moitié jamais en un passage), le défaut est aléatoire.

Ce qui a été vu (diagnostic temporaire, hors arbre) au moment du rouge :
- l'arbre de l'écran : la liste est vide (`rowCount=0`, largeur 0), sans l'état vide : l'écran en est resté à l'état initial « chargement » ;
- le ViewModel est bien celui de l'écran (1 abonné) ; lui faire émettre une valeur de plus, depuis le fil principal, montre aussitôt la ligne et son ⋮ (`message shown=1 menus=1`) ;
- forcer images, notifications de snapshot et mise en page n'y change rien.

Cause : la règle de test Compose v1 (`createEmptyComposeRule`) fait tourner les effets sur un répartiteur de test « unconfined ». Un `collectAsStateWithLifecycle` reprend alors sur le fil qui émet : la liste, chargée sur un fil IO, écrit l'état de l'écran hors du fil principal, pendant que le fil principal compose. Parfois cette écriture n'est jamais recomposée. Sur un téléphone, les effets tournent sur le répartiteur de Compose (fil principal) : le cas n'existe pas. Garde : `Tour3RenameEffectsIdleTest#aFlowEmittedOnAnIoThreadIsWrittenOnTheMainThread` (rouge 6eeceba : « written on the main thread (…), as a phone does: [io-emitter] » ; vert d65c607).

Réparation : les tests de renommage prennent la règle v2 (`junit4.v2.createEmptyComposeRule`, répartiteur standard : le collecteur reprend sur le fil principal quand l'horloge du test tourne). Aucun délai n'est élargi, aucun test n'est désactivé.

Reproduction : une boucle paramétrée du renommage (16 passages dans une JVM, gardée hors de l'arbre, scratchpad `banc-fin/ZzRenameLoopTest.kt`) après `Bench*` : avant, 3 rouges sur 18 renommages (le même message) ; après, 0 sur 18, deux fois (36 verts), avec tous les `Bench*` verts à côté. Seul, sans `Bench*`, le renommage passait déjà 16 fois sur 16.

Non fait, à décider : les deux lignes de `selftest-quarantaine.txt` restent (critère « 3 passages verts de suite » de banc45 à jouer en CI). 180 autres classes de tests utilisent la règle v1 avec des flux d'appli : même risque, hors de la zone de ce lot.

## Commits

| Commit | Quoi |
|---|---|
| 3f83167 | rouge: règle (c) du juge, cas adverses |
| e73b423 | vert: règle (c) resserrée, décision |
| d51c48c | A.1 : sondes réécrites, rejeu, sorties, § 3 |
| 7f3c17c | journal (règle (c), A.1) |
| 6eeceba | rouge: garde du fil des effets |
| d65c607 | vert: règle v2 pour les renommages, décision |
| (suivant) | journal (renommages), ce rapport |

## Vérifications

- `python3 -I scripts/bench/test_ultra_bench.py` : « ultra bench scripts: OK ».
- `bash scripts/bench/selftest.sh <dossier>` : « 165 verts, 0 rouges, 0 en quarantaine, 41 sautés », selftest vert (les quatre tests de renommage verts, la garde nouvelle comprise).
- `python3 -I scripts/bench/verifie-chantier.py --resume <dossier>/selftest-resume.txt` : « chantier : 22 écarts », tous anciens (même liste qu'au lot pads-live).
- Suite complète (`PS_STALL_MIN=70`) : voir la dernière ligne de ce rapport.
- `git ls-tree -r HEAD | grep -c '^120000'` : 0. Aucun identifiant de modèle dans les messages.

## Classes à rejouer à la fusion

`Tour3RenameEffectsIdleTest`, `Tour3RenameIdleTest`, `Tour3StudioRenameIdleTest`, `Tour3NameFieldIdleTest`, `AuditFideliteTmpTest`, `Tour5AppProofTest`, `BenchTour3Test`, `BenchAmeneesTest`, `BenchTour4Test`, `Tour3AppProofTest`, `Tour4AppProofTest`, `com.pocketstudio.app.padlive.Tour3Zone1Test` ; `test_ultra_bench.py`.

## Doutes

- La cause exacte côté Compose (pourquoi une écriture d'état hors du fil principal se perd) n'est pas lue dans le code de Compose ; elle est établie par l'expérience (valeur suivante montrée aussitôt, rouge reproduit puis disparu avec le répartiteur standard) et par la garde.
- `DISCIPLINE_SINCE` est une date écrite en dur : une garde ajoutée avant elle par n'importe quel commit reste acceptée.
- Mineurs de `tour-5-pads-live-relecture.md` (horloge des pads après la fin du morceau, KDoc des gardes de siège, `closingSaves` dans `afterATestWithTheRule`, journal) : non repris dans ce lot.
