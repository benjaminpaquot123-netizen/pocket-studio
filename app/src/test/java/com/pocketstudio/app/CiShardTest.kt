package com.pocketstudio.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Lot « ci-rapide » (tour 9): the CI runs this suite on several machines side by side, `-Pshard=<k>/<n>`
 * (app/build.gradle.kts keeps the classes whose path falls in part k; .github/workflows/ci.yml gives n as TEST_PARTS).
 * The rule is written twice, in the build script and here: this test holds the two together, and looks at what the
 * rule does with the real classes, so that a part never ends up with most of the suite (the wait would be back).
 */
class CiShardTest {
    /** The build script's rule: the outer class's path, without « .class », hashed, modulo n. */
    private fun part(path: String, n: Int): Int = Math.floorMod(path.removeSuffix(".class").substringBefore('$').hashCode(), n)

    private fun classFiles(): List<String> {
        val root = File(CiShardTest::class.java.protectionDomain.codeSource.location.toURI())
        return root.walkTopDown().filter { it.isFile && it.name.endsWith(".class") }
            .map { it.relativeTo(root).path.replace(File.separatorChar, '/') }.toList()
    }

    private fun parts(): Int {
        val ci = File("../.github/workflows/ci.yml").readText()
        return Regex("""TEST_PARTS: "(\d+)"""").find(ci)?.groupValues?.get(1)?.toInt() ?: error("no TEST_PARTS in ci.yml")
    }

    @Test
    fun theBuildScriptSplitsByThisRule() {
        val script = File("build.gradle.kts").readText()
        assertTrue(
            "app/build.gradle.kts no longer splits by the rule this test checks: change both together",
            "Math.floorMod(el.relativePath.pathString.removeSuffix(\".class\").substringBefore('\$').hashCode(), n) != k - 1" in script,
        )
    }

    @Test
    fun aNestedClassRunsWithItsOuterClass() {
        val n = parts()
        val files = classFiles()
        val outer = files.filter { '$' !in it }.associate { it.removeSuffix(".class") to part(it, n) }
        val strays = files.filter { '$' in it }
            .filter { f -> outer[f.substringBefore('$')]?.let { it != part(f, n) } ?: false }
        assertEquals("nested classes away from their outer class", emptyList<String>(), strays)
    }

    @Test
    fun noPartCarriesMostOfTheSuite() {
        val n = parts()
        val counts = classFiles().filter { '$' !in it && it.endsWith("Test.class") }.groupingBy { part(it, n) }.eachCount()
        val total = counts.values.sum()
        assertTrue("too few test classes found ($total): the folder read is not the suite's", total > 200)
        for (k in 0 until n) {
            val share = (counts[k] ?: 0).toDouble() / total
            assertTrue(
                "part ${k + 1}/$n holds ${counts[k] ?: 0} of $total test classes: the split is lopsided ($counts)",
                share > 0.5 / n && share < 1.6 / n,
            )
        }
    }
}
