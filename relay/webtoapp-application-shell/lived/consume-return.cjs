'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');

function arg(name) {
  const index = process.argv.indexOf('--' + name);
  if (index < 0 || !process.argv[index + 1]) throw new Error('--' + name + ' is required');
  return process.argv[index + 1];
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function sha256File(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

const sdkRoot = path.resolve(arg('sdk-root'));
const hostProfileRef = arg('host');
const taskZip = path.resolve(arg('task-zip'));
const returnZip = path.resolve(arg('return-zip'));
const output = path.resolve(arg('output'));

const host =
  hostProfileRef === 'macos-victor'
    ? {
        platform: 'macos',
        profile:
          'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json',
        runner: 'lib/vexlocalbridge/mac-runner.js',
      }
    : hostProfileRef === 'windows-victor'
      ? {
          platform: 'windows',
          profile:
            'docs/private-continuity/vexlocalbridge/profiles/windows-victor-vexlife-prod.json',
          runner: 'lib/vexlocalbridge/windows-runner.js',
        }
      : null;
if (!host) throw new Error('HOST_PROFILE_UNSUPPORTED:' + hostProfileRef);

const builder = require(path.join(sdkRoot, 'lib/vexlocalbridge/package-builder.js'));
const manifestContract = require(
  path.join(sdkRoot, 'lib/continuity-operations/vexlocalbridge-manifest-contract.js'),
);
const runner = require(path.join(sdkRoot, host.runner));
const profile = builder.readJson(path.join(sdkRoot, host.profile));
const adapter = runner.createHostAdapter(profile);

const root = fs.mkdtempSync(
  path.join(os.tmpdir(), 'vexlife-pr398-' + host.platform + '-consume-'),
);
const taskRoot = path.join(root, 'task');
const returnRoot = path.join(root, 'return');

const requiredSteps = [
  'exact-application-formed',
  'first-product-current',
  'first-human-turn-completed',
  'presentation-close-preserved-product',
  'explicit-quit-released-product-runtime',
  'same-home-preserved-after-quit',
  'reopen-fresh-product-runtime',
  'second-human-turn-completed',
  'final-quit-cleanup',
];

const lifecycleSteps = [
  'first-product-current',
  'presentation-close-preserved-product',
  'explicit-quit-released-product-runtime',
  'same-home-preserved-after-quit',
  'reopen-fresh-product-runtime',
  'final-quit-cleanup',
];

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
  const inventoryFile = readJson(path.join(returnRoot, 'inventory.json'));
  if (JSON.stringify(inventoryFile) !== JSON.stringify(returnManifest.inventory)) {
    throw new Error('RETURN_INVENTORY_FILE_MISMATCH');
  }

  const observedInventory = returnManifest.inventory.map((row) => {
    const target = path.join(returnRoot, row.path);
    if (!fs.existsSync(target) || !fs.lstatSync(target).isFile()) {
      throw new Error('RETURN_INVENTORY_MEMBER_MISSING:' + row.path);
    }
    return {
      path: row.path,
      sha256: builder.hashFile(target),
      class: row.class,
    };
  });

  const checked = manifestContract.validateReturnManifest(returnManifest, {
    taskManifest: taskLoaded.task,
    observedInputArchiveSha256: taskLoaded.outer.sha256,
    archiveFilename: path.basename(returnZip),
    archiveBytes: fs.readFileSync(returnZip),
    contentInventory: observedInventory,
  });
  if (!checked.valid) {
    throw new Error('RETURN_VALIDATION_FAILED:' + checked.errors.join('; '));
  }

  const completedPath = path.join(returnRoot, 'completed-step-receipts.json');
  const afterStatePath = path.join(returnRoot, 'after-state.json');
  const beforeStatePath = path.join(returnRoot, 'before-state.json');
  const failurePath = path.join(returnRoot, 'first-substantive-failure.json');

  const completed = fs.existsSync(completedPath) ? readJson(completedPath) : [];
  const afterState = fs.existsSync(afterStatePath) ? readJson(afterStatePath) : null;
  const beforeState = fs.existsSync(beforeStatePath) ? readJson(beforeStatePath) : null;
  const firstFailure = fs.existsSync(failurePath) ? readJson(failurePath) : null;

  const stepState = Object.fromEntries(
    (Array.isArray(completed) ? completed : []).map((row) => [
      row.step,
      row.state ?? row.disposition ?? null,
    ]),
  );
  const livedPredicates = Object.fromEntries(
    requiredSteps.map((step) => [step, stepState[step] === 'PASS']),
  );
  const allLivedPredicatesPass =
    requiredSteps.every((step) => livedPredicates[step] === true);
  const allLifecyclePredicatesPass =
    lifecycleSteps.every((step) => livedPredicates[step] === true);

  const consumed = {
    schemaVersion: 'vexlife-prod.pr398-application-lived-consumed-result/v1',
    continuity: '[VXG RealForever]',
    platform: host.platform,
    returnValidation: 'PASS',
    relayDisposition: returnManifest.disposition,
    stopReasonOrNull: returnManifest.stopReasonOrNull,
    firstSubstantiveFailureOrNull:
      firstFailure ?? returnManifest.firstSubstantiveFailureOrNull,
    exactProductBase: taskLoaded.task.baseRefOrNull,
    exactProductHead: taskLoaded.task.headRefOrNull,
    exactProductTree: taskLoaded.task.treeRefOrNull,
    taskRef: taskLoaded.task.taskRef,
    attemptRef: taskLoaded.task.attemptRef,
    canonicalReturnSha256: sha256File(returnZip),
    inputTaskSha256: taskLoaded.outer.sha256,
    livedPredicates,
    applicationLivedPass:
      returnManifest.disposition === 'PASS' && allLivedPredicatesPass,
    productQuitLifecyclePass:
      returnManifest.disposition === 'PASS' && allLifecyclePredicatesPass,
    lifecycleEvidenceOwnerRef: 'github.pull.vexlife-prod.391',
    applicationEvidenceOwnerRef: 'github.pull.vexlife-prod.396',
    effects: returnManifest.effects,
    harnessFindings: returnManifest.harnessFindings,
    environmentFindings: returnManifest.environmentFindings,
    unknownFindings: returnManifest.unknownFindings,
    beforeState,
    afterState,
    completedStepReceipts: completed,
  };

  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, JSON.stringify(consumed, null, 2) + '\n');
  process.stdout.write(JSON.stringify(consumed, null, 2) + '\n');
} finally {
  fs.rmSync(root, { recursive: true, force: true });
}

// [VXG RealForever]
