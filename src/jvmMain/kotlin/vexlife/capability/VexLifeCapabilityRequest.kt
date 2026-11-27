package vexlife.capability

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import vexlife.contract.VexContractJson

/**
 * The one request shape Vex writes to ask the host for reach, and its parse.
 *
 * Relationship (the same one Conversation Source Return proved, generalized):
 * ```
 * Vex's whole reply for this stage = header line + one JSON object
 *   -> host parses it here (no effect)
 *   -> VexLifeCapabilityBridge admits it against this turn's frame and performs it via the owner
 *   -> VEXLIFE_CAPABILITY_RESULT/v1 returns in the next provider stage of the same human turn
 * ```
 *
 * Parsing is deliberately forgiving about form (code fences, `capabilityRef` vs `capability`,
 * trailing whitespace, one exact nested protocol/version wrapper, and the exact sibling-version
 * representation observed from the qualified Qwen 4B runtime) and strict about meaning.
 * The canonical header-line form remains preferred. A clearly intended request that cannot be read
 * becomes [Parsed.Malformed], which the bridge answers with the exact form instead of failing the
 * human's turn.
 */
object VexLifeCapabilityRequest {
    const val RequestHeader = "VEXLIFE_CAPABILITY_REQUEST/v1"
    const val ResultHeader = "VEXLIFE_CAPABILITY_RESULT/v1"

    /**
     * One source-owned instruction for the text-control request channel.
     *
     * Ordinary prompt projection and exact-runtime qualification both consume this relationship so
     * provider guidance cannot drift away from the parser/Bridge contract.
     */
    val TextControlProviderInstruction =
        "For a text-control capability request, the whole reply for that provider stage must begin " +
            "with the exact header " +
            RequestHeader +
            " followed by one JSON object with capability, action, and arguments. " +
            "The canonical header is mandatory unless the exact structured protocol/version wrapper " +
            "is used; bare JSON or code-fenced JSON without either admitted request marker is " +
            "ordinary assistant text and performs nothing. Stop composing the human-facing answer " +
            "until the host returns a real " +
            ResultHeader +
            " result."

    /** The header must start a line within this many characters of the start of the reply. */
    const val MaximumControlPreambleCharacters = 512

    data class Call(
        val capability: String,
        val action: String,
        val arguments: JsonObject,
    )

    sealed interface Parsed {
        /** The exact control text Vex emitted, kept as an assistant message in later stages. */
        val controlContent: String
        val controlSha256: String

        data class Request(
            val call: Call,
            override val controlContent: String,
            override val controlSha256: String,
        ) : Parsed

        data class Malformed(
            val reason: String,
            override val controlContent: String,
            override val controlSha256: String,
        ) : Parsed
    }

    /** Index of the request header when the reply is choosing the control, else null. */
    fun controlHeaderIndexOrNull(modelContent: String): Int? {
        val index = modelContent.indexOf(RequestHeader)
        if (index < 0 || index > MaximumControlPreambleCharacters) return null
        if (index > 0 && modelContent[index - 1] != '\n') return null
        return index
    }

    /**
     * Null when the reply is not a capability request; otherwise the request or why it is
     * unreadable.
     */
    fun parseOrNull(modelContent: String): Parsed? {
        val rawHeaderIndex =
            Regex(
                    "(?m)^" +
                        Regex.escape(RequestHeader)
                )
                .find(modelContent)
                ?.range
                ?.first
                ?: return parseStructuredWrapperOrNull(modelContent)
        val headerIndex =
            controlHeaderIndexOrNull(modelContent)
                ?: return Parsed.Malformed(
                    reason =
                        "the capability request header appeared outside the machine-control prefix; " +
                            "if I intend to reach, the request must be my whole reply for this provider stage",
                    controlContent = modelContent.substring(rawHeaderIndex).trimEnd(),
                    controlSha256 = sha256(modelContent.substring(rawHeaderIndex).trimEnd()),
                )
        val control = modelContent.substring(headerIndex).trimEnd()
        val sha = sha256(control)
        fun malformed(reason: String) = Parsed.Malformed(reason, control, sha)

        if (control.indexOf(RequestHeader, RequestHeader.length) >= 0) {
            return malformed("one request per reply: the header appeared more than once")
        }
        val body =
            control
                .substring(RequestHeader.length)
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .trim()
                .removeSuffix("```")
                .trim()
        val objectText =
            firstJsonObject(body) ?: return malformed("no JSON object after the header")
        val element =
            runCatching { VexContractJson.Strict.parseToJsonElement(objectText) }.getOrNull()
                ?: return malformed("the JSON after the header is not valid JSON")
        val json = element as? JsonObject ?: return malformed("the request must be one JSON object")
        return parseRequestObject(
            json = json,
            control = control,
            sha = sha,
        )
    }

    private fun parseStructuredWrapperOrNull(modelContent: String): Parsed? {
        val control =
            modelContent
                .trim()
        val body =
            control
                .removePrefix("```json")
                .removePrefix("```")
                .trim()
                .removeSuffix("```")
                .trim()
        if (
            !body.startsWith(
                "{",
            )
        ) {
            return null
        }

        val objectText =
            firstJsonObject(
                body,
            )
                ?: return null
        if (
            objectText.length !=
                body.length
        ) {
            return null
        }
        val element =
            runCatching {
                    VexContractJson.Strict.parseToJsonElement(
                        objectText,
                    )
                }
                .getOrNull()
                ?: return null
        val wrapper =
            element as? JsonObject
                ?: return null
        if (
            "VEXLIFE_CAPABILITY_REQUEST" !in
            wrapper
        ) {
            return null
        }

        val sha =
            sha256(
                control,
            )
        fun malformed(reason: String) =
            Parsed.Malformed(
                reason = reason,
                controlContent = control,
                controlSha256 = sha,
            )

        val siblingVersion =
            wrapper[
                "VEXLIFE_CAPABILITY_REQUEST"
            ]
                .stringOrNull()
        if (
            siblingVersion !=
                null
        ) {
            if (
                siblingVersion !=
                    "v1"
            ) {
                return malformed(
                    "structured capability request sibling version must be exactly \"v1\"",
                )
            }
            val expectedKeys =
                setOf(
                    "VEXLIFE_CAPABILITY_REQUEST",
                    "capability",
                    "action",
                    "arguments",
                )
            if (
                wrapper.keys !=
                    expectedKeys
            ) {
                return malformed(
                    "structured capability request sibling form must contain only the exact v1 marker, capability, action, and arguments",
                )
            }
            return parseRequestObject(
                json =
                    JsonObject(
                        wrapper.filterKeys {
                            it !=
                                "VEXLIFE_CAPABILITY_REQUEST"
                        },
                    ),
                control = control,
                sha = sha,
            )
        }

        if (
            wrapper.keys !=
                setOf(
                    "VEXLIFE_CAPABILITY_REQUEST",
                )
        ) {
            return malformed(
                "structured capability request wrapper must contain only \"VEXLIFE_CAPABILITY_REQUEST\"",
            )
        }
        val protocol =
            wrapper[
                "VEXLIFE_CAPABILITY_REQUEST"
            ] as? JsonObject
                ?: return malformed(
                    "structured capability request wrapper must contain one version object",
                )
        if (
            protocol.keys !=
                setOf(
                    "v1",
                )
        ) {
            return malformed(
                "structured capability request wrapper must contain only version \"v1\"",
            )
        }
        val json =
            protocol[
                "v1"
            ] as? JsonObject
                ?: return malformed(
                    "structured capability request version \"v1\" must contain one request object",
                )

        return parseRequestObject(
            json = json,
            control = control,
            sha = sha,
        )
    }

    private fun parseRequestObject(
        json: JsonObject,
        control: String,
        sha: String,
    ): Parsed {
        fun malformed(reason: String) =
            Parsed.Malformed(
                reason = reason,
                controlContent = control,
                controlSha256 = sha,
            )

        val capability =
            (
                json[
                    "capability"
                ] ?: json[
                    "capabilityRef"
                ]
            )
                .stringOrNull()
                ?: return malformed(
                    "missing \"capability\" (a handle such as web, github, home, memory, atlas)",
                )
        val action =
            json[
                "action"
            ]
                .stringOrNull()
                ?: return malformed(
                    "missing \"action\"",
                )
        val arguments =
            when (
                val value =
                    json[
                        "arguments"
                    ]
            ) {
                null ->
                    JsonObject(
                        emptyMap(),
                    )

                is JsonObject ->
                    value

                else ->
                    return malformed(
                        "\"arguments\" must be a JSON object",
                    )
            }

        return Parsed.Request(
            call =
                Call(
                    capability = capability.trim(),
                    action = action.trim(),
                    arguments = arguments,
                ),
            controlContent = control,
            controlSha256 = sha,
        )
    }

    /** The text of the first balanced `{...}` object in [text], honouring JSON strings. */
    private fun firstJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun JsonElement?.stringOrNull(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.takeIf { it.isNotBlank() }

    internal fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

// [VXG RealForever]
