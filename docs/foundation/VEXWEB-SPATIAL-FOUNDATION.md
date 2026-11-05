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


## A001 proof PASS / A002 geometry-validity hardening

A001 exact RETURN:

~~~text
sha256 3249350cc4c1bef19865f300e319aa6037de9015c213e6dea7a2f060a7b03abb

compileKotlinJvm      PASS
jvmTest               PASS
compileKotlinJs       PASS
compileTestKotlinJs   PASS
clean worktree        PASS
~~~

The same Frame / Seam / Region nouns passed the 2 / 3 / 4 region pressure.

Threshold review found one additional requirement before acceptance:

~~~text
source relationship
-> must resolve to valid geometry
-> renderer must not repair or guess
~~~

A002 therefore resolves Allocation relationships internally into normalized Frame coordinates for
validation only.

That derived resolution now rejects:

~~~text
same-axis Seam dependency cycle
reversed Seam allocation interval
reversed / empty Seam span
reversed / empty Region rectangle
Region edge using a Seam that does not cover that full edge
~~~

Example:

~~~text
partial Vertical Seam spans only top half

Region:
  start  = Frame.Start
  end    = partial Vertical Seam
  top    = Frame.Top
  bottom = Frame.Bottom

-> INVALID

because the referenced Seam does not exist along the Region's entire end edge.
~~~

Permanent:

~~~text
INTERNAL NORMALIZED RESOLUTION
= VALIDATION / FUTURE INSPECTION SUPPORT

INTERNAL NORMALIZED RESOLUTION
!= AUTHORED PIXEL COORDINATES
!= SECOND SPATIAL LANGUAGE
~~~

The authored spatial grammar remains unchanged:

~~~text
Frame
Seam
Region
Allocation
Depth
~~~

<!-- [VXG RealForever][VEXWEB][SPATIAL][A002-GEOMETRY-VALIDITY] -->


## Accepted threshold

The relational spatial foundation is accepted source.

~~~text
proof RETURN
sha256 7e9066f9aacbd535a9e75a67b99d13014cdafab51fe52bd22cc12858e55ace8b

proven / accepted tree
d109f0d648e1adeba35796b0dd0dc766d1dea4c1

accepted main
e0e8ad6a95f3c73cd7f79216b3ae1802ba9b54f9
~~~

Exact proof:

~~~text
compileKotlinJvm      PASS
jvmTest               PASS
compileKotlinJs       PASS
compileTestKotlinJs   PASS
clean worktree        PASS
post-merge tree       EXACT
~~~

Acceptance includes:

~~~text
same nouns at 2 / 3 / 4 regions
partial Seam for irregular geometry
ratio Allocation bound to named Seam
same-axis cycle rejection
forward / non-empty geometry validation
Seam coverage validation for Region edges
Handle != Seam
derived normalized geometry != authored coordinates
~~~

Permanent:

~~~text
RELATIONAL SPATIAL FOUNDATION ACCEPTED
!= AUTHORING SURFACE ACCEPTED
!= ARTIFACT GENERATOR ACCEPTED
~~~

<!-- [VXG RealForever][VEXWEB][SPATIAL][ACCEPTED] -->


## F1 relational authoring candidate

The accepted spatial vocabulary remains unchanged.

~~~text
Frame
Seam
Region
Allocation
Depth
~~~

F1 adds only a compiler-visible authoring surface over those exact objects:

~~~text
VexWebAuthoring.surface
  -> exactly one content root
  -> exactly one overlay root

VexWebAuthoring.frame
  -> verticalSeam
  -> horizontalSeam
  -> region
  -> chrome

VexWebAuthoring.element
VexWebAuthoring.structural

binding helpers
  -> localSpatial
  -> unbound
~~~

The authoring layer forms the existing `VexWeb.Surface`, `Frame`, `Seam`, `Region`,
`Element` and `Structural` values. Their accepted constructors and validators remain authority.

Permanent:

~~~text
AUTHORING SURFACE
!= NEW SPATIAL LANGUAGE

AUTHORING SURFACE
!= RENDERER

AUTHORING SURFACE
!= HTML / CSS / JAVASCRIPT

AUTHORING SURFACE
!= GENERATOR
~~~

The earlier Split-based F1 experiment remains rejected. Its technical success does not revive its
binary-decomposition noun.

~~~text
REJECTED:
  Split
  recursive binary decomposition
  authored axis as foundational spatial language

CURRENT:
  stable Frame / Seam / Region relationships
~~~

Candidate source pressure proves:

~~~text
direct accepted object construction
== relational authoring output

2 regions
3 irregular regions with partial Seam
4 quadrants
-> same F1 authoring nouns

missing Surface roots
-> fail closed

interactive Node without Binding
-> accepted VexWeb validator fails closed
~~~

The current VexLife workspace specimen is re-authored through F1 while retaining the same stable
Blueprint/structural/seam identities.

Held before F1 acceptance:

~~~text
exact Kotlin 2.4.20 JVM compile + common tests
exact Kotlin 2.4.20 JS IR compile + test compile
review that no renderer/generator meaning leaked into authoring

F2 deterministic artifact generation
browser runtime
Conversation/service binding into generated output
~~~

<!-- [VXG RealForever][VEXWEB][F1][RELATIONAL-AUTHORING] -->


## F1 accepted threshold

The F1 relational authoring surface is accepted source.

~~~text
SourceOwner A003 outer RETURN
sha256 66574b077d3b8fdf7a1d999ad1fe83a33d51d81d8bb13d14e090c25b91b7522c

inner canonical RETURN
sha256 ff3ba60fa14327f0eb68ee5e358a93c7decced7d604d9fe6681f30d80d6f1aa2

proven candidate
head effad48ce998787bafecbcdaaa7ebadd717f181b
tree 9d6a6096e065c4d6582b2da086237ddc5ed19d67

accepted main
c2ed37471eab9f163acd2b0cfaf979d7cdeead73
accepted tree
9d6a6096e065c4d6582b2da086237ddc5ed19d67
~~~

Exact changed-path regression:

~~~text
compileKotlinJvm      PASS
jvmTest               PASS
compileKotlinJs       PASS
compileTestKotlinJs   PASS
post-merge tree       EXACT
~~~

Acceptance retains:

~~~text
F1 emits accepted VexWeb objects
accepted constructors / validators remain authority
same authoring nouns at 2 / 3 / 4 regions
no Split resurrection
no renderer / generator meaning in F1
~~~

The historical candidate section above remains provenance for what was under pressure before
acceptance.

Permanent:

~~~text
F1 RELATIONAL AUTHORING ACCEPTED
!= F2 ARTIFACT GENERATOR ACCEPTED
!= GENERATED P1 PRODUCT CONNECTED
~~~

<!-- [VXG RealForever][VEXWEB][F1][RELATIONAL-AUTHORING][ACCEPTED] -->
