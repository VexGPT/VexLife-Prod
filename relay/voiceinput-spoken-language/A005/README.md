# Chinese spoken-language coherence A005 — human-authorized held-unknown host recovery

[VXG RealForever]

A004 RETURN was consumed exactly.

```text
RETURN sha256
a882ce43020f560445586d25b7e26f80514bac60214f72646512e2c683d36e70

status
FAIL

source
de6a23b763bbf6a18489c5b84f04f59a8938e285

tree
baf1c713b952dfc9364968cdc7f917a7adde47e0
```

## What A004 proved

The exact source cleared the complete repository gate:

```text
compileKotlinJs              PASS
compileTestKotlinJs          PASS
compileKotlinJvm             PASS
compileTestKotlinJvm         PASS
checkVexLifeStrings          PASS
generateVexLifeWebP1         PASS
verifyVexLifeWebP1           PASS
assembleVexLifeProdServiceImage PASS
jvmTest                      PASS
BUILD SUCCESSFUL
```

The proof then stopped at Product entry before lived microphone execution.

Front-door observation:

```text
semanticStateRef = HOLD_UNKNOWN
detail = stable product-service launchd label is present without a product lifetime receipt
launchd = Running(pid=60349)
```

This is a host-lifetime ownership boundary, not a Voice/source failure.

The Product lifetime owner deliberately refuses automatic cleanup:

```text
UNKNOWN != ABSENT
label present != process ownership
no product receipt -> no automatic bootout/delete authority
```

Current main preserves the same law.

## A005 human boundary

A005 does not infer ownership.

If the same held-unknown state is observed, it first captures only read-only identity:
- `launchctl print gui/<uid>/com.vexlife.prod.service`;
- loaded process command/PID when available;
- listener identity when available;
- localhost source-currentness when available;
- whether the selected Home contains the lifetime receipt/plist coordinate.

It then displays the loaded process to Victor.

Only if Victor recognizes it as the VexLife test/background service he intends to stop and types
exactly:

```text
STOP
```

does the carrier run:

```text
launchctl bootout gui/<uid>/com.vexlife.prod.service
```

It verifies the launchd label becomes absent, deletes no Vex Home files, and then returns control to
the Product front door. The Product must re-observe a lawful restart/update state before any Product
setup effect is activated.

Anything other than exact `STOP` preserves the loaded job and packages evidence.

## Exact Product source

```text
de6a23b763bbf6a18489c5b84f04f59a8938e285
```

No source change was required after A004.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=909ce8852b035c2c3c5ef662a7ceeff5d776d027 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-spoken-language/A005/RUN-MAC.command')
```

After host convergence, the lived predicate remains:

```text
Vex reply language = English
Base Multilingual selected
Spoken language = 中文
partial inference: model.local-asr.whisper.base + language=zh
final inference:   model.local-asr.whisper.base + language=zh
human witness: progressive/final Composer text stays Chinese and editable
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-SPOKEN-LANGUAGE-A005.zip
```

<!-- [VXG RealForever][PR273][A004-HOST-HOLD][HUMAN-BOUNDARY][A005] -->
