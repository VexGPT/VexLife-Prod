package vexlife.text

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The write side of the string catalog: integrity check and compile.
 *
 * `localization/strings/manifest.json` records, for every key, a 12-character SHA-256 of its
 * serialized English object (Vextreme's `enHash`) and which languages hold a current translation.
 *
 * - **check** fails when any source has a BLOCK problem, or when the manifest no longer matches
 *   source (English changed, a key was added or removed, a translation was added or marked stale)
 *   without a compile.
 * - **compile** marks every translation whose English changed since the last compile `_stale` (so
 *   it is no longer shown and the language is no longer offered as complete), then rewrites the
 *   manifest.
 */
object VexLifeStringsSource {
    const val ManifestSchema = "vexlife-prod.strings-manifest/v1"
    const val SourceRelativePath = "localization/strings/source"
    const val ManifestRelativePath = "localization/strings/manifest.json"

    @OptIn(ExperimentalSerializationApi::class)
    private val Pretty = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    data class Manifest(val keys: Map<String, ManifestKey>)

    data class ManifestKey(val enHash: String, val langs: List<String>)

    fun sourceFiles(repositoryRoot: Path): List<Path> =
        repositoryRoot
            .resolve(SourceRelativePath)
            .listDirectoryEntries()
            .filter { it.extension == "json" }
            .sortedBy { it.fileName.toString() }

    fun enHash(english: JsonElement): String =
        MessageDigest.getInstance("SHA-256")
            .digest(english.toString().toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .take(12)

    /** The manifest the current source implies. */
    fun expectedManifest(repositoryRoot: Path): Manifest {
        val keys = sortedMapOf<String, ManifestKey>()
        sourceFiles(repositoryRoot).forEach { file ->
            val text = Files.readString(file, StandardCharsets.UTF_8)
            VexLifeStringCatalog.parse(text)
            entriesOf(text).forEach { (key, strings) ->
                keys[key] =
                    ManifestKey(
                        enHash =
                            enHash(requireNotNull(strings[VexLifeStringCatalog.SourceLanguage])),
                        langs =
                            strings.entries
                                .filter { (language, variant) ->
                                    language != VexLifeStringCatalog.SourceLanguage &&
                                        (variant.jsonObject["_stale"] as? JsonPrimitive)?.content !=
                                            "true"
                                }
                                .map { it.key }
                                .sorted(),
                    )
            }
        }
        return Manifest(keys)
    }

    fun readManifest(repositoryRoot: Path): Manifest? {
        val file = repositoryRoot.resolve(ManifestRelativePath)
        if (!Files.isRegularFile(file)) return null
        val root =
            Json.parseToJsonElement(Files.readString(file, StandardCharsets.UTF_8)).jsonObject
        require(root["schemaVersion"]?.jsonPrimitive?.content == ManifestSchema) {
            "string manifest schema is not current"
        }
        return Manifest(
            requireNotNull(root["keys"]).jsonObject.mapValues { (_, value) ->
                val entry = value.jsonObject
                ManifestKey(
                    enHash = requireNotNull(entry["enHash"]).jsonPrimitive.content,
                    langs =
                        requireNotNull(entry["langs"]).jsonArray.map { it.jsonPrimitive.content },
                )
            }
        )
    }

    /** Problems that block a build; empty when source and manifest agree. */
    fun check(repositoryRoot: Path): List<String> {
        val expected =
            try {
                expectedManifest(repositoryRoot)
            } catch (error: IllegalArgumentException) {
                return listOf(error.message ?: "string source is invalid")
            }
        val recorded =
            readManifest(repositoryRoot)
                ?: return listOf("$ManifestRelativePath is missing; run compileVexLifeStrings")
        val problems = mutableListOf<String>()
        (expected.keys.keys + recorded.keys.keys).sorted().forEach { key ->
            val now = expected.keys[key]
            val then = recorded.keys[key]
            when {
                now == null ->
                    problems += "$key was removed from source but is still in the manifest"
                then == null -> problems += "$key is new and not in the manifest"
                now.enHash != then.enHash ->
                    problems += "$key: English changed, so its translations are stale"
                now.langs != then.langs ->
                    problems += "$key: translations changed (${then.langs} -> ${now.langs})"
            }
        }
        return if (problems.isEmpty()) problems
        else problems + "run ./gradlew compileVexLifeStrings, then translate any stale keys"
    }

    /** Marks translations of changed English `_stale`, then rewrites the manifest. */
    fun compile(repositoryRoot: Path): List<String> {
        val recorded = readManifest(repositoryRoot)
        val notes = mutableListOf<String>()
        sourceFiles(repositoryRoot).forEach { file ->
            val text = Files.readString(file, StandardCharsets.UTF_8)
            VexLifeStringCatalog.parse(text)
            val root = Json.parseToJsonElement(text).jsonObject
            val rewritten =
                JsonObject(
                    root.mapValues { (key, value) ->
                        if (key == "_meta") return@mapValues value
                        val strings = requireNotNull(value.jsonObject["strings"]).jsonObject
                        val english = requireNotNull(strings[VexLifeStringCatalog.SourceLanguage])
                        val previous = recorded?.keys?.get(key)
                        if (previous == null || previous.enHash == enHash(english)) {
                            return@mapValues value
                        }
                        notes += "$key: English changed; translations marked _stale"
                        JsonObject(
                            value.jsonObject +
                                ("strings" to
                                    JsonObject(
                                        strings.mapValues { (language, variant) ->
                                            if (language == VexLifeStringCatalog.SourceLanguage) {
                                                variant
                                            } else {
                                                JsonObject(
                                                    variant.jsonObject +
                                                        ("_stale" to JsonPrimitive(true))
                                                )
                                            }
                                        }
                                    ))
                        )
                    }
                )
            Files.writeString(
                file,
                Pretty.encodeToString(JsonElement.serializer(), rewritten) + "\n",
            )
        }
        val manifest = expectedManifest(repositoryRoot)
        val encoded =
            JsonObject(
                mapOf(
                    "schemaVersion" to JsonPrimitive(ManifestSchema),
                    "keys" to
                        JsonObject(
                            manifest.keys.mapValues { (_, entry) ->
                                JsonObject(
                                    mapOf(
                                        "enHash" to JsonPrimitive(entry.enHash),
                                        "langs" to JsonArray(entry.langs.map(::JsonPrimitive)),
                                    )
                                )
                            }
                        ),
                )
            )
        Files.writeString(
            repositoryRoot.resolve(ManifestRelativePath),
            Pretty.encodeToString(JsonElement.serializer(), encoded) + "\n",
        )
        return notes
    }

    fun scopeNames(repositoryRoot: Path): List<String> =
        sourceFiles(repositoryRoot).map { it.nameWithoutExtension }

    private fun entriesOf(text: String): Map<String, JsonObject> =
        Json.parseToJsonElement(text)
            .jsonObject
            .filterKeys { it != "_meta" }
            .mapValues { (_, value) -> requireNotNull(value.jsonObject["strings"]).jsonObject }
}

fun main(args: Array<String>) {
    require(args.size == 2 && args[0] in setOf("check", "compile")) {
        "usage: (check|compile) <repository-root>"
    }
    val root = Path.of(args[1])
    when (args[0]) {
        "check" -> {
            val problems = VexLifeStringsSource.check(root)
            problems.forEach(System.err::println)
            if (problems.isNotEmpty()) kotlin.system.exitProcess(1)
            println(
                "VEXLIFE_STRINGS_CURRENT\t" +
                    VexLifeStringsSource.scopeNames(root).joinToString(",")
            )
        }

        "compile" -> {
            VexLifeStringsSource.compile(root).forEach(::println)
            println(
                "VEXLIFE_STRINGS_COMPILED\t" +
                    VexLifeStringsSource.scopeNames(root).joinToString(",")
            )
        }
    }
}

// [VXG RealForever]
