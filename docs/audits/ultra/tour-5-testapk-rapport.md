# Tour 5, lot « testapk » : BenStudio Test, le robot dans l'appli (2026-11-11 au 2026-11-12)

Plan : `docs/PLAN-TEST-APK.md` (v2). Branche : `eq/tour-5-testapk`. Décisions : `docs/decisions/tour-5-testapk.md`.
Auto-audit de la boucle : `docs/audits/ultra/auto-audit-test-apk-2026-11-12.md`.

## Ce que fait le lot
- `-PbenTest` : une appli « BenStudio Test » (com.pocketstudio.test) installée à côté de la vraie, signée avec la clé de débogage locale, robot intégré (src/journey, src/benTest), carnet de bord, zip de rapport.
- Trois boutons : « Lancer l'essai éclair » (5 min), « Tout tester » (37 styles, exports relus, latence, Guide, images lentes, rotation, écran éteint, feux), « Retester les corrections » (étapes rouges ou grises du dernier passage, cochables).
- Feux audio sur le vrai pire bloc et le plus long écart du moteur (lot hardtechno), niveau en LUFS au lieu du plafond du limiteur.
- Robot : attend qu'une liste défilée s'arrête, refuse les touchers sous une autre fenêtre, ne touche jamais un bouton qui ouvre une autre appli, revient seul si l'appli passe derrière (étape notée « chronométrage faussé »), retouche « Choisir ce style » si le premier toucher est avalé.
- Un vrai défaut de l'appli corrigé : « Nouveau morceau impossible : Job was cancelled » journalisé à tort (ProjectsViewModel).

## Preuves
- Passages sur le Honor de Benjamin : essais éclair 1 à 3, « Tout tester » v4 et v5 (chiffres dans `docs/tests-telephone/`, jamais de capture ni de son).
- Tests JVM / Robolectric du robot (`com.pocketstudio.bentest.*`) verts au dernier commit.

## Restes
- Non prouvé sur téléphone : la reprise après une sortie de l'appli, le nouveau toucher de Liquid DnB, l'écran de « Retester les corrections ».
- T2 (contrôle de mise en page) réduit, T3 (questions à l'oreille) en un passage, T1b (micro, empreinte) reporté.

## Chemins (endroits qui lisent ou montrent les mêmes données)
- chemin: Robot › toucher d'une liste défilée | test: JourneyDriverTest#aTapWaitsForOneCopyThenScrollsAndTouches
- chemin: Robot › toucher dans toutes les fenêtres | test: InAppDriverTest#realTouchesInEveryWindowAndListsScrolledToTheirTarget
- chemin: Robot › bouton qui ouvre une autre appli | test: InAppDriverTest#aControlThatOpensAnotherAppIsNotTouched
- chemin: Guide › carte « morceau de référence » | test: SortieDeLAppliTest#theGuideSkipsTheCardThatOpensTheFilePickerAndSaysSo
- chemin: Premier lancement › premier morceau | test: PremiereInstallationTest#aFirstInstallReachesTheFirstSongsCreation
- chemin: Mes projets › nouveau morceau ouvert | test: NewSongOpenTest#theNewSongOpeningIsNotLoggedAsAFailure
- chemin: Tout tester › écoute seconde par seconde | test: ToutTesterTest#aSteadySongIsHeardSecondBySecondWithoutAlert
- chemin: Rapport › zip | test: RapportZipTest#theZipStaysUnderItsSizeWithTheTextsAlwaysIn

tests: com.pocketstudio.bentest.* *ProjectsViewModel*
