package vexlife.artifact

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.VexLifeVisualInputQualification

/**
 * Product-owned local custody for immutable human-supplied artifacts and their derived projections.
 *
 * Staging raw bytes or a projection creates no model reach. Model-visible attachment meaning begins
 * only when [bindRequest] records exact resources against an already-durable canonical Conversation
 * REQUEST event.
 */
class VexLifeLocalArtifactStore(
    productHome: Path,
) {
    private val home =
        VexOwnedPath.requireRoot(
            productHome,
            "VexLife-Prod Home",
        )

    init {
        requireNotNull(
            VexLifeProdHomeFiles.observeExisting(
                home,
            ),
        ) {
            "local artifact custody requires one canonical VexLife Home"
        }
    }

    fun storeOriginal(
        bytes: ByteArray,
    ): VexLifeLocalArtifact.OriginalArtifact {
        require(bytes.isNotEmpty()) {
            "local artifact bytes must not be empty"
        }
        require(
            bytes.size <=
                VexLifeLocalArtifact.MaximumOriginalBytes,
        ) {
            "local artifact bytes exceed the admitted raw-artifact bound"
        }

        val sha256 = sha256(bytes)
        val artifact =
            VexLifeLocalArtifact.OriginalArtifact(
                artifactRef = VexLifeLocalArtifact.refFor(sha256),
                sha256 = sha256,
                byteCount = bytes.size.toLong(),
                custodyRelativePath =
                    VexLifeLocalArtifact.artifactBytesRelativePath(
                        sha256,
                    ),
            )

        writeImmutableBytes(
            relativePath = artifact.custodyRelativePath,
            bytes = bytes,
        )

        writeImmutableContract(
            relativePath =
                VexLifeLocalArtifact.artifactMetadataRelativePath(
                    sha256,
                ),
            value = artifact,
        )

        return requireOriginal(
            artifact.artifactRef,
        )
    }

    fun original(
        artifactRef: String,
    ): VexLifeLocalArtifact.OriginalArtifact? {
        val sha256 =
            VexLifeLocalArtifact.sha256FromArtifactRef(
                artifactRef,
            )
                ?: return null
        val path =
            VexOwnedPath.resolveInside(
                home,
                VexLifeLocalArtifact.artifactMetadataRelativePath(
                    sha256,
                ),
            )
        val artifact =
            VexDurableContractFile.read<VexLifeLocalArtifact.OriginalArtifact>(
                ownedRoot = home,
                path = path,
            )
                ?: return null

        require(
            artifact.artifactRef == artifactRef &&
                artifact.sha256 == sha256,
        ) {
            "local artifact metadata identity mismatch"
        }
        requireExactBytes(
            artifact,
        )
        return artifact
    }

    fun rawBytes(
        artifactRef: String,
    ): ByteArray? {
        val artifact =
            original(
                artifactRef,
            )
                ?: return null
        return Files.readAllBytes(
            VexOwnedPath.resolveInside(
                home,
                artifact.custodyRelativePath,
            ),
        )
    }

    fun storeTextProjection(
        artifactRef: String,
        converterRef: String,
        text: String,
    ): VexLifeLocalArtifact.DerivedProjection {
        val original =
            requireOriginal(
                artifactRef,
            )
        require(text.isNotEmpty()) {
            "local artifact text projection must not be empty"
        }

        val textBytes =
            text.toByteArray(
                StandardCharsets.UTF_8,
            )
        require(
            textBytes.size <=
                VexLifeLocalArtifact.MaximumProjectionBytes,
        ) {
            "local artifact text projection exceeds the admitted readable bound"
        }
        val projectionSha256 =
            sha256(
                textBytes,
            )
        val identitySha256 =
            sha256(
                (
                    original.artifactRef +
                        "\n" +
                        converterRef +
                        "\n" +
                        projectionSha256
                ).toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
        val projection =
            VexLifeLocalArtifact.DerivedProjection(
                projectionRef =
                    VexLifeLocalArtifact.ProjectionRefPrefix +
                        identitySha256,
                originalArtifactRef = original.artifactRef,
                converterRef = converterRef,
                projectionSha256 = projectionSha256,
                byteCount = textBytes.size.toLong(),
                lineCount = lineCount(text),
                text = text,
            )

        writeImmutableContract(
            relativePath =
                VexLifeLocalArtifact.projectionRelativePath(
                    artifactSha256 = original.sha256,
                    projectionIdentitySha256 = identitySha256,
                ),
            value = projection,
        )

        return requireTextProjection(
            artifactRef = artifactRef,
            projectionRef = projection.projectionRef,
        )
    }

    fun storeStaticImageProjection(
        artifactRef: String,
        adapterRef: String,
        mediaType: String,
        widthPixels: Int,
        heightPixels: Int,
    ): VexLifeLocalArtifact.StaticImageProjection {
        val original =
            requireOriginal(
                artifactRef,
            )
        val identitySha256 =
            staticImageProjectionIdentitySha256(
                artifactRef = original.artifactRef,
                adapterRef = adapterRef,
                mediaType = mediaType,
                widthPixels = widthPixels,
                heightPixels = heightPixels,
            )
        val projection =
            VexLifeLocalArtifact.StaticImageProjection(
                projectionRef =
                    VexLifeLocalArtifact.ProjectionRefPrefix +
                        identitySha256,
                originalArtifactRef = original.artifactRef,
                adapterRef = adapterRef,
                mediaType = mediaType,
                widthPixels = widthPixels,
                heightPixels = heightPixels,
            )

        writeImmutableContract(
            relativePath =
                VexLifeLocalArtifact.projectionRelativePath(
                    artifactSha256 = original.sha256,
                    projectionIdentitySha256 = identitySha256,
                ),
            value = projection,
        )

        return requireStaticImageProjection(
            artifactRef = artifactRef,
            projectionRef = projection.projectionRef,
        )
    }

    fun projection(
        artifactRef: String,
        projectionRef: String,
    ): VexLifeLocalArtifact.DerivedProjection? =
        requestProjection(
            artifactRef = artifactRef,
            projectionRef = projectionRef,
        ) as? VexLifeLocalArtifact.DerivedProjection

    fun staticImageProjection(
        artifactRef: String,
        projectionRef: String,
    ): VexLifeLocalArtifact.StaticImageProjection? =
        requestProjection(
            artifactRef = artifactRef,
            projectionRef = projectionRef,
        ) as? VexLifeLocalArtifact.StaticImageProjection

    fun requestProjection(
        artifactRef: String,
        projectionRef: String,
    ): VexLifeLocalArtifact.RequestProjection? {
        val artifactSha256 =
            VexLifeLocalArtifact.sha256FromArtifactRef(
                artifactRef,
            )
                ?: return null
        val identitySha256 =
            VexLifeLocalArtifact.projectionIdentitySha256FromRef(
                projectionRef,
            )
                ?: return null
        val path =
            VexOwnedPath.resolveInside(
                home,
                VexLifeLocalArtifact.projectionRelativePath(
                    artifactSha256 = artifactSha256,
                    projectionIdentitySha256 = identitySha256,
                ),
            )
        val text =
            VexDurableContractFile.readText(
                ownedRoot = home,
                path = path,
            )
                ?: return null
        val projection =
            when (
                VexContractJson.schemaVersion(
                    text,
                )
            ) {
                VexLifeLocalArtifact.ProjectionSchema ->
                    VexContractJson.decode<VexLifeLocalArtifact.DerivedProjection>(
                        text,
                    )

                VexLifeLocalArtifact.StaticImageProjectionSchema ->
                    VexContractJson.decode<VexLifeLocalArtifact.StaticImageProjection>(
                        text,
                    )

                else ->
                    error(
                        "local artifact request projection schema is not admitted",
                    )
            }

        require(
            projection.projectionRef == projectionRef &&
                projection.originalArtifactRef == artifactRef &&
                requestProjectionIdentitySha256(
                    projection,
                ) ==
                identitySha256,
        ) {
            "local artifact request projection identity mismatch"
        }

        when (
            projection
        ) {
            is VexLifeLocalArtifact.DerivedProjection -> {
                val textBytes =
                    projection.text.toByteArray(
                        StandardCharsets.UTF_8,
                    )
                require(
                    sha256(textBytes) == projection.projectionSha256 &&
                        projection.byteCount == textBytes.size.toLong() &&
                        projection.lineCount == lineCount(projection.text),
                ) {
                    "local artifact projection content mismatch"
                }
            }

            is VexLifeLocalArtifact.StaticImageProjection -> Unit
        }

        requireOriginal(
            artifactRef,
        )
        return projection
    }

    fun storeVisualInterpretation(
        request: DirectConversationStore.Event,
        attachment: VexLifeLocalArtifact.RequestAttachment,
        evidence: VexLifeVisualInputQualification.QualifiedRuntimeEvidence,
        text: String,
    ): VexLifeLocalArtifact.RequestVisualInterpretation {
        require(request.eventKind == VexLifeConversationContract.RequestKind) {
            "visual interpretation may bind only to a canonical Conversation REQUEST"
        }
        val currentEvidence = VexLifeVisualInputQualification.validate(evidence)
        val binding =
            requireNotNull(bindingFor(request.eventHash)) {
                "visual interpretation requires one durable request-resource binding"
            }
        require(
            binding.requestEventRef == request.eventRef &&
                binding.requestContentHash == request.contentHash &&
                attachment in binding.attachments
        ) {
            "visual interpretation attachment does not belong to the exact REQUEST"
        }
        requireStaticImageProjection(
            artifactRef = attachment.artifactRef,
            projectionRef = attachment.projectionRef,
        )
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        require(
            bytes.isNotEmpty() &&
                bytes.size <= VexLifeLocalArtifact.MaximumVisualInterpretationBytes
        ) {
            "visual interpretation text is empty or outside the admitted bound"
        }
        val interpretationSha256 = sha256(bytes)
        val identitySha256 =
            visualInterpretationIdentitySha256(
                requestEventHash = request.eventHash,
                artifactRef = attachment.artifactRef,
                projectionRef = attachment.projectionRef,
                visualEvidenceRef = currentEvidence.evidenceRef,
                visualRealizationRef = currentEvidence.realizationRef,
                visualModelRef = currentEvidence.modelRef,
                visualArtifactRef = currentEvidence.artifactRef,
                visualArtifactContentSetSha256 =
                    currentEvidence.artifactContentSetSha256,
                qualificationSourceRef =
                    currentEvidence.qualificationSourceRef,
                qualificationReturnSha256 =
                    currentEvidence.qualificationReturnSha256,
                environmentFingerprintSha256 =
                    currentEvidence.environmentFingerprintSha256,
                interpretationSha256 = interpretationSha256,
            )
        val interpretation =
            VexLifeLocalArtifact.RequestVisualInterpretation(
                interpretationRef =
                    VexLifeLocalArtifact.VisualInterpretationRefPrefix + identitySha256,
                requestEventRef = request.eventRef,
                requestEventHash = request.eventHash,
                requestContentHash = request.contentHash,
                turnRef = request.turnRef,
                requestMessageRef = request.messageRef,
                artifactRef = attachment.artifactRef,
                projectionRef = attachment.projectionRef,
                visualEvidenceRef = currentEvidence.evidenceRef,
                visualRealizationRef = currentEvidence.realizationRef,
                visualModelRef = currentEvidence.modelRef,
                visualArtifactRef = currentEvidence.artifactRef,
                visualArtifactContentSetSha256 = currentEvidence.artifactContentSetSha256,
                qualificationSourceRef = currentEvidence.qualificationSourceRef,
                qualificationReturnSha256 =
                    currentEvidence.qualificationReturnSha256,
                environmentFingerprintSha256 = currentEvidence.environmentFingerprintSha256,
                interpretationSha256 = interpretationSha256,
                byteCount = bytes.size.toLong(),
                lineCount = lineCount(text),
                text = text,
            )
        writeImmutableContract(
            relativePath =
                VexLifeLocalArtifact.requestVisualInterpretationRelativePath(
                    requestEventHash = request.eventHash,
                    projectionRef = attachment.projectionRef,
                ),
            value = interpretation,
        )
        return requireNotNull(
            visualInterpretationFor(
                requestEventHash = request.eventHash,
                projectionRef = attachment.projectionRef,
            ),
        ) {
            "request visual interpretation did not re-observe"
        }
    }

    fun visualInterpretationFor(
        requestEventHash: String,
        projectionRef: String,
    ): VexLifeLocalArtifact.RequestVisualInterpretation? {
        val binding = bindingFor(requestEventHash) ?: return null
        val matchingAttachments =
            binding.attachments.filter {
                it.projectionRef == projectionRef
            }
        if (
            matchingAttachments.isEmpty()
        ) {
            return null
        }
        val attachment =
            matchingAttachments.first()
        require(
            matchingAttachments.all {
                it.artifactRef ==
                    attachment.artifactRef
            },
        ) {
            "duplicate request image projections disagree on exact artifact identity"
        }
        requireStaticImageProjection(
            artifactRef = attachment.artifactRef,
            projectionRef = projectionRef,
        )
        val interpretation =
            VexDurableContractFile.read<VexLifeLocalArtifact.RequestVisualInterpretation>(
                ownedRoot = home,
                path =
                    VexOwnedPath.resolveInside(
                        home,
                        VexLifeLocalArtifact.requestVisualInterpretationRelativePath(
                            requestEventHash = requestEventHash,
                            projectionRef = projectionRef,
                        ),
                    ),
            ) ?: return null
        val bytes = interpretation.text.toByteArray(StandardCharsets.UTF_8)
        VexLifeVisualInputQualification.validate(
            VexLifeVisualInputQualification.QualifiedRuntimeEvidence(
                evidenceRef = interpretation.visualEvidenceRef,
                realizationRef = interpretation.visualRealizationRef,
                modelRef = interpretation.visualModelRef,
                artifactRef = interpretation.visualArtifactRef,
                artifactContentSetSha256 =
                    interpretation.visualArtifactContentSetSha256,
                environmentFingerprintSha256 =
                    interpretation.environmentFingerprintSha256,
                qualificationSourceRef =
                    interpretation.qualificationSourceRef,
                qualificationReturnSha256 =
                    interpretation.qualificationReturnSha256,
            ),
        )
        val expectedIdentity =
            visualInterpretationIdentitySha256(
                requestEventHash = interpretation.requestEventHash,
                artifactRef = interpretation.artifactRef,
                projectionRef = interpretation.projectionRef,
                visualEvidenceRef = interpretation.visualEvidenceRef,
                visualRealizationRef = interpretation.visualRealizationRef,
                visualModelRef = interpretation.visualModelRef,
                visualArtifactRef = interpretation.visualArtifactRef,
                visualArtifactContentSetSha256 =
                    interpretation.visualArtifactContentSetSha256,
                qualificationSourceRef =
                    interpretation.qualificationSourceRef,
                qualificationReturnSha256 =
                    interpretation.qualificationReturnSha256,
                environmentFingerprintSha256 =
                    interpretation.environmentFingerprintSha256,
                interpretationSha256 =
                    interpretation.interpretationSha256,
            )
        require(
            interpretation.requestEventRef == binding.requestEventRef &&
                interpretation.requestEventHash == binding.requestEventHash &&
                interpretation.requestContentHash == binding.requestContentHash &&
                interpretation.turnRef == binding.turnRef &&
                interpretation.requestMessageRef == binding.requestMessageRef &&
                interpretation.artifactRef == attachment.artifactRef &&
                interpretation.projectionRef == attachment.projectionRef &&
                interpretation.interpretationRef ==
                    VexLifeLocalArtifact.VisualInterpretationRefPrefix + expectedIdentity &&
                interpretation.interpretationSha256 == sha256(bytes) &&
                interpretation.byteCount == bytes.size.toLong() &&
                interpretation.lineCount == lineCount(interpretation.text)
        ) {
            "request visual interpretation identity/content mismatch"
        }
        return interpretation
    }

    /**
     * Bind already-staged artifacts to one exact durable human REQUEST.
     *
     * The request content is never rewritten. This sidecar is the only attachment manifest for the
     * request and is immutable once written.
     */
    fun bindRequest(
        request: DirectConversationStore.Event,
        attachments: List<VexLifeLocalArtifact.RequestAttachment>,
    ): VexLifeLocalArtifact.RequestBinding {
        require(
            request.eventKind ==
                VexLifeConversationContract.RequestKind,
        ) {
            "local artifacts may bind only to a canonical Conversation REQUEST"
        }
        require(
            attachments.size in
                1..VexLifeLocalArtifact.MaximumPerRequest,
        ) {
            "request artifact count is outside the admitted range"
        }

        attachments.forEach {
            attachment ->
            requireOriginal(
                attachment.artifactRef,
            )
            requireRequestProjection(
                artifactRef = attachment.artifactRef,
                projectionRef = attachment.projectionRef,
            )
        }

        val binding =
            VexLifeLocalArtifact.RequestBinding(
                requestEventRef = request.eventRef,
                requestEventHash = request.eventHash,
                requestContentHash = request.contentHash,
                turnRef = request.turnRef,
                requestMessageRef = request.messageRef,
                attachments = attachments.toList(),
            )

        writeImmutableContract(
            relativePath =
                VexLifeLocalArtifact.requestBindingRelativePath(
                    request.eventHash,
                ),
            value = binding,
        )

        return requireNotNull(
            bindingFor(
                request.eventHash,
            ),
        ) {
            "request artifact binding did not re-observe"
        }
    }

    fun bindingFor(
        requestEventHash: String,
    ): VexLifeLocalArtifact.RequestBinding? {
        val path =
            VexOwnedPath.resolveInside(
                home,
                VexLifeLocalArtifact.requestBindingRelativePath(
                    requestEventHash,
                ),
            )
        val binding =
            VexDurableContractFile.read<VexLifeLocalArtifact.RequestBinding>(
                ownedRoot = home,
                path = path,
            )
                ?: return null

        require(
            binding.requestEventHash ==
                requestEventHash,
        ) {
            "request artifact binding identity mismatch"
        }
        binding.attachments.forEach {
            attachment ->
            requireRequestProjection(
                artifactRef = attachment.artifactRef,
                projectionRef = attachment.projectionRef,
            )
        }
        return binding
    }

    private fun requireOriginal(
        artifactRef: String,
    ): VexLifeLocalArtifact.OriginalArtifact =
        requireNotNull(
            original(
                artifactRef,
            ),
        ) {
            "local artifact is not in exact Product custody: " +
                artifactRef
        }

    private fun requireTextProjection(
        artifactRef: String,
        projectionRef: String,
    ): VexLifeLocalArtifact.DerivedProjection =
        requireNotNull(
            projection(
                artifactRef = artifactRef,
                projectionRef = projectionRef,
            ),
        ) {
            "local artifact text projection is not in exact Product custody: " +
                projectionRef
        }

    private fun requireRequestProjection(
        artifactRef: String,
        projectionRef: String,
    ): VexLifeLocalArtifact.RequestProjection =
        requireNotNull(
            requestProjection(
                artifactRef = artifactRef,
                projectionRef = projectionRef,
            ),
        ) {
            "local artifact request projection is not in exact Product custody: " +
                projectionRef
        }

    private fun requireStaticImageProjection(
        artifactRef: String,
        projectionRef: String,
    ): VexLifeLocalArtifact.StaticImageProjection =
        requireNotNull(
            staticImageProjection(
                artifactRef = artifactRef,
                projectionRef = projectionRef,
            ),
        ) {
            "local artifact static image projection is not in exact Product custody: " +
                projectionRef
        }

    private fun requireExactBytes(
        artifact: VexLifeLocalArtifact.OriginalArtifact,
    ) {
        val path =
            VexOwnedPath.resolveInside(
                home,
                artifact.custodyRelativePath,
            )
        require(
            ExactArtifactFile.classify(
                path = path,
                expectedSha256 = artifact.sha256,
                expectedBytes = artifact.byteCount,
            ) is ExactArtifactFile.Classification.VerifiedReusable,
        ) {
            "local artifact raw-byte custody is not exact: " +
                artifact.artifactRef
        }
    }

    private inline fun <reified T> writeImmutableContract(
        relativePath: String,
        value: T,
    ) {
        val bytes =
            (
                VexContractJson.encode(
                    value,
                ) +
                    "\n"
            ).toByteArray(
                StandardCharsets.UTF_8,
            )

        writeImmutableBytes(
            relativePath = relativePath,
            bytes = bytes,
        )

        val observed =
            VexDurableContractFile.read<T>(
                ownedRoot = home,
                path =
                    VexOwnedPath.resolveInside(
                        home,
                        relativePath,
                    ),
            )
        require(
            observed ==
                value,
        ) {
            "immutable local artifact contract disagrees with the requested value: " +
                relativePath
        }
    }

    private fun writeImmutableBytes(
        relativePath: String,
        bytes: ByteArray,
    ) {
        require(bytes.isNotEmpty()) {
            "immutable local artifact bytes must not be empty"
        }

        val path =
            VexOwnedPath.resolveInside(
                home,
                relativePath,
            )
        VexOwnedPath.ensureParent(
            home,
            path,
        )

        val expectedSha256 =
            sha256(
                bytes,
            )

        if (
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            requireExactFile(
                path = path,
                sha256 = expectedSha256,
                byteCount = bytes.size.toLong(),
            )
            return
        }

        val temporary =
            path.resolveSibling(
                "." +
                    path.fileName +
                    ".partial-" +
                    UUID.randomUUID(),
            )

        try {
            Files.write(
                temporary,
                bytes,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
            VexDurableContractFile.secureFile(
                temporary,
            )
            try {
                Files.move(
                    temporary,
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (
                _: FileAlreadyExistsException,
            ) {
                // Another identical content-addressed writer may have won the race.
            } catch (
                _: AtomicMoveNotSupportedException,
            ) {
                error(
                    "atomic local artifact publication is unavailable for " +
                        path,
                )
            }
        } finally {
            Files.deleteIfExists(
                temporary,
            )
        }

        requireExactFile(
            path = path,
            sha256 = expectedSha256,
            byteCount = bytes.size.toLong(),
        )
        VexDurableContractFile.secureFile(
            path,
        )
    }

    private fun requireExactFile(
        path: Path,
        sha256: String,
        byteCount: Long,
    ) {
        require(
            ExactArtifactFile.classify(
                path = path,
                expectedSha256 = sha256,
                expectedBytes = byteCount,
            ) is ExactArtifactFile.Classification.VerifiedReusable,
        ) {
            "immutable local artifact path already contains different bytes: " +
                path
        }
    }

    private fun projectionIdentitySha256(
        projection: VexLifeLocalArtifact.DerivedProjection,
    ): String =
        sha256(
            (
                projection.originalArtifactRef +
                    "\n" +
                    projection.converterRef +
                    "\n" +
                    projection.projectionSha256
            ).toByteArray(
                StandardCharsets.UTF_8,
            ),
        )

    private fun requestProjectionIdentitySha256(
        projection: VexLifeLocalArtifact.RequestProjection,
    ): String =
        when (
            projection
        ) {
            is VexLifeLocalArtifact.DerivedProjection ->
                projectionIdentitySha256(
                    projection,
                )

            is VexLifeLocalArtifact.StaticImageProjection ->
                staticImageProjectionIdentitySha256(
                    artifactRef = projection.originalArtifactRef,
                    adapterRef = projection.adapterRef,
                    mediaType = projection.mediaType,
                    widthPixels = projection.widthPixels,
                    heightPixels = projection.heightPixels,
                )
        }

    private fun visualInterpretationIdentitySha256(
        requestEventHash: String,
        artifactRef: String,
        projectionRef: String,
        visualEvidenceRef: String,
        visualRealizationRef: String,
        visualModelRef: String,
        visualArtifactRef: String,
        visualArtifactContentSetSha256: String,
        qualificationSourceRef: String,
        qualificationReturnSha256: String,
        environmentFingerprintSha256: String,
        interpretationSha256: String,
    ): String =
        sha256(
            (
                requestEventHash +
                    "\n" +
                    artifactRef +
                    "\n" +
                    projectionRef +
                    "\n" +
                    visualEvidenceRef +
                    "\n" +
                    visualRealizationRef +
                    "\n" +
                    visualModelRef +
                    "\n" +
                    visualArtifactRef +
                    "\n" +
                    visualArtifactContentSetSha256 +
                    "\n" +
                    qualificationSourceRef +
                    "\n" +
                    qualificationReturnSha256 +
                    "\n" +
                    environmentFingerprintSha256 +
                    "\n" +
                    interpretationSha256
            ).toByteArray(
                StandardCharsets.UTF_8,
            ),
        )

    private fun staticImageProjectionIdentitySha256(
        artifactRef: String,
        adapterRef: String,
        mediaType: String,
        widthPixels: Int,
        heightPixels: Int,
    ): String =
        sha256(
            (
                artifactRef +
                    "\n" +
                    adapterRef +
                    "\n" +
                    mediaType +
                    "\n" +
                    widthPixels +
                    "x" +
                    heightPixels
            ).toByteArray(
                StandardCharsets.UTF_8,
            ),
        )

    private fun lineCount(
        text: String,
    ): Int =
        text
            .removeSuffix(
                "\n",
            )
            .count {
                it ==
                    '\n'
            } +
            1

    private fun sha256(
        bytes: ByteArray,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                bytes,
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
