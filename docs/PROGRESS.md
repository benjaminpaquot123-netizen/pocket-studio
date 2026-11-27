# Avancement

Page courte, lue par le chef à chaque début de session. Tout ce qui date d'avant le 19 novembre 2026 est dans `docs/archive/PROGRESS-2026-10-18-a-2026-11-18.md` (historique, ne se suit pas).

## Où on en est (2026-11-27, 08 h 40 belge)
- Sons, deuxième passe des 12 RAR : finie, réunie (`eq/passe2-reunion` `8d4b14d9e`, 12 425 lignes, 0 erreur) et critiquée à l'aveugle (`eq/passe2-critique` `eefa5745d` : 8 chiffres tiennent, 6 fragiles, 2 faux). Plan : `docs/sons/PLAN-PASSE2.md` (restent les étapes 1 et 9).
- Sons du matin pour Benjamin (écoute seule, rien dans l'intégration) : finis. Page privée de 157 MP3 (adresse dans la conversation) : avant-après des lots du Grand Remplacement (`eq/matin-avant-apres` `e456217ef`), morceaux entiers et batteries seules avec la batterie mesurée, coups un par un et variantes (`eq/matin-batterie2` `a275ae1ce`). Toutes les discussions de la nuit sont archivées.
- Tour 9 et Grand Remplacement : en pause (décisions du 2026-11-25), voir `docs/audits/ultra/EN-COURS.md`.

## Prochaine étape exacte

- **2026-11-27, 08 h 40** : fin de l'analyse des sons (demande de Benjamin : 157 sons, c'est trop). Attendre les « fini » de `eq/son-fin-tables`, `eq/son-fin-outil` et `eq/son-fin-batterie` ; contrôler chaque branche (fichiers, 0 lien, 0 son, 0 nom d'éditeur) ; ouvrir le lot de conclusion mélodies/boucles/voix/MIDI dès que les tables sont finies, puis la critique à l'aveugle ; rejouer le best-of (un MP3 par style) et refaire la page privée. Détail : `docs/audits/ultra/EN-COURS.md`, section « Nuit du 26 au 27 ».
- **2026-11-27, matin** : attendre l'écoute de Benjamin (préférences de variantes par style, « go » ou non pour faire entrer la batterie mesurée dans l'appli). Branches `eq/mine-or-*` : Benjamin a dit « 1A », mais cette session ne peut pas supprimer de branche (refus du portail) ; lui seul peut le faire depuis GitHub, voir `docs/DECISIONS.md` du 2026-11-27. Sans lui, ce qui peut avancer : lot de correction des tables de la passe 2 (`chiffres.md`, `INVENTAIRE.md`, fausse alerte du lanceur), étape 1 de `docs/sons/PLAN-PASSE2.md`.
- **2026-11-25, 14 h 15 (belge)** : tour 9 en cours, voir la section « 25 novembre » de `docs/audits/ultra/EN-COURS.md` (entrés : robot-a3, gr-styles2 ; portail : tests-fragiles, ci-fiab ; 6 lots en remise à niveau).
- **2026-11-24, 2 h 05 (heure belge)** : démarrer `docs/audits/ultra/PLAN-APRES-ROBOT.md` (cinq discussions neuves : E audit des scénarios, A Robot, B Moteur son en Fable 5.2, C Fluidité et mémoire, D Service audio ; la section 7 « Audit Fable » fait foi). Rappel posé : `trig_01CN1EkYPspt76rNhzHDSjSf`.
0. (2026-11-22) Entrés : difficiles, rouges3, flash (voir dernière ligne de `EN-COURS.md`). Suivant : CI sur la tête, latence robot Xiaomi, mini-contrôle, V1 → fiche-tuto → V2, tour 10.
0b. (2026-11-21) Reprise : voir la dernière ligne de `docs/audits/ultra/EN-COURS.md` (problèmes difficiles à relire et fusionner, CI complète, ci-rapide, mini-contrôle, puis fiche-tuto pour le rouge T9).
1. Déjà fusionnés : L7, L4, L6, L14, L5a. Reste, un à la fois : L3 (rouge TutoJourneyTest hat_triplet, aussi rouge sur la base), L15 (oracle japonais tab_Sons), L13 (inachevé), puis les miettes (restes L2/L7, F101, F111, L6 renameScene, L17 F032/F034, L3 F031/F041).
2. Remettre au vert TutoReadabilityBudgetTest et les captures instables, puis la grande vérification et un tour de testeurs neufs.

## Ce qui n'est pas vert
Voir `docs/audits/ultra/EN-COURS.md`, même rubrique.
