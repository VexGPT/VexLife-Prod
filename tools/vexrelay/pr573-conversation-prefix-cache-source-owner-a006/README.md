# PR #573 Append-only Conversation prefix cache — SourceOwner A006

[VXG RealForever]

DO-NOT-MERGE proof attempt for Product PR #573.

## Exact Product candidate

```text
base   a23b6a949d27e4b77530273a390955f0d127ad1a
head   3a47bac8618c99214cfdb3d4396e626bc95b96ef
tree   733a514e1999ef5722e494160edc8894a8126458
branch VXG-112326-chatgpt-conversation-prefix-cache-a001
paths  10
```

Changed paths:

```text
docs/foundation/HERETIC-EXECUTION-TOPOLOGY.md
src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt
src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt
src/jvmMain/kotlin/vexlife/companion/DirectPromptContext.kt
src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntime.kt
src/jvmTest/kotlin/vexlife/companion/DirectCompanionConnectionTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectPromptContextTest.kt
src/jvmTest/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntimeTest.kt
src/jvmTest/kotlin/vexlife/model/heretic/runtime/MacHereticRuntimePlatformTest.kt
```

## Candidate relationship

The candidate projects the already-owned `APPEND_ONLY_HISTORY` Conversation boundary into provider
cache segmentation. A001 lived evidence proved the semantic boundary reached the provider but the old
partial-template token locator fell back under Qwen's context-sensitive assistant-history rendering.
A002 uses two context-preserving probes that both retain a later user and takes their common exact
prefix before activating custom segmentation. It changes no model-visible prompt bytes/order and does not create another
Conversation, Memory, prompt or cache semantic owner.

Provider optimization is fail-soft: unsupported/missing/token-prefix-mismatched boundaries retain
ordinary mlx-lm segmentation and the human Send remains independent of cache reuse.

The runtime launch contract also recognizes the exact #555 final-prompt-cache adapter as historical
retirement provenance so a currently running #555 candidate can converge into this successor without
becoming an unknown process.

## Canonical proof

```text
bash tools/release/prove-mac-candidate.sh 0.0.0
```

No Product launch, model/runtime, Conversation/Memory, approval, READY or merge effect is admitted.

On PASS: fresh Independent Assurance on the corrected token locator, then a new lived 4B cache=2
continuation proof. A001 lived is terminal negative evidence and must not be replayed.

<!-- [VXG RealForever][PR573][SOURCEOWNER-A006][DO-NOT-MERGE] -->

## A004 diagnostic distinction

A002 reached the exact corrected candidate, completed JVM tests and app/DMG formation, then stopped in the unchanged Mac packaging-verification tail after the embedded whisper help check. A004 makes no Product change. It runs the same repository-owned proof script under `bash -x` so a repeated stop identifies the exact assertion; a PASS demonstrates the A002 tail failure did not reproduce.

A002 remains terminal SAFE_FAILURE evidence and is not replayed.

## A004 proof-environment distinction

A003 traced a JVM-test failure at ContinuityPressureAdapterTest.kt:61. That assertion compares a
before/after set of every matching continuity-pressure scratch directory in the process-wide
`java.io.tmpdir`; the adapter separately deletes and verifies its own exact scratch root.

A004 makes no Product or test-source change. It gives this proof invocation a private JVM temp root
through `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=<isolated>` and `TMPDIR=<isolated>`, while retaining
`bash -x` on the same repository-owned proof script.

This removes unrelated host/process temp-namespace interference without weakening or skipping any
repository test.

## A005 candidate distinction

SourceOwner A004 passed for the prior bytes. The later isolated lived A003 RETURN proved the
lane-local runtime architecture itself preserved canonical Product state, then exposed one real
candidate defect before health:

```text
SyntaxError: unmatched '}'
```

The generated Qwen probe dictionaries encoded empty string values with nested double-quote escaping.
The corrected candidate uses Python single-quoted literals inside the generated `trep` source:

```text
{'role':'user','content':''}
{'role':'assistant','content':''}
```

No cache semantic relationship changed. The runtime test asserts the exact emitted safe forms and the
exact failed A003 program with only those two substitutions passes Python AST syntax parsing.

A005 proves these new exact bytes. A004 remains terminal PASS only for the prior head.

## A006 test-only distinction

A005 SourceOwner reached Kotlin compilation and stopped on the newly added regression guard itself:

```text
HereticPersistentRuntimeTest.kt:1093
Syntax error: Expecting ')'
```

The runtime source containing the Python-safe probe literals was not rejected.

The malformed negative escape-pattern assertion was removed. The two positive assertions for the exact
safe emitted probe forms remain:

```text
prefix_messages+[{'role':'user','content':''}]
prefix_messages+[{'role':'assistant','content':''},{'role':'user','content':''}]
```

A006 therefore changes only the test guard relative to A005. The runtime implementation bytes for the
probe syntax correction are unchanged.
