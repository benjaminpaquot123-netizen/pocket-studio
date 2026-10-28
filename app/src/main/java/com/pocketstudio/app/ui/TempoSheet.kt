package com.pocketstudio.app.ui

import com.pocketstudio.ui.ScaledBottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
)

/**
 * The tempo sheet, from the top bar's tempo: the keypad (type 128, OK), the ready-made tempos (one
 * touch, and the sheet closes), ½ and ×2, and « Taper en rythme ». What it changes is heard at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoSheet(tempoBpm: Double, actions: TempoActions, onClose: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onClose() } }
    ScaledBottomSheet(onDismissRequest = onClose, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("tempo_sheet")) {
        TempoPad(tempoBpm, actions, close, Modifier.navigationBarsPadding())
    }
}

/** The sheet's content (also drawn on its own for the screenshots). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TempoPad(tempoBpm: Double, actions: TempoActions, close: () -> Unit, modifier: Modifier = Modifier) {
    var draft by rememberSaveable(stateSaver = TempoDraftSaver) { mutableStateOf(TempoDraft()) }
    val current = Math.round(tempoBpm).toInt()
    val typing = draft.digits.isNotEmpty()
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Tempo"), style = MaterialTheme.typography.titleLarge, color = Neon.TextPrimary, modifier = Modifier.weight(1f))
            TextButton(onClick = close, modifier = Modifier.testTag("tempo_close")) { Text(tr("Fermer")) }
        }
        // The tempo as typed (accent) or as it is now, between − and + (held: they repeat).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            RepeatingIconButton({ actions.set((current - 1).toDouble().coerceAtLeast(Project.MIN_TEMPO)); draft = TempoDraft() }, Modifier.testTag("tempo_minus")) {
                Icon(Icons.Filled.Remove, contentDescription = tr("Tempo moins"))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                val big = MonoStyle.copy(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                // Typed figures show as typed; the tempo itself rolls to its new value (−, +, a tap).
                if (typing) Text(draft.digits, style = big, color = Neon.Cyan, modifier = Modifier.testTag("tempo_display"))
                else RollingText("$current", style = big, color = Neon.TextPrimary, modifier = Modifier.testTag("tempo_display"))
                Text(" BPM", style = MaterialTheme.typography.titleMedium, color = Neon.TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
            }
            RepeatingIconButton({ actions.set((current + 1).toDouble().coerceAtMost(Project.MAX_TEMPO)); draft = TempoDraft() }, Modifier.testTag("tempo_plus")) {
                Icon(Icons.Filled.Add, contentDescription = tr("Tempo plus"))
            }
        }
        if (typing && draft.bpm == null) {
            Text(
                tr("Entre %1\$d et %2\$d", Project.MIN_TEMPO.toInt(), Project.MAX_TEMPO.toInt()), style = MaterialTheme.typography.bodyMedium,
                color = Neon.Amber, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
        // One touch: the tempo, and the sheet closes. ½ and ×2 stay open (to try one after the other).
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
                selected = false, enabled = half != null, onClick = { half?.let { actions.set(it.toDouble()); draft = TempoDraft() } },
                label = { Text("½", style = MaterialTheme.typography.titleMedium) }, colors = pillColors(),
                modifier = Modifier.semantics { contentDescription = tr("Moitié du tempo") }.testTag("tempo_half"),
            )
            FilterChip(
                selected = false, enabled = double != null, onClick = { double?.let { actions.set(it.toDouble()); draft = TempoDraft() } },
                label = { Text("×2", style = MaterialTheme.typography.titleMedium) }, colors = pillColors(),
                modifier = Modifier.semantics { contentDescription = tr("Double du tempo") }.testTag("tempo_double"),
            )
        }
        // The keypad: 1 to 9, then erase, 0, OK.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { d -> Key("$d", "tempo_key_$d", Modifier.weight(1f)) { draft = draft.type(d) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Key(null, "tempo_key_erase", Modifier.weight(1f), enabled = typing, description = tr("Effacer un chiffre")) { draft = draft.erase() }
                Key("0", "tempo_key_0", Modifier.weight(1f)) { draft = draft.type(0) }
                Key(tr("OK"), "tempo_key_ok", Modifier.weight(1f), enabled = draft.bpm != null, accent = true) {
                    draft.bpm?.let { actions.set(it.toDouble()) }
                    close()
                }
            }
        }
        // Tap on the beat: from the second tap the tempo follows (the sheet stays open).
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp))
                .border(1.dp, Neon.Outline, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) { draft = TempoDraft(); actions.tap() }
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
}

/** A keypad key: a digit, ⌫ ([label] null) or OK ([accent]). */
@Composable
private fun Key(label: String?, tag: String, modifier: Modifier, enabled: Boolean = true, accent: Boolean = false, description: String? = null, onClick: () -> Unit) {
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
        modifier.height(52.dp).widthIn(min = 64.dp).pressScale(enabled).clip(shape).background(bg).border(1.dp, Color.White.copy(alpha = 0.06f), shape)
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
