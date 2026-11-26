package vexlife.companion

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.capability.VexLifeCapabilityBridge
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.capability.VexLifeWorkspaceJavaTest
import vexlife.capability.VexLifeWorkspaceJavaTestDefinitions
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.sources.VexLifeLocalSourcesFamily
import vexlife.workspace.VexLifeWorkspaceRunsFamily

class VexLifeWorkspaceJavaTestTurnIntegrationTest {
    private fun productHome(): Path {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-workspace-java-test-turn-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home = home,
            deviceSuffix = "java-test-turn-device",
            lineageSuffix = "java-test-turn-lineage",
        )
        return home
    }

    private fun setup(
        home: Path,
    ) =
        VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = home.toString(),
            homeDisposition = "EXISTING_HOME_PRESERVED",
            modelSelectionDisposition = "BOUND",
            managedPythonDisposition = "REUSED_VERIFIED",
            artifactDisposition = "REUSED_VERIFIED",
            environmentDisposition = "REUSED_VERIFIED",
            runtimeDisposition = "REUSED_EXACT_OWNED_MLX_RUNTIME",
            modelBindingDisposition = "BOUND_CURRENT",
            modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.workspace-java-test",
                    runtimeState = HereticRuntimeState.Qualified,
                    modelRef = HereticProductIdentity.ModelRef,
                    modelProfileRef = HereticProductIdentity.ModelProfileRef,
                    artifactRef = HereticProductIdentity.ArtifactSetRef,
                    artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
                    runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
                    runtimeClass = HereticProductIdentity.RuntimeClass,
                    endpoint = HereticProductIdentity.Endpoint,
                    requestModel = HereticProductIdentity.RequestModel,
                    qualification = HereticRuntimeQualification.qualified(),
                ),
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
        )

    private fun reply(
        content: String,
    ) =
        DirectCompanionConnection.HttpResult(
            status = 200,
            body =
                """{"model":"default_model","choices":[{"message":{"role":"assistant","content":""" +
                    Json.encodeToString(
                        kotlinx.serialization.json.JsonPrimitive.serializer(),
                        kotlinx.serialization.json.JsonPrimitive(
                            content,
                        ),
                    ) +
                    """}}]}""",
        )

    @Test
    fun grantedWorkspaceJavaTestReturnsTypedFailureEvidenceAndContinuesTheSameTurn() {
        val home =
            productHome()
        try {
            val sourceRef =
                VexLifeLocalSourcesFamily.sourceRefForSha256(
                    "a".repeat(
                        64,
                    ),
                )
            val grant =
                VexLifeWorkspaceRunsFamily.Grant(
                    grantRef =
                        VexLifeWorkspaceRunsFamily.GrantRefPrefix +
                            "b".repeat(
                                64,
                            ),
                    sourceRef =
                        sourceRef,
                    sourceGrantedAt =
                        "2026-11-26T08:00:00Z",
                    grantedAt =
                        "2026-11-26T08:01:00Z",
                )
            val runtimeRef =
                VexLifeWorkspaceJavaTest.RuntimeRefPrefix +
                    "c".repeat(
                        64,
                    )
            var calls =
                0
            val transport =
                DirectCompanionConnection.Transport {
                    call ->
                    calls += 1
                    when (calls) {
                        1 -> {
                            val request =
                                Json.parseToJsonElement(
                                    call.body,
                                )
                                    .jsonObject
                            val available =
                                request
                                    .getValue(
                                        "messages",
                                    )
                                    .jsonArray
                                    .mapNotNull {
                                        it.jsonObject["content"]
                                            ?.jsonPrimitive
                                            ?.content
                                    }
                                    .any {
                                        it.contains(
                                            VexLifeWorkspaceJavaTestDefinitions.Handle,
                                        )
                                    }
                            assertTrue(
                                available,
                            )
                            reply(
                                VexLifeCapabilityRequest.RequestHeader +
                                    "\n" +
                                    """{"capability":"workspace-test","action":"test","arguments":{"source":"$sourceRef","sources":["src/Calculator.java","test/CalculatorTest.java"],"testClass":"CalculatorTest"}}""",
                            )
                        }

                        2 -> {
                            val providerRequest =
                                Json.parseToJsonElement(
                                    call.body,
                                )
                                    .jsonObject
                            val providerResult =
                                providerRequest
                                    .getValue(
                                        "messages",
                                    )
                                    .jsonArray
                                    .last()
                                    .jsonObject
                            assertEquals(
                                "tool",
                                providerResult
                                    .getValue(
                                        "role",
                                    )
                                    .jsonPrimitive
                                    .content,
                            )
                            val returned =
                                Json.parseToJsonElement(
                                    providerResult
                                        .getValue(
                                            "content",
                                        )
                                        .jsonPrimitive
                                        .content,
                                )
                                    .jsonObject
                            assertEquals(
                                "UNTRUSTED_EXTERNAL",
                                returned
                                    .getValue(
                                        "contentTrust",
                                    )
                                    .jsonPrimitive
                                    .content,
                                "project-controlled compiler/test output must stay explicitly untrusted",
                            )
                            val metadata =
                                returned
                                    .getValue(
                                        "metadata",
                                    )
                                    .jsonObject
                            assertEquals(
                                VexLifeWorkspaceJavaTest.DispositionTestFailed,
                                metadata
                                    .getValue(
                                        "disposition",
                                    )
                                    .jsonPrimitive
                                    .content,
                            )
                            assertTrue(
                                metadata
                                    .getValue(
                                        "testStderr",
                                    )
                                    .jsonPrimitive
                                    .content
                                    .contains(
                                        "expected 42 but got 41",
                                    ),
                            )
                            assertFalse(
                                call.body.contains(
                                    "PRIVATE-WORKSPACE-BODY",
                                ),
                            )
                            reply(
                                "The real project test failed; I will repair the implementation before running it again.",
                            )
                        }

                        else ->
                            error(
                                "unexpected provider stage",
                            )
                    }
                }

            val result =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.workspace-java-test.same-turn.001",
                            content =
                                "Run the project test and use the real failure to decide what to fix.",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport =
                                transport,
                            currentWorkspaceRunGrants = {
                                listOf(
                                    grant,
                                )
                            },
                            currentWorkspaceJavaTestRuntimeRef = {
                                runtimeRef
                            },
                            capabilityOwners =
                                VexLifeCapabilityBridge.OwnerBindings(
                                    testWorkspace = {
                                        _,
                                        operationRef,
                                        observedSourceRef,
                                        request,
                                        ->
                                        assertEquals(
                                            sourceRef,
                                            observedSourceRef,
                                        )
                                        assertEquals(
                                            listOf(
                                                "src/Calculator.java",
                                                "test/CalculatorTest.java",
                                            ),
                                            request.sources,
                                        )
                                        assertEquals(
                                            "CalculatorTest",
                                            request.testClass,
                                        )
                                        VexLifeWorkspaceJavaTest.Outcome.Completed(
                                            VexLifeWorkspaceJavaTest.Receipt(
                                                schemaVersion =
                                                    VexLifeWorkspaceJavaTest.ReceiptSchema,
                                                processRef =
                                                    VexLifeWorkspaceJavaTestDefinitions.ProcessRef,
                                                commandRef =
                                                    VexLifeWorkspaceJavaTestDefinitions.CommandRef,
                                                operationRef =
                                                    operationRef,
                                                workingScopeRef =
                                                    sourceRef,
                                                resourceLeaseRef =
                                                    VexLifeWorkspaceJavaTest.ResourceLeaseRefPrefix +
                                                        "d".repeat(
                                                            64,
                                                        ),
                                                runGrantRef =
                                                    grant.grantRef,
                                                runtimeRef =
                                                    runtimeRef,
                                                sandboxSha256 =
                                                    "e".repeat(
                                                        64,
                                                    ),
                                                sources =
                                                    request.sources.map {
                                                        path ->
                                                        VexLifeWorkspaceJavaTest.SourceObservation(
                                                            path = path,
                                                            sha256 = "f".repeat(64),
                                                            bytes = 12,
                                                        )
                                                    },
                                                testClass =
                                                    request.testClass,
                                                disposition =
                                                    VexLifeWorkspaceJavaTest.DispositionTestFailed,
                                                diagnostics =
                                                    emptyList(),
                                                testExitCode =
                                                    1,
                                                testTimedOut =
                                                    false,
                                                testStdout =
                                                    "",
                                                testStderr =
                                                    "expected 42 but got 41",
                                                stdoutTruncated =
                                                    false,
                                                stderrTruncated =
                                                    false,
                                                scratchResidualRecovered =
                                                    false,
                                                cleanupComplete =
                                                    true,
                                                externalEffectsExecuted =
                                                    true,
                                                effectAuthorityGranted =
                                                    true,
                                                currentEffectAuthority =
                                                    true,
                                            ),
                                        )
                                    },
                                ),
                        ),
                )

            assertEquals(
                2,
                calls,
            )
            assertEquals(
                "The real project test failed; I will repair the implementation before running it again.",
                result.content,
            )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.RETURNED,
                result.capabilityUses.single().state,
            )
            assertEquals(
                VexLifeWorkspaceJavaTestDefinitions.Handle,
                result.capabilityUses.single().handle,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
