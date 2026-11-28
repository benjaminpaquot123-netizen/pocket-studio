package com.pocketstudio.app.export

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Tour 9, lot L16: the share window's words (finitions F089, F099). */
class Tour9L16ShareTextsTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    /**
     * F089 (seat t9-40, fr): the window titled « C'est prêt » opened on « C'est prêt : <file>… ». In every language,
     * its first line does not say the title again.
     */
    @Test
    fun theReadyLineDoesNotRepeatTheTitle() {
        val wrong = Lang.entries.flatMap { l ->
            I18n.lang = l
            listOf(true, false).mapNotNull { inMusic ->
                val (title, lines) = shareBack(listOf(shareReadyText(listOf(File("a.m4a")), inMusic)), listOf("a.m4a"), returned = false, picked = false)
                "${l.code}: « $title » / « ${lines[0]} »".takeIf { lines[0].contains(title, ignoreCase = true) }
            }
        }
        assertTrue("first lines repeating the title (${wrong.size}):\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }

    /** F099 (seat t9-57, tr): « OK » stayed in English under Turkish texts. No screen writes the button's word by hand. */
    @Test
    fun noScreenWritesOkByHand() {
        val ok = Regex("""Text\(\s*"OK"\s*[,)]""")
        val wrong = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, line -> "${f.path}:${i + 1}".takeIf { ok.containsMatchIn(line) } }
        }.toList()
        assertTrue("Text(\"OK\") written by hand (${wrong.size}):\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }
}
