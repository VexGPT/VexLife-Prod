package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHome
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState

class FileHereticModelConfigurationStoreTest {
    private fun qualifiedRuntime(): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.test",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    @Test
    fun canonicalHomeGetsASeparateProductRuntimeBindingWithoutConfigRewrite() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-model-binding-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val canonicalModelPath =
                home.resolve(
                    "config/model.json",
                )
            val canonicalModelBefore =
                Files.readString(
                    canonicalModelPath,
                )
            val store =
                FileHereticModelConfigurationStore(
                    home,
                )

            assertIs<FileHereticModelConfigurationStore.Snapshot.Unconfigured>(
                store.read(
                    fixture.manifest,
                ),
            )

            assertFailsWith<IllegalArgumentException> {
                store.bindQualified(
                    manifest = fixture.manifest,
                    qualifiedRuntime = qualifiedRuntime(),
                )
            }

            val selected =
                store.selectHeretic(
                    fixture.manifest,
                )

            assertEquals(
                FileHereticModelConfigurationStore.TransitionState.SelectedPending,
                selected.state,
            )
            assertTrue(
                selected.writePerformed,
            )
            assertEquals(
                HereticModelConfiguration.State.EstablishmentPending,
                selected.value.state,
            )

            val selectedAgain =
                store.selectHeretic(
                    fixture.manifest,
                )

            assertEquals(
                FileHereticModelConfigurationStore.TransitionState.AlreadySelectedPending,
                selectedAgain.state,
            )
            assertFalse(
                selectedAgain.writePerformed,
            )

            val bound =
                store.bindQualified(
                    manifest = fixture.manifest,
                    qualifiedRuntime = qualifiedRuntime(),
                )

            assertEquals(
                FileHereticModelConfigurationStore.TransitionState.BoundToQualifiedRuntime,
                bound.state,
            )
            assertTrue(
                bound.writePerformed,
            )
            assertTrue(
                HereticModelConfiguration.isBound(
                    bound.value,
                ),
            )

            val rebound =
                store.bindQualified(
                    manifest = fixture.manifest,
                    qualifiedRuntime = qualifiedRuntime(),
                )

            assertEquals(
                FileHereticModelConfigurationStore.TransitionState.AlreadyBound,
                rebound.state,
            )
            assertFalse(
                rebound.writePerformed,
            )

            val current =
                assertIs<FileHereticModelConfigurationStore.Snapshot.Heretic>(
                    store.read(
                        fixture.manifest,
                    ),
                )

            assertEquals(
                HereticModelConfiguration.State.BoundProductModel,
                current.value.state,
            )
            assertTrue(
                Files.isRegularFile(
                    home.resolve(
                        VexLifeProdHome.ModelConfigurationRelativePath,
                    ),
                ),
            )
            assertEquals(
                canonicalModelBefore,
                Files.readString(
                    canonicalModelPath,
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
    @Test
    fun boundModelMayRebindBetweenAdmittedStableAndProductRuntimeReceiptOwners() {
        val parent =
            Files.createTempDirectory(
                "vexlife-model-config-rebind-runtime-owner-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val store =
                FileHereticModelConfigurationStore(
                    home,
                )
            store.selectHeretic(
                fixture.manifest,
            )
            store.bindQualified(
                manifest = fixture.manifest,
                qualifiedRuntime = qualifiedRuntime(),
                runtimeReceiptPath = HereticProductIdentity.StableRuntimeReceiptRelativePath,
            )
            val rebound =
                store.bindQualified(
                    manifest = fixture.manifest,
                    qualifiedRuntime = qualifiedRuntime(),
                    runtimeReceiptPath = HereticProductIdentity.ProductRuntimeReceiptRelativePath,
                )

            assertEquals(
                FileHereticModelConfigurationStore.TransitionState.ReboundToQualifiedRuntime,
                rebound.state,
            )
            assertEquals(
                HereticProductIdentity.ProductRuntimeReceiptRelativePath,
                rebound.value.runtimeReceiptPath,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun legacyV1BoundRecordReadsAsV2WithoutMutatingTheHome() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-model-binding-v1-migration-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val store =
                FileHereticModelConfigurationStore(
                    home,
                )
            store.selectHeretic(
                fixture.manifest,
            )
            val current =
                store.bindQualified(
                    manifest = fixture.manifest,
                    qualifiedRuntime = qualifiedRuntime(),
                )
                    .value
            val legacy =
                HereticModelConfiguration.LegacyValueV1(
                    schemaVersion =
                        HereticModelConfiguration.LegacySchema,
                    state =
                        current.state,
                    homeRef =
                        current.homeRef,
                    companionLineageRef =
                        current.companionLineageRef,
                    modelRef =
                        current.modelRef,
                    modelProfileRef =
                        current.modelProfileRef,
                    artifactRef =
                        current.artifactRef,
                    artifactContentSetSha256 =
                        current.artifactContentSetSha256,
                    runtimeAdapterRef =
                        current.runtimeAdapterRef,
                    runtimeClass =
                        current.runtimeClass,
                    endpoint =
                        current.endpoint,
                    requestModel =
                        current.requestModel,
                    runtimeReceiptPath =
                        current.runtimeReceiptPath,
                    automaticFallback =
                        current.automaticFallback,
                    automaticDownload =
                        current.automaticDownload,
                    automaticActivation =
                        current.automaticActivation,
                )
            val path =
                home.resolve(
                    VexLifeProdHome.ModelConfigurationRelativePath,
                )
            Files.writeString(
                path,
                VexContractJson.encode(
                    legacy,
                ),
            )
            val before =
                Files.readString(
                    path,
                )

            val migrated =
                assertIs<FileHereticModelConfigurationStore.Snapshot.Heretic>(
                    store.read(
                        fixture.manifest,
                    ),
                )
                    .value

            assertEquals(
                HereticModelConfiguration.Schema,
                migrated.schemaVersion,
            )
            assertEquals(
                HereticRuntimeRealization
                    .require(
                        modelRef = migrated.modelRef,
                        target = HereticRuntimeRealization.MacArm64,
                    )
                    .realizationRef,
                migrated.realizationRef,
            )
            assertEquals(
                before,
                Files.readString(
                    path,
                ),
                "read migration must not rewrite durable Home state",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun windowsQualifiedRealizationPersistsWithoutMacRuntimeIdentity() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-model-binding-windows-realization-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val store =
                FileHereticModelConfigurationStore(
                    home,
                )
            val realization =
                HereticRuntimeRealization.WindowsQwen4B
            val model =
                vexlife.model.heretic.HereticModelCatalog.Models.Qwen4B
            store.selectModel(
                manifest = fixture.manifest,
                model = model,
            )
            val evidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef =
                        "receipt.vexlife-prod.windows-heretic.4242",
                    runtimeState =
                        HereticRuntimeState.Qualified,
                    modelRef =
                        realization.modelRef,
                    realizationRef =
                        realization.realizationRef,
                    modelProfileRef =
                        realization.modelProfileRef,
                    artifactRef =
                        realization.artifact.artifactRef,
                    artifactContentSetSha256 =
                        realization.artifact.contentSetSha256,
                    runtimeAdapterRef =
                        realization.runtime.runtimeAdapterRef,
                    runtimeClass =
                        realization.runtime.runtimeClass,
                    endpoint =
                        realization.runtime.endpoint,
                    requestModel =
                        realization.runtime.requestModel,
                    qualification =
                        HereticRuntimeQualification.qualified(),
                )

            val transition =
                store.bindQualified(
                    manifest = fixture.manifest,
                    qualifiedRuntime = evidence,
                    runtimeReceiptPath =
                        realization.runtimeReceiptRelativePath,
                )

            assertEquals(
                FileHereticModelConfigurationStore.TransitionState.BoundToQualifiedRuntime,
                transition.state,
            )
            assertEquals(
                realization.realizationRef,
                transition.value.realizationRef,
            )
            assertEquals(
                realization.artifact.artifactRef,
                transition.value.artifactRef,
            )
            assertEquals(
                realization.runtime.runtimeAdapterRef,
                transition.value.runtimeAdapterRef,
            )
            assertEquals(
                realization.runtime.runtimeClass,
                transition.value.runtimeClass,
            )
            assertEquals(
                realization.runtimeReceiptRelativePath,
                transition.value.runtimeReceiptPath,
            )
            assertEquals(
                realization,
                HereticModelConfiguration.boundRealization(
                    transition.value,
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun unsupportedPersistedModelDefaultsToCurrentRecommendedModelWithoutNeedingTheOldModel() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-model-selection-convergence-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val store =
                FileHereticModelConfigurationStore(
                    home,
                )
            store.selectHeretic(
                fixture.manifest,
            )
            val current =
                assertIs<FileHereticModelConfigurationStore.Snapshot.Heretic>(
                    store.read(
                        fixture.manifest,
                    ),
                )
                    .value
            val unsupported =
                current.copy(
                    modelRef = "model.vexlife-prod.experimental.gpt-oss.removed",
                    modelProfileRef = "model-profile.vexlife-prod.experimental.gpt-oss.removed",
                    artifactRef = "artifact.vexlife-prod.experimental.gpt-oss.removed",
                    artifactContentSetSha256 = "b".repeat(64),
                )
            Files.writeString(
                home.resolve(
                    VexLifeProdHome.ModelConfigurationRelativePath,
                ),
                VexContractJson.encode(
                    unsupported,
                ),
            )

            val transition =
                store.ensureSupportedSelection(
                    fixture.manifest,
                    totalMemoryBytes = 64L * 1024L * 1024L * 1024L,
                )

            assertEquals(
                FileHereticModelConfigurationStore.TransitionState.DefaultedUnsupportedSelectionPending,
                transition.state,
            )
            assertTrue(transition.writePerformed)
            assertEquals(
                vexlife.model.heretic.HereticModelCatalog.Models.Qwen27B,
                HereticModelConfiguration.selected(
                    transition.value,
                ),
            )
            assertFalse(transition.value.automaticFallback)
            assertFalse(transition.value.automaticDownload)
            assertFalse(transition.value.automaticActivation)
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun unsupportedSelectionFromAnotherHomeScopeCannotAutoDefault() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-model-selection-foreign-scope-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val store =
                FileHereticModelConfigurationStore(
                    home,
                )
            store.selectHeretic(
                fixture.manifest,
            )
            val current =
                assertIs<FileHereticModelConfigurationStore.Snapshot.Heretic>(
                    store.read(
                        fixture.manifest,
                    ),
                )
                    .value
            val foreign =
                current.copy(
                    homeRef = "vex-home.device.vexlife.foreign",
                    modelRef = "model.vexlife-prod.experimental.gpt-oss.removed",
                )
            val path =
                home.resolve(
                    VexLifeProdHome.ModelConfigurationRelativePath,
                )
            Files.writeString(
                path,
                VexContractJson.encode(
                    foreign,
                ),
            )
            val before =
                Files.readString(
                    path,
                )

            val failure =
                assertFailsWith<IllegalArgumentException> {
                    store.ensureSupportedSelection(
                        fixture.manifest,
                        totalMemoryBytes = 64L * 1024L * 1024L * 1024L,
                    )
                }

            assertEquals(
                "Heretic model configuration Home identity drift",
                failure.message,
            )
            assertEquals(
                before,
                Files.readString(
                    path,
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun aPersonChoosesAnotherModelAndOnlyThatModelsRuntimeCanBindIt() {
        val parent = Files.createTempDirectory("vexlife-prod-model-choice-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            val fixture = CanonicalVexLifeHomeFixture.form(home)
            val store = FileHereticModelConfigurationStore(home)
            store.selectHeretic(fixture.manifest)
            store.bindQualified(manifest = fixture.manifest, qualifiedRuntime = qualifiedRuntime())
            assertEquals(vexlife.model.heretic.HereticModelCatalog.Models.Qwen27B, store.selectedModel(fixture.manifest))

            val nine = vexlife.model.heretic.HereticModelCatalog.Models.Qwen9B
            val switched = store.selectModel(fixture.manifest, nine)
            assertEquals(FileHereticModelConfigurationStore.TransitionState.ModelSwitchedPending, switched.state)
            assertEquals(HereticModelConfiguration.State.EstablishmentPending, switched.value.state)
            assertEquals(nine, store.selectedModel(fixture.manifest))
            assertEquals(FileHereticModelConfigurationStore.TransitionState.AlreadySelectedPending, store.selectModel(fixture.manifest, nine).state)

            // The 27B runtime that was serving this Home cannot bind it now that it chose 9B.
            assertFailsWith<IllegalArgumentException> {
                store.bindQualified(manifest = fixture.manifest, qualifiedRuntime = qualifiedRuntime())
            }
            val nineRuntime =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.nine",
                    runtimeState = HereticRuntimeState.Qualified,
                    modelRef = nine.identity.modelRef,
                    modelProfileRef = nine.identity.modelProfileRef,
                    artifactRef = nine.identity.artifactSetRef,
                    artifactContentSetSha256 = nine.identity.artifactContentSetSha256,
                    runtimeAdapterRef = nine.identity.runtimeAdapterRef,
                    runtimeClass = nine.identity.runtimeClass,
                    endpoint = nine.identity.endpoint,
                    requestModel = nine.identity.requestModel,
                    qualification = HereticRuntimeQualification.qualified(),
                )
            val bound =
                store.bindQualified(
                    manifest = fixture.manifest,
                    qualifiedRuntime = nineRuntime,
                    runtimeReceiptPath = HereticProductIdentity.ProductRuntimeReceiptRelativePath,
                )
            assertEquals(HereticModelConfiguration.State.BoundProductModel, bound.value.state)
            assertEquals(nine.modelRef, bound.value.modelRef)
            assertEquals(FileHereticModelConfigurationStore.TransitionState.AlreadyBound, store.selectModel(fixture.manifest, nine).state)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
