package com.pocketstudio.app.instruments

import com.pocketstudio.ui.pressScale
import com.pocketstudio.model.i18n.tr

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
            if (s.file == null) tr("N'importe quel son, même une seconde de ta voix : chaque touche le rejoue plus grave ou plus aigu.") + " " +
                tr("Prends-le dans la bibliothèque, dans un fichier, ou d'un pad avec « Jouer ce sample au clavier » dans Rythme.")
            else tr("« %s » sur tout le clavier.", s.name),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = ui.chooseSample, modifier = Modifier.pressScale().weight(1f).testTag("sampled_choose")) {
            Icon(Icons.Filled.LibraryMusic, contentDescription = null)
            Text(tr("  Bibliothèque"), maxLines = 1)
        }
        OutlinedButton(onClick = ui.importSample, modifier = Modifier.weight(1f).testTag("sampled_import")) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Text(tr("  Un fichier"), maxLines = 1)
        }
    }
    ui.sampledMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.Amber) }
    if (s.file == null) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Note d'origine"), style = MaterialTheme.typography.bodyMedium)
        HelpDot(Glossary.ROOT_NOTE)
        Box(Modifier.weight(1f))
        IconButton(onClick = { ui.editSampled(null) { it.copy(root = (it.root - 1f).coerceAtLeast(0f)) } }, modifier = Modifier.testTag("root_down")) {
            Icon(Icons.Filled.Remove, contentDescription = tr("Un demi-ton plus bas"))
        }
        Text(RootNote.name(s.root, com.pocketstudio.app.settings.englishNotes()), style = MonoStyle, color = Neon.Cyan, modifier = Modifier.testTag("root_note"))
        IconButton(onClick = { ui.editSampled(null) { it.copy(root = (it.root + 1f).coerceAtMost(127f)) } }, modifier = Modifier.testTag("root_up")) {
            Icon(Icons.Filled.Add, contentDescription = tr("Un demi-ton plus haut"))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = s.glide > 0f,
            onClick = { ui.editSampled(null) { it.copy(glide = if (it.glide > 0f) 0f else SampledPatch.SLIDE) } },
            label = { Text(tr("Glisse (808)")) }, colors = chipColors, modifier = Modifier.testTag("sampled_glide"),
        )
        HelpDot(Glossary.GLIDE)
        FilterChip(
            selected = s.loop,
            onClick = { ui.editSampled(null) { it.copy(loop = !it.loop) } },
            label = { Text(tr("En boucle")) }, colors = chipColors, modifier = Modifier.testTag("sampled_loop"),
        )
    }
    Text(
        if (s.glide > 0f) tr("Une seule note à la fois : tiens une touche et appuie sur une autre, la note glisse.")
        else tr("Plusieurs notes à la fois : les accords marchent."),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
    if (s.glide > 0f) {
        Seconds(tr("Temps de glisse"), s.glide, 0.01f..0.5f, "sampled_glide_time") { v -> ui.editSampled("glide") { it.copy(glide = v) } }
    }
    Seconds(tr("Attaque"), s.attack, 0f..1f, "sampled_attack") { v -> ui.editSampled("attack") { it.copy(attack = v) } }
    Seconds(tr("Relâchement"), s.release, 0f..2f, "sampled_release") { v -> ui.editSampled("release") { it.copy(release = v) } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Volume"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(com.pocketstudio.model.i18n.percent((s.gain * 100).roundToInt()), style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(value = s.gain / 2f, onValueChange = { v -> ui.editSampled("gain") { it.copy(gain = v * 2f) } }, modifier = Modifier.testTag("sampled_gain"))
}

@Composable
private fun Seconds(label: String, value: Float, range: ClosedFloatingPointRange<Float>, tag: String, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(tr("%.2f s", value), style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range, modifier = Modifier.testTag(tag))
}
