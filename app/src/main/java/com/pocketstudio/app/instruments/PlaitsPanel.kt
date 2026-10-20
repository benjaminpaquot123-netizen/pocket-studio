package com.pocketstudio.app.instruments

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SoundTile
import com.pocketstudio.ui.TileGrid
import com.pocketstudio.ui.FoldSection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Plaits: pick one of 24 models by family, then shape it with the module's knobs. */
@Composable
internal fun PlaitsPanel(ui: InstrumentUi) {
    val p = ui.plaits
    Text(
        "24 modèles de synthèse du module Plaits (Mutable Instruments) : du son analogique à la FM façon DX7, des cordes aux percussions.",
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
    PlaitsPatch.FAMILIES.forEach { (family, models) ->
        val look = SoundLooks.of(family)
        FoldSection(family, color = look.color, foldKey = "Plaits : $family", spacing = 8.dp, subtle = true) {
            TileGrid(models.size) { k, m ->
                val model = models[k]
                SoundTile(
                    title = PlaitsPatch.MODELS[model], icon = look.icon, accent = look.color, selected = p.model == model,
                    onClick = { ui.setPlaitsModel(model) }, subtitle = SoundLooks.PLAITS_HINTS.getOrNull(model),
                    modifier = m.testTag("plaits_model_$model"),
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
        ProSlider(value = values[param], onValueChange = { ui.setPlaitsParam(param, it) }, modifier = Modifier.testTag("plaits_param_$param"))
    }
}
