package com.pocketstudio.bentest

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * The floating banner of « Questions à l'oreille » (docs/PLAN-TEST-APK.md, niveau 3): one question at a
 * time over the studio, ✅ and ❌ big, « Passer », an optional one-line word. It is a view added to the
 * studio's own window ([attacher]): no permission, nothing over other apps. The model is plain Kotlin:
 * the robot's thread [poser]s and waits, the buttons [repondre].
 */
object Banniere {
    data class Etat(val question: Question)

    private val _etat = MutableStateFlow<Etat?>(null)
    val etat: StateFlow<Etat?> = _etat
    private val reponses = LinkedBlockingQueue<Reponse>()

    /** Shows [question], waits at most [maxMs] for the answer (no answer: [Verdict.SANS_REPONSE]); the banner is gone after. */
    fun poser(question: Question, maxMs: Long): Reponse {
        reponses.clear()
        _etat.value = Etat(question)
        return try {
            reponses.poll(maxMs, TimeUnit.MILLISECONDS) ?: Reponse(Verdict.SANS_REPONSE)
        } finally {
            _etat.value = null
        }
    }

    /** The answer of the banner's buttons; with no question waiting it is dropped (it must not answer the next one). */
    fun repondre(verdict: Verdict, commentaire: String = "") {
        if (_etat.value != null) reponses.offer(Reponse(verdict, commentaire.trim().replace('\n', ' ').take(MOT_MAX)))
    }

    const val MOT_MAX = 140

    /** [activity]'s window gets the banner (once): a view over the studio, at the top, drawn only while a question waits. */
    fun attacher(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (root.findViewWithTag<android.view.View>(VUE_TAG) != null) return
        val view = ComposeView(activity).apply {
            tag = VUE_TAG
            setContent { PocketStudioTheme { Vue() } }
        }
        root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))
    }

    private const val VUE_TAG = "bentest_banniere_vue"

    @Composable
    fun Vue() {
        val etat by Banniere.etat.collectAsStateWithLifecycle()
        etat?.let { BanniereCarte(it.question.texte, ::repondre) }
    }
}

/** The banner's card: the question, a word (optional), ❌ ✅ and « Passer ». */
@Composable
fun BanniereCarte(texte: String, repondre: (Verdict, String) -> Unit) {
    var mot by remember(texte) { mutableStateOf("") }
    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp).background(Neon.Surface, RoundedCornerShape(16.dp)).padding(12.dp).testTag("bentest_banniere"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(texte, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary, modifier = Modifier.testTag("bentest_banniere_question"))
        OutlinedTextField(
            value = mot, onValueChange = { mot = it.take(Banniere.MOT_MAX) }, singleLine = true,
            placeholder = { Text("Un mot si tu veux (facultatif)") }, modifier = Modifier.fillMaxWidth().testTag("bentest_banniere_mot"),
            keyboardOptions = KeyboardOptions.Default,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { repondre(Verdict.NON, mot) }, modifier = Modifier.heightIn(min = 64.dp).weight(1f).testTag("bentest_banniere_non")) { Text("❌", style = MaterialTheme.typography.headlineMedium) }
            Button(onClick = { repondre(Verdict.OUI, mot) }, modifier = Modifier.heightIn(min = 64.dp).weight(1f).testTag("bentest_banniere_oui")) { Text("✅", style = MaterialTheme.typography.headlineMedium) }
        }
        TextButton(onClick = { repondre(Verdict.PASSEE, mot) }, modifier = Modifier.heightIn(min = 48.dp).testTag("bentest_banniere_passer")) { Text("Passer") }
    }
}
