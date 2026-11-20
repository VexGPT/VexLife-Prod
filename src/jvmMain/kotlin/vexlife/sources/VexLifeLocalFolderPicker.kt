package vexlife.sources

import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

/**
 * Host realization for one human-only folder selection.
 *
 * The picker proposes a path. It grants nothing; [VexLifeLocalSourceStore.add] remains the only
 * admission/effect owner.
 */
fun interface VexLifeLocalFolderPicker {
    fun chooseFolder(): Result

    sealed interface Result {
        data class Selected(
            val path: Path,
        ) : Result

        data object Cancelled : Result

        data class Unavailable(
            val reason: String,
        ) : Result
    }

    companion object {
        val SystemDesktop =
            VexLifeLocalFolderPicker {
                if (
                    GraphicsEnvironment.isHeadless()
                ) {
                    return@VexLifeLocalFolderPicker Result.Unavailable(
                        "This VexLife host has no graphical folder picker.",
                    )
                }

                val observed =
                    AtomicReference<Result>()

                val choose = {
                    observed.set(
                        if (
                            System.getProperty(
                                "os.name",
                            )
                                .orEmpty()
                                .lowercase()
                                .contains(
                                    "mac",
                                )
                        ) {
                            chooseMacFolder()
                        } else {
                            chooseSwingFolder()
                        },
                    )
                }

                try {
                    if (
                        SwingUtilities.isEventDispatchThread()
                    ) {
                        choose()
                    } else {
                        SwingUtilities.invokeAndWait {
                            choose()
                        }
                    }
                    observed.get()
                        ?: Result.Unavailable(
                            "The folder picker returned no result.",
                        )
                } catch (
                    error: Throwable,
                ) {
                    Result.Unavailable(
                        "The folder picker could not open: " +
                            (
                                error.message
                                    ?: error::class.simpleName
                                    ?: "unknown host failure"
                            ),
                    )
                }
            }

        private fun chooseMacFolder(): Result {
            val property =
                "apple.awt.fileDialogForDirectories"
            val prior =
                System.getProperty(
                    property,
                )

            return try {
                System.setProperty(
                    property,
                    "true",
                )
                val dialog =
                    FileDialog(
                        null as Frame?,
                        "Choose a folder Vex may read",
                        FileDialog.LOAD,
                    )
                dialog.isMultipleMode =
                    false
                dialog.isVisible =
                    true

                val file =
                    dialog.file
                val directory =
                    dialog.directory

                if (
                    file ==
                        null ||
                    directory ==
                        null
                ) {
                    Result.Cancelled
                } else {
                    Result.Selected(
                        Path.of(
                            directory,
                            file,
                        )
                            .toAbsolutePath()
                            .normalize(),
                    )
                }
            } finally {
                if (
                    prior ==
                        null
                ) {
                    System.clearProperty(
                        property,
                    )
                } else {
                    System.setProperty(
                        property,
                        prior,
                    )
                }
            }
        }

        private fun chooseSwingFolder(): Result {
            val chooser =
                JFileChooser()
                    .apply {
                        dialogTitle =
                            "Choose a folder Vex may read"
                        fileSelectionMode =
                            JFileChooser.DIRECTORIES_ONLY
                        isMultiSelectionEnabled =
                            false
                        isAcceptAllFileFilterUsed =
                            false
                    }

            return when (
                chooser.showOpenDialog(
                    null,
                )
            ) {
                JFileChooser.APPROVE_OPTION ->
                    chooser.selectedFile
                        ?.toPath()
                        ?.toAbsolutePath()
                        ?.normalize()
                        ?.let {
                            Result.Selected(
                                it,
                            )
                        }
                        ?: Result.Unavailable(
                            "The folder picker returned no selected directory.",
                        )

                JFileChooser.CANCEL_OPTION ->
                    Result.Cancelled

                else ->
                    Result.Unavailable(
                        "The folder picker did not complete normally.",
                    )
            }
        }
    }
}

// [VXG RealForever]
