package com.pocketstudio.app.record

import com.pocketstudio.ui.ScaledBottomSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongVersion
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.songBars
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon

/** One line of « Versions du morceau »: its title (when and why it was put aside) and its size. */
data class VersionRow(val title: String, val detail: String)

/** The clock time of [at] (« 14:32 »), with the day when it is not today (« 23/10 14:32 »). */
fun versionTime(at: Long, now: Long = System.currentTimeMillis()): String {
    val locale = I18n.lang.locale
    val day = java.text.SimpleDateFormat("yyyyMMdd", locale)
    val sameDay = day.format(java.util.Date(at)) == day.format(java.util.Date(now))
    return java.text.SimpleDateFormat(if (sameDay) "HH:mm" else "dd/MM HH:mm", locale).format(java.util.Date(at))
}

/** The rows of the journal, newest first, for project [p]. */
fun versionRows(p: Project, now: Long = System.currentTimeMillis()): List<VersionRow> = p.songVersions.map { v ->
    val time = versionTime(v.at, now)
    VersionRow(
        if (v.kind == SongVersion.RESTORE) tr("Mise de côté à %s", time) else tr("Avant la prise de %s", time),
        tr("%1\$d parties · %2\$d mes.", v.song.size, v.bars(p)),
    )
}

class SongVersionsActions(
    val restore: (Int) -> Unit = {},
    val drop: (Int) -> Unit = {},
    val close: () -> Unit = {},
)

/**
 * « Versions du morceau »: every song a Live take (or a restore) put aside, newest first, each
 * one touch from coming back (the song now is then put aside in its place: nothing is lost).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongVersionsSheet(p: Project, actions: SongVersionsActions) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = actions.close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("versions_sheet")) {
        SongVersionsContent(p, versionRows(p), actions)
    }
}

/** The sheet's body (drawn alone in the screenshots). */
@Composable
fun SongVersionsContent(p: Project, rows: List<VersionRow>, actions: SongVersionsActions) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Versions du morceau"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = actions.close, modifier = Modifier.testTag("versions_close")) { Text(tr("Fermer")) }
        }
        Text(
            tr("Chaque prise Live crée une nouvelle version : l'ancienne reste ici. « Ramener » la remet et garde celle en cours à sa place."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        // The song now, for comparison.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Acid.copy(alpha = 0.12f)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(tr("Le morceau en cours"), style = MaterialTheme.typography.titleSmall)
                Text(tr("%1\$d parties · %2\$d mes.", p.song.size, p.songBars), style = MonoStyle, color = Neon.TextSecondary)
            }
        }
        if (rows.isEmpty()) {
            Text(tr("Pas encore d'autre version : la prochaine prise Live gardera celle-ci."), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("versions_empty"))
        }
        rows.forEachIndexed { i, row ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    .testTag("version_$i"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(row.title, style = MaterialTheme.typography.titleSmall)
                    Text(row.detail, style = MonoStyle, color = Neon.TextSecondary)
                }
                OutlinedButton(onClick = { actions.restore(i) }, colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Neon.Cyan), modifier = Modifier.heightIn(min = 48.dp).testTag("version_restore_$i")) { Text(tr("Ramener")) }
                IconButton(onClick = { actions.drop(i) }, modifier = Modifier.testTag("version_drop_$i")) {
                    Icon(Icons.Filled.Close, contentDescription = tr("Oublier cette version"))
                }
            }
        }
    }
}
