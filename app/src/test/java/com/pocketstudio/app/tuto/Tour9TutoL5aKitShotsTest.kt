package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * Tour 9 lot L5a on the seats' phone (360 dp, 100 % and 130 %, Simple and Pro): the disco ready step while the kit's
 * card is offered (F061: no « kit chargé », « Plus tard » named), and the acid_techno 303 step with its plain lines
 * (F065, Simple only: 303, Accent, Slide).
 */
abstract class Tour9TutoL5aKitShots(private val name: String) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() {
        TutoTestHooks.packs = null
        TutoTestHooks.kitOffer = true
        h.finish()
    }

    /**
     * The style's packs in a catalogue, none on the phone: the kit's card is offered (« Télécharger » / « Plus tard »)
     * without the network, the same in every run (review L5a: the real catalogue made the ready shots order-dependent).
     */
    private fun catalogue(style: String) {
        val s = TutoTarget.of(style)!!.style
        val ids = (s.lines.filter { it.voice == null }.mapNotNull { s.kit[it.role] } + s.id).distinct()
        val entries = ids.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_500_000, 12) }
        TutoTestHooks.kitOffer = true
        TutoTestHooks.packs = MutableStateFlow(PacksUi(IndexState.Ready(PackIndex(entries)), emptyList()))
    }

    private fun texts(): String = compose.onAllNodesWithTag("tuto_text", useUnmergedTree = true).fetchSemanticsNodes()
        .flatMap { n -> n.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList() }.joinToString(" ")

    private fun settle(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            compose.mainClock.advanceTimeBy(50)
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "$what: not in time" }
        }
        repeat(20) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100)); compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }
    }

    private fun shot(style: String, step: String, level: TutoLevel, simple: Boolean, file: String): String {
        catalogue(style)
        h.start(simple = simple, lang = Lang.FR, build = {
            session.edit { TutoTarget.of(style)!!.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress(style, level, current = step))
        })
        settle("the panel") { texts().isNotBlank() }
        compose.onRoot().captureRoboImage("screenshots/tour9_l5a_${file}_${if (simple) "simple" else "pro"}_$name.png")
        return texts()
    }

    @Test
    fun simpleReadyStepOffersTheKitLater() {
        val t = shot("disco", "ready", TutoLevel.Facile, simple = true, file = "ready")
        assertTrue(t, "Plus tard" in t && "chargé" !in t)
    }

    @Test
    fun proReadyStepOffersTheKitLater() {
        val t = shot("disco", "ready", TutoLevel.Pro, simple = false, file = "ready")
        assertTrue(t, "Plus tard" in t && "Déjà prêt" !in t)
    }

    @Test
    fun simpleAcidStepExplainsItsWords() {
        val t = shot("acid_techno", "acid_show", TutoLevel.Facile, simple = true, file = "acidwords")
        assertTrue(t, t.isNotBlank())
    }

    @Test
    fun proAcidStepKeepsItsText() {
        val t = shot("acid_techno", "acid_do", TutoLevel.Pro, simple = false, file = "acidwords")
        assertTrue(t, t.isNotBlank())
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.0f)
class Tour9TutoL5aKitShotsTest : Tour9TutoL5aKitShots("100")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Tour9TutoL5aKitShotsLargeTextTest : Tour9TutoL5aKitShots("130")
