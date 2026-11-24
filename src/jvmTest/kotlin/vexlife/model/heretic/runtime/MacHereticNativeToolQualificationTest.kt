package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification

class MacHereticNativeToolQualificationTest {
    private class Runner(
        private val qualificationStdout: String,
    ) :
        HereticPythonEnvironment.Runner {
        val calls =
            mutableListOf<List<String>>()

        override fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): HereticPythonEnvironment.CommandResult {
            calls += args
            return HereticPythonEnvironment.CommandResult(
                status = 0,
                stdout = qualificationStdout,
                stderr = "",
            )
        }
    }

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
        output: String,
    ): Pair<HereticRuntimeQualification, List<String>> {
        val root =
            Files.createTempDirectory(
                "vexlife-native-tool-qualification-",
            )
                .toAbsolutePath()
        val runner =
            Runner(
                qualificationStdout = output,
            )
        val platform =
            MacHereticRuntimePlatform(
                productHome = root,
                commandRunner = runner,
            )

        return try {
            platform.qualify(
                binding(
                    root,
                ),
            ) to
                runner.calls.single()
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
                "QUALIFIED\n",
            )
        assertEquals(
            null,
            held.first.providerNativeToolsQualificationRef,
            "ordinary runtime qualification survives a held optional adapter",
        )
        assertEquals(
            "true",
            held.second.last(),
        )

        val qualified =
            qualify(
                "QUALIFIED_NATIVE_TOOLS\n",
            )
        assertEquals(
            HereticRuntimeQualification.ProviderNativeToolsQualificationRef,
            qualified.first.providerNativeToolsQualificationRef,
        )
        assertEquals(
            "true",
            qualified.second.last(),
        )

        val script =
            qualified.second[
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
