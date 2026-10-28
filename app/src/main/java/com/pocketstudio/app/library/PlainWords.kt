package com.pocketstudio.app.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * Remix testers (Sarah, Mode simple): the assistant's texts say « two-step », « hoover », « Sub »,
 * « octave », « BPM »… The words stay (they are the style's own), and in the Mode simple each one
 * found in a step's texts gets a plain line under them. The texts are looked at in French (the
 * keys), so the same words are explained whatever the language shown.
 */
internal object PlainWords {
    /** A word of the trade as it appears in the French texts, and its explanation. */
    private class Term(val pattern: Regex, val plain: () -> String)

    private val TERMS = listOf(
        Term(Regex("""\bBPM\b""")) { tr("BPM : la vitesse du morceau, en battements par minute.") },
        Term(Regex("""(?i)\btwo-step\b""")) { tr("Two-step : le nom de ce rythme de batterie, deux coups par mesure.") },
        Term(Regex("""(?i)\bhoovers?\b""")) { tr("Hoover : un gros synthé qui hurle, le son des raves.") },
        Term(Regex("""(?i)\bsub\b""")) { tr("Sub : la basse la plus grave, celle qu'on sent dans le ventre.") },
        Term(Regex("""(?i)\boctaves?\b""")) { tr("Octave : la même note, plus grave ou plus aiguë.") },
    )

    /** The plain lines for the words of the trade in [french] (French keys), each once, in the list's order. */
    fun of(vararg french: String?): List<String> {
        val texts = french.filterNotNull()
        return TERMS.filter { t -> texts.any { t.pattern.containsMatchIn(it) } }.map { it.plain() }
    }
}

/** The plain lines of [PlainWords.of], in the Mode simple only. */
@Composable
internal fun PlainWordsLines(vararg french: String?, modifier: Modifier = Modifier) {
    if (!com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode) return
    val lines = PlainWords.of(*french)
    if (lines.isEmpty()) return
    Column(modifier.testTag("wizard_plain_words"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary) }
    }
}
