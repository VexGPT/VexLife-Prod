package vexlife.artifact

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * First readable projection owner for Local Artifacts.
 *
 * This reader recognizes only strict UTF-8 text. ZIP / DOCX / XLSX / PPTX are intentionally left
 * to their converter owner; a foreign/binary input is HELD, not guessed as text.
 */
object VexLifePlainTextArtifactReader {
    const val ConverterRef =
        "converter.vexlife-prod.plain-text.v1"

    sealed interface Result {
        data class Ready(
            val original: VexLifeLocalArtifact.OriginalArtifact,
            val projection: VexLifeLocalArtifact.DerivedProjection,
        ) : Result

        data class Held(
            val artifactRefOrNull: String?,
            val reasonKey: String,
            val message: String,
        ) : Result
    }

    fun stage(
        store: VexLifeLocalArtifactStore,
        rawName: String,
        bytes: ByteArray,
    ): Result {
        val displayName =
            VexLifeLocalArtifact.admitDisplayName(
                rawName,
            )
                ?: return Result.Held(
                    artifactRefOrNull = null,
                    reasonKey = "workspace.status.attachment-refused",
                    message = "The file name could not be used.",
                )

        if (
            bytes.isEmpty()
        ) {
            return Result.Held(
                artifactRefOrNull = null,
                reasonKey = "workspace.status.attachment-refused",
                message = displayName + " is empty.",
            )
        }
        if (
            bytes.size >
            VexLifeLocalArtifact.MaximumOriginalBytes
        ) {
            return Result.Held(
                artifactRefOrNull = null,
                reasonKey = "workspace.status.attachment-refused",
                message =
                    displayName +
                        " is larger than " +
                        (
                            VexLifeLocalArtifact.MaximumOriginalBytes /
                                (1024 * 1024)
                        ) +
                        " MB.",
            )
        }

        val original =
            store.storeOriginal(
                bytes,
            )
        val text =
            strictUtf8(
                bytes,
            )
                ?.removePrefix(
                    "\uFEFF",
                )
                ?.takeIf {
                    '\u0000' !in
                        it
                }
                ?: return Result.Held(
                    artifactRefOrNull = original.artifactRef,
                    reasonKey = "workspace.status.attachment-refused",
                    message =
                        displayName +
                            " is not plain UTF-8 text yet. ZIP, DOCX, XLSX and PPTX support is the next reader slice.",
                )

        if (
            text.isEmpty()
        ) {
            return Result.Held(
                artifactRefOrNull = original.artifactRef,
                reasonKey = "workspace.status.attachment-refused",
                message = displayName + " has no readable text.",
            )
        }
        if (
            text.toByteArray(
                StandardCharsets.UTF_8,
            ).size >
            VexLifeLocalArtifact.MaximumProjectionBytes
        ) {
            return Result.Held(
                artifactRefOrNull = original.artifactRef,
                reasonKey = "workspace.status.attachment-refused",
                message =
                    displayName +
                        " holds more than " +
                        (
                            VexLifeLocalArtifact.MaximumProjectionBytes /
                                (1024 * 1024)
                        ) +
                        " MB of readable text.",
            )
        }

        val projection =
            store.storeTextProjection(
                artifactRef = original.artifactRef,
                converterRef = ConverterRef,
                text = text,
            )

        return Result.Ready(
            original = original,
            projection = projection,
        )
    }

    private fun strictUtf8(
        bytes: ByteArray,
    ): String? =
        try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(
                    CodingErrorAction.REPORT,
                )
                .onUnmappableCharacter(
                    CodingErrorAction.REPORT,
                )
                .decode(
                    ByteBuffer.wrap(
                        bytes,
                    ),
                )
                .toString()
        } catch (
            _: CharacterCodingException,
        ) {
            null
        }
}

// [VXG RealForever]
