# Tableau de bord de l'audit ultra

Calculé par `scripts/bench/tableau-de-bord.py` ; « ? » = chiffre manquant, la condition n'est pas remplie.

## tour-5

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 8 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 10 | ≤ 2, prouvés ; 0 | ? |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 1 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 ; 14 contredite(s) | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 20/24 | ≥ 90 % | non |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 62 (2) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, 0, 5 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides, lots non joués, sièges joués / coupés par le banc | ?, 0, 24 / 0 | 0, 0, ≥ 16 / ≤ 3 | ? |

Toutes remplies : non

## Courbe de convergence

| tour | sieges | fiches | fiches_par_siege | graves_declares_par_siege | grappes | revient | incidents_banc | promesses_tenues |
|---|---|---|---|---|---|---|---|---|
| tour-5 | 24 | 190 | 7.9 | 0.33 | 182 | 0 | 14 | 0/0 |

**Arrêt de l'audit possible : non** (A à I remplies sur les deux derniers tours complets).
