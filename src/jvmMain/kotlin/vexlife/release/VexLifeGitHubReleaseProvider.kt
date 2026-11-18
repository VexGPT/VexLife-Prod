package vexlife.release

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * Read-only provider for intentionally published Mac VexLife releases.
 *
 * The fixed request carries no Home, device, person, Conversation, model or analytics identity.
 * GitHub's wire representation is converted into Product-owned release identity here and nowhere
 * else.
 */
class VexLifeGitHubReleaseProvider(
    private val transport: Transport =
        JavaTransport(),
) {
    data class Response(
        val status: Int,
        val body: String,
    )

    fun interface Transport {
        fun get(): Response
    }

    fun observe(
        previous: VexLifePublishedReleaseObservation.State,
        observedAtEpochMillis: Long =
            System.currentTimeMillis(),
    ): VexLifePublishedReleaseObservation.State {
        require(
            observedAtEpochMillis >=
                0,
        ) {
            "published-release observation time must be non-negative"
        }

        val priorSnapshot =
            when (previous) {
                is VexLifePublishedReleaseObservation.State.Current ->
                    previous.snapshot

                is VexLifePublishedReleaseObservation.State.Unavailable ->
                    previous.priorSnapshot

                VexLifePublishedReleaseObservation.State.Unobserved ->
                    null
            }

        return try {
            val response =
                transport.get()
            require(
                response.status ==
                    200,
            ) {
                "GitHub Releases observation returned HTTP ${response.status}"
            }
            require(
                response.body
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
                    .size <=
                    MaximumResponseBytes,
            ) {
                "GitHub Releases observation exceeded the bounded response size"
            }

            VexLifePublishedReleaseObservation.State.Current(
                snapshot(
                    body = response.body,
                    observedAtEpochMillis = observedAtEpochMillis,
                ),
            )
        } catch (
            error: Exception,
        ) {
            if (
                error is
                    InterruptedException
            ) {
                Thread
                    .currentThread()
                    .interrupt()
            }
            VexLifePublishedReleaseObservation.State.Unavailable(
                attemptedAtEpochMillis = observedAtEpochMillis,
                priorSnapshot = priorSnapshot,
            )
        }
    }

    internal fun snapshot(
        body: String,
        observedAtEpochMillis: Long,
    ): VexLifePublishedReleaseObservation.Snapshot {
        val root =
            Json
                .parseToJsonElement(
                    body,
                ) as?
                JsonArray
                ?: error(
                    "GitHub Releases response must be one JSON array",
                )

        val releases =
            root
                .mapNotNull(
                    ::publishedReleaseOrNull,
                )
                .sortedByDescending(
                    VexLifeReleaseIdentity.Published::publishedAtEpochMillis,
                )

        return VexLifePublishedReleaseObservation.Snapshot(
            releases = releases,
            observedAtEpochMillis = observedAtEpochMillis,
        )
    }

    private fun publishedReleaseOrNull(
        element: JsonElement,
    ): VexLifeReleaseIdentity.Published? {
        val release =
            element as?
                JsonObject
                ?: error(
                    "GitHub Releases response contains a non-object release",
                )

        if (
            release.requiredBoolean(
                "draft",
            )
        ) {
            return null
        }

        val tag =
            release.requiredString(
                "tag_name",
            )
        val stable =
            StableTag.matchEntire(
                tag,
            )
        val preview =
            PreviewTag.matchEntire(
                tag,
            )
        if (
            stable ==
                null &&
            preview ==
                null
        ) {
            return null
        }

        val prerelease =
            release.requiredBoolean(
                "prerelease",
            )
        if (
            stable !=
                null &&
            prerelease
        ) {
            return null
        }
        require(
            preview ==
                null ||
                prerelease,
        ) {
            "published VexLife Preview tag is not marked prerelease"
        }

        val assets =
            release.requiredArray(
                "assets",
            )
        val carriesMacApplication =
            assets.any {
                asset ->
                (
                    asset as?
                        JsonObject
                )
                    ?.requiredString(
                        "name",
                    ) ==
                    MacDiskImageAssetName
            }
        if (
            !carriesMacApplication
        ) {
            return null
        }

        val body =
            release.optionalString(
                "body",
            )
                .orEmpty()
        val sourceMatches =
            SourceShaLine
                .findAll(
                    body,
                )
                .map {
                    it.groupValues[
                        1
                    ]
                }
                .toList()
        require(
            sourceMatches.size ==
                1,
        ) {
            "published VexLife release must carry exactly one source-sha line"
        }
        val sourceSha =
            VexLifeReleaseIdentity.SourceSha(
                sourceMatches.single(),
            )

        val identity =
            when {
                stable !=
                    null ->
                    VexLifeReleaseIdentity.Value.Stable(
                        version =
                            VexLifeReleaseIdentity.SemanticVersion(
                                major =
                                    stable.groupValues[
                                        1
                                    ]
                                        .toInt(),
                                minor =
                                    stable.groupValues[
                                        2
                                    ]
                                        .toInt(),
                                patch =
                                    stable.groupValues[
                                        3
                                    ]
                                        .toInt(),
                            ),
                        sourceSha = sourceSha,
                    )

                preview !=
                    null -> {
                    val tagSource =
                        VexLifeReleaseIdentity.SourceSha(
                            preview.groupValues[
                                1
                            ],
                        )
                    require(
                        tagSource ==
                            sourceSha,
                    ) {
                        "published VexLife Preview tag/source-sha identity mismatch"
                    }
                    VexLifeReleaseIdentity.Value.Preview(
                        sourceSha = sourceSha,
                    )
                }

                else ->
                    error(
                        "recognized VexLife release tag lost its identity",
                    )
            }

        val publishedAt =
            Instant
                .parse(
                    release.requiredString(
                        "published_at",
                    ),
                )
                .toEpochMilli()

        // GitHub owns publication provenance, but the human View action stays on the Vex-owned
        // public download surface so it remains usable inside VexLife's nested Browser.
        validateCanonicalReleasePage(
            release.requiredString(
                "html_url",
            ),
        )

        return VexLifeReleaseIdentity.Published(
            identity = identity,
            publishedAtEpochMillis = publishedAt,
            page =
                VexLifeReleaseIdentity.HttpsPage(
                    PublicDownloadPage,
                ),
        )
    }

    private fun validateCanonicalReleasePage(
        value: String,
    ) {
        val uri =
            URI(
                value,
            )
        require(
            uri.scheme ==
                "https" &&
                uri.host ==
                "github.com" &&
                uri.port ==
                -1 &&
                uri.userInfo ==
                null &&
                uri.query ==
                null &&
                uri.fragment ==
                null &&
                uri.path.startsWith(
                    ReleasePagePathPrefix,
                ),
        ) {
            "published VexLife release page escaped the canonical GitHub repository"
        }

    }

    private fun JsonObject.requiredString(
        name: String,
    ): String =
        (
            this[
                name
            ] as?
                JsonPrimitive
        )
            ?.contentOrNull
            ?.takeIf(
                String::isNotBlank,
            )
            ?: error(
                "GitHub Releases field '$name' is missing or invalid",
            )

    private fun JsonObject.optionalString(
        name: String,
    ): String? =
        (
            this[
                name
            ] as?
                JsonPrimitive
        )
            ?.contentOrNull

    private fun JsonObject.requiredBoolean(
        name: String,
    ): Boolean =
        (
            this[
                name
            ] as?
                JsonPrimitive
        )
            ?.booleanOrNull
            ?: error(
                "GitHub Releases field '$name' is missing or invalid",
            )

    private fun JsonObject.requiredArray(
        name: String,
    ): JsonArray =
        this[
            name
        ] as?
            JsonArray
            ?: error(
                "GitHub Releases field '$name' is missing or invalid",
            )

    private class JavaTransport :
        Transport {
        private val client =
            HttpClient
                .newBuilder()
                .connectTimeout(
                    Duration.ofSeconds(
                        3,
                    ),
                )
                .followRedirects(
                    HttpClient.Redirect.NEVER,
                )
                .build()

        override fun get(): Response {
            val request =
                HttpRequest
                    .newBuilder(
                        ReleasesUri,
                    )
                    .timeout(
                        Duration.ofSeconds(
                            4,
                        ),
                    )
                    .header(
                        "accept",
                        "application/vnd.github+json",
                    )
                    .header(
                        "user-agent",
                        "VexLife-Prod-release-observer",
                    )
                    .GET()
                    .build()
            val response =
                client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8,
                    ),
                )

            return Response(
                status = response.statusCode(),
                body = response.body(),
            )
        }
    }

    companion object {
        internal val ReleasesUri: URI =
            URI.create(
                "https://api.github.com/repos/VexGPT/VexLife-Prod/releases?per_page=100",
            )

        internal const val MacDiskImageAssetName =
            "VexLife-macOS-AppleSilicon.dmg"

        internal const val PublicDownloadPage =
            "https://vexgpt.github.io/VexLife-Prod/#download"

        private const val MaximumResponseBytes =
            2 * 1024 * 1024

        private const val ReleasePagePathPrefix =
            "/VexGPT/VexLife-Prod/releases/tag/"

        private val StableTag =
            Regex(
                "^v([0-9]+)\\.([0-9]+)\\.([0-9]+)$",
            )

        private val PreviewTag =
            Regex(
                "^private-service-image-([0-9a-f]{40})$",
            )

        private val SourceShaLine =
            Regex(
                "(?m)^source-sha:\\s*([0-9a-f]{40})\\s*$",
            )
    }
}

// [VXG RealForever]
