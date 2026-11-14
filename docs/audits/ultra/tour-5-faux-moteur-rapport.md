# Lot 3 « Le faux moteur » : rapport de l'auteur (2026-11-10)

Atelier `faux-moteur`, branche `eq/tour-5-faux-moteur`, au-dessus de a683872. Un seul auteur, rien n'est poussé. À relire par un relecteur neuf avant fusion.

But du lot : le faux moteur du banc (`BenchEngine`) dit la même chose que le vrai moteur (`engine/src/main/cpp/core/Engine.cpp`), et des tests le prouvent. Aucune ligne de l'appli (`app/src/main`) n'est changée : `git diff --stat a683872..HEAD -- app/src/main` est vide.

## En tête : le « moyen » laissé par la relecture du lot 2

La règle inverse du juge (`annotation_gaps` dans `verifie-chantier.py`) ne lisait que le premier « RECETTE tel-N » en majuscules de chaque segment. Une preuve inventée fermait donc un écart sans bruit.

- Maintenant, chaque « tel-NN ✅/❌ » d'une cellule est lu (`finditer`), avec ou sans le mot RECETTE, en majuscules ou non, premier du segment ou non.
- Quand `retours-benjamin.md` existe, seule une annotation rattachée à un essai du propriétaire (numéro, marque, appareil, date) compte comme preuve. Une annotation inventée donne un écart et ne prouve plus rien.
- Quand `retours-benjamin.md` est absent, le juge le dit : un écart « retours-benjamin.md absent », dès que `verdicts.tsv` porte des annotations d'essai.
- Preuve : rouge 96c32ee / vert 08c1f5d (`test_ultra_bench.py::owner_trials`, et `judge_criteria`, `open_app_proof` pour les notes absentes). Les trois sondes de la relecture, rejouées sur le vrai `verdicts.tsv` (note de T4-01, fichier remis après chaque essai) : 24 écarts à chaque fois, au lieu de 22 avant. La ligne « T4-01 sans preuve » reste et l'annotation est signalée.
- Sur les vraies données, le juge donne les mêmes 23 écarts avant et après ce correctif.
- Le mutant « règle muette quand les notes ne contiennent aucun essai » a maintenant son cas de test. Le mutant « n'importe quel numéro d'essai » n'a pas de cas à lui.

## Le lot, ligne par ligne

| Ligne | Fait ? | Ce qui a changé | Ce que le test prouve | Preuve |
|---|---|---|---|---|
| A.9 (F4) | fait | Une seule horloge. `transportStep` et `transportPosition` sont calculés par le faux moteur : −1 à l'arrêt et pendant le décompte, pas gardés quand le tempo change. Le nombre que la tête de lecture du banc écrivait dans le moteur a disparu. Avec le moteur présent, `BenchPlayhead` lit le moteur ; son propre compte ne sert plus que sans aucun moteur. Le micro du faux moteur n'a plus son décompte à lui : la prise part au pas 0 du transport. | `BenchSongClockTest#transportStepKeepsItsStepsAcrossATempoChange` : 16 pas à 120, on passe à 60, il reste 16, puis 20 une seconde après. `#transportStepIsMinusOneDuringTheCountIn` : −1 pendant le décompte, 8 une seconde après le « 1 ». `#aTakeWaitsForTheTransportsOwnCountIn` : avec deux mesures de décompte, rien n'est enregistré avant le « 1 ». `BenchEngineTest#thePlayHeadReadsTheEnginesClock` : la tête de lecture garde 16 au changement de tempo (elle disait 8). | rouge 0fcb63d / vert 419a5c4 |
| A.7 (F1) | fait, en partie déjà fait avant | La prise gardée à la fin du morceau était déjà réparée (87b6693). Ce qui restait de F1 : la marque de morceau en attente tombe à la fin du morceau, comme à ■ ; ■ pendant le décompte laisse la prise armée. La marque est jouée comme dans Engine.cpp. | `BenchSongClockTest#aTakeArmedIsKeptWhenTheSongEnds` : état 2, 32 × 6000 images, et la marque abandonnée ne se réalise pas au ▶ suivant (elle revenait au pas 64). `#aTakeStillCountingInIsNotEndedByStop`. `#aSongMarkComesTrueOnItsLineAndPutsTheSongThere`. | rouge 49c3325 / vert 2e650ca |
| A.7, garde de T5-Ab-27 | fait | Deux gardes, vertes dès leur premier lancement (ce ne sont pas des réparations). `T5-Ab-27` passe « corrigé » (87b6693, 2e650ca) avec une preuve sans banc. | `BenchEngineTest#aMicTakeLeftRunningIsKeptWhenTheSongEnds` : sur le faux moteur, la prise laissée tourner finit sur sa piste, un clip de la longueur du morceau. `TakeAtTheSongsEndTest#aTakeLeftRunningIsKeptWhenTheEngineEndsTheSong` : l'appli seule, sur un moteur scripté par le test. | 8f4f7c1 |
| A.8 (F2) | fait | `BenchEngine.currentStep()` existe : c'est le pas du motif de batterie, comme `currentStep_` du vrai moteur. La longueur du motif vient des commandes de l'appli (motif en attente pris au relais), pas d'une valeur poussée par le banc. | `BenchEngineTest#aKnobMovedWithAutomationRecordWritesAPoint` : ● puis volume tourné pendant la lecture : une courbe, un point au pas 5, puis au pas 0 du tour suivant. `#aKnobMovedWhileTheSongPlaysWritesOnTheSongsBar` : courbe de morceau, point sur la deuxième mesure. | rouge c6e9f7e / vert c9bb80d |
| A.15 (F5) | fait | `absoluteStep` et `playPosition` comptent depuis le début de la partie qui joue. La tête de lecture du banc ne recalcule plus rien : elle recopie `currentStep`, `absoluteStep` et `songStep` du moteur. | `BenchSongClockTest#absoluteStepRestartsAtTheSwap` : relais au pas 32, on lit 8 au pas 40 (on lisait 40). `BenchEngineTest#theGridsHeadTurnsWithTheDrumsUnderALongMelody` : mélodie de 64 pas, batterie de 16 : la tête de la grille est au pas 8 quand la partie est au pas 40. `BenchSongClockTest#theDrumPatternsStepFollowsTheSceneTakingOver` (vert dès le départ, 1afd1a6). | rouge e3db5e2 / vert 7bfe58b |
| A.13 (G1) | fait pour deux gardes | `myExportsListsTheProjectZipJustSent` compose l'action `rememberSendProject` d'AppRoot et l'appelle. `noChangedWarningRightAfterSharing` compose `ShareSongProgress`, qui reçoit `shareRequests`. Aucune des deux ne réécrit le câblage. Règle de méthode écrite en tête de `Tour5AppProofTest`. | `scripts/bench/mutation-l1.sh` applique la mutation L1 de l'audit, joue la classe, retire la mutation. Avant : les deux gardes restent « known defect still there » (sortie 1). Après : les deux disent « no longer fails » (sortie 0). Sans mutation : 4 tests verts, 4 défauts « still there ». | rouge ec9cc11 / vert dddbb0d |
| Hors liste | fait | « Quitter le morceau » retire la position du morceau tout de suite, comme `leaveSongNow()`. | `BenchSongClockTest#leavingTheSongTakesTheSongPositionAwayAtOnce`. | rouge b089c12 / vert b8a403f |

Chaque rouge a été lancé et vu rouge pour la bonne raison avant son vert ; les messages d'échec sont dans les commits « rouge: ».

## Données mises à jour

- `verdicts.tsv` : `T5-Ab-27` « corrigé », preuve sans banc et trois gardes. Cellules de preuve de `T5-Cd-68`, `T5-Cc-41a`, `T5-Cc-30` et `T5-Ec-61` : « (rouge ; @Ignore) », faux depuis longtemps, remplacé par ce que la garde est aujourd'hui.
- `base.tsv` : clé `audio_tracks|audio_record_0|rien`, « banc » devient « corrigé », tour-5.
- `banc-fiable-journal.md` : les lignes du lot, chacune avec sa paire.
- `docs/DECISIONS.md` : six lignes ajoutées à la fin.
- Aucun fait d'essai sur appareil n'est ajouté ou changé. `retours-benjamin.md` n'est pas touché.

## Commandes lancées, résultats vus

- `python3 -I scripts/bench/test_ultra_bench.py` : « ultra bench scripts: OK ».
- Gradle, toujours par `scripts/bench/gradle-file.sh`, ciblé : `BenchSongClockTest` 15 verts, `BenchEngineTest` 10 verts, `TakeAtTheSongsEndTest` 1 vert, `Tour5AppProofTest` 4 verts.
- Avant le selftest, toutes les classes `Bench*` et `Tour*AppProof*Test` une fois : 176 tests, 0 rouge, 41 sautés.
- `bash scripts/bench/selftest.sh <lot>` : **« Selftest vert : 149 verts, 0 rouges, 0 en quarantaine, 41 sautés »**, une fois, arbre propre côté code. Les 41 sautés sont les `drive` des sièges, tous dans la liste bornée ; rien n'y est ajouté. Le selftest a tourné sur le commit 49bbb35, qui n'existe plus sous ce nom : voir « Identifiants de commit » plus bas. Le code est le même, au fichier près (`git diff 49bbb35 b8a403f` était vide).
- `python3 -I scripts/bench/verifie-chantier.py --resume <lot>/selftest-resume.txt` : **22 écarts** (23 avant le lot). Détail plus bas.

## Le juge : 23 écarts avant, 22 après

Un seul écart en moins : **T5-Ab-27** n'est plus « banc sans preuve sans banc », parce que la ligne porte maintenant `TakeAtTheSongsEndTest`, un test de l'appli hors `Bench*`. Aucune règle n'est affaiblie, rien n'est ajouté à `selftest-sauts-connus.txt`. Les sept paires rouge/vert du lot sont appariées et les lignes cochées du journal les citent.

Les 22 qui restent, tous d'avant le lot :

- 10 lignes « banc » sans preuve sans banc : T4-19, T4-16, T4-17, T5-Dc-4, T5-Fb-73, T5-Ac-61, T5-Db-16, T5-Fc-8, T5-Cc-41b, T5-Ab-14.
- 2 lignes « appli » sans preuve : T4-01, T5-TEL10.
- 6 paires rouge/vert historiques : verts sans rouge 0b1cbbc, dcc3367, 87b6693 ; rouges sans vert 8f36fa3, df794a9, d503254.
- 4 lignes cochées du journal sans preuve : 4.0 (deux fois), 4.2, 4.3.

## Identifiants de commit

Après le selftest, le message d'un commit a été réécrit (le vert de A.9), sans toucher à un seul fichier. Raison : ce message nommait un test ajouté par un vieux rouge resté ouvert (d503254). Le juge apparie par nom de test : mon vert fermait donc ce vieux rouge à la place du sien. Le total ne bougeait pas, mais l'appariement était faux. Le message ne nomme plus ce test ; d503254 est de nouveau compté ouvert, comme avant le lot.

Conséquence : les seize commits du lot ont changé d'identifiant. Ceux de ce rapport, du journal, de `verdicts.tsv`, de `base.tsv` et de DECISIONS sont les nouveaux. Le tampon `selftest.ok` du dossier du lot porte l'ancien identifiant de tête : il ne vaut plus pour lancer des sièges, il faudra relancer le selftest pour cela.

## Ce qui n'a pas pu être fait, et les limites

- **Le faux moteur ne joue toujours pas la grille Live.** Un lancement de la grille Live déplace la batterie seule dans le vrai moteur ; ici `currentStep` suit seulement la partie. Les pads Live partent toujours tout de suite. C'est A.16, hors du lot.
- **Prises micro.** Seule la prise « au départ » est jouée (la seule que l'appli arme). Une prise armée pendant que la musique joue attendrait le prochain ▶ sur un téléphone ; ici elle enregistre tout de suite. Un ▶ relancé pendant une prise remet son compteur à zéro. Pas de test pour ces deux cas.
- **A.13 ne couvre qu'un des deux chemins de « Envoyer le projet ».** Celui de « Mes projets › ⋯ » est un bloc écrit en ligne dans AppRoot : une correction faite seulement là ne serait pas vue. La garde `noChangedWarningRightAfterAMixExport` (export MIDI) n'est pas changée : elle appelle les fonctions de l'appli, pas une copie du câblage.
- **`mutation-l1.sh` modifie `app/src/main` le temps de son passage**, puis le remet. Il n'est lancé ni par le selftest ni par la CI, et n'a pas de test dans `test_ultra_bench.py`.
- **`TakeAtTheSongsEndTest` n'a pas été éprouvé par mutation** : il faudrait toucher l'appli. Il échoue bien si le moteur ne dit jamais « fini » (attente dépassée).
- **Le banc continue d'écrire le tempo et la mesure du projet dans son moteur à chaque tic** (`HumanTester.pass`). L'appli envoie la commande avant, donc l'horloge ne saute pas ; ce filet n'a pas été retiré.
- **Sondes de l'audit (A.1).** Les sondes a1, a2, b1, b2, b3 et la mutation L1 sont maintenant des tests ou un script du dépôt. `AUDIT-BANC-CREDIBLE.md` n'est pas modifié. La mutation L2 (pads) n'est pas rejouée : elle dépend de A.16.
- **Comportement du vrai moteur lu, pas exécuté.** Chaque règle vient de la lecture d'Engine.cpp (lignes citées dans les tests). Les tests hôtes du moteur n'ont pas été relancés : le C++ n'est pas touché.
- Pas d'émulateur ni de téléphone : rien n'est dit de plus que `retours-benjamin.md`.

## Défauts de l'appli révélés

Aucun nouveau. Les deux gardes refaites confirment, par le vrai câblage, les deux défauts déjà connus : T5-ZIP (le .zip envoyé n'est pas dans « Mes exports ») et T5-ALERTE (alerte « le morceau a changé » juste après le partage). Leur correction relève de l'étape 2.
