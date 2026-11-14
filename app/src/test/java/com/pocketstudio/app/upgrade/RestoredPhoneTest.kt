package com.pocketstudio.app.upgrade

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.ProjectOpening
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.AppTheme
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.i18n.Lang
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Audit ultra §5 bis, « réinstallation, changement de téléphone »: Android's cloud backup brings back
 * only what `data_extraction_rules.xml` keeps (the projects' compressed copies, the favourites, the
 * settings), never the sounds. A phone set up again from it: every project opens, the user is told
 * once that the sounds stayed behind, and nothing else is lost (hits, notes, mixer, settings).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class RestoredPhoneTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @After
    fun end() {
        SettingsStore.forgetForTests()
        com.pocketstudio.model.i18n.I18n.lang = Lang.FR
    }

    @Test
    fun theProjectsComeBackWithoutTheirSoundsSaidOnceAndNothingElseLost() {
        // The old phone: today's files (audit-1's), the beat's snare on a sample of its own.
        PhoneFiles.install(app, "audit-1")
        val store = ProjectStore(File(app.filesDir, "projects"), com.pocketstudio.app.projects.backupMirrors(app))
        val beat = store.load("projet-1")!!.let { p ->
            val lanes = p.drums.lanes.toMutableList()
            lanes[1] = lanes[1].copy(sample = PadSample("snare-rec.wav", "Ma caisse"))
            p.copy(drums = p.drums.copy(lanes = lanes)).sanitized()
        }
        File(store.folder("projet-1"), "snare-rec.wav").writeBytes(ByteArray(64))
        store.save("projet-1", beat)
        store.save("notes", store.load("notes")!!)
        assertTrue("the sample was on the old phone", "snare-rec.wav" in File(app.filesDir, "projects/projet-1").list().orEmpty())

        // What the cloud backup holds, alone, on the new phone (the rest of filesDir is not backed up).
        val keep = File(app.filesDir, "projects-backup").listFiles().orEmpty().associate { it.name to it.readBytes() }
        assertEquals(setOf("notes.json.gz", "projet-1.json.gz"), keep.keys)
        app.filesDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
        app.getSharedPreferences("opening", 0).edit().clear().commit()
        File(app.filesDir, "projects-backup").apply { mkdirs() }.let { dir -> keep.forEach { (n, b) -> File(dir, n).writeBytes(b) } }
        CurrentProject.set(app, "projet-1")

        // First opening: both projects back, said once.
        val first = runBlocking { ProjectOpening.prepare(app) }
        assertEquals("projet-1", first.id)
        assertEquals(ProjectOpening.restoredNotice(2), first.notice)
        val p = first.project
        PhoneFiles.assertTheBeat(p, "restored")
        assertNull("the sample's sound is not there: the pad is back on its kit sound", p.allScenes[p.scene].drums.lanes[1].sample)
        // The other project too, with its notes.
        CurrentProject.set(app, "notes")
        val second = runBlocking { ProjectOpening.prepare(app) }
        assertNull("said once only (${second.notice})", second.notice)
        PhoneFiles.assertTheNotes(second.project, "restored")
        // The settings (backed up) are kept.
        SettingsStore.forgetForTests()
        val s = SettingsStore.load(app)
        assertEquals(AppTheme.Night, s.theme)
        assertEquals(Lang.DE, s.language)
        // The app opens on it and « Annuler » works.
        val vm = RhythmViewModel(app).also { shadowOf(android.os.Looper.getMainLooper()).idle() }
        vm.setTempo(90.0)
        vm.session.undo()
        assertEquals(128.0, vm.project.value.tempoBpm, 1e-9)
        assertTrue(ProjectJson.encode(vm.project.value).isNotEmpty())
    }
}
