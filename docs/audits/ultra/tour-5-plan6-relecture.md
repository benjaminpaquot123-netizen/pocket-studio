Relecture du lot « plan6 » (tour 5, zone plan6) par un relecteur neuf
Relu : 7149311650d60546f2ec4746d46388f7bda844b7

Portée : `git diff fx-int...HEAD` (14 fichiers, uniquement des documents ; aucun script, aucun code, aucun binaire). fx-int n'a pas bougé depuis la base (62233649) ; `git merge-tree` fx-int HEAD : fusion sans conflit. Rien n'a été écrit dans l'arbre réel : les essais ont eu lieu sur une copie temporaire.

## 1. Tableau des sièges : conforme
- 8 sièges : A a, A b, A c, B a, B b (5 à mission : simple, padlive, exports, remix-voix, tuto-pro), B c (siège Pro en plus, `fresh=reel`, mission `libre`, réponse 4B), C a (tablette `tablet8` 130 %) et C b (téléphone 100 %) en mission `libre`, `gestes.max=100`, budget de 30 images écrit dans le profil (réponse 1A).
- 5 langues : hi, fr, es, de, ar. A a = hindi, `phone360`, 130 %, `fresh=reel`, sans mode (réglages d'origine de #27) ; A b = fr, `phone`, 100 %, simple (réglages d'origine de #117). Conforme au § 4 du plan et à la décision.
- `fabrique-sieges.py <copie>/tour-6 --tour tour-6` : « 8 sièges », code 0, 8 `start.txt` (options `fresh=reel`, `main=gauche` bien écrites), `sieges.tsv` à 11 colonnes, ids `t6-Aa-1` à `t6-Bc-4` (4 promesses libres par siège, 24 en tout, = `promesses-numerotees.tsv`), 8 briefs. Missions toutes connues de GOALS.
- Le rapport de zone passe `rapport-zone.py` (accepté) ; les trois `Classe#méthode` existent (SeatOptionsParseTest, BenchSeatOptionsTest).

## 2. Liste de lancement : commandes et sorties
Vérifié existant et conforme à ce que les scripts impriment : `git log --grep "Merge team mixwake of tour-5"` → `f2051def` (présent) ; `fabrique-sieges.py` → « 8 sièges » ; `pre-vol.sh --ci` → dernière ligne « PRÉ-VOL : PRÊT (N à voir). » (pre-vol.sh l. 38) ; `ci-etat.sh <sha> --ecris <hors-banc.tsv>` → `ci_build`/`ci_emulateur` (il réécrit les deux lignes `?`, au format du tour 5) ; `ci.yml` a bien `workflow_dispatch` ; `verifie-chantier.py --resume` → « chantier : N écarts » ; le `grep -cE "^(2b34c216|…) "` donne 7 sur la trace mixwake (24 écarts au total : chiffre du plan exact) ; le `grep -lE "^(touche|appui-long|double-touche) [0-9]"` donne bien 279 fichiers dans les 8 `t5-0*` (et 0 dans `rejeux/`) ; `grep -cP "\t\?$" depart.txt` donne 8 aujourd'hui, 0 attendu au lancement. Les étapes 2, 3, 4, 5 disent honnêtement « aujourd'hui : non » (rejeux6, journal des tenus et retest v6 pas encore faits) : ce sont des cibles, pas des sorties actuelles.

## 3. defauts-caches.tsv et hors-banc.tsv : format bon, bancs verts
- B24 à B26 : 11 colonnes comme B20 à B23, source « remarque », vu « non », fichiers cités existants (`tour-5-hardtechno-rapport.md`, `tour-5-mixwake-rapport.md`, `auto-audit-test-apk-2026-11-12.md`).
- `python3 -I scripts/bench/defauts-caches.py` : 26 défauts (9 vus, 3 en partie, 14 cachés), « 0 écart(s), critère 9 rempli », 85 % inchangé (17/20). `test_defauts_caches.py` vert. `python3 scripts/bench/test_ultra_bench.py` : « ultra bench scripts: OK ». `test_socle.py` : 15 tests OK. `tableau-de-bord.py` tour-5 + tour-6 : passe, le tour-5 ne change pas, le tour-6 sort en « ? » sur D, E, G, H, I comme annoncé (H lit bien `recette_points_rouges` = 4).
- `telephone.tsv` : 4 lignes rouges = les 4 de `hors-banc.tsv` ; les liens `cache_id` (B02, B07, B24 à B26) et `verdicts.tsv` (T5-Ad-43 et T5-Fb-13 « ouvert », T5-Ec-28 corrigé) sont exacts.

## 4. Ce qui change sur fx-int quand `sieges/tour-6/` existe (aucun blocage pour les lots tour 5 restants)
- `fusion-equipes.sh` l. 157 prend le dernier tour (`sort -V`, `rounds[-1]`) : à chaque fusion d'un lot tour 5, le tableau de bord est calculé sur tour-5 + tour-6 et écrit dans `sieges/tour-6/tableau-de-bord.md`, plus dans `sieges/tour-5/`. Les `base.tsv`, `clusters*`, `promesses.tsv` restent ceux de `sieges/<tour du lot>` (tour-5). Les lots tour 5 ne touchent pas ce fichier : pas de conflit.
- `sieges/tour-5/tableau-de-bord.md` n'est plus mis à jour (le 85 % se lit alors dans celui du tour 6, comme le plan le dit). « Arrêt de l'audit possible » se lit désormais sur tour-5 + tour-6 (tour 6 jamais rempli : « non », sans conséquence pour la fusion).
- `verifie-chantier.py` D1 (`lot_trace_gaps`) ne regarde que les dossiers `lot*` de tour ≥ 6 : sans `lot*` committé, aucun écart ajouté. Dès qu'un `lotA` est committé sans ses traces, c'est un écart : c'est prévu par la liste (étape 8 à 11, selftest avant commit).
- La seule autre branche qui touche un fichier partagé du suivi est `eq/tour-5-juge6` (`verdicts.tsv`) : aucun recouvrement avec ce lot.

## 5. Dépôt public
Pas de courriel, mot de passe, jeton, numéro ni fichier binaire dans le diff. Seules données « personnelles » : le prénom de Benjamin et le chemin `/home/user/fx-int`, déjà dans le dépôt (même chemin dans `GABARIT-CONSIGNES-*`, `META-AUDIT.md`). Aucun son, aucune capture.

## Constats
1. **Moyen** : étape 11 de la liste contredite par une décision. `docs/decisions/tour-5-plan6.md` dit que `lot*/start.txt`, `briefs/` et `sieges.tsv` ne sont pas committés, mais l'étape 8 les génère dans l'arbre de fx-int et l'étape 11 exige `git status --short sieges/tour-6/` vide après le commit de `depart.txt`, `hors-banc.tsv` et du lot A : `briefs/` et `sieges.tsv` resteraient non suivis. À trancher : les committer, ou les écarter (`.gitignore` ou retrait) avant l'étape 11.
2. **Moyen** : seule la procédure du lot 1 est écrite. Relancer `fabrique-sieges.py` avant le lot 2 refait aussi `lotC` (et `lotA`) : `lotC` sans traces serait un écart du juge (D1) pendant le selftest du lot 2 ; même cas pour `lotB` au lot 3. Il faut dire « `rm -r lotC` après la fabrication, avant le lot 2 » et son pendant au lot 3.
3. **Mineur** : `docs/decisions/tour-5-plan6.md` garde la ligne « Juge du chantier : lancement permis à N ≤ 6 écarts » au-dessus de celle qui la remplace (« 7 écarts nommés »). Le plan, lui, ne dit plus que 7. Ajouter « remplacée par la ligne suivante » à la première.
4. **Mineur** : `PROMPT-FABLE-TOUR-6.md` parle encore de 6 testeurs, de 8 agents et de « réponse 2A : 6 testeurs » : c'est le prompt de l'audit du plan, d'avant la révision (8 sièges, 10 agents). À marquer « version d'avant révision » en tête, pour qu'on ne le recolle pas tel quel.
5. **Mineur** : l'étape 3 (`grep "^(touche|appui-long|double-touche) [0-9]"`) ne voit pas une étiquette qui commencerait par un chiffre, et ne dit rien des `glisse` par pixels (il n'existe pas de forme à étiquette pour `glisse`, comme `test_socle.py`). Sans conséquence aujourd'hui.

Aucun constat bloquant ni grave : le lot ne contient que des documents et des fiches, rien n'est relancé, aucun script ni test n'est touché, et les contrôles du banc restent verts.

Verdict : 0 bloquant, 0 grave — fusion possible
