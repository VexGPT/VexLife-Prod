# PR #396 Mac candidate build delivery

[VXG RealForever]

Single **DO-NOT-MERGE** VexRelay/VexLocalBridge companion for producing one human-testable macOS
candidate build from exact WebToApp source.

## Attempt history

```text
A001  TERMINAL SAFE_FAILURE
      Product head d13e8e4a774aea60362491420ffeee2c07f91d1c
      RETURN sha256 07ed05428bfc54df22236fc1e109eae380024fe8e3b1d4b8f33f2b767023fbcf
      first source failure osacompile -2741 at setup-window line 254

A002  TERMINAL SAFE_FAILURE
      Product head 9dfb0262fb3a632406e86d357a43e7ba66a617e4
      RETURN sha256 9f56ca0519e4f05aa71589f8e6600d9b8d15fac17900d39087e1a631233d58f6
      first source failure false Swift-unavailable classification after xcrun selected swiftc

A003  CURRENT
      Product head f917d2d1ed2605ce2b6ebfd7c0c747397c4e72a7
      Product tree 6045ab4a1711633606ebac394296d18d293be5ae
```

A001 and A002 are immutable historical evidence. Do not replay them.

A003 binds the source repair that invokes Swift through:
```text
/usr/bin/xcrun --sdk macosx swiftc ...
```
instead of rejecting the toolchain's selected compiler path based on `NOFOLLOW_LINKS` regular-file
shape.

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
-> copy verified DMG + receipt into ~/Downloads/VexLife-WebToApp-Mac-f917d2d1/
```

The payload never launches VexLife, changes Product lifecycle/Home/model state, kills a Product
process, installs into /Applications, publishes, or merges anything.

## Human result

After A003 PASS:

```text
~/Downloads/VexLife-WebToApp-Mac-f917d2d1/
  VexLife-macOS-AppleSilicon.dmg
  VexLife-macOS-AppleSilicon.dmg.sha256
  VexLife-macOS-AppleSilicon.receipt.json
  BUILD-DELIVERY.json
```

This build may be opened and inspected manually. Manual inspection is not the formal application-lived
PASS.

<!-- [VXG RealForever][PR396][MAC-BUILD-DELIVERY][A003-CURRENT][DO-NOT-MERGE] -->
