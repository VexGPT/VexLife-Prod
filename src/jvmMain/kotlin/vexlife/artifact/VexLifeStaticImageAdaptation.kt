package vexlife.artifact

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream

/**
 * Deterministic first static-image adaptation over exact Local Artifact bytes.
 *
 * This owner recognizes PNG/JPEG from bytes and reads only image metadata. It never decodes a
 * raster, interprets image meaning, calls a model, changes Vision state, or creates Memory.
 *
 * Permanent:
 * STATIC_IMAGE_SIGNATURE != FILENAME
 * IMAGE_METADATA != VISUAL_INTERPRETATION
 * IMAGE_ADAPTATION != MODEL_REACH
 */
object VexLifeStaticImageAdaptation {
    const val AdapterRef =
        "adapter.vexlife-prod.static-image-metadata.v1"

    enum class Format(
        val mediaType: String,
        val displayName: String,
    ) {
        PNG(
            mediaType = "image/png",
            displayName = "PNG",
        ),
        JPEG(
            mediaType = "image/jpeg",
            displayName = "JPEG",
        ),
        ;

        internal fun admitsReaderName(
            name: String,
        ): Boolean =
            when (
                this
            ) {
                PNG ->
                    name.equals(
                        "png",
                        ignoreCase = true,
                    )

                JPEG ->
                    name.equals(
                        "jpeg",
                        ignoreCase = true,
                    ) ||
                        name.equals(
                            "jpg",
                            ignoreCase = true,
                        )
            }
    }

    data class Value(
        val adapterRef: String = AdapterRef,
        val format: Format,
        val widthPixels: Int,
        val heightPixels: Int,
    ) {
        init {
            require(
                adapterRef ==
                    AdapterRef &&
                    widthPixels >
                    0 &&
                    heightPixels >
                    0,
            ) {
                "static image adaptation is invalid"
            }
        }

        val mediaType: String
            get() =
                format.mediaType
    }

    enum class Refusal {
        EMPTY,
        TOO_LARGE,
        NOT_IMAGE,
        UNREADABLE,
    }

    sealed interface Result {
        data class Adapted(
            val value: Value,
        ) : Result

        data class Held(
            val refusal: Refusal,
            val reason: String,
        ) : Result
    }

    fun adapt(
        displayName: String,
        bytes: ByteArray,
    ): Result {
        if (
            bytes.isEmpty()
        ) {
            return Result.Held(
                refusal = Refusal.EMPTY,
                reason = displayName + " is empty.",
            )
        }
        if (
            bytes.size >
            VexLifeLocalArtifact.MaximumOriginalBytes
        ) {
            return Result.Held(
                refusal = Refusal.TOO_LARGE,
                reason =
                    displayName +
                        " is larger than the current " +
                        (
                            VexLifeLocalArtifact.MaximumOriginalBytes /
                                (1024 * 1024)
                        ) +
                        " MB Local Artifacts bound.",
            )
        }

        val format =
            formatOf(
                bytes,
            )
                ?: return Result.Held(
                    refusal = Refusal.NOT_IMAGE,
                    reason = displayName + " has no admitted PNG/JPEG content signature.",
                )

        return metadata(
            displayName = displayName,
            bytes = bytes,
            format = format,
        )
    }

    private fun metadata(
        displayName: String,
        bytes: ByteArray,
        format: Format,
    ): Result =
        try {
            val input =
                MemoryCacheImageInputStream(
                    ByteArrayInputStream(
                        bytes,
                    ),
                )

            input.use {
                val readers =
                    ImageIO.getImageReaders(
                        input,
                    )
                require(
                    readers.hasNext(),
                ) {
                    "no image metadata reader accepted the content"
                }

                val reader =
                    readers.next()
                try {
                    reader.setInput(
                        input,
                        true,
                        true,
                    )
                    require(
                        format.admitsReaderName(
                            reader.formatName,
                        ),
                    ) {
                        "image reader format disagrees with the content signature"
                    }
                    Result.Adapted(
                        Value(
                            format = format,
                            widthPixels =
                                reader.getWidth(
                                    0,
                                ),
                            heightPixels =
                                reader.getHeight(
                                    0,
                                ),
                        ),
                    )
                } finally {
                    reader.dispose()
                }
            }
        } catch (
            _: Exception,
        ) {
            Result.Held(
                refusal = Refusal.UNREADABLE,
                reason =
                    displayName +
                        " has a " +
                        format.displayName +
                        " signature, but its image metadata is unreadable.",
            )
        }

    private fun formatOf(
        bytes: ByteArray,
    ): Format? =
        when {
            bytes.startsWith(
                byteArrayOf(
                    0x89.toByte(),
                    0x50,
                    0x4e,
                    0x47,
                    0x0d,
                    0x0a,
                    0x1a,
                    0x0a,
                ),
            ) ->
                Format.PNG

            bytes.size >=
                2 &&
                bytes[
                    0
                ] ==
                0xff.toByte() &&
                bytes[
                    1
                ] ==
                0xd8.toByte() ->
                Format.JPEG

            else ->
                null
        }

    private fun ByteArray.startsWith(
        prefix: ByteArray,
    ): Boolean {
        if (
            size <
            prefix.size
        ) {
            return false
        }
        prefix.indices.forEach {
            index ->
            if (
                this[
                    index
                ] !=
                prefix[
                    index
                ]
            ) {
                return false
            }
        }
        return true
    }
}

// [VXG RealForever]
