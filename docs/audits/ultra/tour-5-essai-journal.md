# Essai à blanc complet (PLAN-RESTE 7.6 et 7.7), 2026-11-11

Fait sur une intégration jetable : la branche locale `fx-essai` (atelier `/home/user/fx-essai`), partie de `claude/socle-du-banc-alqcr3` (2e1e48ac), jamais poussée. Une fausse équipe `eq/tour-5-essai` (atelier `/home/user/eq-tour-5-essai`, créé par `equipe-atelier.sh`) corrige un défaut factice du banc. Les réparations des scripts sont dans `wip-essai` (paires « rouge: » / « vert: »), fusionnées dans `fx-essai` au fil de l'essai. Gradle seulement par `scripts/bench/gradle-file.sh`. Temps mesurés sur cette machine (16 Go, une seule file Gradle).

## Le défaut factice

- Posé sur `fx-essai` par l'« agent banc » : `SeatOptions.parse` lit l'ancienne clé `simple=` comme « oui » dès qu'elle ne vaut pas `false` (`simple=non` ouvre le siège en Mode simple). Siège `docs/audits/ultra/sieges/essai-7-6/a` (copie de `tour-5/lotC/c`, `simple=non`), rejeu `t5-90-essai-simple-non` écrit par `rejeu-nouveau.sh` dans `rejeux-attente/` (0 geste, `attendu=What do you want to do?`), ligne `T5-ESSAI-1` dans `verdicts.tsv` (« banc », ouvert). Le paquet 2 est ouvert sur `fx-essai` (`portail-paquet.txt`).
- Premier essai de défaut (`mode=Pro` refusé à cause de la majuscule) : le selftest l'a attrapé lui-même (`BenchAmeneesProfilesTest` sur le profil du siège rejoué : « mode=Pro : simple ou pro »), et `rejeu-defauts.sh` a dit « selftest rouge : rejeux non joués ». C'est le comportement voulu (le banc se garde), pas une casse ; le défaut a été déplacé là où le selftest ne regarde pas (`simple=` n'est pas une clé de profil).

## Étapes, commandes, temps

| # | Étape | Commande | Temps | Résultat |
|---|---|---|---|---|
| 1 | Atelier de l'équipe | `scripts/bench/equipe-atelier.sh cree tour-5 essai` | 2 s | atelier, branche, rambarde Gradle installée |
| 2 | Rejeu avant correction | `PS_REJEUX_DIR=docs/audits/ultra/rejeux-attente scripts/bench/rejeu-defauts.sh t5-90-essai-simple-non` | 761 s | « oracle non tenu : « What do you want to do? » absent », dernier écran différent de l'origine : écran 1, première ligne différente nommée (7.5) |
| 3 | Rouge | commit bf4d0860 (deux tests), `gradle-file.sh :app:testDebugUnitTest --tests …SeatOptionsParseTest --tests …BenchSeatOptionsTest.aSeatWithTheOldSimpleNoIsInPro` | 66 s | 2 rouges sur 2 |
| 4 | Vert | commit c6c7edc9 (une ligne), mêmes tests | 66 s | verts |
| 5 | Rejeu déplacé dans `rejeux/` (61c6dcc3), rejoué | `scripts/bench/rejeu-defauts.sh t5-90-essai-simple-non` | 733 s | « oracle tenu » (le selftest du lot de rejeu tourne après le dernier commit de code : fiche, point 4) |
| 6 | Rapport de zone, décisions, ligne de verdicts | `tour-5-essai-rapport.md` (ligne `tests:`, deux lignes `chemin:`, ligne du rejeu), `docs/decisions/tour-5-essai.md` (c31a0b1b) | — | — |
| 7 | Avant relecture | `scripts/bench/pre-vol.sh --avant-relecture tour-5 essai` | 1 s | PRÊT |
| 8 | Relecture | `tour-5-essai-relecture.md`, « Relu : c31a0b1b… », dernière ligne « Verdict : 0 bloquant, 0 grave » (6f9a8a7c) | — | avant commit, `pre-vol.sh` disait bien RECALÉ (« pas de relecture neuve commitée ») |
| 9 | Avant portail | `scripts/bench/pre-vol.sh tour-5 essai` | 0 s | PRÊT |
| 10 | Portail, 1 zone | `scripts/bench/fusion-equipes.sh tour-5 essai` (paquet 2) | 695 s puis 835 s | 1er passage : rouge sur un test capricieux (casse 5), fusion annulée proprement ; 2e : fusionné (e9656856), rejeux `t4-26-prise-clavier` et `t5-90-essai-simple-non` tenus, tests ciblés de `tests:` rejoués (filtres `Classe.méthode`), traces + `DECISIONS.md` + tableau de bord commités (cc0892c3) |
| 11 | Marquer corrigé | `scripts/bench/marque-corrige.sh T5-ESSAI-1 c6c7edc9 tour-5 docs/audits/ultra/portail/2026-11-11-tour-5-essai/selftest-resume.txt` | 5 s | verdicts.tsv et base.tsv à « corrigé » ; juge : 2 écarts, les mêmes qu'avant la fusion (paires rouge/vert d'un autre lot) ; commit 3fd6704b |
| 12 | Tableau de bord | `python3 -I scripts/bench/tableau-de-bord.py docs/audits/ultra/sieges/tour-5` (consigne) | < 1 s | casse 2 ; après réparation, relancé par `marque-corrige.sh` lui-même |
| 13 | Portail, 2 zones de plus (7.7) | `equipe-atelier.sh cree` ×2, lots sans code, `pre-vol.sh tour-5 essai2 essai3`, `fusion-equipes.sh tour-5 essai2 essai3` | 1676 s | deux fusions (d87eed4d, db54fb2f), traces commitées, tableau sans « fichiers non commités » (casse 3 réparée) |

## Casses trouvées et réparées (dans `wip-essai`)

1. **Ouvrir le paquet 2 rendait le selftest rouge** (« scripts du banc ») : les maquettes du portail de `test_ultra_bench.py` copiaient le `portail-paquet.txt` du dépôt et attendaient le paquet 1. Plus aucun lot ne pouvait se jouer. Réparé : la maquette écrit son propre « paquet: 1 ». e7476667 / a16cc034.
2. **Tableau de bord contradictoire** : le portail le calcule sur tous les tours, la consigne (`consignes-equipes.md`) le faisait relancer sur `tour-5` seul, ce qui l'écrasait (94 lignes de moins) et le laissait non commité. Réparé : `marque-corrige.sh` le relance lui-même sur tous les tours, dans l'ordre du portail ; la consigne le dit. 45cda86d / 8c07513b.
3. **Le tableau commité par le portail comptait ses propres traces comme « fichiers non commités »** (G : 2). Réparé : le portail nomme ce qu'il va committer (`PS_PORTAIL_ECRIT`) ; `tableau-de-bord.py` l'écarte. Vu à 0 sur les fusions essai2 et essai3. 77310338 / 1d7d8de0.
4. **Message d'échec qui pointe un fichier effacé** : « SELFTEST ROUGE : scripts du banc (voir …/selftest-scripts.log) », fichier supprimé avec le lot par `rejeu-defauts.sh`. Réparé : les lignes FAIL sont affichées avant. 49c5ccd8 / 45d79172 (pas encore dans `fx-essai`).
5. **Non réparée (test capricieux, connu)** : `BenchAmeneesTest#aTapDuringTheExampleIsNotCounted` rouge 1 fois sur 7 selftests ici (« YourTurn(c, 0) attendu »), déjà vu au lot banc45. Le portail a annulé la fusion proprement ; relancé, vert. Reste à régler sur la base (quarantaine ou cause).

## 7.7 : le temps

- Avant : par zone, le portail lançait `selftest.sh` (≈ 13 min ici, mesuré : 817 s sur un selftest seul), puis `rejeu-defauts.sh`, qui relançait un selftest pour son lot de rejeux (≈ 13 min) avant de les jouer. **Deux selftests par zone.**
- Réparé (f95b81b1 / 9fcbbd9e) : `rejeu-defauts.sh` laisse le selftest de son premier lot (résumé, tampon, juge, résultats JUnit) dans `$PS_REJEUX_SELFTEST` ; le portail ne lance plus le sien et garde celui-là dans ses traces (le juge le lit scellé, comme avant). Les lignes de refus du portail sont inchangées (`KNOWN_RULES` du pré-vol tient).
- Mesuré après : 835 s pour 1 zone, 1676 s pour 2 zones, **3 zones en 2511 s ≈ 42 min** (cible < 60 min tenue). Avant la réparation, ≈ 3 × 27 min ≈ 81 min (estimé : un selftest de plus par zone).
- Limite : `rejeu-defauts.sh` joue 6 rejeux par lot, chaque lot avec son selftest. Avec les 8 rejeux de `rejeux-attente/` du tour 5 passés dans `rejeux/` (10 au total), il faudra 2 lots, soit un selftest de plus par zone : ≈ 27 min par zone, ≈ 80 min pour 3 zones. Non réparé ici (il faudrait plus de 6 sièges par lot).
- Démons Gradle : inchangé, `gradle-file.sh` les arrête avant chaque passage (aucune mesure mémoire neuve).

## Étapes manuelles

Toutes les vérifications, la fusion, le marquage et le tableau passent par les scripts. Restent faites à la main, par construction (les fiches les demandent) : poser le défaut et écrire la ligne de `verdicts.tsv` (agent banc), fusionner l'intégration dans la branche de l'équipe (`git merge fx-essai`, fiche point 2), les commits rouge / vert et le `git mv` du rejeu vers `rejeux/`, écrire le rapport, les décisions et la relecture, committer `verdicts.tsv`, `base.tsv` et le tableau après `marque-corrige.sh`. **0 étape manuelle hors script** pour le contrôle et la fusion ; aucune n'a demandé de corriger un fichier à la main pour passer un script.

## Branches (locales, jamais poussées)

- `fx-essai` : e736d40f ; `eq/tour-5-essai` : 6f9a8a7c ; `eq/tour-5-essai2` : 4529a1c2 ; `eq/tour-5-essai3` : 31e1dcd6.
