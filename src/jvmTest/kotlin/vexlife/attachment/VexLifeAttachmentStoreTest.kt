package vexlife.attachment

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import vexlife.attachment.VexLifeAttachmentStore.Staged

class VexLifeAttachmentStoreTest {
    private fun withHome(block: (Path) -> Unit) {
        val home = Files.createTempDirectory("vexlife-attachments-")
        try {
            block(home)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun stagingTextIsContentAddressedAndIdempotent() = withHome { home ->
        val store = VexLifeAttachmentStore(home)
        val bytes = "alpha\nbeta\n".toByteArray(StandardCharsets.UTF_8)
        val first = assertIs<Staged.Admitted>(store.stage("notes.txt", bytes)).descriptor
        val again = assertIs<Staged.Admitted>(store.stage("notes.txt", bytes)).descriptor
        assertEquals(first, again)
        assertEquals(2, first.lineCount)
        assertEquals(bytes.size, first.byteCount)
        assertEquals("alpha\nbeta\n", store.text(first.ref))
        assertEquals(listOf(first), store.recent(10))
        assertEquals(1, Files.list(home.resolve("attachments/text")).use { it.count() })
    }

    @Test
    fun aByteOrderMarkIsNotPartOfTheText() = withHome { home ->
        val store = VexLifeAttachmentStore(home)
        val staged = store.stage("bom.txt", byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte(), 'x'.code.toByte()))
        assertEquals("x", store.text(assertIs<Staged.Admitted>(staged).descriptor.ref))
    }

    @Test
    fun nonTextOversizedEmptyAndUnnamedFilesAreRefusedInTheOwnersTerms() = withHome { home ->
        val store = VexLifeAttachmentStore(home)
        fun refusal(name: String, bytes: ByteArray) = assertIs<Staged.Refused>(store.stage(name, bytes)).refusal
        assertEquals(VexLifeTurnAttachment.Refusal.NOT_TEXT, refusal("a.bin", byteArrayOf(0xC3.toByte(), 0x28)))
        assertEquals(VexLifeTurnAttachment.Refusal.NOT_TEXT, refusal("a.bin", byteArrayOf('a'.code.toByte(), 0)))
        assertEquals(
            VexLifeTurnAttachment.Refusal.TOO_LARGE,
            refusal("big.txt", ByteArray(VexLifeTurnAttachment.MaximumBytes + 1) { 'a'.code.toByte() }),
        )
        assertEquals(VexLifeTurnAttachment.Refusal.EMPTY, refusal("empty.txt", ByteArray(0)))
        assertEquals(VexLifeTurnAttachment.Refusal.NAME_INVALID, refusal("  ", byteArrayOf('a'.code.toByte())))
        assertEquals(emptyList(), store.recent(10))
    }

    @Test
    fun anUnknownOrMalformedRefReadsNothing() = withHome { home ->
        val store = VexLifeAttachmentStore(home)
        assertNull(store.descriptor(VexLifeTurnAttachment.refFor("0".repeat(64))))
        assertNull(store.text("../../etc/passwd"))
    }
}

// [VXG RealForever]
