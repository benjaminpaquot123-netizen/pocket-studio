package com.pocketstudio.app.mixer


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import com.pocketstudio.app.ui.showcasePanel
import com.pocketstudio.model.Channel
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ProSlider
import com.pocketstudio.ui.ScaledBottomSheet
import com.pocketstudio.ui.fadingEdges
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
    /** « Glisse un fader… » under the title (left out when the faders were lowered to fit). */
    hint: Boolean = faderHeight >= CONSOLE_FADER_H,
    /** The pads' names as the project has them (by lane); the built-in kit's names by default. */
    padNames: (Int) -> String = { com.pocketstudio.app.rhythm.DrumKitInfo.names.getOrElse(it) { "" } },
) {
    var sheetFor by rememberSaveable { mutableStateOf<Int?>(null) }
    var padsOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().showcasePanel(RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp).testTag("console"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Tour 9: the card's title in the same colour as « Courbes et effets » below (readable under the help labels' veil).
            Text(tr("Volumes"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            HelpDot(Glossary.MUTE_SOLO)
            // Tour 9 (D12): the pads' own effects and sends, one touch away from the Mix.
            // As the HelpDot beside it: drawn no taller than the title (a 48 dp row pushed the first fader out of its pane
            // lying down at 130 %), touched over 48 dp (Compose widens a small target's touch).
            Row(
                Modifier.weight(1f).wrapContentWidth(Alignment.End).clip(RoundedCornerShape(8.dp))
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { padsOpen = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp).testTag("console_pads"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
                // One line, smaller before it wraps.
                com.pocketstudio.ui.FitText(
                    " " + tr("  Effets du pad").trim(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, min = 10.sp,
                )
            }
        }
        if (hint) Text(
            tr("Glisse un fader pour le volume ; touche le nom d'une piste pour ses effets et ses courbes."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.padding(horizontal = 12.dp),
        )
        val scroll = rememberScrollState()
        Box {
            Row(
                Modifier.fillMaxWidth().fadingEdges(scroll).horizontalScroll(scroll).padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                strips.forEachIndexed { i, st ->
                    ConsoleStrip(
                        st, channels.getOrElse(st.ch) { Channel() }, { meters().getOrElse(st.ch) { 0f } }, actions, moves[st.ch] ?: 0,
                        open = { sheetFor = st.ch }, first = i == 0, faderHeight = faderHeight,
                    )
                }
            }
            MoreTracksTab(scroll, "console_more_right", Modifier.align(Alignment.CenterEnd))
        }
    }
    if (padsOpen) PadSheet(channels, actions, padNames) { padsOpen = false }
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
    val probe = LocalFitProbe.current
    SideEffect { if (!probe) stripCompositions++ }
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
            // Tour 9 (F023, « Drum / s »): smaller before a word is cut in two, as the Mode simple's heads.
            com.pocketstudio.ui.FitText(
                name, style = MaterialTheme.typography.labelMedium.copy(lineHeight = 14.sp), fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, maxLines = 2, min = 7.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            com.pocketstudio.app.ui.MuteToggle(c.mute, name, "mute_$index", color = Neon.Red, size = androidx.compose.ui.unit.DpSize(32.dp, 40.dp)) { actions.toggleMute(index) }
            com.pocketstudio.app.ui.SoloToggle(c.solo, name, "solo_$index", size = androidx.compose.ui.unit.DpSize(32.dp, 40.dp)) { actions.toggleSolo(index) }
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
    val off = Neon.SurfaceHigh
    val accent = Neon.Cyan
    val bar = com.pocketstudio.ui.rememberLevelBar(dissolve = 22.dp)
    val peak = com.pocketstudio.ui.rememberPeakHold { (meter() / 0.89f).coerceIn(0f, 1f) }
    val dim = Neon.TextSecondary
    val cap = Neon.TextPrimary
    Canvas(
        modifier.width(56.dp).height(height)
            .onSizeChanged { travel[0] = it.height.toFloat().coerceAtLeast(1f) }
            .capDrag({ (1f - latest) * travel[0] }, { latest }, { from, dy -> change((from - dy / travel[0]).coerceIn(0f, 1f)) })
            .semantics {
                contentDescription = description
                stateDescription = faderDb(value)
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                setProgress { v -> change(v.coerceIn(0f, 1f)); true }
            }
            .testTag(tag)
            // Its meter redraws it at each frame: in a layer of its own, the console around it is not recorded again.
            .graphicsLayer(),
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

/** F024: a finger that lands this close to a fader's cap takes it; anywhere else on the fader it scrolls the page. */
internal val FADER_GRAB = 32.dp

/**
 * A fader's slide (F024, option A): only a finger landing within [FADER_GRAB] of the cap (at [capY], in pixels from the
 * top) takes it; elsewhere nothing is consumed and the page scrolls. Once taken, [move] gets the value at the touch
 * ([start]) and how far the finger went down since it touched: the cap follows the finger from the touch on (no dead
 * zone, then no jump, S30).
 */
internal fun Modifier.capDrag(capY: () -> Float, start: () -> Float, move: (from: Float, dy: Float) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()
            if (kotlin.math.abs(down.position.y - capY()) > FADER_GRAB.toPx()) return@awaitEachGesture
            val first = awaitVerticalTouchSlopOrCancellation(down.id) { c, _ -> c.consume() } ?: return@awaitEachGesture
            val from = start()
            move(from, first.position.y - down.position.y)
            verticalDrag(first.id) { c ->
                c.consume()
                move(from, c.position.y - down.position.y)
            }
        }
    }

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
    // Tour 9 (F025): the track's name stays above what scrolls (it went out at the top, « XY… »), whole on two lines at most.
    ScaledBottomSheet(
        onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("strip_sheet"), scrolls = true,
        header = {
            Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 4.dp, height = 22.dp).background(info.color ?: Neon.Cyan, RoundedCornerShape(2.dp)))
                com.pocketstudio.ui.WholeWordsText(
                    name, Modifier.padding(start = 8.dp).weight(1f).testTag("strip_sheet_title"), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = close, modifier = Modifier.testTag("strip_sheet_close")) { Text(tr("Fermer")) }
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = c.mute, onClick = { actions.toggleMute(index) }, label = { Text(tr("Muet")) }, colors = pillColors(Neon.Red), modifier = Modifier.testTag("sheet_mute_$index"))
                FilterChip(selected = c.solo, onClick = { actions.toggleSolo(index) }, label = { Text(tr("Solo")) }, colors = pillColors(), modifier = Modifier.testTag("sheet_solo_$index"))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Volume"), Modifier.weight(1f))
                Text(faderDb(c.volume), style = MonoStyle, color = Neon.Cyan)
            }
            ProSlider(value = c.volume, onValueChange = { actions.setVolume(index, it) }, label = tr("Volume"), valueText = faderDb(c.volume), modifier = Modifier.testTag("sheet_volume_$index"))
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
            androidx.compose.runtime.CompositionLocalProvider(LocalFxTrackName provides name) { EffectsBox(index, c, actions) }
        }
    }
}

/**
 * Tour 9 (S34, seats 18 34: « Melodia » off-screen, nothing said): while more strips wait further on, an arrow
 * stands on the row's far edge (as on the Live's columns), « Voir les autres pistes »; a touch brings the last
 * ones in, and it goes.
 */
@Composable
internal fun MoreTracksTab(scroll: androidx.compose.foundation.ScrollState, tag: String, modifier: Modifier = Modifier) {
    if (!scroll.canScrollForward) return
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Box(
        modifier.size(32.dp, 56.dp).clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)).background(Neon.Cyan.copy(alpha = 0.9f))
            .clickable(role = androidx.compose.ui.semantics.Role.Button) { scope.launch { scroll.animateScrollTo(scroll.maxValue) } }
            .semantics { contentDescription = tr("Voir les autres pistes") }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Neon.OnAccent, modifier = Modifier.size(24.dp))
    }
}

/**
 * Tour 9 (D12, seats 18 49 59: « Envoi réverbe » on the pad « Caisse », not found from the Mix): the pads'
 * effects and sends, a pad picked by its name, without leaving the Mix.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PadSheet(channels: List<Channel>, actions: MixerActions, padNames: (Int) -> String, close: () -> Unit) {
    var lane by rememberSaveable { mutableStateOf(0) }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Tour 9 (F025, the same as a strip's sheet): the title stays above what scrolls.
    ScaledBottomSheet(
        onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("pad_sheet"), scrolls = true,
        header = {
            Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                com.pocketstudio.ui.WholeWordsText(
                    tr("  Effets du pad").trim(), Modifier.weight(1f).testTag("pad_sheet_head"), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = close, modifier = Modifier.testTag("pad_sheet_close")) { Text(tr("Fermer")) }
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (l in 0 until com.pocketstudio.model.DrumPattern.PAD_COUNT) {
                    FilterChip(
                        selected = l == lane, onClick = { lane = l }, label = { Text(tr("Pad %1\$d · %2\$s", l + 1, padNames(l)), maxLines = 1) },
                        colors = pillColors(), modifier = Modifier.testTag("pad_fx_$l"),
                    )
                }
            }
            val ch = com.pocketstudio.model.Mixer.padChannel(lane)
            Text(tr("Effets · %s", tr("pad %1\$d (%2\$s)", lane + 1, padNames(lane))), style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("pad_sheet_title"))
            androidx.compose.runtime.key(lane) {
                androidx.compose.runtime.CompositionLocalProvider(LocalFxTrackName provides padNames(lane)) { EffectsBox(ch, channels.getOrElse(ch) { Channel() }, actions) }
            }
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
