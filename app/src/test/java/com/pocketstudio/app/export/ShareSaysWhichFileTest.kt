package com.pocketstudio.app.export

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Tour 7 lot 5 D1: « Renvoyer » on a file of « Mes fichiers » opened Android's menu titled « Partager le morceau » for
 * the .wav and for the .m4a alike: no way to tell which one leaves. The menu's title names the file.
 */
@RunWith(RobolectricTestRunner::class)
class ShareSaysWhichFileTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun sendingAgainNamesTheFile() {
        val wav = MyExport("Mon morceau 2.wav", Uri.parse("content://media/external/audio/media/2"), 1_000_000, 0, "audio/wav")
        assertTrue(MyExports.share(app, wav))
        val chooser = shadowOf(app).nextStartedActivity
        val title = chooser.getCharSequenceExtra(Intent.EXTRA_TITLE)?.toString().orEmpty()
        assertTrue("the menu's title « $title » names the file", title.contains("Mon morceau 2.wav"))
    }

    @Test
    fun oneFileSharedNamesItSeveralKeepTheTitle() {
        assertEquals("Partager mon morceau · Mon morceau.m4a", chooserTitle("Partager mon morceau", listOf("Mon morceau.m4a")))
        assertEquals("Partager mes pistes", chooserTitle("Partager mes pistes", listOf("a.wav", "b.wav")))
    }
}
