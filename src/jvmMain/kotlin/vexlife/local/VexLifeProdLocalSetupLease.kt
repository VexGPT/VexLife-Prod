package vexlife.local

import java.nio.file.Files
import java.nio.file.Path
import vexlife.filesystem.VexExclusiveFileLease
import vexlife.logging.VexLogger
import vexlife.packaging.VexLifeProdPrivateServiceImage

/**
 * Stable setup-serialization coordinate for the ordinary VexLife-Prod local setup effect.
 *
 * OS file-lock mechanics are delegated to [VexExclusiveFileLease]. This owner answers only which
 * lock coordinate serializes one Prod Home setup.
 *
 * A person who explicitly chooses Update & Restart / Repair & Restart has also chosen to supersede
 * an older exact VexLife setup for that same Home. That takeover remains fail-closed: the lock
 * holder must be proven as one exact VexLife setup process from its Product classpath + main class +
 * Home/source arguments before this owner may ask it to terminate. The Java executable itself may
 * intentionally come from a separately retained Java-provider image and is therefore not Product
 * source identity.
 */
internal object VexLifeProdLocalSetupLease {
    private const val LockSuffix = ".setup.lock"
    private const val SetupMainClass = "vexlife.setup.VexLifeProdSetupProcessKt"
    private const val LsofExecutable = "/usr/sbin/lsof"
    private const val TakeoverPollMs = 100L
    private const val TakeoverTimeoutMs = 10_000L

    private val GitSha = Regex("[0-9a-f]{40}")

    internal data class ProcessSnapshot(
        val pid: Long,
        val command: String,
        val arguments: List<String>,
    )

    internal data class TakeoverHooks(
        val leaseAvailable: (Path) -> Boolean = ::leaseAvailable,
        val holderPids: (Path) -> List<Long> = ::holderPids,
        val observeProcess: (Long) -> ProcessSnapshot? = ::observeProcess,
        val verifyProductImage: (Path, String) -> Boolean = { imageRoot, sourceSha ->
            runCatching {
                    VexLifeProdPrivateServiceImage.observe(
                        imageRoot = imageRoot,
                        expectedSourceSha = sourceSha,
                    )
                }
                .isSuccess
        },
        val terminate: (Long) -> Boolean = { pid ->
            ProcessHandle
                .of(pid)
                .map {
                    it.destroy()
                }
                .orElse(false)
        },
        val processAlive: (Long) -> Boolean = { pid ->
            ProcessHandle
                .of(pid)
                .map {
                    it.isAlive
                }
                .orElse(false)
        },
        val currentPid: () -> Long = {
            ProcessHandle.current().pid()
        },
        val nowMs: () -> Long = System::currentTimeMillis,
        val sleep: (Long) -> Unit = Thread::sleep,
    )

    fun acquire(productHome: Path): VexExclusiveFileLease.Held {
        val path = lockPath(productHome)

        // The lock sits beside the not-yet-existing Prod Home in ambient user space.
        Files.createDirectories(requireNotNull(path.parent))

        return try {
            VexExclusiveFileLease.acquire(path)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException(
                "another VexLife-Prod local setup is already in progress or the setup lease is invalid",
                error,
            )
        }
    }

    /**
     * Restart-only predecessor takeover.
     *
     * This does not acquire setup authority for the caller. It only clears an exact older VexLife
     * setup holder when one is proven, then verifies the lease is available again. Ordinary setup
     * still acquires the same kernel lease through [acquire] afterwards, so a concurrent race
     * remains safely serialized.
     */
    internal fun prepareRestartTakeover(
        productHome: Path,
        hooks: TakeoverHooks = TakeoverHooks(),
    ) {
        require(productHome.isAbsolute) {
            "VexLife-Prod Home must be absolute"
        }
        val selectedHome = productHome.normalize()
        val path = lockPath(selectedHome)

        Files.createDirectories(requireNotNull(path.parent))
        if (hooks.leaseAvailable(path)) {
            VexLogger.record(
                VexLogger.Fact.SetupRestartTakeoverResolved,
                mapOf("outcome" to "LEASE_ALREADY_AVAILABLE"),
            )
            return
        }

        val holders =
            hooks
                .holderPids(path)
                .distinct()
        require(holders.size == 1) {
            "VexLife setup lease is held but one exact predecessor setup holder could not be proven"
        }

        val pid = holders.single()
        require(pid > 0 && pid != hooks.currentPid()) {
            "VexLife setup restart takeover cannot target the current setup process"
        }

        val first =
            requireNotNull(
                hooks.observeProcess(pid),
            ) {
                "VexLife setup lease holder process disappeared before ownership could be proven"
            }
        require(
            isOwnedSetupForHome(
                snapshot = first,
                selectedHome = selectedHome,
                hooks = hooks,
            )
        ) {
            "VexLife setup lease holder is not one exact owned VexLife setup for this Home"
        }
        val holderSourceSha =
            requireNotNull(
                flagValue(
                    first.arguments,
                    "--expected-source-sha",
                )?.takeIf(
                    GitSha::matches,
                ),
            ) {
                "owned VexLife setup holder lost its exact source identity"
            }

        // Ownership and lock custody are re-observed immediately before the terminal signal.
        if (hooks.leaseAvailable(path)) {
            VexLogger.record(
                VexLogger.Fact.SetupRestartTakeoverResolved,
                mapOf(
                    "outcome" to "LEASE_RELEASED_BEFORE_TAKEOVER",
                    "holderPid" to pid.toString(),
                    "holderSourceSha" to holderSourceSha,
                ),
            )
            return
        }
        require(
            hooks
                .holderPids(path)
                .distinct() ==
                listOf(pid)
        ) {
            "VexLife setup lease holder changed before restart takeover"
        }
        val current =
            requireNotNull(
                hooks.observeProcess(pid),
            ) {
                "VexLife setup lease holder disappeared before restart takeover"
            }
        require(current == first) {
            "VexLife setup lease holder identity changed before restart takeover"
        }

        val signalAccepted =
            hooks.terminate(pid)
        require(
            signalAccepted ||
                !hooks.processAlive(pid)
        ) {
            "exact older VexLife setup did not accept graceful restart takeover"
        }

        val deadline =
            hooks.nowMs() +
                TakeoverTimeoutMs
        while (
            hooks.nowMs() <
                deadline
        ) {
            if (hooks.leaseAvailable(path)) {
                VexLogger.record(
                    VexLogger.Fact.SetupRestartTakeoverResolved,
                    mapOf(
                        "outcome" to "EXACT_PREDECESSOR_RETIRED",
                        "holderPid" to pid.toString(),
                        "holderSourceSha" to holderSourceSha,
                    ),
                )
                return
            }
            hooks.sleep(
                TakeoverPollMs,
            )
        }

        error(
            "exact older VexLife setup did not release the Home setup lease before bounded restart timeout"
        )
    }

    private fun isOwnedSetupForHome(
        snapshot: ProcessSnapshot,
        selectedHome: Path,
        hooks: TakeoverHooks,
    ): Boolean {
        val commandName =
            runCatching {
                    Path.of(snapshot.command)
                        .fileName
                        ?.toString()
                }
                .getOrNull()
                ?: return false
        if (commandName != "java") {
            return false
        }

        val mainIndex =
            snapshot.arguments.indexOf(
                SetupMainClass,
            )
        if (mainIndex < 1) {
            return false
        }

        val prefix =
            snapshot.arguments.take(mainIndex)
        val classpathIndex =
            prefix.indexOfLast {
                it == "-cp" ||
                    it == "-classpath"
            }
        if (
            classpathIndex < 0 ||
            classpathIndex + 1 >=
                prefix.size
        ) {
            return false
        }

        val classpath =
            prefix[classpathIndex + 1]
        val suffix =
            "/lib/*"
        if (!classpath.endsWith(suffix)) {
            return false
        }
        val imageRoot =
            runCatching {
                    Path.of(
                            classpath.removeSuffix(suffix),
                        )
                        .toAbsolutePath()
                        .normalize()
                }
                .getOrNull()
                ?: return false

        val setupArguments =
            snapshot.arguments.drop(mainIndex + 1)
        if (
            setupArguments.firstOrNull() !in
                setOf(
                    "--activate-front-door-control",
                    "--home",
                    "--form-home",
                )
        ) {
            return false
        }

        val home =
            flagValue(
                setupArguments,
                "--home",
            )
                ?.let {
                    runCatching {
                            Path.of(it)
                                .toAbsolutePath()
                                .normalize()
                        }
                        .getOrNull()
                }
                ?: return false
        if (home != selectedHome) {
            return false
        }

        val sourceSha =
            flagValue(
                setupArguments,
                "--expected-source-sha",
            )
                ?.takeIf(
                    GitSha::matches,
                )
                ?: return false

        return hooks.verifyProductImage(
            imageRoot,
            sourceSha,
        )
    }

    private fun flagValue(
        arguments: List<String>,
        flag: String,
    ): String? {
        val index =
            arguments.indexOf(
                flag,
            )
        if (
            index < 0 ||
            index + 1 >=
                arguments.size
        ) {
            return null
        }
        return arguments[index + 1]
            .takeUnless {
                it.startsWith("--")
            }
    }

    private fun leaseAvailable(
        path: Path,
    ): Boolean =
        runCatching {
                VexExclusiveFileLease
                    .acquire(path)
                    .use {
                        // Probe only: release immediately.
                    }
            }
            .isSuccess

    private fun holderPids(
        path: Path,
    ): List<Long> {
        val process =
            ProcessBuilder(
                    LsofExecutable,
                    "-t",
                    path.toString(),
                )
                .start()
        val stdout =
            process.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }
        val stderr =
            process.errorStream
                .bufferedReader()
                .use {
                    it.readText()
                }
        val status =
            process.waitFor()

        if (
            status == 1 &&
            stdout.isBlank()
        ) {
            return emptyList()
        }
        require(status == 0) {
            "VexLife setup lease holder observation failed: " +
                stderr.trim()
        }

        return stdout
            .lineSequence()
            .map {
                it.trim()
            }
            .filter {
                it.isNotEmpty()
            }
            .mapNotNull(
                String::toLongOrNull,
            )
            .toList()
    }

    private fun observeProcess(
        pid: Long,
    ): ProcessSnapshot? {
        val handle =
            ProcessHandle
                .of(pid)
                .orElse(null)
                ?: return null
        if (!handle.isAlive) {
            return null
        }
        val info =
            handle.info()
        val command =
            info.command()
                .orElse(null)
                ?: return null
        val arguments =
            info.arguments()
                .orElse(null)
                ?.toList()
                ?: return null

        return ProcessSnapshot(
            pid = pid,
            command = command,
            arguments = arguments,
        )
    }

    fun lockPath(productHome: Path): Path {
        require(productHome.isAbsolute) {
            "VexLife-Prod Home must be absolute"
        }
        val home = productHome.normalize()
        return requireNotNull(home.parent).resolve("." + home.fileName + LockSuffix)
    }
}

// [VXG RealForever]
