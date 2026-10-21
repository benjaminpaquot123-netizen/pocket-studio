package com.pocketstudio.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.pillColors

/**
 * The interface language, each named in its own language (French first, the default). Picking
 * one also names the notes the way that language does (Do Ré Mi in French, C D E otherwise).
 */
@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by SettingsStore.state.collectAsState()
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Lang.entries.forEach { lang ->
            FilterChip(
                selected = settings.language == lang,
                onClick = { if (settings.language != lang) SettingsStore.update(context) { it.copy(language = lang, englishNotes = lang != Lang.FR) } },
                label = { Text(lang.nativeName) },
                colors = pillColors(),
                modifier = Modifier.testTag("lang_${lang.code}"),
            )
        }
    }
}
