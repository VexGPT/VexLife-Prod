# Windows Heretic runtime realization

`[VXG RealForever]`

## Human question

Once VexLife has:

- one current Product model choice; and
- one exact observed platform target;

what exact implementation realizes that model on that target, and can the Product materialize the
required bytes without inventing a second model chooser or silently falling back to another
platform?

## Relationship

```text
HereticModelCatalog
  what models the Product offers
        +
VexLifePlatformRealization.Target
  exact platform / architecture selected from host truth
        ↓
HereticRuntimeRealization
  exact (modelRef, target) -> realization or explicit absence
        ↓
platform dependency materializer
        ↓
verified local dependencies
        ↓
later runtime-process owner
```

Permanent:

```text
MODEL OFFERED
!= REALIZATION EXISTS ON THIS TARGET

RUNTIME CONFIGURATION
!= RUNTIME REALIZATION

DEPENDENCIES MATERIALIZED
!= RUNTIME STARTED

RUNTIME STARTED
!= PRODUCT SERVICE STARTED
```

## VexArchitecture ownership map

This relationship follows the current VexArchitecture rule that an architectural noun is earned by
the question it alone answers. The edges are therefore owners, not a required pipeline of fashionable
types:

```text
human model intention
        ↓
VexLifeModelsFamily
  State / Control / lawful Effect meaning
        +
VexLifePlatformRealization.Target
  exact observed platform / architecture
        ↓
HereticRuntimeRealization
  static exact (modelRef, target) composition
        ↓
VexLifeWindowsRuntimeMaterialization
  exact dependency custody/materialization
        ↓
VexLifeWindowsHereticRuntime
  concrete process lifetime + qualification
        ↓
realization-scoped qualified evidence
        ↓
later Product-binding reconciliation
        ↓
concrete platform Product-service lifetime
        ↓
shared VexLife Product service / Conversation
```

The neighboring current-main owners remain separate because they answer different questions:

- `HereticCompanionRepresentation` owns provider/message/reasoning/generation representation. It does
  not become runtime realization merely because both eventually affect inference.
- `HereticExecutionTopology` is a read-only projection of effective model/runtime/continuity
  configuration and provenance. It does not become configuration or realization authority.
- PR #246 keeps human model target, runtime-configuration choice, and explicit Start semantics in the
  Models relationship. This foundation supplies exact target realization; it does not create a
  second model Family or a Windows-specific chooser.
- The currently implemented `promptCacheEntries` setting was earned against the Product MLX runtime.
  A Windows llama.cpp realization must not inherit that setting merely because it realizes the same
  `modelRef`. Cross-target configuration must be admitted by the selected realization/runtime
  relationship, not by model identity alone.

### Partner and sustained-service decisions

No `VexPartner` relationship is earned by the current Windows runtime edge. The llama.cpp process is
a Product-owned concrete runtime realization, and the current proof does not expose a recurring
independently-owned Source / Signal / Connection / Conversion / Observation vocabulary whose loss
would make the relationship ambiguous. If such an external-participant relationship appears later,
it can earn Partner then; the name `Connection` or an HTTP boundary alone is not enough.

Likewise, this source does not invent a generic `VexService` root. The Windows runtime process and
the later long-lived Product service have concrete lifetime owners. A reusable Service root is only
earned if a repeated Admission / Retention / Release law becomes real and would otherwise be lost.

```text
SEPARATE OWNERSHIP
!= FRAGMENTATION

ONE PRODUCT BODY
!= ONE GIANT OWNER
```

## Checkpoint marker — reusable target-neutral resolver

Checkpoint:

```text
checkpoint.vexlife-prod.heretic-runtime-realization.target-neutral-resolver.20261117A
```

The reusable Product-facing interface is now:

```kotlin
val resolver =
    HereticRuntimeRealization.Resolver(
        targetSource,
    )

val result =
    resolver.resolve(
        selectedModel,
    )
```

The consumer does **not** contain:

```text
if Mac -> use MLX
if Windows -> use llama.cpp
```

Construction supplies one no-effect `TargetSource` from the rightful host/platform observation
owner. On every call, `Resolver` asks that source for the fresh exact target and returns:

```text
Resolution.Supported(exact realization)
or
Resolution.Unsupported(modelRef, exact target)
```

This means the same consumer can survive another target realization being added later without
learning a new platform branch. Adding Linux, another Windows architecture, or another Mac
realization means adding exact source-owned realization coverage plus its rightful platform owners;
it does not mean teaching every caller another conditional.

The interface deliberately does **not** observe the host itself. Host truth belongs to the platform
observation owner. This preserves the VexArchitecture boundary:

```text
host observation
  -> TargetSource
  -> Resolver
  -> exact realization

OBSERVATION OWNER
!= REALIZATION OWNER
!= CONSUMER
```

Focused source tests pressure both directions through the same Resolver instance:

```text
fresh target = macOS ARM64
  -> Qwen 4B -> MLX realization

fresh target = Windows x64
  -> same Qwen 4B -> llama.cpp realization

fresh target = Windows x64
  -> Qwen 9B -> Unsupported
  -> no Mac fallback
```

<!-- VXG-CHECKPOINT:HERETIC-RUNTIME-REALIZATION-TARGET-NEUTRAL-RESOLVER-20261117A -->

## Current coverage

Mac ARM64 coverage is explicit for every currently offered catalog model. This source does not move
or replace the existing Mac runtime owner; it records the current model/target relationship so a new
platform does not accidentally redefine what "supported" means.

Windows x64 currently has one admitted model realization:

```text
Heretic 4B Distill
-> yachen4ever/Qwen3.8-4B-Distill-Heretic-Abliterated-GGUF
-> revision c0807aa5c3a6e689a4dda5db58ab1d10eb40ab61
-> model-Q4_K_M.gguf
-> 2,790,000,800 bytes
-> sha256 7b95847ce1684f98d96f13bad2daaeffcfd2f95ea2ff3821ee68943590a10159
```

No current Windows realization exists for 27B, 9B, or gpt-oss 20B. Their presence in the Product
catalog does not grant Windows support.

## Windows runtime binary

The lived #227 proof initially mislabeled the runtime as CUDA. Exact executable identity later made
the real relationship visible:

```text
provider       ggml-org/llama.cpp
release        b10107
source commit  c0bc8591e8815c63cb01dd3f051a8b0df02501c9

asset
llama-b10107-bin-win-cpu-x64.zip

asset bytes
18,213,827

asset sha256
52133a0a5a8f6035b1bdd2f89c3425ea8b742413d9bdb9a2dee30e3a1681b18c

packaging source
ggml-org/llama.cpp@c0bc8591e8815c63cb01dd3f051a8b0df02501c9:
.github/workflows/release.yml#windows-cpu

producer relationship
7z packs build/bin/Release/* into the CPU ZIP;
the release job only renames that ZIP

llama-server.exe sha256
af3e56d6bdb84a9b6bd50f6ad748809370bc29be42d58088041408b2933d80f7

backend
CPU
```

The ordinary Product realization therefore identifies the backend as CPU and does not request GPU
offload.

## Materialization owner

`VexLifeWindowsRuntimeMaterialization` owns only exact dependency presence/materialization beneath
one established Vex Home.

Exact paths for the current realization:

```text
model:
<HOME>/models/heretic/qwen3.8-4b-distill-gguf/
  c0807aa5c3a6e689a4dda5db58ab1d10eb40ab61/
  model-Q4_K_M.gguf

runtime archive:
<HOME>/runtime/vexlife-prod/llama-cpp/b10107/windows-x64-cpu/
  archive/
  llama-b10107-bin-win-cpu-x64.zip

runtime image:
<HOME>/runtime/vexlife-prod/llama-cpp/b10107/windows-x64-cpu/
  image/
  llama-server.exe
  ...exact files extracted from the verified release archive...
```

Transport is not reimplemented. The materializer delegates byte carriage to
`DirectHttpsArtifactDelivery`, which already owns resumable HTTPS range requests, exact size/hash
verification, partial recovery, and atomic file publication.

The runtime archive is itself exact by bytes + SHA-256 before extraction. Extraction:

- rejects absolute, backslash, duplicate, empty-segment and `..` paths;
- occurs into one staging directory inside the Home;
- verifies the exact `llama-server.exe` SHA-256;
- atomically publishes the extracted runtime image;
- never overwrites unexpected existing model/runtime bytes.

Missing dependencies require explicit acquisition authority. Invalid existing bytes are held for a
separate repair owner rather than silently replaced.

## Runtime process / qualification owner

`VexLifeWindowsHereticRuntime` consumes only a ready exact materialization. It does not acquire
dependencies itself.

Current lived launch profile carried by the realization:

```text
llama-server.exe
-m <exact GGUF>
--alias default_model
--host 127.0.0.1
--port 18080
--ctx-size 16384
--jinja

GPU offload flag
ABSENT
```

The `16384` context and Jinja relationship are recorded as the **currently lived Windows 4B launch
profile**, not as a universal Product context policy. The runtime realization can evolve only through
source + matching proof.

Qualification reuses the exact behavior already earned by the #227 real-host proof:

```text
process live
-> GET /health = status ok
-> GET /v1/models exposes exact request alias once
-> POST /v1/chat/completions
     max_tokens = 1
     temperature = 0
     stream = false
     enable_thinking = false
-> returned model = default_model
-> non-empty assistant content
-> qualification output is not persisted into Conversation
```

After qualification the owner writes:

```text
runtime/vexlife-prod/heretic-windows-runtime-owner-receipt.json
```

The receipt names the exact realization, PID, model/runtime paths, runtime binary/archive identity,
qualification evidence, effect boundary, working directory, **exact launch argv**, and runtime log.
Same-process currentness requires:

- the process is still live;
- the exact whole runtime materialization is still current;
- the durable receipt still names that same PID/evidence/paths;
- the receipt launch argv still equals the launch contract current source would form for that same
  realization;
- health and exact model exposure still pass.

Cross-process adoption/recovery is deliberately not invented here. A later Windows lifetime owner
must prove exact process ownership before it may reuse or retire a process.

## Current Product-binding hold

Current-main `HereticModelConfiguration.QualifiedRuntimeEvidence` still validates runtime fields
against the catalog model identity's Mac/MLX coordinates and has no `realizationRef`. That source is
the actual present boundary; an older PR number is not current authority for it.

Therefore this Windows leaf forms its own exact `QualifiedRealizationEvidence` and stops there.

Permanent:

```text
WINDOWS RUNTIME QUALIFIED
!= PERMISSION TO BYPASS CURRENT PRODUCT BINDING OWNER

SAME MODEL
!= SAME RUNTIME REALIZATION
!= SAME RUNTIME CONFIGURATION
```

A coordinated consumer must admit realization-aware evidence into Product binding before ordinary
Windows setup/service can treat this runtime as the selected Product runtime. It must also reconcile
#246's staged target/configuration/explicit-Start semantics without applying MLX-only configuration
to llama.cpp by `modelRef` alone. This PR must not force Windows through Mac runtime identity merely
to satisfy the existing type.

## Exact owners inside this foundation

This foundation contains three deliberately different owners:

- `HereticRuntimeRealization` owns static model + target -> dependency/process/qualification
  composition and performs no effect.
- `VexLifeWindowsRuntimeMaterialization` owns exact dependency presence/acquisition and performs no
  model-selection or runtime-start effect.
- `VexLifeWindowsHereticRuntime` owns the concrete Windows llama.cpp process it starts, its bounded
  HTTP qualification, same-process re-observation, receipt, and retirement of that exact started
  process.

The foundation as a whole still does **not** own:

- the person's model-selection / staged-target / Start meaning;
- per-model runtime configuration such as MLX prompt-cache entries;
- Product model binding;
- the long-lived Windows Product-service lifetime;
- Windows Product-entry observation/Setup manifestation or the private Product image;
- app/package installation;
- Windows release publication.

Those remain separate current or successor owners. Keeping them separate preserves the causal edges;
it is not permission for two owners to decide the same State or effect.

## Proof boundary

Deterministic source proof must establish:

```text
all current models have deliberate Mac target coverage
Windows x64 has only explicitly admitted model coverage
exact Windows GGUF/runtime provenance is pinned
no target fallback exists

missing dependencies + no authority -> no effect
partial model delivery -> resumes through shared transport
exact downloads -> verified
runtime archive -> safe staged extraction
second observation -> reuse without transport
invalid existing bytes -> preserved / held
archive traversal -> rejected before image publication
```

A later real-host proof is required before claiming ordinary Windows acquisition. That host proof
should download/reuse the exact current realization into a technical/protected test Home first; it
does not require the final Windows Product service or installer to prove this materialization owner.

## Lineage and current convergence

- #223 owns Windows host/architecture/Home readiness observation and is currentized onto current
  `main`.
- #246 is the current-main Models integration surface for staged target + runtime configuration +
  explicit Start; #238 is its development/proof lineage.
- #227 is lived proof/evidence: the 4B Windows runtime, shared Product Conversation/restart circuit,
  realization-bound Product binding, and CPU identity correction all passed there. Its patient-zero
  process/relay history is not ordinary Product architecture.
- This foundation owns the durable selected-model + exact-target runtime realization, dependency
  materialization, and concrete Windows runtime qualification relationship.
- The next integration edge is realization-aware Product binding + ordinary Windows
  Setup/service-lifetime consumption. That edge should join these current owners; it should not
  recreate their meanings in a new all-in-one owner.

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS][HERETIC-RUNTIME-REALIZATION] -->
