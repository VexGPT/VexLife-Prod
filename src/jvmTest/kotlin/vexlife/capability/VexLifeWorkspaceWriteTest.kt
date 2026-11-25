package vexlife.capability

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.sources.VexLifeLocalSourceStore
import vexlife.workspace.VexLifeWorkspaceEffectStore

class VexLifeWorkspaceWriteTest {
    private data class Fixture(
        val parent: Path,
        val home: Path,
        val source: Path,
        val sourceRef: String,
        val effects: VexLifeWorkspaceEffectStore,
        val writer: VexLifeWorkspaceWrite,
    )

    private fun withFixture(block: (Fixture) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-workspace-write-").toRealPath()
        val person = Files.createDirectories(parent.resolve("person"))
        val home = person.resolve("VexLife-Prod")
        val source = Files.createDirectories(person.resolve("project"))
        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "workspace-write",
                lineageSuffix = "workspace-write",
            )
            val localSources =
                VexLifeLocalSourceStore(
                    productHome = home,
                    personHome = person,
                    now = { Instant.parse("2026-11-24T21:00:00Z") },
                )
            assertIs<VexLifeLocalSourceStore.Outcome.Realized>(localSources.add(source))
            val sourceRef = localSources.grants().single().sourceRef
            val effects =
                VexLifeWorkspaceEffectStore(
                    productHome = home,
                    localSources = localSources,
                    now = { Instant.parse("2026-11-24T21:01:00Z") },
                )
            assertIs<VexLifeWorkspaceEffectStore.Outcome.Realized>(effects.grant(sourceRef))
            val writer =
                VexLifeWorkspaceWrite(
                    productHome = home,
                    localSources = localSources,
                    workspaceEffects = effects,
                    now = { Instant.parse("2026-11-24T21:02:00Z") },
                )
            block(Fixture(parent, home, source, sourceRef, effects, writer))
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun createIsBoundedAndReceiptDoesNotPersistReturnedSourceBody() =
        withFixture { fixture ->
            val outcome =
                assertIs<VexLifeWorkspaceWrite.Outcome.Completed>(
                    fixture.writer.write(
                        operationRef = "operation.test.create",
                        sourceRef = fixture.sourceRef,
                        relativePath = "hello.txt",
                        content = "PRIVATE-CONTENT",
                    )
                )

            assertEquals(
                VexLifeWorkspaceWrite.DispositionCreated,
                outcome.receipt.disposition,
            )
            assertEquals(
                "PRIVATE-CONTENT",
                Files.readString(fixture.source.resolve("hello.txt")),
            )
            assertTrue(outcome.receipt.externalEffectsExecuted)
            assertTrue(outcome.receipt.effectAuthorityGranted)

            val key = sha256("operation.test.create")
            val receiptText =
                Files.readString(
                    fixture.home.resolve(
                        VexLifeWorkspaceWrite.RuntimeRoot + "/receipts/" + key + ".json"
                    )
                )
            assertTrue(!receiptText.contains("PRIVATE-CONTENT"))
        }

    @Test
    fun editRequiresExactPriorDigestAndKeepsPrivateBeforeImage() =
        withFixture { fixture ->
            val target = fixture.source.resolve("app.js")
            Files.writeString(target, "before")
            val beforeSha = sha256("before")

            val replaced =
                assertIs<VexLifeWorkspaceWrite.Outcome.Completed>(
                    fixture.writer.write(
                        operationRef = "operation.test.replace",
                        sourceRef = fixture.sourceRef,
                        relativePath = "app.js",
                        content = "after",
                        expectedPriorSha256 = beforeSha,
                    )
                )
            assertEquals(VexLifeWorkspaceWrite.DispositionReplaced, replaced.receipt.disposition)
            assertEquals("after", Files.readString(target))

            val key = sha256("operation.test.replace")
            val beforeImage =
                Files.readString(
                    fixture.home.resolve(
                        VexLifeWorkspaceWrite.RuntimeRoot + "/before-images/" + key + ".json"
                    )
                )
            assertTrue(beforeImage.contains("before"))
            val receipt =
                Files.readString(
                    fixture.home.resolve(
                        VexLifeWorkspaceWrite.RuntimeRoot + "/receipts/" + key + ".json"
                    )
                )
            assertTrue(!receipt.contains("\"content\""))

            val stale =
                assertIs<VexLifeWorkspaceWrite.Outcome.Refused>(
                    fixture.writer.write(
                        operationRef = "operation.test.stale",
                        sourceRef = fixture.sourceRef,
                        relativePath = "app.js",
                        content = "wrong",
                        expectedPriorSha256 = beforeSha,
                    )
                )
            assertEquals(VexLifeWorkspaceWrite.RefusedStaleExpectation, stale.reason)
            assertEquals("after", Files.readString(target))
        }

    @Test
    fun missingReceiptRecoversCompletedIntentWithoutReplayingMutation() =
        withFixture { fixture ->
            val first =
                assertIs<VexLifeWorkspaceWrite.Outcome.Completed>(
                    fixture.writer.write(
                        operationRef = "operation.test.recover",
                        sourceRef = fixture.sourceRef,
                        relativePath = "recover.txt",
                        content = "formed",
                    )
                )
            assertEquals(VexLifeWorkspaceWrite.DispositionCreated, first.receipt.disposition)

            val key = sha256("operation.test.recover")
            Files.delete(
                fixture.home.resolve(
                    VexLifeWorkspaceWrite.RuntimeRoot + "/receipts/" + key + ".json"
                )
            )
            val modifiedAt = Files.getLastModifiedTime(fixture.source.resolve("recover.txt"))

            val recovered =
                assertIs<VexLifeWorkspaceWrite.Outcome.Completed>(
                    fixture.writer.write(
                        operationRef = "operation.test.recover",
                        sourceRef = fixture.sourceRef,
                        relativePath = "recover.txt",
                        content = "formed",
                    )
                )
            assertEquals(VexLifeWorkspaceWrite.DispositionRecovered, recovered.receipt.disposition)
            assertEquals(modifiedAt, Files.getLastModifiedTime(fixture.source.resolve("recover.txt")))
        }

    @Test
    fun completedIntentMayRecoverAfterRevocationButNewMutationCannotReplay() =
        withFixture { fixture ->
            assertIs<VexLifeWorkspaceWrite.Outcome.Completed>(
                fixture.writer.write(
                    operationRef = "operation.test.revoked-recovery",
                    sourceRef = fixture.sourceRef,
                    relativePath = "revoked.txt",
                    content = "already-written",
                )
            )
            val key = sha256("operation.test.revoked-recovery")
            Files.delete(
                fixture.home.resolve(
                    VexLifeWorkspaceWrite.RuntimeRoot + "/receipts/" + key + ".json"
                )
            )
            assertIs<VexLifeWorkspaceEffectStore.Outcome.Realized>(
                fixture.effects.revoke(fixture.sourceRef)
            )

            val recovered =
                assertIs<VexLifeWorkspaceWrite.Outcome.Completed>(
                    fixture.writer.write(
                        operationRef = "operation.test.revoked-recovery",
                        sourceRef = fixture.sourceRef,
                        relativePath = "revoked.txt",
                        content = "already-written",
                    )
                )
            assertEquals(VexLifeWorkspaceWrite.DispositionRecovered, recovered.receipt.disposition)

            val refused =
                assertIs<VexLifeWorkspaceWrite.Outcome.Refused>(
                    fixture.writer.write(
                        operationRef = "operation.test.after-revoke",
                        sourceRef = fixture.sourceRef,
                        relativePath = "new.txt",
                        content = "must-not-write",
                    )
                )
            assertEquals(VexLifeWorkspaceWrite.RefusedGrantNotCurrent, refused.reason)
            assertTrue(!Files.exists(fixture.source.resolve("new.txt")))
        }

    @Test
    fun pathEscapeMissingParentAndSymlinkTargetsFailClosed() =
        withFixture { fixture ->
            assertEquals(
                VexLifeWorkspaceWrite.RefusedPath,
                assertIs<VexLifeWorkspaceWrite.Outcome.Refused>(
                    fixture.writer.write(
                        operationRef = "operation.test.escape",
                        sourceRef = fixture.sourceRef,
                        relativePath = "../escape.txt",
                        content = "no",
                    )
                ).reason,
            )
            assertEquals(
                VexLifeWorkspaceWrite.RefusedParentMissing,
                assertIs<VexLifeWorkspaceWrite.Outcome.Refused>(
                    fixture.writer.write(
                        operationRef = "operation.test.parent",
                        sourceRef = fixture.sourceRef,
                        relativePath = "missing/file.txt",
                        content = "no",
                    )
                ).reason,
            )

            val outside = fixture.parent.resolve("outside.txt")
            Files.writeString(outside, "outside")
            val link = fixture.source.resolve("link.txt")
            val linked = runCatching { Files.createSymbolicLink(link, outside) }.isSuccess
            if (linked) {
                val refused =
                    assertIs<VexLifeWorkspaceWrite.Outcome.Refused>(
                        fixture.writer.write(
                            operationRef = "operation.test.link",
                            sourceRef = fixture.sourceRef,
                            relativePath = "link.txt",
                            content = "no",
                            expectedPriorSha256 = sha256("outside"),
                        )
                    )
                assertEquals(VexLifeWorkspaceWrite.RefusedTargetInvalid, refused.reason)
                assertEquals("outside", Files.readString(outside))
            }
        }

    private fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

// [VXG RealForever]
