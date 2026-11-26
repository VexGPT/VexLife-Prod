package vexlife.capability

/**
 * Source-managed definition for the first project-code execution effect admitted by C3.
 *
 * The model selects only bounded Java source paths and one test main class. Product source owns the
 * Java runtime, compiler realization, sandbox profile, classpath, environment, timeout and output
 * limits. Registration describes the request; separate current Run authority owns execution.
 */
object VexLifeWorkspaceJavaTestDefinitions {
    const val CapabilityRef =
        "capability.vexlife.workspace.test.java"

    const val Handle =
        "workspace-test"

    const val PermissionRef =
        "permission.vexlife-prod.workspace.run"

    const val ResourceRef =
        "resource.vexlife-prod.workspace.current-run-grant"

    const val RuntimeResourceRef =
        "resource.vexlife-prod.private-service-image.sandboxed-java-test"

    const val CommandRef =
        "command.vexlife-prod.workspace.test.java/v1"

    const val ProcessRef =
        "process.vexlife.cli.execute-sandboxed-typed"

    const val ProcessMainClass =
        "vexlife.capability.VexLifeWorkspaceJavaTestProcessKt"

    const val EffectClass =
        "SANDBOXED_WORKSPACE_TEST"

    const val ResourceClass =
        "PRODUCT_JAVA_COMPILER_SANDBOX_AND_PERSON_GRANTED_WORKSPACE"

    const val MaximumSources =
        24

    const val MaximumPathCharacters =
        512

    const val MaximumTestClassCharacters =
        256

    const val MaximumSourceBytes =
        1_048_576

    const val MaximumDiagnostics =
        64

    const val MaximumDiagnosticCharacters =
        2_048

    const val MaximumStdoutBytes =
        65_536

    const val MaximumStderrBytes =
        32_768

    const val TimeoutMs =
        45_000L

    const val ChildTimeoutMs =
        20_000L

    const val ScratchDirectoryName =
        ".vexlife-run"

    enum class ParameterType {
        STRING,
        STRING_ARRAY,
    }

    data class Parameter(
        val name: String,
        val type: ParameterType,
        val required: Boolean,
        val maximumItems: Int?,
        val maximumCharacters: Int,
    )

    data class CommandDefinition(
        val commandRef: String,
        val processRef: String,
        val purpose: String,
        val effectClass: String,
        val workingScopeRule: String,
        val executableRealizationRef: String,
        val parameters: List<Parameter>,
        val timeoutMs: Long,
        val maximumStdoutBytes: Int,
        val maximumStderrBytes: Int,
        val environmentKeys: List<String>,
        val arbitraryExecutableAllowed: Boolean,
        val shellTextAllowed: Boolean,
        val packageManagerAllowed: Boolean,
        val projectCodeExecutionAllowed: Boolean,
    ) {
        init {
            require(
                commandRef == CommandRef &&
                    processRef == ProcessRef &&
                    effectClass == EffectClass &&
                    workingScopeRule == "EXACT_CURRENT_LOCAL_SOURCE_GENERATION" &&
                    executableRealizationRef == RuntimeResourceRef &&
                    timeoutMs == TimeoutMs &&
                    maximumStdoutBytes == MaximumStdoutBytes &&
                    maximumStderrBytes == MaximumStderrBytes &&
                    environmentKeys.isEmpty() &&
                    !arbitraryExecutableAllowed &&
                    !shellTextAllowed &&
                    !packageManagerAllowed &&
                    projectCodeExecutionAllowed,
            ) {
                "workspace Java-test command definition is invalid"
            }
        }
    }

    val JavaTestCommand =
        CommandDefinition(
            commandRef = CommandRef,
            processRef = ProcessRef,
            purpose =
                "Compile exact captured Java sources and execute one project-authored test main under Product-owned sandbox confinement.",
            effectClass = EffectClass,
            workingScopeRule = "EXACT_CURRENT_LOCAL_SOURCE_GENERATION",
            executableRealizationRef = RuntimeResourceRef,
            parameters =
                listOf(
                    Parameter(
                        name = "sources",
                        type = ParameterType.STRING_ARRAY,
                        required = true,
                        maximumItems = MaximumSources,
                        maximumCharacters = MaximumPathCharacters,
                    ),
                    Parameter(
                        name = "testClass",
                        type = ParameterType.STRING,
                        required = true,
                        maximumItems = null,
                        maximumCharacters = MaximumTestClassCharacters,
                    ),
                ),
            timeoutMs = TimeoutMs,
            maximumStdoutBytes = MaximumStdoutBytes,
            maximumStderrBytes = MaximumStderrBytes,
            environmentKeys = emptyList(),
            arbitraryExecutableAllowed = false,
            shellTextAllowed = false,
            packageManagerAllowed = false,
            projectCodeExecutionAllowed = true,
        )

    val Definitions =
        listOf(
            VexLifeCapabilityAtlas.Definition(
                capabilityRef = CapabilityRef,
                handle = Handle,
                purpose =
                    "Compile bounded Java project sources and run one test main under separately granted, Product-owned sandbox confinement.",
                defaultStage = VexLifeCapabilityAtlas.Stage.EXECUTABLE,
                roleRefs = listOf(VexLifeCapabilityAtlas.CompanionRoleRef),
                platformRefs = listOf(VexLifeCapabilityAtlas.MacPlatformRef),
                permissionRef = PermissionRef,
                resourceRef = ResourceRef,
                effectClass = EffectClass,
                resourceClass = ResourceClass,
                modelParticipationClass = "REQUEST_RETURN_EFFECT",
                sourcePath =
                    "src/jvmMain/kotlin/vexlife/capability/VexLifeWorkspaceJavaTest.kt",
                priority = 48,
                actions =
                    listOf(
                        VexLifeCapabilityAtlas.Action(
                            action = "test",
                            purpose =
                                "Compile the listed Java source/test files and run one test main class. The test process receives no shell, package manager, ambient environment, network authority or source-write authority.",
                            arguments =
                                listOf(
                                    VexLifeCapabilityAtlas.Argument(
                                        name = "source",
                                        schema = VexLifeCapabilityAtlas.ValueSchema("string"),
                                        required = true,
                                        meaning =
                                            "Exact Local Source ref whose current generation has a separate project-run grant.",
                                    ),
                                    VexLifeCapabilityAtlas.Argument(
                                        name = "sources",
                                        schema =
                                            VexLifeCapabilityAtlas.ValueSchema(
                                                type = "array",
                                                items = VexLifeCapabilityAtlas.ValueSchema("string"),
                                            ),
                                        required = true,
                                        meaning =
                                            "Bounded distinct relative .java paths captured from the granted workspace.",
                                    ),
                                    VexLifeCapabilityAtlas.Argument(
                                        name = "testClass",
                                        schema = VexLifeCapabilityAtlas.ValueSchema("string"),
                                        required = true,
                                        meaning =
                                            "Fully-qualified compiled test main class. Product owns the executable, classpath, sandbox and timeout.",
                                    ),
                                ),
                            exampleArguments =
                                """{"source":"source.vexlife-prod.folder.sha256.<sha256>","sources":["src/Calculator.java","test/CalculatorTest.java"],"testClass":"CalculatorTest"}""",
                            returns =
                                "Typed compile/test receipt with source digests, bounded compiler diagnostics and bounded test output. Workspace source bodies are never returned by the runner.",
                        ),
                    ),
            ),
        )

    fun requireCommand(commandRef: String) {
        require(commandRef == CommandRef) {
            "workspace Java-test command is not registered: $commandRef"
        }
    }
}

// [VXG RealForever]
