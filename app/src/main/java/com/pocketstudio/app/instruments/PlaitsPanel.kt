package com.pocketstudio.app.instruments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import kotlin.math.roundToInt

/** Plaits: pick one of 24 models by family, then shape it with the module's knobs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaitsPanel(ui: InstrumentUi) {
    val p = ui.plaits
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    Text(
        "24 modèles de synthèse du module Plaits (Mutable Instruments) : du son analogique à la FM façon DX7, des cordes aux percussions.",
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
    PlaitsPatch.FAMILIES.forEach { (family, models) ->
        Text(family, style = MaterialTheme.typography.titleSmall, color = Neon.TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            models.forEach { m ->
                FilterChip(
                    selected = p.model == m,
                    onClick = { ui.setPlaitsModel(m) },
                    label = { Text(PlaitsPatch.MODELS[m]) },
                    colors = chipColors,
                    modifier = Modifier.testTag("plaits_model_$m"),
                )
            }
        }
    }
    val values = p.engineValues()
    PlaitsPatch.PARAMS.forEachIndexed { i, label ->
        val param = i + 1
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("${(values[param] * 100).roundToInt()}", style = MonoStyle, color = Neon.Cyan)
        }
        Slider(value = values[param], onValueChange = { ui.setPlaitsParam(param, it) }, modifier = Modifier.testTag("plaits_param_$param"))
    }
}
