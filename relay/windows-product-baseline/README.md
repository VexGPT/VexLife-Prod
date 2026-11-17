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



## Current real-host successor — readiness R002

Current Product implementation successor remains:

```text
#223 [Platform][Windows][READINESS][DRAFT]
head=9ba98244269897a04535a9d27996b5cafc50e758
tree=65e56fd706d28efcd1ac77191b2161483726b634
base main=262c208b2e65e828dd867719c29ecd286ae58a80
hosted Windows source proof run=44053924414 PASS
```

### R001 — harness automatic-variable SAFE_FAILURE

Victor's first readiness run stopped at stage 1 before any checkpoint formed:

```text
Cannot overwrite variable Host because it is read-only or constant.
```

Cause:

```text
PowerShell variables are case-insensitive.

$host == $Host
$home == $HOME
```

R001 therefore learned no host/Product truth. It performed no Product/Home/model/runtime/service
effect.

### R002 — corrected checkpointed readiness harness

R002 renames the semantic records:

```text
$hostObservation
$homeObservation
```

and preserves the same no-effect four-stage route:

```text
[1/4] Windows OS / native architecture / CPU / physical memory
[2/4] proposed Home path existence + free storage on its volume
[3/4] graphics / NVIDIA evidence when already present
[4/4] no-effect readiness summary against #223
```

The script was sanity-executed end-to-end on a standard GitHub-hosted Windows runner before Victor
replay:

```text
Actions run 44056068045
job harness
Execute no-effect readiness harness PASS
conclusion SUCCESS
```

The temporary hosted workflow was removed after PASS; the run remains durable evidence.

Source-managed R002:

```text
relay/windows-product-baseline/readiness/R002/
  RUN-VEXLIFE-WINDOWS-READINESS.ps1
  RECOVER-VEXLIFE-WINDOWS-READINESS.ps1
  manifest.json
```

R002 does not compile Product source and performs no model, runtime, service, install, Home or release
effect.

Every completed stage is written to Downloads before the next stage begins. If Victor cancels before
the final RETURN ZIP is formed, the checkpoint directory intentionally remains.

Recovery packages the latest retained R002 checkpoint directory and explicitly performs no replay.

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS][READINESS-R002] -->

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS][SOURCE-BASELINE][CURRENT] -->
