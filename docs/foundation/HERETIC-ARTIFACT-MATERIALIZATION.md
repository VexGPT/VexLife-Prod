# Heretic artifact materialization

`[VXG RealForever]`

## Human question

Given the certified Heretic artifact catalog, can VexLife-Prod answer whether the exact local model
bytes are present—and acquire only missing/invalid bytes when explicitly authorized—without model
selection, runtime process ownership, or setup UI becoming artifact-integrity owners?

## Relationship

~~~text
HereticArtifactCatalog
  exact 11-member identity
        ↓
HereticArtifactMaterialization.plan()
  one provider revision
  UTF-8 path ordering
  recomputed content-set SHA-256
        ↓
classify each destination
  MISSING
  INVALID_SIZE
  INVALID_HASH
  VERIFIED_REUSABLE
        ↓
VERIFIED_REUSABLE -> reuse

other state
  -> acquisition authorized?
       no  -> HOLD
       yes -> delegated Acquirer
              -> independent exact reclassification
              -> only VERIFIED_REUSABLE continues
~~~

The network/provider downloader is intentionally injected. This owner decides **artifact exactness and
materialization**, not where network bytes may come from.

## Faithfully ported current VexLife laws

From `heretic-runtime-establishment.mjs` and `artifact-delivery.mjs`:

- one exact provider revision across all certified members;
- logical paths are safe single path segments;
- ordering is exact UTF-8 byte ordering;
- content-set identity is recomputed as:
  `path + NUL + sha256 + NUL + byteLength + LF` for every ordered member;
- a local artifact must be a regular non-symlink file;
- byte length must match exactly;
- SHA-256 must match exactly;
- only exact artifacts are reusable;
- missing/invalid bytes do not grant acquisition authority;
- after delegated acquisition, exact bytes are independently rechecked.

## Current model placement

The faithful current layout is preserved:

~~~text
<Vex Home>/
  models/
    heretic/
      qwen3.8-27b-mlx4/
        808b82a2bd04b2bac362b5d0b7a1197aeea59e57/
          <11 certified members>
~~~

The provider revision is derived from the certified provider source ref rather than copied into setup
scripts as a second truth.

## VexArchitecture boundary

~~~text
PRODUCT IDENTITY
  HereticProductIdentity

CERTIFIED COMPOSITION
  HereticArtifactCatalog

ARTIFACT MATERIALIZATION
  HereticArtifactMaterialization

NETWORK / PROVIDER DELIVERY
  delegated Acquirer (next/rightful owner)

PYTHON / MLX ENVIRONMENT
  separate owner

PERSISTENT RUNTIME PROCESS
  separate owner

COMPANION / CONVERSATION
  separate owners
~~~

This materializer may mutate artifact files only after explicit acquisition authorization. It cannot
start the runtime or activate a model merely because materialization completed.

## Formation proof

Disposable Kotlin/JVM 1.9 against the actual ported catalog:

~~~text
production source compile                       PASS
provider revision = 808b82a2...a59e57          PASS
member count = 11                               PASS
total bytes = 15,153,242,506                    PASS
content-set SHA-256 recomputation               PASS
missing classification                          PASS
exact reusable classification                   PASS
~~~

Still held before acceptance:

~~~text
repository-owned Kotlin 2.4.20 JVM compile + jvmTest
authorized acquisition integration
real fresh-host materialization proof
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][HERETIC-ARTIFACT-MATERIALIZATION] -->
