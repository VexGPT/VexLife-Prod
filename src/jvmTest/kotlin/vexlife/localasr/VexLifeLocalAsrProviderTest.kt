package vexlife.localasr

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.platform.VexLifePlatformRealization

class VexLifeLocalAsrProviderTest {
    @Test
    fun persistentWhisperServerBindsExactModelToLoopback() {
        val command =
            whisperCppServerCommand(
                executablePath =
                    Path.of(
                        "/vex/home/bin/whisper-server",
                    ),
                modelPath =
                    Path.of(
                        "/vex/home/models/ggml-base.bin",
                    ),
                port =
                    18427,
            )

        assertEquals(
            Path.of(
                "/vex/home/bin/whisper-server",
            ),
            Path.of(
                command.first(),
            ),
        )
        assertTrue(
            command.windowed(
                2,
            )
                .any {
                    it.firstOrNull() ==
                        "-m" &&
                        it
                            .getOrNull(
                                1,
                            )
                            ?.let(
                                Path::of,
                            ) ==
                        Path.of(
                            "/vex/home/models/ggml-base.bin",
                        )
                },
        )
        assertTrue(
            command.windowed(
                2,
            )
                .any {
                    it ==
                        listOf(
                            "--host",
                            "127.0.0.1",
                        )
                },
        )
        assertTrue(
            command.windowed(
                2,
            )
                .any {
                    it ==
                        listOf(
                            "--port",
                            "18427",
                        )
                },
        )
        assertTrue(
            command.contains(
                "-nt",
            ),
        )
    }

    @Test
    fun serverInferenceMultipartCarriesAudioLanguageAndJsonContract() {
        val audio =
            "RIFF-test-WAVE"
                .toByteArray(
                    StandardCharsets.US_ASCII,
                )
        val request =
            whisperCppServerMultipart(
                wavBytes =
                    audio,
                languageCode =
                    "zh",
                boundary =
                    "vexlife-test-boundary",
            )

        assertEquals(
            "multipart/form-data; boundary=vexlife-test-boundary",
            request.contentType,
        )
        val body =
            request.body.toString(
                StandardCharsets.ISO_8859_1,
            )
        assertTrue(
            body.contains(
                "name=\"file\"; filename=\"audio.wav\"",
            ),
        )
        assertTrue(
            body.contains(
                "Content-Type: audio/wav",
            ),
        )
        assertTrue(
            body.contains(
                "RIFF-test-WAVE",
            ),
        )
        assertTrue(
            body.contains(
                "name=\"language\"\r\n\r\nzh",
            ),
        )
        assertTrue(
            body.contains(
                "name=\"response_format\"\r\n\r\njson",
            ),
        )
        assertTrue(
            body.contains(
                "name=\"no_timestamps\"\r\n\r\ntrue",
            ),
        )
        assertTrue(
            body.contains(
                "name=\"translate\"\r\n\r\nfalse",
            ),
        )
    }

    @Test
    fun serverInferenceMultipartCanTranslateToEnglish() {
        val request =
            whisperCppServerMultipart(
                wavBytes =
                    byteArrayOf(
                        1,
                    ),
                languageCode =
                    "zh",
                translateToEnglish =
                    true,
                boundary =
                    "vexlife-test-translate",
            )

        val body =
            request.body.toString(
                StandardCharsets.ISO_8859_1,
            )
        assertTrue(
            body.contains(
                "name=\"language\"\r\n\r\nzh",
            ),
        )
        assertTrue(
            body.contains(
                "name=\"translate\"\r\n\r\ntrue",
            ),
        )
    }

    @Test
    fun serverInferenceMultipartCanRequestAutoDetection() {
        val request =
            whisperCppServerMultipart(
                wavBytes =
                    byteArrayOf(
                        1,
                    ),
                languageCode =
                    "auto",
                boundary =
                    "vexlife-test-auto",
            )

        val body =
            request.body.toString(
                StandardCharsets.ISO_8859_1,
            )
        assertTrue(
            body.contains(
                "name=\"language\"\r\n\r\nauto",
            ),
        )
    }

    @Test
    fun providerRealizationsKeepMacAndWindowsProcessMechanicsBelowOneSemanticProvider() {
        assertEquals(
            "bin/whisper-server",
            VexLifeLocalAsrMacArm64ProviderRealization.executableRelativePath,
        )
        assertFalse(
            VexLifeLocalAsrMacArm64ProviderRealization.acquisitionSupported,
        )

        val macExecutable =
            Path.of(
                    "/vex/home/bin/whisper-server",
                )
                .toAbsolutePath()
                .normalize()
        val expectedMacProviderBin =
            requireNotNull(
                macExecutable.parent,
            )
        val macProcess =
            ProcessBuilder(
                "echo",
            )
        VexLifeLocalAsrMacArm64ProviderRealization.configureProcess(
            processBuilder =
                macProcess,
            executable =
                macExecutable,
        )
        assertEquals(
            expectedMacProviderBin,
            macProcess.directory()
                .toPath()
                .toAbsolutePath()
                .normalize(),
        )
        assertEquals(
            expectedMacProviderBin.toString(),
            macProcess.environment()["DYLD_LIBRARY_PATH"],
        )

        assertEquals(
            "image/Release/whisper-server.exe",
            VexLifeLocalAsrWindowsX64ProviderRealization.executableRelativePath,
        )
        assertEquals(
            "b5454",
            VexLifeLocalAsrWindowsX64ProviderRealization.acquisition?.releaseTag,
        )
        assertEquals(
            8_928_640L,
            VexLifeLocalAsrWindowsX64ProviderRealization.acquisition?.bytes,
        )
        assertEquals(
            "6ba69e3482d7826214f90a6a9c84ca07782aec1e1d0c6a7c30c994fd5d816ccb",
            VexLifeLocalAsrWindowsX64ProviderRealization.acquisition?.sha256,
        )

        assertEquals(
            VexLifeLocalAsrMacArm64ProviderRealization,
            VexLifeLocalAsrProviderRealizationResolver.resolve(
                VexLifeLocalAsrMacArm64ProviderRealization.target,
            ),
        )
        assertEquals(
            VexLifeLocalAsrWindowsX64ProviderRealization,
            VexLifeLocalAsrProviderRealizationResolver.resolve(
                VexLifeLocalAsrWindowsX64ProviderRealization.target,
            ),
        )
    }

    @Test
    fun windowsProviderMaterializationPublishesExactPinnedArchiveInsideVexHome() {
        val home =
            Files.createTempDirectory(
                "vexlife-local-asr-provider-windows-",
            )
                .toAbsolutePath()
        try {
            val zipBytes =
                ByteArrayOutputStream()
                    .also {
                        bytes ->
                        ZipOutputStream(
                            bytes,
                        )
                            .use {
                                zip ->
                                zip.putNextEntry(
                                    ZipEntry(
                                        "Release/",
                                    ),
                                )
                                zip.closeEntry()
                                zip.putNextEntry(
                                    ZipEntry(
                                        "Release/whisper-server.exe",
                                    ),
                                )
                                zip.write(
                                    byteArrayOf(
                                        1,
                                        2,
                                        3,
                                        4,
                                    ),
                                )
                                zip.closeEntry()
                                zip.putNextEntry(
                                    ZipEntry(
                                        "Release/ggml.dll",
                                    ),
                                )
                                zip.write(
                                    byteArrayOf(
                                        5,
                                        6,
                                        7,
                                    ),
                                )
                                zip.closeEntry()
                            }
                    }
                    .toByteArray()
            val sha =
                MessageDigest.getInstance(
                    "SHA-256",
                )
                    .digest(
                        zipBytes,
                    )
                    .joinToString(
                        "",
                    ) {
                        "%02x".format(
                            it.toInt() and
                                0xff,
                        )
                    }
            val realization =
                object : VexLifeLocalAsrWhisperCppProcessRealization() {
                    override val realizationRef =
                        "realization.vexlife-prod.local-asr.whispercpp.test-x64"

                    override val target =
                        VexLifeLocalAsrWindowsX64ProviderRealization.target

                    override val providerRootRelativePath =
                        "runtime/vexlife-prod/local-asr/test-provider"

                    override val executableRelativePath =
                        "image/Release/whisper-server.exe"

                    override val sourceIdentityRelativePath =
                        "provider-source.txt"

                    override val acquisition =
                        requireNotNull(
                            VexLifeLocalAsrWindowsX64ProviderRealization.acquisition,
                        )
                            .copy(
                                url =
                                    "https://example.test/whisper-bin-x64.zip",
                                bytes =
                                    zipBytes.size.toLong(),
                                sha256 =
                                    sha,
                            )

                    override fun configureProcess(
                        processBuilder: ProcessBuilder,
                        executable: Path,
                    ) {
                        processBuilder.directory(
                            requireNotNull(
                                executable.parent,
                            )
                                .toFile(),
                        )
                    }

                    override fun executableCurrent(
                        executable: Path,
                    ): Boolean =
                        Files.isRegularFile(
                            executable,
                        )
                }
            val transport =
                DirectHttpsArtifactDelivery.Transport {
                    _,
                    resumeFrom,
                    ->
                    val offset =
                        resumeFrom.toInt()
                    DirectHttpsArtifactDelivery.Opened(
                        status =
                            if (
                                offset >
                                    0
                            ) {
                                206
                            } else {
                                200
                            },
                        contentLength =
                            (zipBytes.size - offset).toLong(),
                        contentRange =
                            if (
                                offset >
                                    0
                            ) {
                                "bytes $offset-" +
                                    (zipBytes.size - 1) +
                                    "/" +
                                    zipBytes.size
                            } else {
                                null
                            },
                        body =
                            ByteArrayInputStream(
                                zipBytes.copyOfRange(
                                    offset,
                                    zipBytes.size,
                                ),
                            ),
                    )
                }

            val before =
                VexLifeLocalAsrProviderMaterialization.observe(
                    productHome =
                        home,
                    realization =
                        realization,
                )
            assertEquals(
                VexLifeLocalAsrProviderMaterialization.State.ABSENT,
                before.state,
            )

            val result =
                VexLifeLocalAsrProviderMaterialization.ensure(
                    productHome =
                        home,
                    realization =
                        realization,
                    transport =
                        transport,
                )

            assertTrue(
                result.observation.ready,
            )
            assertTrue(
                Files.isRegularFile(
                    Path.of(
                        result.observation.executablePath,
                    ),
                ),
            )
            assertEquals(
                VexLifeLocalAsrWhisperCppProcessRealization.ProviderSourceSha,
                Files.readString(
                        Path.of(
                            result.observation.sourceIdentityPath,
                        ),
                    )
                    .trim(),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }


    @Test
    fun resolverNeverFallsBackToAnotherPlatformWhenAndroidHasNoRealizationYet() {
        val androidArm64 =
            VexLifePlatformRealization.Target(
                platform =
                    VexLifePlatformRealization.PlatformFamily.ANDROID,
                architecture =
                    VexLifePlatformRealization.Architecture.ARM64,
            )

        assertEquals(
            null,
            VexLifeLocalAsrProviderRealizationResolver.resolve(
                androidArm64,
            ),
        )
    }

}

// [VXG RealForever]
