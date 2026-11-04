package com.pocketstudio.app.padlive

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.session.PadClock
import com.pocketstudio.app.session.livePadColor
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.hit
import com.pocketstudio.ui.padLight
import com.pocketstudio.ui.rememberPadLight

/**
 * Lot B3, « les lumières suivent le son »: how loud each pad sounds now, read from the engine once
 * a frame while something plays ([update]); a pad's light reads it when drawn ([of]). Nothing
 * reads or redraws while nothing plays: a still screen stays still.
 */
@Stable
class PadLevels {
    private val values = FloatArray(LivePads.ALL)
    private var frame by mutableLongStateOf(0L)

    /** A new frame: [read] fills the levels (the engine's, [com.pocketstudio.engine.NativeEngine.clipLevels]). */
    fun update(read: (FloatArray) -> Unit) {
        read(values)
        frame++
    }

    /** Everything went quiet: every light falls to nothing (one last redraw). */
    fun clear() {
        if (values.all { it == 0f }) return
        values.fill(0f)
        frame++
    }

    /** Pad [pad]'s level now (0..1), read when drawn. */
    fun of(pad: Int): Float {
        frame
        return values.getOrElse(pad) { 0f }
    }

    /** The loudest of [pads] now (the room's light in « Fête »). */
    fun loudest(pads: IntRange): Float {
        frame
        var m = 0f
        for (p in pads) if (p in values.indices && values[p] > m) m = values[p]
        return m
    }
}

/**
 * « Fête » (lot B3): the pads of the page shown on the whole screen, big, nothing else but the way
 * out, ▶/■ and the pages; the room's light follows the music (the loudest pad), each pad's light
 * its own sound. Nothing moves while nothing plays. The pads are played as on Pad Live (their
 * modes, their groups); a pad with no sound is left dark (sounds are chosen on Pad Live).
 */
@Composable
fun PartyScreen(
    ui: PadLiveUi,
    actions: PadLiveActions,
    clockOf: () -> Int,
    levels: PadLevels,
    readLevels: ((FloatArray) -> Unit)?,
    leave: () -> Unit,
    still: Boolean = false,
) {
    val clock = remember { PadClock() }
    val statesNow by rememberUpdatedState(ui.states)
    val clipsNow by rememberUpdatedState(ui.pads.all)
    val clockNow by rememberUpdatedState(clockOf)
    val tempoNow by rememberUpdatedState(ui.tempoBpm)
    val barNow by rememberUpdatedState(ui.beatsPerBar)
    val readNow by rememberUpdatedState(readLevels)
    val active = !still && (ui.playing || ui.states.any { it != 0 })
    LaunchedEffect(active) {
        if (!active) {
            clock.stop()
            // Stopped: every light falls dark (a still capture keeps the levels it was given).
            if (!still) levels.clear()
            return@LaunchedEffect
        }
        val tick: (Long) -> Unit = { n ->
            clock.tick(n, clockNow(), statesNow, clipsNow, tempoNow, barNow)
            readNow?.let { levels.update(it) }
        }
        androidx.compose.runtime.withFrameNanos(tick)
        while (true) androidx.compose.animation.core.withInfiniteAnimationFrameNanos(tick)
    }
    val page = ui.page
    val first = PadLive.pad(page, 0)
    val pagePads = first until first + PadLive.PAGE_SIZE
    val glow = Neon.Amber
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .drawBehind {
                // The room's light: a wide glow from below, as bright as the music is loud (none at rest).
                val loud = if (readLevels == null) 0f else levels.loudest(pagePads)
                if (loud > 0.002f) {
                    val a = (com.pocketstudio.ui.padLightFollow(loud) - com.pocketstudio.ui.PAD_LIGHT_FLOOR) / (1f - com.pocketstudio.ui.PAD_LIGHT_FLOOR)
                    drawRect(
                        Brush.radialGradient(
                            0f to glow.copy(alpha = 0.35f * a), 1f to Color.Transparent,
                            center = Offset(size.width / 2, size.height), radius = size.maxDimension * 0.9f,
                        ),
                    )
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("padlive_party"),
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundButton(Icons.Filled.Close, tr("Quitter les gros pads"), "padlive_party_leave", Neon.SurfaceHigh, Neon.TextPrimary, leave)
                Spacer(Modifier.weight(1f))
                PadLive.PAGE_NAMES.forEachIndexed { i, letter ->
                    val chosen = i == page
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (chosen) Neon.SurfaceHigh else Color.Transparent)
                            .clickable(role = Role.Tab) { actions.setPage(i) }
                            .semantics { selected = chosen; contentDescription = tr("Page %s", letter) }
                            .testTag("padlive_party_page_$letter"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(letter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (chosen) Neon.TextPrimary else Neon.TextSecondary)
                    }
                }
                Spacer(Modifier.weight(1f))
                RoundButton(
                    if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, if (ui.playing) tr("Arrêter") else tr("Jouer"), "padlive_party_play",
                    if (ui.playing) Neon.Acid else Neon.SurfaceHigh, if (ui.playing) Color.Black else Neon.TextPrimary, actions.togglePlay,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val gap = 10.dp
                val w = (maxWidth - gap * 3) / 4
                val h = (maxHeight - gap * 3) / 4
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (row in 0 until PadLive.ROWS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            for (col in 0 until PadLive.ROW_SIZE) {
                                val pad = PadLive.pad(page, row * PadLive.ROW_SIZE + col)
                                PartyPad(pad, ui, clock, levels.takeIf { readLevels != null }, Modifier.size(width = w, height = h), actions.touch)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tag: String, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(bg).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint) }
}

/** One big pad of « Fête »: its colour, its name in big, its light following its sound. */
@Composable
private fun PartyPad(pad: Int, ui: PadLiveUi, clock: PadClock, levels: PadLevels?, modifier: Modifier, touch: (Int, Boolean) -> Unit) {
    val clip = ui.pads.at(pad)
    val state = ui.states.getOrElse(pad) { 0 }
    val haptics = LocalHapticFeedback.current
    val light = rememberPadLight()
    val color = clip?.let { livePadColor(it.name, pad % PadLive.PAGE_SIZE) } ?: Neon.Surface
    val shape = RoundedCornerShape(20.dp)
    val touchNow by rememberUpdatedState(touch)
    val has = clip != null
    val loops = clip != null && !clip.oneShot
    val beats = clip?.beats ?: 4
    Box(
        modifier.clip(shape)
            .background(if (clip == null) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.02f), Color.Transparent))
                else Brush.verticalGradient(listOf(lerp(Neon.SurfaceHigh, color, 0.6f), lerp(Color.Black, color, 0.3f))))
            .border(1.dp, Color.White.copy(alpha = if (clip == null) 0.05f else 0.12f), shape)
            .padLight(
                light, color, playing = has && state == 2, waiting = has && state == 1,
                progress = if (loops) ({ clock.position(pad, beats) }) else null,
                veil = ui.effectHeld && state == 2, corner = 20.dp, touchable = has,
                level = levels?.let { l -> { l.of(pad) } },
            )
            .then(
                if (!has) Modifier else Modifier.pointerInput(pad) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val localPad = pad
                        haptics.hit()
                        touchNow(localPad, true)
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            // A pad emptied or a party closed under the finger still lets go (fix 2, n° 2).
                            touchNow(localPad, false)
                        }
                    }
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = padDescription(pad, clip, state)
                if (has) role = Role.Button
                padActions(this, pad, has, touchNow)
            }
            .testTag("padlive_party_pad_$pad"),
        contentAlignment = Alignment.Center,
    ) {
        if (clip != null) {
            Text(
                DefaultNames.shownSample(clip.name), color = Neon.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }
    }
}
