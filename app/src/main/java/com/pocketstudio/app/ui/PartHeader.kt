package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledDropdownMenu

/**
 * What the editors (Rythme, Acide, Synthé, Audio) say above their grids: [title] the part being
 * edited (« Partie : C · Drop »), [note] one short line under it when there is something to know,
 * [loop] the part loops (the ⟳ icon lit in the accent).
 */
data class PartHeaderUi(val title: String, val note: String? = null, val loop: Boolean = false)

/**
 * The header of the editors for [p]: the part shown; while the song plays and the screens follow
 * it, a word that a touch on the grid keeps this part; once « ⟳ Partie » went on by itself
 * ([armedBySelf]), that this part loops and how to get the song back. [songPlaying]: the song
 * plays (not a part on its own); [loopPart]: « ⟳ Partie » is on. [waiting]: a part picked while the music plays, which
 * takes over at the next bar line (round 5, A a #27: until then nothing said the choice was taken).
 */
fun partHeader(p: Project, playing: Boolean, songPlaying: Boolean, loopPart: Boolean, armedBySelf: Boolean, waiting: Int? = null): PartHeaderUi {
    val title = tr("Partie : %s", p.sceneTitle(p.scene))
    val hasSong = p.song.isNotEmpty()
    return when {
        playing && waiting != null && waiting != p.scene && waiting in p.allScenes.indices ->
            PartHeaderUi(title, tr("%s arrive à la prochaine mesure…", p.sceneTitle(waiting)), loop = loopPart)
        armedBySelf && playing -> PartHeaderUi(title, tr("Elle tourne en boucle pendant que tu la modifies. ⟳ Boucler, en haut, pour reprendre le morceau."), loop = true)
        loopPart && hasSong -> PartHeaderUi(title, tr("En boucle (⟳ Boucler). ▶ joue cette partie seule."), loop = true)
        songPlaying -> PartHeaderUi(title, tr("Le morceau joue : touche la grille et cette partie reste sous tes doigts, en boucle."))
        else -> PartHeaderUi(title)
    }
}

/**
 * The editors' header: one line, the accent for the part's name; a short note under it when needed.
 * [parts] (the Mode simple, whose top bar has no part chip): a touch on the name lists the parts
 * to edit another ([pick]); [current] is the one shown.
 */
@Composable
fun PartHeader(ui: PartHeaderUi, modifier: Modifier = Modifier, parts: List<String>? = null, current: Int = 0, pick: (Int) -> Unit = {}, newPart: (() -> Unit)? = null) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    // Audit ultra round 1, n° 24 (seat B b looked here for a part B): the menu also makes a new part, so it shows with one part too.
    val menu = parts != null && (parts.size > 1 || newPart != null)
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).testTag("part_header")) {
        Row(
            if (menu) {
                Modifier.heightIn(min = 40.dp).clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = tr("Changer de partie")) { open = true }.testTag("part_header_pick")
            } else Modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ui.loop) {
                Icon(Icons.Filled.Repeat, contentDescription = tr("en boucle"), tint = Neon.Cyan, modifier = Modifier.padding(end = 4.dp).size(16.dp).testTag("part_header_loop"))
            }
            Text(
                ui.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.Cyan,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).testTag("part_header_title"),
            )
            if (menu && parts != null) {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(20.dp))
                ScaledDropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.testTag("part_header_menu")) {
                    Text(tr("Partie à modifier"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    parts.forEachIndexed { i, t ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(t, fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal, color = if (i == current) Neon.Cyan else Neon.TextPrimary) },
                            onClick = { open = false; if (i != current) pick(i) },
                            modifier = Modifier.testTag("part_pick_$i"),
                        )
                    }
                    if (newPart != null) {
                        androidx.compose.material3.HorizontalDivider(color = Neon.Outline.copy(alpha = 0.45f))
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(tr("+ Nouvelle partie (copie de %s)", parts.getOrElse(current) { "" })) },
                            onClick = { open = false; newPart() },
                            modifier = Modifier.testTag("part_new"),
                        )
                    }
                }
            }
        }
        if (ui.note != null) {
            Text(ui.note, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("part_header_note"))
        }
    }
}
