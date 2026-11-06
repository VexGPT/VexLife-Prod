package vexlife.conversation

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/**
 * Test-only writer for the exact canonical lived VexLife bounded-context contract.
 *
 * Tests that are about Conversation/prompt selection should not be forced to invent provider runtime
 * evidence just to advance a canonical completed head. Production turn code uses
 * DirectTurnContextStore instead.
 */
object CanonicalLivedContextFixture {
    data class Formed(
        val relativePath: String,
        val sha256: String,
    )

    fun write(
        productHome: Path,
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        formedAt: Instant,
    ): Formed {
        require(
            request.eventKind ==
                VexLifeConversationContract.RequestKind &&
                response.eventKind ==
                VexLifeConversationContract.ResponseKind &&
                response.priorEventHash ==
                request.eventHash &&
                response.turnRef ==
                request.turnRef &&
                response.instanceRef ==
                request.instanceRef,
        ) {
            "canonical context fixture requires one exact lived request/response pair"
        }

        val core =
            VexLifeConversationContract.ContextCore(
                schemaVersion = VexLifeConversationContract.ContextSchema,
                homeRef = request.homeRef,
                deviceRef = request.deviceRef,
                companionLineageRef = request.companionLineageRef,
                instanceRef = request.instanceRef,
                threadRef = request.threadRef,
                turnRef = request.turnRef,
                contextSourceRefs =
                    listOf(
                        request.eventRef,
                        response.eventRef,
                    ),
                requestEventHash = request.eventHash,
                responseEventHash = response.eventHash,
                promptContextMaterializationReceipt = null,
                privacyClass = VexLifeConversationContract.PrivacyClass,
                formedAt = formedAt.toString(),
            )
                .let(
                    VexLifeConversationContract::validateContextCore,
                )
        val hash =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        val value =
            VexLifeConversationContract.Context(
                schemaVersion = core.schemaVersion,
                homeRef = core.homeRef,
                deviceRef = core.deviceRef,
                companionLineageRef = core.companionLineageRef,
                instanceRef = core.instanceRef,
                threadRef = core.threadRef,
                turnRef = core.turnRef,
                contextSourceRefs = core.contextSourceRefs,
                requestEventHash = core.requestEventHash,
                responseEventHash = core.responseEventHash,
                promptContextMaterializationReceipt = core.promptContextMaterializationReceipt,
                privacyClass = core.privacyClass,
                formedAt = core.formedAt,
                serializedContextSha256 = hash,
            )
                .let(
                    VexLifeConversationContract::validateContext,
                )

        val relative =
            "context/" +
                request.companionLineageRef +
                "/" +
                request.threadRef +
                "/" +
                request.turnRef +
                ".json"
        VexDurableContractFile.write(
            ownedRoot = productHome,
            path =
                VexOwnedPath.resolveInside(
                    productHome,
                    relative,
                ),
            value = value,
        )

        return Formed(
            relativePath = relative,
            sha256 = hash,
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
