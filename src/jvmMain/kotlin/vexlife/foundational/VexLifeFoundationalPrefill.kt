package vexlife.foundational

import vexlife.companion.DirectCompanionCancellation
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectPromptContext
import vexlife.context.VexLifeContextProfile
import vexlife.model.heretic.HereticModelConfiguration

/**
 * No-effect bridge from the prompt owner's current Foundation prefix to the provider's reusable
 * system-prefix cache.
 *
 * This owner never authors Foundation text. Every attempt rereads [VexLifeFoundationalField], and
 * [DirectPromptContext] composes that Product Foundation with the active Context Profile extension
 * when admitted. Provider framing is delegated to [DirectCompanionConnection]. No Conversation,
 * Memory, prompt receipt or generated model text is written.
 */
internal object VexLifeFoundationalPrefill {
    enum class Disposition {
        PREFILLED,
        HELD_FOUNDATION,
        HELD_PROVIDER_PROFILE,
        FAILED,
    }

    data class Result(
        val disposition: Disposition,
        val fieldRef: String?,
        val fieldVersion: String?,
        val bodySha256: String?,
        /** SHA of the exact combined Foundation prefix sent to the provider. */
        val systemMessageSha256: String?,
        val productFoundationSystemMessageSha256: String?,
        val contextProfileRef: String?,
        val contextProfileFingerprint: String?,
        val profileFoundationSystemMessageSha256: String?,
        val runtimeReceiptRef: String?,
        val modelRef: String?,
        val providerPrefillTokens: Int?,
        val providerCachedPromptTokens: Int?,
        val providerPromptTokens: Int?,
        val providerTotalMillis: Long?,
        val heldReasonOrNull: String?,
        val failureClassOrNull: String?,
        val effectsPerformed: Boolean = false,
    )

    internal fun interface ProviderPrefill {
        fun prefill(
            runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
            systemContent: String,
            cancellation: DirectCompanionCancellation,
        ): DirectCompanionConnection.SystemPrefixPrefillResult
    }

    internal fun perform(
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        contextProfile: VexLifeContextProfile.Effective? = null,
        cancellation: DirectCompanionCancellation,
        providerPrefill: ProviderPrefill =
            ProviderPrefill {
                currentRuntime,
                systemContent,
                currentCancellation,
                ->
                DirectCompanionConnection.prefillStableSystemPrefix(
                    runtime = currentRuntime,
                    systemContent = systemContent,
                    cancellation = currentCancellation,
                )
            },
    ): Result {
        cancellation.checkpoint()
        val projection =
            DirectPromptContext.projectFoundationalPrefill(
                foundationalField =
                    VexLifeFoundationalField.readCurrent(),
                contextProfile = contextProfile,
            )
        val evidence =
            projection.productFoundation
        val systemContent =
            projection.systemContent
        if (
            evidence.state ==
                VexLifeFoundationalField.State.HELD ||
            systemContent ==
                null
        ) {
            return Result(
                disposition = Disposition.HELD_FOUNDATION,
                fieldRef = evidence.fieldRef,
                fieldVersion = evidence.fieldVersion,
                bodySha256 = evidence.bodySha256,
                systemMessageSha256 = projection.systemMessageSha256,
                productFoundationSystemMessageSha256 = evidence.systemMessageSha256,
                contextProfileRef = projection.contextProfileRef,
                contextProfileFingerprint = projection.contextProfileFingerprint,
                profileFoundationSystemMessageSha256 =
                    projection.profileFoundationSystemMessageSha256,
                runtimeReceiptRef = runtime.runtimeReceiptRef,
                modelRef = runtime.modelRef,
                providerPrefillTokens = null,
                providerCachedPromptTokens = null,
                providerPromptTokens = null,
                providerTotalMillis = null,
                heldReasonOrNull =
                    evidence.heldReason
                        ?.name
                        ?: "FOUNDATION_NOT_CARRIED",
                failureClassOrNull = null,
            )
        }

        return try {
            when (
                val provider =
                    providerPrefill.prefill(
                        runtime = runtime,
                        systemContent = systemContent,
                        cancellation = cancellation,
                    )
            ) {
                is DirectCompanionConnection.SystemPrefixPrefillResult.Performed -> {
                    require(
                        provider.runtimeReceiptRef ==
                            runtime.runtimeReceiptRef &&
                            provider.modelRef ==
                            runtime.modelRef &&
                            provider.systemMessageSha256 ==
                            projection.systemMessageSha256
                    ) {
                        "Foundation prefill provider result does not bind the exact projected runtime/prefix"
                    }
                    Result(
                        disposition = Disposition.PREFILLED,
                        fieldRef = evidence.fieldRef,
                        fieldVersion = evidence.fieldVersion,
                        bodySha256 = evidence.bodySha256,
                        systemMessageSha256 = projection.systemMessageSha256,
                        productFoundationSystemMessageSha256 = evidence.systemMessageSha256,
                        contextProfileRef = projection.contextProfileRef,
                        contextProfileFingerprint = projection.contextProfileFingerprint,
                        profileFoundationSystemMessageSha256 =
                            projection.profileFoundationSystemMessageSha256,
                        runtimeReceiptRef = provider.runtimeReceiptRef,
                        modelRef = provider.modelRef,
                        providerPrefillTokens = provider.prefillTokens,
                        providerCachedPromptTokens = provider.cachedPromptTokens,
                        providerPromptTokens = provider.promptTokens,
                        providerTotalMillis = provider.totalMillis,
                        heldReasonOrNull = null,
                        failureClassOrNull = null,
                    )
                }

                is DirectCompanionConnection.SystemPrefixPrefillResult.Held ->
                    Result(
                        disposition = Disposition.HELD_PROVIDER_PROFILE,
                        fieldRef = evidence.fieldRef,
                        fieldVersion = evidence.fieldVersion,
                        bodySha256 = evidence.bodySha256,
                        systemMessageSha256 = projection.systemMessageSha256,
                        productFoundationSystemMessageSha256 = evidence.systemMessageSha256,
                        contextProfileRef = projection.contextProfileRef,
                        contextProfileFingerprint = projection.contextProfileFingerprint,
                        profileFoundationSystemMessageSha256 =
                            projection.profileFoundationSystemMessageSha256,
                        runtimeReceiptRef = runtime.runtimeReceiptRef,
                        modelRef = runtime.modelRef,
                        providerPrefillTokens = null,
                        providerCachedPromptTokens = null,
                        providerPromptTokens = null,
                        providerTotalMillis = null,
                        heldReasonOrNull = provider.reasonRef,
                        failureClassOrNull = null,
                    )
            }
        } catch (
            cancelled: DirectCompanionCancellation.Cancelled,
        ) {
            throw cancelled
        } catch (
            error: Throwable,
        ) {
            Result(
                disposition = Disposition.FAILED,
                fieldRef = evidence.fieldRef,
                fieldVersion = evidence.fieldVersion,
                bodySha256 = evidence.bodySha256,
                systemMessageSha256 = projection.systemMessageSha256,
                productFoundationSystemMessageSha256 = evidence.systemMessageSha256,
                contextProfileRef = projection.contextProfileRef,
                contextProfileFingerprint = projection.contextProfileFingerprint,
                profileFoundationSystemMessageSha256 =
                    projection.profileFoundationSystemMessageSha256,
                runtimeReceiptRef = runtime.runtimeReceiptRef,
                modelRef = runtime.modelRef,
                providerPrefillTokens = null,
                providerCachedPromptTokens = null,
                providerPromptTokens = null,
                providerTotalMillis = null,
                heldReasonOrNull = null,
                failureClassOrNull = error.javaClass.simpleName,
            )
        }
    }
}

// [VXG RealForever]
