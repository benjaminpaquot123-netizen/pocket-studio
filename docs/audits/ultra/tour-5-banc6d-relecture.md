# Relecture neuve du lot « banc6d » (tour 5, étape 3, lot 6, fin)
Relu : 55e6a1a6b9e70e9ec49f1b23b2d81d50aa94e970

Relecteur neuf (ni auteur du code). Lu : `git diff $(git merge-base HEAD fx-int)...HEAD` (fuse-reports.py `button_fixed`, tableau-de-bord.py `cases_vides` / `ecrans_non_analyses`, verifie-chantier.py `seat_class`, `test_ultra_bench.py`, `ShareSongUi.kt`, `Tour5AppProof4Test`, verdicts.tsv, revient.tsv, carte-ecrans.tsv, DECISIONS, REVIEW), le rapport de l'auteur.

## Lectures « revient ? » refaites (8 sur 18, sur les journaux, écrans et sons des sièges)
- A d #21 (`launch_projects`) : `sound-1.txt` montre bien le message « 「はじめてのビート」はマイプロジェクトで待っています », `screen-22.txt` ne liste que « Live ». Même famille que T1-22 (F d 5 : projet absent de Mes projets avant sa 1re modification) : « oui » confirmé.
- B a #72 (`tuto_notes_left`) : gestes 72-74, la flèche « plus haut ↑ » (T1-34, B d 94) fait défiler, la note reste cachée, 2 glissements de plus : « oui » confirmé.
- E a #117 : prise « Ta prise » en attente perdue après Pad Live, sans rapport avec les pads vides de T1-05 : « non » confirmé.
- A a #30 : FM au lieu du Marimba, sans rapport avec la demande de notifications (T3-26a) : « non » confirmé.
- E d #11 : « Tout couper » laisse tourner le transport = T4-LIVESTOP, encore ouvert (`@KnownDefect`), donc pas un retour d'un corrigé : « non » confirmé.
- C b #30 : messages écran 30 « bloc A » puis écran 31 « bloc B », grille écran 32 « Voz · 2 » en A, B vide. Le code (`StemBoardRouter` → `LiveRemixCells.place(scene)`) pose bien dans la brique annoncée ; l'écrasement de T3-04 E d 33 n'est pas établi par le journal : « non » accepté, texte faux gardé comme défaut du tour.
- F d #55 : le reproche est « Annuler défait tout l'import » ; le bandeau resté (« Tempo calé … 120 ») ressemble à T3-04 E d 22 (message de remplacement resté après Annuler) mais c'est un autre message : « non » accepté, à surveiller.
- C c #20 : Annuler remet la cible d'export : « non » confirmé.

## Verdicts « voulu » / « banc » contrôlés
- T5-Fa-5 « banc » : le tour 5 a été joué le 2026-11-05, l'horloge du faux moteur réparée le 2026-11-08 (ffd58ce) ; téléphone OK ; la preuve sans banc couvre le chemin du milieu (`playSongPart`). Justifié.
- T5-Bb-58 « voulu » : l'annulation est juste ; le morceau qui passe de 176 à 572 mesures sans prévenir reste un vrai manque, gardé en idée dans REVIEW (mineur au tableau). Accepté, voir M1.
- T5-Cc-23 « voulu » : la mesure Streaming porte bien sur tout le fichier ; le mot « morceau » pour une boucle reste discutable (mineur). T5-Cc-41c « voulu » (idée) : accepté.
- T5-Ca-14 : vrai défaut de l'appli, ouvert, `@KnownDefect` ; le test n'est pas tautologique (il compare les couleurs réelles de `livePadColor`).
- T5-Cb-67 reste « non-tranché » faute de garde Bench* : honnête.

## Correction appli (ShareSongUi)
- `shareReportLines` : en Mode simple, la ligne du niveau en mots est remplacée par la ligne face à la cible ; sans cible, inchangé ; Mode avancé inchangé. Pas de régression vue (`Round3Zone2Test` vert pendant la mutation 1, qui ne touchait que la branche à cible).

## Portail
- Le cherry-pick f04d6679 des traces de « verdicts » est identique à 252e28d6 de fx-int (aucune différence sous `docs/audits/ultra/portail/` entre fx-int et la tête) ; `git merge-tree --write-tree fx-int HEAD` : sans conflit.
- Aucun rejeu retiré ou changé, aucun rouge constaté apporté par la branche.

## Mutations
| n° | mutation | résultat |
|---|---|---|
| 1 | `ShareSongUi` : la ligne « très fort » n'est plus remplacée | rouge (`theSimpleShareReportSaysTheLevelOnce`) |
| 2 | fuse-reports : un « revient ? » refusé redevient « nouveau » au lieu de « même bouton » | rouge (`test_ultra_bench.py`) |
| 3 | fuse-reports : `button_fixed` jamais rempli | rouge |
| 4 | verifie-chantier : `seat_class` sans parenthèses équilibrées | rouge |
| 5 | tableau-de-bord : `ecrans_non_analyses == 0` retiré de la condition I | survit (voir M2) |

## Moyens (non bloquants, notés)
- M1 : A d #21 et B a #72 sont des retours confirmés sans ligne dans verdicts.tsv ni étape suivante notée : à ouvrir au prochain lot (la condition C du tableau reste « non », ce qui est honnête).
- M2 : le seuil `ecrans_non_analyses == 0` de la condition I n'est tenu par aucun test (mutation 5 survit) ; les chiffres mesurés, eux, sont testés.
- M3 : `seat_class` ne lit que le premier parent après « : » (`class X : UneInterface, HumanTester()` échapperait), comme avant le lot.

Tests rejoués : `python3 -I scripts/bench/test_ultra_bench.py` OK ; pendant la mutation 1, `Tour5AppProof4Test` + `Round3Zone2Test` : 24 tests, seul le test muté rouge (les 23 autres verts). Rejeu propre sur la tête (avec `Tour5AppProof3Test`) arrêté dans la file Gradle partagée après 30 min d'attente : à rejouer par le portail.

Verdict : 0 bloquant, 0 grave — fusion possible
