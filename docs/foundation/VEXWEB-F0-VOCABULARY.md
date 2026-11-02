# VexWeb F0 — first fractal vocabulary pressure

`[VXG RealForever]`

## Source pressure

F0 descends from the accepted completed-view blueprint:

`docs/foundation/VEXWEB-COMPLETED-VIEW-BLUEPRINT.md`

The first specimen is deliberately smaller than the whole VexLife screen:

~~~text
SplitContainer
├── BrowserPane
│   └── BrowserViewport
├── SplitHandle
└── CompanionPane
    └── Conversation
        └── Composer
            └── Send
                └── NotYetImplemented -> Snackbar

OverlayRoot
└── Snackbar
~~~

No web generator exists yet.

## Vocabulary earned so far

The specimen currently earns these relationships:

~~~text
Identity
  Blueprint(element)
  Structural(ref)

Parameter
  SplitRatio
  SplitMode
    Balanced
    LeadingOnly
    TrailingOnly

Depth
  Content < Chrome < Transient < Modal

Placement
  InFlow
  FillAvailable
  Leading
  Trailing
  Overlay

Presentation
  Surface
  SplitHandle
  PrimaryAction
  TransientFeedback

Interaction
  Activate
  Resize
  CollapseLeading
  CollapseTrailing

Binding
  None
  LocalPresentation(target)
  Unbound(fallback)

Fallback
  NotYetImplemented(
    capabilityRef,
    feedbackTarget
  )

Node
  Element(Blueprint identity)
  Structural(VexWeb identity)
  Split(explicit leading / handle / trailing)

Surface
  content
  overlayRoot
  -> fractal Coordinate projection
~~~

These names are F0 candidate vocabulary, not universal ontology.

## Why the bindings carry targets now

The F0 source deliberately binds relationships that would otherwise be reconstructed downstream.

~~~text
SplitHandle
-> LocalPresentation(
     target = exact SplitContainer
   )

Send
-> Unbound(
     NotYetImplemented(
       capabilityRef = conversation.send,
       feedbackTarget = exact Snackbar
     )
   )
~~~

The future renderer should not have to rediscover which split a handle controls or where missing
capability feedback belongs.

F0 now enforces this mechanically:

~~~text
interactive node
-> Binding must be explicit

LocalPresentation(target)
-> target must resolve inside the same Surface

NotYetImplemented(feedbackTarget)
-> target must resolve inside the same Surface
-> target must live on Transient or Modal depth
~~~

So a clickable/resizeable node cannot silently exist with `Binding.None`, and a fallback cannot
point at an imaginary snackbar.

This follows the same architectural instinct behind fully-bound Family Methods: keep meaning bound
until the one mechanical layer that must realize it.

## Blueprint identity and structural identity

Product-visible elements retain accepted Blueprint refs:

~~~text
element.vexlife-workspace.browser
element.vexlife-workspace.browser.viewport
element.vexlife-workspace.companion
element.vexlife-workspace.companion.conversation.composer
element.vexlife-workspace.companion.conversation.send
~~~

VexWeb-only manifestation structure has its own explicit source identity:

~~~text
vexweb.vexlife-workspace.split
vexweb.vexlife-workspace.split.handle
vexweb.vexlife-workspace.overlay
vexweb.vexlife-workspace.overlay.snackbar
~~~

Permanent:

~~~text
BLUEPRINT IDENTITY
!= MANIFESTATION STRUCTURE IDENTITY

MANIFESTATION IDENTITY
!= CSS SELECTOR
~~~

A generator may later derive selectors/DOM attributes from these refs.

## Split mode remains presentation

F0 models:

~~~text
Balanced(ratio)
LeadingOnly
TrailingOnly
~~~

as VexWeb presentation parameters.

There is no StateFlow, persistence, Family, provider or semantic effect.

That absence is intentional.

## Depth remains relative meaning

F0 establishes only relative depth classes:

~~~text
Content
< Chrome
< Transient
< Modal
~~~

No numeric z-index appears in authored source.

A renderer may later map the relationship to platform stacking mechanics.

## Honest incomplete UI

The Send control is intentionally visible before Conversation Send exists.

Its source is not a no-op:

~~~text
Activate Send
-> Unbound
-> NotYetImplemented(conversation.send)
-> exact Snackbar feedback target
~~~

This is the first builder-facing incomplete-capability foundation.

## Held until later pressure

F0 does not yet establish:

~~~text
HTML tags
CSS properties
JavaScript event code
generator output format
responsive layout
actual split resizing state holder
browser provider
Conversation Family / Send Control
QuickActionCluster
Dialog
accessibility mapping
DOM context projection
~~~

## Required proof

Before F0 vocabulary acceptance:

~~~text
exact Kotlin 2.4.20 root JVM compile/test
exact Kotlin/JS compile/test
all F0 tests PASS

and review:
- no platform syntax leaked into VexWeb source;
- no product Family invented for split presentation;
- no manual selector vocabulary;
- all bound targets are exact source identities.
~~~

<!-- [VXG RealForever][VEXWEB][F0][VOCABULARY] -->


## A001 proof reconciliation — portable source identity

A001 exact RETURN:

~~~text
sha256 03a7309be17244e29d19ec332e18bcbc8e80d38c97fc2537e0bd9f6cdbd71fa4

compileKotlinJvm PASS
jvmTest          PASS
compileKotlinJs  SAFE_FAILURE
~~~

The first JS compiler finding was not a VexWeb relationship failure. It found JVM-specific
representation metadata in portable common source:

~~~text
@JvmInline value class StructuralRef
@JvmInline value class CapabilityRef
~~~

A002 retains the typed source identities and validation but uses platform-neutral data classes with
consistent copy visibility.

Permanent:

~~~text
SOURCE IDENTITY
!= JVM REPRESENTATION OPTIMIZATION
~~~

Current VexArchitecture orientation also changed during this threshold: VexLife-Android PR #209 is
now merged at `66c866dea6a8dd78e2946da88715c4deb78c1847`. Future architecture grounding descends to
accepted repository source; PR #209 remains provenance for the pressure/reasoning history.

<!-- [VXG RealForever][VEXWEB][F0][A002] -->
