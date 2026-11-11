# Lot banc « portail » : troisième relecture neuve (correctif de G2 et M8)
Relu : 8c840dbda14c22c3a02bb9a5da72096eae3eec43

Relecteur neuf (n'a écrit ni le code ni les relectures précédentes), 2026-11-10. Lus : la deuxième relecture (en annexe), la section « Après relecture 2 » de `tour-5-portail-rapport.md`, `git log e5514137..8c840dbd --no-merges` et le diff de `scripts/bench/` des quatre commits a989d0b5, 9a94aed0, 6be9442e, 6614d1ba (le contenu venu de fx-int par le merge 8c840dbd ignoré). Expériences dans un clone jetable (`scratchpad/relecture-portail3/clone`, supprimé à la fin, aucune branche dans le dépôt partagé) ; aucun selftest complet (un portail lancé dans le clone a été coupé à son entrée dans la file Gradle, rien n'a tourné).

En bref : G2 tel qu'écrit est fermé (le test inexistant est refusé par le juge, et le portail voit désormais le dossier `rouges-constates/` et exige sa ligne dans la relecture). Mais le juge seul accepte toujours un XML d'échec fabriqué pour un test réel qui n'a jamais rougi, et même pour une fonction d'aide privée qui n'est pas un test : la seule barrière est une ligne de texte libre dans la relecture neuve. Moyen, pas grave : rien n'arrive sur main par là sans qu'un relecteur écrive « rouge constaté » pour ce sha. M8 : les neuf formes demandées sont refusées, aucune des 14 relectures réelles acceptées avant n'est refusée ; d'autres écritures passent encore (la dernière ligne reste la porte).

## Constats

### Bloquant

Aucun.

### Grave

Aucun.

### Moyen

**M9. La preuve « rouge constaté » se fabrique encore pour un test réel, ou pour une fonction qui n'est pas un test ; seule la ligne de la relecture l'arrête.** `test_in()` (verifie-chantier.py) cherche `Classe#méthode` parmi toutes les fonctions (`FUN` = tout `fun`) des fichiers de test, sans `@Test` ni `@Ignore` (le point (a) de la correction proposée l'exigeait). Forge rejouée (clone, tête 8c840dbd, `scratchpad/relecture-portail3/forge.sh` : un commit « vert: the made-up fix (Classe#méthode) » qui ajoute un commentaire à `AppInfo.kt`, puis un commit qui ajoute `rouge.txt` et un `f.xml` d'une ligne avec `<failure>`). Juge sans résumé : 23 écarts avant, puis 24 après les trois forges, la seule ligne nouvelle étant A. Sans les dossiers de preuve (déplacés), les trois sont « vert sans rouge » : la preuve blanchit donc B et C.
Cas A, `NoSuchTest#neverExisted` : refusé (« vert sans rouge du même test avant lui »). Cas B, `SimpleModeUnchangedTest#aProjectWithoutMasterProSavesAsBefore` (test réel, `@Test`, jamais rouge) : accepté comme paire. Cas C, `TutoJourneyTest#idle` (la fonction d'aide `private fun idle(ms: Long = 50)`, pas un test, ne peut pas avoir de `<testcase>`) : accepté comme paire.
Au portail (vrai `fusion-equipes.sh`, intégration du clone à 8c840dbd, branche `eq/tour-9-…` qui apporte les trois dossiers) : relecture sans ligne « rouge constaté » : refusée, exit 3, les trois sha nommés ; relecture avec une ligne « rouge constaté » pour un autre sha (8c840dbd) : refusée, exit 3, les trois sha nommés ; relecture avec « rouge constaté : <sha> : rejoué » pour les trois (texte quelconque) : la revue passe et la fusion se fait (`Merge team d3 of tour-9`, coupée ensuite à l'entrée du selftest, fusion défaite dans le clone). La barrière est donc la ligne de la relecture, en texte libre comme « rejeu retiré » (m3) : un relecteur pressé ou complaisant fait passer B ou C. Hors portail (commit direct dans fx-int, fusion à la main, juge lancé dans un atelier), rien ne la voit ; le juge lit d'ailleurs le dossier dans l'arbre de travail, donc une preuve non commitée compte aussi pour un juge lancé en atelier. Correction proposée (≈ 0,5 h) : `test_in` ne retient que les fonctions précédées de `@Test` (et sans `@Ignore`), avec un cas « fonction d'aide » dans `red_seen_in_a_run` ; dire dans la consigne du relecteur ce que « rouge constaté » exige (rejouer le test sur le parent du vert et le voir rouge, ou écrire pourquoi pas).

**M10. M8 corrigé pour les formes listées, d'autres écritures passent encore.** Script `scratchpad/relecture-portail3/m8.py` (dernière ligne « 0 bloquant, 0 grave — fusion possible ») contre l'ancien script (e5514137) et le nouveau. Désormais REFUSÉES (toutes acceptées avant) : puce « pas **corrigé** », paragraphe « Non **corrigé**. » et « Pas **corrigé**. » sous un titre de gravité, titre de gravité dont le premier paragraphe est « Aucun. » suivi d'un vrai constat, liste « 1. grave … » et « • grave … », « 1) **Grave** … », titre « ### G1 (grave) », puce « - **G1 (grave)** … », cellule « très grave ». Toujours ACCEPTÉES (le rapport en avoue quatre) : titre « Constats graves », paragraphe simple « Grave : … », cellules « Gravissime » et « critique ». ACCEPTÉES, non dites par le rapport : « Pas encore **corrigé** », « jamais **corrigé** », « Non  **corrigé** » (deux espaces), « non : **corrigé** », « *non* **corrigé** » (le contrôle de négation ne lit que « Non » ou « Pas » suivi d'une seule espace juste avant le gras) ; titres « #### G1 [grave] » et « ### G1 — grave » ; puces « - G1, grave : … », « a) grave … », citation « > - grave … » ; paragraphe « **Grave.** … » hors titre. Les cas honnêtes restent acceptés (« **corrigé** en abc1234 », « Aucun. » seul, tableau de décompte). Comme le dit le rapport, c'est une aide contre l'incohérence, la dernière ligne reste la porte (elle tient) ; le nom du constat doit le dire tel quel dans la consigne.

### Mineur

- **m9.** Ce lot sera fusionné par le portail de fx-int (ancienne version) : il ne lit pas `relecture-verdict.py` (absent de fx-int) mais cherche `Verdict *: *0 bloquant, *0 grave` n'importe où dans le texte. Les relectures en annexe doivent donc écrire « Ancien verdict » (« v » minuscule) pour toute citation de verdict, ce qui est fait ici. Le nouveau portail ne prend effet qu'après cette fusion.
- **m10.** La règle G2 du portail ne s'applique pas en arrière : la relecture de « bloquants » (qui apportait la vraie preuve de dd31ec4b) n'avait pas de ligne « rouge constaté » ; sans conséquence (déjà fusionnée, preuve cohérente, rejouée par la deuxième relecture).

## Réponses aux trois questions

1. **G2 fermé tel qu'écrit.** La forge `NoSuchTest#neverExisted` de la deuxième relecture, rejouée sur 8c840dbd, est refusée par le juge (cas A). Variante « test qui existe mais n'a jamais rougi » : passe le juge (cas B), arrêtée au portail seulement si la relecture n'a pas de ligne « rouge constaté » pour ce sha. Variante « fonction d'aide » : passe aussi (cas C, M9). Variante « dossier apporté avec une ligne rouge constaté pour un autre sha » : refusée par le portail (exit 3, sha manquants nommés). dd31ec4b reste une paire (test réel), le juge sur l'historique réel garde ses 23 écarts sans résumé, dont « résumé absent ».
2. **M8** : les formes listées par la deuxième relecture et corrigées par le rapport sont refusées (détail en M10). **14 relectures réelles** : chaque relecture telle qu'au second parent des 21 fusions « Merge team … of tour-5 », passée à l'ancien script (e5514137) et au nouveau avec les arguments `constat:` que le portail calculerait (aucun) : mêmes codes de sortie pour les 21 ; 14 acceptées par les deux (banc-fin, pads-live, partage, ci-fuite, faux-moteur, juge, banc45, seance5a, fusion64, corr61, banc41, banc-r1, scan-r3, recette2), 7 refusées par les deux (realisme, bloquants et memoire ont leur grave sans « **corrigé** », composants un « 1. **Grave n° 1 … : corrigé.** » sans gras sur le mot (déjà refusé par l'ancien script), banc, banc2 et banc3 ont une dernière ligne d'avant la règle). Aucun refus nouveau.
3. **Suite** : `python3 -I scripts/bench/test_ultra_bench.py` → `ultra bench scripts: OK` (26 s). Rouges : a989d0b5 → `FAIL: evidence that does not hold (nosuch) makes no pair`, 6be9442e → `FAIL: an open blocking or grave finding is refused: ### Grave`. Mutations (une à la fois, fichier restauré par `git checkout -- <fichier>`) : boucle `test_in` retirée de `red_seen` → `FAIL: evidence that does not hold (nosuch) makes no pair` ; `$evidence` retiré de l'appel du portail → `FAIL: red evidence brought without its line in the review is refused` ; lecture des négations retirée de `FIXED_BOLD` → `FAIL: an open blocking or grave finding is refused: ### Grave`.

## Ce qui tient

- Le juge exige que le test nommé existe, dans le parent du vert et dans le vert (classe propriétaire comprise) ; le portail liste tout dossier `rouges-constates/<sha>/` apporté ou changé par la branche, et un lien symbolique reste refusé (règle 7.1), ce qui ferme le contournement par un dossier lié.
- Chaque correction a sa paire `rouge:`/`vert:`, et chaque rouge échoue sur son cas.

Cette branche apporte elle-même, par rapport à fx-int, la preuve `rouges-constates/dd31ec4b…/` ; le nouveau portail exigerait donc la ligne suivante (l'ancien, qui fusionnera ce lot, ne la lit pas). Relu, pas rejoué ici (pas de Gradle) : parent b5bf7b1f juste, test réel `@Test` paramétré, le vert ne change que `Spotlight.kt`, XML avec `<failure>` pour `theWholeCourse[techno Pro]`, rejeu noté dans `rouge.txt` (74 tests, 1 rouge).

rouge constaté : dd31ec4bf6b302499c9987903930a9456656a3c1 : relu, pas rejoué par cette relecture ; rejoué sur b5bf7b1f par l'équipe (rouge.txt) et vu cohérent par la deuxième relecture

## Relectures précédentes

### Deuxième relecture (e11c6d0d)

Relu : e11c6d0da3c9b817d92c5ef18f08deed53cb9c67

Relecteur neuf (n'a écrit ni le code ni la première relecture), 2026-11-10. Lus : la première relecture (en annexe), la section « Après relecture » de `tour-5-portail-rapport.md`, `git log 93f6719f..e11c6d0d` et son diff dans `scripts/bench/` (le contenu venu de fx-int par 04f5eaa9 ignoré). Expériences dans un clone jetable (`scratchpad/relecture-portail2/clone`, supprimé à la fin, aucune branche dans le dépôt partagé) ; aucun Gradle ; atelier `eq-tour-5-realisme` lu seulement.

En bref : G1 tel qu'écrit est fermé (le tampon de deux lignes est refusé), M1 à M5 tiennent et chaque correction a un test qui rougit si on la retire. Mais la nouvelle preuve « rouges-constates/<sha>/ » blanchit un vert sans rouge avec deux fichiers écrits à la main, pour un test qui n'existe même pas, et rien ne la voit au portail : c'est le même défaut que G1, déplacé. Grave.

### Constats

#### Bloquant

Aucun.

#### Grave

**G2. La preuve « rouge constaté » se fabrique à la main et blanchit un vert sans rouge, même d'un test inexistant.** **corrigé** (9a94aed0 : test inexistant refusé, dossier vu par le portail ; ce qui passe encore : M9 de la troisième relecture, en tête de ce fichier). `red_seen()` (verifie-chantier.py) accepte un vert dès que `docs/audits/ultra/rouges-constates/<sha complet du vert>/` contient un `rouge.txt` (« parent: » = le parent du vert, « test: Classe#méthode » nommé par le vert) et un XML où un `<testcase>` de ce nom a un `<failure>`, et que le vert ne touche aucun fichier de test. Rien ne vérifie que le test existe (ni dans le parent ni dans le vert), qu'il tourne, que le XML vient d'un vrai Gradle, ni que la relecture neuve a vu ou rejoué ce rouge ; le portail ne regarde pas ce dossier. Le rapport dit « le juge revient à 22 sans être affaibli » : il l'est, au prix de deux fichiers texte. Preuve (clone, tête e11c6d0d) : un commit « vert: the made-up fix (NoSuchTest#neverExisted) » qui ajoute une ligne de commentaire à un `.kt` de `app/src/main`, puis un commit qui ajoute `rouges-constates/<sha>/rouge.txt` (deux lignes) et `f.xml` (une ligne : `<testsuite><testcase classname="x.NoSuchTest" name="neverExisted"><failure message="made up"/></testcase></testsuite>`) :

##### Preuve d'alors (G2)

```
avant la preuve : 1 ligne « neverExisted » dans le juge (vert sans rouge)
après la preuve : 0 ligne « neverExisted » ; chantier : 23 écarts (22 + « résumé absent », comme sur la tête nue)
```

C'est la barre de G1 (forge sans Gradle, quelques lignes), mais commitée, donc durable sur main, et sans le filet du portail (qui refuse une trace écrite par une branche, pas une preuve de rouge). Correction proposée (≈ 1 h) : (a) le test nommé doit exister avec `@Test` et sans `@Ignore` dans le parent et dans le vert (`class_methods`, paramètre entre crochets ôté) ; (b) un dossier `rouges-constates/` ajouté par une branche exige dans la relecture neuve une ligne « rouge constaté : <sha> : <comment il a été rejoué> », lue par `relecture-verdict.py` comme « rejeu retiré » (le relecteur rejoue ou dit pourquoi) ; (c) un test « forge » dans `red_seen_in_a_run` : XML d'un test absent des sources = refusé. Non corrigé alors ; corrigé depuis par 9a94aed0 pour (b) et (c), pour (a) sans le contrôle `@Test`/`@Ignore` (M9 de la troisième relecture).

#### Moyen

**M6. Une fausse trace du portail, commitée par une branche, redonne un résumé vert sans aucun XML (variante 2 de G1).** `committed_trace()` ne vérifie que le chemin (`docs/audits/ultra/portail/…`) et le sujet du dernier commit (« Portal traces of the merge … »), que tout le monde peut écrire. Preuve (clone) : le résumé et le tampon de deux lignes de la forge G1 copiés dans `docs/audits/ultra/portail/2026-11-10-tour-9-faux/`, commit « Portal traces of the merge of faux (tour-9) », `verifie-chantier.py --resume <ce résumé>` → `chantier : 22 écarts`, aucune « pas verte », aucun « résumé refusé ». Le portail refuse une branche qui touche ce dossier (vérifié par la première relecture) : la forge ne trompe qu'un juge lancé hors du portail (atelier, rapport d'équipe) ou une fusion faite à la main (m1 d'alors). Correction : la trace n'est lue que si le commit qui la pose a pour parent une fusion « Merge team … » dont `fusion.txt` nomme le sha, ou ne plus lire de trace comme résumé (garder les XML dans la trace).

**M7. Des XML fabriqués redonnent toujours un résumé vert, scellé, que `tampon.sh verifie` accepte (variante 1 de G1).** C'est la barre que la première relecture proposait et que le rapport annonce (« la forge reste possible localement ») ; le prix, mesuré : un script de 20 lignes qui écrit un `TEST-<classe>.xml` par classe attendue (`selftest-attendus.py --root .`, 28 classes) avec un `<testcase>` vert inventé, plus un `<testcase>` vert par garde ; `selftest-verdict.py` en fait le résumé (« 40 verts, 0 rouges »), `tampon.sh ecris` → 0, `tampon.sh verifie` → 0, juge → `chantier : 22 écarts`. Aucun Gradle. Le portail relance son propre selftest, donc rien n'arrive sur main par là. Durcissement possible : comparer les `<testcase>` aux fonctions `@Test` des sources (nombre et noms par classe), pas seulement les classes.

**M8. M1 corrigé pour les formes demandées, pas pour toutes.** Script `m1.py` (dernière ligne « Ancien verdict : 0 bloquant, 0 grave — fusion possible ») : REFUSÉES à bon droit, en cellule `grave`, `Grave`, `GRAVE`, `grave (G1)`, `**grave**`, `graves`, `bloquant (P1)`, `Gravité : grave`, dernière cellule `corrigé ? non` ou `non corrigé`, « corrigé » dans une autre cellule que la dernière ; en puce `- grave …`, `- **Grave** …`, `- GRAVE (G1) …`, `**non corrigé**` ; en titre `Grave`, `GRAVE`, `Grave (G1)`, `Graves`. ACCEPTÉS à bon droit : dernière cellule `**Corrigé** en abc1234`, cellule `grave (corrigé en abc1234)`, puce ou paragraphe avec `**corrigé**`, `Aucun.` sous le titre, décomptes « bloquant 0 / grave 0 » (même en gras) ; « grave 1 » en décompte est refusé. ACCEPTÉS encore à tort : puce `pas **corrigé**`, paragraphe `Non **corrigé**.` ou `Pas **corrigé**.` (le gras suffit, la négation devant n'est pas lue) ; titre « Grave » dont le premier paragraphe est `Aucun.` suivi d'un vrai constat ; liste numérotée `1. grave …` ou `• grave …` ; titre `G1 (grave)` ou `Constats graves` ; puce `- **G1 (grave)** …` ; paragraphe simple `Grave : …` ; cellules `très grave`, `Gravissime`, `critique`. La dernière ligne reste la vraie barrière (elle tient) ; ce contrôle est une aide contre l'incohérence, à décrire comme telle. Les blocs de code ne sont pas reconnus (une ligne `| 1 | grave |` dans un bloc ```` ``` ```` fait refuser : prudent).

#### Mineur

- **m6. Règle (c) : une classe renommée n'est pas suivie en pratique.** `_guard_commits` liste les commits par `git log -G KnownDefect` : un commit qui renomme seulement la classe ne change pas la ligne `@KnownDefect`, il n'est donc jamais examiné, et la branche « renamed » de `guard_origin` ne sert que si le renommage touche aussi l'annotation. Contrôle ajouté à la main dans `rule_c_adversarial` (rouge honnête de `RenProofTest#renTest`, « chore: » qui renomme la classe en `RenNewTest`, vert qui lève la garde et touche l'appli) : `FAIL: control R is a pair` (refusé). Sens prudent (faux refus, jamais une forge acceptée) ; deux mutations de cette branche (« renommé = pas d'origine », « renommé = origine ») laissent la suite verte : elle n'est pas testée. Le docstring et le rapport (« classe renommée suivie vers son ancien nom ») disent plus que le code.
- **m7.** Le `trap ERR` de M2 ne couvre que les commandes simples (pas d'échec dans une substitution `$(…)` de l'écriture de `fusion.txt`, pas de `set -E`) ; suffisant pour le cas de la première relecture (`cp` sans `selftest.ok`), rejoué par le test.
- **m8.** `relecture-verdict.py` exige « **corrigé** » en gras sous un titre « Grave » : `corrigé en abc1234` sans gras est refusé (essai sur la relecture « réalisme » ci-dessous). Prudent, mais à écrire dans la consigne du relecteur.

### Réponses aux cinq questions

1. **G1 fermé tel qu'écrit.** Forge de la première relecture rejouée sur e11c6d0d (12 « vert : » recopiés des « pas verte », `code testé :`, tampon `resume:` + `code:`) → `résumé refusé : ni résultats JUnit à côté (junit/), ni trace du portail commitée par « Portal traces of the merge … » : un tampon sans eux s'écrit à la main`, `chantier : 39 écarts` (22 + 16 « pas verte » + le refus). Passent encore : XML fabriqués (M7, 20 lignes de Python) et fausse trace commitée (M6, un commit), tous deux arrêtés par le portail ; et le même défaut dans la nouvelle preuve de rouge (G2), qui, lui, passe le portail.
2. **M1** : toutes les formes demandées sont refusées (cellule, puce, titre, « Grave », « GRAVE », « grave (G1) ») ; un grave dit **corrigé** est accepté ; « | grave | 0 | » est accepté. Restes : M8. **Relecture « réalisme »** (`eq-tour-5-realisme`, c18af300), dernière ligne seule passée à « 0 grave » : REFUSÉE (`a finding under « ### Grave » is not « **corrigé** »: **G1. L'état des pads …`), à bon droit puisque rien n'y dit G1 corrigé. Avec `(**corrigé** en abc1234)` dans le paragraphe de G1 : acceptée (exit 0). Avec `(corrigé en abc1234)` sans gras : refusée (m8).
3. **Mutations** (chacune appliquée seule dans le clone, suite complète relancée, fichier restauré) :
   - M2, `trap undo_merge ERR` retiré → `FAIL: a failure while writing the traces undoes the merge, nothing left`.
   - M3, origine suivie par nom de méthode seul (ancien code) → `FAIL: case M3 of judge_adv2 is refused`.
   - M4, classes attendues relues dans le lot (`$lot/selftest-attendus.txt`) → `FAIL: results of one test where the self-test expects its classes: no stamp`.
   - M5, `changed` jamais rempli → `FAIL: a replay changed without « rejeu changé » is refused`.
   - G1, branche `committed_trace` neutralisée → `FAIL: a stamp written by hand without JUnit results is refused (G1)`.
   - M1, puces et titres non lus → `FAIL: an open blocking or grave finding is refused: - **Bloquant** : …`.
   - 23e écart, `red_seen` qui rend toujours False → `FAIL: a green whose red was seen in a run, its XML kept, is a pair`.
   - Seules exceptions : les deux mutations de la branche « classe renommée » (m6).
4. **« rouges-constates/<sha>/ » : oui, elle blanchit un vert sans rouge** (G2), pour le prix de deux fichiers texte et d'un commit, sans Gradle, sans même que le test existe ; le seul frein est qu'elle se voit dans le diff, si le relecteur neuf la regarde. La preuve réelle (dd31ec4b, TutoJourneyTest#theWholeCourse[techno Pro], rejouée sur b5bf7b1f) est cohérente : parent juste, le vert ne touche pas les tests.
5. **Juge sur l'historique réel** (clone à e11c6d0d) : sans résumé, `chantier : 23 écarts` = 22 + « résumé absent » ; avec un résumé accepté (lot M7), `chantier : 22 écarts`. Comparé (sha ôtés, lignes « pas verte » et refus de résumé à part) au juge de 93f6719f : seule différence, la ligne `dd31ec4b « vert: TutoJourneyTest#theWholeCourse[techno Pro] … » : vert sans rouge` disparue. `python3 -I scripts/bench/test_ultra_bench.py` → `ultra bench scripts: OK` (28 s).

### Ce qui tient

- G1 tel qu'écrit, M2, M3 (cas « même nom, autre classe » refusé, M3a honnête accepté), M4, M5 : corrigés, chacun avec sa paire `rouge:`/`vert:` et un test qui rougit à la mutation.
- Le portail refuse toujours une branche qui écrit dans `docs/audits/ultra/portail/` ; il relance le selftest sur le code fusionné (les forges M6 et M7 n'y passent pas).
- `seal_resume` fabrique désormais `junit/` et le tampon complet : les tests ne codifient plus le tampon de deux lignes.

### Première relecture (79a092a6)

Relu : 79a092a6133d8059158c3696887a5a504bc0b55b

Relecteur neuf (n'a pas écrit ce code), 2026-11-10. Lus : `tour-5-portail-rapport.md`, `audit-banc2/portail-et-scripts.md`, `audit-banc2/depot.md`, `git log f1ff40b..79a092a6` et le diff de `scripts/bench/` (fusion-equipes.sh, relecture-verdict.py, tampon.sh, selftest.sh, selftest-verdict.py, selftest-quarantaine.txt, verifie-chantier.py, sortie-de-siege.py, test_ultra_bench.py). Expériences dans deux clones jetables sous le scratchpad (`relecture-portail/clone`, `clone2`), supprimés à la fin ; aucune branche créée dans le dépôt partagé ; aucun Gradle lancé (le selftest est remplacé par un bouchon dans le clone, commit « STUB », pour tester la porte du portail seule). Atelier `eq-tour-5-realisme` lu seulement.

En bref : les quatre forges de l'audit sont bien refusées, le cas honnête passe, les paires rouge/vert sont honnêtes. Mais un résumé inventé passe encore le juge avec un tampon écrit à la main (deux lignes, sans aucun XML) : le constat P3 de l'audit n'est pas fermé, contrairement à ce que dit le rapport.

#### Constats

##### Bloquant

Aucun.

##### Grave

**G1. Résumé inventé + tampon écrit à la main : le juge l'accepte (P3 de l'audit rejoué, il passe).** **corrigé** (68d0adc7 : le tampon de deux lignes est refusé, voir la question 1 ci-dessus ; G2, M6 et M7 pour ce qui passe encore). `unsealed()` (verifie-chantier.py) ne vérifie dans `selftest.ok` que deux lignes, `resume: <sha256 du résumé>` et `code: <arbre>`, toutes deux calculables par n'importe qui ; quand `junit/` est absent à côté du résumé, rien d'autre n'est demandé (ni `tampon.sh verifie`, ni XML). L'aide de test `seal_resume` de `test_ultra_bench.py` écrit d'ailleurs exactement ce tampon de deux lignes : les tests codifient que le juge accepte un tampon que `tampon.sh verifie` refuse. Le rapport dit « un résumé de tests inventés ne passe plus » et place la barre de la forge à « fabriquer des XML » (P2) : pour le juge, il n'en faut aucun.

###### Preuve d'alors (G1)

Preuve (clone, tête 79a092a6) : les 16 « vert : » recopiés des « pas verte » du juge, puis :

```
$ { echo "16 verts, 0 rouges, 0 en quarantaine, 0 sautés"; sed -n 's/^.* : garde \([^ ]*\) pas verte.*/vert : \1/p' juge-vide.txt | sort -u; echo "code testé : $(bash scripts/bench/tampon.sh code)"; } > lot/selftest-resume.txt
$ printf 'resume: %s\ncode: %s\n' "$(sha256sum < lot/selftest-resume.txt | cut -d' ' -f1)" "$(bash scripts/bench/tampon.sh code)" > lot/selftest.ok
$ python3 -I scripts/bench/verifie-chantier.py --resume lot/selftest-resume.txt
exit=1
chantier : 22 écarts
$ grep -c "pas verte\|résumé refusé" juge-forge.txt
0
$ bash scripts/bench/tampon.sh verifie lot ; echo verifie=$?
verifie=1
```

(Sans le tampon : `chantier : 39 écarts`, dont 16 « pas verte » et « résumé refusé ».) Correction proposée (≈ 0,5 h) : le juge ne lit un résumé que s'il a son `junit/` et que `tampon.sh resultats` passe (même barre que P2), ou s'il est une trace du portail commitée (`docs/audits/ultra/portail/…`, ajoutée par un commit « Portal traces of the merge … ») ; `seal_resume` doit fabriquer un `junit/` au lieu du tampon de deux lignes, et un cas « tampon écrit à la main, sans junit » refusé dans `judge_refuses_unsealed_summary`.

##### Moyen

**M1. La lecture des lignes de tableau de la relecture se contourne par une autre écriture, et refuse un tableau de décompte honnête.** `relecture-verdict.py` ne voit une cellule que si elle vaut exactement « bloquant » ou « grave », et tient pour « corrigé » toute ligne où le mot apparaît (sauf juste après « non » / « pas » suivi d'une espace). Avec la dernière ligne « Ancien verdict : 0 bloquant, 0 grave — fusion possible », sont ACCEPTÉES (essai direct du script) : cellule `bloquant (P1)` ; `Gravité : grave` ; `Grave ⚠` ; `bloquant, ouvert` ; ligne avec `pas encore corrigé` ; avec `non-corrigé` ; avec `corrigé ? non` ; avec `à corriger (les points corrigés sont plus bas)` ; une puce `- **Bloquant** : … (non corrigé).` ; un titre `### Bloquant` suivi d'un constat. Une branche réelle (bouchon de selftest) avec `bloquant (P1)` dans le tableau et un `tampon.sh` qui sort 0 : `n1: merged (3b682bc0), traces in docs/audits/ultra/portail/2026-11-10-tour-9-n1`, `exit=0`. À l'inverse, un tableau de décompte honnête (lignes `bloquant` / `0` et `grave` / `0`) est refusé (« a bloquant row of the review is not « corrigé » »), refus sans danger mais à tort. La règle sûre reste la dernière ligne (elle tient) ; le contrôle des tableaux est une aide, à décrire comme telle, ou à durcir (mot « bloquant »/« grave » au début d'une cellule ; « corrigé » cherché dans la dernière cellule seulement ; ligne de décompte « | bloquant | 0 | » permise).

**M2. Une étape qui échoue après une fusion verte laisse la fusion en place.** Le script annonce « A red step stops everything: the merge is undone », mais `cp`, `mkdir`, `git commit` des traces tournent sous `set -e` sans `reset`. Branche dont le `selftest.sh` (bouchon) ne laisse pas `selftest.ok` :

```
== n6 exit=1
t4-26-prise-clavier : oracle tenu (stub)
cp: cannot stat '/tmp/tmp.BJySD7TczW/selftest.ok': No such file or directory
   -> MERGED: 585534ec Merge team n6 of tour-9 (audit ultra) 7419aa73 review
?? docs/audits/ultra/portail/
```

Fusion restée sans traces, dossier de traces non suivi, lot temporaire non effacé. Correction : un `trap` qui fait `git reset --keep "$before"` (et retire la trace partielle) sur toute erreur entre la fusion et le commit des traces.

**M3. Règle (c) : une garde posée par un « chore: » est blanchie par le rouge honnête d'un test de même nom dans une autre classe.** `guard_origin` suit la garde par le nom de la fonction seulement : si `AProofTest#sameName` est gardé par son propre rouge, un « chore: » qui garde `BProofTest#sameName` n'est pas une origine (le parent « garde déjà » ce nom), la recherche remonte au rouge de A, qui nomme un test de `there`. Jouet (`adv3.py`, même forme que `judge_adv2.py`) :

```
chantier : 0 écarts
A honnête e340c51 accepté
B garde du chore blanchie par le rouge de A (même nom de méthode) 10130c0 ACCEPTÉ comme paire
```

Le rapport ne voit que le sens prudent (faux refus). Correction : suivre `Classe#méthode` (le parent garde-t-il la même classe, ou la classe dont celle-ci est renommée), ou exiger que le rouge nomme le test exact levé par le vert.

**M4. Le tampon (P2) se forge avec un XML d'une ligne.** `green_summary` rejoue le verdict avec le fichier des classes attendues du lot (`$lot/selftest-attendus.txt`), que le faussaire contrôle ; absent, aucune classe n'est attendue. Preuve (clone) : un `junit/TEST-x.xml` d'un seul `<testcase>` vert, le verdict rejoué, « code testé » ajouté : `forge-XML ecris=0`, `forge-XML verifie=0`. La barre « fabriquer des XML » du rapport est donc une ligne. Correction : recalculer les classes attendues (`selftest-attendus.py`) dans `tampon.sh` au lieu de lire celles du lot.

**M5. Un rejeu vidé passe le portail sans un mot.** Le portail exige une raison pour un rejeu retiré de `rejeux/`, mais pas pour un oracle changé dans le dossier : branche qui remplace `rejeu.absent=Rien n'a été joué pendant la prise` par `rejeu.absent=zzz texte jamais affiche` dans `rejeux/t4-26-prise-clavier/oracle.txt`, relecture minimale : `n5: merged (fb90681c)`, `exit=0` (bouchon de selftest et de rejeux). Le rejeu « tient » alors toujours. Correction : traiter une modification de `oracle.txt`/`start.txt`/`cmd-*` d'un rejeu comme un retrait (ligne « rejeu changé : <id> : <raison> » exigée).

##### Mineur

- **m1.** « fusion sans trace du portail » ne voit que les commits dont le sujet commence par « Merge team ». Fusion à la main `Merge eq/tour-9-n5 by hand` : `0` ligne « sans trace » ; la même avec le sujet « Merge team n5 … » : `f02815c4 … : fusion sans trace du portail`. Un squash ou une avance rapide ne se voient pas non plus.
- **m2.** `tampon.sh empreinte` (seul contrôle de `verifie`, donc de `human-tester.sh`) ne voit pas un fichier marqué `--skip-worktree` puis modifié : `skip-worktree: empreinte NON-VU code vu`. Geste délibéré ; `tampon.sh code` le voit.
- **m3.** « rejeu retiré : <id> : <raison> » accepte n'importe quelle raison (« non tranché » suffit) ; le seul vrai filet est `rejeux/` non vide. Voulu, à dire dans la consigne de relecture.
- **m4.** Pour le fusionneur : cette branche entre en conflit avec fx-int (`03b6fd52`, « bloquants » déjà fusionné) sur `docs/DECISIONS.md` (ajouts des deux côtés, union sans perte vérifiée dans le clone). Une fois fx-int pris, le juge compte 23 écarts anciens et non 22 : le 23e, `dd31ec4b « vert: TutoJourneyTest#theWholeCourse[techno Pro] … » : vert sans rouge`, vient du lot « bloquants » et l'ancien juge de fx-int le donne aussi (mêmes lignes, ordre à part, `diff` des ensembles vide).
- **m5.** Une équipe qui reprend fx-int après sa relecture (conflit d'ajout dans DECISIONS, PLAN-RESTE, journal) est refusée par la règle « changé après la relecture » (`realisme: changed after the review (52b502be…): docs/DECISIONS.md docs/audits/ultra/PLAN-RESTE.md docs/audits/ultra/banc-fiable-journal.md: not merged.`). Règle d'avant ce lot, non aggravée ; une relecture qui nomme le commit de reprise passe (voir plus bas). « bloquants » l'avait contournée en portant ses lignes dans fx-int d'abord.

#### Ce qui tient (rejoué)

- **Forges de l'audit**, clone (fx-int `03b6fd52` + ce lot + bouchon de selftest) : P1 forge2 (`tampon.sh` qui sort 0, t4-26 sorti, relecture « 1 bloquant » + verdict suffixé) → `exit=3`, les trois refus listés ; même branche avec « rejeu retiré : t4-26-prise-clavier : non tranché » → `no replay test left in docs/audits/ultra/rejeux/ after the merge: undone.`, `exit=5` ; P4 relecture laissée dans l'atelier → `no fresh review (…tour-9-p4-relecture.md) committed in eq/tour-9-p4: not merged.`, `exit=3` ; P2 `ecris` sans selftest → `tampon : pas de résumé vert du selftest`, `ecris=1`, `verifie=1`.
- **Nouvelles forges refusées** : tampon scellé puis XML changé → `N2 XML changé verifie=1` ; trace du portail écrite par la branche → `the branch touches the portal's traces: docs/audits/ultra/portail/2026-11-10-tour-9-x/fusion.txt: not merged.` ; résumé scellé d'un autre arbre de code → refusé (test du lot) ; un arbre qui ne diffère que par `docs/` garde le résumé, voulu (les tests ne lisent `docs/` qu'en commentaire).
- **Cas honnête** : la relecture actuelle de « bloquants » (`git show eq/tour-5-bloquants:…relecture.md`) → `exit=0` (deux « Relu : », le premier, 37d6a977, est le bon) ; celle de « réalisme » (c18af300 et l'atelier) est refusée à bon droit (« Ancien verdict : 0 bloquant, 1 grave — fusion impossible »). Simulation « réalisme » après correction : reprise de fx-int (conflits d'ajout résolus par union), nouvelle relecture qui nomme ce commit, ligne de tableau « G1 / grave / … / **Corrigé** en abc1234 » → `realisme: merged (08ec66d8), traces in docs/audits/ultra/portail/2026-11-10-tour-5-realisme`, `exit=0`, quatre traces commitées, `fusion.txt` complet, juge relancé avec les traces (pas de « sans trace » pour elle-même).
- **Règle (c)** : `judge_adv2.py` contre le juge de cette branche : les 11 cas « REFUSÉ » (G et H aussi : leur rouge nomme un autre test, refus juste) ; les témoins honnêtes du lot (G, H déplacé, I d'avant la discipline, J « sans code : … ») acceptés par `rule_c_adversarial`. Historique réel : 22 écarts sur la tête (39 = 22 + 16 « pas verte » + le refus du résumé vide), mêmes lignes que l'ancien juge sur fx-int + ce lot ; aucun vert honnête nouvellement refusé.
- **Empreinte** (`git diff --full-index`) : stable (retour à l'identique après essais), voit un octet ajouté à un `.ogg` (`binaire vu: empreinte oui code oui`) et un changement de mode (`mode vu: oui`).
- **Paires rouge/vert** : les rouges 043332d7, d3868c65, 6a81e3c0, 8bd0c866, f8718ff6, 9bfb5874 échouent chacun sur leur cas ; 1467e1b8 et 7e993b81 passent. Sept mutations sur la tête, chacune rougit son test : contrôle des tableaux retiré (`a review whose table holds an open grave is refused`), `green_summary` retiré de `ecris` (`a red summary: no stamp`), `unsealed` neutralisé (`a summary written by hand is refused`), `--full-index` retiré (`the fingerprint is the same whatever git's short names`), message de quarantaine ignoré (`another cause is red`), « le rouge nomme le test » retiré (`case A of judge_adv2 is refused`), juge non relancé après les traces (`the judge kept in the traces ran with the traces there`).
- `python3 -I scripts/bench/test_ultra_bench.py` → `ultra bench scripts: OK` (18 s).

Ancien verdict : 0 bloquant, 1 grave — fusion impossible

Ancien verdict : 0 bloquant, 1 grave — fusion impossible

Verdict : 0 bloquant, 0 grave — fusion possible
