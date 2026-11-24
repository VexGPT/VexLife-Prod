package vexlife.local

import java.nio.file.Path
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeState

/**
 * Platform-neutral runtime capability consumed by the shared Product body.
 *
 * Concrete platform owners retain acquisition, process, qualification and recovery mechanics.
 * Conversation/service code receives only one ready Product foundation plus fresh-currentness
 * capabilities.
 */
internal interface VexLifeProdRuntimeSession :
    AutoCloseable {
    data class Foundation(
        val state: String,
        val productHome: String,
        val qualifiedRuntimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        val promptCacheEntryLimit: Int? = null,
    ) {
        init {
            require(
                state ==
                    ReadyState,
            ) {
                "runtime session requires one ready local Product foundation"
            }
            require(
                Path.of(
                    productHome,
                )
                    .isAbsolute,
            ) {
                "runtime session Product Home must be absolute"
            }
            require(
                qualifiedRuntimeEvidence.runtimeState ==
                    HereticRuntimeState.Qualified,
            ) {
                "runtime session requires qualified realization evidence"
            }
            require(
                promptCacheEntryLimit == null ||
                    promptCacheEntryLimit > 0,
            ) {
                "runtime session prompt-cache entry limit must be absent or positive"
            }
        }
    }

    val foundation: Foundation

    fun currentQualifiedRuntimeEvidence(
        onRecovery: (() -> Unit)? = null,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence

    fun observeCurrentQualifiedRuntimeEvidence():
        HereticModelConfiguration.QualifiedRuntimeEvidence

    /**
     * Compatibility alias for releasing the exact runtime represented by this Session.
     *
     * Concrete platform Sessions make successful release idempotent. Callers that own the broader
     * service lifetime should prefer [close], which is the portable terminal runtime-release
     * boundary used when the Product service itself terminates.
     */
    fun releaseSustainedRuntime() {
        close()
    }

    /**
     * Terminate the exact runtime custody represented by this Session.
     *
     * Every concrete realization must make its terminal-custody decision explicit. A realization
     * must not inherit an accidental no-op while owning a live Product runtime.
     */
    override fun close()

    companion object {
        const val ReadyState =
            "LOCAL_HERETIC_FOUNDATION_READY"
    }
}

// [VXG RealForever]
