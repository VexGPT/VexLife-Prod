# macOS Product Service Lifetime

[VXG RealForever]

## Human question

Given one exact VexLife-Prod service realization on a Mac, how does Product preserve enough custody
to reuse, rotate, retire or replace that process even when the selected Vex Home disappears, while
never treating "port answered", "PID exists", "launchd label exists", or a killed model process as
sufficient ownership or cleanup authority?

## Current causal relationship

~~~text
desired exact VexLife-Prod source SHA
        |
        v
installed private image verifier
        |
        v
VexLifeProdMacServiceLifetime
        |
        +-> product lifetime receipt
        |
        +-> MacUserLaunchdProcess
        |     generic launchd construction/retirement
        |
        +-> /api/v1/source-currentness
              source SHA
              + exact listener PID
        |
        v
RUNNING_CURRENT
~~~

Three truths remain independent:

~~~text
IMAGE CURRENT
!= PROCESS OWNERSHIP CURRENT
!= HTTP SOURCE/PID CURRENT
~~~

All three must agree before the service may be reused.

## Observed process-leak class and lifetime-placement law

The failure that earned this rule was lived, not hypothetical:

~~~text
Preview A running
-> selected ~/.vexlife deleted
-> v1 service-lifetime receipt deleted with Home
-> com.vexlife.prod.service still independently loaded/alive in launchd
-> model Python process killed separately
-> Product service still existed
-> fresh setup could see a process but could no longer prove exact ownership
-> fail closed: "VexLife will not replace the current process."
~~~

That was a lifetime mismatch between the effect and the evidence that authorized its retirement.

Permanent distinctions:

~~~text
DELETE HOME != RETIRE PRODUCT SERVICE
KILL MODEL PYTHON != RETIRE PRODUCT SERVICE
RUNTIME CHILD EXIT != LAUNCHD SERVICE RETIREMENT

LABEL EXISTS != OWNERSHIP
PID EXISTS != OWNERSHIP
PORT ANSWERS != OWNERSHIP
PROCESS NAME MATCHES != OWNERSHIP

PROCESS MAY OUTLIVE HOME
-> OWNERSHIP / RETIREMENT EVIDENCE MUST OUTLIVE HOME
~~~

Startup is a convergence boundary, not only an observer. If an old/stale Product service is
exactly owned, startup may offer Repair / Update & Restart / Start and the lifetime owner retires
that exact realization before starting the desired one. This is normal recovery, not a process leak.

If the durable receipt is gone but a strict legacy-orphan witness can still prove one VexLife
Product service identity, startup should offer an explicit **Retire old VexLife service & continue**
recovery control. That recovery must be narrower than generic process killing: bind the stable
VexLife launchd label, observed PID when present, accepted VexLife service launch shape, and exact
source-currentness/listener evidence when available; retire only that witnessed VexLife coordinate;
then verify both launchd absence and process death before replacement.

If that witness cannot be established, remain **HELD_UNKNOWN**. The human surface must still be
useful: explain what was found and provide bounded inspection/manual-recovery guidance rather than
a dead-end message. Emergency guidance may show commands such as `launchctl print` for the exact
`com.vexlife.prod.service` coordinate and an explicit `launchctl bootout` recovery action, but it
must never recommend broad `pkill python`, `killall java`, port-based killing, or PID killing without
first showing the exact process being targeted. Manual cleanup is a human-selected fallback, not
automatic ownership inference.

This rule stays with the existing service-lifetime owner. It does not create a universal cleanup
manager, another process registry, or a second lifecycle truth store.

## Stable product process coordinate

~~~text
launchd label:
  com.vexlife.prod.service

legacy v1 lifetime receipt:
  <Home>/runtime/product-service/service-lifetime.json

v2 Mac-user Product-service lifetime receipt:
  ~/Library/Application Support/VexLife/ProductService/service-lifetime.json

plist:
  <Home>/runtime/product-service/launchd/com.vexlife.prod.service.plist

logs:
  <Home>/runtime/product-service/logs/service.out.log
  <Home>/runtime/product-service/logs/service.err.log
~~~

The program is always the verified installed image's:

~~~text
bin/vexlife-prod-service
  --source-sha <exact desired source SHA>
~~~

The current launch contract carries the selected Home explicitly:

~~~text
bin/vexlife-prod-service
  --source-sha <exact desired source SHA>
  --home <exact selected Home path>
~~~

The plist/log/image coordinates may remain Home-scoped because they describe that selected
realization. The v2 ownership receipt moves to the longer Mac-user Product-service lifetime because
it must remain available after Home deletion.

At the time this foundation correction was formed, the v2 realization was under exact
implementation/proof in PR #281. Until that candidate is accepted, current-main executable behavior
must still be read from current source rather than inferred from this architecture rule.

## Observation states

~~~text
ABSENT
  desired image exact
  no product receipt
  stable launchd label absent

RUNNING_CURRENT
  desired image exact
  product lifetime receipt exact
  launchd receipt/spec/plist exact-current
  exact receipt PID live
  HTTP source-currentness:
    exact source SHA
    exact same process PID

OWNED_CURRENT_NOT_READY
  exact current product receipt exists
  but current service is not fully reusable/current

OWNED_PRIOR_SOURCE
  exact product receipt owns a different prior source

HELD_UNKNOWN
  stable launchd label present without product receipt
  OR lifetime receipt unreadable/invalid
  OR launchd observation unknown
  OR observed launchd PID differs from exact receipt
~~~

Unknown never becomes absence or cleanup authority.

## Start / reuse / source rotation

~~~text
RUNNING_CURRENT
  -> reuse
  -> zero process mutation
  -> zero receipt rewrite

ABSENT
  -> start desired image

OWNED_CURRENT_NOT_READY
  -> retire exact owned receipt/process
  -> start desired image

OWNED_PRIOR_SOURCE
  -> retire exact prior receipt/process
  -> start desired image

HELD_UNKNOWN
  -> HOLD
  -> no bootout
  -> no delete
~~~

Permanent:

~~~text
PATH_CLEAR != PROCESS OWNERSHIP

LABEL EXISTS != OWNED PROCESS

PORT ANSWERS != OWNED PROCESS

SOURCE SHA ANSWERS
!= EXACT LISTENER PROCESS

UNKNOWN != ABSENT
~~~

## Source currentness v2

The shared source-currentness wire contract is advanced to:

~~~text
vexlife-prod.local-service-source-currentness/v2
~~~

It adds:

~~~text
processPid
~~~

The local service reports its actual JVM PID through:

~~~text
ProcessHandle.current().pid()
~~~

The lifetime owner requires:

~~~text
HTTP processPid == exact launchd receipt PID
~~~

before source-currentness can satisfy readiness.

This closes the gap where another process on the fixed port could answer the right source SHA while
the launchd-owned process was not actually the listener.

## launchd receipt v2

The shared launchd substrate is advanced to:

~~~text
vexlife.macos-user-launchd-process/v2
~~~

Its durable receipt now includes the canonical launch environment.

Previously:

~~~text
Spec.environment
-> plist
-> process

but

Receipt
  did not retain environment
~~~

Heretic uses a narrow non-secret allowlist:

~~~text
HOME
PATH
TMPDIR
LANG
LC_ALL
USER
HF_HUB_CACHE
~~~

The v2 receipt now binds that environment, and Heretic reconstructs launchd currentness from the exact
receipt rather than replacing it with an empty environment.

## Persisted launchd definition currentness

A reusable current process also requires:

~~~text
launchd receipt valid
+ plist is regular non-symlink file
+ plist bytes exactly equal renderPlist(receipt)
+ launchd reports exact PID
+ exact PID live
~~~

This is exposed by:

~~~text
MacUserLaunchdProcess.receiptCurrent(...)
~~~

Important distinction:

~~~text
PLIST DRIFT
-> NOT CURRENT FOR REUSE

but

DURABLE EXACT RECEIPT
+ SAME LAUNCHD PID
-> may still authorize exact retirement
~~~

Currentness is intentionally stricter than retirement authority.

## KeepAlive policy

The generic substrate remains:

~~~text
KeepAlive = false
~~~

This is intentional.

The first product-lifetime proof must demonstrate:

~~~text
launcher/setup process exits
-> service naturally remains alive
-> source currentness still exact
-> later Send succeeds
~~~

Automatic respawn is not used to hide a crash or turn an unproven service into apparently sustained
behavior.

A future automatic-respawn policy would require its own source/currentness/recovery design.

## Transactional service start

A new service is not considered product-owned merely because launchd bootstrap returned.

The start transaction is:

~~~text
MacUserLaunchdProcess.start()
        ↓
HTTP source-currentness exact source + exact PID
        ↓
MacUserLaunchdProcess.receiptCurrent()
        ↓
form + validate product lifetime receipt
        ↓
atomic durable receipt write
        ↓
STARTED_CURRENT_SERVICE
~~~

If **any** step after launchd start fails:

~~~text
exact newly-started launchd receipt
-> exact retire
-> verify not live
-> do not leave product lifetime receipt
-> surface original startup failure
~~~

If cleanup itself cannot be verified, that cleanup failure is surfaced explicitly rather than
pretending startup merely failed.

This prevents:

~~~text
SERVICE STARTED
+ RECEIPT COMMIT FAILED
-> UNOWNED LIVE PROCESS
~~~

### Bounded cold-start readiness

A launchd bootstrap returning an exact PID does not mean the Product HTTP surface is already listening.
The service process still has to re-admit Home/runtime Session truth before it binds
`127.0.0.1:18210`.

The ordinary readiness window remains 120 seconds. If that window expires while the exact launchd
receipt/PID is still current, the lifetime owner grants one 180-second cold-start grace period:

~~~text
launchd exact PID formed
-> setup progress: Starting VexLife
-> readiness confirmation begins
-> setup progress: Confirming VexLife
-> poll exact HTTP source + exact PID

current within 120 s
  -> continue normally

not current at 120 s
+ exact launchd receipt/PID still current
  -> one bounded grace
  -> hard stop at 300 s total

not current at 120 s
+ exact launchd ownership already lost
  -> no grace
  -> fail / preserve exact cleanup rules

not current at 300 s
  -> fail
  -> exact newly-started process retirement
  -> no lifetime receipt
~~~

The grace is not driven by log activity and is not renewable without bound. VexLogger remains
observation only. Exact launchd ownership is the only predicate that admits the extra wait.

This distinguishes:

~~~text
SLOW EXACT-OWNED COLD START
!= DEAD / REPLACED SERVICE

EXACT PID STILL CURRENT
!= PRODUCT READY

PROGRESS DISPLAY
!= SUCCESS CLAIM
~~~

## Product lifetime receipt

Two receipt generations remain distinguishable.

Legacy v1:

~~~text
schema = vexlife-prod.macos-service-lifetime/v1
coordinate = <Home>/runtime/product-service/service-lifetime.json
~~~

v1 can prove an exact service while the Home still exists. It cannot by itself preserve retirement
custody after that Home is deleted.

v2:

~~~text
schema = vexlife-prod.macos-service-lifetime/v2
coordinate = ~/Library/Application Support/VexLife/ProductService/service-lifetime.json
~~~

v2 binds the original service identity plus the exact Home identity it serves:

~~~text
sourceSha
imagePayloadSha256
imagePath
fixed service origin
exact MacUserLaunchdProcess receipt
homeBinding:
  homePath
  homeRef
  deviceRef
  companionLineageRef
~~~

Path equality never substitutes for Home identity. A fresh Home at the same filesystem path is a
different Home when its canonical identity differs.

A corrupt/unreadable lifetime receipt is preserved and projected as HELD_UNKNOWN. A legacy v1
receipt remains readable under its own accepted launch contract; ordinary service convergence may
re-form the new lifetime evidence only after exact ownership is established.

## Service termination releases the current runtime Session

The Product service owns the current runtime **Session reference**, including after an in-service
model switch. Service termination must therefore release through that current Session rather than
through a runtime capability captured only at initial service startup.

```text
SERVICE START SESSION
-> later model switch
-> CURRENT SESSION CHANGES

therefore

SERVICE TERMINATION
-> close current Product service surface
-> close current runtime Session
-> rightful platform owner releases exact current runtime
-> verify runtime release
-> only then terminal service close
```

Mac and Windows use different concrete mechanics but the same Product lifetime meaning:

```text
Mac Session.close()
-> exact captured Heretic / predecessor-compatibility retirement capability
-> successful release becomes idempotent

Windows Session.close()
-> exact Product-owned Process custody
-> successful release becomes idempotent
```

A failed exact runtime release remains retryable and is recorded as `SERVICE_STOP_FAILED`. The
service process must not claim terminal cleanup merely because the first close attempt failed.

Permanent:

```text
SERVICE PROCESS TERMINATING
!= RUNTIME RELEASE VERIFIED

STARTUP SESSION
!= NECESSARILY CURRENT SESSION

QUIT/CLOSE RETRY
!= DOUBLE RETIREMENT

QUALIFIED LIVE RUNTIME
!= SESSION WITHOUT EXACT TERMINAL RELEASE CUSTODY
```

Session formation therefore binds terminal release custody before admitting the runtime relationship. A Product-owned runtime binds its exact Product receipt-owned retirement capability; a borrowed stable runtime binds the compatibility owner's exact captured retirement capability. If that capability cannot be captured after the runtime was observed, Session formation fails closed rather than admitting a runtime whose later `close()` could become an accidental no-op.

This is the cleanup primitive required by a later explicit human **Quit VexLife** control. Close-window
presentation policy remains separate; this rule applies when the Product service lifetime itself is
being terminated.

## Home loss and sustained-process retirement

The service process snapshots the exact admitted Home identity at startup. A transient filesystem
observation must not instantly tear down a healthy service; the current candidate requires three
consecutive non-current Home observations before sustained loss is confirmed.

After sustained Home loss:

~~~text
stop exposing Product service effects
-> keep exact in-memory/captured retirement capability
-> release the exact runtime through its rightful owner
-> verify retirement
-> only then let the Product service JVM exit
~~~

For a Product-owned Heretic runtime, `HereticPersistentRuntime` retains runtime lifecycle ownership.
For a borrowed stable/predecessor runtime, the compatibility owner supplies a captured release
capability that re-proves predecessor receipt + launchd + PID/process identity. The Product service
does not silently steal that runtime merely because it depends on it.

If exact runtime retirement cannot yet be verified, the service stays alive as a non-listening
custody holder and retries the exact release. Exiting and losing the final retirement capability is
worse than temporarily retaining a non-serving custody process.

The already-orphaned v1 service is a separate recovery case. Recovery is inspection-first: identify
the exact service/launchd/runtime evidence that still exists, retire only a realization whose
ownership or strict legacy-orphan witness can be established, and verify absence before a replacement
is formed. Do not use the new v2 design as retrospective proof that an arbitrary old process is safe
to kill.

## Launch contracts across versions

A lifetime receipt is validated under the launch contract that formed it, never re-derived from
today's contract (`VexLifeProdMacServiceLifetime.LaunchContract`):

| Contract | argv | Admitted where |
|---|---|---|
| `SourceAndHome` (current) | `--source-sha <sha> --home <home>` | any selected Home |
| `SourceOnlyDefaultHome` (before c170faa) | `--source-sha <sha>` | only `~/.vexlife`, the Home such a service served implicitly |

~~~text
LEGACY EXACT-OWNED PRODUCT != FOREIGN PROCESS
  prove ownership exactly under the receipt's own contract (receipt, plist bytes, label, PID,
  endpoint source-currentness)
  -> owned prior Product -> UPDATE_AVAILABLE -> retire only that exact service
  -> start the desired source under the current contract -> re-observe

A RECEIPT MATCHING NO ACCEPTED CONTRACT STAYS HELD
A PRIOR-CONTRACT SERVICE IS NEVER REUSED AS CURRENT, EVEN AT THE SAME SOURCE

OWNED CURRENT RECEIPT + LAUNCHD ABSENT = STOPPED, NOT GARBAGE -> START_REQUIRED
~~~

Lived origin: on a certified Mac the installed 8faf58e8 service was coherent in every observed
fact, yet the current owner rebuilt today's argv from its receipt, failed the spec check and held
it as unknown, so Update & Restart was never offered. When the launch specification changes
again, add a contract here rather than changing what an old receipt is expected to say.

## Source-formed proof

Current tests form proof for:

~~~text
no receipt + absent label -> ABSENT

no receipt + present label -> HELD_UNKNOWN / no mutation

ABSENT
  -> launchd start
  -> exact source/PID readiness
  -> exact lifetime receipt

RUNNING_CURRENT
  -> reuse
  -> no bootstrap / bootout / receipt rewrite

desired source rotation
  -> exact prior retirement
  -> desired image start
  -> new exact receipt

launchd PID mismatch
  -> HELD_UNKNOWN
  -> no bootout

HTTP source-currentness wrong PID
  -> not reusable
  -> exact current process restarted

same source + tampered plist
  -> not reusable
  -> exact current process retired/restarted

startup readiness timeout
  -> newly started exact process retired
  -> no lifetime receipt

corrupt lifetime receipt
  -> HELD_UNKNOWN
  -> evidence preserved

lifetime receipt write failure after process start
  -> newly started exact process retired
  -> no unowned live process

launchd receipt environment survives formation
plist drift fails receiptCurrent
exact retirement remains available from durable receipt
~~~

## Held before acceptance

The Home-loss correction is not complete merely because source describes the right ownership model.
Completion requires exact-head source proof plus the lived regression that originally failed:

~~~text
#281 exact source candidate
-> compile/test/string proof PASS
-> startup inspects and retires the already-orphaned v1 Preview-A service, or offers bounded manual recovery if exact legacy recovery cannot be established
-> install/run the corrected Product
-> establish exact Home + Product service + runtime
-> delete that Home while Product is live
-> Product service stops exposing 127.0.0.1:18210
-> exact runtime retirement is verified
-> com.vexlife.prod.service is absent/not-live as required
-> start VexLife again at the same path
-> form a fresh canonical Home identity
-> no "VexLife will not replace the current process" dead end
~~~

~~~text
#25 -> #31 accepted/proven predecessors
exact repository Kotlin 2.4.20 / Gradle proof
real macOS arm64 launchd proof

real installed private image
real service start on fixed 18210
real process PID == source-currentness PID
setup/bootstrap exits
service remains alive
later Conversation Send succeeds
source rotation on real host

native setup window
provider archive/release bootstrap
VexWeb generated presentation
ordinary fresh-user product entry
~~~

## VexBoard projectability

This owner creates a clean Depth path for later projection:

~~~text
verified private image
-> VexLifeProdMacServiceLifetime
-> MacUserLaunchdProcess
-> launchd
-> exact service JVM PID
-> HTTP source-currentness
-> service API
~~~

It remains construction/lifetime meaning, not a semantic Family.

<!-- [VXG RealForever][VEXLIFE-PROD][MAC-SERVICE-LIFETIME] -->
