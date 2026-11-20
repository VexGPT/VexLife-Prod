package vexlife.sources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeLocalSourcesFamilyTest {
    private fun grant(
        digest: Char,
        name: String,
    ) =
        VexLifeLocalSourcesFamily.Grant(
            sourceRef =
                VexLifeLocalSourcesFamily.sourceRefForSha256(
                    digest
                        .toString()
                        .repeat(
                            64,
                        ),
                ),
            name = name,
            path = "/tmp/" + name,
            grantedAt = "2026-11-20T07:00:00Z",
        )

    @Test
    fun emptyStateOffersOnlyAddFolder() {
        val display =
            VexLifeLocalSourcesFamily.displayStateFor(
                VexLifeLocalSourcesFamily.Empty,
            )

        assertEquals(
            VexLifeLocalSourcesFamily.ControlAddFolder,
            display.addControlRef,
        )
        assertTrue(
            display.grants.isEmpty(),
        )
        assertTrue(
            VexLifeLocalSourcesFamily.resolve(
                currentState =
                    VexLifeLocalSourcesFamily.Empty,
                control =
                    VexLifeLocalSourcesFamily.AddFolder,
            )
                ?.effect is
                VexLifeLocalSourcesFamily.Effect.RequestFolderSelection,
        )
    }

    @Test
    fun removeIsLawfulOnlyForAnExactCurrentGrant() {
        val grant =
            grant(
                'a',
                "Project",
            )
        val state =
            VexLifeLocalSourcesFamily.state(
                listOf(
                    grant,
                ),
            )

        assertEquals(
            VexLifeLocalSourcesFamily.Effect.RemoveGrant(
                grant.sourceRef,
            ),
            VexLifeLocalSourcesFamily.resolve(
                currentState = state,
                control =
                    VexLifeLocalSourcesFamily.Remove(
                        grant.sourceRef,
                    ),
            )
                ?.effect,
        )
        assertNull(
            VexLifeLocalSourcesFamily.resolve(
                currentState = state,
                control =
                    VexLifeLocalSourcesFamily.Remove(
                        VexLifeLocalSourcesFamily.sourceRefForSha256(
                            "b".repeat(
                                64,
                            ),
                        ),
                    ),
            ),
        )
    }

    @Test
    fun grantLimitHidesAddRatherThanLettingRendererInventALimit() {
        val grants =
            (0 until VexLifeLocalSourcesFamily.MaximumGrants).map {
                index ->
                val hex =
                    (
                        index %
                            16
                    )
                        .toString(
                            16,
                        )
                        .single()
                grant(
                    hex,
                    "folder-" +
                        index,
                )
            }
        val state =
            VexLifeLocalSourcesFamily.state(
                grants,
            )
        val display =
            VexLifeLocalSourcesFamily.displayStateFor(
                state,
            )

        assertNull(
            display.addControlRef,
        )
        assertEquals(
            "workspace.status.local-sources-refused-limit-reached",
            display.limitStatus?.key,
        )
        assertNull(
            VexLifeLocalSourcesFamily.resolve(
                currentState = state,
                control =
                    VexLifeLocalSourcesFamily.AddFolder,
            ),
        )
    }
}

// [VXG RealForever]
