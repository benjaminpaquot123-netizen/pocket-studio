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
        // Audit ultra round 1, n° 37 (seats A b, D b, D d): the words of the trade the Mode simple still showed bare.
        Term(Regex("""(?i)\bkicks?\b""")) { tr("Kick : la grosse caisse, le « boum » grave de la batterie.") },
        Term(Regex("""(?i)\bcharleys?\b|\bhi-hats?\b""")) { tr("Charley : la petite cymbale qui fait « tss », le tic-tac du rythme.") },
        Term(Regex("""(?i)\btriolets?\b""")) { tr("Triolet : trois coups serrés dans la place de deux.") },
        Term(Regex("""(?i)\broulements?\b""")) { tr("Roulement : des coups très rapides à la suite, comme un tambour qui roule.") },
        Term(Regex("""\bDrops?\b""")) { tr("Drop : le moment fort du morceau, quand tout revient d'un coup.") },
        Term(Regex("""(?i)\bpenta(tonique)?\b""")) { tr("Penta : une gamme de cinq notes, faciles à jouer ensemble.") },
        Term(Regex("""(?i)\bphrygien(ne)?\b""")) { tr("Phrygienne : une gamme sombre, un peu inquiétante.") },
        Term(Regex("""(?i)\bcontretemps\b""")) { tr("Contretemps : entre deux temps, là où on ne tape pas du pied.") },
        Term(Regex("""\b808\b""")) { tr("808 : une basse grave qui sonne comme un kick long.") },
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
    if (!com.pocketstudio.app.settings.observedSettings().simpleMode) return
    val lines = PlainWords.of(*french)
    if (lines.isEmpty()) return
    Column(modifier.testTag("wizard_plain_words"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary) }
    }
}
