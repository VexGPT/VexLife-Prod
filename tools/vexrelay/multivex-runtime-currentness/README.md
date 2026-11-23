# [Multi-Vex] Product runtime-currentness diagnostic

[VXG RealForever]

DO NOT MERGE. This is a read-only recovery diagnostic for the neutral-lived trajectory.

## Why this exists

The simplified neutral-lived A001 proof reached:

~~~text
exact source PASS
focused neutral source gate PASS
full JVM source gate PASS
~~~

and then stopped before Sarah or Vex inference at:

~~~text
failureStage = PRODUCT_RUNTIME_SETUP

existing Heretic runtime receipt does not match the current binding
~~~

Canonical neutral-lived RETURN:

~~~text
e31d898ad9c39e671de6b1e719f043b4ba341610f8717ae74a94d5cf42307b18
~~~

The Product model selection itself remained unchanged:

~~~text
model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20
~~~

Canonical Product Conversation and Memory were unchanged by the failed attempt.

## Why this is not a Multi-Vex source regression

The runtime-owner source blobs are byte-identical between:
- steering A003, which successfully established the Product runtime relationship; and
- neutral-lived A001, which stopped at runtime currentness.

Identical blobs include:

~~~text
VexLifeProdLocalSetup.kt
HereticPersistentRuntime.kt
HereticRuntimeConfiguration.kt
FileHereticRuntimeReceiptStore.kt
FileHereticRuntimeConfigurationStore.kt
~~~

The last successful steering proof observed:

~~~text
Product runtime receipt
receipt.vexlife-prod.heretic-persistent-runtime.6bb92df935fd3446f35a60a3

pid
31743

prompt-cache-size
2
~~~

Host state changed before neutral-lived A001. In particular the canonical Conversation tree observed
by the proof carrier was no longer the same tree as after steering A003. That does not by itself
identify the runtime mismatch; it establishes only that the host did not remain frozen between the
two runs.

## What this diagnostic reads

Read-only Product-owned coordinates beneath the selected Home:

~~~text
runtime/vexlife-prod/model-binding.json
runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json
runtime/vexlife-prod/heretic-runtime-configuration.json
runtime/heretic/persistent-runtime-owner-receipt.json
~~~

It records:
- file SHA-256;
- current Product runtime receipt ref / formedAt / model identity;
- implementation identity recorded by the receipt;
- runtime pid / process group / exact-owned flag;
- exact runtime argv;
- observed prompt-cache-size;
- process liveness and ps view;
- loopback /health response;
- runtime configuration bytes;
- Product model-binding bytes;
- stable predecessor receipt, when present;
- whether receiptRef / pid / cache changed from the last successful steering proof.

It re-reads the Product-owned coordinates afterward and requires their file identities to remain
unchanged during observation.

## What this diagnostic does not do

~~~text
NO source mutation
NO Home mutation
NO runtime repair
NO runtime restart
NO model acquisition
NO model switch
NO Conversation mutation
NO Memory mutation
NO Foundation mutation
~~~

Loopback /health is recorded honestly as a network observation.

## Source coordinate being resumed

~~~text
repository VexGPT/VexLife-Prod
head f9cc4a05ab2be6a45127ad4badaab8c9110afbb4
tree 220e7ddccca1b45ae56e576041ce2bc860dbf229
branch VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
~~~

The neutral-lived encounter itself remains preserved and must not be replayed until this runtime owner
question is resolved.

<!-- [VXG RealForever][MULTI-VEX][RUNTIME-CURRENTNESS][A001] -->
