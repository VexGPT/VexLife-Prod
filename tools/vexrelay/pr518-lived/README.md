# PR #518 Warm Boot — lived cache proof companion

[VXG RealForever]

This companion owns only the human-host lived proof carriage for Product PR #518. It must not be
merged into Product source.

## Current accepted source boundary

```text
#516 Stage A
  merged main 0566b1eed876a28c2fcc6586743ae1f4af7eaecd
  exact merged/proved tree 101e6bcc70e3b654ed7a95960332711bb6700a71

#518 Stage B
  head 3f85f2ecdea8824ba45c2d7240c96ec03c56ae0d
  tree 0ea9001c3d3a0ef0a87ff755a57be33c91ffdc1b
  SourceOwner A001 PASS
  fresh Independent Assurance CLEAR_ON_EXACT_HEAD
```

#518 is not READY and must not merge until lived provider cache reuse is observed.

## Lived question

Does the exact Stage-B candidate produce real reuse on the same live Qwen4B MLX process?

Acceptance:

```text
PromptPreparationObserved = READY_REUSABLE
same runtime receipt + same runtime PID
one ordinary human Send
PromptPreparationObserved = CONSUMED
first ProviderStageObserved:
  cachedPromptTokens > 0
  prefillTokens < promptTokens
TURN_COMPLETED for that same Send
```

The harness never writes Conversation or Memory and never synthesizes the Send.

## Human boundary

Victor performs one terminal paste. During the run:

```text
do not Send yet
-> candidate service starts
-> exact preparation reaches READY_REUSABLE
-> harness opens candidate VexLife browser
-> that browser opening is the NOW SEND gate
-> Victor sends one ordinary message in VexLife
-> harness observes the result
-> one canonical RETURN ZIP
```

No terminal text is relied on as the live gate because VexLocalBridge buffers payload output.

## Product host membrane

Exact SDK carrier source:

```text
Vextreme-SDK PR #1956
head 149455eeec4c7b7ca00e91022154a85faa3d608c
tree 53ea701f9bbf24a5cf37d43e4dbe3836187f4011
profile profile.vexlocalbridge.macos-victor-vexlife-prod.v1
repository profile.repository.vexlife-prod.macos-v1
```

Allowed effects:

```text
effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1
effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1
```

Held by the current Product host policy:
- host installation;
- Home recreate/delete;
- harness Conversation mutation;
- harness Memory mutation;
- model acquisition;
- model switch-for-proof.

The lived payload uses `--reuse-existing`: the existing selected 4B runtime must already be exact
and current. If not, the proof stops rather than changing models.

## Content boundary

RETURN evidence contains only:
- exact source/tree coordinates;
- service/runtime refs and PIDs;
- aggregate SHA-256 of the protected `conversations/` and `memory/` trees before preparation and at READY;
- preparation state/reason;
- turnRef;
- provider token/timing counters;
- lifecycle/cache acceptance findings.

It exports no human message, model answer, Memory text or reasoning.

## Candidate startup

The payload:
1. aggregate-hashes protected Conversation/Memory trees;
2. clones exact #518 head/tree into task work;
3. builds source-bound `VexLife.app` with Product-owned 4 GB Gradle/Kotlin heap;
4. launches its embedded setup with
   `--home ~/.vexlife --expected-source-sha <HEAD> --reuse-existing`;
5. requires source-current candidate service + selected/serving `heretic-4b`;
6. requires exact live runtime receipt/PID;
7. waits preparation terminal;
8. opens browser only on READY_REUSABLE;
9. observes one real Send to terminality.

## Held after this proof

Even a lived PASS does not itself add UI, continuity-epoch A/B rotation, EvolveContext changes,
approval, READY or merge. It is the evidence required to decide the next lifecycle edge.

<!-- [VXG RealForever][PR518][WARM-BOOT][LIVED-A001][DO-NOT-MERGE] -->
