package com.pocketstudio.app.journey

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

/**
 * A user's path through the app, touch by touch, by the screens' test tags (docs/TESTS-REELS.md,
 * « Parcours »). One copy for both places it runs: on the emulator with the real app and engine
 * (androidTest: JourneyTest, EnduranceTest) and in Robolectric (test: JourneyPathsTest, the same
 * touches on the app without its engine, so that a renamed tag fails in seconds rather than on
 * the emulator). Waits by looking again and again (the main looper runs in between), never by
 * a fixed pause.
 */
class Journey(private val compose: ComposeTestRule, private val log: (String) -> Unit = {}) {
    fun exists(tag: String): Boolean = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /** Waits for something tagged [tag] (work off the main thread, a screen rebuilt). */
    fun await(tag: String, timeoutMs: Long = 15_000) {
        awaitAny(tag, timeoutMs = timeoutMs)
    }

    /** Waits for one of [tags]; the first one there. */
    fun awaitAny(vararg tags: String, timeoutMs: Long = 15_000): String {
        val end = System.currentTimeMillis() + timeoutMs
        while (true) {
            var problem: Throwable? = null
            tags.firstOrNull { t -> runCatching { exists(t) }.onFailure { problem = it }.getOrDefault(false) }?.let { return it }
            check(System.currentTimeMillis() < end) { "nothing tagged ${tags.joinToString(" or ")} after $timeoutMs ms" + (problem?.let { " ($it)" } ?: "") }
            idle(50)
        }
    }

    /** Waits while [tag] is on screen. */
    fun awaitGone(tag: String, timeoutMs: Long = 15_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (runCatching { exists(tag) }.getOrDefault(true)) {
            check(System.currentTimeMillis() < end) { "$tag still there after $timeoutMs ms" }
            idle(50)
        }
    }

    /** A short wait that lets the app work (Robolectric's main looper too). */
    fun idle(ms: Long) {
        val end = System.currentTimeMillis() + ms
        do {
            Thread.sleep(minOf(50L, ms))
            runCatching { compose.waitForIdle() }
        } while (System.currentTimeMillis() < end)
    }

    /**
     * A touch on [tag], scrolled to first when it is out of sight. While a screen changes (a
     * crossfade), the leaving one still holds a copy of the button: the touch waits for one only.
     */
    fun tap(tag: String, timeoutMs: Long = 15_000) {
        await(tag, timeoutMs)
        val end = System.currentTimeMillis() + timeoutMs
        while (runCatching { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size }.getOrDefault(0) != 1) {
            check(System.currentTimeMillis() < end) { "$tag is not there once after $timeoutMs ms" }
            idle(50)
        }
        settled(tag, end)
        log("tap $tag")
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) runCatching { node.performScrollTo() }
        node.performClick()
        runCatching { compose.waitForIdle() }
    }

    /**
     * Waits until [tag] stops moving: a sheet still sliding in puts the button somewhere else
     * than where the touch lands (on a device the touch then hits the scrim and only closes the
     * sheet). Two reads 100 ms apart at the same place, with a real size.
     */
    private fun settled(tag: String, end: Long) {
        var last: androidx.compose.ui.geometry.Rect? = null
        val seen = ArrayList<String>()
        while (System.currentTimeMillis() < end) {
            val read = runCatching { compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow }
            val now = read.getOrNull()
            if (now != null && now == last && now.width > 0f && now.height > 0f) return
            // Some devices report no bounds at all for these nodes (the CI emulator: all zero): nothing to wait for.
            if (now != null && now == last && now.width == 0f && now.height == 0f) return
            if (seen.size < 6) seen += read.fold({ "$it" }, { "${it.javaClass.simpleName}: ${it.message?.take(80)}" })
            last = now
            idle(100)
        }
        // Never still: say what moved (or why the node could not be read) in the device's log.
        log("$tag never settled: $seen")
    }

    /** The tags on screen that match [pattern] (the keys of a keyboard, the cells of a grid). */
    fun tagsMatching(pattern: Regex): List<String> =
        compose.onAllNodes(SemanticsMatcher("tag matches $pattern") { n -> n.config.getOrNull(SemanticsProperties.TestTag)?.matches(pattern) == true }, useUnmergedTree = true)
            .fetchSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }

    // --- The paths -------------------------------------------------------------------------------

    /**
     * A first launch: the welcome, « Comment veux-tu travailler ? » ([simple] or every tool), then
     * the guided song ([guided]: the assistant opens) or « Plus tard » (the starter beat).
     */
    fun firstLaunch(simple: Boolean, guided: Boolean) {
        await("welcome", 30_000)
        tap(if (simple) "welcome_mode_simple" else "welcome_mode_full")
        tap(if (guided) "welcome_guided" else "welcome_later")
        if (!guided) awaitGone("welcome")
    }

    /** « Que veux-tu faire ? », as each later start of the app asks: « Continuer » opens the last project. */
    fun continueLastProject() {
        await("launch", 30_000)
        tap("launch_continue")
        awaitGone("launch")
    }

    /** « Que veux-tu faire ? » › « Nouveau morceau » › « Pas à pas avec l'assistant ». */
    fun newSongInAssistant() {
        await("launch", 30_000)
        tap("launch_new")
        tap("starter_wizard")
        await("wizard")
    }

    /**
     * The assistant's first step: [style] (its family opened when folded), « Choisir ce style »;
     * then « Créer maintenant » is there, with the assistant's advised choices.
     */
    fun chooseStyle(style: String, family: String) {
        await("wizard")
        if (!exists("wizard_style_$style")) tap(familyTag(family))
        tap("wizard_style_$style")
        tap("wizard_style_choose_$style")
        await("wizard_create_now")
    }

    /** « Créer maintenant »: the song is made, then the app opens it on « Ton morceau est prêt ! » (the screen is rebuilt). */
    fun createNow() = tap("wizard_create_now")

    /** The part sheet of song part [part] from Structure's list of parts (« ⋮ » on its row). */
    fun openPartSheet(part: Int) {
        if (!exists("part_menu_$part")) {
            tap("structure_parts")
            // The switch's crossfade: a touch during it can land on the view that leaves.
            awaitGone("timeline_screen")
        }
        tap("part_menu_$part")
        if (runCatching { await("part_sheet", 4_000) }.isFailure) {
            tap("part_menu_$part")
            await("part_sheet")
        }
    }

    /** One of the part sheet's intentions ([intentTag]: part_fill, part_buildup_4…) on part [part]: done, the sheet closes. */
    fun partIntent(part: Int, intentTag: String) {
        openPartSheet(part)
        tap(intentTag)
        awaitGone("part_sheet")
    }

    /** The missions' card over the screen (the guided song starts them): « Arrêter les missions ». */
    fun stopMissionsIfShown() {
        if (exists("mission_stop")) tap("mission_stop")
    }

    companion object {
        /** The family header of the assistant's style list for [family] (StyleFamilies' tags). */
        fun familyTag(family: String) = "wizard_family_$family"

        /** A key of the on-screen keyboard (any layout): `key_<note>`. */
        val KEY = Regex("key_\\d+")
    }
}
