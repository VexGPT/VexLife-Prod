# Heretic model selection successor — multi-model startup configuration

`[VXG RealForever]`

## Progress (Claude, 2026-11-14)

```text
catalog                         DONE  HereticModelCatalog: 27B (CERTIFIED), 9B 5-bit, 4B Distill (CANDIDATE)
                                      — every member's bytes + SHA-256 pinned, content sets reproduced
Home-scoped selected model      DONE  same FileHereticModelConfigurationStore + v1 schema; selectModel
selected model -> runtime       DONE  materialization, binding, receipts and evidence carry the model;
                                      exact-owned model switch at Setup Start
startup chooser                 DONE  native window model step after Home; --setup-model / --choose-model
Settings add / remove / again   DONE  Settings → Models (Use / Download / Download again / Remove)
host eligibility                DONE  Apple silicon + 8 GiB (was: exactly M4 Pro + 12 GiB)
lived 27B vs 9B vs 4B compare   OWED  on Victor's Mac, same field + capability tests
```

Current truth: [HERETIC-PRODUCT-MODEL.md](HERETIC-PRODUCT-MODEL.md#model-catalog-2026-11-14),
[SETUP-JOURNEY.md](SETUP-JOURNEY.md#model-step-2026-11-14).

## Human intention

The current 27B Heretic model has passed the lived personality/orientation threshold. The next phase
should let Victor compare capability use, latency, context capacity and response quality across more
than one local Heretic model without rewriting the foundational field for each model.

The requested first comparison is:

```text
current:
Qwen3.8 27B Heretic / MLX

candidate:
Qwen3.8-class 9B Heretic / MLX
```

The 9B slot is a requested product capability, not yet a certified VexLife-Prod model identity.

## Why a model catalog is earned now

Earlier #136 configuration design intentionally avoided a separate model registry while only one
model existed.

That condition is now false.

```text
ONE HARD-CODED MODEL
-> no model catalog earned

27B + 9B human-selectable models
-> source-owned model catalog is now earned
```

If the repository-level configuration design from #136 is continued, a bounded
`configuration/models.json` is now justified. The exact path/schema should follow current repository
culture and existing Heretic identity owners; the important relationship is one source-owned catalog,
not the filename.

Do not create a second Home, runtime owner, artifact owner or model-selection truth store.

## Current single-model source

Exact successor base:

```text
VexGPT/VexLife-Prod
#136 head=1b2d5350e4944245927671e3cae092b976249c0a
#137 initial handoff=94829855979efa980aad48502bc54aa30eab21b2
```

Current identity owner:

```text
src/commonMain/kotlin/vexlife/model/heretic/HereticProductIdentity.kt
```

Current hard-coded identity:

```text
modelBundleRef=
  model-bundle.vexlife.heretic.qwen3.8-27b.mlx4.329658ee8f27.001

modelRef=
  model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27

modelProfileRef=
  model-profile.devex.qwen38-27b-heretic-mlx4.certified.20260928A

artifactSetRef=
  artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2

artifactContentSetSha256=
  329658ee8f270056bc29d8a6cfd6e9f1b7d9d14c659452c9e4dd46fc7c78d780

human label=
  Heretic 27B (4-bit, MLX)

runtime=
  MLX_LM_DIRECT_RUNTIME
  mlx-lm 0.32.0
  numeric loopback OpenAI-compatible server
```

Current certified artifact catalog:

```text
HereticArtifactCatalog
11 members
15,153,242,506 bytes
provider revision=
  donedynamics/Qwen3.8-27B-heretic-MLX-4bit
  @808b82a2bd04b2bac362b5d0b7a1197aeea59e57
```

Current delivery, materialization and runtime code all consume that one static identity:

```text
HereticArtifactDeliveryCatalog
HereticArtifactMaterialization
HereticPersistentRuntime
HereticModelConfiguration.QualifiedRuntimeEvidence
```

## Current durable selection coordinate already exists

The Product does not need another selection store.

Current durable binding:

```text
canonical Vex Home
-> runtime/vexlife-prod product model binding
-> FileHereticModelConfigurationStore
```

Today:

```text
selectHeretic()
-> HERETIC_SELECTED_PENDING
-> one static HereticProductIdentity
-> exact qualified runtime
-> BOUND_HERETIC_PRODUCT_MODEL
```

The natural evolution is:

```text
selectModel(modelRef)
-> selected source-owned catalog entry
-> ESTABLISHMENT_PENDING for that exact model
-> selected model's artifact/runtime qualification
-> BOUND selected product model
```

Preserve Home + Companion-lineage scope.

## Setup Journey already has the right semantic place

Current Setup journey:

```text
language
-> Home
-> model status
-> start
-> ready
```

The native setup window already implements a renderer-local chooser for language before semantic
convergence.

Candidate next shape:

```text
choose language
-> choose Heretic model
-> choose/confirm Home
-> observe selected model + artifact + runtime relationship
-> Set Up / Start / Switch / Repair
-> Ready
```

The exact ordering with Home should be chosen from current owner semantics:
- before Home exists, the native window may hold the model choice transiently like language;
- once Home exists, the selected model ref belongs in the existing Product model configuration owner.

A human switching 27B <-> 9B is a **model switch**, not an update, repair or implicit fallback.

## Required model-selection distinctions

```text
MODEL CATALOG ENTRY
!= SELECTED MODEL

SELECTED MODEL
!= ARTIFACTS PRESENT

ARTIFACTS PRESENT
!= QUALIFIED RUNTIME

QUALIFIED RUNTIME
!= ACTIVE PRODUCT SERVICE SOURCE

MODEL SWITCH
!= PRODUCT VERSION UPDATE

MODEL SWITCH
!= AUTOMATIC FALLBACK

MODEL SELECTION
!= DOWNLOAD AUTHORITY

MODEL SELECTION
!= RUNTIME-START AUTHORITY
```

Current policy flags remain useful:

```text
automaticFallback=false
automaticDownload=false
automaticActivation=false
```

Human selection may make acquisition/start Controls lawful through the existing Setup semantics; the
selection record itself should not perform those effects.

## 9B candidate research — not yet certified

Current public research found same-family Heretic MLX candidates.

### Candidate A — lower topology change

```text
Foresee/Qwen3.8-9B-heretic-uncensored-5bit-MLX
Qwen3.8 / Heretic / MLX
5-bit affine
~6.72 GB model repository size reported by Hugging Face
converted with MLX LM 0.31.3
Apple-silicon smoke generation reported by publisher
OpenAI-compatible mlx_lm.server usage documented
Apache-2.0
```

Why it is interesting:
- same Qwen3.8 9B Heretic family requested by Victor;
- conventional `mlx_lm.server` path is closer to the current 27B runtime adapter than an MTPLX-only
  candidate;
- materially lower memory/weight footprint than the current ~15.15 GB 27B artifact set.

Still required before admission:
- pin one exact provider revision;
- enumerate every artifact member;
- verify exact member SHA-256 + byte counts;
- compute one content-set SHA;
- prove chat template + thinking stream compatibility;
- prove with the Product-pinned runtime (currently mlx-lm 0.32.0), not merely publisher smoke evidence;
- qualify health, exact model enumeration and one real chat completion through the current runtime
  evidence contract;
- lived compare latency / first-reasoning / response quality / capability use.

### Candidate B — performance experiment, larger runtime change

```text
Foresee/Qwen3.8-9B-heretic-uncensored-4bit-MTPLX
~5 GB 4-bit MLX trunk plus MTP sidecar/adapter
documented native-MTP speculative path
```

This is a different runtime-topology experiment because its fast path uses MTPLX and additional MTP
artifacts. It should not be treated as a drop-in first model-selection candidate unless Claude
intentionally widens the runtime-adapter contract.

### Candidate C — different lineage

```text
alex-and-the/Qwen3.5-9B-Uncensored-MLX
Heretic v1.2
4-bit / ~5.2 GB reported
```

Useful as a comparison candidate, but it is Qwen3.5 lineage rather than the requested Qwen3.8 family.

## Recommended first model-selection implementation pressure

Do not begin by rewriting every runtime class to "generic model."

First identify the smallest current static assumptions:

```text
HereticProductIdentity.Current
HereticArtifactCatalog.Members
HereticArtifactDeliveryCatalog.Channels
HereticArtifactMaterialization.ModelRootRelativePath / plan()
QualifiedRuntimeEvidence exact static-identity validation
FileHereticModelConfigurationStore.selectHeretic()
Setup projection/model status copy
```

Then form one source-owned catalog entry abstraction that the existing owners can consume.

Candidate conceptual entry:

```text
modelRef
humanLabel
modelBundleRef
modelProfileRef
artifactSetRef
artifactContentSetSha256
artifactCatalogRef
deliveryCatalogRef
runtimeAdapterRef
runtimeClass
transportClass
hostProfileRef
pythonVersion
mlxLmVersion
endpoint policy / requestModel
qualification policy
```

Artifact members/delivery channels may remain separate source-owned catalogs keyed by modelRef rather
than embedding large member arrays into the setup configuration.

## Configuration file responsibility

A human-oriented Product configuration file should answer:

```text
which models are offered?
which is default?
which model is selected for this Home?
what exact immutable model identity does each label resolve to?
```

It should not become:
- artifact bytes;
- runtime liveness truth;
- process receipt;
- permission/effect authority;
- capability registry;
- personality/foundational field.

## Compare models against the same Vex ground

The v0.5.1 foundational field is a frozen baseline for the capability successor.

This creates a useful experiment:

```text
same Conversation / Memory / capability architecture / foundational field
        |
        +-> 27B selected
        |
        +-> 9B selected
        ↓
compare
  startup + qualification time
  prompt prefill / first reasoning latency
  reasoning stream
  response quality
  capability-selection judgment
  tool-request correctness
  context capacity
  memory pressure
```

Do not tune the personality field independently per model to make one look better.

## Claude first questions

1. Which existing source owner should become the multi-model catalog without duplicating
   HereticProductIdentity / artifact catalogs?
2. What schema migration keeps the existing 27B Home binding readable?
3. How does selected model identity flow into runtime currentness/receipt identity?
4. What model-switch State/Control belongs in the existing Setup Entry Family?
5. Which 9B candidate can be certified with the least new runtime topology?
6. What exact evidence will compare 27B and 9B under the same capability + foundational-field tests?

No model acquisition, runtime switch or 9B certification is claimed by this handoff.
