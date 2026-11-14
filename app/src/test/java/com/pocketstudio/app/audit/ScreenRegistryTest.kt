package com.pocketstudio.app.audit

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Audit ultra §1: the registry of the screens finds every screenshot class and their screens (cheap: no screen drawn). */
class ScreenRegistryTest {
    @Test
    fun everyScreenshotClassIsInTheRegistry() {
        val names = ScreenRegistry.classNames()
        val loader = javaClass.classLoader!!
        val missing = names.filter { runCatching { Class.forName(it, false, loader) }.isFailure }
        assertTrue("screenshot classes the registry cannot load: $missing", missing.isEmpty())
        val screens = ScreenRegistry.screens()
        // Each runnable class gives at least one screen of its own, or inherits all of its parent's (a size or a language).
        val runnable = names.map { Class.forName(it, false, loader) }.filter(ScreenRegistry::runnable)
        assertTrue("${runnable.size} classes", runnable.size >= 30)
        assertTrue("${screens.size} screens", screens.size >= 250)
        assertTrue("one id per screen", screens.map { it.id }.distinct().size == screens.size)
        File("build/recensement").mkdirs()
        File("build/recensement/registry.txt").writeText(screens.joinToString("\n", postfix = "\n"))
    }
}
