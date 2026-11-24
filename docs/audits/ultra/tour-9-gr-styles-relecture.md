# Relecture neuve, lot tour-9 gr-styles
relu : 4438394f1d2be3ee974747b4b723eb5cd28b506c

Portée : `git diff d9910076..4438394f` (14 commits du lot), hors `StyleRecipes.kt` et `separation/`. Aucun fichier du moteur C++ touché (thread audio intact).

Vérifié sans défaut :
- Traductions : `model.json` et `guide.json` ont les mêmes 888 et 669 clés dans les 14 langues, aucune vide ; les 2 textes acid retirés (707, « couine ») ont disparu partout.
- Pistes : `Project.tracks` ne compte que les pistes ajoutées (ids 1 à 7) ; `free(n) = 7 - taille - n` est juste. House : basse, piano, lead, doublure, montée, voix, sub drop = 7 pistes, 1 libre. Amapiano : 7 avec la texture, 1 libre. Disco : texture gardée. `mood!!` (l. 828) est sûr, `mood != null` est testé juste avant.
- `make_guides.py` : la voix objet `{type, sample}` est lue comme son type ; c'est le seul endroit du générateur qui lit `vocal`.
- Doublure : `pitch - 12 * floorDiv(max(0, pitch - 85), 12)` descend de ceil((p-96)/12) octaves : 97 → 85, 108 → 96, 109 → 85, 96 reste. Rien ne se perd (l'ancien filtre `<= 108` supprimait des notes).
- Acid house, EQ `[0.826, 0.375, 0.2416, 0]` : 20·1000^0,826 ≈ 6,0 kHz ; (0,375-0,5)·36 = -4,5 dB ; Q = 0,3·(10/0,3)^0,2416 ≈ 0,70 ; param 3 = 0, donc pas de coupe-bas. RoleMix ajoute seulement son coupe-bas à cette EQ (param 3), la cloche reste.
- Rouges : le test acid (`HouseStylesTest.theAcidHouseDoesNotHurtTheEars`) échoue sur l'ancien code (résonance 0,92 > 0,75, pas d'EQ). Les 6 autres « rouge: » n'ajoutent pas de test : ils notent dans `tour-9-gr-styles-rouges.md` des tests déjà rouges après les fusions (SimpleMelodyTest, GuideParcoursTest, ClubTransitionsTest, HonestMixTest, MusicTheoryTest, ModelTextsTest, I18nCatalogTest). C'est un rouge mesuré valable. `StyleBodyTest` est durci (96 au lieu de 108). Le changement trap → grime de `GuideActionsTest` garde le sens du test (une copie de chaîne vers le contre-chant).

Points relevés :
- mineur — `project-model/src/main/kotlin/com/pocketstudio/model/StarterWizard.kt:829` : la branche `mine == 0` de `room()` est morte. `room()` ne garde que la doublure et la texture, et ces deux pistes ne viennent que si `mine == 1`. Elle garde aussi l'ancien décalage d'une piste (`8 - taille > later` laisse prendre la dernière place d'une piste « later »). Sans effet aujourd'hui, mais trompeur à la lecture.
- mineur — `StarterWizard.kt:826` : avec `mine == 0`, le contre-chant entre toujours sans vérifier la place des pistes « later ». Le défaut existait avant le lot, il est noté dans le rapport (jump_up remplit ses 7 pistes). La règle « une piste pour ma mélodie » ne vaut donc que pour les styles qui ont du corps.
- mineur — `project-model/src/test/kotlin/com/pocketstudio/model/SimpleMelodyTest.kt:15` : la place libre pour « ma mélodie » n'est prouvée que sur la house. Aucun test ne parcourt tous les styles avec du corps (deep_house, amapiano, disco, styles hip-hop `body: true`). Un test sur tous les styles (`doublure ou texture ⇒ tracks.size <= 6`) empêcherait un retour en arrière.
- mineur — `StarterWizard.kt:826-830` : deep_house perd sa texture « Nappe chaude », pourtant nommée dans sa recette. Le calcul donne `later = 2` : le contre-chant et la doublure entrent, puis `free(1) = 2 < 3`. C'est voulu (la texture cède la première, `docs/decisions/tour-9-gr-styles.md`), mais aucun test ni texte ne le dit pour ce style. Une texture écrite exprès dans une recette disparaît sans bruit.
- mineur — `StarterWizard.kt:1541` : `NO_DROP = setOf("lofi", "boom_bap")` recopie à la main `WizardMix.NO_CLIP` (`WizardMix.kt:318`). Si un style hip-hop sans Drop s'ajoute à l'un des deux et pas à l'autre, le montage et le clipper divergent de nouveau. Il vaudrait mieux une seule liste, ou la déduire de la forme du morceau.
- mineur — `scripts/guides/make_guides.py:180-181` : le générateur lit `counter` et `riser` dans la recette brute. Il ignore donc `body` (qui ajoute contre-chant et montée) et la nouvelle règle de place. La cohérence carte « contre-chant » ⇔ piste « Contre-chant » ne tient que parce que les recettes house ont été retouchées à la main. Aucun test ne compare les cartes du guide aux pistes du morceau construit (le défaut speed_garage du rouge n'a été vu qu'au nombre de cartes).
- mineur — `docs/audits/ultra/tour-9-gr-styles-rapport.md:10` : le rapport dit « −4 dB » ; la valeur réelle est -4,5 dB (0,375 → -4,5). Ce n'est qu'un écart de texte.

Verdict : 0 bloquant, 0 grave
