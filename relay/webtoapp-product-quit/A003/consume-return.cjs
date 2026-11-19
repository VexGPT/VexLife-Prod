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

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

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

const profilePath = path.join(
  sdkRoot,
  'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json',
);
const profile = builder.readJson(profilePath);
const adapter = mac.createHostAdapter(profile);

const root = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-pr391-a003-consume-'));
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
  const inventoryFile = readJson(path.join(returnRoot, 'inventory.json'));

  if (JSON.stringify(inventoryFile) !== JSON.stringify(returnManifest.inventory)) {
    throw new Error('RETURN_INVENTORY_FILE_MISMATCH');
  }

  const observedInventory = returnManifest.inventory.map((row) => {
    const target = path.join(returnRoot, row.path);
    if (!fs.existsSync(target) || !fs.lstatSync(target).isFile()) {
      throw new Error(`RETURN_INVENTORY_MEMBER_MISSING:${row.path}`);
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
    throw new Error(`RETURN_VALIDATION_FAILED:${checked.errors.join('; ')}`);
  }

  for (let i = 0; i < observedInventory.length; i += 1) {
    if (observedInventory[i].sha256 !== returnManifest.inventory[i].sha256) {
      throw new Error(`RETURN_MEMBER_DIGEST_MISMATCH:${observedInventory[i].path}`);
    }
  }

  const afterStatePath = path.join(returnRoot, 'after-state.json');
  const beforeStatePath = path.join(returnRoot, 'before-state.json');
  const stepsPath = path.join(returnRoot, 'completed-step-receipts.json');
  const failurePath = path.join(returnRoot, 'first-substantive-failure.json');

  const afterState = fs.existsSync(afterStatePath) ? readJson(afterStatePath) : null;
  const beforeState = fs.existsSync(beforeStatePath) ? readJson(beforeStatePath) : null;
  const completedSteps = fs.existsSync(stepsPath) ? readJson(stepsPath) : [];
  const firstFailure = fs.existsSync(failurePath) ? readJson(failurePath) : null;

  const stepState = Object.fromEntries(
    (Array.isArray(completedSteps) ? completedSteps : []).map((step) => [
      step.step,
      step.state ?? step.disposition ?? null,
    ]),
  );

  const consumed = {
    schemaVersion: 'vexlife-prod.pr391-relay-consumed-result/v1',
    continuity: '[VXG RealForever]',
    returnValidation: 'PASS',
    relayDisposition: returnManifest.disposition,
    stopReasonOrNull: returnManifest.stopReasonOrNull,
    firstSubstantiveFailureOrNull: firstFailure ?? returnManifest.firstSubstantiveFailureOrNull,
    exactProductHead: taskLoaded.task.headRefOrNull,
    exactProductTree: taskLoaded.task.treeRefOrNull,
    exactProductBase: taskLoaded.task.baseRefOrNull,
    taskRef: taskLoaded.task.taskRef,
    attemptRef: taskLoaded.task.attemptRef,
    canonicalReturnSha256: sha256File(returnZip),
    inputTaskSha256: taskLoaded.outer.sha256,
    nextSafeRouteRef: returnManifest.nextSafeRouteRef,
    effects: returnManifest.effects,
    harnessFindings: returnManifest.harnessFindings,
    environmentFindings: returnManifest.environmentFindings,
    unknownFindings: returnManifest.unknownFindings,
    livedPredicatesProven: {
      exactCandidateServiceRuntimeReady:
        stepState['exact-candidate-service-runtime-ready'] === 'PASS',
      exactProductServiceBootout:
        stepState['exact-product-service-bootout'] === 'PASS',
      runtimeReleaseAfterServiceTermination:
        stepState['runtime-release-after-service-termination'] === 'PASS',
      sameHomeRestart:
        stepState['same-home-restart'] === 'PASS',
    },
    recoveryDisposition: {
      attempted: afterState?.recoveryAttempted ?? false,
      succeeded: afterState?.recoverySucceeded ?? false,
    },
    beforeState,
    afterState,
    completedStepReceipts: completedSteps,
  };

  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, JSON.stringify(consumed, null, 2) + '\n');
  process.stdout.write(JSON.stringify(consumed, null, 2) + '\n');
} finally {
  fs.rmSync(root, { recursive: true, force: true });
}

// [VXG RealForever]
