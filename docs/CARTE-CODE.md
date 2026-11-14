# Carte du code (étape A3, 2026-11-03)

Lue sur fx-fin (`d82a0ee`, rangée dans fx-int à l'étape A2). Sert aux consignes fermées des lots B, C, D, E (`docs/PLAN-REPRISE.md`) : une équipe ne touche que les fichiers de sa liste ; un fichier absent de sa liste = demander à l'orchestrateur dans sa note de suivi `docs/lots/<lot>.md`.

Abréviations (les chemins sont exacts une fois développés) :
- `A/` = `app/src/main/java/com/pocketstudio/app/` · `T/` = `app/src/test/java/com/pocketstudio/app/`
- `M/` = `project-model/src/main/kotlin/com/pocketstudio/model/` · `MT/` = `project-model/src/test/kotlin/com/pocketstudio/model/`
- `I/` = `project-model/src/main/resources/i18n/<langue>/` (14 langues ; la clé est le texte français de `tr("…")`)
- `C/` = `engine/src/main/cpp/core/` · `EJ/` = `engine/src/main/java/com/pocketstudio/engine/` · `U/` = `ui-components/src/main/java/com/pocketstudio/ui/`

## 1. Une ligne par zone

**Racine et build**
- `settings.gradle.kts` (5 modules : app, engine, ui-components, project-model, separation), `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml` (versions), `signing/` (clés chiffrées), `dist/`.

**app (Kotlin, Compose)**
- `A/` racine : `MainActivity.kt` (activité unique, aucun raccourci clavier), `PocketStudioApplication.kt`, `AppInfo.kt`, `Metrics.kt`.
- `A/ui/` : `AppRoot.kt` (4 361 lignes : `AppTab`, toutes les routes et tous les branchements écran ↔ ViewModel, dont Live, Remix, Mix/export), `AppSpaces.kt` et `Spaces.kt` (barre d'espaces), `MainViewModel.kt`, `TopBar.kt`, `PlayStrip.kt`, `TempoSheet.kt`, `MuteSolo.kt`, `UiScale.kt`, `DoneBar.kt`, `ModeChoice.kt`.
- `A/onboarding/` : `LaunchChoice.kt` (accueil « Que veux-tu faire ? », cartes Continuer / Nouveau morceau / Mes projets / Remix), `BrandIntro.kt`, `Welcome.kt`, `IntroSound.kt`.
- `A/projects/` : `ProjectsScreen.kt` (Mes projets), `NewProjectPage.kt`, `ProjectsViewModel.kt`, `ProjectGate.kt`, `CurrentProject.kt`.
- `A/rhythm/` : `RhythmViewModel.kt` (10 903 lignes, le ViewModel central, découpé en sections `// --- …` : Live pads l. 679, Live grid l. 3614, Enregistrer la performance l. 4543, Lot 4b l. 4729, Remix l. 5501, 5b l. 5800, 5d l. 5979, Stem board l. 6190, Audio to notes l. 6828, Live recording l. 8072, Acid l. 8848, Export l. 9972 ; `ExportFormat` l. 269), `RhythmScreen.kt` (pas à pas et pads de batterie), `Slicer.kt`, `PadSamples.kt`, `StemExports.kt`, `TakeVault.kt`, `LiveTakeJournals.kt`, `Waveform.kt`.
- `A/live/` (nouveau Live, grille de briques) : `LiveScreen.kt`, `LiveGridLook.kt` (cases vides immobiles), `LiveCellMenu.kt`, `LiveFx.kt` + `LiveFxUi.kt` + `LiveXy.kt` (effets maintenus), `LiveAcid.kt`, `LiveNotes.kt`, `LiveDock.kt`, `LivePress.kt`, `LiveTake.kt`.
- `A/session/` : `SessionScreen.kt` (Morceau, Structure), `TimelineScreen.kt`, `PartSheet.kt`, `SongChords.kt`, `LivePad.kt` + `LivePadsCard.kt` + `LivePadsFullScreen.kt` (les pads live d'aujourd'hui).
- `A/remix/` : `RemixScreen.kt`, `RemixViewModel.kt`, `RemixFromHome.kt`, `SongImportSheet.kt`, `SeparationService.kt` + `SeparationJob.kt` + `SeparationModel.kt`, `StemStore.kt`, `StemBoard.kt` (contrat, `StemDestination`), `StemBoardModel.kt`, `StemBoardRouter.kt` (`StemBoardHost`, envois vers pads/Live/bibliothèque/piste), `StemBoardScreen.kt` (tableau des pistes, bouton « Garder »), `StemEditor.kt` (coupes, phrases), `SongEnergy.kt`, `RemixMemory.kt`.
- `A/library/` : `SoundShelfBrowser.kt` (le navigateur unique : dossiers, Récents, Favoris), `LibraryScreen.kt`, `WizardScreen.kt`, `SoundLibraryViewModel.kt`, `MySoundLibrary.kt`, `MySoundsOnKeys.kt`, `PackStore.kt`, `StyleFamilies.kt`, `SongGuide.kt`. `A/sounds/` : `SoundBrowser.kt`, `SoundLooks.kt`.
- `A/synth/` (`SynthScreen.kt`, `PianoRoll.kt`, `FloatingKeyboard.kt`, `ChordPads.kt`, `TranscribeCard.kt`…), `A/acid/` (`AcidScreen.kt`), `A/audiotracks/` (`AudioTracksScreen.kt`), `A/instruments/` : écrans d'instruments.
- `A/mixer/` : `MixerScreen.kt` (tranches et options d'export), `SimpleMix.kt` (Mode simple), `Console.kt`, `EffectsPanel.kt`, `EffectsBox.kt`, `QuickFx.kt`, `AutomationCard.kt`, `AutomateSheet.kt`, `MasterLoudness.kt` (LUFS, « Volume visé »), `EqCurve.kt`, `WhatPlays.kt`.
- `A/export/` : `ExportService.kt`, `WavExporter.kt`, `FlacEncoder.kt`, `AacEncoder.kt`, `CodecPump.kt`, `Loudness.kt`, `ExportCheck.kt`, `ShareSheet.kt`, `ShareSong.kt`, `ShareSongUi.kt` ; `A/export/video/` (vidéo de partage).
- `A/midi/` : `MidiViewModel.kt`, `MidiParser.kt` (lit déjà l'horloge 0xF8), `MidiScreen.kt`, `ControllerUi.kt`, `ControllerLegend.kt`, `KnobPages.kt`, `AutoKnobsVm.kt`. `A/record/` : `RecordBar.kt`, `Takes.kt`, `SongVersionsSheet.kt`.
- `A/audio/` (`AudioDecoder.kt`, `MicRecorder.kt`, `PcmPlayer.kt`, `FloatWav.kt`, `Headphones.kt`), `A/playback/` (service de fond), `A/settings/`, `A/guide/`, `A/tuto/`, `A/learn/`, `A/help/`, `A/report/`, `A/retours/`, `A/update/`, `A/download/`, `A/recette/`, `A/latency/`, `A/studio/`, `A/generate/`, `A/analysis/`.
- `app/src/main/AndroidManifest.xml` (aucun `configChanges` : l'activité est recréée quand la fenêtre change), `app/src/main/assets/` (`party/` sons Fête, `basic-pitch/` modèle, `changelog.txt`, `recette.json`).
- Tests : `T/<paquet>/` (live, remix, export, journey, onboarding, human = banc testeur humain, ui avec `MuteSoloEverywhereTest.kt`…) et `T/` racine (`ScreenshotTest.kt`, `TabletScreenshotTest.kt`, `CommandIdsTest.kt`, `LivePadsFullScreenTest.kt`…) ; `app/src/sharedTest/` (parcours) ; `app/src/androidTest/` (fumée émulateur) ; `app/screenshots/` (références Roborazzi).

**ui-components** (`U/`) : `Theme.kt` (Neon), `Components.kt`, `Adaptive.kt` (largeur à deux volets), `Windows.kt` (taille d'interface), `Light.kt` (lumières, animations coupées), `Feel.kt` (vibrations), `Holding.kt`, `SoundTile.kt`, `Knob.kt`, `Meter.kt`, `Motion.kt`, `Direction.kt`, `Help.kt`.

**project-model** (Kotlin pur, `M/`)
- Projet : `Project.kt` (+ `Mixer`, `DrumPattern`, `Channel`), `ProjectJson.kt`, `ProjectStore.kt` (écriture atomique), `ProjectSession.kt`, `History.kt` (annuler), `ProjectCatalog.kt`, `OpenOn.kt`.
- Live et pads : `Live.kt` (grille, réducteurs `LiveState`), `LiveCells.kt`, `LiveFx.kt`, `LivePads.kt` (`Project.live`), `LivePadPlan.kt`, `LivePadTake.kt`, `LiveStarter.kt` (pads prêts), `PartySounds.kt` (Fête), `Performance.kt`, `TapTempo.kt`.
- Remix : `Remix.kt` (`Project.remix`), `RemixVoice.kt`, `StemEdits.kt`, `SongMatter.kt`, `Transcription.kt`, `MySounds.kt`, `SoundShelves.kt`.
- Son et morceau : `SoundLibrary.kt`, `SoundPacks.kt`, `PackCatalog.kt`, `Effects.kt`, `FxPresets.kt`, `LoudnessTarget.kt`, `StarterWizard.kt`, `StyleRecipes.kt` (généré, jamais à la main), `Scenes.kt`, `SongTimeline.kt`, `MidiFile.kt`, `MidiMap.kt` ; sous-paquets `guide/`, `tuto/`, `learn/`, `midi/` (`Controllers.kt`), `video/`, `i18n/I18n.kt` (`I18n.AREAS`).
- Textes : `I/<zone>.json` ; tests : `MT/`.

**engine** (C++20 + JNI)
- Cœur `C/` (sans Android, testé sur Linux) : `Engine.h`/`.cpp` (`CommandType` : derniers numéros 140 à 146, `process`), `SpscQueue.h`, `Sequencer.h`, `ClipLauncher` (slots du Live), `LiveEffects.cpp` (effets de scène), `Mixer`, `MasterChain`, `Loudness.h`, `TruePeak.h`, `Oversampling.h`, `DriveShaper.h`, `OfflineRenderer` (export), `WavWriter`, `StemPlayer` (Remix), `TimeStretch`, `Sample`/`Sampler`/`SampleInstrument`/`MultiSampler`, `DrumKit`, `AcidTrack`, `Synth`, `PlaitsSynth`, `SoundFontPlayer`, `NoteTrack`, `AudioTracks`, `Effects`/`ExtraEffects`/`PlateReverb`, `OnsetDetector`/`Analysis`, `RoundTripMeter`.
- Colle Android : `engine/src/main/cpp/android/` (`AudioHost` Oboe, `JniBridge.cpp`) ; Kotlin `EJ/` (`NativeEngine.kt` API et `sink`, `CommandSink.kt` numéros de commande, `NativeSample.kt` dont `onsets`, `NativeStems.kt`, `OfflineExport.kt`, `SoundFont.kt`).
- `engine/host-tests/` : `engine_tests.cpp` + un `*_tests.inc` par lot, `CMakeLists.txt`, `reference/`. `engine/bench/` : `ps_render.cpp`, `ps_measure.cpp`, `ps_live.cpp`. `engine/third_party/` : liens, jamais commités.

**separation** : `separation/src/main/cpp/` (`Separator`, `PitchTranscriber`, `SeparationJni.cpp`), `separation/src/main/java/com/pocketstudio/separation/` (`Demucs.kt`, `BasicPitch.kt`, `StableAudio.kt`), `separation/host-tests/` (`separation_test.cpp`, `pitch_test.cpp`, `generate_test.cpp`).

**scripts** : `host-tests.sh` (moteur sur Linux), `live-bench.sh` (écoute en direct), `song-bench.sh` (banc du son), `separation-tests.sh` (séparation d'un mélange synthétique + Basic Pitch), `emulator-tests.sh`, `human-tester.sh` + `human-act.py` (testeur humain), `real-projects.sh`, `soak.sh`, `cpu-bench.sh`, `check-jni.sh`, `local-release.sh`, `screenshot-diff.py`, `bench/` (rapports), `packs/` (recettes → `StyleRecipes.kt`, packs), `party/synth_party.py` (sons Fête), `models/`, `guides/`, `brand/`.

**.github/workflows** : `ci.yml` (PR et main : tests, APK, pré-release), `nightly.yml` (« Nightly real use », à la main sur fx-int), `packs.yml`, `models.yml`.

## 2. Fichiers par lot

Règles : un lot ne modifie que sa liste ; « lit » = appelle sans changer. Fichiers neufs : chemins proposés, ajustables dans le même paquet. Chaque lot a sa note `docs/lots/<lot>.md`, son fichier de textes `I/<lot>.json` (+ une ligne dans `I18n.AREAS`), ses propres classes de test (jamais dans `T/ScreenshotTest.kt`).

### B. Pad Live (n° 25, `docs/maquettes/padlive-trois.png`)
- **B1 existants** : `A/onboarding/LaunchChoice.kt` (carte « Pad Live » sous « Remix ») ; `A/ui/AppRoot.kt` (route plein écran) ; `A/ui/AppSpaces.kt`, `A/ui/MainViewModel.kt` ; `A/projects/ProjectsViewModel.kt`, `ProjectsScreen.kt`, `NewProjectPage.kt`, `CurrentProject.kt` ; `M/ProjectCatalog.kt`, `M/OpenOn.kt` (projet « Live » créé ou repris) ; `A/session/LivePad.kt`, `LivePadsCard.kt`, `LivePadsFullScreen.kt` ; `M/LivePads.kt`, `M/LiveStarter.kt`, `M/PartySounds.kt`, `M/LivePadPlan.kt` ; `app/src/main/assets/party/`, `scripts/party/synth_party.py` (packs Techno, Trap, Fête…) ; règle « rien ne bouge » : `A/live/LiveGridLook.kt`, `A/live/LiveScreen.kt`, `A/rhythm/RhythmScreen.kt`, `U/Light.kt`, `U/Feel.kt`, `docs/DESIGN.md`, `docs/LIVE.md` ; `A/rhythm/RhythmViewModel.kt` (sections Live pads, Live grid, Enregistrer la performance, Lot 4b) ; `M/Project.kt` (champs à la fin) ; tests existants : `T/live/StillEmptyCellsTest.kt`, `T/live/LiveGridStyleTest.kt`, `T/LivePadsFullScreenTest.kt`, `T/onboarding/LaunchRuleTest.kt`, `T/onboarding/OnboardingUiTest.kt`, `T/journey/JourneyPathsTest.kt`, `app/src/sharedTest/java/com/pocketstudio/app/journey/`.
- **B1 neufs** : `A/padlive/PadLiveScreen.kt` (plein écran, niveau simple, « Plus »), `A/padlive/PadLivePages.kt` (pages A à D, choix du pack), `A/padlive/PadModes.kt` (coup, boucle, tenir, groupes, réglages par pad), `A/padlive/PadLiveController.kt` (logique tenue par `RhythmViewModel` en un seul champ, pour ne pas grossir la section Live) ; `M/PadLive.kt` (pages, modes, groupes, packs : réducteurs purs) ; `U/PadLight.kt` (le composant unique pad/case : lumière intérieure, anneau d'avancement, onde 200 ms, voile) ; `I/padlive.json` ; tests `T/padlive/PadLiveTouchCountTest.kt`, `T/padlive/PadLiveSaveUndoTest.kt`, `T/padlive/PadLiveScreenshotTest.kt`, `T/ui/StillPadsEverywhereTest.kt`, `MT/PadLiveTest.kt`.
- **B2 existants** : `C/LiveEffects.cpp`, `C/ClipLauncher.h`/`.cpp` (ligne lancée à la mesure), `C/Engine.h`/`.cpp`, `C/Mixer.h`/`.cpp`, `engine/src/main/cpp/android/JniBridge.cpp`, `EJ/NativeEngine.kt`, `EJ/CommandSink.kt`, `engine/host-tests/engine_tests.cpp` (une ligne `#include`), `T/CommandIdsTest.kt` ; `A/live/LiveFx.kt`, `LiveFxUi.kt`, `LiveXy.kt`, `M/LiveFx.kt` (Filtre, Écho, Bégaiement, Montée, Chute, Roulement) ; `M/TapTempo.kt`, `A/ui/TempoSheet.kt` ; `A/midi/MidiParser.kt`, `A/midi/MidiViewModel.kt` (horloge en entrée et en sortie) ; `M/LivePadTake.kt`, `M/Performance.kt`, `A/live/LiveTake.kt`, `A/rhythm/LiveTakeJournals.kt`, `A/rhythm/TakeVault.kt` (« Enregistrer ce que je joue »).
- **B2 neufs** : `engine/host-tests/padlive_tests.inc` ; `A/padlive/HeldFx.kt` ; `A/midi/MidiClockOut.kt` ; `T/padlive/PadLiveMidiTest.kt`, `T/padlive/PadLiveTakeTest.kt`. Commandes moteur : numéros 150 à 169.
- **B3 existants** : `A/export/ExportService.kt`, `A/export/ShareSheet.kt`, `A/rhythm/StemExports.kt` (export de la performance) ; `A/export/video/` (essai vidéo) ; `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml` (Ableton Link, capture d'écran) ; `T/human/HumanTester.kt` et `T/human/Bench*.kt` (personas) ; `scripts/human-tester.sh`.
- **B3 neufs** : `A/padlive/Challenge.kt` + `M/PadChallenge.kt` (défis), `A/padlive/PartyMode.kt` (« Fête » plein écran, lumières qui suivent le son), `A/padlive/PadLiveA11y.kt` ; `T/padlive/PadLiveExportTest.kt`, `T/human/HumanPadLivePersonasTest.kt`.

### C. Remix (n° 24 découpe `docs/maquettes/decoupe-deux.png`, « Garder », voix en notes)
- **Existants** : `A/remix/StemBoardScreen.kt` (entrée « Découper », « Garder » l. 650), `StemBoardModel.kt`, `StemBoardRouter.kt` (sauf la branche `StemDestination.Live`, à B), `StemBoard.kt`, `StemEditor.kt` (coupes auto : Phrases existe, Attaques et Mesures à ajouter), `RemixScreen.kt`, `RemixViewModel.kt`, `RemixFromHome.kt`, `SongImportSheet.kt`, `SeparationService.kt` + `SeparationJob.kt` (séparation coupée qui reprend), `StemStore.kt`, `SongEnergy.kt`, `RemixMemory.kt` ; `A/rhythm/RhythmViewModel.kt` (sections Remix, Step 5b, Step 5d, Stem board, Audio to notes seulement) ; `M/Remix.kt`, `M/StemEdits.kt` (dupliquer, boucler, inverser, transposer), `M/RemixVoice.kt`, `M/SongMatter.kt`, `M/Transcription.kt`, `M/MySounds.kt`, `M/SoundShelves.kt` ; « Garder » dans le navigateur unique : `A/library/SoundShelfBrowser.kt` (C seul), `A/library/MySoundLibrary.kt`, `A/library/LibraryScreen.kt`, `A/library/SoundLibraryViewModel.kt` ; `M/Project.kt` (champs à la fin) ; `docs/remix/REMIX-PROJET.md` ; tests existants `T/remix/*` (dont `StemEditorTest.kt`, `StemBoardRouterTest.kt`, `StemBoardScreenTest.kt`, `VoiceInWholeSongTest.kt`), `T/StemBoardScreenshotTest.kt`.
- **Lit sans changer** : `EJ/NativeSample.kt` (`onsets`, pour « Attaques »), `A/rhythm/Waveform.kt`, `C/OnsetDetector`.
- **Outil vraie chanson (Linux)** : `scripts/separation-tests.sh` (mode « un fichier donné », MP3 décodé par ffmpeg), `separation/host-tests/CMakeLists.txt`, `separation/host-tests/separation_test.cpp`, `separation/src/main/cpp/PitchTranscriber.cpp` (si la hauteur de la voix est à corriger) ; neufs `separation/host-tests/separate_file.cpp`, `scripts/voice-notes-check.py` (notes comparées à Basic Pitch, ±1 demi-ton, ≥ 80 %). La chanson et ses rendus restent hors du dépôt.
- **Neufs** : `A/remix/CutScreen.kt` (« Découper la chanson »), `A/remix/CutWave.kt` (onde, loupe, poignées, aimant), `A/remix/CutModel.kt` (bouts numérotés et colorés, sélection, annulation), `A/remix/RightsNotice.kt` (rappel des droits au partage) ; `M/SongCuts.kt` (coupes auto Phrases/Attaques/Mesures, aimant Temps : pur) ; `I/decoupe.json` ; tests `T/remix/CutJourneyTest.kt` (bout en bout, son synthétique), `T/remix/CutModelTest.kt`, `T/remix/CutScreenshotTest.kt`, `T/remix/KeepInBrowserTest.kt`, `MT/SongCutsTest.kt`.
- **Interdit à C** : moteur (`C/`, `JniBridge.cpp`, `EJ/`), `A/ui/AppRoot.kt` (la découpe s'ouvre depuis `StemBoardScreen.kt`, déjà routé), `A/live/`, `A/session/LivePad*`, `M/LivePads.kt`, `U/`. Étirer une piste de la chanson demande `C/StemPlayer`/`C/TimeStretch` : « Idée » dans REVIEW ou lot D.

### D. Restes (après B)
- **Case Remix du Live** : `C/ClipLauncher.*`, `C/StemPlayer.*`, `C/Engine.*`, `JniBridge.cpp`, `EJ/NativeEngine.kt`, `EJ/CommandSink.kt`, `T/CommandIdsTest.kt` (commandes 170 à 189) ; `M/Live.kt`, `M/LiveCells.kt`, `M/Remix.kt` ; `A/live/LiveScreen.kt`, `LiveCellMenu.kt`, `LiveGridLook.kt` ; `RhythmViewModel.kt` (Live grid, Lot 4b, Remix) ; neufs `engine/host-tests/liveremix_tests.inc`, `T/live/LiveRemixCellTest.kt`.
- **MIDI vers la 303** : `A/midi/MidiViewModel.kt`, `A/live/LiveAcid.kt`, `RhythmViewModel.kt` (Lot 4b, Acid) ; `T/live/LiveMidiJourneyTest.kt`.
- **Anciens écrans comme le Live** : `A/rhythm/RhythmScreen.kt`, `A/acid/AcidScreen.kt`, `A/synth/SynthScreen.kt`, `A/audiotracks/AudioTracksScreen.kt`, `A/session/SessionScreen.kt`, `A/record/RecordBar.kt`, `A/record/Takes.kt`, `RhythmViewModel.kt` (Live recording) ; neuf `T/journey/OldScreensLikeLiveTest.kt`.
- **Lumières du MiniLab** : `M/midi/Controllers.kt`, `A/midi/ControllerUi.kt`, `A/midi/MidiViewModel.kt`, `docs/MINILAB.md`.
- **Tablette et clavier** : `U/Adaptive.kt`, `U/Windows.kt`, `A/ui/AppRoot.kt`, `A/live/LiveScreen.kt`, `A/padlive/PadLiveScreen.kt` (paysage), `A/session/SessionScreen.kt` + `A/remix/CutScreen.kt` (côte à côte), `A/MainActivity.kt` (touches), `app/src/main/AndroidManifest.xml` (`configChanges`), `A/PocketStudioApplication.kt`, `A/playback/PlaybackService.kt` ; `T/TabletScreenshotTest.kt` ; neufs `A/ui/KeyShortcuts.kt`, `T/ui/KeyShortcutsTest.kt`, `T/ui/ResizeWhilePlayingTest.kt`, `T/padlive/TenFingersTest.kt`.
- **Vieux restes** : `scripts/packs/recipes/*.json` → `M/StyleRecipes.kt` par `scripts/packs/recipes_to_kotlin.py` ; `I/restes.json`.

### E. Master pro (PLAN-AUDITS-FINAUX §1 bis, points 1 à 6)
- **Moteur** : `C/MasterChain.h`/`.cpp`, `C/Loudness.h`, `C/TruePeak.h`, `C/OfflineRenderer.*`, `C/WavWriter.*`, `C/Mixer.*` (A/B à gain compensé), `C/Effects.*` (EQ, glue), `C/Engine.*`, `JniBridge.cpp`, `EJ/NativeEngine.kt`, `EJ/OfflineExport.kt`, `EJ/CommandSink.kt`, `T/CommandIdsTest.kt` (commandes 190 à 209) ; `engine/bench/ps_render.cpp`, `engine/bench/ps_measure.cpp`, `scripts/song-bench.sh` ; neufs `C/LoudnessRange.h` (court terme, LRA, PLR, corrélation), `engine/host-tests/masterpro_tests.inc`, `scripts/export-matrix.sh` (paires de la matrice).
- **App** : `A/export/` (tous ses fichiers, `video/` compris, règle de B3), `A/rhythm/StemExports.kt`, `A/mixer/MasterLoudness.kt`, `A/mixer/MixerScreen.kt`, `A/mixer/EffectsPanel.kt`, `A/mixer/SimpleMix.kt` (lu : 3 choix inchangés), `RhythmViewModel.kt` (`ExportFormat`, sections Export et S6 son) ; `A/ui/AppRoot.kt` : seulement le bloc `MixerActions`, un paramètre en plus ; `M/LoudnessTarget.kt`, `M/Project.kt` (`Mixer`, champs à la fin), `M/MidiFile.kt`, `M/Effects.kt`.
- **Neufs** : `A/mixer/MasterPro.kt` (« Avancé » en Pro : chaîne, A/B, référence), `A/export/ExportOptions.kt` (bits, fréquence, dither, plage, fondus), `A/export/ExportReport.kt`, `A/export/Metadata.kt`, `M/MasterPro.kt` ; `I/masterpro.json` ; tests `T/export/ExportPairsTest.kt`, `T/export/StemNullTest.kt`, `T/export/AbSameLevelTest.kt`, `T/mixer/SimpleModeUnchangedTest.kt`.

### E2 (pour mémoire) : `app/build.gradle.kts`, `A/ui/AppRoot.kt`, `.github/workflows/ci.yml`, `CLAUDE.md`, `scripts/*.sh`, `docs/play/`, `A/settings/` (« À propos et licences »).

## 3. Fichiers partagés et propriétaire

| Fichier | Propriétaire | Règle |
| --- | --- | --- |
| `docs/DECISIONS.md`, `REVIEW.md`, `PROGRESS.md`, `THIRD_PARTY.md` | orchestrateur | les lots écrivent dans `docs/lots/<lot>.md`, recopié au rangement |
| `M/i18n/I18n.kt` (`I18n.AREAS`), `I/<lot>.json` | chaque lot pour sa ligne et son fichier | conflit = garder les deux lignes |
| `M/Project.kt` | B, C, D, E | champs à la fin, valeur par défaut, pas de nouveau format |
| `A/ui/AppRoot.kt`, `LaunchChoice.kt`, `AppSpaces.kt` | B, puis D | C n'y touche pas ; E : seul le bloc `MixerActions` |
| `A/rhythm/RhythmViewModel.kt` | par section (voir §1) | B : Live ; C : Remix ; D : Live après B ; E : Export |
| `A/remix/StemBoardRouter.kt` | C | branche `StemDestination.Live` (lit `p.live.clips`, `LivePads.MAX`) : B |
| `A/library/SoundShelfBrowser.kt` | C | B l'appelle sans le changer |
| `A/export/ShareSheet.kt`, `ExportService.kt` | B (pendant B/C), puis E | rappel des droits de C dans `A/remix/RightsNotice.kt`, appelé d'une ligne au rangement |
| `U/*` (ui-components) | B (pendant B/C), puis D | C crée ses composants dans `A/remix/` |
| `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml` | B (pendant B/C), puis D | un autre lot demande par sa note |
| moteur, `EJ/`, `JniBridge.cpp`, `engine/host-tests/engine_tests.cpp`, `T/CommandIdsTest.kt` | une équipe à la fois (B, D, E) | commandes B 150–169, D 170–189, E 190–209 |
| `M/Live.kt`, `M/LiveCells.kt`, `A/live/*` | B, puis D | |
| `M/Remix.kt`, `A/remix/*` | C, puis D | |
| `app/screenshots/` | personne | conflit = réenregistré et regardé au rangement |
