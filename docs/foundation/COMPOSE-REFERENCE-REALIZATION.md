# Compose reference realization

`[VXG RealForever]`

## Human question

Can the accepted Kotlin semantic/Blueprint foundation produce a simple Compose UI relationship without making Compose a semantic owner or requiring a renderer to reconstruct identity from string convention?

Current answer under pressure:

~~~text
accepted semantic State
-> Family Projection
-> DisplayState
-> concrete presentation
-> Compose manifestation

stable Screen Blueprint
-> compiler-visible element handle
-> Compose testTag / presentation coordinate
~~~

## Why Blueprint handles were refined

The accepted P0 Blueprint already produced stable refs, but its first DSL returned only a completed tree. A renderer would therefore have to retype strings such as:

~~~text
element.vexlife-workspace.browser.viewport
~~~

That is exactly the kind of assumption channel New Beginnings is intended to remove.

The Compose pressure therefore refines the Blueprint to expose named Kotlin handles:

~~~kotlin
VexLifeWorkspaceBlueprint.BrowserViewport
VexLifeWorkspaceBlueprint.Composer
VexLifeWorkspaceBlueprint.Send
~~~

Each handle carries its authored ref, kind and parent ref.

~~~text
STABLE REF
!= HUMAN MEMORY OF THE REF

COMPILER-VISIBLE HANDLE
-> renderer consumes existing identity
~~~

## Reference renderer scope

The first Compose realization is JVM Desktop only.

That is deliberate:

~~~text
Compose Desktop
= reference manifestation / design laboratory

Web
= later browser-native realization

Compose Desktop
!= portable UI authority
!= P1 web runtime
~~~

The renderer manifests the supplied initial-engagement structure:

~~~text
VexLife shell
├── header
├── browser placeholder
└── companion
    ├── Conversation placeholder
    └── Settings placeholder
~~~

No browser, Conversation, model, Memory, Home or Settings backend is connected.

Unavailable capabilities render as unavailable/unbound rather than receiving invented state.

## Local presentation does not automatically earn Family

The Conversation/Settings swap is intentionally:

~~~kotlin
remember { mutableStateOf(...) }
~~~

inside the Compose reference.

That is a pressure result, not a shortcut:

~~~text
local visual selection
!= semantic product State
!= automatically a Family
~~~

If later product evidence says the pane selection must survive, participate in another domain, drive effects, or carry semantic currentness, that concrete pressure may earn a rightful owner.

## DisplayState -> UI / UI -> Control proof

A reference-only Toggle Family exists under the Compose reference package.

It is not wired into the VexLife Workspace and is not a product feature.

Its Composable receives only:

~~~text
Family Projection
-> DisplayState.checked

and

onControl(Control)
~~~

It does not receive:

~~~text
Store/provider
Effect executor
Logger
dependency factory
raw product State mutation
~~~

This is the simplicity threshold being tested:

> A renderer should normally need to know what to display and which typed Control it may emit.

## Toolchain

This candidate retains the accepted Kotlin 2.4.20 semantic toolchain and adds:

~~~text
Compose Multiplatform 1.12.2
Compose Compiler plugin 2.4.20
JVM Desktop reference renderer
~~~

Compose Multiplatform is implementation substrate for this reference only. A future version bump is a build/toolchain decision, not a product-semantic migration.

## Proof boundary

Required before acceptance:

~~~text
exact candidate compile
-> Kotlin/JVM tests
-> Compose JVM source compile
-> Kotlin/JS production/test compile still PASS
-> no product Family introduced for local pane state
-> Blueprint handle/parent tests PASS
~~~

A visual human inspection may become a later lived/reference checkpoint; source compilation alone does not prove pixel-level acceptance.

<!-- [VXG RealForever][VEXLIFE-PROD][P0][COMPOSE-REFERENCE] -->


## A001 proof reconciliation and #209 alignment hardening

A001 stopped before Kotlin source compilation because Compose Desktop's AndroidX transitive
dependencies require Google's Maven repository. Maven Central alone was an incomplete dependency
source.

~~~text
A001 COMPILE_KOTLIN_JVM safe failure
-> missing Google Maven repository
-> candidate semantics not reached
~~~

The same threshold review re-read current VexLife-Android PR #209 and found two useful compiler
hardening opportunities in this clean contract:

1. nested Family primitives now require their generic Family coordinate to actually be a
   `VexFamilyContract<...>`, matching #209's family-scoped compiler fence;
2. the reference-only Toggle now carries one `Parameter.Activation` instance by identity through
   `Control.Select -> Effect.Remember`.

This is intentional alignment of the semantic law, not wholesale API parity.

Still deliberately absent until earned by real runtime pressure:

~~~text
live Family state holder
activate / observe
Act.Begin + execution Approach
RelationshipId / ResourceId
Act.Method / MethodFollowUp
VexExecution
Store/provider realization
~~~

Those omissions preserve the clean-container boundary; adding them only to resemble #209 would
violate the same judgment discipline #209 documents.

<!-- [VXG RealForever][VEXLIFE-PROD][P0][COMPOSE-REFERENCE][A002] -->


## A002 proof reconciliation

A002 reached Kotlin source compilation and failed only because the reference source explicitly
imported `androidx.compose.foundation.layout.weight`.

Under the current Compose source, `weight` is a parent-layout scope extension: Row/Column placement
owns whether a child has weight. The correction removes the explicit import and leaves each
`Modifier.weight(...)` expression at its existing callsite inside a Row/Column scope.

~~~text
PARENT LAYOUT OWNS PLACEMENT
!= CHILD IMPORTS A GLOBAL WEIGHT PRIMITIVE
~~~

No Family, Blueprint, Parameter, or presentation ownership changed.

<!-- [VXG RealForever][VEXLIFE-PROD][P0][COMPOSE-REFERENCE][A003] -->
