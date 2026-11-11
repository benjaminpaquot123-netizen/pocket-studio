# Consignes communes aux équipes de correction, tour 5 (méthode v2)

Faites à partir de `docs/audits/ultra/GABARIT-CONSIGNES-EQUIPES-V2.md`, avec les deux fiches de `docs/AIDE-FUSIONS-TESTS.md` (PLAN-RESTE 7.3 et 7.4). Lis-les en entier avant de commencer.

## Les trois équipes du tour 5, et l'ordre de fusion

| Ordre | Zone | Ce qu'elle fait | Branche | Fichiers |
|---|---|---|---|---|
| 1 | `socle` (équipe « banc ») | Le socle du banc : base verte, décisions par lot, rejeux par étiquette, CI rapide en premier, règles gelées par paquet ; puis les lignes 7.1 à 7.7 de `PLAN-RESTE.md` | `eq/tour-5-socle` puis `eq/tour-5-banc7` (copies de `claude/socle-du-banc-alqcr3`, deux lots relus chacun) | `scripts/bench/`, `.github/workflows/ci.yml`, tests du banc (`app/src/test/.../human`), `docs/audits/ultra/` ; jamais les écrans de l'appli |
| 2 | `remix` | Chantier 10 de l'étape 4 : une seule carte conseillée, un bouton clair, l'alerte « la fin sera coupée » | `eq/tour-5-remix` | écrans et logique du Remix (`app/.../remix/`), leurs textes, ses tests |
| 3 | `courbes-accessibilite` (discussion principale) | Chantiers 8 (Courbes) et 9 (Accessibilité) de l'étape 4 : la courbe se montre, « Voir le réglage » y mène ; feuilles qui défilent en paysage, trois fiches « ? » | `eq/tour-5-courbes-accessibilite` | écrans du mixeur, feuilles, aide « ? » |

- Le banc fusionne **en premier** : les deux autres équipes jugent leurs corrections avec le banc à jour.
- L'ordre ci-dessus est celui de `scripts/bench/fusion-equipes.sh tour-5 socle banc7 remix courbes-accessibilite` (l'équipe banc livre deux lots : `socle`, puis `banc7`).
- Trois équipes au plus. Aucune n'écrit dans les fichiers d'une autre.

## L'atelier et les fichiers

- **Atelier** : le tien seul, `/home/user/eq-tour-5-<zone>` sur la branche `eq/tour-5-<zone>` (créé par le coordinateur avec `scripts/bench/equipe-atelier.sh cree tour-5 <zone>`). Tu ne touches jamais `/home/user/fx-int` ni l'atelier d'une autre équipe.
- **Fichiers** : ceux de ta zone d'après `docs/CARTE-CODE.md` et le tableau ci-dessus. Pour un fichier partagé qui n'est pas à toi, écris ta demande dans ton rapport (section « Demandes ») au lieu de le modifier.
- **Propriétaires du tour 5** :
  - `app/src/main/java/com/pocketstudio/app/ui/AppRoot.kt` → zone `courbes-accessibilite` (les feuilles et l'aide « ? » s'y branchent). Le Remix est déjà routé depuis `StemBoardScreen.kt` : s'il lui faut une ligne dans `AppRoot.kt`, il la demande.
  - `app/src/main/java/com/pocketstudio/app/rhythm/RhythmViewModel.kt` → zone `remix`, sections Remix, Step 5b, Step 5d, Stem board et Audio to notes seulement. `courbes-accessibilite` demande.
  - La liste `I18n.AREAS` (`project-model/src/main/kotlin/com/pocketstudio/model/i18n/I18n.kt`, ligne `val AREAS = listOf(`) → zone `remix` d'abord, puis `courbes-accessibilite`, selon l'ordre ci-dessous.
  - L'équipe `socle` ne touche aucun des trois.

## Les textes (`I18n.AREAS`) : ordre imposé et proposition

Les catalogues JSON sont déjà un fichier par zone (`project-model/src/main/resources/i18n/<langue>/<zone>.json`, 14 langues en JSON, 66 fichiers chacune ; le français est dans le code (`tr("…")`), sans JSON). Le seul endroit où deux équipes se marchent dessus est la liste codée `I18n.AREAS` : chaque équipe y ajoute une ligne, au même endroit.

**Ordre imposé pour ce tour :**
1. Une seule équipe à la fois modifie `AREAS` : `remix`, puis `courbes-accessibilite` (l'ordre de fusion).
2. Ta ligne s'ajoute **en fin de liste**, juste avant la parenthèse fermante, avec un commentaire d'une ligne (date, zone, ce que ces textes couvrent). Jamais au milieu, jamais une ligne d'un autre déplacée.
3. `courbes-accessibilite` ajoute sa ligne seulement **après** la fusion de `remix` : elle fusionne d'abord l'intégration dans sa branche, puis ajoute sa ligne derrière celle du Remix.
4. Tes textes vont dans ton propre fichier `<zone>.json` (le nom de ta ligne), dans les 14 langues en JSON. Jamais dans le fichier d'un autre lot.

**Proposition aux équipes du tour 5 (à trancher par elles, pas imposée) :** remplacer `AREAS` par un index généré, comme première correction de la zone i18n.
- Idée : la liste se fabrique depuis les noms des fichiers de `i18n/en/` (une tâche Gradle du module `project-model` qui écrit l'index au moment de la construction). Une équipe ajoute seulement son `<zone>.json` ; plus aucune ligne partagée, plus de conflit.
- Pourquoi c'est sûr : aujourd'hui les 66 noms de `AREAS` et les 66 fichiers de chaque langue correspondent un à un ; et `I18nCatalogTest#aFrenchTextHasOneTranslationAcrossAreas` refuse une même clé traduite de deux façons, donc l'ordre de la liste ne change aucun texte affiché (à revérifier par l'équipe qui le fait).
- Preuve attendue : commit « rouge: » avec un test qui échoue tant qu'un fichier JSON n'est pas lu par l'index, puis « vert: » ; `I18nCatalogTest` et `LangTest` verts.
- Si une équipe la prend, elle le dit dans son rapport et la fait **avant** toute autre ligne ajoutée à `AREAS` ; sa décision va dans `docs/decisions/tour-5-<zone>.md`. Sinon, l'ordre imposé reste.

## Avant de coder, pour chaque défaut grave ou bloquant

0. Si l'agent banc l'a déjà écrit, le rejeu attend dans `docs/audits/ultra/rejeux-attente/<id>/` : il doit dire « non tenu » avant ta correction (`PS_REJEUX_DIR=docs/audits/ultra/rejeux-attente scripts/bench/rejeu-defauts.sh <id>`). Avec ta correction, tu le déplaces dans `rejeux/` (sur ta branche, jamais dans fx-int avant la correction : `rejeux/` doit toujours être tout « tenu »). Un rejeu rouge est gardé dans `rejeux/_echecs/<date>/<id>/`.
1. Sinon, le rejeu du siège : `scripts/bench/rejeu-nouveau.sh <dossier du siège> <n> <id> 'attendu=<texte>' ['absent=<texte>']` (§12.5), puis `scripts/bench/rejeu-defauts.sh <id>` : il doit dire « oracle non tenu ». Le rejeu touche les boutons par leur étiquette (fiche, point 10).
2. La **liste des chemins** (§12.6) : tous les endroits qui lisent ou montrent la même donnée, chacun avec le test qui le couvrira, une ligne par chemin : `chemin: <écran> | test: Classe#méthode`.
3. Si la cause peut être le banc : pas de verdict sans les trois preuves du §12.3. Tu proposes, l'agent banc écrit la ligne de `docs/audits/ultra/verdicts.tsv`.

## Tests et Gradle

- Un test qui échoue avant la correction et vérifie ce que le testeur lisait à l'écran (pas un total).
- Gradle **uniquement** par `scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests <Classe>` (tests ciblés ; jamais la suite complète).
- **Rambarde Gradle** : `scripts/bench/equipe-atelier.sh` installe `~/.gradle/init.d/pocket-studio-file.gradle`. Un `./gradlew` lancé en direct sur pocket-studio est refusé avec le message « run Gradle through scripts/bench/gradle-file.sh ». C'est une rambarde, pas un verrou : ne la contourne pas en posant `PS_GRADLE_FILE=1` toi-même. Son test : `scripts/bench/test_garde_gradle.sh`.

## Commits

- Sur ta branche, par chemin (jamais `git add -A`, `git add .` ni `git commit -a` ; `git ls-tree -r HEAD | grep -c '^120000'` = 0). Rien de non commité à la fin.
- Messages en anglais, terminés par les deux lignes `Co-Authored-By` et `Claude-Session` que ta discussion te donne.
- Tu ne pousses pas et tu ne fusionnes pas : c'est l'agent de fusion (`scripts/bench/fusion-equipes.sh`), après la relecture neuve de ta branche.
- Interface en français via `tr()`, toute nouvelle chaîne dans les 14 catalogues JSON ; design selon `docs/DESIGN.md` ; captures ré-enregistrées seulement pour ce que tu changes, et regardées. Jamais ré-enregistrées : `recette`, `cut_screen`, `cut_screen_magnifier`, `remix_stem_edit_menu`, `rhythm_scrolled_under_part`.

## Le rapport

`docs/audits/ultra/tour-5-<zone>-rapport.md` (50 lignes au plus, sur ta branche) : par item, n° de synthèse, liste des chemins cochée, id du rejeu et son verdict avant et après, test, commits « rouge: » et « vert: », verdict proposé (appli, banc ou voulu), reste avec un propriétaire et un tour. Une ligne `tests: Classe#méthode, …` avec tes tests ciblés (l'agent de fusion les rejoue). Une section « Demandes » pour les fichiers partagés.

## La fiche des équipes (recopiée de `docs/AIDE-FUSIONS-TESTS.md`)

Avant de dire « j'ai fini » :
1. Tout est commité dans ta branche, par chemin. Aucun lien symbolique, rien sous `docs/audits/ultra/portail/`.
2. Si l'intégration a bougé, fusionne-la dans ta branche **maintenant**, résous, rejoue tes tests ciblés. Un conflit résolu après la relecture coûte une deuxième relecture.
3. Chaque correction a un test qui rougit sans elle : un commit « rouge: <sujet> » (des tests seulement), puis « vert: <le même sujet> » (le code). Vérifie-le : retire la ligne corrigée, le test doit rougir.
4. Le selftest se relance après ton **dernier** commit de code.
5. Tu retires ou changes un rejeu, tu apportes une preuve dans `rouges-constates/` : dis-le au relecteur, il doit l'écrire.
6. N'écris rien de plus fort que ta preuve (verdicts, rapport, décisions).
7. Regarde en entier les captures des écrans touchés, en Simple et en Pro.
8. Le suivi est à jour dans le lot : `verdicts.tsv`, PLAN-RESTE, journal.
9. Tes décisions vont dans `docs/decisions/tour-5-<zone>.md` (une ligne par choix : date, zone, choix, raison, retour arrière), **jamais** dans `docs/DECISIONS.md` : le portail y ajoute lui-même le lien vers ton fichier. Refusé à partir du paquet 2.
10. Un rejeu touche un bouton par son étiquette de test (`touche <étiquette>`, aussi `appui-long`, `double-touche`), jamais par ses pixels : un changement de mise en page ne le casse plus.

Puis : `scripts/bench/pre-vol.sh --avant-relecture tour-5 <zone>` ; chaque ligne « RECALÉ » se corrige avant de demander la relecture.

## La fiche du relecteur (recopiée de `docs/AIDE-FUSIONS-TESTS.md`)

- Ton fichier : `docs/audits/ultra/tour-5-<zone>-relecture.md`, commité **dans la branche**.
- Ligne 2 : « Relu : <sha du commit que tu as lu> ».
- Chaque constat bloquant ou grave corrigé porte « **corrigé** » en gras, sans « pas » ni « non » devant ; dans un tableau, la dernière case commence par « corrigé ».
- Rejeu retiré ou changé : une ligne « rejeu retiré : <id> : <raison> » ou « rejeu changé : <id> : <raison> ».
- Preuve rouge apportée : « rouge constaté : <sha du vert> : rejoué sur <sha de son parent> : <ce que tu as vu> ».
- Dernière ligne, seule : « Verdict : 0 bloquant, 0 grave » (« — fusion possible » est permis).
- Après toi, plus rien ne bouge dans la branche, sauf ton fichier et une fusion de l'intégration.

Puis, côté fusion : `scripts/bench/pre-vol.sh tour-5 socle banc7 remix courbes-accessibilite` (dans l'ordre de fusion).

## Après chaque fusion : `marque-corrige.sh`, obligatoire

L'agent de fusion, juste après chaque `fusion-equipes.sh` réussi, pour **chaque** item que la zone a corrigé :

```
scripts/bench/marque-corrige.sh <item> <sha du commit « vert: »> tour-5 docs/audits/ultra/portail/<date>-tour-5-<zone>/selftest-resume.txt
```

- `<item>` : l'id de `verdicts.tsv` (par exemple `T5-Cd-68`). Le script passe la ligne à « corrigé » avec ce commit dans `verdicts.tsv`, met la clé à « corrigé » (`corrige_au` = `tour-5`) dans `base.tsv`, puis lance `verifie-chantier.py --resume` avec le résumé du selftest gardé par le portail dans ses traces.
- Il ne commite rien : commit par chemin de `docs/audits/ultra/verdicts.tsv` et `docs/audits/ultra/base.tsv`, avant la zone suivante.
- Un item absent de `verdicts.tsv` arrête le script : l'agent banc écrit d'abord sa ligne.
- Le juge doit rester sans nouvel écart ; un écart nouveau se corrige avant de fusionner la zone suivante.
- Ensuite le tableau de bord se relance : `python3 -I scripts/bench/tableau-de-bord.py docs/audits/ultra/sieges/tour-5` (il écrit `sieges/tour-5/tableau-de-bord.md`).
