package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The catalog of offered models: exact, distinct, and honest about how each one stands. */
class HereticModelCatalogTest {
    private val gib = 1024L * 1024L * 1024L

    @Test
    fun theCertified27BIsTheUnchangedDefaultAndTheOthersAreCandidates() {
        val certified = HereticModelCatalog.Certified27B
        assertEquals(HereticModelCatalog.Default, certified)
        assertEquals(HereticProductIdentity.Current, certified.identity)
        assertEquals(HereticArtifactCatalog.Members, certified.members)
        assertEquals(HereticArtifactCatalog.CertifiedBytes, certified.totalBytes)
        assertEquals("models/heretic/qwen3.8-27b-mlx4", certified.modelRootRelativePath)
        assertEquals(HereticModelCatalog.Standing.CERTIFIED, certified.standing)
        assertEquals(
            listOf(HereticModelCatalog.Standing.CANDIDATE, HereticModelCatalog.Standing.CANDIDATE),
            listOf(
                HereticModelCatalog.Candidate9B.standing,
                HereticModelCatalog.Candidate4B.standing,
            ),
        )
    }

    @Test
    fun eachModelIsPinnedToOneProviderRevisionWithExactBytes() {
        assertEquals(6_736_900_225L, HereticModelCatalog.Candidate9B.totalBytes)
        assertEquals(10, HereticModelCatalog.Candidate9B.members.size)
        assertEquals(3_054_412_642L, HereticModelCatalog.Candidate4B.totalBytes)
        assertEquals(11, HereticModelCatalog.Candidate4B.members.size)
        for (entry in HereticModelCatalog.Entries) {
            assertTrue(
                entry.members.all { it.sourceRef.endsWith(entry.providerRevision) },
                entry.handle,
            )
            assertTrue(entry.members.any { it.filename == "chat_template.jinja" }, entry.handle)
            assertTrue(entry.members.any { it.filename == "tokenizer.json" }, entry.handle)
            // Every model shares the certified runtime relationship; only what it is differs.
            assertEquals(HereticProductIdentity.RuntimeAdapterRef, entry.identity.runtimeAdapterRef)
            assertEquals(HereticProductIdentity.MlxLmVersion, entry.identity.mlxLmVersion)
            assertEquals(HereticProductIdentity.Endpoint, entry.identity.endpoint)
            assertEquals(entry, HereticModelCatalog.entry(entry.modelRef))
            assertEquals(entry, HereticModelCatalog.byHandle(entry.handle))
        }
        assertNull(HereticModelCatalog.entry("model.not-offered"))
    }

    @Test
    fun aMacStartsWithTheLargestModelThatFitsItsMemory() {
        assertEquals(HereticModelCatalog.Certified27B, HereticModelCatalog.recommendedFor(64 * gib))
        assertEquals(HereticModelCatalog.Certified27B, HereticModelCatalog.recommendedFor(32 * gib))
        assertEquals(HereticModelCatalog.Candidate9B, HereticModelCatalog.recommendedFor(24 * gib))
        assertEquals(HereticModelCatalog.Candidate9B, HereticModelCatalog.recommendedFor(16 * gib))
        assertEquals(HereticModelCatalog.Candidate4B, HereticModelCatalog.recommendedFor(8 * gib))
        assertEquals(HereticModelCatalog.Candidate4B, HereticModelCatalog.recommendedFor(4 * gib))
        assertEquals(HereticModelCatalog.Default, HereticModelCatalog.recommendedFor(null))
        assertFalse(HereticModelCatalog.fits(HereticModelCatalog.Candidate4B, null))
    }

    @Test
    fun aConfigurationCanNameAnyOfferedModelAndNothingElse() {
        val scope =
            HereticModelConfiguration.BindingScope(
                "home.vexlife.test",
                "companion-lineage.vexlife.test",
            )
        val nine = HereticModelConfiguration.pending(scope, HereticModelCatalog.Candidate9B)
        assertEquals(HereticModelCatalog.Candidate9B.modelRef, nine.modelRef)
        assertEquals(HereticModelCatalog.Candidate9B, HereticModelConfiguration.selected(nine))
        assertNotNull(HereticModelConfiguration.validate(nine, scope))
        // The single-model-era value shape still reads as the certified default.
        assertEquals(
            HereticModelCatalog.Certified27B,
            HereticModelConfiguration.selected(HereticModelConfiguration.pending(scope)),
        )
        runCatching {
                HereticModelConfiguration.validate(nine.copy(modelRef = "model.not-offered"), scope)
            }
            .also { assertTrue(it.isFailure) }
        runCatching {
                HereticModelConfiguration.validate(
                    nine.copy(
                        artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256
                    ),
                    scope,
                )
            }
            .also { assertTrue(it.isFailure) }
    }
}
