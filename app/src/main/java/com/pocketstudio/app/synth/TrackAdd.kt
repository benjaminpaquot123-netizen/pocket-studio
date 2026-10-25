package com.pocketstudio.app.synth

import com.pocketstudio.ui.ScaledDialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.model.TrackKind
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SoundTile
import com.pocketstudio.ui.TileGrid

/** "+ Piste": what the new track is for, in a window over the synth screen. */
@Composable
fun TrackAddDialog(onDismiss: () -> Unit, voice: (() -> Unit)? = null, simple: Boolean = false, onPick: (TrackKind?, Boolean) -> Unit) {
    ScaledDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Neon.Surface, modifier = Modifier.widthIn(max = 520.dp)) {
            TrackAddChoices(onPick = onPick, onDismiss = onDismiss, modifier = Modifier.verticalScroll(rememberScrollState()), voice = voice, simple = simple)
        }
    }
}

/**
 * The choices of "+ Piste": a melody written for me (four bars in the song's key), a track of a
 * kind (melody, chords, bass, arpeggio, pluck) with a sound that suits it, or an empty one.
 * [onPick] gets the kind (null: the plain track of before) and whether to write the notes.
 */
@Composable
fun TrackAddChoices(
    onPick: (TrackKind?, Boolean) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier,
    /** Phase 36: « Voix (micro) », an audio track to sing on (null: not offered). */
    voice: (() -> Unit)? = null,
    /** Phase 36, Mode simple: the everyday kinds alone (no pluck), in everyday words. */
    simple: Boolean = false,
) {
    Column(modifier.padding(20.dp).testTag("track_add_sheet"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Ajouter une piste"), style = MaterialTheme.typography.titleLarge)
        Button(onClick = { onPick(TrackKind.Melody, true) }, modifier = Modifier.fillMaxWidth().testTag("track_add_write")) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Text("  " + tr("Écrire une mélodie pour moi"), textAlign = TextAlign.Center)
        }
        Text(tr("Une piste Mélodie avec 4 mesures déjà écrites dans la tonalité du morceau. « Annuler » la retire."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        Text(tr("Ou une piste à remplir toi-même, avec le bon son :"), style = MaterialTheme.typography.bodyMedium)
        val kinds = if (simple) TrackKind.entries - TrackKind.Pluck else TrackKind.entries
        TileGrid(kinds.size, minTileWidth = 260.dp) { i, m ->
            val kind = kinds[i]
            val look = SoundLooks.of(kind.family)
            SoundTile(
                title = if (simple && kind == TrackKind.Melody) tr("Mélodie") else if (simple && kind == TrackKind.Chords) tr("Accords") else tr(kind.title),
                icon = look.icon, accent = look.color, selected = false,
                onClick = { onPick(kind, false) }, subtitle = tr(kind.description), modifier = m.testTag("track_add_${kind.id}"),
            )
        }
        voice?.let { v ->
            androidx.compose.material3.OutlinedButton(onClick = v, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("track_add_voice")) {
                Icon(Icons.Filled.Mic, contentDescription = null)
                Text("  " + tr("Voix (micro) : chanter ou rapper sur le morceau"))
            }
        }
        TextButton(onClick = { onPick(null, false) }, modifier = Modifier.fillMaxWidth().testTag("track_add_empty")) {
            Text(tr("Piste vide (son de base)"))
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(tr("Annuler")) }
    }
}
