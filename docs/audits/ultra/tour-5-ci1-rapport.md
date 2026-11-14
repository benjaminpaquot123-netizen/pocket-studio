# Tour 5, lot « ci1 » : la CI rouge du paquet de fusions (passage 43137620831)

Atelier : `/home/user/eq-tour-5-ci1`, branche `eq/tour-5-ci1`, partie de l'intégration à jour (510b4920, tous les lots de l'étape 4 dont « plus »). Décisions : `docs/decisions/tour-5-ci1.md`. Rien de poussé.

| Rouge de la CI | Cause trouvée | Correctif | Preuve |
|---|---|---|---|
| Fumée sur émulateur : `JourneyTest#aFirstSongWithEveryTool`, « nothing tagged mode_card_pro after 15000 ms » (Journey.kt:152, 3e lancement, « Nouveau morceau » › assistant) | Réponse 3A (lot « plus ») : « Simple ou Pro ? » n'est plus posé une fois un mode choisi sur le téléphone ; le parcours partagé l'attendait toujours | `Journey.newSongInAssistant` : carte touchée si l'étape est là ; sinon l'assistant doit dire son mode (`wizard_mode`) et « Changer » remet le mode voulu ; le mode final est vérifié dans les deux cas | rouge 5fc583f6 (les deux nouveaux tests Robolectric échouent avec le message exact de la CI), vert 72abdd53 ; `JourneyPathsTest` 11/11 ; `compileDebugAndroidTestKotlin` vert |
| `UnusedStarterTest#anUntouchedFirstProjectGoes` (63 ms, le premier projet encore là) | Pas une instabilité : `ProjectsViewModel` lançait chaque opération dans sa coroutine, qui prenait ensuite le verrou ; la liste demandée juste après le premier travail pouvait passer devant et dire « chargée » avant l'effacement | Une file lue par un seul travailleur : les opérations tournent dans l'ordre demandé | rouge 97b1df2c (rouge au 13e tour sur 200, et le test d'origine rouge dans la même suite), vert dd89cd10 ; avant le correctif, le test d'origine était vert 3 fois seul et 3 fois dans sa classe ici, la course ne se montrant que sous charge |
| Avertissement : 18 captures diffèrent | 16 références que les lots ont changées sans les réenregistrer (pastille « ↓ n autres » qui ne couvre plus de carte, nom du projet retiré à 130 % en Mode simple, famille ouverte plus lisible, bouton du Remix) ; 2 « ajoutées » (`share_overrun_*`) écrites dans `app/build/`, hors du dépôt | Les 16 réenregistrées (c88d77ed), regardées une à une avant / après ; aucune des cinq interdites touchée | `recordRoborazziDebug` sur les classes concernées : exactement ces 16 fichiers changent |

Non rejoué ici : la fumée sur émulateur elle-même (pas d'émulateur) et la suite complète de 33 minutes.

tests: com.pocketstudio.app.journey.JourneyPathsTest com.pocketstudio.app.projects.UnusedStarterTest com.pocketstudio.app.nav.GuideJourneyTest com.pocketstudio.app.rhythm.KickPanJourneyTest com.pocketstudio.app.projects.ProjectsSafetyTest com.pocketstudio.app.projects.ProjectsTrashTest com.pocketstudio.app.projects.ZeroProjectsTest com.pocketstudio.app.nav.LaunchFlowTest com.pocketstudio.app.nav.ModeChoiceTest com.pocketstudio.app.nav.PlusSameInBothModesTest

## Liste des chemins

- chemin: « Que veux-tu faire ? » › Nouveau morceau › assistant, mode déjà choisi (pas de question) | test: JourneyPathsTest#aLaterStartWithAModeAlreadyChosenSkipsTheModeStep
- chemin: « Que veux-tu faire ? » › Nouveau morceau › assistant › « Changer » (dernier choix différent du mode affiché) | test: JourneyPathsTest#aLaterStartWithAnotherLastModeChangesItInTheAssistant
- chemin: « Que veux-tu faire ? » › Nouveau morceau › assistant, premier choix de mode | test: JourneyPathsTest#aLaterStartMakesANewSongInTheAssistant
- chemin: Mes projets, à l'ouverture (premier projet inutilisé effacé avant la liste) | test: UnusedStarterTest#anUntouchedFirstProjectGoesBeforeTheListIsShown
- chemin: Mes projets, à l'ouverture (premier projet inutilisé) | test: UnusedStarterTest#anUntouchedFirstProjectGoes
