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
        val certified = HereticModelCatalog.Models.Qwen27B
        assertEquals(HereticModelCatalog.Default, certified)
        assertEquals(HereticProductIdentity.Current, certified.identity)
        assertEquals(HereticArtifactCatalog.Members, certified.members)
        assertEquals(HereticArtifactCatalog.CertifiedBytes, certified.totalBytes)
        assertEquals("models/heretic/qwen3.8-27b-mlx4", certified.modelRootRelativePath)
        assertEquals(HereticModelCatalog.Standing.CERTIFIED, certified.standing)
        assertEquals(
            listOf(HereticModelCatalog.Standing.CANDIDATE, HereticModelCatalog.Standing.CANDIDATE),
            listOf(
                HereticModelCatalog.Models.Qwen9B.standing,
                HereticModelCatalog.Models.Qwen4B.standing,
            ),
        )
    }

    @Test
    fun eachModelIsPinnedToOneProviderRevisionWithExactBytes() {
        assertEquals(6_736_900_225L, HereticModelCatalog.Models.Qwen9B.totalBytes)
        assertEquals(10, HereticModelCatalog.Models.Qwen9B.members.size)
        assertEquals(3_054_412_642L, HereticModelCatalog.Models.Qwen4B.totalBytes)
        assertEquals(11, HereticModelCatalog.Models.Qwen4B.members.size)
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
    fun gptOss20BIsOfferedPinnedAndHarmonyButNeverStartedByDefault() {
        val oss = HereticModelCatalog.Models.GptOss20B
        assertEquals(12_104_201_036L, oss.totalBytes)
        assertEquals(12, oss.members.size)
        assertEquals(HereticModelCatalog.Standing.CANDIDATE, oss.standing)
        assertEquals(HereticCompanionRepresentation.Profiles.GptOss20B, oss.companionProfile)
        assertEquals(
            HereticCompanionRepresentation.ReachRepresentationStanding.QUALIFIED,
            oss.companionProfile.reachRepresentationStanding,
        )
        assertEquals(
            "10c6bad646aa379476f3d8c1c595b77ce618d7e106665cfcefd06ad12935bf75",
            oss.identity.artifactContentSetSha256,
        )
        assertEquals(24 * gib, oss.recommendedMemoryBytes)
        assertEquals(true, HereticModelCatalog.fits(oss, 24 * gib))
        assertFalse(oss.autoRecommended)
        // Offered between the 27B and the 9B by the memory it wants; chosen only by the person.
        assertEquals(
            listOf("heretic-27b", "gpt-oss-20b", "heretic-9b", "heretic-4b"),
            HereticModelCatalog.Entries.map { it.handle },
        )
        for (memory in listOf(8L, 16L, 24L, 32L, 64L)) {
            assertTrue(HereticModelCatalog.recommendedFor(memory * gib) != oss, "$memory GiB")
        }
        assertEquals(
            HereticCompanionRepresentation.Profiles.Qwen27B,
            HereticModelCatalog.Models.Qwen27B.companionProfile,
        )
        assertEquals(
            HereticCompanionRepresentation.Profiles.Qwen9B,
            HereticModelCatalog.Models.Qwen9B.companionProfile,
        )
        assertEquals(
            HereticCompanionRepresentation.Profiles.Qwen4B,
            HereticModelCatalog.Models.Qwen4B.companionProfile,
        )
    }

    @Test
    fun everyOfferedModelChoosesACompleteProviderProfileWithoutConnectionDefaults() {
        for (entry in HereticModelCatalog.Entries) {
            val profile = entry.companionProfile
            assertTrue(profile.profileRef.isNotBlank(), entry.handle)
            assertTrue(profile.generation.productMaxOutputTokens > 0, entry.handle)
            assertTrue(profile.generation.sampling.sourceRef.isNotBlank(), entry.handle)
            assertTrue(profile.generation.sampling.temperature >= 0.0, entry.handle)
            assertTrue(profile.generation.sampling.topP in 0.0..1.0, entry.handle)
            assertTrue(profile.generation.sampling.topK >= 0, entry.handle)
        }

        val qwen27 = HereticCompanionRepresentation.Profiles.Qwen27B
        assertEquals(
            HereticCompanionRepresentation.ReasoningContinuationStanding.QUALIFIED,
            qwen27.reasoningContinuationStanding,
        )
        assertEquals(1.0, qwen27.generation.sampling.temperature)
        assertEquals(0.95, qwen27.generation.sampling.topP)
        assertEquals(20, qwen27.generation.sampling.topK)
        assertEquals(
            HereticCompanionRepresentation.ReachRepresentationStanding.QUALIFIED,
            qwen27.reachRepresentationStanding,
        )

        val qwen9 = HereticCompanionRepresentation.Profiles.Qwen9B
        assertEquals(
            HereticCompanionRepresentation.ReasoningContinuationStanding.QUALIFIED,
            qwen9.reasoningContinuationStanding,
        )
        assertEquals(0.6, qwen9.generation.sampling.temperature)
        assertEquals(0.95, qwen9.generation.sampling.topP)
        assertEquals(20, qwen9.generation.sampling.topK)

        val qwen4 = HereticCompanionRepresentation.Profiles.Qwen4B
        assertEquals(
            HereticCompanionRepresentation.ReasoningContinuationStanding.QUALIFIED,
            qwen4.reasoningContinuationStanding,
        )
        assertEquals(0.6, qwen4.generation.sampling.temperature)
        assertEquals(0.95, qwen4.generation.sampling.topP)
        assertEquals(20, qwen4.generation.sampling.topK)
        assertEquals(
            HereticCompanionRepresentation.CapabilityRepresentation.PROVIDER_NATIVE_TOOLS,
            qwen27.capabilityRepresentation,
        )
        assertEquals(
            HereticCompanionRepresentation.CapabilityRepresentation.PROVIDER_NATIVE_TOOLS,
            qwen9.capabilityRepresentation,
        )
        assertEquals(
            HereticCompanionRepresentation.CapabilityRepresentation.VEXLIFE_CONTROL_TEXT,
            qwen4.capabilityRepresentation,
        )
        assertEquals(
            HereticCompanionRepresentation.ReachRepresentationStanding.HELD_PROVIDER_BRIDGE,
            qwen4.reachRepresentationStanding,
        )

        val oss = HereticCompanionRepresentation.Profiles.GptOss20B
        assertEquals(
            HereticCompanionRepresentation.ReasoningContinuationStanding.HELD_PROVIDER_TEMPLATE,
            oss.reasoningContinuationStanding,
        )
        assertEquals(1.0, oss.generation.sampling.temperature)
        assertEquals(1.0, oss.generation.sampling.topP)
        assertEquals(0, oss.generation.sampling.topK)
        assertEquals(HereticCompanionRepresentation.ReasoningEffort.MEDIUM, oss.generation.reasoningEffort)
    }

    @Test
    fun aMacStartsWithTheLargestModelThatFitsItsMemory() {
        assertEquals(HereticModelCatalog.Models.Qwen27B, HereticModelCatalog.recommendedFor(64 * gib))
        assertEquals(HereticModelCatalog.Models.Qwen27B, HereticModelCatalog.recommendedFor(32 * gib))
        assertEquals(HereticModelCatalog.Models.Qwen9B, HereticModelCatalog.recommendedFor(24 * gib))
        assertEquals(HereticModelCatalog.Models.Qwen9B, HereticModelCatalog.recommendedFor(16 * gib))
        assertEquals(HereticModelCatalog.Models.Qwen4B, HereticModelCatalog.recommendedFor(8 * gib))
        assertEquals(HereticModelCatalog.Models.Qwen4B, HereticModelCatalog.recommendedFor(4 * gib))
        assertEquals(HereticModelCatalog.Default, HereticModelCatalog.recommendedFor(null))
        assertEquals(null, HereticModelCatalog.fits(HereticModelCatalog.Models.Qwen4B, null))
    }

    @Test
    fun aConfigurationCanNameAnyOfferedModelAndNothingElse() {
        val scope =
            HereticModelConfiguration.BindingScope(
                "home.vexlife.test",
                "companion-lineage.vexlife.test",
            )
        val nine = HereticModelConfiguration.pending(scope, HereticModelCatalog.Models.Qwen9B)
        assertEquals(HereticModelCatalog.Models.Qwen9B.modelRef, nine.modelRef)
        assertEquals(HereticModelCatalog.Models.Qwen9B, HereticModelConfiguration.selected(nine))
        assertNotNull(HereticModelConfiguration.validate(nine, scope))
        // The single-model-era value shape still reads as the certified default.
        assertEquals(
            HereticModelCatalog.Models.Qwen27B,
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
