@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pocketstudio.app.mixer

import com.pocketstudio.ui.ScaledBottomSheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.partMarks
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.songBars
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * Phase 37: the lane « Automatiser » opened from a knob (appui long), over the screen where the
 * knob is: its curve along the song, the parts named under it, a point a bar or a beat. The same
 * lane as in the Mixeur's « Automation » card (where every curve stays).
 */
@Composable
fun AutomateLaneRoute(vm: RhythmViewModel, target: com.pocketstudio.model.AutoTarget, close: () -> Unit) {
    val project by vm.project.collectAsState()
    val live by vm.live.collectAsState()
    val transport by vm.transport.collectAsState()
    // Found by its control (an undo may have taken the lane away, or moved it): none, nothing shown.
    val lane = project.automation.indexOfFirst { it.target == target }
    val l = project.automation.getOrNull(lane) ?: return
    val strips = com.pocketstudio.app.ui.mixerStrips(project)
    val ui = AutomationUi(
        project.automation, project.drums.steps, -1, false,
        scene = project.scene,
        songBars = project.songBars,
        stepsPerBar = project.stepsPerBar,
        playStep = { if (transport.playing) live.currentStep else -1 },
        playSongStep = { if (transport.playing) live.songStep else -1 },
        parts = project.partMarks(),
        partSteps = project.sceneSteps(project.scene),
        playPartStep = { if (transport.playing) live.absoluteStep else -1 },
    )
    val actions = AutomationActions(
        remove = { vm.removeAutomation(it); close() },
        clear = vm::clearAutomation,
        setPoint = vm::setAutomationPoint,
        setMode = vm::setAutomationMode,
        setSongPoint = vm::setAutomationSongPoint,
        edit = vm::editAutomationLane,
        setResolution = vm::setLaneResolution,
        setLoopSteps = vm::setAutomationLoopSteps,
    )
    AutomateLaneSheet(lane, ui, project.mixer.channels, project.mixer.master, strips, actions, automationLabel(l.target, project.mixer.channels, project.mixer.master, strips), close)
}

@Composable
fun AutomateLaneSheet(
    lane: Int, ui: AutomationUi, channels: List<com.pocketstudio.model.Channel>, master: List<com.pocketstudio.model.Insert>,
    strips: List<StripInfo>, actions: AutomationActions, title: String, close: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("automate_sheet")) {
        AutomateLaneContent(lane, ui, channels, master, strips, actions, title, close)
    }
}

/** The sheet's body (drawn alone in the screenshots). */
@Composable
fun AutomateLaneContent(
    lane: Int, ui: AutomationUi, channels: List<com.pocketstudio.model.Channel>, master: List<com.pocketstudio.model.Insert>,
    strips: List<StripInfo>, actions: AutomationActions, title: String, close: () -> Unit,
) {
    val l = ui.lanes.getOrNull(lane) ?: return
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(tr("Automatiser"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Neon.Amber, modifier = Modifier.testTag("automate_title"))
            }
            TextButton(onClick = close, modifier = Modifier.testTag("automate_close")) { Text(tr("Fermer")) }
        }
        Text(
            tr("Dessine la courbe du réglage au doigt (ou une ligne en deux touches) : il bouge tout seul pendant le morceau. Toutes les courbes sont aussi dans Mix."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        LaneBlock(lane, l, ui, channels, master, strips, actions)
    }
}
