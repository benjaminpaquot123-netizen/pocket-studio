# Tour 6 — lot 1 « tuto-grille » : relecture neuve (v2, après le refus du portail)
Relu : 19a9137d3de8304817a9e54af6b3f2a1887d3e5d

Delta relu depuis `d247350f` (relecture précédente : `9c3cf777`, 0 bloquant, 0 grave). Code du lot touché par le
delta : `PianoRoll.kt` seulement (commit `dcba8920`, 5 lignes), plus 8 captures et 2 docs (`19a9137d`). Le
`Headphones.kt` du diff vient de la fusion de banc7 (`1f736d50`/`78017f60`), hors lot, non relu ici. Aucun test relancé.

Cause : plausible. La v1 passait `ToolRow(true, …)` (rangée en `FlowRow` sur téléphone) ; une ligne d'outils de plus
au-dessus du rouleau, dans la page Simple, descend la grille. `BenchTour1Test#notesAddedInThePianoRollAreNotNothingChanges`
touche au plus à y = 1000 px (`minOf(grid.bottom, 1000f)`) : grille plus basse, touchers hors grille, aucune note.
La v2 revient à `ToolRow(tablet, …)`, exactement l'appel d'avant le lot (vérifié contre `3d1a443a`) : la mise en page
du téléphone est celle qui passait le banc, la tablette garde son passage à la ligne. Seul changement restant : la
puce « Basse en fantôme » déplacée en 2e, même `onClick`, mêmes couleurs, pas de `testTag` perdu.

T16 non réintroduit : capture `scale130/fr_piano_roll_waltz.png` (360 dp, 130 %) regardée en entier : « Accords » et
« Basse en fantôme » entiers, grille, couloir de force et aide visibles ; rien ne chevauche. `RollToolsWordsTest`
(360 et 392 dp) vérifie la puce entière à l'écran, et rougissait sur la position d'avant le lot (`d98de596`).

## Bloquant
Aucun.

## Grave
Aucun.

## Moyen
- Ouverts de la relecture précédente, code inchangé par le delta :
  - `RhythmScreen.kt:717` (U6, chemin « peindre ») éteint « Suivre » sans condition de lecture, contrairement au toucher (l. 804).
  - `PianoRoll.kt:371` : `rollSight.value` lu en composition, recomposition du rouleau à chaque image pendant un défilement de page.
  - `PianoRoll.kt:744` : `spots.coverTop` n'est pas un état, `rollSight` peut garder un ancien haut de panneau.

## Mineur
- Le rapport dit « la ligne de plus poussait la grille hors de l'écran ». La page défile : la grille restait atteignable
  en faisant défiler, elle sortait de la zone touchée par le banc et du premier écran. La conclusion (refuser la v1)
  tient, la formule est un peu forte. Le message de commit dit « real regression: no grid in sight », même nuance.
- Aucun test du lot ne garde « la grille dans le premier écran en Simple à 360 dp » : seul le test du banc le couvre
  (de façon indirecte, par ses touchers). Acceptable, il tourne dans le self-test du portail.
- Rapport et décision v2 disent vrai : « Effacer », « En rythme 50 % », « 100 % » restent au bout de la rangée qui
  défile (visible sur la capture : « En rythm… » sous le fondu), noté « hors lot, famille R19 » ; la v1 annonçait aussi
  « Effacer » plus coupé, la v2 ne le prétend plus. Décision datée, raison et retour arrière présents.
- Ouverts de la relecture précédente : `PianoRoll.kt:1331` (flèche ↑/↓ sur la 1re note cachée d'une liste non triée),
  N8 (accord renversé non compté), `TutoGrilleTest` `@After` ne remet pas `TutoHints.step`/`page`, chiffres N8 du
  rapport non prouvés par le test.

Verdict : 0 bloquant, 0 grave — fusion possible
