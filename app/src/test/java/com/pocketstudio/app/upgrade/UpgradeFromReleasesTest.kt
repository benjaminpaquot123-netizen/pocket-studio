package com.pocketstudio.app.upgrade

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.ProjectOpening
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.AppTheme
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.Lang
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Audit ultra §5 bis, « mise à jour depuis chaque ancienne version »: the files five published versions
 * left on a phone (the first with projects v0.1.23, v0.9.95, the last two APKs sent v0.22.173 and
 * v0.22.175, `audit-1`), each written by that version's own model code (scripts/upgrade-fixtures/), opened
 * by today's app: each project opens as it was (nothing told, nothing set aside), the settings are kept,
 * « Annuler » works on it, and saved again in today's format it reads back the same.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class UpgradeFromReleasesTest(private val tag: String) {
    companion object {
        val TAGS = listOf("v0.1.23", "v0.9.95", "v0.22.173", "v0.22.175", "audit-1")

        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun tags(): List<Array<Any>> = TAGS.map { arrayOf<Any>(it) }
    }

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @After
    fun end() {
        SettingsStore.forgetForTests()
        com.pocketstudio.model.i18n.I18n.lang = Lang.FR
        com.pocketstudio.model.i18n.I18n.everyday = false
    }

    private fun open(id: String): RhythmViewModel {
        CurrentProject.set(app, id)
        val opened = runBlocking { ProjectOpening.prepare(app) }
        assertEquals("$tag $id: the project opened", id, opened.id)
        assertNull("$tag $id: nothing to tell (${opened.notice})", opened.notice)
        return RhythmViewModel(app).also { shadowOf(android.os.Looper.getMainLooper()).idle() }
    }

    @Test
    fun everyProjectOpensAsItWasWithItsSettings() {
        PhoneFiles.install(app, tag)
        SettingsStore.forgetForTests()
        val s = SettingsStore.load(app)
        if (tag != "v0.1.23") {
            assertEquals("$tag: theme", AppTheme.Night, s.theme)
            assertEquals("$tag: size", 1.15f, s.uiScale)
            assertEquals("$tag: metronome", 0.4f, s.metronomeLevel)
            assertEquals("$tag: count-in", 2, s.recordCountIn)
            assertTrue("$tag: the welcome already seen", s.guideSeen)
            assertTrue("$tag: notes in C D E", s.englishNotes)
        }
        if (tag !in listOf("v0.1.23", "v0.9.95")) assertEquals("$tag: language", Lang.DE, s.language)
        val ids = File(app.filesDir, "projects").list().orEmpty().sorted()
        assertEquals("$tag: the projects", if (tag == "v0.1.23") listOf("projet-1") else listOf("notes", "projet-1"), ids)
        for (id in ids) {
            val vm = open(id)
            val p = vm.project.value
            if (id == "notes") PhoneFiles.assertTheNotes(p, "$tag $id") else PhoneFiles.assertTheBeat(p, "$tag $id")
            // « Annuler » on the project read.
            vm.setTempo(140.0)
            assertEquals(140.0, vm.project.value.tempoBpm, 1e-9)
            vm.session.undo()
            assertEquals("$tag $id: undone", p.tempoBpm, vm.project.value.tempoBpm, 1e-9)
            // Saved in today's format, read back the same.
            runBlocking { vm.flushNow() }
            val again = ProjectOpening.projectStore(app).load(id)!!
            if (id == "notes") PhoneFiles.assertTheNotes(again, "$tag $id saved again") else PhoneFiles.assertTheBeat(again, "$tag $id saved again")
        }
    }
}
