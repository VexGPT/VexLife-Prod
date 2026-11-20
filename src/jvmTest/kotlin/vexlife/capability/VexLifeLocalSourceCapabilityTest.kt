package vexlife.capability

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.sources.VexLifeLocalSourceStore
import vexlife.sources.VexLifeLocalSourcesFamily

class VexLifeLocalSourceCapabilityTest {
    private val tokens: (String) -> Int = {
        (
            it.toByteArray(
                StandardCharsets.UTF_8,
            ).size +
                3
        ) /
            4 +
            4
    }

    private fun request(
        json: String,
    ): VexLifeCapabilityRequest.Parsed =
        assertNotNull(
            VexLifeCapabilityRequest.parseOrNull(
                VexLifeCapabilityRequest.RequestHeader +
                    "\n" +
                    json,
            ),
        )

    private fun perform(
        home: Path,
        json: String,
    ): VexLifeCapabilityBridge.Exchange =
        VexLifeCapabilityBridge.perform(
            parsed =
                request(
                    json,
                ),
            admission =
                VexLifeCapabilityBridge.Admission(
                    productHome = home,
                    availableCapabilityRefs =
                        VexLifeReachDefinitions.Definitions
                            .map {
                                it.capabilityRef
                            }
                            .toSet(),
                    requestNumber = 1,
                    tokensAvailable = 20_000,
                ),
            estimateTokens = tokens,
        )

    @Test
    fun bridgeListsFindsAndReadsOnlyInsideAnExactPersistentGrant() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-source-capability-",
            )
                .toRealPath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val folder =
            Files.createDirectories(
                parent.resolve(
                    "Project",
                ),
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "source-capability",
                lineageSuffix = "source-capability",
            )
            Files.createDirectories(
                folder.resolve(
                    "notes",
                ),
            )
            Files.writeString(
                folder.resolve(
                    "notes/invoice-2026.md",
                ),
                "# Invoice\nTotal due: 42\n",
            )
            Files.writeString(
                folder.resolve(
                    "notes/other.txt",
                ),
                "nothing here\n",
            )

            val sourceRef =
                (
                    VexLifeLocalSourceStore(
                        productHome = home,
                        personHome = null,
                    )
                        .add(
                            folder,
                        ) as
                        VexLifeLocalSourceStore.Outcome.Realized
                )
                    .grants
                    .single()
                    .sourceRef

            assertTrue(
                VexLifeLocalSourcesFamily.isSourceRef(
                    sourceRef,
                ),
            )

            val grants =
                perform(
                    home,
                    """{"capability":"sources","action":"list","arguments":{}}""",
                )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.RETURNED,
                grants.entry.state,
            )
            assertTrue(
                grants.resultMessage.content.contains(
                    sourceRef,
                ),
            )
            assertFalse(
                grants.entry.untrustedContent,
                "grant metadata is Product-authored state",
            )

            val found =
                perform(
                    home,
                    """{"capability":"sources","action":"find","arguments":{"source":"$sourceRef","name":"invoice","text":"total due"}}""",
                )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.RETURNED,
                found.entry.state,
            )
            assertTrue(
                found.entry.untrustedContent,
            )
            assertTrue(
                found.resultMessage.content.contains(
                    "notes/invoice-2026.md:2",
                ),
            )

            val read =
                perform(
                    home,
                    """{"capability":"sources","action":"read","arguments":{"source":"$sourceRef","path":"notes/invoice-2026.md","startLine":2,"maxLines":1}}""",
                )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.RETURNED,
                read.entry.state,
            )
            assertTrue(
                read.entry.untrustedContent,
            )
            assertTrue(
                read.resultMessage.content.contains(
                    "Total due: 42",
                ),
            )
            assertTrue(
                read.entry.sourceRefs.contains(
                    sourceRef,
                ),
            )

            val escape =
                perform(
                    home,
                    """{"capability":"sources","action":"read","arguments":{"source":"$sourceRef","path":"../VexLife-Prod/config/home.json"}}""",
                )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.REFUSED,
                escape.entry.state,
            )

            val ungranted =
                perform(
                    home,
                    """{"capability":"sources","action":"read","arguments":{"source":"source.vexlife-prod.folder.sha256.ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff","path":"notes/invoice-2026.md"}}""",
                )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.NOT_FOUND,
                ungranted.entry.state,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun symlinkInsideGrantIsNeverFollowed() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-source-link-",
            )
                .toRealPath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val folder =
            Files.createDirectories(
                parent.resolve(
                    "Project",
                ),
            )
        val outside =
            Files.writeString(
                parent.resolve(
                    "secret.txt",
                ),
                "outside",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "source-link",
                lineageSuffix = "source-link",
            )
            Files.createSymbolicLink(
                folder.resolve(
                    "escape.txt",
                ),
                outside,
            )
            val sourceRef =
                (
                    VexLifeLocalSourceStore(
                        productHome = home,
                        personHome = null,
                    )
                        .add(
                            folder,
                        ) as
                        VexLifeLocalSourceStore.Outcome.Realized
                )
                    .grants
                    .single()
                    .sourceRef

            val read =
                perform(
                    home,
                    """{"capability":"sources","action":"read","arguments":{"source":"$sourceRef","path":"escape.txt"}}""",
                )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.REFUSED,
                read.entry.state,
            )
            assertFalse(
                read.resultMessage.content.contains(
                    "outside",
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
