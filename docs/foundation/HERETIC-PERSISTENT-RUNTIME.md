# Heretic persistent local runtime

`[VXG RealForever]`

## Human question

Once VexLife-Prod has an exact certified Heretic model, exact local artifact bytes, and an exact
Python/MLX environment, can it consume one qualified runtime without re-declaring those facts,
counterfeiting predecessor process ownership, or confusing "a process exists" with "this exact runtime
is current for Vex"?

## Dependency chain

~~~text
VexLifeProdHome
  isolated Home + Companion-lineage coordinate
        ↓
HereticProductIdentity
HereticArtifactCatalog
HereticModelConfiguration
        ↓
HereticArtifactMaterialization.Result
  exact model bytes
        ↓
HereticPythonEnvironment.Result
  exact Python 3.14.7 + mlx-lm 0.32.0
        ↓
HereticPersistentRuntime.bind(...)
  consumes those exact outputs
  + one explicit product-runtime implementation identity
        ↓
runtime selection
  |
  +-> StableVexLifeHereticRuntimeCompatibility
  |     read-only predecessor receipt/process verification
  |     no ownership transfer / no receipt rewrite
  |
  +-> HereticPersistentRuntime
        VexLife-Prod-owned STARTING / QUALIFIED
        reuse / recover / implementation-rotate / stop
        ↓
MacHereticRuntimePlatform
  physical process / port / health / MLX qualification
        ↓
MacUserLaunchdProcess
  generic construction + lifetime substrate
~~~

No downstream owner gets a second copy of the model ref, artifact hash, Python version, MLX version,
or runtime path as caller-provided configuration.

## Why the binding exists

The current VexLife implementation carries many runtime facts together because one Node module both
validates the binding and performs the process effects.

The fresh body makes that dependency explicit:

~~~text
materialization result
+ environment result
+ source identity
        ↓
runtime Binding
~~~

A caller cannot form a runtime Binding merely by remembering:

~~~text
model path
python path
port
model ref
profile ref
content hash
versions
~~~

The earlier owners must first produce their verified results.

This is the anti-hidden-configuration boundary.

## Portable runtime meaning

Common source owns the meaning of:

~~~text
state
  PERSISTENT_MLX_RUNTIME_STARTING
  PERSISTENT_MLX_RUNTIME_QUALIFIED

qualification
  exact /health = ok
  exact model materialization exposed exactly once
  one real non-streaming chat completion
  qualification output is NOT lived Conversation

resource ownership
  UNOWNED
  UNKNOWN
  AMBIGUOUS
  EXACT_OWNED

resource lifecycle
  CURRENT
  STALE
  ABANDONED
  UNKNOWN
~~~

Permanent:

~~~text
PID MATCH
!= EXACT PROCESS OWNERSHIP

PORT ANSWERS
!= EXACT PROCESS OWNERSHIP

HEALTHY PROCESS
!= QUALIFIED HERETIC PRODUCT RUNTIME
~~~

## Persistent runtime state machine

### Stable predecessor borrow

Canonical-Home continuity may already have one qualified Heretic runtime owned by stable VexLife:

~~~text
runtime/heretic/persistent-runtime-owner-receipt.json
  stable VexLife owner
        ↓
exact stable runtime schema/state/model/artifact/environment/argv/effect validation
        ↓
stable launchd v1 receipt validation
  (v1 durable receipt omits environment)
        ↓
launchd coordinate + PID currentness
+ exact live process/process-group identity
        ↓
borrow QualifiedRuntimeEvidence
        ↓
re-observe same predecessor owner before later inference
~~~

Borrowing does **not** write a VexLife-Prod receipt and does not make the stable process
VexLife-Prod-owned.

~~~text
BORROWED QUALIFIED RUNTIME
!= PROCESS OWNERSHIP TRANSFER

STABLE RECEIPT READ
!= STABLE RECEIPT MUTATION

STABLE LAUNCHD v1 RECEIPT
!= VEXLIFE-PROD LAUNCHD v2 RECEIPT

V1 COMPATIBILITY OBSERVATION
!= DURABLE SCHEMA RELABELING

PORT 8765 ANSWERS
!= BORROWED RUNTIME ADMISSION
~~~

If the stable receipt is absent, product-owned runtime formation may proceed under the separate
VexLife-Prod receipt coordinate. If the stable receipt is present but invalid/drifting, setup holds
rather than silently starting a competing process on the same endpoint.

### New product-owned start

~~~text
fresh exact environment observation
-> matching-process census is empty
-> port 8765 is unoccupied
-> launch through generic macOS process substrate
-> prove exact PID + process group + launchd substrate
-> write STARTING ownership receipt
-> wait for exact health
-> perform qualification
-> write QUALIFIED receipt
~~~

The STARTING receipt is deliberately written **before** readiness/qualification so a crash does not
leave an unrecorded owned process.

### Exact current reuse

~~~text
current receipt
+ live exact process identity
+ exact current launchd substrate
-> requalify
-> refresh QUALIFIED receipt
-> reuse
~~~

### STARTING recovery

~~~text
current STARTING receipt
+ exact live process
-> finish bounded readiness
-> qualify
-> QUALIFIED
~~~

### Source rotation

A runtime source change is a true dependency change.

~~~text
same binding facts
+ prior runtime-owner source SHA
+ exact owned prior process
-> retire exact prior process
-> verify not live
-> delete old ownership receipt
-> start current source runtime
~~~

A source change does **not** change the model identity, Home, port, UI, or setup topology.

### Failure before semantic ownership

A newly launched process may have a source-owned launchd construction receipt before Heretic semantic
ownership has been proven.

~~~text
spawned process
-> exact Heretic process identity NOT proven
-> retire through provisional construction ownership
-> NO Heretic runtime receipt is manufactured
~~~

This distinction was added explicitly because inventing an "exact-owned" semantic receipt merely to
clean up an unproven process would be false ownership.

### Stop

~~~text
current exact receipt
-> if live, prove exact process + process group + launchd substrate
-> retire exact launchd process
-> verify not live
-> delete receipt
~~~

Unknown or drifting ownership holds before termination.

## macOS realization

`MacHereticRuntimePlatform` owns the physical observations/effects required by the semantic runtime:

- fresh Python/MLX environment observation;
- macOS process census;
- exact PID/process-group identity;
- loopback port occupancy;
- generic launchd process start/retire;
- bounded `/health` readiness;
- MLX `/v1/models` exact model identity;
- one bounded `/v1/chat/completions` qualification.

Qualification runs through the **same verified Python environment** that owns the MLX runtime. It
uses Python standard-library HTTP/JSON support, so this port does not invent another runtime package
or JSON dependency for the MLX probe.

## Effect boundary

A qualified persistent-runtime receipt carries:

~~~text
modelRuntime       true

providerNetwork    false
modelDownload      false
artifactMutation   false
training           false
weightMutation     false
memoryMutation     false
activation         false
succession         false
publication        false
repositoryMutation false
homeMutation       false
~~~

Artifact acquisition and environment creation are previous owners. Runtime qualification does not
grant those effects retroactively.

## Process isolation

A newly-created VexLife-Prod runtime lives under the product-owned runtime namespace and uses the
generic macOS process substrate:

~~~text
product receipt:
  runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json

stable predecessor receipt:
  runtime/heretic/persistent-runtime-owner-receipt.json
  read-only to VexLife-Prod
~~~

A product-owned runtime launchd label remains in the VexLife-Prod namespace. A borrowed stable
runtime retains its stable VexLife launchd/process ownership; only portable qualified-runtime
evidence crosses the compatibility boundary.

## Current source

~~~text
src/commonMain/kotlin/vexlife/model/heretic/
src/jvmMain/kotlin/vexlife/model/heretic/runtime/
src/jvmMain/kotlin/vexlife/platform/macos/

src/commonTest/kotlin/vexlife/model/heretic/
src/jvmTest/kotlin/vexlife/model/heretic/runtime/
src/jvmTest/kotlin/vexlife/platform/macos/
~~~

## Provenance

The port is grounded in current VexLife:

~~~text
src/core/heretic-runtime-link.mjs
src/core/heretic-runtime-establishment.mjs
src/core/heretic-persistent-runtime-owner.mjs
src/core/artifact-delivery.mjs
src/core/macos-user-launchd-process.mjs
scripts/establish-heretic-runtime.mjs
~~~

Those paths are provenance/reference for the relationship port. The fresh runtime must not require
the VexLife repository at runtime.

## Proof boundary

Source tests are formed for:

- exact product identity/catalog/configuration;
- portable runtime semantic invariants;
- exact artifact plan/classification;
- exact Python/MLX create/verify/reuse semantics;
- generic launchd start/retire behavior under a fake host;
- persistent runtime start/reuse/STARTING recovery/implementation rotation;
- exact stable VexLife predecessor receipt/process compatibility without ownership transfer;
- orphan/port holds;
- provisional cleanup without false semantic ownership;
- qualification failure cleanup;
- exact stop ownership;
- macOS adapter wiring and qualification command shape.

Still required before acceptance:

~~~text
repository-owned Kotlin 2.4.20 JVM + common/jvm tests
Kotlin/JS compile for common contracts
durable product-owned Home/runtime receipt persistence through VexContractJson
lived stable-runtime borrow against the canonical predecessor receipt
real macOS launchd + process census proof for a product-owned fallback runtime
real certified model artifact/environment reuse or acquisition
real MLX start + health + model + chat qualification
sustained local runtime after launcher completion
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][HERETIC-PERSISTENT-RUNTIME] -->
