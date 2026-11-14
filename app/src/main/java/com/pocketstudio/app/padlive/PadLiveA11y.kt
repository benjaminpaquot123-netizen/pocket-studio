package com.pocketstudio.app.padlive

import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.onClick
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.PadLive

/**
 * Pad Live for TalkBack (lot B3): what a pad says, the same on Pad Live and in « Fête »: its sound,
 * how it plays, whether it plays, and where it is (« Klaxon, coup, prêt, pad A9 »). A double tap
 * plays it as a finger does (down then up: a « tenir » pad plays while TalkBack holds it).
 */
internal fun padDescription(pad: Int, clip: LiveClip?, state: Int): String {
    val where = com.pocketstudio.model.i18n.tr("pad %s", padName(pad))
    if (clip == null) return com.pocketstudio.model.i18n.tr("vide") + ", " + where
    val what = when (state) {
        2 -> com.pocketstudio.model.i18n.tr("joue")
        1 -> com.pocketstudio.model.i18n.tr("en attente")
        else -> com.pocketstudio.model.i18n.tr("prêt")
    }
    val group = if (clip.group > 0) ", " + com.pocketstudio.model.i18n.tr("Groupe %d", clip.group) else ""
    return "${DefaultNames.shownSample(clip.name)}, ${modeWord(PadLive.modeOf(clip))}$group, $what, $where"
}

/** A double tap with TalkBack on a pad that holds a sound: a finger's touch (down, then up). */
internal fun padActions(receiver: SemanticsPropertyReceiver, pad: Int, has: Boolean, touch: (Int, Boolean) -> Unit) {
    if (!has) return
    receiver.onClick {
        touch(pad, true)
        touch(pad, false)
        true
    }
}
