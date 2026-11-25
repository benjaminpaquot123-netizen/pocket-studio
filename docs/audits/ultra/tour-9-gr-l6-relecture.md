# Tour 9, zone « gr-l6 » : relecture neuve
Relu : 15f64845f243a51d45053da44fc377c4c8f061eb

Diff relu : `git diff 8b905fe0..HEAD` hors `docs/audits/RECHERCHE-TRAP-VOIX.md` (4452f0b4 « rouge: », 098744c1 « vert: », 15f64845 rapport et décisions).

## Ce qui a été vérifié (et tient)

1. **Source VSCO-2 (6dd651d5, FluteSusVib.sfz)** : il y a bien une région `LDFlute_susvib_C3_v1_1.wav`, lokey 60, hikey 61, pitch_keycenter 60 (le nom « C3 » est la convention Yamaha, la note est bien le do 60). Centres de la flûte : 60, 64, 69, 72 (2 couches), 76 (2), 81, 84, 88 (2), 93, 96. Les fichiers durent 4,8 s (C6) à 10,6 s (E3), 7 à 8 s pour la plupart : la coupe à 6 s / 4,5 s n'est pas mangée par la fin naturelle.
2. **instrument_pack.py** : `reduce_part` garde les 10 centres de [60, 100] ; la première zone de la flûte part à 60 (bord de la partie). Clarinette [52, 59] : centres gardés 53 et 58, zones 52-55 et 56-59. Basson jusqu'à 51. Aucun trou ni chevauchement 51/52 et 59/60. `seconds_for` reçoit bien la courbe propre à la partie (`p.get('seconds', inst['seconds'])`, ligne 481) : 6,0 s à 60, 4,65 s au centre 96. Basson et clarinette gardent 3 s.
3. **Budget 48 Mo** : estimation large (tout en stéréo 48 kHz 16 bits, 192 ko/s) : flûte 13 zones × ~5,4 s ≈ 70 s, basson 12 × 3 s, clarinette 4 × 3 s, soit ~118 s ≈ 23 Mo. Sous 48 Mo et même sous l'ancien 32 Mo : la boucle de réduction ne se déclenche pas, la flûte garde ≥ 4,5 s. Le passage à 48 Mo est donc sans effet (plafond seulement), sans danger.
4. **StyleRecipes.kt** : régénéré dans un fichier temporaire avec `recipes_to_kotlin.py` : identique au fichier commité.
5. **Tests** : `scripts/packs/tests/run.sh` : 41 tests, OK. Les tests neufs joués sur le code de 8b905fe0 (archive, tests de HEAD copiés) : les 3 tests TrapFlute neufs et `test_index_lists_the_recipe_sources_actually_built` échouent (4 échecs ; les 2 erreurs restantes sont les tests « pas d'audio dans git », hors dépôt). Les tests prouvent donc le changement. Kotlin ciblés (`HipHopRecipesTest`, `RecipeFormatV2Test`, `StyleBodyTest`, `FirstListenTest`) : verts.
6. **Aucune dépendance aux anciennes plages** : `recipeParts` n'est lu par aucun code Kotlin (seul `build_index.py` l'écrit) ; l'appli charge l'instrument « Bois » entier. Seul `RecipeFormatV2Test` contient `[67, 100]`, pour un instrument factice `instrument_test` : sans lien.
7. **Notes réellement jouées par l'appli** (MelodyBuilder.lead lancé sur le jar de project-model, toutes humeurs, toutes versions) : Sombre / Futur / Joyeux 77 à 89, Rêveur 76 à 91 ; la version « Refrain 2 » double à l'octave du dessous et descend à 65 (64 en Rêveur). Tout est dans [60, 100] et dans la tessiture d'une flûte (do 4 à do 7). Avant le lot, ces notes 64-66 du Refrain 2 tombaient sur la clarinette : le défaut était réel et le lot le corrige.
8. **Phrase** : sombre (mineur) fa 6, mi♭, do, la♭, silence, do, la♭, sol, fa 5 : ligne liée qui descend vers la tonique, cohérente ; en majeur, mêmes degrés sur l'accord de la mesure, sans note hors gamme. Originale (pas de copie de « Mask Off »).
9. **Licences** : aucune source neuve (libraries.json, THIRD_PARTY.md inchangés), VSCO-2 CE déjà créditée ; aucun audio dans git.

## Bloquant

Aucun.

## Grave

Aucun.

## Mineur

1. **Le test se trompe d'octave.** `lead_notes()` calcule la tonique à `12 * octave + root` = 65, alors que l'appli joue `12 * (leadOctave + 1) + root` = 77 (MelodyBuilder.chorus). Le test passe pour une raison voisine de la vraie : les notes basses réelles viennent du doublage à l'octave du Refrain 2 (65, et 64 en Rêveur, que le modèle Python ne voit pas). Il protège bien la plage [60, 100] aujourd'hui, mais si quelqu'un relevait plus tard le bas de la flûte à 65, le 64 du Rêveur passerait inaperçu. À corriger un jour : modèle d'octave aligné sur Kotlin et doublage du Refrain 2 compté.
2. **Le niveau de tout l'instrument « Bois » monte d'environ 3,5 dB.** La note de référence du gain (`ref`, zone la plus proche du do 60) passe de la clarinette la♯ 2 (v3, ~-25,3 dB RMS sur sa première seconde, volume SFZ compris) à la flûte do 4 (~-28,8 dB). `gainDb` monte donc d'environ 3,5 dB pour basson, clarinette et flûte, dans toutes les utilisations de « Bois ». Pour la flûte trap (échantillons la 4 et do 5, 12 à 18 dB plus forts que le do 4), le niveau à vélocité 100 avant fader passe d'environ -11,6 / -5,2 dBFS RMS à -8,1 / -1,7 dBFS RMS. Le rapport dit déjà que le niveau flûte contre 808 n'est pas mesuré ; à vérifier au premier pack construit par la CI (ligne « gain » du journal d'`instrument_pack.py`) ou sur le téléphone, et au besoin poser `target_rms_db` sur l'instrument.
3. **« Deux silences »** (rapport, décisions, notes de recette) : la phrase n'a qu'un silence, de deux pas (`..`). Le test vérifie `count('.') >= 2`, ce qui compte deux pas, pas deux silences. Texte à ajuster, sans effet sur le son.
4. Le budget 48 Mo n'est pas nécessaire (l'estimation tient sous 32 Mo) ; la décision le justifie par prudence, c'est acceptable.

Verdict : 0 bloquant, 0 grave
