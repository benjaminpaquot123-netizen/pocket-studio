# Avancement

Page courte, lue par le chef à chaque début de session. Tout ce qui date d'avant le 19 novembre 2026 est dans `docs/archive/PROGRESS-2026-10-18-a-2026-11-18.md` (historique, ne se suit pas).

## Où on en est (2026-11-20, soir)
- Tour 9, réparation arrêtée sur ordre de Benjamin (économie de quota). Entrés dans l'intégration (`ccr-80af1a1b-mn0wec`) : L9, L10, L8, L5b, L1, L17, L12, L2, L16, L11. Environ 29 défauts tentés sur 166 (registre `corrections.tsv`), 0 constaté par un testeur.
- Finis mais pas entrés : L3 (rouge TutoJourneyTest, piège Facile hat_triplet, probable effet de F032/L17), L7 (F133 ajouté après la revue), L4 (conflit Help.kt + captures), L6 (changements après la revue), L15 (oracle de rejeu « bouton 1 » absent, tab_Sons japonais).
- En pause sans revue : L5a, L13, L14. L18 et L19 archivés vides.

## Prochaine étape exacte
1. Reprendre L3, L7, L4, L6, L15 (revue neuve sur la tête, puis portail), puis L5a, L13, L14 et les miettes (restes L2/L7, F101, F111, demandes hors fichiers : L6 renameScene, L17 F032/F034, L3 F031/F041).
2. Remettre au vert TutoReadabilityBudgetTest et les captures instables, puis la grande vérification et un tour de testeurs neufs.

## Ce qui n'est pas vert
Voir `docs/audits/ultra/EN-COURS.md`, même rubrique.
