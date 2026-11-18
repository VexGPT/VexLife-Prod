#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const { spawnSync } = require('node:child_process');

const RESULT_SCHEMA = 'vextreme.vexlocalbridge-mac-payload-result/v1';

function sha256(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function defaultEffects() {
  return {
    sourceMutationPerformed: false,
    branchMutationPerformed: false,
    claimMutationPerformed: false,
    prMetadataMutationPerformed: false,
    reviewPerformed: false,
    approvalPerformed: false,
    readyTransitionPerformed: false,
    mergePerformed: false,
    mainMutationPerformed: false,
    checkpointMutationPerformed: false,
    forcePushPerformed: false,
    historyRewritePerformed: false,
    hostInstallationPerformed: false,
    modelRuntimePerformed: false,
    providerOrNetworkEffectPerformed: false,
    publicationPerformed: false,
    credentialSerializationPerformed: false,
    filesystemOutsideQualifiedRootsPerformed: false,
  };
}

function failureRecord(error, stage) {
  const summary = String(error?.message || error || 'unknown release failure').slice(0, 1600);
  return {
    failureFamily: 'VEXLIFE_PROD_PREVIEW_PUBLICATION',
    failureFingerprint: sha256(Buffer.from(stage + '\n' + summary)),
    stage,
    summary,
    evidenceRef: 'payload-result.json',
  };
}

function writeResult(value) {
  const root = process.env.VEXLOCALBRIDGE_RESULT_DIR;
  if (!root) throw new Error('VEXLOCALBRIDGE_RESULT_DIR is unavailable');
  fs.mkdirSync(root, { recursive: true });
  fs.writeFileSync(
    path.join(root, 'payload-result.json'),
    JSON.stringify({ schemaVersion: RESULT_SCHEMA, acceptedExitCodes: [0], ...value }, null, 2) + '\n',
  );
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd,
    env: options.env || process.env,
    encoding: 'utf8',
    timeout: options.timeoutMs || 120000,
    maxBuffer: 64 * 1024 * 1024,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  return {
    status: Number.isInteger(result.status) ? result.status : 127,
    stdout: String(result.stdout || ''),
    stderr: String(result.stderr || (result.error ? result.error.message : '')),
    error: result.error || null,
  };
}

function requireRun(command, args, options = {}) {
  const result = run(command, args, options);
  if (result.status !== 0) {
    throw new Error(
      command + ' ' + args.join(' ') + ' failed exit=' + result.status +
      '\nstdout=' + result.stdout.slice(-10000) +
      '\nstderr=' + result.stderr.slice(-10000),
    );
  }
  return result;
}

function ghJson(gh, apiPath, options = {}) {
  const result = run(gh, ['api', apiPath], {
    timeoutMs: options.timeoutMs || 60000,
    env: options.env || process.env,
  });
  if (result.status !== 0) return { present: false, status: result.status, stderr: result.stderr };
  return { present: true, value: JSON.parse(result.stdout), status: 0 };
}

function ghPagesSnapshot(gh, repository) {
  const response = ghJson(
    gh,
    'repos/' + repository + '/contents/data/releases.json?ref=gh-pages',
  );
  if (!response.present) return null;
  const encoded = String(response.value?.content || '').replace(/\s/g, '');
  if (!encoded) return null;
  return JSON.parse(Buffer.from(encoded, 'base64').toString('utf8'));
}

function releaseSummary(release) {
  if (!release) return null;
  return {
    tagName: release.tag_name ?? null,
    targetCommitish: release.target_commitish ?? null,
    prerelease: release.prerelease ?? null,
    publishedAt: release.published_at ?? null,
    url: release.html_url ?? null,
    assetNames: Array.isArray(release.assets) ? release.assets.map((asset) => asset.name).sort() : [],
    sourceLinePresent: typeof release.body === 'string' && /^source-sha:\s*[0-9a-f]{40}\s*$/m.test(release.body),
  };
}

async function main() {
  const bindingPath = process.argv[2];
  if (!bindingPath) throw new Error('candidate binding path is required');
  const binding = JSON.parse(fs.readFileSync(path.resolve(bindingPath), 'utf8'));

  const effects = defaultEffects();
  const resultBase = {
    effects,
    candidateFindings: [],
    harnessFindings: [],
    environmentFindings: [],
    unknownFindings: [],
    beforeState: {},
    afterState: {},
    completedStepReceipts: [],
  };

  let stage = 'HOST_PREFLIGHT';
  let work = null;
  let beforeRelease = null;
  let afterRelease = null;
  let beforePagesSha = null;
  let afterPagesSha = null;

  try {
    if (process.platform !== 'darwin' || process.arch !== 'arm64') {
      resultBase.environmentFindings.push('HOST_NOT_DARWIN_ARM64');
      writeResult({
        disposition: 'SAFE_FAILURE',
        stopReasonOrNull: 'HOST_NOT_DARWIN_ARM64',
        firstSubstantiveFailureOrNull:
          failureRecord(new Error('Preview publication requires Apple-Silicon macOS'), stage),
        ...resultBase,
      });
      return;
    }

    const gh = ['/opt/homebrew/bin/gh', '/usr/local/bin/gh'].find((candidate) => fs.existsSync(candidate));
    const git = '/usr/bin/git';
    const bash = '/bin/bash';
    if (!gh) throw new Error('GitHub CLI is unavailable');
    if (!fs.existsSync(git)) throw new Error('Git is unavailable');
    if (!fs.existsSync(bash)) throw new Error('Bash is unavailable');

    requireRun(gh, ['auth', 'status', '--hostname', 'github.com'], { timeoutMs: 30000 });
    const javaHome = requireRun('/usr/libexec/java_home', ['-v', '21'], { timeoutMs: 10000 }).stdout.trim();
    const releaseEnv = {
      ...process.env,
      JAVA_HOME: javaHome,
      PATH: javaHome + '/bin:' + process.env.PATH,
      VEXLIFE_REPOSITORY: binding.repository,
    };

    effects.providerOrNetworkEffectPerformed = true;
    work = fs.mkdtempSync(path.join(process.env.TMPDIR || os.tmpdir(), 'vexlife-preview-publication-'));
    const checkout = path.join(work, 'VexLife-Prod');

    stage = 'EXACT_CURRENT_MAIN';
    requireRun(gh, ['repo', 'clone', binding.repository, checkout, '--', '--no-checkout'], {
      timeoutMs: 180000,
      env: releaseEnv,
    });
    requireRun(git, ['-C', checkout, 'checkout', '--detach', binding.head], {
      timeoutMs: 60000,
      env: releaseEnv,
    });
    requireRun(git, ['-C', checkout, 'fetch', '--quiet', 'origin', 'main'], {
      timeoutMs: 60000,
      env: releaseEnv,
    });

    const head = requireRun(git, ['-C', checkout, 'rev-parse', 'HEAD'], { env: releaseEnv }).stdout.trim();
    const tree = requireRun(git, ['-C', checkout, 'rev-parse', 'HEAD^{tree}'], { env: releaseEnv }).stdout.trim();
    const remoteMain = requireRun(git, ['-C', checkout, 'rev-parse', 'refs/remotes/origin/main'], { env: releaseEnv }).stdout.trim();
    const status = requireRun(git, ['-C', checkout, 'status', '--porcelain=v1', '-uall'], { env: releaseEnv }).stdout.trim();

    if (head !== binding.head) throw new Error('candidate head mismatch: ' + head);
    if (tree !== binding.tree) throw new Error('candidate tree mismatch: ' + tree);
    if (remoteMain !== binding.head) throw new Error('origin/main moved before publication: ' + remoteMain);
    if (status !== '') throw new Error('exact current-main checkout is dirty before publication');

    resultBase.completedStepReceipts.push({
      step: 'exact-current-main',
      state: 'PASS',
      head,
      tree,
      remoteMain,
    });

    const tag = 'private-service-image-' + binding.head;
    const beforeReleaseRead = ghJson(gh, 'repos/' + binding.repository + '/releases/tags/' + tag, { env: releaseEnv });
    beforeRelease = beforeReleaseRead.present ? beforeReleaseRead.value : null;
    const beforePages = ghJson(gh, 'repos/' + binding.repository + '/branches/gh-pages', { env: releaseEnv });
    beforePagesSha = beforePages.present ? beforePages.value?.commit?.sha ?? null : null;

    resultBase.beforeState = {
      sourceHead: binding.head,
      sourceTree: binding.tree,
      previewTag: tag,
      releaseAlreadyPresent: Boolean(beforeRelease),
      release: releaseSummary(beforeRelease),
      ghPagesHead: beforePagesSha,
    };

    stage = 'SOURCE_OWNED_PREVIEW_PUBLICATION';
    let scriptResult = { status: 0, stdout: '', stderr: '', skipped: false };
    if (!beforeRelease) {
      scriptResult = run(bash, ['tools/release/publish-mac-release.sh', 'preview'], {
        cwd: checkout,
        env: releaseEnv,
        timeoutMs: 690000,
      });
    } else {
      scriptResult.skipped = true;
    }

    stage = 'REMOTE_PUBLICATION_READBACK';
    const afterReleaseRead = ghJson(gh, 'repos/' + binding.repository + '/releases/tags/' + tag, { env: releaseEnv });
    afterRelease = afterReleaseRead.present ? afterReleaseRead.value : null;
    const afterPages = ghJson(gh, 'repos/' + binding.repository + '/branches/gh-pages', { env: releaseEnv });
    afterPagesSha = afterPages.present ? afterPages.value?.commit?.sha ?? null : null;
    const snapshot = ghPagesSnapshot(gh, binding.repository);

    effects.publicationPerformed = !beforeRelease && Boolean(afterRelease);
    effects.branchMutationPerformed =
      Boolean(beforePagesSha && afterPagesSha && beforePagesSha !== afterPagesSha);

    if (!afterRelease) {
      throw new Error(
        'Preview release is absent after publication attempt; script exit=' + scriptResult.status +
        '\nstdout=' + scriptResult.stdout.slice(-10000) +
        '\nstderr=' + scriptResult.stderr.slice(-10000),
      );
    }

    if (afterRelease.tag_name !== tag) throw new Error('published Preview tag mismatch');
    if (afterRelease.prerelease !== true) throw new Error('published Preview is not marked prerelease');
    if (afterRelease.target_commitish !== binding.head) throw new Error('published Preview target source mismatch');
    if (!new RegExp('^source-sha:\\s*' + binding.head + '\\s*$', 'm').test(String(afterRelease.body || ''))) {
      throw new Error('published Preview body lacks exact source-sha');
    }
    const assetNames = new Set((afterRelease.assets || []).map((asset) => asset.name));
    if (!assetNames.has('VexLife-macOS-AppleSilicon.dmg')) {
      throw new Error('published Preview lacks the Mac DMG');
    }

    const projected = snapshot?.releases?.find((release) => release.tag === tag);
    if (!snapshot || snapshot.repository !== binding.repository) {
      throw new Error('gh-pages release snapshot is unavailable or bound to another repository');
    }
    if (snapshot.mainSha !== binding.head) {
      throw new Error('gh-pages snapshot mainSha does not equal the published source');
    }
    if (!projected || projected.sourceSha !== binding.head) {
      throw new Error('gh-pages release snapshot does not contain the exact published Preview');
    }

    resultBase.completedStepReceipts.push({
      step: 'source-owned-preview-publication',
      state: scriptResult.status === 0 || scriptResult.skipped ? 'PASS' : 'FAILED_WITH_REMOTE_EFFECT',
      scriptExit: scriptResult.status,
      scriptSkippedBecauseExactReleaseAlreadyPresent: Boolean(scriptResult.skipped),
      tag,
    });
    resultBase.completedStepReceipts.push({
      step: 'remote-release-readback',
      state: 'PASS',
      release: releaseSummary(afterRelease),
    });
    resultBase.completedStepReceipts.push({
      step: 'gh-pages-release-catalog-readback',
      state: 'PASS',
      ghPagesHead: afterPagesSha,
      snapshotMainSha: snapshot.mainSha,
      projectedTag: projected.tag,
      projectedSourceSha: projected.sourceSha,
    });

    resultBase.afterState = {
      sourceHead: binding.head,
      sourceTree: binding.tree,
      previewTag: tag,
      release: releaseSummary(afterRelease),
      ghPagesHead: afterPagesSha,
      ghPagesSnapshotMainSha: snapshot.mainSha,
      ghPagesProjectedRelease: {
        tag: projected.tag,
        sourceSha: projected.sourceSha,
        channel: projected.channel,
        publishedAt: projected.publishedAt,
      },
      scriptExit: scriptResult.status,
      scriptSkippedBecauseExactReleaseAlreadyPresent: Boolean(scriptResult.skipped),
    };

    if (scriptResult.status !== 0) {
      resultBase.candidateFindings.push('PREVIEW_PUBLICATION_SCRIPT_FAILED_AFTER_REMOTE_EFFECT');
      writeResult({
        disposition: 'PARTIAL_FAIL_CLOSED',
        stopReasonOrNull: 'SOURCE_OWNED_PREVIEW_PUBLICATION_SCRIPT_FAILED',
        firstSubstantiveFailureOrNull:
          failureRecord(
            new Error(
              'publish-mac-release.sh exit=' + scriptResult.status +
              '\nstdout=' + scriptResult.stdout.slice(-8000) +
              '\nstderr=' + scriptResult.stderr.slice(-8000),
            ),
            'SOURCE_OWNED_PREVIEW_PUBLICATION',
          ),
        ...resultBase,
      });
      return;
    }

    writeResult({
      disposition: 'PASS',
      stopReasonOrNull: null,
      firstSubstantiveFailureOrNull: null,
      ...resultBase,
    });
  } catch (error) {
    const remoteReleaseNow = afterRelease || beforeRelease;
    const publicationObserved = Boolean(remoteReleaseNow);
    effects.publicationPerformed = effects.publicationPerformed || (!beforeRelease && Boolean(afterRelease));
    effects.branchMutationPerformed =
      effects.branchMutationPerformed ||
      Boolean(beforePagesSha && afterPagesSha && beforePagesSha !== afterPagesSha);

    if (publicationObserved) {
      resultBase.candidateFindings.push('PREVIEW_RELEASE_EXISTS_AT_FAILURE_BOUNDARY');
    }
    resultBase.afterState = {
      ...resultBase.afterState,
      stage,
      previewReleaseObserved: publicationObserved,
      release: releaseSummary(remoteReleaseNow),
      ghPagesHead: afterPagesSha,
      errorClass: error?.constructor?.name || 'Error',
      errorMessage: String(error?.message || error).slice(0, 1800),
    };

    writeResult({
      disposition: publicationObserved ? 'PARTIAL_FAIL_CLOSED' : 'SAFE_FAILURE',
      stopReasonOrNull: 'VEXLIFE_PREVIEW_PUBLICATION_STOPPED',
      firstSubstantiveFailureOrNull: failureRecord(error, stage),
      ...resultBase,
    });
  } finally {
    if (work) {
      try { fs.rmSync(work, { recursive: true, force: true }); } catch (_) {}
    }
  }
}

main().catch((error) => {
  try {
    writeResult({
      disposition: 'SAFE_FAILURE',
      stopReasonOrNull: 'UNCAUGHT_VEXLIFE_PREVIEW_PUBLICATION_FAILURE',
      firstSubstantiveFailureOrNull: failureRecord(error, 'UNCAUGHT'),
      candidateFindings: [],
      harnessFindings: ['UNCAUGHT_VEXLIFE_PREVIEW_PUBLICATION_FAILURE'],
      environmentFindings: [],
      unknownFindings: [],
      effects: defaultEffects(),
      beforeState: {},
      afterState: {
        errorClass: error?.constructor?.name || 'Error',
        errorMessage: String(error?.message || error).slice(0, 1800),
      },
      completedStepReceipts: [],
    });
  } catch (_) {}
  process.exitCode = 0;
});

// [VXG RealForever]
