package com.pocketstudio.model.guide

import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.SongVersion
import com.pocketstudio.model.restoreVersion
import com.pocketstudio.model.withVersionKept
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** The Guide's state lives in the project (docs/PLAN-GUIDE.md §4.1): saved, exported, imported, kept by a version restore. */
class GuideStateTest {
    private val state = GuideState(
        parcours = "jump_up", parcoursVersion = 1, step = "fx.bass",
        cards = mapOf("frame.style" to GuideCardState(GuideStatus.DONE, 1000L, "keep"), "fx.sub" to GuideCardState(GuideStatus.LATER, 2000L)),
        refusals = listOf(GuideRefusal("mix.pans", "", 3000L), GuideRefusal("fx.lead", "dry", 3100L)),
        history = listOf(GuideChange("inst:3", GuideChange.INSERT, "${GuideActions.SEND}0", JsonPrimitive(0.5f), JsonPrimitive(0.7f), 4000L, GuideChange.GUIDE, "fx.sub")),
        notes = listOf(GuideNote("Pas de réverbe sur la basse, c'est voulu.", 5000L, "inst:0")),
        seen = mapOf("inst:0" to 42),
        measures = GuideMeasures(6000L, -8.2f, -1.1f, mapOf("PEAK" to -7.5f, "BREAK" to -13f)),
        lastUsed = 6000L,
    )
    private val project = Project(name = "Guidé", song = listOf(SongPart(0, 2)), guide = state)

    @Test
    fun anEmptyStateIsLeftOutOfTheFileAndAFullOneReadsBackTheSame() {
        assertFalse("\"guide\"" in ProjectJson.encode(Project()))
        assertTrue(GuideState().isEmpty)
        val text = ProjectJson.encode(project)
        assertTrue("\"guide\"" in text)
        assertEquals(state, ProjectJson.decode(text).guide)
    }

    @Test
    fun theStateReadsTolerantly() {
        // A newer app's state: an unknown key, an unknown status, a card id too long, a negative date.
        val text = ProjectJson.encode(project).replace("\"status\":\"LATER\"", "\"status\":\"ARCHIVED\"").replace("\"step\":", "\"future\":1,\"step\":")
        val read = ProjectJson.decode(text).guide
        assertEquals(GuideStatus.TODO, read.cards.getValue("fx.sub").status)
        assertEquals("fx.bass", read.step)
        val damaged = state.copy(cards = state.cards + ("x".repeat(200) to GuideCardState(GuideStatus.DONE, -5)), notes = state.notes + GuideNote("  "), lastUsed = -1)
        val clean = damaged.sanitized()
        assertEquals(state.cards.keys, clean.cards.keys)
        assertEquals(state.notes, clean.notes)
        assertEquals(0L, clean.lastUsed)
        assertTrue(state.sanitized() === state)
    }

    @Test
    fun theStateGoesThroughASaveAZipAndAnImport() {
        val dir = Files.createTempDirectory("guide").toFile()
        try {
            val catalog = ProjectCatalog(File(dir, "projects"), File(dir, "trash"))
            val id = catalog.create(project)
            assertEquals(state, catalog.store.load(id)!!.guide)
            val bytes = java.io.ByteArrayOutputStream().also { catalog.exportZip(id, it) }.toByteArray()
            val imported = catalog.importZip(bytes.inputStream()).id
            assertEquals(state, catalog.store.load(imported)!!.guide)
            val copy = catalog.duplicate(id, "Copie")
            assertEquals(state, catalog.store.load(copy)!!.guide)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aVersionRestoredKeepsTheGuide() {
        val kept = project.withVersionKept(SongVersion.BEFORE_TAKE, 100L).copy(song = listOf(SongPart(0, 4)))
        val restored = kept.restoreVersion(0, 200L)
        assertEquals(listOf(SongPart(0, 2)), restored.song)
        assertEquals(state, restored.guide)
    }

    @Test
    fun refusalsHistoryAndNotesStayWithinTheirLimits() {
        var s = GuideState()
        repeat(GuideState.MAX_HISTORY + 50) { s = s.withChanges(listOf(GuideChange("inst:0", at = it.toLong()))) }
        assertEquals(GuideState.MAX_HISTORY, s.history.size)
        assertEquals((GuideState.MAX_HISTORY + 49).toLong(), s.history.last().at)
        s = s.withRefusal("a", "", 1).withRefusal("a", "", 2).withRefusal("a", "x", 3)
        assertEquals(1, s.refusals.size)  // refused as a whole already: its options too
        assertTrue(s.refused("a") && s.refused("a", "y"))
        assertEquals(s, s.withNote("   ", 1))
        assertEquals("a".repeat(GuideState.MAX_NOTE), s.withNote("a".repeat(900), 1).notes.single().text)
        assertEquals(listOf("inst:0"), s.historyOf("inst:0").map { it.track }.distinct())
    }
}
