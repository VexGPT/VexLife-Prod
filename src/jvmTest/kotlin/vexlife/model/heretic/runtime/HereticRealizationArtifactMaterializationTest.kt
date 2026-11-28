package vexlife.model.heretic.runtime

import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelImport
import vexlife.model.heretic.HereticModelOfferings
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.platform.windows.VexLifeWindowsRuntimeMaterialization

class HereticRealizationArtifactMaterializationTest {
    @Test
    fun macObservationDelegatesToTheCatalogMaterializationOwner() {
        val parent =
            Files.createTempDirectory(
                "vexlife-realization-artifact-mac-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        try {
            CanonicalVexLifeHomeFixture.form(
                home,
            )
            val realization =
                HereticRuntimeRealization.require(
                    modelRef =
                        HereticModelCatalog.Models.Qwen4B.modelRef,
                    target =
                        HereticRuntimeRealization.MacArm64,
                )

            val observed =
                HereticRealizationArtifactMaterialization.observe(
                    productHome = home,
                    realization = realization,
                )

            assertEquals(
                realization.realizationRef,
                observed.realizationRef,
            )
            assertEquals(
                HereticRealizationArtifactMaterialization.PresenceState.ABSENT,
                observed.state,
            )
            assertEquals(
                HereticModelCatalog.Models.Qwen4B.totalBytes,
                observed.totalBytes,
            )
            assertEquals(
                0L,
                observed.presentBytes,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun homeImportedModelUsesItsDynamicMacRealizationForArtifactObservation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-realization-artifact-imported-mac-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        try {
            CanonicalVexLifeHomeFixture.form(
                home,
            )
            val revision =
                "5".repeat(
                    40,
                )
            val record =
                HereticModelImport.Record(
                    importRef =
                        "model-import.vexlife-prod.huggingface.artifact-realization-test",
                    sourceUrl =
                        "https://huggingface.co/example/Artifact-Realization-Test",
                    providerRepository =
                        "example/Artifact-Realization-Test",
                    providerRevision =
                        revision,
                    handle =
                        "hf-artifact-realization-test",
                    humanLabel =
                        "Artifact Realization Test",
                    modelBundleRef =
                        "model-bundle.vexlife.imported.hf-artifact-realization-test.001",
                    generationRef =
                        "generation.vexlife-prod.imported-provider." +
                            revision.take(
                                12,
                            ),
                    modelRef =
                        "model.vexlife-prod.imported.hf-artifact-realization-test.001",
                    modelProfileRef =
                        "model-profile.vexlife-prod.imported.hf-artifact-realization-test.001",
                    artifactSetRef =
                        "artifact-set.vexlife-prod.imported.hf-artifact-realization-test.001",
                    artifactCustodyEvidenceRef =
                        "artifact-custody.vexlife-prod.imported.hf-artifact-realization-test.001",
                    artifactContentSetSha256 =
                        "9ce1987541512005be95684d18fbb5f0252825da4d743a328b44ef4fed1e0d61",
                    modelRootRelativePath =
                        "models/heretic/hf-artifact-realization-test",
                    providerLicense =
                        "apache-2.0",
                    representationRef =
                        HereticModelImport.RepresentationQwen3OpenAiChat,
                    companionProfileRef =
                        "companion-profile.vexlife-prod.imported.hf-artifact-realization-test.001",
                    members =
                        listOf(
                            HereticModelImport.Member(
                                roleRef =
                                    "artifact-role.vexlife-prod.imported.config",
                                artifactRef =
                                    "artifact.vexlife-prod.imported.hf-artifact-realization-test.config",
                                filename =
                                    "config.json",
                                mediaType =
                                    "application/json",
                                sha256 =
                                    "7".repeat(
                                        64,
                                    ),
                                expectedBytes =
                                    1L,
                                sourceRef =
                                    "source.huggingface.example.artifact-realization-test." +
                                        revision,
                                licenseRef =
                                    "license.huggingface.example.artifact-realization-test.apache-2.0",
                            ),
                        ),
                )
            FileHereticModelImportStore(
                home,
            )
                .add(
                    record,
                )
            val imported =
                HereticModelOfferings.requireEntry(
                    home,
                    record.modelRef,
                )
            val realization =
                requireNotNull(
                    HereticRuntimeRealization.resolve(
                        model = imported,
                        target = HereticRuntimeRealization.MacArm64,
                    ),
                )

            val observed =
                HereticRealizationArtifactMaterialization.observe(
                    productHome = home,
                    realization = realization,
                )

            assertEquals(
                imported.modelRef,
                observed.modelRef,
            )
            assertEquals(
                1L,
                observed.totalBytes,
            )
            assertEquals(
                HereticRealizationArtifactMaterialization.PresenceState.ABSENT,
                observed.state,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun windowsModelOnlyAcquireAndRemoveDoNotMaterializeRuntimeBinary() {
        val parent =
            Files.createTempDirectory(
                "vexlife-realization-artifact-windows-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        val bytes =
            "abc"
                .encodeToByteArray()
        try {
            CanonicalVexLifeHomeFixture.form(
                home,
            )
            val realization =
                syntheticWindowsRealization()
            val transport =
                DirectHttpsArtifactDelivery.Transport {
                    _,
                    resumeFrom,
                    ->
                    assertEquals(
                        0L,
                        resumeFrom,
                    )
                    DirectHttpsArtifactDelivery.Opened(
                        status = 200,
                        contentLength = bytes.size.toLong(),
                        contentRange = null,
                        body = ByteArrayInputStream(bytes),
                    )
                }

            val acquired =
                HereticRealizationArtifactMaterialization.ensureModel(
                    productHome = home,
                    realization = realization,
                    allowAcquisition = true,
                    macAcquirer =
                        HereticArtifactMaterialization.Acquirer {
                            _,
                            _,
                            ->
                            error(
                                "Windows model acquisition must not use the Mac artifact acquirer",
                            )
                        },
                    windowsTransport = transport,
                )

            assertEquals(
                HereticRealizationArtifactMaterialization.PresenceState.PRESENT,
                acquired.observation.state,
            )
            assertEquals(
                bytes.size.toLong(),
                acquired.observation.totalBytes,
            )
            assertTrue(
                Files.isRegularFile(
                    java.nio.file.Path.of(
                        acquired.observation.modelPath,
                    ),
                ),
            )

            val windows =
                VexLifeWindowsRuntimeMaterialization.observe(
                    productHome = home,
                    realization = realization,
                )
            assertEquals(
                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
                windows.modelState,
            )
            assertEquals(
                VexLifeWindowsRuntimeMaterialization.FileState.ABSENT,
                windows.runtimeArchiveState,
            )
            assertEquals(
                VexLifeWindowsRuntimeMaterialization.FileState.ABSENT,
                windows.runtimeImageState,
            )
            assertEquals(
                VexLifeWindowsRuntimeMaterialization.FileState.ABSENT,
                windows.runtimeExecutableState,
            )

            val removed =
                HereticRealizationArtifactMaterialization.removeModel(
                    productHome = home,
                    realization = realization,
                )
            assertEquals(
                1,
                removed.removedFiles,
            )
            assertEquals(
                bytes.size.toLong(),
                removed.freedBytes,
            )
            assertFalse(
                Files.exists(
                    java.nio.file.Path.of(
                        acquired.observation.modelPath,
                    ),
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun syntheticWindowsRealization(): HereticRuntimeRealization.Value =
        HereticRuntimeRealization.Value(
            realizationRef =
                "realization.vexlife-prod.heretic-4b.windows-x64.synthetic-artifact-test.001",
            modelRef =
                HereticModelCatalog.Models.Qwen4B.modelRef,
            modelProfileRef =
                HereticModelCatalog.Models.Qwen4B.identity.modelProfileRef,
            target =
                HereticRuntimeRealization.WindowsX64,
            artifact =
                HereticRuntimeRealization.ArtifactProfile(
                    artifactRef =
                        "artifact.vexlife-prod.synthetic.windows-model",
                    providerRepository =
                        "example/synthetic",
                    providerRevision =
                        "a".repeat(
                            40,
                        ),
                    primaryFilename =
                        "model.gguf",
                    expectedPrimaryBytes =
                        3L,
                    contentSetSha256 =
                        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                    materializationRootRelativePath =
                        "models/heretic/synthetic-windows",
                    directHttpsUrl =
                        "https://example.invalid/model.gguf",
                ),
            runtimeBinary =
                HereticRuntimeRealization.RuntimeBinaryProfile(
                    binaryRef =
                        "runtime-binary.vexlife-prod.synthetic.windows-x64",
                    providerRepository =
                        "example/runtime",
                    providerReleaseTag =
                        "test",
                    providerSourceCommit =
                        "b".repeat(
                            40,
                        ),
                    providerPackagingSourceRef =
                        "example/runtime@test:package",
                    archiveFilename =
                        "runtime.zip",
                    archiveBytes =
                        1L,
                    archiveSha256 =
                        "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
                    archiveUrl =
                        "https://example.invalid/runtime.zip",
                    executableRelativePath =
                        "runtime.exe",
                    executableSha256 =
                        "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
                    materializationRootRelativePath =
                        "runtime/vexlife-prod/synthetic-windows",
                ),
            runtime =
                HereticRuntimeRealization.RuntimeProfile(
                    runtimeAdapterRef =
                        "adapter.runtime.synthetic.windows.001",
                    runtimeClass =
                        "SYNTHETIC_WINDOWS_RUNTIME",
                    transportClass =
                        "SYNTHETIC_LOOPBACK",
                    endpoint =
                        "http://127.0.0.1:19090",
                    requestModel =
                        "default_model",
                    livedLaunchProfile =
                        HereticRuntimeRealization.RuntimeLaunchProfile(
                            contextTokens = 1024,
                            jinja = false,
                            gpuOffloadLayers = null,
                        ),
                ),
            runtimeConfigurationProfileRef =
                null,
            process =
                HereticRuntimeRealization.ProcessProfile(
                    processSubstrateRef =
                        "substrate.vexlife.synthetic.windows.001",
                    lifetimeOwnerRef =
                        "owner.vexlife-prod.synthetic-windows-runtime.001",
                ),
            qualification =
                HereticRuntimeRealization.QualificationProfile(
                    qualificationRef =
                        "qualification.vexlife-prod.synthetic.windows.v1",
                    healthPath =
                        "/health",
                    modelsPath =
                        "/v1/models",
                    completionPath =
                        "/v1/chat/completions",
                    modelExposure =
                        HereticRuntimeRealization.ModelExposure.EXACT_REQUEST_MODEL_ALIAS,
                    maxTokens =
                        1,
                    temperature =
                        0.0,
                    enableThinking =
                        false,
                    stream =
                        false,
                ),
            runtimeReceiptRelativePath =
                "runtime/vexlife-prod/synthetic-windows-receipt.json",
        )
}

// [VXG RealForever]
