package com.pocketstudio.app.padlive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.ui.barsShort
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadMode
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ProSlider
import com.pocketstudio.ui.ScaledBottomSheet
import com.pocketstudio.ui.pillColors

/**
 * A pad's settings (its corner touched, or a long touch for TalkBack): how it plays (« Coup », «
 * Boucle », « Tenir »), its « un seul à la fois » group, a loop's length, its volume, another
 * sound, « Vider ». Each change is one undo step and is heard at once.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun PadSettingsSheet(pad: Int, clip: LiveClip, actions: PadLiveActions, close: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chips = pillColors()
    ScaledBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("padlive_settings")) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                tr("Pad %1\$s%2\$d : %3\$s", PadLive.PAGE_NAMES[PadLive.pageOf(pad)], pad % PadLive.PAGE_SIZE + 1, DefaultNames.shownSample(clip.name)),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Neon.TextPrimary,
            )
            Text(tr("Quand je touche le pad"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
            val mode = PadLive.modeOf(clip)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PadMode.SHOT to tr("Coup : il joue une fois"),
                    PadMode.LOOP to tr("Boucle : un toucher lance, un autre arrête"),
                    PadMode.HOLD to tr("Tenir : il joue tant que j'appuie"),
                ).forEach { (m, label) ->
                    FilterChip(selected = mode == m, onClick = { actions.setMode(pad, m) }, label = { Text(label) }, colors = chips,
                        modifier = Modifier.testTag("padlive_mode_${m.name.lowercase()}"))
                }
            }
            Text(tr("Un seul à la fois"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
            Text(
                tr("Les pads d'un même groupe se coupent entre eux (charley ouvert et fermé)."),
                style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..PadLive.GROUPS).forEach { g ->
                    FilterChip(
                        selected = clip.group == g, onClick = { actions.setGroup(pad, g) },
                        label = { Text(if (g == 0) tr("Aucun") else tr("Groupe %d", g)) }, colors = chips,
                        modifier = Modifier.testTag("padlive_group_$g"),
                    )
                }
            }
            if (!clip.oneShot) {
                Text(tr("Longueur de la boucle"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LivePads.BEATS.forEach { b ->
                        FilterChip(
                            selected = clip.beats == b, onClick = { actions.setBeats(pad, b) },
                            label = { Text(if (b < 4) (if (b == 1) tr("1 temps") else tr("%d temps", b)) else barsShort(b / 4)) }, colors = chips,
                            modifier = Modifier.testTag("padlive_beats_$b"),
                        )
                    }
                }
            }
            Text(tr("Volume"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
            ProSlider(clip.gain, { actions.setGain(pad, it) }, valueRange = 0f..2f, modifier = Modifier.testTag("padlive_gain"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { close(); actions.browse(pad) }, modifier = Modifier.weight(1f).testTag("padlive_change_sound")) {
                    Text(tr("Changer le son"))
                }
                OutlinedButton(onClick = { close(); actions.clear(pad) }, modifier = Modifier.weight(1f).testTag("padlive_clear")) {
                    Text(tr("Vider"))
                }
            }
        }
    }
}
