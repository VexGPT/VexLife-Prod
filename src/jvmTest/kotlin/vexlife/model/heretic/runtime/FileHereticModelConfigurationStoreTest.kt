package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHome
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
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
                "HERETIC_SELECTED_PENDING",
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
                "HERETIC_ALREADY_SELECTED_PENDING",
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
                "HERETIC_BOUND_TO_QUALIFIED_RUNTIME",
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
                "HERETIC_ALREADY_BOUND",
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
                "HERETIC_REBOUND_TO_QUALIFIED_RUNTIME",
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
    fun aPersonChoosesAnotherModelAndOnlyThatModelsRuntimeCanBindIt() {
        val parent = Files.createTempDirectory("vexlife-prod-model-choice-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            val fixture = CanonicalVexLifeHomeFixture.form(home)
            val store = FileHereticModelConfigurationStore(home)
            store.selectHeretic(fixture.manifest)
            store.bindQualified(manifest = fixture.manifest, qualifiedRuntime = qualifiedRuntime())
            assertEquals(vexlife.model.heretic.HereticModelCatalog.Certified27B, store.selectedModel(fixture.manifest))

            val nine = vexlife.model.heretic.HereticModelCatalog.Candidate9B
            val switched = store.selectModel(fixture.manifest, nine)
            assertEquals("HERETIC_MODEL_SWITCHED_PENDING", switched.state)
            assertEquals(HereticModelConfiguration.State.EstablishmentPending, switched.value.state)
            assertEquals(nine, store.selectedModel(fixture.manifest))
            assertEquals("HERETIC_ALREADY_SELECTED_PENDING", store.selectModel(fixture.manifest, nine).state)

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
            assertEquals("HERETIC_ALREADY_BOUND", store.selectModel(fixture.manifest, nine).state)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
