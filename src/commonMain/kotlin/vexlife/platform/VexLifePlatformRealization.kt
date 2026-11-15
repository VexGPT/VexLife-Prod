package vexlife.platform

/**
 * Portable target/realization vocabulary for VexLife platform construction.
 *
 * This owner answers only which source-owned implementation coordinates compose one exact target.
 * It does not observe the host, download artifacts, start processes, request permissions, choose a
 * model, or grant effect authority.
 *
 * The first concrete entry is formed in JVM source from the already-existing Apple-silicon owners.
 * Future platform work should add another exact realization rather than scatter platform branches
 * through unrelated Product owners.
 */
object VexLifePlatformRealization {
    enum class PlatformFamily {
        MACOS,
        WINDOWS,
        ANDROID,
        IOS_IPADOS,
    }

    enum class Architecture {
        ARM64,
        X86_64,
    }

    data class Target(
        val platform: PlatformFamily,
        val architecture: Architecture,
    )

    /**
     * References to implementation owners that already exist for one target.
     *
     * These are references, not copied implementation truth. The referenced owner still decides and
     * proves its own behavior/currentness.
     */
    data class DependencyRefs(
        val hostExecutionProfileRef: String,
        val modelRuntimeAdapterRef: String,
        val modelRuntimeClass: String,
        val modelTransportClass: String,
        val processSubstrateRef: String,
        val privateServiceImagePlatform: String,
    ) {
        init {
            requirePortableRef(hostExecutionProfileRef, "hostExecutionProfileRef")
            requirePortableRef(modelRuntimeAdapterRef, "modelRuntimeAdapterRef")
            require(modelRuntimeClass.isNotBlank()) {
                "modelRuntimeClass must be non-blank"
            }
            require(modelTransportClass.isNotBlank()) {
                "modelTransportClass must be non-blank"
            }
            requirePortableRef(processSubstrateRef, "processSubstrateRef")
            require(
                privateServiceImagePlatform.matches(
                    Regex("[a-z][a-z0-9-]*"),
                ),
            ) {
                "privateServiceImagePlatform must be one normalized target coordinate"
            }
        }
    }

    data class Entry(
        val realizationRef: String,
        val target: Target,
        val dependencies: DependencyRefs,
    ) {
        init {
            requirePortableRef(realizationRef, "realizationRef")
        }
    }

    sealed interface Resolution {
        val target: Target

        data class Supported(
            val entry: Entry,
        ) : Resolution {
            override val target: Target = entry.target
        }

        data class Unsupported(
            override val target: Target,
        ) : Resolution
    }

    /**
     * Exact-match resolver. There is intentionally no nearest-target or architecture fallback.
     *
     * A configuration can select an existing qualified realization; it cannot manufacture support
     * for a target whose implementation/runtime does not exist.
     */
    class Catalog(
        entries: List<Entry>,
    ) {
        val entries: List<Entry> = entries.toList()

        private val byTarget: Map<Target, Entry>

        init {
            require(this.entries.isNotEmpty()) {
                "platform realization catalog must contain at least one entry"
            }
            require(this.entries.map(Entry::realizationRef).distinct().size == this.entries.size) {
                "platform realization refs must be unique"
            }
            require(this.entries.map(Entry::target).distinct().size == this.entries.size) {
                "platform realization targets must be unique"
            }

            byTarget = this.entries.associateBy(Entry::target)
        }

        fun resolve(
            target: Target,
        ): Resolution =
            byTarget[target]
                ?.let(Resolution::Supported)
                ?: Resolution.Unsupported(target)
    }

    private fun requirePortableRef(
        value: String,
        field: String,
    ) {
        require(
            value.matches(
                Regex("[a-z][a-z0-9.-]*"),
            ),
        ) {
            "$field must be one portable ref"
        }
    }
}

// [VXG RealForever]
