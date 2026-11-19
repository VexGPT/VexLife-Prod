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
      first source failure macOS-unavailable WebKit motion delegate + Swift @main parse mode

A004  CURRENT
      Product head 7b6c5cf9bddfd867f9520040cb9f4ba38250b89e
      Product tree 9344d214de6b73177dc164a05ef0073f3283e786
```

A001-A003 are immutable historical evidence. Do not replay them.

A004 binds:
- no macOS use of the iOS-only device-orientation/motion WKUIDelegate callback;
- Swift presenter compilation with `-parse-as-library`;
- the prior xcrun compiler invocation repair;
- the prior AppleScriptObjC NSTask launch repair.

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
-> copy verified delivery into ~/Downloads/VexLife-WebToApp-Mac-7b6c5cf9/
```

## Human result

After A004 PASS:

```text
~/Downloads/VexLife-WebToApp-Mac-7b6c5cf9/
  VexLife-macOS-AppleSilicon.dmg
  VexLife-macOS-AppleSilicon.dmg.sha256
  VexLife-macOS-AppleSilicon.receipt.json
  BUILD-DELIVERY.json
```

<!-- [VXG RealForever][PR396][MAC-BUILD-DELIVERY][A004-CURRENT][DO-NOT-MERGE] -->
