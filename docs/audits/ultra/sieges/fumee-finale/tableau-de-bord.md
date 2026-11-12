# Tableau de bord de l'audit ultra

Calculé par `scripts/bench/tableau-de-bord.py` ; « ? » = chiffre manquant, la condition n'est pas remplie.

## fumee-finale

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 0 | 0 | oui |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 0 | 0 ; ≤ 2 ; 0 | oui |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 12/120 ; 3 contredite(s) | ≥ 90 % ; ≥ 80 % | non |
| E | Missions réussies (oracle mesures.json) | 2/4 | ≥ 90 % | non |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 2 (0) | ≤ 5 (0) | oui |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, 1 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 27 (projects, new_project, live_screen, live_grid, live_pads_full, timeline_screen, rhythm, piano_roll, acid, instrument_panel, mixer, master_pro_panel, video_sheet, remix_project, cut_screen, board, library, midi, plus, settings, help_page, glossary, latency, recette, studio, guide_panel, tuto_panel), 36, 0, 4 / 1 | 0, 0, 0, ≥ 16 / ≤ 3 | non |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 11 cachés sur 23 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Toutes remplies : non

## Courbe de convergence

`echelle` : v1 = le mot du testeur (bloquant, grave…), v2 = la conséquence (mission-impossible, contourne-en-N-gestes) ; `graves_declares` : ce que les testeurs ont écrit, à leur échelle ; `graves_confirmes` : ce que la fusion garde grave (revu par le testeur, ni « banc? » ni verdict « banc »). Les graves ne se comparent qu'entre tours de même échelle.

| tour | sieges | fiches | fiches_par_siege | echelle | graves_declares_par_siege | graves_confirmes_par_siege | grappes | revient | incidents_banc | promesses_tenues |
|---|---|---|---|---|---|---|---|---|---|---|
| fumee-finale | 4 | 5 | 1.2 | v2 | 0.0 | 0.0 | 5 | 0 | 0 | 0/0 |

**Arrêt de l'audit possible : non** (A à J remplies sur les deux derniers tours complets).
