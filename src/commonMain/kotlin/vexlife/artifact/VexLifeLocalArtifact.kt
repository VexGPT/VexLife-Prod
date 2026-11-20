package vexlife.artifact

import kotlinx.serialization.Serializable

/**
 * Semantic identities for local material the person gave VexLife.
 *
 * Raw artifact identity is the exact immutable byte sequence. Filename, media classification,
 * converter output and Conversation attachment meaning are deliberately separate.
 *
 * ```text
 * OriginalArtifact
 *   exact bytes + full SHA-256
 *
 * DerivedProjection
 *   OriginalArtifact + exact converter relationship + bounded derived text
 *
 * RequestBinding
 *   one already-durable canonical Conversation REQUEST + per-message display names + projections
 * ```
 *
 * Permanent:
 *
 * ```text
 * ORIGINAL BYTES != DERIVED TEXT
 * DERIVED TEXT != MODEL INTERPRETATION
 * STAGED != SENT
 * SENT != READ
 * EXACT HUMAN WORDS != PRODUCT RESOURCE MANIFEST
 * ```
 */
object VexLifeLocalArtifact {
    const val OriginalSchema = "vexlife-prod.local-artifact/v1"
    const val ProjectionSchema = "vexlife-prod.local-artifact-projection/v1"
    const val RequestBindingSchema = "vexlife-prod.request-artifacts/v1"

    const val ArtifactRefPrefix = "artifact.vexlife-prod.local.sha256."
    const val ProjectionRefPrefix = "projection.vexlife-prod.local.sha256."

    /** Bound on exact bytes the person gives this Product-local artifact owner. */
    const val MaximumOriginalBytes = 16 * 1024 * 1024

    /** Bound on one derived readable-text projection. */
    const val MaximumProjectionBytes = 8 * 1024 * 1024

    const val MaximumPerRequest = 8
    const val MaximumDisplayNameCharacters = 160

    private val Sha256 = Regex("[0-9a-f]{64}")
    private val StableRef = Regex("[A-Za-z0-9][A-Za-z0-9._:/-]*")

    @Serializable
    data class OriginalArtifact(
        val schemaVersion: String = OriginalSchema,
        val artifactRef: String,
        val sha256: String,
        val byteCount: Long,
        val custodyRelativePath: String,
    ) {
        init {
            require(schemaVersion == OriginalSchema) {
                "local artifact schema is not admitted"
            }
            require(Sha256.matches(sha256)) {
                "local artifact requires one full lowercase SHA-256"
            }
            require(artifactRef == refFor(sha256)) {
                "local artifact ref must preserve the full byte digest"
            }
            require(byteCount > 0) {
                "local artifact byte count must be positive"
            }
            require(custodyRelativePath == artifactBytesRelativePath(sha256)) {
                "local artifact custody path must derive from its full byte digest"
            }
        }
    }

    @Serializable
    data class DerivedProjection(
        val schemaVersion: String = ProjectionSchema,
        val projectionRef: String,
        val originalArtifactRef: String,
        val converterRef: String,
        val projectionSha256: String,
        val byteCount: Long,
        val lineCount: Int,
        val text: String,
    ) {
        init {
            require(schemaVersion == ProjectionSchema) {
                "local artifact projection schema is not admitted"
            }
            require(isProjectionRef(projectionRef)) {
                "local artifact projection ref is invalid"
            }
            require(isArtifactRef(originalArtifactRef)) {
                "local artifact projection requires one exact original artifact"
            }
            require(StableRef.matches(converterRef)) {
                "local artifact projection converterRef must be one stable ref"
            }
            require(Sha256.matches(projectionSha256)) {
                "local artifact projection requires one full lowercase SHA-256"
            }
            require(
                byteCount in 1..MaximumProjectionBytes.toLong() &&
                    lineCount > 0 &&
                    text.isNotEmpty(),
            ) {
                "local artifact projection must contain non-empty bounded text"
            }
        }
    }

    @Serializable
    data class RequestAttachment(
        val artifactRef: String,
        val projectionRef: String,
        val displayName: String,
    ) {
        init {
            require(isArtifactRef(artifactRef)) {
                "request attachment artifactRef is invalid"
            }
            require(isProjectionRef(projectionRef)) {
                "request attachment projectionRef is invalid"
            }
            require(admitDisplayName(displayName) == displayName) {
                "request attachment display name is not admitted"
            }
        }
    }

    @Serializable
    data class RequestBinding(
        val schemaVersion: String = RequestBindingSchema,
        val requestEventRef: String,
        val requestEventHash: String,
        val requestContentHash: String,
        val turnRef: String,
        val requestMessageRef: String,
        val attachments: List<RequestAttachment>,
    ) {
        init {
            require(schemaVersion == RequestBindingSchema) {
                "request artifact binding schema is not admitted"
            }
            require(StableRef.matches(requestEventRef)) {
                "request artifact binding event ref is invalid"
            }
            require(Sha256.matches(requestEventHash) && Sha256.matches(requestContentHash)) {
                "request artifact binding requires exact event/content hashes"
            }
            require(StableRef.matches(turnRef) && StableRef.matches(requestMessageRef)) {
                "request artifact binding requires exact turn/message refs"
            }
            require(attachments.size in 1..MaximumPerRequest) {
                "request artifact binding count is outside the admitted range"
            }
        }
    }

    fun refFor(sha256: String): String {
        require(Sha256.matches(sha256)) {
            "artifact SHA-256 must be full lowercase hex"
        }
        return ArtifactRefPrefix + sha256
    }

    fun sha256FromArtifactRef(artifactRef: String): String? =
        artifactRef
            .takeIf(::isArtifactRef)
            ?.removePrefix(ArtifactRefPrefix)

    fun projectionIdentitySha256FromRef(projectionRef: String): String? =
        projectionRef
            .takeIf(::isProjectionRef)
            ?.removePrefix(ProjectionRefPrefix)

    fun isArtifactRef(value: String): Boolean =
        value.startsWith(ArtifactRefPrefix) &&
            Sha256.matches(value.removePrefix(ArtifactRefPrefix))

    fun isProjectionRef(value: String): Boolean =
        value.startsWith(ProjectionRefPrefix) &&
            Sha256.matches(value.removePrefix(ProjectionRefPrefix))

    fun artifactBytesRelativePath(sha256: String): String {
        require(Sha256.matches(sha256)) {
            "artifact SHA-256 must be full lowercase hex"
        }
        return "attachments/artifacts/$sha256.bin"
    }

    fun artifactMetadataRelativePath(sha256: String): String {
        require(Sha256.matches(sha256)) {
            "artifact SHA-256 must be full lowercase hex"
        }
        return "attachments/artifacts/$sha256.json"
    }

    fun projectionRelativePath(
        artifactSha256: String,
        projectionIdentitySha256: String,
    ): String {
        require(Sha256.matches(artifactSha256) && Sha256.matches(projectionIdentitySha256)) {
            "projection path requires full lowercase SHA-256 identities"
        }
        return "attachments/projections/$artifactSha256/$projectionIdentitySha256.json"
    }

    fun requestBindingRelativePath(requestEventHash: String): String {
        require(Sha256.matches(requestEventHash)) {
            "request binding requires one full event SHA-256"
        }
        return "runtime/vexlife-prod/request-resources/$requestEventHash.json"
    }

    fun admitDisplayName(raw: String): String? {
        val lastSegment =
            raw
                .substringAfterLast('/')
                .substringAfterLast('\\')
        val name =
            lastSegment
                .filterNot {
                    it.isISOControl() ||
                        it == '·'
                }
                .trim()
                .take(
                    MaximumDisplayNameCharacters,
                )
                .trim()
        return name.takeIf {
            it.isNotEmpty() &&
                it != "." &&
                it != ".."
        }
    }
}

// [VXG RealForever]
