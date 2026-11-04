package com.pocketstudio.app.upgrade

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Audit ultra §5 bis, « premier lancement réel »: the bench forces French; a real first start takes the
 * phone's language. For each of the 15 languages and the awkward neighbours (pt-PT, zh-TW, en-IN, in-ID,
 * fr-BE, de-AT, es-MX, a language the app has not: sw-KE), the phone set to it, nothing chosen yet: the
 * language MainActivity adopts from the phone's configuration, then the first screen (« Bienvenue ! »)
 * in it, nothing of it left in French.
 * (Notifications, battery saving and the first model download are asked on a real phone: emulator, `audit-emulator.sh`.)
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class FirstLaunchTest(private val phone: String, private val expected: String) {
    companion object {
        /** The phone's language (Robolectric's qualifier « b+… ») and the interface it gets. */
        val CASES: List<Pair<String, Lang>> =
            Lang.entries.map { (if (it.code.contains('-')) it.code else it.code + "-" + defaultCountry(it)) to it } + listOf(
                "pt-PT" to Lang.PT_BR, "zh-TW" to Lang.ZH_CN, "en-IN" to Lang.EN, "in-ID" to Lang.ID,
                "fr-BE" to Lang.FR, "de-AT" to Lang.DE, "es-MX" to Lang.ES, "sw-KE" to Lang.EN,
            )

        private fun defaultCountry(l: Lang) = when (l) {
            Lang.EN -> "GB"; Lang.HI -> "IN"; Lang.AR -> "SA"; Lang.JA -> "JP"; Lang.KO -> "KR"; else -> l.code.uppercase()
        }

        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = CASES.map { arrayOf<Any>(it.first, it.second.code) }

        /** A locale tag as Robolectric's resource qualifiers write it (« b+pt+PT »). */
        fun qualifier(tag: String) = "b+" + tag.replace('-', '+')
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    @Test
    fun theFirstScreenIsInThePhonesLanguage() {
        RuntimeEnvironment.setQualifiers("+" + qualifier(phone))
        val app = h.app
        app.getSharedPreferences("settings", 0).edit().clear().commit()
        SettingsStore.forgetForTests()
        // As MainActivity does at its creation.
        SettingsStore.adoptPhoneLanguage(app, app.resources.configuration.locales[0])
        val lang = SettingsStore.state.value.language
        assertEquals("$phone: the interface's language", expected, lang.code)
        assertEquals("$phone: the notes named as the language does", !lang.solfege, SettingsStore.state.value.englishNotes)
        // Then the app's first screen, in it.
        h.start(firstLaunch = true, lang = lang)
        compose.onNodeWithTag("welcome").assertExists()
        assertEquals(lang, I18n.lang)
        val first = I18n.t("Créer mon premier morceau (guidé)")
        assertTrue("$phone: « $first » on the first screen", compose.onAllNodesWithText(first).fetchSemanticsNodes().isNotEmpty())
        if (lang != Lang.FR) {
            for (fr in listOf("Créer mon premier morceau (guidé)", "J'apprends", "Partir de zéro", "Écouter une démo")) {
                assertTrue("$phone: « $fr » left in French", compose.onAllNodesWithText(fr).fetchSemanticsNodes().isEmpty())
            }
        }
    }
}
