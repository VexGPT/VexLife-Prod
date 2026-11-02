# VexWeb completed-view blueprint

`[VXG RealForever]`

## Purpose

This is the durable human-intention snapshot for the VexLife nested-browser experience before
renderer/platform syntax is allowed to become architecture.

The supplied design evidence shows:

- persistent VexLife chrome;
- browser/query/app controls;
- quick-action/FAB-like controls;
- a nested browser surface;
- a resizable/collapsible browser/companion boundary;
- Conversation and Settings companion views;
- message/thought/reasoning surfaces;
- composer + Send;
- capability/resource/settings projections;
- transient/modal surfaces.

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

## Completed-view relationship graph

~~~text
VexLifeWorkspace
│
├── ShellChrome
│   ├── ProductIdentity
│   │   ├── Logo
│   │   └── ProductName
│   ├── BrowserQuery
│   ├── AppSelector
│   └── QuickActionCluster
│       └── QuickAction ...
│
├── WorkspaceBody
│   └── WorkspaceFrame
│       ├── BrowserCompanionSeam
│       │   └── optional SeamHandle
│       ├── BrowserRegion
│       │   └── NestedBrowserSurface
│       └── CompanionRegion
│           ├── CompanionHeader
│           └── CompanionContent
│               ├── ConversationView
│               │   ├── Timeline
│               │   │   ├── Message
│               │   │   ├── ThoughtDisclosure
│               │   │   └── ReasoningSurface
│               │   └── Composer
│               │       ├── Input
│               │       └── SendControl
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
    ├── QuickActionOverlay
    ├── Snackbar / transient feedback
    ├── Dialog
    ├── Popover / menu
    └── Modal surface
~~~

The graph is orientation evidence, not a requirement to mint one type for every noun.

## Fractal view law

Every meaningful coordinate should answer the same questions regardless of depth:

~~~text
View
├── Identity
├── source Parent
├── manifestation Container
├── Children
├── Spatial Region / boundaries where applicable
├── Depth
├── Presentation
├── Interaction
├── Binding
└── Fallback
~~~

A fresh worker should be able to descend from Workspace to Send or enter at Send and recover the
surrounding relationships without implementation archaeology.

## Product identity versus manifestation containment

~~~text
BLUEPRINT IDENTITY
!= MANIFESTATION CONTAINMENT
!= DOM TREE PATH
!= CSS SELECTOR PATH
~~~

A Blueprint-owned element preserves its accepted source parent even when VexWeb manifests it inside an
additional spatial/container relationship.

## Permanent spatial scalability law

Victor's human law:

> Expansion must scale the language fractally rather than requiring revisitation of the language.

Therefore the foundation does not use a binary decomposition noun as spatial truth.

The stable spatial grammar is:

~~~text
Frame
-> local coordinate/container space

Seam
-> named internal boundary

Region
-> four edges bound to Frame edges / Seams

Allocation
-> relational position of a Seam

Handle
-> optional interaction bound to a Seam

Depth
-> independent overlap relationship
~~~

### Two regions

~~~text
Frame
-> one Seam
-> two Regions
~~~

### Three regions

~~~text
Frame
-> one full Seam
-> one partial Seam
-> three Regions
~~~

The partial Seam may begin/end on another Seam.

### Four quadrants

~~~text
Frame
-> one Vertical Seam
-> one Horizontal Seam
-> four Regions
~~~

The nouns do not change when N changes.

## Perceptive orientation

The source should make location explainable in visual terms.

Example:

~~~text
BrowserRegion
Frame   = WorkspaceFrame
start   = Frame.Start
end     = BrowserCompanionSeam
top     = Frame.Top
bottom  = Frame.Bottom
~~~

That relationship can later derive:

- a matrix view when the layout is regular;
- normalized coordinates for inspection/debugging;
- a relationship graph;
- renderer-specific layout instructions.

Those projections are not source authority.

## Seam and Handle are different

~~~text
Seam
= geometry / boundary truth

Handle
= optional interactive manifestation
~~~

A renderer may show a draggable bar, arrows, gesture target, or no control at all while the Seam
relationship remains unchanged.

## Allocation

Current pressure earns ratio allocation:

~~~text
Allocation.Ratio(beforeParts, afterParts)
~~~

This keeps numbers bound to a named spatial relationship.

Raw CSS percentages/pixels are not the authoring meaning.

## Depth / layering law

Authored source preserves relative depth:

~~~text
Content
< Chrome
< Transient
< Modal
~~~

The renderer may choose platform-specific stacking values.

~~~text
AUTHORED DEPTH RELATIONSHIP
!= NUMERIC Z-INDEX AUTHORITY
~~~

## Honest incomplete UI

A visible control may exist before its real capability is connected.

~~~text
Send
-> Unbound
-> NotYetImplemented(conversation.send)
-> exact Snackbar target
~~~

The interface can therefore be built without silent no-ops or fake provider state.

## QuickActionCluster / FAB intention

~~~text
QuickActionCluster
= persistent high-availability actions
  anchored to workspace chrome
  independent of ordinary document flow
~~~

The stable relationship describes grouping, anchoring, interaction and depth. Compose/Web may manifest
it differently.

## Dialog / modal intention

~~~text
ModalInteraction
-> presents above parent surface
-> scopes/blocks lower interaction
-> owns bounded focus
-> returns/dispatches a result or typed Control
~~~

Platform dialog mechanics are renderer decisions.

## Nested browser boundary

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

## Generated artifact law

Humans/AI author VexWeb relationship source.

They do not normally author generated HTML/CSS/JS.

~~~text
VexWeb relationship source
        ↓
validated / normalized projection
        ↓
deterministic generator
        ↓
HTML
CSS
JS
semantic manifest
~~~

Permanent:

~~~text
GENERATED CSS
!= SOURCE ARCHITECTURE

GENERATED JAVASCRIPT
!= SOURCE INTENTION

MANUAL GENERATED-FILE EDIT
-> verification failure / regeneration
~~~

## Source-addressable DOM

Generated browser artifacts should preserve exact Vex identity.

Conceptually:

~~~html
<div data-vex-ref="element.vexlife-workspace.browser.viewport"></div>
~~~

so later:

~~~text
DOM node
-> Vex ref
-> Blueprint element
-> VexWeb relationship
-> product/source intention
~~~

## First spatial pressure specimen

~~~text
WorkspaceFrame
├── BrowserCompanionSeam
├── BrowserRegion
│   └── BrowserViewport
├── CompanionRegion
│   └── Conversation
│       └── Composer
│           └── Send
└── SeamHandle

OverlayRoot
└── Snackbar
~~~

This pressures stable identity, relational geometry, depth, interaction, explicit unbound capability,
and transient feedback without requiring browser/model/provider connections.

## Completed-view law

> A VexWeb surface is a graph of identifiable views and relational spatial boundaries whose source
> parent, manifestation containment, region boundaries, depth, presentation, interaction,
> capability-binding and fallback relationships remain explicit until deterministic realization.

## Deliberately not decided yet

~~~text
final Kotlin authoring DSL
HTML tags
CSS properties
JavaScript runtime structure
responsive breakpoints
accessibility mapping
dynamic spatial state holder
generic constraint equations
nested-browser provider
DOM context projection
final generated output layout
~~~

Those decisions must be earned by concrete pressure.

<!-- [VXG RealForever][VEXLIFE-PROD][VEXWEB][COMPLETED-VIEW-BLUEPRINT] -->
