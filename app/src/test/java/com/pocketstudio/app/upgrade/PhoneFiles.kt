package com.pocketstudio.app.upgrade

import android.content.Context
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Audit ultra §5 bis: a phone's own files as a version of the app left them, put back under a fresh
 * install (synthetic: nothing of Benjamin's). `src/test/resources/upgrade/<tag>/`: `projects/<id>/project.json`
 * as that version's own code wrote them (scripts/upgrade-fixtures/), `shared_prefs/<name>.xml` as Android
 * keeps that version's settings; the settings are put back through the SharedPreferences themselves (the
 * test's application has read them already: a file copied under it would not be seen).
 */
object PhoneFiles {
    val ROOT = File("src/test/resources/upgrade")

    fun install(context: Context, tag: String) {
        val from = File(ROOT, tag)
        val projects = File(context.filesDir, "projects")
        projects.deleteRecursively()
        File(from, "projects").copyRecursively(projects, overwrite = true)
        File(from, "shared_prefs").listFiles().orEmpty().forEach { putPrefs(context, it.nameWithoutExtension, it) }
    }

    /** The values of a SharedPreferences file ([xml], Android's own format) written back through the API. */
    fun putPrefs(context: Context, name: String, xml: File) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml)
        val e = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val n = nodes.item(i) as? Element ?: continue
            val key = n.getAttribute("name")
            when (n.tagName) {
                "boolean" -> e.putBoolean(key, n.getAttribute("value").toBoolean())
                "int" -> e.putInt(key, n.getAttribute("value").toInt())
                "long" -> e.putLong(key, n.getAttribute("value").toLong())
                "float" -> e.putFloat(key, n.getAttribute("value").toFloat())
                "string" -> e.putString(key, n.textContent)
            }
        }
        e.commit()
    }

    /** The beat every fixture holds (« projet-1 »): its name, tempo, swing, kick and snare, two mixer moves. */
    fun assertTheBeat(p: Project, what: String) {
        assertEquals("$what: name", "Mon beat", p.name)
        assertEquals("$what: tempo", 128.0, p.tempoBpm, 1e-9)
        assertEquals("$what: swing", 0.2, p.swing, 1e-6)
        val drums = p.allScenes[p.scene].drums
        assertEquals("$what: kick", setOf(0, 4, 8, 12), drums.lanes[0].hits.keys)
        assertEquals("$what: snare", setOf(4, 12), drums.lanes[1].hits.keys)
        assertEquals("$what: the snare's strength", 0.8f, drums.lanes[1].hits.getValue(4).velocity, 1e-6f)
        assertEquals("$what: the drums' fader", 0.65f, p.mixer.channels[0].volume, 1e-6f)
        assertTrue("$what: the muted channel", p.mixer.channels[1].mute)
    }

    /** « notes »: the beat with four notes on the synth (every fixture from v0.9.95). */
    fun assertTheNotes(p: Project, what: String) {
        assertEquals("$what: name", "Ma mélodie", p.name)
        val notes = (p.allScenes.flatMap { it.synthNotes.notes } + p.synthNotes.notes).map { it.start to it.pitch }.toSet()
        assertEquals("$what: the four notes", setOf(0 to 60, 4 to 63, 8 to 67, 12 to 70), notes)
    }
}
