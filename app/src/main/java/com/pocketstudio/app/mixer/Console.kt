package com.pocketstudio.app.mixer

import com.pocketstudio.ui.ScaledBottomSheet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.help.Glossary
import com.pocketstudio.app.learn.spot
import com.pocketstudio.model.Channel
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.app.ui.showcasePanel
import com.pocketstudio.ui.ProSlider
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt

/** The width of one strip of the console: a dozen of them side by side on a tablet, four or five on a phone. */
internal val CONSOLE_STRIP_W = 76.dp

/** The fader's own travel, at least (it grows with a big text size, never shrinks under a finger's reach). */
internal val CONSOLE_FADER_H = 176.dp

/** Audit final (09): what the console holds besides its faders (title, a strip's name, M S, dB, marks, padding). */
internal val CONSOLE_ROOM = 240.dp

/** The faders' height in a pane [height] high: lower lying down on a short screen (the A11 at 130 %). */
internal fun consoleFaderHeight(height: Dp, lying: Boolean): Dp =
    if (!lying || height == Dp.Infinity) CONSOLE_FADER_H else (height - CONSOLE_ROOM).coerceIn(80.dp, CONSOLE_FADER_H)

/**
 * Phase 36: the console of the Mix space, every track's strip side by side as on a mixing desk:
 * its colour and name (a touch opens the strip's sheet: volume, pan, effects, curves, « Ce qui
 * joue »), M and S, its vertical fader with the meter beside it (the meter read only when drawn).
 */
@Composable
internal fun Console(
    strips: List<StripInfo>, channels: List<Channel>, meters: () -> FloatArray, actions: MixerActions,
    moves: Map<Int, Int>, openMoves: (Int) -> Unit, openCurves: (Int) -> Unit,
    /** The faders' height; lower on a short screen lying down, the hint then left out. */
    faderHeight: Dp = CONSOLE_FADER_H,
) {
    var sheetFor by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(
        Modifier.fillMaxWidth().showcasePanel(RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp).testTag("console"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Console"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Neon.Cyan, modifier = Modifier.weight(1f))
            HelpDot(Glossary.MUTE_SOLO)
        }
        if (faderHeight >= CONSOLE_FADER_H) Text(
            tr("Glisse un fader pour le volume ; touche le nom d'une piste pour ses effets et ses courbes."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.padding(horizontal = 12.dp),
        )
        Row(
            Modifier.fillMaxWidth().fadingHorizontalScroll().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            strips.forEachIndexed { i, st ->
                ConsoleStrip(
                    st, channels.getOrElse(st.ch) { Channel() }, { meters().getOrElse(st.ch) { 0f } }, actions, moves[st.ch] ?: 0,
                    open = { sheetFor = st.ch }, first = i == 0, faderHeight = faderHeight,
                )
            }
        }
    }
    sheetFor?.let { ch ->
        val st = strips.firstOrNull { it.ch == ch }
        val c = channels.getOrNull(ch)
        if (st == null || c == null) {
            sheetFor = null
        } else {
            StripSheet(
                st, c, actions, moves[ch] ?: 0, close = { sheetFor = null },
                openMoves = { sheetFor = null; openMoves(ch) }, openCurves = { sheetFor = null; openCurves(ch) },
            )
        }
    }
}

/** One strip: colour, name (a touch: its sheet), M S, the fader and its meter, the dB, its effects. */
@Composable
private fun ConsoleStrip(info: StripInfo, c: Channel, meter: () -> Float, actions: MixerActions, moves: Int, open: () -> Unit, first: Boolean, faderHeight: Dp) {
    SideEffect { stripCompositions++ }
    val name = com.pocketstudio.model.DefaultNames.shown(info.name)
    val index = info.ch
    val tint = info.color ?: Neon.Cyan
    Column(
        Modifier.width(CONSOLE_STRIP_W).consoleStrip(RoundedCornerShape(12.dp), 0.35f)
            // The tutorial rings (and brings into view) the whole strip it names, its name with its fader.
            .spot("console_strip_$index")
            .padding(horizontal = 4.dp, vertical = 6.dp).testTag("strip_$index"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(tint, RoundedCornerShape(2.dp)))
        // The name opens the strip's sheet (effects, pan, curves): a 48 dp touch, two lines at most.
        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = tr("Effets et courbes de %s", name), onClick = open)
                .then(if (first) Modifier.spot("console_name", tr("Effets, courbes")) else Modifier)
                .testTag("strip_more_$index"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ConsoleToggle("M", c.mute, Neon.Red, tr("Couper la piste %s", name), "mute_$index") { actions.toggleMute(index) }
            ConsoleToggle("S", c.solo, Neon.Cyan, tr("Écouter la piste %s seule", name), "solo_$index") { actions.toggleSolo(index) }
        }
        VerticalFader(
            c.volume, { actions.setVolume(index, it) }, meter, tr("Volume de %s", name), "volume_$index", height = faderHeight,
            modifier = if (first) Modifier.spot("console_fader", tr("Glisse : volume")) else Modifier,
        )
        Text(faderDb(c.volume).replace(" dB", ""), style = MonoStyle.copy(fontSize = 12.sp), color = Neon.Cyan, maxLines = 1)
        val active = c.inserts.count { it.effect != com.pocketstudio.model.EffectType.NONE }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (active > 0) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = tr("Effets · %d", active), tint = Neon.Magenta, modifier = Modifier.size(14.dp))
                Text("$active", style = MaterialTheme.typography.labelSmall, color = Neon.Magenta)
            }
            if (moves > 0) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = tr("Ce qui bouge sur cette piste"), tint = Neon.Amber, modifier = Modifier.size(14.dp))
                Text("$moves", style = MaterialTheme.typography.labelSmall, color = Neon.Amber)
            }
        }
    }
}

@Composable
private fun ConsoleToggle(letter: String, on: Boolean, color: Color, description: String, tag: String, toggle: () -> Unit) {
    Box(
        Modifier.size(width = 32.dp, height = 40.dp).clip(RoundedCornerShape(8.dp))
            .background(if (on) color else Neon.SurfaceHigh)
            .clickable(onClick = toggle)
            .semantics { contentDescription = description; stateDescription = if (on) tr("activé") else tr("désactivé") }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (on) Color.Black else Neon.TextSecondary)
    }
}

/**
 * A fader standing up: a slide of the finger moves it from where it is (it never jumps under a
 * touch), the meter lit beside it, read only when drawn.
 */
@Composable
internal fun VerticalFader(
    value: Float, onChange: (Float) -> Unit, meter: () -> Float, description: String, tag: String,
    height: Dp = CONSOLE_FADER_H, modifier: Modifier = Modifier,
) {
    val latest by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    // The travel's height in pixels (known once drawn) and the value under the finger while it slides
    // (kept apart from the value given back: a slide moves it by as much as the finger goes).
    val travel = remember { floatArrayOf(1f) }
    val held = remember { floatArrayOf(Float.NaN) }
    val state = rememberDraggableState { dy ->
        val from = if (held[0].isNaN()) latest else held[0]
        val now = (from - dy / travel[0]).coerceIn(0f, 1f)
        held[0] = now
        change(now)
    }
    val off = Neon.SurfaceHigh
    val accent = Neon.Cyan
    val bar = com.pocketstudio.ui.rememberLevelBar(dissolve = 22.dp)
    val peak = com.pocketstudio.ui.rememberPeakHold { (meter() / 0.89f).coerceIn(0f, 1f) }
    val dim = Neon.TextSecondary
    val cap = Neon.TextPrimary
    Canvas(
        modifier.width(56.dp).height(height)
            .draggable(state, Orientation.Vertical, onDragStarted = { held[0] = latest }, onDragStopped = { held[0] = Float.NaN })
            .semantics {
                contentDescription = description
                stateDescription = faderDb(value)
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                setProgress { v -> change(v.coerceIn(0f, 1f)); true }
            }
            .testTag(tag),
    ) {
        travel[0] = size.height.coerceAtLeast(1f)
        val trackX = size.width * 0.42f
        val trackW = 6.dp.toPx()
        drawFaderSlot(trackX, 0f, trackW, size.height, off)
        val y = (1f - value) * size.height
        drawRoundRect(accent.copy(alpha = 0.55f), Offset(trackX - trackW / 2f, y), Size(trackW, size.height - y), CornerRadius(trackW / 2f))
        // 0 dB marked on the travel.
        val unity = (1f - 0.8f) * size.height
        drawLine(dim.copy(alpha = 0.6f), Offset(trackX - 10.dp.toPx(), unity), Offset(trackX + 10.dp.toPx(), unity), 1.dp.toPx())
        // The meter: a thin column to the right, green, amber, red from the bottom (the app's LevelBar:
        // a gradient, its top in fine pixels, its peak held).
        val mx = size.width * 0.80f
        val mw = 5.dp.toPx()
        drawRoundRect(off, Offset(mx, 0f), Size(mw, size.height), CornerRadius(2.dp.toPx()))
        bar.draw(this, (meter() / 0.89f).coerceIn(0f, 1f), Offset(mx, 0f), Size(mw, size.height), vertical = true, peak = peak.floatValue)
        // The cap, wide enough for a thumb to see where it is.
        val capH = 14.dp.toPx()
        val capW = 30.dp.toPx()
        drawFaderCap(trackX, y, capW, capH, cap, CAP_MARK, 1.5.dp.toPx())
    }
}

private val CAP_MARK = Color.Black.copy(alpha = 0.6f)

/**
 * A strip's sheet: its volume and pan, M and S, its effects (the same box as under each sound),
 * « Ce qui bouge sur cette piste » and its curves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StripSheet(
    info: StripInfo, c: Channel, actions: MixerActions, moves: Int, close: () -> Unit, openMoves: () -> Unit, openCurves: () -> Unit,
) {
    val name = com.pocketstudio.model.DefaultNames.shown(info.name)
    val index = info.ch
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("strip_sheet")) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 4.dp, height = 22.dp).background(info.color ?: Neon.Cyan, RoundedCornerShape(2.dp)))
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp).weight(1f).testTag("strip_sheet_title"))
                TextButton(onClick = close, modifier = Modifier.testTag("strip_sheet_close")) { Text(tr("Fermer")) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = c.mute, onClick = { actions.toggleMute(index) }, label = { Text(tr("Muet")) }, colors = pillColors(Neon.Red), modifier = Modifier.testTag("sheet_mute_$index"))
                FilterChip(selected = c.solo, onClick = { actions.toggleSolo(index) }, label = { Text(tr("Solo")) }, colors = pillColors(), modifier = Modifier.testTag("sheet_solo_$index"))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Volume"), Modifier.weight(1f))
                Text(faderDb(c.volume), style = MonoStyle, color = Neon.Cyan)
            }
            ProSlider(value = c.volume, onValueChange = { actions.setVolume(index, it) }, modifier = Modifier.testTag("sheet_volume_$index"))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Place"))
                HelpDot(Glossary.PAN)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Text(consolePanLabel(c.pan), style = MonoStyle, color = Neon.Cyan)
            }
            // Safe at the edge (a back swipe closes the sheet, it never pans), centre caught, two touches = centre.
            com.pocketstudio.ui.PanSlider(
                c.pan, { v -> actions.setPan(index, v) }, step = 0.01f, modifier = Modifier.testTag("sheet_pan_$index"),
                label = tr("Place"), stateLabel = ::consolePanLabel,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = openMoves, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("strip_moves_$index")) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + if (moves > 0) tr("Ce qui bouge (%d)", moves) else tr("Ce qui joue"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(onClick = openCurves, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("strip_curves_$index")) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + tr("Courbes"), maxLines = 1)
                }
            }
            Text(tr("Effets"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("effects_$index"))
            EffectsBox(index, c, actions)
        }
    }
}

/** A strip's pan in words (« Gauche 30 », « Centre »): shown and said by TalkBack. */
internal fun consolePanLabel(v: Float): String {
    val pan = (v * 100).roundToInt()
    return when {
        pan < -2 -> tr("Gauche %d", -pan)
        pan > 2 -> tr("Droite %d", pan)
        else -> tr("Centre")
    }
}
