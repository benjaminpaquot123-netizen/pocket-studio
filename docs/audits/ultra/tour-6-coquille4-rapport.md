# Tour 6 — lot « coquille4 » (le motif de « Mes fichiers ») : rapport

Branche `eq/tour-6-coquille4`, depuis `eq/tour-6-coquille3` (596ba12b), à la demande du chef : le motif trouvé dans « Mes fichiers » (contrôle Cq3, portail du paquet 8) — un effet d'écran qui lit hors du fil principal puis écrit le résultat dans l'état de l'écran depuis le fil de la lecture, écriture qui peut ne jamais arriver à l'écran — cherché dans toute l'appli et corrigé.

Fichiers touchés : `retours/RetoursUi.kt`, `ui/AppRoot.kt`, `export/video/VideoSheet.kt` ; tests `ui/Coquille2SeatTest.kt`, `ui/Coquille3SeatTest.kt`, `human/HumanRemixJourneyTest.kt` ; test `audit/StateWrittenOnMainTest.kt` (neuf).

tests: com.pocketstudio.app.audit.StateWrittenOnMainTest com.pocketstudio.app.audit.ScreenScanTest com.pocketstudio.app.nav.RemixSongScreenshotTest com.pocketstudio.app.ui.Coquille2SeatTest com.pocketstudio.app.ui.Coquille3SeatTest

## Tenté, preuve rejouée
- Un test de garde lit toutes les sources de l'appli : un `LaunchedEffect` ou un `produceState` qui garde le résultat d'un `withContext(Dispatchers.IO)` doit l'écrire sur le fil principal. Rouge sur trois endroits, vert après correction : `RetoursUi.kt:326` (les fichiers de retours), `AppRoot.kt:547` (la taille des packs de la bibliothèque), `AppRoot.kt:1372` (la chanson séparée depuis l'accueil, qui attend son projet) ; puis, relecture (moyen), le même motif avec `Dispatchers.Default` : `VideoSheet.kt:218` (l'aperçu de la vidéo), rouge puis vert. `StateWrittenOnMainTest#aScreensEffectKeepsWhatItReadOffTheMainThreadOnTheMainThread`.
- Rejoués verts autour : `retours.*`, `library.*`, `remix.*`, `nav.RemixBothWaysTest`, `nav.Tour6RemixCancelTest`, `ui.Coquille2SeatTest`, `ui.Coquille3SeatTest`.

chemin: Bibliothèque de sons (taille des packs) | test: StateWrittenOnMainTest#aScreensEffectKeepsWhatItReadOffTheMainThreadOnTheMainThread
chemin: Accueil › Remix › Dans un projet existant (la chanson qui attend) | test: StateWrittenOnMainTest#aScreensEffectKeepsWhatItReadOffTheMainThreadOnTheMainThread
chemin: Partager › Vidéo pour les réseaux (aperçu) | test: StateWrittenOnMainTest#aScreensEffectKeepsWhatItReadOffTheMainThreadOnTheMainThread
chemin: Retours › fichiers | test: StateWrittenOnMainTest#aScreensEffectKeepsWhatItReadOffTheMainThreadOnTheMainThread

## Limites
- Le test lit le code : il voit `x = withContext(Dispatchers.IO ou Default)` et `val x = …` dans un `LaunchedEffect` ou un `produceState` ; il ne voit pas `x.y = …`, les `rememberCoroutineScope().launch`, et toute mention de `Dispatchers.Main` dans le bloc le satisfait ; une lecture cachée derrière une fonction (comme le `read` par défaut de « Mes fichiers », corrigé dans coquille3) lui échappe.
- Les `scope.launch` des classes qui ne sont pas des écrans (portée du fil principal) ne sont pas visés.

## CI 43566115749 et le Banc : suites
- **Recensement** (`ScreenScanTest` census `RemixSongScreenshotTest.matter`, HELP 45 > 42) : les deux boutons de l'avis venus de coquille2/3 ; « Compris » ferme la fenêtre (`notice_close`, une sortie évidente), et la phrase de l'avis explique « Allonger le morceau » (`HelpAbout`). Vert : `ScreenScanTest` en entier, `nav.RemixSongScreenshotTest`, `remix.*`, `HumanRemixJourneyTest`, `Coquille2SeatTest`, `Coquille3SeatTest` (655 tests d'un passage, le recensement après ceux du Remix).
- **Ordre mélangé** (Banc : `Coquille2SeatTest#e2_` « export pas commencé ») : les tests de sièges attendent que le fichier soit fait, puis touchent « WhatsApp » par son nom quand le menu de partage du banc est là. Mesuré : le banc ne montre ce menu que si la prochaine activité lancée de Robolectric est le partage ; dans un ordre mélangé, une activité laissée avant passe devant et le menu ne vient pas (le fichier, lui, est fait). Les tests n'en ont pas besoin pour ce qu'ils lisent et continuent sans. Vert 3 fois de suite mélangé avec `export.*` et `HumanRemixJourneyTest`. À réparer côté banc : `HumanTester.shareMenu()` devrait laisser passer les activités qui ne sont pas un partage. Pas d'option `-Pshuffle` sur cette branche (elle vient du lot Banc).
- Vu une fois dans ces passages : `HumanRemixJourneyTest` « no progress: [download, separate, analyse, decode] » (avant la pose, pas ce lot).
