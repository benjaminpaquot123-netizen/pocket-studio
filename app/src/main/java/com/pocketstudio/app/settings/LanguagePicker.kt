package com.pocketstudio.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledAlertDialog

/**
 * The interface language: one button with the language in use, which opens the list of every
 * language, each named in itself (« Deutsch », « 日本語 », « العربية »), so that anyone finds theirs
 * whatever the screen shows. The list grows with [Lang] alone. Picking one also names the notes
 * the way that language does (Do Ré Mi in French, C D E otherwise).
 */
@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by SettingsStore.state.collectAsState()
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier.heightIn(min = 48.dp).testTag("lang_button")) {
        Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(" " + settings.language.nativeName, fontWeight = FontWeight.Bold)
        Icon(Icons.Filled.ExpandMore, contentDescription = tr("Choisis ta langue"), modifier = Modifier.size(20.dp))
    }
    if (open) {
        LanguageList(
            current = settings.language,
            pick = { lang ->
                open = false
                if (settings.language != lang) SettingsStore.update(context) { it.copy(language = lang, englishNotes = lang != Lang.FR) }
            },
            close = { open = false },
        )
    }
}

/** Every language of [Lang], scrolling when there are many, the one in use ticked. */
@Composable
fun LanguageList(current: Lang, pick: (Lang) -> Unit, close: () -> Unit) {
    ScaledAlertDialog(
        onDismissRequest = close,
        title = { Text(tr("Choisis ta langue")) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).testTag("lang_list")) {
                items(Lang.entries, key = { it.code }) { lang ->
                    val chosen = lang == current
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp))
                            .selectable(selected = chosen, role = Role.RadioButton) { pick(lang) }
                            .padding(horizontal = 12.dp)
                            .testTag("lang_${lang.code}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Each name in its own script and direction, whatever the interface's.
                        Text(
                            lang.nativeName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                localeList = LocaleList(lang.code),
                                textDirection = if (lang.rtl) TextDirection.Rtl else TextDirection.Ltr,
                            ),
                            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                            color = if (chosen) Neon.Cyan else Neon.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (chosen) Icon(Icons.Filled.Check, contentDescription = null, tint = Neon.Cyan)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = close, modifier = Modifier.testTag("lang_close")) { Text(tr("Fermer")) } },
        modifier = Modifier.testTag("lang_dialog"),
    )
}
