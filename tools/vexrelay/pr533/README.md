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
head   6ceff940bc2e24e15fb251b0135126af8c97e381
tree   aff3ec05f7016071cdd9eeeb95b8fe5f8ced0f48
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

<!-- [VXG RealForever][PR533][SOURCEOWNER-A001][DO-NOT-MERGE] -->
