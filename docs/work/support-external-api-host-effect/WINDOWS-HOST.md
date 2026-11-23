# Support External API — Windows protected Partner host

`[VXG RealForever]`

## Purpose

Implement the source-owned native-helper protocol on Windows without moving the provider secret into
the JVM, argv, environment variables, temporary files or Product logs.

This is a platform candidate, not a release claim.

## Host realization

```text
PowerShell 5.1 one-shot process
        ↓
embedded C# host
  Credential Manager P/Invoke
  HttpClient request
        ↓
OS credential store + HTTPS
```

The repository already uses Windows PowerShell as a host substrate. This helper reuses that substrate
but does not use PowerShell command-line arguments for secrets.

## Credential Manager

The embedded host uses:
- `CRED_TYPE_GENERIC`;
- `CredWriteW`;
- `CredReadW`;
- `CredDeleteW`;
- `CredFree`;
- `CRED_PERSIST_LOCAL_MACHINE`.

Target identity:

```text
VexGPT_VexLife_Partner::<providerRef>::<credentialRef>
```

The stored generic credential also records providerRef as the username field and validates it on
read/observe.

The Product-wide secret size bound is 2,560 UTF-8 bytes, matching
`CRED_MAX_CREDENTIAL_BLOB_SIZE`.

No session-persistence fallback is implemented. If the OS/policy refuses local-machine persistence,
STORE fails.

## Process membrane

argv contains only the PowerShell executable / helper script path when launched by the later Windows
packaging/runtime integration.

stdin follows `VexLifePartnerNativeHostProtocol`:
- STORE header + raw secret bytes;
- OBSERVE/DELETE header only;
- EXECUTE header + raw provider request body.

stdout contains protocol NDJSON only.

The helper does not write secrets or provider response bodies to files.

## Provider authority

The helper independently owns an allowlist for the three built-in provider coordinates and verifies:
- providerRef;
- exact request URL;
- dialect;
- allowed public headers;
- Anthropic API version;
- optional Anthropic workspace ID shape.

It does not trust an arbitrary URL from JVM input.

## HTTPS

The embedded C# client sets:
- `AllowAutoRedirect=false`;
- `UseCookies=false`;
- `UseDefaultCredentials=false`;
- certificate revocation checking;
- gzip/deflate decompression.

The provider secret is read inside the helper process, converted only there for the Authorization
header, removed from the request object after response headers arrive, and never returned through the
protocol.

3xx is converted to `REDIRECT_REJECTED`.

Ordinary provider 4xx/5xx remains `RESPONSE_START + BODY_CHUNK + COMPLETED`, so P1/A5 retain
provider-specific error meaning.

## Current proof boundary

Source formed here:
- credential lifecycle implementation;
- exact provider allowlist;
- no-secret stdin protocol;
- HTTP redirect refusal;
- streaming response chunk protocol;
- bounded 32 MiB response transport.

Still owed:
- Windows-host syntax/compile execution;
- real Credential Manager lifecycle proof on Windows;
- local deterministic HTTPS redirect/stream/error proof;
- packaging identity/path;
- JVM-to-helper Windows lived proof.

No paid provider account is needed for those host proofs.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][WINDOWS-HOST] -->

## Source-controlled deterministic Windows proof

Proof runner:

tools/windows-partner-host/PROVE-CREDENTIAL-HOST.ps1

It requires no provider account and performs no provider network request.

Sequence:

STORE random synthetic secret -> OBSERVE present -> DELETE -> OBSERVE absent -> EXECUTE same deleted credentialRef -> require CREDENTIAL_NOT_PRESENT before network.

Cleanup always attempts idempotent DELETE for the exact random proof ref in finally, even if an earlier proof assertion fails. The synthetic secret byte array is cleared before process exit.

The only proof artifact is non-secret JSON under build/windows-partner-host-proof/windows-partner-host-proof.json.

Execution is still owed on a real Windows host.
