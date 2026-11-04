import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const SERVER = fileURLToPath(new URL('../server.mjs', import.meta.url));
const LAUNCHER = fileURLToPath(new URL('../../../open-vexlife.command', import.meta.url));

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function waitForFile(filePath, child, timeoutMs = 5000) {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    if (fs.existsSync(filePath)) return;
    if (child.exitCode !== null) throw new Error(`workspace server exited before startup receipt (code ${child.exitCode})`);
    await sleep(25);
  }
  throw new Error('workspace startup receipt did not appear');
}

test('direct workspace CLI binds fresh loopback ports and emits an exact-source startup receipt', async () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-prod-startup-'));
  const readyFile = path.join(root, 'workspace-ready.json');
  const dataDir = path.join(root, 'data');
  const sourceSha = 'f'.repeat(40);
  const child = spawn(process.execPath, [
    SERVER,
    '--port', '0',
    '--frame-port', '0',
    '--data', dataDir,
    '--companion-origin', 'http://127.0.0.1:9',
    '--ready-file', readyFile
  ], {
    env: { ...process.env, VEXLIFE_PROD_SOURCE_SHA: sourceSha },
    stdio: ['ignore', 'pipe', 'pipe']
  });

  let stdout = '';
  let stderr = '';
  child.stdout.setEncoding('utf8');
  child.stderr.setEncoding('utf8');
  child.stdout.on('data', (chunk) => { stdout += chunk; });
  child.stderr.on('data', (chunk) => { stderr += chunk; });

  try {
    await waitForFile(readyFile, child);
    const receipt = JSON.parse(fs.readFileSync(readyFile, 'utf8'));
    assert.equal(receipt.schemaVersion, 'vexlife-prod.workspace-startup/v1');
    assert.equal(receipt.sourceSha, sourceSha);
    assert.match(receipt.origin, /^http:\/\/127\.0\.0\.1:\d+$/u);
    assert.equal(Number.isInteger(receipt.port) && receipt.port > 0, true);
    assert.equal(Number.isInteger(receipt.framePort) && receipt.framePort > 0, true);
    assert.notEqual(receipt.port, receipt.framePort);
    assert.equal(receipt.dataDir, dataDir);

    const sessionResponse = await fetch(`${receipt.origin}/api/session`);
    assert.equal(sessionResponse.status, 200);
    const session = await sessionResponse.json();
    assert.equal(session.schemaVersion, 'vexlife-prod.exploration.session/v2');
    assert.equal(session.canonicalConversation, true);
    assert.equal(session.frameHost.port, receipt.framePort);
    assert.match(stdout, /VexLife web workspace \(exploration\) on http:\/\/127\.0\.0\.1:\d+/u);
    assert.equal(stderr, '');
  } finally {
    if (child.exitCode === null) {
      child.kill('SIGTERM');
      await once(child, 'exit');
    }
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('Mac front door defaults to collision-free ports and waits for the source-bound startup receipt', () => {
  const launcher = fs.readFileSync(LAUNCHER, 'utf8');
  assert.match(launcher, /WORKSPACE_PORT="\$\{VEXLIFE_PROD_PORT:-0\}"/u);
  assert.match(launcher, /FRAME_PORT="\$\{VEXLIFE_PROD_FRAME_PORT:-0\}"/u);
  assert.match(launcher, /--ready-file "\$READY_FILE"/u);
  assert.match(launcher, /STARTUP_SOURCE=.*sourceSha/u);
  assert.match(launcher, /STARTUP_SOURCE" = "\$SOURCE_SHA"/u);
  assert.match(launcher, /VexLife Prod startup log:/u);
  assert.match(launcher, /--noproxy '\*'/u);
});
