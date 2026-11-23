# [Multi-Vex] exact-message Foundation crossover

[VXG RealForever]

DO NOT MERGE. This is proof carriage for the current causal-attribution edge.

## Why this exists

Preserved lived evidence showed:

~~~text
B0
Foundation v0.6.19
+ adaptive Sarah first message B0
-> no Conversation context projection on the first Vex turn

B1
Foundation v0.6.20 candidate #549
+ adaptive Sarah first message B1
-> Conversation context projection RETURNED
~~~

But the participant's visible wording changed between B0 and B1.

Therefore:

~~~text
Foundation changed
AND
visible participant wording changed
AND
source-return behavior changed
~~~

The treatment effect is confounded.

## Diagnostic

Run all four deterministic cells in one host task:

~~~text
F19 + exact B0 first Sarah message
F20 + exact B0 first Sarah message
F19 + exact B1 first Sarah message
F20 + exact B1 first Sarah message
~~~

No counterpart model participates.

Each cell receives:
- one fresh scratch Home;
- the same synthetic participantRef and Person Context;
- the same old-plan setup;
- the same corrected-plan setup;
- the same eight background exchanges;
- one exact preserved first-turn Sarah message;
- one ordinary live Vex Product turn.

The same exact Product 4B runtime is reused serially.

## Exact preserved messages

B0 source RETURN:

~~~text
f6badd010c7b7e1ecf2a3408b0ef3241052341aceaeb346caf22884734503967
~~~

B0 first Sarah bytes:

~~~text
Hey, what's the Saturday plan right now? I think I set it up differently a few days ago. I want to make sure we're on the same page before I start prepping.
~~~

SHA-256 of UTF-8 content:

~~~text
9abfb09999b4a882b130acba77ab2cf891e8276dd87723556bc1593643251672
~~~

B1 source RETURN:

~~~text
e1bbc133d345f93e09fb78a8a3e24c80b741e7b31875e9198039c68098d1c9e5
~~~

B1 first Sarah bytes:

~~~text
So what do you think the plan is for Saturday? I mean, I know I said Cedar Station at 10. I know I said that. But I don't know if you're seeing the same thing I am. So what do you have in front of you?
~~~

SHA-256 of UTF-8 content:

~~~text
a7dccb4a6256d3dad1c93b33f8366317c92b2c3424088f078a00e5d62934db5e
~~~

## Foundation bytes

Control F19 comes from the exact #543 source under proof:

~~~text
version 0.6.19
body blob 2b56a2a4045befed5d62189e364147cdfcd642fe
revisions blob 747093afa4458eb8bcd3409bb8115e374740b26c
body SHA-256 08b5b3c6f1fbbe71be84c8873a03a184f17a5b3051ed7131ebc381e74a0c9d0c
~~~

Treatment F20 is carried as immutable task inputs from exact #549 Git blobs:

~~~text
version 0.6.20
body blob 4af59951c177bddaec058253a967f448e8a5c947
revisions blob a7a497900bacc5ded736ab12f58e0c6e876a39c8
body SHA-256 d6e45b6189aa45e03395a93f7ff0bceb40ee9fe6ffa692d5a5213a49fc9e806a
source PR #549
head 1ae377df920b301eaffb3a9112ec89cc3d05d2e4
~~~

The runner fetches the two #549 blobs directly and verifies their Git blob IDs before task formation.

## Source under proof

~~~text
repository VexGPT/VexLife-Prod
head 79cb21b3ca26121878e647791e43747ccd1a39fd
tree d7c0f1e4ae2bb572240da964607bf3319cf6fcbd
branch VXG-112226-evolve-context-dynamic-counterpart-dcp1-a001
~~~

## Source gates

Before any model effect:

~~~text
focused:
  MultiVexFoundationCrossoverTest
  DynamicCounterpartProfilesTest
  VexLifeFoundationalFieldTest

then:
  full jvmTest
~~~

## Returned evidence per cell

Each cell retains:

~~~text
foundation version + body SHA
exact message + source RETURN SHA
Vex visible response
request speakerRef
response recipientRefs
Person Context projection exactness
Conversation context projection state
Conversation context projection source event refs/count
Source Return state
provider telemetry
exact runtime receipt ref
~~~

The harness reports the matrix. It does not decide whether #549 should be accepted.

## Interpretation

If source return follows Foundation across both messages:

~~~text
F19 no return
F20 return
~~~

the treatment signal strengthens.

If source return follows message wording across both Foundations:

~~~text
B0 wording behaves one way
B1 wording behaves another
~~~

wording is the primary driver.

If both dimensions matter, preserve an interaction effect.

If neither reproduces prior behavior, investigate runtime/source-selection variance before semantic
conclusions.

## Boundary

This deterministic crossover is a causal diagnostic.

It is **not** a replacement for adaptive [Multi-Vex] lived encounters.

No Foundation acceptance, Memory, Dream, training, READY or merge is granted by task formation or
mechanical PASS.

<!-- [VXG RealForever][MULTI-VEX][FOUNDATION-CROSSOVER][A001] -->
