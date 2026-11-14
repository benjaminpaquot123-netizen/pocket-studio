# Relecture neuve : chantier « banc fiable », séance 1 (branche eq/tour-5-banc, dff04ad..)

Relecteur : un agent neuf, qui n'a écrit aucune ligne de la branche (2026-11-08). Lecture seule, tests des scripts lancés.
Vérifié sans problème : 0 lien symbolique, aucune modification de `app/src/main`, aucun morceau ni projet de Benjamin, aucun secret ;
format du tampon identique entre écriture et vérification ; reprise des gestes depuis le journal (#1 ≠ #10) ;
XML tronqué = rouge ; `BenchSeparationWaitTest` vérifie bien ce que la personne voit.

| # | Gravité | Constat | Suite |
|---|---|---|---|
| 1 | grave | `selftest.sh` : un 2e passage Gradle qui ne produit rien (mémoire, blocage, plantage) laissait le verdict vert sur le 1er seul | **corrigé** : liste des classes attendues lue dans les sources (une classe sans résultat = rouge) ; un Gradle en échec sans test rouge = rouge ; un journal Gradle par passage ; testé |
| 2 | grave | `tableau-de-bord.py` : une grappe « banc? » jugée « appli » sortait de A pour toujours | **corrigé** : elle ne sort de A que tant qu'elle n'a pas de verdict ; test rouge avant, vert après |
| 3 | moyen | `PS_SKIP_SELFTEST` sautait tout le portail | **corrigé** : plus aucune dérogation (un rouge connu va en quarantaine, avec tampon) |
| 4 | moyen | le tampon (résumé du diff) ne voyait ni les fichiers nouveaux ni deux changements de même taille | **corrigé** : empreinte sha256 du diff et des fichiers non suivis sous app/ et scripts/ ; chemin du lot résolu avant `cd` |
| 5 | mineur | `slowWorkDone` : pile de tous les fils toutes les 50 ms ; 120 s par « attends » si la séparation reste « occupée » en attendant la personne | noté dans `docs/REVIEW.md` (à surveiller au tour de fumée, séance 3) |
| 6 | mineur | le test de renommage réessaie sans le dire | **corrigé** : chaque nouvel essai est écrit dans la sortie du test ; fuite d'état entre classes notée (coupable à trouver, séance 3) |
| 7 | mineur | échecs non gardés pour tout `PS_REJEUX_DIR` | **corrigé** : seulement pour `rejeux-attente/` |

Verdict après corrections : fusion possible (portail vert exigé par `fusion-equipes.sh`).
