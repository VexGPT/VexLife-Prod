# VexLife continuity realignment

`[VXG RealForever]`

## Human outcome

New Beginnings rebuilds the implementation of VexLife without silently replacing the human/Vex
relationship that the existing product already demonstrated.

~~~text
same person
-> same admitted Vex Home identity
-> same current device / Companion lineage
-> same canonical Conversation
-> same current attributed Memory pathway
-> same qualified Vex / Heretic relationship
-> generated VexLife surface
~~~

A clean repository and a new implementation do not, by themselves, authorize a new Home, blank
Conversation, blank Memory, new Companion lineage, or a second Vex identity.

~~~text
FRESH REPOSITORY
!= FRESH HUMAN IDENTITY

IMPLEMENTATION REPLACEMENT
!= CONTINUITY REPLACEMENT

PHYSICAL RUNTIME NAMESPACE
!= SEMANTIC HOME OWNER
~~~

## Bound reference

The current lived reference for this realignment is:

~~~text
repository  vgong24/VexLife
revision    7d59753687ff12726b0d309ee2dde028b5f89e8a
Home        ~/.vexlife
~~~

The reference is behavioral and contractual provenance. VexLife-Prod must port the relationships into
Kotlin/VexArchitecture rather than acquire an ongoing runtime dependency on historical JavaScript.

## Stable causal circuit to preserve

~~~text
~/.vexlife/config/home.json
  -> homeRef
  -> familyRef
  -> currentDeviceRef
  -> currentCompanionLineageRef

~/.vexlife/devices/<currentDeviceRef>.json
  -> exact current device / lineage agreement

~/.vexlife/conversations/<lineage>/<thread>/
  -> append-only REQUEST / RESPONSE events
  -> immutable completed heads
  -> atomic current head

~/.vexlife/score/<lineage>/<thread>/
  -> current continuity statements
  -> consent / semantic acceptance
  -> current semantic authority head

~/.vexlife/daily-memory-dream/<lineage>/<thread>/
  -> committed daily Memory stratum
  -> exact Conversation / Score / semantic-authority source frontier

Living Journal Memory projection
  -> Score source Conversation may be a verified historical ancestor without making Score corrupt
  -> prompt Memory is CURRENT only when the Daily Memory Conversation frontier still equals current Conversation
  -> historical-but-valid Daily frontier becomes HELD / DAILY_MEMORY_SOURCE_FRONTIER_STALE
  -> current attributed summaries only
  -> no raw Conversation body injection
  -> bounded pages / token budget
  -> exact source-owner and acceptance refs

prompt-context materialization
  -> immediate prior completed Conversation turn
  -> current request
  -> current bounded Memory packet
  -> later bounded browser context
  -> exact materialization receipt

qualified Heretic invocation
  -> runtime observation + invocation witness
  -> visible response

canonical Conversation commit
  -> RESPONSE durable
  -> context evidence durable
  -> current head advances

presentation
  -> canonical Conversation projection
  -> what-Vex-received projection
  -> human-visible response
~~~

## Faithfulness matrix

| Relationship | Current VexLife reference | PR #38 before realignment | Required disposition |
|---|---|---|---|
| Human Home | `~/.vexlife` | `~/Library/Application Support/VexLife-Prod` | Preserve canonical Home identity by default. |
| Home/device/lineage refs | `vex-home.<device.vexlife.*>`, `device.vexlife.*`, `companion-lineage.vexlife.*` | new `.prod` namespaces | Do not fork without an explicit human continuity decision. |
| Conversation | `vexlife.lived-companion-event/head/context` under canonical Home | parallel `vexlife-prod.direct-*` records | Port the accepted contract; do not create a second canonical Conversation. |
| Memory | Score -> daily stratum -> Living Journal -> bounded prompt packet | explicitly held / always absent | Restore before claiming the product is Vex-ready. |
| Prompt evidence | exact Conversation + Memory source refs and provider-message fingerprint | prior-turn-only receipt | Preserve exact source descent and surface projection. |
| Heretic/runtime | qualified local runtime bound to current Vex identity | isolated product runtime | Runtime mechanics may be replaced; identity/currentness must reconnect explicitly. |
| Browser context | visible page and supplied context remain distinct | bounded candidate in PR #37 | Reconcile after canonical continuity is restored. |
| Presentation | lived nested browser + canonical Conversation threshold | generated P1 candidate | Generated surface remains a projection, not a new truth owner. |

## Product-specific runtime state

VexLife-Prod may own construction/lifetime records beneath source-owned namespaces:

~~~text
~/.vexlife/runtime/vexlife-prod/
~/.vexlife/runtime/product-service/
~~~

The stable predecessor continues to own:

~~~text
~/.vexlife/runtime/heretic/persistent-runtime-owner-receipt.json
~~~

New Beginnings may validate and borrow qualified runtime evidence from that stable receipt, but does
not overwrite it or manufacture product ownership of its process. A product-owned fallback runtime
uses its separate receipt beneath `runtime/vexlife-prod`.

Those product coordinates may store product-image, service-lifetime, implementation-binding, or
product-runtime evidence. They must not overwrite or redefine canonical `config/home.json`, the
current device/lineage records, canonical Conversation, Score, daily Memory, or their current heads.

## Current candidate disposition

PR #38 remains useful source, but the prior separate-Home premise is not accepted product meaning.
PR #37 remains useful browser/context convergence, but it must consume the corrected continuity body.

The continuity source body now includes canonical Home admission, Conversation v1/v2 persistence,
reference-only Semantic Relay validation, a read-only current Memory reader, Memory-aware prompt
receipts, and exact canonical writer-currentness/recovery primitives.

The compatibility hold earned retirement only after all three independent predicates agreed on the
same source lineage:

~~~text
A029 exact SourceOwner proof                    PASS
fresh semantic Assurance                       CLEAR
A030 read-only lived ~/.vexlife compatibility  PASS

A030 current Memory
  HELD / DAILY_MEMORY_SOURCE_FRONTIER_STALE
  provider Memory content = none

A030 continuity before/after
  byte-identical
~~~

This clears the temporary Conversation/Memory compatibility gate. It does **not** prove service
lifetime or the lived product:

~~~text
CONTINUITY COMPATIBILITY CLEARED
!= SUSTAINED SERVICE PROVEN
!= LIVED VEX TURN
!= GENERATED PRESENTATION COMPLETE
~~~

## Remaining acceptance order

1. Prove the exact gate-retirement + pre-runtime writer-convergence source.
2. Run fresh semantic Assurance on that exact head.
3. Prove stable Heretic predecessor borrowing (or product-owned fallback) through real setup.
4. Build/install the exact private image and prove sustained fixed-port service lifetime.
5. Prove one real canonical direct Vex turn through the production service.
6. Reconcile PR #37's bounded browser/context delta onto that cleared substrate.
7. Prove the lived browser-context-aware whole circuit and reload continuity.

## Lived completion gate

~~~text
open VexLife
-> canonical existing Home / lineage observed
-> generated VexLife presentation served
-> bounded public page rendered
-> current rendered-page context captured
-> Victor sends one natural question
-> exact prior Conversation + current Memory + browser context materialized
-> prompt receipt shows what Vex received
-> real Heretic inference occurs
-> response becomes canonical Conversation truth
-> surface renders the same response
-> reload preserves continuity
-> setup/bootstrap exit does not kill the current service
~~~

Only that circuit earns the first New Beginnings working baseline.

<!-- [VXG RealForever][VEXLIFE-PROD][CONTINUITY-REALIGNMENT] -->
