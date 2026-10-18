package com.pocketstudio.app.acid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

private val NOTE_NAMES = listOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")

/** "La" for a MIDI note, French names, no octave. */
fun noteNameFr(midi: Int): String = NOTE_NAMES[Math.floorMod(midi, 12)]

private val SOUND_LABELS = listOf("Coupure", "Résonance", "Enveloppe", "Déclin", "Accent", "Accord", "Onde (scie → carré)", "Saturation", "Volume")

data class AcidActions(
    val togglePlay: () -> Unit = {},
    val generate: () -> Unit = {},
    val selectStep: (Int) -> Unit = {},
    val editStep: ((AcidStep) -> AcidStep) -> Unit = {},
    val toggleLock: () -> Unit = {},
    val setSound: (Int, Float) -> Unit = { _, _ -> },
    val shiftRoot: (Int) -> Unit = {},
    val setDensity: (Float) -> Unit = {},
)

@Composable
fun AcidScreen(
    acid: AcidPattern,
    playing: Boolean,
    currentStep: Int,
    selected: Int,
    actions: AcidActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("acid"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NeonCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = actions.togglePlay,
                    modifier = Modifier.height(56.dp).testTag("acid_play"),
                    colors = ButtonDefaults.buttonColors(containerColor = if (playing) Neon.Magenta else Neon.Cyan),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                }
                Button(
                    onClick = actions.generate,
                    modifier = Modifier.weight(1f).height(56.dp).testTag("acid_generate"),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Text("  Génère-moi un pattern", style = MaterialTheme.typography.titleSmall)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Densité", Modifier.weight(1f))
                Text("${(acid.density * 100).roundToInt()} %", style = MonoStyle, color = Neon.Cyan)
            }
            Slider(value = acid.density, onValueChange = actions.setDensity)
        }

        StepGrid(acid, currentStep, selected, actions.selectStep)
        StepEditor(acid, selected, actions)
        SoundCard(acid, actions)
    }
}

@Composable
private fun StepGrid(acid: AcidPattern, currentStep: Int, selected: Int, onSelect: (Int) -> Unit) {
    val playStep = if (currentStep >= 0) currentStep % acid.length else -1
    NeonCard {
        SectionLabel("Ligne de basse · base ${noteNameFr(acid.root)}")
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 8) {
                    val i = row * 8 + col
                    val s = acid.steps[i]
                    val isSelected = i == selected
                    val fill = when {
                        !s.gate -> Neon.Surface
                        s.accent -> Neon.Cyan
                        else -> Neon.Cyan.copy(alpha = 0.45f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(fill)
                            .border(
                                width = if (isSelected || i == playStep) 2.dp else 1.dp,
                                color = when {
                                    i == playStep -> Neon.TextPrimary
                                    isSelected -> Neon.Magenta
                                    else -> Neon.Outline
                                },
                                shape = RoundedCornerShape(6.dp),
                            )
                            .clickable { onSelect(i) }
                            .testTag("acid_step_$i"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (s.gate) noteNameFr(acid.root + s.note) else "–",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (s.gate && s.accent) Neon.Background else Neon.TextPrimary,
                            )
                            val marks = buildString {
                                if (s.gate && s.octave > 0) append("↑".repeat(s.octave))
                                if (s.gate && s.octave < 0) append("↓")
                                if (s.gate && s.slide) append("⌒")
                                if (i in acid.locked) append("🔒")
                            }
                            if (marks.isNotEmpty()) {
                                Text(marks, fontSize = 11.sp, color = if (s.gate && s.accent) Neon.Background else Neon.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
        Text(
            "Touche un pas pour le modifier. Couleur pleine = accent, ⌒ = slide, ↑ = octave, – = silence, 🔒 = gardé par le générateur.",
            style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary,
        )
    }
}

@Composable
private fun StepEditor(acid: AcidPattern, index: Int, actions: AcidActions) {
    val s = acid.steps[index]
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f),
        selectedLabelColor = Neon.Cyan,
    )
    NeonCard(accent = Neon.Magenta) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Pas ${index + 1}", color = Neon.Magenta)
            Box(Modifier.weight(1f))
            FilterChip(
                selected = index in acid.locked,
                onClick = actions.toggleLock,
                label = { Text("Verrou") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                colors = chipColors,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { actions.editStep { it.copy(note = (it.note - 1).coerceAtLeast(0), gate = true) } }) {
                Icon(Icons.Filled.Remove, contentDescription = "Note plus grave")
            }
            Text(
                if (s.gate) noteNameFr(acid.root + s.note) else "Silence",
                style = MonoStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                color = if (s.gate) Neon.Cyan else Neon.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { actions.editStep { it.copy(note = (it.note + 1).coerceAtMost(12), gate = true) } }) {
                Icon(Icons.Filled.Add, contentDescription = "Note plus aiguë")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = s.gate, onClick = { actions.editStep { it.copy(gate = !it.gate) } }, label = { Text("Note") }, colors = chipColors)
            FilterChip(selected = s.accent, onClick = { actions.editStep { it.copy(accent = !it.accent, gate = true) } }, label = { Text("Accent") }, colors = chipColors)
            FilterChip(selected = s.slide, onClick = { actions.editStep { it.copy(slide = !it.slide, gate = true) } }, label = { Text("Slide") }, colors = chipColors)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Octave", Modifier.weight(1f))
            IconButton(onClick = { actions.editStep { it.copy(octave = (it.octave - 1).coerceAtLeast(-1), gate = true) } }) {
                Icon(Icons.Filled.Remove, contentDescription = "Octave plus bas")
            }
            Text(if (s.octave > 0) "+${s.octave}" else "${s.octave}", style = MonoStyle, color = Neon.Cyan)
            IconButton(onClick = { actions.editStep { it.copy(octave = (it.octave + 1).coerceAtMost(2), gate = true) } }) {
                Icon(Icons.Filled.Add, contentDescription = "Octave plus haut")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Note de base", Modifier.weight(1f))
            IconButton(onClick = { actions.shiftRoot(-1) }) { Icon(Icons.Filled.Remove, contentDescription = "Base plus grave") }
            Text(noteNameFr(acid.root), style = MonoStyle, color = Neon.Cyan)
            IconButton(onClick = { actions.shiftRoot(1) }) { Icon(Icons.Filled.Add, contentDescription = "Base plus aiguë") }
        }
    }
}

@Composable
private fun SoundCard(acid: AcidPattern, actions: AcidActions) {
    val values = acid.sound.values()
    NeonCard {
        SectionLabel("Son")
        SOUND_LABELS.forEachIndexed { i, label ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text("${(values[i] * 100).roundToInt()}", style = MonoStyle, color = Neon.Cyan)
            }
            Slider(
                value = values[i],
                onValueChange = { actions.setSound(i, it) },
                modifier = Modifier.testTag("acid_sound_$i"),
            )
        }
    }
}
