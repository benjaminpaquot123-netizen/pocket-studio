package com.pocketstudio.app.human

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * The main thread's watchdog (Audit ultra §3.1, « gels du fil principal »): every 50 ms it reads
 * where the main thread is. The app holds it when the top-most frame of the app's own code (not the
 * bench's, not the libraries') stays the same method for more than [limitMs]: a blocking read, a
 * lock, a long loop. A recomposition that moves from one composable to the next is not a stall (on
 * the JVM it is slower than on a phone). Each stall is said once, with its length and its stack.
 * [paused] while the bench draws the screen for its image and scans it (its own work).
 */
internal class BenchWatchdog(private val main: Thread, private val limitMs: Long = 700) {
    val stalls = ConcurrentLinkedQueue<String>()
    @Volatile var paused = false
    @Volatile private var running = true
    private val thread = Thread({ loop() }, "bench-watchdog").apply { isDaemon = true }

    fun start() = apply { thread.start() }

    fun stop() {
        running = false
        thread.join(500)
    }

    private fun loop() {
        var frame: String? = null
        var since = 0L
        var stack: List<StackTraceElement> = emptyList()
        var reported = false
        while (running) {
            val now = System.currentTimeMillis()
            val st = if (paused) emptyArray() else main.stackTrace
            val top = topAppFrame(st)
            if (top == null || top != frame) {
                if (reported && frame != null) stalls += report(now - since, stack)
                frame = top
                since = now
                reported = false
            } else if (!reported && now - since > limitMs) {
                // Said when it ends (its length known); the stack as it was past the limit.
                reported = true
                stack = st.take(14)
            }
            try { Thread.sleep(50) } catch (_: InterruptedException) { return }
        }
        if (reported) stalls += report(System.currentTimeMillis() - since, stack)
    }

    private fun report(ms: Long, stack: List<StackTraceElement>) = "gel $ms ms\n" + stack.joinToString("\n") { "    at $it" }

    companion object {
        private val WARMUP = listOf("org.objectweb.asm.", "org.robolectric.internal.bytecode.", "org.robolectric.util.reflector.", "java.lang.ClassLoader", "jdk.internal.loader.")

        /** The top-most frame of the app's code ([Class.method]), or null when the bench's own code is above it (it waits, it draws). */
        fun topAppFrame(st: Array<StackTraceElement>): String? {
            for (f in st) {
                val c = f.className
                // The JVM loading and instrumenting a class (Robolectric's first use of it): not the app's time.
                if (WARMUP.any { c.startsWith(it) }) return null
                if (c.startsWith("com.pocketstudio.app.human.") || c.startsWith("com.pocketstudio.app.audit.")) return null
                if (c.startsWith("com.pocketstudio.")) return "$c.${f.methodName}"
            }
            return null
        }
    }
}
