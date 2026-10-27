package com.pocketstudio.app.rhythm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.guide.GuideTrack
import com.pocketstudio.model.guide.guideTracks
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * « Écouter seul » (bug report, APK 224): what is heard alone, looped. [pads]: drum pads heard alone
 * inside the kit (the rest of the kit and every other track silent); [channels]: track strips heard
 * alone (a whole track, the drums as a whole on strip 0). [parts]: their song keys ([SongPart.plays]),
 * to find a part where they play.
 */
data class AloneTarget(val pads: Set<Int> = emptySet(), val channels: Set<Int> = emptySet(), val parts: Set<String> = emptySet()) {
    val isEmpty: Boolean get() = pads.isEmpty() && channels.isEmpty()

    /**
     * The scene looped: the one shown when the sound plays there, else the first one where it plays
     * (the shown one when it plays nowhere yet).
     */
    fun sceneIn(p: Project): Int {
        val scenes = p.allScenes
        fun plays(scene: Int): Boolean {
            val s = scenes.getOrNull(scene) ?: return false
            if (pads.isNotEmpty()) return pads.any { lane -> s.drums.lanes.getOrNull(lane)?.hits?.keys?.any { it < s.drums.steps } == true }
            if (p.song.isEmpty() || parts.isEmpty()) return true
            return p.song.any { part -> parts.any { k -> part.plays(k) && part.sceneFor(k) == scene } }
        }
        if (plays(p.scene)) return p.scene
        return scenes.indices.firstOrNull(::plays) ?: p.scene
    }

    companion object {
        /** A track of the Guide ([GuideTrack.key]: "pad:0", "drums", "inst:3"…). */
        fun ofKey(key: String): AloneTarget {
            if (key.startsWith("pad:")) {
                val lane = key.removePrefix("pad:").toIntOrNull()?.takeIf { it in 0 until Mixer.PADS } ?: return AloneTarget()
                return AloneTarget(pads = setOf(lane), parts = setOf(SongPart.DRUMS))
            }
            val ch = GuideTrack.channelOf(key)?.takeIf { it != Mixer.MASTER } ?: return AloneTarget()
            return AloneTarget(channels = setOf(ch), parts = setOfNotNull(GuideTrack.partKey(key)))
        }

        /** A track of « Mes pistes » (its song key: "drums", "acid", "inst:3", "audio:1"). */
        fun ofSongKey(key: String): AloneTarget = when {
            key == SongPart.DRUMS -> ofKey(GuideTrack.DRUMS)
            key == SongPart.ACID -> ofKey(GuideTrack.ACID)
            key.startsWith("inst:") -> AloneTarget(channels = setOfNotNull(key.removePrefix("inst:").toIntOrNull()?.let(InstrumentTrack::mixerChannel)), parts = setOf(key))
            key.startsWith("audio:") -> AloneTarget(channels = setOfNotNull(key.removePrefix("audio:").toIntOrNull()?.let(AudioTrack::mixerChannel)), parts = setOf(key))
            else -> AloneTarget()
        }

        /** Every track of role [role] in [p] (the snare's card: the snare and the clap pads). */
        fun ofRole(p: Project, role: String): AloneTarget {
            val tracks = p.guideTracks().filter { it.role == role && it.channel != Mixer.MASTER }
            val pads = tracks.filter { it.key.startsWith("pad:") }
            // Pads and tracks of one role (a trap's 808 pad and its bass track): the tracks win, a pad alone would hide them.
            val chosen = if (pads.size == tracks.size) pads else tracks.filter { !it.key.startsWith("pad:") }
            return chosen.map { ofKey(it.key) }.fold(AloneTarget()) { a, b -> AloneTarget(a.pads + b.pads, a.channels + b.channels, a.parts + b.parts) }
        }
    }
}

/** What is being heard alone, and from where it was asked ([from]: "card:<id>" for a Guide card, else ""). */
data class AloneListen(val target: AloneTarget, val from: String = "")

/** Keeps « Écouter seul » going while this screen shows ([active]); once no screen holds it any more, it stops. */
@Composable
fun HoldAlone(active: Boolean, hold: () -> (() -> Unit)) {
    DisposableEffect(active) {
        val release = if (active) hold() else null
        onDispose { release?.invoke() }
    }
}

/**
 * « ▶ Écouter seul » / « ■ Arrêter »: one touch, the sound alone in a loop; another, everything as it
 * was. [label]: « Écouter seul » or « Écouter seule »; [name]: what is heard, for TalkBack.
 */
@Composable
fun AloneButton(on: Boolean, label: String, name: String, tag: String, modifier: Modifier = Modifier, toggle: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier.heightIn(min = 48.dp).clip(shape)
            .background(if (on) Neon.Acid else Neon.Acid.copy(alpha = 0.14f))
            .border(1.dp, Neon.Acid, shape)
            .clickable(role = Role.Button, onClickLabel = if (on) tr("Arrêter") else label, onClick = toggle)
            .semantics {
                selected = on
                contentDescription = if (on) tr("Arrêter") else tr("Écouter « %s » seul, en boucle", name)
            }
            .padding(horizontal = 16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val ink = if (on) Color.Black else Neon.Acid
        Icon(if (on) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        Text(if (on) tr("Arrêter") else label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ink, maxLines = 1)
    }
}
