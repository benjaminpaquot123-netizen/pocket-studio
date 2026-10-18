package com.pocketstudio.app.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.report.IssueReport
import com.pocketstudio.engine.StreamInfo
import com.pocketstudio.ui.LevelMeter
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.StatRow
import java.util.Locale
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private const val MIN_HZ = 55f
private const val MAX_HZ = 1760f
private val NOTE_NAMES = listOf("La", "La#", "Si", "Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#")

/** French note name closest to [hz], e.g. "La 4". */
fun noteName(hz: Float): String {
    val semitonesFromA4 = (12 * log2(hz / 440f)).roundToInt()
    val index = Math.floorMod(semitonesFromA4, 12)
    // Octave numbers change on Do (C), three semitones above La (A).
    val octave = 4 + Math.floorDiv(semitonesFromA4 + 9, 12)
    return "${NOTE_NAMES[index]} $octave"
}

private fun hzToSlider(hz: Float) = (ln(hz / MIN_HZ) / ln(MAX_HZ / MIN_HZ)).coerceIn(0f, 1f)
private fun sliderToHz(v: Float) = MIN_HZ * (MAX_HZ / MIN_HZ).pow(v)

@Composable
fun StudioScreen(
    state: StudioUiState,
    stream: StreamInfo,
    engineAvailable: Boolean,
    onTogglePlay: () -> Unit,
    onFrequency: (Float) -> Unit,
    onVolume: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("studio"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Son de test", style = MaterialTheme.typography.headlineMedium, color = Neon.TextPrimary)
        Text("Une note pure pour vérifier que le moteur audio sonne juste.", color = Neon.TextSecondary)

        NeonCard(accent = if (state.playing) Neon.Cyan else Neon.Outline) {
            SectionLabel("Oscillateur de test")
            val buttonColor by animateColorAsState(if (state.playing) Neon.Magenta else Neon.Cyan, label = "play")
            Button(
                onClick = onTogglePlay,
                enabled = engineAvailable,
                modifier = Modifier.fillMaxWidth().height(64.dp).testTag("play"),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            ) {
                Icon(
                    if (state.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (state.playing) "Arrêter" else "Jouer", style = MaterialTheme.typography.titleMedium)
            }
            LevelMeter(stream.peak)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Hauteur", Modifier.weight(1f))
                Text(
                    "${noteName(state.frequencyHz)} · ${state.frequencyHz.roundToInt()} Hz",
                    style = MonoStyle,
                    color = Neon.Cyan,
                )
            }
            Slider(value = hzToSlider(state.frequencyHz), onValueChange = { onFrequency(sliderToHz(it)) })

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Volume", Modifier.weight(1f))
                Text("${(state.volume * 100).roundToInt()} %", style = MonoStyle, color = Neon.Cyan)
            }
            Slider(value = state.volume, onValueChange = onVolume)
        }

        NeonCard {
            SectionLabel("Flux audio")
            if (!engineAvailable) {
                Text("Moteur audio indisponible sur cet appareil.", color = Neon.Red)
            }
            StatRow("Mode", IssueReport.describeStream(stream).substringBefore(", ${stream.sampleRate}"))
            StatRow("Fréquence", if (stream.sampleRate > 0) "${stream.sampleRate} Hz" else "–")
            StatRow("Taille de burst", if (stream.framesPerBurst > 0) "${stream.framesPerBurst} éch." else "–")
            StatRow(
                "Buffer",
                if (stream.bufferSize > 0) {
                    String.format(
                        Locale.FRANCE, "%d éch. (%.1f ms)",
                        stream.bufferSize, 1000.0 * stream.bufferSize / stream.sampleRate,
                    )
                } else "–",
            )
            StatRow("Latence de sortie", IssueReport.formatMs(stream.outputLatencyMs))
            StatRow(
                "Décrochages",
                stream.xruns.toString(),
                if (stream.xruns == 0) Neon.Acid else Neon.Red,
            )
        }
    }
}
