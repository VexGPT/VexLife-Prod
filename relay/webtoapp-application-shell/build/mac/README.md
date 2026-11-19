# PR #396 Mac candidate build delivery

[VXG RealForever]

Single **DO-NOT-MERGE** VexRelay/VexLocalBridge companion for producing one human-testable macOS
candidate build from exact WebToApp source.

## Attempt history

```text
A001  TERMINAL SAFE_FAILURE
      Product head d13e8e4a774aea60362491420ffeee2c07f91d1c
      first source failure AppleScriptObjC launch parser collision

A002  TERMINAL SAFE_FAILURE
      Product head 9dfb0262fb3a632406e86d357a43e7ba66a617e4
      first source failure false Swift-unavailable classification

A003  TERMINAL SAFE_FAILURE
      Product head f917d2d1ed2605ce2b6ebfd7c0c747397c4e72a7
      first source failure macOS-unavailable WebKit callback + Swift @main parse mode

A004  TERMINAL SAFE_FAILURE
      Product head 7b6c5cf9bddfd867f9520040cb9f4ba38250b89e
      first source failure duplicate NSMicrophoneUsageDescription insertion after osacompile

A005  PAYLOAD PASS / CARRIER FINALIZATION FAILED
      Product head ca0304e310ec7a721db8e15dfab6c5275084c10e
      payload exit 0; exact Mac build and retained evidence verified
      outer VexLocalBridge deleted transient work root before RETURN copied retained evidence
      no canonical RETURN formed; do not replay exact attempt

A006  CURRENT
      Product head ca0304e310ec7a721db8e15dfab6c5275084c10e
      Product tree 5e9edaba29fd050110f28e9b752ecc4becd19975
```

A001-A005 are historical attempts. Do not replay them.

A006 does **not** change Product source. It corrects retained-evidence custody only:
- build remains bound to the same A005-proven Product bytes;
- DMG, receipt and SHA sidecar are copied to a bridge-owned custody root outside transient
  `bridge/work/<task>/<attempt>` before payload terminal return;
- payload guards against accidentally placing retained evidence under `VEXLOCALBRIDGE_RESULT_DIR`;
- canonical RETURN formation can therefore consume the retained files after closed-pass work cleanup.

## Meaning

This companion **builds only**. It does not launch VexLife or claim SourceOwner PASS,
application-lived PASS, Product-Quit PASS, READY, release, or merge.

Expected PASS delivery remains:

```text
~/Downloads/VexLife-WebToApp-Mac-ca0304e3/
  VexLife-macOS-AppleSilicon.dmg
  VexLife-macOS-AppleSilicon.dmg.sha256
  VexLife-macOS-AppleSilicon.receipt.json
  BUILD-DELIVERY.json
```

<!-- [VXG RealForever][PR396][MAC-BUILD-DELIVERY][A006-CURRENT][CUSTODY-REPAIR][DO-NOT-MERGE] -->
