# Heretic product model package

`[VXG RealForever]`

## Human question

Can the fresh VexLife product answer **what local Heretic model it is built to use** from one
compiler-visible package, without making setup scripts, runtime processes, or presentation code
alternate owners of that identity?

This foundation ports the already-earned Heretic product identity from current VexLife into:

~~~text
vexlife.model.heretic
  HereticProductIdentity
    Current

  HereticArtifactCatalog
    Members

  HereticModelConfiguration
    BindingScope
    State
    Value

  HereticContractValidation
    shared validation only
~~~

## Porting rule

This is a faithful relationship port, not an implementation transplant.

Current VexLife spreads the relationship across:

~~~text
blueprint/model-bundle-registry.json
blueprint/artifact-registry.json
src/core/heretic-runtime-link.mjs
src/core/heretic-runtime-establishment.mjs
src/core/heretic-persistent-runtime-owner.mjs
scripts/establish-heretic-runtime.mjs
~~~

Those sources currently answer several different questions. The fresh package sorts the **stable
product facts** into one semantic/catalog owner while keeping runtime effects outside it.

~~~text
MODEL IDENTITY / CERTIFIED ARTIFACT COMPOSITION
  -> vexlife.model.heretic

ARTIFACT ACQUISITION
PYTHON / MLX ENVIRONMENT CREATION
PROCESS START / STOP
LAUNCHD / PLATFORM OWNERSHIP
HEALTH + MODEL + CHAT QUALIFICATION
  -> separate platform/runtime realization
~~~

The package is deliberately effect-inert.

## Exact current product identity ported

~~~text
model bundle
  model-bundle.vexlife.heretic.qwen3.8-27b.mlx4.329658ee8f27.001

model
  model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27

profile
  model-profile.devex.qwen38-27b-heretic-mlx4.certified.20260928A

artifact set
  artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2

content-set SHA-256
  329658ee8f270056bc29d8a6cfd6e9f1b7d9d14c659452c9e4dd46fc7c78d780

certified artifact members
  11

certified bytes
  15,153,242,506

runtime adapter
  adapter.runtime.mlx.macos-victor.001

runtime class
  MLX_LM_DIRECT_RUNTIME

transport
  MLX_LM_OPENAI_NUMERIC_LOOPBACK

current VexLife product host profile
  profile.repository.vexlife.macos-v1

Python
  3.14.7

mlx-lm
  0.32.0

endpoint
  http://127.0.0.1:8765

request model
  default_model
~~~

The historical runtime-link source also recognizes an older SDK host-profile identity for legacy
receipt compatibility. The fresh package does **not** collapse that historical identity into the
current product identity.

## Configuration relationship

Current VexLife's product configuration is preserved exactly in meaning:

~~~text
Heretic establishment pending
  HERETIC_ESTABLISHMENT_PENDING

        ↓ exact qualified runtime receipt

bound Heretic product model
  BOUND_HERETIC_PRODUCT_MODEL
~~~

Both states remain scoped to:

~~~text
Vex Home ref                  <- supplied by rightful Home owner
current Companion lineage ref <- supplied by rightful Companion owner
exact Heretic product identity
~~~

`BindingScope` is therefore a borrowed coordinate, not a second Home or Companion identity owner.

And neither configuration state grants these effects:

~~~text
automaticFallback   = false
automaticDownload   = false
automaticActivation = false
~~~

Selecting the model is therefore not permission to download it, start it, replace another model, or
activate a runtime.

The product-owned binding may point at one of two admitted runtime-evidence coordinates:

~~~text
stable predecessor:
  runtime/heretic/persistent-runtime-owner-receipt.json

VexLife-Prod-owned:
  runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json
~~~

That path records **where qualification evidence comes from**. It does not transfer process
ownership. A later exact qualified observation may rebind this product projection from one admitted
owner coordinate to the other without changing the model/Vex identity.

## Model catalog (2026-11-14)

The Product now offers more than one local Heretic model. `HereticModelCatalog`
(`src/commonMain/kotlin/vexlife/model/heretic/HereticModelCatalog.kt`) is the one source-owned index:

| handle | model | weights | wants | standing |
|---|---|---|---|---|
| `heretic-27b` | Qwen3.8 27B Heretic, MLX 4-bit — `donedynamics/Qwen3.8-27B-heretic-MLX-4bit@808b82a2` | 15.15 GB, 11 members | 32 GiB | CERTIFIED (the identity above, unchanged) |
| `heretic-9b` | Qwen3.8 9B Heretic, MLX 5-bit — `Foresee/Qwen3.8-9B-heretic-uncensored-5bit-MLX@8a3eec7d` | 6.74 GB, 10 members, content set `5901a94b…` | 16 GiB | CANDIDATE |
| `heretic-4b` | Qwen3.8→Qwen3.5 4B Distill, Heretic + abliterated, MLX 4-bit — `yachen4ever/Qwen3.8-4B-Distill-Heretic-Abliterated-MLX-4bit@f47bef77` | 3.05 GB, 11 members, content set `d2f8f402…` | 8 GiB | CANDIDATE |

Every member of every model is pinned by byte length and SHA-256 at one provider revision (LFS
members by their published SHA-256 oid, small files by hashing the bytes at that revision), and
each model's content-set SHA-256 is reproduced by `HereticArtifactMaterialization.plan(entry)` with
the same formula that reproduces the certified `329658ee…`. All three share the Qwen3.5
`ForConditionalGeneration` architecture class and the certified runtime relationship (adapter,
MLX_LM_DIRECT_RUNTIME, mlx-lm 0.32.0, Python 3.14.7, loopback endpoint, `default_model`). The 9B and
4B chat templates are the standard Qwen3.5 template (same `<think>` handling, no reasoning-effort
preamble); the 27B's adds reasoning-effort instructions.

CANDIDATE means: exact bytes pinned, qualified at every start by the same runtime qualification as
the certified model (health, model enumeration, one real chat completion), lived comparison still
owed. It is never an automatic fallback; a person chooses it.

What the catalog does not own: bytes on disk (`HereticArtifactMaterialization`, now per model under
`models/heretic/<model folder>/<revision>`), the Home's choice (`FileHereticModelConfigurationStore`
— same v1 schema; a bound 27B Home reads exactly as before), runtime liveness and receipts
(`HereticPersistentRuntime`, whose `Binding` now names the catalog model its files are and whose
receipts carry that model's identity).

Relationships that changed:

~~~text
Home never chose        -> largest offered model that fits this Mac's memory (27B on Victor's Mac)
Home chose model X      -> X is materialized, bound and run; nothing else binds the Home
runtime serves Y != X   -> at Setup Start the runtime owner retires its own exact Y runtime
                           (same exact-ownership path as a stale implementation), starts X:
                           SWITCHED_MODEL_AND_STARTED_NEW_EXACT_MLX_RUNTIME
borrowed stable runtime -> only ever for the certified 27B
service process         -> still reuse-only: it never starts or switches a runtime
~~~

Host eligibility (`HereticMacHostProfile`) was a stale guard for this: it required exactly an
Apple M4 Pro and 12 GiB, the certification envelope of the 27B on Victor's Mac, which kept every
other Mac out. It now admits Apple silicon (any M-series chip) with at least the smallest model's
8 GiB; whether a given model fits is answered per model. The profile ref is unchanged so existing
runtime receipts stay exact.

Removing a model (`HereticArtifactMaterialization.remove`) deletes only that model's own pinned
member files (and their `.partial` downloads), then its empty folders. The model stays offered and
can be downloaded again.

## Certified artifact catalog

The 11-member bundle is source-placed as typed data with:

~~~text
role ref
artifact ref
filename
media type
SHA-256
expected byte count
provider source ref
license ref
~~~

The catalog asserts unique refs/names, exact member count, and the exact aggregate byte count.

The artifact bytes themselves are **not** stored in this repository.

## Portable runtime meaning now carried with the package

The package also ports the **meaning** that the current VexLife persistent-runtime owner must prove,
without porting its macOS process effects yet:

~~~text
HereticRuntimeState
  STARTING
  QUALIFIED

HereticRuntimeQualification
  healthAndExactModelVerified = true
  actualChatCompletionPerformed = true
  qualificationInferenceEffect = true
  qualificationOutputPersisted = false
  livedConversationEffect = false

HereticRuntimeObservation
  ownership:
    UNOWNED
    UNKNOWN
    AMBIGUOUS
    EXACT_OWNED

  lifecycle:
    CURRENT
    STALE
    ABANDONED
    UNKNOWN

HereticRuntimeEffectBoundary
  modelRuntime = true
  providerNetwork = false
  modelDownload = false
  artifactMutation = false
  training = false
  weightMutation = false
  memoryMutation = false
  activation = false
  succession = false
  publication = false
  repositoryMutation = false
  homeMutation = false
~~~

The observation contract preserves a particularly important current VexLife rule:

~~~text
PID MATCH
!= EXACT PROCESS OWNERSHIP
~~~

An `EXACT_OWNED` observation must carry current receipt/runtime/executable/process-group evidence and
must explicitly say `pidMatchOnly = false`. An `ABANDONED` runtime requires current not-live
evidence. A `STALE` runtime requires exact ownership plus source-stale evidence.

Qualification preserves the existing lived qualification threshold:

~~~text
GET /health
-> exact status = ok

GET /v1/models
-> exact model materialization appears exactly once

POST /v1/chat/completions
-> requestModel = default_model
-> one real non-streaming completion
-> non-empty response
-> not written into lived Conversation
~~~

The future platform runtime owner must produce these portable truths. It does not get to redefine
their meaning because it happens to own launchd/process mechanics.

## VexArchitecture boundary

This package is not a Family merely because it is important.

It currently answers an identity/configuration question, not a human Control against live State.

~~~text
HERETIC PRODUCT MODEL CONTRACT
!= LIVE MODEL PROCESS
!= DOWNLOAD OWNER
!= EXECUTION CUSTODY
!= COMPANION
!= PRESENTATION
~~~

When the fresh build ports runtime realization, that work must identify the next rightful owners
rather than growing effectful methods inside the Heretic product package.

## Provenance and currentness

The VexLife sources above are formation provenance for the faithful port.

After acceptance, this package owns the fresh product's Heretic identity. Runtime code should consume
this package rather than repeatedly reading historical VexLife files or copying their constants.

A later intentional model/profile/artifact change must update this package as one source change and
re-prove every true dependent. It must not be inferred from whichever runtime script changed most
recently.

<!-- [VXG RealForever][VEXLIFE-PROD][HERETIC-PRODUCT-MODEL] -->


## Formation proof

Disposable compiler pressure on the source-formed package:

~~~text
kotlinc-jvm 1.9.0
  production package compile                         PASS

runtime probes
  exact identity load                               PASS
  11-member catalog / 15,153,242,506-byte total    PASS
  pending configuration has no automatic effects   PASS
  bound configuration transition                   PASS
  STARTING / QUALIFIED wire compatibility          PASS
  qualification effect boundary                     PASS
  exact-owned != PID-only ownership                PASS
  abandoned runtime requires not-live evidence     PASS

Kotlin/JS IR 1.9.0
  production package compile                        PASS
~~~

This is formation evidence only.

~~~text
DISPOSABLE KOTLIN 1.9 PROOF
!= REPOSITORY-OWNED KOTLIN 2.4.20 ACCEPTANCE
~~~

Required before acceptance:

~~~text
exact candidate
-> repository Gradle wrapper
-> Kotlin 2.4.20 JVM compile + common tests
-> Kotlin 2.4.20 JS IR compile + test compile
-> exact source review against current VexLife provenance
~~~

Runtime realization remains a separate held effect even after this identity/configuration package is
accepted.
