# PR #396 WebToApp application-shell SourceOwner A005

[VXG RealForever]

A001-A004 are immutable superseded-before-execution history.

A005 binds the **same frozen Product candidate as A004**; Product source did not change. A004 is
superseded only because its proof threshold omitted compilation/typechecking of the source-owned Swift
WebKit presenter.

## Exact candidate

```text
base   fdb9f77bbd9895d0ab6932f669091b46ea813e9b
head   d13e8e4a774aea60362491420ffeee2c07f91d1c
tree   98ba3219e49f722272c976171cb736ed19ae9ac9
branch VXG-111926-chatgpt-webtoapp-application-shell-a001
paths  22
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

The second predicate is required because WebToApp now depends on concrete WKUIDelegate,
WKNavigationDelegate and WKDownloadDelegate API surfaces. Static source-string regression tests do
not substitute for Swift type correctness.

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

The native carrier consumes the inner canonical RETURN and the A005 consumer validates/consumes the
outer RETURN locally.

## Merge boundary

SourceOwner PASS still cannot create merge authority.

```text
built Mac VexLife application lived run PASS
+ built Windows VexLife application lived run PASS
= required before merge consideration
```

<!-- [VXG RealForever][PR396][PR397][SOURCEOWNER][A005] -->
