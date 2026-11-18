package vexlife.service

import vexlife.browser.VexLifeProdBrowserContract
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.time.Duration
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.packaging.VexLifeProdPrivateServiceImageInstaller
import vexlife.logging.VexLogger
import vexlife.platform.macos.MacUserLaunchdProcess

/**
 * macOS construction/lifetime owner for the installed VexLife-Prod local product service.
 *
 * It owns source/image/process reconciliation for one stable launchd service. It does not own
 * Conversation, Heretic semantic/runtime meaning, setup authorization, release discovery, or UI.
 */
object VexLifeProdMacServiceLifetime {
    const val ReceiptSchema =
        "vexlife-prod.macos-service-lifetime/v2"

    const val LegacyReceiptSchema =
        "vexlife-prod.macos-service-lifetime/v1"

    const val Label =
        "com.vexlife.prod.service"

    /** v1 compatibility coordinate. New receipts live at the Mac service lifetime. */
    const val ReceiptRelativePath =
        "runtime/product-service/service-lifetime.json"

    const val LifetimeReceiptFileName =
        "service-lifetime.json"

    const val PlistRelativePath =
        "runtime/product-service/launchd/com.vexlife.prod.service.plist"

    const val StdoutRelativePath =
        "runtime/product-service/logs/service.out.log"

    const val StderrRelativePath =
        "runtime/product-service/logs/service.err.log"

    private const val ReadinessTimeoutMs =
        120_000L

    private const val PollMs =
        100L

    private val GitSha =
        Regex(
            "[0-9a-f]{40}",
        )

    private val Sha256 =
        Regex(
            "[0-9a-f]{64}",
        )

    @Serializable
    data class HomeBinding(
        val homePath: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
    )

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val sourceSha: String,
        val imagePayloadSha256: String,
        val imagePath: String,
        val serviceOrigin: String,
        val launchd: MacUserLaunchdProcess.Receipt,
        val homeBinding: HomeBinding? = null,
    )

    /**
     * The owned service launch contracts this owner has accepted, newest first.
     *
     * A lifetime receipt is validated under the contract it was formed under, never re-derived from
     * today's contract. When the launch specification gains a field, a legitimate prior exact-owned
     * Product must stay recognizable as *owned*: re-deriving today's spec from an old receipt made a
     * coherent prior service (receipt, label, PID, endpoint and source-currentness all agreeing) fail
     * the spec check, so Product entry held it as unknown and could never offer Update & Restart for
     * it. Lived on a certified Mac: installed 8faf58e8 under current b1351c32.
     *
     * Ownership is still proven exactly; only the expected argv comes from the receipt's own era.
     */
    internal enum class LaunchContract {
        /** Current: the service is told which Home it serves. */
        SourceAndHome,

        /**
         * Before c170faa ("Carry selected Home through Product entry") the service took only
         * `--source-sha` and always served the default Home. A receipt of this era is coherent only
         * at that coordinate, so it is admitted nowhere else.
         */
        SourceOnlyDefaultHome,
    }

    internal fun launchArguments(
        contract: LaunchContract,
        sourceSha: String,
        home: Path,
    ): List<String> =
        when (contract) {
            LaunchContract.SourceAndHome -> listOf("--source-sha", sourceSha, "--home", home.toString())
            LaunchContract.SourceOnlyDefaultHome -> listOf("--source-sha", sourceSha)
        }

    /** The Home a pre-selected-Home service served implicitly: `~/.vexlife`. */
    fun legacyDefaultHome(): Path =
        Path.of(System.getProperty("user.home"), ".vexlife").toAbsolutePath().normalize()

    /** Mac-user lifetime of the single Product launchd realization, independent of any one Home. */
    fun defaultLifetimeRoot(
        userHome: Path =
            Path.of(
                System.getProperty(
                    "user.home",
                ),
            ),
    ): Path {
        require(
            userHome.isAbsolute,
        ) {
            "VexLife Product service lifetime requires one absolute macOS user Home"
        }

        return userHome
            .normalize()
            .resolve(
                "Library",
            )
            .resolve(
                "Application Support",
            )
            .resolve(
                "VexLife",
            )
            .resolve(
                "ProductService",
            )
            .normalize()
    }

    /**
     * Which accepted contract formed [receipt]. A receipt that matches none is not owned by any
     * contract this source accepts and stays held (fail closed); it is never coerced into today's.
     */
    internal fun receiptContract(
        receipt: Receipt,
        home: Path,
        legacyDefaultHome: Path,
    ): LaunchContract {
        val arguments = receipt.launchd.programArguments
        return when {
            arguments == launchArguments(LaunchContract.SourceAndHome, receipt.sourceSha, home) ->
                LaunchContract.SourceAndHome

            arguments == launchArguments(LaunchContract.SourceOnlyDefaultHome, receipt.sourceSha, home) &&
                home.toAbsolutePath().normalize() == legacyDefaultHome.toAbsolutePath().normalize() ->
                LaunchContract.SourceOnlyDefaultHome

            else ->
                throw IllegalArgumentException(
                    "product-service launchd receipt matches no accepted launch contract for this Home"
                )
        }
    }

    sealed interface Observation {
        data class Absent(
            val desiredImage: VexLifeProdPrivateServiceImage.Observation,
        ) : Observation

        data class RunningCurrent(
            val desiredImage: VexLifeProdPrivateServiceImage.Observation,
            val receipt: Receipt,
            val sourceCurrentness: VexLifeProdServiceContract.SourceCurrentness,
        ) : Observation

        data class OwnedCurrentNotReady(
            val desiredImage: VexLifeProdPrivateServiceImage.Observation,
            val receipt: Receipt,
            val launchdObservation: MacUserLaunchdProcess.Observation,
            val servesSelectedHome: Boolean,
        ) : Observation

        data class OwnedPriorSource(
            val desiredImage: VexLifeProdPrivateServiceImage.Observation,
            val receipt: Receipt,
            val launchdObservation: MacUserLaunchdProcess.Observation,
        ) : Observation

        data class HeldUnknown(
            val desiredImage: VexLifeProdPrivateServiceImage.Observation,
            val detail: String,
        ) : Observation
    }

    sealed interface ExistingObservation {
        data object Absent : ExistingObservation

        data class RunningOwned(
            val receipt: Receipt,
            val sourceCurrentness: VexLifeProdServiceContract.SourceCurrentness,
        ) : ExistingObservation

        data class OwnedNotReady(
            val receipt: Receipt,
            val launchdObservation: MacUserLaunchdProcess.Observation,
            val servesSelectedHome: Boolean,
        ) : ExistingObservation

        data class HeldUnknown(
            val detail: String,
        ) : ExistingObservation
    }

    data class Result(
        val disposition: String,
        val sourceSha: String,
        val imagePayloadSha256: String,
        val pid: Long,
        val serviceOrigin: String,
        val processStarted: Boolean,
        val priorProcessRetired: Boolean,
        val receiptWritePerformed: Boolean,
    )

    fun interface SourceCurrentnessProbe {
        fun observe(
            origin: String,
        ): VexLifeProdServiceContract.SourceCurrentness?
    }

    object SystemSourceCurrentnessProbe : SourceCurrentnessProbe {
        private val client =
            HttpClient
                .newBuilder()
                .followRedirects(
                    HttpClient.Redirect.NEVER,
                )
                .connectTimeout(
                    Duration.ofSeconds(
                        2,
                    ),
                )
                .build()

        override fun observe(
            origin: String,
        ): VexLifeProdServiceContract.SourceCurrentness? =
            try {
                val request =
                    HttpRequest
                        .newBuilder(
                            URI(
                                origin +
                                    VexLifeProdServiceContract.SourceCurrentnessPath,
                            ),
                        )
                        .timeout(
                            Duration.ofSeconds(
                                2,
                            ),
                        )
                        .GET()
                        .build()
                val response =
                    client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8,
                        ),
                    )

                if (
                    response.statusCode() !=
                    200
                ) {
                    null
                } else {
                    VexContractJson.decode<VexLifeProdServiceContract.SourceCurrentness>(
                        response.body(),
                    )
                }
            } catch (
                _: Exception,
            ) {
                null
            }
    }

    /**
     * Read-only projection of the already-existing Product service before a desired image is
     * installed or any lifecycle effect is admitted.
     *
     * This is intentionally separate from [observe], which evaluates one desired installed image.
     * Front-door UX can therefore explain and ask before install/retire/restart effects.
     */
    fun observeExisting(
        productHome: Path,
        host: MacUserLaunchdProcess.Host =
            MacUserLaunchdProcess.SystemHost,
        probe: SourceCurrentnessProbe =
            SystemSourceCurrentnessProbe,
        legacyDefaultHome: Path = legacyDefaultHome(),
        lifetimeRoot: Path = defaultLifetimeRoot(),
    ): ExistingObservation {
        val home =
            currentHome(
                productHome,
            )
        val selectedHome =
            currentHomeBinding(
                home,
            )
        val receiptResult =
            runCatching {
                readReceipt(
                    home = home,
                    host = host,
                    legacyDefaultHome = legacyDefaultHome,
                    lifetimeRoot = lifetimeRoot,
                )
            }
        if (
            receiptResult.isFailure
        ) {
            return ExistingObservation.HeldUnknown(
                detail =
                    "product-service lifetime receipt is unreadable or invalid: " +
                        (
                            receiptResult.exceptionOrNull()
                                ?.message
                                ?: "unknown receipt failure"
                        ),
            )
        }

        val receipt =
            receiptResult.getOrNull()

        if (
            receipt ==
                null
        ) {
            val coordinate =
                MacUserLaunchdProcess.observeCoordinate(
                    domainTarget =
                        "gui/" +
                            host.uid,
                    label = Label,
                    host = host,
                )
            if (
                coordinate !=
                MacUserLaunchdProcess.Observation.Absent
            ) {
                return ExistingObservation.HeldUnknown(
                    detail =
                        "stable product-service launchd label is present without a product lifetime receipt: " +
                            coordinate,
                )
            }

            val endpoint =
                probe.observe(
                    VexLifeProdServiceContract.Origin,
                )
            return if (
                endpoint ==
                    null
            ) {
                ExistingObservation.Absent
            } else {
                ExistingObservation.HeldUnknown(
                    detail =
                        "product-service endpoint responds without exact product lifetime ownership: " +
                            endpoint.sourceSha +
                            " / pid " +
                            endpoint.processPid,
                )
            }
        }

        val receiptHome =
            receiptHome(
                receipt = receipt,
                legacyHome = home,
            )
        val servesSelectedHome =
            receiptServesSelectedHome(
                receipt = receipt,
                selected = selectedHome,
            )
        val receiptSpec =
            specFromReceipt(
                receiptHome = receiptHome,
                receipt = receipt,
                host = host,
                legacyDefaultHome = legacyDefaultHome,
            )
        val launchd =
            MacUserLaunchdProcess.observe(
                receiptSpec,
                host,
            )

        if (
            launchd is
            MacUserLaunchdProcess.Observation.Unknown
        ) {
            return ExistingObservation.HeldUnknown(
                detail =
                    "product-service launchd observation is unknown: " +
                        launchd.detail,
            )
        }

        if (
            launchd is
                MacUserLaunchdProcess.Observation.Running &&
            launchd.pid !=
                receipt.launchd.pid
        ) {
            return ExistingObservation.HeldUnknown(
                detail =
                    "product-service launchd PID does not match the exact product receipt",
            )
        }

        if (
            launchd is
                MacUserLaunchdProcess.Observation.Running &&
            !host.processAlive(
                receipt.launchd.pid,
            )
        ) {
            return ExistingObservation.HeldUnknown(
                detail =
                    "product-service launchd reports the receipt PID but that process is not live",
            )
        }

        val endpoint =
            probe.observe(
                VexLifeProdServiceContract.Origin,
            )
        val endpointAgrees =
            endpoint !=
                null &&
                validSourceCurrentness(
                    currentness = endpoint,
                    expectedSourceSha = receipt.sourceSha,
                    expectedPid = receipt.launchd.pid,
                )

        if (
            servesSelectedHome &&
            launchd is
                MacUserLaunchdProcess.Observation.Running &&
            MacUserLaunchdProcess.receiptCurrent(
                receipt = receipt.launchd,
                host = host,
                expectedPid = receipt.launchd.pid,
            ) &&
            endpointAgrees
        ) {
            return ExistingObservation.RunningOwned(
                receipt = receipt,
                sourceCurrentness =
                    requireNotNull(
                        endpoint,
                    ),
            )
        }

        if (
            endpoint !=
                null &&
            !endpointAgrees
        ) {
            return ExistingObservation.HeldUnknown(
                detail =
                    "product-service endpoint does not agree with exact owned launchd/receipt currentness",
            )
        }

        return ExistingObservation.OwnedNotReady(
            receipt = receipt,
            launchdObservation = launchd,
            servesSelectedHome = servesSelectedHome,
        )
    }

    fun observe(
        productHome: Path,
        desiredSourceSha: String,
        host: MacUserLaunchdProcess.Host =
            MacUserLaunchdProcess.SystemHost,
        probe: SourceCurrentnessProbe =
            SystemSourceCurrentnessProbe,
        legacyDefaultHome: Path = legacyDefaultHome(),
        lifetimeRoot: Path = defaultLifetimeRoot(),
    ): Observation {
        require(
            GitSha.matches(
                desiredSourceSha,
            ),
        ) {
            "desired VexLife-Prod service source must be one exact Git commit"
        }

        val home =
            currentHome(
                productHome,
            )
        val selectedHome =
            currentHomeBinding(
                home,
            )
        val desiredImagePath =
            VexLifeProdPrivateServiceImageInstaller.installedImagePath(
                productHome = home,
                sourceSha = desiredSourceSha,
            )
        val desiredImage =
            VexLifeProdPrivateServiceImage.observe(
                imageRoot = desiredImagePath,
                expectedSourceSha = desiredSourceSha,
            )
        val desiredSpec =
            spec(
                home = home,
                image = desiredImage,
                host = host,
                createDirectories = false,
            )
        val receiptResult =
            runCatching {
                readReceipt(
                    home = home,
                    host = host,
                    legacyDefaultHome = legacyDefaultHome,
                    lifetimeRoot = lifetimeRoot,
                )
            }
        if (
            receiptResult.isFailure
        ) {
            return Observation.HeldUnknown(
                desiredImage = desiredImage,
                detail =
                    "product-service lifetime receipt is unreadable or invalid: " +
                        (
                            receiptResult.exceptionOrNull()
                                ?.message
                                ?: "unknown receipt failure"
                        ),
            )
        }
        val receipt =
            receiptResult.getOrNull()

        if (
            receipt ==
                null
        ) {
            return when (
                val launchd =
                    MacUserLaunchdProcess.observe(
                        desiredSpec,
                        host,
                    )
            ) {
                MacUserLaunchdProcess.Observation.Absent ->
                    Observation.Absent(
                        desiredImage,
                    )

                else ->
                    Observation.HeldUnknown(
                        desiredImage = desiredImage,
                        detail =
                            "stable product-service launchd label is present without a product lifetime receipt: " +
                                launchd,
                    )
            }
        }

        val receiptHome =
            receiptHome(
                receipt = receipt,
                legacyHome = home,
            )
        val servesSelectedHome =
            receiptServesSelectedHome(
                receipt = receipt,
                selected = selectedHome,
            )
        val receiptSpec =
            specFromReceipt(
                receiptHome = receiptHome,
                receipt = receipt,
                host = host,
                legacyDefaultHome = legacyDefaultHome,
            )
        val launchd =
            MacUserLaunchdProcess.observe(
                receiptSpec,
                host,
            )

        if (
            launchd is
            MacUserLaunchdProcess.Observation.Unknown
        ) {
            return Observation.HeldUnknown(
                desiredImage = desiredImage,
                detail =
                    "product-service launchd observation is unknown: " +
                        launchd.detail,
            )
        }

        if (
            launchd is
                MacUserLaunchdProcess.Observation.Running &&
            launchd.pid !=
                receipt.launchd.pid
        ) {
            return Observation.HeldUnknown(
                desiredImage = desiredImage,
                detail =
                    "product-service launchd PID does not match the exact product receipt",
            )
        }

        if (
            launchd is
                MacUserLaunchdProcess.Observation.Running &&
            !host.processAlive(
                receipt.launchd.pid,
            )
        ) {
            return Observation.HeldUnknown(
                desiredImage = desiredImage,
                detail =
                    "product-service launchd reports the receipt PID but that process is not live",
            )
        }

        if (
            receipt.schemaVersion !=
                ReceiptSchema ||
                receipt.sourceSha !=
                desiredSourceSha ||
                receiptContract(
                    receipt = receipt,
                    home = receiptHome,
                    legacyDefaultHome = legacyDefaultHome,
                ) !=
                LaunchContract.SourceAndHome
        ) {
            return Observation.OwnedPriorSource(
                desiredImage = desiredImage,
                receipt = receipt,
                launchdObservation = launchd,
            )
        }

        if (
            !servesSelectedHome
        ) {
            return Observation.OwnedCurrentNotReady(
                desiredImage = desiredImage,
                receipt = receipt,
                launchdObservation = launchd,
                servesSelectedHome = false,
            )
        }

        require(
            receipt.imagePayloadSha256 ==
                desiredImage.payloadSha256 &&
                Path.of(
                    receipt.imagePath,
                )
                    .normalize() ==
                desiredImagePath.normalize(),
        ) {
            "current product-service receipt does not bind the exact desired private image"
        }
        requireReceiptMatchesSpec(
            receipt.launchd,
            desiredSpec,
        )

        if (
            launchd is
                MacUserLaunchdProcess.Observation.Running &&
            MacUserLaunchdProcess.receiptCurrent(
                receipt = receipt.launchd,
                host = host,
                expectedPid = receipt.launchd.pid,
            )
        ) {
            val currentness =
                probe.observe(
                    VexLifeProdServiceContract.Origin,
                )
            if (
                currentness !=
                    null &&
                validSourceCurrentness(
                    currentness = currentness,
                    expectedSourceSha = desiredSourceSha,
                    expectedPid = receipt.launchd.pid,
                )
            ) {
                return Observation.RunningCurrent(
                    desiredImage = desiredImage,
                    receipt = receipt,
                    sourceCurrentness = currentness,
                )
            }
        }

        return Observation.OwnedCurrentNotReady(
            desiredImage = desiredImage,
            receipt = receipt,
            launchdObservation = launchd,
            servesSelectedHome = true,
        )
    }

    fun startOrReuse(
        productHome: Path,
        desiredSourceSha: String,
        host: MacUserLaunchdProcess.Host =
            MacUserLaunchdProcess.SystemHost,
        probe: SourceCurrentnessProbe =
            SystemSourceCurrentnessProbe,
        legacyDefaultHome: Path = legacyDefaultHome(),
        lifetimeRoot: Path = defaultLifetimeRoot(),
    ): Result {
        val home =
            currentHome(
                productHome,
            )

        return when (
            val observed =
                observe(
                    productHome = home,
                    desiredSourceSha = desiredSourceSha,
                    host = host,
                    probe = probe,
                    legacyDefaultHome = legacyDefaultHome,
                    lifetimeRoot = lifetimeRoot,
                )
        ) {
            is Observation.RunningCurrent ->
                Result(
                        disposition = "REUSED_CURRENT_SERVICE",
                    sourceSha = desiredSourceSha,
                    imagePayloadSha256 = observed.desiredImage.payloadSha256,
                    pid = observed.receipt.launchd.pid,
                    serviceOrigin = VexLifeProdServiceContract.Origin,
                    processStarted = false,
                    priorProcessRetired = false,
                    receiptWritePerformed = false,
                )
                    .also {
                        VexLogger.record(
                            VexLogger.Fact.ServiceReusedCurrent,
                            mapOf("serviceSourceSha" to it.sourceSha, "pid" to it.pid.toString()),
                        )
                    }

            is Observation.HeldUnknown ->
                error(
                    "VexLife-Prod product service ownership is held unknown: " +
                        observed.detail,
                )

            is Observation.Absent ->
                startDesired(
                    home = home,
                    image = observed.desiredImage,
                    host = host,
                    probe = probe,
                    priorProcessRetired = false,
                    lifetimeRoot = lifetimeRoot,
                )

            is Observation.OwnedCurrentNotReady -> {
                retireExact(
                    selectedHome = home,
                    receipt = observed.receipt,
                    host = host,
                    legacyDefaultHome = legacyDefaultHome,
                    lifetimeRoot = lifetimeRoot,
                )
                startDesired(
                    home = home,
                    image = observed.desiredImage,
                    host = host,
                    probe = probe,
                    priorProcessRetired = true,
                    lifetimeRoot = lifetimeRoot,
                )
            }

            is Observation.OwnedPriorSource -> {
                retireExact(
                    selectedHome = home,
                    receipt = observed.receipt,
                    host = host,
                    legacyDefaultHome = legacyDefaultHome,
                    lifetimeRoot = lifetimeRoot,
                )
                startDesired(
                    home = home,
                    image = observed.desiredImage,
                    host = host,
                    probe = probe,
                    priorProcessRetired = true,
                    lifetimeRoot = lifetimeRoot,
                )
            }
        }
    }

    private fun startDesired(
        home: Path,
        image: VexLifeProdPrivateServiceImage.Observation,
        host: MacUserLaunchdProcess.Host,
        probe: SourceCurrentnessProbe,
        priorProcessRetired: Boolean,
        lifetimeRoot: Path,
    ): Result {
        prepareRuntimeDirectories(
            home,
        )
        val desiredSpec =
            spec(
                home = home,
                image = image,
                host = host,
                createDirectories = true,
            )
        val launchd =
            MacUserLaunchdProcess.start(
                desiredSpec,
                host,
            )

        try {
            var currentness: VexLifeProdServiceContract.SourceCurrentness? =
                null
            val deadline =
                host.nowMs() +
                    ReadinessTimeoutMs

            while (
                host.nowMs() <
                deadline
            ) {
                val observed =
                    probe.observe(
                        VexLifeProdServiceContract.Origin,
                    )
                if (
                    observed !=
                        null &&
                    validSourceCurrentness(
                        currentness = observed,
                        expectedSourceSha = image.sourceSha,
                        expectedPid = launchd.pid,
                    )
                ) {
                    currentness =
                        observed
                    break
                }
                host.sleep(
                    PollMs,
                )
            }

            checkNotNull(
                currentness,
            ) {
                "VexLife-Prod service process did not become source/PID-current before timeout"
            }

            require(
                MacUserLaunchdProcess.receiptCurrent(
                    receipt = launchd,
                    host = host,
                    expectedPid = launchd.pid,
                ),
            ) {
                "VexLife-Prod service lost exact launchd/plist ownership before lifetime receipt commit"
            }

            val receipt =
                Receipt(
                    schemaVersion = ReceiptSchema,
                    sourceSha = image.sourceSha,
                    imagePayloadSha256 = image.payloadSha256,
                    imagePath =
                        VexLifeProdPrivateServiceImageInstaller.installedImagePath(
                            productHome = home,
                            sourceSha = image.sourceSha,
                        )
                            .toString(),
                    serviceOrigin = VexLifeProdServiceContract.Origin,
                    launchd = launchd,
                    homeBinding =
                        currentHomeBinding(
                            home,
                        ),
                )

            validateReceipt(
                selectedHome = home,
                receipt = receipt,
                host = host,
                legacyDefaultHome = legacyDefaultHome(),
            )
            writeMachineReceipt(
                lifetimeRoot = lifetimeRoot,
                receipt = receipt,
            )

            VexLogger.record(
                VexLogger.Fact.ServiceStartedCurrent,
                mapOf(
                    "serviceSourceSha" to image.sourceSha,
                    "pid" to launchd.pid.toString(),
                    "priorProcessRetired" to priorProcessRetired.toString(),
                ),
            )
            return Result(
                disposition = "STARTED_CURRENT_SERVICE",
                sourceSha = image.sourceSha,
                imagePayloadSha256 = image.payloadSha256,
                pid = launchd.pid,
                serviceOrigin = VexLifeProdServiceContract.Origin,
                processStarted = true,
                priorProcessRetired = priorProcessRetired,
                receiptWritePerformed = true,
            )
        } catch (
            startupError: Throwable,
        ) {
            try {
                val retired =
                    MacUserLaunchdProcess.retire(
                        receipt = launchd,
                        host = host,
                        expectedPid = launchd.pid,
                    )
                require(
                    retired.exactOwnershipVerified &&
                        retired.verifiedNotLive,
                ) {
                    "new product service was not exactly retired after startup failure"
                }
            } catch (
                cleanupError: Throwable,
            ) {
                throw IllegalStateException(
                    "VexLife-Prod service startup failed and exact cleanup also failed",
                    cleanupError,
                )
            }

            throw startupError
        }
    }

    private fun retireExact(
        selectedHome: Path,
        receipt: Receipt,
        host: MacUserLaunchdProcess.Host,
        legacyDefaultHome: Path,
        lifetimeRoot: Path,
    ) {
        validateReceipt(
            selectedHome = selectedHome,
            receipt = receipt,
            host = host,
            legacyDefaultHome = legacyDefaultHome,
        )
        val retired =
            MacUserLaunchdProcess.retire(
                receipt = receipt.launchd,
                host = host,
                expectedPid = receipt.launchd.pid,
            )
        require(
            retired.exactOwnershipVerified &&
                retired.verifiedNotLive,
        ) {
            "VexLife-Prod prior service was not verified retired"
        }
        val receiptHome =
            receiptHome(
                receipt = receipt,
                legacyHome = selectedHome,
            )
        VexLogger.record(
            VexLogger.Fact.ServiceRetiredVerified,
            mapOf(
                "serviceSourceSha" to receipt.sourceSha,
                "pid" to receipt.launchd.pid.toString(),
                "launchContract" to
                    receiptContract(
                        receipt = receipt,
                        home = receiptHome,
                        legacyDefaultHome = legacyDefaultHome,
                    )
                        .name,
            ),
        )

        deleteReceipt(
            selectedHome = selectedHome,
            receipt = receipt,
            lifetimeRoot = lifetimeRoot,
        )
    }

    private fun readReceipt(
        home: Path,
        host: MacUserLaunchdProcess.Host,
        legacyDefaultHome: Path,
        lifetimeRoot: Path,
    ): Receipt? {
        val machine =
            readMachineReceipt(
                lifetimeRoot,
            )
        if (
            machine !=
                null
        ) {
            require(
                machine.schemaVersion ==
                    ReceiptSchema,
            ) {
                "Mac-lifetime product-service receipt is not the current v2 schema"
            }
            return validateReceipt(
                selectedHome = home,
                receipt = machine,
                host = host,
                legacyDefaultHome = legacyDefaultHome,
            )
        }

        val legacy =
            VexDurableContractFile.read<Receipt>(
                ownedRoot = home,
                path =
                    VexOwnedPath.resolveInside(
                        home,
                        ReceiptRelativePath,
                    ),
            )
                ?: return null
        require(
            legacy.schemaVersion ==
                LegacyReceiptSchema,
        ) {
            "Home-scoped product-service receipt is not the accepted legacy v1 schema"
        }

        return validateReceipt(
            selectedHome = home,
            receipt = legacy,
            host = host,
            legacyDefaultHome = legacyDefaultHome,
        )
    }

    private fun validateReceipt(
        selectedHome: Path,
        receipt: Receipt,
        host: MacUserLaunchdProcess.Host,
        legacyDefaultHome: Path = legacyDefaultHome(),
    ): Receipt {
        require(
            receipt.schemaVersion in
                setOf(
                    LegacyReceiptSchema,
                    ReceiptSchema,
                ) &&
                GitSha.matches(
                    receipt.sourceSha,
                ) &&
                Sha256.matches(
                    receipt.imagePayloadSha256,
                ) &&
                receipt.serviceOrigin ==
                VexLifeProdServiceContract.Origin,
        ) {
            "VexLife-Prod service lifetime receipt identity drift"
        }

        val receiptHome =
            receiptHome(
                receipt = receipt,
                legacyHome = selectedHome,
            )
        val expectedImagePath =
            VexLifeProdPrivateServiceImageInstaller.installedImageCoordinate(
                productHome = receiptHome,
                sourceSha = receipt.sourceSha,
            )
        require(
            Path.of(
                receipt.imagePath,
            )
                .normalize() ==
                expectedImagePath.normalize(),
        ) {
            "VexLife-Prod service lifetime receipt image coordinate drift"
        }

        if (
            receipt.schemaVersion ==
                ReceiptSchema
        ) {
            val binding =
                requireNotNull(
                    receipt.homeBinding,
                ) {
                    "v2 product-service lifetime receipt requires exact Home binding"
                }
            require(
                Path.of(
                    binding.homePath,
                )
                    .isAbsolute &&
                    Path.of(
                        binding.homePath,
                    )
                        .normalize() ==
                    receiptHome &&
                    binding.homeRef.startsWith(
                        "vex-home.device.vexlife.",
                    ) &&
                    binding.deviceRef.startsWith(
                        "device.vexlife.",
                    ) &&
                    binding.companionLineageRef.startsWith(
                        "companion-lineage.vexlife.",
                    ),
            ) {
                "v2 product-service lifetime Home binding is invalid"
            }
        } else {
            require(
                receipt.homeBinding ==
                    null,
            ) {
                "legacy product-service lifetime receipt gained a v2 Home binding"
            }
        }

        MacUserLaunchdProcess.validate(
            receipt.launchd,
        )
        val expectedSpec =
            specFromReceipt(
                receiptHome = receiptHome,
                receipt = receipt,
                host = host,
                legacyDefaultHome = legacyDefaultHome,
            )
        requireReceiptMatchesSpec(
            receipt.launchd,
            expectedSpec,
        )

        return receipt
    }

    private fun specFromReceipt(
        receiptHome: Path,
        receipt: Receipt,
        host: MacUserLaunchdProcess.Host,
        legacyDefaultHome: Path,
    ): MacUserLaunchdProcess.Spec {
        val imagePath =
            Path.of(
                receipt.imagePath,
            )
                .normalize()

        return MacUserLaunchdProcess.form(
            label = Label,
            plistPath =
                runtimeCoordinate(
                    receiptHome,
                    PlistRelativePath,
                )
                    .toString(),
            programPath =
                imagePath
                    .resolve(
                        "bin/vexlife-prod-service",
                    )
                    .toString(),
            programArguments =
                launchArguments(
                    receiptContract(
                        receipt = receipt,
                        home = receiptHome,
                        legacyDefaultHome = legacyDefaultHome,
                    ),
                    receipt.sourceSha,
                    receiptHome,
                ),
            workingDirectory = imagePath.toString(),
            stdoutPath =
                runtimeCoordinate(
                    receiptHome,
                    StdoutRelativePath,
                )
                    .toString(),
            stderrPath =
                runtimeCoordinate(
                    receiptHome,
                    StderrRelativePath,
                )
                    .toString(),
            environment = emptyMap(),
            uid = host.uid,
        )
    }

    private fun spec(
        home: Path,
        image: VexLifeProdPrivateServiceImage.Observation,
        host: MacUserLaunchdProcess.Host,
        createDirectories: Boolean,
    ): MacUserLaunchdProcess.Spec {
        if (
            createDirectories
        ) {
            prepareRuntimeDirectories(
                home,
            )
        }

        return MacUserLaunchdProcess.form(
            label = Label,
            plistPath =
                VexOwnedPath.resolveInside(
                    home,
                    PlistRelativePath,
                )
                    .toString(),
            programPath = image.launcherPath,
            programArguments =
                launchArguments(
                    LaunchContract.SourceAndHome,
                    image.sourceSha,
                    home,
                ),
            workingDirectory =
                requireNotNull(
                    Path.of(
                        image.launcherPath,
                    )
                        .parent
                        ?.parent,
                )
                    .toString(),
            stdoutPath =
                VexOwnedPath.resolveInside(
                    home,
                    StdoutRelativePath,
                )
                    .toString(),
            stderrPath =
                VexOwnedPath.resolveInside(
                    home,
                    StderrRelativePath,
                )
                    .toString(),
            environment = emptyMap(),
            uid = host.uid,
        )
    }

    private fun requireReceiptMatchesSpec(
        receipt: MacUserLaunchdProcess.Receipt,
        spec: MacUserLaunchdProcess.Spec,
    ) {
        require(
            receipt.schemaVersion ==
                spec.schemaVersion &&
                receipt.substrateRef ==
                spec.substrateRef &&
                receipt.label ==
                spec.label &&
                receipt.domainTarget ==
                spec.domainTarget &&
                receipt.plistPath ==
                spec.plistPath &&
                receipt.programPath ==
                spec.programPath &&
                receipt.programArguments ==
                spec.programArguments &&
                receipt.workingDirectory ==
                spec.workingDirectory &&
                receipt.stdoutPath ==
                spec.stdoutPath &&
                receipt.stderrPath ==
                spec.stderrPath &&
                receipt.environment ==
                spec.environment &&
                receipt.keepAlive ==
                spec.keepAlive,
        ) {
            "product-service launchd receipt does not match the exact expected spec"
        }
    }

    private fun prepareRuntimeDirectories(
        home: Path,
    ) {
        for (
            relative in
            listOf(
                "runtime/product-service",
                "runtime/product-service/launchd",
                "runtime/product-service/logs",
            )
        ) {
            VexOwnedPath.ensureDirectory(
                home,
                VexOwnedPath.resolveInside(
                    home,
                    relative,
                ),
            )
        }
    }

    private fun currentHomeBinding(
        home: Path,
    ): HomeBinding {
        val current =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    home,
                ),
            ) {
                "product-service lifetime requires one current canonical Home identity"
            }

        return HomeBinding(
            homePath = current.productHome,
            homeRef = current.manifest.homeRef,
            deviceRef = current.manifest.currentDeviceRef,
            companionLineageRef = current.manifest.currentCompanionLineageRef,
        )
    }

    private fun receiptHome(
        receipt: Receipt,
        legacyHome: Path,
    ): Path =
        receipt.homeBinding
            ?.homePath
            ?.let(
                Path::of,
            )
            ?.toAbsolutePath()
            ?.normalize()
            ?: legacyHome.toAbsolutePath().normalize()

    private fun receiptServesSelectedHome(
        receipt: Receipt,
        selected: HomeBinding,
    ): Boolean =
        receipt.homeBinding
            ?.let {
                it ==
                    selected
            }
            ?: true

    private fun runtimeCoordinate(
        home: Path,
        relative: String,
    ): Path {
        require(
            home.isAbsolute,
        ) {
            "product-service Home coordinate must be absolute"
        }
        val root =
            home.normalize()
        val relativePath =
            Path.of(
                relative,
            )
        require(
            relative.isNotBlank() &&
                !relativePath.isAbsolute,
        ) {
            "product-service runtime coordinate must be one relative path"
        }
        val resolved =
            root
                .resolve(
                    relativePath,
                )
                .normalize()
        require(
            resolved.startsWith(
                root,
            ),
        ) {
            "product-service runtime coordinate escaped its Home"
        }

        return resolved
    }

    private fun readMachineReceipt(
        lifetimeRoot: Path,
    ): Receipt? {
        require(
            lifetimeRoot.isAbsolute,
        ) {
            "VexLife Product service lifetime root must be absolute"
        }
        val root =
            lifetimeRoot.normalize()
        if (
            !Files.exists(
                root,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return null
        }
        val owned =
            VexOwnedPath.requireRoot(
                root,
                "VexLife Product service lifetime root",
            )

        return VexDurableContractFile.read(
            ownedRoot = owned,
            path =
                VexOwnedPath.resolveInside(
                    owned,
                    LifetimeReceiptFileName,
                ),
        )
    }

    private fun writeMachineReceipt(
        lifetimeRoot: Path,
        receipt: Receipt,
    ) {
        require(
            lifetimeRoot.isAbsolute,
        ) {
            "VexLife Product service lifetime root must be absolute"
        }
        Files.createDirectories(
            lifetimeRoot.normalize(),
        )
        val owned =
            VexOwnedPath.requireRoot(
                lifetimeRoot.normalize(),
                "VexLife Product service lifetime root",
            )
        VexDurableContractFile.write(
            ownedRoot = owned,
            path =
                VexOwnedPath.resolveInside(
                    owned,
                    LifetimeReceiptFileName,
                ),
            value = receipt,
        )
    }

    private fun deleteReceipt(
        selectedHome: Path,
        receipt: Receipt,
        lifetimeRoot: Path,
    ) {
        if (
            receipt.schemaVersion ==
                ReceiptSchema
        ) {
            val root =
                lifetimeRoot.normalize()
            if (
                Files.exists(
                    root,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                val owned =
                    VexOwnedPath.requireRoot(
                        root,
                        "VexLife Product service lifetime root",
                    )
                VexDurableContractFile.delete(
                    ownedRoot = owned,
                    path =
                        VexOwnedPath.resolveInside(
                            owned,
                            LifetimeReceiptFileName,
                        ),
                )
            }
        } else {
            VexDurableContractFile.delete(
                ownedRoot = selectedHome,
                path =
                    VexOwnedPath.resolveInside(
                        selectedHome,
                        ReceiptRelativePath,
                    ),
            )
        }
    }

    private fun validSourceCurrentness(
        currentness: VexLifeProdServiceContract.SourceCurrentness,
        expectedSourceSha: String,
        expectedPid: Long,
    ): Boolean =
        currentness.schemaVersion ==
            VexLifeProdServiceContract.SourceCurrentnessSchema &&
            currentness.serviceRef ==
            VexLifeProdServiceContract.ServiceRef &&
            currentness.productRef ==
            VexLifeProdServiceContract.ProductRef &&
            currentness.serviceState ==
            "SOURCE_BOUND_LISTENING" &&
            currentness.sourceSha ==
            expectedSourceSha &&
            currentness.processPid ==
            expectedPid &&
            currentness.frameHostOrigin ==
                VexLifeProdBrowserContract.FrameHostOrigin &&
            !currentness.effectsPerformed

    private fun currentHome(
        productHome: Path,
    ): Path {
        val current =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    productHome,
                ),
            ) {
                "VexLife-Prod product service requires one established Home"
            }

        return VexOwnedPath.requireRoot(
            Path.of(
                current.productHome,
            ),
            "VexLife-Prod Home",
        )
    }
}

// [VXG RealForever]
