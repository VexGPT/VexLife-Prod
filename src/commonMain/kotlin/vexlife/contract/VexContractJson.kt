package vexlife.contract

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * One repository-owned JSON boundary for durable VexLife contracts.
 *
 * Domain packages own schemas and meaning. This object owns deterministic transport plus the
 * canonical key-sorted compact JSON preimage used by current VexLife semantic hashes.
 */
object VexContractJson {
    val Strict =
        Json {
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = false
            isLenient = false
            allowSpecialFloatingPointValues = false
            allowStructuredMapKeys = false
            prettyPrint = true
        }

    private val Canonical =
        Json {
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = false
            isLenient = false
            allowSpecialFloatingPointValues = false
            allowStructuredMapKeys = false
            prettyPrint = false
        }

    inline fun <reified T> encode(
        value: T,
    ): String =
        Strict.encodeToString(
            value,
        )

    inline fun <reified T> decode(
        text: String,
    ): T =
        Strict.decodeFromString(
            text,
        )

    /**
     * Produce the same canonical JSON preimage as current VexLife `semanticHash`:
     * recursively sort object keys, preserve array order, and emit compact JSON.
     */
    inline fun <reified T> canonicalEncode(
        value: T,
    ): String =
        canonicalEncodeElement(
            Strict.encodeToJsonElement(
                value,
            ),
        )

    fun canonicalEncodeElement(
        value: JsonElement,
    ): String =
        Canonical.encodeToString(
            JsonElement.serializer(),
            canonicalize(
                value,
            ),
        )

    fun canonicalize(
        value: JsonElement,
    ): JsonElement =
        when (
            value
        ) {
            is JsonObject ->
                JsonObject(
                    value.entries
                        .sortedBy {
                            it.key
                        }
                        .associate {
                            entry ->
                            entry.key to
                                canonicalize(
                                    entry.value,
                                )
                        },
                )

            is JsonArray ->
                JsonArray(
                    value.map(
                        ::canonicalize,
                    ),
                )

            else ->
                value
        }

    fun schemaVersion(
        text: String,
    ): String {
        val value =
            Strict
                .parseToJsonElement(
                    text,
                )
                .jsonObject[
                    "schemaVersion"
                ]
                ?.jsonPrimitive
                ?.content

        require(
            !value.isNullOrBlank(),
        ) {
            "durable contract requires one non-empty schemaVersion"
        }

        return value
    }
}

// [VXG RealForever]
