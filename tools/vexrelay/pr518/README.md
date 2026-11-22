# PR #518 Warm Boot Stage B — SourceOwner companion

[VXG RealForever]

This directory preserves the current SourceOwner carriage for VexLife-Prod PR #518.
It is tooling/evidence continuity only. Product/source authority remains PR #518.

## Human contract

Victor's host-side responsibility stays:

```text
one exact Mac command
-> one canonical RETURN ZIP in ~/Downloads
-> return that ZIP to the active instance
```

The normal route does not require a ChatGPT sandbox ZIP.

## Trajectory inherited

```text
#499
  Foundation-only prewarm executed but real first turn reused 0 tokens.
  Closed without merge. Provider stream mechanic is historical precedent only.

#504
  EvolveContext Stage A merged. Semantic/context ownership stays independent.

#510 A005
  exact MLX runtime survived Update & Restart = PASS
  prompt-visible Conversation/Memory preserved = PASS
  comparison turn completed = PASS
  prompt=48111 cached=0 prefill=48111

#516
  Warm Boot Stage A exact-prefix primitive SourceOwner PASS
  Independent Assurance CLEAR_ON_EXACT_HEAD
  merged main=0566b1eed876a28c2fcc6586743ae1f4af7eaecd
  exact merged/proved tree=101e6bcc70e3b654ed7a95960332711bb6700a71
```

Stage B now forms and validates a real provider-preparation candidate. No UI/readiness claim is
allowed until lived provider evidence records actual KV reuse.

## Exact PR #518 candidate

```text
base   0566b1eed876a28c2fcc6586743ae1f4af7eaecd
head   3f85f2ecdea8824ba45c2d7240c96ec03c56ae0d
tree   0ea9001c3d3a0ef0a87ff755a57be33c91ffdc1b
branch VXG-112226-chatgpt-initial-prompt-preparation-b001
paths  9
```

Changed paths:

```text
src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt
src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt
src/jvmMain/kotlin/vexlife/companion/VexLifeInitialPromptPreparation.kt
src/jvmMain/kotlin/vexlife/logging/VexLogger.kt
src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt
src/jvmTest/kotlin/vexlife/companion/DirectCompanionConnectionTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectCompanionTurnTest.kt
src/jvmTest/kotlin/vexlife/companion/DirectPromptContextTest.kt
src/jvmTest/kotlin/vexlife/companion/VexLifeInitialPromptPreparationTest.kt
```

There is no path overlap with the currently open EvolveContext #512 or localization #513 lanes.

## SourceOwner A001 claim

Stage B source must prove:

1. the historical cache-preserving mlx-lm stream mechanic still behaves exactly:
   - stops after first nonterminal provider event;
   - terminal assistant-cache replacement never claims success;
   - cancellation drains;
   - non-MLX and provider-native-tools representations HOLD before provider effect;

2. the actual lived Qwen4B text-control shape satisfies:
   ```text
   boot.messages.single()
   ==
   realGenesis.messages.first()
   ```

3. the Prompt Preparation owner:
   - consumes exact readiness once;
   - invalidates on runtime/provider-prefix/Memory drift;
   - never converts provider HOLD to READY;
   - cancels on human wait timeout;
   - drains an admitted worker even when watchdog submission fails;

4. the real turn seam:
   - checks preparation only after the canonical REQUEST exists;
   - exact candidate may be consumed;
   - stale candidate falls back cold;
   - no second REQUEST/RESPONSE truth store is created;

5. full `jvmTest` remains green.

## Product-owned build-memory contract

Every Gradle proof uses the repository-owned envelope already established by #490 and reused
successfully by #516 A004:

```text
--no-daemon
-Dorg.gradle.jvmargs=-Xmx4g
-Dkotlin.daemon.jvm.options=-Xmx4g
```

This is build/test JVM configuration, not VexLife production-runtime memory policy.

## Proof order / timing

```text
focused Stage-B matrix <= 600,000 ms
full jvmTest           <= 900,000 ms
combined command cap   <= 1,500,000 ms
generic carrier payload = 1,800,000 ms
```

The remaining carrier time is for exact clone/currentness/review/RETURN formation.

## Carrier

Fresh formation is bound to current accepted SDK/Relay source:

```text
SDK head 6f8f6f50467f5df5a542713f7dc25b82db4ac8fa
SDK tree 96795640b01f1969da980f52d5a8ec136a8c44aa

contractRef source.vexrelay.current-contract/v1
runtime v0.2.0-RC1
recipe ExactHeadSourceOwnerProof

outer profile profile.vexlocalbridge.macos-victor-vextreme-sdk.v1
repositoryExecutionProfileRef profile.repository.vextreme-sdk.macos-v1
carrier adapter.vexlocalbridge.native-vexrelay-task-archive/v1
```

The outer carrier belongs to the SDK repository carrier. The inner `vex.relay.task/v2` owns the
exact VexLife-Prod #518 source binding.

## Held

SourceOwner A001 does NOT:
- launch VexLife Product;
- perform real model warmup or lived inference;
- change prompt-cache size;
- add presentation/UI;
- add ACTIVE/STAGING continuity epoch rotation;
- modify EvolveContext;
- approve/READY/merge;
- mutate Home, Conversation, Memory or model runtime.

A SourceOwner PASS advances to fresh Independent Assurance on the exact head. Lived provider proof
comes only after source/assurance acceptance.

<!-- [VXG RealForever][PR518][SOURCEOWNER-A001][RELAY-COMPANION][DO-NOT-MERGE] -->
