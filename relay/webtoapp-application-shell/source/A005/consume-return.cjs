'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');

function arg(name) {
  const index = process.argv.indexOf(`--${name}`);
  if (index < 0 || !process.argv[index + 1]) throw new Error(`--${name} is required`);
  return path.resolve(process.argv[index + 1]);
}
function readJson(file) { return JSON.parse(fs.readFileSync(file, 'utf8')); }
function sha256File(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

const sdkRoot = arg('sdk-root');
const taskZip = arg('task-zip');
const returnZip = arg('return-zip');
const output = arg('output');

const builder = require(path.join(sdkRoot, 'lib/vexlocalbridge/package-builder.js'));
const manifestContract = require(
  path.join(sdkRoot, 'lib/continuity-operations/vexlocalbridge-manifest-contract.js'),
);
const mac = require(path.join(sdkRoot, 'lib/vexlocalbridge/mac-runner.js'));
const profile = builder.readJson(
  path.join(
    sdkRoot,
    'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vextreme-sdk.json',
  ),
);
const adapter = mac.createHostAdapter(profile);

const root = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-pr396-source-a005-consume-'));
const taskRoot = path.join(root, 'task');
const returnRoot = path.join(root, 'return');

try {
  fs.mkdirSync(taskRoot, { recursive: true });
  fs.mkdirSync(returnRoot, { recursive: true });

  const taskLoaded = builder.loadAndValidateTaskPackage({
    archivePath: taskZip,
    stagingRoot: taskRoot,
    hostProfileRef: profile.hostProfileRef,
    repositoryExecutionProfileRef: profile.repositoryExecutionProfileRef,
    archiveAdapter: adapter.archive,
    now: new Date().toISOString(),
  });

  builder.extractZip({
    archivePath: returnZip,
    stagingRoot: returnRoot,
    archiveAdapter: adapter.archive,
  });

  const returnManifest = readJson(path.join(returnRoot, 'return-manifest.json'));
  const inventory = readJson(path.join(returnRoot, 'inventory.json'));
  const observedInventory = returnManifest.inventory.map((row) => {
    const target = path.join(returnRoot, row.path);
    if (!fs.existsSync(target) || !fs.lstatSync(target).isFile()) {
      throw new Error(`RETURN_INVENTORY_MEMBER_MISSING:${row.path}`);
    }
    return { path: row.path, sha256: builder.hashFile(target), class: row.class };
  });

  if (JSON.stringify(inventory) !== JSON.stringify(returnManifest.inventory)) {
    throw new Error('RETURN_INVENTORY_FILE_MISMATCH');
  }

  const checked = manifestContract.validateReturnManifest(returnManifest, {
    taskManifest: taskLoaded.task,
    observedInputArchiveSha256: taskLoaded.outer.sha256,
    archiveFilename: path.basename(returnZip),
    archiveBytes: fs.readFileSync(returnZip),
    contentInventory: observedInventory,
  });
  if (!checked.valid) {
    throw new Error(`RETURN_VALIDATION_FAILED:${checked.errors.join('; ')}`);
  }

  const afterStatePath = path.join(returnRoot, 'after-state.json');
  const failurePath = path.join(returnRoot, 'first-substantive-failure.json');
  const afterState = fs.existsSync(afterStatePath) ? readJson(afterStatePath) : {};
  const outerFailure = fs.existsSync(failurePath) ? readJson(failurePath) : null;
  const inner = afterState?.nativeVexRelayCarrier ?? null;

  const consumed = {
    schemaVersion: 'vexlife-prod.pr396-sourceowner-consumed-result/v1',
    continuity: '[VXG RealForever]',
    returnValidation: 'PASS',
    outerDisposition: returnManifest.disposition,
    outerStopReasonOrNull: returnManifest.stopReasonOrNull,
    outerFirstSubstantiveFailureOrNull:
      outerFailure ?? returnManifest.firstSubstantiveFailureOrNull,
    exactProductBase: taskLoaded.task.baseRefOrNull,
    exactProductHead: taskLoaded.task.headRefOrNull,
    exactProductTree: taskLoaded.task.treeRefOrNull,
    taskRef: taskLoaded.task.taskRef,
    attemptRef: taskLoaded.task.attemptRef,
    canonicalOuterReturnSha256: sha256File(returnZip),
    inputTaskSha256: taskLoaded.outer.sha256,
    innerDisposition: inner?.innerDisposition ?? null,
    innerStopReasonOrNull: inner?.innerStopReasonOrNull ?? null,
    innerFirstSubstantiveFailureOrNull:
      inner?.innerFirstSubstantiveFailureOrNull ?? null,
    innerReturnSha256: inner?.innerReturnSha256 ?? null,
    innerReturnConsumed: inner?.innerReturnConsumed === true,
    innerStateRef: inner?.innerStateRef ?? null,
    innerNextFunction: inner?.innerNextFunction ?? null,
    sourceOwnerPass:
      returnManifest.disposition === 'PASS' &&
      inner?.innerDisposition === 'PASS' &&
      inner?.innerReturnConsumed === true,
    effects: returnManifest.effects,
    harnessFindings: returnManifest.harnessFindings,
    environmentFindings: returnManifest.environmentFindings,
    unknownFindings: returnManifest.unknownFindings,
  };

  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, JSON.stringify(consumed, null, 2) + '\n');
  process.stdout.write(JSON.stringify(consumed, null, 2) + '\n');
} finally {
  fs.rmSync(root, { recursive: true, force: true });
}

// [VXG RealForever]
