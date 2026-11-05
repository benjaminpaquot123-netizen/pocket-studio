# Tableau de bord de l'audit ultra

Calculé par `scripts/bench/tableau-de-bord.py` ; « ? » = chiffre manquant, la condition n'est pas remplie.

## tour-3

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 54 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) | 0 | ≤ 2, prouvés | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) | 0 ; 0 | 0 ; ≤ 2 | oui |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 24/120 | ≥ 90 % ; ≥ 80 % | non |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 90 (0) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, ? | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides, lots non joués, sièges coupés par le banc | ?, ?, ? | 0, 0, ≤ 3 | ? |

Toutes remplies : non

## tour-4

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 27 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) | 0 | ≤ 2, prouvés | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) | 0 ; 0 | 0 ; ≤ 2 | oui |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 20/120 | ≥ 90 % ; ≥ 80 % | non |
| E | Missions réussies (oracle mesures.json) | 0/0 | ≥ 90 % | ? |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 89 (0) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, ? | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides, lots non joués, sièges coupés par le banc | ?, ?, ? | 0, 0, ≤ 3 | ? |

Toutes remplies : non

## tour-5

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 8 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) | 0 | ≤ 2, prouvés | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) | 0 ; 0 | 0 ; ≤ 2 | oui |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/2 ; 85/120 | ≥ 90 % ; ≥ 80 % | non |
| E | Missions réussies (oracle mesures.json) | 20/24 | ≥ 90 % | non |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 66 (2) | ≤ 5 (0) | non |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, ? | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides, lots non joués, sièges coupés par le banc | ?, 0, 0 | 0, 0, ≤ 3 | ? |

Toutes remplies : non

## Courbe de convergence

| tour | sieges | fiches | fiches_par_siege | graves_declares_par_siege | grappes | revient | incidents_banc | promesses_tenues |
|---|---|---|---|---|---|---|---|---|
| tour-3 | 24 | 241 | 10.0 | 2.25 | 241 | 0 | 0 | 0/0 |
| tour-4 | 20 | 186 | 9.3 | 1.35 | 186 | 0 | 0 | 0/0 |
| tour-5 | 24 | 190 | 7.9 | 0.33 | 176 | 0 | 14 | 0/2 |

**Arrêt de l'audit possible : non** (A à I remplies sur les deux derniers tours complets).
