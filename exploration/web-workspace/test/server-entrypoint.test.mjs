import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const SERVER = fileURLToPath(new URL('../server.mjs', import.meta.url));

async function waitForOrigin(child, output, timeoutMs = 5000) {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    const match = /VexLife web workspace \(exploration\) on (http:\/\/127\.0\.0\.1:\d+)/u.exec(output.value);
    if (match) return match[1];
    if (child.exitCode !== null) throw new Error(`server exited before listening: ${output.value}`);
    await new Promise((resolve) => setTimeout(resolve, 25));
  }
  throw new Error(`server did not listen through symlink entry: ${output.value}`);
}

test('direct server entry survives a symlinked argv path and reports exact presentation source', async () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-prod-realpath-entry-'));
  const linked = path.join(root, 'server-link.mjs');
  const dataDir = path.join(root, 'data');
  fs.symlinkSync(SERVER, linked);
  const sourceSha = 'c'.repeat(40);
  const output = { value: '' };
  const child = spawn(process.execPath, [
    linked,
    '--port', '0',
    '--frame-port', '0',
    '--data', dataDir,
    '--companion-origin', 'http://127.0.0.1:9',
    '--source-sha', sourceSha
  ], { stdio: ['ignore', 'pipe', 'pipe'] });
  child.stdout.setEncoding('utf8');
  child.stderr.setEncoding('utf8');
  child.stdout.on('data', (chunk) => { output.value += chunk; });
  child.stderr.on('data', (chunk) => { output.value += chunk; });

  try {
    const origin = await waitForOrigin(child, output);
    const currentness = await fetch(`${origin}/api/source-currentness`).then((response) => response.json());
    assert.deepEqual(currentness, {
      schemaVersion: 'vexlife-prod.presentation-source-currentness/v1',
      state: 'CURRENT',
      sourceSha,
      effectsPerformed: false
    });
    const session = await fetch(`${origin}/api/session`);
    assert.equal(session.status, 200);
  } finally {
    if (child.exitCode === null) {
      child.kill('SIGTERM');
      await new Promise((resolve) => child.once('exit', resolve));
    }
    fs.rmSync(root, { recursive: true, force: true });
  }
});
