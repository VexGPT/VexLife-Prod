package vexlife.attachment

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.listDirectoryEntries
import kotlinx.serialization.Serializable
import vexlife.attachment.VexLifeTurnAttachment.Descriptor
import vexlife.attachment.VexLifeTurnAttachment.Refusal
import vexlife.contract.VexDurableContractFile

/**
 * Durable custody of staged turn attachments under the VexLife Product Home.
 *
 * Each attachment is one strict contract file, `attachments/text/<digest>.json`, holding its
 * [Descriptor] and exact text. Storage is content-addressed: staging the same bytes again returns
 * the same ref. [VexDurableContractFile] owns confinement, private permissions and atomic
 * replacement; this owner owns only admission and lookup.
 *
 * Staged attachments are kept because a sent message names them durably. Removing staged bytes that
 * no message ever named is a later retention decision, not something staging infers.
 */
class VexLifeAttachmentStore(
    private val productHome: Path,
) {
    @Serializable
    data class Record(
        val schemaVersion: String,
        val descriptor: Descriptor,
        val text: String,
    )

    sealed interface Staged {
        data class Admitted(val descriptor: Descriptor) : Staged

        data class Refused(val refusal: Refusal, val reason: String) : Staged
    }

    private val directory: Path = productHome.resolve("attachments").resolve("text")

    fun stage(
        rawName: String,
        bytes: ByteArray,
    ): Staged {
        val name =
            VexLifeTurnAttachment.admitName(rawName)
                ?: return Staged.Refused(Refusal.NAME_INVALID, "The file name could not be used.")
        if (bytes.isEmpty()) return Staged.Refused(Refusal.EMPTY, "$name is empty.")
        if (bytes.size > VexLifeTurnAttachment.MaximumBytes) {
            return Staged.Refused(
                Refusal.TOO_LARGE,
                "$name is larger than ${VexLifeTurnAttachment.MaximumBytes / 1024} KB.",
            )
        }
        val text =
            strictUtf8(bytes)?.removePrefix("﻿")?.takeIf { '\u0000' !in it }
                ?: return Staged.Refused(Refusal.NOT_TEXT, "$name is not plain UTF-8 text.")
        if (text.isEmpty()) return Staged.Refused(Refusal.EMPTY, "$name is empty.")

        val sha256 = sha256(bytes)
        val ref = VexLifeTurnAttachment.refFor(sha256)
        val descriptor =
            Descriptor(
                ref = ref,
                name = name,
                mediaKind = VexLifeTurnAttachment.MediaKind.TEXT,
                byteCount = bytes.size,
                lineCount = VexLifeTurnAttachment.lineCount(text),
                sha256 = sha256,
            )
        val existing = record(ref)
        if (existing == null || existing.descriptor != descriptor) {
            VexDurableContractFile.write(
                ownedRoot = productHome,
                path = pathFor(ref),
                value = Record(RecordSchema, descriptor, text),
            )
        }
        return Staged.Admitted(descriptor)
    }

    fun descriptor(ref: String): Descriptor? = record(ref)?.descriptor

    fun text(ref: String): String? = record(ref)?.text

    /** Staged attachments, most recently staged first. */
    fun recent(limit: Int): List<Descriptor> {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        return directory
            .listDirectoryEntries("*.json")
            .filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
            .sortedByDescending { Files.getLastModifiedTime(it, LinkOption.NOFOLLOW_LINKS) }
            .asSequence()
            .mapNotNull { file -> runCatching { readRecord(file)?.descriptor }.getOrNull() }
            .take(limit)
            .toList()
    }

    private fun record(ref: String): Record? {
        if (!VexLifeTurnAttachment.isRef(ref)) return null
        val file = pathFor(ref)
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return null
        return readRecord(file)?.takeIf { it.descriptor.ref == ref }
    }

    private fun readRecord(file: Path): Record? {
        val record = VexDurableContractFile.read<Record>(ownedRoot = productHome, path = file)
        return record?.takeIf {
            it.schemaVersion == RecordSchema &&
                VexLifeTurnAttachment.lineCount(it.text) == it.descriptor.lineCount
        }
    }

    private fun pathFor(ref: String): Path =
        directory.resolve(ref.removePrefix(VexLifeTurnAttachment.RefPrefix) + ".json")

    private fun strictUtf8(bytes: ByteArray): String? =
        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            null
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }

    companion object {
        const val RecordSchema = "vexlife-prod.turn-attachment/v1"
    }
}

// [VXG RealForever]
