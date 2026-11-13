# Tour 6 — lot « coquille2 » (fichiers partagés `RhythmViewModel.kt` / `AppRoot.kt`) : rapport

Branche `eq/tour-6-coquille2`, depuis la tête poussée `ccr-80af1a1b-mn0wec`, une paire rouge/vert par test. Restes « pas pris » de remix2b (siège B a) et d'exports2 (siège A c), plus P5 de padlive2 (siège A b). Preuves rejouées par le banc dans les conditions du siège d'origine, appli entière (`Coquille2SeatTest`), jamais sur un écran isolé.

Fichiers touchés : `rhythm/RhythmViewModel.kt`, `ui/AppRoot.kt`, `remix/CutScreen.kt`, `mixer/MixerScreen.kt` ; tests `ui/Coquille2SeatTest.kt` (neuf), `remix/CutScreenTightTest.kt` (lit `cut_auto` en vue au lieu de défiler jusqu'à lui : il ne défile plus).

tests: com.pocketstudio.app.ui.Coquille2SeatTest com.pocketstudio.app.remix.CutScreenTightTest

## Tenté, preuve rejouée (banc, conditions du siège)
- **E2** (A c, fr, Simple) : choisir seulement le volume du prochain fichier (« Streaming ») ne fait plus dire « Le morceau a changé depuis cet export ». `#e2_noSongChangeSaidWhenOnlyTheFilesVolumeIsChosen` (rouge 8eace0da, vert 00d8dafd).
- **P5** (A b, fr, Simple) : le projet Live rouvert depuis « Mes projets » s'ouvre sur Pad Live. `#p5_theLiveProjectOpensAgainOnPadLive` (rouge ad354da4, vert c63656f2).
- **A6** (B a, es, Simple, main gauche) : la chanson (0:40) entendue au-delà du projet (boucle d'1 mesure) : la fenêtre dit « su final se cortará » ET propose « Alargar el tema » ; le toucher allonge le morceau. `#a6_aSongLongerThanTheProjectIsSaidWithTheOfferToLengthen` (rouge f77cff89, vert b57a9c7c).
- **A4** (B a, geste 19) : en « Frases », « ✂ Frases ▾ » et « En el tiempo ▾ » restent entiers, plus coupés sous le panneau des actions. `#a4_thePhrasesModeMenusStayWholeInView` (rouge db8ecbe3, vert 41035050).
- **N2** (A c, geste 21) : pendant « Fabrication du son » de l'export Pro, « Annuler » est entier, plus sous la barre « Guide ». `#n2_theExportsCancelIsWholeWhileTheSoundIsMade` (rouge 44264af9, vert d5705280).

chemin: Partager › Autres fichiers › Volume visé Streaming (fr, téléphone, 100 %, Mode simple) | test: Coquille2SeatTest#e2_noSongChangeSaidWhenOnlyTheFilesVolumeIsChosen
chemin: Pad Live › Mes projets › autre projet › projet Live (fr, Mode simple) | test: Coquille2SeatTest#p5_theLiveProjectOpensAgainOnPadLive
chemin: Remix › Cortar lo que quiero › voz encendida (es, Mode simple, main gauche) | test: Coquille2SeatTest#a6_aSongLongerThanTheProjectIsSaidWithTheOfferToLengthen
chemin: Remix › Cortar la canción › Frases (es, Mode simple, main gauche) | test: Coquille2SeatTest#a4_thePhrasesModeMenusStayWholeInView
chemin: Partager › Autres fichiers › Exporter (Fabrication du son) (fr, Mode simple) | test: Coquille2SeatTest#n2_theExportsCancelIsWholeWhileTheSoundIsMade

## Pas pris (pour un coquille3)
- **E7** (« Retour » après un export revient d'où je viens) : contredit la règle du propriétaire (2026-10-29, « Mes projets › ← » va à l'accueil) et le lot « l'accueil arrête la musique » ; six tests l'encodent. Tentative annulée avant poussée. À trancher.
- **P1** (« Notes » montre tout ce que je joue) : le cadrage vit dans `synth/PianoRoll.kt` (zone tuto) et la règle « rien ne zoome tout seul » (lot tuto 5) ; la puce de `SynthScreen.kt` ne peut pas cadrer sans lui.
- **A10** (un seul « Descargar el modelo » par panneau) : retirer le bouton de la carte casse `nav/SimpleModeAuditTest` et `audit/BenchContrastRemixLibraryTest` (hors liste) ; retirer celui du pouce casse la portée du pouce gauche (`RemixThumbReachTest`). À autoriser.
- **N1, N4, N5, A11** : au-delà des 8 du lot (consigne du chef).
