# VexWeb completed-view blueprint

`[VXG RealForever]`

## Purpose

This is the durable human-intention snapshot for the first VexWeb foundation work.

It records the completed VexLife nested-browser experience **before** VexWeb implementation syntax is
allowed to harden around platform mechanics.

The source evidence is the supplied VexLife Nested Browser Experience design showing:

- persistent VexLife chrome;
- browser/query/app controls;
- quick-action/FAB-like controls;
- a nested browser surface;
- a resizable/collapsible browser/companion split;
- Conversation and Settings companion views;
- message/thought/reasoning surfaces;
- composer + Send;
- capability/resource/settings projections;
- future destructive/reset controls.

Permanent:

~~~text
SCREENSHOT
= HUMAN INTENTION + EXPERIENCE EVIDENCE

SCREENSHOT
!= HTML SPECIFICATION
!= CSS SPECIFICATION
!= JAVASCRIPT SPECIFICATION
!= COMPLETE BACKEND CONTRACT
~~~

VexWeb exists so platform syntax is generated from authored relationships rather than reverse-engineered
into meaning after the fact.

## Completed-view relationship graph

~~~text
VexLifeWorkspace
│
├── ShellChrome
│   │
│   ├── ProductIdentity
│   │   ├── Logo
│   │   └── ProductName
│   │
│   ├── BrowserQuery
│   ├── AppSelector
│   │
│   └── QuickActionCluster
│       ├── QuickAction
│       ├── QuickAction
│       ├── QuickAction
│       └── ...
│
├── WorkspaceBody
│   │
│   └── SplitContainer
│       │
│       ├── BrowserPane
│       │   └── NestedBrowserSurface
│       │
│       ├── SplitHandle
│       │   ├── CollapseBrowser
│       │   ├── Resize
│       │   └── CollapseCompanion
│       │
│       └── CompanionPane
│           │
│           ├── CompanionHeader
│           │
│           └── CompanionContent
│               │
│               ├── ConversationView
│               │   ├── Timeline
│               │   │   ├── Message
│               │   │   ├── ThoughtDisclosure
│               │   │   ├── ReasoningSurface
│               │   │   └── ...
│               │   │
│               │   └── Composer
│               │       ├── Input
│               │       └── SendControl
│               │
│               └── SettingsView
│                   ├── ContextWindowProjection
│                   ├── ResourceProjection
│                   ├── ThoughtVisibilityControl
│                   ├── ResourceAlertControl
│                   ├── HomeSection
│                   ├── DirectoryControls
│                   ├── ContinuityControl
│                   ├── DreamSyncControl
│                   └── DestructiveResetControl
│
└── OverlayRoot
    ├── QuickActionOverlay / FAB manifestation
    ├── Snackbar / transient feedback
    ├── Dialog
    ├── Popover / menu
    └── Modal surface
~~~

This graph is a completed-view orientation, not a requirement to mint one class or type for every noun.

## Fractal view law

Every meaningful view coordinate should be understandable through the same questions regardless of
whether the coordinate is the whole Workspace or one Send control.

~~~text
View
├── Identity
├── Parent / containing relationship
├── Children / contained relationships
├── Placement
├── Depth
├── Presentation
├── Interaction
├── Binding
└── Fallback
~~~

Example:

~~~text
WorkspaceBody
├── parent = Workspace
├── children = SplitContainer
├── placement = FillAvailable
├── depth = Content
└── binding = none

SendControl
├── parent = Composer
├── placement = Trailing
├── depth = InFlow
├── presentation = PrimaryAction
├── interaction = Activate
├── binding = Conversation.Send
└── fallback = NotYetImplemented(conversation.send)
~~~

The grammar should remain fractal: a fresh worker can descend from Workspace to Send or enter at Send
and recover its enclosing relationship without reconstructing implementation history.

## Blueprint identity versus manifestation hierarchy

VexBlueprint already owns stable product/UI identity.

VexWeb may introduce manifestation containers such as SplitContainer or OverlayRoot without rewriting
accepted Blueprint refs merely to make their path mirror a DOM nesting tree.

~~~text
BLUEPRINT IDENTITY
!= DOM TREE PATH
!= CSS SELECTOR PATH

VEXWEB MANIFESTATION CONTAINER
may relate existing Blueprint elements
without renaming those elements
~~~

The browser manifestation must consume compiler-visible Blueprint handles where product identity
exists.

It must not recreate selectors from memory.

## Split / drawer relationship

The browser/companion divider is not primarily two arrow buttons.

The reusable relationship is:

~~~text
SplitContainer
├── LeadingPane
├── Handle
└── TrailingPane
~~~

Current presentation pressure includes:

~~~text
Balanced(ratio)
BrowserFocused
CompanionFocused
~~~

These describe presentation/layout meaning.

They do **not** automatically create a product Family.

~~~text
RESIZABLE / COLLAPSIBLE PRESENTATION
!= SEMANTIC PRODUCT STATE
!= AUTOMATIC FAMILY
~~~

If later evidence requires persistence, cross-domain participation, semantic effects or explicit
currentness, that pressure may earn another rightful owner.

## Depth / layering law

VexWeb should not treat arbitrary numeric `z-index` values as authoring meaning.

The completed experience already contains multiple relative depth responsibilities:

~~~text
Workspace
├── ContentPlane
│   ├── Browser
│   └── Companion
│
├── ChromePlane
│   ├── SplitHandle
│   └── persistent quick actions
│
├── TransientPlane
│   └── Snackbar / transient feedback
│
└── ModalPlane
    └── Dialog / blocking modal interaction
~~~

Authored source should preserve relative depth/priority.

Generated platform code may choose numeric stacking values.

~~~text
AUTHOR
Transient above Content

GENERATOR
z-index: <mechanical value>
~~~

Numeric values are implementation output unless a numeric value is itself the meaningful parameter.

## Unbound capability / NotYetImplemented foundation

The interface must be buildable before every capability is connected without silently doing nothing
or inventing fake provider state.

Initial pressure:

~~~text
ControlBinding
├── Bound(typed relationship)
└── Unbound(Fallback)
~~~

First fallback:

~~~text
NotYetImplemented(ref)
~~~

Example:

~~~text
SendControl
-> Unbound(
     NotYetImplemented("conversation.send")
   )

human activation
-> standard transient feedback
-> source-addressable missing capability
~~~

This gives builders an honest UI scaffold while preserving the future connection coordinate.

Do not pre-invent a large availability/status ontology. Additional forms such as PermissionRequired,
Unavailable or Loading must be earned by concrete product pressure.

## QuickActionCluster / FAB intention

The top-right action circles are useful pressure for a reusable **intent**, not for copying one
specific FAB implementation.

~~~text
QuickActionCluster
= persistent high-availability actions
  anchored to workspace chrome
  independent of ordinary document flow
~~~

Potential manifestations may differ:

~~~text
Compose
-> FAB / action-row style realization

Web
-> floating circular action cluster

another target
-> another equivalent manifestation
~~~

The stable source relationship should describe grouping, anchoring, interaction and depth rather than
hard-coding one renderer's widget vocabulary.

## Dialog / modal intention

Likewise, Dialog should first be understood through its relationship:

~~~text
ModalInteraction
-> presents above parent surface
-> blocks or scopes lower interaction
-> owns a bounded focus relationship
-> returns/dispatches a result or typed Control
~~~

Whether web later uses native `<dialog>`, another DOM primitive, or a platform-specific mechanism is a
renderer realization decision.

## Nested browser boundary

The first browser implementation may use ordinary/older DOM or iframe techniques.

That is acceptable.

The semantic boundary remains:

~~~text
NestedBrowserSurface
├── human-visible document surface
├── current document identity
└── Vex context-perception binding = initially UNBOUND
~~~

Permanent:

~~~text
HUMAN NESTED RENDER
!= VEX CONTEXT ACCESS

VISIBLE PAGE
!= VEX CONTEXT CURRENT
~~~

VexWeb may own browser manifestation mechanics. It does not acquire the semantic authority for what
context Vex may perceive.

## Generated artifact law

Humans/AI author VexWeb relationship source.

They do not normally author generated HTML/CSS/JS.

~~~text
Kotlin VexWeb relationships
        ↓
validated / normalized VexWeb projection
        ↓
deterministic generator
        ↓
HTML
CSS
JS
semantic manifest
~~~

Generated artifacts are compiler/build outputs.

~~~text
GENERATED CSS
!= SOURCE ARCHITECTURE

GENERATED JAVASCRIPT
!= SOURCE INTENTION

MANUAL GENERATED-FILE EDIT
-> verification failure / regeneration
~~~

Meaningful values should come from named/typed parameters when that improves provenance.

~~~text
padding = Space.Panel
width   = Size.CompanionPanel

not

"14px"
"357px"
with no source relationship
~~~

Do not wrap every primitive merely for symmetry. Parameter names a meaningful dimension, not a
storage type.

## Source-addressable DOM

Generated browser artifacts should preserve Vex source identity.

Conceptual example:

~~~html
<div data-vex-ref="element.vexlife-workspace.browser.viewport"></div>
~~~

Later this allows:

~~~text
current DOM node
-> exact Vex ref
-> Blueprint element
-> VexWeb manifestation relationship
-> product/source intention
~~~

That bridge is important for later browser-context perception.

## First VEXWEB-F0 pressure specimen

Do not begin by implementing the entire screenshot.

The first specimen should exercise the smallest relationship set that still pressures the important
architecture:

~~~text
VexLifeWorkspace
└── SplitContainer
    ├── BrowserPane
    │   └── BrowserPlaceholder
    │
    ├── SplitHandle
    │
    └── CompanionPane
        └── ConversationView
            └── Composer
                └── SendControl
                    └── NotYetImplemented feedback

OverlayRoot
└── Snackbar
~~~

This slice pressures:

~~~text
stable Blueprint identity
nested manifestation containers
parent / child relationships
layout
split / resize / collapse presentation
depth
overlay priority
interaction
unbound capability
transient feedback
generated artifact identity
~~~

without requiring:

~~~text
real nested browser provider
real Conversation backend
model inference
Memory
Settings backend
DOM context projection for Vex
~~~

After this fractal container model survives pressure, the next strong specimen is QuickActionCluster.

## Completed-view law

> A VexWeb surface is a nested graph of identifiable views whose parent/child, placement, depth,
> presentation, interaction, capability-binding and fallback relationships remain explicit until
> deterministic browser generation.

The initial construction sequence is:

~~~text
completed human view
-> VEXWEB-F0 vocabulary pressure
-> compiler-visible VexWeb relationships
-> deterministic browser artifact compiler
-> generated-artifact membrane
-> Compose/VexWeb meaning parity
-> inert VexLife web specimen
-> nested-browser practicum
~~~

## Deliberately not decided yet

This snapshot does not yet standardize:

~~~text
exact Kotlin DSL syntax
exact VexWeb class/type names beyond the working vocabulary
CSS property mapping
HTML tag choices
JS runtime structure
responsive breakpoint system
accessibility mapping
browser state holder
split persistence
dialog implementation
nested-browser provider implementation
final output directory structure
~~~

Those choices must descend from F0/F1 pressure rather than become precedent because this document
needed examples.

<!-- [VXG RealForever][VEXLIFE-PROD][VEXWEB][COMPLETED-VIEW-BLUEPRINT] -->
