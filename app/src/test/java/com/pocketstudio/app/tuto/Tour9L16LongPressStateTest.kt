package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.tr
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 9, lot L16, F093 (seat t9-68, zh): TalkBack said « tap it » (« 点一下 ») on the clap's ghost cell, where the
 * step asks a long press. A dotted cell a tap does not make says so.
 */
class Tour9L16LongPressStateTest {
    @After
    fun clear() = TutoHints.reset()

    @Test
    fun aLongPressCellSaysLongPress() {
        TutoHints.cells = 2 to setOf(4, 12, 15)
        TutoHints.longPress = setOf(15)
        assertEquals(tr("vide, en pointillé : appui long"), TutoHints.dottedState(2, 15))
        assertEquals(tr("vide, en pointillé : à toucher"), TutoHints.dottedState(2, 12))
        assertEquals(null, TutoHints.dottedState(1, 15))
        assertEquals(null, TutoHints.dottedState(2, 3))
    }
}
