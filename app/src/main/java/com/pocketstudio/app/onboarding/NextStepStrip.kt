package com.pocketstudio.app.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/** The tabs where the song is made: the strip shows on each of them (not on Outils). */
val STRIP_TABS: Set<AppTab> = AppTab.entries.toSet() - AppTab.Outils

/**
 * The « Prochaine étape » strip of the open project ([projectId]) under the top bar, on [here]
 * (one of [STRIP_TABS]): hidden once closed (Outils › Aide shows it again).
 */
@Composable
fun ProjectNextStepStrip(project: Project, projectId: String, here: AppTab, share: () -> Unit, go: (AppTab) -> Unit) {
    if (here !in STRIP_TABS) return
    val context = LocalContext.current
    val prefs = remember(projectId, NextStepPrefs.changes) { NextStepPrefs(context, projectId) }
    if (prefs.closed) return
    val steps = remember(project, prefs.marks) { nextSteps(project, prefs.marks) }
    NextStepStrip(steps, here, go = go, share = share, mark = prefs::mark, close = prefs::close)
}

/**
 * The checklist from a first beat to a shared song, in one thin line: « 3/8 · Ajoute une
 * mélodie → ». A touch goes where the step is done (its tab; the last one opens « Prêt à
 * partager ? » through [share]); on that very tab ([here]) it opens the whole card instead (its
 * hint, « C'est fait »), which « Replier » folds back. The cross closes it. All done, it says so
 * until closed.
 */
@Composable
fun NextStepStrip(
    steps: List<NextStep>,
    here: AppTab,
    go: (AppTab) -> Unit = {},
    share: () -> Unit = {},
    mark: (StepGoal) -> Unit = {},
    close: () -> Unit = {},
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val next = steps.next()
    if (open && next != null) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            NextStepCard(
                steps, go = { open = false; go(it) }, mark = mark, close = close, here = here,
                share = { open = false; share() }, compact = false, shrink = { open = false },
            )
        }
        return
    }
    // Another tab, or the share sheet: the arrow says the touch goes somewhere; here, it unfolds.
    val elsewhere = next != null && (next.goal == StepGoal.Share || next.tab != here)
    val line = Neon.Cyan.copy(alpha = 0.35f)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(Neon.Cyan.copy(alpha = 0.08f))
            .drawBehind { drawLine(line, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f) }
            .then(
                if (next == null) Modifier
                else Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = if (elsewhere) tr("Y aller") else tr("Déplier"),
                ) {
                    when {
                        next.goal == StepGoal.Share -> share()
                        next.tab != here -> go(next.tab)
                        else -> open = true
                    }
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (next == null) tr("Ton morceau est fini, bravo !")
                else tr("Prochaine étape %1\$d sur %2\$d : %3\$s", next.number, steps.size, next.action)
            }
            .padding(start = 12.dp)
            .testTag("next_step_strip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .background(if (next == null) Neon.Acid.copy(alpha = 0.25f) else Neon.Cyan.copy(alpha = 0.2f))
                .border(1.dp, if (next == null) Neon.Acid else Neon.Cyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (next == null) Icon(Icons.Filled.Check, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(14.dp))
            else Text("${next.number}", style = MaterialTheme.typography.labelMedium, color = Neon.Cyan)
        }
        Text(
            if (next == null) "${steps.size}/${steps.size} · " + tr("Ton morceau est fini, bravo !")
            else "${next.number}/${steps.size} · ${next.action}",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = if (next == null) Neon.Acid else Neon.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag("next_step_strip_text"),
        )
        if (next != null) {
            Icon(
                if (elsewhere) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.ExpandMore,
                contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = close, modifier = Modifier.testTag("next_step_strip_close")) {
            Icon(Icons.Filled.Close, contentDescription = tr("Fermer les étapes"), tint = Neon.TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
