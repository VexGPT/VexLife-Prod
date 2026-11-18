package vexlife.companion

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.context.VexLifeContextProfile
import vexlife.conversation.DirectConversationStore
import vexlife.foundational.VexLifeFoundationalField
import vexlife.home.CanonicalVexLifeHomeFixture

class DirectPromptContextProfileTest {
    private fun withHome(block: (Path) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-context-profile-prompt-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            CanonicalVexLifeHomeFixture.form(home)
            block(home)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private fun profile(
        mode: VexLifeContextProfile.FoundationMode,
        text: String = "",
        fingerprint: Char = 'c',
    ) =
        VexLifeContextProfile.Effective(
            profileRef = "profile.vexlife.context.test",
            revision = 1,
            definition =
                VexLifeContextProfile.Definition(
                    name = "Test profile",
                    foundationMode = mode,
                    foundationText = text,
                ),
            basedOnProductFoundation =
                VexLifeContextProfile.ProductFoundationIdentity(
                    version = "0.5.2",
                    bodySha256 = "a".repeat(64),
                ),
            fingerprint = fingerprint.toString().repeat(64),
            formedAt = "2026-11-17T20:00:00Z",
            productDefault = false,
            productFoundationChangedSinceSave = false,
        )

    @Test
    fun extendKeepsProductFoundationFirstAndReceiptsThePersonalExtensionSeparately() =
        withHome { home ->
            DirectConversationStore(home).withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.context-profile.extend",
                        requestMessageRef = "message.context-profile.human.extend",
                        content = "How does this profile change your context?",
                    )
                val materialized =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                        foundationalField = VexLifeFoundationalField.readCurrent(),
                        contextProfile =
                            profile(
                                mode = VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT,
                                text = "Prefer explanations that make the causal relationship visible.",
                            ),
                    )

                val system = materialized.messages.first().content
                val productIndex = system.indexOf(VexLifeFoundationalField.ProviderHeader)
                val personalIndex = system.indexOf(DirectPromptContext.ContextProfileFoundationHeader)
                assertTrue(productIndex >= 0)
                assertTrue(personalIndex > productIndex)

                val receipt = materialized.receipt
                assertEquals("EXTEND_PRODUCT_DEFAULT", receipt.contextProfileFoundationMode)
                assertEquals("CARRIED", receipt.profileFoundationState)
                assertTrue(receipt.profileFoundationEstimatedSystemMessageTokens > 0)
                assertTrue(receipt.profileFoundationSourceCharacters > 0)
                assertTrue(receipt.foundationalField != null)

                val refs = receipt.contextComponents.map { it.componentRef }
                assertTrue(DirectPromptContext.ComponentRefs.FoundationalField in refs)
                assertTrue(DirectPromptContext.ComponentRefs.ContextProfileFoundation in refs)
                assertTrue(
                    refs.indexOf(DirectPromptContext.ComponentRefs.FoundationalField) <
                        refs.indexOf(DirectPromptContext.ComponentRefs.ContextProfileFoundation),
                )
            }
        }

    @Test
    fun productDefaultProfileKeepsProductFoundationAndAddsNoPersonalExtension() =
        withHome { home ->
            DirectConversationStore(home).withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.context-profile.default",
                        requestMessageRef = "message.context-profile.human.default",
                        content = "Use the Product foundation.",
                    )
                val materialized =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                        foundationalField = VexLifeFoundationalField.readCurrent(),
                        contextProfile =
                            profile(
                                mode = VexLifeContextProfile.FoundationMode.PRODUCT_DEFAULT,
                            ),
                    )

                assertTrue(
                    materialized.messages.any {
                        VexLifeFoundationalField.ProviderHeader in it.content
                    },
                )
                assertFalse(
                    materialized.messages.any {
                        DirectPromptContext.ContextProfileFoundationHeader in it.content
                    },
                )
                assertTrue(materialized.receipt.foundationalField != null)
                assertEquals(
                    "PRODUCT_DEFAULT",
                    materialized.receipt.contextProfileFoundationMode,
                )
                assertNull(materialized.receipt.profileFoundationState)
                assertTrue(
                    DirectPromptContext.ComponentRefs.FoundationalField in
                        materialized.receipt.contextComponents.map { it.componentRef },
                )
                assertFalse(
                    DirectPromptContext.ComponentRefs.ContextProfileFoundation in
                        materialized.receipt.contextComponents.map { it.componentRef },
                )
            }
        }

}

// [VXG RealForever]
