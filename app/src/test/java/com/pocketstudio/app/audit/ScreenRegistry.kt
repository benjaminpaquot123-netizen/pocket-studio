package com.pocketstudio.app.audit

import org.junit.Test
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Audit ultra §1: the one list of the app's screens, read from the screenshot tests themselves (every
 * class whose name ends with `ScreenshotTest`, found in the test sources, then its `@Test` methods by
 * reflection): a screen added to a screenshot test is in the registry without a line here. Each method
 * counts once, in the class that declares it (a subclass that only shoots the same screens again at
 * another size or language adds nothing; one that overrides or adds a method adds it).
 *
 * The registry feeds [ScreenScanTest] (« sans nom », « sans ? », cibles, contrastes on every screen).
 */
object ScreenRegistry {
    /** A screen: the test class that draws it and its method (« ScreenshotTest.mixer »). */
    data class Screen(val className: String, val method: String) {
        val simpleClass: String get() = className.substringAfterLast('.')
        val id: String get() = "$simpleClass.$method"
        override fun toString() = id
    }

    private val CLASS = Regex("""^(?:@[\w.]+(?:\([^)]*\))?\s+)*(?:(?:open|abstract|internal|public|private)\s+)*class\s+(\w*ScreenshotTest)\b""", RegexOption.MULTILINE)
    private val PACKAGE = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)

    /** The `*ScreenshotTest` classes of the test sources (the test's working directory is the app module). */
    fun classNames(sources: File = File("src/test/java")): List<String> =
        sources.walkTopDown().filter { it.isFile && it.extension == "kt" }.sortedBy { it.path }.flatMap { f ->
            val text = f.readText()
            val pkg = PACKAGE.find(text)?.groupValues?.get(1) ?: return@flatMap emptySequence()
            CLASS.findAll(text).map { "$pkg.${it.groupValues[1]}" }
        }.distinct().toList()

    /** Whether [c] can be run as it is (concrete, a constructor without arguments). */
    fun runnable(c: Class<*>): Boolean =
        !Modifier.isAbstract(c.modifiers) && !c.isInterface && c.constructors.any { it.parameterCount == 0 }

    /** The registry: every runnable screenshot class's own `@Test` methods, in a stable order. */
    fun screens(loader: ClassLoader = ScreenRegistry::class.java.classLoader!!): List<Screen> =
        classNames().mapNotNull { n -> runCatching { Class.forName(n, false, loader) }.getOrNull() }.filter(::runnable).flatMap { c ->
            c.declaredMethods.filter { it.isAnnotationPresent(Test::class.java) && Modifier.isPublic(it.modifiers) && it.parameterCount == 0 }
                .map { Screen(c.name, it.name) }.sortedBy { it.method }
        }

    /** [s]'s method, found on its class or a superclass. */
    fun method(c: Class<*>, s: Screen): Method = c.getMethod(s.method)
}
