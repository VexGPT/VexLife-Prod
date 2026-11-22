#!/usr/bin/env node
// [VXG RealForever]
// Source-controlled lived discriminator for merged EvolveContext Stage A.
// Product runtime is unchanged. The companion PR selects which proof suites run through run-plan.json.

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const EXPECTED_MAIN = '0566b1eed876a28c2fcc6586743ae1f4af7eaecd';
const EXPECTED_MAIN_TREE = '101e6bcc70e3b654ed7a95960332711bb6700a71';
const EXPECTED_FOUNDATION_VERSION = '0.6.19';
const EXPECTED_FOUNDATION_BLOB = '2b56a2a4045befed5d62189e364147cdfcd642fe';
const EXPECTED_MODEL_REF = 'model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20';
const EXPECTED_MODEL_PROFILE_REF = 'model-profile.vexlife-prod.qwen38-4b-distill-heretic-mlx4.candidate.20261114A';
const DEPTH_SCENARIO_SHA256 = '65c81bfdcc0935d261204566bfc05468317c59e962eabceffb9f7e0fca359cc1';
const STANDING_SCENARIO_SHA256 = '9a0d2c23dc357526007c5b575b74fdf795d712a1190cffc9cf95daff6e5be084';
const INTERVIEW_SCENARIO_SHA256 = '9af61b538c414599fe275e7e90f55f0914418ceb5def3715de68a39324f0c230';
const ORDINARY_CURRENTIZATION_SCENARIO_SHA256 = '60910038e557305f75bd8f56c9fa6c7bcdae17b97ff1da0236bf987824db6a93';
const DEPTH_SCENARIO_REF = 'scenario.vexlife.continuity-pressure.depth-continuity.v1';
const STANDING_SCENARIO_REF = 'scenario.vexlife.continuity-pressure.lane.claude.historical-current-state.v1';
const INTERVIEW_SCENARIO_REF = 'scenario.vexlife.evolve-context.orientation-interview.v1';
const ORDINARY_CURRENTIZATION_SCENARIO_REF = 'scenario.vexlife.evolve-context.ordinary-currentization.v1';
const DIRECT_CONVERSATION_THREAD_REF = 'thread.self-development.open-conversation';
const COBALT_ANCHOR = 'the cobalt lantern is stored beside the cedar gate';
const PROJECTION_HEADER = 'VEXLIFE_CONVERSATION_CONTEXT_PROJECTION/v1';
const PROOF_PATH = 'tools/continuity-pressure/evolve-context-lived-4b-proof.mjs';
const BOOTSTRAP_PATH = 'relay/evolve-context/lived-4b-proof/RUN-CURRENT-PLAN.command';
const FORM_PATH = 'relay/evolve-context/lived-4b-proof/form-and-run.cjs';
const PAYLOAD_PATH = 'relay/evolve-context/lived-4b-proof/payload.cjs';
const PLAN_PATH = 'relay/evolve-context/lived-4b-proof/run-plan.json';
const ADAPTER_PATH = 'src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapter.kt';
const INTERVIEW_SCENARIO_PATH = 'tools/continuity-pressure/scenarios/evolve-context-orientation-interview-v1.json';
const ORDINARY_CURRENTIZATION_SCENARIO_PATH = 'tools/continuity-pressure/scenarios/evolve-context-ordinary-currentization-v1.json';
const EXTENSION_KEY = 'vexlife-prod.claude-c20/v1';
const ALLOWED_SUITES = new Set(['reach-baseline', 'reach-treatment', 'standing-currentization', 'orientation-interview', 'ordinary-currentization']);
const GRADLE_MEMORY_ARGS = [
  '-Dorg.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g',
  '-Pkotlin.daemon.jvmargs=-Xmx4g -Xms512m',
];

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const home = path.join(os.homedir(), '.vexlife');
const evidencePath = process.env.VEX_EVOLVE_CONTEXT_EVIDENCE_JSON || null;
const requestedWorkRoot = process.env.VEX_EVOLVE_CONTEXT_WORK_ROOT || null;
const planRef = process.env.VEX_EVOLVE_CONTEXT_PLAN_REF || null;
let selectedSuites;
try { selectedSuites = JSON.parse(process.env.VEX_EVOLVE_CONTEXT_SUITE_PLAN_JSON || 'null'); }
catch { selectedSuites = null; }
if (!planRef || !Array.isArray(selectedSuites) || selectedSuites.length === 0
    || new Set(selectedSuites).size !== selectedSuites.length
    || selectedSuites.some((suite) => !ALLOWED_SUITES.has(suite))) {
  throw new Error('EVOLVECONTEXT_SUITE_PLAN_INVALID');
}
const workParent = requestedWorkRoot ? path.resolve(requestedWorkRoot) : os.tmpdir();
fs.mkdirSync(workParent, { recursive: true });
const workRoot = fs.mkdtempSync(path.join(workParent, 'evolve-context-lived-4b-'));

const state = {
  schemaVersion: 'vexlife-prod.evolve-context-lived-4b-proof/v2',
  executionClass: 'LIVED_EVIDENCE_ACQUISITION',
  planRef,
  selectedSuites,
  completedSuites: [],
  proofExecution: {
    gradleJvmMaxHeap: '4g',
    kotlinDaemonMaxHeap: '4g',
    kotlinDaemonInitialHeap: '512m',
  },
  productMain: EXPECTED_MAIN,
  proofHead: null,
  foundationVersion: null,
  currentState: null,
  writerCurrentnessBefore: null,
  writerCurrentnessAfter: null,
  modelRuntimeAttempted: false,
  preflight: null,
  interviewPreflight: null,
  interview: null,
  ordinaryCurrentizationPreflight: null,
  ordinaryCurrentization: null,
  baseline: null,
  treatment: null,
  standing: null,
  mechanicalState: 'NOT_RUN',
  semanticJudgment: 'NOT_REACHED',
  failure: null,
};

function sha256File(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

function writeEvidence() {
  if (!evidencePath) return;
  fs.mkdirSync(path.dirname(evidencePath), { recursive: true });
  fs.writeFileSync(evidencePath, JSON.stringify(state, null, 2) + '\n');
}

function fail(message) {
  const error = new Error(message);
  error.code = String(message).split(':')[0];
  throw error;
}

function run(command, args, { cwd = repo, timeoutMs = 1_800_000, env = {} } = {}) {
  const startedAt = new Date().toISOString();
  const result = spawnSync(command, args, {
    cwd,
    env: { ...process.env, ...env },
    encoding: 'utf8',
    timeout: timeoutMs,
    maxBuffer: 32 * 1024 * 1024,
    shell: false,
  });
  return {
    command,
    args,
    cwd,
    startedAt,
    completedAt: new Date().toISOString(),
    exitCode: Number.isInteger(result.status) ? result.status : null,
    signal: result.signal || null,
    error: result.error ? String(result.error.message || result.error) : null,
    stdout: result.stdout || '',
    stderr: result.stderr || '',
  };
}

function requirePass(result, label) {
  if (result.error || result.exitCode !== 0) {
    process.stderr.write(`\n[${label}] exit=${result.exitCode} error=${result.error || ''}\n`);
    if (result.stdout) process.stderr.write(`--- stdout ---\n${result.stdout}\n`);
    if (result.stderr) process.stderr.write(`--- stderr ---\n${result.stderr}\n`);
    fail(`${label}_FAILED:${result.exitCode ?? 'NO_EXIT'}`);
  }
  return result;
}

function git(...args) {
  return requirePass(run('/usr/bin/git', args, { cwd: repo, timeoutMs: 120_000 }), 'GIT').stdout.trim();
}

function parseLastJson(text, label) {
  const source = String(text);
  let depth = 0;
  let start = -1;
  let inString = false;
  let escaped = false;
  let lastValue = null;

  for (let i = 0; i < source.length; i += 1) {
    const ch = source[i];

    if (inString) {
      if (escaped) {
        escaped = false;
      } else if (ch === '\\') {
        escaped = true;
      } else if (ch === '"') {
        inString = false;
      }
      continue;
    }

    if (ch === '"') {
      inString = true;
      continue;
    }

    if (ch === '{') {
      if (depth === 0) start = i;
      depth += 1;
      continue;
    }

    if (ch === '}' && depth > 0) {
      depth -= 1;
      if (depth === 0 && start >= 0) {
        const candidate = source.slice(start, i + 1);
        try { lastValue = JSON.parse(candidate); } catch { /* keep scanning */ }
        start = -1;
      }
    }
  }

  if (lastValue != null) return lastValue;
  fail(`${label}_JSON_NOT_FOUND`);
}

function walkFiles(root, predicate) {
  const out = [];
  if (!fs.existsSync(root)) return out;
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const p = path.join(root, entry.name);
    if (entry.isDirectory()) out.push(...walkFiles(p, predicate));
    else if (entry.isFile() && predicate(p)) out.push(p);
  }
  return out;
}

function processState(pid) {
  try {
    process.kill(pid, 0);
    return 'ACTIVE';
  } catch (error) {
    if (error?.code === 'ESRCH') return 'ABSENT';
    if (error?.code === 'EPERM') return 'ACTIVE';
    return 'UNVERIFIABLE';
  }
}

function observeExactThreadWriterCurrentness() {
  const manifestPath = path.join(home, 'config', 'home.json');
  if (!fs.existsSync(manifestPath)) fail('REAL_HOME_MANIFEST_MISSING');

  let manifest;
  try { manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8')); }
  catch { fail('REAL_HOME_MANIFEST_UNREADABLE'); }

  const lineage = manifest?.currentCompanionLineageRef;
  if (typeof lineage !== 'string' || !lineage.startsWith('companion-lineage.vexlife.')) {
    fail('REAL_HOME_CURRENT_LINEAGE_INVALID');
  }

  const relativeLockPath = path.posix.join(
    'runtime', 'thread-writer-locks', lineage, DIRECT_CONVERSATION_THREAD_REF + '.lock',
  );
  const lockPath = path.join(home, ...relativeLockPath.split('/'));
  const allLocksRoot = path.join(home, 'runtime', 'thread-writer-locks');
  const allWriterLockCount = walkFiles(allLocksRoot, (p) => p.endsWith('.lock')).length;

  if (!fs.existsSync(lockPath)) {
    return {
      state: 'IDLE',
      ownerState: null,
      instanceRef: null,
      leaseSha256: null,
      pid: null,
      formedAt: null,
      relativeLockPath,
      allWriterLockCount,
    };
  }

  let lease;
  try { lease = JSON.parse(fs.readFileSync(lockPath, 'utf8')); }
  catch {
    return {
      state: 'BLOCKED',
      ownerState: 'UNVERIFIABLE',
      instanceRef: null,
      leaseSha256: null,
      pid: null,
      formedAt: null,
      relativeLockPath,
      allWriterLockCount,
      reason: 'EXACT_CURRENT_THREAD_LEASE_UNREADABLE',
    };
  }

  const minimallyValid =
    lease?.schemaVersion === 'vexlife.thread-writer-lease/v1'
    && lease?.companionLineageRef === lineage
    && lease?.threadRef === DIRECT_CONVERSATION_THREAD_REF
    && typeof lease?.instanceRef === 'string'
    && lease.instanceRef.length > 0
    && Number.isInteger(lease?.pid)
    && lease.pid > 0
    && typeof lease?.leaseSha256 === 'string'
    && /^[0-9a-f]{64}$/.test(lease.leaseSha256);

  if (!minimallyValid) {
    return {
      state: 'BLOCKED',
      ownerState: 'UNVERIFIABLE',
      instanceRef: typeof lease?.instanceRef === 'string' ? lease.instanceRef : null,
      leaseSha256: typeof lease?.leaseSha256 === 'string' ? lease.leaseSha256 : null,
      pid: Number.isInteger(lease?.pid) ? lease.pid : null,
      formedAt: typeof lease?.formedAt === 'string' ? lease.formedAt : null,
      relativeLockPath,
      allWriterLockCount,
      reason: 'EXACT_CURRENT_THREAD_LEASE_INVALID_SHAPE',
    };
  }

  const ownerState = processState(lease.pid);
  return {
    state: ownerState === 'ACTIVE' ? 'ACTIVE' : ownerState === 'ABSENT' ? 'RECOVERY_REQUIRED' : 'BLOCKED',
    ownerState,
    instanceRef: lease.instanceRef,
    leaseSha256: lease.leaseSha256,
    pid: lease.pid,
    formedAt: typeof lease.formedAt === 'string' ? lease.formedAt : null,
    relativeLockPath,
    allWriterLockCount,
    reason: null,
  };
}

function requireWriterIdle(observation, suffix = '') {
  if (observation.state === 'IDLE') return;
  const detail = [
    observation.state,
    observation.ownerState || 'NONE',
    observation.instanceRef || 'NONE',
    observation.leaseSha256 || 'NONE',
  ].join(':');
  fail('REAL_HOME_CONVERSATION_WRITER_' + observation.state + suffix + ':' + detail);
}

function findProbe(result) {
  const turns = Array.isArray(result.turns) ? result.turns : [];
  const probe = turns.find((turn) => turn.scenarioTurnRef === 'probe.01');
  if (!probe) fail('PROBE_01_MISSING');
  return probe;
}

function findHarnessZip(runDir) {
  const files = fs.readdirSync(runDir).filter((name) => name.endsWith('.zip'));
  if (files.length !== 1) fail(`HARNESS_ZIP_COUNT:${files.length}`);
  return path.join(runDir, files[0]);
}

function firstProviderCallMessages(runDir, probe) {
  const zip = findHarnessZip(runDir);
  const extracted = requirePass(
    run('/usr/bin/unzip', ['-p', zip, 'implementation/provider-calls.jsonl'], { timeoutMs: 120_000 }),
    'PROVIDER_CALL_EVIDENCE',
  ).stdout;
  const rows = extracted.split(/\r?\n/).filter(Boolean).map((line) => JSON.parse(line));
  const row = rows.find((entry) => Number(entry.ordinal) === Number(probe.ordinal) && Number(entry.call) === 0);
  if (!row || !Array.isArray(row.messages)) fail('FIRST_PROVIDER_CALL_NOT_FOUND');
  return row.messages;
}

function residencyState(probe, scenarioTurnRef) {
  const row = (probe.residency || []).find(
    (item) => String(item.ref || '').startsWith(scenarioTurnRef + ':'),
  );
  return row?.state || null;
}

function formation01Residency(probe) {
  return residencyState(probe, 'formation.01');
}

function extension(probe) {
  return probe?.implementationExtensions?.[EXTENSION_KEY] || {};
}

function summarizeRun(label, runDir) {
  const resultPath = path.join(runDir, 'result.json');
  if (!fs.existsSync(resultPath)) fail(`CONTINUITY_${label.toUpperCase()}_RESULT_MISSING`);
  const result = JSON.parse(fs.readFileSync(resultPath, 'utf8'));
  const probe = findProbe(result);
  const messages = firstProviderCallMessages(runDir, probe);
  const prompt = probe.prompt || {};
  return {
    label,
    status: result.status || null,
    executionClass: result.executionClass || null,
    scenarioRef: result.scenarioRef || null,
    scratchDeletedAfterBundle: result.scratch?.deletedAfterBundle ?? null,
    probe: {
      ordinal: probe.ordinal,
      responseText: probe.responseText,
      residencyFormation01: formation01Residency(probe),
      contextMode: prompt.contextMode || null,
      conversationContextProjectionState: prompt.conversationContextProjectionState || null,
      conversationContextProjectionSourceEventRefs: prompt.conversationContextProjectionSourceEventRefs || [],
      sourceReturnSourceEventRefs: prompt.sourceReturnSourceEventRefs || [],
      firstProviderCallCarriesCobalt: messages.some((m) => String(m.content || '').includes(COBALT_ANCHOR)),
      firstProviderCallCarriesProjectionHeader: messages.some((m) => String(m.content || '').startsWith(PROJECTION_HEADER)),
      providerCallCount: extension(probe).providerCallCount ?? null,
      capabilityUses: probe.capabilityUses || [],
      canonicalReplyLanguage: extension(probe).canonicalReplyLanguage ?? null,
      carriedReplyLanguages: extension(probe).carriedReplyLanguages || [],
      assertions: probe.assertions || [],
    },
  };
}

function summarizeInterview(runDir, baseSummary) {
  const resultPath = path.join(runDir, 'result.json');
  if (!fs.existsSync(resultPath)) fail('INTERVIEW_RESULT_MISSING');
  const result = JSON.parse(fs.readFileSync(resultPath, 'utf8'));
  const scenario = JSON.parse(fs.readFileSync(path.join(repo, INTERVIEW_SCENARIO_PATH), 'utf8'));
  const inputByTurnRef = new Map(
    scenario.sets.flatMap((set) => set.turns).map((turn) => [turn.turnRef, turn.input]),
  );
  const interviewTurns = (Array.isArray(result.turns) ? result.turns : [])
    .filter((turn) => /^probe\.0[1-5]$/.test(String(turn.scenarioTurnRef || '')));
  const responses = interviewTurns.map((turn, index) => {
    const messages = firstProviderCallMessages(runDir, turn);
    const currentInput = inputByTurnRef.get(turn.scenarioTurnRef) || null;
    const priorResponse = index > 0 ? interviewTurns[index - 1].responseText || '' : null;
    const currentRequestMessageIndex =
      currentInput == null ? null : messages.findIndex((m) => m.role === 'user' && m.content === currentInput);
    const priorResponseMessageIndex =
      priorResponse == null ? null : messages.findIndex((m) => m.role === 'assistant' && m.content === priorResponse);
    const components = turn.prompt?.contextComponents || [];
    const currentRequestComponent =
      components.find((c) => String(c.componentRef || '').endsWith('current-request')) || null;
    const conversationHistoryComponents =
      components.filter((c) =>
        String(c.componentRef || '').endsWith('conversation-history')
        || String(c.componentRef || '').endsWith('successor-prior-response')
      );
    return {
      turnRef: turn.scenarioTurnRef,
      responseText: turn.responseText || '',
      contextMode: turn.prompt?.contextMode || null,
      residencyFormation01: formation01Residency(turn),
      conversationContextProjectionState: turn.prompt?.conversationContextProjectionState || null,
      sourceReturnSourceEventRefs: turn.prompt?.sourceReturnSourceEventRefs || [],
      capabilityUses: turn.capabilityUses || [],
      providerCallCount: extension(turn).providerCallCount ?? null,
      priorCompletedExchangeCount: turn.prompt?.priorCompletedExchangeCount ?? null,
      selectedConversationEventRefs: turn.prompt?.selectedConversationEventRefs || [],
      overlapTail: extension(turn).overlapTail ?? null,
      currentRequestProviderMessageIndex: currentRequestMessageIndex >= 0 ? currentRequestMessageIndex : null,
      currentRequestPresentExact: currentRequestMessageIndex >= 0,
      immediatelyPriorInterviewResponseProviderMessageIndex:
        priorResponseMessageIndex >= 0 ? priorResponseMessageIndex : null,
      immediatelyPriorInterviewResponsePresentExact:
        priorResponse == null ? null : priorResponseMessageIndex >= 0,
      currentRequestComponent,
      conversationHistoryComponents,
      providerMessageCount: messages.length,
      providerMessageRoles: messages.map((m) => m.role),
    };
  });
  if (responses.length !== 5) fail(`INTERVIEW_RESPONSE_COUNT:${responses.length}`);
  return {
    ...baseSummary,
    setupExecutionClass: 'DETERMINISTIC_FIXTURE',
    liveProviderTurnRefs: result.liveProviderTurnRefs || [],
    fixtureProviderTurnRefs: result.fixtureProviderTurnRefs || [],
    perceptionGroundAudit: {
      currentRequestCheckedAgainstExactProviderMessages: true,
      priorLiveResponseCheckedAgainstExactProviderMessages: true,
      durableExtentComparedWithResidentEventRefs: true,
    },
    responses,
  };
}

function summarizeOrdinaryCurrentization(runDir, baseSummary) {
  const resultPath = path.join(runDir, 'result.json');
  if (!fs.existsSync(resultPath)) fail('ORDINARY_CURRENTIZATION_RESULT_MISSING');
  const result = JSON.parse(fs.readFileSync(resultPath, 'utf8'));
  const probe = (Array.isArray(result.turns) ? result.turns : [])
    .find((turn) => String(turn.scenarioTurnRef || '') === 'live.01');
  if (!probe) fail('ORDINARY_CURRENTIZATION_LIVE_PROBE_MISSING');
  const messages = firstProviderCallMessages(runDir, probe);
  const oldText = 'For Saturday, Sarah and I are meeting at Harbor Cafe at 9:00 a.m.';
  const currentText = 'Actually Sarah just texted: change of plan for Saturday.';
  const currentRequest =
    "What did we decide about where and when I'm meeting Sarah on Saturday?";
  const currentRequestIndex =
    messages.findIndex((message) => message.role === 'user' && message.content === currentRequest);
  return {
    ...baseSummary,
    setupExecutionClass: 'DETERMINISTIC_FIXTURE',
    liveProviderTurnRefs: result.liveProviderTurnRefs || [],
    fixtureProviderTurnRefs: result.fixtureProviderTurnRefs || [],
    lived: {
      responseText: probe.responseText || '',
      oldPlanResidency: residencyState(probe, 'plan.old'),
      correctedPlanResidency: residencyState(probe, 'plan.current'),
      conversationContextProjectionState: probe.prompt?.conversationContextProjectionState || null,
      conversationContextProjectionSourceEventRefs:
        probe.prompt?.conversationContextProjectionSourceEventRefs || [],
      sourceReturnSourceEventRefs: probe.prompt?.sourceReturnSourceEventRefs || [],
      firstProviderCallCarriesOldPlan:
        messages.some((message) => String(message.content || '').includes(oldText)),
      firstProviderCallCarriesCorrectedPlan:
        messages.some((message) => String(message.content || '').includes(currentText)),
      currentHumanRequestPresentExact: currentRequestIndex >= 0,
      currentHumanRequestIsLast:
        currentRequestIndex >= 0 && currentRequestIndex === messages.length - 1,
      providerCallCount: extension(probe).providerCallCount ?? null,
      capabilityUses: probe.capabilityUses || [],
    },
  };
}

function runProjectionHarness(label, scenarioRef, scenarioFile, options) {
  const runDir = path.join(workRoot, label);
  fs.mkdirSync(runDir, { recursive: true });
  const args = [
    '--stage', 'projection',
    '--scenario-ref', scenarioRef,
    '--scenario-file', scenarioFile,
    '--result-dir', runDir,
    '--source-sha', state.proofHead,
    '--source-clean', 'true',
    '--conversation-context-projection', options.projection ? 'true' : 'false',
    '--effective-input-token-budget', String(options.budget),
    '--payload-scale', String(options.payloadScale),
  ];
  if (options.sets != null) args.push('--sets', String(options.sets));
  if (options.turnsPerSet != null) args.push('--turns-per-set', String(options.turnsPerSet));

  const gradle = run('./gradlew', [
    '--no-daemon', '-q', '--console=plain', ...GRADLE_MEMORY_ARGS, 'continuityPressure', `--args=${args.join(' ')}`,
  ], { cwd: repo, timeoutMs: 1_800_000 });
  if (gradle.error || gradle.exitCode !== 0) {
    const resultPath = path.join(runDir, 'result.json');
    const detail = fs.existsSync(resultPath) ? fs.readFileSync(resultPath, 'utf8') : '';
    process.stderr.write(`\n[projection-${label}] exit=${gradle.exitCode} error=${gradle.error || ''}\n`);
    if (gradle.stdout) process.stderr.write(`--- stdout ---\n${gradle.stdout}\n`);
    if (gradle.stderr) process.stderr.write(`--- stderr ---\n${gradle.stderr}\n`);
    if (detail) process.stderr.write(`--- adapter result ---\n${detail}\n`);
    fail(`PROJECTION_${label.toUpperCase()}_FAILED:${gradle.exitCode ?? 'NO_EXIT'}`);
  }
  const summary = summarizeRun(label, runDir);
  if (summary.scratchDeletedAfterBundle !== true) fail(`PROJECTION_${label.toUpperCase()}_SCRATCH_NOT_DELETED`);
  return summary;
}

function runHarness(label, scenarioRef, scenarioFile, options) {
  const runDir = path.join(workRoot, label);
  fs.mkdirSync(runDir, { recursive: true });
  const args = [
    '--stage', 'quick',
    '--scenario-ref', scenarioRef,
    '--scenario-file', scenarioFile,
    '--result-dir', runDir,
    '--source-sha', state.proofHead,
    '--source-clean', 'true',
    '--runtime-home', home,
    '--experiment-window', 'true',
    '--runtime-start-at-bind', 'true',
    '--conversation-context-projection', options.projection ? 'true' : 'false',
    '--effective-input-token-budget', String(options.budget),
    '--payload-scale', String(options.payloadScale),
  ];
  if (options.sets != null) args.push('--sets', String(options.sets));
  if (options.turnsPerSet != null) args.push('--turns-per-set', String(options.turnsPerSet));

  state.modelRuntimeAttempted = true;
  writeEvidence();
  const gradle = run('./gradlew', [
    '--no-daemon', '-q', '--console=plain', ...GRADLE_MEMORY_ARGS, 'continuityPressure', `--args=${args.join(' ')}`,
  ], { cwd: repo, timeoutMs: 1_800_000 });
  if (gradle.error || gradle.exitCode !== 0) {
    const heldPath = path.join(runDir, 'held.json');
    const resultPath = path.join(runDir, 'result.json');
    const detail = fs.existsSync(resultPath)
      ? fs.readFileSync(resultPath, 'utf8')
      : fs.existsSync(heldPath) ? fs.readFileSync(heldPath, 'utf8') : '';
    process.stderr.write(`\n[continuity-${label}] exit=${gradle.exitCode} error=${gradle.error || ''}\n`);
    if (gradle.stdout) process.stderr.write(`--- stdout ---\n${gradle.stdout}\n`);
    if (gradle.stderr) process.stderr.write(`--- stderr ---\n${gradle.stderr}\n`);
    if (detail) process.stderr.write(`--- adapter result ---\n${detail}\n`);
    fail(`CONTINUITY_${label.toUpperCase()}_FAILED:${gradle.exitCode ?? 'NO_EXIT'}`);
  }
  const summary = summarizeRun(label, runDir);
  if (summary.scratchDeletedAfterBundle !== true) fail(`CONTINUITY_${label.toUpperCase()}_SCRATCH_NOT_DELETED`);
  return summary;
}

function ensureProofSource() {
  state.proofHead = git('rev-parse', 'HEAD');
  const mergeBase = git('merge-base', 'HEAD', EXPECTED_MAIN);
  if (mergeBase !== EXPECTED_MAIN) fail(`PROOF_BASE_MISMATCH:${mergeBase}`);
  const baseTree = git('rev-parse', `${EXPECTED_MAIN}^{tree}`);
  if (baseTree !== EXPECTED_MAIN_TREE) fail(`MAIN_TREE_MISMATCH:${baseTree}`);
  const status = git('status', '--porcelain=v1', '-uall');
  if (status !== '') fail('PROOF_SOURCE_DIRTY');
  const changed = git('diff', '--name-only', `${EXPECTED_MAIN}..HEAD`).split(/\r?\n/).filter(Boolean).sort();
  const allowed = [
    ADAPTER_PATH,
    BOOTSTRAP_PATH,
    FORM_PATH,
    INTERVIEW_SCENARIO_PATH,
    ORDINARY_CURRENTIZATION_SCENARIO_PATH,
    PAYLOAD_PATH,
    PLAN_PATH,
    PROOF_PATH,
  ].sort();
  if (changed.length !== allowed.length || changed.some((p, index) => p !== allowed[index])) {
    fail(`PROOF_PATH_MEMBRANE_MISMATCH:${changed.join(',')}`);
  }
  const sourcePlan = JSON.parse(fs.readFileSync(path.join(repo, PLAN_PATH), 'utf8'));
  if (sourcePlan.planRef !== planRef || JSON.stringify(sourcePlan.suites) !== JSON.stringify(selectedSuites)) {
    fail('SOURCE_PLAN_BINDING_MISMATCH');
  }
  const foundationPath = 'src/jvmMain/resources/foundational/vex-foundational-field.json';
  const baseFoundationBlob = git('rev-parse', `${EXPECTED_MAIN}:${foundationPath}`);
  const proofFoundationBlob = git('rev-parse', `HEAD:${foundationPath}`);
  if (baseFoundationBlob !== EXPECTED_FOUNDATION_BLOB || proofFoundationBlob !== EXPECTED_FOUNDATION_BLOB) {
    fail(`FOUNDATION_BLOB_CHANGED:${baseFoundationBlob}:${proofFoundationBlob}`);
  }
  const foundation = JSON.parse(fs.readFileSync(path.join(repo, foundationPath), 'utf8'));
  state.foundationVersion = foundation.version;
  if (foundation.version !== EXPECTED_FOUNDATION_VERSION) fail(`FOUNDATION_VERSION_CHANGED:${foundation.version}`);
}

function observeCurrentState() {
  const result = requirePass(run('./gradlew', [
    '--no-daemon', '-q', '--console=plain', ...GRADLE_MEMORY_ARGS, 'observeVexLifeProdCurrentState',
  ], { cwd: repo, timeoutMs: 1_200_000 }), 'CURRENT_STATE_OBSERVATION');
  const current = parseLastJson(result.stdout, 'CURRENT_STATE');
  state.currentState = current;
  if (current.homePath !== home || current.effectsPerformed !== false) fail('CURRENT_STATE_HOME_OR_EFFECT_MISMATCH');
  if (current.activeModelRef !== EXPECTED_MODEL_REF || current.activeModelProfileRef !== EXPECTED_MODEL_PROFILE_REF) {
    fail(`SELECTED_MODEL_NOT_EXPECTED_4B:${current.activeModelRef || 'NONE'}:${current.activeModelProfileRef || 'NONE'}`);
  }
  if (current.modelArtifactMissingBytes !== 0 || current.modelArtifactInvalidSizeBytes !== 0) {
    fail(`SELECTED_4B_ARTIFACT_NOT_EXACTLY_PRESENT:${current.modelArtifactMissingBytes}:${current.modelArtifactInvalidSizeBytes}`);
  }
}

try {
  ensureProofSource();
  if (!fs.existsSync(home) || !fs.statSync(home).isDirectory()) fail(`DEFAULT_PRODUCT_HOME_NOT_PRESENT:${home}`);
  state.writerCurrentnessBefore = observeExactThreadWriterCurrentness();
  writeEvidence();
  requireWriterIdle(state.writerCurrentnessBefore);

  const depthScenario = path.join(repo, 'tools/continuity-pressure/scenarios/depth-continuity-v1.json');
  const standingScenario = path.join(repo, 'tools/continuity-pressure/scenarios/lane/historical-current-state-v1.json');
  const interviewScenario = path.join(repo, INTERVIEW_SCENARIO_PATH);
  const ordinaryCurrentizationScenario = path.join(repo, ORDINARY_CURRENTIZATION_SCENARIO_PATH);

  const reachSuitesSelected =
    selectedSuites.includes('reach-baseline') || selectedSuites.includes('reach-treatment');
  const reachProfile = { budget: 52_000, payloadScale: 7, sets: 3, turnsPerSet: 5 };

  if (reachSuitesSelected) {
    if (sha256File(depthScenario) !== DEPTH_SCENARIO_SHA256) fail('DEPTH_SCENARIO_DIGEST_MISMATCH');
    const projectionBaseline = runProjectionHarness(
      'preflight-baseline',
      DEPTH_SCENARIO_REF,
      depthScenario,
      { projection: false, ...reachProfile },
    );
    const projectionTreatment = runProjectionHarness(
      'preflight-treatment',
      DEPTH_SCENARIO_REF,
      depthScenario,
      { projection: true, ...reachProfile },
    );
    state.preflight = {
      profile: reachProfile,
      baseline: projectionBaseline,
      treatment: projectionTreatment,
    };
    writeEvidence();

    if (projectionBaseline.probe.firstProviderCallCarriesCobalt !== false) {
      fail('PREFLIGHT_BASELINE_FIRST_PROVIDER_CALL_CARRIES_COBALT');
    }
    if (projectionTreatment.probe.firstProviderCallCarriesCobalt !== true) {
      fail('PREFLIGHT_TREATMENT_FIRST_PROVIDER_CALL_MISSING_COBALT');
    }
    if (projectionTreatment.probe.firstProviderCallCarriesProjectionHeader !== true) {
      fail('PREFLIGHT_TREATMENT_FIRST_PROVIDER_CALL_MISSING_PROJECTION_HEADER');
    }
    if (!Array.isArray(projectionTreatment.probe.conversationContextProjectionSourceEventRefs)
        || projectionTreatment.probe.conversationContextProjectionSourceEventRefs.length === 0) {
      fail('PREFLIGHT_TREATMENT_PROJECTION_SOURCE_REFS_EMPTY');
    }
    if (!Array.isArray(projectionTreatment.probe.sourceReturnSourceEventRefs)
        || projectionTreatment.probe.sourceReturnSourceEventRefs.length !== 0) {
      fail('PREFLIGHT_TREATMENT_IMPERSONATES_SOURCE_RETURN');
    }
  }

  if (selectedSuites.includes('standing-currentization')) {
    if (sha256File(standingScenario) !== STANDING_SCENARIO_SHA256) {
      fail('STANDING_SCENARIO_DIGEST_MISMATCH');
    }
  }

  if (selectedSuites.includes('orientation-interview')) {
    if (sha256File(interviewScenario) !== INTERVIEW_SCENARIO_SHA256) {
      fail('INTERVIEW_SCENARIO_DIGEST_MISMATCH');
    }
    state.interviewPreflight = runProjectionHarness(
      'interview-preflight',
      INTERVIEW_SCENARIO_REF,
      interviewScenario,
      { projection: false, ...reachProfile },
    );
    writeEvidence();
    if (state.interviewPreflight.probe.firstProviderCallCarriesCobalt !== false) {
      fail('INTERVIEW_PREFLIGHT_FIRST_PROVIDER_CALL_CARRIES_COBALT');
    }
  }

  if (selectedSuites.includes('ordinary-currentization')) {
    if (sha256File(ordinaryCurrentizationScenario) !== ORDINARY_CURRENTIZATION_SCENARIO_SHA256) {
      fail('ORDINARY_CURRENTIZATION_SCENARIO_DIGEST_MISMATCH');
    }
    const profile = { budget: 52_000, payloadScale: 1 };
    const baseline = runProjectionHarness(
      'ordinary-currentization-preflight-baseline',
      ORDINARY_CURRENTIZATION_SCENARIO_REF,
      ordinaryCurrentizationScenario,
      { projection: false, ...profile },
    );
    const treatment = runProjectionHarness(
      'ordinary-currentization-preflight-treatment',
      ORDINARY_CURRENTIZATION_SCENARIO_REF,
      ordinaryCurrentizationScenario,
      { projection: true, ...profile },
    );
    state.ordinaryCurrentizationPreflight = { profile, baseline, treatment };
    writeEvidence();

    const baselineOld = baseline.probe?.oldPlanResidency
      || baseline.probe?.residencyOldPlan
      || null;
    const baselineCurrent = baseline.probe?.correctedPlanResidency
      || baseline.probe?.residencyCorrectedPlan
      || null;
    // summarizeRun does not specialize these refs; inspect the raw deterministic result below.
    const baselineResult = JSON.parse(
      fs.readFileSync(path.join(workRoot, 'ordinary-currentization-preflight-baseline', 'result.json'), 'utf8'),
    );
    const treatmentResult = JSON.parse(
      fs.readFileSync(path.join(workRoot, 'ordinary-currentization-preflight-treatment', 'result.json'), 'utf8'),
    );
    const baselineProbe = findProbe(baselineResult);
    const treatmentProbe = findProbe(treatmentResult);
    const baselineOldState = residencyState(baselineProbe, 'plan.old');
    const baselineCurrentState = residencyState(baselineProbe, 'plan.current');
    const treatmentOldState = residencyState(treatmentProbe, 'plan.old');
    const treatmentCurrentState = residencyState(treatmentProbe, 'plan.current');
    const treatmentMessages = firstProviderCallMessages(
      path.join(workRoot, 'ordinary-currentization-preflight-treatment'),
      treatmentProbe,
    );

    state.ordinaryCurrentizationPreflight.sourceAudit = {
      baselineOldPlan: baselineOldState,
      baselineCorrectedPlan: baselineCurrentState,
      treatmentOldPlan: treatmentOldState,
      treatmentCorrectedPlan: treatmentCurrentState,
      treatmentCarriesOldPlan:
        treatmentMessages.some((message) => String(message.content || '').includes('Harbor Cafe at 9:00')),
      treatmentCarriesCorrectedPlan:
        treatmentMessages.some((message) => String(message.content || '').includes('Cedar Station at 10:00')),
      treatmentProjectionSourceEventRefs:
        treatmentProbe.prompt?.conversationContextProjectionSourceEventRefs || [],
    };
    writeEvidence();

    if (baselineOldState !== 'OMITTED_BUDGET' || baselineCurrentState !== 'OMITTED_BUDGET') {
      fail(`ORDINARY_CURRENTIZATION_BASELINE_NOT_OMITTED:${baselineOldState}:${baselineCurrentState}`);
    }
    if (treatmentOldState !== 'CONVERSATION_CONTEXT_PROJECTION_THIS_TURN'
        || treatmentCurrentState !== 'CONVERSATION_CONTEXT_PROJECTION_THIS_TURN') {
      fail(`ORDINARY_CURRENTIZATION_TREATMENT_NOT_PROJECTED:${treatmentOldState}:${treatmentCurrentState}`);
    }
    if (!state.ordinaryCurrentizationPreflight.sourceAudit.treatmentCarriesOldPlan
        || !state.ordinaryCurrentizationPreflight.sourceAudit.treatmentCarriesCorrectedPlan) {
      fail('ORDINARY_CURRENTIZATION_TREATMENT_SOURCE_TEXT_INCOMPLETE');
    }
  }

  observeCurrentState();
  writeEvidence();

  if (selectedSuites.includes('orientation-interview')) {
    const runDir = path.join(workRoot, 'orientation-interview');
    const baseInterview = runHarness(
      'orientation-interview',
      INTERVIEW_SCENARIO_REF,
      interviewScenario,
      { projection: false, ...reachProfile },
    );
    state.interview = summarizeInterview(runDir, baseInterview);
    state.completedSuites.push('orientation-interview');
    writeEvidence();
  }

  if (selectedSuites.includes('ordinary-currentization')) {
    const runDir = path.join(workRoot, 'ordinary-currentization');
    const base = runHarness(
      'ordinary-currentization',
      ORDINARY_CURRENTIZATION_SCENARIO_REF,
      ordinaryCurrentizationScenario,
      { projection: true, budget: 52_000, payloadScale: 1 },
    );
    state.ordinaryCurrentization = summarizeOrdinaryCurrentization(runDir, base);
    if (state.ordinaryCurrentization.lived.oldPlanResidency
        !== 'CONVERSATION_CONTEXT_PROJECTION_THIS_TURN'
        || state.ordinaryCurrentization.lived.correctedPlanResidency
        !== 'CONVERSATION_CONTEXT_PROJECTION_THIS_TURN') {
      fail('ORDINARY_CURRENTIZATION_LIVE_SOURCE_NOT_PROJECTED');
    }
    if (!state.ordinaryCurrentization.lived.firstProviderCallCarriesOldPlan
        || !state.ordinaryCurrentization.lived.firstProviderCallCarriesCorrectedPlan) {
      fail('ORDINARY_CURRENTIZATION_LIVE_SOURCE_TEXT_INCOMPLETE');
    }
    if (!state.ordinaryCurrentization.lived.currentHumanRequestPresentExact
        || !state.ordinaryCurrentization.lived.currentHumanRequestIsLast) {
      fail('ORDINARY_CURRENTIZATION_LIVE_REQUEST_NOT_FOREGROUND');
    }
    state.completedSuites.push('ordinary-currentization');
    writeEvidence();
  }

  if (selectedSuites.includes('reach-baseline')) {
    state.baseline = runHarness('baseline', DEPTH_SCENARIO_REF, depthScenario, {
      projection: false, ...reachProfile,
    });
    if (state.baseline.probe.firstProviderCallCarriesCobalt !== false) {
      fail('BASELINE_FIRST_PROVIDER_CALL_UNEXPECTEDLY_CARRIES_COBALT');
    }
    state.completedSuites.push('reach-baseline');
    writeEvidence();
  }

  if (selectedSuites.includes('reach-treatment')) {
    state.treatment = runHarness('treatment', DEPTH_SCENARIO_REF, depthScenario, {
      projection: true, ...reachProfile,
    });
    if (state.treatment.probe.firstProviderCallCarriesCobalt !== true) {
      fail('TREATMENT_FIRST_PROVIDER_CALL_MISSING_COBALT');
    }
    if (state.treatment.probe.firstProviderCallCarriesProjectionHeader !== true) {
      fail('TREATMENT_FIRST_PROVIDER_CALL_MISSING_PROJECTION_HEADER');
    }
    if (!Array.isArray(state.treatment.probe.conversationContextProjectionSourceEventRefs)
        || state.treatment.probe.conversationContextProjectionSourceEventRefs.length === 0) {
      fail('TREATMENT_PROJECTION_SOURCE_REFS_EMPTY');
    }
    if (!Array.isArray(state.treatment.probe.sourceReturnSourceEventRefs)
        || state.treatment.probe.sourceReturnSourceEventRefs.length !== 0) {
      fail('TREATMENT_IMPERSONATES_SOURCE_RETURN');
    }
    state.completedSuites.push('reach-treatment');
    writeEvidence();
  }

  if (selectedSuites.includes('standing-currentization')) {
    state.standing = runHarness('standing', STANDING_SCENARIO_REF, standingScenario, {
      projection: true, budget: 52_000, payloadScale: 1,
    });
    state.completedSuites.push('standing-currentization');
    writeEvidence();
  }

  state.writerCurrentnessAfter = observeExactThreadWriterCurrentness();
  writeEvidence();
  requireWriterIdle(state.writerCurrentnessAfter, '_AFTER');

  state.mechanicalState = 'PASS';
  state.semanticJudgment = 'CONSUME_RESPONSE_TEXT';
  writeEvidence();

  const output = {
    schemaVersion: state.schemaVersion,
    executionClass: state.executionClass,
    planRef: state.planRef,
    selectedSuites: state.selectedSuites,
    completedSuites: state.completedSuites,
    proofExecution: state.proofExecution,
    writerCurrentnessBefore: state.writerCurrentnessBefore,
    writerCurrentnessAfter: state.writerCurrentnessAfter,
    preflight: state.preflight,
    interviewPreflight: state.interviewPreflight,
    interview: state.interview,
    ordinaryCurrentizationPreflight: state.ordinaryCurrentizationPreflight,
    ordinaryCurrentization: state.ordinaryCurrentization,
    mechanicalState: state.mechanicalState,
    semanticJudgment: state.semanticJudgment,
    productMain: state.productMain,
    proofHead: state.proofHead,
    foundationVersion: state.foundationVersion,
    currentState: {
      homePath: state.currentState.homePath,
      activeModelRef: state.currentState.activeModelRef,
      activeModelProfileRef: state.currentState.activeModelProfileRef,
      modelArtifactMissingBytes: state.currentState.modelArtifactMissingBytes,
      modelArtifactInvalidSizeBytes: state.currentState.modelArtifactInvalidSizeBytes,
      effectsPerformed: state.currentState.effectsPerformed,
    },
    baseline: state.baseline,
    treatment: state.treatment,
    standing: state.standing,
  };
  process.stdout.write('VEX_EVOLVE_CONTEXT_LIVED_RESULT=' + JSON.stringify(output) + '\n');
} catch (error) {
  state.mechanicalState = 'SAFE_FAILURE';
  state.semanticJudgment = 'NOT_REACHED';
  state.failure = {
    code: error?.code || String(error?.message || error).split(':')[0] || 'UNKNOWN',
    message: String(error?.message || error),
    stack: String(error?.stack || ''),
  };
  writeEvidence();
  process.stderr.write('VEX_EVOLVE_CONTEXT_LIVED_FAILURE=' + JSON.stringify(state.failure) + '\n');
  process.exitCode = 1;
} finally {
  try { fs.rmSync(workRoot, { recursive: true, force: true }); } catch {}
}
