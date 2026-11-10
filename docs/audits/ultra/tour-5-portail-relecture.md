# Lot banc « portail » : relecture neuve
Relu : 79a092a6133d8059158c3696887a5a504bc0b55b

Relecteur neuf (n'a pas écrit ce code), 2026-11-10. Lus : `tour-5-portail-rapport.md`, `audit-banc2/portail-et-scripts.md`, `audit-banc2/depot.md`, `git log f1ff40b..79a092a6` et le diff de `scripts/bench/` (fusion-equipes.sh, relecture-verdict.py, tampon.sh, selftest.sh, selftest-verdict.py, selftest-quarantaine.txt, verifie-chantier.py, sortie-de-siege.py, test_ultra_bench.py). Expériences dans deux clones jetables sous le scratchpad (`relecture-portail/clone`, `clone2`), supprimés à la fin ; aucune branche créée dans le dépôt partagé ; aucun Gradle lancé (le selftest est remplacé par un bouchon dans le clone, commit « STUB », pour tester la porte du portail seule). Atelier `eq-tour-5-realisme` lu seulement.

En bref : les quatre forges de l'audit sont bien refusées, le cas honnête passe, les paires rouge/vert sont honnêtes. Mais un résumé inventé passe encore le juge avec un tampon écrit à la main (deux lignes, sans aucun XML) : le constat P3 de l'audit n'est pas fermé, contrairement à ce que dit le rapport.

## Constats

### Bloquant

Aucun.

### Grave

**G1. Résumé inventé + tampon écrit à la main : le juge l'accepte (P3 de l'audit rejoué, il passe).** `unsealed()` (verifie-chantier.py) ne vérifie dans `selftest.ok` que deux lignes, `resume: <sha256 du résumé>` et `code: <arbre>`, toutes deux calculables par n'importe qui ; quand `junit/` est absent à côté du résumé, rien d'autre n'est demandé (ni `tampon.sh verifie`, ni XML). L'aide de test `seal_resume` de `test_ultra_bench.py` écrit d'ailleurs exactement ce tampon de deux lignes : les tests codifient que le juge accepte un tampon que `tampon.sh verifie` refuse. Le rapport dit « un résumé de tests inventés ne passe plus » et place la barre de la forge à « fabriquer des XML » (P2) : pour le juge, il n'en faut aucun.

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

### Moyen

**M1. La lecture des lignes de tableau de la relecture se contourne par une autre écriture, et refuse un tableau de décompte honnête.** `relecture-verdict.py` ne voit une cellule que si elle vaut exactement « bloquant » ou « grave », et tient pour « corrigé » toute ligne où le mot apparaît (sauf juste après « non » / « pas » suivi d'une espace). Avec la dernière ligne « Verdict : 0 bloquant, 0 grave — fusion possible », sont ACCEPTÉES (essai direct du script) : cellule `bloquant (P1)` ; `Gravité : grave` ; `Grave ⚠` ; `bloquant, ouvert` ; ligne avec `pas encore corrigé` ; avec `non-corrigé` ; avec `corrigé ? non` ; avec `à corriger (les points corrigés sont plus bas)` ; une puce `- **Bloquant** : … (non corrigé).` ; un titre `### Bloquant` suivi d'un constat. Une branche réelle (bouchon de selftest) avec `bloquant (P1)` dans le tableau et un `tampon.sh` qui sort 0 : `n1: merged (3b682bc0), traces in docs/audits/ultra/portail/2026-11-10-tour-9-n1`, `exit=0`. À l'inverse, un tableau de décompte honnête (lignes `bloquant` / `0` et `grave` / `0`) est refusé (« a bloquant row of the review is not « corrigé » »), refus sans danger mais à tort. La règle sûre reste la dernière ligne (elle tient) ; le contrôle des tableaux est une aide, à décrire comme telle, ou à durcir (mot « bloquant »/« grave » au début d'une cellule ; « corrigé » cherché dans la dernière cellule seulement ; ligne de décompte « | bloquant | 0 | » permise).

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

### Mineur

- **m1.** « fusion sans trace du portail » ne voit que les commits dont le sujet commence par « Merge team ». Fusion à la main `Merge eq/tour-9-n5 by hand` : `0` ligne « sans trace » ; la même avec le sujet « Merge team n5 … » : `f02815c4 … : fusion sans trace du portail`. Un squash ou une avance rapide ne se voient pas non plus.
- **m2.** `tampon.sh empreinte` (seul contrôle de `verifie`, donc de `human-tester.sh`) ne voit pas un fichier marqué `--skip-worktree` puis modifié : `skip-worktree: empreinte NON-VU code vu`. Geste délibéré ; `tampon.sh code` le voit.
- **m3.** « rejeu retiré : <id> : <raison> » accepte n'importe quelle raison (« non tranché » suffit) ; le seul vrai filet est `rejeux/` non vide. Voulu, à dire dans la consigne de relecture.
- **m4.** Pour le fusionneur : cette branche entre en conflit avec fx-int (`03b6fd52`, « bloquants » déjà fusionné) sur `docs/DECISIONS.md` (ajouts des deux côtés, union sans perte vérifiée dans le clone). Une fois fx-int pris, le juge compte 23 écarts anciens et non 22 : le 23e, `dd31ec4b « vert: TutoJourneyTest#theWholeCourse[techno Pro] … » : vert sans rouge`, vient du lot « bloquants » et l'ancien juge de fx-int le donne aussi (mêmes lignes, ordre à part, `diff` des ensembles vide).
- **m5.** Une équipe qui reprend fx-int après sa relecture (conflit d'ajout dans DECISIONS, PLAN-RESTE, journal) est refusée par la règle « changé après la relecture » (`realisme: changed after the review (52b502be…): docs/DECISIONS.md docs/audits/ultra/PLAN-RESTE.md docs/audits/ultra/banc-fiable-journal.md: not merged.`). Règle d'avant ce lot, non aggravée ; une relecture qui nomme le commit de reprise passe (voir plus bas). « bloquants » l'avait contournée en portant ses lignes dans fx-int d'abord.

## Ce qui tient (rejoué)

- **Forges de l'audit**, clone (fx-int `03b6fd52` + ce lot + bouchon de selftest) : P1 forge2 (`tampon.sh` qui sort 0, t4-26 sorti, relecture « 1 bloquant » + verdict suffixé) → `exit=3`, les trois refus listés ; même branche avec « rejeu retiré : t4-26-prise-clavier : non tranché » → `no replay test left in docs/audits/ultra/rejeux/ after the merge: undone.`, `exit=5` ; P4 relecture laissée dans l'atelier → `no fresh review (…tour-9-p4-relecture.md) committed in eq/tour-9-p4: not merged.`, `exit=3` ; P2 `ecris` sans selftest → `tampon : pas de résumé vert du selftest`, `ecris=1`, `verifie=1`.
- **Nouvelles forges refusées** : tampon scellé puis XML changé → `N2 XML changé verifie=1` ; trace du portail écrite par la branche → `the branch touches the portal's traces: docs/audits/ultra/portail/2026-11-10-tour-9-x/fusion.txt: not merged.` ; résumé scellé d'un autre arbre de code → refusé (test du lot) ; un arbre qui ne diffère que par `docs/` garde le résumé, voulu (les tests ne lisent `docs/` qu'en commentaire).
- **Cas honnête** : la relecture actuelle de « bloquants » (`git show eq/tour-5-bloquants:…relecture.md`) → `exit=0` (deux « Relu : », le premier, 37d6a977, est le bon) ; celle de « réalisme » (c18af300 et l'atelier) est refusée à bon droit (« Verdict : 0 bloquant, 1 grave — fusion impossible »). Simulation « réalisme » après correction : reprise de fx-int (conflits d'ajout résolus par union), nouvelle relecture qui nomme ce commit, ligne de tableau « G1 / grave / … / **Corrigé** en abc1234 » → `realisme: merged (08ec66d8), traces in docs/audits/ultra/portail/2026-11-10-tour-5-realisme`, `exit=0`, quatre traces commitées, `fusion.txt` complet, juge relancé avec les traces (pas de « sans trace » pour elle-même).
- **Règle (c)** : `judge_adv2.py` contre le juge de cette branche : les 11 cas « REFUSÉ » (G et H aussi : leur rouge nomme un autre test, refus juste) ; les témoins honnêtes du lot (G, H déplacé, I d'avant la discipline, J « sans code : … ») acceptés par `rule_c_adversarial`. Historique réel : 22 écarts sur la tête (39 = 22 + 16 « pas verte » + le refus du résumé vide), mêmes lignes que l'ancien juge sur fx-int + ce lot ; aucun vert honnête nouvellement refusé.
- **Empreinte** (`git diff --full-index`) : stable (retour à l'identique après essais), voit un octet ajouté à un `.ogg` (`binaire vu: empreinte oui code oui`) et un changement de mode (`mode vu: oui`).
- **Paires rouge/vert** : les rouges 043332d7, d3868c65, 6a81e3c0, 8bd0c866, f8718ff6, 9bfb5874 échouent chacun sur leur cas ; 1467e1b8 et 7e993b81 passent. Sept mutations sur la tête, chacune rougit son test : contrôle des tableaux retiré (`a review whose table holds an open grave is refused`), `green_summary` retiré de `ecris` (`a red summary: no stamp`), `unsealed` neutralisé (`a summary written by hand is refused`), `--full-index` retiré (`the fingerprint is the same whatever git's short names`), message de quarantaine ignoré (`another cause is red`), « le rouge nomme le test » retiré (`case A of judge_adv2 is refused`), juge non relancé après les traces (`the judge kept in the traces ran with the traces there`).
- `python3 -I scripts/bench/test_ultra_bench.py` → `ultra bench scripts: OK` (18 s).

Verdict : 0 bloquant, 1 grave — fusion impossible
