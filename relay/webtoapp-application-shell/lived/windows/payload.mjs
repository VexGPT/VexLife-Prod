import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import http from 'node:http';
import { fileURLToPath } from 'node:url';
import { spawn, spawnSync } from 'node:child_process';

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-windows-payload-result/v1';
const POLICY_ENV = 'VEXLOCALBRIDGE_VEXLIFE_PROD_HOST_EFFECT_POLICY_JSON';
const RESULT_DIR = process.env.VEXLOCALBRIDGE_RESULT_DIR;
const CAPSULE_ROOT = process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
const PAYLOAD_FILE = fileURLToPath(import.meta.url);
const PACKAGE_ROOT = path.resolve(path.dirname(PAYLOAD_FILE), '..');
const DEFAULT_HOME = path.join(os.homedir(), '.vexlife');
const SERVICE_ORIGIN = 'http://127.0.0.1:18210';
const BUILD_FETCH_EFFECT =
  'effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1';
const APPLICATION_LIVED_EFFECT =
  'effect.vexlocalbridge.vexlife-prod.application-lived-proof/v1';
const LIFECYCLE_EFFECT =
  'effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1';

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
let activeApplication = null;
let lastRuntimePid = null;

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
    afterState,
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

function truncate(value, limit = 16000) {
  const text = String(value || '');
  return text.length <= limit ? text : text.slice(text.length - limit);
}

function run(executable, args, {
  cwd = process.cwd(),
  timeoutMs = 20 * 60 * 1000,
  env = {},
  acceptedExitCodes = [0],
  label = path.basename(executable),
} = {}) {
  if (!path.isAbsolute(executable)) {
    fail('EXECUTABLE_NOT_ABSOLUTE', executable);
  }
  const receipt = spawnSync(executable, args, {
    cwd,
    env: { ...process.env, ...env },
    encoding: 'utf8',
    timeout: timeoutMs,
    maxBuffer: 48 * 1024 * 1024,
    shell: false,
  });
  const exitCode = Number.isInteger(receipt.status) ? receipt.status : 127;
  const out = truncate(receipt.stdout);
  const err = truncate(receipt.stderr || receipt.error?.message || '');
  console.log('COMMAND ' + label + ' exit=' + exitCode);
  if (out) console.log(out);
  if (err) console.error(err);
  if (!acceptedExitCodes.includes(exitCode)) {
    const error = new Error(
      label + ' failed with exit ' + exitCode + ': ' + (err || out),
    );
    error.code =
      label.toUpperCase().replace(/[^A-Z0-9]+/g, '_') + '_FAILED';
    throw error;
  }
  return {
    exitCode,
    stdout: receipt.stdout || '',
    stderr: receipt.stderr || '',
  };
}

function processAlive(pid) {
  if (!Number.isInteger(pid) || pid <= 0) return false;
  try {
    process.kill(pid, 0);
    return true;
  } catch (error) {
    return error?.code === 'EPERM';
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitUntil(predicate, timeoutMs, code) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await predicate()) return;
    await sleep(500);
  }
  fail(code);
}

function httpJson(url, optional = false) {
  return new Promise((resolve, reject) => {
    const request = http.get(url, { timeout: 2500 }, (response) => {
      let body = '';
      response.setEncoding('utf8');
      response.on('data', (chunk) => {
        body += chunk;
      });
      response.on('end', () => {
        if (response.statusCode !== 200) {
          if (optional) {
            resolve(null);
            return;
          }
          reject(new Error('HTTP_' + response.statusCode + ':' + url));
          return;
        }
        try {
          resolve(JSON.parse(body));
        } catch (error) {
          if (optional) resolve(null);
          else reject(error);
        }
      });
    });
    request.on('timeout', () => request.destroy(new Error('HTTP_TIMEOUT')));
    request.on('error', (error) => {
      if (optional) resolve(null);
      else reject(error);
    });
  });
}

function getJson(url) {
  return httpJson(url, false);
}

function optionalJson(url) {
  return httpJson(url, true);
}

async function waitForExactSource(head, timeoutMs = 8 * 60 * 1000) {
  let current = null;
  await waitUntil(
    async () => {
      current = await optionalJson(SERVICE_ORIGIN + '/api/v1/source-currentness');
      return (
        current !== null &&
        current.sourceSha === head &&
        current.effectsPerformed === false &&
        Number.isInteger(current.processPid) &&
        current.processPid > 0
      );
    },
    timeoutMs,
    'EXACT_PRODUCT_SOURCE_NOT_CURRENT',
  );
  return current;
}

function writerLeasePaths(home) {
  const candidates = [
    path.join(home, 'runtime', 'vexlife-prod', 'conversation-writer.lock'),
    path.join(home, 'runtime', 'conversation', 'writer.lock'),
    path.join(home, 'conversation', 'writer.lock'),
  ];
  return candidates.filter((candidate) => fs.existsSync(candidate));
}

function validateDefaultHome(policy) {
  const configured = process.env.VEXLIFE_HOME;
  if (configured && path.resolve(configured) !== path.resolve(DEFAULT_HOME)) {
    fail('HELD_CUSTOM_HOME_NOT_YET_QUALIFIED', configured);
  }

  if (
    policy?.productDataPolicy?.existingHomeMode !== 'DEFAULT_EXISTING_HOME_ONLY' ||
    policy?.productDataPolicy?.customHomeDisposition !==
      'HELD_CUSTOM_HOME_NOT_YET_QUALIFIED' ||
    policy?.productDataPolicy?.homeRecreationAllowed !== false ||
    policy?.productDataPolicy?.homeDeletionAllowed !== false ||
    policy?.productDataPolicy?.conversationMutationAllowed !== false ||
    policy?.productDataPolicy?.memoryMutationAllowed !== false
  ) {
    fail('PRODUCT_DATA_POLICY_INVALID');
  }

  if (!fs.existsSync(DEFAULT_HOME)) {
    fail('DEFAULT_HOME_MISSING');
  }
  const entry = fs.lstatSync(DEFAULT_HOME);
  if (!entry.isDirectory() || entry.isSymbolicLink()) {
    fail('DEFAULT_HOME_NOT_QUALIFIED_DIRECTORY');
  }

  const locks = writerLeasePaths(DEFAULT_HOME);
  if (locks.length > 0) {
    fail(
      'CONVERSATION_WRITER_LEASE_PRESENT_BEFORE_LIFECYCLE_MUTATION_HELD',
      'writer lease count=' + locks.length,
    );
  }
  return path.resolve(DEFAULT_HOME);
}

function chronologyFiles(home) {
  const directory = path.join(
    home,
    'runtime',
    'vexlife-prod',
    'logs',
    'chronology',
  );
  if (!fs.existsSync(directory)) return [];
  return fs
    .readdirSync(directory, { withFileTypes: true })
    .filter(
      (entry) =>
        entry.isFile() &&
        !entry.isSymbolicLink() &&
        entry.name.startsWith('chronology') &&
        entry.name.endsWith('.jsonl'),
    )
    .map((entry) => path.join(directory, entry.name))
    .sort((left, right) => {
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
  const provider = records.findIndex(
    (row, index) =>
      index > send && row?.fact === 'PROVIDER_INVOCATION_REQUESTED',
  );
  if (provider < 0) return null;
  const completed = records.findIndex(
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

function assertWithin(root, target, code) {
  const rootAbs = path.resolve(root);
  const targetAbs = path.resolve(target);
  const relative = path.relative(rootAbs, targetAbs);
  if (
    relative.startsWith('..' + path.sep) ||
    relative === '..' ||
    path.isAbsolute(relative)
  ) {
    fail(code, targetAbs);
  }
  return targetAbs;
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

function runtimeReceiptFromCurrent(home, current) {
  if (!current?.runtimeReceiptPath) {
    fail('WINDOWS_RUNTIME_RECEIPT_PATH_MISSING');
  }
  const receiptPath = path.isAbsolute(current.runtimeReceiptPath)
    ? path.resolve(current.runtimeReceiptPath)
    : path.resolve(home, current.runtimeReceiptPath);
  assertWithin(home, receiptPath, 'WINDOWS_RUNTIME_RECEIPT_ESCAPED_HOME');
  if (!fs.existsSync(receiptPath)) {
    fail('WINDOWS_RUNTIME_RECEIPT_MISSING', receiptPath);
  }
  const receipt = readJson(receiptPath);
  if (
    !Number.isInteger(receipt.processPid) ||
    receipt.processPid <= 0 ||
    typeof receipt.runtimeExecutablePath !== 'string' ||
    !path.isAbsolute(receipt.runtimeExecutablePath) ||
    !Array.isArray(receipt.launchArguments)
  ) {
    fail('WINDOWS_RUNTIME_RECEIPT_INVALID');
  }
  if (!processAlive(receipt.processPid)) {
    fail('WINDOWS_RUNTIME_RECEIPT_PID_NOT_LIVE');
  }
  return {
    path: receiptPath,
    pid: receipt.processPid,
    executable: path.resolve(receipt.runtimeExecutablePath),
    args: receipt.launchArguments,
    startedAt: receipt.startedAt,
    runtimeReceiptRef:
      receipt.qualified?.runtimeReceiptRef ?? null,
  };
}

function exactWindowsApplication(sourceRoot, head, tree) {
  const appRoot = path.join(
    sourceRoot,
    'build',
    'windows-lived-preview',
    'image',
    'VexLife',
  );
  const receiptPath = path.join(appRoot, 'windows-lived-preview.receipt.json');
  if (!fs.existsSync(receiptPath)) {
    fail('WINDOWS_APPLICATION_RECEIPT_MISSING');
  }
  const receipt = readJson(receiptPath);
  if (
    receipt.schemaVersion !== 'vexlife-prod.windows-lived-preview-receipt/v2' ||
    receipt.sourceSha !== head ||
    receipt.sourceTree !== tree ||
    receipt.platform !== 'windows-x64' ||
    receipt.installer !== false ||
    receipt.windowsServiceInstalled !== false ||
    receipt.autoStartConfigured !== false ||
    receipt.entrypoint !== 'VexLife.exe' ||
    receipt.webView2RuntimeMode !== 'EVERGREEN_REQUIRED_FOR_LIVED_PREVIEW'
  ) {
    fail('WINDOWS_APPLICATION_RECEIPT_INVALID');
  }
  if (
    typeof receipt.webView2Shell !== 'string' ||
    receipt.webView2Shell.trim() === ''
  ) {
    fail('WINDOWS_WEBVIEW2_SHELL_COORDINATE_MISSING');
  }

  const executable = assertWithin(
    appRoot,
    path.join(appRoot, receipt.entrypoint),
    'WINDOWS_APPLICATION_EXECUTABLE_ESCAPED_APP',
  );
  const webView2Shell = assertWithin(
    appRoot,
    path.join(appRoot, receipt.webView2Shell),
    'WINDOWS_WEBVIEW2_SHELL_ESCAPED_APP',
  );

  for (const required of [executable, webView2Shell]) {
    if (!fs.existsSync(required) || !fs.statSync(required).isFile()) {
      fail('WINDOWS_APPLICATION_MEMBER_MISSING', required);
    }
  }

  return { appRoot, receiptPath, receipt, executable, webView2Shell };
}

function launchApplication(application, label) {
  const child = spawn(application.executable, [], {
    cwd: application.appRoot,
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

function writeProcessObserver(root) {
  const script = path.join(root, 'observe-vexlife-webview2.ps1');
  const lines = [
    'param([Int64]$ParentPid,[string]$ExpectedPath)',
    "$ErrorActionPreference = 'Stop'",
    '$Expected = [IO.Path]::GetFullPath($ExpectedPath)',
    '$Matches = @(',
    '  Get-CimInstance Win32_Process -Filter ("ParentProcessId = " + $ParentPid) |',
    '    Where-Object {',
    '      $_.ExecutablePath -and',
    '      [String]::Equals([IO.Path]::GetFullPath($_.ExecutablePath), $Expected, [StringComparison]::OrdinalIgnoreCase)',
    '    } |',
    '    ForEach-Object {',
    '      [ordered]@{',
    '        pid = [Int64]$_.ProcessId',
    '        parentPid = [Int64]$_.ParentProcessId',
    '        executablePath = [IO.Path]::GetFullPath($_.ExecutablePath)',
    '      }',
    '    }',
    ')',
    '[ordered]@{ count = $Matches.Count; matches = $Matches } | ConvertTo-Json -Depth 5 -Compress',
  ];
  fs.writeFileSync(script, lines.join('\r\n') + '\r\n', 'utf8');
  return script;
}

function helperObservation(powershell, observerScript, parentPid, expectedPath) {
  const receipt = run(
    powershell,
    [
      '-NoLogo',
      '-NoProfile',
      '-NonInteractive',
      '-File',
      observerScript,
      '-ParentPid',
      String(parentPid),
      '-ExpectedPath',
      expectedPath,
    ],
    { timeoutMs: 15000, label: 'observe-webview2-helper' },
  );
  try {
    return JSON.parse(receipt.stdout.trim() || '{}');
  } catch (error) {
    fail('WINDOWS_WEBVIEW2_HELPER_OBSERVATION_INVALID', error.message);
  }
}

async function waitForHelperAbsent(
  powershell,
  observerScript,
  parentPid,
  expectedPath,
  timeoutMs,
) {
  await waitUntil(
    () =>
      helperObservation(
        powershell,
        observerScript,
        parentPid,
        expectedPath,
      ).count === 0,
    timeoutMs,
    'WINDOWS_WEBVIEW2_HELPER_REMAINED_AFTER_CLOSE',
  );
}

async function waitForPresentationAndHumanTurn(
  home,
  baseline,
  head,
  application,
  powershell,
  observerScript,
  parentPid,
  label,
) {
  const deadline = Date.now() + 7 * 60 * 1000;
  let presentation = null;
  let helperObserved = false;

  while (Date.now() < deadline) {
    const records = candidateRecordsAfter(home, baseline, head);
    presentation =
      presentation ||
      records.find((row) => row?.fact === 'PRESENTATION_ARRIVED') ||
      null;
    helperObserved =
      helperObserved ||
      helperObservation(
        powershell,
        observerScript,
        parentPid,
        application.webView2Shell,
      ).count > 0;
    const turn = orderedTurnEvidence(records);

    if (presentation && helperObserved && turn) {
      return {
        presentation: {
          at: presentation.at,
          owner: presentation.owner,
          refs: presentation.refs,
        },
        helperObserved,
        turn,
      };
    }

    if (activeApplication?.exitCode !== null) {
      fail(
        'APPLICATION_EXITED_BEFORE_LIVED_TURN',
        label + ' exit=' + activeApplication.exitCode,
      );
    }
    await sleep(750);
  }
  fail('HUMAN_LIVED_TURN_NOT_OBSERVED', label);
}

async function waitForPresentation(
  home,
  baseline,
  head,
  application,
  powershell,
  observerScript,
  parentPid,
  label,
) {
  const deadline = Date.now() + 4 * 60 * 1000;
  let helperObserved = false;

  while (Date.now() < deadline) {
    const record = candidateRecordsAfter(home, baseline, head).find(
      (row) => row?.fact === 'PRESENTATION_ARRIVED',
    );
    helperObserved =
      helperObserved ||
      helperObservation(
        powershell,
        observerScript,
        parentPid,
        application.webView2Shell,
      ).count > 0;
    if (record && helperObserved) {
      return {
        at: record.at,
        owner: record.owner,
        refs: record.refs,
        helperObserved,
      };
    }
    if (activeApplication?.exitCode !== null) {
      fail(
        'APPLICATION_EXITED_BEFORE_PRESENTATION',
        label + ' exit=' + activeApplication.exitCode,
      );
    }
    await sleep(750);
  }
  fail('PRESENTATION_ARRIVAL_NOT_OBSERVED', label);
}

async function main() {
  if (!RESULT_DIR || !CAPSULE_ROOT) {
    fail('VEXLOCALBRIDGE_ENVIRONMENT_MISSING');
  }
  const context = readJson(path.join(PACKAGE_ROOT, 'inputs', 'context.json'));
  const policy = JSON.parse(process.env[POLICY_ENV] || 'null');
  if (!policy) fail('VEXLIFE_PROD_HOST_EFFECT_POLICY_MISSING');

  if (
    context?.proof?.platform !== 'windows' ||
    context?.candidate?.head !== 'd13e8e4a774aea60362491420ffeee2c07f91d1c' ||
    context?.candidate?.tree !== '98ba3219e49f722272c976171cb736ed19ae9ac9' ||
    context?.carrier?.candidateHead !== '36eb5c82becd97dcfcbc8fa67455a74d8540503a'
  ) {
    fail('IMMUTABLE_CONTEXT_MISMATCH');
  }

  if (
    policy.requiredAllowedEffectRef !== LIFECYCLE_EFFECT ||
    policy.lifecycleProofAuthorized !== true ||
    policy.applicationLivedProofEffectRef !== APPLICATION_LIVED_EFFECT ||
    policy.applicationLivedProofAuthorized !== true ||
    policy.buildDependencyFetchEffectRef !== BUILD_FETCH_EFFECT ||
    policy.buildDependencyFetchAuthorized !== true
  ) {
    fail('APPLICATION_LIVED_AUTHORITY_REQUIRED');
  }

  if (
    policy.applicationProofPolicy?.buildTaskOwnedApplicationAllowed !== true ||
    policy.applicationProofPolicy?.launchTaskOwnedApplicationAllowed !== true ||
    policy.applicationProofPolicy?.frameworkManagedWebProfileAllowed !== true ||
    policy.applicationProofPolicy?.hostInstallationAllowed !== false ||
    policy.applicationProofPolicy?.systemAutomationAllowed !== false ||
    policy.applicationProofPolicy?.screenCaptureAllowed !== false ||
    policy.processPolicy?.broadProcessKillAllowed !== false ||
    policy.processPolicy?.arbitraryPidTerminationAllowed !== false ||
    policy.productDataPolicy?.conversationMutationAllowed !== false ||
    policy.productDataPolicy?.memoryMutationAllowed !== false ||
    policy.modelPolicy?.acquisitionAllowed !== false ||
    policy.modelPolicy?.switchModelForProofAllowed !== false
  ) {
    fail('APPLICATION_LIVED_POLICY_TOO_BROAD_OR_INCOMPLETE');
  }

  const git = context?.hostTools?.git;
  const powershell = context?.hostTools?.powershell;
  if (
    typeof git !== 'string' ||
    !path.isAbsolute(git) ||
    typeof powershell !== 'string' ||
    !path.isAbsolute(powershell)
  ) {
    fail('QUALIFIED_WINDOWS_HOST_TOOLS_MISSING');
  }

  const home = validateDefaultHome(policy);
  beforeState.context = context;
  beforeState.policy = {
    lifecycleProofAuthorized: policy.lifecycleProofAuthorized,
    applicationLivedProofAuthorized: policy.applicationLivedProofAuthorized,
    buildDependencyFetchAuthorized: policy.buildDependencyFetchAuthorized,
    frameworkManagedWebProfileAllowed:
      policy.applicationProofPolicy.frameworkManagedWebProfileAllowed,
  };

  const bundle = path.join(PACKAGE_ROOT, 'inputs', 'vexlife-prod.bundle');
  if (!fs.existsSync(bundle)) fail('PRODUCT_BUNDLE_MISSING');

  const sourceRoot = path.join(process.cwd(), 'product-source');
  fs.rmSync(sourceRoot, { recursive: true, force: true });
  run(git, ['clone', bundle, sourceRoot], {
    timeoutMs: 5 * 60 * 1000,
    label: 'clone-product-bundle',
  });
  run(git, ['-C', sourceRoot, 'checkout', '--detach', context.candidate.head], {
    timeoutMs: 2 * 60 * 1000,
    label: 'checkout-product-head',
  });
  const observedHead = run(git, ['-C', sourceRoot, 'rev-parse', 'HEAD'], {
    label: 'product-head',
  }).stdout.trim();
  const observedTree = run(
    git,
    ['-C', sourceRoot, 'rev-parse', 'HEAD^{tree}'],
    { label: 'product-tree' },
  ).stdout.trim();
  if (
    observedHead !== context.candidate.head ||
    observedTree !== context.candidate.tree
  ) {
    fail('PRODUCT_SOURCE_IDENTITY_MISMATCH');
  }
  run(
    git,
    ['-C', sourceRoot, 'merge-base', '--is-ancestor', context.candidate.base, context.candidate.head],
    { label: 'product-base-ancestor' },
  );

  effects.providerOrNetworkEffectPerformed = true;
  const outputRoot = path.join(process.cwd(), 'formed-windows');
  fs.mkdirSync(outputRoot, { recursive: true });
  const buildEnv = {
    GRADLE_OPTS:
      '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process',
    DOTNET_SKIP_FIRST_TIME_EXPERIENCE: '1',
    DOTNET_NOLOGO: '1',
  };
  run(
    powershell,
    [
      '-NoLogo',
      '-NoProfile',
      '-NonInteractive',
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      path.join(sourceRoot, 'tools', 'windows-preview', 'FORM-WINDOWS-PREVIEW.ps1'),
      '-OutputDirectory',
      outputRoot,
    ],
    {
      cwd: sourceRoot,
      timeoutMs: 45 * 60 * 1000,
      env: buildEnv,
      label: 'form-windows-vexlife-application',
    },
  );

  const application = exactWindowsApplication(
    sourceRoot,
    context.candidate.head,
    context.candidate.tree,
  );
  afterState.application = {
    appRoot: application.appRoot,
    entrypoint: application.receipt.entrypoint,
    webView2Shell: application.receipt.webView2Shell,
    taskOwnedBuild: true,
    hostInstallationPerformed: false,
    webView2RuntimeMode: application.receipt.webView2RuntimeMode,
  };
  completedStepReceipts.push({
    step: 'exact-application-formed',
    state: 'PASS',
  });

  const observerScript = writeProcessObserver(process.cwd());

  const firstBaseline = chronologyBaseline();
  console.log(
    'HUMAN_ACTION_REQUIRED FIRST: In VexLife, if Start VexLife is offered choose it; if Set Up would acquire a model, do not continue. Open VexLife, send one ordinary message and wait for the response, then close only the Product/WebView2 window. Do not choose Quit VexLife.',
  );
  const firstApp = launchApplication(application, 'first-windows-application');
  const serviceBefore = await waitForExactSource(context.candidate.head);
  const currentBefore = await getJson(SERVICE_ORIGIN + '/api/v1/current-state');
  const homeIdentityBefore = homeIdentityFromCurrentState(currentBefore);
  const runtimeBefore = runtimeReceiptFromCurrent(home, currentBefore);
  lastRuntimePid = runtimeBefore.pid;
  effects.modelRuntimePerformed = true;

  beforeState.exactCandidateService = serviceBefore;
  beforeState.exactCandidateRuntime = runtimeBefore;
  beforeState.homeIdentity = homeIdentityBefore;
  completedStepReceipts.push({
    step: 'first-product-current',
    state: 'PASS',
  });

  const firstLived = await waitForPresentationAndHumanTurn(
    home,
    firstBaseline,
    context.candidate.head,
    application,
    powershell,
    observerScript,
    serviceBefore.processPid,
    'first-windows-application',
  );
  completedStepReceipts.push({
    step: 'first-human-turn-completed',
    state: 'PASS',
  });

  await waitForHelperAbsent(
    powershell,
    observerScript,
    serviceBefore.processPid,
    application.webView2Shell,
    4 * 60 * 1000,
  );

  const serviceAfterClose = await waitForExactSource(
    context.candidate.head,
    2 * 60 * 1000,
  );
  const currentAfterClose = await getJson(SERVICE_ORIGIN + '/api/v1/current-state');
  const runtimeAfterClose = runtimeReceiptFromCurrent(home, currentAfterClose);
  if (
    serviceAfterClose.processPid !== serviceBefore.processPid ||
    runtimeAfterClose.pid !== runtimeBefore.pid ||
    !processAlive(serviceBefore.processPid) ||
    !processAlive(runtimeBefore.pid)
  ) {
    fail('PRESENTATION_CLOSE_CHANGED_PRODUCT_LIFETIME');
  }
  afterState.firstSession = {
    lived: firstLived,
    servicePidBefore: serviceBefore.processPid,
    servicePidAfterClose: serviceAfterClose.processPid,
    runtimePidBefore: runtimeBefore.pid,
    runtimePidAfterClose: runtimeAfterClose.pid,
    parentApplicationStillLive: firstApp.exitCode === null,
    exactWebView2HelperAbsent: true,
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
    'HUMAN_ACTION_REQUIRED SECOND: In the VexLife window that returned after Close, choose Open VexLife again, then choose Quit VexLife from the Product presentation. Do not just close the Product window.',
  );
  afterState.quitPresentation = await waitForPresentation(
    home,
    quitBaseline,
    context.candidate.head,
    application,
    powershell,
    observerScript,
    serviceAfterClose.processPid,
    'quit-windows-application',
  );

  const firstExit = await waitForApplicationExit(
    firstApp,
    4 * 60 * 1000,
    'WINDOWS_APPLICATION_DID_NOT_EXIT_AFTER_QUIT',
  );
  if (firstExit !== 0) {
    fail('WINDOWS_APPLICATION_QUIT_EXIT_NONZERO', String(firstExit));
  }

  await waitUntil(
    () => !processAlive(serviceAfterClose.processPid),
    2 * 60 * 1000,
    'WINDOWS_PRODUCT_PROCESS_REMAINED_AFTER_QUIT',
  );
  await waitUntil(
    () => !processAlive(runtimeAfterClose.pid),
    2 * 60 * 1000,
    'WINDOWS_RUNTIME_PROCESS_REMAINED_AFTER_QUIT',
  );
  const endpointAfterQuit = await optionalJson(
    SERVICE_ORIGIN + '/api/v1/source-currentness',
  );
  if (endpointAfterQuit !== null) {
    fail('WINDOWS_PRODUCT_ENDPOINT_RESPONDED_AFTER_QUIT');
  }
  completedStepReceipts.push({
    step: 'explicit-quit-released-product-runtime',
    state: 'PASS',
  });

  const homeEntry = fs.lstatSync(home);
  if (!homeEntry.isDirectory() || homeEntry.isSymbolicLink()) {
    fail('HOME_NOT_PRESERVED_AFTER_QUIT');
  }

  const reopenBaseline = chronologyBaseline();
  console.log(
    'HUMAN_ACTION_REQUIRED THIRD: Reopen VexLife. If Start VexLife is offered choose it; if Set Up would acquire a model, do not continue. Open VexLife, send one ordinary message and wait for the response. Do not Quit yet; wait for the next terminal instruction.',
  );
  const reopenApp = launchApplication(application, 'reopen-windows-application');
  const serviceReopen = await waitForExactSource(context.candidate.head);
  const currentReopen = await getJson(SERVICE_ORIGIN + '/api/v1/current-state');
  const homeIdentityReopen = homeIdentityFromCurrentState(currentReopen);
  requireSameHomeIdentity(homeIdentityBefore, homeIdentityReopen);
  const runtimeReopen = runtimeReceiptFromCurrent(home, currentReopen);
  lastRuntimePid = runtimeReopen.pid;
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
    application,
    powershell,
    observerScript,
    serviceReopen.processPid,
    'reopen-windows-application',
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
    'FINAL_WINDOWS_APPLICATION_DID_NOT_EXIT',
  );
  if (finalExit !== 0) {
    fail('FINAL_WINDOWS_APPLICATION_EXIT_NONZERO', String(finalExit));
  }

  await waitUntil(
    () => !processAlive(serviceReopen.processPid),
    2 * 60 * 1000,
    'FINAL_WINDOWS_PRODUCT_PROCESS_REMAINED_LIVE',
  );
  await waitUntil(
    () => !processAlive(runtimeReopen.pid),
    2 * 60 * 1000,
    'FINAL_WINDOWS_RUNTIME_PROCESS_REMAINED_LIVE',
  );
  const finalEndpoint = await optionalJson(
    SERVICE_ORIGIN + '/api/v1/source-currentness',
  );
  if (finalEndpoint !== null) {
    fail('FINAL_WINDOWS_PRODUCT_ENDPOINT_RESPONDED_AFTER_QUIT');
  }

  afterState.final = {
    homeIdentityBefore,
    homeIdentityReopen,
    firstTurn: firstLived,
    secondTurn: secondLived,
    firstServicePid: serviceBefore.processPid,
    firstRuntimePid: runtimeBefore.pid,
    reopenedServicePid: serviceReopen.processPid,
    reopenedRuntimePid: runtimeReopen.pid,
    runtimeReceiptPersistsByContract: true,
    runtimeDirectKillPerformed: false,
    hostInstallationPerformed: false,
    frameworkManagedWebProfileAllowed: true,
  };
  completedStepReceipts.push({
    step: 'final-quit-cleanup',
    state: 'PASS',
  });

  writeResult('PASS', null);
  console.log('VEXLIFE_PROD_APPLICATION_LIVED_PROOF_WINDOWS=PASS');
}

try {
  await main();
  process.exitCode = 0;
} catch (error) {
  const code = error?.code || 'WINDOWS_APPLICATION_LIVED_PROOF_FAILED';
  console.error(code + ': ' + (error?.stack || error?.message || error));

  if (
    activeApplication !== null &&
    activeApplication.exitCode === null
  ) {
    unknownFindings.push(
      'APPLICATION_CHILD_STILL_LIVE_WINDOWS_JOB_OBJECT_CLEANUP_REQUIRED',
    );
  }
  if (lastRuntimePid && processAlive(lastRuntimePid)) {
    unknownFindings.push(
      'RUNTIME_PID_STILL_LIVE_WINDOWS_JOB_OBJECT_CLEANUP_REQUIRED',
    );
  }

  const value = String(error?.message || '');
  if (
    code.includes('BUILD') ||
    code.includes('JDK') ||
    code.includes('DOTNET') ||
    value.includes('JDK') ||
    value.includes('DOTNET') ||
    value.includes('WebView2')
  ) {
    environmentFindings.push(code);
  } else {
    harnessFindings.push(code);
  }

  writeResult('SAFE_FAILURE', code);
  process.exitCode = 1;
}

// [VXG RealForever]
