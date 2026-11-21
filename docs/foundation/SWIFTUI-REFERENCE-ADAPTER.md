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
