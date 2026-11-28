package vexlife.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.sources.VexLifeLocalSourcesFamily

class VexLifeWorkspaceChecksFamilyTest {
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
        VexLifeWorkspaceChecksFamily.Grant(
            grantRef =
                VexLifeWorkspaceChecksFamily.GrantRefPrefix +
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
    fun readableSourceNeedsSeparateWorkspaceCheckGrant() {
        val sourceRef =
            source(
                'a',
            )
        assertEquals(
            VexLifeWorkspaceChecksFamily.Effect.GrantChecks(
                sourceRef,
            ),
            VexLifeWorkspaceChecksFamily.resolve(
                VexLifeWorkspaceChecksFamily.Empty,
                VexLifeWorkspaceChecksFamily.Allow(
                    sourceRef,
                ),
            )?.effect,
        )
    }

    @Test
    fun currentCheckGrantCanBeRevokedButNotGrantedTwice() {
        val grant =
            grant(
                'b',
            )
        val state =
            VexLifeWorkspaceChecksFamily.state(
                listOf(
                    grant,
                ),
            )

        assertNull(
            VexLifeWorkspaceChecksFamily.resolve(
                state,
                VexLifeWorkspaceChecksFamily.Allow(
                    grant.sourceRef,
                ),
            ),
        )
        assertEquals(
            VexLifeWorkspaceChecksFamily.Effect.RevokeChecks(
                grant.sourceRef,
            ),
            VexLifeWorkspaceChecksFamily.resolve(
                state,
                VexLifeWorkspaceChecksFamily.Revoke(
                    grant.sourceRef,
                ),
            )?.effect,
        )
        assertTrue(
            VexLifeWorkspaceChecksFamily.isGrantRef(
                grant.grantRef,
            ),
        )
    }
}

// [VXG RealForever]
