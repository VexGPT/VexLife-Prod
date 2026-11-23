# Support External API — native helper process protocol

`[VXG RealForever]`

## Why one process protocol

macOS and Windows need different protected-secret APIs, but Vex should not have two semantic
credential/network stacks.

The JVM/native boundary is therefore source-owned once:

```text
JVM
  providerRef
  credentialRef
  exact non-secret NetworkPlan
        ↓
one-shot native helper protocol
        ↓
macOS helper OR Windows helper
        ↓
OS protected secret + HTTPS
```

## Secret rule

Secret data is never serialized into a JSON command.

### STORE

stdin:

```text
one UTF-8 JSON header line
  operation=STORE
  providerRef
  credentialRef
  secretBytes=N

then exactly N raw secret bytes
then EOF
```

The secret is not argv, environment, JSON, log metadata, or a temp file.

### EXECUTE

stdin:

```text
one UTF-8 JSON header line
  operation=EXECUTE
  providerRef
  credentialRef
  expectedRequestUrl
  dialect
  public protocol headers
  requestBodySha256
  requestBodyBytes=N
  redirectPolicy=DENY

then exactly N raw request-body bytes
then EOF
```

No credential value is transmitted. The native helper resolves it from its OS-protected store.

## Helper endpoint authority

The helper must not trust `expectedRequestUrl` as authority.

It independently resolves `providerRef` through the built-in provider allowlist compiled/generated
with the helper and requires that:
- expected URL matches;
- dialect matches;
- required public protocol headers match;
- redirect policy is DENY.

This is intentional defense at the credential boundary.

## stdout

Helper stdout is NDJSON.

Event types:

```text
STORED
OBSERVED
DELETED
RESPONSE_START
BODY_CHUNK
COMPLETED
TRANSPORT_FAILURE
```

Response body chunks are Base64-framed so arbitrary UTF-8/SSE bytes remain line-safe. They may contain
private provider response content and are ephemeral transport data, not log material.

No event may contain:
- API key/token;
- Authorization value;
- keychain/Credential Manager secret blob;
- redirect-followed=true.

## Platform implementations

### macOS

Intended realization:
- Security.framework Keychain Services for secret storage;
- generic password item keyed by Product service + opaque credentialRef;
- URLSession default/ephemeral task with redirect delegate returning nil to refuse redirects.

Apple documents that the redirect delegate may return nil to refuse the redirect. The helper should
not use the insecure `security -w <password>` argv form.

### Windows

Intended realization:
- Credential Manager `CRED_TYPE_GENERIC`;
- `CredWriteW` / `CredReadW` / `CredDeleteW`;
- verify intended persistence support rather than silently degrading;
- WinHTTP with redirect policy NEVER (or equivalent disable-redirect flag).

Microsoft documents both Credential Manager secure storage and WinHTTP redirect-disabling controls.

## Remaining proof

Still required before production acceptance:
- exact native helper source;
- helper packaging identity;
- macOS build + Keychain lifecycle proof;
- Windows build + Credential Manager lifecycle proof;
- redirect negative controls;
- no-secret argv/environment/temp/log audit;
- exact JVM/native protocol conformance.

No provider API account is needed for those proofs; a local deterministic HTTPS fixture can exercise
the host protocol later.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][NATIVE-HOST-PROTOCOL] -->
