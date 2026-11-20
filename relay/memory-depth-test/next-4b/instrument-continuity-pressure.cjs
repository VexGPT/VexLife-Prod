'use strict';

const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

const PATCH_REF = 'memory-depth.progress-checkpoint.v1';
const EXPECTED_SOURCE_GIT_BLOB = 'cc16bb92147b340fc9860a245dab7c474764cc0e';
const PROGRESS_SCHEMA = 'vexlife.continuity-pressure-progress/v1';

const CALL_ANCHOR =
  '                    peakBytes = maxOf(peakBytes, directoryBytes(home))';
const FUNCTION_ANCHOR =
  '    private fun writeResult(\n';

function count(haystack, needle) {
  return haystack.split(needle).length - 1;
}

function gitBlobSha(buffer) {
  const header = Buffer.concat([Buffer.from(`blob ${buffer.length}`, 'utf8'), Buffer.from([0])]);
  return crypto.createHash('sha1').update(header).update(buffer).digest('hex');
}

function sha256(buffer) {
  return crypto.createHash('sha256').update(buffer).digest('hex');
}

function checkpointFunction() {
  return `
    /**
     * Attempt-local Memory Depth evidence checkpoint.
     *
     * This does not change Product/Conversation/Working Continuity semantics. It serializes only
     * already-completed observed turns so an outer bounded carrier can preserve useful evidence
     * when a later turn exceeds the semantic experiment window.
     */
    private fun writeProgressCheckpoint(
        config: Config,
        scenario: Scenario,
        scenarioSha: String,
        experimentRef: String,
        budget: Int?,
        observed: List<Observed>,
        peakBytes: Long,
        provider: Provider,
    ) {
        val turns = turnsJson(observed, budget)
        val checkpoint =
            buildJsonObject {
                put("schemaVersion", "${PROGRESS_SCHEMA}")
                put("experimentRef", experimentRef)
                put("scenarioRef", scenario.scenarioRef)
                put("scenarioSha256", scenarioSha)
                put("sourceSha", config.sourceSha)
                put("sourceTreeClean", config.sourceClean)
                put("adapterRef", AdapterRef)
                put(
                    "executionClass",
                    if (provider.live) "LIVE_MODEL" else "DETERMINISTIC_PROJECTION",
                )
                put("status", "IN_PROGRESS")
                put("completedTurnCount", observed.size)
                val last = observed.lastOrNull()
                if (last == null) {
                    put("lastCompletedOrdinal", JsonNull)
                } else {
                    put("lastCompletedOrdinal", last.ordinal)
                }
                put("peakScratchBytesSoFar", peakBytes)
                put(
                    "runtime",
                    buildJsonObject {
                        put("borrowedLiveRuntime", provider.live)
                        put("modelRef", if (provider.live) provider.evidence.modelRef else null)
                        put(
                            "modelProfileRef",
                            if (provider.live) provider.evidence.modelProfileRef else null,
                        )
                        put(
                            "runtimeReceiptRef",
                            if (provider.live) provider.evidence.runtimeReceiptRef else null,
                        )
                        put("artifactRef", if (provider.live) provider.evidence.artifactRef else null)
                        put(
                            "artifactContentSetSha256",
                            if (provider.live) {
                                provider.evidence.artifactContentSetSha256
                            } else {
                                null
                            },
                        )
                        put(
                            "runtimeAdapterRef",
                            if (provider.live) provider.evidence.runtimeAdapterRef else null,
                        )
                    },
                )
                put("turns", json.encodeToJsonElement(turns))
                put("summary", summary(turns))
            }

        val target = config.resultDir.resolve("progress-checkpoint.json")
        val temporary = config.resultDir.resolve("progress-checkpoint.json.tmp")
        Files.writeString(temporary, checkpoint.toString() + "\\n")
        try {
            Files.move(
                temporary,
                target,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(
                temporary,
                target,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

`;
}

function instrumentSource(source) {
  const input = Buffer.from(source, 'utf8');
  const observedBlob = gitBlobSha(input);
  if (observedBlob !== EXPECTED_SOURCE_GIT_BLOB) {
    throw new Error(
      `CONTINUITY_PRESSURE_SOURCE_BLOB_MISMATCH expected=${EXPECTED_SOURCE_GIT_BLOB} observed=${observedBlob}`,
    );
  }
  if (source.includes(PROGRESS_SCHEMA)) {
    throw new Error('CONTINUITY_PRESSURE_SOURCE_ALREADY_INSTRUMENTED');
  }
  if (count(source, CALL_ANCHOR) !== 1) {
    throw new Error('CONTINUITY_PRESSURE_CALL_ANCHOR_NOT_EXACTLY_ONCE');
  }
  if (count(source, FUNCTION_ANCHOR) !== 1) {
    throw new Error('CONTINUITY_PRESSURE_FUNCTION_ANCHOR_NOT_EXACTLY_ONCE');
  }

  let output = source.replace(
    CALL_ANCHOR,
    CALL_ANCHOR +
      '\n' +
      '                    writeProgressCheckpoint(\n' +
      '                        config = config,\n' +
      '                        scenario = scenario,\n' +
      '                        scenarioSha = scenarioSha,\n' +
      '                        experimentRef = experimentRef,\n' +
      '                        budget = budget,\n' +
      '                        observed = observed,\n' +
      '                        peakBytes = peakBytes,\n' +
      '                        provider = provider,\n' +
      '                    )',
  );
  output = output.replace(FUNCTION_ANCHOR, checkpointFunction() + FUNCTION_ANCHOR);
  return output;
}

function arg(name) {
  const index = process.argv.indexOf('--' + name);
  if (index < 0 || !process.argv[index + 1]) {
    throw new Error('--' + name + ' is required');
  }
  return path.resolve(process.argv[index + 1]);
}

function main() {
  const inputPath = arg('input');
  const outputPath = arg('output');
  const bindingPath = arg('binding');

  const source = fs.readFileSync(inputPath, 'utf8');
  const output = instrumentSource(source);
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.mkdirSync(path.dirname(bindingPath), { recursive: true });
  fs.writeFileSync(outputPath, output, 'utf8');

  const outputBytes = Buffer.from(output, 'utf8');
  const binding = {
    schemaVersion: 'memory-depth.progress-checkpoint-instrument-binding/v1',
    patchRef: PATCH_REF,
    source: {
      path: 'src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapter.kt',
      gitBlobSha: EXPECTED_SOURCE_GIT_BLOB,
      sha256: sha256(Buffer.from(source, 'utf8')),
      bytes: Buffer.byteLength(source, 'utf8'),
    },
    output: {
      sha256: sha256(outputBytes),
      gitBlobSha: gitBlobSha(outputBytes),
      bytes: outputBytes.length,
    },
    progressCheckpoint: {
      schemaVersion: PROGRESS_SCHEMA,
      filename: 'progress-checkpoint.json',
      writeBoundary: 'AFTER_EACH_COMPLETED_TURN',
      atomicReplaceRequested: true,
      incompleteTurnPromotedAsComplete: false,
    },
    semanticMutation: false,
    productTrackedSourceMutation: false,
  };
  fs.writeFileSync(bindingPath, JSON.stringify(binding, null, 2) + '\n', 'utf8');
  process.stdout.write(JSON.stringify(binding) + '\n');
}

if (require.main === module) {
  try {
    main();
  } catch (error) {
    process.stderr.write(String(error && error.stack ? error.stack : error) + '\n');
    process.exitCode = 1;
  }
}

module.exports = {
  EXPECTED_SOURCE_GIT_BLOB,
  PATCH_REF,
  PROGRESS_SCHEMA,
  gitBlobSha,
  instrumentSource,
};
