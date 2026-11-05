package vexlife.platform.macos

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MacUserLaunchdProcessTest {
    private class FakeHost(
        override val uid: Long = 501,
        private val pid: Long = 4242,
    ) : MacUserLaunchdProcess.Host {
        val calls = mutableListOf<List<String>>()
        var servicePresent = false
        var running = false
        var alive = false
        var now = 0L
        var bootstrapFailure = false
        var unknownPrint = false

        override fun launchctl(
            args: List<String>,
        ): MacUserLaunchdProcess.CommandResult {
            calls += args
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
                                stdout = "state = running\npid = $pid\n",
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
                    if (
                        bootstrapFailure
                    ) {
                        MacUserLaunchdProcess.CommandResult(
                            status = 5,
                            stdout = "",
                            stderr = "bootstrap failed",
                        )
                    } else {
                        servicePresent = true
                        running = true
                        alive = true
                        MacUserLaunchdProcess.CommandResult(
                            status = 0,
                            stdout = "",
                            stderr = "",
                        )
                    }
                }

                "bootout" -> {
                    servicePresent = false
                    running = false
                    alive = false
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
                this.pid

        override fun nowMs(): Long =
            now

        override fun sleep(
            milliseconds: Long,
        ) {
            now +=
                milliseconds
        }
    }

    private fun fixture(): Pair<MacUserLaunchdProcess.Spec, java.nio.file.Path> {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-launchd-",
            )
        Files.createDirectory(
            root.resolve(
                "launchd",
            ),
        )
        Files.createDirectory(
            root.resolve(
                "runtime",
            ),
        )

        val spec =
            MacUserLaunchdProcess.form(
                label = "com.vexlife.prod.presentation",
                plistPath =
                    root
                        .resolve(
                            "launchd/presentation.plist",
                        )
                        .toString(),
                programPath = "/usr/bin/env",
                programArguments =
                    listOf(
                        "node",
                        "server.mjs",
                    ),
                workingDirectory =
                    root.toString(),
                stdoutPath =
                    root
                        .resolve(
                            "runtime/stdout.log",
                        )
                        .toString(),
                stderrPath =
                    root
                        .resolve(
                            "runtime/stderr.log",
                        )
                        .toString(),
                environment =
                    mapOf(
                        "VEXLIFE_PROD_TEST" to
                            "true",
                    ),
                uid = 501,
            )
        return spec to
            root
    }

    @Test
    fun specificationAndPlistKeepLifetimeMeaningExplicit() {
        val (
            spec,
            root,
        ) =
            fixture()

        try {
            assertFalse(
                spec.keepAlive,
            )
            assertEquals(
                "gui/501",
                spec.domainTarget,
            )

            val plist =
                MacUserLaunchdProcess.renderPlist(
                    spec,
                )

            assertTrue(
                plist.contains(
                    "<key>RunAtLoad</key>\n  <true/>",
                ),
            )
            assertTrue(
                plist.contains(
                    "<key>KeepAlive</key>\n  <false/>",
                ),
            )
            assertTrue(
                plist.contains(
                    "<string>node</string>",
                ),
            )
            assertTrue(
                plist.contains(
                    "<key>VEXLIFE_PROD_TEST</key>",
                ),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun startFormsOneExactReceiptAfterLaunchdReportsRunning() {
        val (
            spec,
            root,
        ) =
            fixture()
        val host =
            FakeHost()

        try {
            val receipt =
                MacUserLaunchdProcess.start(
                    spec,
                    host,
                )

            assertEquals(
                4242,
                receipt.pid,
            )
            assertFalse(
                receipt.keepAlive,
            )
            assertEquals(
                spec.environment,
                receipt.environment,
            )
            assertTrue(
                Files.exists(
                    java.nio.file.Path.of(
                        receipt.plistPath,
                    ),
                ),
            )
            assertTrue(
                host.calls.any {
                    it.first() ==
                        "bootstrap"
                },
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun startRefusesToReplaceAnyAlreadyPresentLaunchdJob() {
        val (
            spec,
            root,
        ) =
            fixture()
        val host =
            FakeHost()
                .also {
                    it.servicePresent = true
                    it.running = true
                    it.alive = true
                }

        try {
            assertFailsWith<IllegalArgumentException> {
                MacUserLaunchdProcess.start(
                    spec,
                    host,
                )
            }
            assertFalse(
                host.calls.any {
                    it.first() ==
                        "bootstrap"
                },
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun bootstrapFailureRemovesTheUnownedPlist() {
        val (
            spec,
            root,
        ) =
            fixture()
        val host =
            FakeHost()
                .also {
                    it.bootstrapFailure = true
                }

        try {
            assertFailsWith<IllegalStateException> {
                MacUserLaunchdProcess.start(
                    spec,
                    host,
                )
            }
            assertFalse(
                Files.exists(
                    java.nio.file.Path.of(
                        spec.plistPath,
                    ),
                ),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun receiptCurrentRequiresExactPersistedPlistButRetirementStillUsesDurableReceipt() {
        val (
            spec,
            root,
        ) =
            fixture()
        val host =
            FakeHost()

        try {
            val receipt =
                MacUserLaunchdProcess.start(
                    spec,
                    host,
                )

            assertTrue(
                MacUserLaunchdProcess.receiptCurrent(
                    receipt = receipt,
                    host = host,
                ),
            )

            Files.writeString(
                java.nio.file.Path.of(
                    receipt.plistPath,
                ),
                "tampered",
            )

            assertFalse(
                MacUserLaunchdProcess.receiptCurrent(
                    receipt = receipt,
                    host = host,
                ),
            )

            val retired =
                MacUserLaunchdProcess.retire(
                    receipt = receipt,
                    host = host,
                )

            assertTrue(
                retired.exactOwnershipVerified,
            )
            assertTrue(
                retired.verifiedNotLive,
            )
            assertFalse(
                host.alive,
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun retirementRejectsPidDriftBeforeBootout() {
        val (
            spec,
            root,
        ) =
            fixture()
        val host =
            FakeHost()
        try {
            val receipt =
                MacUserLaunchdProcess.start(
                    spec,
                    host,
                )

            assertFailsWith<IllegalStateException> {
                MacUserLaunchdProcess.retire(
                    receipt = receipt,
                    host = host,
                    expectedPid = 9999,
                )
            }

            assertFalse(
                host.calls.any {
                    it.first() ==
                        "bootout"
                },
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun retirementBootsOutOnlyTheExactOwnedJobAndVerifiesNotLive() {
        val (
            spec,
            root,
        ) =
            fixture()
        val host =
            FakeHost()

        try {
            val receipt =
                MacUserLaunchdProcess.start(
                    spec,
                    host,
                )
            val retired =
                MacUserLaunchdProcess.retire(
                    receipt,
                    host,
                )

            assertTrue(
                retired.exactOwnershipVerified,
            )
            assertTrue(
                retired.verifiedNotLive,
            )
            assertEquals(
                "LAUNCHD_BOOTOUT",
                retired.signalSent,
            )
            assertFalse(
                Files.exists(
                    java.nio.file.Path.of(
                        receipt.plistPath,
                    ),
                ),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun unknownObservationNeverBecomesAbsenceOrOwnership() {
        val (
            spec,
            root,
        ) =
            fixture()
        val host =
            FakeHost()
                .also {
                    it.unknownPrint = true
                }

        try {
            val observed =
                MacUserLaunchdProcess.observe(
                    spec,
                    host,
                )
            assertTrue(
                observed is
                    MacUserLaunchdProcess.Observation.Unknown,
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
