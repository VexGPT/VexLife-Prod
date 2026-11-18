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

function effects() {
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

function failure(error, stage) {
  const summary = String(error?.message || error || 'unknown proof failure').slice(0, 1800);
  return {
    failureFamily: 'VEXLIFE_PROD_PR281_SOURCE_PROOF',
    failureFingerprint: sha256(Buffer.from(stage + '\n' + summary)),
    stage,
    summary,
    evidenceRef: 'payload-result.json',
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
    timeout: options.timeoutMs || 600000,
    maxBuffer: 64 * 1024 * 1024,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(
      command + ' ' + args.join(' ') + ' failed exit=' + result.status +
      '\nstdout=' + String(result.stdout || '').slice(-12000) +
      '\nstderr=' + String(result.stderr || '').slice(-12000),
    );
  }
  return result;
}

async function main() {
  const bindingPath = process.argv[2];
  if (!bindingPath) throw new Error('candidate binding path is required');
  const binding = JSON.parse(fs.readFileSync(path.resolve(bindingPath), 'utf8'));

  let work = null;
  let stage = 'HOST_PREFLIGHT';
  const completedStepReceipts = [];

  try {
    if (process.platform !== 'darwin' || process.arch !== 'arm64') {
      throw new Error('PR281 proof requires Apple-Silicon macOS');
    }
    const gh = ['/opt/homebrew/bin/gh', '/usr/local/bin/gh'].find((candidate) => fs.existsSync(candidate));
    if (!gh) throw new Error('GitHub CLI unavailable');
    run(gh, ['auth', 'status', '--hostname', 'github.com'], { timeoutMs: 30000 });

    const javaHome = run('/usr/libexec/java_home', ['-v', '21'], { timeoutMs: 10000 }).stdout.trim();
    const env = {
      ...process.env,
      JAVA_HOME: javaHome,
      PATH: javaHome + '/bin:' + process.env.PATH,
    };

    work = fs.mkdtempSync(path.join(process.env.TMPDIR || os.tmpdir(), 'vexlife-pr281-proof-'));
    const checkout = path.join(work, 'candidate');

    stage = 'EXACT_SOURCE';
    run(gh, ['repo', 'clone', binding.repository, checkout, '--', '--no-checkout'], {
      timeoutMs: 180000,
      env,
    });
    run('/usr/bin/git', ['-C', checkout, 'checkout', '--detach', binding.head], {
      timeoutMs: 60000,
      env,
    });
    const head = run('/usr/bin/git', ['-C', checkout, 'rev-parse', 'HEAD'], { env }).stdout.trim();
    const tree = run('/usr/bin/git', ['-C', checkout, 'rev-parse', 'HEAD^{tree}'], { env }).stdout.trim();
    const status = run('/usr/bin/git', ['-C', checkout, 'status', '--porcelain=v1', '-uall'], { env }).stdout.trim();
    if (head !== binding.head || tree !== binding.tree || status !== '') {
      throw new Error('candidate source identity/currentness mismatch');
    }
    completedStepReceipts.push({ step: 'exact-source', state: 'PASS', head, tree });

    stage = 'JVM_STRING_PROOF';
    const proof = run(
      './gradlew',
      ['--no-daemon', '-Dorg.gradle.java.home=' + javaHome, 'jvmTest', 'checkVexLifeStrings'],
      { cwd: checkout, timeoutMs: 690000, env },
    );
    completedStepReceipts.push({
      step: 'jvm-test-and-strings',
      state: 'PASS',
      stdoutTail: String(proof.stdout || '').slice(-5000),
      stderrTail: String(proof.stderr || '').slice(-3000),
    });

    writeResult({
      disposition: 'PASS',
      stopReasonOrNull: null,
      firstSubstantiveFailureOrNull: null,
      candidateFindings: [],
      harnessFindings: [],
      environmentFindings: [],
      unknownFindings: [],
      effects: effects(),
      beforeState: { candidateHead: binding.head, candidateTree: binding.tree },
      afterState: { candidateHead: head, candidateTree: tree, repositoryProof: 'PASS' },
      completedStepReceipts,
    });
  } catch (error) {
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'PR281_SOURCE_PROOF_STOPPED',
      firstSubstantiveFailureOrNull: failure(error, stage),
      candidateFindings: [stage === 'JVM_STRING_PROOF' ? 'CANDIDATE_BUILD_OR_TEST_FAILURE' : ''],
      harnessFindings: [],
      environmentFindings: [],
      unknownFindings: [],
      effects: effects(),
      beforeState: { candidateHead: binding.head, candidateTree: binding.tree },
      afterState: {
        stage,
        errorClass: error?.constructor?.name || 'Error',
        errorMessage: String(error?.message || error).slice(0, 1800),
      },
      completedStepReceipts,
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
      stopReasonOrNull: 'UNCAUGHT_PR281_PROOF_FAILURE',
      firstSubstantiveFailureOrNull: failure(error, 'UNCAUGHT'),
      candidateFindings: [],
      harnessFindings: ['UNCAUGHT_PR281_PROOF_FAILURE'],
      environmentFindings: [],
      unknownFindings: [],
      effects: effects(),
      beforeState: {},
      afterState: {},
      completedStepReceipts: [],
    });
  } catch (_) {}
  process.exitCode = 0;
});

// [VXG RealForever]
