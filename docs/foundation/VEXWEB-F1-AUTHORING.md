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
