package com.pocketstudio.app.remix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Project
import kotlinx.coroutines.launch
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongImport
import com.pocketstudio.model.SongPlace
import com.pocketstudio.model.SongSection
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledAlertDialog

/**
 * Step 5c, « Mettre Boys dans mon projet »: one screen with safe choices already made (the tempo
 * as step 4e says, the key, the whole song, its parts as bricks). « Mettre dans mon projet » puts
 * it in (one undo step), « Annuler » changes nothing. [sections] reads the song's parts (worker
 * thread); [put] gets the choices and the parts found.
 */
@Composable
fun SongImportSheet(
    set: StemSet, project: Project, sections: suspend () -> List<SongSection>,
    put: (SongImport, List<SongSection>) -> Unit, cancel: () -> Unit,
) {
    val info = set.info
    var choice by remember(set.id) { mutableStateOf(SongImport.defaults(project)) }
    var parts by remember(set.id) { mutableStateOf<List<SongSection>?>(null) }
    LaunchedEffect(set.id) { parts = runCatching { sections() }.getOrDefault(emptyList()) }
    val songBars = if (info.bpm > 0) ((set.seconds - info.firstBeat) / (240.0 / info.bpm)).toInt().coerceAtLeast(1) else 0
    val songFree = project.song.isEmpty()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var going by remember(set.id) { mutableStateOf(false) }
    ScaledAlertDialog(
        onDismissRequest = cancel,
        modifier = Modifier.testTag("song_import"),
        title = { Text(tr("Mettre %s dans mon projet", set.name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (info.bpm > 0) {
                    Heading(tr("Tempo"))
                    Choice(tr("Mon projet prend le tempo de %1\$s (%2\$s)", set.name, "%.0f BPM".format(info.bpm)), choice.takeTempo, "import_tempo_song") {
                        choice = choice.copy(takeTempo = true)
                    }
                    Choice(tr("%1\$s se cale sur mon tempo (%2\$s)", set.name, "%.0f BPM".format(project.tempoBpm)), !choice.takeTempo, "import_tempo_mine") {
                        choice = choice.copy(takeTempo = false)
                    }
                }
                info.keyLabel?.let { key ->
                    Heading(tr("La tonalité"))
                    Text(tr("%1\$s est en %2\$s.", set.name, key), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(choice.setKey, role = Role.Checkbox) { choice = choice.copy(setKey = it) }.testTag("import_key"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(choice.setKey, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Mes synthés et basses jouent en %s", key))
                    }
                }
                Heading(tr("Où la mettre"))
                Choice(tr("Toute la chanson, à partir de la brique A"), choice.place == SongPlace.Whole, "import_whole") { choice = choice.copy(place = SongPlace.Whole) }
                if (songBars > 1) {
                    Choice(tr("Seulement un passage"), choice.place == SongPlace.Passage, "import_passage") { choice = choice.copy(place = SongPlace.Passage) }
                    if (choice.place == SongPlace.Passage) {
                        Stepper(tr("De la mesure %d", choice.fromBar + 1), "import_from") { d ->
                            choice = choice.copy(fromBar = (choice.fromBar + d * 4).coerceIn(0, (songBars - 1).coerceAtLeast(0)))
                        }
                        Stepper(tr("%d mesures", choice.bars), "import_bars") { d ->
                            choice = choice.copy(bars = (choice.bars + d * 4).coerceIn(4, 64))
                        }
                    }
                }
                Choice(tr("Juste la voix sur mon beat"), choice.place == SongPlace.VoiceOnly, "import_voice") { choice = choice.copy(place = SongPlace.VoiceOnly) }
                Heading(tr("Parties"))
                val can = songFree && choice.place != SongPlace.Passage
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(choice.bricks && can, enabled = can, role = Role.Switch) { choice = choice.copy(bricks = it) }.testTag("import_bricks"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Faire des briques avec ses parties"))
                        val found = parts
                        Text(
                            when {
                                !songFree -> tr("Ton morceau a déjà ses parties")
                                found == null -> tr("Je cherche les parties…")
                                found.isEmpty() -> tr("Pas de parties trouvées")
                                else -> found.joinToString(" · ") { Scene.shownLabel(it.label) }
                            },
                            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                            modifier = Modifier.testTag("import_parts"),
                        )
                    }
                    Switch(choice.bricks && can, onCheckedChange = null, enabled = can)
                }
            }
        },
        confirmButton = {
            val bricksPossible = songFree && choice.place != SongPlace.Passage
            Button(
                onClick = {
                    val found = parts
                    if (going) return@Button
                    going = true
                    if (choice.bricks && bricksPossible && found == null) {
                        // The parts still being read: they are waited for, then it goes in.
                        scope.launch { val read = runCatching { sections() }.getOrDefault(emptyList()); put(choice.copy(bricks = read.isNotEmpty()), read) }
                    } else {
                        put(choice.copy(bricks = choice.bricks && bricksPossible && !found.isNullOrEmpty()), found.orEmpty())
                    }
                },
                modifier = Modifier.testTag("import_go"),
            ) { Text(tr("Mettre dans mon projet")) }
        },
        dismissButton = { TextButton(onClick = cancel, modifier = Modifier.testTag("import_cancel")) { Text(tr("Annuler")) } },
    )
}

@Composable
private fun Heading(text: String) =
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Neon.Cyan, modifier = Modifier.padding(top = 8.dp))

@Composable
private fun Choice(text: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun Stepper(text: String, tag: String, step: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { step(-1) }, modifier = Modifier.testTag("${tag}_less")) { Text("−") }
        Text(text, Modifier.weight(1f), maxLines = 1)
        OutlinedButton(onClick = { step(1) }, modifier = Modifier.testTag("${tag}_more")) { Text("+") }
    }
}
