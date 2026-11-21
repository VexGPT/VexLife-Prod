package vexlife.artifact

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.VexLifeVisualInputQualification
import vexlife.model.heretic.HereticModelCatalog

class VexLifeLocalArtifactStoreTest {
    @Test
    fun sameRawBytesKeepOneArtifactIdentityWhileEachMessageBindingKeepsItsOwnName() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifact-same-bytes-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "artifact-same-bytes",
                lineageSuffix = "artifact-same-bytes",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val bytes =
                "same bytes, one immutable artifact\n"
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
            val first =
                store.storeOriginal(
                    bytes,
                )
            val second =
                store.storeOriginal(
                    bytes,
                )

            assertEquals(
                first,
                second,
            )
            assertEquals(
                64,
                first.sha256.length,
            )
            assertTrue(
                first.artifactRef.endsWith(
                    first.sha256,
                ),
            )

            val projection =
                store.storeTextProjection(
                    artifactRef = first.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "same bytes, one immutable artifact\n",
                )

            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.local-artifact.same-bytes.001",
                            requestMessageRef = "message.local-artifact.human.same-bytes.001",
                            content = "Compare these two named files.",
                            formedAt =
                                Instant.parse(
                                    "2026-11-20T06:00:00Z",
                                ),
                        )
                    }

            val binding =
                store.bindRequest(
                    request = request,
                    attachments =
                        listOf(
                            VexLifeLocalArtifact.RequestAttachment(
                                artifactRef = first.artifactRef,
                                projectionRef = projection.projectionRef,
                                displayName = "alpha.txt",
                            ),
                            VexLifeLocalArtifact.RequestAttachment(
                                artifactRef = second.artifactRef,
                                projectionRef = projection.projectionRef,
                                displayName = "beta.md",
                            ),
                        ),
                )

            assertEquals(
                listOf(
                    "alpha.txt",
                    "beta.md",
                ),
                binding.attachments.map {
                    it.displayName
                },
            )
            assertEquals(
                listOf(
                    first.artifactRef,
                    first.artifactRef,
                ),
                binding.attachments.map {
                    it.artifactRef
                },
            )
            assertEquals(
                "Compare these two named files.",
                request.content,
                "attachment binding must not rewrite canonical human REQUEST words",
            )
            assertFalse(
                request.content.contains(
                    "Attached files",
                    ignoreCase = true,
                ),
            )

            val rawFileCount =
                Files.list(
                    home.resolve(
                        "attachments/artifacts",
                    ),
                )
                    .use {
                        paths ->
                        paths
                            .filter {
                                it.fileName
                                    .toString()
                                    .endsWith(
                                        ".bin",
                                    )
                            }
                            .count()
                    }
            assertEquals(
                1L,
                rawFileCount,
                "same bytes must not mint another raw artifact",
            )
            assertTrue(
                store.rawBytes(
                    first.artifactRef,
                )!!
                    .contentEquals(
                        bytes,
                    ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun stagingRawAndDerivedMaterialDoesNotCreateARequestBinding() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifact-staged-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "artifact-staged",
                lineageSuffix = "artifact-staged",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    "not sent yet"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            store.storeTextProjection(
                artifactRef = original.artifactRef,
                converterRef = "converter.vexlife-prod.plain-text.v1",
                text = "not sent yet",
            )

            assertFalse(
                Files.exists(
                    home.resolve(
                        "runtime/vexlife-prod/request-resources",
                    ),
                ),
                "staging must not create model-visible request-resource binding",
            )

            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.local-artifact.staged.001",
                            requestMessageRef = "message.local-artifact.human.staged.001",
                            content = "Send without the staged file.",
                        )
                    }

            assertEquals(
                null,
                store.bindingFor(
                    request.eventHash,
                ),
                "a durable REQUEST does not acquire staged resources by proximity",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun exactRequestBindingIsImmutableAfterPublication() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifact-binding-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "artifact-binding",
                lineageSuffix = "artifact-binding",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    "immutable binding"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val projection =
                store.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "immutable binding",
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.local-artifact.binding.001",
                            requestMessageRef = "message.local-artifact.human.binding.001",
                            content = "Use the attached evidence.",
                        )
                    }
            val first =
                VexLifeLocalArtifact.RequestAttachment(
                    artifactRef = original.artifactRef,
                    projectionRef = projection.projectionRef,
                    displayName = "evidence.txt",
                )

            val bound =
                store.bindRequest(
                    request = request,
                    attachments = listOf(first),
                )
            assertEquals(
                request.eventHash,
                bound.requestEventHash,
            )
            assertEquals(
                request.contentHash,
                bound.requestContentHash,
            )

            assertFailsWith<IllegalArgumentException> {
                store.bindRequest(
                    request = request,
                    attachments =
                        listOf(
                            first.copy(
                                displayName = "renamed-after-send.txt",
                            ),
                        ),
                )
            }

            assertEquals(
                bound,
                store.bindingFor(
                    request.eventHash,
                ),
                "failed rebinding must preserve the original exact request-resource relationship",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun historicalRequestBindingSurvivesLaterRawArtifactCustodyLoss() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifact-historical-binding-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "artifact-historical-binding",
                lineageSuffix = "artifact-historical-binding",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    "historical resource bytes"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val projection =
                store.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "historical resource bytes",
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.local-artifact.historical-binding.001",
                            requestMessageRef =
                                "message.local-artifact.human.historical-binding.001",
                            content = "Keep showing what I sent even if I free the local bytes later.",
                        )
                    }
            val bound =
                store.bindRequest(
                    request = request,
                    attachments =
                        listOf(
                            VexLifeLocalArtifact.RequestAttachment(
                                artifactRef = original.artifactRef,
                                projectionRef = projection.projectionRef,
                                displayName = "history.txt",
                            ),
                        ),
                )

            Files.delete(
                home.resolve(
                    original.custodyRelativePath,
                ),
            )

            assertEquals(
                projection,
                store.historicalRequestProjection(
                    artifactRef = original.artifactRef,
                    projectionRef = projection.projectionRef,
                ),
                "historical presentation metadata must survive later original-byte custody loss",
            )
            assertFailsWith<IllegalArgumentException> {
                store.requestProjection(
                    artifactRef = original.artifactRef,
                    projectionRef = projection.projectionRef,
                )
            }
            assertEquals(
                bound,
                store.historicalBindingFor(
                    request.eventHash,
                ),
                "the immutable REQUEST -> resource relationship must survive later custody loss",
            )
            assertFailsWith<IllegalArgumentException> {
                store.bindingFor(
                    request.eventHash,
                )
            }
            assertEquals(
                "Keep showing what I sent even if I free the local bytes later.",
                request.content,
                "historical resource continuity must not rewrite canonical Conversation words",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun mutatedRawArtifactFailsClosedAgainstItsOriginalIdentity() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifact-tamper-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "artifact-tamper",
                lineageSuffix = "artifact-tamper",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val artifact =
                store.storeOriginal(
                    "hello world"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val rawPath =
                home.resolve(
                    artifact.custodyRelativePath,
                )

            Files.writeString(
                rawPath,
                "HELLO WORLD",
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )

            assertFailsWith<IllegalArgumentException> {
                store.original(
                    artifact.artifactRef,
                )
            }
            assertNotNull(
                Files.readString(
                    home.resolve(
                        VexLifeLocalArtifact.artifactMetadataRelativePath(
                            artifact.sha256,
                        ),
                    ),
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
    @Test
    fun staticImageBindsToRequestAndKeepsOneImmutableQualifiedInterpretation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifact-visual-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "artifact-visual",
                lineageSuffix = "artifact-visual",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    byteArrayOf(
                        1,
                        2,
                        3,
                        4,
                    ),
                )
            val projection =
                store.storeStaticImageProjection(
                    artifactRef = original.artifactRef,
                    adapterRef = "adapter.vexlife-prod.static-image.png.v1",
                    mediaType = "image/png",
                    widthPixels = 512,
                    heightPixels = 512,
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.local-artifact.visual.001",
                            requestMessageRef = "message.local-artifact.visual.001",
                            content = "What is shown?",
                        )
                    }
            val attachment =
                VexLifeLocalArtifact.RequestAttachment(
                    artifactRef = original.artifactRef,
                    projectionRef = projection.projectionRef,
                    displayName = "photo.png",
                )
            val duplicateAttachment =
                attachment.copy(
                    displayName = "photo-copy.png",
                )
            val binding =
                store.bindRequest(
                    request = request,
                    attachments =
                        listOf(
                            attachment,
                            duplicateAttachment,
                        ),
                )
            assertEquals(
                listOf(
                    "photo.png",
                    "photo-copy.png",
                ),
                binding.attachments.map {
                    it.displayName
                },
            )

            val evidence =
                VexLifeVisualInputQualification.QualifiedRuntimeEvidence(
                    evidenceRef = "evidence.vexlife-prod.visual.test.001",
                    modelRef = HereticModelCatalog.Models.Qwen4B.modelRef,
                    artifactRef =
                        HereticModelCatalog.Models.Qwen4B.identity.artifactSetRef,
                    artifactContentSetSha256 =
                        HereticModelCatalog.Models.Qwen4B.identity.artifactContentSetSha256,
                    environmentFingerprintSha256 =
                        "a".repeat(
                            64,
                        ),
                )
            val interpreterRef =
                "interpreter.vexlife-prod.visual.store-test.v1"
            val first =
                store.storeVisualInterpretation(
                    request = request,
                    attachment = attachment,
                    interpreterRef = interpreterRef,
                    evidence = evidence,
                    text = "A red square on a white background.",
                )
            val observed =
                assertNotNull(
                    store.visualInterpretationFor(
                        requestEventHash = request.eventHash,
                        projectionRef = projection.projectionRef,
                    ),
                )
            assertEquals(
                first,
                observed,
            )
            assertEquals(
                interpreterRef,
                observed.interpreterRef,
            )
            assertEquals(
                evidence.schemaVersion,
                observed.visualRuntimeEvidenceSchemaVersion,
            )
            assertEquals(
                evidence.evidenceRef,
                observed.visualEvidenceRef,
            )
            assertEquals(
                evidence.runtimeFamilyRef,
                observed.visualRuntimeFamilyRef,
            )
            assertEquals(
                evidence.pythonVersion,
                observed.visualPythonVersion,
            )
            assertEquals(
                evidence.mlxVlmVersion,
                observed.visualMlxVlmVersion,
            )
            assertEquals(
                evidence.mlxVersion,
                observed.visualMlxVersion,
            )
            assertEquals(
                evidence.transformersVersion,
                observed.visualTransformersVersion,
            )
            assertEquals(
                VexLifeVisualInputQualification.QualificationReturnSha256,
                observed.qualificationReturnSha256,
            )
            assertEquals(
                "A red square on a white background.",
                observed.text,
            )
            assertFailsWith<IllegalArgumentException> {
                store.storeVisualInterpretation(
                    request = request,
                    attachment = attachment,
                    interpreterRef = interpreterRef,
                    evidence = evidence,
                    text = "Different interpretation after publication.",
                )
            }
            assertEquals(
                first,
                store.visualInterpretationFor(
                    requestEventHash = request.eventHash,
                    projectionRef = projection.projectionRef,
                ),
            )

            val interpretationPath =
                home.resolve(
                    VexLifeLocalArtifact.requestVisualInterpretationRelativePath(
                        requestEventHash = request.eventHash,
                        projectionRef = projection.projectionRef,
                    ),
                )
            val originalContract =
                Files.readString(
                    interpretationPath,
                    StandardCharsets.UTF_8,
                )
            Files.writeString(
                interpretationPath,
                originalContract.replace(
                    evidence.environmentFingerprintSha256,
                    "e".repeat(
                        64,
                    ),
                ),
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
            assertFailsWith<IllegalArgumentException> {
                store.visualInterpretationFor(
                    requestEventHash = request.eventHash,
                    projectionRef = projection.projectionRef,
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
