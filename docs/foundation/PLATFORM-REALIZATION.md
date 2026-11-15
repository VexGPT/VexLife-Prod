# Platform Realization — source-owned target composition

`[VXG RealForever]`

Status: **FOUNDATION CANDIDATE — composition only, no Product consumer migrated**

## Human question

Can VexLife add another supported operating-system / CPU realization without making every downloader,
runtime, service, setup surface and UI independently decide what "this platform" means?

Victor's working intuition was an `IPlatformDependencyWrapper`: one interface-shaped place that lets
the Product pull the right implementation family from current configuration/host context.

The exact class name is not the foundation. The durable relationship is:

~~~text
fresh HostObservation
+ current Product/model selection
+ source-owned qualified target realizations
        ↓
exact target resolution
        ↓
one realization composition
        ↓
existing rightful owners receive only their own coordinate
~~~

This first source slice intentionally stops before consumer migration so the relationship can be
reviewed against current source before it gains runtime consequences.

## Why this is earned now

Until now VexLife-Prod had one actual Product target, so platform choice could remain implicit.

The #138 source line now exposes a repeated Apple-silicon composition across several owners:

~~~text
HereticMacHostProfile
  -> darwin / arm64 host admission

HereticProductIdentity
  -> Mac-specific runtime adapter + host-profile refs

ManagedPython
  -> uv-aarch64-apple-darwin

HereticPythonEnvironment
  -> Python 3.14.7 + mlx-lm 0.32.0

MacHereticRuntimePlatform
  -> physical MLX/process realization

MacUserLaunchdProcess
  -> macOS process substrate

VexLifeProdPrivateServiceImage
  -> darwin-arm64 Java/service image

VexLifeProdMacFrontDoor / VexLifeProdCurrentState
  -> Mac presentation/current-state consumers
~~~

Windows, Intel Mac, Android official release and iOS pathways now make the implicit composition a
real repeated engineering question.

## First source shape

Portable vocabulary:

~~~text
VexLifePlatformRealization
  PlatformFamily
  Architecture
  Target
  DependencyRefs
  Entry
  Catalog
  Resolution
~~~

Current Product catalog:

~~~text
VexLifeProdPlatformRealizations
  MacAppleSilicon
    target = MACOS / ARM64
    hostExecutionProfileRef -> HereticProductIdentity current owner
    modelRuntimeAdapterRef  -> HereticProductIdentity current owner
    runtimeClass            -> current MLX class
    transportClass          -> current MLX transport
    processSubstrateRef     -> MacUserLaunchdProcess current owner
    privateServiceImagePlatform -> darwin-arm64
~~~

The entry deliberately **references** current owners. It does not copy their host rules, process
commands, artifact hashes, runtime liveness, effect law or model bytes.

~~~text
REALIZATION MAP
!= SECOND IMPLEMENTATION DATABASE
~~~

## Exact-match law

The first resolver is intentionally strict:

~~~text
MACOS / ARM64
-> current Apple-silicon realization

MACOS / X86_64
-> UNSUPPORTED until an Intel realization actually exists

WINDOWS / X86_64
-> UNSUPPORTED until a Windows realization actually exists
~~~

There is no nearest-target fallback.

~~~text
CONFIGURATION VALUE
!= SUPPORT EXISTS

ARM64 REALIZATION
!= INTEL REALIZATION

EMULATION POSSIBILITY
!= QUALIFIED PRODUCT TARGET
~~~

## What this candidate does not yet model

Do not add placeholders merely to make the object look complete.

Still unresolved / separately owned:

- public `VexLife.app` bundle identity and DMG distribution profile;
- minimum supported macOS version;
- Developer ID / notarization realization;
- `SMAppService` versus the current direct launchd substrate;
- whether managed Python / MLX executable runtime is bundled or exactly acquired after install;
- distribution-qualified Vision helper/runtime bytes;
- app-contained VexWeb / WKWebView manifestation;
- platform permission adapters;
- Windows runtime/backend selection;
- Android/iOS application distribution.

These are the next questions the Official Application Project is intended to pressure. They are not
filled with guessed refs in this source candidate.

## Current Product seams for the next worker to inspect

The first consumer migration should be chosen because it proves the abstraction, not because a file
contains "Mac" in its name.

Strong current pressure points:

~~~text
VexLifeProdCurrentState
  directly calls HereticMacHostProfile.observeCurrent()

VexLifeProdLocalService
  Models projection directly calls HereticMacHostProfile.observeCurrent()

VexLifeProdMacFrontDoor
  composes Mac host + Mac service lifetime into portable Entry State

VexLifeProdSetupProcess
  directly delegates to VexLifeProdMacServiceLifetime

VexLifeProdLocalSetup
  composes the current model/runtime/materialization path

HereticProductIdentity
  currently carries Mac-specific runtimeAdapterRef + hostExecutionProfileRef
~~~

Recommended first question for Claude:

> Which one or two direct target-selection call sites can consume the realization map without moving
> the actual host/runtime/process truth out of its existing owner?

A successful refactor should make another target mostly additive:

~~~text
new exact target realization
+ its rightful implementation owners
+ proof
-> existing Product relationships can resolve it

not

new target
-> platform conditionals scattered across every Product owner
~~~

## Build-time and runtime remain different

The same target vocabulary may later inform two moments:

~~~text
BUILD TIME
target -> which compiled/native/runtime/application bytes form this release?

RUNTIME
fresh observed host + selected Product/model -> which supported realization is usable now?
~~~

This candidate only source-places the composition vocabulary. It does not make a runtime
configuration capable of changing already-compiled machine code.

## Source basis

Exact formation base:

~~~text
VexGPT/VexLife-Prod PR #138
head ef56b26d5a323c03a8e969a642ab3296b3d50360
~~~

Cross-project implementation map:

~~~text
VexGPT/VexWork PR #304
projects/vexlife-official-application/MACOS-APPLE-SILICON-OFFICIAL-APP-HANDOFF.md
~~~

That VexWork source is work/navigation context. This repository remains the owner of Product
implementation meaning.

## Proof boundary

This foundation can prove:

- one portable typed target/composition vocabulary exists;
- one exact Apple-silicon entry points at the current source owners;
- target resolution is exact;
- Mac Intel and Windows x64 do not silently borrow the Mac arm64 implementation.

It does **not** prove:

- any Product consumer now uses this catalog;
- a DMG/app exists;
- Apple Developer signing/notarization;
- Vision distribution;
- Windows/Intel/mobile support;
- an official application release.

<!-- [VXG RealForever][VEXLIFE-PROD][PLATFORM-REALIZATION][FOUNDATION] -->
