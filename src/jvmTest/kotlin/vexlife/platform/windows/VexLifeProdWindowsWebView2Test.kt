package vexlife.platform.windows

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vexlife.service.VexLifeProdServiceContract

class VexLifeProdWindowsWebView2Test {
    @Test
    fun exactProductOriginIsCarriedAndClosePresentationDoesNotMeanQuit() {
        val helper =
            Files.createTempFile(
                "vexlife-webview2-shell-",
                ".exe",
            )
                .toAbsolutePath()
        var observedArgv: List<String>? =
            null

        val disposition =
            VexLifeProdWindowsWebView2.presentWithHooks(
                origin = VexLifeProdServiceContract.Origin,
                quitLabel = "Quit VexLife",
                hooks =
                    VexLifeProdWindowsWebView2.Hooks(
                        helperPath = {
                            helper
                        },
                        run = {
                            argv ->
                            observedArgv =
                                argv
                            VexLifeProdWindowsWebView2.ClosePresentationExitCode
                        },
                    ),
            )

        assertEquals(
            VexLifeProdWindowsWebView2.Disposition.ClosePresentation,
            disposition,
        )
        assertEquals(
            listOf(
                helper.toString(),
                "--origin",
                VexLifeProdServiceContract.Origin,
                "--quit-label",
                "Quit VexLife",
            ),
            observedArgv,
        )
    }

    @Test
    fun quitExitMapsToQuitProductOnly() {
        val helper =
            Files.createTempFile(
                "vexlife-webview2-shell-quit-",
                ".exe",
            )
                .toAbsolutePath()

        val disposition =
            VexLifeProdWindowsWebView2.presentWithHooks(
                origin = VexLifeProdServiceContract.Origin + "/",
                quitLabel = "Quit VexLife",
                hooks =
                    VexLifeProdWindowsWebView2.Hooks(
                        helperPath = {
                            helper
                        },
                        run = {
                            VexLifeProdWindowsWebView2.QuitProductExitCode
                        },
                    ),
            )

        assertEquals(
            VexLifeProdWindowsWebView2.Disposition.QuitProduct,
            disposition,
        )
    }

    @Test
    fun externalTopLevelOriginAndUnknownExitFailClosed() {
        val helper =
            Files.createTempFile(
                "vexlife-webview2-shell-held-",
                ".exe",
            )
                .toAbsolutePath()

        assertFailsWith<IllegalArgumentException> {
            VexLifeProdWindowsWebView2.presentWithHooks(
                origin = "https://example.com/",
                quitLabel = "Quit VexLife",
                hooks =
                    VexLifeProdWindowsWebView2.Hooks(
                        helperPath = {
                            helper
                        },
                        run = {
                            error(
                                "external origin must fail before process execution",
                            )
                        },
                    ),
            )
        }

        val failed =
            runCatching {
                VexLifeProdWindowsWebView2.presentWithHooks(
                    origin = VexLifeProdServiceContract.Origin,
                    quitLabel = "Quit VexLife",
                    hooks =
                        VexLifeProdWindowsWebView2.Hooks(
                            helperPath = {
                                helper
                            },
                            run = {
                                70
                            },
                        ),
                )
            }

        assertTrue(
            failed.isFailure,
        )
        assertTrue(
            failed
                .exceptionOrNull()
                ?.message
                ?.contains(
                    "exit 70",
                ) ==
                true,
        )
    }
}

// [VXG RealForever]
