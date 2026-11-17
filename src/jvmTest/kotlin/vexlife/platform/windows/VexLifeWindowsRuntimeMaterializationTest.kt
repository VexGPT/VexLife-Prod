package vexlife.platform.windows

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticRuntimeRealization

class VexLifeWindowsRuntimeMaterializationTest {
    private data class Fixture(
        val home: Path,
        val realization: HereticRuntimeRealization.Value,
        val modelBytes: ByteArray,
        val archiveBytes: ByteArray,
        val serverBytes: ByteArray,
        val helperBytes: ByteArray,
    )

    private class MemoryTransport(
        private val payloads: Map<String, ByteArray>,
    ) : DirectHttpsArtifactDelivery.Transport {
        data class Call(
            val url: String,
            val resumeFrom: Long,
        )

        val calls = mutableListOf<Call>()

        override fun open(
            url: URI,
            resumeFrom: Long,
        ): DirectHttpsArtifactDelivery.Opened {
            calls +=
                Call(
                    url = url.toString(),
                    resumeFrom = resumeFrom,
                )

            val payload =
                requireNotNull(
                    payloads[url.toString()],
                ) {
                    "unexpected test URL: $url"
                }
            require(
                resumeFrom in 0..payload.size.toLong(),
            ) {
                "invalid test resume offset"
            }
            val remaining =
                payload.copyOfRange(
                    resumeFrom.toInt(),
                    payload.size,
                )

            return DirectHttpsArtifactDelivery.Opened(
                status =
                    if (resumeFrom > 0) {
                        206
                    } else {
                        200
                    },
                contentLength = remaining.size.toLong(),
                contentRange =
                    if (resumeFrom > 0) {
                        "bytes " +
                            resumeFrom +
                            "-" +
                            (payload.size - 1) +
                            "/" +
                            payload.size
                    } else {
                        null
                    },
                body = ByteArrayInputStream(remaining),
            )
        }
    }

    @Test
    fun exactDependenciesMaterializeAndThenReuseWithoutEffects() {
        val fixture = fixture()
        val transport =
            MemoryTransport(
                payloads(fixture),
            )

        val before =
            VexLifeWindowsRuntimeMaterialization.observe(
                fixture.home,
                fixture.realization,
            )
        assertFalse(before.ready)

        assertFailsWith<IllegalArgumentException> {
            VexLifeWindowsRuntimeMaterialization.ensure(
                productHome = fixture.home,
                allowAcquisition = false,
                realization = fixture.realization,
                transport = transport,
            )
        }
        assertEquals(
            emptyList(),
            transport.calls,
        )

        val first =
            VexLifeWindowsRuntimeMaterialization.ensure(
                productHome = fixture.home,
                allowAcquisition = true,
                realization = fixture.realization,
                transport = transport,
            )

        assertTrue(first.observation.ready)
        assertTrue(first.runtimeExtractionPerformed)
        assertEquals(
            "DOWNLOADED_AND_VERIFIED",
            first.modelDisposition,
        )
        assertEquals(
            "DOWNLOADED_AND_VERIFIED",
            first.runtimeArchiveDisposition,
        )
        assertContentEquals(
            fixture.modelBytes,
            Files.readAllBytes(
                Path.of(
                    first.observation.modelPath,
                ),
            ),
        )
        assertContentEquals(
            fixture.serverBytes,
            Files.readAllBytes(
                Path.of(
                    first.observation.runtimeExecutablePath,
                ),
            ),
        )
        assertContentEquals(
            fixture.helperBytes,
            Files.readAllBytes(
                Path.of(
                        first.observation.runtimeImagePath,
                    )
                    .resolve(
                        "lib/helper.dll",
                    ),
            ),
        )

        val callCount =
            transport.calls.size
        val second =
            VexLifeWindowsRuntimeMaterialization.ensure(
                productHome = fixture.home,
                allowAcquisition = false,
                realization = fixture.realization,
                transport = transport,
            )

        assertTrue(second.observation.ready)
        assertFalse(second.runtimeExtractionPerformed)
        assertEquals(
            "REUSED_VERIFIED_MODEL",
            second.modelDisposition,
        )
        assertEquals(
            "REUSED_VERIFIED_RUNTIME_ARCHIVE",
            second.runtimeArchiveDisposition,
        )
        assertEquals(
            callCount,
            transport.calls.size,
        )
    }

    @Test
    fun partialModelDownloadResumesThroughTheSharedTransport() {
        val fixture =
            fixture()
        val before =
            VexLifeWindowsRuntimeMaterialization.observe(
                fixture.home,
                fixture.realization,
            )
        val modelPath =
            Path.of(
                before.modelPath,
            )
        Files.createDirectories(
            requireNotNull(
                modelPath.parent,
            ),
        )
        val prefix =
            fixture.modelBytes.copyOfRange(
                0,
                4,
            )
        Files.write(
            modelPath.resolveSibling(
                modelPath.fileName.toString() +
                    ".partial",
            ),
            prefix,
        )

        val transport =
            MemoryTransport(
                payloads(fixture),
            )
        val result =
            VexLifeWindowsRuntimeMaterialization.ensure(
                productHome = fixture.home,
                allowAcquisition = true,
                realization = fixture.realization,
                transport = transport,
            )

        assertTrue(result.observation.ready)
        assertTrue(
            transport.calls.any {
                it.url ==
                    requireNotNull(
                        fixture.realization.artifact.directHttpsUrl,
                    ) &&
                    it.resumeFrom ==
                    prefix.size.toLong()
            },
        )
        assertEquals(
            "RESUMED_AND_VERIFIED",
            result.modelDisposition,
        )
    }

    @Test
    fun invalidExistingModelIsHeldAndNeverOverwritten() {
        val fixture =
            fixture()
        val observation =
            VexLifeWindowsRuntimeMaterialization.observe(
                fixture.home,
                fixture.realization,
            )
        val modelPath =
            Path.of(
                observation.modelPath,
            )
        Files.createDirectories(
            requireNotNull(
                modelPath.parent,
            ),
        )
        val wrong =
            "wrong".toByteArray()
        Files.write(
            modelPath,
            wrong,
        )

        val transport =
            MemoryTransport(
                payloads(fixture),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWindowsRuntimeMaterialization.ensure(
                productHome = fixture.home,
                allowAcquisition = true,
                realization = fixture.realization,
                transport = transport,
            )
        }
        assertContentEquals(
            wrong,
            Files.readAllBytes(modelPath),
        )
        assertEquals(
            emptyList(),
            transport.calls,
        )
    }

    @Test
    fun unsafeArchivePathIsRejectedBeforeRuntimeImagePublication() {
        val modelBytes =
            "model".toByteArray()
        val serverBytes =
            "server".toByteArray()
        val unsafeArchive =
            zip(
                mapOf(
                    "../escape.dll" to "escape".toByteArray(),
                    "llama-server.exe" to serverBytes,
                ),
            )
        val fixture =
            fixture(
                modelBytes = modelBytes,
                archiveBytes = unsafeArchive,
                serverBytes = serverBytes,
            )
        val transport =
            MemoryTransport(
                payloads(fixture),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWindowsRuntimeMaterialization.ensure(
                productHome = fixture.home,
                allowAcquisition = true,
                realization = fixture.realization,
                transport = transport,
            )
        }

        assertFalse(
            Files.exists(
                fixture.home.resolve(
                    "escape.dll",
                ),
            ),
        )
        assertFalse(
            Files.exists(
                Path.of(
                    VexLifeWindowsRuntimeMaterialization
                        .observe(
                            fixture.home,
                            fixture.realization,
                        )
                        .runtimeImagePath,
                ),
            ),
        )
    }

    private fun fixture(
        modelBytes: ByteArray =
            "small-test-model".toByteArray(),
        serverBytes: ByteArray =
            "small-test-server".toByteArray(),
        helperBytes: ByteArray =
            "small-test-helper".toByteArray(),
        archiveBytes: ByteArray? =
            null,
    ): Fixture {
        val parent =
            Files
                .createTempDirectory(
                    "vexlife-windows-runtime-materialization-",
                )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        CanonicalVexLifeHomeFixture.form(
            home,
        )

        val actualArchive =
            archiveBytes
                ?: zip(
                    mapOf(
                        "lib/" to null,
                        "lib/helper.dll" to helperBytes,
                        "llama-server.exe" to serverBytes,
                    ),
                )

        val base =
            HereticRuntimeRealization.WindowsQwen4B
        val realization =
            base.copy(
                realizationRef =
                    "realization.vexlife-prod.test.windows-materialization.001",
                artifact =
                    base.artifact.copy(
                        expectedPrimaryBytes =
                            modelBytes.size.toLong(),
                        contentSetSha256 =
                            sha256(modelBytes),
                        materializationRootRelativePath =
                            "runtime-tests/windows/model",
                        directHttpsUrl =
                            "https://example.test/model.gguf",
                    ),
                runtimeBinary =
                    requireNotNull(
                        base.runtimeBinary,
                    )
                        .copy(
                            archiveFilename =
                                "runtime.zip",
                            archiveBytes =
                                actualArchive.size.toLong(),
                            archiveSha256 =
                                sha256(actualArchive),
                            archiveUrl =
                                "https://example.test/runtime.zip",
                            executableRelativePath =
                                "llama-server.exe",
                            executableSha256 =
                                sha256(serverBytes),
                            materializationRootRelativePath =
                                "runtime-tests/windows/runtime",
                        ),
            )

        return Fixture(
            home = home,
            realization = realization,
            modelBytes = modelBytes,
            archiveBytes = actualArchive,
            serverBytes = serverBytes,
            helperBytes = helperBytes,
        )
    }

    private fun payloads(
        fixture: Fixture,
    ): Map<String, ByteArray> =
        mapOf(
            requireNotNull(
                fixture.realization.artifact.directHttpsUrl,
            ) to fixture.modelBytes,
            requireNotNull(
                fixture.realization.runtimeBinary,
            ).archiveUrl to fixture.archiveBytes,
        )

    private fun zip(
        entries: Map<String, ByteArray?>,
    ): ByteArray {
        val bytes =
            ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach {
                (
                    name,
                    body,
                ),
                ->
                zip.putNextEntry(
                    ZipEntry(
                        name,
                    ),
                )
                if (body != null) {
                    zip.write(
                        body,
                    )
                }
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private fun sha256(
        bytes: ByteArray,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(bytes)
            .joinToString("") {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
