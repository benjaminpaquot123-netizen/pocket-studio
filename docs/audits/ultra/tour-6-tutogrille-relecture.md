# Tour 6 — lot 1 « tuto-grille » : relecture neuve
Relu : 9c3cf7771385a8219d6c3ddcdd68096f1c2669e0

Relecture par lecture du code et de l'historique (aucun test relancé). Base : `3d1a443a`. 18 commits : chaque « rouge »
ne touche que des tests et précède son « vert », qui ne touche que `PianoRoll.kt` ou `RhythmScreen.kt`. Code touché :
ces deux fichiers seulement (plus 3 fichiers de test, 8 captures, 2 docs). Les 9 défauts du lot ont chacun leur
correction et un test qui, à la lecture, rougit sans elle (sauf T4 pads / T15, garde-fou annoncé comme tel).

## Bloquant
Aucun.

## Grave
Aucun.

## Moyen
- `RhythmScreen.kt:715` (U6, chemin « peindre ») : `if (follow) { chosenBar = bar; follow = false }` sans condition de
  lecture, alors que le chemin « toucher » (l. 802) exige `followedBar >= 0`. Scénario : transport arrêté, « Suivre »
  allumé (valeur d'office), un glissé sur les cases éteint « Suivre » ; au Play suivant la page ne suit plus la tête et
  la puce « Suivre » est éteinte sans que l'utilisateur l'ait touchée. Non couvert par un test (seul le toucher l'est).
  À aligner sur le toucher (lire `followedBar` via `rememberUpdatedState`).
- `PianoRoll.kt:370` et `PianoRoll.kt:650` (T4/N4) : `rollSight.value` est lu en composition (dans `PianoRoll` et dans
  `Grid`), et `onGloballyPositioned` (l. 738) le réécrit à chaque changement de position. Le rouleau est dans la page
  défilante de `SynthScreen.kt:291` : pendant un défilement de la page où le rouleau est coupé (bord de l'écran, ou
  panneau du tuto ouvert), `from`/`to` changent à chaque image, donc `PianoRoll` et `Grid` se recomposent à chaque
  image, y compris hors tuto. Pas de boucle (l'écriture est filtrée par égalité et la mise en page ne dépend pas de
  `sight`), mais c'est du travail lourd à chaque image. Parade : ne lire la valeur que dans les effets
  (`snapshotFlow`) et passer l'état, pas sa valeur, à `TutoNotesLeft` (lu dans son `derivedStateOf`).
- `PianoRoll.kt:743` : `spots.coverTop` est un simple `var` (`Spotlight.kt:74`), pas un état. Si le panneau du tuto
  change de hauteur sans que le rouleau bouge (consigne plus longue à l'étape suivante, panneau replié/déplié), le
  rappel de position du rouleau ne repart pas et `rollSight` garde l'ancien haut du panneau : le placement de l'étape
  suivante (et la flèche ↑/↓) se fait sur des rangées qui ne sont plus visibles. Plausible, non prouvé ; les tests
  n'ouvrent qu'une étape sans changer le panneau.

## Mineur
- `PianoRoll.kt:1330` : `y(hidden.first()) < from` prend la première note cachée d'une liste non triée ; avec des
  notes cachées à la fois au-dessus et au-dessous, la flèche ↑/↓ dépend de l'ordre des pointillés.
- `PianoRoll.kt:1371` (N8) : le compte « touches = départs distincts » suppose qu'un toucher en mode Accords pose
  exactement l'accord en pointillé ; si l'accord de l'harmonie diffère (renversement), le compteur ne baisse pas.
  Non vérifié dans ce lot.
- `TutoGrilleTest.kt:42` : l'`@After` remet `cells` et `notes` mais pas `TutoHints.step` (le test T11 y écrit
  « …_next ») ni `page` ; `active` est remis par `AppRoot` à la fermeture. Même schéma que `TutoScreenTest`, fuite
  possible d'état global entre classes Robolectric.
- Rapport, N8 : « 16 → 4 en house » alors que le verdict parle de 36 notes pour 9 accords ; le test ne vérifie que la
  règle (N touches, pas N notes), pas ces nombres. Préciser le niveau ou retirer les chiffres.
- Rapport, T4 pads / T15 : « déjà tenu » est bien présenté comme garde-fou sans rouge, et le test accepte une aide
  des pads « défilée hors de vue » (`help.height <= 1f`) : formulation exacte, rien de plus fort que la preuve.
- Rapport, N8 : l'affirmation porte bien sur « Accords » allumé (le test l'allume) ; hors mode Accords le compteur
  reste en notes, ce qui est voulu (un toucher = une note).
- Défilement « jamais tout seul » hors tuto : respecté (le défilement horizontal de T11 est sous
  `TutoHints.active`, le placement vertical ne joue que comme avant). Rien à redire.

Verdict : 0 bloquant, 0 grave — fusion possible
