# En cours

Tenu par le chef d'orchestre (méthode v3, `docs/audits/AUDIT-ULTRA.md` §13.7). Une page, mise à jour à chaque événement. C'est aussi la fiche de reprise : qui reprend le rôle de chef lit ce fichier, puis `docs/PROGRESS.md`. L'histoire du 13 au 19 novembre (tours 6, 7 et 8) est dans `docs/archive/EN-COURS-2026-11-13-a-19.md` : elle ne se suit pas.

- **Mise à jour** : 2026-11-19, 08 h 00 (heure belge).
- **Chef d'orchestre** : la discussion principale, `session_01NqonxZBBysqqWBbnsC2VTP` (outil `send_message`).
- **Temps en cours** : TOUR 9 lancé (Benjamin : « A. Et termine tout avant de réparer. ») : balayage machine 37 tutos × 15 langues, puis 37 sièges « tuto entier », AUCUNE réparation avant la fin du tri. Avant : Tour 8 clôturé (`sieges/tour-8/controle/verdict-final.md`). Aucune vague de testeurs ne joue. Avant d'écrire le plan du tour 9 : `docs/audits/ultra/PREPARATION-TOUR-9.md`.
- **Intégration** : branche `ccr-80af1a1b-mn0wec`, tête `26e315e22` ; dernière CI verte : 389, sur `c9d48c185` (l'écart n'est que du texte). Tour 7 dans `main` (PR 59) ; le tour 8 n'y est pas encore.
- **Dernier départ jugé** : `1585bb46` (tour 6, 19 sièges, 12 novembre). Contrôle final du tour 8 joué sur `c9d48c185`.
- **Lots ouverts** : aucun. **File du portail** : vide. **Discussions du tour 8** : à fermer.
- **Branches pas entrées** : (rangées le 19 novembre, 08 h) 9 branches de sièges du tour 8 (rapports seuls : `aud/tour-8-ctrl-t0b-d`, `-d2`, `-e`, `-e2`, `aud/tour-8-ctrl-t0c-a`, `-b`, `-c`, `-e`, `eq/tour-8-controle-t0a`) ; `eq/tour-8-guetteur` (essai sur un test du moteur, à laisser : le lot « machinerie » le remplace).

## Discussions du tour 9 (19 novembre, 08 h)

- 08 h 26 : T9-BALAYAGE fini (eq/tour-9-balayage 6cb53e072, archivé) : 3/3 connus retrouvés ; graves probables : grille cachée par le panneau (360 dp même à 100 %, toutes langues), « Déjà fait » faux sur 717 étapes (cause unique `TutoPlay.undone`), compte-faux maracas, 40 vélocités non vérifiées ; 305 finitions. Seul le rapport (docs) entre maintenant ; les 3 tests et `scripts/bench/tuto-sweep.sh` attendent la fin des vagues (rien n'entre dans le banc pendant une vague), puis une CI. Place libérée → siège t9-11.

- 08 h 12 : T9-PLAN fini (b39201e84 fusionné → ab7fbf4e8, archivé) : 76 sièges t9-01 à t9-76 en 10 vagues, code testé ab7fbf4e8. Vague 1 envoyée : attente-01 à 08 = t9-01 à t9-08 (t9-02, t9-04 = preuve de l'amenée hors house/techno). Rotation : t9-09 session_014RxrDJygytJrYW5v5XvDEJ, t9-10 session_014RxrDM5RKsa62BdNuCT8AS. Ouvertes : 11 (balayage + 10 sièges).

- 08 h 06 : T9-REVUE fini (7fd356dee fusionné, discussion archivée) : 0 bloquant, appli 0 grave ; 3 tests creux graves (PlusRowsFit:65, GuideNoEmptyStep:33-37, OrdinaryTestsLeaveNothing:69-94) ajoutés au lot « tests creux » de la réparation ; 15 à noter dans `docs/REVIEW.md`. Place libérée : 10 discussions ouvertes.

Rotation (Benjamin) : **31 discussions au plus** (était 11 ; Benjamin, 08 h 33 : « Ouvre 20 discussion en plus »). Une discussion finie est archivée et une neuve prend sa place. Surveillance du chef : notifications, veilleur en arrière-plan (branches `aud/tour-9-*`, `eq/tour-9-*`, `rev/tour-8-*`, intégration, CI), une minuterie de secours < 1 h avec un seul `list_sessions(children)`.
- T9-REVUE `session_014RxrDKB2N7gX4yxMKTtohb` (revue fin de phase tour 8, branche `rev/tour-8-fin-de-phase`)
- T9-BALAYAGE `session_014RxrDJMkVzhuYV7LFeF1pQ` (37 tutos × 15 langues, `eq/tour-9-balayage`)
- T9-PLAN `session_014RxrDLxKw1RRtXuZbwYQZB` (plan + fiche type des sièges, `eq/tour-9-plan`)
- Sièges en attente de fiche (machine chauffée) : 01 `session_014RxrDLsqZWPq2K97S4NFUy`, 02 `session_014RxrDJmEtLG2eK7Tp3Q6nq`, 03 `session_014RxrDMkyaiUUNQ1soeFrKE`, 04 `session_014RxrDNFN9ejLJFqz19nXgh`, 05 `session_014RxrDKrNiqYWDc6771ST75`, 06 `session_014RxrDNs8hTMn3kJV6cVgPb`, 07 `session_014RxrDMMKnYLFnxSWf95h8x`, 08 `session_014RxrDLJ3ouVbfqqYCqexSW`

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
- Le script de préparation de l'environnement est posé (19 novembre) mais pas encore vu tourner au démarrage d'une discussion neuve.
- Sur une machine du cloud, `pre-vol.sh --ci --rapide` est recalé par `test_portail4.py` (historique git coupé) : faux rouge local, vert sur la CI ; à réparer dans le lot « machinerie ».

## Prochain départ (tour 9)

Pas avant : le rangement du tour 8 (branches de sièges, revue, PR vers `main`), le lot « machinerie » fusionné, `selftest.sh` vert, et un siège pilote par mission neuve. Ordre conseillé et règles de dépense : `PREPARATION-TOUR-9.md` §5 et §6. Le chef note son compteur d'unités au départ et à la clôture.
