package com.pocketstudio.app.human

import android.content.Context
import com.pocketstudio.app.remix.StemStore
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.songBars
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * What a seat left on the phone, for the mission's oracle (scripts/bench/seat-metrics.py, Audit ultra
 * §3.3: « réussie » is read in the state, never said by the tester): each project's file
 * (etat/projects/<id>/project.json, the open one as it is in memory), the app's preferences
 * (etat/prefs/), the shared files (etat/partage/, copied when Android's share menu got them) and
 * etat/resume.json, the facts the oracles read.
 */
internal object SeatState {
    fun save(app: Context, vm: RhythmViewModel?, dir: File, shared: List<String>) {
        val out = File(dir, "etat").apply { mkdirs() }
        val store = ProjectStore(File(app.filesDir, "projects"))
        val open = vm?.let { runCatching { it.projectId to it.project.value }.getOrNull() }
        val projects = LinkedHashMap<String, Project>()
        store.list().forEach { id -> store.load(id)?.let { projects[id] = it } }
        open?.let { (id, p) -> projects[id] = p }
        val copies = ProjectStore(File(out, "projects"))
        val list = JSONArray()
        for ((id, p) in projects) {
            runCatching { copies.save(id, p) }
            list.put(facts(id, p, id == open?.first))
        }
        File(app.filesDir, "../shared_prefs").listFiles()?.forEach { f -> f.copyTo(File(out, "prefs/${f.name}"), overwrite = true) }
        val s = SettingsStore.state.value
        val resume = JSONObject()
            .put("projects", list)
            .put("open", open?.first ?: JSONObject.NULL)
            .put("stem_sets", runCatching { StemStore.list(app).size }.getOrDefault(0))
            .put("shared", JSONArray(shared))
            .put("prefs", JSONObject().put("simpleMode", s.simpleMode).put("language", s.language.code).put("theme", s.theme.name).put("uiScale", s.uiScale.toDouble()))
        File(out, "resume.json").writeText(resume.toString(2))
    }

    private fun facts(id: String, p: Project, open: Boolean): JSONObject {
        val voice = Regex("^(Voix|Vocals)")
        val seconds = if (p.tempoBpm > 0) p.songBars * p.beatsPerBar * 60.0 / p.tempoBpm else 0.0
        return JSONObject()
            .put("id", id)
            .put("name", p.name)
            .put("open", open)
            .put("parts", p.song.size)
            .put("seconds", Math.round(seconds * 10) / 10.0)
            .put("tempo", p.tempoBpm)
            .put("remix_set", p.remix.setId ?: JSONObject.NULL)
            .put("voice_pad", p.drums.lanes.any { it.sample?.name?.let(voice::containsMatchIn) == true } || p.live.clips.any { it?.name?.let(voice::containsMatchIn) == true })
            .put("live_pads", p.live.clips.count { it != null })
            .put("stars", JSONArray(p.padLive.stars))
            .put("audio_clips", p.audioTracks.count { it.clip != null })
            .put("tracks", p.tracks.size)
            .put("synth_tracks", synthTracks(p))
    }

    /**
     * Round 1 (lot banc, mission « synthe »): each synth track (the Synth and the instrument tracks on the synth
     * engine), its sound (a key: the same sound on two tracks has the same), its notes, and whether the sound was
     * made (neither the synth's default nor a ready-made sound as it comes).
     */
    private fun synthTracks(p: Project): JSONArray {
        val out = JSONArray()
        val ready = com.pocketstudio.model.SynthPatch.PRESETS.map { it.second.values } + listOf(com.pocketstudio.model.SynthPatch.DEFAULTS)
        fun put(name: String, patch: com.pocketstudio.model.SynthPatch, notes: Int) {
            out.put(JSONObject().put("name", name).put("sound", patch.values.joinToString(",") { "%.4f".format(java.util.Locale.ROOT, it) })
                .put("notes", notes).put("edited", ready.none { it == patch.values }))
        }
        if (p.synthEngine == 0) put(p.synthTrackName, p.synth, p.synthNotes.notes.size)
        p.tracks.filter { it.engine == 0 }.forEach { put(it.name, it.synth, it.notes.notes.size) }
        return out
    }
}
