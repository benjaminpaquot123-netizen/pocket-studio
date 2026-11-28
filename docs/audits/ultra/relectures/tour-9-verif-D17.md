# Tour 9 — vérification D17 (siège t9-75)

Base : 7455749a5. Défaut : « Kit UK Garage downloaden… 0 % » disparaît sans message, le kit reste « Studio » (gestes #3 tuto_kit_download, #4 tuto_next_big).

## Preuve sans banc
Test : `app/src/test/java/com/pocketstudio/app/tuto/T9D17KitDisappearsTest.kt` (Compose + Robolectric, sans banc humain).
Même suite que le siège : UK Garage, Pro, première étape faite, paquet absent → carte → « Télécharger » → barre à 0 % → (« Étape suivante » si présent) → le paquet arrive avec de vrais fichiers FLAC sur le disque.
Commande : `./gradlew :app:testDebugUnitTest --tests '*T9D17*'` → BUILD SUCCESSFUL, 2/2 verts.
- (a) décodeur du téléphone (`tutoKitDecodeForTest`) : kit = `pack`, pads [0, 4, 5, 6] chargés, la carte part. Le kit reste.
- (b) vrai `AudioDecoder` sous Robolectric (MediaCodec sans codec, comme dans le banc) : kit = `builtin`, aucun pad, la carte part, aucun `tuto_kit_error`, journal « Tuto : son du kit illisible ». C'est exactement l'écran 5 du siège.

## Cause
- `RhythmViewModel.kt:7320` : `AudioDecoder.decode` échoue sous Robolectric (pas de vrai MediaCodec) → chaque son est sauté.
- `RhythmViewModel.kt:7328-7330` : pads vides → `done(false)`.
- `AppRoot.kt` (~1983-1995) : `done(false)` → `tuto.setKit(KIT_BUILT_IN)` sans message ; l'offre passe à Wait, la carte disparaît.
Sur un téléphone le FLAC se décode (cas a) : le défaut vient du décodeur absent du banc, pas du parcours de l'appli.

## Remarque (finition, hors verdict)
Si un son du paquet était vraiment illisible sur un téléphone, le repli vers le kit Studio serait lui aussi silencieux (AppRoot ~1992). Un message court (« Kit indisponible, kit Studio gardé ») serait plus honnête. À trier comme finition, pas comme D17.

Verdict D17 : banc
