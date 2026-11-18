# Voice language/output A001 — Chinese scripts + Composer modes

[VXG RealForever]

Successor proof for draft source PR #331, stacked on the A004-passed Voice circuit in #310.

## Reused evidence

Parent Voice proof:

```text
RETURN sha256 96cc5bb17d5a0cfd79518f397d27631e368f7162a79216e3b94c9e1119640cbb
source        2c01df0d45a0269ebd53bc1163f82f2c5b284f14
disposition   PASS
```

The pinned whisper.cpp source, persistent `whisper-server`, native dylibs and exact Base model bytes
are unchanged by #331. A001 therefore verifies and reuses that qualified Home provider instead of
rebuilding it.

## Exact candidate

```text
source PR #331
head      91f1370cd89813de3c94afc8f4849237bad74f3a
base PR   #310
```

## What is new

```text
Vex reply language/script
  简体中文  zh-Hans
  繁體中文  zh-Hant
       ↓
Follow reply language
       ↓
spoken Chinese remains zh

Composer output
  AS_SPOKEN
  TRANSLATE_TO_ENGLISH
```

As-spoken remains the default and preserves mixed-language dictation. Translate-to-English uses
Whisper's real native source-language -> English translation task and requires Base Multilingual.

The proof does not claim English -> Chinese or arbitrary target translation.

## Lived sequence

The terminal opens current VexLife and waits for content-free machine predicates while the person:

1. observes both Chinese reply-script choices;
2. selects 简体中文 and Follow Vex reply language;
3. uses Base Multilingual + As spoken and speaks Chinese -> English in one dictation;
4. switches Composer output to Translate to English and speaks Chinese;
5. switches Vex reply language to 繁體中文 while Follow remains selected;
6. may inspect Speak Vex aloud to witness script-matching local voice preference.

Composer text itself remains human-only witness. No microphone audio or transcript content is placed
in the RETURN. Send remains a separate human action.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=4c3233dbdfa2a83f6f28b7456bcbfbe4152ef3c4 'repos/VexGPT/VexLife-Prod/contents/relay/voice-language-output/A001/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICE-LANGUAGE-OUTPUT-A001.zip
```

<!-- [VXG RealForever][PR331][VOICE-LANGUAGE-OUTPUT][TEMP-PROOF][A001] -->
