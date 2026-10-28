package com.pocketstudio.app.mixer

import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.pocketstudio.model.i18n.inSentence
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Buses
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.model.i18n.tr

/** Short names for the slot chips (read when drawn, in the interface's language). */
private fun shortName(type: EffectType): String = when (type) {
    EffectType.NONE -> "—"
    EffectType.EQ -> "EQ"
    EffectType.COMPRESSOR -> "Comp"
    EffectType.SATURATION -> tr("Satu")
    EffectType.CHORUS -> "Chorus"
    EffectType.FILTER -> tr("Filtre")
    EffectType.PUMP -> tr("Pompe")
    EffectType.FLANGER -> "Flanger"
    EffectType.PHASER -> "Phaser"
    EffectType.GATE -> "Gate"
    EffectType.TREMOLO -> tr("Trémolo")
    EffectType.WIDENER -> tr("Large")
    EffectType.BITCRUSHER -> "Crush"
    EffectType.PARAM_EQ -> "EQ+"
    EffectType.ROBOT -> "Robot"
    EffectType.WAH -> "Wah"
    EffectType.VINYL -> tr("Vinyle")
    EffectType.RADIO -> "Radio"
    EffectType.CHOPPER -> tr("Hachoir")
    EffectType.DISTORTION -> tr("Disto")
    EffectType.ECHO -> tr("Écho")
    EffectType.PING_PONG -> tr("Ping-pong")
    EffectType.REVERB -> tr("Réverbe")
    EffectType.AUTO_PAN -> tr("Auto-pan")
    EffectType.OTT -> "OTT"
    EffectType.FREQ_SHIFT -> tr("Décalage")
    EffectType.TAPE_STOP -> tr("Arrêt")
    EffectType.WOBBLE -> "Wobble"
    EffectType.CLIPPER -> "Clip"
    EffectType.BASS_EXCITER -> tr("Basse+")
}

/** One-line summary of a channel's effects, e.g. "EQ · Comp · Réverbe". */
fun effectsSummary(inserts: List<Insert>, sends: List<Float> = emptyList()): String {
    val parts = inserts.filter { it.effect != EffectType.NONE && !it.bypass }.map { shortName(it.effect) } +
        sends.mapIndexedNotNull { b, v -> if (v > 0.01f) tr(Buses.NAMES[b]) else null }
    return parts.joinToString(" · ").ifEmpty { tr("Aucun") }
}

@Composable
fun SendSliders(ch: Int, sends: List<Float>, actions: MixerActions) {
    // « Tuto de A à Z »: the send its step asks for, dotted and brought into view (« Je le fais » opened this box).
    val asked = com.pocketstudio.app.tuto.TutoHints.send?.takeIf { it.first == ch }?.second
    sends.forEachIndexed { bus, level ->
        val wanted = bus == asked
        val bring = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
        if (wanted) androidx.compose.runtime.LaunchedEffect(ch, bus) { kotlinx.coroutines.delay(300); runCatching { bring.bringIntoView() } }
        val slider: @Composable () -> Unit = {
            LabeledSlider(tr("Envoi %s", tr(Buses.NAMES[bus]).inSentence()), com.pocketstudio.model.i18n.percent((level * 100).toInt()), level, "send_${ch}_$bus") {
                actions.setSend(ch, bus, it)
            }
        }
        // Only the asked one in a frame of its own: the others laid out as everywhere else.
        if (wanted) androidx.compose.foundation.layout.Column(
            Modifier.fillMaxWidth().bringIntoViewRequester(bring).then(com.pocketstudio.app.tuto.tutoDashed()).padding(6.dp).testTag("tuto_send_$bus"),
        ) {
            // Testers (lot « tuto 4 »): which of the step's strips is done, which is this one.
            com.pocketstudio.app.tuto.TutoHints.sendNote?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = Neon.Amber, modifier = Modifier.testTag("tuto_send_note")) }
            slider()
        } else slider()
    }
}

/** The reverb and the delay shared by every channel (their sends set how much each gets). */
@Composable
fun BusesCard(buses: List<List<Float>>, actions: MixerActions) {
    NeonCard(accent = Neon.Magenta) {
        SectionLabel(tr("Réverbe et écho"), color = Neon.Magenta, help = Glossary.REVERB_DELAY)
        Text(
            tr("Communs à toutes les voies : règle combien chaque voie y envoie dans ses effets. L'écho suit le tempo."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        buses.forEachIndexed { bus, params ->
            Text(tr(Buses.NAMES[bus]), style = MaterialTheme.typography.titleSmall, color = Neon.Magenta)
            Buses.PARAMS[bus].forEachIndexed { k, p ->
                LabeledSlider(tr(p.label), tr(p.format(params[k])), params[k], "bus_${bus}_$k") { actions.setBusParam(bus, k, it) }

            }
        }
    }
}

@Composable
internal fun LabeledSlider(label: String, value: String, v: Float, tag: String, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(value = v, onValueChange = onChange, modifier = Modifier.testTag(tag), label = label, valueText = value)
}
