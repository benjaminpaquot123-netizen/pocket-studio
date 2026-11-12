# Tableau de bord de l'audit ultra

Calculé par `scripts/bench/tableau-de-bord.py` ; « ? » = chiffre manquant, la condition n'est pas remplie.

## tour-0

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 13 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 3 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 10 (0) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, 2 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 24 (new_project, padlive_party, live_screen, live_pads_full, timeline_screen, synth, piano_roll, acid, audio_tracks, instrument_panel, mixer, simple_mix, master_pro_panel, video_sheet, midi, plus, settings, help_page, glossary, latency, recette, studio, guide_panel, tuto_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Toutes remplies : non

## tour-1

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 51 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 4 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 0 | 0 ; ≤ 2 ; 0 | oui |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 77 (3) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, ?, 2 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 11 (live_screen, piano_roll, acid, instrument_panel, board, midi, help_page, glossary, recette, studio, guide_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Toutes remplies : non

## tour-2

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 14 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 1 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 0 | 0 ; ≤ 2 ; 0 | oui |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 29 (0) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, ?, 2 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 20 (padlive_party, live_screen, live_grid, live_pads_full, timeline_screen, piano_roll, acid, instrument_panel, video_sheet, remix_project, board, midi, plus, settings, help_page, glossary, latency, recette, studio, guide_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Toutes remplies : non

## tour-3

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 54 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 1 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 2 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 83 (0) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, ?, 2 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 10 (live_pads_full, piano_roll, instrument_panel, master_pro_panel, remix_project, board, midi, latency, recette, studio), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Toutes remplies : non

## tour-4

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 26 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 1 ; 2 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 7 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 84 (1) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, 2 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 9 (live_screen, live_pads_full, instrument_panel, video_sheet, remix_project, board, latency, recette, studio), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Toutes remplies : non

## tour-5

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 8 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 1 ; 1 ; 0 | 0 ; ≤ 2 ; 0 | non |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 ; 14 contredite(s) | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 20/24 | ≥ 90 % | non |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 62 (2) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 1, 124, 2 | 0, 0, 0, 0 | non |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | vert (c55ffc1)/rouge (c55ffc1), 8 (16 essais sur 18 faits le 2026-11-08 ; tel-15 et tel-19 par preuve d'appli) | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 10 (padlive_party, live_pads_full, piano_roll, instrument_panel, master_pro_panel, remix_project, glossary, latency, recette, studio), 36, 0, 24 / 1 | 0, 0, 0, ≥ 16 / ≤ 3 | non |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Toutes remplies : non

## tour-6

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 0 | 0 | oui |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 0 | 0 ; ≤ 2 ; 0 | oui |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 0 (0) | ≤ 5 (0) | oui |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, 2 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, 4 (lignes rouges de telephone.tsv au 2026-11-12, avant le retest v6 : T6-T02, T6-T03, T6-T05, T6-T06) | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 36 (launch, projects, new_project, wizard, padlive, padlive_party, live_screen, live_grid, live_pads_full, session, timeline_screen, rhythm, synth, piano_roll, acid, audio_tracks, instrument_panel, mixer, simple_mix, master_pro_panel, share_sheet, video_sheet, remix_project, cut_screen, board, library, midi, plus, settings, help_page, glossary, latency, recette, studio, guide_panel, tuto_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Toutes remplies : non

## Courbe de convergence

`echelle` : v1 = le mot du testeur (bloquant, grave…), v2 = la conséquence (mission-impossible, contourne-en-N-gestes) ; `graves_declares` : ce que les testeurs ont écrit, à leur échelle ; `graves_confirmes` : ce que la fusion garde grave (revu par le testeur, ni « banc? » ni verdict « banc »). Les graves ne se comparent qu'entre tours de même échelle.

| tour | sieges | fiches | fiches_par_siege | echelle | graves_declares_par_siege | graves_confirmes_par_siege | grappes | revient | incidents_banc | promesses_tenues |
|---|---|---|---|---|---|---|---|---|---|---|
| tour-0 | 3 | 29 | 9.7 | v1 | 4.33 | 4.33 | 28 | 0 | 0 | 0/0 |
| tour-1 | 24 | 186 | 7.8 | v1 | 2.46 | 2.29 | 174 | 0 | 0 | 0/0 |
| tour-2 | 8 | 84 | 10.5 | v1 | 1.88 | 1.75 | 77 | 0 | 0 | 0/0 |
| tour-3 | 24 | 241 | 10.0 | v1 | 2.25 | 2.25 | 231 | 0 | 0 | 0/0 |
| tour-4 | 20 | 186 | 9.3 | v1 | 1.35 | 1.3 | 183 | 0 | 0 | 0/0 |
| tour-5 | 24 | 190 | 7.9 | v2 | 0.62 | 0.33 | 182 | 2 | 14 | 0/0 |
| tour-6 | 0 | 0 | 0.0 | ? | 0.0 | 0.0 | 0 | 0 | 0 | 0/0 |

Changement d'échelle entre tour-4 et tour-5 (v1 → v2) : les graves de ces deux tours ne se comparent pas.

Changement d'échelle entre tour-5 et tour-6 (v2 → ?) : les graves de ces deux tours ne se comparent pas.

**Arrêt de l'audit possible : non** (A à J remplies sur les deux derniers tours complets).
