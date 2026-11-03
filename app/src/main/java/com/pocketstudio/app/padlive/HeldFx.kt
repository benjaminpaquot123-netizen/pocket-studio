package com.pocketstudio.app.padlive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.LiveDivision
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import kotlin.math.pow
import kotlin.math.roundToInt

/** The held effects with a panel of their own while a finger holds them (lot B2). */
enum class HeldKind { FILTER, ROLL }

/**
 * A held effect's finger, lot B2 (« Filtre DJ » as the mockup's third screen): pressed on its
 * button, the effect plays as before (the Filtre by the finger's place across its button, the
 * Roulement at its last division); slid up into the panel shown over the pads, the finger's place
 * there is the filter's cutoff (across) and resonance (up), or the roll's division (across: slower
 * on the left, faster on the right). Let go: everything comes back.
 */
@Stable
class HeldFx {
    /** The effect held (null: none, the panel hidden). */
    var kind by mutableStateOf<HeldKind?>(null)
        private set
    /** The filter: 0 grave (low-pass) .. 0.5 open .. 1 aigu (high-pass). */
    var cutoff by mutableFloatStateOf(0.5f)
        private set
    /** The filter's resonance, 0..1 (the engine's rest: [REST_RESONANCE]). */
    var resonance by mutableFloatStateOf(REST_RESONANCE)
        private set
    /** The roll's division (the last one chosen stays for the next press). */
    var division by mutableStateOf(LiveDivision.SIXTEENTH)
        private set

    internal var panel: LayoutCoordinates? = null

    /**
     * The finger at [window] (window coordinates) on [kind]'s button, whose own place across it is
     * [across] (0..1); null: let go. Returns what changed for the engine (null: nothing).
     */
    fun move(kind: HeldKind, window: Offset?, across: Float): Change? {
        if (window == null) {
            this.kind = null
            val wasRes = resonance
            cutoff = 0.5f
            resonance = REST_RESONANCE
            return if (kind == HeldKind.FILTER) Change.FilterOff(wasRes != REST_RESONANCE) else Change.RollOff
        }
        val started = this.kind != kind
        this.kind = kind
        val inPanel = panel?.takeIf { it.isAttached }?.let { p ->
            val local = p.windowToLocal(window)
            val w = p.size.width.toFloat().coerceAtLeast(1f)
            val h = p.size.height.toFloat().coerceAtLeast(1f)
            // Above the panel's bottom edge: the panel takes over (the finger may go past its top).
            if (local.y <= h) Offset((local.x / w).coerceIn(0f, 1f), (1f - local.y / h).coerceIn(0f, 1f)) else null
        }
        return when (kind) {
            HeldKind.FILTER -> {
                val x = inPanel?.x ?: across.coerceIn(0f, 1f)
                val r = inPanel?.let { resonanceAt(it.y) } ?: resonance
                val moved = started || x != cutoff || r != resonance
                cutoff = x
                resonance = r
                if (moved) Change.Filter(x, r) else null
            }
            HeldKind.ROLL -> {
                val d = inPanel?.let { rollAt(it.x) } ?: division
                val moved = started || d != division
                division = d
                if (moved) Change.Roll(d) else null
            }
        }
    }

    /** What the engine is told. */
    sealed interface Change {
        data class Filter(val cutoff: Float, val resonance: Float) : Change
        /** Let go; [resonance]: the resonance was moved (back to its rest too). */
        data class FilterOff(val resonance: Boolean) : Change
        data class Roll(val division: LiveDivision) : Change
        data object RollOff : Change
    }

    companion object {
        const val REST_RESONANCE = 0.3f
        /** The roll's divisions across the panel, slowest first (the triplets between their neighbours). */
        val ROLLS = listOf(
            LiveDivision.QUARTER, LiveDivision.EIGHTH, LiveDivision.EIGHTH_TRIPLET,
            LiveDivision.SIXTEENTH, LiveDivision.SIXTEENTH_TRIPLET, LiveDivision.THIRTY_SECOND,
        )

        fun rollAt(x: Float): LiveDivision = ROLLS[(x.coerceIn(0f, 1f) * ROLLS.size).toInt().coerceAtMost(ROLLS.lastIndex)]

        /** The panel's height (0 bottom .. 1 top) as the filter's resonance: from its rest up to 0.95. */
        fun resonanceAt(y: Float): Float = REST_RESONANCE + (0.95f - REST_RESONANCE) * y.coerceIn(0f, 1f)

        /** The filter's cutoff in Hz as the engine sets it (PerformanceFx::applyFilter); 0: open. */
        fun cutoffHz(x: Float): Float = when {
            x < 0.499f -> 150f * (18000f / 150f).pow(x * 2f)
            x > 0.501f -> 30f * (6000f / 30f).pow((x - 0.5f) * 2f)
            else -> 0f
        }

        fun hzWords(hz: Float): String =
            if (hz >= 1000f) tr("%s kHz", String.format(java.util.Locale.getDefault(), "%.1f", hz / 1000f)) else tr("%d Hz", hz.roundToInt())

        /** « Passe-haut 1,8 kHz · résonance 40 % » (the mockup's words). */
        fun filterWords(cutoff: Float, resonance: Float): String {
            val hz = cutoffHz(cutoff)
            val pass = when {
                hz == 0f -> tr("Ouvert")
                cutoff < 0.5f -> tr("Passe-bas %s", hzWords(hz))
                else -> tr("Passe-haut %s", hzWords(hz))
            }
            return tr("%1\$s · résonance %2\$d %%", pass, (resonance * 100).roundToInt())
        }

        /** A division's name on the panel and on the button (« 1/8 T » for a triplet). */
        fun divisionWords(d: LiveDivision): String = when (d) {
            LiveDivision.EIGHTH_TRIPLET -> tr("1/8 T")
            LiveDivision.SIXTEENTH_TRIPLET -> tr("1/16 T")
            else -> d.label
        }
    }
}

/**
 * The panel of the held effect (null: nothing drawn): « FILTRE DJ · TANT QUE TU APPUIES · Lâche :
 * tout revient », the filter's words and its point on a grid (across: cutoff, up: resonance); or
 * the roll's divisions, the one playing lit.
 */
@Composable
internal fun HeldFxPanel(fx: HeldFx, modifier: Modifier = Modifier) {
    val kind = fx.kind ?: return
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(lerp(Neon.Background, Color.Black, 0.35f).copy(alpha = 0.96f))
            .border(1.dp, Neon.Amber.copy(alpha = 0.35f), shape).padding(12.dp).testTag("padlive_held_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val style = MaterialTheme.typography.labelMedium
            val fit = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp)
            Text(
                if (kind == HeldKind.FILTER) tr("FILTRE DJ · TANT QUE TU APPUIES") else tr("ROULEMENT · GLISSE POUR CHANGER"),
                style = style, color = Neon.TextSecondary, maxLines = 1, softWrap = false, autoSize = fit,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Text(tr("Lâche : tout revient"), style = style, color = Neon.TextPrimary, maxLines = 1, softWrap = false)
        }
        Box(
            Modifier.fillMaxWidth().height(PANEL_HEIGHT).clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(Neon.SurfaceHigh, Neon.Surface)))
                .onGloballyPositioned { fx.panel = it }
                .testTag("padlive_held_area"),
        ) {
            when (kind) {
                HeldKind.FILTER -> FilterArea(fx)
                HeldKind.ROLL -> RollArea(fx)
            }
        }
    }
}

@Composable
private fun FilterArea(fx: HeldFx) {
    val words = HeldFx.filterWords(fx.cutoff, fx.resonance)
    val amber = Neon.Amber
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = words }) {
        val line = Color.White.copy(alpha = 0.06f)
        for (i in 1..3) {
            drawLine(line, Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f, size.height), 1f)
            drawLine(line, Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), 1f)
        }
        val p = Offset(size.width * fx.cutoff, size.height * (1f - (fx.resonance - HeldFx.REST_RESONANCE) / (0.95f - HeldFx.REST_RESONANCE)))
        drawLine(amber.copy(alpha = 0.45f), Offset(p.x, 0f), Offset(p.x, size.height), 2f)
        drawLine(amber.copy(alpha = 0.25f), Offset(0f, p.y), Offset(size.width, p.y), 2f)
        drawCircle(Brush.radialGradient(listOf(amber.copy(alpha = 0.55f), Color.Transparent), center = p, radius = 70f), radius = 70f, center = p)
        drawCircle(Color.White, radius = 22f, center = p)
    }
    Text(
        words, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).testTag("padlive_filter_words"),
    )
}

@Composable
private fun RollArea(fx: HeldFx) {
    Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HeldFx.ROLLS.forEach { d ->
            val on = d == fx.division
            Box(
                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(10.dp))
                    .background(if (on) Neon.Amber.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.04f))
                    .border(if (on) 2.dp else 1.dp, if (on) Neon.Amber else Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .testTag("padlive_roll_${d.name.lowercase()}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    HeldFx.divisionWords(d), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = if (on) Neon.TextPrimary else Neon.TextSecondary, textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}

/** The panel's grid's height. */
private val PANEL_HEIGHT = 140.dp
