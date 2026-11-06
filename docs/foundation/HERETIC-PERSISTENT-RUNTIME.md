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
StableVexLifeHereticRuntimeCompatibility
  read-only predecessor receipt/process verification
  no ownership transfer / no receipt rewrite
        ↓
HereticRuntimeAvailability
  Family question:
    "What does EnsureAvailable mean
     against exact current runtime truth?"
  |
  +-> exact predecessor borrowable
  |     -> admit Qualified(BorrowedStableVexLife)
  |     -> no product-owned Effect
  |
  +-> predecessor unavailable
        -> request product-owned Effect
           OBSERVE current
           or ENSURE available
        ↓
HereticPersistentRuntime
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

## Runtime availability relationship

The lived baseline earned one cross-owner semantic question that does not belong to either process
implementation owner:

> **What does `EnsureAvailable` mean against the exact current Heretic runtime relationship?**

That question is now owned by `HereticRuntimeAvailability`.

~~~text
State.StableBorrowable
+ Control.EnsureAvailable
-> State.Qualified(BorrowedStableVexLife)
-> no product-owned Effect

State.StablePredecessorUnavailable
+ Control.EnsureAvailable(AcquisitionAllowed)
-> Effect.EnsureProductOwnedRuntime

State.StablePredecessorUnavailable
+ Control.EnsureAvailable(ObservationOnly)
-> Effect.ObserveCurrentProductOwnedRuntime

product-owned qualified observation
-> Family interpretation
-> State.Qualified(VexLifeProdOwned)
~~~

Permanent ownership boundary:

~~~text
HereticRuntimeAvailability
  owns the semantic availability decision

StableVexLifeHereticRuntimeCompatibility
  owns predecessor receipt/process observation

HereticPersistentRuntime
  owns VexLife-Prod process lifecycle + qualification

VexLifeProdLocalSetup
  composes the relationship and realizes the requested rightful Effect

BORROWED QUALIFIED EVIDENCE
!= PROCESS OWNERSHIP TRANSFER

FAMILY EFFECT
!= FAMILY PERFORMS PLATFORM WORK
~~~

The Family is intentionally small. Artifact acquisition, Python/MLX environment formation, launchd,
HTTP qualification and product-service lifetime remain with their existing owners.

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
VexLife-Prod receipt coordinate. An exact stable receipt whose recorded PID is no longer live is
classified as predecessor `UNOWNED / ABANDONED`: it is left untouched, is not borrowed, and the
product-owned runtime path may proceed only through its own matching-process census and port-occupancy
checks. If the stable receipt is malformed, ambiguous, or claims a live-but-drifting process, setup
holds rather than silently starting a competing process on the same endpoint.

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
runtime/vexlife-prod/
  heretic-persistent-runtime-owner-receipt.json
  launchd/
  mlx-lm-server.out.log
  mlx-lm-server.err.log
  huggingface-cache/

stable predecessor receipt:
  runtime/heretic/persistent-runtime-owner-receipt.json
  read-only to VexLife-Prod
~~~

The fallback launch environment binds `HF_HUB_CACHE` to the product-owned cache directory
explicitly. This preserves the stable VexLife lesson that MLX/Hugging Face cache selection is runtime
construction state, not ambient shell configuration. In mlx-lm 0.32.0, `/v1/models` scans the
Hugging Face cache; therefore an implicit or invalid cache coordinate can make model-identity
qualification fail even after `/health` is already ready.

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

The pre-refactor runtime body has already crossed the lived baseline on accepted VexLife-Prod main:

~~~text
accepted main
1a1745547558110fdbc420d4c15896cd353f5c46

accepted tree
760a6d10f2aa98f3a0a2eebf7d53a2f5311aa7cf
~~~

That lived trajectory established, among other predicates:

- exact predecessor receipt/currentness classification, including abandoned predecessor fallthrough
  without ownership transfer or predecessor mutation;
- product-owned fallback runtime formation/qualification under the VexLife-Prod runtime namespace;
- explicit product-owned Hugging Face cache binding for MLX model discovery;
- sustained source-current product service;
- one real canonical direct Vex turn with durable RESPONSE / Context / Head;
- a later real browser-context turn and fresh reload continuity;
- accepted-main service rotation preserving the same canonical Conversation/runtime relationship.

The **refactor claim is narrower than re-proving those product effects**. The current candidate must
prove:

~~~text
same runtime selection behavior
+ same predecessor/product custody boundary
+ same product-owned Effect realization
+ Family semantics compile/test on common source
+ LocalSetup JVM tests remain green
+ no new runtime / model / Home / Conversation effect
~~~

Do not replay a lived Conversation Send merely to prove this source refactor. A new lived effect is
required only if later source changes alter a boundary whose claim cannot be proved deterministically.

<!-- [VXG RealForever][VEXLIFE-PROD][HERETIC-PERSISTENT-RUNTIME] -->
