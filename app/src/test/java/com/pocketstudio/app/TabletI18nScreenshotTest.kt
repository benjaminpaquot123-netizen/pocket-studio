package com.pocketstudio.app

import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The tablet in English and Dutch (a guitarist's way through the app): the synth's chord pads held
 * upright, the tuner and the song lying down. Images in app/screenshots/tablet/{en,land_en,land_nl}_*.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class TabletEnglishScreenshotTest : ScreenshotTest() {
    override val prefix = "tablet/en_"
    override fun wanted(name: String) = name in setOf("synth_chord_pads", "wizard_style")

    @Before
    fun english() {
        I18n.lang = Lang.EN
        // Picking a language other than French turns C D E note names on, as in the app.
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.EN, englishNotes = true) }
    }

    @After
    fun french() {
        I18n.lang = Lang.FR
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class TabletLandscapeEnglishScreenshotTest : ScreenshotTest() {
    override val prefix = "tablet/land_en_"
    override fun wanted(name: String) = name in setOf("tuner", "synth")

    @Before
    fun english() {
        I18n.lang = Lang.EN
        // Picking a language other than French turns C D E note names on, as in the app.
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.EN, englishNotes = true) }
    }

    @After
    fun french() {
        I18n.lang = Lang.FR
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class TabletLandscapeDutchScreenshotTest : ScreenshotTest() {
    override val prefix = "tablet/land_nl_"
    override fun wanted(name: String) = name in setOf("session", "rhythm")

    // Shot at the phone's own (taller) size in ScreenshotTest; here at the tablet's size.
    @Test override fun session() = super.session()

    @Before
    fun dutch() {
        I18n.lang = Lang.NL
        // Picking a language other than French turns C D E note names on, as in the app.
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.NL, englishNotes = true) }
    }

    @After
    fun french() {
        I18n.lang = Lang.FR
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
    }
}
