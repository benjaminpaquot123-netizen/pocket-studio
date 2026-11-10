package com.pocketstudio.app.export

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

/**
 * Hands-on audit, chantier 1 (« partager et retrouver »): the screens it changed, whole (windows included), on a phone at
 * 100 % and 130 % and on the owner's tablet at 130 % (app/screenshots/partage/…): the loop's length in « Prêt à
 * partager ? » (Mode simple's page and the Pro's sheet), « Mes fichiers » full (long names) and empty, « C'est prêt »
 * with « Voir mes fichiers » after a song and after a project's .zip.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class ShareScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    protected open val uiScale = 1f
    protected open val prefix = "partage/"
    private var zone: TimeZone? = null

    /** The files' dates are written in the device's time zone: the same image on every machine. */
    @Before
    fun utc() {
        zone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun zoneBack() {
        zone?.let(TimeZone::setDefault)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            com.pocketstudio.app.ui.UiScaled(uiScale) { PocketStudioTheme { Box(Modifier.fillMaxSize().background(Neon.Background)) { content() } } }
        }
        compose.waitForIdle()
        captureScreenRoboImage("screenshots/$prefix$name.png")
    }

    private val loop = ShareSheetUi(
        "Mon morceau", seconds = 16, parts = 0, checks = listOf(ShareCheck.NoSong(8)), projectName = "Mon projet 1",
        loop = LoopPick(8, LoopPick.BARS),
    )

    /** The Mode simple's « Partager » page, no song yet: the loop's length under the duration, « Mes fichiers » under « Partager ». */
    @Test
    fun loopSimple() = shoot("loop_simple") { ShareSheetContent(loop, ShareSheetActions(toMixer = null, sendProject = {}), page = true) }

    /** The Pro's sheet, the same project, 16 bars picked: its volume target too. */
    @Test
    fun loopPro() = shoot("loop_pro") {
        ShareSheetContent(
            loop.copy(seconds = 32, checks = listOf(ShareCheck.NoSong(16)), loop = LoopPick(16, LoopPick.BARS), format = ExportFormat.Wav,
                target = com.pocketstudio.app.mixer.TargetPick(com.pocketstudio.model.TargetChoice.OFF)),
            ShareSheetActions(sendProject = {}), Modifier.background(Neon.Surface),
        )
    }

    private fun file(name: String, bytes: Long, at: Long, kind: MyExport.Kind) = MyExport(name, Uri.parse("content://media/$at"), bytes, at, null, kind)

    /** « Mes fichiers »: a long name on two lines, a project's .zip and a video said as such, « Renvoyer » on each. */
    @Test
    fun myFiles() = shoot("my_files") {
        MyExportsDialog(
            shown = listOf(
                file("Nuit-sur-la-ville-20261108-174901.pocketstudio.zip", 3_400_000, 1_794_160_141, MyExport.Kind.Project),
                file("Jump-up-DnB-bootleg-Mehdi-morceau-club-20261108-174512.wav", 44_090_000, 1_794_159_912, MyExport.Kind.Sound),
                file("Nuit-sur-la-ville-20261108-171502.mp4", 9_800_000, 1_794_158_102, MyExport.Kind.Video),
                file("Nuit-sur-la-ville-20261107-101502.m4a", 900_000, 1_794_046_502, MyExport.Kind.Sound),
            ),
        ) {}
    }

    /** « Mes fichiers », nothing shared yet: the message does not contradict who has just shared. */
    @Test
    fun myFilesEmpty() = shoot("my_files_empty") { MyExportsDialog(shown = emptyList()) {} }

    /** « C'est prêt » after « Partager »: the file, its report, OK and « Voir mes fichiers ». */
    @Test
    fun readySong() = shoot("ready_song") {
        ShareBilanDialog(
            listOf(
                shareReadyText(listOf(java.io.File("Nuit-sur-la-ville-20261108-174512.m4a")), savedInMusic = true),
                "Durée : 2 min 58", "Niveau : fort et propre",
            ),
            dismiss = {}, seeFiles = {},
        )
    }

    /** The same window after « Sauvegarder le morceau en .zip ». */
    @Test
    fun readyZip() = shoot("ready_zip") {
        ShareBilanDialog(zipSavedLines("Nuit-sur-la-ville-20261108-174901.pocketstudio.zip", kept = true), dismiss = {}, seeFiles = {})
    }
}

/** The same at 130 % on a common phone (316 dp of room). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class ShareScale130ScreenshotTest : ShareScreenshotTest() {
    override val uiScale = 1.3f
    override val prefix = "partage/130_"
}

/** The same on the owner's tablet upright (Galaxy Tab A11, 600 × 934 dp) at 130 %, as he uses it. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "sw600dp-w600dp-h934dp-port-tvdpi")
class ShareTabA11Scale130ScreenshotTest : ShareScreenshotTest() {
    override val uiScale = 1.3f
    override val prefix = "partage/tabA11_130_"
}
