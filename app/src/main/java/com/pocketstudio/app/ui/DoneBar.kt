package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import kotlinx.coroutines.delay

/** How long « Fait : … » stays. */
private const val DONE_MS = 6000L

/**
 * « Fait : un fill à la fin de C · Drop » with « Annuler » beside it, after an intention (the
 * part sheet, a kit): what was done is said, and undone in one touch. It goes by itself.
 */
@Composable
fun DoneBar(text: String?, undo: () -> Unit, dismiss: () -> Unit) {
    if (text == null) return
    LaunchedEffect(text) {
        delay(DONE_MS)
        dismiss()
    }
    Surface(
        shape = RoundedCornerShape(12.dp), color = Neon.SurfaceHigh, shadowElevation = 6.dp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().testTag("done_bar"),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary, maxLines = 3,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp).testTag("done_text"))
            TextButton(onClick = undo, modifier = Modifier.testTag("done_undo")) { Text(tr("Annuler")) }
        }
    }
}
