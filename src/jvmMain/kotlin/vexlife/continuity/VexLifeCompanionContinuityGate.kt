package vexlife.continuity

/**
 * Temporary executable gate for the active New Beginnings semantic realignment.
 *
 * The old product-specific Conversation store is still useful as conversion/test machinery, but it
 * is not compatible with the canonical lived VexLife event/head/context bytes. The current Memory
 * contract is also formed before its real Score/daily/Living-Journal reader. Production service
 * startup therefore fails before opening a listener or accepting a Send.
 *
 * Remove this gate only in the same exact candidate that proves:
 * 1. canonical Conversation event/head/context read + write compatibility;
 * 2. real current Memory projection from canonical Home source;
 * 3. prompt receipt/source projection through the service;
 * 4. whole-turn recovery without a second Conversation owner.
 */
object VexLifeCompanionContinuityGate {
    const val State =
        "HELD_PENDING_CANONICAL_CONVERSATION_AND_MEMORY_PORT"

    const val ExactNextOwner =
        "VexLife-Prod continuity realignment"

    fun requireProductionReady(): Nothing =
        error(
            "VexLife Companion production service is held until canonical Conversation persistence " +
                "and the real current-Memory reader are proven on the same exact source",
        )
}

// [VXG RealForever]
