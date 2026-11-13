# Relecture neuve — lot « exports2 » (tour 6, zone Exports), aller-retour 1

Relecteur neuf (n'a pas écrit le code), sur le diff `d946d916..HEAD` : export/ShareSheet.kt, ShareSongUi.kt,
MyExports.kt, SaveProjectZip.kt, projects/ProjectsScreen.kt (deux libellés accordés), i18n/*/partage.json (une clé accordée).

relu : 4cd0dade1f8b56f911a630aac2c9a07a537e230a

## Relancé et vu
- `:project-model:test --tests '*I18nCatalogTest'` (13/13 vert) et `:app:testDebugUnitTest` export.*, projects.*,
  BenchSelfTest : 325 tests, 0 échec.
- Rejeu rouge : ShareSheet.kt remis à `5272056b^` → `Tour6ExportsWholeLinesTest` ROUGE (2/2, assertion
  « share_check_ok at 470dp: 12 of 60 px ») ; restauré, arbre propre, HEAD inchangé.
- Banc, parcours du siège Ac-2 (start.txt du siège, appli entière, selftest 176/0) : « oracle tenu » à l'écran 47.

## Contrôles (OK)
- Pas de boucle de mise en page (`stops` stable sous verticalScroll, garde `it != stops`).
- `snapHeight` jamais ≤ 0 (`pad + cut.top > 0` exigé).
- U+200B seulement à l'affichage (`breakableName` dans le `Text`), ni le nom ni l'URI ne changent.
- « Envoyer le projet (.zip) » dans les 14 langues ; seuls ProjectsScreen.kt:338 et :345 changent.

## Graves
Aucun.

## Moyens (repris dans docs/REVIEW.md)
- AppRoot.kt:940 (projet illisible) et ProjectsViewModel.kt:215/233 disent encore « Sauvegarder le morceau en .zip ».
- SettingsScreen.kt:224 : l'aide renvoie à « Partager › Sauvegarder le morceau en .zip », libellé disparu.
- ShareSheet « détails » (Pro) : la mesure jouée sous « Volume » (la cible), sans mot pour les distinguer.
- E3 : une ligne entière peut être cachée sous un indice de fondu plus faible ; la première image n'est pas arrondie.
- `Tour6ExportsWholeLinesTest` ne vérifie pas `share_target` (grande police).
- Mode simple : l'aide sous les puces a disparu, l'étiquette est « Volume visé » (clés déjà traduites).

Verdict : 0 bloquant, 0 grave
