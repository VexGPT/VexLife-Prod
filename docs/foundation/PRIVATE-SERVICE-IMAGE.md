# Private macOS Service Image

[VXG RealForever]

## Human question

How can a fresh supported Mac run the source-bound VexLife-Prod service without requiring a
preinstalled Java/Kotlin/Gradle development environment and without creating another visible product
application?

## Relationship

~~~text
clean VexLife-Prod Git source @ exact HEAD
        |
        v
Gradle JVM compilation + repository runtimeDependencyFiles
        |
        v
jdeps
  derives required JDK modules
        |
        v
jlink / JDK 21
  private darwin-arm64 runtime image
        |
        +-> exact product JAR
        +-> exact runtime dependency JARs
        +-> private service launcher
        |
        v
service-image.json
  source SHA
  module set
  artifact hashes / byte counts
  whole payload fingerprint
        |
        v
VexLifeProdPrivateServiceImage.observe(...)
  independent runtime verification
        |
        v
later macOS product-lifetime owner
~~~

Packaging is construction/lifetime source. It does not own product semantics, Conversation, Heretic
meaning, or human presentation.

## Why a private image rather than a second .app

The current product still intends one ordinary human-facing VexLife-Prod entry.

Therefore this foundation does **not** establish a visible "VexLife-Prod Service.app" as product
precedent.

The private image is intended to live under product-owned local storage and later be executed by the
macOS service lifetime owner.

~~~text
PRIVATE SERVICE IMAGE
!= HUMAN APPLICATION

PRIVATE SERVICE IMAGE
!= PRESENTATION

PRIVATE SERVICE IMAGE
!= INSTALLER
~~~

## Build route

Source task:

~~~text
./gradlew assembleVexLifeProdServiceImage
~~~

The task accepts **no source-SHA argument**.

Source identity is observed:

~~~text
git rev-parse HEAD
+
clean git status
        ↓
source admission
        ↓
JVM compilation
        ↓
same HEAD + clean status rechecked
        ↓
package manifest sourceSha
~~~

Permanent:

~~~text
CALLER-SUPPLIED SOURCE SHA
!= SOURCE CURRENTNESS
~~~

The build fails if source changes between admission and image formation.

Build outputs remain under:

~~~text
build/vexlife-prod-service-image/images/<exact-source-sha>/
~~~

Ordinary repository compile/test does not require any packaging property.

## Build-host contract

The private runtime is platform-specific.

Current product host:

~~~text
macOS
arm64 / Apple Silicon
JDK 21 build tools
~~~

The packaging task therefore fails closed unless it is running on macOS arm64 and can resolve one
exact JDK 21 toolchain with executable `jdeps` + `jlink`.

The Gradle launcher/build JVM is a separate construction concern and does **not** need to be Java 21.
The service-image task asks Gradle's Java toolchain owner for JDK 21 and uses that installation's tools.
If no matching JDK is already installed, the settings-level Foojay resolver may provision one into
Gradle-managed toolchain storage. That provisioned JDK remains build tooling; it does not become product
runtime state or a fresh-user prerequisite.

The fresh **user** does not need JDK 21. The resulting private image contains its own custom Java 21
runtime.

## Dependency formation

Kotlin Multiplatform's JVM main compilation owns the real runtime dependency files.

The image builder consumes:

~~~text
jvmJar
+
jvm main runtimeDependencyFiles
~~~

It does not name Kotlin stdlib or kotlinx-serialization JARs by memory.

~~~text
DEPENDENCY GRAPH
-> BUILD INPUTS

BUILD SCRIPT
!= SECOND DEPENDENCY REGISTRY
~~~

`jdeps --print-module-deps` analyzes the application JAR against that exact runtime classpath.

Its result feeds `jlink --add-modules`.

The current service requires the derived module set to include at least:

~~~text
java.net.http
jdk.httpserver
~~~

Other transitive JDK modules remain derived.

## Image shape

~~~text
<image>/
  service-image.json

  bin/
    vexlife-prod-service
    vexlife-prod-setup

  lib/
    vexlife-prod.jar
    <exact JVM runtime dependency JARs>

  runtime/
    bin/java
    lib/modules
    ...
~~~

The launcher always uses the bundled runtime:

~~~text
<image>/runtime/bin/java
  -cp <image>/lib/*
  vexlife.service.VexLifeProdServiceProcessKt
~~~

It does not search PATH for Java.

## Manifest

Schema:

~~~text
vexlife-prod.private-service-image/v3
~~~

Truth class:

~~~text
SOURCE_BOUND_PRIVATE_SERVICE_IMAGE
~~~

The manifest records:

~~~text
sourceSha
platform = darwin-arm64
javaRuntimeMajor = 21
mainClass
launcher
setupLauncher
setupWindow
runtimeModules[]
payloadSha256
artifacts[]:
  path
  sha256
  bytes
~~~

The manifest is evidence about one already-built image. It grants no execution authority.

## Payload fingerprint protocol

The whole image payload is fingerprinted **before** the manifest is written.

`service-image.json` itself is excluded.

Walk every remaining entry in sorted relative-path order without following symlinks.

For each entry form one UTF-8 descriptor:

~~~text
regular file:
  "F" NUL relativePath NUL sortedPosixPermissions NUL fileSha256

directory:
  "D" NUL relativePath NUL sortedPosixPermissions

symlink:
  "L" NUL relativePath NUL sortedPosixPermissions NUL linkTarget
~~~

Append one NUL byte after every descriptor.

SHA-256 the complete descriptor stream.

That value is:

~~~text
payloadSha256
~~~

Permissions are included because an executable launcher/runtime with wrong permission bits is not the
same runnable image.

The build implementation and runtime verifier intentionally implement this same explicit protocol.
Changing the protocol requires a schema/version decision rather than an incidental refactor.

## Source-addressed archive carriage

The build also source-forms:

~~~text
./gradlew archiveVexLifeProdServiceImage
~~~

It consumes the already-formed exact image and produces:

~~~text
build/vexlife-prod-service-image/archives/<source-sha>/
  vexlife-prod-service-image-darwin-arm64-<source-sha>.tar.gz
  vexlife-prod-service-image-darwin-arm64-<source-sha>.tar.gz.sha256
  vexlife-prod-bootstrap-darwin-arm64-<source-sha>.command
  vexlife-prod-bootstrap-darwin-arm64-<source-sha>.command.sha256
  vexlife-prod-bootstrap-darwin-arm64.command
  vexlife-prod-bootstrap-darwin-arm64.command.sha256
  vexlife-prod-service-image-darwin-arm64-<source-sha>.receipt.json
~~~

The archive uses macOS `/usr/bin/tar` with `COPYFILE_DISABLE=1` so carriage preserves the private
image's file modes/symlinks without adding AppleDouble sidecars.

The archive receipt binds:

~~~text
sourceSha
platform = darwin-arm64
archiveName
archiveSha256
archiveBytes
releaseTag
currentChannel = github-release-latest
bootstrapName
bootstrapSha256
currentBootstrapAssetName
currentBootstrapSha256AssetName
bootstrapBytes
imagePayloadSha256
~~~

Permanent:

~~~text
ARCHIVE
!= IMAGE CURRENTNESS

ARCHIVE SHA
!= IMAGE PAYLOAD SHA

CURRENT RELEASE CHANNEL
-> selects one exact release

CURRENT BOOTSTRAP ALIAS
= byte-exact copy of that release's source-specific bootstrap

CURRENT BOOTSTRAP ALIAS
!= mutable runtime configuration

ARCHIVE RECEIPT
!= INSTALL AUTHORITY
~~~

The archive is carriage around the already-source-bound image. After extraction, the image verifier
must still independently re-prove the internal payload.

## Runtime verification

`VexLifeProdPrivateServiceImage.observe(imageRoot, expectedSourceSha)` performs no effects.

It requires:

~~~text
exact expected source SHA
strict manifest schema
darwin-arm64
Java runtime major 21
exact main class
exact launcher coordinate
sorted unique runtime module set
java.net.http + jdk.httpserver present
sorted unique artifact catalog
artifact byte counts + SHA-256 exact
real executable launcher
real executable bundled java
runtime/lib/modules present
whole payload fingerprint exact
~~~

A source mismatch or any payload mutation fails closed.

~~~text
IMAGE DIRECTORY EXISTS
!= IMAGE CURRENT

MANIFEST EXISTS
!= PAYLOAD CURRENT

SOURCE SHA MATCHES
!= PAYLOAD CURRENT
~~~

All predicates must agree.

## Source-formed proof

Current source/tests prove:

~~~text
source identity is derived from clean Git rather than an argument
source is rechecked after compilation
runtime dependency JAR names are derived from KMP
JDK modules are derived by jdeps
private runtime is built by jlink
launcher uses bundled java
manifest binds source + artifacts + payload
exact image observation has no effects
wrong expected source fails
mutated payload fails
~~~

## Held

~~~text
exact repository Gradle 9.6 / Kotlin 2.4.20 packaging execution
real macOS arm64 JDK 21 image formation
real jdeps module result
real jlink runtime launch
real archive + generated bootstrap formation / receipt proof
provider release publication / asset delivery
exact bootstrap download + extraction
installation into VexLife-Prod local storage
launchd-owned product service currentness
source rotation
sustained service after human launcher exits
VexWeb presentation
ordinary fresh-user entry
~~~

## Relationship to Java packaging tools

The product uses a private `jlink` runtime image rather than asking the human to install Java.

A future distributable installer may use another platform packaging layer, but that layer must consume
this exact source-bound private image rather than recomputing runtime/product truth independently.

~~~text
PRIVATE IMAGE
= SERVICE EXECUTION PAYLOAD

INSTALLER
= CARRIAGE / MATERIALIZATION

INSTALLER
!= SERVICE SOURCE OWNER
~~~

## VexBoard projectability

This foundation supplies explicit Depth relationships for a later Lens:

~~~text
service process
-> private image
-> bundled java
-> app/dependency JARs
-> exact source revision

macOS lifetime owner
-> verified image
-> service process
~~~

Those are construction/lifetime coordinates, not semantic Family nodes.

<!-- [VXG RealForever][VEXLIFE-PROD][PRIVATE-SERVICE-IMAGE] -->
