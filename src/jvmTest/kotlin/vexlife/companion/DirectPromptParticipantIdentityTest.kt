package vexlife.companion

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class DirectPromptParticipantIdentityTest {
    @Test
    fun exactCurrentSpeakerRemainsKnownWhileHumanNameMayRemainUnknown() {
        val parent = Files.createTempDirectory("vexlife-participant-identity-unknown-")
        val home = parent.resolve(".vexlife")
        try {
            CanonicalVexLifeHomeFixture.form(home)
            val request = request(home, "unknown")
            val effective =
                DirectPromptContext.effectivePersonContext(
                    currentRequest = request,
                    context =
                        DirectPromptContext.PersonContext(
                            replyLanguage = "en",
                            replyLanguageName = "English",
                            privateConversations = emptyList(),
                        ),
                )

            assertEquals(request.speakerRef, effective.participantRef)
            assertEquals("person.local-user", effective.participantRef)
            assertNull(effective.name)
            assertEquals("UNKNOWN", effective.nameStanding)
            assertTrue(effective.nameSourceRefs.isEmpty())

            val modelFacing = DirectPromptContext.personContextSystemContent(effective)
            assertContains(modelFacing, "VEXLIFE_PERSON_CONTEXT/v2")
            assertContains(modelFacing, "\"participantRef\":\"person.local-user\"")
            assertContains(modelFacing, "\"name\":null")
            assertContains(modelFacing, "\"nameStanding\":\"UNKNOWN\"")
            assertContains(
                modelFacing,
                "do not infer the person's name from VexLife, an account, a device, or nearby product words",
            )
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun sourceBackedSyntheticNameDoesNotReplaceCanonicalSpeakerRef() {
        val parent = Files.createTempDirectory("vexlife-participant-identity-sarah-")
        val home = parent.resolve(".vexlife")
        try {
            CanonicalVexLifeHomeFixture.form(home)
            val request = request(home, "sarah")
            val sourceRef = "scenario.vexlife.evolve-context.dynamic-counterpart.changed-plan.v1"
            val effective =
                DirectPromptContext.effectivePersonContext(
                    currentRequest = request,
                    context =
                        DirectPromptContext.PersonContext(
                            replyLanguage = "en",
                            replyLanguageName = "English",
                            privateConversations = emptyList(),
                            nameClaim =
                                DirectPromptContext.PersonNameClaim(
                                    name = "Sarah",
                                    standing = DirectPromptContext.PersonNameStanding.PROFILE_DECLARED,
                                    sourceRefs = listOf(sourceRef),
                                ),
                        ),
                )

            assertEquals(request.speakerRef, effective.participantRef)
            assertEquals("person.local-user", effective.participantRef)
            assertEquals("Sarah", effective.name)
            assertEquals("PROFILE_DECLARED", effective.nameStanding)
            assertEquals(listOf(sourceRef), effective.nameSourceRefs)

            val modelFacing = DirectPromptContext.personContextSystemContent(effective)
            assertContains(modelFacing, "\"participantRef\":\"person.local-user\"")
            assertContains(modelFacing, "\"name\":\"Sarah\"")
            assertContains(modelFacing, "\"nameStanding\":\"PROFILE_DECLARED\"")
            assertContains(modelFacing, sourceRef)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private fun request(home: java.nio.file.Path, suffix: String): DirectConversationStore.Event =
        DirectConversationStore(home).withWriterLease {
            beginRequest(
                turnRef = "turn.self-development.participant-identity.$suffix",
                requestMessageRef = "message.self-development.human.participant-identity.$suffix",
                content = "Hello.",
            )
        }
}

// [VXG RealForever]
