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

## What this does not own

This foundation does not:

- choose the person's model;
- modify per-model runtime configuration such as prompt-cache entries;
- start or stop llama.cpp;
- qualify runtime HTTP behavior;
- bind the Product model configuration;
- start/stop the long-lived Product service;
- form the Windows private Product image;
- install an app/package;
- publish a Windows release.

Those are downstream owners.

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

## Lineage

- #223 owns current Windows host/Home readiness observations.
- #238 owns staged model target + per-model runtime configuration / explicit Start.
- #227 preserves the lived 4B Windows runtime/Conversation proof and the CPU identity correction.
- This foundation owns the durable selected-model + exact-target runtime realization and dependency
  materialization relationship.

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS][HERETIC-RUNTIME-REALIZATION] -->
