package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Channel
import com.pocketstudio.ui.LevelMeter
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.log10
import kotlin.math.roundToInt

val CHANNEL_NAMES = listOf("Batterie", "Basse acide", "Synthé")

data class MixerActions(
    val setVolume: (Int, Float) -> Unit = { _, _ -> },
    val setPan: (Int, Float) -> Unit = { _, _ -> },
    val toggleMute: (Int) -> Unit = {},
    val toggleSolo: (Int) -> Unit = {},
)

/** Fader position (0.8 = unity, quadratic law, as in the engine) shown in dB. */
fun faderDb(v: Float): String {
    if (v <= 0.001f) return "−∞ dB"
    val gain = (v / 0.8f) * (v / 0.8f)
    val db = 20 * log10(gain)
    return (if (db >= 0) "+" else "") + String.format(java.util.Locale.FRANCE, "%.1f dB", db)
}

@Composable
fun MixerScreen(channels: List<Channel>, meters: FloatArray, actions: MixerActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("mixer"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CHANNEL_NAMES.forEachIndexed { i, name ->
            Strip(name, channels[i], meters.getOrElse(i) { 0f }, i, actions)
        }
        NeonCard(accent = Neon.Cyan) {
            SectionLabel("Sortie générale")
            LevelMeter(meters.getOrElse(3) { 0f } / 0.89f)
            val gain = meters.getOrElse(4) { 1f }
            Text(
                if (gain < 0.98f) "Le limiteur protège la sortie (−${String.format(java.util.Locale.FRANCE, "%.1f", -20 * log10(gain))} dB)"
                else "Pas de saturation : tout va bien.",
                style = MaterialTheme.typography.bodySmall,
                color = if (gain < 0.7f) Neon.Amber else Neon.TextSecondary,
            )
        }
    }
}

@Composable
private fun Strip(name: String, c: Channel, meter: Float, index: Int, actions: MixerActions) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f),
        selectedLabelColor = Neon.Cyan,
    )
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilterChip(
                selected = c.mute,
                onClick = { actions.toggleMute(index) },
                label = { Text("Muet") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Red.copy(alpha = 0.25f), selectedLabelColor = Neon.Red),
                modifier = Modifier.testTag("mute_$index"),
            )
            FilterChip(selected = c.solo, onClick = { actions.toggleSolo(index) }, label = { Text("Solo") }, colors = chipColors,
                modifier = Modifier.testTag("solo_$index"))
        }
        LevelMeter(meter / 0.89f)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Volume", Modifier.weight(1f))
            Text(faderDb(c.volume), style = MonoStyle, color = Neon.Cyan)
        }
        Slider(value = c.volume, onValueChange = { actions.setVolume(index, it) }, modifier = Modifier.testTag("volume_$index"))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Panoramique", Modifier.weight(1f))
            val pan = (c.pan * 100).roundToInt()
            Text(
                when {
                    pan < -2 -> "Gauche ${-pan}"
                    pan > 2 -> "Droite $pan"
                    else -> "Centre"
                },
                style = MonoStyle, color = Neon.Cyan,
            )
        }
        Slider(value = c.pan, onValueChange = { v -> actions.setPan(index, if (kotlin.math.abs(v) < 0.04f) 0f else v) }, valueRange = -1f..1f)
    }
}
