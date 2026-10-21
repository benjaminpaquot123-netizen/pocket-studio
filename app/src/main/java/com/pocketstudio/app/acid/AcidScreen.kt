@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.acid

import com.pocketstudio.model.i18n.tr

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.app.help.Glossary
import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
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
import com.pocketstudio.ui.aspectRatioAtMost
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.pocketstudio.ui.ProSlider
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
import com.pocketstudio.app.sounds.SoundBrowser
import com.pocketstudio.app.sounds.SoundBrowserUi
import com.pocketstudio.model.AcidSound
import com.pocketstudio.ui.Knob
import com.pocketstudio.ui.holding
import com.pocketstudio.ui.KnobColors
import com.pocketstudio.model.AcidStep
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

/** French (Do Ré Mi) or English (C D E) name, no octave, as most tabs found online use. */
fun noteName(midi: Int, english: Boolean): String = com.pocketstudio.model.NoteNames.pitchClass(midi, english)

/** The note naming the user picked, remembered on the phone. */
/** Note names follow Réglages › Notation (also switchable from the acid grid). */
private object Notation {
    fun isEnglish(context: Context) = com.pocketstudio.app.settings.SettingsStore.load(context).englishNotes
    fun setEnglish(context: Context, on: Boolean) = com.pocketstudio.app.settings.SettingsStore.update(context) { it.copy(englishNotes = on) }
}

private val SOUND_LABELS: List<String>
    get() = listOf(tr("Coupure"), tr("Résonance"), tr("Enveloppe"), tr("Déclin"), tr("Accent"), tr("Accordage"), tr("Onde (scie → carré)"), tr("Saturation"), tr("Volume"))

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
    /** Automation recording on or off (the knobs turned while playing go into lanes). */
    val toggleRecord: () -> Unit = {},
    /** A finger on knob (or slider) [index] or off it: while held, its automation lane leaves it to the finger. */
    val holdSound: (Int, Boolean) -> Unit = { _, _ -> },
)

@Composable
fun AcidScreen(
    acid: AcidPattern,
    playing: Boolean,
    currentStep: Int,
    selected: Int,
    actions: AcidActions,
    modifier: Modifier = Modifier,
    browser: SoundBrowserUi? = null,
    /** The acid line's effects, in a card under its sound (null: none shown). */
    effects: com.pocketstudio.app.mixer.EffectsUi? = null,
    /** Knob gestures are being recorded into automation lanes. */
    recording: Boolean = false,
    /** The knobs ([AcidSound.values] indices) an automation lane moves: a line under them says so. */
    automated: Set<Int> = emptySet(),
) {
    val context = LocalContext.current
    val settings by com.pocketstudio.app.settings.SettingsStore.state.collectAsState()
    val english = settings.englishNotes
    val setEnglish: (Boolean) -> Unit = { Notation.setEnglish(context, it) }
    // On a tablet the line (generator and steps) on the left; the chosen step and the sound on the right.
    com.pocketstudio.ui.AdaptivePanes(modifier.testTag("acid"), first = {
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
                    Text(tr("  Génère-moi un pattern"), style = MaterialTheme.typography.titleSmall)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Densité"), Modifier.weight(1f))
                Text(com.pocketstudio.model.i18n.percent((acid.density * 100).roundToInt()), style = MonoStyle, color = Neon.Cyan)
            }
            ProSlider(value = acid.density, onValueChange = actions.setDensity)
        }
        StepGrid(acid, playing, currentStep, selected, actions, english, setEnglish)
    }, second = {
        StepEditor(acid, selected, actions, english)
        SoundCard(acid, actions, browser, recording, automated)
        effects?.let { com.pocketstudio.app.mixer.EffectsCard(it) }
    })
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
    val chipColors = pillColors()
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(tr("Ligne de basse · base %s", noteName(acid.root, english)), Modifier.weight(1f), help = Glossary.ACID, foldKey = "Ligne de basse")
            FilterChip(
                selected = english, onClick = { setEnglish(!english) },
                label = { Text(if (english) "C D E" else "Do Ré Mi") },
                colors = chipColors,
                modifier = Modifier.testTag("acid_notation"),
            )
        }
        Row(Modifier.fadingHorizontalScroll(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Longueur"), style = MaterialTheme.typography.bodySmall)
            AcidPattern.LENGTHS_IN_BARS.forEach { bars ->
                FilterChip(
                    selected = acid.length == bars * AcidPattern.STEPS_PER_PAGE,
                    onClick = { actions.setBars(bars) },
                    label = { Text(if (bars == 1) tr("1 mesure") else "$bars") },
                    colors = chipColors,
                    modifier = Modifier.testTag("acid_bars_$bars"),
                )
            }
        }
        if (pages > 1) {
            Row(Modifier.fadingHorizontalScroll(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Mesure"), style = MaterialTheme.typography.bodySmall)
                for (p in 0 until pages) {
                    FilterChip(
                        selected = shown == p,
                        onClick = { page = p; follow = false },
                        label = { Text("${p + 1}") },
                        colors = chipColors,
                        modifier = Modifier.testTag("acid_page_$p"),
                    )
                }
                FilterChip(selected = follow, onClick = { follow = !follow }, label = { Text(tr("Suivre")) }, colors = chipColors)
                TextButton(onClick = { actions.copyPage(shown, (shown + 1) % pages) }, modifier = Modifier.testTag("acid_copy_page")) {
                    Text(tr("Copier → %d", (shown + 1) % pages + 1))
                }
            }
        }
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 8) {
                    val i = shown * AcidPattern.STEPS_PER_PAGE + row * 8 + col
                    AcidStepCell(
                        acid, i, english, selected = i == selected, playing = i == playStep,
                        modifier = Modifier.weight(1f).clickable { actions.selectStep(i) }.testTag("acid_step_$i"),
                    )
                }
            }
        }
        Text(
            tr("Touche un pas pour le modifier. Couleur pleine = accent, ⌒ = slide, ↑ = octave au-dessus (Up), ↓ = en dessous (Down), – = silence, 🔒 = gardé par le générateur."),
            style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary,
        )
    }
}

/**
 * One step of the 303 line, as the Acide tab and the assistant draw it: its note's name (– for a
 * rest), full colour for an accent, ↑ ↓ for its octave, ⌒ for a slide, 🔒 when the generator keeps
 * it; a thicker edge when [selected] or [playing]. [modifier] carries its size and its touches.
 */
@Composable
fun AcidStepCell(acid: AcidPattern, index: Int, english: Boolean, modifier: Modifier = Modifier, selected: Boolean = false, playing: Boolean = false) {
    val s = acid.steps[index]
    val fill = when {
        !s.gate -> Neon.Surface
        s.accent -> Neon.Cyan
        else -> Neon.Cyan.copy(alpha = 0.45f)
    }
    Box(
        modifier = Modifier
            // Capped on a wide pane (tablet): the two rows of steps stay compact.
            .aspectRatioAtMost(0.7f, 76.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(fill)
            .border(
                width = if (selected || playing) 2.dp else 1.dp,
                color = when {
                    playing -> Neon.TextPrimary
                    selected -> Neon.Magenta
                    else -> Neon.Outline
                },
                shape = RoundedCornerShape(6.dp),
            )
            .then(modifier),
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
                if (index in acid.locked) append("🔒")
            }
            if (marks.isNotEmpty()) {
                Text(marks, fontSize = 11.sp, color = if (s.gate && s.accent) Neon.Background else Neon.TextSecondary)
            }
        }
    }
}

@Composable
private fun StepEditor(acid: AcidPattern, index: Int, actions: AcidActions, english: Boolean) {
    val s = acid.steps[index]
    val chipColors = pillColors()
    NeonCard(accent = Neon.Magenta) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(tr("Pas %d", index + 1), color = Neon.Magenta, help = Glossary.ACID_STEP, foldKey = "Pas de la basse acide")
            Box(Modifier.weight(1f))
            FilterChip(
                selected = index in acid.locked,
                onClick = actions.toggleLock,
                label = { Text(tr("Verrou")) },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                colors = chipColors,
            )
        }
        Text(
            if (s.gate) noteName(acid.root + s.note, english) + octaveMark(s.octave) else tr("Silence"),
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
            FilterChip(selected = s.gate, onClick = { actions.editStep { it.copy(gate = !it.gate) } }, label = { Text(tr("Note")) }, colors = chipColors)
            FilterChip(selected = s.accent, onClick = { actions.editStep { it.copy(accent = !it.accent, gate = true) } }, label = { Text(tr("Accent")) }, colors = chipColors)
            FilterChip(selected = s.slide, onClick = { actions.editStep { it.copy(slide = !it.slide, gate = true) } }, label = { Text(tr("Slide")) }, colors = chipColors)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Octave (Down ↓ / Up ↑)"), Modifier.weight(1f))
            IconButton(onClick = { actions.editStep { it.copy(octave = (it.octave - 1).coerceAtLeast(-1), gate = true) } }) {
                Icon(Icons.Filled.Remove, contentDescription = tr("Octave plus bas"))
            }
            Text(if (s.octave > 0) "+${s.octave}" else "${s.octave}", style = MonoStyle, color = Neon.Cyan)
            IconButton(onClick = { actions.editStep { it.copy(octave = (it.octave + 1).coerceAtMost(2), gate = true) } }) {
                Icon(Icons.Filled.Add, contentDescription = tr("Octave plus haut"))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Note de base"), Modifier.weight(1f))
            IconButton(onClick = { actions.shiftRoot(-1) }) { Icon(Icons.Filled.Remove, contentDescription = tr("Base plus grave")) }
            Text(noteName(acid.root, english), style = MonoStyle, color = Neon.Cyan)
            IconButton(onClick = { actions.shiftRoot(1) }) { Icon(Icons.Filled.Add, contentDescription = tr("Base plus aiguë")) }
        }
    }
}

@Composable
private fun SoundCard(acid: AcidPattern, actions: AcidActions, browser: SoundBrowserUi?, recording: Boolean = false, automated: Set<Int> = emptySet()) {
    val values = acid.sound.values()
    val context = LocalContext.current
    var sliders by remember { mutableStateOf(SoundView.sliders(context)) }
    NeonCard {
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel(tr("Son"), Modifier.weight(1f), help = Glossary.FILTER, foldKey = "Son acide")
            listOf(false to tr("Panneau"), true to tr("Curseurs")).forEach { (mode, label) ->
                FilterChip(
                    selected = sliders == mode,
                    onClick = { sliders = mode; SoundView.setSliders(context, mode) },
                    label = { Text(label) },
                    modifier = Modifier.testTag(if (mode) "acid_view_sliders" else "acid_view_panel"),
                )
            }
        }
        browser?.let { SoundBrowser(it) }
        // The knobs' moves recorded into automation lanes (the Mixer's Automation card shows them).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = recording, onClick = actions.toggleRecord,
                label = { Text(tr("Enregistrer")) },
                leadingIcon = { Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = if (recording) Neon.Red else Neon.TextSecondary) },
                colors = com.pocketstudio.ui.pillColors(Neon.Red),
                modifier = Modifier.testTag("acid_record"),
            )
            Text(
                if (recording) tr("Lance la lecture et tourne un bouton : son mouvement s'enregistre (Mixeur › Automation).")
                else tr("Enregistre tes mouvements de bouton pendant la lecture."),
                Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
        if (!sliders) {
            Panel303(values, actions.setSound, actions.holdSound)
            AutomatedHint(automated)
            return@NeonCard
        }
        // Two columns of sliders when the card is wide (tablet).
        com.pocketstudio.ui.AdaptiveColumns(SOUND_LABELS.mapIndexed { i, label ->
            @Composable {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("${(values[i] * 100).roundToInt()}", style = MonoStyle, color = Neon.Cyan)
                }
                ProSlider(
                    value = values[i],
                    onValueChange = { actions.setSound(i, it) },
                    modifier = Modifier.holding { actions.holdSound(i, it) }.testTag("acid_sound_$i"),
                )
            }
        }, minCellWidth = 240.dp)
        AutomatedHint(automated)
    }
}

/**
 * Under the 303's controls when an automation lane moves some of them: why they move on their
 * own during the song, and how to take one back (a finger on it).
 */
@Composable
private fun AutomatedHint(automated: Set<Int>) {
    if (automated.isEmpty()) return
    val names = automated.sorted().mapNotNull { SOUND_LABELS.getOrNull(it) }.joinToString(", ")
    Text(
        tr("Bouge tout seul (courbe d'automation) : %s. Garde le doigt dessus pour le reprendre.", names),
        style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("acid_automated"),
    )
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
private val PANEL_LABELS: List<String>
    get() = listOf(tr("Coupure"), tr("Réso."), tr("Env."), tr("Déclin"), tr("Accent"), tr("Accordage"), tr("Onde"), tr("Satur."), tr("Volume"))

/** The 303's sound as two compact rows of knobs. */
@Composable
internal fun Panel303(values: List<Float>, setSound: (Int, Float) -> Unit, hold: (Int, Boolean) -> Unit = { _, _ -> }) {
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
                        onHold = { hold(i, it) },
                        modifier = Modifier.weight(1f).testTag("acid_knob_$i"),
                    )
                }
                // Same knob width on both rows.
                repeat(PANEL_ROWS[0].size - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Text(tr("Glisse vers le haut ou le bas pour tourner ; deux touchers : réglage de départ."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

private fun knobText(index: Int, v: Float): String = when (index) {
    5 -> tr("%+.1f", (v - 0.5f) * 24f)  // tuning: ±12 semitones, decimal mark of the language
    6 -> when {
        v < 0.15f -> tr("scie")
        v > 0.85f -> tr("carré")
        else -> tr("mélange")
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
