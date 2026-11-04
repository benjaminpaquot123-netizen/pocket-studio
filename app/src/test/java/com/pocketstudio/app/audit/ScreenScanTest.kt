package com.pocketstudio.app.audit

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.ui.HelpAbout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.lang.reflect.InvocationTargetException

/**
 * Audit ultra §1, the census of the controls on every screen of [ScreenRegistry]: each screenshot
 * test's method run as it is (its rules, `@Before`, the method, `@After`), then, on what it left on the
 * screen, four counts:
 * - **LABEL** (« sans nom »): a control TalkBack can act on (touch, long press, slider, switch) with no
 *   words nor description ([LayoutScan], LABEL extended to sliders, switches and long presses);
 * - **HELP** (« sans ? »): a control with a test tag that no « ? » on the screen explains ([HelpAbout]);
 * - **TARGET**: a touch target under 48 dp to touch or 24 dp seen; **CONTRAST**: a text under WCAG AA.
 *
 * The whole census is written to `app/build/recensement/` (one file per screen, never committed); the
 * counts accepted, each with its reason, are in `src/test/resources/audit/recensement-acceptes.tsv`. A
 * case fails only when a count rises above its accepted ceiling (a screen not in the list: ceiling 0),
 * as [com.pocketstudio.app.CarteFonctionsTest] does for its empty cells: a ceiling only goes down.
 *
 * The ≈ 290 screens drawn a second time take about three minutes (2 workers): on every run. Only the screen's last state is read (a method that draws several states in
 * a row is read on its last one); the class's own `@Config` size is not taken (one phone, 400 x 890 dp).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class ScreenScanTest(private val screen: String) {
    companion object {
        val SCREENS: Map<String, ScreenRegistry.Screen> by lazy { ScreenRegistry.screens().associateBy { it.id } }

        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = SCREENS.keys.map { arrayOf<Any>(it) }

        val KINDS = listOf("LABEL", "HELP", "TARGET", "CONTRAST")

        /** How far a count may go over its ceiling with the test order alone (see [census]). */
        const val ORDER_MARGIN = 2

        val ACCEPTED_FILE = File("src/test/resources/audit/recensement-acceptes.tsv")
        val OUT = File("build/recensement")

        /** Screen › kind › the count accepted. */
        val ACCEPTED: Map<String, Map<String, Int>> by lazy {
            if (!ACCEPTED_FILE.exists()) emptyMap() else ACCEPTED_FILE.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('\t') }
                .groupBy({ it[0] }, { it[1] to it[2].toInt() }).mapValues { it.value.toMap() }
        }

        /** A « ? »'s tag pattern: `*` any text, `#` a number (as [com.pocketstudio.app.help.HelpCoverageTest] reads them). */
        fun regex(glob: String) = Regex(glob.split('*').joinToString(".*") { part -> part.split('#').joinToString("[0-9]+") { Regex.escape(it) } })

        /** A help button's own tags, and what needs no « ? » anywhere (back, close, a « ? » itself). */
        private val OBVIOUS = listOf(Regex("help_.*"), Regex(".*_(close|back|cancel|scrim)"))
    }

    @After
    fun french() {
        com.pocketstudio.app.settings.SettingsStore.forgetForTests()
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        com.pocketstudio.model.i18n.I18n.everyday = false
    }

    @Test
    fun census() {
        val s = SCREENS.getValue(screen)
        val cls = Class.forName(s.className, true, javaClass.classLoader)
        val target = cls.getConstructor().newInstance()
        val tc = TestClass(cls)
        val rules = tc.getAnnotatedMethodValues(target, Rule::class.java, TestRule::class.java) +
            tc.getAnnotatedFieldValues(target, Rule::class.java, TestRule::class.java)
        val compose = rules.filterIsInstance<ComposeTestRule>().firstOrNull()
        assertTrue("$screen: no Compose rule", compose != null)
        var found: List<Pair<String, String>> = emptyList()
        var statement: Statement = object : Statement() {
            override fun evaluate() {
                tc.getAnnotatedMethods(Before::class.java).forEach { it.invokeExplosively(target) }
                try {
                    try { ScreenRegistry.method(cls, s).invoke(target) } catch (e: InvocationTargetException) { throw e.targetException }
                    found = scan(compose!!)
                } finally {
                    tc.getAnnotatedMethods(After::class.java).forEach { it.invokeExplosively(target) }
                }
            }
        }
        for (r in rules) statement = r.apply(statement, Description.createTestDescription(cls, s.method))
        statement.evaluate()

        OUT.mkdirs()
        File(OUT, "$screen.txt").writeText(found.joinToString("") { "${it.first}\t${it.second}\n" })
        val counts = KINDS.associateWith { k -> found.count { it.first == k } }
        File(OUT, "$screen.tsv").writeText(KINDS.joinToString("") { "$screen\t$it\t${counts[it]}\n" })
        val accepted = ACCEPTED[screen].orEmpty()
        // A margin for the test order: the cards folded by an earlier test stay folded (LocalFoldStore's default
        // store is shared by the whole JVM), which hides or shows a few controls. A screen not accepted: none.
        val margin = if (accepted.isEmpty()) 0 else ORDER_MARGIN
        val over = KINDS.filter { (counts[it] ?: 0) > (accepted[it] ?: 0) + margin }
        assertTrue(
            "$screen: more than accepted (${over.joinToString { "$it ${counts[it]} > ${accepted[it] ?: 0}" }}); fix them, or accept them with a reason " +
                "in ${ACCEPTED_FILE.path}:\n" + found.filter { it.first in over }.joinToString("\n") { "${it.first} ${it.second}" },
            over.isEmpty(),
        )
    }

    private fun scan(compose: ComposeTestRule): List<Pair<String, String>> {
        val density = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        val out = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.LABEL, LayoutScan.Kind.TARGET, LayoutScan.Kind.CONTRAST))
            .map { it.kind.name to "« ${it.what} » ${it.detail}" }.toMutableList()
        // « sans ? »: the tagged controls no « ? » of the screen explains.
        val nodes = compose.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)
        val explained = nodes.flatMap { it.config.getOrNull(HelpAbout).orEmpty() }.map(::regex)
        nodes.filter { LayoutScan.CONTROL.matches(it) && !LayoutScan.hidden(it) }
            .mapNotNull { n -> n.config.getOrNull(SemanticsProperties.TestTag)?.let { it to n } }
            .filter { (tag, _) -> OBVIOUS.none { it.matches(tag) } && explained.none { it.matches(tag) } }
            .distinctBy { it.first }.sortedBy { it.first }
            .forEach { (tag, n) -> out += "HELP" to "$tag (${LayoutScan.controlKind(n)} « ${LayoutScan.nameOf(n).take(40)} »)" }
        return out
    }
}
