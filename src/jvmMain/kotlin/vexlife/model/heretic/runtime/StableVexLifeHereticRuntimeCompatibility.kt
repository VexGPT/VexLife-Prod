package vexlife.model.heretic.runtime

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.platform.macos.MacUserLaunchdProcess

/**
 * Read-only compatibility bridge for the already-owned stable VexLife Heretic runtime.
 *
 * New Beginnings may borrow exact qualified runtime evidence from this predecessor owner. It does not
 * rewrite the stable receipt, manufacture VexLife-Prod process ownership, or turn port presence into
 * runtime authority.
 */
object StableVexLifeHereticRuntimeCompatibility {
    const val ReceiptSchema =
        "vexlife.heretic-persistent-runtime-owner-receipt/v1"

    const val QualifiedState =
        "PERSISTENT_MLX_RUNTIME_QUALIFIED"

    const val StableAuthorityRef =
        "authority.vexlife.heretic-runtime-establishment.explicit-user-setup.v1"

    const val StableHostExecutionProfileRef =
        "profile.repository.vexlife.macos-v1"

    const val StableOwnerRepository =
        "vgong24/VexLife"

    const val StableOwnerModulePath =
        "src/core/heretic-persistent-runtime-owner.mjs"

    @Serializable
    data class SourceIdentity(
        val repository: String,
        val modulePath: String,
        val moduleSha256: String,
    )

    const val StableLaunchdReceiptSchema =
        "vexlife.macos-user-launchd-process/v1"

    const val StableLaunchdSubstrateRef =
        "substrate.vexlife.macos-user-launchd-process.001"

    @Serializable
    data class StableMacUserLaunchdProcessReceipt(
        val schemaVersion: String,
        val substrateRef: String,
        val label: String,
        val domainTarget: String,
        val plistPath: String,
        val programPath: String,
        val programArguments: List<String>,
        val workingDirectory: String,
        val stdoutPath: String,
        val stderrPath: String,
        val keepAlive: Boolean,
        val pid: Long,
    )

    @Serializable
    data class RuntimeProcess(
        val pid: Long,
        val processGroupId: Long,
        val exactOwnedDetachedGroup: Boolean,
        val args: List<String>,
        val processSubstrate: StableMacUserLaunchdProcessReceipt? = null,
    )

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val receiptRef: String,
        val state: String,
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
        val sourceIdentity: SourceIdentity,
        val runtime: RuntimeProcess,
        val effects: HereticRuntimeEffectBoundary.Value,
        val qualification: HereticRuntimeQualification? = null,
    )

    data class Observation(
        val evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        val receiptRelativePath: String,
        val pid: Long,
        val launchContract: HereticPersistentRuntime.ServerLaunchContract =
            HereticPersistentRuntime.ServerLaunchContract.CURRENT,
    )

    const val RetiredPriorLaunchContractState =
        "RETIRED_STABLE_PRIOR_LAUNCH_CONTRACT"

    data class Retirement(
        val launchContract: HereticPersistentRuntime.ServerLaunchContract,
        val exactOwnershipVerified: Boolean,
        val verifiedNotLive: Boolean,
        val receiptDeleted: Boolean,
    ) {
        val state: String
            get() = RetiredPriorLaunchContractState
    }

    private val RefPattern =
        Regex(
            "[A-Za-z0-9][A-Za-z0-9._:/-]*",
        )

    private val Sha256Pattern =
        Regex(
            "[0-9a-f]{64}",
        )

    private val StableLaunchdLabelPattern =
        Regex(
            "com\\.vexlife\\.heretic\\.[a-z0-9][a-z0-9.-]*",
        )

    private val LaunchdDomainPattern =
        Regex(
            "gui/\\d+",
        )

    fun observe(
        productHome: Path,
        binding: HereticPersistentRuntime.Binding,
        platform: HereticPersistentRuntime.Platform,
        launchdHost: MacUserLaunchdProcess.Host =
            MacUserLaunchdProcess.SystemHost,
    ): Observation? {
        // The stable predecessor only ever serves the certified 27B. A Home that chose another
        // catalog model is never served by borrowing it.
        if (binding.model.modelRef != HereticProductIdentity.ModelRef) {
            return null
        }
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "canonical VexLife Home",
            )
        val receipt =
            VexDurableContractFile.read<Receipt>(
                ownedRoot = home,
                path =
                    VexOwnedPath.resolveInside(
                        home,
                        HereticProductIdentity.StableRuntimeReceiptRelativePath,
                    ),
            )
                ?: return null

        return observeReceipt(
            receipt = receipt,
            binding = binding,
            platform = platform,
            launchdHost = launchdHost,
        )
    }

    internal fun observeReceipt(
        receipt: Receipt,
        binding: HereticPersistentRuntime.Binding,
        platform: HereticPersistentRuntime.Platform,
        launchdHost: MacUserLaunchdProcess.Host,
    ): Observation? {
        require(
            platform.platform ==
                "darwin" &&
                platform.architecture ==
                "arm64" &&
                platform.environmentCurrent(
                    binding,
                ),
        ) {
            "stable VexLife Heretic runtime compatibility requires the exact current darwin/arm64 environment"
        }
        require(
            receipt.schemaVersion ==
                ReceiptSchema &&
                receipt.state ==
                QualifiedState &&
                RefPattern.matches(
                    receipt.receiptRef,
                ) &&
                receipt.authorityRef ==
                StableAuthorityRef &&
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
                StableHostExecutionProfileRef &&
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
                receipt.sourceIdentity.repository ==
                StableOwnerRepository &&
                receipt.sourceIdentity.modulePath ==
                StableOwnerModulePath &&
                Sha256Pattern.matches(
                    receipt.sourceIdentity.moduleSha256,
                ) &&
                receipt.effects ==
                HereticRuntimeEffectBoundary.QualifiedRuntime,
        ) {
            "stable VexLife Heretic runtime receipt is not compatible with the certified New Beginnings runtime relationship"
        }

        val launchContract =
            requireNotNull(
                HereticPersistentRuntime.serverLaunchContract(
                    binding = binding,
                    args = receipt.runtime.args,
                ),
            ) {
                "stable VexLife Heretic runtime receipt launch contract is not admitted by this Product"
            }
        val expectedArgs =
            receipt.runtime.args
        require(
            receipt.runtime.pid >
                0 &&
                receipt.runtime.processGroupId >
                0 &&
                receipt.runtime.exactOwnedDetachedGroup,
        ) {
            "stable VexLife Heretic runtime receipt process identity is invalid"
        }

        receipt.runtime.processSubstrate
            ?.let {
                substrate ->
                validateStableLaunchdReceipt(
                    receipt = substrate,
                    expectedPythonPath = binding.pythonPath,
                    expectedWorkingDirectory = binding.pythonEnvironmentRoot,
                    expectedArguments = expectedArgs,
                    expectedPid = receipt.runtime.pid,
                )
            }

        if (
            !platform.processAlive(
                receipt.runtime.pid,
            )
        ) {
            return null
        }

        val qualification =
            requireNotNull(
                receipt.qualification,
            ) {
                "live stable VexLife Heretic runtime receipt lacks exact qualification evidence"
            }
        val substrate =
            requireNotNull(
                receipt.runtime.processSubstrate,
            ) {
                "live stable VexLife Heretic runtime receipt lacks exact process-substrate ownership evidence"
            }
        val launchd =
            MacUserLaunchdProcess.observeCoordinate(
                domainTarget = substrate.domainTarget,
                label = substrate.label,
                host = launchdHost,
            )
        require(
            launchd is
                MacUserLaunchdProcess.Observation.Running &&
                launchd.pid ==
                receipt.runtime.pid,
        ) {
            "stable VexLife Heretic runtime receipt does not bind one live exact-owned process"
        }
        val identity =
            requireNotNull(
                platform.processIdentity(
                    receipt.runtime.pid,
                    expectedArgs,
                ),
            ) {
                "stable VexLife Heretic process identity is unavailable"
            }
        require(
            identity.pid ==
                receipt.runtime.pid &&
                identity.processGroupId ==
                receipt.runtime.processGroupId &&
                identity.exactOwnedDetachedGroup,
        ) {
            "stable VexLife Heretic process/substrate no longer matches its exact owner receipt"
        }

        return Observation(
            evidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = receipt.receiptRef,
                    runtimeState = HereticRuntimeState.Qualified,
                    modelRef = receipt.modelRef,
                    modelProfileRef = receipt.modelProfileRef,
                    artifactRef = receipt.artifactRef,
                    artifactContentSetSha256 = receipt.artifactContentSetSha256,
                    runtimeAdapterRef = receipt.runtimeAdapterRef,
                    runtimeClass = receipt.runtimeClass,
                    endpoint = receipt.endpoint,
                    requestModel = receipt.requestModel,
                    qualification = qualification,
                ),
            receiptRelativePath =
                HereticProductIdentity.StableRuntimeReceiptRelativePath,
            pid = receipt.runtime.pid,
            launchContract = launchContract,
        )
    }

    /**
     * Upgrade/downgrade bridge for a source-known stable predecessor launch contract.
     *
     * The stable receipt is re-read and re-proved under the contract that formed it. Only an
     * exact-owned live process with a recognized prior argv contract may be retired. Unknown argv,
     * foreign scope, missing substrate evidence, or changed PID stays fail-closed.
     */
    fun retirePriorExact(
        productHome: Path,
        binding: HereticPersistentRuntime.Binding,
        expected: Observation,
        platform: HereticPersistentRuntime.Platform,
        launchdHost: MacUserLaunchdProcess.Host =
            MacUserLaunchdProcess.SystemHost,
    ): Retirement {
        require(
            expected.launchContract !=
                HereticPersistentRuntime.ServerLaunchContract.CURRENT,
        ) {
            "current stable Heretic launch contract does not require migration"
        }
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "canonical VexLife Home",
            )
        val receiptPath =
            VexOwnedPath.resolveInside(
                home,
                HereticProductIdentity.StableRuntimeReceiptRelativePath,
            )
        val receipt =
            requireNotNull(
                VexDurableContractFile.read<Receipt>(
                    ownedRoot = home,
                    path = receiptPath,
                ),
            ) {
                "stable VexLife Heretic runtime receipt disappeared before migration"
            }
        val current =
            requireNotNull(
                observeReceipt(
                    receipt = receipt,
                    binding = binding,
                    platform = platform,
                    launchdHost = launchdHost,
                ),
            ) {
                "stable VexLife Heretic runtime disappeared before exact migration"
            }
        require(
            current.receiptRelativePath ==
                expected.receiptRelativePath &&
                current.pid ==
                expected.pid &&
                current.evidence.runtimeReceiptRef ==
                expected.evidence.runtimeReceiptRef &&
                current.launchContract ==
                expected.launchContract,
        ) {
            "stable VexLife Heretic runtime changed before exact migration"
        }

        val substrate =
            requireNotNull(
                receipt.runtime.processSubstrate,
            ) {
                "stable VexLife Heretic runtime lacks exact process-substrate ownership evidence"
            }
        val productEraReceipt =
            MacUserLaunchdProcess.Receipt(
                schemaVersion = MacUserLaunchdProcess.Schema,
                substrateRef = substrate.substrateRef,
                label = substrate.label,
                domainTarget = substrate.domainTarget,
                plistPath = substrate.plistPath,
                programPath = substrate.programPath,
                programArguments = substrate.programArguments,
                workingDirectory = substrate.workingDirectory,
                stdoutPath = substrate.stdoutPath,
                stderrPath = substrate.stderrPath,
                environment = emptyMap(),
                keepAlive = substrate.keepAlive,
                pid = substrate.pid,
            )
        // The predecessor v1 receipt intentionally did not persist its environment map,
        // so today's v2 plist byte-reconstruction cannot be required retroactively. Exact ownership
        // was already re-proved above using the predecessor's own receipt contract, launchd
        // coordinate/PID and exact process argv/process-group identity. Retire through the same
        // launchd coordinate semantics the v1 owner used.
        val retired =
            MacUserLaunchdProcess.retire(
                receipt = productEraReceipt,
                host = launchdHost,
                expectedPid = receipt.runtime.pid,
            )
        require(
            retired.exactOwnershipVerified &&
                retired.verifiedNotLive,
        ) {
            "stable VexLife Heretic predecessor runtime retirement was not exact"
        }
        val deleted =
            VexDurableContractFile.delete(
                ownedRoot = home,
                path = receiptPath,
            )
        require(
            deleted,
        ) {
            "stable VexLife Heretic predecessor receipt remained after verified retirement"
        }

        return Retirement(
            launchContract = expected.launchContract,
            exactOwnershipVerified = true,
            verifiedNotLive = true,
            receiptDeleted = true,
        )
    }

    private fun validateStableLaunchdReceipt(
        receipt: StableMacUserLaunchdProcessReceipt,
        expectedPythonPath: String,
        expectedWorkingDirectory: String,
        expectedArguments: List<String>,
        expectedPid: Long,
    ) {
        require(
            receipt.schemaVersion ==
                StableLaunchdReceiptSchema &&
                receipt.substrateRef ==
                StableLaunchdSubstrateRef &&
                StableLaunchdLabelPattern.matches(
                    receipt.label,
                ) &&
                LaunchdDomainPattern.matches(
                    receipt.domainTarget,
                ) &&
                Path.of(
                    receipt.plistPath,
                )
                    .isAbsolute &&
                Path.of(
                    receipt.programPath,
                )
                    .isAbsolute &&
                Path.of(
                    receipt.workingDirectory,
                )
                    .isAbsolute &&
                Path.of(
                    receipt.stdoutPath,
                )
                    .isAbsolute &&
                Path.of(
                    receipt.stderrPath,
                )
                    .isAbsolute &&
                receipt.programPath ==
                expectedPythonPath &&
                receipt.programArguments ==
                expectedArguments &&
                receipt.workingDirectory ==
                expectedWorkingDirectory &&
                !receipt.keepAlive &&
                receipt.pid ==
                expectedPid &&
                receipt.pid >
                0,
        ) {
            "stable VexLife launchd v1 receipt is not exact to the borrowed Heretic runtime"
        }
    }
}

// [VXG RealForever]
