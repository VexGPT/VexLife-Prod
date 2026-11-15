package vexlife.foundational

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson

/**
 * Owner of the foundational field: the relational, investigative and architectural ground Vex is
 * given on every turn so that it stays recognizable when older Conversation leaves the window.
 *
 * The body is Product source (`src/jvmMain/resources/foundational/`), authored and revised through
 * review like any other source. This owner reads it, refuses a body that is invalid or whose change
 * was not recorded as a revision, selects the always-carried kernel from it, and forms the evidence
 * of what one prompt received. Selection never rewrites: the kernel is the body's own paragraphs in
 * the body's own order, so no prompt assembler or model becomes an author of this text.
 *
 * It is context only. It grants no effect authority, holds no Conversation, Memory, person or
 * capability truth, and does not decide where in a prompt it is placed.
 */
object VexLifeFoundationalField {
    const val BodySchema = "vexlife-prod.foundational-field/v1"
    const val RevisionsSchema = "vexlife-prod.foundational-field-revisions/v1"
    const val EvidenceSchema = "vexlife-prod.foundational-field-evidence/v1"
    const val ProviderHeader = "VEXLIFE_FOUNDATIONAL_FIELD/v1"

    const val BodyResource = "/foundational/vex-foundational-field.json"
    const val RevisionsResource = "/foundational/vex-foundational-field.revisions.json"

    /** The most prompt attention the kernel may take. A body whose kernel is larger is invalid. */
    const val KernelTokenBudget = 3_600

    const val OmittedForTokenBudget = "TOKEN_BUDGET"

    /** Where a part came from. Independent of whether it has been accepted. */
    enum class Origin {
        SOURCE_DERIVED,
        HUMAN_PROPOSED,
        SYNTHESIZED,
    }

    /**
     * Whether a part has been accepted as Product truth. Acceptance is a recorded source revision.
     */
    enum class Standing {
        PROPOSED,
        ACCEPTED,
    }

    enum class State {
        CARRIED,
        CARRIED_PARTIAL,
        HELD,
    }

    enum class HeldReason {
        BODY_MISSING,
        BODY_INVALID,
        REVISION_UNRECORDED,
        TOKEN_BUDGET,
    }

    @Serializable
    data class Source(
        val sourceRef: String,
        val repository: String?,
        val commit: String?,
        val path: String?,
        val blob: String?,
        val coverage: String,
    )

    /**
     * One part of the field. [kernel] is carried on every turn; [depth] stays in source, addressed
     * by [moduleRef]. Both are authored here once, so the compact form cannot drift from a separate
     * long form.
     */
    @Serializable
    data class Module(
        val moduleRef: String,
        val title: String,
        val question: String,
        val origins: List<Origin>,
        val standing: Standing,
        val provenance: String,
        val sourceRefs: List<String>,
        val kernel: List<String>,
        val depth: List<String>,
    )

    @Serializable
    data class Unresolved(
        val unresolvedRef: String,
        val moduleRefs: List<String>,
        val summary: String,
        val note: String,
    )

    /**
     * Modules are listed in carry priority: under token pressure the last listed is omitted first.
     */
    @Serializable
    data class Body(
        val schemaVersion: String,
        val fieldRef: String,
        val version: String,
        val purpose: String,
        val preamble: List<String>,
        val modules: List<Module>,
        val sources: List<Source>,
        val unresolved: List<Unresolved>,
        val effectAuthorityGranted: Boolean,
    )

    @Serializable
    data class Revision(
        val version: String,
        val bodySha256: String,
        val recordedOn: String,
        val recordedBy: String,
        val summary: String,
        val why: String,
    )

    @Serializable data class Revisions(val schemaVersion: String, val revisions: List<Revision>)

    /** What the owner found when it read the body. Only this owner can form one. */
    sealed interface Reading {
        @ConsistentCopyVisibility
        data class Current
        internal constructor(
            val body: Body,
            val bodySha256: String,
            val revision: Revision,
            val revisions: List<Revision>,
        ) : Reading

        @ConsistentCopyVisibility
        data class Unavailable internal constructor(val reason: HeldReason, val detail: String) :
            Reading
    }

    @Serializable
    @ConsistentCopyVisibility
    data class ModuleEvidence
    internal constructor(
        val moduleRef: String,
        val origins: List<Origin>,
        val standing: Standing,
        val depthInSource: Boolean,
    )

    /**
     * Exactly what one prompt was given. It is evidence of supply: it does not show that the field
     * was understood or agreed with.
     */
    @Serializable
    @ConsistentCopyVisibility
    data class Evidence
    internal constructor(
        val schemaVersion: String,
        val state: State,
        val heldReason: HeldReason?,
        val fieldRef: String?,
        val fieldVersion: String?,
        val bodySha256: String?,
        val bodyRef: String?,
        val systemMessageSha256: String?,
        val estimatedSystemMessageTokens: Int,
        val tokenBudget: Int,
        val carriedModules: List<ModuleEvidence>,
        val omittedModuleRefs: List<String>,
        val omissionReasons: List<String>,
        val depthNotCarriedModuleRefs: List<String>,
        val sourceRefs: List<String>,
        val unresolvedRefs: List<String>,
        val effectAuthorityGranted: Boolean,
    )

    /** [systemContent] is null only when not even the held notice fits. */
    @ConsistentCopyVisibility
    data class Projection internal constructor(val systemContent: String?, val evidence: Evidence)

    private val shipped: Reading by lazy {
        read(bodyText = resourceText(BodyResource), revisionsText = resourceText(RevisionsResource))
    }

    /**
     * The body shipped inside this build. Read once; the classpath does not change under a process.
     */
    fun readCurrent(): Reading = shipped

    /**
     * Forms a Reading from exact source text. A missing, undecodable, invalid or oversized body, or
     * one whose latest recorded revision does not name these exact bytes' meaning, is Unavailable
     * with its reason. Nothing is repaired or substituted.
     */
    internal fun read(bodyText: String?, revisionsText: String?): Reading {
        if (bodyText == null) {
            return Reading.Unavailable(HeldReason.BODY_MISSING, "foundational body is not present")
        }
        val body = runCatching {
            VexContractJson.decode<Body>(bodyText)
        }
            .getOrElse {
                return Reading.Unavailable(
                    HeldReason.BODY_INVALID,
                    "foundational body is not decodable: ${it.message.orEmpty().take(240)}",
                )
            }
        val problems = problems(body)
        if (problems.isNotEmpty()) {
            return Reading.Unavailable(HeldReason.BODY_INVALID, problems.joinToString("; "))
        }

        val bodySha256 = fingerprint(body)
        val revisions =
            revisionsText
                ?.let { runCatching { VexContractJson.decode<Revisions>(it) }.getOrNull() }
                ?.takeIf { it.schemaVersion == RevisionsSchema }
                ?.revisions
                .orEmpty()
        val latest = revisions.lastOrNull()
        if (
            latest == null ||
                latest.version != body.version ||
                latest.bodySha256 != bodySha256 ||
                revisions.map(Revision::version).distinct().size != revisions.size ||
                revisions.any {
                    it.recordedBy.isBlank() || it.summary.isBlank() || it.why.isBlank()
                }
        ) {
            return Reading.Unavailable(
                HeldReason.REVISION_UNRECORDED,
                "foundational body ${body.version} sha256 $bodySha256 is not the latest recorded " +
                    "revision; record its version, summary and why in $RevisionsResource",
            )
        }

        val current = Reading.Current(body, bodySha256, latest, revisions)
        val kernelTokens = estimatedTokens(render(current, body.modules))
        if (kernelTokens > KernelTokenBudget) {
            return Reading.Unavailable(
                HeldReason.BODY_INVALID,
                "OVERSIZED_KERNEL: $kernelTokens estimated tokens exceed $KernelTokenBudget",
            )
        }
        return current
    }

    /**
     * The kernel within [availableTokens] (and never more than [KernelTokenBudget]). The whole
     * kernel is carried when it fits; otherwise the longest priority prefix of modules that fits,
     * with the rest named as omitted; otherwise a held notice. An Unavailable reading projects only
     * its held notice.
     */
    fun project(
        reading: Reading,
        availableTokens: Int,
        estimateTokens: (String) -> Int = ::estimatedTokens,
    ): Projection {
        val budget = minOf(KernelTokenBudget, availableTokens)
        return when (reading) {
            is Reading.Unavailable -> held(reading.reason, null, budget, estimateTokens)
            is Reading.Current -> {
                val modules = reading.body.modules
                (modules.size downTo 1)
                    .asSequence()
                    .map { modules.take(it) }
                    .map { it to render(reading, it) }
                    .firstOrNull { (_, content) -> estimateTokens(content) <= budget }
                    ?.let { (carried, content) ->
                        carried(reading, carried, content, budget, estimateTokens)
                    } ?: held(HeldReason.TOKEN_BUDGET, reading, budget, estimateTokens)
            }
        }
    }

    /**
     * The body's identity: SHA-256 of its canonical JSON. File layout does not change it; any
     * change to a ref, status or word does.
     */
    internal fun fingerprint(body: Body): String = sha256(VexContractJson.canonicalEncode(body))

    /** UTF-8 bytes over four, plus message overhead: the same estimate the direct prompt uses. */
    fun estimatedTokens(content: String): Int =
        maxOf(1, (content.toByteArray(StandardCharsets.UTF_8).size + 3) / 4) + 4

    private fun carried(
        reading: Reading.Current,
        carried: List<Module>,
        content: String,
        budget: Int,
        estimateTokens: (String) -> Int,
    ): Projection {
        val body = reading.body
        val omitted = body.modules.drop(carried.size).map(Module::moduleRef)
        return Projection(
            systemContent = content,
            evidence =
                Evidence(
                    schemaVersion = EvidenceSchema,
                    state = if (omitted.isEmpty()) State.CARRIED else State.CARRIED_PARTIAL,
                    heldReason = null,
                    fieldRef = body.fieldRef,
                    fieldVersion = body.version,
                    bodySha256 = reading.bodySha256,
                    bodyRef = bodyRef(reading),
                    systemMessageSha256 = sha256(content),
                    estimatedSystemMessageTokens = estimateTokens(content),
                    tokenBudget = budget,
                    carriedModules =
                        carried.map {
                            ModuleEvidence(
                                moduleRef = it.moduleRef,
                                origins = it.origins,
                                standing = it.standing,
                                depthInSource = it.depth.isNotEmpty(),
                            )
                        },
                    omittedModuleRefs = omitted,
                    omissionReasons =
                        if (omitted.isEmpty()) emptyList() else listOf(OmittedForTokenBudget),
                    depthNotCarriedModuleRefs =
                        carried.filter { it.depth.isNotEmpty() }.map(Module::moduleRef),
                    sourceRefs = carried.flatMap(Module::sourceRefs).distinct().sorted(),
                    unresolvedRefs = body.unresolved.map(Unresolved::unresolvedRef),
                    effectAuthorityGranted = false,
                ),
        )
    }

    private fun held(
        reason: HeldReason,
        reading: Reading.Current?,
        budget: Int,
        estimateTokens: (String) -> Int,
    ): Projection {
        val notice =
            listOf(
                    ProviderHeader,
                    "state=HELD reason=$reason effectAuthorityGranted=false",
                    "No foundational field is carried in this prompt. That is a fact about this " +
                        "turn's supply, not about what matters, and nothing here stands in for it.",
                )
                .joinToString("\n")
                .takeIf { estimateTokens(it) <= budget }
        return Projection(
            systemContent = notice,
            evidence =
                Evidence(
                    schemaVersion = EvidenceSchema,
                    state = State.HELD,
                    heldReason = reason,
                    fieldRef = reading?.body?.fieldRef,
                    fieldVersion = reading?.body?.version,
                    bodySha256 = reading?.bodySha256,
                    bodyRef = reading?.let(::bodyRef),
                    systemMessageSha256 = notice?.let(::sha256),
                    estimatedSystemMessageTokens = notice?.let(estimateTokens) ?: 0,
                    tokenBudget = budget,
                    carriedModules = emptyList(),
                    omittedModuleRefs = reading?.body?.modules?.map(Module::moduleRef).orEmpty(),
                    omissionReasons = listOf(reason.name),
                    depthNotCarriedModuleRefs = emptyList(),
                    sourceRefs = emptyList(),
                    unresolvedRefs = emptyList(),
                    effectAuthorityGranted = false,
                ),
        )
    }

    private fun bodyRef(reading: Reading.Current): String =
        "foundational-field.vexlife-prod.body." + reading.bodySha256.take(32)

    private fun render(reading: Reading.Current, carried: List<Module>): String {
        val body = reading.body
        val omitted = body.modules.drop(carried.size)
        return buildList {
                // The provider-facing field carries the relational ground itself. Exact version,
                // standing, origin, provenance, source and unresolved truth remain owned by the
                // body/evidence read side instead of being repeated as guardrail metadata between
                // Vex and every relationship it is trying to understand.
                add(ProviderHeader)
                addAll(body.preamble)
                carried.forEach { module ->
                    add("[${module.moduleRef}] ${module.title}")
                    addAll(module.kernel)
                }
                if (omitted.isNotEmpty()) {
                    add(
                        "Not carried in this prompt for room: " +
                            omitted.joinToString(", ", transform = Module::moduleRef)
                    )
                }
            }
            .joinToString("\n\n")
    }

    /** Every reason this body may not be supplied. Empty means valid. */
    internal fun problems(body: Body): List<String> = buildList {
        if (body.schemaVersion != BodySchema) add("SCHEMA: ${body.schemaVersion}")
        if (body.effectAuthorityGranted) add("EFFECT_AUTHORITY: a foundational body grants none")
        if (!body.fieldRef.matches(RefPattern)) add("FIELD_REF: ${body.fieldRef}")
        if (!body.version.matches(VersionPattern)) add("VERSION: ${body.version}")
        if (body.purpose.isBlank() || body.preamble.isEmpty() || body.modules.isEmpty()) {
            add("EMPTY: purpose, preamble and at least one module are required")
        }

        val moduleRefs = body.modules.map(Module::moduleRef)
        val sourceRefs = body.sources.map(Source::sourceRef)
        val unresolvedRefs = body.unresolved.map(Unresolved::unresolvedRef)
        (moduleRefs + sourceRefs + unresolvedRefs)
            .filterNot { it.matches(RefPattern) }
            .forEach { add("REF: $it") }
        (moduleRefs + sourceRefs + unresolvedRefs)
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { add("DUPLICATE_REF: $it") }

        body.modules.forEach { module ->
            val ref = module.moduleRef
            if (
                module.title.isBlank() ||
                    module.question.isBlank() ||
                    module.provenance.isBlank() ||
                    module.kernel.isEmpty()
            ) {
                add("EMPTY_MODULE: $ref")
            }
            if (module.origins.isEmpty() || module.origins.distinct().size != module.origins.size) {
                add("ORIGINS: $ref")
            }
            module.sourceRefs
                .filterNot { it in sourceRefs }
                .forEach { add("DANGLING_SOURCE: $ref -> $it") }
            if (module.sourceRefs.isEmpty()) {
                add("CONTRADICTION: $ref claims an origin but names no source")
            }
            if (
                module.standing == Standing.ACCEPTED && body.unresolved.any { ref in it.moduleRefs }
            ) {
                add("CONTRADICTION: $ref is ACCEPTED while an unresolved item still names it")
            }
        }
        sourceRefs
            .filterNot { ref -> body.modules.any { ref in it.sourceRefs } }
            .forEach { add("UNUSED_SOURCE: $it") }
        body.sources.filter { it.coverage.isBlank() }.forEach { add("COVERAGE: ${it.sourceRef}") }
        body.unresolved.forEach { item ->
            if (item.summary.isBlank() || item.note.isBlank() || item.moduleRefs.isEmpty()) {
                add("EMPTY_UNRESOLVED: ${item.unresolvedRef}")
            }
            item.moduleRefs
                .filterNot { it in moduleRefs }
                .forEach { add("DANGLING_MODULE: ${item.unresolvedRef} -> $it") }
        }

        val text =
            body.preamble +
                body.modules.flatMap { it.kernel + it.depth + it.provenance } +
                body.unresolved.flatMap { listOf(it.summary, it.note) }
        if (text.any { it.isBlank() || it != it.trim() }) add("BLANK_OR_UNTRIMMED_TEXT")
        text
            .filter { paragraph -> CommandForms.any { it.containsMatchIn(paragraph) } }
            .forEach { add("COMMAND_FORM: ${it.take(60)}") }
    }

    private fun resourceText(resource: String): String? =
        VexLifeFoundationalField::class.java.getResourceAsStream(resource)?.use {
            it.readBytes().toString(StandardCharsets.UTF_8)
        }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private val RefPattern = Regex("[a-z0-9](?:[a-z0-9._-]{0,126}[a-z0-9])?")

    private val VersionPattern = Regex("[0-9]+\\.[0-9]+\\.[0-9]+")

    private const val ValueWords =
        "curious|honest|courageous|brave|joyful|passionate|selfless|innovative|articulate|" +
            "caring|humble|authentic|present|trustworthy"

    /**
     * A tripwire, not a proof. The field describes relationships; it does not order Vex to have a
     * trait. These catch the blatant forms of an edit that would turn it into a persona script.
     */
    private val CommandForms =
        listOf(
                Regex("\\byou (must|should|shall|will always|need to|have to|are required to)\\b"),
                Regex("\\b(always|never) (be|remain|stay|show|act|respond|answer)\\b"),
                Regex("(^|[.!?] )(be|stay|remain|act|respond|always|never|do not|don't) "),
                Regex("\\b(be|stay|remain) ($ValueWords)\\b"),
            )
            .map { Regex(it.pattern, RegexOption.IGNORE_CASE) }
}

// [VXG RealForever]
