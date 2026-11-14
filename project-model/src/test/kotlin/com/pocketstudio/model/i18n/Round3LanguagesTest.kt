package com.pocketstudio.model.i18n

import com.pocketstudio.model.NoteNames
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** Audit ultra round 3, n° 38: what a filled-in word changes around it, and the notes as each language names them. */
class Round3LanguagesTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
        I18n.everyday = false
    }

    @Test
    fun koreanParticlesFollowTheWordPutIn() {
        I18n.lang = Lang.KO
        // The seat's « “최종 파일”란 뭘까요? », « “목표 음량”란 »: after a consonant, « 이란 ».
        assertEquals("“최종 파일”이란 뭘까요?", tr("Qu'est-ce que « %s » ?", "최종 파일"))
        assertEquals("“목표 음량”이란 뭘까요?", tr("Qu'est-ce que « %s » ?", "목표 음량"))
        assertEquals("“리버브”란 뭘까요?", tr("Qu'est-ce que « %s » ?", "리버브"))
        // « 을(를) » and « 이(가) » written both ways in the catalogue: one.
        assertEquals("내 차례: “확인”을 누르세요.", tr("À toi : touche « %s ».", "확인"))
        assertEquals("내 차례: “다음”을 누르세요.", tr("À toi : touche « %s ».", "다음").let { it })
        assertEquals("내 차례: “저장하기”를 누르세요.", tr("À toi : touche « %s ».", "저장하기"))
        assertEquals("“킥”이 다른 소리를 덮어요.", tr("« %s » couvre les autres.", "킥"))
        assertEquals("“베이스”가 다른 소리를 덮어요.", tr("« %s » couvre les autres.", "베이스"))
        // A Latin name: read as Korean would say it (« Bass » 베이스, « Kick » 킥).
        assertEquals("“Bass”가 다른 소리를 덮어요.", tr("« %s » couvre les autres.", "Bass"))
        assertEquals("“Kick”이 다른 소리를 덮어요.", tr("« %s » couvre les autres.", "Kick"))
        // No combined form is left in any Korean text with a word put in.
        assertEquals("파트 A는 비어 있지 않아요.", tr("La partie %s n'est pas vide.", "A"))
        assertEquals("파트 M은 비어 있지 않아요.", tr("La partie %s n'est pas vide.", "M"))
    }

    @Test
    fun portugueseAndSpanishJoinTheArticle() {
        I18n.lang = Lang.PT_BR
        assertEquals("na parte A", tr("sur %s", "a parte A"))
        assertEquals("na música inteira", tr("sur %s", "a música inteira"))
        assertEquals("em Verso", tr("sur %s", "Verso"))
        I18n.lang = Lang.ES
        assertEquals("del estribillo", Contractions.join(Lang.ES, "de el estribillo", arrayOf("el estribillo")))
        I18n.lang = Lang.FR
        assertEquals("sur la partie A", tr("sur %s", "la partie A"))
    }

    @Test
    fun notesAsEachLanguageNamesThem() {
        I18n.lang = Lang.IT
        assertEquals("Re♭", NoteNames.withAccidentals(1, english = false, sharps = false))
        assertEquals("Re", NoteNames.pitchClass(62, english = false))
        I18n.lang = Lang.FR
        assertEquals("Ré♭", NoteNames.withAccidentals(1, english = false, sharps = false))
        I18n.lang = Lang.AR
        assertEquals("لا", NoteNames.key(9, english = false))
        assertEquals("صول♯", NoteNames.withAccidentals(8, english = false, sharps = true))
        assertEquals(true, Lang.AR.solfege)
    }

    @Test
    fun aCountSaysOneInEveryLanguage() {
        // The seats' « Timeline: 1 parts, 3 tracks » and « 1 parti »: the one form, then the language's plural.
        val one = mapOf(Lang.EN to "1 part", Lang.IT to "1 parte", Lang.DE to "1 Teil", Lang.PL to "1 część")
        for ((lang, words) in one) {
            I18n.lang = lang
            assertEquals(lang.code, words, trCount(1, "1 partie", "%d parties"))
        }
        I18n.lang = Lang.EN
        assertEquals("Timeline: 1 part, 3 tracks", tr("Ligne du temps : %1\$s, %2\$s", trCount(1, "1 partie", "%d parties"), trCount(3, "1 piste", "%d pistes")))
        I18n.lang = Lang.PL
        assertEquals("16 kroków", trCount(16, "1 pas", "%d pas"))
        assertEquals("2 takty", trCount(2, "1 mesure", "%d mesures"))
        assertEquals("Długość pętli tego pada:", tr("Ce pad boucle sur"))
    }
}
