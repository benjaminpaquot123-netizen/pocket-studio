# Relecture neuve, lot tour-9 gr-styles
relu : a2e8a831b (republication ; la relecture neuve a porté sur 8e6daff2511bffbc2a8625dd4a30670c55fead33)

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

## Relecture de la fusion avec l'intégration (2026-11-24)

Portée : les 3 commits sur 042775a8 : la fusion fa7554d0 (avec 0ed59078, base 9f0c5eb8), la régénération 00e3904c de `StyleRecipes.kt`, et 64daaf8e (ligne gr-l1b déplacée).

Vérifié sans défaut :
- Rien ne se perd. Chaque fichier changé par le lot a dans la fusion la version du lot (sauf `docs/DECISIONS.md`, voir plus bas).
- Côté intégration, 53 fichiers changés depuis la base. 30 sont repris tels quels, dont `engine/bench/ps_measure.cpp`, les tests de `scripts/packs/tests/` et `build_index.py`. Pour `instruments.json`, `libraries.json` et `WizardMix.kt`, le patch de l'intégration se retrouve en entier dans la fusion.
- Les 19 fichiers en conflit (StarterWizard.kt, les 14 `i18n/*/model.json`, RecipeFormatV2Test.kt, StarterWizardTest.kt, StyleBodyTest.kt, recipes_to_kotlin.py) : la version de l'intégration est octet pour octet celle de b0ac8fe3, un ancêtre du lot. Le lot les a ensuite fait évoluer. Garder le côté lot est donc juste.
- `docs/DECISIONS.md` : toutes les lignes ajoutées par l'une ou l'autre branche sont dans la fusion ; aucune ligne retirée.
- `StyleRecipes.kt` : le générateur relancé ne change rien. `make_guides.py` relancé ne change rien. `scripts/packs/tests/run.sh` : OK (code 0).
- La ligne gr-l1b déplacée vers `docs/decisions/tour-9-gr-styles.md` est intacte, mot pour mot. L'intégration ne l'avait pas : rien ne disparaît d'elle.
- `engine/src` est identique à l'intégration : thread audio intact.

Points relevés :
- mineur — `project-model/src/main/kotlin/com/pocketstudio/model/StyleRecipes.kt` : à la tête du lot 042775a8, ce fichier ne suivait pas les recettes, et `StyleRecipesUpToDateTest` y aurait échoué. C'est réglé par 00e3904c. Tant que ce commit n'est pas là, la tête du lot n'est pas verte.

## Relecture de la fusion 5293b39ad et du rejeu t4-26

Portée : 4c556e3e (fusion de origin/ccr-80af1a1b-mn0wec, tête 5293b39a, base 0ed59078) et 786153d5 (rejeu t4-26).

- La fusion n'apporte que les 19 fichiers changés par l'intégration depuis la base (scripts du banc `fusion-equipes.sh`, `porte-paquet.sh`, `test_porte_paquet.py`, `TutoKickSoundTest.kt`, `docs/DECISIONS.md`, traces du portail, rapports et relectures kick-ci et porte-paquet). Chacun est octet pour octet celui de 5293b39a, et le lot n'avait touché aucun d'eux : pas de conflit, rien de perdu.
- Les fichiers du lot (StarterWizard.kt, recettes, StyleRecipes.kt, i18n) ne changent pas dans la fusion. Le générateur relancé sur `scripts/packs/recipes/*.json` redonne `StyleRecipes.kt` à l'identique.
- Rejeu t4-26 : le lo-fi passe de 4 à 5 lignes de batterie (`Role.Snap` ajouté dans `hiphop.json` et dans `StyleRecipes.kt`). `StarterWizard.steps()` fait une étape par ligne (`style.lines.map { Step.Drum(it) }`), donc `lastStep` grandit de 1. `wizard_create` n'apparaît que quand `step >= lastStep` (WizardScreen.kt:254) : il faut bien un `wizard_next` de plus avant lui.
- Le rejeu est cohérent : `cmd-1` à `cmd-34` sans trou, l'ajout (`touche wizard_next` + `attends 1`) est en 21-22, les anciennes 21-32 deviennent 23-34 sans autre changement, `oracle.txt` dit `rejeu.n=34`. Les `cmd-*.txt` n'ont pas de saut de ligne final et `oracle.txt`/`start.txt` en ont un, comme avant.

## Relecture de la fusion d87dea73f (robot1b)

Portée : 8e6daff2 (fusion de origin/eq/ci-int-robot1b, tête d87dea73f, sur 8167ff96).

- La fusion n'apporte que 28 fichiers : le banc BenTest (`app/src/benTest/`, ses tests), `scripts/bentest/check-apk.sh`, `docs/PLAN-TEST-APK.md`, `docs/DECISIONS.md`, traces du portail, rapports et relectures robot1 et robot1b. Chacun est octet pour octet celui de d87dea73f (`git diff origin/eq/ci-int-robot1b 8e6daff2` vide sur ces fichiers).
- Aucun fichier du lot ne change dans la fusion (ni `project-model/`, ni `scripts/packs/`, ni i18n, ni le rejeu t4-26). Le générateur relancé sur `scripts/packs/recipes/*.json` redonne `StyleRecipes.kt` à l'identique.
- `engine/src` n'est pas touché : thread audio intact.

rejeu changé : t4-26-prise-clavier : le lot ajoute une page à l'assistant (5e ligne de batterie « snap » du lofi), deux pas de plus ; accord de Benjamin (1A)

Republication (chef, 2026-11-25) : lot republié en un seul commit (10357b21f, arbre identique à 8b905fe0c) sous le nom gr-styles2 ; depuis 8e6daff25, seuls les fichiers du lot dans docs/ ont changé de nom, aucun code.

Verdict : 0 bloquant, 0 grave
