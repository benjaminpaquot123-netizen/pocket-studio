package com.pocketstudio.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A word of the app explained in one or two plain sentences. */
data class Help(val term: String, val text: String)

/**
 * A small faded "?" in a circle next to a term; a tap explains it. Discreet on purpose: it is
 * there for whoever wonders, not to draw the eye.
 */
@Composable
fun HelpDot(help: Help, modifier: Modifier = Modifier, color: Color = Neon.TextSecondary) {
    var open by remember { mutableStateOf(false) }
    val ink = color.copy(alpha = 0.55f)
    // The touch target is larger than the drawn circle.
    Box(
        modifier
            .clip(CircleShape)
            .clickable { open = true }
            .padding(4.dp)
            .semantics { contentDescription = "Qu'est-ce que « ${help.term} » ?" }
            .testTag("help_${help.term}"),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(15.dp).border(1.dp, ink, CircleShape), contentAlignment = Alignment.Center) {
            Text("?", color = ink, fontSize = 10.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
    if (open) HelpDialog(help) { open = false }
}

@Composable
fun HelpDialog(help: Help, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Compris") } },
        title = { Text(help.term) },
        text = { Text(help.text, style = MaterialTheme.typography.bodyMedium) },
        containerColor = Neon.Surface,
    )
}

/** A term followed by its help dot, on one line. */
@Composable
fun WithHelp(help: Help?, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        content()
        if (help != null) HelpDot(help)
    }
}
