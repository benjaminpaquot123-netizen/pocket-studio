package com.pocketstudio.app.tuto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audit.CaptureMatrixTest
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoChapter
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Round 6, « tuto-balayage » (owner, 2026-11-12: the tutorials tested in Facile and Pro, in every language).
 *
 * Where TutoPanelFitsTest draws the 8 longest steps of a language, this draws EVERY step of every course
 * (37 styles, Facile then Pro, each step once per distinct words) in the tuto's panel, unfolded, at the
 * bottom of the narrowest phone (phone360), at 100 and 130 % font, in one of the 15 languages per case
 * (Arabic right to left), with the Mode simple's words on in Facile (its plain-words lines under the step)
 * and off in Pro. [LayoutScan] reads cut words and texts drawn over each other; a step's words that
 * scroll with no « Lire la suite » are cut too (TutoPanelFitsTest's rule, and its one accepted overlap).
 *
 * What is drawn: the chapter's title, « Chapitre x/n · Étape y/m · Facile|Pro », the step's words and
 * their plain-words lines, the closed « Pourquoi ? », the song's map where the chapter has one, the
 * action bar. What is not: « Pourquoi ? » opened, the folded line, « Bravo » (TutoTextSweepTest reads
 * those texts, it does not draw them).
 *
 * The app is not changed by this lot: what is found is listed in
 * docs/audits/ultra/sieges/tour-6/tuto/connus-panneau.tsv (one line per finding: lang, font, level,
 * style, step, kind, what). A finding not in the list fails (a new defect), and so does a listed one no
 * longer found (fixed: take its line out). Every finding of the run is written to
 * app/build/tuto-sweep/panel-<lang>-<font>.tsv.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class TutoSweepPanelTest(private val lang: String, private val font: String) {
    companion object {
        val FONTS = listOf("1.0", "1.3")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = Lang.entries.flatMap { l -> FONTS.map { arrayOf<Any>(l.code, it) } }

        /** The known findings, from the round's folder (the app module's tests run in app/). */
        val KNOWN_FILE = File("../docs/audits/ultra/sieges/tour-6/tuto/connus-panneau.tsv")

        /** One step as the panel shows it: its course, level, chapters (of that level) and place. */
        class Placed(val style: String, val level: TutoLevel, val chapters: List<TutoChapter>, val chapter: Int, val step: Int) {
            val id: String get() = chapters[chapter].steps[step].id
        }

        /** Every step of every course, Facile first, then Pro. */
        val ALL: List<Placed> by lazy {
            TutoLevel.entries.flatMap { level ->
                TutoCourse.STYLES.flatMap { style ->
                    val chapters = TutoCourse.of(style)!!.chaptersFor(level)
                    chapters.flatMapIndexed { c, ch -> ch.steps.indices.map { Placed(style, level, chapters, c, it) } }
                }
            }
        }

        /** What the panel of [p] draws that changes from one step to another: two equal keys draw the same panel. */
        fun drawKey(p: Placed): String {
            val ch = p.chapters[p.chapter]
            val s = ch.steps[p.step]
            return listOf(p.level.name, ch.title, s.textFor(p.level), (s.why ?: ch.why).isNotEmpty(), ch.map, p.chapters.size, ch.steps.size).joinToString("\u0001")
        }

        fun line(lang: String, font: String, p: Placed, f: LayoutScan.Finding): String =
            listOf(lang, font, p.level.name, p.style, p.id, f.kind.name, f.what.replace('\t', ' ').replace('\n', ' ').take(80)).joinToString("\t")

        fun known(): Set<String> = if (KNOWN_FILE.exists()) KNOWN_FILE.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.toSet() else emptySet()
    }

    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false, simpleMode = false) }
        I18n.lang = Lang.FR
    }

    /** « Lire la suite » lies over the last line on purpose, on a fade (TutoPanelFitsTest's accepted finding). */
    private fun allowed(f: LayoutScan.Finding): Boolean {
        val cue = com.pocketstudio.model.i18n.tr("Lire la suite")
        val t = f.toString()
        return f.kind == LayoutScan.Kind.OVERLAP && (Regex("over (the touchable )?« ${Regex.escape(cue)}").containsMatchIn(t) || t.startsWith("OVERLAP « $cue »"))
    }

    private fun more() = compose.onAllNodes(androidx.compose.ui.test.hasTestTag("tuto_text_more"), useUnmergedTree = true)
        .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    @Test
    fun everyStepFits() {
        val l = Lang.entries.first { it.code == lang }
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = ctx.resources.displayMetrics.density
        val d = CaptureMatrixTest.DEVICES.getValue("phone360")
        val todo = ALL.distinctBy(::drawKey)
        var shown by mutableStateOf(todo.first())
        var level: TutoLevel? = null
        fun setLevel(v: TutoLevel) {
            if (level == v) return
            level = v
            SettingsStore.update(ctx) { it.copy(language = l, englishNotes = l != Lang.FR, simpleMode = v == TutoLevel.Facile) }
            I18n.lang = l
        }
        setLevel(todo.first().level)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(l.rtl)) {
                val local = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(local.density, local.fontScale * font.toFloat())) {
                    PocketStudioTheme(Palettes.Studio) {
                        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black).padding(d.chrome).background(Neon.Background), contentAlignment = Alignment.BottomCenter) {
                            val p = shown
                            key(drawKey(p), p.style) {
                                val s = p.chapters[p.chapter].steps[p.step]
                                val ui = TutoUi(TutoProgress(p.style, p.level, emptySet(), s.id), p.chapters, p.chapter, p.step)
                                TutoPanel(ui, canShow = true, actions = TutoActions(), map = tutoMap(p.style))
                            }
                        }
                    }
                }
            }
        }
        val found = ArrayList<String>()
        val pictured = HashSet<String>()
        for (p in todo) {
            setLevel(p.level)
            shown = p
            compose.waitForIdle()
            val here = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT, LayoutScan.Kind.OVERLAP)).toMutableList()
            compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false).forEach { n ->
                    val range = n.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) ?: return@forEach
                    val hidden = range.maxValue() / density
                    if (hidden > 2f && !more()) here += LayoutScan.Finding(LayoutScan.Kind.CUT, "(scroll)", "the panel's words scroll (%.0f dp unseen) with no « Lire la suite »".format(hidden))
                }
            val kept = here.filterNot(::allowed)
            kept.forEach { f -> found += line(lang, font, p, f) + "\t" + f.detail.replace('\t', ' ').take(160) }
            // `-Proborazzi.test.record=true`: the first panel of each kind of finding, to look at (app/build/tuto-sweep/images/).
            if (CaptureMatrixTest.RECORDING) kept.map { it.kind.name + " " + it.detail.substringBefore('(').trim() }.distinct().filter { pictured.add(it) }.forEach { k ->
                compose.onRoot().captureRoboImage("build/tuto-sweep/images/$lang-$font-${p.level}-${p.style}-${p.id}-${k.replace(Regex("[^A-Za-z]+"), "_")}.png")
            }
        }
        val out = File("build/tuto-sweep").apply { mkdirs() }
        File(out, "panel-$lang-$font.tsv").writeText(found.joinToString("\n", postfix = if (found.isEmpty()) "" else "\n"))
        File(out, "panel-$lang-$font.count").writeText("${todo.size} panels drawn\n")
        // Compared without the detail column (its numbers move with a pixel).
        val keys = found.map { it.substringBeforeLast('\t') }.toSet()
        val known = known().filter { it.startsWith("$lang\t$font\t") }.toSet()
        val fresh = keys - known
        val gone = known - keys
        assertTrue(
            "$lang $font: ${todo.size} panels; new findings (${fresh.size}):\n" + fresh.take(30).joinToString("\n") +
                "\nlisted but no longer found (${gone.size}, fixed? take them out of ${KNOWN_FILE.path}):\n" + gone.take(30).joinToString("\n"),
            fresh.isEmpty() && gone.isEmpty(),
        )
    }
}
