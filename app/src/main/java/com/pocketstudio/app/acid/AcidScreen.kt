package com.pocketstudio.app.acid

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.TextButton
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
import com.pocketstudio.model.AcidSound
import com.pocketstudio.ui.Knob
import com.pocketstudio.ui.KnobColors
import com.pocketstudio.model.AcidStep
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

private val NOTE_NAMES = listOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
private val NOTE_NAMES_EN = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/** "La" for a MIDI note, French names, no octave. */
fun noteNameFr(midi: Int): String = NOTE_NAMES[Math.floorMod(midi, 12)]

/** French (Do Ré Mi) or English (C D E) name, as most tabs found online use. */
fun noteName(midi: Int, english: Boolean): String =
    (if (english) NOTE_NAMES_EN else NOTE_NAMES)[Math.floorMod(midi, 12)]

/** The note naming the user picked, remembered on the phone. */
private object Notation {
    private const val KEY = "notation_english"
    fun isEnglish(context: Context) = context.getSharedPreferences("ui", Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun setEnglish(context: Context, on: Boolean) =
        context.getSharedPreferences("ui", Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
}

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
    val setBars: (Int) -> Unit = {},
    val copyPage: (from: Int, to: Int) -> Unit = { _, _ -> },
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

        val context = LocalContext.current
        var english by remember { mutableStateOf(Notation.isEnglish(context)) }
        val setEnglish: (Boolean) -> Unit = { english = it; Notation.setEnglish(context, it) }
        StepGrid(acid, playing, currentStep, selected, actions, english, setEnglish)
        StepEditor(acid, selected, actions, english)
        SoundCard(acid, actions)
    }
}

@Composable
private fun StepGrid(
    acid: AcidPattern,
    playing: Boolean,
    currentStep: Int,
    selected: Int,
    actions: AcidActions,
    english: Boolean,
    setEnglish: (Boolean) -> Unit,
) {
    val playStep = if (currentStep >= 0) currentStep % acid.length else -1
    val pages = (acid.length + AcidPattern.STEPS_PER_PAGE - 1) / AcidPattern.STEPS_PER_PAGE
    var page by rememberSaveable { mutableIntStateOf(0) }
    var follow by rememberSaveable { mutableStateOf(true) }
    if (page >= pages) page = 0
    // While playing, the grid shows the bar being heard (unless the user picked one).
    val shown = if (playing && follow && playStep >= 0) playStep / AcidPattern.STEPS_PER_PAGE else page
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Ligne de basse · base ${noteName(acid.root, english)}", Modifier.weight(1f))
            FilterChip(
                selected = english, onClick = { setEnglish(!english) },
                label = { Text(if (english) "C D E" else "Do Ré Mi") },
                colors = chipColors,
                modifier = Modifier.testTag("acid_notation"),
            )
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Longueur", style = MaterialTheme.typography.bodySmall)
            AcidPattern.LENGTHS_IN_BARS.forEach { bars ->
                FilterChip(
                    selected = acid.length == bars * AcidPattern.STEPS_PER_PAGE,
                    onClick = { actions.setBars(bars) },
                    label = { Text(if (bars == 1) "1 mesure" else "$bars") },
                    colors = chipColors,
                    modifier = Modifier.testTag("acid_bars_$bars"),
                )
            }
        }
        if (pages > 1) {
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Mesure", style = MaterialTheme.typography.bodySmall)
                for (p in 0 until pages) {
                    FilterChip(
                        selected = shown == p,
                        onClick = { page = p; follow = false },
                        label = { Text("${p + 1}") },
                        colors = chipColors,
                        modifier = Modifier.testTag("acid_page_$p"),
                    )
                }
                FilterChip(selected = follow, onClick = { follow = !follow }, label = { Text("Suivre") }, colors = chipColors)
                TextButton(onClick = { actions.copyPage(shown, (shown + 1) % pages) }, modifier = Modifier.testTag("acid_copy_page")) {
                    Text("Copier → ${(shown + 1) % pages + 1}")
                }
            }
        }
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 8) {
                    val i = shown * AcidPattern.STEPS_PER_PAGE + row * 8 + col
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
                            .clickable { actions.selectStep(i) }
                            .testTag("acid_step_$i"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (s.gate) noteName(acid.root + s.note, english) else "–",
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
            "Touche un pas pour le modifier. Couleur pleine = accent, ⌒ = slide, ↑ = octave au-dessus (Up), ↓ = en dessous (Down), – = silence, 🔒 = gardé par le générateur.",
            style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary,
        )
    }
}

@Composable
private fun StepEditor(acid: AcidPattern, index: Int, actions: AcidActions, english: Boolean) {
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
        Text(
            if (s.gate) noteName(acid.root + s.note, english) + octaveMark(s.octave) else "Silence",
            style = MonoStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            color = if (s.gate) Neon.Cyan else Neon.TextSecondary,
        )
        // Any of the 12 notes in one tap; the octave (Up / Down) is set below.
        NoteKeys(
            selected = if (s.gate) Math.floorMod(acid.root + s.note, 12) else -1,
            english = english,
            onPick = { pc -> actions.editStep { it.copy(note = Math.floorMod(pc - acid.root, 12), gate = true) } },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = s.gate, onClick = { actions.editStep { it.copy(gate = !it.gate) } }, label = { Text("Note") }, colors = chipColors)
            FilterChip(selected = s.accent, onClick = { actions.editStep { it.copy(accent = !it.accent, gate = true) } }, label = { Text("Accent") }, colors = chipColors)
            FilterChip(selected = s.slide, onClick = { actions.editStep { it.copy(slide = !it.slide, gate = true) } }, label = { Text("Slide") }, colors = chipColors)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Octave (Down ↓ / Up ↑)", Modifier.weight(1f))
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
            Text(noteName(acid.root, english), style = MonoStyle, color = Neon.Cyan)
            IconButton(onClick = { actions.shiftRoot(1) }) { Icon(Icons.Filled.Add, contentDescription = "Base plus aiguë") }
        }
    }
}

@Composable
private fun SoundCard(acid: AcidPattern, actions: AcidActions) {
    val values = acid.sound.values()
    val context = LocalContext.current
    var sliders by remember { mutableStateOf(SoundView.sliders(context)) }
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("Son", Modifier.weight(1f))
            listOf(false to "Panneau", true to "Curseurs").forEach { (mode, label) ->
                FilterChip(
                    selected = sliders == mode,
                    onClick = { sliders = mode; SoundView.setSliders(context, mode) },
                    label = { Text(label) },
                    modifier = Modifier.testTag(if (mode) "acid_view_sliders" else "acid_view_panel"),
                )
            }
        }
        if (!sliders) {
            Panel303(values, actions.setSound)
            return@NeonCard
        }
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

/** Remembers whether the 303's sound shows as the knob panel (default) or as sliders. */
private object SoundView {
    private const val KEY = "acid_sliders"
    fun sliders(context: Context) = context.getSharedPreferences("ui", Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun setSliders(context: Context, on: Boolean) =
        context.getSharedPreferences("ui", Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
}

// Knob layout of the panel, in rows (AcidSound value indices): the filter first, then the rest.
private val PANEL_ROWS = listOf(listOf(0, 1, 2, 3, 4), listOf(5, 6, 7, 8))
private val PANEL_LABELS = listOf("Coupure", "Réso.", "Env.", "Déclin", "Accent", "Accord", "Onde", "Satur.", "Volume")

/** The 303's sound as two compact rows of knobs. */
@Composable
internal fun Panel303(values: List<Float>, setSound: (Int, Float) -> Unit) {
    val defaults = AcidSound().values()
    val knobColors = KnobColors(
        cap = Neon.SurfaceHigh, capEdge = Neon.Outline, pointer = Neon.TextPrimary,
        track = Neon.Outline, value = Neon.Cyan, label = Neon.TextSecondary,
    )
    Column(Modifier.fillMaxWidth().testTag("acid_panel"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PANEL_ROWS.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { i ->
                    Knob(
                        value = values[i],
                        onValueChange = { setSound(i, it) },
                        label = PANEL_LABELS[i],
                        default = defaults[i],
                        size = 44.dp,
                        colors = knobColors,
                        valueText = knobText(i, values[i]),
                        modifier = Modifier.weight(1f).testTag("acid_knob_$i"),
                    )
                }
                // Same knob width on both rows.
                repeat(PANEL_ROWS[0].size - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Text("Glisse vers le haut ou le bas pour tourner ; deux touchers : réglage de départ.", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

private fun knobText(index: Int, v: Float): String = when (index) {
    5 -> "%+.1f".format((v - 0.5f) * 24f).replace('.', ',')  // tuning: ±12 semitones
    6 -> when {
        v < 0.15f -> "scie"
        v > 0.85f -> "carré"
        else -> "mélange"
    }
    else -> "${(v * 100).roundToInt()}"
}

private fun octaveMark(octave: Int) = when {
    octave > 0 -> " " + "↑".repeat(octave)
    octave < 0 -> " ↓"
    else -> ""
}

/** A one-octave mini keyboard: white keys below, sharps above, the chosen note lit. */
@Composable
private fun NoteKeys(selected: Int, english: Boolean, onPick: (Int) -> Unit) {
    val whites = listOf(0, 2, 4, 5, 7, 9, 11)
    val blacks = listOf(1 to 0, 3 to 1, 6 to 3, 8 to 4, 10 to 5)  // pitch class to the white key it follows
    BoxWithConstraints(Modifier.fillMaxWidth().height(96.dp).testTag("acid_note_keys")) {
        val keyWidth = maxWidth / 7
        whites.forEachIndexed { i, pc ->
            Box(
                Modifier
                    .offset(x = keyWidth * i)
                    .width(keyWidth)
                    .fillMaxHeight()
                    .padding(1.dp)
                    .background(if (pc == selected) Neon.Magenta else Color(0xFFF2F3F5), RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .clickable { onPick(pc) }
                    .testTag("acid_key_$pc"),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(noteName(pc, english), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF23262B), modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        blacks.forEach { (pc, after) ->
            Box(
                Modifier
                    .offset(x = keyWidth * (after + 1) - keyWidth * 0.3f)
                    .width(keyWidth * 0.6f)
                    .height(56.dp)
                    .background(if (pc == selected) Neon.Magenta else Color(0xFF23262B), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .clickable { onPick(pc) }
                    .testTag("acid_key_$pc"),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(noteName(pc, english), fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}
