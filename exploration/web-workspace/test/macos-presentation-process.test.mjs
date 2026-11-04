import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import {
  formPresentationConfig,
  observePresentation,
  startPresentation,
  stopPresentation
} from '../lib/macos-presentation-process.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const LAUNCHER = path.resolve(HERE, '..', '..', '..', 'open-vexlife.command');

function fakeLaunchdSubstrate() {
  const processes = new Map();
  let nextPid = 41000;
  return {
    validateMacUserLaunchdProcessReceipt(value) { return value; },
    observeMacUserLaunchdProcess(value) {
      const record = processes.get(value.label);
      if (!record || record.child.exitCode !== null) return { state: 'ABSENT', pid: null };
      return { state: 'RUNNING', pid: record.receipt.pid };
    },
    async startMacUserLaunchdProcess(spec) {
      const child = spawn(spec.programPath, spec.programArguments, {
        cwd: spec.workingDirectory,
        env: { ...process.env, ...spec.environment },
        stdio: ['ignore', 'ignore', 'ignore']
      });
      const receipt = {
        schemaVersion: 'vexlife.macos-user-launchd-process/v1',
        substrateRef: 'substrate.vexlife.macos-user-launchd-process.001',
        label: spec.label,
        domainTarget: 'gui/501',
        plistPath: spec.plistPath,
        programPath: spec.programPath,
        programArguments: spec.programArguments,
        workingDirectory: spec.workingDirectory,
        stdoutPath: spec.stdoutPath,
        stderrPath: spec.stderrPath,
        keepAlive: false,
        pid: ++nextPid
      };
      processes.set(spec.label, { child, receipt });
      return receipt;
    },
    async retireMacUserLaunchdProcess(value) {
      const record = processes.get(value.label);
      if (record?.child && record.child.exitCode === null) {
        record.child.kill('SIGTERM');
        await new Promise((resolve) => record.child.once('exit', resolve));
      }
      processes.delete(value.label);
      return { state: 'RETIRED_VERIFIED', exactOwnershipVerified: true, verifiedNotLive: true };
    }
  };
}

function makeSource(root, name, sourceSha, runtimeRoot, port, framePort) {
  const sourceRoot = path.join(root, name);
  const serverDir = path.join(sourceRoot, 'exploration', 'web-workspace');
  fs.mkdirSync(serverDir, { recursive: true });
  fs.writeFileSync(path.join(serverDir, 'server.mjs'), `
import http from 'node:http';
const values = process.argv.slice(2);
const args = {};
for (let i = 0; i < values.length; i += 2) args[values[i].slice(2)] = values[i + 1];
const sourceSha = args['source-sha'];
const server = http.createServer((request, response) => {
  if (request.url === '/api/source-currentness') {
    response.writeHead(200, { 'content-type': 'application/json' });
    response.end(JSON.stringify({
      schemaVersion: 'vexlife-prod.presentation-source-currentness/v1',
      state: 'CURRENT',
      sourceSha,
      effectsPerformed: false
    }));
    return;
  }
  if (request.url === '/api/session') {
    response.writeHead(200, { 'content-type': 'application/json' });
    response.end(JSON.stringify({ schemaVersion: 'vexlife-prod.exploration.session/v2' }));
    return;
  }
  response.writeHead(404);
  response.end();
});
server.listen(Number(args.port), '127.0.0.1');
process.on('SIGTERM', () => server.close(() => process.exit(0)));
`);
  return formPresentationConfig({ sourceRoot, sourceSha, runtimeRoot, port, framePort });
}

test('Prod presentation process owns only start/reuse/source-rotation/stop over an injected launchd substrate', async () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-prod-presentation-process-'));
  const runtimeRoot = path.join(root, 'runtime-root');
  const substrate = fakeLaunchdSubstrate();
  try {
    const firstConfig = makeSource(root, 'source-a', 'a'.repeat(40), runtimeRoot, 19210, 19211);
    const first = await startPresentation(firstConfig, { substrate });
    assert.equal(first.state, 'STARTED_CURRENT');
    assert.equal((await observePresentation(firstConfig, { substrate })).state, 'RUNNING_CURRENT');

    const reused = await startPresentation(firstConfig, { substrate });
    assert.equal(reused.state, 'REUSED_CURRENT');

    const secondConfig = makeSource(root, 'source-b', 'b'.repeat(40), runtimeRoot, 19210, 19211);
    const rotated = await startPresentation(secondConfig, { substrate });
    assert.equal(rotated.state, 'STARTED_CURRENT');
    assert.equal((await observePresentation(secondConfig, { substrate })).state, 'RUNNING_CURRENT');

    const stopped = await stopPresentation(secondConfig, { substrate });
    assert.equal(stopped.state, 'STOPPED');
    assert.equal((await observePresentation(secondConfig, { substrate })).state, 'STOPPED_OWNED');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('Mac front door delegates backend setup/repair to current VexLife before starting Prod presentation', { skip: process.platform === 'win32' }, () => {
  const source = fs.readFileSync(LAUNCHER, 'utf8');
  const syntax = spawnSync('/bin/bash', ['-n', LAUNCHER], { encoding: 'utf8' });
  assert.equal(syntax.status, 0, syntax.stderr);

  const backend = source.indexOf('setup-vexlife.command');
  const backendRun = source.indexOf('/bin/bash "$BACKEND_BOOTSTRAP"');
  const prodResolve = source.indexOf('Getting current VexLife Prod presentation source');
  const presentationStart = source.indexOf('tools/macos-presentation.mjs');

  assert.ok(backend >= 0);
  assert.ok(backendRun > backend);
  assert.ok(prodResolve > backendRun);
  assert.ok(presentationStart > prodResolve);
  assert.match(source, /raw\.githubusercontent\.com\/\$VEXLIFE_REPOSITORY\/\$VEX_SOURCE_SHA\/src\/core\/macos-user-launchd-process\.mjs/u);
  assert.match(source, /VEXLIFE_SETUP_COMPLETION="return-ready"/u);
  assert.match(source, /BACKEND_SETUP_MODE="window"/u);
  assert.match(source, /COMPANION_ORIGIN="http:\/\/127\.0\.0\.1:18110"/u);
  assert.doesNotMatch(source, /VEXLIFE_PROD_SOURCE_ROOT|VEXLIFE_PROD_RUNTIME_ROOT|VEXLIFE_COMPANION_ORIGIN|VEXLIFE_PROD_VEXLIFE_SETUP_MODE/u);
  assert.match(source, /VEXLIFE_SOURCE_REF="\$VEXLIFE_SOURCE_REF"/u);
  assert.match(source, /commits\/\$VEXLIFE_SOURCE_REF/u);
  assert.match(source, /canonical VexLife lifecycle did not finish with Companion READY/u);
  assert.doesNotMatch(source, /--operation (?:repair|rebuild-preserve|uninstall-preserve)/u);
  assert.doesNotMatch(source, /setup-vexlife\.command"[^\n]*main/u);
});
