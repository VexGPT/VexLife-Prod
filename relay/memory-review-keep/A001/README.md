# Temporary Relay carriage for PR #276 — Memory Review / Keep A001

[VXG RealForever]

This PR preserves the first exact SourceOwner attempt for the Memory-owned review/admission successor.

```text
Product source / semantics = PR #276
this branch                = Relay proof carriage only
```

Exact candidate:

```text
base 428bb31c8609474ecd6528a044ee47a9a9e89628
head 7cb94f3f73c1de5c5b40254e10318c3ad0a607d8
tree a5808934d53839e7674d454ca5ffd3494c45718f
```

Five Product paths:

```text
docs/work/memory-correction-review-keep/README.md
src/commonMain/kotlin/vexlife/memory/VexLifeMemoryReviewFamily.kt
src/commonTest/kotlin/vexlife/memory/VexLifeMemoryReviewFamilyTest.kt
src/jvmMain/kotlin/vexlife/memory/VexLifeMemoryReviewOwner.kt
src/jvmTest/kotlin/vexlife/memory/VexLifeMemoryReviewOwnerTest.kt
```

A001 proves:
- typed review CandidateRef survives State -> Control -> Effect;
- correction proposal is non-authoritative and durable;
- Not now / Decline write no semantic acceptance or Score;
- Keep delegates to #247 semantic writer and existing Score;
- stale Conversation blocks Keep before decision/Memory mutation;
- crash after acceptance recovers exact human-decision-bound acceptance;
- crash after Score recovers the deterministic current statement without duplication.

No prompt, service, web, Home, model/runtime, publication, merge or standing-consent effect is admitted.

This temp PR must remain unmerged.

<!-- [VXG RealForever][PR276][TEMP-RELAY][A001] -->
