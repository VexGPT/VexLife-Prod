package vexlife.publicentry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeProdPublicEntryArtifactsTest {
    private fun projection(): VexLifeProdPublicEntry.Projection =
        VexLifeProdPublicEntry.project(
            release =
                VexLifeProdPublicEntry.ReleaseCoordinate(
                    schemaVersion =
                        "vexlife-prod.private-service-release/v1",
                    repository =
                        "VexGPT/VexLife-Prod",
                    platform =
                        "darwin-arm64",
                    currentChannel =
                        "github-release-latest",
                    currentBootstrapAssetName =
                        "vexlife-prod-bootstrap-darwin-arm64.command",
                    currentBootstrapSha256AssetName =
                        "vexlife-prod-bootstrap-darwin-arm64.command.sha256",
                ),
            hostProfile =
                VexLifeProdPublicEntry.HostProfile(
                    platform = "darwin",
                    architecture = "arm64",
                    chipModel = "Apple M4 Pro",
                    minimumMemoryBytes =
                        12L *
                            1024L *
                            1024L *
                            1024L,
                ),
        )

    @Test
    fun generatedPublicEntryIsDeterministicAndNoEffect() {
        val first =
            VexLifeProdPublicEntryArtifacts.generate(
                projection(),
            )
        val second =
            VexLifeProdPublicEntryArtifacts.generate(
                projection(),
            )

        assertEquals(
            first,
            second,
        )
        assertEquals(
            listOf(
                "index.html",
                "vexlife.css",
                "vexlife.js",
                "public-entry.json",
            ),
            first.paths,
        )

        assertTrue(
            first.html.contains(
                "data-product-effect-class=\"NONE\"",
            ),
        )
        assertTrue(
            first.html.contains(
                "Continue with VexLife",
            ),
        )
        assertTrue(
            first.html.contains(
                "Update &amp; Restart",
            ),
        )
        assertTrue(
            first.html.contains(
                "Open Current",
            ),
        )
        assertTrue(
            first.html.contains(
                "Apple M4 Pro",
            ),
        )
        assertTrue(
            first.html.contains(
                "https://vexgpt.github.io/VexLife-Prod/",
            ),
        )
        assertTrue(
            first.html.contains(
                "https://vgong24.github.io/VexLife/",
            ),
        )
        assertTrue(
            first.manifest.contains(
                "\"effectsPerformed\": false",
            ),
        )

        assertFalse(
            first.html.contains(
                "raw.githubusercontent.com/vgong24/VexLife",
            ),
        )
        assertFalse(
            first.javascript.contains(
                "fetch(",
            ),
        )
        assertFalse(
            first.javascript.contains(
                "127.0.0.1",
            ),
        )
    }

    @Test
    fun publicPageExplainsAllEntryStatesWithoutObservingAHost() {
        val bundle =
            VexLifeProdPublicEntryArtifacts.generate(
                projection(),
            )

        listOf(
            "Already current",
            "Update available",
            "Repair needed",
            "Update + repair",
            "First setup",
            "Ownership unclear",
        )
            .forEach {
                label ->
                assertTrue(
                    bundle.html.contains(
                        label,
                    ),
                )
            }

        assertTrue(
            bundle.html.contains(
                "The public page only explains them.",
            ),
        )
        assertFalse(
            bundle.html.contains(
                "--inspect-front-door",
            ),
        )
    }
}

// [VXG RealForever]
