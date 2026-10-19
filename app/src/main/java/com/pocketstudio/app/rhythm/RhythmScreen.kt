package com.pocketstudio.app.rhythm

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.Project
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

data class RhythmActions(
    val togglePlay: () -> Unit = {},
    val toggleMetronome: () -> Unit = {},
    val toggleCountIn: () -> Unit = {},
    val hitPad: (Int) -> Unit = {},
    val toggleStep: (Int) -> Unit = {},
    val cycleVelocity: (Int) -> Unit = {},
    val clearPad: () -> Unit = {},
    val nudgeTempo: (Int) -> Unit = {},
    val tapTempo: () -> Unit = {},
    val setSwing: (Double) -> Unit = {},
    val undo: () -> Unit = {},
    val redo: () -> Unit = {},
)

@Composable
fun RhythmScreen(
    project: Project,
    transport: TransportUi,
    live: LiveUi,
    canUndo: Boolean,
    canRedo: Boolean,
    actions: RhythmActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("rhythm"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TransportBar(project, transport, canUndo, canRedo, actions)
        PadGrid(project, transport.selectedPad, live.litPads, actions.hitPad)
        StepGrid(project, transport.selectedPad, live.currentStep, actions)
        GrooveCard(project, transport, actions)
    }
}

@Composable
private fun TransportBar(project: Project, transport: TransportUi, canUndo: Boolean, canRedo: Boolean, actions: RhythmActions) {
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = actions.togglePlay,
                modifier = Modifier.height(56.dp).testTag("transport_play"),
                colors = ButtonDefaults.buttonColors(containerColor = if (transport.playing) Neon.Magenta else Neon.Cyan),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(if (transport.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                Text(if (transport.playing) " Stop" else " Jouer", style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.weight(1f))
            IconButton(onClick = actions.undo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Annuler")
            }
            IconButton(onClick = actions.redo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rétablir")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { actions.nudgeTempo(-1) }) { Icon(Icons.Filled.Remove, contentDescription = "Tempo moins") }
            Text(
                "${project.tempoBpm.roundToInt()} BPM",
                style = MonoStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                color = Neon.Cyan,
                modifier = Modifier.testTag("tempo"),
            )
            IconButton(onClick = { actions.nudgeTempo(1) }) { Icon(Icons.Filled.Add, contentDescription = "Tempo plus") }
            Box(Modifier.weight(1f))
            OutlinedButton(
                onClick = actions.tapTempo,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Neon.TextPrimary),
                modifier = Modifier.testTag("tap"),
            ) { Text("Tap") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PadGrid(project: Project, selected: Int, lit: Set<Int>, onHit: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 4) {
                    val lane = row * 4 + col
                    val color = DrumKitInfo.colors[lane]
                    val hasHits = project.drums.lanes[lane].hits.isNotEmpty()
                    val background = when {
                        lane in lit -> color
                        lane == selected -> color.copy(alpha = 0.45f)
                        else -> color.copy(alpha = 0.18f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.25f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(background)
                            .border(if (lane == selected) 2.dp else 1.dp, if (lane == selected) color else Neon.Outline, RoundedCornerShape(10.dp))
                            .combinedClickable(onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onHit(lane)
                            })
                            .testTag("pad_$lane"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            DrumKitInfo.names[lane],
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (lane in lit) Color(0xFF1E2023) else Neon.TextPrimary,
                            modifier = Modifier.padding(4.dp),
                        )
                        if (hasHits) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(6.dp).size(7.dp)
                                    .clip(RoundedCornerShape(4.dp)).background(color),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepGrid(project: Project, lane: Int, currentStep: Int, actions: RhythmActions) {
    val haptics = LocalHapticFeedback.current
    val hits = project.drums.lanes[lane].hits
    val color = DrumKitInfo.colors[lane]
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Pas : ${DrumKitInfo.names[lane]}", color = color)
            Box(Modifier.weight(1f))
            TextButton(onClick = actions.clearPad, enabled = hits.isNotEmpty()) { Text("Effacer") }
        }
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 8) {
                    val step = row * 8 + col
                    val velocity = hits[step]?.velocity
                    val isBeat = step % 4 == 0
                    val playing = step == currentStep
                    val fill = if (isBeat) Neon.SurfaceHigh else Neon.Surface
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.8f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(fill)
                            .border(if (playing) 2.dp else 1.dp, if (playing) Neon.TextPrimary else Neon.Outline, RoundedCornerShape(6.dp))
                            .combinedClickable(
                                onClick = { actions.toggleStep(step) },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    actions.cycleVelocity(step)
                                },
                            )
                            .testTag("step_$step"),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // The hit fills the cell up to its strength: full, two thirds or one third.
                        if (velocity != null) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight(velocity.coerceIn(0.2f, 1f)).background(color))
                        }
                        if (isBeat) {
                            Text(
                                "${step / 4 + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Neon.TextSecondary,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            "Touche une case pour ajouter un coup. Appui long sur un coup : fort → moyen → doux (la case se vide à moitié).",
            style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary,
        )
    }
}

@Composable
private fun GrooveCard(project: Project, transport: TransportUi, actions: RhythmActions) {
    NeonCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f),
                selectedLabelColor = Neon.Cyan,
            )
            FilterChip(selected = transport.metronome, onClick = actions.toggleMetronome, label = { Text("Métronome") }, colors = chipColors)
            FilterChip(selected = transport.countIn, onClick = actions.toggleCountIn, label = { Text("Décompte") }, colors = chipColors)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Swing", Modifier.weight(1f))
            Text("${(project.swing / Project.MAX_SWING * 100).roundToInt()} %", style = MonoStyle, color = Neon.Cyan)
        }
        Slider(
            value = (project.swing / Project.MAX_SWING).toFloat(),
            onValueChange = { actions.setSwing(it * Project.MAX_SWING) },
        )
    }
}
