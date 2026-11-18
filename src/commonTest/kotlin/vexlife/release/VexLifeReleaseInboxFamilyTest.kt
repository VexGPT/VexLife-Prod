package vexlife.release

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VexLifeReleaseInboxFamilyTest {
    private fun sha(character: Char) =
        VexLifeReleaseIdentity.SourceSha(
            character.toString().repeat(40),
        )

    private fun notice(
        minor: Int,
        sourceCharacter: Char,
        publishedAtEpochMillis: Long,
    ): VexLifeReleaseInboxFamily.Notice =
        VexLifeReleaseInboxFamily.Notice(
            VexLifeReleaseIdentity.Published(
                identity =
                    VexLifeReleaseIdentity.Value.Stable(
                        version =
                            VexLifeReleaseIdentity.SemanticVersion(
                                major = 0,
                                minor = minor,
                                patch = 0,
                            ),
                        sourceSha = sha(sourceCharacter),
                    ),
                publishedAtEpochMillis = publishedAtEpochMillis,
                page =
                    VexLifeReleaseIdentity.HttpsPage(
                        "https://github.com/VexGPT/VexLife-Prod/releases/example",
                    ),
            ),
        )

    @Test
    fun newReleaseCommunicationIsUnreadAndNewestFirst() {
        val older = notice(5, 'a', 100)
        val newer = notice(6, 'b', 200)

        val state =
            VexLifeReleaseInboxFamily.observe(
                notices = listOf(older, newer),
                acknowledgements = emptyList(),
            )
        val display =
            VexLifeReleaseInboxFamily.displayStateFor(
                state,
            )

        assertEquals(
            listOf(newer.identity, older.identity),
            state.communications.map { it.notice.identity },
        )
        assertEquals(
            2,
            display.unreadCount,
        )
        assertEquals(
            listOf("0.6.0", "0.5.0"),
            display.items.map { it.version },
        )
        assertEquals(
            VexLifeReleaseInboxFamily.KeyUpdateAvailable,
            display.items.first().status.key,
        )
    }

    @Test
    fun viewedCommunicationRemainsVisibleWithoutRequestingAttention() {
        val release = notice(5, 'a', 100)

        val state =
            VexLifeReleaseInboxFamily.observe(
                notices = listOf(release),
                acknowledgements =
                    listOf(
                        VexLifeReleaseInboxFamily.Acknowledgement(
                            identity = release.identity,
                            standing =
                                VexLifeReleaseInboxFamily.AcknowledgementStanding.Viewed,
                        ),
                    ),
            )
        val display =
            VexLifeReleaseInboxFamily.displayStateFor(
                state,
            )

        assertEquals(
            0,
            display.unreadCount,
        )
        assertEquals(
            1,
            display.items.size,
        )
        assertEquals(
            VexLifeReleaseInboxFamily.KeyViewed,
            display.items.single().status.key,
        )
    }

    @Test
    fun dismissedCommunicationLeavesTheActiveInbox() {
        val release = notice(5, 'a', 100)

        val state =
            VexLifeReleaseInboxFamily.observe(
                notices = listOf(release),
                acknowledgements =
                    listOf(
                        VexLifeReleaseInboxFamily.Acknowledgement(
                            identity = release.identity,
                            standing =
                                VexLifeReleaseInboxFamily.AcknowledgementStanding.Dismissed,
                        ),
                    ),
            )

        assertEquals(
            emptyList(),
            state.communications,
        )
        assertEquals(
            0,
            VexLifeReleaseInboxFamily.displayStateFor(state).unreadCount,
        )
    }

    @Test
    fun viewAndDismissControlsAuthorEffectsWithoutPredictingAcknowledgement() {
        val release = notice(5, 'a', 100)
        val state =
            VexLifeReleaseInboxFamily.observe(
                notices = listOf(release),
                acknowledgements = emptyList(),
            )

        val view =
            assertNotNull(
                VexLifeReleaseInboxFamily.resolve(
                    currentState = state,
                    control =
                        VexLifeReleaseInboxFamily.Control.View(
                            release.identity,
                        ),
                ),
            )
        assertNull(
            view.admittedState,
        )
        assertEquals(
            release,
            assertIs<VexLifeReleaseInboxFamily.Effect.ViewRelease>(view.effect).notice,
        )

        val dismiss =
            assertNotNull(
                VexLifeReleaseInboxFamily.resolve(
                    currentState = state,
                    control =
                        VexLifeReleaseInboxFamily.Control.Dismiss(
                            release.identity,
                        ),
                ),
            )
        assertNull(
            dismiss.admittedState,
        )
        assertEquals(
            release.identity,
            assertIs<VexLifeReleaseInboxFamily.Effect.DismissRelease>(dismiss.effect).identity,
        )
    }

    @Test
    fun aControlForCommunicationThatIsNotInTheInboxIsNotLawful() {
        val release = notice(5, 'a', 100)
        val foreign = notice(6, 'b', 200)
        val state =
            VexLifeReleaseInboxFamily.observe(
                notices = listOf(release),
                acknowledgements = emptyList(),
            )

        assertNull(
            VexLifeReleaseInboxFamily.resolve(
                currentState = state,
                control =
                    VexLifeReleaseInboxFamily.Control.Dismiss(
                        foreign.identity,
                    ),
            ),
        )
    }
}

// [VXG RealForever]
