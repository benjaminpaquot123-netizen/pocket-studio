package com.pocketstudio.app.human

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.onboarding.LaunchRule
import com.pocketstudio.app.rhythm.ClosingSaves
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.I18n
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner

/**
 * The full suite (CI, 2026-11-10: three bench tests red at BenchPristine.kt:54, each the first of the bench run after the
 * app's ordinary tests). A test that does not carry [BenchPristineRule] leaves what the app's tests leave (59 classes of
 * them, seen in a full run): the ViewModel factory that keeps its Application (any screen opened), a save still closing
 * (a project's screen closed), settings changed, the launch rule off. The test after it must start on a pristine process.
 *
 * On purpose without the rule: this class is the ordinary test. Its two tests run in the order of their names.
 */
@RunWith(RobolectricTestRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class OrdinaryTestsLeaveNothingTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Test
    fun a_anOrdinaryTestLeavesWhatTheAppsTestsLeave() {
        ViewModelProvider.AndroidViewModelFactory.getInstance(app)  // as any screen with a ViewModel (603 times in a full run)
        ClosingSaves.launch { delay(400) }  // as a project's screen closed with edits to write (GuideAloneTest, SimpleMixTest...)
        SettingsStore.update(app) { it.copy(simpleMode = false, guideSeen = true) }  // as ShellFixesTest, OnboardingUiTest
        I18n.everyday = true  // as OnboardingUiTest
        LaunchRule.askAtStartForTest = false  // as SpacesRestoreTest
        val left = BenchPristine.leaks().joinToString("; ")
        for (what in listOf("AndroidViewModelFactory._instance", "ClosingSaves busy", "SettingsStore.state", "I18n.everyday", "LaunchRule.askAtStartForTest")) {
            assertTrue("this test must leave $what for the next one to be a proof: $left", what in left)
        }
    }

    @Test
    fun b_theTestAfterAnOrdinaryTestStartsPristine() {
        assertEquals("left by the ordinary test before", emptyList<String>(), BenchPristine.leaks())
    }
}
