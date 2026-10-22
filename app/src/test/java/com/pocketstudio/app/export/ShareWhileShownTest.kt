package com.pocketstudio.app.export

import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/**
 * « Partager mon morceau » while the user went to another app: the share sheet waits for their
 * return (Android does not open it from the background) instead of being lost.
 */
@RunWith(RobolectricTestRunner::class)
class ShareWhileShownTest {
    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun aSongFinishedInTheBackgroundIsSharedOnReturn() {
        val owner = Owner().apply { registry.currentState = Lifecycle.State.RESUMED }
        val requests = Channel<List<File>>(Channel.CONFLATED)
        val shared = mutableListOf<List<File>>()
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        scope.launch { shareWhileShown(owner.lifecycle, requests.receiveAsFlow()) { shared += it } }
        val idle = { shadowOf(Looper.getMainLooper()).idle() }

        requests.trySend(listOf(File("a.m4a"))); idle()
        assertEquals(1, shared.size)

        // The user goes to WhatsApp while the song is prepared.
        owner.registry.currentState = Lifecycle.State.CREATED; idle()
        requests.trySend(listOf(File("b.m4a"))); idle()
        assertEquals("nothing opens in the background", 1, shared.size)

        // Back in the app: the sheet opens then.
        owner.registry.currentState = Lifecycle.State.RESUMED; idle()
        assertEquals(listOf(File("b.m4a")), shared.last())
        assertEquals(2, shared.size)
        scope.cancel()
    }
}
