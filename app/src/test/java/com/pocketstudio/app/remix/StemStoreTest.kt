package com.pocketstudio.app.remix

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.projectCatalog
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.separation.Demucs
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class StemStoreTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun clean() {
        listOf("stems", "projects", "projects-trash").forEach { File(app.filesDir, it).deleteRecursively() }
        StemStore.pin(app, null)
    }

    private fun separated(key: String): StemSet {
        val dir = StemStore.dirFor(app, key).apply { mkdirs() }
        Demucs.STEMS.forEach { File(dir, "$it.wav").writeText("pcm") }
        Thread.sleep(3)  // distinct creation times
        return StemStore.commit(dir, "Chanson $key", 60.0, SongInfo())
    }

    @Test
    fun separatingMoreSongsNeverDeletesStemsAProjectUses() {
        val sets = (1..6).map { separated("s$it") }
        val catalog = projectCatalog(app)
        catalog.create(Project(name = "Mon remix", remix = Remix(setId = sets[0].id)))
        catalog.delete(catalog.create(Project(name = "Jeté", remix = Remix(setId = sets[1].id))))
        StemStore.pin(app, sets[2].id)  // the open project, maybe not saved yet
        StemStore.prune(app)
        // The three newest, plus every set in use (saved, trashed or open project).
        assertEquals(setOf("s1", "s2", "s3", "s4", "s5", "s6"), StemStore.list(app).map { it.id }.toSet())
        StemStore.pin(app, null)
        separated("s7")
        StemStore.prune(app)
        assertEquals(setOf("s1", "s2", "s5", "s6", "s7"), StemStore.list(app).map { it.id }.toSet())
        assertEquals(listOf("Mon remix"), StemStore.list(app).first { it.id == "s1" }.usedBy)
    }

    @Test
    fun aSetInUseIsNotDeleted() {
        val used = separated("a")
        val free = separated("b")
        projectCatalog(app).create(Project(name = "Mon remix", remix = Remix(setId = used.id)))
        assertFalse(StemStore.delete(app, used))
        assertTrue(used.dir.isDirectory)
        assertTrue(StemStore.delete(app, free))
        assertFalse(free.dir.exists())
    }

    @Test
    fun deletingAskedFirstAndRefusedWhileAProjectUsesIt() {
        val free = StemSet(File(app.filesDir, "stems/libre"), "Libre", 60.0, 2L)
        val used = StemSet(File(app.filesDir, "stems/pris"), "Prise", 60.0, 1L, usedBy = listOf("Mon remix"))
        val deleted = mutableListOf<String>()
        compose.setContent {
            PocketStudioTheme {
                RemixScreen(RemixUi(true, SeparationState.Idle, listOf(free, used)), RemixActions(delete = { deleted += it.id }))
            }
        }
        compose.onNodeWithTag("delete_pris").performScrollTo().performClick()
        compose.onNodeWithText("Morceau utilisé").assertExists()
        compose.onNodeWithTag("delete_stems_ok").performClick()
        assertTrue(deleted.isEmpty())

        compose.onNodeWithTag("delete_libre").performScrollTo().performClick()
        assertTrue(deleted.isEmpty())  // one tap only asks
        compose.onNodeWithTag("delete_stems_confirm").performClick()
        assertEquals(listOf("libre"), deleted)
    }
}
