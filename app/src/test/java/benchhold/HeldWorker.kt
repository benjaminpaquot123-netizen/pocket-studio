package benchhold

import java.util.concurrent.CountDownLatch

/**
 * Tour 9, L9 (OrdinaryTestsLeaveNothingTest, late start): work that is not the app's holding a coroutine worker, as a
 * loaded machine does. Outside com.pocketstudio on purpose: the bench names a worker the app's when one of its frames
 * is of this package's code, and this one must not be.
 */
class HeldWorker(private val held: CountDownLatch, private val release: CountDownLatch) : Runnable {
    override fun run() {
        held.countDown()
        release.await()
    }
}
