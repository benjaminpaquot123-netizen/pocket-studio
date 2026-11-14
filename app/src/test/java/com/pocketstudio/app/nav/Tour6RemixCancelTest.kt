package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.remix.SongInfo
import com.pocketstudio.app.remix.StemSet
import com.pocketstudio.app.remix.StemStore
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Audit ultra round 6, U17 (seat G a #34 to #43, Turkish): Remix › « Dans un projet existant » › a project, then
 * « Annuler » on « Mettre … dans mon projet »: the user was left in that project, without the song, far from the Remix
 * page he came from (two more touches to find it again). « Annuler » now leads back to the Remix page of « Que veux-tu
 * faire ? », the song still there to send elsewhere; nothing was put in the project.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6RemixCancelTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @Before
    fun appViewModels() = h.appViewModelsOfThisTest()

    @After
    fun end() {
        h.finish()
        StemStore.dirFor(h.app, SET).parentFile?.deleteRecursively()
    }

    /** A song already separated on the phone. */
    private fun song(): StemSet {
        val dir = StemStore.dirFor(h.app, SET)
        dir.mkdirs()
        listOf("drums", "bass", "other", "vocals").forEach { File(dir, "$it.wav").writeBytes(ByteArray(44)) }
        return StemStore.commit(dir, "Ma chanson", 180.0, SongInfo(bpm = 98.0, firstBeat = 0.1, root = 9, minor = true))
    }

    @Test
    fun cancelOfTheImportIntoAnExistingProjectLeadsBackToTheRemixPage() {
        song()
        h.start(launchChoice = true)
        val open = CurrentProject.get(h.app)
        h.awaitTag("launch_remix")
        shell.tap("launch_remix")
        h.awaitTag("use_$SET")
        assertTrue("the Remix page of « Que veux-tu faire ? »", shell.has("remix_home"))
        shell.tap("use_$SET")
        shell.tap("remix_dest_existing")
        h.awaitTag("remix_dest_project_$open")
        shell.tap("remix_dest_project_$open")
        h.awaitTag("import_cancel")
        shell.tap("import_cancel")
        shell.idle(600)
        assertFalse("the import's choices are closed", shell.has("import_cancel"))
        assertNull("nothing was put in the project", h.vm.project.value.remix.setId)
        assertTrue(
            "« Annuler » left the user in the project (its own Remix page: ${shell.has("audio_page_1")}), not on the Remix page he came from",
            shell.reached(3_000) { shell.has("remix_home") && shell.has("use_$SET") },
        )
    }

    private companion object {
        const val SET = "song-tour6-cancel"
    }
}
