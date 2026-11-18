# A010 — independent browser + VoiceInput startup proof

[VXG RealForever]

A009 human witness established the final local Whisper transcript reaches the existing Composer.

A010 proves two product-currentness corrections discovered from that same lived run:

1. the human browser initial page must not be suppressed by an unrelated local
   Conversation/Context/Vision projection failure;
2. Local ASR currentness must be observed at startup so VoiceInput becomes available without the
   person opening Settings first.

## Exact Product candidate

```text
source PR  #245
head       0c17d4e445bb87439bcad6200ae6e622b3284ebe
```

## Human predicate

Do not open Settings first.

```text
fresh VexLife launch
-> initial Vextreme page appears in the left browser
-> microphone is already available in Conversation
-> click mic / speak / Stop
-> final transcript appears in editable Composer
-> no automatic Send
-> Settings may then be opened only as a secondary currentness check
```

The runner continues the content-free endpoint status capture from A009 for the separate JSON
serialization regression.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=c9f9569b7ceda40ba1da331533bcf4dc110a974e 'repos/VexGPT/VexLife-Prod/contents/relay/local-asr/A010/RUN-MAC.command')
```

Expected result:

```text
~/Downloads/RETURN-VEXLIFE-LOCAL-ASR-PRODUCT-CIRCUIT-A010.zip
```

<!-- [VXG RealForever][PR245][TEMP-HOST-PROOF][LOCAL-ASR][A010] -->
