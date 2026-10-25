package com.pocketstudio.app.learn

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Project
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * « Mon premier morceau » (phase 36, from phase 39's plan): six short steps from a style to a
 * shared song, each pointing at the real control that does it. A step is done when the project
 * shows it (a song made, a sound changed, notes recorded, the parts changed) or the music plays
 * or is exported: never by a touch on « OK ». Each one can be skipped, and the whole can stop.
 */
enum class Mission {
    Style, Listen, Sound, Melody, Arrange, Share;

    val title: String get() = when (this) {
        Style -> tr("Choisis un style")
        Listen -> tr("Écoute ton morceau")
        Sound -> tr("Change un son")
        Melody -> tr("Enregistre une mélodie")
        Arrange -> tr("Arrange les parties")
        Share -> tr("Partage-le")
    }

    /** What to do, in one or two short sentences ([simple]: the Mode simple's words and tabs). */
    fun text(simple: Boolean): String = when (this) {
        Style -> tr("Touche le dossier en haut à gauche, puis « Nouveau projet » et un style : l'appli écrit tout le morceau.")
        Listen -> tr("Touche ▶ en haut : ton morceau joue du début à la fin.")
        Sound -> if (simple) tr("Dans Mon morceau, touche une piste puis « Changer le son ».")
        else tr("Dans Structure, touche le nom d'une piste : choisis un autre son.")
        Melody -> if (simple) tr("Va dans Clavier, touche « ● Enregistrer ma mélodie » et joue quelques notes.")
        else tr("Deux touches sur un bloc de la mélodie dans Structure (ou Sons), puis « ● Enregistrer ma mélodie ».")
        Arrange -> if (simple) tr("Dans Mon morceau, appui long sur une partie : ajoute un Fill, un Break ou une Montée.")
        else tr("Appui long sur le drapeau d'une partie : ajoute un Fill, un Break ou une Montée.")
        Share -> if (simple) tr("Va dans Envoyer : vérifie, puis touche « Partager ».")
        else tr("Touche ⤴ en haut à droite : vérifie, puis touche « Partager ».")
    }

    /** The controls the step points at, the first one on the screen wins (« * »: any id starting so). */
    fun targets(simple: Boolean): List<String> = when (this) {
        Style -> listOf("top_projects")
        Listen -> listOf("top_play")
        Sound -> if (simple) listOf("simple_track_*", "tab_simple_Morceau") else listOf("timeline_name_inst:*", "track_sound_*", "tab_Structure")
        Melody -> if (simple) listOf("record_start", "tab_simple_Clavier") else listOf("record_start", "timeline_grid", "tab_Structure")
        Arrange -> if (simple) listOf("part_play_1", "part_play_0", "tab_simple_Morceau") else listOf("timeline_ruler", "part_play_1", "tab_Structure")
        Share -> if (simple) listOf("share_go", "tab_simple_Envoyer") else listOf("top_share")
    }

    /**
     * What the project looks like for this step: the step is done once it differs from what it
     * was when the step began (a sound, the notes, the parts). Null: decided otherwise (a song
     * made, the music playing, an export).
     */
    fun signature(p: Project): Int? = when (this) {
        Sound -> listOf(p.tracks.map { listOf(it.synth, it.plaits, it.engine, it.instrument, it.multi, it.sampled) }, p.acid.sound, p.kitVersion, p.drums.lanes.map { it.sample }).hashCode()
        Melody -> p.tracks.sumOf { t -> t.notes.notes.size } + p.allScenes.sumOf { s -> s.synthNotes.notes.size + s.trackNotes.values.sumOf { it.notes.size } }
        Arrange -> listOf(p.song, p.automation.size).hashCode()
        else -> null
    }
}

/** Where the missions are (SharedPreferences « missions »): on or not, the step, its starting point. */
class MissionPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var active by mutableStateOf(prefs.getBoolean(ACTIVE, false))
        private set
    var step by mutableIntStateOf(prefs.getInt(STEP, 0).coerceIn(0, Mission.entries.size))
        private set
    /** The step's starting point ([Mission.signature] when it began), null: not taken yet. */
    var start: Int? = if (prefs.contains(START)) prefs.getInt(START, 0) else null
        private set
    /** The steps skipped rather than done (said at the end). */
    val skipped: Set<Int> get() = prefs.getStringSet(SKIPPED, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    val current: Mission? get() = if (active) Mission.entries.getOrNull(step) else null
    val finished: Boolean get() = active && step >= Mission.entries.size

    fun begin() {
        active = true
        step = 0
        start = null
        prefs.edit().putBoolean(ACTIVE, true).putInt(STEP, 0).remove(START).remove(SKIPPED).apply()
    }

    fun markStart(signature: Int) {
        start = signature
        prefs.edit().putInt(START, signature).apply()
    }

    /** The step done (or [skip]ped): on to the next one. */
    fun next(skip: Boolean = false) {
        if (skip) prefs.edit().putStringSet(SKIPPED, (skipped + step).map { it.toString() }.toSet()).apply()
        step = (step + 1).coerceAtMost(Mission.entries.size)
        start = null
        prefs.edit().putInt(STEP, step).remove(START).apply()
    }

    fun stop() {
        active = false
        prefs.edit().putBoolean(ACTIVE, false).apply()
    }

    companion object {
        const val PREFS = "missions"
        private const val ACTIVE = "active"
        private const val STEP = "step"
        private const val START = "start"
        private const val SKIPPED = "skipped"
    }
}

/**
 * Whether [m] is done now, for the project [p] as it is and what the app does: [playing] the
 * song plays, [exporting] an export runs; [start] the step's starting point.
 */
fun missionDone(m: Mission, p: Project, playing: Boolean, exporting: Boolean, start: Int?): Boolean = when (m) {
    Mission.Style -> p.song.size >= 2
    Mission.Listen -> playing && p.song.isNotEmpty()
    Mission.Share -> exporting
    else -> start != null && m.signature(p) != start
}

/** The card of the step shown: « Mission 3/6 », what to do, « Passer » and « Arrêter ». */
@Composable
fun MissionCard(m: Mission?, step: Int, simple: Boolean, skip: () -> Unit, stop: () -> Unit, modifier: Modifier = Modifier, remix: (() -> Unit)? = null) {
    val total = Mission.entries.size
    Column(
        modifier.padding(horizontal = 12.dp, vertical = 88.dp).widthIn(max = 520.dp).fillMaxWidth()
            .background(Neon.Surface, RoundedCornerShape(16.dp)).border(2.dp, Neon.Amber, RoundedCornerShape(16.dp))
            .padding(14.dp).testTag("mission_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (m == null) {
            Text(tr("Bravo, ton premier morceau est fait !"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.Acid)
            Text(tr("Tu sais choisir un style, écouter, changer un son, enregistrer, arranger et partager."), style = MaterialTheme.typography.bodyMedium)
            // Lot 2: what comes next, the Remix (Benjamin wants it seen).
            if (remix != null) {
                Text(tr("Et maintenant ? Remixe une chanson que tu aimes : l'appli la sépare en batterie, basse, voix et le reste."), style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = stop, modifier = Modifier.heightIn(min = 48.dp).testTag("mission_done")) { Text(tr("Terminer")) }
                if (remix != null) {
                    androidx.compose.material3.OutlinedButton(onClick = remix, modifier = Modifier.heightIn(min = 48.dp).testTag("mission_remix")) { Text(tr("Remixer une chanson")) }
                }
            }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("Mission %1\$d/%2\$d · %3\$s", step + 1, total, m.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = Neon.Amber, modifier = Modifier.weight(1f).testTag("mission_title"),
            )
        }
        LinearProgressIndicator(progress = { step / total.toFloat() }, modifier = Modifier.fillMaxWidth(), color = Neon.Amber)
        Text(m.text(simple), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("mission_text"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = skip, modifier = Modifier.heightIn(min = 48.dp).testTag("mission_skip")) { Text(tr("Passer cette étape")) }
            TextButton(onClick = stop, modifier = Modifier.heightIn(min = 48.dp).testTag("mission_stop")) { Text(tr("Arrêter les missions"), color = Neon.TextSecondary) }
        }
    }
}

/**
 * Watches the open project while the missions are on: the step's starting point is taken when it
 * begins, and the step moves on by itself once done. Kept apart so a change of the project
 * recomposes this alone, not the app around it.
 */
@Composable
fun MissionWatcher(vm: com.pocketstudio.app.rhythm.RhythmViewModel, missions: MissionPrefs) {
    val current = missions.current ?: return
    val project by vm.project.collectAsStateWithLifecycle()
    val transport by vm.transport.collectAsStateWithLifecycle()
    val export by vm.export.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(current, project, transport.playing, export.running) {
        if (missions.start == null) current.signature(project)?.let(missions::markStart)
        if (missionDone(current, project, transport.playing, export.running, missions.start)) missions.next()
    }
}
