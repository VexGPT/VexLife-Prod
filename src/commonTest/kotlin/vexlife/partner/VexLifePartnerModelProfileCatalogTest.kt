package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifePartnerModelProfileCatalogTest {
    @Test
    fun initialCatalogHasOneNarrowBaselinePerBuiltInProvider() {
        val definitions =
            VexLifePartnerModelProfileCatalog.definitions()

        assertEquals(3, definitions.size)
        assertEquals(
            setOf(
                VexLifePartnerProviderCatalog.OpenAiRef,
                VexLifePartnerProviderCatalog.AnthropicRef,
                VexLifePartnerProviderCatalog.KimiInternationalRef,
            ),
            definitions.map { it.providerRef }.toSet(),
        )
        assertEquals(definitions.size, definitions.map { it.profileRef }.distinct().size)
        assertEquals(definitions.size, definitions.map { it.modelRef }.distinct().size)
    }

    @Test
    fun exactCurrentBaselineModelIdsAreSourceOwned() {
        assertEquals(
            "gpt-6.2-sol",
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol.modelRef,
        )
        assertEquals(
            "claude-sonnet-5-5",
            VexLifePartnerModelProfileCatalog.AnthropicClaudeSonnet55.modelRef,
        )
        assertEquals(
            "kimi-k3",
            VexLifePartnerModelProfileCatalog.KimiK3.modelRef,
        )
    }

    @Test
    fun catalogRepresentsOnlyClientToolCapabilityShapeNotAvailability() {
        VexLifePartnerModelProfileCatalog.definitions().forEach { definition ->
            assertTrue(definition.providerNativeToolsRepresented)
            assertTrue(definition.reasoningEffortControlRepresented)
            assertTrue(definition.visualInputRepresented)
            assertTrue(definition.clientToolsOnly)
            assertTrue(definition.sourceRefs.isNotEmpty())
        }
    }

    @Test
    fun providerDialectMustMatchBuiltInTopology() {
        VexLifePartnerModelProfileCatalog.definitions().forEach { definition ->
            assertEquals(
                VexLifePartnerProviderCatalog.resolve(definition.providerRef)?.dialect,
                definition.dialect,
            )
        }
    }

    @Test
    fun providerModelResolutionIsExactAndUnknownAccountModelsDoNotBecomeSupported() {
        assertEquals(
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol,
            VexLifePartnerModelProfileCatalog.resolve(
                VexLifePartnerProviderCatalog.OpenAiRef,
                "gpt-6.2-sol",
            ),
        )
        assertNull(
            VexLifePartnerModelProfileCatalog.resolve(
                VexLifePartnerProviderCatalog.OpenAiRef,
                "some-provider-visible-model",
            ),
        )
        assertNull(
            VexLifePartnerModelProfileCatalog.resolve(
                VexLifePartnerProviderCatalog.KimiInternationalRef,
                "kimi-for-coding",
            ),
        )
    }

    @Test
    fun kimiCodeAndProviderHostedToolsAreNotSmuggledIntoInitialCatalog() {
        val serializedShape =
            VexLifePartnerModelProfileCatalog.definitions()
                .joinToString("\n") {
                    it.toString()
                }

        assertTrue(
            !serializedShape.contains(
                "kimi-for-coding",
                ignoreCase = true,
            ),
        )
        assertTrue(
            VexLifePartnerModelProfileCatalog.definitions().all {
                it.clientToolsOnly
            },
        )
    }
}

// [VXG RealForever]
