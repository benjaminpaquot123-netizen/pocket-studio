package com.pocketstudio.app.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.library.QuickUi
import com.pocketstudio.app.library.familyLook
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.app.ui.showcasePanel
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

/**
 * « Nouveau projet »: three big choices, one sentence each (Benjamin, 2026-10-27): « J'apprends »
 * (the tutorial, Facile or Pro, then the style), « Fais-moi un morceau » (the assistant, then the
 * Guide), « Je pars de zéro » (an empty project). Everything else (a style's song in one touch,
 * the starting beat, the demos, the Remix, an import) waits folded under « Autres façons de
 * commencer » (open while a song is being prepared).
 *
 * Owner (2026-10-29): no new project without « Simple ou Pro ? ». Each way to start goes through
 * [com.pocketstudio.app.ui.ModeStep] first (Retour comes back here); for « J'apprends » the mode
 * is also the tutorial's level (Simple: Facile), asked once. The demos only play: no step.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewProjectPage(
    starters: List<ProjectStarter>, quick: QuickUi, actions: ProjectsActions, close: () -> Unit,
    /** Before a starter or the assistant: the page closes (back on the list), or stays under it when it is the whole screen. */
    leave: () -> Unit = close,
) {
    // While a song is being prepared, every other choice waits.
    val busy = quick.creating != null
    // Leaving the page ends a demo still playing.
    DisposableEffect(Unit) { onDispose { actions.stopDemo() } }
    val context = androidx.compose.ui.platform.LocalContext.current
    // The way to start touched, waiting for « Simple ou Pro ? » (kept through a rotation): see [startOf].
    var pending by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var learning by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    // Before the steps below: « Autres façons de commencer » stays open on the way back from them.
    var others by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (learning) {
        // Retour: back on « Simple ou Pro ? », one step at a time.
        androidx.activity.compose.BackHandler { learning = false }
        val simple = com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode
        com.pocketstudio.app.tuto.TutoChooser(
            start = { style, level -> learning = false; pending = null; leave(); actions.learn(style, level) },
            close = { learning = false },
            simple = simple,
            fixedLevel = if (simple) com.pocketstudio.model.tuto.TutoLevel.Facile else com.pocketstudio.model.tuto.TutoLevel.Pro,
        )
        return
    }
    // The step lies over the page, which stays as it was under it (its families open or folded).
    Box(Modifier.fillMaxSize()) {
    val asking = pending
    Column(Modifier.fillMaxSize().then(if (asking != null) Modifier.clearAndSetSemantics {} else Modifier).padding(horizontal = 12.dp, vertical = 8.dp).testTag("new_project"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = close, enabled = !busy, modifier = Modifier.testTag("new_project_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
            }
            Text(tr("Nouveau projet"), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("new_project_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BigChoice(Icons.Filled.School, tr("J'apprends"), tr("Tu fais tout le morceau toi-même, étape par étape : je t'explique et je vérifie."),
                Neon.Acid, !busy, "start_learn") { pending = START_LEARN }
            BigChoice(Icons.Filled.Bolt, tr("Fais-moi un morceau"), tr("Choisis un style : l'assistant fait le morceau, le Guide t'aide à le finir."),
                Neon.Cyan, !busy, "starter_wizard") { pending = START_WIZARD }
            BigChoice(Icons.Filled.Edit, tr("Je pars de zéro"), tr("Un projet vide : à toi de jouer."),
                Neon.Amber, !busy, "start_empty") {
                starters.indexOfFirst { it.group == StarterGroup.Empty }.takeIf { it >= 0 }?.let { pending = START_STARTER + it }
            }
            val open = others || busy || quick.error != null
            TextButton(onClick = { others = !others }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("start_others")) {
                Text((if (open) "▾ " else "▸ ") + tr("Autres façons de commencer"), color = Neon.TextSecondary, style = MaterialTheme.typography.titleSmall)
            }
            if (open) OtherStarts(starters, quick, actions, busy, leave) { pending = it }
        }
    }
    if (asking != null) {
        androidx.activity.compose.BackHandler { pending = null }
        androidx.compose.material3.Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
            com.pocketstudio.app.ui.ModeStep(
                choose = { simple ->
                    com.pocketstudio.app.settings.ProjectModes.choose(context, simple)
                    if (asking == START_LEARN) learning = true
                    else {
                        pending = null
                        startOf(asking, starters, actions, leave)
                    }
                },
                back = { pending = null },
                tuto = asking == START_LEARN,
            )
        }
    }
    }
}

private const val START_LEARN = "learn"
private const val START_WIZARD = "wizard"
private const val START_STARTER = "starter:"
private const val START_QUICK = "quick:"
private const val START_CUSTOM = "custom:"

/** Runs the way to start [key] stands for, once its mode is chosen. */
private fun startOf(key: String, starters: List<ProjectStarter>, actions: ProjectsActions, leave: () -> Unit) {
    when {
        key == START_WIZARD -> { leave(); actions.wizard() }
        key.startsWith(START_STARTER) -> key.removePrefix(START_STARTER).toIntOrNull()?.takeIf { it in starters.indices }?.let { leave(); actions.create(it) }
        key.startsWith(START_QUICK) -> actions.quickStyle(key.removePrefix(START_QUICK))
        key.startsWith(START_CUSTOM) -> actions.customStyle(key.removePrefix(START_CUSTOM))
    }
}

/** A big choice of the page: a round icon, a title and one sentence. */
@Composable
private fun BigChoice(icon: ImageVector, title: String, line: String, accent: androidx.compose.ui.graphics.Color, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 96.dp)
            .showcasePanel(shape, accent = accent.copy(alpha = 0.8f), edge = 1.5.dp, wash = 0.17f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.pocketstudio.app.ui.IconMedallion(icon, accent, 52.dp, 30.dp, filled = true)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Neon.TextPrimary)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary.copy(alpha = 0.8f))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accent)
    }
}

/**
 * « Autres façons de commencer »: the assistant's styles (the same list, families and order;
 * every family folded at first) made whole in one touch or opened step by step, the starting
 * beat, the demos (a play button each), the Remix and an import.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OtherStarts(starters: List<ProjectStarter>, quick: QuickUi, actions: ProjectsActions, busy: Boolean, leave: () -> Unit, ask: (String) -> Unit) {
    SectionLabel(StarterGroup.Style.title, Modifier.padding(top = 4.dp))
    Text(
        // Lot 2: the real lengths (a Techno lasts 5 to 6 minutes), each style says its own.
        com.pocketstudio.app.library.StyleLengths.range().let { (from, to) -> tr("Une touche, un morceau complet de %1\$d à %2\$d minutes selon le style, prêt à écouter.", from, to) },
        style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
    )
    quick.error?.let {
        Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().clickable(onClick = actions.dismissQuickError).testTag("quick_error"))
    }
    // The assistant's families, all folded at first (but the one of a song being prepared): one touch on a style makes its song.
    com.pocketstudio.app.library.StyleFamilies("quick", style = { QuickStyleCard(it, quick, ask) },
        open = setOfNotNull(StarterWizard.STYLES.find { it.id == quick.creating }?.family),
        calm = com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode)
    // The other starters under their headings (the starting beat closes the styles' group; the empty project is above).
    StarterGroup.entries.filter { it != StarterGroup.Empty }.forEach { group ->
        val inGroup = starters.withIndex().filter { it.value.group == group }
        if (inGroup.isEmpty()) return@forEach
        if (group != StarterGroup.Style) SectionLabel(group.title, Modifier.padding(top = 12.dp))
        var demo = 0
        inGroup.forEach { (i, s) ->
            if (group == StarterGroup.Demo) {
                // The demos keep the order of Demos.ALL: their position in the group is their index there.
                DemoRow(s, demo, quick, actions)
                demo++
            } else {
                StarterRow(s, Icons.Filled.GraphicEq, !busy) { ask(START_STARTER + i) }
            }
        }
    }
    actions.remix?.let { remix ->
        StarterRow(tr("Extraire la voix d'une chanson"), tr("L'appli sépare une chanson en batterie, basse, voix et le reste : rejoue-la à ta façon."), "remix",
            // The Remix opens on the project already open: no new project, no mode to choose.
            Icons.Filled.Album, !busy) { leave(); remix() }
    }
    StarterRow(tr("Importer un projet (.zip)"), tr("Un projet envoyé depuis un autre téléphone."), "import", Icons.Filled.FolderOpen, !busy) { actions.importZip() }
}

/** A simple starter (the starting beat, the empty project): an icon, its name, a line, a chevron. */
@Composable
private fun StarterRow(s: ProjectStarter, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) =
    StarterRow(tr(s.title), tr(s.description), s.tag, icon, enabled, onClick)

@Composable
private fun StarterRow(title: String, description: String, tag: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).testTag("starter_$tag"),
    ) {
        Row(Modifier.padding(12.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Neon.Cyan.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Neon.TextSecondary)
        }
    }
}

/**
 * A demo: a play button on its left that only plays it (no project is created or opened), its
 * name and a line. ▶ plays, ■ stops, a wheel while it is prepared; green is for what plays.
 */
@Composable
private fun DemoRow(s: ProjectStarter, index: Int, quick: QuickUi, actions: ProjectsActions) {
    val playing = quick.demoPlaying == index
    val preparing = quick.demoPreparing == index
    val ink = if (playing) Neon.Acid else Neon.Cyan
    Card(
        colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh),
        border = if (playing) BorderStroke(1.5.dp, Neon.Acid) else null,
        modifier = Modifier.fillMaxWidth().testTag("starter_${s.tag}"),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { actions.previewDemo(index) },
                modifier = Modifier.size(52.dp).clip(CircleShape).background(ink.copy(alpha = if (playing) 0.28f else 0.16f)).testTag("demo_play_$index"),
            ) {
                when {
                    preparing -> CircularProgressIndicator(
                        Modifier.size(22.dp).semantics { contentDescription = tr("Préparation de l'écoute") }, strokeWidth = 2.dp, color = ink,
                    )
                    playing -> Icon(Icons.Filled.Stop, contentDescription = tr("Arrêter l'écoute"), tint = ink)
                    else -> Icon(Icons.Filled.PlayArrow, contentDescription = tr("Écouter"), tint = ink, modifier = Modifier.size(30.dp))
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                Text(tr(s.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(tr(s.description), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * One style: a touch makes its whole song with the advised choices; « Pas à pas » opens the
 * assistant on it. The family's colour runs down its left edge. While its song is prepared, a
 * wheel says so in place of its description.
 */
@Composable
private fun QuickStyleCard(s: StarterWizard.Style, quick: QuickUi, ask: (String) -> Unit) {
    val preparing = quick.creating == s.id
    val busy = quick.creating != null
    val color = familyLook(s.family).second
    Card(
        colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh),
        border = if (preparing) BorderStroke(1.5.dp, Neon.Cyan) else null,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { ask(START_QUICK + s.id) }.testTag("quick_style_${s.id}"),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(color))
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)) {
                Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(s.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (preparing) Neon.Cyan else Neon.TextPrimary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("${s.bpm.roundToInt()} BPM · ${com.pocketstudio.app.library.StyleLengths.label(s)}", style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp))
                }
                if (preparing) {
                    Row(Modifier.padding(vertical = 4.dp).testTag("quick_preparing"), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Neon.Cyan)
                        Text("  " + tr("Préparation du morceau…"), style = MaterialTheme.typography.bodyMedium, color = Neon.Cyan)
                    }
                } else {
                    Text(tr(s.description), style = MaterialTheme.typography.bodySmall, color = Neon.TextPrimary.copy(alpha = 0.72f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 8.dp))
                }
                // The touch on the card makes the song; the assistant is the second, quieter way.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val ink = if (busy) Neon.TextSecondary.copy(alpha = 0.5f) else Neon.Cyan
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
                    Text(" " + tr("Créer le morceau"), style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TextButton(onClick = { ask(START_CUSTOM + s.id) }, enabled = !busy, modifier = Modifier.testTag("quick_custom_${s.id}")) {
                        val quiet = if (busy) Neon.TextSecondary.copy(alpha = 0.5f) else Neon.TextPrimary.copy(alpha = 0.8f)
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = quiet)
                        Text(" " + tr("Pas à pas"), color = quiet, maxLines = 1)
                    }
                }
            }
        }
    }
}
