package com.pocketstudio.app.onboarding

import com.pocketstudio.model.i18n.tr
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.Project
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

/**
 * What the "Prochaine étape" card remembers, per project (SharedPreferences "next_steps"): closed
 * for good, and the steps ticked by hand.
 */
class NextStepPrefs(context: Context, private val projectId: String) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    var closed by mutableStateOf(prefs.getBoolean("$projectId.closed", false))
        private set
    var marks by mutableStateOf(
        prefs.getStringSet("$projectId.marks", emptySet()).orEmpty().mapNotNull { n -> StepGoal.entries.firstOrNull { it.name == n } }.toSet(),
    )
        private set

    fun close() {
        closed = true
        prefs.edit().putBoolean("$projectId.closed", true).apply()
    }

    fun mark(goal: StepGoal) {
        marks = marks + goal
        prefs.edit().putStringSet("$projectId.marks", marks.map { it.name }.toSet()).apply()
        changes++
    }

    /** Shows the card again after its cross closed it (Outils › Aide). */
    fun reopen() {
        closed = false
        prefs.edit().putBoolean("$projectId.closed", false).apply()
        changes++
    }

    companion object {
        const val PREFS = "next_steps"

        /** Bumped by every change: a card already shown reads its project's steps again. */
        var changes by androidx.compose.runtime.mutableIntStateOf(0)
            private set
    }
}

/**
 * The "Prochaine étape" card of the open project ([projectId]), at the top of the Rythme tab:
 * shown until every step is done or the user closes it. [go] opens a tab.
 */
@Composable
fun ProjectNextSteps(project: Project, projectId: String, share: () -> Unit = {}, showHere: (() -> Unit)? = null, go: (AppTab) -> Unit) {
    val context = LocalContext.current
    val prefs = remember(projectId, NextStepPrefs.changes) { NextStepPrefs(context, projectId) }
    if (prefs.closed) return
    val steps = nextSteps(project, prefs.marks)
    if (steps.all { it.done }) return
    NextStepCard(steps, go = go, mark = prefs::mark, close = prefs::close, share = share, showHere = showHere)
}

/**
 * The checklist from a first beat to a shared song: seven numbered steps (ticked when done), and
 * the one chosen (the first left to do at first) with its hint and a button to the right tab.
 * [here]: the tab the card is on (no button to it, but [showHere] when given: "Voir la grille").
 * [share] exports and opens the share sheet; the step is ticked once the sheet opens (see
 * [rememberShareSong][com.pocketstudio.app.export.rememberShareSong]), not when touched.
 * Folds by its title; the cross closes it (Outils › Aide shows it again).
 */
@Composable
fun NextStepCard(
    steps: List<NextStep>,
    go: (AppTab) -> Unit = {},
    mark: (StepGoal) -> Unit = {},
    close: () -> Unit = {},
    here: AppTab = AppTab.Rythme,
    share: (() -> Unit)? = null,
    showHere: (() -> Unit)? = null,
    /** One line that opens on a touch (a phone: the grid stays on screen under it); null: a phone's width decides. */
    compact: Boolean? = null,
) {
    val next = steps.next() ?: return
    var chosenGoal by rememberSaveable { mutableStateOf<StepGoal?>(null) }
    // A step chosen and then done gives way to the next one left.
    val shown = steps.firstOrNull { it.goal == chosenGoal && !it.done } ?: next
    val small = compact ?: (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < com.pocketstudio.ui.TWO_PANES_FROM.value)
    var opened by rememberSaveable { mutableStateOf(false) }
    if (small && !opened) {
        CompactNextStep(steps, shown) { opened = true }
        return
    }
    NeonCard(accent = Neon.Cyan, modifier = Modifier.testTag("next_steps")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(
                tr("Prochaine étape · %d/%d", steps.count { it.done }, steps.size),
                Modifier.weight(1f), foldKey = "next_steps",
            )
            if (small) {
                IconButton(onClick = { opened = false }, modifier = Modifier.size(40.dp).testTag("next_steps_shrink")) {
                    Icon(Icons.Filled.ExpandLess, contentDescription = tr("Replier"), tint = Neon.TextSecondary)
                }
            }
            IconButton(onClick = close, modifier = Modifier.size(40.dp).testTag("next_steps_close")) {
                Icon(Icons.Filled.Close, contentDescription = tr("Fermer les étapes"), tint = Neon.TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
        // The seven steps in a row: a tick when done, the one shown lit.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            steps.forEach { s -> StepDot(s, s.goal == shown.goal, Modifier.weight(1f)) { chosenGoal = s.goal } }
        }
        Text("${shown.number}. ${shown.goal.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("next_step_title"))
        Text(shown.hint, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shown.goal == StepGoal.Share && share != null) {
                // Sharing is one tap from here: export the song and open the share sheet.
                Button(onClick = share, modifier = Modifier.heightIn(min = 48.dp).testTag("next_step_go")) { Text(tr("Partager mon morceau")) }
            } else if (shown.tab != here) {
                Button(onClick = { go(shown.tab) }, modifier = Modifier.heightIn(min = 48.dp).testTag("next_step_go")) { Text(shown.button) }
            } else if (showHere != null) {
                // The steps can be below the screen's edge on a phone: one touch brings them up.
                Button(onClick = showHere, modifier = Modifier.heightIn(min = 48.dp).testTag("next_step_here")) { Text(tr("Voir la grille")) }
            }
            if (shown.manual && !(shown.goal == StepGoal.Share && share != null)) {
                OutlinedButton(onClick = { mark(shown.goal) }, modifier = Modifier.heightIn(min = 48.dp).testTag("next_step_done")) { Text(tr("C'est fait")) }
            }
        }
    }
}

/**
 * The card on a phone: one line (« Prochaine étape · 5/7 », the step's number and name) that opens
 * the whole card on a touch, so the beat's grid stays on screen right under it.
 */
@Composable
private fun CompactNextStep(steps: List<NextStep>, shown: NextStep, open: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).background(Neon.Surface)
            .border(1.5.dp, Neon.Cyan.copy(alpha = 0.8f), shape)
            .clickable(role = Role.Button, onClickLabel = tr("Déplier"), onClick = open)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("next_steps_compact"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(Neon.Cyan.copy(alpha = 0.2f)).border(1.dp, Neon.Cyan, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("${shown.number}", style = MaterialTheme.typography.labelMedium, color = Neon.Cyan) }
        Column(Modifier.weight(1f)) {
            Text(tr("Prochaine étape · %d/%d", steps.count { it.done }, steps.size).uppercase(), style = MaterialTheme.typography.labelSmall, color = Neon.Cyan, maxLines = 1)
            Text(shown.goal.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1,
                modifier = Modifier.testTag("next_step_title"))
        }
        Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.TextSecondary)
    }
}

/** One step: its number (a tick once done); a touch shows its name and hint below the row. */
@Composable
private fun StepDot(step: NextStep, shown: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = when {
        step.done -> Neon.Acid
        shown -> Neon.Cyan
        else -> Neon.TextSecondary
    }
    Column(
        modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp).padding(vertical = 4.dp)
            .semantics { contentDescription = if (step.done) tr("%d. %s, fait", step.number, step.goal.title) else "${step.number}. ${step.goal.title}" }
            .testTag("next_step_${step.goal.name}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(if (step.done) Neon.Acid.copy(alpha = 0.25f) else if (shown) Neon.Cyan.copy(alpha = 0.2f) else Neon.Surface)
                .border(1.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (step.done) Icon(Icons.Filled.Check, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(16.dp))
            else Text("${step.number}", style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
