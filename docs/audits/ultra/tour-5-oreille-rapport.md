# Tour 5, lot « oreille » : T3, les questions à l'oreille de BenStudio Test (2026-11-12)

Plan : `docs/PLAN-TEST-APK.md`, « Niveau 3 » et étape T3. Branche : `eq/tour-5-oreille` (sur `eq/tour-5-testapk`). Décisions : `docs/decisions/tour-5-oreille.md`.

## Ce que fait le lot
- Quatrième bouton « Questions à l'oreille » : 8 questions en un passage. 1 à 4 « Ça tape ? » au haut-parleur (jump up, house, trap, techno) sur un morceau que le robot a fait et lancé ; 5 casque filaire et 6 Bluetooth (sautées si le téléphone n'en a pas) ; 7 jouer avec les doigts (le robot ouvre Live) ; 8 trois glissés depuis le bord gauche sur Sons › Batterie, lus par le robot seul.
- Bandeau flottant dans la fenêtre du studio (sans permission) : la question, ✅ et ❌ grands, « Passer », un mot facultatif. Un ❌ garde la capture des fenêtres, les dernières lignes du journal et les mesures.
- Réponses dans `rapport.md` (section « Questions à l'oreille ») et `mesures.json` (`questions`).
- Rien de la vraie appli n'est changé.

## Restes
- Non prouvé sur téléphone : le bandeau à l'écran (taille des boutons, clavier du mot), la détection du casque et du Bluetooth, le glissé du bord gauche sur le pad, le son lui-même.

## Chemins (endroits qui lisent ou montrent les mêmes données)
- chemin: BenStudio Test › questions à l'oreille, enchaînement | test: OreilleTest#theEightQuestionsAreAskedInOnePass
- chemin: BenStudio Test › question 5 sans casque | test: OreilleTest#withoutAWiredHeadsetQuestionFiveIsSkippedNotAsked
- chemin: BenStudio Test › question 6 sans Bluetooth connecté, bandeau « Prêt » | test: OreilleTest#withoutAConnectedBluetoothDeviceTheBannerAsksToConnectOneThenSkipsIfStillNone
- chemin: BenStudio Test › bandeau, un ❌ | test: OreilleTest#aNoKeepsItsPictureItsJournalAndItsMeasuresAndTheRunGoesOn
- chemin: BenStudio Test › bandeau, délai | test: OreilleTest#theBannerGivesUpAtItsTimeout
- chemin: BenStudio Test › question 8, gestes retour faits par Benjamin, pan avant/après | test: OreilleTest#aKickThatMovedIsARedWithItsPictureAndBothValues
- chemin: Rapport › questions à l'oreille | test: OreilleTest#theReportHasItsSectionAndTheMeasuresHoldTheAnswers

tests: com.pocketstudio.bentest.*
