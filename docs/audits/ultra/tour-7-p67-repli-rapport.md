# Tour 7, p67-repli : rapport de zone (bloc 0b de P6/P7, 2026-11-18)
code de départ : 3cd69a96 ; branche eq/tour-7-p67-repli.
Fait : repli du panneau branché. Les exemples (Mode simple) se replient en premier quand la consigne défile ; la carte du kit et « ▸ Pourquoi ? » restent épinglés ; coupée malgré tout, la consigne garde le kit et « Pourquoi ? » avec « Lire la suite ». Le défilement est remesuré après le repli (« Réduire » ramène bien « Lire la suite »).
Preuves : rouge 428957c2 (TutoReadabilityRuleTest#cutAnywayTheKitAndWhyStay, Tour7TutoPanelFoldTest rougissent sur le code de départ) ; vert 80a2b698. Retirer le `SideEffect` de repli ou remettre l'ancienne fin de `layout` les refait rougir.
Batterie ciblée : 82 tests, 0 échec (Tour6TutoPanel*, Tour7Tuto*, TutoKit*, TutoReadability*, TutoOneMomentShownTest, TutoLineHeightByScriptTest, *PlainWords*). Aucune capture commitée.
Machine : le SDK Android a été installé dans le conteneur (/opt/android-sdk, local.properties non commité) pour jouer Gradle.
CI 353 (rouge) : Tour6TutoReadAllTest échouait (72 px de la consigne sur 108 après « Lire la suite »). Cause mesurée : la carte du kit, chargée en fond, arrive ou non avant la mesure ; quand elle est là, le code de départ 3cd69a96 échoue pareil. Rouge cd611c7c (Tour7TutoReadAllKitTest attend la carte du kit, rouge sur 516f2515), vert a27e0224 (sous « Lire la suite », les mots gardent leur hauteur entière). Coquille2SeatTest : vert ici sur 516f2515 (11 tests, 0 échec) ; son chemin (rouvrir le projet du Live sur le pad Live) ne passe pas par le panneau du tuto.
Décisions : docs/decisions/tour-7-p67-repli.md.
tests: :app:testDebugUnitTest --tests 'com.pocketstudio.app.tuto.Tour6TutoPanel*' --tests 'com.pocketstudio.app.tuto.Tour7Tuto*' --tests 'com.pocketstudio.app.tuto.TutoKit*' --tests 'com.pocketstudio.app.tuto.TutoReadability*' --tests 'com.pocketstudio.app.tuto.TutoOneMomentShownTest' --tests 'com.pocketstudio.app.tuto.TutoLineHeightByScriptTest' --tests 'com.pocketstudio.app.library.*PlainWords*' --tests 'com.pocketstudio.app.tuto.Tour6TutoReadAllTest'
chemin: panneau du tuto, petit téléphone, Facile, exemples repliés avant de couper | test: Tour7TutoPanelFoldTest#theBlocksFoldInOrderTheKitStaysAndWhyIsTouchable
chemin: panneau du tuto, mots coupés malgré tout : kit et « Pourquoi ? » gardés | test: TutoReadabilityRuleTest#cutAnywayTheKitAndWhyStay
chemin: panneau du tuto, ordre de repli des blocs | test: TutoReadabilityRuleTest#theBlocksFoldInOrderBeforeTheWordsAreCut
chemin: panneau du tuto, carte du kit sur un cours commencé | test: TutoKitTest#onACourseUnderWayThePackIsOfferedNotPutInSilence
chemin: panneau du tuto, « Pourquoi ? » en arabe à 130 % | test: Tour6TutoPanelFitWhyTest#whyIsNeverCutWithTheInterfaceAt130PercentRightToLeft
chemin: panneau du tuto, « Réduire » puis « Lire la suite » | test: Tour7TutoReduceTest#reduceGivesTheCellsTheirRoomBack
chemin: panneau du tuto, « Lire la suite » avec la carte du kit épinglée | test: Tour7TutoReadAllKitTest#readOnShowsTheWholeInstructionInOneTouch
