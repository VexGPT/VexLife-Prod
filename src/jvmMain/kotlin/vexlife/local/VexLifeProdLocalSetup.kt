package vexlife.local

import java.net.InetAddress
import java.nio.file.Path
import java.time.OffsetDateTime
import vexlife.conversation.DirectConversationStore
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeAvailability
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticArtifactDelivery
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile
import vexlife.model.heretic.runtime.HereticPersistentRuntime
import vexlife.model.heretic.runtime.HereticPythonEnvironment
import vexlife.model.heretic.runtime.MacHereticRuntimePlatform
import vexlife.model.heretic.runtime.StableVexLifeHereticRuntimeCompatibility
import vexlife.platform.python.ManagedPython

/**
 * Application-level composition for VexLife-Prod runtime machinery beneath the canonical VexLife
 * Home/identity.
 *
 * This owner contributes sequencing only. It does not mint another Home, device, Companion lineage,
 * Conversation, Memory or human product identity. Every artifact fact, environment rule,
 * process/lifetime effect and runtime-binding transition remains owned by the component that answers
 * that question.
 */
object VexLifeProdLocalSetup {
    const val RuntimeAuthorityRef =
        "authority.vexlife-prod.local-setup.heretic-runtime.001"

    data class Result(
        val state: String,
        val productHome: String,
        val homeDisposition: String,
        val modelSelectionDisposition: String,
        val managedPythonDisposition: String,
        val artifactDisposition: String,
        val environmentDisposition: String,
        val runtimeDisposition: String,
        val modelBindingDisposition: String,
        val modelConfigurationState: HereticModelConfiguration.State,
        val qualifiedRuntimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        val endpoint: String,
        val requestModel: String,
    )

    /**
     * Construction/lifetime capability for long-lived product consumers.
     *
     * The public result remains the setup projection. This Session retains only the capability to
     * ask the rightful Heretic runtime owner for fresh currentness before later effects.
     */
    internal class Session internal constructor(
        val result: Result,
        private val currentRuntimeEvidence: () -> HereticModelConfiguration.QualifiedRuntimeEvidence,
        private val recoverRuntimeEvidence: (() -> HereticModelConfiguration.QualifiedRuntimeEvidence)?,
    ) {
        fun currentQualifiedRuntimeEvidence(): HereticModelConfiguration.QualifiedRuntimeEvidence =
            try {
                currentRuntimeEvidence()
            } catch (
                original: RuntimeException,
            ) {
                recoverRuntimeEvidence
                    ?.invoke()
                    ?: throw original
            }
    }

    private data class Established(
        val result: Result,
        val currentRuntimeEvidence: () -> HereticModelConfiguration.QualifiedRuntimeEvidence,
        val recoverRuntimeEvidence: (() -> HereticModelConfiguration.QualifiedRuntimeEvidence)?,
    )

    internal data class Hooks(
        val observeHost: () -> HereticMacHostProfile.Result = {
            HereticMacHostProfile.observeCurrent()
        },
        val deviceName: () -> String = {
            currentDeviceName()
        },
        val now: () -> String = {
            OffsetDateTime.now()
                .toString()
        },
        val observeConversationWriter: (
            productHome: Path,
        ) -> DirectConversationStore.ThreadWriterCurrentness = {
            productHome ->
            DirectConversationStore(
                productHome,
            )
                .threadWriterCurrentness()
        },
        val recoverConversationWriter: (
            productHome: Path,
            instanceRef: String,
            leaseSha256: String,
        ) -> Boolean = {
            productHome,
            instanceRef,
            leaseSha256,
            ->
            DirectConversationStore(
                productHome,
            )
                .recoverAbandonedWriterLease(
                    expectedAbandonedInstanceRef = instanceRef,
                    expectedLeaseSha256 = leaseSha256,
                )
        },
        val ensureManagedPython: (
            productHome: Path,
            allowAcquisition: Boolean,
        ) -> ManagedPython.Result = {
            productHome,
            allowAcquisition,
            ->
            ManagedPython.ensure(
                productHome = productHome,
                pythonVersion = HereticProductIdentity.PythonVersion,
                allowAcquisition = allowAcquisition,
            )
        },
        val materializeArtifacts: (
            productHome: Path,
            allowAcquisition: Boolean,
        ) -> HereticArtifactMaterialization.Result = {
            productHome,
            allowAcquisition,
            ->
            HereticArtifactMaterialization.materializeCurrent(
                productHome = productHome,
                allowAcquisition = allowAcquisition,
                acquirer = HereticArtifactDelivery.Acquirer(),
            )
        },
        val ensureHereticEnvironment: (
            managedPython: ManagedPython.Result,
            productHome: Path,
            allowCreation: Boolean,
        ) -> HereticPythonEnvironment.Result = {
            managedPython,
            productHome,
            allowCreation,
            ->
            HereticPythonEnvironment.ensure(
                managedPython = managedPython,
                productHome = productHome,
                allowCreation = allowCreation,
            )
        },
        val observeStableRuntime: (
            productHome: Path,
            binding: HereticPersistentRuntime.Binding,
        ) -> StableVexLifeHereticRuntimeCompatibility.Observation? = {
            productHome,
            binding,
            ->
            StableVexLifeHereticRuntimeCompatibility.observe(
                productHome = productHome,
                binding = binding,
                platform =
                    MacHereticRuntimePlatform(
                        productHome = productHome,
                    ),
            )
        },
        val observeCurrentProductRuntime: (
            productHome: Path,
            binding: HereticPersistentRuntime.Binding,
            store: HereticPersistentRuntime.ReceiptStore,
        ) -> HereticModelConfiguration.QualifiedRuntimeEvidence = {
            productHome,
            binding,
            store,
            ->
            HereticPersistentRuntime.observeCurrentQualified(
                binding = binding,
                store = store,
                platform =
                    MacHereticRuntimePlatform(
                        productHome = productHome,
                    ),
            )
        },
        val startRuntime: (
            productHome: Path,
            binding: HereticPersistentRuntime.Binding,
            store: HereticPersistentRuntime.ReceiptStore,
        ) -> HereticPersistentRuntime.StartResult = {
            productHome,
            binding,
            store,
            ->
            HereticPersistentRuntime.startOrReuse(
                binding = binding,
                store = store,
                platform =
                    MacHereticRuntimePlatform(
                        productHome = productHome,
                    ),
            )
        },
    )

    /**
     * Ordinary product route. The canonical Home coordinate is fixed by the existing VexLife
     * relationship; [allowAcquisition] authorizes only missing model/runtime prerequisites.
     */
    fun establish(
        allowAcquisition: Boolean,
    ): Result =
        establishSession(
            allowAcquisition,
        )
            .result

    internal fun establishSession(
        allowAcquisition: Boolean,
    ): Session {
        val productHome =
            VexLifeProdHomeFiles.defaultMacRoot(
                Path.of(
                    System.getProperty(
                        "user.home",
                    ),
                ),
            )

        return VexLifeProdLocalSetupLease
            .acquire(
                productHome,
            )
            .use {
                formSession(
                    establishDetailedAt(
                        productHome = productHome,
                        allowAcquisition = allowAcquisition,
                        hooks = Hooks(),
                    ),
                )
            }
    }

    internal fun establishSerializedAt(
        productHome: Path,
        allowAcquisition: Boolean,
        hooks: Hooks,
    ): Result =
        VexLifeProdLocalSetupLease
            .acquire(
                productHome,
            )
            .use {
                establishDetailedAt(
                    productHome = productHome,
                    allowAcquisition = allowAcquisition,
                    hooks = hooks,
                )
                    .result
            }

    internal fun establishAt(
        productHome: Path,
        allowAcquisition: Boolean,
        hooks: Hooks,
    ): Result =
        establishDetailedAt(
            productHome = productHome,
            allowAcquisition = allowAcquisition,
            hooks = hooks,
        )
            .result

    private fun establishDetailedAt(
        productHome: Path,
        allowAcquisition: Boolean,
        hooks: Hooks,
    ): Established {
        val host =
            hooks.observeHost()

        require(
            host.state ==
                HereticMacHostProfile.State.Eligible &&
                !host.effectsPerformed,
        ) {
            "current Mac does not satisfy the certified VexLife-Prod Heretic host profile: " +
                (
                    host.reason
                        ?: "UNKNOWN"
                )
        }

        val home =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    productHome,
                ),
            ) {
                "canonical ~/.vexlife Home is absent; VexLife-Prod must not mint a parallel Home or Companion lineage"
            }

        val canonicalHome =
            Path.of(
                home.productHome,
            )

        convergeConversationWriter(
            productHome = canonicalHome,
            hooks = hooks,
        )

        val modelConfiguration =
            FileHereticModelConfigurationStore(
                canonicalHome,
            )

        val selected =
            modelConfiguration.selectHeretic(
                home.manifest,
            )

        val managedPython =
            hooks.ensureManagedPython(
                Path.of(
                    home.productHome,
                ),
                allowAcquisition,
            )

        val materialized =
            hooks.materializeArtifacts(
                Path.of(
                    home.productHome,
                ),
                allowAcquisition,
            )

        val environment =
            hooks.ensureHereticEnvironment(
                managedPython,
                Path.of(
                    home.productHome,
                ),
                allowAcquisition,
            )

        val binding =
            HereticPersistentRuntime.bind(
                authorityRef = RuntimeAuthorityRef,
                materialization = materialized,
                environment = environment,
            )

        val borrowedStableRuntime =
            hooks.observeStableRuntime(
                canonicalHome,
                binding,
            )

        val availabilityAct =
            requireNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState =
                        borrowedStableRuntime
                            ?.let {
                                stable ->
                                HereticRuntimeAvailability.State.StableBorrowable(
                                    evidence =
                                        stable.evidence,
                                    runtimeReceiptPath =
                                        stable.receiptRelativePath,
                                )
                            }
                            ?: HereticRuntimeAvailability.State.StablePredecessorUnavailable,
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            productOwnedMode =
                                if (
                                    allowAcquisition
                                ) {
                                    HereticRuntimeAvailability.ProductOwnedMode.AcquisitionAllowed
                                } else {
                                    HereticRuntimeAvailability.ProductOwnedMode.ObservationOnly
                                },
                        ),
                ),
            ) {
                "Heretic runtime availability did not resolve one lawful relationship"
            }

        val availabilityAndDisposition =
            when (
                val admitted =
                    availabilityAct.admittedState
            ) {
                is HereticRuntimeAvailability.State.Qualified -> {
                    require(
                        availabilityAct.effect ==
                            null &&
                            admitted.source ==
                            HereticRuntimeAvailability.Source.BorrowedStableVexLife,
                    ) {
                        "borrowed stable Heretic runtime availability must not request a product-owned effect"
                    }

                    admitted to
                        "BORROWED_STABLE_VEXLIFE_QUALIFIED_RUNTIME"
                }

                null -> {
                    val productRuntimeStore =
                        FileHereticRuntimeReceiptStore(
                            canonicalHome,
                        )

                    when (
                        availabilityAct.effect
                    ) {
                        HereticRuntimeAvailability.Effect.EnsureProductOwnedRuntime -> {
                            val runtime =
                                hooks.startRuntime(
                                    canonicalHome,
                                    binding,
                                    productRuntimeStore,
                                )

                            HereticRuntimeAvailability.interpretProductOwnedQualified(
                                HereticPersistentRuntime.configurationEvidence(
                                    binding = binding,
                                    receipt = runtime.receipt,
                                ),
                            ) to
                                runtime.disposition
                        }

                        HereticRuntimeAvailability.Effect.ObserveCurrentProductOwnedRuntime -> {
                            val evidence =
                                hooks.observeCurrentProductRuntime(
                                    canonicalHome,
                                    binding,
                                    productRuntimeStore,
                                )

                            HereticRuntimeAvailability.interpretProductOwnedQualified(
                                evidence,
                            ) to
                                "REOBSERVED_CURRENT_PRODUCT_QUALIFIED_RUNTIME"
                        }

                        null ->
                            error(
                                "unresolved Heretic runtime availability lacks one product-owned effect",
                            )
                    }
                }

                else ->
                    error(
                        "Heretic runtime availability admitted a non-qualified terminal state",
                    )
            }

        val availability =
            availabilityAndDisposition.first
        val runtimeDisposition =
            availabilityAndDisposition.second
        val qualifiedRuntimeEvidence =
            availability.evidence
        val runtimeReceiptPath =
            availability.runtimeReceiptPath

        val currentRuntimeEvidence: () -> HereticModelConfiguration.QualifiedRuntimeEvidence =
            when (
                availability.source
            ) {
                HereticRuntimeAvailability.Source.BorrowedStableVexLife -> {
                    {
                        requireNotNull(
                            hooks.observeStableRuntime(
                                canonicalHome,
                                binding,
                            ),
                        ) {
                            "borrowed stable VexLife Heretic runtime is no longer current"
                        }
                            .evidence
                    }
                }

                HereticRuntimeAvailability.Source.VexLifeProdOwned -> {
                    {
                        hooks.observeCurrentProductRuntime(
                            canonicalHome,
                            binding,
                            FileHereticRuntimeReceiptStore(
                                canonicalHome,
                            ),
                        )
                    }
                }
            }

        val recoverRuntimeEvidence: (() -> HereticModelConfiguration.QualifiedRuntimeEvidence)? =
            when (
                availability.source
            ) {
                HereticRuntimeAvailability.Source.VexLifeProdOwned -> {
                    {
                        val recovered =
                            hooks.startRuntime(
                                canonicalHome,
                                binding,
                                FileHereticRuntimeReceiptStore(
                                    canonicalHome,
                                ),
                            )
                        HereticPersistentRuntime.configurationEvidence(
                            binding = binding,
                            receipt = recovered.receipt,
                        )
                    }
                }

                HereticRuntimeAvailability.Source.BorrowedStableVexLife ->
                    null
            }

        val bound =
            modelConfiguration.bindQualified(
                manifest = home.manifest,
                qualifiedRuntime = qualifiedRuntimeEvidence,
                runtimeReceiptPath = runtimeReceiptPath,
            )

        return Established(
            result =
                Result(
                    state = "LOCAL_HERETIC_FOUNDATION_READY",
                    productHome = home.productHome,
                    homeDisposition = home.state,
                    modelSelectionDisposition = selected.state,
                    managedPythonDisposition = managedPython.state,
                    artifactDisposition = materialized.state,
                    environmentDisposition = environment.state,
                    runtimeDisposition = runtimeDisposition,
                    modelBindingDisposition = bound.state,
                    modelConfigurationState = bound.value.state,
                    qualifiedRuntimeEvidence = qualifiedRuntimeEvidence,
                    endpoint = qualifiedRuntimeEvidence.endpoint,
                    requestModel = qualifiedRuntimeEvidence.requestModel,
                ),
            currentRuntimeEvidence = currentRuntimeEvidence,
            recoverRuntimeEvidence = recoverRuntimeEvidence,
        )
    }

    private fun convergeConversationWriter(
        productHome: Path,
        hooks: Hooks,
    ) {
        val before =
            hooks.observeConversationWriter(
                productHome,
            )

        if (
            before.state ==
                "IDLE"
        ) {
            require(
                !before.recoveryRequired &&
                    before.ownerState ==
                    null &&
                    before.instanceRef ==
                    null &&
                    before.leaseSha256 ==
                    null,
            ) {
                "idle VexLife Conversation writer currentness is internally inconsistent"
            }
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
                    "abandoned VexLife Conversation writer lacks exact instance identity"
                }
            val leaseSha256 =
                requireNotNull(
                    before.leaseSha256,
                ) {
                    "abandoned VexLife Conversation writer lacks exact lease identity"
                }

            require(
                hooks.recoverConversationWriter(
                    productHome,
                    instanceRef,
                    leaseSha256,
                ),
            ) {
                "abandoned VexLife Conversation writer recovery did not perform the exact effect"
            }

            val after =
                hooks.observeConversationWriter(
                    productHome,
                )
            require(
                after.state ==
                    "IDLE" &&
                    !after.recoveryRequired &&
                    after.ownerState ==
                    null &&
                    after.instanceRef ==
                    null &&
                    after.leaseSha256 ==
                    null,
            ) {
                "abandoned VexLife Conversation writer recovery did not converge to IDLE"
            }
            return
        }

        error(
            "Open Vex thread writer is not safely startable: " +
                before.state +
                "/" +
                (
                    before.ownerState
                        ?: "NONE"
                    ),
        )
    }

    private fun formSession(
        established: Established,
    ): Session {
        return Session(
            result = established.result,
            currentRuntimeEvidence = established.currentRuntimeEvidence,
            recoverRuntimeEvidence = established.recoverRuntimeEvidence,
        )
    }

    private fun currentDeviceName(): String {
        val computerName =
            try {
                val process =
                    ProcessBuilder(
                        "/usr/sbin/scutil",
                        "--get",
                        "ComputerName",
                    )
                        .start()
                val value =
                    process.inputStream
                        .bufferedReader()
                        .readText()
                        .trim()
                process.errorStream
                    .bufferedReader()
                    .readText()

                value.takeIf {
                    process.waitFor() ==
                        0 &&
                        it.isNotBlank()
                }
            } catch (
                _: Exception,
            ) {
                null
            }

        return computerName
            ?: InetAddress
                .getLocalHost()
                .hostName
                .takeIf(
                    String::isNotBlank,
                )
            ?: "VexLife-Prod Mac"
    }
}

// [VXG RealForever]
