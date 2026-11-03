package com.pocketstudio.app.remix

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/*
 * docs/remix/REMIX-PROJET.md, « Rien ne doit se perdre »: when a song made with someone else's
 * song is shared, one discreet line says that song belongs to its authors. Never a block: the app
 * is for personal use. Lot C writes it; the share sheet (B, then E) calls [RightsLine] in one line.
 */
object RightsNotice {
    /** The line for [p], null when it holds no separated song. */
    fun line(p: Project): String? {
        if (p.remix.setId == null) return null
        val name = p.remix.name.trim()
        return if (name.isEmpty()) tr("La chanson d'origine appartient à ses auteurs : garde ce remix pour un usage personnel.")
        else tr("« %s » appartient à ses auteurs : garde ce remix pour un usage personnel.", name)
    }
}

/** [RightsNotice.line] for [project], small and grey under the share buttons; nothing without a song. */
@Composable
fun RightsLine(project: Project, modifier: Modifier = Modifier) {
    val line = RightsNotice.line(project) ?: return
    Row(modifier.testTag("share_rights"), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = Neon.TextSecondary, modifier = Modifier.size(16.dp))
        Text(line, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.padding(start = 6.dp))
    }
}
