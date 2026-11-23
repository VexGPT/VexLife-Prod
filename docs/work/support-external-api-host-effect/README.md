# Support External API — protected host effect boundary

`[VXG RealForever]`

## Why the boundary changed

The portable credential-custody source originally allowed a JVM callback to receive the resolved
secret. That is no longer the production design.

Current Product has no native-FFI layer. Returning an API key to JVM HTTP code would eventually force
authentication material through ordinary JVM string/object lifetime that cannot be reliably zeroized.

The corrected boundary is:

```text
JVM
  exact non-secret NetworkPlan
        ↓
platform protected host effect
  resolve credential from OS store
  form authentication header
  perform exact HTTPS request
  refuse redirects
  discard secret
        ↓
JVM
  status
  bounded non-secret response headers
  ephemeral response body
  non-secret effect receipt
```

The API key does not return to JVM runtime/semantic code after it has been stored.

## Platform direction

### macOS

Current Apple guidance uses Keychain Services / `SecItem*` APIs for application passwords/secrets.
The `security add-generic-password -w <value>` CLI form is not admitted because Apple itself warns
that supplying the password as an argument is insecure.

The eventual macOS helper should therefore call Security.framework directly.

### Windows

Current Microsoft guidance prefers Windows Credential Manager for application credentials. The
eventual Windows helper should use the Credential Manager APIs such as `CredWriteW`, `CredReadW`,
and `CredDeleteW` with a generic credential, subject to current host persistence support.

Do not silently downgrade durable Product credential intent to session-only storage when host policy
does not support the intended persistence.

## JVM-facing contract

`VexLifePartnerHostEffect.Executor` has one method:

```text
execute(NetworkPlan) -> Result
```

It has no secret-read method.

Any ordinary non-redirect HTTP response, including 4xx/5xx, returns as `Result.Http` so P1/A5 can
interpret provider-specific error bodies.

Host-level failures remain separate:

```text
CREDENTIAL_NOT_PRESENT
CREDENTIAL_PROVIDER_MISMATCH
DNS_FAILURE
CONNECT_FAILURE
TLS_FAILURE
TIMEOUT
REDIRECT_REJECTED
HOST_UNAVAILABLE
```

## Receipt membrane

The host receipt may retain:

```text
providerRef
credentialRef
exact request URL
request-body SHA-256
whether a network effect was attempted
HTTP status when one existed
response-body SHA-256 when one existed
redirectFollowed=false
secretProjected=false
```

The JVM verifies that receipt against the exact admitted NetworkPlan.

The secret value, authentication header, keychain record bytes, Credential Manager blob, and native
process memory are not receipt material.

## Remaining platform realization

Still unformed:

```text
macOS native helper
  Security.framework Keychain
  HTTPS client with redirect refusal

Windows native helper
  Credential Manager
  HTTPS client with redirect refusal

packaging / helper identity / process protocol
platform-host proof
```

No live provider credential is required to implement or deterministically test the protocol.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][HOST-EFFECT] -->
