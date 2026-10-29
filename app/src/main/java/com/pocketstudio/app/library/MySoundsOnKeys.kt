package com.pocketstudio.app.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.SoundTile
import com.pocketstudio.ui.TileGrid
import kotlinx.coroutines.launch

/**
 * « Changer le son » of an instrument track (the Clavier, « Jouer au clavier »): the sounds kept in
 * « Ma bibliothèque », as the app's own sounds are shown (the same tiles, their family's colour, an
 * « À moi » badge). A touch plays it across the keys of track [track] (heard at once) and closes.
 * Nothing shows while the library is empty.
 */
@Composable
fun MySoundsOnKeys(vm: RhythmViewModel, track: Int, close: () -> Unit) {
    val context = LocalContext.current
    val mine by MySoundLibrary.sounds.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { MySoundLibrary.refresh(context) }
    if (mine.isEmpty()) return
    val scope = rememberCoroutineScope()
    Column(Modifier.testTag("keys_mine"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(tr("Ma bibliothèque"))
        TileGrid(mine.size) { i, m ->
            val s = mine[i]
            val look = SoundLooks.of(s.kind)
            SoundTile(
                title = DefaultNames.shownSample(s.name), icon = look.icon, accent = look.color, selected = false,
                subtitle = mySoundDetails(s), badge = tr("À moi"),
                onClick = {
                    scope.launch {
                        val pcm = MySoundLibrary.load(context, s.id)
                        if (pcm == null) return@launch
                        if (vm.selectedTrack.value != track) vm.selectTrack(track)
                        if (vm.useAsTrackSound(pcm, s.name, s.kind)) close()
                    }
                },
                modifier = m.testTag("keys_mine_$i"),
            )
        }
        Text(tr("Touche un son : il remplace celui de cette piste dans tout le morceau."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}
