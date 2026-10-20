package com.pocketstudio.app.analysis

import androidx.compose.foundation.Canvas
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
    Text("Graves  ·  Médiums  ·  Aigus", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
}

private val NAMES_FR = listOf("Do", "Do♯", "Ré", "Ré♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")
private val NAMES_EN = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

/** The tuner: the note heard, a needle for how far off it is, and the frequency. */
@Composable
fun TunerView(reading: TunerReading?, micOk: Boolean?, englishNotes: Boolean = false) {
    Column(Modifier.fillMaxSize().padding(16.dp).testTag("tuner"), verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Accordeur")
            when {
                micOk == null -> Text("Ouverture du micro…", color = Neon.TextSecondary)
                !micOk -> Text("Le micro n'a pas pu être ouvert (autorisation, ou une prise est en cours).", color = Neon.Amber)
                reading == null -> Text("Joue ou chante une note près du téléphone.", color = Neon.TextSecondary)
            }
            val names = if (englishNotes) NAMES_EN else NAMES_FR
            val inTune = reading != null && abs(reading.cents) < 5
            Text(
                reading?.let { names[Math.floorMod(it.midi, 12)] + "${it.midi / 12 - 1}" } ?: "—",
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
                    "${"%.1f".format(java.util.Locale.FRANCE, it.hz)} Hz · " + when {
                        abs(c) < 5 -> "juste"
                        c < 0 -> "trop bas de ${-c} cents"
                        else -> "trop haut de $c cents"
                    }
                } ?: " ",
                style = MonoStyle, color = Neon.TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Text("Accorde ta guitare, ta basse ou ta voix avant d'enregistrer une piste audio.", style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary)
    }
}
