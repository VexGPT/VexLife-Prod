package vexlife.capability

/**
 * Source-managed definition of the first C3-C typed command.
 *
 * This definition is deliberately not yet registered into the ordinary Companion Capability Frame.
 * C3-C1 proves the process/check owner first; C3-C2 owns frame/Bridge/UI admission.
 */
object VexLifeWorkspaceCheckDefinitions {
    const val CapabilityRef =
        "capability.vexlife.workspace.check"

    const val Handle =
        "workspace-check"

    const val PermissionRef =
        "permission.vexlife-prod.workspace.check"

    const val ResourceRef =
        "resource.vexlife-prod.workspace.current-check-grant"

    const val RuntimeResourceRef =
        "resource.vexlife-prod.private-service-image.product-java"

    const val CommandRef =
        "command.vexlife-prod.workspace.check.text/v1"

    const val ProcessRef =
        "process.vexlife.cli.execute-typed"

    const val ProcessMainClass =
        "vexlife.capability.VexLifeWorkspaceTextCheckProcessKt"

    const val EffectClass =
        "BOUNDED_WORKSPACE_CHECK"

    const val ResourceClass =
        "PRODUCT_JAVA_AND_PERSON_GRANTED_WORKSPACE"

    const val MaximumPathCharacters =
        512

    const val MaximumAssertions =
        16

    const val MaximumLiteralCharacters =
        512

    const val MaximumInputBytes =
        1_048_576

    const val MaximumStdoutBytes =
        65_536

    const val MaximumStderrBytes =
        16_384

    const val TimeoutMs =
        30_000L

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
    ) {
        init {
            require(
                commandRef ==
                    CommandRef &&
                    processRef ==
                    ProcessRef &&
                    effectClass ==
                    EffectClass &&
                    workingScopeRule ==
                    "EXACT_CURRENT_LOCAL_SOURCE_GENERATION" &&
                    executableRealizationRef ==
                    RuntimeResourceRef &&
                    timeoutMs in
                    1..TimeoutMs &&
                    maximumStdoutBytes in
                    1..MaximumStdoutBytes &&
                    maximumStderrBytes in
                    1..MaximumStderrBytes &&
                    environmentKeys ==
                    environmentKeys
                        .distinct()
                        .sorted() &&
                    !arbitraryExecutableAllowed &&
                    !shellTextAllowed,
            ) {
                "workspace text-check command definition is invalid"
            }
        }
    }

    val TextCommand =
        CommandDefinition(
            commandRef =
                CommandRef,
            processRef =
                ProcessRef,
            purpose =
                "Run one Product-owned bounded UTF-8 text assertion check inside one exact current Local Source generation.",
            effectClass =
                EffectClass,
            workingScopeRule =
                "EXACT_CURRENT_LOCAL_SOURCE_GENERATION",
            executableRealizationRef =
                RuntimeResourceRef,
            parameters =
                listOf(
                    Parameter(
                        name =
                            "path",
                        type =
                            ParameterType.STRING,
                        required =
                            true,
                        maximumItems =
                            null,
                        maximumCharacters =
                            MaximumPathCharacters,
                    ),
                    Parameter(
                        name =
                            "required",
                        type =
                            ParameterType.STRING_ARRAY,
                        required =
                            false,
                        maximumItems =
                            MaximumAssertions,
                        maximumCharacters =
                            MaximumLiteralCharacters,
                    ),
                    Parameter(
                        name =
                            "forbidden",
                        type =
                            ParameterType.STRING_ARRAY,
                        required =
                            false,
                        maximumItems =
                            MaximumAssertions,
                        maximumCharacters =
                            MaximumLiteralCharacters,
                    ),
                ),
            timeoutMs =
                TimeoutMs,
            maximumStdoutBytes =
                MaximumStdoutBytes,
            maximumStderrBytes =
                MaximumStderrBytes,
            environmentKeys =
                emptyList(),
            arbitraryExecutableAllowed =
                false,
            shellTextAllowed =
                false,
        )

    fun requireCommand(
        commandRef: String,
    ): CommandDefinition {
        require(
            commandRef ==
                CommandRef,
        ) {
            "workspace check command is not registered"
        }
        return TextCommand
    }
}

// [VXG RealForever]
