package com.pocketstudio.app.remix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.model.Remix
import com.pocketstudio.model.Scale
import com.pocketstudio.model.StemMix
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.ScaledAlertDialog

/*
 * The Remix from « Que veux-tu faire ? » (owner, 2026-10-29): a song is separated first, outside
 * any project, then it goes where the user says: a new song built on it (its tempo and key, its
 * parts playing), a new empty project that takes pieces of it, or a project already there.
 */

/** Where a song separated from the home screen goes. */
enum class RemixWay {
    /** « Faire un morceau à partir de cette chanson »: a new project at its tempo and key, the song playing along. */
    Build,

    /** « Dans un nouveau projet »: a new empty project; the song waits on its Remix page, silent, to take pieces from. */
    Source,
}

object RemixFromHome {
    /** The new project for [set] made [way]: its name, tempo, key and the song in its Remix. */
    fun projectFor(set: StemSet, way: RemixWay): Project {
        val info = set.info
        val remix = Remix(
            set.id, set.name, info.bpm, info.firstBeat, seconds = set.seconds,
            // A source: every part muted, nothing heard over what the user makes; its pieces still cut.
            stems = if (way == RemixWay.Source) List(Remix.STEMS) { StemMix(mute = true) } else List(Remix.STEMS) { StemMix() },
        )
        return when (way) {
            RemixWay.Build -> Project(
                name = set.name.trim().take(NAME_MAX).ifBlank { "Remix" },
                tempoBpm = if (info.bpm > 0) info.bpm else Project().tempoBpm,
                harmony = info.root?.let { Harmony(it, if (info.minor) Scale.Minor else Scale.Major) } ?: Project().harmony,
                // Step 4e: the song set the tempo (« Revenir au tempo de Boys ») and follows it.
                remix = remix.copy(followTempo = info.bpm > 0),
                tempoLead = info.bpm.takeIf { it > 0 }?.let { com.pocketstudio.model.TempoLead(set.name, it).sanitized() },
            )
            RemixWay.Source -> Project(name = "Remix", remix = remix)
        }.sanitized()
    }

    private const val NAME_MAX = 40

    // --- A song for a project not open yet ----------------------------------------------------

    private val waiting = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Song [setId] goes into project [projectId] once it is open (« Dans un projet existant »). */
    fun put(projectId: String, setId: String) {
        waiting[projectId] = setId
    }

    /** The song waiting for [projectId], once. */
    fun take(projectId: String): String? = waiting.remove(projectId)
}

/**
 * « Que faire de cette chanson ? »: the three ways, the recommended one first, each in one line;
 * « Dans un projet existant » opens the list of projects (the open one marked).
 */
@Composable
fun RemixDestinationDialog(
    set: StemSet,
    projects: List<ProjectInfo>,
    openId: String?,
    onWay: (RemixWay) -> Unit,
    onProject: (ProjectInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    var picking by rememberSaveable(set.id) { mutableStateOf(false) }
    val readable = projects.filter { !it.unreadable }
    ScaledAlertDialog(
        onDismissRequest = { if (picking) picking = false else onDismiss() },
        modifier = Modifier.testTag("remix_dest"),
        title = { Text(if (picking) tr("Dans quel projet ?") else tr("Que faire de « %s » ?", set.name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!picking) {
                    val what = listOfNotNull(set.info.bpm.takeIf { it > 0 }?.let { "%.0f BPM".format(it) }, set.info.keyLabel).joinToString(", ")
                    Way(
                        Icons.Filled.AutoAwesome, Neon.Acid, tr("Faire un morceau à partir de cette chanson"),
                        if (what.isEmpty()) tr("Un nouveau projet avec la chanson et ses pistes prêtes.")
                        else tr("Un nouveau projet à son tempo et dans sa tonalité (%s), avec ses pistes prêtes.", what),
                        "remix_dest_build",
                    ) { onWay(RemixWay.Build) }
                    Way(
                        Icons.Filled.AddCircle, Neon.Cyan, tr("Dans un nouveau projet"),
                        tr("Un projet vide ; la chanson t'attend dans le Remix pour en prendre des morceaux."),
                        "remix_dest_new",
                    ) { onWay(RemixWay.Source) }
                    Way(
                        Icons.Filled.FolderOpen, Neon.Magenta, tr("Dans un projet existant"),
                        if (readable.isEmpty()) tr("Aucun projet pour l'instant.") else tr("Choisis lequel : la chanson arrive dans son Remix."),
                        "remix_dest_existing", enabled = readable.isNotEmpty(),
                    ) { picking = true }
                } else {
                    readable.forEach { p ->
                        Way(
                            Icons.Filled.FolderOpen, Neon.Magenta, com.pocketstudio.model.DefaultNames.shown(p.name),
                            listOfNotNull(
                                tr("projet ouvert").takeIf { p.id == openId },
                                "%.0f BPM".format(p.tempoBpm),
                                tr("a déjà une chanson : elle sera remplacée").takeIf { p.hasRemix },
                            ).joinToString(" · "),
                            "remix_dest_project_${p.id}",
                        ) { onProject(p) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { if (picking) picking = false else onDismiss() }, modifier = Modifier.testTag("remix_dest_back")) {
                if (picking) Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(if (picking) " " + tr("Retour") else tr("Annuler"))
            }
        },
    )
}

/** One way to go: a big touch target, its icon in its colour, a title and a line. */
@Composable
private fun Way(icon: ImageVector, accent: Color, title: String, line: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    NeonCard(
        accent = accent,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick).testTag(tag),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(icon, contentDescription = null, tint = if (enabled) accent else Neon.TextSecondary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (enabled) Neon.TextPrimary else Neon.TextSecondary)
                if (line.isNotEmpty()) Text(line, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/**
 * The Remix page of « Que veux-tu faire ? »: « ← Accueil », separate a song (or pick one already
 * separated), listen to its parts, then « Utiliser cette chanson » asks where it goes. A song just
 * separated here asks at once.
 */
@Composable
fun RemixHomePage(
    vm: RemixViewModel,
    projects: List<ProjectInfo>,
    openId: String?,
    close: () -> Unit,
    pickSong: () -> Unit,
    downloadModel: () -> Unit,
    onWay: (StemSet, RemixWay) -> Unit,
    onProject: (StemSet, ProjectInfo) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val sets by vm.sets.collectAsStateWithLifecycle()
    val modelInstalled by vm.modelInstalled.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val memory by vm.memoryProblem.collectAsStateWithLifecycle()
    val memoryNever by vm.memoryNever.collectAsStateWithLifecycle()
    var asking by rememberSaveable { mutableStateOf<String?>(null) }
    var startedHere by rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refreshSets() }
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.pollPreview() }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { vm.stop() } }
    // A song separated from this page: where it goes is asked at once (never a dead end).
    androidx.compose.runtime.LaunchedEffect(state) {
        val done = state as? SeparationState.Done ?: return@LaunchedEffect
        if (startedHere) {
            startedHere = false
            asking = done.set.id
        }
    }
    androidx.activity.compose.BackHandler(onBack = close)
    Column(Modifier.fillMaxSize().testTag("remix_home")) {
        Row(
            Modifier.fillMaxWidth().background(Neon.Surface).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp).testTag("remix_home_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + tr("Retour"))
            }
            Text(tr("Remix"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.Cyan)
        }
        RemixScreen(
            RemixUi(
                modelInstalled = modelInstalled, state = state, sets = sets, preview = preview,
                memoryProblem = memory, memoryNever = memoryNever, home = true,
            ),
            RemixActions(
                downloadModel = downloadModel,
                checkMemory = vm::checkMemory,
                pickSong = { startedHere = true; pickSong() },
                cancel = vm::cancel,
                dismiss = vm::dismiss,
                retry = { startedHere = true; vm.retry() },
                playAll = vm::playAll,
                toggleStem = vm::toggleStem,
                pause = vm::pause,
                delete = vm::delete,
                useSong = { set -> vm.pause(); asking = set.id },
            ),
        )
    }
    val set = asking?.let { id -> sets.firstOrNull { it.id == id } }
    if (set != null) {
        RemixDestinationDialog(
            set, projects, openId,
            onWay = { way -> asking = null; vm.stop(); onWay(set, way) },
            onProject = { p -> asking = null; vm.stop(); onProject(set, p) },
            onDismiss = { asking = null },
        )
    }
}
