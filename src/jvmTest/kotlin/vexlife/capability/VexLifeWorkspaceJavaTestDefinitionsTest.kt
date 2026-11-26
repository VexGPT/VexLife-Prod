package vexlife.capability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeWorkspaceJavaTestDefinitionsTest {
    @Test
    fun typedProjectTestCommandOwnsExecutableAndSandboxSelection() {
        val definition =
            VexLifeWorkspaceJavaTestDefinitions.JavaTestCommand

        assertEquals(
            "process.vexlife.cli.execute-sandboxed-typed",
            definition.processRef,
        )
        assertEquals(
            "EXACT_CURRENT_LOCAL_SOURCE_GENERATION",
            definition.workingScopeRule,
        )
        assertEquals(
            VexLifeWorkspaceJavaTestDefinitions.RuntimeResourceRef,
            definition.executableRealizationRef,
        )
        assertEquals(
            VexLifeWorkspaceJavaTestDefinitions.EffectClass,
            definition.effectClass,
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
        assertFalse(
            definition.packageManagerAllowed,
        )
        assertTrue(
            definition.projectCodeExecutionAllowed,
        )
        assertEquals(
            listOf(
                "sources",
                "testClass",
            ),
            definition.parameters.map {
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
                        "compilerFlags",
                        "classpath",
                        "workingDirectory",
                        "environment",
                        "sandbox",
                        "timeout",
                    )
            },
        )
    }

    @Test
    fun atlasDefinitionExposesOnlyTypedJavaTestIntent() {
        val definition =
            VexLifeWorkspaceJavaTestDefinitions.Definitions.single()
        val action =
            definition.actions.single()

        assertEquals(
            VexLifeWorkspaceJavaTestDefinitions.CapabilityRef,
            definition.capabilityRef,
        )
        assertEquals(
            VexLifeWorkspaceJavaTestDefinitions.PermissionRef,
            definition.permissionRef,
        )
        assertEquals(
            VexLifeWorkspaceJavaTestDefinitions.ResourceRef,
            definition.resourceRef,
        )
        assertEquals(
            VexLifeCapabilityAtlas.Stage.EXECUTABLE,
            definition.defaultStage,
        )
        assertEquals(
            "test",
            action.action,
        )
        assertEquals(
            listOf(
                "source",
                "sources",
                "testClass",
            ),
            action.arguments.map {
                it.name
            },
        )
        assertEquals(
            "string[]",
            action.arguments.single {
                it.name ==
                    "sources"
            }
                .type,
        )
        assertTrue(
            action.arguments.none {
                it.name in
                    setOf(
                        "program",
                        "executable",
                        "shell",
                        "command",
                        "compilerFlags",
                        "classpath",
                        "workingDirectory",
                        "environment",
                        "sandbox",
                        "timeout",
                    )
            },
        )
        assertTrue(
            VexLifeCapabilityAtlas.definitions().any {
                it.capabilityRef ==
                    VexLifeWorkspaceJavaTestDefinitions.CapabilityRef
            },
        )
    }

    @Test
    fun runAuthorityIsNotCheckOrWriteAuthority() {
        assertTrue(
            VexLifeWorkspaceJavaTestDefinitions.PermissionRef !=
                VexLifeWorkspaceCheckDefinitions.PermissionRef,
        )
        assertTrue(
            VexLifeWorkspaceJavaTestDefinitions.PermissionRef !=
                VexLifeWorkspaceEffectDefinitions.PermissionRef,
        )
        assertTrue(
            VexLifeWorkspaceJavaTestDefinitions.CapabilityRef !=
                VexLifeWorkspaceCheckDefinitions.CapabilityRef,
        )
        assertTrue(
            VexLifeWorkspaceJavaTestDefinitions.CapabilityRef !=
                VexLifeWorkspaceEffectDefinitions.WriteRef,
        )
    }
}

// [VXG RealForever]
