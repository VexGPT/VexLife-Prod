package vexlife.continuity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class VexLifeContinuityTest {
    private val identity =
        VexLifeContinuity.HomeIdentity(
            homeRef =
                "vex-home.device.vexlife.00000000-0000-0000-0000-000000000001",
            familyRef =
                "vex-family.local-user",
            deviceRef =
                "device.vexlife.00000000-0000-0000-0000-000000000001",
            companionLineageRef =
                "companion-lineage.vexlife.00000000-0000-0000-0000-000000000002",
        )

    @Test
    fun newBeginningsPreservesTheCanonicalVexLifeHomeIdentity() {
        assertEquals(
            ".vexlife",
            VexLifeContinuity.CanonicalMacHomeRelativeRoot,
        )
        assertEquals(
            "vexlife.home/v0",
            VexLifeContinuity.HomeSchema,
        )
        assertFalse(
            identity.homeRef.startsWith(
                "vex-home.prod.",
            ),
        )
        assertFalse(
            identity.deviceRef.startsWith(
                "device.vexlife-prod.",
            ),
        )
        assertFalse(
            identity.companionLineageRef.startsWith(
                "companion-lineage.vexlife-prod.",
            ),
        )
    }

    @Test
    fun oneIdentityProjectsConversationMemoryAndProductRuntimeCoordinates() {
        val coordinates =
            VexLifeContinuity.coordinates(
                identity = identity,
                threadRef =
                    "thread.self-development.open-conversation",
            )

        assertEquals(
            "config/home.json",
            coordinates.homeManifest,
        )
        assertEquals(
            "devices/${identity.deviceRef}.json",
            coordinates.deviceRecord,
        )
        assertEquals(
            "conversations/${identity.companionLineageRef}/thread.self-development.open-conversation/head.json",
            coordinates.conversationHead,
        )
        assertEquals(
            "score/${identity.companionLineageRef}/thread.self-development.open-conversation/head.json",
            coordinates.scoreHead,
        )
        assertEquals(
            "daily-memory-dream/${identity.companionLineageRef}/thread.self-development.open-conversation/head.json",
            coordinates.dailyMemoryHead,
        )
        assertEquals(
            "runtime/vexlife-prod/model-binding.json",
            coordinates.productModelBinding,
        )
        assertEquals(
            "runtime/product-service",
            coordinates.productServiceRoot,
        )
    }

    @Test
    fun repositoryProductNameCannotMintAnotherHumanVexIdentity() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeContinuity.HomeIdentity(
                homeRef =
                    "vex-home.prod.00000000-0000-0000-0000-000000000001",
                familyRef =
                    "vex-family.local-user",
                deviceRef =
                    "device.vexlife-prod.00000000-0000-0000-0000-000000000001",
                companionLineageRef =
                    "companion-lineage.vexlife-prod.00000000-0000-0000-0000-000000000002",
            )
        }
    }
}

// [VXG RealForever]
