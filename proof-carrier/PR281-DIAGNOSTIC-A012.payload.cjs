#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-mac-payload-result/v1';

function sha256(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function defaultEffects() {
  return {
    sourceMutationPerformed: false,
    branchMutationPerformed: false,
    claimMutationPerformed: false,
    prMetadataMutationPerformed: false,
    reviewPerformed: false,
    approvalPerformed: false,
    readyTransitionPerformed: false,
    mergePerformed: false,
    mainMutationPerformed: false,
    checkpointMutationPerformed: false,
    forcePushPerformed: false,
    historyRewritePerformed: false,
    hostInstallationPerformed: false,
    modelRuntimePerformed: false,
    providerOrNetworkEffectPerformed: true,
    publicationPerformed: false,
    credentialSerializationPerformed: false,
    filesystemOutsideQualifiedRootsPerformed: false,
  };
}

function writeResult(value) {
  const root = process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if (!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR is unavailable');
  fs.mkdirSync(root, { recursive: true });
  fs.writeFileSync(
    path.join(root, 'payload-result.json'),
    JSON.stringify({ schemaVersion: RESULT_SCHEMA, acceptedExitCodes: [0], ...value }, null, 2) + '\n',
  );
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd,
    env: options.env || process.env,
    encoding: 'utf8',
    timeout: options.timeoutMs || 690000,
    maxBuffer: 96 * 1024 * 1024,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  return {
    status: Number.isInteger(result.status) ? result.status : 127,
    signal: result.signal || null,
    stdout: String(result.stdout || ''),
    stderr: String(result.stderr || (result.error ? result.error.message : '')),
    error: result.error || null,
  };
}

function retainedLog(root, stage, command, result) {
  const fileName = stage.toLowerCase().replace(/[^a-z0-9]+/g, '-') + '.log';
  const fullPath = path.join(root, fileName);
  const content = [
    'stage=' + stage,
    'command=' + command.join(' '),
    'exitCode=' + result.status,
    'signal=' + (result.signal || ''),
    '',
    '===== STDOUT =====',
    result.stdout,
    '',
    '===== STDERR =====',
    result.stderr,
    '',
  ].join('\n');
  fs.writeFileSync(fullPath, content);
  return {
    sourcePath: fullPath,
    targetPath: 'retained/inner-vexrelay/' + fileName,
    expectedSha256: sha256(Buffer.from(content)),
  };
}

function failure(stage, result, evidenceRef) {
  const tail = (result.stdout + '\n' + result.stderr).slice(-8000);
  return {
    failureFamily: 'VEXLIFE_PROD_PR281_DIAGNOSTIC_PROOF',
    failureFingerprint: sha256(Buffer.from(stage + '\n' + tail)),
    stage,
    summary:
      stage + ' failed exit=' + result.status +
      (result.signal ? ' signal=' + result.signal : '') +
      '; full output retained as ' + evidenceRef,
    evidenceRef,
  };
}

async function main() {
  const bindingPath = process.argv[2];
  if (!bindingPath) throw new Error('candidate binding path is required');
  const binding = JSON.parse(fs.readFileSync(path.resolve(bindingPath), 'utf8'));
  const resultRoot = process.env.VEXLOCALBRIDGE_RESULT_DIR;
  const capsuleRoot = process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
  if (!capsuleRoot || !path.isAbsolute(capsuleRoot)) {
    throw new Error('VEXLOCALBRIDGE_CAPSULE_ROOT is unavailable');
  }
  const retainedRoot = path.join(
    capsuleRoot,
    'bridge',
    'retained-diagnostics',
    process.env.VEXLOCALBRIDGE_TASK_REF || 'task',
    process.env.VEXLOCALBRIDGE_ATTEMPT_REF || 'attempt',
  );
  fs.mkdirSync(retainedRoot, { recursive: true });
  let work = null;
  const retainedEvidenceFiles = [];
  const completedStepReceipts = [];

  try {
    if (process.platform !== 'darwin' || process.arch !== 'arm64') {
      throw new Error('PR281 diagnostic proof requires Apple-Silicon macOS');
    }
    const gh = ['/opt/homebrew/bin/gh', '/usr/local/bin/gh'].find((candidate) => fs.existsSync(candidate));
    if (!gh) throw new Error('GitHub CLI unavailable');
    const auth = run(gh, ['auth', 'status', '--hostname', 'github.com'], { timeoutMs: 30000 });
    if (auth.status !== 0) throw new Error('GitHub CLI authentication unavailable');

    const javaHomeResult = run('/usr/libexec/java_home', ['-v', '21'], { timeoutMs: 10000 });
    if (javaHomeResult.status !== 0) throw new Error('Java 21 home unavailable');
    const javaHome = javaHomeResult.stdout.trim();
    const env = {
      ...process.env,
      JAVA_HOME: javaHome,
      PATH: javaHome + '/bin:' + process.env.PATH,
    };

    work = fs.mkdtempSync(path.join(process.env.TMPDIR || os.tmpdir(), 'vexlife-pr281-diagnostic-'));
    const checkout = path.join(work, 'candidate');

    const clone = run(gh, ['repo', 'clone', binding.repository, checkout, '--', '--no-checkout'], {
      timeoutMs: 180000,
      env,
    });
    if (clone.status !== 0) throw new Error('candidate clone failed: ' + clone.stderr.slice(-2000));

    const checkoutResult = run('/usr/bin/git', ['-C', checkout, 'checkout', '--detach', binding.head], {
      timeoutMs: 60000,
      env,
    });
    if (checkoutResult.status !== 0) throw new Error('candidate checkout failed: ' + checkoutResult.stderr.slice(-2000));

    const head = run('/usr/bin/git', ['-C', checkout, 'rev-parse', 'HEAD'], { env }).stdout.trim();
    const tree = run('/usr/bin/git', ['-C', checkout, 'rev-parse', 'HEAD^{tree}'], { env }).stdout.trim();
    const status = run('/usr/bin/git', ['-C', checkout, 'status', '--porcelain=v1', '-uall'], { env }).stdout.trim();
    if (head !== binding.head || tree !== binding.tree || status !== '') {
      throw new Error('candidate source identity/currentness mismatch');
    }
    completedStepReceipts.push({ step: 'exact-source', state: 'PASS', head, tree });

    const stages = [
      {
        ref: 'COMPILE_TEST_SOURCE',
        args: ['--no-daemon', '-Dorg.gradle.java.home=' + javaHome, 'compileTestKotlinJvm', '--stacktrace'],
      },
      {
        ref: 'JVM_TEST',
        args: ['--no-daemon', '-Dorg.gradle.java.home=' + javaHome, 'jvmTest', '--stacktrace'],
      },
      {
        ref: 'STRING_CHECK',
        args: ['--no-daemon', '-Dorg.gradle.java.home=' + javaHome, 'checkVexLifeStrings', '--stacktrace'],
      },
    ];

    for (const stage of stages) {
      const result = run('./gradlew', stage.args, { cwd: checkout, timeoutMs: 690000, env });
      const retained = retainedLog(retainedRoot, stage.ref, ['./gradlew', ...stage.args], result);
      retainedEvidenceFiles.push(retained);
      completedStepReceipts.push({
        step: stage.ref.toLowerCase(),
        state: result.status === 0 ? 'PASS' : 'SAFE_FAILURE',
        exitCode: result.status,
        signal: result.signal,
        evidenceRef: retained.targetPath,
      });

      if (result.status !== 0) {
        writeResult({
          disposition: 'SAFE_FAILURE',
          stopReasonOrNull: 'PR281_DIAGNOSTIC_STAGE_FAILED',
          firstSubstantiveFailureOrNull: failure(stage.ref, result, retained.targetPath),
          candidateFindings: ['CANDIDATE_BUILD_OR_TEST_FAILURE'],
          harnessFindings: [],
          environmentFindings: [],
          unknownFindings: [],
          effects: defaultEffects(),
          beforeState: { candidateHead: binding.head, candidateTree: binding.tree },
          afterState: {
            candidateHead: head,
            candidateTree: tree,
            failedStage: stage.ref,
            exitCode: result.status,
            signal: result.signal,
          },
          completedStepReceipts,
          retainedEvidenceFiles,
        });
        return;
      }
    }

    writeResult({
      disposition: 'PASS',
      stopReasonOrNull: null,
      firstSubstantiveFailureOrNull: null,
      candidateFindings: [],
      harnessFindings: [],
      environmentFindings: [],
      unknownFindings: [],
      effects: defaultEffects(),
      beforeState: { candidateHead: binding.head, candidateTree: binding.tree },
      afterState: {
        candidateHead: head,
        candidateTree: tree,
        compileTestProof: 'PASS',
        jvmTestProof: 'PASS',
        stringProof: 'PASS',
      },
      completedStepReceipts,
      retainedEvidenceFiles,
    });
  } catch (error) {
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'PR281_DIAGNOSTIC_PROOF_STOPPED',
      firstSubstantiveFailureOrNull: {
        failureFamily: 'VEXLIFE_PROD_PR281_DIAGNOSTIC_HARNESS',
        failureFingerprint: sha256(Buffer.from(String(error?.message || error))),
        stage: 'HARNESS',
        summary: String(error?.message || error).slice(0, 4000),
        evidenceRef: 'payload-result.json',
      },
      candidateFindings: [],
      harnessFindings: ['DIAGNOSTIC_HARNESS_STOPPED'],
      environmentFindings: [],
      unknownFindings: [],
      effects: defaultEffects(),
      beforeState: { candidateHead: binding.head, candidateTree: binding.tree },
      afterState: { errorClass: error?.constructor?.name || 'Error' },
      completedStepReceipts,
      retainedEvidenceFiles,
    });
  } finally {
    if (work) {
      try { fs.rmSync(work, { recursive: true, force: true }); } catch (_) {}
    }
  }
}

main().catch((error) => {
  try {
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'UNCAUGHT_PR281_DIAGNOSTIC_FAILURE',
      firstSubstantiveFailureOrNull: {
        failureFamily: 'VEXLIFE_PROD_PR281_DIAGNOSTIC_UNCAUGHT',
        failureFingerprint: sha256(Buffer.from(String(error?.message || error))),
        stage: 'UNCAUGHT',
        summary: String(error?.message || error).slice(0, 4000),
        evidenceRef: 'payload-result.json',
      },
      candidateFindings: [],
      harnessFindings: ['UNCAUGHT_PR281_DIAGNOSTIC_FAILURE'],
      environmentFindings: [],
      unknownFindings: [],
      effects: defaultEffects(),
      beforeState: {},
      afterState: {},
      completedStepReceipts: [],
      retainedEvidenceFiles: [],
    });
  } catch (_) {}
  process.exitCode = 0;
});

// [VXG RealForever]
