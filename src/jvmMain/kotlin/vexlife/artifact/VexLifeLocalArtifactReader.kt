package vexlife.artifact

/**
 * Product staging owner that keeps exact raw bytes and forms one bounded readable projection.
 *
 * The conversion owner decides only whether the bytes are readable and how. This owner binds that
 * result to immutable artifact/projection custody.
 */
object VexLifeLocalArtifactReader {
    sealed interface Result {
        data class Ready(
            val original: VexLifeLocalArtifact.OriginalArtifact,
            val projection: VexLifeLocalArtifact.DerivedProjection,
            val format: VexLifeLocalArtifactConversion.Format,
        ) : Result

        data class Held(
            val artifactRefOrNull: String?,
            val refusal: VexLifeLocalArtifactConversion.Refusal,
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
                    refusal =
                        VexLifeLocalArtifactConversion.Refusal.UNREADABLE,
                    reasonKey =
                        "workspace.status.attachment-refused",
                    message =
                        "The file name could not be used.",
                )

        if (
            bytes.isEmpty()
        ) {
            return Result.Held(
                artifactRefOrNull = null,
                refusal =
                    VexLifeLocalArtifactConversion.Refusal.EMPTY,
                reasonKey =
                    "workspace.status.attachment-refused",
                message =
                    displayName +
                        " is empty.",
            )
        }
        if (
            bytes.size >
            VexLifeLocalArtifact.MaximumOriginalBytes
        ) {
            return Result.Held(
                artifactRefOrNull = null,
                refusal =
                    VexLifeLocalArtifactConversion.Refusal.TOO_LARGE,
                reasonKey =
                    "workspace.status.attachment-refused",
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

        // Exact bytes enter custody even when a current reader cannot derive readable text. That
        // preserves the person's file for a later earned reader without pretending it is readable now.
        val original =
            store.storeOriginal(
                bytes,
            )

        return when (
            val readable =
                VexLifeLocalArtifactConversion.readable(
                    displayName = displayName,
                    bytes = bytes,
                )
        ) {
            is VexLifeLocalArtifactConversion.Result.Readable -> {
                val projection =
                    store.storeTextProjection(
                        artifactRef =
                            original.artifactRef,
                        converterRef =
                            readable.converterRef,
                        text =
                            readable.text,
                    )
                Result.Ready(
                    original = original,
                    projection = projection,
                    format = readable.format,
                )
            }

            is VexLifeLocalArtifactConversion.Result.Held ->
                Result.Held(
                    artifactRefOrNull =
                        original.artifactRef,
                    refusal =
                        readable.refusal,
                    reasonKey =
                        "workspace.status.attachment-refused",
                    message =
                        readable.reason,
                )
        }
    }
}

// [VXG RealForever]
