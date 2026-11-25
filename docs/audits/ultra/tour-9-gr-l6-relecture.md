# Tour 9, zone « gr-l6 » : relecture neuve
Relu : 55da1350

Diff relu : `git diff origin/eq/ci-int-styles HEAD` (4f3f19c2), après la fusion 540b9558 (conflits résolus côté lot) et 55da1350 (retrait des fichiers restants du lot gr-styles).

## Ce qui a été vérifié

1. **Rien de l'intégration n'est perdu.** Base commune d87dea73. Côté intégration, `instruments.json` n'apportait qu'un changement (release de « Bois » 0,4 → 0,2) : il est gardé. Pour `hiphop.json`, `StyleRecipes.kt` et `test_hiphop_recipes.py`, l'écart avec l'intégration ne contient que les lignes du lot : 2 morceaux dans `hiphop.json` (motif du lead trap, notes), 1 ligne dans `StyleRecipes.kt` (même motif), 57 lignes ajoutées et 0 retirée dans le test. Tous les autres styles et tests de l'intégration sont identiques.
2. **StyleRecipes.kt** : régénéré dans un fichier temporaire avec `recipes_to_kotlin.py` (37 styles) : identique au fichier commité.
3. **JSON** : `instruments.json` et `hiphop.json` se lisent sans erreur.
4. **Tests** : `pytest` sur `test_hiphop_recipes.py` et `test_recipe_sources.py` : 28 réussis. Les 3 tests TrapFlute neufs vérifient des valeurs précises (plage de la flûte contre les notes du motif, jointure clarinette/flûte sans trou ni chevauchement, durée tenue ≥ 4 s, version ≥ 2, motif neuf lié avec des silences) ; `test_recipe_sources.py` suit la plage [60, 100].
5. **55da1350** ne retire que 3 fichiers de docs du lot gr-styles, absents de l'intégration : sans effet.
6. Plus aucune trace de l'ancienne plage [67, 100] hors l'instrument factice de `RecipeFormatV2Test.kt:114` (sans lien).

## Bloquant

Aucun.

## Grave

Aucun.

## Moyen

Aucun.

## Mineur

1. **L'attaque 0,04 s de « Bois » n'est couverte par aucun test** (`scripts/packs/instruments.json:145`) : seules la plage, la durée et la version le sont. Un retour à 0,02 passerait inaperçu.

## Mineurs d'avant, toujours valables

1. **Le test se trompe d'octave** (`scripts/packs/tests/test_hiphop_recipes.py:24-38`) : `lead_notes()` place la tonique à `12 * octave` (65), l'appli à `12 * (leadOctave + 1)` (77) ; les notes basses réelles viennent du doublage à l'octave du Refrain 2 (65, 64 en Rêveur), que le modèle Python ne voit pas.
2. **Niveau de tout « Bois » relevé d'environ 3,5 dB** : la zone de référence du gain passe de la clarinette à la flûte do 4. À vérifier au premier pack construit par la CI ou sur le téléphone.
3. **« Deux silences »** dans le rapport et les décisions : le motif `O---7-5-----3-..5-----3-2-R-----` n'a qu'un silence de deux pas ; le test compte des pas (`count('.') >= 2`, ligne 221).
4. Le budget 48 Mo n'est pas nécessaire (l'estimation tient sous 32 Mo) ; acceptable par prudence.

Verdict : 0 bloquant, 0 grave
