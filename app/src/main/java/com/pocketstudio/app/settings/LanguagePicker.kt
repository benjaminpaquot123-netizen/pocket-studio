package com.pocketstudio.app.settings

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledAlertDialog
import java.util.Locale

/**
 * The interface language: one button with the language in use (its flag and its name), which
 * opens the list of every language, each named in itself (« Deutsch », « 日本語 », « العربية »)
 * with its name in the interface's language underneath, so that anyone finds theirs whatever the
 * screen shows. The list grows with [Lang] alone. Picking one also names the notes the way that
 * language does (Do Ré Mi in French, C D E otherwise).
 */
@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by SettingsStore.state.collectAsState()
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.heightIn(min = 48.dp).testTag("lang_button"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Neon.Outline),
        contentPadding = PaddingValues(start = 12.dp, end = 8.dp),
    ) {
        Text(settings.language.flag, fontSize = 20.sp)
        Text("  " + settings.language.nativeName, fontWeight = FontWeight.Bold, color = Neon.TextPrimary)
        Icon(Icons.Filled.ExpandMore, contentDescription = tr("Choisis ta langue"), tint = Neon.TextSecondary, modifier = Modifier.size(20.dp))
    }
    if (open) {
        LanguageList(
            current = settings.language,
            pick = { lang ->
                open = false
                if (settings.language != lang) SettingsStore.update(context) { it.copy(language = lang, englishNotes = !lang.solfege) }
            },
            close = { open = false },
        )
    }
}

/** The phone's own language when the app has it (null otherwise: no English suggested to a Swedish phone). */
fun phoneLang(): Lang? {
    val locale = Resources.getSystem().configuration.locales[0] ?: return null
    val language = locale.language.let { if (it == "in") "id" else it }
    return Lang.forLocale(locale).takeIf { Locale.forLanguageTag(it.code).language == language }
}

/**
 * Every language of [Lang] in the picker's order ([Lang.PICKER]), scrolling, the one in use
 * outlined in the accent colour and ticked, and the [phone]'s language suggested on top.
 */
@Composable
fun LanguageList(current: Lang, pick: (Lang) -> Unit, close: () -> Unit, phone: Lang? = remember { phoneLang() }) {
    ScaledAlertDialog(
        onDismissRequest = close,
        title = { Text(tr("Choisis ta langue")) },
        text = {
            // Opened on the language in use (15 do not fit at once); the phone's block is 3 items.
            val top = if (phone != null) 3 else 1
            val first = if (current == phone) 0 else (top + Lang.PICKER.indexOf(current) - 2).coerceAtLeast(0)
            val list = rememberLazyListState(initialFirstVisibleItemIndex = first)
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 460.dp).testTag("lang_list"),
                state = list,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (phone != null) {
                    item(key = "h_phone") { SectionLabel(tr("Langue du téléphone")) }
                    item(key = "phone") { LanguageRow(phone, phone == current, "lang_suggested") { pick(phone) } }
                }
                item(key = "h_all") { SectionLabel(tr("Toutes les langues")) }
                items(Lang.PICKER, key = { it.code }) { lang -> LanguageRow(lang, lang == current, "lang_${lang.code}") { pick(lang) } }
            }
        },
        confirmButton = { TextButton(onClick = close, modifier = Modifier.testTag("lang_close")) { Text(tr("Fermer")) } },
        modifier = Modifier.testTag("lang_dialog"),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(I18n.lang.locale),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = Neon.TextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

/** One language: its flag in a soft tile, its own name in bold, its name in the interface's language underneath. */
@Composable
private fun LanguageRow(lang: Lang, chosen: Boolean, tag: String, onPick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val translated = lang.nameIn(I18n.lang)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape)
            // Lit from above, as every panel (lot « design pro »).
            .background(
                if (chosen) Brush.verticalGradient(listOf(Neon.Cyan.copy(alpha = 0.17f), Neon.Cyan.copy(alpha = 0.08f)))
                else Brush.verticalGradient(listOf(Neon.SurfaceHigh.copy(alpha = 0.62f), Neon.SurfaceHigh.copy(alpha = 0.4f))),
            )
            .border(if (chosen) 1.5.dp else 1.dp, if (chosen) Neon.Cyan else Neon.Outline.copy(alpha = 0.4f), shape)
            .selectable(selected = chosen, role = Role.RadioButton, onClick = onPick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            // The flag in a slight hollow: darker at the top, a faint light edge.
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(lerp(Neon.Surface, Color.Black, 0.14f), Neon.Surface)))
                .border(1.dp, Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.08f))), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(lang.flag, fontSize = 24.sp) }
        Column(Modifier.weight(1f)) {
            // Each name in its own script and direction, whatever the interface's.
            Text(
                lang.nativeName,
                style = MaterialTheme.typography.titleMedium.copy(
                    localeList = LocaleList(lang.code),
                    textDirection = if (lang.rtl) TextDirection.Rtl else TextDirection.Ltr,
                ),
                fontWeight = FontWeight.Bold,
                color = if (chosen) Neon.Cyan else Neon.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!translated.equals(lang.nativeName, ignoreCase = true)) {
                Text(translated, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (chosen) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(24.dp))
    }
}
