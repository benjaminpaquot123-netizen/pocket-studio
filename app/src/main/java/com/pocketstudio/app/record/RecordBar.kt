package com.pocketstudio.app.record

import com.pocketstudio.app.learn.spot
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.i18n.percent
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.WholeWordsText
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt

/** What an editor's record bar shows. */
class RecordBarUi(
    /** The take recording or just done (null: none, the bar offers « ● Enregistrer »). */
    val take: NoteTakeUi?,
    /** The button's words in this editor (« Enregistrer ma mélodie »). */
    val label: String,
    /** The take's counter (« Couplet · mes. 5/16 »), read only where it is shown. */
    val counter: () -> String? = { null },
    /** Something was played lately: « Garder ce que je viens de jouer » shows. */
    val canKeepPlayed: Boolean = false,
    /** False while another recording (the microphone, the Live page) is on. */
    val enabled: Boolean = true,
    /** The last take ended with nothing played: said under the button. */
    val empty: Boolean = false,
)

class RecordBarActions(
    /** ● Enregistrer, and ■ Arrêter during the take (also during its count-in). */
    val record: () -> Unit = {},
    val keep: () -> Unit = {},
    val inTime: (Float) -> Unit = {},
    val asPlayed: () -> Unit = {},
    val redo: () -> Unit = {},
    val keepPlayed: () -> Unit = {},
)

/**
 * « ● Enregistrer » where the music is played (Clavier, Notes, 303, Rythme): one big red button;
 * then the count-in in big (« 4 · 3 · 2 · 1 »), the take's counter (« Couplet · mes. 5/16 ») and
 * ■ Arrêter; then the choice that follows in one touch: Garder, Remettre en rythme (the strength
 * and « Comme joué » under it once chosen), Refaire. [compact]: one line less (lying down).
 */
@Composable
fun RecordBar(ui: RecordBarUi, actions: RecordBarActions, modifier: Modifier = Modifier, compact: Boolean = false) {
    val take = ui.take
    when {
        take == null -> IdleBar(ui, actions, modifier, compact)
        take.phase == TakePhase.DONE -> DoneCard(take, actions, modifier)
        else -> RunningBar(ui, take, actions, modifier)
    }
}

@Composable
private fun IdleBar(ui: RecordBarUi, actions: RecordBarActions, modifier: Modifier, compact: Boolean) {
    Column(modifier.fillMaxWidth()) {
    if (ui.empty) {
        Text(
            tr("Rien n'a été joué pendant la prise."), style = MaterialTheme.typography.bodySmall, color = Neon.Amber,
            modifier = Modifier.padding(bottom = 4.dp).testTag("take_empty"),
        )
    }
    Row(Modifier.fillMaxWidth().testTag("record_bar"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = actions.record, enabled = ui.enabled,
            colors = ButtonDefaults.buttonColors(containerColor = Neon.Red, contentColor = Color.White),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            modifier = Modifier.weight(1f).heightIn(min = if (compact) 48.dp else 56.dp).spot("record_start", tr("Enregistrer ici")).testTag("record_start"),
        ) {
            Icon(Icons.Filled.FiberManualRecord, contentDescription = null, Modifier.size(18.dp))
            WholeWordsText(" " + ui.label, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        if (ui.canKeepPlayed) {
            OutlinedButton(
                onClick = actions.keepPlayed, enabled = ui.enabled,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.weight(1f).heightIn(min = if (compact) 48.dp else 56.dp).testTag("keep_played"),
            ) {
                Icon(Icons.Filled.History, contentDescription = null, Modifier.size(18.dp))
                WholeWordsText(" " + tr("Garder ce que je viens de jouer"), textAlign = TextAlign.Center)
            }
        }
    }
    }
}

@Composable
private fun RunningBar(ui: RecordBarUi, take: NoteTakeUi, actions: RecordBarActions, modifier: Modifier) {
    val counting = take.phase == TakePhase.COUNT_IN
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(Neon.Red.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 4.dp).testTag("record_running"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (counting) {
            // Big, where the eyes are: the beats left before the take.
            Text(
                "${take.beatsLeft}", style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp), color = Neon.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 40.dp).testTag("record_count").semantics { liveRegion = LiveRegionMode.Polite },
            )
            Column(Modifier.weight(1f)) {
                Text(tr("Prêt ?"), style = MaterialTheme.typography.titleMedium)
                Text(tr("La prise commence après le décompte."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 3)
            }
        } else {
            RecDot()
            Column(Modifier.weight(1f)) {
                Text(tr("Enregistrement"), style = MaterialTheme.typography.titleMedium, color = Neon.Red)
                Counter(ui.counter)
            }
        }
        Button(
            onClick = actions.record,
            colors = ButtonDefaults.buttonColors(containerColor = Neon.Red, contentColor = Color.White),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
            modifier = Modifier.heightIn(min = 48.dp).testTag("record_stop"),
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null, Modifier.size(18.dp))
            Text(" " + tr("Arrêter"), maxLines = 1)
        }
    }
}

/** The part and the bar, read where shown (it changes once a bar). */
@Composable
private fun Counter(counter: () -> String?) {
    val text = counter() ?: return
    Text(text, style = MonoStyle, color = Neon.TextPrimary, maxLines = 2, modifier = Modifier.testTag("record_counter"))
}

@Composable
private fun RecDot() {
    val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "rec-dot",
    )
    Box(Modifier.size(16.dp).alpha(pulse).clip(CircleShape).background(Neon.Red))
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DoneCard(take: NoteTakeUi, actions: RecordBarActions, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Neon.SurfaceHigh)
            .padding(10.dp).testTag("take_done"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(take.summary(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("take_summary"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = actions.keep, contentPadding = ButtonDefaults.TextButtonContentPadding, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("take_keep")) {
                WholeWordsText(tr("Garder"), textAlign = TextAlign.Center)
            }
            if (take.canPutInTime) {
                val inTime = take.strength != null
                OutlinedButton(
                    onClick = { actions.inTime(1f) },
                    // The way on: in the accent, filled once done.
                    colors = if (inTime) ButtonDefaults.outlinedButtonColors(containerColor = Neon.Cyan.copy(alpha = 0.22f), contentColor = Neon.TextPrimary)
                    else ButtonDefaults.outlinedButtonColors(contentColor = Neon.Cyan),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    modifier = Modifier.weight(1.3f).heightIn(min = 48.dp).testTag("take_in_time")
                        .semantics { if (inTime) contentDescription = tr("Remise en rythme") },
                ) {
                    WholeWordsText((if (inTime) "✓ " else "") + tr("Remettre en rythme"), textAlign = TextAlign.Center)
                }
            }
            OutlinedButton(onClick = actions.redo, colors = ButtonDefaults.outlinedButtonColors(contentColor = Neon.TextPrimary), contentPadding = ButtonDefaults.TextButtonContentPadding, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("take_redo")) {
                WholeWordsText(if (take.source == TakeSource.CAPTURE) tr("Enlever") else tr("Refaire"), textAlign = TextAlign.Center)
            }
        }
        if (take.tuned) {
            // For pros: how far onto the sixteenths, and back as played (nothing was lost).
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val chip = pillColors()
                FilterChip(selected = take.strength == null, onClick = actions.asPlayed, label = { Text(tr("Comme joué")) }, colors = chip, modifier = Modifier.testTag("take_as_played"))
                IN_TIME_STRENGTHS.forEach { k ->
                    val pct = (k * 100).roundToInt()
                    FilterChip(
                        selected = take.strength == k, onClick = { actions.inTime(k) }, label = { Text(percent(pct)) }, colors = chip,
                        modifier = Modifier.testTag("take_strength_$pct"),
                    )
                }
            }
        }
    }
}

/** The record bar of an editor, wired to the ViewModel: [label] on its button, takes of [source]. */
@Composable
fun recordBarUi(vm: com.pocketstudio.app.rhythm.RhythmViewModel, label: String, source: TakeSource): RecordBarUi {
    val take by vm.noteTake.collectAsStateWithLifecycle()
    val empty by vm.takeEmpty.collectAsStateWithLifecycle()
    val canKeep by vm.canKeepPlayed.collectAsStateWithLifecycle()
    val mic by vm.audioRecording.collectAsStateWithLifecycle()
    val live by vm.take.collectAsStateWithLifecycle()
    val project by vm.project.collectAsStateWithLifecycle()
    val session by vm.sessionUi.collectAsStateWithLifecycle()
    // Changes once a bar at most, read only by the counter.
    val bar = vm.playBar.collectAsStateWithLifecycle()
    return RecordBarUi(
        take, label,
        counter = { takeCounter(project, session.shownSongIndex, bar.value) },
        // The 303's take records its knobs: nothing played on keys or pads to keep there.
        canKeepPlayed = canKeep && source != TakeSource.ACID,
        enabled = mic == null && !live.running,
        empty = empty,
    )
}

fun recordBarActions(vm: com.pocketstudio.app.rhythm.RhythmViewModel, source: TakeSource) = RecordBarActions(
    record = { vm.recordTake(source) }, keep = vm::keepNoteTake, inTime = vm::takeInTime, asPlayed = vm::takeAsPlayed,
    redo = vm::redoTake, keepPlayed = vm::keepPlayed,
)
