package com.pocketstudio.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * Renames the open project where its name is (a long press on it in the top bar): a bar right
 * under it, the name selected and the keyboard up, ready to be typed over. OK or the keyboard's ✓
 * keeps it (one undo step); ✕ or Retour leaves it as it was.
 */
@Composable
fun RenameProjectBar(current: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue(current, selection = TextRange(0, current.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BackHandler(onBack = onDismiss)
    val ok = name.text.isNotBlank()
    Row(
        Modifier.fillMaxWidth().background(Neon.Surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
            .testTag("rename_project_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = if (it.text.length <= ProjectCatalog.MAX_NAME) it else it.copy(text = it.text.take(ProjectCatalog.MAX_NAME)) },
            singleLine = true,
            label = { Text(tr("Nom du projet")) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (ok) onDone(name.text) }),
            modifier = Modifier.weight(1f).focusRequester(focus).testTag("rename_project_field"),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.testTag("rename_project_cancel")) {
            Icon(Icons.Filled.Close, contentDescription = tr("Annuler"), tint = Neon.TextSecondary)
        }
        Button(
            onClick = { onDone(name.text) }, enabled = ok,
            colors = ButtonDefaults.buttonColors(containerColor = Neon.Cyan, contentColor = Neon.OnAccent),
            modifier = Modifier.testTag("rename_project_ok"),
        ) { Text(tr("OK")) }
    }
}
