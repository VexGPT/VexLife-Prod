package vexlife.local

import vexlife.model.heretic.HereticModelConfiguration

/**
 * Platform-neutral runtime capability consumed by the shared Product body.
 *
 * A platform realization may acquire/start/observe its runtime however its declared dependencies
 * require. Once it crosses this boundary, shared Conversation/service code receives only the
 * already-qualified setup projection plus fresh-currentness capabilities.
 */
internal interface VexLifeProdRuntimeSession {
    val result: VexLifeProdLocalSetup.Result

    fun currentQualifiedRuntimeEvidence(
        onRecovery: (() -> Unit)? = null,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence

    fun observeCurrentQualifiedRuntimeEvidence():
        HereticModelConfiguration.QualifiedRuntimeEvidence
}

// [VXG RealForever]
