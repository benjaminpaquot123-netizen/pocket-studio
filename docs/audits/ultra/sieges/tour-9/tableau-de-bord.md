# Tableau de bord de l'audit ultra

Calculé par `scripts/bench/tableau-de-bord.py` ; « ? » = chiffre manquant, la condition n'est pas remplie.

## tour-0

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 13 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 3 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 0/0 ; 0/120 ; 0 oui, 0 non, 0 pas vue | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 10 (0) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, 1 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 24 (new_project, padlive_party, live_screen, live_pads_full, timeline_screen, synth, piano_roll, acid, audio_tracks, instrument_panel, mixer, simple_mix, master_pro_panel, video_sheet, midi, plus, settings, help_page, glossary, latency, recette, studio, guide_panel, tuto_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 0, rejoué 0, tenté 0, pas pris 0 — réparés : 0 sur 0 pris (seul « constaté » se dit corrigé ; les pas pris à part)

Toutes remplies : non

### tour-0 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## tour-1

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 51 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 4 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 0 | 0 ; ≤ 2 ; 0 | oui |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 0/0 ; 0/120 ; 0 oui, 0 non, 0 pas vue | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 77 (3) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, ?, 1 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 11 (live_screen, piano_roll, acid, instrument_panel, board, midi, help_page, glossary, recette, studio, guide_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 0, rejoué 0, tenté 0, pas pris 0 — réparés : 0 sur 0 pris (seul « constaté » se dit corrigé ; les pas pris à part) ; états d'avant la règle : banc 10, corrigé 35

Toutes remplies : non

### tour-1 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## tour-2

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 14 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 1 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 0 | 0 ; ≤ 2 ; 0 | oui |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 0/0 ; 0/120 ; 0 oui, 0 non, 0 pas vue | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 29 (0) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, ?, 1 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 20 (padlive_party, live_screen, live_grid, live_pads_full, timeline_screen, piano_roll, acid, instrument_panel, video_sheet, remix_project, board, midi, plus, settings, help_page, glossary, latency, recette, studio, guide_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 0, rejoué 0, tenté 0, pas pris 0 — réparés : 0 sur 0 pris (seul « constaté » se dit corrigé ; les pas pris à part) ; états d'avant la règle : banc 6, corrigé 50, voulu 3

Toutes remplies : non

### tour-2 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## tour-3

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 54 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 1 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 2 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 0/0 ; 0/120 ; 1 oui, 2 non, 0 pas vue | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 83 (0) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, ?, 1 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 10 (live_pads_full, piano_roll, instrument_panel, master_pro_panel, remix_project, board, midi, latency, recette, studio), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 0, rejoué 0, tenté 0, pas pris 0 — réparés : 0 sur 0 pris (seul « constaté » se dit corrigé ; les pas pris à part) ; états d'avant la règle : corrigé 28, banc 9

Toutes remplies : non

### tour-3 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## tour-4

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 26 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 1 ; 2 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 7 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 0/0 ; 0/120 ; 1 oui, 1 non, 0 pas vue | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 84 (1) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, 1 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 9 (live_screen, live_pads_full, instrument_panel, video_sheet, remix_project, board, latency, recette, studio), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 0, rejoué 0, tenté 0, pas pris 0 — réparés : 0 sur 0 pris (seul « constaté » se dit corrigé ; les pas pris à part) ; états d'avant la règle : banc 2

Toutes remplies : non

### tour-4 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## tour-5

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 8 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 1 ; 1 ; 0 | 0 ; ≤ 2 ; 0 | non |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 0/0 ; 0/120 ; 24 oui, 54 non, 7 pas vue ; 14 contredite(s) | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 20/24 | ≥ 90 % | non |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 62 (2) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 1, ?, 1 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | vert (c55ffc1)/rouge (c55ffc1), 8 (16 essais sur 18 faits le 2026-11-08 ; tel-15 et tel-19 par preuve d'appli) | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 10 (padlive_party, live_pads_full, piano_roll, instrument_panel, master_pro_panel, remix_project, glossary, latency, recette, studio), 36, 0, 24 / 1 | 0, 0, 0, ≥ 16 / ≤ 3 | non |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 0, rejoué 0, tenté 0, pas pris 0 — réparés : 0 sur 0 pris (seul « constaté » se dit corrigé ; les pas pris à part)

Toutes remplies : non

### tour-5 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## tour-6

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 8 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 7 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 12 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 8/22 ; 0/120 ; 39 oui, 38 non, 1 pas vue ; 14 contredite(s) | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 6/12 | ≥ 90 % | non |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 50 (1) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, ?, 1 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/rouge (9f26c244, passage 43375199284 : JourneyTest.aFirstSongInTheModeSimple), 6 (lignes rouges de telephone.tsv au 2026-11-12, après le retest v6 : T6-T01, T6-T02, T6-T03, T6-T04, T6-T05, T6-T06) | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 13 (padlive_party, live_screen, piano_roll, acid, audio_tracks, master_pro_panel, video_sheet, remix_project, board, midi, glossary, latency, studio), 36, 2, 11 / 1 | 0, 0, 0, ≥ 16 / ≤ 3 | non |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 0, rejoué 0, tenté 0, pas pris 0 — réparés : 0 sur 0 pris (seul « constaté » se dit corrigé ; les pas pris à part)

Toutes remplies : non

### tour-6 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## tour-7

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 5 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 1 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 0/1 ; 0/120 ; 12 oui, 43 non, 0 pas vue ; 5 contredite(s) | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 9/10 | ≥ 90 % | oui |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 31 (1) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, ?, 1 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 22 (padlive, padlive_party, live_screen, live_grid, live_pads_full, synth, piano_roll, acid, audio_tracks, instrument_panel, master_pro_panel, video_sheet, remix_project, board, library, midi, settings, glossary, latency, recette, studio, guide_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 0, rejoué 0, tenté 0, pas pris 0 — réparés : 0 sur 0 pris (seul « constaté » se dit corrigé ; les pas pris à part)

Toutes remplies : non

### tour-7 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## tour-8

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 4 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 2 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 0/1 ; 0/120 ; 14 oui, 19 non, 0 pas vue | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 12 (0) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, 0, 1 | 0, 0, 0, 0 | non |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 32 (launch, projects, new_project, wizard, padlive, padlive_party, live_screen, live_grid, live_pads_full, timeline_screen, rhythm, synth, piano_roll, acid, audio_tracks, instrument_panel, mixer, master_pro_panel, video_sheet, remix_project, cut_screen, board, library, midi, plus, settings, help_page, glossary, latency, recette, studio, guide_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 5, rejoué 0, tenté 23, pas pris 17 — réparés : 5 sur 28 pris (seul « constaté » se dit corrigé ; les pas pris à part)

Toutes remplies : non

### tour-8 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## tour-9

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 31 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 6 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 5 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois ; toutes les lignes « promesse: » | 0/1 ; 0/120 ; 2 oui, 6 non, 1 pas vue | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 55 (6) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 0, 552, 1 | 0, 0, 0, 0 | non |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 27 (projects, new_project, wizard, padlive, padlive_party, live_screen, live_grid, live_pads_full, timeline_screen, piano_roll, audio_tracks, instrument_panel, master_pro_panel, video_sheet, remix_project, cut_screen, board, library, midi, plus, settings, help_page, glossary, latency, recette, studio, guide_panel), 36, ?, ? / ? | 0, 0, 0, ≥ 16 / ≤ 3 | ? |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 17/20 (85 % ; à l'essai 50 %) ; 14 cachés sur 26 ; 0 écart(s) | ≥ 80 %, 0 écart | oui |

Réparations (registre corrections.tsv) : constaté 0, rejoué 7, tenté 59, pas pris 6 — réparés : 0 sur 66 pris (seul « constaté » se dit corrigé ; les pas pris à part)

Toutes remplies : non

### tour-9 par niveau

| Niveau | Sièges | Réussi sans aide | Avec l'aide de l'appli | Bloqué | Gestes faits / chemin idéal | Gestes avant le premier son | Surprises | Mots incompris |
|---|---|---|---|---|---|---|---|---|
| 0 (curieux) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 1 (débutant motivé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 2 (moyen) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 3 (confirmé) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |
| 4 (pro) | 0 | 0 | 0 | 0 | ? / ? | ? | 0 | 0 |

## Courbe de convergence

`echelle` : v1 = le mot du testeur (bloquant, grave…), v2 = la conséquence (mission-impossible, contourne-en-N-gestes) ; `graves_declares` : ce que les testeurs ont écrit, à leur échelle ; `graves_confirmes` : ce que la fusion garde grave (revu par le testeur, ni « banc? » ni verdict « banc »). Les graves ne se comparent qu'entre tours de même échelle.

| tour | sieges | fiches | fiches_par_siege | echelle | graves_declares_par_siege | graves_confirmes_par_siege | grappes | revient | incidents_banc | promesses_tenues | promesses_pas_vues |
|---|---|---|---|---|---|---|---|---|---|---|---|
| tour-0 | 3 | 29 | 9.7 | v1 | 4.33 | 4.33 | 28 | 0 | 0 | 0/0 | 0 |
| tour-1 | 24 | 186 | 7.8 | v1 | 2.46 | 2.29 | 174 | 0 | 0 | 0/0 | 0 |
| tour-2 | 8 | 84 | 10.5 | v1 | 1.88 | 1.75 | 77 | 0 | 0 | 0/0 | 0 |
| tour-3 | 24 | 241 | 10.0 | v1 | 2.25 | 2.25 | 231 | 0 | 0 | 1/3 | 0 |
| tour-4 | 20 | 186 | 9.3 | v1 | 1.35 | 1.3 | 183 | 0 | 0 | 1/2 | 0 |
| tour-5 | 24 | 190 | 7.9 | v2 | 0.62 | 0.33 | 182 | 2 | 14 | 24/78 | 7 |
| tour-6 | 54 | 262 | 4.9 | v2 | 0.24 | 0.17 | 219 | 0 | 81 | 39/77 | 1 |
| tour-7 | 20 | 126 | 6.3 | v2 | 0.4 | 0.25 | 112 | 0 | 29 | 12/55 | 0 |
| tour-8 | 15 | 94 | 6.3 | v2 | 0.33 | 0.27 | 83 | 0 | 17 | 14/33 | 0 |
| tour-9 | 42 | 333 | 7.9 | v2 | 1.0 | 0.76 | 279 | 0 | 44 | 2/8 | 1 |

Changement d'échelle entre tour-4 et tour-5 (v1 → v2) : les graves de ces deux tours ne se comparent pas.

**Arrêt de l'audit possible : non** (A à J remplies sur les deux derniers tours complets).
