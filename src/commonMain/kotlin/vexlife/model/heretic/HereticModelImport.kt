package vexlife.model.heretic

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Exact Home-owned description of one model the person added from an external provider.
 *
 * This contract is not a second runtime/catalog. It records the provider facts VexLife admitted so
 * the existing artifact, Models and runtime relationships can consume one normal
 * [HereticModelCatalog.Entry]. Product-shipped entries remain owned by [HereticModelCatalog].
 */
object HereticModelImport {
    const val Schema = "vexlife-prod.heretic-model-imports/v1"
    const val RepresentationQwen3OpenAiChat = "QWEN3_OPENAI_CHAT_V1"

    @Serializable
    data class Member(
        val roleRef: String,
        val artifactRef: String,
        val filename: String,
        val mediaType: String,
        val sha256: String,
        val expectedBytes: Long,
        val sourceRef: String,
        val licenseRef: String,
    ) {
        fun catalogMember(): HereticArtifactCatalog.Member =
            HereticArtifactCatalog.Member(
                roleRef = roleRef,
                artifactRef = artifactRef,
                filename = filename,
                mediaType = mediaType,
                sha256 = sha256,
                expectedBytes = expectedBytes,
                sourceRef = sourceRef,
                licenseRef = licenseRef,
            )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Record(
        val importRef: String,
        val sourceUrl: String,
        val providerRepository: String,
        val providerRevision: String,
        val handle: String,
        val humanLabel: String,
        val modelBundleRef: String,
        val generationRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val artifactSetRef: String,
        val artifactCustodyEvidenceRef: String,
        val artifactContentSetSha256: String,
        val modelRootRelativePath: String,
        val providerLicense: String,
        val representationRef: String,
        val companionProfileRef: String,
        /** Observed from the exact pinned provider chat template/tokenizer config at import time. */
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val nativeToolCalling: Boolean = false,
        val members: List<Member>,
    )

    @Serializable
    data class Value(
        val schemaVersion: String = Schema,
        val models: List<Record> = emptyList(),
    )

    fun empty(): Value = Value()

    fun validate(value: Value): Value {
        require(value.schemaVersion == Schema) {
            "Heretic model imports schema is not admitted"
        }
        require(value.models == value.models.sortedBy { it.modelRef }) {
            "Heretic model imports must be canonical"
        }
        require(value.models.map { it.importRef }.distinct().size == value.models.size) {
            "Heretic model import refs must be unique"
        }
        require(value.models.map { it.modelRef }.distinct().size == value.models.size) {
            "Heretic imported model refs must be unique"
        }
        require(value.models.map { it.handle }.distinct().size == value.models.size) {
            "Heretic imported model handles must be unique"
        }
        value.models.forEach(::validate)
        return value
    }

    fun validate(record: Record): Record {
        requirePortableRef(record.importRef, "importRef")
        require(
            record.sourceUrl == "https://huggingface.co/" + record.providerRepository &&
                record.providerRepository.matches(Regex("[A-Za-z0-9._-]+/[A-Za-z0-9._-]+"))
        ) {
            "imported model source must be one canonical public Hugging Face repository page"
        }
        require(record.providerRevision.matches(Regex("[0-9a-f]{40}"))) {
            "imported model provider revision must be one exact commit"
        }
        require(record.handle.matches(Regex("[a-z][a-z0-9-]*"))) {
            "imported model handle must be one short name"
        }
        require(record.humanLabel.isNotBlank()) {
            "imported model human label must be non-blank"
        }
        requirePortableRef(record.modelBundleRef, "modelBundleRef")
        requirePortableRef(record.generationRef, "generationRef")
        requirePortableRef(record.modelRef, "modelRef")
        requirePortableRef(record.modelProfileRef, "modelProfileRef")
        requirePortableRef(record.artifactSetRef, "artifactSetRef")
        requirePortableRef(record.artifactCustodyEvidenceRef, "artifactCustodyEvidenceRef")
        requireSha256(record.artifactContentSetSha256, "artifactContentSetSha256")
        require(record.modelRootRelativePath.matches(Regex("models/heretic/[a-z0-9.-]+"))) {
            "imported model root must be one folder under models/heretic"
        }
        require(record.providerLicense.isNotBlank()) {
            "imported model requires an observed provider license"
        }
        require(record.representationRef == RepresentationQwen3OpenAiChat) {
            "imported model representation is not admitted"
        }
        requirePortableRef(record.companionProfileRef, "companionProfileRef")
        require(
            record.members.isNotEmpty() &&
                record.members.map { it.filename }.distinct().size == record.members.size &&
                record.members.map { it.artifactRef }.distinct().size == record.members.size
        ) {
            "imported model members must be non-empty and unique"
        }
        record.members.forEach { member ->
            member.catalogMember()
            require(member.sourceRef.endsWith("." + record.providerRevision)) {
                "every imported model member must bind the exact provider revision"
            }
        }
        return record
    }

    fun entry(record: Record): HereticModelCatalog.Entry {
        val current = validate(record)
        return HereticModelCatalog.Entry(
            identity =
                HereticProductIdentity.Current.copy(
                    modelBundleRef = current.modelBundleRef,
                    generationRef = current.generationRef,
                    modelRef = current.modelRef,
                    modelProfileRef = current.modelProfileRef,
                    artifactSetRef = current.artifactSetRef,
                    artifactCustodyEvidenceRef = current.artifactCustodyEvidenceRef,
                    artifactContentSetSha256 = current.artifactContentSetSha256,
                    trainerSourceCommit = current.providerRevision,
                ),
            handle = current.handle,
            humanLabel = current.humanLabel,
            providerRepository = current.providerRepository,
            providerRevision = current.providerRevision,
            members = current.members.map(Member::catalogMember),
            modelRootRelativePath = current.modelRootRelativePath,
            recommendedMemoryBytes = null,
            companionProfile =
                HereticCompanionRepresentation.Profiles.importedQwen(
                    profileRef = current.companionProfileRef,
                    nativeToolCalling = current.nativeToolCalling,
                ),
            standing = HereticModelCatalog.Standing.CANDIDATE,
            origin = HereticModelCatalog.Origin.HOME_IMPORTED,
            providerLicense = current.providerLicense,
            autoRecommended = false,
        )
    }
}

// [VXG RealForever]
