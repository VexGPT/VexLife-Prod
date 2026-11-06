package vexlife.platform.macos

import kotlinx.serialization.Serializable

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID

/**
 * Generic macOS user-launchd construction/lifetime owner.
 *
 * Domain owners supply an already-decided process specification. This owner only answers:
 * what exact user launchd process exists, how it is started, and whether exact retirement completed.
 *
 * It owns no Heretic, Conversation, Vex Home, or presentation meaning.
 */
object MacUserLaunchdProcess {
    const val Schema = "vexlife.macos-user-launchd-process/v2"
    const val SubstrateRef = "substrate.vexlife.macos-user-launchd-process.001"
    const val DefaultStartTimeoutMs = 15_000L
    const val DefaultRetireTimeoutMs = 10_000L
    const val DefaultPollMs = 100L

    private val labelPattern = Regex("com\\.vexlife\\.[a-z0-9][a-z0-9.-]*")
    private val environmentKeyPattern = Regex("[A-Za-z_][A-Za-z0-9_]*")
    private val missingServicePattern = Regex(
        "could not find service|service not found|no such process|no such file or directory",
        RegexOption.IGNORE_CASE,
    )
    private val pidPattern = Regex("^\\s*pid\\s*=\\s*(\\d+)\\s*$", RegexOption.MULTILINE)

    data class CommandResult(
        val status: Int?,
        val stdout: String,
        val stderr: String,
        val errorMessage: String? = null,
    )

    interface Host {
        val uid: Long
        fun launchctl(args: List<String>): CommandResult
        fun processAlive(pid: Long): Boolean
        fun nowMs(): Long = System.currentTimeMillis()
        fun sleep(milliseconds: Long) = Thread.sleep(milliseconds)
    }

    object SystemHost : Host {
        override val uid: Long
            get() {
                val result = runCommand(listOf("/usr/bin/id", "-u"))
                require(result.status == 0 && result.errorMessage == null) {
                    "unable to resolve current macOS numeric uid"
                }
                return result.stdout.trim().toLong()
            }

        override fun launchctl(args: List<String>): CommandResult =
            runCommand(listOf("/bin/launchctl") + args)

        override fun processAlive(pid: Long): Boolean =
            ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)
    }

    data class Spec(
        val schemaVersion: String,
        val substrateRef: String,
        val label: String,
        val domainTarget: String,
        val plistPath: String,
        val programPath: String,
        val programArguments: List<String>,
        val workingDirectory: String,
        val stdoutPath: String,
        val stderrPath: String,
        val environment: Map<String, String>,
        val keepAlive: Boolean,
    )

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val substrateRef: String,
        val label: String,
        val domainTarget: String,
        val plistPath: String,
        val programPath: String,
        val programArguments: List<String>,
        val workingDirectory: String,
        val stdoutPath: String,
        val stderrPath: String,
        val environment: Map<String, String>,
        val keepAlive: Boolean,
        val pid: Long,
    )

    sealed interface Observation {
        data object Absent : Observation
        data object LoadedStopped : Observation
        data class Running(val pid: Long) : Observation
        data class Unknown(val detail: String) : Observation
    }

    data class Retirement(
        val state: String,
        val exactOwnershipVerified: Boolean,
        val verifiedNotLive: Boolean,
        val signalSent: String?,
        val launchdLabel: String,
        val launchdDomainTarget: String,
    )

    fun form(
        label: String,
        plistPath: String,
        programPath: String,
        programArguments: List<String> = emptyList(),
        workingDirectory: String,
        stdoutPath: String,
        stderrPath: String,
        environment: Map<String, String> = emptyMap(),
        uid: Long,
    ): Spec {
        require(labelPattern.matches(label)) { "launchd label must be one com.vexlife.* identity" }
        require(uid >= 0) { "launchd uid must be non-negative" }
        require(environment.keys.all(environmentKeyPattern::matches)) { "launchd environment key is invalid" }

        return Spec(
            schemaVersion = Schema,
            substrateRef = SubstrateRef,
            label = label,
            domainTarget = "gui/$uid",
            plistPath = absolute(plistPath, "plistPath"),
            programPath = absolute(programPath, "programPath"),
            programArguments = programArguments.toList(),
            workingDirectory = absolute(workingDirectory, "workingDirectory"),
            stdoutPath = absolute(stdoutPath, "stdoutPath"),
            stderrPath = absolute(stderrPath, "stderrPath"),
            environment = environment.toSortedMap(),
            keepAlive = false,
        )
    }

    fun validate(receipt: Receipt): Receipt {
        require(receipt.schemaVersion == Schema && receipt.substrateRef == SubstrateRef) {
            "launchd receipt schema/substrate is not current"
        }
        require(receipt.domainTarget.matches(Regex("gui/\\d+"))) { "launchd receipt domainTarget is invalid" }
        require(receipt.environment.keys.all(environmentKeyPattern::matches)) { "launchd receipt environment key is invalid" }
        require(receipt.environment == receipt.environment.toSortedMap()) { "launchd receipt environment must be canonical/sorted" }
        require(!receipt.keepAlive) { "launchd receipt KeepAlive must remain false" }
        require(receipt.pid > 0) { "launchd receipt requires one positive pid" }
        receipt.toSpec()
        return receipt
    }

    fun observeCoordinate(
        domainTarget: String,
        label: String,
        host: Host = SystemHost,
    ): Observation {
        require(
            domainTarget.matches(
                Regex(
                    "gui/\\d+",
                ),
            ),
        ) {
            "launchd observation domainTarget is invalid"
        }
        require(
            labelPattern.matches(
                label,
            ),
        ) {
            "launchd observation label must be one com.vexlife.* identity"
        }

        val result =
            host.launchctl(
                listOf(
                    "print",
                    "$domainTarget/$label",
                ),
            )
        result.errorMessage
            ?.let {
                return Observation.Unknown(
                    it,
                )
            }
        if (
            result.status !=
            0
        ) {
            val detail =
                listOf(
                    result.stdout,
                    result.stderr,
                )
                    .joinToString(
                        "\n",
                    )
                    .trim()
            return if (
                missingServicePattern.containsMatchIn(
                    detail,
                )
            ) {
                Observation.Absent
            } else {
                Observation.Unknown(
                    detail,
                )
            }
        }
        val pid =
            pidPattern.find(
                result.stdout,
            )
                ?.groupValues
                ?.get(
                    1,
                )
                ?.toLongOrNull()
        return if (
            pid ==
            null
        ) {
            Observation.LoadedStopped
        } else {
            Observation.Running(
                pid,
            )
        }
    }

    fun observe(
        spec: Spec,
        host: Host = SystemHost,
    ): Observation =
        observeCoordinate(
            domainTarget = spec.domainTarget,
            label = spec.label,
            host = host,
        )

    fun receiptCurrent(
        receipt: Receipt,
        host: Host = SystemHost,
        expectedPid: Long = receipt.pid,
    ): Boolean {
        return try {
            validate(
                receipt,
            )
            require(
                expectedPid >
                    0,
            )

            val spec =
                receipt.toSpec()
            val plist =
                Path.of(
                    receipt.plistPath,
                )

            if (
                !Files.isRegularFile(
                    plist,
                    LinkOption.NOFOLLOW_LINKS,
                ) ||
                Files.isSymbolicLink(
                    plist,
                )
            ) {
                return false
            }

            if (
                Files.readString(
                    plist,
                    StandardCharsets.UTF_8,
                ) !=
                renderPlist(
                    spec,
                )
            ) {
                return false
            }

            val observed =
                observe(
                    spec,
                    host,
                )

            observed is
                Observation.Running &&
                observed.pid ==
                expectedPid &&
                receipt.pid ==
                expectedPid &&
                host.processAlive(
                    expectedPid,
                )
        } catch (
            _: Exception,
        ) {
            false
        }
    }

    fun start(
        spec: Spec,
        host: Host = SystemHost,
        timeoutMs: Long = DefaultStartTimeoutMs,
        pollMs: Long = DefaultPollMs,
    ): Receipt {
        require(timeoutMs > 0 && pollMs > 0) { "launchd start timeouts must be positive" }
        val before = observe(spec, host)
        require(before == Observation.Absent) { "launchd job must be absent before bootstrap: $before" }

        writePlist(spec)
        val bootstrap = host.launchctl(listOf("bootstrap", spec.domainTarget, spec.plistPath))
        if (bootstrap.errorMessage != null || bootstrap.status != 0) {
            removePlist(spec.plistPath)
            error("launchd bootstrap failed: ${commandDetail(bootstrap)}")
        }

        fun cleanupFailedBootstrap() {
            host.launchctl(listOf("bootout", jobTarget(spec)))
            removePlist(spec.plistPath)
        }

        val deadline = host.nowMs() + timeoutMs
        while (host.nowMs() < deadline) {
            when (val observed = observe(spec, host)) {
                is Observation.Running -> return receiptFromSpec(spec, observed.pid)
                is Observation.Unknown -> {
                    cleanupFailedBootstrap()
                    error("launchd job observation failed: ${observed.detail}")
                }
                Observation.Absent,
                Observation.LoadedStopped -> host.sleep(pollMs)
            }
        }

        cleanupFailedBootstrap()
        error("launchd job did not reach RUNNING before bounded startup timeout")
    }

    fun retire(
        receipt: Receipt,
        host: Host = SystemHost,
        expectedPid: Long = receipt.pid,
        timeoutMs: Long = DefaultRetireTimeoutMs,
        pollMs: Long = DefaultPollMs,
    ): Retirement {
        validate(receipt)
        require(expectedPid > 0) { "launchd expected pid must be positive" }
        require(timeoutMs > 0 && pollMs > 0) { "launchd retirement timeouts must be positive" }

        val spec = receipt.toSpec()
        val before = observe(spec, host)
        if (before is Observation.Unknown) error("launchd job ownership is unknown: ${before.detail}")
        if (before is Observation.Running && before.pid != expectedPid) {
            error("launchd job pid mismatch: expected $expectedPid, observed ${before.pid}")
        }

        if (before != Observation.Absent) {
            val bootout = host.launchctl(listOf("bootout", jobTarget(spec)))
            if (bootout.errorMessage != null || bootout.status != 0) {
                error("launchd bootout failed: ${commandDetail(bootout)}")
            }
        }

        val deadline = host.nowMs() + timeoutMs
        while (true) {
            val observed = observe(spec, host)
            val pidLive = host.processAlive(expectedPid)
            if (observed == Observation.Absent && !pidLive) {
                removePlist(spec.plistPath)
                return Retirement(
                    state = "RETIRED_VERIFIED",
                    exactOwnershipVerified = true,
                    verifiedNotLive = true,
                    signalSent = if (before == Observation.Absent) null else "LAUNCHD_BOOTOUT",
                    launchdLabel = spec.label,
                    launchdDomainTarget = spec.domainTarget,
                )
            }
            if (observed is Observation.Unknown) {
                error("launchd retirement observation failed: ${observed.detail}")
            }
            if (host.nowMs() >= deadline) {
                error("launchd job retirement was not verified before bounded timeout")
            }
            host.sleep(pollMs)
        }
    }

    fun renderPlist(spec: Spec): String {
        val argumentsXml = (listOf(spec.programPath) + spec.programArguments)
            .joinToString("\n") { "    <string>${xml(it)}</string>" }
        val environmentXml = spec.environment.entries.sortedBy { it.key }
            .joinToString("\n") { (key, value) ->
                "    <key>${xml(key)}</key>\n    <string>${xml(value)}</string>"
            }
        val environmentBlock = if (environmentXml.isBlank()) "" else
            "  <key>EnvironmentVariables</key>\n  <dict>\n$environmentXml\n  </dict>\n"

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
            "<plist version=\"1.0\">\n<dict>\n" +
            "  <key>Label</key>\n  <string>${xml(spec.label)}</string>\n" +
            "  <key>ProgramArguments</key>\n  <array>\n$argumentsXml\n  </array>\n" +
            "  <key>WorkingDirectory</key>\n  <string>${xml(spec.workingDirectory)}</string>\n" +
            "  <key>StandardOutPath</key>\n  <string>${xml(spec.stdoutPath)}</string>\n" +
            "  <key>StandardErrorPath</key>\n  <string>${xml(spec.stderrPath)}</string>\n" +
            environmentBlock +
            "  <key>RunAtLoad</key>\n  <true/>\n" +
            "  <key>KeepAlive</key>\n  <false/>\n" +
            "  <key>ProcessType</key>\n  <string>Background</string>\n" +
            "</dict>\n</plist>\n"
    }

    private fun Receipt.toSpec(): Spec = form(
        label = label,
        plistPath = plistPath,
        programPath = programPath,
        programArguments = programArguments,
        workingDirectory = workingDirectory,
        stdoutPath = stdoutPath,
        stderrPath = stderrPath,
        environment = environment,
        uid = domainTarget.substringAfter("gui/").toLong(),
    )

    private fun receiptFromSpec(spec: Spec, pid: Long) = Receipt(
        schemaVersion = Schema,
        substrateRef = SubstrateRef,
        label = spec.label,
        domainTarget = spec.domainTarget,
        plistPath = spec.plistPath,
        programPath = spec.programPath,
        programArguments = spec.programArguments,
        workingDirectory = spec.workingDirectory,
        stdoutPath = spec.stdoutPath,
        stderrPath = spec.stderrPath,
        environment = spec.environment,
        keepAlive = false,
        pid = pid,
    )

    private fun jobTarget(spec: Spec) = "${spec.domainTarget}/${spec.label}"

    private fun absolute(value: String, label: String): String {
        val path = Path.of(value)
        require(path.isAbsolute) { "$label must be absolute" }
        return path.normalize().toString()
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun writePlist(spec: Spec) {
        val plist = Path.of(spec.plistPath)
        secureDirectory(plist.parent)
        secureDirectory(Path.of(spec.stdoutPath).parent)
        secureDirectory(Path.of(spec.stderrPath).parent)

        val temporary = plist.resolveSibling(
            "${plist.fileName}.partial-${ProcessHandle.current().pid()}-${UUID.randomUUID()}",
        )
        Files.writeString(
            temporary,
            renderPlist(spec),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE,
        )
        secureFile(temporary)
        try {
            Files.move(
                temporary,
                plist,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, plist, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun removePlist(value: String) = Files.deleteIfExists(Path.of(value))

    private fun secureDirectory(path: Path?) {
        if (path == null) return

        if (
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
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
                "launchd runtime directory must be one real non-symlink directory: $path"
            }
        } else {
            Files.createDirectory(
                path,
            )
        }

        setPosixPermissionsIfSupported(
            path,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )
    }

    private fun secureFile(path: Path) = setPosixPermissionsIfSupported(
        path,
        setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
        ),
    )

    private fun setPosixPermissionsIfSupported(path: Path, permissions: Set<PosixFilePermission>) {
        try {
            Files.setPosixFilePermissions(path, permissions)
        } catch (_: UnsupportedOperationException) {
            // macOS is POSIX; non-POSIX test hosts may not expose these permissions.
        }
    }

    private fun commandDetail(result: CommandResult): String =
        result.errorMessage ?: listOf(result.stdout, result.stderr).joinToString("\n").trim()

    private fun runCommand(command: List<String>): CommandResult = try {
        val process = ProcessBuilder(command).start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        CommandResult(process.waitFor(), stdout, stderr)
    } catch (error: Exception) {
        CommandResult(null, "", "", error.message ?: error::class.simpleName ?: "command failed")
    }
}

// [VXG RealForever]
