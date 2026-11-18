package vexlife.release

import kotlinx.serialization.Serializable

/** Product-owned identity for one VexLife release, independent of provider wire representation. */
object VexLifeReleaseIdentity {
    @Serializable
    data class SourceSha(
        val value: String,
    ) {
        init {
            require(value.matches(Regex("[0-9a-f]{40}"))) {
                "VexLife release source identity must be one exact lowercase Git commit"
            }
        }
    }

    @Serializable
    data class SemanticVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
    ) : Comparable<SemanticVersion> {
        init {
            require(major >= 0 && minor >= 0 && patch >= 0) {
                "VexLife semantic version parts must be non-negative"
            }
        }

        override fun compareTo(other: SemanticVersion): Int =
            compareValuesBy(
                this,
                other,
                SemanticVersion::major,
                SemanticVersion::minor,
                SemanticVersion::patch,
            )

        override fun toString(): String =
            listOf(
                major,
                minor,
                patch,
            )
                .joinToString(
                    ".",
                )
    }

    @Serializable
    enum class Channel {
        Stable,
        Preview,
    }

    sealed interface Value {
        val sourceSha: SourceSha

        data class Stable(
            val version: SemanticVersion,
            override val sourceSha: SourceSha,
        ) : Value

        data class Preview(
            override val sourceSha: SourceSha,
        ) : Value
    }

    /**
     * Typed serialization/control coordinate for one exact release identity.
     *
     * Provider JSON keys and UI dataset names never become the identity itself.
     */
    @Serializable
    data class Coordinate(
        val channel: Channel,
        val sourceSha: SourceSha,
        val semanticVersion: SemanticVersion?,
    ) {
        init {
            require(
                when (channel) {
                    Channel.Stable -> semanticVersion != null
                    Channel.Preview -> semanticVersion == null
                },
            ) {
                "release coordinate channel/version relationship is invalid"
            }
        }

        fun value(): Value =
            when (channel) {
                Channel.Stable ->
                    Value.Stable(
                        version =
                            requireNotNull(
                                semanticVersion,
                            ),
                        sourceSha = sourceSha,
                    )

                Channel.Preview ->
                    Value.Preview(
                        sourceSha = sourceSha,
                    )
            }

        companion object {
            fun from(value: Value): Coordinate =
                when (value) {
                    is Value.Stable ->
                        Coordinate(
                            channel = Channel.Stable,
                            sourceSha = value.sourceSha,
                            semanticVersion = value.version,
                        )

                    is Value.Preview ->
                        Coordinate(
                            channel = Channel.Preview,
                            sourceSha = value.sourceSha,
                            semanticVersion = null,
                        )
                }
        }
    }

    @Serializable
    data class HttpsPage(
        val value: String,
    ) {
        init {
            require(value.startsWith("https://") && value.length > "https://".length) {
                "VexLife release page must use one non-empty HTTPS URL"
            }
        }
    }

    data class Published(
        val identity: Value,
        val publishedAtEpochMillis: Long,
        val page: HttpsPage,
    ) {
        init {
            require(publishedAtEpochMillis >= 0) {
                "published VexLife release requires a non-negative publication instant"
            }
        }
    }

    fun coordinate(value: Value): Coordinate =
        Coordinate.from(
            value,
        )

    fun identifier(value: Value): String =
        when (value) {
            is Value.Stable -> value.version.toString()
            is Value.Preview -> value.sourceSha.value.take(7)
        }
}

// [VXG RealForever]
