package vexlife.model.heretic

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.contract.VexContractJson

/**
 * Provider/model wire normalization for the held Qwen 4B text-control representation.
 *
 * The semantic request language remains [VexLifeCapabilityRequest]. This adapter accepts only one
 * exact provider-emitted representation observed in lived A020 evidence and converts it into the
 * same canonical request text used by every Bridge path.
 *
 * WIRE REPRESENTATION != CANONICAL REQUEST MEANING
 * WIRE NORMALIZATION != CAPABILITY ADMISSION
 * WIRE NORMALIZATION != EFFECT AUTHORITY
 */
internal object HereticTextControlCapabilityWire {
    enum class Adaptation {
        NONE,
        QWEN4B_SIBLING_VERSION_V1,
    }

    data class Normalized(
        val canonicalControl: String,
        val adaptation: Adaptation,
    )

    fun normalize(
        profile: HereticCompanionRepresentation.Profile,
        content: String,
    ): Normalized {
        if (
            profile.profileRef !=
                HereticCompanionRepresentation.Profiles.Qwen4B.profileRef
        ) {
            return Normalized(
                canonicalControl = content,
                adaptation = Adaptation.NONE,
            )
        }

        val wrapper =
            exactWholeObject(
                content,
            )
                ?: return Normalized(
                    canonicalControl = content,
                    adaptation = Adaptation.NONE,
                )
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
            return Normalized(
                canonicalControl = content,
                adaptation = Adaptation.NONE,
            )
        }

        val marker =
            (wrapper["VEXLIFE_CAPABILITY_REQUEST"] as? JsonPrimitive)
                ?.takeIf(
                    JsonPrimitive::isString,
                )
                ?.contentOrNull
        if (
            marker !=
                "v1"
        ) {
            return Normalized(
                canonicalControl = content,
                adaptation = Adaptation.NONE,
            )
        }

        val capability =
            (wrapper["capability"] as? JsonPrimitive)
                ?.takeIf(
                    JsonPrimitive::isString,
                )
                ?.contentOrNull
                ?.takeIf(String::isNotBlank)
                ?: return Normalized(
                    canonicalControl = content,
                    adaptation = Adaptation.NONE,
                )
        val action =
            (wrapper["action"] as? JsonPrimitive)
                ?.takeIf(
                    JsonPrimitive::isString,
                )
                ?.contentOrNull
                ?.takeIf(String::isNotBlank)
                ?: return Normalized(
                    canonicalControl = content,
                    adaptation = Adaptation.NONE,
                )
        val arguments =
            wrapper["arguments"] as? JsonObject
                ?: return Normalized(
                    canonicalControl = content,
                    adaptation = Adaptation.NONE,
                )

        val request =
            JsonObject(
                linkedMapOf(
                    "capability" to
                        JsonPrimitive(
                            capability,
                        ),
                    "action" to
                        JsonPrimitive(
                            action,
                        ),
                    "arguments" to arguments,
                ),
            )

        return Normalized(
            canonicalControl =
                VexLifeCapabilityRequest.RequestHeader +
                    "\n" +
                    VexContractJson.canonicalEncodeElement(
                        request,
                    ),
            adaptation =
                Adaptation.QWEN4B_SIBLING_VERSION_V1,
        )
    }

    private fun exactWholeObject(
        content: String,
    ): JsonObject? {
        val body =
            content
                .trim()
                .removePrefix(
                    "```json",
                )
                .removePrefix(
                    "```",
                )
                .trim()
                .removeSuffix(
                    "```",
                )
                .trim()
        if (
            !body.startsWith(
                "{",
            ) ||
            !body.endsWith(
                "}",
            )
        ) {
            return null
        }
        return runCatching {
                VexContractJson.Strict
                    .parseToJsonElement(
                        body,
                    )
            }
            .getOrNull() as? JsonObject
    }
}

// [VXG RealForever]
