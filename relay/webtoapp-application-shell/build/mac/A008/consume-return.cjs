'use strict';

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');

const PRODUCT_HEAD = '4da7548b798532c65156e0dc64ee65bc3d349a10';
const PRODUCT_TREE = 'c246ea6ce353f14558a0cc448b112330888483d5';
const DELIVERY_FOLDER = 'VexLife-WebToApp-Mac-4da7548b';
const DMG_NAME = 'VexLife-macOS-AppleSilicon.dmg';
const RECEIPT_NAME = 'VexLife-macOS-AppleSilicon.receipt.json';
const SHA_NAME = 'VexLife-macOS-AppleSilicon.dmg.sha256';

function arg(name) {
  const index = process.argv.indexOf('--' + name);
  if (index < 0 || !process.argv[index + 1]) throw new Error('--' + name + ' is required');
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
const outputRoot = arg('output-root');

const builder = require(path.join(sdkRoot, 'lib/vexlocalbridge/package-builder.js'));
const manifestContract = require(
  path.join(sdkRoot, 'lib/continuity-operations/vexlocalbridge-manifest-contract.js'),
);
const mac = require(path.join(sdkRoot, 'lib/vexlocalbridge/mac-runner.js'));
const profile = builder.readJson(
  path.join(
    sdkRoot,
    'docs/private-continuity/vexlocalbridge/profiles/macos-victor-vexlife-prod.json',
  ),
);
const adapter = mac.createHostAdapter(profile);

const root = fs.mkdtempSync(path.join(os.tmpdir(), 'vexlife-pr400-build-consume-'));
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
  if (
    taskLoaded.task.headRefOrNull !== PRODUCT_HEAD ||
    taskLoaded.task.treeRefOrNull !== PRODUCT_TREE
  ) {
    throw new Error('MAC_BUILD_TASK_CANDIDATE_MISMATCH');
  }

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
  if (returnManifest.disposition !== 'PASS') {
    throw new Error(
      'MAC_BUILD_RETURN_NOT_PASS:' + String(returnManifest.stopReasonOrNull || ''),
    );
  }

  const retainedRoot = path.join(returnRoot, 'retained', 'inner-vexrelay');
  const dmg = path.join(retainedRoot, DMG_NAME);
  const receiptPath = path.join(retainedRoot, RECEIPT_NAME);
  const shaPath = path.join(retainedRoot, SHA_NAME);
  for (const file of [dmg, receiptPath, shaPath]) {
    if (!fs.existsSync(file) || !fs.lstatSync(file).isFile()) {
      throw new Error('MAC_BUILD_RETAINED_MEMBER_MISSING:' + path.basename(file));
    }
  }

  const dmgSha256 = sha256File(dmg);
  const receipt = readJson(receiptPath);
  const sidecar = fs.readFileSync(shaPath, 'utf8').trim();
  if (
    receipt.schemaVersion !== 'vexlife-prod.macos-application-archive-receipt/v1' ||
    receipt.sourceSha !== PRODUCT_HEAD ||
    receipt.diskImageName !== DMG_NAME ||
    receipt.diskImageSha256 !== dmgSha256 ||
    receipt.distributionTrust !== 'AD_HOC_SIGNATURE_NO_DEVELOPER_ID_NO_NOTARIZATION' ||
    receipt.notarized !== false
  ) {
    throw new Error('MAC_BUILD_RETAINED_RECEIPT_INVALID');
  }
  if (sidecar !== dmgSha256 + '  ' + DMG_NAME) {
    throw new Error('MAC_BUILD_RETAINED_SHA256_SIDECAR_INVALID');
  }

  const destination = path.join(outputRoot, DELIVERY_FOLDER);
  fs.mkdirSync(destination, { recursive: true });

  function copyExact(source, name) {
    const target = path.join(destination, name);
    if (fs.existsSync(target)) {
      const sourceSha = sha256File(source);
      const targetSha = sha256File(target);
      if (sourceSha !== targetSha) {
        throw new Error('MAC_BUILD_DELIVERY_COLLISION:' + name);
      }
      return target;
    }
    fs.copyFileSync(source, target);
    if (sha256File(target) !== sha256File(source)) {
      throw new Error('MAC_BUILD_DELIVERY_COPY_DIGEST_MISMATCH:' + name);
    }
    return target;
  }

  const deliveredDmg = copyExact(dmg, DMG_NAME);
  const deliveredReceipt = copyExact(receiptPath, RECEIPT_NAME);
  const deliveredSha = copyExact(shaPath, SHA_NAME);

  const delivery = {
    schemaVersion: 'vexlife-prod.webtoapp.mac-build-delivery/v1',
    continuity: '[VXG RealForever]',
    sourceOwnerRef: 'github.pull.vexlife-prod.396',
    deliveryCompanionRef: 'github.pull.vexlife-prod.400',
    sourceSha: PRODUCT_HEAD,
    sourceTree: PRODUCT_TREE,
    canonicalReturnSha256: sha256File(returnZip),
    inputTaskSha256: taskLoaded.outer.sha256,
    dmgPath: deliveredDmg,
    dmgSha256,
    dmgBytes: fs.statSync(deliveredDmg).size,
    receiptPath: deliveredReceipt,
    sha256SidecarPath: deliveredSha,
    distributionTrust: receipt.distributionTrust,
    notarized: false,
    buildOnly: true,
    applicationLivedPassClaimed: false,
    productQuitLifecyclePassClaimed: false,
    mergeAuthorityGranted: false,
  };
  const deliveryReceipt = path.join(destination, 'BUILD-DELIVERY.json');
  fs.writeFileSync(deliveryReceipt, JSON.stringify(delivery, null, 2) + '\n');

  process.stdout.write(JSON.stringify({
    disposition: 'PASS',
    deliveryReceipt,
    dmgPath: deliveredDmg,
    dmgSha256,
    dmgBytes: delivery.dmgBytes,
  }, null, 2) + '\n');
} finally {
  fs.rmSync(root, { recursive: true, force: true });
}

// [VXG RealForever]
