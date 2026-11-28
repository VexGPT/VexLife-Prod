# Support External API — Partner credential custody

`[VXG RealForever]`

## Human outcome

Let a person connect a Partner provider without turning the provider secret into Vex Memory,
Conversation, configuration JSON, logs, source, or a reusable cross-provider bearer.

The Product should retain only enough durable/public meaning to say:

```text
this Partner connection uses credentialRef X
credential standing is MISSING / PRESENT_UNVERIFIED / ACCEPTED / REJECTED
```

The secret value itself belongs only at the protected host custody + exact provider request boundary.

## Current source audit

Current VexLife-Prod main has no accepted credential/API-key store owner.

A bounded predecessor-source audit found two related but semantically different security owners.

### Android Home device possession key

Predecessor VexLife owns an AndroidKeyStore-backed non-exportable P-256 private signing key for device
possession.

Its properties include:

```text
OS-backed private key
privateKeyExportable=false
sign canonical challenge
public descriptor/fingerprint exposed
```

That owner is not reusable for external provider API keys:

```text
NON-EXPORTABLE SIGNING KEY
!= RETRIEVABLE THIRD-PARTY API SECRET
```

### Home Bridge authenticated session handle

Predecessor VexLife also owns short-lived opaque authenticated-session handles.

It:

```text
returns raw handle only to immediate caller
stores only SHA-256 digest + bounded assertion/currentness
does not persist raw handle
process restart destroys verifier map
does not restore bearer credential from disk
```

That owner is also not the external-provider credential store:

```text
SHORT-LIVED PROCESS SESSION
!= LONGER-LIVED USER-ADDED PROVIDER CREDENTIAL
```

These are useful custody principles, not owners to repurpose by name similarity.

## Earned new owner

External Partner support therefore earns one new **host credential-custody** responsibility.

The smallest contract should answer only:

```text
store/replace one secret for exact provider relationship
-> return opaque credentialRef

observe whether credentialRef is present/currently resolvable
-> never reveal secret

execute one admitted exact provider network effect
-> host effect owner resolves the exact credential internally
-> inject at execution boundary
-> never return secret to JVM semantic/runtime layers

delete/retire credential by exact credentialRef/provider relationship
```

It does not own:

```text
provider model selection
provider endpoint/dialect
account/billing standing
model access
Conversation
Memory
prompt context
Capability Atlas
provider retry policy
release authority
```

## Durable state boundary

Allowed durable/public state:

```text
credentialRef
providerRef
credential standing
created/replaced/retired evidence without secret material
exact host custody realization ref when useful
```

Forbidden durable/public state:

```text
API key / OAuth access-token value
Authorization header value
x-api-key value
raw credential bytes
secret in Git / Workgraph / Home semantic JSON
secret in Conversation / Memory / prompt Context
secret in VexLogger detail
secret in invocation receipts
secret in request-body hash input
```

## Exact-provider confinement

One credential relationship is bound to one providerRef.

```text
credentialRef(openai)
-> only provider.openai.api execution

credentialRef(anthropic)
-> only provider.anthropic.api execution

credentialRef(kimi-open-platform-international)
-> only provider.kimi.open-platform.international execution
```

Permanent:

```text
CREDENTIAL PRESENT != AUTH ACCEPTED
AUTH ACCEPTED != ACCOUNT USABLE
ACCOUNT USABLE != MODEL ACCESSIBLE
MODEL ACCESSIBLE != INVOCATION QUALIFIED

ONE PROVIDER CREDENTIAL != GENERIC BEARER FOR ARBITRARY HOSTS
```

Built-in provider origin/dialect remains source-owned by `VexLifePartnerProviderCatalog`.

Redirects must not widen credential scope to another origin.

## Platform realization

The semantic custody contract should remain portable while the host realization may differ:

```text
Partner credential custody meaning
        /                         \
macOS protected realization     Windows protected realization
```

An OS-backed secret facility is preferred over inventing Product-owned reversible encryption files.

The exact macOS/Windows API/adapter is **not selected by this document**. It must be chosen and proven
against the current host platform without leaking the secret through process arguments, logs,
temporary files, test fixtures, crash evidence, or source.

Do not implement "encrypted JSON in Vex Home" merely because it is easy.

## Execution-boundary resolution

The future provider network adapter should receive:

```text
providerRef
credentialRef
admitted built-in provider origin
prepared provider request body
```

Then the protected host effect owner:

```text
verifies providerRef / credentialRef binding
-> resolves secret inside the host-native process
-> forms auth header
-> performs exact HTTPS request
-> returns only status/headers/body + bounded non-secret evidence
-> never returns secret to JVM semantic/runtime layers
```

The request body remains independently hashable because authentication headers are not part of the
semantic provider request body.

## Tests required before production use

At minimum:

```text
store -> opaque ref, no secret projection
execute exact ref/provider -> secret remains inside protected host effect boundary
wrong providerRef -> fail before network
missing/deleted ref -> fail before network
replace -> old secret no longer resolvable
delete -> secret no longer resolvable
logs/receipts/exceptions -> no secret
request-body evidence -> no secret
redirect/cross-origin -> secret never forwarded
restart -> intended durable credential remains available only through protected host owner
test fake -> no production fallback
```

A fake/in-memory store is valid for deterministic tests only and must not silently become production
fallback.

## Current standing

```text
need for credential custody          ESTABLISHED
existing VexLife-Prod owner          NOT FOUND
predecessor reusable exact owner     NO
portable custody contract            SOURCE DESIGN PRESENT
portable custody coordinator         SOURCE PRESENT
protected-host custody port          SOURCE PRESENT
secret-read method in JVM port       NONE
deterministic fake realization       TEST ONLY
macOS protected realization          UNFORMED
Windows protected realization        UNFORMED
real provider credential             NOT REQUIRED FOR CONTRACT IMPLEMENTATION
```

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][CREDENTIAL-CUSTODY] -->

## Portable coordinator source

The design is now compiler-visible in:

```text
src/jvmMain/kotlin/vexlife/partner/VexLifePartnerCredentialCustody.kt
src/jvmTest/kotlin/vexlife/partner/VexLifePartnerCredentialCustodyTest.kt
```

The coordinator owns only:

```text
built-in providerRef binding
opaque credentialRef formation
store / observe / replace / retire lifecycle
non-secret mutation receipts
```

It deliberately exposes no credential-read/resolve method to JVM Product code.

The host port remains narrower:

```text
ProtectedHostStore
  store(exact provider/ref, secret chars)
  contains(exact provider/ref)
  delete(exact provider/ref)

Production request execution is intentionally a separate host-effect protocol.
```

The source contains no HTTP/network dependency, VexLogger call, environment lookup, API-key format
assumption, Home semantic persistence, or production fallback store.

### Rotation failure

Replacement is deliberately fail-closed:

```text
store new protected secret under new opaque ref
-> retire exact old ref

old retirement proves absent
  -> REPLACED receipt

old retirement fails / still present
  -> retire staged new ref again
  -> prove staged ref absent
  -> fail replacement
```

A successful replacement therefore may not leave both old and new refs current. If rollback of the
staged new credential cannot itself be proved, the operation fails with that stronger cleanup error.

### Test-only fake

The deterministic in-memory store exists only inside JVM test source. It is not reachable as a
production default or fallback.

Tests source-pressure:
- opaque ref / no secret in receipt;
- wrong provider coordinate does not become a current stored credential;
- no JVM-facing credential-read method exists;
- replace makes the prior ref unresolvable;
- failed replace removes the staged new ref before failing;
- retire makes the exact ref absent/unresolvable;
- an unformed Custom provider cannot mint custody.

Repository-native JVM execution remains owed.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][CREDENTIAL-CUSTODY][PORTABLE-COORDINATOR] -->
