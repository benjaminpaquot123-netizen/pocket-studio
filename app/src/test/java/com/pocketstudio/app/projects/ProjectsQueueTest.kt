package com.pocketstudio.app.projects

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.Project
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * p5 (CI 398, 403, 410: « Chargement de la liste… » for good, no app thread at work): one catalog operation ended by a
 * stray cancellation (its model still alive) stopped the queue's only worker, and every later look at the list waited
 * forever. One operation's end never stops the ones asked after it.
 */
@RunWith(RobolectricTestRunner::class)
class ProjectsQueueTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun theListStillLoadsAfterAnOperationWasCancelledOnItsOwn() {
        val catalog = projectCatalog(app)
        catalog.create(Project(name = "Gardé"))
        val vm = ProjectsViewModel(app)
        val once = AtomicBoolean(true)
        vm.beforeOperationForTest = { if (once.getAndSet(false)) throw kotlinx.coroutines.CancellationException("stray") }
        vm.refresh()
        vm.refresh()
        val until = System.currentTimeMillis() + 10_000
        while (vm.state.value.loading || vm.state.value.projects.isEmpty()) {
            assertTrue("the list never loaded after a cancelled operation", System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue(vm.state.value.projects.any { it.name == "Gardé" })
    }
}
