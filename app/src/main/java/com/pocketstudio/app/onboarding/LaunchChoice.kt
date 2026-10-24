package com.pocketstudio.app.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketstudio.app.export.shareSeconds
import com.pocketstudio.app.projects.whenText
import com.pocketstudio.app.ui.barsShort
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * When « Que veux-tu faire ? » opens again once a choice was made (docs/DECISIONS.md, 2026-10-24):
 * a new start of the app always asks (no saved screen state); a rotation, another project opened
 * or a short trip to another app never do; back after [AWAY_MS] or more (the app left in the
 * background, or its process killed and its screens restored), it asks again, except while music
 * plays or a take runs in the background (the user comes back to it).
 */
object LaunchRule {
    const val AWAY_MS = 30 * 60_000L

    /** Tests of other screens: the app opens straight on the project, as before this screen (AppHarness). */
    @androidx.annotation.VisibleForTesting
    @Volatile
    var askAtStartForTest = true

    /** Back in the app at [now] after leaving it at [leftAt] (0: never left). */
    fun askAgain(leftAt: Long, now: Long, busy: Boolean): Boolean = !busy && leftAt > 0 && now - leftAt >= AWAY_MS
}

/** When the app's screen was last left, kept across process deaths. */
class LaunchPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var leftAt: Long
        get() = prefs.getLong(LEFT_AT, 0L)
        set(value) = prefs.edit().putLong(LEFT_AT, value).apply()

    companion object {
        const val PREFS = "launch"
        private const val LEFT_AT = "left_at"
    }
}

/**
 * The chooser's state, kept in the screen's saved state: a rotation, the rebuild that opening a
 * project does and a process restored keep it; a new start of the app (nothing saved) opens it.
 */
@Stable
class LaunchGate(open: Boolean, back: Boolean = false) {
    /** « Que veux-tu faire ? » is on screen. */
    var open by mutableStateOf(open)
        private set

    /** A screen opened from it (Mes projets, Nouveau morceau) shows: its Retour comes back here. */
    var back by mutableStateOf(back)
        private set

    /** A choice made: the chooser closes; [comeBack] when the screen it opens returns to it. */
    fun choose(comeBack: Boolean = false) {
        open = false
        back = comeBack
    }

    /** The screen opened from it closed with Retour: the chooser again (true) when it came from it. */
    fun returned(): Boolean {
        if (!back) return false
        back = false
        open = true
        return true
    }

    /** Asked again (back after a long time away). */
    fun ask() {
        open = true
        back = false
    }

    /** Something done in the project itself (another project opened, the song shared): no way back here. */
    fun settled() {
        back = false
    }

    companion object {
        val Saver = listSaver<LaunchGate, Boolean>(save = { listOf(it.open, it.back) }, restore = { LaunchGate(it[0], it[1]) })
    }
}

/** The chooser of this screen; closed at a first launch (the welcome is its choice). */
@Composable
fun rememberLaunchGate(firstLaunch: Boolean): LaunchGate =
    rememberSaveable(saver = LaunchGate.Saver, key = "launch_gate") { LaunchGate(open = !firstLaunch && LaunchRule.askAtStartForTest) }

/** What « Continuer » shows of the last project: its name, style, length and last edit. */
data class LaunchUi(
    val name: String,
    val style: String? = null,
    val length: String? = null,
    val edited: String? = null,
    val simple: Boolean = false,
    val missionsActive: Boolean = false,
)

/** [p] as « Continuer » shows it; [info]: its entry in the projects' list (its last save), when read. */
fun launchUi(p: Project, info: ProjectInfo?, now: Long, simple: Boolean, missionsActive: Boolean): LaunchUi {
    val style = (p.style ?: com.pocketstudio.app.library.styleOfProject(p.name))?.let(StarterWizard::style)
    val length = when {
        p.song.isNotEmpty() -> clock(shareSeconds(p, 1)) + " · " + if (p.song.size == 1) tr("1 partie") else tr("%d parties", p.song.size)
        info != null && info.bars > 0 -> tr("Boucle de %s", barsShort(info.bars))
        else -> null
    }
    return LaunchUi(
        name = DefaultNames.shown(p.name),
        style = style?.let { tr(it.name) },
        length = length,
        edited = info?.takeIf { it.modified > 0 }?.let { tr("modifié %s", whenText(it.modified, now)) },
        simple = simple,
        missionsActive = missionsActive,
    )
}

private fun clock(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

/**
 * « Que veux-tu faire ? » wired to the app: shown while [show], on the open project ([vm]) and its
 * last save (the projects' list); remembers when the app is left and asks again at [gate] when
 * it comes back after a long time away ([LaunchRule]).
 */
@Composable
internal fun LaunchChooser(
    gate: LaunchGate, vm: com.pocketstudio.app.rhythm.RhythmViewModel, projectsVm: com.pocketstudio.app.projects.ProjectsViewModel,
    missions: com.pocketstudio.app.learn.MissionPrefs, show: Boolean, simple: Boolean, actions: LaunchActions,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = androidx.compose.runtime.remember { LaunchPrefs(context) }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) { prefs.leftAt = System.currentTimeMillis() }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_START) {
        val busy = vm.transport.value.playing || vm.recording.value || vm.audioRecording.value != null
        if (com.pocketstudio.app.settings.SettingsStore.state.value.guideSeen && LaunchRule.askAgain(prefs.leftAt, System.currentTimeMillis(), busy)) gate.ask()
    }
    if (!show) return
    val project by vm.project.collectAsStateWithLifecycle()
    val list by projectsVm.state.collectAsStateWithLifecycle()
    // Its last save, for « modifié il y a 2 h ».
    androidx.compose.runtime.LaunchedEffect(Unit) { projectsVm.refresh() }
    val now = androidx.compose.runtime.remember { System.currentTimeMillis() }
    LaunchChoice(true, launchUi(project, list.projects.firstOrNull { it.id == vm.projectId }, now, simple, missions.active), actions)
}

/** The choices; nothing in the project changes nor plays until one is made. */
class LaunchActions(
    /** « Continuer »: the last project, as it was left (also the system's Retour). */
    val resume: () -> Unit = {},
    /** « Nouveau morceau »: a style in one touch, the assistant, a demo or an empty project. */
    val newSong: () -> Unit = {},
    val projects: () -> Unit = {},
    val missions: () -> Unit = {},
    /** The Mode simple (true) or every tool; the chooser stays. */
    val chooseMode: (Boolean) -> Unit = {},
)

/** « Que veux-tu faire ? » over the whole screen while [show]. */
@Composable
fun LaunchChoice(show: Boolean, ui: LaunchUi, actions: LaunchActions) {
    if (!show) return
    Dialog(
        onDismissRequest = actions.resume,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) { LaunchScreen(ui, actions) }
    }
}

/**
 * The screen: « Continuer » first and biggest (one touch), then « Nouveau morceau » and « Mes
 * projets », then the missions and the mode, smaller. Side by side when the screen is wide and low
 * (a phone or a tablet lying down), one under the other otherwise.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LaunchScreen(ui: LaunchUi, actions: LaunchActions) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val wide = maxWidth >= 680.dp && maxWidth > maxHeight
        val screenHeight = maxHeight
        com.pocketstudio.ui.ReadingWidth(maxWidth = if (wide) 960.dp else 600.dp) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    // Lying down, in the middle of the screen when it has room (a tablet); on top otherwise.
                    .heightIn(min = if (wide) screenHeight else 0.dp)
                    .padding(horizontal = 20.dp, vertical = if (wide) 16.dp else 28.dp).testTag("launch"),
                verticalArrangement = Arrangement.spacedBy(if (wide) 12.dp else 16.dp, if (wide) Alignment.CenterVertically else Alignment.Top),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Pocket Studio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Neon.Cyan)
                    Text(tr("Que veux-tu faire ?"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                if (wide) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ContinueCard(ui, actions.resume, Modifier.weight(1.15f).fillMaxHeight())
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) { OtherChoices(actions) }
                    }
                } else {
                    ContinueCard(ui, actions.resume, Modifier.fillMaxWidth())
                    OtherChoices(actions)
                }
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(onClick = actions.missions, modifier = Modifier.heightIn(min = 48.dp).align(Alignment.CenterVertically).testTag("launch_missions")) {
                        Icon(Icons.Filled.Flag, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.padding(end = 6.dp).size(20.dp))
                        Text(if (ui.missionsActive) tr("Reprendre les missions") else tr("Missions : Mon premier morceau"), color = Neon.TextPrimary)
                    }
                    ModeSwitch(ui.simple, actions.chooseMode, Modifier.align(Alignment.CenterVertically))
                }
            }
        }
    }
}

/** « Continuer »: the last project's name, its style, its length and when it was last changed. */
@Composable
private fun ContinueCard(ui: LaunchUi, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val color = Neon.Cyan
    Row(
        modifier.heightIn(min = 120.dp).clip(shape)
            .background(color.copy(alpha = 0.2f))
            .border(2.dp, color, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp)
            .testTag("launch_continue"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tr("Continuer"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(
                ui.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Neon.TextPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("launch_name"),
            )
            listOfNotNull(ui.style, ui.length).takeIf { it.isNotEmpty() }?.let {
                Text(it.joinToString(" · "), style = MaterialTheme.typography.bodyLarge, color = Neon.TextPrimary.copy(alpha = 0.85f), modifier = Modifier.testTag("launch_details"))
            }
            ui.edited?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("launch_edited")) }
        }
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(color), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Neon.OnAccent, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun OtherChoices(actions: LaunchActions) {
    Choice(
        Icons.Filled.AutoAwesome, tr("Nouveau morceau"), tr("Un style en une touche, ou pas à pas avec l'assistant."),
        Neon.Cyan, actions.newSong, "launch_new",
    )
    Choice(
        Icons.Filled.FolderOpen, tr("Mes projets"), tr("Ouvrir un autre morceau, le copier ou le renommer."),
        Color(0xFF5BB8F5), actions.projects, "launch_projects",  // sky blue: green means "playing"
    )
}

/** A choice under « Continuer »: a round icon, a title and one line. */
@Composable
private fun Choice(icon: ImageVector, title: String, line: String, color: Color, onClick: () -> Unit, tag: String) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 80.dp).clip(shape)
            .background(Neon.Surface)
            .border(1.dp, Neon.Outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Neon.TextPrimary)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        }
    }
}

/** « Mode : Simple | Complet », small: one touch changes it, nothing in the project changes. */
@Composable
private fun ModeSwitch(simple: Boolean, choose: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.testTag("launch_mode"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(tr("Mode"), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        ModePill(tr("Simple"), simple, "launch_mode_simple") { choose(true) }
        ModePill(tr("Complet"), !simple, "launch_mode_full") { choose(false) }
    }
}

@Composable
private fun ModePill(label: String, chosen: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.heightIn(min = 48.dp).clip(shape)
            .selectable(selected = chosen, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.clip(shape)
                .background(if (chosen) Neon.Cyan.copy(alpha = 0.22f) else Neon.Surface)
                .border(if (chosen) 1.5.dp else 1.dp, if (chosen) Neon.Cyan else Neon.Outline, shape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                color = if (chosen) Neon.Cyan else Neon.TextPrimary)
        }
    }
}
