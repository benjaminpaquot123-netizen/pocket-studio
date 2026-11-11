# Lot banc « banc6b » (étape 3, lot 6, suite) : rapport de l'auteur

Atelier `/home/user/eq-tour-5-banc6b`, branche `eq/tour-5-banc6b`, partie de `165c4e9c`. Zone : `app/src/test/.../human`, `scripts/bench`, `docs`. Rien dans `app/src/main`.

## Juge et portail (scripts, `test_ultra_bench.py` OK)
- 5 écarts « textes » : le juge apparie un rouge et un vert par leur **sujet** (mot camelCase avant la parenthèse, présent comme classe ou début de fonction dans un fichier de test que le rouge touche) ; un vert ferme un rouge par sujet nommé, jamais deux du même sujet. Paire a27f5070 / 60c6dea6. Sur l'historique réel, seuls 183e094c, 7e92aaff, 17d48e43, 2a2c2a10, c93c8aeb sortent de la liste.
- M10 : « Pas encore », « jamais », « Non  » (deux espaces), « non : », « *non* », « toujours pas » avant **corrigé** gardent le constat ouvert (18e455da / bf068ce0). Les 50 relectures du dépôt gardent leur réponse (comparé à l'ancien script).
- M9 : une preuve « rouge constaté » ne vaut que pour un `@Test` sans `@Ignore` (cas « fonction d'aide » et « ignoré » ajoutés, 105236cc / 42cd01b5) ; la ligne de relecture doit dire « rejoué sur <parent du vert> », le portail passe le parent, « relu » ou « pas rejoué » sont refusés (5c743b13 / f175438b) ; règle écrite au § 12.7 d'AUDIT-ULTRA.
- `fuse-reports.py` : un export de moins de 64 Ko ou « vide » est de l'appli (ni marque, ni doute de l'étiquette) ; « fichier » seul, « en N s » seul et la taille d'un texte ne font plus un export (8db74c19 / 727a99f6).

## Contrôles de la séance 5 (Gradle par la file)
- 5.10 : tri par cadres (premier cadre `com.pocketstudio` hors `.human` = appli). 4 exceptions provoquées (API Compose, `error()` du banc, liste vide, fichier absent) → 4 lignes « BANC », « FIN : banc » ; `ExportFormat.valueOf` raté → « plantage ». 96573c2c / 395576d2.
- 5.3 : « DIALOGUE N s » par geste dans une fenêtre à champ (0,2 à 0,3 s au renommage), > 30 s → « BANC dialogue lent ». b2de5bc7 / 678ce5c3. Témoin `Zone3DialogDiagTest` dé-@Ignore une fois : toujours rouge (`AppNotIdleException` en 60 s), la limite Robolectric reste.
- 5.11 : mesures en direct = rendu du banc du projet live (niveaux des pistes) au compteur de l'appli ; −29,3 puis −40,6 LUFS les pistes à 0,2 (avant : −14,0 fixe). e22c78b6 / be759849.
- Gardes vertes dès l'écriture (e77df8c8, 2e commit `test:` pour le seuil), rouges prouvés par banc cassé (jetable, rien commité) : 4.4 fil occupé (40,2 s ; sans `coroutineWorkersBusy` 25,2 s, d'où le seuil 35 s) ; 5.1 rendu de 1, 4, 90 mesures aux cibles −8 / −14 (MAX_SECONDS 120 → « 120.0 ») ; 5.4 ligne « tous les blocs Live sont arrêtés » et aucun pad live après « Tout couper » (ligne retirée → rouge) ; 5.2 PhoneEngine qui ne compte plus ses bascules → `aMelodyTakeWhileAnotherPartIsShownShowsItsNotes` rouge sur « part shown ».

## Juge
- Avant : 27 écarts (trace du portail de « restes »). Après : SELFTEST_A_REMPLIR.

## Ce qui reste
- 5.11 : le spectre n'est pas rendu (le banc ne le remplit pas) ; un « — (banc) » écrit à l'écran n'existe pas, seulement des tirets sans moteur du banc.
- 5.4 : la vue Live est jouée par le modèle (`stopLiveAll`), pas par le bouton ; 5.2 et 5.4 ne sont prouvés rouges qu'à la main (pas de commit jetable gardé).
- Les 22 écarts restants (verdicts « banc » sans preuve sans banc, 3 paires LayoutScan/reachTag/tel-09, journal 4.0-4.3) : lots suivants.
