package vexlife.platform.windows

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore

class VexLifeProdWindowsRuntimeSessionTest {
    @Test
    fun exactWindowsQualifiedRuntimeBecomesPortableBoundSession() {
        val parent =
            Files.createTempDirectory(
                "vexlife-windows-runtime-session-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        var startCalls =
            0
        var ensureCalls =
            0
        var closeCalls =
            0

        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val model =
                HereticModelCatalog.Models.Qwen4B
            FileHereticModelConfigurationStore(
                home,
            )
                .selectModel(
                    manifest = fixture.manifest,
                    model = model,
                )

            val realization =
                HereticRuntimeRealization.WindowsQwen4B
            val evidence =
                VexLifeWindowsHereticRuntime.modelConfigurationEvidence(
                    VexLifeWindowsHereticRuntime.qualifiedEvidence(
                        realization = realization,
                        processPid = 4242L,
                    ),
                )
            val ready =
                readyObservation(
                    home = home,
                    realization = realization,
                )

            val session =
                VexLifeProdWindowsRuntimeSession.establish(
                    productHome = home,
                    realization = realization,
                    allowAcquisition = false,
                    allowRuntimeStart = true,
                    hooks =
                        VexLifeProdWindowsRuntimeSession.Hooks(
                            observeMaterialization = {
                                observedHome,
                                observedRealization,
                                ->
                                assertEquals(
                                    home,
                                    observedHome,
                                )
                                assertEquals(
                                    realization,
                                    observedRealization,
                                )
                                ready
                            },
                            ensureMaterialization = {
                                _,
                                _,
                                _,
                                ->
                                ensureCalls +=
                                    1
                                error(
                                    "reuse-only Session must not acquire dependencies",
                                )
                            },
                            startRuntime = {
                                observedHome,
                                observedRealization,
                                ->
                                startCalls +=
                                    1
                                assertEquals(
                                    home,
                                    observedHome,
                                )
                                assertEquals(
                                    realization,
                                    observedRealization,
                                )
                                VexLifeProdWindowsRuntimeSession.RuntimeHandle(
                                    evidence = evidence,
                                    observeCurrent = {
                                        evidence
                                    },
                                    closeRuntime = {
                                        closeCalls +=
                                            1
                                    },
                                )
                            },
                        ),
                )

            assertEquals(
                VexLifeProdRuntimeSession.ReadyState,
                session.foundation.state,
            )
            assertEquals(
                home.toString(),
                session.foundation.productHome,
            )
            assertEquals(
                realization.realizationRef,
                session.foundation.qualifiedRuntimeEvidence.realizationRef,
            )
            assertEquals(
                evidence,
                session.currentQualifiedRuntimeEvidence(),
            )
            assertEquals(
                evidence,
                session.observeCurrentQualifiedRuntimeEvidence(),
            )
            assertEquals(
                1,
                startCalls,
            )
            assertEquals(
                0,
                ensureCalls,
            )

            val bound =
                assertIs<FileHereticModelConfigurationStore.Snapshot.Heretic>(
                    FileHereticModelConfigurationStore(
                        home,
                    )
                        .read(
                            fixture.manifest,
                        ),
                )
                    .value
            assertEquals(
                HereticModelConfiguration.State.BoundProductModel,
                bound.state,
            )
            assertEquals(
                realization.realizationRef,
                bound.realizationRef,
            )
            assertEquals(
                realization.runtimeReceiptRelativePath,
                bound.runtimeReceiptPath,
            )

            session.close()
            assertEquals(
                1,
                closeCalls,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun windowsSessionWithoutRuntimeStartAuthorityPerformsNoRuntimeEffect() {
        val parent =
            Files.createTempDirectory(
                "vexlife-windows-runtime-session-held-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        var started =
            false

        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val model =
                HereticModelCatalog.Models.Qwen4B
            FileHereticModelConfigurationStore(
                home,
            )
                .selectModel(
                    manifest = fixture.manifest,
                    model = model,
                )
            val realization =
                HereticRuntimeRealization.WindowsQwen4B

            val result =
                runCatching {
                    VexLifeProdWindowsRuntimeSession.establish(
                        productHome = home,
                        realization = realization,
                        allowAcquisition = false,
                        allowRuntimeStart = false,
                        hooks =
                            VexLifeProdWindowsRuntimeSession.Hooks(
                                observeMaterialization = {
                                    _,
                                    _,
                                    ->
                                    readyObservation(
                                        home = home,
                                        realization = realization,
                                    )
                                },
                                startRuntime = {
                                    _,
                                    _,
                                    ->
                                    started =
                                        true
                                    error(
                                        "runtime must not start without authority",
                                    )
                                },
                            ),
                    )
                }

            assertTrue(
                result.isFailure,
            )
            assertFalse(
                started,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun readyObservation(
        home: java.nio.file.Path,
        realization: HereticRuntimeRealization.Value,
    ): VexLifeWindowsRuntimeMaterialization.Observation {
        val root =
            home.resolve(
                "runtime-session-test",
            )
        return VexLifeWindowsRuntimeMaterialization.Observation(
            realizationRef =
                realization.realizationRef,
            modelPath =
                root.resolve(
                    "model.gguf",
                )
                    .toString(),
            modelState =
                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
            runtimeArchivePath =
                root.resolve(
                    "runtime.zip",
                )
                    .toString(),
            runtimeArchiveState =
                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
            runtimeImagePath =
                root.resolve(
                    "image",
                )
                    .toString(),
            runtimeImageState =
                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
            runtimeExecutablePath =
                root.resolve(
                    "image/llama-server.exe",
                )
                    .toString(),
            runtimeExecutableState =
                VexLifeWindowsRuntimeMaterialization.FileState.VERIFIED,
            effectsPerformed = false,
        )
    }
}

// [VXG RealForever]
