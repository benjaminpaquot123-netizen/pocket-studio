package com.pocketstudio.app

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.mixer.EffectsBox
import com.pocketstudio.app.mixer.FxGenres
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.mixer.QuickFxActions
import com.pocketstudio.app.mixer.QuickFxPanel
import com.pocketstudio.app.mixer.insertRowCompositions
import com.pocketstudio.app.mixer.meterLevel
import com.pocketstudio.app.mixer.miniCurve
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.Insert
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.withFx
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The effects box: lights and mini views, hold to listen, favourites, styles. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class FxBrowseTest {
    @get:Rule
    val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private val dubDeep = FxPresets.byId("dub_deep")!!

    @Test
    fun holdingAnAmbienceListensAndLettingGoComesBack() {
        val calls = mutableListOf<String>()
        val actions = QuickFxActions(applyFx = { _, id -> calls += "apply $id" }, audition = { ch, id -> calls += "listen $ch $id" })
        compose.setContent { PocketStudioTheme { QuickFxPanel(2, Channel(), actions) } }
        compose.onNodeWithTag("fx_category_Ambiance").performClick()
        compose.onNodeWithTag("fx_preset_cathedral").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithTag("fx_auditioning").assertIsDisplayed()
        compose.onNodeWithTag("fx_preset_cathedral").performTouchInput { up() }
        // Heard while held, then the saved sound again; never chosen.
        assertEquals(listOf("listen 2 cathedral", "listen 2 null"), calls)
        assertFalse(exists("fx_auditioning"))
        // A quick touch still chooses it.
        calls.clear()
        compose.onNodeWithTag("fx_preset_cathedral").performTouchInput { down(center); up() }
        assertEquals(listOf("apply cathedral"), calls)
    }

    @Test
    fun aStarKeepsAnAmbienceInFavourites() {
        val favourites = MutableStateFlow(emptySet<String>())
        val actions = QuickFxActions(favourites = favourites, toggleFavourite = { id -> favourites.value = favourites.value.let { if (id in it) it - id else it + id } })
        compose.setContent { PocketStudioTheme { QuickFxPanel(2, Channel().withFx(dubDeep), actions) } }
        compose.onNodeWithTag("fx_category_favourites").performClick()
        compose.onNodeWithTag("fx_favourites_none").assertIsDisplayed()
        compose.onNodeWithTag("fx_favourite").performClick()
        assertEquals(setOf("dub_deep"), favourites.value)
        compose.waitForIdle()
        assertFalse(exists("fx_favourites_none"))
        compose.onNodeWithTag("fx_preset_dub_deep").assertIsDisplayed()
        assertFalse(exists("fx_preset_cathedral"))
        compose.onNodeWithTag("fx_favourite").performClick()
        assertEquals(emptySet<String>(), favourites.value)
    }

    @Test
    fun aStyleShowsTheAmbiencesThatSuitIt() {
        compose.setContent { PocketStudioTheme { QuickFxPanel(2, Channel(), QuickFxActions()) } }
        compose.onNodeWithTag("fx_genre_${FxGenres.LOFI}").performScrollTo().performClick()
        compose.onNodeWithTag("fx_preset_lofi_tape").assertIsDisplayed()
        assertFalse(exists("fx_preset_edm_pump"))
        compose.onNodeWithTag("fx_genre_${FxGenres.DUBSTEP}").performScrollTo().performClick()
        compose.onNodeWithTag("fx_preset_wobble").assertIsDisplayed()
        // Every style has something, every tagged preset exists.
        FxGenres.ALL.forEach { assertTrue(it, FxGenres.presets(it).isNotEmpty()) }
    }

    @Test
    fun favouritesAndTheBeatOptionAreKeptOnThePhone() {
        val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        SettingsStore.forgetForTests()
        val fresh = SettingsStore.load(app)
        assertTrue(fresh.favouriteFx.isEmpty())
        assertFalse(fresh.liveBeatHaptics)
        SettingsStore.toggleFavouriteFx(app, "dub_deep")
        SettingsStore.toggleFavouriteFx(app, "rumble")
        SettingsStore.toggleFavouriteFx(app, "rumble")
        SettingsStore.update(app) { it.copy(liveBeatHaptics = true) }
        assertEquals(setOf("dub_deep"), SettingsStore.favouriteFx.value)
        // As after a new start of the app.
        SettingsStore.forgetForTests()
        val again = SettingsStore.load(app)
        assertEquals(setOf("dub_deep"), again.favouriteFx)
        assertEquals(setOf("dub_deep"), SettingsStore.favouriteFx.value)
        assertTrue(again.liveBeatHaptics)
        // « Revenir aux réglages d'origine » keeps the favourites.
        SettingsStore.reset(app)
        assertEquals(setOf("dub_deep"), SettingsStore.state.value.favouriteFx)
        assertFalse(SettingsStore.state.value.liveBeatHaptics)
        prefs.edit().clear().commit()
        SettingsStore.forgetForTests()
    }

    @Test
    fun listeningChangesNothingInTheProject() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        val before = vm.project.value
        vm.auditionFx(2, "cathedral")
        assertEquals(2 to "cathedral", vm.auditioning.value)
        assertEquals(before, vm.project.value)
        vm.auditionFx(2, null)
        assertNull(vm.auditioning.value)
        assertEquals(before, vm.project.value)
        // Nothing to listen to on the master or an unknown preset.
        vm.auditionFx(Mixer.MASTER, "cathedral")
        vm.auditionFx(2, "nope")
        assertNull(vm.auditioning.value)
    }

    @Test
    fun theLightsFollowTheSoundWithoutRecomposingTheBox() {
        val meters = MutableStateFlow(FloatArray(Mixer.CHANNELS + 1))
        val channel = Channel(inserts = listOf(Insert(EffectType.ECHO.ordinal, EffectType.ECHO.defaults), Insert(EffectType.DISTORTION.ordinal, EffectType.DISTORTION.defaults)) + List(Mixer.INSERTS - 2) { Insert() })
        compose.mainClock.autoAdvance = false
        compose.setContent { PocketStudioTheme { EffectsBox(2, channel.sanitized(), MixerActions(meters = meters)) } }
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        assertTrue(exists("fx_activity_2_0") && exists("fx_activity_2_1"))
        val rows = insertRowCompositions
        assertTrue(rows > 0)
        repeat(20) { i ->
            meters.value = FloatArray(Mixer.CHANNELS + 1) { if (it == 2) 0.05f * i else 0f }
            compose.mainClock.advanceTimeBy(50)
        }
        compose.waitForIdle()
        assertEquals(rows, insertRowCompositions)
    }

    @Test
    fun eachEffectHasAMiniViewFromItsSettings() {
        EffectType.entries.forEach { type ->
            val c = miniCurve(Insert(type.ordinal, type.defaults))
            assertTrue(type.name, c.all { it.isFinite() && it in -1f..1f })
        }
        // A low-pass and a high-pass do not look alike; more echo feedback, longer taps.
        val lp = miniCurve(Insert(EffectType.FILTER.ordinal, listOf(0.5f, 0.2f, 0f, 1f)))
        val hp = miniCurve(Insert(EffectType.FILTER.ordinal, listOf(0.5f, 0.2f, 1f, 1f)))
        assertTrue(lp.first() > lp.last() && hp.first() < hp.last())
        val short = miniCurve(Insert(EffectType.ECHO.ordinal, listOf(0.4f, 0.1f, 0.5f, 0.3f)))
        val long = miniCurve(Insert(EffectType.ECHO.ordinal, listOf(0.4f, 0.9f, 0.5f, 0.3f)))
        assertTrue(long.sum() > short.sum())
        // The light: -48 dB dark, 0 dB full.
        assertEquals(0f, meterLevel(0f))
        assertEquals(1f, meterLevel(1f))
        assertEquals(0.5f, meterLevel(0.0631f), 0.01f)
    }
}
