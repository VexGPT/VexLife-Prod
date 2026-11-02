# Semantic contract authoring practicum

`[VXG RealForever]`

## Human question

Can VexLife use Kotlin as a compiler-constrained semantic/Blueprint authoring language so future AI instances extend explicit relationships instead of copying implementation patterns?

P0 deliberately answers the smallest part first.

~~~text
semantic relationship
-> compiler-visible Family / State / Control / Effect kinship
-> bounded DisplayState projection

static UI intention
-> rooted Screen / Element identity
-> no renderer yet
~~~

This source does **not** establish Compose, web rendering, execution, Store/provider, Logger, dependency construction, Conversation, model connection, or browser-context access.

## Why this shape is smaller than VexLife-Android

VexLife-Android PR #209 is current architecture pressure, not source to transplant wholesale.

The first clean-container question does not yet require runtime Job custody or provider realization. Therefore this practicum retains only the semantic relationships needed to test the compiler-authoring thesis:

~~~text
VexFamilyContract
  Parameter
  State
  Control
  Act
    admitted State?
    Effect?
  Effect
  DisplayState
  Projection
~~~

There is intentionally no live Family state holder yet.

~~~text
SEMANTIC CONTRACT
!= RUNTIME STATE HOLDER
!= EXECUTION OWNER
~~~

A real product consumer must earn the runtime shape. Do not add an execution bridge merely to make this source resemble Android.

## Test specimen is not product semantics

The Family test uses a private specimen with:

~~~text
Mode Parameter
-> Select Control
-> optional admitted State
-> Persist Effect
-> Active / Inactive DisplayState
~~~

That specimen exists only to prove type/relationship behavior.

~~~text
TEST SPECIMEN
!= VEXLIFE FEATURE
!= PERMISSION TO CREATE A FAMILY FOR EVERY UI CONTROL
~~~

In particular, the Conversation / Settings visual swap in the supplied UI remains local presentation until real semantic pressure proves otherwise.

## Initial workspace Blueprint

`VexLifeWorkspaceBlueprint` records only static relationships visible in the supplied initial-engagement design:

~~~text
screen.vexlife-workspace
  header
    brand
    browser-query
    app-selector
    utilities

  browser
    viewport

  companion
    conversation
      timeline
      response-progress
      composer
      send

    settings
      context
      resources
      home
      continuity
      reset
~~~

The Blueprint gives these structures stable addresses. It does not claim the corresponding capabilities are connected.

Dynamic Conversation messages and browser/provider content are intentionally not static Blueprint members.

## Portable pressure

The source lives in `commonMain` and the candidate build declares JVM + Kotlin/JS targets.

The intended question is not "can Kotlin compile?"

It is:

> Can the same semantic source remain platform-neutral enough that Compose and a browser-native renderer can later consume its bounded projections without re-authoring the product meaning?

That claim requires exact repository-owned Kotlin 2.4.20 JVM/JS proof before this candidate is accepted.

## Current proof boundary

Disposable formation pressure performed outside the repository checkout:

~~~text
container kotlinc-jvm 1.9.0
production source compile = PASS
specimen runtime probe    = PASS
static Blueprint refs     = 19 unique
~~~

This proves only that the candidate idea is syntactically/coherently expressible in that disposable environment.

It does **not** prove:

~~~text
exact repository candidate build
Kotlin 2.4.20 compatibility
Kotlin/JS compilation
Gradle project health
Compose realization
web realization
product semantics
~~~

Current missing proof:

~~~text
complete repository-owned Gradle wrapper
  text launcher/properties = SOURCE-FORMED
  binary gradle-wrapper.jar = HELD_HOST_MATERIALIZATION

-> exact Kotlin 2.4.20 common/JVM/JS compile
-> common tests
~~~

The text wrapper source is byte-identical to current VexLife-Android PR #209 at `b544ca789831305dfeeb9badb6d41ca1ac50439c`:

~~~text
gradlew                                  git blob 249efbb032ce46a80c687c0723eb172e85f6a136
gradlew.bat                              git blob a51ec4f5886d3bb106b26724ee4507addbbe3fa8
gradle/wrapper/gradle-wrapper.properties git blob 65d8749eabe0ced0bdda3a2cafb2f10019d9ddb5
expected wrapper JAR git blob            b1b8ef56b44f16b14dc800fa8103a6d89abb526f
~~~

The connected GitHub surface cannot transfer non-UTF-8 repository blobs across repositories. The missing JAR is therefore a host-carriage/build-substrate predicate, not candidate semantic evidence.

Do not call this foundation proved until the exact wrapper JAR is materialized from that content-addressed source, committed to this candidate, and the exact Kotlin 2.4.20 JVM/JS/common-test proof passes.

<!-- [VXG RealForever][VEXLIFE-PROD][P0][SEMANTIC-CONTRACT] -->


## Accepted threshold

The P0 semantic-contract threshold is now accepted source.

~~~text
host proof RETURN
sha256 f90ad6d92e5910d534dff2fa021685a473dbfbea33cabe849d017cb9279913f5

proven / accepted tree
4e6e74255bb444d2adfbd991ddd6c1fba0049fb0

accepted main
c9aca940a49cb2e828ecfbd4be726f3553bf0063
~~~

Exact proof:

~~~text
Kotlin/JVM compile + jvmTest     PASS
Kotlin/JS production compile    PASS
Kotlin/JS test compile          PASS
post-merge candidate tree       EXACT
~~~

The historical formation/proof sections above remain provenance for how the threshold was earned. They are not the current lifecycle state.

Current next construction question:

> Can a Compose reference renderer consume the accepted Blueprint and DisplayState relationships with simple presentation code, without introducing product semantics, provider mechanics, or a production Family merely for visual state?

Permanent:

~~~text
SEMANTIC CONTRACT ACCEPTED
!= COMPOSE ACCEPTED
!= PORTABLE RENDERER CONTRACT ACCEPTED
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][P0][SEMANTIC-CONTRACT][ACCEPTED] -->
