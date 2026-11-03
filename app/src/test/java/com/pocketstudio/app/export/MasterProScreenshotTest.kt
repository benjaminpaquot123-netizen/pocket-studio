package com.pocketstudio.app.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.ScreenshotTest
import com.pocketstudio.app.mixer.MasterProActions
import com.pocketstudio.app.mixer.MasterProController
import com.pocketstudio.app.mixer.MasterProPanel
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.MasterPro
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.NeonCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot E (« Master pro »): the master's « Avancé » open (a free target, the chain partly in place, a
 * reference) and the export card's « Fichier final » open, on a small phone (360 dp), in French and in
 * German (long words): nothing cut (app/screenshots/masterpro_*.png).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h1800dp-xxhdpi")
class MasterProScreenshotTest : ScreenshotTest() {
    override fun wanted(name: String) = name.startsWith("masterpro_")

    private val project = MutableStateFlow(
        Project().let { p ->
            p.copy(mixer = p.mixer.copy(
                master = p.mixer.master.mapIndexed { i, ins -> if (i == 0) Insert(type = EffectType.PARAM_EQ.ordinal) else if (i == 1) Insert(type = EffectType.COMPRESSOR.ordinal) else ins },
                monoBassHz = 120f,
                pro = MasterPro(targetLufs = -11f, ceilingDb = -0.5f, attack = 0.6f, release = 1.5f, reference = "ref.wav", referenceName = "Ma référence", referenceLufs = -8.4f),
            ))
        },
    )

    private fun actions() = MasterProActions(
        MasterProController(CoroutineScope(Dispatchers.Unconfined), ApplicationProvider.getApplicationContext(), project, { _, _ -> }, {}, { _, _, _ -> }, {}, { "shot" }),
    )

    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    private fun master(lang: Lang) {
        I18n.lang = lang
        shoot("masterpro_panel" + if (lang == Lang.FR) "" else "_${lang.code}", prepare = {
            compose.onNodeWithTag("master_pro").performClick()
            compose.waitForIdle()
        }) {
            Column(Modifier.verticalScroll(rememberScrollState())) { NeonCard { MasterProPanel(-8f, actions()) {} } }
        }
    }

    private fun options(lang: Lang) {
        I18n.lang = lang
        val export = ExportUi(what = ExportWhat.Song, parts = 5, songBars = 64,
            pro = ExportOptions(rate = 96000, bits = 16, shaping = true, fromBar = 9, toBar = 24, fadeInMs = 500, trimStart = true, trimEnd = true, artist = "Benjamin"))
        shoot("masterpro_file" + if (lang == Lang.FR) "" else "_${lang.code}", prepare = {
            compose.onNodeWithTag("export_final").performClick()
            compose.waitForIdle()
        }) {
            Column(Modifier.verticalScroll(rememberScrollState())) { NeonCard { ExportOptionsPanel(export, hasSong = true, actions()) } }
        }
    }

    @Test fun masterFr() = master(Lang.FR)
    @Test fun masterDe() = master(Lang.DE)
    @Test fun optionsFr() = options(Lang.FR)
    @Test fun optionsDe() = options(Lang.DE)
}
