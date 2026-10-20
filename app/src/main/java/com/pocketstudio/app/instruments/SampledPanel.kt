package com.pocketstudio.app.instruments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.help.Glossary
import com.pocketstudio.model.RootNote
import com.pocketstudio.model.SampledPatch
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.WithHelp
import java.util.Locale
import kotlin.math.roundToInt

/** The track plays a sound across the keys: which sound, its note, glide, loop and envelope. */
@Composable
internal fun SampledPanel(ui: InstrumentUi) {
    val s = ui.sampled
    val chipColors = pillColors()
    WithHelp(Glossary.SAMPLED) {
        Text(
            if (s.file == null) "N'importe quel son, même une seconde de ta voix : chaque touche le rejoue plus grave ou plus aigu. " +
                "Prends-le dans la bibliothèque, dans un fichier, ou d'un pad avec « Jouer ce sample au clavier » dans Rythme."
            else "« ${s.name} » sur tout le clavier.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = ui.chooseSample, modifier = Modifier.weight(1f).testTag("sampled_choose")) {
            Icon(Icons.Filled.LibraryMusic, contentDescription = null)
            Text("  Bibliothèque", maxLines = 1)
        }
        OutlinedButton(onClick = ui.importSample, modifier = Modifier.weight(1f).testTag("sampled_import")) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Text("  Un fichier", maxLines = 1)
        }
    }
    ui.sampledMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.Amber) }
    if (s.file == null) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Note d'origine", style = MaterialTheme.typography.bodyMedium)
        HelpDot(Glossary.ROOT_NOTE)
        Box(Modifier.weight(1f))
        IconButton(onClick = { ui.editSampled(null) { it.copy(root = (it.root - 1f).coerceAtLeast(0f)) } }, modifier = Modifier.testTag("root_down")) {
            Icon(Icons.Filled.Remove, contentDescription = "Un demi-ton plus bas")
        }
        Text(RootNote.name(s.root), style = MonoStyle, color = Neon.Cyan, modifier = Modifier.testTag("root_note"))
        IconButton(onClick = { ui.editSampled(null) { it.copy(root = (it.root + 1f).coerceAtMost(127f)) } }, modifier = Modifier.testTag("root_up")) {
            Icon(Icons.Filled.Add, contentDescription = "Un demi-ton plus haut")
        }
    }
    // Read octaves away from its own pitch, a sound lasts a fraction of itself (a 808 under chords
    // becomes a click): say so, and put it back in the octave of the notes in one touch.
    val fitted = SampledPatch.fitRoot(s.root, ui.notes)
    val octaves = Math.round((fitted - s.root) / 12f)
    if (ui.notes.isNotEmpty() && octaves != 0) {
        Text(
            if (octaves > 0) "Les notes de la piste jouent ce son $octaves octave${if (octaves > 1) "s" else ""} plus haut : il est lu ${1 shl minOf(octaves, 4)} fois plus vite et ne dure presque rien."
            else "Les notes de la piste jouent ce son ${-octaves} octave${if (octaves < -1) "s" else ""} plus bas : il est lu au ralenti.",
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber,
        )
        OutlinedButton(
            onClick = { ui.editSampled(null) { it.copy(root = fitted, glide = if (SampledPatch.isMonophonic(ui.notes)) it.glide else 0f) } },
            modifier = Modifier.testTag("root_fit"),
        ) { Text("Le caler sur les notes (${RootNote.name(fitted)})") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = s.glide > 0f,
            onClick = { ui.editSampled(null) { it.copy(glide = if (it.glide > 0f) 0f else SampledPatch.SLIDE) } },
            label = { Text("Glisse (808)") }, colors = chipColors, modifier = Modifier.testTag("sampled_glide"),
        )
        HelpDot(Glossary.GLIDE)
        FilterChip(
            selected = s.loop,
            onClick = { ui.editSampled(null) { it.copy(loop = !it.loop) } },
            label = { Text("En boucle") }, colors = chipColors, modifier = Modifier.testTag("sampled_loop"),
        )
    }
    Text(
        if (s.glide > 0f) "Une seule note à la fois : tiens une touche et appuie sur une autre, la note glisse."
        else "Plusieurs notes à la fois : les accords marchent.",
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
    if (s.glide > 0f) {
        Seconds("Temps de glisse", s.glide, 0.01f..0.5f, "sampled_glide_time") { v -> ui.editSampled("glide") { it.copy(glide = v) } }
    }
    Seconds("Attaque", s.attack, 0f..1f, "sampled_attack") { v -> ui.editSampled("attack") { it.copy(attack = v) } }
    Seconds("Relâchement", s.release, 0f..2f, "sampled_release") { v -> ui.editSampled("release") { it.copy(release = v) } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Volume", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("${(s.gain * 100).roundToInt()} %", style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(value = s.gain / 2f, onValueChange = { v -> ui.editSampled("gain") { it.copy(gain = v * 2f) } }, modifier = Modifier.testTag("sampled_gain"))
}

@Composable
private fun Seconds(label: String, value: Float, range: ClosedFloatingPointRange<Float>, tag: String, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(String.format(Locale.FRANCE, "%.2f s", value), style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range, modifier = Modifier.testTag(tag))
}
