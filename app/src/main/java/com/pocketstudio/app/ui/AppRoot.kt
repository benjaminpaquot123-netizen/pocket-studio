package com.pocketstudio.app.ui

import com.pocketstudio.app.instruments.InstrumentUi
import com.pocketstudio.app.synth.NotesUi
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.model.NoteClip
import com.pocketstudio.app.report.AppLog
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Tune
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketstudio.app.Metrics
import com.pocketstudio.app.acid.AcidActions
import com.pocketstudio.app.acid.AcidScreen
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.latency.LatencyScreen
import com.pocketstudio.app.playback.BackgroundPage
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.mixer.MixerScreen
import com.pocketstudio.app.latency.LatencyViewModel
import com.pocketstudio.app.recette.RecetteScreen
import com.pocketstudio.app.recette.RecetteViewModel
import com.pocketstudio.app.rhythm.PadSoundUi
import com.pocketstudio.app.rhythm.RhythmActions
import com.pocketstudio.app.rhythm.RhythmScreen
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.SampleTask
import com.pocketstudio.app.studio.StudioScreen
import com.pocketstudio.app.synth.SynthActions
import com.pocketstudio.app.synth.SynthScreen
import com.pocketstudio.app.remix.RemixActions
import com.pocketstudio.app.session.SessionActions
import com.pocketstudio.app.session.SessionScreen
import com.pocketstudio.app.synth.TranscribeUi
import com.pocketstudio.app.midi.MidiActions
import com.pocketstudio.app.midi.MidiCommand
import com.pocketstudio.app.midi.MidiScreen
import com.pocketstudio.app.midi.MidiViewModel
import com.pocketstudio.app.sounds.SoundBrowserUi
import com.pocketstudio.model.SoundCategories
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.app.session.LivePadsActions
import com.pocketstudio.app.session.LivePadsUi
import com.pocketstudio.app.session.SessionScreenUi
import com.pocketstudio.model.Scene
import com.pocketstudio.model.allScenes
import com.pocketstudio.app.mixer.AutomationActions
import com.pocketstudio.app.mixer.AutomationUi
import com.pocketstudio.app.remix.RemixScreen
import com.pocketstudio.app.remix.RemixUi
import com.pocketstudio.app.remix.RemixViewModel
import com.pocketstudio.app.studio.StudioViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.ui.Neon
import kotlinx.coroutines.delay

enum class AppTab(val label: String, val icon: ImageVector) {
    Rythme("Rythme", Icons.Filled.GridView),
    Acide("Acide", Icons.Filled.GraphicEq),
    Synthe("Synthé", Icons.Filled.Piano),
    Mixeur("Mixeur", Icons.Filled.Equalizer),
    Remix("Remix", Icons.Filled.Layers),
    Morceau("Morceau", Icons.Filled.QueueMusic),
    Outils("Outils", Icons.Filled.Tune),
}

private enum class ToolPage(val label: String) { Recette("Recette"), Midi("MIDI"), Fond("Fond"), Latence("Latence"), Son("Son") }

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val mainVm: MainViewModel = viewModel()
    val studioVm: StudioViewModel = viewModel()
    val latencyVm: LatencyViewModel = viewModel()
    val recetteVm: RecetteViewModel = viewModel()
    val midiVm: MidiViewModel = viewModel()
    val rhythmVm: RhythmViewModel = viewModel()
    val remixVm: RemixViewModel = viewModel()
    var tab by rememberSaveable { mutableStateOf(AppTab.Rythme) }
    var toolPage by rememberSaveable { mutableStateOf(ToolPage.Recette) }

    val engineAvailable = NativeEngine.isAvailable
    val stream by Metrics.streamInfo.collectAsStateWithLifecycle()
    val roundTrip by Metrics.roundTripMs.collectAsStateWithLifecycle()

    // The audio stream lives while the app is visible, and in the background while it plays.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        rhythmVm.visible = true
        val started = engineAvailable && NativeEngine.start()
        AppLog.add(if (started) "Premier plan : son ouvert" else "Premier plan : son NON ouvert (moteur ${if (engineAvailable) "présent" else "absent"})")
        if (started) {
            studioVm.pushAll()
            rhythmVm.syncEngine()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        latencyVm.cancel()
        rhythmVm.flush()
        rhythmVm.visible = false
        rhythmVm.synthAllOff()
        rhythmVm.stopRecording()  // never keep the microphone open in the background
        // While the transport plays, PlaybackService keeps the music going in the background.
        val keep = rhythmVm.keepsRunningInBackground
        AppLog.add(if (keep) "Arrière-plan : la lecture continue" else "Arrière-plan : son fermé")
        if (engineAvailable && !keep) NativeEngine.stop()
    }
    val saveError by rhythmVm.session.lastSaveError.collectAsStateWithLifecycle()
    val notice by rhythmVm.notice.collectAsStateWithLifecycle()
    notice?.let { text ->
        AlertDialog(
            onDismissRequest = rhythmVm::dismissNotice,
            confirmButton = { TextButton(onClick = rhythmVm::dismissNotice) { Text("Compris") } },
            text = { Text(text) },
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(engineAvailable) {
        if (!engineAvailable) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                Metrics.updateStream(NativeEngine.streamInfo())
                delay(100)
            }
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) latencyVm.measure()
    }
    val recordPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) rhythmVm.startRecording()
    }
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) midiVm.scanBluetooth()
    }
    // MIDI clock, start and stop drive the project's tempo and transport (main thread).
    LaunchedEffect(Unit) {
        midiVm.commands.collect { c ->
            when (c) {
                MidiCommand.Start -> if (!rhythmVm.transport.value.playing) rhythmVm.togglePlay()
                MidiCommand.Stop -> if (rhythmVm.transport.value.playing) rhythmVm.togglePlay()
                is MidiCommand.Tempo -> rhythmVm.setTempo(c.bpm)
            }
        }
    }
    val pickNotesAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.transcribeFile(uri)
    }
    val notesMicPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) rhythmVm.recordNotes()
    }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importSample(uri)
    }
    var liveImportPad by rememberSaveable { mutableIntStateOf(0) }
    val pickLiveAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importLive(liveImportPad, uri)
    }
    val pickSong = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) remixVm.separate(uri)
    }
    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importZip(uri)
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) rhythmVm.importFolder(uri)
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Scaffold(
        containerColor = Neon.Background,
        topBar = {
            if (saveError != null) {
                Text(
                    "Sauvegarde impossible (mémoire pleine ?). Tes dernières modifications ne sont pas encore enregistrées.",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().background(Neon.Amber).statusBarsPadding().padding(8.dp)
                        .testTag("save_error"),
                )
            }
        },
        bottomBar = {
            if (!landscape) {
                NavigationBar(containerColor = Neon.Surface) {
                    AppTab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("tab_${t.name}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Neon.Cyan,
                                selectedTextColor = Neon.Cyan,
                                indicatorColor = Neon.SurfaceHigh,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(Modifier.padding(padding)) {
            // Landscape: tabs on the side so the screen keeps its height (wider synth keyboard).
            if (landscape) {
                NavigationRail(containerColor = Neon.Surface) {
                    AppTab.entries.forEach { t ->
                        NavigationRailItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                            modifier = Modifier.testTag("tab_${t.name}"),
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Neon.Cyan,
                                selectedTextColor = Neon.Cyan,
                                indicatorColor = Neon.SurfaceHigh,
                            ),
                        )
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                when (tab) {
                    AppTab.Rythme -> {
                        val project by rhythmVm.project.collectAsStateWithLifecycle()
                        val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                        val live by rhythmVm.live.collectAsStateWithLifecycle()
                        val canUndo by rhythmVm.session.canUndo.collectAsStateWithLifecycle()
                        val canRedo by rhythmVm.session.canRedo.collectAsStateWithLifecycle()
                        val sampleInfo by rhythmVm.sampleInfo.collectAsStateWithLifecycle()
                        val sampleTask by rhythmVm.sampleTask.collectAsStateWithLifecycle()
                        val slicer by rhythmVm.slicer.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                        }
                        RhythmScreen(
                            project = project,
                            transport = transport,
                            live = live,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            actions = RhythmActions(
                                togglePlay = rhythmVm::togglePlay,
                                toggleMetronome = rhythmVm::toggleMetronome,
                                toggleCountIn = rhythmVm::toggleCountIn,
                                hitPad = rhythmVm::hitPad,
                                toggleStep = rhythmVm::toggleStep,
                                cycleVelocity = rhythmVm::cycleVelocity,
                                clearPad = rhythmVm::clearPad,
                                nudgeTempo = rhythmVm::nudgeTempo,
                                tapTempo = rhythmVm::tapTempo,
                                setSwing = rhythmVm::setSwing,
                                undo = rhythmVm::undo,
                                redo = rhythmVm::redo,
                                importSample = { pickAudio.launch(arrayOf("audio/*")) },
                                startRecording = {
                                    if (hasMic(context)) rhythmVm.startRecording()
                                    else recordPermission.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                stopRecording = rhythmVm::stopRecording,
                                removeSample = rhythmVm::removeSample,
                                dismissSampleError = rhythmVm::dismissSampleError,
                                editSample = rhythmVm::editPadSample,
                                openSlicer = rhythmVm::openSlicer,
                                closeSlicer = rhythmVm::closeSlicer,
                                setSliceMode = rhythmVm::setSliceMode,
                                setSliceSensitivity = rhythmVm::setSliceSensitivity,
                                setSliceGrid = rhythmVm::setSliceGrid,
                                toggleSliceMarker = rhythmVm::toggleSliceMarker,
                                applySlices = rhythmVm::applySlices,
                                resampleSong = rhythmVm::resampleSong,
                                importFolder = { pickFolder.launch(null) },
                                importZip = { pickZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                            ),
                            padSound = PadSoundUi(sampleInfo, sampleTask, slicer),
                        )
                    }
                    AppTab.Acide -> {
                        val project by rhythmVm.project.collectAsStateWithLifecycle()
                        val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                        val live by rhythmVm.live.collectAsStateWithLifecycle()
                        val selected by rhythmVm.selectedAcidStep.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                        }
                        val favorites by rhythmVm.favorites.collectAsStateWithLifecycle()
                        AcidScreen(
                            acid = project.acid,
                            playing = transport.playing,
                            currentStep = live.absoluteStep,
                            selected = selected,
                            actions = AcidActions(
                                togglePlay = rhythmVm::togglePlay,
                                generate = rhythmVm::generateAcid,
                                selectStep = rhythmVm::selectAcidStep,
                                editStep = rhythmVm::editAcidStep,
                                toggleLock = rhythmVm::toggleAcidLock,
                                setSound = rhythmVm::setAcidSound,
                                shiftRoot = rhythmVm::shiftAcidRoot,
                                setDensity = rhythmVm::setAcidDensity,
                                setBars = rhythmVm::setAcidBars,
                                copyPage = rhythmVm::copyAcidPage,
                            ),
                            browser = SoundBrowserUi(
                                SoundCategories.ACID, SoundLibrary.ACID, favorites.acid,
                                current = (SoundLibrary.ACID + favorites.acid).firstOrNull {
                                    it.sound.copy(tuning = project.acid.sound.tuning, level = project.acid.sound.level) == project.acid.sound
                                }?.name,
                                // Tuning and volume stay as they are: a sound changes the character only.
                                pick = { rhythmVm.applyAcidSound(SoundLibrary.ACID[it].sound.copy(tuning = project.acid.sound.tuning, level = project.acid.sound.level)) },
                                pickFavorite = { rhythmVm.applyAcidSound(favorites.acid[it].sound) },
                                deleteFavorite = rhythmVm::deleteAcidFavorite,
                                random = rhythmVm::randomAcid,
                                save = rhythmVm::saveAcidFavorite,
                                tag = "acid_sounds",
                            ),
                        )
                    }
                    AppTab.Synthe -> {
                        DisposableEffect(Unit) { onDispose { rhythmVm.synthAllOff() } }
                        val project by rhythmVm.project.collectAsStateWithLifecycle()
                        val octave by rhythmVm.keyboardOctave.collectAsStateWithLifecycle()
                        val live by rhythmVm.live.collectAsStateWithLifecycle()
                        val library by rhythmVm.library.collectAsStateWithLifecycle()
                        val bankId by rhythmVm.bankId.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                        }
                        val favorites by rhythmVm.favorites.collectAsStateWithLifecycle()
                        val transcription by rhythmVm.transcription.collectAsStateWithLifecycle()
                        SynthScreen(
                            patch = project.synth,
                            octave = octave,
                            actions = SynthActions(
                                noteOn = rhythmVm::synthNoteOn,
                                noteOff = rhythmVm::synthNoteOff,
                                shiftOctave = rhythmVm::shiftKeyboardOctave,
                                setParam = rhythmVm::setSynthParam,
                                applyPreset = rhythmVm::applySynthPreset,
                                chordOn = rhythmVm::synthChordOn,
                                chordOff = rhythmVm::synthChordOff,
                            ),
                            notes = NotesUi(
                                clip = project.synthNotes,
                                ghosts = NoteClip.ghostsOf(project.acid, project.synthNotes.steps),
                                playStep = live.absoluteStep,
                                actions = PianoRollActions(
                                    add = rhythmVm::addNote,
                                    remove = rhythmVm::removeNote,
                                    change = rhythmVm::changeNote,
                                    quantize = rhythmVm::quantizeNotes,
                                    setSteps = rhythmVm::setNoteClipSteps,
                                    clear = rhythmVm::clearNotes,
                                    addChord = rhythmVm::addChord,
                                    setHarmony = rhythmVm::setHarmony,
                                    applyProgression = rhythmVm::applyProgression,
                                    setPerformance = rhythmVm::setPerformance,
                                ),
                                harmony = project.harmony,
                            ),
                            instrument = InstrumentUi(
                                library = library,
                                current = project.instrument,
                                open = rhythmVm::openLibrary,
                                download = rhythmVm::downloadLibrary,
                                cancel = rhythmVm::cancelLibraryDownload,
                                select = rhythmVm::selectInstrument,
                                bankId = bankId,
                                selectBank = rhythmVm::selectBank,
                                loadKit = rhythmVm::loadKit,
                                synthEngine = project.synthEngine,
                                plaits = project.plaits,
                                selectEngine = rhythmVm::selectSynthEngine,
                                setPlaitsModel = rhythmVm::setPlaitsModel,
                                setPlaitsParam = rhythmVm::setPlaitsParam,
                            ),
                            transcribe = TranscribeUi(
                                task = transcription,
                                pickFile = { rhythmVm.dismissTranscription(); pickNotesAudio.launch(arrayOf("audio/*")) },
                                record = {
                                    rhythmVm.dismissTranscription()
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) rhythmVm.recordNotes()
                                    else notesMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                stop = rhythmVm::stopNoteRecording,
                            ),
                            browser = if (project.synthEngine == 1) {
                                SoundBrowserUi(
                                    SoundCategories.PLAITS, SoundLibrary.PLAITS, favorites.plaits,
                                    current = (SoundLibrary.PLAITS + favorites.plaits).firstOrNull { it.sound == project.plaits.copy(level = it.sound.level) }?.name,
                                    pick = { rhythmVm.applyPlaits(SoundLibrary.PLAITS[it].sound.copy(level = project.plaits.level)) },
                                    pickFavorite = { rhythmVm.applyPlaits(favorites.plaits[it].sound) },
                                    deleteFavorite = rhythmVm::deletePlaitsFavorite,
                                    random = rhythmVm::randomPlaits,
                                    save = rhythmVm::savePlaitsFavorite,
                                    tag = "plaits_sounds",
                                )
                            } else {
                                SoundBrowserUi(
                                    SoundCategories.SYNTH, SoundLibrary.SYNTH, favorites.synth,
                                    current = (SoundLibrary.SYNTH + favorites.synth).firstOrNull { it.sound.sanitized() == project.synth.sanitized() }?.name,
                                    pick = { rhythmVm.applySynthPreset(SoundLibrary.SYNTH[it].sound) },
                                    pickFavorite = { rhythmVm.applySynthPreset(favorites.synth[it].sound) },
                                    deleteFavorite = rhythmVm::deleteSynthFavorite,
                                    random = rhythmVm::randomSynth,
                                    save = rhythmVm::saveSynthFavorite,
                                    tag = "synth_sounds",
                                )
                            },
                        )
                    }
                    AppTab.Mixeur -> {
                        val project by rhythmVm.project.collectAsStateWithLifecycle()
                        val meters by rhythmVm.meters.collectAsStateWithLifecycle()
                        val export by rhythmVm.export.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollMeters() }
                        }
                        val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                        val live by rhythmVm.live.collectAsStateWithLifecycle()
                        val automationRecord by rhythmVm.automationRecord.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                        }
                        MixerScreen(
                            channels = project.mixer.channels,
                            meters = meters,
                            actions = MixerActions(
                                setVolume = rhythmVm::setChannelVolume,
                                setPan = rhythmVm::setChannelPan,
                                toggleMute = rhythmVm::toggleMute,
                                toggleSolo = rhythmVm::toggleSolo,
                                setExportBars = rhythmVm::setExportBars,
                                export = rhythmVm::exportWav,
                                cancelExport = rhythmVm::cancelExport,
                                share = { export.file?.let { WavExporter.share(context, it) } },
                                setInsertType = rhythmVm::setInsertType,
                                setInsertParam = rhythmVm::setInsertParam,
                                setSend = rhythmVm::setSend,
                                setBusParam = rhythmVm::setBusParam,
                            ),
                            master = project.mixer.master,
                            buses = project.mixer.buses,
                            automation = AutomationUi(project.automation, project.drums.steps, if (transport.playing) live.currentStep else -1, automationRecord),
                            automationActions = AutomationActions(
                                add = rhythmVm::addAutomation,
                                remove = rhythmVm::removeAutomation,
                                clear = rhythmVm::clearAutomation,
                                setPoint = rhythmVm::setAutomationPoint,
                                toggleRecord = rhythmVm::toggleAutomationRecord,
                            ),
                            export = export,
                        )
                    }
                    AppTab.Morceau -> {
                        val project by rhythmVm.project.collectAsStateWithLifecycle()
                        val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                        val sessionUi by rhythmVm.sessionUi.collectAsStateWithLifecycle()
                        val liveStates by rhythmVm.liveStates.collectAsStateWithLifecycle()
                        val sampleTask by rhythmVm.sampleTask.collectAsStateWithLifecycle()
                        var liveEditing by rememberSaveable { mutableStateOf<Int?>(null) }
                        LaunchedEffect(Unit) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLiveStates() }
                        }
                        val all = project.allScenes
                        SessionScreen(
                            SessionScreenUi(
                                scenes = List(Scene.MAX) { all.getOrNull(it) },
                                current = project.scene,
                                pending = sessionUi.pending,
                                song = project.song,
                                songIndex = sessionUi.songIndex,
                                playing = transport.playing,
                                tempoBpm = project.tempoBpm,
                            ),
                            SessionActions(
                                select = rhythmVm::selectScene,
                                duplicate = rhythmVm::duplicateScene,
                                addPart = rhythmVm::addSongPart,
                                removePart = rhythmVm::removeSongPart,
                                setRepeats = rhythmVm::setSongRepeats,
                                toggleSong = rhythmVm::toggleSong,
                                togglePlay = rhythmVm::togglePlay,
                                performance = { param, v -> if (NativeEngine.isAvailable) NativeEngine.setPerformance(param, v) },
                            ),
                            live = LivePadsUi(
                                pads = project.live,
                                states = liveStates,
                                editing = liveEditing,
                                drumSounds = project.drums.lanes.mapIndexedNotNull { i, l -> l.sample?.let { i to it.name } },
                                busy = when (val t = sampleTask) {
                                    is SampleTask.Busy -> t.message
                                    is SampleTask.Failed -> t.message
                                    else -> null
                                },
                            ),
                            liveActions = LivePadsActions(
                                tap = rhythmVm::toggleLive,
                                stopAll = rhythmVm::stopLive,
                                setSize = rhythmVm::setLiveSize,
                                edit = { liveEditing = it },
                                import = { pad -> liveImportPad = pad; rhythmVm.dismissSampleError(); pickLiveAudio.launch(arrayOf("audio/*")) },
                                fromDrumPad = rhythmVm::liveFromDrumPad,
                                setBeats = rhythmVm::setLiveBeats,
                                setOneShot = rhythmVm::setLiveOneShot,
                                setGain = rhythmVm::setLiveGain,
                                clear = rhythmVm::clearLive,
                            ),
                        )
                    }
                    AppTab.Remix -> {
                        val remixState by remixVm.state.collectAsStateWithLifecycle()
                        val sets by remixVm.sets.collectAsStateWithLifecycle()
                        val modelInstalled by remixVm.modelInstalled.collectAsStateWithLifecycle()
                        val preview by remixVm.preview.collectAsStateWithLifecycle()
                        val remixLoading by rhythmVm.remixLoading.collectAsStateWithLifecycle()
                        val remixStretching by rhythmVm.remixStretching.collectAsStateWithLifecycle()
                        val overview by rhythmVm.stemOverview.collectAsStateWithLifecycle()
                        val stemPosition by rhythmVm.stemPosition.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollStemPosition() }
                        }
                        LaunchedEffect(Unit) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { remixVm.pollPreview() }
                        }
                        val project by rhythmVm.project.collectAsStateWithLifecycle()
                        val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                        val sampleTask by rhythmVm.sampleTask.collectAsStateWithLifecycle()
                        RemixScreen(
                            RemixUi(
                                modelInstalled, remixState, sets, preview,
                                remix = project.remix,
                                remixLoading = remixLoading,
                                stretching = remixStretching,
                                transportPlaying = transport.playing,
                                projectBpm = project.tempoBpm,
                                padTask = when (val t = sampleTask) {
                                    is SampleTask.Busy -> t.message
                                    is SampleTask.Failed -> t.message
                                    else -> null
                                },
                                overview = overview,
                                position = stemPosition,
                            ),
                            RemixActions(
                                downloadModel = remixVm::downloadModel,
                                pickSong = { pickSong.launch(arrayOf("audio/*")) },
                                cancel = remixVm::cancel,
                                dismiss = remixVm::dismiss,
                                playAll = remixVm::playAll,
                                toggleStem = remixVm::toggleStem,
                                pause = remixVm::pause,
                                stemToLive = rhythmVm::stemToLive,
                                setFollowTempo = rhythmVm::setRemixFollowTempo,
                                delete = remixVm::delete,
                                useSong = { set ->
                                    rhythmVm.useSong(set)
                                    val what = listOfNotNull(set.info.bpm.takeIf { it > 0 }?.let { "%.0f BPM".format(it) }, set.info.keyLabel)
                                    if (what.isNotEmpty()) {
                                        android.widget.Toast.makeText(context, "Projet calé : ${what.joinToString(", ")}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                clearRemix = rhythmVm::clearRemix,
                                togglePlay = rhythmVm::togglePlay,
                                setStartBar = rhythmVm::setRemixStartBar,
                                setStemVolume = rhythmVm::setStemVolume,
                                toggleStemMute = rhythmVm::toggleStemMute,
                                toggleStemSolo = rhythmVm::toggleStemSolo,
                                stemToPad = rhythmVm::stemToPad,
                                setStartAt = rhythmVm::setRemixStartAt,
                            ),
                        )
                    }
                    AppTab.Outils -> Column {
                        TabRow(selectedTabIndex = toolPage.ordinal, containerColor = Neon.Surface, contentColor = Neon.Cyan) {
                            ToolPage.entries.forEach { page ->
                                Tab(
                                    selected = toolPage == page,
                                    onClick = { toolPage = page },
                                    text = { Text(page.label) },
                                    modifier = Modifier.testTag("tool_${page.name}"),
                                )
                            }
                        }
                        when (toolPage) {
                            ToolPage.Recette -> {
                                val state by recetteVm.state.collectAsStateWithLifecycle()
                                RecetteScreen(
                                    state = state,
                                    stream = stream,
                                    roundTripMs = roundTrip,
                                    onUpdate = recetteVm::update,
                                    onSendReport = { recetteVm.buildReport { openUri(context, it) } },
                                    onReportProblem = { text -> recetteVm.buildProblem(text) { openUri(context, it) } },
                                )
                            }
                            ToolPage.Midi -> {
                                val midi by midiVm.state.collectAsStateWithLifecycle()
                                MidiScreen(
                                    midi,
                                    MidiActions(
                                        connect = midiVm::connect,
                                        disconnect = midiVm::disconnect,
                                        scanBluetooth = { bluetoothPermission.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)) },
                                        setFollowClock = midiVm::setFollowClock,
                                    ),
                                )
                            }
                            ToolPage.Fond -> BackgroundPage()
                            ToolPage.Latence -> {
                                val state by latencyVm.state.collectAsStateWithLifecycle()
                                LatencyScreen(
                                    state = state,
                                    stream = stream,
                                    lastRoundTripMs = roundTrip,
                                    engineAvailable = engineAvailable,
                                    onMeasure = {
                                        if (hasMic(context)) latencyVm.measure()
                                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                )
                            }
                            ToolPage.Son -> {
                                val state by studioVm.state.collectAsStateWithLifecycle()
                                StudioScreen(
                                    state = state,
                                    stream = stream,
                                    engineAvailable = engineAvailable,
                                    onTogglePlay = studioVm::togglePlaying,
                                    onFrequency = studioVm::setFrequency,
                                    onVolume = studioVm::setVolume,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchPrompts(mainVm)
}

@Composable
private fun LaunchPrompts(vm: MainViewModel) {
    val context = LocalContext.current
    val crash by vm.crash.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()

    if (crash != null) {
        AlertDialog(
            onDismissRequest = vm::dismissCrash,
            title = { Text("Aïe, l'appli est tombée") },
            text = { Text("Elle s'est fermée brutalement la dernière fois. Envoyer le rapport ? Il est déjà rédigé, tu n'as qu'à appuyer sur Envoyer.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = vm.crashReportUri()
                    vm.dismissCrash()
                    uri?.let { openUri(context, it) }
                }) { Text("Envoyer le rapport") }
            },
            dismissButton = { TextButton(onClick = vm::dismissCrash) { Text("Ignorer") } },
        )
        return
    }

    when (val u = update) {
        is UpdateState.Available, is UpdateState.Failed -> {
            val info = if (u is UpdateState.Available) u.info else (u as UpdateState.Failed).info
            AlertDialog(
                onDismissRequest = vm::dismissUpdate,
                title = { Text("Nouvelle version ${info.versionName}") },
                text = {
                    Column {
                        if (u is UpdateState.Failed) Text("Le téléchargement a échoué, on retente ?", color = Neon.Red)
                        Text(info.notes.ifBlank { "Une nouvelle version est prête." })
                    }
                },
                confirmButton = { TextButton(onClick = vm::installUpdate) { Text("Installer") } },
                dismissButton = { TextButton(onClick = vm::dismissUpdate) { Text("Plus tard") } },
            )
        }
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Téléchargement…") },
            text = { LinearProgressIndicator(progress = { u.progress }) },
            confirmButton = {},
        )
        UpdateState.None -> Unit
    }
}

private fun hasMic(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun openUri(context: Context, uri: Uri) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Aucun navigateur pour ouvrir GitHub", Toast.LENGTH_LONG).show()
    }
}
