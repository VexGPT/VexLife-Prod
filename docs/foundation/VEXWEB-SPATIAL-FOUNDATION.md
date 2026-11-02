# VexWeb relational spatial foundation

`[VXG RealForever]`

## Human law

The spatial language must scale by the same nouns rather than requiring new vocabulary when the number
or arrangement of regions changes.

Permanent:

~~~text
ADDING REGIONS
!= REVISITING THE SPATIAL LANGUAGE

TWO
THREE
FOUR
IRREGULAR
-> SAME RELATIONAL GRAMMAR
~~~

A binary decomposition noun was tested and rejected even after technical proof because expansion would
make the source describe construction history rather than stable spatial relationships.

## Current spatial grammar

~~~text
Frame
├── outer boundaries
│   ├── Start
│   ├── End
│   ├── Top
│   └── Bottom
│
├── Seams
│   ├── named orientation
│   ├── relational position between parallel boundaries
│   ├── Allocation
│   └── span between perpendicular boundaries
│
├── Regions
│   ├── start boundary
│   ├── end boundary
│   ├── top boundary
│   ├── bottom boundary
│   └── content Node
│
└── chrome
    └── optional Handles bound to Seams
~~~

This says **where something is** rather than narrating how the container was repeatedly divided to get
there.

## Seam

A Seam is a stable internal boundary.

Vertical Seams are positioned between X boundaries and span between Y boundaries.

Horizontal Seams are positioned between Y boundaries and span between X boundaries.

A Seam may span the whole Frame or only part of it.

That partial-span rule is what lets the same language express an irregular three-region arrangement:

~~~text
┌────────────┬────────────┐
│            │  Region 2  │
│  Region 1  ├────────────┤
│            │  Region 3  │
└────────────┴────────────┘

one full-height Vertical Seam
+
one Horizontal Seam whose span begins at that Vertical Seam
~~~

No three-region special type exists.

## Allocation

Current authored placement uses:

~~~text
Allocation.Ratio(
  beforeParts,
  afterParts
)
~~~

Examples:

~~~text
1 : 1
-> center

2 : 1
-> two thirds before the Seam / one third after

1 : 2
-> one third before / two thirds after
~~~

The Allocation is attached to a named Seam relationship rather than emitted as a raw percentage.

Future allocation forms may be added only when concrete pressure earns them, without changing the
Frame / Seam / Region grammar.

## Region

A Region is source-addressable and binds four edges:

~~~text
Region
  start  -> Frame.Start or Vertical Seam
  end    -> Frame.End   or Vertical Seam
  top    -> Frame.Top   or Horizontal Seam
  bottom -> Frame.Bottom or Horizontal Seam
~~~

This makes perceptive orientation recoverable directly from source.

A tool can answer:

~~~text
Where is this content?

Frame   = WorkspaceFrame
start   = Frame.Start
end     = BrowserCompanionSeam
top     = Frame.Top
bottom  = Frame.Bottom
~~~

without reverse-engineering a nested layout history.

## Two / three / four scale through the same nouns

Two regions:

~~~text
Frame
-> one Seam
-> two Regions
~~~

Three regions:

~~~text
Frame
-> one full Seam
-> one partial Seam
-> three Regions
~~~

Four quadrants:

~~~text
Frame
-> one Vertical Seam
-> one Horizontal Seam
-> four Regions
~~~

Larger arrangements continue by adding named relational boundaries and Regions. A new container
ontology is not earned merely because N increases.

## Handle is not Seam

Permanent:

~~~text
Seam
= spatial boundary relationship

Handle
= optional interactive manifestation bound to that Seam
~~~

A Seam may exist with:

~~~text
no visible handle
a draggable divider
collapse controls
hover-only affordance
touch gesture
another renderer-specific manifestation
~~~

without changing spatial meaning.

## Matrix / coordinates / graph

These are useful **derived perceptual projections**, not the current source language.

A regular arrangement may project naturally to a matrix.

A Region may later project to normalized coordinates such as:

~~~text
x 0.00 -> 0.50
y 0.50 -> 1.00
~~~

A relationship inspector may visualize the Frame as a graph.

Permanent:

~~~text
DERIVED MATRIX
!= SOURCE AUTHORITY

DERIVED COORDINATES
!= AUTHORED PIXELS

RELATIONSHIP GRAPH
!= REQUIREMENT TO AUTHOR GENERIC GRAPH EDGES
~~~

The source remains the smaller constrained geometry language.

## Constraint-language boundary

VexWeb does not currently accept arbitrary equations such as:

~~~text
left = other.right + 14
width = parent.width * 0.45
~~~

That would be expressive but would recreate low-level implementation archaeology.

Current source stays bounded to:

~~~text
Frame edges
Seams
Regions
Allocations
Depth
~~~

If a real product relationship cannot be expressed truthfully through this grammar, that concrete
failure may earn a more general constraint layer later.

## VexLife first specimen

~~~text
WorkspaceFrame
├── BrowserCompanionSeam
├── BrowserRegion
│   └── Browser / BrowserViewport
├── CompanionRegion
│   └── Companion / Conversation / Composer / Send
└── SeamHandle
    -> LocalSpatial(BrowserCompanionSeam)

OverlayRoot
└── Snackbar
~~~

Send remains truthfully unbound:

~~~text
Send
-> NotYetImplemented(conversation.send)
-> Snackbar
~~~

## Held

This foundation does not yet establish:

~~~text
HTML
CSS
JavaScript
responsive breakpoints
pixel coordinates
generic constraint equations
dynamic spatial runtime state
generator output
browser provider
Conversation/model hookup
DOM context projection
~~~

<!-- [VXG RealForever][VEXWEB][SPATIAL][FRAME-SEAM-REGION] -->
