# Canonical VexLife Home compatibility

`[VXG RealForever]`

## Human question

How can VexLife-Prod replace implementation machinery without silently replacing Victor's existing
Vex Home, device/Companion lineage, Conversation, Memory or model-configuration continuity?

## Current answer

New Beginnings binds the already-established canonical VexLife Home:

~~~text
~/.vexlife
  config/home.json
  devices/<currentDeviceRef>.json
  config/model.json
  conversations/<currentCompanionLineageRef>/<threadRef>/
  context/<currentCompanionLineageRef>/<threadRef>/
  score/<currentCompanionLineageRef>/<threadRef>/
  daily-memory-dream/<currentCompanionLineageRef>/<threadRef>/
  runtime/
~~~

The repository/product name does not mint another human identity:

~~~text
FRESH REPOSITORY
!= FRESH HOME

VEXLIFE-PROD IMPLEMENTATION
!= NEW VEX / NEW COMPANION LINEAGE

PHYSICAL RUNTIME NAMESPACE
!= SEMANTIC HOME OWNER
~~~

Current canonical namespaces remain:

~~~text
vex-home.<device.vexlife.*>
device.vexlife.*
companion-lineage.vexlife.*
~~~

## Product-runtime namespace

VexLife-Prod may own source-bound implementation/lifetime records beneath the admitted Home:

~~~text
~/.vexlife/runtime/vexlife-prod/
  model-binding.json

~/.vexlife/runtime/product-service/
  images/
  launchd/
  logs/
  service-lifetime.json
~~~

Those records may describe the new runtime implementation. They may not overwrite or reinterpret:

~~~text
config/home.json
current device installation
current Companion lineage
config/model.json
canonical Conversation
Score / daily Memory
current heads / source descent
~~~

## Admission and fresh-formation sequence

Existing Home admission remains unchanged:

~~~text
canonical ~/.vexlife exists
        ↓
config/home.json exact current schema
        ↓
currentDeviceRef
        ↓
devices/<currentDeviceRef>.json exact agreement
        ↓
currentCompanionLineageRef exact agreement
        ↓
VexLife-Prod runtime setup may continue
~~~

The current source candidate also ports the missing fresh-user edge from the accepted VexLife
bootstrap relationship without resurrecting the superseded isolated Prod Home:

~~~text
canonical ~/.vexlife absent
        ↓
read-only current-state projection
  ABSENT_CANONICAL_VEXLIFE_HOME
        ↓
Product-entry SETUP_REQUIRED
        ↓
human selects Set Up VexLife
        ↓
CONVERGE_PRODUCT(allowAcquisition=true)
        ↓
per-Home setup lease
        ↓
host eligibility
        ↓
canonical bootstrap plan
        ↓
private sibling staging root
        ↓
exact canonical identity nucleus
        ↓
re-observe staged Home
        ↓
atomic publish to ~/.vexlife
        ↓
existing LocalSetup model/runtime path continues
~~~

Fresh formation writes only the minimum identity/runtime nucleus currently consumed by New
Beginnings:

~~~text
config/home.json
config/model.json
devices/<currentDeviceRef>.json
runtime/vexlife-prod/model-binding.json
recovery/bootstrap-receipt.json
~~~

The canonical model configuration begins UNCONFIGURED. The Product runtime binding separately begins
UNCONFIGURED. No model artifact is downloaded by Home formation itself. Conversation, Memory, Score,
Dream, culture projection and other domain state are not fabricated merely because a fresh Home is
formed; their rightful owners form their own state when needed.

The candidate preserves these boundaries:

~~~text
HOME ABSENT
+ EXPLICIT FIRST-SETUP AUTHORITY
= MAY FORM CANONICAL vex-home.<device.vexlife.*>

HOME ABSENT
+ UPDATE / REPAIR / REUSE-ONLY AUTHORITY
= HOLD

HOME ABSENT
!= PERMISSION TO MINT vex-home.prod.*

FRESH CANONICAL HOME FORMATION
!= MODEL/RUNTIME ACQUISITION
!= PRODUCT SERVICE START
!= CONVERSATION OR MEMORY FORMATION
~~~

The fresh Home is assembled under a private sibling staging root and is published only through an
atomic move after the staged Home/device/model relationships re-observe successfully. If formation
stops before publication, the staging root is removed and the canonical Home coordinate remains
absent.

This is source-candidate behavior until exact Kotlin proof and a bounded lived fresh-Home exercise
clear it.

## Canonical configuration preservation

Current VexLife owns `config/model.json`. VexLife-Prod's implementation binding therefore lives at:

~~~text
runtime/vexlife-prod/model-binding.json
~~~

This permits the new Kotlin runtime to bind exact Heretic implementation evidence without turning its
product-specific schema into the canonical VexLife model owner.

## Portable source

~~~text
src/commonMain/kotlin/vexlife/continuity/VexLifeContinuity.kt
src/commonMain/kotlin/vexlife/home/VexLifeProdHome.kt
src/jvmMain/kotlin/vexlife/home/VexLifeProdHomeFiles.kt
src/jvmMain/kotlin/vexlife/model/heretic/runtime/FileHereticModelConfigurationStore.kt
~~~

The portable source makes these relationships compiler-visible:

- canonical Home root;
- Home/device/Companion lineage identity law;
- canonical Conversation/context/Score/daily-Memory coordinates;
- product-specific runtime coordinates;
- exact reference repository/revision for the port.

## Provenance

Behavioral/contract reference:

~~~text
repository  vgong24/VexLife
revision    7d59753687ff12726b0d309ee2dde028b5f89e8a

src/core/boot.mjs
src/core/device-family.mjs
src/core/lived-companion.mjs
src/core/score-context-continuity.mjs
src/core/daily-memory-dream.mjs
src/core/living-journal-memory-projection.mjs
src/core/browser-prompt-context-runtime.mjs
~~~

Historical JavaScript is provenance for earned relationships, not a required runtime dependency of
the Kotlin product.

## Current proof boundary

Source tests now target:

- default product Home resolves to `~/.vexlife`;
- current Home/device/lineage bytes are observed without mutation;
- `.prod` identity namespaces are rejected;
- absent Home current-state awareness remains read-only and carries no invented identity;
- absent Home reaches Product-entry `SETUP_REQUIRED` without descending into service-lifetime
  observation;
- explicit first setup may form one canonical Home/device/Companion lineage;
- reuse/update/repair authority cannot form a missing Home;
- fresh formation writes canonical `config/model.json` plus the separate Product runtime binding;
- staging is re-observed before atomic publication;
- rerun preserves the same canonical identity;
- product runtime binding remains under `runtime/vexlife-prod`;
- existing canonical `config/home.json`, current device bytes and `config/model.json` remain unchanged
  on ordinary reuse.

Still required before this candidate can be accepted:

~~~text
exact Kotlin JVM + JS/common proof
exact candidate path review
fresh semantic Assurance
bounded lived absent-Home proof on a disposable Home coordinate
integration/lifecycle currentness
~~~

A lived disposable-Home proof must not touch Victor's current canonical `~/.vexlife`. Full public
onboarding, first Product Release and public-page deployment remain later edges.

<!-- [VXG RealForever][VEXLIFE-PROD][CANONICAL-HOME-COMPATIBILITY] -->
