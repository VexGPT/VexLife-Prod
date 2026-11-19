package vexlife.platform.windows

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import vexlife.service.VexLifeProdServiceContract

/**
 * Windows WebToApp presentation realization.
 *
 * Product meaning stays with VexLifeProdEntryFamily and Product lifetime stays with RunningProduct.
 * This owner only finds and runs the packaged WebView2 presentation process, carrying one admitted
 * local Product origin into it and mapping its terminal presentation result back to the JVM.
 */
internal object VexLifeProdWindowsWebView2 {
    const val ClosePresentationExitCode = 0
    const val QuitProductExitCode = 42

    enum class Disposition {
        ClosePresentation,
        QuitProduct,
    }

    internal data class Hooks(
        val helperPath: () -> Path = ::packagedHelperPath,
        val run: (List<String>) -> Int = ::runProcess,
    )

    fun present(
        origin: String,
        quitLabel: String,
    ): Disposition =
        presentWithHooks(
            origin = origin,
            quitLabel = quitLabel,
            hooks = Hooks(),
        )

    internal fun presentWithHooks(
        origin: String,
        quitLabel: String,
        hooks: Hooks,
    ): Disposition {
        requireExactProductOrigin(
            origin,
        )
        require(
            quitLabel.isNotBlank(),
        ) {
            "Windows WebView2 presentation requires one non-empty Quit label"
        }

        val helper =
            hooks
                .helperPath()
                .toAbsolutePath()
                .normalize()
        require(
            Files.isRegularFile(
                helper,
            )
        ) {
            "packaged VexLife WebView2 shell is unavailable: $helper"
        }

        val exitCode =
            hooks.run(
                listOf(
                    helper.toString(),
                    "--origin",
                    VexLifeProdServiceContract.Origin,
                    "--quit-label",
                    quitLabel,
                ),
            )

        return dispositionForExitCode(
            exitCode,
        )
    }

    internal fun dispositionForExitCode(
        exitCode: Int,
    ): Disposition =
        when (
            exitCode
        ) {
            ClosePresentationExitCode ->
                Disposition.ClosePresentation

            QuitProductExitCode ->
                Disposition.QuitProduct

            else ->
                error(
                    "VexLife WebView2 presentation failed with exit $exitCode",
                )
        }

    internal fun requireExactProductOrigin(
        origin: String,
    ) {
        val uri =
            runCatching {
                    URI.create(
                        origin,
                    )
                }
                .getOrElse {
                    error(
                        "Windows WebView2 presentation Product origin is invalid",
                    )
                }

        require(
            uri.scheme == "http" &&
                uri.host == VexLifeProdServiceContract.Host &&
                uri.port == VexLifeProdServiceContract.Port &&
                (
                    uri.path.isNullOrEmpty() ||
                        uri.path == "/"
                ) &&
                uri.query == null &&
                uri.fragment == null
        ) {
            "Windows WebView2 presentation admits only the exact local Product origin"
        }
    }

    private fun packagedHelperPath(): Path {
        val codeSource =
            Path.of(
                    VexLifeProdWindowsWebView2::class.java
                        .protectionDomain
                        .codeSource
                        .location
                        .toURI(),
                )
                .toAbsolutePath()
                .normalize()
        val appRoot =
            if (
                Files.isRegularFile(
                    codeSource,
                )
            ) {
                requireNotNull(
                    codeSource.parent,
                )
            } else {
                codeSource
            }

        return appRoot
            .resolve(
                "webview2",
            )
            .resolve(
                "VexLife.WebShell.exe",
            )
    }

    private fun runProcess(
        argv: List<String>,
    ): Int {
        require(
            argv.isNotEmpty(),
        ) {
            "Windows WebView2 presentation process argv is empty"
        }

        val process =
            ProcessBuilder(
                    argv,
                )
                .inheritIO()
                .start()

        return process.waitFor()
    }
}

// [VXG RealForever]
