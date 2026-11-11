package vexlife.companion

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.continuity.VexLifeWorkingCarryContract
import vexlife.contract.VexContractJson

/**
 * Provider-ready projection of one current Vex present working carry.
 *
 * RELEASE occupies no prompt space. CARRY is one compact source-linked system message whose complete
 * serialized message must fit the caller-supplied carry allowance.
 */
object DirectWorkingCarryPromptContext {
    const val PacketSchema =
        "vexlife-prod.working-carry-prompt/v1"

    const val Header =
        "VEXLIFE_PRESENT_WORKING_CARRY/v1"

    const val OwnerCurrent =
        "OWNER_CURRENT"

    const val TurnCandidate =
        "TURN_CANDIDATE"

    @Serializable
    private data class Packet(
        val schemaVersion: String,
        val truthClass: String,
        val perspective: String,
        val sourceState: String,
        val sourceBindings: List<VexLifeWorkingCarryContract.SourceBinding>,
        val carryItems: List<VexLifeWorkingCarryContract.CarryItem>,
    )

    @ConsistentCopyVisibility
    data class Projection internal constructor(
        val included: Boolean,
        val sourceState: String,
        val systemContent: String?,
        val systemMessageSha256: String?,
        val estimatedSystemMessageTokens: Int,
        val sourceEventRefs: List<String>,
    )

    fun materialize(
        draft: VexLifeWorkingCarryContract.Draft,
        sourceState: String,
    ): Projection {
        val current =
            VexLifeWorkingCarryContract.requireDraftShape(
                draft,
            )
        require(
            sourceState in
                setOf(
                    OwnerCurrent,
                    TurnCandidate,
                ),
        ) {
            "working carry prompt source state is not admitted"
        }
        require(
            current.carryTokenCap <=
                DirectCompanionConnection.OutputTokenLimit,
        ) {
            "working carry prompt allowance exceeds the current local-model output ceiling"
        }

        if (
            current.disposition ==
                VexLifeWorkingCarryContract.Release
        ) {
            return Projection(
                included = false,
                sourceState = sourceState,
                systemContent = null,
                systemMessageSha256 = null,
                estimatedSystemMessageTokens = 0,
                sourceEventRefs = emptyList(),
            )
        }

        val packet =
            Packet(
                schemaVersion = PacketSchema,
                truthClass = current.truthClass,
                perspective = current.perspective,
                sourceState = sourceState,
                sourceBindings = current.sourceBindings,
                carryItems = current.carryItems,
            )
        val systemContent =
            listOf(
                Header,
                "This is Vex's source-linked, self-relative present working continuity.",
                "It is not durable Memory, canonical Conversation, a human profile, or a relationship agreement.",
                "If this working interpretation conflicts with the exact current human request or exact Conversation text present in this prompt, follow the exact current source and revise the interpretation rather than defending it.",
                "Source bindings are return coordinates. Do not invent source content that is not directly present.",
                VexContractJson.compactEncode(
                    packet,
                ),
            )
                .joinToString(
                    "\n",
                )
        val estimate =
            DirectPromptContext.estimatedMessageTokens(
                systemContent,
            )
        require(
            estimate <=
                current.carryTokenCap,
        ) {
            "working carry provider projection exceeds its reserved carry allowance"
        }

        return Projection(
            included = true,
            sourceState = sourceState,
            systemContent = systemContent,
            systemMessageSha256 =
                sha256(
                    systemContent,
                ),
            estimatedSystemMessageTokens = estimate,
            sourceEventRefs =
                current.sourceBindings.map(
                    VexLifeWorkingCarryContract.SourceBinding::eventRef,
                ),
        )
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
