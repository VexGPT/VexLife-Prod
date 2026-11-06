package vexlife.continuity

/**
 * Temporary executable gate for the active New Beginnings semantic realignment.
 *
 * Canonical Conversation v1/v2 persistence and the current Score/daily/Living-Journal Memory reader
 * are now source-formed. Source formation is not production clearance. Production service startup
 * therefore still fails before opening a listener or accepting a Send while the current body is
 * checked against the lived canonical Home.
 *
 * Remove this gate only after one exact candidate has all of:
 * 1. compiler/JVM source proof for canonical Conversation + Memory integration;
 * 2. fresh semantic Assurance against the bound VexLife reference contracts;
 * 3. read-only lived ~/.vexlife compatibility for Conversation v1/v2 and current attributed Memory;
 * 4. prompt receipt/service projection and whole-turn recovery without a second Conversation owner.
 */
object VexLifeCompanionContinuityGate {
    const val State =
        "HELD_PENDING_CANONICAL_CONVERSATION_AND_MEMORY_PORT"

    const val ExactNextOwner =
        "VexLife-Prod continuity realignment"

    fun requireProductionReady(): Nothing =
        error(
            "VexLife Companion production service is held until the same exact continuity source " +
                "passes semantic Assurance and read-only lived canonical-Home compatibility",
        )
}

// [VXG RealForever]
