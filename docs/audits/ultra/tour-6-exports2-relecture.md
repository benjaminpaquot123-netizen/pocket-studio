# Relecture neuve — lot « exports2 » (tour 6, zone Exports), aller-retour 1

Relecteur neuf (n'a pas écrit le code), sur le diff `d946d916..HEAD` : export/ShareSheet.kt, ShareSongUi.kt,
MyExports.kt, SaveProjectZip.kt, projects/ProjectsScreen.kt (deux libellés accordés), i18n/*/partage.json (une clé accordée).

relu : 773e0caab44daf3feddc841d5ae2964b2fc2c011

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

## Re-signature après fusion de la tête (bdb777a9)
- La fusion ne touche aucun fichier du lot (export/, ProjectsScreen.kt, i18n/*/partage.json, tests export) ; relancés sur la
  fusion : I18nCatalogTest, export.*, projects.*, BenchSelfTest : verts.

## Relecture après la CI 276 (relu 3ef415e1)
- Diff 248a2ca3..3ef415e1 : « Partager le projet (.zip) » (LexiconWordsTest : « Envoyer le projet » mot ancien, réponse
  1A) et le « ? » du sélecteur Simple (ScreenScanTest). Relancés : I18nCatalogTest, LexiconWordsTest, export.*, projects.*,
  ScreenScanTest, HelpCoverageTest, BenchSelfTest : 659 + 20 tests, 0 échec. Rejeu rouge : ShareSheet.kt de 6344abf5^ →
  Tour6ExportsSimpleTargetHelpTest ROUGE (« expected:<1> but was:<0> ») ; restauré, arbre propre.
- Traductions : les 14 langues disent « partager » avec le mot de leur clé « Partager ». ProjectsScreen.kt : l. 338 et
  345 seulement. Aucune unité LUFS dans l'aide du Mode simple.
- Moyens : « Envoyer le projet » reste dans deux commentaires et un nom de test ; « Volume visé » en indonésien est
  « Target loudness » (anglais), plus visible comme titre du « ? ».

## Relecture après la fusion de langues2 (relu fdbc0f50)
- MyExports.kt identique à la tête (FileName de langues2, même défaut E4) ; `Tour6ExportsNameWrapTest` lit les lignes
  coupées à la main, toujours sensible à une coupure entre deux lettres ou deux chiffres.
- `SimpleTargetPicker` en un seul bloc (mots, puces, « ? ») ; « Désactivée » montrée seulement quand c'est la cible.
- Relancés : I18nCatalogTest, LexiconWordsTest, export.*, projects.*, ScreenScanTest, HelpCoverageTest, BenchSelfTest :
  701 tests, 0 échec. Rejeu rouge : ShareSheet.kt de fdbc0f50^ → `theSimpleTargetStaysCompact` ROUGE (134,67 dp > 110).
- Banc, parcours Ac-2 (selftest 176/0) : « oracle tenu », puces Streaming et Club touchées sans défiler.
- Moyens : le test de noms ne regarde plus `hasVisualOverflow` ; une ligne inutile (`after`) ; en Simple, revenir à
  « Désactivée » demande Pro ou Mix ; aucune capture du bloc « Volume visé » simple.

## Re-signature (fusion de la tête c384b693, contrôle Ac-4)
- Depuis fdbc0f50 (dernière relecture du code) : fusion de la tête, copie du siège Ac-4, rapport ; docs/REVIEW.md rendu
  à la tête. Aucun fichier du lot (export/, ProjectsScreen.kt, partage.json, tests export) n'a changé ; relancés sur la
  fusion : I18nCatalogTest, LexiconWordsTest, export.*, projects.*, ScreenScanTest, BenchSelfTest : verts.

## Re-signature (refus de forme du portail)
- Fusion de la tête et ligne « tests: » du rapport en noms de classes ; aucun fichier de code ni de test du lot n'a changé.

## Rouge constaté rejoué par le relecteur
rouge constaté : d8ef698c2cb38b7a11bde5980c1033d1291501fb : rejoué sur 8c73d6c6 : BenchSelfTest#chipsTogglesAndDrawnMarksAreWrittenAsDrawn ROUGE (1 échec sur 12 tests, 1 ignoré) : « java.lang.AssertionError: written otherwise than drawn: the check's icon before « Rien à entendre »: - » ; VERT sur d8ef698c (0 échec sur 12, 1 ignoré)

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
