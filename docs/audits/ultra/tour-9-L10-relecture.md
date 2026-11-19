# Tour 9, lot L10 « divers » : seconde relecture neuve
Relu : 99f9df46

Diff lu : c79d3442..99f9df46 (FitText.kt, CutScreen.kt, FitTextBoxTest, CutStaleMessageTest, CutCurrentMessageScrollTest, LiveArabicTitlesTest, Tour9TabsWholeTest).

rouge constaté : 6042f7e4 : rejoué sur e988fee0 : avec le `CutScreen.kt` d'e988fee0, `CutCurrentMessageScrollTest#theOldMessageLeftUnderAWorkDoesNotPullTheColumnDown` est rouge (« the column ran down to it expected:<0.0> but was:<1910.0> »), `aNewMessageStillBringsTheColumnDown` reste vert ; fichier remis, `git status` propre.

## Réponses à la première relecture
- grave 1 tenu : `CutStaleMessageTest.kt` est identique à c79d3442 (diff vide) ; `staleMessageKeepsTheInstructionOnTop` est vert sur 99f9df46 (le « drive » du banc est sauté, normal).
- grave 2 tenu : `shrinkToFit` est revenu à la base (diff vide sur cette fonction) ; `aShrunkContentWiderThanItsBoxStaysInside` est déclaré « garde » dans le test, le rapport et les décisions.

## Constats
1. **moyen** : le scénario du rouge F139 (un travail en cours avec l'ancien message gardé dessous) ne sort jamais du vrai routeur : `launchWork` vide le message au départ (`StemBoardRouter.kt:525`), donc après un travail la Découpe suit bien le résultat, comme voulu. Le chemin réel corrigé par le même mécanisme est la note locale (« Aucune coupe trouvée… » puis effacée au toucher : la colonne redescendait vers l'ancien message) ; il n'a pas de test. Le testeur neuf dira si c'est bien le cas du siège.
2. **moyen** (reporté) : au plancher de 1 sp, un texte ajusté reste entier mais peut devenir illisible (sous-titre du compteur du Live à 200 %) ; remède : plus de place, lot `live/*`.
3. **mineur** : `aShrunkContentWiderThanItsBoxStaysInside` (et sans doute les deux tests « case étroite ») ne peuvent pas rougir : un `Text` est rogné à sa case. Gardes honnêtement déclarées, pas des preuves.
4. **mineur** : quand rien ne tient, `getFontSize` fait quelques mises en page de plus ; une case qui s'anime depuis 0 fait sauter la taille du texte. Rien vu sur les captures.
5. **mineur** : `arrived` et `old` sont mémorisés dans `curve()` ; si l'écran change de disposition (rotation), le message du moment redevient « trouvé à l'arrivée ». Même comportement que la base pour `arrived`.

## Relancé sur 99f9df46
FitTextBoxTest 7/7, CutStaleMessageTest 1/1 (+ drive sauté), CutCurrentMessageScrollTest 2/2, LiveArabicTitlesTest 2/2, CutBoutBigTextTest 5/5, Tour9TabsWholeTest 3/3 (+ drive sauté) : tout vert. Aucun plantage possible vu : `largestFitting` rend null sans boucle quand le plancher est sous 1 sp, et `lowEnough` rend alors 1 sp.

Verdict : 0 bloquant, 0 grave
