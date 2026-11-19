package vexlife.platform.windows

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    fun packagedSourceKeepsWebView2AsPresentationAndProductQuitInTheJvmOwner() {
        val root =
            Path.of(
                    System.getProperty(
                        "user.dir",
                    ),
                )
                .toAbsolutePath()
                .normalize()
        val project =
            Files.readString(
                root.resolve(
                    "packaging/windows-webview2/VexLife.WebShell/VexLife.WebShell.csproj",
                ),
            )
        val shell =
            Files.readString(
                root.resolve(
                    "packaging/windows-webview2/VexLife.WebShell/Program.cs",
                ),
            )
        val window =
            Files.readString(
                root.resolve(
                    "src/jvmMain/kotlin/vexlife/platform/windows/VexLifeProdWindowsSetupWindow.kt",
                ),
            )
        val builder =
            Files.readString(
                root.resolve(
                    "tools/windows-preview/FORM-WINDOWS-PREVIEW.ps1",
                ),
            )

        assertTrue(
            project.contains(
                "<TargetFramework>net10.0-windows</TargetFramework>",
            ),
        )
        assertTrue(
            project.contains(
                "<SelfContained>true</SelfContained>",
            ),
        )
        assertTrue(
            project.contains(
                "Microsoft.Web.WebView2\" Version=\"1.0.4314.35",
            ),
        )
        assertTrue(
            shell.contains(
                "internal const int QuitProductExitCode = 42;",
            ),
        )
        assertTrue(
            shell.contains(
                "args.Handled = true;",
            ),
        )
        assertTrue(
            shell.contains(
                "target.Port == 18210",
            ),
        )
        assertFalse(
            shell.contains(
                "CoreWebView2.AddHostObjectToScript",
            ),
            "first WebToApp shell must not give Product pages ambient native authority",
        )

        assertTrue(
            window.contains(
                "VexLifeProdWindowsWebView2.present(",
            ),
        )
        assertTrue(
            window.contains(
                "VexLifeProdEntryFamily.QuitCurrent",
            ),
        )
        assertTrue(
            window.contains(
                "VexLifeProdEntryFamily.QuitProduct",
            ),
        )
        assertFalse(
            window.contains(
                "Desktop.getDesktop()",
            ),
            "Product presentation must stay inside VexLife instead of the external browser",
        )

        assertTrue(
            builder.contains(
                "'WEBVIEW2_PUBLISH'",
            ),
        )
        assertTrue(
            builder.contains(
                "'--self-contained'",
            ),
        )
        assertTrue(
            builder.contains(
                "app\\webview2",
            ),
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
