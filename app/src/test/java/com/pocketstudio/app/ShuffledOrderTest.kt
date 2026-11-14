package com.pocketstudio.app

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.model.RunnerBuilder
import java.io.File
import java.lang.reflect.Modifier
import kotlin.random.Random

/**
 * Tour 6, lot « stable »: the app's whole suite in an order drawn from a seed, in one JVM (as the CI runs it: one test
 * process, the classes one after the other). Every class's neighbours change, so a class that leaves work or state to the
 * next one is met by other classes than in the usual (alphabetical) order.
 *
 *     ./gradlew :app:testDebugUnitTest -Pshuffle=<seed>
 *
 * (app/build.gradle.kts: the property limits the run to this class and gives the seed as `ps.shuffle`; Gradle runs a
 * suite whose own class matches the filter whole.) Without the property this class runs nothing. The order is printed,
 * and the JUnit XML's timestamps keep it (`scripts/bench/ci-rouge.sh --dossier`).
 *
 *     ./gradlew :app:testDebugUnitTest -Pshuffle=list:<class>,<class>,…
 *
 * replays a red's neighbours: exactly these classes (simple or full names), in this order, in one JVM (Gradle's own
 * `--tests` filters keep the order of the class files, not the one given).
 */
@RunWith(ShuffledOrder::class)
class ShuffledOrderTest

class ShuffledOrder(klass: Class<*>, builder: RunnerBuilder) : Suite(builder, klass, ShuffledOrder.classes(klass))  {
    companion object {
        fun classes(self: Class<*>): Array<Class<*>> {
            val asked = System.getProperty("ps.shuffle") ?: return emptyArray()
            val seed = asked.toLongOrNull()
            if (seed == null && !asked.startsWith("list:")) return emptyArray()
            val root = File(self.protectionDomain.codeSource.location.toURI())
            val loader = self.classLoader
            val found = root.walkTopDown().filter { it.isFile && it.name.endsWith(".class") && '$' !in it.name }
                .map { it.relativeTo(root).path.removeSuffix(".class").replace(File.separatorChar, '.') }
                .filter { it != self.name }
                .mapNotNull { name -> runCatching { Class.forName(name, false, loader) }.getOrNull() }
                .filter(::isTestClass)
                .sortedBy { it.name }
                .toList()
            val order = if (seed != null) found.shuffled(Random(seed)) else asked.removePrefix("list:").split(',')
                .map(String::trim).filter(String::isNotEmpty)
                .map { n -> found.firstOrNull { it.name == n || it.simpleName == n } ?: error("ShuffledOrder: no test class « $n »") }
            println("ShuffledOrder seed $asked: ${order.size} classes, in this order: " + order.joinToString(" ") { it.simpleName })
            // Also beside the build's results (the test's working folder is the module's): build/shuffled-order-<seed>.txt.
            runCatching { File("build/shuffled-order-${seed ?: "list"}.txt").writeText(order.joinToString("\n") { it.name } + "\n") }
            return order.toTypedArray()
        }

        /** As Gradle's JUnit 4 scan: a concrete public class with a runner of its own or a @Test method (its own or inherited). */
        private fun isTestClass(c: Class<*>): Boolean {
            if (c.isInterface || Modifier.isAbstract(c.modifiers) || !Modifier.isPublic(c.modifiers) || c.isAnnotation) return false
            if (c.isAnnotationPresent(RunWith::class.java)) return true
            var k: Class<*>? = c
            while (k != null && k != Any::class.java) {
                if (runCatching { k.declaredMethods.any { it.isAnnotationPresent(Test::class.java) } }.getOrDefault(false)) return true
                k = k.superclass
            }
            return false
        }
    }
}
