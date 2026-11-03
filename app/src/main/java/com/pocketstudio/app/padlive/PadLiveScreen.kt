package com.pocketstudio.app.padlive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.session.PadClock
import com.pocketstudio.app.session.livePadColor
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadMode
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.padLight
import com.pocketstudio.ui.rememberPadLight

/** What Pad Live shows (n° 25, lot B1, `docs/maquettes/padlive-trois.png`). */
class PadLiveUi(
    val pads: LivePads,
    /** Per pad of the four pages: 0 off, 1 waiting for its beat, 2 playing. */
    val states: List<Int>,
    val page: Int,
    /** The pack last put on the pads (its id, [PadLive.PACKS]). */
    val pack: String,
    val playing: Boolean,
    val tempoBpm: Double,
    val beatsPerBar: Int,
    /** « Préparation du pack Trap… » (null: ready). */
    val busy: String? = null,
    /** A held effect works on the music: the pads that play wear its veil. */
    val effectHeld: Boolean = false,
    val canUndo: Boolean = false,
)

/** Every gesture of Pad Live, given by the app. */
class PadLiveActions(
    val back: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    val nudgeTempo: (Int) -> Unit = {},
    val openTempo: () -> Unit = {},
    /** A finger down (true) on a pad of any page, or up (false). */
    val touch: (pad: Int, down: Boolean) -> Unit = { _, _ -> },
    val setPage: (Int) -> Unit = {},
    val choosePack: (String) -> Unit = {},
    /** An empty pad: the one sound browser, for it. */
    val browse: (pad: Int) -> Unit = {},
    val setMode: (pad: Int, PadMode) -> Unit = { _, _ -> },
    val setGroup: (pad: Int, Int) -> Unit = { _, _ -> },
    val setBeats: (pad: Int, Int) -> Unit = { _, _ -> },
    val setGain: (pad: Int, Float) -> Unit = { _, _ -> },
    val clear: (pad: Int) -> Unit = {},
    val stopAll: () -> Unit = {},
    /** Held effects: Filtre (null: let go; 0 grave .. 1 aigu), Écho, Roulement, then Montée, Chute, Bégaie under « Plus ». */
    val filter: (Float?) -> Unit = {},
    val echo: (Boolean) -> Unit = {},
    val roll: (Boolean) -> Unit = {},
    val riser: () -> Unit = {},
    val brake: (Boolean) -> Unit = {},
    val stutter: (Boolean) -> Unit = {},
    val undo: () -> Unit = {},
    /** « Plus » › the whole Live page (bricks, notes, Acid 303) of the same project. */
    val openLive: () -> Unit = {},
    /** « Plus » › the effects box of the pads' strip. */
    val effects: () -> Unit = {},
)

/**
 * Pad Live on the whole screen: the pads ready to play, the tempo, three held effects, « Plus »
 * for the rest. Above: back, ▶/■, « − 124 + », ● (the Live take, [record]). Then the pages A to
 * D and « Pack : Fête ▾ », the 16 pads of the page, the effects, « Plus ». Each pad wears the one
 * light of every pad ([padLight]): nothing moves on a pad that does not play.
 */
@Composable
fun PadLiveScreen(
    ui: PadLiveUi,
    actions: PadLiveActions,
    /** Transport steps since play (-1: stopped), read only by the pads' clock. */
    clockOf: () -> Int,
    record: @Composable () -> Unit = {},
    /** Tests: the pads' clock does not run (a still capture). */
    still: Boolean = false,
) {
    var more by rememberSaveable { mutableStateOf(false) }
    var settingsOf by rememberSaveable { mutableStateOf<Int?>(null) }
    val clock = remember { PadClock() }
    val statesNow by rememberUpdatedState(ui.states)
    val clipsNow by rememberUpdatedState(ui.pads.all)
    val clockNow by rememberUpdatedState(clockOf)
    val tempoNow by rememberUpdatedState(ui.tempoBpm)
    val barNow by rememberUpdatedState(ui.beatsPerBar)
    // One frame clock for every pad, only while something plays: still otherwise.
    val active = !still && (ui.playing || ui.states.any { it != 0 })
    LaunchedEffect(active) {
        if (!active) {
            clock.stop()
            return@LaunchedEffect
        }
        val tick: (Long) -> Unit = { n -> clock.tick(n, clockNow(), statesNow, clipsNow, tempoNow, barNow) }
        androidx.compose.runtime.withFrameNanos(tick)
        while (true) androidx.compose.animation.core.withInfiniteAnimationFrameNanos(tick)
    }
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(Neon.Background, Color.Black, 0.2f), lerp(Neon.Background, Color.Black, 0.5f))))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp)
            .testTag("padlive"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PadLiveBar(ui, actions, record)
        PageRow(ui, actions)
        ui.busy?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("padlive_busy"))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val gap = 8.dp
            val byWidth = (maxWidth - gap * 3) / 4
            val byHeight = (maxHeight - gap * 3) / 4
            val side = minOf(byWidth, byHeight)
            Column(Modifier.align(Alignment.TopCenter), verticalArrangement = Arrangement.spacedBy(gap)) {
                for (row in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (col in 0 until 4) {
                            val slot = row * 4 + col
                            val pad = PadLive.pad(ui.page, slot)
                            val clip = ui.pads.at(pad)
                            PadLivePad(
                                pad, clip, ui.states.getOrElse(pad) { 0 }, clock, ui.effectHeld,
                                Modifier.size(width = byWidth.coerceAtMost(side * 1.25f), height = side),
                                touch = actions.touch,
                                empty = { actions.browse(pad) },
                                settings = { settingsOf = pad },
                            )
                        }
                    }
                }
            }
        }
        SceneEffects(actions, more)
        AnimatedVisibility(more) { MoreRow(ui, actions) }
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End) {
            Row(
                Modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(if (more) Neon.SurfaceHigh else Neon.Surface)
                    .clickable(role = Role.Button) { more = !more }.padding(horizontal = 22.dp).testTag("padlive_more"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = Neon.TextPrimary)
                Text(if (more) tr("Moins") else tr("Plus"), style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary)
            }
        }
    }
    settingsOf?.let { pad ->
        val clip = ui.pads.at(pad)
        if (clip == null) settingsOf = null
        else PadSettingsSheet(pad, clip, actions, close = { settingsOf = null })
    }
}

/** ← Pad Live · ▶/■ · « − 124 + » · ●. */
@Composable
private fun PadLiveBar(ui: PadLiveUi, actions: PadLiveActions, record: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = actions.back, modifier = Modifier.testTag("padlive_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"), tint = Neon.Amber)
        }
        Text(tr("Pad Live"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(if (ui.playing) Neon.Acid else Neon.SurfaceHigh)
                .clickable(role = Role.Button, onClick = actions.togglePlay).testTag("padlive_play")
                .semantics { contentDescription = if (ui.playing) tr("Arrêter") else tr("Jouer") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = if (ui.playing) Color.Black else Neon.TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Neon.Surface), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { actions.nudgeTempo(-1) }, modifier = Modifier.testTag("padlive_tempo_less")) {
                Icon(Icons.Filled.Remove, contentDescription = tr("Moins vite"))
            }
            Column(
                Modifier.clickable(role = Role.Button, onClick = actions.openTempo).testTag("padlive_tempo"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${ui.tempoBpm.toInt()}", style = MonoStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = Neon.Amber)
                Text("BPM", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
            }
            IconButton(onClick = { actions.nudgeTempo(1) }, modifier = Modifier.testTag("padlive_tempo_more")) {
                Icon(Icons.Filled.Add, contentDescription = tr("Plus vite"))
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.testTag("padlive_rec")) { record() }
    }
}

/**
 * One pad of Pad Live: its colour, its name at the bottom, how it plays in the corner (a touch
 * there: its settings), the one light of every pad. A sound pad acts on the finger's down (a hit
 * at once; « tenir » until the finger leaves); an empty one opens the sound browser.
 */
@Composable
private fun PadLivePad(
    pad: Int,
    clip: LiveClip?,
    state: Int,
    clock: PadClock,
    veil: Boolean,
    modifier: Modifier,
    touch: (Int, Boolean) -> Unit,
    empty: () -> Unit,
    settings: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val light = rememberPadLight()
    val color = clip?.let { livePadColor(it.name, pad % PadLive.PAGE_SIZE) } ?: Neon.Surface
    val shape = RoundedCornerShape(14.dp)
    val touchNow by rememberUpdatedState(touch)
    val emptyNow by rememberUpdatedState(empty)
    val has by rememberUpdatedState(clip != null)
    val loops = clip != null && !clip.oneShot
    val beats = clip?.beats ?: 4
    val name = clip?.let { DefaultNames.shownSample(it.name) }
    Box(
        modifier
            .clip(shape)
            .background(
                if (clip == null) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.03f), Color.Black.copy(alpha = 0.18f)))
                else Brush.verticalGradient(listOf(lerp(Neon.SurfaceHigh, color, 0.5f), lerp(Neon.Surface, color, 0.22f))),
            )
            .border(1.dp, if (clip == null) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.10f), shape)
            .padLight(
                light, color, playing = clip != null && state == 2, waiting = clip != null && state == 1,
                progress = if (loops) ({ clock.position(pad, beats) }) else null,
                veil = veil && state == 2, corner = 14.dp, touchable = clip != null,
            )
            .pointerInput(pad) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (!has) {
                        if (waitForUpOrCancellation() != null) emptyNow()
                        return@awaitEachGesture
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    touchNow(pad, true)
                    waitForUpOrCancellation()
                    touchNow(pad, false)
                }
            }
            .semantics {
                val what = when {
                    clip == null -> tr("vide")
                    state == 2 -> tr("joue")
                    state == 1 -> tr("en attente")
                    else -> tr("prêt")
                }
                contentDescription = "${name ?: tr("Pad %d", pad % PadLive.PAGE_SIZE + 1)} · $what"
                role = Role.Button
                onClick {
                    if (clip == null) emptyNow() else { touchNow(pad, true); touchNow(pad, false) }
                    true
                }
                if (clip != null) onLongClick(tr("Réglages du pad")) { settings(); true }
            }
            .testTag("padlive_pad_$pad"),
    ) {
        if (clip == null) {
            Text("+", Modifier.align(Alignment.Center), color = Neon.TextSecondary.copy(alpha = 0.55f), fontSize = 26.sp, fontWeight = FontWeight.Light)
            return@Box
        }
        // How it plays, in the corner: a touch there opens its settings.
        Box(
            Modifier.align(Alignment.TopEnd).size(width = 64.dp, height = 36.dp)
                .clickable(role = Role.Button, onClickLabel = tr("Réglages du pad"), onClick = settings)
                .testTag("padlive_mode_$pad"),
            contentAlignment = Alignment.TopEnd,
        ) {
            Text(
                modeWord(PadLive.modeOf(clip)) + if (clip.group > 0) " · ${clip.group}" else "",
                style = MaterialTheme.typography.labelSmall, color = Neon.TextPrimary.copy(alpha = 0.7f), maxLines = 1,
                modifier = Modifier.padding(top = 6.dp, end = 8.dp),
            )
        }
        Text(
            name.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Neon.TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

/** A pad's mode in one small word (the mockup's « boucle », « coup », « tenir »). */
internal fun modeWord(mode: PadMode): String = when (mode) {
    PadMode.SHOT -> tr("coup")
    PadMode.LOOP -> tr("boucle")
    PadMode.HOLD -> tr("tenir")
}

/**
 * « Effets de scène · Appuie et maintiens »: Filtre (the finger's place on it: grave to aigu),
 * Écho, Roulement; with « Plus », Montée, Chute and Bégaie too. Let go: everything comes back.
 */
@Composable
private fun SceneEffects(actions: PadLiveActions, more: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("EFFETS DE SCÈNE"), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary, modifier = Modifier.weight(1f))
            Text(tr("Appuie et maintiens"), style = MaterialTheme.typography.labelMedium, color = Neon.TextPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeldEffect(tr("Filtre"), tr("grave ↔ aigu"), "filter", Modifier.weight(1f), slide = actions.filter) {}
            HeldEffect(tr("Écho"), tr("en sortie"), "echo", Modifier.weight(1f), hold = actions.echo)
            HeldEffect(tr("Roulement"), "1/16", "roll", Modifier.weight(1f), hold = actions.roll)
        }
        if (more) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeldEffect(tr("Montée"), tr("jusqu'à la mesure"), "riser", Modifier.weight(1f), hold = { if (it) actions.riser() })
                HeldEffect(tr("Chute"), tr("freinage"), "brake", Modifier.weight(1f), hold = actions.brake)
                HeldEffect(tr("Bégaie"), "1/16", "stutter", Modifier.weight(1f), hold = actions.stutter)
            }
        }
    }
}

/** A held effect's button: on while the finger stays ([hold]); [slide]: also the finger's place across it (0..1, null: let go). */
@Composable
private fun HeldEffect(
    title: String,
    line: String,
    tag: String,
    modifier: Modifier,
    slide: ((Float?) -> Unit)? = null,
    hold: (Boolean) -> Unit,
) {
    var held by remember { mutableStateOf(false) }
    val holdNow by rememberUpdatedState(hold)
    val slideNow by rememberUpdatedState(slide)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.heightIn(min = 60.dp).clip(shape).background(if (held) Neon.Amber.copy(alpha = 0.22f) else Neon.Surface)
            .border(if (held) 2.dp else 1.dp, if (held) Neon.Amber else Color.White.copy(alpha = 0.06f), shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    held = true
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    slideNow?.invoke((down.position.x / w).coerceIn(0f, 1f))
                    holdNow(true)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        slideNow?.invoke((change.position.x / w).coerceIn(0f, 1f))
                        change.consume()
                    }
                    held = false
                    slideNow?.invoke(null)
                    holdNow(false)
                }
            }
            .semantics {
                contentDescription = "$title · $line"
                role = Role.Button
                onClick { holdNow(true); holdNow(false); true }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("padlive_fx_$tag"),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Neon.TextPrimary, maxLines = 1)
        Text(line, style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1)
    }
}

/** « Plus »: everything stopped, « Annuler », the whole Live page, the pads' effects box. */
@Composable
private fun MoreRow(ui: PadLiveUi, actions: PadLiveActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MoreButton(Icons.Filled.StopCircle, tr("Tout couper"), "padlive_stop_all", Modifier.weight(1f), actions.stopAll)
        MoreButton(Icons.AutoMirrored.Filled.Undo, tr("Annuler"), "padlive_undo", Modifier.weight(1f), actions.undo, enabled = ui.canUndo)
        MoreButton(Icons.Filled.GridView, tr("Briques"), "padlive_live", Modifier.weight(1f), actions.openLive)
        MoreButton(Icons.Filled.Tune, tr("Effets"), "padlive_effects", Modifier.weight(1f), actions.effects)
    }
}

@Composable
private fun MoreButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tag: String, modifier: Modifier, onClick: () -> Unit, enabled: Boolean = true) {
    Column(
        modifier.heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp)).background(Neon.Surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(6.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val tint = if (enabled) Neon.TextPrimary else Neon.TextSecondary.copy(alpha = 0.5f)
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
