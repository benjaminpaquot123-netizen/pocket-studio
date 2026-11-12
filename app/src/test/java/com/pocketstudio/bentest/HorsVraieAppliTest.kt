package com.pocketstudio.bentest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * « Écoute au micro » (T1b) never reaches the real app: its code lives in src/benTest only, which the app's build
 * takes for -PbenTest only; the release check refuses the robot's classes; BenStudio Test's check wants them in.
 */
class HorsVraieAppliTest {
    /** The unit tests run in the app module's folder. */
    private val app = File(".").canonicalFile.let { if (File(it, "src/benTest").isDirectory) it else File(it, "app") }
    private val root = app.parentFile

    private val files = listOf("Spectre.kt", "Guet.kt", "EcouteMicro.kt", "MicroReel.kt")

    @Test
    fun theMicrophonesCodeIsInBenStudioTestOnly() {
        val dir = File(app, "src/benTest/java/com/pocketstudio/bentest")
        for (f in files) assertTrue("$f in src/benTest", File(dir, f).isFile)
        val names = listOf("com.pocketstudio.bentest", "EcouteMicro", "MicroReel", "Courbe.balayage", "Empreinte.octaves")
        val inMain = File(app, "src/main").walkTopDown().filter { it.isFile }.filter { f -> names.any { f.readText().contains(it) } }.toList()
        assertEquals("the real app names none of it", emptyList<File>(), inMain)
    }

    @Test
    fun theBuildTakesItForBenStudioTestOnly() {
        val gradle = File(app, "build.gradle.kts").readText()
        val start = gradle.indexOf("if (benTest) {\n            getByName(\"main\")")
        assertTrue("the main source set takes src/benTest under « if (benTest) »", start >= 0)
        var depth = 0
        var end = start
        for (i in start until gradle.length) {
            if (gradle[i] == '{') depth++
            if (gradle[i] == '}') { depth--; if (depth == 0) { end = i; break } }
        }
        val all = Regex("getByName\\(\"main\"\\)[^\\n]*src/benTest").findAll(gradle).map { it.range.first }.toList()
        assertTrue(all.isNotEmpty())
        assertTrue("every « main » line naming src/benTest is inside the block", all.all { it in start..end })
        assertTrue("its manifest (the robot's activity) for -PbenTest only", Regex("if \\(benTest\\) \\{\\s*androidComponents").containsMatchIn(gradle))
    }

    @Test
    fun theApksChecksKnowTheMicrophone() {
        val jni = File(root, "scripts/check-jni.sh").readText()
        assertTrue("a release APK holding the robot is refused", jni.contains("Lcom/pocketstudio/(bentest|app/journey)/"))
        val apk = File(root, "scripts/bentest/check-apk.sh").readText()
        for (cls in listOf("bentest/EcouteMicro", "bentest/MicroReel", "bentest/Guet")) assertTrue("check-apk.sh wants $cls in BenStudio Test", apk.contains(cls))
    }
}
