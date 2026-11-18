# Tour 7, lot fable-repar : relecture neuve de P8 et des deux phrases

Deux relecteurs neufs, qui n'ont pas écrit le code. Lecture seule. Têtes relues : fe9502c2 (P8) puis 4170af63 (phrases).

## Partie 1 : P8 (25844180..fe9502c2)

### Relecture neuve du lot « fable-repar P8 »

Périmètre : `git diff 25844180..fe9502c2`, fichiers du tuto seulement. Lecture seule : aucun test lancé, aucun fichier du dépôt touché.

### Grave

Rien. Aucune faute à corriger avant la fusion n'a été trouvée.

### À ranger (non bloquant)

1. **Ancienne progression, cas entre deux versions.** `TutoCourse.kt`, `TutoProgress.decode` / `withPieces()`. Une progression sans clé `pieces` est lue comme « d'avant la découpe ». Une progression sauvée par une version d'après la découpe mais d'avant ce lot n'a pas la clé non plus. Si `X_do` y est fait et `X_2` pas encore, `X_2` est coché à la première lecture. Effet : une étape Facile sautée, une seule fois. Les deux cas ne peuvent pas se distinguer ; à noter dans DECISIONS.md plutôt qu'à corriger.

2. **Test de durée fragile.** `project-model/src/test/.../Tour7TutoNeedCostTest.kt` (médiane < 5 ms, pire < 60 ms). Il mesure une horloge sur la machine de CI. Le meilleur de trois tours protège, mais une machine chargée peut le rendre rouge sans faute dans le code. Si un rouge va et vient ici, c'est le test.

3. **Coût sur le fil de l'écran, léger.** `TutoChecks.heldElsewhere` (`TutoChecks.kt:934`) lance `done` deux fois. Il est appelé trois fois (notes, 303, croix) à chaque changement du projet par `AppRoot.kt:2036-2040`. Seulement pour les étapes « toute la boucle » (Lane, Notes, AcidNotes), sans partie nommée, avec au moins deux parties. Supportable ; un seul calcul partagé par les trois suffirait.

4. **Écriture d'état pendant la mesure.** `TutoPanel.kt`, `WordsRoom` : `starved()` fait `regrown++` et `held = 0` dans la passe de mesure. Borné à deux fois par étape, donc pas de boucle. Mais c'est un motif à surveiller.

5. **Mots de contrôle que plus aucun cours n'emploie.** `TutoKind.kt:252` (`changed:kit` → `KitChanged`), `:259` (`crash:once` → `CrashOnce`), `changed:volumes` nu, et la sortie `NothingWatched`. Aucun des 37 cours ne les cite. Ils restent lus par `TutoValidation.kt:89,170`, `TutoKickSoundTest.kt:123` et `Tour7TutoEveryChapterFromBlankTest.kt:20`. À garder ou à retirer ensemble.

6. **Nouveaux tests par préfixe.** `TutoPlay.undone` (`TutoPlay.kt:71-80`) et `TutoPanel.kt:1047` (`step.check.startsWith("part:")`) ajoutent des `startsWith` hors de `TutoKind`. Même famille que les restes déjà connus ; à passer par `onlyMakesPart` / `namesPart` au prochain rangement.

7. **`getOrPut` sur la table des mots.** `TutoKind.of` : deux fils peuvent analyser le même mot en même temps, et `TutoKindTest « a word is parsed once »` ne le verrait pas (un seul fil). Sans effet : l'analyse est pure, les genres n'ont pas d'état. `computeIfAbsent` rendrait la phrase du test vraie.

### Ce qui a été vérifié et tient

- **Migration P8, branche par branche.** `done`, `shown`, `signature`, `partsOf`, `channelsToDo`, `lineOf`, `followRole`, `sceneFor`, `mineFor`, `ownNeed`, `citedParts`, le filtre des étapes d'avant, `helps`, `ghostNotes`, `hint`, `toErase`, les images de « Montre-moi » : chaque genre rend ce que rendait l'ancienne branche.
- **Ordre des préfixes** (`TutoKind.kt:230-262`) : `notes:acid` avant `notes:`, `crash` et `crash:once` par égalité, les `changed:` précis avant le `changed:` général. Même ordre de fait qu'avant.
- **Mots inconnus.** Les 37 cours : aucun mot ne tombe sur `Unknown`. `TutoKindTest` le garde, avec un garde-fou « plus de 20 mots ».
- **Table des mots.** Sûre entre fils (ConcurrentHashMap, valeurs sans état). Bornée : seules les clés des cours y entrent ; `signature` reconstruit le mot d'origine (`TutoPlay.kt:41`).
- **Mémoires d'un coup** (`lastClean`, `lastNeed`, `lastLaid`) : une seule entrée immuable, comparée par identité ; pas de résultat périmé possible.
- **« Montre-moi » offert là où il montre.** `canShow` équivaut à l'ancien `demo != null` ; les deux seuls cas sans image (Notes sans piste, Sub sans piste libre) sont bien exclus.
- **« Créer la partie » pose la partie dans le morceau.** `placed` : boucle bornée par `SongPart.MAX_PARTS`, pas appelée quand l'étape pose elle-même le morceau.
- **Ancienne progression.** Sur les 37 cours : 82 morceaux, toujours `X_do` puis `X_2`/`X_3` au même contrôle ; aucune étape « écoute » ou « à la main » prise à tort (`kick_808` n'est pas pris). Une progression sauvée par ce lot garde ses morceaux à faire. Un seul chemin d'écriture (`TutoController.kt:55-57`).
- **Coche retirée** (`TutoController.kt:825`) : seulement à l'arrivée sur une étape, seulement pour une transition ou une étape de partie dont la partie est vide ou absente. Le reste de la progression n'est pas touché.
- **Rouleau.** `TutoRollHint.brought` et `lit` : priorités des opérateurs vérifiées, pas d'indice hors bornes. `PianoRoll.kt` : le mode Accords allumé par l'utilisateur reste allumé ; celui allumé par le tuto s'éteint à la fin.
- **Phrases.** « Posé : %s. » et « Il n'y a pas de partie « %s »… » sont dans les 14 langues.
- **Tests.** Les 22 fichiers ont des assertions réelles et un garde-fou « rien vu = rouge » là où ils parcourent les cours. Aucun test vide.
- **Fil audio.** Non touché par ce périmètre.

## Partie 2 : phrases du chapitre 12 et de la cymbale (3e3ce3b0..4170af63)

### Relecture ch. 12 techno / detroit (3e3ce3b0..4170af63)

Lecture seule. Aucun test relancé : je m'appuie sur le code lu et sur les résultats verts déjà présents dans `project-model/build/test-results` (18:52-18:53 UTC, après le dernier commit 4170af63).

### Grave

Rien de grave vérifié.

### À ranger

1. Les traductions de la phrase Facile ne sont gardées par aucun test.
   - `project-model/src/test/kotlin/com/pocketstudio/model/tuto/Tour6Tuto4TechnoTest.kt:392` : la boucle passait sur `texts(gap)` (text + textPro) dans les 14 langues. Elle ne passe plus que sur `gap.textFor(TutoLevel.Pro)`.
   - `Tour7TutoGapWordsMatchCrossesTest.kt` ne lit que le français.
   - Effet : si une traduction Facile dit 3 au lieu de 4, ou nomme le mauvais pad, tout reste vert.
   - Vérifié à la main aujourd'hui : les 14 langues disent 4 et nomment le pad Clap comme leur dictionnaire. Donc pas de faute en ce moment, juste un filet en moins.
   - Rangement simple : dans la boucle de la ligne 392, repasser sur les deux phrases, chacune avec son nombre attendu (Facile 4, Pro 3).

2. Le plancher `single >= 2` ne tient que sur minimal.
   - `Tour7TutoCrashWordsMatchPadTest.kt:36` et `:45`. Seule `tutos/minimal.json:393` dit encore « un seul coup » (2 niveaux = 2).
   - Si on reformule minimal, le test devient rouge par son plancher : c'est bruyant, donc pas dangereux. Mais ce jour-là, il ne vérifiera plus rien d'utile.
   - La phrase de minimal est vraie aujourd'hui (pad de cymbale vide dans sa partie).

3. Aucun test ne tient la nouvelle promesse « encore vide ».
   - Le test ne cherche que les mots « un seul coup ». Les nouvelles phrases (`tutos/techno.json:522`, `tutos/detroit.json:396`) ne sont donc plus regardées par lui.
   - La promesse est vraie par construction (voir plus bas), mais un test direct coûterait trois lignes : « si la phrase dit "encore vide", la case 0 de la cymbale est vide au début de l'étape ».

4. Le plancher `named >= 6` est exactement techno + boom_bap + lofi, deux niveaux chacun (`Tour7TutoGapWordsMatchCrossesTest.kt:52`). Même remarque que le point 2 : il casse fort, pas en silence.

5. Hors du diff, à noter : le « pourquoi » de l'étape cymbale techno (`tutos/techno.json:523`, « ailleurs, elle se répéterait toutes les mesures ») sonne bizarre en Pro, où la cymbale se répète déjà dans la partie.

6. Les deux phrases cymbale ne sont pas tournées pareil : techno dit « sur le pad « Cymbale » », detroit dit « du pad de la cymbale ». Sans effet, simple raccord possible.

### Vérifié et tient

a) Bon texte par niveau, dans chaque langue
- `TutoStep.textFor` (`TutoCourse.kt:218`) : Pro lit `textPro`, sinon `text`. Les quatre appels de `TutoPanel.kt` (656, 665, 968, 1202) passent par lui.
- Les 14 fichiers `i18n/<lang>/tuto_techno.json` sont du JSON valide, sans clé en double.
- Les 3 clés neuves (silence Facile, cymbale techno, cymbale detroit) sont dans les 14 fichiers, mot pour mot égales aux phrases des `tutos/*.json`.
- L'ancienne clé du silence (devenue `textPro`) est restée partout. Les deux anciennes clés cymbale sont parties partout : pas d'orpheline.
- Aucune valeur vide, aucune égale au français. `build/i18n-problems.txt` est vide.

b) Phrases vraies face au moteur
- Silence techno : test vert sur HEAD, joué par `TutoPlay` comme l'appli. Facile : 4 croix, pad Clap. Pro : 3 croix, pad Charley.
- Cymbale : la première case est vide au début de l'étape, en techno Pro, techno Facile et detroit. Deux raisons : l'étape n'est finie que si la case 0 de la cymbale est jouée (`TutoKind.kt:768-779`), et la création de la partie retire cette case (`TutoChecks.kt`, autour de 1530-1577).
- En Pro, le reste de la cymbale joue bien déjà (techno : cases 2, 4, 6… ; detroit : case 8). « un seul coup » était donc faux, « encore vide : un coup » est juste.
- Autres styles : seule minimal garde « un seul coup », et c'est vrai chez elle. hard_techno a une cymbale mais ne promet pas un coup unique. dnb et liquid_dnb disent « si elle est vide ».

c) Les tests
- Les deux tests neufs peuvent rougir : les commits « rouge » f9160320 et fd4a8325 existent avant les verts.
- Ils jouent le parcours par `TutoPlay` et lisent `toErase` et les pads : ils ne recopient pas la règle.
- `Tour6Tuto4TechnoTest.kt:263` (le silence nomme le pad qu'il vide) : passer à la phrase Pro est correct, ce test décrit le cas Pro. Seule la boucle de la ligne 392 perd de la couverture (point 1).

d) Traductions, coup d'œil
- de, es, it, pt-BR, ja, ko (et les 8 autres) : le nom du pad Clap, de la partie Montée et de la cymbale est le même que dans le dictionnaire de la langue.
- Le 4 est écrit deux fois en chiffres partout, sauf ar et hi qui écrivent le second en toutes lettres (comme ils le faisaient déjà pour le 3).
- Longueur : la phrase Facile fait 1 à 2 caractères de moins que la Pro dans chaque langue. Les phrases cymbale sont à peu près égales aux anciennes (de et ko un peu plus longues). Pas de risque de débordement neuf.

## Reprise après le contrôle P8 (07b8c7c1..67e791fa) : relecture neuve

Relecteur : sous-agent neuf, lecture seule, n'a écrit aucun des quatre commits. Aucun test lancé par lui ; les commits « rouge » n'ont pas été rejoués par lui.

### Grave
0 grave.

### À ranger (tour 8)
1. « la dernière rafale » / « une dernière rafale » dans l'étape roulement (`dnb.json:474`, `jump_up.json:511`, `techno.json:502`, `trance.json:401`, et les 14 langues) alors que la Montée arrive maintenant sans roulement : « une rafale » suffirait.
2. `jungle.json:327` : « la batterie roule de plus en plus vite », ambigu maintenant que rien ne roule.
3. Parcours en cours avec une ancienne Montée sauvegardée : les coups ×2 restent ; ni blocage ni « Déjà fait » (l'étape demande ×3), mais des roulements dont le texte ne parle plus. Ni migration ni test.
4. Morceau exemple (`TutoChecks.kt`, vers l.1748) : Montée moins dense qu'avant (seul le ×4 final reste) : à réécouter au `live-bench`.
5. `Tour7TutoBuildNotRolledTest` : le test de vocabulaire ne lit que le français.
6. `Tour7TutoTouchShowsNewNoteTest` : le rouleau simulé ne modélise ni le panneau du tuto ni la marge basse `covered` de `PianoRoll.kt` (l.887-890) ; la limite basse n'est prouvée que par lecture.
7. Deux hauteurs trop éloignées (La 45 et La 57) : toucher sans poser fait alterner entre grave et aigu ; pas un blocage, chaque touche montre une note entière.

### Vérifié sans défaut
`TutoRollHint.brought` (note cachée entière au-dessus du panneau en une touche, horizontal inchangé, limites haute et basse) ; `built()` (4, 8, 12, 12 coups, aucun `repeats`, silence final) ; étapes « fill », « gap », « crash » des 17 cours à Montée (aucune faite à l'arrivée, aucune impossible) ; « Montre-moi » du roulement ; 7 phrases × 14 langues (clés identiques, aucune orpheline, JSON valide ; ar contrôlée par script seulement).

Note du correcteur : après cette relecture, un seul commit de test (`TutoGrilleTest`, basse) a suivi, pour aligner deux tests sur la règle « une touche » ; aucun code de l appli n a changé depuis.
