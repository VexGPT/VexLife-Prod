# macOS Distribution Lifetime

`[VXG RealForever]`

## Human question

When a person repeatedly opens VexLife DMGs while testing, updating, or recovering VexLife, can the
next VexLife startup retire the old mounted VexLife distributions it no longer needs without killing
unrelated macOS processes, deleting the person's downloads, or confusing distribution cleanup with
Home/model/service ownership?

## Observed need

Repeated lived Mac testing produced many simultaneously mounted VexLife disk images:

~~~text
VexLife DMG A
-> /Volumes/VexLife
-> VexLife.app visible on the Desktop
-> diskimagesiod helper

VexLife DMG B
-> /Volumes/VexLife 1
-> another visible VexLife.app
-> another diskimagesiod helper

...
~~~

A read-only census followed by exact normal detach proved the class concretely:

~~~text
49 stale VexLife mounts
-> 49 exact normal detaches
-> 0 held
-> diskimagesiod population collapsed to the one unrelated OBS Studio image
~~~

Most of the apparent "duplicate VexLife apps" were therefore the `VexLife.app` inside each mounted
distribution volume, not 49 independent installed applications.

This is a Product distribution-lifetime relationship. It is not a reason to add a generic cleanup
manager.

## Owner

~~~text
VexLifeProdMacDistributionLifetime
  owns transient mounted VexLife distribution reconciliation

VexLifeProdPrivateServiceImageRetention
  owns old installed private service images inside Vex Home

VexLifeProdMacServiceLifetime
  owns exact Product service / launchd / listener lifetime

HereticPersistentRuntime
  owns exact model runtime lifetime

VexLifeReleaseInboxFamily
  owns release communication, not installation or cleanup
~~~

Permanent:

~~~text
MOUNTED DISTRIBUTION != INSTALLED PRIVATE SERVICE IMAGE
MOUNTED DISTRIBUTION != PRODUCT SERVICE
MOUNTED DISTRIBUTION != MODEL RUNTIME
MOUNTED DISTRIBUTION != HUMAN-OWNED DMG FILE

APP VISIBLE INSIDE /Volumes/... != INDEPENDENT INSTALLED APP COPY
~~~

## Startup sequence

The ordinary Mac setup application invokes this owner as explicit startup hygiene:

~~~text
VexLife setup application starts
-> exact source/setup launcher already bound
-> language resolves
-> RECONCILE_DISTRIBUTION_LIFETIME
     observe attached disk images
     -> mounted volume contains VexLife.app?
          NO  -> outside this owner
          YES -> re-prove:
                   CFBundleIdentifier = com.vexlife.prod
                   exact VexLifeProdExpectedSourceSha
     -> current setup code executes beneath this mount?
          YES -> retain current execution mount
          NO  -> re-observe image-path + mount relationship
                re-observe exact VexLife application identity
                normal detach only
                verify mount absent
     -> busy / changed / unreadable / unknown
          HOLD that mount; never force it
-> choose Home
-> observe Product-entry State
-> realize only the human-selected lawful Product Effect
-> continue ordinary Home/model/runtime/service/presentation convergence
~~~

Distribution reconciliation is not a prerequisite for Home or Product correctness. If macOS cannot
observe or normally detach one stale distribution, that entry is held and VexLife continues. The
cleanup never upgrades uncertainty into destructive authority.

The pure Product-entry observer remains available separately for diagnostics and deterministic tests.
The human startup path composes the distribution-lifetime effect before that observation because
startup now owns convergence of its own transient distribution resources.

## Exact ownership boundary

A mounted image is in scope only when its mounted root contains an exact source-bound VexLife
application:

~~~text
<mount>/VexLife.app/Contents/Info.plist

CFBundleIdentifier
  = com.vexlife.prod

VexLifeProdExpectedSourceSha
  = one exact 40-character source SHA
~~~

Before detach, the owner re-observes both:

~~~text
same mount point -> same image path
same mounted VexLife.app -> same source identity
~~~

Only then may normal `hdiutil detach` occur. Successful detach is followed by absence
re-observation.

Forbidden:

~~~text
killall diskimagesiod
pkill by process name
force detach
detach by volume name alone
detach another application's image
delete a .dmg from Downloads
delete an arbitrary .app from Desktop/Downloads
treat source-looking filenames as ownership
~~~

## Human-owned files

The source DMG remains the person's file. This owner removes the *mounted realization*, not the
download itself.

Likewise, an ordinary `.app` copied into Desktop, Downloads, Applications, or another folder is not
silently deleted by this owner. A future human-facing storage/review surface may identify exact old
VexLife files and offer removal, but file deletion needs its own human/product relationship and proof.

## Apple distribution trust is separate

This relationship requires no Apple Developer Program membership.

~~~text
AD_HOC / UNSIGNED DISTRIBUTION TRUST
!= DISTRIBUTION LIFETIME OWNERSHIP

DEVELOPER ID / NOTARIZATION
  may improve first-run trust UX later
  does not own stale-mount cleanup
~~~

The current affordable DMG path may therefore gain truthful startup cleanup without reviving the
native WebToApp distribution-trust trajectory or requiring a paid Apple identity.

## Source

~~~text
src/jvmMain/kotlin/vexlife/application/VexLifeProdMacDistributionLifetime.kt
src/jvmTest/kotlin/vexlife/application/VexLifeProdMacDistributionLifetimeTest.kt
src/jvmMain/kotlin/vexlife/setup/VexLifeProdSetupProcess.kt
packaging/private-service-image/setup/vexlife-prod-setup-window.applescript
~~~

The setup process exposes `--reconcile-distribution-lifetime` explicitly. The AppleScript setup
window invokes that named effect during startup. This keeps the relationship source-addressable
rather than burying cleanup in renderer shell code.

## Proof boundary

Deterministic source proof must establish:

- unrelated mounted applications are ignored;
- the distribution currently executing setup is retained;
- another exact VexLife distribution is normally detached;
- image/application identity drift is held;
- a busy/failed detach is held and never forced;
- observation loss does not block Product entry;
- Apple `hdiutil info -plist` structure is parsed without loading its external DTD;
- human startup invokes reconciliation before Home choice and Product-entry observation.

Lived DMG acceptance must then establish on a real Mac:

~~~text
mount old exact VexLife DMG
mount/open new candidate VexLife DMG
-> candidate startup retains its own executing mount
-> old exact VexLife mount disappears
-> unrelated disk image remains
-> Product entry continues
-> current service becomes reachable
-> one real Vex turn still completes
~~~

A lived PASS is about that whole sequence. Source/unit PASS alone does not claim real macOS cleanup or
Product completion.

<!-- [VXG RealForever][VEXLIFE-PROD][MAC-DISTRIBUTION-LIFETIME] -->
