package vexlife.service

import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.local.VexLifeProdRuntimeSessionFactory
import vexlife.logging.VexLogger
import vexlife.packaging.VexLifeProdPrivateServiceImage

/**
 * Long-lived JVM entry for the isolated local product service.
 *
 * This process program owns sequencing only:
 * exact source identity -> existing local Session -> fixed loopback service -> process lifetime.
 *
 * It never grants model/runtime acquisition authority and never opens the human presentation.
 */
object VexLifeProdServiceProcess {
    private val SourceShaPattern =
        Regex(
            "[0-9a-f]{40,64}",
        )

    private const val HomeRetentionPollMs =
        2_000L

    private const val HomeLossConfirmations =
        3

    private const val ServiceCloseRetryMs =
        5_000L

    internal data class HomeIdentity(
        val homePath: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
    )

    internal class HomeRetention {
        private var consecutiveLosses =
            0

        fun observe(
            current: Boolean,
        ): Boolean {
            if (
                current
            ) {
                consecutiveLosses =
                    0
                return false
            }

            consecutiveLosses +=
                1
            return consecutiveLosses >=
                HomeLossConfirmations
        }
    }

    data class Arguments(
        val sourceSha: String,
        val productHome: Path,
    )

    internal data class RuntimeExecutablePaths(
        val visionRuntimeExecutablePathOrNull: String?,
        val partnerHostExecutablePathOrNull: String?,
    )

    internal data class Hooks(
        val resolveRuntimeExecutablePaths: (
            sourceSha: String,
        ) -> RuntimeExecutablePaths = {
            sourceSha ->
            VexLifeProdPrivateServiceImage
                .observe(
                    imageRoot =
                        Path.of(
                            "",
                        )
                            .toAbsolutePath()
                            .normalize(),
                    expectedSourceSha = sourceSha,
                )
                .let {
                    observation ->
                    RuntimeExecutablePaths(
                        visionRuntimeExecutablePathOrNull =
                            observation.visionRuntimeExecutablePathOrNull,
                        partnerHostExecutablePathOrNull =
                            observation.partnerHostExecutablePathOrNull,
                    )
                }
        },
        val establishSession: (
            productHome: Path,
            allowAcquisition: Boolean,
        ) -> VexLifeProdRuntimeSession = {
            productHome,
            allowAcquisition,
            ->
            require(
                !allowAcquisition,
            ) {
                "long-lived Product service process never grants runtime acquisition authority"
            }
            VexLifeProdRuntimeSessionFactory.establishForServiceProcess(
                productHome,
            )
        },
        val startService: (
            sourceSha: String,
            session: VexLifeProdRuntimeSession,
            visionRuntimeExecutablePathOrNull: String?,
            partnerHostExecutablePathOrNull: String?,
        ) -> AutoCloseable = {
            sourceSha,
            session,
            visionRuntimeExecutablePathOrNull,
            partnerHostExecutablePathOrNull,
            ->
            VexLifeProdLocalService.start(
                sourceSha = sourceSha,
                session = session,
                visionRuntimeExecutablePathOrNull =
                    visionRuntimeExecutablePathOrNull,
                partnerHostExecutablePathOrNull =
                    partnerHostExecutablePathOrNull,
            )
        },
        val reportReady: (
            Arguments,
        ) -> Unit = {
            arguments ->
            println(
                "VEXLIFE_PROD_SERVICE_READY\t" +
                    arguments.sourceSha +
                    "\t" +
                    VexLifeProdServiceContract.Origin,
            )
        },
        val observeHomeIdentity: (
            Path,
        ) -> HomeIdentity? =
            ::observeHomeIdentity,
        val awaitTermination: (
            AutoCloseable,
            () -> Boolean,
        ) -> Unit = ::awaitProcessTermination,
    )

    fun parse(
        args: Array<String>,
    ): Arguments {
        require(
            args.size == 4 &&
                args[0] == "--source-sha" &&
                args[2] == "--home",
        ) {
            "VexLife-Prod service process requires exactly --source-sha <git-sha> --home <absolute-path>"
        }

        val sourceSha = args[1]

        require(
            SourceShaPattern.matches(
                sourceSha,
            ),
        ) {
            "VexLife-Prod service process source SHA must be lowercase hex"
        }

        val productHome = Path.of(args[3])
        require(productHome.isAbsolute) {
            "VexLife-Prod service process Home path must be absolute"
        }

        return Arguments(
            sourceSha = sourceSha,
            productHome = productHome.normalize(),
        )
    }

    fun run(
        args: Array<String>,
    ) {
        runWithHooks(
            arguments =
                parse(
                    args,
                ),
            hooks = Hooks(),
        )
    }

    internal fun runWithHooks(
        arguments: Arguments,
        hooks: Hooks,
    ) {
        // The service only ever runs for an established Home, so its chronology lives there.
        VexLogger.bind(
            processRole = "service",
            sourceSha = arguments.sourceSha,
            chronologyHome = arguments.productHome,
        )
        VexLogger.record(
            VexLogger.Fact.ServiceProcessStarted,
            mapOf("pid" to ProcessHandle.current().pid().toString()),
        )

        val retainedHomeIdentity =
            requireNotNull(
                hooks.observeHomeIdentity(
                    arguments.productHome,
                ),
            ) {
                "VexLife-Prod service requires one exact current Home identity at admission"
            }

        val runtimeExecutablePaths =
            hooks.resolveRuntimeExecutablePaths(
                arguments.sourceSha,
            )

        val session =
            try {
                hooks.establishSession(
                    arguments.productHome,
                    false,
                )
            } catch (error: Throwable) {
                VexLogger.record(
                    VexLogger.Fact.ServiceStartFailed,
                    detail = error.javaClass.simpleName + ": " + (error.message ?: ""),
                )
                throw error
            }

        val service =
            try {
                hooks.startService(
                    arguments.sourceSha,
                    session,
                    runtimeExecutablePaths.visionRuntimeExecutablePathOrNull,
                    runtimeExecutablePaths.partnerHostExecutablePathOrNull,
                )
            } catch (error: Throwable) {
                VexLogger.record(
                    VexLogger.Fact.ServiceStartFailed,
                    detail = error.javaClass.simpleName + ": " + (error.message ?: ""),
                )
                throw error
            }

        VexLogger.record(VexLogger.Fact.ServiceListening, mapOf("origin" to VexLifeProdServiceContract.Origin))
        hooks.reportReady(
            arguments,
        )

        hooks.awaitTermination(
            service,
            {
                hooks.observeHomeIdentity(
                    arguments.productHome,
                ) ==
                    retainedHomeIdentity
            },
        )
    }

    private fun observeHomeIdentity(
        productHome: Path,
    ): HomeIdentity? =
        runCatching {
                VexLifeProdHomeFiles.observeExisting(
                    productHome,
                )
            }
            .getOrNull()
            ?.let {
                current ->
                HomeIdentity(
                    homePath = current.productHome,
                    homeRef = current.manifest.homeRef,
                    deviceRef = current.manifest.currentDeviceRef,
                    companionLineageRef = current.manifest.currentCompanionLineageRef,
                )
            }

    internal fun awaitProcessTermination(
        service: AutoCloseable,
        homeCurrent: () -> Boolean,
    ) {
        val latch =
            CountDownLatch(
                1,
            )
        val closed =
            AtomicBoolean(
                false,
            )
        val closeFence =
            Any()

        fun closeOnce() {
            if (
                closed.get()
            ) {
                return
            }

            synchronized(
                closeFence,
            ) {
                if (
                    closed.get()
                ) {
                    return
                }
                service.close()
                closed.set(
                    true,
                )
            }
        }

        fun closeUntilSuccess(
            interruptible: Boolean,
        ): Boolean {
            while (
                !closed.get()
            ) {
                try {
                    closeOnce()
                    return true
                } catch (
                    error: Throwable,
                ) {
                    System.err.println(
                        "VEXLIFE_PROD_SERVICE_STOP_RETRY\t" +
                            error.javaClass.simpleName +
                            "\t" +
                            (error.message ?: ""),
                    )
                    try {
                        Thread.sleep(
                            ServiceCloseRetryMs,
                        )
                    } catch (
                        _: InterruptedException,
                    ) {
                        if (
                            interruptible
                        ) {
                            Thread
                                .currentThread()
                                .interrupt()
                            return false
                        }
                    }
                }
            }
            return true
        }

        val shutdownHook =
            Thread(
                {
                    VexLogger.record(
                        VexLogger.Fact.ServiceProcessTerminating,
                    )
                    if (
                        closeUntilSuccess(
                            interruptible = false,
                        )
                    ) {
                        latch.countDown()
                    }
                },
                "vexlife-prod-service-shutdown",
            )

        val homeRetention =
            Thread(
                {
                    val retention =
                        HomeRetention()
                    while (
                        !Thread
                            .currentThread()
                            .isInterrupted
                    ) {
                        val current =
                            runCatching(
                                homeCurrent,
                            )
                                .getOrDefault(
                                    false,
                                )
                        if (
                            retention.observe(
                                current,
                            )
                        ) {
                            // Stop exposing Product effects as soon as the exact admitted Home is
                            // no longer current. VexLifeProdLocalService.close() closes the current
                            // runtime Session (including a post-start model switch) through the
                            // rightful platform owner. Keep this JVM alive until that exact close
                            // succeeds so Home loss cannot erase the last retirement capability.
                            if (
                                closeUntilSuccess(
                                    interruptible = true,
                                )
                            ) {
                                latch.countDown()
                            }
                            return@Thread
                        }

                        try {
                            Thread.sleep(
                                HomeRetentionPollMs,
                            )
                        } catch (
                            _: InterruptedException,
                        ) {
                            Thread
                                .currentThread()
                                .interrupt()
                        }
                    }
                },
                "vexlife-prod-home-retention",
            )
                .also {
                    it.isDaemon =
                        true
                }

        Runtime
            .getRuntime()
            .addShutdownHook(
                shutdownHook,
            )
        homeRetention.start()

        try {
            latch.await()
        } finally {
            homeRetention.interrupt()
            try {
                Runtime
                    .getRuntime()
                    .removeShutdownHook(
                        shutdownHook,
                    )
            } catch (
                _: IllegalStateException,
            ) {
                // JVM shutdown is already in progress.
            }
            closeOnce()
        }
    }

}

fun main(
    args: Array<String>,
) {
    VexLifeProdServiceProcess.run(
        args,
    )
}

// [VXG RealForever]
