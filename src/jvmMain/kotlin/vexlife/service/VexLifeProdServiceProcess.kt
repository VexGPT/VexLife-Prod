package vexlife.service

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import vexlife.local.VexLifeProdLocalSetup

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

    data class Arguments(
        val sourceSha: String,
    )

    internal data class Hooks(
        val establishSession: (
            allowAcquisition: Boolean,
        ) -> VexLifeProdLocalSetup.Session = {
            allowAcquisition ->
            VexLifeProdLocalSetup.establishSession(
                allowAcquisition,
            )
        },
        val startService: (
            sourceSha: String,
            session: VexLifeProdLocalSetup.Session,
        ) -> AutoCloseable = {
            sourceSha,
            session,
            ->
            VexLifeProdLocalService.start(
                sourceSha = sourceSha,
                session = session,
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
        val awaitTermination: (
            AutoCloseable,
        ) -> Unit = ::awaitProcessTermination,
    )

    fun parse(
        args: Array<String>,
    ): Arguments {
        require(
            args.size ==
                2 &&
                args[0] ==
                "--source-sha",
        ) {
            "VexLife-Prod service process requires exactly --source-sha <git-sha>"
        }

        val sourceSha =
            args[1]

        require(
            SourceShaPattern.matches(
                sourceSha,
            ),
        ) {
            "VexLife-Prod service process source SHA must be lowercase hex"
        }

        return Arguments(
            sourceSha = sourceSha,
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
        val session =
            hooks.establishSession(
                false,
            )

        val service =
            hooks.startService(
                arguments.sourceSha,
                session,
            )

        hooks.reportReady(
            arguments,
        )

        hooks.awaitTermination(
            service,
        )
    }

    private fun awaitProcessTermination(
        service: AutoCloseable,
    ) {
        val latch =
            CountDownLatch(
                1,
            )
        val closed =
            AtomicBoolean(
                false,
            )

        fun closeOnce() {
            if (
                closed.compareAndSet(
                    false,
                    true,
                )
            ) {
                runCatching {
                    service.close()
                }
            }
        }

        val shutdownHook =
            Thread(
                {
                    closeOnce()
                    latch.countDown()
                },
                "vexlife-prod-service-shutdown",
            )

        Runtime
            .getRuntime()
            .addShutdownHook(
                shutdownHook,
            )

        try {
            latch.await()
        } finally {
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
