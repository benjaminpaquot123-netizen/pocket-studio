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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.library.QuickUi
import com.pocketstudio.app.library.familyLook
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

/**
 * « Nouveau projet »: the guided song first and standing out, then the assistant's styles (the same
 * list, families and order; every family folded at first, a colour and an icon each) made whole in
 * one touch or opened step by step, then the other starters: the starting beat, the demos (a play
 * button each, to hear them without opening a project) and the empty project under their headings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewProjectPage(starters: List<ProjectStarter>, quick: QuickUi, actions: ProjectsActions, close: () -> Unit) {
    // While a song is being prepared, every other choice waits.
    val busy = quick.creating != null
    // Leaving the page ends a demo still playing.
    DisposableEffect(Unit) { onDispose { actions.stopDemo() } }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp).testTag("new_project"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = close, enabled = !busy, modifier = Modifier.testTag("new_project_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
            }
            Text(tr("Nouveau projet"), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("new_project_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GuidedCard(busy) { close(); actions.wizard() }
            SectionLabel(StarterGroup.Style.title, Modifier.padding(top = 12.dp))
            Text(
                tr("Une touche, un morceau complet (environ 3 minutes), prêt à écouter."),
                style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
            )
            quick.error?.let {
                Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = actions.dismissQuickError).testTag("quick_error"))
            }
            // The assistant's families, all folded at first (but the one of a song being prepared): one touch on a style makes its song.
            com.pocketstudio.app.library.StyleFamilies("quick", style = { QuickStyleCard(it, quick, actions) },
                open = setOfNotNull(StarterWizard.STYLES.find { it.id == quick.creating }?.family),
                calm = com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode)
            // The other starters under their headings (the starting beat closes the styles' group).
            StarterGroup.entries.forEach { group ->
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
                        StarterRow(s, if (group == StarterGroup.Empty) Icons.Filled.Add else Icons.Filled.GraphicEq, !busy) { close(); actions.create(i) }
                    }
                }
            }
        }
    }
}

/** The guided song: the page's main action, filled with the accent and bigger than the rest. */
@Composable
private fun GuidedCard(busy: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val accent = Neon.Cyan
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.34f), accent.copy(alpha = 0.10f))))
            .border(1.5.dp, accent.copy(alpha = 0.8f), shape)
            .clickable(enabled = !busy, onClick = onClick)
            .padding(16.dp).testTag("starter_wizard"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Neon.OnAccent, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(tr("Nouveau morceau guidé"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Neon.TextPrimary)
            Text(tr("Idéal pour débuter : je t'explique chaque son."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary.copy(alpha = 0.8f))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accent)
    }
}

/** A simple starter (the starting beat, the empty project): an icon, its name, a line, a chevron. */
@Composable
private fun StarterRow(s: ProjectStarter, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).testTag("starter_${s.tag}"),
    ) {
        Row(Modifier.padding(12.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Neon.Cyan.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(tr(s.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(tr(s.description), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
private fun QuickStyleCard(s: StarterWizard.Style, quick: QuickUi, actions: ProjectsActions) {
    val preparing = quick.creating == s.id
    val busy = quick.creating != null
    val color = familyLook(s.family).second
    Card(
        colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh),
        border = if (preparing) BorderStroke(1.5.dp, Neon.Cyan) else null,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { actions.quickStyle(s.id) }.testTag("quick_style_${s.id}"),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(color))
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)) {
                Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(s.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (preparing) Neon.Cyan else Neon.TextPrimary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("${s.bpm.roundToInt()} BPM", style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1,
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
                    TextButton(onClick = { actions.customStyle(s.id) }, enabled = !busy, modifier = Modifier.testTag("quick_custom_${s.id}")) {
                        val quiet = if (busy) Neon.TextSecondary.copy(alpha = 0.5f) else Neon.TextPrimary.copy(alpha = 0.8f)
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = quiet)
                        Text(" " + tr("Pas à pas"), color = quiet, maxLines = 1)
                    }
                }
            }
        }
    }
}
