# macOS Product Service Lifetime

[VXG RealForever]

## Human question

Given one established VexLife-Prod Home and one exact installed private service image, how does the
product own exactly one current macOS user service process across reuse, source rotation and failed
startup without treating "port answered" or "launchd label exists" as sufficient currentness?

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

## Stable product process coordinate

~~~text
launchd label:
  com.vexlife.prod.service

lifetime receipt:
  <Home>/runtime/product-service/service-lifetime.json

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

No model, endpoint, Home or port values are launchd parameters.

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

## Product lifetime receipt

Schema:

~~~text
vexlife-prod.macos-service-lifetime/v1
~~~

It binds:

~~~text
sourceSha
imagePayloadSha256
imagePath
fixed service origin
exact MacUserLaunchdProcess v2 receipt
~~~

A corrupt/unreadable lifetime receipt is preserved and projected as HELD_UNKNOWN.

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
