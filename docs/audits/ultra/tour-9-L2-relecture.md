# Relecture neuve — tour 9, lot L2 (Mix)

relu : de199d3423b5b2d4ba93c54c53034274f9334e1d

Branche `eq/tour-9-L2`, diff `7455749a5..HEAD -- app/src/main app/src/test` (captures et docs ignorées). Relecteur : sous-agent neuf, sans Gradle (lecture seule).

## Périmètre
- Fichiers touchés : tous dans la liste permise (mixer/AutomationCard, Console, EffectsBox, EffectsPanel, MixerScreen, QuickFx, SimpleMix ; human/ScreenText, human/BenchMixFaderCoverTest ; nav/SimpleMixTest ; nav/AppHarness limité à `GestureBudget.slide` ; tests neufs `mixer/Tour9*.kt`). Rien dans ui-components, tuto, AppRoot, RhythmScreen, project-model.
- Chaînes `tr(...)` neuves : toutes déjà présentes dans les catalogues (`  Effets du pad`, `Pad %1$d · %2$s`, `pad %1$d (%2$s)`, `Effets · %s`, `Voir les autres pistes`, `Sur 1 mesure.`, `Sur %d mesures.`, `Pour : %s`). Aucune chaîne en dur à l'écran.
- Thread audio, sauvegarde : non touchés (UI seule ; les réglages passent par les mêmes `MixerActions`). Pas de perte de sauvegarde vue.

## Points

- **grave** — **corrigé** — (part du lot ; reste AppRoot, confié au chef) app/src/main/java/com/pocketstudio/app/mixer/QuickFx.kt:111 (F136) : « Pour : <piste> » ne vient que de `LocalFxTrackName`, fourni seulement par les deux feuilles du Mix (Console.kt:377 et :438). Les deux autres entrées des effets rapides n'en fournissent pas et disent toujours la famille du son (« Pour : Pluck » sur Rhodes) : la carte Effets sous un son (`EffectsCard`, EffectsBox.kt:98, `EffectsUi` n'a pas de nom) et la boîte « Effets · nom » d'AppRoot.kt:673. La phrase « Les effets rapides proposés nomment la piste ouverte » n'est tenue que dans le Mix ; règle « corriger partout » (CLAUDE.md). AppRoot est hors du lot : à confier au lot qui le tient, ou ajouter le nom à `EffectsUi`.
- **mineur (risque D01)** — Console.kt:303-315 `capDrag`, `FADER_GRAB = 32.dp` : un fader ne se prend plus qu'à 32 dp de sa poignée (F024 option A). Sur un fader de 176 dp à 0 dB, un doigt posé au milieu (≈ 53 dp de la poignée) fait défiler la page et ne bouge rien, alors que l'aide dit toujours « Glisse un fader pour le volume ». C'est la phrase d'acceptation de F024, mais elle rétrécit la cible du défaut bloquant D01 pour un humain. Le choix « option A » n'a pas de ligne dans `docs/DECISIONS.md`. À regarder au prochain tour de testeurs (sièges débutants).
- **mineur** — Console.kt:229 `padNames` : aucun appelant ne le passe (MixerScreen inchangé), donc la feuille « Effets du pad » nomme toujours les pads du kit intégré, même si le projet a un autre kit ou des pads renommés.
- **mineur** — D12 : le test `d12_aPadsSendsAreFoundFromTheMix` prouve que la boîte d'effets du pad 3 s'ouvre depuis le Mix, pas la cohérence avec ce que dit le tuto (« là où le tuto le dit ») ; le texte du tuto (tutos/*.json, hors lot) n'est pas vérifié.
- **mineur** — MixerScreen.kt:310 (F141) : la liste au repos est mémorisée par `remember(channels)` ; pendant qu'on glisse un fader, `channels` change à chaque image (volume), donc la liste est encore refaite à chaque image du glissé (l'ancienne clé, volumes mis à 0, ne changeait pas). Gain réel au repos, pas pendant un glissé ; clé possible sans le volume. Le compteur `fitProbeRestLists++` dans un `remember` est un effet de bord de test en composition (sans danger).
- **mineur** — Tour9MixFinishTest.kt:151 (F141) : le test compte un compteur qui n'existait pas avant le correctif ; il ne pouvait pas être rouge sur l'ancien code (pas de commit « rouge » probant pour F141).
- **mineur** — SimpleMix.kt:851 : avec `LocalHeadLook`, les en-têtes non ouverts passent en `Text` simple (plus de `FitText`) : correct tant que `heads` (SimpleMix.kt:232) mesure exactement les mêmes noms et largeurs (`t.name + " ▸"`, `STRIP_W - 16.dp`, « Sortie » à `STRIP_W`) ; un changement de largeur de tranche sans changer cette liste couperait un nom sans prévenir.
- **mineur** — AutomationCard.kt:886-937 (F127) : la zone de toucher dépasse de 14 dp sous la courbe, `valueAt` borne à 0 : correct. Le `Modifier` reçu va maintenant sur la `Box` et le `testTag` sur le `Canvas` : vérifié, les tests visent bien le tag du Canvas.
- **mineur** — EffectsBox.kt:158 (F029) : `LaunchedEffect` conditionnel dans une boucle sans `key` ; `remember(i)` recrée le `BringIntoViewRequester` si les effets changent d'ordre : correct, pas de fuite. `added` n'est pas remis à -1 si l'emplacement ajouté n'apparaît pas (sans effet visible).

Rien de bloquant côté Compose : `capDrag` est en `pointerInput(Unit)` mais lit l'état par `rememberUpdatedState` (valeur et `onChange` à jour) ; le défilement horizontal des tranches et vertical de la page restent possibles (rien n'est consommé hors poignée, la pente verticale n'est prise qu'au-delà du seuil) ; S30 tenu (le déplacement compte depuis le premier contact, seuil compris) ; `setProgress` (TalkBack) conservé ; `MoreTracksTab` stable (disparaît en fin de défilement sans boucle de mise en page) ; pas d'allocation par image ajoutée dans les `Canvas`.

## Preuve D01

`app/src/test/java/com/pocketstudio/app/mixer/Tour9MixFaderNoBenchTest.kt` (relu en entier) : aucun import de `com.pocketstudio.app.human` (ni ScreenText, ni BenchWindows, ni sièges) ; seulement `createComposeRule`, `MixerScreen` / `SimpleMixScreen` réels, `acidTechnoSong()` (nav) et `simpleTracks()` (ui) comme données. Téléphone 360 dp, police à 200 %. `d01_aDragMovesTheProConsolesFader` et `d01_aDragMovesTheSimpleMixsFader` glissent d'un doigt Compose ordinaire, depuis la poignée, et vérifient que `setVolume` est appelé pour la bonne tranche ; `d11_aTouchOnAStripsNameOpensItsEffects` touche le nom et trouve `strip_sheet` + `effects_box_1`. Lancés verts par l'équipe : Tour9MixFaderNoBenchTest 8/8 (non relancé ici, Gradle interdit). Limite : ni le panneau du tuto ouvert ni la police à 130 % ne sont joués, alors que la phrase de D01 dit « pendant le tuto » ; les étapes levels/meters/buses du tuto ne sont pas rejouées par ce test (elles le sont par le banc, `GestureBudget.slide` visant la poignée).

## Seconde lecture (tête)
Relu `git diff f6e1ab5f HEAD -- app/src/main app/src/test`, plus 52c58e73 et 9267baab.
- Code de l'appli : seul EffectsBox.kt change depuis f6e1ab5f (`trackName` passé avant `more`). `EffectsCard` entoure `EffectsBox` d'un `LocalFxTrackName provides (ui.trackName ?: LocalFxTrackName.current)` : le nom passé gagne, sinon celui du dessus est gardé (aucune régression pour le Mix).
- Appels d'`EffectsUi` : AppRoot.kt:3093 et :3157 et ScreenshotTest.kt (1490, 1543, 1553, 1574, 1588, 1605) passent 3 arguments positionnels puis un lambda final, qui se lie bien à `more` (dernier paramètre). Le test neuf passe `trackName` par nom. Pas d'erreur de compilation visible à la lecture.
- `Tour9EffectsCardNameTest` : vrai `EffectsCard`, son de famille Pluck, nom « Rhodes » ; il vérifie le texte exact « Pour : Rhodes » sur `fx_category_suggested`, donc il échouerait sur « Pour : Pluck ». Non relancé ici (Gradle interdit) ; vert chez le correcteur (1/1).
- Fusion 4d45d774 : banc, tests du banc et docs seulement (plus deux lignes de CLAUDE.md) ; aucun fichier du lot, aucun code de l'appli.
- Jugement F136 : corrigé au sein du lot (le lot fournit le moyen et sa preuve ; il ne peut pas toucher AppRoot). Sur le téléphone, la phrase n'est pas encore tenue : AppRoot.kt:3093 et :3157 ne passent pas `trackName`, et la boîte « Effets · nom » (AppRoot.kt:673, `EffectsBox` direct) ne fournit pas `LocalFxTrackName`. Ces trois câblages vont au chef ; pour le compteur, F136 reste « tenté » jusque-là.
- Rien d'autre de grave vu.

## Suite donnée (correcteur)
- **corrigé** — Grave F136 : `EffectsUi.trackName` ajouté, `EffectsCard` le fournit (test `Tour9EffectsCardNameTest`). Le passage du nom depuis `AppRoot.kt` (hors lot) est confié au chef.
- Mineur F024 : noté dans `docs/decisions/tour-9-L2.md`.
- `trackName` placé avant `more` dans `EffectsUi` : les appels d'`AppRoot.kt` avec un lambda final compilent sans changement (tests ciblés verts).

Verdict : 0 bloquant, 0 grave
