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
import vexlife.capability.VexLifeWorkspaceCheckDefinitions
import vexlife.capability.VexLifeWorkspaceTextCheck
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.sources.VexLifeLocalSourcesFamily
import vexlife.workspace.VexLifeWorkspaceChecksFamily

class VexLifeWorkspaceCheckTurnIntegrationTest {
    private fun productHome(): Path {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-workspace-check-turn-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home = home,
            deviceSuffix = "check-turn-device",
            lineageSuffix = "check-turn-lineage",
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
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.workspace-check",
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
    fun grantedWorkspaceCheckReturnsTypedFailureEvidenceAndContinuesTheSameTurn() {
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
                VexLifeWorkspaceChecksFamily.Grant(
                    grantRef =
                        VexLifeWorkspaceChecksFamily.GrantRefPrefix +
                            "b".repeat(
                                64,
                            ),
                    sourceRef =
                        sourceRef,
                    sourceGrantedAt =
                        "2026-11-25T04:00:00Z",
                    grantedAt =
                        "2026-11-25T04:01:00Z",
                )
            val checkRuntimeRef =
                "image.vexlife-prod.private-service." +
                    "c".repeat(
                        32,
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
                                            VexLifeWorkspaceCheckDefinitions.Handle,
                                        )
                                    }
                            assertTrue(
                                available,
                            )
                            reply(
                                VexLifeCapabilityRequest.RequestHeader +
                                    "\n" +
                                    """{"capability":"workspace-check","action":"check","arguments":{"source":"$sourceRef","path":"README.md","required":["ready"]}}""",
                            )
                        }

                        2 -> {
                            assertTrue(
                                call.body.contains(
                                    VexLifeWorkspaceTextCheck.DispositionCheckFailed,
                                ),
                            )
                            assertFalse(
                                call.body.contains(
                                    "PRIVATE-WORKSPACE-BODY",
                                ),
                            )
                            reply(
                                "The check ran and the required text was missing.",
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
                                "send.workspace-check.same-turn.001",
                            content =
                                "Check whether README.md says ready.",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport =
                                transport,
                            currentWorkspaceCheckGrants = {
                                listOf(
                                    grant,
                                )
                            },
                            currentWorkspaceCheckRuntimeRef = {
                                checkRuntimeRef
                            },
                            capabilityOwners =
                                VexLifeCapabilityBridge.OwnerBindings(
                                    checkWorkspace = {
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
                                            "README.md",
                                            request.relativePath,
                                        )
                                        VexLifeWorkspaceTextCheck.Outcome.Completed(
                                            VexLifeWorkspaceTextCheck.Receipt(
                                                schemaVersion =
                                                    VexLifeWorkspaceTextCheck.ReceiptSchema,
                                                processRef =
                                                    VexLifeWorkspaceCheckDefinitions.ProcessRef,
                                                commandRef =
                                                    VexLifeWorkspaceCheckDefinitions.CommandRef,
                                                operationRef =
                                                    operationRef,
                                                workingScopeRef =
                                                    sourceRef,
                                                resourceLeaseRef =
                                                    VexLifeWorkspaceTextCheck.ResourceLeaseRefPrefix +
                                                        "d".repeat(
                                                            64,
                                                        ),
                                                checkGrantRef =
                                                    grant.grantRef,
                                                runtimeRef =
                                                    checkRuntimeRef,
                                                relativePath =
                                                    request.relativePath,
                                                disposition =
                                                    VexLifeWorkspaceTextCheck.DispositionCheckFailed,
                                                exitCode =
                                                    0,
                                                timedOut =
                                                    false,
                                                stdout =
                                                    "",
                                                stderr =
                                                    "",
                                                stdoutTruncated =
                                                    false,
                                                stderrTruncated =
                                                    false,
                                                observedSha256 =
                                                    "e".repeat(
                                                        64,
                                                    ),
                                                bytes =
                                                    12,
                                                failures =
                                                    listOf(
                                                        VexLifeWorkspaceTextCheck.Failure(
                                                            VexLifeWorkspaceTextCheck.FailureRequiredMissing,
                                                            0,
                                                        ),
                                                    ),
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
                "The check ran and the required text was missing.",
                result.content,
            )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.RETURNED,
                result.capabilityUses.single().state,
            )
            assertEquals(
                VexLifeWorkspaceCheckDefinitions.Handle,
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
