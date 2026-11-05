package vexlife.local

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile
import vexlife.model.heretic.runtime.HereticPersistentRuntime
import vexlife.model.heretic.runtime.HereticPythonEnvironment
import vexlife.platform.macos.MacUserLaunchdProcess
import vexlife.platform.python.ManagedPython

class VexLifeProdLocalSetupTest {
    private fun eligibleHost(): HereticMacHostProfile.Result =
        HereticMacHostProfile.evaluate(
            HereticMacHostProfile.Observation(
                platform = "darwin",
                architecture = "arm64",
                chipModel = HereticMacHostProfile.RequiredChipModel,
                totalMemoryBytes = HereticMacHostProfile.MinimumMemoryBytes,
            ),
        )

    private fun unsupportedHost(): HereticMacHostProfile.Result =
        HereticMacHostProfile.evaluate(
            HereticMacHostProfile.Observation(
                platform = "linux",
                architecture = "x86_64",
                chipModel = null,
                totalMemoryBytes = 4L * 1024L * 1024L * 1024L,
            ),
        )

    private fun managedPython(
        home: Path,
        state: String = "INSTALLED_VERIFIED",
    ): ManagedPython.Result =
        ManagedPython.Result(
            state = state,
            pythonVersion = HereticProductIdentity.PythonVersion,
            pythonPath =
                home.resolve(
                    "runtime/python/managed/cpython-3.14.7/bin/python3.14",
                )
                    .toString(),
            pythonInstallRoot =
                home.resolve(
                    ManagedPython.PythonInstallRelativePath,
                )
                    .toString(),
            uvVersion = ManagedPython.UvVersion,
            uvPath =
                home.resolve(
                    ManagedPython.UvToolRelativePath,
                )
                    .resolve(
                        "uv",
                    )
                    .toString(),
            uvArchiveSha256 = ManagedPython.UvMacArm64ArchiveSha256,
            uvReleaseSourceCommit = ManagedPython.UvReleaseSourceCommit,
            uvAcquisitionPerformed =
                state ==
                    "INSTALLED_VERIFIED",
            pythonInstallationPerformed =
                state ==
                    "INSTALLED_VERIFIED",
            globalBinMutationPerformed = false,
        )

    private fun materialization(
        home: Path,
        state: String = "HERETIC_ARTIFACTS_MATERIALIZED_EXACT",
    ): HereticArtifactMaterialization.Result =
        HereticArtifactMaterialization.Result(
            state = state,
            modelPath =
                home.resolve(
                    "models/heretic/qwen3.8-27b-mlx4/" +
                        "8".repeat(
                            40,
                        ),
                )
                    .toString(),
            providerRevision = "8".repeat(
                40,
            ),
            contentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            memberCount = 11,
            totalBytes = 15_153_242_506L,
            reusedMemberCount = 0,
            acquiredMemberCount = 11,
            acquisitionPerformed = true,
            artifactMutationPerformed = true,
        )

    private fun environment(
        home: Path,
        state: String = "CREATED_VERIFIED",
    ): HereticPythonEnvironment.Result {
        val root =
            home.resolve(
                "runtime/heretic/" +
                    HereticPythonEnvironment.EnvironmentDirectoryName,
            )

        return HereticPythonEnvironment.Result(
            state = state,
            pythonEnvironmentRoot = root.toString(),
            pythonPath =
                root.resolve(
                    "bin/python",
                )
                    .toString(),
            pythonVersion = HereticProductIdentity.PythonVersion,
            mlxLmVersion = HereticProductIdentity.MlxLmVersion,
            pipReportPath =
                root.resolve(
                    HereticPythonEnvironment.InstallReportFilename,
                )
                    .toString(),
            environmentMutationPerformed =
                state ==
                    "CREATED_VERIFIED",
        )
    }

    private fun runtimeResult(
        binding: HereticPersistentRuntime.Binding,
    ): HereticPersistentRuntime.StartResult {
        val pid =
            4242L
        val qualification =
            HereticRuntimeQualification.qualified()
        val receipt =
            HereticPersistentRuntime.Receipt(
                schemaVersion = HereticPersistentRuntime.ReceiptSchema,
                receiptRef = "receipt.vexlife-prod.heretic-persistent-runtime.test",
                state = HereticRuntimeState.Qualified,
                formedAt = "2026-11-04T18:00:00-08:00",
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
                    HereticPersistentRuntime.RuntimeProcess(
                        pid = pid,
                        processGroupId = pid,
                        exactOwnedDetachedGroup = true,
                        args =
                            HereticPersistentRuntime.serverArgs(
                                binding,
                            ),
                        processSubstrate =
                            MacUserLaunchdProcess.Receipt(
                                schemaVersion = MacUserLaunchdProcess.Schema,
                                substrateRef = MacUserLaunchdProcess.SubstrateRef,
                                label = "com.vexlife.prod.heretic.test",
                                domainTarget = "gui/501",
                                plistPath = "/tmp/vexlife-prod-test/heretic.plist",
                                programPath = binding.pythonPath,
                                programArguments = emptyList(),
                                workingDirectory = binding.pythonEnvironmentRoot,
                                stdoutPath = "/tmp/vexlife-prod-test/heretic.out.log",
                                stderrPath = "/tmp/vexlife-prod-test/heretic.err.log",
                                keepAlive = false,
                                pid = pid,
                            ),
                    ),
                effects = HereticRuntimeEffectBoundary.QualifiedRuntime,
                qualification = qualification,
            )

        return HereticPersistentRuntime.StartResult(
            disposition = "STARTED_NEW_EXACT_MLX_RUNTIME",
            pid = pid,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            receipt = receipt,
            receiptWritePerformed = true,
            qualification = qualification,
        )
    }

    private fun hooks(
        trace: MutableList<String>,
        host: HereticMacHostProfile.Result = eligibleHost(),
        reuse: Boolean = false,
    ): VexLifeProdLocalSetup.Hooks =
        VexLifeProdLocalSetup.Hooks(
            observeHost = {
                trace +=
                    "HOST"
                host
            },
            deviceName = {
                trace +=
                    "DEVICE"
                "Test Mac"
            },
            now = {
                trace +=
                    "NOW"
                "2026-11-04T18:00:00-08:00"
            },
            ensureManagedPython = {
                home,
                _,
                ->
                trace +=
                    "MANAGED_PYTHON"
                managedPython(
                    home,
                    if (
                        reuse
                    ) {
                        "REUSED_VERIFIED"
                    } else {
                        "INSTALLED_VERIFIED"
                    },
                )
            },
            materializeArtifacts = {
                home,
                _,
                ->
                trace +=
                    "ARTIFACTS"
                materialization(
                    home,
                )
            },
            ensureHereticEnvironment = {
                _,
                home,
                _,
                ->
                trace +=
                    "HERETIC_ENVIRONMENT"
                environment(
                    home,
                    if (
                        reuse
                    ) {
                        "REUSED_VERIFIED"
                    } else {
                        "CREATED_VERIFIED"
                    },
                )
            },
            startRuntime = {
                _,
                binding,
                _,
                ->
                trace +=
                    "RUNTIME"
                runtimeResult(
                    binding,
                )
            },
        )

    @Test
    fun authorizedFreshSetupComposesOneReadableLocalFoundationChain() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-local-setup-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val trace =
            mutableListOf<String>()

        try {
            val result =
                VexLifeProdLocalSetup.establishAt(
                    productHome = home,
                    allowAcquisition = true,
                    hooks = hooks(
                        trace,
                    ),
                )

            assertEquals(
                "LOCAL_HERETIC_FOUNDATION_READY",
                result.state,
            )
            assertEquals(
                "NEW_HOME_FORMED",
                result.homeDisposition,
            )
            assertEquals(
                "HERETIC_SELECTED_PENDING",
                result.modelSelectionDisposition,
            )
            assertEquals(
                "HERETIC_BOUND_TO_QUALIFIED_RUNTIME",
                result.modelBindingDisposition,
            )
            assertEquals(
                HereticModelConfiguration.State.BoundProductModel,
                result.modelConfigurationState,
            )
            assertEquals(
                HereticProductIdentity.Endpoint,
                result.endpoint,
            )
            assertEquals(
                listOf(
                    "HOST",
                    "DEVICE",
                    "NOW",
                    "MANAGED_PYTHON",
                    "ARTIFACTS",
                    "HERETIC_ENVIRONMENT",
                    "RUNTIME",
                ),
                trace,
            )

            val manifest =
                requireNotNull(
                    VexLifeProdHomeFiles.observeExisting(
                        home,
                    ),
                )
                    .manifest
            val current =
                assertIs<FileHereticModelConfigurationStore.Snapshot.Heretic>(
                    FileHereticModelConfigurationStore(
                        home,
                    )
                        .read(
                            manifest,
                        ),
                )
            assertTrue(
                HereticModelConfiguration.isBound(
                    current.value,
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun freshSetupWithoutAcquisitionAuthorizationStopsBeforeHomeFormation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-local-setup-no-consent-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val trace =
            mutableListOf<String>()

        try {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdLocalSetup.establishAt(
                    productHome = home,
                    allowAcquisition = false,
                    hooks = hooks(
                        trace,
                    ),
                )
            }

            assertEquals(
                listOf(
                    "HOST",
                ),
                trace,
            )
            assertFalse(
                Files.exists(
                    home,
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun unsupportedHostStopsBeforeHomeFormation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-local-setup-host-held-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val trace =
            mutableListOf<String>()

        try {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdLocalSetup.establishAt(
                    productHome = home,
                    allowAcquisition = true,
                    hooks =
                        hooks(
                            trace = trace,
                            host = unsupportedHost(),
                        ),
                )
            }

            assertEquals(
                listOf(
                    "HOST",
                ),
                trace,
            )
            assertFalse(
                Files.exists(
                    home,
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun existingHomeRerunPreservesIdentityAndReusesBoundConfiguration() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-local-setup-rerun-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val firstTrace =
            mutableListOf<String>()

        try {
            val first =
                VexLifeProdLocalSetup.establishAt(
                    productHome = home,
                    allowAcquisition = true,
                    hooks = hooks(
                        firstTrace,
                    ),
                )
            val manifestBefore =
                Files.readString(
                    home.resolve(
                        "config/home.json",
                    ),
                )

            val secondTrace =
                mutableListOf<String>()
            val second =
                VexLifeProdLocalSetup.establishAt(
                    productHome = home,
                    allowAcquisition = false,
                    hooks = hooks(
                        trace = secondTrace,
                        reuse = true,
                    ),
                )

            assertEquals(
                "EXISTING_HOME_PRESERVED",
                second.homeDisposition,
            )
            assertEquals(
                "HERETIC_ALREADY_BOUND",
                second.modelSelectionDisposition,
            )
            assertEquals(
                "HERETIC_ALREADY_BOUND",
                second.modelBindingDisposition,
            )
            assertEquals(
                first.productHome,
                second.productHome,
            )
            assertEquals(
                manifestBefore,
                Files.readString(
                    home.resolve(
                        "config/home.json",
                    ),
                ),
            )
            assertEquals(
                listOf(
                    "HOST",
                    "MANAGED_PYTHON",
                    "ARTIFACTS",
                    "HERETIC_ENVIRONMENT",
                    "RUNTIME",
                ),
                secondTrace,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
