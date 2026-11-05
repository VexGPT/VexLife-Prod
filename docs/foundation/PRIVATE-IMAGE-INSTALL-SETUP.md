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

## Image contract v2

This slice advances the strict private-image schema to:

~~~text
vexlife-prod.private-service-image/v2
~~~

v2 binds two private launchers:

~~~text
bin/vexlife-prod-service
bin/vexlife-prod-setup
~~~

Both use the bundled Java runtime and exact image classpath.

A v1 image cannot silently masquerade as v2 because the strict manifest shape and schema version
change together.

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
~~~

The setup process still does not:

~~~text
start long-lived service
own launchd
open presentation
resolve current release
download its own private image
~~~

Those remain later owners.

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

private image v2 verifies both service + setup launchers
~~~

## Held

~~~text
exact Gradle/Kotlin proof
real private image v2 build
real archive extraction
real setup process from extracted image
real 15GB artifact acquisition/reuse
real install into Product Home
provider release publication / bootstrap carriage
launchd service lifetime
presentation
ordinary human entry
~~~

## Bootstrap consequence

The eventual pre-JVM bootstrap only needs to own carriage:

~~~text
resolve desired exact release source
download exact archive/receipt
verify archive bytes
extract to temporary staging
invoke staged bin/vexlife-prod-setup with explicit human authorization
~~~

It should not:

~~~text
pre-create Product Home
choose install paths
run model commands directly
write Home manifests
own service launchd
~~~

That keeps the bootstrap replaceable and small.

<!-- [VXG RealForever][VEXLIFE-PROD][PRIVATE-IMAGE-INSTALL-SETUP] -->
