# Support External API — provider contract fixtures

`[VXG RealForever]`

## Purpose

Build the zero-cost proof body for external Partner providers **without** pretending synthetic fixtures
prove live provider/account/model qualification.

This branch is intentionally parallel to the current Warm Boot / Direct Companion writer.

Formation base:

```text
VexLife-Prod main
0566b1eed876a28c2fcc6586743ae1f4af7eaecd

branch
VXG-112326-support-external-api-contract-fixtures-a001
```

At branch formation, Warm Boot Stage B PR #518 owns overlapping current-main source in:

```text
DirectCompanionConnection.kt
DirectCompanionConnectionTest.kt
DirectCompanionTurn.kt
prompt-preparation/lifetime consumers
```

This fixture lane therefore authors **new docs/test-resource/test-harness paths only**. It does not edit
those shared classes.

## Proof ladder

External-provider support has four different proof classes. Do not collapse them.

```text
P0  DOCUMENTED CONTRACT FIDELITY
    official provider docs
    -> source-controlled synthetic JSON/SSE/error fixtures
    -> fixture-shape tests

P1  ADAPTER CONFORMANCE
    those exact fixtures
    -> VexLife provider adapter
    -> normalized content / tool request / usage / failure
    -> deterministic tests

P2  VEX RELATIONSHIP CONFORMANCE
    normalized adapter result
    -> Direct Companion turn / Capability Bridge / Settings / evidence
    -> Conversation/Memory/context ownership preserved
    -> deterministic tests

P3  LIVE QUALIFICATION
    real credential/account/model/current provider backend
    -> one bounded live call
    -> exact provider/model qualification receipt
```

P0-P2 require no paid API access.

P3 remains separately held until a legitimate usable credential/account/credit exists.

## Fixture authority

Every fixture is tagged by provenance.

```text
OFFICIAL_DOC_CONFORMANT_SYNTHETIC
  manually constructed to satisfy the provider's current official public contract.
  This is the only synthetic class allowed to establish fixture-contract expectations.

LIVE_CAPTURE_REDACTED
  future exact provider observation with secrets/personal content removed.
  May strengthen/repair a synthetic fixture after live qualification.

SECONDARY_IMPLEMENTATION_CROSSCHECK
  open-source/third-party implementation evidence such as n8n.
  Useful to discover integration edge cases.
  Never overrides the official provider contract.
```

Permanent:

```text
N8N BEHAVIOR != PROVIDER SPEC
SDK DEFAULT != VEXLIFE POLICY
SYNTHETIC PASS != LIVE PROVIDER PASS
HTTP 200 != TERMINAL STREAM SUCCESS
MODEL LIST != VEXLIFE SUPPORT AUTHORITY
```

## Current official contract anchors — re-ground before implementation/live proof

OpenAI:
- https://developers.openai.com/api/docs/guides/streaming-responses
- https://developers.openai.com/api/docs/guides/function-calling
- https://developers.openai.com/api/docs/guides/error-codes

Current documented relationships used by the fixtures include:
- Responses streaming events such as `response.output_text.delta`, `response.completed`, and `error`;
- function-call output items carry `type=function_call`, a `call_id`, name and JSON-encoded arguments;
- function results return with `type=function_call_output` and the same `call_id`;
- status alone is insufficient for 429 classification because rate limits, exhausted credits and spend/usage limits have distinct codes.

Anthropic:
- https://platform.claude.com/docs/en/api/messages
- https://platform.claude.com/docs/en/build-with-claude/streaming
- https://platform.claude.com/docs/en/api/errors

Current documented relationships used by the fixtures include:
- Messages content is block-shaped;
- streaming uses named SSE events plus matching JSON `type`;
- tool input may arrive as `input_json_delta.partial_json` and is complete only after the content block closes;
- a terminal `message_stop` completes an ordinary stream;
- an `error` event may occur after HTTP 200;
- 401 authentication, 402 billing, 403 permission, 429 rate/spend-cap and 529 overload remain distinct provider observations.

Kimi Open Platform:
- https://www.kimi.ai/help/kimi-api/api-overview
- https://www.kimi.ai/help/kimi-api/api-error-codes
- https://www.kimi.ai/help/kimi-api/api-troubleshooting

Current documented relationships used by the fixtures include:
- Chat Completions/OpenAI-compatible request/response family;
- SSE streaming;
- `tool_calls` with tool-result correlation through `tool_call_id`;
- `finish_reason=tool_calls` for a tool stage;
- 429 must be classified by `error.type`, including rate limit, insufficient quota and engine overload;
- the OpenAI SDK may retry transient failures automatically, so SDK retry defaults are evidence, not VexLife retry policy.

## Secondary implementation cross-check

n8n current public source was inspected only as secondary implementation evidence.

Useful observed examples include:
- separate OpenAI/Anthropic credential mapping and base-URL handling;
- Anthropic model discovery carrying `x-api-key` plus an Anthropic API version header;
- OpenAI Responses SSE fixtures using `response.output_text.delta`;
- Anthropic tool-use code requiring matching `tool_use` / `tool_result` relationships;
- mock/evaluation infrastructure that explicitly models provider-specific response quirks.

This supports the value of a fixture-driven integration harness. It does not define VexLife contracts.

## Fixture design

The initial corpus covers:

```text
OpenAI Responses
  text streaming
  function call identity
  authentication / rate / billing / server failure distinctions

Anthropic Messages
  text response
  streaming tool_use partial JSON
  stream error after initial success
  authentication / billing / permission / rate / overload distinctions

Kimi Chat Completions
  text response
  streaming tool_calls assembly
  authentication / permission / model / rate / quota / overload / server distinctions
```

No fixture contains a real API key, account identifier, Conversation, Memory, Home path, or personal
content.

## Merge / continuation rule

This branch is deliberately disjoint from #518.

If #518 changes the Connection/Turn contracts:

```text
do not rebase fixture meaning around old implementation names
-> retain the fixture corpus
-> re-ground accepted current Product
-> adapter lane consumes fixtures through the new accepted seam
-> resolve compile/integration changes there
```

If #518 closes unmerged:

```text
re-ground current main
-> keep this fixture branch as contract proof
-> form A0 adapter/source candidate from current accepted source
```

If #518 merges:

```text
re-ground merged Connection semantics
-> preserve Warm Boot behavior
-> form provider-neutral local-parity seam
-> attach these fixtures to Partner adapters
```

The fixture branch does not need to win a merge race against #518 because it owns no shared source path.

## Next proof edge

After P0 fixture-shape proof is green:

```text
add provider adapter implementation on accepted A0 seam
-> consume exactly these fixtures
-> prove canonical normalization
-> prove failure classification
-> prove credential material is absent from durable evidence
-> prove local realization remains unchanged
```

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][CONTRACT-FIXTURES] -->
