package vexlife.model.heretic.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticProductIdentity

class HereticArtifactMaterializationTest {
    private fun sha256(
        text: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                text.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }

    @Test
    fun currentCatalogReproducesTheCertifiedMaterializationPlanExactly() {
        val plan =
            HereticArtifactMaterialization.plan()

        assertEquals(
            "808b82a2bd04b2bac362b5d0b7a1197aeea59e57",
            plan.providerRevision,
        )
        assertEquals(
            11,
            plan.memberCount,
        )
        assertEquals(
            15_153_242_506L,
            plan.totalBytes,
        )
        assertEquals(
            HereticProductIdentity.ArtifactContentSetSha256,
            plan.contentSetSha256,
        )
        assertEquals(
            HereticProductIdentity.ArtifactContentSetSha256,
            HereticArtifactMaterialization.computeContentSetSha256(
                plan.members,
            ),
        )
        assertEquals(
            plan.members.map {
                it.logicalPath
            },
            plan.members
                .map {
                    it.logicalPath
                }
                .sorted(),
        )
    }

    @Test
    fun classifierDistinguishesMissingSizeHashAndExactReuse() {
        val root =
            Files.createTempDirectory(
                "vexlife-heretic-artifact-",
            )
        val file =
            root.resolve(
                "member.bin",
            )

        try {
            assertIs<HereticArtifactMaterialization.Classification.Missing>(
                HereticArtifactMaterialization.classifyExactArtifact(
                    finalPath = file,
                    expectedSha256 =
                        sha256(
                            "exact",
                        ),
                    expectedBytes = 5,
                ),
            )

            Files.writeString(
                file,
                "wrong-size",
            )

            assertIs<HereticArtifactMaterialization.Classification.InvalidSize>(
                HereticArtifactMaterialization.classifyExactArtifact(
                    finalPath = file,
                    expectedSha256 =
                        sha256(
                            "exact",
                        ),
                    expectedBytes = 5,
                ),
            )

            Files.writeString(
                file,
                "other",
            )

            assertIs<HereticArtifactMaterialization.Classification.InvalidHash>(
                HereticArtifactMaterialization.classifyExactArtifact(
                    finalPath = file,
                    expectedSha256 =
                        sha256(
                            "exact",
                        ),
                    expectedBytes = 5,
                ),
            )

            Files.writeString(
                file,
                "exact",
            )

            val exact =
                assertIs<HereticArtifactMaterialization.Classification.VerifiedReusable>(
                    HereticArtifactMaterialization.classifyExactArtifact(
                        finalPath = file,
                        expectedSha256 =
                            sha256(
                                "exact",
                            ),
                        expectedBytes = 5,
                    ),
                )

            assertEquals(
                5,
                exact.bytes,
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun classifierRejectsNonRegularArtifactDestinations() {
        val root =
            Files.createTempDirectory(
                "vexlife-heretic-artifact-policy-",
            )

        try {
            assertFailsWith<IllegalArgumentException> {
                HereticArtifactMaterialization.classifyExactArtifact(
                    finalPath = root,
                    expectedSha256 =
                        "0".repeat(
                            64,
                        ),
                    expectedBytes = 1,
                )
            }
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun ordinaryMaterializationCannotAcquireWithoutExplicitAuthorization() {
        val home =
            Files.createTempDirectory(
                "vexlife-heretic-home-",
            )
                .toAbsolutePath()
        var acquisitionCalls =
            0

        try {
            assertFailsWith<IllegalArgumentException> {
                HereticArtifactMaterialization.materializeCurrent(
                    productHome = home,
                    allowAcquisition = false,
                    acquirer =
                        HereticArtifactMaterialization.Acquirer {
                            _,
                            _,
                            ->
                            acquisitionCalls +=
                                1
                        },
                )
            }

            assertEquals(
                0,
                acquisitionCalls,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun symlinkedModelsParentCannotRedirectArtifactAcquisition() {
        val parent =
            Files.createTempDirectory(
                "vexlife-heretic-model-parent-link-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val outside =
            parent.resolve(
                "outside",
            )
        var acquisitionCalls =
            0

        try {
            Files.createDirectory(
                home,
            )
            Files.createDirectory(
                outside,
            )
            Files.createSymbolicLink(
                home.resolve(
                    "models",
                ),
                outside,
            )

            assertFailsWith<IllegalArgumentException> {
                HereticArtifactMaterialization.materializeCurrent(
                    productHome = home,
                    allowAcquisition = true,
                    acquirer =
                        HereticArtifactMaterialization.Acquirer {
                            _,
                            _,
                            ->
                            acquisitionCalls +=
                                1
                        },
                )
            }

            assertEquals(
                0,
                acquisitionCalls,
            )
            assertFalse(
                Files.exists(
                    outside.resolve(
                        "heretic",
                    ),
                ),
            )
        } finally {
            Files.deleteIfExists(
                home.resolve(
                    "models",
                ),
            )
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun materializerOwnsArtifactMutationButNotRuntimeActivation() {
        val resultType =
            HereticArtifactMaterialization.Result(
                state = "HERETIC_ARTIFACTS_MATERIALIZED_EXACT",
                modelPath = "/tmp/model",
                providerRevision = "0".repeat(40),
                contentSetSha256 = "0".repeat(64),
                memberCount = 1,
                totalBytes = 1,
                reusedMemberCount = 1,
                acquiredMemberCount = 0,
                acquisitionPerformed = false,
                artifactMutationPerformed = false,
            )

        assertFalse(
            resultType.acquisitionPerformed,
        )
        assertFalse(
            resultType.artifactMutationPerformed,
        )
        assertTrue(
            resultType.state.contains(
                "MATERIALIZED",
            ),
        )
    }
}

// [VXG RealForever]
