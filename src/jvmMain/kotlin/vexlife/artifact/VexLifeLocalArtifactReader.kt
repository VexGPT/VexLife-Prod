package vexlife.artifact

/**
 * Product staging owner that keeps exact raw bytes and forms one bounded request projection.
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

        /**
         * Exact static-image bytes are admitted and recognized as one typed request projection.
         *
         * This owner does not decide model visibility. The service may make the projection READY
         * only when the current selected-model/runtime relationship has qualified visual input.
         * The typed adaptation itself is still not model interpretation.
         */
        data class ImageAdapted(
            val original: VexLifeLocalArtifact.OriginalArtifact,
            val projection: VexLifeLocalArtifact.StaticImageProjection,
            val reasonKey: String,
            val message: String,
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
                if (
                    readable.refusal ==
                    VexLifeLocalArtifactConversion.Refusal.NOT_TEXT
                ) {
                    when (
                        val image =
                            VexLifeStaticImageAdaptation.adapt(
                                displayName = displayName,
                                bytes = bytes,
                            )
                    ) {
                        is VexLifeStaticImageAdaptation.Result.Adapted -> {
                            val projection =
                                store.storeStaticImageProjection(
                                    artifactRef = original.artifactRef,
                                    adapterRef = image.value.adapterRef,
                                    mediaType = image.value.mediaType,
                                    widthPixels = image.value.widthPixels,
                                    heightPixels = image.value.heightPixels,
                                )
                            Result.ImageAdapted(
                                original = original,
                                projection = projection,
                                reasonKey = "workspace.status.attachment-refused",
                                message =
                                    displayName +
                                        " is a " +
                                        image.value.format.displayName +
                                        " image (" +
                                        image.value.widthPixels +
                                        "x" +
                                        image.value.heightPixels +
                                        "). Exact bytes and the typed image projection are preserved, " +
                                        "but attached-image interpretation is not currently available for " +
                                        "the selected local model/runtime.",
                            )
                        }

                        is VexLifeStaticImageAdaptation.Result.Held ->
                            if (
                                image.refusal ==
                                VexLifeStaticImageAdaptation.Refusal.NOT_IMAGE
                            ) {
                                held(
                                    original = original,
                                    readable = readable,
                                )
                            } else {
                                Result.Held(
                                    artifactRefOrNull = original.artifactRef,
                                    refusal =
                                        VexLifeLocalArtifactConversion.Refusal.UNREADABLE,
                                    reasonKey = "workspace.status.attachment-refused",
                                    message = image.reason,
                                )
                            }
                    }
                } else {
                    held(
                        original = original,
                        readable = readable,
                    )
                }
        }
    }

    private fun held(
        original: VexLifeLocalArtifact.OriginalArtifact,
        readable: VexLifeLocalArtifactConversion.Result.Held,
    ): Result.Held =
        Result.Held(
            artifactRefOrNull = original.artifactRef,
            refusal = readable.refusal,
            reasonKey = "workspace.status.attachment-refused",
            message = readable.reason,
        )
}

// [VXG RealForever]
