package vexlife.model

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticModelImport
import vexlife.model.heretic.HereticRuntimeConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HuggingFaceModelInspection
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.FileHereticModelImportStore
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile
import vexlife.partner.ExternalProviderWire
import vexlife.partner.VexLifePartnerAccountScope
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerLiveQualification
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerSupportQualification
import vexlife.service.VexLifeProdCurrentState
import vexlife.setup.VexLifeProdSetupModel

/**
 * Settings → Models against a real Home: choices persist, lawfulness is re-resolved, files are
 * exact.
 */
class VexLifeModelsTest {
    private val gib = 1024L * 1024L * 1024L

    private fun withHome(block: (Path, CanonicalVexLifeHomeFixture.Formed) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-models-settings-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            val fixture = CanonicalVexLifeHomeFixture.form(home)
            FileHereticModelConfigurationStore(home).selectHeretic(fixture.manifest)
            block(home, fixture)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private fun place(home: Path, entry: HereticModelCatalog.Entry, filename: String) {
        val folder = home.resolve(entry.modelRootRelativePath).resolve(entry.providerRevision)
        Files.createDirectories(folder)
        val member = entry.members.first { it.filename == filename }
        Files.write(folder.resolve(filename), ByteArray(member.expectedBytes.toInt()))
    }

    private fun models(
        home: Path,
        serving: () -> String? = { HereticModelCatalog.Models.Qwen27B.modelRef },
        qualifiedRuntimeEvidence:
            () -> HereticModelConfiguration.QualifiedRuntimeEvidence? = { null },
        qualifiedVisualInputEvidence:
            () -> VexLifeVisualInputQualification.QualifiedRuntimeEvidence? = { null },
        modelSwitchRealization: VexLifeModels.ModelSwitchRealization? = null,
        partnerSwitchRealization: VexLifeModels.PartnerSwitchRealization? = null,
        failDownload: Boolean = true,
        target: vexlife.platform.VexLifePlatformRealization.Target =
            HereticRuntimeRealization.MacArm64,
    ) =
        VexLifeModels(
            productHome = home,
            servingModelRef = serving,
            currentQualifiedRuntimeEvidence = qualifiedRuntimeEvidence,
            currentQualifiedVisualInputEvidence = qualifiedVisualInputEvidence,
            totalMemoryBytes = { 16 * gib },
            runtimeRealizations =
                HereticRuntimeRealization.Resolver(
                    HereticRuntimeRealization.TargetSource {
                        target
                    },
                ),
            modelSwitchRealization = modelSwitchRealization,
            partnerSwitchRealization = partnerSwitchRealization,
            acquirer = { _, _ ->
                HereticArtifactMaterialization.Acquirer { _, _ ->
                    if (failDownload) error("network unavailable in test")
                }
            },
        )

    private fun qualifiedRuntimeEvidence(
        model: HereticModelCatalog.Entry,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        val realization =
            HereticRuntimeRealization.require(
                modelRef = model.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.models-capabilities-test",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = model.modelRef,
            realizationRef = realization.realizationRef,
            modelProfileRef = realization.modelProfileRef,
            artifactRef = realization.artifact.artifactRef,
            artifactContentSetSha256 = realization.artifact.contentSetSha256,
            runtimeAdapterRef = realization.runtime.runtimeAdapterRef,
            runtimeClass = realization.runtime.runtimeClass,
            endpoint = realization.runtime.endpoint,
            requestModel = realization.runtime.requestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )
    }

    private fun partnerConnection(
        profile: VexLifePartnerModelProfileCatalog.Definition,
        qualified: Boolean = false,
    ): VexLifePartnerConnectionProjection.Projection =
        VexLifePartnerConnectionProjection.project(
            VexLifePartnerConnectionProjection.Observation(
                providerRef = profile.providerRef,
                modelRef = profile.modelRef,
                credential =
                    VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED,
                account =
                    VexLifePartnerConnectionProjection.AccountStanding.USABLE,
                service =
                    VexLifePartnerConnectionProjection.ServiceStanding.READY,
                modelAccess =
                    VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE,
                invocation =
                    if (
                        qualified
                    ) {
                        VexLifePartnerConnectionProjection.InvocationStanding.QUALIFIED
                    } else {
                        VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED
                    },
            ),
        )

    private fun combinedInferenceModels(
        owner: VexLifeModels,
        profile: VexLifePartnerModelProfileCatalog.Definition,
        partnerTargeted: Boolean,
        partnerServing: Boolean,
        partnerConnection:
            VexLifePartnerConnectionProjection.Projection =
            partnerConnection(
                profile,
            ),
        partnerSupport:
            VexLifePartnerSupportQualification.Projection =
            VexLifePartnerDeterministicSupport.projection(
                profile,
            ),
    ): VexLifeModelSourceProjection.Projection {
        val local =
            owner.projection()
                .models
                .map {
                    row ->
                    val entry =
                        HereticModelCatalog.Entries.single {
                            it.handle ==
                                row.handle
                        }
                    VexLifeModelSourceProjection.LocalOffering(
                        handle = row.handle,
                        modelRef = entry.modelRef,
                        targeted = row.targeted,
                        serving = row.serving,
                    )
                }
        return VexLifeModelSourceProjection.project(
            local = local,
            partner =
                listOf(
                    VexLifeModelSourceProjection.PartnerOffering(
                        handle = profile.profileRef,
                        providerRef = profile.providerRef,
                        modelRef = profile.modelRef,
                        targeted = partnerTargeted,
                        serving = partnerServing,
                        connection = partnerConnection,
                        support = partnerSupport,
                    ),
                ),
        )
    }

    private fun qualifiedPartnerOutcome(
        profile: VexLifePartnerModelProfileCatalog.Definition,
        credentialRef: String =
            "credential.vexlife.partner.models-switch-test",
    ): VexLifePartnerLiveQualification.Outcome.Qualified {
        val accountScope =
            VexLifePartnerAccountScope.Selection(
                providerRef = profile.providerRef,
            )
        val receipt =
            VexLifePartnerLiveQualification.Receipt(
                providerRef = profile.providerRef,
                modelRef = profile.modelRef,
                profileRef = profile.profileRef,
                credentialRef = credentialRef,
                accountScope = accountScope,
                requestBodySha256 =
                    "a".repeat(
                        64,
                    ),
                responseBodySha256 =
                    "b".repeat(
                        64,
                    ),
                providerRequestId =
                    "request.models-partner-switch-test",
                observedAt =
                    "2026-11-24T23:00:00Z",
                receiptRef =
                    "receipt.vexlife.partner.live.models-switch-test",
            )
        val support =
            VexLifePartnerSupportQualification.acceptLiveQualification(
                current =
                    VexLifePartnerDeterministicSupport.projection(
                        profile,
                    ),
                live =
                    receipt.supportLiveQualification(),
            )
        return VexLifePartnerLiveQualification.Outcome.Qualified(
            receipt = receipt,
            connection =
                partnerConnection(
                    profile = profile,
                    qualified = true,
                ),
            support = support,
        )
    }

    private fun heldPartnerOutcome(
        profile: VexLifePartnerModelProfileCatalog.Definition,
    ): VexLifePartnerLiveQualification.Outcome.Held =
        VexLifePartnerLiveQualification.Outcome.Held(
            connection =
                VexLifePartnerConnectionProjection.project(
                    VexLifePartnerConnectionProjection.Observation(
                        providerRef = profile.providerRef,
                        modelRef = profile.modelRef,
                        credential =
                            VexLifePartnerConnectionProjection.CredentialStanding.REJECTED,
                        account =
                            VexLifePartnerConnectionProjection.AccountStanding.UNKNOWN,
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
                        modelAccess =
                            VexLifePartnerConnectionProjection.ModelAccessStanding.UNKNOWN,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    ),
                ),
            support =
                VexLifePartnerDeterministicSupport.projection(
                    profile,
                ),
            providerFailure =
                ExternalProviderWire.Failure(
                    provider =
                        ExternalProviderWire.Provider.OPENAI_RESPONSES,
                    meaning =
                        ExternalProviderWire.FailureMeaning.CREDENTIAL_REJECTED,
                    retry =
                        ExternalProviderWire.RetryDisposition.DO_NOT_RETRY_UNCHANGED,
                    httpStatus = 401,
                    providerType = "invalid_request_error",
                    providerCode = "invalid_api_key",
                    message = "synthetic rejected credential",
                ),
        )

    @Test
    fun effectiveCapabilitiesRequireExactQualifiedRuntimeEvidence() = withHome { home, _ ->
        val current =
            HereticModelCatalog.Models.Qwen27B
        val evidence =
            qualifiedRuntimeEvidence(
                current,
            )

        models(
            home = home,
            qualifiedRuntimeEvidence = {
                evidence
            },
        )
            .use {
                owner ->
                val projection =
                    owner.projection()
                val serving =
                    projection.models.first {
                        it.handle ==
                            current.handle
                    }
                assertEquals(
                    VexLifeEffectiveModelCapabilities.RuntimeStanding.QUALIFIED_SERVING,
                    serving.capabilities.runtime,
                )
                assertEquals(
                    evidence.runtimeReceiptRef,
                    serving.capabilities.qualifiedRuntimeReceiptRef,
                )
                assertEquals(
                    VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
                    serving.capabilities.vexReach,
                )
                assertEquals(
                    VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
                    serving.capabilities.providerNativeTools,
                )
                assertEquals(
                    VexLifeEffectiveModelCapabilities.Standing.NOT_APPLICABLE,
                    serving.capabilities.reasoningEffortControl,
                )
                assertEquals(
                    VexLifeEffectiveModelCapabilities.Standing.HELD,
                    serving.capabilities.visualInput,
                    "visual input stays held until a separate qualified visual realization exists",
                )

                val four =
                    projection.models.first {
                        it.handle ==
                            HereticModelCatalog.Models.Qwen4B.handle
                    }
                assertEquals(
                    VexLifeEffectiveModelCapabilities.RuntimeStanding.REALIZABLE_NOT_QUALIFIED,
                    four.capabilities.runtime,
                )
                assertNull(
                    four.capabilities.qualifiedRuntimeReceiptRef,
                )
                assertEquals(
                    VexLifeEffectiveModelCapabilities.Standing.HELD,
                    four.capabilities.vexReach,
                )
                assertEquals(
                    VexLifeEffectiveModelCapabilities.Standing.NOT_APPLICABLE,
                    four.capabilities.providerNativeTools,
                )
                assertEquals(
                    VexLifeEffectiveModelCapabilities.Standing.HELD,
                    four.capabilities.visualInput,
                )
            }

        models(
            home = home,
            serving = {
                current.modelRef
            },
        )
            .use {
                owner ->
                val looseServing =
                    owner.projection()
                        .models
                        .first {
                            it.handle ==
                                current.handle
                        }
                assertTrue(
                    looseServing.serving,
                    "legacy serving observation may still identify the row",
                )
                assertEquals(
                    VexLifeEffectiveModelCapabilities.RuntimeStanding.REALIZABLE_NOT_QUALIFIED,
                    looseServing.capabilities.runtime,
                    "capabilities must not promote a loose serving name without exact qualified evidence",
                )
                assertEquals(
                    VexLifeEffectiveModelCapabilities.Standing.HELD,
                    looseServing.capabilities.vexReach,
                )
            }
    }

    @Test
    fun exactServing4BProjectsVisualAvailabilityOnlyWithMatchingCurrentEvidence() =
        withHome {
            home,
            _,
            ->
            val four =
                HereticModelCatalog.Models.Qwen4B
            val textRuntime =
                qualifiedRuntimeEvidence(
                    four,
                )
            val visual =
                VexLifeVisualInputQualification.QualifiedRuntimeEvidence(
                    evidenceRef =
                        "evidence.vexlife-prod.models-visual-test.001",
                    modelRef = four.modelRef,
                    artifactRef =
                        four.identity.artifactSetRef,
                    artifactContentSetSha256 =
                        four.identity.artifactContentSetSha256,
                    environmentFingerprintSha256 =
                        "e".repeat(
                            64,
                        ),
                )

            models(
                home = home,
                serving = {
                    four.modelRef
                },
                qualifiedRuntimeEvidence = {
                    textRuntime
                },
                qualifiedVisualInputEvidence = {
                    visual
                },
            )
                .use {
                    owner ->
                    val projection =
                        owner.projection()
                    val row =
                        projection.models.first {
                            it.handle ==
                                four.handle
                        }
                    assertEquals(
                        VexLifeEffectiveModelCapabilities.RuntimeStanding.QUALIFIED_SERVING,
                        row.capabilities.runtime,
                    )
                    assertEquals(
                        VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
                        row.capabilities.visualInput,
                    )
                    assertTrue(
                        projection.models
                            .filter {
                                it.handle !=
                                    four.handle
                            }
                            .all {
                                it.capabilities.visualInput ==
                                    VexLifeEffectiveModelCapabilities.Standing.HELD
                            },
                    )
                }
        }

    @Test
    fun everyOfferedModelIsListedWithWhatTheHomeHasOfIt() = withHome { home, _ ->
        models(home).use { owner ->
            val projection = owner.projection()
            assertEquals(
                HereticModelCatalog.Entries.map { it.handle },
                projection.models.map { it.handle },
            )
            val certified = projection.models.first { it.handle == "heretic-27b" }
            assertTrue(certified.chosen && certified.serving)
            assertEquals(false, certified.fitsThisMac)
            assertEquals(true, projection.models.first { it.handle == "heretic-9b" }.fitsThisMac)
            assertEquals(
                listOf(
                    VexLifeModelsFamily.ControlSelect,
                    VexLifeModelsFamily.ControlDownload,
                ),
                projection.models
                    .first { it.handle == "heretic-4b" }
                    .display
                    .controls
                    .map { it.controlRef },
            )
            assertFalse(projection.switchPending)
        }
    }

    @Test
    fun usingADownloadedModelConvergesTheServingModelNow() = withHome { home, fixture ->
        val nine = HereticModelCatalog.Models.Qwen9B
        nine.members
            .filter { it.expectedBytes < 50_000_000 }
            .forEach { place(home, nine, it.filename) }
        val folder = home.resolve(nine.modelRootRelativePath).resolve(nine.providerRevision)
        nine.members
            .filter { it.expectedBytes >= 50_000_000 }
            .forEach { member ->
                java.io.RandomAccessFile(folder.resolve(member.filename).toFile(), "rw").use {
                    it.setLength(member.expectedBytes)
                }
            }

        var serving: String? = HereticModelCatalog.Models.Qwen27B.modelRef
        val phases = mutableListOf<VexLifeModels.SwitchPhase>()
        val modelSwitchRealization =
            VexLifeModels.ModelSwitchRealization {
                realization,
                observe,
                ->
                observe(VexLifeModels.SwitchPhase.WaitingForCurrentTurn)
                observe(VexLifeModels.SwitchPhase.PreparingTarget)
                observe(VexLifeModels.SwitchPhase.SwitchingRuntime)
                phases +=
                    listOf(
                        VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                        VexLifeModels.SwitchPhase.PreparingTarget,
                        VexLifeModels.SwitchPhase.SwitchingRuntime,
                    )
                serving = realization.modelRef
            }

        models(
            home = home,
            serving = { serving },
            modelSwitchRealization = modelSwitchRealization,
        )
            .use {
                owner ->
                val selected =
                    owner.control(
                        VexLifeModelsFamily.ControlSelect,
                        "heretic-9b",
                    )
                assertEquals(
                    VexLifeModels.OutcomeState.Selected.stateRef,
                    selected.lastOutcome?.state,
                )
                assertFalse(
                    selected.models.first { it.handle == "heretic-9b" }.chosen,
                )
                assertTrue(
                    selected.models.first { it.handle == "heretic-9b" }.targeted,
                )
                assertFalse(
                    selected.models.first { it.handle == "heretic-9b" }.serving,
                )
                assertEquals(
                    HereticModelCatalog.Models.Qwen27B.modelRef,
                    serving,
                )
                assertTrue(
                    phases.isEmpty(),
                )
                assertEquals(
                    HereticModelCatalog.Models.Qwen27B,
                    FileHereticModelConfigurationStore(home).selectedModel(fixture.manifest),
                )

                owner.control(
                    VexLifeModelsFamily.ControlUse,
                    "heretic-9b",
                )
                owner.awaitIdle()
                val after = owner.projection()
                assertEquals(VexLifeModels.OutcomeState.Switched.stateRef, after.lastOutcome?.state)
                assertFalse(after.switchPending)
                assertEquals(null, after.switching)
                assertTrue(after.models.first { it.handle == "heretic-9b" }.chosen)
                assertTrue(after.models.first { it.handle == "heretic-9b" }.serving)
                assertEquals(
                    listOf(
                        VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                        VexLifeModels.SwitchPhase.PreparingTarget,
                        VexLifeModels.SwitchPhase.SwitchingRuntime,
                    ),
                    phases,
                )
                assertEquals(
                    nine,
                    FileHereticModelConfigurationStore(home).selectedModel(fixture.manifest),
                )
                assertTrue(Files.exists(folder.resolve("config.json")))
            }
    }

    @Test
    fun qualifiedPartnerUseBecomesTheOnlyLogicalServingInference() =
        withHome {
            home,
            _,
            ->
            val profile =
                VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
            val phases =
                mutableListOf<VexLifeModels.SwitchPhase>()
            val partnerSwitch =
                VexLifeModels.PartnerSwitchRealization {
                    target,
                    observe,
                    ->
                    assertEquals(
                        profile.profileRef,
                        target.handle,
                    )
                    observe(
                        VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                    )
                    observe(
                        VexLifeModels.SwitchPhase.PreparingTarget,
                    )
                    phases +=
                        listOf(
                            VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                            VexLifeModels.SwitchPhase.PreparingTarget,
                        )
                    qualifiedPartnerOutcome(
                        profile,
                    )
                }

            models(
                home = home,
                partnerSwitchRealization = partnerSwitch,
            )
                .use {
                    owner ->
                    val before =
                        combinedInferenceModels(
                            owner = owner,
                            profile = profile,
                            partnerTargeted = false,
                            partnerServing = false,
                        )
                    val target =
                        owner.stageInferenceTarget(
                            models = before,
                            handle = profile.profileRef,
                        )
                    val targeted =
                        combinedInferenceModels(
                            owner = owner,
                            profile = profile,
                            partnerTargeted = true,
                            partnerServing = false,
                        )

                    owner.useStagedPartnerInference(
                        targeted,
                    )
                    owner.awaitIdle()

                    assertEquals(
                        target,
                        owner.partnerServing()
                            ?.coordinate,
                    )
                    assertEquals(
                        VexLifeModels.OutcomeState.Switched.stateRef,
                        owner.projection()
                            .lastOutcome
                            ?.state,
                    )
                    assertNull(
                        owner.projection()
                            .servingHandle,
                        "Local Models must stop projecting Local as serving while Partner is serving",
                    )
                    assertTrue(
                        owner.projection()
                            .models
                            .none {
                                it.serving
                            },
                    )
                    assertEquals(
                        listOf(
                            VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                            VexLifeModels.SwitchPhase.PreparingTarget,
                        ),
                        phases,
                    )
                    assertEquals(
                        target,
                        owner.stagedInferenceTarget(),
                        "Partner target remains process-local because there is no durable Partner choice store",
                    )
                }
        }

    @Test
    fun heldPartnerUseLeavesExistingLocalServingTruthUnchanged() =
        withHome {
            home,
            _,
            ->
            val profile =
                VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
            models(
                home = home,
                partnerSwitchRealization =
                    VexLifeModels.PartnerSwitchRealization {
                        _,
                        _,
                        ->
                        heldPartnerOutcome(
                            profile,
                        )
                    },
            )
                .use {
                    owner ->
                    val before =
                        combinedInferenceModels(
                            owner = owner,
                            profile = profile,
                            partnerTargeted = false,
                            partnerServing = false,
                        )
                    owner.stageInferenceTarget(
                        models = before,
                        handle = profile.profileRef,
                    )
                    val targeted =
                        combinedInferenceModels(
                            owner = owner,
                            profile = profile,
                            partnerTargeted = true,
                            partnerServing = false,
                        )

                    owner.useStagedPartnerInference(
                        targeted,
                    )
                    owner.awaitIdle()

                    assertNull(
                        owner.partnerServing(),
                    )
                    assertEquals(
                        HereticModelCatalog.Models.Qwen27B.handle,
                        owner.projection()
                            .servingHandle,
                    )
                    assertTrue(
                        owner.projection()
                            .models
                            .single {
                                it.handle ==
                                    HereticModelCatalog.Models.Qwen27B.handle
                            }
                            .serving,
                    )
                    assertEquals(
                        VexLifeModels.OutcomeState.Failed.stateRef,
                        owner.projection()
                            .lastOutcome
                            ?.state,
                    )
                }
        }

    @Test
    fun qualifiedLocalSwitchClearsPartnerServingOnlyAfterLocalBecomesServing() =
        withHome {
            home,
            _,
            ->
            val nine =
                HereticModelCatalog.Models.Qwen9B
            nine.members
                .filter {
                    it.expectedBytes <
                        50_000_000
                }
                .forEach {
                    place(
                        home,
                        nine,
                        it.filename,
                    )
                }
            val folder =
                home.resolve(
                    nine.modelRootRelativePath,
                )
                    .resolve(
                        nine.providerRevision,
                    )
            nine.members
                .filter {
                    it.expectedBytes >=
                        50_000_000
                }
                .forEach {
                    member ->
                    java.io.RandomAccessFile(
                        folder.resolve(
                            member.filename,
                        )
                            .toFile(),
                        "rw",
                    )
                        .use {
                            it.setLength(
                                member.expectedBytes,
                            )
                        }
                }

            val profile =
                VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
            var localServing: String? =
                HereticModelCatalog.Models.Qwen27B.modelRef
            val localSwitch =
                VexLifeModels.ModelSwitchRealization {
                    realization,
                    observe,
                    ->
                    observe(
                        VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                    )
                    observe(
                        VexLifeModels.SwitchPhase.PreparingTarget,
                    )
                    localServing =
                        realization.modelRef
                }
            val partnerSwitch =
                VexLifeModels.PartnerSwitchRealization {
                    _,
                    observe,
                    ->
                    observe(
                        VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                    )
                    observe(
                        VexLifeModels.SwitchPhase.PreparingTarget,
                    )
                    qualifiedPartnerOutcome(
                        profile,
                    )
                }

            models(
                home = home,
                serving = {
                    localServing
                },
                modelSwitchRealization = localSwitch,
                partnerSwitchRealization = partnerSwitch,
            )
                .use {
                    owner ->
                    val beforePartner =
                        combinedInferenceModels(
                            owner = owner,
                            profile = profile,
                            partnerTargeted = false,
                            partnerServing = false,
                        )
                    owner.stageInferenceTarget(
                        models = beforePartner,
                        handle = profile.profileRef,
                    )
                    owner.useStagedPartnerInference(
                        combinedInferenceModels(
                            owner = owner,
                            profile = profile,
                            partnerTargeted = true,
                            partnerServing = false,
                        ),
                    )
                    owner.awaitIdle()

                    assertEquals(
                        profile.profileRef,
                        owner.partnerServing()
                            ?.coordinate
                            ?.handle,
                    )
                    assertNull(
                        owner.projection()
                            .servingHandle,
                    )
                    assertEquals(
                        HereticModelCatalog.Models.Qwen27B.modelRef,
                        localServing,
                        "Partner serving does not kill or silently retarget the Local runtime",
                    )

                    owner.control(
                        VexLifeModelsFamily.ControlSelect,
                        nine.handle,
                    )
                    owner.control(
                        VexLifeModelsFamily.ControlUse,
                        nine.handle,
                    )
                    owner.awaitIdle()

                    assertEquals(
                        nine.modelRef,
                        localServing,
                    )
                    assertNull(
                        owner.partnerServing(),
                    )
                    assertEquals(
                        nine.handle,
                        owner.projection()
                            .servingHandle,
                    )
                    assertTrue(
                        owner.projection()
                            .models
                            .single {
                                it.handle ==
                                    nine.handle
                            }
                            .serving,
                    )
                }
        }

    @Test
    fun removingAModelFreesItsFilesAndKeepsItOffered() = withHome { home, _ ->
        val four = HereticModelCatalog.Models.Qwen4B
        place(home, four, "config.json")
        models(home).use { owner ->
            val after = owner.control(VexLifeModelsFamily.ControlRemove, "heretic-4b")
            assertEquals(VexLifeModels.OutcomeState.Removed.stateRef, after.lastOutcome?.state)
            val row = after.models.first { it.handle == "heretic-4b" }
            assertEquals(0L, row.presentBytes)
            assertEquals(
                listOf(
                    VexLifeModelsFamily.ControlSelect,
                    VexLifeModelsFamily.ControlDownload,
                ),
                row.display.controls.map { it.controlRef },
            )
        }
    }

    @Test
    fun selectedModelConfigurationChangesWithoutStartingTheRuntime() =
        withHome { home, _ ->
            models(
                home = home,
            )
                .use {
                    owner ->
                    val before =
                        owner.projection()
                            .models
                            .first {
                                it.handle == "heretic-4b"
                            }
                    assertEquals(
                        1,
                        requireNotNull(before.runtimeConfiguration).promptCacheEntries,
                    )
                    assertEquals(
                        2,
                        requireNotNull(before.runtimeConfiguration).recommendedPromptCacheEntries,
                    )
                    assertEquals(
                        3,
                        requireNotNull(before.runtimeConfiguration).maximumPromptCacheEntries,
                    )
                    assertEquals(
                        HereticRuntimeConfiguration.RecommendationExperimentalCandidate,
                        requireNotNull(before.runtimeConfiguration).recommendationStanding,
                    )

                    owner.control(
                        VexLifeModelsFamily.ControlSelect,
                        "heretic-4b",
                    )
                    val experimental =
                        owner.control(
                            VexLifeModelsFamily.ControlSetPromptCacheEntries,
                            "heretic-4b",
                            promptCacheEntries = 3,
                        )
                    assertEquals(
                        3,
                        experimental.models
                            .first {
                                it.handle == "heretic-4b"
                            }
                            .runtimeConfiguration
                            .let(::requireNotNull)
                            .promptCacheEntries,
                    )
                    val configured =
                        owner.control(
                            VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration,
                            "heretic-4b",
                        )
                    val four =
                        configured.models.first {
                            it.handle == "heretic-4b"
                        }

                    assertFalse(
                        four.chosen,
                    )
                    assertTrue(
                        four.targeted,
                    )
                    assertFalse(
                        four.serving,
                    )
                    assertEquals(
                        HereticRuntimeConfiguration.ModeExplicit,
                        requireNotNull(four.runtimeConfiguration).mode,
                    )
                    assertEquals(
                        2,
                        requireNotNull(four.runtimeConfiguration).promptCacheEntries,
                    )
                    assertEquals(
                        "heretic-27b",
                        configured.servingHandle,
                    )
                    assertEquals(
                        "heretic-4b",
                        configured.selectedHandle,
                    )
                    assertNull(
                        configured.switching,
                    )
                    val manifest =
                        requireNotNull(
                            vexlife.home.VexLifeProdHomeFiles.observeExisting(
                                home,
                            ),
                        ).manifest
                    assertEquals(
                        HereticModelCatalog.Models.Qwen27B,
                        FileHereticModelConfigurationStore(home).selectedModel(manifest),
                    )
                }
        }

    private fun assertImportedModelSurvivesRestart(
        home: Path,
        record: HereticModelImport.Record,
        handle: String,
    ) {
        val observedHome =
            requireNotNull(VexLifeProdHomeFiles.observeExisting(home))
        val imported =
            requireNotNull(
                FileHereticModelImportStore(home)
                    .entries()
                    .singleOrNull { it.handle == handle },
            )
        val configuration = FileHereticModelConfigurationStore(home)

        // Restart reconstruction begins after Start/Use has committed the staged target into Home
        // truth. Select itself remains process-local UI intent and is intentionally not durable.
        configuration.selectModel(
            observedHome.manifest,
            imported,
        )

        val setupAfterRestart =
            VexLifeProdSetupModel.observe(
                productHome = home,
                explicit = null,
                language = "en",
                totalMemoryBytes = 16 * gib,
                runtimeRealizations =
                    HereticRuntimeRealization.Resolver(
                        HereticRuntimeRealization.TargetSource {
                            HereticRuntimeRealization.MacArm64
                        },
                    ),
            )
        assertEquals(handle, setupAfterRestart.selected)
        val setupImported = setupAfterRestart.models.single { it.handle == handle }
        assertNull(setupImported.fits)
        assertFalse(setupImported.recommended)
        assertTrue(setupImported.line.contains("memory fit not established"))

        val currentAfterRestart =
            VexLifeProdCurrentState.observeWithHooks(
                VexLifeProdCurrentState.Hooks(
                    resolveProductHome = { home },
                    observeLocation = { observedHome ->
                        VexLifeProdHomeFiles.observeLocation(observedHome)
                    },
                    observeHost = {
                        HereticMacHostProfile.evaluate(
                            HereticMacHostProfile.Observation(
                                platform = "darwin",
                                architecture = "arm64",
                                chipModel = "Apple M4 Pro",
                                totalMemoryBytes = 16 * gib,
                            )
                        )
                    },
                    observeModel = { observedHome, manifest ->
                        FileHereticModelConfigurationStore(observedHome).read(manifest)
                    },
                    observeArtifacts = { observedHome, model ->
                        HereticArtifactMaterialization.observePresence(observedHome, model)
                    },
                    observeFreeStorageBytes = { 50 * gib },
                    now = { Instant.parse("2026-11-18T12:00:00Z") },
                )
            )
        assertEquals(record.modelRef, currentAfterRestart.activeModelRef)
        assertNull(currentAfterRestart.modelMinimumMemoryBytes)

        // Restore this lifecycle test's original built-in Home choice so its later Remove / Forget
        // assertions continue testing cleanup rather than model-switch retirement semantics.
        configuration.selectModel(
            observedHome.manifest,
            HereticModelCatalog.Models.Qwen27B,
        )
    }

    @Test
    fun importedMlxOfferingRemainsVisibleButNotRunnableOnWindows() =
        withHome { home, _ ->
            val record =
                HereticModelImport.Record(
                    importRef = "model-import.vexlife-prod.huggingface.windows-held",
                    sourceUrl = "https://huggingface.co/example/Windows-Held-MLX",
                    providerRepository = "example/Windows-Held-MLX",
                    providerRevision = "2".repeat(40),
                    handle = "hf-windows-held",
                    humanLabel = "Windows Held MLX",
                    modelBundleRef = "model-bundle.vexlife.imported.hf-windows-held.001",
                    generationRef = "generation.vexlife-prod.imported-provider." + "2".repeat(12),
                    modelRef = "model.vexlife-prod.imported.hf-windows-held.001",
                    modelProfileRef = "model-profile.vexlife-prod.imported.hf-windows-held.candidate",
                    artifactSetRef = "artifact-set.vexlife-prod.imported.hf-windows-held.001",
                    artifactCustodyEvidenceRef =
                        "artifact-custody.vexlife-prod.imported.hf-windows-held.001",
                    artifactContentSetSha256 =
                        "54a5a3d062d90b8095d66e0633d46a94daf5c33d84218dc147e17c34f7e90841",
                    modelRootRelativePath = "models/heretic/hf-windows-held",
                    providerLicense = "apache-2.0",
                    representationRef = HereticModelImport.RepresentationQwen3OpenAiChat,
                    companionProfileRef =
                        "companion-profile.vexlife-prod.imported.hf-windows-held.001",
                    members =
                        listOf(
                            HereticModelImport.Member(
                                roleRef =
                                    "artifact-role.vexlife-prod.imported.config",
                                artifactRef =
                                    "artifact.vexlife-prod.imported.hf-windows-held.member.config",
                                filename = "config.json",
                                mediaType = "application/json",
                                sha256 = "4".repeat(64),
                                expectedBytes = 1L,
                                sourceRef =
                                    "source.huggingface.example.windows-held." +
                                        "2".repeat(40),
                                licenseRef =
                                    "license.huggingface.example.windows-held.apache-2.0",
                            ),
                        ),
                )
            FileHereticModelImportStore(home).add(record)

            models(
                home = home,
                target = HereticRuntimeRealization.WindowsX64,
            ).use { owner ->
                val row =
                    owner.projection()
                        .models
                        .single { it.handle == record.handle }
                assertTrue(row.imported)
                assertEquals(
                    "workspace.status.model-control-not-lawful",
                    row.display.status.key,
                )
                assertTrue(
                    row.display.controls.none {
                        it.controlRef in
                            setOf(
                                VexLifeModelsFamily.ControlSelect,
                                VexLifeModelsFamily.ControlUse,
                                VexLifeModelsFamily.ControlDownload,
                                VexLifeModelsFamily.ControlRedownload,
                            )
                    },
                )
            }
        }

    @Test
    fun importedModelCanBeAddedDownloadedRemovedRedownloadedAndForgotten() =
        withHome { home, _ ->
            val bytes = "{\"model_type\":\"qwen3\"}".toByteArray()
            val sha =
                MessageDigest.getInstance("SHA-256")
                    .digest(bytes)
                    .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            val revision = "1".repeat(40)
            val planMember =
                HereticArtifactMaterialization.PlanMember(
                    roleRef = "artifact-role.vexlife-prod.imported.member.config-json",
                    artifactRef = "artifact.vexlife-prod.imported.test.member.config-json",
                    logicalPath = "config.json",
                    sha256 = sha,
                    byteLength = bytes.size.toLong(),
                )
            val contentSet =
                HereticArtifactMaterialization.computeContentSetSha256(
                    listOf(planMember),
                )
            val handle = "hf-tiny-qwen-" + contentSet.take(8)
            val record =
                HereticModelImport.Record(
                    importRef = "model-import.vexlife-prod.huggingface." + contentSet.take(24),
                    sourceUrl = "https://huggingface.co/example/Tiny-Qwen",
                    providerRepository = "example/Tiny-Qwen",
                    providerRevision = revision,
                    handle = handle,
                    humanLabel = "Tiny-Qwen",
                    modelBundleRef =
                        "model-bundle.vexlife.imported." +
                            handle +
                            "." +
                            contentSet.take(12) +
                            ".001",
                    generationRef =
                        "generation.vexlife-prod.imported-provider." +
                            revision.take(12),
                    modelRef =
                        "model.vexlife-prod.imported." +
                            handle +
                            "." +
                            contentSet.take(12),
                    modelProfileRef =
                        "model-profile.vexlife-prod.imported." +
                            handle +
                            "." +
                            contentSet.take(12) +
                            ".candidate",
                    artifactSetRef =
                        "artifact.vexlife-prod.imported." +
                            handle +
                            "." +
                            contentSet.take(12),
                    artifactCustodyEvidenceRef =
                        "resolution.vexlife-prod.model-import.provider-pin." +
                            revision.take(12) +
                            "." +
                            contentSet.take(12),
                    artifactContentSetSha256 = contentSet,
                    modelRootRelativePath = "models/heretic/imported." + handle,
                    providerLicense = "apache-2.0",
                    representationRef = HereticModelImport.RepresentationQwen3OpenAiChat,
                    companionProfileRef =
                        "companion-profile.vexlife-prod.imported." +
                            handle +
                            ".qwen3.v1",
                    members =
                        listOf(
                            HereticModelImport.Member(
                                roleRef = planMember.roleRef,
                                artifactRef = planMember.artifactRef,
                                filename = planMember.logicalPath,
                                mediaType = "application/json",
                                sha256 = planMember.sha256,
                                expectedBytes = planMember.byteLength,
                                sourceRef = "hf.example.tiny-qwen." + revision,
                                licenseRef = "license.huggingface.apache-2.0",
                            ),
                        ),
                )
            val candidate =
                HuggingFaceModelInspection.Candidate(
                    importRef = record.importRef,
                    sourceUrl = record.sourceUrl,
                    providerRepository = record.providerRepository,
                    providerRevision = record.providerRevision,
                    humanLabel = record.humanLabel,
                    providerLicense = record.providerLicense,
                    totalBytes = bytes.size.toLong(),
                    memberCount = 1,
                    relationship = record.representationRef,
                    record = record,
                )

            VexLifeModels(
                productHome = home,
                servingModelRef = { HereticModelCatalog.Models.Qwen27B.modelRef },
                totalMemoryBytes = { 16 * gib },
                runtimeRealizations =
                    HereticRuntimeRealization.Resolver(
                        HereticRuntimeRealization.TargetSource {
                            HereticRuntimeRealization.MacArm64
                        },
                    ),
                acquirer = { _, progress ->
                    HereticArtifactMaterialization.Acquirer { _, finalPath ->
                        Files.write(finalPath, bytes)
                        progress(bytes.size.toLong())
                    }
                },
                inspectImport = { candidate },
            ).use { owner ->
                val checked =
                    owner.checkImport(
                        "https://huggingface.co/example/Tiny-Qwen",
                    )
                assertEquals("READY", checked.modelImport?.state)
                assertFalse(checked.models.any { it.handle == handle })

                val added = owner.addCheckedImport(candidate.importRef)
                val imported = added.models.single { it.handle == handle }
                assertTrue(imported.imported)
                assertNull(imported.recommendedMemoryBytes)
                assertNull(imported.fitsThisMac)
                assertTrue(
                    FileHereticModelImportStore(home)
                        .entries()
                        .any { it.handle == handle },
                )

                owner.control(VexLifeModelsFamily.ControlSelect, handle)

                assertImportedModelSurvivesRestart(
                    home = home,
                    record = record,
                    handle = handle,
                )

                owner.control(VexLifeModelsFamily.ControlDownload, handle)
                owner.awaitIdle()
                var current = owner.projection().models.single { it.handle == handle }
                assertTrue(current.presentBytes > 0)
                assertTrue(
                    VexLifeModelsFamily.ControlRemove in
                        current.display.controls.map { it.controlRef },
                )

                owner.control(VexLifeModelsFamily.ControlRemove, handle)
                current = owner.projection().models.single { it.handle == handle }
                assertEquals(0L, current.presentBytes)

                owner.control(VexLifeModelsFamily.ControlSelect, handle)
                assertTrue(
                    VexLifeModelsFamily.ControlDownload in
                        owner.projection()
                            .models
                            .single { it.handle == handle }
                            .display
                            .controls
                            .map { it.controlRef },
                )
                owner.control(VexLifeModelsFamily.ControlDownload, handle)
                owner.awaitIdle()
                assertTrue(
                    owner.projection()
                        .models
                        .single { it.handle == handle }
                        .presentBytes > 0,
                )

                owner.control(VexLifeModelsFamily.ControlRemove, handle)
                owner.control(VexLifeModelsFamily.ControlSelect, handle)
                val beforeForget = owner.projection().models.single { it.handle == handle }
                assertTrue(
                    VexLifeModelsFamily.ControlForgetImportedModel in
                        beforeForget.display.controls.map { it.controlRef },
                )
                val forgotten =
                    owner.control(
                        VexLifeModelsFamily.ControlForgetImportedModel,
                        handle,
                    )
                assertFalse(forgotten.models.any { it.handle == handle })
                assertEquals(
                    VexLifeModels.OutcomeState.Forgotten.stateRef,
                    forgotten.lastOutcome?.state,
                )
                assertTrue(FileHereticModelImportStore(home).entries().isEmpty())
            }
        }

    @Test
    fun aDownloadThatCannotFinishSaysSoAndLeavesNothingRunning() = withHome { home, _ ->
        models(home).use { owner ->
            owner.control(VexLifeModelsFamily.ControlDownload, "heretic-4b")
            owner.awaitIdle()
            val after = owner.projection()
            assertNull(after.downloading)
            assertEquals(VexLifeModels.OutcomeState.Failed.stateRef, after.lastOutcome?.state)
            // The model can be downloaded again; nothing about the failure is hidden.
            assertTrue(
                VexLifeModelsFamily.ControlDownload in
                    after.models
                        .first { it.handle == "heretic-4b" }
                        .display
                        .controls
                        .map { it.controlRef }
            )
        }
    }
}
