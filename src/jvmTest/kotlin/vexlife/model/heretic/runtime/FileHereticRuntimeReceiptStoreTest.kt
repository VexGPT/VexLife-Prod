package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.platform.macos.MacUserLaunchdProcess

class FileHereticRuntimeReceiptStoreTest {
    private fun receipt(
        productHome: java.nio.file.Path,
    ): HereticPersistentRuntime.Receipt {
        val pid =
            5151L
        val python =
            productHome.resolve(
                "runtime/heretic/python-3.14.7-mlx-lm-0.32.0/bin/python",
            )
        val environment =
            python.parent.parent
        val model =
            productHome.resolve(
                "models/heretic/qwen3.8-27b-mlx4/808b82a2bd04b2bac362b5d0b7a1197aeea59e57",
            )
        val substrate =
            MacUserLaunchdProcess.Receipt(
                schemaVersion = MacUserLaunchdProcess.Schema,
                substrateRef = MacUserLaunchdProcess.SubstrateRef,
                label = "com.vexlife.prod.heretic.test",
                domainTarget = "gui/501",
                plistPath =
                    productHome.resolve(
                        "runtime/heretic/launchd/test.plist",
                    )
                        .toString(),
                programPath = python.toString(),
                programArguments =
                    listOf(
                        "-m",
                        "mlx_lm.server",
                    ),
                workingDirectory = environment.toString(),
                stdoutPath =
                    productHome.resolve(
                        "runtime/heretic/mlx-lm-server.out.log",
                    )
                        .toString(),
                stderrPath =
                    productHome.resolve(
                        "runtime/heretic/mlx-lm-server.err.log",
                    )
                        .toString(),
                environment = emptyMap(),
                keepAlive = false,
                pid = pid,
            )

        return HereticPersistentRuntime.Receipt(
            schemaVersion = HereticPersistentRuntime.ReceiptSchema,
            receiptRef = "receipt.vexlife-prod.heretic-persistent-runtime.test",
            state = HereticRuntimeState.Qualified,
            formedAt = "2026-11-04T17:30:00-08:00",
            authorityRef = "authority.vexlife-prod.heretic-runtime.test",
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactCustodyEvidenceRef = HereticProductIdentity.ArtifactCustodyEvidenceRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            transportClass = HereticProductIdentity.TransportClass,
            hostExecutionProfileRef = HereticProductIdentity.HostExecutionProfileRef,
            pythonVersion = HereticProductIdentity.PythonVersion,
            mlxLmVersion = HereticProductIdentity.MlxLmVersion,
            trainerSourceCommit = HereticProductIdentity.TrainerSourceCommit,
            modelPath = model.toString(),
            pythonPath = python.toString(),
            pythonEnvironmentRoot = environment.toString(),
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            implementationIdentity =
                HereticPersistentRuntime.currentImplementationIdentity(),
            runtime =
                HereticPersistentRuntime.RuntimeProcess(
                    pid = pid,
                    processGroupId = pid,
                    exactOwnedDetachedGroup = true,
                    args =
                        listOf(
                            "-m",
                            "mlx_lm.server",
                        ),
                    processSubstrate = substrate,
                ),
            effects = HereticRuntimeEffectBoundary.QualifiedRuntime,
            qualification = HereticRuntimeQualification.qualified(),
        )
    }

    @Test
    fun runtimeReceiptRoundTripsAtOneProdHomeOwnedPath() {
        val productHome =
            Files.createTempDirectory(
                "vexlife-prod-runtime-receipt-",
            )
                .toAbsolutePath()
        val store =
            FileHereticRuntimeReceiptStore(
                productHome,
            )
        val value =
            receipt(
                productHome,
            )

        try {
            assertNull(
                store.read(),
            )

            store.write(
                value,
            )

            assertEquals(
                productHome.resolve(
                    HereticProductIdentity.ProductRuntimeReceiptRelativePath,
                )
                    .normalize(),
                store.path(),
            )
            assertEquals(
                value,
                store.read(),
            )

            store.delete()

            assertNull(
                store.read(),
            )
            assertFalse(
                Files.exists(
                    store.path(),
                ),
            )
        } finally {
            productHome
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun symlinkedRuntimeDirectoryCannotRedirectReceiptCustody() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-runtime-receipt-link-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )
        val outside =
            parent.resolve(
                "outside",
            )

        try {
            Files.createDirectory(
                productHome,
            )
            Files.createDirectory(
                outside,
            )
            Files.createSymbolicLink(
                productHome.resolve(
                    "runtime",
                ),
                outside,
            )

            val store =
                FileHereticRuntimeReceiptStore(
                    productHome,
                )

            assertFailsWith<IllegalArgumentException> {
                store.read()
            }
            assertFailsWith<IllegalArgumentException> {
                store.write(
                    receipt(
                        productHome,
                    ),
                )
            }
            assertFalse(
                Files.exists(
                    outside.resolve(
                        "heretic/persistent-runtime-owner-receipt.json",
                    ),
                ),
            )
        } finally {
            Files.deleteIfExists(
                productHome.resolve(
                    "runtime",
                ),
            )
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun receiptPathCannotEscapeProdHome() {
        val productHome =
            Files.createTempDirectory(
                "vexlife-prod-runtime-path-",
            )
                .toAbsolutePath()

        try {
            val store =
                FileHereticRuntimeReceiptStore(
                    productHome,
                )

            assertTrue(
                store.path()
                    .startsWith(
                        productHome,
                    ),
            )
        } finally {
            productHome
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
