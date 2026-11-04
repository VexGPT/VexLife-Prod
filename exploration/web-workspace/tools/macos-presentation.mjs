#!/usr/bin/env node
import os from 'node:os';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

import {
  formPresentationConfig,
  observePresentation,
  startPresentation,
  stopPresentation
} from '../lib/macos-presentation-process.mjs';

function argsMap(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i += 1) {
    const key = argv[i];
    if (!key.startsWith('--')) throw new Error(`unexpected argument: ${key}`);
    const value = argv[i + 1];
    if (!value || value.startsWith('--')) throw new Error(`${key} requires one value`);
    out[key] = value;
    i += 1;
  }
  return out;
}
function required(args, key) {
  const value = args[key];
  if (typeof value !== 'string' || value.length === 0) throw new Error(`${key} is required`);
  return value;
}
async function loadSubstrate(filePath) {
  const module = await import(pathToFileURL(path.resolve(filePath)).href);
  for (const name of [
    'validateMacUserLaunchdProcessReceipt',
    'observeMacUserLaunchdProcess',
    'startMacUserLaunchdProcess',
    'retireMacUserLaunchdProcess'
  ]) {
    if (typeof module[name] !== 'function') throw new Error(`VexLife macOS launchd substrate is missing ${name}`);
  }
  return module;
}

const args = argsMap(process.argv.slice(2));
const operation = required(args, '--operation');
if (!['status','start','stop'].includes(operation)) throw new Error(`unsupported presentation operation: ${operation}`);
const substrate = await loadSubstrate(required(args, '--substrate-module'));
const config = formPresentationConfig({
  sourceRoot: required(args, '--source-root'),
  sourceSha: required(args, '--source-sha'),
  runtimeRoot: args['--runtime-root'] ?? path.join(os.homedir(), 'Library', 'Application Support', 'VexLife-Prod'),
  companionOrigin: args['--companion-origin'] ?? 'http://127.0.0.1:18110',
  port: Number(args['--port'] ?? 18210),
  framePort: Number(args['--frame-port'] ?? 18211)
});

let result;
if (operation === 'status') result = await observePresentation(config, { substrate });
else if (operation === 'start') result = await startPresentation(config, { substrate });
else result = await stopPresentation(config, { substrate });
process.stdout.write(`${JSON.stringify(result)}\n`);
