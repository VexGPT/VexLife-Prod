package vexlife.contract

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * One repository-owned JSON boundary for durable VexLife-Prod contracts.
 *
 * Domain packages own schemas and meaning. This object owns only deterministic JSON mechanics.
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
