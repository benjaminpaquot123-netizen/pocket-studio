package com.pocketstudio.app.journey

/**
 * A user's path through the app, touch by touch, by the screens' test tags (docs/TESTS-REELS.md,
 * « Parcours »). One copy for every place it runs: on the emulator with the real app and engine
 * (androidTest: JourneyTest, EnduranceTest), in Robolectric (test: JourneyPathsTest, the same
 * touches on the app without its engine, so that a renamed tag fails in seconds rather than on
 * the emulator), and inside « BenStudio Test » on the phone (docs/PLAN-TEST-APK.md). What it does
 * on the screen goes through a [JourneyDriver]: the compose test rule's (sharedTest) or the
 * in-app one (src/benTest). Waits by looking again and again (the app runs in between), never by
 * a fixed pause.
 */
class Journey(private val driver: JourneyDriver, private val log: (String) -> Unit = {}) {
    fun exists(tag: String): Boolean = driver.count(tag, merged = false) > 0

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
    fun idle(ms: Long) = driver.idle(ms)

    /**
     * A touch on [tag], scrolled to first when it is out of sight. While a screen changes (a
     * crossfade), the leaving one still holds a copy of the button: the touch waits for one only.
     */
    fun tap(tag: String, timeoutMs: Long = 15_000) {
        // The opening (BrandIntro, under 5 s) covers the app at each start: a touch would only skip it.
        awaitGone("intro", timeoutMs)
        await(tag, timeoutMs)
        val end = System.currentTimeMillis() + timeoutMs
        while (runCatching { driver.count(tag, merged = true) }.getOrDefault(0) != 1) {
            check(System.currentTimeMillis() < end) { "$tag is not there once after $timeoutMs ms" }
            idle(50)
        }
        settled(tag, end)
        log("tap $tag")
        // Always into view: a node half past a scrolling row's edge counts as displayed, and a
        // touch at its centre lands off the screen (Sons' chips on a narrow phone).
        runCatching { driver.scrollTo(tag) }
        driver.click(tag)
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
            val read = runCatching { driver.bounds(tag) }
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
    fun tagsMatching(pattern: Regex): List<String> = driver.tags(pattern)

    // --- The paths -------------------------------------------------------------------------------

    /**
     * A first launch: the welcome, then the guided song ([guided]: « Simple ou Pro ? » ([simple] or
     * Pro), the assistant opens) or « Plus tard » (the starter beat, then Pro with the top bar's
     * mode chip when not [simple]).
     */
    fun firstLaunch(simple: Boolean, guided: Boolean) {
        // The welcome waits for the opening (about 6.5 s since 2026-11-04): its end first, on its own cap.
        awaitGone("intro", 60_000)
        await("welcome", 30_000)
        if (guided) {
            tap("welcome_guided")
            tap(modeCard(simple))
        } else {
            tap("welcome_later")
            awaitGone("welcome")
            if (!simple) {
                tap("top_mode")
                tap(modeCard(false))
                awaitGone("mode_switch")
            }
        }
    }

    /** « Simple ou Pro ? »'s card. */
    private fun modeCard(simple: Boolean) = if (simple) "mode_card_simple" else "mode_card_pro"

    /** « Que veux-tu faire ? », as each later start of the app asks: « Continuer » opens the last project. */
    fun continueLastProject() {
        await("launch", 30_000)
        tap("launch_continue")
        awaitGone("launch")
    }

    /**
     * « Que veux-tu faire ? » › « Nouveau morceau » › « Pas à pas avec l'assistant », the song in
     * [simple]'s mode (by default the mode the app is in, read before the start: a mode taken
     * without asking would change it).
     */
    fun newSongInAssistant(simple: Boolean = currentMode()) {
        await("launch", 30_000)
        tap("launch_new")
        tap("starter_wizard")
        // « Simple ou Pro ? » (owner, 2026-10-29) is only asked while no mode was ever chosen on the phone
        // (answer 3A, tour 5): otherwise the assistant opens in the last one, said with « Changer ».
        if (awaitAny(modeCard(simple), "wizard") == "wizard") {
            await("wizard_mode")
            if (currentMode() != simple) {
                tap("wizard_mode_change")
                tap(modeCard(simple))
                awaitGone("mode_step")
            }
        } else {
            tap(modeCard(simple))
        }
        await("wizard")
        check(currentMode() == simple) { "the assistant's song is in mode ${modeName(currentMode())}, not ${modeName(simple)}" }
    }

    private fun currentMode(): Boolean = com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode

    private fun modeName(simple: Boolean) = if (simple) "Simple" else "Pro"

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

    companion object {
        /** The family header of the assistant's style list for [family] (StyleFamilies' tags). */
        fun familyTag(family: String) = "wizard_family_$family"

        /** A key of the on-screen keyboard (any layout): `key_<note>`. */
        val KEY = Regex("key_\\d+")
    }
}
