# Current-main VoiceInput A004 — persistent Whisper progressive proof

[VXG RealForever]

A004 continues source PR #310 from Victor's lived A003 screenshots and diagnostics export.

## Lived regression consumed

Debug export:

```text
sha256 0182c7d74c526793dd027f35a5b9be0f21f70d33eb10088dc97d576ce0f99c25
source c5c0fb72288036fafbbd0d9f240963db476c4582
```

The lived path established:

```text
Base Multilingual selected
Spoken language 中文 / zh
mic Control -> Listening
PCM capture -> PRESENT
Local-ASR partial request -> STARTED
Composer progressive text -> ABSENT
```

The first Chinese partial eventually completed after:

```text
inferenceMillis=99288
```

By then Stop and later Settings changes had already advanced the browser session. The stale result was
discarded correctly; the old Stop then attempted final zh against a now-English microphone-language
setting and the service rejected that mismatch.

This is not evidence that Chinese selection or microphone capture failed.

## Root cause

The progressive browser loop was driving the temporary process provider:

```text
every partial
-> spawn whisper-cli
-> load Base model again
-> feed WAV through child stdin
-> infer
-> process exits
```

The old provider also started its 45-second process timeout only *after* writing the complete WAV to
the child's stdin. A blocked pipe/model startup could therefore consume time outside that timeout;
the lived 99.288-second partial falsified this topology for progressive dictation.

The historical progressive PR already named CLI-per-partial as a temporary pressure surface and a
persistent Whisper context as the intended successor once progressive UX was retained.

## Source correction

A004 source keeps the existing VexArchitecture ownership:

```text
VoiceInput Family
  -> Start / Stop + session State + Begin / End

Local ASR owners
  -> speech model + spoken language + provider currentness

service-lifetime Local-ASR runtime
  -> one warm pinned whisper-server for the selected model

browser realization
  Begin
  -> prepare selected provider/model concurrently with microphone permission
  -> only enter capture/Listening after both are ready
  -> bounded partial/final HTTP inference
  -> existing Composer

Send remains separate.
```

The same pinned whisper.cpp source is used. A004 simply builds and installs its
`whisper-server` executable alongside `whisper-cli`.

The browser session-currentness race is also repaired:

- Requesting / Listening / Stopping remain Family-owned until their realization settles;
- capability refresh cannot overwrite an in-flight session with Ready;
- speech model / spoken-language changes remain disabled while the session owns State;
- an old Begin preparation cannot fail a newer session.

## Exact source

```text
source PR  #310
head       2c01df0d45a0269ebd53bc1163f82f2c5b284f14
tree       137c87bd026c0aff67132707191009756b19384b
base       b95c2034ad9416920f24801ee87f5c21eda47cdf
```

## Lived predicate

Keep Vex reply language **English**.

1. Settings -> Local voice recognition: use **Whisper Base Multilingual**.
2. Set **Spoken language = 中文**.
3. Return to Conversation.
4. Click the microphone. The first Begin may briefly remain in preparation while the model becomes
   warm; once it says Listening, speak a natural Chinese sentence.
5. Chinese text should begin appearing in the Composer while you continue speaking.
6. Continue briefly, then press Stop.
7. Final text remains Chinese and editable.
8. Send remains a separate human action.

Machine PASS still requires exact-source completion facts for both:

```text
partial -> model.local-asr.whisper.base + language=zh
final   -> model.local-asr.whisper.base + language=zh
```

A partial inference that exceeds its bounded provider request will fail the proof rather than remain
alive for ~99 seconds.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=ad916f598f03f24acbf41eeeaa14a07137a10bc4 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-current-main/A004/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-CURRENT-MAIN-A004.zip
```

No microphone audio or transcript content is retained in the RETURN.

<!-- [VXG RealForever][PR310][VOICEINPUT][PERSISTENT-WHISPER][TEMP-PROOF][A004] -->
