package vexlife.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.sources.VexLifeLocalSourcesFamily

class VexLifeWorkspaceEffectsFamilyTest {
    private fun source(hex: Char) =
        VexLifeLocalSourcesFamily.sourceRefForSha256(hex.toString().repeat(64))

    private fun grant(hex: Char) =
        VexLifeWorkspaceEffectsFamily.Grant(
            grantRef = VexLifeWorkspaceEffectsFamily.GrantRefPrefix + hex.toString().repeat(64),
            sourceRef = source(hex),
            sourceGrantedAt = "2026-11-24T20:00:00Z",
            grantedAt = "2026-11-24T20:01:00Z",
        )

    @Test
    fun readableSourceNeedsSeparateWorkspaceGrant() {
        val sourceRef = source('a')
        assertEquals(
            VexLifeWorkspaceEffectsFamily.Effect.GrantWorkspace(sourceRef),
            VexLifeWorkspaceEffectsFamily.resolve(
                VexLifeWorkspaceEffectsFamily.Empty,
                VexLifeWorkspaceEffectsFamily.Allow(sourceRef),
            )?.effect,
        )
    }

    @Test
    fun currentWorkspaceGrantCanBeRevokedButNotGrantedTwice() {
        val grant = grant('b')
        val state = VexLifeWorkspaceEffectsFamily.state(listOf(grant))

        assertNull(
            VexLifeWorkspaceEffectsFamily.resolve(
                state,
                VexLifeWorkspaceEffectsFamily.Allow(grant.sourceRef),
            )
        )
        assertEquals(
            VexLifeWorkspaceEffectsFamily.Effect.RevokeWorkspace(grant.sourceRef),
            VexLifeWorkspaceEffectsFamily.resolve(
                state,
                VexLifeWorkspaceEffectsFamily.Revoke(grant.sourceRef),
            )?.effect,
        )
        assertTrue(VexLifeWorkspaceEffectsFamily.isGrantRef(grant.grantRef))
    }
}

// [VXG RealForever]
