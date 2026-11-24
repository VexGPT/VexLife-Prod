package vexlife.harness

import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Collections
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.companion.DirectCompanionConnection
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableLong(key: String, value: Long?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}

/**
 * DO-NOT-MERGE Response Readiness proof harness.
 * Ordinary jvmTest is a no-op unless the explicit proof environment is set.
 */
class ResponseReadinessResourcePressureTest {
    @Test
    fun cacheOneVsCacheTwoUsesTheSameProviderAndRecordsPhysicalPressure() {
        if (System.getenv(ENABLE) != "1") return

        val repo = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        val home = Path.of(needEnv(HOME)).toAbsolutePath().normalize()
        val out = Path.of(needEnv(OUT)).toAbsolutePath().normalize()
        require(!Files.exists(out)) { "result root already exists: " + out }
        Files.createDirectories(out)
        proveMembrane(repo)

        val receipt = requireNotNull(FileHereticRuntimeReceiptStore(home).read()) {
            "current Product runtime receipt is required only for read-only model/Python custody"
        }
        require(receipt.modelRef == HereticModelCatalog.Models.Qwen4B.modelRef)
        require(receipt.mlxLmVersion == "0.32.0")
        val canonicalAliveAtStart = alive(receipt.runtime.pid)
        val totalMemoryAtStart = totalMemoryBytes()

        val completed = mutableListOf<String>()
        var failure: Throwable? = null
        try {
            for (spec in SPECS) {
                runOne(repo, home, out, receipt, spec)
                completed += spec.ref
            }
        } catch (error: Throwable) {
            failure = error
        } finally {
            Files.writeString(
                out.resolve("index.json"),
                buildJsonObject {
                    put("schemaVersion", "vexlife-prod.response-readiness-resource-pressure-index/v1")
                    put("productHead", PRODUCT_HEAD)
                    put("productTree", PRODUCT_TREE)
                    put("canonicalRuntimePid", receipt.runtime.pid)
                    put("canonicalRuntimeAliveAtStart", canonicalAliveAtStart)
                    putNullableLong("totalMemoryBytesAtStart", totalMemoryAtStart)
                    put("completed", buildJsonArray { completed.forEach { add(JsonPrimitive(it)) } })
                    put(
                        "failure",
                        failure?.let {
                            buildJsonObject {
                                put("class", it.javaClass.name)
                                put("message", it.message ?: "")
                            }
                        } ?: JsonNull,
                    )
                }.toString() + "\n",
            )
        }
        failure?.let { throw it }
    }

    private fun runOne(
        repo: Path,
        home: Path,
        out: Path,
        receipt: HereticPersistentRuntime.Receipt,
        spec: Spec,
    ) {
        val root = out.resolve(spec.ref)
        val logs = root.resolve("child")
        val hub = root.resolve("hf-cache")
        Files.createDirectories(logs)
        Files.createDirectories(hub)

        val binding =
            HereticPersistentRuntime.Binding(
                schemaVersion = HereticPersistentRuntime.BindingSchema,
                authorityRef = "authority.vexlife-prod.response-readiness.resource-pressure",
                modelPath = receipt.modelPath,
                pythonPath = receipt.pythonPath,
                pythonEnvironmentRoot = receipt.pythonEnvironmentRoot,
                implementationIdentity = HereticPersistentRuntime.currentImplementationIdentity(),
                model = HereticModelCatalog.Models.Qwen4B,
                promptCacheEntryLimit = spec.cache,
            )

        val port = ServerSocket(0).use { it.localPort }
        require(port != URI.create(receipt.endpoint).port)
        val args = HereticPersistentRuntime.serverArgs(binding).toMutableList()
        val portIndex = args.indexOf("--port")
        require(portIndex >= 0 && portIndex + 1 < args.size)
        args[portIndex + 1] = port.toString()

        val stdout = logs.resolve("stdout.log")
        val stderr = logs.resolve("stderr.log")
        val child =
            ProcessBuilder(listOf(binding.pythonPath) + args)
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile())
                .also { it.environment()["HF_HUB_CACHE"] = hub.toString() }
                .start()
        val endpoint = "http://127.0.0.1:" + port
        val phase = AtomicReference("CHILD_STARTING")
        var adapterExit: Int? = null
        var qualification: HereticRuntimeQualification? = null
        var sampler: Sampler? = null
        var failure: Throwable? = null

        try {
            waitHealthy(endpoint, child)
            qualification = qualify(endpoint, binding, receipt, spec.ref)
            phase.set("P0_QUALIFIED")

            val evidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef =
                        "receipt.vexlife-prod.response-readiness.resource-pressure." +
                            spec.ref + "." + UUID.randomUUID().toString().lowercase(),
                    runtimeState = HereticRuntimeState.Qualified,
                    modelRef = receipt.modelRef,
                    modelProfileRef = receipt.modelProfileRef,
                    artifactRef = receipt.artifactRef,
                    artifactContentSetSha256 = receipt.artifactContentSetSha256,
                    runtimeAdapterRef = receipt.runtimeAdapterRef,
                    runtimeClass = receipt.runtimeClass,
                    endpoint = endpoint,
                    requestModel = receipt.requestModel,
                    qualification = requireNotNull(qualification),
                )
            val live =
                ContinuityPressureAdapter.LiveRuntime(
                    evidence = evidence,
                    observe = {
                        require(child.isAlive) { "isolated child exited during run" }
                        evidence
                    },
                    transport = EndpointTransport(endpoint, phase),
                )

            sampler = Sampler(child.pid(), receipt.runtime.pid, phase).also { it.start() }
            phase.set("P2_BEFORE_RUN")
            adapterExit =
                ContinuityPressureAdapter.run(
                    ContinuityPressureAdapter.Config(
                        stage = ContinuityPressureAdapter.Stage.quick,
                        scenarioRef = ContinuityPressureAdapter.CanonicalScenarioRef,
                        scenarioFile = repo.resolve(SCENARIO),
                        resultDir = root.resolve("continuity-pressure"),
                        strategy = ContinuityPressureAdapter.Strategy.EFFECTIVE_SELECTION_BUDGET,
                        effectiveInputTokenBudget = 80_000,
                        sets = spec.sets,
                        turnsPerSet = spec.turns,
                        sourceSha = PRODUCT_HEAD,
                        sourceClean = true,
                        keepScratch = false,
                        runtimeHome = home,
                        experimentWindow = true,
                        runtimeStartAtBind = false,
                        payloadScale = spec.scale,
                        conversationContextProjectionEnabled = false,
                    ),
                ) { live }
            phase.set("P4_AFTER_RUN")
            Thread.sleep(500)
            require(adapterExit == 0) {
                "continuity-pressure " + spec.ref + " exit=" + adapterExit
            }
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            sampler?.stop()
            stop(child)
            val samples = sampler?.snapshot().orEmpty().toMutableList()
            samples += pressure("P5_AFTER_CLEANUP", child.pid(), receipt.runtime.pid)
            hub.toFile().deleteRecursively()
            Files.writeString(
                root.resolve("pressure.csv"),
                "atEpochMillis,phase,reclaimableBytes,childRssBytes,canonicalRssBytes,childAlive,canonicalAlive\n" +
                    samples.joinToString("\n") { it.csv() } + "\n",
            )
            Files.writeString(
                root.resolve("resource-pressure.json"),
                summary(spec, receipt, child, endpoint, qualification, adapterExit, samples, failure).toString() + "\n",
            )
        }
    }

    private fun summary(
        spec: Spec,
        receipt: HereticPersistentRuntime.Receipt,
        child: Process,
        endpoint: String,
        qualification: HereticRuntimeQualification?,
        adapterExit: Int?,
        samples: List<Sample>,
        failure: Throwable?,
    ) =
        buildJsonObject {
            put("schemaVersion", "vexlife-prod.response-readiness-resource-pressure-proof/v1")
            put("productHead", PRODUCT_HEAD)
            put("productTree", PRODUCT_TREE)
            put("specRef", spec.ref)
            put("cacheEntries", spec.cache)
            put("sets", spec.sets)
            put("turnsPerSet", spec.turns)
            put("payloadScale", spec.scale)
            put("childPid", child.pid())
            put("endpoint", endpoint)
            put("canonicalRuntimePid", receipt.runtime.pid)
            put("canonicalRuntimeAlive", alive(receipt.runtime.pid))
            putNullableLong("totalMemoryBytes", totalMemoryBytes())
            put("qualificationPerformed", qualification != null)
            if (adapterExit == null) put("adapterExit", JsonNull) else put("adapterExit", adapterExit)
            put("childStopped", !child.isAlive)
            putNullableLong("minimumReclaimableBytes", samples.mapNotNull { it.reclaimable }.minOrNull())
            putNullableLong("maximumChildRssBytes", samples.mapNotNull { it.childRss }.maxOrNull())
            putNullableLong(
                "maximumCanonicalRssBytes",
                samples.mapNotNull { it.canonicalRss }.maxOrNull(),
            )
            put("reclaimableMethod", "vm_stat: free + inactive + speculative + purgeable pages")
            put("rssMethod", "ps -o rss= -p <pid>")
            put("sampleIntervalMillis", SAMPLE_MS)
            put(
                "failure",
                failure?.let {
                    buildJsonObject {
                        put("class", it.javaClass.name)
                        put("message", it.message ?: "")
                    }
                } ?: JsonNull,
            )
        }

    private fun qualify(
        endpoint: String,
        binding: HereticPersistentRuntime.Binding,
        receipt: HereticPersistentRuntime.Receipt,
        ref: String,
    ): HereticRuntimeQualification {
        val health = json(get(endpoint + "/health", 5))
        require(health["status"]?.jsonPrimitive?.contentOrNull == "ok")

        val models = json(get(endpoint + "/v1/models", 5))
        val expected = Path.of(binding.modelPath).toRealPath()
        val exact =
            models["data"]?.jsonArray.orEmpty()
                .mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
                .count { runCatching { Path.of(it).toRealPath() == expected }.getOrDefault(false) }
        require(exact == 1) { "exact model is not exposed exactly once" }

        val body =
            buildJsonObject {
                put("model", receipt.requestModel)
                put(
                    "messages",
                    buildJsonArray {
                        add(buildJsonObject { put("role", "user"); put("content", "Reply with OK.") })
                    },
                )
                put("max_tokens", 1)
                put("temperature", 0)
                put("stream", false)
                put("chat_template_kwargs", buildJsonObject { put("enable_thinking", false) })
            }.toString()
        val completion =
            json(
                HttpRequest.newBuilder(URI.create(endpoint + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(180))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build(),
            )
        require(completion["model"]?.jsonPrimitive?.contentOrNull == receipt.requestModel)
        val content =
            completion["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
        require(!content.isNullOrBlank())
        return HereticRuntimeQualification.qualified(
            loadResultRef = "load.vexlife-prod.response-readiness.resource-pressure." + ref,
        )
    }

    private fun waitHealthy(endpoint: String, child: Process) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
        while (System.nanoTime() < deadline) {
            require(child.isAlive) { "isolated child exited before health" }
            val ok =
                runCatching {
                    val response = HTTP.send(get(endpoint + "/health", 2), HttpResponse.BodyHandlers.ofString())
                    response.statusCode() == 200 &&
                        Json.parseToJsonElement(response.body()).jsonObject["status"]
                            ?.jsonPrimitive?.contentOrNull == "ok"
                }.getOrDefault(false)
            if (ok) return
            Thread.sleep(250)
        }
        error("isolated child health timeout")
    }

    private fun get(url: String, seconds: Long) =
        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(seconds)).GET().build()

    private fun json(request: HttpRequest) =
        HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).let { response ->
            require(response.statusCode() == 200) {
                "HTTP " + response.statusCode() + ": " + response.body().take(512)
            }
            Json.parseToJsonElement(response.body()).jsonObject
        }

    private fun proveMembrane(repo: Path) {
        require(git(repo, "rev-parse", PRODUCT_HEAD + "^{tree}").stdout.trim() == PRODUCT_TREE)
        git(repo, "merge-base", "--is-ancestor", PRODUCT_HEAD, "HEAD")
        val changed =
            git(repo, "diff", "--name-only", PRODUCT_HEAD + "...HEAD")
                .stdout.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        require(changed == setOf(HARNESS)) { "unexpected proof paths: " + changed }
        require(git(repo, "status", "--porcelain=v1", "--untracked-files=no").stdout.isBlank())
    }

    private data class Spec(val ref: String, val cache: Int, val sets: Int, val turns: Int, val scale: Int)

    private data class Sample(
        val at: Long,
        val phase: String,
        val reclaimable: Long?,
        val childRss: Long?,
        val canonicalRss: Long?,
        val childAlive: Boolean,
        val canonicalAlive: Boolean,
    ) {
        fun csv() =
            listOf(
                at.toString(),
                phase,
                reclaimable?.toString().orEmpty(),
                childRss?.toString().orEmpty(),
                canonicalRss?.toString().orEmpty(),
                childAlive.toString(),
                canonicalAlive.toString(),
            ).joinToString(",")
    }

    private class Sampler(
        private val child: Long,
        private val canonical: Long,
        private val phase: AtomicReference<String>,
    ) {
        private val running = AtomicBoolean()
        private val samples = Collections.synchronizedList(mutableListOf<Sample>())
        private var thread: Thread? = null

        fun start() {
            require(running.compareAndSet(false, true))
            thread =
                Thread(
                    {
                        while (running.get()) {
                            samples += pressure(phase.get(), child, canonical)
                            try {
                                Thread.sleep(SAMPLE_MS)
                            } catch (error: InterruptedException) {
                                Thread.currentThread().interrupt()
                                break
                            }
                        }
                    },
                    "vexlife-response-readiness-pressure",
                ).also { it.isDaemon = true; it.start() }
        }

        fun stop() {
            running.set(false)
            thread?.join(5_000)
        }

        fun snapshot(): List<Sample> = synchronized(samples) { samples.toList() }
    }

    private class EndpointTransport(
        private val endpoint: String,
        private val phase: AtomicReference<String>,
    ) : DirectCompanionConnection.StreamingTransport {
        private val call = AtomicInteger()

        override fun post(c: DirectCompanionConnection.HttpCall): DirectCompanionConnection.HttpResult =
            around { DirectCompanionConnection.SystemTransport.post(rewrite(c)) }

        override fun postStreaming(
            c: DirectCompanionConnection.HttpCall,
            onLine: (String) -> Unit,
        ): DirectCompanionConnection.StreamingHttpResult =
            around { DirectCompanionConnection.SystemTransport.postStreaming(rewrite(c), onLine) }

        private fun <T> around(block: () -> T): T {
            val ordinal = call.incrementAndGet()
            phase.set("P3_PROVIDER_CALL_" + ordinal)
            return try {
                block()
            } finally {
                phase.set("P1_AFTER_PROVIDER_CALL_" + ordinal)
            }
        }

        private fun rewrite(c: DirectCompanionConnection.HttpCall): DirectCompanionConnection.HttpCall {
            val suffix = c.uri.rawPath + (c.uri.rawQuery?.let { "?" + it } ?: "")
            return c.copy(uri = URI.create(endpoint + suffix))
        }
    }

    companion object {
        private const val ENABLE = "VEXLIFE_RESPONSE_READINESS_RESOURCE_PRESSURE"
        private const val HOME = "VEXLIFE_RUNTIME_HOME"
        private const val OUT = "VEXLIFE_RESOURCE_PRESSURE_RESULT_ROOT"
        private const val SCENARIO = "tools/continuity-pressure/scenarios/depth-continuity-v1.json"
        private const val HARNESS =
            "src/jvmTest/kotlin/vexlife/harness/ResponseReadinessResourcePressureTest.kt"
        private const val PRODUCT_HEAD = "3a47bac8618c99214cfdb3d4396e626bc95b96ef"
        private const val PRODUCT_TREE = "733a514e1999ef5722e494160edc8894a8126458"
        private const val SAMPLE_MS = 250L
        private val HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
        private val SPECS =
            listOf(
                Spec("small-cache1", 1, 1, 2, 1),
                Spec("small-cache2", 2, 1, 2, 1),
                Spec("larger-cache1", 1, 2, 2, 8),
                Spec("larger-cache2", 2, 2, 2, 8),
            )

        private fun needEnv(key: String) =
            requireNotNull(System.getenv(key)?.takeIf { it.isNotBlank() }) { key + " is required" }

        private fun pressure(phase: String, child: Long, canonical: Long) =
            Sample(
                System.currentTimeMillis(),
                phase,
                reclaimable(),
                rss(child),
                rss(canonical),
                alive(child),
                alive(canonical),
            )

        private fun totalMemoryBytes(): Long? {
            val result = command("/usr/sbin/sysctl", listOf("-n", "hw.memsize"))
            if (result.status != 0) return null
            return result.stdout.trim().toLongOrNull()
        }

        private fun reclaimable(): Long? {
            val result = command("/usr/bin/vm_stat", emptyList())
            if (result.status != 0) return null
            val lines = result.stdout.lineSequence().toList()
            val page =
                lines.firstOrNull()?.let {
                    Regex("page size of ([0-9]+) bytes").find(it)?.groupValues?.getOrNull(1)?.toLongOrNull()
                } ?: return null
            val counts =
                lines.drop(1).mapNotNull {
                    val name = it.substringBefore(":", "").trim()
                    val value = it.substringAfter(":", "").trim().trimEnd('.').toLongOrNull()
                    if (name.isEmpty() || value == null) null else name to value
                }.toMap()
            return listOf("Pages free", "Pages inactive", "Pages speculative", "Pages purgeable")
                .sumOf { counts[it] ?: 0L } * page
        }

        private fun rss(pid: Long): Long? {
            if (!alive(pid)) return null
            val result = command("/bin/ps", listOf("-o", "rss=", "-p", pid.toString()))
            if (result.status != 0) return null
            return result.stdout.trim().toLongOrNull()?.times(1024L)
        }

        private fun alive(pid: Long) = ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

        private fun stop(process: Process) {
            if (!process.isAlive) return
            process.destroy()
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                process.waitFor(10, TimeUnit.SECONDS)
            }
        }

        private data class Cmd(val status: Int, val stdout: String, val stderr: String)

        private fun git(repo: Path, vararg args: String): Cmd {
            val result = command("/usr/bin/git", listOf("-C", repo.toString()) + args, 30_000)
            require(result.status == 0) {
                "git " + args.joinToString(" ") + " failed: " + result.stderr.take(1_000)
            }
            return result
        }

        private fun command(program: String, args: List<String>, timeoutMs: Long = 5_000): Cmd {
            val process = ProcessBuilder(listOf(program) + args).start()
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                process.waitFor(5, TimeUnit.SECONDS)
            }
            return Cmd(
                if (completed) process.exitValue() else -1,
                process.inputStream.bufferedReader().readText(),
                process.errorStream.bufferedReader().readText(),
            )
        }
    }
}

// [VXG RealForever]
