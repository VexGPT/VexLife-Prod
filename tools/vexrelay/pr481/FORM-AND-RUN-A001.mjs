#!/usr/bin/env node
// [VXG RealForever]
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { pathToFileURL } from 'node:url';
import { spawnSync } from 'node:child_process';

const [configPath, sdkRoot, outputDirectory, metadataPath] = process.argv.slice(2);
if (!configPath || !sdkRoot || !outputDirectory || !metadataPath) {
  throw new Error('USAGE: FORM-AND-RUN-A001.mjs <config> <sdkRoot> <outputDirectory> <metadataPath>');
}
const cfg = JSON.parse(fs.readFileSync(configPath, 'utf8'));
const contract = JSON.parse(fs.readFileSync(path.join(sdkRoot, 'docs/private-continuity/vexrelay/current-contract.json'), 'utf8'));
if (contract.contractRef !== cfg.sdk.contractRef) throw new Error(\`CURRENT_CONTRACT_REF_MISMATCH:\${contract.contractRef}\`);
if (contract.currentRuntimeVersion !== cfg.sdk.runtimeVersion) throw new Error(\`CURRENT_RUNTIME_VERSION_MISMATCH:\${contract.currentRuntimeVersion}\`);

const formation = await import(pathToFileURL(path.join(sdkRoot, 'lib/vexlocalbridge/vexrelay-task-archive-carrier-formation.mjs')).href);
const native = (await import(pathToFileURL(path.join(sdkRoot, 'lib/vexlocalbridge/vexrelay-task-archive.js')).href)).default;
const suffix = \`\${new Date().toISOString().replace(/[-:.TZ]/g, '')}-\${crypto.randomBytes(6).toString('hex')}\`;
const now = new Date();
const replacements = {
  '{{SUFFIX}}': suffix,
  '{{NOW}}': now.toISOString(),
  '{{EXPIRY}}': new Date(now.getTime() + cfg.route.ttlMs).toISOString(),
  '{{NATIVE_PAYLOAD_CLASS}}': native.VEXRELAY_ARCHIVE_PAYLOAD_CLASS,
  '{{NATIVE_PAYLOAD_RUNTIME}}': native.VEXRELAY_ARCHIVE_RUNTIME,
};
const materialize = (value) => {
  if (Array.isArray(value)) return value.map(materialize);
  if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, materialize(v)]));
  if (typeof value !== 'string') return value;
  let out = value;
  for (const [from, to] of Object.entries(replacements)) out = out.split(from).join(to);
  return out;
};
const sha256 = (file) => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const zipFiles = (root) => {
  const out = [];
  if (!fs.existsSync(root)) return out;
  const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) walk(full);
      else if (entry.isFile() && entry.name.endsWith('.zip')) {
        out.push({ path: full, relativePath: path.relative(root, full), sha256: sha256(full), bytes: fs.statSync(full).size });
      }
    }
  };
  walk(root);
  return out;
};
const writeMeta = (value) => fs.writeFileSync(metadataPath, JSON.stringify(value, null, 2) + '\n');

const result = await formation.formVexRelayTaskArchiveCarrier({
  recipe: cfg.route.recipe,
  seed: materialize(cfg.seedTemplate),
  stage: materialize(cfg.stageTemplate),
  outerTaskTemplate: materialize(cfg.outerTaskTemplate),
  authorityEnvelope: materialize(cfg.outerAuthorityTemplate),
  outputDirectory,
  outputStem: cfg.route.outputStem,
  ttlMs: cfg.route.ttlMs,
});
const capsuleRoot = result.outer.finalZip.replace(/\.zip$/u, '') + '-run-' + result.outer.digest.slice(0, 16);
const baseMeta = {
  schemaVersion: 'vexlife-prod.companion-engagement-relay-run/v1',
  sourceHead: cfg.target.head,
  sourceTree: cfg.target.tree,
  relayContractRef: cfg.sdk.contractRef,
  sdkHead: cfg.sdk.head,
  outerZip: result.outer.finalZip,
  outerSha256: result.outer.digest,
  capsuleRoot,
  innerTaskRef: result.inner.manifest.taskRef,
  innerAttemptRef: result.inner.manifest.attemptRef,
};
writeMeta({ ...baseMeta, runStatus: null, evidenceZips: [] });
console.log(\`VEXRELAY_OUTER_ZIP=\${result.outer.finalZip}\`);
console.log(\`VEXRELAY_OUTER_SHA256=\${result.outer.digest}\`);

const launcher = path.join(sdkRoot, 'lib/vexlocalbridge/templates/RUN-VEX-TASK.command');
const run = spawnSync('/bin/bash', [launcher, result.outer.finalZip], { stdio: 'inherit', env: process.env });
if (run.error) throw run.error;
const status = run.status ?? 1;
writeMeta({ ...baseMeta, runStatus: status, evidenceZips: zipFiles(capsuleRoot) });
process.exit(status);
