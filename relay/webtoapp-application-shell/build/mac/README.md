# PR #396 Mac candidate build delivery

[VXG RealForever]

Single **DO-NOT-MERGE** VexRelay/VexLocalBridge companion for producing one human-testable macOS
candidate build from exact WebToApp source.

## Current attempt

```text
A001  TERMINAL SAFE_FAILURE
      Product head d13e8e4a774aea60362491420ffeee2c07f91d1c
      RETURN sha256 07ed05428bfc54df22236fc1e109eae380024fe8e3b1d4b8f33f2b767023fbcf
      first source failure osacompile -2741 at setup-window line 254

A002  CURRENT
      Product head 9dfb0262fb3a632406e86d357a43e7ba66a617e4
      Product tree 871fe1db8b963cc0dacefb0e85f756a5c45b9ddc
```

A001 is immutable historical evidence. Do not replay it.

A002 binds the repaired presenter launch path:
```text
setExecutableURL
setArguments
launchAndReturnError:(reference)
waitUntilExit
```
and the packaging regression that forbids `presenter's launch()`.

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
-> copy verified DMG + receipt into ~/Downloads/VexLife-WebToApp-Mac-9dfb0262/
```

The payload never:
- launches VexLife;
- starts/stops Product service;
- touches Home, Conversation or Memory;
- acquires/switches a model;
- kills any process;
- installs into /Applications;
- publishes or merges anything.

## Human result

After A002 PASS:

```text
~/Downloads/VexLife-WebToApp-Mac-9dfb0262/
  VexLife-macOS-AppleSilicon.dmg
  VexLife-macOS-AppleSilicon.dmg.sha256
  VexLife-macOS-AppleSilicon.receipt.json
  BUILD-DELIVERY.json
```

This build may be opened and tested manually. Manual testing is observation; it does not by itself
create the formal Mac application-lived PASS consumed by #398.

<!-- [VXG RealForever][PR396][MAC-BUILD-DELIVERY][A002-CURRENT][DO-NOT-MERGE] -->
