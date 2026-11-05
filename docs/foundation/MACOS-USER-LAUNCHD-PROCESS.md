# macOS user launchd process substrate

`[VXG RealForever]`

## Human question

Can the fresh VexLife product own long-lived macOS user processes through one explicit construction /
lifetime primitive, without making each domain (Heretic, presentation, future workers) copy
`launchctl` behavior or infer process ownership from ports and PIDs?

This foundation ports the already-earned current VexLife relationship from
`src/core/macos-user-launchd-process.mjs` into the fresh JVM body.

## VexArchitecture placement

This is **construction and lifetime**, not product semantics.

~~~text
domain owner
  already decides:
    what process should exist
    why it should exist
    which executable / args / working directory it needs

        ↓ Spec

MacUserLaunchdProcess
  writes one private plist
  bootstraps one user launchd job
  observes exact launchd identity
  returns one receipt
  retires that exact job only

        ↓ Receipt

domain owner
~~~

Permanent:

~~~text
PROCESS SUBSTRATE
!= HERETIC OWNER
!= PRESENTATION OWNER
!= CONVERSATION OWNER
!= VEX HOME OWNER
!= PRODUCT STATE
~~~

## Faithfully ported invariants

~~~text
schema
  vexlife.macos-user-launchd-process/v1

substrate
  substrate.vexlife.macos-user-launchd-process.001

launchd domain
  gui/<numeric uid>

label
  com.vexlife.*

RunAtLoad
  true

KeepAlive
  false

ProcessType
  Background

start
  job must be ABSENT
  -> write private plist
  -> launchctl bootstrap
  -> bounded observation
  -> RUNNING + exact pid
  -> receipt

retire
  re-observe job
  -> UNKNOWN holds
  -> RUNNING pid must equal expected pid
  -> launchctl bootout
  -> require launchd ABSENT
  -> require expected pid not live
  -> remove plist
  -> RETIRED_VERIFIED
~~~

A PID by itself is not process ownership.

A port answering is not process ownership.

A loaded launchd label with the wrong PID is not process ownership.

## JVM realization

The fresh source lives at:

~~~text
src/jvmMain/kotlin/vexlife/platform/macos/MacUserLaunchdProcess.kt
~~~

The production Host uses:

~~~text
/usr/bin/id -u
/bin/launchctl
ProcessHandle liveness
POSIX-private directories/files
~~~

Tests inject a fake Host while using real temporary plist files. This allows the exact state machine
to be proven on non-macOS CI without pretending that such tests prove real macOS launchd behavior.

## Why KeepAlive remains false

The current accepted source deliberately owns **start / observe / retire** rather than granting launchd
an unbounded restart policy.

~~~text
PROCESS DIED
!= AUTOMATICALLY SAFE TO RESTART
~~~

A domain owner must re-observe source/currentness/ownership and decide the next effect. Changing
KeepAlive is therefore an architecture change, not a reliability tweak.

## Proof boundary

Disposable Kotlin/JVM 1.9 formation proof:

~~~text
production source compile             PASS
fake launchd start -> exact receipt   PASS
plist persisted                       PASS
exact retire -> launchd bootout       PASS
verified-not-live                     PASS
plist removed after verified retire   PASS
~~~

Source candidate tests cover:

- plist meaning and environment projection;
- absent-before-start;
- exact receipt after RUNNING;
- refusal to replace an existing launchd job;
- bootstrap cleanup;
- PID drift hold before bootout;
- exact retirement + verified-not-live;
- UNKNOWN never collapsing into absence.

Still required before acceptance:

~~~text
repository-owned Kotlin 2.4.20 JVM compile + jvmTest
real macOS host proof against launchctl
~~~

A Linux/JVM fake-host PASS is construction-state proof, not a lived macOS process proof.

<!-- [VXG RealForever][VEXLIFE-PROD][MACOS-USER-LAUNCHD-PROCESS] -->
