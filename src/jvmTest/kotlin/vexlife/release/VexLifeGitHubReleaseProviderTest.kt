package vexlife.release

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VexLifeGitHubReleaseProviderTest {
    private fun release(
        tag: String,
        prerelease: Boolean,
        sourceSha: String,
        publishedAt: String,
        htmlUrl: String =
            "https://github.com/VexGPT/VexLife-Prod/releases/tag/$tag",
        assetName: String =
            VexLifeGitHubReleaseProvider.MacDiskImageAssetName,
    ): String =
        """
        {
          "tag_name": "$tag",
          "draft": false,
          "prerelease": $prerelease,
          "published_at": "$publishedAt",
          "html_url": "$htmlUrl",
          "body": "VexLife release\n\nsource-sha: $sourceSha\n",
          "assets": [
            {"name": "$assetName"}
          ]
        }
        """
            .trimIndent()

    @Test
    fun previewAndStableWireBecomeTypedPublishedReleaseTruth() {
        val previewSha =
            "a".repeat(
                40,
            )
        val stableSha =
            "b".repeat(
                40,
            )
        val provider =
            VexLifeGitHubReleaseProvider(
                VexLifeGitHubReleaseProvider.Transport {
                    VexLifeGitHubReleaseProvider.Response(
                        status = 200,
                        body =
                            """
                            [
                              ${release("private-service-image-$previewSha", true, previewSha, "2026-11-17T08:00:00Z")},
                              ${release("v0.5.0", false, stableSha, "2026-11-18T08:00:00Z")}
                            ]
                            """
                                .trimIndent(),
                    )
                },
            )

        val current =
            assertIs<VexLifePublishedReleaseObservation.State.Current>(
                provider.observe(
                    previous =
                        VexLifePublishedReleaseObservation.State.Unobserved,
                    observedAtEpochMillis = 300,
                ),
            )

        assertEquals(
            2,
            current.snapshot.releases.size,
        )
        assertIs<VexLifeReleaseIdentity.Value.Stable>(
            current.snapshot.releases[
                0
            ].identity,
        )
        assertIs<VexLifeReleaseIdentity.Value.Preview>(
            current.snapshot.releases[
                1
            ].identity,
        )
    }

    @Test
    fun unrelatedOrNonMacReleaseDoesNotBecomeMacUpdateTruth() {
        val source =
            "a".repeat(
                40,
            )
        val body =
            """
            [
              ${release("other-tag", false, source, "2026-11-17T08:00:00Z")},
              ${release("v0.5.0", false, source, "2026-11-17T09:00:00Z", assetName = "VexLife-Windows-x64.zip")}
            ]
            """
                .trimIndent()

        val snapshot =
            VexLifeGitHubReleaseProvider(
                VexLifeGitHubReleaseProvider.Transport {
                    VexLifeGitHubReleaseProvider.Response(
                        200,
                        body,
                    )
                },
            )
                .snapshot(
                    body = body,
                    observedAtEpochMillis = 300,
                )

        assertEquals(
            emptyList(),
            snapshot.releases,
        )
    }

    @Test
    fun contradictoryPreviewSourceFailsClosedAsUnavailable() {
        val tagSource =
            "a".repeat(
                40,
            )
        val bodySource =
            "b".repeat(
                40,
            )
        val provider =
            VexLifeGitHubReleaseProvider(
                VexLifeGitHubReleaseProvider.Transport {
                    VexLifeGitHubReleaseProvider.Response(
                        status = 200,
                        body =
                            """
                            [
                              ${release("private-service-image-$tagSource", true, bodySource, "2026-11-17T08:00:00Z")}
                            ]
                            """
                                .trimIndent(),
                    )
                },
            )

        assertIs<VexLifePublishedReleaseObservation.State.Unavailable>(
            provider.observe(
                previous =
                    VexLifePublishedReleaseObservation.State.Unobserved,
                observedAtEpochMillis = 300,
            ),
        )
    }

    @Test
    fun transportFailureRetainsPriorSuccessfulSnapshot() {
        val source =
            "a".repeat(
                40,
            )
        val prior =
            VexLifePublishedReleaseObservation.Snapshot(
                releases =
                    listOf(
                        VexLifeReleaseIdentity.Published(
                            identity =
                                VexLifeReleaseIdentity.Value.Preview(
                                    VexLifeReleaseIdentity.SourceSha(
                                        source,
                                    ),
                                ),
                            publishedAtEpochMillis = 100,
                            page =
                                VexLifeReleaseIdentity.HttpsPage(
                                    "https://github.com/VexGPT/VexLife-Prod/releases/tag/private-service-image-$source",
                                ),
                        ),
                    ),
                observedAtEpochMillis = 200,
            )
        val provider =
            VexLifeGitHubReleaseProvider(
                VexLifeGitHubReleaseProvider.Transport {
                    error(
                        "offline",
                    )
                },
            )

        val unavailable =
            assertIs<VexLifePublishedReleaseObservation.State.Unavailable>(
                provider.observe(
                    previous =
                        VexLifePublishedReleaseObservation.State.Current(
                            prior,
                        ),
                    observedAtEpochMillis = 300,
                ),
            )

        assertTrue(
            unavailable.priorSnapshot ===
                prior,
        )
    }
}

// [VXG RealForever]
