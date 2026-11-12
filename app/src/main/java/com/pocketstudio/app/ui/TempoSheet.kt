package com.pocketstudio.app.ui

import com.pocketstudio.ui.ScaledBottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.RollingText
import com.pocketstudio.ui.pressScale
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import kotlinx.coroutines.launch

/** Ready-made tempos: hip-hop, boom-bap / reggaeton, house, trap / dubstep, drum and bass. */
val TEMPO_PRESETS = listOf(70, 90, 120, 140, 174)

/** What the keypad has typed: up to 3 digits; [bpm] once it is a tempo the app plays (40 to 300). */
@Immutable
data class TempoDraft(val digits: String = "") {
    fun type(digit: Int): TempoDraft = if (digits.length >= 3 || (digits.isEmpty() && digit == 0)) this else TempoDraft(digits + digit)
    fun erase(): TempoDraft = TempoDraft(digits.dropLast(1))
    val bpm: Int? get() = digits.toIntOrNull()?.takeIf { it >= Project.MIN_TEMPO && it <= Project.MAX_TEMPO }
}

/** Half or double [bpm] (rounded), null when it would leave the tempos the app plays. */
fun scaledTempo(bpm: Double, factor: Double): Int? =
    Math.round(bpm * factor).toInt().takeIf { it >= Project.MIN_TEMPO && it <= Project.MAX_TEMPO }

class TempoActions(
    /** A tempo chosen (heard at once; the whole sheet is one undo step). */
    val set: (Double) -> Unit = {},
    /** « Taper en rythme »: one tap. */
    val tap: () -> Unit = {},
    /** The sound the tempo was set on (step 4e): « Revenir au tempo de Boys » while the tempo is elsewhere. */
    val lead: com.pocketstudio.model.TempoLead? = null,
    val backToLead: () -> Unit = {},
)

/**
 * The tempo sheet, from the top bar's tempo: the keypad (type 128, OK), the ready-made tempos (one
 * touch, and the sheet closes), ½ and ×2, and « Taper en rythme ». What it changes is heard at once.
 * Hands-on audit G10: the title, the tempo and « Fermer » stay at the top while the rest scrolls (130 %);
 * on a phone lying down the keypad and the rest sit side by side, OK on screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoSheet(tempoBpm: Double, actions: TempoActions, onClose: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onClose() } }
    var draft by rememberSaveable(stateSaver = TempoDraftSaver) { mutableStateOf(TempoDraft()) }
    val typed: (TempoDraft) -> Unit = { draft = it }
    ScaledBottomSheet(
        onDismissRequest = onClose, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("tempo_sheet"), scrolls = true,
        header = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TempoTop(tempoBpm, draft, typed, actions, close)
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = if (lyingDown()) 8.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { TempoRest(tempoBpm, draft, typed, actions, close) }
    }
}

/** The sheet's content (also drawn on its own for the screenshots). */
@Composable
fun TempoPad(tempoBpm: Double, actions: TempoActions, close: () -> Unit, modifier: Modifier = Modifier) {
    var draft by rememberSaveable(stateSaver = TempoDraftSaver) { mutableStateOf(TempoDraft()) }
    val typed: (TempoDraft) -> Unit = { draft = it }
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TempoTop(tempoBpm, draft, typed, actions, close)
        TempoRest(tempoBpm, draft, typed, actions, close)
    }
}

/** A phone lying down (or a short window): little height, room in width. */
@Composable
private fun lyingDown(): Boolean = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp && it.screenHeightDp < LYING_MAX_HEIGHT_DP }

private const val LYING_MAX_HEIGHT_DP = 480

/** What stays at the top: the title, its « ? », « Fermer », and the tempo between − and + (one row when lying down). */
@Composable
private fun TempoTop(tempoBpm: Double, draft: TempoDraft, typed: (TempoDraft) -> Unit, actions: TempoActions, close: () -> Unit) {
    val current = Math.round(tempoBpm).toInt()
    val lying = lyingDown()
    val title: @Composable () -> Unit = {
        Text(tr("Tempo"), style = MaterialTheme.typography.titleLarge, color = Neon.TextPrimary)
        com.pocketstudio.ui.HelpDot(com.pocketstudio.app.help.ScreenHelp.TEMPO_SET, about = listOf("tempo_tap", "tempo_display", "tempo_half", "tempo_double"))
    }
    val closeButton: @Composable () -> Unit = { TextButton(onClick = close, modifier = Modifier.testTag("tempo_close")) { Text(tr("Fermer")) } }
    // The tempo as typed (accent) or as it is now, between − and + (held: they repeat).
    val value: @Composable RowScope.(Modifier) -> Unit = { m ->
        RepeatingIconButton({ actions.set((current - 1).toDouble().coerceAtLeast(Project.MIN_TEMPO)); typed(TempoDraft()) }, Modifier.testTag("tempo_minus")) {
            Icon(Icons.Filled.Remove, contentDescription = tr("Tempo moins"))
        }
        Row(m, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            val big = MonoStyle.copy(fontSize = if (lying) 36.sp else 44.sp, fontWeight = FontWeight.ExtraBold)
            // Typed figures show as typed; the tempo itself rolls to its new value (−, +, a tap).
            if (draft.digits.isNotEmpty()) Text(draft.digits, style = big, color = Neon.Cyan, modifier = Modifier.testTag("tempo_display"))
            else RollingText("$current", style = big, color = Neon.TextPrimary, modifier = Modifier.testTag("tempo_display"))
            Text(" BPM", style = MaterialTheme.typography.titleMedium, color = Neon.TextSecondary, modifier = Modifier.padding(bottom = if (lying) 6.dp else 8.dp))
        }
        RepeatingIconButton({ actions.set((current + 1).toDouble().coerceAtMost(Project.MAX_TEMPO)); typed(TempoDraft()) }, Modifier.testTag("tempo_plus")) {
            Icon(Icons.Filled.Add, contentDescription = tr("Tempo plus"))
        }
    }
    if (lying) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            title()
            value(Modifier.weight(1f))
            closeButton()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            title()
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            closeButton()
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { value(Modifier.weight(1f)) }
    }
}

/** What may scroll: the ready-made tempos, the way back to the sound's tempo, the keypad and « Taper en rythme ». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TempoRest(tempoBpm: Double, draft: TempoDraft, typed: (TempoDraft) -> Unit, actions: TempoActions, close: () -> Unit) {
    val current = Math.round(tempoBpm).toInt()
    val typing = draft.digits.isNotEmpty()
    val outOfRange: @Composable () -> Unit = {
        if (typing && draft.bpm == null) {
            Text(
                tr("Entre %1\$d et %2\$d", Project.MIN_TEMPO.toInt(), Project.MAX_TEMPO.toInt()), style = MaterialTheme.typography.bodyMedium,
                color = Neon.Amber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    // One touch: the tempo, and the sheet closes. ½ and ×2 stay open (to try one after the other).
    val presets: @Composable () -> Unit = {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TEMPO_PRESETS.forEach { bpm ->
                FilterChip(
                    selected = current == bpm, onClick = { actions.set(bpm.toDouble()); close() },
                    label = { Text("$bpm", style = MonoStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)) },
                    colors = pillColors(), modifier = Modifier.testTag("tempo_preset_$bpm"),
                )
            }
            val half = scaledTempo(tempoBpm, 0.5)
            val double = scaledTempo(tempoBpm, 2.0)
            FilterChip(
                selected = false, enabled = half != null, onClick = { half?.let { actions.set(it.toDouble()); typed(TempoDraft()) } },
                label = { Text("½", style = MaterialTheme.typography.titleMedium) }, colors = pillColors(),
                modifier = Modifier.semantics { contentDescription = tr("Moitié du tempo") }.testTag("tempo_half"),
            )
            FilterChip(
                selected = false, enabled = double != null, onClick = { double?.let { actions.set(it.toDouble()); typed(TempoDraft()) } },
                label = { Text("×2", style = MaterialTheme.typography.titleMedium) }, colors = pillColors(),
                modifier = Modifier.semantics { contentDescription = tr("Double du tempo") }.testTag("tempo_double"),
            )
        }
    }
    // Step 4e: back to the tempo of the sound that set it, in one touch (its exact tempo).
    val backToLead: @Composable () -> Unit = {
        val lead = actions.lead
        if (lead != null && kotlin.math.abs(lead.bpm - tempoBpm) >= 0.05) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                    .background(Neon.Cyan.copy(alpha = 0.12f))
                    .border(1.dp, Neon.Cyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { typed(TempoDraft()); actions.backToLead() }
                    .padding(horizontal = 16.dp)
                    .testTag("tempo_back_to_lead"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    tr("Revenir au tempo de %1\$s (%2\$d)", com.pocketstudio.model.DefaultNames.shownSample(lead.name), Math.round(lead.bpm).toInt()),
                    style = MaterialTheme.typography.titleSmall, color = Neon.Cyan, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
    val lying = lyingDown()
    // The keypad: 1 to 9, then erase, 0, OK.
    val keypad: @Composable (Modifier) -> Unit = { m ->
        val keyHeight = if (lying) 48.dp else 52.dp
        Column(m, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { d -> Key("$d", "tempo_key_$d", Modifier.weight(1f), keyHeight) { typed(draft.type(d)) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Key(null, "tempo_key_erase", Modifier.weight(1f), keyHeight, enabled = typing, description = tr("Effacer un chiffre")) { typed(draft.erase()) }
                Key("0", "tempo_key_0", Modifier.weight(1f), keyHeight) { typed(draft.type(0)) }
                Key(tr("OK"), "tempo_key_ok", Modifier.weight(1f), keyHeight, enabled = draft.bpm != null, accent = true) {
                    draft.bpm?.let { actions.set(it.toDouble()) }
                    close()
                }
            }
        }
    }
    // Tap on the beat: from the second tap the tempo follows (the sheet stays open).
    val tap: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp))
                .border(1.dp, Neon.Outline, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) { typed(TempoDraft()); actions.tap() }
                .padding(horizontal = 16.dp)
                .testTag("tempo_tap"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.TouchApp, contentDescription = null, tint = Neon.Cyan)
            Column(Modifier.padding(start = 10.dp)) {
                Text(tr("Taper en rythme"), style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary)
                Text(tr("Touche ici à chaque temps : le tempo suit."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
        }
    }
    if (lying) {
        // Side by side: the keypad on the left, the rest on the right (rule 22 of docs/DESIGN.md).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            keypad(Modifier.weight(1f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                outOfRange()
                presets()
                backToLead()
                tap()
            }
        }
    } else {
        outOfRange()
        presets()
        backToLead()
        keypad(Modifier)
        tap()
    }
}

/** A keypad key: a digit, ⌫ ([label] null) or OK ([accent]). */
@Composable
private fun Key(label: String?, tag: String, modifier: Modifier, height: androidx.compose.ui.unit.Dp, enabled: Boolean = true, accent: Boolean = false, description: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val bg = when {
        accent && enabled -> Neon.Cyan
        else -> Neon.SurfaceHigh
    }
    val fg = when {
        !enabled -> Neon.TextSecondary.copy(alpha = 0.4f)
        accent -> Neon.OnAccent
        else -> Neon.TextPrimary
    }
    Box(
        modifier.height(height).widthIn(min = 64.dp).pressScale(enabled).clip(shape).background(bg).border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (label == null) Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        else Text(label, style = MonoStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = fg)
    }
}

private val TempoDraftSaver = androidx.compose.runtime.saveable.Saver<TempoDraft, String>(save = { it.digits }, restore = { TempoDraft(it) })

/** An icon button that repeats while held (after a short wait, faster and faster): tempo − and +. */
@Composable
internal fun RepeatingIconButton(onStep: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val step by rememberUpdatedState(onStep)
    // Set once the hold has started repeating: the release then adds no extra step.
    var repeated by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        repeated = false
        kotlinx.coroutines.delay(REPEAT_AFTER_MS)
        repeated = true
        var wait = REPEAT_FIRST_MS
        while (true) {
            step()
            kotlinx.coroutines.delay(wait)
            wait = (wait - 15).coerceAtLeast(REPEAT_FASTEST_MS)
        }
    }
    IconButton(onClick = { if (!repeated) step(); repeated = false }, interactionSource = interaction, modifier = modifier) { content() }
}

private const val REPEAT_AFTER_MS = 450L
private const val REPEAT_FIRST_MS = 140L
private const val REPEAT_FASTEST_MS = 50L
