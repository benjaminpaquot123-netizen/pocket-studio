package com.pocketstudio.app.export

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, F124: « Annuler » in « Je prépare ton morceau… » (reached from Partager) reads at WCAG AA on its dialog
 * (seat t9-70 saw « 取消 » very pale grey on the dark during the export).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class ShareCancelReadableTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    @Test
    fun annulerReadsDuringTheExportInEveryLanguage() {
        val density = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        val faults = mutableListOf<String>()
        var shown by androidx.compose.runtime.mutableStateOf(Lang.FR)
        compose.setContent {
            androidx.compose.runtime.key(shown) {
                PocketStudioTheme { ShareProgressDialog(ExportUi(running = true, progress = 0.4f), cancel = {}, dismiss = {}) }
            }
        }
        for (lang in listOf(Lang.FR, Lang.ZH_CN, Lang.AR)) {
            I18n.lang = lang
            shown = lang
            compose.waitForIdle()
            val cancel = com.pocketstudio.model.i18n.tr("Annuler")
            faults += LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CONTRAST)).filter { it.what == cancel }.map { "${lang.code}: $it" }
        }
        assertEquals(emptyList<String>(), faults)
    }
}
