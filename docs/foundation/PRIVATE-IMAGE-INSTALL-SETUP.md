# Private Image Install + Bundled Setup Process

[VXG RealForever]

## Human question

After a source-bound private service image has been delivered to temporary staging, how can one
explicitly-authorized first-run setup establish VexLife-Prod Home + Heretic and install that exact
image into its rightful Home without making shell bootstrap the product installer?

## Causal sequence

~~~text
temporary extracted private image
        ↓
bin/vexlife-prod-setup
        ↓
VexLifeProdSetupProcess
        |
        +-> derive own image root from packaged vexlife-prod.jar
        |
        +-> VexLifeProdPrivateServiceImage.observe(expected source)
        |
        +-> VexLifeProdLocalSetup.establishSession(allowAcquisition=true)
        |     -> Home
        |     -> model/Python/runtime
        |
        +-> VexLifeProdPrivateServiceImageInstaller.install(...)
              -> established Home proof
              -> runtime/product-service/images/<sourceSha>
              -> copy to owned staging
              -> verify staged payload
              -> atomic publish
              -> verify installed payload
        |
        v
VEXLIFE_PROD_SETUP_READY
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

## Setup process input

Exactly:

~~~text
--expected-source-sha <40-char lowercase Git SHA>
--allow-acquisition
~~~

No arguments exist for:

~~~text
Home path
install path
model
endpoint
runtime port
service port
Python path
artifact source
~~~

The image derives its own root; Home derives its own default coordinate; the installer derives the
final image coordinate.

## Human acquisition boundary

The setup process refuses to run without the explicit acquisition flag.

~~~text
SETUP PROCESS
= HUMAN-AUTHORIZED ACQUISITION CAPABILITY

SERVICE PROCESS
= REUSE-ONLY / allowAcquisition=false
~~~

Those are intentionally different process roles.

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
setup arguments expose only expected source + explicit acquisition
image verification precedes setup
setup receives acquisition=true
install follows Home establishment
readiness is last

missing image installs and verifies
exact installed image reuses with zero mutation
invalid installed image is preserved/held
arbitrary directory cannot masquerade as Home

private image v3 verifies service + setup launchers + native setup-window source
service currentness precedes SETUP_READY
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
window carries explicit human acquisition consent
        ↓
bundled setup process
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
