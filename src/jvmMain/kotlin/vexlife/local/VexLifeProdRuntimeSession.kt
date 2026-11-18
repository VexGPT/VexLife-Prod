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
        }
    }

    val foundation: Foundation

    fun currentQualifiedRuntimeEvidence(
        onRecovery: (() -> Unit)? = null,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence

    fun observeCurrentQualifiedRuntimeEvidence():
        HereticModelConfiguration.QualifiedRuntimeEvidence

    /**
     * Release a sustained runtime whose ownership was already captured at Session formation.
     *
     * Mac uses this after exact Home loss because the Home may no longer be available to
     * rediscover runtime ownership. Windows runtime custody is already closed with the service,
     * so its implementation may remain a no-op.
     */
    fun releaseSustainedRuntime() = Unit

    /** Platform owners with independent process lifetime may keep this as a no-op. */
    override fun close() = Unit

    companion object {
        const val ReadyState =
            "LOCAL_HERETIC_FOUNDATION_READY"
    }
}

// [VXG RealForever]
