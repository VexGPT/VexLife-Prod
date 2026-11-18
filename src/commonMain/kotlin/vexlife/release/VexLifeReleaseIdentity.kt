package vexlife.release

/** Product-owned identity for one VexLife release, independent of GitHub wire representation. */
object VexLifeReleaseIdentity {
    data class SourceSha(
        val value: String,
    ) {
        init {
            require(value.matches(Regex("[0-9a-f]{40}"))) {
                "VexLife release source identity must be one exact lowercase Git commit"
            }
        }
    }

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
}

// [VXG RealForever]
