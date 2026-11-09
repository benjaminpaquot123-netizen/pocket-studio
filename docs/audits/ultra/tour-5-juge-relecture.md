# Relecture critique du lot 1 « Le juge » (lignes A.3, A.4, A.10, A.12)
Relu : 65a17b81276118d8899dcdfba5a924b92eabe910

Plage relue : `git diff b243996..65a17b8` (12 commits). Relecteur neuf, qui n'a rien écrit du lot.
Méthode : lecture complète, rejeu de chaque commit rouge puis vert, 63 mutations des scripts et des
données, rejeu du verdict sur les résultats JUnit de l'auteur, lancement du juge sur le dépôt réel.

## Ce qui tient

- `python3 -I scripts/bench/test_ultra_bench.py` : « ultra bench scripts: OK », sortie 0, environ 4 s.
  Les trois scripts n'importent que la bibliothèque standard (argparse, json, os, re, subprocess, sys,
  pathlib, xml.etree).
- Les cinq paires rouge/vert sont vraiment rouges puis vertes (chaque commit extrait et rejoué) :
  004e366 → 7f5a0ce, c3eebf7 → 7e33ec5, 03c768c → 8180d82, 0a4d552 → a460613, c39ca77 → 3dab50a.
- Le juge sur le dépôt réel, avec le résumé rejoué de l'auteur : « chantier : 24 écarts », sortie 1.
  Les 24 sont de vrais écarts selon la règle : 11 lignes « banc » sans preuve hors banc, 3 lignes
  « appli » sans preuve (T4-01, T5-Fc-31, T5-TEL10), 6 paires boiteuses (0b1cbbc, dcc3367, 87b6693
  verts sans rouge ; 8f36fa3, df794a9, d503254 rouges sans vert), 4 lignes cochées du journal sans
  preuve (4.0, 4.2, 4.3, 4.0 (b), lignes 60, 62, 63, 69). Identique au rapport de l'auteur.
- La règle n'est pas affaiblie par 3dab50a. Les douze sauts connus sont tous `Bench*Test#drive`,
  hérité de `HumanTester.kt` (« only with -Phuman.dir »). Les 41 sauts du lot sont 30 cas paramétrés
  de `BenchAmeneesProfilesTest#drive[...]` et 11 `drive` simples. Chacune des douze classes garde au
  moins un test vert. Une classe sautée en entier reste rouge même avec ses sauts connus (vérifié en
  vidant `BenchSelfTest` de son seul test vert : « rouge : BenchSelfTest#* : aucun test non sauté »,
  sortie 1).
- Le juge non bloquant est dit honnêtement : commentaire dans `selftest.sh`, ligne dans
  `docs/DECISIONS.md`, point 6 du rapport. Sa sortie est gardée dans `<lot>/selftest-chantier.txt`
  et affichée.
- Les corrections de `verdicts.tsv` (b173bcf) sont toutes appuyées : tel-03, tel-05, tel-08, tel-11,
  tel-14 en échec le 2026-11-08 dans `retours-benjamin.md` (lignes 11 à 42) ; le commit `ffd58ce`
  existe, date du 2026-11-08, et c'est bien lui qui ajoute
  `BenchPlayThenTabsTest#theBarCounterGoesOnWhileTheSongPlays`. Aucune valeur inventée.
- Tous les identifiants `@KnownDefect` réels (T5-ZIP, T5-ALERTE, T5-8MES, T5-REMIXFIN,
  T5-DEFI-TENDANCE, T5-DEFI-ECOUTE) sont portés par la colonne `garde_id`. Plus aucun `@Ignore`
  dans les preuves du tour 5.
- Fichiers absents : résumé absent ou dossier → un écart de plus, sortie 1, sans plantage ;
  dossier JUnit absent → tout rouge, sortie 1 ; liste des sauts absente → 41 rouges, sortie 1.
  Tout échoue du côté sûr.

## Constats

| # | Gravité | Quoi | Preuve (commande et résultat) | Correction proposée |
|---|---|---|---|---|
| 1 | moyen | Le câblage de `selftest.sh` n'est vérifié que par présence de texte. On peut ne plus passer la liste des classes attendues au verdict (A.4 perdue), figer cette liste en dur, ou ignorer un verdict rouge : aucun test ne rougit. | Mutations S04, S06, S07 : `test_ultra_bench.py` reste « OK ». Sans la liste attendue, `BenchSelfTest` sautée en entier donne « 129 verts, 0 rouges », sortie 0, au lieu de 1 rouge, sortie 1. | Un test qui lance `selftest.sh` avec un faux Gradle (ou un mode à blanc) sur un dossier JUnit où une classe attendue est sautée en entier, et qui exige une sortie non nulle. |
| 2 | moyen | Le test d'acceptation de A.3 (« un `@KnownDefect` rendu vert à la main rend le selftest rouge ») n'a pas été rejoué en vrai, et le selftest complet n'a pas été relancé après 3dab50a : pas de tampon vert sur la tête relue. Le rapport le dit honnêtement. | Rapport de l'auteur, point sur les limites ; `selftest-gradle-echecs.txt` du lot contient « renommage » ; seul le rejeu hors ligne donne « 130 verts, 0 rouges, 1 en quarantaine, 41 sautés ». La règle `KnownDefectRule` est bien branchée dans les trois classes, lu mais pas exécuté. | Relancer `selftest.sh` une fois sur la tête, garder le tampon ; rejouer une fois le cas « défaut connu corrigé à la main » par un test Gradle ciblé. |
| 3 | moyen | L'appel du juge dans `marque-corrige.sh` n'a aucun test. | Mutation C01 (appel sans `--resume`) : « OK ». | Un test qui lance `marque-corrige.sh` sur un dépôt jouet sans résumé et exige le refus. |
| 4 | moyen | Rien ne borne le contenu de `selftest-sauts-connus.txt`. On peut y inscrire un vrai test : son saut devient silencieux tant que sa classe garde un autre test vert. | Mutation D01 (une ligne d'un vrai test ajoutée) : « OK ». | Un test qui exige que chaque ligne finisse par `#drive`, ou une raison écrite par ligne et vérifiée. |
| 5 | mineur | Paires du journal : le contrôle « le rouge précède le vert » et le contrôle des préfixes « rouge: / vert: » se masquent l'un l'autre. | Mutations M14 et M15 survivent chacune ; le seul cas à l'envers ne cherche que « ligne cochée A.4 : paire ». | Deux cas séparés, chacun avec son message exact. |
| 6 | mineur | Paire par sujet identique jamais testée, alors que la vraie paire c39ca77 / 3dab50a s'appuie dessus. | Mutation M23 (égalité de sujet retirée) : « OK ». | Un cas de test : même sujet, aucun nom de test cité. |
| 7 | mineur | Essai daté en échec : l'exigence d'un numéro `tel-NN` n'est pas testée, et l'essai n'est lié ni à la colonne « téléphone » de la ligne ni à `recette.json`. | Mutation M29 : « OK ». Lecture de `dated_failed_trial`. | Un cas « ❌ + date sans tel-NN → pas une preuve » ; exiger que le tel-NN soit celui de la ligne. |
| 8 | mineur | Saut connu : le sens « `drive` ne couvre pas `driveX` » n'est pas testé. | Mutation V04 (couverture par préfixe) : « OK ». | Un cas `driveX` sauté, attendu rouge. |
| 9 | mineur | `selftest-attendus.py` : le test « un `@KnownDefect` en commentaire ne compte pas » ne teste rien, le commentaire est placé avant `class PlainTest`, hors de tout corps de classe. | Mutation A03 : « OK ». | Mettre le commentaire dans le corps d'une classe sans défaut connu. |
| 10 | mineur | Une preuve « appli » n'est jamais vérifiée verte dans le résumé (seule la garde de banc l'est) : une preuve sautée, rouge ou jamais lancée compte quand même. | Lecture de `check` : `garde_verte` n'est appelé que pour `garde_banc`. L'audit ne le demandait pas. | Exiger « vert : » pour la preuve aussi, ou le noter comme limite. |
| 11 | mineur | `preuve: script <chemin>` accepte n'importe quel fichier existant. Assouplissement absent du texte de l'audit, non utilisé à ce jour. | `grep -c 'preuve: script' banc-fiable-journal.md` → 0. | Restreindre à `scripts/bench/` et à un fichier exécutable, ou retirer. |
| 12 | mineur | `verifie-chantier.py` plante avec une trace Python si `verdicts.tsv` manque ; `--preuves` sur un item inconnu n'écrit rien, sortie 0. | `--root <dossier vide>` : `FileNotFoundError`, sortie 1. `--preuves ZZZ` : rien, sortie 0. | Message d'une ligne et sortie 2 ; « item inconnu », sortie 1. |
| 13 | mineur | Le commit rouge 0a4d552 est rouge par plantage (script absent), pas par assertion. | Rejeu de 0a4d552 : `AssertionError` sur l'appel d'un fichier qui n'existe pas. | Rien à refaire ; à l'avenir, rouge par assertion. |
| 14 | mineur | Les messages du lanceur C++ ne sont vérifiés que par présence de chaîne ; le retrait de `garde_id` sur une vraie ligne n'est épinglé par aucun test. | Mutations E05 et D02 : « OK » (le juge, lui, afficherait l'écart). | Un test hôte qui lit la sortie d'un `known_defect_` ; épingler le dépôt réel quand il sera à 0 écart. |
| 15 | observation | Le test d'acceptation de A.4 s'appelle `skipped_class` au lieu de `classe_sautee` demandé par l'audit. | `grep -n 'def skipped_class' scripts/bench/test_ultra_bench.py`. | Renommer ou noter l'équivalence dans le rapport. |
| 16 | observation | Texte périmé « (rouge ; @Ignore) » dans `verdicts.tsv` pour T5-Cd-68, T5-Cc-41a, T5-Cc-30, T5-Ec-61, T5-Cc-27, alors que ces tests sont maintenant `@KnownDefect` et sortent « vert : » dans le résumé. | Lignes 14, 16, 18 de `verdicts.tsv` ; lignes 158 à 161 du résumé rejoué. | Remplacer par « défaut connu ». |
| 17 | observation | « Jamais un test Bench* » est décidé au nom de la classe seulement ; `@KnownDefect(id = "…")` en argument nommé, ou sur la même ligne qu'une autre annotation, ne serait pas vu. Aucun cas réel aujourd'hui. | Lecture des expressions `KNOWN` et `KNOWN_DEFECT`. | Élargir les deux expressions. |
| 18 | observation | Hors lot : `git checkout -- .` dans un atelier remplace les huit liens `third_party` par des dossiers vides. Arrivé pendant cette relecture, réparé (liens recréés vers `/home/user/pocket-studio/...`). | `git status --short` : de nouveau les huit lignes « T ». | Dans les consignes d'atelier, restaurer fichier par fichier. |

## Mutations essayées (63)

Attrapées (47) :
- `verifie-chantier.py` : M01 à M13, M16 à M22, M24 à M28, M30, M32.
- `selftest-verdict.py` : V01 à V03, V05 à V10.
- `selftest-attendus.py` : A01, A02, A04, A05.
- `selftest.sh` : S01 à S03.
- C++ (`engine_tests.cpp`, `padlive_tests.inc`) : E01 à E04.

Survivantes (16) :
- M14 : contrôle « rouge ancêtre du vert » désactivé dans les paires du journal.
- M15 : contrôle des préfixes « rouge: / vert: » désactivé.
- M23 : égalité de sujet retirée de `same_test`.
- M29 : essai daté en échec accepté sans `tel-NN`.
- M31 : écart « garde introuvable » retiré (contrôle antérieur au lot).
- V04 : un saut connu couvre par préfixe (`drive` couvre `driveX`).
- A03 : `@KnownDefect` dans un commentaire compté.
- S04 : liste attendue écrite en dur, script non appelé.
- S05 : liste des sauts remplacée par `/dev/null` (sens sûr : tout rouge).
- S06 : verdict rouge ignoré (antérieur au lot).
- S07 : classes attendues non passées au verdict.
- S08 : motif Gradle sans chiffres (sens sûr : « aucun résultat », rouge).
- C01 : juge appelé sans `--resume` dans `marque-corrige.sh`.
- E05 : messages du lanceur C++ inversés.
- D01 : un vrai test inscrit dans `selftest-sauts-connus.txt`.
- D02 : `garde_id` de T5-PADRETRIG retiré du vrai `verdicts.tsv`.
- Note : M14 et M15 se masquent l'une l'autre ; S05 et S08 échouent du côté sûr.

Verdict : 0 bloquant, 0 grave — fusion possible
