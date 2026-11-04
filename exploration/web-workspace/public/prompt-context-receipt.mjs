// Safe presentation projection of VexLife's canonical prompt-materialization receipt.
//
// The canonical Companion owns prompt selection/materialization. This module does not reconstruct the
// prompt, infer missing context, or expose model reasoning. It only validates and projects the
// content-free receipt already returned by current VexLife.
//
// [VXG RealForever]

const SHA256 = /^[0-9a-f]{64}$/u;
const ROLES = new Set(['system', 'user', 'assistant']);

function integer(value, label) {
  if (!Number.isSafeInteger(value) || value < 0) throw new TypeError(`${label} must be one non-negative integer`);
  return value;
}

function optionalInteger(value, label) {
  return value == null ? null : integer(value, label);
}

function string(value, label) {
  if (typeof value !== 'string' || value.length === 0) throw new TypeError(`${label} must be one non-empty string`);
  return value;
}

function optionalString(value, label) {
  return value == null ? null : string(value, label);
}

function sha(value, label) {
  if (typeof value !== 'string' || !SHA256.test(value)) throw new TypeError(`${label} must be one SHA-256`);
  return value;
}

function optionalSha(value, label) {
  return value == null ? null : sha(value, label);
}

function strings(value, label) {
  if (!Array.isArray(value) || value.some((item) => typeof item !== 'string' || item.length === 0)) {
    throw new TypeError(`${label} must be strings`);
  }
  return Object.freeze([...value]);
}

function entry(value, index) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new TypeError(`orderedMaterializationEntries[${index}] is invalid`);
  if (!ROLES.has(value.role)) throw new TypeError(`orderedMaterializationEntries[${index}].role is invalid`);
  return Object.freeze({
    ordinal: integer(value.ordinal, `orderedMaterializationEntries[${index}].ordinal`),
    role: value.role,
    sourceEventRef: optionalString(value.sourceEventRef, `orderedMaterializationEntries[${index}].sourceEventRef`),
    sourceEventHash: optionalSha(value.sourceEventHash, `orderedMaterializationEntries[${index}].sourceEventHash`),
    sourceContentHash: optionalSha(value.sourceContentHash, `orderedMaterializationEntries[${index}].sourceContentHash`),
    sequence: optionalInteger(value.sequence, `orderedMaterializationEntries[${index}].sequence`),
    completedHeadSha256: optionalSha(value.completedHeadSha256, `orderedMaterializationEntries[${index}].completedHeadSha256`),
    memoryContextRef: optionalString(value.memoryContextRef, `orderedMaterializationEntries[${index}].memoryContextRef`),
    memoryContextFingerprint: optionalSha(value.memoryContextFingerprint, `orderedMaterializationEntries[${index}].memoryContextFingerprint`),
    estimatedTokens: integer(value.estimatedTokens, `orderedMaterializationEntries[${index}].estimatedTokens`)
  });
}

export function projectPromptContextMaterializationReceipt(value) {
  if (value == null) return null;
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new TypeError('prompt-context receipt must be one object');
  if (value.schemaVersion !== 'vexlife.prompt-context-materialization-receipt/v2') {
    throw new TypeError('prompt-context receipt schema is not current');
  }
  if (value.hiddenReasoningIncluded !== false) throw new TypeError('prompt-context receipt may not include hidden reasoning');
  if (value.wholeHistoryEventEnumerationPerformed !== false) throw new TypeError('prompt-context receipt unexpectedly enumerated whole history');
  if (value.currentRequestIncludedExactlyOnce !== true || value.tokenBudgetSatisfied !== true || value.sourceCurrentnessVerified !== true) {
    throw new TypeError('prompt-context receipt does not prove exact current materialization');
  }
  const entries = Array.isArray(value.orderedMaterializationEntries)
    ? Object.freeze(value.orderedMaterializationEntries.map(entry))
    : (() => { throw new TypeError('prompt-context receipt entries are missing'); })();
  const messageCount = integer(value.messageCount, 'messageCount');
  if (entries.length !== messageCount) throw new TypeError('prompt-context receipt entry count does not match messageCount');
  if (entries.some((item, index) => item.ordinal !== index)) throw new TypeError('prompt-context receipt ordinals are not contiguous');

  return Object.freeze({
    schemaVersion: value.schemaVersion,
    receiptRef: string(value.receiptRef, 'receiptRef'),
    semanticFingerprint: sha(value.semanticFingerprint, 'semanticFingerprint'),
    authorityRef: string(value.authorityRef, 'authorityRef'),
    authorityObservedAt: string(value.authorityObservedAt, 'authorityObservedAt'),
    providerBoundaryCurrentnessVerified: value.providerBoundaryCurrentnessVerified === true,
    providerBoundarySourceBindingsVerified: value.providerBoundarySourceBindingsVerified === true,
    contextLeaseRef: string(value.contextLeaseRef, 'contextLeaseRef'),
    contextLeaseFingerprint: sha(value.contextLeaseFingerprint, 'contextLeaseFingerprint'),
    contextLeaseFormedAt: string(value.contextLeaseFormedAt, 'contextLeaseFormedAt'),
    contextLeaseExpiresAt: string(value.contextLeaseExpiresAt, 'contextLeaseExpiresAt'),
    contextLeaseCurrentness: string(value.contextLeaseCurrentness, 'contextLeaseCurrentness'),
    contextLeaseLifecycle: string(value.contextLeaseLifecycle, 'contextLeaseLifecycle'),
    continuityProjectionRef: string(value.continuityProjectionRef, 'continuityProjectionRef'),
    continuityProjectionFingerprint: sha(value.continuityProjectionFingerprint, 'continuityProjectionFingerprint'),
    lineageRef: string(value.lineageRef, 'lineageRef'),
    threadRef: string(value.threadRef, 'threadRef'),
    priorConversationHeadSha256: optionalSha(value.priorConversationHeadSha256, 'priorConversationHeadSha256'),
    turnRef: string(value.turnRef, 'turnRef'),
    currentRequestEventRef: string(value.currentRequestEventRef, 'currentRequestEventRef'),
    currentRequestContentHash: sha(value.currentRequestContentHash, 'currentRequestContentHash'),
    selectedConversationEventRefs: strings(value.selectedConversationEventRefs ?? [], 'selectedConversationEventRefs'),
    memoryContextRefOrNull: optionalString(value.memoryContextRefOrNull, 'memoryContextRefOrNull'),
    memoryContextFingerprintOrNull: optionalSha(value.memoryContextFingerprintOrNull, 'memoryContextFingerprintOrNull'),
    memoryContextIncluded: value.memoryContextIncluded === true,
    memorySourceRefs: strings(value.memorySourceRefs ?? [], 'memorySourceRefs'),
    memoryTokenCap: integer(value.memoryTokenCap, 'memoryTokenCap'),
    memoryEstimatedSystemMessageTokens: integer(value.memoryEstimatedSystemMessageTokens, 'memoryEstimatedSystemMessageTokens'),
    memoryTokenBoundedOutPageCount: integer(value.memoryTokenBoundedOutPageCount, 'memoryTokenBoundedOutPageCount'),
    memoryBoundedOutStatementCount: integer(value.memoryBoundedOutStatementCount, 'memoryBoundedOutStatementCount'),
    memoryHeldOrDeferredStatementCount: integer(value.memoryHeldOrDeferredStatementCount, 'memoryHeldOrDeferredStatementCount'),
    includedSourceRefs: strings(value.includedSourceRefs ?? [], 'includedSourceRefs'),
    orderedMaterializationEntries: entries,
    exactMessagesSha256: sha(value.exactMessagesSha256, 'exactMessagesSha256'),
    messageCount,
    materializedInputTokenEstimate: integer(value.materializedInputTokenEstimate, 'materializedInputTokenEstimate'),
    contextLeaseInputTokenEstimate: integer(value.contextLeaseInputTokenEstimate, 'contextLeaseInputTokenEstimate'),
    reservedOutputTokens: integer(value.reservedOutputTokens, 'reservedOutputTokens'),
    hardTokenLimit: integer(value.hardTokenLimit, 'hardTokenLimit'),
    exactEventFileReadCount: integer(value.exactEventFileReadCount, 'exactEventFileReadCount'),
    headMetadataReadCount: integer(value.headMetadataReadCount, 'headMetadataReadCount'),
    wholeHistoryEventEnumerationPerformed: false,
    currentRequestIncludedExactlyOnce: true,
    priorHumanMessagesUnmodified: value.priorHumanMessagesUnmodified === true,
    priorAssistantMessagesUnmodified: value.priorAssistantMessagesUnmodified === true,
    tokenBudgetSatisfied: true,
    sourceCurrentnessVerified: true,
    crossLineageLeakage: value.crossLineageLeakage === true,
    crossThreadLeakage: value.crossThreadLeakage === true,
    memoryEffectPerformed: value.memoryEffectPerformed === true,
    trainingSelectionPerformed: value.trainingSelectionPerformed === true,
    modelWeightEffectPerformed: value.modelWeightEffectPerformed === true,
    hiddenReasoningIncluded: false
  });
}

export function describePromptContextMaterializationReceipt(receipt) {
  const value = projectPromptContextMaterializationReceipt(receipt);
  if (value === null) return null;
  const input = value.materializedInputTokenEstimate;
  const reserved = value.reservedOutputTokens;
  const hard = value.hardTokenLimit;
  const remaining = Math.max(0, hard - input - reserved);
  const entryLines = value.orderedMaterializationEntries.map((item) => {
    const source = item.memoryContextRef
      ? `Memory ${item.memoryContextRef}`
      : item.sourceEventRef
        ? `${item.sourceEventRef}${item.sequence == null ? '' : ` · sequence ${item.sequence}`}`
        : 'source-managed materialization';
    return `${item.ordinal + 1}. ${item.role.toUpperCase()} · ~${item.estimatedTokens.toLocaleString()} tokens · ${source}`;
  });
  const sourceLines = value.memorySourceRefs.length
    ? ['','Memory source refs:', ...value.memorySourceRefs.map((ref) => `• ${ref}`)]
    : ['','Memory source refs: none included'];
  return Object.freeze({
    facts: Object.freeze([
      Object.freeze(['Receipt', value.receiptRef]),
      Object.freeze(['Turn', value.turnRef]),
      Object.freeze(['Context lease', `${value.contextLeaseCurrentness} / ${value.contextLeaseLifecycle}`]),
      Object.freeze(['Materialized input', `~${input.toLocaleString()} tokens`]),
      Object.freeze(['Input budget', `~${value.contextLeaseInputTokenEstimate.toLocaleString()} tokens`]),
      Object.freeze(['Reserved output', `${reserved.toLocaleString()} tokens`]),
      Object.freeze(['Hard limit', `${hard.toLocaleString()} tokens`]),
      Object.freeze(['Remaining at admission', `~${remaining.toLocaleString()} tokens`]),
      Object.freeze(['Memory included', value.memoryContextIncluded ? `Yes · ~${value.memoryEstimatedSystemMessageTokens.toLocaleString()} tokens` : 'No']),
      Object.freeze(['Prior conversation events', String(value.selectedConversationEventRefs.length)]),
      Object.freeze(['Whole history enumerated', 'No']),
      Object.freeze(['Hidden reasoning included', 'No']),
      Object.freeze(['Source currentness', value.sourceCurrentnessVerified ? 'Verified' : 'Not verified']),
      Object.freeze(['Exact provider message-set hash', value.exactMessagesSha256])
    ]),
    text: [
      'Provider-visible materialization receipt',
      '',
      ...entryLines,
      ...sourceLines,
      '',
      'This is the canonical Companion’s content-free receipt for what it admitted to the provider-bound prompt.',
      'It does not expose hidden model reasoning. It also does not reconstruct raw prompt text that the receipt did not retain.'
    ].join('\n')
  });
}