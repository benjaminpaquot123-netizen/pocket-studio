package com.pocketstudio.app

import com.pocketstudio.app.library.PackStore
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class PackStoreTest {
    @Test
    fun thePinnedIndexIsTheOneTheModelTestsRead() {
        // The copy of the published index.json that SoundPacksTest parses must be the pinned one.
        val copy = File("../project-model/src/test/resources/packs-index.json").readBytes()
        val pin = PackStore.INDEX!!
        assertEquals(pin.sizeBytes, copy.size.toLong())
        val sha = MessageDigest.getInstance("SHA-256").digest(copy).joinToString("") { "%02x".format(it) }
        assertEquals(pin.sha256, sha)
        assertEquals(true, pin.url.contains(PackStore.RELEASE))
    }
}
