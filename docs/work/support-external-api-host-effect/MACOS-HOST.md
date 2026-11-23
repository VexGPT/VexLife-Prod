# Support External API — macOS protected Partner host

`[VXG RealForever]`

## Purpose

Implement the source-owned Partner native-helper protocol on macOS without exposing the provider
secret to JVM runtime/semantic code.

This is a platform source candidate. It is not yet a lived Keychain/HTTPS proof.

## Keychain realization

The Swift helper uses Security.framework generic-password items:

```text
kSecClassGenericPassword
kSecAttrService = com.vexlife.prod.partner.<providerRef>
kSecAttrAccount = <credentialRef>
kSecValueData   = secret bytes
```

Lifecycle:
- `SecItemAdd` for STORE;
- `SecItemCopyMatching` for OBSERVE/read;
- `SecItemDelete` for DELETE.

The helper does not use the `security` CLI or secret-bearing process arguments.

Apple documents service/account as the composite identity of generic-password items and Keychain
Services as encrypted storage for password data.

## HTTPS realization

The helper uses an ephemeral `URLSession` with a serial data delegate.

The redirect delegate calls the completion handler with `nil`, refusing the redirect.

Ordinary non-redirect 4xx/5xx stays provider response data:
- RESPONSE_START;
- BODY_CHUNK;
- COMPLETED.

Network/TLS/timeout/redirect failures stay TRANSPORT_FAILURE.

Response body transport is bounded to 32 MiB.

## Secret lifetime

STORE receives raw secret bytes from protocol stdin.

EXECUTE retrieves Keychain data only inside the Swift helper, forms the Authorization bearer header
inside that one-shot process, and never returns the key through stdout/protocol.

Mutable Keychain Data is reset before exit. The temporary Swift String used by URLRequest cannot be
reliably zeroized, so it remains confined to this one-shot helper process and is cleared from the
local request variable as soon as the URLSession task is formed.

## Current macOS risk/proof question

The current VexLife Mac app uses ad-hoc signing with no Developer ID/notarization.

Keychain access continuity across rebuilt/ad-hoc-signed helper binaries must therefore be **lived
proven** rather than assumed. This candidate makes no claim that a stored item will survive a Product
upgrade without prompt/ACL consequences.

That is a host proof issue, not a reason to return the secret to JVM code.

## Still owed

- swiftc compile on the exact Mac build host;
- Keychain STORE/OBSERVE/DELETE proof;
- helper identity/access continuity across the packaged app lifecycle;
- deterministic local HTTPS stream/error/redirect proof;
- packaging into the source-bound private service image;
- JVM -> helper lived protocol proof.

No paid provider API is needed for these host proofs.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][MACOS-HOST] -->

## Source-controlled deterministic macOS proof

Proof runner:

tools/macos-partner-host/PROVE-CREDENTIAL-HOST.command

It requires no provider account and performs no provider network request.

The runner first compiles the exact Swift source with xcrun swiftc, then runs:

STORE random synthetic secret -> OBSERVE present -> DELETE -> OBSERVE absent -> EXECUTE same deleted credentialRef -> require CREDENTIAL_NOT_PRESENT before network.

Cleanup always attempts idempotent DELETE for the exact random proof ref in the shell EXIT trap.

The non-secret proof receipt is written under build/macos-partner-host-proof/macos-partner-host-proof.json.

Execution remains owed on a Darwin arm64 host.
