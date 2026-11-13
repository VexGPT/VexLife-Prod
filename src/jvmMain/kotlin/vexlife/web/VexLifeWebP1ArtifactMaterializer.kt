package vexlife.web

import java.nio.file.Path
import vexlife.artifact.ExactGeneratedArtifactMaterializer

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
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()

        require(
            bundle.paths.toSet() ==
                ExpectedPaths,
        ) {
            "generated VexLife web bundle paths changed without materializer admission"
        }

        ExactGeneratedArtifactMaterializer.run(
            mode =
                when (
                    mode
                ) {
                    Mode.Write ->
                        ExactGeneratedArtifactMaterializer.Mode.Write

                    Mode.Verify ->
                        ExactGeneratedArtifactMaterializer.Mode.Verify
                },
            root = root,
            contentByPath =
                bundle.paths
                    .associateWith(
                        bundle::content,
                    ),
        )
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
                    "unknown generated VexLife web materializer mode: " +
                        args[
                            0
                        ],
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
