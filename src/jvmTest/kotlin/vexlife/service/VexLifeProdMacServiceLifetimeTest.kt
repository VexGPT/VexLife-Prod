package vexlife.service

import vexlife.browser.VexLifeProdBrowserContract
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.packaging.VexLifeProdPrivateServiceImageInstaller
import vexlife.platform.macos.MacUserLaunchdProcess

class VexLifeProdMacServiceLifetimeTest {
    private class Host(
        override val uid: Long =
            501,
    ) : MacUserLaunchdProcess.Host {
        val calls =
            mutableListOf<List<String>>()
        var servicePresent =
            false
        var running =
            false
        var alive =
            false
        var currentPid =
            4242L
        var nextBootstrapPid =
            4242L
        var printPidOverride: Long? =
            null
        var unknownPrint =
            false
        var now =
            0L

        override fun launchctl(
            args: List<String>,
        ): MacUserLaunchdProcess.CommandResult {
            calls +=
                args

            return when (
                args.first()
            ) {
                "print" ->
                    when {
                        unknownPrint ->
                            MacUserLaunchdProcess.CommandResult(
                                status = 5,
                                stdout = "",
                                stderr = "permission denied",
                            )

                        !servicePresent ->
                            MacUserLaunchdProcess.CommandResult(
                                status = 113,
                                stdout = "",
                                stderr = "Could not find service",
                            )

                        running ->
                            MacUserLaunchdProcess.CommandResult(
                                status = 0,
                                stdout =
                                    "state = running\n" +
                                        "pid = " +
                                        (
                                            printPidOverride
                                                ?: currentPid
                                        ) +
                                        "\n",
                                stderr = "",
                            )

                        else ->
                            MacUserLaunchdProcess.CommandResult(
                                status = 0,
                                stdout = "state = exited\n",
                                stderr = "",
                            )
                    }

                "bootstrap" -> {
                    servicePresent =
                        true
                    running =
                        true
                    alive =
                        true
                    currentPid =
                        nextBootstrapPid
                    printPidOverride =
                        null

                    MacUserLaunchdProcess.CommandResult(
                        status = 0,
                        stdout = "",
                        stderr = "",
                    )
                }

                "bootout" -> {
                    servicePresent =
                        false
                    running =
                        false
                    alive =
                        false
                    printPidOverride =
                        null

                    MacUserLaunchdProcess.CommandResult(
                        status = 0,
                        stdout = "",
                        stderr = "",
                    )
                }

                else ->
                    error(
                        "unexpected launchctl call: $args",
                    )
            }
        }

        override fun processAlive(
            pid: Long,
        ): Boolean =
            alive &&
                pid ==
                currentPid

        override fun nowMs(): Long =
            now

        override fun sleep(
            milliseconds: Long,
        ) {
            now +=
                milliseconds
        }

        fun bootstrapCount(): Int =
            calls.count {
                it.firstOrNull() ==
                    "bootstrap"
            }

        fun bootoutCount(): Int =
            calls.count {
                it.firstOrNull() ==
                    "bootout"
            }
    }

    private class Probe(
        var sourceSha: String,
        private val host: Host,
    ) : VexLifeProdMacServiceLifetime.SourceCurrentnessProbe {
        var available =
            true
        var pidOverride: Long? =
            null
        var state =
            "SOURCE_BOUND_LISTENING"
        var effectsPerformed =
            false
        var calls =
            0

        override fun observe(
            origin: String,
        ): VexLifeProdServiceContract.SourceCurrentness? {
            calls +=
                1

            if (
                !available
            ) {
                return null
            }

            assertEquals(
                VexLifeProdServiceContract.Origin,
                origin,
            )

            return sourceCurrentness(
                sourceSha = sourceSha,
                pid =
                    pidOverride
                        ?: host.currentPid,
                state = state,
                effectsPerformed = effectsPerformed,
            )
        }
    }

    private data class Fixture(
        val parent: Path,
        val home: Path,
        val host: Host,
        val sourceSha: String,
    ) : AutoCloseable {
        override fun close() {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun fixture(
        sourceSha: String =
            "a".repeat(
                40,
            ),
    ): Fixture {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-service-lifetime-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        bootstrapHome(
            home,
        )
        installImage(
            parent = parent,
            home = home,
            sourceSha = sourceSha,
            label = "desired",
        )

        return Fixture(
            parent = parent,
            home = home,
            host = Host(),
            sourceSha = sourceSha,
        )
    }

    private fun bootstrapHome(
        productHome: Path,
    ) {
        VexLifeProdHomeFiles.bootstrap(
            productHome = productHome,
            plan =
                VexLifeProdHome.plan(
                    personRef = VexLifeProdHome.DefaultPersonRef,
                    familyRef = VexLifeProdHome.DefaultFamilyRef,
                    deviceName = "Service Lifetime Test Mac",
                    platform = "darwin",
                    architecture = "arm64",
                    ids =
                        VexLifeProdHome.InstallationIds(
                            deviceRef = "device.vexlife-prod.service-lifetime-test",
                            companionLineageRef =
                                "companion-lineage.vexlife-prod.service-lifetime-test",
                        ),
                    createdAt = "2026-11-05T06:00:00-08:00",
                ),
        )
    }

    private fun installImage(
        parent: Path,
        home: Path,
        sourceSha: String,
        label: String,
    ): VexLifeProdPrivateServiceImageInstaller.Result {
        val source =
            parent.resolve(
                "source-image-" +
                    label,
            )
        Files.createDirectory(
            source,
        )
        formImage(
            source,
            sourceSha,
        )

        return VexLifeProdPrivateServiceImageInstaller.install(
            sourceImageRoot = source,
            productHome = home,
            expectedSourceSha = sourceSha,
        )
    }

    private fun formImage(
        root: Path,
        sourceSha: String,
    ) {
        Files.createDirectories(
            root.resolve(
                "runtime/bin",
            ),
        )
        Files.createDirectories(
            root.resolve(
                "runtime/lib",
            ),
        )
        Files.createDirectories(
            root.resolve(
                "lib",
            ),
        )
        Files.createDirectories(
            root.resolve(
                "bin",
            ),
        )
        Files.createDirectories(
            root.resolve(
                "setup",
            ),
        )

        val java =
            root.resolve(
                "runtime/bin/java",
            )
        val modules =
            root.resolve(
                "runtime/lib/modules",
            )
        val serviceLauncher =
            root.resolve(
                VexLifeProdPrivateServiceImage.LauncherRelativePath,
            )
        val setupLauncher =
            root.resolve(
                VexLifeProdPrivateServiceImage.SetupLauncherRelativePath,
            )
        val setupWindow =
            root.resolve(
                VexLifeProdPrivateServiceImage.SetupWindowRelativePath,
            )
        val app =
            root.resolve(
                "lib/vexlife-prod.jar",
            )
        val dependency =
            root.resolve(
                "lib/kotlin-stdlib.jar",
            )

        Files.writeString(
            java,
            "fake-java",
        )
        Files.writeString(
            modules,
            "fake-module-image",
        )
        Files.writeString(
            serviceLauncher,
            "#!/bin/bash\nexit 0\n",
        )
        Files.writeString(
            setupLauncher,
            "#!/bin/bash\nexit 0\n",
        )
        Files.writeString(
            setupWindow,
            "use framework \"AppKit\"\n-- test setup window\n",
        )
        Files.writeString(
            app,
            "app",
        )
        Files.writeString(
            dependency,
            "stdlib",
        )

        for (
            executable in
            listOf(
                java,
                serviceLauncher,
                setupLauncher,
            )
        ) {
            Files.setPosixFilePermissions(
                executable,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                ),
            )
        }

        val artifacts =
            listOf(
                app,
                dependency,
            )
                .sortedBy {
                    it.fileName
                        .toString()
                }
                .map {
                    file ->
                    VexLifeProdPrivateServiceImage.Artifact(
                        path =
                            "lib/" +
                                file.fileName,
                        sha256 =
                            sha256(
                                file,
                            ),
                        bytes =
                            Files.size(
                                file,
                            ),
                    )
                }

        val payload =
            VexLifeProdPrivateServiceImage.payloadFingerprint(
                root,
            )
        val manifest =
            VexLifeProdPrivateServiceImage.Manifest(
                schemaVersion = VexLifeProdPrivateServiceImage.Schema,
                truthClass = VexLifeProdPrivateServiceImage.TruthClass,
                sourceSha = sourceSha,
                platform = VexLifeProdPrivateServiceImage.Platform,
                javaRuntimeMajor = VexLifeProdPrivateServiceImage.JavaRuntimeMajor,
                mainClass = VexLifeProdPrivateServiceImage.MainClass,
                launcher = VexLifeProdPrivateServiceImage.LauncherRelativePath,
                setupLauncher = VexLifeProdPrivateServiceImage.SetupLauncherRelativePath,
                setupWindow = VexLifeProdPrivateServiceImage.SetupWindowRelativePath,
                runtimeModules =
                    listOf(
                        "java.base",
                        "java.net.http",
                        "jdk.httpserver",
                    ),
                payloadSha256 = payload,
                artifacts = artifacts,
            )

        Files.writeString(
            root.resolve(
                VexLifeProdPrivateServiceImage.ManifestRelativePath,
            ),
            VexContractJson.encode(
                manifest,
            ) +
                "\n",
            StandardCharsets.UTF_8,
        )
    }

    private fun sourceCurrentness(
        sourceSha: String,
        pid: Long,
        state: String =
            "SOURCE_BOUND_LISTENING",
        effectsPerformed: Boolean =
            false,
    ): VexLifeProdServiceContract.SourceCurrentness =
        VexLifeProdServiceContract.SourceCurrentness(
            schemaVersion = VexLifeProdServiceContract.SourceCurrentnessSchema,
            serviceRef = VexLifeProdServiceContract.ServiceRef,
            productRef = VexLifeProdServiceContract.ProductRef,
            serviceState = state,
            sourceSha = sourceSha,
            processPid = pid,
            frameHostOrigin = VexLifeProdBrowserContract.FrameHostOrigin,
            runtimeCurrentness = "NOT_REOBSERVED_BY_SERVICE",
            startupQualifiedRuntimeReceiptRef =
                "receipt.vexlife-prod.heretic.service-lifetime-test",
            currentConversationHeadSha256 = null,
            effectsPerformed = effectsPerformed,
        )

    private fun lifetimeReceipt(
        home: Path,
    ): VexLifeProdMacServiceLifetime.Receipt? =
        VexDurableContractFile.read(
            ownedRoot = home,
            path =
                VexOwnedPath.resolveInside(
                    home,
                    VexLifeProdMacServiceLifetime.ReceiptRelativePath,
                ),
        )

    @Test
    fun noReceiptAndNoStableLaunchdJobIsAbsentButPresentLabelIsHeldUnknown() {
        fixture()
            .use {
                fixture ->
                val probe =
                    Probe(
                        fixture.sourceSha,
                        fixture.host,
                    )

                val absent =
                    VexLifeProdMacServiceLifetime.observe(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )
                assertIs<VexLifeProdMacServiceLifetime.Observation.Absent>(
                    absent,
                )
                assertEquals(
                    0,
                    probe.calls,
                )

                fixture.host.servicePresent =
                    true
                fixture.host.running =
                    true
                fixture.host.alive =
                    true

                val unknown =
                    VexLifeProdMacServiceLifetime.observe(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )
                assertIs<VexLifeProdMacServiceLifetime.Observation.HeldUnknown>(
                    unknown,
                )
                assertEquals(
                    0,
                    probe.calls,
                )
            }
    }

    @Test
    fun absentServiceStartsOnlyAfterExactImageAndCommitsExactLifetimeReceipt() {
        fixture()
            .use {
                fixture ->
                val probe =
                    Probe(
                        fixture.sourceSha,
                        fixture.host,
                    )

                val started =
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )

                assertEquals(
                    "STARTED_CURRENT_SERVICE",
                    started.disposition,
                )
                assertTrue(
                    started.processStarted,
                )
                assertTrue(
                    started.receiptWritePerformed,
                )
                assertFalse(
                    started.priorProcessRetired,
                )
                assertEquals(
                    1,
                    fixture.host.bootstrapCount(),
                )
                assertEquals(
                    0,
                    fixture.host.bootoutCount(),
                )

                val receipt =
                    assertNotNull(
                        lifetimeReceipt(
                            fixture.home,
                        ),
                    )
                assertEquals(
                    fixture.sourceSha,
                    receipt.sourceSha,
                )
                assertEquals(
                    fixture.host.currentPid,
                    receipt.launchd.pid,
                )
                assertEquals(
                    emptyMap(),
                    receipt.launchd.environment,
                )
                assertFalse(
                    receipt.launchd.keepAlive,
                )
                assertTrue(
                    receipt.launchd.programPath.endsWith(
                        "/bin/vexlife-prod-service",
                    ),
                )
                assertEquals(
                    listOf(
                        "--source-sha",
                        fixture.sourceSha,
                    ),
                    receipt.launchd.programArguments,
                )
            }
    }

    @Test
    fun exactCurrentServiceReusesWithoutBootstrapBootoutOrReceiptRewrite() {
        fixture()
            .use {
                fixture ->
                val probe =
                    Probe(
                        fixture.sourceSha,
                        fixture.host,
                    )

                val first =
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )
                val receiptBefore =
                    Files.readString(
                        fixture.home.resolve(
                            VexLifeProdMacServiceLifetime.ReceiptRelativePath,
                        ),
                    )

                val second =
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )

                assertEquals(
                    "REUSED_CURRENT_SERVICE",
                    second.disposition,
                )
                assertFalse(
                    second.processStarted,
                )
                assertFalse(
                    second.priorProcessRetired,
                )
                assertFalse(
                    second.receiptWritePerformed,
                )
                assertEquals(
                    first.pid,
                    second.pid,
                )
                assertEquals(
                    1,
                    fixture.host.bootstrapCount(),
                )
                assertEquals(
                    0,
                    fixture.host.bootoutCount(),
                )
                assertEquals(
                    receiptBefore,
                    Files.readString(
                        fixture.home.resolve(
                            VexLifeProdMacServiceLifetime.ReceiptRelativePath,
                        ),
                    ),
                )
            }
    }

    @Test
    fun desiredSourceChangeRetiresOnlyExactPriorReceiptThenStartsDesiredImage() {
        fixture()
            .use {
                fixture ->
                val probe =
                    Probe(
                        fixture.sourceSha,
                        fixture.host,
                    )
                val first =
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )

                val nextSource =
                    "b".repeat(
                        40,
                    )
                installImage(
                    parent = fixture.parent,
                    home = fixture.home,
                    sourceSha = nextSource,
                    label = "next",
                )
                probe.sourceSha =
                    nextSource
                fixture.host.nextBootstrapPid =
                    5252L

                val rotated =
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = nextSource,
                        host = fixture.host,
                        probe = probe,
                    )

                assertEquals(
                    "STARTED_CURRENT_SERVICE",
                    rotated.disposition,
                )
                assertTrue(
                    rotated.priorProcessRetired,
                )
                assertEquals(
                    5252L,
                    rotated.pid,
                )
                assertEquals(
                    2,
                    fixture.host.bootstrapCount(),
                )
                assertEquals(
                    1,
                    fixture.host.bootoutCount(),
                )
                assertFalse(
                    fixture.host.processAlive(
                        first.pid,
                    ),
                    "the fake host reuses one alive flag, but the old receipt PID must no longer be current",
                )

                val receipt =
                    assertNotNull(
                        lifetimeReceipt(
                            fixture.home,
                        ),
                    )
                assertEquals(
                    nextSource,
                    receipt.sourceSha,
                )
                assertEquals(
                    5252L,
                    receipt.launchd.pid,
                )
            }
    }

    @Test
    fun launchdPidMismatchIsHeldUnknownAndNeverBootedOut() {
        fixture()
            .use {
                fixture ->
                val probe =
                    Probe(
                        fixture.sourceSha,
                        fixture.host,
                    )
                VexLifeProdMacServiceLifetime.startOrReuse(
                    productHome = fixture.home,
                    desiredSourceSha = fixture.sourceSha,
                    host = fixture.host,
                    probe = probe,
                )
                val bootoutBefore =
                    fixture.host.bootoutCount()

                fixture.host.printPidOverride =
                    9999L

                val observed =
                    VexLifeProdMacServiceLifetime.observe(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )

                assertIs<VexLifeProdMacServiceLifetime.Observation.HeldUnknown>(
                    observed,
                )

                assertFailsWith<IllegalStateException> {
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )
                }

                assertEquals(
                    bootoutBefore,
                    fixture.host.bootoutCount(),
                )
            }
    }

    @Test
    fun sameSourceWithTamperedPersistedPlistIsRestartedRatherThanReused() {
        fixture()
            .use {
                fixture ->
                val probe =
                    Probe(
                        fixture.sourceSha,
                        fixture.host,
                    )
                VexLifeProdMacServiceLifetime.startOrReuse(
                    productHome = fixture.home,
                    desiredSourceSha = fixture.sourceSha,
                    host = fixture.host,
                    probe = probe,
                )

                val receipt =
                    assertNotNull(
                        lifetimeReceipt(
                            fixture.home,
                        ),
                    )
                Files.writeString(
                    Path.of(
                        receipt.launchd.plistPath,
                    ),
                    "tampered",
                )
                fixture.host.nextBootstrapPid =
                    5252L

                val restarted =
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )

                assertEquals(
                    "STARTED_CURRENT_SERVICE",
                    restarted.disposition,
                )
                assertTrue(
                    restarted.priorProcessRetired,
                )
                assertEquals(
                    5252L,
                    restarted.pid,
                )
                assertEquals(
                    2,
                    fixture.host.bootstrapCount(),
                )
                assertEquals(
                    1,
                    fixture.host.bootoutCount(),
                )
            }
    }

    @Test
    fun httpCurrentnessMustBindExactLaunchdPidOrCurrentOwnedServiceIsRestarted() {
        fixture()
            .use {
                fixture ->
                val initialProbe =
                    Probe(
                        fixture.sourceSha,
                        fixture.host,
                    )
                VexLifeProdMacServiceLifetime.startOrReuse(
                    productHome = fixture.home,
                    desiredSourceSha = fixture.sourceSha,
                    host = fixture.host,
                    probe = initialProbe,
                )

                fixture.host.nextBootstrapPid =
                    5252L
                var calls =
                    0
                val probe =
                    VexLifeProdMacServiceLifetime.SourceCurrentnessProbe {
                        _ ->
                        calls +=
                            1
                        sourceCurrentness(
                            sourceSha = fixture.sourceSha,
                            pid =
                                if (
                                    calls ==
                                        1
                                ) {
                                    9999L
                                } else {
                                    fixture.host.currentPid
                                },
                        )
                    }

                val restarted =
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )

                assertTrue(
                    restarted.priorProcessRetired,
                )
                assertEquals(
                    5252L,
                    restarted.pid,
                )
                assertEquals(
                    2,
                    fixture.host.bootstrapCount(),
                )
                assertEquals(
                    1,
                    fixture.host.bootoutCount(),
                )
            }
    }

    @Test
    fun startupReadinessTimeoutRetiresNewProcessAndLeavesNoProductReceipt() {
        fixture()
            .use {
                fixture ->
                val probe =
                    Probe(
                        fixture.sourceSha,
                        fixture.host,
                    )
                        .also {
                            it.available =
                                false
                        }

                assertFailsWith<IllegalStateException> {
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )
                }

                assertEquals(
                    1,
                    fixture.host.bootstrapCount(),
                )
                assertEquals(
                    1,
                    fixture.host.bootoutCount(),
                )
                assertFalse(
                    fixture.host.servicePresent,
                )
                assertFalse(
                    fixture.host.alive,
                )
                assertEquals(
                    null,
                    lifetimeReceipt(
                        fixture.home,
                    ),
                )
            }
    }

    @Test
    fun corruptLifetimeReceiptIsHeldWithoutProcessMutation() {
        fixture()
            .use {
                fixture ->
                val receiptPath =
                    fixture.home.resolve(
                        VexLifeProdMacServiceLifetime.ReceiptRelativePath,
                    )
                Files.createDirectories(
                    requireNotNull(
                        receiptPath.parent,
                    ),
                )
                Files.writeString(
                    receiptPath,
                    "{}",
                )

                val observed =
                    VexLifeProdMacServiceLifetime.observe(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe =
                            Probe(
                                fixture.sourceSha,
                                fixture.host,
                            ),
                    )

                assertIs<VexLifeProdMacServiceLifetime.Observation.HeldUnknown>(
                    observed,
                )
                assertEquals(
                    0,
                    fixture.host.bootstrapCount(),
                )
                assertEquals(
                    0,
                    fixture.host.bootoutCount(),
                )
                assertEquals(
                    "{}",
                    Files.readString(
                        receiptPath,
                    ),
                )
            }
    }

    @Test
    fun receiptWriteFailureRetiresNewExactServiceInsteadOfLeavingUnownedProcess() {
        fixture()
            .use {
                fixture ->
                val receiptPath =
                    fixture.home.resolve(
                        VexLifeProdMacServiceLifetime.ReceiptRelativePath,
                    )
                val outside =
                    fixture.parent.resolve(
                        "outside-receipt-target",
                    )
                Files.writeString(
                    outside,
                    "preserve-me",
                )

                var probeCalls =
                    0
                val probe =
                    VexLifeProdMacServiceLifetime.SourceCurrentnessProbe {
                        _ ->
                        probeCalls +=
                            1
                        if (
                            probeCalls ==
                                1
                        ) {
                            Files.createSymbolicLink(
                                receiptPath,
                                outside,
                            )
                        }
                        sourceCurrentness(
                            sourceSha = fixture.sourceSha,
                            pid = fixture.host.currentPid,
                        )
                    }

                assertFailsWith<IllegalArgumentException> {
                    VexLifeProdMacServiceLifetime.startOrReuse(
                        productHome = fixture.home,
                        desiredSourceSha = fixture.sourceSha,
                        host = fixture.host,
                        probe = probe,
                    )
                }

                assertEquals(
                    1,
                    fixture.host.bootstrapCount(),
                )
                assertEquals(
                    1,
                    fixture.host.bootoutCount(),
                )
                assertFalse(
                    fixture.host.servicePresent,
                )
                assertTrue(
                    Files.isSymbolicLink(
                        receiptPath,
                    ),
                )
                assertEquals(
                    "preserve-me",
                    Files.readString(
                        outside,
                    ),
                )

                Files.deleteIfExists(
                    receiptPath,
                )
            }
    }

    private fun sha256(
        path: Path,
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )
        digest.update(
            Files.readAllBytes(
                path,
            ),
        )
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
