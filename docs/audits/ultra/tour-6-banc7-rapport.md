# Tour 6, lot « banc7 » : les défauts du banc vus pendant le tour 6

Branche `claude/benstudio-audit-tour-6-xeiz3b-banc7`, partie de `ccr-80af1a1b-mn0wec` (3b4235db). Zone : `app/src/test` (le banc) et `scripts/` (lanceur et outils du banc). Rien dans `app/src/main` ni dans le moteur. Décisions : `docs/decisions/tour-6-banc7.md`. Sources : fin de `lots-correction.md` (branche eq/tour-6-tri-gaucher) et § 6-7 de `lecture-recurrents.md` (branche du rassemblement).

| Point | Ce qui était faux | Cause | Correctif | Preuve (rouge / vert) |
|---|---|---|---|---|
| 1 (E1, D b #70) | Siège clos « rien ne change » pendant qu'on pose des notes | `HumanTester` ne comparait que le texte de l'écran et la ligne du son ; les notes posées n'y sont pas | Une empreinte du projet ouvert (`songDigest()`) : un geste qui change le morceau remet le compte à 1 | 5b3b6605 / 8f54be03 (`BenchTour1Test`) |
| 2 (E12, C b #58) | « tape » de 4 coups « compté pour 1 » | Le banc livre bien 4 touches (prouvé par le test). Le « 0 sur 4 » compte les coups sur le bon pad : le testeur tapait la grosse caisse (`#58 DOIGT padlive_pad_8`), le défi « Boum boum » se joue sur le premier pad « coup » (`PadChallenge.kt`), ici « Claquement de mains » ; que l'appli ne le dise pas clairement est un candidat défaut de l'appli, renvoyé au tri. Défaut du banc : la ligne du son disait « a joué une fois » pour 4 coups (`hitsPlayed().distinct()`) | La ligne du son compte les coups par pad (« a joué 4 fois ») | 136c07cd / fbec704d (`BenchAmeneesTest`) |
| 3 | `seat-metrics.py` relancé change le verdict de 6 sièges (8 en le rejouant à la relecture) | Les oracles lisent `etat/` du siège, non commité : un état vide donne « non » | Sans `etat/`, le verdict du `mesures.json` commité est gardé (avec une note) ; sans lui, seul un « reussie » prouvé par le journal tient, sinon `inconnu`, jamais un « non » inventé. Les 12 sièges qui ont un `mesures.json` gardent leur verdict | 4f309e1f / d141c8db (`test_ultra_bench.py`, `verdict_without_state`) |
| 4 | `tableau-de-bord.py` : promesses « 0/0 » | Les lignes `promesse: id=` étaient lues, mais une promesse ne comptait que vue par 2 sièges ; au tour 6 chaque siège a ses propres ids | Toutes les lignes comptées (oui / non / pas vue), la « -suite » d'un siège remplace son « pas vue » ; tour 6 : 38 oui, 34 non, 1 pas vue | 03dda926 / 29a95549 (`promises_of_one_seat`) |
| 5 | `verifie-chantier.py` : 28 écarts après le rassemblement, 12 dans les selftests | Analyse, sans correctif (voir dessous) | — | sortie rejouée sur cd3c7a70 |
| 6 (T1) | 120 gestes pour un tuto entier (fini au chapitre 7/15) ; le lanceur tue un lot après 20 min sans sortie | `fabrique-sieges.py` acceptait tout budget ; `gradle-file.sh` coupe à `PS_STALL_MIN` = 20 min alors qu'un siège attend jusqu'à 30 min un geste | `fabrique-sieges.py` refuse tuto-facile et tuto-pro sous 250 gestes ; `human-tester.sh` règle `PS_STALL_MIN` = plus long `timeout.minutes` du lot + 10 | d57dc4dc / d180a65c (`banc7_budget_and_launcher_stall`) |
| 7 (E11, E c) | Tampon refusé sans raison (« No green self-test ») quand les start.txt d'autres sièges sont là | `tampon.sh verifie` refusait en silence | Chaque refus dit sa raison ; celui des sièges nomme les start.txt présents et dit de mettre les autres de côté AVANT le selftest | f525cdbc / 5d237295 (`banc7_stamp_reason`) |
| 8 (E9) | Premier « regarde » : 15 min d'attente puis « L'appli ne s'est pas ouverte » | Rien ne disait que le lanceur n'était pas lancé | `human-tester.sh` écrit `<lot>/.banc-en-cours` (son PID) ; `human-act.py` répond aussitôt, sans banc vivant : c'est un incident du banc, le coordinateur lance `scripts/human-tester.sh <lot>` ; le marqueur est posé avant la vérification du tampon | 07b28078 / 7c741786 (`banc7_launcher_messages`) |

## Point 5 : les 28 écarts de `verifie-chantier.py`

Rejoué sur cd3c7a70 avec `--resume lotF/selftest-resume.txt` :
- **1 vrai écart de méthode** : « résumé refusé : résumé d'un autre code ». Le résumé du lot F a été fait sur un autre code que celui jugé ; le juge le refuse (règle P3), à raison.
- **19 lignes « garde … pas verte »** (T4-16, T4-17, T4-19, T5-Dc-4, T5-Fb-73, T5-Ac-61, T5-Db-16, T5-Fa-5, T5-Fc-8, T5-Cc-41b, T5-Ca-32, T5-Ab-14 ×2, T5-Ab-27 ×3, T5-Ed-11, T5-PADRETRIG, T5-PREMIER-ECRAN) : **pas de vrais écarts**, la conséquence du résumé refusé (sans résumé du code jugé, aucune garde n'est verte, par règle). Le selftest de ci2 du 2026-11-12 (dfd23e2c, résumé scellé) n'en garde aucune.
- **8 vrais écarts historiques** du journal git : 5 « vert » sans « rouge » avant (2b34c216, a2a6ec58, 11c48708, 411b2d51, bc0e6f27) et 3 « rouge » sans « vert » après (9b8fc4fd, 96a7a702, ade1a174). Ce sont les 8 que trouvent les selftests d'aujourd'hui ; ils ne se ferment qu'en appariant ces commits (lot juge), pas ici.
- Les « 12 » des selftests des lots = ces 8 + 4 gardes alors pas vertes (T5-Ab-27, T5-Ed-11, T5-PADRETRIG, T5-PREMIER-ECRAN), vertes au selftest d'aujourd'hui.

Pour avoir un compte juste après un rassemblement : `verifie-chantier.py` avec le résumé d'un selftest fait sur ce code-là (ou `selftest.sh`).

Restent hors de ce lot : E3 (écrans en texte vide), E4 (notes du piano roll absentes du texte, choix « réalisme » R1 gardé), E5, E6, E7, E8, R8, U12, U22 de la liste du banc.

Joué ici : les tests Python du banc (`test_ultra_bench.py`, `test_socle.py`, `test_report.py`, `test_defauts_caches.py`, `test_cpu_report.py`) verts sur la branche fusionnée ; `Bench*` entier vert dans l'atelier des points 1-2 (195 tests, 44 sautés, 0 rouge) ; `BenchTour1Test` et `BenchAmeneesTest` rejoués sur la branche fusionnée.

tests: com.pocketstudio.app.human.BenchTour1Test com.pocketstudio.app.human.BenchAmeneesTest

## Liste des chemins

- chemin: Clavier en mode simple notes posées dans la grille | test: BenchTour1Test#notesAddedInThePianoRollAreNotNothingChanges
- chemin: Pad Live défi Boum boum À moi puis tape 4 sur un autre pad | test: BenchAmeneesTest#fourTapsOnAnotherPadAreFourTouchesAndHeardFourTimes
