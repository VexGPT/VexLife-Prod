# Temporary proof for PR #273 — Chinese spoken-language coherence A001

[VXG RealForever]

## Why this attempt exists

Base Multilingual already transcribed lived Chinese audio, but the Composer produced English-looking
text because VoiceInput still inherited Vex's English reply-language setting and sent `-l en` to
Whisper.

That was a VexLife configuration/coherence defect, not evidence that multilingual Whisper requires
one model download per language.

This source separates:

```text
speech model artifact
spoken microphone language
Vex reply language
```

## Exact source

```text
source PR  #273
head       f616571a897693ff4dd325672c7a7c270dbaa323
base       1413e2795d69099802312228ada535404b429fe6
```

## Human predicate

Keep Vex reply language **English**.

In Settings -> Local voice recognition:

1. Confirm Base English and Base Multilingual are offered with storage controls.
2. Use **Whisper Base Multilingual**.
3. Set **Spoken language = 中文**.
4. Return to Conversation and speak naturally in Chinese while the mic remains active.
5. Progressive text must appear in **Chinese**, not be coerced into English.
6. Stop and confirm final editable Composer text remains Chinese.
7. Send remains a separate human action.

## Machine predicate

The carrier only passes after exact-source chronology observes both:

```text
LOCAL_ASR_INFERENCE_COMPLETED
  dictationPass=partial
  modelRef=model.local-asr.whisper.base
  language=zh

LOCAL_ASR_INFERENCE_COMPLETED
  dictationPass=final
  modelRef=model.local-asr.whisper.base
  language=zh
```

The final Local-ASR projection must also report:

```text
selectedModelRef          model.local-asr.whisper.base
spokenLanguageMode        FIXED
spokenLanguageCode        zh
resolvedSpokenLanguageCode zh
```

No microphone audio or transcript text is retained in the RETURN.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=1bebc2229d695b7d9ff179337657c3ef558dce3c 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-spoken-language/A001/RUN-MAC.command')
```

Expected result:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-SPOKEN-LANGUAGE-A001.zip
```

This PR is temporary proof carriage only; close unmerged after evidence consumption.

<!-- [VXG RealForever][PR273][SPOKEN-LANGUAGE][ZH][TEMP-PROOF][A001] -->
