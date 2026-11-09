# Tableau de bord de l'audit ultra

Calculé par `scripts/bench/tableau-de-bord.py` ; « ? » = chiffre manquant, la condition n'est pas remplie.

## fumee-banc-fiable

| | Condition | Mesure | Seuil | Remplie |
|---|---|---|---|---|
| A | Items ≥ grave côté appli, non tranchés ou non faits | 2 | 0 | non |
| B | Items ≥ grave classés banc (garde + preuve sans banc) ; « banc? » ≥ moyen sans verdict | 0 ; 0 | ≤ 2, prouvés ; 0 | oui |
| C | Revient / banc contredit (≥ moyen ; mineurs) ; candidats à lire | 0 ; 0 ; 2 | 0 ; ≤ 2 ; 0 | ? |
| D | Promesses tenues (vérifiées par ≥ 2 sièges) ; vues au moins une fois | 0/0 ; 0/120 | ≥ 90 % ; ≥ 80 % | ? |
| E | Missions réussies (oracle mesures.json) | 4/4 | ≥ 90 % | oui |
| F | Moyens nouveaux (dont vus par ≥ 2 sièges) | 3 (0) | ≤ 5 (0) | oui |
| G | Relecture neuve, corrections sans test, fichiers non commités | ?, ?, ?, 0 | 0, 0, 0, 0 | ? |
| H | Hors banc : CI build + émulateur, points rouges de la recette téléphone | ?/?, ? | vert/vert, 0 | ? |
| I | Couverture : cases vides, lots non joués, sièges joués / coupés par le banc | ?, 0, 4 / 0 | 0, 0, ≥ 16 / ≤ 3 | ? |

Toutes remplies : non

## Courbe de convergence

`echelle` : v1 = le mot du testeur (bloquant, grave…), v2 = la conséquence (mission-impossible, contourne-en-N-gestes) ; `graves_declares` : ce que les testeurs ont écrit, à leur échelle ; `graves_confirmes` : ce que la fusion garde grave (revu par le testeur, ni « banc? » ni verdict « banc »). Les graves ne se comparent qu'entre tours de même échelle.

| tour | sieges | fiches | fiches_par_siege | echelle | graves_declares_par_siege | graves_confirmes_par_siege | grappes | revient | incidents_banc | promesses_tenues |
|---|---|---|---|---|---|---|---|---|---|---|
| fumee-banc-fiable | 4 | 19 | 4.8 | v2 | 0.5 | 0.5 | 19 | 0 | 6 | 0/0 |

**Arrêt de l'audit possible : non** (A à I remplies sur les deux derniers tours complets).
