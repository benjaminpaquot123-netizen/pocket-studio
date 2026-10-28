package com.pocketstudio.ui

import com.pocketstudio.model.i18n.upperShown
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.DpOffset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.i18n.tr

/**
 * Where folded cards are remembered, by title. The app gives one that lasts (its preferences);
 * the default only lasts while the app is open.
 */
interface FoldStore {
    fun isFolded(key: String): Boolean
    fun setFolded(key: String, folded: Boolean)
}

/** Folds kept in memory (Compose state, so a change redraws the card). */
open class MemoryFoldStore(initial: Map<String, Boolean> = emptyMap()) : FoldStore {
    private val folded = mutableStateMapOf<String, Boolean>().apply { putAll(initial) }
    override fun isFolded(key: String) = folded[key] ?: false
    override fun setFolded(key: String, folded: Boolean) {
        this.folded[key] = folded
    }

    /** Every card open again (the settings' reset). */
    open fun unfoldAll() {
        folded.clear()
    }
}

val LocalFoldStore = staticCompositionLocalOf<FoldStore> { MemoryFoldStore() }

/** False where cards must stay open (a step-by-step assistant, a dialog). */
val LocalFolding = staticCompositionLocalOf { true }

/** A card's fold: its first [SectionLabel] becomes the header that folds it. */
@Stable
internal class CardFold {
    var header: Any? = null
    var key by mutableStateOf<String?>(null)
    var card: LayoutCoordinates? = null
    var headerCoordinates: LayoutCoordinates? = null
    /** Bottom of the header in the card, in pixels (0: not measured yet). */
    var headerBottom by mutableFloatStateOf(0f)
    /** Size changes animate once the user has folded or opened the card, not when it first shows. */
    var animate by mutableStateOf(false)

    /** Called when the card or its header is placed: whichever comes last can measure. */
    fun measure() {
        val card = card ?: return
        val header = headerCoordinates ?: return
        if (!card.isAttached || !header.isAttached) return
        headerBottom = card.localPositionOf(header, Offset(0f, header.size.height.toFloat())).y
        key?.let { headerBottoms[it] = headerBottom }
    }
}

/** Last measured header bottoms, by fold key: a folded card shows folded from its first frame. */
private val headerBottoms = HashMap<String, Float>()

internal val LocalCardFold = compositionLocalOf<CardFold?> { null }

/**
 * The basic container of every screen: a graphite panel lit from above (a light gradient, a thin
 * bright edge, a soft shadow). With a [SectionLabel] as title, a touch on the title folds it.
 * [accent] other than the outline draws a coloured edge (an important card).
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    accent: Color = Neon.Outline,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val surface = Neon.Surface
    val fold = remember { CardFold() }
    val store = LocalFoldStore.current
    val folded = fold.key?.let(store::isFolded) == true && fold.headerBottom > 0f
    val highlighted = accent != Neon.Outline
    val headerHeight = with(LocalDensity.current) { fold.headerBottom.toDp() } + 16.dp
    Box(
        modifier
            .fillMaxWidth()
            // Two shadows, as under a real panel: a wide soft one, a tight one where it touches.
            // An important card glows faintly in its colour instead of the wide one.
            .dropShadow(shape, if (highlighted) Shadow(14.dp, accent, alpha = 0.26f) else PanelShadowWide)
            .dropShadow(shape, PanelShadowContact)
            .clip(shape)
            .then(remember(surface, accent, highlighted) { Modifier.panelFinish(surface, accent.takeIf { highlighted }) })
            .then(if (fold.animate) Modifier.animateContentSize(StudioMotion.spatial()) else Modifier)
            .then(if (folded) Modifier.height(headerHeight).clipToBounds() else Modifier)
            .onGloballyPositioned {
                fold.card = it
                fold.measure()
            },
    ) {
        CompositionLocalProvider(LocalCardFold provides fold) {
            Column(
                // Folded, the content keeps its natural size and is only cut under the title; nothing
                // under the title is drawn, so no half line of text or row of chips peeks out.
                modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = folded)
                    .then(if (folded) Modifier.drawWithContent { clipRect(bottom = fold.headerBottom) { this@drawWithContent.drawContent() } } else Modifier)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/** Under every panel: wide and soft, then tight where it touches (made once). */
private val PanelShadowWide = Shadow(radius = 14.dp, color = Color.Black, offset = DpOffset(0.dp, 5.dp), alpha = 0.34f)
private val PanelShadowContact = Shadow(radius = 2.dp, color = Color.Black, offset = DpOffset(0.dp, 1.dp), alpha = 0.30f)

/**
 * A panel's material, lit from above: a micro-gradient (lighter at the top, a shade darker at
 * the foot), a grain too fine to see but that keeps it from looking flat, and over the content a
 * hairline edge, bright along the top and fading down the sides, darker at the foot (a bevel).
 * [accent]: a coloured edge instead (an important card). Its brushes are made when the size
 * changes, not on each frame. Put it after the clip.
 */
fun Modifier.panelFinish(surface: Color, accent: Color? = null, corner: Dp = 16.dp): Modifier = drawWithCache {
    val h = size.height.coerceAtLeast(1f)
    val body = Brush.verticalGradient(
        0f to lerp(surface, Color.White, 0.055f),
        (48.dp.toPx() / h).coerceIn(0.05f, 0.5f) to lerp(surface, Color.White, 0.015f),
        1f to lerp(surface, Color.Black, 0.075f),
        startY = 0f, endY = h,
    )
    val edgeWidth = if (accent != null) 1.5.dp.toPx() else 1.dp.toPx() * 0.75f
    val edge = if (accent != null) {
        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.55f)), startY = 0f, endY = h)
    } else {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.24f),
            (28.dp.toPx() / h).coerceIn(0.02f, 0.4f) to Color.White.copy(alpha = 0.06f),
            0.7f to Color.White.copy(alpha = 0.03f),
            1f to Color.Black.copy(alpha = 0.22f),
            startY = 0f, endY = h,
        )
    }
    val stroke = Stroke(edgeWidth)
    val r = corner.toPx()
    val inset = Offset(edgeWidth / 2, edgeWidth / 2)
    val edgeSize = Size(size.width - edgeWidth, size.height - edgeWidth)
    val edgeRadius = CornerRadius((r - edgeWidth / 2).coerceAtLeast(0f))
    onDrawWithContent {
        drawRect(body)
        if (PanelGrain.enabled) drawRect(PanelGrain.brush)
        drawContent()
        drawRoundRect(edge, inset, edgeSize, edgeRadius, style = stroke)
    }
}

/**
 * A card's title in small capitals after a bar of its [color]; with [help], a faded "?" explains
 * the term. The first title of a card folds it: a discreet chevron shows it, a touch folds or
 * opens it, and the app remembers it under [foldKey] (the title by default).
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Neon.Cyan, help: Help? = null, foldKey: String? = null) {
    val fold = LocalCardFold.current.takeIf { LocalFolding.current }
    val me = remember { Any() }
    val isHeader = fold != null && (fold.header == null || fold.header === me)
    if (isHeader) {
        fold!!.header = me
        DisposableEffect(fold) {
            onDispose {
                if (fold.header === me) {
                    fold.header = null
                    fold.key = null
                    fold.headerCoordinates = null
                    fold.headerBottom = 0f
                }
            }
        }
    }
    val key = foldKey ?: com.pocketstudio.model.i18n.I18n.frenchOf(text)
    val store = LocalFoldStore.current
    val folded = isHeader && store.isFolded(key)
    if (isHeader && fold!!.key != key) {
        fold.key = key
        if (fold.headerBottom == 0f) fold.headerBottom = headerBottoms[key] ?: 0f
    }
    val label: @Composable () -> Unit = { SectionTitle(text, color, folded.takeIf { isHeader }) }
    val headerModifier = if (isHeader) {
        modifier
            .onGloballyPositioned { c ->
                fold!!.headerCoordinates = c
                fold.measure()
            }
            .foldToggle(key, folded) { fold!!.animate = true }
    } else modifier
    if (help == null) Box(headerModifier, contentAlignment = Alignment.CenterStart) { label() }
    else WithHelp(help, headerModifier) { label() }
}

/**
 * The bar, the title in small capitals and, when it folds something ([folded] not null), the
 * chevron. [subtle]: a dot and grey text instead, for a group inside a card ([TileSection]'s look).
 */
@Composable
private fun SectionTitle(text: String, color: Color, folded: Boolean?, subtle: Boolean = false) {
    val turn by animateFloatAsState(if (folded == true) -90f else 0f, StudioMotion.spatial(), label = "fold-chevron")
    val chevron = Neon.TextSecondary.copy(alpha = 0.45f)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (subtle) Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        else Box(Modifier.width(3.dp).height(12.dp).clip(RoundedCornerShape(2.dp)).background(color))
        // Audit final (22): a title TalkBack can jump to.
        Text(text.upperShown(), style = if (subtle) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelSmall.merge(SectionCaps), color = if (subtle) Neon.TextSecondary else lerp(color, Neon.TextPrimary, 0.25f),
            modifier = Modifier.semantics { heading() })
        if (folded != null) {
            // A small open "v", turned to ">" when folded: there, not shouting.
            Canvas(Modifier.size(12.dp).rotate(turn)) {
                val w = size.width
                val stroke = 1.6.dp.toPx()
                drawLine(chevron, Offset(w * 0.2f, w * 0.38f), Offset(w * 0.5f, w * 0.66f), stroke, StrokeCap.Round)
                drawLine(chevron, Offset(w * 0.5f, w * 0.66f), Offset(w * 0.8f, w * 0.38f), stroke, StrokeCap.Round)
            }
        }
    }
}

/**
 * Section titles in small capitals: semi-bold and spaced a little wider, as engraved on a console
 * (about the same width as the bold ones before them).
 */
private val SectionCaps = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, letterSpacing = 1.1.sp)

/** A title that folds: a touch folds or opens what it heads, remembered under [key]. */
@Composable
private fun Modifier.foldToggle(key: String, folded: Boolean, beforeToggle: () -> Unit = {}): Modifier {
    val store = LocalFoldStore.current
    return clip(RoundedCornerShape(6.dp))
        .clickable(role = Role.Button, onClickLabel = if (folded) tr("Déplier") else tr("Replier")) {
            beforeToggle()
            store.setFolded(key, !folded)
        }
        .semantics { stateDescription = if (folded) tr("Replié") else tr("Déplié") }
        .heightIn(min = 24.dp)
        .testTag("fold_$key")
}

/**
 * A title over a group (cards in a list, or tiles in a card when [subtle]) that folds the whole
 * group, like a card's title folds the card: same chevron, remembered under [foldKey] (the title
 * by default).
 */
@Composable
fun FoldSection(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = Neon.Cyan,
    foldKey: String? = null,
    spacing: Dp = 12.dp,
    subtle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val key = foldKey ?: title
    val canFold = LocalFolding.current
    val folded = canFold && LocalFoldStore.current.isFolded(key)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        // The whole width folds (an easy target), not just the words.
        Box(Modifier.fillMaxWidth().then(if (canFold) Modifier.foldToggle(key, folded) else Modifier), contentAlignment = Alignment.CenterStart) {
            SectionTitle(title, color, folded.takeIf { canFold }, subtle)
        }
        AnimatedVisibility(
            !folded,
            enter = expandVertically(StudioMotion.spatial()) + fadeIn(StudioMotion.effects()),
            exit = shrinkVertically(StudioMotion.spatial()) + fadeOut(StudioMotion.effects()),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing), content = content)
        }
    }
}

/** Label on the left, value on the right; used for technical readouts. A new value rolls in. */
@Composable
fun StatRow(label: String, value: String, valueColor: Color = Neon.TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        RollingText(
            value,
            modifier = Modifier.weight(1f),
            style = MonoStyle,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = Int.MAX_VALUE,
            softWrap = true,
        )
    }
}

/** Colours of the selectable pills: the chosen one is filled with the accent. */
@Composable
fun pillColors(accent: Color = Neon.Cyan): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = Neon.SurfaceHigh.copy(alpha = 0.55f),
    labelColor = Neon.TextSecondary,
    iconColor = Neon.TextSecondary,
    selectedContainerColor = accent,
    selectedLabelColor = Neon.OnAccent,
    selectedLeadingIconColor = Neon.OnAccent,
    selectedTrailingIconColor = Neon.OnAccent,
)

/**
 * A slider as on studio gear: a fine track filled with the accent in a gradient, a round thumb
 * that glows and grows while it moves. Same use as Material's Slider (semantics, steps).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    accent: Color = Neon.Cyan,
    /** Audit final (22): what TalkBack calls the slider and says of its value (instead of a bare %). */
    label: String? = null,
    valueText: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val dragged by interaction.collectIsDraggedAsState()
    val active = pressed || dragged
    val grow by animateFloatAsState(if (active) 1.25f else 1f, StudioMotion.spatial(), label = "thumb")
    val ink = if (enabled) accent else Neon.TextSecondary.copy(alpha = 0.5f)
    val rail = Neon.SurfaceHigh
    val haptics = LocalHapticFeedback.current
    Slider(
        value = value,
        // A few steps (a choice of 2 to 24): a light tick at each one, like a notched control.
        onValueChange = if (steps in 1..NOTCHED_STEPS_MAX) { v -> if (v != value) haptics.notch(); onValueChange(v) } else onValueChange,
        modifier = modifier.then(
            if (label == null && valueText == null) Modifier
            else Modifier.semantics {
                label?.let { contentDescription = it }
                valueText?.let { stateDescription = it }
            },
        ),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interaction,
        thumb = {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(18.dp * grow)
                        .shadow(if (active) 10.dp else 4.dp, CircleShape, ambientColor = ink, spotColor = ink)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color.White, lerp(ink, Color.White, 0.35f))))
                        .border(2.dp, ink, CircleShape),
                )
            }
        },
        track = { state ->
            val span = (state.valueRange.endInclusive - state.valueRange.start).takeIf { it > 0f } ?: 1f
            val fraction = ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(color = rail, cornerRadius = r)
                if (fraction > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(lerp(ink, Color.Black, 0.25f), ink), endX = size.width * fraction),
                        size = Size(size.width * fraction, size.height),
                        cornerRadius = r,
                    )
                }
                if (state.steps > 0) {
                    for (i in 0..state.steps + 1) {
                        val x = size.width * i / (state.steps + 1)
                        drawCircle(Color.White.copy(alpha = 0.35f), radius = 1.5.dp.toPx(), center = Offset(x, size.height / 2))
                    }
                }
            }
        },
    )
}
