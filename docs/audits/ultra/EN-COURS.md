# En cours

Tenu par le chef d'orchestre (méthode v3, `docs/audits/AUDIT-ULTRA.md` §13.7). Une page, mise à jour à chaque événement. C'est aussi la fiche de reprise : qui reprend le rôle de chef lit ce fichier, puis `docs/PROGRESS.md`. L'histoire du 13 au 19 novembre (tours 6, 7 et 8) est dans `docs/archive/EN-COURS-2026-11-13-a-19.md` : elle ne se suit pas.

- **Mise à jour** : 2026-11-19, 15 h 45 (heure belge).
- **Chef d'orchestre** : la discussion principale, `session_01NqonxZBBysqqWBbnsC2VTP` (outil `send_message`).
- **Temps en cours** : RÉPARATION du tour 9. Liste gelée (Benjamin : « Objectif. Terminé Tout les défaut trouvé et stoppé ») : `docs/audits/ultra/tour-9-tri/` (défauts D01…D18, finitions F001…F136). Plus aucun siège qui cherche ; des testeurs neufs constatent seulement. Toute faute neuve vue pendant la réparation entre dans la liste. On s'arrête quand la liste est vide. Points à Benjamin seulement sur sa demande (on le tutoie).
- **Intégration** : branche `ccr-80af1a1b-mn0wec`, tête poussée `7455749a5` (CI 44537768073 en cours) ; commits de texte locaux dans `/home/user/fx-int2` à pousser après elle.
- **Lots ouverts** (un fichier = un lot) :
  - L1 tuto-panneau (sous-agent du chef, `/home/user/wt-L1`, `eq/tour-9-L1`) : D02, D13 (visibilité), D14.
  - L7 piano roll (sous-agent du chef, `/home/user/wt-L7`, `eq/tour-9-L7`) : D06, S17 ; selftest en cours.
  - L6 Structure `session_014RxrDMkhUdqXCdciTjjS1T` (`eq/tour-9-L6`) : D08, D10 + F100…F111.
  - L2 Mix `session_014RxrDMZBvLi55WqYcZy7t7` (`eq/tour-9-L2`) : D01, D11, D12, S30, S34 + F023…F030 (Mix), F130, F136.
  - L8 feuilles et partage `session_014RxrDKvhB4oNgXkVj2tghN` (`eq/tour-9-L8`) : F127, F131 (graves) + F030 (Partage), F119…F135.
  - L4 grille rythme `session_014RxrDNJtiS2AgPKRrbmdzj` (`eq/tour-9-L4`) : D04 (bloquant), S56, S76, F053 (graves) + F047…F057.
  - L9 banc et tests creux `session_014RxrDKb4jSvE5dmsmczsoZ` (`eq/tour-9-L9`) : D17 (banc), 3 tests creux, pré-vol sur clone coupé, ci-etat.sh, Coquille2SeatTest p5.
- **Paquets** : 1 = L1, L7, L6 ; 2 = L2, L3, L4 ; puis L8, L5, secondes passes. L3 (validation, D03 D07 D09 D15 D18 S67) s'ouvre après L1 (le tuto est une seule zone) ; L5 (contenu des cours) après L3. Le message de repli muet des kits (D17, AppRoot) attend la fermeture de L1.
- **Pas encore de lot** : L10 divers (F139 CutScreen, F142 SynthScreen, F131 Retour avec tuto ouvert dans AppRoot, message D17 de repli du kit ; après L1), secondes passes L1 (22 finitions) et L7 (7).
- **Branches de rapports pas entrées** : `aud/tour-9-*-r2`, `aud/tour-9-verif-d17`.

## En arrivant (toute discussion, avant toute mission)

1. Lis seulement `CLAUDE.md`, cette page et ta fiche. Pas `PROGRESS.md`, pas les issues, pas le test de nuit : c'est le travail du chef.
2. La machine est préparée au démarrage : SDK Android dans `/opt/android-sdk`, cache des tests rempli, `local.properties` écrit. Vérifie : `ls /opt/android-sdk/platforms`. Si le dossier manque (environnement sans script de préparation), suis la recette de `docs/audits/ultra/tour-8-fiches/LISEZ-MOI.md`, puis `bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*BenchSelfTest'`.
3. Pas de message « prêt » (boost éco, 19 novembre) : la fiche est dans ton message d'ouverture, tu travailles tout de suite ; tu n'écris au chef qu'une fois, « fini <sha> » ou « bloqué : … ».
4. Économie de quota (Benjamin, 13 novembre) : aucun texte pour Benjamin dans ta discussion, pas de résumé ni de récit d'étape ; tes messages au chef tiennent en une ligne ; sorties de commandes filtrées (`tail`, `grep`), jamais un fichier entier quand un extrait suffit ; un sous-agent seulement quand la fiche le demande.
5. Ta fiche est dans ton message d'ouverture (ton rôle, tes fichiers, ton commit). Aucune question à Benjamin : une ligne au chef.
6. Quand tu as poussé : « fini », puis silence. Tu n'attends pas ta CI ; le chef te réveille seulement si elle est rouge.

## Ce qui n'est pas vert (19 novembre)

- Le tour 8 n'est pas dans `main` ; sa revue de fin de phase n'est pas faite.
- Tuto, 2 graves connus (chapitre 12, maracas) et P6-grille (japonais, pliable, 200 %) : pas pris, pour le tour 9.
- `Coquille2SeatTest.p5` capricieux ; 4 à 6 captures bougent d'une CI à l'autre (`shell_audio_pages_end_*`, `130_land_nav_sheet_share`, `sons_synth_take`, `share_overrun_*`).
- `corrections.tsv` n'a pas les verdicts du contrôle final (17 « pas pris », 23 « tentés » jamais confirmés).
- `scripts/bench/ci-etat.sh` ne répond pas (outil `gh` non connecté) : la garde « CI rouge ou en cours » du pré-vol ne voit rien.
- Les 4 pilotes des niveaux et les sièges d'élargissement n'ont jamais été joués.
- Le script de préparation de l'environnement est posé (19 novembre) mais pas encore vu tourner au démarrage d'une discussion neuve.
- Sur une machine du cloud, `pre-vol.sh --ci --rapide` est recalé par `test_portail4.py` (historique git coupé) : faux rouge local, vert sur la CI ; à réparer dans le lot « machinerie ».

## Prochain départ (tour 9)

Pas avant : le rangement du tour 8 (branches de sièges, revue, PR vers `main`), le lot « machinerie » fusionné, `selftest.sh` vert, et un siège pilote par mission neuve. Ordre conseillé et règles de dépense : `PREPARATION-TOUR-9.md` §5 et §6. Le chef note son compteur d'unités au départ et à la clôture.

## Arrêt du 2026-11-20 (ordre de Benjamin)
- Entrés : L9, L10, L8, L5b, L1, L17, L12, L2, L16, L11.
- En attente (finis, refusés au portail) : L3 (sortie 5, TutoJourneyTest), L7 (3, F133 après revue), L4 (4, conflit Help.kt), L6 (3, changements après revue), L15 (5, oracle de rejeu).
- En pause sans revue : L5a, L13, L14.
- Pas vert : TutoReadabilityBudgetTest (depuis L5b), captures audio_pages_end instables, tempo_sheet / nl_tempo_sheet / tracks rouges sur la base, écarts du juge 123 → 125.
- Reprise éco (2026-11-20, soir) : L7, L4, L6 entrés après fusion de l'intégration côté chef (captures et F111 pris de l'intégration, test F111 du lot retiré car doublon non branché). Juge : 134 écarts. Restent : L3, L15 (rouges), L5a, L13, L14 (sans relecture).
