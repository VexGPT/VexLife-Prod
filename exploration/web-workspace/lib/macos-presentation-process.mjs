import fs from 'node:fs';
import net from 'node:net';
import path from 'node:path';

export const PRESENTATION_PROCESS_SCHEMA = 'vexlife-prod.macos-presentation-process/v1';
export const PRESENTATION_SOURCE_CURRENTNESS_SCHEMA = 'vexlife-prod.presentation-source-currentness/v1';
const SHA = /^[0-9a-f]{40}$/u;

function assertObject(value, label) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new TypeError(`${label} must be one object`);
  return value;
}
function absolute(value, label) {
  if (typeof value !== 'string' || !path.isAbsolute(value)) throw new TypeError(`${label} must be one absolute path`);
  return path.resolve(value);
}
function exactSha(value) {
  if (typeof value !== 'string' || !SHA.test(value)) throw new TypeError('sourceSha must be one exact 40-character Git SHA');
  return value;
}
function exactLoopbackOrigin(value, label = 'origin') {
  const url = new URL(value);
  if (url.protocol !== 'http:' || url.hostname !== '127.0.0.1' || !url.port || url.pathname !== '/' || url.search || url.hash) {
    throw new TypeError(`${label} must be one numeric-loopback HTTP origin`);
  }
  return url.origin;
}
function writeJsonAtomic(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true, mode: 0o700 });
  const temporary = `${filePath}.partial-${process.pid}`;
  fs.writeFileSync(temporary, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600, flag: 'wx' });
  fs.renameSync(temporary, filePath);
}
function readJson(filePath) {
  try {
    const stat = fs.lstatSync(filePath);
    if (stat.isSymbolicLink() || !stat.isFile()) throw new Error('receipt must be one regular non-link file');
    return JSON.parse(fs.readFileSync(filePath, 'utf8'));
  } catch (error) {
    if (error?.code === 'ENOENT') return null;
    throw error;
  }
}
function sleepDefault(ms) { return new Promise((resolve) => setTimeout(resolve, ms)); }
function portOpen(port, { connect = net.createConnection } = {}) {
  return new Promise((resolve) => {
    const socket = connect({ host: '127.0.0.1', port });
    let settled = false;
    const done = (value) => {
      if (settled) return;
      settled = true;
      socket.destroy();
      resolve(value);
    };
    socket.once('connect', () => done(true));
    socket.once('error', () => done(false));
    socket.setTimeout?.(500, () => done(false));
  });
}

export function formPresentationConfig({
  sourceRoot,
  sourceSha,
  runtimeRoot,
  companionOrigin = 'http://127.0.0.1:18110',
  port = 18210,
  framePort = 18211,
} = {}) {
  const root = absolute(sourceRoot, 'sourceRoot');
  const runtime = absolute(runtimeRoot, 'runtimeRoot');
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new TypeError('port must be one valid TCP port');
  if (!Number.isInteger(framePort) || framePort < 1 || framePort > 65535 || framePort === port) throw new TypeError('framePort must be one different valid TCP port');
  const serverScriptPath = path.join(root, 'exploration', 'web-workspace', 'server.mjs');
  if (!fs.existsSync(serverScriptPath) || !fs.statSync(serverScriptPath).isFile()) throw new Error('VexLife-Prod presentation server is unavailable in exact source');
  return Object.freeze({
    sourceRoot: root,
    sourceSha: exactSha(sourceSha),
    runtimeRoot: runtime,
    companionOrigin: exactLoopbackOrigin(companionOrigin, 'companionOrigin'),
    port,
    framePort,
    origin: `http://127.0.0.1:${port}`,
    serverScriptPath,
    dataDir: path.join(runtime, 'data', 'web-workspace'),
    receiptPath: path.join(runtime, 'recovery', 'presentation-process.json'),
    plistPath: path.join(runtime, 'runtime', 'launchd', 'presentation.plist'),
    stdoutPath: path.join(runtime, 'runtime', 'presentation.out.log'),
    stderrPath: path.join(runtime, 'runtime', 'presentation.err.log'),
    label: 'com.vexlife.prod.presentation',
  });
}

export function validatePresentationReceipt(value) {
  const receipt = assertObject(value, 'presentation receipt');
  if (receipt.schemaVersion !== PRESENTATION_PROCESS_SCHEMA) throw new TypeError('presentation receipt schema is not current');
  if (!['RUNNING','STOPPED_BY_LIFECYCLE','STALE_STOPPED'].includes(receipt.state)) throw new TypeError('presentation receipt state is invalid');
  exactSha(receipt.sourceSha);
  absolute(receipt.sourceRootPath, 'receipt.sourceRootPath');
  absolute(receipt.serverScriptPath, 'receipt.serverScriptPath');
  absolute(receipt.dataDir, 'receipt.dataDir');
  exactLoopbackOrigin(receipt.companionOrigin, 'receipt.companionOrigin');
  exactLoopbackOrigin(receipt.origin, 'receipt.origin');
  if (!Number.isInteger(receipt.framePort) || receipt.framePort < 1 || receipt.framePort > 65535) throw new TypeError('receipt.framePort is invalid');
  assertObject(receipt.processSubstrate, 'receipt.processSubstrate');
  return Object.freeze(receipt);
}

async function sourceCurrentness(origin, expectedSha, fetchImpl = fetch) {
  try {
    const response = await fetchImpl(`${origin}/api/source-currentness`, { cache: 'no-store' });
    const body = await response.json().catch(() => null);
    if (!response.ok || body?.schemaVersion !== PRESENTATION_SOURCE_CURRENTNESS_SCHEMA || body?.effectsPerformed !== false || !SHA.test(body?.sourceSha ?? '')) {
      return Object.freeze({ state: 'UNAVAILABLE', sourceSha: null });
    }
    return Object.freeze({ state: body.sourceSha === expectedSha ? 'CURRENT' : 'PRIOR_SOURCE', sourceSha: body.sourceSha });
  } catch {
    return Object.freeze({ state: 'UNAVAILABLE', sourceSha: null });
  }
}

export async function observePresentation(config, {
  substrate,
  fetchImpl = fetch,
} = {}) {
  if (!substrate || typeof substrate.validateMacUserLaunchdProcessReceipt !== 'function' || typeof substrate.observeMacUserLaunchdProcess !== 'function') {
    throw new TypeError('presentation lifecycle requires the accepted VexLife macOS launchd substrate');
  }
  const raw = readJson(config.receiptPath);
  if (raw === null) return Object.freeze({ state: 'ABSENT', receipt: null, sourceCurrentness: null });
  const receipt = validatePresentationReceipt(raw);
  const processSubstrate = substrate.validateMacUserLaunchdProcessReceipt(receipt.processSubstrate);
  const process = substrate.observeMacUserLaunchdProcess(processSubstrate);
  if (process.state === 'UNKNOWN') return Object.freeze({ state: 'HELD_UNKNOWN', receipt, process, sourceCurrentness: null });
  if (process.state !== 'RUNNING') return Object.freeze({ state: 'STOPPED_OWNED', receipt, process, sourceCurrentness: null });
  if (process.pid !== processSubstrate.pid) return Object.freeze({ state: 'HELD_UNKNOWN', receipt, process, sourceCurrentness: null });
  const currentness = await sourceCurrentness(receipt.origin, config.sourceSha, fetchImpl);
  if (currentness.state === 'CURRENT' && receipt.sourceSha === config.sourceSha) {
    return Object.freeze({ state: 'RUNNING_CURRENT', receipt, process, sourceCurrentness: currentness });
  }
  if (currentness.state === 'PRIOR_SOURCE' || receipt.sourceSha !== config.sourceSha) {
    return Object.freeze({ state: 'RUNNING_PRIOR_SOURCE', receipt, process, sourceCurrentness: currentness });
  }
  return Object.freeze({ state: 'RUNNING_UNRESPONSIVE_OWNED', receipt, process, sourceCurrentness: currentness });
}

async function retireReceipt(observed, config, substrate) {
  if (observed.receipt === null) return { disposition: 'NO_RECEIPT' };
  const processSubstrate = substrate.validateMacUserLaunchdProcessReceipt(observed.receipt.processSubstrate);
  const retired = await substrate.retireMacUserLaunchdProcess(processSubstrate, { expectedPid: processSubstrate.pid });
  if (retired?.exactOwnershipVerified !== true || retired?.verifiedNotLive !== true) throw new Error('presentation launchd retirement did not prove exact process stop');
  const stopped = { ...observed.receipt, state: 'STOPPED_BY_LIFECYCLE', stoppedAt: new Date().toISOString() };
  writeJsonAtomic(config.receiptPath, stopped);
  return retired;
}

async function waitForReady(config, {
  fetchImpl = fetch,
  sleep = sleepDefault,
  maxAttempts = 60,
  intervalMs = 250,
} = {}) {
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const currentness = await sourceCurrentness(config.origin, config.sourceSha, fetchImpl);
    if (currentness.state === 'CURRENT') {
      try {
        const response = await fetchImpl(`${config.origin}/api/session`, { cache: 'no-store' });
        if (response.ok) return Object.freeze({ state: 'READY', attempt, currentness });
      } catch {}
    }
    if (attempt < maxAttempts) await sleep(intervalMs);
  }
  throw new Error('VexLife-Prod presentation did not reach exact-source readiness');
}

export async function startPresentation(config, {
  substrate,
  fetchImpl = fetch,
  sleep = sleepDefault,
  now = () => new Date(),
  portOpenImpl = portOpen,
} = {}) {
  if (!substrate || typeof substrate.startMacUserLaunchdProcess !== 'function' || typeof substrate.retireMacUserLaunchdProcess !== 'function') {
    throw new TypeError('presentation lifecycle requires start/retire from the accepted VexLife macOS launchd substrate');
  }
  let observed = await observePresentation(config, { substrate, fetchImpl });
  if (observed.state === 'RUNNING_CURRENT') {
    const ready = await waitForReady(config, { fetchImpl, sleep, maxAttempts: 1, intervalMs: 0 });
    return Object.freeze({ schemaVersion: PRESENTATION_PROCESS_SCHEMA, state: 'REUSED_CURRENT', origin: config.origin, sourceSha: config.sourceSha, pid: observed.receipt.processSubstrate.pid, ready });
  }
  if (observed.state === 'HELD_UNKNOWN') throw new Error('presentation lifecycle is held because exact process ownership is unknown');
  if (observed.receipt !== null) {
    await retireReceipt(observed, config, substrate);
    observed = await observePresentation(config, { substrate, fetchImpl });
    if (observed.state !== 'STOPPED_OWNED') throw new Error(`presentation lifecycle did not settle after exact retirement: ${observed.state}`);
  }
  if (await portOpenImpl(config.port) || await portOpenImpl(config.framePort)) {
    throw new Error('VexLife-Prod presentation ports are occupied without a current exact-owned presentation');
  }
  fs.mkdirSync(config.dataDir, { recursive: true, mode: 0o700 });
  const processSubstrate = await substrate.startMacUserLaunchdProcess({
    label: config.label,
    plistPath: config.plistPath,
    programPath: process.execPath,
    programArguments: [
      config.serverScriptPath,
      '--port', String(config.port),
      '--frame-port', String(config.framePort),
      '--data', config.dataDir,
      '--companion-origin', config.companionOrigin,
      '--source-sha', config.sourceSha,
    ],
    workingDirectory: config.sourceRoot,
    stdoutPath: config.stdoutPath,
    stderrPath: config.stderrPath,
    environment: Object.fromEntries(['HOME', 'PATH', 'TMPDIR', 'LANG', 'LC_ALL', 'USER']
      .filter((key) => typeof process.env[key] === 'string' && process.env[key].length > 0)
      .map((key) => [key, process.env[key]])),
  });
  const receipt = {
    schemaVersion: PRESENTATION_PROCESS_SCHEMA,
    state: 'RUNNING',
    sourceSha: config.sourceSha,
    sourceRootPath: config.sourceRoot,
    serverScriptPath: config.serverScriptPath,
    companionOrigin: config.companionOrigin,
    origin: config.origin,
    framePort: config.framePort,
    dataDir: config.dataDir,
    formedAt: now().toISOString(),
    processSubstrate,
  };
  try {
    writeJsonAtomic(config.receiptPath, receipt);
  } catch (error) {
    const retired = await substrate.retireMacUserLaunchdProcess(processSubstrate, { expectedPid: processSubstrate.pid });
    if (retired?.exactOwnershipVerified !== true || retired?.verifiedNotLive !== true) throw new Error(`presentation receipt write failed and exact launchd cleanup was not verified: ${error.message}`);
    throw error;
  }
  try {
    const ready = await waitForReady(config, { fetchImpl, sleep });
    return Object.freeze({ schemaVersion: PRESENTATION_PROCESS_SCHEMA, state: 'STARTED_CURRENT', origin: config.origin, sourceSha: config.sourceSha, pid: processSubstrate.pid, ready });
  } catch (error) {
    const retired = await substrate.retireMacUserLaunchdProcess(processSubstrate, { expectedPid: processSubstrate.pid });
    const stopped = { ...receipt, state: 'STALE_STOPPED', stoppedAt: now().toISOString(), failure: error.message };
    writeJsonAtomic(config.receiptPath, stopped);
    if (retired?.exactOwnershipVerified !== true || retired?.verifiedNotLive !== true) throw new Error(`presentation readiness failed and exact launchd cleanup was not verified: ${error.message}`);
    throw error;
  }
}

export async function stopPresentation(config, { substrate, fetchImpl = fetch } = {}) {
  const observed = await observePresentation(config, { substrate, fetchImpl });
  if (observed.receipt === null) return Object.freeze({ schemaVersion: PRESENTATION_PROCESS_SCHEMA, state: 'ALREADY_STOPPED' });
  if (observed.state === 'HELD_UNKNOWN') throw new Error('presentation lifecycle is held because exact process ownership is unknown');
  await retireReceipt(observed, config, substrate);
  return Object.freeze({ schemaVersion: PRESENTATION_PROCESS_SCHEMA, state: 'STOPPED' });
}
