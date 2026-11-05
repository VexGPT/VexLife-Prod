package vexlife.web

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal object VexLifeWebP1ArtifactMaterializer {
    enum class Mode {
        Write,
        Verify,
    }

    private val ExpectedPaths =
        setOf(
            "index.html",
            "vexweb.css",
            "vexweb.js",
            "vexweb-manifest.json",
        )

    fun run(
        mode: Mode,
        root: Path,
    ) {
        val target =
            root
                .toAbsolutePath()
                .normalize()

        require(
            !Files.exists(target) ||
                !Files.isSymbolicLink(target),
        ) {
            "generated VexLife web root cannot be a symlink"
        }

        when (mode) {
            Mode.Write ->
                write(
                    target,
                )

            Mode.Verify ->
                verify(
                    target,
                )
        }
    }

    private fun write(
        root: Path,
    ) {
        Files.createDirectories(
            root,
        )
        requireNoUnownedMembers(
            root,
        )

        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()

        require(
            bundle.paths.toSet() ==
                ExpectedPaths,
        ) {
            "generated VexLife web bundle paths changed without materializer admission"
        }

        bundle.paths.forEach { name ->
            writeExact(
                target = root.resolve(name),
                content = bundle.content(name),
            )
        }

        verify(
            root,
        )
    }

    private fun verify(
        root: Path,
    ) {
        require(
            Files.isDirectory(
                root,
            ) &&
                !Files.isSymbolicLink(
                    root,
                ),
        ) {
            "generated VexLife web root is unavailable or unsafe"
        }

        val observed =
            Files.list(
                root,
            )
                .use { stream ->
                    stream
                        .map {
                            it.fileName
                                .toString()
                        }
                        .toList()
                        .toSet()
                }

        require(
            observed ==
                ExpectedPaths,
        ) {
            "generated VexLife web artifact membrane drift: expected=$ExpectedPaths observed=$observed"
        }

        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()

        ExpectedPaths.forEach { name ->
            val path =
                root.resolve(
                    name,
                )

            require(
                Files.isRegularFile(
                    path,
                ) &&
                    !Files.isSymbolicLink(
                        path,
                    ),
            ) {
                "generated VexLife web artifact is not one regular file: $name"
            }

            val observedText =
                Files.readString(
                    path,
                    StandardCharsets.UTF_8,
                )

            require(
                observedText ==
                    bundle.content(
                        name,
                    ),
            ) {
                "generated VexLife web artifact drift: $name"
            }
        }
    }

    private fun requireNoUnownedMembers(
        root: Path,
    ) {
        val observed =
            Files.list(
                root,
            )
                .use { stream ->
                    stream
                        .map {
                            it.fileName
                                .toString()
                        }
                        .toList()
                        .toSet()
                }

        require(
            observed.all {
                it in
                    ExpectedPaths
            },
        ) {
            "generated VexLife web root contains unowned files: $observed"
        }
    }

    private fun writeExact(
        target: Path,
        content: String,
    ) {
        require(
            target.fileName
                .toString() in
                ExpectedPaths,
        )

        val forming =
            target.resolveSibling(
                ".${target.fileName}.forming",
            )

        require(
            !Files.exists(forming),
        ) {
            "stale generated-artifact formation file exists: ${forming.fileName}"
        }

        Files.writeString(
            forming,
            content,
            StandardCharsets.UTF_8,
        )

        try {
            Files.move(
                forming,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (
            _: AtomicMoveNotSupportedException,
        ) {
            Files.move(
                forming,
                target,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }
}

fun main(
    args: Array<String>,
) {
    require(
        args.size ==
            2,
    ) {
        "usage: <write|verify> <output-directory>"
    }

    val mode =
        when (
            args[
                0
            ]
        ) {
            "write" ->
                VexLifeWebP1ArtifactMaterializer.Mode.Write

            "verify" ->
                VexLifeWebP1ArtifactMaterializer.Mode.Verify

            else ->
                error(
                    "unknown generated VexLife web materializer mode: ${args[0]}",
                )
        }

    VexLifeWebP1ArtifactMaterializer.run(
        mode = mode,
        root =
            Path.of(
                args[
                    1
                ],
            ),
    )
}

// [VXG RealForever]
