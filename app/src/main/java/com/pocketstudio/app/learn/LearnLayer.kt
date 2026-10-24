package com.pocketstudio.app.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.learn.SoundGlossary
import com.pocketstudio.ui.LocalHelpListen

/** Opens the glossary from anywhere (Aide, Réglages, a « ? »): [open] with the word to look for ("" for all). */
class LearnHost(val open: (String) -> Unit = {})

/** The glossary's door for the screens; does nothing outside [LearnLayer] (tests, previews). */
val LocalLearn = staticCompositionLocalOf { LearnHost() }

/**
 * Around the whole app: the glossary « Les mots de la musique » as a page of its own over it,
 * opened through [LocalLearn], and « ▶ Écouter un exemple » in every « ? » whose word has one
 * ([LocalHelpListen]). Its examples are rendered on a private engine described by the open
 * project's own sync, as the library's demos are. Kept outside the screens on purpose: the
 * navigation can change (phase 36) without moving it.
 */
@Composable
fun LearnLayer(content: @Composable () -> Unit) {
    val learnVm: LearnViewModel = viewModel()
    val rhythmVm: RhythmViewModel = viewModel()
    DisposableEffect(learnVm, rhythmVm) {
        learnVm.sync = { p -> rhythmVm.syncEngine(p) }
        onDispose { learnVm.sync = null }
    }
    // null: closed; else the search typed.
    var query by rememberSaveable { mutableStateOf<String?>(null) }
    val host = remember { LearnHost(open = { query = it }) }
    val listen: (com.pocketstudio.ui.Help) -> (() -> Unit)? = remember {
        { help ->
            SoundGlossary.forHelp(I18n.frenchOf(help.term))?.let { word ->
                { query = word.term; learnVm.play(word) }
            }
        }
    }
    CompositionLocalProvider(LocalLearn provides host, LocalHelpListen provides listen) {
        content()
        query?.let { q ->
            val playing by learnVm.playing.collectAsStateWithLifecycle()
            val close = { learnVm.stop(); query = null }
            Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
                GlossaryPage(q, playing, GlossaryActions(search = { query = it }, toggle = learnVm::toggle, close = close))
            }
        }
    }
}
