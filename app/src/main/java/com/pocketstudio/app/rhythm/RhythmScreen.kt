package com.pocketstudio.app.rhythm

import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import com.pocketstudio.model.PadSample
import com.pocketstudio.ui.LevelMeter
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
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
    /** Drum loop length in bars (1 to 4). */
    val setBars: (Int) -> Unit = {},
    val fill: (every: Int) -> Unit = {},
    val rotate: (by: Int) -> Unit = {},
    val cycleRoll: (step: Int) -> Unit = {},
    /** The selected pad's pan, -1..1. */
    val setPadPan: (Float) -> Unit = {},
    /** One hit's own pan (-1..1) and pitch (semitones) on the selected pad. */
    val setHitPan: (step: Int, pan: Float) -> Unit = { _, _ -> },
    val setHitPitch: (step: Int, pitch: Int) -> Unit = { _, _ -> },
    /** Opens the drums' quick effects. */
    val openEffects: () -> Unit = {},
    /** Opens the sound library for the selected pad. */
    val openLibrary: () -> Unit = {},
    /** All-pads grid: (lane, step). */
    val toggleHit: (Int, Int) -> Unit = { _, _ -> },
    val cycleVelocity: (Int) -> Unit = {},
    val clearPad: () -> Unit = {},
    val nudgeTempo: (Int) -> Unit = {},
    val tapTempo: () -> Unit = {},
    val setSwing: (Double) -> Unit = {},
    val undo: () -> Unit = {},
    val redo: () -> Unit = {},
    val importSample: () -> Unit = {},
    val startRecording: () -> Unit = {},
    val stopRecording: () -> Unit = {},
    val removeSample: () -> Unit = {},
    val dismissSampleError: () -> Unit = {},
    val editSample: (key: String?, transform: (PadSample) -> PadSample) -> Unit = { _, _ -> },
    val openSlicer: () -> Unit = {},
    val closeSlicer: () -> Unit = {},
    val setSliceMode: (SliceMode) -> Unit = {},
    val setSliceSensitivity: (Float) -> Unit = {},
    val setSliceGrid: (Int) -> Unit = {},
    val toggleSliceMarker: (Float) -> Unit = {},
    val applySlices: () -> Unit = {},
    val resampleSong: (bars: Int) -> Unit = {},
    val importFolder: () -> Unit = {},
    /** A sample pack as a .zip file. */
    val importZip: () -> Unit = {},
)

/** The selected pad's own sound, if any, and what the sampler is busy with. */
data class PadSoundUi(
    val info: Map<String, SampleInfo> = emptyMap(),
    val task: SampleTask = SampleTask.Idle,
    val slicer: SlicerUi? = null,
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
    padSound: PadSoundUi = PadSoundUi(),
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("rhythm"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TransportBar(project, transport, canUndo, canRedo, actions)
        PadGrid(project, transport.selectedPad, live.litPads, actions.hitPad)
        StepGrid(project, transport.selectedPad, live.currentStep, transport.playing, actions)
        PadSoundCard(project, transport.selectedPad, padSound, actions)
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
private fun StepGrid(project: Project, lane: Int, currentStep: Int, playing: Boolean, actions: RhythmActions) {
    val haptics = LocalHapticFeedback.current
    val hits = project.drums.lanes[lane].hits
    val color = DrumKitInfo.colors[lane]
    // One page per bar of the time signature (12 steps in 3/4, 16 in 4/4…).
    val per = project.stepsPerBar
    val bars = (project.drums.steps / per).coerceAtLeast(1)
    var chosenBar by rememberSaveable { mutableIntStateOf(0) }
    var follow by rememberSaveable { mutableStateOf(true) }
    // 0 hits, 1 rolls, 2 pan, 3 pitch.
    var tool by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableIntStateOf(-1) }
    val ownSound = project.drums.lanes[lane].sample != null
    var allPads by rememberSaveable { mutableStateOf(false) }
    // Playing and following: the page turns with the playhead.
    val bar = (if (playing && follow && currentStep >= 0) currentStep / per else chosenBar).coerceIn(0, bars - 1)
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(if (allPads) "Pas : tous les pads" else "Pas : ${DrumKitInfo.names[lane]}", color = if (allPads) Neon.Cyan else color)
            Box(Modifier.weight(1f))
            FilterChip(selected = allPads, onClick = { allPads = !allPads }, label = { Text("Tous les pads") }, colors = chipColors,
                modifier = Modifier.testTag("all_pads"))
            if (!allPads) TextButton(onClick = actions.clearPad, enabled = hits.isNotEmpty()) { Text("Effacer") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Longueur", style = MaterialTheme.typography.bodyMedium)
            (1..4).filter { it * per <= 64 }.forEach { b ->
                FilterChip(selected = bars == b, onClick = { actions.setBars(b) }, label = { Text(if (b == 1) "1 mesure" else "$b") },
                    colors = chipColors, modifier = Modifier.testTag("drum_bars_$b"))
            }
        }
        if (bars > 1) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Mesure", style = MaterialTheme.typography.bodyMedium)
                (0 until bars).forEach { b ->
                    FilterChip(selected = bar == b, onClick = { chosenBar = b; follow = false }, label = { Text("${b + 1}") },
                        colors = chipColors, modifier = Modifier.testTag("drum_page_$b"))
                }
                FilterChip(selected = follow, onClick = { follow = !follow; chosenBar = bar }, label = { Text("Suivre") }, colors = chipColors)
            }
        }
        if (allPads) {
            AllPadsGrid(project, bar, currentStep, actions)
            return@NeonCard
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selected = tool == 0, onClick = { tool = 0 }, label = { Text("Coups") }, colors = chipColors)
            FilterChip(selected = tool == 1, onClick = { tool = 1 }, label = { Text("Roulements") }, colors = chipColors,
                modifier = Modifier.testTag("roll_tool"))
            FilterChip(selected = tool == 2, onClick = { tool = 2 }, label = { Text("Pan") }, colors = chipColors,
                modifier = Modifier.testTag("pan_tool"))
            FilterChip(selected = tool == 3, onClick = { tool = 3 }, label = { Text("Hauteur") }, colors = chipColors,
                modifier = Modifier.testTag("pitch_tool"))
            Text("  Remplir", style = MaterialTheme.typography.bodySmall)
            listOf(1 to "tout", 2 to "1/2", 4 to "1/4").forEach { (every, label) ->
                TextButton(onClick = { actions.fill(every) }, modifier = Modifier.testTag("fill_$every")) { Text(label) }
            }
            TextButton(onClick = { actions.rotate(-1) }, enabled = hits.isNotEmpty(), modifier = Modifier.testTag("rotate_left")) { Text("◀") }
            TextButton(onClick = { actions.rotate(1) }, enabled = hits.isNotEmpty(), modifier = Modifier.testTag("rotate_right")) { Text("▶") }
        }
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until per / 2) {
                    val step = bar * per + row * (per / 2) + col
                    val velocity = hits[step]?.velocity
                    val isBeat = step % 4 == 0
                    val playing = step == currentStep
                    val hit = hits[step]
                    val isPicked = tool >= 2 && step == picked && hit != null
                    val fill = if (isBeat) Neon.SurfaceHigh else Neon.Surface
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.8f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(fill)
                            .border(
                                if (playing || isPicked) 2.dp else 1.dp,
                                when {
                                    isPicked -> Neon.Cyan
                                    playing -> Neon.TextPrimary
                                    else -> Neon.Outline
                                },
                                RoundedCornerShape(6.dp),
                            )
                            .combinedClickable(
                                onClick = {
                                    // A loop shorter than the page (an older length): its end is not editable.
                                    if (step < project.drums.steps) {
                                        when (tool) {
                                            0 -> actions.toggleStep(step)
                                            1 -> actions.cycleRoll(step)
                                            else -> if (hit != null) picked = step
                                        }
                                    }
                                },
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
                            val repeats = hits[step]?.repeats ?: 1
                            if (repeats > 1) {
                                Text("×$repeats", style = MaterialTheme.typography.labelLarge, color = Color(0xFF1E2023),
                                    modifier = Modifier.align(Alignment.Center))
                            }
                            // Own pan or pitch: a small mark at the top of the cell.
                            val mark = listOfNotNull(
                                hit?.pan?.takeIf { it != 0f }?.let { panShort(it) },
                                hit?.pitch?.takeIf { it != 0 }?.let { if (it > 0) "+$it" else "$it" },
                            ).joinToString(" ")
                            if (mark.isNotEmpty()) {
                                Text(mark, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E2023),
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp))
                            }
                        }
                        if (isBeat) {
                            Text(
                                "${(step % per) / 4 + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Neon.TextSecondary,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        val pickedHit = hits[picked]?.takeIf { tool >= 2 }
        if (pickedHit != null && tool == 2) {
            LabeledSlider("Pan du coup ${picked % per + 1}", panLabel(pickedHit.pan), pickedHit.pan, -1f..1f, steps = 19) {
                actions.setHitPan(picked, (it * 10).roundToInt() / 10f)
            }
        }
        if (pickedHit != null && tool == 3 && ownSound) {
            LabeledSlider("Hauteur du coup ${picked % per + 1}", semitones(pickedHit.pitch), pickedHit.pitch.toFloat(), -12f..12f, steps = 23) {
                actions.setHitPitch(picked, it.roundToInt())
            }
        }
        Text(
            when (tool) {
                1 -> "Roulements : touche un coup pour le doubler, tripler, quadrupler (×2, ×3, ×4), puis revenir à un seul."
                2 -> "Pan : touche un coup, puis glisse pour l'envoyer à gauche ou à droite (en plus du pan du pad)."
                3 -> if (ownSound) "Hauteur : touche un coup, puis glisse pour le monter ou le descendre (en demi-tons)."
                else "Hauteur : seulement pour un son importé, enregistré ou généré (pas pour les sons synthétisés)."
                else -> "Touche une case pour ajouter un coup. Appui long sur un coup : fort → moyen → doux (la case se vide à moitié)."
            },
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
            Box(Modifier.weight(1f))
            OutlinedButton(onClick = actions.openEffects, modifier = Modifier.testTag("drum_effects")) { Text("Effets") }
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

@Composable
internal fun PadSoundCard(project: Project, lane: Int, ui: PadSoundUi, actions: RhythmActions) {
    val color = DrumKitInfo.colors[lane]
    val sample = project.drums.lanes[lane].sample
    NeonCard(modifier = Modifier.testTag("pad_sound")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Son du pad", color = color)
            Box(Modifier.weight(1f))
            Text(
                sample?.name ?: "${DrumKitInfo.names[lane]} (synthétisé)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (sample != null) Neon.TextPrimary else Neon.TextSecondary,
            )
        }
        val pan = project.drums.lanes[lane].pan
        LabeledSlider("Pan du pad", panLabel(pan), pan, -1f..1f, steps = 19) { actions.setPadPan((it * 10).roundToInt() / 10f) }
        val info = sample?.let { ui.info[it.file] }
        val slicer = ui.slicer?.takeIf { sample != null && it.file == sample.file }
        if (sample != null && info != null && slicer != null) {
            SlicerPanel(slicer, info, lane, color, actions)
            return@NeonCard
        }
        if (sample != null && info != null) {
            Waveform(info.peaks, color, region = sample.start to sample.end)
            Text(
                String.format(java.util.Locale.FRANCE, "%.2f s · pince pour zoomer", info.seconds * (sample.end - sample.start)),
                style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary,
            )
            PadSampleControls(sample, info.seconds, project.tempoBpm, actions.editSample)
            OutlinedButton(onClick = actions.openSlicer, modifier = Modifier.fillMaxWidth().testTag("open_slicer")) {
                Icon(Icons.Filled.ContentCut, contentDescription = null); Text(" Découper en tranches")
            }
        }
        when (val task = ui.task) {
            is SampleTask.Busy -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = color)
                Text(task.message)
            }
            is SampleTask.Recording -> {
                Text(String.format(java.util.Locale.FRANCE, "Enregistrement… %.1f s / 30 s", task.seconds), color = Neon.Red)
                LevelMeter(task.level)
                Button(
                    onClick = actions.stopRecording,
                    modifier = Modifier.fillMaxWidth().testTag("record_stop"),
                    colors = ButtonDefaults.buttonColors(containerColor = Neon.Red, contentColor = Color.Black),
                ) { Icon(Icons.Filled.Stop, contentDescription = null); Text(" Arrêter") }
            }
            else -> {
                if (task is SampleTask.Failed) {
                    Text(task.message, color = Neon.Red, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { actions.dismissSampleError(); actions.openLibrary() }, modifier = Modifier.fillMaxWidth().testTag("open_library")) {
                    Text("Bibliothèque de sons")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { actions.dismissSampleError(); actions.importSample() }, modifier = Modifier.weight(1f).testTag("import_sample")) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null); Text(" Importer")
                    }
                    OutlinedButton(onClick = { actions.dismissSampleError(); actions.startRecording() }, modifier = Modifier.weight(1f).testTag("record_sample")) {
                        Icon(Icons.Filled.Mic, contentDescription = null); Text(" Micro")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pack sur les pads suivants :", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { actions.dismissSampleError(); actions.importFolder() }, modifier = Modifier.testTag("import_folder")) { Text("Dossier") }
                    TextButton(onClick = { actions.dismissSampleError(); actions.importZip() }, modifier = Modifier.testTag("import_zip")) { Text(".zip") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Enregistrer le morceau :", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    listOf(1, 2, 4).forEach { bars ->
                        TextButton(
                            onClick = { actions.dismissSampleError(); actions.resampleSong(bars) },
                            modifier = Modifier.testTag("resample_$bars"),
                        ) { Text(if (bars == 1) "1 mesure" else "$bars mes.") }
                    }
                }
                if (sample != null) {
                    TextButton(onClick = actions.removeSample, modifier = Modifier.testTag("remove_sample")) {
                        Text("Revenir au son ${DrumKitInfo.names[lane]}")
                    }
                } else {
                    Text(
                        "Mets ton propre son sur ce pad : un fichier du téléphone (MP3, WAV, FLAC, M4A…) ou le micro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neon.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PadSampleControls(s: PadSample, seconds: Double, bpm: Double, edit: (String?, (PadSample) -> PadSample) -> Unit) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    LabeledSlider("Début", String.format(java.util.Locale.FRANCE, "%.2f s", s.start * seconds), s.start, 0f..1f) { v ->
        edit("start") { it.copy(start = minOf(v, it.end - 0.001f)) }
    }
    LabeledSlider("Fin", String.format(java.util.Locale.FRANCE, "%.2f s", s.end * seconds), s.end, 0f..1f) { v ->
        edit("end") { it.copy(end = maxOf(v, it.start + 0.001f)) }
    }
    val semis = s.pitch.roundToInt()
    LabeledSlider("Hauteur", if (semis == 0) "normale" else "${if (semis > 0) "+" else ""}$semis demi-tons", s.pitch, -24f..24f) { v ->
        edit("pitch") { it.copy(pitch = v.roundToInt().toFloat()) }
    }
    val regionSeconds = seconds * (s.end - s.start)
    Text(
        if (s.fitBeats == 0) "Calé au tempo : non (le son garde sa vitesse)"
        else "Calé au tempo : ${s.fitBeats} temps à ${bpm.roundToInt()} BPM, sans changer la hauteur",
        style = MaterialTheme.typography.bodyMedium,
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val suggestion = PadSample.suggestBeats(regionSeconds, bpm)
        PadSample.FIT_BEATS.forEach { beats ->
            FilterChip(
                selected = s.fitBeats == beats,
                onClick = { edit(null) { it.copy(fitBeats = beats) } },
                label = { Text(if (beats == 0) "Non" else if (beats == suggestion) "$beats ★" else "$beats") },
                colors = chipColors,
                modifier = Modifier.testTag("fit_$beats"),
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = s.reverse, onClick = { edit(null) { it.copy(reverse = !it.reverse) } }, label = { Text("À l'envers") }, colors = chipColors)
        FilterChip(selected = s.loop, onClick = { edit(null) { it.copy(loop = !it.loop) } }, label = { Text("En boucle") }, colors = chipColors)
    }
    TextButton(onClick = { advanced = !advanced }) {
        Text(if (advanced) "Masquer l'enveloppe et les groupes" else "Enveloppe, volume, groupe d'étouffement")
    }
    if (advanced) {
        LabeledSlider("Volume", "${(s.gain * 100).roundToInt()} %", s.gain, 0f..2f) { v -> edit("gain") { it.copy(gain = v) } }
        LabeledSlider("Attaque", ms(s.attack), s.attack, 0f..1f) { v -> edit("attack") { it.copy(attack = v) } }
        LabeledSlider("Décroissance", ms(s.decay), s.decay, 0f..2f) { v -> edit("decay") { it.copy(decay = v) } }
        LabeledSlider("Maintien", "${(s.sustain * 100).roundToInt()} %", s.sustain, 0f..1f) { v -> edit("sustain") { it.copy(sustain = v) } }
        LabeledSlider("Relâche", ms(s.release), s.release, 0f..2f) { v -> edit("release") { it.copy(release = v) } }
        Text("Groupe d'étouffement : un coup coupe les autres pads du même groupe (charleston ouvert/fermé).",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..PadSample.CHOKE_GROUPS).forEach { g ->
                FilterChip(
                    selected = s.choke == g,
                    onClick = { edit(null) { it.copy(choke = g) } },
                    label = { Text(if (g == 0) "Aucun" else "$g") },
                    colors = chipColors,
                )
            }
        }
    }
}

internal fun panLabel(pan: Float): String = when {
    pan < -0.05f -> "G ${(-pan * 100).roundToInt()}"
    pan > 0.05f -> "D ${(pan * 100).roundToInt()}"
    else -> "Centre"
}

private fun panShort(pan: Float) = if (pan < 0f) "G" else "D"

private fun semitones(n: Int) = when {
    n == 0 -> "0"
    n > 0 -> "+$n demi-ton${if (n > 1) "s" else ""}"
    else -> "$n demi-ton${if (n < -1) "s" else ""}"
}

private fun ms(seconds: Float) =
    if (seconds < 1f) "${(seconds * 1000).roundToInt()} ms" else String.format(java.util.Locale.FRANCE, "%.2f s", seconds)

@Composable
private fun LabeledSlider(
    label: String,
    value: String,
    v: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MonoStyle, color = Neon.Cyan)
    }
    Slider(value = v.coerceIn(range), onValueChange = onChange, valueRange = range, steps = steps)
}

@Composable
private fun SlicerPanel(s: SlicerUi, info: SampleInfo, lane: Int, color: Color, actions: RhythmActions) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    Text("Découpe", style = MaterialTheme.typography.titleMedium)
    Waveform(info.peaks, color, markers = s.markers, markerColor = Neon.TextPrimary, onTap = actions.toggleSliceMarker,
        modifier = Modifier.testTag("slicer_wave"))
    Text("Touche la forme d'onde pour ajouter ou retirer une coupe. Pince pour zoomer.",
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SliceMode.entries.forEach { m ->
            FilterChip(selected = s.mode == m, onClick = { actions.setSliceMode(m) }, label = { Text(m.label) }, colors = chipColors)
        }
    }
    when (s.mode) {
        SliceMode.Attacks -> LabeledSlider(
            "Sensibilité",
            if (s.busy) "…" else "${(s.sensitivity * 100).roundToInt()} %",
            s.sensitivity, 0f..1f,
        ) { actions.setSliceSensitivity(it) }
        SliceMode.Grid -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Slicer.GRIDS.forEach { n ->
                FilterChip(selected = s.grid == n, onClick = { actions.setSliceGrid(n) }, label = { Text("$n") }, colors = chipColors)
            }
        }
    }
    val count = minOf(s.markers.size, DrumKitInfo.names.size - lane)
    Text(
        if (count <= 1) "Une seule tranche : ajoute des coupes." else "$count tranches → pads ${lane + 1} à ${lane + count} (Annuler possible)",
        style = MaterialTheme.typography.bodyMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = actions.closeSlicer, modifier = Modifier.weight(1f)) { Text("Fermer") }
        Button(
            onClick = actions.applySlices,
            enabled = count > 1 && !s.busy,
            modifier = Modifier.weight(1f).testTag("apply_slices"),
        ) { Text("Sur les pads") }
    }
}

/** Every pad's steps for one bar: rows of pads, tap a cell to add or remove a hit. */
@Composable
internal fun AllPadsGrid(project: Project, bar: Int, currentStep: Int, actions: RhythmActions) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.testTag("all_pads_grid")) {
        project.drums.lanes.forEachIndexed { lane, l ->
            val color = DrumKitInfo.colors[lane]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(DrumKitInfo.names[lane], style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    color = if (l.hits.isEmpty()) Neon.TextSecondary else Neon.TextPrimary, modifier = Modifier.width(72.dp),
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                for (col in 0 until project.stepsPerBar) {
                    val step = bar * project.stepsPerBar + col
                    val hit = l.hits[step]
                    Box(
                        Modifier.weight(1f).height(22.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (hit != null) color.copy(alpha = 0.35f + 0.65f * hit.velocity) else if (col % 4 == 0) Neon.SurfaceHigh else Neon.Surface)
                            .border(if (step == currentStep) 2.dp else 0.5.dp, if (step == currentStep) Neon.TextPrimary else Neon.Outline, RoundedCornerShape(3.dp))
                            .clickable { actions.toggleHit(lane, step) }
                            .testTag("cell_${lane}_$step"),
                    )
                }
            }
        }
    }
}
