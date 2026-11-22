# PR #533 First Local Start UI — SourceOwner companion

[VXG RealForever]

DO-NOT-MERGE proof carriage for Product PR #533.

## Why this candidate exists

C003 lived proof on #531 established real reusable-prefix behavior on Qwen4B:

```text
same MLX runtime receipt/PID across source rotation
READY_REUSABLE -> real human Send -> CONSUMED
same Send TURN_COMPLETED
cachedPromptTokens 2939 > 0
prefillTokens 45221 < promptTokens 48160
```

Victor's screenshot from that exact proof showed the remaining human-facing gap: ordinary
“Vex is working / thinking…” does not explain that the first local reply may still carry one-time
startup/prefill work.

PR #533 projects only existing Prompt Preparation truth into one temporary orange Composer card.

## Exact candidate

```text
base   5a26a310878b191f39de521a8b5ae01a2d10b224
head   3f124ed97ee74df0dffb49e5b368fbc78be1d57f
tree   67f842dcababff034eeb61c3d401cf45c1c4bd00
branch VXG-112226-chatgpt-first-local-start-ui-d001
paths  12
```

Changed paths:

```text
localization/strings/manifest.json
localization/strings/source/workspace.json
src/commonMain/kotlin/vexlife/service/VexLifeProdServiceContract.kt
src/commonMain/kotlin/vexlife/web/VexLifeWebP1.kt
src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceMarkup.kt
src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceStyle.kt
src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt
src/commonMain/kotlin/vexlife/workspace/VexLifeWorkspaceBlueprint.kt
src/commonTest/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifactsTest.kt
src/jvmMain/kotlin/vexlife/companion/VexLifeInitialPromptPreparation.kt
src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt
src/jvmTest/kotlin/vexlife/companion/VexLifeInitialPromptPreparationTest.kt
```

## SourceOwner claim

The Product-owned canonical Mac candidate proof must pass on these exact bytes:

```text
bash tools/release/prove-mac-candidate.sh 0.0.0
```

Expected relationships:
- no new preparation manager/store/timer/durable receipt;
- Prompt Preparation remains the owner;
- ConversationTurn projects only first-local-start booleans;
- the active turn remembers whether it began in that one initial window;
- VexWeb renders a caution/orange card beside the canonical Composer;
- the card remains through that first active turn and disappears afterward;
- browser source contains no READY_REUSABLE/CONSUMED provider-cache vocabulary;
- all 11 current screen languages remain complete;
- Send lawfulness, Conversation, Memory, runtime, and cache behavior are unchanged.

This proof owns source/build correctness only. It does not launch Product, perform model inference,
approve, READY, merge, or claim lived visual acceptance.

On PASS: fresh Independent Assurance, then one lived UI preview/proof on the exact head.

<!-- [VXG RealForever][PR533][SOURCEOWNER-A003][DO-NOT-MERGE] -->


## A001 consumed

A001 stopped before Relay task formation on the current SDK manifest:

```text
MANIFEST_ENTRY_CLASS:PRESENTATION_DELTA
```

No Product proof/runtime/Conversation/Memory effect occurred.

A003 corrects only Relay formation vocabulary and current protocol limits:

```text
entryClass SAME_LANE_SUCCESSOR
scopeMode STAGE_BOUNDED
role CODER
maxFiles 10
maxSelectedSourceEntries 10
maxSelectedSourceBytes 5,000,000
```

The exact Product candidate remains unchanged:

```text
head 3f124ed97ee74df0dffb49e5b368fbc78be1d57f
tree 67f842dcababff034eeb61c3d401cf45c1c4bd00
paths 12
```

The review snapshot still records all changed paths and the full candidate diff. Current Relay limits
only the inline selected source bodies to ten files.

<!-- [VXG RealForever][PR533][SOURCEOWNER-A001-CONSUMED][SOURCEOWNER-A003-CURRENT] -->


## A002 consumed / architecture-subtracted A003 current

A002 canonical outer RETURN:
```text
sha256 31818d53d2f770a8dcf91f63d63c76e8d4d65f9c3e0c090be0663ae41ce3d431
```

Retained inner VexRelay RETURN:
```text
sha256 e9fb7caf6ecab3bf4e0fd222c26db48d82d7883623e941ff7050de5db3bc3e35
SAFE_FAILURE
```

A002 reached Kotlin compile and 1020 JVM tests; exactly three failed because the first source shape added
a new Blueprint/VexWeb coordinate and did not refresh the source-derived Screen Lens.

Architecture re-grounding subtracted that coordinate instead of blessing it through snapshot updates.

A003 exact source:
```text
head 3f124ed97ee74df0dffb49e5b368fbc78be1d57f
tree 67f842dcababff034eeb61c3d401cf45c1c4bd00
paths 11
```

The first-start human meaning now manifests through the existing ResponseProgress coordinate. Screen
Lens semantic structure is unchanged; only its static HTML/CSS view is refreshed.

<!-- [VXG RealForever][PR533][SOURCEOWNER-A002-CONSUMED][A003-ARCHITECTURE-SUBTRACTED-CURRENT] -->
