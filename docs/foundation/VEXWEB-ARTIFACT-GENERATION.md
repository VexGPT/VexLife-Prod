# VexWeb deterministic artifact generation

[VXG RealForever]

## Human question

Can the accepted VexWeb relationship source produce browser-native artifacts deterministically without
making HTML, CSS or JavaScript a second product/spatial authority?

## Current relationship

~~~text
accepted VexWeb Surface
  identity / source parent / manifestation container
  Frame / Seam / Region / Allocation
  Depth / Presentation / Interaction / Binding / Fallback
        ↓
VexWebArtifacts.generate(...)
        ↓
index.html
vexweb.css
vexweb.js
vexweb-manifest.json
~~~

The generator consumes validated VexWeb objects. It does not reinterpret invalid source into a
renderable result.

Permanent:

~~~text
VEXWEB ARTIFACT GENERATOR
!= PRODUCT SEMANTIC OWNER
!= SPATIAL SOURCE OWNER
!= CONVERSATION OWNER
!= PROVIDER/RUNTIME OWNER

GENERATED HTML/CSS/JS
!= AUTHORING SOURCE
~~~

## Geometry realization

Frame / Seam / Region remain the authored language.

The generator derives exact rational normalized geometry from those accepted relationships and
projects it into renderer percentages. This derivation is realization mechanics, not a second
authoring language.

~~~text
authored:
  named Seam
  Allocation.Ratio
  Region edge -> Frame edge / Seam

generated:
  normalized rational position
  deterministic percent
  browser layout instruction
~~~

The semantic manifest retains stable Vex coordinates and generated normalized geometry so a later
projector can walk from browser artifact back to source identity.

## Source-addressable browser output

Generated nodes retain exact Vex refs through data-vex-ref.

The manifest separately preserves:

~~~text
ref
sourceParentRef
containerRef
Placement
Depth
Presentation
Interaction
Binding / Fallback
Frame / Seam / Region geometry projection
~~~

DOM containment therefore does not replace Blueprint source parentage.

## Current runtime boundary

F2 realizes one already-earned fallback behavior:

~~~text
Unbound
-> NotYetImplemented(capability)
-> exact transient feedback target
~~~

The generated JavaScript may show that explicit feedback. It does not invoke the local product
service or invent provider state.

Current held runtime relationships remain:

~~~text
dynamic Seam resize/collapse state
nested-browser provider
Conversation/service binding
browser-context admission
Memory / CLEAN_START / working-checkpoint continuation
~~~

## Determinism pressure

The same Surface must emit byte-identical artifacts on repeated generation.

Tests additionally pressure:

~~~text
current VexLife specimen
-> exact source-addressable HTML
-> 50/50 relational geometry realization
-> explicit unbound Send fallback

irregular three-region specimen
-> same generator
-> partial-Seam geometry
-> no special three-pane renderer type
~~~

## Lifecycle

F1 relational authoring is accepted source at merge c2ed37471eab9f163acd2b0cfaf979d7cdeead73
with accepted tree 9d6a6096e065c4d6582b2da086237ddc5ed19d67.

This F2 source remains a candidate until the repository-owned Kotlin/JVM + Kotlin/JS regression
exercises the exact candidate.

~~~text
F1 ACCEPTED
!= F2 ACCEPTED

F2 ACCEPTED
!= GENERATED P1 PRODUCT CONNECTED
~~~

<!-- [VXG RealForever][VEXWEB][F2][ARTIFACT-GENERATION] -->


## Accepted threshold

F2 deterministic browser-artifact generation is accepted source.

~~~text
A002 full behavioral regression
  compileKotlinJvm      PASS
  jvmTest               PASS
  compileKotlinJs       PASS
  compileTestKotlinJs   PASS

A003 post-A002 compatibility delta
  compileKotlinJvm      PASS
  compileKotlinJs       PASS

accepted main
cf7de23e2979cc49133e2de4c5eee841d5bcf584

accepted tree
c20e92d48aab1296da52596691e898cad9cd6c21
~~~

The historical lifecycle section above remains provenance for the candidate state before acceptance.

Permanent:

~~~text
F2 DETERMINISTIC ARTIFACT GENERATION ACCEPTED
!= GENERATED P1 WORKSPACE ACCEPTED
!= RUNTIME CONVERGENCE COMPLETE
~~~

<!-- [VXG RealForever][VEXWEB][F2][ARTIFACT-GENERATION][ACCEPTED] -->
