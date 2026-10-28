@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pocketstudio.app.session

import com.pocketstudio.ui.pressScale
import com.pocketstudio.model.i18n.inSentence
import com.pocketstudio.ui.ScaledBottomSheet
import com.pocketstudio.ui.ScaledDropdownMenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.ChordTrack
import com.pocketstudio.model.Freeze
import com.pocketstudio.model.Progression
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.labelIn
import com.pocketstudio.model.chordPads
import com.pocketstudio.model.progressionDegree
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors

/** One scene of the song in « Accords du morceau »: its title and its chords, one a bar. */
class ChordPartUi(val scene: Int, val title: String, val chords: List<String>, val guessed: Boolean)

/**
 * « Accords du morceau » (phase 37): the key, the ready-made progressions (named in the key), each
 * scene of the song with its chords, the tracks that follow, and whether chords are being tried.
 */
class SongChordsUi(
    val key: String,
    val progressions: List<Pair<Progression, String>>,
    val chosen: Progression?,
    val parts: List<ChordPartUi>,
    /** The key's chords a bar can take (degree, name). */
    val choices: List<Pair<Int, String>>,
    /** (track id, name, follows the chords, frozen). */
    val tracks: List<FollowUi>,
    val trying: Boolean,
)

class FollowUi(val id: Int, val name: String, val follows: Boolean, val frozen: Boolean = false)

class SongChordsActions(
    val tryProgression: (Progression) -> Unit = {},
    val tryChord: (scene: Int, bar: Int, degree: Int) -> Unit = { _, _, _ -> },
    val setFollow: (id: Int, on: Boolean) -> Unit = { _, _ -> },
    val apply: () -> Unit = {},
    val cancel: () -> Unit = {},
)

/** The sheet's content from [p] (the project before any try) and the chords tried ([tried]: scene to chords). */
fun songChordsUi(p: Project, tried: RhythmViewModel.ChordTry?, english: Boolean): SongChordsUi {
    val base = tried?.follows?.entries?.fold(p) { q, (id, on) -> ChordTrack.withFollow(q, id, on) } ?: p
    val pads = base.harmony.chordPads(48)
    val plain = (0..6).filter { base.harmony.progressionDegree(it) == it }
    fun names(degrees: List<Int>) = degrees.map { ChordTrack.name(base, it, english) }
    val frozen = Freeze.frozenIds(base)
    return SongChordsUi(
        key = "${base.harmony.keyName(english)} ${tr(base.harmony.chordKey.scale.label).inSentence()}",
        progressions = Progression.entries.map { pr -> pr to "${pr.labelIn(base.harmony)}  ·  ${names(ChordTrack.degreesOf(base, pr)).joinToString(" ")}" },
        chosen = tried?.progression,
        parts = ChordTrack.songScenes(base).map { s ->
            val chords = tried?.chords?.get(s)?.takeIf { it.isNotEmpty() }?.let { c -> List(ChordTrack.bars(base, s)) { c[it % c.size] } } ?: ChordTrack.chordsOf(base, s)
            ChordPartUi(s, base.sceneTitle(s), names(chords), guessed = tried?.chords?.get(s) == null && ChordTrack.isGuessed(base, s))
        },
        choices = plain.map { it to pads[it].name(english) },
        tracks = base.instrumentTracks.map { t ->
            FollowUi(t.id, com.pocketstudio.model.DefaultNames.shown(t.name), ChordTrack.follows(base, t) && t.id !in frozen, t.id in frozen)
        } + listOfNotNull(
            // The 303, when it plays in the song: its steps move by the scale like the notes.
            FollowUi(ChordTrack.ACID, tr("Ligne acide (TB-303)"), ChordTrack.acidFollows(base)).takeIf { ChordTrack.acidPlays(base) },
        ),
        trying = tried != null,
    )
}

/** « Accords du morceau » over the screen, wired to [vm]; closing it without « Appliquer » gives the song back. */
@Composable
fun SongChordsRoute(vm: RhythmViewModel, close: () -> Unit) {
    val project by vm.project.collectAsState()
    val tried by vm.chordTry.collectAsState()
    val english = com.pocketstudio.app.settings.englishNotes()
    val ui = remember(project, tried, english) { songChordsUi(vm.chordBase, tried, english) }
    SongChordsSheet(
        ui,
        SongChordsActions(
            tryProgression = vm::tryProgression, tryChord = vm::tryChord, setFollow = vm::setFollowsChords,
            apply = { vm.applyChords(); close() }, cancel = { vm.cancelChords(); close() },
        ),
        close = { vm.cancelChords(); close() },
    )
}

@Composable
fun SongChordsSheet(ui: SongChordsUi, actions: SongChordsActions, close: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("song_chords_sheet")) {
        SongChordsContent(ui, actions)
    }
}

/** The sheet's body (drawn alone in the screenshots). */
@Composable
fun SongChordsContent(ui: SongChordsUi, actions: SongChordsActions) {
    val chips = pillColors()
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(tr("Accords du morceau"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(tr("Tonalité : %s. Les pistes qui suivent changent avec les accords, dans toutes les parties.", ui.key),
            style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        // The buttons first: on a phone they stay in view while the list below scrolls.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = actions.apply, enabled = ui.trying,
                colors = ButtonDefaults.buttonColors(containerColor = Neon.Magenta, contentColor = Neon.OnAccent),
                modifier = Modifier.pressScale(enabled = ui.trying).weight(1f).heightIn(min = 48.dp).testTag("song_chords_apply"),
            ) { Text(tr("Appliquer à tout le morceau"), maxLines = 2) }
            OutlinedButton(onClick = actions.cancel, modifier = Modifier.heightIn(min = 48.dp).testTag("song_chords_cancel")) {
                Text(if (ui.trying) tr("Annuler") else tr("Fermer"), maxLines = 1)
            }
        }
        Text(
            if (ui.trying) tr("Tu écoutes les nouveaux accords. « Appliquer » les garde (une étape à annuler), « Annuler » remet ceux d'avant.")
            else tr("Touche une progression : tu l'entends tout de suite, sur tout le morceau, avant de l'appliquer."),
            style = MaterialTheme.typography.bodySmall, color = if (ui.trying) Neon.Acid else Neon.TextSecondary,
            modifier = Modifier.testTag("song_chords_hint"),
        )
        Text(tr("Progression pour tout le morceau"), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ui.progressions.forEach { (pr, label) ->
                FilterChip(
                    selected = ui.chosen == pr, onClick = { actions.tryProgression(pr) }, colors = chips,
                    label = { Text(label, maxLines = 1) }, modifier = Modifier.testTag("song_progression_${pr.name}"),
                )
            }
        }
        Text(tr("Partie par partie (une case = une mesure)"), style = MaterialTheme.typography.titleSmall)
        ui.parts.forEach { part -> PartChords(part, ui.choices, actions) }
        Text(tr("Pistes qui suivent les accords"), style = MaterialTheme.typography.titleSmall)
        Text(tr("Les pistes de l'assistant suivent ; les notes que tu as écrites restent, sauf si tu le demandes ici."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ui.tracks.forEach { t ->
                FilterChip(
                    selected = t.follows, enabled = !t.frozen, onClick = { actions.setFollow(t.id, !t.follows) }, colors = chips,
                    label = { Text(if (t.frozen) tr("%s (gelée)", t.name) else t.name, maxLines = 1) },
                    modifier = Modifier.testTag("chord_follow_${t.id}"),
                )
            }
        }
    }
}

/** A scene's chords, one cell a bar; a touch on a cell picks another chord of the key for that bar. */
@Composable
private fun PartChords(part: ChordPartUi, choices: List<Pair<Int, String>>, actions: SongChordsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            part.title + if (part.guessed) "  ·  " + tr("trouvés dans tes notes") else "",
            style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            part.chords.forEachIndexed { bar, name ->
                var open by remember { mutableStateOf(false) }
                Box {
                    Box(
                        Modifier.widthIn(min = 56.dp).heightIn(min = 48.dp)
                            .background(Neon.SurfaceHigh, RoundedCornerShape(10.dp))
                            .border(1.dp, Neon.Magenta.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { open = true }
                            .padding(horizontal = 10.dp)
                            .testTag("chord_${part.scene}_$bar"),
                        contentAlignment = Alignment.Center,
                    ) { Text(name, style = MaterialTheme.typography.titleSmall, color = Neon.TextPrimary, maxLines = 1) }
                    ScaledDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        choices.forEach { (degree, chord) ->
                            DropdownMenuItem(
                                text = { Text(chord) }, onClick = { open = false; actions.tryChord(part.scene, bar, degree) },
                                modifier = Modifier.testTag("chord_pick_$degree"),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The song's chords under its clock (Morceau): each part's chords in a line; a touch opens
 * « Accords du morceau ».
 */
@Composable
internal fun SongChordsStrip(parts: List<Pair<String, List<String>>>, open: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().background(Neon.Magenta.copy(alpha = 0.08f), shape).border(1.dp, Neon.Magenta.copy(alpha = 0.4f), shape)
            .clickable(role = Role.Button, onClickLabel = tr("Accords du morceau"), onClick = open).padding(10.dp).testTag("song_chords"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(tr("Accords du morceau"), style = MaterialTheme.typography.titleSmall, color = Neon.Magenta, fontWeight = FontWeight.Bold)
        parts.take(6).forEach { (title, chords) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, Modifier.widthIn(max = 140.dp), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(chords.joinToString("  "), style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (parts.size > 6) Text(if (parts.size == 7) tr("… et 1 autre partie") else tr("… et %d autres parties", parts.size - 6), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        Text(tr("Touche pour changer les accords de tout le morceau."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

/** What [SongChordsStrip] shows: each scene of the song with its chords' names. */
fun songChordsStrip(p: Project, english: Boolean): List<Pair<String, List<String>>> =
    ChordTrack.songScenes(p).map { s -> p.sceneTitle(s) to ChordTrack.chordsOf(p, s).map { ChordTrack.name(p, it, english) } }
