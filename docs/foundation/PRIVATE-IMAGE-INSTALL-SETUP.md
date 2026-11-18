# Private Image Install + Bundled Setup Process

[VXG RealForever]

## Human question

After a source-bound private service image has been delivered to temporary staging, how can Product
entry preserve two distinct human choices: first form the canonical Vex Home identity, then separately
authorize model/runtime acquisition and exact Product installation?

## Causal sequence

~~~text
temporary extracted private image
        ↓
native Product-entry window
        ↓
human chooses Vex Home coordinate
  default suggestion = ~/.vexlife
  alternate absolute location allowed
        ↓
observe/classify that exact selected Home
        ↓
DisplayState -> human Control
        ↓
bin/vexlife-prod-setup
  --activate-front-door-control <Control>
  --home <selected absolute Home>
  --expected-source-sha <sha>
        ↓
VexLifeProdMacFrontDoorProcess
  -> fresh Product-entry observation
  -> VexLifeProdEntryFamily(State + Control)
  -> typed Entry Effect stays inside Product
        ↓
HOME_FORMATION_REQUIRED + Create Vex Home
  -> VexLifeProdSetupProcess.realizeHomeFormation(...)
  -> verify exact source-bound image
  -> VexLifeProdLocalSetup.formFreshHome()
  -> canonical Home/device/Companion identity only
  -> re-observe the selected Home / service truth
  -> REFRESH_ENTRY
  -> render whatever State is actually current
        ↓
native window refreshes DisplayState + resource plan
        ↓
human: Set Up VexLife
        ↓
CONVERGE_PRODUCT(allowAcquisition=true)
  -> VexLifeProdSetupProcess.realizeProductConvergence(...)
  -> VexLifeProdLocalSetup.establishSession(true)
  -> model/Python/runtime prerequisites
  -> VexLifeProdPrivateServiceImageInstaller.install(...)
  -> VexLifeProdMacServiceLifetime.startOrReuse(...)
  -> re-observe the selected Home / Product-service truth
  -> if CURRENT: OPEN_PRODUCT
  -> otherwise: REFRESH_ENTRY and render the actual State
~~~

## Image contract v3

This slice advances the strict private-image schema to:

~~~text
vexlife-prod.private-service-image/v3
~~~

v3 binds the two private JVM launchers plus the source-owned native setup-window projection:

~~~text
bin/vexlife-prod-service
bin/vexlife-prod-setup
setup/vexlife-prod-setup-window.applescript
~~~

The JVM launchers use the bundled Java runtime and exact image classpath. The AppKit source is carried
inside the same payload fingerprint and may only invoke the exact bundled setup launcher selected by
the bootstrap.

An older image cannot silently masquerade as v3 because the strict manifest shape, payload fingerprint
and schema version change together.

## Setup process inputs

Home-only formation:

~~~text
--form-home
--home <selected absolute Home>
--expected-source-sha <40-char lowercase Git SHA>
~~~

Product acquisition/setup after Home exists:

~~~text
--home <selected absolute Home>
--expected-source-sha <40-char lowercase Git SHA>
--allow-acquisition
~~~

Reuse/update/repair convergence:

~~~text
--home <selected absolute Home>
--expected-source-sha <40-char lowercase Git SHA>
--reuse-existing
~~~

No arguments exist for:

~~~text
install path
model
endpoint
runtime port
service port
Python path
artifact source
~~~

The native journey supplies the human-selected Home coordinate explicitly. The image derives its own
root; the installer derives the final image coordinate beneath that selected Home.

## Human authority boundary

Identity formation and model/runtime acquisition are intentionally separate:

~~~text
FORM_CANONICAL_HOME
= HOME / DEVICE / COMPANION IDENTITY EFFECT ONLY

CONVERGE_PRODUCT(allowAcquisition=true)
= MODEL / RUNTIME / PRODUCT SETUP AUTHORITY

CONVERGE_PRODUCT(allowAcquisition=false, allowRuntimeStart=true)
= START / UPDATE / REPAIR WITHOUT NEW ACQUISITION
  (may start the exact local runtime from prerequisites already present)

SERVICE PROCESS
= REUSE-ONLY / allowAcquisition=false / allowRuntimeStart=false
~~~

Runtime start is not acquisition: after a Mac restart the owned receipt survives but neither the
service nor the local runtime is running. Treating start as acquisition made that ordinary return
path fail. See `SETUP-JOURNEY.md` for the held/start States that now precede every Control.

A model/runtime acquisition flag cannot create a missing Home. The Product-entry adapter must first
delegate the Home-only effect to its concrete setup owner, re-observe current truth, return only a
manifestation instruction, and let the native window refresh the source-owned setup resource plan
before a separate human Set Up choice.

The setup process itself does not decide whether individual artifacts are missing; LocalSetup's
rightful owners still classify/reuse/acquire exact prerequisites.

## Install coordinate

Only after Home has been established:

~~~text
<Home>/runtime/product-service/images/<sourceSha>
~~~

The installer requires VexLifeProdHomeFiles.observeExisting(...) to succeed first.

~~~text
REAL DIRECTORY
!= VEXLIFE-PROD HOME
~~~

No arbitrary caller directory can become install authority.

## Install dispositions

~~~text
target absent
  -> copy verified image into owned staging
  -> verify staging
  -> atomic move
  -> verify installed image
  -> INSTALLED_VERIFIED_PRIVATE_SERVICE_IMAGE

target exact-current
  -> REUSED_VERIFIED_INSTALLED_IMAGE
  -> no mutation

setup already executing from exact installed target
  -> REUSED_ALREADY_INSTALLED_EXACT_IMAGE
  -> no mutation

target exists but invalid
  -> HOLD
  -> no delete
  -> no overwrite
~~~

Permanent:

~~~text
INSTALL AUTHORITY
!= REPAIR AUTHORITY

INVALID EXISTING IMAGE
!= DELETE PERMISSION
~~~

## Install serialization

One domain-owned lock coordinate:

~~~text
<Home>/runtime/product-service/.image-install.lock
~~~

uses the shared VexExclusiveFileLease construction mechanic.

~~~text
LOCK MECHANIC
!= INSTALL SEMANTICS
~~~

## Bounded installed dependency retention

Each source-bound private image remains self-contained:

~~~text
<source image>
  runtime/bin/java
  runtime/lib/modules
  lib/vexlife-prod.jar
  lib/<exact runtime dependency JARs>
~~~

That exactness is useful for source verification and recovery, but retaining every historical source
image forever would make ordinary Preview/update use accumulate duplicate Java/runtime/JAR bytes.

The first optimization is therefore **retention**, not a new shared-runtime topology.

After setup has already proved:

~~~text
new source image exact
-> installed image exact
-> Product service exact-current on that same source + payload
~~~

the setup composition invokes `VexLifeProdPrivateServiceImageRetention`.

The retention owner:

~~~text
derive the owned image base from the current installed-image coordinate
-> re-prove the current image through VexLifeProdPrivateServiceImage.observe
-> enumerate only direct installed-image entries
-> current source image: retain
-> non-source / symlink / unknown entry: HOLD
-> older source-shaped directory:
     independently verify its manifest
     + runtime module requirements
     + dependency JAR byte counts / SHA-256
     + bundled Java executable
     + whole payload fingerprint
   -> only if exact: reclaim it with no-follow deletion
   -> if invalid/unreadable: HOLD, do not convert uncertainty into delete authority
-> re-prove the current image after reclamation
~~~

Permanent:

~~~text
SOURCE ROTATION SUCCEEDED
!= DELETE EVERYTHING OLD

DIRECTORY NAME LOOKS LIKE SHA
!= VERIFIED RETIRED IMAGE

INVALID OLD IMAGE
!= DELETE AUTHORITY

CURRENT IMAGE VERIFIED BEFORE CLEANUP
+ CURRENT SERVICE VERIFIED BEFORE CLEANUP
-> VERIFIED RETIRED IMAGE MAY BE RECLAIMED
~~~

This keeps installed Product dependency storage bounded to the current exact image plus any
unknown/invalid evidence that must be held for explicit recovery.

The Product does **not** delete DMGs from the person's Downloads folder; those are human-owned
download artifacts. It also does not touch model artifacts or the Heretic runtime, which have their
own owners/lifetimes.

### Why Java is not globally shared yet

The repeated macOS trust dialog observed while testing Preview updates comes from each downloaded
application carrying a newly downloaded source-bound private image whose setup launcher executes that
image's bundled `runtime/bin/java`.

This retention correction reduces persistent installed-image bloat, but it intentionally does not
turn Java into one floating global prerequisite.

A future shared/content-addressed Java substrate would need to earn and prove at least:

~~~text
exact Java major/runtime identity
required jlink module-set compatibility for the requesting Product source
provider/security module availability
source-image dependency manifest binding
install/currentness/retirement ownership
fresh-user fallback when no shared runtime exists
macOS trust/quarantine behavior
cross-version rollback/recovery semantics
~~~

Until those predicates have a real consumer/proof, keeping one exact self-contained current image
and reclaiming verified retired images is the smaller truthful solution.

## Setup readiness

Machine-readable setup output is emitted only after:

~~~text
source image verified
Home/local setup complete
private image installed/reused exact
exact macOS product service source/PID current
~~~

The setup process delegates service start/reuse to `VexLifeProdMacServiceLifetime` before readiness,
but still does not own that lifetime.

~~~text
SETUP COMPOSITION
-> calls exact service-lifetime owner

SETUP COMPOSITION
!= LAUNCHD / PROCESS CURRENTNESS OWNER
~~~

It still does not open the final VexLife presentation, resolve a mutable "latest" release, or download
its own private image. Those remain separate boundaries.

## Source-formed proof

Tests currently form:

~~~text
Home-only arguments carry form-home + selected Home + expected source
Home-only effect verifies its source image and stops before model/runtime/Product effects
native entry re-observes after Home formation
setup resource consequences are projected before acquisition authority
acquisition setup receives acquisition=true only after Home exists
install follows Home establishment
readiness is last

missing image installs and verifies
exact installed image reuses with zero mutation
invalid installed image is preserved/held
arbitrary directory cannot masquerade as Home

private image v3 verifies service + setup launchers + native setup-window source
service currentness precedes retired-image reclamation
retired-image reclamation precedes SETUP_READY
retention re-proves current image before and after cleanup
valid retired image is reclaimed
invalid/unknown image is held and preserved
~~~

## Held

~~~text
exact Gradle/Kotlin proof
real private image v3 build
real archive/bootstrap generation + extraction
real native AppKit setup window
real setup process from extracted image
real 15GB artifact acquisition/reuse
real install into Product Home
real launchd service currentness after setup
provider release publication
presentation
ordinary human entry
~~~

## Bootstrap consequence

The pre-JVM bootstrap is now source-formed as **carriage only**.

~~~text
source-owned release contract
+ exact archive/source identity
        ↓
archiveVexLifeProdServiceImage
        ↓
exact digest-bound bootstrap .command
        ↓
GitHub Release asset URL for exact source tag
        ↓
download exact archive
        ↓
verify embedded archive SHA-256
        ↓
extract into private temporary staging
        ↓
verify extracted manifest binds same source
        ↓
compile/open packaged AppKit setup window
        ↓
window emits only the selected Product-entry Control
        ↓
Mac Product-entry adapter resolves the typed Effect
        ↓
concrete Home/setup owner realizes only that Effect
        ↓
fresh Product-entry re-observation
        ↓
window receives only REFRESH_ENTRY / OPEN_PRODUCT / NO_ACTION
        ↓
separate explicit acquisition consent remains a later Control
~~~

The bootstrap does not:

~~~text
pre-create Product Home
choose install paths
run model commands directly
write Home manifests
own Heretic runtime
own service launchd
invent presentation state
~~~

Release naming is owned by:

~~~text
packaging/private-service-image/release-contract.json
~~~

The archive task consumes that contract and the bootstrap template mechanically. A release-contract
field that is not source-valid therefore fails formation rather than becoming remembered provider
convention.

Each exact release still contains a source-specific bootstrap pinned to one source SHA and one archive
SHA-256.

The release contract now also defines a stable provider-current alias:

~~~text
currentChannel:
  github-release-latest

asset:
  vexlife-prod-bootstrap-darwin-arm64.command
~~~

Archive formation requires that alias to be a byte-exact copy of the source-specific bootstrap and
binds both names into archive receipt v3. Therefore:

~~~text
STABLE CURRENT URL
-> provider chooses one admitted latest Release
-> stable bootstrap asset name
-> exact bootstrap bytes
-> embedded exact source SHA + archive SHA-256

CURRENT POINTER
!= CURRENT TOPOLOGY
!= MUTABLE BOOTSTRAP LOGIC
~~~

Provider publication is still held: this source forms the contract/assets but does not claim that a
GitHub Release currently exists or is correctly marked latest.

That keeps carriage replaceable, currentizable and small.

<!-- [VXG RealForever][VEXLIFE-PROD][PRIVATE-IMAGE-INSTALL-SETUP] -->
