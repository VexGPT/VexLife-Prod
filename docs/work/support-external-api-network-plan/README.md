# Support External API — no-effect Partner network plan

`[VXG RealForever]`

## Human question

Before the credential secret is resolved and before any HTTP call exists, can VexLife name the exact
remote effect it intends to perform?

## Relationship

```text
source-owned built-in provider topology
+ exact provider-bound credentialRef
+ P1 provider request body
        ↓
VexLifePartnerNetworkPlan
        ↓
no-effect exact request plan
```

The plan contains:

```text
providerRef
credentialRef               # opaque ref only
exact HTTPS origin
exact inference path / URL
provider dialect
credential carriage class   # not secret value
account-scope requirement
POST
public protocol headers
ephemeral request body
request-body SHA-256
redirect policy DENY
```

It contains no Authorization value or API key.

## Why this exists

This separates two materially different questions:

```text
what exact provider request is intended?
!=
may the protected credential be resolved and the network effect performed?
```

The future transport may consume this plan only after the rightful credential/network boundary admits
the effect.

## Redirect boundary

Built-in provider requests are source-bound to their exact origin.

```text
OpenAI     https://api.openai.com/v1/responses
Anthropic  https://api.anthropic.com/v1/messages
Kimi       https://api.moonshot.ai/v1/chat/completions
```

The plan always carries:

```text
redirectPolicy = DENY
```

A redirect is not permission to forward a resolved bearer secret to another origin.

## Public headers

The plan may carry only non-secret protocol headers, including:
- `content-type: application/json`;
- `accept: text/event-stream, application/json`;
- source-owned provider version headers such as Anthropic's current protocol version.

Credential-bearing headers are rejected from the no-effect plan.

## Evidence

The request body remains separately hashable before credential injection:

```text
provider semantic body
-> requestBodySha256

authorization header
-> execution-only secret carriage
-> not part of semantic request-body digest
```

This preserves the existing VexLife pattern where durable evidence can bind the provider request body
without retaining secret material.

## Non-claims

This source does not:
- resolve a credential;
- create an Authorization header;
- open a socket;
- follow a redirect;
- perform retry/backoff;
- establish account/model availability;
- grant model Use authority;
- mutate Conversation/Memory;
- claim live provider qualification.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][A6][NETWORK-PLAN] -->
