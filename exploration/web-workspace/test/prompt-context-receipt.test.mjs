import test from 'node:test';
import assert from 'node:assert/strict';
import {
  describePromptContextMaterializationReceipt,
  projectPromptContextMaterializationReceipt
} from '../public/prompt-context-receipt.mjs';

function receipt(overrides = {}) {
  return {
    schemaVersion: 'vexlife.prompt-context-materialization-receipt/v2',
    receiptRef: 'receipt.vexlife.prompt-context-materialization.' + '1'.repeat(32),
    semanticFingerprint: '1'.repeat(64),
    authorityRef: 'authority.vexlife.browser-prompt-context-runtime',
    authorityObservedAt: '2026-11-03T04:00:03.000Z',
    providerBoundaryCurrentnessVerified: true,
    providerBoundarySourceBindingsVerified: true,
    contextLeaseRef: 'lease.vexlife.browser-prompt-context.test',
    contextLeaseFingerprint: '2'.repeat(64),
    contextLeaseFormedAt: '2026-11-03T04:00:02.000Z',
    contextLeaseExpiresAt: '2026-11-03T04:01:02.000Z',
    contextLeaseCurrentness: 'CURRENT',
    contextLeaseLifecycle: 'ACTIVE',
    continuityProjectionRef: 'projection.vexlife.continuity-stream-adapter.test',
    continuityProjectionFingerprint: '3'.repeat(64),
    lineageRef: 'companion-lineage.test',
    threadRef: 'thread.self-development.open-conversation',
    priorConversationHeadSha256: 'b'.repeat(64),
    turnRef: 'turn.vexlife.test',
    currentRequestEventRef: 'event.vexlife.request.test',
    currentRequestContentHash: '4'.repeat(64),
    selectedConversationEventRefs: ['event.prior.request', 'event.prior.response'],
    memoryContextRefOrNull: 'memory-context.vexlife.prompt.test',
    memoryContextFingerprintOrNull: '5'.repeat(64),
    memoryContextIncluded: true,
    memorySourceRefs: ['page.living-journal.memory.test', 'statement.memory.test'],
    memoryTokenCap: 100000,
    memoryEstimatedSystemMessageTokens: 12000,
    memoryTokenBoundedOutPageCount: 1,
    memoryBoundedOutStatementCount: 2,
    memoryHeldOrDeferredStatementCount: 3,
    includedSourceRefs: ['event.prior.request', 'event.prior.response', 'event.vexlife.request.test'],
    orderedMaterializationEntries: [
      { ordinal: 0, role: 'system', sourceEventRef: null, sourceEventHash: null, sourceContentHash: '6'.repeat(64), sequence: null, completedHeadSha256: null, memoryContextRef: 'memory-context.vexlife.prompt.test', memoryContextFingerprint: '5'.repeat(64), estimatedTokens: 12000 },
      { ordinal: 1, role: 'user', sourceEventRef: 'event.prior.request', sourceEventHash: '7'.repeat(64), sourceContentHash: '8'.repeat(64), sequence: 1, completedHeadSha256: '9'.repeat(64), memoryContextRef: null, memoryContextFingerprint: null, estimatedTokens: 20 },
      { ordinal: 2, role: 'assistant', sourceEventRef: 'event.prior.response', sourceEventHash: 'a'.repeat(64), sourceContentHash: 'b'.repeat(64), sequence: 2, completedHeadSha256: '9'.repeat(64), memoryContextRef: null, memoryContextFingerprint: null, estimatedTokens: 24 },
      { ordinal: 3, role: 'user', sourceEventRef: 'event.vexlife.request.test', sourceEventHash: 'c'.repeat(64), sourceContentHash: '4'.repeat(64), sequence: 3, completedHeadSha256: null, memoryContextRef: null, memoryContextFingerprint: null, estimatedTokens: 18 }
    ],
    exactMessagesSha256: 'd'.repeat(64),
    messageCount: 4,
    materializedInputTokenEstimate: 12062,
    contextLeaseInputTokenEstimate: 192000,
    reservedOutputTokens: 16000,
    hardTokenLimit: 208000,
    exactEventFileReadCount: 3,
    headMetadataReadCount: 2,
    wholeHistoryEventEnumerationPerformed: false,
    currentRequestIncludedExactlyOnce: true,
    priorHumanMessagesUnmodified: true,
    priorAssistantMessagesUnmodified: true,
    tokenBudgetSatisfied: true,
    sourceCurrentnessVerified: true,
    crossLineageLeakage: false,
    crossThreadLeakage: false,
    memoryEffectPerformed: false,
    trainingSelectionPerformed: false,
    modelWeightEffectPerformed: false,
    hiddenReasoningIncluded: false,
    ...overrides
  };
}

test('projects the canonical content-free prompt receipt without hidden reasoning', () => {
  const value = projectPromptContextMaterializationReceipt(receipt());
  assert.equal(value.materializedInputTokenEstimate, 12062);
  assert.equal(value.memoryEstimatedSystemMessageTokens, 12000);
  assert.equal(value.hiddenReasoningIncluded, false);
  assert.equal(value.wholeHistoryEventEnumerationPerformed, false);
  assert.equal(value.orderedMaterializationEntries.length, 4);
  assert.equal(Object.hasOwn(value.orderedMaterializationEntries[0], 'content'), false);
});

test('describes what Vex received using receipt truth rather than reconstructed prompt text', () => {
  const value = describePromptContextMaterializationReceipt(receipt());
  assert.equal(value.facts.some(([key, val]) => key === 'Hidden reasoning included' && val === 'No'), true);
  assert.match(value.text, /SYSTEM · ~12,000 tokens/u);
  assert.match(value.text, /Memory source refs:/u);
  assert.doesNotMatch(value.text, /prior human|prior Vex/u);
});

test('rejects receipts that claim hidden reasoning or whole-history enumeration', () => {
  assert.throws(() => projectPromptContextMaterializationReceipt(receipt({ hiddenReasoningIncluded: true })), /hidden reasoning/u);
  assert.throws(() => projectPromptContextMaterializationReceipt(receipt({ wholeHistoryEventEnumerationPerformed: true })), /whole history/u);
});