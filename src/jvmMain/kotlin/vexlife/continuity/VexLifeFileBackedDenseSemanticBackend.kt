package vexlife.continuity

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath

/**
 * File-only persistent realization of the existing dense semantic Locator backend.
 *
 * The persisted snapshot is disposable acceleration only:
 * - it contains source coordinates/fingerprints and normalized vectors, never Conversation/Memory bodies;
 * - canonical Conversation remains the only source truth;
 * - a stale/corrupt/incompatible snapshot fails closed and is rebuildable;
 * - one atomic private file replaces the whole derived generation; no daemon or database is created.
 */
internal class VexLifeFileBackedDenseSemanticBackend(
    productHome: Path,
    private val encoder: VexLifeVolatileDenseSemanticBackend.Encoder,
    secretSource: () -> ByteArray = {
        SecureRandom().generateSeed(
            32,
        )
    },
) : VexLifeContinuitySourceLocator.SemanticIndexedBackend {
    companion object {
        const val SnapshotSchema =
            "vexlife-prod.continuity-file-derived-store/v1"

        const val SnapshotRelativePath =
            "runtime/vexlife-prod/continuity/locator-derived-v1.json"

        private const val FingerprintBindingPrefix =
            "fingerprint.file-dense-v1."
    }

    @Serializable
    private data class SnapshotRecord(
        val schemaVersion: String,
        val fingerprintSecretBase64: String,
        val fingerprintBindingRef: String,
        val semanticRepresentationBindingRef: String,
        val denseDimension: Int,
        val vectorPayloadBase64: String,
        val vectorPayloadSha256: String,
        val generation: GenerationRecord,
    )

    @Serializable
    private data class GenerationRecord(
        val schemaVersion: String,
        val sourceConversationHeadSha256: String?,
        val sourceConversationSequence: Long?,
        val fingerprintBindingRef: String,
        val sourceEventCount: Int,
        val completedTurnCount: Int,
        val canonicalCompletedTurnCount: Int,
        val sourceLifecycleBindingRef: String,
        val entries: List<EntryRecord>,
    )

    @Serializable
    private data class EntryRecord(
        val turnRef: String,
        val canonicalOrdinal: Int,
        val request: AddressRecord,
        val response: AddressRecord,
        val requestFingerprints: List<String>,
        val responseFingerprints: List<String>,
        val typedEdges: List<String>,
    )

    @Serializable
    private data class AddressRecord(
        val sequence: Long,
        val eventRef: String,
        val eventHash: String,
    )

    private data class Loaded(
        val secret: ByteArray,
        val fingerprintBindingRef: String,
        val snapshot: VexLifeVolatileDenseSemanticBackend.Snapshot?,
        val corruptionReason: String?,
    )

    private val home =
        VexOwnedPath.requireRoot(
            productHome,
            "continuity file-derived store Home",
        )

    private val snapshotPath =
        VexOwnedPath.resolveInside(
            home,
            SnapshotRelativePath,
        )

    private val loaded =
        loadSnapshot(
            encoder = encoder,
            secretSource = secretSource,
        )

    private val delegate =
        VexLifeVolatileDenseSemanticBackend(
            encoder = encoder,
            secret = loaded.secret,
            fingerprintBindingRef = loaded.fingerprintBindingRef,
        )

    private var corruptionReason: String? =
        loaded.corruptionReason

    override val fingerprintBindingRef: String
        get() =
            delegate.fingerprintBindingRef

    override val semanticRepresentationBindingRef: String
        get() =
            delegate.semanticRepresentationBindingRef

    init {
        loaded.snapshot
            ?.let {
                snapshot ->
                runCatching {
                    delegate.restore(
                        snapshot,
                    )
                }
                    .onFailure {
                        error ->
                        corruptionReason =
                            "continuity file-derived snapshot restore failed: " +
                                (
                                    error.message
                                        ?: error.javaClass.simpleName
                                )
                    }
            }
    }

    override fun fingerprint(
        value: String,
    ): String =
        delegate.fingerprint(
            value,
        )

    override fun load(): VexLifeContinuitySourceLocator.BackendLoad =
        corruptionReason
            ?.let {
                reason ->
                VexLifeContinuitySourceLocator.BackendLoad.Corrupt(
                    reason,
                )
            }
            ?: delegate.load()

    override fun replace(
        generation: VexLifeContinuitySourceLocator.Generation,
    ) {
        delegate.replace(
            generation,
        )
        persistCurrent()
    }

    override fun candidateEntryIndexes(
        queryFingerprints: List<Set<String>>,
        speakerScope: VexLifeContinuitySourceLocator.SpeakerScope,
    ): VexLifeContinuitySourceLocator.IndexedCandidateLookup =
        delegate.candidateEntryIndexes(
            queryFingerprints = queryFingerprints,
            speakerScope = speakerScope,
        )

    override fun entryIndexesForRefs(
        refs: Set<String>,
    ): VexLifeContinuitySourceLocator.IndexedRefLookup =
        delegate.entryIndexesForRefs(
            refs,
        )

    override fun entryIndexesForTypedEdges(
        typedEdges: Set<VexLifeContinuitySourceLocator.TypedEdgeKind>,
    ): VexLifeContinuitySourceLocator.IndexedTypedEdgeLookup =
        delegate.entryIndexesForTypedEdges(
            typedEdges,
        )

    override fun replaceAfterAppend(
        priorGeneration: VexLifeContinuitySourceLocator.Generation,
        generation: VexLifeContinuitySourceLocator.Generation,
        appendedEntry: VexLifeContinuitySourceLocator.Entry,
    ) {
        delegate.replaceAfterAppend(
            priorGeneration = priorGeneration,
            generation = generation,
            appendedEntry = appendedEntry,
        )
        persistCurrent()
    }

    override fun replaceWithSemanticSources(
        generation: VexLifeContinuitySourceLocator.Generation,
        sourceContents: List<VexLifeContinuitySourceLocator.SemanticSourceContent>,
    ) {
        delegate.replaceWithSemanticSources(
            generation = generation,
            sourceContents = sourceContents,
        )
        corruptionReason =
            null
        persistCurrent()
    }

    override fun replaceAfterAppendWithSemanticSource(
        priorGeneration: VexLifeContinuitySourceLocator.Generation,
        generation: VexLifeContinuitySourceLocator.Generation,
        appendedEntry: VexLifeContinuitySourceLocator.Entry,
        appendedSource: VexLifeContinuitySourceLocator.SemanticSourceContent,
    ) {
        delegate.replaceAfterAppendWithSemanticSource(
            priorGeneration = priorGeneration,
            generation = generation,
            appendedEntry = appendedEntry,
            appendedSource = appendedSource,
        )
        corruptionReason =
            null
        persistCurrent()
    }

    override fun semanticCandidateEntryIndexes(
        queryTerms: List<String>,
        speakerScope: VexLifeContinuitySourceLocator.SpeakerScope,
    ): VexLifeContinuitySourceLocator.SemanticCandidateLookup =
        delegate.semanticCandidateEntryIndexes(
            queryTerms = queryTerms,
            speakerScope = speakerScope,
        )

    override fun clear() {
        delegate.clear()
        corruptionReason =
            null
        VexDurableContractFile.delete(
            ownedRoot = home,
            path = snapshotPath,
        )
    }

    internal fun persistentSnapshotPath(): Path =
        snapshotPath

    private fun persistCurrent() {
        val snapshot =
            delegate.snapshotOrNull()
                ?: run {
                    VexDurableContractFile.delete(
                        ownedRoot = home,
                        path = snapshotPath,
                    )
                    return
                }
        val payload =
            encodeVectors(
                snapshot.vectors,
            )
        val record =
            SnapshotRecord(
                schemaVersion = SnapshotSchema,
                fingerprintSecretBase64 =
                    Base64
                        .getEncoder()
                        .encodeToString(
                            loaded.secret,
                        ),
                fingerprintBindingRef =
                    fingerprintBindingRef,
                semanticRepresentationBindingRef =
                    semanticRepresentationBindingRef,
                denseDimension =
                    encoder.dimension,
                vectorPayloadBase64 =
                    Base64
                        .getEncoder()
                        .encodeToString(
                            payload,
                        ),
                vectorPayloadSha256 =
                    sha256(
                        payload,
                    ),
                generation =
                    snapshot.generation
                        .toRecord(),
            )
        VexDurableContractFile.write(
            ownedRoot = home,
            path = snapshotPath,
            value = record,
        )
    }

    private fun loadSnapshot(
        encoder: VexLifeVolatileDenseSemanticBackend.Encoder,
        secretSource: () -> ByteArray,
    ): Loaded {
        val record =
            try {
                VexDurableContractFile.read<SnapshotRecord>(
                    ownedRoot = home,
                    path = snapshotPath,
                )
            } catch (
                error: Throwable,
            ) {
                return freshCorrupt(
                    secretSource = secretSource,
                    reason =
                        "continuity file-derived snapshot is unreadable: " +
                            error.javaClass.simpleName,
                )
            }

        if (
            record ==
                null
        ) {
            return fresh(
                secretSource,
            )
        }

        return try {
            require(
                record.schemaVersion ==
                    SnapshotSchema
            ) {
                "snapshot schema is unsupported"
            }
            val secret =
                Base64
                    .getDecoder()
                    .decode(
                        record.fingerprintSecretBase64,
                    )
            require(
                secret.size >=
                    32
            ) {
                "snapshot fingerprint secret is too short"
            }
            val expectedFingerprintBinding =
                fingerprintBindingRef(
                    secret,
                )
            require(
                record.fingerprintBindingRef ==
                    expectedFingerprintBinding &&
                    record.generation.fingerprintBindingRef ==
                    expectedFingerprintBinding
            ) {
                "snapshot fingerprint binding is inconsistent"
            }

            val delegate =
                VexLifeVolatileDenseSemanticBackend(
                    encoder = encoder,
                    secret = secret,
                    fingerprintBindingRef =
                        expectedFingerprintBinding,
                )
            require(
                record.semanticRepresentationBindingRef ==
                    delegate.semanticRepresentationBindingRef
            ) {
                "snapshot semantic representation binding changed"
            }
            require(
                record.denseDimension ==
                    encoder.dimension &&
                    record.denseDimension >
                    0
            ) {
                "snapshot dense dimension changed"
            }

            val payload =
                Base64
                    .getDecoder()
                    .decode(
                        record.vectorPayloadBase64,
                    )
            require(
                sha256(
                    payload,
                ) ==
                    record.vectorPayloadSha256
            ) {
                "snapshot vector payload digest changed"
            }
            val generation =
                record.generation
                    .toGeneration()
            val vectors =
                decodeVectors(
                    payload = payload,
                    dimension = record.denseDimension,
                    vectorCount =
                        generation.entries.size *
                            2,
                )

            Loaded(
                secret = secret,
                fingerprintBindingRef =
                    expectedFingerprintBinding,
                snapshot =
                    VexLifeVolatileDenseSemanticBackend.Snapshot(
                        generation = generation,
                        vectors = vectors,
                    ),
                corruptionReason = null,
            )
        } catch (
            error: Throwable,
        ) {
            freshCorrupt(
                secretSource = secretSource,
                reason =
                    "continuity file-derived snapshot is incompatible: " +
                        (
                            error.message
                                ?: error.javaClass.simpleName
                        ),
            )
        }
    }

    private fun fresh(
        secretSource: () -> ByteArray,
    ): Loaded {
        val secret =
            secretSource()
                .copyOf()
        require(
            secret.size >=
                32
        ) {
            "continuity file-derived fingerprint secret must contain at least 32 bytes"
        }
        return Loaded(
            secret = secret,
            fingerprintBindingRef =
                fingerprintBindingRef(
                    secret,
                ),
            snapshot = null,
            corruptionReason = null,
        )
    }

    private fun freshCorrupt(
        secretSource: () -> ByteArray,
        reason: String,
    ): Loaded =
        fresh(
            secretSource,
        )
            .copy(
                corruptionReason = reason,
            )

    private fun fingerprintBindingRef(
        secret: ByteArray,
    ): String =
        FingerprintBindingPrefix +
            sha256(
                secret,
            )
                .take(
                    24,
                )

    private fun encodeVectors(
        vectors: List<FloatArray>,
    ): ByteArray {
        if (
            vectors.isEmpty()
        ) {
            return ByteArray(
                0,
            )
        }
        val dimension =
            vectors.first().size
        require(
            dimension >
                0 &&
                vectors.all {
                    it.size ==
                        dimension
                }
        ) {
            "persistent dense vectors must have one exact non-zero dimension"
        }
        val bytes =
            ByteBuffer
                .allocate(
                    vectors.size *
                        dimension *
                        Float.SIZE_BYTES,
                )
                .order(
                    ByteOrder.LITTLE_ENDIAN,
                )
        vectors.forEach {
            vector ->
            vector.forEach {
                value ->
                bytes.putFloat(
                    value,
                )
            }
        }
        return bytes.array()
    }

    private fun decodeVectors(
        payload: ByteArray,
        dimension: Int,
        vectorCount: Int,
    ): List<FloatArray> {
        val expectedBytes =
            vectorCount.toLong() *
                dimension.toLong() *
                Float.SIZE_BYTES.toLong()
        require(
            expectedBytes <=
                Int.MAX_VALUE &&
                payload.size.toLong() ==
                expectedBytes
        ) {
            "persistent dense vector payload size is inconsistent"
        }
        val bytes =
            ByteBuffer
                .wrap(
                    payload,
                )
                .order(
                    ByteOrder.LITTLE_ENDIAN,
                )
        return List(
            vectorCount,
        ) {
            FloatArray(
                dimension,
            ) {
                bytes.getFloat()
            }
        }
    }

    private fun GenerationRecord.toGeneration():
        VexLifeContinuitySourceLocator.Generation =
        VexLifeContinuitySourceLocator.Generation(
            schemaVersion = schemaVersion,
            sourceConversationHeadSha256 =
                sourceConversationHeadSha256,
            sourceConversationSequence =
                sourceConversationSequence,
            fingerprintBindingRef =
                fingerprintBindingRef,
            sourceEventCount =
                sourceEventCount,
            completedTurnCount =
                completedTurnCount,
            canonicalCompletedTurnCount =
                canonicalCompletedTurnCount,
            entries =
                entries.map {
                    it.toEntry()
                },
            sourceLifecycleBindingRef =
                sourceLifecycleBindingRef,
        )

    private fun EntryRecord.toEntry():
        VexLifeContinuitySourceLocator.Entry =
        VexLifeContinuitySourceLocator.Entry(
            turnRef = turnRef,
            canonicalOrdinal =
                canonicalOrdinal,
            coordinates =
                VexLifeContinuitySourceLocator.SourceCoordinates(
                    request =
                        request.toAddress(),
                    response =
                        response.toAddress(),
                ),
            requestFingerprints =
                requestFingerprints
                    .toSet(),
            responseFingerprints =
                responseFingerprints
                    .toSet(),
            typedEdges =
                typedEdges
                    .map(
                        VexLifeContinuitySourceLocator.TypedEdgeKind::valueOf,
                    )
                    .toSet(),
        )

    private fun AddressRecord.toAddress():
        DirectConversationStore.EventAddress =
        DirectConversationStore.EventAddress(
            sequence = sequence,
            eventRef = eventRef,
            eventHash = eventHash,
        )

    private fun VexLifeContinuitySourceLocator.Generation.toRecord():
        GenerationRecord =
        GenerationRecord(
            schemaVersion = schemaVersion,
            sourceConversationHeadSha256 =
                sourceConversationHeadSha256,
            sourceConversationSequence =
                sourceConversationSequence,
            fingerprintBindingRef =
                fingerprintBindingRef,
            sourceEventCount =
                sourceEventCount,
            completedTurnCount =
                completedTurnCount,
            canonicalCompletedTurnCount =
                canonicalCompletedTurnCount,
            sourceLifecycleBindingRef =
                sourceLifecycleBindingRef,
            entries =
                entries.map {
                    it.toRecord()
                },
        )

    private fun VexLifeContinuitySourceLocator.Entry.toRecord():
        EntryRecord =
        EntryRecord(
            turnRef = turnRef,
            canonicalOrdinal =
                canonicalOrdinal,
            request =
                coordinates.request
                    .toRecord(),
            response =
                coordinates.response
                    .toRecord(),
            requestFingerprints =
                requestFingerprints
                    .sorted(),
            responseFingerprints =
                responseFingerprints
                    .sorted(),
            typedEdges =
                typedEdges
                    .map {
                        it.name
                    }
                    .sorted(),
        )

    private fun DirectConversationStore.EventAddress.toRecord():
        AddressRecord =
        AddressRecord(
            sequence = sequence,
            eventRef = eventRef,
            eventHash = eventHash,
        )

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
