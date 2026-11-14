package com.pocketstudio.model.i18n

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit ultra round 2, n° 28 (seat A a, a senior in the Mode simple: « Kick, Rumble, Clap, Cowbell,
 * Shaker, Zap, Rim FX, BPM »; seat B b in Korean: « 노멀라이즈 », « 리버스 »): the Mode simple shows
 * the pads, the tempo and the takes' tools in the person's own words, in every language; the texts
 * that name a pad between quotes name it the same way. The Mode pro keeps the trade's words.
 */
class EverydayPadWordsTest {
    @After
    fun back() {
        I18n.everyday = false
        I18n.lang = Lang.FR
    }

    /** The kit's pads and the studio voices as the code names them (DrumKitInfo, padVoiceName). */
    private val trade = listOf("Kick", "Kick 2", "Clap", "Rim", "Cowbell", "Shaker", "Zap", "Kick studio", "Kick dur", "Rumble")
    private val english = Regex("""\b(Kick|Rumble|Clap|Cowbell|Shaker|Zap|Rim|BPM)\b""")

    @Test
    fun theSimpleModeNamesThePadsInFrench() {
        I18n.everyday = true
        trade.forEach { assertFalse("« $it » shows as « ${tr(it)} »", english.containsMatchIn(tr(it))) }
        assertEquals("Grosse caisse 1", tr("Kick"))
        assertEquals("Grondement grave", tr("Rumble"))
        assertEquals("Tempo", tr("BPM"))
        assertEquals("Tempo : 132 battements par minute", tr("Tempo : %d BPM", 132))
        assertEquals("Volume au max", tr("Normaliser"))
        I18n.everyday = false
        assertEquals("the Mode pro keeps its words", "Kick", tr("Kick"))
        assertEquals("BPM", tr("BPM"))
    }

    @Test
    fun everyLanguageHasItsOwnWords() {
        I18n.everyday = true
        for (lang in Lang.entries - Lang.FR) {
            I18n.lang = lang
            trade.forEach { k ->
                I18n.everyday = false
                val pro = tr(k)
                I18n.everyday = true
                // A language whose word is already an everyday one (Chinese « 拍手 » for the clap) may keep it.
                assertTrue("$lang: « $k » still « ${tr(k)} » in the Mode simple", tr(k) != pro || !Regex("[A-Za-z]").containsMatchIn(pro) && lang != Lang.KO && lang != Lang.JA)
            }
        }
        I18n.lang = Lang.KO
        assertEquals("최대 음량", tr("Normaliser"))
        assertEquals("거꾸로", tr("Inverser"))
        assertEquals("큰북 1", tr("Kick"))
    }

    /** The tutorial's « sur le pad « Rumble » » names the pad as it reads in the Mode simple. */
    @Test
    fun aQuotedPadNameFollows() {
        val key = "À toi : sur le pad « Rumble » (le grondement), touche les mêmes cases que le kick."
        I18n.everyday = true
        assertTrue(tr(key), "« Grondement grave »" in tr(key))
        I18n.lang = Lang.KO
        assertTrue(tr(key), "“낮은 울림”" in tr(key) && "럼블" !in tr(key))
        I18n.lang = Lang.EN
        assertTrue(tr(key), "“Low rumble”" in tr(key))
        I18n.everyday = false
        assertTrue(tr(key), "“Rumble”" in tr(key))
    }

    @Test
    fun aKoreanParticleFollowsTheNewName() {
        val names = setOf("마라카스", "손뼉", "큰북 2", "낮은 울림")
        assertEquals("“손뼉”을 탭", KoreanParticles.follow("“손뼉”를 탭", names))
        assertEquals("“마라카스”를 탭", KoreanParticles.follow("“마라카스”을 탭", names))
        assertEquals("“손뼉”과 “큰북 2”를", KoreanParticles.follow("“손뼉”와 “큰북 2”을", names))
        assertEquals("“낮은 울림”으로", KoreanParticles.follow("“낮은 울림”로", names))
        assertEquals("“마라카스”예요", KoreanParticles.follow("“마라카스”이에요", names))
        assertEquals("unknown names stay", "“셰이커”를", KoreanParticles.follow("“셰이커”를", names))
    }
}
