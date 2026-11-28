package vexlife.harness

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.HereticArtifactMaterialization

/**
 * [vexbody 20b test] host-side experiment wrapper.
 *
 * The Product model-selection/runtime owners stay authoritative. This wrapper adds no model owner:
 * it verifies the current Home starts on exact Qwen 4B, refuses to download GPT-OSS implicitly,
 * temporarily selects the already-present GPT-OSS 20B candidate, runs the unchanged VexBody
 * scenario through [FamilyBBuilderCanary], releases that test runtime Session, then restores and
 * re-qualifies exact Qwen 4B before returning.
 */
object VexBody20BTest {
    private const val ResultClass = "VEXBODY_20B_TEST_RESULT"

    private val json =
        Json {
            prettyPrint = true
        }

    fun run(
        imageRoot: Path,
        sourceSha: String,
        resultDir: Path,
        effectiveInputTokenBudget: Int,
    ): Int {
        Files.createDirectories(
            resultDir,
        )

        val runtimeHome =
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
        val runtimeHomeState =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    runtimeHome,
                ),
            ) {
                "VexBody 20B test requires the current canonical Vex Home"
            }
        val modelStore =
            FileHereticModelConfigurationStore(
                runtimeHome,
            )
        val startingModel =
            modelStore.selectedModel(
                runtimeHomeState.manifest,
            )
        val expectedStartingModel =
            HereticModelCatalog.Models.Qwen4B
        val targetModel =
            HereticModelCatalog.Models.GptOss20B

        require(
            startingModel.modelRef ==
                expectedStartingModel.modelRef
        ) {
            "VexBody 20B isolation test requires exact Qwen 4B as the starting Home selection"
        }

        val targetPresence =
            HereticArtifactMaterialization.observePresence(
                productHome = runtimeHome,
                model = targetModel,
            )
        if (
            targetPresence.state !=
                HereticArtifactMaterialization.PresenceState.CompleteBySizeNotHashed
        ) {
            writeResult(
                resultDir = resultDir,
                value =
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
                            "HELD_TARGET_ARTIFACT_NOT_PRESENT",
                        )
                        put(
                            "productSourceSha",
                            sourceSha,
                        )
                        put(
                            "startingModelRef",
                            startingModel.modelRef,
                        )
                        put(
                            "targetModelRef",
                            targetModel.modelRef,
                        )
                        put(
                            "targetPresence",
                            targetPresence.state.stateRef,
                        )
                        put(
                            "targetPresentBytes",
                            targetPresence.presentBySizeBytes,
                        )
                        put(
                            "targetExpectedBytes",
                            targetPresence.totalBytes,
                        )
                        put(
                            "modelSelectionMutationPerformed",
                            false,
                        )
                        put(
                            "downloadPerformed",
                            false,
                        )
                        put(
                            "restorationRequired",
                            false,
                        )
                    },
            )
            return 2
        }

        var innerExitCode: Int? =
            null
        var innerFailure: String? =
            null
        var targetSelectionMutationPerformed =
            false
        var restorationSucceeded =
            false
        var restorationFailure: String? =
            null
        var restoredRuntimeReceiptRef: String? =
            null

        try {
            targetSelectionMutationPerformed =
                modelStore
                    .selectModelFromExplicitHumanChoice(
                        manifest = runtimeHomeState.manifest,
                        model = targetModel,
                    )
                    .writePerformed

            innerExitCode =
                FamilyBBuilderCanary.run(
                    FamilyBBuilderCanary.Config(
                        imageRoot = imageRoot,
                        sourceSha = sourceSha,
                        resultDir = resultDir,
                        effectiveInputTokenBudget = effectiveInputTokenBudget,
                        expectedModelHandle = targetModel.handle,
                        releaseRuntimeSessionOnExit = true,
                    ),
                )
        } catch (
            error: Throwable,
        ) {
            innerFailure =
                error.javaClass.simpleName +
                    ": " +
                    (
                        error.message
                            ?: ""
                    )
        } finally {
            try {
                modelStore.selectModelFromExplicitHumanChoice(
                    manifest = runtimeHomeState.manifest,
                    model = expectedStartingModel,
                )
                val restoredSession =
                    VexLifeProdLocalSetup.establishSession(
                        productHome = runtimeHome,
                        allowAcquisition = false,
                        allowRuntimeStart = true,
                    )
                val restoredEvidence =
                    restoredSession.observeCurrentQualifiedRuntimeEvidence()
                require(
                    restoredEvidence.modelRef ==
                        expectedStartingModel.modelRef
                ) {
                    "VexBody 20B test restoration did not return to exact Qwen 4B"
                }
                require(
                    HereticCompanionRepresentation.vexReachActive(
                        profile = expectedStartingModel.companionProfile,
                        qualification = restoredEvidence.qualification,
                    ),
                ) {
                    "VexBody 20B test restored Qwen 4B without its active Vex Reach relationship"
                }
                restoredRuntimeReceiptRef =
                    restoredEvidence.runtimeReceiptRef
                restorationSucceeded =
                    true

                // Deliberately do not close this Session: the test began with Qwen 4B as the
                // sustained selected runtime and must leave that same relationship serving.
                @Suppress("UNUSED_VARIABLE")
                val keepRestoredRuntimeServing =
                    restoredSession
            } catch (
                error: Throwable,
            ) {
                restorationFailure =
                    error.javaClass.simpleName +
                        ": " +
                        (
                            error.message
                                ?: ""
                        )
            }

            writeResult(
                resultDir = resultDir,
                value =
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
                            when {
                                !restorationSucceeded ->
                                    "RESTORATION_FAILED"

                                innerFailure !=
                                    null ->
                                    "TEST_FAILED_QWEN4B_RESTORED"

                                innerExitCode ==
                                    0 ->
                                    "BODY_OBSERVER_COMPLETED_QWEN4B_RESTORED"

                                else ->
                                    "TEST_NONZERO_QWEN4B_RESTORED"
                            },
                        )
                        put(
                            "productSourceSha",
                            sourceSha,
                        )
                        put(
                            "startingModelRef",
                            startingModel.modelRef,
                        )
                        put(
                            "targetModelRef",
                            targetModel.modelRef,
                        )
                        put(
                            "targetCompanionProfileRef",
                            targetModel.companionProfile.profileRef,
                        )
                        put(
                            "targetPresence",
                            targetPresence.state.stateRef,
                        )
                        put(
                            "targetSelectionMutationPerformed",
                            targetSelectionMutationPerformed,
                        )
                        put(
                            "downloadPerformed",
                            false,
                        )
                        put(
                            "innerResultPath",
                            resultDir
                                .resolve(
                                    "family-b-result.json",
                                )
                                .toString(),
                        )
                        innerExitCode
                            ?.let {
                                put(
                                    "innerExitCode",
                                    it,
                                )
                            }
                        put(
                            "innerFailure",
                            innerFailure
                                ?.let(
                                    ::JsonPrimitive,
                                )
                                ?: JsonNull,
                        )
                        put(
                            "qwen4bRestored",
                            restorationSucceeded,
                        )
                        put(
                            "restoredRuntimeReceiptRef",
                            restoredRuntimeReceiptRef
                                ?.let(
                                    ::JsonPrimitive,
                                )
                                ?: JsonNull,
                        )
                        put(
                            "restorationFailure",
                            restorationFailure
                                ?.let(
                                    ::JsonPrimitive,
                                )
                                ?: JsonNull,
                        )
                    },
            )
        }

        return when {
            !restorationSucceeded ->
                3

            innerFailure !=
                null ->
                1

            innerExitCode ==
                0 ->
                0

            else ->
                1
        }
    }

    private fun writeResult(
        resultDir: Path,
        value: kotlinx.serialization.json.JsonObject,
    ) {
        val path =
            resultDir.resolve(
                "vexbody-20b-test-result.json",
            )
        Files.writeString(
            path,
            json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                value,
            ) +
                "\n",
            StandardCharsets.UTF_8,
        )
        println(
            "VEXBODY_20B_TEST_RESULT=" +
                path,
        )
        println(
            "VEXBODY_20B_TEST_STATUS=" +
                value
                    .getValue(
                        "status",
                    )
                    .let {
                        (it as JsonPrimitive).content
                    },
        )
    }
}

fun main(
    args: Array<String>,
) {
    require(
        args.size in
            3..4,
    ) {
        "usage: <private-service-image-root> <source-sha> <result-dir> [effective-input-token-budget]"
    }
    val code =
        VexBody20BTest.run(
            imageRoot =
                Path.of(
                    args[0],
                )
                    .toAbsolutePath()
                    .normalize(),
            sourceSha =
                args[1],
            resultDir =
                Path.of(
                    args[2],
                )
                    .toAbsolutePath()
                    .normalize(),
            effectiveInputTokenBudget =
                args
                    .getOrNull(
                        3,
                    )
                    ?.toInt()
                    ?: 52_000,
        )
    kotlin.system.exitProcess(
        code,
    )
}

// [VXG RealForever][VEXBODY-20B-TEST]
