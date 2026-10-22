package com.pocketstudio.app.live

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.analysis.drawSpectrumBackdrop
import com.pocketstudio.app.mixer.XY_INSET
import com.pocketstudio.app.mixer.XyLabelled
import com.pocketstudio.app.mixer.xyPosition
import com.pocketstudio.app.mixer.xyValue
import com.pocketstudio.app.session.Perf
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.fadingHorizontalScroll

/** What the big pad of the Live effects panel plays (chosen by the chips above it). */
enum class XyMode {
    /** Whole mix: the DJ filter (left low-pass, right high-pass, up resonance). */
    DJ_FILTER,
    /** The 303: cutoff × resonance. */
    ACID,
    /** Whole mix: reverb return × echo return; a track: its reverb send × its echo send. */
    SPACE,
    /** A track: its Saleté macro × its Couleur macro. */
    CRUSH,
    /** A track: its Couleur macro × a macro of choice (the mixer's pad XY). */
    COLOUR,
    /** Slots for the engine's coming effects: shown once their sound exists (see [xyModesFor]). */
    WOBBLE,
    REPEAT,
}

/**
 * The pad's modes for a target: the whole mix ([track] null) or one track. [hasAcid]: the project
 * has a 303 line (its pad is offered on the whole mix, and on the acid column). [extra]: the
 * coming modes ([XyMode.WOBBLE], [XyMode.REPEAT]) whose sound the engine already plays.
 */
fun xyModesFor(track: LiveColumn?, hasAcid: Boolean, extra: Set<XyMode> = emptySet()): List<XyMode> {
    val base = if (track == null) {
        listOfNotNull(XyMode.DJ_FILTER, XyMode.ACID.takeIf { hasAcid }, XyMode.SPACE)
    } else {
        listOfNotNull(XyMode.COLOUR, XyMode.CRUSH, XyMode.SPACE, XyMode.ACID.takeIf { hasAcid && track.key == SongPart.ACID })
    }
    return base + listOf(XyMode.WOBBLE, XyMode.REPEAT).filter { it in extra }
}

/** A mode's chip, in words a beginner knows. */
fun xyModeLabel(mode: XyMode): String = when (mode) {
    XyMode.DJ_FILTER -> tr("Filtre DJ")
    XyMode.ACID -> tr("Acide")
    XyMode.SPACE -> tr("Espace")
    XyMode.CRUSH -> tr("Son cassé")
    XyMode.COLOUR -> tr("Couleur")
    XyMode.WOBBLE -> tr("Wobble")
    XyMode.REPEAT -> tr("Répéter")
}

/** The pages of the Live effects panel for the whole mix (a track has its own page). */
enum class LivePage { PAD, HOLDS, ROLLS, DUB, BUILD, SETTINGS }

/**
 * What the Live effects panel keeps between its tabs (held by the page, kept through a rotation:
 * [Saver]): the page and the track shown, the lock mode, the pad's mode for the whole mix and for
 * a track, where a finger holds the DJ filter, and the track buttons latched on by the lock (each
 * with its strip and band, to let it go; the whole mix's latched effects are the ViewModel's
 * locks, [LiveFxUi.locks]).
 */
@Stable
class LiveFxState {
    /** « Verrouiller »: a held effect stays after the finger lifts, the DJ filter stays where it is let go. */
    var locked by mutableStateOf(false)
    var page by mutableStateOf(LivePage.PAD)
    /** The track whose page shows (its mixer channel), -1: the whole mix's pages. */
    var target by mutableIntStateOf(-1)
    var mixMode by mutableStateOf(XyMode.DJ_FILTER)
    var trackMode by mutableStateOf(XyMode.COLOUR)

    /** Where a finger holds the DJ filter (0..1 each, y up); null: no finger. */
    var djAt by mutableStateOf<Offset?>(null)

    /** Track buttons latched on by the lock: their tag, to the strip and band ([CHOP]: the chopper). */
    private val latches = mutableStateMapOf<String, Pair<Int, Int>>()

    fun isLatched(key: String): Boolean = key in latches

    internal fun latch(key: String, ch: Int, band: Int) {
        latches[key] = ch to band
    }

    /** Let go by a touch: its strip and band (null: it was not latched). */
    internal fun unlatch(key: String): Pair<Int, Int>? = latches.remove(key)

    /** The ViewModel let it go (a Drop, Stop): the button no longer shows it kept. */
    internal fun forget(key: String) {
        latches.remove(key)
    }

    /**
     * Every latched effect let go: the track buttons through [release] (strip, band; [CHOP]: the
     * chopper), the whole mix's [locks] unlocked.
     */
    fun releaseAll(locks: Set<Int>, setLock: (Int, Boolean) -> Unit, release: (ch: Int, band: Int) -> Unit) {
        val all = latches.values.toList()
        latches.clear()
        all.forEach { (ch, band) -> release(ch, band) }
        djAt = null
        locks.forEach { setLock(it, false) }
    }

    /** « Tout remettre » already put everything at rest: only forget what was latched here. */
    fun forget() {
        latches.clear()
        djAt = null
    }

    companion object {
        /** A track's « Hachoir de voie » (its band, for [latch]). */
        const val CHOP = -1

        /** Kept through a rotation (the activity is recreated): everything but a finger's place. */
        val Saver: Saver<LiveFxState, Any> = listSaver(
            save = { s ->
                listOf(
                    s.locked, s.page.name, s.target, s.mixMode.name, s.trackMode.name,
                    ArrayList(s.latches.map { (k, v) -> "${v.first}|${v.second}|$k" }),
                )
            },
            restore = { l ->
                LiveFxState().apply {
                    locked = l[0] as Boolean
                    page = runCatching { LivePage.valueOf(l[1] as String) }.getOrDefault(LivePage.PAD)
                    target = l[2] as Int
                    mixMode = runCatching { XyMode.valueOf(l[3] as String) }.getOrDefault(XyMode.DJ_FILTER)
                    trackMode = runCatching { XyMode.valueOf(l[4] as String) }.getOrDefault(XyMode.COLOUR)
                    @Suppress("UNCHECKED_CAST")
                    (l[5] as List<String>).forEach { e ->
                        val parts = e.split('|', limit = 3)
                        val ch = parts.getOrNull(0)?.toIntOrNull()
                        val band = parts.getOrNull(1)?.toIntOrNull()
                        if (ch != null && band != null && parts.size == 3) latches[parts[2]] = ch to band
                    }
                }
            },
        )
    }
}

/**
 * The pads XY of the Live page showing now (each counts itself in while composed): the output
 * spectrum behind them is measured only then ([LiveScreenUi.spectrum]). Null: nobody counts.
 */
val LocalSpectrumUsers = staticCompositionLocalOf<MutableIntState?> { null }

/** This pad shows the spectrum behind it while it is on screen. */
@Composable
internal fun ShowsSpectrum() {
    val users = LocalSpectrumUsers.current ?: return
    DisposableEffect(users) {
        users.intValue++
        onDispose { users.intValue-- }
    }
}

/**
 * « Tout remettre » with the controls the page has today: the whole-mix effects at rest (filter
 * open, nothing repeated, braked or chopped), the returns as mixed, the 303 back to its saved
 * sound (a finger's take-over let go), every « Hachoir de voie » let go. During a take these
 * neutral values are written too. [LiveActions.resetAll] (one engine command with its short
 * fades) takes over once the engine has it.
 */
internal fun resetLiveEffects(ui: LiveScreenUi, actions: LiveActions) {
    actions.performance(Perf.REPEAT, 0f)
    actions.performance(Perf.DIVISION, AutoTarget.liveRest(AutoTarget.LIVE_DIVISION))
    actions.performance(Perf.TAPE_STOP, 0f)
    actions.performance(Perf.CHOP, 0f)
    actions.performance(Perf.FILTER, AutoTarget.liveRest(AutoTarget.LIVE_FILTER))
    actions.performance(Perf.RESONANCE, AutoTarget.liveRest(AutoTarget.LIVE_RESONANCE))
    actions.setReturn(0, AutoTarget.liveRest(AutoTarget.LIVE_REVERB_RETURN))
    actions.setReturn(1, AutoTarget.liveRest(AutoTarget.LIVE_ECHO_RETURN))
    AutoTarget.ACID_PARAMS.forEach { actions.holdAcid(it, false) }
    ui.acid.keys.toList().forEach(actions.resetAcid)
    ui.chops.toList().forEach { actions.trackChop(it, false) }
}

/** A track button the lock kept, let go: its chopper ([LiveFxState.CHOP]) or its band's kill. */
internal fun releaseTrackLatch(actions: LiveActions, ch: Int, band: Int) {
    if (band == LiveFxState.CHOP) actions.trackChop(ch, false) else actions.kill(band, false, ch)
}

/**
 * The row that never scrolls away: « Tout remettre » (all back to the saved sound), the lock and
 * the effects' settings (⚙). On a narrow panel the lock shows its padlock alone.
 */
@Composable
internal fun LiveFxSafety(ui: LiveScreenUi, actions: LiveActions, state: LiveFxState) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val narrow = maxWidth < 340.dp
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val red = Neon.Red
            Row(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(red.copy(alpha = 0.2f))
                    .border(1.5.dp, red, RoundedCornerShape(24.dp))
                    .clickable {
                        state.forget()
                        val reset = actions.resetAll
                        if (reset != null) reset() else resetLiveEffects(ui, actions)
                    }
                    .padding(horizontal = 10.dp).testTag("live_reset"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = Neon.TextPrimary, modifier = Modifier.size(20.dp))
                Text(tr("Tout remettre"), Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge, color = Neon.TextPrimary, maxLines = 1)
            }
            val on = state.locked
            val lockText = if (on) tr("Verrouillé") else tr("Verrouiller")
            Row(
                Modifier.height(48.dp).then(if (narrow) Modifier.width(48.dp) else Modifier).clip(RoundedCornerShape(24.dp))
                    .background(if (on) Neon.Amber else Neon.SurfaceHigh)
                    .clickable {
                        state.locked = !on
                        if (on) state.releaseAll(ui.fx.locks, actions.setLock) { ch, band -> releaseTrackLatch(actions, ch, band) }
                    }
                    .padding(horizontal = if (narrow) 0.dp else 14.dp).testTag("live_lock")
                    .semantics {
                        selected = on
                        if (narrow) contentDescription = lockText
                        stateDescription = if (on) tr("les effets restent après le doigt") else tr("les effets s'arrêtent quand le doigt part")
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                val ink = if (on) Color.Black else Neon.TextPrimary
                Icon(if (on) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                if (!narrow) Text(lockText, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1)
            }
            val settings = state.page == LivePage.SETTINGS && state.target < 0
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(if (settings) Neon.Cyan else Neon.SurfaceHigh)
                    .clickable {
                        state.target = -1
                        state.page = if (settings) LivePage.PAD else LivePage.SETTINGS
                    }
                    .testTag("live_fx_settings").semantics { selected = settings; contentDescription = tr("Réglages des effets") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = if (settings) Neon.OnAccent else Neon.TextPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** The pad's mode chips (48 dp each), above it. */
@Composable
internal fun XyModeRow(modes: List<XyMode>, chosen: XyMode, choose: (XyMode) -> Unit) {
    Row(Modifier.fillMaxWidth().fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        modes.forEach { m ->
            val selected = m == chosen
            Box(
                Modifier.height(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Neon.Cyan.copy(alpha = 0.9f) else Neon.SurfaceHigh.copy(alpha = 0.7f))
                    .clickable { choose(m) }.padding(horizontal = 12.dp).testTag("live_xy_mode_${m.name}")
                    .semantics { this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                Text(xyModeLabel(m), color = if (selected) Neon.OnAccent else Neon.TextPrimary, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

/**
 * A pad XY of the Live page: [at] where its dot is (0..1 each, y up; null: no dot), the output
 * spectrum faintly behind (read when drawn: the pad is redrawn, never recomposed, by the sound).
 * [hold] true when a finger lands, false when it leaves (also when the pad goes mid-gesture);
 * [move] with each position; [doubleTap] (two touches close together): back to its rest.
 */
@Composable
internal fun LiveXyPad(
    tag: String,
    description: String,
    accent: Color,
    height: Dp,
    top: String, left: String, bottom: String, right: String, topEnd: String?,
    at: () -> Offset?,
    spectrum: () -> FloatArray?,
    hold: (Boolean) -> Unit,
    move: (x: Float, y: Float) -> Unit,
    doubleTap: (() -> Unit)? = null,
) {
    val onHold by rememberUpdatedState(hold)
    val onMove by rememberUpdatedState(move)
    val onDouble by rememberUpdatedState(doubleTap)
    val lastUp = remember { longArrayOf(0L) }
    val lastAt = remember { arrayOf(Offset.Zero) }
    val grid = Neon.Outline
    val haze = Neon.Cyan
    SideEffect { liveXyPadCompositions++ }
    ShowsSpectrum()
    // Without a finger (TalkBack, switch access): a step each way, and back to its rest.
    val nudge: (Float, Float) -> Boolean = { dx, dy ->
        val p = at() ?: Offset(0.5f, 0.5f)
        onMove((p.x + dx).coerceIn(0f, 1f), (p.y + dy).coerceIn(0f, 1f))
        true
    }
    val steps = listOf(
        CustomAccessibilityAction(left) { nudge(-0.1f, 0f) },
        CustomAccessibilityAction(right) { nudge(0.1f, 0f) },
        CustomAccessibilityAction(top) { nudge(0f, 0.1f) },
        CustomAccessibilityAction(tr("Moins : %s", top)) { nudge(0f, -0.1f) },
        CustomAccessibilityAction(tr("Remettre")) {
            val back = onDouble
            if (back != null) back() else onHold(false)
            true
        },
    )
    XyLabelled(top = top, left = left, bottom = bottom, right = right, topEnd = topEnd) {
        Box(
            Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(14.dp)).background(Neon.Background)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .testTag(tag)
                .semantics {
                    contentDescription = description
                    customActions = steps
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val double = onDouble
                        if (double != null && isDoubleTouch(down.uptimeMillis, lastUp[0], down.position, lastAt[0], 48.dp.toPx())) {
                            double()
                            lastUp[0] = 0L
                            return@awaitEachGesture
                        }
                        fun send(o: Offset) = onMove((o.x / size.width).coerceIn(0f, 1f), xyValue(o.y, size.height.toFloat(), XY_INSET.toPx()))
                        onHold(true)
                        try {
                            send(down.position)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    lastUp[0] = change.uptimeMillis
                                    lastAt[0] = change.position
                                    break
                                }
                                if (change.positionChange() != Offset.Zero) send(change.position)
                                change.consume()
                            }
                        } finally {
                            onHold(false)
                        }
                    }
                },
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawSpectrumBackdrop(spectrum(), haze.copy(alpha = 0.13f))
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                for (i in 1 until 4) {
                    drawLine(grid.copy(alpha = 0.5f), Offset(size.width * i / 4, 0f), Offset(size.width * i / 4, size.height), strokeWidth = 1f, pathEffect = dash)
                    drawLine(grid.copy(alpha = 0.5f), Offset(0f, size.height * i / 4), Offset(size.width, size.height * i / 4), strokeWidth = 1f, pathEffect = dash)
                }
                val p = at() ?: return@Canvas
                val c = Offset(p.x * size.width, xyPosition(p.y, size.height, XY_INSET.toPx()))
                drawLine(accent.copy(alpha = 0.35f), Offset(c.x, 0f), Offset(c.x, size.height), strokeWidth = 1.5f)
                drawLine(accent.copy(alpha = 0.35f), Offset(0f, c.y), Offset(size.width, c.y), strokeWidth = 1.5f)
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.5f), Color.Transparent), center = c, radius = 110f), radius = 110f, center = c)
                drawCircle(accent, radius = 22f, center = c)
                drawCircle(Color.White.copy(alpha = 0.85f), radius = 8f, center = c)
            }
        }
    }
}

/**
 * When a press takes effect: the next line of the grid ([steps] sixteenths), from the transport
 * [clock]. Set by a touch, read when drawn (the clock redraws the ring, never recomposes it).
 */
@Stable
class StartWait(private val clock: () -> Int) {
    private var at by mutableIntStateOf(-1)
    private var steps by mutableIntStateOf(0)

    /** A touch now: the start waits for the next line of [lineSteps] (none under a beat: too short to see). */
    fun arm(lineSteps: Int) {
        val s = clock()
        if (lineSteps < 4 || s < 0) {
            at = -1
            return
        }
        steps = lineSteps
        at = (s / lineSteps + 1) * lineSteps
    }

    fun clear() {
        at = -1
    }

    /** The part of the wait left (0..1), null: nothing waits. */
    fun left(): Float? {
        val a = at
        if (a < 0) return null
        val s = clock()
        if (s < 0 || s >= a) return null
        return ((a - s).toFloat() / steps).coerceIn(0f, 1f)
    }
}

/**
 * A hold button of the Live page. Not [locking]: on while the finger stays ([hold] true, then
 * false). [locking] (the lock is on): a touch turns it on and, let go, it stays ([latch]; a small
 * padlock on it, [latched]); the next touch lets it go ([unlatch]). [active]: on from elsewhere
 * (the ViewModel's state). Lit, it glows. [wait]: its start waits for the grid's next line (a ring).
 */
@Composable
internal fun LatchButton(
    label: String,
    modifier: Modifier,
    tag: String,
    locking: Boolean,
    latched: Boolean,
    height: Dp = 56.dp,
    active: Boolean = false,
    wait: StartWait? = null,
    waitSteps: Int = 0,
    hold: (Boolean) -> Unit,
    latch: () -> Unit,
    unlatch: () -> Unit,
) {
    var held by remember { mutableStateOf(false) }
    // The press loop outlives recompositions: it calls the latest callbacks (a new target), and a
    // release goes to the same [hold] as its press.
    val currentHold by rememberUpdatedState(hold)
    val isLatched by rememberUpdatedState(latched)
    val isLocking by rememberUpdatedState(locking)
    val doLatch by rememberUpdatedState(latch)
    val doUnlatch by rememberUpdatedState(unlatch)
    val lit = held || active || latched
    val glow = Neon.Acid
    val shape = RoundedCornerShape(12.dp)
    SideEffect { latchButtonCompositions++ }
    Box(
        modifier
            .height(height)
            .then(if (lit) Modifier.shadow(10.dp, shape, clip = false, ambientColor = glow, spotColor = glow) else Modifier)
            .clip(shape)
            .background(if (lit) glow else Neon.SurfaceHigh)
            .then(if (wait != null) Modifier.countdownRing({ wait.left() }, if (lit) Neon.Background else glow) else Modifier)
            .testTag(tag)
            .semantics {
                selected = lit
                if (latched) stateDescription = tr("verrouillé")
                // Without a finger to hold it (TalkBack, switch access): a touch turns it on and
                // keeps it, the next one lets it go.
                onClick(label = if (lit) tr("Arrêter") else tr("Activer")) {
                    when {
                        isLatched -> doUnlatch()
                        active -> currentHold(false)
                        else -> {
                            currentHold(true)
                            doLatch()
                        }
                    }
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    held = true
                    if (isLatched) {
                        // Latched: this touch lets it go.
                        val release = doUnlatch
                        try {
                            tryAwaitRelease()
                        } finally {
                            held = false
                            wait?.clear()
                            release()
                        }
                        return@detectTapGestures
                    }
                    val pressed = currentHold
                    wait?.arm(waitSteps)
                    pressed(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        // Also when the button leaves the screen while held: unlocked, the effect stops.
                        held = false
                        if (isLocking) doLatch() else {
                            wait?.clear()
                            pressed(false)
                        }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, Modifier.padding(horizontal = 4.dp), color = if (lit) Neon.Background else Neon.TextPrimary, style = MaterialTheme.typography.titleSmall,
            maxLines = 2, textAlign = TextAlign.Center,
        )
        if (latched) {
            Icon(
                Icons.Filled.Lock, contentDescription = null, tint = Neon.Background,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(14.dp),
            )
        }
    }
}

/**
 * A ring around the edge that empties as a start comes: [left] the part of the wait left (0..1;
 * null: nothing waits), read when drawn (the clock redraws it, never recomposes it).
 */
internal fun Modifier.countdownRing(left: () -> Float?, color: Color): Modifier = drawWithContent {
    drawContent()
    val f = left() ?: return@drawWithContent
    val w = 4.dp.toPx()
    val d = minOf(size.width, size.height) - w * 2
    val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
    drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, topLeft, Size(d, d), style = Stroke(w))
    drawArc(color, -90f, 360f * f.coerceIn(0f, 1f), false, topLeft, Size(d, d), style = Stroke(w))
}

/** The part of the wait left until the next launch line ([quantum] steps), from the transport [step]. */
fun launchWaitLeft(step: Int, quantum: Int): Float = if (step < 0 || quantum <= 0) 1f else 1f - (step % quantum) / quantum.toFloat()

/** Tests: how many times a hold button was composed (the clock must only redraw it). */
@VisibleForTesting
internal var latchButtonCompositions = 0

/** Tests: how many times a Live pad XY was composed (the spectrum must only redraw it). */
@VisibleForTesting
internal var liveXyPadCompositions = 0
