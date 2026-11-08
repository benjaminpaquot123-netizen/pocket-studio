# Relecture neuve : chantier « banc fiable », séance 2 (branche eq/tour-5-banc2, 199a5e4..)

Relecteur : un agent neuf, qui n'a rien écrit de la branche (2026-11-08). Vérifié sans problème : `base.tsv` sain (4 colonnes, en-tête, pas de doublon), `update_base` n'écrase jamais un état, tests des scripts verts.

| # | Gravité | Constat | Suite |
|---|---|---|---|
| 1 | grave | fiches v1 : bouton du geste d'avant pris quel que soit le symptôme (310/489 sans rapport) | **corrigé** : seulement pour un symptôme de geste ; base reconstruite |
| 2 | grave | `applique-corrections.py` : une citation sur plusieurs fiches, la dernière gagne (42/345) | **corrigé** : citations ambiguës listées (43), jamais devinées |
| 3 | grave | « revient » sans regarder l'ordre des tours (et tri alphabétique tour-10 < tour-2) | **corrigé** : colonne `corrige_au`, ordre numérique, candidat « revient ? » |
| 4 | grave | 7 « revient » annoncés, 1 seul vrai | **corrigé** : « revient ? » lu par un lecteur (`revient.tsv`) avant de compter ; C = « ? » tant qu'un candidat attend |
| 5 | moyen | « non commités » comptait les sorties de la fusion | **corrigé** (dossier du tour et base exclus) |
| 6 | moyen | pas de reflog = 0 corrections sans test | **corrigé** (« ? ») |
| 7 | moyen | `sieges.tsv` sans en-tête (tours 3-4) : 1er siège perdu ; tour sans mesures = « 6 lots non joués » | **corrigé** |
| 8 | mineur | D du tour 5 non mesurable (ids t5-…) | **corrigé** : D = « ? » sans id du catalogue |
| 9 | mineur | `applique-corrections` non idempotent, « voulu » écrasait « banc », relectures du banc comptées comme relectures de l'appli | **corrigé** ; `fabrique-sieges.py` tronque au-delà de 5 promesses sans le dire : noté (REVIEW.md) |
| 10 | moyen | tests manquants ou tautologiques | **corrigé** en partie : revient (ordre, lecteur oui/non), fiche d'état à son écran ; restent sans test : `measured()`, « même bouton » (REVIEW.md) |

Verdict après corrections : fusion possible.
