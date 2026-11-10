# Lot banc « portail » (étape 3, suite du lot 5) : rapport de l'auteur

Atelier `/home/user/eq-tour-5-portail`, branche `eq/tour-5-portail`, partie de fx-int `f1ff40b`. Lignes A.18, A.19, A.22 (en partie) de `audit-banc2/SYNTHESE.md`, plus le moyen M1 de `tour-5-banc-fin-relecture.md` (règle (c) du juge). Rien dans `app/src/main` ; aucun test Kotlin touché ; zone : `scripts/bench/`, `docs/audits/ultra/`, `docs/DECISIONS.md`.

## En trois lignes (pour Benjamin)

- Le portail de fusion ne se laisse plus tromper : les quatre fusions truquées de l'audit sont refusées dans un clone jetable, et le contrôle du banc tourne à chaque fusion.
- Un tampon ou un résumé de tests inventés ne passent plus : le tampon exige de vrais résultats verts, et le juge refuse un résumé qui n'est pas celui du code d'aujourd'hui.
- Ce que les contrôles ont dit est maintenant gardé dans le dépôt à chaque fusion ; le juge suit enfin l'origine d'une garde (les 7 contournements de la dernière relecture sont refusés). Restent pour le lot suivant : le testeur qui lit les noms d'icônes (R1) et compter les défauts que le banc cache (MT1).

## Constats : preuve rouge, correction, vert

Chaque constat a d'abord un test qui échoue (commit `rouge:`), puis la correction (commit `vert:` du même nom). Tests dans `scripts/bench/test_ultra_bench.py` (nom de la fonction entre parenthèses dans le message).

| Constat | Rouge (ce que l'ancien code faisait) | Correction | Vert |
|---|---|---|---|
| **P1 bloquant + P4** fusion forgée (`fusion_forged`) | 043332d : la forge2 de l'audit (relecture « \| 1 \| bloquant \| » + phrase du verdict, les rejeux sortis de `rejeux/`, `PS_REJEUX_DIR` vers un dossier vide) : `forge2: merged`, sortie 0 | `fusion-equipes.sh` + `relecture-verdict.py` : verdict = dernière ligne non vide, exactement « Verdict : 0 bloquant, 0 grave » (« — fusion possible » permis) ; ligne de tableau « bloquant »/« grave » sans « corrigé » (et pas « non corrigé ») = refus ; relecture lue dans la branche seulement (repli `cat` retiré, P4) ; rejeu retiré par la branche sans ligne « rejeu retiré : <id> : <raison> » = refus ; `rejeux/` vide après fusion = fusion défaite ; selftest toujours lancé sur le code fusionné ; rejeux du dépôt (`env -u PS_REJEUX_DIR`) ; une branche ne touche jamais `docs/audits/ultra/portail/` | 1467e1b |
| **P2** tampon forgé (`stamp_needs_green_summary`) | d3868c6 : `ecris` sans aucun test donnait un tampon accepté | `tampon.sh ecris` exige `selftest-resume.txt` vert (« N verts, 0 rouges ») ET égal au verdict rejoué de `junit/` ; le tampon porte `resume:` et `junit:` (sha256) ; `verifie` recalcule les deux et rejoue le verdict | de9d84d |
| **P3** résumé inventé (`judge_refuses_unsealed_summary`) | 6a81e3c : un résumé écrit à la main était lu (le juge des tests jouets : 0 écart) | `tampon.sh code` = arbre git des fichiers hors `docs/` (index temporaire) ; `selftest.sh` écrit `code testé : <arbre>` dans le résumé, `ecris` le scelle (`code:`) et refuse un autre code ; le juge ne lit un résumé que si `selftest.ok` à côté le scelle pour le code jugé (et, si `junit/` est là, si les XML le donnent : `tampon.sh resultats`) ; sinon « résumé refusé » et aucune garde verte ; le juge tourne après le tampon dans `selftest.sh` | 81e0c68 |
| **D1** traces (`portal_traces`) | b3be3c1 : la fusion ne commitait rien | `fusion-equipes.sh` commite, juste après la fusion, `selftest-resume.txt`, `selftest.ok`, `selftest-chantier.txt` et `fusion.txt` (fusion, tête de branche, relecture, rejeux) sous `docs/audits/ultra/portail/<date>-<tour>-<zone>/` ; le juge : « fusion sans trace du portail » (fusion faite par un script qui écrit les traces) et « lot sans trace du portail » (lots `sieges/tour-N/lot*`, N ≥ 6) | 894033d |
| **D1, suite** (trouvé par la fusion à blanc) (`portal_traces`) | de0df4e : le juge gardé dans les traces est celui du selftest, lancé avant que les traces existent : il compte « fusion sans trace du portail » pour la fusion même qu'il documente (23 écarts dans la trace, 22 une fois commitée) | `fusion-equipes.sh` relance le juge une fois les traces écrites, sur le résumé scellé à côté | ba7e1d3 |
| **P2, suite** (trouvé par le 2e selftest final) (`fingerprint_stable`) | 8bd0c866 : « SELFTEST ROUGE : le code a changé pendant le selftest » sans aucun changement : l'empreinte hachait `git diff`, dont les lignes « index abc..def » utilisent des noms courts que git allonge quand le dépôt grossit (`tampon.sh code` ajoute des objets ; jouet : core.abbrev 7 contre 12) | `git diff --full-index` | 7e993b81 |
| **Règle (c)** du juge, moyen M1 de la relecture « banc-fin » (`rule_c_adversarial`) | f8718ff : jouet repris de `judge_adv2.py` ; l'ancien juge : A, B, C, D, E, E2, F, F2 « ACCEPTÉ », et la garde honnête déplacée (H) refusée | origine de la garde = dernier commit où un test garde une fonction de ce nom et son parent aucune (classe renommée, annotation reformatée, fichier déplacé ne sont pas une origine) ; elle doit être un « rouge: » qui nomme le test gardé, ou un ancêtre de 8f36fa3 (`DISCIPLINE_SINCE` et toute date retirés, `--debut-discipline` pour les jouets) ; le vert garde `@Test` sans `@Ignore` et touche du code hors tests et docs, ou écrit « sans code : <raison> » ; `@KnownDefect(id = …)` lu ; `inner`/`sealed`/`enum`/`object` sont des classes ; les paires se lisent depuis 8f36fa3 par ascendance | 5c95ef1 |
| **P5** preuve `@Ignore` ou en commentaire (`proof_must_run`) | 21c8018 | une preuve Kotlin de `preuve_appli` doit être une fonction de sa classe, commentaires ôtés, avec `@Test` et sans `@Ignore`/`@Disabled` | 6b9c72b |
| **P6** quarantaine qui excuse tout (`quarantine_needs_its_message`) | 9bfb587 : échec « NullPointerException: tout autre chose » excusé, sortie 0 | colonne « attendu : <texte> » ; un autre message est rouge, une ligne sans elle n'excuse rien ; les deux lignes réelles : « no project's ⋮ » | 7539576 |
| **P7** sortie de siège (`seat_exit_anchored`) | 7ee664e : `g/rapport.md`, `notes/brouillon.tsv` et un fichier du dépôt hors du lot passaient | liste blanche ancrée à la racine du lot ; `--depot` (le dépôt du script par défaut) : fichier neuf ou changé hors du lot = « dépôt : <chemin> » | f1da11f |

Découverte en route (règle (c)) : l'ancien juge lisait les paires avec `git log --since=2026-11-07` ; un seul commit antidaté (cas C) arrête ce parcours et **cache tous les verts plus anciens** (dans le jouet, A et B disparaissaient de la sortie). Corrigé dans le même vert (lecture par ascendance depuis 8f36fa3, parents d'abord) ; même liste de 111 commits rouge/vert sur l'historique réel.

## Preuves sur le vrai dépôt (copiées)

Forges de l'audit rejouées sur la tête de la branche (sans Gradle) :

```
$ tampon.sh empreinte > fp ; tampon.sh ecris lot fp ; tampon.sh verifie lot
tampon : pas de résumé vert du selftest dans …/forges/lot
ecris exit=1
verifie exit=1
$ verifie-chantier.py --resume resume-forge.txt        # les 12 « vert : … » tapés à la main de l'audit (P3)
résumé refusé : il ne nomme pas le code testé (« code testé : <arbre> », écrit par selftest.sh) (…) : aucune garde n'y est verte
chantier : 39 écarts                                     # 22 + 16 gardes « pas verte » + le refus (audit : 22)
```

Script adverse de la relecture « banc-fin » (`judge_adv2.py`, juge de cette branche) : les 11 cas sont refusés, y compris G et H, que la relecture appelait « témoins » mais dont le rouge parle d'un autre test (« rouge: unrelated … ») : refusés à bon droit par la règle « le rouge nomme Classe#méthode ». Les vrais témoins honnêtes sont dans `rule_c_adversarial` (G, H déplacé, I d'avant la discipline, J « sans code : … ») et sont acceptés.

Fusion à blanc dans un clone jetable (`scratchpad/portail/blanc/clone`, fx-int `f1ff40b` + un commit qui prend le nouveau portail ; branches d'essai seulement dans ce clone) :

```
== forge2   (tampon cassé, t4-26 sorti de rejeux/, relecture « | 1 | bloquant | » + verdict « (avant correction du constat 1) »)
forge2: review refused: the review's last line is not « Verdict : 0 bloquant, 0 grave » (…)
a bloquant row of the review is not « corrigé »: | 1 | bloquant | le tampon ne vérifie plus rien |
replay t4-26-prise-clavier taken out of rejeux/ without « rejeu retiré : t4-26-prise-clavier : <raison> » in the review: not merged.
exit=3
== forge2b  (même branche, relecture propre, rejeu sorti sans raison)       exit=3
== forge2c  (même branche, « rejeu retiré : t4-26-prise-clavier : non tranché ») 
forge2c: no replay test left in docs/audits/ultra/rejeux/ after the merge: undone.
exit=5
== forge3   (relecture laissée dans l'atelier, jamais commitée)
forge3: no fresh review (…tour-9-forge3-relecture.md) committed in eq/tour-9-forge3: not merged.
exit=3
```

Fusion à blanc honnête, même clone : la branche de ce lot (tête f1da11f) + une relecture d'essai, `PS_REJEUX_DIR` vers un dossier vide (ignoré) :

```
t4-26-prise-clavier : oracle tenu
portail: merged (5165cef3), traces in docs/audits/ultra/portail/2026-11-10-tour-9-portail
exit=0
$ git log --oneline -2
6268ed91 Portal traces of the merge of portail (tour-9)
5165cef3 Merge team portail of tour-9 (audit ultra)
$ git ls-tree -r --name-only HEAD docs/audits/ultra/portail
…/2026-11-10-tour-9-portail/fusion.txt, selftest-chantier.txt, selftest-resume.txt, selftest.ok
$ cat …/fusion.txt
fusion: 5165cef3373b0463a5a13875099eee948d651a0a
branche: eq/tour-9-portail
relu: f1da11f78a8284ecdd917f9d5005005570fce23d
rejeux :
  t4-26-prise-clavier : oracle tenu
$ head -1 …/selftest-resume.txt          → 165 verts, 0 rouges, 0 en quarantaine, 41 sautés
$ verifie-chantier.py --resume …/2026-11-10-tour-9-portail/selftest-resume.txt   → chantier : 22 écarts
```

Ce passage a montré un défaut (ligne « D1, suite » du tableau) : le `selftest-chantier.txt` gardé disait 23 écarts, dont « 5165cef3 … fusion sans trace du portail ». Corrigé (de0df4e / ba7e1d3) et vérifié par le jouet ; la fusion à blanc n'a pas été rejouée en entier après cette ligne (≈ 40 min de Gradle), mais la commande ajoutée, lancée à la main sur la trace du clone, donne bien 22.

Vérifications de fin (atelier, tête du lot) :

- `python3 -I scripts/bench/test_ultra_bench.py` → « ultra bench scripts: OK ».
- `bash scripts/bench/selftest.sh <dossier>` (PS_STALL_MIN=70) → « Selftest vert » (165 verts, 0 rouges, 0 en quarantaine, 41 sautés : les `drive` des sièges), sur la tête 7e993b81 ; `tampon.sh verifie` → 0. (Le 2e passage avait rougi « le code a changé » sans changement : ligne « P2, suite ».)
- `python3 -I scripts/bench/verifie-chantier.py --resume <dossier>/selftest-resume.txt` → « chantier : 22 écarts » ; mêmes lignes, sha ôtés, que le juge de la relecture « banc-fin » (`diff` vide).
- Suite complète `:app:testDebugUnitTest` par `gradle-file.sh`, PS_STALL_MIN=70 : BUILD SUCCESSFUL en 20 min 44 s, 8 156 tests, 0 échec (4 479 sautés : sièges et tests réservés à `-Phuman.dir`). Aucun fichier Kotlin n'est touché par ce lot.

## Ce qui reste

- **R1 (grave)** et **MT1 (grave)** : pas dans ce lot, volontairement (R1 change ce que voient tous les sièges ; MT1 est de la méthode). Lot suivant : A.20 (R1 + consignes de R2) et A.21 (MT1 + MT3) de PLAN-RESTE.
- **P8, R2-R5, D2, D3, D4, MT2-MT4** : MT2 fait en texte (critère 6 de PLAN-RESTE) ; le reste non fait (A.22).
- **P5, partie 2** : 3 preuves de `verdicts.tsv` ne sont jouées par aucun selftest (`JourneyTest#aFirstSongInTheModeSimple`, `SharePathTest#theSheetsChipsSetTheLoopLengthAndTheDuration`, `TakeAtTheSongsEndTest#aTakeLeftRunningIsKeptWhenTheEngineEndsTheSong`) : le juge vérifie maintenant qu'elles tournent (`@Test`, pas d'`@Ignore`), pas qu'elles sont vertes. Piste : une passe « preuves » au selftest.
- **Le portail lance le code de la branche** : une branche qui affaiblit à la fois un script et son test dans `test_ultra_bench.py` n'est arrêtée que par la relecture neuve. La forge reste possible localement (fabriquer des XML JUnit, un tampon, un résumé) ; elle se voit désormais dans les traces commitées.
- **Lots de sièges** : rien ne commite encore automatiquement les trois fichiers du selftest à côté des sièges ; le juge le demande à partir du tour 6 (« lot sans trace du portail »). À faire par celui qui commite les sièges du tour 6.
- Coût : une fusion lance deux selftests (le sien et celui des rejeux) ; accepté (DECISIONS).
- La règle (c) suit une garde par le nom de sa fonction : deux tests de même nom dans deux classes, l'un déjà gardé, font refuser une garde honnête de l'autre (prudent, pas dangereux).

## À rejouer à la fusion

- `python3 -I scripts/bench/test_ultra_bench.py` (toutes les fonctions ci-dessus).
- `bash scripts/bench/selftest.sh <dossier>` puis `python3 -I scripts/bench/verifie-chantier.py --resume <dossier>/selftest-resume.txt` : 22 écarts, mêmes lignes qu'avant le lot.
- Attention : la fusion de CE lot se fait encore avec l'ancien `fusion-equipes.sh` (celui de fx-int) ; la suivante utilisera le nouveau, qui commitera ses traces. Après la fusion, `git log --merges` : la fusion de ce lot ne compte pas comme « sans trace » (le script de son premier parent n'écrit pas les traces).
- Les commits de ce lot ont des sha à 8 caractères dans le journal (le dépôt a grossi) ; le juge les lit tels quels.
- Scripts touchés : `fusion-equipes.sh`, `relecture-verdict.py` (nouveau), `tampon.sh`, `selftest.sh`, `selftest-verdict.py`, `selftest-quarantaine.txt`, `verifie-chantier.py`, `sortie-de-siege.py`, `test_ultra_bench.py`.
