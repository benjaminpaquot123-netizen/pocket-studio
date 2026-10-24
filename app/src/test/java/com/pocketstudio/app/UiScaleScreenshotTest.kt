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
 * Réglages › Taille de l'interface 130 % on a 411 dp phone: 316 dp of room, like a 316 dp phone
 * (and a 360 dp phone at 115 %). The key screens in French (app/screenshots/scale130/fr_*.png),
 * then in Dutch (longest texts): nothing may vanish (REC on the Live page) or be cut mid-word.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
open class UiScaleScreenshotTest : ScreenshotTest() {
    protected open val lang = Lang.FR
    override val uiScale = 1.3f
    override val prefix get() = "scale130/${lang.code}_"
    override fun wanted(name: String) = name in SCREENS

    // Shot taller on the phone; here at the same 411 dp width.
    @org.junit.Test @Config(qualifiers = "w411dp-h1700dp-xxhdpi") override fun session() = super.session()
    @org.junit.Test @Config(qualifiers = "w411dp-h1300dp-xxhdpi") override fun settings() = super.settings()

    @Before
    fun language() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = lang, englishNotes = lang != Lang.FR) }
    }

    @After
    fun french() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
    }

    companion object {
        val SCREENS = setOf(
            "live_grid", "live_fx", "live_rec", "live_pads", "rhythm", "top_bar", "mixer", "synth", "session", "library",
            "pad_sound", "effects_box", "welcome", "projects", "acid", "audio_tracks", "next_steps", "settings",
            "play_strip", "tempo_sheet", "mixer_master",
            // Phase 38.
            "real_instruments", "export_report", "track_add",
        )
    }
}

/** The same screens in Dutch. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class UiScaleDutchScreenshotTest : UiScaleScreenshotTest() {
    override val lang = Lang.NL
}
