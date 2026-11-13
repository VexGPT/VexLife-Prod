package vexlife.service

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile

/**
 * Read-only convergence of current host, Home, model-artifact and startup runtime truth for the
 * human product surface.
 *
 * This projector performs no setup, repair, acquisition, runtime start, model selection or Home
 * mutation. It consumes observations from their current owners and makes unknowns explicit.
 */
object VexLifeProdCurrentState {
    internal data class Hooks(
        val observeHost: () -> HereticMacHostProfile.Result = {
            HereticMacHostProfile.observeCurrent()
        },
        val observeArtifacts: (
            productHome: Path,
        ) -> HereticArtifactMaterialization.Observation = {
            productHome ->
            HereticArtifactMaterialization.observeCurrent(
                productHome,
            )
        },
        val observeFreeStorageBytes: (
            productHome: Path,
        ) -> Long? = {
            productHome ->
            try {
                Files
                    .getFileStore(
                        productHome,
                    )
                    .usableSpace
            } catch (
                _: Exception,
            ) {
                null
            }
        },
        val now: () -> Instant = {
            Instant.now()
        },
    )

    fun observe(
        setup: VexLifeProdLocalSetup.Result,
    ): VexLifeProdServiceContract.CurrentStateProjection =
        observeWithHooks(
            setup = setup,
            hooks = Hooks(),
        )

    internal fun observeWithHooks(
        setup: VexLifeProdLocalSetup.Result,
        hooks: Hooks,
    ): VexLifeProdServiceContract.CurrentStateProjection {
        val productHome =
            Path.of(
                setup.productHome,
            )
        require(
            productHome.isAbsolute,
        ) {
            "current-state projection requires one absolute VexLife Home"
        }

        val host =
            hooks.observeHost()
        val artifacts =
            hooks.observeArtifacts(
                productHome,
            )
        val runtime =
            setup.qualifiedRuntimeEvidence

        return VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion =
                VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt =
                hooks.now()
                    .toString(),
            homePath =
                productHome.toString(),
            homeDisposition =
                setup.homeDisposition,
            platform =
                host.observation.platform,
            architecture =
                host.observation.architecture,
            chipModel =
                host.observation.chipModel,
            hostEligibilityState =
                host.state
                    .name
                    .uppercase(),
            hostEligibilityReason =
                host.reason,
            totalMemoryBytes =
                host.observation.totalMemoryBytes,
            modelMinimumMemoryBytes =
                HereticMacHostProfile.MinimumMemoryBytes,
            freeStorageBytes =
                hooks.observeFreeStorageBytes(
                    productHome,
                ),
            activeModelRef =
                runtime.modelRef,
            activeModelProfileRef =
                runtime.modelProfileRef,
            runtimeState =
                runtime.runtimeState
                    .name
                    .uppercase(),
            runtimeCurrentness =
                "STARTUP_QUALIFIED_NOT_REOBSERVED",
            runtimeReceiptRef =
                runtime.runtimeReceiptRef,
            modelArtifactState =
                artifacts.state,
            modelArtifactTotalBytes =
                artifacts.totalBytes,
            modelArtifactVerifiedBytes =
                artifacts.verifiedBytes,
            modelArtifactMissingBytes =
                artifacts.missingBytes,
            modelArtifactInvalidBytes =
                artifacts.invalidBytes,
            effectsPerformed =
                false,
        )
    }
}

// [VXG RealForever]
