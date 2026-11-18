# Temporary host proof for PR #262 — Whisper Base Multilingual A001

[VXG RealForever]

Exact source candidate:

```text
source PR  #262
head       54885105bb88ca352fb2bf998eec34802d971d9a
base       a363b216486f8f857dda4913176a9f9692135e3b
```

## What this proves

The carrier materializes two exact Home-owned model artifacts beside the same pinned whisper.cpp
runtime:

```text
Whisper Base English
  ggml-base.en.bin
  sha256 a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002
  bytes  147964211

Whisper Base Multilingual
  ggml-base.bin
  sha256 60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe
  bytes  147951465
```

Then it requires the Product Local-ASR projection to report both options READY.

The lived human step explicitly selects **whisper.cpp · Whisper Base Multilingual**. The carrier only
passes after exact-source chronology observes both a progressive `partial` pass and a `final` pass
whose modelRef is:

`model.local-asr.whisper.base`

So English Base cannot accidentally satisfy the multilingual proof.

## Human action

Run exactly:

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=d47203720b812738be230fd8a592a50cc85f617a 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-multilingual/A001/RUN-MAC.command')
```

In VexLife:
1. Open Settings -> Local voice recognition.
2. Confirm both Base English and Base Multilingual appear.
3. Select Base Multilingual and confirm Ready locally.
4. Return to Conversation; speak while the mic remains active; confirm progressive text appears.
5. Stop and confirm final editable text. Do not Send unless you personally want to.

If your Vex reply language is already non-English, you can speak that language. The proof does not
retain transcript text or microphone audio.

Expected result:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-MULTILINGUAL-A001.zip
```

This PR is proof carriage only and should close unmerged after evidence is consumed.

<!-- [VXG RealForever][PR262][MULTILINGUAL-BASE][TEMP-PROOF][A001] -->
