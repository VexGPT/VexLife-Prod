# PR #396 WebToApp application-shell SourceOwner A006

[VXG RealForever]

A001-A005 are immutable superseded-before-execution history.

A006 binds the current repaired Product candidate after the two real Mac build findings:
1. AppleScriptObjC `presenter's launch()` parser collision;
2. false Swift-unavailable classification caused by requiring the `xcrun --find swiftc` result to be a
   regular executable with `NOFOLLOW_LINKS`.

## Exact candidate

```text
base   fdb9f77bbd9895d0ab6932f669091b46ea813e9b
head   f917d2d1ed2605ce2b6ebfd7c0c747397c4e72a7
tree   6045ab4a1711633606ebac394296d18d293be5ae
branch VXG-111926-chatgpt-webtoapp-application-shell-a001
paths  23
```

## SourceOwner threshold

```text
./gradlew
  checkVexLifeStrings
  jvmTest
  compileKotlinJs
  compileTestKotlinJs

/usr/bin/xcrun swiftc -typecheck
  -framework AppKit
  -framework WebKit
  packaging/macos-webkit/VexLifeProductPresentation.swift
```

A006 includes the packaging regression that forbids the AppleScript-reserved `presenter's launch()`
selector shape and the Mac application regression that requires Swift compilation through `xcrun`
without the rejected `--find swiftc` filesystem-form probe.

## Route

```text
source.vexrelay.current-contract/v1
ExactHeadSourceOwnerProof
adapter.vexlocalbridge.native-vexrelay-task-archive/v1
macos-victor

GitHub Actions NOT USED
Local Workspace NOT USED
Victor ZIP carriage NOT USED
Product/application launch HELD
real Home/model effects HELD
merge HELD
```

## Merge boundary

SourceOwner PASS still cannot create merge authority.

```text
built Mac VexLife application lived run PASS
+ built Windows VexLife application lived run PASS
= required before merge consideration
```

<!-- [VXG RealForever][PR396][PR397][SOURCEOWNER][A006] -->
