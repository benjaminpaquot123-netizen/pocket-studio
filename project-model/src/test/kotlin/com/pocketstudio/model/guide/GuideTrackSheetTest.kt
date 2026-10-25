package com.pocketstudio.model.guide

import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LayerRole
import com.pocketstudio.model.LayerSound
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.addLayer
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.layersOf
import com.pocketstudio.model.removeLayer
import com.pocketstudio.model.setLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G3, a track's sheet: its layers and a chain copied by hand go into its history with the value
 * before (one line per gesture, a slider's drag one line), each brought back in a gesture without
 * erasing what came after; the sheet shows the layers and the lines.
 */
class GuideTrackSheetTest {
    private val song: Project by lazy {
        val style = StarterWizard.style("jump_up")!!
        GuideActions.withBaseline(StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }))
    }

    private fun id(p: Project, name: String) = p.instrumentTracks.first { it.name == name }.id

    private val air = LayerSound.synth(SoundLibrary.SYNTH.first().sound)

    @Test
    fun aLayerAddedSetAndRemovedIsInTheTracksHistoryAndComesBack() {
        val bass = id(song, "Basse")
        val key = GuideTrack.inst(bass)
        val added = GuideActions.withTrackEdit(song, song.addLayer(bass, air, LayerRole.AIR, SoundLibrary.SYNTH.first().name)!!, at = 1000L)
        assertEquals(1, added.layersOf(bass).size)
        val lines = GuideHistory.lines(added.guide.historyOf(key))
        assertEquals(1, lines.size)
        assertTrue(lines[0].text, lines[0].text.startsWith("Couche ajoutée"))
        assertFalse(lines[0].byGuide)
        // A drag of its level: many steps, one line, holding the last value.
        var dragged = added
        for (db in listOf(-7f, -8f, -9f)) dragged = GuideActions.withTrackEdit(dragged, dragged.setLayer(bass, 0, gainDb = db), at = 2000L, merge = true)
        val afterDrag = GuideHistory.lines(dragged.guide.historyOf(key))
        assertEquals(2, afterDrag.size)
        assertTrue(afterDrag[0].text, afterDrag[0].text.startsWith("Couche réglée"))
        assertEquals(-9f, dragged.layersOf(bass)[0].gainDb, 0f)
        // Removed, then brought back from the history: the layer is there again with its level.
        val removed = GuideActions.withTrackEdit(dragged, dragged.removeLayer(bass, 0), at = 3000L)
        assertTrue(removed.layersOf(bass).isEmpty())
        val removal = GuideHistory.lines(removed.guide.historyOf(key)).first()
        assertTrue(removal.text, removal.text.startsWith("Couche retirée"))
        val back = GuideActions.tryRevert(removed, removal.id, 4000L)
        assertEquals(null, back.problem)
        assertEquals(-9f, back.project.layersOf(bass).single().gainDb, 0f)
        assertTrue(GuideHistory.lines(back.project.guide.historyOf(key)).first().reverted)
        // The level line now refuses: its layer was removed and brought back, but bringing the level
        // back is still possible (the stack is what it left): -6 dB, the air's own level.
        val level = GuideActions.tryRevert(back.project, afterDrag[0].id, 5000L)
        assertEquals(null, level.problem)
        assertEquals(LayerRole.layer(air, LayerRole.AIR).gainDb, level.project.layersOf(bass).single().gainDb, 0f)
    }

    @Test
    fun bringingALayerBackNeverErasesWhatCameAfter() {
        val bass = id(song, "Basse")
        val added = GuideActions.withTrackEdit(song, song.addLayer(bass, air, LayerRole.AIR)!!, at = 1000L)
        val line = GuideHistory.lines(added.guide.historyOf(GuideTrack.inst(bass))).first()
        // Its level changed since, outside the sheet (nothing kept): bringing the addition back would erase it.
        val later = added.setLayer(bass, 0, pan = 0.5f)
        val r = GuideActions.tryRevert(later, line.id, 2000L)
        assertNotNull(r.problem)
        assertEquals(later, r.project)
    }

    @Test
    fun aChainCopiedOnAnotherTrackIsKeptInItsHistory() {
        val bass = id(song, "Basse")
        val lead = song.instrumentTracks.first { GuideRoles.of(it) == GuideRoles.LEAD && it.id != bass }.id
        val from = GuideTrack.inst(bass)
        val to = GuideTrack.inst(lead)
        val r = GuideActions.tryCopyChain(song, from, to, at = 1000L)
        assertEquals(null, r.problem)
        val strips = r.project.mixer.channels
        assertEquals(strips[InstrumentTrack.mixerChannel(bass)].inserts, strips[InstrumentTrack.mixerChannel(lead)].inserts)
        assertEquals(strips[InstrumentTrack.mixerChannel(bass)].sends, strips[InstrumentTrack.mixerChannel(lead)].sends)
        val line = GuideHistory.lines(r.project.guide.historyOf(to)).first()
        assertTrue(line.text, line.text.startsWith("Effet"))
        // Copied again: nothing changes, said.
        assertNotNull(GuideActions.tryCopyChain(r.project, from, to, 2000L).problem)
        // Brought back: the lead's own chain again.
        val back = GuideActions.tryRevert(r.project, line.id, 3000L).project
        assertEquals(song.mixer.channels[InstrumentTrack.mixerChannel(lead)].inserts, back.mixer.channels[InstrumentTrack.mixerChannel(lead)].inserts)
        assertEquals(song.mixer.channels[InstrumentTrack.mixerChannel(lead)].sends, back.mixer.channels[InstrumentTrack.mixerChannel(lead)].sends)
    }

    @Test
    fun theSheetShowsTheLayersAndTheLinesAndTheLayersCardTicks() {
        val bass = id(song, "Basse")
        val p = GuideActions.withTrackEdit(song, song.addLayer(bass, air, LayerRole.AIR, SoundLibrary.SYNTH.first().name)!!, at = 1000L)
        val report = GuideAnalyzer.analyze(p, simple = false, now = 2000L)
        val sheet = report.sheets.first { it.track.key == GuideTrack.inst(bass) }
        assertEquals(listOf(SoundLibrary.SYNTH.first().name), sheet.sound.layers)
        assertEquals(1, sheet.lines.size)
        // The Guide's own changes say their card.
        val applied = GuideActions.apply(p, "fx.bass", "dirtier", 3000L)
        val lines = GuideAnalyzer.analyze(applied, simple = false, now = 4000L).sheets.first { it.track.key == GuideTrack.inst(bass) }.lines
        assertTrue(lines.first().byGuide)
        assertNotNull(lines.first().card)
        assertTrue(lines.first().text, lines.first().text.contains("Effet"))
    }

    @Test
    fun aStackKeptInTheHistoryStaysSmall() {
        val bass = id(song, "Basse")
        var p = song
        repeat(3) { p = GuideActions.withTrackEdit(p, p.addLayer(bass, air, LayerRole.BODY)!!, at = 1000L) }
        // Three layers before and after: a few kB at most (the history holds ~48 000 characters).
        val last = p.guide.history.last()
        assertTrue("${last.chars} characters", last.chars < 3000)
        // A slot holding an effect still reads as its effect.
        assertEquals(EffectType.COMPRESSOR, GuideActions.insertOf(kotlinx.serialization.json.Json.encodeToJsonElement(Insert.serializer(), Insert(EffectType.COMPRESSOR.ordinal, EffectType.COMPRESSOR.defaults)))?.effect)
    }
}
