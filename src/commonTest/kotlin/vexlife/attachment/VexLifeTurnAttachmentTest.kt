package vexlife.attachment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeTurnAttachmentTest {
    private fun descriptor(seed: Char, name: String = "notes.txt") =
        VexLifeTurnAttachment.Descriptor(
            ref = VexLifeTurnAttachment.refFor(seed.toString().repeat(64)),
            name = name,
            mediaKind = VexLifeTurnAttachment.MediaKind.TEXT,
            byteCount = 12,
            lineCount = 3,
            sha256 = seed.toString().repeat(64),
        )

    @Test
    fun aMessageWithoutAttachmentsIsExactlyThePersonsWords() {
        assertEquals("hello", VexLifeTurnAttachment.messageContent("hello", emptyList()))
        assertEquals(emptyList(), VexLifeTurnAttachment.refsIn("hello"))
    }

    @Test
    fun theTrailerNamesEveryAttachmentAndReadsBackInOrder() {
        val first = descriptor('a')
        val second = descriptor('b', "plan.md")
        val content = VexLifeTurnAttachment.messageContent("Read these  \n", listOf(first, second))
        assertTrue(content.startsWith("Read these\n\n" + VexLifeTurnAttachment.TrailerHeading))
        assertTrue(content.contains("- plan.md · ${second.ref} · 3 lines · 12 bytes"))
        assertEquals(listOf(first.ref, second.ref), VexLifeTurnAttachment.refsIn(content))
    }

    @Test
    fun attachmentsAloneFormANonEmptyMessage() {
        val content = VexLifeTurnAttachment.messageContent("  ", listOf(descriptor('c')))
        assertTrue(content.startsWith(VexLifeTurnAttachment.TrailerHeading))
    }

    @Test
    fun theTrailerIsDeterministicForAReplayedSend() {
        val attachments = listOf(descriptor('a'), descriptor('b'))
        assertEquals(
            VexLifeTurnAttachment.messageContent("x", attachments),
            VexLifeTurnAttachment.messageContent("x", attachments),
        )
    }

    @Test
    fun oneMessageNamesEachAttachmentOnceAndWithinTheBound() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeTurnAttachment.messageContent("x", listOf(descriptor('a'), descriptor('a')))
        }
        val tooMany = "abcdef012".map { descriptor(it) }
        assertFailsWith<IllegalArgumentException> { VexLifeTurnAttachment.messageContent("x", tooMany) }
    }

    @Test
    fun namesKeepOnlyTheLastSegmentWithoutControlOrTrailerSeparators() {
        assertEquals("notes.txt", VexLifeTurnAttachment.admitName("/Users/me/Desktop/notes.txt"))
        assertEquals("a.txt", VexLifeTurnAttachment.admitName("C:\\work\\a.txt"))
        assertEquals("ab.txt", VexLifeTurnAttachment.admitName("a\u0000b·.txt".replace("·", "")))
        assertEquals("ab.txt", VexLifeTurnAttachment.admitName("a·b.txt"))
        assertNull(VexLifeTurnAttachment.admitName("   "))
        assertNull(VexLifeTurnAttachment.admitName("dir/.."))
    }

    @Test
    fun aTrailingNewlineDoesNotOpenAnotherLine() {
        assertEquals(1, VexLifeTurnAttachment.lineCount("one\n"))
        assertEquals(2, VexLifeTurnAttachment.lineCount("one\ntwo"))
        assertEquals(0, VexLifeTurnAttachment.lineCount(""))
    }

    @Test
    fun aDescriptorRefMustDeriveFromItsDigest() {
        assertFailsWith<IllegalArgumentException> {
            descriptor('a').copy(ref = VexLifeTurnAttachment.refFor("b".repeat(64)))
        }
    }
}

// [VXG RealForever]
