package vexlife.platform

/**
 * Exact Windows download routing over already-published Product artifacts.
 *
 * This owner answers only which published Windows artifact corresponds to one exact Product target.
 * It does not observe the host, build artifacts, publish releases, choose a runtime, or grant support.
 */
object VexLifeWindowsDownloadRoute {
    data class Artifact(
        val artifactRef: String,
        val target: VexLifePlatformRealization.Target,
        val assetName: String,
        val sha256: String,
    ) {
        init {
            requirePortableRef(artifactRef, "artifactRef")
            require(target.platform == VexLifePlatformRealization.PlatformFamily.WINDOWS) {
                "Windows download artifact must target WINDOWS"
            }
            require(
                assetName.isNotBlank() &&
                    !assetName.contains('/') &&
                    !assetName.contains('\\'),
            ) {
                "assetName must be one safe release filename"
            }
            require(sha256.matches(Regex("[0-9a-f]{64}"))) {
                "sha256 must be one lowercase SHA-256"
            }
        }
    }

    sealed interface Resolution {
        val target: VexLifePlatformRealization.Target

        data class Available(
            val artifact: Artifact,
        ) : Resolution {
            override val target: VexLifePlatformRealization.Target = artifact.target
        }

        data class Unavailable(
            override val target: VexLifePlatformRealization.Target,
        ) : Resolution
    }

    class Catalog(
        artifacts: List<Artifact>,
    ) {
        val artifacts: List<Artifact> = artifacts.toList()

        private val byTarget: Map<VexLifePlatformRealization.Target, Artifact>

        init {
            require(this.artifacts.map(Artifact::artifactRef).distinct().size == this.artifacts.size) {
                "Windows download artifact refs must be unique"
            }
            require(this.artifacts.map(Artifact::target).distinct().size == this.artifacts.size) {
                "Windows download targets must be unique"
            }
            byTarget = this.artifacts.associateBy(Artifact::target)
        }

        fun resolve(
            target: VexLifePlatformRealization.Target,
        ): Resolution {
            require(target.platform == VexLifePlatformRealization.PlatformFamily.WINDOWS) {
                "Windows download routing requires a WINDOWS target"
            }
            return byTarget[target]
                ?.let { Resolution.Available(it) }
                ?: Resolution.Unavailable(target)
        }
    }

    private fun requirePortableRef(
        value: String,
        field: String,
    ) {
        require(value.matches(Regex("[a-z][a-z0-9.-]*"))) {
            "$field must be one portable ref"
        }
    }
}

// [VXG RealForever]
