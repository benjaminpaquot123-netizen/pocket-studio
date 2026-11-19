# En cours

Tenu par le chef d'orchestre (méthode v3, `docs/audits/AUDIT-ULTRA.md` §13.7). Une page, mise à jour à chaque événement. C'est aussi la fiche de reprise : qui reprend le rôle de chef lit ce fichier, puis `docs/PROGRESS.md`. L'histoire du 13 au 19 novembre (tours 6, 7 et 8) est dans `docs/archive/EN-COURS-2026-11-13-a-19.md` : elle ne se suit pas.

- **Mise à jour** : 2026-11-19, 07 h 30 (heure belge).
- **Chef d'orchestre** : la discussion principale, `session_01NqonxZBBysqqWBbnsC2VTP` (outil `send_message`).
- **Temps en cours** : ENTRE DEUX TOURS. Tour 8 clôturé (`sieges/tour-8/controle/verdict-final.md`). Aucune vague de testeurs ne joue. Avant d'écrire le plan du tour 9 : `docs/audits/ultra/PREPARATION-TOUR-9.md`.
- **Intégration** : branche `ccr-80af1a1b-mn0wec`, tête `26e315e22` ; dernière CI verte : 389, sur `c9d48c185` (l'écart n'est que du texte). Tour 7 dans `main` (PR 59) ; le tour 8 n'y est pas encore.
- **Dernier départ jugé** : `1585bb46` (tour 6, 19 sièges, 12 novembre). Contrôle final du tour 8 joué sur `c9d48c185`.
- **Lots ouverts** : aucun. **File du portail** : vide. **Discussions du tour 8** : à fermer.
- **Branches pas entrées** : 9 branches de sièges du tour 8 (rapports seuls : `aud/tour-8-ctrl-t0b-d`, `-d2`, `-e`, `-e2`, `aud/tour-8-ctrl-t0c-a`, `-b`, `-c`, `-e`, `eq/tour-8-controle-t0a`) ; `eq/tour-8-guetteur` (essai sur un test du moteur, à laisser : le lot « machinerie » le remplace).

## En arrivant (toute discussion, avant toute mission)

1. Lis seulement `CLAUDE.md`, cette page et ta fiche. Pas `PROGRESS.md`, pas les issues, pas le test de nuit : c'est le travail du chef.
2. La machine est préparée au démarrage : SDK Android dans `/opt/android-sdk`, cache des tests rempli, `local.properties` écrit. Vérifie : `ls /opt/android-sdk/platforms`. Si le dossier manque (environnement sans script de préparation), suis la recette de `docs/audits/ultra/tour-8-fiches/LISEZ-MOI.md`, puis `bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*BenchSelfTest'`.
3. Écris au chef d'orchestre, en une ligne : « prêt : <un nom court> ; modifier l'appli : oui/non ; pousser une branche neuve : oui/non ; tests chauds : oui/non ».
4. Économie de quota (Benjamin, 13 novembre) : aucun texte pour Benjamin dans ta discussion, pas de résumé ni de récit d'étape ; tes messages au chef tiennent en une ligne ; sorties de commandes filtrées (`tail`, `grep`), jamais un fichier entier quand un extrait suffit ; un sous-agent seulement quand la fiche le demande.
5. Attends ta fiche (ton rôle, tes fichiers, ton commit). Aucune question à Benjamin : une ligne au chef.
6. Quand tu as poussé : « fini », puis silence. Tu n'attends pas ta CI ; le chef te réveille seulement si elle est rouge.

## Ce qui n'est pas vert (19 novembre)

- Le tour 8 n'est pas dans `main` ; sa revue de fin de phase n'est pas faite.
- Tuto, 2 graves connus (chapitre 12, maracas) et P6-grille (japonais, pliable, 200 %) : pas pris, pour le tour 9.
- `Coquille2SeatTest.p5` capricieux ; 4 à 6 captures bougent d'une CI à l'autre (`shell_audio_pages_end_*`, `130_land_nav_sheet_share`, `sons_synth_take`, `share_overrun_*`).
- `corrections.tsv` n'a pas les verdicts du contrôle final (17 « pas pris », 23 « tentés » jamais confirmés).
- `scripts/bench/ci-etat.sh` ne répond pas (outil `gh` non connecté) : la garde « CI rouge ou en cours » du pré-vol ne voit rien.
- Les 4 pilotes des niveaux et les sièges d'élargissement n'ont jamais été joués.
- Sur une machine du cloud, `pre-vol.sh --ci --rapide` est recalé par `test_portail4.py` (historique git coupé) : faux rouge local, vert sur la CI ; à réparer dans le lot « machinerie ».

## Prochain départ (tour 9)

Pas avant : le rangement du tour 8 (branches de sièges, revue, PR vers `main`), le lot « machinerie » fusionné, `selftest.sh` vert, et un siège pilote par mission neuve. Ordre conseillé et règles de dépense : `PREPARATION-TOUR-9.md` §5 et §6. Le chef note son compteur d'unités au départ et à la clôture.
