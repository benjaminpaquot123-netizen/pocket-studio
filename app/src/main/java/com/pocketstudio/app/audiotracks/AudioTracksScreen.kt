package com.pocketstudio.app.audiotracks

import com.pocketstudio.ui.named
import com.pocketstudio.ui.ScaledAlertDialog
import com.pocketstudio.ui.ScaledDropdownMenu

import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.audio.Listening
import com.pocketstudio.app.rhythm.AudioRecordingUi
import com.pocketstudio.app.rhythm.RecordTarget
import com.pocketstudio.app.rhythm.SampleInfo
import com.pocketstudio.app.rhythm.Waveform
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.TrackColors
import com.pocketstudio.ui.trackColor
import kotlin.math.roundToInt
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import com.pocketstudio.model.i18n.percent

class AudioTracksUi(
    val tracks: List<AudioTrack>,
    val recording: AudioRecordingUi? = null,
    val info: Map<String, SampleInfo> = emptyMap(),
    /** A clip edit or import in progress, or its error. */
    val task: String? = null,
    /** The round trip was measured (Outils › Latence): takes are placed exactly. */
    val latencyMeasured: Boolean = false,
    /** Sixteenths in a bar (time signature). */
    val stepsPerBar: Int = 16,
    /** What the music is heard on (advice: headphones, Bluetooth delay). */
    val listening: Listening = Listening.Wired,
    /** « Enregistrer sur »: the scene shown or the whole song. */
    val target: RecordTarget = RecordTarget(),
    /** The scene shown ("B · Couplet"). */
    val sceneTitle: String = "A",
    /** Each song part as shown ("A · Intro"); empty without a song. */
    val songParts: List<String> = emptyList(),
    /** Bars of the whole song (song clips start on one of them). */
    val songBars: Int = 0,
    val tempoBpm: Double = 120.0,
    /** The stopped take is still being written (Annuler waits). */
    val saving: Boolean = false,
    /** The scene shown (index). */
    val scene: Int = 0,
    /** Every scene as shown ("B · Couplet"), by index: where a scene clip plays. */
    val sceneTitles: List<String> = emptyList(),
    /** The song step each song part starts on (where a song clip sits). */
    val partStarts: List<Int> = emptyList(),
    /** « M'entendre dans le casque » is chosen (it works with wired headphones only). */
    val monitor: Boolean = false,
    /** The audio track heard alone in a loop (its take picker's « Écouter seule »), or null. */
    val aloneTrack: Int? = null,
)

class AudioTracksActions(
    /** Opens a track's quick effects. */
    val effects: (Int) -> Unit = {},
    /** Step 7e: « Garder dans ma bibliothèque » (a track's clip). */
    val keepInLibrary: (Int) -> Unit = {},
    val add: () -> Unit = {},
    val remove: (Int) -> Unit = {},
    val rename: (Int, String) -> Unit = { _, _ -> },
    val setColor: (Int, Int) -> Unit = { _, _ -> },
    val record: (Int) -> Unit = {},
    val stop: () -> Unit = {},
    val cancel: () -> Unit = {},
    val import: (Int) -> Unit = {},
    val setStart: (Int, Int) -> Unit = { _, _ -> },
    val setLoopBars: (Int, Int) -> Unit = { _, _ -> },
    val setGain: (Int, Float) -> Unit = { _, _ -> },
    val setFadeIn: (Int, Float) -> Unit = { _, _ -> },
    val setFadeOut: (Int, Float) -> Unit = { _, _ -> },
    val normalize: (Int) -> Unit = {},
    val reverse: (Int) -> Unit = {},
    val trim: (Int, Float, Float) -> Unit = { _, _, _ -> },
    val clear: (Int) -> Unit = {},
    val setTarget: (RecordTarget) -> Unit = {},
    /** Plays once per part (true) or loops (false). */
    val setOnce: (Int, Boolean) -> Unit = { _, _ -> },
    /** Its start is a bar of the song (true) or of the scene (false). */
    val setSong: (Int, Boolean) -> Unit = { _, _ -> },
    /** « Décaler la prise », in ms. */
    val setOffset: (Int, Float) -> Unit = { _, _ -> },
    /** A scene clip plays in the parts of this scene only (null: every part). */
    val setScene: (Int, Int?) -> Unit = { _, _ -> },
    /** « ▶ Depuis ma prise »: the song from the part the clip starts in. */
    val playFrom: (Int) -> Unit = {},
    /** « Prise précédente »: the take the last one replaced comes back. */
    val restorePrevious: (Int) -> Unit = {},
    /** The take picker: take (second) of the track's other takes, newest first, plays. */
    val chooseTake: (Int, Int) -> Unit = { _, _ -> },
    /** « Prises »: an other take of a track left out (undoable). */
    val forgetTake: (Int, Int) -> Unit = { _, _ -> },
    /** « Recaler sur le tempo »: a take recorded at another tempo stretched to the song's. */
    val realign: (Int) -> Unit = {},
    /** « Refaire mes. A-B »: track, first bar, last bar (of the song for a song clip, else of the part). */
    val punch: (Int, Int, Int) -> Unit = { _, _, _ -> },
    /** « M'entendre dans le casque » on or off. */
    val setMonitor: (Boolean) -> Unit = {},
    /**
     * Audit ultra round 3 (n° 25, seat C a: « Take 1 » touched, nothing heard): a take touched (track, its index in
     * [AudioTrack.otherTakes], null for the one playing) plays on its track, and the track is heard alone to compare.
     */
    val hearTake: (Int, Int?) -> Unit = { _, _ -> },
    /** « ▶ Écouter seule » of a track's takes: the track alone in a loop, or everything again. */
    val listenAlone: (Int) -> Unit = {},
    /** Keeps the track heard alone going while this screen shows (see [com.pocketstudio.app.rhythm.HoldAlone]). */
    val holdAlone: () -> (() -> Unit) = { {} },
)

private val LOOP_BARS = listOf(1, 2, 4, 8, 16)

/** Audio tracks: record your voice or an instrument over the song, or bring a sound file. */
@Composable
fun AudioTracksScreen(ui: AudioTracksUi, actions: AudioTracksActions, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("audio_tracks"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        com.pocketstudio.app.rhythm.HoldAlone(ui.aloneTrack != null, actions.holdAlone)
        // Audit ultra round 3 (n° 24, seat E c): the take's state is in the card of the track that records (below);
        // here only for a track not (yet) in the list.
        ui.recording?.let { rec -> if (ui.tracks.none { it.id == rec.track }) RecordingCard(rec, null, ui, actions) }
        ui.task?.let { Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodySmall) }
        if (ui.tracks.isEmpty()) {
            NeonCard {
                SectionLabel(tr("Pistes audio"), help = Glossary.AUDIO_TRACK)
                Text(
                    tr("Enregistre ta voix, une guitare, des claquements de mains… par-dessus ta musique, ou ajoute un fichier son.") + " " +
                        tr("Une prise sur une partie joue une fois à chaque passage de cette partie ; une prise sur tout le morceau joue d'un bout à l'autre."),
                    style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
                )
                ListeningAdvice(ui.listening)
            }
        } else if (ui.recording == null) {
            TargetCard(ui, actions)
        }
        // The tracks, then the button for one more: in two columns on a tablet.
        val cards = ui.tracks.map { t -> @Composable { TrackCard(t, ui, actions) } } +
            listOfNotNull(
                if (ui.tracks.size < AudioTrack.MAX) {
                    @Composable {
                        Button(onClick = actions.add, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("audio_add")) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("  " + tr("Piste audio"))
                        }
                    }
                } else null,
            )
        com.pocketstudio.ui.AdaptiveColumns(cards, minCellWidth = 360.dp, spacing = 10.dp, columnSpacing = 10.dp, masonry = true)
        if (!ui.latencyMeasured && ui.tracks.isNotEmpty()) {
            Text(
                tr("Astuce : mesure la latence une fois (Plus › Latence) pour que tes prises tombent pile sur le temps."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
    }
}

/** « Mets des écouteurs » on the speaker; a short delay warning over Bluetooth. */
@Composable
private fun ListeningAdvice(listening: Listening) {
    when (listening) {
        Listening.Speaker -> Text(
            tr("Mets des écouteurs (filaires de préférence) : sinon la musique s'enregistre avec ta voix."),
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("audio_headphones"),
        )
        Listening.Bluetooth -> Text(
            tr("Écouteurs Bluetooth : ils ajoutent du retard. Des écouteurs filaires sont plus précis ; sinon, recale la prise avec « Décaler la prise »."),
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("audio_bluetooth"),
        )
        Listening.Wired -> Unit
    }
}

/**
 * « M'entendre dans le casque »: with wired headphones, the voice comes back in them during the
 * take (with the track's effects). Never on the speaker (it would howl); in Bluetooth, too late.
 */
@Composable
private fun MonitorChoice(ui: AudioTracksUi, actions: AudioTracksActions) {
    when (ui.listening) {
        Listening.Wired -> FilterChip(
            selected = ui.monitor, onClick = { actions.setMonitor(!ui.monitor) },
            label = { Text(tr("M'entendre dans le casque")) }, colors = pillColors(), modifier = Modifier.testTag("audio_monitor"),
        )
        Listening.Bluetooth -> if (ui.monitor) Text(
            tr("En Bluetooth, ta voix reviendrait trop tard dans le casque : elle n'y revient qu'avec des écouteurs filaires."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        Listening.Speaker -> Unit
    }
}

/** « Enregistrer sur »: the scene shown, or the whole song (from a part). */
@Composable
private fun TargetCard(ui: AudioTracksUi, actions: AudioTracksActions) {
    val chip = pillColors()
    val song = ui.target.song && ui.songParts.isNotEmpty()
    NeonCard(modifier = Modifier.testTag("audio_target")) {
        Text(tr("Enregistrer sur"), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
            FilterChip(
                selected = !song, onClick = { actions.setTarget(ui.target.copy(song = false)) },
                label = { Text(tr("La partie %s", ui.sceneTitle)) }, colors = chip, modifier = Modifier.testTag("audio_target_scene"),
            )
            FilterChip(
                selected = song, enabled = ui.songParts.isNotEmpty(), onClick = { actions.setTarget(ui.target.copy(song = true)) },
                label = { Text(tr("Tout le morceau")) }, colors = chip, modifier = Modifier.testTag("audio_target_song"),
            )
        }
        if (ui.songParts.isEmpty()) {
            Text(tr("Pour enregistrer sur tout le morceau, crée d'abord un morceau (Structure)."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        } else if (song) {
            Text(tr("À partir de la partie"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
                ui.songParts.forEachIndexed { i, title ->
                    FilterChip(selected = ui.target.fromPart == i, onClick = { actions.setTarget(ui.target.copy(fromPart = i)) }, label = { Text("${i + 1}. $title") }, colors = chip)
                }
            }
            Text(tr("Le morceau démarre après le décompte ; ta prise se pose à sa place dans le morceau et joue une seule fois."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        } else {
            Text(tr("La partie %s tourne en boucle ; ta prise joue une fois à chaque passage de cette partie.", ui.sceneTitle), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        ListeningAdvice(ui.listening)
        MonitorChoice(ui, actions)
    }
}

@Composable
private fun RecordingCard(rec: AudioRecordingUi, track: AudioTrack?, ui: AudioTracksUi, actions: AudioTracksActions) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    // n° 24 (seat E c): brought into sight as the take starts (the second track's take was off screen, at the page's top).
    val inSight = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    androidx.compose.runtime.LaunchedEffect(rec.track) {
        androidx.compose.runtime.withFrameNanos { }  // placed first (the cards are laid out by a subcomposition)
        inSight.bringIntoView()
    }
    NeonCard(accent = Neon.Red, modifier = Modifier.testTag("audio_recording").bringIntoViewRequester(inSight)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = Neon.Red)
            Text(
                when {
                    !rec.waiting -> tr("Enregistrement : %.1f s", rec.seconds)
                    rec.beatsLeft > 0 -> tr("Décompte : %d… la prise commence sur le premier temps", rec.beatsLeft)
                    else -> tr("Décompte… la prise commence sur le premier temps")
                },
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
            )
        }
        val where = if (rec.song) tr("tout le morceau") else tr("la partie %s", ui.sceneTitle)
        Text(listOfNotNull(track?.name?.let { com.pocketstudio.model.DefaultNames.shown(it) }, tr("sur %s", where)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        // n° 20 (round 2, seat B b: a thin green line, no word, nothing for TalkBack): named, thick, with a word.
        com.pocketstudio.app.record.InputLevel(rec.level)
        ListeningAdvice(ui.listening)
        MonitorChoice(ui, actions)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.stop, colors = ButtonDefaults.buttonColors(containerColor = Neon.Red), modifier = Modifier.weight(1f).testTag("audio_stop")) {
                Text(tr("Arrêter et garder"))
            }
            // A long take is not thrown away by a slip of the finger.
            OutlinedButton(
                onClick = { if (rec.seconds > CONFIRM_CANCEL_SECONDS) confirm = true else actions.cancel() },
                modifier = Modifier.weight(1f).testTag("audio_cancel"),
            ) { Text(tr("Annuler")) }
        }
    }
    if (confirm) {
        ScaledAlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(tr("Jeter cette prise ?")) },
            text = { Text(tr("Elle dure %d s et ne sera pas gardée.", rec.seconds.roundToInt())) },
            confirmButton = { TextButton(onClick = { confirm = false; actions.cancel() }, modifier = Modifier.testTag("audio_cancel_confirm")) { Text(tr("Jeter la prise")) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(tr("Continuer la prise")) } },
        )
    }
}

/** « Annuler » asks first beyond this. */
private const val CONFIRM_CANCEL_SECONDS = 10f

@Composable
private fun TrackCard(t: AudioTrack, ui: AudioTracksUi, actions: AudioTracksActions) {
    val color = trackColor(t.color)
    // n° 20 (round 2): « Audio 1 » in the interface's language, wherever the track is named.
    val shownName = com.pocketstudio.model.DefaultNames.shown(t.name)
    // Kept when a tablet turns (the activity is recreated).
    var menu by rememberSaveable(t.id) { mutableStateOf(false) }
    var renaming by rememberSaveable(t.id) { mutableStateOf(false) }
    var colouring by rememberSaveable(t.id) { mutableStateOf(false) }
    NeonCard(modifier = Modifier.testTag("audio_track_${t.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            Text(shownName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = tr("Options de %s", shownName)) }
                ScaledDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(tr("Renommer")) }, onClick = { menu = false; renaming = true })
                    DropdownMenuItem(text = { Text(tr("Couleur")) }, onClick = { menu = false; colouring = true })
                    if (t.clip != null) DropdownMenuItem(text = { Text(tr("Vider la piste")) }, onClick = { menu = false; actions.clear(t.id) })
                    DropdownMenuItem(text = { Text(tr("Supprimer la piste (Annuler possible)")) }, onClick = { menu = false; actions.remove(t.id) })
                }
            }
        }
        val busy = ui.recording != null || ui.saving
        // n° 24 (seat E c): count-in, level and « Arrêter et garder » right in the card of the track that records.
        ui.recording?.takeIf { it.track == t.id }?.let { RecordingCard(it, t, ui, actions) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { actions.record(t.id) }, enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Neon.Red),
                modifier = Modifier.weight(1f).testTag("audio_record_${t.id}"),
            ) {
                Icon(Icons.Filled.FiberManualRecord, contentDescription = null, Modifier.size(16.dp))
                com.pocketstudio.ui.WholeWordsText(" " + if (t.clip == null) tr("Enregistrer") else tr("Nouvelle prise"))
            }
            OutlinedButton(onClick = { actions.import(t.id) }, enabled = !busy, modifier = Modifier.weight(1f)) { com.pocketstudio.ui.WholeWordsText(tr("Fichier son"), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fadingHorizontalScroll()) {
            // Audit ultra round 1 (n° 31, seat E b): the count-in beside « Enregistrer » (it was at the bottom of Réglages).
            com.pocketstudio.app.record.CountInChoice()
            TextButton(onClick = { actions.effects(t.id) }, modifier = Modifier.testTag("audio_effects_${t.id}")) { Text(tr("Effets de cette piste")) }
            // Audit ultra round 3 (n° 25, seat E c): no « Prise précédente » any more: it swapped two takes back and forth
            // (« précédente » then the next one); the take picker below numbers them all, one touch each.
        }
        if (t.clip != null && t.otherTakes.isNotEmpty()) TakePicker(t, busy, ui.aloneTrack == t.id, actions)
        val clip = t.clip ?: return@NeonCard
        ClipEditor(t.id, clip, ui.info[clip.file], color, actions, ui, t.takesInOrder().firstOrNull { it.first.file == clip.file }?.second ?: 1)
    }
    if (renaming) {
        var name by rememberSaveable(t.id) { mutableStateOf(shownName) }
        ScaledAlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(tr("Renommer la piste")) },
            // n° 26 (seat E b: « Audio 1Chitarra »): the name selected, so what is typed replaces it; its limit said.
            text = { com.pocketstudio.app.ui.NameField(name, { name = it }, 24, tr("Nom"), modifier = Modifier.testTag("audio_name_field")) },
            confirmButton = { TextButton(onClick = { actions.rename(t.id, name); renaming = false }, enabled = name.isNotBlank()) { Text("OK") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(tr("Annuler")) } },
        )
    }
    if (colouring) {
        ScaledAlertDialog(
            onDismissRequest = { colouring = false },
            title = { Text(tr("Couleur de « %s »", shownName)) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrackColors.forEachIndexed { i, c ->
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(c)
                                .border(BorderStroke(if (i == t.color) 3.dp else 0.dp, Neon.TextPrimary), CircleShape)
                                .clickable { actions.setColor(t.id, i); colouring = false },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { colouring = false }) { Text(tr("Fermer")) } },
        )
    }
}

/**
 * « Prises » : every take the track keeps, numbered (« Prise 1 », « Prise 2 »…) in a fixed order,
 * the one playing lit; a touch plays that one instead (one undo step) and nothing moves (n° 18,
 * round 2, seat B b: two « 테이크 03:37 » swapped places at each touch). A take not playing can be
 * deleted (Annuler brings it back).
 */
@Composable
private fun TakePicker(t: AudioTrack, busy: Boolean, alone: Boolean, actions: AudioTracksActions) {
    val chip = pillColors()
    val takes = t.takesInOrder()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tr("Prises (%d)", takes.size), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        // n° 25 (seat C a): the track alone in a loop: each take touched is heard there, one after the other.
        com.pocketstudio.app.rhythm.AloneButton(alone, tr("Écouter seule"), com.pocketstudio.model.DefaultNames.shown(t.name), "audio_alone_${t.id}", compact = true, toggle = { if (!busy || alone) actions.listenAlone(t.id) })
    }
    // On several lines rather than scrolled: every take in sight, the one playing (often the last) too.
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().testTag("audio_takes_${t.id}"),
    ) {
        takes.forEach { (take, number) ->
            val playing = take.file == t.clip?.file
            // Its place among the takes not playing (what « choose » and « delete » are given).
            val other = t.otherTakes.indexOfFirst { it.file == take.file }
            // n° 25 (seat E c: « Take 2 · 8 s » here, « Take 2 · 8.5 s » under it): the same length as the editor's.
            val label = takeLabel(take, number, decimals = 1)
            // A take and its ✕ together: a line break never parts them.
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = playing, enabled = playing || !busy, onClick = { if (!busy) actions.hearTake(t.id, other.takeIf { !playing && it >= 0 }) },
                    label = { Text(label) }, colors = chip, modifier = Modifier.testTag("audio_take_${t.id}_$number"),
                )
                if (!playing && other >= 0) {
                    // Owner (2026-11-04): an unwanted take can go (Annuler brings it back).
                    IconButton(onClick = { actions.forgetTake(t.id, other) }, enabled = !busy, modifier = Modifier.testTag("audio_take_forget_${t.id}_$number")) {
                        Icon(Icons.Filled.Close, contentDescription = tr("Supprimer %s", label), tint = Neon.TextSecondary)
                    }
                }
            }
        }
    }
}

/** [seconds] in the interface's language (« 7 s », « 7초 »), with [decimals] decimals. */
private fun secondsText(seconds: Number, decimals: Int): String =
    tr("%s s", String.format(I18n.lang.locale, "%.${decimals}f", seconds.toDouble()))

/**
 * A take as the picker names it: « Prise 2 » (its [number], [AudioTrack.takesInOrder]) and its length;
 * a sound brought from a file keeps its own name.
 */
private fun takeLabel(c: AudioClip, number: Int, decimals: Int = 0): String {
    val name = if (c.take > 0 || com.pocketstudio.model.DefaultNames.isDefault(c.name)) tr("Prise %d", number)
    else com.pocketstudio.model.DefaultNames.shownSample(c.name)
    return "$name · ${secondsText(c.seconds, decimals)}"
}

/**
 * Where each bar line falls in [clip]'s sound, as (fraction of the sound, bar number from 1): of
 * the song for a song clip, of the part otherwise, at [tempoBpm]. 64 at most.
 */
fun clipBarMarks(clip: AudioClip, tempoBpm: Double, stepsPerBar: Int): List<Pair<Float, Int>> {
    val steps = clip.seconds * tempoBpm / 15.0
    if (steps <= 0.0 || stepsPerBar <= 0) return emptyList()
    val first = clip.start + clip.offsetMs / 1000.0 * tempoBpm / 15.0
    val out = ArrayList<Pair<Float, Int>>()
    var bar = kotlin.math.ceil(first / stepsPerBar).toInt().coerceAtLeast(0)
    while (out.size < 64) {
        val at = (bar * stepsPerBar - first) / steps
        if (at > 1.0) break
        if (at >= 0.0) out += at.toFloat() to bar + 1
        bar++
    }
    return out
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipEditor(id: Int, clip: AudioClip, info: SampleInfo?, color: androidx.compose.ui.graphics.Color, actions: AudioTracksActions, ui: AudioTracksUi, number: Int) {
    val per = ui.stepsPerBar
    val chip = pillColors()
    // The cut's handles, kept when a tablet turns.
    var rangeStart by rememberSaveable(clip.file) { mutableStateOf(0f) }
    var rangeEnd by rememberSaveable(clip.file) { mutableStateOf(1f) }
    val range = rangeStart..rangeEnd
    Text(takeLabel(clip, number, decimals = 1), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    if (clip.tempoDiffers(ui.tempoBpm)) {
        Text(
            tr("Enregistrée à %1\$d BPM, le morceau est à %2\$d BPM : elle ne tombe plus pile sur le temps.", clip.bpm.roundToInt(), ui.tempoBpm.roundToInt()),
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("audio_tempo_$id"),
        )
        // Stretched to the song's tempo, its pitch kept (Annuler brings it back as it was).
        OutlinedButton(onClick = { actions.realign(id) }, enabled = ui.recording == null && !ui.saving, modifier = Modifier.testTag("audio_realign_$id")) {
            Text(tr("Recaler sur le tempo (%d BPM)", ui.tempoBpm.roundToInt()))
        }
    }
    // The bar lines over the sound: where each bar of the song (or the part) falls in the take.
    info?.let { Waveform(it.peaks, color, region = range.start to range.endInclusive, bars = clipBarMarks(clip, ui.tempoBpm, per)) }
    // Cut: keep the part between the two handles.
    RangeSlider(value = range, onValueChange = { rangeStart = it.start; rangeEnd = it.endInclusive }, modifier = Modifier.testTag("audio_trim_$id").named(tr("Garder la sélection")))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
        OutlinedButton(onClick = { actions.trim(id, range.start, range.endInclusive) }, enabled = range.start > 0.001f || range.endInclusive < 0.999f) {
            Text(tr("Garder la sélection"))
        }
        OutlinedButton(onClick = { actions.normalize(id) }, modifier = Modifier.testTag("audio_normalize_$id")) { Text(tr("Normaliser")) }
        OutlinedButton(onClick = { actions.reverse(id) }) { Text(tr("Inverser")) }
    }
    // Step 7e: the take for every project (the project does not change).
    OutlinedButton(onClick = { actions.keepInLibrary(id) }, modifier = Modifier.fillMaxWidth().testTag("audio_keep_library_$id")) {
        Icon(Icons.Filled.BookmarkAdd, contentDescription = null)
        Text(" " + tr("Garder dans ma bibliothèque"))
    }
    // Where it plays: in the parts of its scene, in every part, or once along the whole song.
    val ownScene = clip.scene ?: ui.scene
    if (ui.songParts.isNotEmpty() || clip.song) {
        Text(tr("Où elle joue"), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
            FilterChip(selected = clip.song, onClick = { actions.setSong(id, true) }, label = { Text(tr("Tout le morceau")) }, colors = chip, modifier = Modifier.testTag("audio_song_$id"))
            FilterChip(
                selected = !clip.song && clip.scene != null, onClick = { actions.setScene(id, ownScene) },
                label = { Text(tr("La partie %s", ui.sceneTitles.getOrElse(ownScene) { "?" })) }, colors = chip, modifier = Modifier.testTag("audio_scene_$id"),
            )
            FilterChip(selected = !clip.song && clip.scene == null, onClick = { actions.setScene(id, null) }, label = { Text(tr("Toutes les parties")) }, colors = chip,
                modifier = Modifier.testTag("audio_every_$id"))
        }
    }
    if (clip.song) {
        val bar = clip.startBar(per)
        val last = maxOf(ui.songBars, bar)
        // Where it sits: the part it starts in and the bar inside it (M5).
        val part = ui.partStarts.indexOfLast { it <= clip.start }
        if (part >= 0) {
            val inPart = (clip.start - ui.partStarts[part]) / per + 1
            Text(
                tr("Ta prise commence dans la partie %1\$s, mesure %2\$d (mesure %3\$d du morceau).", "${part + 1}. ${ui.songParts.getOrElse(part) { "?" }}", inPart, bar),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("audio_where_$id"),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Commence à la mesure du morceau"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { actions.setStart(id, bar - 1) }, enabled = bar > 1) { Text("−") }
            Text("$bar", style = MonoStyle, color = Neon.Cyan, modifier = Modifier.testTag("audio_song_bar_$id"))
            OutlinedButton(onClick = { actions.setStart(id, bar + 1) }, enabled = bar < last) { Text("+") }
        }
        if (ui.partStarts.isNotEmpty()) {
            Text(tr("Mettre au début de"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
                ui.partStarts.forEachIndexed { i, step ->
                    FilterChip(selected = clip.start == step, onClick = { actions.setStart(id, step / per + 1) },
                        label = { Text("${i + 1}. ${ui.songParts.getOrElse(i) { "?" }}") }, colors = chip, modifier = Modifier.testTag("audio_part_${id}_$i"))
                }
            }
        }
    } else {
        Text(tr("Commence à la mesure"), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
            (1..8).forEach { bar ->
                FilterChip(selected = clip.start == (bar - 1) * per, onClick = { actions.setStart(id, bar) }, label = { Text("$bar") }, colors = chip)
            }
        }
        Text(tr("Elle rejoue"), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
            FilterChip(selected = clip.once, onClick = { actions.setOnce(id, true) }, label = { Text(tr("une fois par partie")) }, colors = chip, modifier = Modifier.testTag("audio_once_$id"))
            LOOP_BARS.forEach { bars ->
                FilterChip(selected = !clip.once && clip.loopSteps == bars * per, onClick = { actions.setLoopBars(id, bars) },
                    label = { Text(trCount(bars, "à chaque mesure", "toutes les %d mesures")) }, colors = chip)
            }
        }
    }
    if (ui.songParts.isNotEmpty() && ui.recording == null) {
        OutlinedButton(onClick = { actions.playFrom(id) }, modifier = Modifier.testTag("audio_play_from_$id")) { Text(tr("▶ Depuis ma prise")) }
    }
    PunchRow(id, clip, ui, actions)
    // Up to ×4 (+12 dB) for a quiet voice; 100 % stays in the middle (03 G2).
    LabeledSlider(tr("Volume"), percent((clip.gain * 100).roundToInt()), kotlin.math.sqrt(clip.gain / AudioClip.MAX_GAIN)) {
        actions.setGain(id, AudioClip.MAX_GAIN * it * it)
    }
    LabeledSlider(tr("Fondu au début"), secondsText(clip.fadeIn, 2), clip.fadeIn / 2f) { actions.setFadeIn(id, it * 2f) }
    LabeledSlider(tr("Fondu à la fin"), secondsText(clip.fadeOut, 2), clip.fadeOut / 2f) { actions.setFadeOut(id, it * 2f) }
    // « Décaler la prise »: a fine nudge (5 ms) or a big one (50 ms, a Bluetooth delay) when
    // the voice is early or late; a touch on the value puts it back to 0.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Décaler la prise"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            if (clip.offsetMs == 0f) "0 ms" else String.format(I18n.lang.locale, "%+d ms", clip.offsetMs.roundToInt()),
            style = MonoStyle, color = Neon.Cyan, modifier = Modifier.clickable { actions.setOffset(id, 0f) }.testTag("audio_offset_$id"),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val early = clip.offsetMs > -AudioClip.MAX_OFFSET_MS
        val late = clip.offsetMs < AudioClip.MAX_OFFSET_MS
        OutlinedButton(onClick = { actions.setOffset(id, clip.offsetMs - AudioClip.OFFSET_BIG_STEP_MS) }, enabled = early,
            modifier = Modifier.weight(1f).testTag("audio_much_earlier_$id")) { Text("−50") }
        OutlinedButton(onClick = { actions.setOffset(id, clip.offsetMs - AudioClip.OFFSET_STEP_MS) }, enabled = early,
            modifier = Modifier.weight(1f).testTag("audio_earlier_$id")) { Text("−5") }
        OutlinedButton(onClick = { actions.setOffset(id, clip.offsetMs + AudioClip.OFFSET_STEP_MS) }, enabled = late,
            modifier = Modifier.weight(1f).testTag("audio_later_$id")) { Text("+5") }
        OutlinedButton(onClick = { actions.setOffset(id, clip.offsetMs + AudioClip.OFFSET_BIG_STEP_MS) }, enabled = late,
            modifier = Modifier.weight(1f).testTag("audio_much_later_$id")) { Text("+50") }
    }
    Text(tr("« − » : plus tôt, « + » : plus tard (en millisecondes)."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
}

/**
 * « Refaire un passage »: the bars to sing again (of the song for a song clip, of the part
 * otherwise), then « ● Refaire mes. 5-8 »: the music starts from the part holding the bar before (or the part's start), the take stops by itself.
 */
@Composable
private fun PunchRow(id: Int, clip: AudioClip, ui: AudioTracksUi, actions: AudioTracksActions) {
    val per = ui.stepsPerBar
    val firstBar = clip.startBar(per)
    val lastBar = (firstBar + kotlin.math.ceil(clip.steps(ui.tempoBpm) / per.toDouble()).toInt() - 1).coerceAtLeast(firstBar)
    var from by rememberSaveable(clip.file) { mutableStateOf(firstBar) }
    var to by rememberSaveable(clip.file) { mutableStateOf(minOf(firstBar + 3, lastBar)) }
    val busy = ui.recording != null || ui.saving
    Text(tr("Refaire un passage"), style = MaterialTheme.typography.bodyMedium)
    // One line per end, so the numbers keep their room at any text size.
    BarStepper(tr("de la mesure"), from, down = { from-- }, up = { from++; if (to < from) to = from }, canDown = from > firstBar, canUp = from < lastBar, tag = "audio_punch_from_$id")
    BarStepper(tr("à la mesure"), to, down = { to-- }, up = { to++ }, canDown = to > from, canUp = to < lastBar, tag = "audio_punch_to_$id")
    val canPunch = !busy && !clip.tempoDiffers(ui.tempoBpm)
    Button(
        onClick = { actions.punch(id, from, to) }, enabled = canPunch,
        colors = ButtonDefaults.buttonColors(containerColor = Neon.Red),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("audio_punch_$id"),
    ) {
        Icon(Icons.Filled.FiberManualRecord, contentDescription = null, Modifier.size(16.dp))
        Text(" " + if (from == to) tr("Refaire la mes. %d", from) else tr("Refaire mes. %1\$d-%2\$d", from, to))
    }
    Text(
        if (clip.tempoDiffers(ui.tempoBpm)) tr("Recale d'abord la prise sur le tempo pour refaire un passage.")
        else tr("La musique repart du début de la partie ; seul ce passage est remplacé, la prise entière reste dans les prises."),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
}

/** « de la mesure  − 17 + »: a bar chosen one up or down. */
@Composable
private fun BarStepper(label: String, value: Int, down: () -> Unit, up: () -> Unit, canDown: Boolean, canUp: Boolean, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = down, enabled = canDown, modifier = Modifier.testTag("${tag}_less")) { Text("−") }
        Text("$value", style = MonoStyle, color = Neon.Cyan, modifier = Modifier.widthIn(min = 32.dp).testTag(tag), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedButton(onClick = up, enabled = canUp, modifier = Modifier.testTag("${tag}_more")) { Text("+") }
    }
}

@Composable
private fun LabeledSlider(label: String, value: String, position: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(value = position.coerceIn(0f, 1f), onValueChange = onChange, label = label, valueText = value)
}
