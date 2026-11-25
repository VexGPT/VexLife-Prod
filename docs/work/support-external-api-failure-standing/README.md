# Support External API — provider failure to connection standing

`[VXG RealForever]`

## Human question

When an external provider invocation fails, which current relationship actually changed?

The Product must not reduce materially different provider evidence to one generic API error.

## Composition

This workbench composes two already-separated source candidates:

```text
P1 #554
  provider-specific wire/error evidence
        +
A2 #558
  orthogonal Partner connection dimensions
        ↓
A5 failure/currentness projection
```

Formation checkpoint:

```text
merge commit 389d38908454b66b7ffc8042514d1f32b1680a58
parent P1   6cef2b6cb733175ff325644f3c03f8386c1f71ef
parent A2   ec10602f1714f75d343c5dedb850a3d305ae9716
```

No Warm Boot #518 path is changed.

## Mapping

```text
CREDENTIAL_REJECTED
  -> credential REJECTED
  -> dependent account/service/model observations UNKNOWN
  -> last invocation failed

ACCOUNT_OR_BILLING_HELD
  -> account BILLING_HELD

PERMISSION_HELD
  -> account PERMISSION_HELD

MODEL_NOT_ACCESSIBLE
  -> model access NO_ACCESS

RATE_LIMITED
  -> service RATE_LIMITED

PROVIDER_OVERLOADED
  -> service OVERLOADED

PROVIDER_UNAVAILABLE
  -> service UNAVAILABLE

REQUEST_INVALID / CONTEXT_LIMIT_EXCEEDED
  -> connection facts remain current
  -> invocation LAST_INVOCATION_FAILED

STREAM_INTERRUPTED / RESPONSE_INVALID / UNKNOWN_PROVIDER_FAILURE
  -> service UNKNOWN
  -> invocation LAST_INVOCATION_FAILED
```

The last group is deliberately conservative: ambiguous provider/transport evidence does not prove
that the service is unavailable, but it also cannot leave stale READY as current truth.

## Ownership

This projection performs no:

```text
retry
network request
credential store mutation
billing action
model switch
automatic fallback
Conversation mutation
Memory mutation
release effect
```

Retryability carried by the wire failure remains evidence for later turn policy. It is not action
authority.

Provider identity must match the connection receiving the failure. An OpenAI failure cannot update
an Anthropic or Kimi connection.

## Proof boundary

Focused tests source-pressure:
- invalid credential invalidates credential-dependent stale currentness;
- billing, permission and model access remain distinct;
- rate, overload and outage remain distinct;
- request/context failures do not invent connection loss;
- ambiguous stream/response failures stop claiming READY;
- cross-provider evidence is rejected;
- a provider failure cannot be applied to a connection that has no credential;
- provider/model identity survives projection.

Repository-native execution remains owed.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][A5][FAILURE-STANDING] -->
