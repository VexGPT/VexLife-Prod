# VexWeb F1 — compositional authoring surface

`[VXG RealForever]`

## Question

Can the accepted F0 vocabulary be authored as a readable fractal Kotlin tree without creating new
manifestation meaning or beginning platform generation?

F1 deliberately keeps:

~~~text
F0 vocabulary
= meaning

F1 authoring surface
= composition ergonomics
~~~

## Pressure

The constructor-heavy F0 specimen proved the vocabulary but made the completed-view relationship tree
harder to perceive at a glance.

F1 should let source read in the same direction as the completed view:

~~~text
Surface
├── content
│   └── split
│       ├── leading
│       │   └── Browser
│       │       └── BrowserViewport
│       ├── handle
│       │   └── SplitHandle
│       └── trailing
│           └── Companion
│               └── Conversation
│                   └── Composer
│                       └── Send
└── overlayRoot
    └── Overlay
        └── Snackbar
~~~

while still producing the exact accepted F0 Node/Surface types.

## Compiler-visible authoring rules

F1 introduces an internal Kotlin DSL marker and bounded scopes.

~~~text
SurfaceScope
  content { exactly one Node }
  overlayRoot { exactly one Node }

SplitScope
  leading { exactly one Node }
  handle { exactly one Node }
  trailing { exactly one Node }

ChildrenScope
  zero or more nested Nodes
~~~

Split roles remain named roles, not child-order convention.

Interactive authoring uses separate functions:

~~~text
element(...)
structural(...)

interactiveElement(
  interactions,
  binding,
  ...
)

interactiveStructural(
  interactions,
  binding,
  ...
)
~~~

so ordinary authoring cannot accidentally imply that an interaction's Binding is optional.

The accepted F0 runtime invariants remain the final validator.

## No new semantic vocabulary

F1 does not add:

~~~text
new Depth classes
new Placement values
new Presentation values
new Interaction values
new Binding kinds
new Fallback kinds
new SplitMode values
HTML / CSS / JS
generator output
browser runtime
~~~

If the authoring surface cannot express a future relationship, that pressure must return to the
rightful vocabulary layer rather than being hidden in DSL magic.

## Typed lookup

F1 adds source-typed lookup helpers:

~~~text
Surface.requireNode(StructuralRef)
Surface.requireNode(VexUiElementRef)
~~~

so tests/tooling can recover authored nodes without retyping identity strings.

## Required proof

Before F1 acceptance:

~~~text
exact root JVM compile/test
exact root JS compile/test

existing F0 semantic tests remain PASS

and:
- F1-authored F0 specimen preserves accepted F0 coordinates;
- split roles remain explicit;
- missing split roles fail;
- no HTML/CSS/JS vocabulary appears;
- F1 does not reopen accepted F0 meaning.
~~~

<!-- [VXG RealForever][VEXWEB][F1][AUTHORING] -->


## A001 proof PASS / recursive split scalability pressure

F1 A001 exact RETURN:

~~~text
sha256 3da3493468bc36d7db608205c2c74ee33d4407a621329dfccbd17b574d5db00f

compileKotlinJvm      PASS
jvmTest               PASS
compileKotlinJs       PASS
compileTestKotlinJs   PASS
clean worktree        PASS
~~~

Before acceptance, human pressure asked whether `Split` is reusable beyond two panes, including a
four-player split-screen experience.

The existing F0 vocabulary already has a fractal property:

~~~text
Split.leading  : Node
Split.trailing : Node

Node may itself be another Split
~~~

So the smallest reusable answer is recursive composition, not a new `Grid`, `MultiSplit`, or
`Partition` abstraction.

The real missing intention was orientation. F1 A002 therefore adds one presentation parameter:

~~~text
SplitAxis
  Horizontal
  Vertical
~~~

A four-pane 2x2 layout is then:

~~~text
Split(axis = Horizontal)
├── leading -> Split(axis = Vertical)
│   ├── PlayerOne
│   └── PlayerTwo
└── trailing -> Split(axis = Vertical)
    ├── PlayerThree
    └── PlayerFour
~~~

This preserves the fractal law: larger pane topologies are composed from the same relationship rather
than switching to a second layout language.

Do not generalize further yet.

A broader N-way Partition is earned only if concrete pressure appears that recursive binary Split does
not express cleanly, such as:

~~~text
synchronized resizing across N siblings
dynamic pane counts as one maintained relationship
non-binary weighting that becomes unreadable through recursion
another invariant that truly belongs to the group as a whole
~~~

Permanent:

~~~text
MORE PANES
!= AUTOMATICALLY A NEW CONTAINER ONTOLOGY

RECURSIVE SPLIT
+ AUTHORED AXIS
= CURRENT SMALLEST FRACTAL ANSWER
~~~

<!-- [VXG RealForever][VEXWEB][F1][A002-SPLIT-SCALABILITY] -->
