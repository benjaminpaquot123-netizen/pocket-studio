package com.pocketstudio.app.instruments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.MultiInstruments
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.SoundTile
import com.pocketstudio.ui.TileGrid
import kotlin.math.roundToInt

/** Where one instrument of the chooser is: to download (its size), coming, on the phone, or not published yet. */
sealed interface RealState {
    data class Missing(val sizeLabel: String) : RealState
    data class Downloading(val progress: Float) : RealState
    /** Downloaded; [loading] while it is read into memory for a track. */
    data class Ready(val loading: Boolean = false) : RealState
    /** The catalogue does not list it (offline, or not built yet): nothing to download. */
    data object Unavailable : RealState
}

data class RealInstrumentItem(
    val entry: MultiInstruments.Entry,
    val state: RealState,
    val selected: Boolean = false,
    val previewing: Boolean = false,
)

/**
 * The multizone instruments in the sound chooser (phase 38): real recordings (pianos, strings,
 * brass, woodwinds, harp) and the 808. A touch on a downloaded one gives it to the track; on one
 * not downloaded, the download starts and the track gets it when it is there. ▶ plays a few
 * seconds of it, downloaded or not.
 */
class RealInstrumentsUi(
    val items: List<RealInstrumentItem>,
    val pick: (MultiInstruments.Entry) -> Unit = {},
    val preview: (MultiInstruments.Entry) -> Unit = {},
    val cancel: (MultiInstruments.Entry) -> Unit = {},
    /** The 808 tile (null: not offered); [is808]: the track plays it. */
    val use808: (() -> Unit)? = null,
    val is808: Boolean = false,
)

private fun iconOf(category: String): ImageVector = when (category) {
    "piano" -> Icons.Filled.Piano
    "strings" -> Icons.Filled.Waves
    "brass" -> Icons.Filled.Campaign
    "woodwinds" -> Icons.Filled.Air
    else -> Icons.Filled.MusicNote
}

@Composable
fun RealInstruments(ui: RealInstrumentsUi, modifier: Modifier = Modifier) {
    Column(modifier.testTag("real_instruments"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(tr("Instruments réels"), foldKey = "Instruments réels")
        Text(
            tr("Enregistrés note par note. À télécharger une seule fois (Wi-Fi conseillé) ; en attendant, la piste joue un son de synthé."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        val tiles = ui.items.size + if (ui.use808 != null) 1 else 0
        TileGrid(tiles, minTileWidth = 240.dp) { i, m ->
            if (i == ui.items.size) {
                SoundTile(
                    title = tr("Basse 808"), icon = Icons.Filled.GraphicEq, accent = Neon.Magenta, selected = ui.is808,
                    onClick = { ui.use808?.invoke() }, subtitle = tr("Elle glisse d'une note à l'autre, rien à télécharger"),
                    modifier = m.testTag("real_808"),
                )
                return@TileGrid
            }
            val item = ui.items[i]
            val e = item.entry
            val subtitle = when (val st = item.state) {
                is RealState.Missing -> tr("À télécharger : %s", st.sizeLabel)
                is RealState.Downloading -> tr("Téléchargement… %d %%", (st.progress * 100).roundToInt())
                is RealState.Ready -> if (st.loading) tr("Préparation…") else tr(e.description)
                RealState.Unavailable -> tr("Bientôt disponible")
            }
            SoundTile(
                title = tr(e.name), icon = iconOf(e.category), accent = Neon.Cyan, selected = item.selected,
                onClick = {
                    when (item.state) {
                        is RealState.Downloading -> ui.cancel(e)
                        RealState.Unavailable -> Unit
                        else -> ui.pick(e)
                    }
                },
                subtitle = subtitle,
                modifier = m.testTag("real_${e.pack}"),
                trailing = {
                    when (val st = item.state) {
                        is RealState.Downloading -> CircularProgressIndicator(
                            progress = { st.progress }, modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = Neon.Cyan,
                        )
                        RealState.Unavailable -> Unit
                        else -> IconButton(
                            onClick = { ui.preview(e) },
                            modifier = Modifier.testTag("real_play_${e.pack}").semantics { contentDescription = tr("Écouter") },
                        ) {
                            Icon(if (item.previewing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.Cyan)
                        }
                    }
                },
            )
        }
        // The credits the recordings ask for (Salamander: CC BY 3.0), always in sight.
        Text(
            tr("Enregistrements : Salamander Grand Piano V3 (Alexander Holm, CC BY 3.0), Splendid Grand Piano (Akai, domaine public), VS Chamber Orchestra (Versilian Studios, CC0)."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("real_credits"),
        )
    }
}

/** The chooser's items from what the catalogue lists and what is on the phone. */
fun realItems(
    published: Map<String, com.pocketstudio.model.PackEntry>,
    installed: List<com.pocketstudio.model.PackManifest>,
    downloading: Map<String, Float>,
    loading: Set<String>,
    currentPack: String?,
    previewing: String?,
): List<RealInstrumentItem> = MultiInstruments.KNOWN.map { e ->
    val have = installed.firstOrNull { it.id == e.pack && it.instrument != null }
    val state = when {
        e.pack in downloading -> RealState.Downloading(downloading.getValue(e.pack))
        have != null -> RealState.Ready(loading = e.pack in loading)
        published[e.pack] != null -> RealState.Missing(published.getValue(e.pack).sizeLabel)
        else -> RealState.Unavailable
    }
    RealInstrumentItem(e, state, selected = e.pack == currentPack, previewing = previewing == e.pack)
}
