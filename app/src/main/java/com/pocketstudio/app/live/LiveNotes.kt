package com.pocketstudio.app.live

import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.settings.KeyLayout
import com.pocketstudio.app.synth.PlayKeys
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.NoteNames
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.pillColors

/**
 * The Live page's « Notes »: play an instrument track live, on the notes of the key (no wrong
 * note) or on a piano. During a take the notes go into it, each on its sixteenth, in the part the
 * track plays.
 */
@Composable
internal fun LiveNotes(ui: LiveScreenUi, actions: LiveActions, modifier: Modifier = Modifier) {
    val tracks = ui.project.instrumentTracks
    Column(modifier.testTag("live_notes"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ui.cellRecording?.key == com.pocketstudio.model.SongPart.DRUMS) {
            Text(
                tr("Joue aux pads : ils vont dans la case. Retouche-la pour arrêter."),
                style = MaterialTheme.typography.bodySmall, color = Neon.Red, modifier = Modifier.padding(horizontal = 2.dp),
            )
            LiveDrumPads(List(8) { com.pocketstudio.app.rhythm.padName(ui.project, it) }, actions.drumHit)
            return@Column
        }
        if (tracks.isEmpty()) {
            Text(tr("Pas de piste à jouer : ajoute une piste dans Synthé."), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        var chosen by rememberSaveable { mutableIntStateOf(ui.selectedTrack?.takeIf { s -> tracks.any { it.id == s } } ?: tracks.first().id) }
        // Lot 4b: a cell recording plays its own track (the drums: their pads, below).
        val rec = ui.cellRecording
        val recId = rec?.key?.takeIf { it.startsWith("inst:") }?.removePrefix("inst:")?.toIntOrNull()
        androidx.compose.runtime.LaunchedEffect(recId) { if (recId != null) chosen = recId }
        val id = tracks.firstOrNull { it.id == chosen }?.id ?: tracks.first().id
        // Step 4f: a MIDI keyboard plays the track shown here (its notes go where these keys' go).
        androidx.compose.runtime.LaunchedEffect(id) { actions.selectTrack(id) }
        var octave by rememberSaveable { mutableIntStateOf(4) }
        var inKey by rememberSaveable { mutableStateOf(true) }
        val chip = pillColors()
        Row(Modifier.fillMaxWidth().systemGestureExclusion().fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tracks.forEach { t ->
                FilterChip(
                    selected = t.id == id, onClick = { chosen = t.id }, label = { Text(DefaultNames.shown(t.name), maxLines = 1) }, colors = chip,
                    modifier = Modifier.testTag("live_notes_track_${t.id}"),
                )
            }
        }
        val settings by com.pocketstudio.app.settings.SettingsStore.state.collectAsState()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = inKey, onClick = { inKey = true }, label = { Text(tr("Gamme")) }, colors = chip, modifier = Modifier.testTag("live_notes_scale"))
            FilterChip(selected = !inKey, onClick = { inKey = false }, label = { Text(tr("Clavier")) }, colors = chip, modifier = Modifier.testTag("live_notes_piano"))
            Box(Modifier.weight(1f))
            IconButton(onClick = { octave = (octave - 1).coerceAtLeast(1) }, enabled = octave > 1) { Icon(Icons.Filled.Remove, contentDescription = tr("Octave plus grave")) }
            Text(NoteNames.pitchClass(12 * (octave + 1), settings.englishNotes) + octave, style = MonoStyle, color = Neon.Cyan)
            IconButton(onClick = { octave = (octave + 1).coerceAtMost(7) }, enabled = octave < 7) { Icon(Icons.Filled.Add, contentDescription = tr("Octave plus aiguë")) }
        }
        Text(
            if (rec != null) tr("Tes notes vont dans la case. Retouche-la pour arrêter : la boucle se cale sur la mesure.")
            else if (ui.take is TakeUi.Recording) tr("Tes notes s'ajoutent à la boucle de la partie que joue la piste, comme un looper, chacune sur sa double croche.")
            else tr("Joue en direct. Avec ●, tes notes vont dans la prise."),
            style = MaterialTheme.typography.bodySmall, color = if (rec != null || ui.take is TakeUi.Recording) Neon.Red else Neon.TextSecondary,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        // A glissando from the screen's edge plays, it does not go back.
        Box(Modifier.systemGestureExclusion()) {
            PlayKeys(
                if (inKey) KeyLayout.Scale else KeyLayout.TwoOctaves, octave, 2, ui.project.harmony, settings.englishNotes,
                onNoteOn = { pitch, velocity -> actions.noteOn(id, pitch, velocity) }, onNoteOff = { pitch -> actions.noteOff(id, pitch) },
                height = 150.dp, tag = "live_notes_keys", keyTag = "live_note",
            )
        }
    }
}
