# Tour 9, lot L13 : relecture neuve

Relu : c72349d0

Périmètre : commits du lot (2428f6ee..c72349d0, sans fusions) sur `TutoPanel.kt`, `TutoController.kt` et les tests `Tour9L13*`. Lecture seule, aucun test lancé.

## Défauts

- **Corrigé** (c9a0bdb1) — grave : couché (sans plafond), après « Lire la suite », les mots étaient mesurés sans poids avant la barre d'actions, qui tombait à 0 px. Vérifié : les mots gardent `weight(1f, fill = false)` (ou `weight(1f)` quand la hauteur est tenue), donc la colonne mesure d'abord la tête et `ActionBar`, et `WordsRoom` borne sa place (`cap = minOf(c.maxHeight, …)`) à ce qui reste ; « Lire la suite » lève toujours `panelCap`. Debout, `reading` borne la colonne (`heightIn(max = reach)`) et le délestage (`shed`, 3 au plus) rend la consigne entière. Couché, `MoreWords` reste et page la suite. Les boucles possibles sont bornées (`wholeWords` monotone, `regrown` ≤ 2, `shed`/`whyShed` ≤ 3). Le test `Tour9L13TutoReadAllTest` mesure de vrais bords visibles (`boundsInRoot`, coupé par le défilement) et exige une hauteur > 0 : il vérifie bien quelque chose, dans les deux sens.

Aucun autre bloquant ni grave trouvé (pas de plantage Compose, pas de boucle de recomposition, placement `placeRelative` correct en RTL, 200 % couvert par les tests du lot).

## Notes (mineurs)

- Couché après « Lire la suite », le test ne vérifie que le bouton « suivant », pas qu'il reste au moins une ligne de consigne visible (à 200 % sur 360 dp, la tête et la barre d'actions peuvent tout prendre ; comportement d'avant le lot, pas une régression).
- `userFolded` n'est remis à zéro que par `start()` et `toggleOpen()` : une ouverture automatique (`softSaid`) ou une reprise d'un autre cours par `show()` garde le repli choisi auparavant.

Verdict : 0 bloquant, 0 grave
