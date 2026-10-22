package com.pocketstudio.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.midi.MidiViewModel
import com.pocketstudio.app.onboarding.FirstLaunch
import com.pocketstudio.app.onboarding.WelcomeActions
import com.pocketstudio.app.playback.NotificationAsk
import com.pocketstudio.app.playback.PlaybackService
import com.pocketstudio.app.playback.notificationAsk
import com.pocketstudio.app.playback.playbackNotificationsShown
import com.pocketstudio.app.settings.GuideDialog
import com.pocketstudio.app.settings.GuidePage
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.studio.StudioViewModel
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.ColorblindPalette
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The app shell's small fixes of phase 28 (permissions, settings, welcome, guide, colours, MIDI). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class ShellFixesTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @After
    fun french() {
        SettingsStore.update(app) { it.copy(language = Lang.FR, englishNotes = false) }
    }

    @Test
    fun autoriserOpensTheSettingsOnceAndroidNoLongerAsks() {
        assertEquals(NotificationAsk.Ask, notificationAsk(permitted = false, askedBefore = false, rationale = false))
        assertEquals(NotificationAsk.Ask, notificationAsk(permitted = false, askedBefore = true, rationale = true))
        // Refused twice: Android answers « no » at once, only the settings can change it.
        assertEquals(NotificationAsk.OpenSettings, notificationAsk(permitted = false, askedBefore = true, rationale = false))
        // Allowed but notifications (or the « Lecture » channel) turned off by hand.
        assertEquals(NotificationAsk.OpenSettings, notificationAsk(permitted = true, askedBefore = true, rationale = false))
    }

    @Test
    fun aLectureChannelTurnedOffIsNotDone() {
        assertTrue(playbackNotificationsShown(app))
        app.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(PlaybackService.CHANNEL_ID, "Lecture", NotificationManager.IMPORTANCE_NONE))
        assertFalse(playbackNotificationsShown(app))
    }

    @Test
    fun resetKeepsTheLanguagesNoteNames() {
        SettingsStore.update(app) { it.copy(language = Lang.NL, englishNotes = true, haptics = false) }
        SettingsStore.reset(app)
        assertTrue("C D E in Dutch", SettingsStore.state.value.englishNotes)
        assertTrue(SettingsStore.state.value.haptics)
        SettingsStore.update(app) { it.copy(language = Lang.FR, englishNotes = true) }
        SettingsStore.reset(app)
        assertFalse("Do Ré Mi in French", SettingsStore.state.value.englishNotes)
    }

    @Test
    fun twoQuickTouchesOnTheWelcomeMakeOneProject() {
        var empty = 0
        var demo = 0
        compose.setContent { PocketStudioTheme { FirstLaunch(true, WelcomeActions(empty = { empty++ }, demo = { demo++ }), playDemo = {}) } }
        compose.onNodeWithTag("welcome_empty").performClick()
        compose.onNodeWithTag("welcome_empty").performClick()
        compose.onNodeWithTag("welcome_demo").performClick()
        assertEquals(1, empty)
        assertEquals(0, demo)
    }

    @Test
    fun theGuidesLastPageHasNoEmptySkipButton() {
        compose.setContent { PocketStudioTheme { GuideDialog(onClose = {}, pages = listOf(GuidePage("Un", listOf("a")), GuidePage("Deux", listOf("b")))) } }
        compose.onNodeWithTag("guide_skip").assertExists()
        compose.onNodeWithTag("guide_next").performClick()
        compose.onNodeWithTag("guide_skip").assertDoesNotExist()
    }

    @Test
    fun leavingTheSoundPageStopsTheTestTone() {
        val vm = StudioViewModel()
        vm.togglePlaying()
        assertTrue(vm.state.value.playing)
        vm.stop()
        assertFalse(vm.state.value.playing)
    }

    @Test
    fun smallTextIsReadableOnThePanels() {
        fun contrast(a: Color, b: Color): Double {
            val (hi, lo) = listOf(a.luminance() + 0.05, b.luminance() + 0.05).sortedDescending()
            return hi / lo
        }
        for (p in listOf(Palettes.Studio, ColorblindPalette)) for (panel in listOf(p.surface, p.surfaceHigh)) {
            assertTrue("red text ${contrast(p.badText, panel)}", contrast(p.badText, panel) >= 4.5)
            assertTrue("secondary text ${contrast(p.textSecondary, panel)}", contrast(p.textSecondary, panel) >= 4.5)
        }
    }

    @Test
    fun aBluetoothSearchStopsWhenAnotherProjectOpens() {
        shadowOf(app).grantPermissions(android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT)
        val adapter = app.getSystemService(BluetoothManager::class.java).adapter
        shadowOf(adapter).setState(android.bluetooth.BluetoothAdapter.STATE_ON)
        val store = ViewModelStore()
        val vm = androidx.lifecycle.ViewModelProvider.create(store, androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))[MidiViewModel::class.java]
        vm.scanBluetooth()
        val scanner = shadowOf(adapter.bluetoothLeScanner)
        assertEquals(1, scanner.scanCallbacks.size)
        store.clear()  // another project: the ViewModels go
        assertEquals(0, scanner.scanCallbacks.size)
    }
}
