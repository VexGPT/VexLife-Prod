import assert from 'node:assert/strict';
import http from 'node:http';
import test from 'node:test';

import { createWorkspaceServer } from '../server.mjs';
import { requestTurnStream, getJson, postJson } from '../public/turn-stream.mjs';

const identity = {
  projectRef: 'project.self-development',
  threadRef: 'thread.self-development.open-conversation',
  channelRef: 'channel.self-development.companion'
};

function listen(server) {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      server.off('error', reject);
      resolve(server.address().port);
    });
  });
}

function close(server) {
  return new Promise((resolve) => server.close(() => resolve()));
}

function body(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', (chunk) => chunks.push(chunk));
    req.on('end', () => {
      try { resolve(chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {}); }
      catch (error) { reject(error); }
    });
    req.on('error', reject);
  });
}

function fakeVexLife() {
  const observed = { turns: [] };
  const server = http.createServer(async (req, res) => {
    const url = new URL(req.url, `http://${req.headers.host}`);
    const json = (status, value) => {
      res.writeHead(status, { 'content-type': 'application/json', 'cache-control': 'no-store' });
      res.end(`${JSON.stringify(value)}\n`);
    };
    if (req.method === 'GET' && url.pathname === '/api/v1/companion/availability') {
      return json(200, {
        schemaVersion: 'vexlife.companion-availability/v1',
        truthClass: 'SOURCE_BOUND_COMPANION_AVAILABILITY',
        bindingRef: 'binding.vexlife.test',
        homeRef: 'home.test',
        companionLineageRef: 'companion-lineage.test',
        modelRefOrNull: 'model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27',
        generationRefOrNull: 'generation.test',
        runtimeAdapterRef: 'adapter.vexlife.heretic.mlx',
        runtimeObservationRef: 'observation.runtime.test',
        availabilityState: 'READY',
        recoveryClass: 'NONE_REQUIRED',
        reasonCode: 'EXACT_RUNTIME_READY',
        bindingState: 'BOUND',
        runtimeOwnershipState: 'EXACT_OWNED',
        runtimeState: 'HEALTHY',
        qualificationState: 'CURRENT',
        sourceRefs: ['source.test'],
        effectAuthorityGranted: false,
        rendererAuthorityGranted: false,
        modelIdentityAuthorityGranted: false,
        processAuthorityGranted: false,
        conversationAuthorityGranted: false,
        projectionRef: 'projection.test',
        projectionSha256: 'a'.repeat(64)
      });
    }
    if (req.method === 'GET' && url.pathname === '/api/v1/companion/conversation-currentness') {
      assert.deepEqual(Object.fromEntries(url.searchParams), identity);
      return json(200, {
        schemaVersion: 'vexlife.browser-companion-conversation-currentness/v1',
        state: 'CURRENT',
        truthClass: 'CURRENT_LOCAL_CONVERSATION_IDENTITY',
        ...identity,
        headMessageRefOrNull: 'message.companion.1',
        headSequenceOrNull: 2,
        canonicalConversationHeadSha256: 'b'.repeat(64),
        effectsPerformed: false
      });
    }
    if (req.method === 'GET' && url.pathname === '/api/v1/companion/conversation') {
      assert.deepEqual(Object.fromEntries(url.searchParams), identity);
      return json(200, {
        schemaVersion: 'vexlife.browser-companion-conversation-projection/v1',
        state: 'CURRENT',
        truthClass: 'CURRENT_LOCAL_CONVERSATION_PROJECTION',
        ...identity,
        headMessageRefOrNull: 'message.companion.1',
        headSequenceOrNull: 2,
        canonicalConversationHeadSha256: 'b'.repeat(64),
        messages: [
          { messageRef: 'message.human.1', sequence: 1, speakerRole: 'VICTOR', recipientRoles: ['COMPANION'], content: 'prior human', contentHash: 'h1', createdAt: '2026-11-03T04:00:00.000Z', eventSha256: 'c'.repeat(64) },
          { messageRef: 'message.companion.1', sequence: 2, speakerRole: 'COMPANION', recipientRoles: ['VICTOR'], content: 'prior Vex', contentHash: 'h2', createdAt: '2026-11-03T04:00:01.000Z', eventSha256: 'd'.repeat(64) }
        ],
        truncated: false,
        effectsPerformed: false
      });
    }
    if (req.method === 'POST' && url.pathname === '/api/v1/companion/turn/stream') {
      const request = await body(req);
      observed.turns.push(request);
      assert.equal(request.projectRef, identity.projectRef);
      assert.equal(request.threadRef, identity.threadRef);
      assert.equal(request.channelRef, identity.channelRef);
      assert.equal(request.content, 'hello current Vex');
      assert.match(request.idempotencyKey, /^browser-send\.vexlife-prod\.[0-9a-f]{32}$/u);
      assert.equal(request.screenRef, 'screen.vexlife-workspace');
      assert.equal(request.selectedNodeRef, 'element.vexlife-workspace.companion.conversation.composer');
      assert.equal('contextRef' in request, false);
      res.writeHead(200, { 'content-type': 'application/x-ndjson; charset=utf-8', 'cache-control': 'no-store' });
      const frame = (frameType, sequence, payload) => res.write(`${JSON.stringify({ schemaVersion: 'vexlife.browser-companion-turn-stream-frame/v1', frameType, sequence, payload })}\n`);
      frame('PROGRESS', 1, { truthClass: 'PUBLIC_COMPANION_WORK_PROGRESS', hiddenReasoningIncluded: false, progressState: 'TASK_RECEIVED', label: 'Vex is thinking…' });
      frame('REASONING', 2, { truthClass: 'EPHEMERAL_MODEL_EMITTED_REASONING', reasoningDelta: 'reason ', effectsPerformed: false });
      frame('DELTA', 3, { truthClass: 'EPHEMERAL_LOCAL_MODEL_PROGRESS', contentDelta: 'hello ', effectsPerformed: false });
      frame('DELTA', 4, { truthClass: 'EPHEMERAL_LOCAL_MODEL_PROGRESS', contentDelta: 'back', effectsPerformed: false });
      frame('FINAL', 5, {
        schemaVersion: 'vexlife.browser-companion-turn/v1',
        state: 'TURN_COMPLETED',
        truthClass: 'CURRENT_LOCAL_MODEL',
        content: 'hello back',
        modelNameOrBoundedTestProfileRef: 'model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27',
        turnRef: 'turn.vexlife.test',
        responseMessageRef: 'message.companion.2',
        conversationHeadSha256: 'e'.repeat(64),
        actualHttpCall: true,
        loopbackOnly: true,
        promptContextMaterialization: {
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
          threadRef: identity.threadRef,
          priorConversationHeadSha256: 'b'.repeat(64),
          turnRef: 'turn.vexlife.test',
          currentRequestEventRef: 'event.vexlife.request.test',
          currentRequestContentHash: '4'.repeat(64),
          selectedConversationEventRefs: ['event.vexlife.prior.request', 'event.vexlife.prior.response'],
          memoryContextRefOrNull: 'memory-context.vexlife.prompt.test',
          memoryContextFingerprintOrNull: '5'.repeat(64),
          memoryContextIncluded: true,
          memorySourceRefs: ['page.living-journal.memory.test', 'statement.memory.test'],
          memoryTokenCap: 100000,
          memoryEstimatedSystemMessageTokens: 12000,
          memoryTokenBoundedOutPageCount: 1,
          memoryBoundedOutStatementCount: 2,
          memoryHeldOrDeferredStatementCount: 3,
          includedSourceRefs: ['event.vexlife.prior.request', 'event.vexlife.prior.response', 'event.vexlife.request.test', 'page.living-journal.memory.test'],
          orderedMaterializationEntries: [
            { ordinal: 0, role: 'system', sourceEventRef: null, sourceEventHash: null, sourceContentHash: '6'.repeat(64), sequence: null, completedHeadSha256: null, memoryContextRef: 'memory-context.vexlife.prompt.test', memoryContextFingerprint: '5'.repeat(64), estimatedTokens: 12000 },
            { ordinal: 1, role: 'user', sourceEventRef: 'event.vexlife.prior.request', sourceEventHash: '7'.repeat(64), sourceContentHash: '8'.repeat(64), sequence: 1, completedHeadSha256: '9'.repeat(64), memoryContextRef: null, memoryContextFingerprint: null, estimatedTokens: 20 },
            { ordinal: 2, role: 'assistant', sourceEventRef: 'event.vexlife.prior.response', sourceEventHash: 'a'.repeat(64), sourceContentHash: 'b'.repeat(64), sequence: 2, completedHeadSha256: '9'.repeat(64), memoryContextRef: null, memoryContextFingerprint: null, estimatedTokens: 24 },
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
          hiddenReasoningIncluded: false
        }
      });
      return res.end();
    }
    json(404, { message: 'not found' });
  });
  return { server, observed };
}

test('threshold shell can project and stream the current canonical VexLife Companion without raw Heretic ownership', async () => {
  const upstream = fakeVexLife();
  const upstreamPort = await listen(upstream.server);
  const workspace = createWorkspaceServer({
    port: 0,
    framePort: 0,
    dataDir: `/tmp/vexlife-prod-bridge-test-${process.pid}-${Date.now()}`,
    companionOrigin: `http://127.0.0.1:${upstreamPort}`,
    log: () => {}
  });
  const { origin } = await workspace.listen();
  try {
    const session = await getJson('/api/session', { origin });
    assert.equal(session.canonicalConversation, true);
    assert.equal(session.turnCancellation, false);
    assert.equal(session.browserContextBridge, false);
    assert.equal(session.lastSequence, 0);

    const runtime = await postJson('/api/companion/probe', {}, { origin });
    assert.equal(runtime.type, 'RuntimeReady');
    assert.equal(runtime.modelRef, 'model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27');
    assert.equal(runtime.endpointRef, 'adapter.vexlife.heretic.mlx');

    const conversation = await getJson('/api/conversation', { origin });
    assert.equal(conversation.canonicalConversation, true);
    assert.equal(conversation.lastSequence, 1);
    assert.equal(conversation.turns.length, 1);
    assert.equal(conversation.turns[0].userText, 'prior human');
    assert.equal(conversation.turns[0].response.text, 'prior Vex');

    const events = [];
    const outcome = await requestTurnStream({
      origin,
      effect: { turn: 2, text: 'hello current Vex', contextRef: null },
      onEvent: (event, data) => events.push({ event, data })
    });
    assert.equal(outcome.accepted, true);
    assert.deepEqual(events.filter((e) => e.event === 'delta').map((e) => e.data.text), ['hello ', 'back']);
    assert.deepEqual(events.filter((e) => e.event === 'reasoning').map((e) => e.data.text), ['reason ']);
    assert.equal(events.some((e) => e.event === 'observation' && e.data.type === 'ResponseStarted'), true);
    assert.equal(events.some((e) => e.event === 'observation' && e.data.type === 'ResponseCompleted'), true);
    const record = events.find((e) => e.event === 'record')?.data;
    assert.equal(record.response.text, 'hello back');
    assert.equal(record.canonical.actualHttpCall, true);
    assert.equal(record.canonical.loopbackOnly, true);
    assert.equal(record.prompt.materialization.schemaVersion, 'vexlife.prompt-context-materialization-receipt/v2');
    assert.equal(record.prompt.materialization.materializedInputTokenEstimate, 12062);
    assert.equal(record.prompt.materialization.memoryContextIncluded, true);
    assert.equal(record.prompt.materialization.hiddenReasoningIncluded, false);
    assert.equal(record.prompt.materialization.orderedMaterializationEntries.length, 4);
    assert.equal(upstream.observed.turns.length, 1);
  } finally {
    await workspace.close();
    await close(upstream.server);
  }
});

test('browser page text is held instead of being silently injected into the canonical Conversation', async () => {
  const upstream = fakeVexLife();
  const upstreamPort = await listen(upstream.server);
  const workspace = createWorkspaceServer({
    port: 0,
    framePort: 0,
    dataDir: `/tmp/vexlife-prod-bridge-context-test-${process.pid}-${Date.now()}`,
    companionOrigin: `http://127.0.0.1:${upstreamPort}`,
    log: () => {}
  });
  const { origin } = await workspace.listen();
  try {
    const response = await fetch(`${origin}/api/conversation/turns`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ turn: 2, text: 'use this page', contextRef: 'context-0123456789abcdef' })
    });
    assert.equal(response.status, 409);
    const value = await response.json();
    assert.equal(value.error, 'BROWSER_CONTEXT_BRIDGE_HELD');
    assert.equal(upstream.observed.turns.length, 0);
  } finally {
    await workspace.close();
    await close(upstream.server);
  }
});

test('the new shell boots even when the canonical Companion service is unavailable', async () => {
  const workspace = createWorkspaceServer({
    port: 0,
    framePort: 0,
    dataDir: `/tmp/vexlife-prod-bridge-unavailable-${process.pid}-${Date.now()}`,
    companionOrigin: 'http://127.0.0.1:9',
    log: () => {}
  });
  const { origin } = await workspace.listen();
  try {
    const session = await getJson('/api/session', { origin });
    assert.equal(session.canonicalConversation, true);
    assert.equal(session.turnCancellation, false);
    assert.equal(session.browserContextBridge, false);
    assert.equal(session.lastSequence, 0);

    const htmlResponse = await fetch(`${origin}/`);
    assert.equal(htmlResponse.status, 200);
    assert.match(await htmlResponse.text(), /Converse with/u);

    const probeResponse = await fetch(`${origin}/api/companion/probe`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: '{}'
    });
    assert.equal(probeResponse.status, 500);
  } finally {
    await workspace.close();
  }
});