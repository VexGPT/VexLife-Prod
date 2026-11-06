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
    )

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
    ): Observation {
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

        val qualification =
            requireNotNull(
                receipt.qualification,
            ) {
                "stable VexLife Heretic runtime receipt lacks exact qualification evidence"
            }
        val substrate =
            requireNotNull(
                receipt.runtime.processSubstrate,
            ) {
                "stable VexLife Heretic runtime receipt lacks exact process-substrate ownership evidence"
            }
        val expectedArgs =
            HereticPersistentRuntime.serverArgs(
                binding,
            )
        validateStableLaunchdReceipt(
            receipt = substrate,
            expectedPythonPath = binding.pythonPath,
            expectedWorkingDirectory = binding.pythonEnvironmentRoot,
            expectedArguments = expectedArgs,
            expectedPid = receipt.runtime.pid,
        )
        val launchd =
            MacUserLaunchdProcess.observeCoordinate(
                domainTarget = substrate.domainTarget,
                label = substrate.label,
                host = launchdHost,
            )
        require(
            receipt.runtime.pid >
                0 &&
                receipt.runtime.processGroupId >
                0 &&
                receipt.runtime.exactOwnedDetachedGroup &&
                receipt.runtime.args ==
                expectedArgs &&
                launchd is
                MacUserLaunchdProcess.Observation.Running &&
                launchd.pid ==
                receipt.runtime.pid &&
                platform.processAlive(
                    receipt.runtime.pid,
                ),
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
