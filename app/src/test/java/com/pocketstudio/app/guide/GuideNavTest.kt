package com.pocketstudio.app.guide

import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideAction
import com.pocketstudio.model.guide.GuideLibrary
import com.pocketstudio.model.guide.GuideParcours
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** « Voir le réglage »: every screen a parcours can ask for goes somewhere, in the Mode simple too. */
class GuideNavTest {
    private fun song(id: String): Project {
        val style = StarterWizard.style(id)!!
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })
    }

    /** The Mode simple's four tabs, and the pages it opens from them (the Remix, the 303's own). */
    private val simpleTabs = setOf(AppTab.Morceau, AppTab.Rythme, AppTab.Synthe, AppTab.Envoyer, AppTab.Audio, AppTab.Acide)

    @Test
    fun everyScreenOfEveryParcoursIsKnownAndReachable() {
        val opens = (StarterWizard.STYLES.map { it.id } + GuideParcours.GENERIC).flatMap { id ->
            val parcours = GuideLibrary.forStyle(id) ?: return@flatMap emptyList()
            parcours.cards.flatMap { c -> (c.proposals + c.elsewhere).filter { it.action.type == GuideAction.OPEN }.map { o -> id to GuideOpen(o.action.spot, o.action.target.ifEmpty { c.target }) } }
        }
        assertTrue(opens.size > 500)
        val unknown = opens.map { it.second.spot }.toSet() - GuideNav.SPOTS
        assertTrue("screens the app does not know: $unknown", unknown.isEmpty())
        val songs = HashMap<String, Project>()
        for ((style, open) in opens) {
            val p = songs.getOrPut(style) { if (style == GuideParcours.GENERIC) Project() else song(style) }
            val simple = GuideNav.go(open, p, simple = true)
            if (simple is GuideGo.Tab) assertTrue("$style $open -> $simple", simple.tab in simpleTabs && simple.mixer == null)
            GuideNav.go(open, p, simple = false)
        }
    }

    /** Audit final (01 M2, 05 #9, 08 #7): a drum role's pad, the volume's button, a 303 line made. */
    @Test
    fun aDrumRoleIsItsPadTheVolumeIsItsButtonAndTheAcidIsMade() {
        val p = song("house")
        val kick = GuideNav.trackOf(p, "kick")!!.key.removePrefix("pad:").toInt()
        assertEquals(GuideGo.Pad(kick), GuideNav.go(GuideOpen("rhythm.kit", "kick"), p, simple = true))
        assertEquals(GuideGo.Kit, GuideNav.go(GuideOpen("rhythm.kit", "drums"), p, simple = true))
        assertEquals(listOf("simple_volume_*"), (GuideNav.go(GuideOpen("mixer.strip", "song"), p, simple = true) as GuideGo.Tab).spots)
        assertEquals(GuideGo.AcidGenerate, GuideNav.go(GuideOpen("acid.generate", "song"), Project(), simple = true))
    }

    @Test
    fun aTracksScreenIsThatTracks() {
        val p = song("jump_up")
        val bass = p.instrumentTracks.first { it.name == "Basse" }.id
        assertEquals(GuideGo.Sound(bass), GuideNav.go(GuideOpen("sounds.track", "bass"), p, simple = false))
        assertEquals(GuideGo.Notes(bass), GuideNav.go(GuideOpen("notes.track", "bass"), p, simple = true))
        assertEquals(GuideGo.Kit, GuideNav.go(GuideOpen("sounds.track", "kick"), p, simple = false))
        assertEquals(GuideGo.Tempo, GuideNav.go(GuideOpen("tempo", "song"), p, simple = false))
        val parts = GuideNav.go(GuideOpen("song.parts", "song"), p, simple = false) as GuideGo.Tab
        assertEquals(AppTab.Morceau, parts.tab)
        assertTrue(parts.partsView && parts.spots.isNotEmpty())
        // The master: the mixer's card in the full app, « Envoyer » in the Mode simple (no mixer there).
        assertEquals(AppTab.Mixeur, (GuideNav.go(GuideOpen("mixer.master", "master"), p, simple = false) as GuideGo.Tab).tab)
        assertEquals(AppTab.Envoyer, (GuideNav.go(GuideOpen("mixer.master", "master"), p, simple = true) as GuideGo.Tab).tab)
        // A role the song does not have: its tracks, on Structure.
        assertEquals(AppTab.Morceau, (GuideNav.go(GuideOpen("sounds.track", "vocal"), Project(), simple = false) as GuideGo.Tab).tab)
    }

    /** G5 (from « Ton morceau est prêt »): « Changer un son » opens the Lead (named for it), never the chords' track with its many notes. */
    @Test
    fun changerUnSonOpensTheTrackThatCarriesTheTune() {
        val song = song("house")
        val lead = song.tracks.first { it.name == "Lead" }
        assertEquals(lead.id, GuideNav.mainTrackOf(song))
        assertEquals(GuideGo.Sound(lead.id), GuideNav.go(GuideOpen("sounds.main", "song"), song, simple = true))
        assertEquals(null, GuideNav.mainTrackOf(Project()))
        val bass = Project(synthNotes = com.pocketstudio.model.NoteClip(notes = listOf(com.pocketstudio.model.Note(0, 4, 36), com.pocketstudio.model.Note(4, 4, 38), com.pocketstudio.model.Note(8, 4, 41))))
        assertEquals(0, GuideNav.mainTrackOf(bass))
        val withLead = bass.addTrack("Mélodie")!!.let { p ->
            p.withTrack(p.track(1).copy(notes = com.pocketstudio.model.NoteClip(notes = listOf(com.pocketstudio.model.Note(0, 2, 72), com.pocketstudio.model.Note(2, 2, 74)))))
        }
        assertEquals(1, GuideNav.mainTrackOf(withLead))
    }
}
