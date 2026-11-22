package vexlife.local

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.service.VexLifeProdMacServiceLifetime

/**
 * Bounded custody handoff for one exact Product-owned Heretic runtime during a Mac service restart.
 *
 * Setup already owns one exact runtime Session before it rotates a prior Product service. This owner
 * lets that prior service relinquish only its runtime-release duty when setup is still alive and the
 * exact service/runtime tuple is unchanged. The handoff is short-lived and Home-owned; it is not a
 * runtime truth store, a KeepAlive policy, or authority to reuse a different model/runtime.
 */
internal object VexLifeProdMacRuntimeHandoff {
    const val SchemaVersion =
        "vexlife-prod.mac-runtime-restart-handoff/v1"

    const val RelativePath =
        "runtime/vexlife-prod/mac-runtime-restart-handoff.json"

    internal const val LifetimeMs =
        15_000L

    private val GitSha =
        Regex(
            "[0-9a-f]{40}",
        )

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val priorServiceSourceSha: String,
        val priorServicePid: Long,
        val runtimeReceiptRef: String,
        val runtimePid: Long,
        val setupPid: Long,
        val formedAtEpochMs: Long,
        val expiresAtEpochMs: Long,
    ) {
        init {
            require(
                schemaVersion ==
                    SchemaVersion &&
                    GitSha.matches(
                        priorServiceSourceSha,
                    ) &&
                    priorServicePid >
                    0 &&
                    runtimeReceiptRef.isNotBlank() &&
                    runtimePid >
                    0 &&
                    setupPid >
                    0 &&
                    formedAtEpochMs >=
                    0 &&
                    expiresAtEpochMs >
                    formedAtEpochMs,
            ) {
                "Mac runtime restart handoff receipt is invalid"
            }
        }
    }

    data class Preparation(
        val state: String,
        val receipt: Receipt?,
        val effectsPerformed: Boolean,
    )

    fun prepare(
        productHome: Path,
        priorService: VexLifeProdMacServiceLifetime.Receipt,
        expectedRuntime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        setupPid: Long =
            ProcessHandle
                .current()
                .pid(),
        nowMs: Long =
            System.currentTimeMillis(),
        processAlive: (Long) -> Boolean =
            ::systemProcessAlive,
    ): Preparation {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        clear(
            home,
        )

        val serviceHome =
            priorService.homeBinding
                ?.homePath
                ?.let(
                    Path::of,
                )
                ?.toAbsolutePath()
                ?.normalize()
                ?: return held(
                    "PRIOR_SERVICE_HOME_UNBOUND",
                )
        if (
            serviceHome !=
            home
        ) {
            return held(
                "PRIOR_SERVICE_HOME_MISMATCH",
            )
        }

        val servicePid =
            priorService.launchd.pid
        if (
            !processAlive(
                setupPid,
            ) ||
            !processAlive(
                servicePid,
            )
        ) {
            return held(
                "HANDOFF_PROCESS_NOT_LIVE",
            )
        }

        val runtime =
            FileHereticRuntimeReceiptStore(
                home,
            )
                .read()
                ?: return held(
                    "PRODUCT_RUNTIME_RECEIPT_ABSENT",
                )
        if (
            runtime.state !=
                HereticRuntimeState.Qualified ||
            runtime.receiptRef !=
                expectedRuntime.runtimeReceiptRef ||
            runtime.modelRef !=
                expectedRuntime.modelRef ||
            !processAlive(
                runtime.runtime.pid,
            )
        ) {
            return held(
                "PRODUCT_RUNTIME_NOT_EXACT_CURRENT",
            )
        }

        val receipt =
            Receipt(
                schemaVersion =
                    SchemaVersion,
                priorServiceSourceSha =
                    priorService.sourceSha,
                priorServicePid =
                    servicePid,
                runtimeReceiptRef =
                    runtime.receiptRef,
                runtimePid =
                    runtime.runtime.pid,
                setupPid =
                    setupPid,
                formedAtEpochMs =
                    nowMs,
                expiresAtEpochMs =
                    nowMs +
                        LifetimeMs,
            )
        VexDurableContractFile.write(
            ownedRoot =
                home,
            path =
                path(
                    home,
                ),
            value =
                receipt,
        )
        return Preparation(
            state =
                "PREPARED",
            receipt =
                receipt,
            effectsPerformed =
                true,
        )
    }

    /**
     * Consume one exact handoff at old-service close.
     *
     * false means ordinary service cleanup remains authoritative and the runtime must be released.
     */
    fun consumeForServiceClose(
        productHome: Path,
        serviceSourceSha: String,
        servicePid: Long =
            ProcessHandle
                .current()
                .pid(),
        nowMs: Long =
            System.currentTimeMillis(),
        processAlive: (Long) -> Boolean =
            ::systemProcessAlive,
    ): Boolean {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val receipt =
            VexDurableContractFile.read<Receipt>(
                ownedRoot =
                    home,
                path =
                    path(
                        home,
                    ),
            )
                ?: return false

        if (
            nowMs >
                receipt.expiresAtEpochMs ||
            !processAlive(
                receipt.setupPid,
            )
        ) {
            clear(
                home,
            )
            return false
        }

        if (
            receipt.priorServiceSourceSha !=
                serviceSourceSha ||
            receipt.priorServicePid !=
                servicePid
        ) {
            return false
        }

        val runtime =
            FileHereticRuntimeReceiptStore(
                home,
            )
                .read()
                ?: return false
        if (
            runtime.state !=
                HereticRuntimeState.Qualified ||
            runtime.receiptRef !=
                receipt.runtimeReceiptRef ||
            runtime.runtime.pid !=
                receipt.runtimePid ||
            !processAlive(
                runtime.runtime.pid,
            )
        ) {
            return false
        }

        clear(
            home,
        )
        return true
    }

    fun clear(
        productHome: Path,
    ) {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        VexDurableContractFile.delete(
            ownedRoot =
                home,
            path =
                path(
                    home,
                ),
        )
    }

    private fun held(
        state: String,
    ): Preparation =
        Preparation(
            state =
                state,
            receipt =
                null,
            effectsPerformed =
                false,
        )

    private fun path(
        productHome: Path,
    ): Path =
        VexOwnedPath.resolveInside(
            productHome,
            RelativePath,
        )

    private fun systemProcessAlive(
        pid: Long,
    ): Boolean =
        ProcessHandle
            .of(
                pid,
            )
            .map(
                ProcessHandle::isAlive,
            )
            .orElse(
                false,
            )
}

// [VXG RealForever]
