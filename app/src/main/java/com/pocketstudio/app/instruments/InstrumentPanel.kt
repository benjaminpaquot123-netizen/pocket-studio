package com.pocketstudio.app.instruments

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
)

/** Where the synth track's sound comes from: our synth, or one of 128 General MIDI instruments. */
@Composable
fun InstrumentPanel(ui: InstrumentUi) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    var showLibrary by rememberSaveable { mutableIntStateOf(if (ui.current != null) 1 else 0) }
    NeonCard(modifier = Modifier.testTag("instrument_panel")) {
        SectionLabel("Source du son")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = ui.current == null && showLibrary == 0,
                onClick = { showLibrary = 0; ui.select(null) },
                label = { Text("Synthé") }, colors = chipColors,
            )
            FilterChip(
                selected = ui.current != null || showLibrary == 1,
                onClick = { showLibrary = 1; ui.open() },
                label = { Text("Instruments") }, colors = chipColors,
                modifier = Modifier.testTag("source_instruments"),
            )
        }
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
    var family by rememberSaveable { mutableIntStateOf(current?.takeIf { it.bank < 128 }?.program?.div(8) ?: 0) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        InstrumentLibrary.FAMILIES.forEachIndexed { i, name ->
            FilterChip(selected = family == i, onClick = { family = i }, label = { Text(name) }, colors = chipColors)
        }
        FilterChip(selected = family == KITS, onClick = { family = KITS }, label = { Text("Kits de batterie") }, colors = chipColors,
            modifier = Modifier.testTag("family_kits"))
    }
    if (family == KITS) {
        Text("Touche un kit : ses sons vont sur les 16 pads du Rythme (Annuler possible).",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            presets.filter { it.bank == 128 }.sortedBy { it.program }.forEach { kit ->
                OutlinedButton(onClick = { loadKit(kit) }, modifier = Modifier.testTag("kit_${kit.program}")) { Text(kit.name) }
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
