# Tour 5, zone « retest » : « Retester les corrections » après un autre passage (2026-11-12)

Branche : `eq/tour-5-retest`. Décisions : `docs/decisions/tour-5-retest.md`.

## Le défaut vu sur le téléphone
Passage v6 (Honor, Android 17) : « Lancer l'essai éclair » réussi (7 sur 7), puis « Retester les corrections » avec 5 étapes cochées (Style Techno, Style Hard techno, Style Liquid DnB, Mémoire et images lentes, Écran éteint) : les 5 ratées en 15 s chacune (« nothing tagged launch », « top_projects is not there once »), alors que la note listait les étiquettes du studio.

## Cause
- Le retest sautait « Ouverture du studio », la seule étape qui met le studio devant : le robot partait de l'écran de BenStudio Test.
- Le robot lisait aussi les fenêtres cachées : le studio arrêté sous le banc gardait sa fenêtre et son dernier arbre (Compose ne le redessine plus), donc il était lu et touché sans effet.

## Correction
- L'ouverture du studio passe toujours en premier dans un retest, sans être proposée dans la liste ; l'en-tête du rapport compte les étapes cochées.
- Le robot ne lit et ne touche que les fenêtres affichées.
- Sans passage gardé par cette version, la liste coche d'après le dernier carnet enregistré, sinon le dernier zip de rapport ; sinon rien et « Coche les étapes à revoir. ».
- Toute la ligne d'une étape coche ou décoche.
- « Questions à l'oreille » après un autre passage : déjà correct, gardé par un test.

## Preuves
- Rouge (correction retirée) : le test Robolectric qui rejoue la séquence du téléphone donne les messages du téléphone (« nothing tagged launch », « top_projects is not there once »), les 5 étapes ratées.
- Vert : studio devant, chaque étape va jusqu'à « Créer maintenant » ; « Mémoire et images lentes » et « Écran éteint » réussies. Les 3 styles s'arrêtent ensuite à leur minute : sous Robolectric l'assistant ne sait faire que Jump up (« un son n'a pas pu être lu »).
- Non prouvé sur téléphone : à revoir au prochain passage de Benjamin.

## Chemins (endroits qui lisent ou montrent les mêmes données)
chemin: Banc › Retester les corrections › 5 étapes après l'essai éclair | test: RetestApresEclairTest#theRetestOfFiveStepsAfterTheEssaiEclairStartsFromTheStudio
chemin: Robot › studio arrêté sous le banc | test: RetestApresEclairTest#aStoppedStudioUnderTheBenchIsNotReadByTheRobot
chemin: Banc › Questions à l'oreille après un autre passage | test: RetestApresEclairTest#theQuestionsAfterTheEssaiEclairStartFromTheStudio
chemin: Banc › liste à cocher sans passage gardé | test: RetestApresEclairTest#withNoRunKeptTheChecklistTicksWhatTheLastLogbookFailed
chemin: Banc › liste à cocher vide puis lancement | test: RetestApresEclairTest#theChecklistWithNothingTickedSaysToTickAndClosesBeforeTheRun
chemin: Retest › sous-ensemble lancé | test: RetestTest#theRunnerRunsTheOpeningThenOnlyTheChosenSteps

tests: com.pocketstudio.bentest.*
