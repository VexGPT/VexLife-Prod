import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import http from 'node:http';
import { spawn, spawnSync } from 'node:child_process';

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-mac-payload-result/v1';
const POLICY_ENV = 'VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON';
const RESULT_DIR = process.env.VEXLOCALBRIDGE_RESULT_DIR;
const CAPSULE_ROOT = process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
const DEFAULT_HOME = path.join(os.homedir(), '.vexlife');
const SERVICE_RECEIPT = path.join(
  os.homedir(),
  'Library',
  'Application Support',
  'VexLife',
  'ProductService',
  'service-lifetime.json',
);
const RUNTIME_RECEIPT_RELATIVES = [
  'runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json',
  'runtime/heretic/persistent-runtime-owner-receipt.json',
];
const SERVICE_ORIGIN = 'http://127.0.0.1:18210';
const SERVICE_LABEL = 'com.vexlife.prod.service';
const RUNTIME_LABEL_PREFIX = 'com.vexlife.prod.heretic.';
const BUILD_FETCH_EFFECT = 'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1';
const APPLICATION_LIVED_EFFECT = 'effect.vexlocalbridge.vexlife-prod.application-lived-proof/v1';
const CHRONOLOGY_RELATIVE =
  'runtime/vexlife-prod/logs/chronology/chronology.jsonl';

const effects = {
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
  providerOrNetworkEffectPerformed: false,
  publicationPerformed: false,
  credentialSerializationPerformed: false,
  filesystemOutsideQualifiedRootsPerformed: false,
};

const completedStepReceipts = [];
const candidateFindings = [];
const harnessFindings = [];
const environmentFindings = [];
const unknownFindings = [];
const beforeState = {};
const afterState = {};
let setupPath = null;
let productMutationAttempted = false;
let recoveryAttempted = false;
let recoverySucceeded = false;
let activeApplication = null;

function resultPath() {
  if (!RESULT_DIR) throw new Error('VEXLOCALBRIDGE_RESULT_DIR_MISSING');
  fs.mkdirSync(RESULT_DIR, { recursive: true });
  return path.join(RESULT_DIR, 'payload-result.json');
}

function writeResult(disposition, stopReasonOrNull = null) {
  const value = {
    schemaVersion: RESULT_SCHEMA,
    disposition,
    stopReasonOrNull,
    firstSubstantiveFailureOrNull: null,
    acceptedExitCodes: [0],
    candidateFindings,
    harnessFindings,
    environmentFindings,
    unknownFindings,
    effects,
    beforeState,
    afterState: {
      ...afterState,
      recoveryAttempted,
      recoverySucceeded,
    },
    completedStepReceipts,
  };
  fs.writeFileSync(resultPath(), JSON.stringify(value, null, 2) + '\n');
}

function fail(code, message = code) {
  const error = new Error(message);
  error.code = code;
  throw error;
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function truncate(value, limit = 12000) {
  const text = String(value || '');
  return text.length <= limit ? text : text.slice(text.length - limit);
}

function run(executable, args, {
  cwd = process.cwd(),
  timeoutMs = 7 * 60 * 1000,
  env = {},
  acceptedExitCodes = [0],
  label = path.basename(executable),
} = {}) {
  const receipt = spawnSync(executable, args, {
    cwd,
    env: { ...process.env, ...env },
    encoding: 'utf8',
    timeout: timeoutMs,
    maxBuffer: 32 * 1024 * 1024,
    shell: false,
  });
  const exitCode = Number.isInteger(receipt.status) ? receipt.status : 127;
  const out = truncate(receipt.stdout);
  const err = truncate(receipt.stderr || receipt.error?.message || '');
  console.log(`COMMAND ${label} exit=${exitCode}`);
  if (out) console.log(out);
  if (err) console.error(err);
  if (!acceptedExitCodes.includes(exitCode)) {
    const error = new Error(`${label} failed with exit ${exitCode}: ${err || out}`);
    error.code = `${label.toUpperCase().replace(/[^A-Z0-9]+/g, '_')}_FAILED`;
    throw error;
  }
  return { exitCode, stdout: receipt.stdout || '', stderr: receipt.stderr || '' };
}

function processAlive(pid) {
  if (!Number.isInteger(pid) || pid <= 0) return false;
  const receipt = spawnSync('/bin/ps', ['-p', String(pid), '-o', 'pid='], {
    encoding: 'utf8',
    timeout: 5000,
    shell: false,
  });
  return receipt.status === 0 && String(receipt.stdout || '').trim() === String(pid);
}

function processSnapshot(pid) {
  const receipt = spawnSync(
    '/bin/ps',
    ['-p', String(pid), '-o', 'pid=,ppid=,pgid=,rss=,lstart=,command='],
    { encoding: 'utf8', timeout: 5000, shell: false },
  );
  return {
    pid,
    alive: receipt.status === 0,
    summary: truncate(receipt.stdout || receipt.stderr || '', 4000).trim(),
  };
}

function launchdLoaded(target) {
  const receipt = spawnSync('/bin/launchctl', ['print', target], {
    encoding: 'utf8',
    timeout: 5000,
    shell: false,
  });
  return {
    loaded: receipt.status === 0,
    output: truncate(receipt.stdout || receipt.stderr || '', 6000),
  };
}

async function sleep(ms) {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

async function getJson(url, timeoutMs = 5000) {
  return await new Promise((resolve, reject) => {
    const request = http.get(url, {
      timeout: timeoutMs,
      headers: { Accept: 'application/json' },
    }, (response) => {
      const chunks = [];
      response.on('data', (chunk) => chunks.push(chunk));
      response.on('end', () => {
        const body = Buffer.concat(chunks).toString('utf8');
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`HTTP_${response.statusCode}:${body.slice(0, 1000)}`));
          return;
        }
        try {
          resolve(JSON.parse(body));
        } catch (error) {
          reject(new Error(`JSON_PARSE:${error.message}`));
        }
      });
    });
    request.on('timeout', () => request.destroy(new Error('HTTP_TIMEOUT')));
    request.on('error', reject);
  });
}

async function optionalJson(url) {
  try {
    return await getJson(url, 3000);
  } catch {
    return null;
  }
}

async function waitForExactSource(head, timeoutMs = 180000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const current = await optionalJson(`${SERVICE_ORIGIN}/api/v1/source-currentness`);
    if (current?.sourceSha === head && Number(current?.processPid) > 0) return current;
    await sleep(1000);
  }
  fail('EXACT_SOURCE_NOT_REACHABLE', `VexLife-Prod ${head} did not become reachable`);
}

async function waitUntil(check, timeoutMs, code) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await check()) return;
    await sleep(750);
  }
  fail(code);
}

function writerLeasePaths(home) {
  const root = path.join(home, 'runtime', 'thread-writer-locks');
  if (!fs.existsSync(root)) return [];
  const found = [];
  const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const target = path.join(dir, entry.name);
      if (entry.isSymbolicLink()) fail('WRITER_LOCK_PATH_SYMLINK_HELD');
      if (entry.isDirectory()) walk(target);
      else if (entry.isFile() && entry.name.endsWith('.lock')) found.push(target);
    }
  };
  walk(root);
  return found.sort();
}

function validateDefaultHomeBeforeMutation() {
  const normalizedDefault = path.resolve(DEFAULT_HOME);
  if (!fs.existsSync(normalizedDefault)) {
    fail('DEFAULT_HOME_ABSENT_HELD', 'existing default Vex Home is required; Home formation is not authorized');
  }
  const stat = fs.lstatSync(normalizedDefault);
  if (!stat.isDirectory() || stat.isSymbolicLink()) {
    fail('DEFAULT_HOME_NOT_PLAIN_DIRECTORY_HELD');
  }

  let lifetime = null;
  if (fs.existsSync(SERVICE_RECEIPT)) {
    lifetime = readJson(SERVICE_RECEIPT);
    const boundHome = lifetime?.homeBinding?.homePath;
    if (typeof boundHome === 'string' && path.resolve(boundHome) !== normalizedDefault) {
      fail(
        'HELD_CUSTOM_HOME_NOT_YET_QUALIFIED',
        `service lifetime binds custom Home ${boundHome}`,
      );
    }
  }

  const locks = writerLeasePaths(normalizedDefault);
  if (locks.length > 0) {
    fail('CONVERSATION_WRITER_LEASE_PRESENT_HELD', `writer lease count=${locks.length}`);
  }

  return { home: normalizedDefault, lifetime };
}

async function validateCurrentEndpointHome(home) {
  const current = await optionalJson(`${SERVICE_ORIGIN}/api/v1/current-state`);
  if (current?.homePath && path.resolve(current.homePath) !== path.resolve(home)) {
    fail('HELD_CUSTOM_HOME_NOT_YET_QUALIFIED', `current service uses ${current.homePath}`);
  }
  return current;
}

function observeLiveRuntime(home) {
  const live = [];
  const observed = [];
  for (const relative of RUNTIME_RECEIPT_RELATIVES) {
    const receiptPath = path.join(home, relative);
    if (!fs.existsSync(receiptPath)) continue;
    let receipt;
    try {
      receipt = readJson(receiptPath);
    } catch (error) {
      fail('RUNTIME_RECEIPT_UNREADABLE_HELD', `${relative}: ${error.message}`);
    }
    const pid = Number(receipt?.runtime?.pid);
    const processSubstrate = receipt?.runtime?.processSubstrate;
    const entry = {
      relative,
      receiptPath,
      receipt,
      pid,
      alive: processAlive(pid),
      label: processSubstrate?.label || null,
      domainTarget: processSubstrate?.domainTarget || null,
      substratePid: Number(processSubstrate?.pid),
    };
    observed.push(entry);
    if (entry.alive) live.push(entry);
  }
  if (live.length !== 1) {
    fail(
      live.length === 0 ? 'EXACT_LIVE_RUNTIME_NOT_FOUND' : 'MULTIPLE_LIVE_RUNTIME_RECEIPTS_HELD',
      `live runtime receipts=${live.length}, observed=${observed.length}`,
    );
  }
  const runtime = live[0];
  if (
    !runtime.label ||
    !runtime.label.startsWith(RUNTIME_LABEL_PREFIX) ||
    !runtime.domainTarget ||
    runtime.substratePid !== runtime.pid
  ) {
    fail('RUNTIME_RECEIPT_PROCESS_SUBSTRATE_MISMATCH_HELD');
  }
  const launchd = launchdLoaded(`${runtime.domainTarget}/${runtime.label}`);
  if (!launchd.loaded) fail('RUNTIME_LAUNCHD_COORDINATE_NOT_LOADED');
  return {
    relative: runtime.relative,
    pid: runtime.pid,
    label: runtime.label,
    domainTarget: runtime.domainTarget,
    launchdTarget: `${runtime.domainTarget}/${runtime.label}`,
    process: processSnapshot(runtime.pid),
  };
}

async function observeExactService(home, head) {
  if (!fs.existsSync(SERVICE_RECEIPT)) fail('SERVICE_LIFETIME_RECEIPT_MISSING');
  const receipt = readJson(SERVICE_RECEIPT);
  if (receipt.sourceSha !== head) fail('SERVICE_RECEIPT_SOURCE_MISMATCH');
  if (receipt.serviceOrigin !== SERVICE_ORIGIN) fail('SERVICE_ORIGIN_MISMATCH');
  if (path.resolve(receipt?.homeBinding?.homePath || '') !== path.resolve(home)) {
    fail('SERVICE_HOME_BINDING_MISMATCH');
  }
  if (receipt?.launchd?.label !== SERVICE_LABEL) fail('SERVICE_LABEL_MISMATCH');
  const pid = Number(receipt?.launchd?.pid);
  if (!processAlive(pid)) fail('SERVICE_RECEIPT_PID_NOT_LIVE');
  const source = await waitForExactSource(head);
  if (Number(source.processPid) !== pid) fail('SERVICE_ENDPOINT_PID_MISMATCH');
  const target = `${receipt.launchd.domainTarget}/${receipt.launchd.label}`;
  const launchd = launchdLoaded(target);
  if (!launchd.loaded) fail('SERVICE_LAUNCHD_COORDINATE_NOT_LOADED');
  return {
    pid,
    target,
    sourceSha: receipt.sourceSha,
    homePath: receipt.homeBinding.homePath,
    process: processSnapshot(pid),
  };
}

function setupCommand(home, head) {
  if (!setupPath || !fs.existsSync(setupPath)) fail('PRODUCT_SETUP_LAUNCHER_MISSING');
  productMutationAttempted = true;
  effects.modelRuntimePerformed = true;
  return run(setupPath, [
    '--home', home,
    '--expected-source-sha', head,
    '--reuse-existing',
  ], {
    cwd: path.dirname(setupPath),
    timeoutMs: 5 * 60 * 1000,
    label: 'product-setup-reuse',
  });
}

async function recoverProduct(home, head) {
  recoveryAttempted = true;
  try {
    setupCommand(home, head);
    await waitForExactSource(head, 180000);
    recoverySucceeded = true;
    return true;
  } catch (error) {
    recoverySucceeded = false;
    unknownFindings.push(`PRODUCT_RECOVERY_UNCONFIRMED:${error.code || error.message}`);
    console.error(`RECOVERY_FAILED ${error.stack || error.message}`);
    return false;
  }
}


function chronologyFiles(home) {
  const directory = path.join(home, 'runtime', 'vexlife-prod', 'logs', 'chronology');
  if (!fs.existsSync(directory)) return [];
  const files = fs
    .readdirSync(directory, { withFileTypes: true })
    .filter(
      (entry) =>
        entry.isFile() &&
        !entry.isSymbolicLink() &&
        entry.name.startsWith('chronology') &&
        entry.name.endsWith('.jsonl'),
    )
    .map((entry) => path.join(directory, entry.name));
  return files.sort((left, right) => {
    const leftCurrent = path.basename(left) === 'chronology.jsonl';
    const rightCurrent = path.basename(right) === 'chronology.jsonl';
    if (leftCurrent !== rightCurrent) return leftCurrent ? 1 : -1;
    return path.basename(left).localeCompare(path.basename(right));
  });
}

function chronologyRecords(home) {
  const records = [];
  for (const file of chronologyFiles(home)) {
    const text = fs.readFileSync(file, 'utf8');
    text
      .split(/\r?\n/)
      .filter((line) => line.trim() !== '')
      .forEach((line, index) => {
        try {
          records.push(JSON.parse(line));
        } catch (error) {
          fail(
            'CHRONOLOGY_RECORD_UNREADABLE',
            path.basename(file) + ':' + index + ' ' + error.message,
          );
        }
      });
  }
  return records;
}

function chronologyBaseline() {
  return Date.now();
}

function candidateRecordsAfter(home, baselineEpochMs, head) {
  return chronologyRecords(home).filter((record) => {
    const at = Date.parse(record?.at || '');
    return (
      Number.isFinite(at) &&
      at >= baselineEpochMs &&
      record?.sourceSha === head
    );
  });
}

function orderedTurnEvidence(records) {
  const send = records.findIndex((row) => row?.fact === 'SEND_ACCEPTED');
  if (send < 0) return null;
  const provider =
    records.findIndex(
      (row, index) => index > send && row?.fact === 'PROVIDER_INVOCATION_REQUESTED',
    );
  if (provider < 0) return null;
  const completed =
    records.findIndex(
      (row, index) => index > provider && row?.fact === 'TURN_COMPLETED',
    );
  if (completed < 0) return null;
  return {
    send: {
      at: records[send].at,
      owner: records[send].owner,
      refs: records[send].refs,
    },
    provider: {
      at: records[provider].at,
      owner: records[provider].owner,
      refs: records[provider].refs,
    },
    completed: {
      at: records[completed].at,
      owner: records[completed].owner,
      refs: records[completed].refs,
    },
  };
}

async function waitForPresentationAndHumanTurn(home, baseline, head, child, label) {
  const deadline = Date.now() + 7 * 60 * 1000;
  let presentation = null;
  while (Date.now() < deadline) {
    const records = candidateRecordsAfter(home, baseline, head);
    presentation =
      presentation ||
      records.find((row) => row?.fact === 'PRESENTATION_ARRIVED') ||
      null;
    const turn = orderedTurnEvidence(records);
    if (presentation && turn) {
      return {
        presentation: {
          at: presentation.at,
          owner: presentation.owner,
          refs: presentation.refs,
        },
        turn,
      };
    }
    if (child.exitCode !== null) {
      fail(
        'APPLICATION_EXITED_BEFORE_LIVED_TURN',
        label + ' exit=' + child.exitCode,
      );
    }
    await sleep(750);
  }
  fail('HUMAN_LIVED_TURN_NOT_OBSERVED', label);
}

async function waitForPresentation(home, baseline, head, child, label) {
  const deadline = Date.now() + 4 * 60 * 1000;
  while (Date.now() < deadline) {
    const record =
      candidateRecordsAfter(home, baseline, head)
        .find((row) => row?.fact === 'PRESENTATION_ARRIVED');
    if (record) {
      return { at: record.at, owner: record.owner, refs: record.refs };
    }
    if (child.exitCode !== null) {
      fail(
        'APPLICATION_EXITED_BEFORE_PRESENTATION',
        label + ' exit=' + child.exitCode,
      );
    }
    await sleep(750);
  }
  fail('PRESENTATION_ARRIVAL_NOT_OBSERVED', label);
}

function plutilRaw(infoPlist, key) {
  return run('/usr/bin/plutil', ['-extract', key, 'raw', '-o', '-', infoPlist], {
    cwd: path.dirname(infoPlist),
    timeoutMs: 10000,
    label: 'plist-' + key,
  }).stdout.trim();
}

function exactMacApplication(sourceRoot, head) {
  const bundle = path.join(
    sourceRoot,
    'build',
    'vexlife-prod-mac-application',
    head,
    'VexLife.app',
  );
  const info = path.join(bundle, 'Contents', 'Info.plist');
  if (!fs.existsSync(info)) fail('FORMED_VEXLIFE_APP_INFO_PLIST_MISSING');
  const expectedSource = plutilRaw(info, 'VexLifeProdExpectedSourceSha');
  const presentationMode = plutilRaw(info, 'VexLifeProdPresentationMode');
  const executableName = plutilRaw(info, 'CFBundleExecutable');
  if (expectedSource !== head) fail('FORMED_VEXLIFE_APP_SOURCE_MISMATCH');
  if (presentationMode !== 'NATIVE_EMBEDDED') {
    fail('FORMED_VEXLIFE_APP_NOT_NATIVE_EMBEDDED');
  }
  if (!executableName || executableName.includes('/') || executableName.includes('\\')) {
    fail('FORMED_VEXLIFE_APP_EXECUTABLE_INVALID');
  }
  const executable = path.resolve(bundle, 'Contents', 'MacOS', executableName);
  const relative = path.relative(path.resolve(bundle), executable);
  if (relative.startsWith('..') || path.isAbsolute(relative)) {
    fail('FORMED_VEXLIFE_APP_EXECUTABLE_ESCAPED_BUNDLE');
  }
  if (!fs.existsSync(executable) || !fs.statSync(executable).isFile()) {
    fail('FORMED_VEXLIFE_APP_EXECUTABLE_MISSING');
  }
  fs.accessSync(executable, fs.constants.X_OK);
  const microphoneCopy = plutilRaw(info, 'NSMicrophoneUsageDescription');
  if (!microphoneCopy) fail('FORMED_VEXLIFE_APP_MICROPHONE_PRIVACY_COPY_MISSING');
  return { bundle, info, executable, executableName, presentationMode };
}

function launchApplication(application, label) {
  const child = spawn(application.executable, [], {
    cwd: path.dirname(application.executable),
    env: process.env,
    stdio: 'inherit',
    detached: false,
  });
  child.once('error', (error) => {
    console.error(label + ' launch error: ' + error.message);
  });
  activeApplication = child;
  return child;
}

async function waitForApplicationExit(child, timeoutMs, code) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) {
      if (activeApplication === child) activeApplication = null;
      return child.exitCode;
    }
    await sleep(500);
  }
  fail(code);
}

function homeIdentityFromCurrentState(current) {
  if (!current?.homeRef || !current?.deviceRef || !current?.companionLineageRef) {
    fail('HOME_IDENTITY_PROJECTION_INCOMPLETE');
  }
  return {
    homePath: current.homePath,
    homeRef: current.homeRef,
    deviceRef: current.deviceRef,
    companionLineageRef: current.companionLineageRef,
  };
}

function requireSameHomeIdentity(expected, actual) {
  if (
    !actual ||
    path.resolve(actual.homePath || '') !== path.resolve(expected.homePath || '') ||
    actual.homeRef !== expected.homeRef ||
    actual.deviceRef !== expected.deviceRef ||
    actual.companionLineageRef !== expected.companionLineageRef
  ) {
    fail('HOME_IDENTITY_CHANGED_ACROSS_QUIT_REOPEN');
  }
}

async function main() {
  if (process.platform !== 'darwin') fail('MACOS_REQUIRED');
  if (!['arm64', 'aarch64'].includes(process.arch)) fail('APPLE_SILICON_REQUIRED');
  if (!CAPSULE_ROOT || !path.isAbsolute(CAPSULE_ROOT)) fail('CAPSULE_ROOT_REQUIRED');

  const context = readJson(path.join(process.cwd(), 'inputs', 'context.json'));
  const policy = JSON.parse(process.env[POLICY_ENV] || 'null');
  if (!policy) fail('VEXLIFE_PROD_HOST_EFFECT_POLICY_MISSING');
  if (policy.repository !== 'VexGPT/VexLife-Prod') fail('POLICY_REPOSITORY_MISMATCH');
  if (policy.hostProfileRef !== 'macos-victor') fail('POLICY_HOST_MISMATCH');
  if (policy.candidate?.head !== context.candidate.head
      || policy.candidate?.tree !== context.candidate.tree
      || policy.candidate?.base !== context.candidate.base
      || policy.candidate?.branch !== context.candidate.branch) {
    fail('POLICY_CANDIDATE_MISMATCH');
  }
  if (policy.processPolicy?.broadProcessKillAllowed !== false
      || policy.processPolicy?.arbitraryPidTerminationAllowed !== false) {
    fail('POLICY_PROCESS_BOUNDARY_WIDENED');
  }
  if (policy.productDataPolicy?.hostInstallationAllowed !== false
      || policy.productDataPolicy?.homeDeletionAllowed !== false
      || policy.productDataPolicy?.homeRecreationAllowed !== false
      || policy.productDataPolicy?.modelAcquisitionAllowed !== false
      || policy.productDataPolicy?.modelSwitchForProofAllowed !== false) {
    fail('POLICY_PRODUCT_DATA_BOUNDARY_WIDENED');
  }
  if (policy.buildDependencyFetchEffectRef !== BUILD_FETCH_EFFECT
      || policy.buildDependencyFetchAuthorized !== true) {
    fail('BUILD_DEPENDENCY_FETCH_AUTHORITY_REQUIRED');
  }
  if (policy.applicationLivedProofEffectRef !== APPLICATION_LIVED_EFFECT
      || policy.applicationLivedProofAuthorized !== true
      || policy.applicationProofPolicy?.buildTaskOwnedApplicationAllowed !== true
      || policy.applicationProofPolicy?.launchTaskOwnedApplicationAllowed !== true
      || policy.applicationProofPolicy?.frameworkManagedWebProfileAllowed !== true
      || policy.applicationProofPolicy?.hostInstallationAllowed !== false
      || policy.applicationProofPolicy?.systemAutomationAllowed !== false
      || policy.applicationProofPolicy?.screenCaptureAllowed !== false) {
    fail('APPLICATION_LIVED_AUTHORITY_REQUIRED');
  }
  completedStepReceipts.push({ step: 'host-effect-policy', state: 'PASS' });

  const homeObservation = validateDefaultHomeBeforeMutation();
  const home = homeObservation.home;
  const currentStateBefore = await validateCurrentEndpointHome(home);
  beforeState.homePath = home;
  beforeState.currentSourceBeforeOrNull =
    (await optionalJson(`${SERVICE_ORIGIN}/api/v1/source-currentness`))?.sourceSha || null;
  beforeState.currentStateBeforeOrNull = currentStateBefore;
  completedStepReceipts.push({ step: 'existing-home-and-writer-preflight', state: 'PASS' });

  const bundlePath = path.join(process.cwd(), 'inputs', 'vexlife-prod.bundle');
  if (!fs.existsSync(bundlePath)) fail('IMMUTABLE_PRODUCT_BUNDLE_MISSING');
  const sourceRoot = path.join(CAPSULE_ROOT, 'product-source');
  fs.rmSync(sourceRoot, { recursive: true, force: true });
  run('/usr/bin/git', ['clone', bundlePath, sourceRoot], {
    cwd: CAPSULE_ROOT,
    timeoutMs: 120000,
    label: 'clone-immutable-product-bundle',
  });
  run('/usr/bin/git', ['checkout', '--detach', context.candidate.head], {
    cwd: sourceRoot,
    timeoutMs: 30000,
    label: 'checkout-exact-product-head',
  });
  const observedHead = run('/usr/bin/git', ['rev-parse', 'HEAD'], {
    cwd: sourceRoot,
    timeoutMs: 10000,
    label: 'product-head',
  }).stdout.trim();
  const observedTree = run('/usr/bin/git', ['rev-parse', 'HEAD^{tree}'], {
    cwd: sourceRoot,
    timeoutMs: 10000,
    label: 'product-tree',
  }).stdout.trim();
  if (observedHead !== context.candidate.head) fail('PRODUCT_HEAD_MISMATCH');
  if (observedTree !== context.candidate.tree) fail('PRODUCT_TREE_MISMATCH');
  run('/usr/bin/git', ['merge-base', '--is-ancestor', context.candidate.base, context.candidate.head], {
    cwd: sourceRoot,
    timeoutMs: 10000,
    label: 'product-base-ancestor',
  });
  completedStepReceipts.push({ step: 'exact-product-source', state: 'PASS' });

  const gradleHome = path.join(CAPSULE_ROOT, 'gradle-home');
  fs.mkdirSync(gradleHome, { recursive: true });
  const buildEnv = {
    GRADLE_USER_HOME: gradleHome,
    GRADLE_OPTS:
      '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process',
    KOTLIN_DAEMON_JVMARGS: '-Xmx4096m',
    KONAN_DATA_DIR: path.join(CAPSULE_ROOT, 'konan-data'),
  };

  effects.providerOrNetworkEffectPerformed = true;
  run('/bin/bash', [
    'gradlew',
    '--no-daemon',
    '--max-workers=1',
    '-PvexlifeAppVersion=0.0.0',
    'archiveVexLifeProdMacApplication',
  ], {
    cwd: sourceRoot,
    timeoutMs: 30 * 60 * 1000,
    env: buildEnv,
    label: 'mac-app-formation',
  });
  completedStepReceipts.push({ step: 'mac-app-formation', state: 'PASS' });

  const application = exactMacApplication(sourceRoot, context.candidate.head);
  completedStepReceipts.push({ step: 'exact-application-formed', state: 'PASS' });
  afterState.application = {
    bundle: application.bundle,
    executableName: application.executableName,
    presentationMode: application.presentationMode,
    taskOwnedBuild: true,
    hostInstallationPerformed: false,
  };

  setupPath = path.join(
    application.bundle,
    'Contents',
    'Resources',
    'private-service-image',
    'bin',
    'vexlife-prod-setup',
  );
  if (!fs.existsSync(setupPath)) fail('FORMED_PRODUCT_SETUP_LAUNCHER_MISSING');

  setupCommand(home, context.candidate.head);
  const serviceBefore = await observeExactService(home, context.candidate.head);
  const runtimeBefore = observeLiveRuntime(home);
  const currentBefore = await getJson(`${SERVICE_ORIGIN}/api/v1/current-state`);
  const homeIdentityBefore = homeIdentityFromCurrentState(currentBefore);
  beforeState.exactCandidateService = serviceBefore;
  beforeState.exactCandidateRuntime = runtimeBefore;
  beforeState.homeIdentity = homeIdentityBefore;
  completedStepReceipts.push({ step: 'first-product-current', state: 'PASS' });

  const firstBaseline = chronologyBaseline();
  console.log(
    'HUMAN_ACTION_REQUIRED FIRST: In VexLife, choose Open VexLife, send one ordinary message and wait for the response. Then close the Product window with the red close button. Do NOT choose Quit VexLife in this phase.',
  );
  const firstApp = launchApplication(application, 'first-application');
  const firstLived = await waitForPresentationAndHumanTurn(
    home,
    firstBaseline,
    context.candidate.head,
    firstApp,
    'first-application',
  );
  completedStepReceipts.push({
    step: 'first-human-turn-completed',
    state: 'PASS',
  });

  const firstExit = await waitForApplicationExit(
    firstApp,
    4 * 60 * 1000,
    'FIRST_APPLICATION_DID_NOT_EXIT_AFTER_PRESENTATION_CLOSE',
  );
  if (firstExit !== 0) {
    fail('FIRST_APPLICATION_EXIT_NONZERO', String(firstExit));
  }

  const serviceAfterClose = await observeExactService(home, context.candidate.head);
  const runtimeAfterClose = observeLiveRuntime(home);
  if (
    serviceAfterClose.pid !== serviceBefore.pid ||
    runtimeAfterClose.pid !== runtimeBefore.pid
  ) {
    fail('PRESENTATION_CLOSE_CHANGED_PRODUCT_LIFETIME');
  }
  afterState.firstSession = {
    lived: firstLived,
    applicationExit: firstExit,
    servicePidBefore: serviceBefore.pid,
    servicePidAfterClose: serviceAfterClose.pid,
    runtimePidBefore: runtimeBefore.pid,
    runtimePidAfterClose: runtimeAfterClose.pid,
  };
  completedStepReceipts.push({
    step: 'presentation-close-preserved-product',
    state: 'PASS',
  });

  const locksBeforeQuit = writerLeasePaths(home);
  if (locksBeforeQuit.length > 0) {
    fail(
      'CONVERSATION_WRITER_LEASE_PRESENT_BEFORE_QUIT_HELD',
      'writer lease count=' + locksBeforeQuit.length,
    );
  }
  const quitBaseline = chronologyBaseline();
  console.log(
    'HUMAN_ACTION_REQUIRED SECOND: Reopen VexLife, choose Open VexLife, then choose Quit VexLife (Cmd-Q) from the Product presentation. Do NOT use the red close button in this phase.',
  );
  const quitApp = launchApplication(application, 'quit-application');
  afterState.quitPresentation =
    await waitForPresentation(
      home,
      quitBaseline,
      context.candidate.head,
      quitApp,
      'quit-application',
    );
  const quitExit = await waitForApplicationExit(
    quitApp,
    4 * 60 * 1000,
    'QUIT_APPLICATION_DID_NOT_EXIT',
  );
  if (quitExit !== 0) {
    fail('QUIT_APPLICATION_EXIT_NONZERO', String(quitExit));
  }

  const servicePid = serviceAfterClose.pid;
  const runtimePid = runtimeAfterClose.pid;
  await waitUntil(() => !processAlive(servicePid), 120000, 'SERVICE_PROCESS_REMAINED_LIVE_AFTER_QUIT');
  await waitUntil(() => !processAlive(runtimePid), 120000, 'RUNTIME_PROCESS_REMAINED_LIVE_AFTER_QUIT');
  await waitUntil(
    () => !launchdLoaded(serviceAfterClose.target).loaded,
    120000,
    'SERVICE_LAUNCHD_REMAINED_AFTER_QUIT',
  );
  await waitUntil(
    () => !launchdLoaded(runtimeAfterClose.launchdTarget).loaded,
    120000,
    'RUNTIME_LAUNCHD_REMAINED_AFTER_QUIT',
  );
  const endpointAfterQuit = await optionalJson(`${SERVICE_ORIGIN}/api/v1/source-currentness`);
  if (endpointAfterQuit !== null) fail('PRODUCT_ENDPOINT_RESPONDED_AFTER_QUIT');
  completedStepReceipts.push({
    step: 'explicit-quit-released-product-runtime',
    state: 'PASS',
  });

  const homeStatAfterQuit = fs.lstatSync(home);
  if (!homeStatAfterQuit.isDirectory() || homeStatAfterQuit.isSymbolicLink()) {
    fail('HOME_NOT_PRESERVED_AFTER_QUIT');
  }

  const reopenBaseline = chronologyBaseline();
  console.log(
    'HUMAN_ACTION_REQUIRED THIRD: Reopen VexLife. If Start VexLife is offered, start it. Open VexLife, send one ordinary message and wait for the response. Do not Quit yet; wait for the next terminal instruction.',
  );
  const reopenApp = launchApplication(application, 'reopen-application');

  await waitForExactSource(context.candidate.head, 4 * 60 * 1000);
  const serviceReopen = await observeExactService(home, context.candidate.head);
  const runtimeReopen = observeLiveRuntime(home);
  const currentReopen = await getJson(`${SERVICE_ORIGIN}/api/v1/current-state`);
  const homeIdentityReopen = homeIdentityFromCurrentState(currentReopen);
  requireSameHomeIdentity(homeIdentityBefore, homeIdentityReopen);
  if (runtimeReopen.pid === runtimeBefore.pid) {
    fail('REOPEN_DID_NOT_FORM_FRESH_RUNTIME');
  }
  completedStepReceipts.push({
    step: 'same-home-preserved-after-quit',
    state: 'PASS',
  });
  completedStepReceipts.push({
    step: 'reopen-fresh-product-runtime',
    state: 'PASS',
  });

  const secondLived = await waitForPresentationAndHumanTurn(
    home,
    reopenBaseline,
    context.candidate.head,
    reopenApp,
    'reopen-application',
  );
  completedStepReceipts.push({
    step: 'second-human-turn-completed',
    state: 'PASS',
  });

  const locksBeforeFinalQuit = writerLeasePaths(home);
  if (locksBeforeFinalQuit.length > 0) {
    fail(
      'CONVERSATION_WRITER_LEASE_PRESENT_BEFORE_FINAL_QUIT_HELD',
      'writer lease count=' + locksBeforeFinalQuit.length,
    );
  }
  console.log(
    'HUMAN_ACTION_REQUIRED FOURTH: The second turn is complete. Now choose Quit VexLife for final cleanup.',
  );

  const finalExit = await waitForApplicationExit(
    reopenApp,
    4 * 60 * 1000,
    'FINAL_QUIT_APPLICATION_DID_NOT_EXIT',
  );
  if (finalExit !== 0) {
    fail('FINAL_APPLICATION_EXIT_NONZERO', String(finalExit));
  }

  await waitUntil(
    () => !processAlive(serviceReopen.pid),
    120000,
    'FINAL_SERVICE_PROCESS_REMAINED_LIVE',
  );
  await waitUntil(
    () => !processAlive(runtimeReopen.pid),
    120000,
    'FINAL_RUNTIME_PROCESS_REMAINED_LIVE',
  );
  const finalEndpoint = await optionalJson(`${SERVICE_ORIGIN}/api/v1/source-currentness`);
  if (finalEndpoint !== null) fail('FINAL_PRODUCT_ENDPOINT_RESPONDED_AFTER_QUIT');

  afterState.final = {
    homeIdentityBefore,
    homeIdentityReopen,
    firstTurn: firstLived,
    secondTurn: secondLived,
    firstServicePid: serviceBefore.pid,
    firstRuntimePid: runtimeBefore.pid,
    reopenedServicePid: serviceReopen.pid,
    reopenedRuntimePid: runtimeReopen.pid,
    finalProductEndpointAbsent: true,
    runtimeDirectKillPerformed: false,
    hostInstallationPerformed: false,
    frameworkManagedWebProfileAllowed: true,
  };
  completedStepReceipts.push({ step: 'final-quit-cleanup', state: 'PASS' });

  writeResult('PASS', null);
  console.log('VEXLIFE_PROD_APPLICATION_LIVED_PROOF_MAC=PASS');
}

try {
  await main();
} catch (error) {
  const code = error?.code || 'VEXLIFE_PROD_LIVED_PROOF_FAILED';
  harnessFindings.push(code);
  console.error(error?.stack || error?.message || String(error));

  const activeChildStillLive =
    activeApplication !== null && activeApplication.exitCode === null;
  if (activeChildStillLive) {
    unknownFindings.push('APPLICATION_CHILD_STILL_LIVE_CARRIER_PROCESS_GROUP_CLEANUP_REQUIRED');
  } else if (productMutationAttempted && setupPath && fs.existsSync(setupPath)) {
    const home = path.resolve(DEFAULT_HOME);
    if (writerLeasePaths(home).length === 0) {
      await recoverProduct(
        home,
        (readJson(path.join(process.cwd(), 'inputs', 'context.json'))).candidate.head,
      );
    } else {
      unknownFindings.push('RECOVERY_HELD_BY_CONVERSATION_WRITER_LEASE');
    }
  }

  if (!recoverySucceeded && recoveryAttempted) {
    unknownFindings.push('PRODUCT_LEFT_NONCURRENT_OR_STOPPED_AFTER_FAILED_PROOF');
  }

  afterState.failure = {
    code,
    message: String(error?.message || error),
  };
  writeResult('SAFE_FAILURE', code);
  process.exitCode = 1;
}
