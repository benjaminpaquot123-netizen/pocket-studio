package com.pocketstudio.app.live

import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.mixer.MacroKnobs
import com.pocketstudio.app.mixer.XyPad
import com.pocketstudio.app.session.Perf
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.LiveDivision
import com.pocketstudio.model.LiveFxSettings
import com.pocketstudio.model.Macros
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.i18n.percent
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ProSlider
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.fadingVerticalEdges
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt

/** The pads' height on the Live page (their ends are written above and under, about 34 dp). */
private val PAD_HEIGHT = 132.dp

/**
 * Effects under the fingers (lot 4d, in sections). With [tools] on top: « Tout remettre », the lock
 * and the settings (⚙) (the Live page's open panel has them as icons in its header instead).
 * « L'effet »: what the pad does (Filtre DJ, Acide, Espace…) for the whole mix, then « Pistes : »
 * and a chip per track (its own pad and buttons). « Comment il joue »: Appui (the pad), Tenu,
 * Répéter, Dub, Montée. Then the page. Each whole-mix page fits a phone's panel without scrolling;
 * a track's page scrolls under its pad.
 */
@Composable
fun LiveFx(
    ui: LiveScreenUi, columns: List<LiveColumn>, actions: LiveActions, modifier: Modifier,
    state: LiveFxState = remember { LiveFxState() }, tools: Boolean = true,
) {
    val column = columns.firstOrNull { it.channel == state.target }
    if (column == null && state.target >= 0) state.target = -1
    // The clock, read only when the waiting rings are drawn.
    val clockNow by rememberUpdatedState(ui.clockOf)
    val clock: () -> Int = remember { { clockNow() } }
    BoxWithConstraints(modifier) {
        // A short panel (a big text size, a small phone): no titles, the tools share the effects'
        // line, icons only, so that the pad keeps its room.
        val short = maxHeight < SHORT_PANEL
        Column(Modifier.fillMaxSize().testTag("live_fx"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                tools && short -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LiveFxSafety(ui, actions, state, Modifier, iconsOnly = true)
                    EffectRow(ui, columns, column, state, Modifier.weight(1f))
                }
                tools -> {
                    LiveFxSafety(ui, actions, state)
                    SectionTitle(tr("L'effet"))
                    EffectRow(ui, columns, column, state)
                }
                else -> EffectRow(ui, columns, column, state)
            }
            if (!short) SectionTitle(tr("Comment il joue"))
            PlayRow(state)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when {
                    column != null -> Scrolled { TrackFx(ui, column, actions, state, clock) }
                    state.page == LivePage.HOLDS -> HoldsPage(ui, actions, state, clock)
                    state.page == LivePage.ROLLS -> RollsPage(ui, actions, state)
                    state.page == LivePage.DUB -> DubPage(ui, actions, state, clock)
                    state.page == LivePage.BUILD -> BuildPage(ui, actions, clock)
                    state.page == LivePage.SETTINGS -> Scrolled { SettingsPage(ui.project.liveFx, actions.setFxSettings) }
                    else -> Scrolled { PadPage(ui, actions, state) }
                }
            }
        }
    }
}

/** Below this height the effects lose their titles and the tools share a line. */
private val SHORT_PANEL = 300.dp

@Composable
private fun Scrolled(content: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().fadingVerticalEdges(scroll).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** A section's title (« L'EFFET », « COMMENT IL JOUE »): a small accent bar, the words in capitals. */
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(3.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(Neon.Cyan))
        Text(
            text.uppercase(), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Neon.TextSecondary, maxLines = 1,
            letterSpacing = 1.sp,
        )
    }
}

/** A track's pad modes (its Wobble once that effect is on the track). */
private fun trackModes(ui: LiveScreenUi, c: LiveColumn): List<XyMode> {
    val channel = ui.project.mixer.channels.getOrNull(c.channel)
    val wobble = channel?.inserts?.any { it.effect == EffectType.WOBBLE && !it.bypass } == true
    return xyModesFor(c, ui.hasAcid, ui.extraModes + if (wobble) setOf(XyMode.WOBBLE) else emptySet())
}

/**
 * « L'effet »: the pad's modes (the whole mix's, or the chosen track's), then « Pistes : » and a
 * chip per track. A mode of the whole mix brings the pad back (« Appui »).
 */
@Composable
private fun EffectRow(ui: LiveScreenUi, columns: List<LiveColumn>, column: LiveColumn?, state: LiveFxState, modifier: Modifier = Modifier.fillMaxWidth()) {
    Row(modifier.systemGestureExclusion().fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (column != null) {
            val modes = trackModes(ui, column)
            val mode = state.trackMode.takeIf { it in modes } ?: XyMode.COLOUR
            modes.forEach { m -> Chip(xyModeLabel(m), m == mode, Neon.Cyan, "live_xy_mode_${m.name}") { state.trackMode = m } }
        } else {
            val modes = xyModesFor(null, ui.hasAcid, ui.extraModes)
            val mode = state.mixMode.takeIf { it in modes } ?: XyMode.DJ_FILTER
            modes.forEach { m ->
                Chip(xyModeLabel(m), state.page == LivePage.PAD && m == mode, Neon.Cyan, "live_xy_mode_${m.name}") {
                    state.page = LivePage.PAD
                    state.mixMode = m
                }
            }
        }
        Text(tr("Pistes :"), Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1)
        columns.forEach { c ->
            Chip(c.name, state.target == c.channel, Neon.Magenta, "live_fx_${c.key}", dot = c.color) { state.target = c.channel }
        }
    }
}

/** « Comment il joue », one segmented control: Appui (the pad), Tenu, Répéter, Dub, Montée (the whole mix). */
@Composable
private fun PlayRow(state: LiveFxState) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = 0.25f)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(
            LivePage.PAD to tr("Appui"), LivePage.HOLDS to tr("Tenu"), LivePage.ROLLS to tr("Répéter"),
            LivePage.DUB to tr("Dub"), LivePage.BUILD to tr("Montée"),
        ).forEach { (page, label) ->
            val selected = state.target < 0 && state.page == page
            Box(
                Modifier.weight(label.length + 3f).fillMaxHeight().clip(RoundedCornerShape(19.dp))
                    .background(if (selected) Neon.SurfaceHigh else Color.Transparent)
                    .clickable {
                        state.target = -1
                        state.page = page
                    }.testTag("live_page_${page.name}").semantics { this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                val style = MaterialTheme.typography.labelLarge
                Text(
                    label, color = if (selected) Neon.TextPrimary else Neon.TextSecondary, style = style, maxLines = 1, softWrap = false,
                    autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, accent: Color, tag: String, dot: Color? = null, choose: () -> Unit) {
    Row(
        Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (selected) accent else Neon.SurfaceHigh)
            .clickable(onClick = choose).padding(horizontal = 12.dp).testTag(tag).semantics { this.selected = selected },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) Box(Modifier.padding(end = 6.dp).size(8.dp).clip(CircleShape).background(dot))
        Text(label, color = if (selected) Neon.OnAccent else Neon.TextPrimary, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

// --- Pad -------------------------------------------------------------------------------------

@Composable
private fun PadPage(ui: LiveScreenUi, actions: LiveActions, state: LiveFxState) {
    // Its mode is chosen in « L'effet », above.
    val modes = xyModesFor(null, ui.hasAcid, ui.extraModes)
    val mode = state.mixMode.takeIf { it in modes } ?: XyMode.DJ_FILTER
    when (mode) {
        XyMode.ACID -> AcidXy(ui, actions, PAD_HEIGHT)
        XyMode.SPACE -> ReturnsXy(ui, actions)
        else -> DjFilterXy(ui, actions, state)
    }
}

/**
 * The DJ filter on the whole mix: left low-pass, right high-pass, up resonance. Let go, it springs
 * back to neutral (it never stays on the mix by accident) unless the lock keeps it there; kept,
 * two touches (or a new touch) take it back.
 */
@Composable
private fun DjFilterXy(ui: LiveScreenUi, actions: LiveActions, state: LiveFxState) {
    val kept = AutoTarget.LIVE_FILTER in ui.fx.locks
    val at = state.djAt ?: if (kept) {
        Offset(ui.fx.value(AutoTarget.LIVE_FILTER, AutoTarget.liveRest(AutoTarget.LIVE_FILTER)), ui.fx.value(AutoTarget.LIVE_RESONANCE, AutoTarget.liveRest(AutoTarget.LIVE_RESONANCE)))
    } else {
        null
    }
    val description = if (at == null) tr("Filtre DJ : ouvert") else tr("Filtre DJ : %1\$s, résonance %2\$s", percent((at.x * 100).roundToInt()), percent((at.y * 100).roundToInt()))
    val unlock = {
        // Unlocked, the ViewModel brings the filter and its resonance back to rest.
        actions.setLock(AutoTarget.LIVE_FILTER, false)
        actions.setLock(AutoTarget.LIVE_RESONANCE, false)
    }
    val locks by rememberUpdatedState(ui.fx.locks)
    LiveXyPad(
        tag = "xy_pad", description = description, accent = Neon.Acid, height = PAD_HEIGHT,
        top = tr("↑ Résonance"), left = tr("← Passe-bas"), bottom = tr("Filtre"), right = tr("Passe-haut →"),
        topEnd = if (kept || state.locked) tr("Verrouillé : il reste en place") else tr("Lâche : tout revient"),
        at = { at }, spectrum = ui.spectrum,
        hold = { on ->
            if (on) {
                // A new touch on a kept filter takes it over (the finger plays it again).
                if (AutoTarget.LIVE_FILTER in locks) unlock()
            } else {
                state.djAt = null
                if (state.locked) {
                    actions.setLock(AutoTarget.LIVE_FILTER, true)
                    actions.setLock(AutoTarget.LIVE_RESONANCE, true)
                } else {
                    actions.performance(Perf.FILTER, AutoTarget.liveRest(AutoTarget.LIVE_FILTER))
                    actions.performance(Perf.RESONANCE, AutoTarget.liveRest(AutoTarget.LIVE_RESONANCE))
                }
            }
        },
        move = { x, y ->
            state.djAt = Offset(x, y)
            actions.performance(Perf.RESONANCE, y)
            actions.performance(Perf.FILTER, x)
        },
        doubleTap = if (kept) unlock else null,
    )
}

/** Espace on the whole mix: the reverb return (left to right) × the echo return (bottom to top); two touches: as mixed. */
@Composable
private fun ReturnsXy(ui: LiveScreenUi, actions: LiveActions) {
    val reverb = ui.returns.getOrElse(0) { 0.5f }
    val echo = ui.returns.getOrElse(1) { 0.5f }
    LiveXyPad(
        tag = "live_xy_space", description = tr("Pad Espace : réverbe %1\$s, écho %2\$s", percent((reverb * 200).roundToInt()), percent((echo * 200).roundToInt())),
        accent = Neon.Magenta, height = PAD_HEIGHT,
        top = tr("↑ Plus d'écho"), left = tr("← Moins de réverbe"), bottom = tr("↓ Moins d'écho"), right = tr("Plus de réverbe →"),
        topEnd = tr("Deux touchers : comme au mix"),
        at = { Offset(reverb, echo) }, spectrum = ui.spectrum,
        hold = {},
        move = { x, y ->
            actions.setReturn(0, x)
            actions.setReturn(1, y)
        },
        doubleTap = {
            actions.setReturn(0, AutoTarget.liveRest(AutoTarget.LIVE_REVERB_RETURN))
            actions.setReturn(1, AutoTarget.liveRest(AutoTarget.LIVE_ECHO_RETURN))
        },
    )
}

// --- Tenus -------------------------------------------------------------------------------------

/** A held effect of the whole mix: the ViewModel's hold, its lock when the lock is on, its wait for the grid. */
@Composable
private fun FxHold(label: String, param: Int, tag: String, ui: LiveScreenUi, actions: LiveActions, state: LiveFxState, clock: () -> Int, modifier: Modifier) {
    val wait = remember { StartWait(clock) }
    LatchButton(
        label, modifier, tag, locking = state.locked, latched = param in ui.fx.locks, active = ui.fx.on(param),
        wait = wait, waitSteps = ui.project.liveFx.gridSteps,
        hold = { on -> actions.hold(param, on) },
        latch = { actions.setLock(param, true) },
        unlatch = { actions.setLock(param, false) },
    )
}

/** « Tenus »: nine held effects of the whole mix, three by three; the kills on the last row. */
@Composable
private fun HoldsPage(ui: LiveScreenUi, actions: LiveActions, state: LiveFxState, clock: () -> Int) {
    val rows = listOf(
        listOf(
            Triple(tr("Freinage"), AutoTarget.LIVE_TAPE_STOP, "live_hold_brake"),
            Triple(tr("Rembobiner"), AutoTarget.LIVE_BACKSPIN, "live_hold_backspin"),
            Triple(tr("À l'envers"), AutoTarget.LIVE_REVERSE, "live_hold_reverse"),
        ),
        listOf(
            Triple(tr("Hachoir"), AutoTarget.LIVE_CHOP, "live_hold_chop"),
            Triple(tr("Demi-tempo"), AutoTarget.LIVE_HALF_TIME, "live_hold_half"),
            Triple(tr("Sortie en écho"), AutoTarget.LIVE_ECHO_OUT, "live_hold_echo_out"),
        ),
        listOf(
            Triple(tr("Sans graves"), AutoTarget.LIVE_KILL_LOW, "live_kill_0"),
            Triple(tr("Sans médiums"), AutoTarget.LIVE_KILL_MID, "live_kill_1"),
            Triple(tr("Sans aigus"), AutoTarget.LIVE_KILL_HIGH, "live_kill_2"),
        ),
    )
    Column(Modifier.fillMaxWidth().testTag("live_holds"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, param, tag) -> FxHold(label, param, tag, ui, actions, state, clock, Modifier.weight(1f)) }
            }
        }
    }
}

// --- Répéter (roll pads) -----------------------------------------------------------------------

/**
 * Six roll pads (1/4, 1/8, 1/16, 1/32, 1/8T, 1/16T): the whole mix repeats while a finger stays
 * on one, sliding to another changes it; with the lock, it keeps rolling until touched again.
 */
@Composable
private fun RollsPage(ui: LiveScreenUi, actions: LiveActions, state: LiveFxState) {
    val rolls = LiveDivision.ROLLS
    val rolling = if (ui.fx.on(AutoTarget.LIVE_REPEAT)) LiveDivision.ofValue(ui.fx.value(AutoTarget.LIVE_DIVISION, AutoTarget.liveRest(AutoTarget.LIVE_DIVISION))) else null
    val latched = AutoTarget.LIVE_REPEAT in ui.fx.locks
    val isLatched by rememberUpdatedState(latched)
    val locking by rememberUpdatedState(state.locked)
    val act by rememberUpdatedState(actions)
    val gap = 8.dp
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.fillMaxWidth().testTag("live_rolls").pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val gapPx = gap.toPx()
                    fun at(o: Offset): LiveDivision? {
                        if (o.x < 0 || o.y < 0 || o.x > size.width || o.y > size.height) return null
                        val col = ((o.x + gapPx / 2) / ((size.width + gapPx) / 3f)).toInt().coerceIn(0, 2)
                        val row = ((o.y + gapPx / 2) / ((size.height + gapPx) / 2f)).toInt().coerceIn(0, 1)
                        return rolls[row * 3 + col]
                    }
                    if (isLatched) {
                        // Kept rolling: this touch lets it go.
                        do {
                            val e = awaitPointerEvent()
                        } while (e.changes.any { it.pressed })
                        act.setLock(AutoTarget.LIVE_REPEAT, false)
                        return@awaitEachGesture
                    }
                    var current = at(down.position)
                    current?.let { act.roll(it) }
                    try {
                        while (true) {
                            val e = awaitPointerEvent()
                            val change = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val d = at(change.position)
                            if (d != null && d != current) {
                                current = d
                                act.roll(d)
                            }
                            change.consume()
                        }
                    } finally {
                        if (current != null) {
                            if (locking) act.setLock(AutoTarget.LIVE_REPEAT, true) else act.roll(null)
                        }
                    }
                }
            },
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            rolls.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { d ->
                        // Without a finger (TalkBack, switch access): a touch rolls it, the next one stops.
                        RollPad(d, d == rolling, latched && d == rolling, Modifier.weight(1f)) {
                            when {
                                d == rolling && latched -> act.setLock(AutoTarget.LIVE_REPEAT, false)
                                d == rolling -> act.roll(null)
                                else -> act.roll(d)
                            }
                        }
                    }
                }
            }
        }
        Text(
            tr("Garde le doigt sur un pad : ça répète en rythme. Glisse vers un autre pour changer."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
    }
}

@Composable
private fun RollPad(d: LiveDivision, lit: Boolean, latched: Boolean, modifier: Modifier, toggle: () -> Unit) {
    val glow = Neon.Acid
    val shape = RoundedCornerShape(12.dp)
    val said = tr("Répéter %s", d.label) + if (latched) " · " + tr("verrouillé") else ""
    Box(
        modifier.height(64.dp)
            .then(if (lit) Modifier.shadow(10.dp, shape, clip = false, ambientColor = glow, spotColor = glow) else Modifier)
            .clip(shape).background(if (lit) glow else Neon.SurfaceHigh)
            .testTag("live_roll_${d.name}")
            .semantics {
                selected = lit
                contentDescription = said
                onClick(label = if (lit) tr("Arrêter") else tr("Activer")) { toggle(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(d.label, style = MaterialTheme.typography.titleMedium, color = if (lit) Neon.Background else Neon.TextPrimary)
    }
}

// --- Dub ---------------------------------------------------------------------------------------

/**
 * A one-shot button: [fire] now; its effect starts on the next line of [waitSteps] sixteenths (a
 * bar empties along its bottom edge until then).
 */
@Composable
private fun TapButton(label: String, tag: String, clock: () -> Int, waitSteps: Int, modifier: Modifier, height: Dp = 56.dp, fire: () -> Unit) {
    val wait = remember { StartWait(clock) }
    val accent = Neon.Acid
    Box(
        modifier.height(height).clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh)
            .countdownBar({ wait.left() }, accent)
            .clickable {
                wait.arm(waitSteps)
                fire()
            }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.titleSmall, color = Neon.TextPrimary, maxLines = 2, textAlign = TextAlign.Center)
    }
}

/** « Dub »: the reverb frozen while held, a throw into the reverb or the echo, the two returns. */
@Composable
private fun DubPage(ui: LiveScreenUi, actions: LiveActions, state: LiveFxState, clock: () -> Int) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FxHold(tr("Réverbe figée"), AutoTarget.LIVE_FREEZE, "live_hold_freeze", ui, actions, state, clock, Modifier.weight(1f))
            val beat = if (ui.project.liveFx.gridSteps > 0) 4 else 0
            TapButton(tr("Lancer dans la réverbe"), "live_throw_0", clock, beat, Modifier.weight(1f)) { actions.throwFx(0, Mixer.MASTER) }
            TapButton(tr("Lancer dans l'écho"), "live_throw_1", clock, beat, Modifier.weight(1f)) { actions.throwFx(1, Mixer.MASTER) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReturnSlider(tr("Réverbe"), ui.returns.getOrElse(0) { 0.5f }, Modifier.weight(1f), "live_return_0") { actions.setReturn(0, it) }
            ReturnSlider(tr("Écho"), ui.returns.getOrElse(1) { 0.5f }, Modifier.weight(1f), "live_return_1") { actions.setReturn(1, it) }
        }
    }
}

/** A return: 0 none, the middle as mixed, the top twice as much. */
@Composable
private fun ReturnSlider(label: String, value: Float, modifier: Modifier, tag: String, change: (Float) -> Unit) {
    Column(modifier) {
        Text(tr("%1\$s : %2\$s", label, percent((value * 200).roundToInt())), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1)
        ProSlider(value, change, Modifier.fillMaxWidth().height(48.dp).testTag(tag), accent = Neon.Magenta)
    }
}

// --- Montée ------------------------------------------------------------------------------------

/**
 * « Montée »: one wide slider for the build-up, the big « Drop » (everything back on the next bar
 * line; armed, it lights and empties to that line) and the noise rising or falling.
 */
@Composable
private fun BuildPage(ui: LiveScreenUi, actions: LiveActions, clock: () -> Int) {
    val bar = ui.project.stepsPerBar
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WideSlider(tr("Montée"), ui.fx.value(AutoTarget.LIVE_BUILD_UP), "live_build_up", Neon.Amber, actions.buildUp)
        val armed = ui.fx.dropArmed
        val amber = Neon.Amber
        Box(
            Modifier.fillMaxWidth().height(64.dp)
                .then(if (armed) Modifier.shadow(12.dp, RoundedCornerShape(14.dp), clip = false, ambientColor = amber, spotColor = amber) else Modifier)
                .clip(RoundedCornerShape(14.dp)).background(if (armed) amber else Neon.Red.copy(alpha = 0.85f))
                .countdownBar({ if (armed) launchWaitLeft(clock(), bar) else null }, Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = actions.drop)
                .testTag("live_drop").semantics { selected = armed },
            contentAlignment = Alignment.Center,
        ) {
            // Armed: the longer words on two lines if need be (a narrow panel), never cut.
            Text(
                if (armed) tr("Drop à la prochaine mesure…") else tr("Drop"),
                Modifier.padding(horizontal = 8.dp),
                style = if (armed) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = if (armed) Color.Black else Color.White, maxLines = 2, textAlign = TextAlign.Center,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TapButton(tr("↗ Bruit qui monte"), "live_riser", clock, bar, Modifier.weight(1f)) { actions.riser(false) }
            TapButton(tr("↘ Bruit qui descend"), "live_downlifter", clock, bar, Modifier.weight(1f)) { actions.riser(true) }
        }
    }
}

/** A wide slider for a finger (56 dp): filled from the left, its name and value inside; it stays where it is let go. */
@Composable
private fun WideSlider(label: String, value: Float, tag: String, accent: Color, set: (Float) -> Unit) {
    val change by rememberUpdatedState(set)
    val rail = Neon.SurfaceHigh
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).testTag(tag)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
                setProgress { v -> change(v.coerceIn(0f, 1f)); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    change((down.position.x / size.width).coerceIn(0f, 1f))
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        change((c.position.x / size.width).coerceIn(0f, 1f))
                        c.consume()
                    }
                }
            }
            .drawWithContent {
                val r = CornerRadius(14.dp.toPx())
                drawRoundRect(rail, cornerRadius = r)
                if (value > 0f) drawRoundRect(accent.copy(alpha = 0.75f), size = Size(size.width * value.coerceIn(0f, 1f), size.height), cornerRadius = r)
                drawContent()
            },
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(percent((value * 100).roundToInt()), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** A bar along the bottom edge that empties as a start comes ([left]: 0..1, null: nothing waits; read when drawn). */
internal fun Modifier.countdownBar(left: () -> Float?, color: Color): Modifier = drawWithContent {
    drawContent()
    val f = left() ?: return@drawWithContent
    val h = 5.dp.toPx()
    drawRect(color, Offset(0f, size.height - h), Size(size.width * f.coerceIn(0f, 1f), h))
}

// --- Réglages ----------------------------------------------------------------------------------

/** A length in beats: « 1/4 temps », « 1 temps », « 2 temps ». */
internal fun beatsLabel(beats: Double): String = when {
    beats == 1.0 -> tr("1 temps")
    beats == 0.25 -> tr("%s temps", "1/4")
    beats == 0.5 -> tr("%s temps", "1/2")
    else -> tr("%d temps", beats.roundToInt())
}

/** The effects' settings (⚙): when held effects start, how they end, their lengths. */
@Composable
private fun SettingsPage(s: LiveFxSettings, set: (LiveFxSettings) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("live_fx_settings_page"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Choices(tr("Un effet tenu démarre"), "grid", s.grid, listOf(tr("Tout de suite"), tr("À la double croche"), tr("Au temps"))) { set(s.copy(grid = it)) }
        Choices(
            tr("Quand on lâche Hachoir, À l'envers, Demi-tempo ou Répéter"), "release", s.release,
            listOf(tr("Ça s'arrête"), tr("Écho"), tr("Freinage"), tr("Rembobinage")),
        ) { set(s.copy(release = it)) }
        Choices(tr("Durée du freinage"), "brake", s.brake, LiveFxSettings.BRAKE_BEATS.map(::beatsLabel)) { set(s.copy(brake = it)) }
        Choices(
            tr("Lâché, le freinage repart"), "restart", s.restart,
            LiveFxSettings.RESTART_BEATS.map { if (it == 0.0) tr("D'un coup") else beatsLabel(it) },
        ) { set(s.copy(restart = it)) }
        Choices(tr("Durée du rembobinage"), "backspin", s.backspin, LiveFxSettings.BACKSPIN_BEATS.map(::beatsLabel)) { set(s.copy(backspin = it)) }
        Choices(tr("Bout joué à l'envers"), "reverse", s.reverse, listOf("1/16", "1/8", "1/4")) { set(s.copy(reverse = it)) }
        Choices(tr("Hachoir"), "chop", LiveDivision.ROLLS.indexOf(s.chop), LiveDivision.ROLLS.map { it.label }) { set(s.copy(chop = LiveDivision.ROLLS[it])) }
        Choices(tr("Demi-tempo"), "half", if (s.halfTimeBar) 1 else 0, listOf(tr("Par temps"), tr("Par mesure"))) { set(s.copy(halfTimeBar = it == 1)) }
        Choices(
            tr("Bruit qui monte ou descend"), "riser", LiveFxSettings.RISER_BARS.indexOf(s.riserBars),
            LiveFxSettings.RISER_BARS.map { trCount(it, "1 mesure", "%d mesures") },
        ) { set(s.copy(riserBars = LiveFxSettings.RISER_BARS[it])) }
    }
}

@Composable
private fun Choices(title: String, key: String, chosen: Int, labels: List<String>, choose: (Int) -> Unit) {
    Text(title, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary)
    Row(Modifier.fillMaxWidth().fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            FilterChip(
                selected = i == chosen, onClick = { choose(i) }, label = { Text(label, maxLines = 1) },
                colors = pillColors(Neon.Cyan), modifier = Modifier.testTag("live_set_${key}_$i"),
            )
        }
    }
}

// --- A track -----------------------------------------------------------------------------------

@Composable
private fun TrackFx(ui: LiveScreenUi, c: LiveColumn, actions: LiveActions, state: LiveFxState, clock: () -> Int) {
    val channel = ui.project.mixer.channels.getOrNull(c.channel) ?: return
    val wobble = channel.inserts.indexOfFirst { it.effect == EffectType.WOBBLE && !it.bypass }
    // Its mode is chosen in « L'effet », above.
    val modes = trackModes(ui, c)
    val mode = state.trackMode.takeIf { it in modes } ?: XyMode.COLOUR
    when (mode) {
        XyMode.CRUSH -> {
            val dirt = channel.macros[Macros.DIRT]
            val colour = channel.macros[Macros.COLOR]
            LiveXyPad(
                tag = "live_xy_crush_${c.key}",
                description = tr("Pad Son cassé : saleté %1\$s, couleur %2\$s", percent((dirt * 100).roundToInt()), percent((colour * 100).roundToInt())),
                accent = Neon.Amber, height = PAD_HEIGHT,
                top = tr("↑ Brillant"), left = tr("← Propre"), bottom = tr("↓ Sombre"), right = tr("Sale →"),
                topEnd = tr("Deux touchers : son de départ"),
                at = { Offset(dirt, colour) }, spectrum = ui.spectrum,
                hold = {},
                move = { x, y -> actions.fx.setMacroPair(c.channel, Macros.DIRT, x, Macros.COLOR, y) },
                doubleTap = { actions.fx.setMacroPair(c.channel, Macros.DIRT, Macros.DEFAULTS[Macros.DIRT], Macros.COLOR, Macros.DEFAULTS[Macros.COLOR]) },
            )
        }
        XyMode.SPACE -> {
            val reverb = channel.sends.getOrElse(0) { 0f }
            val echo = channel.sends.getOrElse(1) { 0f }
            LiveXyPad(
                tag = "live_xy_sends_${c.key}",
                description = tr("Pad Espace : envoi réverbe %1\$s, envoi écho %2\$s", percent((reverb * 100).roundToInt()), percent((echo * 100).roundToInt())),
                accent = Neon.Magenta, height = PAD_HEIGHT,
                top = tr("↑ Plus d'écho"), left = tr("← Sec"), bottom = tr("↓ Moins d'écho"), right = tr("Plus de réverbe →"),
                topEnd = null,
                at = { Offset(reverb, echo) }, spectrum = ui.spectrum,
                hold = {},
                move = { x, y ->
                    actions.setSend(c.channel, 0, x)
                    actions.setSend(c.channel, 1, y)
                },
            )
        }
        XyMode.WOBBLE -> {
            val p = channel.inserts[wobble].params
            val rate = p.getOrElse(0) { 0.5f }
            val depth = p.getOrElse(1) { 0.7f }
            LiveXyPad(
                tag = "live_xy_wobble_${c.key}",
                description = tr("Pad Wobble : vitesse %1\$s, profondeur %2\$s", percent((rate * 100).roundToInt()), percent((depth * 100).roundToInt())),
                accent = Neon.Cyan, height = PAD_HEIGHT,
                top = tr("↑ Plus profond"), left = tr("← Lent"), bottom = tr("↓ Léger"), right = tr("Rapide →"),
                topEnd = null,
                at = { Offset(rate, depth) }, spectrum = ui.spectrum,
                hold = {},
                move = { x, y ->
                    actions.setInsertParam(c.channel, wobble, 0, x)
                    actions.setInsertParam(c.channel, wobble, 1, y)
                },
            )
        }
        XyMode.ACID -> AcidXy(ui, actions, PAD_HEIGHT)
        else -> {
            ShowsSpectrum()
            XyPad(
                c.channel, channel.macros, actions.fx, height = PAD_HEIGHT, hint = false, background = Neon.Background, backdrop = ui.spectrum,
                // Saleté and Espace have their own modes: here the colour × the movement.
                yFixed = Macros.MOTION,
            )
        }
    }
    // Held: the track chopped in sixteenths, its bands cut; a touch: thrown into the reverb or echo.
    TrackLatch(tr("Hachoir de voie"), "live_track_chop_${c.key}", c.channel in ui.chops, state, c.channel, LiveFxState.CHOP, Modifier.fillMaxWidth()) { on ->
        actions.trackChop(c.channel, on)
    }
    val kills = ui.fx.trackKills[c.channel].orEmpty()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(tr("Sans graves"), tr("Sans médiums"), tr("Sans aigus")).forEachIndexed { band, label ->
            TrackLatch(label, "live_track_kill_${c.key}_$band", band in kills, state, c.channel, band, Modifier.weight(1f)) { on -> actions.kill(band, on, c.channel) }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val beat = if (ui.project.liveFx.gridSteps > 0) 4 else 0
        TapButton(tr("Lancer dans la réverbe"), "live_track_throw_${c.key}_0", clock, beat, Modifier.weight(1f)) { actions.throwFx(0, c.channel) }
        TapButton(tr("Lancer dans l'écho"), "live_track_throw_${c.key}_1", clock, beat, Modifier.weight(1f)) { actions.throwFx(1, c.channel) }
    }
    MacroKnobs(c.channel, channel.macros, actions.fx, size = 56.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(tr("Envoi réverbe"), tr("Envoi écho")).forEachIndexed { bus, label ->
            val v = channel.sends.getOrElse(bus) { 0f }
            Column(Modifier.weight(1f)) {
                Text(tr("%1\$s : %2\$s", label, percent((v * 100).roundToInt())), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1)
                ProSlider(v, { actions.setSend(c.channel, bus, it) }, Modifier.fillMaxWidth().height(48.dp).testTag("live_send_${c.key}_$bus"), accent = Neon.Magenta)
            }
        }
    }
    OutlinedButton(onClick = { actions.openEffects(c.channel) }, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("live_fx_box")) {
        Text(tr("Effets… (%s)", c.name), maxLines = 1)
    }
}

/**
 * A track's held button: with the lock, kept on by the page until touched again. Its strip [ch]
 * and [band] ([LiveFxState.CHOP]: the chopper) let it go when the page does. Let go elsewhere
 * (a Drop, Stop: [active] goes off), it no longer shows the padlock and the next touch plays it.
 */
@Composable
private fun TrackLatch(label: String, tag: String, active: Boolean, state: LiveFxState, ch: Int, band: Int, modifier: Modifier, hold: (Boolean) -> Unit) {
    val kept = state.isLatched(tag)
    if (kept && !active) SideEffect { state.forget(tag) }
    LatchButton(
        label, modifier, tag, locking = state.locked, latched = kept && active, active = active,
        hold = hold,
        latch = { state.latch(tag, ch, band) },
        unlatch = { if (state.unlatch(tag) != null) hold(false) },
    )
}
