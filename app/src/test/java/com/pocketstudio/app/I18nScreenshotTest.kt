package com.pocketstudio.app

import com.pocketstudio.model.i18n.I18n
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
    override fun wanted(name: String) = name in SCREENS

    @Before
    fun language() {
        I18n.lang = lang
    }

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    companion object {
        val SCREENS = setOf("session", "top_bar", "welcome", "rhythm")
    }
}

/** The same screens in Dutch. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class I18nDutchScreenshotTest : I18nScreenshotTest() {
    override val lang = Lang.NL
}
