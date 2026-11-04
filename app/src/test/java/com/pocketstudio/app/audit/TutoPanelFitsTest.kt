package com.pocketstudio.app.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.tuto.TutoActions
import com.pocketstudio.app.tuto.TutoPanel
import com.pocketstudio.app.tuto.TutoUi
import com.pocketstudio.app.tuto.tutoMap
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoChapter
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit ultra §2, « Le panneau tient »: in each of the 15 languages, the longest step texts and chapter
 * titles of all the courses (Facile and Pro, as that language writes them) drawn in the tuto's panel,
 * unfolded, at the bottom of the screen of a device of [CaptureMatrixTest.DEVICES], at 100 or 130 % font,
 * in pairs ([Pairwise]: every language meets every rank, every device and every font size). [LayoutScan]
 * reads cut words and texts over each other; the panel's text box must not need scrolling either (its
 * scroll shows no bar: the end of the words would stay unseen). The rank-th longest step goes with the
 * rank-th longest title, in its own chapter.
 *
 * Images: `-Proborazzi.test.record=true` writes app/build/audit-captures/tuto-panel/<device>/<lang>_<rank>_<font>.png.
 */
abstract class TutoPanelFitsTest(private val lang: String, private val rank: Int, private val font: String, private val device: String) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun panelFits() {
        val l = Lang.entries.first { it.code == lang }
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = l, englishNotes = l != Lang.FR) }
        I18n.lang = l
        val (step, title) = longest(l)[rank]
        val chapters = step.chapters.mapIndexed { i, c -> if (i == step.chapter) c.copy(title = title) else c }
        val steps = chapters[step.chapter].steps
        val s = steps[step.step]
        val ui = TutoUi(TutoProgress(step.style, step.level, emptySet(), s.id), chapters, step.chapter, step.step)
        val d = CaptureMatrixTest.DEVICES.getValue(device)
        val density = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides com.pocketstudio.ui.layoutDirectionOf(l.rtl)) {
                val local = androidx.compose.ui.platform.LocalDensity.current
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(local.density, local.fontScale * font.toFloat()),
                ) {
                    PocketStudioTheme(Palettes.Studio) {
                        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black).padding(d.chrome).background(Neon.Background), contentAlignment = Alignment.BottomCenter) {
                            TutoPanel(ui, canShow = true, actions = TutoActions(), map = tutoMap(step.style))
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val found = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT, LayoutScan.Kind.OVERLAP)).toMutableList()
        // The step's words in a box that scrolls with no bar to say so.
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange), useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false).forEach { n ->
                val range = n.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) ?: return@forEach
                val hidden = range.maxValue() / density
                if (hidden > 2f && !more()) found += LayoutScan.Finding(LayoutScan.Kind.CUT, s.textFor(step.level).take(40), "the panel's words scroll (%.0f dp unseen) with no « Lire la suite »".format(hidden))
            }
        if (CaptureMatrixTest.RECORDING) compose.onRoot().captureRoboImage("build/audit-captures/tuto-panel/$device/${lang}_${rank}_$font.png")
        val left = found.filterNot(::allowed)
        assertTrue("$device $lang rank $rank (${step.style} ${step.level} ${s.id}) font $font:\n" + left.joinToString("\n"), left.isEmpty())
    }

    private fun more() = compose.onAllNodes(androidx.compose.ui.test.hasTestTag("tuto_text_more"), useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    @After
    fun french() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
        I18n.lang = Lang.FR
    }

    /** A step of a course as its panel shows it. */
    class Placed(val style: String, val level: TutoLevel, val chapters: List<TutoChapter>, val chapter: Int, val step: Int) {
        val shown: TutoStep get() = chapters[chapter].steps[step]
    }

    companion object {
        const val RANKS = 8
        val FONTS = listOf("1.0", "1.3")

        /**
         * Findings looked at and accepted, with their reason: « Lire la suite » lies over the last line on purpose,
         * on a fade (the words go on under it, a touch scrolls them up).
         */
        private fun allowed(f: LayoutScan.Finding): Boolean {
            val cue = Regex.escape(com.pocketstudio.model.i18n.tr("Lire la suite"))
            val t = f.toString()
            return f.kind == LayoutScan.Kind.OVERLAP && (Regex("over (the touchable )?« $cue").containsMatchIn(t) || t.startsWith("OVERLAP « ${com.pocketstudio.model.i18n.tr("Lire la suite")} »"))
        }

        private fun shown(l: Lang, fr: String): String = if (l == Lang.FR) fr else I18n.table(l)[fr]?.takeIf { it.isNotEmpty() } ?: fr

        private val cache = HashMap<Lang, List<Pair<Placed, String>>>()

        /** The [RANKS] longest steps of [l] (by their words in [l]), each with the rank-th longest chapter title. */
        fun longest(l: Lang): List<Pair<Placed, String>> = synchronized(cache) {
            cache.getOrPut(l) {
                val placed = TutoCourse.STYLES.flatMap { style ->
                    val course = TutoCourse.of(style)!!
                    TutoLevel.entries.flatMap { level ->
                        val chapters = course.chaptersFor(level)
                        chapters.flatMapIndexed { c, ch -> ch.steps.indices.map { Placed(style, level, chapters, c, it) } }
                    }
                }
                val steps = placed.distinctBy { shown(l, it.shown.textFor(it.level)) }.sortedByDescending { shown(l, it.shown.textFor(it.level)).length }.take(RANKS)
                val titles = placed.map { it.chapters[it.chapter].title }.distinct().sortedByDescending { shown(l, it).length }.take(RANKS)
                steps.zip(titles)
            }
        }

        /** The pairwise cases of the whole panel matrix (the same on every run). */
        val ALL: List<List<String>> by lazy {
            Pairwise.cases(listOf(Lang.entries.map { it.code }, (0 until RANKS).map { it.toString() }, CaptureMatrixTest.DEVICES.keys.toList(), FONTS))
        }

        fun casesOf(device: String): List<Array<Any>> = ALL.filter { it[2] == device }.map { arrayOf<Any>(it[0], it[1].toInt(), it[3]) }
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class TutoPanelFitsPhone360Test(lang: String, rank: Int, font: String) : TutoPanelFitsTest(lang, rank, font, "phone360") {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases() = casesOf("phone360")
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class TutoPanelFitsPhone411Test(lang: String, rank: Int, font: String) : TutoPanelFitsTest(lang, rank, font, "phone411") {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases() = casesOf("phone411")
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h360dp-land-xxhdpi")
class TutoPanelFitsPhoneLandTest(lang: String, rank: Int, font: String) : TutoPanelFitsTest(lang, rank, font, "phoneLand") {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases() = casesOf("phoneLand")
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h1006dp-tvdpi")
class TutoPanelFitsTablet8Test(lang: String, rank: Int, font: String) : TutoPanelFitsTest(lang, rank, font, "tablet8") {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases() = casesOf("tablet8")
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class TutoPanelFitsTablet11LandTest(lang: String, rank: Int, font: String) : TutoPanelFitsTest(lang, rank, font, "tablet11Land") {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases() = casesOf("tablet11Land")
    }
}

/** Audit ultra round 1, n° 38 (seat A b): the panel in French at 200 % on a small phone (« Écouter l'exempl » was cut). */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class TutoPanelFitsBigFontTest(rank: Int) : TutoPanelFitsTest("fr", rank, "2.0", "phone360") {
    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "fr_{0}_2.0")
        fun cases() = (0 until RANKS).map { arrayOf<Any>(it) }
    }
}
