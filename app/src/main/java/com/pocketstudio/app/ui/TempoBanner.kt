package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/** How long « Tempo calé sur … » stays, unless touched. */
const val TEMPO_BANNER_MS = 12_000L

/**
 * Step 4e: « Tempo calé sur Boys : 113 · Annuler », under the top bar on every tab once the first
 * sound with a tempo set the project's (Benjamin's list n° 16). « Annuler » gives the tempo before
 * back in one step, the sound kept; the banner leaves by itself after a while.
 */
@Composable
fun TempoBanner(set: RhythmViewModel.TempoSet?, undo: () -> Unit, dismiss: () -> Unit) {
    if (set == null) return
    LaunchedEffect(set) {
        kotlinx.coroutines.delay(TEMPO_BANNER_MS)
        dismiss()
    }
    Row(
        Modifier.fillMaxWidth().background(Neon.SurfaceHigh)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("tempo_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Speed, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
        Text(
            tr("Tempo calé sur %1\$s : %2\$d", DefaultNames.shownSample(set.name), Math.round(set.bpm).toInt()),
            style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            tr("Annuler"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.Cyan, maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).border(1.5.dp, Neon.Cyan, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = undo)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("tempo_banner_undo"),
        )
    }
}
