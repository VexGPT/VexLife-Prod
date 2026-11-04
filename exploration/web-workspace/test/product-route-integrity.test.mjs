import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, '..', '..', '..');
const workspace = fs.readFileSync(path.join(ROOT, 'exploration', 'web-workspace', 'public', 'workspace.mjs'), 'utf8');
const launcher = fs.readFileSync(path.join(ROOT, 'open-vexlife.command'), 'utf8');

test('ordinary product recovery copy does not promote the Terminal fallback', () => {
  assert.match(workspace, /BridgeUnreachable: 'This workspace lost its local service\. Reopen VexLife to reconnect\.'/u);
  assert.doesNotMatch(workspace, /Restart it from Terminal/u);
});

test('Prod consumes VexLife lifecycle readiness without opening the reference presentation', () => {
  assert.match(launcher, /VEXLIFE_SETUP_COMPLETION="return-ready"/u);
  assert.match(launcher, /VEXLIFE_PROD_VEXLIFE_SOURCE_REF:-main/u);
  assert.match(launcher, /raw\.githubusercontent\.com\/\$VEXLIFE_REPOSITORY\/\$VEXLIFE_SOURCE_REF\/setup-vexlife\.command/u);
  assert.match(launcher, /commits\/\$VEXLIFE_SOURCE_REF/u);
  assert.doesNotMatch(launcher, /raw\.githubusercontent\.com\/\$VEXLIFE_REPOSITORY\/main\/setup-vexlife\.command/u);
});
