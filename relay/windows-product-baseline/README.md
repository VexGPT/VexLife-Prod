[VXG RealForever]

# Windows Product baseline — current durable orientation

## Why this surface exists

This companion surface preserves the first real Windows execution path for VexLife-Prod so later
instances do not reconstruct the trajectory from chat, repeat harness mistakes, or confuse build
tooling with Product/runtime behavior.

The original question was:

> How much of VexLife-Prod is already portable to a real Windows x64 host, and where does the first
> genuine Windows Product realization boundary begin?

This surface is proof/harness continuity only. It is not the Windows Product implementation owner.

## What is now grounded

Real Victor Windows host:

```text
OS target                 WINDOWS / X86_64
process architecture      X86_64
routeRef                  route.vexlife-prod.windows.x86-64
```

Real-host source proof against accepted Product source at the time:

```text
repository                 VexGPT/VexLife-Prod
source                     655e7c78cefba06a5e87afad285bb25a08632f05
tree                       886c131da9e3c011ed88a6badad401430362b35c

attempt-local JDK          Temurin 21.0.13+9
JDK x64 ZIP sha256         a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9

JVM Product main compile   PASS
keyed localization check   PASS
```

No lived Windows Product effect has been claimed:

```text
Vex Home mutation          NO
model download             NO
model load                 NO
model runtime start        NO
Product service start      NO
Windows native onboarding  NO
Windows package/install    NO
real Vex turn              NO
release publication        NO
```

Therefore the earned boundary is:

```text
SHARED JVM PRODUCT BODY COMPILES ON REAL WINDOWS X64

!= WINDOWS PRODUCT REALIZATION COMPLETE
```

## Attempt chronology and what each attempt taught

### A001 — harness host-observation failure

Stopped before Product source execution.

Cause: an invalid Windows PowerShell use of .NET `RuntimeInformation` static-member access.
A second latent issue was also found: `gradlew.bat` must cross the Windows command-script boundary.

Product meaning learned: none.

### A002 — source carriage reached, Java absent

```text
Windows x64 observation   PASS
exact Product source ZIP  PASS
ComSpec boundary          PASS
Java available            NO
```

This established that the machine did not already carry a build JDK. It did not establish a Product
failure.

### A003 — attempt-local JDK materialization PASS

The exact pinned Temurin JDK downloaded and verified byte-exactly.

The run then stopped because Windows PowerShell 5.1 reclassified Java's ordinary `-version` stderr
banner under `ErrorActionPreference=Stop`.

Product meaning learned: none. JDK materialization itself was proven.

### A004 — broad source proof stopped by human

A004 corrected native-process stream capture, but its source-proof scope remained too broad. The run
was stopped when host memory pressure became disproportionate to the question being asked.

No model or Product service was involved.

### A005 — bounded resource shape worked; Gradle never started

A005 intentionally narrowed the proof:

```text
Gradle max heap      2 GiB
Java child max heap  1 GiB
workers              1
parallel             false
full jvmTest         omitted
model effects        none
```

Observed host pressure was healthy at the stage boundary:

```text
used RAM before check     ~38.5%
free RAM                  ~19.6 GiB
```

The task returned exit 1 because `cmd.exe` looked for `gradlew.bat` from the wrong working
directory. Recovery proved Gradle/Product execution had not started.

### A006 — JVM Product main + localization PASS

A006 changed into the exact extracted Product source root before invoking the Gradle wrapper.

Observed:

```text
A006 START checkVexLifeStrings
A006 PASS  checkVexLifeStrings
```

Current source defines `checkVexLifeStrings` as depending on the JVM main compile task. Therefore the
PASS proves both:

```text
current Product JVM main source compiled on real Windows x64
keyed localization source validated on real Windows x64
```

The run was then stopped before `compileKotlinJs`.

Why Task Manager later showed very high PowerShell memory:

```text
Gradle console stream
-> Tee-Object
-> unconsumed PowerShell success-stream values
-> function call assigned to $step
-> console objects retained by PowerShell
```

That retention happened after the task's memory snapshot. It was harness memory, not Vex/model/server
memory.

No Windows Vex service or model runtime was running.

## What we deliberately do not need to replay

Do not rerun local generic compilation merely to complete the original checklist.

Held/deferred:

```text
compileKotlinJs
full jvmTest
compileTestKotlinJs
```

These may be run off-host later if a source change actually needs them. They are not the next
Windows-specific uncertainty.

## Repository currentness after the baseline

The baseline above was against `655e7c78...`.

Current `main` has since advanced to:

```text
262c208b2e65e828dd867719c29ecd286ae58a80
tree=71f485c0687eab3c6e419fd635e2737a0385d0ae
```

The earlier platform experiment remains useful evidence but is not the current implementation base:

```text
#138 Claude selectable-model draft       head=9a0bcf3b4beb1649e82134d4697cbd74a15937e6
#139 platform foundation                 head=2a1786aed06cc981cc894f99a4b9d54ded01ca55
#215 Windows routing experiment          head=73e2f14009b545d13f7d9cedfe4e097a9bf8b080
```

Current `main` does not contain the #139 portable platform vocabulary or #215 Windows host observer.

Therefore:

```text
USE #215 AS EVIDENCE
!= CONTINUE NEW PRODUCT WORK ON #215'S STALE STACK
```

## Chosen next route and why

### Source/build proof

Generic source compilation should not use Victor's real Windows PC as the default build farm.

For deterministic source-only proof:

- use repository/provider proof when it materially helps;
- standard GitHub-hosted Actions may be used as an optional proof executor for this public repository;
- do not add an Actions ceremony merely because it exists.

### Real Windows Product proof

Use Victor's real Windows host through the Relay/VexLocalBridge human-boundary pattern for facts that
a hosted runner cannot prove:

```text
real host resources
selected/proposed Vex Home
Windows runtime
Windows service lifetime
native onboarding
real model inference
quit/reopen continuity
```

The next Product implementation edge is one **no-effect Windows Readiness / Current State adapter**
grounded on current `main`, not a runtime or installer.

It should answer in ordinary Product meaning:

```text
Host
  Windows/x64
  total memory
  free storage
  accelerator evidence where available

Home
  proposed/selected coordinate
  current Home disposition

Product
  shared JVM body proven
  language source available

Windows realization
  host/current-state adapter
  model runtime availability
  Product service availability
  native setup availability
  package availability

effectsPerformed = false
```

This is the smallest rightful next edge because current `VexLifeProdCurrentState` is already the
no-effect convergence owner, but its default host observation still directly uses
`HereticMacHostProfile`, and the current front door still directly uses
`VexLifeProdMacServiceLifetime`.

## Model policy for later lived proof

Do not use the 27B as the Windows architecture proof.

First prove the Product relationship using the smallest rightful Windows-compatible model/runtime
realization that preserves the intended Vex relationship.

```text
WINDOWS VEXLIFE WORKS
!= THIS MACHINE CAN RUN 27B
```

The current 4B/9B Claude candidates are MLX artifacts and therefore are not automatically Windows
realizations.

## Future real-host run contract

A real-host run must be understandable while it is executing and recoverable if Victor cancels it.

Each material stage should:

1. print ordinary human meaning before it starts;
2. state whether model/runtime/service effects are being performed;
3. write its evidence checkpoint before advancing;
4. allow cancellation without losing already-completed checkpoints;
5. form the final RETURN from those checkpoints rather than making final ZIP formation the only
   durable evidence boundary.

Example:

```text
[1/6] Observe Windows host       PASS -> checkpoint
[2/6] Observe Vex Home           PASS -> checkpoint
[3/6] Resolve runtime plan       PASS -> checkpoint
[4/6] Start runtime              ...
```



## Current real-host state — readiness R002 PASS

Current Product implementation successor remains:

```text
#223 [Platform][Windows][READINESS][DRAFT]
head=9ba98244269897a04535a9d27996b5cafc50e758
tree=65e56fd706d28efcd1ac77191b2161483726b634
base main=262c208b2e65e828dd867719c29ecd286ae58a80
hosted Windows source proof run=44053924414 PASS
```

### R001 — harness automatic-variable SAFE_FAILURE

R001 stopped before checkpoint 1 because PowerShell treated `$host` as built-in `$Host`.
Source review found the same latent collision for `$home` vs `$HOME`.

No Product/Home/model/runtime/service effect occurred.

### R002 — real-host readiness PASS

R002 was first sanity-executed end-to-end on a standard hosted Windows runner:

```text
Actions run 44056068045
conclusion SUCCESS
```

Victor then returned exact real-host evidence:

```text
RETURN sha256          a7168ed7b81673ea4813e16266125a9f06f9003b6aa12eb2330633d6f088b1c4
Windows                Microsoft Windows 11 Pro
target                 WINDOWS / X86_64
CPU                    AMD Ryzen 9 5950X
cores / threads        16 / 32
system RAM             31.9 GiB
free RAM at probe      21.8 GiB
GPU                    NVIDIA GeForce RTX 3080
nvidia-smi VRAM        10240 MiB
C: free                ~1.26 TiB
proposed Home          exists
effectsPerformed       false
```

Generic `Win32_VideoController.AdapterRAM` returned a lower 32-bit-sized value; NVIDIA-specific
`nvidia-smi` is the accelerator VRAM evidence.

The proposed Home path is:

```text
C:\Users\victo\.vexlife
EXISTING_DIRECTORY_NOT_CANONICALLY_CLASSIFIED_BY_PROBE
```

R002 deliberately did not classify or modify that directory.

The exact R002 RETURN is retained under the proof branch. Future public summaries should avoid
copying raw Home/device/Companion identifiers unnecessarily.

## Home contract evidence R003 — PASS, with Product model-authority correction

R003 was read-only and consumed only bounded contract coordinates. Its synthetic-host harness proof
passed first:

```text
Actions run 44057166444
R003 no-effect evidence harness PASS
```

Victor then returned exact R003 evidence. Raw Home/device/Companion identifiers remain private to the
RETURN and are intentionally omitted here.

### What R003 proved

The canonical continuity relationships needed by current VexLife-Prod are present and coherent:

```text
Home schema                 vexlife.home/v0
device installation schema  vexlife.device-installation/v0
bootstrap receipt schema    vexlife.bootstrap-receipt/v0

Home/device/family/Companion scope relationships = coherent
Home continuity                              = PRESERVE
```

R003 also observed two distinct model coordinates:

```text
config/model.json
  predecessor VexLife model configuration
  schema = vexlife.model-configuration/v1
  historical state = BOUND_QUALIFIED

runtime/vexlife-prod/model-binding.json
  current VexLife-Prod Product model-selection authority
  state = ABSENT
```

This distinction changes the runtime plan.

### Predecessor model state is evidence, not current Product authority

The predecessor model record names the older release-qualified Windows Qwen3.5 4B / llama.cpp
relationship. That proves Windows runtime history and is useful as a process/runtime example.

It does **not** mean VexLife-Prod must restore, select or expose that predecessor model.

Current VexLife-Prod model selection is ahead of the predecessor:

```text
#138  selectable 27B / 9B / 4B foundation
#153  + GPT-OSS 20B fourth option
#190  generic catalog extension point
#201  explicit per-model Companion/provider profiles
#204  stale persisted Product model converges to a current offered model
```

#204 captures the relevant currentness law:

```text
historical persisted model
!= hard prerequisite that obsolete model still exist

exact Home / Companion scope
-> current Product offered catalog
-> preserve current selection if still offered
-> otherwise converge through current Product selection law
```

Therefore:

```text
PRESERVE Home / device / Companion continuity
DO NOT make predecessor Qwen3.5 the VexLife-Prod model authority
DO NOT create a Windows-only Setup/Settings model registry
```

The temporary predecessor-runtime observer that was briefly authored on #223 during R003
interpretation was removed after this correction. #223's tree returned to the already-hosted-proved
readiness source.

### Current Windows model-realization requirement

The human-facing/Product model chooser remains one generic path:

```text
HereticModelCatalog
-> Setup
-> Settings → Models
-> Product-scoped Home model selection
-> Companion/provider profile
```

Windows needs a realization behind that path:

```text
selected current Product model
-> platform-specific artifact/runtime realization

Mac      -> current MLX realization
Windows  -> qualified Windows realization
```

Model choice, platform artifact/runtime identity and provider-message representation are distinct.

Current external artifact research confirms Windows-compatible GGUF lineages exist for multiple
current choices:

```text
current 4B Heretic/Abliterated lineage -> publisher GGUF / llama.cpp form exists
current 9B Heretic lineage             -> GGUF forms exist
GPT-OSS 20B Heretic ARA v3 lineage     -> GGUF forms exist
27B Heretic lineage                    -> GGUF forms exist, but is not the patient-zero fit for 10 GiB VRAM
```

These are candidate realizations only. Exact provider revision, member digests, runtime version,
request/response behavior and Product qualification remain owed before any becomes an accepted
Windows Product realization.

R002 hardware still makes the current 4B choice the proportionate patient-zero lived proof target:

```text
RTX 3080 / 10 GiB VRAM
31.9 GiB RAM
current 4B GGUF ~consumer-GPU class

patient-zero explicit current Product choice = 4B is reasonable
architecture = generic catalog path, not a hard-coded 4B branch
```

### Next source edge

Do not run another Home/model effect yet.

The next source problem is to establish one rightful **model-choice -> platform-realization** seam
against the current VexLife-Prod model architecture, without rewriting or overriding its active model
owners.

#201 has been given the Windows realization pressure explicitly. Because #153/#190/#201/#204 are
still draft/currentizing lanes rather than one accepted Product floor, Windows realization source
must not silently fork their ownership. The next implementation should compose with the currentized
model lane when that owner shape is sufficiently stable.

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS][R003][CURRENT-MODEL-AUTHORITY] -->

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS][SOURCE-BASELINE][CURRENT] -->
