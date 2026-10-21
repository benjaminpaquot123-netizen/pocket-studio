package com.pocketstudio.app.analysis

import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.abs
import kotlin.math.roundToInt
import com.pocketstudio.model.i18n.tr

/** Spectrum bars (from [Spectrum.bands]): lows on the left, highs on the right. */
@Composable
fun SpectrumView(bands: FloatArray?, modifier: Modifier = Modifier) {
    val bar = Neon.Cyan
    val hot = Neon.Amber
    val empty = Neon.Outline
    Canvas(modifier.fillMaxWidth().height(110.dp).testTag("spectrum")) {
        val n = bands?.size ?: 40
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        for (i in 0 until n) {
            val db = bands?.getOrNull(i) ?: Spectrum.MIN_DB
            val h = ((db - Spectrum.MIN_DB) / -Spectrum.MIN_DB).coerceIn(0f, 1f) * size.height
            val x = i * (w + gap)
            drawRoundRect(empty, Offset(x, size.height - 2f), Size(w, 2f))
            if (h > 0f) drawRoundRect(if (db > -6f) hot else bar, Offset(x, size.height - h), Size(w, h), CornerRadius(2f))
        }
    }
    Text(tr("Graves  ·  Médiums  ·  Aigus"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
}

/** A guitar's six strings in standard tuning, low to high: E2 A2 D3 G3 B3 E4 (MIDI notes). */
val GUITAR_STRINGS = listOf(40, 45, 50, 55, 59, 64)

/** The index in [GUITAR_STRINGS] of the string closest to the note heard ([midi] and [cents] off it). */
fun nearestString(midi: Int, cents: Double): Int {
    val heard = midi + cents / 100.0
    return GUITAR_STRINGS.indices.minBy { abs(GUITAR_STRINGS[it] - heard) }
}

/** The tuner: the note heard, a needle for how far off it is, the frequency, and a guitar's strings. */
@Composable
fun TunerView(reading: TunerReading?, micOk: Boolean?, englishNotes: Boolean) {
    // A comfortable width on a tablet: the needle does not stretch across a screen lying down.
    com.pocketstudio.ui.ReadingWidth(maxWidth = 520.dp) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("tuner"), verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel(tr("Accordeur"), help = Glossary.TUNER)
            when {
                micOk == null -> Text(tr("Ouverture du micro…"), color = Neon.TextSecondary)
                !micOk -> Text(tr("Le micro n'a pas pu être ouvert (autorisation, ou une prise est en cours)."), color = Neon.Amber)
                reading == null -> Text(tr("Joue ou chante une note près de l'appareil."), color = Neon.TextSecondary)
            }
            val inTune = reading != null && abs(reading.cents) < 5
            Text(
                reading?.let { com.pocketstudio.model.NoteNames.withOctave(it.midi, englishNotes, sharp = "♯") } ?: "—",
                fontSize = 64.sp, fontWeight = FontWeight.Bold,
                color = if (inTune) Neon.Acid else Neon.TextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            val needle = if (inTune) Neon.Acid else Neon.Amber
            val scale = Neon.Outline
            Canvas(Modifier.fillMaxWidth().height(48.dp)) {
                val mid = size.width / 2
                for (c in -50..50 step 10) {
                    val x = mid + c / 50f * (size.width / 2 - 8f)
                    drawLine(scale, Offset(x, size.height * (if (c == 0) 0.1f else 0.4f)), Offset(x, size.height * 0.9f), strokeWidth = if (c == 0) 4f else 2f)
                }
                reading?.let {
                    val x = mid + (it.cents.toFloat().coerceIn(-50f, 50f) / 50f) * (size.width / 2 - 8f)
                    drawCircle(needle, 12.dp.toPx() / 2, Offset(x, size.height / 2), style = Stroke(width = 6f))
                    drawLine(needle, Offset(x, 0f), Offset(x, size.height), strokeWidth = 6f)
                }
            }
            Text(
                reading?.let {
                    val c = it.cents.roundToInt()
                    "${"%.1f".format(com.pocketstudio.model.i18n.I18n.lang.locale, it.hz)} Hz · " + when {
                        abs(c) < 5 -> tr("juste")
                        c < 0 -> tr("trop bas de %d cents", -c)
                        else -> tr("trop haut de %d cents", c)
                    }
                } ?: " ",
                style = MonoStyle, color = Neon.TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            GuitarStrings(reading, englishNotes)
        }
        Text(tr("Accorde ta guitare, ta basse ou ta voix avant d'enregistrer une piste audio."), style
 = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary)
    }
    }
}

/** "Guitare : E2 A2 D3 G3 B3 E4", the string nearest to the note heard lit (green once in tune). */
@Composable
private fun GuitarStrings(reading: TunerReading?, englishNotes: Boolean) {
    val nearest = reading?.let { nearestString(it.midi, it.cents) }
    Text(tr("Guitare (accordage standard)"), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary)
    Row(Modifier.fillMaxWidth().testTag("tuner_strings"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        GUITAR_STRINGS.forEachIndexed { i, note ->
            val lit = i == nearest
            val onIt = lit && reading?.midi == note
            val color = when {
                onIt && abs(reading!!.cents) < 5 -> Neon.Acid
                lit -> Neon.Cyan
                else -> Neon.Outline
            }
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (lit) color.copy(alpha = 0.2f) else Color.Transparent)
                    .border(if (lit) 2.dp else 1.dp, color, RoundedCornerShape(8.dp))
                    .testTag("tuner_string_$i"),
                contentAlignment = Alignment.Center,
            ) {
                Text(com.pocketstudio.model.NoteNames.withOctave(note, englishNotes, sharp = "♯"), style = MaterialTheme.typography.labelLarge,
                    color = if (lit) color else Neon.TextSecondary, maxLines = 1)
            }
        }
    }
}

/** The output spectrum as faint bars from the bottom (lows on the left), behind a pad. */
fun DrawScope.drawSpectrumBackdrop(bands: FloatArray?, color: Color) {
    if (bands == null || bands.isEmpty()) return
    val n = bands.size
    val w = size.width / n
    for (i in 0 until n) {
        val h = ((bands[i] - Spectrum.MIN_DB) / -Spectrum.MIN_DB).coerceIn(0f, 1f) * size.height
        if (h > 0f) drawRect(color, Offset(i * w + 1f, size.height - h), Size((w - 2f).coerceAtLeast(1f), h))
    }
}
