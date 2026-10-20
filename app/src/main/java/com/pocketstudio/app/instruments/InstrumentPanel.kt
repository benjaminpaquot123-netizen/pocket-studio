package com.pocketstudio.app.instruments

import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.LibraryState
import com.pocketstudio.engine.Preset
import com.pocketstudio.model.InstrumentChoice
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.model.SampledPatch
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

private const val KITS = 16

class InstrumentUi(
    val library: LibraryState,
    val current: InstrumentChoice?,
    val open: () -> Unit = {},
    val download: () -> Unit = {},
    val cancel: () -> Unit = {},
    val select: (InstrumentChoice?) -> Unit = {},
    val bankId: String = InstrumentLibrary.BANKS[0].id,
    val selectBank: (String) -> Unit = {},
    val loadKit: (Preset) -> Unit = {},
    /** The synth track's own voice when no instrument is chosen: 0 subtractive, 1 Plaits, 2 a sampled sound. */
    val synthEngine: Int = 0,
    val plaits: PlaitsPatch = PlaitsPatch(),
    val selectEngine: (Int) -> Unit = {},
    val setPlaitsModel: (Int) -> Unit = {},
    val setPlaitsParam: (Int, Float) -> Unit = { _, _ -> },
    val sampled: SampledPatch = SampledPatch(),
    /** Opens the sound library to pick the track's sampled sound. */
    val chooseSample: () -> Unit = {},
    /** [key]: slider drags with the same key merge into one undo step (null = a step of its own). */
    val editSampled: (String?, (SampledPatch) -> SampledPatch) -> Unit = { _, _ -> },
    val sampledMessage: String? = null,
)

/** Where the synth track's sound comes from: our synth, or one of 128 General MIDI instruments. */
@Composable
fun InstrumentPanel(ui: InstrumentUi) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    var showLibrary by rememberSaveable { mutableIntStateOf(if (ui.current != null) 1 else 0) }
    NeonCard(modifier = Modifier.testTag("instrument_panel")) {
        SectionLabel("Source du son", help = Glossary.SOURCE)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = ui.current == null && showLibrary == 0 && ui.synthEngine == 0,
                onClick = { showLibrary = 0; ui.selectEngine(0) },
                label = { Text("Synthé") }, colors = chipColors,
            )
            FilterChip(
                selected = ui.current == null && showLibrary == 0 && ui.synthEngine == 1,
                onClick = { showLibrary = 0; ui.selectEngine(1) },
                label = { Text("Plaits") }, colors = chipColors,
                modifier = Modifier.testTag("source_plaits"),
            )
            FilterChip(
                selected = ui.current == null && showLibrary == 0 && ui.synthEngine == 2,
                onClick = { showLibrary = 0; ui.selectEngine(2) },
                label = { Text("Échantillon") }, colors = chipColors,
                modifier = Modifier.testTag("source_sampled"),
            )
            FilterChip(
                selected = ui.current != null || showLibrary == 1,
                onClick = { showLibrary = 1; ui.open() },
                label = { Text("Instruments") }, colors = chipColors,
                modifier = Modifier.testTag("source_instruments"),
            )
        }
        if (showLibrary == 0 && ui.current == null && ui.synthEngine == 1) PlaitsPanel(ui)
        if (showLibrary == 0 && ui.current == null && ui.synthEngine == 2) SampledPanel(ui)
        if (showLibrary == 1 || ui.current != null) {
            LaunchedEffect(Unit) { ui.open() }
            val bank = InstrumentLibrary.bank(ui.bankId)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InstrumentLibrary.BANKS.forEach { b ->
                    FilterChip(
                        selected = b.id == ui.bankId,
                        onClick = { ui.selectBank(b.id) },
                        label = { Text(b.name) },
                        colors = chipColors,
                        modifier = Modifier.testTag("bank_${b.id}"),
                    )
                }
            }
            Text(bank.description, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            when (val lib = ui.library) {
                LibraryState.Missing -> {
                    Text(
                        "À télécharger une seule fois (${bank.sizeLabel}, Wi-Fi conseillé). Licence libre.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = ui.download, modifier = Modifier.fillMaxWidth().testTag("download_library")) {
                        Text("Télécharger ${bank.name} (${bank.sizeLabel})")
                    }
                }
                is LibraryState.Downloading -> {
                    Text("Téléchargement… ${(lib.progress * 100).roundToInt()} %")
                    LinearProgressIndicator(progress = { lib.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Neon.Cyan)
                    OutlinedButton(onClick = ui.cancel, modifier = Modifier.fillMaxWidth()) { Text("Annuler") }
                }
                LibraryState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.padding(2.dp), strokeWidth = 2.dp)
                    Text("Ouverture de la bibliothèque…")
                }
                is LibraryState.Failed -> {
                    Text(lib.message, color = Neon.Red)
                    Button(onClick = ui.download, modifier = Modifier.fillMaxWidth()) { Text("Réessayer") }
                }
                is LibraryState.Ready -> Browser(lib.presets, ui.current, ui.bankId, ui.select, ui.loadKit)
            }
        }
    }
}

@Composable
private fun Browser(
    presets: List<Preset>,
    current: InstrumentChoice?,
    bankId: String,
    select: (InstrumentChoice?) -> Unit,
    loadKit: (Preset) -> Unit,
) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    var family by rememberSaveable {
        mutableIntStateOf(if (current?.bank == 128) KITS else current?.program?.div(8) ?: 0)
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        InstrumentLibrary.FAMILIES.forEachIndexed { i, name ->
            FilterChip(selected = family == i, onClick = { family = i }, label = { Text(name) }, colors = chipColors)
        }
        FilterChip(selected = family == KITS, onClick = { family = KITS }, label = { Text("Kits de batterie") }, colors = chipColors,
            modifier = Modifier.testTag("family_kits"))
    }
    if (family == KITS) {
        Text("Touche un kit : il se joue au clavier et dans le piano roll (Do = grosse caisse, Ré = caisse claire, Fa# = charleston).",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        val kits = presets.filter { it.bank == 128 }.sortedBy { it.program }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            kits.forEach { kit ->
                FilterChip(
                    selected = current?.bankId == bankId && current.bank == 128 && current.program == kit.program,
                    onClick = { select(InstrumentChoice(128, kit.program, kit.name, bankId)) },
                    label = { Text(kit.name) },
                    colors = chipColors,
                    modifier = Modifier.testTag("kit_${kit.program}"),
                )
            }
        }
        val chosen = kits.firstOrNull { current?.bankId == bankId && current.bank == 128 && current.program == it.program }
        if (chosen != null) {
            Button(onClick = { loadKit(chosen) }, modifier = Modifier.fillMaxWidth().testTag("kit_to_pads")) {
                Text("Mettre « ${chosen.name} » sur les 16 pads du Rythme")
            }
        }
        return
    }
    // Every variation of the family (bank 0 first, then the bank's extras).
    val inFamily = presets.filter { it.bank < 128 && it.program / 8 == family }.sortedWith(compareBy({ it.program }, { it.bank }))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        inFamily.forEach { p ->
            FilterChip(
                selected = current?.bankId == bankId && current.bank == p.bank && current.program == p.program,
                onClick = { select(InstrumentChoice(p.bank, p.program, p.name, bankId)) },
                label = { Text(p.name) },
                colors = chipColors,
                modifier = Modifier.testTag("preset_${p.bank}_${p.program}"),
            )
        }
    }
    Text(
        current?.let { "Joue : ${it.name}. Le piano roll et le clavier utilisent cet instrument." } ?: "Choisis un instrument : il remplace le synthé.",
        style = MaterialTheme.typography.bodySmall,
        color = Neon.TextSecondary,
    )
}
