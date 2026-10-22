package com.pocketstudio.app.ui

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.track
import kotlin.math.roundToInt
import com.pocketstudio.app.instruments.InstrumentUi
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.app.audiotracks.AudioTracksActions
import com.pocketstudio.app.audiotracks.AudioTracksScreen
import com.pocketstudio.app.audiotracks.AudioTracksUi
import com.pocketstudio.model.viewOfTrack
import com.pocketstudio.model.songBars
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.model.audioSpans
import com.pocketstudio.model.songStepOf
import androidx.compose.runtime.remember
import com.pocketstudio.app.synth.TracksUi
import com.pocketstudio.app.synth.NotesUi
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.model.NoteClip
import com.pocketstudio.app.report.AppLog
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Alignment
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
import androidx.compose.runtime.SideEffect
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
import com.pocketstudio.model.longFxCount
import com.pocketstudio.model.soundKindOf
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
import com.pocketstudio.app.projects.StarterGroup
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
import com.pocketstudio.model.automatedAcidKnobs
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.SoundCategories
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.app.session.LivePadsActions
import com.pocketstudio.app.session.LivePadsUi
import com.pocketstudio.app.session.SessionScreenUi
import com.pocketstudio.model.Scene
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.copyOf
import com.pocketstudio.app.mixer.AutomationActions
import com.pocketstudio.app.mixer.AutomationUi
import com.pocketstudio.app.remix.RemixScreen
import com.pocketstudio.app.remix.RemixUi
import com.pocketstudio.app.remix.RemixViewModel
import com.pocketstudio.app.studio.StudioViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.ui.Neon
import kotlinx.coroutines.delay

enum class AppTab(val icon: ImageVector) {
    Rythme(Icons.Filled.GridView),
    Acide(Icons.Filled.GraphicEq),
    Synthe(Icons.Filled.Piano),
    Mixeur(Icons.Filled.Equalizer),
    Audio(Icons.Filled.Mic),
    Morceau(Icons.Filled.QueueMusic),
    Outils(Icons.Filled.Tune);

    /** The tab's name in the interface's language. */
    val label: String get() = when (this) {
        Rythme -> tr("Rythme")
        Acide -> tr("Acide")
        Synthe -> tr("Synthé")
        Mixeur -> tr("Mixeur")
        Audio -> tr("Audio")
        Morceau -> tr("Morceau")
        Outils -> tr("Outils")
    }
}

internal enum class ToolPage {
    // Help first: where a lost beginner looks.
    Aide, Reglages, Recette, Midi, Fond, Latence, Son;

    val label: String get() = when (this) {
        Aide -> tr("Aide")
        Reglages -> tr("Réglages")
        Recette -> tr("Recette")
        Midi -> "MIDI"
        Fond -> tr("Fond")
        Latence -> tr("Latence")
        Son -> tr("Son")
    }

    companion object {
        /** Outils opens on Aide the first time, then on the page last chosen ([saved]: its name). */
        fun initial(saved: String?): ToolPage = entries.firstOrNull { it.name == saved } ?: Aide

        private const val PREFS = "tools"
        private const val KEY = "page"

        fun load(context: android.content.Context): ToolPage =
            initial(context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getString(KEY, null))

        fun save(context: android.content.Context, page: ToolPage) =
            context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit().putString(KEY, page.name).apply()
    }
}

/**
 * Ways to start a new project (Mes projets › Nouveau projet), by group, besides the guided song
 * and the assistant's styles made whole in one touch (shown by the page itself).
 */
internal val PROJECT_STARTERS: List<Pair<ProjectStarter, () -> Project>> = listOf(
    ProjectStarter("Rythme de départ", "Un rythme et une ligne d'acide prêts à tourner.", "starter", StarterGroup.Style) to { RhythmViewModel.starterProject("") },
) + com.pocketstudio.model.Demos.ALL.mapIndexed { i, (title, description, build) ->
    ProjectStarter(title.removePrefix("Démo · "), description, "demo_$i", StarterGroup.Demo) to build
} + listOf(
    ProjectStarter("Projet vide", "Tout est vierge : à toi de jouer.", "empty", StarterGroup.Empty) to { Project(name = "") },
)

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
    val tabStates = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    var tab by rememberSaveable { mutableStateOf(AppTab.Rythme) }
    // « Ce qui joue »: -2 closed, -1 the whole song, else a scene; [overviewCh]: one track's (-1: all).
    var overviewScene by rememberSaveable { mutableStateOf(-2) }
    var overviewCh by rememberSaveable { mutableStateOf(-1) }
    // Where « Ce qui joue » sent the user in the mixer (a lane, the master, the buses).
    var mixerFocus by remember { mutableStateOf<com.pocketstudio.app.mixer.Where?>(null) }
    var showProjects by rememberSaveable { mutableStateOf(false) }
    // « Partager mon morceau » (Mixeur, Mes projets): name, export in M4A, share sheet.
    val shareSong = com.pocketstudio.app.export.rememberShareSong(rhythmVm)
    // ▶ plays the song on the Morceau tab until the user chooses; the section being edited elsewhere.
    SideEffect { rhythmVm.onSongTab = tab == AppTab.Morceau }
    val projectsVm: ProjectsViewModel = viewModel()
    val pickProjectZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) projectsVm.importZip(uri)
    }
    val openScope = rememberCoroutineScope()
    // Another project: save this one, remember the choice, then rebuild every screen on it.
    // [deleteAfter]: the project left goes to the trash once the next one is open (the open
    // project itself deleted from « Mes projets », which then stays on screen).
    val switchProject: (String, String?) -> Unit = { id, deleteAfter ->
        openScope.launch {
            rhythmVm.keepTake()  // a microphone take in progress lands on its track first
            rhythmVm.flushNow()
            // Not saved (a full phone): stay here rather than reopen this project without its latest edits.
            if (rhythmVm.session.lastSaveError.value != null) {
                Toast.makeText(context, tr("Le projet n'a pas pu être sauvegardé (appareil plein ?) : libère de la place avant d'en ouvrir un autre."), Toast.LENGTH_LONG).show()
                return@launch
            }
            showProjects = deleteAfter != null  // remembered across the rebuild
            CurrentProject.set(context, id)
            deleteAfter?.let { CurrentProject.deleteLater(context, it) }
            AppLog.add("Ouverture d'un autre projet")
            context.findActivity()?.let { activity ->
                activity.viewModelStore.clear()
                activity.recreate()
            }
        }
    }
    val openProject: (String) -> Unit = { id -> switchProject(id, null) }
    // The sound library, for the selected drum pad or for a live pad (libraryLive >= 0).
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    var libraryLive by rememberSaveable { mutableIntStateOf(-1) }
    // ... or for the selected instrument track's sampled sound.
    var libraryTrack by rememberSaveable { mutableStateOf(false) }
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
        // A sound still loading when the library closed lands nowhere (libraryVm.closed()).
        val close = { libraryVm.closed(); showLibrary = false; libraryLive = -1; libraryTrack = false }
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
                            forTrack -> tr("la piste « %s »", com.pocketstudio.model.DefaultNames.shown(libProject.track(libTrack).name))
                            live >= 0 -> tr("le pad live %d", live + 1)
                            // The sound on the pad now (« Kick de ma voix »), as the pad and the grid name it.
                            else -> tr("le pad %1\$d (%2\$s)", lane + 1, com.pocketstudio.app.rhythm.padName(libProject, lane))
                        },
                        startKind = if (forTrack) "808" else if (live >= 0) "stab" else padKind(lane),
                        sizes = sizes,
                        songPlaying = libTransport.playing,
                        styleId = com.pocketstudio.app.library.styleOfProject(libProject.name),
                        metered = androidx.compose.runtime.remember {
                            runCatching { context.getSystemService(android.net.ConnectivityManager::class.java).isActiveNetworkMetered }.getOrDefault(false)
                        },
                    ),
                    com.pocketstudio.app.library.LibraryActions(
                        close = close,
                        togglePlay = rhythmVm::togglePlay,
                        preview = libraryVm::preview,
                        use = { option ->
                            libraryVm.use(option.ref) { pcm ->
                                val name = option.name.substringBefore(" · ")
                                val placed = when {
                                    forTrack -> rhythmVm.useAsTrackSound(pcm, name, (option.ref as? com.pocketstudio.app.library.SoundRef.Pack)?.sound?.kind)
                                    live >= 0 -> rhythmVm.placeOnLivePad(live, pcm, name)
                                    else -> rhythmVm.placeOnPad(pcm, name, oneShot = true)
                                }
                                if (placed) close()
                                else libraryVm.notice(tr("Un son est encore en train d'être posé ou enregistré : réessaie dans un instant."))
                            }
                        },
                        download = libraryVm::downloadPack,
                        cancelDownload = libraryVm::cancelDownload,
                        deletePack = libraryVm::deletePack,
                        downloadAll = libraryVm::downloadAll,
                        cancelAll = libraryVm::cancelAll,
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
                tr("pad %1\$d (%2\$s)", lane + 1, fxProject.drums.lanes.getOrNull(lane)?.sample?.name?.let { com.pocketstudio.model.DefaultNames.shownSample(it) } ?: com.pocketstudio.app.rhythm.DrumKitInfo.names[lane])
            } else {
                com.pocketstudio.model.DefaultNames.shown(mixerStrips(fxProject).firstOrNull { it.ch == fxChannel }?.name ?: "Piste")
            }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { fxChannel = -1 },
                // The whole width of the phone: everything seen at once, nothing to scroll for.
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 10.dp),
                title = { Text(tr("Effets · %s", name)) },
                text = {
                    Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                        // The same box as under every sound (Synthé, Acide), here whole: pad XY, macros and sends too.
                        com.pocketstudio.app.mixer.EffectsBox(fxChannel, channel, effectsActions(rhythmVm))
                    }
                },
                confirmButton = { TextButton(onClick = { fxChannel = -1 }, modifier = Modifier.testTag("fx_close")) { Text(tr("Fermer")) } },
            )
        }
    }
    if (overviewScene >= -1) {
        val p by rhythmVm.project.collectAsStateWithLifecycle()
        val all = p.allScenes
        val sceneChoices = (p.song.map { it.scene } + p.scene).distinct().sorted().filter { it in all.indices }.map { i ->
            i to (Scene.NAMES.getOrElse(i) { "?" } + all[i].label.takeIf { it.isNotBlank() }?.let { " · " + Scene.shownLabel(it) }.orEmpty())
        }
        com.pocketstudio.app.mixer.WhatPlaysDialog(
            p, mixerStrips(p), scene = overviewScene.takeIf { it >= 0 }, onlyCh = overviewCh.takeIf { it >= 0 }, scenes = sceneChoices,
            setScene = { overviewScene = it ?: -1 },
            showAll = { overviewCh = -1 },
            go = { w ->
                overviewScene = -2
                when (w) {
                    is com.pocketstudio.app.mixer.Where.Effects -> fxChannel = w.ch
                    else -> { tab = AppTab.Mixeur; mixerFocus = w }
                }
            },
            dismiss = { overviewScene = -2 },
        )
    }
    // The assistant made a song: a short guide to its two scenes and what to do next.
    // Kept through the rebuild that opening the new project does (the activity is recreated).
    var songGuide by com.pocketstudio.app.library.rememberSongGuide()
    songGuide?.let { pages ->
        com.pocketstudio.app.settings.GuideDialog(
            onClose = { songGuide = null }, pages = pages, tag = "song_guide", done = tr("▶ Écouter mon morceau"),
            // The guide ends on the song itself, from its first part.
            onDone = {
                songGuide = null
                if (!rhythmVm.transport.value.playing) {
                    if (rhythmVm.project.value.song.isNotEmpty()) rhythmVm.toggleSong() else rhythmVm.togglePlay()
                }
            },
        )
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
                        setAcidEnabled = libraryVm::setAcidEnabled,
                        acidTap = libraryVm::acidTapStep,
                        acidHold = libraryVm::acidHoldStep,
                        generateAcid = libraryVm::generateAcid,
                        setAcidDensity = libraryVm::setAcidDensity,
                        setAcidBars = libraryVm::setAcidBars,
                        acidFromStyle = libraryVm::acidFromStyle,
                        chooseAcidSound = libraryVm::chooseAcidSound,
                        setAcidKnob = libraryVm::setAcidKnob,
                        choosePartSound = libraryVm::choosePartSound,
                        preview = libraryVm::preview,
                        download = libraryVm::downloadPack,
                        cancelDownload = libraryVm::cancelDownload,
                        deletePack = libraryVm::deletePack,
                        create = {
                            libraryVm.create { project, sounds, guide ->
                                projectsVm.create(project, sounds, failed = libraryVm::createFailed) { id ->
                                    songGuide = guide
                                    libraryVm.closeWizard()
                                    openProject(id)
                                }
                            }
                        },
                    ),
                    preparing = preparing,
                )
            }
        }
    }
    if (showProjects) {
        val projectsUi by projectsVm.state.collectAsStateWithLifecycle()
        val quickUi by libraryVm.quick.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { projectsVm.refresh() }
        androidx.activity.compose.BackHandler { showProjects = false }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showProjects = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            androidx.compose.material3.Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                ProjectsScreen(
                    projectsUi, rhythmVm.projectId, PROJECT_STARTERS.map { it.first },
                    quick = quickUi,
                    actions = ProjectsActions(
                        back = { showProjects = false },
                        open = { openProject(it.id) },
                        create = { i -> projectsVm.create(PROJECT_STARTERS[i].second()) { id -> openProject(id) } },
                        wizard = {
                            rhythmVm.stop()
                            libraryVm.openWizard()
                        },
                        // A style's whole song in one touch: the assistant's own song, its guide after.
                        quickStyle = { id ->
                            rhythmVm.stop()
                            libraryVm.quickCreate(id) { project, sounds, guide ->
                                projectsVm.create(project, sounds, failed = libraryVm::quickFailed) { newId ->
                                    songGuide = guide
                                    libraryVm.quickDone()
                                    openProject(newId)
                                }
                            }
                        },
                        customStyle = { id ->
                            rhythmVm.stop()
                            libraryVm.openWizard(id)
                        },
                        dismissQuickError = libraryVm::dismissQuickError,
                        rename = { p, name ->
                            // The open project: saved first, so the list reads its new name.
                            if (p.id == rhythmVm.projectId) openScope.launch { rhythmVm.renameProject(name); rhythmVm.flushNow(); projectsVm.refresh() }
                            else projectsVm.rename(p.id, name)
                        },
                        // The open project is saved first; not saved (a full phone): refused, not copied as it was on disk.
                        duplicate = { p -> projectsVm.duplicate(p, saveFirst = { p.id != rhythmVm.projectId || rhythmVm.flushNow() }) },
                        exportZip = { p ->
                            projectsVm.exportZip(p, saveFirst = { p.id != rhythmVm.projectId || rhythmVm.flushNow() }) { file ->
                                WavExporter.share(context, listOf(file), tr("Envoyer le projet"))
                            }
                        },
                        importZip = { pickProjectZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                        // The open project too (an assistant's song included): another one opens first
                        // (the latest, else a new blank one), then it goes to the trash.
                        delete = { p ->
                            if (p.id != rhythmVm.projectId) projectsVm.delete(p)
                            else {
                                rhythmVm.stop()
                                val next = projectsUi.projects.firstOrNull { it.id != p.id }
                                if (next != null) switchProject(next.id, p.id)
                                else projectsVm.create(Project(name = "")) { id -> switchProject(id, p.id) }
                            }
                        },
                        undoDelete = projectsVm::undoDelete,
                        restore = projectsVm::restore,
                        deleteForever = projectsVm::deleteForever,
                        emptyTrash = projectsVm::emptyTrash,
                        dismissDeleted = projectsVm::dismissDeleted,
                        dismissMessage = projectsVm::dismissMessage,
                        shareSong = { showProjects = false; shareSong() },
                    ),
                )
            }
        }
    }
    var toolPage by rememberSaveable { mutableStateOf(ToolPage.load(context)) }
    val settings by SettingsStore.state.collectAsStateWithLifecycle()
    var showGuide by rememberSaveable { mutableStateOf(false) }
    // After the crash report's question (never both dialogs on top of each other).
    val pendingCrash by mainVm.crash.collectAsStateWithLifecycle()
    if (showGuide && pendingCrash == null) {
        GuideDialog(onClose = {
            showGuide = false
            SettingsStore.update(context) { it.copy(guideSeen = true) }
        })
    }
    // The first launch: three big choices (guided song, demo, empty project), not pages of text.
    val firstLaunchSeen = { SettingsStore.update(context) { it.copy(guideSeen = true) } }
    com.pocketstudio.app.onboarding.FirstLaunch(
        show = !settings.guideSeen && pendingCrash == null,
        actions = com.pocketstudio.app.onboarding.WelcomeActions(
            guided = { firstLaunchSeen(); rhythmVm.stop(); libraryVm.openWizard() },
            demo = { firstLaunchSeen(); projectsVm.create(com.pocketstudio.model.Demos.ALL.first().third()) { id -> openProject(id) } },
            empty = { firstLaunchSeen(); projectsVm.create(Project(name = "")) { id -> openProject(id) } },
            later = firstLaunchSeen,
        ),
        playDemo = { if (rhythmVm.project.value.song.isNotEmpty()) rhythmVm.toggleSong() else rhythmVm.togglePlay() },
    )
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
        rhythmVm.flush()
        // Turning the phone also stops the activity: the take, the held notes and the sound go on.
        if (context.findActivity()?.isChangingConfigurations == true) return@LifecycleEventEffect
        latencyVm.cancel()
        // A sound being tried (the line looping with the beat) stops when the app is left.
        libraryVm.stopPreview()
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
            confirmButton = { TextButton(onClick = rhythmVm::dismissNotice) { Text(tr("Compris")) } },
            dismissButton = if (rhythmVm.canUndoFavoriteDelete) {
                { TextButton(onClick = rhythmVm::undoFavoriteDelete, modifier = Modifier.testTag("favorite_undo")) { Text(tr("Annuler")) } }
            } else null,
            text = { Text(text) },
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val foldStore = com.pocketstudio.ui.LocalFoldStore.current
    LaunchedEffect(engineAvailable) {
        if (!engineAvailable) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                Metrics.updateStream(NativeEngine.streamInfo())
                delay(100)
            }
        }
    }

    // A permission refused (twice, Android stops asking): say why nothing happens and where to allow it.
    var permissionHelp by rememberSaveable { mutableStateOf<String?>(null) }
    permissionHelp?.let { text ->
        AlertDialog(
            onDismissRequest = { permissionHelp = null },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    permissionHelp = null
                    runCatching {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }, modifier = Modifier.testTag("permission_settings")) { Text(tr("Ouvrir les réglages")) }
            },
            dismissButton = { TextButton(onClick = { permissionHelp = null }) { Text(tr("Plus tard")) } },
        )
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) latencyVm.measure() else permissionHelp = MIC_REFUSED
    }
    val recordPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) rhythmVm.startRecording() else permissionHelp = MIC_REFUSED
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
        else permissionHelp = tr("Le Bluetooth (appareils à proximité) est refusé : autorise-le dans les réglages de l'appareil pour trouver un clavier MIDI.")
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
        if (granted) rhythmVm.recordNotes() else permissionHelp = MIC_REFUSED
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
        if (granted) rhythmVm.recordAudio(audioRecordTrack) else permissionHelp = MIC_REFUSED
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
    // The Live page (opened from Morceau) takes the whole screen: no top bar, no tabs.
    var showLive by rememberSaveable { mutableStateOf(false) }
    // The live pad being set up (Morceau and the Live page's Pads share it).
    var liveEditing by rememberSaveable { mutableStateOf<Int?>(null) }
    // Leaving the Live page: the « Morceau écrit » line goes with it (its Annuler would come back
    // later, after other changes), and nothing set there stays on unseen (the Montée, what the
    // lock kept). During a take the page asks first (LiveScreen's own Back). A rotation keeps all.
    val closeLive = { rhythmVm.leaveLive(); rhythmVm.dismissPerformance(); showLive = false }
    androidx.activity.compose.BackHandler(enabled = showLive) { closeLive() }
    Scaffold(
        containerColor = Neon.Background,
        topBar = {
            if (!showLive) {
            val topProject by rhythmVm.project.collectAsStateWithLifecycle()
            val topTransport by rhythmVm.transport.collectAsStateWithLifecycle()
            val topUndo by rhythmVm.session.canUndo.collectAsStateWithLifecycle()
            val topRedo by rhythmVm.session.canRedo.collectAsStateWithLifecycle()
            val topRecording by rhythmVm.recording.collectAsStateWithLifecycle()
            val topRecordInto by rhythmVm.recordInto.collectAsStateWithLifecycle()
            val topTake by rhythmVm.audioRecording.collectAsStateWithLifecycle()
            val topSaving by rhythmVm.takeSaving.collectAsStateWithLifecycle()
            val topPerformance by rhythmVm.take.collectAsStateWithLifecycle()
            // No undo during a take (or while it is written): it could remove the track it goes to;
            // a performance on the Live page undoes in one step once written.
            val undoable = !topRecording && topTake == null && !topSaving && !topPerformance.running
            // "✓ Enregistré" only once nothing waits to be written.
            val topUnsaved by rhythmVm.session.unsaved.collectAsStateWithLifecycle()
            // On the Audio tracks screen (or during a take) the ● is the microphone's.
            val micMode = (tab == AppTab.Audio && audioPage == 0) || topTake != null
            TopBar(
                TopBarUi(
                    com.pocketstudio.model.DefaultNames.shown(topProject.name), topProject.tempoBpm, topTransport.playing, topUndo && undoable, topRedo && undoable, topRecording || topTake != null,
                    micTake = micMode,
                    // The Audio tracks screen records on the scene shown: it can be changed from there.
                    scene = if (tab in SCENE_TABS || (tab == AppTab.Audio && audioPage == 0)) sceneChip(topProject) else null,
                    saved = saveError == null && !topUnsaved,
                    recordInto = topRecordInto?.let { topProject.sceneTitle(it) },
                ),
                TopBarActions(
                    projects = { showProjects = true }, undo = rhythmVm::undo, redo = rhythmVm::redo,
                    togglePlay = rhythmVm::togglePlay,
                    toggleRecord = {
                        when {
                            // From the synth the take is a played phrase: a one-bar loop grows to four first.
                            !micMode -> rhythmVm.toggleRecord(notes = tab == AppTab.Synthe)
                            topTake != null -> rhythmVm.stopAudioRecording()
                            !topSaving -> rhythmVm.micTakeTrack()?.let { id ->
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    rhythmVm.recordAudio(id)
                                } else {
                                    audioRecordTrack = id
                                    audioMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    },
                    selectScene = rhythmVm::selectScene, newScene = rhythmVm::newScene,
                ),
            )
            if (saveError != null) {
                Text(
                    tr("Sauvegarde impossible (mémoire pleine ?). Tes dernières modifications ne sont pas encore sauvegardées. Pour faire de la place : Mes projets › Corbeille › Vider la corbeille."),
                    color = Color.Black,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().background(Neon.Amber).windowInsetsPadding(SIDE_INSETS).padding(8.dp)
                        .testTag("save_error"),
                )
            }
            val loopGrown by rhythmVm.takeLoopGrown.collectAsStateWithLifecycle()
            val loopSong by rhythmVm.takeLoopSong.collectAsStateWithLifecycle()
            if (loopGrown) {
                // Said where the eyes are during the count-in, without a window in front of the keys.
                Row(
                    Modifier.fillMaxWidth().background(Neon.Surface).windowInsetsPadding(SIDE_INSETS).padding(start = 12.dp, end = 4.dp).testTag("take_loop_grown"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (loopSong) tr("La boucle passe à 4 mesures pour ta prise, et les parties du morceau avec cette scène aussi. ↶ en haut pour revenir à 1 mesure.")
                        else tr("La boucle passe à 4 mesures pour ta prise : ta phrase ne se repliera pas sur une seule mesure. ↶ en haut pour revenir à 1 mesure."),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = rhythmVm::dismissTakeLoop) { Text(tr("Compris")) }
                }
            }
            }
        },
        bottomBar = {
            if (!landscape && !showLive) androidx.compose.foundation.layout.BoxWithConstraints {
                // Seven tabs: below ~58 dp each (small phone, big text) the labels shrink, so none is cut.
                val tight = maxWidth / AppTab.entries.size < 58.dp
                NavigationBar(
                    containerColor = lerp(Neon.Background, Color.Black, 0.12f),
                    modifier = Modifier.drawBehind { drawLine(Color.White.copy(alpha = 0.07f), Offset.Zero, Offset(size.width, 0f), 1f) },
                ) {
                    AppTab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = if (tight) t.label else null) },
                            label = {
                                Text(
                                    t.label, maxLines = 1, softWrap = false,
                                    style = if (tight) MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85f)
                                    else MaterialTheme.typography.labelMedium,
                                )
                            },
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
        if (showLive) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                LiveRoute(
                    rhythmVm, lifecycleOwner, close = closeLive,
                    openEffects = { fxChannel = it },
                    padsActions = com.pocketstudio.app.session.LivePadsActions(
                        tap = rhythmVm::toggleLive,
                        stopAll = rhythmVm::stopLive,
                        setSize = rhythmVm::setLiveSize,
                        edit = { liveEditing = it },
                        import = { pad -> liveImportPad = pad; rhythmVm.dismissSampleError(); pickLiveAudio.launch(arrayOf("audio/*")) },
                        library = { pad -> rhythmVm.dismissSampleError(); libraryLive = pad; showLibrary = true },
                        generate = {
                            // A take never goes on unseen behind another page: it ends here (armed: dropped).
                            if (rhythmVm.performanceTaking) rhythmVm.togglePerformance()
                            liveEditing = null; closeLive(); tab = AppTab.Audio; audioPage = 3
                        },
                        fromDrumPad = rhythmVm::liveFromDrumPad,
                        setBeats = rhythmVm::setLiveBeats,
                        setOneShot = rhythmVm::setLiveOneShot,
                        setGain = rhythmVm::setLiveGain,
                        clear = rhythmVm::clearLive,
                        effects = { fxChannel = 3 },
                    ),
                    liveEditing = liveEditing,
                )
            }
            return@Scaffold
        }
        // The background darkens a little towards the bottom: the panels seem to float above it.
        val bg = Neon.Background
        Row(Modifier.padding(padding).background(Brush.verticalGradient(listOf(lerp(bg, Color.White, 0.03f), bg, lerp(bg, Color.Black, 0.18f))))) {
            // Landscape: tabs on the side so the screen keeps its height (wider synth keyboard).
            if (landscape) {
                NavigationRail(containerColor = lerp(Neon.Background, Color.Black, 0.12f)) {
                    // Seven tabs need more height than a phone lying down has: the rail scrolls.
                    androidx.compose.foundation.layout.Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
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
            }
            Box(Modifier.weight(1f)) {
                // Each tab keeps its own screen state (Notes or Clavier, folded cards, chosen tools)
                // while another tab is shown, as long as the app lives.
                tabStates.SaveableStateProvider(tab.name) {
                when (tab) {
                        AppTab.Rythme -> {
                            val project by rhythmVm.project.collectAsStateWithLifecycle()
                            val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                            val live by rhythmVm.live.collectAsStateWithLifecycle()
                            val canUndo by rhythmVm.session.canUndo.collectAsStateWithLifecycle()
                            val canRedo by rhythmVm.session.canRedo.collectAsStateWithLifecycle()
                            val take by rhythmVm.audioRecording.collectAsStateWithLifecycle()
                            val performance by rhythmVm.take.collectAsStateWithLifecycle()
                            val sampleInfo by rhythmVm.sampleInfo.collectAsStateWithLifecycle()
                            val sampleTask by rhythmVm.sampleTask.collectAsStateWithLifecycle()
                            val slicer by rhythmVm.slicer.collectAsStateWithLifecycle()
                            val rhythmListen by rhythmVm.listenSong.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                            }
                            RhythmScreen(
                                project = project,
                                transport = transport,
                                live = live,
                                canUndo = canUndo && take == null && !performance.running,
                                canRedo = canRedo && take == null && !performance.running,
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
                                    setHitVelocity = rhythmVm::setHitVelocity,
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
                                    cancelResample = rhythmVm::cancelResample,
                                    importFolder = { pickFolder.launch(null) },
                                    importZip = { pickZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                                    setListenSong = rhythmVm::setListenSong,
                                ),
                                padSound = PadSoundUi(sampleInfo, sampleTask, slicer),
                                // The same choice as Morceau › Écouter, shown where ▶ is touched.
                                // Until chosen (saved with the project), ▶ here plays the section being edited.
                                listenSong = if (project.song.isEmpty()) null else rhythmListen == true,
                                header = { showGrid ->
                                    com.pocketstudio.app.onboarding.ProjectNextSteps(project, rhythmVm.projectId, share = shareSong, showHere = showGrid) { tab = it }
                                },
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
                            val automationRecording by rhythmVm.automationRecord.collectAsStateWithLifecycle()
                            AcidScreen(
                                effects = project.mixer.channels.getOrNull(1)?.let { c ->
                                    com.pocketstudio.app.mixer.EffectsUi(1, c, effectsActions(rhythmVm)) { fxChannel = 1 }
                                },
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
                                    toggleRecord = rhythmVm::toggleAutomationRecord,
                                    holdSound = rhythmVm::holdAcidKnob,
                                ),
                                recording = automationRecording,
                                automated = project.automatedAcidKnobs,
                                browser = SoundBrowserUi(
                                    SoundCategories.ACID, SoundLibrary.ACID, favorites.acid,
                                    current = (SoundLibrary.ACID + favorites.acid).firstOrNull {
                                        it.sound.copy(tuning = project.acid.sound.tuning, level = project.acid.sound.level) == project.acid.sound
                                    }?.name,
                                    // Tuning stays; the volume is the sound's calibrated one unless the user set their own.
                                    pick = { rhythmVm.applyAcidSound(SoundLibrary.withAcidPreset(SoundLibrary.ACID[it].sound, project.acid.sound)) },
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
                            val trackFx = com.pocketstudio.model.InstrumentTrack.mixerChannel(shownTrack)
                            SynthScreen(
                                effects = fullProject.mixer.channels.getOrNull(trackFx)?.let { c ->
                                    com.pocketstudio.app.mixer.EffectsUi(trackFx, c, effectsActions(rhythmVm)) { fxChannel = trackFx }
                                },
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
                                    sampledMessage = trackSoundMessage?.let { if (it == RhythmViewModel.BUSY_TRACK_SOUND) tr("Préparation du son…") else it },
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
                                    addKind = rhythmVm::addTrackOf,
                                    rename = rhythmVm::renameTrack,
                                    setColor = rhythmVm::setTrackColor,
                                    duplicate = rhythmVm::duplicateTrack,
                                    remove = rhythmVm::removeTrack,
                                    effects = { id -> fxChannel = com.pocketstudio.model.InstrumentTrack.mixerChannel(id) },
                                    clear = rhythmVm::clearTrackNotes,
                                    scene = fullProject.sceneTitle(fullProject.scene),
                                ),
                                browser = if (project.synthEngine == 1) {
                                    SoundBrowserUi(
                                        SoundCategories.PLAITS, SoundLibrary.PLAITS, favorites.plaits,
                                        current = (SoundLibrary.PLAITS + favorites.plaits).firstOrNull { it.sound == project.plaits.copy(level = it.sound.level, gain = it.sound.gain) }?.name,
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
                                        current = SoundLibrary.synthNameOf(project.synth, SoundLibrary.SYNTH + favorites.synth),
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
                            // Read only by the meters when they draw: a reading (20 a second) composes nothing.
                            val meters = rhythmVm.meters.collectAsStateWithLifecycle()
                            val export by rhythmVm.export.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollMeters() }
                            }
                            val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                            // Read only when the curves draw their playhead: a step composes nothing.
                            val live = rhythmVm.live.collectAsStateWithLifecycle()
                            val automationRecord by rhythmVm.automationRecord.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                            }
                            val playing = transport.playing
                            MixerScreen(
                                channels = project.mixer.channels,
                                meters = { meters.value },
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
                                    setExportFade = rhythmVm::setExportFade,
                                    setExportNoLimiter = rhythmVm::setExportNoLimiter,
                                    setStemsWithMaster = rhythmVm::setStemsWithMaster,
                                    setMasterDb = rhythmVm::setMasterDb,
                                    shareSong = shareSong,
                                    exportMidi = rhythmVm::exportMidi,
                                    setInsertType = rhythmVm::setInsertType,
                                    setInsertParam = rhythmVm::setInsertParam,
                                    setSend = rhythmVm::setSend,
                                    setBusParam = rhythmVm::setBusParam,
                                    quick = quickFxActions(rhythmVm),
                                    setInsertBypass = rhythmVm::setInsertBypass,
                                    moveInsert = rhythmVm::moveInsert,
                                    kindOf = effectsActions(rhythmVm).kindOf,
                                    longFxCount = effectsActions(rhythmVm).longFxCount,
                                    cpuBusy = rhythmVm.cpuBusy,
                                    setInsertScenes = rhythmVm::setInsertScenes,
                                    scenes = rhythmVm.sceneLabels,
                                    // Polled by the mixer itself (above).
                                    meters = rhythmVm.meters,
                                ),
                                master = project.mixer.master,
                                masterDb = project.mixer.masterDb,
                                buses = project.mixer.buses,
                                strips = mixerStrips(project),
                                spectrum = spectrum,
                                analyserOpen = analyserOn,
                                setAnalyser = { analyserOn = it },
                                automation = AutomationUi(
                                    project.automation, project.drums.steps, -1, automationRecord,
                                    scene = project.scene,
                                    songBars = project.songBars,
                                    stepsPerBar = project.stepsPerBar,
                                    playStep = { if (playing) live.value.currentStep else -1 },
                                    playSongStep = { if (playing) live.value.songStep else -1 },
                                ),
                                overview = { ch -> overviewCh = ch ?: -1; overviewScene = -1 },
                                focus = mixerFocus,
                                onFocused = { mixerFocus = null },
                                automationActions = AutomationActions(
                                    add = rhythmVm::addAutomation,
                                    remove = rhythmVm::removeAutomation,
                                    clear = rhythmVm::clearAutomation,
                                    setPoint = rhythmVm::setAutomationPoint,
                                    toggleRecord = rhythmVm::toggleAutomationRecord,
                                    setMode = rhythmVm::setAutomationMode,
                                    setSongPoint = rhythmVm::setAutomationSongPoint,
                                    edit = rhythmVm::editAutomationLane,
                                    addFilter = rhythmVm::addFilterAutomation,
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
                            val listenSong by rhythmVm.listenSong.collectAsStateWithLifecycle()
                            val followSong by rhythmVm.followSong.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLiveStates() }
                            }
                            // The song's clock and progress: read only when drawn (and once a beat by the clock's words).
                            val songLive = rhythmVm.live.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                            }
                            val all = project.allScenes
                            SessionScreen(
                                SessionScreenUi(
                                    scenes = List(Scene.MAX) { all.getOrNull(it) },
                                    current = project.scene,
                                    pending = sessionUi.pending,
                                    song = project.song,
                                    songIndex = sessionUi.songIndex,
                                    shownSongIndex = sessionUi.shownSongIndex,
                                    playing = transport.playing,
                                    tempoBpm = project.tempoBpm,
                                    beatsPerBar = project.beatsPerBar,
                                    sceneInfo = sceneInfo(project),
                                    rows = timelineRows(project),
                                    upcoming = if (transport.playing) sessionUi.upcoming(project.song.size, project.songLoops) else -1,
                                    listenSong = project.song.isNotEmpty() && listenSong != false,
                                    songLoops = project.songLoops,
                                    followSong = followSong,
                                    songStep = { if (transport.playing) songLive.value.songStep else -1 },
                                    partSteps = project.song.map { project.partSteps(it) },
                                ),
                                SessionActions(
                                    select = rhythmVm::selectScene,
                                    duplicate = rhythmVm::duplicateScene,
                                    newScene = rhythmVm::newScene,
                                    clear = rhythmVm::clearScene,
                                    rename = rhythmVm::renameScene,
                                    moveScene = rhythmVm::moveScene,
                                    addPart = rhythmVm::addSongPart,
                                    insertPart = rhythmVm::insertSongPart,
                                    duplicatePart = rhythmVm::duplicateSongPart,
                                    movePart = rhythmVm::moveSongPart,
                                    removePart = rhythmVm::removeSongPart,
                                    setRepeats = rhythmVm::setSongRepeats,
                                    playPart = rhythmVm::playSongPart,
                                    toggleSong = rhythmVm::toggleSong,
                                    togglePlay = rhythmVm::togglePlay,
                                    setListenSong = rhythmVm::setListenSong,
                                    setSongLoops = rhythmVm::setSongLoops,
                                    setFollowSong = rhythmVm::setFollowSong,
                                    performance = { param, v -> if (NativeEngine.isAvailable) NativeEngine.setPerformance(param, v) },
                                    togglePartTrack = rhythmVm::togglePartTrack,
                                    setPartScene = rhythmVm::setPartScene,
                                    setBeatsPerBar = rhythmVm::setBeatsPerBar,
                                    openLive = { showLive = true },
                                    whatPlays = { scene -> overviewCh = -1; overviewScene = scene ?: -1 },
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
                                    // The live pads play through the remix's strip: its effects.
                                    effects = { fxChannel = 3 },
                                ),
                            )
                        }
                        AppTab.Audio -> Column {
                            // Scrollable: whole words on a small phone or with big text (never "Remi/x").
                            androidx.compose.material3.ScrollableTabRow(selectedTabIndex = audioPage, containerColor = Neon.Surface, contentColor = Neon.Cyan, edgePadding = 8.dp) {
                                listOf(tr("Pistes audio"), tr("Remix"), tr("Accordeur"), tr("Générer")).forEachIndexed { i, label ->
                                    Tab(selected = audioPage == i, onClick = { audioPage = i }, text = { Text(label, maxLines = 1, softWrap = false) }, modifier = Modifier.testTag("audio_page_$i"))
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
                                var granted by remember { mutableStateOf(hasMic(context)) }
                                // Back from the device's settings (« Autoriser le micro » there): read again.
                                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasMic(context) }
                                val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                                    granted = it
                                    if (!it) permissionHelp = MIC_REFUSED
                                }
                                if (granted) {
                                    LaunchedEffect(Unit) {
                                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.runTuner() }
                                    }
                                    com.pocketstudio.app.analysis.TunerView(reading, micOk, settings.englishNotes)
                                } else {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(tr("L'accordeur écoute le micro : autorise-le pour t'accorder."))
                                        Button(onClick = { ask.launch(Manifest.permission.RECORD_AUDIO) }) { Text(tr("Autoriser le micro")) }
                                    }
                                }
                            } else if (audioPage == 0) {
                                val project by rhythmVm.project.collectAsStateWithLifecycle()
                                val recording by rhythmVm.audioRecording.collectAsStateWithLifecycle()
                                val info by rhythmVm.sampleInfo.collectAsStateWithLifecycle()
                                val task by rhythmVm.sampleTask.collectAsStateWithLifecycle()
                                val roundTripMs by Metrics.roundTripMs.collectAsStateWithLifecycle()
                                val listening by com.pocketstudio.app.audio.Headphones.state.collectAsStateWithLifecycle()
                                val recordTarget by rhythmVm.recordTarget.collectAsStateWithLifecycle()
                                val takeSaving by rhythmVm.takeSaving.collectAsStateWithLifecycle()
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
                                        listening = listening,
                                        target = recordTarget,
                                        sceneTitle = project.sceneTitle(project.scene),
                                        songParts = project.song.map { project.sceneTitle(it.scene) },
                                        songBars = project.songBars,
                                        tempoBpm = project.tempoBpm,
                                        saving = takeSaving,
                                        scene = project.scene,
                                        sceneTitles = project.allScenes.indices.map { project.sceneTitle(it) },
                                        partStarts = project.song.indices.map { project.songStepOf(it) },
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
                                        setTarget = rhythmVm::setRecordTarget,
                                        setOnce = rhythmVm::setAudioOnce,
                                        setSong = rhythmVm::setAudioSong,
                                        setOffset = rhythmVm::setAudioOffset,
                                        setScene = rhythmVm::setAudioScene,
                                        playFrom = rhythmVm::playFromTake,
                                        restorePrevious = rhythmVm::restorePreviousTake,
                                    ),
                                )
                            } else {
                            val remixState by remixVm.state.collectAsStateWithLifecycle()
                            val sets by remixVm.sets.collectAsStateWithLifecycle()
                            val modelInstalled by remixVm.modelInstalled.collectAsStateWithLifecycle()
                            val preview by remixVm.preview.collectAsStateWithLifecycle()
                            val remixMemory by remixVm.memoryProblem.collectAsStateWithLifecycle()
                            val remixMemoryNever by remixVm.memoryNever.collectAsStateWithLifecycle()
                            // The free memory changes: read again each time the Remix page is shown.
                            LaunchedEffect(Unit) { remixVm.checkMemory() }
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
                                    memoryProblem = remixMemory,
                                    memoryNever = remixMemoryNever,
                                ),
                                RemixActions(
                                    effects = { fxChannel = 3 },
                                    downloadModel = remixVm::downloadModel,
                                    checkMemory = remixVm::checkMemory,
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
                                            android.widget.Toast.makeText(context, tr("Projet calé : %s", what.joinToString(", ")), android.widget.Toast.LENGTH_SHORT).show()
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
                            androidx.compose.material3.ScrollableTabRow(selectedTabIndex = toolPage.ordinal, containerColor = Neon.Surface, contentColor = Neon.Cyan, edgePadding = 8.dp) {
                                ToolPage.entries.forEach { page ->
                                    Tab(
                                        selected = toolPage == page,
                                        onClick = { toolPage = page; ToolPage.save(context, page) },
                                        text = { Text(page.label, maxLines = 1, softWrap = false) },
                                        modifier = Modifier.testTag("tool_${page.name}"),
                                    )
                                }
                            }
                            when (toolPage) {
                                ToolPage.Aide -> com.pocketstudio.app.settings.HelpPage(
                                    com.pocketstudio.app.settings.HelpActions(
                                        openGuide = { showGuide = true },
                                        showSteps = {
                                            com.pocketstudio.app.onboarding.NextStepPrefs(context, rhythmVm.projectId).reopen()
                                            tab = AppTab.Rythme
                                        },
                                    ),
                                )
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
                                        // The default settings also open every folded card (a beginner may not find a fold again).
                                        reset = {
                                            SettingsStore.reset(context)
                                            (foldStore as? com.pocketstudio.ui.MemoryFoldStore)?.unfoldAll()
                                        },
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
                                    // Leaving the page (another tab, another tool) stops the 440 Hz.
                                    androidx.compose.runtime.DisposableEffect(studioVm) { onDispose { studioVm.stop() } }
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
            title = { Text(tr("Aïe, l'appli est tombée")) },
            text = { Text(tr("Elle s'est fermée brutalement la dernière fois. Envoyer le rapport ? Il est déjà rédigé, tu n'as qu'à appuyer sur Envoyer.")) },
            confirmButton = {
                TextButton(onClick = {
                    val uri = vm.crashReportUri()
                    vm.dismissCrash()
                    uri?.let { openUri(context, it) }
                }) { Text(tr("Envoyer le rapport")) }
            },
            dismissButton = { TextButton(onClick = vm::dismissCrash) { Text(tr("Ignorer")) } },
        )
        return
    }

    when (val u = update) {
        is UpdateState.Available, is UpdateState.Failed -> {
            val info = if (u is UpdateState.Available) u.info else (u as UpdateState.Failed).info
            AlertDialog(
                onDismissRequest = { vm.dismissUpdate(remember = false) },
                title = { Text(tr("Nouvelle version %s", info.versionName)) },
                text = {
                    // Long notes scroll: the buttons always stay on screen.
                    Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                        if (u is UpdateState.Failed) Text(tr("Le téléchargement a échoué, on retente ?"), color = Neon.RedText)
                        Text(info.notes.ifBlank { tr("Une nouvelle version est prête.") })
                    }
                },
                confirmButton = { TextButton(onClick = vm::installUpdate) { Text(tr("Installer")) } },
                dismissButton = { TextButton(onClick = { vm.dismissUpdate() }) { Text(tr("Plus tard")) } },
            )
        }
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text(tr("Téléchargement…")) },
            text = { LinearProgressIndicator(progress = { u.progress }) },
            confirmButton = {},
            dismissButton = { TextButton(onClick = vm::cancelUpdate, modifier = Modifier.testTag("update_cancel")) { Text(tr("Annuler")) } },
        )
        UpdateState.None -> Unit
    }
}

/** The tabs that edit one scene: the top bar shows which, and picks another. */
private val SCENE_TABS = setOf(AppTab.Rythme, AppTab.Acide, AppTab.Synthe)

/** The lines under the top bar keep clear of a side navigation bar and the camera hole (lying down). */
private val SIDE_INSETS: androidx.compose.foundation.layout.WindowInsets
    @Composable get() = androidx.compose.foundation.layout.WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal)

/** The top bar's scene chip: every scene of the session by its title, the one edited, the empty ones. */
internal fun sceneChip(p: Project): SceneChipUi {
    val all = p.allScenes
    val count = maxOf(all.size, p.scene + 1).coerceAtMost(Scene.MAX)
    return SceneChipUi(
        current = p.scene,
        titles = List(count) { p.sceneTitle(it) },
        empty = (0 until count).filter { all.getOrNull(it)?.isEmpty != false }.toSet(),
    )
}

/** What the scene shown is, in a sentence: new and empty, a fresh copy of another, or nothing to say. */
internal fun sceneInfo(p: Project): String {
    val name = Scene.NAMES[p.scene]
    val all = p.allScenes
    val global = if (p.audioTracks.any { it.clip != null } || p.remix.setId != null) " " + tr("Les pistes audio et le remix jouent dans toutes les scènes.") else ""
    return when {
        all.getOrNull(p.scene)?.isEmpty != false -> tr("Scène %s : nouvelle et vide. Remplis-la dans Rythme, Acide ou Synthé.", name) + global
        else -> p.copyOf(p.scene)?.let { tr("Scène %1\$s : copie de la scène %2\$s. Change-la pour en faire une variante, ou « Vider ».", name, Scene.NAMES[it]) } ?: ""
    }
}

private val MIC_REFUSED: String get() = tr("Le micro est refusé : autorise-le dans les réglages de l'appareil (Autorisations › Micro), puis réessaie.")

private fun hasMic(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun openUri(context: Context, uri: Uri) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, tr("Aucun navigateur pour ouvrir GitHub"), Toast.LENGTH_LONG).show()
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

/** What the effects box does, wherever it shows (one box for every sound). */
fun effectsActions(vm: RhythmViewModel): MixerActions = MixerActions(
    setInsertType = vm::setInsertType,
    setInsertParam = vm::setInsertParam,
    setSend = vm::setSend,
    quick = quickFxActions(vm),
    setInsertBypass = vm::setInsertBypass,
    moveInsert = vm::moveInsert,
    kindOf = { ch -> vm.project.value.soundKindOf(ch) },
    longFxCount = { type -> vm.project.value.mixer.longFxCount(type) },
    cpuBusy = vm.cpuBusy,
    setInsertScenes = vm::setInsertScenes,
    scenes = vm.sceneLabels,
    meters = vm.meters,
    watchMeters = vm::pollMeters,
)

/** A strip's quick controls and ready-made effects (hold to listen, starred ones kept on the phone). */
fun quickFxActions(vm: RhythmViewModel): com.pocketstudio.app.mixer.QuickFxActions = com.pocketstudio.app.mixer.QuickFxActions(
    vm::setMacro, vm::setMacroPair, vm::applyFx,
    audition = vm::auditionFx,
    favourites = SettingsStore.favouriteFx,
    toggleFavourite = { id -> SettingsStore.toggleFavouriteFx(vm.getApplication(), id) },
)

/** The timeline rows: drums, acid, then each instrument and audio track in its colour. */
fun timelineRows(p: Project): List<com.pocketstudio.app.session.TimelineRow> =
    listOf(
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.DRUMS, "Batterie", com.pocketstudio.app.live.DRUMS_COLOR),
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.ACID, "Basse acide", androidx.compose.ui.graphics.Color(0xFF81C784)),
    ) + p.instrumentTracks.map { t ->
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.inst(t.id), t.name, com.pocketstudio.ui.trackColor(t.color))
    } + p.audioTracks.map { t ->
        // Where its take is really heard in each part (its scene's parts, its place in the song).
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.audio(t.id), t.name, com.pocketstudio.ui.trackColor(t.color), spans = p.audioSpans(t.id))
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
            tr("Synthé : %s (piste choisie)", com.pocketstudio.app.synth.SYNTH_LABELS.firstOrNull { it.first == target.param }?.second ?: tr("réglage"))
        com.pocketstudio.model.MidiMapping.PLAITS ->
            tr("Plaits : %s (piste choisie)", tr(com.pocketstudio.model.PlaitsPatch.PARAMS.getOrElse(target.param - 1) { "réglage" }))
        com.pocketstudio.model.MidiMapping.PERFORMANCE -> if (target.param == 0) tr("Effet live : filtre") else tr("Effet live : résonance")
        com.pocketstudio.model.MidiMapping.TEMPO -> tr("Tempo (60 à 200 BPM)")
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


/** Every gesture of the Live page, to the ViewModel (a take records them). */
fun liveActionsOf(
    vm: RhythmViewModel,
    back: () -> Unit = {},
    openEffects: (Int) -> Unit = {},
    togglePlay: () -> Unit = vm::togglePlay,
): com.pocketstudio.app.live.LiveActions = com.pocketstudio.app.live.LiveActions(
    back = back,
    togglePlay = togglePlay,
    setQuantum = vm::setLiveQuantum,
    launchPad = vm::launchPad,
    launchRow = vm::launchRow,
    stopTrack = vm::stopLiveTrack,
    stopAll = vm::stopLiveAll,
    press = vm::pressLive,
    retract = vm::retractLive,
    toggleMute = vm::toggleLiveMute,
    performance = vm::livePerformance,
    setReturn = vm::setLiveReturn,
    fx = quickFxActions(vm),
    setSend = vm::setSend,
    openEffects = openEffects,
    setAcid = vm::setLiveAcid,
    holdAcid = vm::holdAcidKnob,
    resetAcid = vm::resetLiveAcid,
    record = vm::togglePerformance,
    setTakeAppend = vm::setTakeAppend,
    undoTake = vm::undoPerformance,
    listenTake = vm::listenPerformance,
    dismissTake = vm::dismissPerformance,
    trackChop = vm::liveTrackChop,
    resetAll = vm::liveResetAll,
    hold = vm::liveHold,
    roll = vm::liveRoll,
    kill = vm::liveKill,
    throwFx = vm::liveThrow,
    riser = vm::liveRiser,
    buildUp = vm::setLiveBuildUp,
    drop = vm::liveDrop,
    setLock = vm::setLiveLock,
    setFxSettings = vm::setLiveFxSettings,
    setInsertParam = vm::setInsertParam,
)

/** The Live page with its state from the ViewModel; every gesture goes back to it. */
@Composable
internal fun LiveRoute(
    vm: RhythmViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    close: () -> Unit,
    openEffects: (Int) -> Unit,
    padsActions: com.pocketstudio.app.session.LivePadsActions,
    liveEditing: Int?,
) {
    val project by vm.project.collectAsStateWithLifecycle()
    val transport by vm.transport.collectAsStateWithLifecycle()
    val grid by vm.liveGrid.collectAsStateWithLifecycle()
    // The clock (30 times a second) is read only where it is shown: this page does not recompose with it.
    val clock = vm.liveClock.collectAsStateWithLifecycle()
    val clockOf = remember(clock) { { clock.value } }
    val muted by vm.liveMuted.collectAsStateWithLifecycle()
    val acid by vm.liveAcid.collectAsStateWithLifecycle()
    val returns by vm.liveReturns.collectAsStateWithLifecycle()
    val take by vm.take.collectAsStateWithLifecycle()
    val takeAppend by vm.takeAppend.collectAsStateWithLifecycle()
    val chops by vm.liveChops.collectAsStateWithLifecycle()
    // The effects (moved at finger speed by the DJ filter and the Montée): read only where shown.
    val liveFx = vm.liveFx.collectAsStateWithLifecycle()
    val liveFxOf = remember(liveFx) { { liveFx.value } }
    val liveStates by vm.liveStates.collectAsStateWithLifecycle()
    val sampleTask by vm.sampleTask.collectAsStateWithLifecycle()
    // The output spectrum behind the pads XY: read only where drawn (the page does not recompose with it).
    val spectrum = vm.spectrum.collectAsStateWithLifecycle()
    val spectrumOf = remember(spectrum) { { spectrum.value } }
    // Measured only while a pad XY shows it (not on Tenus, Montée, the settings...).
    val spectrumUsers = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val settings by SettingsStore.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            kotlinx.coroutines.coroutineScope {
                launch { vm.pollLiveGrid() }
                launch { vm.pollLiveStates() }
                launch {
                    androidx.compose.runtime.snapshotFlow { spectrumUsers.intValue > 0 }.collectLatest { shown -> if (shown) vm.pollSpectrum() }
                }
            }
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(com.pocketstudio.app.live.LocalSpectrumUsers provides spectrumUsers) {
    com.pocketstudio.app.live.LiveScreen(
        com.pocketstudio.app.live.LiveScreenUi(
            project, grid, playing = transport.playing, muted = muted, acid = acid, returns = returns, take = take, takeAppend = takeAppend,
            chops = chops, takeUndoable = vm.canUndoPerformance(), clockOf = clockOf,
            spectrum = spectrumOf, beatHaptics = settings.liveBeatHaptics, fxOf = liveFxOf,
        ),
        liveActionsOf(
            vm, back = close, openEffects = openEffects,
            togglePlay = {
                // ▶ here plays what the grid shows: the scene, not the song.
                if (transport.playing) vm.togglePlay() else vm.launchRow(project.scene)
            },
        ),
        pads = com.pocketstudio.app.session.LivePadsUi(
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
        padsActions = padsActions,
    )
    }
}
