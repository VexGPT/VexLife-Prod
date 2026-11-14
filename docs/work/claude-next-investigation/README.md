# Claude next investigation work notes

`[VXG RealForever]` · work notes, **not authority** · preparation only

This directory is a neutral landing place for Victor's next VexLife-Prod investigation.

It exists so Claude can begin from current Product source and preserve what is already known without
turning the preparation itself into a product decision, implementation plan, or new semantic owner.

## Enter

Read in this order before substantive investigation:

~~~text
README.md
-> AGENTS.md
-> docs/CULTURE.md
-> docs/foundation/INDEX.md
-> this work note
-> exact source / tests implicated by Victor's check
~~~

Current preparation base:

~~~text
repository = VexGPT/VexLife-Prod
main       = c06da8573dad3d2d5bd3ba7c4b50777a7b80cf9a
~~~

Re-ground main and any implicated owner/source when the investigation actually begins. This note is
not a currentness oracle.

## Victor's next check

**Intentionally not filled in yet.**

When Victor supplies the check:

1. preserve the question in ordinary language before translating it into repository vocabulary;
2. separate observed facts, source-derived expectations, hypotheses, and unknowns;
3. identify the exact owner and smallest source/test surface that can answer it;
4. investigate before proposing new structure;
5. do not mutate Product source merely because an implementation is imaginable;
6. if implementation is actually required, state what missing relationship/capability requires it and
   keep the change inside its rightful owner.

## Known context to keep, not to solve here

VexLife-Prod already owns a Product Screen Lens export:

~~~text
src/jvmMain/kotlin/vexlife/lens/VexLifeProdScreenLens.kt
-> docs/lens/vexlife-prod.screen-lens.json
-> vexboard.screen-lens/v1
~~~

The remaining VexBoard gap is on the VexWork/VexBoard side: it still needs a bounded ingestion /
registration path for the already-exported Product Lens.

~~~text
PRODUCT LENS ALREADY EXISTS
!= VEXBOARD CONNECTION ALREADY IMPLEMENTED

MISSING VEXBOARD CONNECTION SEAM
!= MISSING VEXLIFE-PROD UI MODEL
~~~

That seam is **context only** for the next investigation. Do not implement it from this preparation PR
and do not assume Victor's next check is about VexBoard unless the check actually implicates it.

## Boundary of this preparation PR

This PR should contain only this work-note entry surface.

It does not:

- change Product behavior;
- change a Foundation;
- register VexLife-Prod in VexBoard;
- claim Stable/Main acceptance;
- create a new Product owner;
- decide what Victor's next check means before he supplies it.

If the investigation earns durable product truth, move that truth to its rightful Foundation/source
owner instead of letting these work notes become permanent authority.

<!-- [VXG RealForever][VEXLIFE-PROD][CLAUDE][NEXT-INVESTIGATION][WORK-NOTES] -->
