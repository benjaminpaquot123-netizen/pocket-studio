package com.pocketstudio.app.report

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * « Signaler un bug » (app/screenshots/bug_*.png): the sheet with its thumbnails, the same after a
 * crash, the 📸 chip and the crash banner; subclasses shoot them at 130 % (French, Dutch) and on a tablet.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class BugReportScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    protected open val prefix = ""
    protected open val uiScale = 1f
    protected open val lang = Lang.FR

    @Before
    fun language() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = lang, englishNotes = lang != Lang.FR) }
    }

    @After
    fun french() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            com.pocketstudio.app.ui.UiScaled(uiScale) {
                PocketStudioTheme(Palettes.Studio) {
                    Box(Modifier.fillMaxSize().background(Neon.Background)) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/${prefix}bug_$name.png")
    }

    /** A small picture of a screen: a bar, a few blocks of colour (what a capture's thumbnail looks like). */
    private fun fakeScreen(seed: Int): ImageBitmap {
        val b = Bitmap.createBitmap(108, 240, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(0xFF12141A.toInt())
        val p = Paint()
        p.color = 0xFF1F2430.toInt(); c.drawRect(0f, 0f, 108f, 22f, p)
        p.color = listOf(0xFF4DD0E1, 0xFFFFB74D, 0xFF81C784)[seed % 3].toInt(); c.drawCircle(92f, 11f, 7f, p)
        repeat(5) { i ->
            p.color = listOf(0xFF26C6DA, 0xFFE57373, 0xFF9575CD, 0xFFAED581, 0xFFFFD54F)[(i + seed) % 5].toInt()
            c.drawRect(8f, 34f + i * 34f, 20f + ((i * 37 + seed * 19) % 80), 58f + i * 34f, p)
        }
        p.color = 0xFF1F2430.toInt(); c.drawRect(0f, 214f, 108f, 240f, p)
        return b.asImageBitmap()
    }

    private val summary
        get() = listOf(
            com.pocketstudio.model.i18n.tr("Ton texte (%d caractères)", 64),
            com.pocketstudio.model.i18n.tr("La version de l'appli, le modèle du téléphone et sa version d'Android"),
            com.pocketstudio.model.i18n.tr("Le son de la dernière minute : niveau, décrochages (%1\$d), son rouvert (%2\$d)", 3, 1),
            com.pocketstudio.model.i18n.tr("Les %1\$d derniers écrans et gestes, et %2\$d erreurs", 50, 2),
            com.pocketstudio.model.i18n.tr("Captures d'écran : %d", 3),
            com.pocketstudio.model.i18n.tr("Le projet « %s » (sans ses sons)", "Nuit d'été"),
        )

    @Test
    fun sheet() = shoot("sheet") {
        BugReportContent(
            BugReportUi(
                text = "Le son a coupé quand j'ai touché ▶ dans Live, juste après le refrain.",
                autoShot = ShotUi(1, fakeScreen(0), auto = true),
                shots = listOf(ShotUi(2, fakeScreen(1)), ShotUi(3, fakeScreen(2))),
                attachProject = true, projectName = "Nuit d'été", summary = summary,
            ),
            BugReportActions(), Modifier.background(Neon.Surface),
        )
    }

    @Test
    fun sheetEmpty() = shoot("sheet_empty") {
        BugReportContent(
            BugReportUi(
                autoShot = ShotUi(1, fakeScreen(0), auto = true), projectName = "Mon projet 3",
                summary = listOf(com.pocketstudio.model.i18n.tr("Ton texte : rien d'écrit pour l'instant (dis ce que tu faisais et ce qui a cloché).")),
            ),
            BugReportActions(), Modifier.background(Neon.Surface),
        )
    }

    @Test
    fun sheetAfterCrash() = shoot("sheet_crash") {
        BugReportContent(
            BugReportUi(
                attachAuto = false, crash = true, projectName = "Nuit d'été",
                shots = (2L..6L).map { ShotUi(it, fakeScreen(it.toInt())) },
                summary = listOf(com.pocketstudio.model.i18n.tr("Le rapport du plantage de la dernière fois")),
                message = com.pocketstudio.model.i18n.tr("Une image n'a pas pu être ajoutée."),
            ),
            BugReportActions(), Modifier.background(Neon.Surface),
        )
    }

    @Test
    fun chipAndBanner() = shoot("chip_banner") {
        Column(Modifier.fillMaxSize().padding(vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            CrashBanner(send = {}, dismiss = {})
            CaptureChipContent(capture = {}, cancel = {})
        }
    }
}

/** At 130 % on a 411 dp phone (like a 316 dp one). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class BugReportScale130ScreenshotTest : BugReportScreenshotTest() {
    override val prefix = "scale130/fr_"
    override val uiScale = 1.3f
}

/** The same in Dutch at 130 % (the longest texts). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class BugReportDutchScale130ScreenshotTest : BugReportScreenshotTest() {
    override val prefix = "scale130/nl_"
    override val uiScale = 1.3f
    override val lang = Lang.NL
}

/** On an 11-inch tablet held upright. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h1280dp-hdpi")
class BugReportTabletScreenshotTest : BugReportScreenshotTest() {
    override val prefix = "tablet/"
}
