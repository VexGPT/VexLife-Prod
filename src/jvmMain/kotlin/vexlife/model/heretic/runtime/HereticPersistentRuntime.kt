package vexlife.model.heretic.runtime

import kotlinx.serialization.Serializable

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.platform.macos.MacUserLaunchdProcess

/**
 * Heretic persistent-runtime semantic/effect owner.
 *
 * Binding is formed only from exact outputs of the artifact + Python environment owners. Platform
 * process census, launchd construction, liveness and HTTP qualification are injected effects.
 */
object HereticPersistentRuntime {
    const val BindingSchema =
        "vexlife.heretic-persistent-runtime-owner-binding/v1"

    const val ReceiptSchema =
        "vexlife.heretic-persistent-runtime-owner-receipt/v1"

    const val OwnerRef =
        "owner.vexlife-prod.heretic-persistent-runtime.001"

    const val StopActionRef =
        "action.vexlife-prod.heretic-persistent-runtime.stop-exact-owned-runtime"

    val Port: Int =
        HereticProductIdentity.Endpoint
            .substringAfterLast(
                ":",
            )
            .toInt()
            .also {
                require(
                    it in
                        1024..65535,
                ) {
                    "Heretic product endpoint must expose one unprivileged runtime port"
                }
            }

    const val OwnershipTimeoutMs =
        30_000L

    const val OwnershipPollMs =
        50L

    private val RefPattern =
        Regex(
            "[A-Za-z0-9][A-Za-z0-9._:/-]*",
        )

    private val Sha256Pattern =
        Regex(
            "[0-9a-f]{64}",
        )

    const val ImplementationClassResource =
        "vexlife/model/heretic/runtime/HereticPersistentRuntime.class"

    @Serializable
    @ConsistentCopyVisibility
    data class ImplementationIdentity internal constructor(
        val repository: String,
        val classResource: String,
        val classSha256: String,
    ) {
        init {
            require(
                repository ==
                    "VexGPT/VexLife-Prod",
            ) {
                "Heretic runtime implementation identity must belong to VexLife-Prod"
            }
            require(
                classResource ==
                    ImplementationClassResource,
            ) {
                "Heretic runtime implementation class identity drift"
            }
            require(
                Sha256Pattern.matches(
                    classSha256,
                ),
            ) {
                "Heretic runtime implementation identity requires lowercase SHA-256"
            }
        }
    }

    fun currentImplementationIdentity(): ImplementationIdentity {
        val bytes =
            requireNotNull(
                HereticPersistentRuntime::class.java.classLoader
                    .getResourceAsStream(
                        ImplementationClassResource,
                    ),
            ) {
                "packaged Heretic runtime implementation class bytes are unavailable"
            }
                .use {
                    it.readBytes()
                }

        return ImplementationIdentity(
            repository = "VexGPT/VexLife-Prod",
            classResource = ImplementationClassResource,
            classSha256 = sha256(
                bytes,
            ),
        )
    }

    @ConsistentCopyVisibility
    data class Binding private constructor(
        val schemaVersion: String,
        val authorityRef: String,
        val modelPath: String,
        val pythonPath: String,
        val pythonEnvironmentRoot: String,
        val implementationIdentity: ImplementationIdentity,
    )

    data class ProcessIdentity(
        val pid: Long,
        val processGroupId: Long,
        val exactOwnedDetachedGroup: Boolean,
    )

    @Serializable
    data class RuntimeProcess(
        val pid: Long,
        val processGroupId: Long,
        val exactOwnedDetachedGroup: Boolean,
        val args: List<String>,
        val processSubstrate: MacUserLaunchdProcess.Receipt,
    )

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val receiptRef: String,
        val state: HereticRuntimeState,
        val formedAt: String,
        val authorityRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val artifactRef: String,
        val artifactCustodyEvidenceRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val transportClass: String,
        val hostExecutionProfileRef: String,
        val pythonVersion: String,
        val mlxLmVersion: String,
        val trainerSourceCommit: String,
        val modelPath: String,
        val pythonPath: String,
        val pythonEnvironmentRoot: String,
        val endpoint: String,
        val requestModel: String,
        val implementationIdentity: ImplementationIdentity,
        val runtime: RuntimeProcess,
        val effects: HereticRuntimeEffectBoundary.Value,
        val qualification: HereticRuntimeQualification?,
    )

    interface ReceiptStore {
        fun read(): Receipt?

        fun write(
            receipt: Receipt,
        )

        fun delete()
    }

    data class Spawned(
        val pid: Long,
        val processSubstrate: MacUserLaunchdProcess.Receipt,
    )

    data class Retirement(
        val exactOwnershipVerified: Boolean,
        val verifiedNotLive: Boolean,
    )

    interface Platform {
        val platform: String
        val architecture: String

        fun environmentCurrent(
            binding: Binding,
        ): Boolean

        fun processAlive(
            pid: Long,
        ): Boolean

        fun processIdentity(
            pid: Long,
            args: List<String>,
        ): ProcessIdentity?

        fun processSubstrateCurrent(
            receipt: MacUserLaunchdProcess.Receipt,
            expectedPid: Long,
        ): Boolean

        fun matchingProcesses(
            args: List<String>,
        ): List<ProcessIdentity>

        fun portOccupied(
            port: Int,
        ): Boolean

        fun spawn(
            binding: Binding,
            args: List<String>,
        ): Spawned

        fun retire(
            receipt: Receipt,
        ): Retirement

        /**
         * Retire a process that has a source-owned construction receipt but has not yet earned
         * semantic Heretic runtime ownership.
         */
        fun retireProvisional(
            spawned: Spawned,
            args: List<String>,
        ): Retirement

        fun waitUntilHealthy(
            binding: Binding,
            pid: Long,
        )

        fun qualify(
            binding: Binding,
        ): HereticRuntimeQualification

        fun cancellationRequested(): Boolean =
            false

        fun now(): String

        fun nowMs(): Long =
            System.currentTimeMillis()

        fun sleep(
            milliseconds: Long,
        ) {
            Thread.sleep(
                milliseconds,
            )
        }
    }

    data class StartResult(
        val disposition: String,
        val pid: Long,
        val endpoint: String,
        val requestModel: String,
        val receipt: Receipt,
        val receiptWritePerformed: Boolean,
        val qualification: HereticRuntimeQualification,
    )

    data class StopResult(
        val disposition: String,
        val pid: Long?,
        val processTerminationPerformed: Boolean,
        val verifiedNotLive: Boolean,
    )

    fun bind(
        authorityRef: String,
        materialization: HereticArtifactMaterialization.Result,
        environment: HereticPythonEnvironment.Result,
    ): Binding {
        require(
            RefPattern.matches(
                authorityRef,
            ),
        ) {
            "Heretic runtime authorityRef must be one stable ref"
        }
        require(
            materialization.state ==
                "HERETIC_ARTIFACTS_MATERIALIZED_EXACT" &&
                materialization.contentSetSha256 ==
                HereticProductIdentity.ArtifactContentSetSha256 &&
                materialization.memberCount ==
                11,
        ) {
            "Heretic runtime binding requires exact certified model materialization"
        }
        require(
            (
                environment.state ==
                    "REUSED_VERIFIED" ||
                    environment.state ==
                    "CREATED_VERIFIED"
            ) &&
                environment.pythonVersion ==
                HereticProductIdentity.PythonVersion &&
                environment.mlxLmVersion ==
                HereticProductIdentity.MlxLmVersion,
        ) {
            "Heretic runtime binding requires exact verified Python/mlx-lm environment"
        }

        val model =
            absolute(
                materialization.modelPath,
                "modelPath",
            )
        val environmentRoot =
            absolute(
                environment.pythonEnvironmentRoot,
                "pythonEnvironmentRoot",
            )
        val python =
            absolute(
                environment.pythonPath,
                "pythonPath",
            )

        require(
            python.startsWith(
                environmentRoot,
            ),
        ) {
            "Heretic runtime Python executable must belong to the verified environment root"
        }

        return Binding(
            schemaVersion = BindingSchema,
            authorityRef = authorityRef,
            modelPath = model.toString(),
            pythonPath = python.toString(),
            pythonEnvironmentRoot = environmentRoot.toString(),
            implementationIdentity = currentImplementationIdentity(),
        )
    }

    fun configurationEvidence(
        binding: Binding,
        receipt: Receipt,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        validateBinding(
            binding,
        )
        require(
            receiptBinds(
                binding = binding,
                receipt = receipt,
                requireImplementationSha = true,
            ) &&
                receipt.state ==
                HereticRuntimeState.Qualified &&
                receipt.qualification !=
                null,
        ) {
            "Heretic model configuration evidence requires one exact current qualified runtime receipt"
        }

        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = receipt.receiptRef,
            runtimeState = receipt.state,
            modelRef = receipt.modelRef,
            modelProfileRef = receipt.modelProfileRef,
            artifactRef = receipt.artifactRef,
            artifactContentSetSha256 = receipt.artifactContentSetSha256,
            runtimeAdapterRef = receipt.runtimeAdapterRef,
            runtimeClass = receipt.runtimeClass,
            endpoint = receipt.endpoint,
            requestModel = receipt.requestModel,
            qualification =
                requireNotNull(
                    receipt.qualification,
                ),
        )
    }

    fun serverArgs(
        binding: Binding,
    ): List<String> {
        validateBinding(
            binding,
        )

        return listOf(
            "-m",
            "mlx_lm.server",
            "--model",
            binding.modelPath,
            "--host",
            "127.0.0.1",
            "--port",
            Port.toString(),
            "--chat-template-args",
            "{\"enable_thinking\":false}",
        )
    }

    fun startOrReuse(
        binding: Binding,
        store: ReceiptStore,
        platform: Platform,
    ): StartResult {
        validateBinding(
            binding,
        )
        require(
            platform.platform ==
                "darwin" &&
                platform.architecture ==
                "arm64",
        ) {
            "Heretic persistent runtime requires darwin/arm64"
        }
        require(
            platform.environmentCurrent(
                binding,
            ),
        ) {
            "Heretic persistent runtime requires a fresh exact environment observation"
        }
        heldIfCancelled(
            platform,
        )

        val args =
            serverArgs(
                binding,
            )
        val prior =
            store.read()
        var staleImplementationRotated =
            false

        if (
            prior !=
                null &&
                !receiptBinds(
                    binding,
                    prior,
                    requireImplementationSha = true,
                ) &&
                receiptBinds(
                    binding,
                    prior,
                    requireImplementationSha = false,
                )
        ) {
            if (
                platform.processAlive(
                    prior.runtime.pid,
                )
            ) {
                requireExactOwnedProcess(
                    prior,
                    args,
                    platform,
                )
            }

            val retired =
                platform.retire(
                    prior,
                )
            require(
                retired.exactOwnershipVerified &&
                    retired.verifiedNotLive,
            ) {
                "stale-implementation Heretic runtime could not be retired before current-implementation restart"
            }
            store.delete()
            staleImplementationRotated =
                true
        } else if (
            prior !=
                null &&
                !receiptBinds(
                    binding,
                    prior,
                    requireImplementationSha = true,
                )
        ) {
            error(
                "existing Heretic runtime receipt does not match the current binding",
            )
        }

        val currentPrior =
            if (
                staleImplementationRotated
            ) {
                null
            } else {
                prior
            }

        if (
            currentPrior !=
                null
        ) {
            val pid =
                currentPrior.runtime.pid

            if (
                platform.processAlive(
                    pid,
                )
            ) {
                val currentIdentity =
                    requireExactOwnedProcess(
                        currentPrior,
                        args,
                        platform,
                    )

                if (
                    currentPrior.state ==
                        HereticRuntimeState.Starting
                ) {
                    try {
                        platform.waitUntilHealthy(
                            binding,
                            pid,
                        )
                    } catch (
                        error: Exception,
                    ) {
                        val cleanup =
                            platform.retire(
                                currentPrior,
                            )
                        require(
                            cleanup.verifiedNotLive,
                        ) {
                            "STARTING runtime recovery failed and cleanup was not verified"
                        }
                        store.delete()
                        throw error
                    }
                }

                if (
                    platform.cancellationRequested()
                ) {
                    val cleanup =
                        platform.retire(
                            currentPrior,
                        )
                    require(
                        cleanup.verifiedNotLive,
                    ) {
                        "cancelled runtime reuse could not be cleaned up"
                    }
                    store.delete()
                    error(
                        "Heretic runtime start was cancelled",
                    )
                }

                val qualification =
                    platform.qualify(
                        binding,
                    )
                val qualified =
                    formReceipt(
                        binding = binding,
                        pid = pid,
                        identity =
                            currentIdentity,
                        state =
                            HereticRuntimeState.Qualified,
                        formedAt =
                            platform.now(),
                        receiptRef =
                            currentPrior.receiptRef,
                        processSubstrate =
                            currentPrior.runtime.processSubstrate,
                        qualification =
                            qualification,
                    )

                store.write(
                    qualified,
                )

                return StartResult(
                    disposition =
                        if (
                            currentPrior.state ==
                                HereticRuntimeState.Starting
                        ) {
                            "RECOVERED_EXACT_STARTING_MLX_RUNTIME"
                        } else {
                            "REUSED_EXACT_OWNED_MLX_RUNTIME"
                        },
                    pid = pid,
                    endpoint = qualified.endpoint,
                    requestModel = qualified.requestModel,
                    receipt = qualified,
                    receiptWritePerformed = true,
                    qualification = qualification,
                )
            }

            val retired =
                platform.retire(
                    currentPrior,
                )
            require(
                retired.exactOwnershipVerified &&
                    retired.verifiedNotLive,
            ) {
                "dead Heretic runtime substrate could not be retired with exact substrate ownership"
            }
            store.delete()
        }

        heldIfCancelled(
            platform,
        )

        require(
            platform
                .matchingProcesses(
                    args,
                )
                .isEmpty(),
        ) {
            "matching mlx_lm.server exists without a current exact receipt"
        }
        require(
            !platform.portOccupied(
                Port,
            ),
        ) {
            "Heretic endpoint is occupied without exact ownership"
        }

        val spawned =
            platform.spawn(
                binding,
                args,
            )
        require(
            spawned.pid >
                0,
        ) {
            "mlx_lm.server returned no PID"
        }

        val identity =
            waitForExactOwnership(
                spawned,
                args,
                platform,
            )

        if (
            identity ==
                null
        ) {
            val cleanup =
                platform.retireProvisional(
                    spawned,
                    args,
                )
            require(
                cleanup.exactOwnershipVerified &&
                    cleanup.verifiedNotLive,
            ) {
                "provisional Heretic process cleanup was not verified"
            }

            if (
                platform.cancellationRequested()
            ) {
                error(
                    "Heretic runtime start was cancelled before exact process ownership",
                )
            }

            error(
                "new Heretic process ownership could not be proven",
            )
        }

        val receiptRef =
            "receipt.vexlife-prod.heretic-persistent-runtime." +
                sha256(
                    binding.implementationIdentity.classSha256 +
                        "|" +
                        spawned.pid +
                        "|" +
                        platform.now(),
                )
                    .take(
                        24,
                    )

        val starting =
            formReceipt(
                binding = binding,
                pid = spawned.pid,
                identity = identity,
                state = HereticRuntimeState.Starting,
                formedAt = platform.now(),
                receiptRef = receiptRef,
                processSubstrate = spawned.processSubstrate,
                qualification = null,
            )

        try {
            store.write(
                starting,
            )
        } catch (
            error: Exception,
        ) {
            val cleanup =
                platform.retire(
                    starting,
                )
            require(
                cleanup.verifiedNotLive,
            ) {
                "STARTING receipt write failed and exact cleanup was not verified"
            }
            throw error
        }

        val qualification =
            try {
                heldIfCancelled(
                    platform,
                )
                platform.waitUntilHealthy(
                    binding,
                    spawned.pid,
                )
                heldIfCancelled(
                    platform,
                )
                platform.qualify(
                    binding,
                )
            } catch (
                error: Exception,
            ) {
                val cleanup =
                    platform.retire(
                        starting,
                    )
                require(
                    cleanup.verifiedNotLive,
                ) {
                    "qualification failed and exact cleanup was not verified"
                }
                store.delete()
                throw error
            }

        val qualified =
            formReceipt(
                binding = binding,
                pid = spawned.pid,
                identity = identity,
                state = HereticRuntimeState.Qualified,
                formedAt = platform.now(),
                receiptRef = receiptRef,
                processSubstrate = spawned.processSubstrate,
                qualification = qualification,
            )

        store.write(
            qualified,
        )

        return StartResult(
            disposition =
                if (
                    staleImplementationRotated
                ) {
                    "ROTATED_STALE_IMPLEMENTATION_AND_STARTED_NEW_EXACT_MLX_RUNTIME"
                } else {
                    "STARTED_NEW_EXACT_MLX_RUNTIME"
                },
            pid = spawned.pid,
            endpoint = qualified.endpoint,
            requestModel = qualified.requestModel,
            receipt = qualified,
            receiptWritePerformed = true,
            qualification = qualification,
        )
    }

    fun stop(
        binding: Binding,
        store: ReceiptStore,
        platform: Platform,
        requireCurrentSource: Boolean = true,
    ): StopResult {
        validateBinding(
            binding,
        )

        val receipt =
            store.read()
                ?: return StopResult(
                    disposition = "NO_HERETIC_RUNTIME_RECEIPT",
                    pid = null,
                    processTerminationPerformed = false,
                    verifiedNotLive = true,
                )

        require(
            receiptBinds(
                binding,
                receipt,
                requireImplementationSha =
                    requireCurrentSource,
            ),
        ) {
            "exact current Heretic runtime receipt is required to stop"
        }

        if (
            platform.processAlive(
                receipt.runtime.pid,
            )
        ) {
            requireExactOwnedProcess(
                receipt,
                serverArgs(
                    binding,
                ),
                platform,
            )
        }

        val retired =
            platform.retire(
                receipt,
            )
        require(
            retired.exactOwnershipVerified &&
                retired.verifiedNotLive,
        ) {
            "exact Heretic runtime stop could not be verified"
        }

        store.delete()

        return StopResult(
            disposition = "STOPPED_EXACT_OWNED_MLX_RUNTIME",
            pid = receipt.runtime.pid,
            processTerminationPerformed = true,
            verifiedNotLive = true,
        )
    }

    private fun validateBinding(
        binding: Binding,
    ) {
        require(
            binding.schemaVersion ==
                BindingSchema,
        )
        require(
            RefPattern.matches(
                binding.authorityRef,
            ),
        )
        require(
            Path.of(
                binding.modelPath,
            ).isAbsolute,
        )
        require(
            Path.of(
                binding.pythonPath,
            ).isAbsolute,
        )
        require(
            Path.of(
                binding.pythonEnvironmentRoot,
            ).isAbsolute,
        )
    }

    private fun receiptBinds(
        binding: Binding,
        receipt: Receipt,
        requireImplementationSha: Boolean,
    ): Boolean {
        val expectedArgs =
            serverArgs(
                binding,
            )

        return receipt.schemaVersion ==
            ReceiptSchema &&
            receipt.authorityRef ==
            binding.authorityRef &&
            receipt.modelRef ==
            HereticProductIdentity.ModelRef &&
            receipt.modelProfileRef ==
            HereticProductIdentity.ModelProfileRef &&
            receipt.artifactRef ==
            HereticProductIdentity.ArtifactSetRef &&
            receipt.artifactCustodyEvidenceRef ==
            HereticProductIdentity.ArtifactCustodyEvidenceRef &&
            receipt.artifactContentSetSha256 ==
            HereticProductIdentity.ArtifactContentSetSha256 &&
            receipt.runtimeAdapterRef ==
            HereticProductIdentity.RuntimeAdapterRef &&
            receipt.runtimeClass ==
            HereticProductIdentity.RuntimeClass &&
            receipt.transportClass ==
            HereticProductIdentity.TransportClass &&
            receipt.hostExecutionProfileRef ==
            HereticProductIdentity.HostExecutionProfileRef &&
            receipt.pythonVersion ==
            HereticProductIdentity.PythonVersion &&
            receipt.mlxLmVersion ==
            HereticProductIdentity.MlxLmVersion &&
            receipt.trainerSourceCommit ==
            HereticProductIdentity.TrainerSourceCommit &&
            receipt.modelPath ==
            binding.modelPath &&
            receipt.pythonPath ==
            binding.pythonPath &&
            receipt.pythonEnvironmentRoot ==
            binding.pythonEnvironmentRoot &&
            receipt.endpoint ==
            HereticProductIdentity.Endpoint &&
            receipt.requestModel ==
            HereticProductIdentity.RequestModel &&
            receipt.implementationIdentity.repository ==
            binding.implementationIdentity.repository &&
            receipt.implementationIdentity.classResource ==
            binding.implementationIdentity.classResource &&
            (
                !requireImplementationSha ||
                    receipt.implementationIdentity.classSha256 ==
                    binding.implementationIdentity.classSha256
            ) &&
            receipt.runtime.pid >
            0 &&
            receipt.runtime.processGroupId >
            0 &&
            receipt.runtime.exactOwnedDetachedGroup &&
            receipt.runtime.args ==
            expectedArgs &&
            receipt.runtime.processSubstrate.pid ==
            receipt.runtime.pid &&
            receipt.effects ==
            HereticRuntimeEffectBoundary.QualifiedRuntime
    }

    private fun requireExactOwnedProcess(
        receipt: Receipt,
        args: List<String>,
        platform: Platform,
    ): ProcessIdentity {
        val identity =
            requireNotNull(
                platform.processIdentity(
                    receipt.runtime.pid,
                    args,
                ),
            ) {
                "live Heretic receipt process identity is unavailable"
            }

        require(
            identity.exactOwnedDetachedGroup &&
                identity.processGroupId ==
                receipt.runtime.processGroupId &&
                identity.pid ==
                receipt.runtime.pid &&
                platform.processSubstrateCurrent(
                    receipt.runtime.processSubstrate,
                    receipt.runtime.pid,
                ),
        ) {
            "live receipt process is not the exact owned Heretic process/lifetime substrate"
        }

        return identity
    }

    private fun waitForExactOwnership(
        spawned: Spawned,
        args: List<String>,
        platform: Platform,
    ): ProcessIdentity? {
        val deadline =
            platform.nowMs() +
                OwnershipTimeoutMs

        while (
            platform.nowMs() <
            deadline
        ) {
            if (
                platform.cancellationRequested()
            ) {
                return null
            }

            if (
                !platform.processAlive(
                    spawned.pid,
                )
            ) {
                return null
            }

            val identity =
                platform.processIdentity(
                    spawned.pid,
                    args,
                )

            if (
                identity !=
                    null &&
                    identity.exactOwnedDetachedGroup &&
                    identity.pid ==
                    spawned.pid &&
                    platform.processSubstrateCurrent(
                        spawned.processSubstrate,
                        spawned.pid,
                    )
            ) {
                return identity
            }

            platform.sleep(
                OwnershipPollMs,
            )
        }

        return null
    }

    private fun formReceipt(
        binding: Binding,
        pid: Long,
        identity: ProcessIdentity,
        state: HereticRuntimeState,
        formedAt: String,
        receiptRef: String,
        processSubstrate: MacUserLaunchdProcess.Receipt,
        qualification: HereticRuntimeQualification?,
        argsOverride: List<String>? = null,
    ): Receipt =
        Receipt(
            schemaVersion = ReceiptSchema,
            receiptRef = receiptRef,
            state = state,
            formedAt = formedAt,
            authorityRef = binding.authorityRef,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactCustodyEvidenceRef = HereticProductIdentity.ArtifactCustodyEvidenceRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            transportClass = HereticProductIdentity.TransportClass,
            hostExecutionProfileRef = HereticProductIdentity.HostExecutionProfileRef,
            pythonVersion = HereticProductIdentity.PythonVersion,
            mlxLmVersion = HereticProductIdentity.MlxLmVersion,
            trainerSourceCommit = HereticProductIdentity.TrainerSourceCommit,
            modelPath = binding.modelPath,
            pythonPath = binding.pythonPath,
            pythonEnvironmentRoot = binding.pythonEnvironmentRoot,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            implementationIdentity = binding.implementationIdentity,
            runtime =
                RuntimeProcess(
                    pid = pid,
                    processGroupId = identity.processGroupId,
                    exactOwnedDetachedGroup = identity.exactOwnedDetachedGroup,
                    args =
                        argsOverride
                            ?: serverArgs(
                                binding,
                            ),
                    processSubstrate = processSubstrate,
                ),
            effects = HereticRuntimeEffectBoundary.QualifiedRuntime,
            qualification = qualification,
        )

    private fun heldIfCancelled(
        platform: Platform,
    ) {
        if (
            platform.cancellationRequested()
        ) {
            error(
                "Heretic runtime start was cancelled",
            )
        }
    }

    private fun absolute(
        value: String,
        label: String,
    ): Path {
        val path =
            Path.of(
                value,
            )
        require(
            path.isAbsolute,
        ) {
            "$label must be absolute"
        }
        return path.normalize()
    }

    private fun sha256(
        value: ByteArray,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value,
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
