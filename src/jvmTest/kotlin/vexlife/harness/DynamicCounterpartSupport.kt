package vexlife.harness

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.companion.DirectPromptContext
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity

internal class DynamicCounterpartFixtureTransport : DirectCompanionConnection.Transport {
    override fun post(call: DirectCompanionConnection.HttpCall): DirectCompanionConnection.HttpResult {
        val body =
            buildJsonObject {
                put("model", HereticProductIdentity.RequestModel)
                put(
                    "choices",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put(
                                    "message",
                                    buildJsonObject {
                                        put("role", "assistant")
                                        put("content", "noted")
                                    },
                                )
                            },
                        ),
                    ),
                )
            }
                .toString()
        return DirectCompanionConnection.HttpResult(status = 200, body = body)
    }
}

internal fun dynamicCounterpartHooks(
    session: VexLifeProdLocalSetup.Session,
    transport: DirectCompanionConnection.Transport,
): DirectCompanionTurn.Hooks =
    DirectCompanionTurn.Hooks(
        transport = transport,
        currentRuntimeEvidence = { session.observeCurrentQualifiedRuntimeEvidence() },
        reobserveSameRuntimeEvidence = { session.observeCurrentQualifiedRuntimeEvidence() },
        experimentEffectiveInputTokenBudget = 52_000,
        experimentConversationContextProjectionEnabled = true,
    )

internal fun performDynamicCounterpartVexTurn(
    scratchHome: Path,
    evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
    content: String,
    idempotencyKey: String,
    hooks: DirectCompanionTurn.Hooks,
): DirectCompanionTurn.Result =
    DirectCompanionTurn.performWithHooks(
        setup =
            VexLifeProdLocalSetup.Result(
                state = "LOCAL_HERETIC_FOUNDATION_READY",
                productHome = scratchHome.toString(),
                homeDisposition = "EXISTING_HOME_PRESERVED",
                modelSelectionDisposition = "BOUND",
                managedPythonDisposition = "REUSED_VERIFIED",
                artifactDisposition = "REUSED_VERIFIED",
                environmentDisposition = "REUSED_VERIFIED",
                runtimeDisposition = "REUSED_EXACT_OWNED_MLX_RUNTIME",
                modelBindingDisposition = "BOUND_CURRENT",
                modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
                qualifiedRuntimeEvidence = evidence,
                endpoint = HereticProductIdentity.Endpoint,
                requestModel = HereticProductIdentity.RequestModel,
            ),
        intent = DirectCompanionTurn.Intent(idempotencyKey = idempotencyKey, content = content),
        hooks = hooks,
    )

internal fun dynamicCounterpartBackgroundPayload(
    seed: Long,
    ordinal: Int,
    targetTokens: Int,
): String {
    var state = seed xor (ordinal.toLong() * 0x9E3779B97F4A7C15uL.toLong())
    val words = StringBuilder()
    val vocab =
        listOf(
            "cobble", "moss", "window", "paper", "river", "amber", "ridge", "thread",
            "orchard", "signal", "stone", "quiet", "harborless", "meadow", "circuit", "map",
        )
    while (DirectPromptContext.estimatedMessageTokens(words.toString()) < targetTokens) {
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        words.append(vocab[((state ushr 1) % vocab.size).toInt()]).append(' ')
    }
    return words.toString().trimEnd()
}

internal fun writeDynamicCounterpartBundle(
    resultDir: Path,
    scenarioBytes: ByteArray,
    result: kotlinx.serialization.json.JsonObject,
    sarahRuntime: DynamicCounterpartSarahRuntime,
) {
    Files.createDirectories(resultDir)
    val files = linkedMapOf<String, ByteArray>()
    files["result.json"] = result.toString().toByteArray(StandardCharsets.UTF_8)
    files["scenario.json"] = scenarioBytes
    if (Files.exists(sarahRuntime.stdout)) {
        files["sarah-runtime/stdout.log"] = Files.readAllBytes(sarahRuntime.stdout)
    }
    if (Files.exists(sarahRuntime.stderr)) {
        files["sarah-runtime/stderr.log"] = Files.readAllBytes(sarahRuntime.stderr)
    }
    val manifest =
        buildJsonObject {
            put("schemaVersion", "vexlife.dynamic-counterpart-zip/v1")
            put(
                "files",
                buildJsonObject {
                    files.forEach { (name, bytes) -> put(name, sha256DynamicCounterpart(bytes)) }
                },
            )
        }
    val entries = linkedMapOf<String, ByteArray>()
    entries["manifest.json"] = manifest.toString().toByteArray(StandardCharsets.UTF_8)
    entries.putAll(files)
    val zipBytes = zipDynamicCounterpart(entries)
    val zipPath = resultDir.resolve(
        "dynamic-counterpart-${sha256DynamicCounterpart(scenarioBytes).take(16)}.zip",
    )
    Files.write(resultDir.resolve("result.json"), files.getValue("result.json"))
    Files.write(zipPath, zipBytes)
    println(
        buildJsonObject {
            put("state", "FORMED")
            put("zip", zipPath.toString())
            put("zipSha256", sha256DynamicCounterpart(zipBytes))
        },
    )
}

private fun zipDynamicCounterpart(entries: Map<String, ByteArray>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name).apply { time = 0L })
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}

private fun sha256DynamicCounterpart(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

// [VXG RealForever]
