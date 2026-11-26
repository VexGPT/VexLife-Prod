package vexlife.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.sources.VexLifeLocalSourcesFamily

class VexLifeWorkspaceRunsFamilyTest {
    private fun source(
        hex: Char,
    ) =
        VexLifeLocalSourcesFamily.sourceRefForSha256(
            hex.toString()
                .repeat(
                    64,
                ),
        )

    private fun grant(
        hex: Char,
    ) =
        VexLifeWorkspaceRunsFamily.Grant(
            grantRef =
                VexLifeWorkspaceRunsFamily.GrantRefPrefix +
                    hex.toString()
                        .repeat(
                            64,
                        ),
            sourceRef =
                source(
                    hex,
                ),
            sourceGrantedAt =
                "2026-11-24T20:00:00Z",
            grantedAt =
                "2026-11-24T20:01:00Z",
        )

    @Test
    fun readableSourceNeedsSeparateProjectRunGrant() {
        val sourceRef =
            source(
                'a',
            )
        assertEquals(
            VexLifeWorkspaceRunsFamily.Effect.GrantRuns(
                sourceRef,
            ),
            VexLifeWorkspaceRunsFamily.resolve(
                VexLifeWorkspaceRunsFamily.Empty,
                VexLifeWorkspaceRunsFamily.Allow(
                    sourceRef,
                ),
            )?.effect,
        )
    }

    @Test
    fun currentProjectRunGrantCanBeRevokedButNotGrantedTwice() {
        val grant =
            grant(
                'b',
            )
        val state =
            VexLifeWorkspaceRunsFamily.state(
                listOf(
                    grant,
                ),
            )

        assertNull(
            VexLifeWorkspaceRunsFamily.resolve(
                state,
                VexLifeWorkspaceRunsFamily.Allow(
                    grant.sourceRef,
                ),
            ),
        )
        assertEquals(
            VexLifeWorkspaceRunsFamily.Effect.RevokeRuns(
                grant.sourceRef,
            ),
            VexLifeWorkspaceRunsFamily.resolve(
                state,
                VexLifeWorkspaceRunsFamily.Revoke(
                    grant.sourceRef,
                ),
            )?.effect,
        )
        assertTrue(
            VexLifeWorkspaceRunsFamily.isGrantRef(
                grant.grantRef,
            ),
        )
    }
}

// [VXG RealForever]
