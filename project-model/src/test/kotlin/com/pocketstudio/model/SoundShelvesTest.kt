package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The sound browser's folders (Benjamin's list n° 13–14): by meaning, two levels, no tiny folder, best first. */
class SoundShelvesTest {
    private fun items(family: String, sub: String?, pack: String, n: Int) =
        (1..n).map { SoundShelves.Item("$pack-$it", family, sub, pack) }

    @Test
    fun familiesFirstLevelInTheStyleChooserOrderPinnedFirst() {
        val shelves = SoundShelves.build(
            items("House", null, "house", 10) + items("Machines", "TR-909", "tr909", 10) + items("Fête", null, "party", 3),
            pinned = listOf("Fête"),
        )
        assertEquals(listOf("Fête", "Machines", "House"), shelves.map { it.id })
        // A pinned family keeps its folder however small.
        assertEquals(3, shelves.first().count)
    }

    @Test
    fun aSmallFamilyJoinsOthersAndASmallGroupJoinsItsFamily() {
        val shelves = SoundShelves.build(
            items("Techno", "Sec et court", "techno", 12) + items("Techno", "Saturé", "techno2", 3) +
                items("Techno", "Long et grave", "techno3", 9) + items("Hip-hop", null, "trap", 4) + items("Autres", null, "pop", 5),
        )
        assertEquals(listOf("Techno", "Autres"), shelves.map { it.id })
        val techno = shelves.first()
        assertEquals(listOf("Techno/Sec et court", "Techno/Long et grave"), techno.children.map { it.id })
        // The 3 « Saturé » sounds are the family's own, not a folder of 3.
        assertEquals(3, techno.items.size)
        assertEquals(9, shelves[1].count)
        // Never more than two levels, never a folder under the minimum (but the family's own sounds).
        shelves.forEach { s -> s.children.forEach { c -> assertTrue(c.children.isEmpty()); assertTrue(c.items.size >= SoundShelves.MIN_FOLDER) } }
    }

    @Test
    fun oneGroupAloneIsNoChoice() {
        val shelves = SoundShelves.build(items("House", "Rond et punchy", "house", 20) + items("House", "Mat", "deep", 2))
        assertTrue(shelves.single().children.isEmpty())
        assertEquals(22, shelves.single().items.size)
    }

    @Test
    fun thePacksTakeTurnsBestFirst() {
        val list = SoundShelves.turns(
            listOf(SoundShelves.Item("kit", "K")) + items("Techno", null, "a", 3) + items("Techno", null, "b", 2),
        )
        assertEquals(listOf("kit", "a-1", "b-1", "a-2", "b-2", "a-3"), list)
    }

    @Test
    fun pathOfASoundOpensItsFolder() {
        val shelves = SoundShelves.build(items("Machines", "TR-909", "tr909", 10) + items("Machines", "TR-808", "tr808", 10))
        assertEquals(listOf("Machines", "Machines/TR-808"), SoundShelves.pathOf(shelves, "tr808-4"))
        assertEquals(emptyList<String>(), SoundShelves.pathOf(shelves, "nowhere"))
        assertEquals("TR-909", shelves.first().find("Machines/TR-909")?.title)
        assertNull(shelves.first().find("Techno"))
    }

    @Test
    fun aLibrarySoundsGroupIsItsLabel() {
        assertEquals("Cartes à jouer", SoundShelves.groupOfName("Cartes à jouer 12"))
        assertEquals("Conga", SoundShelves.groupOfName("Conga"))
    }

    @Test
    fun theStylePacksDrumsAreFiledByMeasuredCharacter() {
        val labels = SoundCharacter.LABELS.values.toSet()
        // Every kick of a style pack has a character; a machine or a library has none (its own second level).
        val kicks = (1..16).map { "kick_%02d.flac".format(it) }
        assertTrue(kicks.all { SoundCharacter.of("techno", it) in labels })
        assertNull(SoundCharacter.of("tr909", "kick_01.flac"))
        assertNull(SoundCharacter.of("techno", "stab_01.flac"))
        assertFalse(kicks.map { SoundCharacter.of("hard_techno", it) }.distinct().size < 2)
    }
}
