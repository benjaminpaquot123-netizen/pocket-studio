package com.pocketstudio.app.ui

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
import com.pocketstudio.app.studio.StudioScreen
import com.pocketstudio.app.synth.SynthActions
import com.pocketstudio.app.synth.SynthScreen
import com.pocketstudio.app.studio.StudioViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.ui.Neon
import kotlinx.coroutines.delay

enum class AppTab(val label: String, val icon: ImageVector) {
    Rythme("Rythme", Icons.Filled.GridView),
    Acide("Acide", Icons.Filled.GraphicEq),
    Synthe("Synthé", Icons.Filled.Piano),
    Mixeur("Mixeur", Icons.Filled.Equalizer),
    Outils("Outils", Icons.Filled.Tune),
}

private enum class ToolPage(val label: String) { Recette("Recette"), Fond("Arrière-plan"), Latence("Latence"), Son("Son test") }

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val mainVm: MainViewModel = viewModel()
    val studioVm: StudioViewModel = viewModel()
    val latencyVm: LatencyViewModel = viewModel()
    val recetteVm: RecetteViewModel = viewModel()
    val rhythmVm: RhythmViewModel = viewModel()
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
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importSample(uri)
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
                            label = { Text(t.label) },
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
                        AcidScreen(
                            acid = project.acid,
                            playing = transport.playing,
                            currentStep = live.currentStep,
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
                            ),
                        )
                    }
                    AppTab.Synthe -> {
                        DisposableEffect(Unit) { onDispose { rhythmVm.synthAllOff() } }
                        val project by rhythmVm.project.collectAsStateWithLifecycle()
                        val octave by rhythmVm.keyboardOctave.collectAsStateWithLifecycle()
                        SynthScreen(
                            patch = project.synth,
                            octave = octave,
                            actions = SynthActions(
                                noteOn = rhythmVm::synthNoteOn,
                                noteOff = rhythmVm::synthNoteOff,
                                shiftOctave = rhythmVm::shiftKeyboardOctave,
                                setParam = rhythmVm::setSynthParam,
                                applyPreset = rhythmVm::applySynthPreset,
                            ),
                        )
                    }
                    AppTab.Mixeur -> {
                        val project by rhythmVm.project.collectAsStateWithLifecycle()
                        val meters by rhythmVm.meters.collectAsStateWithLifecycle()
                        val export by rhythmVm.export.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollMeters() }
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
                            ),
                            export = export,
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
