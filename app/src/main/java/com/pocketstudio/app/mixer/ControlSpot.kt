package com.pocketstudio.app.mixer

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/** « Voir le réglage » (a curve's way to the control it moves): that control, null: none. */
val LocalControlSpot = staticCompositionLocalOf<AutoTarget?> { null }

/** The control [LocalControlSpot] names is this one ([kind] on [ch], [slot], [param]). */
@Composable
internal fun isSpot(kind: Int, ch: Int, slot: Int = 0, param: Int = 0): Boolean =
    LocalControlSpot.current?.let { it.kind == kind && it.ch == ch && it.slot == slot && it.param == param } == true

/**
 * Chantier 8 (Benjamin, tel-14: « Voir le réglage » opened the effects on their top, the send far
 * below): the control a curve moves, framed in the curves' amber, said in a line and brought into
 * view once laid out. The list scrolls once; nothing else moves.
 */
@Composable
internal fun SpotFrame(tag: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val bring = remember { BringIntoViewRequester() }
    LaunchedEffect(Unit) { bring.bringIntoView() }
    Column(
        modifier.fillMaxWidth().bringIntoViewRequester(bring)
            .border(2.dp, Neon.Amber, RoundedCornerShape(10.dp)).padding(6.dp).testTag(tag),
    ) {
        Text(
            tr("Une courbe fait bouger ce réglage pendant la lecture."),
            style = MaterialTheme.typography.labelMedium, color = Neon.Amber,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        content()
    }
}
