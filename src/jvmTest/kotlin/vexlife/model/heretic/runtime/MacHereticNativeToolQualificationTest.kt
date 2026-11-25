package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification

class MacHereticNativeToolQualificationTest {
    private class Runner(
        private val results: List<HereticPythonEnvironment.CommandResult>,
    ) :
        HereticPythonEnvironment.Runner {
        val calls =
            mutableListOf<List<String>>()
        val timeouts =
            mutableListOf<Long>()
        private var next =
            0

        override fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): HereticPythonEnvironment.CommandResult {
            calls += args
            timeouts += timeoutMs
            return results
                .getOrNull(
                    next++,
                )
                ?: error(
                    "unexpected qualification command",
                )
        }
    }

    private data class ProbeRun(
        val qualification: HereticRuntimeQualification,
        val calls: List<List<String>>,
        val timeouts: List<Long>,
    )

    private fun recognizedModel(): HereticModelCatalog.Entry =
        HereticModelCatalog.Models.Qwen27B.copy(
            companionProfile =
                HereticCompanionRepresentation.Profiles.importedQwen(
                    profileRef =
                        "companion-profile.vexlife-prod.imported.native-tool-qualification-test.v1",
                    nativeToolCalling = true,
                ),
            recommendedMemoryBytes = null,
            origin = HereticModelCatalog.Origin.HOME_IMPORTED,
            autoRecommended = false,
        )

    private fun binding(
        root: java.nio.file.Path,
    ): HereticPersistentRuntime.Binding {
        val model =
            recognizedModel()
        val environment =
            HereticPythonEnvironment.environmentRoot(
                root,
            )
        val python =
            HereticPythonEnvironment.pythonPath(
                environment,
            )
        Files.createDirectories(
            python.parent,
        )
        Files.writeString(
            python,
            "fake",
        )
        Files.createDirectories(
            root.resolve(
                "model",
            ),
        )
        Files.writeString(
            environment.resolve(
                HereticPythonEnvironment.InstallReportFilename,
            ),
            "{}",
        )

        return HereticPersistentRuntime.bind(
            authorityRef =
                "authority.vexlife-prod.native-tool-qualification-test",
            materialization =
                HereticArtifactMaterialization.Result(
                    modelPath =
                        root.resolve(
                            "model",
                        )
                            .toString(),
                    providerRevision = model.providerRevision,
                    contentSetSha256 =
                        model.identity.artifactContentSetSha256,
                    memberCount = model.members.size,
                    totalBytes = model.totalBytes,
                    reusedMemberCount = model.members.size,
                    acquiredMemberCount = 0,
                    acquisitionPerformed = false,
                    artifactMutationPerformed = false,
                ),
            environment =
                HereticPythonEnvironment.Result(
                    state = "REUSED_VERIFIED",
                    pythonEnvironmentRoot =
                        environment.toString(),
                    pythonPath =
                        python.toString(),
                    pythonVersion =
                        HereticProductIdentity.PythonVersion,
                    mlxLmVersion =
                        HereticProductIdentity.MlxLmVersion,
                    pipReportPath =
                        environment.resolve(
                            HereticPythonEnvironment.InstallReportFilename,
                        )
                            .toString(),
                    environmentMutationPerformed = false,
                ),
            model = model,
        )
    }

    private fun qualify(
        nativeProbeResult: HereticPythonEnvironment.CommandResult,
    ): ProbeRun {
        val root =
            Files.createTempDirectory(
                "vexlife-native-tool-qualification-",
            )
                .toAbsolutePath()
        val runner =
            Runner(
                results =
                    listOf(
                        HereticPythonEnvironment.CommandResult(
                            status = 0,
                            stdout = "QUALIFIED\n",
                            stderr = "",
                        ),
                        nativeProbeResult,
                    ),
            )
        val platform =
            MacHereticRuntimePlatform(
                productHome = root,
                commandRunner = runner,
            )

        return try {
            ProbeRun(
                qualification =
                    platform.qualify(
                        binding(
                            root,
                        ),
                    ),
                calls = runner.calls.toList(),
                timeouts = runner.timeouts.toList(),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun recognizedNativeToolProbeIsOptionalAndFailSoft() {
        val held =
            qualify(
                HereticPythonEnvironment.CommandResult(
                    status = null,
                    stdout = "",
                    stderr = "",
                    timedOut = true,
                ),
            )
        assertEquals(
            null,
            held.qualification.providerNativeToolsQualificationRef,
            "optional native-tool process timeout must not revoke ordinary runtime qualification",
        )
        assertEquals(
            listOf(
                180_000L,
                300_000L,
            ),
            held.timeouts,
        )
        assertEquals(
            2,
            held.calls.size,
        )
        assertFalse(
            held.calls[0][
                1
            ]
                .contains(
                    "vexlife_qualification_echo",
                ),
            "ordinary runtime qualification must complete independently of optional adapter probing",
        )

        val qualified =
            qualify(
                HereticPythonEnvironment.CommandResult(
                    status = 0,
                    stdout = "QUALIFIED_NATIVE_TOOLS\n",
                    stderr = "",
                ),
            )
        assertEquals(
            HereticRuntimeQualification.ProviderNativeToolsQualificationRef,
            qualified.qualification.providerNativeToolsQualificationRef,
        )
        assertEquals(
            2,
            qualified.calls.size,
        )

        val script =
            qualified.calls[
                1
            ][
                1
            ]
        for (
            required in
            listOf(
                "vexlife_qualification_echo",
                "\"tool_calls\"",
                "\"tool_call_id\"",
                "vexlife-native-tool-roundtrip",
                "NATIVE_TOOL_HELD:",
                "QUALIFIED_NATIVE_TOOLS",
            )
        ) {
            assertTrue(
                script.contains(
                    required,
                ),
                required,
            )
        }
    }
}

// [VXG RealForever]
