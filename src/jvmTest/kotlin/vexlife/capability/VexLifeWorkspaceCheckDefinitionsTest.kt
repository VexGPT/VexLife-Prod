package vexlife.capability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeWorkspaceCheckDefinitionsTest {
    @Test
    fun firstTypedCommandIsRegisteredWithoutShellOrExecutableChoice() {
        val definition =
            VexLifeWorkspaceCheckDefinitions.requireCommand(
                VexLifeWorkspaceCheckDefinitions.CommandRef,
            )

        assertEquals(
            "process.vexlife.cli.execute-typed",
            definition.processRef,
        )
        assertEquals(
            "EXACT_CURRENT_LOCAL_SOURCE_GENERATION",
            definition.workingScopeRule,
        )
        assertEquals(
            VexLifeWorkspaceCheckDefinitions.RuntimeResourceRef,
            definition.executableRealizationRef,
        )
        assertTrue(
            definition.environmentKeys.isEmpty(),
        )
        assertFalse(
            definition.arbitraryExecutableAllowed,
        )
        assertFalse(
            definition.shellTextAllowed,
        )
        assertEquals(
            listOf(
                "path",
                "required",
                "forbidden",
            ),
            definition.parameters
                .map {
                    it.name
                },
        )
        assertTrue(
            definition.parameters.none {
                it.name in
                    setOf(
                        "program",
                        "executable",
                        "shell",
                        "command",
                        "workingDirectory",
                    )
            },
        )
    }
}

// [VXG RealForever]
