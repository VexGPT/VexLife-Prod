package vexlife.continuity

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VexLifeBgeM3ArtifactMaterializationTest {
    @Test
    fun qualifiedManifestIsExactSevenMemberObservedSet() {
        assertEquals(
            7,
            VexLifeBgeM3ArtifactMaterialization.Members.size,
        )
        assertEquals(
            2_289_700_238L,
            VexLifeBgeM3ArtifactMaterialization.TotalBytes,
        )
        assertEquals(
            setOf(
                "onnx/model.onnx",
                "onnx/model.onnx_data",
                "onnx/config.json",
                "onnx/tokenizer.json",
                "onnx/tokenizer_config.json",
                "onnx/special_tokens_map.json",
                "onnx/sentencepiece.bpe.model",
            ),
            VexLifeBgeM3ArtifactMaterialization.Members
                .map {
                    it.relativePath
                }
                .toSet(),
        )
        assertTrue(
            VexLifeBgeM3ArtifactMaterialization.Members.all {
                member ->
                member.sourceUrl ==
                    "https://huggingface.co/BAAI/bge-m3/resolve/" +
                    VexLifeBgeM3DenseIdentity.Revision +
                    "/" +
                    member.relativePath
            },
        )
        assertTrue(
            VexLifeBgeM3ArtifactMaterialization.Members.none {
                it.relativePath ==
                    "onnx/Constant_7_attr__value"
            },
            "upstream directory membership must not silently expand the qualified consumed artifact set",
        )
    }

    @Test
    fun missingHomeArtifactIsObservedWithoutMutationAndAcquisitionRemainsHeld() {
        val home =
            Files.createTempDirectory(
                "vexlife-bge-artifact-",
            )
                .toAbsolutePath()

        try {
            val before =
                VexLifeBgeM3ArtifactMaterialization.observe(
                    home,
                )
            assertEquals(
                VexLifeBgeM3ArtifactMaterialization.PresenceState.ABSENT,
                before.state,
            )
            assertEquals(
                0L,
                before.verifiedBytes,
            )
            assertTrue(
                before.members.all {
                    it.state ==
                        VexLifeBgeM3ArtifactMaterialization.MemberState.MISSING
                },
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeBgeM3ArtifactMaterialization.ensure(
                    productHome =
                        home,
                    allowAcquisition =
                        false,
                )
            }

            val after =
                VexLifeBgeM3ArtifactMaterialization.observe(
                    home,
                )
            assertEquals(
                before,
                after,
                "held acquisition must not create runtime/continuity directories",
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun conflictingExistingMemberIsDamageNotPermissionToRepair() {
        val home =
            Files.createTempDirectory(
                "vexlife-bge-artifact-damaged-",
            )
                .toAbsolutePath()

        try {
            val root =
                VexLifeBgeM3ArtifactMaterialization.modelRoot(
                    home,
                )
            Files.createDirectories(
                root.resolve(
                    "onnx",
                ),
            )
            Files.writeString(
                root.resolve(
                    "onnx/config.json",
                ),
                "{}",
            )

            val observed =
                VexLifeBgeM3ArtifactMaterialization.observe(
                    home,
                )
            assertEquals(
                VexLifeBgeM3ArtifactMaterialization.PresenceState.DAMAGED,
                observed.state,
            )
            assertTrue(
                observed.members.any {
                    it.member.relativePath ==
                        "onnx/config.json" &&
                        it.state ==
                        VexLifeBgeM3ArtifactMaterialization.MemberState.INVALID_SIZE
                },
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeBgeM3ArtifactMaterialization.ensure(
                    productHome =
                        home,
                    allowAcquisition =
                        true,
                )
            }
            assertEquals(
                "{}",
                Files.readString(
                    root.resolve(
                        "onnx/config.json",
                    ),
                ),
                "acquisition authority must not become repair/deletion authority",
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
