package com.pocketstudio.app.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.ui.Neon

/** The projects whose « Ton morceau est prêt » was seen (SharedPreferences "song_ready"): never shown again for them. */
class SongReadyPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun seen(projectId: String): Boolean = prefs.getBoolean(projectId, false)

    fun markSeen(projectId: String) = prefs.edit().putBoolean(projectId, true).apply()

    companion object {
        const val PREFS = "song_ready"
    }
}

/** What « Ton morceau est prêt » says: the song's name, how long it lasts, its parts; [playing]: ▶ became ■. */
data class SongReadyUi(val name: String, val seconds: Int, val parts: Int, val playing: Boolean = false)

class SongReadyActions(
    /** ▶ the whole song (or stop it): the card stays, for the next choice. */
    val listen: () -> Unit = {},
    /** « Changer un son »: the main track's sounds (Synthé). */
    val changeSound: () -> Unit = {},
    /** « Modifier le morceau »: the song's parts on the timeline (named after the Morceau tab, until phase 36 renames it). */
    val structure: () -> Unit = {},
    /** « Partager »: « Prêt à partager ? ». */
    val share: () -> Unit = {},
    /** « Lire le guide de ton morceau » (null: no guide for this song). */
    val guide: (() -> Unit)? = null,
    /** « Plus tard », the cross, or Back. */
    val close: () -> Unit = {},
)

/**
 * « Ton morceau est prêt », once, after the assistant or « Partir d'un style » made a song for
 * project [projectId] ([pending]: just made). Any choice but ▶ closes it for good (so does
 * « Plus tard »): it never shows again for that project, even if the screen is rebuilt.
 */
@Composable
fun SongReady(pending: Boolean, projectId: String, ui: SongReadyUi, actions: SongReadyActions) {
    val context = LocalContext.current
    val prefs = remember { SongReadyPrefs(context) }
    var seen by remember(projectId) { mutableStateOf(prefs.seen(projectId)) }
    if (!pending || seen) return
    fun then(action: () -> Unit): () -> Unit = {
        prefs.markSeen(projectId)
        seen = true
        action()
    }
    val done = SongReadyActions(
        listen = actions.listen,
        changeSound = then(actions.changeSound),
        structure = then(actions.structure),
        share = then(actions.share),
        guide = actions.guide?.let { then(it) },
        close = then(actions.close),
    )
    Dialog(onDismissRequest = done.close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            SongReadyCard(ui, done, Modifier.widthIn(max = 560.dp))
        }
    }
}

/** The card itself: a friendly title, the song in one line, four big choices, the guide and « Plus tard ». */
@Composable
fun SongReadyCard(ui: SongReadyUi, actions: SongReadyActions, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(Neon.Surface, Color.White, 0.04f), Neon.Surface)))
            .border(1.dp, Neon.Cyan.copy(alpha = 0.5f), shape)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
            .testTag("song_ready"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Neon.Cyan.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Celebration, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(tr("Ton morceau est prêt !"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    listOf(ui.name, clock(ui.seconds), if (ui.parts == 1) tr("1 partie") else tr("%d parties", ui.parts)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("song_ready_summary"),
                )
            }
            IconButton(onClick = actions.close, modifier = Modifier.testTag("song_ready_close")) {
                Icon(Icons.Filled.Close, contentDescription = tr("Fermer"), tint = Neon.TextSecondary)
            }
        }
        Text(tr("Écoute-le, puis fais-le tien : un son, la structure, et partage-le."), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigChoice(
                if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, if (ui.playing) tr("Arrêter") else tr("Écouter"),
                primary = true, tag = "song_ready_listen", onClick = actions.listen, modifier = Modifier.weight(1f),
            )
            BigChoice(Icons.Filled.LibraryMusic, tr("Changer un son"), tag = "song_ready_sound", onClick = actions.changeSound, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigChoice(Icons.Filled.ViewTimeline, tr("Modifier le morceau"), tag = "song_ready_structure", onClick = actions.structure, modifier = Modifier.weight(1f))
            BigChoice(Icons.Filled.IosShare, tr("Partager"), tag = "song_ready_share", onClick = actions.share, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            actions.guide?.let { guide ->
                TextButton(onClick = guide, modifier = Modifier.heightIn(min = 48.dp).testTag("song_ready_guide")) {
                    Text(tr("Lire le guide de ton morceau"), color = Neon.Cyan)
                }
            }
            Box(Modifier.weight(1f))
            TextButton(onClick = actions.close, modifier = Modifier.heightIn(min = 48.dp).testTag("song_ready_later")) {
                Text(tr("Plus tard"), color = Neon.TextSecondary)
            }
        }
    }
}

/** A big square-ish choice: a round icon over its word; [primary] filled with the accent. */
@Composable
private fun BigChoice(icon: ImageVector, label: String, tag: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    val shape = RoundedCornerShape(18.dp)
    val color = Neon.Cyan
    Column(
        modifier.heightIn(min = 104.dp).clip(shape)
            .background(if (primary) color.copy(alpha = 0.2f) else Neon.SurfaceHigh)
            .border(if (primary) 2.dp else 1.dp, if (primary) color else Neon.Outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(if (primary) color else color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (primary) Neon.OnAccent else color, modifier = Modifier.size(26.dp))
        }
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** "2:40" from seconds. */
private fun clock(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

/** Notes below this pitch (C3) make a track a bass line. */
private const val BASS_BELOW = 48

/**
 * The track « Changer un son » opens: the instrument track that carries the tune (the most
 * notes over the song, a bass line only if nothing else plays), or null when none has notes.
 */
fun mainTrackOf(project: Project): Int? {
    val scenes = project.allScenes
    val tracks = project.instrumentTracks.map { t ->
        val notes = scenes.flatMap { s -> (if (t.id == 0) s.synthNotes else s.trackNotes[t.id])?.notes.orEmpty() }
        t.id to notes
    }.filter { it.second.isNotEmpty() }
    if (tracks.isEmpty()) return null
    val tunes = tracks.filter { (_, notes) -> notes.map { it.pitch }.sorted()[notes.size / 2] >= BASS_BELOW }
    return (tunes.ifEmpty { tracks }).maxByOrNull { it.second.size }?.first
}

