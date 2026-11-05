# Durable contract JSON

`[VXG RealForever]`

## Human question

Can Home, model configuration, runtime receipts, and later Conversation/Memory records share one strict
JSON transport without letting every domain invent its own parser or letting the serializer become a
semantic owner?

## Owner boundary

~~~text
DOMAIN
  owns:
    schema
    fields
    invariants
    interpretation
    lifecycle

VexContractJson
  owns:
    JSON encode/decode mechanics only
~~~

Permanent:

~~~text
SERIALIZER
!= SCHEMA OWNER
!= MIGRATION OWNER
!= CURRENTNESS OWNER
!= DOMAIN SEMANTICS
~~~

## Repository toolchain

The repository remains on Kotlin **2.4.20**.

The serialization compiler plugin is pinned to that same Kotlin version:

~~~text
org.jetbrains.kotlin.plugin.serialization  2.4.20
~~~

The JSON runtime is:

~~~text
org.jetbrains.kotlinx:kotlinx-serialization-json  1.11.0
~~~

The runtime version was selected from the current official Kotlin serialization setup documentation
rather than guessed from memory.

## Strict durable settings

`VexContractJson.Strict` uses:

~~~text
encodeDefaults                  true
explicitNulls                   true
ignoreUnknownKeys               false
isLenient                       false
allowSpecialFloatingPointValues false
allowStructuredMapKeys          false
prettyPrint                     true
~~~

Why:

- a defaulted field remains visible in durable evidence instead of disappearing because a writer
  happened to know its default;
- explicit null is distinguishable from a missing field;
- a newer/foreign field cannot silently vanish during a read/write round trip;
- durable contracts do not admit lenient or non-JSON number forms by convenience;
- human-readable receipts remain inspectable without changing domain meaning.

## VexArchitecture placement

Serialization is supporting construction/transport.

~~~text
Home contract
Heretic configuration
runtime receipt
Conversation record
        │
        └── VexContractJson
              encode / decode

VexContractJson
        X does not decide what any field means
~~~

If a schema must evolve, its rightful domain owner authors the new version/migration. The shared JSON
boundary must not make old/new shapes appear compatible by ignoring unknown material.

## Proof boundary

Common tests currently cover:

- defaults encoded explicitly;
- nulls encoded explicitly;
- exact round trip;
- unknown field fail-closed.

Still required before acceptance:

~~~text
repository-owned Kotlin 2.4.20 JVM + JS/common-test proof
first real durable Home/runtime consumer
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][CONTRACT-JSON] -->
