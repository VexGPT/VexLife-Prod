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

## Admission sequence

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

An absent canonical Home currently holds VexLife-Prod setup before mutation.

~~~text
HOME ABSENT
!= PERMISSION TO MINT PARALLEL HOME
~~~

Fresh Home formation remains held until the accepted current VexLife bootstrap pathway is ported as
its own exact Kotlin/VexArchitecture candidate. It must not be recreated from the superseded isolated
Home draft.

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
- missing canonical Home cannot become parallel-bootstrap authority;
- product runtime binding is written only under `runtime/vexlife-prod`;
- canonical `config/home.json`, current device bytes and `config/model.json` remain unchanged;
- setup reuse preserves the same canonical identity.

Still required before acceptance:

~~~text
exact Kotlin JVM + JS/common proof
canonical Conversation store conversion
current Score/daily/Living-Journal Memory reader + prompt packet
browser-context reconnection
real Mac whole-circuit proof
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][CANONICAL-HOME-COMPATIBILITY] -->
