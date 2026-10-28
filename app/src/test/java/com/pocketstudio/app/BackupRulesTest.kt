package com.pocketstudio.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Android's backup holds the projects: the cloud backup is capped at 25 MB and saves nothing over
 * it, so models, packs, banks and separated songs must stay out of it. Every folder or file the
 * app keeps under filesDir is classified here: a new one fails this test until it is.
 */
class BackupRulesTest {
    private val main = File("src/main")

    /**
     * The user's own work: backed up. In the cloud, the projects' compressed copies alone (their
     * sounds would pass the 25 MB cap, and then nothing at all is backed up: audit 14/19).
     */
    private val ownWork = setOf("projects-backup", "favorites.json")
    /** Moved whole from phone to phone (no cap), never in the cloud: the projects' folders and their sounds. */
    private val transferOnly = setOf("projects")
    /** Re-downloadable, rebuilt, huge or tied to this phone: never in the cloud backup. */
    private val leftOut = setOf("retours", "models", "packs", "sounds", "stems", "stems-pinned.txt", "basic-pitch.onnx", "projects-trash", "last_crash.txt", "applog.txt", "applog-previous.txt",
        // A separation running (its marker, for this phone's next launch only).
        "separation")
    /** Left out too, by name prefix: a Live take in progress (one file per project), only for this phone's next launch. */
    private val leftOutPrefixes = listOf("live-take-")

    private fun includes(file: String, section: String? = null): List<Pair<String, String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(main, "res/xml/$file"))
        val scope = section?.let { doc.getElementsByTagName(it).item(0) as Element } ?: doc.documentElement
        val nodes = scope.getElementsByTagName("include")
        return List(nodes.length) { (nodes.item(it) as Element).let { e -> e.getAttribute("domain") to e.getAttribute("path").trimEnd('/') } }
    }

    @Test
    fun theManifestPointsAtTheRules() {
        val manifest = File(main, "AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
    }

    @Test
    fun theCloudBackupHoldsTheProjectsAndNothingBig() {
        val cloud = includes("data_extraction_rules.xml", "cloud-backup")
        val files = cloud.filter { it.first == "file" }.map { it.second }.toSet()
        assertEquals(ownWork, files)
        assertTrue(cloud.any { it == "sharedpref" to "settings.xml" })
        // Latency measured on this phone is not another phone's.
        assertTrue(cloud.none { it.second == "metrics.xml" })
        // Older Android: the same.
        assertEquals(cloud.toSet(), includes("backup_rules.xml").toSet())
        // A phone-to-phone transfer has no cap: the separated songs and the trash go too, models and packs not.
        val transfer = includes("data_extraction_rules.xml", "device-transfer").filter { it.first == "file" }.map { it.second }.toSet()
        assertTrue(transfer.containsAll(ownWork + transferOnly + setOf("stems", "projects-trash")))
        assertTrue("the sounds never go to the cloud", cloud.none { it.second == "projects" })
        assertTrue(transfer.none { it in setOf("models", "packs", "sounds", "basic-pitch.onnx") })
    }

    @Test
    fun everyFileUnderFilesDirIsClassified() {
        val name = Regex("""filesDir\s*\)?\s*,\s*"([^"]+)"""")
        val used = File(main, "java").walkTopDown().filter { it.extension == "kt" }
            .flatMap { f -> name.findAll(f.readText()).map { it.groupValues[1] } }.toSet()
        assertTrue("found the app's folders", "projects" in used && "models" in used)
        val unknown = (used - ownWork - transferOnly - leftOut).filterNot { u -> leftOutPrefixes.any { u.startsWith(it) } }
        assertTrue("classify in BackupRulesTest and the backup rules: $unknown", unknown.isEmpty())
    }
}
