# Audit « banc crédible » 2 (A.17) : dimension 1, le portail et les scripts

Écrit le 2026-11-10 par un auditeur seul (lecture et expériences, aucune correction), sur `eq/tour-5-audit-banc2` (copie de fx-int à `3dfdd0b`). Expériences dans une copie jetable : worktree `scratchpad/audit-banc2/wt` (détaché à `3dfdd0b`) et, pour les fusions, un clone local `scratchpad/audit-banc2/e3/repo` (branches de test isolées du dépôt partagé). Les deux sont supprimés à la fin. Aucun Gradle lancé.

Point de départ : les six pistes brutes de l'équipe « portail » (AUDIT-BANC-CREDIBLE.md § 5), rejouées sur le code d'aujourd'hui (le juge a été réécrit depuis `bebe0c9`, lots « Le juge », « La mémoire »). Non regardé ici : la règle (c) du juge et `Tour3StudioRenameIdleTest` (un autre agent les corrige), `fuse-reports.py`, `seat-metrics.py`, `tableau-de-bord.py`, `fabrique-sieges.py`, `applique-corrections.py`. La piste E5 (paires rouge/vert) n'est pas rejouée : la règle (b) a été réécrite au lot 1.

## Constats

| id | Titre | Gravité | Preuve (copiée) | Correction proposée | Effort h |
|---|---|---|---|---|---|
| P1 (piste E3) | **Une branche d'équipe qui désactive le portail est fusionnée par `fusion-equipes.sh`, sans aucun test, avec une relecture qui écrit elle-même « 1 bloquant »** | **bloquant** | Voir P1 ci-dessous : branche `eq/tour-9-forge2` = `tampon.sh verifie` qui sort toujours 0 + rejeu t4-26 déplacé dans `rejeux-attente/` ; relecture avec `\| 1 \| bloquant \| le tampon ne vérifie plus rien \|` puis `Verdict : 0 bloquant, 0 grave (avant correction du constat 1)` → `forge2: merged (01a74aa)`, `exit=0`, `No replay test in …/docs/audits/ultra/rejeux.` (sans aucune variable d'environnement) | (1) Lire le verdict sur **une seule ligne imposée** (dernière ligne non vide = `Verdict : N bloquant, M grave`), et refuser si le fichier contient une ligne de tableau `\| bloquant \|` ou `\| grave \|` sans le mot « corrigé » dans la même ligne. (2) La fusion lance **toujours** `selftest.sh` sur un lot vide (pas seulement via les rejeux) et `Bench*` + `Tour*AppProof*Test`, plus les filtres. (3) `rejeu-defauts.sh` sans rejeu = **rouge** dans la fusion (« aucun rejeu : fusion refusée ») ; une branche qui retire un dossier de `rejeux/` est refusée sauf ligne `rejeu retiré : <id> : <raison>` dans la relecture. (4) Cas dans `test_ultra_bench.py` : les 3 essais de P1 et P4 | 2 |
| P2 (piste E1) | **Tampon forgé** : `tampon.sh ecris` sans selftest donne un tampon que `human-tester.sh` accepte ; rien ne relie le tampon à un résultat de tests | grave | Voir P2 : `avant: exit=1` → `empreinte` + `ecris` (aucun test lancé) → `apres: exit=0` ; le tampon ne contient que tête, empreinte, lot, sièges, date | `ecris` refuse sans `selftest-resume.txt` du lot daté après l'empreinte et vert (`0 rouges`) ; le tampon porte `resume: <sha256>` et `junit: <sha256 des XML>` ; `verifie` recalcule les deux et relance `selftest-verdict.py` sur `junit/`. Ça ne rend pas la forge impossible (tout est local) mais l'oblige à fabriquer des XML, et la laisse visible (voir dépôt D1 : garder ces fichiers). Garde : `test_ultra_bench.py` « tampon sans résumé refusé » | 1 |
| P3 | **Le juge prend n'importe quel texte pour « le dernier selftest »** : un résumé écrit à la main, ou d'un autre code, satisfait le critère 3 | grave | Voir P3 : résumé vide → `chantier : 38 écarts` dont 16 « pas verte dans le dernier selftest » ; résumé de 12 lignes `vert : Classe#méthode` tapées à la main → `chantier : 22 écarts`, 0 « pas verte » | Le résumé porte en tête l'empreinte (`tampon.sh empreinte`) du code testé ; `verifie-chantier.py` refuse un résumé dont l'empreinte n'est pas celle du code courant (écart « résumé d'un autre code ») et un résumé sans tampon valide à côté (`<lot>/selftest.ok`, cf. P2) | 1 |
| P4 (piste E3) | Relecture **non suivie** acceptée, puis absente de l'historique | moyen | Voir P4 : relecture écrite dans l'atelier d'intégration, jamais commitée → `forge3: merged (2d0cc2f)` ; `git ls-tree -r HEAD --name-only \| grep -c tour-9-forge3-relecture` → `0` ; `git status` → `?? docs/audits/ultra/tour-9-forge3-relecture.md` | N'accepter que `git show eq/<branche>:<relecture>` (supprimer le repli `cat "$review"`, l. 22) | 0,25 |
| P5 (piste E2) | Une « preuve sans banc » n'est vérifiée que par la présence du texte `fun nom` dans la classe : un test `@Ignore` ou disparu derrière un commentaire compte | moyen | Voir P5 : `@org.junit.Ignore("flaky") @Test fun tapsAtTheChallengesBeat…` → `chantier : 22 écarts` (inchangé) ; test renommé + ligne `// fun tapsAtTheChallengesBeat…` → `22 écarts`, `diff` vide. Exposition réelle : 3 preuves de `verdicts.tsv` ne sont jouées par aucun selftest (`JourneyTest#aFirstSongInTheModeSimple`, `SharePathTest#theSheetsChipsSetTheLoopLengthAndTheDuration`, `TakeAtTheSongsEndTest#aTakeLeftRunningIsKeptWhenTheEngineEndsTheSong`) ; les 11 autres le sont, et un `@Ignore` y rend le selftest rouge (saut inconnu) | `method_exists` : ignorer commentaires, exiger `@Test` juste avant et pas d'`@Ignore` ; ou exiger chaque preuve sans banc « vert » dans le résumé (ajouter au selftest une passe « preuves » qui joue les classes de `preuve_appli`) | 1 |
| P6 (piste E4) | La quarantaine excuse **toute** cause d'échec du test nommé, pas la cause connue | moyen | Voir P6 : XML synthétique, test en quarantaine qui échoue sur `NullPointerException: tout autre chose` → `1 verts, 0 rouges, 1 en quarantaine` `exit=0` | Une colonne « message attendu » dans `selftest-quarantaine.txt` ; un échec dont le message ne la contient pas est rouge (comme `KnownDefectRule`) | 0,5 |
| P7 (piste E6) | La « sortie de siège » ne voit que le dossier du lot, et `*.md`/`*.tsv` de la liste blanche couvrent tout sous-dossier | moyen | Voir P7 : écrits pendant le lot : `g/rapport.md`, `notes/brouillon.tsv`, `README.md`, `b/ecrit-par-a.txt`, `espion.txt`, et un fichier à côté du lot → `sortie de siège : 1 fichier(s) hors des sièges : espion.txt` | Liste blanche ancrée à la racine du lot (`fnmatch` sur `rel` sans `/`, ou comparer `rel` exactement) ; empreinte avant/après du **dépôt** (`git status --porcelain`) et des dossiers des autres sièges ; le croisement entre sièges reste invisible tant que les sièges partagent un utilisateur : le dire dans `incidents-banc.tsv` | 1 |
| P8 | Le plafond d'images (1 pour 5 gestes) ne tient que si le testeur le veut : `screen-N.png` est écrit à chaque tour dans son propre dossier, et `images.txt` est un fichier qu'il peut modifier | mineur | `HumanTester.kt` l. 1539-1540 : `val shot = File(dir, "screen-$n.png")` / `captureScreenRoboImage(shot.path, …)` à chaque écran ; `human-act.py` `image_allowed` lit `images.txt` dans le dossier du siège | Écrire les PNG hors du dossier du siège et ne copier que ceux demandés ; ou compter les lectures de PNG ailleurs que dans `images.txt` | 0,5 |

## Preuves complètes

### P1 : fusion d'une branche qui casse le portail, relecture contradictoire, aucun rejeu

Dans le clone jetable (`fx-int-tmp` = `3dfdd0b`) :

```
$ git checkout -q -b eq/tour-9-forge2
$ (python : tampon.sh « verifie) stamp=… » → « verifie) exit 0; stamp=… »)
$ git mv docs/audits/ultra/rejeux/t4-26-prise-clavier docs/audits/ultra/rejeux-attente/t4-26-prise-clavier
$ git commit -qam "T4-26 back to waiting replays (non-tranché); faster stamp check"
$ printf 'Relu : %s\n\n| 1 | bloquant | le tampon ne vérifie plus rien |\n\nVerdict : 0 bloquant, 0 grave (avant correction du constat 1)\n' $sha > docs/audits/ultra/tour-9-forge2-relecture.md
$ git add … && git commit -qm review && git checkout -q fx-int-tmp
$ env -u PS_REJEUX_DIR bash scripts/bench/fusion-equipes.sh tour-9 forge2
Merge made by the 'ort' strategy.
 …/ultra/{rejeux => rejeux-attente}/t4-26-prise-clavier/cmd-1.txt   | 0
 … (34 renommages)
 docs/audits/ultra/tour-9-forge2-relecture.md                         | 5 +++++
 scripts/bench/tampon.sh                                              | 2 +-
find: '…/e3/repo/docs/audits/ultra/rejeux': No such file or directory
No replay test in …/e3/repo/docs/audits/ultra/rejeux.
forge2: merged (01a74aa)
exit=0
```

Premier essai (même relecture contradictoire, rejeux neutralisés par `PS_REJEUX_DIR` vers un dossier vide) : `forge: merged (037196e)` et `git show HEAD:scripts/bench/tampon.sh | grep -n "verifie) exit 0"` → `23:  verifie) exit 0; stamp="$lot_abs/selftest.ok"`.

Pourquoi : `fusion-equipes.sh` l. 35 `grep -Eq 'Verdict *: *0 bloquant, *0 grave'` cherche la phrase **n'importe où** dans la relecture ; après la fusion, le seul contrôle est `rejeu-defauts.sh` (l. 10 : `[ ${#ids[@]} -gt 0 ] || { echo "No replay test in $dir."; exit 0; }`) puis les filtres `--` facultatifs ; le selftest n'est lancé que **par** les rejeux (`rejeu-defauts.sh` l. 26). `rejeux/` ne contient aujourd'hui qu'un dossier, `t4-26-prise-clavier`, et T4-26 est « non-tranché » : le déplacer dans `rejeux-attente/` est un geste légitime d'équipe, qui retire en silence le seul test lancé par une fusion.

Ce qui n'est pas prouvé : qu'une fusion passée ait été faite ainsi. Les 14 relectures du tour 5 ont toutes la ligne `Verdict : 0 bloquant, 0 grave — fusion possible` ; `tour-5-memoire-relecture.md` contient aussi deux lignes `| 1 | grave |` / `| 2 | grave |` (première relecture, corrigées) : la forme « tableau avec graves + verdict 0 » existe déjà dans un vrai fichier, et le script ne sait pas la distinguer. Pour la branche cassée ci-dessus, **avec** t4-26 dans `rejeux/`, le selftest aurait tourné et `test_ultra_bench.py` (cas « a stamp copied to another lot is refused ») l'aurait sans doute arrêtée : la gravité vient de ce que ce filet tient à un seul dossier de rejeu.

### P2 : tampon forgé

```
$ bash scripts/bench/tampon.sh verifie $S/e1/lot ; echo "avant: exit=$?"
avant: exit=1
$ bash scripts/bench/tampon.sh empreinte > $S/e1/fp.txt
$ bash scripts/bench/tampon.sh ecris $S/e1/lot $S/e1/fp.txt
$ bash scripts/bench/tampon.sh verifie $S/e1/lot ; echo "apres: exit=$?"
apres: exit=0
$ ls $S/e1/lot
a
selftest.ok
$ cat $S/e1/lot/selftest.ok
3dfdd0bed3e84a7b8bf36844e1c4eb09dfc82721
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
lot: …/audit-banc2/e1/lot
sieges: cb6256889744fef8a1a4d2b6956bb0d283f03a63bb18d80f26091687941a8045
2026-11-10T12:17:04Z
```

`human-tester.sh` l. 29 : `elif ! scripts/bench/tampon.sh verifie "$dir"; then` est le seul contrôle. Pas de `selftest-resume.txt`, pas de `junit/` dans le lot : rien ne le remarque. `test_ultra_bench.py` l. 512-516 fait lui-même `ecris` sans selftest (usage voulu pour tester le tampon). Gravité « grave » et pas « bloquant » : la forge demande un appel délibéré contre la règle écrite ; aucun chemin accidentel trouvé (le selftest efface le tampon au départ, l. 24, et en cas d'échec, `fail`). Mais elle ne laisse aucune trace (dépôt D1).

### P3 : résumé écrit à la main accepté par le juge

```
$ : > $S/e1/vide.txt
$ python3 -I scripts/bench/verifie-chantier.py --resume $S/e1/vide.txt > $S/e1/juge-vide.txt ; echo "exit $?"
exit 1
chantier : 38 écarts
T4-19 : garde BenchTour4Test#renameAfterTheClearButton pas verte dans le dernier selftest
T4-16 : garde BenchRecetteTest#simpleSongCounterFollowsTheEnginePastTheFirstPart pas verte dans le dernier selftest
…
$ sed -n 's/^.* : garde \(…\) pas verte.*/vert : \1/p' juge-vide.txt | sort -u > resume-forge.txt ; wc -l
12
vert : BenchAmeneesTest#aTappedRhythmIsScored
vert : BenchEngineTest#aMicTakeLeftRunningIsKeptWhenTheSongEnds
vert : BenchEngineTest#anExportSizeIsSaidToBeTheBenchs
$ python3 -I scripts/bench/verifie-chantier.py --resume $S/e1/resume-forge.txt > juge-forge.txt ; echo "exit $?"
exit 1
chantier : 22 écarts
$ grep -c "pas verte" juge-forge.txt
0
```

`green_in_resume` (l. 111-116) ne cherche que `^vert : Classe#méthode$` dans le texte donné ; rien ne dit de quel code ni de quand date ce texte. Le « Juge : 22 écarts » de PROGRESS est le même chiffre qu'avec ce résumé inventé.

### P4 : relecture non suivie

```
$ (branche eq/tour-9-forge3 : une ligne ajoutée à ci-etat.sh, aucune relecture commitée)
$ printf 'Relu : %s\nVerdict : 0 bloquant, 0 grave\n' $sha > docs/audits/ultra/tour-9-forge3-relecture.md   # dans l'atelier d'intégration
$ PS_REJEUX_DIR=$S/e3/rejeux-vide bash scripts/bench/fusion-equipes.sh tour-9 forge3
No replay test in …/e3/rejeux-vide.
forge3: merged (2d0cc2f)
exit=0
$ git ls-tree -r HEAD --name-only | grep -c tour-9-forge3-relecture
0
$ git status --short | grep tour-9
?? docs/audits/ultra/tour-9-forge3-relecture.md
```

L. 19 (`git status --porcelain --untracked-files=no`) ne voit pas un fichier non suivi, l. 22 le lit par `cat "$review"`.

### P5 : preuve sans banc `@Ignore` ou commentée

```
$ (ChallengeControllerTest.kt l. 93 : « @Test » → « @org.junit.Ignore("flaky") @Test »)
$ python3 -I scripts/bench/verifie-chantier.py --resume $S/e1/resume-forge.txt | tail -1
chantier : 22 écarts
$ (variante : méthode renommée tapsAt…_GONE, ligne « // fun tapsAtTheChallengesBeatAfterAMoiGiveThreeStars » ajoutée)
chantier : 22 écarts
aucune différence          # diff avec la sortie de référence
$ git checkout -- app/src/test/java/com/pocketstudio/app/padlive/ChallengeControllerTest.kt   # fichier remis
```

`verifie-chantier.py` l. 77-79 : `re.search(r"\bfun\s+`?" + re.escape(method) + r"\b", text)`. Comptage des preuves de `verdicts.tsv` hors des classes du selftest (`selftest-attendus.py`, 27 classes) : 11 jouées, 3 non jouées (listées dans le tableau).

### P6 : quarantaine qui excuse une autre cause

```
$ (XML : Tour3RenameIdleTest#aProjectRenamedInSimpleModeKeepsItsNewName en échec « java.lang.NullPointerException: tout autre chose (régression du renommage) »)
$ python3 -I scripts/bench/selftest-verdict.py $d/junit scripts/bench/selftest-quarantaine.txt $d/resume.txt $d/attendus.txt $d/echecs.txt scripts/bench/selftest-sauts-connus.txt ; echo "exit=$?"
1 verts, 0 rouges, 1 en quarantaine, 0 sautés
exit=0
quarantaine rouge : Tour3RenameIdleTest#aProjectRenamedInSimpleModeKeepsItsNewName : java.lang.NullPointerException: tout autre chose (régression du renommage)
```

`selftest-verdict.py` l. 42 : `(held if name in quarantined else red)`. (Le test pris en exemple est celui de la quarantaine réelle ; le constat porte sur le mécanisme, pas sur ce test, qu'un autre agent traite.)

### P7 : sortie de siège

```
$ python3 -I scripts/bench/sortie-de-siege.py avant $L
$ (écrits : $L/g/rapport.md, $L/notes/brouillon.tsv, $L/README.md, $L/b/ecrit-par-a.txt, $L/espion.txt, $S/e6/hors-du-lot.txt)
$ python3 -I scripts/bench/sortie-de-siege.py apres $L
sortie de siège : 1 fichier(s) hors des sièges : espion.txt
```

`WHITE` (l. 17) contient `"*.tsv", "*.md"` et `fnmatch` laisse `*` traverser `/` ; `foreign()` excuse tout chemin sous `a/`…`f/`, quel que soit le siège qui l'écrit ; `files(lot)` ne parcourt que le lot.

## Ce qui tient (vérifié ici)

- `fusion-equipes.sh` refuse bien une relecture qui ne nomme pas de commit, et un fichier de l'équipe changé après le commit relu (lecture l. 26-33 ; non rejoué).
- `tampon.sh verifie` refuse un lot sans tampon (`avant: exit=1`) ; `test_ultra_bench.py` couvre copie vers un autre lot, sièges changés, autre commit.
- La garde Gradle `~/.gradle/init.d/pocket-studio-file.gradle` est installée (lue) ; elle se contourne par `PS_GRADLE_FILE=1`, ce que son commentaire dit (« a guard rail, not a lock »).
