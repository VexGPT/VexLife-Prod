package vexlife.web

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VexLifeWebP1ArtifactMaterializerTest {
    @Test
    fun materializerWritesExactBundleAndFailsClosedOnDrift() {
        val root =
            Files.createTempDirectory(
                "vexlife-web-p1-materializer-test-",
            )

        try {
            VexLifeWebP1ArtifactMaterializer.run(
                mode = VexLifeWebP1ArtifactMaterializer.Mode.Write,
                root = root,
            )

            assertTrue(
                Files.exists(
                    root.resolve(
                        "index.html",
                    ),
                ),
            )

            VexLifeWebP1ArtifactMaterializer.run(
                mode = VexLifeWebP1ArtifactMaterializer.Mode.Verify,
                root = root,
            )

            Files.writeString(
                root.resolve(
                    "vexweb.css",
                ),
                "drift",
                StandardCharsets.UTF_8,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeWebP1ArtifactMaterializer.run(
                    mode = VexLifeWebP1ArtifactMaterializer.Mode.Verify,
                    root = root,
                )
            }

            Files.delete(
                root.resolve(
                    "vexweb.css",
                ),
            )
            VexLifeWebP1ArtifactMaterializer.run(
                mode = VexLifeWebP1ArtifactMaterializer.Mode.Write,
                root = root,
            )
            Files.writeString(
                root.resolve(
                    "extra.txt",
                ),
                "unowned",
                StandardCharsets.UTF_8,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeWebP1ArtifactMaterializer.run(
                    mode = VexLifeWebP1ArtifactMaterializer.Mode.Verify,
                    root = root,
                )
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
