# Current-main VoiceInput A003 — source-current lived Chinese proof

[VXG RealForever]

A003 continues PR #310 after consuming A002.

## A002 result

```text
RETURN sha256  363d63248fb631094815798cd45565961d5976e38da9750efb21447eb3ac5a92
source         4d509b1f72ddc6cfeef38ce94192b62b4b61a3e4
source tree    382dfcc3fcf7e5b9dd47e90900c02019d66109b5
status         FAIL
stage          jvmTest
tests          745 total / 5 failed
```

A002 cleared the A001 compiler-memory failure:

- `compileKotlinJvm` PASS;
- `compileTestKotlinJvm` PASS;
- string source check PASS;
- VexWeb generation/verification PASS;
- service-image assembly PASS.

It stopped before provider materialization or any lived Product/microphone effect.

## Five A002 failures

All five were proof/currentness defects exposed by the architecture correction, not new Product
runtime failures:

1. three dead Voice language-state keys remained in the workspace catalog after Voice stopped owning
   spoken-language state;
2. the workspace dataKey registry did not yet admit current VoiceInput/Local-ASR semantic refs;
3. the Voice renderer test still looked for the pre-correction language-owner expression;
4. the Release test scoped from the first `setInterval`, which is now the progressive dictation
   timer rather than the 2.5 s Conversation/Vision polling loop;
5. the Models test globally rejected `model-row`, accidentally treating Local-ASR model rows as the
   retired Settings -> Models card renderer.

Current source corrects those proof surfaces without changing the Voice runtime circuit.

## Exact source

```text
source PR  #310
head       c5c0fb72288036fafbbd0d9f240963db476c4582
tree       79790a6fd308d47b2d2b05c981bd802f0ae00baf
base       d2b241b903e34e0e14b3c561ffcfa77ae367662a
ahead      34
behind     0
```

## Build policy

A003 preserves the A002 bounded build policy that successfully removed the OOM:

```text
Gradle JVM max heap       2048m
Gradle max workers        2
Kotlin compiler           in-process
```

## Remaining acceptance predicate

If the repository gate passes, A003 continues through the current Product front door and asks for the
same lived Voice witness:

```text
Vex reply language = English
speech model       = Whisper Base Multilingual
spoken language    = 中文

mic
-> progressive Chinese Composer text
-> Stop
-> final Chinese editable Composer text
-> no automatic Send
```

Machine evidence must contain exact-source completion facts for both:

```text
partial -> modelRef=model.local-asr.whisper.base + language=zh
final   -> modelRef=model.local-asr.whisper.base + language=zh
```

No microphone audio or transcript content is retained.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=317799a2ab6c451a225081eca7565a191fbb7413 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-current-main/A003/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-CURRENT-MAIN-A003.zip
```

<!-- [VXG RealForever][PR310][VOICEINPUT][CURRENT-MAIN][TEMP-PROOF][A003] -->
