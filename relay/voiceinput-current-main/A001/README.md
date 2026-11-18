# Current-main VoiceInput A001 — Chinese coherence + lifetime-current proof

[VXG RealForever]

This is the temporary lived-proof surface for current-main source PR #310.

## Why this replaces A005

A005 was built from a pre-#281 Voice source. It therefore saw the newly recovered Product service:

```text
source 4a65fe6771dae8553974969b321472bcc2917bd5
pid    93164
```

through obsolete lifetime semantics and paused at `HOLD_UNKNOWN`.

That service is the accepted #281 Product-service lifetime recovery implementation, not disposable
Voice proof residue. A005 must not stop it.

PR #310 ports Voice onto current main, preserving #281/#291 lifetime custody.

## Exact source

```text
source PR  #310
head       412a90db21236a0e860ea2f2a98e606de1bbce3d
base       5675d93fb3343fd05459a14fba810c0de2ca1ef3
```

## Repository proof

Before any lived effect, the carrier runs:

```text
compileKotlinJs
compileTestKotlinJs
compileKotlinJvm
compileTestKotlinJvm
jvmTest
checkVexLifeStrings
generateVexLifeWebP1
verifyVexLifeWebP1
assembleVexLifeProdServiceImage
```

A failure packages a RETURN instead of proceeding to the mic.

## Host/currentness proof

The carrier then:
- materializes the already-qualified pinned whisper.cpp v1.9.5 provider and both exact Base weights;
- uses the candidate Product front door, including the current `SERVICE_RECOVERY_REQUIRED` state;
- activates only the Product-owned primary Control;
- requires the running service to report exact candidate source currentness.

There is no manual launchd `STOP` path in this carrier.

## Lived predicate

Keep Vex reply language **English**.

In Settings -> Local voice recognition:

1. Confirm Base English and Base Multilingual are listed with Download/Use/Remove storage controls.
2. Use **Whisper Base Multilingual**.
3. Set **Spoken language = 中文**.
4. Return to Conversation and speak a natural Chinese sentence while the mic remains active.
5. Progressive Composer text must appear in **Chinese**, not English.
6. Continue briefly and press Stop.
7. Final Composer text remains Chinese and editable; Send remains a separate human action.

Machine PASS requires exact-source completion facts for both:

```text
partial
  modelRef=model.local-asr.whisper.base
  language=zh

final
  modelRef=model.local-asr.whisper.base
  language=zh
```

No microphone audio or transcript content is retained.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=e63e0fc116fafb8e67c798788280fa74ad136986 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-current-main/A001/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-CURRENT-MAIN-A001.zip
```

<!-- [VXG RealForever][PR310][CURRENT-MAIN][VOICEINPUT][TEMP-PROOF][A001] -->
