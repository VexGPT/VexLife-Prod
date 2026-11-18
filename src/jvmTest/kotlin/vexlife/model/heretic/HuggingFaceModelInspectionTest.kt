package vexlife.model.heretic

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HuggingFaceModelInspectionTest {
    private val revision = "73e3e38d981303bc594367cd910ea6eb48349da8"
    private val config =
        """{"architectures":["Qwen3ForCausalLM"],"model_type":"qwen3","quantization":{"bits":4,"group_size":128}}"""
            .toByteArray(StandardCharsets.UTF_8)

    private fun metadata(): ByteArray =
        """
        {
          "sha":"$revision",
          "private":false,
          "gated":false,
          "disabled":false,
          "tags":["text-generation","mlx","safetensors","qwen3","license:apache-2.0"],
          "cardData":{"license":"apache-2.0"},
          "siblings":[
            {"rfilename":"config.json"},
            {"rfilename":"tokenizer.json"},
            {"rfilename":"tokenizer_config.json"},
            {"rfilename":"model.safetensors"},
            {"rfilename":"model.safetensors.index.json"},
            {"rfilename":"README.md"}
          ]
        }
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

    private class FakeTransport(
        private val revision: String,
        private val metadata: ByteArray,
        private val config: ByteArray,
    ) : HuggingFaceModelInspection.Transport {
        val gets = mutableListOf<String>()
        val heads = mutableListOf<String>()

        override fun get(
            url: String,
            maxBytes: Long,
        ): HuggingFaceModelInspection.HttpResult {
            gets += url
            val body =
                when {
                    url.contains("/api/models/") -> metadata
                    url.contains("/config.json") -> config
                    else -> error("unexpected GET $url")
                }
            require(body.size <= maxBytes)
            return HuggingFaceModelInspection.HttpResult(
                status = 200,
                headers = emptyMap(),
                body = body,
            )
        }

        override fun head(url: String): HuggingFaceModelInspection.HttpResult {
            heads += url
            val filename = url.substringAfter("/$revision/").substringBefore('?')
            val identity =
                when (filename) {
                    "config.json" -> config.size.toLong() to sha256(config)
                    "tokenizer.json" -> 11_400_000L to "a".repeat(64)
                    "tokenizer_config.json" -> 9_700L to "b".repeat(64)
                    "model.safetensors" -> 335_000_000L to "c".repeat(64)
                    "model.safetensors.index.json" -> 49_700L to "d".repeat(64)
                    else -> error("unexpected HEAD $filename")
                }
            return HuggingFaceModelInspection.HttpResult(
                status = 302,
                headers =
                    mapOf(
                        "x-linked-size" to listOf(identity.first.toString()),
                        "x-linked-etag" to listOf("\"${identity.second}\""),
                    ),
            )
        }
    }

    @Test
    fun publicQwen3MlxPageBecomesOneExactImportWithoutDownloadingWeights() {
        val transport =
            FakeTransport(
                revision = revision,
                metadata = metadata(),
                config = config,
            )

        val candidate =
            HuggingFaceModelInspection.inspect(
                "https://huggingface.co/mlx-community/Qwen3-0.6B-4bit/tree/main",
                transport,
            )

        assertEquals(
            "https://huggingface.co/mlx-community/Qwen3-0.6B-4bit",
            candidate.sourceUrl,
        )
        assertEquals(revision, candidate.providerRevision)
        assertEquals("apache-2.0", candidate.providerLicense)
        assertEquals(
            HereticModelImport.RepresentationQwen3OpenAiChat,
            candidate.relationship,
        )
        assertFalse(candidate.testedByVexLife)
        assertFalse(candidate.memoryExpectationEstablished)
        assertEquals(
            335_000_000L + 11_400_000L + 9_700L + 49_700L + config.size,
            candidate.totalBytes,
        )
        assertTrue(
            candidate.record.members.any {
                it.filename == "model.safetensors" &&
                    it.sha256 == "c".repeat(64) &&
                    it.expectedBytes == 335_000_000L
            },
        )
        assertFalse(
            transport.gets.any { it.contains("/model.safetensors") },
            "Check must not download the large model weights",
        )
        assertEquals(
            HereticModelCatalog.Origin.HOME_IMPORTED,
            HereticModelImport.entry(candidate.record).origin,
        )
    }

    @Test
    fun providerRemoteCodeIsHeldBeforeItCanBecomeAnImport() {
        val remoteCode =
            """{"architectures":["Qwen3ForCausalLM"],"model_type":"qwen3","auto_map":{"AutoModel":"custom.Model"}}"""
                .toByteArray(StandardCharsets.UTF_8)
        val failure =
            assertFailsWith<HuggingFaceModelInspection.InspectionException> {
                HuggingFaceModelInspection.inspect(
                    "https://huggingface.co/example/remote-code-model",
                    FakeTransport(
                        revision = revision,
                        metadata = metadata(),
                        config = remoteCode,
                    ),
                )
            }
        assertEquals(
            HuggingFaceModelInspection.FailureCode.MODEL_REMOTE_CODE_REQUIRED,
            failure.code,
        )
    }

    companion object {
        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}

// [VXG RealForever]
