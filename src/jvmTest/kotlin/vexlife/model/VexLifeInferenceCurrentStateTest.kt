package vexlife.model

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerSupportQualification

class VexLifeInferenceCurrentStateTest {
    private val gib =
        1024L * 1024L * 1024L

    private fun <T> withLocalProjection(
        block: (
            home: Path,
            local: VexLifeModels.Projection,
        ) -> T,
    ): T {
        val parent =
            Files.createTempDirectory(
                "vexlife-inference-current-state-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        return try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            FileHereticModelConfigurationStore(
                home,
            )
                .selectHeretic(
                    fixture.manifest,
                )

            VexLifeModels(
                productHome = home,
                servingModelRef = {
                    HereticModelCatalog.Models.Qwen27B.modelRef
                },
                totalMemoryBytes = {
                    16 * gib
                },
                runtimeRealizations =
                    HereticRuntimeRealization.Resolver(
                        HereticRuntimeRealization.TargetSource {
                            HereticRuntimeRealization.MacArm64
                        },
                    ),
            )
                .use {
                    block(
                        home,
                        it.projection(),
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun connection(
        providerRef: String,
        modelRef: String,
    ): VexLifePartnerConnectionProjection.Projection =
        VexLifePartnerConnectionProjection.project(
            VexLifePartnerConnectionProjection.Observation(
                providerRef = providerRef,
                modelRef = modelRef,
                credential =
                    VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED,
                account =
                    VexLifePartnerConnectionProjection.AccountStanding.USABLE,
                service =
                    VexLifePartnerConnectionProjection.ServiceStanding.READY,
                modelAccess =
                    VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE,
                invocation =
                    VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED,
            ),
        )

    private fun support(
        providerRef: String,
        modelRef: String,
    ): VexLifePartnerSupportQualification.Projection =
        VexLifePartnerSupportQualification.project(
            VexLifePartnerSupportQualification.Evidence(
                providerRef = providerRef,
                modelRef = modelRef,
                contractFixtureProofRef =
                    "proof.partner.contract.fixture",
                adapterConformanceProofRef =
                    "proof.partner.adapter.fixture",
                vexRelationshipConformanceProofRef =
                    "proof.partner.vex.fixture",
                liveQualificationReceiptRef =
                    null,
            ),
        )

    private fun partner(
        targeted: Boolean = false,
        serving: Boolean = false,
    ): VexLifePartnerModelsCurrentState.Projection {
        val providerRef =
            "provider.openai.api"
        val modelRef =
            "model.openai.fixture"

        return VexLifePartnerModelsCurrentState.Projection(
            configuredProviders =
                emptyList(),
            connectionByProfileRef =
                emptyMap(),
            offerings =
                listOf(
                    VexLifeModelSourceProjection.PartnerOffering(
                        handle = "openai-fixture",
                        providerRef = providerRef,
                        modelRef = modelRef,
                        targeted = targeted,
                        serving = serving,
                        connection =
                            connection(
                                providerRef = providerRef,
                                modelRef = modelRef,
                            ),
                        support =
                            support(
                                providerRef = providerRef,
                                modelRef = modelRef,
                            ),
                    ),
                ),
        )
    }

    @Test
    fun localCurrentnessAndPartnerInventoryJoinWithoutChangingLocalTarget() =
        withLocalProjection {
            home,
            local,
            ->
            val current =
                VexLifeInferenceCurrentState.project(
                    productHome = home,
                    local = local,
                    partner = partner(),
                )

            assertEquals(
                "heretic-27b",
                current.inference.targeted?.handle,
            )
            assertEquals(
                "heretic-27b",
                current.inference.serving?.handle,
            )
            assertEquals(
                VexLifeModelSourceProjection.SourceKind.LOCAL,
                current.inference.targeted?.sourceKind,
            )
            assertFalse(
                current.inference.switchPending,
            )
            assertFalse(
                current.inference.sourceSwitchPending,
            )
            assertTrue(
                current.models.rows.any {
                    it.handle ==
                        "openai-fixture"
                },
            )
        }

    @Test
    fun explicitPartnerTargetReprojectsOneCrossSourcePendingSwitchWithoutRuntimeEffect() =
        withLocalProjection {
            home,
            local,
            ->
            val current =
                VexLifeInferenceCurrentState.project(
                    productHome = home,
                    local = local,
                    partner = partner(),
                    stagedTargetHandleOrNull =
                        "openai-fixture",
                )

            assertEquals(
                "openai-fixture",
                current.inference.targeted?.handle,
            )
            assertEquals(
                VexLifeModelSourceProjection.SourceKind.PARTNER,
                current.inference.targeted?.sourceKind,
            )
            assertEquals(
                "heretic-27b",
                current.inference.serving?.handle,
            )
            assertEquals(
                VexLifeModelSourceProjection.SourceKind.LOCAL,
                current.inference.serving?.sourceKind,
            )
            assertTrue(
                current.inference.switchPending,
            )
            assertTrue(
                current.inference.sourceSwitchPending,
            )
            assertFalse(
                current.models.rows
                    .single {
                        it.handle ==
                            "heretic-27b"
                    }
                    .targeted,
            )
            assertTrue(
                current.models.rows
                    .single {
                        it.handle ==
                            "openai-fixture"
                    }
                    .targeted,
            )
        }

    @Test
    fun unknownStagedTargetFailsClosed() =
        withLocalProjection {
            home,
            local,
            ->
            assertFailsWith<IllegalArgumentException> {
                VexLifeInferenceCurrentState.project(
                    productHome = home,
                    local = local,
                    partner = partner(),
                    stagedTargetHandleOrNull =
                        "not-a-current-row",
                )
            }
        }

    @Test
    fun duplicateServingAcrossLocalAndPartnerFailsInsteadOfInventingCurrentInference() =
        withLocalProjection {
            home,
            local,
            ->
            assertFailsWith<IllegalArgumentException> {
                VexLifeInferenceCurrentState.project(
                    productHome = home,
                    local = local,
                    partner =
                        partner(
                            serving = true,
                        ),
                )
            }
        }
}

// [VXG RealForever]
