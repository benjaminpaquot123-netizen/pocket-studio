package com.pocketstudio.app.ui

import com.pocketstudio.model.track
import kotlin.math.roundToInt
import com.pocketstudio.app.instruments.InstrumentUi
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.app.audiotracks.AudioTracksActions
import com.pocketstudio.app.audiotracks.AudioTracksScreen
import com.pocketstudio.app.audiotracks.AudioTracksUi
import com.pocketstudio.model.viewOfTrack
import com.pocketstudio.model.songBars
import com.pocketstudio.model.sceneSteps
import androidx.compose.runtime.remember
import com.pocketstudio.app.synth.TracksUi
import com.pocketstudio.app.synth.NotesUi
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.model.NoteClip
import com.pocketstudio.app.report.AppLog
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
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
import com.pocketstudio.app.settings.GuideDialog
import com.pocketstudio.app.AppInfo
import com.pocketstudio.app.settings.SettingsActions
import com.pocketstudio.app.settings.SettingsScreen
import com.pocketstudio.app.settings.SettingsStore
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Arrangement
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.ProjectStarter
import com.pocketstudio.app.projects.ProjectsActions
import com.pocketstudio.app.projects.ProjectsScreen
import com.pocketstudio.app.projects.ProjectsViewModel
import com.pocketstudio.model.Project
import com.pocketstudio.app.synth.TranscribeUi
import com.pocketstudio.app.midi.MidiActions
import com.pocketstudio.app.midi.MidiCommand
import com.pocketstudio.app.midi.MidiScreen
import com.pocketstudio.app.midi.MidiViewModel
import com.pocketstudio.app.sounds.SoundBrowserUi
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.model.AcidSound
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.model.SynthPatch
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
    Audio("Audio", Icons.Filled.Mic),
    Morceau("Morceau", Icons.Filled.QueueMusic),
    Outils("Outils", Icons.Filled.Tune),
}

private enum class ToolPage(val label: String) { Reglages("Réglages"), Recette("Recette"), Midi("MIDI"), Fond("Fond"), Latence("Latence"), Son("Son") }

/** Ways to start a new project (Mes projets › Nouveau projet). */
private val PROJECT_STARTERS: List<Pair<ProjectStarter, () -> Project>> = listOf(
    ProjectStarter("Projet vide", "Tout est vierge : à toi de jouer.", "empty") to { Project(name = "") },
    ProjectStarter("Rythme de départ", "Un rythme et une ligne d'acide prêts à tourner.", "starter") to { RhythmViewModel.starterProject("") },
) + com.pocketstudio.model.Genres.ALL.mapIndexed { i, (title, description, build) ->
    ProjectStarter("Style · $title", description, "genre_$i") to build
} + com.pocketstudio.model.Demos.ALL.mapIndexed { i, (title, description, build) ->
    ProjectStarter(title, description, "demo_$i") to build
}

private tailrec fun android.content.Context.findActivity(): androidx.activity.ComponentActivity? = when (this) {
    is androidx.activity.ComponentActivity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

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
    val generateVm: com.pocketstudio.app.generate.GenerateViewModel = viewModel()
    val libraryVm: com.pocketstudio.app.library.SoundLibraryViewModel = viewModel()
    var tab by rememberSaveable { mutableStateOf(AppTab.Rythme) }
    var showProjects by rememberSaveable { mutableStateOf(false) }
    val projectsVm: ProjectsViewModel = viewModel()
    val pickProjectZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) projectsVm.importZip(uri)
    }
    val openScope = rememberCoroutineScope()
    // Another project: save this one, remember the choice, then rebuild every screen on it.
    val openProject: (String) -> Unit = { id ->
        openScope.launch {
            rhythmVm.keepTake()  // a microphone take in progress lands on its track first
            rhythmVm.flushNow()
            showProjects = false  // remembered across the rebuild: close it first
            CurrentProject.set(context, id)
            AppLog.add("Ouverture d'un autre projet")
            context.findActivity()?.let { activity ->
                activity.viewModelStore.clear()
                activity.recreate()
            }
        }
    }
    // The sound library, for the selected drum pad or for a live pad (libraryLive >= 0).
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    var libraryLive by rememberSaveable { mutableIntStateOf(-1) }
    // ... or for the selected instrument track's sampled sound.
    var libraryTrack by rememberSaveable { mutableStateOf(false) }
    // Bumped at every close: a sound still loading when the library closed lands nowhere.
    var libraryToken by remember { mutableIntStateOf(0) }
    if (showLibrary) {
        val packsUi by libraryVm.packs.collectAsStateWithLifecycle()
        val previewing by libraryVm.playing.collectAsStateWithLifecycle()
        val libTransport by rhythmVm.transport.collectAsStateWithLifecycle()
        val lane = libTransport.selectedPad
        LaunchedEffect(Unit) {
            libraryVm.dismissMessage()
            libraryVm.refreshPacks()
        }
        val sizes by androidx.compose.runtime.produceState(emptyMap<String, Long>(), packsUi.installed) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                packsUi.installed.associate { it.id to com.pocketstudio.app.library.PackStore.bytesOnDisk(context, it.id) }
            }
        }
        val close = { libraryVm.stopPreview(); showLibrary = false; libraryLive = -1; libraryTrack = false; libraryToken += 1 }
        val live = libraryLive
        val forTrack = libraryTrack
        val libProject by rhythmVm.project.collectAsStateWithLifecycle()
        val libTrack by rhythmVm.selectedTrack.collectAsStateWithLifecycle()
        androidx.compose.ui.window.Dialog(
            onDismissRequest = close,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            androidx.compose.material3.Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                com.pocketstudio.app.library.LibraryScreen(
                    com.pocketstudio.app.library.LibraryUi(
                        packsUi, previewing,
                        target = when {
                            forTrack -> "la piste « ${libProject.track(libTrack).name} »"
                            live >= 0 -> "le pad live ${live + 1}"
                            else -> "le pad ${lane + 1} (${com.pocketstudio.app.rhythm.DrumKitInfo.names[lane]})"
                        },
                        startKind = if (forTrack) "808" else if (live >= 0) "stab" else padKind(lane),
                        sizes = sizes,
                        songPlaying = libTransport.playing,
                    ),
                    com.pocketstudio.app.library.LibraryActions(
                        close = close,
                        togglePlay = rhythmVm::togglePlay,
                        preview = libraryVm::preview,
                        use = { option ->
                            val token = libraryToken
                            libraryVm.use(option.ref) { pcm ->
                                if (token != libraryToken) return@use
                                val name = option.name.substringBefore(" · ")
                                val placed = when {
                                    forTrack -> rhythmVm.useAsTrackSound(pcm, name, (option.ref as? com.pocketstudio.app.library.SoundRef.Pack)?.sound?.kind)
                                    live >= 0 -> rhythmVm.placeOnLivePad(live, pcm, name)
                                    else -> rhythmVm.placeOnPad(pcm, name, oneShot = true)
                                }
                                if (placed) close()
                                else libraryVm.notice("Un son est encore en train d'être posé ou enregistré : réessaie dans un instant.")
                            }
                        },
                        download = libraryVm::downloadPack,
                        cancelDownload = libraryVm::cancelDownload,
                        deletePack = libraryVm::deletePack,
                    ),
                )
            }
        }
    }
    // Quick effects of one mixer channel, opened from the synth, drums or audio tracks (-1: closed).
    var fxChannel by rememberSaveable { mutableStateOf(-1) }
    if (fxChannel >= 0) {
        val fxProject by rhythmVm.project.collectAsStateWithLifecycle()
        val channel = fxProject.mixer.channels.getOrNull(fxChannel)
        if (channel != null) {
            val name = if (fxChannel >= com.pocketstudio.model.Mixer.FIRST_PAD) {
                val lane = fxChannel - com.pocketstudio.model.Mixer.FIRST_PAD
                "pad ${lane + 1} (${fxProject.drums.lanes.getOrNull(lane)?.sample?.name ?: com.pocketstudio.app.rhythm.DrumKitInfo.names[lane]})"
            } else {
                mixerStrips(fxProject).firstOrNull { it.ch == fxChannel }?.name ?: "Piste"
            }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { fxChannel = -1 },
                // The whole width of the phone: everything seen at once, nothing to scroll for.
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                title = { Text("Effets · $name") },
                text = {
                    Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                        com.pocketstudio.app.mixer.QuickFxPanel(
                            fxChannel, channel,
                            com.pocketstudio.app.mixer.QuickFxActions(rhythmVm::setMacro, rhythmVm::setMacroPair, rhythmVm::applyFx),
                        )
                        com.pocketstudio.app.mixer.DetailedFx(
                            fxChannel, channel,
                            MixerActions(setInsertType = rhythmVm::setInsertType, setInsertParam = rhythmVm::setInsertParam, setSend = rhythmVm::setSend),
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { fxChannel = -1 }, modifier = Modifier.testTag("fx_close")) { Text("Fermer") } },
            )
        }
    }
    val wizardUi by libraryVm.wizard.collectAsStateWithLifecycle()
    if (wizardUi.open) {
        val packsUi by libraryVm.packs.collectAsStateWithLifecycle()
        val previewing by libraryVm.playing.collectAsStateWithLifecycle()
        val preparing by libraryVm.preparing.collectAsStateWithLifecycle()
        androidx.compose.ui.window.Dialog(
            onDismissRequest = libraryVm::closeWizard,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            androidx.activity.compose.BackHandler { if (wizardUi.step > 0) libraryVm.back() else libraryVm.closeWizard() }
            androidx.compose.material3.Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                com.pocketstudio.app.library.WizardScreen(
                    wizardUi, packsUi, previewing,
                    com.pocketstudio.app.library.WizardActions(
                        close = libraryVm::closeWizard,
                        chooseStyle = libraryVm::chooseStyle,
                        next = libraryVm::next,
                        back = libraryVm::back,
                        setEnabled = libraryVm::setRowEnabled,
                        setCategory = libraryVm::setCategory,
                        choose = libraryVm::choose,
                        setMood = libraryVm::setMood,
                        setPartEnabled = libraryVm::setPartEnabled,
                        choosePartSound = libraryVm::choosePartSound,
                        preview = libraryVm::preview,
                        download = libraryVm::downloadPack,
                        cancelDownload = libraryVm::cancelDownload,
                        deletePack = libraryVm::deletePack,
                        create = { libraryVm.create { project, sounds -> projectsVm.create(project, sounds) { id -> libraryVm.closeWizard(); openProject(id) } } },
                    ),
                    preparing = preparing,
                )
            }
        }
    }
    if (showProjects) {
        val projectsUi by projectsVm.state.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { projectsVm.refresh() }
        androidx.activity.compose.BackHandler { showProjects = false }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showProjects = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            androidx.compose.material3.Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                ProjectsScreen(
                    projectsUi, rhythmVm.projectId, PROJECT_STARTERS.map { it.first },
                    ProjectsActions(
                        back = { showProjects = false },
                        open = { openProject(it.id) },
                        create = { i -> projectsVm.create(PROJECT_STARTERS[i].second()) { id -> openProject(id) } },
                        wizard = {
                            rhythmVm.stop()
                            libraryVm.openWizard()
                        },
                        rename = { p, name -> if (p.id == rhythmVm.projectId) { rhythmVm.renameProject(name); projectsVm.refresh() } else projectsVm.rename(p.id, name) },
                        duplicate = { p -> openScope.launch { if (p.id == rhythmVm.projectId) rhythmVm.flushNow(); projectsVm.duplicate(p) } },
                        exportZip = { p ->
                            openScope.launch {
                                if (p.id == rhythmVm.projectId) rhythmVm.flushNow()
                                projectsVm.exportZip(p) { file -> WavExporter.share(context, listOf(file), "Envoyer le projet") }
                            }
                        },
                        importZip = { pickProjectZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                        delete = projectsVm::delete,
                        undoDelete = projectsVm::undoDelete,
                        dismissDeleted = projectsVm::dismissDeleted,
                        dismissMessage = projectsVm::dismissMessage,
                    ),
                )
            }
        }
    }
    var toolPage by rememberSaveable { mutableStateOf(ToolPage.Reglages) }
    val settings by SettingsStore.state.collectAsStateWithLifecycle()
    var showGuide by rememberSaveable { mutableStateOf(false) }
    if (showGuide || !settings.guideSeen) {
        GuideDialog(onClose = {
            showGuide = false
            SettingsStore.update(context) { it.copy(guideSeen = true) }
        })
    }
    // Réglages › Écran toujours allumé: only while the music plays.
    val screenTransport by rhythmVm.transport.collectAsStateWithLifecycle()
    LaunchedEffect(screenTransport.playing, settings.keepScreenOn) {
        val window = context.findActivity()?.window ?: return@LaunchedEffect
        val flag = android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (screenTransport.playing && settings.keepScreenOn) window.addFlags(flag) else window.clearFlags(flag)
    }
    // Engine-side settings follow the Réglages screen at once.
    LaunchedEffect(settings.metronomeLevel) {
        if (NativeEngine.isAvailable) NativeEngine.setMetronomeLevel(settings.metronomeLevel)
    }
    LaunchedEffect(settings.bufferBursts) {
        if (NativeEngine.isAvailable) NativeEngine.setBufferBursts(settings.bufferBursts)
    }

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
            rhythmVm.engineStarted()
            NativeEngine.setMetronomeLevel(SettingsStore.state.value.metronomeLevel)
            NativeEngine.setBufferBursts(SettingsStore.state.value.bufferBursts)
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
            dismissButton = if (rhythmVm.canUndoFavoriteDelete) {
                { TextButton(onClick = rhythmVm::undoFavoriteDelete, modifier = Modifier.testTag("favorite_undo")) { Text("Annuler") } }
            } else null,
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
    // The "REC" live activity is a notification: without the permission (Android 13+), the phone
    // hides it. Asked once per launch, at the first recording, where it makes sense.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var notificationsAsked by rememberSaveable { mutableStateOf(false) }
    val anyRecording by rhythmVm.recording.collectAsStateWithLifecycle()
    val anyTake by rhythmVm.audioRecording.collectAsStateWithLifecycle()
    LaunchedEffect(anyRecording || anyTake != null) {
        if (!(anyRecording || anyTake != null) || notificationsAsked || android.os.Build.VERSION.SDK_INT < 33) return@LaunchedEffect
        notificationsAsked = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
                is MidiCommand.Note -> rhythmVm.midiNote(c.channel, c.pitch, c.velocity, c.on)
                is MidiCommand.Control -> rhythmVm.midiControl(c.channel, c.controller, c.value)
            }
        }
    }
    // Scale lock reaches the MIDI keyboard too.
    LaunchedEffect(Unit) {
        rhythmVm.project.collect { midiVm.harmony = it.harmony }
    }
    // Clock out: the connected MIDI device follows our transport and tempo.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.flow.combine(rhythmVm.transport, rhythmVm.project) { t, p -> t.playing to p.tempoBpm }
            .collect { (playing, bpm) -> midiVm.transport(playing, bpm) }
    }
    val pickMidiFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importMidi(uri)
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
    val pickKeyboardSound = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importTrackSound(uri)
    }
    var liveImportPad by rememberSaveable { mutableIntStateOf(0) }
    var audioPage by rememberSaveable { mutableIntStateOf(0) }
    var analyserOn by rememberSaveable { mutableStateOf(false) }
    var audioRecordTrack by rememberSaveable { mutableIntStateOf(0) }
    var audioImportTrack by rememberSaveable { mutableIntStateOf(0) }
    val audioMicPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) rhythmVm.recordAudio(audioRecordTrack)
    }
    val pickTrackAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importAudio(audioImportTrack, uri)
    }
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
            val topProject by rhythmVm.project.collectAsStateWithLifecycle()
            val topTransport by rhythmVm.transport.collectAsStateWithLifecycle()
            val topUndo by rhythmVm.session.canUndo.collectAsStateWithLifecycle()
            val topRedo by rhythmVm.session.canRedo.collectAsStateWithLifecycle()
            val topRecording by rhythmVm.recording.collectAsStateWithLifecycle()
            val topTake by rhythmVm.audioRecording.collectAsStateWithLifecycle()
            val topSaving by rhythmVm.takeSaving.collectAsStateWithLifecycle()
            // No undo during a take (or while it is written): it could remove the track it goes to.
            val undoable = !topRecording && topTake == null && !topSaving
            TopBar(
                TopBarUi(topProject.name, topProject.tempoBpm, topTransport.playing, topUndo && undoable, topRedo && undoable, topRecording),
                TopBarActions(
                    projects = { showProjects = true }, undo = rhythmVm::undo, redo = rhythmVm::redo,
                    togglePlay = rhythmVm::togglePlay, toggleRecord = rhythmVm::toggleRecord,
                ),
            )
            if (saveError != null) {
                Text(
                    "Sauvegarde impossible (mémoire pleine ?). Tes dernières modifications ne sont pas encore enregistrées.",
                    color = Color.Black,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().background(Neon.Amber).padding(8.dp)
                        .testTag("save_error"),
                )
            }
        },
        bottomBar = {
            if (!landscape) {
                NavigationBar(
                    containerColor = lerp(Neon.Background, Color.Black, 0.12f),
                    modifier = Modifier.drawBehind { drawLine(Color.White.copy(alpha = 0.07f), Offset.Zero, Offset(size.width, 0f), 1f) },
                ) {
                    AppTab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("tab_${t.name}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Neon.OnAccent,
                                selectedTextColor = Neon.Cyan,
                                indicatorColor = Neon.Cyan,
                                unselectedIconColor = Neon.TextSecondary,
                                unselectedTextColor = Neon.TextSecondary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        // The background darkens a little towards the bottom: the panels seem to float above it.
        val bg = Neon.Background
        Row(Modifier.padding(padding).background(Brush.verticalGradient(listOf(lerp(bg, Color.White, 0.03f), bg, lerp(bg, Color.Black, 0.18f))))) {
            // Landscape: tabs on the side so the screen keeps its height (wider synth keyboard).
            if (landscape) {
                NavigationRail(containerColor = lerp(Neon.Background, Color.Black, 0.12f)) {
                    AppTab.entries.forEach { t ->
                        NavigationRailItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                            modifier = Modifier.testTag("tab_${t.name}"),
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Neon.OnAccent,
                                selectedTextColor = Neon.Cyan,
                                indicatorColor = Neon.Cyan,
                                unselectedIconColor = Neon.TextSecondary,
                                unselectedTextColor = Neon.TextSecondary,
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
                        val take by rhythmVm.audioRecording.collectAsStateWithLifecycle()
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
                            canUndo = canUndo && take == null,
                            canRedo = canRedo && take == null,
                            actions = RhythmActions(
                                togglePlay = rhythmVm::togglePlay,
                                toggleMetronome = rhythmVm::toggleMetronome,
                                toggleCountIn = rhythmVm::toggleCountIn,
                                hitPad = rhythmVm::hitPad,
                                toggleStep = rhythmVm::toggleStep,
                                setBars = rhythmVm::setDrumBars,
                                fill = rhythmVm::fillPad,
                                rotate = rhythmVm::rotatePad,
                                cycleRoll = rhythmVm::cycleRoll,
                                openEffects = { fxChannel = 0 },
                                openPadEffects = { fxChannel = com.pocketstudio.model.Mixer.padChannel(rhythmVm.transport.value.selectedPad) },
                                openLibrary = { showLibrary = true },
                                toKeyboard = { if (rhythmVm.padToKeyboard()) tab = AppTab.Synthe },
                                setPadPan = rhythmVm::setPadPan,
                                setHitPan = rhythmVm::setHitPan,
                                setHitPitch = rhythmVm::setHitPitch,
                                toggleHit = rhythmVm::toggleHit,
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
                                describe = { (it.sound as? AcidSound)?.let(SoundLooks::describe) },
                            ),
                        )
                    }
                    AppTab.Synthe -> {
                        DisposableEffect(Unit) { onDispose { rhythmVm.synthAllOff() } }
                        val fullProject by rhythmVm.project.collectAsStateWithLifecycle()
                        val selectedTrack by rhythmVm.selectedTrack.collectAsStateWithLifecycle()
                        // The selected instrument track's sound and notes, where the synth screen looks.
                        val project = remember(fullProject, selectedTrack) { fullProject.viewOfTrack(selectedTrack) }
                        val shownTrack = if (fullProject.tracks.any { it.id == selectedTrack }) selectedTrack else 0
                        val playUi by rhythmVm.play.collectAsStateWithLifecycle()
                        val octave by rhythmVm.keyboardOctave.collectAsStateWithLifecycle()
                        val live by rhythmVm.live.collectAsStateWithLifecycle()
                        val library by rhythmVm.library.collectAsStateWithLifecycle()
                        val trackSoundMessage by rhythmVm.trackSoundBusy.collectAsStateWithLifecycle()
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
                                    applyBassLine = rhythmVm::applyBassLine,
                                    applyArpeggio = rhythmVm::applyArpeggio,
                                    setPerformance = rhythmVm::setPerformance,
                                    replaceClip = rhythmVm::replaceNoteClip,
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
                                sampled = project.sampled,
                                chooseSample = { rhythmVm.dismissTrackSoundMessage(); libraryTrack = true; showLibrary = true },
                                importSample = { rhythmVm.dismissTrackSoundMessage(); pickKeyboardSound.launch(arrayOf("audio/*")) },
                                editSampled = rhythmVm::editSampled,
                                sampledMessage = trackSoundMessage,
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
                                pickMidi = { rhythmVm.dismissTranscription(); pickMidiFile.launch(arrayOf("audio/midi", "audio/x-midi", "application/x-midi", "application/octet-stream")) },
                            ),
                            play = playUi,
                            playActions = com.pocketstudio.app.synth.PlayActions(
                                setBend = rhythmVm::setBend,
                                setMod = rhythmVm::setMod,
                                setSustain = rhythmVm::setSustain,
                                setArp = rhythmVm::setArp,
                            ),
                            tracks = TracksUi(
                                tracks = fullProject.instrumentTracks,
                                selected = shownTrack,
                                select = rhythmVm::selectTrack,
                                add = rhythmVm::addTrack,
                                rename = rhythmVm::renameTrack,
                                setColor = rhythmVm::setTrackColor,
                                duplicate = rhythmVm::duplicateTrack,
                                remove = rhythmVm::removeTrack,
                                effects = { id -> fxChannel = com.pocketstudio.model.InstrumentTrack.mixerChannel(id) },
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
                                    describe = { (it.sound as? PlaitsPatch)?.let(SoundLooks::describe) },
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
                                    describe = { (it.sound as? SynthPatch)?.let(SoundLooks::describe) },
                                )
                            },
                        )
                    }
                    AppTab.Mixeur -> {
                        val project by rhythmVm.project.collectAsStateWithLifecycle()
                        val sessionUi by rhythmVm.sessionUi.collectAsStateWithLifecycle()
                        val spectrum by rhythmVm.spectrum.collectAsStateWithLifecycle()
                        LaunchedEffect(analyserOn) {
                            if (analyserOn) lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollSpectrum() }
                        }
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
                                share = { WavExporter.share(context, export.files) },
                                setExportWhat = rhythmVm::setExportWhat,
                                setExportFormat = rhythmVm::setExportFormat,
                                exportMidi = rhythmVm::exportMidi,
                                setInsertType = rhythmVm::setInsertType,
                                setInsertParam = rhythmVm::setInsertParam,
                                setSend = rhythmVm::setSend,
                                setBusParam = rhythmVm::setBusParam,
                                quick = com.pocketstudio.app.mixer.QuickFxActions(rhythmVm::setMacro, rhythmVm::setMacroPair, rhythmVm::applyFx),
                            ),
                            master = project.mixer.master,
                            buses = project.mixer.buses,
                            strips = mixerStrips(project),
                            spectrum = spectrum,
                            analyserOpen = analyserOn,
                            setAnalyser = { analyserOn = it },
                            automation = AutomationUi(
                                project.automation, project.drums.steps, if (transport.playing) live.currentStep else -1, automationRecord,
                                scene = project.scene,
                                songBars = project.songBars,
                                songBar = if (transport.playing && live.songStep >= 0) live.songStep / project.stepsPerBar else -1,
                                stepsPerBar = project.stepsPerBar,
                            ),
                            automationActions = AutomationActions(
                                add = rhythmVm::addAutomation,
                                remove = rhythmVm::removeAutomation,
                                clear = rhythmVm::clearAutomation,
                                setPoint = rhythmVm::setAutomationPoint,
                                toggleRecord = rhythmVm::toggleAutomationRecord,
                                setMode = rhythmVm::setAutomationMode,
                                setSongPoint = rhythmVm::setAutomationSongPoint,
                            ),
                            export = export,
                            hasSong = project.song.isNotEmpty(),
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
                                beatsPerBar = project.beatsPerBar,
                                rows = timelineRows(project),
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
                                togglePartTrack = rhythmVm::togglePartTrack,
                                setBeatsPerBar = rhythmVm::setBeatsPerBar,
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
                                library = { pad -> rhythmVm.dismissSampleError(); libraryLive = pad; showLibrary = true },
                                generate = { liveEditing = null; tab = AppTab.Audio; audioPage = 3 },
                                fromDrumPad = rhythmVm::liveFromDrumPad,
                                setBeats = rhythmVm::setLiveBeats,
                                setOneShot = rhythmVm::setLiveOneShot,
                                setGain = rhythmVm::setLiveGain,
                                clear = rhythmVm::clearLive,
                            ),
                        )
                    }
                    AppTab.Audio -> Column {
                        TabRow(selectedTabIndex = audioPage, containerColor = Neon.Surface, contentColor = Neon.Cyan) {
                            listOf("Pistes audio", "Remix", "Accordeur", "Générer").forEachIndexed { i, label ->
                                Tab(selected = audioPage == i, onClick = { audioPage = i }, text = { Text(label) }, modifier = Modifier.testTag("audio_page_$i"))
                            }
                        }
                        if (audioPage == 3) {
                            val installed by generateVm.installed.collectAsStateWithLifecycle()
                            val download by generateVm.download.collectAsStateWithLifecycle()
                            val genState by generateVm.state.collectAsStateWithLifecycle()
                            val prompt by generateVm.prompt.collectAsStateWithLifecycle()
                            val seconds by generateVm.seconds.collectAsStateWithLifecycle()
                            val generated by generateVm.result.collectAsStateWithLifecycle()
                            val previewPlaying by generateVm.playing.collectAsStateWithLifecycle()
                            val project by rhythmVm.project.collectAsStateWithLifecycle()
                            val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                            val task by rhythmVm.sampleTask.collectAsStateWithLifecycle()
                            DisposableEffect(Unit) {
                                generateVm.visible = true
                                onDispose {
                                    generateVm.visible = false
                                    generateVm.stopPreview()
                                }
                            }
                            com.pocketstudio.app.generate.GenerateScreen(
                                com.pocketstudio.app.generate.GenerateUi(
                                    installed, download, genState, prompt, seconds,
                                    result = generated?.name,
                                    resultSeconds = generated?.pcm?.seconds ?: 0.0,
                                    playing = previewPlaying,
                                    projectBpm = project.tempoBpm.roundToInt(),
                                    selectedPad = transport.selectedPad,
                                    placeTask = when (val t = task) {
                                        is SampleTask.Busy -> t.message
                                        is SampleTask.Failed -> t.message
                                        else -> null
                                    },
                                ),
                                com.pocketstudio.app.generate.GenerateActions(
                                    download = generateVm::downloadModel,
                                    cancelDownload = generateVm::cancelDownload,
                                    setPrompt = generateVm::setPrompt,
                                    setSeconds = generateVm::setSeconds,
                                    generate = generateVm::generate,
                                    cancel = generateVm::cancel,
                                    dismissError = generateVm::dismissError,
                                    play = generateVm::play,
                                    stop = generateVm::stopPreview,
                                    toPad = { generated?.let { rhythmVm.placeOnPad(it.pcm, it.name) } },
                                    toLive = { generated?.let { rhythmVm.placeOnLive(it.pcm, it.name) } },
                                    toAudioTrack = { generated?.let { rhythmVm.placeOnAudioTrack(it.pcm, it.name) } },
                                    discard = generateVm::discard,
                                    removeModel = generateVm::removeModel,
                                ),
                            )
                        } else if (audioPage == 2) {
                            val reading by rhythmVm.tuner.collectAsStateWithLifecycle()
                            val micOk by rhythmVm.tunerMic.collectAsStateWithLifecycle()
                            val settings by SettingsStore.state.collectAsStateWithLifecycle()
                            var granted by remember {
                                mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                            }
                            val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
                            if (granted) {
                                LaunchedEffect(Unit) {
                                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.runTuner() }
                                }
                                com.pocketstudio.app.analysis.TunerView(reading, micOk, settings.englishNotes)
                            } else {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("L'accordeur écoute le micro : autorise-le pour t'accorder.")
                                    Button(onClick = { ask.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Autoriser le micro") }
                                }
                            }
                        } else if (audioPage == 0) {
                            val project by rhythmVm.project.collectAsStateWithLifecycle()
                            val recording by rhythmVm.audioRecording.collectAsStateWithLifecycle()
                            val info by rhythmVm.sampleInfo.collectAsStateWithLifecycle()
                            val task by rhythmVm.sampleTask.collectAsStateWithLifecycle()
                            val roundTripMs by Metrics.roundTripMs.collectAsStateWithLifecycle()
                            AudioTracksScreen(
                                AudioTracksUi(
                                    project.audioTracks, recording, info,
                                    task = when (val t = task) {
                                        is SampleTask.Busy -> t.message
                                        is SampleTask.Failed -> t.message
                                        else -> null
                                    },
                                    latencyMeasured = roundTripMs != null,
                                    stepsPerBar = project.stepsPerBar,
                                ),
                                AudioTracksActions(
                                    add = rhythmVm::addAudioTrack,
                                    effects = { id -> fxChannel = com.pocketstudio.model.AudioTrack.mixerChannel(id) },
                                    remove = rhythmVm::removeAudioTrack,
                                    rename = rhythmVm::renameAudioTrack,
                                    setColor = rhythmVm::setAudioTrackColor,
                                    record = { id ->
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            rhythmVm.recordAudio(id)
                                        } else {
                                            audioRecordTrack = id
                                            audioMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    stop = rhythmVm::stopAudioRecording,
                                    cancel = rhythmVm::cancelAudioRecording,
                                    import = { id -> audioImportTrack = id; pickTrackAudio.launch(arrayOf("audio/*")) },
                                    setStart = rhythmVm::setAudioStart,
                                    setLoopBars = rhythmVm::setAudioLoopBars,
                                    setGain = rhythmVm::setAudioGain,
                                    setFadeIn = rhythmVm::setAudioFadeIn,
                                    setFadeOut = rhythmVm::setAudioFadeOut,
                                    normalize = rhythmVm::normalizeAudio,
                                    reverse = rhythmVm::reverseAudio,
                                    trim = rhythmVm::trimAudio,
                                    clear = rhythmVm::clearAudioClip,
                                ),
                            )
                        } else {
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
                            ToolPage.Reglages -> SettingsScreen(
                                settings,
                                AppInfo.versionLabel,
                                SettingsActions(
                                    update = { t -> SettingsStore.update(context, t) },
                                    reset = { SettingsStore.reset(context) },
                                    openGuide = { showGuide = true },
                                ),
                            )
                            ToolPage.Midi -> {
                                val midi by midiVm.state.collectAsStateWithLifecycle()
                                val project by rhythmVm.project.collectAsStateWithLifecycle()
                                val learning by rhythmVm.learning.collectAsStateWithLifecycle()
                                val strips = mixerStrips(project)
                                MidiScreen(
                                    midi,
                                    MidiActions(
                                        connect = midiVm::connect,
                                        disconnect = midiVm::disconnect,
                                        scanBluetooth = { bluetoothPermission.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)) },
                                        setFollowClock = midiVm::setFollowClock,
                                        setSendClock = midiVm::setSendClock,
                                    ),
                                    com.pocketstudio.app.midi.LearnUi(
                                        mappings = project.midiMap.map { it.controller to controlLabel(it.target, project, strips) },
                                        targets = learnTargets(project, strips).map { controlLabel(it, project, strips) to it },
                                        waiting = learning?.let { controlLabel(it, project, strips) },
                                        learn = { rhythmVm.learn(it as? com.pocketstudio.model.AutoTarget) },
                                        remove = rhythmVm::removeMapping,
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

/** Drums, acid, every instrument track in its colour, then the remix. */
fun mixerStrips(p: com.pocketstudio.model.Project): List<com.pocketstudio.app.mixer.StripInfo> =
    listOf(com.pocketstudio.app.mixer.StripInfo(0, "Batterie"), com.pocketstudio.app.mixer.StripInfo(1, "Basse acide")) +
        p.instrumentTracks.map { t ->
            com.pocketstudio.app.mixer.StripInfo(com.pocketstudio.model.InstrumentTrack.mixerChannel(t.id), t.name, com.pocketstudio.ui.trackColor(t.color))
        } +
        p.audioTracks.map { t ->
            com.pocketstudio.app.mixer.StripInfo(com.pocketstudio.model.AudioTrack.mixerChannel(t.id), t.name, com.pocketstudio.ui.trackColor(t.color))
        } +
        com.pocketstudio.app.mixer.StripInfo(3, "Remix")

/** The timeline rows: drums, acid, then each instrument and audio track in its colour. */
fun timelineRows(p: Project): List<com.pocketstudio.app.session.TimelineRow> =
    listOf(
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.DRUMS, "Batterie", androidx.compose.ui.graphics.Color(0xFFFF8A65)),
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.ACID, "Basse acide", androidx.compose.ui.graphics.Color(0xFF81C784)),
    ) + p.instrumentTracks.map { t ->
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.inst(t.id), t.name, com.pocketstudio.ui.trackColor(t.color))
    } + p.audioTracks.map { t ->
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.audio(t.id), t.name, com.pocketstudio.ui.trackColor(t.color))
    }

/** Settings a MIDI knob can move: the instrument's sound, live effects, tempo, then the mixer. */
fun learnTargets(p: Project, strips: List<com.pocketstudio.app.mixer.StripInfo>): List<com.pocketstudio.model.AutoTarget> {
    val t = com.pocketstudio.model.MidiMapping
    return com.pocketstudio.app.synth.SYNTH_LABELS.map { (index, _) -> com.pocketstudio.model.AutoTarget(t.SYNTH, param = index) } +
        (1..com.pocketstudio.model.PlaitsPatch.PARAMS.size).map { com.pocketstudio.model.AutoTarget(t.PLAITS, param = it) } +
        listOf(0, 1).map { com.pocketstudio.model.AutoTarget(t.PERFORMANCE, param = it) } +
        com.pocketstudio.model.AutoTarget(t.TEMPO) +
        com.pocketstudio.app.mixer.automationTargets(p.mixer.channels, p.mixer.master, strips)
}

/** What a MIDI knob moves, in words. */
fun controlLabel(target: com.pocketstudio.model.AutoTarget, p: Project, strips: List<com.pocketstudio.app.mixer.StripInfo>): String =
    when (target.kind) {
        com.pocketstudio.model.MidiMapping.SYNTH ->
            "Synthé : " + (com.pocketstudio.app.synth.SYNTH_LABELS.firstOrNull { it.first == target.param }?.second ?: "réglage") + " (piste choisie)"
        com.pocketstudio.model.MidiMapping.PLAITS ->
            "Plaits : " + com.pocketstudio.model.PlaitsPatch.PARAMS.getOrElse(target.param - 1) { "réglage" } + " (piste choisie)"
        com.pocketstudio.model.MidiMapping.PERFORMANCE -> if (target.param == 0) "Effet live : filtre" else "Effet live : résonance"
        com.pocketstudio.model.MidiMapping.TEMPO -> "Tempo (60 à 200 BPM)"
        else -> com.pocketstudio.app.mixer.automationLabel(target, p.mixer.channels, p.mixer.master, strips)
    }

/** The kind of sound a drum pad usually holds, to open the library on it. */
private fun padKind(lane: Int) = when (lane) {
    0, 1 -> "kick"
    2, 3, 10 -> "snare"
    4 -> "clap"
    5, 13 -> "hat"
    6, 12 -> "open_hat"
    else -> "perc"
}
