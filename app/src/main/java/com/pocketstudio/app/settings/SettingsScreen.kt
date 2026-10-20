package com.pocketstudio.app.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

class SettingsActions(
    val update: ((AppSettings) -> AppSettings) -> Unit = {},
    val reset: () -> Unit = {},
    val openGuide: () -> Unit = {},
)

/** Réglages: look, notes, feel, sound; the same for every project. */
@Composable
fun SettingsScreen(s: AppSettings, version: String, actions: SettingsActions) {
    val chip = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("settings"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NeonCard {
            SectionLabel("Affichage")
            Text("Thème", style = MaterialTheme.typography.bodyMedium)
            AppTheme.entries.forEach { t ->
                FilterChip(selected = s.theme == t, onClick = { actions.update { it.copy(theme = t) } }, label = { Text(t.label) },
                    colors = chip, modifier = Modifier.testTag("theme_${t.name}"))
            }
            Text("Taille de l'interface", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppSettings.UI_SCALES.forEach { scale ->
                    FilterChip(selected = s.uiScale == scale, onClick = { actions.update { it.copy(uiScale = scale) } },
                        label = { Text("${(scale * 100).roundToInt()} %") }, colors = chip, modifier = Modifier.testTag("scale_${(scale * 100).roundToInt()}"))
                }
            }
            Text("Noms des notes", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !s.englishNotes, onClick = { actions.update { it.copy(englishNotes = false) } }, label = { Text("Do Ré Mi") }, colors = chip)
                FilterChip(selected = s.englishNotes, onClick = { actions.update { it.copy(englishNotes = true) } }, label = { Text("C D E") }, colors = chip)
            }
        }
        NeonCard {
            SectionLabel("Toucher")
            Toggle("Vibrations", "Une petite vibration sur les pads et les appuis longs.", s.haptics, "haptics") { v -> actions.update { it.copy(haptics = v) } }
            Toggle("Écran toujours allumé pendant la lecture", "L'écran ne s'éteint pas tant que la musique joue.", s.keepScreenOn, "keep_screen") { v ->
                actions.update { it.copy(keepScreenOn = v) }
            }
        }
        NeonCard {
            SectionLabel("Son")
            Text("Volume du métronome : ${(s.metronomeLevel * 100).roundToInt()} %", style = MaterialTheme.typography.bodyMedium)
            Slider(value = s.metronomeLevel, onValueChange = { v -> actions.update { it.copy(metronomeLevel = v) } }, modifier = Modifier.testTag("metronome_level"))
            Text("Décompte avant d'enregistrer", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "Aucun", 1 to "1 mesure", 2 to "2 mesures").forEach { (bars, label) ->
                    FilterChip(selected = s.recordCountIn == bars, onClick = { actions.update { it.copy(recordCountIn = bars) } }, label = { Text(label) }, colors = chip)
                }
            }
            Text("Mode du son", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppSettings.BUFFERS.forEach { (bursts, label) ->
                    FilterChip(selected = s.bufferBursts == bursts, onClick = { actions.update { it.copy(bufferBursts = bursts) } }, label = { Text(label) },
                        colors = chip, modifier = Modifier.testTag("buffer_$bursts"))
                }
            }
            Text(
                "« Rapide » répond au doigt sans délai. Si le son craque, passe à « Sûr » : un tout petit peu plus de retard, mais fini les craquements.",
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
        NeonCard {
            SectionLabel("Aide")
            OutlinedButton(onClick = actions.openGuide, modifier = Modifier.fillMaxWidth().testTag("open_guide")) { Text("Revoir le guide des premiers pas") }
            TextButton(onClick = actions.reset, modifier = Modifier.fillMaxWidth().testTag("settings_reset")) { Text("Remettre les réglages par défaut") }
            Text("Pocket Studio $version", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
    }
}

@Composable
private fun Toggle(title: String, hint: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}
