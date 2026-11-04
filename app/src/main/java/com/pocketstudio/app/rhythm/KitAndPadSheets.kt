@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.rhythm

import com.pocketstudio.ui.ScaledBottomSheet

import com.pocketstudio.app.learn.spot
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.percent
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ProSlider
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt

/** A kit « Kit ▾ » offers: [id] "builtin:3", "gm:<program>" or "pack:<id>"; [group] one of [BUILT_IN], [GM], [PACK]. */
data class KitChoice(val id: String, val name: String, val group: Int) {
    companion object {
        const val BUILT_IN = 0
        const val GM = 1
        const val PACK = 2
    }
}

/**
 * The kit on the pads now, as « Kit ▾ » says it: the kit built in (Studio or Classique) when no
 * pad has a sound of its own, the kit's name when all the pads' sounds come from one kit
 * (« Standard · Caisse »), else « Perso ».
 */
fun kitName(p: Project): String {
    // Cuts of a separated song (« Voix 2 ») sit on free pads: they never rename the kit (R2).
    val samples = p.drums.lanes.mapNotNull { it.sample?.name }.filterNot(com.pocketstudio.model.RemixVoice::isCut)
    if (samples.isEmpty()) return when {
        // Tutorial lot 3: the drum & bass kit built in (its short kick on the kick's pad).
        com.pocketstudio.model.StudioDrums.isDnbKick(p.padVoices[0]) -> tr("Drum & bass")
        // Lot 2: the kit with the 808 on its pad (« Trap (808) », « Drill (808) », the assistant's trap).
        p.padVoices.values.any { it.kind == com.pocketstudio.model.PadVoice.BASS_808 } -> tr("Studio + 808")
        p.kitVersion >= Project.KIT_V2 -> tr("Studio")
        else -> tr("Classique")
    }
    val kit = samples.map { it.substringBefore(" · ", "") }.toSet().singleOrNull()?.takeIf { it.isNotBlank() } ?: return tr("Perso")
    return DefaultNames.shownSample(samples.first()).substringBefore(" · ").ifBlank { kit }
}

/** « Kit : Studio ▾ » at the head of the drums: a touch lists the kits. */
@Composable
internal fun KitChip(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(tr("Kit : %s", name), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        colors = AssistChipDefaults.assistChipColors(labelColor = Neon.TextPrimary, trailingIconContentColor = Neon.Cyan),
        border = BorderStroke(1.dp, Neon.Cyan.copy(alpha = 0.6f)),
        modifier = modifier.heightIn(min = 48.dp).spot("kit_chip", tr("Changer le kit")).testTag("kit_chip"),
    )
}

/** The kits in a sheet: a touch puts one on the 16 pads (one undo step) and closes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KitSheet(current: String, kits: List<KitChoice>, choose: (String) -> Unit, close: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("kit_sheet")) {
        KitSheetContent(current, kits, { close(); choose(it) }, close)
    }
}

@Composable
fun KitSheetContent(current: String, kits: List<KitChoice>, choose: (String) -> Unit, close: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Kit de batterie"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(tr("Touche un kit : il remplace les sons des 16 pads. Annuler le remet."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            TextButton(onClick = close, modifier = Modifier.testTag("kit_sheet_close")) { Text(tr("Fermer")) }
        }
        listOf(
            KitChoice.BUILT_IN to tr("Dans l'appli"),
            KitChoice.PACK to tr("Packs téléchargés"),
            KitChoice.GM to tr("Kits General MIDI"),
        ).forEach { (group, title) ->
            val list = kits.filter { it.group == group }
            if (list.isEmpty()) return@forEach
            Text(title, style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
                list.forEach { kit ->
                    val chosen = kit.name == current
                    OutlinedButton(
                        onClick = { choose(kit.id) },
                        border = BorderStroke(if (chosen) 2.dp else 1.dp, if (chosen) Neon.Cyan else Neon.Outline),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("kit_choice_${kit.id}"),
                    ) {
                        Text(kit.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                if (list.size % 2 == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (kits.none { it.group != KitChoice.BUILT_IN }) {
            Text(
                tr("Plus de kits : télécharge des packs dans la bibliothèque de sons, ou la banque d'instruments dans Sons › une piste › Instruments."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
    }
}

/** What a pad's sheet shows: its sound, its own volume and pan, whether it is cut. */
data class PadSheetUi(
    val lane: Int,
    val name: String,
    val volume: Float,
    val pan: Float,
    val muted: Boolean,
    val hasSample: Boolean,
    /** Sounds of the pad's kind to put on it in one touch (the kit's voices, the packs'). */
    val sounds: List<String> = emptyList(),
    /** The pad is heard alone in a loop now (« Écouter seul »). */
    val alone: Boolean = false,
    /** Mode Pro: the pad's « Couches » show (other sounds for harder hits). */
    val pro: Boolean = false,
    /** The pad's velocity layers: name and the velocity (1..127) each starts from. */
    val layers: List<Pair<String, Int>> = emptyList(),
)

fun padSheetUi(p: Project, lane: Int, sounds: List<String>, alone: Boolean = false, pro: Boolean = false): PadSheetUi {
    val strip = p.mixer.channels.getOrNull(Mixer.padChannel(lane))
    val sample = p.drums.lanes[lane].sample
    return PadSheetUi(
        lane, padName(p, lane), strip?.volume ?: 0.8f, p.drums.lanes[lane].pan, strip?.mute == true, sample != null, sounds, alone,
        pro = pro, layers = sample?.layers.orEmpty().map { DefaultNames.shownSample(it.name) to it.from },
    )
}

/** Callbacks of a pad's sheet. */
class PadSheetActions(
    val pickSound: (Int) -> Unit = {},
    val origin: () -> Unit = {},
    val more: () -> Unit = {},
    val setVolume: (Float) -> Unit = {},
    val setPan: (Float) -> Unit = {},
    val toggleMute: () -> Unit = {},
    /** The pan's gesture ended (one journal line). */
    val panDone: () -> Unit = {},
    /** « ▶ Écouter seul »: this pad alone in a loop (again: it stops); null: not offered. */
    val listenAlone: (() -> Unit)? = null,
    /** The sheet keeps « Écouter seul » going while it shows (the returned call lets go). */
    val holdAlone: () -> (() -> Unit) = { {} },
    /** « Couches »: sound [i] of the sheet's list becomes a layer for the hard hits. */
    val addLayer: (Int) -> Unit = {},
    val removeLayer: (Int) -> Unit = {},
    /** Layer [index] starts from velocity [from] (1..127). */
    val setLayerFrom: (index: Int, from: Int) -> Unit = { _, _ -> },
)

/** A pad held: its sheet (another sound in one touch, its volume and pan, cut it). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PadSheet(ui: PadSheetUi, actions: PadSheetActions, close: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    HoldAlone(true, actions.holdAlone)
    ScaledBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("pad_sheet")) {
        PadSheetContent(
            ui,
            PadSheetActions(
                // Heard alone: the sheet stays, so each sound is compared in the loop.
                pickSound = { if (!ui.alone) close(); actions.pickSound(it) }, origin = { if (!ui.alone) close(); actions.origin() }, more = { close(); actions.more() },
                setVolume = actions.setVolume, setPan = actions.setPan, toggleMute = actions.toggleMute, panDone = actions.panDone,
                listenAlone = actions.listenAlone,
                addLayer = actions.addLayer, removeLayer = actions.removeLayer, setLayerFrom = actions.setLayerFrom,
            ),
            close,
        )
    }
}

@Composable
fun PadSheetContent(ui: PadSheetUi, actions: PadSheetActions, close: () -> Unit) {
    val color = DrumKitInfo.colors[ui.lane]
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Pad %1\$d · %2\$s", ui.lane + 1, ui.name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).testTag("pad_sheet_title"))
            TextButton(onClick = close, modifier = Modifier.testTag("pad_sheet_close")) { Text(tr("Fermer")) }
        }
        actions.listenAlone?.let { AloneButton(ui.alone, tr("Écouter seul"), ui.name, "pad_alone", toggle = it) }
        if (ui.alone) Text(tr("Joue en boucle, tout seul : change le son pour comparer."), style = MaterialTheme.typography.bodySmall, color = Neon.Acid)
        Text(tr("Son"), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
            if (ui.hasSample) SoundTile(tr("Son d'origine"), "pad_sound_origin", Modifier.weight(1f), actions.origin)
            ui.sounds.forEachIndexed { i, name -> SoundTile(name, "pad_sound_$i", Modifier.weight(1f)) { actions.pickSound(i) } }
            OutlinedButton(onClick = actions.more, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("pad_sound_more")) {
                Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                // Remix testers (Mehdi): « Tous les » cut at one line next to the icon; whole, on two if it must.
                Text(tr("Tous les sons…"), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        // Volume: 0.8 is the kit's own level (100 %).
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Volume du pad"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(percent((ui.volume / 0.8f * 100).roundToInt()), style = MonoStyle, color = Neon.Cyan)
        }
        // Audit ultra tour 0 (seat b): the slider is named as its line says (TalkBack read « curseur » alone).
        val volumeName = tr("Volume du pad")
        ProSlider(value = ui.volume, onValueChange = actions.setVolume, modifier = Modifier.semantics { contentDescription = volumeName }.testTag("pad_volume"))
        // Safe at the edge: a back swipe from the side closes the sheet, it never pans the pad.
        PanRow(tr("Place du pad"), ui.pan, onChange = actions.setPan, onEnd = actions.panDone, tag = "pad_pan")
        FilterChip(
            selected = ui.muted, onClick = actions.toggleMute, colors = pillColors(),
            label = { Text(if (ui.muted) tr("Pad coupé : touche pour l'entendre") else tr("Couper ce pad")) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("pad_mute"),
        )
        if (ui.pro && ui.hasSample) PadLayers(ui, actions)
    }
}

/** « Couches » (Pro): other sounds for the pad's harder hits, each from a strength of its own. */
@Composable
private fun PadLayers(ui: PadSheetUi, actions: PadSheetActions) {
    var choosing by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    com.pocketstudio.ui.WithHelp(
        com.pocketstudio.app.help.ScreenHelp.LAYERS, about = listOf("pad_layers", "pad_layer_add", "pad_layer_slider_*", "pad_layer_remove_*"),
    ) {
        Text(tr("Couches"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("pad_layers"))
    }
    Text(
        tr("Frappe forte : un autre son. Le son du pad garde les coups doux."),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
    ui.layers.forEachIndexed { i, (name, from) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Text(tr("dès %s", percent((from * 100f / 127f).roundToInt())), style = MonoStyle, color = Neon.Cyan, modifier = Modifier.testTag("pad_layer_from_$i"))
            TextButton(onClick = { actions.removeLayer(i) }, modifier = Modifier.heightIn(min = 48.dp).testTag("pad_layer_remove_$i")) { Text(tr("Retirer")) }
        }
        ProSlider(
            value = from.toFloat(), valueRange = com.pocketstudio.model.PadLayer.FROM_RANGE.first.toFloat()..com.pocketstudio.model.PadLayer.FROM_RANGE.last.toFloat(),
            onValueChange = { actions.setLayerFrom(i, it.roundToInt()) }, label = name, valueText = tr("dès %s", percent((from * 100f / 127f).roundToInt())), modifier = Modifier.testTag("pad_layer_slider_$i"),
        )
    }
    if (ui.layers.size < com.pocketstudio.model.PadLayer.MAX && ui.sounds.isNotEmpty()) {
        OutlinedButton(onClick = { choosing = !choosing }, modifier = Modifier.heightIn(min = 48.dp).testTag("pad_layer_add")) {
            Text(if (choosing) tr("Fermer") else tr("Ajouter une couche"))
        }
        if (choosing) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
            ui.sounds.forEachIndexed { i, name -> SoundTile(name, "pad_layer_sound_$i", Modifier.weight(1f)) { choosing = false; actions.addLayer(i) } }
        }
    }
}

@Composable
private fun SoundTile(name: String, tag: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp).testTag(tag)) {
        Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
