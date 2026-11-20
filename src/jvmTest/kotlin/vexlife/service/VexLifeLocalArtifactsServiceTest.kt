package vexlife.service

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vexlife.artifact.VexLifeLocalArtifactStore
import vexlife.attachment.VexLifeAttachmentDraftFamily
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.VexLifeVisualInputQualification
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState
import vexlife.sources.VexLifeLocalFolderPicker
import vexlife.sources.VexLifeLocalSourceStore
import vexlife.sources.VexLifeLocalSourcesFamily

class VexLifeLocalArtifactsServiceTest {
    private val json =
        Json {
            encodeDefaults =
                true
            explicitNulls =
                true
            ignoreUnknownKeys =
                false
            isLenient =
                false
        }

    private val client =
        HttpClient.newHttpClient()

    private fun setup(
        home: Path,
    ): VexLifeProdLocalSetup.Result {
        val runtime =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef =
                    "receipt.vexlife-prod.heretic.local-artifacts-service-test",
                runtimeState =
                    HereticRuntimeState.Qualified,
                modelRef =
                    HereticProductIdentity.ModelRef,
                modelProfileRef =
                    HereticProductIdentity.ModelProfileRef,
                artifactRef =
                    HereticProductIdentity.ArtifactSetRef,
                artifactContentSetSha256 =
                    HereticProductIdentity.ArtifactContentSetSha256,
                runtimeAdapterRef =
                    HereticProductIdentity.RuntimeAdapterRef,
                runtimeClass =
                    HereticProductIdentity.RuntimeClass,
                endpoint =
                    HereticProductIdentity.Endpoint,
                requestModel =
                    HereticProductIdentity.RequestModel,
                qualification =
                    HereticRuntimeQualification.qualified(),
            )

        return VexLifeProdLocalSetup.Result(
            state =
                "LOCAL_HERETIC_FOUNDATION_READY",
            productHome =
                home.toString(),
            homeDisposition =
                "TEST_CURRENT",
            modelSelectionDisposition =
                "TEST_BOUND",
            managedPythonDisposition =
                "TEST_REUSED",
            artifactDisposition =
                "TEST_REUSED",
            environmentDisposition =
                "TEST_REUSED",
            runtimeDisposition =
                "TEST_REUSED",
            modelBindingDisposition =
                "TEST_BOUND",
            modelConfigurationState =
                HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence =
                runtime,
            endpoint =
                runtime.endpoint,
            requestModel =
                runtime.requestModel,
        )
    }

    private fun setup4B(
        home: Path,
    ): VexLifeProdLocalSetup.Result {
        val model =
            HereticModelCatalog.Models.Qwen4B
        val realization =
            HereticRuntimeRealization.require(
                modelRef = model.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        val runtime =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef =
                    "receipt.vexlife-prod.heretic.local-artifacts-visual-test",
                runtimeState =
                    HereticRuntimeState.Qualified,
                modelRef = model.modelRef,
                realizationRef = realization.realizationRef,
                modelProfileRef = realization.modelProfileRef,
                artifactRef = realization.artifact.artifactRef,
                artifactContentSetSha256 =
                    realization.artifact.contentSetSha256,
                runtimeAdapterRef =
                    realization.runtime.runtimeAdapterRef,
                runtimeClass =
                    realization.runtime.runtimeClass,
                endpoint =
                    realization.runtime.endpoint,
                requestModel =
                    realization.runtime.requestModel,
                qualification =
                    HereticRuntimeQualification.qualified(),
            )
        return VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = home.toString(),
            homeDisposition = "TEST_CURRENT",
            modelSelectionDisposition = "TEST_BOUND",
            managedPythonDisposition = "TEST_REUSED",
            artifactDisposition = "TEST_REUSED",
            environmentDisposition = "TEST_REUSED",
            runtimeDisposition = "TEST_REUSED",
            modelBindingDisposition = "TEST_BOUND",
            modelConfigurationState =
                HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence = runtime,
            endpoint = runtime.endpoint,
            requestModel = runtime.requestModel,
        )
    }

    private fun visualEvidence(): VexLifeVisualInputQualification.QualifiedRuntimeEvidence {
        val model =
            HereticModelCatalog.Models.Qwen4B
        return VexLifeVisualInputQualification.QualifiedRuntimeEvidence(
            evidenceRef =
                "evidence.vexlife-prod.local-artifacts-visual-test.001",
            modelRef = model.modelRef,
            artifactRef =
                model.identity.artifactSetRef,
            artifactContentSetSha256 =
                model.identity.artifactContentSetSha256,
            environmentFingerprintSha256 =
                "f".repeat(
                    64,
                ),
        )
    }

    private fun get(
        origin: String,
        path: String,
    ): HttpResponse<String> =
        client.send(
            HttpRequest
                .newBuilder(
                    URI(
                        origin +
                            path,
                    ),
                )
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8,
            ),
        )

    private fun postJson(
        origin: String,
        path: String,
        body: String,
    ): HttpResponse<String> =
        client.send(
            HttpRequest
                .newBuilder(
                    URI(
                        origin +
                            path,
                    ),
                )
                .header(
                    "content-type",
                    "application/json",
                )
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        body,
                        StandardCharsets.UTF_8,
                    ),
                )
                .build(),
            HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8,
            ),
        )

    private fun zipOf(
        vararg members: Pair<String, ByteArray>,
    ): ByteArray {
        val output =
            ByteArrayOutputStream()
        ZipOutputStream(
            output,
        )
            .use {
                zip ->
                members.forEach {
                    (name, bytes) ->
                    zip.putNextEntry(
                        ZipEntry(
                            name,
                        ),
                    )
                    zip.write(
                        bytes,
                    )
                    zip.closeEntry()
                }
            }
        return output.toByteArray()
    }

    private fun postArtifact(
        origin: String,
        draftRef: String,
        name: String,
        bytes: ByteArray,
    ): HttpResponse<String> =
        client.send(
            HttpRequest
                .newBuilder(
                    URI(
                        origin +
                            VexLifeProdServiceContract.AttachmentStagePath,
                    ),
                )
                .header(
                    "content-type",
                    "application/octet-stream",
                )
                .header(
                    VexLifeProdServiceContract.AttachmentDraftRefHeader,
                    draftRef,
                )
                .header(
                    VexLifeProdServiceContract.AttachmentNameHeader,
                    java.net.URLEncoder.encode(
                        name,
                        StandardCharsets.UTF_8,
                    ),
                )
                .POST(
                    HttpRequest.BodyPublishers.ofByteArray(
                        bytes,
                    ),
                )
                .build(),
            HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8,
            ),
        )

    @Test
    fun plainTextStageThroughCanonicalSendBindsExactRequestAndClearsOnlyAfterCompletion() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifacts-service-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "local-artifacts-service",
                lineageSuffix = "local-artifacts-service",
            )
            var observedIntent: DirectCompanionTurn.Intent? =
                null
            var providerCalls =
                0
            val provider =
                DirectCompanionConnection.Transport {
                    providerCalls +=
                        1
                    DirectCompanionConnection.HttpResult(
                        status = 200,
                        body =
                            """{"model":"default_model","choices":[{"message":{"role":"assistant","content":"I read the attached note."}}]}""",
                    )
                }

            VexLifeProdLocalService
                .startForTest(
                    sourceSha =
                        "a".repeat(
                            40,
                        ),
                    setup =
                        setup(
                            home,
                        ),
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            foundation,
                            intent,
                            ->
                            observedIntent =
                                intent
                            DirectCompanionTurn.performWithHooks(
                                setup = foundation,
                                intent = intent,
                                hooks =
                                    DirectCompanionTurn.Hooks(
                                        transport = provider,
                                    ),
                            )
                        },
                )
                .use {
                    service ->
                    val initial =
                        json.decodeFromString<VexLifeProdServiceContract.AttachmentDraftProjection>(
                            get(
                                service.origin,
                                VexLifeProdServiceContract.AttachmentDraftPath,
                            ).body(),
                        )
                    assertEquals(
                        "EMPTY",
                        initial.state,
                    )

                    val stage =
                        postArtifact(
                            origin = service.origin,
                            draftRef = "draft.attachment.service.001",
                            name = "notes.txt",
                            bytes =
                                "alpha\nbeta\n"
                                    .toByteArray(
                                        StandardCharsets.UTF_8,
                                    ),
                        )
                    assertEquals(
                        200,
                        stage.statusCode(),
                        stage.body(),
                    )
                    val staged =
                        json.decodeFromString<VexLifeProdServiceContract.AttachmentStageResponse>(
                            stage.body(),
                        )
                    assertEquals(
                        VexLifeProdServiceContract.AttachmentStageOutcome.READY,
                        staged.outcome,
                    )
                    assertEquals(
                        "notes.txt",
                        staged.draft.items.single().displayName,
                    )
                    assertTrue(
                        staged.draft.display.requestResourcesReady,
                    )

                    val humanWords =
                        "Please use the note I attached."
                    val send =
                        postJson(
                            origin = service.origin,
                            path = VexLifeProdServiceContract.ConversationSendPath,
                            body =
                                json.encodeToString(
                                    VexLifeProdServiceContract.SendRequest(
                                        idempotencyKey =
                                            "send.local-artifacts.service.001",
                                        content =
                                            humanWords,
                                    ),
                                ),
                        )
                    assertEquals(
                        200,
                        send.statusCode(),
                        send.body(),
                    )
                    assertEquals(
                        1,
                        providerCalls,
                    )

                    val intent =
                        assertNotNull(
                            observedIntent,
                        )
                    assertEquals(
                        humanWords,
                        intent.content,
                    )
                    assertEquals(
                        1,
                        intent.requestAttachments.size,
                    )
                    assertEquals(
                        "notes.txt",
                        intent.requestAttachments.single().displayName,
                    )

                    val request =
                        DirectConversationStore(
                            home,
                        )
                            .current()
                            .canonicalEvents
                            .single {
                                it.eventKind ==
                                    "REQUEST"
                            }
                    assertEquals(
                        humanWords,
                        request.content,
                    )
                    val binding =
                        assertNotNull(
                            VexLifeLocalArtifactStore(
                                home,
                            )
                                .bindingFor(
                                    request.eventHash,
                                ),
                        )
                    assertEquals(
                        intent.requestAttachments,
                        binding.attachments,
                    )

                    val after =
                        json.decodeFromString<VexLifeProdServiceContract.AttachmentDraftProjection>(
                            get(
                                service.origin,
                                VexLifeProdServiceContract.AttachmentDraftPath,
                            ).body(),
                        )
                    assertEquals(
                        "EMPTY",
                        after.state,
                    )
                    assertTrue(
                        after.items.isEmpty(),
                    )

                    // The browser may miss the terminal event after the canonical Send completed.
                    // Reconciliation must recover the durable REQUEST + binding even though the
                    // in-memory Composer draft has already been cleared.
                    val reconciled =
                        postJson(
                            origin = service.origin,
                            path = VexLifeProdServiceContract.ConversationSendPath,
                            body =
                                json.encodeToString(
                                    VexLifeProdServiceContract.SendRequest(
                                        idempotencyKey =
                                            "send.local-artifacts.service.001",
                                        content =
                                            humanWords,
                                    ),
                                ),
                        )
                    assertEquals(
                        200,
                        reconciled.statusCode(),
                        reconciled.body(),
                    )
                    val recovered =
                        json.decodeFromString<VexLifeProdServiceContract.SendResponse>(
                            reconciled.body(),
                        )
                    assertTrue(
                        recovered.recoveredCompletedTurn,
                    )
                    assertEquals(
                        1,
                        providerCalls,
                        "reconciliation after draft clear must not invoke the provider again",
                    )
                    assertEquals(
                        intent.requestAttachments,
                        assertNotNull(
                            observedIntent,
                        )
                            .requestAttachments,
                        "the service must restore the durable request-resource relationship for the duplicate",
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
    @Test
    fun recognizedStaticImageStagesHeldAndCannotEnterCanonicalSend() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifacts-image-service-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "local-artifacts-image-service",
                lineageSuffix = "local-artifacts-image-service",
            )
            var turnCalls =
                0

            VexLifeProdLocalService
                .startForTest(
                    sourceSha =
                        "c".repeat(
                            40,
                        ),
                    setup =
                        setup(
                            home,
                        ),
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            _,
                            _,
                            ->
                            turnCalls +=
                                1
                            error(
                                "held static image must not enter a Companion turn",
                            )
                        },
                )
                .use {
                    service ->
                    val imageBytes =
                        ByteArrayOutputStream()
                            .also {
                                output ->
                                assertTrue(
                                    ImageIO.write(
                                        BufferedImage(
                                            3,
                                            2,
                                            BufferedImage.TYPE_INT_RGB,
                                        ),
                                        "png",
                                        output,
                                    ),
                                )
                            }
                            .toByteArray()

                    val stage =
                        postArtifact(
                            origin = service.origin,
                            draftRef = "draft.attachment.image-service.001",
                            name = "renamed-image.txt",
                            bytes = imageBytes,
                        )
                    assertEquals(
                        200,
                        stage.statusCode(),
                        stage.body(),
                    )
                    val staged =
                        json.decodeFromString<VexLifeProdServiceContract.AttachmentStageResponse>(
                            stage.body(),
                        )
                    assertEquals(
                        VexLifeProdServiceContract.AttachmentStageOutcome.HELD,
                        staged.outcome,
                    )
                    assertTrue(
                        staged.message
                            ?.contains(
                                "not currently available",
                            ) ==
                            true,
                    )
                    val item =
                        staged.draft.items.single()
                    assertEquals(
                        VexLifeAttachmentDraftFamily.ItemStatus.HELD,
                        item.status,
                    )
                    assertNotNull(
                        item.artifactRef,
                    )
                    assertEquals(
                        null,
                        item.projectionRef,
                        "held image must not claim a request projection in the Composer draft",
                    )
                    assertTrue(
                        !staged.draft.display.requestResourcesReady,
                        "held image must not become a Send resource",
                    )

                    val send =
                        postJson(
                            origin = service.origin,
                            path = VexLifeProdServiceContract.ConversationSendPath,
                            body =
                                json.encodeToString(
                                    VexLifeProdServiceContract.SendRequest(
                                        idempotencyKey =
                                            "send.local-artifacts.image-service.001",
                                        content =
                                            "What is in this image?",
                                    ),
                                ),
                        )
                    assertEquals(
                        409,
                        send.statusCode(),
                        send.body(),
                    )
                    assertTrue(
                        send.body()
                            .contains(
                                "ATTACHMENT_DRAFT_NOT_READY",
                            ),
                    )
                    assertEquals(
                        0,
                        turnCalls,
                        "held image staging must not invoke the Companion",
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun qualifiedStaticImageStagesReadyAndCanonicalSendConsumesOneInterpretation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifacts-image-ready-service-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "local-artifacts-image-ready",
                lineageSuffix = "local-artifacts-image-ready",
            )
            val evidence =
                visualEvidence()
            var interpreterCalls =
                0
            var providerCalls =
                0
            val provider =
                DirectCompanionConnection.Transport {
                    call ->
                    providerCalls +=
                        1
                    assertTrue(
                        call.body.contains(
                            "A green triangle on a white background.",
                        ),
                    )
                    DirectCompanionConnection.HttpResult(
                        status = 200,
                        body =
                            """{"model":"default_model","choices":[{"message":{"role":"assistant","content":"The image shows a green triangle."}}]}""",
                    )
                }

            VexLifeProdLocalService
                .startForTest(
                    sourceSha =
                        "d".repeat(
                            40,
                        ),
                    setup =
                        setup4B(
                            home,
                        ),
                    currentQualifiedVisualInputEvidence = {
                        evidence
                    },
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            foundation,
                            intent,
                            ->
                            DirectCompanionTurn.performWithHooks(
                                setup = foundation,
                                intent = intent,
                                hooks =
                                    DirectCompanionTurn.Hooks(
                                        transport = provider,
                                        visualInterpreter =
                                            DirectCompanionTurn.VisualInterpreter {
                                                request ->
                                                interpreterCalls +=
                                                    1
                                                assertTrue(
                                                    request.bytes.isNotEmpty(),
                                                )
                                                DirectCompanionTurn.VisualInterpretationResult(
                                                    interpreterRef =
                                                        "interpreter.vexlife-prod.visual.service-test.v1",
                                                    evidence = evidence,
                                                    text =
                                                        "A green triangle on a white background.",
                                                )
                                            },
                                    ),
                            )
                        },
                )
                .use {
                    service ->
                    val imageBytes =
                        ByteArrayOutputStream()
                            .also {
                                output ->
                                assertTrue(
                                    ImageIO.write(
                                        BufferedImage(
                                            32,
                                            24,
                                            BufferedImage.TYPE_INT_RGB,
                                        ),
                                        "png",
                                        output,
                                    ),
                                )
                            }
                            .toByteArray()

                    val stage =
                        postArtifact(
                            origin = service.origin,
                            draftRef =
                                "draft.attachment.image-ready.001",
                            name =
                                "renamed-image.txt",
                            bytes = imageBytes,
                        )
                    assertEquals(
                        200,
                        stage.statusCode(),
                        stage.body(),
                    )
                    val staged =
                        json.decodeFromString<VexLifeProdServiceContract.AttachmentStageResponse>(
                            stage.body(),
                        )
                    assertEquals(
                        VexLifeProdServiceContract.AttachmentStageOutcome.READY,
                        staged.outcome,
                    )
                    val item =
                        staged.draft.items.single()
                    assertEquals(
                        VexLifeAttachmentDraftFamily.ItemStatus.READY,
                        item.status,
                    )
                    assertNotNull(
                        item.projectionRef,
                    )
                    assertTrue(
                        staged.draft.display.requestResourcesReady,
                    )

                    val send =
                        postJson(
                            origin = service.origin,
                            path =
                                VexLifeProdServiceContract.ConversationSendPath,
                            body =
                                json.encodeToString(
                                    VexLifeProdServiceContract.SendRequest(
                                        idempotencyKey =
                                            "send.local-artifacts.image-ready.001",
                                        content =
                                            "What shape is shown?",
                                    ),
                                ),
                        )
                    assertEquals(
                        200,
                        send.statusCode(),
                        send.body(),
                    )
                    assertEquals(
                        1,
                        interpreterCalls,
                    )
                    assertEquals(
                        1,
                        providerCalls,
                    )

                    val request =
                        DirectConversationStore(
                            home,
                        )
                            .current()
                            .canonicalEvents
                            .single {
                                it.eventKind ==
                                    "REQUEST"
                            }
                    val binding =
                        assertNotNull(
                            VexLifeLocalArtifactStore(
                                home,
                            )
                                .bindingFor(
                                    request.eventHash,
                                ),
                        )
                    val attachment =
                        binding.attachments.single()
                    val interpretation =
                        assertNotNull(
                            VexLifeLocalArtifactStore(
                                home,
                            )
                                .visualInterpretationFor(
                                    requestEventHash = request.eventHash,
                                    projectionRef =
                                        attachment.projectionRef,
                                ),
                        )
                    assertEquals(
                        evidence.evidenceRef,
                        interpretation.visualEvidenceRef,
                    )
                    assertEquals(
                        "A green triangle on a white background.",
                        interpretation.text,
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun officeContainerUsesTheSameDraftRouteAndContentDerivedConverter() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifacts-office-service-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "local-artifacts-office",
                lineageSuffix = "local-artifacts-office",
            )

            VexLifeProdLocalService
                .startForTest(
                    sourceSha =
                        "b".repeat(
                            40,
                        ),
                    setup =
                        setup(
                            home,
                        ),
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            _,
                            _,
                            ->
                            error(
                                "office staging test must not send a Conversation turn",
                            )
                        },
                )
                .use {
                    service ->
                    val document =
                        """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>
                        <w:p><w:r><w:t>Service-level Word evidence</w:t></w:r></w:p>
                        </w:body></w:document>"""
                            .trimIndent()
                    val stage =
                        postArtifact(
                            origin = service.origin,
                            draftRef = "draft.attachment.office.001",
                            name = "renamed-office.data",
                            bytes =
                                zipOf(
                                    "word/document.xml" to
                                        document.toByteArray(),
                                ),
                        )
                    assertEquals(
                        200,
                        stage.statusCode(),
                        stage.body(),
                    )
                    val staged =
                        json.decodeFromString<VexLifeProdServiceContract.AttachmentStageResponse>(
                            stage.body(),
                        )
                    assertEquals(
                        VexLifeProdServiceContract.AttachmentStageOutcome.READY,
                        staged.outcome,
                    )
                    val item =
                        staged.draft.items.single()
                    val projection =
                        assertNotNull(
                            VexLifeLocalArtifactStore(
                                home,
                            )
                                .projection(
                                    artifactRef =
                                        assertNotNull(
                                            item.artifactRef,
                                        ),
                                    projectionRef =
                                        assertNotNull(
                                            item.projectionRef,
                                        ),
                                ),
                        )
                    assertEquals(
                        "converter.vexlife-prod.docx-text.v1",
                        projection.converterRef,
                    )
                    assertTrue(
                        projection.text.contains(
                            "Service-level Word evidence",
                        ),
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun localSourcesAddFolderUsesPickerThenPersistsAndRemoveRevokesTheGrant() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-sources-service-",
            )
                .toRealPath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val project =
            Files.createDirectories(
                parent.resolve(
                    "Project",
                ),
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "local-sources-service",
                lineageSuffix = "local-sources-service",
            )

            var pickerCalls =
                0
            val picker =
                VexLifeLocalFolderPicker {
                    pickerCalls +=
                        1
                    VexLifeLocalFolderPicker.Result.Selected(
                        project,
                    )
                }

            VexLifeProdLocalService
                .startForTest(
                    sourceSha =
                        "b".repeat(
                            40,
                        ),
                    setup =
                        setup(
                            home,
                        ),
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            _,
                            _,
                            ->
                            error(
                                "Local Sources settings test must not send a Conversation turn",
                            )
                        },
                    localFolderPicker =
                        picker,
                )
                .use {
                    service ->
                    val initial =
                        json.decodeFromString<VexLifeProdServiceContract.LocalSourcesProjection>(
                            get(
                                service.origin,
                                VexLifeProdServiceContract.LocalSourcesPath,
                            ).body(),
                        )
                    assertEquals(
                        VexLifeLocalSourcesFamily.StateEmpty,
                        initial.state,
                    )

                    val addedResponse =
                        postJson(
                            origin = service.origin,
                            path = VexLifeProdServiceContract.LocalSourcesControlPath,
                            body =
                                json.encodeToString(
                                    VexLifeProdServiceContract.LocalSourcesControlRequest(
                                        controlRef =
                                            VexLifeLocalSourcesFamily.ControlAddFolder,
                                    ),
                                ),
                        )
                    assertEquals(
                        200,
                        addedResponse.statusCode(),
                        addedResponse.body(),
                    )
                    val added =
                        json.decodeFromString<VexLifeProdServiceContract.LocalSourcesControlResponse>(
                            addedResponse.body(),
                        )
                    assertEquals(
                        VexLifeProdServiceContract.LocalSourcesControlOutcome.REALIZED,
                        added.outcome,
                    )
                    assertTrue(
                        added.grantMutationPerformed,
                    )
                    assertEquals(
                        1,
                        pickerCalls,
                    )
                    val grant =
                        added.sources.grants.single()
                    assertEquals(
                        "Project",
                        grant.name,
                    )
                    assertTrue(
                        VexLifeLocalSourcesFamily.isSourceRef(
                            grant.sourceRef,
                        ),
                    )
                    assertEquals(
                        listOf(
                            grant,
                        ),
                        VexLifeLocalSourceStore(
                            productHome = home,
                            personHome = null,
                        )
                            .grants(),
                        "grant must survive outside the service instance projection",
                    )

                    val removedResponse =
                        postJson(
                            origin = service.origin,
                            path = VexLifeProdServiceContract.LocalSourcesControlPath,
                            body =
                                json.encodeToString(
                                    VexLifeProdServiceContract.LocalSourcesControlRequest(
                                        controlRef =
                                            VexLifeLocalSourcesFamily.ControlRemove,
                                        sourceRef =
                                            grant.sourceRef,
                                    ),
                                ),
                        )
                    val removed =
                        json.decodeFromString<VexLifeProdServiceContract.LocalSourcesControlResponse>(
                            removedResponse.body(),
                        )
                    assertEquals(
                        VexLifeProdServiceContract.LocalSourcesControlOutcome.REALIZED,
                        removed.outcome,
                    )
                    assertTrue(
                        removed.sources.grants
                            .isEmpty(),
                    )
                    assertTrue(
                        VexLifeLocalSourceStore(
                            productHome = home,
                            personHome = null,
                        )
                            .grants()
                            .isEmpty(),
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

}

// [VXG RealForever]
