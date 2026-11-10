package com.pocketstudio.app.human

import com.pocketstudio.app.ui.DoubleTouchGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onAllNodesWithTag
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit ultra round 4 (lot banc): the same gestures as the seats, on the bench (the app's side: Tour4AppProofTest).
 * - « Enregistrer ma mélodie », three keys, « Arrêter » (seats A b, C b, C d, D a, D d): « Rien n'a été joué ».
 * - « Renommer » › ✕ « Effacer » (seats C a, E a): the bench never came back.
 * - Live's « ■ Tout » and a column's ■ (seats B c, C c): the blocks played on.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchTour4Test : HumanTester("tour4") {
    @Before
    fun notOnTheBench() {
        assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())
        DoubleTouchGuard.reset()
    }

    private fun tap(tag: String): String {
        waitFor("« $tag »") { centerOf(tag) != null }
        val at = reachTag(tag) ?: centerOf(tag)!!
        return act("touche ${at.first} ${at.second}")
    }

    private fun tapText(text: String): String {
        waitFor("« $text »") { centerOfText(text, substring = true) != null }
        val at = centerOfText(text, substring = true)!!
        return act("touche ${at.first} ${at.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    /** The assistant's song in Mode simple (several parts, as the seats' projects), on its « Morceau » tab. */
    private fun simpleSong() {
        begin()
        tapText("Créer mon premier morceau")
        tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
    }

    /** The seats' way: the song playing, « Clavier », « Enregistrer ma mélodie », three keys of the scale, « Arrêter ». */
    @Test
    fun aMelodyTakeOnTheBenchKeepsItsKeys() {
        simpleSong()
        tap("top_play")
        act("attends 1")
        tap("tab_simple_Clavier")
        tap("record_start")
        val vm = rhythm()!!
        assertTrue("the take is on (${vm.lastTakeReport})", vm.recording.value)
        waitFor("the take records") { vm.noteTake.value?.phase == com.pocketstudio.app.record.TakePhase.RECORDING }
        // Three keys the song's scale lets play (a key out of it is greyed and plays nothing, as on a phone).
        val keys = (60..84).map { "key_$it" }.filter { k ->
            runCatching { compose.onAllNodesWithTag(k).fetchSemanticsNodes().firstOrNull()?.config?.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == false }.getOrDefault(false)
        }.take(3)
        assertEquals("three keys of the scale on screen", 3, keys.size)
        val seen = keys.map { k ->
            val at = reachTag(k) ?: centerOf(k)!!
            val under = runCatching { screenText().under(at.first, at.second) }.getOrDefault("?")
            val r = act("touche ${at.first} ${at.second}")
            "$k@$at under « $under » → ${r.lineSequence().firstOrNull { "Son" in it }.orEmpty().take(80)}"
        }
        val near = screenLines().filter { l -> Regex("@ (\\d+),(\\d+)").find(l)?.groupValues?.get(2)?.toIntOrNull()?.let { it in 600..900 } == true }
        assertTrue("still recording after the keys (${vm.lastTakeReport})", vm.recording.value)
        tap("record_stop")
        val facts = "${vm.lastTakeReport}; keys: $seen; screen near the keys:\n" + near.joinToString("\n")
        assertFalse("« Rien n'a été joué » on the bench: $facts", vm.takeEmpty.value)
        assertTrue(facts, vm.lastTakeReport!!.contains(" 3 notes"))
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    /**
     * The keyboard of « Clavier » on the bench's phone (392 x 852 dp), Mode simple, the Guide's bar shown, then during a
     * take: how much of each key the screen shows (the seats: « clavier à moitié caché sous la barre du Guide »). A
     * measure for the report (the layout is the app's), printed as « TOUR4 keys ».
     */
    @Test
    fun howMuchOfTheKeyboardIsShown() {
        simpleSong()
        tap("tab_simple_Clavier")
        act("attends 1")
        val before = keysShown("Guide bar")
        tap("record_start")
        act("attends 1")
        val during = keysShown("during the take")
        println("TOUR4 keys: $before | $during")
        assertTrue(before, before.contains("keys"))
    }

    private fun keysShown(what: String): String {
        val m = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        fun nodes(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)
        val keys = (48..84).flatMap { nodes("key_$it") }
        if (keys.isEmpty()) return "$what: no keys"
        val dp = m.density
        val full = keys.maxOf { it.size.height } / dp
        val shown = keys.map { it.boundsInWindow.height / dp }
        val top = keys.minOf { it.boundsInWindow.top } / dp
        val bottom = keys.maxOf { it.boundsInWindow.bottom } / dp
        val guide = nodes("guide_bar").firstOrNull()?.boundsInWindow?.let { "guide bar ${(it.top / dp).toInt()}..${(it.bottom / dp).toInt()} dp" } ?: "no guide bar"
        val hint = nodes("take_loop_grown").firstOrNull()?.boundsInWindow?.let { "hint ${(it.top / dp).toInt()}..${(it.bottom / dp).toInt()} dp" } ?: "no hint"
        return "$what: ${keys.size} keys, ${full.toInt()} dp tall, shown ${shown.min().toInt()}..${shown.max().toInt()} dp " +
            "(${(100 * shown.min() / full).toInt()}..${(100 * shown.max() / full).toInt()} %), keys at ${top.toInt()}..${bottom.toInt()} dp of ${(m.heightPixels / dp).toInt()}; $guide; $hint"
    }

    /** « Mes projets » › ⋮ › « Renommer », ✕ « Effacer », the field touched, a name typed, « OK »: renamed, never frozen. */
    @Test
    fun renameAfterTheClearButton() {
        simpleSong()
        tap("top_projects")
        waitFor("a project's ⋮") { screenLines().any { "project_menu_" in it } || centerOfMenu() != null }
        val menu = centerOfMenu()!!
        act("touche ${menu.first} ${menu.second}")
        tapText("Renommer")
        waitFor("the rename window") { centerOf("project_name_field") != null }
        val at = System.currentTimeMillis()
        // Where the bench waits after « Effacer » (round 4, 240 s): the test thread's stack every 15 s, printed.
        val me = Thread.currentThread()
        val spy = Thread {
            try {
                while (true) {
                    Thread.sleep(15_000)
                    println("TOUR4 STACK +${System.currentTimeMillis() - at} ms\n" + me.stackTrace.filter { "pocketstudio" in it.className || "compose.ui.test" in it.className }.take(30).joinToString("\n") { "    at $it" })
                }
            } catch (_: InterruptedException) {}
        }.apply { isDaemon = true; start() }
        try { tap("name_clear") } finally { spy.interrupt() }
        assertTrue("« Effacer » answered in ${System.currentTimeMillis() - at} ms", System.currentTimeMillis() - at < 60_000)
        // Nothing focused yet: the keys go nowhere (said); the field touched, they go in.
        tap("project_name_field")
        act("ecris Morceau de Max v2")
        tap("project_rename_ok")
        waitFor("the new name on the list") { centerOfText("Morceau de Max v2") != null }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    /**
     * T5-Dc-4, T5-Ac-61, T5-Db-16 (round 5, seats A c #60, D b #16, D c #4; recette tel-01 on the phone: the letters go in after
     * ✕). The seats' journals: each time, ✕ « Effacer » was touched right after the window opened, the field never touched, then
     * « ecris »: no field holds the cursor (the window gives it to none, on a phone no keyboard would show), and the bench said
     * so (« touche d'abord le champ »), which the seats reported as the app's. The field touched, then ✕, then « ecris »: the
     * field keeps the cursor and takes the name (seat A c #50-#53 did just that and it worked), as the phone does.
     */
    @Test
    fun theClearButtonKeepsTheFieldsFocus() {
        simpleSong()
        tap("top_projects")
        waitFor("a project's ⋮") { screenLines().any { "project_menu_" in it } || centerOfMenu() != null }
        val menu = centerOfMenu()!!
        act("touche ${menu.first} ${menu.second}")
        tapText("Renommer")
        waitFor("the rename window") { centerOf("project_name_field") != null }
        // The seats' way: ✕ first, the field never touched. Nothing holds the cursor: said, the name stays empty.
        assertFalse("the window opened gives the cursor to no field (a phone shows no keyboard)\n" + focusReport(), BenchWindows.focusedTextField())
        tap("name_clear")
        val blind = act("ecris Morceau de Max v3")
        assertTrue(blind, "touche d'abord le champ" in blind)
        assertEquals("nothing typed blind", "", fieldText("project_name_field"))
        // A finger's way: the field touched (the cursor comes), ✕, the name typed.
        tap("project_name_field")
        act("ecris Ancien nom")
        assertTrue("the field touched holds the cursor\n" + focusReport(), BenchWindows.focusedTextField())
        tap("name_clear")
        assertEquals("the name emptied", "", fieldText("project_name_field"))
        assertTrue("after ✕ « Effacer » the field still holds the cursor (as on the phone, recette tel-01)\n" + focusReport(), BenchWindows.focusedTextField())
        val said = act("ecris Morceau de Max v3")
        assertFalse(said, "Rien ne s'écrit" in said)
        assertEquals("Morceau de Max v3", fieldText("project_name_field"))
        tap("project_rename_ok")
        waitFor("the new name on the list") { centerOfText("Morceau de Max v3") != null }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun fieldText(tag: String): String? = compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false)
        .firstOrNull()?.config?.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.EditableText)?.text

    /** Where the focus is, for a failure's message: each window's touch mode and focused view, the focused Compose nodes. */
    private fun focusReport(): String {
        val windows = BenchWindows.windows().joinToString("\n") { w ->
            "window type ${w.params.type}: touchMode=${w.view.isInTouchMode} windowFocus=${w.view.hasWindowFocus()} focusedView=${w.view.findFocus()?.javaClass?.simpleName}"
        }
        val nodes = compose.onAllNodes(androidx.compose.ui.test.isFocused()).fetchSemanticsNodes(atLeastOneRootRequired = false).map { n ->
            n.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag)
                ?: n.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)?.joinToString()
                ?: n.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Role)?.toString() ?: "?"
        }
        return "$windows\nfocused nodes: $nodes"
    }

    /** The Live page's « ■ Tout » (its stopAll) and a column's ■ while the music plays: the blocks stop on the launch line. */
    @Test
    fun liveStopsReachTheirLaunchLineOnTheBench() {
        simpleSong()
        tap("top_play")
        act("attends 1")
        val vm = rhythm()!!
        assertTrue(vm.transport.value.playing)
        benchEngine.padCommandsHeard()
        vm.stopLiveTrack(com.pocketstudio.model.SongPart.DRUMS)
        assertEquals(-1, vm.liveGrid.value!!.slot(com.pocketstudio.model.LiveSlots.DRUMS).pending)
        // A.16 (G2): the stop went to the engine, which makes it on its line and reports it (no bench hook any more).
        val drums = 1 shl com.pocketstudio.model.LiveSlots.DRUMS
        assertTrue("the drums' ■ went to the engine", benchEngine.padCommandsHeard().any { it.startsWith("armLaunch $drums ") && it.endsWith(" -1") })
        act("attends 4")
        assertEquals("the drums' ■ reached its line", -1, vm.liveGrid.value!!.slot(com.pocketstudio.model.LiveSlots.DRUMS).playing)
        vm.stopLiveAll()
        val all = com.pocketstudio.model.LiveSlots.ALL and com.pocketstudio.engine.NativeEngine.ALL_SLOTS
        assertTrue("« ■ Tout » went to the engine", benchEngine.padCommandsHeard().any { it.startsWith("armLaunch $all ") && it.endsWith(" -1") })
        act("attends 4")
        val g = vm.liveGrid.value!!
        assertFalse("nothing waits any more: $g", g.hasPending)
        assertTrue("every block stopped: $g", (0 until com.pocketstudio.model.LiveSlots.COUNT).all { g.slot(it).playing == -1 })
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun centerOfMenu(): Pair<Float, Float>? = runCatching {
        val n = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("⋮") { n ->
            n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith("project_menu_")
        }).fetchSemanticsNodes().firstOrNull() ?: return null
        val tag = n.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag]
        centerOf(tag)
    }.getOrNull()
}
