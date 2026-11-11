# Tableau de bord de l'audit ultra

Calculé par `scripts/bench/tableau-de-bord.py` ; « ? » = chiffre manquant, la condition n'est pas remplie.

## tour-5

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 8 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 1 ; 1 ; 0 | 0 ; ≤ 2 ; 0 | non |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 ; 14 contredite(s) | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 20/24 | ≥ 90 % | non |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 62 (2) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | 0, 1, 53, 0 | 0, 0, 0, 0 | non |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | vert (c55ffc1)/rouge (c55ffc1), 8 (16 essais sur 18 faits le 2026-11-08 ; tel-15 et tel-19 par preuve d'appli) | vert/vert, 0 | ? |
| I | Couverture : cases vides (écrans de CARTE-CODE sans route), écrans non analysés (œil neuf), lots non joués, sièges joués / coupés par le banc | 10 (padlive_party, live_pads_full, piano_roll, instrument_panel, master_pro_panel, remix_project, glossary, latency, recette, studio), 36, 0, 24 / 1 | 0, 0, 0, ≥ 16 / ≤ 3 | non |
| J | Défauts vus sur l'appareil que le banc montre (défauts cachés) | 13/20 (65 % ; à l'essai 50 %) ; 11 cachés sur 23 ; 0 écart(s) | ≥ 80 %, 0 écart | non |

Toutes remplies : non

## Courbe de convergence

`echelle` : v1 = le mot du testeur (bloquant, grave…), v2 = la conséquence (mission-impossible, contourne-en-N-gestes) ; `graves_declares` : ce que les testeurs ont écrit, à leur échelle ; `graves_confirmes` : ce que la fusion garde grave (revu par le testeur, ni « banc? » ni verdict « banc »). Les graves ne se comparent qu'entre tours de même échelle.

| tour | sieges | fiches | fiches_par_siege | echelle | graves_declares_par_siege | graves_confirmes_par_siege | grappes | revient | incidents_banc | promesses_tenues |
|---|---|---|---|---|---|---|---|---|---|---|
| tour-5 | 24 | 190 | 7.9 | v2 | 0.62 | 0.33 | 182 | 2 | 14 | 0/0 |

**Arrêt de l'audit possible : non** (A à J remplies sur les deux derniers tours complets).
