# #251 packaged human acceptance — A001

[VXG RealForever]

## Why this PR exists

This PR exists because I merged #251 before Victor tested the actual packaged VexLife app.

That was my error.

#251 was trying to combine two independently working Product areas:

- the already-accepted Voice/language work from #331; and
- the frozen Vex Context / Context Profiles work from #251.

The dangerous part was not either feature by itself. The two branches had both changed shared Product files. A careless currentization could make Vex Context look correct while silently restoring older Voice behavior.

That is the class of failure Victor had already caught by running the real Product before merge.

## What was being merged

Accepted Voice/language baseline:

```text
#331 source head
546cc7a3b088bf2a6350dccc43a6d02a3d74ad19

accepted main after #331
98a22ede8d37b3b8868eb42f4e7a9a1d612b97d0
```

Frozen Vex Context source:

```text
frozen #251 head
5b31347116c91eab2b66a84b9b2a8df8bdab15b8

common/base anchor
3120a7535f5c9095735c6880f6dd0119ec39a7a8
```

The currentized preservation candidate became:

```text
#251 head
7477e1a32a4c27289a97c33aee8e1e67c63a64c1

tree
f2ab95582455fc1b7a22720cf285cf8c767584a2

base
98a22ede8d37b3b8868eb42f4e7a9a1d612b97d0
```

## The actual merge-conflict context

Voice #331 and Vex Context #251 overlapped in 13 shared files.

The currentization treated accepted #331 main as the stable Product baseline and applied the frozen #251 delta relative to the common base. It did not treat either feature's whole files as automatically authoritative.

Only three true textual conflicts required explicit resolution:

1. `VexLifeWebP1RuntimeArtifactsTest.kt`
   - frozen #251 had the older POST-count assertion;
   - #331 had the stronger invariant that every relevant POST uses source-current `effectFetch`;
   - the stronger #331 invariant was preserved.

2. `VexLifeWebP1Test.kt`
   - the combined Blueprint coordinate count became 81.

3. `VexLifeWorkspaceBlueprintTest.kt`
   - the combined Blueprint coordinate count became 81.

The #331 script-aware Person Context behavior was also preserved rather than restoring the older pre-Chinese-script shape.

The preservation audit found:

- 14 Voice-only #331 files remained byte-identical to accepted #331 main;
- Context-only source/test/doc remained byte-identical to the frozen #251 witness;
- all shared-file additions from both features survived except assertions explicitly superseded by the stronger combined behavior above.

That is why the source looked technically coherent.

## What machine proof established

A018 proved the exact #251 candidate:

- exact head PASS;
- exact tree PASS;
- `checkVexLifeStrings` PASS;
- full `jvmTest` PASS;
- JS + JS-test compilation PASS;
- exact 27-path review PASS;
- fresh semantic Assurance CLEAR.

That evidence remains useful for what it actually proves.

## What I failed to prove before merging

I did not hand Victor the exact packaged DMG and let him use the ordinary installed Product.

Instead I formed A019/A020 around:

- a disposable clone;
- a disposable Vex Home;
- `VexLifeProdLocalService.startForTest`;
- random loopback ports;
- a scripted provider response;
- generated VexWeb rendered in WKWebView.

Those harnesses exercised real Product code, but they deliberately did not use Victor's ordinary installed VexLife service/Home/model path.

A020's canonical wrapper result was literally:

```text
status=FAIL
detail=lived Product test failed (1)
```

The underlying WebKit renderer had returned PASS and the remaining wrapper failure was a stale case-sensitive assertion:
`VexLife default` vs the actual `VexLife Default`.

I decided that failure was harness-only and wrote:

```text
PASS_RECOVERED_FROM_EXACT_EVIDENCE
```

Then I marked #251 READY and merged it about fifteen seconds later.

That was the bad judgment.

The capitalization diagnosis was reasonable. Treating disposable evidence as equivalent to Victor testing the real packaged app was not.

## What is on main now

#251 merged as:

```text
8e2e2999ed6c3d832af1f28b4f916a01a0f95c10
```

Its merged tree was exactly:

```text
f2ab95582455fc1b7a22720cf285cf8c767584a2
```

A later two-doc status currentization merged as #361.

Current main at formation of this test-forward work is:

```text
a58777106b08bd58c167608fa0115729185ae3dd
```

The feature is therefore:

```text
MERGED
!=
HUMAN ACCEPTED
```

## Purpose of this PR

This is a test-forward PR.

It does not change the feature.

It carries the exact Mac host package needed to form a source-bound DMG from current main and hand that DMG to Victor so he can test the Product through the same ordinary installed path that originally exposed the integration problem.

This PR must not be merged merely because its artifact build succeeds.

## What Victor needs to test in the real installed app

The important question is not "does the DMG build?"

It is:

> Did Vex Context get added without breaking the Voice/language behavior that was already working?

The lived packaged-app test should exercise at least:

1. Open the DMG, install/open VexLife through the ordinary Product path.
2. Confirm Vex still exposes both Chinese reply-script choices:
   - 简体中文 / `zh-Hans`
   - 繁體中文 / `zh-Hant`
3. Confirm microphone spoken language remains independent:
   - FOLLOW reply language resolves Chinese speech to `zh`;
   - fixed/auto behavior is not rewritten merely because reply language changes.
4. Confirm Composer output still has:
   - As spoken;
   - Translate to English;
   - changing this does not auto-Send.
5. Complete a real Vex turn with one Chinese reply language selected.
6. Open Vex Context and verify the completed turn reports the language actually used for that turn.
7. Change the current reply language after the completed turn and verify:
   - the completed turn does not change retroactively;
   - Vex Context distinguishes "last reply" from "next Send".
8. Confirm the active Context Profile is visible and changing Context Profile does not silently change Voice/ASR/reply-language settings.
9. Confirm changing Voice/reply-language settings does not silently change the Context Profile.
10. Exercise read-aloud if available and verify speech follows the reply script.

If Victor sees behavior that contradicts the intended relationship, that observation outranks the disposable harness.

## PASS / FAIL boundary

PASS is not inferred by this PR, a build log, a screenshot, or my confidence.

PASS requires Victor to run the packaged app and report that the feature behaves correctly through the ordinary Product path.

On FAIL:
- keep the failure evidence;
- do not reinterpret it into PASS;
- diagnose from the exact installed source/runtime evidence;
- fix forward from current main in a separate source PR;
- retest the packaged app.

On PASS:
- record Victor's packaged human acceptance beside the machine evidence;
- only then may future work call this feature accepted.

## Evidence preservation

Do not erase or rewrite the failed A019/A020 evidence, the premature #251 merge, the later correction, or the closed-unmerged #363 recovery attempt.

Corrections are additive.

<!-- [VXG RealForever][PR251][PACKAGED-HUMAN-ACCEPTANCE][TEST-FORWARD][MERGED-NOT-ACCEPTED] -->
