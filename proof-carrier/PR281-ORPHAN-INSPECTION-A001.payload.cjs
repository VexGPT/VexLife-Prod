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

function writeResult(value) {
  const root = process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if (!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR is unavailable');
  fs.mkdirSync(root, { recursive: true });
  fs.writeFileSync(
    path.join(root, 'payload-result.json'),
    JSON.stringify({ schemaVersion: RESULT_SCHEMA, acceptedExitCodes: [0], ...value }, null, 2) + '\n',
  );
}

function run(command, args, timeoutMs = 10000) {
  const result = spawnSync(command, args, {
    encoding: 'utf8',
    timeout: timeoutMs,
    maxBuffer: 8 * 1024 * 1024,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  return {
    status: Number.isInteger(result.status) ? result.status : 127,
    signal: result.signal || null,
    stdout: String(result.stdout || ''),
    stderr: String(result.stderr || (result.error ? result.error.message : '')),
  };
}

function executable(candidates) {
  return candidates.find((candidate) => fs.existsSync(candidate)) || null;
}

function safeJson(text) {
  try { return JSON.parse(text); } catch (_) { return null; }
}

function fileObservation(file) {
  const normalized = path.resolve(file);
  try {
    const stat = fs.lstatSync(normalized);
    if (stat.isSymbolicLink()) {
      return {
        path: normalized,
        exists: true,
        kind: 'SYMLINK',
        linkTarget: fs.readlinkSync(normalized),
      };
    }
    if (!stat.isFile()) {
      return {
        path: normalized,
        exists: true,
        kind: stat.isDirectory() ? 'DIRECTORY' : 'OTHER',
      };
    }
    const bytes = fs.readFileSync(normalized);
    const text = bytes.length <= 1024 * 1024 ? bytes.toString('utf8') : null;
    return {
      path: normalized,
      exists: true,
      kind: 'FILE',
      bytes: bytes.length,
      sha256: sha256(bytes),
      json: text === null ? null : safeJson(text),
      textOrNull: text,
    };
  } catch (error) {
    if (error && error.code === 'ENOENT') {
      return { path: normalized, exists: false, kind: 'ABSENT' };
    }
    return {
      path: normalized,
      exists: false,
      kind: 'UNREADABLE',
      error: String(error && error.message ? error.message : error),
    };
  }
}

function stripLaunchctlValue(raw) {
  let value = String(raw || '').trim();
  if (/^\d+\s*=\s*/.test(value)) value = value.replace(/^\d+\s*=\s*/, '');
  if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
    value = value.slice(1, -1);
  }
  return value;
}

function parseLaunchctl(stdout) {
  const lines = String(stdout || '').split(/\r?\n/);
  const pidMatch = String(stdout || '').match(/^\s*pid\s*=\s*(\d+)\s*$/m);
  const programMatch = String(stdout || '').match(/^\s*program\s*=\s*(.+)\s*$/m);
  const pathMatch = String(stdout || '').match(/^\s*path\s*=\s*(.+)\s*$/m);
  const start = lines.findIndex((line) => line.trim() === 'arguments = {');
  const args = [];
  if (start >= 0) {
    for (let i = start + 1; i < lines.length; i += 1) {
      const line = lines[i].trim();
      if (line === '}') break;
      if (line) args.push(stripLaunchctlValue(line));
    }
  }
  return {
    pid: pidMatch ? Number(pidMatch[1]) : null,
    programPath: programMatch ? stripLaunchctlValue(programMatch[1]) : null,
    plistPath: pathMatch ? stripLaunchctlValue(pathMatch[1]) : null,
    programArguments: args,
  };
}

function normalizedServiceArgs(parsed) {
  if (!parsed || !Array.isArray(parsed.programArguments)) return [];
  const args = [...parsed.programArguments];
  if (parsed.programPath && args[0] === parsed.programPath) return args.slice(1);
  return args;
}

function parseServiceLaunch(parsed) {
  const args = normalizedServiceArgs(parsed);
  let sourceSha = null;
  let homePath = null;
  for (let i = 0; i < args.length - 1; i += 1) {
    if (args[i] === '--source-sha') sourceSha = args[i + 1];
    if (args[i] === '--home') homePath = args[i + 1];
  }
  return {
    args,
    sourceSha,
    homePath: homePath && path.isAbsolute(homePath) ? path.normalize(homePath) : null,
  };
}

function curlGet(curl, url) {
  if (!curl) return { available: false, status: null, body: '', json: null, stderr: 'curl unavailable' };
  const marker = '\n__VEX_HTTP_STATUS__:';
  const result = run(curl, [
    '--silent', '--show-error', '--max-time', '4',
    '--write-out', marker + '%{http_code}',
    url,
  ], 6000);
  const index = result.stdout.lastIndexOf(marker);
  const body = index >= 0 ? result.stdout.slice(0, index) : result.stdout;
  const status = index >= 0 ? Number(result.stdout.slice(index + marker.length).trim()) : null;
  return {
    available: result.status === 0 && status !== null,
    commandExit: result.status,
    status,
    body,
    json: safeJson(body),
    stderr: result.stderr,
  };
}

function retainedWriter(root) {
  const files = [];
  function write(name, content) {
    const full = path.join(root, name);
    fs.mkdirSync(path.dirname(full), { recursive: true });
    const value = typeof content === 'string' ? content : JSON.stringify(content, null, 2) + '\n';
    fs.writeFileSync(full, value);
    const item = {
      sourcePath: full,
      targetPath: 'retained/orphan-inspection/' + name,
      expectedSha256: sha256(Buffer.from(value)),
    };
    files.push(item);
    return item.targetPath;
  }
  return { files, write };
}

async function main() {
  const configPath = process.argv[2];
  if (!configPath) throw new Error('inspection config path is required');
  const config = JSON.parse(fs.readFileSync(path.resolve(configPath), 'utf8'));
  const capsuleRoot = process.env.VEXLOCALBRIDGE_CAPSULE_ROOT;
  if (!capsuleRoot || !path.isAbsolute(capsuleRoot)) throw new Error('VEXLOCALBRIDGE_CAPSULE_ROOT is unavailable');

  const retainedRoot = path.join(
    capsuleRoot,
    'bridge',
    'retained-diagnostics',
    process.env.VEXLOCALBRIDGE_TASK_REF || 'task',
    process.env.VEXLOCALBRIDGE_ATTEMPT_REF || 'attempt',
  );
  fs.mkdirSync(retainedRoot, { recursive: true });
  const retained = retainedWriter(retainedRoot);
  const completedStepReceipts = [];

  try {
    if (process.platform !== 'darwin' || process.arch !== 'arm64') {
      throw new Error('PR281 orphan inspection requires Apple-Silicon macOS');
    }

    const launchctl = executable(['/bin/launchctl']);
    const id = executable(['/usr/bin/id']);
    const ps = executable(['/bin/ps', '/usr/bin/ps']);
    const lsof = executable(['/usr/sbin/lsof']);
    const curl = executable(['/usr/bin/curl']);
    if (!launchctl || !id || !ps) throw new Error('required macOS inspection executables are unavailable');

    const uidResult = run(id, ['-u']);
    if (uidResult.status !== 0) throw new Error('unable to resolve current uid');
    const uid = Number(uidResult.stdout.trim());
    const domain = 'gui/' + uid;
    const jobTarget = domain + '/' + config.serviceLabel;

    const launchctlResult = run(launchctl, ['print', jobTarget]);
    retained.write('product-service-launchctl.txt', [
      'command=' + launchctl + ' print ' + jobTarget,
      'exitCode=' + launchctlResult.status,
      '',
      '===== STDOUT =====',
      launchctlResult.stdout,
      '',
      '===== STDERR =====',
      launchctlResult.stderr,
      '',
    ].join('\n'));
    const launch = launchctlResult.status === 0 ? parseLaunchctl(launchctlResult.stdout) : {
      pid: null,
      programPath: null,
      plistPath: null,
      programArguments: [],
    };
    const launchIdentity = parseServiceLaunch(launch);
    completedStepReceipts.push({
      step: 'product-service-launchd-observation',
      state: launchctlResult.status === 0 ? 'PRESENT' : 'ABSENT_OR_UNKNOWN',
      pid: launch.pid,
    });

    let servicePs = null;
    if (launch.pid) {
      const psResult = run(ps, ['-ww', '-p', String(launch.pid), '-o', 'pid=,ppid=,uid=,lstart=,command=']);
      servicePs = {
        exitCode: psResult.status,
        stdout: psResult.stdout,
        stderr: psResult.stderr,
      };
      retained.write('product-service-process.txt', [
        'pid=' + launch.pid,
        'exitCode=' + psResult.status,
        '',
        '===== STDOUT =====',
        psResult.stdout,
        '',
        '===== STDERR =====',
        psResult.stderr,
        '',
      ].join('\n'));
    }

    let serviceListener = null;
    if (lsof) {
      const result = run(lsof, ['-nP', '-iTCP:' + String(config.servicePort), '-sTCP:LISTEN']);
      serviceListener = { exitCode: result.status, stdout: result.stdout, stderr: result.stderr };
      retained.write('product-service-listener.txt', result.stdout + (result.stderr ? '\nSTDERR:\n' + result.stderr : ''));
    }

    const sourceCurrentness = curlGet(curl, config.serviceOrigin + config.sourceCurrentnessPath);
    retained.write('source-currentness.txt', [
      'url=' + config.serviceOrigin + config.sourceCurrentnessPath,
      'httpStatus=' + String(sourceCurrentness.status),
      'commandExit=' + String(sourceCurrentness.commandExit),
      '',
      sourceCurrentness.body,
      sourceCurrentness.stderr ? '\nSTDERR:\n' + sourceCurrentness.stderr : '',
    ].join('\n'));

    const currentState = curlGet(curl, config.serviceOrigin + config.currentStatePath);
    retained.write('current-state.txt', [
      'url=' + config.serviceOrigin + config.currentStatePath,
      'httpStatus=' + String(currentState.status),
      'commandExit=' + String(currentState.commandExit),
      '',
      currentState.body,
      currentState.stderr ? '\nSTDERR:\n' + currentState.stderr : '',
    ].join('\n'));

    const userHome = os.homedir();
    const machineReceiptPath = path.join(userHome, config.machineLifetimeReceiptRelativePath);
    const machineReceipt = fileObservation(machineReceiptPath);

    const observedLaunchHome = launchIdentity.homePath;
    const defaultHome = path.join(userHome, '.vexlife');
    const homes = [...new Set([observedLaunchHome, defaultHome].filter(Boolean))];
    const homeEvidence = [];
    const runtimeEvidence = [];

    for (const home of homes) {
      const evidence = {
        homePath: home,
        homeRoot: fileObservation(home),
        legacyServiceReceipt: fileObservation(path.join(home, config.legacyServiceReceiptRelativePath)),
        servicePlist: fileObservation(path.join(home, config.servicePlistRelativePath)),
        productRuntimeReceipt: fileObservation(path.join(home, config.productRuntimeReceiptRelativePath)),
        stableRuntimeReceipt: fileObservation(path.join(home, config.stableRuntimeReceiptRelativePath)),
      };
      homeEvidence.push(evidence);

      for (const [kind, receiptObservation] of [
        ['product', evidence.productRuntimeReceipt],
        ['stable', evidence.stableRuntimeReceipt],
      ]) {
        const receipt = receiptObservation && receiptObservation.json;
        if (!receipt || !receipt.runtime) continue;
        const pid = Number(receipt.runtime.pid || 0) || null;
        const substrate = receipt.runtime.processSubstrate || null;
        const item = {
          homePath: home,
          kind,
          receiptPath: receiptObservation.path,
          receiptRef: receipt.receiptRef || null,
          state: receipt.state || null,
          endpoint: receipt.endpoint || null,
          pid,
          processGroupId: receipt.runtime.processGroupId || null,
          processSubstrate: substrate,
        };
        if (pid) {
          const psResult = run(ps, ['-ww', '-p', String(pid), '-o', 'pid=,ppid=,uid=,pgid=,lstart=,command=']);
          item.process = { exitCode: psResult.status, stdout: psResult.stdout, stderr: psResult.stderr };
          retained.write('runtime-' + kind + '-' + String(pid) + '-process.txt', psResult.stdout + (psResult.stderr ? '\nSTDERR:\n' + psResult.stderr : ''));
        }
        if (substrate && substrate.label && substrate.domainTarget) {
          const lr = run(launchctl, ['print', substrate.domainTarget + '/' + substrate.label]);
          item.launchdObservation = { exitCode: lr.status, parsed: lr.status === 0 ? parseLaunchctl(lr.stdout) : null };
          retained.write('runtime-' + kind + '-launchctl.txt', lr.stdout + (lr.stderr ? '\nSTDERR:\n' + lr.stderr : ''));
        }
        runtimeEvidence.push(item);
      }
    }

    retained.write('machine-service-lifetime-receipt.json', machineReceipt);
    retained.write('home-lifecycle-evidence.json', homeEvidence);
    retained.write('runtime-lifecycle-evidence.json', runtimeEvidence);

    let runtimeListener = null;
    if (lsof) {
      const result = run(lsof, ['-nP', '-iTCP:' + String(config.hereticPort), '-sTCP:LISTEN']);
      runtimeListener = { exitCode: result.status, stdout: result.stdout, stderr: result.stderr };
      retained.write('heretic-runtime-listener.txt', result.stdout + (result.stderr ? '\nSTDERR:\n' + result.stderr : ''));
    }

    const endpoint = sourceCurrentness.json;
    const launchShapeMatches =
      Boolean(
        launch.pid &&
        launch.programPath &&
        launchIdentity.sourceSha &&
        observedLaunchHome &&
        /^[0-9a-f]{40}$/.test(launchIdentity.sourceSha)
      ) &&
      path.normalize(launch.programPath) ===
        path.join(
          observedLaunchHome,
          'runtime/product-service/images',
          launchIdentity.sourceSha,
          'bin/vexlife-prod-service',
        );

    const endpointMatchesLaunch =
      Boolean(endpoint) &&
      endpoint.schemaVersion === config.sourceCurrentnessSchema &&
      endpoint.serviceRef === config.serviceRef &&
      endpoint.productRef === config.productRef &&
      endpoint.serviceState === 'SOURCE_BOUND_LISTENING' &&
      endpoint.effectsPerformed === false &&
      Number(endpoint.processPid) === Number(launch.pid) &&
      endpoint.sourceSha === launchIdentity.sourceSha;

    const legacyReceiptForLaunchHome =
      homeEvidence.find((item) => item.homePath === observedLaunchHome)?.legacyServiceReceipt || null;

    const summary = {
      historicalPreviewASourceSha: config.historicalPreviewASourceSha,
      launchdPresent: launchctlResult.status === 0,
      launchdPid: launch.pid,
      launchProgramPath: launch.programPath,
      launchSourceSha: launchIdentity.sourceSha,
      launchHomePath: observedLaunchHome,
      launchShapeMatchesAcceptedProductService: launchShapeMatches,
      sourceCurrentnessHttpStatus: sourceCurrentness.status,
      sourceCurrentness: endpoint,
      endpointMatchesLaunch,
      machineLifetimeReceiptPresent: machineReceipt.exists && machineReceipt.kind === 'FILE',
      launchHomeLegacyReceiptPresent: Boolean(
        legacyReceiptForLaunchHome &&
        legacyReceiptForLaunchHome.exists &&
        legacyReceiptForLaunchHome.kind === 'FILE'
      ),
      productServiceListenerObserved: serviceListener,
      hereticRuntimeListenerObserved: runtimeListener,
      runtimeReceiptCount: runtimeEvidence.length,
      strictLegacyOrphanWitnessCandidate:
        launchctlResult.status === 0 &&
        launchShapeMatches &&
        endpointMatchesLaunch &&
        !(machineReceipt.exists && machineReceipt.kind === 'FILE') &&
        !(
          legacyReceiptForLaunchHome &&
          legacyReceiptForLaunchHome.exists &&
          legacyReceiptForLaunchHome.kind === 'FILE'
        ),
    };

    retained.write('inspection-summary.json', summary);
    completedStepReceipts.push({ step: 'product-service-currentness-observation', state: 'PASS' });
    completedStepReceipts.push({ step: 'lifecycle-evidence-observation', state: 'PASS' });

    writeResult({
      disposition: 'PASS',
      stopReasonOrNull: null,
      firstSubstantiveFailureOrNull: null,
      candidateFindings: summary.strictLegacyOrphanWitnessCandidate
        ? ['STRICT_LEGACY_ORPHAN_WITNESS_CANDIDATE']
        : [],
      harnessFindings: [],
      environmentFindings: [],
      unknownFindings: [],
      effects: effects(),
      beforeState: {
        inspectionOnly: true,
        expectedServiceLabel: config.serviceLabel,
        expectedServiceOrigin: config.serviceOrigin,
      },
      afterState: {
        inspectionOnly: true,
        summary,
        homeEvidence: homeEvidence.map((item) => ({
          homePath: item.homePath,
          homeKind: item.homeRoot.kind,
          legacyServiceReceiptKind: item.legacyServiceReceipt.kind,
          productRuntimeReceiptKind: item.productRuntimeReceipt.kind,
          stableRuntimeReceiptKind: item.stableRuntimeReceipt.kind,
        })),
        runtimeEvidence: runtimeEvidence.map((item) => ({
          homePath: item.homePath,
          kind: item.kind,
          receiptRef: item.receiptRef,
          state: item.state,
          endpoint: item.endpoint,
          pid: item.pid,
          processGroupId: item.processGroupId,
          launchdLabel: item.processSubstrate && item.processSubstrate.label
            ? item.processSubstrate.label
            : null,
        })),
      },
      completedStepReceipts,
      retainedEvidenceFiles: retained.files,
    });
  } catch (error) {
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'PR281_ORPHAN_INSPECTION_STOPPED',
      firstSubstantiveFailureOrNull: {
        failureFamily: 'VEXLIFE_PROD_PR281_ORPHAN_INSPECTION',
        failureFingerprint: sha256(Buffer.from(String(error && error.message ? error.message : error))),
        stage: 'HOST_INSPECTION',
        summary: String(error && error.message ? error.message : error).slice(0, 4000),
        evidenceRef: 'payload-result.json',
      },
      candidateFindings: [],
      harnessFindings: ['ORPHAN_INSPECTION_STOPPED'],
      environmentFindings: [],
      unknownFindings: [],
      effects: effects(),
      beforeState: { inspectionOnly: true },
      afterState: { inspectionOnly: true },
      completedStepReceipts,
      retainedEvidenceFiles: retained.files,
    });
  }
}

main().catch((error) => {
  try {
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'UNCAUGHT_PR281_ORPHAN_INSPECTION_FAILURE',
      firstSubstantiveFailureOrNull: {
        failureFamily: 'VEXLIFE_PROD_PR281_ORPHAN_INSPECTION_UNCAUGHT',
        failureFingerprint: sha256(Buffer.from(String(error && error.message ? error.message : error))),
        stage: 'UNCAUGHT',
        summary: String(error && error.message ? error.message : error).slice(0, 4000),
        evidenceRef: 'payload-result.json',
      },
      candidateFindings: [],
      harnessFindings: ['UNCAUGHT_PR281_ORPHAN_INSPECTION_FAILURE'],
      environmentFindings: [],
      unknownFindings: [],
      effects: effects(),
      beforeState: { inspectionOnly: true },
      afterState: { inspectionOnly: true },
      completedStepReceipts: [],
      retainedEvidenceFiles: [],
    });
  } catch (_) {}
  process.exitCode = 0;
});

// [VXG RealForever]
