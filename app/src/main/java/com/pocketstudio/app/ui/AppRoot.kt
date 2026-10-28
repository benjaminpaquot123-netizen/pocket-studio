package com.pocketstudio.app.ui

import com.pocketstudio.app.report.isStorageFull
import com.pocketstudio.app.report.forUser
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.pocketstudio.ui.ScaledAlertDialog
import com.pocketstudio.ui.ScaledBottomSheet
import com.pocketstudio.ui.ScaledDialog

import com.pocketstudio.model.guide.guideTracks
import com.pocketstudio.model.partMarks
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
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
import com.pocketstudio.app.library.toGuidePages
import com.pocketstudio.app.library.toSongGuide
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
import com.pocketstudio.model.partKeys
import com.pocketstudio.model.songLanesOf
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

/** The tabs with their own record bar (« ● Enregistrer », then the take's card). */
private val RECORD_BAR_TABS = setOf(AppTab.Rythme, AppTab.Acide, AppTab.Synthe)

/** What the top bar's ● records on [tab] (outside the Audio tab, where it is the microphone's). */
fun takeSourceOf(tab: AppTab): com.pocketstudio.app.record.TakeSource = when (tab) {
    AppTab.Synthe -> com.pocketstudio.app.record.TakeSource.KEYS
    AppTab.Acide -> com.pocketstudio.app.record.TakeSource.ACID
    else -> com.pocketstudio.app.record.TakeSource.PADS
}

enum class AppTab(val icon: ImageVector) {
    Rythme(Icons.Filled.GridView),
    Acide(Icons.Filled.GraphicEq),
    Synthe(Icons.Filled.Piano),
    Mixeur(Icons.Filled.Equalizer),
    Audio(Icons.Filled.Mic),
    Morceau(Icons.Filled.QueueMusic),
    Outils(Icons.Filled.Tune),
    /** Phase 36: the Live page, a space of its own (out of the creation path). */
    Live(Icons.Filled.Apps),
    /** Phase 36, Mode simple: « Envoyer », the « Fini ? » page (checks, Partager, WAV). */
    Envoyer(Icons.AutoMirrored.Filled.Send);

    /** The screen's name in the interface's language (the spaces and the Mode simple's tabs have their own: [Space], [SimpleTab]). */
    val label: String get() = when (this) {
        Rythme -> tr("Rythme")
        Acide -> tr("Acide")
        Synthe -> tr("Synthé")
        Mixeur -> tr("Mixeur")
        Audio -> tr("Audio")
        Morceau -> tr("Morceau")
        Outils -> tr("Outils")
        Live -> tr("Live")
        Envoyer -> tr("Envoyer")
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
    // The app's own ViewModels outlive a change of project (AppViewModels); the others are the project's.
    val mainVm: MainViewModel = com.pocketstudio.app.projects.appViewModel()
    val studioVm: StudioViewModel = viewModel()
    val latencyVm: LatencyViewModel = viewModel()
    val recetteVm: RecetteViewModel = com.pocketstudio.app.projects.appViewModel()
    val midiVm: MidiViewModel = com.pocketstudio.app.projects.appViewModel()
    val rhythmVm: RhythmViewModel = viewModel()
    // The screens ran a moment on the project opened: it did not crash the app as it opened (ProjectOpening).
    LaunchedEffect(rhythmVm) {
        androidx.compose.runtime.withFrameNanos { }
        kotlinx.coroutines.delay(3000)
        com.pocketstudio.app.rhythm.ProjectOpening.settled(context)
    }
    // Phase 39: the controller's first eight knobs follow the screen shown.
    val knobsVm: com.pocketstudio.app.midi.KnobsViewModel = viewModel()
    remember(knobsVm, rhythmVm) { knobsVm.attach(com.pocketstudio.app.midi.rhythmKnobHost(rhythmVm)) }
    val remixVm: RemixViewModel = com.pocketstudio.app.projects.appViewModel()
    val generateVm: com.pocketstudio.app.generate.GenerateViewModel = com.pocketstudio.app.projects.appViewModel()
    val libraryVm: com.pocketstudio.app.library.SoundLibraryViewModel = viewModel()
    // « Signaler un bug »: opened from Plus, the Mode simple's ⋯, a long press on « ? » and the crash banner.
    val bugVm: com.pocketstudio.app.report.BugReportViewModel = viewModel()
    val reportBug: (String) -> Unit = { from -> bugVm.request(from) }
    // The demos of « Nouveau projet » are rendered on a private engine described by the open project's own sync.
    DisposableEffect(libraryVm, rhythmVm) {
        libraryVm.demoSync = { p -> rhythmVm.syncEngine(p) }
        onDispose { libraryVm.demoSync = null }
    }
    val tabStates = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    // Phase 36: the Mode simple (four tabs) or the five spaces; only how the project is shown.
    val navSettings by SettingsStore.state.collectAsStateWithLifecycle()
    val simple = navSettings.simpleMode
    // The screen shown. A project with a song opens on it (Structure, Mon morceau), else on the drums.
    var tab by rememberSaveable {
        mutableStateOf(if (SettingsStore.state.value.simpleMode || rhythmVm.project.value.song.isNotEmpty()) AppTab.Morceau else AppTab.Rythme)
    }
    // Where the navigation was left: the Sons space opens on its last editor, Structure on its last view.
    val navPrefs = remember { NavPrefs(context) }
    var lastSons by rememberSaveable { mutableStateOf(navPrefs.lastSons) }
    var partsView by rememberSaveable { mutableStateOf(navPrefs.partsView) }
    LaunchedEffect(tab) { if (tab in SONS_TABS) { lastSons = tab; navPrefs.lastSons = tab } }
    // Switching modes keeps the screen when the other mode has it (nothing in the project changes).
    LaunchedEffect(simple) {
        if (simple && SimpleTab.of(tab) == null && tab != AppTab.Live && tab != AppTab.Outils && tab != AppTab.Acide && tab != AppTab.Audio) tab = AppTab.Morceau
        if (!simple && tab == AppTab.Envoyer) tab = AppTab.Morceau
    }
    // « ? qui étiquette l'écran », and where each main control is for it and for the Guide.
    var helpLabels by rememberSaveable { mutableStateOf(false) }
    val spots = remember { com.pocketstudio.app.learn.SpotRegistry() }
    // Le Guide's panel (G2), opened by itself on a song just made (G5: « Ton morceau est prêt » is its first card).
    var guideOpen by rememberSaveable(key = "guide_open") { mutableStateOf(false) }
    // G5: the next song the assistant makes is « Mon premier morceau » (the welcome's guided song).
    var firstSongNext by rememberSaveable(key = "first_song_next") { mutableStateOf(false) }
    // G5: a song just made (or « Mon premier morceau ») opens the Guide on its first card by itself,
    // once per project and visit (« Ton morceau est prêt » was a card over the screen before).
    // Followed apart from the screen (an edit does not recompose the whole app for it).
    var welcomedFor by rememberSaveable(key = "guide_welcomed_for") { mutableStateOf<String?>(null) }
    LaunchedEffect(rhythmVm) {
        rhythmVm.project.collect { p ->
            if (welcomedFor != rhythmVm.projectId && com.pocketstudio.model.guide.GuideStart.welcomes(p)) {
                welcomedFor = rhythmVm.projectId
                guideOpen = true
            }
        }
    }
    // G6 (§3.6): back a day or more after the Guide was last used, it opens by itself on « Te revoilà »
    // (where the song was, what is done, what is left), once per project and visit, as the welcome above.
    LaunchedEffect(rhythmVm) {
        rhythmVm.guide.ui.collect { ui ->
            if (!ui.loading && ui.back && welcomedFor != rhythmVm.projectId) {
                welcomedFor = rhythmVm.projectId
                guideOpen = true
            }
        }
    }
    // G5: a user mid-mission (the old « missions » settings) lands on the Guide's card of the step reached.
    // Only once a project is open (G5 review: with none, the progress would be dropped).
    LaunchedEffect(rhythmVm.projectId) {
        if (CurrentProject.isNone(rhythmVm.projectId)) return@LaunchedEffect
        com.pocketstudio.app.guide.GuideMigration.missions(context)?.let { (step, skipped) ->
            rhythmVm.guide.fromMissions(step, skipped)
            guideOpen = true
        }
    }
    // « Ce qui joue »: -2 closed, -1 the whole song, else a scene; [overviewCh]: one track's (-1: all).
    var overviewScene by rememberSaveable { mutableStateOf(-2) }
    var overviewCh by rememberSaveable { mutableStateOf(-1) }
    // Where « Ce qui joue » sent the user in the mixer (a lane, the master, the buses).
    var mixerFocus by remember { mutableStateOf<com.pocketstudio.app.mixer.Where?>(null) }
    // « Voir le potard » from an automation lane: the 303 knob to show on the Acide tab.
    var acidFocus by remember { mutableStateOf<Int?>(null) }
    // The track being worked on (the last of Rythme, Acide, Synthé visited): « Automatiser un
    // réglage » starts on it.
    var workChannel by rememberSaveable { mutableStateOf(-1) }
    LaunchedEffect(tab) {
        when (tab) {
            AppTab.Rythme -> workChannel = 0
            AppTab.Acide -> workChannel = com.pocketstudio.model.AutoTarget.ACID_CHANNEL
            AppTab.Synthe -> workChannel = com.pocketstudio.model.InstrumentTrack.mixerChannel(rhythmVm.selectedTrack.value)
            else -> {}
        }
    }
    var showProjects by rememberSaveable { mutableStateOf(false) }
    // « Mes projets » opened on « Nouveau projet » alone (« Nouveau morceau » of « Que veux-tu faire ? »).
    var projectsNewOnly by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showProjects) { if (!showProjects) projectsNewOnly = false }
    // « Que veux-tu faire ? » at each start of the app (not at the first: the welcome asks), kept
    // through rotations and the rebuild that opening a project does (see LaunchRule).
    val launch = com.pocketstudio.app.onboarding.rememberLaunchGate(firstLaunch = !SettingsStore.state.value.guideSeen)
    // A long press on the project's name in the top bar: rename it there.
    var renamingProject by rememberSaveable { mutableStateOf(false) }
    // A touch on the tempo (under the top bar, or on Rythme): the tempo sheet.
    var tempoSheetOpen by rememberSaveable { mutableStateOf(false) }
    // ⤴ « Prêt à partager ? » (top bar, Mes projets, the steps, « Ton morceau est prêt »): the
    // checks, the name and M4A or WAV; then the export's progress and Android's share sheet.
    val sharePrefs = remember { com.pocketstudio.app.export.SharePrefs(context) }
    var showShare by rememberSaveable { mutableStateOf(false) }
    var showVideo by rememberSaveable { mutableStateOf(false) }
    // « Changer un son »: the sounds of an instrument track in a sheet (null: closed).
    var soundChooserTrack by rememberSaveable { mutableStateOf<Int?>(null) }
    // G3: a track's sheet (its Guide key) and an instrument track's layers editor.
    var trackSheetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var layersTrack by rememberSaveable { mutableStateOf<Int?>(null) }
    var songChordsOpen by rememberSaveable { mutableStateOf(false) }
    val shareSong: () -> Unit = { showShare = true }
    com.pocketstudio.app.export.ShareSongProgress(rhythmVm, wav = sharePrefs.format == com.pocketstudio.app.rhythm.ExportFormat.Wav)
    val projectsVm: ProjectsViewModel = viewModel()
    val pickProjectZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) projectsVm.importZip(uri)
    }
    val openScope = rememberCoroutineScope()
    // Another project: save this one, remember the choice, then rebuild every screen on it.
    // [deleteAfter]: the project left goes to the trash once the next one is open (the open
    // project itself deleted from « Mes projets », which then stays on screen).
    // Testers (lot « tuto 3 »): while another project opens (the activity is rebuilt on it), a plain background
    // rather than the project left behind (« Guide · Étape 7/18 », 120 BPM, a moment before the tutorial's own).
    var switching by remember { mutableStateOf(false) }
    LaunchedEffect(switching) { if (switching) { kotlinx.coroutines.delay(SWITCH_COVER_MS); switching = false } }
    val switchProject: (String, String?) -> Unit = { id, deleteAfter ->
        openScope.launch {
            rhythmVm.keepTake()  // a microphone take in progress lands on its track first
            rhythmVm.flushNow()
            // Not saved (a full phone): stay here rather than reopen this project without its latest edits.
            val failed = rhythmVm.session.lastSaveError.value
            if (failed != null) {
                val text = if (failed.isStorageFull()) tr("Le projet n'a pas pu être sauvegardé (appareil plein ?) : libère de la place avant d'en ouvrir un autre.")
                    else tr("Le projet n'a pas pu être sauvegardé (%s) : il reste ouvert, réessaie dans un instant.", failed.forUser())
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                switching = false
                return@launch
            }
            switching = true
            showProjects = deleteAfter != null  // remembered across the rebuild
            projectsNewOnly = false
            launch.settled()  // the project opened is the choice: its screens never go back to « Que veux-tu faire ? »
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
        ScaledDialog(
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
            val name = if (com.pocketstudio.model.Mixer.isPad(fxChannel)) {
                val lane = fxChannel - com.pocketstudio.model.Mixer.FIRST_PAD
                tr("pad %1\$d (%2\$s)", lane + 1, fxProject.drums.lanes.getOrNull(lane)?.sample?.name?.let { com.pocketstudio.model.DefaultNames.shownSample(it) } ?: com.pocketstudio.app.rhythm.DrumKitInfo.names[lane])
            } else {
                com.pocketstudio.model.DefaultNames.shown(mixerStrips(fxProject).firstOrNull { it.ch == fxChannel }?.name ?: "Piste")
            }
            ScaledAlertDialog(
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
                when {
                    w is com.pocketstudio.app.mixer.Where.Effects -> fxChannel = w.ch
                    // Audit final (05 #12, 08): the Mode simple stays in its four tabs: the master's
                    // level is on « Envoyer » (« Baisser le volume général »), the rest has no « Modifier ».
                    simple -> tab = AppTab.Envoyer
                    else -> { tab = AppTab.Mixeur; mixerFocus = w }
                }
            },
            canGo = { w -> !simple || w is com.pocketstudio.app.mixer.Where.Effects || w == com.pocketstudio.app.mixer.Where.Master },
            dismiss = { overviewScene = -2 },
        )
    }
    // G5: the assistant made a song: it starts in the Guide (« Ton morceau est prêt » is its first
    // card, the panel opens on it), its guide kept with it and read from that card.
    var songGuideOpen by rememberSaveable(key = "song_guide_open") { mutableStateOf(false) }
    val songMade: (Project, List<com.pocketstudio.app.settings.GuidePage>) -> Project = { project, guide ->
        val mode = if (firstSongNext) com.pocketstudio.model.guide.GuideStart.FIRST else com.pocketstudio.model.guide.GuideStart.SONG
        firstSongNext = false
        com.pocketstudio.model.guide.GuideStart.begin(project, mode, guide.toSongGuide())
    }
    if (songGuideOpen) rhythmVm.project.collectAsStateWithLifecycle().value.guide.songGuide.takeIf { it.isNotEmpty() }?.let { saved ->
        com.pocketstudio.app.settings.GuideDialog(
            onClose = { songGuideOpen = false }, pages = saved.toGuidePages(), tag = "song_guide", done = tr("▶ Écouter mon morceau"),
            // The guide ends on the song itself, from its first part.
            onDone = {
                songGuideOpen = false
                if (!rhythmVm.transport.value.playing) {
                    if (rhythmVm.project.value.song.isNotEmpty()) rhythmVm.toggleSong() else rhythmVm.togglePlay()
                }
            },
        )
    }
    val wizardUi by libraryVm.wizard.collectAsStateWithLifecycle()
    // Closed without a song (G5 review): the next song is not « Mon premier morceau » by mistake.
    val closeWizard: () -> Unit = { firstSongNext = false; libraryVm.closeWizard() }
    if (wizardUi.open) {
        val packsUi by libraryVm.packs.collectAsStateWithLifecycle()
        val previewing by libraryVm.playing.collectAsStateWithLifecycle()
        val preparing by libraryVm.preparing.collectAsStateWithLifecycle()
        ScaledDialog(
            onDismissRequest = closeWizard,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            androidx.activity.compose.BackHandler { if (wizardUi.step > 0) libraryVm.back() else closeWizard() }
            androidx.compose.material3.Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                com.pocketstudio.app.library.WizardScreen(
                    wizardUi, packsUi, previewing,
                    com.pocketstudio.app.library.WizardActions(
                        close = closeWizard,
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
                        chooseBassGrid = libraryVm::chooseBassGrid,
                        preview = libraryVm::preview,
                        download = libraryVm::downloadPack,
                        cancelDownload = libraryVm::cancelDownload,
                        deletePack = libraryVm::deletePack,
                        create = {
                            libraryVm.create { project, sounds, guide ->
                                projectsVm.create(songMade(project, guide), sounds, failed = libraryVm::createFailed) { id ->
                                    guideOpen = true
                                    libraryVm.closeWizard()
                                    tab = AppTab.Morceau  // the song is ready to hear: land on it (kept across the rebuild)
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
    // « Tuto de A à Z »: an empty project whose tutorial starts at once (its level sets the Mode simple or not).
    val startTuto: (String, com.pocketstudio.model.tuto.TutoLevel) -> Unit = { style, level ->
        // Only the tutorial's project is seen: from now on, a plain background until it opens.
        switching = true
        rhythmVm.stop()
        SettingsStore.update(context) { it.copy(simpleMode = level == com.pocketstudio.model.tuto.TutoLevel.Facile) }
        // Owner (2026-10-28): truly empty, the style's drum sounds already on the pads, opened on the first steps' screen.
        val blank = com.pocketstudio.model.tuto.TutoTarget.of(style)?.blank() ?: Project(name = "")
        projectsVm.create(blank) { id ->
            com.pocketstudio.app.tuto.PrefsTutoStore(context).save(id, com.pocketstudio.model.tuto.TutoProgress(style, level))
            showProjects = false
            projectsNewOnly = false
            tab = AppTab.Rythme
            openProject(id)
        }
    }
    // « J'apprends » from the welcome: its chooser over the screen.
    var tutoChooserOpen by rememberSaveable { mutableStateOf(false) }
    if (tutoChooserOpen) {
        ScaledDialog(onDismissRequest = { tutoChooserOpen = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            androidx.compose.material3.Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                com.pocketstudio.app.tuto.TutoChooser(
                    start = { style, level -> tutoChooserOpen = false; startTuto(style, level) },
                    close = { tutoChooserOpen = false },
                    simple = SettingsStore.state.value.simpleMode,
                )
            }
        }
    }
    // Lot 1: no project open (the last one deleted): « Mes projets » and its empty state stay on screen.
    val noProject = CurrentProject.isNone(rhythmVm.projectId)
    // Leaving the app's screens with no project: back to the phone (nothing to go back to here).
    val leaveApp = { context.findActivity()?.moveTaskToBack(true); Unit }
    if (showProjects || noProject) {
        val projectsUi by projectsVm.state.collectAsStateWithLifecycle()
        val quickUi by libraryVm.quick.collectAsStateWithLifecycle()
        val previewingDemo by libraryVm.playing.collectAsStateWithLifecycle()
        val preparingDemo by libraryVm.preparing.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { projectsVm.refresh() }
        // Retour: back where it was opened from (« Que veux-tu faire ? » or the project).
        val closeProjects = {
            showProjects = false
            projectsNewOnly = false
            // With no project there is nothing under it: « Que veux-tu faire ? » when it came from there, else out of the app.
            if (!launch.returned() && noProject) leaveApp()
            Unit
        }
        androidx.activity.compose.BackHandler { closeProjects() }
        ScaledDialog(
            onDismissRequest = closeProjects,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            androidx.compose.material3.Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                // Built afresh when it opens on « Nouveau projet » alone or not: with no project (lot 1) the
                // list stays on screen under « Que veux-tu faire ? », and its page must follow that choice.
                androidx.compose.runtime.key(projectsNewOnly) { ProjectsScreen(
                    projectsUi, rhythmVm.projectId, PROJECT_STARTERS.map { it.first },
                    quick = quickUi.copy(
                        demoPlaying = (previewingDemo as? com.pocketstudio.app.library.SoundRef.Demo)?.index,
                        demoPreparing = (preparingDemo as? com.pocketstudio.app.library.SoundRef.Demo)?.index,
                    ),
                    newOnly = projectsNewOnly,
                    actions = ProjectsActions(
                        previewDemo = { libraryVm.preview(com.pocketstudio.app.library.SoundRef.Demo(it)) },
                        stopDemo = libraryVm::stopDemoPreview,
                        back = closeProjects,
                        // The open project touched: on it, as « Continuer » does.
                        resume = { showProjects = false; projectsNewOnly = false; launch.settled() },
                        open = { openProject(it.id) },
                        create = { i -> projectsVm.create(PROJECT_STARTERS[i].second()) { id -> openProject(id) } },
                        wizard = {
                            rhythmVm.stop()
                            libraryVm.openWizard()
                        },
                        learn = { style, level -> startTuto(style, level) },
                        remix = if (noProject) null else ({
                            showProjects = false
                            projectsNewOnly = false
                            launch.settled()
                            com.pocketstudio.app.remix.SeparationJob.requestOpen()
                        }),
                        // A style's whole song in one touch: the assistant's own song, its guide after.
                        quickStyle = { id ->
                            rhythmVm.stop()
                            libraryVm.quickCreate(id) { project, sounds, guide ->
                                projectsVm.create(songMade(project, guide), sounds, failed = libraryVm::quickFailed) { newId ->
                                    guideOpen = true
                                    libraryVm.quickDone()
                                    tab = AppTab.Morceau  // the song is ready to hear: land on it (kept across the rebuild)
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
                        exportRaw = { p ->
                            projectsVm.exportRaw(p) { file -> WavExporter.share(context, listOf(file), tr("Envoyer le projet")) }
                        },
                        restoreOpening = projectsVm::restoreOpening,
                        importZip = { pickProjectZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                        // The open project too (an assistant's song included): another one opens first
                        // (the latest, else a new blank one), then it goes to the trash.
                        delete = { p ->
                            if (p.id != rhythmVm.projectId) projectsVm.delete(p)
                            else {
                                rhythmVm.stop()
                                val next = projectsUi.projects.firstOrNull { it.id != p.id }
                                openScope.launch {
                                    // So « untouched » is judged on what is really saved; a save that fails (phone full)
                                    // keeps the project where it is rather than risk deleting unsaved work.
                                    if (!rhythmVm.flushNow()) {
                                        projectsVm.say(tr("Le projet n'a pas pu être enregistré : il reste là (stockage plein ?)."))
                                        return@launch
                                    }
                                    // The latest other project opens; the last one closes (lot 1: zero projects is
                                    // allowed, « Mes projets » then shows its empty state). A blank one nobody touched
                                    // is deleted outright, never trashed (ProjectsViewModel.delete).
                                    switchProject(next?.id ?: CurrentProject.NONE, p.id)
                                }
                            }
                        },
                        undoDelete = projectsVm::undoDelete,
                        restore = projectsVm::restore,
                        deleteForever = projectsVm::deleteForever,
                        emptyTrash = projectsVm::emptyTrash,
                        dismissDeleted = projectsVm::dismissDeleted,
                        dismissMessage = projectsVm::dismissMessage,
                        shareSong = { showProjects = false; launch.settled(); shareSong() },
                    ),
                ) }
            }
        }
    }
    val settings by SettingsStore.state.collectAsStateWithLifecycle()
    var showGuide by rememberSaveable { mutableStateOf(false) }
    // A crash of the last session only shows a banner under the bar: it holds nothing back.
    val pendingCrash by mainVm.crash.collectAsStateWithLifecycle()
    if (showGuide) {
        GuideDialog(onClose = {
            showGuide = false
            SettingsStore.update(context) { it.copy(guideSeen = true) }
        }, pages = com.pocketstudio.app.settings.guidePages(settings.simpleMode))
    }
    // The first launch: three big choices (guided song, demo, empty project), not pages of text.
    val firstLaunchSeen = { SettingsStore.update(context) { it.copy(guideSeen = true) } }
    // Phase 36: a first launch starts in the Mode simple (the welcome says so, one touch changes it).
    LaunchedEffect(Unit) { if (!SettingsStore.state.value.guideSeen && !SettingsStore.state.value.simpleMode) SettingsStore.update(context) { it.copy(simpleMode = true) } }
    com.pocketstudio.app.onboarding.FirstLaunch(
        // After the opening (BrandIntro): a window would cover it.
        show = !settings.guideSeen && !com.pocketstudio.app.onboarding.Intro.playing,
        simple = settings.simpleMode,
        actions = com.pocketstudio.app.onboarding.WelcomeActions(
            chooseMode = { v -> SettingsStore.update(context) { it.copy(simpleMode = v) } },
            // The guided song is « Mon premier morceau »: its song starts on the Guide's first cards (G5).
            guided = { firstLaunchSeen(); firstSongNext = true; rhythmVm.stop(); libraryVm.openWizard() },
            learn = { firstLaunchSeen(); tutoChooserOpen = true },
            demo = { firstLaunchSeen(); projectsVm.create(com.pocketstudio.model.Demos.ALL.first().third()) { id -> openProject(id) } },
            empty = { firstLaunchSeen(); projectsVm.create(Project(name = "")) { id -> openProject(id) } },
            later = firstLaunchSeen,
        ),
        playDemo = { if (rhythmVm.project.value.song.isNotEmpty()) rhythmVm.toggleSong() else rhythmVm.togglePlay() },
    )
    // Réglages › Écran toujours allumé: only while the music plays.
    // Revue neuve (MOYEN 7): only whether it plays is read here (not each change of the transport).
    val screenPlaying by remember(rhythmVm) { rhythmVm.transport.map { it.playing }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(rhythmVm.transport.value.playing)
    val screenPads by rhythmVm.padsAlone.collectAsStateWithLifecycle()
    val musicPlays = screenPlaying || screenPads  // live pads play without ▶ too
    LaunchedEffect(musicPlays, settings.keepScreenOn) {
        val window = context.findActivity()?.window ?: return@LaunchedEffect
        val flag = android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (musicPlays && settings.keepScreenOn) window.addFlags(flag) else window.clearFlags(flag)
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
        remixVm.pause()
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
    // G3 review: a track's sheet or its layers show their notice in their own line, never a window
    // over them that blocks every touch.
    notice?.takeIf { trackSheetKey == null && layersTrack == null }?.let { text ->
        ScaledAlertDialog(
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
            val liveCopy = com.pocketstudio.app.report.LiveCopyClock()
            while (true) {
                val info = NativeEngine.streamInfo()
                Metrics.updateStream(info)
                // The bug report's health journal (4 readings a second, atomics only).
                val now = android.os.SystemClock.elapsedRealtime()
                com.pocketstudio.app.report.HealthMonitor.feed(info, now, System.currentTimeMillis())
                // A copy of the journal on disk every 10 s while it or the sound moves: a native crash
                // leaves no time to write one. Built and written off the main thread (a few hundred KB).
                if (liveCopy.due(now)) {
                    launch(kotlinx.coroutines.Dispatchers.IO) { com.pocketstudio.app.report.JournalFiles.saveLive(context.filesDir) }
                }
                delay(100)
            }
        }
    }

    // A permission refused (twice, Android stops asking): say why nothing happens and where to allow it.
    var permissionHelp by rememberSaveable { mutableStateOf<String?>(null) }
    permissionHelp?.let { text ->
        ScaledAlertDialog(
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
    // Revue neuve (MOYEN 7): whether a take runs, not its level every 50 ms.
    val anyTake by remember(rhythmVm) { rhythmVm.audioRecording.map { it != null }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(rhythmVm.audioRecording.value != null)
    LaunchedEffect(anyRecording || anyTake) {
        if (!(anyRecording || anyTake) || notificationsAsked || android.os.Build.VERSION.SDK_INT < 33) return@LaunchedEffect
        notificationsAsked = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Audit final (02 M4): a Remix runs in the background (« tu peux éteindre l'écran »): its progress
    // and its end are notifications, asked for once per launch when a separation or the model's
    // download starts (a user who only remixes never records).
    val askNotifications: () -> Unit = {
        if (!notificationsAsked && android.os.Build.VERSION.SDK_INT >= 33) {
            notificationsAsked = true
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) midiVm.scanBluetooth()
        else permissionHelp = tr("Le Bluetooth (appareils à proximité) est refusé : autorise-le dans les réglages de l'appareil pour trouver un clavier MIDI.")
    }
    // « Apprendre mes pads »: the notes heard so far, kept for the device once done.
    val padsHeard = remember { mutableListOf<Int>() }
    val finishPads: () -> Unit = {
        val name = midiVm.state.value.controller?.name
        if (name != null && padsHeard.isNotEmpty()) knobsVm.setPads(name, padsHeard.toList())
        padsHeard.clear()
        midiVm.learnPads(false)
    }
    // MIDI clock, start and stop drive the project's tempo and transport (main thread).
    LaunchedEffect(Unit) {
        midiVm.commands.collect { c ->
            when (c) {
                // Following another machine's clock: no count-in, Start from the top (lined up again), Continue from where it stopped.
                MidiCommand.Start -> rhythmVm.midiStart(fromTop = true)
                MidiCommand.Continue -> rhythmVm.midiStart(fromTop = false)
                MidiCommand.Stop -> if (rhythmVm.transport.value.playing) rhythmVm.togglePlay()
                is MidiCommand.Tempo -> rhythmVm.followClockTempo(c.bpm)
                is MidiCommand.Note -> rhythmVm.midiNote(c.channel, c.pitch, c.velocity, c.on)
                is MidiCommand.Control -> if (!rhythmVm.midiControl(c.channel, c.controller, c.value)) {
                    val m = midiVm.state.value
                    knobsVm.control(c.controller, c.value, m.controller?.name ?: m.sources.firstOrNull { it.id == m.connected }?.name, c.channel)
                }
                // The Live page's pads: launches, mutes, live pads (the drums are played on the MIDI thread).
                is MidiCommand.Pad -> knobsVm.pad(c.index)
                is MidiCommand.PadLearnt -> {
                    if (c.packed !in padsHeard) padsHeard += c.packed
                    midiVm.padsLearnt(padsHeard.size)
                    if (padsHeard.size >= com.pocketstudio.model.midi.Controllers.PADS) finishPads()
                }
            }
        }
    }
    // The controller connected: its profile and its own pads (as « Apprendre mes pads » kept them).
    LaunchedEffect(Unit) {
        kotlinx.coroutines.flow.combine(midiVm.state, knobsVm.settings) { m, s -> m.controller to s }
            .distinctUntilChanged()
            .collect { (c, s) ->
                knobsVm.setController(c?.name, c?.profile)
                midiVm.pads = com.pocketstudio.app.midi.PadMap(c?.profile, c?.name?.let { s.pads[it] })
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
    // Audit final (03 M5, 15): « Refaire mes. X-Y » asked for the microphone first ("track,from,to"), then redone.
    var audioPunch by rememberSaveable { mutableStateOf<String?>(null) }
    var audioImportTrack by rememberSaveable { mutableIntStateOf(0) }
    val audioMicPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val punch = audioPunch?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 }
        audioPunch = null
        when {
            !granted -> permissionHelp = MIC_REFUSED
            punch != null -> rhythmVm.recordPunch(punch[0], punch[1], punch[2])
            else -> rhythmVm.recordAudio(audioRecordTrack)
        }
    }
    val pickTrackAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importAudio(audioImportTrack, uri)
    }
    val pickLiveAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importLive(liveImportPad, uri)
    }
    val pickSong = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            remixVm.separate(uri)
            askNotifications()
        }
    }
    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) rhythmVm.importZip(uri)
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) rhythmVm.importFolder(uri)
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Phase 36: the Live page is a space of its own (the bottom bar stays; its own bar replaces the
    // app's). It goes back to where it was opened from.
    val showLive = tab == AppTab.Live
    var liveFrom by rememberSaveable { mutableStateOf(AppTab.Morceau) }
    var liveImmersive by remember { mutableStateOf(false) }
    // The Live page's panel shown (Effets, Acide, Pads, Notes): a MIDI controller follows it.
    var liveTab by rememberSaveable { mutableStateOf("Effets") }
    LaunchedEffect(showLive) { if (!showLive) liveImmersive = false }
    val openLive: () -> Unit = { if (tab != AppTab.Live) liveFrom = tab; tab = AppTab.Live }
    // The live pad being set up (Morceau and the Live page's Pads share it).
    var liveEditing by rememberSaveable { mutableStateOf<Int?>(null) }
    // Leaving the Live page: the « Morceau écrit » line goes with it (its Annuler would come back
    // later, after other changes), and nothing set there stays on unseen (the Montée, what the
    // lock kept). During a take the page asks first (LiveScreen's own Back). A rotation keeps all.
    val closeLive = { rhythmVm.leaveLive(); rhythmVm.dismissPerformance(); tab = liveFrom.takeIf { it != AppTab.Live } ?: AppTab.Morceau }
    // A space chosen in the bottom bar. Leaving the Live page that way ends its take first (a take
    // never goes on unseen behind another page; it is kept as a new version of the song).
    val go: (AppTab) -> Unit = { t ->
        if (tab == AppTab.Live && t != AppTab.Live) rhythmVm.leaveLiveEndingTake()
        tab = t
    }
    var toolPage by rememberSaveable { mutableStateOf<ToolPage?>(null) }
    // Lot 2: the Remix (Sons › Voix, its second page), « mis en avant »: one touch from « Que veux-tu
    // faire ? », a chip in Sons, a line of the Mode simple's « ⋯ ».
    val openRemix: () -> Unit = { audioPage = 1; go(AppTab.Audio) }
    // The separation's notification touched: its Remix page (audit 02 M5).
    val remixAsked by com.pocketstudio.app.remix.SeparationJob.openRemix.collectAsStateWithLifecycle()
    LaunchedEffect(remixAsked) {
        if (remixAsked) {
            com.pocketstudio.app.remix.SeparationJob.openHandled()
            openRemix()
        }
    }
    // Audit final (03 G4): « Ma voix », the audio tracks' page (a first track made when there is none).
    val openVoice: () -> Unit = {
        if (rhythmVm.project.value.audioTracks.isEmpty()) rhythmVm.addAudioTrack()
        audioPage = 0
        go(AppTab.Audio)
    }
    // Lot 2: the Mode simple's Clavier records « ma mélodie » into the song's melody track (the
    // first track, « Synthé », was shown before), once per visit: a track chosen there stays.
    // Once per project (kept through a rotation): a track chosen there afterwards stays.
    var melodyShownFor by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(simple, tab) {
        if (simple && tab == AppTab.Synthe && melodyShownFor != rhythmVm.projectId) {
            melodyShownFor = rhythmVm.projectId
            rhythmVm.melodyTrackId()?.let(rhythmVm::selectTrack)
        }
    }
    val goSpace: (Space) -> Unit = { sp ->
        when (sp) {
            Space.Structure -> go(AppTab.Morceau)
            Space.Sons -> go(if (tab in SONS_TABS) tab else lastSons)
            Space.Mix -> go(AppTab.Mixeur)
            Space.Live -> openLive()
            // Touched again on Plus: back to its list.
            Space.Plus -> { if (tab == AppTab.Outils) toolPage = null; go(AppTab.Outils) }
        }
    }
    val navEntries: List<NavEntry> = if (simple) {
        SimpleTab.entries.map { st -> NavEntry("simple_${st.name}", st.label, st.hint, st.icon, selected = SimpleTab.of(tab) == st) { go(st.tab) } }
    } else {
        Space.entries.map { sp -> NavEntry(sp.name, sp.label, sp.hint, sp.icon, selected = tab.space == sp && tab != AppTab.Envoyer) { goSpace(sp) } }
    }
    androidx.activity.compose.BackHandler(enabled = showLive) { closeLive() }
    // The live pads on the whole screen (from Morceau or the Live page), above everything. Leaving
    // them from Morceau, nothing set in their effects stays on unseen, and a take started there
    // ends with them (written, as leaving the Live page does); above the Live page, its effects
    // and its take stay as that page shows them.
    var showLivePads by rememberSaveable { mutableStateOf(false) }
    // The controller follows the screen shown (the Live page's panel too): knobs, pads, faders, encoder.
    val controllerScreen = com.pocketstudio.app.midi.KnobScreen.of(tab.name, showLive, liveTab, showLivePads)
    com.pocketstudio.app.midi.KnobScreenFollower(knobsVm, controllerScreen)
    LaunchedEffect(controllerScreen) { midiVm.padsToDrums = !controllerScreen.isLive }
    // Revue neuve (MOYEN 5): its own restartable scope, so a MIDI message recomposes the overlays, not the app.
    ControllerOverlays(midiVm, knobsVm, rhythmVm, controllerScreen, onMidiPage = tab == AppTab.Outils && toolPage == ToolPage.Midi)
    val closeLivePads = {
        if (!showLive) rhythmVm.leaveLiveEndingTake()
        showLivePads = false
    }
    // The « Ligne du temps » (opened from Morceau) takes the whole screen too.
    var showTimeline by rememberSaveable { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = showTimeline && !showLive) { showTimeline = false }
    // Raised by the timeline's « Ouvrir dans Notes »: the Synthé tab shows its piano roll.
    var notesRequest by rememberSaveable { mutableIntStateOf(0) }
    // « Versions du morceau » from the Morceau tab (the Live page has its own).
    var songVersionsOpen by rememberSaveable { mutableStateOf(false) }
    if (songVersionsOpen) {
        val versionsProject by rhythmVm.project.collectAsStateWithLifecycle()
        com.pocketstudio.app.record.SongVersionsSheet(
            versionsProject,
            com.pocketstudio.app.record.SongVersionsActions(
                restore = { songVersionsOpen = false; rhythmVm.restoreSongVersion(it) }, drop = rhythmVm::dropSongVersion, close = { songVersionsOpen = false },
            ),
        )
    }
    // Phase 36: a track's name (Structure, Mon morceau) opens « Changer le son »: the kit for the
    // drums, the acid's sounds, the instrument's sheet; an audio track opens in Sons.
    var kitSheetOpen by rememberSaveable { mutableStateOf(false) }
    var acidSheetOpen by rememberSaveable { mutableStateOf(false) }
    // The bug report's journal: each screen, space or sheet shown is a line (and the transport, takes, exports).
    com.pocketstudio.app.report.JournalWatch(rhythmVm, bugVm) {
        buildList {
            add((if (simple) "Mode simple" else tab.space.name) + " › " + tab.name + (if (tab == AppTab.Outils) toolPage?.let { " › " + it.name } ?: "" else ""))
            if (tab.space == Space.Structure && !simple) add(if (partsView) "vue Parties" else "vue Ligne du temps")
            if (showProjects) add("Mes projets")
            if (showTimeline) add("Ligne du temps plein écran")
            if (showLivePads) add("Pads live plein écran")
            if (tempoSheetOpen) add("fiche Tempo")
            if (showShare) add("Prêt à partager ?")
            if (showVideo) add("fiche Vidéo")
            soundChooserTrack?.let { add("Changer le son (piste $it)") }
            trackSheetKey?.let { add("Fiche de la piste ($it)") }
            layersTrack?.let { add("Couches (piste $it)") }
            if (songChordsOpen) add("Accords du morceau")
            if (showLibrary) add("Bibliothèque de sons")
            if (fxChannel >= 0) add("Effets (tranche $fxChannel)")
            if (songVersionsOpen) add("Versions du morceau")
            if (kitSheetOpen) add("fiche Kits")
            if (acidSheetOpen) add("fiche Sons de l'acide")
            if (helpLabels) add("étiquettes « ? »")
        }.joinToString(" · ")
    }
    val trackSound: (String) -> Unit = { key ->
        when {
            key == com.pocketstudio.model.SongPart.DRUMS -> { rhythmVm.refreshPacks(); kitSheetOpen = true }
            key == com.pocketstudio.model.SongPart.ACID -> acidSheetOpen = true
            key.startsWith("inst:") -> key.removePrefix("inst:").toIntOrNull()?.let { rhythmVm.selectTrack(it); soundChooserTrack = it }
            key.startsWith("audio:") -> { go(AppTab.Audio); audioPage = 0 }
        }
    }
    if (kitSheetOpen) {
        val kitProject by rhythmVm.project.collectAsStateWithLifecycle()
        val kitPacks by rhythmVm.packsOnPhone.collectAsStateWithLifecycle()
        val kitLib by rhythmVm.library.collectAsStateWithLifecycle()
        com.pocketstudio.app.rhythm.KitSheet(
            com.pocketstudio.app.rhythm.kitName(kitProject), rhythmVm.kitChoices(kitPacks, kitLib), rhythmVm::chooseKit, close = { kitSheetOpen = false },
        )
    }
    if (acidSheetOpen) AcidSoundSheet(rhythmVm, close = { acidSheetOpen = false })
    val simpleTracksActions = SimpleTracksActions(
        changeSound = trackSound, setVolume = rhythmVm::setChannelVolume, toggleMute = rhythmVm::toggleMute, toggleSolo = rhythmVm::toggleSolo,
        listenAlone = { key -> rhythmVm.toggleAlone(com.pocketstudio.app.rhythm.AloneTarget.ofSongKey(key), from = SimpleTracksActions.FROM) },
        listenPadAlone = { lane -> rhythmVm.toggleAlone(com.pocketstudio.app.rhythm.AloneTarget.ofKey(com.pocketstudio.model.guide.GuideTrack.pad(lane)), from = SimpleTracksActions.FROM) },
        holdAlone = rhythmVm::holdAlone,
    )
    val learn = com.pocketstudio.app.learn.LocalLearn.current
    // G5: « Mon premier morceau » (the old Missions): its first cards, in the Guide. Audit final: on a new song,
    // from the assistant (the open one stays as it is); the song made opens with the Guide on its first cards.
    val startMissions: () -> Unit = {
        helpLabels = false
        toolPage = null
        firstSongNext = true
        rhythmVm.stop()
        libraryVm.openWizard()
    }
    com.pocketstudio.app.onboarding.LaunchChooser(launch, rhythmVm, projectsVm, show = launch.open && settings.guideSeen && !com.pocketstudio.app.onboarding.Intro.playing, simple = simple,
        hasProject = !noProject,
        actions = com.pocketstudio.app.onboarding.LaunchActions(
            // No project to continue: Retour leaves the app (« Mes projets » would only lead back here).
            resume = { if (noProject) leaveApp() else launch.choose() },
            newSong = { launch.choose(comeBack = true); projectsNewOnly = true; showProjects = true },
            projects = { launch.choose(comeBack = true); projectsNewOnly = false; showProjects = true },
            remix = {
                launch.choose()
                // Audit final (02 M9): with no project, « Mes projets » stays over everything; the Remix
                // gets a project of its own first (as « Mon premier morceau » does), then opens on it.
                if (noProject) {
                    projectsVm.create(Project(name = "Remix")) { id ->
                        openRemix()  // kept across the rebuild the new project brings
                        openProject(id)
                    }
                } else {
                    openRemix()
                }
            },
            missions = {
                launch.choose()
                when {
                    // No project (lot 1): « Mon premier morceau » starts on a new one (the blank stand-in is never saved).
                    noProject -> projectsVm.create(com.pocketstudio.model.guide.GuideStart.begin(Project(name = ""), com.pocketstudio.model.guide.GuideStart.FIRST)) { id ->
                        guideOpen = true
                        openProject(id)
                    }
                    else -> startMissions()
                }
            },
            chooseMode = { v -> SettingsStore.update(context) { it.copy(simpleMode = v) } },
        ),
    )
    // G3: a track's sheet; its layers and its sound open over it (the sheet waits underneath).
    trackSheetKey?.let { key ->
        com.pocketstudio.app.guide.TrackSheetRoute(
            rhythmVm, key, close = { trackSheetKey = null; rhythmVm.dismissNotice() },
            changeSound = { id -> rhythmVm.selectTrack(id); soundChooserTrack = id },
            layers = { id -> rhythmVm.selectTrack(id); layersTrack = id },
            effects = { ch -> fxChannel = ch },
            reuse = { trackSheetKey = null; partsView = true; navPrefs.partsView = true; go(AppTab.Morceau) },
        )
    }
    layersTrack?.let { track -> com.pocketstudio.app.guide.LayersEditorRoute(rhythmVm, track, close = { layersTrack = null }) }
    soundChooserTrack?.let { track ->
        SoundChooserSheet(rhythmVm, track, close = { soundChooserTrack = null }, real = realInstrumentsUi(rhythmVm, libraryVm, track) { soundChooserTrack = null })
    }
    // Phase 37: « Accords du morceau » (Morceau, Gamme et accords) and a knob's « Automatiser ».
    if (songChordsOpen) com.pocketstudio.app.session.SongChordsRoute(rhythmVm, close = { songChordsOpen = false })
    val automateTarget by rhythmVm.automateTarget.collectAsStateWithLifecycle()
    automateTarget?.let { target -> com.pocketstudio.app.mixer.AutomateLaneRoute(rhythmVm, target, close = rhythmVm::closeAutomateLane) }
    // An instrument downloaded (from the chooser or the library): the tracks waiting for it get it.
    val installedPacks by libraryVm.packs.collectAsStateWithLifecycle()
    LaunchedEffect(installedPacks.installed) { rhythmVm.instrumentsChanged() }
    if (showShare) {
        com.pocketstudio.app.export.ShareSheetRoute(
            rhythmVm, sharePrefs, close = { showShare = false },
            toStructure = { go(AppTab.Morceau) },
            // Audit final (01 M6, 05, 08): the Mode simple has no Mix: no « Voir dans Mix »; « Autres
            // exports » says it goes to the full mode, and does.
            toMixer = if (simple) null else ({ tab = AppTab.Mixeur; mixerFocus = com.pocketstudio.app.mixer.Where.Master }),
            otherExports = {
                if (simple) SettingsStore.update(context) { it.copy(simpleMode = false) }
                go(AppTab.Mixeur)
                mixerFocus = com.pocketstudio.app.mixer.Where.Export
            },
            video = { showVideo = true },
            simple = simple,
        )
    }
    // « Vidéo pour les réseaux » (from « Prêt à partager ? »): 30 s of the song in 9:16.
    if (showVideo) com.pocketstudio.app.export.video.VideoExportRoute(rhythmVm, close = { showVideo = false })
    // The timeline (in Structure or on the whole screen): two touches on a clip open its editor on
    // that part (the track's scene there, in Sons); a curve opens in Mix.
    val openFromTimeline: (Int, String) -> Unit = { i, key ->
        val part = rhythmVm.project.value.song.getOrNull(i)
        if (part != null) {
            // The track's scene in that part, in its own editor.
            rhythmVm.showSceneToEdit(part.sceneFor(key))
            showTimeline = false
            when {
                key == com.pocketstudio.model.SongPart.DRUMS -> go(AppTab.Rythme)
                key == com.pocketstudio.model.SongPart.ACID -> go(AppTab.Acide)
                key.startsWith("inst:") -> {
                    key.removePrefix("inst:").toIntOrNull()?.let(rhythmVm::selectTrack)
                    notesRequest++
                    go(AppTab.Synthe)
                }
                key.startsWith("audio:") -> { go(AppTab.Audio); audioPage = 0 }
            }
        }
    }
    // The instrument tracks for the Sons space's chips ([bar]) or for the synth screen's own use.
    // « Tuto de A à Z »: the first track hidden while it is still blank (set below, once the tutorial is known).
    var tutoHidesFirst by remember { mutableStateOf(false) }
    fun tracksUiOf(fp: Project, selected: Int, freezing: Int?, bar: Boolean): TracksUi {
        val frozen = com.pocketstudio.model.Freeze.frozenIds(fp)
        return TracksUi(
            tracks = fp.instrumentTracks.filter { !(tutoHidesFirst && it.id == 0) },
            selected = selected,
            select = { id -> rhythmVm.selectTrack(id); go(AppTab.Synthe) },
            add = rhythmVm::addTrack,
            addKind = rhythmVm::addTrackOf,
            rename = rhythmVm::renameTrack,
            setColor = rhythmVm::setTrackColor,
            duplicate = rhythmVm::duplicateTrack,
            remove = rhythmVm::removeTrack,
            // Audit final (08, 01 M10): the Mode simple's tracks change their sound, name, colour;
            // effects, the track's sheet and freezing are the full mode's.
            effects = if (simple) null else ({ id -> fxChannel = com.pocketstudio.model.InstrumentTrack.mixerChannel(id) }),
            clear = rhythmVm::clearTrackNotes,
            scene = fp.sceneTitle(fp.scene),
            changeSound = { id -> rhythmVm.selectTrack(id); soundChooserTrack = id },
            sheet = if (simple) null else ({ id -> trackSheetKey = com.pocketstudio.app.guide.sheetKeyOf(id) }),
            freeze = if (simple) null else rhythmVm::freezeTrack,
            unfreeze = rhythmVm::unfreezeTrack,
            frozen = frozen,
            freezing = freezing,
            stale = frozen.filter { com.pocketstudio.model.Freeze.isStale(fp, it) }.toSet(),
            showBar = bar,
            simple = simple,
            addVoice = if (simple) null else ({ rhythmVm.addAudioTrack(); audioPage = 0; go(AppTab.Audio) }),
        )
    }
    val openLaneFromTimeline: (Int) -> Unit = { lane ->
        showTimeline = false
        go(AppTab.Mixeur)
        mixerFocus = com.pocketstudio.app.mixer.Where.Lane(lane)
    }
    // Le Guide (docs/PLAN-GUIDE.md G2): its bar above the spaces, its panel opened from it (never
    // over what is edited: it takes its own room), « Voir le réglage » lighting the control.
    val guideVm = rhythmVm.guide
    LaunchedEffect(guideVm, simple) { guideVm.setSimple(simple) }
    var guideSelected by rememberSaveable { mutableStateOf<String?>(null) }
    var guideNotice by remember { mutableStateOf<String?>(null) }
    // The controls « Voir le réglage » lights, the first one on the screen (null: none).
    var guideSpot by remember { mutableStateOf<List<String>?>(null) }
    // Audit final: a pad's sheet the Guide asks for (the Rythme tab opens it, then forgets the request).
    var guidePad by remember { mutableStateOf<Int?>(null) }
    // The tab the Guide's « Autre idée de son » just went to (« Écouter seul » goes on there).
    var aloneTabByGuide by remember { mutableStateOf<AppTab?>(null) }
    val guideScope = rememberCoroutineScope()
    val guideListen = remember(rhythmVm) {
        com.pocketstudio.app.guide.GuideListen(
            guideScope, project = { rhythmVm.project.value }, render = rhythmVm::renderGuideAB,
            player = com.pocketstudio.app.guide.EnginePcmPlayer(guideScope),
            pauseMusic = { if (rhythmVm.transport.value.playing) rhythmVm.togglePlay() },
            check = rhythmVm::renderGuideCheck,
        )
    }
    // G4: « À côté d'un morceau que tu aimes »: an audio file picked, then heard at the song's loudness.
    val guideReference by rhythmVm.guideReference.collectAsStateWithLifecycle()
    var guideReferenceFor by remember { mutableStateOf<Pair<String, String>?>(null) }
    val pickGuideReference = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val then = guideReferenceFor
        guideReferenceFor = null
        if (uri != null) guideScope.launch {
            if (!rhythmVm.loadGuideReference(uri)) guideNotice = tr("Ce fichier ne se lit pas : choisis un autre morceau (MP3, M4A, WAV…).")
            else if (then != null) guideListen.toggle(then.first, then.second)
        }
    }
    val guideListenOrPick: (String, String) -> Unit = { card, option ->
        val p = rhythmVm.project.value
        val action = com.pocketstudio.model.guide.GuideActions.cardOf(p, card)?.let { com.pocketstudio.model.guide.GuideActions.optionOf(p, it, option) }?.action
        if (action?.type == com.pocketstudio.model.guide.GuideAction.LISTEN && action.name == "reference" && rhythmVm.guideReference.value == null) {
            guideListen.stop()
            guideReferenceFor = card to option
            runCatching { pickGuideReference.launch(arrayOf("audio/*")) }
        } else {
            learn.stop()  // G5 review: one sound at a time (the word's example, or « Sans / Avec »)
            guideListen.toggle(card, option)
        }
    }
    DisposableEffect(guideListen) { onDispose { guideListen.stop() } }
    // « Tuto de A à Z » in this project (its progress kept per project); its examples rendered from the course, never the project.
    val tuto = remember(rhythmVm) {
        com.pocketstudio.app.tuto.TutoController(
            rhythmVm.viewModelScope, rhythmVm.projectId, rhythmVm.project, rhythmVm::tutoEdit,
            com.pocketstudio.app.tuto.PrefsTutoStore(context),
            render = { ex ->
                rhythmVm.renderExample(ex.after)?.let { after -> com.pocketstudio.app.tuto.TutoController.joined(ex.before?.let { rhythmVm.renderExample(it) }, after) }
            },
            player = com.pocketstudio.app.tuto.TutoTestHooks.player ?: com.pocketstudio.app.guide.EnginePcmPlayer(rhythmVm.viewModelScope),
            pauseMusic = { if (rhythmVm.transport.value.playing) rhythmVm.togglePlay() },
            demoShow = rhythmVm::tutoDemo, demoSound = rhythmVm::tutoDemoSound,
        )
    }
    DisposableEffect(tuto) { onDispose { tuto.stopListening(); tuto.stopDemo(); rhythmVm.tutoDemo(null) } }
    // « Recommencer le tuto » (owner, 2A): a new empty project, the same style and level; this one is kept.
    val restartTuto: () -> Unit = { tuto.saved?.let { startTuto(it.style, it.level) } }
    // The key step's own sheet, and the control « Je le fais » rings a moment.
    var tutoKeySheet by rememberSaveable { mutableStateOf(false) }
    var tutoSpot by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(tutoSpot) { if (tutoSpot != null) { kotlinx.coroutines.delay(TUTO_SPOT_MS); tutoSpot = null } }
    // Testers (lot « tuto 3 »): the step's control brought into view, ringed or not (its screen may take a few frames).
    var tutoReveal by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(tutoReveal) {
        val ids = tutoReveal ?: return@LaunchedEffect
        for (i in 0 until 10) {
            kotlinx.coroutines.delay(100)
            if (ids.any { spots.reveal(it) }) break
        }
        tutoReveal = null
    }
    if (tutoKeySheet) {
        val keyProject by rhythmVm.project.collectAsStateWithLifecycle()
        val wanted = tuto.ui.value?.progress?.style?.let { com.pocketstudio.model.tuto.TutoTarget.of(it)?.harmony }
        if (wanted == null) tutoKeySheet = false
        else com.pocketstudio.app.tuto.TutoKeySheet(keyProject.harmony, wanted, rhythmVm::setHarmony, close = { tutoKeySheet = false })
    }
    val tutoUi by tuto.ui.collectAsStateWithLifecycle()
    // « Mode retours »: the screen shown and the tutorial's step, in its event log (French, for whoever reads it).
    LaunchedEffect(tab, toolPage) { com.pocketstudio.app.retours.Retours.screen(tab.name + (toolPage?.takeIf { tab == AppTab.Outils }?.let { " › ${it.name}" } ?: "")) }
    LaunchedEffect(tutoUi?.chapter, tutoUi?.step, tutoUi == null) { com.pocketstudio.app.retours.Retours.tutoAt(tutoUi?.let(::tutoWhere) ?: "") }
    var tutoDoneSeen by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(tutoUi?.progress?.done) {
        val done = tutoUi?.progress?.done ?: return@LaunchedEffect
        tutoDoneSeen?.let { seen -> (done - seen).forEach { com.pocketstudio.app.retours.Retours.event("tuto fait", it) } }
        tutoDoneSeen = done
    }
    var tutoChapters by rememberSaveable { mutableStateOf(false) }
    // The dotted cells of a drum step (Rythme), the dotted notes of a melodic one (its notes), shown the whole step.
    val tutoProject = rhythmVm.project.collectAsStateWithLifecycle().value
    val tutoSteps = tutoProject.drums.steps
    LaunchedEffect(tutoUi?.current?.id, tutoSteps, tutoUi == null, tutoUi?.need, tutoUi?.progress?.done) { com.pocketstudio.app.tuto.TutoHints.cells = tuto.hint(tutoSteps) }
    LaunchedEffect(tutoUi?.current?.id, tutoProject, tutoUi == null, tutoUi?.need) {
        com.pocketstudio.app.tuto.TutoHints.notes = tuto.ghostNotes(tutoProject)
        com.pocketstudio.app.tuto.TutoHints.erase = tuto.toErase(tutoProject)
        com.pocketstudio.app.tuto.TutoHints.sendNote = tuto.sendNote(tutoProject)
    }
    // Testers (lot « tuto 4 »): the tutorial's panel has the bottom of the screen (no floating keyboard over its
    // rings), a step ticked closes a hit's menu (its first touch on « Étape suivante » was lost closing it).
    LaunchedEffect(tutoUi == null) { com.pocketstudio.app.tuto.TutoHints.active = tutoUi != null }
    LaunchedEffect(tutoUi?.progress?.done?.size) { com.pocketstudio.app.tuto.TutoHints.ticked++ }
    LaunchedEffect(tutoUi?.current?.id) { com.pocketstudio.app.tuto.TutoHints.knobs = tuto.place()?.knobs.orEmpty().toSet() }
    // The hits to take away left on another pad only (« Caisse » done, « Charley » next): that pad shown, said.
    val tutoErase = com.pocketstudio.app.tuto.TutoHints.erase
    val tutoPad by remember(rhythmVm) { rhythmVm.transport.map { it.selectedPad }.distinctUntilChanged() }.collectAsStateWithLifecycle(rhythmVm.transport.value.selectedPad)
    LaunchedEffect(tutoErase, tutoPad) {
        val left = tutoErase?.filterValues { it.isNotEmpty() }?.keys ?: return@LaunchedEffect
        if (left.isEmpty() || tutoPad in left) return@LaunchedEffect
        kotlinx.coroutines.delay(com.pocketstudio.app.tuto.TUTO_TURN_MS)
        val next = left.min()
        rhythmVm.selectPad(next)
        com.pocketstudio.app.tuto.TutoHints.banner = "→ " + com.pocketstudio.app.rhythm.padName(rhythmVm.project.value, next)
    }
    LaunchedEffect(tutoUi?.demoCells, tutoUi?.fade) {
        com.pocketstudio.app.tuto.TutoHints.fading = tutoUi?.demoCells?.let { (lane, cells) -> Triple(lane, cells, tutoUi?.fade ?: 0f) }
    }
    // A drum step shows its pad on the grid (its dotted cells in sight), without leaving the screen shown.
    LaunchedEffect(tutoUi?.current?.id) { tutoUi?.pad?.let(rhythmVm::selectPad) }
    // Testers (lot « tuto 3 »): a new step's control brought into view when it is on the screen shown (the
    // grid was all under the panel), without a ring nor another screen.
    LaunchedEffect(tutoUi?.current?.id) {
        val open = tuto.place()?.open ?: return@LaunchedEffect
        val g = com.pocketstudio.app.guide.GuideNav.go(open, rhythmVm.project.value, SettingsStore.state.value.simpleMode)
        if (g is com.pocketstudio.app.guide.GuideGo.Tab && g.tab == tab && g.spots.isNotEmpty()) tutoReveal = g.spots
    }
    LaunchedEffect(tutoUi?.demo != null) { com.pocketstudio.app.tuto.TutoHints.demo = tutoUi?.demo != null }
    // The send a step asks for, dotted in the effects box (« Je le fais » opens the right one).
    LaunchedEffect(tutoUi?.current?.id, tutoUi == null) {
        com.pocketstudio.app.tuto.TutoHints.send = tuto.place()?.let { g -> g.effects?.let { ch -> g.send?.let { ch to it } } }
    }
    // « Écoute toute ta batterie »: the user's own music heard a little while (whatever started it) ticks it.
    val tutoMusicPlaying by remember(rhythmVm) { rhythmVm.transport.map { it.playing }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(rhythmVm.transport.value.playing)
    LaunchedEffect(tutoMusicPlaying, tutoUi?.current?.id) {
        if (tutoMusicPlaying && tutoUi?.current?.listening == true && tuto.mineToHear()) {
            kotlinx.coroutines.delay(TUTO_MINE_HEARD_MS)
            if (rhythmVm.transport.value.playing) tuto.heardMine()
        }
    }
    // The tutorial hides the first track while it is still blank: the project starts truly empty.
    LaunchedEffect(tutoUi == null, tutoProject) { tutoHidesFirst = tutoUi != null && com.pocketstudio.model.tuto.TutoChecks.blankFirst(tutoProject) }
    if (tutoChapters) tutoUi?.let { u ->
        ScaledDialog(onDismissRequest = { tutoChapters = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            androidx.compose.material3.Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                com.pocketstudio.app.tuto.TutoChapterList(
                    u, goTo = { tuto.goTo(it); tutoChapters = false },
                    setLevel = { level ->
                        SettingsStore.update(context) { it.copy(simpleMode = level == com.pocketstudio.model.tuto.TutoLevel.Facile) }
                        tuto.setLevel(level)
                    },
                    quit = { tuto.quit(); tutoChapters = false }, close = { tutoChapters = false },
                    restart = { tutoChapters = false; restartTuto() },
                )
            }
        }
    }
    // G4 review: a measure running stops with the panel (and when the music starts, below).
    LaunchedEffect(guideOpen) { if (!guideOpen) { guideListen.stop(); learn.stop(); guideVm.cancelMeasure() } }
    // G2 review: the « Sans / Avec » never plays under a take, the Live page or the song started (the
    // panel only hides there, it stays open).
    val guideQuietRec by rhythmVm.recording.collectAsStateWithLifecycle()
    // Revue neuve (MOYEN 7): only whether a take runs and the song plays (the take's level comes every
    // 50 ms, the transport often): the whole app is not recomposed for them.
    val guideQuietTaking by remember(rhythmVm) { rhythmVm.audioRecording.map { it != null }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(rhythmVm.audioRecording.value != null)
    val guideQuietPlaying by remember(rhythmVm) { rhythmVm.transport.map { it.playing }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(rhythmVm.transport.value.playing)
    // Le Guide keeps quiet during a take, on the Live page and on the keyboard lying down, where every dp is a key's
    // (§3.1); lying down, its panel opens on the side ([guideSide]).
    val guideShown = !showLive && !liveImmersive && !showTimeline && !showLivePads && !guideQuietRec && !guideQuietTaking && !(landscape && tab == AppTab.Synthe)
    val guideSide = guideShown && guideOpen && landscape
    LaunchedEffect(guideQuietRec, guideQuietTaking, showLive, guideQuietPlaying) {
        if (guideQuietRec || guideQuietTaking || showLive || guideQuietPlaying) {
            guideListen.stop()
            learn.stop()
            guideVm.cancelMeasure()
        }
        // G5 review: the song just made has been heard (the top ▶ too): « Ton morceau est prêt » is decided,
        // the Guide no longer opens on it by itself.
        if (guideQuietPlaying && com.pocketstudio.model.guide.GuideStart.welcomeCard(rhythmVm.project.value) == com.pocketstudio.model.guide.GuideStart.READY) {
            guideVm.markDone(com.pocketstudio.model.guide.GuideStart.READY, "drop")
        }
    }
    val guideGo: (com.pocketstudio.app.guide.GuideGo) -> Unit = { g ->
        when (g) {
            is com.pocketstudio.app.guide.GuideGo.Tab -> {
                g.audioPage?.let { audioPage = it }
                if (g.partsView) { partsView = true; navPrefs.partsView = true }
                go(g.tab)
                g.mixer?.let { mixerFocus = it }
                // The screen whole, its control lit: the panel folds (its card stays chosen, one touch away).
                guideOpen = false
                if (g.spots.isNotEmpty()) guideSpot = g.spots
            }
            com.pocketstudio.app.guide.GuideGo.Tempo -> tempoSheetOpen = true
            com.pocketstudio.app.guide.GuideGo.Chords -> songChordsOpen = true
            // Audit final: the track the Guide picks stays picked (the Clavier's first visit no longer swaps it for the melody's).
            is com.pocketstudio.app.guide.GuideGo.Sound -> { melodyShownFor = rhythmVm.projectId; rhythmVm.selectTrack(g.track); soundChooserTrack = g.track }
            is com.pocketstudio.app.guide.GuideGo.Layers -> { melodyShownFor = rhythmVm.projectId; rhythmVm.selectTrack(g.track); layersTrack = g.track }
            com.pocketstudio.app.guide.GuideGo.Kit -> kitSheetOpen = true
            // Audit final: one pad's sound (the kick's « Autre idée de son »), on the Rythme tab.
            is com.pocketstudio.app.guide.GuideGo.Pad -> { if (tab != AppTab.Rythme) aloneTabByGuide = AppTab.Rythme; go(AppTab.Rythme); guidePad = g.lane }
            com.pocketstudio.app.guide.GuideGo.AcidSound -> acidSheetOpen = true
            // Audit final: « Générer une basse acide » makes the line (a song without one), then offers its sounds.
            com.pocketstudio.app.guide.GuideGo.AcidGenerate -> {
                if (!com.pocketstudio.model.guide.GuideStart.bassline(rhythmVm.project.value)) rhythmVm.generateAcid()
                acidSheetOpen = true
            }
            is com.pocketstudio.app.guide.GuideGo.Notes -> { melodyShownFor = rhythmVm.projectId; rhythmVm.selectTrack(g.track); notesRequest++; go(AppTab.Synthe); guideOpen = false }
            com.pocketstudio.app.guide.GuideGo.WhatPlays -> { overviewCh = -1; overviewScene = -1 }
            com.pocketstudio.app.guide.GuideGo.Share -> showShare = true
            com.pocketstudio.app.guide.GuideGo.Video -> showVideo = true
            // G5: « Ton morceau est prêt »'s « Écouter » (about 8 bars before the first Drop) and « Depuis le début ».
            is com.pocketstudio.app.guide.GuideGo.Play -> when {
                rhythmVm.project.value.song.isEmpty() -> if (!rhythmVm.transport.value.playing) rhythmVm.togglePlay()
                g.fromDrop -> rhythmVm.listenFromDrop()
                else -> rhythmVm.listenFromStart()
            }
            com.pocketstudio.app.guide.GuideGo.SongGuide -> songGuideOpen = true
            com.pocketstudio.app.guide.GuideGo.NewStyle -> {
                // « Mon premier morceau »: the song the assistant makes now goes on with its first cards.
                firstSongNext = rhythmVm.project.value.guide.start == com.pocketstudio.model.guide.GuideStart.FIRST
                rhythmVm.stop()
                libraryVm.openWizard()
            }
            com.pocketstudio.app.guide.GuideGo.Remix -> { guideOpen = false; openRemix() }
            is com.pocketstudio.app.guide.GuideGo.Record -> {
                melodyShownFor = rhythmVm.projectId
                rhythmVm.selectTrack(g.track)
                go(AppTab.Synthe)
                guideOpen = false
                guideSpot = listOf("record_start")
            }
        }
    }
    LaunchedEffect(guideVm) {
        guideVm.opens.collect { o -> guideGo(com.pocketstudio.app.guide.GuideNav.go(o, rhythmVm.project.value, SettingsStore.state.value.simpleMode)) }
    }
    LaunchedEffect(guideVm) { guideVm.notices.collect { guideNotice = it } }
    LaunchedEffect(tuto) {
        tuto.goes.collect { g ->
            g.pad?.let(rhythmVm::selectPad)
            val simpleNow = SettingsStore.state.value.simpleMode
            when {
                g.keySheet -> tutoKeySheet = true
                g.notes != null -> {
                    melodyShownFor = rhythmVm.projectId
                    rhythmVm.selectTrack(g.notes)
                    notesRequest++
                    go(AppTab.Synthe)
                    // The next dotted note brought into view (and lit), wherever the roll was.
                    com.pocketstudio.app.tuto.TutoHints.focus++
                }
                // An effect or a send: that track's effects box, the send it asks for dotted in it.
                g.effects != null -> {
                    com.pocketstudio.app.tuto.TutoHints.send = g.send?.let { g.effects to it }
                    fxChannel = g.effects
                }
                // The sound's settings (« Coupure »…) of that track, not its ready-made sounds; the knobs the step
                // names ringed (testers, lot « tuto 4 »: « Glissé » was five scrolls down, behind « Réglages avancés »).
                g.settings != null -> {
                    melodyShownFor = rhythmVm.projectId
                    rhythmVm.selectTrack(g.settings)
                    com.pocketstudio.app.tuto.TutoHints.knobs = g.knobs.toSet()
                    go(AppTab.Synthe)
                    val ids = g.knobs.map { "synth_knob_$it" } + SYNTH_SETTINGS_SPOT
                    if (g.spot) tutoSpot = ids else tutoReveal = ids
                }
                // The fader of the track the step names: the Mode simple's « Mix », else the console's strip with its
                // name (testers, lot « tuto 4 »: the first strip, the drums', was ringed for the « Accords »).
                g.strip != null -> {
                    go(AppTab.Mixeur)
                    val ids = listOf(if (simpleNow) "mix_strip_${g.strip}" else "console_strip_${g.strip}")
                    if (g.spot) tutoSpot = ids else tutoReveal = ids
                }
                g.open != null -> {
                    guideGo(com.pocketstudio.app.guide.GuideNav.go(g.open, rhythmVm.project.value, simpleNow))
                    // No card to close (owner: « Le Guide t'attend… » + « Compris » each time): a ring that goes by itself.
                    val lit = guideSpot
                    guideSpot = null
                    if (g.spot) tutoSpot = lit else tutoReveal = lit
                }
            }
        }
    }
    // A lit control goes dark by itself after a while, or as soon as the space changes.
    LaunchedEffect(guideSpot) { if (guideSpot != null) { kotlinx.coroutines.delay(GUIDE_SPOT_MS); guideSpot = null } }
    // Audit final: Retour closes what is over the screen (the « ? » labels, the lit control, the Guide's panel), it no
    // longer leaves the app (the Live page and the timeline keep their own, registered before: these come first).
    // A sheet or a dialog over the screen takes Retour first (its own window on the phone): never the Guide behind it.
    val sheetOver = tempoSheetOpen || songChordsOpen || songGuideOpen || showShare || showVideo || kitSheetOpen || acidSheetOpen ||
        soundChooserTrack != null || layersTrack != null || trackSheetKey != null
    androidx.activity.compose.BackHandler(enabled = guideOpen && guideShown && !sheetOver) { guideOpen = false }
    androidx.activity.compose.BackHandler(enabled = guideSpot != null && !sheetOver) { guideSpot = null }
    androidx.activity.compose.BackHandler(enabled = helpLabels && !sheetOver) { helpLabels = false }
    val guideActions = com.pocketstudio.app.guide.GuidePanelActions(
        toggle = {
            guideOpen = !guideOpen
            // G5 review: folded on its welcome card, the card waits « plus tard » (it does not open by itself again).
            if (!guideOpen) com.pocketstudio.model.guide.GuideStart.welcomeCard(rhythmVm.project.value)?.let(guideVm::markLater)
            com.pocketstudio.app.report.Journal.action(if (guideOpen) "Guide ouvert" else "Guide replié")
        },
        stopListen = { guideListen.stop() },
        apply = { card, option -> guideListen.stop(); guideNotice = null; guideVm.apply(card, option); if (guideSelected == card) guideSelected = null },
        open = { card, option -> guideListen.stop(); guideVm.apply(card, option) },
        listen = guideListenOrPick,
        later = { card -> guideListen.stop(); guideVm.later(card); guideSelected = null },
        skip = { card -> guideListen.stop(); guideVm.skip(card); guideSelected = null },
        refuse = { card -> guideListen.stop(); guideVm.skip(card, forever = true); guideSelected = null },
        undo = { card -> guideListen.stop(); guideVm.undoChoice(card) },
        select = { card -> guideListen.stop(); guideSelected = card },
        dismissNotice = { guideNotice = null },
        sheet = { key -> guideListen.stop(); trackSheetKey = key },
        // Measured only with the music stopped and nothing recording (the renders load the phone).
        measure = {
            if (!rhythmVm.transport.value.playing && !rhythmVm.recording.value && rhythmVm.audioRecording.value == null) guideVm.measure()
        },
        pickReference = { guideListen.stop(); guideReferenceFor = null; runCatching { pickGuideReference.launch(arrayOf("audio/*")) } },
        // « ▶ Écouter seul » (bug report, APK 224): the card's sound alone, looped by the engine (one sound at a time).
        listenAlone = { card ->
            guideListen.stop()
            learn.stop()
            val role = guideVm.ui.value.cards[card]?.target
            if (role != null) rhythmVm.toggleAlone(com.pocketstudio.app.rhythm.AloneTarget.ofRole(rhythmVm.project.value, role), from = "card:$card")
        },
        holdAlone = rhythmVm::holdAlone,
    )
    val aloneNow by rhythmVm.alone.collectAsStateWithLifecycle()
    val aloneCard = aloneNow?.from?.takeIf { it.startsWith("card:") }?.removePrefix("card:")
    // The sheets where a sound is changed keep « Écouter seul » going (« Autre idée de son » opens them).
    com.pocketstudio.app.rhythm.HoldAlone(kitSheetOpen || acidSheetOpen || soundChooserTrack != null || layersTrack != null, rhythmVm::holdAlone)
    // Another screen (a tab, Mes projets, the Live page, the timeline, the library): « Écouter seul » stops.
    // The Guide's own « Autre idée de son » goes to the Rythme tab on purpose: that change keeps it.
    LaunchedEffect(tab, showProjects, showLive, showTimeline, showLivePads, showLibrary) {
        if (aloneTabByGuide == tab) aloneTabByGuide = null else rhythmVm.stopAlone()
    }
    val studioBackground = Neon.Background
    androidx.compose.runtime.CompositionLocalProvider(com.pocketstudio.app.learn.LocalSpots provides spots) {
    // A new mode draws every screen again (their names and words change; the project does not).
    androidx.compose.runtime.key(simple) {
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        // « Mes projets » is a dialog above: behind it, a plain background, so the studio never flashes
        // through while the screens are rebuilt on another project.
        modifier = if (showProjects || switching) Modifier.drawWithContent { drawRect(studioBackground) } else Modifier,
        containerColor = Neon.Background,
        // One under the other (the slot alone would draw them on top of each other).
        topBar = { Column {
            if (!showLive && !showTimeline && !showLivePads) {
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
                    com.pocketstudio.model.DefaultNames.shown(topProject.name), topTransport.playing, topUndo && undoable, topRedo && undoable, topRecording || topTake != null,
                    micTake = micMode,
                    // The Audio tracks screen records on the scene shown: it can be changed from there.
                    // Audit final (20 M16): the Mode simple's bar is lighter: no part chip (the editors'
                    // header picks the part), no redo (↶ stays).
                    // Lying down the editors' header gives its line to the editor: the chip stays there.
                    scene = if ((!simple || landscape) && (tab in SCENE_TABS || (tab == AppTab.Audio && audioPage == 0))) sceneChip(topProject) else null,
                    redo = !simple,
                    saved = saveError == null && !topUnsaved,
                    recordInto = topRecordInto?.let { topProject.sceneTitle(it) },
                ),
                TopBarActions(
                    projects = { showProjects = true }, rename = { renamingProject = true },
                    undo = { com.pocketstudio.app.report.Journal.action("↶ Annuler"); rhythmVm.undo() },
                    redo = { com.pocketstudio.app.report.Journal.action("↷ Rétablir"); rhythmVm.redo() },
                    togglePlay = rhythmVm::togglePlay,
                    toggleRecord = {
                        when {
                            // From the synth the take is a played phrase (a one-bar loop grows to four first);
                            // from the 303, its knobs; elsewhere what is played (pads, keys, MIDI).
                            // Audit final (05 #11): the Mode simple's ● where nothing is played (Mon morceau,
                            // Envoyer, the Remix…) records « ma mélodie » on the Clavier, not an empty take of pads.
                            !micMode && simple && tab !in RECORD_BAR_TABS && !topRecording -> { go(AppTab.Synthe); rhythmVm.recordMelodyTake() }
                            !micMode -> rhythmVm.recordTake(takeSourceOf(tab))
                            topTake != null -> rhythmVm.stopAudioRecording()
                            !topSaving -> rhythmVm.micTakeTrack()?.let { id ->
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    rhythmVm.recordAudio(id)
                                } else {
                                    audioRecordTrack = id
                                    audioPunch = null
                                    audioMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    },
                    selectScene = rhythmVm::selectScene, newScene = rhythmVm::newScene,
                    share = shareSong,
                    // Mode simple: « Envoyer » is a tab; the rest of the app in « ⋯ ».
                    more = if (!simple) null else listOf(
                        TopMenuItem(tr("Live : jouer en direct"), "more_live", openLive),
                        // Audit final (03 G4): the voice has its own way in the Mode simple.
                        TopMenuItem(tr("Enregistrer ma voix"), "more_voice", openVoice),
                        TopMenuItem(tr("Remixer une chanson"), "more_remix", openRemix),
                        // Audit final (05): the long press on the name was the only way to rename the song.
                        TopMenuItem(tr("Renommer le morceau"), "more_rename") { renamingProject = true },
                        TopMenuItem(tr("Mes projets"), "more_projects") { showProjects = true },
                        TopMenuItem(tr("Réglages"), "more_settings") { toolPage = ToolPage.Reglages; go(AppTab.Outils) },
                        TopMenuItem(tr("Aide et premiers pas"), "more_help") { toolPage = ToolPage.Aide; go(AppTab.Outils) },
                        TopMenuItem(tr("Passer en mode complet"), "more_full") { SettingsStore.update(context) { it.copy(simpleMode = false) } },
                        TopMenuItem(tr("Signaler un bug"), "more_bug") { reportBug("menu ⋯") },
                    ),
                ),
            )
            // The rename bar (a long press on the name), the tempo, where the song is and « ⟳ Partie », the solo banner; the tempo sheet.
            UnderTopBar(
                rhythmVm, tempoOpen = tempoSheetOpen, setTempoOpen = { tempoSheetOpen = it },
                renaming = renamingProject, closeRename = { renamingProject = false },
                rename = { name -> openScope.launch { rhythmVm.renameProject(name); rhythmVm.flushNow(); projectsVm.refresh() } },
                helpOn = helpLabels, toggleHelp = { helpLabels = !helpLabels },
                reportBug = { reportBug("appui long sur « ? »") },
                loopChip = !simple,
            )
            // After a crash, a discreet line rather than a window: the report opens with the crash in it.
            val crashText = pendingCrash
            if (crashText != null) {
                com.pocketstudio.app.report.CrashBanner(
                    send = {
                        bugVm.request("plantage", crash = crashText, crashJournal = com.pocketstudio.app.report.CrashReporter.crashJournal())
                        mainVm.dismissCrash()
                    },
                    dismiss = mainVm::dismissCrash,
                    modifier = Modifier.windowInsetsPadding(SIDE_INSETS),
                )
            }
            if (saveError != null) {
                // Audit 14: a full phone said as such, any other failure with its own words.
                val error = saveError
                Text(
                    if (error == null || error.isStorageFull()) {
                        tr("Sauvegarde impossible (mémoire pleine ?). Tes dernières modifications ne sont pas encore sauvegardées. Pour faire de la place : Mes projets › Corbeille › Vider la corbeille.")
                    } else {
                        tr("Sauvegarde impossible (%s). Tes dernières modifications ne sont pas encore sauvegardées : l'appli réessaie à la prochaine modification.", error.forUser())
                    },
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
                    // Lot 2: plain words (« boucle », « ↶ » meant nothing to a beginner); the Mode simple says only what it does.
                    Text(
                        when {
                            simple -> tr("Tu as 4 mesures pour jouer ta mélodie, puis elle recommence.")
                            loopSong -> tr("Ta prise peut durer 4 mesures : cette partie s'allonge à 4 mesures dans le morceau aussi. ↶ Annuler la remet à 1 mesure.")
                            else -> tr("Ta prise peut durer 4 mesures, elle ne sera pas coupée après la première. ↶ Annuler la remet à 1 mesure.")
                        },
                        style = MaterialTheme.typography.bodySmall, maxLines = if (landscape) 2 else 4, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = rhythmVm::dismissTakeLoop) { Text(tr("Compris")) }
                }
            }
            // A take started from the top bar on a tab without its own record bar: its count-in, its
            // counter and the choice after it (Garder, Remettre en rythme, Refaire), under the bar.
            // A take that ended with nothing played, on a tab without its own record bar: said here.
            val stripEmpty by rhythmVm.takeEmpty.collectAsStateWithLifecycle()
            if (stripEmpty && tab !in RECORD_BAR_TABS) {
                Text(
                    tr("Rien n'a été joué pendant la prise."), style = MaterialTheme.typography.bodySmall, color = Neon.Amber,
                    modifier = Modifier.fillMaxWidth().background(Neon.Surface).windowInsetsPadding(SIDE_INSETS).padding(horizontal = 12.dp, vertical = 6.dp).testTag("take_empty"),
                )
            }
            val stripTake by rhythmVm.noteTake.collectAsStateWithLifecycle()
            if (stripTake != null && tab !in RECORD_BAR_TABS) {
                val source = stripTake?.source ?: takeSourceOf(tab)
                com.pocketstudio.app.record.RecordBar(
                    com.pocketstudio.app.record.recordBarUi(rhythmVm, tr("Enregistrer"), source),
                    com.pocketstudio.app.record.recordBarActions(rhythmVm, source),
                    modifier = Modifier.windowInsetsPadding(SIDE_INSETS).padding(horizontal = 8.dp, vertical = 4.dp),
                    compact = true,
                )
            }
            }
        } },
        // « Fait : … Annuler » after an intention (the part sheet, a kit).
        snackbarHost = {
            val done by rhythmVm.done.collectAsStateWithLifecycle()
            DoneBar(done, undo = rhythmVm::undoDone, dismiss = rhythmVm::dismissDone)
        },
        bottomBar = {
            Column {
                // Le Guide: its bar, or its panel (a third of a phone's height, half a tablet's). It
                // keeps quiet during a take, on the Live page and on the keyboard lying down, where
                // every dp of height is a key's (§3.1: it is quiet while the keys are played).
                // « Tuto de A à Z »: its panel takes the Guide's place while it runs.
                val tutoNow = tutoUi
                if (tutoNow != null && !showLive && !showLivePads && !showTimeline) {
                    val tutoProject by rhythmVm.project.collectAsStateWithLifecycle()
                    val tutoTransport by rhythmVm.transport.collectAsStateWithLifecycle()
                    // « Écouter ton résultat »: only once there is something of the user's to hear.
                    val mine = tutoProject.drums.lanes.any { it.hits.isNotEmpty() } || tutoProject.track(0).notes.notes.isNotEmpty() ||
                        tutoProject.tracks.any { it.notes.notes.isNotEmpty() }
                    val mineFirst = tutoNow.current.listening && com.pocketstudio.app.tuto.TutoController.mineFor(tutoNow.current, tutoProject)
                    com.pocketstudio.app.tuto.TutoPanel(
                        tutoNow, tuto.canShow(),
                        com.pocketstudio.app.tuto.TutoActions(
                            listen = tuto::listen, doIt = tuto::doIt, showMe = { tuto.showMe() }, stopDemo = tuto::stopDemo,
                            need = tuto::fulfilNeed, markDone = tuto::markDone,
                            toggle = tuto::toggleOpen, next = tuto::next, previous = tuto::previous,
                            chapters = { tutoChapters = true }, quit = tuto::quit, restart = restartTuto,
                            playMine = {
                                tuto.stopListening()
                                if (rhythmVm.project.value.song.isNotEmpty()) rhythmVm.toggleSong() else rhythmVm.togglePlay()
                            },
                            goTo = { c, i -> tuto.goTo(c, i) }, leaveEnd = tuto::leaveEnd,
                        ),
                        modifier = if (landscape) Modifier.navigationBarsPadding() else Modifier,
                        map = remember(tutoNow.progress.style) { com.pocketstudio.app.tuto.tutoMap(tutoNow.progress.style) },
                        mine = mine, minePlaying = tutoTransport.playing, mineFirst = mineFirst,
                    )
                } else if (tuto.resumable && !showLive && !showLivePads && !showTimeline) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = tuto::resume, modifier = Modifier.weight(1f).heightIn(min = 40.dp).testTag("tuto_resume")) {
                            Text(tr("Reprendre le tuto"), color = Neon.Cyan)
                        }
                        TextButton(onClick = restartTuto, modifier = Modifier.weight(1f).heightIn(min = 40.dp).testTag("tuto_restart_quit")) {
                            Text(com.pocketstudio.app.tuto.restartTutoLabel(), color = Neon.TextSecondary)
                        }
                    }
                }
                if (guideShown && !guideSide && tutoNow == null) {
                    val guideUi by guideVm.ui.collectAsStateWithLifecycle()
                    if (guideOpen) {
                        val listening by guideListen.state.collectAsStateWithLifecycle()
                        val config = LocalConfiguration.current
                        val share = if (config.smallestScreenWidthDp >= 600) 0.5f else 0.4f
                        com.pocketstudio.app.guide.GuidePanel(
                            guideUi, guideSelected, listening, guideNotice, guideActions,
                            height = (config.screenHeightDp * share).dp.coerceAtLeast(200.dp),
                            modifier = if (landscape) Modifier.navigationBarsPadding() else Modifier,
                            reference = guideReference?.name,
                            aloneCard = aloneCard,
                        )
                    } else {
                        com.pocketstudio.app.guide.GuideBar(
                            guideUi, open = false, toggle = guideActions.toggle,
                            modifier = if (landscape) Modifier.navigationBarsPadding() else Modifier,
                        )
                    }
                }
                // Phase 36: the five spaces (or the Mode simple's four tabs); lying down, the rail on the side.
                if (!landscape && !liveImmersive && !showTimeline && !showLivePads) SpaceBar(navEntries)
            }
        },
    ) { padding ->
        if (showLivePads) {
            // Drawn above the page it was opened from, which stays as it was (its scroll, its tab).
            Box(Modifier.fillMaxSize().zIndex(1f)) {
                LivePadsRoute(
                    rhythmVm, lifecycleOwner, close = closeLivePads,
                    openEffects = { fxChannel = it },
                    padsActions = com.pocketstudio.app.session.LivePadsActions(
                        tap = rhythmVm::toggleLive,
                        stopAll = rhythmVm::stopLive,
                        edit = { liveEditing = it },
                    ),
                )
            }
        }
        if (showLive) {
            Row(Modifier.padding(padding).fillMaxSize()) {
            if (landscape && !liveImmersive) SpaceRail(navEntries)
            Box(Modifier.weight(1f).fillMaxHeight()) {
                LiveRoute(
                    rhythmVm, lifecycleOwner, close = closeLive, onTab = { liveTab = it.name },
                    under = {
                        // Lot 2: the « ? » on the Live page too (its bar replaces the one that carries it elsewhere).
                        PositionStrip(
                            rhythmVm, tempoOpen = tempoSheetOpen, setTempoOpen = { tempoSheetOpen = it },
                            helpOn = helpLabels, toggleHelp = { helpLabels = !helpLabels }, reportBug = { reportBug("appui long sur « ? »") },
                        )
                    },
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
                        fullScreen = { showLivePads = true },
                    ),
                    liveEditing = liveEditing,
                    onImmersive = { liveImmersive = it },
                )
            }
            }
            return@Scaffold
        }
        if (showTimeline) {
            // The timeline's loop is a tool of that screen: leaving it plays the whole song again.
            DisposableEffect(Unit) { onDispose { rhythmVm.setSongLoop(null) } }
            Box(Modifier.padding(padding).fillMaxSize()) {
                TimelineRoute(
                    rhythmVm, lifecycleOwner, close = { showTimeline = false },
                    under = { PositionStrip(rhythmVm, tempoOpen = tempoSheetOpen, setTempoOpen = { tempoSheetOpen = it }) },
                    openEditor = openFromTimeline,
                    openLane = openLaneFromTimeline,
                    trackSound = trackSound,
                    whatPlays = { scene -> overviewCh = -1; overviewScene = scene },
                )
            }
            return@Scaffold
        }
        // The background darkens a little towards the bottom: the panels seem to float above it.
        val bg = Neon.Background
        Row(Modifier.padding(padding).background(Brush.verticalGradient(listOf(lerp(bg, Color.White, 0.03f), bg, lerp(bg, Color.Black, 0.18f))))) {
            // Landscape: the spaces on the side so the screen keeps its height (wider synth keyboard).
            if (landscape) SpaceRail(navEntries)
            Box(Modifier.weight(1f)) {
                // Coming back to the Mixeur tab starts it again at the top: a new key drops its old scroll position
                // (saved across rotations, so a rebuild keeps the count).
                var mixerVisit by rememberSaveable { mutableIntStateOf(0) }
                val shownTab = remember { arrayOf(tab) }
                if (shownTab[0] != tab) {
                    if (tab == AppTab.Mixeur) mixerVisit++
                    shownTab[0] = tab
                }
                Column(Modifier.fillMaxSize()) {
                // Phase 36: the Sons space's tracks as chips above every editor (the Mode simple's
                // Clavier: its instrument tracks alone; its Rythme: none).
                // The Mode simple's Audio pages (Ma voix, Remix, Générer, from « ⋯ », the Guide, a track,
                // the Live page): the page its caller chose, alone, with a way back to the song.
                val simpleRemix = simple && tab == AppTab.Audio
                if (tab in SONS_TABS && !(simple && tab == AppTab.Rythme) && !simpleRemix) {
                    val hp by rhythmVm.project.collectAsStateWithLifecycle()
                    val hSelected by rhythmVm.selectedTrack.collectAsStateWithLifecycle()
                    val hFreezing by rhythmVm.freezing.collectAsStateWithLifecycle()
                    val hShown = if (hp.tracks.any { it.id == hSelected }) hSelected else 0
                    SonsHeader(
                        tab, tracksUiOf(hp, if (tab == AppTab.Synthe) hShown else -1, hFreezing, bar = true),
                        go = { t -> if (t == AppTab.Audio) audioPage = 0; go(t) }, audioTracks = hp.audioTracks.size, simple = simple,
                        remixShown = tab == AppTab.Audio && audioPage == 1, openRemix = openRemix,
                    )
                }
                Box(Modifier.weight(1f)) {
                // Each tab keeps its own screen state (Notes or Clavier, folded cards, chosen tools)
                // while another tab is shown, as long as the app lives.
                tabStates.SaveableStateProvider(tab.name) {
                when (tab) {
                        AppTab.Rythme -> {
                            val project by rhythmVm.project.collectAsStateWithLifecycle()
                            val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                            val liveState = rhythmVm.live.collectAsStateWithLifecycle()
                            val canUndo by rhythmVm.session.canUndo.collectAsStateWithLifecycle()
                            val canRedo by rhythmVm.session.canRedo.collectAsStateWithLifecycle()
                            val take by rhythmVm.audioRecording.collectAsStateWithLifecycle()
                            val performance by rhythmVm.take.collectAsStateWithLifecycle()
                            val sampleInfo by rhythmVm.sampleInfo.collectAsStateWithLifecycle()
                            val sampleTask by rhythmVm.sampleTask.collectAsStateWithLifecycle()
                            val slicer by rhythmVm.slicer.collectAsStateWithLifecycle()
                            val packsOnPhone by rhythmVm.packsOnPhone.collectAsStateWithLifecycle()
                            val kitLibrary by rhythmVm.library.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                rhythmVm.refreshPacks()
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                            }
WithPartHeader(rhythmVm, simple) { partModifier ->
                            WithLive(liveState) { live ->
                                                        RhythmScreen(
                                modifier = partModifier,
                                simple = simple,
                                record = {
                                    val bar = com.pocketstudio.app.record.recordBarUi(rhythmVm, tr("Enregistrer mes pads"), com.pocketstudio.app.record.TakeSource.PADS)
                                    // The pads' take and « Garder ce que je viens de jouer » only when they have something to say.
                                    if (bar.take != null || bar.canKeepPlayed || bar.empty) {
                                        com.pocketstudio.app.record.RecordBar(bar, com.pocketstudio.app.record.recordBarActions(rhythmVm, com.pocketstudio.app.record.TakeSource.PADS), compact = true)
                                    }
                                },
                                openPad = guidePad,
                                padOpened = { guidePad = null },
                                alonePads = rhythmVm.alone.collectAsStateWithLifecycle().value?.target?.takeIf { it.channels.isEmpty() }?.pads.orEmpty(),
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
                                    openTempo = { tempoSheetOpen = true },
                                    selectPad = rhythmVm::selectPad,
                                    setPadVolume = rhythmVm::setPadVolume,
                                    setPadPanOf = rhythmVm::setPadPanOf,
                                    padPanDone = rhythmVm::padPanDoneOf,
                                    togglePadMute = rhythmVm::togglePadMute,
                                    removeSampleOf = rhythmVm::removeSampleOf,
                                    padSounds = { lane -> padSoundOptions(lane, packsOnPhone).map { it.name } },
                                    listenPadAlone = { lane ->
                                        val a = rhythmVm.alone.value
                                        if (a != null && a.target.channels.isEmpty() && lane in a.target.pads) rhythmVm.stopAlone()
                                        else rhythmVm.toggleAlone(com.pocketstudio.app.rhythm.AloneTarget.ofKey(com.pocketstudio.model.guide.GuideTrack.pad(lane)))
                                    },
                                    holdAlone = rhythmVm::holdAlone,
                                    pickPadSound = { lane, i ->
                                        padSoundOptions(lane, packsOnPhone).getOrNull(i)?.let { option ->
                                            rhythmVm.dismissSampleError()
                                            libraryVm.use(option.ref) { pcm ->
                                                if (!rhythmVm.placeOnPad(pcm, option.name.substringBefore(" · "), oneShot = true)) {
                                                    libraryVm.notice(tr("Un son est encore en train d'être posé ou enregistré : réessaie dans un instant."))
                                                }
                                            }
                                        }
                                    },
                                    openKits = rhythmVm::refreshPacks,
                                    chooseKit = rhythmVm::chooseKit,
                                    paintStep = rhythmVm::paintStep,
                                    paintEnd = rhythmVm::paintEnd,
                                    setRoll = rhythmVm::setRoll,
                                    burst = rhythmVm::burstAt,
                                    setPadLength = rhythmVm::setPadLength,
                                    setHitChance = rhythmVm::setHitChance,
                                    rerollChances = rhythmVm::rerollChances,
                                ),
                                padSound = PadSoundUi(sampleInfo, sampleTask, slicer),
                                kits = rhythmVm.kitChoices(packsOnPhone, kitLibrary),
                            )
                            }
                            }
                        }
                        AppTab.Acide -> {
                            val project by rhythmVm.project.collectAsStateWithLifecycle()
                            val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                            val liveState = rhythmVm.live.collectAsStateWithLifecycle()
                            val selected by rhythmVm.selectedAcidStep.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                            }
                            val favorites by rhythmVm.favorites.collectAsStateWithLifecycle()
                            val automationRecording by rhythmVm.automationRecord.collectAsStateWithLifecycle()
WithPartHeader(rhythmVm, simple) { partModifier ->
                            WithLive(liveState) { live ->
                                                        AcidScreen(
                                modifier = partModifier,
                                record = {
                                    com.pocketstudio.app.record.RecordBar(
                                        com.pocketstudio.app.record.recordBarUi(rhythmVm, tr("Enregistrer les boutons"), com.pocketstudio.app.record.TakeSource.ACID),
                                        com.pocketstudio.app.record.recordBarActions(rhythmVm, com.pocketstudio.app.record.TakeSource.ACID),
                                    )
                                },
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
                                focusKnob = acidFocus,
                                onFocused = { acidFocus = null },
                                browser = acidSoundBrowser(rhythmVm, project, favorites),
                            )
                            }
                            }
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
                            val liveState = rhythmVm.live.collectAsStateWithLifecycle()
                            val library by rhythmVm.library.collectAsStateWithLifecycle()
                            val trackSoundMessage by rhythmVm.trackSoundBusy.collectAsStateWithLifecycle()
                            val bankId by rhythmVm.bankId.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                            }
                            val favorites by rhythmVm.favorites.collectAsStateWithLifecycle()
                            val transcription by rhythmVm.transcription.collectAsStateWithLifecycle()
                            val trackFx = com.pocketstudio.model.InstrumentTrack.mixerChannel(shownTrack)
                            val noteClipboard by rhythmVm.noteClipboard.collectAsStateWithLifecycle()
                            val freezing by rhythmVm.freezing.collectAsStateWithLifecycle()
                            val frozenTracks = remember(fullProject) { com.pocketstudio.model.Freeze.frozenIds(fullProject) }
WithPartHeader(rhythmVm, simple) { partModifier ->
                                                        SynthScreen(
                                modifier = partModifier,
                                record = { compact ->
                                    com.pocketstudio.app.record.RecordBar(
                                        com.pocketstudio.app.record.recordBarUi(rhythmVm, tr("Enregistrer ma mélodie"), com.pocketstudio.app.record.TakeSource.KEYS),
                                        // Audit final (01 M3, 05 #5): the Mode simple's melody on « Ma mélodie », heard through the song.
                                        com.pocketstudio.app.record.recordBarActions(rhythmVm, com.pocketstudio.app.record.TakeSource.KEYS, melody = simple),
                                        compact = compact,
                                    )
                                },
                                notesRequest = notesRequest,
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
                                    automate = { param -> rhythmVm.automateKnob(com.pocketstudio.model.AutoTarget.synth(shownTrack, param)) },
                                ),
                                notes = NotesUi(
                                    stepsPerBar = project.stepsPerBar,
                                    clip = project.synthNotes,
                                    ghosts = NoteClip.ghostsOf(project.acid, project.synthNotes.steps),
                                    playStep = -1,
                                    // Read where the play line is drawn, not here (audit 16: the tab redrawn at each step).
                                    playStepOf = { liveState.value.absoluteStep },
                                    actions = PianoRollActions(
                                        add = rhythmVm::addNote,
                                        remove = rhythmVm::removeNote,
                                        change = rhythmVm::changeNote,
                                        quantize = rhythmVm::quantizeNotes,
                                        asPlayed = rhythmVm::notesAsPlayed,
                                        setSteps = rhythmVm::setNoteClipSteps,
                                        clear = rhythmVm::clearNotes,
                                        addChord = rhythmVm::addChord,
                                        setHarmony = rhythmVm::setHarmony,
                                        applyProgression = rhythmVm::applyProgression,
                                        applyBassLine = rhythmVm::applyBassLine,
                                        applyArpeggio = rhythmVm::applyArpeggio,
                                        setPerformance = rhythmVm::setPerformance,
                                        replaceClip = rhythmVm::replaceNoteClip,
                                        copyNotes = rhythmVm::copyNotes,
                                    ),
                                    harmony = project.harmony,
                                    clipboard = noteClipboard,
                                    frozen = shownTrack in frozenTracks,
                                    songChords = { songChordsOpen = true },
                                    hints = com.pocketstudio.app.tuto.TutoHints.notes?.takeIf { it.first == shownTrack }?.second.orEmpty(),
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
                                    automatePlaits = { param -> rhythmVm.automateKnob(com.pocketstudio.model.AutoTarget.plaits(shownTrack, param)) },
                                    sampledMessage = trackSoundMessage?.let { if (it == RhythmViewModel.BUSY_TRACK_SOUND) tr("Préparation du son…") else it },
                                    real = realInstrumentsUi(rhythmVm, libraryVm, shownTrack) {},
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
                                // Phase 36: the tracks are the Sons space's chips, above (none here).
                                tracks = tracksUiOf(fullProject, shownTrack, freezing, bar = false),
                                browser = trackSoundBrowser(rhythmVm, project, favorites),
                                simple = simple,
                            )
                            }
                        }
                        // The Mode simple's « Mix »: the same mixer, in big faders and words.
                        AppTab.Mixeur -> if (simple) SimpleMixTab(rhythmVm) else androidx.compose.runtime.key(mixerVisit) {
                            val project by rhythmVm.project.collectAsStateWithLifecycle()
                            val sessionUi by rhythmVm.sessionUi.collectAsStateWithLifecycle()
                            val spectrum by rhythmVm.spectrum.collectAsStateWithLifecycle()
                            LaunchedEffect(analyserOn) {
                                if (analyserOn) lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollSpectrum() }
                            }
                            // Read only by the meters when they draw: a reading (20 a second) composes nothing.
                            val meters = rhythmVm.meters.collectAsStateWithLifecycle()
                            val loudness = rhythmVm.loudness.collectAsStateWithLifecycle()
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
                                    setLoudnessTarget = rhythmVm::setLoudnessTarget,
                                    setExportCdRate = rhythmVm::setExportCdRate,
                                    setExportBits16 = rhythmVm::setExportBits16,
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
                                    reductions = rhythmVm.reductions,
                                ),
                                master = project.mixer.master,
                                masterDb = project.mixer.masterDb,
                                loudness = { loudness.value },
                                target = project.mixer.target,
                                styleLufs = com.pocketstudio.model.LoudnessTargets.forStyle(project.style, project.mixVersion),
                                buses = project.mixer.buses,
                                // Tutorial testers (lot « tuto 4 », b/139): no silent « Basse acide » strip in the tutorial's project.
                                strips = mixerStrips(project).let { all ->
                                    if (tutoUi != null && project.allScenes.none { s -> s.acid.steps.take(s.acid.length.coerceAtLeast(0)).any { it.gate } }) all.filter { it.ch != com.pocketstudio.model.AutoTarget.ACID_CHANNEL } else all
                                },
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
                                    preferredCh = workChannel,
                                    parts = project.partMarks(),
                                    partSteps = project.sceneSteps(project.scene),
                                    playPartStep = { if (playing) live.value.absoluteStep else -1 },
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
                                    setResolution = rhythmVm::setLaneResolution,
                                    setLoopSteps = rhythmVm::setAutomationLoopSteps,
                                    // « Voir le potard »: the 303 knob on the Acide tab, an effect in its track's box.
                                    showControl = { t ->
                                        when {
                                            t.kind == com.pocketstudio.model.AutoTarget.ACID -> { acidFocus = t.param; tab = AppTab.Acide }
                                            t.kind == com.pocketstudio.model.AutoTarget.BUS_PARAM -> mixerFocus = com.pocketstudio.app.mixer.Where.Buses
                                            t.ch == com.pocketstudio.model.Mixer.MASTER -> mixerFocus = com.pocketstudio.app.mixer.Where.Master
                                            else -> fxChannel = t.ch
                                        }
                                    },
                                ),
                                export = export,
                                hasSong = project.song.isNotEmpty(),
                                preferChannel = { workChannel = it },
                            )
                        }
                        AppTab.Morceau -> {
                            val project by rhythmVm.project.collectAsStateWithLifecycle()
                            val transport by rhythmVm.transport.collectAsStateWithLifecycle()
                            val sessionUi by rhythmVm.sessionUi.collectAsStateWithLifecycle()
                            val liveStates by rhythmVm.liveStates.collectAsStateWithLifecycle()
                            val sampleTask by rhythmVm.sampleTask.collectAsStateWithLifecycle()
                            val followSong by rhythmVm.followSong.collectAsStateWithLifecycle()
                            val songLoop by rhythmVm.songLoop.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLiveStates() }
                            }
                            // The song's clock and progress: read only when drawn (and once a beat by the clock's words).
                            val songLive = rhythmVm.live.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { rhythmVm.pollLive() }
                            }
                            val all = project.allScenes
                            val englishChords = com.pocketstudio.app.settings.englishNotes()
                            val sessionView: @Composable (Boolean, (@Composable () -> Unit)?) -> Unit = { simpleView, top ->
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
                                    upcoming = if (transport.playing) sessionUi.upcoming(project.song.size, project.songLoops, songLoop) else -1,
                                    songLoops = project.songLoops,
                                    followSong = followSong,
                                    songStep = { if (transport.playing) songLive.value.songStep else -1 },
                                    partSteps = project.song.map { project.partSteps(it) },
                                    sheetOf = { i -> com.pocketstudio.app.session.partSheetUi(rhythmVm.project.value, i) },
                                    versions = project.songVersions.size,
                                    chords = remember(project, englishChords) { com.pocketstudio.app.session.songChordsStrip(project, englishChords) },
                                ),
                                SessionActions(
                                    openVersions = { songVersionsOpen = true },
                                    openChords = { songChordsOpen = true },
                                    partIntent = rhythmVm::partIntent,
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
                                    setSongLoops = rhythmVm::setSongLoops,
                                    setFollowSong = rhythmVm::setFollowSong,
                                    performance = { param, v -> if (NativeEngine.isAvailable) NativeEngine.setPerformance(param, v) },
                                    togglePartTrack = rhythmVm::togglePartTrack,
                                    setPartScene = rhythmVm::setPartScene,
                                    setBeatsPerBar = rhythmVm::setBeatsPerBar,
                                    openLive = openLive,
                                    whatPlays = { scene -> overviewCh = -1; overviewScene = scene ?: -1 },
                                    openTimeline = { showTimeline = true },
                                ),
                                arrangeOnly = !simpleView,
                                simple = simpleView,
                                top = top,
                                tracks = if (simpleView) ({
                                    val alone by rhythmVm.alone.collectAsStateWithLifecycle()
                                    SimpleTracksCard(simpleTracks(project, hideFirst = tutoHidesFirst), simpleTracksActions, alone)
                                }) else null,
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
                                    fullScreen = { showLivePads = true },
                                ),
                            )
                            }
                            if (simple) {
                                sessionView(true, null)
                            } else {
                                // Phase 36: the one view of the arrangement, its two halves a switch apart.
                                val hasSong = project.song.isNotEmpty()
                                val showParts = partsView || !hasSong
                                val switch: @Composable () -> Unit = {
                                    StructureSwitch(showParts, timelineEnabled = hasSong) { partsView = it; navPrefs.partsView = it }
                                }
                                if (showParts) {
                                    sessionView(false, switch)
                                } else {
                                    TimelineRoute(
                                        rhythmVm, lifecycleOwner, close = { partsView = true; navPrefs.partsView = true },
                                        openEditor = openFromTimeline, openLane = openLaneFromTimeline,
                                        embedded = true, lead = switch, expand = { showTimeline = true },
                                        trackSound = trackSound, whatPlays = { scene -> overviewCh = -1; overviewScene = scene },
                                    )
                                }
                            }
                        }
                        AppTab.Audio -> Column {
                            if (simpleRemix) {
                                // Audit final (03 G4, 05, 08): the page asked for (« Ma voix » too, it used to be
                                // forced to the Remix), alone, and back to the song.
                                androidx.activity.compose.BackHandler { go(AppTab.Morceau) }
                                Row(Modifier.fillMaxWidth().background(Neon.Surface).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { go(AppTab.Morceau) }, modifier = Modifier.heightIn(min = 48.dp).testTag("remix_back")) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text(" " + tr("Mon morceau"))
                                    }
                                    Text(
                                        simpleAudioTitle(audioPage), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.Cyan,
                                        modifier = Modifier.testTag("simple_audio_title"),
                                    )
                                }
                            } else {
                            // Scrollable: whole words on a small phone or with big text (never "Remi/x").
                            androidx.compose.material3.ScrollableTabRow(selectedTabIndex = audioPage, containerColor = Neon.Surface, contentColor = Neon.Cyan, edgePadding = 8.dp) {
                                listOf(tr("Pistes audio"), tr("Remix"), tr("Accordeur"), tr("Générer")).forEachIndexed { i, label ->
                                    Tab(selected = audioPage == i, onClick = { audioPage = i }, text = { Text(label, maxLines = 1, softWrap = false) }, modifier = Modifier.testTag("audio_page_$i"))
                                }
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
WithPartHeader(rhythmVm, simple) { partModifier ->
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
                                        monitor = settings.monitorVoice,
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
                                                audioPunch = null
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
                                        chooseTake = rhythmVm::chooseTake,
                                        realign = rhythmVm::realignTake,
                                        punch = { id, from, to ->
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                rhythmVm.recordPunch(id, from, to)
                                            } else {
                                                audioPunch = "$id,$from,$to"
                                                audioMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        setMonitor = { on -> com.pocketstudio.app.settings.SettingsStore.update(context) { it.copy(monitorVoice = on) } },
                                    ),
                                    modifier = partModifier,
                                )
                                }
                            } else {
                            val remixState by remixVm.state.collectAsStateWithLifecycle()
                            val sets by remixVm.sets.collectAsStateWithLifecycle()
                            val modelInstalled by remixVm.modelInstalled.collectAsStateWithLifecycle()
                            val preview by remixVm.preview.collectAsStateWithLifecycle()
                            val remixMemory by remixVm.memoryProblem.collectAsStateWithLifecycle()
                            val remixMemoryNever by remixVm.memoryNever.collectAsStateWithLifecycle()
                            // The free memory changes: read again each time the Remix page is shown.
                            LaunchedEffect(Unit) { remixVm.checkMemory() }
                            // « Écouter » belongs to this page: leaving it stops the song (audit 02 M6),
                            // never mixed into the project played elsewhere. Turning the phone keeps it.
                            DisposableEffect(Unit) {
                                onDispose { if (context.findActivity()?.isChangingConfigurations != true) remixVm.stop() }
                            }
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
                                    downloadModel = { remixVm.downloadModel(); askNotifications() },
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
                                    // The project's ▶ and « Écouter » never sound together.
                                    togglePlay = { remixVm.pause(); rhythmVm.togglePlay() },
                                    retry = remixVm::retry,
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
                        AppTab.Outils -> PlusSpace(
                            plusEntries(
                                projects = { showProjects = true },
                                glossary = { learn.open("") },
                                missions = startMissions,
                                bugReport = { reportBug("Plus") },
                                simple = simple,
                                setSimple = { v -> SettingsStore.update(context) { it.copy(simpleMode = v) } },
                            ),
                            page = toolPage,
                            open = { page -> toolPage = page; ToolPage.save(context, page) },
                            // Lot 2: the Mode simple has no Plus: « ← Retour » goes back to the song.
                            back = { toolPage = null; if (simple) go(AppTab.Morceau) },
                            backLabel = if (simple) tr("Retour") else null,
                            widePage = ToolPage.load(context),
                        ) { shownPage ->
                            when (shownPage) {
                                ToolPage.Aide -> com.pocketstudio.app.settings.HelpPage(
                                    com.pocketstudio.app.settings.HelpActions(
                                        openGuide = { showGuide = true },
                                        // G5: the Guide took over « Prochaine étape »: its panel, on the song.
                                        showSteps = { toolPage = null; go(AppTab.Morceau); guideOpen = true },
                                        missions = startMissions,
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
                                        autoKnobs = {
                                            val knobsOn by knobsVm.enabled.collectAsStateWithLifecycle()
                                            com.pocketstudio.app.midi.AutoKnobsCard(knobsOn, knobsVm::setEnabled) {
                                                knobsVm.open(midi.sources.firstOrNull { it.id == midi.connected }?.name)
                                            }
                                        },
                                        controller = {
                                            val knobSettings by knobsVm.settings.collectAsStateWithLifecycle()
                                            val learningControl by knobsVm.learningControl.collectAsStateWithLifecycle()
                                            var legendScreen by rememberSaveable { mutableStateOf(com.pocketstudio.app.midi.KnobScreen.Rythme) }
                                            val name = midi.controller?.name
                                            val rows = remember(legendScreen, project, knobSettings, midi.controller) { knobsVm.legend(legendScreen) }
                                            com.pocketstudio.app.midi.ControllerCard(
                                                com.pocketstudio.app.midi.ControllerCardUi(
                                                    controller = midi.controller, monitor = midiVm.monitor.collectAsStateWithLifecycle().value, screen = legendScreen, legend = rows,
                                                    legendOn = knobSettings.legend, customized = name != null && knobSettings.customized(name),
                                                    learningPads = midi.learningPads, learningControl = learningControl,
                                                ),
                                                com.pocketstudio.app.midi.ControllerCardActions(
                                                    pickScreen = { legendScreen = it },
                                                    setLegend = knobsVm::setLegend,
                                                    learnPads = { on -> if (on) { padsHeard.clear(); midiVm.learnPads(true) } else finishPads() },
                                                    learnControl = knobsVm::learnControl,
                                                    forget = { name?.let(knobsVm::forget) },
                                                    openKnobs = { knobsVm.open(name) },
                                                ),
                                            )
                                        },
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
                        // Shown above (return@Scaffold): never here.
                        AppTab.Live -> Unit
                        // Mode simple: « Envoyer », the « Fini ? » page.
                        AppTab.Envoyer -> com.pocketstudio.ui.ReadingWidth(maxWidth = 640.dp) {
                            com.pocketstudio.app.export.ShareSheetRoute(
                                rhythmVm, sharePrefs, close = {},
                                toStructure = { go(AppTab.Morceau) },
                                // Audit final (01 M6, 05, 08): no « Voir dans Mix » (it left the Mode simple without a word).
                                toMixer = null,
                                otherExports = { SettingsStore.update(context) { it.copy(simpleMode = false) }; go(AppTab.Mixeur); mixerFocus = com.pocketstudio.app.mixer.Where.Export },
                                video = { showVideo = true },
                                page = true,
                            )
                        }
                    }
                }
                }
                }
            }
            // Audit final: lying down, the Guide's panel on the side (the screen keeps its height), not half of it under.
            if (guideSide) {
                val sideUi by guideVm.ui.collectAsStateWithLifecycle()
                val sideListening by guideListen.state.collectAsStateWithLifecycle()
                com.pocketstudio.app.guide.GuidePanel(
                    sideUi, guideSelected, sideListening, guideNotice, guideActions, height = null,
                    modifier = Modifier.width((LocalConfiguration.current.screenWidthDp * GUIDE_SIDE_SHARE).dp.coerceIn(280.dp, 440.dp)).fillMaxHeight().navigationBarsPadding(),
                    reference = guideReference?.name,
                    aloneCard = aloneCard,
                )
            }
        }
    }
    // Phase 36: « ? qui étiquette l'écran » over the screen, and « Voir le réglage » pointing at a control.
    if (helpLabels && !showProjects) {
        com.pocketstudio.app.learn.HelpLabelsOverlay(spots, hide = { helpLabels = false }, reportBug = { reportBug("étiquettes « ? »") })
    } else if (tutoSpot != null && !showProjects) {
        com.pocketstudio.app.learn.SpotRingOverlay(spots, tutoSpot.orEmpty())
    } else if (guideSpot != null && !showProjects) {
        // « Voir le réglage »: the control lit, a word of what it is for, gone at a touch.
        com.pocketstudio.app.learn.SpotlightOverlay(spots, guideSpot.orEmpty()) { modifier, _ ->
            com.pocketstudio.app.guide.GuideSpotCard(close = { guideSpot = null }, modifier = modifier)
        }
    }
    }
    }
    }

    LaunchPrompts(mainVm)
    val bugProject by rhythmVm.project.collectAsStateWithLifecycle()
    com.pocketstudio.app.report.BugReportHost(bugVm, com.pocketstudio.model.DefaultNames.shown(bugProject.name))
}

@Composable
private fun LaunchPrompts(vm: MainViewModel) {
    val context = LocalContext.current
    val update by vm.update.collectAsStateWithLifecycle()

    // A crash of the last session is offered by the banner under the top bar (« Signaler un bug »),
    // which holds nothing back: the update prompt still shows.
    when (val u = update) {
        is UpdateState.Available, is UpdateState.Failed -> {
            val info = if (u is UpdateState.Available) u.info else (u as UpdateState.Failed).info
            ScaledAlertDialog(
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
        is UpdateState.Downloading -> ScaledAlertDialog(
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
    val global = if (p.audioTracks.any { it.clip != null } || p.remix.setId != null) " " + tr("Les pistes audio et le remix jouent dans toutes les parties.") else ""
    return when {
        all.getOrNull(p.scene)?.isEmpty != false -> tr("Partie %s : nouvelle et vide. Remplis-la dans Rythme, Acide ou Synthé.", name) + global
        else -> p.copyOf(p.scene)?.let { tr("Partie %1\$s : copie de la partie %2\$s. Change-la pour en faire une variante, ou « Vider ».", name, Scene.NAMES[it]) } ?: ""
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
    reductions = vm.reductions,
)

/** A strip's quick controls and ready-made effects (hold to listen, starred ones kept on the phone). */
fun quickFxActions(vm: RhythmViewModel): com.pocketstudio.app.mixer.QuickFxActions = com.pocketstudio.app.mixer.QuickFxActions(
    vm::setMacro, vm::setMacroPair, vm::applyFx,
    audition = vm::auditionFx,
    favourites = SettingsStore.favouriteFx,
    toggleFavourite = { id -> SettingsStore.toggleFavouriteFx(vm.getApplication(), id) },
)

/**
 * The timeline rows: drums, acid, then each instrument and audio track in its colour; an audio
 * take's waveform from [info] (the loaded sounds' peaks) when it is there.
 */
fun timelineRows(p: Project, info: Map<String, com.pocketstudio.app.rhythm.SampleInfo> = emptyMap()): List<com.pocketstudio.app.session.TimelineRow> =
    listOf(
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.DRUMS, "Batterie", com.pocketstudio.app.live.DRUMS_COLOR),
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.ACID, "Basse acide", androidx.compose.ui.graphics.Color(0xFF81C784)),
    ) + p.instrumentTracks.map { t ->
        com.pocketstudio.app.session.TimelineRow(com.pocketstudio.model.SongPart.inst(t.id), t.name, com.pocketstudio.ui.trackColor(t.color))
    } + p.audioTracks.map { t ->
        // Where its take is really heard in each part (its scene's parts, its place in the song).
        val clip = t.clip
        com.pocketstudio.app.session.TimelineRow(
            com.pocketstudio.model.SongPart.audio(t.id), t.name, com.pocketstudio.ui.trackColor(t.color), spans = p.audioSpans(t.id),
            peaks = clip?.let { info[it.file]?.peaks },
            takeSteps = clip?.steps(p.tempoBpm) ?: 0,
            takeStart = if (clip?.song == true) clip.start else -1,
        )
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
/**
 * The sounds a pad's sheet offers (appui long on a pad): the built-in kit's voices for its kind,
 * then the downloaded packs' sounds of that kind, a dozen at most.
 */
private fun padSoundOptions(lane: Int, installed: List<com.pocketstudio.model.PackManifest>): List<com.pocketstudio.app.library.SoundOption> =
    com.pocketstudio.app.library.libraryCategories(padKind(lane), installed).flatMap { it.options }.take(12)

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
    restoreVersion = vm::restoreSongVersion,
    dropVersion = vm::dropSongVersion,
    noteOn = vm::liveNoteOn,
    noteOff = vm::liveNoteOff,
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
    /** Under the page's own bar: the position strip (tempo, « mes. x/y »). */
    under: (@Composable () -> Unit)? = null,
    onImmersive: (Boolean) -> Unit = {},
    onTab: (com.pocketstudio.app.live.LivePanelTab) -> Unit = {},
) = LiveStateHost(vm, lifecycleOwner, liveEditing) { ui, pads ->
    com.pocketstudio.app.live.LiveScreen(
        ui,
        liveActionsOf(
            vm, back = close, openEffects = openEffects,
            togglePlay = {
                // ▶ here plays what the grid shows: the scene, not the song.
                if (ui.playing) vm.togglePlay() else vm.launchRow(ui.project.scene)
            },
        ),
        pads = pads,
        padsActions = padsActions,
        under = under,
        onImmersive = onImmersive,
        onTab = onTab,
    )
}

/**
 * The live pads on the whole screen (from Morceau or the Live page's Pads), with the Live page's
 * effects sliding in: the same state, the same gestures as there.
 */
@Composable
internal fun LivePadsRoute(
    vm: RhythmViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    close: () -> Unit,
    openEffects: (Int) -> Unit,
    padsActions: com.pocketstudio.app.session.LivePadsActions,
) = LiveStateHost(vm, lifecycleOwner, liveEditing = null) { ui, pads ->
    val columns = com.pocketstudio.app.live.liveColumnsOf(ui.project)
    val tracks = columns.map { it.name to it.color }
    val actions = liveActionsOf(vm, back = close, openEffects = openEffects)
    val fx = rememberSaveable(saver = com.pocketstudio.app.live.LiveFxState.Saver) { com.pocketstudio.app.live.LiveFxState() }
    var versionsOpen by rememberSaveable { mutableStateOf(false) }
    com.pocketstudio.app.session.LivePadsFullScreen(
        pads, padsActions, close,
        tempoBpm = ui.project.tempoBpm, beatsPerBar = ui.project.beatsPerBar, playing = ui.playing, clockOf = ui.clockOf,
        colorOf = { i, clip -> com.pocketstudio.app.session.livePadColor(clip.name, i, tracks) },
        // ▶ means the same as on the Live page: the scene shown, not the song.
        togglePlay = { if (ui.playing) vm.togglePlay() else vm.launchRow(ui.project.scene) },
        effects = { m -> com.pocketstudio.app.live.LiveFx(ui, columns, actions, m, fx) },
        // The Live take from here too: the pads played are kept with it.
        record = { com.pocketstudio.app.live.RecordButton(ui.take, actions.record) },
        banner = { com.pocketstudio.app.live.TakeBanner(ui, actions, showReady = false, openVersions = { versionsOpen = true }) },
    )
    if (versionsOpen) {
        com.pocketstudio.app.record.SongVersionsSheet(
            ui.project,
            com.pocketstudio.app.record.SongVersionsActions(restore = { versionsOpen = false; vm.restoreSongVersion(it) }, drop = vm::dropSongVersion, close = { versionsOpen = false }),
        )
    }
}

/**
 * The Live page's state from the ViewModel, polled while shown (the grid's clock, the pads'
 * lights, the spectrum while a pad XY shows it), for the Live page and the full-screen pads.
 */
@Composable
private fun LiveStateHost(
    vm: RhythmViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    liveEditing: Int?,
    content: @Composable (com.pocketstudio.app.live.LiveScreenUi, com.pocketstudio.app.session.LivePadsUi) -> Unit,
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
        content(
            com.pocketstudio.app.live.LiveScreenUi(
                project, grid, playing = transport.playing, muted = muted, acid = acid, returns = returns, take = take, takeAppend = takeAppend,
                chops = chops, takeUndoable = vm.canUndoPerformance(), clockOf = clockOf,
                spectrum = spectrumOf, beatHaptics = settings.liveBeatHaptics, fxOf = liveFxOf,
            ),
            com.pocketstudio.app.session.LivePadsUi(
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
        )
    }
}

/** The song's curves shown on the timeline (« Morceau » mode, with points), each under its track or with the whole mix. */
fun timelineLanes(p: Project): List<com.pocketstudio.app.session.TimelineLane> {
    val strips = mixerStrips(p)
    val keys = p.partKeys
    return (keys + listOf<String?>(null)).flatMap { key ->
        p.songLanesOf(key).map { i ->
            com.pocketstudio.app.session.TimelineLane(
                i, key, com.pocketstudio.app.mixer.automationLabel(p.automation[i].target, p.mixer.channels, p.mixer.master, strips),
            )
        }
    }
}

/** The « Ligne du temps » with its state from the ViewModel; every gesture goes back to it. */
@Composable
internal fun TimelineRoute(
    vm: RhythmViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    close: () -> Unit,
    openEditor: (Int, String) -> Unit,
    openLane: (Int) -> Unit,
    /** Under the timeline's own bar: the position strip (tempo, « mes. x/y »). */
    under: (@Composable () -> Unit)? = null,
    /** Phase 36: inside the Structure space (see [com.pocketstudio.app.session.TimelineScreen]). */
    embedded: Boolean = false,
    lead: (@Composable () -> Unit)? = null,
    expand: (() -> Unit)? = null,
    trackSound: ((String) -> Unit)? = null,
    whatPlays: (Int) -> Unit = {},
) {
    val project by vm.project.collectAsStateWithLifecycle()
    val transport by vm.transport.collectAsStateWithLifecycle()
    val sessionUi by vm.sessionUi.collectAsStateWithLifecycle()
    val loop by vm.songLoop.collectAsStateWithLifecycle()
    val canUndo by vm.session.canUndo.collectAsStateWithLifecycle()
    val canRedo by vm.session.canRedo.collectAsStateWithLifecycle()
    val take by vm.audioRecording.collectAsStateWithLifecycle()
    val recording by vm.recording.collectAsStateWithLifecycle()
    val performance by vm.take.collectAsStateWithLifecycle()
    // The song's place: read only when drawn (the playhead), the screen never recomposes with it.
    val live = vm.live.collectAsStateWithLifecycle()
    val songStep = remember(live, transport.playing) { { if (transport.playing) live.value.songStep else -1 } }
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.pollLive() }
    }
    val sampleInfo by vm.sampleInfo.collectAsStateWithLifecycle()
    val rows = remember(project, sampleInfo) { timelineRows(project, sampleInfo) }
    val lanes = remember(project) { timelineLanes(project) }
    // No undo during a take: it could remove the track it goes to.
    val undoable = take == null && !recording && !performance.running
    // The timeline's loop is a tool of that view: leaving it plays the whole song again.
    if (embedded) DisposableEffect(Unit) { onDispose { vm.setSongLoop(null) } }
    com.pocketstudio.app.session.TimelineScreen(
        under = under, embedded = embedded, lead = lead, expand = expand,
        ui = com.pocketstudio.app.session.TimelineUi(
            project, rows, lanes,
            playing = transport.playing,
            songIndex = if (transport.playing) sessionUi.shownSongIndex else -1,
            upcoming = if (transport.playing) sessionUi.upcoming(project.song.size, project.songLoops, loop) else -1,
            songStep = songStep,
            loop = loop,
            canUndo = canUndo && undoable,
            canRedo = canRedo && undoable,
        ),
        actions = com.pocketstudio.app.session.TimelineActions(
            back = close,
            togglePlay = {
                // Looping: ▶ starts on the loop's first part.
                val l = vm.songLoop.value
                if (!vm.transport.value.playing && l != null) vm.playSongPart(l.first) else vm.toggleSong()
            },
            playPart = vm::playSongPart,
            togglePartTrack = vm::togglePartTrack,
            setPartScene = vm::setPartScene,
            duplicatePart = vm::duplicateSongPart,
            removePart = vm::removeSongPart,
            movePart = vm::moveSongPart,
            setRepeats = vm::setSongRepeats,
            splitPart = vm::splitSongPart,
            insertPart = vm::insertSongPart,
            openEditor = openEditor,
            toggleMute = vm::toggleMute,
            toggleSolo = vm::toggleSolo,
            setLoop = vm::setSongLoop,
            openLane = openLane,
            undo = vm::undo,
            redo = vm::redo,
            partIntent = vm::partIntent,
            trackSound = trackSound,
            whatPlays = whatPlays,
        ),
    )
}


/**
 * The editors' « Partie : C · Drop » line above [content] (given the modifier that fills the rest):
 * what part is edited, and whether it loops or the song plays (see [partHeader]).
 */
@Composable
private fun WithPartHeader(vm: RhythmViewModel, simple: Boolean, content: @Composable (Modifier) -> Unit) {
    val project by vm.project.collectAsStateWithLifecycle()
    val transport by vm.transport.collectAsStateWithLifecycle()
    val sessionUi by vm.sessionUi.collectAsStateWithLifecycle()
    val loopPart by vm.loopPart.collectAsStateWithLifecycle()
    val armed by vm.loopArmedBySelf.collectAsStateWithLifecycle()
    val header = partHeader(project, transport.playing, transport.playing && sessionUi.songIndex >= 0, loopPart, armed)
    // Lying down, the part's name alone is already in the top bar (its scene chip): the line is
    // shown only when it has something more to say, and the editor keeps its height.
    val lying = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(Modifier.fillMaxSize()) {
        // Audit final (20 M16): the Mode simple's top bar has no part chip; the part is chosen here.
        if (!lying || header.note != null) {
            if (simple && !lying) PartHeader(header, parts = project.allScenes.indices.map { project.sceneTitle(it) }, current = project.scene, pick = vm::selectScene)
            else PartHeader(header)
        }
        content(Modifier.weight(1f))
    }
}

/**
 * The sounds of the instrument track [project] is the view of (see [com.pocketstudio.model.viewOfTrack]):
 * the synth's presets, or Plaits' when the track plays that engine; the favourites; a touch applies one.
 */
private fun trackSoundBrowser(vm: RhythmViewModel, project: com.pocketstudio.model.Project, favorites: com.pocketstudio.model.Favorites, afterPick: () -> Unit = {}): SoundBrowserUi =
    if (project.synthEngine == 1) {
        SoundBrowserUi(
            SoundCategories.PLAITS, SoundLibrary.PLAITS, favorites.plaits,
            current = (SoundLibrary.PLAITS + favorites.plaits).firstOrNull { it.sound == project.plaits.copy(level = it.sound.level, gain = it.sound.gain) }?.name,
            pick = { vm.applyPlaits(SoundLibrary.PLAITS[it].sound.copy(level = project.plaits.level)); afterPick() },
            pickFavorite = { vm.applyPlaits(favorites.plaits[it].sound); afterPick() },
            deleteFavorite = vm::deletePlaitsFavorite,
            random = vm::randomPlaits,
            save = vm::savePlaitsFavorite,
            tag = "plaits_sounds",
            describe = { (it.sound as? PlaitsPatch)?.let(SoundLooks::describe) },
        )
    } else {
        SoundBrowserUi(
            SoundCategories.SYNTH, SoundLibrary.SYNTH, favorites.synth,
            current = SoundLibrary.synthNameOf(project.synth, SoundLibrary.SYNTH + favorites.synth),
            pick = { vm.applySynthPreset(SoundLibrary.SYNTH[it].sound); afterPick() },
            pickFavorite = { vm.applySynthPreset(favorites.synth[it].sound); afterPick() },
            deleteFavorite = vm::deleteSynthFavorite,
            random = vm::randomSynth,
            save = vm::saveSynthFavorite,
            tag = "synth_sounds",
            describe = { (it.sound as? SynthPatch)?.let(SoundLooks::describe) },
        )
    }

/**
 * « Changer un son » (from « Ton morceau est prêt »): the sounds of track [track] in a sheet,
 * titled with the track's name; one touch on a tile changes the sound and closes the sheet.
 */
/**
 * The multizone instruments for track [track] in the sound chooser (phase 38): downloaded or not
 * (a touch on one not there yet gives it to the track at once, its synth sound playing until the
 * download is done), heard with ▶, and the 808.
 */
@Composable
internal fun realInstrumentsUi(
    vm: RhythmViewModel, libraryVm: com.pocketstudio.app.library.SoundLibraryViewModel, track: Int, close: () -> Unit,
): com.pocketstudio.app.instruments.RealInstrumentsUi {
    val packs by libraryVm.packs.collectAsStateWithLifecycle()
    val playing by libraryVm.playing.collectAsStateWithLifecycle()
    val loading by vm.multiLoading.collectAsStateWithLifecycle()
    val project by vm.project.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { libraryVm.refreshPacks() }
    val t = project.track(track)
    val published = (packs.index as? com.pocketstudio.app.library.IndexState.Ready)?.index?.packs?.associateBy { it.id }.orEmpty()
    return com.pocketstudio.app.instruments.RealInstrumentsUi(
        items = com.pocketstudio.app.instruments.realItems(
            published, packs.installed, packs.downloading, loading,
            currentPack = t.multi.pack.takeIf { t.engine == com.pocketstudio.model.InstrumentTrack.ENGINE_MULTI },
            previewing = (playing as? com.pocketstudio.app.library.SoundRef.Instrument)?.packId,
        ),
        pick = { e ->
            vm.useMultiInstrument(e.pack, e.name, e.fallback, track)
            if (packs.installed.none { it.id == e.pack }) libraryVm.downloadPack(e.pack) else close()
        },
        preview = { libraryVm.preview(com.pocketstudio.app.library.SoundRef.Instrument(it.pack)) },
        cancel = { libraryVm.cancelDownload(it.pack) },
        use808 = { vm.use808(track); close() },
        is808 = t.engine == com.pocketstudio.model.InstrumentTrack.ENGINE_808,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SoundChooserSheet(vm: RhythmViewModel, track: Int, close: () -> Unit, real: com.pocketstudio.app.instruments.RealInstrumentsUi? = null) {
    val fullProject by vm.project.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val project = remember(fullProject, track) { fullProject.viewOfTrack(track) }
    val name = fullProject.tracks.firstOrNull { it.id == track }?.name?.let { com.pocketstudio.model.DefaultNames.shown(it) } ?: tr("Synthé")
    val state = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("sound_chooser")) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Son de : %s", name), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.testTag("sound_chooser_title"))
                    Text(tr("Touche un son : il remplace celui de cette piste dans tout le morceau."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
                TextButton(onClick = close, modifier = Modifier.testTag("sound_chooser_close")) { Text(tr("Fermer")) }
            }
            com.pocketstudio.app.sounds.SoundBrowser(trackSoundBrowser(vm, project, favorites, afterPick = close))
            real?.let { com.pocketstudio.app.instruments.RealInstruments(it) }
        }
    }
}

/** The acid's sounds (the 303's presets, the favourites); a touch applies one (see the Acide editor). */
private fun acidSoundBrowser(vm: RhythmViewModel, project: Project, favorites: com.pocketstudio.model.Favorites, afterPick: () -> Unit = {}): SoundBrowserUi = SoundBrowserUi(
    SoundCategories.ACID, SoundLibrary.ACID, favorites.acid,
    current = (SoundLibrary.ACID + favorites.acid).firstOrNull {
        it.sound.copy(tuning = project.acid.sound.tuning, level = project.acid.sound.level) == project.acid.sound
    }?.name,
    // Tuning stays; the volume is the sound's calibrated one unless the user set their own.
    pick = { vm.applyAcidSound(SoundLibrary.withAcidPreset(SoundLibrary.ACID[it].sound, project.acid.sound)); afterPick() },
    pickFavorite = { vm.applyAcidSound(favorites.acid[it].sound); afterPick() },
    deleteFavorite = vm::deleteAcidFavorite,
    random = vm::randomAcid,
    save = vm::saveAcidFavorite,
    tag = "acid_sounds",
    describe = { (it.sound as? AcidSound)?.let(SoundLooks::describe) },
)

/** Phase 36: « Changer le son » of the acid from its name (Structure, Mon morceau): its sounds in a sheet. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun AcidSoundSheet(vm: RhythmViewModel, close: () -> Unit) {
    val project by vm.project.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val state = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("acid_sound_sheet")) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Son de : %s", tr("Basse acide")), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(tr("Touche un son : il remplace celui de cette piste dans tout le morceau."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
                TextButton(onClick = close, modifier = Modifier.testTag("acid_sound_close")) { Text(tr("Fermer")) }
            }
            com.pocketstudio.app.sounds.SoundBrowser(acidSoundBrowser(vm, project, favorites, afterPick = close))
        }
    }
}

/**
 * « Mes pistes » of the Mode simple: the drums, the acid when it plays, every instrument and
 * audio track, in everyday words, each with its mixer channel.
 */
internal fun simpleTracks(p: Project, hideFirst: Boolean = false): List<SimpleTrack> {
    val channels = p.mixer.channels
    fun track(key: String, ch: Int, name: String, color: Color, pads: List<SimplePad> = emptyList()): SimpleTrack {
        val c = channels.getOrNull(ch) ?: com.pocketstudio.model.Channel()
        return SimpleTrack(key, ch, name, color, c.volume, c.mute, c.solo, pads)
    }
    val acidPlays = p.allScenes.any { s -> s.acid.steps.take(s.acid.length.coerceAtLeast(0)).any { it.gate } }
    // The drums' own sounds (the pads with hits), each heard alone from the Batterie's line.
    val pads = p.guideTracks().filter { it.key.startsWith("pad:") }.mapNotNull { t ->
        t.key.removePrefix("pad:").toIntOrNull()?.let { SimplePad(it, com.pocketstudio.model.DefaultNames.shown(t.name)) }
    }
    return listOfNotNull(
        track(com.pocketstudio.model.SongPart.DRUMS, 0, tr("Batterie"), com.pocketstudio.app.live.DRUMS_COLOR, pads),
        if (acidPlays) track(com.pocketstudio.model.SongPart.ACID, 1, tr("Basse acide"), Color(0xFF81C784)) else null,
    ) + p.instrumentTracks.filter { !(hideFirst && it.id == 0) }.map { t ->
        track(com.pocketstudio.model.SongPart.inst(t.id), com.pocketstudio.model.InstrumentTrack.mixerChannel(t.id), com.pocketstudio.model.DefaultNames.shown(t.name), com.pocketstudio.ui.trackColor(t.color))
    } + p.audioTracks.map { t ->
        track(com.pocketstudio.model.SongPart.audio(t.id), com.pocketstudio.model.AudioTrack.mixerChannel(t.id), com.pocketstudio.model.DefaultNames.shown(t.name), com.pocketstudio.ui.trackColor(t.color))
    }
}

/**
 * The Mode simple's « Mix » tab on the project's own mixer (the console's state: one source of
 * truth, every change undoable): the meters polled while it shows, the tip worked out a moment
 * after the faders stop.
 */
@Composable
private fun SimpleMixTab(vm: RhythmViewModel) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val project by vm.project.collectAsStateWithLifecycle()
    val transport by vm.transport.collectAsStateWithLifecycle()
    val loopPart by vm.loopPart.collectAsStateWithLifecycle()
    val alone by vm.alone.collectAsStateWithLifecycle()
    // Read only by the meters when they draw (and the « Sortie »'s words when they change).
    val meters = vm.meters.collectAsStateWithLifecycle()
    val loudness = vm.loudness.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.pollMeters() } }
    val clipping by remember { androidx.compose.runtime.derivedStateOf { com.pocketstudio.app.mixer.outputWords(loudness.value, meters.value) == com.pocketstudio.app.mixer.OutputWords.CLIP } }
    val tip by androidx.compose.runtime.produceState<com.pocketstudio.model.guide.MixTip?>(null, project.mixer, project.guide.measures, clipping) {
        kotlinx.coroutines.delay(150)
        value = com.pocketstudio.model.guide.MixTips.of(vm.project.value, clipping).firstOrNull()
    }
    val tracks = simpleTracks(project)
    val ui = com.pocketstudio.app.mixer.SimpleMixUi(
        projectName = com.pocketstudio.model.DefaultNames.shown(project.name),
        tracks = tracks,
        padVolumes = tracks.flatMap { it.pads }.associate { it.lane to (project.mixer.channels.getOrNull(com.pocketstudio.model.Mixer.padChannel(it.lane))?.volume ?: com.pocketstudio.model.Mixer.DEFAULT_VOLUME) },
        masterDb = project.mixer.masterDb,
        looping = transport.playing && loopPart && alone == null,
        listening = alone,
        tip = tip,
    )
    val from = com.pocketstudio.app.mixer.SimpleMixActions.FROM
    val actions = remember(vm) {
        com.pocketstudio.app.mixer.SimpleMixActions(
            setVolume = vm::setChannelVolume, resetVolume = vm::resetChannelVolume, toggleMute = vm::toggleMute, setMasterDb = vm::setMasterDb,
            listenAlone = { key -> vm.toggleAlone(com.pocketstudio.app.rhythm.AloneTarget.ofSongKey(key), from = from) },
            listenPadAlone = { lane -> vm.toggleAlone(com.pocketstudio.app.rhythm.AloneTarget.ofKey(com.pocketstudio.model.guide.GuideTrack.pad(lane)), from = from) },
            holdAlone = vm::holdAlone,
            loop = {
                if (vm.transport.value.playing && vm.loopPart.value && vm.alone.value == null) vm.togglePlay()
                else {
                    vm.stopAlone()
                    vm.setLoopPart(true)
                    if (!vm.transport.value.playing) vm.togglePlay()
                }
            },
            resetAll = vm::resetMixLevels, applyTip = vm::applyMixTip, meterPads = vm::meterPads,
        )
    }
    com.pocketstudio.app.mixer.SimpleMixScreen(ui, { meters.value }, { loudness.value }, actions)
}

/** How long « Voir le réglage » keeps a control lit (it also goes at a touch on « Compris »). */
private const val GUIDE_SPOT_MS = 8000L
/** At most this long a plain background stands in for the project while another one opens. */
private const val SWITCH_COVER_MS = 5000L
/** « Je le fais »'s ring stays this long, then goes by itself. */
private const val TUTO_SPOT_MS = 2600L
/** The user's own music heard this long ticks a listening step about it (« Écoute toute ta batterie »). */
// Testers (lot « tuto 5 »): « ta batterie, puis l'exemple » not ticked when the example was touched within 2.5 s (a rule nobody saw).
private const val TUTO_MINE_HEARD_MS = 600L
/** The synth's « Réglages du son » card ([com.pocketstudio.app.synth.SoundCard]). */
internal const val SYNTH_SETTINGS_SPOT = "synth_settings"

/**
 * [content] with the engine's position ([RhythmViewModel.live], new about 30 times a second) read
 * in a scope of its own: the tab around it (its other states, its actions) is not recomposed at
 * each step (audit 16).
 */
@Composable
private fun WithLive(state: androidx.compose.runtime.State<com.pocketstudio.app.rhythm.LiveUi>, content: @Composable (com.pocketstudio.app.rhythm.LiveUi) -> Unit) {
    content(state.value)
}

/** The Mode simple's title of Audio page [page] (shown alone there): Ma voix, Remix, Accordeur, Générer. */
internal fun simpleAudioTitle(page: Int): String = when (page) {
    0 -> tr("Ma voix")
    2 -> tr("Accordeur")
    3 -> tr("Générer")
    else -> tr("Remix")
}

/** Lying down, the Guide's panel takes this share of the width, on the side (audit final). */
private const val GUIDE_SIDE_SHARE = 0.38f

/** The controller's overlays above the app: the knob overlay, the small legend and the flash. */
@Composable
private fun ControllerOverlays(
    midiVm: MidiViewModel,
    knobsVm: com.pocketstudio.app.midi.KnobsViewModel,
    rhythmVm: RhythmViewModel,
    controllerScreen: com.pocketstudio.app.midi.KnobScreen,
    onMidiPage: Boolean,
) {
    // Only what is shown here: the controller and its name (not each message received).
    fun shownOf(m: com.pocketstudio.app.midi.MidiUi) = m.controller to (m.controller?.name ?: m.sources.firstOrNull { it.id == m.connected }?.name)
    val shown by remember(midiVm) { midiVm.state.map(::shownOf).distinctUntilChanged() }.collectAsStateWithLifecycle(shownOf(midiVm.state.value))
    val (controller, name) = shown
    com.pocketstudio.app.midi.KnobOverlay(knobsVm, name)
    val knobSettings by knobsVm.settings.collectAsStateWithLifecycle()
    val flash by knobsVm.flash.collectAsStateWithLifecycle()
    var legendOpen by rememberSaveable { mutableStateOf(false) }
    // The small legend: while a controller is connected, outside its own settings page.
    if (controller != null && knobSettings.legend && !onMidiPage) {
        val project by rhythmVm.project.collectAsStateWithLifecycle()
        val rows = remember(controllerScreen, project, knobSettings, controller) { knobsVm.legend(controllerScreen) }
        com.pocketstudio.app.midi.ControllerLegendOverlay(
            controller.profile?.name?.removePrefix("Arturia ") ?: controller.name, controllerScreen, rows, legendOpen,
        ) { legendOpen = !legendOpen }
    }
    flash?.let { com.pocketstudio.app.midi.ControllerFlash(it) }
}

/** Where the tutorial is, for the « Mode retours » log: « chapitre 2/9 « Le rythme » · étape 3/5 (drums-kick) ». */
internal fun tutoWhere(u: com.pocketstudio.app.tuto.TutoUi): String {
    val c = u.chapters.getOrNull(u.chapter) ?: return ""
    return "chapitre ${u.chapter + 1}/${u.chapters.size} « ${c.title} » · étape ${u.step + 1}/${c.steps.size} (${u.current.id})"
}
