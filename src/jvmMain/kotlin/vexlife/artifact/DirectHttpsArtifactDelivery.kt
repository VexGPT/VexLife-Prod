package vexlife.artifact

import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

/**
 * Resumable credential-free HTTPS file delivery.
 *
 * Artifact/domain owners supply the source URL, exact expected bytes/hash, and an already-owned
 * real destination directory. This owner performs transport + partial-file mechanics only and
 * never grants acquisition or directory-topology authority by itself.
 */
object DirectHttpsArtifactDelivery {
    data class Request(
        val url: String,
        val expectedSha256: String,
        val expectedBytes: Long,
        val maxBytes: Long = expectedBytes,
    )

    data class Opened(
        val status: Int,
        val contentLength: Long?,
        val contentRange: String?,
        val body: InputStream,
    ) : AutoCloseable {
        override fun close() {
            body.close()
        }
    }

    fun interface Transport {
        fun open(
            url: URI,
            resumeFrom: Long,
        ): Opened
    }

    object JdkTransport : Transport {
        private val client =
            HttpClient
                .newBuilder()
                .followRedirects(
                    HttpClient.Redirect.NORMAL,
                )
                .build()

        override fun open(
            url: URI,
            resumeFrom: Long,
        ): Opened {
            val builder =
                HttpRequest
                    .newBuilder(
                        url,
                    )
                    .GET()

            if (
                resumeFrom >
                    0
            ) {
                builder.header(
                    "Range",
                    "bytes=$resumeFrom-",
                )
            }

            val response =
                client.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream(),
                )

            return Opened(
                status = response.statusCode(),
                contentLength =
                    response
                        .headers()
                        .firstValueAsLong(
                            "content-length",
                        )
                        .orElse(
                            -1,
                        )
                        .takeIf {
                            it >=
                                0
                        },
                contentRange =
                    response
                        .headers()
                        .firstValue(
                            "content-range",
                        )
                        .orElse(
                            null,
                        ),
                body = response.body(),
            )
        }
    }

    data class Progress(
        val bytes: Long,
        val resumedFrom: Long,
    )

    data class Result(
        val state: String,
        val disposition: String,
        val path: String,
        val bytes: Long,
        val actualSha256: String,
        val recordedSourceUrl: String,
    )

    fun download(
        request: Request,
        finalPath: Path,
        transport: Transport = JdkTransport,
        onProgress: (Progress) -> Unit = {},
    ): Result {
        validateRequest(
            request,
        )
        require(
            finalPath.isAbsolute,
        ) {
            "artifact final path must be absolute"
        }

        val final =
            finalPath.normalize()
        val uri =
            URI(
                request.url,
            )

        if (
            Files.exists(
                final,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            requireRegularFile(
                final,
                "existing artifact",
            )
            error(
                "existing artifact must be classified by its rightful artifact owner before delivery",
            )
        }

        requireRealDirectory(
            requireNotNull(
                final.parent,
            ) {
                "artifact final path requires one destination parent"
            },
            "artifact destination parent",
        )

        val partial =
            final.resolveSibling(
                final.fileName
                    .toString() +
                    ".partial",
            )

        var resumeFrom =
            0L

        if (
            Files.exists(
                partial,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            requireRegularFile(
                partial,
                "partial artifact",
            )
            resumeFrom =
                Files.size(
                    partial,
                )

            if (
                resumeFrom >
                    request.maxBytes ||
                    resumeFrom >
                    request.expectedBytes
            ) {
                Files.delete(
                    partial,
                )
                resumeFrom =
                    0
            }
        }

        if (
            resumeFrom ==
            request.expectedBytes
        ) {
            val completePartialSha256 =
                sha256File(
                    partial,
                )

            if (
                completePartialSha256 ==
                request.expectedSha256
            ) {
                moveAtomically(
                    partial,
                    final,
                )

                return Result(
                    state = "VERIFIED",
                    disposition = "RECOVERED_COMPLETE_PARTIAL_AND_VERIFIED",
                    path = final.toString(),
                    bytes = request.expectedBytes,
                    actualSha256 = completePartialSha256,
                    recordedSourceUrl =
                        sanitizeSourceUrl(
                            uri,
                        ),
                )
            }

            Files.delete(
                partial,
            )
            resumeFrom =
                0
        }

        val opened =
            transport.open(
                uri,
                resumeFrom,
            )

        opened.use {
            require(
                it.status in
                    200..299,
            ) {
                "download failed: HTTP ${it.status}"
            }

            var append =
                resumeFrom >
                    0 &&
                    it.status ==
                    206

            if (
                resumeFrom >
                    0 &&
                    it.status ==
                    206
            ) {
                require(
                    it.contentRange
                        ?.startsWith(
                            "bytes $resumeFrom-",
                        ) ==
                        true,
                ) {
                    "resume response did not start at the requested byte"
                }
            } else if (
                resumeFrom >
                    0
            ) {
                append =
                    false
                resumeFrom =
                    0
            }

            it.contentLength
                ?.let {
                    declared ->
                    require(
                        resumeFrom +
                            declared <=
                            request.maxBytes,
                    ) {
                        "declared artifact size exceeds maxBytes ${request.maxBytes}"
                    }
                    require(
                        resumeFrom +
                            declared <=
                            request.expectedBytes,
                    ) {
                        "declared artifact size exceeds expectedBytes"
                    }
                }

            val options =
                if (
                    append
                ) {
                    arrayOf(
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND,
                    )
                } else {
                    arrayOf(
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                    )
                }

            var total =
                resumeFrom

            try {
                Files.newOutputStream(
                    partial,
                    *options,
                )
                    .use {
                        output ->
                        val buffer =
                            ByteArray(
                                1024 *
                                    1024,
                            )

                        while (
                            true
                        ) {
                            val count =
                                it.body.read(
                                    buffer,
                                )
                            if (
                                count <
                                    0
                            ) {
                                break
                            }

                            total +=
                                count

                            require(
                                total <=
                                    request.maxBytes &&
                                    total <=
                                    request.expectedBytes,
                            ) {
                                "artifact exceeded admitted size"
                            }

                            output.write(
                                buffer,
                                0,
                                count,
                            )

                            onProgress(
                                Progress(
                                    bytes = total,
                                    resumedFrom = resumeFrom,
                                ),
                            )
                        }
                    }

                require(
                    total ==
                        request.expectedBytes,
                ) {
                    "artifact byte count mismatch: expected ${request.expectedBytes}, actual $total"
                }

                val actualSha256 =
                    sha256File(
                        partial,
                    )

                if (
                    actualSha256 !=
                        request.expectedSha256
                ) {
                    Files.deleteIfExists(
                        partial,
                    )
                    error(
                        "checksum mismatch: expected ${request.expectedSha256}, actual $actualSha256",
                    )
                }

                moveAtomically(
                    partial,
                    final,
                )

                return Result(
                    state = "VERIFIED",
                    disposition =
                        if (
                            resumeFrom >
                                0
                        ) {
                            "RESUMED_AND_VERIFIED"
                        } else {
                            "DOWNLOADED_AND_VERIFIED"
                        },
                    path = final.toString(),
                    bytes = total,
                    actualSha256 = actualSha256,
                    recordedSourceUrl =
                        sanitizeSourceUrl(
                            uri,
                        ),
                )
            } catch (
                error: Exception,
            ) {
                val message =
                    error.message
                        ?: ""

                if (
                    message.contains(
                        "checksum mismatch",
                    ) ||
                    message.contains(
                        "byte count mismatch",
                    ) ||
                    message.contains(
                        "exceeded admitted size",
                    )
                ) {
                    Files.deleteIfExists(
                        partial,
                    )
                }

                throw error
            }
        }
    }

    private fun moveAtomically(
        source: Path,
        target: Path,
    ) {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (
            _: AtomicMoveNotSupportedException,
        ) {
            error(
                "atomic artifact completion is unavailable for $target",
            )
        }
    }

    private fun validateRequest(
        request: Request,
    ) {
        val uri =
            URI(
                request.url,
            )

        require(
            uri.scheme ==
                "https" &&
                uri.userInfo ==
                null &&
                uri.host !=
                null,
        ) {
            "artifact URL must be credential-free HTTPS"
        }
        require(
            request.expectedSha256.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        ) {
            "artifact SHA-256 must be lowercase hexadecimal"
        }
        require(
            request.expectedBytes >
                0 &&
                request.maxBytes >
                0 &&
                request.maxBytes >=
                request.expectedBytes,
        ) {
            "artifact byte bounds must be positive and admit expectedBytes"
        }
    }

    private fun requireRealDirectory(
        path: Path,
        label: String,
    ) {
        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        require(
            attributes.isDirectory &&
                !attributes.isSymbolicLink,
        ) {
            "$label must be one real non-symlink directory"
        }
    }

    private fun requireRegularFile(
        path: Path,
        label: String,
    ) {
        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        require(
            attributes.isRegularFile &&
                !attributes.isSymbolicLink,
        ) {
            "$label must be one regular non-symlink file"
        }
    }

    private fun sha256File(
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
                        1024 *
                            1024,
                    )

                while (
                    true
                ) {
                    val count =
                        input.read(
                            buffer,
                        )
                    if (
                        count <
                            0
                    ) {
                        break
                    }
                    digest.update(
                        buffer,
                        0,
                        count,
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

    private fun sanitizeSourceUrl(
        uri: URI,
    ): String =
        URI(
            uri.scheme,
            null,
            uri.host,
            uri.port,
            uri.path,
            null,
            null,
        )
            .toString()
}

// [VXG RealForever]
