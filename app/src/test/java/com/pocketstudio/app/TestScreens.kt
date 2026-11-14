package com.pocketstudio.app

import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.pocketstudio.app.human.BenchPristine
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * The ViewModels a test opens, each as a screen's (a [ViewModelStore] of its own). [closeAll] clears them as the app does
 * when the screen goes, then waits for their work to end (no worker nor coroutine of the main queue in the app's code), the
 * main looper running, before the test lets its engine stand-in go.
 *
 * Tour 6, lot « stable », follow-up (CI 43531021739): a ViewModel never cleared keeps its work (the live-gain estimate, the
 * performance's and launches' watches, ...); with the engine stand-in gone it throws, or runs, in a later class.
 * `BenchPristine.afterAnOrdinaryTest` names the test that leaves it.
 */
class TestScreens {
    private val stores = ArrayList<ViewModelStore>()

    /** [make]'s ViewModel, held by a screen's store of its own. */
    fun <T : ViewModel> open(make: () -> T): T {
        val made = make()
        val store = ViewModelStore().also { stores += it }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = made as V
        }
        return ViewModelProvider(store, factory)[made.javaClass]
    }

    /** Every screen closed (their ViewModels cleared), their work waited for [capMs] at most, the main looper running. */
    fun closeAll(capMs: Long = 30_000) {
        stores.forEach { it.clear() }
        stores.clear()
        val until = System.currentTimeMillis() + capMs
        while (true) {
            val left = BenchPristine.appThreads() + BenchPristine.appMainCoroutines()
            if (left.isEmpty()) return
            check(System.currentTimeMillis() < until) { "the app's work still runs ${capMs / 1000} s after its screens closed: $left" }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
            Thread.sleep(5)
        }
    }
}
