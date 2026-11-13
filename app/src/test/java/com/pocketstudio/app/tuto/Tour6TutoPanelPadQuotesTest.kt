package com.pocketstudio.app.tuto

import com.pocketstudio.app.rhythm.padName
import com.pocketstudio.model.PadVoice
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tour 6, T2 (the code's part): [tutoPadNames] renamed a pad only when the translated step quoted the kit's name in
 * that language word for word; a translation that cites the pad by another word (« 「バスドラム」 », « “베이스 드럼” »)
 * kept a name no pad shows. The pad the French step names is said by its name on the screen, in every language.
 */
@RunWith(RobolectricTestRunner::class)
class Tour6TutoPanelPadQuotesTest {
    private val fr = "À toi : sur le pad « Kick », touche les 4 cases en pointillé : un coup sur chaque temps."

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    private fun shown(lang: Lang, translated: String, other: String) {
        I18n.lang = lang
        val target = TutoTarget.of("techno")!!
        // The kick pad holds a studio voice: its name on the screen is not the kit's.
        val p = target.blank().let { it.copy(padVoices = it.padVoices + (0 to PadVoice(PadVoice.KICK_STUDIO))) }
        val pad = padName(p, 0)
        val words = tutoPadNames(tutoKitQuotes(fr, translated), p)
        assertTrue("$lang: the pad « $pad » is not named: $words", pad in words)
        assertFalse("$lang: « $other », a name no pad shows: $words", other in words)
    }

    @Test
    fun japanese() = shown(Lang.JA, "あなたの番：「バスドラム」パッドで、点線の4マスをタップしよう：毎拍に1発。", "バスドラム")

    @Test
    fun korean() = shown(Lang.KO, "직접 해 보세요: “베이스 드럼” 패드에서 점선 칸 4개를 탭하세요. 박마다 한 번이에요.", "베이스 드럼")
}
