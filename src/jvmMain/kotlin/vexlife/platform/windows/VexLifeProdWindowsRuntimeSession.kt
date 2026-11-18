package vexlife.platform.windows

import java.nio.file.Path
import vexlife.conversation.DirectConversationStore
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.local.VexLifeProdSetupProgress
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore

/**
 * Windows realization of the portable Product runtime Session.
 *
 * The selected model/realization are already decided before this owner runs. This owner converges
 * only the exact Windows runtime dependencies/process, binds its qualified evidence to the Home, and
 * retains process custody for the shared Product service.
 */
internal object VexLifeProdWindowsRuntimeSession {
    internal data class RuntimeHandle(
        val evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        val observeCurrent:
            () -> HereticModelConfiguration.QualifiedRuntimeEvidence,
        val closeRuntime: () -> Unit,
    )

    internal data class Hooks(
        val observeMaterialization:
            (
                productHome: Path,
                realization: HereticRuntimeRealization.Value,
            ) -> VexLifeWindowsRuntimeMaterialization.Observation =
            VexLifeWindowsRuntimeMaterialization::observe,
        val ensureMaterialization:
            (
                productHome: Path,
                allowAcquisition: Boolean,
                realization: HereticRuntimeRealization.Value,
            ) -> VexLifeWindowsRuntimeMaterialization.Result =
            { productHome, allowAcquisition, realization ->
                VexLifeWindowsRuntimeMaterialization.ensure(
                    productHome = productHome,
                    allowAcquisition = allowAcquisition,
                    realization = realization,
                )
            },
        val startRuntime:
            (
                productHome: Path,
                realization: HereticRuntimeRealization.Value,
            ) -> RuntimeHandle =
            { productHome, realization ->
                val started =
                    VexLifeWindowsHereticRuntime.start(
                        productHome = productHome,
                        realization = realization,
                    )
                RuntimeHandle(
                    evidence =
                        VexLifeWindowsHereticRuntime.modelConfigurationEvidence(
                            started.evidence,
                        ),
                    observeCurrent = {
                        VexLifeWindowsHereticRuntime.modelConfigurationEvidence(
                            VexLifeWindowsHereticRuntime.observeCurrent(
                                started,
                            ),
                        )
                    },
                    closeRuntime =
                        started::close,
                )
            },
    )

    fun establish(
        productHome: Path,
        realization: HereticRuntimeRealization.Value,
        allowAcquisition: Boolean,
        allowRuntimeStart: Boolean,
        progress: VexLifeProdSetupProgress = VexLifeProdSetupProgress.Silent,
        hooks: Hooks = Hooks(),
    ): VexLifeProdRuntimeSession {
        require(
            productHome.isAbsolute,
        ) {
            "Windows runtime Session requires one absolute Vex Home"
        }
        require(
            realization.target ==
                HereticRuntimeRealization.WindowsX64,
        ) {
            "Windows runtime Session requires the exact Windows x64 realization"
        }

        progress.phase(
            VexLifeProdSetupProgress.Phase.CheckingHome,
        )
        val home =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    productHome.normalize(),
                ),
            ) {
                "Windows runtime Session requires one existing canonical Vex Home"
            }
        val canonicalHome =
            Path.of(
                home.productHome,
            )

        convergeConversationWriter(
            canonicalHome,
        )

        val modelConfiguration =
            FileHereticModelConfigurationStore(
                canonicalHome,
            )
        val selected =
            modelConfiguration.read(
                home.manifest,
            )
        val selectedValue =
            requireNotNull(
                (
                    selected as?
                        FileHereticModelConfigurationStore.Snapshot.Heretic
                )
                    ?.value,
            ) {
                "Windows runtime Session requires one explicit Product model selection"
            }
        require(
            selectedValue.modelRef ==
                realization.modelRef,
        ) {
            "Windows runtime Session realization differs from the Home's selected model"
        }

        progress.phase(
            VexLifeProdSetupProgress.Phase.CheckingModelFiles,
        )
        val materialized =
            if (
                allowAcquisition
            ) {
                hooks
                    .ensureMaterialization(
                        canonicalHome,
                        true,
                        realization,
                    )
                    .observation
            } else {
                hooks.observeMaterialization(
                    canonicalHome,
                    realization,
                )
            }
        require(
            materialized.ready,
        ) {
            "Windows runtime Session requires exact model/runtime dependencies already present when acquisition is not authorized"
        }

        require(
            allowRuntimeStart,
        ) {
            "Windows runtime Session cannot form without runtime-start authority"
        }

        progress.phase(
            VexLifeProdSetupProgress.Phase.StartingLocalModel,
        )
        val runtime =
            hooks.startRuntime(
                canonicalHome,
                realization,
            )

        try {
            val evidence =
                runtime.evidence
            val bound =
                modelConfiguration.bindQualified(
                    manifest = home.manifest,
                    qualifiedRuntime = evidence,
                    runtimeReceiptPath =
                        realization.runtimeReceiptRelativePath,
                )
            require(
                bound.value.realizationRef ==
                    realization.realizationRef,
            ) {
                "Windows runtime Session binding lost exact realization identity"
            }

            return Session(
                productHome = canonicalHome,
                runtime = runtime,
                evidence = evidence,
            )
        } catch (
            error: Throwable,
        ) {
            runtime.closeRuntime()
            throw error
        }
    }

    private class Session(
        private val productHome: Path,
        private val runtime: RuntimeHandle,
        evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
    ) :
        VexLifeProdRuntimeSession {
        override val foundation =
            VexLifeProdRuntimeSession.Foundation(
                state =
                    VexLifeProdRuntimeSession.ReadyState,
                productHome =
                    productHome.toString(),
                qualifiedRuntimeEvidence =
                    evidence,
            )

        override fun currentQualifiedRuntimeEvidence(
            onRecovery: (() -> Unit)?,
        ): HereticModelConfiguration.QualifiedRuntimeEvidence =
            observeCurrentQualifiedRuntimeEvidence()

        override fun observeCurrentQualifiedRuntimeEvidence():
            HereticModelConfiguration.QualifiedRuntimeEvidence =
            runtime.observeCurrent()

        override fun close() {
            runtime.closeRuntime()
        }
    }

    private fun convergeConversationWriter(
        productHome: Path,
    ) {
        val conversation =
            DirectConversationStore(
                productHome,
            )
        val before =
            conversation.threadWriterCurrentness()

        if (
            before.state ==
                "IDLE"
        ) {
            return
        }

        if (
            before.state ==
                "RECOVERY_REQUIRED" &&
                before.ownerState ==
                    "ABSENT" &&
                before.recoveryRequired
        ) {
            val instanceRef =
                requireNotNull(
                    before.instanceRef,
                ) {
                    "abandoned Windows Conversation writer lacks instance identity"
                }
            val leaseSha256 =
                requireNotNull(
                    before.leaseSha256,
                ) {
                    "abandoned Windows Conversation writer lacks lease identity"
                }
            require(
                conversation.recoverAbandonedWriterLease(
                    expectedAbandonedInstanceRef =
                        instanceRef,
                    expectedLeaseSha256 =
                        leaseSha256,
                ),
            ) {
                "abandoned Windows Conversation writer recovery did not perform the exact effect"
            }
            val after =
                conversation.threadWriterCurrentness()
            require(
                after.state ==
                    "IDLE" &&
                    !after.recoveryRequired,
            ) {
                "Windows Conversation writer recovery did not converge to idle"
            }
            return
        }

        error(
            "Windows runtime Session cannot replace an active or unknown Conversation writer: " +
                before.state +
                "/" +
                (before.ownerState ?: "NONE"),
        )
    }
}

// [VXG RealForever]
