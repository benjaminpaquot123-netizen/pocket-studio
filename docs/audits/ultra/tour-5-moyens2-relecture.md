# Relecture du lot « moyens2 » (tour 5), par un relecteur neuf
Relu : a6f718b545834de3eefc105299f92d76f1dab9d0

Cible : `git diff fx-int..fc56b596` après fusion de l'intégration (3b4235db) dans la branche : `TopBar.kt` (pastille `FolderStatus`), `RhythmViewModel.kt` (une ligne dans `startExport`), `ui-components` (`WholeWords.kt`, `FitText.kt`, `largestFitting`), trois classes de test, six captures `scale130/`, le rapport et les décisions. La fusion de l'intégration n'a touché aucun fichier du lot (banc et docs seulement), aucun conflit.

## Ce qui a été refait
- Les filtres de la ligne « tests: » du rapport, tels que le portail les lance (`scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests …`), sur fc56b596 : vert, 56 tests, 0 échec, 7 sautés (ceux que `NavScale130ScreenshotTest` saute elle-même). TopBarNameRoomTest 3, TopBarTest 15, OneLoudnessExportTest 1, OneTargetExportTest 1, WholeWordsCostTest 2, PadLiveNarrowWordsTest 1, UiScaleTest 3, NavScale130ScreenshotTest 30.
- `:app:verifyRoborazziDebug --tests '*NavScale130ScreenshotTest'` (la comparaison des captures, que `testDebugUnitTest` seul ne fait pas) : vert, les six captures réenregistrées sont bien celles que le code dessine, aucune autre ne bouge.
- Mutation, les trois correctifs retirés d'un coup (`git apply -R` d'un patch gardé hors dépôt, puis remis ; `git diff HEAD` vide après), les trois classes rejouées : les quatre tests du lot rougissent.
  - `TopBarNameRoomTest#theFolderAloneStillSaysSavedAndWhereTheTakeGoes` : « The component with TestTag = 'top_saved' is not displayed! » (les deux autres tests de la classe restent verts).
  - `OneLoudnessExportTest#anM4aRenderedAgainSaysOneLoudness` : « −9,2 LUFS intégrés » contre « Volume intégré : −9,4 LUFS ».
  - `WholeWordsCostTest#aFitTextLabelAt200FindsItsSizeInAFewLayouts` : 17 mises en page ; `#aWholeWordsLabelAt200FindsItsSizeInAFewLayouts` : 15.
  Mêmes messages que les rouges du rapport (39c35a7f, de77bd22, 938c3c6c).
- `largestFitting` relu ligne à ligne et simulé hors dépôt (Python, flottants 32 bits, 200 000 cas tirés : taille haute, plancher, pas de 0,25 à 1, seuil « tient » monotone) contre l'ancienne boucle : la même taille à chaque fois, y compris « rien ne tient » (plancher) et plancher au-dessus de la taille. La taille rendue est toujours une taille essayée et qui tient, ou le plancher.
- Captures regardées en entier : `fr_nav_simple_keys` et `fr_nav_help_labels_simple` (deux des quatre que le rapport dit ne pas avoir regardées) : la coche verte au coin du dossier, rien d'autre de déplacé, l'étiquette « Mes projets » de l'aide couvre le dossier comme avant.

## Zones sensibles
- Thread audio : rien de touché (ni moteur, ni JNI, ni files). `largestFitting` tourne sur le thread principal, pendant la mise en page.
- Sauvegarde : rien de touché. La ligne de l'export ne change que ce que l'écran d'export affiche (`ExportUi.proReport`), pas le fichier ni le projet.
- Plantages : `largestFitting` ne lit jamais hors de ses pas (`mid` < n, `sizeAt(lo)` seulement si `lo < n`) ; `r?.let { proReport = it }` garde l'ancien rapport si le nouveau rendu n'en a pas.
- Fuites : aucune ressource tenue ; la pastille n'a ni état ni effet.
- Étiquettes : `top_saved` et `top_record_into` n'existent qu'une fois à l'écran (la pastille seulement quand `nameShown` est faux, la ligne et la colonne du nom seulement quand il est vrai). La pastille est dans la rangée cliquable du dossier : TalkBack lit « Mes projets, Sauvegardé ».
- Aucun binaire hors captures PNG, aucun lien symbolique commité, décisions dans `docs/decisions/tour-5-moyens2.md` seulement.

## Constats

Bloquant, grave : aucun. Rien corrigé dans le code par cette relecture.

### m1 (mineur) : « REC dans … » n'est plus lisible à l'œil dans le cas dossier seul
La pastille rouge dit qu'une prise va ailleurs, pas où : « B · Refrain » n'est que dans la description (TalkBack, banc). Le test s'appelle « … says … where the take goes » et ne le prouve que par la description. Le rapport et la décision le disent (« ses mots entiers dans sa description ») ; c'est le prix du choix « aucune largeur, aucune ligne ». À voir au prochain essai téléphone : la partie visée reste écrite dans l'écran lui-même ?

### m2 (mineur) : `WholeWordsCostTest` prouve surtout le plancher
Sonde posée pendant la mutation (une ligne `println`, retirée) : sur les 20 cas (10 mots, 2 tests), 17 finissent au plancher (5,85 ou 6,55 sp), 3 à une taille du milieu (« Bibliothek » 6,0 ; « Effekte des Pads » 7,5 ; « Vom Gerät » 10,5), aucun ne tient à la taille pleine. Une recherche fausse au milieu serait vue par ces 3 cas seulement ; la simulation ci-dessus couvre le reste. Piste : deux ou trois boîtes plus larges (une qui tient d'emblée : une seule mise en page).

### m3 (mineur) : la dichotomie suppose « tient plus petit si tient plus grand »
Vrai pour les libellés (dit dans la décision). Si un texte tenait à 12 sp mais pas à 11 (arrondis de césure), l'ancienne boucle rendait 12 et la nouvelle peut rendre une taille plus petite qui tient, ou le plancher : jamais un mot coupé de plus qu'avant, au pire un texte un peu plus petit. Aucun témoin trouvé.

### m4 (mineur) : le filtre `*NavScale130ScreenshotTest` de la ligne « tests: » ne compare pas les captures
Sous `:app:testDebugUnitTest` (ce que le portail lance) la classe dessine les écrans sans les comparer ; la comparaison est `verifyRoborazziDebug` (rejouée ici, verte). Le filtre prouve « l'écran se dessine », pas « la capture est la bonne ».

### m5 (mineur) : ce que le lot ne prouve pas, et le dit
Le temps du gel après correctif (seul le nombre de mises en page est prouvé : 5 au plus par libellé contre 15 à 17), et l'essai téléphone en allemand à 200 %. Pour l'export, `seconds` et le réglage « drive » retenu (`giveKnownDrive`) restent ceux du premier rendu ; la durée ne change pas d'un rendu à l'autre, le drive est hors du défaut traité.

## Ce qui est juste
- Le rapport ne dit rien de plus fort que ses preuves ; les trois rouges annoncés sont ceux que la mutation redonne.
- La pastille ne prend ni largeur ni ligne : `aRowThatHeldTheNameStaysOneRow` et `theModeSimpleBarKeepsAFewWordsOfTheName` restent verts avec elle.
- Les tailles essayées sont faites par soustractions répétées, comme l'ancienne boucle : les mêmes flottants, donc les mêmes tailles au demi-point près (égalité vérifiée par le test contre l'ancienne boucle, mot par mot).

## Delta (relu 57e6a67d : l'intégration a bougé pendant la relecture)
- L'intégration a reçu le lot « gaucher » (c54debc0 : `AppRoot.kt`, `ModeChoice.kt`, six captures `mode_step`, deux classes de test). Fusionnée dans la branche (57e6a67d), sans conflit ; aucun fichier du lot moyens2 n'a changé (`git diff fc56b596 57e6a67d` sur les fichiers du lot : vide, hors cette relecture).
- `ModeChoice.kt` se sert de `WholeWordsAutoSize` (donc de `largestFitting`) : rejoué pour cela, sur 57e6a67d, `:app:verifyRoborazziDebug` avec les filtres de la ligne « tests: » plus `*ModeStepThumbTest` : vert, 59 tests, 0 échec, 7 sautés ; les captures `scale130/` (dont `fr_nav_mode_step` et `nl_nav_mode_step` de gaucher) sont celles que le code fusionné dessine.
- Les constats ne changent pas.

## Delta 2 (relu a6f718b5 : fusion de l'intégration c40b834e, lots « langues » et « kick »)
- Résolution : six captures `scale130/` en conflit, les deux côtés les ayant réenregistrées (`fr_nav_simple_song`, `fr_nav_simple_rhythm`, `fr_nav_simple_keys`, `fr_nav_simple_send`, `fr_nav_simple_more`, `fr_nav_help_labels_simple`). Aucune des deux versions n'a été gardée : les six ont été réenregistrées depuis le code fusionné (`scripts/bench/gradle-file.sh :app:recordRoborazziDebug --tests '*NavScale130ScreenshotTest'`). Les dix-sept autres captures `fr_nav_*` de la classe sont sorties identiques à celles de l'intégration ; `git status` vide après l'enregistrement, rien d'autre n'a bougé.
- `TopBar.kt` fusionné par git seul, relu : la pastille `FolderStatus` (moyens2, rangée du dossier) et le plafond de taille du mot sous ▶ (`playWord`, lot « langues ») sont dans deux fonctions distinctes, les deux comportements sont là. Aucun autre fichier du lot touché par l'intégration (`git diff fx-int a6f718b5` : les six captures et les fichiers du lot, rien de plus).
- Comparaison pixel à pixel (Pillow, hors dépôt) de chaque capture réenregistrée avec les deux côtés : contre l'intégration, seule change une case de 62 × 62 px au coin bas droit du dossier (la pastille) ; contre l'ancienne capture du lot, seule change la bande de la barre du pouce (« Annuler », ▶ « le morceau », « 120 Tempo », « Plus »). Donc les deux lots, et rien d'autre.
- Captures regardées en entier, une par une :
  - `fr_nav_simple_rhythm` : coche verte au coin du dossier, entière, sans toucher « Simple » ; barre du pouce de l'intégration (« Annuler » à gauche, la place de « Rétablir » gardée vide, ▶ « le morceau », « 120 Tempo », « Plus »), mots entiers ; les seize pads et « Kit : Classique » comme avant.
  - `fr_nav_simple_send` : même barre du haut avec la coche ; « Fini ? Partage ton morceau », le nom, « M4A · pour partager », « WAV · qualité max », « Partager », « Mes fichiers » entiers ; même barre du pouce, onglet « Partager » allumé.
  - `fr_nav_simple_song` : coche sur le dossier ; « MES PISTES » et ses cinq pistes (« Changer le son » sur deux lignes, comme des deux côtés avant la fusion) ; même barre du pouce, onglet « Mon morceau » allumé.
  - `fr_nav_simple_keys` : coche sur le dossier ; les trois pastilles de piste, « Enregistrer ma mélodie », le clavier ; même barre du pouce, onglet « Clavier » allumé.
  - `fr_nav_simple_more` : coche sur le dossier ; « Retour · Plus » et ses tuiles entières ; même barre du pouce.
  - `fr_nav_help_labels_simple` : l'étiquette « Mes projets » de l'aide couvre le dossier, la coche dépasse dessous comme avant la fusion ; les étiquettes « Parties et pistes » et « Volumes » sont posées sur la barre du pouce de l'intégration, comme dans sa propre capture. Les étiquettes se posent sur l'écran, c'est leur dessin ; rien de neuf coupé.
- Rejoué sur a6f718b5 : les filtres de la ligne « tests: » sous `:app:testDebugUnitTest` : vert, 56 tests, 0 échec, 7 sautés (les mêmes comptes par classe qu'avant) ; `:app:verifyRoborazziDebug --tests '*NavScale130ScreenshotTest'` : vert, 30 tests, 0 échec, 7 sautés.
- Non rejoué : la mutation des trois correctifs (leur code n'a pas changé depuis 57e6a67d) et les tests du moteur (lot « kick », hors de ce lot).
- Les constats ne changent pas.

Verdict : 0 bloquant, 0 grave — fusion possible
