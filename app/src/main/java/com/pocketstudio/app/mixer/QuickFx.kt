package com.pocketstudio.app.mixer

import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.app.analysis.drawSpectrumBackdrop
import com.pocketstudio.ui.WithHelp
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Channel
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.Macros
import com.pocketstudio.model.SoundKind
import com.pocketstudio.model.matchingFx
import com.pocketstudio.ui.Knob
import com.pocketstudio.ui.KnobColors
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt
import com.pocketstudio.model.i18n.tr

class QuickFxActions(
    val setMacro: (ch: Int, macro: Int, value: Float) -> Unit = { _, _, _ -> },
    val setMacroPair: (ch: Int, x: Int, xValue: Float, y: Int, yValue: Float) -> Unit = { _, _, _, _, _ -> },
    val applyFx: (ch: Int, presetId: String) -> Unit = { _, _ -> },
    /** Hold-to-listen: ready-made effect [presetId] heard on strip [ch] while a finger holds its chip (null: back to the saved sound). */
    val audition: (ch: Int, presetId: String?) -> Unit = { _, _ -> },
    /** The ready-made effects starred (kept on the phone, the same for every project). */
    val favourites: kotlinx.coroutines.flow.StateFlow<Set<String>> = kotlinx.coroutines.flow.MutableStateFlow(emptySet()),
    val toggleFavourite: (presetId: String) -> Unit = {},
)

/**
 * Sound play for one track, no technical knobs: ready-made effects named after what they sound
 * like (those that suit [kind] first), then, unless [compact], a pad XY to sweep two of the
 * track's macros at once and the four macros as big knobs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickFxPanel(ch: Int, channel: Channel, actions: QuickFxActions, kind: SoundKind? = null, compact: Boolean = false) {
    val chipColors = pillColors(Neon.Magenta)
    val current = channel.matchingFx()
    val suggested = kind?.let { FxPresets.suggestedFor(it, ch) }.orEmpty()
    val favourites by actions.favourites.collectAsState()
    // With suggestions, the families wait behind « Toutes » (unless the one in use is elsewhere).
    val startAll = suggested.isEmpty() || (current != null && current.id != FxPresets.NONE.id && current !in suggested)
    var category by rememberSaveable(ch, kind) {
        mutableStateOf(if (startAll) current?.category?.takeIf { it.isNotEmpty() } ?: FxPresets.CATEGORIES.first() else SUGGESTED)
    }
    // The ambience a finger holds (heard, not chosen).
    var auditioning by remember(ch) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().testTag("quick_fx_$ch"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WithHelp(Glossary.FX_PRESET) { Text(tr("Ambiances toutes prêtes"), style = MaterialTheme.typography.labelLarge, color = Neon.Magenta) }
        Row(Modifier.fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (kind != null) {
                FilterChip(selected = category == SUGGESTED, onClick = { category = SUGGESTED }, label = { Text(tr("Pour : %s", tr(kind.label))) },
                    modifier = Modifier.testTag("fx_category_suggested"))
            }
            FilterChip(selected = category == FAVOURITES, onClick = { category = FAVOURITES }, label = { Text(tr("Favoris")) },
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("fx_category_favourites"))
            FxPresets.CATEGORIES.forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(tr(c)) },
                    modifier = Modifier.testTag("fx_category_$c"))
            }
        }
        // By music style (a preset may suit several).
        Row(Modifier.fadingHorizontalScroll(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Style :"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1, softWrap = false)
            FxGenres.ALL.forEach { g ->
                FilterChip(selected = category == GENRE + g, onClick = { category = GENRE + g }, label = { Text(FxGenres.label(g)) },
                    modifier = Modifier.testTag("fx_genre_$g"))
            }
        }
        val shown = when {
            category == SUGGESTED -> suggested
            category == FAVOURITES -> FxPresets.ALL.filter { it.id in favourites }
            category.startsWith(GENRE) -> FxGenres.presets(category.removePrefix(GENRE))
            else -> FxPresets.ALL.filter { it.category == category }
        }
        if (category == FAVOURITES && shown.isEmpty()) {
            Text(tr("Pas encore de favori : choisis une ambiance, puis touche ☆ pour la garder ici."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("fx_favourites_none"))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            shown.forEach { p ->
                FilterChip(selected = current?.id == p.id || auditioning == p.id, onClick = { actions.applyFx(ch, p.id) },
                    label = { Text((if (p.id in favourites) "★ " else "") + tr(p.name)) },
                    colors = chipColors,
                    modifier = Modifier.holdToListen { on ->
                        auditioning = if (on) p.id else null
                        actions.audition(ch, if (on) p.id else null)
                    }.testTag("fx_preset_${p.id}"))
            }
            FilterChip(selected = current?.id == FxPresets.NONE.id, onClick = { actions.applyFx(ch, FxPresets.NONE.id) },
                label = { Text(tr(FxPresets.NONE.name)) }, colors = chipColors, modifier = Modifier.testTag("fx_preset_none"))
        }
        val heldName = auditioning?.let(FxPresets::byId)?.let { tr(it.name) }
        if (heldName != null) {
            Text(tr("À l'écoute : « %s ». Lâche pour revenir au son d'avant.", heldName),
                style = MaterialTheme.typography.bodySmall, color = Neon.Magenta, modifier = Modifier.testTag("fx_auditioning"))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(current?.description?.let { tr(it) } ?: tr("Touche une ambiance pour l'essayer : tu peux toujours revenir avec Annuler."),
                    Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                if (current != null && current.id != FxPresets.NONE.id) {
                    val starred = current.id in favourites
                    IconButton(onClick = { actions.toggleFavourite(current.id) }, modifier = Modifier.testTag("fx_favourite")) {
                        Icon(
                            if (starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (starred) tr("Retirer des favoris") else tr("Ajouter aux favoris"),
                            tint = if (starred) Neon.Amber else Neon.TextSecondary,
                        )
                    }
                }
            }
        }
        Text(tr("Garde le doigt sur une ambiance pour l'écouter ; lâche, le son revient."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        if (!compact) {
            XyPad(ch, channel.macros, actions)
            MacroKnobs(ch, channel.macros, actions)
        }
    }
}

/**
 * Hold-to-listen on a chip: a finger kept still on it longer than a long press calls [onHold]
 * true, then false when it lifts (also when the chip goes mid-gesture); the chip's own tap is
 * then not made. A quick touch stays a tap, a slide stays a scroll.
 */
private fun Modifier.holdToListen(onHold: (Boolean) -> Unit): Modifier = composed {
    val hold by rememberUpdatedState(onHold)
    val haptics = LocalHapticFeedback.current
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val slop = viewConfiguration.touchSlop
            val ended = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                    if (!c.pressed || (c.position - down.position).getDistance() > slop) break
                }
                true
            }
            if (ended != null) return@awaitEachGesture
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            hold(true)
            try {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    // Taken here: the chip does not also apply the ambience when the finger lifts.
                    e.changes.forEach { it.consume() }
                    if (e.changes.none { it.pressed }) break
                }
            } finally {
                hold(false)
            }
        }
    }
}

/** Room kept above and below the values of a pad XY for its top and bottom labels. */
internal val XY_INSET = 14.dp

/** A pad XY's value (0 at the bottom, 1 at the top) under a finger at [y] of [height], [inset] kept at each end. */
internal fun xyValue(y: Float, height: Float, inset: Float): Float = (1f - (y - inset) / (height - 2 * inset).coerceAtLeast(1f)).coerceIn(0f, 1f)

/** Where value [v] is drawn on a pad XY of [height] (see [xyValue]). */
internal fun xyPosition(v: Float, height: Float, inset: Float): Float = inset + (1f - v.coerceIn(0f, 1f)) * (height - 2 * inset)

/** The suggestions' tab (not a preset category). */
private const val SUGGESTED = "suggested"

/** The favourites' tab, and the prefix of a style's tab. */
private const val FAVOURITES = "favourites"
private const val GENRE = "genre:"

/**
 * X: the colour (dark ↔ bright); Y: a macro of choice. Double tap: both back to rest. [hint]: the
 * sentence under the pad (the Live page has no room for it).
 */
@Composable
internal fun XyPad(
    ch: Int, macros: List<Float>, actions: QuickFxActions, height: androidx.compose.ui.unit.Dp = 170.dp, hint: Boolean = true,
    background: androidx.compose.ui.graphics.Color? = null,
    /** The output spectrum, drawn faintly behind (read when drawn; the Live page). */
    backdrop: (() -> FloatArray?)? = null,
    /** The macro up-down, fixed (no chips to choose it: the Live page's modes do that). */
    yFixed: Int? = null,
) {
    var chosenY by rememberSaveable(ch) { mutableIntStateOf(Macros.SPACE) }
    val yMacro = yFixed ?: chosenY
    val x = macros[Macros.COLOR]
    val y = macros[yMacro]
    val currentY by rememberUpdatedState(yMacro)
    val set by rememberUpdatedState(actions.setMacroPair)
    if (yFixed == null) {
        // The label on its own line: never cut at the left of the chips (a small phone, big text).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Pad XY, haut-bas :"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            HelpDot(Glossary.XY_PAD)
        }
        Row(Modifier.fadingHorizontalScroll(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Macros.SPACE, Macros.DIRT, Macros.MOTION).forEach { m ->
                FilterChip(selected = yMacro == m, onClick = { chosenY = m }, label = { Text(tr(Macros.NAMES[m])) },
                    modifier = Modifier.testTag("xy_axis_$m"))
            }
        }
    }
    val accent = Neon.Magenta
    val grid = Neon.Outline
    val (dark, bright) = Macros.ENDS[Macros.COLOR].let { (a, b) -> tr(a) to tr(b) }
    val (low, high) = Macros.ENDS[yMacro].let { (a, b) -> tr(a) to tr(b) }
    XyLabelled(top = "↑ $high", left = "← $dark", bottom = "↓ $low", right = "$bright →") {
        Box(
            Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(12.dp)).background(background ?: Neon.Surface)
                .pointerInput(ch) {
                    fun send(at: Offset) {
                        val nx = (at.x / size.width).coerceIn(0f, 1f)
                        set(ch, Macros.COLOR, nx, currentY, xyValue(at.y, size.height.toFloat(), XY_INSET.toPx()))
                    }
                    detectDragGestures(onDragStart = ::send) { change, _ -> send(change.position) }
                }
                .pointerInput(ch) {
                    detectTapGestures(
                        onTap = { at ->
                            set(ch, Macros.COLOR, (at.x / size.width).coerceIn(0f, 1f), currentY, xyValue(at.y, size.height.toFloat(), XY_INSET.toPx()))
                        },
                        onDoubleTap = { set(ch, Macros.COLOR, Macros.DEFAULTS[Macros.COLOR], currentY, Macros.DEFAULTS[currentY]) },
                    )
                }
                .testTag("xy_pad_$ch"),
        ) {
            val haze = Neon.Cyan.copy(alpha = 0.13f)
            Canvas(Modifier.matchParentSize()) {
                backdrop?.let { drawSpectrumBackdrop(it(), haze) }
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                // The values span a band inside the top and bottom edges: the dot at either end stays whole.
                val inset = XY_INSET.toPx()
                drawLine(grid, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2f, pathEffect = dash)
                drawLine(grid, Offset(0f, size.height - inset), Offset(size.width, size.height - inset), strokeWidth = 2f)
                val at = Offset(x * size.width, xyPosition(y, size.height, inset))
                drawCircle(accent.copy(alpha = 0.25f), radius = 34f, center = at)
                drawCircle(accent, radius = 16f, center = at)
            }
        }
    }
    if (hint) {
        Text(tr("Glisse le doigt pour triturer le son en direct. Deux touchers : retour au son de départ."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

/**
 * A pad XY with its ends written around it, never on it: the dot (at rest or at an end) never
 * hides a word. [top] above it, [left] [bottom] [right] under it; [topEnd]: a hint above, at the right.
 */
@Composable
internal fun XyLabelled(top: String, left: String, bottom: String, right: String, topEnd: String? = null, pad: @Composable () -> Unit) {
    val style = MaterialTheme.typography.labelSmall
    val color = Neon.TextSecondary
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (topEnd == null) {
                Text(top, Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                // The hint gives way first on a narrow pad (it is cut, never glued to the end's name).
                Text(top, Modifier.padding(end = 12.dp), style = style, color = color, maxLines = 1)
                // On two lines rather than cut (a narrow pad: the 303's beside the effects on a tablet).
                Text(topEnd, Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        pad()
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(left, Modifier.weight(1f), style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(bottom, Modifier.padding(horizontal = 6.dp), style = style, color = color, maxLines = 1)
            Text(right, Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun MacroKnobs(ch: Int, macros: List<Float>, actions: QuickFxActions, size: androidx.compose.ui.unit.Dp = 52.dp) {
    val colors = KnobColors(
        cap = Neon.SurfaceHigh, capEdge = Neon.Outline, pointer = Neon.TextPrimary,
        track = Neon.Outline, value = Neon.Magenta, label = Neon.TextSecondary,
    )
    Row(Modifier.fillMaxWidth()) {
        Macros.NAMES.forEachIndexed { m, name ->
            val v = macros[m]
            Knob(
                value = v,
                onValueChange = { actions.setMacro(ch, m, it) },
                label = tr(name),
                default = Macros.DEFAULTS[m],
                size = size,
                colors = colors,
                valueText = macroText(m, v),
                modifier = Modifier.weight(1f).testTag("macro_${ch}_$m"),
            )
        }
    }
}

internal fun macroText(m: Int, v: Float): String {
    if (m == Macros.COLOR) {
        val c = ((v - 0.5f) * 200).roundToInt()
        return when {
            c < -2 -> tr("Sombre %d", -c)
            c > 2 -> tr("Brillant %d", c)
            else -> tr("Neutre")
        }
    }
    return if (v < 0.01f) "0" else com.pocketstudio.model.i18n.percent((v * 100).roundToInt())
}

/** One line for a closed strip: the matching ready-made effect, or what is on. */
fun quickFxSummary(c: Channel): String {
    val none = tr("Aucun")
    c.matchingFx()?.let { return if (it.id == FxPresets.NONE.id) none else tr(it.name) }
    val macros = c.macros.mapIndexedNotNull { m, v -> if (v != Macros.DEFAULTS[m]) tr(Macros.NAMES[m]) else null }
    return (listOf(effectsSummary(c.inserts, c.sends)).filter { it != none } + macros).joinToString(" · ").ifEmpty { none }
}
