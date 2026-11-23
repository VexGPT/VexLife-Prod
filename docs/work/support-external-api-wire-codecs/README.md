# Support External API — P1 pure provider wire codecs

`[VXG RealForever]`

## Purpose

Consume the P0 official-contract fixture corpus without taking network, credential, model-selection,
Conversation, Memory or capability-effect ownership.

This branch is stacked on:

```text
P0 branch
VXG-112326-support-external-api-contract-fixtures-a001

P0 PR
#553
```

and remains disjoint from the current Warm Boot #518 shared Connection/Turn mutation surface.

## Source boundary

New P1 paths:

```text
src/jvmMain/kotlin/vexlife/partner/ExternalProviderWire.kt
src/jvmMain/kotlin/vexlife/partner/OpenAiResponsesWire.kt
src/jvmMain/kotlin/vexlife/partner/AnthropicMessagesWire.kt
src/jvmMain/kotlin/vexlife/partner/KimiChatWire.kt
src/jvmTest/kotlin/vexlife/partner/ExternalProviderWireCodecTest.kt
```

The codecs intentionally have no:

```text
HTTP client
endpoint selection
API key / credential value
credential store
Home mutation
Conversation store
Memory read/write
Capability Bridge effect
model selection
provider fallback
billing mutation
```

They operate only on already-prepared provider-facing meaning and documented wire bytes/events.

## Current provider representations

### OpenAI Responses

Request projection:
- leading Vex system messages -> top-level `instructions`;
- remaining provider-ready turns -> `input`;
- Product output bound -> `max_output_tokens`;
- Vex read-tool schema -> Responses function tool;
- strict function schema;
- parallel tool calls disabled for the first Vex Bridge relationship;
- streaming enabled.

Response projection:
- `response.output_text.delta` -> text delta;
- function-call output / argument events -> one normalized provider tool call;
- provider `call_id` preserved;
- `response.completed` -> terminal completion;
- error/failed events -> canonical provider-failure observation.

### Anthropic Messages

Request projection:
- leading Vex system messages -> top-level `system`;
- remaining turns -> `messages`;
- Product output bound -> `max_tokens`;
- Vex read-tool schema -> Anthropic `input_schema`;
- `tool_choice=auto` with parallel tool use disabled for the first Bridge relationship;
- streaming enabled.

Response projection:
- text deltas -> text;
- `input_json_delta.partial_json` assembled until content-block stop;
- `tool_use.id` preserved;
- terminal `message_stop` required for ordinary completion;
- SSE `error` after stream start remains failure, not completion.

### Kimi Open Platform

Request projection:
- provider-ready messages remain OpenAI-compatible Chat messages;
- Product output bound -> `max_completion_tokens`;
- Vex read-tool schema -> OpenAI-compatible function tool;
- streaming enabled.

Response projection:
- text deltas -> text;
- fragmented `tool_calls[].function.arguments` assembled by tool-call index;
- provider tool-call `id` preserved;
- terminal `finish_reason` required;
- 429 classification uses provider `error.type`, not status alone.

## Provider call identity pressure

P1 preserves provider-native call identity as bounded carriage:

```text
OpenAI Responses call_id
Anthropic tool_use.id
Kimi tool_call.id
        ↓
ExternalProviderWire.ToolCall.providerCallRef
```

It does **not** yet integrate the provider result continuation into Direct Companion.

Current local native-tool carriage can reconstruct a deterministic synthetic call ID from canonical
Vex control text. A real external provider may require the original provider-issued call identity on
the later result message.

Therefore:

```text
P1 may parse + retain providerCallRef
A4 must carry it across the canonical Bridge result
future provider continuation must reuse the same providerCallRef
```

The provider ID remains representation/evidence, not Vex semantic authority.

## Failure normalization

The first pure classifier retains provider-specific evidence and projects one bounded Vex meaning.

Examples:

```text
401 auth
-> CREDENTIAL_REJECTED

billing/quota-specific provider evidence
-> ACCOUNT_OR_BILLING_HELD

429 temporary rate evidence
-> RATE_LIMITED

provider-specific overload
-> PROVIDER_OVERLOADED

5xx service failure
-> PROVIDER_UNAVAILABLE
```

Kimi's three current 429 meanings remain distinct.

Retry disposition is a normalized observation only. It does not authorize automatic retry and does
not override future Direct Companion turn/idempotency policy.

## Current proof boundary

Source review can establish:
- codecs accept no credential or network dependency;
- provider call identity is retained;
- request serializers represent the documented provider-specific shapes;
- fixture-driven tests exist for stream/tool/error normalization.

Still owed:
- exact JVM/Gradle execution;
- P1 adapter conformance PASS;
- integration into the accepted A0 inference seam;
- credential/endpoint security proof;
- canonical Bridge result continuation using providerCallRef;
- live provider qualification.

## Merge / rebase route

```text
#553 P0 accepted
-> retarget/rebase this P1 candidate onto accepted P0/main

#518 Warm Boot accepted before P1 integration
-> no source collision expected
-> re-ground accepted Connection/Turn semantics before A0 integration

#518 changes provider-stage representation
-> pure codecs remain provider-contract consumers
-> integration adapter changes
-> do not rewrite provider contract meaning to match historical local classes
```

P1 is deliberately provisional contract-first implementation. It does not claim production provider
support merely because its pure codec tests later pass.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][P1][WIRE-CODECS] -->
