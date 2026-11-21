# SwiftUI reference adapter — Quick Actions practicum

`[VXG RealForever]`

## Human question

Can current compiler-visible VexLife relationships generate **native SwiftUI source plus a semantic
manifest** without copying Compose/browser implementation shape or moving Product meaning into the
generator?

This is the first bounded iOS-target pressure for the Component Library direction.

## Why Quick Actions

Current VexLife-Prod already owns this exact subtree:

~~~text
VexLifeWorkspaceBlueprint.QuickActions
├── VexSummon
├── VisionQuickAccess
├── Speech
├── Language
├── Theme
├── ReleaseAlerts
├── Support
└── QuickActionsTrigger
~~~

Current `VexLifeWebP1` separately binds those stable identities to existing presentation
relationships:

~~~text
VexSummon         -> LocalInteraction(Companion)
Vision            -> LocalInteraction(Vision)
Speech            -> LocalInteraction(SpeechPanel)
Language          -> LocalInteraction(LanguagePanel)
Theme             -> PresentationAction(color-mode.toggle)
ReleaseAlerts     -> LocalInteraction(ReleaseAlertsPanel)
Support           -> ExternalNavigation(Vex Support)
Trigger           -> LocalInteraction(QuickActions)
~~~

That is rich enough to test target adaptation while remaining presentation-only.

## Candidate relationship

~~~text
accepted current Kotlin Blueprint handles
+ accepted current VexWeb interaction/binding relationships
        ↓
reference:swiftui generator
        ↓
SwiftUI-native source
+ vexlife.swiftui-reference-manifest/v1
~~~

The generated Swift source uses a native `Menu` and typed `VexQuickAction` enum. It does **not**
embed Product-facing English. Human words are supplied through `VexQuickActionsStrings`, preserving
the existing Localization ownership boundary.

The generated semantic manifest retains:

- element ref;
- source parent ref;
- placement;
- depth;
- presentation;
- interactions;
- binding type + target/action/href;
- target framework/component;
- target role;
- binding realization.

The first target pressure already teaches one important law:

~~~text
SOURCE TREE
  QuickActions
  ├── action children
  └── QuickActionsTrigger

SWIFTUI TARGET
  Menu
  ├── Menu actions
  └── label = QuickActionsTrigger
~~~

The target does **not** have to copy source containment literally. It must preserve the source
relationship and explicitly project how the native target realized it.

For this candidate:

~~~text
QuickActions root
-> SwiftUI Menu

QuickActionsTrigger
-> MENU_LABEL
-> NATIVE_MENU_PRESENTATION

action children
-> MENU_ACTION
-> HOST_DISPATCH
~~~

That mapping is what a later target Screen Lens can observe without reverse-engineering Swift syntax.

That manifest exists so a later iOS Screen Lens adapter can observe the generated source relationship
without treating Swift syntax as canonical meaning.

## Why this consumes VexWeb instead of inventing a portable root immediately

This practicum intentionally pressures the current accepted relationship source first.

~~~text
CURRENT VEXWEB RELATIONSHIPS
-> SWIFTUI ADAPTER PRESSURE
-> observe what survives / what does not
-> only then decide whether a smaller shared portable owner is earned
~~~

If the adapter can preserve the needed relationships directly, that is evidence about the existing
contract. If iOS exposes a relationship VexWeb cannot state without browser-specific leakage, that
specific failure may earn extraction/generalization.

Do **not** create a second generic component ontology merely because cross-platform construction is
desired.

## Boundary

Permanent for this candidate:

~~~text
SWIFTUI REFERENCE GENERATION
!= IOS PRODUCT
!= IOS APPLICATION
!= NATIVE COMPILE PASS
!= PORTABLE COMPONENT ACCEPTANCE

VEXWEB INPUT
!= UNIVERSAL PORTABLE AUTHORITY

GENERATED SWIFT
!= PRODUCT SEMANTIC SOURCE

SEMANTIC MANIFEST
!= CANONICAL WRITEBACK
~~~

No current production renderer is replaced.

## Proof floor

Source-side proof:

~~~text
:reference:swiftui:compileKotlin
:reference:swiftui:test
root JVM / JS regressions remain green
current Quick Actions child/binding identities match exact source
generated source deterministic
generated source contains no hardcoded Product display words
manifest preserves reverse-projection relationships
~~~

Native target proof remains separate:

~~~text
generated Swift source
-> qualified Xcode / Swift compiler
-> target-native compile
-> iOS semantic-manifest / Screen Lens projection
-> later lived presentation proof if the claim requires it
~~~

Until the Xcode step runs, the candidate claims only **deterministic SwiftUI source formation**, not
iOS compilability.

<!-- [VXG RealForever][VEXLIFE-PROD][REFERENCE][SWIFTUI][QUICK-ACTIONS] -->


## Second pressure — stable repeated runtime items

Quick Actions proved that target-native structure may differ from source structure while identity and
realization mapping remain explicit.

The next pressure is harder: **runtime item identity**.

Two independent current/accepted sources already expose the need:

~~~text
VexLife-Android
  VexStableLazyColumn
  -> stable item ref becomes native list key
  -> duplicate/blank refs fail closed
  -> optional requested item
  -> visible item projection
  -> caller owns item meaning/order/content

VexLife-Prod
  VexLifeWorkspaceBlueprint.ConversationTimeline
  -> static collection coordinate

  VexLifeConversationContract
  -> durable messageRef / eventRef / sequence

  current browser runtime
  -> ordinary timeline render rebuilds row nodes
  -> live waiting row is deliberately preserved while streaming
~~~

The SwiftUI reference does **not** port the browser renderer or Android source.

It composes the rightful current Product owners:

~~~text
Workspace Blueprint
  -> collection coordinate

Conversation contract
  -> durable item identity / event coordinate / chronology

reference:swiftui
  -> target-native stable repeated-item source
~~~

Generated Swift maps only the relationships already earned:

~~~text
messageRef
-> VexConversationTimelineItem.id
-> ForEach identity
-> View.id
-> accessibilityIdentifier

ConversationTimeline ref
-> container accessibilityIdentifier

duplicate refs
-> fail closed

supplied item order
-> preserved
-> adapter does not sort
~~~

The remaining Android mechanics are carried honestly as target dispositions rather than silently
dropped:

~~~text
requestedVisibleItemRef   HELD
visible-item projection   HELD
contentType reuse hint    PLATFORM_SPECIFIC
scroll lock               HELD
~~~

This is useful pressure. It says the portable contract may own **stable repeated-item mechanics** while
Conversation continues to own message meaning, chronology, ordering and persistence.

Permanent:

~~~text
FEATURE ITEM IDENTITY
!= VISUAL LIST INDEX

COLLECTION MECHANICS
!= ITEM SEMANTIC OWNER

TARGET DIFFING
!= DURABLE CONVERSATION IDENTITY

HELD TARGET RELATIONSHIP
!= MISSING SOURCE MEANING
~~~

A later SwiftUI/Xcode target proof can decide whether requested scrolling, visible-item projection and
scroll locking are stable enough across the selected Apple deployment floor to become mapped target
relationships.



## Third pressure — typed Frame / Seam / Region to SwiftUI layout

The third pressure consumes current accepted spatial relationships directly rather than reconstructing
geometry from browser CSS.

Current VexLife-Prod source already owns:

~~~text
WorkspaceFrame
  Horizontal HeaderBodySeam
  Allocation 1 : 11
  HeaderRegion / BodyRegion

BodyFrame
  Vertical BrowserCompanionSeam
  Allocation 1 : 1
  BrowserRegion / CompanionRegion
~~~

The SwiftUI reference maps only that exact current full-span/single-Seam shape:

~~~text
WorkspaceFrame
-> GeometryReader
-> VStack proportional height

HeaderRegion
-> 1 / 12 height

BodyRegion
-> 11 / 12 height
-> HStack proportional width

BrowserRegion
-> 1 / 2 width

CompanionRegion
-> 1 / 2 width
~~~

The generated code carries region/frame refs as target accessibility identifiers. Ratios are derived
from the accepted `Allocation.Ratio`; no target pixel sizes become authored Product meaning.

The target manifest is intentionally explicit about the bounded support surface:

~~~text
full-span-single-seam       MAPPED
region-identity             MAPPED

seam-resize-collapse        HELD
partial-or-multi-seam-frame HELD
overlay-depth               HELD
~~~

This prevents the first iOS adapter from silently shrinking the existing Frame / Seam / Region
grammar into a two-pane abstraction.

The exact source proof caught and corrected one real formation defect before any PASS claim:

~~~text
wrong reference import:
  vexlife.web.VexLifeWeb

rightful accepted owner:
  vexlife.web.VexWeb
~~~

After that correction, the exact GitHub generator blob:

~~~text
blob  97271143877e0ba0c78e78fdb1a8a7964c7a24c2
bytes 14530
~~~

passed the bounded proof:

~~~text
kotlinc-jvm 1.9.0 generator compile   PASS
generator execution                  PASS
deterministic generation             PASS
manifest JSON parse                  PASS
Swift 6.2.1 frontend parse           PASS
minimal SwiftUI API-shape typecheck  PASS

generated Swift bytes                2207
generated manifest bytes             1495

Swift sha256
4b756b5657e0b7d899ef44148dccb44c73d39b168e4add504e4769c1e45a0d1e

manifest sha256
2203eb0d90eeb08726f8b28064bacc1ec5bbb46833a1a20b9ada8d3e31c8a53b
~~~

As with the other references:

~~~text
EXACT GENERATOR COMPATIBILITY PROOF
!= REPOSITORY KOTLIN 2.4.20 PROOF
!= APPLE SWIFTUI / XCODE PROOF
~~~

The next generic spatial question should be earned by an actual partial/multi-Seam target specimen.
Do not duplicate VexWeb's generic geometry resolver into SwiftUI merely to make the adapter look
complete.

