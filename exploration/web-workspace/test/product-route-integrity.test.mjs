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
  assert.match(launcher, /VEXLIFE_PROD_PROOF_VEXLIFE_SOURCE_REF:-main/u);
  assert.match(launcher, /VEXLIFE_PROD_PROOF_SOURCE_REF:-main/u);
  assert.match(launcher, /raw\.githubusercontent\.com\/\$VEXLIFE_REPOSITORY\/\$VEXLIFE_SOURCE_REF\/setup-vexlife\.command/u);
  assert.match(launcher, /commits\/\$VEXLIFE_SOURCE_REF/u);
  assert.doesNotMatch(launcher, /raw\.githubusercontent\.com\/\$VEXLIFE_REPOSITORY\/main\/setup-vexlife\.command/u);
  assert.match(launcher, /PROD_SOURCE_ROOT="\$HOME\/Library\/Application Support\/VexLife-Prod\/source"/u);
  assert.match(launcher, /PROD_RUNTIME_ROOT="\$HOME\/Library\/Application Support\/VexLife-Prod"/u);
  assert.match(launcher, /COMPANION_ORIGIN="http:\/\/127\.0\.0\.1:18110"/u);
  assert.match(launcher, /BACKEND_SETUP_MODE="window"/u);
  assert.doesNotMatch(launcher, /VEXLIFE_PROD_SOURCE_ROOT|VEXLIFE_PROD_RUNTIME_ROOT|VEXLIFE_COMPANION_ORIGIN|VEXLIFE_PROD_VEXLIFE_SETUP_MODE/u);
});
