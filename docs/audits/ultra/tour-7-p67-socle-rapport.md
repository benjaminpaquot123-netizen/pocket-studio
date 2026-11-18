# Tour 7, p67-socle : rapport de zone (bloc 0 de P6/P7, 2026-11-17)
code de départ : d558344d (ccr-80af1a1b-mn0wec) ; branche eq/tour-7-p67-socle.
Fait : règle de lisibilité hors Compose (TutoReadability : budget, lignes, layout qui replie exemples, kit, « Pourquoi ? » avant de couper) ; compteur de moments (TutoMoments) et refus de TutoValidation avec liste d'exceptions ; zone i18n tuto_moments (14 langues) ; panneau : en Facile, la phrase du moment « À toi : touche « … ». » seule tant qu'un bouton de préparation est attendu ; hauteur de ligne mesurée sur un caractère de l'écriture (あ, 中, 한, अ, ع) ; étape de partage scindée en gardant les identifiants Facile.
Choix : le panneau ne replie pas encore les blocs (branché, il cachait la carte du kit et laissait « Pourquoi ? » sous « Lire la suite » en arabe à 130 %) ; la règle reste testée seule. Détail : docs/decisions/tour-7-p67-socle.md.
Laissé aux blocs 1 à 4 : étapes de partage techno et house, tonalité et niveaux des familles ; vider les exceptions (Facile figé à 3077 cas, Pro à 3725 et qui ne peut que baisser).
Batterie : project-model 224/0, app 251/0 ; fichiers PS_TUTO_FAULTS vides ; captures tuto_* refaites, deux changées (tuto_step_need_track, tuto_step_need_bars), regardées en entier.
tests: :project-model:test --tests 'com.pocketstudio.model.tuto.*' --tests 'com.pocketstudio.model.i18n.I18nCatalogTest' --tests 'com.pocketstudio.model.i18n.KoreanRegisterTest' --tests 'com.pocketstudio.model.i18n.Tour7LanguagesTest' :app:testDebugUnitTest --tests 'com.pocketstudio.app.tuto.*' --tests '*Tour6TutoPanel*' --tests '*Tour7TutoPanel*'
chemin: panneau du tuto, Facile, bouton de préparation attendu | test: TutoOneMomentShownTest#facileShowsTheNeedAloneAndProKeepsItsText
chemin: panneau du tuto, ja / zh-CN / ko / hi / ar à 100 et 200 % | test: TutoLineHeightByScriptTest#theLineHeightsAreAtLeastAWitnessLine
chemin: panneau du tuto, chaque consigne Facile dans son état | test: TutoReadabilityBudgetTest#everyFacileConsigneFitsOrIsAKnownException
chemin: panneau du tuto, consignes Pro | test: TutoReadabilityBudgetTest#proOutOfBudgetOnlyGoesDown
chemin: panneau du tuto, mots coupés avec « Lire la suite » | test: TutoReadabilityRuleTest#aLineOfTheInstructionIsAlwaysInSightAndCutMeansReadOn
chemin: panneau du tuto, ordre de repli des blocs | test: TutoReadabilityRuleTest#theBlocksFoldInOrderBeforeTheWordsAreCut
chemin: panneau du tuto, carte du kit sur un cours commencé | test: TutoKitTest#onACourseUnderWayThePackIsOfferedNotPutInSilence
chemin: panneau du tuto, « Pourquoi ? » en arabe à 130 % | test: Tour6TutoPanelFitWhyTest#whyIsNeverCutWithTheInterfaceAt130PercentRightToLeft
