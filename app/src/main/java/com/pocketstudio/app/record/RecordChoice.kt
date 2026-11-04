package com.pocketstudio.app.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.settings.observedSettings
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledAlertDialog
import com.pocketstudio.ui.WholeWordsText

/**
 * Audit ultra round 1 (n° 30, seats A d, C c): the top bar's ● where nothing is played (Mon
 * morceau, Mixeur, Envoyer…) asks what to record: a melody on the keys, the pads, or the
 * microphone (a voice, a guitar, any instrument): it opened the keys when a guitar was meant, and the
 * microphone was only « Enregistrer ma voix » in ⋮. The count-in is chosen right there.
 */
@Composable
fun RecordChoiceDialog(keys: () -> Unit, pads: () -> Unit, mic: () -> Unit, dismiss: () -> Unit) {
    ScaledAlertDialog(
        onDismissRequest = dismiss,
        title = { Text(tr("Que veux-tu enregistrer ?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("record_choice")) {
                Choice(Icons.Filled.Mic, tr("Le micro : ta voix, une guitare, un instrument"), "record_choice_mic") { dismiss(); mic() }
                Choice(Icons.Filled.Piano, tr("Une mélodie au clavier"), "record_choice_keys") { dismiss(); keys() }
                Choice(Icons.Filled.GridView, tr("Les pads (la batterie)"), "record_choice_pads") { dismiss(); pads() }
                CountInChoice()
            }
        },
        confirmButton = { TextButton(onClick = dismiss, modifier = Modifier.testTag("record_choice_cancel")) { Text(tr("Annuler")) } },
    )
}

@Composable
private fun Choice(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(tag)) {
        Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = Neon.Red)
        WholeWordsText("  $label", textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
    }
}

/** « Décompte : 1 mesure »: the takes' count-in (Réglages › Son), changed in one touch beside « Enregistrer » (none, 1, 2 bars). */
@Composable
fun CountInChoice(modifier: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
    val bars = observedSettings().recordCountIn
    val label = countInLabel(bars)
    TextButton(
        onClick = { SettingsStore.update(context) { it.copy(recordCountIn = nextCountIn(it.recordCountIn)) } },
        contentPadding = if (compact) androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp) else androidx.compose.material3.ButtonDefaults.TextButtonContentPadding,
        modifier = modifier.heightIn(min = 48.dp).testTag("record_count_in").semantics(mergeDescendants = true) {
            role = Role.Button; contentDescription = label
        },
    ) {
        Icon(Icons.Filled.Timer, contentDescription = null, Modifier.size(18.dp), tint = Neon.Cyan)
        // Beside « Enregistrer », the bars alone (« 1 »); its full words for TalkBack and elsewhere.
        Text((if (compact) " " else "  ") + if (compact) "$bars" else label, style = MaterialTheme.typography.labelLarge, color = Neon.Cyan)
    }
}

/** « Décompte : aucun », « Décompte : 1 mesure », « Décompte : 2 mesures ». */
fun countInLabel(bars: Int): String = when (bars) {
    0 -> tr("Décompte : aucun")
    1 -> tr("Décompte : 1 mesure")
    else -> tr("Décompte : %d mesures", bars)
}

/** The next count-in a touch gives: none, 1, 2, then none again. */
fun nextCountIn(bars: Int): Int = (bars + 1) % 3

/**
 * What the top bar's ● says it records (TalkBack, the bench's seats): on Rythme the pads, on the
 * Clavier the keys, on the 303 its knobs, on the microphone's screen the microphone, elsewhere it asks.
 */
fun recordLabelFor(source: TakeSource?, mic: Boolean, recording: Boolean): String = when {
    recording && mic -> tr("Arrêter la prise et la garder")
    recording -> tr("Arrêter l'enregistrement")
    mic -> tr("Enregistrer le micro sur la piste audio")
    source == TakeSource.KEYS -> tr("Enregistrer ce que je joue au clavier")
    source == TakeSource.ACID -> tr("Enregistrer ce que je joue sur la 303")
    source == TakeSource.PADS -> tr("Enregistrer ce que je joue aux pads")
    else -> tr("Enregistrer : micro, clavier ou pads")
}
