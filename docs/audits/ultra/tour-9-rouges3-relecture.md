# Tour 9, lot rouges3 : relecture neuve

Relu : 072a77377 (base intégration f6d0a30b4)

## Lu
- `git diff f6d0a30b4..072a77377` sur `app/` et `ui-components/` : `TutoPanel.kt`, `Spotlight.kt`, `Windows.kt`.
- Aucun fichier de test modifié (ni affaibli, ni sauté).

## Points vérifiés
- `ScaledPopup` (nouvelle surcharge avec `popupPositionProvider`) : pas d'ambiguïté avec l'ancienne (paramètre obligatoire différent), passe par `RescaledWindow` comme les autres ; le toast « coché » du tuto l'utilise.
- `headShed` (TutoPanel) : état remis à zéro par étape / ouverture / niveau / « Lire la suite » ; borné à 2 ; ne change pas la hauteur tenue (`held`), donc pas de boucle de recomposition. N'agit que si les mots n'ont pas leur ligne et demie : aux tailles normales en portrait, rien ne bouge. Le titre passe à 2 lignes via `WholeWordsText` (réduit, jamais coupé).
- Flèche de repli et « Chapitres » : intacts (même rangée, mêmes libellés d'accessibilité).
- Avec l'offre du kit : l'ancien chemin (`m * 3 > least` → `kitShed`) est conservé ; le nouveau cas « quelques px manquants » cède d'abord la ligne du chapitre puis le titre.
- `reveal(near)` (Spotlight) : seulement à très gros texte ; `above` garde l'élément le plus haut, `near` le plus proche ; défaut `near = above` pour les autres appels ; aucun chemin de plantage neuf (`runCatching` conservé).
- Tests ciblés lancés : ScaledWindowsTest, TutoPanelFitsBigFontTest, T9PanelTargetTest, Tour9L13TutoNeverFullScreenTest, TutoBigFontTest, TutoKitUnderFinger* : 27 tests, 0 échec, 0 sauté.

## Remarques mineures
- `kitShort` est maintenant appelé aussi sans offre du kit : son nom ne dit plus tout ; à renommer un jour (`headShort` / `roomShort`).
- Téléphone couché à 100 % : si les mots manquent de place, la ligne « Chapitre x/y · Étape » peut disparaître pour l'étape ; voulu (place aux mots), à surveiller en recette.
- Quand `kitShed` vaut déjà 1, la branche `else` peut encore monter `headShed` : sans effet visible (la tête est déjà cachée).

Verdict : 0 bloquant, 0 grave
