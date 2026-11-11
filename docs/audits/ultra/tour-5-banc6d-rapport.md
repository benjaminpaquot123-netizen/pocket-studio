# Lot « banc6d » (étape 3, lot 6, fin : 6.2, 6.5, onze lignes « non-tranché ») : rapport de l'auteur

Atelier `/home/user/eq-tour-5-banc6d`, branche `eq/tour-5-banc6d`, partie de `3fa7eeb8` (fusion « verdicts »). Zone : `scripts/bench`, `app/src/test`, `docs` ; une correction dans `app/src/main` (`export/ShareSongUi.kt`), hors accueil et Pad Live.

## 6.2 « revient ? »
- `fuse-reports.py` (rouge f37a1827 / vert b116d4e0, cas `fixed_button_comeback`) : un « même bouton » dont le bouton était « corrigé » à un tour antérieur devient candidat « revient ? » ; refusé, il redevient « même bouton ».
- 18 candidats lus (tour 5 : 14, fumée : 4) dans `revient.tsv` : **A d #21 = T1-22 revient** (Mes projets sans le projet promis), B a #72 = T1-34 revient (note du tuto sous l'en-tête) ; 16 « non » avec leur raison. C du tour 5 et de la fumée : 0 candidat.

## 6.5 tableau de bord complet
- `docs/CARTE-CODE.md` existe mais sans routes : `carte-ecrans.tsv` (36 écrans par l'étiquette de leur page, chacune vérifiée dans `app/src/main`). `tableau-de-bord.py` compte `cases_vides` (écrans sans route dans les journaux) et `ecrans_non_analyses` (liste `ecrans-oeil-neuf.txt` du tour) : rouge 4423d09f / vert 10db41c8, cas `empty_cases`. H1 : `ci-etat.sh` existait.
- Tour 5 : I = 10 cases vides (padlive_party, live_pads_full, piano_roll, instrument_panel, master_pro_panel, remix_project, glossary, latency, recette, studio), 36 écrans non analysés (pas de passe « œil neuf » au tour 5) → « non », plus « ? ».

## Onze lignes (Tour5AppProof4Test, ni HumanTester ni BenchEngine)
- **Appli, corrigé** : T5-Fa-11, T5-Aa-43 : en Mode simple, « Partager » disait « Niveau : très fort » et « Niveau : à la cible » sous le même fichier (rouge ae1eda78 / vert 88e36490, `shareReportLines`). Les parenthèses doublées « (Auto (style)) » restent.
- **Appli, ouvert** : T5-Ca-14, `@KnownDefect T5-DEFI-COULEURS` : les ronds du défi ont la couleur de la famille du son, partagée par d'autres pads (Pad Live : non corrigé).
- **Appli, voulu** : T5-Cc-23 (mesure Streaming sur tout le fichier, boucle comprise), T5-Bb-58 (les 572 mesures viennent de la mélodie de 4 mesures gardée dans une scène d'une mesure ; l'annulation est juste), T5-Cc-41c (idée).
- **Banc** : T5-Fa-5 (départ avant le Drop voulu ; compteur qui suit le moteur depuis une partie du milieu ; garde existante `BenchPlayThenTabsTest#theBarCounterGoesOnWhileTheSongPlays`).
- **Non tranchés** : T5-Cb-67 (preuve sans banc écrite, elle dit « banc » : l'appli coupe le canal 3 ; garde Bench* sur `remixHeard` à écrire) ; T5-Ad-13, T5-Ca-1, T5-DEMO-VIDE non faits (Pad Live et accueil, écrans d'autres auteurs).

## Relecture « verdicts »
- `seat_class` lit les parenthèses imbriquées (rouge 338748e3 / vert a563a6dd) ; mêmes 25 sièges reconnus qu'avant sur le dépôt.
- T5-Cc-41b par la vraie taille d'un export : non fait (le WAV 24 bits est écrit par le moteur natif, absent sous Robolectric).

## Juge
- Traces du portail de la fusion « verdicts » reprises de fx-int (cherry-pick de 252e28d6) : sans elles, 1 écart hérité.
- Selftest sur 1efe48bf : 192 verts, 0 rouge, 41 sautés ; juge lancé par le selftest : 0 écart. `test_ultra_bench.py` : OK.

## Essais à demander à Benjamin
- Aucun nouveau. Pour voir la correction : partager un morceau en Mode simple, la fenêtre « C'est prêt » ne dit le niveau qu'une fois.
