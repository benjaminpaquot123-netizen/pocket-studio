# Tour 7, p67-repli : rapport de zone (bloc 0b de P6/P7, 2026-11-18)
code de départ : 3cd69a96 ; branche eq/tour-7-p67-repli.
Fait : repli du panneau branché. Les exemples (Mode simple) se replient en premier quand la consigne défile ; la carte du kit et « ▸ Pourquoi ? » restent épinglés ; coupée malgré tout, la consigne garde le kit et « Pourquoi ? » avec « Lire la suite ». Le défilement est remesuré après le repli (« Réduire » ramène bien « Lire la suite »).
Preuves : rouge 428957c2 (TutoReadabilityRuleTest#cutAnywayTheKitAndWhyStay, Tour7TutoPanelFoldTest rougissent sur le code de départ) ; vert 80a2b698. Retirer le `SideEffect` de repli ou remettre l'ancienne fin de `layout` les refait rougir.
Batterie ciblée : 82 tests, 0 échec (Tour6TutoPanel*, Tour7Tuto*, TutoKit*, TutoReadability*, TutoOneMomentShownTest, TutoLineHeightByScriptTest, *PlainWords*). Aucune capture commitée.
Machine : le SDK Android a été installé dans le conteneur (/opt/android-sdk, local.properties non commité) pour jouer Gradle.
Décisions : docs/decisions/tour-7-p67-repli.md.
tests: :app:testDebugUnitTest --tests 'com.pocketstudio.app.tuto.Tour6TutoPanel*' --tests 'com.pocketstudio.app.tuto.Tour7Tuto*' --tests 'com.pocketstudio.app.tuto.TutoKit*' --tests 'com.pocketstudio.app.tuto.TutoReadability*' --tests 'com.pocketstudio.app.tuto.TutoOneMomentShownTest' --tests 'com.pocketstudio.app.tuto.TutoLineHeightByScriptTest' --tests 'com.pocketstudio.app.library.*PlainWords*'
chemin: panneau du tuto, petit téléphone, Facile, exemples repliés avant de couper | test: Tour7TutoPanelFoldTest#theBlocksFoldInOrderTheKitStaysAndWhyIsTouchable
chemin: panneau du tuto, mots coupés malgré tout : kit et « Pourquoi ? » gardés | test: TutoReadabilityRuleTest#cutAnywayTheKitAndWhyStay
chemin: panneau du tuto, ordre de repli des blocs | test: TutoReadabilityRuleTest#theBlocksFoldInOrderBeforeTheWordsAreCut
chemin: panneau du tuto, carte du kit sur un cours commencé | test: TutoKitTest#onACourseUnderWayThePackIsOfferedNotPutInSilence
chemin: panneau du tuto, « Pourquoi ? » en arabe à 130 % | test: Tour6TutoPanelFitWhyTest#whyIsNeverCutWithTheInterfaceAt130PercentRightToLeft
chemin: panneau du tuto, « Réduire » puis « Lire la suite » | test: Tour7TutoReduceTest#reduceGivesTheCellsTheirRoomBack
