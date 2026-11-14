# Tour 5, lot « rejeux6 » : les 8 rejeux en attente passés des pixels aux étiquettes

Branche : `eq/tour-5-rejeux6`. Décisions : `docs/decisions/tour-5-rejeux6.md`. Journaux de chaque rejeu : `docs/audits/ultra/rejeux-attente/_journaux/<id>/`.

## Ce que fait le lot
- t5-01 à t5-08 visent les boutons par leur étiquette de test (`touche <étiquette>`), plus par leurs pixels ; chaque oracle « absent » a sa preuve positive (`rejeu.attendu`).
- `rejeu-defauts.sh` garde le journal de chaque rejeu ; un « tenu » exige que l'étiquette voulue soit touchée dans ce journal.
- `reachableNear` : un toucher par étiquette tombe dans la portée du pouce quand le milieu du bouton en est hors.

## Résultats des rejeux (rejeu-defauts.sh, repassés après la relecture)
- Après la relecture (graves 1 et 2) : un « tenu » exige que CHAQUE geste par étiquette ait touché son élément (ligne « DOIGT » qui le nomme, sans « REFUS ») ; un toucher par étiquette ne tombe que sur une partie visible de l'élément, à l'écran, à portée du pouce et sous le doigt, sinon il est refusé (« REFUS »).
- Aucun rejeu tenu. t5-01 n'est plus tenu : mêmes gestes et mêmes doigts que le passage tenu, mais « पार्ट: C » absent de l'écran 28 (cause à instruire, banc ou appli).
- Constaté au banc (modèle de portée du pouce du banc, 500 dp, pas une mesure sur téléphone ; restent dans `rejeux-attente/`) : t5-04, t5-05, t5-06 (les cartes « Simple » et « Pro » sont maintenant refusées dès le pas 2 au pouce gauche).
- Non tenu, cause à instruire : t5-03 (curseur « Swing » absent).
- Divergés (banc) : t5-02 au pas 97, t5-07 au pas 7 (les pas 4 à 6 sont maintenant refusés : le doigt tombait sur une autre ligne du pliable), t5-08 au pas 9.

## Rouges constatés
- reachableNear (relecture, point 3) : `2e2c5f3d` n'avait qu'un rouge de compilation (rejoué sur `62233649` : `nearestReachable` n'existe pas). Le rouge de comportement est la paire « rouge: a tag touch lands on a shown, uncovered point of its element » / « vert: » de cette reprise : 3 tests de `BenchSeatOptionsTest` rouges sur assertion avant la correction.

## Chemins (endroits qui lisent ou montrent les mêmes données)
- chemin: Banc › toucher par étiquette dans la portée du pouce | test: BenchSeatOptionsTest#aTagOutOfTheThumbsReachIsTouchedInsideTheReach
- chemin: Banc › toucher par étiquette d'un élément recouvert ou coupé | test: BenchSeatOptionsTest#aTagCoveredByAnotherElementIsTouchedWhereItShows
- chemin: Banc › verdict d'un rejeu (bouton déplacé) | test: BenchReplayReadableTest#aMovedButtonIsSaidInTheVerdict

tests: *BenchSeatOptionsTest* *BenchReplayReadableTest*

t5-01-partie-simple : oracle non tenu : « पार्ट: C » absent
t5-02-prise-apres-padlive : divergé au pas 97 (étiquette « top_more » introuvable)
t5-03-swing-tuto-pro : oracle non tenu : « curseur « Swing » » absent
t5-04-gaucher-mode : oracle non tenu : « texte « Remix » » absent
t5-05-gaucher-remix-projet : oracle non tenu : « touchable « Remix » absent
t5-06-gaucher-fin-du-bout : oracle non tenu : « « Fin du bout » [état » absent
t5-07-etoile-sons : divergé au pas 7 (étiquette « track_menu_settings » introuvable)
t5-08-mes-exports-simple : divergé au pas 9 (étiquette « vue_Drive » introuvable)
