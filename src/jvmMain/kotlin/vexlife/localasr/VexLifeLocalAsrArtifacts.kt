package vexlife.localasr

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Duration

/** Exact downloadable model artifacts for the pinned whisper.cpp provider. */
internal object VexLifeLocalAsrArtifacts {
    data class Artifact(
        val modelRef: String,
        val fileName: String,
        val bytes: Long,
        val sha256: String,
        val url: String,
    )

    data class Presence(
        val state: String,
        val presentBytes: Long,
        val observedSha256: String?,
    )

    data class Removal(
        val removedFiles: Int,
        val freedBytes: Long,
    )

    val Entries =
        listOf(
            Artifact(
                modelRef =
                    VexLifeLocalAsrSelection.WhisperBaseEnglishModelRef,
                fileName =
                    "ggml-base.en.bin",
                bytes =
                    147_964_211L,
                sha256 =
                    "a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002",
                url =
                    "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin",
            ),
            Artifact(
                modelRef =
                    VexLifeLocalAsrSelection.WhisperBaseMultilingualModelRef,
                fileName =
                    "ggml-base.bin",
                bytes =
                    147_951_465L,
                sha256 =
                    "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe",
                url =
                    "https://huggingface.co/ggerganov/whisper.cpp/resolve/5359861c739e955e79d9a303bcbc70fb988958b1/ggml-base.bin",
            ),
        )

    fun forModelRef(
        modelRef: String,
    ): Artifact =
        requireNotNull(
            Entries.firstOrNull {
                it.modelRef ==
                    modelRef
            },
        ) {
            "local ASR model artifact is not offered"
        }

    fun modelPath(
        productHome: Path,
        artifact: Artifact,
    ): Path =
        insideHome(
            productHome,
            productHome
                .resolve(
                    WhisperCppCliProvider.ProviderRootRelativePath,
                )
                .resolve(
                    "models",
                )
                .resolve(
                    artifact.fileName,
                ),
        )

    fun partialPath(
        productHome: Path,
        artifact: Artifact,
    ): Path =
        modelPath(
            productHome,
            artifact,
        )
            .resolveSibling(
                artifact.fileName +
                    ".part",
            )

    fun observe(
        productHome: Path,
        artifact: Artifact,
    ): Presence {
        val model =
            modelPath(
                productHome,
                artifact,
            )
        val partial =
            partialPath(
                productHome,
                artifact,
            )
        if (
            !Files.exists(
                model,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            val partialBytes =
                if (
                    Files.isRegularFile(
                        partial,
                        LinkOption.NOFOLLOW_LINKS,
                    )
                ) {
                    Files.size(
                        partial,
                    )
                } else {
                    0L
                }
            return Presence(
                state =
                    if (
                        partialBytes >
                        0
                    ) {
                        VexLifeLocalAsrModelsFamily.ArtifactPartial
                    } else {
                        VexLifeLocalAsrModelsFamily.ArtifactAbsent
                    },
                presentBytes =
                    partialBytes,
                observedSha256 =
                    null,
            )
        }
        if (
            !Files.isRegularFile(
                model,
                LinkOption.NOFOLLOW_LINKS,
            ) ||
            Files.isSymbolicLink(
                model,
            )
        ) {
            return Presence(
                state =
                    VexLifeLocalAsrModelsFamily.ArtifactDamaged,
                presentBytes =
                    0,
                observedSha256 =
                    null,
            )
        }
        val size =
            Files.size(
                model,
            )
        if (
            size !=
            artifact.bytes
        ) {
            return Presence(
                state =
                    VexLifeLocalAsrModelsFamily.ArtifactDamaged,
                presentBytes =
                    size,
                observedSha256 =
                    null,
            )
        }
        val observed =
            sha256(
                model,
            )
        return Presence(
            state =
                if (
                    observed ==
                    artifact.sha256
                ) {
                    VexLifeLocalAsrModelsFamily.ArtifactPresent
                } else {
                    VexLifeLocalAsrModelsFamily.ArtifactDamaged
                },
            presentBytes =
                size,
            observedSha256 =
                observed,
        )
    }

    fun acquire(
        productHome: Path,
        artifact: Artifact,
        onProgress: (Long) -> Unit,
    ) {
        val target =
            modelPath(
                productHome,
                artifact,
            )
        val partial =
            partialPath(
                productHome,
                artifact,
            )
        Files.createDirectories(
            requireNotNull(
                target.parent,
            ),
        )
        Files.deleteIfExists(
            partial,
        )

        val client =
            HttpClient
                .newBuilder()
                .followRedirects(
                    HttpClient.Redirect.ALWAYS,
                )
                .connectTimeout(
                    Duration.ofSeconds(
                        20,
                    ),
                )
                .build()
        val response =
            client.send(
                HttpRequest
                    .newBuilder(
                        URI.create(
                            artifact.url,
                        ),
                    )
                    .timeout(
                        Duration.ofMinutes(
                            10,
                        ),
                    )
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofInputStream(),
            )
        require(
            response.statusCode() in
                200..299,
        ) {
            "speech model download returned HTTP " +
                response.statusCode()
        }

        var copied =
            0L
        response.body()
            .use {
                input ->
                Files.newOutputStream(
                    partial,
                )
                    .use {
                        output ->
                        val buffer =
                            ByteArray(
                                1024 * 1024,
                            )
                        while (
                            true
                        ) {
                            val read =
                                input.read(
                                    buffer,
                                )
                            if (
                                read <
                                0
                            ) {
                                break
                            }
                            output.write(
                                buffer,
                                0,
                                read,
                            )
                            copied +=
                                read
                            require(
                                copied <=
                                    artifact.bytes,
                            ) {
                                "speech model download exceeded expected size"
                            }
                            onProgress(
                                copied,
                            )
                        }
                    }
            }

        require(
            copied ==
                artifact.bytes,
        ) {
            "speech model download size mismatch"
        }
        require(
            sha256(
                partial,
            ) ==
                artifact.sha256,
        ) {
            "speech model download digest mismatch"
        }

        try {
            Files.move(
                partial,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (
            _: AtomicMoveNotSupportedException,
        ) {
            Files.move(
                partial,
                target,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }

        require(
            observe(
                productHome,
                artifact,
            ).state ==
                VexLifeLocalAsrModelsFamily.ArtifactPresent,
        ) {
            "speech model did not become current after download"
        }
    }

    fun remove(
        productHome: Path,
        artifact: Artifact,
    ): Removal {
        var removed =
            0
        var freed =
            0L
        listOf(
            modelPath(
                productHome,
                artifact,
            ),
            partialPath(
                productHome,
                artifact,
            ),
        ).forEach {
            path ->
            if (
                Files.isRegularFile(
                    path,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                !Files.isSymbolicLink(
                    path,
                )
            ) {
                freed +=
                    Files.size(
                        path,
                    )
                if (
                    Files.deleteIfExists(
                        path,
                    )
                ) {
                    removed +=
                        1
                }
            }
        }
        return Removal(
            removedFiles =
                removed,
            freedBytes =
                freed,
        )
    }

    private fun insideHome(
        productHome: Path,
        path: Path,
    ): Path {
        val home =
            productHome
                .toAbsolutePath()
                .normalize()
        val normalized =
            path
                .toAbsolutePath()
                .normalize()
        require(
            normalized.startsWith(
                home,
            ),
        ) {
            "local ASR artifact path must remain inside Vex Home"
        }
        return normalized
    }

    private fun sha256(
        path: Path,
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )
        Files.newInputStream(
            path,
        )
            .use {
                input ->
                val buffer =
                    ByteArray(
                        1024 * 1024,
                    )
                while (
                    true
                ) {
                    val read =
                        input.read(
                            buffer,
                        )
                    if (
                        read <
                        0
                    ) {
                        break
                    }
                    digest.update(
                        buffer,
                        0,
                        read,
                    )
                }
            }
        return digest
            .digest()
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
    }
}

// [VXG RealForever]
