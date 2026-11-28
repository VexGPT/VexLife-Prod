package vexlife.harness

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.VexLifeModels
import vexlife.model.VexLifeModelsFamily
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.HereticRealizationArtifactMaterialization

/**
 * [vexbody 20b test] explicit artifact-acquisition edge.
 *
 * This test-only host entry invokes the existing Product Models Family DOWNLOAD_MODEL Control.
 * It does not select the target model, start/switch a model runtime, or redefine artifact identity.
 * The Product's existing VexLifeModels -> HereticRealizationArtifactMaterialization relationship
 * owns acquisition, hashing, reuse, setup-lease serialization, and download outcome.
 */
object VexBody20BArtifactAcquire {
    private const val ResultClass =
        "VEXBODY_20B_ARTIFACT_ACQUISITION_RESULT"

    private const val PollMillis =
        15_000L

    private const val MaximumWaitMillis =
        8L * 60L * 60L * 1000L

    private val json =
        Json {
            prettyPrint = true
        }

    fun run(
        resultDir: Path,
    ): Int {
        require(
            System.getProperty(
                "os.name",
            ) ==
                "Mac OS X",
        ) {
            "VexBody 20B artifact acquisition requires the current Mac host"
        }
        require(
            System.getProperty(
                "os.arch",
            ) in
                setOf(
                    "aarch64",
                    "arm64",
                ),
        ) {
            "VexBody 20B artifact acquisition requires Apple Silicon"
        }

        Files.createDirectories(
            resultDir,
        )

        val productHome =
            VexLifeProdHomeFiles
                .defaultMacRoot(
                    Path.of(
                        System.getProperty(
                            "user.home",
                        ),
                    ),
                )
                .toAbsolutePath()
                .normalize()
        val home =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    productHome,
                ),
            ) {
                "VexBody 20B artifact acquisition requires the current canonical Vex Home"
            }
        val modelStore =
            FileHereticModelConfigurationStore(
                productHome,
            )
        val startingModel =
            modelStore.selectedModel(
                home.manifest,
            )
        val expectedStartingModel =
            HereticModelCatalog.Models.Qwen4B
        val targetModel =
            HereticModelCatalog.Models.GptOss20B

        require(
            startingModel.modelRef ==
                expectedStartingModel.modelRef
        ) {
            "VexBody 20B artifact acquisition requires exact Qwen 4B to remain selected"
        }

        val resolver =
            HereticRuntimeRealization.Resolver(
                HereticRuntimeRealization.TargetSource {
                    HereticRuntimeRealization.MacArm64
                },
            )
        val realization =
            when (
                val resolution =
                    resolver.resolve(
                        targetModel,
                    )
            ) {
                is HereticRuntimeRealization.Resolution.Supported ->
                    resolution.realization

                is HereticRuntimeRealization.Resolution.Unsupported ->
                    error(
                        "GPT-OSS 20B has no admitted Mac runtime realization",
                    )
            }

        val before =
            HereticRealizationArtifactMaterialization.observe(
                productHome = productHome,
                realization = realization,
            )

        val models =
            VexLifeModels(
                productHome = productHome,
                servingModelRef = {
                    expectedStartingModel.modelRef
                },
                totalMemoryBytes = {
                    null
                },
                runtimeRealizations = resolver,
            )

        var timedOut =
            false
        var controlInvoked =
            false

        try {
            if (
                before.state !=
                    HereticRealizationArtifactMaterialization.PresenceState.PRESENT
            ) {
                require(
                    before.state in
                        setOf(
                            HereticRealizationArtifactMaterialization.PresenceState.ABSENT,
                            HereticRealizationArtifactMaterialization.PresenceState.PARTIAL,
                        ),
                ) {
                    "GPT-OSS 20B artifact is damaged; explicit Redownload is a separate decision"
                }

                val accepted =
                    models.control(
                        controlRef =
                            VexLifeModelsFamily.ControlDownload,
                        handle =
                            targetModel.handle,
                    )
                controlInvoked =
                    true
                require(
                    accepted.downloading ==
                        targetModel.handle ||
                        accepted.models
                            .single {
                                it.handle ==
                                    targetModel.handle
                            }
                            .presentBytes ==
                        accepted.models
                            .single {
                                it.handle ==
                                    targetModel.handle
                            }
                            .totalBytes,
                ) {
                    "Product DOWNLOAD_MODEL Control was not admitted for GPT-OSS 20B"
                }

                val deadline =
                    System.currentTimeMillis() +
                        MaximumWaitMillis

                while (
                    true
                ) {
                    val current =
                        models.projection()
                    val row =
                        current.models.single {
                            it.handle ==
                                targetModel.handle
                        }

                    println(
                        "VEXBODY_20B_ACQUIRE_PROGRESS\t" +
                            "present=" +
                            row.presentBytes +
                            "\tdownloading=" +
                            (
                                row.downloadedBytes
                                    ?: 0L
                            ) +
                            "\ttotal=" +
                            row.totalBytes,
                    )

                    if (
                        current.downloading ==
                            null
                    ) {
                        break
                    }

                    if (
                        System.currentTimeMillis() >=
                        deadline
                    ) {
                        timedOut =
                            true
                        break
                    }

                    Thread.sleep(
                        PollMillis,
                    )
                }
            }

            val after =
                HereticRealizationArtifactMaterialization.observe(
                    productHome = productHome,
                    realization = realization,
                )
            val endingModel =
                modelStore.selectedModel(
                    home.manifest,
                )
            val projection =
                models.projection()
            val outcome =
                projection.lastOutcome

            val selectionPreserved =
                endingModel.modelRef ==
                    expectedStartingModel.modelRef
            val artifactPresent =
                after.state ==
                    HereticRealizationArtifactMaterialization.PresenceState.PRESENT &&
                    after.presentBytes ==
                    after.totalBytes
            val ProductOutcomeCompatible =
                !controlInvoked ||
                    (
                        outcome?.handle ==
                            targetModel.handle &&
                            outcome.controlRef ==
                            VexLifeModelsFamily.ControlDownload &&
                            outcome.state ==
                            VexLifeModels.OutcomeState.Downloaded.stateRef
                    )
            val pass =
                !timedOut &&
                    selectionPreserved &&
                    artifactPresent &&
                    ProductOutcomeCompatible

            val result =
                buildJsonObject {
                    put(
                        "evidenceClass",
                        ResultClass,
                    )
                    put(
                        "continuity",
                        "[VXG RealForever]",
                    )
                    put(
                        "status",
                        if (
                            pass
                        ) {
                            if (
                                controlInvoked
                            ) {
                                "ARTIFACT_ACQUIRED_QWEN4B_SELECTION_PRESERVED"
                            } else {
                                "ARTIFACT_ALREADY_PRESENT_QWEN4B_SELECTION_PRESERVED"
                            }
                        } else {
                            "HELD_ARTIFACT_ACQUISITION"
                        },
                    )
                    put(
                        "startingModelRef",
                        startingModel.modelRef,
                    )
                    put(
                        "endingModelRef",
                        endingModel.modelRef,
                    )
                    put(
                        "selectionMutationPerformed",
                        !selectionPreserved,
                    )
                    put(
                        "targetModelRef",
                        targetModel.modelRef,
                    )
                    put(
                        "targetHandle",
                        targetModel.handle,
                    )
                    put(
                        "targetRealizationRef",
                        realization.realizationRef,
                    )
                    put(
                        "targetContentSetSha256",
                        targetModel.identity.artifactContentSetSha256,
                    )
                    put(
                        "beforePresence",
                        before.state.name,
                    )
                    put(
                        "beforePresentBytes",
                        before.presentBytes,
                    )
                    put(
                        "afterPresence",
                        after.state.name,
                    )
                    put(
                        "afterPresentBytes",
                        after.presentBytes,
                    )
                    put(
                        "targetExpectedBytes",
                        after.totalBytes,
                    )
                    put(
                        "downloadControlInvoked",
                        controlInvoked,
                    )
                    put(
                        "timedOut",
                        timedOut,
                    )
                    outcome
                        ?.let {
                            put(
                                "productOutcomeState",
                                it.state,
                            )
                            put(
                                "productOutcomeControlRef",
                                it.controlRef,
                            )
                        }
                    put(
                        "artifactPresent",
                        artifactPresent,
                    )
                    put(
                        "selectionPreserved",
                        selectionPreserved,
                    )
                }

            val resultPath =
                resultDir.resolve(
                    "vexbody-20b-artifact-acquisition-result.json",
                )
            Files.writeString(
                resultPath,
                json.encodeToString(
                    kotlinx.serialization.json.JsonObject.serializer(),
                    result,
                ) +
                    "\n",
                StandardCharsets.UTF_8,
            )

            println(
                "VEXBODY_20B_ARTIFACT_RESULT=" +
                    resultPath,
            )
            println(
                "VEXBODY_20B_ARTIFACT_STATUS=" +
                    result
                        .getValue(
                            "status",
                        )
                        .toString(),
            )

            return if (
                pass
            ) {
                0
            } else {
                2
            }
        } finally {
            models.close()
        }
    }
}

fun main(
    args: Array<String>,
) {
    require(
        args.size ==
            1,
    ) {
        "usage: <result-dir>"
    }

    kotlin.system.exitProcess(
        VexBody20BArtifactAcquire.run(
            Path.of(
                args[0],
            )
                .toAbsolutePath()
                .normalize(),
        ),
    )
}

// [VXG RealForever][VEXBODY-20B-TEST][ARTIFACT-ACQUISITION]
