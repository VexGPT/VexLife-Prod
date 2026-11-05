# VexLife-Prod Home foundation

`[VXG RealForever]`

## Human question

Can a fresh VexLife-Prod installation have one isolated, preservation-safe local Home identity without
copying the entire historical VexLife filesystem shape or silently depending on `~/.vexlife`?

This foundation ports the earned **Home relationship** from current VexLife while giving the fresh
product its own identity namespace and storage root.

## Isolation

~~~text
legacy/current VexLife Home
  default root: ~/.vexlife
  refs:
    vex-home.*
    device.vexlife.*
    companion-lineage.vexlife.*

VexLife-Prod Home
  default macOS root:
    ~/Library/Application Support/VexLife-Prod

  refs:
    vex-home.prod.*
    device.vexlife-prod.*
    companion-lineage.vexlife-prod.*
~~~

The fresh Home does not automatically import, mutate, replace, or alias the legacy Home.

~~~text
VEXLIFE HOME
!= VEXLIFE-PROD HOME

ISOLATED HOME
!= AMNESIA ABOUT EARNED RELATIONSHIPS
~~~

A later migration/import owner may intentionally consume legacy VexLife evidence. Bootstrap itself
does not gain that authority.

## Faithfully ported laws

Current VexLife bootstrap already earned these important defaults:

~~~text
existing Home
  -> preserve / classify
  -> do not overwrite

model
  -> UNCONFIGURED

automatic fallback
  -> false

automatic download
  -> false

automatic activation
  -> false

personal Memory import
  -> false

model artifact download during Home bootstrap
  -> false

existing data deletion
  -> false

Companion lineage
  -> explicit and distinct per device installation
~~~

Those meanings are retained.

## Refined fresh-body shape

Current VexLife bootstrap pre-creates many directories for capabilities accumulated across the mature
repository.

The fresh Home creates only coordinates that the Home foundation actually owns:

~~~text
config/
runtime/
recovery/
~~~

and plans only:

~~~text
config/home.json
config/model.json
recovery/bootstrap-receipt.json
~~~

It intentionally does **not** pre-author:

~~~text
models/
conversations/
Memory
Score
Dream
Training
Projects
Rhythm
Sync
~~~

Those paths appear only when their rightful owners are ported and can state their own invariants.

This is subtraction, not capability rejection.

## Portable source

~~~text
src/commonMain/kotlin/vexlife/home/VexLifeProdHome.kt
~~~

The portable contract owns:

- fresh installation IDs;
- Home manifest meaning;
- initial no-effect model configuration;
- bootstrap receipt meaning;
- minimum base directories;
- preserve-existing policy;
- the borrowed `homeRef + companionLineageRef` coordinate other packages may consume.

It does **not** perform filesystem writes yet.

## Why persistence is held

The repository is beginning to accumulate multiple durable contracts: Home, model configuration,
runtime receipts and later Conversation/Memory records.

Rather than let each package invent its own JSON writer/parser, persistence/serialization will be
formed once as a shared source-bound owner and then consumed here.

~~~text
HOME CONTRACT
!= FILESYSTEM EFFECT

FILESYSTEM EFFECT
!= EACH DOMAIN INVENTS JSON AGAIN
~~~

## Provenance

The port is grounded in current VexLife:

~~~text
src/core/boot.mjs
src/core/device-family.mjs
scripts/bootstrap.mjs
~~~

The old implementation is provenance for the earned relationships, not the runtime dependency of the
fresh Home.

## Proof boundary

Current common tests prove:

- fresh Home/device/Companion-lineage namespaces are distinct from legacy VexLife;
- only Home-owned base directories/writes are planned;
- initial model selection grants no automatic effects;
- bootstrap imports/deletes nothing;
- Home + Companion lineage can be consumed as one borrowed binding scope.

Still held before acceptance:

~~~text
repository-owned Kotlin 2.4.20 JVM + JS/common-test proof
shared durable contract serialization
filesystem bootstrap realization
fresh macOS Home proof
legacy VexLife import/migration (separate owner, if admitted)
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][HOME-FOUNDATION] -->
