# PR #396 Mac candidate build delivery

[VXG RealForever]

Single **DO-NOT-MERGE** VexRelay/VexLocalBridge companion for producing one human-testable macOS
candidate build from exact WebToApp source.

## Attempt history

```text
A001  TERMINAL SAFE_FAILURE
      Product head d13e8e4a774aea60362491420ffeee2c07f91d1c
      RETURN sha256 07ed05428bfc54df22236fc1e109eae380024fe8e3b1d4b8f33f2b767023fbcf
      first source failure AppleScriptObjC launch parser collision

A002  TERMINAL SAFE_FAILURE
      Product head 9dfb0262fb3a632406e86d357a43e7ba66a617e4
      RETURN sha256 9f56ca0519e4f05aa71589f8e6600d9b8d15fac17900d39087e1a631233d58f6
      first source failure false Swift-unavailable classification

A003  TERMINAL SAFE_FAILURE
      Product head f917d2d1ed2605ce2b6ebfd7c0c747397c4e72a7
      RETURN sha256 539b333554f3a9f967bdd11d9c15fa087c7831d718a226521a8e8361fd7d68e0
      first source failure macOS-unavailable WebKit callback + Swift @main parse mode

A004  TERMINAL SAFE_FAILURE
      Product head 7b6c5cf9bddfd867f9520040cb9f4ba38250b89e
      RETURN sha256 0d3f601f6f92570664882756260ab290bd533d6cec08d68a9707876213812178
      first source failure duplicate NSMicrophoneUsageDescription insertion after osacompile

A005  CURRENT
      Product head ca0304e310ec7a721db8e15dfab6c5275084c10e
      Product tree 5e9edaba29fd050110f28e9b752ecc4becd19975
```

A001-A004 are immutable historical evidence. Do not replay them.

A005 binds:
- outer VexLife.app privacy metadata uses `plutil -replace NSMicrophoneUsageDescription`;
- nested VexLifeProduct.app still owns its explicit source-authored microphone usage description;
- Swift presenter compilation remains `xcrun --sdk macosx swiftc -parse-as-library`;
- prior AppleScriptObjC and Swift/WebKit repairs remain preserved.

## Meaning

This companion **builds only**. It does not launch VexLife or claim SourceOwner PASS,
application-lived PASS, Product-Quit PASS, READY, release, or merge.

```text
exact #396 Git bundle
-> archiveVexLifeProdMacApplication
-> verify VexLife.app / nested VexLifeProduct.app
-> verify source binding + NATIVE_EMBEDDED + code seals
-> verify VexLife-macOS-AppleSilicon.dmg
-> retain DMG + receipt in canonical VexLocalBridge RETURN
-> consume RETURN locally
-> copy verified delivery into ~/Downloads/VexLife-WebToApp-Mac-ca0304e3/
```

## Human result

After A005 PASS:

```text
~/Downloads/VexLife-WebToApp-Mac-ca0304e3/
  VexLife-macOS-AppleSilicon.dmg
  VexLife-macOS-AppleSilicon.dmg.sha256
  VexLife-macOS-AppleSilicon.receipt.json
  BUILD-DELIVERY.json
```

<!-- [VXG RealForever][PR396][MAC-BUILD-DELIVERY][A005-CURRENT][DO-NOT-MERGE] -->
