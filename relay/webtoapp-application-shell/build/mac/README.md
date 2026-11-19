# PR #396 Mac candidate build delivery

[VXG RealForever]

Single **DO-NOT-MERGE** VexRelay/VexLocalBridge companion for producing one human-testable macOS
candidate build from exact WebToApp source.

## Exact Product candidate

```text
owner VexGPT/VexLife-Prod#396
base  fdb9f77bbd9895d0ab6932f669091b46ea813e9b
head  d13e8e4a774aea60362491420ffeee2c07f91d1c
tree  98ba3219e49f722272c976171cb736ed19ae9ac9
```

## Exact carrier

```text
Vextreme-SDK #1959
head 36eb5c82becd97dcfcbc8fa67455a74d8540503a
tree bea091ed67793931b838c7b412d9abce39a2e373
```

## Meaning

This companion **builds only**. It does not claim SourceOwner PASS, application-lived PASS,
Product-Quit PASS, READY, release, or merge.

```text
exact #396 Git bundle
-> archiveVexLifeProdMacApplication
-> verify VexLife.app source binding / NATIVE_EMBEDDED mode / executable / ad-hoc code seal
-> verify VexLife-macOS-AppleSilicon.dmg
-> retain DMG + source-bound receipt in canonical VexLocalBridge RETURN
-> consume RETURN locally
-> copy verified DMG + receipt into ~/Downloads/VexLife-WebToApp-Mac-d13e8e4a/
```

The payload never:
- launches VexLife;
- starts/stops Product service;
- touches Home, Conversation or Memory;
- acquires/switches a model;
- kills any process;
- installs into /Applications;
- publishes or merges anything.

The broader VexLife-Prod host policy is reused because it is the current exact repository profile, but
the source-reviewed payload exercises no lifecycle/application-launch mutation.

## Human result

After PASS, the human-facing artifact is:

```text
~/Downloads/VexLife-WebToApp-Mac-d13e8e4a/VexLife-macOS-AppleSilicon.dmg
```

This build may be opened and tested manually. Manual testing is observation; it does not by itself
create the formal Mac application-lived PASS consumed by #398.

<!-- [VXG RealForever][PR396][MAC-BUILD-DELIVERY][DO-NOT-MERGE] -->
