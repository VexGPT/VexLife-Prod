package vexlife.local

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.logging.VexLogger

class VexLifeProdLocalSetupLeaseTest {
    private val sourceSha = "a".repeat(40)

    @Test
    fun oneProdHomeAllowsOnlyOneConcurrentSetupLease() {
        val parent = Files.createTempDirectory("vexlife-prod-setup-lease-").toAbsolutePath()
        val productHome = parent.resolve("VexLife-Prod")

        try {
            VexLifeProdLocalSetupLease.acquire(productHome).use {
                assertFailsWith<IllegalArgumentException> {
                    VexLifeProdLocalSetupLease.acquire(productHome)
                }
            }

            VexLifeProdLocalSetupLease.acquire(productHome).use {
                // A released kernel lock is immediately reusable by the next setup process.
            }

            assertTrue(Files.isRegularFile(VexLifeProdLocalSetupLease.lockPath(productHome)))
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun restartTakeoverRetiresExactOwnedSetupEvenWhenJavaComesFromAnotherProviderImage() {
        val parent = Files.createTempDirectory("vexlife-prod-setup-takeover-").toAbsolutePath()
        val productHome = parent.resolve("VexLife-Prod")
        val productImage = parent.resolve("current-product-image")
        var leaseAvailable = false
        var terminated = 0
        var observedImage: Path? = null
        var observedSource: String? = null
        var now = 0L
        val snapshot =
            setupSnapshot(
                productHome = productHome,
                productImage = productImage,
                javaCommand = parent.resolve("older-java-provider/runtime/bin/java").toString(),
            )

        try {
            VexLifeProdLocalSetupLease.prepareRestartTakeover(
                productHome = productHome,
                hooks =
                    VexLifeProdLocalSetupLease.TakeoverHooks(
                        leaseAvailable = { leaseAvailable },
                        holderPids = {
                            if (leaseAvailable) {
                                emptyList()
                            } else {
                                listOf(snapshot.pid)
                            }
                        },
                        observeProcess = { pid ->
                            snapshot.takeIf { pid == snapshot.pid }
                        },
                        verifyProductImage = { imageRoot, source ->
                            observedImage = imageRoot
                            observedSource = source
                            true
                        },
                        terminate = { pid ->
                            assertEquals(snapshot.pid, pid)
                            terminated += 1
                            leaseAvailable = true
                            true
                        },
                        processAlive = {
                            !leaseAvailable
                        },
                        currentPid = {
                            9999L
                        },
                        nowMs = {
                            now
                        },
                        sleep = { millis ->
                            now += millis
                        },
                    ),
            )

            assertEquals(1, terminated)
            assertEquals(productImage.toAbsolutePath().normalize(), observedImage)
            assertEquals(sourceSha, observedSource)
            assertTrue(leaseAvailable)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun restartTakeoverChronologyDistinguishesNoopReleaseAndExactRetirement() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-setup-takeover-chronology-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )
        val productImage =
            parent.resolve(
                "current-product-image",
            )
        val snapshot =
            setupSnapshot(
                productHome = productHome,
                productImage = productImage,
            )

        try {
            VexLogger.capturing {
                records ->
                VexLifeProdLocalSetupLease.prepareRestartTakeover(
                    productHome = productHome,
                    hooks =
                        VexLifeProdLocalSetupLease.TakeoverHooks(
                            leaseAvailable = {
                                true
                            },
                            holderPids = {
                                error(
                                    "free lease must not inspect holders",
                                )
                            },
                        ),
                )
                val fact =
                    records.single {
                        it.fact ==
                            VexLogger.Fact.SetupRestartTakeoverResolved.wire
                    }
                assertEquals(
                    "LEASE_ALREADY_AVAILABLE",
                    fact.refs["outcome"],
                )
            }

            var preSignalLeaseChecks =
                0
            var preSignalTerminateCalls =
                0
            VexLogger.capturing {
                records ->
                VexLifeProdLocalSetupLease.prepareRestartTakeover(
                    productHome = productHome,
                    hooks =
                        VexLifeProdLocalSetupLease.TakeoverHooks(
                            leaseAvailable = {
                                preSignalLeaseChecks +=
                                    1
                                preSignalLeaseChecks >=
                                    2
                            },
                            holderPids = {
                                listOf(
                                    snapshot.pid,
                                )
                            },
                            observeProcess = { pid ->
                                snapshot.takeIf {
                                    pid ==
                                        snapshot.pid
                                }
                            },
                            verifyProductImage = { _, _ ->
                                true
                            },
                            terminate = {
                                preSignalTerminateCalls +=
                                    1
                                true
                            },
                            currentPid = {
                                9999L
                            },
                        ),
                )
                val fact =
                    records.single {
                        it.fact ==
                            VexLogger.Fact.SetupRestartTakeoverResolved.wire
                    }
                assertEquals(
                    "LEASE_RELEASED_BEFORE_TAKEOVER",
                    fact.refs["outcome"],
                )
                assertEquals(
                    snapshot.pid.toString(),
                    fact.refs["holderPid"],
                )
                assertEquals(
                    sourceSha,
                    fact.refs["holderSourceSha"],
                )
                assertEquals(
                    0,
                    preSignalTerminateCalls,
                )
            }

            var leaseAvailable =
                false
            VexLogger.capturing {
                records ->
                VexLifeProdLocalSetupLease.prepareRestartTakeover(
                    productHome = productHome,
                    hooks =
                        VexLifeProdLocalSetupLease.TakeoverHooks(
                            leaseAvailable = {
                                leaseAvailable
                            },
                            holderPids = {
                                if (
                                    leaseAvailable
                                ) {
                                    emptyList()
                                } else {
                                    listOf(
                                        snapshot.pid,
                                    )
                                }
                            },
                            observeProcess = { pid ->
                                snapshot.takeIf {
                                    pid ==
                                        snapshot.pid
                                }
                            },
                            verifyProductImage = { _, _ ->
                                true
                            },
                            terminate = {
                                leaseAvailable =
                                    true
                                true
                            },
                            processAlive = {
                                !leaseAvailable
                            },
                            currentPid = {
                                9999L
                            },
                        ),
                )
                val fact =
                    records.single {
                        it.fact ==
                            VexLogger.Fact.SetupRestartTakeoverResolved.wire
                    }
                assertEquals(
                    "EXACT_PREDECESSOR_RETIRED",
                    fact.refs["outcome"],
                )
                assertEquals(
                    snapshot.pid.toString(),
                    fact.refs["holderPid"],
                )
                assertEquals(
                    sourceSha,
                    fact.refs["holderSourceSha"],
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun restartTakeoverNeverTerminatesUnknownOrWrongHomeProcesses() {
        val parent = Files.createTempDirectory("vexlife-prod-setup-takeover-held-").toAbsolutePath()
        val productHome = parent.resolve("VexLife-Prod")
        val otherHome = parent.resolve("Other-Home")
        val productImage = parent.resolve("current-product-image")
        var terminated = 0

        fun hooks(snapshot: VexLifeProdLocalSetupLease.ProcessSnapshot) =
            VexLifeProdLocalSetupLease.TakeoverHooks(
                leaseAvailable = {
                    false
                },
                holderPids = {
                    listOf(snapshot.pid)
                },
                observeProcess = { pid ->
                    snapshot.takeIf { pid == snapshot.pid }
                },
                verifyProductImage = { _, _ ->
                    true
                },
                terminate = {
                    terminated += 1
                    true
                },
                processAlive = {
                    true
                },
                currentPid = {
                    9999L
                },
            )

        try {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdLocalSetupLease.prepareRestartTakeover(
                    productHome = productHome,
                    hooks =
                        hooks(
                            setupSnapshot(
                                productHome = productHome,
                                productImage = productImage,
                                mainClass = "example.UnrelatedProcessKt",
                            )
                        ),
                )
            }
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdLocalSetupLease.prepareRestartTakeover(
                    productHome = productHome,
                    hooks =
                        hooks(
                            setupSnapshot(
                                productHome = otherHome,
                                productImage = productImage,
                            )
                        ),
                )
            }

            assertEquals(0, terminated)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun restartTakeoverHoldsWhenProductImageCannotBeVerifiedOrLeaseOwnershipIsAmbiguous() {
        val parent = Files.createTempDirectory("vexlife-prod-setup-takeover-unknown-").toAbsolutePath()
        val productHome = parent.resolve("VexLife-Prod")
        val snapshot =
            setupSnapshot(
                productHome = productHome,
                productImage = parent.resolve("candidate-image"),
            )
        var terminated = 0

        try {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdLocalSetupLease.prepareRestartTakeover(
                    productHome = productHome,
                    hooks =
                        VexLifeProdLocalSetupLease.TakeoverHooks(
                            leaseAvailable = {
                                false
                            },
                            holderPids = {
                                listOf(snapshot.pid, snapshot.pid + 1)
                            },
                            terminate = {
                                terminated += 1
                                true
                            },
                            currentPid = {
                                9999L
                            },
                        ),
                )
            }

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdLocalSetupLease.prepareRestartTakeover(
                    productHome = productHome,
                    hooks =
                        VexLifeProdLocalSetupLease.TakeoverHooks(
                            leaseAvailable = {
                                false
                            },
                            holderPids = {
                                listOf(snapshot.pid)
                            },
                            observeProcess = { pid ->
                                snapshot.takeIf { pid == snapshot.pid }
                            },
                            verifyProductImage = { _, _ ->
                                false
                            },
                            terminate = {
                                terminated += 1
                                true
                            },
                            currentPid = {
                                9999L
                            },
                        ),
                )
            }

            assertEquals(0, terminated)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun restartTakeoverCanNeverTerminateItsOwnSetupProcess() {
        val parent = Files.createTempDirectory("vexlife-prod-setup-takeover-self-").toAbsolutePath()
        val productHome = parent.resolve("VexLife-Prod")
        val snapshot =
            setupSnapshot(
                productHome = productHome,
                productImage = parent.resolve("candidate-image"),
            )
        var terminated = false

        try {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdLocalSetupLease.prepareRestartTakeover(
                    productHome = productHome,
                    hooks =
                        VexLifeProdLocalSetupLease.TakeoverHooks(
                            leaseAvailable = {
                                false
                            },
                            holderPids = {
                                listOf(snapshot.pid)
                            },
                            observeProcess = {
                                snapshot
                            },
                            verifyProductImage = { _, _ ->
                                true
                            },
                            terminate = {
                                terminated = true
                                true
                            },
                            currentPid = {
                                snapshot.pid
                            },
                        ),
                )
            }

            assertFalse(terminated)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun symlinkCanNeverBecomeSetupSerializationAuthority() {
        val parent = Files.createTempDirectory("vexlife-prod-setup-lease-link-").toAbsolutePath()
        val productHome = parent.resolve("VexLife-Prod")
        val target = parent.resolve("other.lock")
        val lockPath = VexLifeProdLocalSetupLease.lockPath(productHome)

        try {
            Files.writeString(target, "not-owned")
            Files.createSymbolicLink(lockPath, target)

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdLocalSetupLease.acquire(productHome)
            }

            assertTrue(Files.isSymbolicLink(lockPath))
            assertTrue(Files.exists(target))
        } finally {
            Files.deleteIfExists(lockPath)
            parent.toFile().deleteRecursively()
        }
    }

    private fun setupSnapshot(
        productHome: Path,
        productImage: Path,
        pid: Long = 4242L,
        javaCommand: String = "/usr/bin/java",
        mainClass: String = "vexlife.setup.VexLifeProdSetupProcessKt",
    ): VexLifeProdLocalSetupLease.ProcessSnapshot =
        VexLifeProdLocalSetupLease.ProcessSnapshot(
            pid = pid,
            command = javaCommand,
            arguments =
                listOf(
                    "-Dvexlife.setupJavaProviderSourceSha=" + "b".repeat(40),
                    "-cp",
                    productImage.toAbsolutePath().normalize().toString() + "/lib/*",
                    mainClass,
                    "--activate-front-door-control",
                    "CONTROL_CONVERGE_DESIRED",
                    "--home",
                    productHome.toAbsolutePath().normalize().toString(),
                    "--expected-source-sha",
                    sourceSha,
                ),
        )
}

// [VXG RealForever]
