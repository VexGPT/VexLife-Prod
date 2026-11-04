// Observed host resource truth for the Settings projections (context window, memory).
//
// Every value here is read, never assumed: memory from the OS, the runtime's resident memory from
// the process listening on the runtime port, and the context window from the model's own
// config.json. Anything that cannot be read is null, and the UI says "unknown".
//
// Reads only. The model config lives inside the Vex Home's model directory; this module reads that
// one file and writes nothing anywhere.
//
// [VXG RealForever]
import { execFile } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

function run(command, args, timeout = 3000) {
  return new Promise((resolve) => {
    execFile(command, args, { timeout, encoding: 'utf8' }, (error, stdout) => resolve(error ? null : stdout));
  });
}

async function darwinAvailableBytes() {
  const output = await run('/usr/bin/vm_stat', []);
  if (!output) return null;
  const pageSize = Number(/page size of (\d+) bytes/u.exec(output)?.[1]);
  if (!Number.isFinite(pageSize)) return null;
  const pages = (label) => Number(new RegExp(`${label}:\\s+(\\d+)`, 'u').exec(output)?.[1] ?? 0);
  // Free + inactive + speculative + purgeable pages are reclaimable without pressure.
  return (pages('Pages free') + pages('Pages inactive') + pages('Pages speculative') + pages('Pages purgeable')) * pageSize;
}

function linuxAvailableBytes() {
  try {
    const kb = Number(/MemAvailable:\s+(\d+)\s+kB/u.exec(fs.readFileSync('/proc/meminfo', 'utf8'))?.[1]);
    return Number.isFinite(kb) ? kb * 1024 : null;
  } catch {
    return null;
  }
}

export async function observeMemory({ platform = process.platform } = {}) {
  const totalBytes = os.totalmem();
  let availableBytes = null;
  let source = null;
  if (platform === 'darwin') {
    availableBytes = await darwinAvailableBytes();
    source = availableBytes === null ? null : 'vm_stat';
  } else if (platform === 'linux') {
    availableBytes = linuxAvailableBytes();
    source = availableBytes === null ? null : '/proc/meminfo';
  }
  if (availableBytes === null) {
    availableBytes = os.freemem();
    source = 'os.freemem';
  }
  return { totalBytes, availableBytes: Math.min(availableBytes, totalBytes), source };
}

export async function observeRuntimeProcess(endpoint) {
  let url;
  try { url = new URL(endpoint); } catch { return null; }
  if (!['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname) || !url.port) return null;
  const pids = await run('lsof', ['-nP', `-iTCP:${url.port}`, '-sTCP:LISTEN', '-t']);
  const pid = Number(pids?.trim().split('\n')[0]);
  if (!Number.isInteger(pid) || pid <= 0) return null;
  const rss = Number((await run('ps', ['-o', 'rss=', '-p', String(pid)]))?.trim());
  return { pid, residentBytes: Number.isFinite(rss) && rss > 0 ? rss * 1024 : null };
}

export function observeContextWindow(modelRef) {
  if (typeof modelRef !== 'string' || !path.isAbsolute(modelRef)) return null;
  const configPath = path.join(modelRef, 'config.json');
  try {
    const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
    const tokens = [
      config.max_position_embeddings,
      config.text_config?.max_position_embeddings,
      config.max_sequence_length,
      config.seq_length
    ].find((value) => Number.isInteger(value) && value > 0);
    return tokens ? { tokens, source: 'model config.json' } : null;
  } catch {
    return null;
  }
}

export async function observeHostResources({ endpoint, modelRef }) {
  const [memory, runtime] = await Promise.all([observeMemory(), observeRuntimeProcess(endpoint)]);
  return {
    schemaVersion: 'vexlife-prod.exploration.host-resources/v1',
    observedAt: new Date().toISOString(),
    memory,
    runtime,
    contextWindow: observeContextWindow(modelRef)
  };
}
