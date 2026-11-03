package com.pocketstudio.app.padlive

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledBottomSheet
import kotlinx.coroutines.delay

/** « Enregistrer ce que je joue » (lot B2), as the mockup's red button: its words say where the take is. */
@Composable
internal fun PlayedButton(state: PlayedTakeUi, press: () -> Unit, modifier: Modifier) {
    val words = when (state) {
        PlayedTakeUi.Ready, is PlayedTakeUi.Done -> tr("Enregistrer ce que je joue")
        PlayedTakeUi.Armed -> tr("Prêt : ça part à la mesure")
        is PlayedTakeUi.Recording -> if (state.bars == 1) tr("1 mesure · touche pour finir") else tr("%d mesures · touche pour finir", state.bars)
        PlayedTakeUi.Ending -> tr("Ça finit à la mesure…")
    }
    val on = state is PlayedTakeUi.Recording || state == PlayedTakeUi.Ending
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.heightIn(min = 52.dp).clip(shape)
            .background(if (on) Neon.Red.copy(alpha = 0.30f) else Neon.Red.copy(alpha = 0.12f))
            .border(1.dp, Neon.Red.copy(alpha = if (on || state == PlayedTakeUi.Armed) 0.9f else 0.45f), shape)
            .clickable(role = Role.Button, onClick = press)
            .padding(horizontal = 14.dp)
            .testTag("padlive_played"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(if (state == PlayedTakeUi.Armed) Neon.Red.copy(alpha = 0.5f) else Neon.Red))
        // Next to « Moins » on a small phone: smaller before it is cut.
        val style = MaterialTheme.typography.titleMedium
        Text(
            words, style = style, color = Neon.TextPrimary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
            modifier = Modifier.weight(1f).testTag("padlive_played_words"),
        )
    }
}

/** The take recorded: on an empty pad ([emptyPad], « A12 »; null: none free), in a brick of the Live, or thrown away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayedTakeSheet(bars: Int, emptyPad: String?, actions: PadLiveActions) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = actions.dropTake, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("padlive_take_sheet")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (bars == 1) tr("Ta prise : 1 mesure") else tr("Ta prise : %d mesures", bars),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Neon.TextPrimary,
            )
            Text(tr("Où la mettre ? Elle tourne en boucle au tempo du projet."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
            Button(onClick = actions.placeOnPad, enabled = emptyPad != null, modifier = Modifier.fillMaxWidth().testTag("padlive_take_pad")) {
                Text(if (emptyPad != null) tr("Sur le pad vide %s", emptyPad) else tr("Aucun pad vide"))
            }
            OutlinedButton(onClick = actions.placeInBrick, modifier = Modifier.fillMaxWidth().testTag("padlive_take_brick")) {
                Text(tr("Dans une brique du Live"))
            }
            TextButton(onClick = actions.dropTake, modifier = Modifier.fillMaxWidth().testTag("padlive_take_drop")) {
                Text(tr("Jeter la prise"), color = Neon.TextSecondary)
            }
        }
    }
}

/** A word shown for a few seconds under the pages (a take placed, too short…), where « Préparation du pack… » shows. */
@Composable
internal fun PadLiveMessage(text: String, shown: () -> Unit) {
    LaunchedEffect(text) {
        delay(MESSAGE_MS)
        shown()
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Neon.Amber, modifier = Modifier.testTag("padlive_message"))
}

private const val MESSAGE_MS = 3_500L
