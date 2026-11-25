package vexlife.capability

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.sources.VexLifeLocalSourceStore
import vexlife.workspace.VexLifeWorkspaceEffectStore
import vexlife.workspace.VexLifeWorkspaceEffectsFamily

/**
 * Recovery-guarded UTF-8 text mutation inside one exact current Workspace Effect grant.
 *
 * This owner deliberately does not create directories, run commands, publish, or grant authority.
 * The caller supplies one host-owned operationRef. An immutable intent is persisted before the
 * external workspace mutation so a later retry can reobserve and recover rather than replay.
 */
class VexLifeWorkspaceWrite(
    productHome: Path,
    private val localSources: VexLifeLocalSourceStore =
        VexLifeLocalSourceStore(productHome),
    private val workspaceEffects: VexLifeWorkspaceEffectStore =
        VexLifeWorkspaceEffectStore(productHome, localSources),
    private val now: () -> Instant = Instant::now,
) {
    @Serializable
    private data class BeforeImage(
        val schemaVersion: String,
        val intentRef: String,
        val sourceRef: String,
        val relativePath: String,
        val sha256: String,
        /** Private recovery custody; never projected into the capability result. */
        val content: String,
        val capturedAt: String,
    )

    @Serializable
    private data class Intent(
        val schemaVersion: String,
        val intentRef: String,
        val operationRef: String,
        val grantRef: String,
        val sourceRef: String,
        val sourceGrantedAt: String,
        val relativePath: String,
        val expectedPriorSha256: String?,
        val observedPriorSha256: String?,
        val newSha256: String,
        val bytes: Int,
        val beforeImageRefOrNull: String?,
        val formedAt: String,
    )

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val receiptRef: String,
        val intentRef: String,
        val operationRef: String,
        val grantRef: String,
        val sourceRef: String,
        val relativePath: String,
        val priorSha256: String?,
        val newSha256: String,
        val bytes: Int,
        val disposition: String,
        val completedAt: String,
        val externalEffectsExecuted: Boolean,
        val effectAuthorityGranted: Boolean,
    ) {
        init {
            require(
                schemaVersion == ReceiptSchema &&
                    receiptRef.isNotBlank() &&
                    intentRef.isNotBlank() &&
                    operationRef.isNotBlank() &&
                    VexLifeWorkspaceEffectsFamily.isGrantRef(grantRef) &&
                    priorSha256?.let(Sha256::matches) != false &&
                    Sha256.matches(newSha256) &&
                    bytes in 0..MaximumContentBytes &&
                    disposition in
                    setOf(
                        DispositionCreated,
                        DispositionReplaced,
                        DispositionRecovered,
                    ) &&
                    externalEffectsExecuted &&
                    effectAuthorityGranted,
            ) {
                "workspace write receipt is invalid"
            }
        }
    }

    sealed interface Outcome {
        /**
         * Call-local truth stays separate from the durable receipt.
         *
         * A durable receipt describes the completed intent historically. Returning/recovering that
         * evidence later does not mean this invocation performed another external effect or that
         * write authority is still current.
         */
        data class Completed(
            val receipt: Receipt,
            val effectPerformedNow: Boolean,
            val currentEffectAuthority: Boolean,
        ) : Outcome

        data class Refused(
            val reason: String,
            val intentRef: String? = null,
            val observedSha256: String? = null,
        ) : Outcome

        /**
         * Mutation may have happened but a current verified receipt could not be formed.
         * The same operationRef must be reobserved; callers must not mint a new replay.
         */
        data class UnknownAfterAttempt(
            val intentRef: String,
            val reason: String,
            val observedSha256: String?,
        ) : Outcome
    }

    private val home =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    fun write(
        operationRef: String,
        sourceRef: String,
        relativePath: String,
        content: String,
        expectedPriorSha256: String? = null,
    ): Outcome {
        if (operationRef.isBlank() || operationRef.length > MaximumOperationRefCharacters) {
            return Outcome.Refused(RefusedOperationRef)
        }
        val normalizedRelative =
            normalizeRelative(relativePath)
                ?: return Outcome.Refused(RefusedPath)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MaximumContentBytes) {
            return Outcome.Refused(RefusedContentTooLarge)
        }
        if (expectedPriorSha256 != null && !Sha256.matches(expectedPriorSha256)) {
            return Outcome.Refused(RefusedExpectedSha)
        }

        val operationKey = sha256(operationRef)
        val intentPath = intentPath(operationKey)
        val receiptPath = receiptPath(operationKey)
        val newSha = sha256(bytes)

        // Completed evidence remains readable even if authority was later revoked. Returning an
        // existing receipt performs no workspace effect.
        val existingReceipt =
            runCatching {
                    VexDurableContractFile.read<Receipt>(
                        ownedRoot = home,
                        path = receiptPath,
                    )
                }
                .getOrElse {
                    return Outcome.Refused(RefusedReceiptInvalid)
                }
        if (existingReceipt != null) {
            return if (
                existingReceipt.operationRef == operationRef &&
                    existingReceipt.sourceRef == sourceRef &&
                    existingReceipt.relativePath == normalizedRelative &&
                    existingReceipt.newSha256 == newSha &&
                    existingReceipt.priorSha256 == expectedPriorSha256
            ) {
                Outcome.Completed(
                    receipt = existingReceipt,
                    effectPerformedNow = false,
                    currentEffectAuthority =
                        workspaceEffects
                            .currentGrant(
                                sourceRef,
                            )
                            ?.grantRef ==
                            existingReceipt.grantRef,
                )
            } else {
                Outcome.Refused(RefusedOperationConflict, existingReceipt.intentRef)
            }
        }

        val existingIntent =
            runCatching {
                    VexDurableContractFile.read<Intent>(
                        ownedRoot = home,
                        path = intentPath,
                    )
                }
                .getOrElse {
                    return Outcome.Refused(RefusedIntentInvalid)
                }

        // A readable Local Source is needed to observe the external target. It does not itself
        // authorize another mutation.
        val root =
            localSources.root(sourceRef)
                ?: return Outcome.Refused(
                    RefusedSourceNotCurrent,
                    existingIntent?.intentRef,
                )
        val target =
            VexOwnedPath.resolveInside(
                root,
                normalizedRelative,
            )
        if (
            VexOwnedPath.observeParent(
                root,
                target,
            ) !=
            VexOwnedPath.DirectoryState.Present
        ) {
            return Outcome.Refused(
                RefusedParentMissing,
                existingIntent?.intentRef,
            )
        }

        if (existingIntent != null) {
            if (
                existingIntent.operationRef != operationRef ||
                existingIntent.sourceRef != sourceRef ||
                existingIntent.relativePath != normalizedRelative ||
                existingIntent.expectedPriorSha256 != expectedPriorSha256 ||
                existingIntent.newSha256 != newSha ||
                existingIntent.bytes != bytes.size
            ) {
                return Outcome.Refused(
                    RefusedOperationConflict,
                    existingIntent.intentRef,
                    observeSha(target),
                )
            }

            // Recovery-before-replay is lawful without renewed write authority: no external effect
            // occurs when the already-intended bytes are simply reobserved and receipted.
            val observed = observe(target)
            if (observed is TargetObservation.Present && observed.sha256 == newSha) {
                val currentGrant =
                    workspaceEffects.currentGrant(
                        sourceRef,
                    )
                return complete(
                    intent = existingIntent,
                    receiptPath = receiptPath,
                    disposition = DispositionRecovered,
                    effectPerformedNow = false,
                    currentEffectAuthority =
                        currentGrant !=
                            null &&
                            currentGrant.grantRef ==
                            existingIntent.grantRef &&
                            currentGrant.sourceGrantedAt ==
                            existingIntent.sourceGrantedAt,
                )
            }

            // Any actual replay still requires the exact same current effect-grant generation that
            // formed the intent. Revocation or re-grant never reactivates an old mutation.
            val currentGrant =
                workspaceEffects.currentGrant(sourceRef)
                    ?: return Outcome.Refused(
                        RefusedGrantNotCurrent,
                        existingIntent.intentRef,
                        (observed as? TargetObservation.Present)?.sha256,
                    )
            if (
                currentGrant.grantRef != existingIntent.grantRef ||
                currentGrant.sourceGrantedAt != existingIntent.sourceGrantedAt
            ) {
                return Outcome.Refused(
                    RefusedGrantGenerationChanged,
                    existingIntent.intentRef,
                    (observed as? TargetObservation.Present)?.sha256,
                )
            }
            return attemptMutation(
                intent = existingIntent,
                target = target,
                receiptPath = receiptPath,
                bytes = bytes,
                observation = observed,
            )
        }

        val grant =
            workspaceEffects.currentGrant(sourceRef)
                ?: return Outcome.Refused(RefusedGrantNotCurrent)
        val prior =
            when (val observation = observe(target)) {
                is TargetObservation.Invalid ->
                    return Outcome.Refused(observation.reason)

                is TargetObservation.Absent ->
                    if (expectedPriorSha256 == null) {
                        null
                    } else {
                        return Outcome.Refused(RefusedExpectedMissing)
                    }

                is TargetObservation.Present -> {
                    if (expectedPriorSha256 == null) {
                        return Outcome.Refused(
                            RefusedCreateTargetExists,
                            observedSha256 = observation.sha256,
                        )
                    }
                    if (observation.sha256 != expectedPriorSha256) {
                        return Outcome.Refused(
                            RefusedStaleExpectation,
                            observedSha256 = observation.sha256,
                        )
                    }
                    observation
                }
            }

        val intentRef = IntentRefPrefix + operationKey
        val beforeImageRef =
            prior?.let {
                val before =
                    BeforeImage(
                        schemaVersion = BeforeImageSchema,
                        intentRef = intentRef,
                        sourceRef = sourceRef,
                        relativePath = normalizedRelative,
                        sha256 = it.sha256,
                        content = it.content,
                        capturedAt = now().toString(),
                    )
                VexDurableContractFile.write(
                    ownedRoot = home,
                    path = beforeImagePath(operationKey),
                    value = before,
                )
                BeforeImageRefPrefix + operationKey
            }
        val intent =
            Intent(
                schemaVersion = IntentSchema,
                intentRef = intentRef,
                operationRef = operationRef,
                grantRef = grant.grantRef,
                sourceRef = sourceRef,
                sourceGrantedAt = grant.sourceGrantedAt,
                relativePath = normalizedRelative,
                expectedPriorSha256 = expectedPriorSha256,
                observedPriorSha256 = prior?.sha256,
                newSha256 = newSha,
                bytes = bytes.size,
                beforeImageRefOrNull = beforeImageRef,
                formedAt = now().toString(),
            )
        runCatching {
                VexDurableContractFile.write(
                    ownedRoot = home,
                    path = intentPath,
                    value = intent,
                )
            }
            .getOrElse {
                return Outcome.Refused(RefusedIntentPersist)
            }

        // Reobserve after the intent becomes durable so a concurrent external change cannot be
        // silently overwritten merely because it matched before intent persistence.
        return attemptMutation(
            intent = intent,
            target = target,
            receiptPath = receiptPath,
            bytes = bytes,
            observation = observe(target),
        )
    }

    private fun attemptMutation(
        intent: Intent,
        target: Path,
        receiptPath: Path,
        bytes: ByteArray,
        observation: TargetObservation,
    ): Outcome {
        when (observation) {
            is TargetObservation.Invalid ->
                return Outcome.Refused(observation.reason, intent.intentRef)

            is TargetObservation.Absent ->
                if (intent.observedPriorSha256 != null) {
                    return Outcome.Refused(RefusedStaleExpectation, intent.intentRef)
                }

            is TargetObservation.Present ->
                if (observation.sha256 != intent.observedPriorSha256) {
                    return Outcome.Refused(
                        RefusedStaleExpectation,
                        intent.intentRef,
                        observation.sha256,
                    )
                }
        }

        val temporary =
            target.resolveSibling(
                "." + target.fileName + ".vex-partial-" + UUID.randomUUID(),
            )
        var moveAttempted = false
        try {
            Files.write(
                temporary,
                bytes,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
            moveAttempted = true
            if (intent.observedPriorSha256 == null) {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } else {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } catch (_: AtomicMoveNotSupportedException) {
            return Outcome.Refused(
                RefusedAtomicMoveUnavailable,
                intent.intentRef,
                observeSha(target),
            )
        } catch (error: Exception) {
            val observed = observeSha(target)
            return if (moveAttempted) {
                Outcome.UnknownAfterAttempt(
                    intentRef = intent.intentRef,
                    reason = error.message ?: error.javaClass.simpleName,
                    observedSha256 = observed,
                )
            } else {
                Outcome.Refused(
                    reason = error.message ?: RefusedWriteFailed,
                    intentRef = intent.intentRef,
                    observedSha256 = observed,
                )
            }
        } finally {
            runCatching { Files.deleteIfExists(temporary) }
        }

        val post = observe(target)
        if (post !is TargetObservation.Present || post.sha256 != intent.newSha256) {
            return Outcome.UnknownAfterAttempt(
                intentRef = intent.intentRef,
                reason = "workspace target did not reobserve the intended content after atomic move",
                observedSha256 = (post as? TargetObservation.Present)?.sha256,
            )
        }

        return complete(
            intent = intent,
            receiptPath = receiptPath,
            disposition =
                if (intent.observedPriorSha256 == null) {
                    DispositionCreated
                } else {
                    DispositionReplaced
                },
            effectPerformedNow = true,
            currentEffectAuthority = true,
        )
    }

    private fun complete(
        intent: Intent,
        receiptPath: Path,
        disposition: String,
        effectPerformedNow: Boolean,
        currentEffectAuthority: Boolean,
    ): Outcome {
        val receipt =
            Receipt(
                schemaVersion = ReceiptSchema,
                receiptRef = ReceiptRefPrefix + sha256(intent.operationRef),
                intentRef = intent.intentRef,
                operationRef = intent.operationRef,
                grantRef = intent.grantRef,
                sourceRef = intent.sourceRef,
                relativePath = intent.relativePath,
                priorSha256 = intent.observedPriorSha256,
                newSha256 = intent.newSha256,
                bytes = intent.bytes,
                disposition = disposition,
                completedAt = now().toString(),
                externalEffectsExecuted = true,
                effectAuthorityGranted = true,
            )
        return runCatching {
                VexDurableContractFile.write(
                    ownedRoot = home,
                    path = receiptPath,
                    value = receipt,
                )
                val readback =
                    requireNotNull(
                        VexDurableContractFile.read<Receipt>(
                            ownedRoot = home,
                            path = receiptPath,
                        )
                    )
                require(readback == receipt) {
                    "workspace write receipt changed after persistence"
                }
                Outcome.Completed(
                    receipt = receipt,
                    effectPerformedNow = effectPerformedNow,
                    currentEffectAuthority = currentEffectAuthority,
                )
            }
            .getOrElse {
                Outcome.UnknownAfterAttempt(
                    intentRef = intent.intentRef,
                    reason = it.message ?: it.javaClass.simpleName,
                    observedSha256 = intent.newSha256,
                )
            }
    }

    private sealed interface TargetObservation {
        data object Absent : TargetObservation

        data class Present(
            val sha256: String,
            val content: String,
        ) : TargetObservation

        data class Invalid(val reason: String) : TargetObservation
    }

    private fun observe(target: Path): TargetObservation {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return TargetObservation.Absent
        }
        val attributes =
            runCatching {
                    Files.readAttributes(
                        target,
                        BasicFileAttributes::class.java,
                        LinkOption.NOFOLLOW_LINKS,
                    )
                }
                .getOrElse {
                    return TargetObservation.Invalid(RefusedTargetInvalid)
                }
        if (
            !attributes.isRegularFile ||
            attributes.isSymbolicLink ||
            attributes.size() > MaximumContentBytes
        ) {
            return TargetObservation.Invalid(RefusedTargetInvalid)
        }
        val content =
            runCatching {
                    Files.readString(
                        target,
                        StandardCharsets.UTF_8,
                    )
                }
                .getOrElse {
                    return TargetObservation.Invalid(RefusedTargetNotUtf8)
                }
        return TargetObservation.Present(
            sha256 = sha256(content.toByteArray(StandardCharsets.UTF_8)),
            content = content,
        )
    }

    private fun observeSha(target: Path): String? =
        (observe(target) as? TargetObservation.Present)?.sha256

    private fun normalizeRelative(value: String): String? {
        if (
            value.isBlank() ||
            value.length > MaximumRelativePathCharacters ||
            value.contains('\\')
        ) {
            return null
        }
        val parts = value.split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) {
            return null
        }
        val path = runCatching { Path.of(value) }.getOrNull() ?: return null
        if (path.isAbsolute) return null
        return path.normalize().toString().replace('\\', '/')
    }

    private fun intentPath(key: String): Path =
        VexOwnedPath.resolveInside(home, RuntimeRoot + "/intents/" + key + ".json")

    private fun receiptPath(key: String): Path =
        VexOwnedPath.resolveInside(home, RuntimeRoot + "/receipts/" + key + ".json")

    private fun beforeImagePath(key: String): Path =
        VexOwnedPath.resolveInside(home, RuntimeRoot + "/before-images/" + key + ".json")

    private fun sha256(value: String): String =
        sha256(value.toByteArray(StandardCharsets.UTF_8))

    private fun sha256(value: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        const val IntentSchema = "vexlife-prod.workspace-write-intent/v1"
        const val BeforeImageSchema = "vexlife-prod.workspace-write-before-image/v1"
        const val ReceiptSchema = "vexlife-prod.workspace-write-receipt/v1"

        const val IntentRefPrefix = "intent.vexlife-prod.workspace-write.sha256."
        const val BeforeImageRefPrefix = "before-image.vexlife-prod.workspace-write.sha256."
        const val ReceiptRefPrefix = "receipt.vexlife-prod.workspace-write.sha256."

        const val RuntimeRoot = "runtime/vexlife-prod/workspace-effects"
        const val MaximumContentBytes = 256 * 1024
        const val MaximumRelativePathCharacters = 512
        const val MaximumOperationRefCharacters = 256

        const val DispositionCreated = "CREATED"
        const val DispositionReplaced = "REPLACED"
        const val DispositionRecovered = "RECOVERED_AFTER_OBSERVATION"

        const val RefusedOperationRef = "WORKSPACE_OPERATION_REF_INVALID"
        const val RefusedGrantNotCurrent = "WORKSPACE_EFFECT_GRANT_NOT_CURRENT"
        const val RefusedGrantGenerationChanged = "WORKSPACE_EFFECT_GRANT_GENERATION_CHANGED"
        const val RefusedSourceNotCurrent = "WORKSPACE_SOURCE_NOT_CURRENT"
        const val RefusedPath = "WORKSPACE_PATH_INVALID"
        const val RefusedParentMissing = "WORKSPACE_PARENT_MISSING"
        const val RefusedContentTooLarge = "WORKSPACE_CONTENT_TOO_LARGE"
        const val RefusedExpectedSha = "WORKSPACE_EXPECTED_SHA_INVALID"
        const val RefusedExpectedMissing = "WORKSPACE_EXPECTED_FILE_MISSING"
        const val RefusedCreateTargetExists = "WORKSPACE_CREATE_TARGET_EXISTS"
        const val RefusedStaleExpectation = "WORKSPACE_EXPECTED_SHA_STALE"
        const val RefusedTargetInvalid = "WORKSPACE_TARGET_NOT_REGULAR_TEXT_FILE"
        const val RefusedTargetNotUtf8 = "WORKSPACE_TARGET_NOT_UTF8"
        const val RefusedIntentInvalid = "WORKSPACE_INTENT_INVALID"
        const val RefusedIntentPersist = "WORKSPACE_INTENT_PERSIST_FAILED"
        const val RefusedReceiptInvalid = "WORKSPACE_RECEIPT_INVALID"
        const val RefusedOperationConflict = "WORKSPACE_OPERATION_REF_CONFLICT"
        const val RefusedAtomicMoveUnavailable = "WORKSPACE_ATOMIC_MOVE_UNAVAILABLE"
        const val RefusedWriteFailed = "WORKSPACE_WRITE_FAILED"

        private val Sha256 = Regex("[0-9a-f]{64}")
    }
}

// [VXG RealForever]
