package vexlife.continuity

import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mac-arm64 execution custody for the selected BGE-M3 dense representation.
 *
 * Artifact identity remains [VexLifeBgeM3ArtifactMaterialization]. Python/ONNX identity remains
 * [VexLifeBgeM3MacPythonEnvironment]. This owner joins those exact prerequisites into one
 * service-lifetime worker and exposes only [VexLifeVolatileDenseSemanticBackend.Encoder].
 *
 * The worker performs derived representation only. It does not own Conversation, Memory,
 * ContextRevisit semantics, persistence, model selection, or the selected conversational model.
 */
object VexLifeBgeM3MacRuntime {
    const val WorkerRequestSchema =
        "vexlife-prod.bge-m3-dense-worker-request/v1"

    const val WorkerResponseSchema =
        "vexlife-prod.bge-m3-dense-worker-response/v1"

    const val StartupTimeoutMs =
        30_000L

    const val EncodeTimeoutMs =
        300_000L

    const val CloseTimeoutMs =
        30_000L

    const val MaximumTextsPerWorkerRequest =
        64

    data class Binding(
        val productHome: String,
        val modelRoot: String,
        val artifactRef: String,
        val environmentRoot: String,
        val pythonPath: String,
        val environmentFingerprintSha256: String,
        val representationBindingRef: String,
    )

    data class Ready(
        val pid: Long,
        val dimension: Int,
        val providers: List<String>,
        val inputNames: List<String>,
    )

    interface Worker :
        AutoCloseable {
        val ready: Ready

        fun encode(
            requestRef: String,
            texts: List<String>,
        ): List<FloatArray>

        override fun close()
    }

    fun interface Launcher {
        fun launch(
            binding: Binding,
        ): Worker
    }

    object SystemLauncher :
        Launcher {
        override fun launch(
            binding: Binding,
        ): Worker =
            SystemWorker(
                binding,
            )
    }

    fun bind(
        productHome: Path,
        artifacts: VexLifeBgeM3ArtifactMaterialization.Observation,
        environment: VexLifeBgeM3MacPythonEnvironment.Result,
    ): Binding {
        val home =
            productHome
                .toAbsolutePath()
                .normalize()
        val expectedModelRoot =
            VexLifeBgeM3ArtifactMaterialization
                .modelRoot(
                    home,
                )
                .normalize()
        val expectedEnvironmentRoot =
            VexLifeBgeM3MacPythonEnvironment
                .environmentRoot(
                    home,
                )
                .normalize()
        val expectedPython =
            VexLifeBgeM3MacPythonEnvironment
                .pythonPath(
                    expectedEnvironmentRoot,
                )
                .normalize()

        require(
            artifacts.state ==
                VexLifeBgeM3ArtifactMaterialization.PresenceState.PRESENT &&
                artifacts.artifactRef ==
                VexLifeBgeM3ArtifactMaterialization.ArtifactRef &&
                Path.of(
                    artifacts.root,
                )
                    .normalize() ==
                expectedModelRoot &&
                artifacts.verifiedBytes ==
                VexLifeBgeM3ArtifactMaterialization.TotalBytes &&
                artifacts.members.all {
                    it.state ==
                        VexLifeBgeM3ArtifactMaterialization.MemberState.VERIFIED
                } &&
                !artifacts.effectsPerformed
        ) {
            "BGE-M3 runtime requires one exact no-effect artifact observation"
        }

        require(
            (
                environment.state ==
                    "REUSED_VERIFIED" ||
                    environment.state ==
                    "CREATED_VERIFIED"
            ) &&
                Path.of(
                    environment.environmentRoot,
                )
                    .normalize() ==
                expectedEnvironmentRoot &&
                Path.of(
                    environment.pythonPath,
                )
                    .normalize() ==
                expectedPython &&
                environment.requirementsSha256 ==
                VexLifeBgeM3MacPythonEnvironment.RequirementsSha256 &&
                environment.pythonVersion ==
                VexLifeBgeM3MacPythonEnvironment.PythonVersion &&
                environment.numpyVersion ==
                VexLifeBgeM3MacPythonEnvironment.NumPyVersion &&
                environment.onnxRuntimeVersion ==
                VexLifeBgeM3MacPythonEnvironment.OnnxRuntimeVersion &&
                environment.transformersVersion ==
                VexLifeBgeM3MacPythonEnvironment.TransformersVersion &&
                environment.huggingFaceHubVersion ==
                VexLifeBgeM3MacPythonEnvironment.HuggingFaceHubVersion &&
                environment.environmentFingerprintSha256.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                )
        ) {
            "BGE-M3 runtime requires one exact qualified Mac Python environment"
        }

        return Binding(
            productHome =
                home.toString(),
            modelRoot =
                expectedModelRoot.toString(),
            artifactRef =
                artifacts.artifactRef,
            environmentRoot =
                expectedEnvironmentRoot.toString(),
            pythonPath =
                expectedPython.toString(),
            environmentFingerprintSha256 =
                environment.environmentFingerprintSha256,
            representationBindingRef =
                VexLifeBgeM3DenseIdentity.RepresentationBindingRef,
        )
    }

    fun start(
        binding: Binding,
        launcher: Launcher = SystemLauncher,
    ): Session {
        validateBinding(
            binding,
        )

        val worker =
            launcher.launch(
                binding,
            )

        try {
            require(
                worker.ready.dimension ==
                    VexLifeBgeM3DenseIdentity.Dimension &&
                    VexLifeBgeM3DenseIdentity.ExecutionProvider in
                    worker.ready.providers &&
                    worker.ready.inputNames.toSet() ==
                    setOf(
                        "input_ids",
                        "attention_mask",
                    )
            ) {
                "BGE-M3 worker readiness differs from qualified runtime shape"
            }

            return Session(
                binding =
                    binding,
                worker =
                    worker,
            )
        } catch (
            error: Exception,
        ) {
            runCatching {
                worker.close()
            }
            throw error
        }
    }

    class Session internal constructor(
        val binding: Binding,
        private val worker: Worker,
    ) :
        VexLifeVolatileDenseSemanticBackend.Encoder,
        AutoCloseable {
        override val representationBindingRef: String =
            binding.representationBindingRef

        override val dimension: Int =
            VexLifeBgeM3DenseIdentity.Dimension

        private val requestSequence =
            AtomicLong(
                0,
            )

        private val lock =
            Any()

        private var closed =
            false

        override fun encodeSources(
            texts: List<String>,
        ): List<FloatArray> =
            synchronized(
                lock,
            ) {
                requireOpen()

                if (
                    texts.isEmpty()
                ) {
                    return@synchronized emptyList()
                }

                require(
                    texts.all(
                        String::isNotEmpty,
                    )
                ) {
                    "BGE-M3 source encoding requires non-empty canonical source text"
                }

                texts
                    .chunked(
                        MaximumTextsPerWorkerRequest,
                    )
                    .flatMap {
                        batch ->
                        encodeBatch(
                            batch,
                        )
                    }
            }

        override fun encodeQuery(
            queryTerms: List<String>,
        ): FloatArray =
            synchronized(
                lock,
            ) {
                requireOpen()

                encodeBatch(
                    listOf(
                        VexLifeBgeM3DenseIdentity.queryText(
                            queryTerms,
                        ),
                    ),
                )
                    .single()
            }

        override fun close() {
            synchronized(
                lock,
            ) {
                if (
                    closed
                ) {
                    return
                }

                closed =
                    true
                worker.close()
            }
        }

        private fun encodeBatch(
            texts: List<String>,
        ): List<FloatArray> {
            val requestRef =
                "request.vexlife-prod.bge-m3-dense." +
                    requestSequence
                        .incrementAndGet()

            val vectors =
                worker.encode(
                    requestRef =
                        requestRef,
                    texts =
                        texts,
                )

            require(
                vectors.size ==
                    texts.size
            ) {
                "BGE-M3 worker returned a vector count different from its request"
            }

            vectors.forEachIndexed {
                index,
                vector,
                ->
                require(
                    vector.size ==
                        dimension &&
                        vector.all {
                            value ->
                            !value.isNaN() &&
                                !value.isInfinite()
                        }
                ) {
                    "BGE-M3 worker returned an invalid vector at index $index"
                }
            }

            return vectors.map(
                FloatArray::copyOf,
            )
        }

        private fun requireOpen() {
            require(
                !closed
            ) {
                "BGE-M3 encoder session is closed"
            }
        }
    }

    private class SystemWorker(
        private val binding: Binding,
    ) :
        Worker {
        private val process: Process

        private val stdout: BufferedReader

        private val stdin: BufferedWriter

        private val readExecutor =
            Executors.newSingleThreadExecutor {
                runnable ->
                Thread(
                    runnable,
                    "vexlife-bge-worker-read",
                )
                    .apply {
                        isDaemon =
                            true
                    }
            }

        private val stderrText =
            StringBuilder()

        private val stderrThread: Thread

        private var closed =
            false

        override val ready: Ready

        init {
            val builder =
                ProcessBuilder(
                    binding.pythonPath,
                    "-u",
                    "-c",
                    WorkerPython,
                    binding.modelRoot,
                )

            builder
                .environment()
                .putAll(
                    OfflineEnvironment,
                )

            process =
                builder.start()
            stdout =
                process.inputStream
                    .bufferedReader(
                        StandardCharsets.UTF_8,
                    )
            stdin =
                process.outputStream
                    .bufferedWriter(
                        StandardCharsets.UTF_8,
                    )
            stderrThread =
                thread(
                    start =
                        true,
                    isDaemon =
                        true,
                    name =
                        "vexlife-bge-worker-stderr",
                ) {
                    process.errorStream
                        .bufferedReader(
                            StandardCharsets.UTF_8,
                        )
                        .useLines {
                            lines ->
                            lines.forEach {
                                line ->
                                synchronized(
                                    stderrText,
                                ) {
                                    if (
                                        stderrText.length <
                                            16_384
                                    ) {
                                        stderrText.appendLine(
                                            line,
                                        )
                                    }
                                }
                            }
                        }
                }

            try {
                val payload =
                    readObject(
                        StartupTimeoutMs,
                    )
                require(
                    payload.string(
                        "schemaVersion",
                    ) ==
                        WorkerResponseSchema &&
                        payload.string(
                            "kind",
                        ) ==
                        "READY"
                ) {
                    "BGE-M3 worker did not return READY"
                }

                val pid =
                    payload
                        .long(
                            "pid",
                        )
                require(
                    pid ==
                        process.pid()
                ) {
                    "BGE-M3 worker READY pid does not match owned child process"
                }

                ready =
                    Ready(
                        pid =
                            pid,
                        dimension =
                            payload.int(
                                "dimension",
                            ),
                        providers =
                            payload.strings(
                                "providers",
                            ),
                        inputNames =
                            payload.strings(
                                "inputNames",
                            ),
                    )
            } catch (
                error: Exception,
            ) {
                failWorker()
                throw error
            }
        }

        override fun encode(
            requestRef: String,
            texts: List<String>,
        ): List<FloatArray> {
            require(
                !closed &&
                    requestRef.matches(
                        Regex(
                            "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                        ),
                    ) &&
                    texts.isNotEmpty() &&
                    texts.size <=
                    MaximumTextsPerWorkerRequest &&
                    texts.all(
                        String::isNotEmpty,
                    )
            ) {
                "BGE-M3 worker encode request is invalid"
            }

            val request =
                JsonObject(
                    mapOf(
                        "schemaVersion" to
                            JsonPrimitive(
                                WorkerRequestSchema,
                            ),
                        "kind" to
                            JsonPrimitive(
                                "ENCODE",
                            ),
                        "requestRef" to
                            JsonPrimitive(
                                requestRef,
                            ),
                        "texts" to
                            JsonArray(
                                texts.map(
                                    ::JsonPrimitive,
                                ),
                            ),
                    ),
                )

            try {
                writeObject(
                    request,
                )

                val response =
                    readObject(
                        EncodeTimeoutMs,
                    )
                require(
                    response.string(
                        "schemaVersion",
                    ) ==
                        WorkerResponseSchema &&
                        response.string(
                            "kind",
                        ) ==
                        "ENCODED" &&
                        response.string(
                            "requestRef",
                        ) ==
                        requestRef &&
                        response.long(
                            "pid",
                        ) ==
                        ready.pid
                ) {
                    "BGE-M3 worker response identity mismatch"
                }

                return requireNotNull(
                    response[
                        "vectors"
                    ],
                )
                    .jsonArray
                    .map {
                        vector ->
                        vector
                            .jsonArray
                            .map {
                                value ->
                                value
                                    .jsonPrimitive
                                    .content
                                    .toFloat()
                            }
                            .toFloatArray()
                    }
            } catch (
                error: Exception,
            ) {
                failWorker()
                throw error
            }
        }

        override fun close() {
            if (
                closed
            ) {
                return
            }

            closed =
                true

            try {
                if (
                    process.isAlive
                ) {
                    writeObject(
                        JsonObject(
                            mapOf(
                                "schemaVersion" to
                                    JsonPrimitive(
                                        WorkerRequestSchema,
                                    ),
                                "kind" to
                                    JsonPrimitive(
                                        "CLOSE",
                                    ),
                                "requestRef" to
                                    JsonPrimitive(
                                        "request.vexlife-prod.bge-m3-dense.close",
                                    ),
                            ),
                        ),
                    )

                    val response =
                        readObject(
                            CloseTimeoutMs,
                        )
                    require(
                        response.string(
                            "schemaVersion",
                        ) ==
                            WorkerResponseSchema &&
                            response.string(
                                "kind",
                            ) ==
                            "CLOSED" &&
                            response.long(
                                "pid",
                            ) ==
                            ready.pid
                    ) {
                        "BGE-M3 worker did not close with exact process identity"
                    }

                    val completed =
                        process.waitFor(
                            CloseTimeoutMs,
                            TimeUnit.MILLISECONDS,
                        )
                    require(
                        completed &&
                            process.exitValue() ==
                            0
                    ) {
                        "BGE-M3 worker did not exit cleanly"
                    }
                }
            } finally {
                if (
                    process.isAlive
                ) {
                    process.destroyForcibly()
                    process.waitFor()
                }

                runCatching {
                    stdin.close()
                }
                runCatching {
                    stdout.close()
                }
                readExecutor.shutdownNow()
                stderrThread.join(
                    1_000,
                )
            }
        }

        private fun writeObject(
            value: JsonObject,
        ) {
            stdin.write(
                JsonCodec.encodeToString(
                    JsonObject.serializer(),
                    value,
                ),
            )
            stdin.newLine()
            stdin.flush()
        }

        private fun readObject(
            timeoutMs: Long,
        ): JsonObject {
            val future =
                readExecutor.submit<String?> {
                    stdout.readLine()
                }

            val line =
                try {
                    future.get(
                        timeoutMs,
                        TimeUnit.MILLISECONDS,
                    )
                } catch (
                    error: Exception,
                ) {
                    future.cancel(
                        true,
                    )
                    throw IllegalStateException(
                        "BGE-M3 worker response timed out: " +
                            stderrSnapshot(),
                        error,
                    )
                }

            require(
                line !=
                    null
            ) {
                "BGE-M3 worker closed before response: " +
                    stderrSnapshot()
            }

            return JsonCodec
                .parseToJsonElement(
                    line,
                )
                .jsonObject
        }

        private fun failWorker() {
            closed =
                true

            if (
                process.isAlive
            ) {
                process.destroyForcibly()
                process.waitFor()
            }

            runCatching {
                stdin.close()
            }
            runCatching {
                stdout.close()
            }
            readExecutor.shutdownNow()
        }

        private fun stderrSnapshot(): String =
            synchronized(
                stderrText,
            ) {
                stderrText
                    .toString()
                    .takeLast(
                        4_000,
                    )
            }
    }

    private fun validateBinding(
        binding: Binding,
    ) {
        require(
            Path.of(
                binding.productHome,
            )
                .isAbsolute &&
                Path.of(
                    binding.modelRoot,
                )
                    .isAbsolute &&
                Path.of(
                    binding.environmentRoot,
                )
                    .isAbsolute &&
                Path.of(
                    binding.pythonPath,
                )
                    .isAbsolute &&
                binding.artifactRef ==
                    VexLifeBgeM3ArtifactMaterialization.ArtifactRef &&
                binding.environmentFingerprintSha256.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ) &&
                binding.representationBindingRef ==
                    VexLifeBgeM3DenseIdentity.RepresentationBindingRef
        ) {
            "BGE-M3 runtime binding is invalid"
        }
    }

    private fun JsonObject.string(
        field: String,
    ): String =
        requireNotNull(
            this[
                field
            ],
        )
            .jsonPrimitive
            .content

    private fun JsonObject.long(
        field: String,
    ): Long =
        string(
            field,
        )
            .toLong()

    private fun JsonObject.int(
        field: String,
    ): Int =
        string(
            field,
        )
            .toInt()

    private fun JsonObject.strings(
        field: String,
    ): List<String> =
        requireNotNull(
            this[
                field
            ],
        )
            .jsonArray
            .map {
                value ->
                requireNotNull(
                    value
                        .jsonPrimitive
                        .contentOrNull,
                )
            }

    private val JsonCodec =
        Json {
            ignoreUnknownKeys =
                false
            isLenient =
                false
        }

    private val OfflineEnvironment =
        mapOf(
            "HF_HUB_OFFLINE" to
                "1",
            "TRANSFORMERS_OFFLINE" to
                "1",
            "HF_DATASETS_OFFLINE" to
                "1",
            "HF_HUB_DISABLE_TELEMETRY" to
                "1",
            "DISABLE_TELEMETRY" to
                "1",
            "PYTHONNOUSERSITE" to
                "1",
            "PYTHONDONTWRITEBYTECODE" to
                "1",
            "TOKENIZERS_PARALLELISM" to
                "false",
        )

    private const val WorkerPython =
        """
import json
import os
import sys
from pathlib import Path

import numpy as np
import onnxruntime as ort
from transformers import AutoTokenizer

REQUEST_SCHEMA = "vexlife-prod.bge-m3-dense-worker-request/v1"
RESPONSE_SCHEMA = "vexlife-prod.bge-m3-dense-worker-response/v1"
DIMENSION = 1024
MAX_LENGTH = 512
BATCH_SIZE = 8

root = Path(sys.argv[1]).resolve()
onnx_root = root / "onnx"

tokenizer = AutoTokenizer.from_pretrained(
    onnx_root,
    local_files_only=True,
    use_fast=True,
)
session = ort.InferenceSession(
    str(onnx_root / "model.onnx"),
    providers=["CPUExecutionProvider"],
)

print(json.dumps({
    "schemaVersion": RESPONSE_SCHEMA,
    "kind": "READY",
    "pid": os.getpid(),
    "dimension": DIMENSION,
    "providers": session.get_providers(),
    "inputNames": [item.name for item in session.get_inputs()],
}), flush=True)

def normalize(value):
    norm = np.linalg.norm(value, axis=1, keepdims=True)
    if np.any(~np.isfinite(norm)) or np.any(norm == 0):
        raise RuntimeError("invalid BGE-M3 vector magnitude")
    return value / norm

def embed(texts):
    batches = []
    for start in range(0, len(texts), BATCH_SIZE):
        batch = texts[start:start + BATCH_SIZE]
        encoded = tokenizer(
            batch,
            max_length=MAX_LENGTH,
            padding=True,
            truncation=True,
            return_tensors="np",
        )
        names = {item.name for item in session.get_inputs()}
        feeds = {}
        for key, value in encoded.items():
            if key in names:
                feeds[key] = value.astype(np.int64, copy=False)
        missing = names.difference(feeds)
        if missing:
            raise RuntimeError("missing ONNX inputs: " + repr(sorted(missing)))
        output = session.run(None, feeds)[0]
        dense = output[:, 0, :]
        if dense.shape[1] != DIMENSION:
            raise RuntimeError("unexpected BGE-M3 dimension: " + repr(dense.shape))
        batches.append(normalize(dense).astype(np.float32, copy=False))
    return np.concatenate(batches, axis=0)

for raw in sys.stdin:
    raw = raw.strip()
    if not raw:
        continue

    request = json.loads(raw)
    if request.get("schemaVersion") != REQUEST_SCHEMA:
        raise RuntimeError("BGE-M3 request schema mismatch")

    kind = request.get("kind")
    if kind == "CLOSE":
        print(json.dumps({
            "schemaVersion": RESPONSE_SCHEMA,
            "kind": "CLOSED",
            "pid": os.getpid(),
        }), flush=True)
        break

    if kind != "ENCODE":
        raise RuntimeError("unsupported BGE-M3 request kind")

    texts = request.get("texts")
    if not isinstance(texts, list) or not texts or len(texts) > 64:
        raise RuntimeError("BGE-M3 text batch outside bound")
    if any(not isinstance(text, str) or not text for text in texts):
        raise RuntimeError("BGE-M3 source/query text must be non-empty strings")

    vectors = embed(texts)
    print(json.dumps({
        "schemaVersion": RESPONSE_SCHEMA,
        "kind": "ENCODED",
        "requestRef": request.get("requestRef"),
        "pid": os.getpid(),
        "vectors": vectors.tolist(),
    }, ensure_ascii=False), flush=True)
"""

}

// [VXG RealForever]
