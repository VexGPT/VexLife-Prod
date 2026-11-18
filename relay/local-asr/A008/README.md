# Temporary Relay carriage for PR #245 — Local ASR / VoiceInput A008

[VXG RealForever]

This temporary surface preserves the evolving host-proof history for PR #245 without turning proof
carriage into Product architecture.

## Exact Product candidate

```text
source PR  #245
head       d63690b103b462cc7fa46a1eeda2edd64942ecec
```

A008 proves the repaired Settings-currentness relationship plus the lived VoiceInput circuit:

```text
Product service converges
-> Settings re-observes Models / Diagnostics / Local ASR
-> Local ASR projects selected whisper.cpp Base English as READY
-> Brave microphone permission/capture
-> in-memory PCM/WAV
-> VexLife localhost
-> Home-owned whisper.cpp
-> local inference
-> transcript reaches existing editable Composer
-> Send remains human-only
```

The RETURN contains no microphone audio or transcript text.

## Human boundary

Run exactly:

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=4624d29a96587f5b95fc2fb609d2fd06fe485a45 'repos/VexGPT/VexLife-Prod/contents/relay/local-asr/A008/RUN-MAC.command')
```

Expected canonical host result:

```text
~/Downloads/RETURN-VEXLIFE-LOCAL-ASR-PRODUCT-CIRCUIT-A008.zip
```

This PR is temporary proof carriage only. It must not be merged as Product source.

<!-- [VXG RealForever][PR245][TEMP-RELAY][LOCAL-ASR][A008] -->
