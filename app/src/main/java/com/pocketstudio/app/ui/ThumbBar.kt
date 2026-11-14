package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/** What the thumb's bar shows: the same state as the top bar's ▶, ↶ and ↷, and the tempo. */
class ThumbBarUi(
    val playing: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val tempoBpm: Int,
    /** ↷ offered all the time (the Pro); false: only once there is something to bring back (the Mode simple). */
    val redoAlways: Boolean,
    /** Tour 5, chantier 4: the song plays (not one part looped), so « ▶ le morceau » shows ■. Defaults to [playing]. */
    val songPlaying: Boolean = playing,
)

class ThumbBarActions(
    val undo: () -> Unit,
    val redo: () -> Unit,
    val togglePlay: () -> Unit,
    val tempo: () -> Unit,
    /** The Mode simple: the top bar's « ⋮ » at the thumb too, the Plus page (tour 5, chantier 5); null: ⤴ Partager. */
    val more: (() -> Unit)?,
    val share: () -> Unit,
)

/**
 * Audit ultra round 1 (n° 21, four seats held the phone in one hand): ▶, ↶ Annuler, the tempo, Partager and the
 * Mode simple's « ⋯ » were only at the top of the screen, out of the thumb's reach. The same actions in a slim bar
 * right above the tabs (the top bar keeps them too); ↷ Rétablir there too, in the Mode simple once ↶ was used (n° 26).
 */
@Composable
fun ThumbBar(
    ui: ThumbBarUi, actions: ThumbBarActions,
    /** The folded Guide's pill, at the bar's end (a wide, short screen: [com.pocketstudio.app.guide.GuidePill]); null: none. */
    guide: (@Composable (Modifier) -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().background(lerp(Neon.Background, Color.Black, 0.06f)).padding(horizontal = 4.dp).testTag("thumb_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // ▶ and the tempo are measured first, whole; the other buttons share what is left (their words end in « … » before
        // the bar runs off the screen: a small phone at 130 % in German).
        val share = Modifier.weight(1f, fill = false)
        ThumbButton(Icons.AutoMirrored.Filled.Undo, tr("Annuler (défaire)"), tr("Annuler la dernière action"), ui.canUndo, "thumb_undo", share, actions.undo)
        if (ui.redoAlways || ui.canRedo) {
            ThumbButton(Icons.AutoMirrored.Filled.Redo, tr("Rétablir"), tr("Rétablir"), ui.canRedo, "thumb_redo", share, actions.redo)
        } else {
            // Tour 5, lot « langues » (t5-Bd-3: « إعادة » came where « تراجع » was, the 2nd touch redid): ↷'s room kept, empty
            // and silent, so ↶ never moves under the finger when ↷ comes.
            Box(share.alpha(0f).clearAndSetSemantics {}) {
                ThumbButton(Icons.AutoMirrored.Filled.Redo, tr("Rétablir"), tr("Rétablir"), false, "thumb_redo_room", onClick = {})
            }
        }
        // « ▶ le morceau » in the middle (tour 5, chantier 4, Benjamin's 2B): the whole song; the top bar's is « ▶ cette partie ».
        val playing = ui.songPlaying
        val playColor = if (playing) Neon.Acid else Neon.Cyan
        val said = if (playing) tr("Arrêter") else tr("Jouer le morceau")
        Column(
            Modifier.widthIn(min = 56.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = said, onClick = actions.togglePlay)
                .semantics { contentDescription = said }
                .padding(horizontal = 4.dp)
                .testTag("thumb_play"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.requiredSize(32.dp).clip(CircleShape).background(playColor), contentAlignment = Alignment.Center) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.OnAccent, modifier = Modifier.size(22.dp))
            }
            // Whole at any size and in any language (« der Song », « la canción »): never « le mor… ».
            Text(
                tr("le morceau"), fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = if (playing) Neon.Acid else Neon.TextPrimary,
                maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
            )
        }
        Column(
            Modifier.widthIn(min = 64.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = tr("Changer le tempo"), onClick = actions.tempo)
                .semantics { contentDescription = tr("Tempo : %d BPM", ui.tempoBpm) }
                .padding(horizontal = 6.dp)
                .testTag("thumb_tempo"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("${ui.tempoBpm}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary, maxLines = 1)
            com.pocketstudio.ui.WholeWordsText(
                tr("Tempo"), style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, lineHeight = 12.sp), color = Neon.TextSecondary,
                maxLines = 1, minScale = 0.5f,
            )
        }
        val more = actions.more
        if (more != null) {
            ThumbButton(Icons.Filled.MoreVert, tr("Plus"), tr("Plus de choix"), true, "thumb_more", share, more)
        } else {
            ThumbButton(Icons.Filled.Share, tr("Partager"), tr("Partager mon morceau"), true, "thumb_share", share, actions.share)
        }
        // Measured first, whole, as ▶ and the tempo.
        guide?.invoke(Modifier)
    }
}

/**
 * A switched-off thumb button (↶ with nothing to undo): still dimmed, but its word stays readable
 * (audit ultra round 2, n° 30, night theme: « Annuler » at 0.38 was 2.1:1, now 3.5:1 at night, 4.2:1 in Studio).
 */
internal const val THUMB_OFF_ALPHA = 0.6f

/** An icon over its word, 48 dp at least each way. */
@Composable
private fun ThumbButton(icon: ImageVector, label: String, description: String, enabled: Boolean, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.widthIn(min = 56.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else THUMB_OFF_ALPHA)
            .padding(horizontal = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Neon.TextPrimary, modifier = Modifier.size(22.dp))
        // Tour 5, lot « langues » (200 %: « Annul… », « Rückg… »): the word set smaller, whole, before any « … ».
        com.pocketstudio.ui.WholeWordsText(
            label, style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, lineHeight = 12.sp), color = Neon.TextSecondary,
            maxLines = 1, minScale = 0.5f, textAlign = TextAlign.Center,
        )
    }
}
