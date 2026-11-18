package vexlife.model.heretic

import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import vexlife.model.heretic.runtime.HereticArtifactMaterialization

/**
 * No-effect inspection of one public Hugging Face model page.
 *
 * The inspector recognizes only the smallest provider relationship the Product can presently
 * justify dynamically: root-level Qwen3 causal-text MLX/Safetensors repositories that can run
 * through the existing mlx-lm/OpenAI-chat runtime. It pins one exact provider commit and every
 * runtime member by byte length + SHA-256 before returning an import candidate.
 *
 * Large model files are never downloaded by Check. Their exact hash/size must be published by the
 * provider's fixed-revision HEAD metadata. Small Git-backed runtime files may be read and hashed.
 */
object HuggingFaceModelInspection {
    private const val ApiBodyLimit = 2_000_000L
    private const val SmallFileBodyLimit = 16L * 1024L * 1024L
    private const val RequestTimeoutMs = 60_000L
    private val Sha256Pattern = Regex("[0-9a-f]{64}")
    private val GitShaPattern = Regex("[0-9a-f]{40}")
    private val SafeProviderPart = Regex("[A-Za-z0-9._-]+")
    private val JsonCodec = Json { ignoreUnknownKeys = true; isLenient = false }

    enum class FailureCode {
        SOURCE_NOT_HUGGING_FACE_MODEL,
        SOURCE_UNAVAILABLE,
        SOURCE_NOT_PUBLIC,
        SOURCE_NOT_PINNABLE,
        MODEL_NOT_MLX_SAFETENSORS,
        MODEL_RELATIONSHIP_NOT_RECOGNIZED,
        MODEL_REMOTE_CODE_REQUIRED,
        MODEL_MULTIMODAL_NOT_ADMITTED,
        MODEL_LICENSE_UNAVAILABLE,
        MODEL_RUNTIME_FILES_INCOMPLETE,
        MODEL_MEMBER_IDENTITY_UNAVAILABLE,
    }

    class InspectionException(
        val code: FailureCode,
        message: String,
        cause: Throwable? = null,
    ) : IllegalArgumentException(message, cause)

    data class HttpResult(
        val status: Int,
        val headers: Map<String, List<String>>,
        val body: ByteArray = byteArrayOf(),
    ) {
        fun header(name: String): String? =
            headers.entries
                .firstOrNull { it.key.equals(name, ignoreCase = true) }
                ?.value
                ?.firstOrNull()
    }

    interface Transport {
        fun get(
            url: String,
            maxBytes: Long,
        ): HttpResult

        fun head(url: String): HttpResult
    }

    object JdkTransport : Transport {
        private val followed =
            HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build()
        private val direct =
            HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(20))
                .build()

        override fun get(
            url: String,
            maxBytes: Long,
        ): HttpResult {
            val request =
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(RequestTimeoutMs))
                    .header("accept", "application/json, text/plain, */*")
                    .header("user-agent", "VexLife-Prod/model-import")
                    .GET()
                    .build()
            val response =
                followed.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream(),
                )
            val output = ByteArrayOutputStream()
            response.body().use { input ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) {
                        throw InspectionException(
                            FailureCode.MODEL_MEMBER_IDENTITY_UNAVAILABLE,
                            "provider response exceeded the bounded inspection size",
                        )
                    }
                    output.write(buffer, 0, read)
                }
            }
            return HttpResult(
                status = response.statusCode(),
                headers = response.headers().map(),
                body = output.toByteArray(),
            )
        }

        override fun head(url: String): HttpResult {
            val request =
                HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(RequestTimeoutMs))
                    .header("user-agent", "VexLife-Prod/model-import")
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build()
            val response =
                direct.send(
                    request,
                    HttpResponse.BodyHandlers.discarding(),
                )
            return HttpResult(
                status = response.statusCode(),
                headers = response.headers().map(),
            )
        }
    }

    @Serializable
    data class Candidate(
        val importRef: String,
        val sourceUrl: String,
        val providerRepository: String,
        val providerRevision: String,
        val humanLabel: String,
        val providerLicense: String,
        val totalBytes: Long,
        val memberCount: Int,
        val relationship: String,
        val testedByVexLife: Boolean = false,
        val memoryExpectationEstablished: Boolean = false,
        val record: HereticModelImport.Record,
    )

    private data class Repository(
        val canonicalUrl: String,
        val repository: String,
    )

    private data class FileIdentity(
        val filename: String,
        val bytes: Long,
        val sha256: String,
        val mediaType: String,
    )

    fun inspect(
        sourceUrl: String,
        transport: Transport = JdkTransport,
    ): Candidate {
        val source = repository(sourceUrl)
        val api =
            getRequired(
                transport = transport,
                url = "https://huggingface.co/api/models/" + source.repository + "?blobs=true",
                maxBytes = ApiBodyLimit,
                code = FailureCode.SOURCE_UNAVAILABLE,
                context = "Hugging Face model metadata",
            )
        val metadata =
            runCatching {
                JsonCodec.parseToJsonElement(
                    api.body.toString(StandardCharsets.UTF_8),
                ).jsonObject
            }
                .getOrElse { error ->
                    throw InspectionException(
                        FailureCode.SOURCE_UNAVAILABLE,
                        "Hugging Face returned unreadable model metadata",
                        error,
                    )
                }

        requirePublic(metadata)
        val revision =
            metadata.string("sha")
                ?.lowercase()
                ?.takeIf { GitShaPattern.matches(it) }
                ?: throw InspectionException(
                    FailureCode.SOURCE_NOT_PINNABLE,
                    "Hugging Face did not expose one exact model revision",
                )
        val tags =
            (metadata["tags"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .orEmpty()
                .map(String::lowercase)
                .toSet()
        require(
            "mlx" in tags &&
                "safetensors" in tags
        ) {
            throw InspectionException(
                FailureCode.MODEL_NOT_MLX_SAFETENSORS,
                "VexLife currently imports only MLX models published with Safetensors",
            )
        }
        val license =
            metadata
                .obj("cardData")
                ?.string("license")
                ?.takeIf(String::isNotBlank)
                ?: tags
                    .firstOrNull { it.startsWith("license:") }
                    ?.substringAfter("license:")
                ?: throw InspectionException(
                    FailureCode.MODEL_LICENSE_UNAVAILABLE,
                    "VexLife could not establish the model's provider-published license",
                )

        val siblingNames =
            (metadata["siblings"] as? JsonArray)
                ?.mapNotNull { element ->
                    (element as? JsonObject)
                        ?.string("rfilename")
                }
                .orEmpty()
                .filter(::safeRuntimeMember)
                .distinct()
                .sortedWith(::compareUtf8)

        requireRuntimeClosure(siblingNames)

        val configBytes =
            exactSmallFile(
                transport = transport,
                repository = source.repository,
                revision = revision,
                filename = "config.json",
            )
        val config =
            runCatching {
                JsonCodec.parseToJsonElement(
                    configBytes.toString(StandardCharsets.UTF_8),
                ).jsonObject
            }
                .getOrElse { error ->
                    throw InspectionException(
                        FailureCode.MODEL_RELATIONSHIP_NOT_RECOGNIZED,
                        "model config.json is not readable JSON",
                        error,
                    )
                }
        requireQwen3Relationship(config)

        val identities =
            siblingNames.map { filename ->
                fileIdentity(
                    transport = transport,
                    repository = source.repository,
                    revision = revision,
                    filename = filename,
                    alreadyRead =
                        configBytes.takeIf {
                            filename == "config.json"
                        },
                )
            }

        val sourceRef =
            "hf." +
                source.repository
                    .lowercase()
                    .replace(Regex("[^a-z0-9]+"), ".")
                    .trim('.') +
                "." +
                revision
        val safeLicense =
            license
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .takeIf(String::isNotBlank)
                ?: "provider-declared"
        val licenseRef = "license.huggingface." + safeLicense

        val preliminaryMembers =
            identities.map { identity ->
                val role =
                    identity.filename
                        .lowercase()
                        .replace(Regex("[^a-z0-9]+"), "-")
                        .trim('-')
                HereticModelImport.Member(
                    roleRef = "artifact-role.vexlife-prod.imported.member." + role,
                    artifactRef =
                        "artifact.vexlife-prod.imported." +
                            revision.take(12) +
                            ".member." +
                            role,
                    filename = identity.filename,
                    mediaType = identity.mediaType,
                    sha256 = identity.sha256,
                    expectedBytes = identity.bytes,
                    sourceRef = sourceRef,
                    licenseRef = licenseRef,
                )
            }

        val planMembers =
            preliminaryMembers.map { member ->
                HereticArtifactMaterialization.PlanMember(
                    roleRef = member.roleRef,
                    artifactRef = member.artifactRef,
                    logicalPath = member.filename,
                    sha256 = member.sha256,
                    byteLength = member.expectedBytes,
                )
            }
        val contentSet =
            HereticArtifactMaterialization.computeContentSetSha256(
                planMembers,
            )
        val providerName = source.repository.substringAfterLast('/')
        val handleBase =
            providerName
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .let { value ->
                    if (value.firstOrNull()?.isLetter() == true) value else "model-" + value
                }
        val handle =
            "hf-" +
                handleBase.take(44).trimEnd('-') +
                "-" +
                contentSet.take(8)
        val identityStem = handle + "." + contentSet.take(12)
        val profileRef =
            "companion-profile.vexlife-prod.imported." + handle + ".qwen3.v1"
        val record =
            HereticModelImport.Record(
                importRef = "model-import.vexlife-prod.huggingface." + contentSet.take(24),
                sourceUrl = source.canonicalUrl,
                providerRepository = source.repository,
                providerRevision = revision,
                handle = handle,
                humanLabel = providerName,
                modelBundleRef = "model-bundle.vexlife.imported." + identityStem + ".001",
                generationRef = "generation.vexlife-prod.imported-provider." + revision.take(12),
                modelRef = "model.vexlife-prod.imported." + identityStem,
                modelProfileRef = "model-profile.vexlife-prod.imported." + identityStem + ".candidate",
                artifactSetRef = "artifact.vexlife-prod.imported." + identityStem,
                artifactCustodyEvidenceRef =
                    "resolution.vexlife-prod.model-import.provider-pin." +
                        revision.take(12) +
                        "." +
                        contentSet.take(12),
                artifactContentSetSha256 = contentSet,
                modelRootRelativePath = "models/heretic/imported." + handle,
                providerLicense = license,
                representationRef = HereticModelImport.RepresentationQwen3OpenAiChat,
                companionProfileRef = profileRef,
                members = preliminaryMembers,
            )
                .let(HereticModelImport::validate)
        // Re-form the Entry + plan now, before Check claims the candidate is admissible.
        HereticArtifactMaterialization.plan(
            HereticModelImport.entry(record),
        )

        return Candidate(
            importRef = record.importRef,
            sourceUrl = source.canonicalUrl,
            providerRepository = source.repository,
            providerRevision = revision,
            humanLabel = record.humanLabel,
            providerLicense = license,
            totalBytes = identities.sumOf(FileIdentity::bytes),
            memberCount = identities.size,
            relationship = record.representationRef,
            record = record,
        )
    }

    private fun repository(sourceUrl: String): Repository {
        val uri =
            runCatching {
                URI.create(
                    sourceUrl.trim(),
                )
            }
                .getOrNull()
                ?: throw InspectionException(
                    FailureCode.SOURCE_NOT_HUGGING_FACE_MODEL,
                    "model link is not a valid URL",
                )
        if (
            uri.scheme != "https" ||
                !uri.host.equals("huggingface.co", ignoreCase = true) ||
                uri.userInfo != null ||
                uri.port != -1
        ) {
            throw InspectionException(
                FailureCode.SOURCE_NOT_HUGGING_FACE_MODEL,
                "VexLife currently checks public huggingface.co model pages",
            )
        }
        val parts =
            uri.path
                .split('/')
                .filter(String::isNotBlank)
        if (
            parts.size < 2 ||
                parts[0] in setOf("datasets", "spaces", "collections") ||
                !SafeProviderPart.matches(parts[0]) ||
                !SafeProviderPart.matches(parts[1])
        ) {
            throw InspectionException(
                FailureCode.SOURCE_NOT_HUGGING_FACE_MODEL,
                "Hugging Face link does not name one model repository",
            )
        }
        val repository = parts[0] + "/" + parts[1]
        return Repository(
            canonicalUrl = "https://huggingface.co/" + repository,
            repository = repository,
        )
    }

    private fun requirePublic(metadata: JsonObject) {
        val privateModel = metadata.boolean("private") == true
        val disabled = metadata.boolean("disabled") == true
        val gated =
            metadata["gated"]
                ?.let { value ->
                    when (value) {
                        is JsonPrimitive ->
                            value.booleanOrNull == true ||
                                value.contentOrNull?.let {
                                    it != "false" && it != "null"
                                } == true
                        else -> true
                    }
                }
                ?: false
        if (privateModel || gated || disabled) {
            throw InspectionException(
                FailureCode.SOURCE_NOT_PUBLIC,
                "VexLife currently imports only public, ungated model repositories",
            )
        }
    }

    private fun requireQwen3Relationship(config: JsonObject) {
        if (config["auto_map"] != null) {
            throw InspectionException(
                FailureCode.MODEL_REMOTE_CODE_REQUIRED,
                "model requires provider-supplied runtime code, which VexLife does not execute",
            )
        }
        if (
            config["vision_config"] != null ||
                config["visual"] != null
        ) {
            throw InspectionException(
                FailureCode.MODEL_MULTIMODAL_NOT_ADMITTED,
                "multimodal model execution is not part of this import relationship yet",
            )
        }
        val architecture =
            (config["architectures"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .orEmpty()
        if (
            config.string("model_type") != "qwen3" ||
                "Qwen3ForCausalLM" !in architecture
        ) {
            throw InspectionException(
                FailureCode.MODEL_RELATIONSHIP_NOT_RECOGNIZED,
                "VexLife does not yet know how to map this model's architecture to its local runtime",
            )
        }
    }

    private fun safeRuntimeMember(filename: String): Boolean {
        if (
            filename.isBlank() ||
                filename.contains('/') ||
                filename.contains('\\')
        ) {
            return false
        }
        return filename in
            setOf(
                "config.json",
                "generation_config.json",
                "tokenizer.json",
                "tokenizer_config.json",
                "added_tokens.json",
                "special_tokens_map.json",
                "vocab.json",
                "merges.txt",
                "chat_template.jinja",
                "model.safetensors",
                "model.safetensors.index.json",
            ) ||
            filename.matches(
                Regex("model-[0-9]{5}-of-[0-9]{5}\\.safetensors"),
            )
    }

    private fun requireRuntimeClosure(files: List<String>) {
        val weights =
            files.filter {
                it == "model.safetensors" ||
                    it.matches(
                        Regex("model-[0-9]{5}-of-[0-9]{5}\\.safetensors"),
                    )
            }
        if (
            "config.json" !in files ||
                "tokenizer.json" !in files ||
                "tokenizer_config.json" !in files ||
                weights.isEmpty()
        ) {
            throw InspectionException(
                FailureCode.MODEL_RUNTIME_FILES_INCOMPLETE,
                "model does not expose the Qwen3 runtime files VexLife requires",
            )
        }
    }

    private fun fileIdentity(
        transport: Transport,
        repository: String,
        revision: String,
        filename: String,
        alreadyRead: ByteArray?,
    ): FileIdentity {
        val url = resolveUrl(repository, revision, filename)
        val head =
            runCatching {
                transport.head(url)
            }
                .getOrElse { error ->
                    throw InspectionException(
                        FailureCode.SOURCE_UNAVAILABLE,
                        "could not inspect " + filename,
                        error,
                    )
                }
        if (head.status !in 200..399) {
            throw InspectionException(
                FailureCode.SOURCE_UNAVAILABLE,
                "provider did not expose " + filename + " at the exact revision",
            )
        }
        val linkedSize =
            head.header("x-linked-size")
                ?.toLongOrNull()
        val contentSize =
            head.header("content-length")
                ?.toLongOrNull()
        val publishedSize =
            linkedSize
                ?: contentSize
        val publishedSha =
            sequenceOf(
                head.header("x-linked-etag"),
                head.header("etag"),
            )
                .mapNotNull(::normalizedEtag)
                .firstOrNull(Sha256Pattern::matches)

        if (
            publishedSha != null &&
                publishedSize != null &&
                publishedSize > 0
        ) {
            return FileIdentity(
                filename = filename,
                bytes = publishedSize,
                sha256 = publishedSha,
                mediaType = mediaType(filename),
            )
        }

        val known = alreadyRead
        val bytes =
            known
                ?: run {
                    if (
                        publishedSize != null &&
                            publishedSize > SmallFileBodyLimit
                    ) {
                        throw InspectionException(
                            FailureCode.MODEL_MEMBER_IDENTITY_UNAVAILABLE,
                            "provider did not publish a SHA-256 for large runtime member " + filename,
                        )
                    }
                    exactSmallFile(
                        transport = transport,
                        repository = repository,
                        revision = revision,
                        filename = filename,
                    )
                }
        return FileIdentity(
            filename = filename,
            bytes = bytes.size.toLong(),
            sha256 = sha256(bytes),
            mediaType = mediaType(filename),
        )
    }

    private fun exactSmallFile(
        transport: Transport,
        repository: String,
        revision: String,
        filename: String,
    ): ByteArray =
        getRequired(
            transport = transport,
            url = resolveUrl(repository, revision, filename),
            maxBytes = SmallFileBodyLimit,
            code = FailureCode.SOURCE_UNAVAILABLE,
            context = filename,
        ).body

    private fun resolveUrl(
        repository: String,
        revision: String,
        filename: String,
    ): String =
        "https://huggingface.co/" +
            repository +
            "/resolve/" +
            revision +
            "/" +
            filename +
            "?download=true"

    private fun getRequired(
        transport: Transport,
        url: String,
        maxBytes: Long,
        code: FailureCode,
        context: String,
    ): HttpResult {
        val response =
            runCatching {
                transport.get(
                    url,
                    maxBytes,
                )
            }
                .getOrElse { error ->
                    if (error is InspectionException) throw error
                    throw InspectionException(
                        code,
                        "could not read " + context,
                        error,
                    )
                }
        if (response.status !in 200..299) {
            throw InspectionException(
                code,
                context + " returned HTTP " + response.status,
            )
        }
        return response
    }

    private fun normalizedEtag(value: String?): String? =
        value
            ?.trim()
            ?.removePrefix("W/")
            ?.trim('"')
            ?.lowercase()

    private fun mediaType(filename: String): String =
        when {
            filename.endsWith(".json") -> "application/json"
            filename.endsWith(".jinja") || filename.endsWith(".txt") -> "text/plain"
            filename.endsWith(".safetensors") -> "application/octet-stream"
            else -> "application/octet-stream"
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.contentOrNull

    private fun JsonObject.boolean(key: String): Boolean? =
        (this[key] as? JsonPrimitive)
            ?.booleanOrNull

    private fun JsonObject.obj(key: String): JsonObject? =
        this[key] as? JsonObject

    private fun compareUtf8(
        left: String,
        right: String,
    ): Int {
        val a = left.toByteArray(StandardCharsets.UTF_8)
        val b = right.toByteArray(StandardCharsets.UTF_8)
        val limit = minOf(a.size, b.size)
        for (index in 0 until limit) {
            val compare = (a[index].toInt() and 0xff) - (b[index].toInt() and 0xff)
            if (compare != 0) return compare
        }
        return a.size - b.size
    }
}

// [VXG RealForever]
