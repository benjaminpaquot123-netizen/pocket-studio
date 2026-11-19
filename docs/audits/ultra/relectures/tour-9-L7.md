# Relecture neuve : tour-9-L7

Relu : `git diff dca571da7 4b43bd235` (`synth/PianoRoll.kt` +48, `PianoRollBigTextDottedTest.kt` neuf, rapport). Paires e5e8f6ccd « rouge » / c68d882b4 « vert » (D06) et 0d741b65a « rouge » / d04041940 « vert » (S17). Code relu : 4b43bd235. `SynthScreen.kt` lu, pas touché par le lot.

## Les tests prouvent-ils la panne ?
Contre-épreuve sur une copie à part (`git archive 4b43bd235`, `PianoRoll.kt` remis à dca571da7, tests du lot gardés), rien de commité. Lancé : `:app:testDebugUnitTest --tests '*PianoRoll*Dotted*'` (les 3 classes du lot, plus 2 tests voisins qui passent).
- 360 dp, 200 % (`PianoRollBigTextDottedTest`) : 7 sur 7 rouges sans la correction. Les 5 tests « toucher » échouent sur « no dotted note in sight after 2 touches on the line » (grille en vue : 10 px, de 658 à 668, panneau à 668). Les 2 glissés échouent plus tôt, faute de note en vue. D06 est donc prouvée à 200 %, en Simple et en Pro, en basse et en accords, et pour le bouton du panneau retouché (siège 62).
- Pliant, 200 % (`…FoldableTest`) : 2 rouges sur 7, les 2 glissés (« a drag along 43@0 wrote nothing (8 then 8 left) »). Les 5 tests « toucher » passent sans la correction.
- 360 dp, 130 % (`PianoRollMidTextDottedTest`) : 2 rouges sur 7, les 2 glissés (même message). Les 5 tests « toucher » passent sans la correction.
- S17 est donc prouvée en Simple et en Pro (glissé : rien posé avant, la note posée après).
- Le vert sur 4b43bd235 n'a pas été relancé par moi : la seule passe permise a servi à la contre-épreuve. Le vert vient du rapport ; la CI du lot doit le confirmer.

## Correction D06 (la page remonte la grille au-dessus du panneau)
- Juste. Quand moins de 4 rangées sont en vue, le rouleau demande à la page de montrer la ligne « encore N notes », les numéros de mesure et la grille, avec la hauteur du panneau en plus (`roomBelow`). Le rectangle est plus grand que l'écran, donc la page cale son haut : la ligne et le haut de la grille arrivent sous l'en-tête.
- Simple et Pro : même chemin, rien ne dépend du mode. Pliant : même calcul, la place du panneau vient de `LocalSpots`.
- Pas de boucle. L'effet ne repart que sur un changement d'étape, de tuto ou de pointillés, ou sur une touche sur la ligne (`TutoHints.focus`), donc toujours après un geste de l'utilisateur. Dès que 4 rangées sont en vue, il s'arrête tout de suite. Si le doigt fait défiler pendant la montée, le doigt l'emporte : pas de bagarre.
- Hors tuto, l'effet sort dès sa première ligne : rien ne change.

## Correction S17 (un glissé sur une note en pointillé la pose)
- Juste. La branche ne s'ouvre que si le doigt part sur une note en pointillé pas encore posée, sans note dessous, sans sélection, et tuto actif. La grille prend les déplacements (consommés) : le défilement du rouleau et de la page ne démarre pas. Quand le doigt se lève, la note est posée. En mode « Accords », c'est l'accord entier, par `dottedTouch`, comme pour un toucher.
- Deux doigts : le geste s'arrête sans rien poser, et le pincement reste possible.
- Un simple toucher passe désormais par cette branche, avec la même note et le même résultat qu'avant (`tutoTapTarget` donne la même note pour un toucher pile dessus).
- Les clés de `pointerInput` (PianoRoll.kt:964) comprennent `clip`, `hints`, `chordMode` et `selection` : aucune valeur périmée.
- Hors tuto, ou pour un glissé qui part hors d'une note en pointillé, rien ne change (défilement, toucher, zoom).

## Compose
- Pas d'allocation lourde dans le dessin. La boucle des doigts suit les mêmes motifs que le code autour.
- Pas de fuite : le `BringIntoViewRequester` est gardé en `remember`, et l'effet vit avec la composition.

## Points relevés
1. mineur, PianoRoll.kt:443 (avec SynthScreen.kt:322) : le rectangle demandé par le rouleau commence au haut de la grille, sans la place des outils (64 dp) que la page garde. À 200 % sur un petit téléphone, cette demande vient après celle de la page (200 ms contre 120 ms) et la remplace. La rangée d'outils repart alors sous « Partie : A », le défaut corrigé dans l'APK 228. Cela n'arrive que quand moins de 4 rangées sont en vue (à 100 %, rien ne change), et c'est moins grave que la grille absente. Piste : commencer à −64 dp quand la place le permet.
2. mineur, PianoRoll.kt:439 : l'attente de 200 ms est une estimation (« après les 120 ms de la page »). Sur un téléphone lent, la montée de la page peut encore tourner, et la demande neuve la remplace (même effet qu'au point 1).
3. mineur, PianoRoll.kt:434 et 441 : la hauteur de rangée (`rowPxNow`) est lue au départ de l'effet et n'en est pas une clé. Un zoom pendant les 200 ms compte donc avec l'ancienne hauteur. L'effet est petit.
4. mineur, PianoRoll.kt:435 : `TutoHints.focus` est lu en haut de `PianoRoll`, donc chaque touche sur la ligne recompose tout le rouleau. Ces touches sont rares, et ce n'est pas lourd, mais le lire dans l'effet suffirait.
5. mineur, PianoRoll.kt:1041-1053 : pendant le tuto, un glissé qui part d'une note en pointillé ne fait plus défiler le rouleau. C'est voulu par la phrase. Mais à 200 %, les longues notes d'accords couvrent une grande part de la grille : pour défiler, il faut partir à côté. Un glissé raté pose la note, et seul « Annuler » la retire. À regarder au rejeu.
6. mineur, PianoRoll.kt:1041 : avec une sélection en cours, un glissé sur une note en pointillé fait toujours défiler et ne pose rien (`selection.isEmpty()`). C'est cohérent avec le toucher, qui vide d'abord la sélection, et c'est l'ancien comportement.
7. mineur, PianoRollBigTextDottedTest.kt:207 : sur le pliant à 200 %, les 5 tests « toucher » passent sans la correction. La grille y a 182 px en vue, donc la correction ne s'y déclenche pas. Le cas du siège 62 (pliant, arabe, 200 %) n'est donc pas reproduit en rouge. Le testeur doit le constater sur ce réglage, en arabe, avant de dire « corrigé ».
8. mineur, PianoRollBigTextDottedTest.kt:212 : à 130 %, les 5 tests « toucher » passent aussi sans la correction. Ce sont des garde-fous, pas une preuve : la moitié « à 130 % » de la phrase repose sur la lecture du code (la correction n'y agit pas).
9. mineur, PianoRollBigTextDottedTest.kt:170-175 : le test du glissé ne vérifie pas que le rouleau n'a pas défilé (position avant et après). La moitié « le rouleau ne défile pas » de la phrase S17 repose seulement sur `change.consume()` (PianoRoll.kt:1052). Sans cette ligne, le test resterait sans doute vert.
10. mineur, PianoRollBigTextDottedTest.kt:175 : « posée entière » ne vérifie que la hauteur et le début de la note (`leftNotes`), pas sa longueur.
11. mineur, PianoRollBigTextDottedTest.kt:178-182 : aucun glissé sur des accords (mode « Accords », PianoRoll.kt:1055-1056), et aucun toucher d'accords en Pro.
12. mineur, PianoRollBigTextDottedTest.kt:114 : `asks < 2` laisse passer deux touches sur la ligne, alors que la phrase en dit une.
13. mineur, PianoRollBigTextDottedTest.kt:95 : le compteur affiché n'est lu que pour le message d'erreur. « Le compteur baisse d'autant » est vérifié dans le modèle, pas à l'écran.
14. mineur, commit c68d882b4 : le « vert » change aussi le test. Il prend l'origine du clic dans `boundsInRoot` (PianoRollBigTextDottedTest.kt:124), et il ajoute après le rouge le test « bouton retouché » et les classes pliant et 130 %. Le changement d'origine est légitime, et la contre-épreuve ci-dessus montre le rouge avec le test final.
15. mineur, filtre de la fiche : `*PianoRollBigTextDotted*` ne prend pas `PianoRollMidTextDottedTest` (130 %). Il faut lancer `*PianoRoll*Dotted*`, comme le fait le rapport.
16. mineur, S17 dans la liste gelée : la phrase de la liste demande aussi que « Eu faço » ne reboucle pas sur les notes déjà posées. Le rapport laisse cette moitié au lot L3. S17 ne se ferme donc pas sur L7 seul.

Verdict : 0 bloquant, 0 grave
