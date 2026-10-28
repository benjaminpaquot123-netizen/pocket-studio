package com.pocketstudio.app.mixer

import com.pocketstudio.app.learn.spot
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.rhythm.AloneListen
import com.pocketstudio.app.rhythm.AloneTarget
import com.pocketstudio.app.ui.SimpleTrack
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.guide.GuideTrack
import com.pocketstudio.model.guide.MixTip
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.fadingHorizontalScroll
import kotlin.math.log10

/** What the Mode simple's « Mix » tab shows: the project's name, its tracks (the drums with their sounds), the master, the tip. */
class SimpleMixUi(
    val projectName: String,
    val tracks: List<SimpleTrack>,
    /** Each drum sound's own level, by pad (its strip's fader). */
    val padVolumes: Map<Int, Float>,
    val masterDb: Float,
    /** « ▶ Partie en boucle » is playing. */
    val looping: Boolean,
    val listening: AloneListen?,
    /** The most urgent tip (null: nothing to say). */
    val tip: MixTip?,
)

class SimpleMixActions(
    val setVolume: (Int, Float) -> Unit = { _, _ -> },
    /** A fader touched twice: 0 dB (its own undo step). */
    val resetVolume: (Int) -> Unit = {},
    val toggleMute: (Int) -> Unit = {},
    val setMasterDb: (Float) -> Unit = {},
    /** « ▶ Écouter seul » of a track (its song key) or of a drum sound (its pad). */
    val listenAlone: (String) -> Unit = {},
    val listenPadAlone: (Int) -> Unit = {},
    val holdAlone: () -> (() -> Unit) = { {} },
    /** « ▶ Partie en boucle » (again: stop). */
    val loop: () -> Unit = {},
    /** « ↺ Tout à zéro »: back to the assistant's levels. */
    val resetAll: () -> Unit = {},
    val applyTip: (MixTip) -> Unit = {},
    /** The drum sounds shown with a meter (empty: none). */
    val meterPads: (Set<Int>) -> Unit = {},
) {
    companion object {
        /** [AloneListen.from] of a sound heard alone from the Mix tab. */
        const val FROM = "mix"
    }
}

/** The output in words, from the master's measures: what the « Sortie » strip says. */
enum class OutputWords { NONE, LOW, GOOD, LOUD, CLIP }

/** Under this short-term loudness (LUFS) the song is « Trop faible »; over [LOUD_LUFS] « Fort et propre ». */
internal const val LOW_LUFS = -18f
internal const val LOUD_LUFS = -10f

/**
 * [loudness] and [meters] as the engine gives them: « Ça sature » when the limiter takes more than
 * 6 dB off or the master goes in 6 dB over full scale, else by the short-term loudness.
 */
fun outputWords(loudness: FloatArray, meters: FloatArray): OutputWords {
    val limiter = meters.getOrElse(NativeEngine.METER_LIMITER) { 1f }
    val into = meters.getOrElse(NativeEngine.METER_MASTER_IN) { 0f }
    if (limiter < 0.5f || into > 2f) return OutputWords.CLIP
    val lufs = loudness.getOrElse(NativeEngine.LOUDNESS_SHORT_TERM) { NativeEngine.LOUDNESS_NONE }
    return when {
        !lufs.isFinite() || lufs <= NativeEngine.LOUDNESS_NONE + 1f -> OutputWords.NONE
        lufs < LOW_LUFS -> OutputWords.LOW
        lufs < LOUD_LUFS -> OutputWords.GOOD
        else -> OutputWords.LOUD
    }
}

internal fun OutputWords.text(): String = when (this) {
    OutputWords.NONE -> tr("Lance ▶")
    OutputWords.LOW -> tr("Trop faible")
    OutputWords.GOOD -> tr("Bien")
    OutputWords.LOUD -> tr("Fort et propre")
    OutputWords.CLIP -> tr("Ça sature")
}

/** A strip's fader position (0..1, 0.8 = 0 dB) shown on the scale; the master's dB put on the same marks (+6 top, 0, −12, −24 bottom). */
internal fun masterPosition(db: Float): Float = if (db >= 0f) 0.8f + db / 6f * 0.2f else (0.8f + db / 12f * 0.4f).coerceAtLeast(0f)

internal fun masterDbAt(pos: Float): Float = if (pos >= 0.8f) (pos - 0.8f) / 0.2f * 6f else (pos - 0.8f) / 0.4f * 12f

/** A meter's height (0..1) for a peak: 0 dBFS on the « 0 » mark, 48 dB under it the bottom. */
internal fun meterPosition(peak: Float): Float {
    if (peak <= 0f || !peak.isFinite()) return 0f
    val db = 20f * log10(peak)
    return if (db >= 0f) (0.8f + db / 6f * 0.2f).coerceAtMost(1f) else ((db + 48f) / 48f * 0.8f).coerceAtLeast(0f)
}

/**
 * The Mode simple's « Mix » tab (owner's mock-up « 1A, en version pro »): « Mix · projet », the
 * loop and « Tout à zéro », a line « À toi », every track's strip side by side (its colour, a big
 * fader with its dB marks and its live meter, M and ▶ alone), the drums opening on their sounds,
 * the « Sortie » saying in words how loud it is, one tip at a time with its button.
 * The meters are read only when drawn: a reading recomposes nothing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimpleMixScreen(ui: SimpleMixUi, meters: () -> FloatArray, loudness: () -> FloatArray, actions: SimpleMixActions) {
    // Closed at first: every track's strip fits on a phone; open, the drums show each sound.
    var drumsOpen by rememberSaveable { mutableStateOf(false) }
    val alone = ui.listening?.target
    com.pocketstudio.app.rhythm.HoldAlone(ui.listening?.from == SimpleMixActions.FROM, actions.holdAlone)
    val drums = ui.tracks.firstOrNull { it.key == com.pocketstudio.model.SongPart.DRUMS }
    val shownPads = if (drumsOpen) drums?.pads.orEmpty().map { it.lane }.toSet() else emptySet()
    val meterPads by rememberUpdatedState(actions.meterPads)
    DisposableEffect(shownPads) {
        meterPads(shownPads)
        onDispose { meterPads(emptySet()) }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp).testTag("simple_mix"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FlowRow(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.align(Alignment.CenterVertically).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Mix"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(" · " + ui.projectName, style = MaterialTheme.typography.titleSmall, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Pill(if (ui.looping) "■ " + tr("Arrêter") else "▶ " + tr("Partie en boucle"), filled = true, tag = "mix_loop", selected = ui.looping, onClick = actions.loop)
            Pill("↺ " + tr("Tout à zéro"), filled = false, tag = "mix_reset", onClick = actions.resetAll)
        }
        Text(
            tr("À toi : monte ce qui doit s'entendre, baisse ce qui gêne. Touche ▶ pour écouter une piste seule."),
            style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Neon.Cyan.copy(alpha = 0.12f))
                .drawStartBar(Neon.Cyan).padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp).testTag("mix_hint"),
        )
        Row(Modifier.fillMaxWidth().fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ui.tracks.forEach { t ->
                val trackAlone = alone != null && alone == AloneTarget.ofSongKey(t.key)
                if (t.pads.isNotEmpty()) {
                    DrumsGroup(t, drumsOpen, { drumsOpen = !drumsOpen }, trackAlone, ui.padVolumes, alone, meters, actions)
                } else {
                    MixStrip(t, trackAlone, meters, actions)
                }
            }
            OutputStrip(ui.masterDb, meters, loudness, actions.setMasterDb)
        }
        TipCard(ui.tip, actions.applyTip)
    }
}

/** A pill of the header: the main one filled with the accent, the other quiet. */
@Composable
private fun Pill(text: String, filled: Boolean, tag: String, selected: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.heightIn(min = 48.dp).clip(shape).clickable(role = Role.Button, onClick = onClick).semantics { this.selected = selected }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1,
            color = if (filled) Neon.OnAccent else Neon.TextPrimary,
            modifier = Modifier.clip(shape).background(if (filled) Neon.Cyan else Neon.SurfaceHigh).padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** A thin bar of [color] on the start side (the hint's edge), right in right-to-left languages. */
private fun Modifier.drawStartBar(color: Color): Modifier = this.drawBehind {
    val w = 3.dp.toPx()
    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - w else 0f
    drawRect(color, Offset(x, 0f), Size(w, size.height))
}

private val STRIP_W = 76.dp
private val FADER_H = 184.dp
private val MINI_W = 60.dp
private val MINI_FADER_H = 120.dp

/** The coloured head of a strip: its name over its colour. */
@Composable
private fun StripHead(name: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.35f))
            .drawBehind { drawRect(color, Offset.Zero, Size(size.width, 3.dp.toPx())) }.padding(horizontal = 2.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

/** One track's strip: head, fader and meter, the exact dB, M and ▶. */
@Composable
private fun MixStrip(t: SimpleTrack, alone: Boolean, meters: () -> FloatArray, actions: SimpleMixActions, head: Boolean = true) {
    Column(
        Modifier.width(STRIP_W).clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh.copy(alpha = 0.45f)).padding(6.dp)
            .spot("mix_strip_${t.ch}").testTag("mix_strip_${t.ch}"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (head) StripHead(t.name, t.color)
        StripBody(t, alone, meters, actions)
    }
}

@Composable
private fun StripBody(t: SimpleTrack, alone: Boolean, meters: () -> FloatArray, actions: SimpleMixActions) {
    MixFader(
        t.volume, { actions.setVolume(t.ch, it) }, { actions.resetVolume(t.ch) }, { meters().getOrElse(t.ch) { 0f } },
        tr("Volume de %s", t.name), faderDb(t.volume), "mix_fader_${t.ch}", top = "+4", bottom = "−∞", height = FADER_H,
    )
    Text(faderDb(t.volume), style = MonoStyle.copy(fontSize = 12.sp), color = Neon.TextPrimary, maxLines = 1, modifier = Modifier.testTag("mix_db_${t.ch}"))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SquareToggle("M", t.mute, Neon.Amber, tr("Couper la piste %s", t.name), "mix_mute_${t.ch}") { actions.toggleMute(t.ch) }
        PlayAlone(alone, t.name, "mix_alone_${t.key}") { actions.listenAlone(t.key) }
    }
}

/** The drums: their strip, and (open) one small strip per sound, each with its level, meter and ▶. */
@Composable
private fun DrumsGroup(
    t: SimpleTrack, open: Boolean, toggle: () -> Unit, alone: Boolean, padVolumes: Map<Int, Float>, target: AloneTarget?,
    meters: () -> FloatArray, actions: SimpleMixActions,
) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh.copy(alpha = 0.45f)).padding(6.dp).testTag("mix_strip_${t.ch}"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.width(if (open) STRIP_W + (MINI_W + 4.dp) * t.pads.size else STRIP_W - 12.dp).heightIn(min = 40.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(role = Role.Button, onClickLabel = if (open) tr("Replier") else tr("Voir ses sons"), onClick = toggle)
                .semantics { stateDescription = if (open) tr("ouvert") else tr("fermé") }
                .testTag("mix_drums_expand"),
        ) {
            StripHead(t.name + if (open) " ▾" else " ▸", t.color)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(Modifier.width(STRIP_W), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StripBody(t, alone, meters, actions)
            }
            if (open) t.pads.forEach { pad ->
                val ch = Mixer.padChannel(pad.lane)
                val v = padVolumes[pad.lane] ?: Mixer.DEFAULT_VOLUME
                val on = target != null && target.channels.isEmpty() && target.pads == setOf(pad.lane)
                Column(
                    Modifier.width(MINI_W).clip(RoundedCornerShape(8.dp)).background(Neon.Surface.copy(alpha = 0.7f)).padding(vertical = 4.dp).testTag("mix_pad_${pad.lane}"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(pad.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 12.sp, modifier = Modifier.heightIn(min = 26.dp))
                    MixFader(
                        v, { actions.setVolume(ch, it) }, { actions.resetVolume(ch) }, { meters().getOrElse(ch) { 0f } },
                        tr("Volume de %s", pad.name), faderDb(v), "mix_pad_fader_${pad.lane}", top = null, bottom = null, height = MINI_FADER_H, mini = true,
                    )
                    Text(faderDb(v).replace(" dB", ""), style = MonoStyle.copy(fontSize = 11.sp), color = Neon.TextSecondary, maxLines = 1)
                    PlayAlone(on, pad.name, "mix_pad_alone_${pad.lane}") { actions.listenPadAlone(pad.lane) }
                }
            }
        }
    }
}

/** « Sortie »: the master's level on the same marks, its meter, and in words how loud it is. */
@Composable
private fun OutputStrip(masterDb: Float, meters: () -> FloatArray, loudness: () -> FloatArray, setDb: (Float) -> Unit) {
    val words by remember { derivedStateOf { outputWords(loudness(), meters()) } }
    Column(
        Modifier.width(STRIP_W + 8.dp).clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh.copy(alpha = 0.7f))
            .border(1.dp, Neon.Outline, RoundedCornerShape(12.dp)).padding(6.dp).testTag("mix_out"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StripHead(tr("Sortie"), Neon.TextSecondary)
        val db = com.pocketstudio.model.LoudnessTargets.format(masterDb.toDouble()) + " dB"
        MixFader(
            masterPosition(masterDb), { setDb((masterDbAt(it) * 2f).let(Math::round) / 2f) }, { setDb(0f) }, { meters().getOrElse(Mixer.MASTER) { 0f } },
            tr("Volume de sortie"), db, "mix_out_fader", top = "+6", bottom = "−24", height = FADER_H,
        )
        val color = when (words) {
            OutputWords.CLIP -> Neon.RedText
            OutputWords.LOW -> Neon.Amber
            OutputWords.NONE -> Neon.TextSecondary
            else -> Neon.Acid
        }
        Text(
            words.text(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center,
            maxLines = 2, lineHeight = 16.sp, modifier = Modifier.heightIn(min = 40.dp).testTag("mix_out_words"),
        )
    }
}

/** The one tip shown, with the button that does it; when there is none, a word to trust one's ears. */
@Composable
private fun TipCard(tip: MixTip?, apply: (MixTip) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh.copy(alpha = 0.6f)).padding(10.dp).testTag("mix_tip"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (tip == null) {
            Text(tr("Aucun conseil pour l'instant : règle à l'oreille."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
            return@Column
        }
        Text("⚠️ " + tip.text, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary, modifier = Modifier.testTag("mix_tip_text"))
        Pill(tip.fix, filled = true, tag = "mix_tip_fix") { apply(tip) }
    }
}

/** « M »: the track silent or heard again. */
@Composable
private fun SquareToggle(letter: String, on: Boolean, color: Color, description: String, tag: String, toggle: () -> Unit) {
    Box(
        Modifier.size(width = 30.dp, height = 40.dp).clip(RoundedCornerShape(8.dp)).background(if (on) color else Neon.Surface)
            .clickable(onClick = toggle)
            .semantics { contentDescription = description; stateDescription = if (on) tr("activé") else tr("désactivé") }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (on) Color.Black else Neon.TextSecondary)
    }
}

/** ▶ « Écouter seul » in a strip: the sound alone in a loop at once; ■ everything back. */
@Composable
private fun PlayAlone(on: Boolean, name: String, tag: String, toggle: () -> Unit) {
    Box(
        Modifier.size(width = 34.dp, height = 40.dp).clip(RoundedCornerShape(8.dp))
            .background(if (on) Neon.Acid else Neon.Acid.copy(alpha = 0.18f))
            .clickable(role = Role.Button, onClick = toggle)
            .semantics { selected = on; contentDescription = if (on) tr("Arrêter") else tr("Écouter « %s » seul, en boucle", name) }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (on) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = if (on) Color.Black else Neon.Acid, modifier = Modifier.size(20.dp))
    }
}

/**
 * The big fader: marks of its dB on the start side ([top], 0, −12, [bottom]; none on a small one),
 * its travel and cap, and its meter on the end side (the app's [com.pocketstudio.ui.LevelBar]: green,
 * amber, red in a gradient, its top dissolving into fine pixels, its peak held). A slide moves it from where it is;
 * two touches put it back at 0 dB.
 */
@Composable
private fun MixFader(
    value: Float, onChange: (Float) -> Unit, onReset: () -> Unit, meter: () -> Float, description: String, said: String, tag: String,
    top: String?, bottom: String?, height: Dp, mini: Boolean = false,
) {
    val latest by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val reset by rememberUpdatedState(onReset)
    val travel = remember { floatArrayOf(1f) }
    val held = remember { floatArrayOf(Float.NaN) }
    val state = rememberDraggableState { dy ->
        val from = if (held[0].isNaN()) latest else held[0]
        val now = (from - dy / travel[0]).coerceIn(0f, 1f)
        held[0] = now
        change(now)
    }
    val measurer = rememberTextMeasurer()
    val labelStyle = MonoStyle.copy(fontSize = 9.sp, color = Neon.TextSecondary)
    val labels = remember(top, bottom, labelStyle) {
        if (top == null || bottom == null) emptyList()
        else listOf(1f to top, 0.8f to "0", 0.4f to "−12", 0f to bottom).map { (pos, s) -> pos to measurer.measure(s, labelStyle) }
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val off = Neon.Surface
    val accent = Neon.Cyan
    // Amber from −12 dBFS, red over −1 (on this scale).
    val bar = com.pocketstudio.ui.rememberLevelBar(MIX_ZONES, dissolve = if (mini) 18.dp else 28.dp)
    val peak = com.pocketstudio.ui.rememberPeakHold { meterPosition(meter()) }
    val cap = Neon.TextPrimary
    Canvas(
        Modifier.width(if (mini) 44.dp else 64.dp).height(height)
            // Two touches within 300 ms (by the touches' own times): 0 dB. Nothing consumed: a slide still moves it.
            .pointerInput(Unit) {
                var lastUp = Long.MIN_VALUE / 2
                awaitEachGesture {
                    // Seen before the slide does (the initial pass): a touch that moved is a slide, not a tap.
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var upAt = -1L
                    while (true) {
                        val c = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                        if ((c.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                        if (!c.pressed) {
                            upAt = c.uptimeMillis
                            break
                        }
                    }
                    if (upAt < 0) return@awaitEachGesture
                    if (down.uptimeMillis - lastUp < 300L) {
                        lastUp = Long.MIN_VALUE / 2
                        reset()
                    } else {
                        lastUp = upAt
                    }
                }
            }
            .draggable(state, Orientation.Vertical, onDragStarted = { held[0] = latest }, onDragStopped = { held[0] = Float.NaN })
            .semantics {
                contentDescription = description
                stateDescription = said
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                setProgress { v -> change(v.coerceIn(0f, 1f)); true }
                onClick(label = tr("Remettre à 0 dB")) { reset(); true }
            }
            .testTag(tag),
    ) {
        val pad = 6.dp.toPx()
        val h = size.height - 2 * pad
        travel[0] = h.coerceAtLeast(1f)
        fun x(v: Float) = if (rtl) size.width - v else v
        fun y(pos: Float) = pad + (1f - pos) * h
        // The marks, on the start side.
        val labelW = if (labels.isEmpty()) 0f else 18.dp.toPx()
        labels.forEach { (pos, layout) ->
            val lx = if (rtl) size.width - layout.size.width else 0f
            drawText(layout, topLeft = Offset(lx, (y(pos) - layout.size.height / 2f).coerceIn(0f, size.height - layout.size.height)))
        }
        val trackW = 4.dp.toPx()
        val trackX = labelW + (size.width - labelW) * 0.42f
        drawRoundRect(Color.Black.copy(alpha = 0.55f), Offset(x(trackX) - trackW / 2f, pad), Size(trackW, h), CornerRadius(trackW / 2f))
        // 0 dB across the travel.
        drawLine(MARK, Offset(x(trackX - 9.dp.toPx()), y(0.8f)), Offset(x(trackX + 9.dp.toPx()), y(0.8f)), 1.dp.toPx())
        // The meter beside the travel.
        val mw = (if (mini) 5.dp else 6.dp).toPx()
        val mx = x(trackX + 14.dp.toPx() + if (rtl) mw else 0f)
        drawRoundRect(off, Offset(mx, pad), Size(mw, h), CornerRadius(mw / 2f))
        bar.draw(this, meterPosition(meter()), Offset(mx, pad), Size(mw, h), vertical = true, peak = peak.floatValue)
        // The cap.
        val cy = y(value)
        val capW = (if (mini) 24.dp else 30.dp).toPx()
        val capH = 14.dp.toPx()
        drawRoundRect(Color.Black.copy(alpha = 0.45f), Offset(x(trackX) - capW / 2f, cy - capH / 2f + 2.dp.toPx()), Size(capW, capH), CornerRadius(4.dp.toPx()))
        drawRoundRect(cap, Offset(x(trackX) - capW / 2f, cy - capH / 2f), Size(capW, capH), CornerRadius(4.dp.toPx()))
        drawLine(accent, Offset(x(trackX) - capW / 2f + 4.dp.toPx(), cy), Offset(x(trackX) + capW / 2f - 4.dp.toPx(), cy), 2.dp.toPx())
    }
}

private val MARK = Color(0x99A0A8B4)

/** Where the Mix's meters turn amber (−12 dBFS) and red (−1 dBFS), on their scale. */
private val MIX_ZONES = com.pocketstudio.ui.MeterZones(amber = 0.6f, red = 0.785f)
