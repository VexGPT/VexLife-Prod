# Current-main VoiceInput A002 — architecture-current Chinese coherence proof

[VXG RealForever]

This continues source PR #310 after consuming A001 and rereading Voice through the current
VexArchitecture ownership boundaries.

## What A001 established

A001 RETURN:

```text
sha256     3350bc1b85c706b396f1fc6e9cda653e47e2dedf8d6f4b354023b9ab563740a1
source     412a90db21236a0e860ea2f2a98e606de1bbce3d
sourceTree 347290d87a2326c08051185bebee0e261d6106b6
status     FAIL
stage      compileKotlinJvm
failure    java.lang.OutOfMemoryError: GC overhead limit exceeded
```

The failure occurred inside Kotlin/JVM backend/ASM method generation before provider materialization,
Product front-door activation, microphone capture, or Local-ASR inference. It is therefore build
evidence, not a Voice/Whisper/Chinese/lifetime failure.

## Architecture correction before replay

Current VexArchitecture says semantic meaning, outside truth, active realization, chronology, and
presentation remain separate owners.

The prior Voice source still carried an older `VoiceInputFamily.Language` value tied to Vex reply
language while current Local ASR already owned the person's spoken-language choice. The browser then
discarded the Family language payload and independently used
`LocalAsrProjection.resolvedSpokenLanguageCode`.

A002 source removes that disconnected double truth:

```text
VoiceInput Family
  owns: Ready / Requesting microphone / Listening / Stopping / Held / Failed
        Start / Stop
        Begin local dictation / End local dictation

Local ASR
  owns: selected speech model
        spoken-language setting
        resolved spoken language
        provider/model currentness

Browser realization
  on Begin effect:
    snapshot current Local-ASR resolved spoken language
    -> microphone/audio custody
    -> partial/final transcription
    -> existing editable Composer
```

No new architecture root was added.

## Exact source

```text
source PR  #310
head       4d509b1f72ddc6cfeef38ce94192b62b4b61a3e4
tree       382dfcc3fcf7e5b9dd47e90900c02019d66109b5
base       d2b241b903e34e0e14b3c561ffcfa77ae367662a
```

The branch has already been currentized over current main / merged #307 private-image retention.

## Repository proof

A002 runs the same complete repository gate as A001, but A001 showed that the implicit build JVM
budget was insufficient. A002 therefore makes the proof budget explicit:

```text
Gradle JVM max heap            2048m
Gradle max workers             2
Kotlin compiler execution      in-process
```

This is proof-carrier sizing, not Product semantics or a repository-wide build-policy change.

If the same JVM compiler/codegen failure persists under this bounded budget, source shape becomes the
next investigation edge rather than repeatedly increasing memory.

## Lived predicate

Keep Vex reply language **English**.

In Settings -> Local voice recognition:

1. Confirm Base English and Base Multilingual are listed with their storage controls.
2. Use **whisper.cpp · Whisper Base Multilingual**.
3. Set **Spoken language = 中文**.
4. Return to Conversation and click the mic.
5. Speak a natural Chinese sentence without immediately pressing Stop.
6. Progressive Composer text should appear in Chinese.
7. Continue briefly, press Stop, and confirm the final text remains Chinese and editable.
8. Do not press Send unless you personally want to.

Machine PASS still requires exact-source facts for both:

```text
partial -> modelRef=model.local-asr.whisper.base + language=zh
final   -> modelRef=model.local-asr.whisper.base + language=zh
```

No microphone audio or transcript content is retained in the RETURN.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=2b1019d941aa5b3f244fa484570083faf46067b1 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-current-main/A002/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-CURRENT-MAIN-A002.zip
```

<!-- [VXG RealForever][PR310][VOICEINPUT][CURRENT-MAIN][TEMP-PROOF][A002] -->
