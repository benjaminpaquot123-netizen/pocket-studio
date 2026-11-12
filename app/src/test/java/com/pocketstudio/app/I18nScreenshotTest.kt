package com.pocketstudio.app

import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A few screens in English (app/screenshots/i18n/en_*.png), to check that the translated texts
 * fit their buttons and chips. The Dutch ones (usually the longest) follow below.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class I18nScreenshotTest : ScreenshotTest() {
    protected open val lang = Lang.EN
    override val prefix get() = "i18n/${lang.code}_"
    override val rtl get() = lang.rtl
    override fun wanted(name: String) = name in SCREENS

    @Before
    fun language() {
        // As the language picker does: every language but French names notes C D E.
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = lang, englishNotes = true) }
    }

    /** Audit stage 1: nothing cut nor drawn over another text, read on the layout ([com.pocketstudio.app.audit.LayoutScan]). */
    override fun inspect(name: String) {
        val density = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        val found = com.pocketstudio.app.audit.LayoutScan.scan(compose, density, com.pocketstudio.app.audit.CaptureMatrixTest.LAYOUT)
            .filterNot { com.pocketstudio.app.audit.Allowed.matches(name, it) }
        org.junit.Assert.assertTrue("$prefix$name:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @After
    fun french() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
    }

    companion object {
        val SCREENS = setOf(
            "session", "top_bar", "welcome", "rhythm", "chord_pads", "piano_roll", "live_pads", "projects", "sampled_instrument", "synth_landscape",
            "play_strip", "tempo_sheet",
        )
    }
}

/** The same screens in Dutch. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class I18nDutchScreenshotTest : I18nScreenshotTest() {
    override val lang = Lang.NL
}

/**
 * Four of the new languages on the screens a newcomer meets (app/screenshots/i18n/<code>_*.png):
 * German (long words), Arabic (right to left), Japanese (CJK), Hindi (Devanagari, tall marks).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class I18nGermanScreenshotTest : I18nScreenshotTest() {
    override val lang = Lang.DE
    override fun wanted(name: String) = name in NEW_SCREENS

    companion object {
        val NEW_SCREENS = setOf("welcome", "guide", "rhythm", "mixer", "live_fx", "settings", "language_list", "session")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class I18nArabicScreenshotTest : I18nGermanScreenshotTest() {
    override val lang = Lang.AR
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class I18nJapaneseScreenshotTest : I18nGermanScreenshotTest() {
    override val lang = Lang.JA
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class I18nHindiScreenshotTest : I18nGermanScreenshotTest() {
    override val lang = Lang.HI
}
