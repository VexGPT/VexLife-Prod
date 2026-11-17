package vexlife.local

import java.nio.file.Path
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeState

/**
 * Platform-neutral runtime capability consumed by the shared Product body.
 *
 * A platform realization may acquire/start/observe its runtime however its declared dependencies
 * require. Once it crosses this boundary, shared Conversation/service code receives only portable
 * Product foundation truth plus fresh-currentness capabilities. Mac setup details (Python/MLX) and
 * Windows setup details (native llama.cpp) stay behind their respective implementations.
 */
internal interface VexLifeProdRuntimeSession {
    data class Foundation(
        val state: String,
        val productHome: String,
        val qualifiedRuntimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
    ) {
        init {
            require(
                state == ReadyState,
            ) {
                "runtime session requires one ready local Product foundation"
            }
            require(
                Path.of(productHome).isAbsolute,
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

    companion object {
        const val ReadyState =
            "LOCAL_HERETIC_FOUNDATION_READY"
    }
}

// [VXG RealForever]
